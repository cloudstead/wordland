package wordland.service;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.cobbzilla.wizard.cache.redis.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import wordland.dao.*;
import wordland.model.*;
import wordland.model.game.GamePlayer;
import wordland.model.game.GameState;
import wordland.model.game.GameStateChange;
import wordland.model.game.RoomState;
import wordland.model.json.GameRoomSettings;
import wordland.model.support.GameRoomJoinResponse;
import wordland.model.support.PlayedTile;
import wordland.server.WordlandConfiguration;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.json.JsonUtil.json;
import static org.cobbzilla.util.security.ShaUtil.sha256_hex;
import static org.cobbzilla.wizard.resources.ResourceUtil.invalidEx;
import static org.cobbzilla.wizard.resources.ResourceUtil.notFoundEx;

@Service @Slf4j @Accessors(chain=true)
public class GamesMaster {

    @Autowired private WordlandConfiguration configuration;
    @Autowired private SymbolSetDAO symbolSetDAO;
    @Autowired private SymbolDistributionDAO distributionDAO;
    @Autowired private PointSystemDAO pointSystemDAO;
    @Autowired private GameDictionaryDAO dictionaryDAO;
    @Autowired private GameBoardDAO gameBoardDAO;
    @Autowired private GameRoomDAO gameRoomDAO;

    @Autowired private RedisService redisService;

    @Getter(lazy=true) private final RedisService sessions = initSessions();
    private RedisService initSessions() { return redisService.prefixNamespace("gamesMaster/sessions/"); }

    @Getter(lazy=true) private final RedisService roomsByPlayer = initRoomsByPlayer();
    private RedisService initRoomsByPlayer() { return redisService.prefixNamespace("gamesMaster/roomsByPlayer/"); }

    @Getter @Setter private AtmosphereEventsService eventService;

    public Map<String, GameDaemon> rooms = new ConcurrentHashMap<>();

    public GameDaemon restartRoom(GameRoom room) {
        final GameDaemon daemon = rooms.get(room.getName());
        if (daemon != null) return daemon;
        return newRoom(room);
    }

    public GameDaemon newRoom(GameRoom room) {
        if (rooms.containsKey(room.getName())) {
            throw invalidEx("err.room.alreadyExists");
        }

        // fill out settings, based on names
        final GameRoomSettings roomSettings = room.getSettings();

        final String symbolSetName = roomSettings.symbolSetName();
        final SymbolSet symbolSet = symbolSetDAO.findByName(symbolSetName);
        if (symbolSet == null) throw notFoundEx(symbolSetName);
        roomSettings.setSymbolSet(symbolSet);

        final String distributionName = roomSettings.symbolDistributionName();
        final SymbolDistribution distribution = distributionDAO.findBySymbolSetAndName(symbolSet.getName(), distributionName);
        if (distribution == null) throw notFoundEx(distributionName);
        roomSettings.setSymbolDistribution(distribution);

        final String pointSystemName = roomSettings.pointSystemName();
        final PointSystem pointSystem = pointSystemDAO.findBySymbolSetAndName(symbolSet.getName(), pointSystemName);
        if (pointSystem == null) throw notFoundEx(pointSystemName);
        roomSettings.setPointSystem(pointSystem);

        final String dictionaryName = roomSettings.dictionaryName();
        final GameDictionary dictionary = dictionaryDAO.findBySymbolSetAndName(symbolSet.getName(), dictionaryName);
        if (dictionary == null) throw notFoundEx(dictionaryName);
        roomSettings.setDictionary(dictionary);

        final String boardName = roomSettings.boardName();
        final GameBoard board = gameBoardDAO.findByName(boardName);
        if (board == null) throw notFoundEx(boardName);
        roomSettings.setBoard(board);

        if (room.isTemplate()) {
            rooms.put(room.getName(), GameDaemon.templateDaemon(room.getName()));
            return null;
        } else {
            final GameDaemon gameDaemon = newGameDaemon(room);
            rooms.put(room.getName(), gameDaemon);
            gameDaemon.initGame();
            return gameDaemon;
        }
    }

    protected GameDaemon newGameDaemon(GameRoom room) {
        return configuration.autowire(new GameDaemon(room, eventService));
    }

    private GameDaemon getGameDaemon(String roomName) { return getGameDaemon(roomName, true); }

    private GameDaemon getGameDaemon(String roomName, boolean throwNotFoundEx) {
        final GameDaemon daemon = rooms.get(roomName);
        if (daemon == null && throwNotFoundEx) throw notFoundEx(roomName);
        return daemon;
    }

    private final Map<String, Collection<GameDaemon>> templateDaemons = new ConcurrentHashMap<>();

    public GameRoomJoinResponse addPlayer(GameRoom room, GamePlayer player) {
        final GameRoomJoinResponse response = _addPlayer(room, player);
        if (response != null) {
            final RedisService roomsByPlayer = getRoomsByPlayer();
            if (!roomsByPlayer.sismember(player.getId(), response.getRoom())) {
                roomsByPlayer.sadd(player.getId(), response.getRoom());
            }
        }
        return response;
    }

    public List<GameRoom> getRoomsForPlayer(String uuid) { return getRoomsForPlayer(uuid, null); }

    public List<GameRoom> getRoomsForPlayer(String uuid, RoomState matchState) {
        final RedisService roomsByPlayer = getRoomsByPlayer();
        final Set<String> roomNames = roomsByPlayer.smembers(uuid);
        final List<GameRoom> rooms = new ArrayList<>();
        if (!empty(roomNames)) {
            for (String roomName : roomNames) {
                final GameRoom room = gameRoomDAO.findByName(roomName);
                if (room == null) {
                    log.warn("getRoomsForPlayer: room not found, removing: " + roomName);
                    roomsByPlayer.srem(uuid, roomName);
                    continue;
                }
                if (room.isTemplate()) {
                    log.warn("getRoomsForPlayer: skipping template room: " + roomName);
                    continue;
                }
                final GameDaemon gameDaemon = getGameDaemon(roomName, false);
                if (gameDaemon == null) {
                    log.warn("getRoomsForPlayer: daemon not found: " + roomName);
                    continue;
                }
                final RoomState roomState = gameDaemon.getGameStateStorage().getRoomState();
                if (matchState == null || matchState == roomState) {
                    rooms.add(room.setRoomState(roomState));
                } else {
                    log.info("getRoomsForPlayer: skipping room, expected status "+ matchState +", had "+roomState+": " + roomName);
                }
            }
        }
        return rooms;
    }

    private GameRoomJoinResponse _addPlayer(GameRoom room, GamePlayer player) {

        if (room.isTemplate()) {
            // find daemons for template
            final Collection<GameDaemon> roomDaemons;
            synchronized (templateDaemons) {
                roomDaemons = templateDaemons.computeIfAbsent(room.getName(), (k) -> new ArrayList<>());

                GameRoomJoinResponse joinResponse = null;
                if (empty(roomDaemons)) {
                    // no rooms for template room, create first one
                    joinResponse = newRoomFromTemplate(room, player);

                } else {
                    // todo: strategy for adding players to room. perhaps based on ELO score?
                    for (GameDaemon daemon : roomDaemons) {
                        try {
                            if (daemon.getGameState().getPlayer(player.getId()) == null) {
                                joinResponse = addPlayerToRoom(daemon, player);
                            }
                        } catch (Exception e) {
                            log.warn("addPlayer: error adding to room: " + daemon.getRoom().getName() + ": " + e, e);
                        }
                    }
                    if (joinResponse == null) {
                        // all existing rooms full, create a new room based on template
                        // todo: enforce max total rooms?
                        joinResponse = newRoomFromTemplate(room, player);
                    }
                }

                roomDaemons.add(getGameDaemon(joinResponse.getRoom()));
                return joinResponse;
            }

        } else {
            return addPlayerToRoom(room, player);
        }
    }

    private GameRoomJoinResponse newRoomFromTemplate(GameRoom room, GamePlayer player) {
        GameRoom newRoom = new GameRoom(room);
        newRoom.setName(room.getName()+"_"+RandomStringUtils.randomAlphanumeric(12));
        newRoom.setAccountOwner(player.getId());
        newRoom.setTemplate(false);
        newRoom = gameRoomDAO.create(newRoom);
        return addPlayerToRoom(newRoom, player);
    }

    private GameRoomJoinResponse addPlayerToRoom(GameRoom room, GamePlayer player) {
        GameDaemon gameDaemon = getGameDaemon(room.getName(), false);
        if (gameDaemon == null) {
            gameDaemon = newRoom(room);
        }
        return addPlayerToRoom(gameDaemon, player);
    }

    private GameRoomJoinResponse addPlayerToRoom(GameDaemon daemon, GamePlayer player) {

        final String roomName = daemon.getRoom().getName();
        if (daemon.addPlayer(player)) {
            final String apiKey = player.getApiToken();
            getSessions().set(apiKey, json(player));
            getSessions().sadd(getRoomSessionKey(roomName, player.getId()), apiKey);
        }
        return new GameRoomJoinResponse(roomName, player);
    }

    protected String getRoomSessionKey(String roomName, String uuid) {
        return uuid+"_"+sha256_hex(roomName);
    }

    public GamePlayer findPlayer(String roomName, GamePlayer player) {
        final GameDaemon daemon = getGameDaemon(roomName, false);
        return daemon == null ? null : daemon.findPlayer(player);
    }
    public GamePlayer findPlayer(String roomName, String uuid) {
        final GameDaemon daemon = getGameDaemon(roomName, false);
        return daemon == null ? null : daemon.findPlayer(uuid);
    }

    public GameStateChange removePlayer(String roomName, String apiKey, String uuid) {
        getSessionPlayer(apiKey, uuid);

        final GameDaemon daemon = getGameDaemon(roomName, false);
        final GameStateChange change;
        if (daemon != null) {
            change = daemon.removePlayer(uuid);
        } else {
            change = null;
        }

        final String roomSessionKey = getRoomSessionKey(roomName, uuid);
        String key = getSessions().spop(roomSessionKey);
        while (key != null) {
            getSessions().del(key);
            key = getSessions().spop(roomSessionKey);
        }
        getSessions().del(apiKey); // just in case
        return change;
    }

    public GameState getGameState(String roomName) {
        return getGameDaemon(roomName).getGameState();
    }

    public GameStateChange playWord(String roomName, String apiKey, GamePlayer player, String word, PlayedTile[] tiles) {

        getSessionPlayer(apiKey, player.getId());

        final GameDaemon daemon = getGameDaemon(roomName, false);
        return daemon != null ? daemon.playWord(player, word, tiles) : null;
    }

    public GamePlayer getSessionPlayer(GamePlayer player) {
        final String apiKey = player.getApiToken();
        final String id = player.getId();
        return getSessionPlayer(apiKey, id);
    }

    public GamePlayer getSessionPlayer(String apiKey, String uuid) {
        final String sessionJson = getSessions().get(apiKey);
        if (sessionJson == null) throw notFoundEx(uuid);

        final GamePlayer session = json(sessionJson, GamePlayer.class);
        if (!session.getId().equals(uuid)) throw notFoundEx(uuid);
        return session;
    }

    public GameRoom findRoom (String roomName) {
        final GameDaemon daemon = getGameDaemon(roomName, false);
        return daemon != null ? daemon.getRoom() : null;
    }

}
