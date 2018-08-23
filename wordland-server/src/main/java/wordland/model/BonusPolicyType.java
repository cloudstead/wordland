package wordland.model;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum BonusPolicyType {

    forbidden, allowed, periodic_limit, absolute_limit;

    @JsonCreator public static BonusPolicyType fromString (String val) { return valueOf(val.toLowerCase()); }

}
