package wordland.resources;

import com.sun.jersey.api.core.HttpContext;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.wizard.auth.AuthenticationException;
import org.cobbzilla.wizard.auth.LoginRequest;
import org.cobbzilla.wizard.util.TestNames;
import org.cobbzilla.wizard.validation.ValidationResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import wordland.dao.AccountDAO;
import wordland.dao.SessionDAO;
import wordland.model.Account;
import wordland.model.support.AccountSession;
import wordland.model.support.RegistrationRequest;

import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.HttpHeaders.USER_AGENT;
import static org.cobbzilla.util.http.HttpContentTypes.APPLICATION_JSON;
import static org.cobbzilla.wizard.resources.ResourceUtil.*;
import static wordland.ApiConstants.*;

@Path(AUTH_ENDPOINT)
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Service @Slf4j
public class AuthResource {

    @Autowired private AccountDAO accountDAO;
    @Autowired private SessionDAO sessionDAO;

    @GET
    public Response currentSession (@Context HttpContext ctx) {
        final AccountSession session = optionalUserPrincipal(ctx);
        return session == null ? notFound() : ok(session);
    }

    /**
     * Login. Upon successful login, this returns an AccountSession containing the session ID and account information.
     * If there is already a logged in user, a 422 error is returned. If the username/password does not match an existing user,
     * then a 404 error is returned.
     * @param ctx session info
     * @return Upon success, an AccountSession containing the session ID and account information
     */
    @POST @Path(EP_LOGIN)
    public Response login (@Context HttpContext ctx, LoginRequest request) {

        final AccountSession alreadyLoggedIn = optionalUserPrincipal(ctx);
        if (alreadyLoggedIn != null) return ok(alreadyLoggedIn);

        if (request != null && request.hasName() && request.hasPassword()) {
            try {
                request.setUserAgent(ctx.getRequest().getHeaderValue(USER_AGENT));
                final Account account = accountDAO.authenticate(request);
                return account != null ? ok(startSession(new AccountSession(account))) : notFound(request.getName());

            } catch (AuthenticationException e) {
                log.warn("login: unexpected error: "+e, e);
                return notFound(request.getName());
            }

        } else {
            final String name = (request == null || !request.hasName()) ? anonymousUsername() : request.getName();
            // start guest session
            return ok(startSession(new AccountSession(name)));
        }
    }

    /**
     * Register a new account. There are a few different scenarios for this endpoint:
     *  - If no user is logged in
     *     - If the request includes a name+email+password, then a normal account is created
     *     - Otherwise the user is logged in to an anonymous account, using the username provided, or one will be generated
     *  - If the user has already logged in
     *    - If the currently logged-in account is non-anonymous, the current session is returned
     *    - If the currently logged-in account is anonymous
     *      - If this request includes a uuid+name+email+password, the anonymous account is updated to a normal account
     *      - Otherwise a validation error is returned
     * @param ctx session info
     * @return Upon success, an AccountSession containing the session ID and account information
     */
    @POST @Path(EP_REGISTER)
    public Response register (@Context HttpContext ctx, @Valid RegistrationRequest request) {

        request.setUserAgent(ctx.getRequest().getHeaderValue(USER_AGENT));

        final AccountSession alreadyLoggedIn = optionalUserPrincipal(ctx);
        final Account account;
        if (alreadyLoggedIn != null) {
            if (!alreadyLoggedIn.isAnonymous()) return ok(alreadyLoggedIn);

            final ValidationResult validation = validateRegistration(request, true);
            if (validation.isValid()) {
                // has username, email and password, try to convert anonymous account into normal account
                // update existing session to preserve session id
                final String apiKey = alreadyLoggedIn.getApiToken();
                final AccountSession session = new AccountSession(accountDAO.register(request));
                session.setApiToken(apiKey);
                sessionDAO.update(apiKey, session);
                return ok(session);
            }
        }

        final ValidationResult validation = validateRegistration(request, false);
        if (validation.isInvalid()) return login(ctx, new LoginRequest(request.getUsername(), null));

        account = accountDAO.register(request);
        return ok(startSession(new AccountSession(account)));
    }

    protected ValidationResult validateRegistration(RegistrationRequest request, boolean checkUuid) {
        final ValidationResult validation = new ValidationResult();
        if (!request.hasUsername()) validation.addViolation("err.username.empty");
        if (!request.hasEmail()) validation.addViolation("err.email.empty");
        if (!request.hasPassword()) validation.addViolation("err.password.empty");
        if (checkUuid && !request.hasId()) validation.addViolation("err.id.empty");
        return validation;
    }

    protected String anonymousUsername() { return TestNames.animal() + " " + TestNames.fruit(); }

    private AccountSession startSession(AccountSession session) {
        session.setApiToken(sessionDAO.create(session));
        return session;
    }
}
