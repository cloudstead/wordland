package wordland.bot.strategy;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.daemon.SimpleDaemon;
import wordland.bot.PianolaBot;
import wordland.bot.PianolaBotStrategy;
import wordland.dao.GameDictionaryDAO;
import wordland.model.GameDictionary;
import wordland.model.GameRoom;
import wordland.model.game.*;
import wordland.model.json.GameBoardSettings;
import wordland.model.json.GameRoomSettings;
import wordland.model.support.GameRuntimeEvent;
import wordland.model.support.PlayedTile;
import wordland.service.GameDaemon;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.json.JsonUtil.json;

@AllArgsConstructor @Slf4j
public class PianolaBotBase extends SimpleDaemon implements PianolaBotStrategy {

    private PianolaBot pianolaBot;

    @Override protected long getSleepTime() { return SECONDS.toMillis(2); }

    @Override protected void process() {
        final GameDaemon daemon = pianolaBot.getGameDaemon();
        final GameState gameState = daemon.getGameState();
        final GameStateStorageService stateStorage = gameState.getStateStorage();

        // is the game over?
        final RoomState roomState = stateStorage.getRoomState();
        if (roomState == null) {
            log.error("process: no stateStorage for room "+getRoom().getName()+", exiting");
            stop();
            return;
        }
        if (roomState == RoomState.ended) {
            log.error("process: game ended in room "+getRoom().getName()+", exiting");
            stop();
            return;
        }

        // is it our turn?
        final String currentPlayerId = stateStorage.getCurrentPlayerId();
        if (myPlayerId().equals(currentPlayerId)) {
            // get a view of the board, where was the most recent play?
            final GameBoardState board;
            final List<GameStateChange> history = stateStorage.getHistory(GameStateChangeType.word_played);
            final List<String> allWords = history.stream()
                    .map(h -> json(h.getObject(), GameRuntimeEvent.class).getWord())
                    .collect(Collectors.toList());
            final List<String> opponentWords = history.stream()
                    .filter(h -> !json(h.getObject(), GameRuntimeEvent.class).getId().equals(myPlayerId()))
                    .map(h -> json(h.getObject(), GameRuntimeEvent.class).getWord())
                    .collect(Collectors.toList());
            final GameTileStateExtended tile;
            if (empty(history)) {
                // we must have first play?
                board = gameState.getBoard(0, Math.min(10, boardSettings().getWidth()), 0, Math.min(10, boardSettings().getLength()));
                tile = new GameTileStateExtended(board.getTiles()[0][0], 0, 0);
            } else {
                final GameStateChange change = history.get(history.size() - 1);
                final GameRuntimeEvent event = json(change.getObject(), GameRuntimeEvent.class);
                final PlayedTile playedTile = event.getTiles()[0];
                board = gameState.getBoard(playedTile.getX(), Math.min(10, boardSettings().getWidth()), playedTile.getY(), Math.min(10, boardSettings().getLength()));
                final PlayedTile playedOnBoard = board.fromRelativeTile(playedTile);
                tile = new GameTileStateExtended(board.getTiles()[playedTile.getX()][playedTile.getY()], playedOnBoard.getX(), playedOnBoard.getY());
            }

            final GameDictionaryDAO dictionaryDAO = pianolaBot.getConfiguration().getBean(GameDictionaryDAO.class);
            final String dictName = roomSettings().getDictionary().getName();
            GameDictionary dictionary = dictionaryDAO.findByName(dictName);
            if (dictionary == null) {
                log.warn("process: dictionary not found ("+dictName+"), using default");
                dictionary = dictionaryDAO.findDefault();
            }

            findWord(daemon, board, allWords, opponentWords, tile, dictionary);
        }
    }

    protected void findWord(GameDaemon daemon,
                            GameBoardState board,
                            List<String> allWords,
                            List<String> opponentWords,
                            GameTileStateExtended tile,
                            GameDictionary dictionary) {
        tile.findWord(board, dictionary, new HashSet<>(allWords),
                event -> daemon.playWord(pianolaBot.getPlayer(), event.getWord(), event.getTiles()));
    }

    private String myPlayerId() { return pianolaBot.getPlayer().getId(); }

    private GameRoom getRoom() { return pianolaBot.getGameDaemon().getRoom(); }

    private GameRoomSettings roomSettings() { return getRoom().getSettings(); }

    private GameBoardSettings boardSettings() { return roomSettings().getBoard().getSettings(); }

}
