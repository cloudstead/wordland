package wordland.dao;

import lombok.Getter;
import org.cobbzilla.wizard.dao.NamedIdentityBaseDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import wordland.model.GameRoom;
import wordland.model.json.GameRoomSettings;
import wordland.service.GamesMaster;

import javax.validation.Valid;
import java.util.List;

import static wordland.ApiConstants.STANDARD;

@Repository
public class GameRoomDAO extends NamedIdentityBaseDAO<GameRoom> {

    @Autowired private GamesMaster gamesMaster;
    @Autowired @Getter private GameBoardDAO gameBoardDAO;
    @Autowired @Getter private SymbolSetDAO symbolSetDAO;
    @Autowired @Getter private SymbolDistributionDAO distributionDAO;
    @Autowired @Getter private PointSystemDAO pointSystemDAO;
    @Autowired @Getter private GameDictionaryDAO dictionaryDAO;

    @Override public Object preCreate(@Valid GameRoom room) {
        gamesMaster.newRoom(room);
        return super.preCreate(room);
    }

    public List<GameRoom> findTemplates() {
        return cacheLookup("findTemplates", o -> findByField("template", Boolean.TRUE));
    }

    public GameRoomSettings getDefaults() { return setDefaults(new GameRoomSettings()); }

    public GameRoomSettings setDefaults(GameRoomSettings rs) {
        if (rs.getSymbolSet() == null) {
            rs.setSymbolSet(symbolSetDAO.findByName(STANDARD));
        }
        if (rs.getSymbolDistribution() == null) {
            rs.setSymbolDistribution(distributionDAO.findByName(STANDARD));
        }
        if (rs.getPointSystem() == null) {
            rs.setPointSystem(pointSystemDAO.findByName(STANDARD));
        }
        if (rs.getDictionary() == null) {
            rs.setDictionary(dictionaryDAO.findByName(STANDARD));
        }
        if (rs.getBoard() == null) {
            rs.setBoard(gameBoardDAO.findByName(STANDARD));
        }
        return rs;
    }

}
