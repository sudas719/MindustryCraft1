package mindustry.world.blocks.environment;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

//can't use an overlay for this because it spans multiple tiles
public class SteamVent extends Floor{
    private static final int reservesBits = 14;
    private static final int reservesMask = (1 << reservesBits) - 1;
    private static final int infiniteBit = 1 << 30;
    private static final int initBit = 1 << 31;

    public static final Point2[] offsets = {
        new Point2(0, 0),
        new Point2(1, 0),
        new Point2(1, 1),
        new Point2(0, 1),
        new Point2(-1, 1),
        new Point2(-1, 0),
        new Point2(-1, -1),
        new Point2(0, -1),
        new Point2(1, -1),
    };

    public Block parent = Blocks.air;
    public Effect effect = Fx.ventSteam;
    public Color effectColor = Pal.vent;
    public float effectSpacing = 15f;
    public int defaultReserves = 1000;
    public boolean defaultInfinite = false;

    static{
        for(var p : offsets){
            p.sub(1, 1);
        }
    }

    public SteamVent(String name){
        super(name);
        variants = 2;
        flags = EnumSet.of(BlockFlag.steamVent);
        saveData = true;
    }

    @Override
    public void drawMain(Tile tile){
        ensureConfig(tile);
        if(parent instanceof Floor floor){
            floor.drawMain(tile);
        }

        if(checkAdjacent(tile)){
            Draw.rect(variantRegions[Mathf.randomSeed(tile.pos(), 0, Math.max(0, variantRegions.length - 1))], tile.worldx() - tilesize, tile.worldy() - tilesize);
        }
    }

    @Override
    public boolean updateRender(Tile tile){
        ensureConfig(tile);
        return checkAdjacent(tile);
    }

    @Override
    public boolean shouldIndex(Tile tile){
        ensureConfig(tile);
        return isCenterVent(tile);
    }

    public boolean isCenterVent(Tile tile){
        Tile topRight = tile.nearby(1, 1);
        return topRight != null && topRight.floor() == tile.floor() && checkAdjacent(topRight);
    }

    @Override
    public void renderUpdate(UpdateRenderState state){
        ensureConfig(state.tile);
        if(state.tile.nearby(-1, -1) != null && state.tile.nearby(-1, -1).block() == Blocks.air && (state.data += Time.delta) >= effectSpacing){
            effect.at(state.tile.x * tilesize - tilesize, state.tile.y * tilesize - tilesize, effectColor);
            state.data = 0f;
        }
    }

    public boolean isInfinite(Tile tile){
        return (ensureConfig(tile) & infiniteBit) != 0;
    }

    public int getReserves(Tile tile){
        return ensureConfig(tile) & reservesMask;
    }

    public void setReserves(Tile tile, int reserves){
        int config = ensureConfig(tile);
        tile.extraData = (config & (initBit | infiniteBit)) | Mathf.clamp(reserves, 0, reservesMask);
    }

    public int consumeGas(Tile tile, int amount){
        if(amount <= 0) return 0;
        int config = ensureConfig(tile);
        if((config & infiniteBit) != 0) return 0;

        int reserves = config & reservesMask;
        int consumed = Math.min(reserves, amount);
        int remaining = reserves - consumed;
        if(remaining <= 0){
            if(!net.client()){
                Floor next = parent instanceof Floor p ? p : Blocks.air.asFloor();
                tile.setFloorNet(next);
            }
        }else{
            tile.extraData = (config & (initBit | infiniteBit)) | remaining;
        }
        return consumed;
    }

    public @Nullable Tile dataTile(Tile tile){
        return tile;
    }

    public void displayTile(Table table, Tile tile){
        table.table(t -> {
            t.center();
            t.add(new Image(getDisplayIcon(tile))).size(8 * 4).row();
            t.labelWrap(getDisplayName(tile)).center().width(190f).padTop(5).row();
            t.labelWrap(() -> {
                if(isInfinite(tile)){
                    return arc.Core.bundle.get("steamvent.remaining") + ": ∞";
                }else{
                    return arc.Core.bundle.get("steamvent.remaining") + ": " + getReserves(tile);
                }
            }).center().width(190f).color(Color.lightGray).padTop(3);
        }).growX().center();
    }

    private int ensureConfig(Tile tile){
        int config = tile.extraData;
        if((config & initBit) == 0){
            config = initBit | (defaultInfinite ? infiniteBit : 0) | Mathf.clamp(defaultReserves, 0, reservesMask);
            if(!net.client()){
                tile.extraData = config;
            }
        }
        return config;
    }

    //note that only the top right tile works for this; render order reasons.
    public boolean checkAdjacent(Tile tile){
        for(var point : offsets){
            Tile other = Vars.world.tile(tile.x + point.x, tile.y + point.y);
            if(other == null || other.floor() != this){
                return false;
            }
        }
        return true;
    }
}
