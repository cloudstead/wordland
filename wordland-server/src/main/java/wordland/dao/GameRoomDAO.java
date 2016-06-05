package wordland.dao;

import org.cobbzilla.wizard.dao.NamedIdentityBaseDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import wordland.model.GameRoom;
import wordland.service.GamesMaster;

import javax.validation.Valid;

@Repository
public class GameRoomDAO extends NamedIdentityBaseDAO<GameRoom> {

    @Autowired private GamesMaster gamesMaster;

    @Override public Object preCreate(@Valid GameRoom room) {
        gamesMaster.newRoom(room);
        return super.preCreate(room);
    }

}
