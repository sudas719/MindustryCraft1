package mindustry.world;

import arc.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.game.EventType.*;
import mindustry.game.*;
import mindustry.game.Teams.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.entities.units.BuildPlan;
import mindustry.world.blocks.*;
import mindustry.world.blocks.ConstructBlock.*;
import mindustry.world.blocks.storage.CoreBlock;
import mindustry.world.blocks.storage.CoreBlock.*;
import mindustry.world.blocks.environment.BorderAreaFloor;
import mindustry.world.blocks.environment.CrystalMineralWall;
import mindustry.world.blocks.environment.Floor;
import mindustry.world.blocks.environment.OverlayFloor;
import mindustry.world.blocks.environment.SteamVent;

import static mindustry.Vars.*;

public class Build{
    private static final IntSet tmp = new IntSet();

    public static @Nullable String ownerName(@Nullable Unit unit){
        if(unit == null) return null;
        String owner = unit.getControllerName();
        return owner != null ? owner : unit.ownerName;
    }

    public static @Nullable String ownerName(@Nullable Building build){
        if(build == null) return null;
        String owner = build.lastAccessed;
        return owner != null ? owner : build.ownerName;
    }

    private static boolean require(boolean present, String name, @Nullable Seq<String> missing){
        if(!present && missing != null){
            missing.add(name);
        }
        return present;
    }

    public static boolean meetsPrerequisites(Block type, Team team){
        return collectMissingPrerequisites(type, team, null);
    }

    public static void addMissingPrerequisites(Block type, Team team, Seq<String> missing){
        if(missing == null) return;
        collectMissingPrerequisites(type, team, missing);
    }

    private static boolean collectMissingPrerequisites(Block type, Team team, @Nullable Seq<String> missing){
        if(type == null || team == null) return false;

        boolean met = true;

        if(type == Blocks.doorLarge){
            met &= require(team.data().hasCore(), "Command Center", missing);
        }else if(type == Blocks.groundFactory){
            met &= require(team.data().getCount(Blocks.doorLarge) + team.data().getCount(Blocks.doorLargeErekir) > 0, "Supply Depot", missing);
        }else if(type == Blocks.multiPress){
            met &= require(team.data().hasCore(), "Command Center", missing);
        }else if(type == Blocks.atmosphericConcentrator){
            met &= require(team.data().getCount(Blocks.groundFactory) > 0, "Barracks", missing);
        }else if(type == Blocks.swarmer){
            met &= require(team.data().getCount(Blocks.multiPress) > 0, "Engineering Bay", missing);
        }else if(type == Blocks.radar){
            met &= require(team.data().getCount(Blocks.multiPress) > 0, "Engineering Bay", missing);
        }else if(type == Blocks.launchPad){
            met &= require(team.data().getCount(Blocks.groundFactory) > 0, "Barracks", missing);
        }else if(type == Blocks.tankFabricator){
            met &= require(team.data().getCount(Blocks.groundFactory) > 0, "Barracks", missing);
        }else if(type == Blocks.shipFabricator){
            met &= require(team.data().getCount(Blocks.tankFabricator) > 0, "Factory", missing);
        }else if(type == Blocks.siliconCrucible){
            met &= require(team.data().getCount(Blocks.tankFabricator) > 0, "Factory", missing);
        }else if(type == Blocks.surgeCrucible){
            met &= require(team.data().getCount(Blocks.shipFabricator) > 0, "Starport", missing);
        }

        return met;
    }

    @Remote(called = Loc.server)
    public static void beginBreak(@Nullable Unit unit, Team team, int x, int y){
        if(!validBreak(team, x, y)){
            return;
        }

        Tile tile = world.tileBuilding(x, y);
        //this should never happen, but it doesn't hurt to check for links
        float prevPercent = 1f;

        if(tile.build != null){
            prevPercent = tile.build.healthf();
        }

        int rotation = tile.build != null ? tile.build.rotation : 0;
        Block previous = tile.block();

        //instantly deconstruct if necessary
        if(previous.instantDeconstruct){
            ConstructBlock.deconstructFinish(tile, previous, unit);
            return;
        }

        Block sub = ConstructBlock.get(previous.size);

        Seq<Building> prevBuild = new Seq<>(1);
        if(tile.build != null){
            prevBuild.add(tile.build);
            tile.build.onDeconstructed(unit);
            tile.build.dead = true;
        }

        tile.setBlock(sub, team, rotation);
        var build = (ConstructBuild)tile.build;
        build.setDeconstruct(previous);
        build.prevBuild = prevBuild;
        tile.build.health = tile.build.maxHealth * prevPercent;

        String owner = ownerName(unit);
        if(owner != null){
            tile.build.lastAccessed = owner;
            tile.build.ownerName = owner;
        }

        Events.fire(new BlockBuildBeginEvent(tile, team, unit, true));
    }

    /** Places a ConstructBlock at this location. To preserve bandwidth, a config is only passed in the case of instant-place blocks. */
    @Remote(called = Loc.server)
    public static void beginPlace(@Nullable Unit unit, Block result, Team team, int x, int y, int rotation, @Nullable Object placeConfig){
        boolean ignoreUnits = ConstructBlock.isPrepaid(Point2.pack(x, y));
        if(!ignoreUnits && unit != null){
            BuildPlan plan = unit.buildPlan();
            if(plan != null && plan.requireClose && !plan.breaking && plan.x == x && plan.y == y){
                ignoreUnits = true;
            }
        }
        boolean persistentBlockersClear = checkPersistentUnitBlockers(result, x, y);
        if(!(ignoreUnits ? validPlaceIgnoreUnits(result, team, x, y, rotation, true, true) && persistentBlockersClear : validPlace(result, team, x, y, rotation))){
            return;
        }

        Tile tile = world.tile(x, y);

        //just in case
        if(tile == null) return;

        //auto-rotate the block to the correct orientation and bail out
        if(tile.team() == team && tile.block == result && tile.build != null && tile.block.quickRotate){
            String owner = ownerName(unit);
            if(owner != null) tile.build.lastAccessed = owner;
            int previous = tile.build.rotation;
            tile.build.rotation = Mathf.mod(rotation, 4);
            tile.build.updateProximity();
            tile.build.noSleep();
            Fx.rotateBlock.at(tile.build.x, tile.build.y, tile.build.block.size);
            Events.fire(new BuildRotateEvent(tile.build, unit, previous));
            if(!headless) Sounds.blockRotate.at(tile.build, 1f + Mathf.range(0.1f), 1f);
            return;
        }

        //repair derelict tile
        if(tile.team() == Team.derelict && team != Team.derelict && tile.block == result && tile.build != null && tile.block.allowDerelictRepair && state.rules.derelictRepair){
            tile.build.rotation = rotation;
            tile.build.changeTeam(team);
            tile.build.enabled = true;
            if(tile.build.power != null){
                tile.build.power.links.clear();
                tile.build.powerGraphRemoved();
            }
            tile.build.checkAllowUpdate();
            tile.build.updateProximity();
            tile.build.onRepaired();

            String owner = ownerName(unit);
            if(owner != null){
                tile.build.lastAccessed = owner;
                tile.build.ownerName = owner;
            }

            if(fogControl.isVisibleTile(team, tile.x, tile.y)){
                result.placeEffect.at(tile.drawx(), tile.drawy(), result.size);
                Fx.rotateBlock.at(tile.build.x, tile.build.y, tile.build.block.size);
                ConstructBlock.playRepairSound(team, tile);
            }

            Events.fire(new BlockBuildEndEvent(tile, unit, team, false, tile.build.config()));
            return;
        }

        //break all props in the way
        tile.getLinkedTilesAs(result, out -> {
            if(out.block != Blocks.air && out.block.alwaysReplace){
                out.block.breakEffect.at(out.drawx(), out.drawy(), out.block.size, out.block.mapColor);
                out.remove();
            }
        });

        //complete it immediately
        if(result.instantBuild){
            Events.fire(new BlockBuildBeginEvent(tile, team, unit, false));
            result.placeBegan(tile, tile.block, unit);
            ConstructBlock.constructFinish(tile, result, unit, (byte)rotation, team, placeConfig);
            return;
        }

        Block previous = tile.block();
        Block sub = ConstructBlock.get(result.size);
        var prevBuild = new Seq<Building>(9);

        result.beforePlaceBegan(tile, previous);
        tmp.clear();

        tile.getLinkedTilesAs(result, t -> {
            if(t.build != null && t.build.team == team && tmp.add(t.build.id)){
                prevBuild.add(t.build);
            }
        });

        tile.setBlock(sub, team, rotation);

        var build = (ConstructBuild)tile.build;

        build.setConstruct(previous.size == sub.size ? previous : Blocks.air, result);
        build.prevBuild = prevBuild;
        String owner = ownerName(unit);
        if(owner != null){
            build.lastAccessed = owner;
            build.ownerName = owner;
        }

        Events.fire(new BlockBuildBeginEvent(tile, team, unit, false));

        result.placeBegan(tile, previous, unit);
    }

    /** Places a ConstructBlock and deducts full cost immediately. Used for RTS build commands. */
    @Remote(called = Loc.server)
    public static void beginPlacePaid(@Nullable Unit unit, Block result, Team team, int x, int y, int rotation, @Nullable Object placeConfig){
        if(!validPlaceIgnoreUnits(result, team, x, y, rotation, true, true) || !checkPersistentUnitBlockers(result, x, y)){
            return;
        }

        if(!state.rules.infiniteResources && !team.rules().infiniteResources){
            CoreBuild core = team.core();
            if(core == null) return;
            if(!team.data().removeResources(result.requirements, state.rules.buildCostMultiplier)) return;

            if(!result.instantBuild){
                ConstructBlock.markPrepaid(Point2.pack(x, y));
            }
        }

        ConstructBlock.markForceBuildTime(Point2.pack(x, y));
        beginPlace(unit, result, team, x, y, rotation, placeConfig);
    }

    /** @return whether a tile can be placed at this location by this team. */
    public static boolean validPlace(Block type, Team team, int x, int y, int rotation){
        return validPlace(type, team, x, y, rotation, true);
    }

    /** @return whether a tile can be placed at this location by this team. */
    public static boolean validPlace(Block type, Team team, int x, int y, int rotation, boolean checkVisible){
        return validPlace(type, team, x, y, rotation, checkVisible, true);
    }

    /** @return whether a tile can be placed at this location by this team. */
    public static boolean validPlace(Block type, Team team, int x, int y, int rotation, boolean checkVisible, boolean checkCoreRadius){
        return validPlaceIgnoreUnits(type, team, x, y, rotation, checkVisible, checkCoreRadius) && checkNoUnitOverlap(type, x, y);
    }

    /** @return whether a lifted building can land at this location. Unlike normal placement, landing never replaces existing blocks/buildings. */
    public static boolean validLandPlace(Block type, Team team, int x, int y, int rotation, boolean allowFoggedUnknowns){
        return validLandPlace(type, team, x, y, rotation, allowFoggedUnknowns, true);
    }

    /** @return whether a lifted building can land at this location. Unlike normal placement, landing never replaces existing blocks/buildings. */
    public static boolean validLandPlace(Block type, Team team, int x, int y, int rotation, boolean allowFoggedUnknowns, boolean checkCoreRadius){
        return validLandPlaceIgnoreUnits(type, team, x, y, rotation, allowFoggedUnknowns, checkCoreRadius) &&
            checkLandUnitOverlap(type, team, x, y, allowFoggedUnknowns);
    }

    /** @return whether a tile can be placed at this location by this team. */
    public static boolean checkNoUnitOverlap(Block type, int x, int y){
        if(!checkPersistentUnitBlockers(type, x, y)) return false;

        float wx = x * tilesize + type.offset - type.size * tilesize / 2f;
        float wy = y * tilesize + type.offset - type.size * tilesize / 2f;
        float size = type.size * tilesize;

        return (!type.solid && !type.solidifes) || !Units.anyEntities(wx, wy, size, size);
    }

    /** @return whether special immobile unit states that must always block placement are absent. */
    public static boolean checkPersistentUnitBlockers(Block type, int x, int y){
        if(type == null) return false;

        float wx = x * tilesize + type.offset - type.size * tilesize / 2f;
        float wy = y * tilesize + type.offset - type.size * tilesize / 2f;
        float size = type.size * tilesize;

        //Buried widow mines always block placement in their footprint.
        if(Units.anyEntities(wx, wy, size, size, UnitTypes::widowIsBuried)) return false;
        //Sieged precept tanks always block placement in their footprint.
        if(Units.anyEntities(wx, wy, size, size, UnitTypes::preceptIsSieged)) return false;
        return true;
    }

    /** @return whether visible units that should block landing are absent. */
    public static boolean checkLandUnitOverlap(Block type, Team team, int x, int y, boolean allowFoggedUnknowns){
        if(type == null) return false;
        if(!checkPersistentUnitBlockers(type, x, y)) return false;
        if(!type.solid && !type.solidifes) return true;

        float wx = x * tilesize + type.offset - type.size * tilesize / 2f;
        float wy = y * tilesize + type.offset - type.size * tilesize / 2f;
        float size = type.size * tilesize;

        return !Units.anyEntities(wx, wy, size, size, u ->
            u != null && u.isValid() && (!allowFoggedUnknowns || !u.inFogTo(team))
        );
    }

    /** @return whether a tile can be placed at this location by this team. Ignores units at this location. */
    public static boolean validPlaceIgnoreUnits(Block type, Team team, int x, int y, int rotation, boolean checkVisible, boolean checkCoreRadius){
        //the wave team can build whatever they want as long as it's visible - banned blocks are not applicable
        boolean envBuildable = type != null && type.environmentBuildable();
        if(type == Blocks.radar || type == Blocks.tankFabricator || type == Blocks.shipFabricator || type == Blocks.surgeCrucible){
            envBuildable = true;
        }

        if(type == null || (!state.rules.editor && (checkVisible && (!envBuildable || (!type.isPlaceable() && !(state.rules.waves && team == state.rules.waveTeam && type.isVisible())))))){
            return false;
        }
        if(!state.rules.editor && !meetsPrerequisites(type, team)){
            return false;
        }

        if(!state.rules.editor && checkCoreRadius){
            //find closest core, if it doesn't match the team, placing is not legal
            if(state.rules.polygonCoreProtection){
                float mindst = Float.MAX_VALUE;
                CoreBuild closest = null;
                for(TeamData data : state.teams.active){
                    if(!data.team.rules().protectCores){
                        continue;
                    }

                    for(CoreBuild tile : data.cores){
                        float dst = tile.dst2(x * tilesize + type.offset, y * tilesize + type.offset);
                        if(dst < mindst){
                            closest = tile;
                            mindst = dst;
                        }
                    }
                }
                if(closest != null && closest.team != team){
                    return false;
                }
            }else if(state.teams.anyEnemyCoresWithinBuildRadius(team, x * tilesize + type.offset, y * tilesize + type.offset)){
                return false;
            }
        }

        Tile tile = world.tile(x, y);

        if(tile == null) return false;
        if(hasSlopeInPlacementArea(type, x, y)) return false;

        if(type instanceof CoreBlock){
            return type.canPlaceOn(tile, team, rotation);
        }

        if(type == Blocks.ventCondenser){
            if(!(tile.floor() instanceof SteamVent vent)) return false;
            for(int dx = -1; dx <= 1; dx++){
                for(int dy = -1; dy <= 1; dy++){
                    Tile other = world.tile(tile.x + dx, tile.y + dy);
                    if(other == null || other.floor() != vent) return false;
                }
            }

            int offsetx = -(type.size - 1) / 2;
            int offsety = -(type.size - 1) / 2;

            for(int dx = 0; dx < type.size; dx++){
                for(int dy = 0; dy < type.size; dy++){
                    int wx = dx + offsetx + tile.x, wy = dy + offsety + tile.y;
                    Tile check = world.tile(wx, wy);
                    if(check == null) return false;
                    if(check.build != null){
                        if(check.build.block == Blocks.ventCondenser) return false;
                        if(!(check.build instanceof ConstructBuild build && build.current == type) && check.build.block != Blocks.ventSpout){
                            return false;
                        }
                    }else if(check.block() != Blocks.air && check.block() != Blocks.ventSpout){
                        return false;
                    }
                }
            }
            return true;
        }

        if(!type.canPlaceOn(tile, team, rotation)){
            return false;
        }

        //floors have different checks
        if(type.isFloor()){
            return type.isOverlay() ? tile.overlay() != type : tile.floor() != type;
        }

        //campaign darkness check
        if(!type.ignoreBuildDarkness && world.getDarkness(x, y) >= 3){
            return false;
        }

        if(type.requiresWater && !contactsShallows(tile.x, tile.y, type) && !type.placeableLiquid){
            return false;
        }

        int offsetx = -(type.size - 1) / 2;
        int offsety = -(type.size - 1) / 2;

        for(int dx = 0; dx < type.size; dx++){
            for(int dy = 0; dy < type.size; dy++){
                int wx = dx + offsetx + tile.x, wy = dy + offsety + tile.y;

                Tile check = world.tile(wx, wy);
                if(check != null && check.build != null){
                    if(!(check.build instanceof ConstructBuild build && build.current == type && check.centerX() == tile.x && check.centerY() == tile.y)){
                        return false;
                    }
                }

                if(
                check == null || //nothing there
                (type.size == 2 && world.getDarkness(wx, wy) >= 3) ||
                (isPlacementDeep(check) && !type.floating && !type.requiresWater && !type.placeableLiquid) || //deep water
                (!state.rules.derelictRepair && check.team() == Team.derelict && check.build != null) ||
                (type == check.block() && check.build != null && rotation == check.build.rotation && type.rotate && !((type == check.block && team != Team.derelict && check.team() == Team.derelict))) || //same block, same rotation
                !check.interactable(team) || //cannot interact
                !isPlacementPlaceable(check) && !type.ignoreBuildDarkness || //solid floor
                //when you have a payload, you cannot place blocks on things, even if normal placement rules allow it. this is a hack that assumes checkVisible = true means it's coming from a payload
                (!checkVisible && checkCoreRadius && !check.block().alwaysReplace) || //replacing a block that should be replaced (e.g. payload placement)
                    !(((type.canReplace(check.block()) || (check.build != null && check.build.canBeReplaced(type)) || (type == check.block && team != Team.derelict && check.team() == Team.derelict)) || //can replace type OR can replace derelict block of same type
                        (check.build instanceof ConstructBuild build && build.current == type && check.centerX() == tile.x && check.centerY() == tile.y)) && //same type in construction
                    type.bounds(tile.x, tile.y, Tmp.r1).grow(0.01f).contains(check.block.bounds(check.centerX(), check.centerY(), Tmp.r2))) || //no replacement
                (type.requiresWater && !hasPlacementWater(check)) //requires water but none found
                ) return false;
            }
        }

        if(state.rules.placeRangeCheck && checkCoreRadius && !state.isEditor() && getEnemyOverlap(type, team, x, y) != null){
            return false;
        }

        return true;
    }

    /** @return whether a lifted building can land at this location while ignoring units at this location. */
    public static boolean validLandPlaceIgnoreUnits(Block type, Team team, int x, int y, int rotation, boolean allowFoggedUnknowns, boolean checkCoreRadius){
        boolean envBuildable = type != null && type.environmentBuildable();
        if(type == Blocks.radar || type == Blocks.tankFabricator || type == Blocks.shipFabricator || type == Blocks.surgeCrucible){
            envBuildable = true;
        }

        if(state.rules.polygonCoreProtection && !envBuildable){
            Building closest = null;
            float mindst = Float.MAX_VALUE;
            Seq<TeamData> data = state.teams.present;
            if(type instanceof CoreBlock){
                for(TeamData dataTeam : data){
                    if(!dataTeam.team.rules().protectCores){
                        continue;
                    }

                    for(CoreBuild tile : dataTeam.cores){
                        float dst = tile.dst2(x * tilesize + type.offset, y * tilesize + type.offset);
                        if(dst < mindst){
                            closest = tile;
                            mindst = dst;
                        }
                    }
                }
                if(closest != null && closest.team != team){
                    return false;
                }
            }else if(state.teams.anyEnemyCoresWithinBuildRadius(team, x * tilesize + type.offset, y * tilesize + type.offset)){
                return false;
            }
        }

        Tile tile = world.tile(x, y);
        if(tile == null) return false;
        if(hasSlopeInPlacementArea(type, x, y)) return false;

        boolean centerHidden = allowFoggedUnknowns && landTileHidden(team, tile);

        if(type instanceof CoreBlock core){
            return centerHidden || core.canLandOn(tile, team, rotation);
        }

        if(type == Blocks.ventCondenser){
            if(centerHidden) return true;
            if(!(tile.floor() instanceof SteamVent vent)) return false;
            for(int dx = -1; dx <= 1; dx++){
                for(int dy = -1; dy <= 1; dy++){
                    Tile other = world.tile(tile.x + dx, tile.y + dy);
                    if(other == null) return false;
                    if(allowFoggedUnknowns && landTileHidden(team, other)) continue;
                    if(other.floor() != vent) return false;
                }
            }

            int offsetx = -(type.size - 1) / 2;
            int offsety = -(type.size - 1) / 2;

            for(int dx = 0; dx < type.size; dx++){
                for(int dy = 0; dy < type.size; dy++){
                    int wx = dx + offsetx + tile.x, wy = dy + offsety + tile.y;
                    Tile check = world.tile(wx, wy);
                    if(check == null) return false;
                    if(allowFoggedUnknowns && landTileHidden(team, check)) continue;
                    if(check.build != null || (check.block() != Blocks.air && !check.block().alwaysReplace)) return false;
                }
            }
            return true;
        }

        if(!centerHidden && !type.canPlaceOn(tile, team, rotation)){
            return false;
        }

        if(type.isFloor()){
            return centerHidden || (type.isOverlay() ? tile.overlay() != type : tile.floor() != type);
        }

        if(!type.ignoreBuildDarkness && world.getDarkness(x, y) >= 3){
            return false;
        }

        if(type.requiresWater && !contactsShallows(tile.x, tile.y, type) && !type.placeableLiquid){
            return false;
        }

        int offsetx = -(type.size - 1) / 2;
        int offsety = -(type.size - 1) / 2;

        for(int dx = 0; dx < type.size; dx++){
            for(int dy = 0; dy < type.size; dy++){
                int wx = dx + offsetx + tile.x, wy = dy + offsety + tile.y;
                Tile check = world.tile(wx, wy);
                if(check == null) return false;
                if(allowFoggedUnknowns && landTileHidden(team, check)) continue;

                if(
                    (type.size == 2 && world.getDarkness(wx, wy) >= 3) ||
                    (isPlacementDeep(check) && !type.floating && !type.requiresWater && !type.placeableLiquid) ||
                    (!state.rules.derelictRepair && check.team() == Team.derelict && check.build != null) ||
                    !check.interactable(team) ||
                    (!isPlacementPlaceable(check) && !type.ignoreBuildDarkness) ||
                    (check.build != null) ||
                    (check.block() != Blocks.air && !check.block().alwaysReplace) ||
                    (type.requiresWater && !hasPlacementWater(check))
                ) return false;
            }
        }

        if(state.rules.placeRangeCheck && checkCoreRadius && !state.isEditor() && getEnemyOverlap(type, team, x, y) != null){
            return false;
        }

        return true;
    }

    private static boolean landTileHidden(@Nullable Team team, @Nullable Tile tile){
        return tile != null && team != null && state.rules.fog && fogControl != null && team != Team.derelict && team.data().isAlive() &&
            !fogControl.isVisibleTile(team, tile.x, tile.y);
    }

    /** @return whether any tile in this block's footprint is marked as slope height. */
    public static boolean hasSlopeInPlacementArea(Block type, int x, int y){
        if(type == null || type.isFloor()) return false;

        Tile center = world.tile(x, y);
        if(center == null) return true;

        int offsetx = -(type.size - 1) / 2;
        int offsety = -(type.size - 1) / 2;

        for(int dx = 0; dx < type.size; dx++){
            for(int dy = 0; dy < type.size; dy++){
                Tile check = world.tile(center.x + dx + offsetx, center.y + dy + offsety);
                if(check == null || HeightLayerData.slope(check)){
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean isPlacementDeep(Tile tile){
        if(tile == null) return true;
        Floor floor = tile.floor();
        return floor instanceof BorderAreaFloor border ? border.mixedDeep(tile) : floor.isDeep();
    }

    private static boolean isPlacementPlaceable(Tile tile){
        if(tile == null) return false;
        if(tile.overlay() instanceof OverlayFloor overlay && !overlay.placeableOn) return false;
        Floor floor = tile.floor();
        return floor instanceof BorderAreaFloor border ? border.mixedPlaceableOn(tile) : floor.placeableOn;
    }

    private static boolean hasPlacementWater(Tile tile){
        if(tile == null) return false;
        Floor floor = tile.floor();
        return floor instanceof BorderAreaFloor border ? border.mixedHasWater(tile) : floor.liquidDrop == Liquids.water;
    }

    public static @Nullable Building getEnemyOverlap(Block block, Team team, int x, int y){
        return indexer.findEnemyTile(team, x * tilesize + block.size, y * tilesize + block.size, block.placeOverlapRange + 4f, b -> b.team.rules().checkPlacement);
    }

    public static boolean contactsGround(int x, int y, Block block){
        if(block.isMultiblock()){
            for(Point2 point : Edges.getEdges(block.size)){
                Tile tile = world.tile(x + point.x, y + point.y);
                if(tile != null && !tile.floor().isLiquid) return true;
            }
        }else{
            for(Point2 point : Geometry.d4){
                Tile tile = world.tile(x + point.x, y + point.y);
                if(tile != null && !tile.floor().isLiquid) return true;
            }
        }
        return false;
    }

    public static boolean contactsShallows(int x, int y, Block block){
        if(block.isMultiblock()){
            for(Point2 point : block.getInsideEdges()){
                Tile tile = world.tile(x + point.x, y + point.y);
                if(tile != null && !isPlacementDeep(tile)) return true;
            }

            for(Point2 point : block.getEdges()){
                Tile tile = world.tile(x + point.x, y + point.y);
                if(tile != null && !isPlacementDeep(tile)) return true;
            }
        }else{
            for(Point2 point : Geometry.d4){
                Tile tile = world.tile(x + point.x, y + point.y);
                if(tile != null && !isPlacementDeep(tile)) return true;
            }
            Tile tile = world.tile(x, y);
            return tile != null && !isPlacementDeep(tile);
        }
        return false;
    }

    /** @return whether the tile at this position is breakable by this team */
    public static boolean validBreak(Team team, int x, int y){
        Tile tile = world.tile(x, y);
        return tile != null && tile.block() != Blocks.air && (tile.block().canBreak(tile) && (tile.breakable() || state.rules.allowEnvironmentDeconstruct)) && tile.interactable(team);
    }
}
