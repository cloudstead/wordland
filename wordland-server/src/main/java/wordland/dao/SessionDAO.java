package wordland.dao;

import org.cobbzilla.wizard.dao.AbstractSessionDAO;
import org.springframework.stereotype.Repository;
import wordland.model.Account;

@Repository public class SessionDAO extends AbstractSessionDAO<Account> {}
