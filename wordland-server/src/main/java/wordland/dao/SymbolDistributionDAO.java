package wordland.dao;

import org.cobbzilla.wizard.dao.NamedIdentityBaseDAO;
import org.springframework.stereotype.Repository;
import wordland.model.SymbolDistribution;

@Repository
public class SymbolDistributionDAO extends NamedIdentityBaseDAO<SymbolDistribution> {

    public SymbolDistribution findBySymbolSetAndName(String symbolSet, String name) {
        return findByUniqueFields("symbolSet", symbolSet, "name", name);
    }

}
