package wordland.model;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum TurnPolicyType {

    periodic_limit, absolute_limit;

    @JsonCreator public static TurnPolicyType fromString (String val) { return valueOf(val.toLowerCase()); }

}
