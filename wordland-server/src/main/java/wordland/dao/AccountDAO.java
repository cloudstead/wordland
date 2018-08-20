package wordland.dao;

import cloudos.dao.AccountBaseDAOBase;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.mail.SimpleEmailMessage;
import org.cobbzilla.mail.TemplatedMail;
import org.cobbzilla.mail.service.TemplatedMailService;
import org.cobbzilla.util.reflect.ReflectionUtil;
import org.cobbzilla.wizard.auth.AuthenticationException;
import org.cobbzilla.wizard.auth.LoginRequest;
import org.cobbzilla.wizard.model.HashedPassword;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import wordland.ApiConstants;
import wordland.model.Account;
import wordland.model.support.RegistrationRequest;
import wordland.server.WordlandConfiguration;

import static cloudos.model.AccountBase.canonicalizeEmail;
import static org.cobbzilla.mail.service.TemplatedMailService.PARAM_ACCOUNT;
import static org.cobbzilla.mail.service.TemplatedMailService.T_WELCOME;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.wizard.resources.ResourceUtil.invalidEx;
import static org.hibernate.criterion.Restrictions.eq;
import static org.hibernate.criterion.Restrictions.or;
import static wordland.ApiConstants.anonymousEmail;

@Repository @Slf4j
public class AccountDAO extends AccountBaseDAOBase<Account> {

    @Autowired private WordlandConfiguration configuration;
    @Autowired private TemplatedMailService mailService;

    public boolean adminsExist() { return !findByField("admin", true).isEmpty(); }

    public Account findByEmail (String email) { return findByUniqueField("canonicalEmail", canonicalizeEmail(email)); }

    public Account findByUuidOrEmail(String id) {
        String email = null;
        try { email = canonicalizeEmail(id); } catch (Exception ignored) {/*noop, not an email address*/}
        return email == null
                ? findByUuid(id)
                : uniqueResult(criteria().add(or(eq("uuid", id), eq("canonicalEmail", email))));
    }

    @Override public Account authenticate(LoginRequest login) throws AuthenticationException {
        final String name = login.getName();
        final Account account = findByEmail(name);

//        audit.log(login, "authenticate", "starting for '"+name+"'");
        if (empty(name) || account == null) {
//            audit.log(login, "authenticate", "no account found: '"+ name + "', return null");
            return null;
        }

        if (account.getHashedPassword().isCorrectPassword(login.getPassword())) {
//            audit.log(login, "authenticate", "successful password login for " + name);
            account.setLastLogin();
            update(account);
            return account;
        }
//        audit.log(login, "authenticate", "unsuccessful login for "+ name+", return null");
        return null;
    }

    public Account register(RegistrationRequest request) {
        final String email = request.getEmail();
        final String password = request.getPassword();

//        audit.log(request, "register", "starting for '"+name+"'");

        Account account = findByEmail(email);
        if (account != null) {
//            audit.log(request, "register", "email exists ("+email+"), returning error");
            throw invalidEx("err.email.notUnique", "Email was not unique");
        }

//        audit.log(request, "register", "creating account for: '"+ name + "'");

        Account newAccount = (Account) new Account().setHashedPassword(new HashedPassword(password));
        ReflectionUtil.copy(newAccount, request, Account.VALUE_FIELDS);
        newAccount.initEmailVerificationCode();
        newAccount.setLastLogin();

        newAccount = create(newAccount);
        sendWelcomeEmail(newAccount);

        return newAccount;
    }

    private void sendWelcomeEmail(Account account) {
        final SimpleEmailMessage welcomeSender = configuration.getEmailSenderNames().get(T_WELCOME);
        final String code = account.initEmailVerificationCode();
        final TemplatedMail mail = new TemplatedMail()
                .setToEmail(account.getEmail())
                .setToName(account.getName())
                .setFromName(welcomeSender.getFromName())
                .setFromEmail(welcomeSender.getFromEmail())
                .setTemplateName(T_WELCOME)
                .setParameter(PARAM_ACCOUNT, account)
                .setParameter("activationUrl", configuration.getPublicUriBase() + "/#/activate/" + code);
        try {
            mailService.getMailSender().deliverMessage(mail);
        } catch (Exception e) {
            log.error("sendWelcomeEmail: Error sending email: "+e, e);
        }
    }

    public Account registerAnonymous(Account account, RegistrationRequest request) {

        final String email = request.getEmail();
        final String name = request.getUsername();
        final String password = request.getPassword();

        if (empty(name) || empty(password)) {
            return null;
        }

        final Account withName = findByName(name);
        if (withName != null) {
            throw invalidEx("err.name.notUnique", "Name was not unique");
        }

        final Account withEmail = findByEmail(request.getEmail());
        if (withEmail != null) {
            throw invalidEx("err.email.notUnique", "Email was not unique");
        }

        final Account anonAccount = findByUuid(account.getUuid());

        if (!anonAccount.isAnonymous()) {
            throw invalidEx("err.email.notAnonymous", "Account was not anonymous, cannot convert to regular account");
        }

        if (!configuration.getRecaptcha().verify(request.getCaptcha())) {
            log.warn("register: captcha failed, returning invalid");
            throw invalidEx(ApiConstants.ERR_CAPTCHA_INCORRECT);
        }

        anonAccount.setName(name);
        anonAccount.setEmail(email);
        anonAccount.setPassword(request.getPassword());
        anonAccount.setAnonymous(false);
        anonAccount.initEmailVerificationCode();

        final Account updated = update(anonAccount);
        sendWelcomeEmail(updated);

        return updated;
    }

    public Account anonymousAccount() {
        final Account account = new Account();
        account.setEmail(anonymousEmail());
        account.setAnonymous(true);
        return create(account);
    }

    public void removeAccount(Account account) {
        // todo: delete everything owned by the account
    }

}
