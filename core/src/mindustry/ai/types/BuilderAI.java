package mindustry.ai.types;

import arc.struct.*;
import arc.util.*;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.game.Teams.*;
import mindustry.gen.*;
import mindustry.world.*;
import mindustry.world.blocks.ConstructBlock.*;

import static mindustry.Vars.*;

public class BuilderAI extends AIController{
    public static float buildRadius = 1500, retreatDst = 110f, retreatDelay = Time.toSeconds * 2f, defaultRebuildPeriod = 60f * 2f;

    public @Nullable Unit assistFollowing;
    public @Nullable Unit following;
    public @Nullable Teamc enemy;
    public @Nullable BlockPlan lastPlan;

    public float fleeRange = 370f, rebuildPeriod = defaultRebuildPeriod;
    public boolean alwaysFlee;
    public boolean onlyAssist;

    boolean found = false;
    float retreatTimer;

    public BuilderAI(boolean alwaysFlee, float fleeRange){
        this.alwaysFlee = alwaysFlee;
        this.fleeRange = fleeRange;
    }

    public BuilderAI(){
    }

    @Override
    public void init(){
        //rebuild much faster with buildAI; there are usually few builder units so this is fine
        if(rebuildPeriod == defaultRebuildPeriod && unit.team.rules().buildAi){
            rebuildPeriod = 10f;
        }
    }

    @Override
    public void updateMovement(){

        if(target != null && shouldShoot()){
            unit.lookAt(target);
        }else if(!unit.type.flying){
            unit.lookAt(unit.prefRotation());
        }

        unit.updateBuilding = true;

        boolean moving = false;
        if(unit.buildPlan() == null || alwaysFlee){
            //not following anyone or building
            if(timer.get(timerTarget4, 40)){
                enemy = target(unit.x, unit.y, fleeRange, true, true);
            }

            //fly away from enemy when not doing anything, but only after a delay
            if((retreatTimer += Time.delta) >= retreatDelay || alwaysFlee){
                if(enemy != null){
                    unit.clearBuilding();
                    var core = unit.closestCore();
                    if(core != null && !unit.within(core, retreatDst)){
                        moveTo(core, retreatDst);
                        moving = true;
                    }
                }
            }
        }

        if(unit.buildPlan() != null){
            if(!alwaysFlee) retreatTimer = 0f;
            //approach plan if building
            BuildPlan req = unit.buildPlan();

            //clear break plan if another player is breaking something
            if(!req.breaking && timer.get(timerTarget2, 40f)){
                for(Player player : Groups.player){
                    if(player.isBuilder() && player.unit().activelyBuilding() && player.unit().buildPlan().samePos(req) && player.unit().buildPlan().breaking){
                        unit.plans.removeFirst();
                        //remove from list of plans
                        unit.team.data().plans.remove(p -> p.x == req.x && p.y == req.y);
                        return;
                    }
                }
            }

            boolean valid =
                !(lastPlan != null && lastPlan.removed) &&
                    ((req.tile() != null && req.tile().build instanceof ConstructBuild cons && cons.current == req.block) ||
                    (req.breaking ?
                        Build.validBreak(unit.team(), req.x, req.y) :
                        Build.validPlace(req.block, unit.team(), req.x, req.y, req.rotation)));

            if(valid){
                float range = Math.min(unit.type.buildRange - unit.type.hitSize * 2f, buildRadius);
                if(req.requireClose && req.block != null){
                    float unitRadius = unit.hitSize / 2f;
                    float closeRange = req.block.size * tilesize / 2f + unitRadius + 0.1f;
                    range = Math.min(range, closeRange);
                }
                //move toward the plan
                moveTo(req.tile(), range, 20f);
                moving = !unit.within(req.tile(), range);
            }else{
                //discard invalid plan
                unit.plans.removeFirst();
                lastPlan = null;
            }
        }else{
            //find new plan
            if(!unit.team.data().plans.isEmpty() && timer.get(timerTarget3, rebuildPeriod)){
                Queue<BlockPlan> blocks = unit.team.data().plans;
                BlockPlan block = blocks.first();

                //check if it's already been placed
                if(world.tile(block.x, block.y) != null && world.tile(block.x, block.y).block() == block.block){
                    blocks.removeFirst();
                }else if(Build.validPlace(block.block, unit.team(), block.x, block.y, block.rotation) && (!alwaysFlee || !nearEnemy(block.x, block.y))){ //it's valid
                    lastPlan = block;
                    //add build plan
                    unit.addBuild(new BuildPlan(block.x, block.y, block.rotation, block.block, block.config));
                    //shift build plan to tail so next unit builds something else
                    blocks.addLast(blocks.removeFirst());
                }else{
                    //shift head of queue to tail, try something else next time
                    blocks.addLast(blocks.removeFirst());
                }
            }
        }

        if(!unit.type.flying){
            unit.updateBoosting(unit.type.boostWhenBuilding || moving || unit.floorOn().isDuct || unit.floorOn().damageTaken > 0f || unit.floorOn().isDeep());
        }
    }

    protected boolean nearEnemy(int x, int y){
        return Units.nearEnemy(unit.team, x * tilesize - fleeRange/2f, y * tilesize - fleeRange/2f, fleeRange, fleeRange);
    }

    @Override
    public AIController fallback(){
        if(unit.team.isAI() && unit.team.rules().prebuildAi){
            return new PrebuildAI();
        }
        return unit.type.flying ? new FlyingAI() : new GroundAI();
    }

    @Override
    public boolean useFallback(){
        if(unit.team.isAI() && unit.team.rules().prebuildAi){
            return true;
        }
        return state.rules.waves && unit.team == state.rules.waveTeam && !unit.team.rules().rtsAi;
    }

    @Override
    public boolean shouldFire(){
        return !(unit.controller() instanceof CommandAI ai) || ai.shouldFire();
    }

    @Override
    public boolean shouldShoot(){
        return !unit.isBuilding() && unit.type.canAttack;
    }
}
