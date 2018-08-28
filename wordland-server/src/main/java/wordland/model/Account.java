package wordland.model;

import cloudos.model.AccountBase;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.util.collection.ArrayUtil;
import org.cobbzilla.wizard.filters.auth.TokenPrincipal;

import javax.persistence.Entity;
import javax.persistence.Transient;

@Entity @NoArgsConstructor @Accessors(chain=true)
public class Account extends AccountBase implements TokenPrincipal {

    public static final String[] VALUE_FIELDS
            = {"email", "locale", "firstName", "lastName", "initials", "mobilePhoneCountryCode", "mobilePhone"};

    public static final String[] VALUE_FIELDS_ADMIN
            = ArrayUtil.append(VALUE_FIELDS, "lastLogin", "emailVerified", "suspended", "admin", "twoFactor");

    @Override public void beforeCreate() { if (!hasUuid()) initUuid(); }

    @Getter @Setter private boolean subscriber;

    // Set by WordlandAuthFilter
    @JsonIgnore @Transient @Getter private String apiToken;
    public void setApiToken(String token) { this.apiToken = token; }

}
