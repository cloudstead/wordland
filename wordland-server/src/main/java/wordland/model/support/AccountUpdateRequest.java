package wordland.model.support;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.wizard.validation.HasValue;
import org.hibernate.validator.constraints.Email;

import javax.validation.constraints.Size;

import static cloudos.model.AccountBase.*;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.wizard.model.UniquelyNamedEntity.NAME_MAXLEN;

@NoArgsConstructor @AllArgsConstructor @Accessors(chain=true)
public class AccountUpdateRequest {

    @HasValue(message="err.name.empty")
    @Size(min=2, max=NAME_MAXLEN, message="err.name.length")
    @Getter @Setter private String name;

    @Email(message=ERR_EMAIL_INVALID)
    @HasValue(message=ERR_EMAIL_EMPTY)
    @Size(max=EMAIL_MAXLEN, message=ERR_EMAIL_LENGTH)
    @Getter @Setter private String email;

    @Getter @Setter private String currentPassword;
    @Getter @Setter private String newPassword;
    public boolean hasPassword () { return !empty(currentPassword) && !empty(newPassword); }

    @Getter @Setter public Boolean subscribe;

}
