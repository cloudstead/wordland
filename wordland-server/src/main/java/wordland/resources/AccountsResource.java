package wordland.resources;

import cloudos.resources.AuthResourceBase;
import com.sun.jersey.api.core.HttpContext;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.mail.service.TemplatedMailService;
import org.cobbzilla.wizard.validation.ValidationResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import wordland.dao.AccountDAO;
import wordland.dao.SessionDAO;
import wordland.model.Account;
import wordland.model.game.RoomState;
import wordland.model.support.AccountSession;
import wordland.model.support.AccountUpdateRequest;
import wordland.model.support.RegistrationRequest;
import wordland.server.WordlandConfiguration;
import wordland.service.GamesMaster;

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
    @Autowired @Getter private GamesMaster gamesMaster;

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
        AccountSession found = optionalUserPrincipal(ctx);
        if (found == null) return notFound();
        final Account account = accountDAO.findByUuid(found.getUuid());
        return (account == null) ? notFound() : ok(found);
    }

    @GET @Path("/rooms")
    public Response findRooms(@Context HttpContext ctx,
                              @QueryParam("state") RoomState state) {
        final AccountSession session = userPrincipal(ctx);
        return ok(gamesMaster.getRoomsForPlayer(session.getId(), state));
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
        sessionDAO.update(sessionAccount.getApiToken(), new AccountSession(updated));

        return ok(updated);
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
