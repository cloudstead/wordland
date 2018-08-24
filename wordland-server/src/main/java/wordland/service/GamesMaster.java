package wordland.service;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.wizard.cache.redis.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import wordland.dao.*;
import wordland.model.*;
import wordland.model.game.GamePlayer;
import wordland.model.game.GameState;
import wordland.model.game.GameStateChange;
import wordland.model.json.GameRoomSettings;
import wordland.model.support.PlayedTile;
import wordland.server.WordlandConfiguration;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.cobbzilla.util.json.JsonUtil.json;
import static org.cobbzilla.util.security.ShaUtil.sha256_hex;
import static org.cobbzilla.wizard.model.StrongIdentifiableBase.newStrongUuid;
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

    @Autowired private RedisService redisService;

    @Getter(lazy=true) private final RedisService sessions = initSessions();
    private RedisService initSessions() { return redisService.prefixNamespace("gamesMaster/sessions/"); }

    @Getter @Setter private AtmosphereEventsService eventService;

    public Map<String, GameDaemon> rooms = new ConcurrentHashMap<>();

    public void newRoom(GameRoom room) {
        if (rooms.containsKey(room.getName())) throw invalidEx("err.room.alreadyExists");

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

        final GameDaemon gameDaemon = newGameDaemon(room);
        rooms.put(room.getName(), gameDaemon);
        gameDaemon.startGame();
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

    public void addPlayer(String roomName, GamePlayer player) {
        getGameDaemon(roomName).addPlayer(player);

        final String apiKey = newStrongUuid();
        getSessions().set(apiKey, json(player.setApiKey(apiKey)));
        getSessions().sadd(getRoomSessionKey(roomName, player.getId()), apiKey);
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

    public void removePlayer(String roomName, String apiKey, String uuid) {
        getSessionPlayer(apiKey, uuid);

        final GameDaemon daemon = getGameDaemon(roomName, false);
        if (daemon != null) daemon.removePlayer(uuid);

        final String roomSessionKey = getRoomSessionKey(roomName, uuid);
        String key = getSessions().spop(roomSessionKey);
        while (key != null) {
            getSessions().del(key);
            key = getSessions().spop(roomSessionKey);
        }
        getSessions().del(apiKey); // just in case
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
        final String apiKey = player.getApiKey();
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
