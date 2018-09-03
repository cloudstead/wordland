package wordland.bot;

import lombok.Getter;
import lombok.Setter;
import org.cobbzilla.wizard.client.ApiClientBase;
import org.cobbzilla.wizard.util.RestResponse;
import wordland.model.game.*;
import wordland.model.support.GameRuntimeEvent;

import java.util.List;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.json.JsonUtil.json;
import static wordland.ApiConstants.EP_PLAY;
import static wordland.ApiConstants.GAME_ROOMS_ENDPOINT;
import static wordland.model.game.TileFunctions.MATCH_UNCLAIMED;
import static wordland.model.game.TileFunctions.countUnclaimed;
import static wordland.model.game.TileFunctions.forEachTile;

public class Pianola implements GameTileReducer<PianolaPlay>, GameTileAccumulator<PianolaPlay, Integer> {

    private final ApiClientBase api;
    private final String roomName;
    private final GameTileState[][] tiles;
    private final List<GamePlayer> players;
    private int currentPlayer = 0;

    @Getter @Setter private PianolaBrain brain;
    @Getter @Setter private GameTileMatcher match = MATCH_UNCLAIMED;

    public Pianola(ApiClientBase api, String roomName, GameTileState[][] tiles, List<GamePlayer> players) {
        this.api = api;
        this.roomName = roomName;
        this.tiles = tiles;
        this.players = players;
    }

    public boolean gameOver() { return countUnclaimed(tiles) == 0; }

    @Override public PianolaPlay apply(GameTileState[][] tiles, int x, int y) throws Exception {
        final GamePlayer player = players.get(currentPlayer % players.size());
        currentPlayer++;
        return brain.play(player, tiles, x, y);
    }

    @Override public void add(GameTileState[][] tiles, int x, int y, PianolaPlay play) throws Exception {
        // play the word
        final GameRuntimeEvent event = play.initEvent();
        try {
            api.pushToken(play.getPlayer().getApiToken());
            final RestResponse restResponse = api.doPost(GAME_ROOMS_ENDPOINT + "/" + roomName + "/" + EP_PLAY, json(event));
            if (!restResponse.isSuccess()) {
                die("add: error playing word, request=" + json(event) + ", response=" + restResponse);
            }
        } finally {
            api.popToken();
        }
    }

    public void play(GameTileState[][] tiles) {
        forEachTile(tiles, new TileMapReduce<PianolaPlay, Integer>()
                .setMatch(match)
                .setReducer(this)
                .setAccumulator(this));
    }

}
