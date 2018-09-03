package wordland.model;

public interface TileXY {

    int getX();
    int getY();

    default boolean sameXY (TileXY other) { return getX() == other.getX() && getY() == other.getY(); }

}
