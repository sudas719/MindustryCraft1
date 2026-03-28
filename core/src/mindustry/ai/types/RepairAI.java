package mindustry.ai.types;

import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.ai.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.blocks.storage.CoreBlock.*;
import mindustry.world.blocks.ConstructBlock.*;

import static mindustry.Vars.*;

public class RepairAI extends AIController{
    public static float retreatDst = 160f, fleeRange = 310f, retreatDelay = Time.toSeconds * 3f;
    private static final int repairTargetNone = 0;
    private static final int repairTargetUnit = 1;
    private static final int repairTargetBuilding = 2;
    private static final float scvRepairRange = 0.95f;
    private static final float scvRepairUnitPercentPerSecond = 0.01f;
    private static final float scvRepairBuildingsPerSecond = 21f;
    private static final float scvRepairCostScale = 0.75f;

    @Nullable Teamc avoid;
    @Nullable Teamc forcedTarget;
    float retreatTimer;
    Building damagedTarget;
    int repairCostTargetKind = repairTargetNone;
    int repairCostTargetUnit = -1;
    int repairCostTargetBuilding = -1;
    float repairCostHealed = 0f;
    IntIntMap repairCostPaid = new IntIntMap();
    int scvHealTargetKind = repairTargetNone;
    int scvHealTargetId = -1;
    float scvHealCarry = 0f;

    @Override
    public void commandTarget(Teamc moveTo){
        forcedTarget = moveTo;
        target = moveTo;
    }

    @Override
    public void commandPosition(Vec2 pos){
        forcedTarget = null;
        target = null;
        resetRepairCostState();
    }

    private void resetRepairCostState(){
        repairCostTargetKind = repairTargetNone;
        repairCostTargetUnit = -1;
        repairCostTargetBuilding = -1;
        repairCostHealed = 0f;
        repairCostPaid.clear();
        scvHealTargetKind = repairTargetNone;
        scvHealTargetId = -1;
        scvHealCarry = 0f;
    }

    private void ensureRepairCostTarget(int kind, int id){
        boolean same;
        if(kind == repairTargetUnit){
            same = repairCostTargetKind == repairTargetUnit && repairCostTargetUnit == id;
        }else if(kind == repairTargetBuilding){
            same = repairCostTargetKind == repairTargetBuilding && repairCostTargetBuilding == id;
        }else{
            same = repairCostTargetKind == repairTargetNone;
        }

        if(same) return;

        resetRepairCostState();
        repairCostTargetKind = kind;
        if(kind == repairTargetUnit){
            repairCostTargetUnit = id;
        }else if(kind == repairTargetBuilding){
            repairCostTargetBuilding = id;
        }
    }

    private void ensureScvHealTarget(int kind, int id){
        if(scvHealTargetKind == kind && scvHealTargetId == id) return;
        scvHealTargetKind = kind;
        scvHealTargetId = id;
        scvHealCarry = 0f;
    }

    private boolean isScv(){
        return unit != null && unit.type == UnitTypes.nova;
    }

    private boolean validScvRepairTarget(@Nullable Teamc value){
        if(value == null || unit == null) return false;
        if(value.team() != unit.team) return false;

        if(value instanceof Unit ally){
            return ally.isValid()
                && ally != unit
                && ally.health < ally.maxHealth() - 0.001f
                && ally.type.unitClasses.contains(UnitClass.mechanical);
        }

        if(value instanceof Building build){
            return build.isValid() && build.team == unit.team && !(build instanceof ConstructBuild);
        }

        return false;
    }

    private float targetRadius(@Nullable Teamc value){
        if(value instanceof Unit ally) return ally.hitSize / 2f;
        if(value instanceof Building build) return build.hitSize() / 2f;
        if(value instanceof Sized sized) return sized.hitSize() / 2f;
        return 4f;
    }

    private float scvRepairApproach(@Nullable Teamc value){
        if(unit == null) return 0f;
        return scvRepairRange + targetRadius(value) + unit.hitSize / 2f;
    }

    private boolean isRepairTargetFull(@Nullable Teamc value){
        if(value instanceof Unit ally){
            return ally.health >= ally.maxHealth() - 0.001f;
        }
        if(value instanceof Building build){
            return build.health >= build.maxHealth() - 0.001f;
        }
        return true;
    }

    private void moveToScvRepairTarget(Teamc value){
        if(unit == null || value == null) return;

        if(value instanceof Building build){
            float tx = build.x, ty = build.y;
            float desired = build.hitSize() / 2f + unit.hitSize / 2f;
            if(unit.dst2(tx, ty) <= desired * desired) return;

            Tmp.v1.set(unit.x - tx, unit.y - ty);
            if(Tmp.v1.isZero(0.001f)){
                Tmp.v1.set(1f, 0f);
            }
            Tmp.v1.setLength(desired);
            Tmp.v2.set(tx + Tmp.v1.x, ty + Tmp.v1.y);
            moveTo(Tmp.v2, 0f, 20f);
            return;
        }

        float approach = scvRepairApproach(value);
        if(!value.within(unit, approach)){
            moveTo(value, approach, 20f);
        }
    }

    private void updateScvMovement(){
        unit.controlWeapons(false);

        if(unit.controller() instanceof CommandAI ai){
            Teamc active = target != null ? target : forcedTarget;
            if(active != null && isRepairTargetFull(active) && ai.commandQueue.size > 0){
                target = null;
                forcedTarget = null;
            }
        }

        if((target == null || !validScvRepairTarget(target)) && (forcedTarget == null || !validScvRepairTarget(forcedTarget))){
            if(unit.controller() instanceof CommandAI ai && ai.commandQueue.size > 0){
                while(ai.commandQueue.size > 0){
                    Position next = ai.commandQueue.remove(0);
                    if(next instanceof Teamc teamc){
                        if(teamc.team() == unit.team){
                            forcedTarget = teamc;
                            target = teamc;
                            break;
                        }else{
                            //Non-friendly queued command: hand control back to CommandAI.
                            ai.command(UnitCommand.moveCommand);
                            ai.commandTarget(teamc, ai.stopAtTarget);
                            return;
                        }
                    }else if(next instanceof CommandAI.RepairMarker marker){
                        Building build = marker.build();
                        if(build != null && build.team == unit.team && !(build instanceof ConstructBuild)){
                            forcedTarget = build;
                            target = build;
                            break;
                        }else{
                            //Target vanished; skip.
                            continue;
                        }
                    }else if(next instanceof Vec2 vec){
                        Building build = world.buildWorld(vec.x, vec.y);
                        if(build != null && build.team == unit.team && !(build instanceof ConstructBuild) && build.within(vec.x, vec.y, build.hitSize() / 2f + 1f)){
                            if(build.health < build.maxHealth() - 0.001f){
                                forcedTarget = build;
                                target = build;
                                break;
                            }else{
                                //Already full; skip and continue processing the queue.
                                continue;
                            }
                        }

                        ai.command(UnitCommand.moveCommand);
                        ai.commandPosition(vec);
                        return;
                    }else{
                        ai.command(UnitCommand.moveCommand);
                        ai.commandPosition(new Vec2(next.getX(), next.getY()));
                        return;
                    }
                }
            }
        }

        if(!validScvRepairTarget(target)){
            if(!validScvRepairTarget(forcedTarget)){
                forcedTarget = null;
                target = null;
                resetRepairCostState();
                return;
            }
            target = forcedTarget;
        }

        if(target == null) return;

        moveToScvRepairTarget(target);
        unit.lookAt(target);

        if(Units.withinTargetRange(target, unit.x, unit.y, scvRepairRange, unit.hitSize / 2f)){
            applyScvRepair(target);
        }
    }

    private void applyScvRepair(Teamc target){
        if(net.client()) return;

        if(target instanceof Unit ally){
            if(ally.health >= ally.maxHealth() - 0.001f) return;
            float heal = ally.maxHealth * scvRepairUnitPercentPerSecond * Time.delta / 60f;
            healScvUnit(ally, heal);
        }else if(target instanceof Building build){
            if(build.health >= build.maxHealth() - 0.001f || build instanceof ConstructBuild) return;
            float heal = scvRepairBuildingsPerSecond * Time.delta / 60f;
            healScvBuilding(build, heal);
        }
    }

    private void healScvUnit(Unit target, float amount){
        if(amount <= 0f || unit == null || target == null || !target.isValid() || target.health >= target.maxHealth() - 0.001f) return;

        ensureScvHealTarget(repairTargetUnit, target.id);

        float remaining = Math.max(target.maxHealth() - target.health, 0f);
        if(remaining <= 0.0001f) return;

        float total = amount + scvHealCarry;
        int whole = (int)total;
        if(whole <= 0){
            scvHealCarry = total;
            return;
        }

        int need = Mathf.ceil(remaining - 0.0001f);
        int apply = Math.min(whole, need);
        if(apply <= 0){
            scvHealCarry = total;
            return;
        }

        float heal = apply;
        if(!consumeScvRepairResources(target, heal)){
            scvHealCarry = total;
            return;
        }

        target.heal(heal);
        scvHealCarry = (total - whole) + (whole - apply);
    }

    private void healScvBuilding(Building target, float amount){
        if(amount <= 0f || unit == null || target == null || !target.isValid() || target.health >= target.maxHealth() - 0.001f) return;

        ensureScvHealTarget(repairTargetBuilding, target.id);

        float remaining = Math.max(target.maxHealth() - target.health, 0f);
        if(remaining <= 0.0001f) return;

        float total = amount + scvHealCarry;
        int whole = (int)total;
        if(whole <= 0){
            scvHealCarry = total;
            return;
        }

        int need = Mathf.ceil(remaining - 0.0001f);
        int apply = Math.min(whole, need);
        if(apply <= 0){
            scvHealCarry = total;
            return;
        }

        float heal = apply;
        if(!consumeScvBuildingResources(target, heal)){
            scvHealCarry = total;
            return;
        }

        target.heal(heal);
        scvHealCarry = (total - whole) + (whole - apply);
    }

    private boolean consumeScvRepairResources(Unit target, float healAmount){
        if(state.rules.infiniteResources || unit.team.rules().infiniteResources) return true;
        if(target == null || healAmount <= 0f || target.maxHealth() <= 0.0001f) return false;

        ItemStack[] requirements = target.type.getTotalRequirements();
        if(requirements == null || requirements.length == 0) return true;

        float unitCostScale = Math.max(state.rules.unitCost(unit.team), 0f) * scvRepairCostScale;
        return consumeRepairCost(requirements, target.maxHealth(), healAmount, unitCostScale, repairTargetUnit, target.id);
    }

    private boolean consumeScvBuildingResources(Building target, float healAmount){
        if(state.rules.infiniteResources || unit.team.rules().infiniteResources) return true;
        if(target == null || healAmount <= 0f || target.maxHealth() <= 0.0001f) return false;

        ItemStack[] requirements = target.block == null ? null : target.block.requirements;
        if(requirements == null || requirements.length == 0) return true;

        float buildCostScale = Math.max(state.rules.buildCostMultiplier, 0f) * scvRepairCostScale;
        return consumeRepairCost(requirements, target.maxHealth(), healAmount, buildCostScale, repairTargetBuilding, target.pos());
    }

    private boolean consumeRepairCost(ItemStack[] requirements, float maxHealth, float healAmount, float costScale, int targetKind, int targetId){
        if(requirements == null || requirements.length == 0) return true;
        if(maxHealth <= 0.0001f || healAmount <= 0f) return false;

        var resources = unit.team.data();

        ensureRepairCostTarget(targetKind, targetId);

        float nextHealed = repairCostHealed + healAmount;

        for(ItemStack stack : requirements){
            if(stack == null || stack.item == null || stack.amount <= 0) continue;
            float scaledCost = stack.amount * costScale;
            if(scaledCost <= 0.0001f) continue;

            int required = Mathf.ceil(nextHealed / maxHealth * scaledCost);
            int paid = repairCostPaid.get(stack.item.id, 0);
            int delta = Math.max(required - paid, 0);
            if(delta > 0 && resources.resource(stack.item) < delta){
                return false;
            }
        }

        for(ItemStack stack : requirements){
            if(stack == null || stack.item == null || stack.amount <= 0) continue;
            float scaledCost = stack.amount * costScale;
            if(scaledCost <= 0.0001f) continue;

            int required = Mathf.ceil(nextHealed / maxHealth * scaledCost);
            int paid = repairCostPaid.get(stack.item.id, 0);
            int delta = Math.max(required - paid, 0);
            if(delta > 0){
                resources.removeResource(stack.item, delta);
                repairCostPaid.put(stack.item.id, paid + delta);
            }
        }

        repairCostHealed = nextHealed;
        return true;
    }

    @Override
    public void updateMovement(){
        if(isScv()){
            updateScvMovement();
            return;
        }

        if(target instanceof Building){
            boolean shoot = false;

            if(target.within(unit, unit.range())){
                unit.aim(target);
                shoot = true;
            }

            unit.controlWeapons(shoot);
        }else if(target == null){
            unit.controlWeapons(false);
        }

        if(target != null && target instanceof Building b && b.team == unit.team){
            if(unit.type.circleTarget){
                circleAttack(unit.type.circleTargetRadius);
            }else if(!target.within(unit, unit.range() * 0.65f)){
                moveTo(target, unit.range() * 0.65f);
            }

            if(!unit.type.circleTarget){
                unit.lookAt(target);
            }
        }

        //not repairing
        if(!(target instanceof Building)){
            if(timer.get(timerTarget4, 40)){
                avoid = target(unit.x, unit.y, fleeRange, true, true);
            }

            if((retreatTimer += Time.delta) >= retreatDelay){
                //fly away from enemy when not doing anything
                if(avoid != null){
                    var core = unit.closestCore();
                    if(core != null && !unit.within(core, retreatDst)){
                        moveTo(core, retreatDst);
                    }
                }
            }
        }else{
            retreatTimer = 0f;
        }
    }

    @Override
    public void updateTargeting(){
        if(isScv()){
            if(!validScvRepairTarget(forcedTarget)){
                forcedTarget = null;
                if(target != null && !validScvRepairTarget(target)){
                    target = null;
                }
                if(target == null){
                    resetRepairCostState();
                }
            }else{
                target = forcedTarget;
            }
            return;
        }

        if(timer.get(timerTarget, 15)){
            damagedTarget = Units.findDamagedTile(unit.team, unit.x, unit.y);
            if(damagedTarget instanceof ConstructBuild) damagedTarget = null;
        }

        if(damagedTarget == null){
            super.updateTargeting();
        }else{
            this.target = damagedTarget;
        }
    }
}
