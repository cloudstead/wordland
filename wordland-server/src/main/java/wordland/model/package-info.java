@TypeDefs({
    @TypeDef(name = SymbolDistributionSettings.JSONB_TYPE,
             typeClass = JSONBUserType.class,
             parameters = {@Parameter(name=JSONBUserType.PARAM_CLASS, value="wordland.model.json.SymbolDistributionSettings")}),
    @TypeDef(name = PointSystemSettings.JSONB_TYPE,
             typeClass = JSONBUserType.class,
             parameters = {@Parameter(name=JSONBUserType.PARAM_CLASS, value="wordland.model.json.PointSystemSettings")}),
    @TypeDef(name = GameBoardSettings.JSONB_TYPE,
             typeClass = JSONBUserType.class,
             parameters = {@Parameter(name=JSONBUserType.PARAM_CLASS, value="wordland.model.json.GameBoardSettings")}),
    @TypeDef(name = GameRoomSettings.JSONB_TYPE,
             typeClass = JSONBUserType.class,
             parameters = {@Parameter(name=JSONBUserType.PARAM_CLASS, value="wordland.model.json.GameRoomSettings")})
})

package wordland.model;

import org.cobbzilla.wizard.model.json.JSONBUserType;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;
import wordland.model.json.GameBoardSettings;
import wordland.model.json.GameRoomSettings;
import wordland.model.json.PointSystemSettings;
import wordland.model.json.SymbolDistributionSettings;