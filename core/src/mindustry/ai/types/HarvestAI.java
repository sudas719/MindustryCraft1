package mindustry.ai.types;

import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.ai.UnitCommand;
import mindustry.content.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.world.*;
import mindustry.world.blocks.environment.*;

import static mindustry.Vars.*;

public class HarvestAI extends AIController{
    protected static final Vec2 vecOut = new Vec2(), vecMovePos = new Vec2(), vecMainPos = new Vec2();
    protected static final boolean[] noFound = {false};
    // Active miners per tile
    public static ObjectMap<Tile, Seq<Unit>> activeNovas = new ObjectMap<>();
    public static ObjectMap<Tile, Seq<Unit>> activePulsars = new ObjectMap<>();
    public static ObjectMap<Tile, Seq<Unit>> novaQueue = new ObjectMap<>();
    public static float lastCleanupTime = 0f;
    public static final float cleanupInterval = 60f; // Clean up every 1 second

    // States
    public enum HarvestState{
        SEEKING,     // Looking for ore to harvest
        HARVESTING,  // Moving to ore and harvesting
        RETURNING    // Returning to core to deposit
    }

    public HarvestState state = HarvestState.SEEKING;
    public Tile harvestTarget;
    public @Nullable Tile forcedTarget;
    public boolean forcedByPlayer;
    public @Nullable Tile miningTile;
    public @Nullable Tile queuedTile;
    public float harvestTimer;
    public Vec2 lastHarvestPos = new Vec2();
    public Building targetCore;
    public float seekTimer = 0f; // Throttle seeking attempts

    public static final float harvestTime = 150f; // 2.5 seconds
    public static final float harvestRange = tilesize; // 1 tile distance
    public static final float searchRadius = 10f * tilesize; // 10x10 grid
    public static final float maxSearchRadius = 50f * tilesize; // Maximum search radius
    public static final float seekInterval = 30f; // Only seek every 0.5 seconds
    public static final float contactBuffer = 0.5f; // Extra spacing when treating collision boxes as touching

    /** Set a specific target for harvesting (called when player right-clicks on ore) */
    public void setHarvestTarget(Vec2 target){
        if(target == null) return;
        Tile tile = world.tileWorld(target.x, target.y);
        if(tile == null || !(tile.block() instanceof CrystalMineralWall)) return;
        forcedTarget = tile;
        forcedByPlayer = true;
        harvestTarget = tile;
        harvestTimer = 0f;
        state = HarvestState.HARVESTING;
    }

    @Override
    public void updateMovement(){
        if(unit.hasItem() && state != HarvestState.RETURNING){
            clearMiningState();
            state = HarvestState.RETURNING;
        }

        // Clean up invalid reservations periodically (not every frame!)
        if(Time.time - lastCleanupTime > cleanupInterval){
            lastCleanupTime = Time.time;
            cleanupMap(activeNovas);
            cleanupMap(activePulsars);
            cleanupQueue(novaQueue);
        }

        switch(state){
            case SEEKING -> updateSeeking();
            case HARVESTING -> updateHarvesting();
            case RETURNING -> updateReturning();
        }
    }

    void updateSeeking(){
        // Throttle seeking attempts to reduce CPU usage
        seekTimer += Time.delta;
        if(seekTimer < seekInterval){
            return;
        }
        seekTimer = 0f;

        // Check if there's a forced target first
        if(forcedTarget != null){
            if(forcedTarget.block() instanceof CrystalMineralWall){
                harvestTarget = forcedTarget;
                harvestTimer = 0f;
                state = HarvestState.HARVESTING;
                return;
            }else{
                forcedTarget = null;
                forcedByPlayer = false;
            }
        }

        // Find nearest crystal mineral
        Tile nearest = findNearestOre();

        if(nearest != null){
            harvestTarget = nearest;
            harvestTimer = 0f;
            state = HarvestState.HARVESTING;
        }else{
            // No ore found, idle
            if(unit.moving()) unit.lookAt(unit.vel.angle());
        }
    }

    void updateHarvesting(){
        if(harvestTarget == null || !(harvestTarget.block() instanceof CrystalMineralWall)){
            // Ore disappeared or was mined out
            clearMiningState();
            if(harvestTarget == forcedTarget){
                forcedTarget = null;
                forcedByPlayer = false;
            }
            harvestTarget = null;
            state = HarvestState.SEEKING;
            return;
        }

        // Always face the target while moving towards it
        unit.lookAt(harvestTarget.worldx(), harvestTarget.worldy());

        // Move to the point where collision boxes just touch
        Rect oreRect = harvestTarget.getHitbox(Tmp.r1);
        Tile approachTile = findApproachTile(harvestTarget, unit.x, unit.y);
        Vec2 oreContact = contactPoint(oreRect,
            approachTile == null ? unit.x : approachTile.worldx(),
            approachTile == null ? unit.y : approachTile.worldy(),
            Tmp.v1);
        if(touching(oreRect)){
            stopAtContact(oreRect, oreContact);
        }else{
            moveToContact(oreContact, approachTile, 20f);
        }

        // Only evaluate real-time mining info when trying to harvest
        if(!touching(oreRect)){
            clearMiningState();
            return;
        }

        boolean miningLocked = miningTile == harvestTarget;
        if(!miningLocked){
            if(!canMineHere(harvestTarget)){
                harvestTimer = 0f;
                if(isNova(unit)){
                    Tile anchor = forcedTarget != null ? forcedTarget : harvestTarget;
                    Tile better = forcedByPlayer ? findNearestFreeOre(anchor.worldx(), anchor.worldy(), searchRadius)
                        : findOreNear(anchor.worldx(), anchor.worldy(), searchRadius, true);
                    if(better != null && better != harvestTarget){
                        int currentCount = novaInterestCount(harvestTarget);
                        int betterCount = novaInterestCount(better);
                        if(currentCount - betterCount >= 2){
                            boolean keepForcedQueue = forcedByPlayer && forcedTarget != null;
                            clearMiningState();
                            if(keepForcedQueue){
                                queueNova(forcedTarget);
                            }
                            harvestTarget = better;
                            harvestTimer = 0f;
                        }
                    }
                }
                return;
            }
        }

        startMining(harvestTarget);

        // Harvest
        harvestTimer += Time.delta;

        if(harvestTimer >= harvestTime){
            // Harvest complete
            CrystalMineralWall crystal = (CrystalMineralWall)harvestTarget.block();
            int amount = crystal.mineAmount(harvestTarget, unit);

            // Add to unit inventory
            unit.addItem(crystal.itemDrop, amount);

            // Decrease reserves
            if(!crystal.isInfinite(harvestTarget)){
                int reserves = crystal.getReserves(harvestTarget);
                int remaining = reserves - amount;
                if(remaining <= 0){
                    harvestTarget.setNet(Blocks.air);
                }else{
                    crystal.setReserves(harvestTarget, remaining);
                }
            }

            // Remember position and transition to returning
            lastHarvestPos.set(harvestTarget.worldx(), harvestTarget.worldy());
            clearMiningState();
            harvestTarget = null;
            state = HarvestState.RETURNING;
        }
    }

    void updateReturning(){
        clearMiningState();

        // Find closest core
        if(targetCore == null || !targetCore.isValid()){
            targetCore = unit.closestCore();
        }

        if(targetCore == null){
            // No core available, drop items
            unit.clearItem();
            state = HarvestState.SEEKING;
            return;
        }

        // Always face the core while moving towards it
        unit.lookAt(targetCore.x, targetCore.y);

        // Move to the point where collision boxes just touch the core
        Rect coreRect = Tmp.r1.setSize(targetCore.block.size * tilesize).setCenter(targetCore.x, targetCore.y);
        Tile coreApproach = unit.isGrounded() ? targetCore.findClosestEdge(unit, t -> !isPassable(t)) : null;
        Vec2 coreContact = contactPoint(coreRect,
            coreApproach == null ? unit.x : coreApproach.worldx(),
            coreApproach == null ? unit.y : coreApproach.worldy(),
            Tmp.v1);
        if(touching(coreRect)){
            stopAtContact(coreRect, coreContact);
        }else{
            moveToContact(coreContact, coreApproach, 100f);
        }

        // Deposit only when collision boxes are touching
        if(touching(coreRect)){
            // Deposit items
            if(unit.hasItem()){
                int accepted = targetCore.acceptStack(unit.item(), unit.stack.amount, unit);
                Call.transferItemTo(unit, unit.item(), accepted,
                    targetCore.x, targetCore.y, targetCore);
                unit.clearItem();
            }

            // Return to last harvest position
            state = HarvestState.SEEKING;
        }
    }

    float collisionRadius(){
        return unit.type.hitSize * unitCollisionRadiusScale;
    }

    boolean touching(Rect rect){
        return Intersector.overlaps(Tmp.cr1.set(unit.x, unit.y, collisionRadius() + contactBuffer), rect);
    }

    Vec2 contactPoint(Rect rect, float fromX, float fromY, Vec2 out){
        float cx = Mathf.clamp(fromX, rect.x, rect.x + rect.width);
        float cy = Mathf.clamp(fromY, rect.y, rect.y + rect.height);
        float radius = collisionRadius() + contactBuffer;

        out.set(fromX - cx, fromY - cy);
        if(out.isZero(0.001f)){
            float rx = rect.x + rect.width / 2f;
            float ry = rect.y + rect.height / 2f;
            out.set(fromX - rx, fromY - ry);
            if(out.isZero(0.001f)){
                out.set(1f, 0f);
            }
        }

        out.setLength(radius);
        out.add(cx, cy);
        return out;
    }

    Vec2 contactPoint(Rect rect, Vec2 out){
        return contactPoint(rect, unit.x, unit.y, out);
    }

    void stopAtContact(Rect rect, Vec2 contact){
        if(rect.contains(unit.x, unit.y)){
            unit.trns(contact.x - unit.x, contact.y - unit.y);
        }
        unit.vel.setZero();
    }

    boolean canMineHere(Tile tile){
        if(isPulsar(unit) || !isNova(unit)) return true;

        Seq<Unit> active = activeNovas.get(tile);
        if(active != null && active.contains(unit)){
            leaveQueue();
            return true;
        }

        if(active != null && !active.isEmpty()){
            queueNova(tile);
            return false;
        }

        Seq<Unit> queue = novaQueue.get(tile);
        if(queue != null && !queue.isEmpty()){
            if(queue.first() == unit){
                queue.remove(unit);
                if(queue.isEmpty()) novaQueue.remove(tile);
                queuedTile = null;
                return true;
            }else{
                queueNova(tile);
                return false;
            }
        }

        queueNova(tile);
        queue = novaQueue.get(tile);
        if(queue != null && !queue.isEmpty() && queue.first() == unit){
            queue.remove(unit);
            if(queue.isEmpty()) novaQueue.remove(tile);
            queuedTile = null;
            return true;
        }

        return false;
    }

    void startMining(Tile tile){
        if(miningTile == tile){
            if(isPulsar(unit)){
                addActive(activePulsars, tile, unit);
            }else if(isNova(unit)){
                addActive(activeNovas, tile, unit);
            }
            return;
        }
        clearMiningState();

        if(isPulsar(unit)){
            addActive(activePulsars, tile, unit);
            miningTile = tile;
        }else if(isNova(unit)){
            addActive(activeNovas, tile, unit);
            miningTile = tile;
        }
    }

    void clearMiningState(){
        if(miningTile != null){
            if(isPulsar(unit)){
                removeActive(activePulsars, miningTile, unit);
            }else if(isNova(unit)){
                removeActive(activeNovas, miningTile, unit);
            }
            miningTile = null;
        }
        leaveQueue();
    }

    void queueNova(Tile tile){
        if(!isNova(unit)) return;
        if(queuedTile != null && queuedTile != tile){
            leaveQueue();
        }

        Seq<Unit> queue = novaQueue.get(tile);
        if(queue == null){
            queue = new Seq<>();
            novaQueue.put(tile, queue);
        }

        if(queue.contains(unit)){
            if(forcedByPlayer && queue.first() != unit){
                queue.remove(unit);
                queue.insert(0, unit);
            }
        }else{
            if(forcedByPlayer){
                queue.insert(0, unit);
            }else{
                queue.add(unit);
            }
        }
        queuedTile = tile;
    }

    void leaveQueue(){
        if(queuedTile == null) return;
        Seq<Unit> queue = novaQueue.get(queuedTile);
        if(queue != null){
            queue.remove(unit);
            if(queue.isEmpty()){
                novaQueue.remove(queuedTile);
            }
        }
        queuedTile = null;
    }

    Tile findNearestOre(){
        // First try to find ore near last harvest position
        boolean useCounts = isNova(unit);
        if(lastHarvestPos.len() > 0){
            Tile near = findOreNear(lastHarvestPos.x, lastHarvestPos.y, searchRadius, useCounts);
            if(near != null) return near;
        }

        // Otherwise find nearest ore to unit with limited search radius
        Tile best = findOreNear(unit.x, unit.y, maxSearchRadius, useCounts);
        if(useCounts && harvestTarget != null && harvestTarget.block() instanceof CrystalMineralWall && best != null){
            int currentCount = minerCount(harvestTarget);
            int bestCount = minerCount(best);
            if(bestCount == currentCount){
                return harvestTarget;
            }
        }
        return best;
    }

    Tile findOreNear(float x, float y, float radius, boolean useCounts){
        Tile best = null;
        float bestDst = Float.MAX_VALUE;
        int bestCount = Integer.MAX_VALUE;

        int range = (int)(radius / tilesize);
        // Clamp range to prevent searching entire map
        range = Math.min(range, 100); // Maximum 100 tiles in each direction

        int tx = world.toTile(x);
        int ty = world.toTile(y);

        for(int dx = -range; dx <= range; dx++){
            for(int dy = -range; dy <= range; dy++){
                Tile tile = world.tile(tx + dx, ty + dy);
                if(tile == null) continue;

                // Check if it's a crystal mineral
                if(!(tile.block() instanceof CrystalMineralWall)) continue;

                // Check distance
                float dst = Mathf.dst2(x, y, tile.worldx(), tile.worldy());
                if(useCounts){
                    int count = minerCount(tile);
                    if(count < bestCount || (count == bestCount && dst < bestDst)){
                        bestCount = count;
                        bestDst = dst;
                        best = tile;
                    }
                }else{
                    if(dst < bestDst){
                        bestDst = dst;
                        best = tile;
                    }
                }
            }
        }

        return best;
    }

    Tile findNearestFreeOre(float x, float y, float radius){
        Tile best = null;
        float bestDst = Float.MAX_VALUE;

        int range = (int)(radius / tilesize);
        range = Math.min(range, 100);

        int tx = world.toTile(x);
        int ty = world.toTile(y);

        for(int dx = -range; dx <= range; dx++){
            for(int dy = -range; dy <= range; dy++){
                Tile tile = world.tile(tx + dx, ty + dy);
                if(tile == null || !(tile.block() instanceof CrystalMineralWall)) continue;

                if(activeCount(activeNovas, tile) > 0 || activeCount(activePulsars, tile) > 0) continue;

                float dst = Mathf.dst2(x, y, tile.worldx(), tile.worldy());
                if(dst < bestDst){
                    bestDst = dst;
                    best = tile;
                }
            }
        }

        return best;
    }

    static boolean isNova(Unit unit){
        return unit.type == UnitTypes.nova;
    }

    static boolean isPulsar(Unit unit){
        return unit.type == UnitTypes.pulsar;
    }

    static int minerCount(Tile tile){
        return activeCount(activeNovas, tile) + activeCount(activePulsars, tile) + queuedCount(novaQueue, tile);
    }

    public static int getActiveNovaCount(Tile tile){
        return activeCount(activeNovas, tile);
    }

    static int novaInterestCount(Tile tile){
        return activeCount(activeNovas, tile) + queuedCount(novaQueue, tile);
    }

    static int activeCount(ObjectMap<Tile, Seq<Unit>> map, Tile tile){
        Seq<Unit> seq = map.get(tile);
        return seq == null ? 0 : seq.size;
    }

    static int queuedCount(ObjectMap<Tile, Seq<Unit>> map, Tile tile){
        Seq<Unit> seq = map.get(tile);
        return seq == null ? 0 : seq.size;
    }

    static void addActive(ObjectMap<Tile, Seq<Unit>> map, Tile tile, Unit unit){
        Seq<Unit> seq = map.get(tile);
        if(seq == null){
            seq = new Seq<>();
            map.put(tile, seq);
        }
        if(!seq.contains(unit)){
            seq.add(unit);
        }
    }

    static void removeActive(ObjectMap<Tile, Seq<Unit>> map, Tile tile, Unit unit){
        Seq<Unit> seq = map.get(tile);
        if(seq == null) return;
        seq.remove(unit);
        if(seq.isEmpty()){
            map.remove(tile);
        }
    }

    static void cleanupMap(ObjectMap<Tile, Seq<Unit>> map){
        Seq<Tile> toRemove = new Seq<>();
        map.each((tile, units) -> {
            units.removeAll(u -> u == null || !u.isValid() || u.dead || !isHarvestingUnit(u));
            if(units.isEmpty()){
                toRemove.add(tile);
            }
        });
        for(Tile tile : toRemove){
            map.remove(tile);
        }
    }

    static void cleanupQueue(ObjectMap<Tile, Seq<Unit>> map){
        Seq<Tile> toRemove = new Seq<>();
        map.each((tile, units) -> {
            units.removeAll(u -> u == null || !u.isValid() || u.dead || !isHarvestingUnit(u));
            if(units.isEmpty()){
                toRemove.add(tile);
            }
        });
        for(Tile tile : toRemove){
            map.remove(tile);
        }
    }

    static boolean isHarvestingUnit(Unit unit){
        if(unit.controller() instanceof HarvestAI) return true;
        if(unit.controller() instanceof CommandAI cmd){
            return cmd.currentCommand() == UnitCommand.harvestCommand;
        }
        return false;
    }

    public void removed(){
        clearMiningState();
    }

    void moveToContact(Vec2 contact, @Nullable Tile approach, float speed){
        if(unit.isGrounded()){
            vecMovePos.set(contact);
            if(approach != null){
                vecMainPos.set(approach.worldx(), approach.worldy());
            }else{
                vecMainPos.set(vecMovePos);
            }

            if(controlPath.getPathPosition(unit, vecMovePos, vecMainPos, vecOut, noFound)){
                moveTo(vecOut, 0f, speed, true, null, true);
            }else{
                moveTo(contact, 0f, speed, true, null, true);
            }
        }else{
            moveTo(contact, 0f, speed, true, null, true);
        }
    }

    @Nullable Tile findApproachTile(Tile target, float fromX, float fromY){
        int size = Math.max(1, target.block().size);
        Tile best = null;
        float bestDst = Float.MAX_VALUE;

        for(Point2 edge : Edges.getEdges(size)){
            Tile other = world.tile(target.x + edge.x, target.y + edge.y);
            if(other == null || !isPassable(other)) continue;

            float dst = Mathf.dst2(fromX, fromY, other.worldx(), other.worldy());
            if(dst < bestDst){
                bestDst = dst;
                best = other;
            }
        }

        return best;
    }

    boolean isPassable(Tile tile){
        if(tile == null) return false;
        if(unit.isFlying()) return true;
        if(unit.isPathImpassable(tile.x, tile.y)) return false;
        return unit.canPass(tile.x, tile.y);
    }
}
