package wordland.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import wordland.model.GameRoom;
import wordland.model.game.GameDaemon;
import wordland.server.WordlandConfiguration;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;

@Service @Slf4j
public class GameStateService {

    @Autowired private WordlandConfiguration configuration;

    public Map<String, GameDaemon> rooms = new ConcurrentHashMap<>();

    public void newRoom(GameRoom room) {
        if (rooms.containsKey(room.getName())) die("newRoom: already exists");
        final GameDaemon gameDaemon = configuration.autowire(new GameDaemon(room.getName()));
        rooms.put(room.getName(), gameDaemon);
        gameDaemon.startGame(room.randomizeTiles());
    }

}
