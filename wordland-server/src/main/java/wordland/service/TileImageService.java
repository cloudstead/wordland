package wordland.service;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.string.Base64;
import org.cobbzilla.wizard.cache.redis.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import wordland.image.TileImage;
import wordland.image.TileImageSettings;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

@Service @Slf4j
public class TileImageService {

    @Autowired private RedisService redis;
    @Getter(lazy=true) private final RedisService tileCache = initTileCache();
    private RedisService initTileCache() { return redis.prefixNamespace(getClass().getSimpleName()); }

    public InputStream png (TileImageSettings settings) {

        final RedisService cache = getTileCache();
        final String cacheKey = "_"+settings.hashCode();
        final String b64image = cache.get(cacheKey);

        if (b64image != null) {
            try {
                return new ByteArrayInputStream(Base64.decode(b64image));
            } catch (Exception e) {
                log.warn("png: error reading image data from cache, removing from cache: "+e);
                cache.del(cacheKey);
            }
        }

        final TileImage tile = new TileImage(settings);
        final byte[] png = tile.png();
        cache.set(cacheKey, Base64.encodeBytes(png));
        return new ByteArrayInputStream(png);
    }

}
