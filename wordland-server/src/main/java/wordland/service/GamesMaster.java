package wordland.service;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
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

        final String distributionName = roomSettings.defaultDistributionName();
        final SymbolDistribution distribution = distributionDAO.findBySymbolSetAndName(symbolSet.getName(), distributionName);
        if (distribution == null) throw notFoundEx(distributionName);
        roomSettings.setDefaultDistribution(distribution);

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

    public GameRoom findRoom (String roomName) {
        final GameDaemon daemon = getGameDaemon(roomName, false);
        return daemon != null ? daemon.getRoom() : null;
    }
}
