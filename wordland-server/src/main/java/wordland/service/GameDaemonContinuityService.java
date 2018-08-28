package wordland.service;

import lombok.Getter;
import org.cobbzilla.wizard.cache.redis.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import wordland.dao.GameRoomDAO;
import wordland.model.GameRoom;

import javax.annotation.PostConstruct;
import java.util.Set;

@Service
public class GameDaemonContinuityService {

    public static final String K_ROOMS = "rooms";

    @Autowired private GameRoomDAO gameRoomDAO;
    @Autowired private GamesMaster gamesMaster;
    @Autowired private RedisService redisService;

    @Getter(lazy=true) private final RedisService gameDaemonCache = initGameDaemonCache();
    private RedisService initGameDaemonCache() { return redisService.prefixNamespace("gameDaemons/"); }

    @PostConstruct
    public void restartIdleDaemons () {
        final Set<String> rooms = getGameDaemonCache().smembers(K_ROOMS);
        if (rooms != null) {
            for (String room : rooms) {
                final GameRoom gameRoom = gameRoomDAO.findByName(room);
                if (gameRoom != null) {
                    gamesMaster.newRoom(gameRoom);
                }
            }
        }
    }

    public void register(GameDaemon daemon) {
        final String roomName = daemon.getRoom().getName();
        if (!getGameDaemonCache().sismember(K_ROOMS, roomName)) {
            getGameDaemonCache().sadd(K_ROOMS, roomName);
        }
    }

    public void deregister(GameDaemon daemon) {
        getGameDaemonCache().srem(K_ROOMS, daemon.getRoom().getName());
    }

}
