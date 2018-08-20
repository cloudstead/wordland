package wordland.dao;

import org.cobbzilla.wizard.dao.NamedIdentityBaseDAO;
import org.springframework.stereotype.Repository;
import wordland.model.PointSystem;

@Repository
public class PointSystemDAO extends NamedIdentityBaseDAO<PointSystem> {

    public PointSystem findBySymbolSetAndName(String symbolSet, String name) {
        return findByUniqueFields("symbolSet", symbolSet, "name", name);
    }

}
