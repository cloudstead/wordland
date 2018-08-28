package wordland.dao;

import org.cobbzilla.wizard.dao.AbstractSessionDAO;
import org.springframework.stereotype.Repository;
import wordland.model.support.AccountSession;

@Repository public class SessionDAO extends AbstractSessionDAO<AccountSession> {}
