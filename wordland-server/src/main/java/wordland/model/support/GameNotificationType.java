package wordland.model.support;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum GameNotificationType {

    invalid_word, sparse_word;

    @JsonCreator public static GameNotificationType create (String val) { return valueOf(val.toLowerCase()); }

}
