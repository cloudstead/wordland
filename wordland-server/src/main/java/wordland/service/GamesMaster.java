package wordland.service;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import wordland.model.GameRoom;
import wordland.model.game.GamePlayer;
import wordland.model.game.GameState;
import wordland.model.game.GameStateChange;
import wordland.model.support.PlayedTile;
import wordland.server.WordlandConfiguration;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.cobbzilla.wizard.resources.ResourceUtil.invalidEx;
import static org.cobbzilla.wizard.resources.ResourceUtil.notFoundEx;

@Service @Slf4j @Accessors(chain=true)
public class GamesMaster {

    @Autowired private WordlandConfiguration configuration;

    @Getter @Setter private AtmosphereEventsService eventService;

    public Map<String, GameDaemon> rooms = new ConcurrentHashMap<>();

    public void newRoom(GameRoom room) {
        if (rooms.containsKey(room.getName())) throw invalidEx("err.room.alreadyExists");
        final GameDaemon gameDaemon = newGameDaemon(room);
        rooms.put(room.getName(), gameDaemon);
        gameDaemon.startGame(room.randomizeTiles());
    }

    public void initRoom(GameRoom room) {
        if (!rooms.containsKey(room.getName())) {
            final GameDaemon gameDaemon = newGameDaemon(room);
            rooms.put(room.getName(), gameDaemon);
            boolean restored = false;
            try {
                restored = gameDaemon.restore();
            } catch (Exception e) {
                log.warn("initRoom: error restoring (starting fresh instead): "+e, e);
            }
            if (!restored) gameDaemon.startGame(room.randomizeTiles());
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

    public void addPlayer(String roomName, GamePlayer player) {
        getGameDaemon(roomName).addPlayer(player);
    }

    public GamePlayer findPlayer(String roomName, GamePlayer player) {
        final GameDaemon daemon = getGameDaemon(roomName, false);
        return daemon == null ? null : daemon.findPlayer(player);
    }
    public GamePlayer findPlayer(String roomName, String uuid) {
        final GameDaemon daemon = getGameDaemon(roomName, false);
        return daemon == null ? null : daemon.findPlayer(uuid);
    }

    public void removePlayer(String roomName, String uuid) {
        final GameDaemon daemon = getGameDaemon(roomName, false);
        if (daemon != null) daemon.removePlayer(uuid);
    }

    public GameState getGameState(String roomName) {
        return getGameDaemon(roomName).getGameState();
    }

    public GameStateChange playWord(String roomName, GamePlayer player, String word, PlayedTile[] tiles) {
        final GameDaemon daemon = getGameDaemon(roomName, false);
        return daemon != null ? daemon.playWord(player, word, tiles) : null;
    }

    public boolean isValidWord(String roomName, String word) {
        final GameDaemon daemon = getGameDaemon(roomName, false);
        return daemon != null && daemon.isValidWord(word);
    }

    public GameRoom findRoom (String roomName) {
        final GameDaemon daemon = getGameDaemon(roomName, false);
        return daemon != null ? daemon.getRoom() : null;
    }
}
