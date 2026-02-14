package mindustry.world;

import arc.math.*;

import static mindustry.Vars.*;

public class CliffLayerData{
    public static final int
    none = 0,
    top = 1,
    bottom = 2,
    left = 3,
    right = 4,
    topLeft = 5,
    topRight = 6,
    bottomLeft = 7,
    bottomRight = 8,
    topLeftToBottomRight = 9,
    topRightToBottomLeft = 10,
    maxType = 10;

    private static final int sideLeft = 0, sideRight = 1, sideUp = 2, sideDown = 3;
    private static final int cliffShift = 3;
    private static final int cliffMask = 0b11111 << cliffShift;

    public static int cliff(Tile tile){
        if(tile == null) return none;
        return cliff(tile.floorData);
    }

    public static int cliff(byte floorData){
        return ((floorData & 0xff) & cliffMask) >>> cliffShift;
    }

    public static byte withCliff(byte floorData, int cliffType){
        int value = floorData & 0xff;
        int encoded = Mathf.clamp(cliffType, none, maxType);
        value = (value & ~cliffMask) | ((encoded << cliffShift) & cliffMask);
        return (byte)value;
    }

    public static boolean blocks(Tile from, Tile to){
        if(from == null || to == null || from == to) return false;

        int dx = to.x - from.x, dy = to.y - from.y;
        int stepx = Mathf.clamp(dx, -1, 1);
        int stepy = Mathf.clamp(dy, -1, 1);
        int ax = Math.abs(dx), ay = Math.abs(dy);
        int steps = Math.max(ax, ay);

        if(steps <= 0) return false;

        Tile current = from;
        for(int i = 0; i < steps; i++){
            int nx = current.x + (i < ax ? stepx : 0);
            int ny = current.y + (i < ay ? stepy : 0);
            Tile next = world.tile(nx, ny);
            if(next == null) return false;
            if(blocksStep(current, next)) return true;
            current = next;
        }

        return false;
    }

    public static boolean blocks(int fromType, int toType, int dx, int dy){
        if(dx == 0 && dy == 0) return false;

        dx = Mathf.clamp(dx, -1, 1);
        dy = Mathf.clamp(dy, -1, 1);

        if(dx < 0 && (blocksSide(fromType, sideLeft) || blocksSide(toType, sideRight))) return true;
        if(dx > 0 && (blocksSide(fromType, sideRight) || blocksSide(toType, sideLeft))) return true;
        if(dy < 0 && (blocksSide(fromType, sideDown) || blocksSide(toType, sideUp))) return true;
        if(dy > 0 && (blocksSide(fromType, sideUp) || blocksSide(toType, sideDown))) return true;

        if(dx != 0 && dy != 0){
            if(blocksDiagonal(fromType, dx, dy) || blocksDiagonal(toType, -dx, -dy)) return true;
        }

        return false;
    }

    private static boolean blocksStep(Tile from, Tile to){
        int dx = to.x - from.x, dy = to.y - from.y;
        int fromType = cliff(from), toType = cliff(to);
        return blocks(fromType, toType, dx, dy);
    }

    private static boolean blocksSide(int type, int side){
        return switch(type){
            case top -> side == sideUp;
            case bottom -> side == sideDown;
            case left -> side == sideLeft;
            case right -> side == sideRight;
            case topLeft -> side == sideUp || side == sideLeft;
            case topRight -> side == sideUp || side == sideRight;
            case bottomLeft -> side == sideDown || side == sideLeft;
            case bottomRight -> side == sideDown || side == sideRight;
            case topLeftToBottomRight -> side == sideUp || side == sideLeft;
            case topRightToBottomLeft -> side == sideUp || side == sideRight;
            default -> false;
        };
    }

    private static boolean blocksDiagonal(int type, int dx, int dy){
        return switch(type){
            case topLeft -> dx < 0 && dy > 0;
            case topRight -> dx > 0 && dy > 0;
            case bottomLeft -> dx < 0 && dy < 0;
            case bottomRight -> dx > 0 && dy < 0;
            case topLeftToBottomRight -> dx == dy;
            case topRightToBottomLeft -> dx == -dy;
            default -> false;
        };
    }
}
