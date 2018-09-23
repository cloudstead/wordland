package wordland.resources;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.wizard.resources.NamedSystemResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import wordland.dao.SymbolFontDAO;
import wordland.model.SymbolFont;

import javax.ws.rs.Path;

import static wordland.ApiConstants.SYMBOL_FONTS_ENDPOINT;

@Path(SYMBOL_FONTS_ENDPOINT)
@Service @Slf4j
public class SymbolFontsResource extends NamedSystemResource<SymbolFont> {

    @Getter @Autowired private SymbolFontDAO dao;

}
