package wordland.auth;

import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.wizard.filters.auth.AuthProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import wordland.dao.SessionDAO;
import wordland.model.support.AccountSession;

@Service @Slf4j
public class WordlandAuthProvider implements AuthProvider<AccountSession> {

    @Autowired private SessionDAO sessionDAO;

    @Override public AccountSession find(String uuid) {
        final AccountSession session = sessionDAO.find(uuid);
        if (session != null) sessionDAO.touch(uuid, session);
        return session;
    }

}
