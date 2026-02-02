package mindustry.world.blocks.environment;

import arc.graphics.*;
import arc.math.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;

import static mindustry.Vars.*;

public class CrystalMineralWall extends StaticWall{
    private static final int novaBits = 8;
    private static final int pulsarBits = 8;
    private static final int reservesBits = 14;

    private static final int novaMask = (1 << novaBits) - 1;
    private static final int pulsarMask = (1 << pulsarBits) - 1;
    private static final int reservesMask = (1 << reservesBits) - 1;

    private static final int pulsarShift = novaBits;
    private static final int reservesShift = novaBits + pulsarBits;
    private static final int infiniteBit = 1 << 30;
    private static final int initBit = 1 << 31;

    public int defaultNovaCollect = 5;
    public int defaultPulsarCollect = 25;
    public int defaultReserves = 1000;
    public boolean defaultInfinite = false;

    public CrystalMineralWall(String name){
        super(name);
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
        int pulsar = getPulsarCollect(config);
        int reserves = getReserves(config);
        boolean infinite = isInfinite(config);

        table.table(t -> {
            t.left();
            t.defaults().pad(3f).left();

            t.add("@crystalmineral.nova");
            t.field(Integer.toString(nova), text -> {
                int current = lastConfig instanceof Integer i ? i : defaultPacked();
                lastConfig = packConfig(
                    Strings.parseInt(text, defaultNovaCollect),
                    getPulsarCollect(current),
                    getReserves(current),
                    isInfinite(current)
                );
            }).valid(text -> isValidInt(text, novaMask));
            t.row();

            t.add("@crystalmineral.pulsar");
            t.field(Integer.toString(pulsar), text -> {
                int current = lastConfig instanceof Integer i ? i : defaultPacked();
                lastConfig = packConfig(
                    getNovaCollect(current),
                    Strings.parseInt(text, defaultPulsarCollect),
                    getReserves(current),
                    isInfinite(current)
                );
            }).valid(text -> isValidInt(text, pulsarMask));
            t.row();

            t.add("@crystalmineral.reserves");
            t.field(Integer.toString(reserves), text -> {
                int current = lastConfig instanceof Integer i ? i : defaultPacked();
                lastConfig = packConfig(
                    getNovaCollect(current),
                    getPulsarCollect(current),
                    Strings.parseInt(text, defaultReserves),
                    isInfinite(current)
                );
            }).valid(text -> isValidInt(text, reservesMask));
            t.row();

            t.check("@crystalmineral.infinite", infinite, value -> {
                int current = lastConfig instanceof Integer i ? i : defaultPacked();
                lastConfig = packConfig(
                    getNovaCollect(current),
                    getPulsarCollect(current),
                    getReserves(current),
                    value
                );
            });
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
    public void blockChanged(Tile tile){
        if((tile.extraData & initBit) == 0){
            tile.extraData = defaultPacked();
        }
    }

    public void displayTile(Table table, Tile tile){
        table.table(t -> {
            t.center();
            t.add(new Image(getDisplayIcon(tile))).size(8 * 4).row();
            t.labelWrap(getDisplayName(tile)).center().width(190f).padTop(5).row();
            t.labelWrap(() -> {
                if(isInfinite(tile)){
                    return arc.Core.bundle.get("crystalmineral.remaining") + ": ∞";
                }else{
                    return arc.Core.bundle.get("crystalmineral.remaining") + ": " + getReserves(tile);
                }
            }).center().width(190f).color(Color.lightGray).padTop(3);
        }).growX().center();
    }

    public int mineAmount(Tile tile, Unit unit){
        int config = ensureConfig(tile);
        UnitType type = unit.type;
        if(type == UnitTypes.nova) return getNovaCollect(config);
        if(type == UnitTypes.pulsar) return getPulsarCollect(config);
        return 1;
    }

    public boolean isInfinite(Tile tile){
        return isInfinite(ensureConfig(tile));
    }

    public int getReserves(Tile tile){
        return getReserves(ensureConfig(tile));
    }

    public void setReserves(Tile tile, int reserves){
        int config = ensureConfig(tile);
        tile.extraData = packConfig(getNovaCollect(config), getPulsarCollect(config), reserves, isInfinite(config));
    }

    private int ensureConfig(Tile tile){
        int config = tile.extraData;
        if((config & initBit) == 0){
            config = defaultPacked();
            if(!net.client()){
                tile.extraData = config;
            }
        }
        return config;
    }

    private int defaultPacked(){
        return packConfig(defaultNovaCollect, defaultPulsarCollect, defaultReserves, defaultInfinite);
    }

    private int sanitizeConfig(int config){
        if((config & initBit) == 0){
            return defaultPacked();
        }
        return packConfig(getNovaCollect(config), getPulsarCollect(config), getReserves(config), isInfinite(config));
    }

    private int packConfig(int nova, int pulsar, int reserves, boolean infinite){
        int clampedNova = Mathf.clamp(nova, 0, novaMask);
        int clampedPulsar = Mathf.clamp(pulsar, 0, pulsarMask);
        int clampedReserves = Mathf.clamp(reserves, 0, reservesMask);
        return initBit | (infinite ? infiniteBit : 0) | (clampedReserves << reservesShift) | (clampedPulsar << pulsarShift) | clampedNova;
    }

    private int getNovaCollect(int config){
        return config & novaMask;
    }

    private int getPulsarCollect(int config){
        return (config >> pulsarShift) & pulsarMask;
    }

    private int getReserves(int config){
        return (config >> reservesShift) & reservesMask;
    }

    private boolean isInfinite(int config){
        return (config & infiniteBit) != 0;
    }

    private boolean isValidInt(String text, int max){
        return Strings.canParseInt(text) && Strings.parseInt(text) >= 0 && Strings.parseInt(text) <= max;
    }
}
