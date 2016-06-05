package wordland.model.support;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

@NoArgsConstructor @Accessors(chain=true)
public class RegistrationRequest {

    @Getter @Setter private String email;
    @Getter @Setter private String password;

    @Getter @Setter private String username;
    @Getter @Setter private String mobilePhone;
    @Getter @Setter private Integer mobilePhoneCountryCode;

    @JsonIgnore @Getter @Setter private String userAgent;
    @JsonIgnore @Getter @Setter private String captcha;

    public boolean isEmpty() { return empty(email) && empty(password); }

}
