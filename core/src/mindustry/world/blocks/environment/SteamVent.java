package mindustry.world.blocks.environment;

import arc.*;
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
import mindustry.game.EventType.*;
import mindustry.world.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

//can't use an overlay for this because it spans multiple tiles
public class SteamVent extends Floor{
    private static final int reservesBits = 14;
    private static final int novaBits = 8;
    private static final int reservesMask = (1 << reservesBits) - 1;
    private static final int novaMask = (1 << novaBits) - 1;
    private static final int novaShift = reservesBits;
    private static final int averageBit = 1 << 28;
    private static final int novaSetBit = 1 << 29;
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
    public int defaultNovaCollect = 4;
    public int defaultReserves = 2250;
    public boolean defaultInfinite = false;

    static{
        for(var p : offsets){
            p.sub(1, 1);
        }
    }

    static{
        Events.on(WorldLoadEvent.class, e -> {
            if(net.client()) return;
            updateAllSpouts();
        });
        Events.on(TileChangeEvent.class, e -> {
            if(net.client()) return;
            updateSpoutsAround(e.tile);
        });
        Events.on(TileFloorChangeEvent.class, e -> {
            if(net.client()) return;
            updateSpoutsAround(e.tile);
        });
    }

    public SteamVent(String name){
        super(name);
        variants = 2;
        flags = EnumSet.of(BlockFlag.steamVent);
        saveData = true;
        editorConfigurable = true;
        saveConfig = true;
    }

    @Override
    public void init(){
        super.init();
        lastConfig = defaultPacked();
    }

    @Override
    public Object getConfig(Tile tile){
        return ensureConfig(tile);
    }

    @Override
    public void buildEditorConfig(Table table){
        int config = lastConfig instanceof Integer i ? i : defaultPacked();
        int nova = getNovaCollect(config);
        int reserves = getReserves(config);

        table.table(t -> {
            t.left();
            t.defaults().pad(3f).left();

            t.add("@steamvent.nova");
            t.field(Integer.toString(nova), text -> {
                int current = lastConfig instanceof Integer i ? i : defaultPacked();
                lastConfig = packConfig(
                    Strings.parseInt(text, defaultNovaCollect),
                    getReserves(current),
                    isInfinite(current),
                    false
                );
            }).valid(text -> isValidInt(text, novaMask));
            t.row();

            t.add("@steamvent.reserves");
            t.field(Integer.toString(reserves), text -> {
                int current = lastConfig instanceof Integer i ? i : defaultPacked();
                lastConfig = packConfig(
                    getNovaCollect(current),
                    Strings.parseInt(text, defaultReserves),
                    isInfinite(current),
                    false
                );
            }).valid(text -> isValidInt(text, reservesMask));
        }).growX();
    }

    @Override
    public void placeEnded(Tile tile, @Nullable Unit builder, int rotation, @Nullable Object config){
        if(config instanceof Integer i){
            tile.extraData = sanitizeConfig(i);
        }
    }

    @Override
    public void editorPicked(Tile tile){
        lastConfig = sanitizeConfig(tile.extraData);
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

    public int getNovaCollect(Tile tile){
        return getNovaCollect(ensureConfig(tile));
    }

    public int getReserves(Tile tile){
        return ensureConfig(tile) & reservesMask;
    }

    public void setReserves(Tile tile, int reserves){
        int config = ensureConfig(tile);
        tile.extraData = (config & (initBit | infiniteBit | averageBit | novaSetBit | (novaMask << novaShift)))
            | Mathf.clamp(reserves, 0, reservesMask);
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
                if(checkAdjacent(tile)){
                    for(var point : offsets){
                        Tile other = Vars.world.tile(tile.x + point.x, tile.y + point.y);
                        if(other != null && other.floor() == this){
                            other.setFloorNet(next);
                        }
                    }
                }else{
                    tile.setFloorNet(next);
                }
            }
        }else{
            tile.extraData = (config & (initBit | infiniteBit | averageBit | novaSetBit | (novaMask << novaShift))) | remaining;
        }
        return consumed;
    }

    public @Nullable Tile dataTile(Tile tile){
        Tile center = findCenter(tile);
        if(center != null){
            ensureAverage(center);
            return center;
        }
        return tile;
    }

    public void displayTile(Table table, Tile tile){
        Tile data = dataTile(tile);
        table.table(t -> {
            t.center();
            t.add(new Image(getDisplayIcon(tile))).size(8 * 4).row();
            t.labelWrap(getDisplayName(tile)).center().width(190f).padTop(5).row();
            t.labelWrap(() -> {
                if(isInfinite(data)){
                    return arc.Core.bundle.get("steamvent.remaining") + ": " + arc.Core.bundle.get("ui.infinite", "Infinite");
                }else{
                    return arc.Core.bundle.get("steamvent.remaining") + ": " + getReserves(data);
                }
            }).center().width(190f).color(Color.lightGray).padTop(3);
        }).growX().center();
    }

    private int ensureConfig(Tile tile){
        int config = tile.extraData;
        if((config & initBit) == 0){
            config = defaultPacked();
            if(!net.client()){
                tile.extraData = config;
            }
        }else if((config & novaSetBit) == 0){
            int reserves = config & reservesMask;
            boolean infinite = (config & infiniteBit) != 0;
            config = packConfig(defaultNovaCollect, reserves, infinite, (config & averageBit) != 0);
            if(!net.client()){
                tile.extraData = config;
            }
        }
        return config;
    }

    private int defaultPacked(){
        return packConfig(defaultNovaCollect, defaultReserves, defaultInfinite, false);
    }

    private int sanitizeConfig(int config){
        if((config & initBit) == 0){
            return defaultPacked();
        }
        return packConfig(getNovaCollect(config), getReserves(config), isInfinite(config), (config & averageBit) != 0);
    }

    private int packConfig(int nova, int reserves, boolean infinite, boolean averaged){
        int clampedNova = Mathf.clamp(nova, 0, novaMask);
        int clampedReserves = Mathf.clamp(reserves, 0, reservesMask);
        return initBit | novaSetBit | (averaged ? averageBit : 0) | (infinite ? infiniteBit : 0)
            | (clampedNova << novaShift) | clampedReserves;
    }

    private int getNovaCollect(int config){
        return (config >> novaShift) & novaMask;
    }

    private int getReserves(int config){
        return config & reservesMask;
    }

    private boolean isInfinite(int config){
        return (config & infiniteBit) != 0;
    }

    private boolean isValidInt(String text, int max){
        return Strings.canParseInt(text) && Strings.parseInt(text) >= 0 && Strings.parseInt(text) <= max;
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

    private @Nullable Tile findCenter(@Nullable Tile tile){
        if(tile == null) return null;
        if(tile.floor() != this) return tile;
        if(checkAdjacent(tile)) return tile;
        for(int dx = -1; dx <= 1; dx++){
            for(int dy = -1; dy <= 1; dy++){
                if(dx == 0 && dy == 0) continue;
                Tile other = tile.nearby(dx, dy);
                if(other != null && other.floor() == this && checkAdjacent(other)){
                    return other;
                }
            }
        }
        return tile;
    }

    private void ensureAverage(Tile center){
        if(center == null || !checkAdjacent(center)) return;
        int config = ensureConfig(center);
        if((config & averageBit) != 0) return;

        int total = 0;
        boolean infinite = isInfinite(config);
        for(var point : offsets){
            Tile other = Vars.world.tile(center.x + point.x, center.y + point.y);
            if(other == null || other.floor() != this) return;
            int otherConfig = ensureConfig(other);
            total += getReserves(otherConfig);
            if(isInfinite(otherConfig)) infinite = true;
        }

        int average = total / offsets.length;
        int nova = getNovaCollect(config);
        if(!net.client()){
            center.extraData = packConfig(nova, average, infinite, true);
        }
    }

    private static boolean updatingSpouts;

    private static void updateAllSpouts(){
        if(Blocks.ventSpout == null || updatingSpouts) return;
        updatingSpouts = true;
        try{
            for(Tile tile : world.tiles){
                if(tile.block() == Blocks.ventSpout){
                    tile.setNet(Blocks.air);
                }
            }

            for(Tile tile : world.tiles){
                if(tile.floor() instanceof SteamVent vent && vent.isCenterVent(tile) && areaClear(tile)){
                    setSpoutArea(tile);
                }
            }
        }catch(Throwable t){
            Log.err("Failed to update vent spouts.", t);
        }finally{
            updatingSpouts = false;
        }
    }

    private static void updateSpoutsAround(Tile tile){
        if(tile == null || Blocks.ventSpout == null || updatingSpouts) return;
        updatingSpouts = true;
        try{
            for(int dx = -2; dx <= 2; dx++){
                for(int dy = -2; dy <= 2; dy++){
                    Tile other = tile.nearby(dx, dy);
                    if(other != null && other.block() == Blocks.ventSpout){
                        other.setNet(Blocks.air);
                    }
                }
            }

            for(int dx = -2; dx <= 2; dx++){
                for(int dy = -2; dy <= 2; dy++){
                    Tile other = tile.nearby(dx, dy);
                    if(other == null) continue;
                    if(other.floor() instanceof SteamVent vent && vent.isCenterVent(other) && areaClear(other)){
                        setSpoutArea(other);
                    }
                }
            }
        }catch(Throwable t){
            Log.err("Failed to update vent spouts around tile.", t);
        }finally{
            updatingSpouts = false;
        }
    }

    private static void setSpoutArea(Tile center){
        for(int dx = -1; dx <= 1; dx++){
            for(int dy = -1; dy <= 1; dy++){
                Tile other = world.tile(center.x + dx, center.y + dy);
                if(other != null && other.block() != Blocks.ventSpout){
                    other.setNet(Blocks.ventSpout);
                }
            }
        }
    }

    private static boolean areaClear(Tile center){
        for(int dx = -1; dx <= 1; dx++){
            for(int dy = -1; dy <= 1; dy++){
                Tile other = world.tile(center.x + dx, center.y + dy);
                if(other == null) return false;
                Block block = other.block();
                if(block != Blocks.air && block != Blocks.ventSpout) return false;
            }
        }
        return true;
    }
}


