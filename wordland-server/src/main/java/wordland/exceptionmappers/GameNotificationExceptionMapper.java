package wordland.exceptionmappers;

import org.cobbzilla.wizard.validation.ConstraintViolationBean;
import wordland.model.game.GameNotificationException;
import wordland.model.support.GameNotification;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import static org.cobbzilla.util.http.HttpStatusCodes.UNPROCESSABLE_ENTITY;
import static org.cobbzilla.util.json.JsonUtil.json;
import static org.cobbzilla.wizard.resources.ResourceUtil.status;

@Provider
public class GameNotificationExceptionMapper implements ExceptionMapper<GameNotificationException> {

    @Override public Response toResponse(GameNotificationException e) {
        final GameNotification n = e.getGameNotification();
        return status(UNPROCESSABLE_ENTITY, new ConstraintViolationBean[] {
                new ConstraintViolationBean(n.getMessageKey(), n.getMessage(), json(n.getParams()))
        });
    }

}
