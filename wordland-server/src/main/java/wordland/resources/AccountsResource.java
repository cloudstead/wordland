package wordland.resources;

import cloudos.resources.AuthResourceBase;
import com.sun.jersey.api.core.HttpContext;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.mail.service.TemplatedMailService;
import org.cobbzilla.wizard.auth.AuthResponse;
import org.cobbzilla.wizard.auth.AuthenticationException;
import org.cobbzilla.wizard.auth.LoginRequest;
import org.cobbzilla.wizard.validation.ValidationResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import wordland.auth.AccountAuthResponse;
import wordland.dao.AccountDAO;
import wordland.dao.SessionDAO;
import wordland.model.Account;
import wordland.model.support.AccountUpdateRequest;
import wordland.model.support.RegistrationRequest;
import wordland.server.WordlandConfiguration;

import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.HttpHeaders.USER_AGENT;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.cobbzilla.wizard.resources.ResourceUtil.*;
import static wordland.ApiConstants.*;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path(ACCOUNTS_ENDPOINT)
@Service @Slf4j
public class AccountsResource extends AuthResourceBase<Account> {

    @Autowired private WordlandConfiguration configuration;
    @Autowired private SessionDAO sessionDAO;
    @Autowired @Getter private AccountDAO accountDAO;
    @Autowired @Getter private TemplatedMailService templatedMailService;

    // only allow password resets via email
    @Override public Account findAccountForForgotPassword(String name) {
        return getAccountDAO().findByEmail(name);
    }

    /**
     * Get info about the currently logged in user
     * @param ctx session info
     * @return The Account object for the current user, or 404 Not Found if no user is logged in
     */
    @GET
    public Response me (@Context HttpContext ctx) {
        Account found = optionalUserPrincipal(ctx);
        if (found == null) return notFound();
        found = accountDAO.findByUuid(found.getUuid());
        return (found == null) ? notFound() : ok(found);
    }

    /**
     * Login. Upon successful login, this returns an AccountAuthResponse containing the session ID and account information.
     * If there is already a logged in user, a 422 error is returned. If the username/password does not match an existing user,
     * then a 404 error is returned.
     * @param ctx session info
     * @return Upon success, an AccountAuthResponse containing the session ID and account information
     */
    @POST @Path(EP_LOGIN)
    public Response login (@Context HttpContext ctx, LoginRequest request) {

        final Account alreadyLoggedIn = optionalUserPrincipal(ctx);
        if (alreadyLoggedIn != null && !alreadyLoggedIn.isAnonymous()) return invalid(ERR_ALREADY_LOGGED_IN);

        try {
            request.setUserAgent(ctx.getRequest().getHeaderValue(USER_AGENT));
            Account account = accountDAO.authenticate(request);
            return account != null ? ok(startSession(account)) : notFound(request.getName());

        } catch (AuthenticationException e) {
            log.warn("login: unexpected error: "+e, e);
            return notFound(request.getName());
        }
    }

    /**
     * Edit account details - only name, email, and password are editable
     * @param ctx session info
     * @param request Updates to the account
     * @return The updated account
     */
    @POST
    public Response updateAccount(@Context HttpContext ctx, @Valid AccountUpdateRequest request) {

        final Account sessionAccount = userPrincipal(ctx);
        final String accountUuid = sessionAccount.getUuid();
        final Account account = accountDAO.findByUuid(accountUuid);

        // Is this also a change password request? If so, try that first
        final ValidationResult validationResult = new ValidationResult();
        if (request.hasPassword()) {
            if (!account.getHashedPassword().isCorrectPassword(request.getCurrentPassword())) {
                validationResult.addViolation("err.password.incorrect", "Password was incorrect");
            } else {
                account.setPassword(request.getNewPassword());
            }
        }

        final Account withName = accountDAO.findByName(request.getName());
        if (withName != null && !withName.getUuid().equals(accountUuid)) {
            validationResult.addViolation("err.name.notUnique", "Name was not unique");
        }
        account.setName(request.getName());

        final Account withEmail = accountDAO.findByEmail(request.getEmail());
        if (withEmail != null && !withEmail.getUuid().equals(accountUuid)) {
            validationResult.addViolation("err.email.notUnique", "Email was not unique");
        }
        account.setEmail(request.getEmail());

        if (!validationResult.isEmpty()) return invalid(validationResult);

        // update email preferences
        if (request.getSubscribe() != null) account.setSubscriber(request.getSubscribe());

        final Account updated = accountDAO.update(account);
        sessionDAO.update(sessionAccount.getApiToken(), updated);

        return ok(updated);
    }

    /**
     * Register a new account. There are a few different scenarios for this endpoint:
     *  - If no user is logged in:
     *    - If the request has no login name (email) or password, an anonymous account is created
     *    - If the request includes a name+password, then a normal account is created
     *  - If the user has already logged in
     *    - If the currently logged-in account is non-anonymous, a 422 error is returned
     *    - If the currently logged-in account is anonymous
     *      - If this request includes a name+password, the anonymous account is updated to a normal account
     *      - If this request does not include a name+password, nothing happens and the same anonymous account is returned
     * @param ctx session info
     * @return Upon success, an AccountAuthResponse containing the session ID and account information
     */
    @POST @Path(EP_REGISTER)
    public Response register (@Context HttpContext ctx, @Valid RegistrationRequest request) {

        request.setUserAgent(ctx.getRequest().getHeaderValue(USER_AGENT));

        final Account alreadyLoggedIn = optionalUserPrincipal(ctx);
        final Account account;
        if (alreadyLoggedIn != null) {
            if (!alreadyLoggedIn.isAnonymous()) return invalid(ERR_ALREADY_LOGGED_IN);

            // already logged in, but this request has no name/password -- just start a new session
            if (request.isEmpty()) return ok(startSession(alreadyLoggedIn));

            // try to convert anonymous account into normal account
            return ok(startSession(accountDAO.registerAnonymous(alreadyLoggedIn, request)));
        }

        account = accountDAO.register(request);
        return ok(startSession(account != null ? account : accountDAO.anonymousAccount()));
    }

    private AuthResponse<Account> startSession(Account account) {
        return new AccountAuthResponse(sessionDAO.create(account), account);
    }

    @Override protected String getResetPasswordUrl(String token) { return configuration.getResetPasswordUrl(token); }

    @POST @Path(EP_REMOVE)
    public Response remove (@Context HttpContext ctx, @Valid RegistrationRequest request) {

        request.setUserAgent(ctx.getRequest().getHeaderValue(USER_AGENT));

        final Account byEmail = accountDAO.findByEmail(request.getEmail());
        if (byEmail == null) return notFound();

        // bypass cache on this lookup to ensure the account really exists, and to populate the HashedPassword field
        final Account byName = accountDAO.findByName(request.getUsername());
        if (byName == null || !byName.getUuid().equals(byEmail.getUuid())) return notFound();

        if (!byName.getHashedPassword().isCorrectPassword(request.getPassword())) return notFound();

        if (!configuration.getRecaptcha().verify(request.getCaptcha())) {
            log.warn("remove: captcha failed, returning invalid");
            return invalid(ERR_CAPTCHA_INCORRECT);
        }

        accountDAO.removeAccount(byName);

        return ok_empty();
    }

}
