package mindustry.entities;

import arc.*;
import arc.func.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.game.*;
import mindustry.game.Teams.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;

import static mindustry.Vars.*;

/** Utility class for unit and team interactions.*/
public class Units{
    private static final Rect hitrect = new Rect();
    private static Unit result;
    private static float cdist, cpriority;
    private static int intResult;
    private static Building buildResult;

    //prevents allocations in anyEntities
    private static boolean anyEntityGround;
    private static float aeX, aeY, aeW, aeH;
    private static final Boolf<Unit> anyEntityLambda = unit -> {
        if((unit.isGrounded() && !unit.type.allowLegStep) == anyEntityGround){
            unit.hitboxTile(hitrect);
            return hitrect.overlaps(aeX, aeY, aeW, aeH);
        }
        return false;
    };

    public static void notifyUnitSpawn(Unit unit){
        if(net.server()){
            Call.unitSpawn(new UnitSyncContainer(unit));
        }
    }

    //syncs a unit spawn so that it appears immediately without waiting for a snapshot
    @Remote(unreliable = true, priority = PacketPriority.low)
    public static void unitSpawn(UnitSyncContainer container){
        //doesn't actually do anything, reading calls add()
    }

    @Remote(called = Loc.server)
    public static void unitCapDeath(Unit unit){
        if(unit != null){
            unit.dead = true;
            Fx.unitCapKill.at(unit);
            Core.app.post(() -> Call.unitDestroy(unit.id));
        }
    }

    @Remote(called = Loc.server)
    public static void unitEnvDeath(Unit unit){
        if(unit != null){
            unit.dead = true;
            Fx.unitEnvKill.at(unit);
            Core.app.post(() -> Call.unitDestroy(unit.id));
        }
    }

    @Remote(called = Loc.server)
    public static void unitDeath(int uid){
        Unit unit = Groups.unit.getByID(uid);

        //if there's no unit don't add it later and get it stuck as a ghost
        if(netClient != null){
            netClient.addRemovedEntity(uid);
        }

        if(unit != null){
            unit.killed();
        }
    }

    //destroys immediately
    @Remote(called = Loc.server)
    public static void unitDestroy(int uid){
        Unit unit = Groups.unit.getByID(uid);

        //if there's no unit don't add it later and get it stuck as a ghost
        if(netClient != null){
            netClient.addRemovedEntity(uid);
        }

        if(unit != null){
            unit.destroy();
        }
    }

    @Remote(called = Loc.server)
    public static void unitDespawn(Unit unit){
        if(unit == null) return;
        Fx.unitDespawn.at(unit.x, unit.y, 0, unit);
        unit.remove();
    }

    /** @return whether a new instance of a unit of this team can be created. */
    public static boolean canCreate(Team team, UnitType type){
        if(type == null) return false;
        if(!type.useUnitCap) return !type.isBanned();
        int cap = getCap(team);
        int cost = Math.max(type.population, 0);
        return (team.data().popCount + cost) <= cap && !type.isBanned();
    }

    public static int getCap(Team team){
        //wave team has no cap
        if((team == state.rules.waveTeam && !state.rules.pvp) || (state.isCampaign() && team == state.rules.waveTeam) || state.rules.disableUnitCap || team.ignoreUnitCap){
            return Integer.MAX_VALUE;
        }
        if(!team.data().hasCore()){
            return 0;
        }
        int cap = Math.max(0, state.rules.unitCapVariable ? state.rules.unitCap + team.data().unitCap : state.rules.unitCap);
        if(cap < Integer.MAX_VALUE - 1){
            cap = Math.min(cap, 200);
        }
        return cap;
    }

    /** @return unit cap as a string, substituting the infinity symbol instead of MAX_VALUE */
    public static String getStringCap(Team team){
        int cap = getCap(team);
        return cap >= Integer.MAX_VALUE - 1 ? "∞" : cap + "";
    }

    /** @return whether this player can interact with a specific tile. if either of these are null, returns true.*/
    public static boolean canInteract(Player player, Building tile){
        return player == null || tile == null || tile.interactable(player.team()) || state.rules.editor;
    }

    /** @return whether a building is targetable by all teams, including its own team. */
    public static boolean targetableAllTeams(@Nullable Building build){
        return build != null && build.block.targetableAllTeams;
    }

    /** @return whether a building can be targeted by a weapon/AI with the provided air/ground flags. */
    public static boolean canTargetBuilding(boolean air, boolean ground, @Nullable Building build){
        return build != null && (ground || (air && build.block.targetableAir));
    }

    /** @return whether this unit has at least one controllable weapon that can hit ground targets. */
    public static boolean unitHasGroundWeapon(@Nullable Unit unit){
        if(unit == null) return false;

        for(var mount : unit.mounts){
            var weapon = mount.weapon;
            if(!weapon.controllable || weapon.noAttack) continue;
            if(weapon.bullet.collidesGround) return true;
        }
        return false;
    }

    /** @return whether air-only weapon targeting should yield to ground weapons for this building target. */
    public static boolean preferGroundWeapons(Unit unit, boolean air, boolean ground, @Nullable Building build){
        return build != null && build.block.targetableAir && air && !ground && unitHasGroundWeapon(unit);
    }

    public static boolean isHittable(@Nullable Posc target, boolean air, boolean ground){
        return target != null &&
            (target instanceof Building b ? canTargetBuilding(air, ground, b) : (target instanceof Unit u && u.checkTarget(air, ground)));
    }

    /** @return hitbox radius of this target, or 0 when no sized hitbox exists. */
    public static float hitRadius(@Nullable Posc target){
        return target instanceof Sized sized ? sized.hitSize() / 2f : 0f;
    }

    /** @return whether target is within edge distance range from a source point + source radius. */
    public static boolean withinTargetRange(@Nullable Posc target, float x, float y, float range, float sourceRadius){
        return target != null && target.within(x, y, range + sourceRadius + hitRadius(target));
    }

    /** @return edge-to-edge distance from source point + source radius to target hitbox. */
    public static float edgeDst(@Nullable Posc target, float x, float y, float sourceRadius){
        return target == null ? Float.MAX_VALUE : Mathf.dst(x, y, target.x(), target.y()) - sourceRadius - hitRadius(target);
    }

    /** @return an aim point on/inside target hitbox from a source point. */
    public static Vec2 aimPoint(@Nullable Posc target, float fromX, float fromY, float targetX, float targetY, Vec2 out){
        float radius = hitRadius(target);
        if(radius <= 0.001f) return out.set(targetX, targetY);

        out.set(targetX - fromX, targetY - fromY);
        float len = out.len();
        if(len <= 0.001f) return out.set(targetX, targetY);

        return out.setLength(Math.max(0f, len - radius)).add(fromX, fromY);
    }

    /**
     * Validates a target.
     * @param target The target to validate
     * @param team The team of the thing doing tha targeting
     * @param x The X position of the thing doing the targeting
     * @param y The Y position of the thing doing the targeting
     * @param range The maximum edge-to-edge distance from the target hitbox to the source hitbox
     * @return whether the target is invalid
     */
    public static boolean invalidateTarget(Posc target, Team team, float x, float y, float range){
        return invalidateTarget(target, team, x, y, range, 0f);
    }

    /** See {@link #invalidateTarget(Posc, Team, float, float, float)} */
    public static boolean invalidateTarget(Posc target, Team team, float x, float y, float range, float sourceRadius){
        return target == null ||
            (range != Float.MAX_VALUE && !withinTargetRange(target, x, y, range, sourceRadius)) ||
            (target instanceof Teamc t && t.team() == team && !(target instanceof Building b && targetableAllTeams(b))) ||
            (target instanceof Healthc h && !h.isValid()) ||
            (target instanceof Unit u && !u.targetable(team));
    }

    /** See {@link #invalidateTarget(Posc, Team, float, float, float)} */
    public static boolean invalidateTarget(Posc target, Team team, float x, float y){
        return invalidateTarget(target, team, x, y, Float.MAX_VALUE);
    }

    /** See {@link #invalidateTarget(Posc, Team, float, float, float)} */
    public static boolean invalidateTarget(Teamc target, Unit targeter, float range){
        return invalidateTarget(target, targeter.team(), targeter.x(), targeter.y(), range, targeter.hitSize / 2f);
    }

    /** Returns whether there are any entities on this tile. */
    public static boolean anyEntities(Tile tile, boolean ground){
        float size = tile.block().size * tilesize;
        return anyEntities(tile.drawx() - size/2f, tile.drawy() - size/2f, size, size, ground);
    }

    /** Returns whether there are any entities on this tile. */
    public static boolean anyEntities(Tile tile){
        return anyEntities(tile, true);
    }

    public static boolean anyEntities(float x, float y, float size){
        return anyEntities(x - size/2f, y - size/2f, size, size, true);
    }

    public static boolean anyEntities(float x, float y, float width, float height){
        return anyEntities(x, y, width, height, true);
    }

    public static boolean anyEntities(float x, float y, float width, float height, boolean ground){
        anyEntityGround = ground;
        aeX = x;
        aeY = y;
        aeW = width;
        aeH = height;

        return nearbyCheck(x, y, width, height, anyEntityLambda);
    }

    /** Note that this checks the tile hitbox, not the standard hitbox. */
    public static boolean anyEntities(float x, float y, float width, float height, Boolf<Unit> check){

        return nearbyCheck(x, y, width, height, unit -> {
            if(check.get(unit)){
                unit.hitboxTile(hitrect);

                return hitrect.overlaps(x, y, width, height);
            }
            return false;
        });
    }

    /** Returns the nearest damaged tile. */
    public static Building findDamagedTile(Team team, float x, float y){
        return indexer.getDamaged(team).min(b -> b.dst2(x, y));
    }

    /** Returns the nearest ally tile in a range. */
    public static Building findAllyTile(Team team, float x, float y, float range, Boolf<Building> pred){
        return indexer.findTile(team, x, y, range, pred);
    }

    /** Returns the nearest enemy tile in a range. */
    public static Building findEnemyTile(Team team, float x, float y, float range, Boolf<Building> pred){
        if(team == Team.derelict){
            return indexer.findEnemyTile(team, x, y, range, b -> targetableAllTeams(b) && pred.get(b));
        }

        return indexer.findEnemyTile(team, x, y, range, pred);
    }

    /** @return the closest building of the provided team that matches the predicate. */
    public static @Nullable Building closestBuilding(Team team, float wx, float wy, float range, Boolf<Building> pred){
        buildResult = null;
        cdist = 0f;

        var buildings = team.data().buildingTree;
        if(buildings == null) return null;
        buildings.intersect(wx - range, wy - range, range*2f, range*2f, b -> {
            if(pred.get(b)){
                float dst = b.dst(wx, wy) - b.hitSize()/2f;
                if(dst <= range && (buildResult == null || dst <= cdist)){
                    cdist = dst;
                    buildResult = b;
                }
            }
        });

        var result = buildResult;
        buildResult = null;

        return result;
    }

    /** Iterates through all buildings in a range. */
    public static void nearbyBuildings(float x, float y, float range, Cons<Building> cons){
        indexer.allBuildings(x, y, range, cons);
    }

    /** Returns the closest target enemy. First, units are checked, then tile entities. */
    public static Teamc closestTarget(Team team, float x, float y, float range){
        return closestTarget(team, x, y, range, 0f, Unit::isValid);
    }

    /** Returns the closest target enemy. First, units are checked, then tile entities. */
    public static Teamc closestTarget(Team team, float x, float y, float range, float sourceRadius){
        return closestTarget(team, x, y, range, sourceRadius, Unit::isValid);
    }

    /** Returns the closest target enemy. First, units are checked, then tile entities. */
    public static Teamc closestTarget(Team team, float x, float y, float range, Boolf<Unit> unitPred){
        return closestTarget(team, x, y, range, 0f, unitPred);
    }

    /** Returns the closest target enemy. First, units are checked, then tile entities. */
    public static Teamc closestTarget(Team team, float x, float y, float range, float sourceRadius, Boolf<Unit> unitPred){
        return closestTarget(team, x, y, range, sourceRadius, unitPred, t -> true);
    }

    /** Returns the closest target enemy. First, units are checked, then tile entities. */
    public static Teamc closestTarget(Team team, float x, float y, float range, Boolf<Unit> unitPred, Boolf<Building> tilePred){
        return closestTarget(team, x, y, range, 0f, unitPred, tilePred);
    }

    /** Returns the closest target enemy. First, units are checked, then tile entities. */
    public static Teamc closestTarget(Team team, float x, float y, float range, float sourceRadius, Boolf<Unit> unitPred, Boolf<Building> tilePred){
        Unit unit = closestEnemy(team, x, y, range, sourceRadius, unitPred);
        if(unit != null){
            return unit;
        }else{
            return findEnemyTile(team, x, y, range + sourceRadius, tilePred);
        }
    }

    /** Returns the closest target enemy. First, units are checked, then buildings. */
    public static Teamc bestTarget(Team team, float x, float y, float range, Boolf<Unit> unitPred, Boolf<Building> tilePred, Sortf sort){
        return bestTarget(team, x, y, range, 0f, unitPred, tilePred, sort);
    }

    /** Returns the closest target enemy. First, units are checked, then buildings. */
    public static Teamc bestTarget(Team team, float x, float y, float range, float sourceRadius, Boolf<Unit> unitPred, Boolf<Building> tilePred, Sortf sort){
        Unit unit = bestEnemy(team, x, y, range, sourceRadius, unitPred, sort);
        if(unit != null){
            return unit;
        }else{
            return findEnemyTile(team, x, y, range + sourceRadius, tilePred);
        }
    }

    /** Returns the closest enemy of this team. Filter by predicate. */
    public static Unit closestEnemy(Team team, float x, float y, float range, Boolf<Unit> predicate){
        return closestEnemy(team, x, y, range, 0f, predicate);
    }

    /** Returns the closest enemy of this team. Filter by predicate. */
    public static Unit closestEnemy(Team team, float x, float y, float range, float sourceRadius, Boolf<Unit> predicate){
        if(team == Team.derelict) return null;

        result = null;
        cdist = 0f;
        cpriority = -99999f;

        nearbyEnemies(team, x, y, range + sourceRadius, e -> {
            if(e.dead() || !predicate.get(e) || e.team == Team.derelict || !e.targetable(team) || e.inFogTo(team)) return;

            float dst = edgeDst(e, x, y, sourceRadius);
            if(dst <= range && (result == null || dst < cdist || e.type.targetPriority > cpriority) && e.type.targetPriority >= cpriority){
                result = e;
                cdist = dst;
                cpriority = e.type.targetPriority;
            }
        });

        return result;
    }

    /** Returns the closest enemy of this team using a custom comparison function. Filter by predicate. */
    public static Unit bestEnemy(Team team, float x, float y, float range, Boolf<Unit> predicate, Sortf sort){
        return bestEnemy(team, x, y, range, 0f, predicate, sort);
    }

    /** Returns the closest enemy of this team using a custom comparison function. Filter by predicate. */
    public static Unit bestEnemy(Team team, float x, float y, float range, float sourceRadius, Boolf<Unit> predicate, Sortf sort){
        if(team == Team.derelict) return null;

        result = null;
        cdist = 0f;
        cpriority = -99999f;

        nearbyEnemies(team, x, y, range + sourceRadius, e -> {
            if(e.dead() || !predicate.get(e) || e.team == Team.derelict || !withinTargetRange(e, x, y, range, sourceRadius) || !e.targetable(team) || e.inFogTo(team)) return;

            float cost = sort.cost(e, x, y);
            if((result == null || cost < cdist || e.type.targetPriority > cpriority) && e.type.targetPriority >= cpriority){
                result = e;
                cdist = cost;
                cpriority = e.type.targetPriority;
            }
        });

        return result;
    }

    /** Returns the closest ally of this team. Filter by predicate. No range. */
    public static Unit closest(Team team, float x, float y, Boolf<Unit> predicate){
        result = null;
        cdist = 0f;

        for(Unit e : Groups.unit){
            if(!predicate.get(e) || e.team() != team) continue;

            float dist = e.dst2(x, y);
            if(result == null || dist < cdist){
                result = e;
                cdist = dist;
            }
        }

        return result;
    }

    /** Returns the closest ally of this team in a range. Filter by predicate. */
    public static Unit closest(Team team, float x, float y, float range, Boolf<Unit> predicate){
        result = null;
        cdist = 0f;

        nearby(team, x, y, range, e -> {
            if(!e.isValid() || !predicate.get(e)) return;

            float dist = e.dst2(x, y);
            if(result == null || dist < cdist){
                result = e;
                cdist = dist;
            }
        });

        return result;
    }

    /** Returns the closest ally of this team in a range. Filter by predicate. */
    public static Unit closest(Team team, float x, float y, float range, Boolf<Unit> predicate, Sortf sort){
        result = null;
        cdist = 0f;

        nearby(team, x, y, range, e -> {
            if(!e.isValid() || !predicate.get(e)) return;

            float dist = sort.cost(e, x, y);
            if(result == null || dist < cdist){
                result = e;
                cdist = dist;
            }
        });

        return result;
    }

    /** Returns the closest ally of this team. Filter by predicate.
     * Unlike the closest() function, this only guarantees that unit hitboxes overlap the range. */
    public static Unit closestOverlap(Team team, float x, float y, float range, Boolf<Unit> predicate){
        result = null;
        cdist = 0f;

        nearby(team, x - range, y - range, range*2f, range*2f, e -> {
            if(!e.isValid() || !predicate.get(e)) return;

            float dist = e.dst2(x, y);
            if(result == null || dist < cdist){
                result = e;
                cdist = dist;
            }
        });

        return result;
    }

    /** @return whether any units exist in this square (centered) */
    public static int count(float x, float y, float size, Boolf<Unit> filter){
        return count(x - size/2f, y - size/2f, size, size, filter);
    }

    /** @return whether any units exist in this rectangle */
    public static int count(float x, float y, float width, float height, Boolf<Unit> filter){
        intResult = 0;
        Groups.unit.intersect(x, y, width, height, v -> {
            if(filter.get(v)){
                intResult ++;
            }
        });
        return intResult;
    }

    /** @return whether any units exist in this rectangle */
    public static boolean any(float x, float y, float width, float height, Boolf<Unit> filter){
        return Groups.unit.intersect(x, y, width, height, filter);
    }

    /** Iterates over all units in a rectangle. */
    public static void nearby(@Nullable Team team, float x, float y, float width, float height, Cons<Unit> cons){
        if(team != null){
            team.data().tree().intersect(x, y, width, height, cons);
        }else{
            for(var other : state.teams.present){
                other.tree().intersect(x, y, width, height, cons);
            }
        }
    }

    /** Iterates over all units in a circle around this position. */
    public static void nearby(@Nullable Team team, float x, float y, float radius, Cons<Unit> cons){
        nearby(team, x - radius, y - radius, radius*2f, radius*2f, unit -> {
            if(unit.within(x, y, radius + unit.hitSize/2f)){
                cons.get(unit);
            }
        });
    }

    /** Iterates over all units in a rectangle. */
    public static void nearby(float x, float y, float width, float height, Cons<Unit> cons){
        Groups.unit.intersect(x, y, width, height, cons);
    }

    /**
     * Iterates over all units in a rectangle.
     * @return whether a unit was found.
     * */
    public static boolean nearbyCheck(float x, float y, float width, float height, Boolf<Unit> cons){
        return Groups.unit.intersect(x, y, width, height, cons);
    }

    /** Iterates over all units in a rectangle. */
    public static void nearby(Rect rect, Cons<Unit> cons){
        nearby(rect.x, rect.y, rect.width, rect.height, cons);
    }

    /** Iterates over all units that are enemies of this team. */
    public static void nearbyEnemies(Team team, float x, float y, float width, float height, Cons<Unit> cons){
        Seq<TeamData> data = state.teams.present;
        for(int i = 0; i < data.size; i++){
            if(data.items[i].team != team){
                nearby(data.items[i].team, x, y, width, height, cons);
            }
        }
    }

    /** Iterates over all units that are enemies of this team. */
    public static void nearbyEnemies(Team team, float x, float y, float radius, Cons<Unit> cons){
        nearbyEnemies(team, x - radius, y - radius, radius * 2f, radius * 2f, u -> {
            if(u.within(x, y, radius + u.hitSize/2f)){
                cons.get(u);
            }
        });
    }

    /** Iterates over all units that are enemies of this team. */
    public static void nearbyEnemies(Team team, Rect rect, Cons<Unit> cons){
        nearbyEnemies(team, rect.x, rect.y, rect.width, rect.height, cons);
    }

    /** @return whether there is an enemy in this rectangle. */
    public static boolean nearEnemy(Team team, float x, float y, float width, float height){
        Seq<TeamData> data = state.teams.present;
        for(int i = 0; i < data.size; i++){
            var other = data.items[i];
            if(other.team != team && other.team != Team.derelict){
                if(other.tree().any(x, y, width, height)){
                    return true;
                }
                if(other.turretTree != null && other.turretTree.any(x, y, width, height)){
                    return true;
                }
            }
        }
        return false;
    }

    public interface Sortf{
        float cost(Unit unit, float x, float y);
    }

    public interface BuildingPriorityf{
        float priority(Building build);
    }

    public static class UnitSyncContainer{
        public Unit unit;

        public UnitSyncContainer(){
        }

        public UnitSyncContainer(Unit unit){
            this.unit = unit;
        }
    }
}
