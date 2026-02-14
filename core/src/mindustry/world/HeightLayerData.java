package mindustry.world;

import arc.math.*;
import mindustry.core.World;

import static mindustry.Vars.*;

public class HeightLayerData{
    public static final int defaultLayer = 1;
    public static final int minLayer = 1;
    public static final int maxLayer = 4;

    private static final int layerMask = 0b11;
    private static final int slopeMask = 0b100;

    public static int layer(Tile tile){
        if(tile == null) return defaultLayer;
        return layer(tile.floorData);
    }

    public static int layer(byte floorData){
        int value = floorData & 0xff;
        return Mathf.clamp((value & layerMask) + 1, minLayer, maxLayer);
    }

    public static boolean slope(Tile tile){
        return tile != null && slope(tile.floorData);
    }

    public static boolean slope(byte floorData){
        return ((floorData & 0xff) & slopeMask) != 0;
    }

    /** Effective fog layer used when this tile is being revealed as a target. */
    public static int fogLayer(Tile tile){
        int layer = layer(tile);
        return slope(tile) ? Math.max(minLayer, layer - 1) : layer;
    }

    /** Returns the highest layer touched by the edge of an axis-aligned collision box. */
    public static int edgeLayer(float worldX, float worldY, float halfSize){
        if(world.width() <= 0 || world.height() <= 0) return defaultLayer;
        if(halfSize <= 0f) return layer(world.tileWorld(worldX, worldY));

        float epsilon = 0.001f;
        int minx = Mathf.clamp(World.toTile(worldX - halfSize - epsilon), 0, world.width() - 1);
        int maxx = Mathf.clamp(World.toTile(worldX + halfSize + epsilon), 0, world.width() - 1);
        int miny = Mathf.clamp(World.toTile(worldY - halfSize - epsilon), 0, world.height() - 1);
        int maxy = Mathf.clamp(World.toTile(worldY + halfSize + epsilon), 0, world.height() - 1);

        int result = minLayer;

        for(int x = minx; x <= maxx; x++){
            result = Math.max(result, layer(world.tile(x, miny)));
            result = Math.max(result, layer(world.tile(x, maxy)));
            if(result >= maxLayer) return maxLayer;
        }

        for(int y = miny + 1; y <= maxy - 1; y++){
            result = Math.max(result, layer(world.tile(minx, y)));
            result = Math.max(result, layer(world.tile(maxx, y)));
            if(result >= maxLayer) return maxLayer;
        }

        return result;
    }

    public static byte withLayer(byte floorData, int layer){
        int value = floorData & 0xff;
        int encoded = Mathf.clamp(layer, minLayer, maxLayer) - 1;
        value = (value & ~layerMask) | (encoded & layerMask);
        return (byte)value;
    }

    public static byte withSlope(byte floorData, boolean slope){
        int value = floorData & 0xff;
        value = slope ? (value | slopeMask) : (value & ~slopeMask);
        return (byte)value;
    }

    public static byte compose(int layer, boolean slope){
        return withSlope(withLayer((byte)0, layer), slope);
    }
}
