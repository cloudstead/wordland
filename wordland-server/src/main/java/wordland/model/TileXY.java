package wordland.model;

public interface TileXY {

    int getX();
    TileXY setX(int x);

    int getY();
    TileXY setY(int y);

    default boolean sameXY (TileXY other) { return getX() == other.getX() && getY() == other.getY(); }

}
