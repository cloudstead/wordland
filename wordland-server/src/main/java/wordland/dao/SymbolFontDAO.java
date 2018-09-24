package wordland.dao;

import lombok.Getter;
import org.apache.commons.codec.binary.Base64InputStream;
import org.cobbzilla.wizard.dao.NamedIdentityBaseDAO;
import org.springframework.stereotype.Repository;
import wordland.model.SymbolFont;

import javax.annotation.PostConstruct;
import java.awt.*;
import java.io.ByteArrayInputStream;
import java.util.*;
import java.util.List;

import static java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment;
import static org.cobbzilla.util.daemon.ZillaRuntime.die;

@Repository
public class SymbolFontDAO extends NamedIdentityBaseDAO<SymbolFont> {

    @Getter(lazy=true) private final Map<String, SymbolFont> allFonts = initAllFonts();
    private Map<String, SymbolFont> initAllFonts() { return initSystemFonts(); }

    @Getter(lazy=true) private final Map<String, SymbolFont> systemFonts = initSystemFonts();
    protected Map<String, SymbolFont> initSystemFonts() {
        final Map<String, SymbolFont> fonts = new HashMap<>();
        final GraphicsEnvironment e = getLocalGraphicsEnvironment();
        for (Font font : e.getAllFonts()) {
            fonts.put(font.getName(), systemFont(font));
        }
        return fonts;
    }

    protected SymbolFont systemFont(Font font) {
        return (SymbolFont) new SymbolFont()
                .setSystem(true)
                .setFont(font)
                .setName(font.getName());
    }

    public SymbolFont findSystemFont (String name) { return getSystemFonts().get(name); }

    @Override public List<SymbolFont> findAll() {
        final Set<SymbolFont> fonts = new HashSet<>(super.findAll());
        fonts.addAll(findSystemFonts());
        return new ArrayList<>(fonts);
    }

    public List<SymbolFont> findSystemFonts () { return new ArrayList<>(getSystemFonts().values()); }

    @PostConstruct public void initFonts () { for (SymbolFont f : findAll()) initFont(f); }

    @Override public SymbolFont findByName(String name) {
        final SymbolFont font = super.findByName(name);
        return font != null ? font : findSystemFont(name);
    }

    @Override public SymbolFont postCreate(SymbolFont symbolFont, Object context) {
        initFont(symbolFont);
        return super.postCreate(symbolFont, context);
    }

    @Override public SymbolFont postUpdate(SymbolFont symbolFont, Object context) {
        initFont(symbolFont);
        return super.postUpdate(symbolFont, context);
    }

    public void initFont(SymbolFont symbolFont) {
        if (symbolFont.isSystem()) return;
        try {
            final Font font = Font.createFont(Font.TRUETYPE_FONT, new Base64InputStream(new ByteArrayInputStream(symbolFont.getBase64ttf().getBytes())));
            final GraphicsEnvironment e = getLocalGraphicsEnvironment();
            e.registerFont(font);
            symbolFont.setFont(font);
            getAllFonts().put(symbolFont.getName(), symbolFont);

        } catch (Exception e) {
            die("initFont: "+e, e);
        }
    }
}
