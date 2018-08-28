package wordland.service;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.wizard.cache.redis.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import wordland.dao.GameRoomDAO;
import wordland.model.GameRoom;

import javax.annotation.PostConstruct;
import java.util.Set;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.cobbzilla.util.daemon.ZillaRuntime.daemon;
import static org.cobbzilla.util.system.Sleep.sleep;

@Service @Slf4j
public class GameDaemonContinuityService {

    public static final String K_ROOMS = "rooms";

    @Autowired private GameRoomDAO gameRoomDAO;
    @Autowired private GamesMaster gamesMaster;
    @Autowired private RedisService redisService;

    @Getter(lazy=true) private final RedisService gameDaemonCache = initGameDaemonCache();
    private RedisService initGameDaemonCache() { return redisService.prefixNamespace("gameDaemons/"); }

    @PostConstruct
    public void restartIdleDaemons () {
        daemon(() ->{
            sleep(SECONDS.toMillis(5), "waiting to restart idle GameDaemons");
            final Set<String> rooms = getGameDaemonCache().smembers(K_ROOMS);
            if (rooms != null) {
                for (String room : rooms) {
                    final GameRoom gameRoom = gameRoomDAO.findByName(room);
                    if (gameRoom != null) {
                        gamesMaster.newRoom(gameRoom);
                    }
                }
            }
            log.info("restartIdleDaemons: refreshed "+rooms.size()+" room daemons");
        });
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