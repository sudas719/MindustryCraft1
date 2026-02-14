package mindustry.world.blocks.environment;

import arc.*;
import arc.audio.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.*;

import static mindustry.Vars.*;

public class BorderAreaFloor extends Floor{
    public static final int modeInset = 0, modeOverflow = 1;
    public static final int sideLeft = 0, sideRight = 1, sideUp = 2, sideDown = 3;

    private static final int idBits = 14;
    private static final int idMask = (1 << idBits) - 1;
    private static final int secondShift = idBits;
    private static final int initBit = 1 << 31;
    private static final float[] verts = new float[4 * 5];
    private static final IntSet resolving = new IntSet();

    public final boolean topLeftToBottomRight;
    public final String spriteName;
    public final int visualMode;
    public Floor fallbackFloor = Blocks.stone.asFloor();
    public float overflowScale = 1.16f;
    public float overflowAlpha = 1f;
    public float diagonalSoften = 0.08f;
    public float overflowSeamAlpha = 1f;
    public float overflowCenterBlendAlpha = 0f;
    public BorderAreaFloor(String name, String spriteName, boolean topLeftToBottomRight){
        this(name, spriteName, topLeftToBottomRight, modeOverflow);
    }

    public BorderAreaFloor(String name, String spriteName, boolean topLeftToBottomRight, int visualMode){
        super(name, 0);
        this.spriteName = spriteName;
        this.topLeftToBottomRight = topLeftToBottomRight;
        this.visualMode = visualMode;

        saveData = true;
        editorConfigurable = false;
        drawEdgeIn = visualMode == modeInset;
        drawEdgeOut = visualMode == modeOverflow;
        supportsOverlay = true;
        //must share the same layer as surrounding floors, otherwise edge blending is skipped.
        cacheLayer = CacheLayer.normal;
    }

    @Override
    public void load(){
        super.load();

        TextureRegion found = Core.atlas.find(spriteName);
        if(found.found()){
            region = found;
            fullIcon = found;
            uiIcon = found;
        }
    }

    @Override
    public TextureRegion[] icons(){
        return new TextureRegion[]{Core.atlas.find(spriteName)};
    }

    @Override
    public void floorChanged(Tile tile){
        ensureData(tile);
    }

    @Override
    public void drawMain(Tile tile){
        ensureData(tile);

        TextureRegion firstRegion = sourceRegion(firstFloor(tile), tile);
        TextureRegion secondRegion = sourceRegion(secondFloor(tile), tile);

        if(visualMode == modeOverflow){
            if(firstRegion != null && firstRegion.found()){
                drawHalf(tile, firstRegion, true, 1f, 1f, overflowSeamAlpha);
            }

            if(secondRegion != null && secondRegion.found()){
                drawHalf(tile, secondRegion, false, 1f, 1f, overflowSeamAlpha);
            }
            return;
        }

        if(firstRegion != null && firstRegion.found()) drawHalf(tile, firstRegion, true, 1f, 1f, 1f);
        if(secondRegion != null && secondRegion.found()) drawHalf(tile, secondRegion, false, 1f, 1f, 1f);
    }

    @Override
    public boolean updateRender(Tile tile){
        return false;
    }

    public Floor sourceForSide(Tile tile, int side){
        ensureData(tile);
        Floor first = firstFloor(tile);
        Floor second = secondFloor(tile);
        if(topLeftToBottomRight){
            return side == sideLeft || side == sideUp ? first : second;
        }else{
            return side == sideRight || side == sideUp ? first : second;
        }
    }

    public float mixedSpeedMultiplier(Tile tile){
        Floor first = firstFloor(tile), second = secondFloor(tile);
        return (first.speedMultiplier + second.speedMultiplier) / 2f;
    }

    public float mixedDragMultiplier(Tile tile){
        Floor first = firstFloor(tile), second = secondFloor(tile);
        return (first.dragMultiplier + second.dragMultiplier) / 2f;
    }

    public float mixedDamageTaken(Tile tile){
        Floor first = firstFloor(tile), second = secondFloor(tile);
        return (first.damageTaken + second.damageTaken) / 2f;
    }

    public float mixedDrownTime(Tile tile){
        Floor first = firstFloor(tile), second = secondFloor(tile);
        return (first.drownTime + second.drownTime) / 2f;
    }

    public boolean mixedDeep(Tile tile){
        Floor first = firstFloor(tile), second = secondFloor(tile);
        return first.isDeep() || second.isDeep();
    }

    public boolean mixedPlaceableOn(Tile tile){
        Floor first = firstFloor(tile), second = secondFloor(tile);
        return first.placeableOn || second.placeableOn;
    }

    public boolean mixedHasWater(Tile tile){
        Floor first = firstFloor(tile), second = secondFloor(tile);
        return first.liquidDrop == Liquids.water || second.liquidDrop == Liquids.water;
    }

    public boolean mixedLiquid(Tile tile){
        Floor first = firstFloor(tile), second = secondFloor(tile);
        return first.isLiquid || second.isLiquid;
    }

    public Effect mixedWalkEffect(Tile tile){
        Floor first = firstFloor(tile), second = secondFloor(tile);
        return first.walkEffect != Fx.none ? first.walkEffect : second.walkEffect;
    }

    public Sound mixedWalkSound(Tile tile){
        Floor first = firstFloor(tile), second = secondFloor(tile);
        return first.walkSound != Sounds.none ? first.walkSound : second.walkSound;
    }

    public Effect mixedDrownUpdateEffect(Tile tile){
        Floor first = firstFloor(tile), second = secondFloor(tile);
        return first.drownUpdateEffect != Fx.none ? first.drownUpdateEffect : second.drownUpdateEffect;
    }

    public float mixedWalkSoundVolume(Tile tile){
        Floor first = firstFloor(tile), second = secondFloor(tile);
        return (first.walkSoundVolume + second.walkSoundVolume) / 2f;
    }

    public float mixedWalkSoundPitchMin(Tile tile){
        Floor first = firstFloor(tile), second = secondFloor(tile);
        return (first.walkSoundPitchMin + second.walkSoundPitchMin) / 2f;
    }

    public float mixedWalkSoundPitchMax(Tile tile){
        Floor first = firstFloor(tile), second = secondFloor(tile);
        return (first.walkSoundPitchMax + second.walkSoundPitchMax) / 2f;
    }

    public Color mixedMapColor(Tile tile, Color out){
        Floor first = firstFloor(tile), second = secondFloor(tile);
        return out.set(first.mapColor).lerp(second.mapColor, 0.5f);
    }

    private TextureRegion sourceRegion(Floor source, Tile tile){
        if(source == null) return null;

        if(source.tilingVariants > 0 || source.autotile){
            return source.region;
        }

        if(source.variantRegions != null && source.variantRegions.length > 0){
            return source.variantRegions[source.variant(tile.x, tile.y, source.variantRegions.length)];
        }

        return source.region;
    }

    private void drawHalf(Tile tile, TextureRegion region, boolean primary, float scale, float cornerAlpha, float seamAlpha){
        float x = tile.worldx(), y = tile.worldy();
        float s = tilesize / 2f * scale;
        float cornerColor = Color.toFloatBits(1f, 1f, 1f, Mathf.clamp(cornerAlpha));
        float seamColor = Color.toFloatBits(1f, 1f, 1f, Mathf.clamp(seamAlpha));
        float u = region.u, u2 = region.u2, v = region.v, v2 = region.v2;
        float diagx = 0f, diagy = 0f;

        if(visualMode == modeOverflow && scale > 1.0001f){
            float soften = diagonalSoften * tilesize;
            float inv = 0.70710677f;

            if(topLeftToBottomRight){
                diagx = (primary ? 1f : -1f) * inv * soften;
                diagy = (primary ? -1f : 1f) * inv * soften;
            }else{
                diagx = (primary ? -1f : 1f) * inv * soften;
                diagy = (primary ? -1f : 1f) * inv * soften;
            }
        }

        if(topLeftToBottomRight){
            if(primary){
                put(0, x - s, y + s, cornerColor, u, v);
                put(1, x + s + diagx, y + s + diagy, seamColor, u2, v);
                put(2, x - s + diagx, y - s + diagy, seamColor, u, v2);
                put(3, x - s + diagx, y - s + diagy, seamColor, u, v2);
            }else{
                put(0, x + s, y - s, cornerColor, u2, v2);
                put(1, x - s + diagx, y - s + diagy, seamColor, u, v2);
                put(2, x + s + diagx, y + s + diagy, seamColor, u2, v);
                put(3, x + s + diagx, y + s + diagy, seamColor, u2, v);
            }
        }else{
            if(primary){
                put(0, x + s, y + s, cornerColor, u2, v);
                put(1, x - s + diagx, y + s + diagy, seamColor, u, v);
                put(2, x + s + diagx, y - s + diagy, seamColor, u2, v2);
                put(3, x + s + diagx, y - s + diagy, seamColor, u2, v2);
            }else{
                put(0, x - s, y - s, cornerColor, u, v2);
                put(1, x + s + diagx, y - s + diagy, seamColor, u2, v2);
                put(2, x - s + diagx, y + s + diagy, seamColor, u, v);
                put(3, x - s + diagx, y + s + diagy, seamColor, u, v);
            }
        }

        Draw.vert(region.texture, verts, 0, verts.length);
    }

    private void put(int index, float x, float y, float color, float u, float v){
        int i = index * 5;
        verts[i] = x;
        verts[i + 1] = y;
        verts[i + 2] = color;
        verts[i + 3] = u;
        verts[i + 4] = v;
    }

    private Floor sourceForPoint(Tile tile, int px, int py){
        Floor first = firstFloor(tile);
        Floor second = secondFloor(tile);

        int cmp = topLeftToBottomRight ? (px - py) : (px + py);
        if(topLeftToBottomRight){
            if(cmp < 0) return first;
            if(cmp > 0) return second;
        }else{
            if(cmp > 0) return first;
            if(cmp < 0) return second;
        }
        return null;
    }

    @Override
    protected TextureRegion[][] edges(int x, int y){
        if(visualMode != modeOverflow){
            return null;
        }
        TextureRegion[][] base = fallbackFloor != null ? fallbackFloor.edges(x, y) : null;
        if(base != null) return base;
        return Blocks.stone.asFloor().edges(x, y);
    }

    @Override
    protected TextureRegion edge(int x, int y, int rx, int ry){
        if(visualMode != modeOverflow){
            return super.edge(x, y, rx, ry);
        }

        int ox = 1 - rx, oy = 1 - ry;
        Tile borderTile = world.tile(x + ox, y + oy);
        if(borderTile == null || borderTile.floor() != this){
            return super.edge(x, y, rx, ry);
        }

        Floor first = firstFloor(borderTile);
        Floor source = resolvedFloorForDirection(borderTile, -ox, -oy);
        if(source == null) source = first;

        if(source != null && source.edges(x, y) != null){
            return source.edge(x, y, rx, ry);
        }

        return super.edge(x, y, rx, ry);
    }

    private Floor resolvedFloorForDirection(Tile tile, int dx, int dy){
        if(tile == null) return null;
        Floor floor = tile.floor();
        if(floor == null || floor.isAir()) return null;

        if(floor instanceof BorderAreaFloor border){
            border.ensureData(tile);
            Floor side = border.sourceForPoint(tile, dx, dy);
            return side != null ? side : border.firstFloor(tile);
        }

        return floor;
    }

    private void ensureData(Tile tile){
        if(tile == null) return;
        if((tile.extraData & initBit) != 0) return;

        int pos = tile.pos();
        if(!resolving.add(pos)){
            Floor fallback = fallbackFloor(tile);
            tile.extraData = pack(fallback, fallback);
            return;
        }

        Floor first = null, second = null;

        if(topLeftToBottomRight){
            first = select(resolveNeighbor(tile.nearby(-1, 0), sideRight), resolveNeighbor(tile.nearby(0, 1), sideDown));
            second = select(resolveNeighbor(tile.nearby(1, 0), sideLeft), resolveNeighbor(tile.nearby(0, -1), sideUp));
        }else{
            first = select(resolveNeighbor(tile.nearby(1, 0), sideLeft), resolveNeighbor(tile.nearby(0, 1), sideDown));
            second = select(resolveNeighbor(tile.nearby(-1, 0), sideRight), resolveNeighbor(tile.nearby(0, -1), sideUp));
        }

        if(first == null) first = fallbackFloor(tile);
        if(second == null) second = first;

        tile.extraData = pack(first, second);
        resolving.remove(pos);
    }

    private Floor resolveNeighbor(Tile neighbor, int sideFacingCurrent){
        if(neighbor == null) return null;

        Floor floor = neighbor.floor();
        if(floor == null || floor.isAir()) return null;

        if(floor instanceof BorderAreaFloor border){
            border.ensureData(neighbor);
            return border.sourceForSide(neighbor, sideFacingCurrent);
        }

        return floor;
    }

    private Floor select(Floor horizontal, Floor vertical){
        if(horizontal != null) return horizontal;
        return vertical;
    }

    private Floor fallbackFloor(Tile tile){
        if(tile != null){
            for(Point2 p : Geometry.d4){
                Floor nearby = resolveNeighbor(tile.nearby(p), oppositeSide(p));
                if(nearby != null && !nearby.isAir()){
                    return nearby;
                }
            }
        }
        return fallbackFloor != null ? fallbackFloor : Blocks.stone.asFloor();
    }

    private int oppositeSide(Point2 p){
        if(p.x < 0) return sideRight;
        if(p.x > 0) return sideLeft;
        if(p.y < 0) return sideUp;
        return sideDown;
    }


    private int pack(Floor first, Floor second){
        return initBit | (first.id & idMask) | ((second.id & idMask) << secondShift);
    }

    private Floor firstFloor(Tile tile){
        return unpack((tile.extraData) & idMask);
    }

    private Floor secondFloor(Tile tile){
        return unpack((tile.extraData >>> secondShift) & idMask);
    }

    private Floor unpack(int id){
        Block block = content.block(id);
        if(block instanceof Floor floor && !floor.isAir()){
            return floor;
        }
        return fallbackFloor != null ? fallbackFloor : Blocks.stone.asFloor();
    }
}
