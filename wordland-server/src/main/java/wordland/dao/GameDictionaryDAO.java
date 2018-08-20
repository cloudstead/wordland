package wordland.dao;

import org.cobbzilla.wizard.dao.NamedIdentityBaseDAO;
import org.springframework.stereotype.Repository;
import wordland.model.GameDictionary;

@Repository
public class GameDictionaryDAO extends NamedIdentityBaseDAO<GameDictionary> {

    public GameDictionary findDefault() { return findByName("standard"); }

    public GameDictionary findBySymbolSetAndName(String symbolSet, String name) {
        return findByUniqueFields("symbolSet", symbolSet, "name", name);
    }

}
