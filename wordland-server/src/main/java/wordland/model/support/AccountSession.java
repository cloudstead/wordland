package wordland.model.support;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.cobbzilla.wizard.filters.auth.TokenPrincipal;
import org.cobbzilla.wizard.model.IdentifiableBase;
import wordland.model.Account;

import static org.cobbzilla.util.daemon.ZillaRuntime.uuid;

@NoArgsConstructor
public class AccountSession extends IdentifiableBase implements TokenPrincipal {

    @Getter @Setter private Account account;
    @Getter @Setter private String name;
    @Getter @Setter private String id;

    @Override public String getUuid() { return id; }

    @Getter @Setter private String apiToken;

    @JsonIgnore public boolean isAdmin() { return hasAccount() && account.isAdmin(); }

    public AccountSession(Account account) {
        this.account = account;
        this.name = account.getName();
        this.id = account.getUuid();
        this.apiToken = account.getApiToken();
    }

    public AccountSession(String name) {
        this.name = name;
        this.id = uuid();
    }

    public boolean hasAccount() { return account != null; }
    @JsonIgnore public boolean isAnonymous() { return account == null; }

}
