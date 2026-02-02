package mindustry.entities.comp;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.ai.types.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.input.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.environment.*;

import static mindustry.Vars.*;

@Component
abstract class MinerComp implements Itemsc, Posc, Teamc, Rotc, Drawc{
    @Import float x, y, rotation, hitSize;
    @Import UnitType type;

    transient float mineTimer;
    @Nullable @SyncLocal Tile mineTile;

    public boolean canMine(@Nullable Item item){
        if(item == null) return false;
        return type.mineTier >= item.hardness;
    }

    public boolean offloadImmediately(){
        return this.<Unit>self().isPlayer();
    }

    boolean mining(){
        return mineTile != null && !this.<Unit>self().activelyBuilding();
    }

    public @Nullable Item getMineResult(@Nullable Tile tile){
        if(tile == null) return null;
        Item result;
        if(type.mineFloor && tile.block() == Blocks.air){
            result = tile.drop();
        }else if(type.mineWalls){
            result = tile.wallDrop();
        }else{
            return null;
        }

        return canMine(result) ? result : null;
    }

    public boolean validMine(Tile tile, boolean checkDst){
        if(tile == null) return false;

        if(checkDst && !within(tile.worldx(), tile.worldy(), type.mineRange)){
            return false;
        }

        return getMineResult(tile) != null;
    }

    public boolean validMine(Tile tile){
        return validMine(tile, true);
    }

    public boolean canMine(){
        return type.mineSpeed * state.rules.unitMineSpeed(team()) > 0 && type.mineTier >= 0;
    }

    @Override
    public void update(){
        // Skip if using HarvestAI
        if(this.<Unit>self().controller() instanceof HarvestAI) return;

        if(mineTile == null) return;

        Building core = closestCore();
        Item item = getMineResult(mineTile);

        if(core != null && item != null && !acceptsItem(item) && within(core, mineTransferRange) && !offloadImmediately()){
            int accepted = core.acceptStack(item(), stack().amount, this);
            if(accepted > 0){
                Call.transferItemTo(self(), item(), accepted,
                mineTile.worldx() + Mathf.range(tilesize / 2f),
                mineTile.worldy() + Mathf.range(tilesize / 2f), core);
                clearItem();
            }
        }

        if((!net.client() || isLocal()) && !validMine(mineTile)){
            mineTile = null;
            mineTimer = 0f;
        }else if(mining() && item != null){
            mineTimer += Time.delta * type.mineSpeed * state.rules.unitMineSpeed(team());

            if(Mathf.chance(0.06 * Time.delta)){
                Fx.pulverizeSmall.at(mineTile.worldx() + Mathf.range(tilesize / 2f), mineTile.worldy() + Mathf.range(tilesize / 2f), 0f, item.color);
            }

            if(mineTimer >= 50f + (type.mineHardnessScaling ? item.hardness*15f : 15f)){
                mineTimer = 0;

                int amount = 1;
                int reserves = 0;
                boolean finite = false;
                CrystalMineralWall crystal = mineTile.block() instanceof CrystalMineralWall cm ? cm : null;
                if(crystal != null){
                    amount = crystal.mineAmount(mineTile, this.<Unit>self());
                    if(!crystal.isInfinite(mineTile)){
                        reserves = crystal.getReserves(mineTile);
                        amount = Math.min(amount, reserves);
                        finite = true;
                    }
                }

                if(amount <= 0){
                    mineTile = null;
                    mineTimer = 0f;
                    return;
                }

                int toCore = 0;
                if(core != null && within(core, mineTransferRange) && offloadImmediately()){
                    toCore = core.acceptStack(item, amount, this);
                }

                int toUnit = 0;
                if(amount - toCore > 0 && acceptsItem(item)){
                    toUnit = Math.min(amount - toCore, maxAccepted(item));
                }

                int mined = toCore + toUnit;
                if(mined <= 0){
                    mineTile = null;
                    mineTimer = 0f;
                    return;
                }

                if(finite && !net.client()){
                    int remaining = reserves - mined;
                    if(remaining <= 0){
                        mineTile.setNet(Blocks.air);
                    }else{
                        crystal.setReserves(mineTile, remaining);
                    }
                }

                if(state.rules.sector != null && team() == state.rules.defaultTeam) state.rules.sector.info.handleProduction(item, mined);

                if(toCore > 0){
                    //add item to inventory before it is transferred
                    if(item() == item && !net.client()) addItem(item, toCore);
                    Call.transferItemTo(self(), item, toCore,
                    mineTile.worldx() + Mathf.range(tilesize / 2f),
                    mineTile.worldy() + Mathf.range(tilesize / 2f), core);
                }

                if(toUnit > 0){
                    //this is clientside, since items are synced anyway
                    for(int i = 0; i < toUnit; i++){
                        InputHandler.transferItemToUnit(item,
                        mineTile.worldx() + Mathf.range(tilesize / 2f),
                        mineTile.worldy() + Mathf.range(tilesize / 2f),
                        this);
                    }
                }else if(mined > 0){
                    mineTile = null;
                    mineTimer = 0f;
                }
            }

            if(!headless){
                control.sound.loop(type.mineSound, this, type.mineSoundVolume);
            }
        }
    }
}
