package wordland.main;

import org.cobbzilla.wizard.main.MainApiOptionsBase;

import static wordland.ApiConstants.ENV_SUPERUSER;
import static wordland.ApiConstants.wordlandEnv;

public class WordlandApiOptionsBase extends MainApiOptionsBase {

    @Override protected String getPasswordEnvVarName() { return "WL_PASS"; }

    @Override public String getAccount() { return wordlandEnv().get(ENV_SUPERUSER); }

}
