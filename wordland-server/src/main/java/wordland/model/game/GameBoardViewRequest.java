package wordland.model.game;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import wordland.model.json.GameBoardSettings;

import static wordland.ApiConstants.MAX_BOARD_VIEW;
import static wordland.model.game.GameState.MAX_BOARD_IMAGE_HEIGHT;
import static wordland.model.game.GameState.MAX_BOARD_IMAGE_WIDTH;
import static wordland.model.game.GameState.TILE_PIXEL_SIZE;

@NoArgsConstructor @Accessors(chain=true)
public class GameBoardViewRequest {

    @Getter @Setter public Integer x1;
    @Getter @Setter public Integer x2;
    @Getter @Setter public Integer y1;
    @Getter @Setter public Integer y2;

    public int tilesWidth() { return 1+x2-x1; }
    public int tilesHeight() { return 1+y2-y1; }

    @Getter @Setter public Integer imageWidth;
    public double imageWidthDouble() { return (double) imageWidth; }

    @Getter @Setter public Integer imageHeight;
    public double imageHeightDouble() { return (double) imageHeight; }

    @Getter @Setter public GameBoardPalette palette;

    @Getter @Setter public Boolean useCache;
    public boolean useCache () { return useCache == null || useCache; }

    @Getter @Setter public Boolean includeTimestamp;
    public boolean includeTimestamp () { return includeTimestamp != null && includeTimestamp; }

    public void initDefaults(GameBoardSettings settings) {

        if (x1 == null) x1 = 0;
        if (y1 == null) y1 = 0;
        if (x2 == null) x2 = x1 + Math.min(MAX_BOARD_VIEW, settings.hasWidth() ? settings.getWidth() : Integer.MAX_VALUE);
        if (y2 == null) y2 = y1 + Math.min(MAX_BOARD_VIEW, settings.hasWidth() ? settings.getWidth() : Integer.MAX_VALUE);

        if (imageWidth == null) {
            imageWidth = settings.hasLength()
                    ? Math.min(MAX_BOARD_IMAGE_WIDTH, settings.getLength()*TILE_PIXEL_SIZE)
                    : MAX_BOARD_IMAGE_WIDTH;
        }
        if (imageHeight == null) {
            imageHeight = settings.hasWidth()
                    ? Math.min(MAX_BOARD_IMAGE_HEIGHT, settings.getWidth()*TILE_PIXEL_SIZE)
                    : MAX_BOARD_IMAGE_HEIGHT;
        }

        // ensure the bounds we draw are not too large, and certainly not larger than the board view
        if (settings.hasWidth()) {
            // if settings has a width, X must always be >= 0
            if (x1 < 0) x1 = 0;

            // cannot ask for something bigger than MAX_BOARD_VIEW
            if (x2 - x1 >= MAX_BOARD_VIEW) x2 = x1 + MAX_BOARD_VIEW - 1;

            // cannot ask for something bigger than the board itself
            if (x2 > settings.getWidth()) x2 = settings.getWidth()-1;

        } else if (x2 - x1 >= MAX_BOARD_VIEW) {
            // infinite board width, still cannot view more than MAX_BOARD_VIEW
            x2 = x1 + MAX_BOARD_VIEW - 1;
        }
        if (settings.hasLength()) {
            // if settings has a length, Y must always be >= 0
            if (y1 < 0) y1 = 0;

            // cannot ask for something bigger than MAX_BOARD_VIEW
            if (y2 - y1 >= MAX_BOARD_VIEW) y2 = y1 + MAX_BOARD_VIEW - 1;

            // cannot ask for something bigger than the board itself
            if (y2 > settings.getLength()) y2 = settings.getLength()-1;

        } else if (y2 - y1 >= MAX_BOARD_VIEW) {
            // infinite board length, still cannot view more than MAX_BOARD_VIEW
            y2 = y1 + MAX_BOARD_VIEW - 1;
        }

        // ensure output image is within bounds
        if (imageWidth > MAX_BOARD_IMAGE_WIDTH) imageWidth = MAX_BOARD_IMAGE_WIDTH;
        if (imageHeight > MAX_BOARD_IMAGE_HEIGHT) imageHeight = MAX_BOARD_IMAGE_HEIGHT;
    }
}
