package mindustry.ai.types;

import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.ai.*;
import mindustry.core.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.payloads.*;
import mindustry.world.blocks.storage.CoreBlock.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class CommandAI extends AIController{
    protected static final int maxCommandQueueSize = 50, avoidInterval = 10;
    protected static final float centerArrivalThreshold = 0.2f;
    protected static final Vec2 vecOut = new Vec2(), vecMovePos = new Vec2();
    protected static final boolean[] noFound = {false};
    protected static final UnitPayload tmpPayload = new UnitPayload(null);
    protected static final int transferStateNone = 0, transferStateLoad = 1, transferStateUnload = 2;

    public Seq<Position> commandQueue = new Seq<>(5);
    public @Nullable Vec2 targetPos;
    public @Nullable Teamc attackTarget;
    /** Group of units that were all commanded to reach the same point. */
    public @Nullable UnitGroup group;
    public int groupIndex = 0;
    /** All encountered unreachable buildings of this AI. Why a sequence? Because contains() is very rarely called on it. */
    public IntSeq unreachableBuildings = new IntSeq(8);
    /** ID of unit read as target. This is set up after reading. Do not access! */
    public int readAttackTarget = -1;

    protected boolean stopAtTarget, stopWhenInRange;
    /** True when current targetPos was issued by force-attack to ground (A + empty ground). */
    public boolean attackMovePosition;
    protected Vec2 lastTargetPos;
    protected boolean blockingUnit;
    protected float timeSpentBlocked;
    protected float payloadPickupCooldown;
    protected int transferState = transferStateNone;

    /** Current command this unit is following. */
    public UnitCommand command;
    /** Stance, usually related to firing mode. Each bit is a stance ID. */
    public Bits stances = new Bits(content.unitStances().size);
    /** Current controller instance based on command. */
    protected @Nullable AIController commandController;
    /** Pending harvest target set by player command. */
    public @Nullable Vec2 pendingHarvestTarget;
    /** Rally follow target, used for building rally points. */
    public @Nullable Teamc followTarget;
    /** Queued follow target while commands are locked. */
    public @Nullable Teamc queuedFollowTarget;
    /** If true, follow is paused until the target moves. */
    private boolean followHold;
    private float followHoldX, followHoldY, followHoldDist;
    /** Last command type assigned. Used for detecting command changes. */
    protected @Nullable UnitCommand lastCommand;
    /** Queued command while the unit is locked inside a condenser. */
    public @Nullable UnitCommand queuedCommand;
    public @Nullable Vec2 queuedCommandPos;
    public @Nullable Teamc queuedCommandTarget;
    private boolean retainAttackTargetOnMove;

    public UnitCommand currentCommand(){
        return command == null ? UnitCommand.moveCommand : command;
    }

    /** Attempts to assign a command to this unit. If not supported by the unit type, does nothing. */
    public void command(UnitCommand command){
        if(commandLocked()){
            queuedCommand = command;
            return;
        }
        if(unit.type.commands.contains(command)){
            //clear old state.
            unit.mineTile = null;
            BuildPlan plan = unit.buildPlan();
            boolean keepBuild = plan != null && plan.requireClose;
            if(!keepBuild){
                unit.clearBuilding();
            }
            this.command = command;
            if(command != UnitCommand.harvestCommand){
                pendingHarvestTarget = null;
            }
        }
    }

    public boolean hasStance(@Nullable UnitStance stance){
        return stance != null && stances.get(stance.id);
    }

    public void setStance(UnitStance stance, boolean enabled){
         if(enabled){
             setStance(stance);
         }else{
             disableStance(stance);
         }
    }

    public void setStance(UnitStance stance){
        //this happens when an older save reads the default "shoot" stance, or any other removed stance
        if(stance == UnitStance.stop) return;

        stances.andNot(stance.incompatibleBits);
        stances.set(stance.id);
        stanceChanged();
    }

    public void disableStance(UnitStance stance){
        stances.clear(stance.id);
        stanceChanged();
    }

    public void stanceChanged(){
        if(commandController != null && !(commandController instanceof CommandAI)){
            commandController.stanceChanged();
        }
    }

    @Override
    public void init(){
        if(command == null){
            command = unit.type.defaultCommand == null && unit.type.commands.size > 0 ? unit.type.commands.first() : unit.type.defaultCommand;
            if(command == null) command = UnitCommand.moveCommand;
        }
    }

    @Override
    public boolean isLogicControllable(){
        return !hasCommand();
    }

    private boolean withinAttackEdgeRange(@Nullable Teamc target, float range){
        return target != null && Units.withinTargetRange(target, unit.x, unit.y, range, unit.hitSize / 2f);
    }

    private void forceAttackTargetInRange(){
        if(attackTarget == null) return;

        target = attackTarget;
        for(var mount : unit.mounts){
            Weapon weapon = mount.weapon;
            if(!weapon.controllable || weapon.noAttack || !weapon.aiControllable) continue;
            if(!weaponCanHitTarget(weapon, attackTarget)){
                mount.target = null;
                mount.rotate = false;
                mount.shoot = false;
                continue;
            }
            float weaponRange = weapon.range() + unit.hitSize / 2f;
            if(!Units.withinTargetRange(attackTarget, unit.x, unit.y, weaponRange, unit.hitSize / 2f)) continue;

            mount.target = attackTarget;
            mount.rotate = true;
            mount.shoot = true;

            float mountX = unit.x + Angles.trnsx(unit.rotation - 90f, weapon.x, weapon.y);
            float mountY = unit.y + Angles.trnsy(unit.rotation - 90f, weapon.x, weapon.y);
            Units.aimPoint(attackTarget, mountX, mountY, attackTarget.getX(), attackTarget.getY(), Tmp.v1);
            mount.aimX = Tmp.v1.x;
            mount.aimY = Tmp.v1.y;
            unit.aimX = Tmp.v1.x;
            unit.aimY = Tmp.v1.y;
        }
    }

    public boolean isAttacking(){
        return withinAttackEdgeRange(target, unit.range());
    }

    @Override
    public void updateUnit(){
        if(!commandLocked()){
            applyQueuedCommand();
        }

        if(command == UnitCommand.mineCommand && !hasStance(UnitStance.mineAuto) && !ItemUnitStance.all().contains(this::hasStance)){
            setStance(UnitStance.mineAuto);
        }

        //pursue the target if relevant
        if(hasStance(UnitStance.pursueTarget) && !hasStance(UnitStance.patrol) && target != null && attackTarget == null && targetPos == null){
            commandTarget(target, false);
        }

        //pursue the target for patrol, keeping the current position
        if(hasStance(UnitStance.patrol) && hasStance(UnitStance.pursueTarget) && target != null && attackTarget == null){
            //commanding a target overwrites targetPos, so add it to the queue
            if(targetPos != null){
                commandQueue.add(targetPos.cpy());
            }
            commandTarget(target, false);
        }

        //remove invalid targets
        if(commandQueue.any()){
            commandQueue.removeAll(e -> {
                if(!(e instanceof Healthc)) return false;
                return !((Healthc)e).isValid();
            });
        }

        //assign defaults
        if(command == null && unit.type.commands.size > 0){
            command = unit.type.defaultCommand == null ? unit.type.commands.first() : unit.type.defaultCommand;
        }

        //update command controller based on index.
        var curCommand = command;
        if(lastCommand != curCommand){
            lastCommand = curCommand;
            commandController = (curCommand == null ? null : curCommand.controller.get(unit));
        }

        //use the command controller if it is provided, and bail out.
        if(commandController != null){
            if(commandController.unit() != unit) commandController.unit(unit);
            if(pendingHarvestTarget != null && commandController instanceof HarvestAI){
                ((HarvestAI)commandController).setHarvestTarget(pendingHarvestTarget);
                pendingHarvestTarget = null;
            }
            commandController.updateUnit();
        }else{
            defaultBehavior();
            //boosting control is not supported, so just don't.
            unit.updateBoosting(false);
        }
    }

    public void clearCommands(){
        commandQueue.clear();
        targetPos = null;
        attackTarget = null;
        attackMovePosition = false;
        retainAttackTargetOnMove = false;
        followTarget = null;
    }

    void tryPickupUnit(Payloadc pay){
        float pickupRange = UnitTypes.isMedivac(unit) ? UnitTypes.medivacLoadRange() : unit.type.hitSize * 2f;
        Unit target = Units.closest(unit.team, unit.x, unit.y, pickupRange, u ->
            (UnitTypes.isMedivac(unit) || u.isAI()) && u != unit && u.isGrounded() && pay.canPickup(u) && u.within(unit, u.hitSize + unit.hitSize));
        if(target != null){
            Call.pickedUnitPayload(unit, target);
        }
    }

    @Override
    public Teamc findMainTarget(float x, float y, float range, boolean air, boolean ground){
        if(!unit.type.autoFindTarget && !(targetPos == null || nearAttackTarget(unit.x, unit.y, unit.range()))){
            return null;
        }
        return super.findMainTarget(x, y, range, air, ground);
    }

    public void defaultBehavior(){
        updateFollowTarget();

        if(!net.client() && unit instanceof Payloadc){
            Payloadc pay = (Payloadc)unit;
            payloadPickupCooldown -= Time.delta;

            //auto-drop everything
            if(command == UnitCommand.unloadPayloadCommand && pay.hasPayload()){
                boolean allowDrop = !UnitTypes.isMedivac(unit)
                    || UnitTypes.medivacMovingUnload(unit)
                    || targetPos == null
                    || unit.within(targetPos, 10f);
                if(allowDrop && (!UnitTypes.isMedivac(unit) || payloadPickupCooldown <= 0f)){
                    Call.payloadDropped(unit, unit.x, unit.y);
                    if(UnitTypes.isMedivac(unit)){
                        payloadPickupCooldown = 30f; // 0.5s interval
                    }
                }
            }

            //try to pick up what's under it
            if(command == UnitCommand.loadUnitsCommand){
                tryPickupUnit(pay);
            }

            //try to pick up a block
            if(command == UnitCommand.loadBlocksCommand && (targetPos == null || unit.within(targetPos, 1f))){
                Building build = world.buildWorld(unit.x, unit.y);

                if(build != null && state.teams.canInteract(unit.team, build.team)){
                    //pick up block's payload
                    Payload current = build.getPayload();
                    if(current != null && pay.canPickupPayload(current)){
                        Call.pickedBuildPayload(unit, build, false);
                        //pick up whole building directly
                    }else if(build.block.buildVisibility != BuildVisibility.hidden && build.canPickup() && pay.canPickup(build)){
                        Call.pickedBuildPayload(unit, build, true);
                    }
                }
            }
        }

        if(!net.client() && command == UnitCommand.enterPayloadCommand && unit.buildOn() != null && (targetPos == null || (world.buildWorld(targetPos.x, targetPos.y) != null && world.buildWorld(targetPos.x, targetPos.y) == unit.buildOn()))){
            var build = unit.buildOn();
            tmpPayload.unit = unit;
            if(build.team == unit.team && build.acceptPayload(build, tmpPayload)){
                Call.unitEnteredPayload(unit, build);
                return; //no use updating after this, the unit is gone!
            }
        }

        updateVisuals();
        updateTargeting();

        if(attackTarget != null && invalid(attackTarget)){
            attackTarget = null;
            if(!retainAttackTargetOnMove){
                targetPos = null;
            }
            attackMovePosition = false;
            retainAttackTargetOnMove = false;
        }

        //move on to the next target
        if(attackTarget == null && targetPos == null){
            finishPath();
        }

        boolean ramming = hasStance(UnitStance.ram);

        if(attackTarget != null){
            if(!retainAttackTargetOnMove){
                if(targetPos == null){
                    targetPos = new Vec2();
                    lastTargetPos = targetPos;
                }
                targetPos.set(attackTarget);

                if(unit.isGrounded() && attackTarget instanceof Building && unit.type.pathCostId != ControlPathfinder.costIdLegs && !ramming){
                    Building build = (Building)attackTarget;
                    if(build.tile.solid()){
                        Tile best = build.findClosestEdge(unit, Tile::solid);
                        if(best != null){
                            targetPos.set(best);
                        }
                    }
                }
            }
        }

        boolean alwaysArrive = false;

        float engageRange = unit.range();
        boolean withinAttackRange = attackTarget != null && withinAttackEdgeRange(attackTarget, engageRange) && !ramming;
        if(forcedFriendlyAttackTarget(attackTarget)){
            //For forced ally attack commands, stop once any valid weapon can actually engage.
            withinAttackRange = forcedFriendlyAttackInWeaponRange(attackTarget) && !ramming;
        }
        if(withinAttackRange){
            forceAttackTargetInRange();
        }

        if(targetPos != null){
            boolean move = true, isFinalPoint = commandQueue.size == 0;
            vecOut.set(targetPos);
            vecMovePos.set(targetPos);
            Vec2 pathTarget = targetPos;
            float buildFinishRange = -1f;

            //the enter payload command requires an exact position
            if(group != null && group.valid && groupIndex < group.units.size && command != UnitCommand.enterPayloadCommand){
                vecMovePos.add(group.positions[groupIndex * 2], group.positions[groupIndex * 2 + 1]);
            }

            BuildPlan plan = unit.buildPlan();
            if(plan != null && plan.requireClose && plan.block != null && targetPos != null &&
                Mathf.equal(targetPos.x, plan.drawx()) && Mathf.equal(targetPos.y, plan.drawy())){
                float cx = plan.drawx(), cy = plan.drawy();
                float half = plan.block.size * tilesize / 2f;
                float unitRadius = unit.hitSize / 2f;
                float edge = half + unitRadius + 0.1f;
                float bestX = cx + edge, bestY = cy;
                float dx = unit.x - bestX, dy = unit.y - bestY;
                float bestDst = dx * dx + dy * dy;

                float altX = cx - edge, altY = cy;
                dx = unit.x - altX;
                dy = unit.y - altY;
                float dst = dx * dx + dy * dy;
                if(dst < bestDst){
                    bestDst = dst;
                    bestX = altX;
                    bestY = altY;
                }

                altX = cx;
                altY = cy + edge;
                dx = unit.x - altX;
                dy = unit.y - altY;
                dst = dx * dx + dy * dy;
                if(dst < bestDst){
                    bestDst = dst;
                    bestX = altX;
                    bestY = altY;
                }

                altX = cx;
                altY = cy - edge;
                dx = unit.x - altX;
                dy = unit.y - altY;
                dst = dx * dx + dy * dy;
                if(dst < bestDst){
                    bestX = altX;
                    bestY = altY;
                }

                vecMovePos.set(bestX, bestY);
                pathTarget = vecMovePos;
                buildFinishRange = edge;
            }

            Building targetBuild = world.buildWorld(targetPos.x, targetPos.y);

            //TODO: should the unit stop when it finds a target?
            if(
                (hasStance(UnitStance.patrol) && !hasStance(UnitStance.pursueTarget) && target != null && unit.within(target, unit.range() - 2f) && !unit.type.circleTarget) ||
                (command == UnitCommand.enterPayloadCommand && unit.within(targetPos, 4f) || (targetBuild != null && unit.within(targetBuild, targetBuild.block.size * tilesize/2f * 0.9f))) ||
                (command == UnitCommand.loopPayloadCommand && unit.within(vecMovePos, 10f))
            ){
                move = false;
            }

            if(unit.isGrounded() && !ramming){
                //TODO: blocking enable or disable?
                if(timer.get(timerTarget3, avoidInterval)){
                    Vec2 dstPos = Tmp.v1.trns(unit.rotation, unit.hitSize/2f);
                    float max = unit.hitSize/2f;
                    float radius = Math.max(7f, max);
                    float margin = 4f;
                    blockingUnit = Units.nearbyCheck(unit.x + dstPos.x - radius/2f, unit.y + dstPos.y - radius/2f, radius, radius, u -> {
                        if(u == unit) return false;
                        if(!u.within(unit, u.hitSize/2f + unit.hitSize/2f + margin)) return false;
                        if(!(u.controller() instanceof CommandAI)) return false;
                        CommandAI ai = (CommandAI)u.controller();
                        if(ai.targetPos == null) return false;
                        //stop for other unit only if it's closer to the target
                        if(!(ai.targetPos.equals(targetPos) && u.dst2(targetPos) < unit.dst2(targetPos))) return false;
                        //don't stop if they're facing the same way
                        if(Angles.within(unit.rotation, u.rotation, 15f)) return false;
                        //must be near an obstacle, stopping in open ground is pointless
                        return ControlPathfinder.isNearObstacle(unit, unit.tileX(), unit.tileY(), u.tileX(), u.tileY());
                    });
                }

                float maxBlockTime = 60f * 5f;

                if(blockingUnit){
                    timeSpentBlocked += Time.delta;

                    if(timeSpentBlocked >= maxBlockTime*2f){
                        timeSpentBlocked = 0f;
                    }
                }else{
                    timeSpentBlocked = 0f;
                }

                //if the unit is next to the target, stop asking the pathfinder how to get there, it's a waste of CPU
                //TODO maybe stop moving too?
                if(withinAttackRange){
                    move = false;
                    noFound[0] = false;
                    vecOut.set(vecMovePos);
                    if(unit.isGrounded()){
                        unit.vel.setZero();
                    }
                }else{
                    move &= controlPath.getPathPosition(unit, vecMovePos, pathTarget, vecOut, noFound) && (!blockingUnit || timeSpentBlocked > maxBlockTime);

                    //TODO: what to do when there's a target and it can't be reached?
                    /*
                    if(noFound[0] && attackTarget != null && attackTarget.within(unit, unit.range() * 2f)){
                        move = true;
                        vecOut.set(targetPos);
                    }*/
                }

                //rare case where unit must be perfectly aligned (happens with 1-tile gaps)
                alwaysArrive = vecOut.epsilonEquals(unit.tileX() * tilesize, unit.tileY() * tilesize);
                //we've reached the final point if the returned coordinate is equal to the supplied input
                isFinalPoint &= vecMovePos.epsilonEquals(vecOut, 4.1f);

                //if the path is invalid, stop trying and record the end as unreachable
                if(unit.team.isAI() && (noFound[0] || unit.isPathImpassable(World.toTile(vecMovePos.x), World.toTile(vecMovePos.y)))){
                    if(attackTarget instanceof Building){
                        Building build = (Building)attackTarget;
                        unreachableBuildings.addUnique(build.pos());
                    }
                    attackTarget = null;
                    finishPath();
                    return;
                }
            }else{
                vecOut.set(vecMovePos);
            }

            if(command == UnitCommand.loopPayloadCommand){
                alwaysArrive = true;
            }

            if(move){
                if(unit.type.circleTarget && attackTarget != null){
                    target = attackTarget;
                    circleAttack(unit.type.circleTargetRadius);
                }else{
                    moveTo(vecOut,
                    withinAttackRange ? engageRange :
                    unit.isGrounded() ? 0f :
                    attackTarget != null && !ramming ? engageRange : 0f,
                    unit.isFlying() ? 40f : 100f, false, null, isFinalPoint || alwaysArrive);
                }
            }

            //if stopAtTarget is set, stop trying to move to the target once it is reached - used for defending
            if(attackTarget != null && stopAtTarget && withinAttackEdgeRange(attackTarget, Math.max(0f, engageRange - 1f))){
                attackTarget = null;
            }

            if(unit.isFlying()){
                if(attackTarget != null){
                    unit.lookAt(attackTarget);
                }else if(move && !(unit.type.circleTarget && !unit.type.omniMovement)){
                    unit.lookAt(vecMovePos);
                }else{
                    faceTarget();
                }
            }else{
                if(attackTarget != null){
                    unit.lookAt(attackTarget);
                }else{
                    faceTarget();
                }
            }

            boolean groupedMove = group != null && group.valid && groupIndex < group.units.size;

            //reached destination, end pathfinding
            if(attackTarget == null || retainAttackTargetOnMove){
                float finishRange;
                Position finishPos = vecMovePos;
                if(command == UnitCommand.enterPayloadCommand){
                    finishRange = 4f;
                    finishPos = targetPos;
                }else if(command == UnitCommand.loopPayloadCommand){
                    finishRange = 10f;
                }else{
                    if(buildFinishRange > 0f && targetPos != null){
                        finishRange = buildFinishRange;
                        finishPos = targetPos;
                    }else{
                        finishRange = groupedMove ? (command.exactArrival && commandQueue.size == 0 ? 1f : Math.max(5f, unit.hitSize / 2f)) : centerArrivalThreshold;
                    }
                }
                if(finishPos != null && unit.within(finishPos, finishRange)){
                    if(!groupedMove){
                        unit.vel.setZero();
                    }
                    if(retainAttackTargetOnMove){
                        attackTarget = null;
                    }
                    finishPath();
                }
            }

            if(stopWhenInRange && targetPos != null && unit.within(vecMovePos, groupedMove ? engageRange * 0.9f : centerArrivalThreshold)){
                if(!groupedMove){
                    unit.vel.setZero();
                }
                finishPath();
                stopWhenInRange = false;
            }

        }else if(target != null){
            if(unit.type.circleTarget && shouldFire()){
                circleAttack(unit.type.circleTargetRadius);
            }else{
                faceTarget();
            }
        }
    }

    /** Sets a crystal harvest target for this unit, forcing the harvest command. */
    public void setHarvestTarget(Vec2 target){
        if(target == null) return;
        pendingHarvestTarget = target.cpy();
        if(command != UnitCommand.harvestCommand){
            command(UnitCommand.harvestCommand);
        }else if(commandController instanceof HarvestAI){
            ((HarvestAI)commandController).setHarvestTarget(pendingHarvestTarget);
            pendingHarvestTarget = null;
        }
    }

    void finishPath(){
        //the enter payload command never finishes until they are actually accepted
        if(command == UnitCommand.enterPayloadCommand && commandQueue.size == 0 && targetPos != null && world.buildWorld(targetPos.x, targetPos.y) != null && world.buildWorld(targetPos.x, targetPos.y).block.acceptsUnitPayloads){
            return;
        }

        if(!net.client() && command == UnitCommand.loopPayloadCommand && unit instanceof Payloadc){
            Payloadc pay = (Payloadc)unit;

            if(transferState == transferStateNone){
                transferState = pay.hasPayload() ? transferStateUnload : transferStateLoad;
            }

            if(payloadPickupCooldown > 0f) return;

            if(transferState == transferStateUnload){
                //drop until there's a failure
                int prev = -1;
                while(pay.hasPayload() && prev != pay.payloads().size){
                    prev = pay.payloads().size;
                    Call.payloadDropped(unit, unit.x, unit.y);
                }

                //wait for everything to unload before running code below
                if(pay.hasPayload()){
                    return;
                }
                payloadPickupCooldown = 60f;
            }else if(transferState == transferStateLoad){
                //pick up units until there's a failure
                int prev = -1;
                while(prev != pay.payloads().size){
                    prev = pay.payloads().size;
                    tryPickupUnit(pay);
                }

                //wait to load things before running code below
                if(!pay.hasPayload()){
                    return;
                }
                payloadPickupCooldown = 60f;
            }

            //it will never finish
            if(commandQueue.size == 0){
                return;
            }
        }

        if(command == UnitCommand.moveCommand && commandQueue.size == 0 && targetPos != null && unit.hasItem()){
            Building targetBuild = world.buildWorld(targetPos.x, targetPos.y);
            if(targetBuild instanceof CoreBuild && targetBuild.team == unit.team){
                CoreBuild core = (CoreBuild)targetBuild;
                float touchRange = unit.hitSize / 2f + core.hitSize() / 2f + 1f;
                if(unit.within(core, touchRange)){
                    Item carried = unit.item();
                    int accepted = core.acceptStack(unit.item(), unit.stack.amount, unit);
                    Call.transferItemTo(unit, unit.item(), accepted, core.x, core.y, core);
                    unit.clearItem();

                    if(unit.type.commands.contains(UnitCommand.harvestCommand)){
                        float searchRadius = tilesize * 15f;
                        Tile next = carried == Items.highEnergyGas
                            ? HarvestAI.findNearestGasTile(unit.x, unit.y, searchRadius, unit.team)
                            : HarvestAI.findNearestCrystalTile(unit.x, unit.y, searchRadius);

                        targetPos = null;
                        attackTarget = null;
                        attackMovePosition = false;
                        retainAttackTargetOnMove = false;
                        commandQueue.clear();

                        if(next != null){
                            setHarvestTarget(Tmp.v1.set(next.worldx(), next.worldy()));
                        }
                        return;
                    }
                }
            }
        }

        transferState = transferStateNone;

        Vec2 prev = targetPos;
        targetPos = null;
        attackMovePosition = false;
        retainAttackTargetOnMove = false;

        if(commandQueue.size > 0){
            Position next = commandQueue.remove(0);
            if(next instanceof Teamc){
                commandTarget((Teamc)next, this.stopAtTarget);
            }else if(next instanceof Vec2){
                commandPosition((Vec2)next);
            }

            if(prev != null && (hasStance(UnitStance.patrol) || command == UnitCommand.loopPayloadCommand)){
                commandQueue.add(prev.cpy());
            }

            //make sure spot in formation is reachable
            if(group != null){
                if(next instanceof Vec2){
                    group.updateRaycast(groupIndex, (Vec2)next);
                }else{
                    group.updateRaycast(groupIndex, Tmp.v3.set(next));
                }
            }
        }else{
            if(group != null){
                group = null;
            }
        }
    }

    private void updateFollowTarget(){
        if(followTarget == null) return;

        boolean valid = true;
        if(followTarget instanceof Healthc){
            Healthc h = (Healthc)followTarget;
            if(!h.isValid()) valid = false;
        }
        if(valid && followTarget instanceof Teamc){
            Teamc t = (Teamc)followTarget;
            if(t.team() != unit.team) valid = false;
        }

        if(!valid){
            followTarget = null;
            targetPos = null;
            return;
        }

        if(followTarget instanceof Building){
            Building build = (Building)followTarget;
            float targetRadius = build.hitSize() / 2f;
            float desired = targetRadius + unit.hitSize / 2f;
            float tx = build.x, ty = build.y;

            if(unit.dst2(tx, ty) <= desired * desired){
                followTarget = null;
                targetPos = null;
                return;
            }

            if(targetPos == null){
                targetPos = new Vec2();
                lastTargetPos = targetPos;
            }

            Tmp.v1.set(unit.x - tx, unit.y - ty);
            if(Tmp.v1.isZero(0.001f)){
                Tmp.v1.set(1f, 0f);
            }
            Tmp.v1.setLength(desired);
            targetPos.set(tx + Tmp.v1.x, ty + Tmp.v1.y);
            attackTarget = null;
            attackMovePosition = false;
            retainAttackTargetOnMove = false;
            return;
        }

        float tx = followTarget.getX();
        float ty = followTarget.getY();
        if(followHold){
            float resumeDst = followHoldDist;
            if(Mathf.dst2(tx, ty, followHoldX, followHoldY) > resumeDst * resumeDst){
                followHold = false;
            }else{
                targetPos = null;
                attackMovePosition = false;
                retainAttackTargetOnMove = false;
                return;
            }
        }

        float targetRadius = 0f;
        if(followTarget instanceof Sized){
            targetRadius = ((Sized)followTarget).hitSize() / 2f;
        }
        float desired = (targetRadius + unit.hitSize / 2f) * 1.05f;
        if(unit.dst2(tx, ty) <= desired * desired){
            followHold = true;
            followHoldX = tx;
            followHoldY = ty;
            followHoldDist = desired;
            targetPos = null;
            attackMovePosition = false;
            retainAttackTargetOnMove = false;
            return;
        }

        if(isFollowBlocked(followTarget)){
            followHold = true;
            followHoldX = tx;
            followHoldY = ty;
            followHoldDist = desired;
            targetPos = null;
            attackMovePosition = false;
            retainAttackTargetOnMove = false;
            return;
        }

        if(targetPos == null){
            targetPos = new Vec2();
            lastTargetPos = targetPos;
        }

        Tmp.v1.set(unit.x - tx, unit.y - ty);
        if(Tmp.v1.isZero(0.001f)){
            Tmp.v1.set(1f, 0f);
        }
        Tmp.v1.setLength(desired);
        targetPos.set(tx + Tmp.v1.x, ty + Tmp.v1.y);
        attackTarget = null;
        attackMovePosition = false;
        retainAttackTargetOnMove = false;
    }

    private boolean isFollowBlocked(Teamc target){
        float unitRadius = unit.hitSize / 2f;
        float checkRad = unitRadius + 0.5f;

        boolean blocked = Units.nearbyCheck(unit.x - checkRad, unit.y - checkRad, checkRad * 2f, checkRad * 2f, u -> {
            if(u == unit || u == target) return false;
            if(!u.isValid()) return false;
            float dst = unit.dst(u);
            float min = unitRadius + u.hitSize / 2f + 0.1f;
            return dst < min;
        });
        if(blocked) return true;

        float range = checkRad + maxBlockSize * tilesize / 2f;
        Tmp.r1.setCentered(unit.x, unit.y, range * 2f, range * 2f);
        boolean[] hit = {false};

        for(mindustry.game.Teams.TeamData data : state.teams.present){
            if(hit[0]) break;
            var tree = data.buildingTree;
            if(tree == null) continue;
            tree.intersect(Tmp.r1, b -> {
                if(hit[0]) return;
                if(b == target) return;
                if(!(b.block.solid || b.checkSolid())) return;
                float br = b.block.size * tilesize / 2f;
                float rs = br + unitRadius;
                if(Mathf.dst2(unit.x, unit.y, b.x, b.y) < rs * rs){
                    hit[0] = true;
                }
            });
        }

        return hit[0];
    }

    @Override
    public void removed(Unit unit){
        clearCommands();
    }

    public void commandQueue(Position location){
        if(targetPos == null && attackTarget == null){
            if(location instanceof Teamc){
                commandTarget((Teamc)location, this.stopAtTarget);
            }else if(location instanceof Vec2){
                commandPosition((Vec2)location);
            }
        }else if(commandQueue.size < maxCommandQueueSize && !commandQueue.contains(location)){
            commandQueue.add(location);
        }
    }

    @Override
    public void afterRead(Unit unit){
        if(readAttackTarget != -1){
            attackTarget = Groups.unit.getByID(readAttackTarget);
            readAttackTarget = -1;
        }
    }

    @Override
    public boolean shouldFire(){
        return !hasStance(UnitStance.holdFire);
    }

    @Override
    public void hit(Bullet bullet){
        if(unit.team.isAI() && bullet.owner instanceof Teamc && ((Teamc)bullet.owner).team() != unit.team && attackTarget == null &&
            //can only counter-attack every few seconds to prevent rapidly changing targets
            !(bullet.owner instanceof Unit && !((Unit)bullet.owner).checkTarget(unit.type.targetAir, unit.type.targetGround)) && timer.get(timerTarget4, 60f * 10f)){
            commandTarget((Teamc)bullet.owner, true);
        }
    }

    @Override
    public boolean keepState(){
        return true;
    }

    @Override
    public Teamc findTarget(float x, float y, float range, boolean air, boolean ground){
        return !nearAttackTarget(x, y, range) ? super.findTarget(x, y, range, air, ground) : Units.isHittable(attackTarget, air, ground) ? attackTarget : null;
    }

    public boolean nearAttackTarget(float x, float y, float range){
        return attackTarget != null && Units.withinTargetRange(attackTarget, x, y, range + 3f, unit.hitSize / 2f);
    }

    private boolean forcedFriendlyAttackTarget(@Nullable Teamc target){
        return target != null && target == attackTarget && unit != null && attackTarget != null && attackTarget.team() == unit.team;
    }

    private boolean weaponCanHitTarget(Weapon weapon, @Nullable Teamc target){
        if(target == null) return false;
        if(target instanceof Unit u){
            return u.isFlying() ? weapon.bullet.collidesAir : weapon.bullet.collidesGround;
        }
        if(target instanceof Building b){
            return Units.canTargetBuilding(weapon.bullet.collidesAir, weapon.bullet.collidesGround, b) &&
                !Units.preferGroundWeapons(unit, weapon.bullet.collidesAir, weapon.bullet.collidesGround, b);
        }
        return false;
    }

    private boolean forcedFriendlyAttackInWeaponRange(@Nullable Teamc target){
        if(!forcedFriendlyAttackTarget(target)) return false;
        for(var mount : unit.mounts){
            Weapon weapon = mount.weapon;
            if(!weapon.controllable || weapon.noAttack || !weapon.aiControllable) continue;
            if(!weaponCanHitTarget(weapon, target)) continue;
            float weaponRange = weapon.range() + unit.hitSize / 2f;
            if(Units.withinTargetRange(target, unit.x, unit.y, weaponRange, unit.hitSize / 2f)){
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean invalid(Teamc target){
        if(forcedFriendlyAttackTarget(target)){
            return target instanceof Healthc h && !h.isValid();
        }
        return super.invalid(target);
    }

    @Override
    public boolean checkTarget(Teamc target, float x, float y, float range){
        if(forcedFriendlyAttackTarget(target)){
            if(range != Float.MAX_VALUE && !Units.withinTargetRange(target, x, y, range, unit.hitSize / 2f)){
                return true;
            }
            return target instanceof Healthc h && !h.isValid();
        }
        return super.checkTarget(target, x, y, range);
    }

    @Override
    public boolean retarget(){
        //retarget faster when there is an explicit target
        return timer.get(timerTarget, attackTarget != null ? 10f : 20f);
    }

    public boolean hasCommand(){
        return targetPos != null;
    }

    public void setupLastPos(){
        lastTargetPos = targetPos;
    }

    @Override
    public void commandPosition(Vec2 pos){
        if(pos == null) return;

        commandPosition(pos, false, false);
        if(commandController != null){
            commandController.commandPosition(pos);
        }
    }

    public void commandPosition(Vec2 pos, boolean stopWhenInRange){
        commandPosition(pos, stopWhenInRange, false);
    }

    public void commandPosition(Vec2 pos, boolean stopWhenInRange, boolean attackMovePosition){
        if(pos == null) return;
        if(commandLocked()){
            queuedCommandPos = pos.cpy();
            return;
        }

        followTarget = null;

        //this is an allocation, but it's relatively rarely called anyway, and outside mutations must be prevented
        targetPos = lastTargetPos = pos.cpy();
        if(command != null && command.snapToBuilding){
            var build = world.buildWorld(targetPos.x, targetPos.y);
            if(build != null && build.team == unit.team){
                targetPos.set(build);
            }
        }
        boolean retainAttack = UnitTypes.isBattlecruiser(unit) && attackTarget != null && !attackMovePosition;
        if(!retainAttack){
            attackTarget = null;
        }
        retainAttackTargetOnMove = retainAttack;
        this.stopWhenInRange = stopWhenInRange;
        this.attackMovePosition = attackMovePosition;
    }

    @Override
    public void commandTarget(Teamc moveTo){
        commandTarget(moveTo, false);
        if(commandController != null){
            commandController.commandTarget(moveTo);
        }
    }

    public void commandTarget(Teamc moveTo, boolean stopAtTarget){
        if(commandLocked()){
            queuedCommandTarget = moveTo;
            return;
        }
        followTarget = null;
        attackTarget = moveTo;
        attackMovePosition = false;
        retainAttackTargetOnMove = false;
        this.stopAtTarget = stopAtTarget;
    }

    public void commandFollow(Teamc target){
        if(target == null) return;
        if(commandLocked()){
            queuedFollowTarget = target;
            return;
        }
        if(command == null || command.switchToMove){
            command(UnitCommand.moveCommand);
        }
        followHold = false;
        followTarget = target;
        attackTarget = null;
        targetPos = null;
        attackMovePosition = false;
        retainAttackTargetOnMove = false;
        commandQueue.clear();
    }

    public void applyQueuedCommand(){
        if(queuedCommand == null && queuedCommandPos == null && queuedCommandTarget == null && queuedFollowTarget == null) return;

        UnitCommand next = queuedCommand != null ? queuedCommand : command;
        if(next != null){
            command(next);
        }
        if(queuedFollowTarget != null){
            commandFollow(queuedFollowTarget);
        }else if(queuedCommandTarget != null){
            commandTarget(queuedCommandTarget, false);
        }else if(queuedCommandPos != null){
            commandPosition(queuedCommandPos, false);
        }

        queuedCommand = null;
        queuedCommandPos = null;
        queuedCommandTarget = null;
        queuedFollowTarget = null;
    }

    private boolean commandLocked(){
        return unit != null && (unit.harvestHidden || unit.activelyBuilding());
    }

}
