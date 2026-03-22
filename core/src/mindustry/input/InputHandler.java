package mindustry.input;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.input.*;
import arc.input.GestureDetector.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.*;
import arc.scene.event.*;
import arc.scene.ui.layout.*;
import arc.struct.Queue;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.ai.*;
import mindustry.ai.types.*;
import mindustry.annotations.Annotations.*;
import mindustry.async.*;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.game.EventType.*;
import mindustry.game.*;
import mindustry.game.Teams.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.input.Placement.*;
import mindustry.net.Administration.*;
import mindustry.net.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.ui.fragments.*;
import mindustry.world.*;
import mindustry.world.blocks.ConstructBlock.*;
import mindustry.world.blocks.*;
import mindustry.world.blocks.distribution.*;
import mindustry.world.blocks.environment.CrystalMineralWall;
import mindustry.world.blocks.environment.SteamVent;
import mindustry.world.blocks.defense.BunkerBlock;
import mindustry.world.blocks.payloads.*;
import mindustry.world.blocks.storage.*;
import mindustry.world.blocks.units.UnitFactory;
import mindustry.world.meta.*;

import java.util.*;

import static arc.Core.*;
import static mindustry.Vars.*;

public abstract class InputHandler implements InputProcessor, GestureListener{
    //not sure where else to put this - maps unique commands based on position to a list of units that will be turned into a unit group
    static ObjectMap<Vec2, Seq<Unit>> queuedCommands = new ObjectMap<>();

    /** Used for dropping items. */
    final static float playerSelectRange = mobile ? 17f : 11f;
    final static float unitSelectRadScl = 1f;
    final static Seq<UnitStance> stancesOut = new Seq<>();
    final static IntSeq removed = new IntSeq();
    final static IntSeq tmpControlBuildings = new IntSeq();
    final static IntSet intSet = new IntSet();
    final static Color placementGridLine = Color.valueOf("ffffff");
    final static Color placementGridInvalid = Color.valueOf("ff9a2f");
    public static final float selectionRingStroke = 0.5f;
    public static final float selectionRingRadiusStep = 0.35f;
    public static final float selectionSolidRadiusOffset = 0f;
    public static final float selectionDashedRadiusOffset = -selectionRingRadiusStep;
    public static final float selectionRotatingDashedRadiusOffset = selectionRingRadiusStep * 2f;

    public static float selectionRingLayer(){
        return Core.settings.getBool("selectionringabove", true) ? Layer.end - 1f : Layer.blockUnder - 0.01f;
    }
    /** Maximum line length. */
    final static int maxLength = 100;
    final static Rect r1 = new Rect(), r2 = new Rect();
    final static Seq<Unit> tmpUnits = new Seq<>(false);
    final static Seq<Building> tmpBuildings = new Seq<>(false);
    final static Color rallyColor = Color.valueOf("c9752e");
    final static KeyBind[] controlGroupBindings = {
    Binding.controlGroup01,
    Binding.controlGroup02,
    Binding.controlGroup03,
    Binding.controlGroup04,
    Binding.controlGroup05,
    Binding.controlGroup06,
    Binding.controlGroup07,
    Binding.controlGroup08,
    Binding.controlGroup09,
    Binding.controlGroup10
    };

    /** If true, there is a cutscene currently occurring in logic. */
    public boolean logicCutscene;
    public Vec2 logicCamPan = new Vec2();
    public float logicCamSpeed = 0.1f;
    public float logicCutsceneZoom = -1f;

    /** If any of these functions return true, input is locked. */
    public Seq<Boolp> inputLocks = Seq.with(() -> renderer.isCutscene(), () -> logicCutscene);
    public Interval controlInterval = new Interval();
    public @Nullable Block block;
    public boolean overrideLineRotation;
    public int rotation = 1;
    public boolean droppingItem;
    public float itemDepositCooldown;
    public Group uiGroup;
    public boolean isBuilding = true, buildWasAutoPaused = false, wasShooting = false;
    public @Nullable UnitType controlledType;
    public float recentRespawnTimer;

    public @Nullable Schematic lastSchematic;
    public GestureDetector detector;
    public PlaceLine line = new PlaceLine();
    public BuildPlan resultplan;
    public BuildPlan bplan = new BuildPlan();
    public Seq<BuildPlan> linePlans = new Seq<>();
    public Seq<BuildPlan> selectPlans = new Seq<>(BuildPlan.class);
    public Queue<BuildPlan> lastPlans = new Queue<>();
    public @Nullable Unit lastUnit;
    public @Nullable Unit spectating;
    public int spectatingPlayer = -1;

    //for RTS controls
    public Seq<Unit> selectedUnits = new Seq<>();
    public Seq<Building> commandBuildings = new Seq<>(false);
    public final HoverInfo hover = new HoverInfo();
    public @Nullable Tile selectedResource;
    public boolean commandMode = false;
    public boolean commandRect = false;
    public boolean tappedOne = false;
    public float commandRectX, commandRectY;
    public float commandRectScreenX, commandRectScreenY;
    public static final float transformSelectionPreserveDuration = 60f * 5f;
    /** Groups of units saved to different hotkeys */
    public IntSeq[] controlGroups = new IntSeq[controlGroupBindings.length];
    private final IntSeq subgroupOrder = new IntSeq();
    private final IntSet subgroupSet = new IntSet();
    private final Seq<Unit> subgroupUnits = new Seq<>();
    private final Seq<Building> subgroupBuildings = new Seq<>();
    private final IntSeq preservedTransformSelection = new IntSeq();
    private int activeSubgroup = -1;
    private final IntSet commandBuildingDedup = new IntSet();
    private final Seq<Unit> scvBuildCandidates = new Seq<>(false);
    private int scvBuildAssignIndex = 0;
    private float preservedTransformSelectionTime = 0f;

    private Seq<BuildPlan> plansOut = new Seq<>(BuildPlan.class);
    private QuadTree<BuildPlan> playerPlanTree = new QuadTree<>(new Rect());

    public final BlockInventoryFragment inv;
    public final BlockConfigFragment config;
    public final PlanConfigFragment planConfig;

    private WidgetGroup group = new WidgetGroup();

    private final Eachable<BuildPlan> allPlans = cons -> {
        if(!player.dead()){
            if(player.isBuilder()){
                player.unit().plans().each(cons);
            }else{
                lastPlans.each(cons);
            }
        }else{
            lastPlans.each(cons);
        }
        selectPlans.each(cons);
        linePlans.each(cons);
    };

    private final Eachable<BuildPlan> allSelectLines = cons -> {
        selectPlans.each(cons);
        linePlans.each(cons);
    };

    public InputHandler(){
        group.touchable = Touchable.childrenOnly;
        inv = new BlockInventoryFragment();
        config = new BlockConfigFragment();
        planConfig = new PlanConfigFragment();

        Events.on(UnitDestroyEvent.class, e -> {
            if(e.unit != null && e.unit.isPlayer() && e.unit.getPlayer().isLocal() && e.unit.type.weapons.contains(w -> w.bullet.killShooter)){
                player.shooting = false;
            }
        });

        Events.on(WorldLoadEvent.class, e -> {
            playerPlanTree = new QuadTree<>(new Rect(0f, 0f, world.unitWidth(), world.unitHeight()));
        });

        Events.on(ResetEvent.class, e -> {
            reset();
        });
    }

    //methods to override

    @Remote(called = Loc.server, unreliable = true)
    public static void transferItemEffect(Item item, float x, float y, Itemsc to){
        if(to == null) return;
        createItemTransfer(item, 1, x, y, to, null);
    }

    @Remote(called = Loc.server, unreliable = true)
    public static void takeItems(Building build, Item item, int amount, Unit to){
        if(to == null || build == null) return;

        int removed = build.removeStack(item, Math.min(to.maxAccepted(item), amount));
        if(removed == 0) return;

        to.addItem(item, removed);
        for(int j = 0; j < Mathf.clamp(removed / 3, 1, 8); j++){
            Time.run(j * 3f, () -> transferItemEffect(item, build.x, build.y, to));
        }
    }

    @Remote(called = Loc.server, unreliable = true)
    public static void transferItemToUnit(Item item, float x, float y, Itemsc to){
        if(to == null) return;
        createItemTransfer(item, 1, x, y, to, () -> to.addItem(item));
    }

    @Remote(called = Loc.server, unreliable = true)
    public static void setItem(Building build, Item item, int amount){
        if(build == null || build.items == null) return;
        build.items.set(item, amount);
    }

    @Remote(called = Loc.server, unreliable = true)
    public static void setItems(Building build, ItemStack[] items){
        if(build == null || build.items == null) return;

        for(ItemStack stack : items){
            build.items.set(stack.item, stack.amount);
        }
    }

    @Remote(called = Loc.server, unreliable = true)
    public static void setTileItems(Item item, int amount, int[] positions){
        for(int pos : positions){
            Building build = world.build(pos);
            if(build != null && build.items != null){
                build.items.set(item, amount);
            }
        }
    }

    @Remote(called = Loc.server, unreliable = true)
    public static void clearItems(Building build){
        if(build == null || build.items == null) return;
        build.items.clear();
    }

    @Remote(called = Loc.server, unreliable = true)
    public static void setLiquid(Building build, Liquid liquid, float amount){
        if(build == null || build.liquids == null) return;
        build.liquids.set(liquid, amount);
    }

    @Remote(called = Loc.server, unreliable = true)
    public static void setLiquids(Building build, LiquidStack[] liquids){
        if(build == null || build.liquids == null) return;

        for(LiquidStack stack : liquids){
            build.liquids.set(stack.liquid, stack.amount);
        }
    }

    @Remote(called = Loc.server, unreliable = true)
    public static void setTileLiquids(Liquid liquid, float amount, int[] positions){
        for(int pos : positions){
            Building build = world.build(pos);
            if(build != null && build.liquids != null){
                build.liquids.set(liquid, amount);
            }
        }
    }

    @Remote(called = Loc.server, unreliable = true)
    public static void clearLiquids(Building build){
        if(build == null || build.liquids == null) return;
        build.liquids.clear();
    }

    @Remote(called = Loc.server, unreliable = true)
    public static void transferItemTo(@Nullable Unit unit, Item item, int amount, float x, float y, Building build){
        if(build == null || build.items == null || item == null) return;

        if(unit != null && unit.item() == item) unit.stack.amount = Math.max(unit.stack.amount - amount, 0);

        for(int i = 0; i < Mathf.clamp(amount / 3, 1, 8); i++){
            Time.run(i * 3, () -> createItemTransfer(item, amount, x, y, build, () -> {}));
        }
        if(amount > 0){
            build.handleStack(item, amount, unit);
        }
    }

    @Remote(called = Loc.both, targets = Loc.both, forward = true, unreliable = true)
    public static void deletePlans(Player player, int[] positions){
        if(net.server() && !netServer.admins.allowAction(player, ActionType.removePlanned, a -> a.plans = positions)){
            throw new ValidateException(player, "Player cannot remove plans.");
        }

        if(player == null) return;

        var it = player.team().data().plans.iterator();
        //O(n^2) search here; no way around it
        outer:
        while(it.hasNext()){
            var plan = it.next();

            for(int pos : positions){
                if(plan.x == Point2.x(pos) && plan.y == Point2.y(pos)){
                    plan.removed = true;
                    it.remove();
                    continue outer;
                }
            }
        }
    }

    public static void createItemTransfer(Item item, int amount, float x, float y, Position to, Runnable done){
        Fx.itemTransfer.at(x, y, amount, item.color, to);
        if(done != null){
            Time.run(Fx.itemTransfer.lifetime, done);
        }
    }

    private static boolean finiteCommandCoord(float value){
        return !Float.isNaN(value) && !Float.isInfinite(value);
    }

    private static @Nullable Vec2 sanitizeRemoteCommandTarget(@Nullable Vec2 target){
        if(target == null) return null;
        if(!finiteCommandCoord(target.x) || !finiteCommandCoord(target.y)) return null;

        float width = Math.max(world.unitWidth(), tilesize);
        float height = Math.max(world.unitHeight(), tilesize);
        if(target.x < -width || target.x > width * 2f || target.y < -height || target.y > height * 2f){
            return null;
        }

        float maxX = Math.max(world.unitWidth() - tilesize, 0f);
        float maxY = Math.max(world.unitHeight() - tilesize, 0f);
        return new Vec2(Mathf.clamp(target.x, 0f, maxX), Mathf.clamp(target.y, 0f, maxY));
    }

    private static void recordPlayerAction(@Nullable Player player){
        HudFragment.recordPlayerAction(player);
    }

    private static boolean canScvRepairTarget(Unit unit, @Nullable Teamc target, boolean explicitRepairCommand){
        if(unit == null || unit.type != UnitTypes.nova || target == null) return false;
        if(target.team() != unit.team) return false;

        if(target instanceof Unit ally){
            if(!ally.isValid() || ally == unit || ally.health >= ally.maxHealth() - 0.001f) return false;
            if(!ally.type.unitClasses.contains(UnitClass.mechanical)) return false;
            if(UnitTypes.isMedivac(ally) && UnitTypes.medivacPayloadSlotsFree(ally) > 0 && !explicitRepairCommand){
                return false;
            }
            return true;
        }

        if(target instanceof Building build){
            return build.isValid() && build.team == unit.team && !(build instanceof ConstructBuild) && build.health < build.maxHealth() - 0.001f;
        }

        return false;
    }

    @Remote(called = Loc.server, targets = Loc.both, forward = true)
    public static void commandUnits(Player player, int[] unitIds, @Nullable Building buildTarget, @Nullable Unit unitTarget, @Nullable Vec2 posTarget, boolean queueCommand, boolean finalBatch, boolean forceAttackTarget){
        if(player == null || unitIds == null) return;

        if(net.server() && !netServer.admins.allowAction(player, ActionType.commandUnits, event -> {
            event.unitIDs = unitIds;
        })){
            throw new ValidateException(player, "Player cannot command units.");
        }

        Teamc teamTarget = buildTarget == null ? unitTarget : buildTarget;
        Vec2 safePosTarget = sanitizeRemoteCommandTarget(posTarget);
        if(teamTarget == null && safePosTarget == null) return;
        Vec2 targetAsVec = teamTarget != null ? new Vec2(teamTarget.getX(), teamTarget.getY()) : safePosTarget.cpy();
        Seq<Unit> toAdd = queuedCommands.get(targetAsVec, Seq::new);
        boolean anyCommandedTarget = false;

        for(int id : unitIds){
            Unit unit = Groups.unit.getByID(id);
                if(unit != null && unit.team == player.team()){

                    if(unit.controller() instanceof CommandAI){
                        if(UnitTypes.isBattlecruiser(unit)){
                            //Player-issued direct commands should override Yamato lock/charge.
                            UnitTypes.commandBattlecruiserCancelYamato(unit);
                        }
                        CommandAI ai = (CommandAI)unit.controller();
                        boolean scvRepairCommandRequested = unit.type == UnitTypes.nova && ai.command == UnitCommand.repairCommand;
                        BuildPlan plan = unit.buildPlan();
                        if(unit.type == UnitTypes.nova && plan != null && plan.requireClose && !plan.initialized){
                            boolean keep = teamTarget == null && safePosTarget != null &&
                                Mathf.equal(safePosTarget.x, plan.drawx()) && Mathf.equal(safePosTarget.y, plan.drawy());
                            if(!keep){
                                unit.clearBuilding();
                            }
                        }

                        boolean scvRepairTarget = teamTarget != null && canScvRepairTarget(unit, teamTarget, scvRepairCommandRequested);
                        //implicitly order it to move
                        if(ai.command == null || (ai.command.switchToMove && !(scvRepairCommandRequested && scvRepairTarget))){
                            ai.command(UnitCommand.moveCommand);
                        }
                        //Forced attack commands must override non-moving stances/commands (e.g. hold).
                        if(forceAttackTarget && teamTarget != null){
                            ai.command(UnitCommand.moveCommand);
                        }

                    if(teamTarget != null){
                        boolean alliedAttackableBuilding = teamTarget instanceof Building b && Units.targetableAllTeams(b);
                        boolean forcedAllyAttack = teamTarget.team() == player.team() && (forceAttackTarget || alliedAttackableBuilding);
                        if(teamTarget.team() == player.team() && scvRepairTarget){
                            if(!queueCommand){
                                ai.command(UnitCommand.repairCommand);
                            }
                            anyCommandedTarget = true;
                            if(queueCommand){
                                //Queue building repairs as positions so the order survives construct/replacement.
                                //RepairAI resolves the actual building at execution time.
                                if(teamTarget instanceof Building){
                                    ai.queueCommand(new CommandAI.RepairMarker((Building)teamTarget), true);
                                }else{
                                    ai.queueCommand(teamTarget, true);
                                }
                            }else{
                                ai.commandQueue.clear();
                                ai.commandTarget(teamTarget);
                            }
                        }else if(teamTarget.team() == player.team() && !forcedAllyAttack){
                            ai.commandFollow(teamTarget);
                        }else if(forcedAllyAttack || !((teamTarget instanceof Unit && !unit.canTarget((Unit)teamTarget)) || (teamTarget instanceof Building && !unit.type.targetGround))){
                            anyCommandedTarget = true;
                            if(queueCommand){
                                ai.commandQueue(teamTarget);
                            }else{
                                ai.commandQueue.clear();
                                ai.commandTarget(teamTarget);
                            }
                        }else if(unit.type.followEnemyWhenUnarmed){
                            ai.commandFollow(teamTarget);
                        }
                    }else if(safePosTarget != null){
                        if(queueCommand){
                            ai.commandQueue(safePosTarget);
                        }else{
                            ai.commandQueue.clear();
                            if(forceAttackTarget){
                                ai.commandPosition(safePosTarget, false, true);
                            }else{
                                ai.commandPosition(safePosTarget);
                            }
                        }
                    }

                    unit.lastCommanded = player.coloredName();
                    if(ai.commandQueue.size <= 0){
                        ai.group = null;
                    }

                    toAdd.add(unit);
                }
            }
        }

        //in the "final batch" of commands, assign formations based on EVERYTHING that was commanded.
        if(finalBatch){
            //each physics layer has its own group
            UnitGroup[] groups = new UnitGroup[PhysicsProcess.layers];
            var units = queuedCommands.remove(targetAsVec);

            for(Unit unit : units){
                if(unit.controller() instanceof CommandAI){
                    CommandAI ai = (CommandAI)unit.controller();
                    //only assign a group when this is not a queued command
                    if(ai.commandQueue.size == 0 && unitIds.length > 1){
                        int layer = unit.collisionLayer();

                        if(layer == -1) layer = 0;

                        if(groups[layer] == null){
                            groups[layer] = new UnitGroup();
                        }

                        groups[layer].units.add(unit);
                        ai.group = groups[layer];
                    }
                }
            }

            for(int i = 0; i < groups.length; i ++){
                var group = groups[i];
                if(group != null && group.units.size > 0){
                    group.calculateFormation(targetAsVec, i);
                }
            }

            recordPlayerAction(player);
        }

        if(unitIds.length > 0 && player == Vars.player && !state.isPaused()){
            if(anyCommandedTarget){
                // Attack-command marker effect disabled by mod behavior.
            }
        }
    }

    @Remote(called = Loc.server, targets = Loc.both, forward = true)
    public static void setUnitCommand(Player player, int[] unitIds, UnitCommand command){
        if(player == null || unitIds == null || command == null) return;

        if(net.server() && !netServer.admins.allowAction(player, ActionType.commandUnits, event -> {
            event.unitIDs = unitIds;
        })){
            throw new ValidateException(player, "Player cannot command units.");
        }

        recordPlayerAction(player);

        for(int id : unitIds){
            Unit unit = Groups.unit.getByID(id);
            if(unit != null && unit.team == player.team() && unit.controller() instanceof CommandAI && unit.type.allowCommand(unit, command)){
                CommandAI ai = (CommandAI)unit.controller();
                boolean reset = command.resetTarget || ai.currentCommand().resetTarget;
                ai.command(command);
                if(reset){
                    ai.targetPos = null;
                    ai.attackTarget = null;
                }
                unit.lastCommanded = player.coloredName();

                //make sure its current stance is valid with its current command
                stancesOut.clear();
                unit.type.getUnitStances(unit, stancesOut);
                for(var stance : content.unitStances()){
                    //disable stances that the unit does not support anymore (TODO: this is slow!)
                    if(ai.hasStance(stance) && !stancesOut.contains(stance)){
                        ai.disableStance(stance);
                    }
                }
            }
        }
    }

    @Remote(called = Loc.server, targets = Loc.both, forward = true)
    public static void setUnitStance(Player player, int[] unitIds, UnitStance stance, boolean enable){
        if(player == null || unitIds == null || stance == null) return;

        if(net.server() && !netServer.admins.allowAction(player, ActionType.commandUnits, event -> {
            event.unitIDs = unitIds;
        })){
            throw new ValidateException(player, "Player cannot command units.");
        }

        recordPlayerAction(player);

        for(int id : unitIds){
            Unit unit = Groups.unit.getByID(id);
            if(unit != null && unit.team == player.team() && unit.controller() instanceof CommandAI){
                CommandAI ai = (CommandAI)unit.controller();
                if(stance == UnitStance.stop){ //not a real stance, just cancels orders
                    ai.clearCommands();
                }else if(unit.type.allowStance(unit, stance)){
                    //if toggle is not allowed, the stance will always be set to true when pressed
                    ai.setStance(stance, !stance.toggle || enable);
                }
                unit.lastCommanded = player.coloredName();
            }
        }
    }

    @Remote(called = Loc.server, targets = Loc.both, forward = true)
    public static void commandHoldPosition(Player player, int[] unitIds, @Nullable Vec2 posTarget){
        if(player == null || unitIds == null || posTarget == null) return;

        if(net.server() && !netServer.admins.allowAction(player, ActionType.commandUnits, event -> {
            event.unitIDs = unitIds;
        })){
            throw new ValidateException(player, "Player cannot command units.");
        }

        recordPlayerAction(player);

        Vec2 safePosTarget = sanitizeRemoteCommandTarget(posTarget);
        for(int id : unitIds){
            Unit unit = Groups.unit.getByID(id);
            if(unit != null && unit.team == player.team() && unit.controller() instanceof CommandAI){
                CommandAI ai = (CommandAI)unit.controller();
                ai.command(UnitCommand.moveCommand);
                ai.setHoldPosition(safePosTarget);
                unit.lastCommanded = player.coloredName();
            }
        }
    }

    @Remote(called = Loc.server, targets = Loc.both, forward = true)
    public static void commandWidowMine(Player player, int[] unitIds, boolean burrow){
        if(player == null || unitIds == null) return;

        if(net.server() && !netServer.admins.allowAction(player, ActionType.commandUnits, event -> {
            event.unitIDs = unitIds;
        })){
            throw new ValidateException(player, "Player cannot command units.");
        }

        recordPlayerAction(player);

        for(int id : unitIds){
            Unit unit = Groups.unit.getByID(id);
            if(unit == null || unit.team != player.team() || !UnitTypes.isWidow(unit)) continue;
            if(UnitTypes.ravenMatrixDisabled(unit)) continue;

            if(burrow){
                UnitTypes.commandWidowBurrow(unit);
            }else{
                UnitTypes.commandWidowUnburrow(unit);
            }

            unit.lastCommanded = player.coloredName();
        }
    }

    @Remote(called = Loc.server, targets = Loc.both, forward = true)
    public static void commandHurricaneLock(Player player, int[] unitIds){
        if(player == null || unitIds == null) return;

        if(net.server() && !netServer.admins.allowAction(player, ActionType.commandUnits, event -> {
            event.unitIDs = unitIds;
        })){
            throw new ValidateException(player, "Player cannot command units.");
        }

        recordPlayerAction(player);

        for(int id : unitIds){
            Unit unit = Groups.unit.getByID(id);
            if(unit == null || unit.team != player.team() || !UnitTypes.isHurricane(unit)) continue;
            if(UnitTypes.ravenMatrixDisabled(unit)) continue;
            UnitTypes.commandHurricaneLock(unit);
            unit.lastCommanded = player.coloredName();
        }
    }

    @Remote(called = Loc.server, targets = Loc.both, forward = true)
    public static void commandPreceptSiege(Player player, int[] unitIds, boolean siege){
        if(player == null || unitIds == null) return;

        if(net.server() && !netServer.admins.allowAction(player, ActionType.commandUnits, event -> {
            event.unitIDs = unitIds;
        })){
            throw new ValidateException(player, "Player cannot command units.");
        }

        preserveLocalTransformSelection(player, unitIds);
        recordPlayerAction(player);

        for(int id : unitIds){
            Unit unit = Groups.unit.getByID(id);
            if(unit == null || unit.team != player.team() || !UnitTypes.isSiegeTank(unit)) continue;
            if(UnitTypes.ravenMatrixDisabled(unit)) continue;
            UnitTypes.commandPreceptSiege(unit, siege);
            unit.lastCommanded = player.coloredName();
        }
    }

    @Remote(called = Loc.server, targets = Loc.both, forward = true)
    public static void commandScepterAirMode(Player player, int[] unitIds, boolean impactMode){
        if(player == null || unitIds == null) return;

        if(net.server() && !netServer.admins.allowAction(player, ActionType.commandUnits, event -> {
            event.unitIDs = unitIds;
        })){
            throw new ValidateException(player, "Player cannot command units.");
        }

        preserveLocalTransformSelection(player, unitIds);
        recordPlayerAction(player);

        for(int id : unitIds){
            Unit unit = Groups.unit.getByID(id);
            if(unit == null || unit.team != player.team() || !UnitTypes.isThor(unit)) continue;
            if(UnitTypes.ravenMatrixDisabled(unit)) continue;
            UnitTypes.commandScepterAirMode(unit, impactMode);
            unit.lastCommanded = player.coloredName();
        }
    }

    @Remote(called = Loc.server, targets = Loc.both, forward = true)
    public static void commandVikingMode(Player player, int[] unitIds, boolean mechMode){
        if(player == null || unitIds == null) return;

        if(net.server() && !netServer.admins.allowAction(player, ActionType.commandUnits, event -> {
            event.unitIDs = unitIds;
        })){
            throw new ValidateException(player, "Player cannot command units.");
        }

        preserveLocalTransformSelection(player, unitIds);
        recordPlayerAction(player);

        for(int id : unitIds){
            Unit unit = Groups.unit.getByID(id);
            if(unit == null || unit.team != player.team() || !UnitTypes.isViking(unit)) continue;
            if(UnitTypes.ravenMatrixDisabled(unit)) continue;
            UnitTypes.commandVikingMode(unit, mechMode);
            unit.lastCommanded = player.coloredName();
        }
    }

    @Remote(called = Loc.server, targets = Loc.both, forward = true)
    public static void commandMaceLocusMode(Player player, int[] unitIds, boolean toLocus){
        if(player == null || unitIds == null) return;

        if(net.server() && !netServer.admins.allowAction(player, ActionType.commandUnits, event -> {
            event.unitIDs = unitIds;
        })){
            throw new ValidateException(player, "Player cannot command units.");
        }

        preserveLocalTransformSelection(player, unitIds);
        recordPlayerAction(player);

        for(int id : unitIds){
            Unit unit = Groups.unit.getByID(id);
            if(unit == null || unit.team != player.team()) continue;
            if(toLocus){
                if(!UnitTypes.isMace(unit) || UnitTypes.ravenMatrixDisabled(unit) || !UnitTypes.maceCanTransformToLocus(unit)) continue;
            }else{
                if(!UnitTypes.isLocus(unit) || UnitTypes.ravenMatrixDisabled(unit) || !UnitTypes.locusCanTransformToMace(unit)) continue;
            }
            UnitTypes.commandMaceLocusMode(unit, toLocus);
            unit.lastCommanded = player.coloredName();
        }
    }

    @Remote(called = Loc.server, targets = Loc.both, forward = true)
    public static void commandLiberatorMode(Player player, int[] unitIds, boolean defenseMode, @Nullable Vec2 zoneTarget){
        if(player == null || unitIds == null) return;

        if(net.server() && !netServer.admins.allowAction(player, ActionType.commandUnits, event -> {
            event.unitIDs = unitIds;
        })){
            throw new ValidateException(player, "Player cannot command units.");
        }

        preserveLocalTransformSelection(player, unitIds);
        recordPlayerAction(player);

        Vec2 safeTarget = sanitizeRemoteCommandTarget(zoneTarget);

        for(int id : unitIds){
            Unit unit = Groups.unit.getByID(id);
            if(unit == null || unit.team != player.team() || !UnitTypes.isLiberator(unit)) continue;
            if(UnitTypes.ravenMatrixDisabled(unit)) continue;
            if(defenseMode){
                if(safeTarget == null) continue;
                UnitTypes.commandLiberatorDefense(unit, safeTarget);
            }else{
                UnitTypes.commandLiberatorFighter(unit);
            }
            unit.lastCommanded = player.coloredName();
        }
    }

    @Remote(called = Loc.server, targets = Loc.both, forward = true)
    public static void commandMedivacAfterburner(Player player, int[] unitIds){
        if(player == null || unitIds == null) return;

        if(net.server() && !netServer.admins.allowAction(player, ActionType.commandUnits, event -> {
            event.unitIDs = unitIds;
        })){
            throw new ValidateException(player, "Player cannot command units.");
        }

        recordPlayerAction(player);

        for(int id : unitIds){
            Unit unit = Groups.unit.getByID(id);
            if(unit == null || unit.team != player.team() || !UnitTypes.isMedivac(unit)) continue;
            if(UnitTypes.ravenMatrixDisabled(unit)) continue;
            UnitTypes.commandMedivacAfterburner(unit);
            unit.lastCommanded = player.coloredName();
        }
    }

    @Remote(called = Loc.server, targets = Loc.both, forward = true)
    public static void commandBarracksStimpack(Player player, int[] unitIds){
        if(player == null || unitIds == null) return;

        if(net.server() && !netServer.admins.allowAction(player, ActionType.commandUnits, event -> {
            event.unitIDs = unitIds;
        })){
            throw new ValidateException(player, "Player cannot command units.");
        }

        recordPlayerAction(player);

        for(int id : unitIds){
            Unit unit = Groups.unit.getByID(id);
            if(unit == null || unit.team != player.team() || !UnitTypes.isBarracksStimpackUnit(unit)) continue;
            if(UnitTypes.ravenMatrixDisabled(unit)) continue;
            UnitTypes.commandBarracksStimpack(unit);
            unit.lastCommanded = player.coloredName();
        }
    }

    @Remote(called = Loc.server, targets = Loc.both, forward = true)
    public static void commandMedivacMovingUnload(Player player, int[] unitIds, boolean enabled){
        if(player == null || unitIds == null) return;

        if(net.server() && !netServer.admins.allowAction(player, ActionType.commandUnits, event -> {
            event.unitIDs = unitIds;
        })){
            throw new ValidateException(player, "Player cannot command units.");
        }

        recordPlayerAction(player);

        for(int id : unitIds){
            Unit unit = Groups.unit.getByID(id);
            if(unit == null || unit.team != player.team() || !UnitTypes.isMedivac(unit)) continue;
            if(UnitTypes.ravenMatrixDisabled(unit)) continue;
            UnitTypes.setMedivacMovingUnload(unit, enabled);
            unit.lastCommanded = player.coloredName();
        }
    }

    @Remote(called = Loc.server, targets = Loc.both, forward = true)
    public static void commandMedivacDropPayload(Player player, int unitId, int payloadIndex){
        if(player == null) return;

        if(net.server() && !netServer.admins.allowAction(player, ActionType.commandUnits, event -> {
            event.unitIDs = new int[]{unitId};
        })){
            throw new ValidateException(player, "Player cannot command units.");
        }

        recordPlayerAction(player);

        Unit unit = Groups.unit.getByID(unitId);
        if(unit == null || unit.team != player.team() || !UnitTypes.isMedivac(unit) || !(unit instanceof Payloadc pay)) return;
        if(UnitTypes.ravenMatrixDisabled(unit)) return;
        if(payloadIndex < 0 || payloadIndex >= pay.payloads().size) return;

        int last = pay.payloads().size - 1;
        if(payloadIndex != last){
            pay.payloads().swap(payloadIndex, last);
        }
        Call.payloadDropped(unit, unit.x, unit.y);
        unit.lastCommanded = player.coloredName();
    }

    @Remote(called = Loc.server, targets = Loc.both, forward = true)
    public static void commandBansheeCloak(Player player, int[] unitIds){
        if(player == null || unitIds == null) return;

        if(net.server() && !netServer.admins.allowAction(player, ActionType.commandUnits, event -> {
            event.unitIDs = unitIds;
        })){
            throw new ValidateException(player, "Player cannot command units.");
        }

        recordPlayerAction(player);

        for(int id : unitIds){
            Unit unit = Groups.unit.getByID(id);
            if(unit == null || unit.team != player.team() || !UnitTypes.isBanshee(unit)) continue;
            UnitTypes.commandBansheeCloak(unit);
            unit.lastCommanded = player.coloredName();
        }
    }

    @Remote(called = Loc.server, targets = Loc.both, forward = true)
    public static void commandGhostCloak(Player player, int[] unitIds){
        if(player == null || unitIds == null) return;

        if(net.server() && !netServer.admins.allowAction(player, ActionType.commandUnits, event -> {
            event.unitIDs = unitIds;
        })){
            throw new ValidateException(player, "Player cannot command units.");
        }

        recordPlayerAction(player);

        for(int id : unitIds){
            Unit unit = Groups.unit.getByID(id);
            if(unit == null || unit.team != player.team() || !UnitTypes.isGhost(unit)) continue;
            if(UnitTypes.ravenMatrixDisabled(unit)) continue;
            UnitTypes.commandGhostCloak(unit);
            unit.lastCommanded = player.coloredName();
        }
    }

    @Remote(called = Loc.server, targets = Loc.both, forward = true)
    public static void commandGhostStableAim(Player player, int[] unitIds, int targetId){
        if(player == null || unitIds == null || targetId < 0) return;

        if(net.server() && !netServer.admins.allowAction(player, ActionType.commandUnits, event -> {
            event.unitIDs = unitIds;
        })){
            throw new ValidateException(player, "Player cannot command units.");
        }

        recordPlayerAction(player);

        Unit target = Groups.unit.getByID(targetId);
        if(!UnitTypes.ghostStableAimValidTarget(target)) return;

        for(int id : unitIds){
            Unit unit = Groups.unit.getByID(id);
            if(unit == null || unit.team != player.team() || !UnitTypes.isGhost(unit)) continue;
            if(UnitTypes.ravenMatrixDisabled(unit)) continue;
            UnitTypes.commandGhostStableAim(unit, target);
            unit.lastCommanded = player.coloredName();
        }
    }

    @Remote(called = Loc.server, targets = Loc.both, forward = true)
    public static void commandGhostStableAimCancel(Player player, int[] unitIds){
        if(player == null || unitIds == null) return;

        if(net.server() && !netServer.admins.allowAction(player, ActionType.commandUnits, event -> {
            event.unitIDs = unitIds;
        })){
            throw new ValidateException(player, "Player cannot command units.");
        }

        recordPlayerAction(player);

        for(int id : unitIds){
            Unit unit = Groups.unit.getByID(id);
            if(unit == null || unit.team != player.team() || !UnitTypes.isGhost(unit)) continue;
            UnitTypes.commandGhostCancelStableAim(unit);
            unit.lastCommanded = player.coloredName();
        }
    }

    @Remote(called = Loc.server, targets = Loc.both, forward = true)
    public static void commandGhostEmp(Player player, int[] unitIds, @Nullable Vec2 target){
        if(player == null || unitIds == null || target == null) return;

        if(net.server() && !netServer.admins.allowAction(player, ActionType.commandUnits, event -> {
            event.unitIDs = unitIds;
        })){
            throw new ValidateException(player, "Player cannot command units.");
        }

        recordPlayerAction(player);

        Vec2 safeTarget = sanitizeRemoteCommandTarget(target);
        if(safeTarget == null) return;

        for(int id : unitIds){
            Unit unit = Groups.unit.getByID(id);
            if(unit == null || unit.team != player.team() || !UnitTypes.isGhost(unit)) continue;
            if(UnitTypes.ravenMatrixDisabled(unit)) continue;
            UnitTypes.commandGhostEmp(unit, safeTarget);
            unit.lastCommanded = player.coloredName();
        }
    }

    @Remote(called = Loc.server, targets = Loc.both, forward = true)
    public static void commandGhostEmpCancel(Player player, int[] unitIds){
        if(player == null || unitIds == null) return;

        if(net.server() && !netServer.admins.allowAction(player, ActionType.commandUnits, event -> {
            event.unitIDs = unitIds;
        })){
            throw new ValidateException(player, "Player cannot command units.");
        }

        recordPlayerAction(player);

        for(int id : unitIds){
            Unit unit = Groups.unit.getByID(id);
            if(unit == null || unit.team != player.team() || !UnitTypes.isGhost(unit)) continue;
            UnitTypes.commandGhostCancelEmp(unit);
            unit.lastCommanded = player.coloredName();
        }
    }

    @Remote(called = Loc.server, targets = Loc.both, forward = true)
    public static void commandReaperKd8(Player player, int[] unitIds, @Nullable Vec2 target){
        if(player == null || unitIds == null || target == null) return;

        if(net.server() && !netServer.admins.allowAction(player, ActionType.commandUnits, event -> {
            event.unitIDs = unitIds;
        })){
            throw new ValidateException(player, "Player cannot command units.");
        }

        recordPlayerAction(player);

        Vec2 safeTarget = sanitizeRemoteCommandTarget(target);
        if(safeTarget == null) return;

        for(int id : unitIds){
            Unit unit = Groups.unit.getByID(id);
            if(unit == null || unit.team != player.team() || unit.type != UnitTypes.reaper) continue;
            if(UnitTypes.ravenMatrixDisabled(unit)) continue;
            UnitTypes.commandReaperKd8(unit, safeTarget);
            unit.lastCommanded = player.coloredName();
        }
    }

    @Remote(called = Loc.server, targets = Loc.both, forward = true)
    public static void commandGhostTacticalNuke(Player player, int[] unitIds, @Nullable Vec2 target){
        if(player == null || unitIds == null || target == null) return;

        if(net.server() && !netServer.admins.allowAction(player, ActionType.commandUnits, event -> {
            event.unitIDs = unitIds;
        })){
            throw new ValidateException(player, "Player cannot command units.");
        }

        recordPlayerAction(player);

        Vec2 safeTarget = sanitizeRemoteCommandTarget(target);
        if(safeTarget == null) return;

        for(int id : unitIds){
            Unit unit = Groups.unit.getByID(id);
            if(unit == null || unit.team != player.team() || !UnitTypes.isGhost(unit)) continue;
            if(UnitTypes.ravenMatrixDisabled(unit)) continue;
            UnitTypes.commandGhostTacticalNuke(unit, safeTarget);
            unit.lastCommanded = player.coloredName();
        }
    }

    @Remote(called = Loc.server, targets = Loc.both, forward = true)
    public static void commandGhostTacticalNukeCancel(Player player, int[] unitIds){
        if(player == null || unitIds == null) return;

        if(net.server() && !netServer.admins.allowAction(player, ActionType.commandUnits, event -> {
            event.unitIDs = unitIds;
        })){
            throw new ValidateException(player, "Player cannot command units.");
        }

        recordPlayerAction(player);

        for(int id : unitIds){
            Unit unit = Groups.unit.getByID(id);
            if(unit == null || unit.team != player.team() || !UnitTypes.isGhost(unit)) continue;
            UnitTypes.commandGhostCancelTacticalNuke(unit);
            unit.lastCommanded = player.coloredName();
        }
    }

    @Remote(called = Loc.server, targets = Loc.both, forward = true)
    public static void commandBattlecruiserYamato(Player player, int[] unitIds, int targetId, int buildPos){
        if(player == null || unitIds == null) return;

        if(net.server() && !netServer.admins.allowAction(player, ActionType.commandUnits, event -> {
            event.unitIDs = unitIds;
        })){
            throw new ValidateException(player, "Player cannot command units.");
        }

        recordPlayerAction(player);

        Teamc target = null;
        if(targetId >= 0){
            Unit u = Groups.unit.getByID(targetId);
            if(u != null && u.isValid()){
                target = u;
            }
        }else if(buildPos >= 0){
            Building b = world.build(buildPos);
            if(b != null && b.isValid()){
                target = b;
            }
        }
        if(target == null) return;

        for(int id : unitIds){
            Unit unit = Groups.unit.getByID(id);
            if(unit == null || unit.team != player.team() || !UnitTypes.isBattlecruiser(unit)) continue;
            if(UnitTypes.ravenMatrixDisabled(unit)) continue;
            UnitTypes.commandBattlecruiserYamato(unit, target);
            unit.lastCommanded = player.coloredName();
        }
    }

    @Remote(called = Loc.server, targets = Loc.both, forward = true)
    public static void commandBattlecruiserWarp(Player player, int[] unitIds, @Nullable Vec2 target){
        if(player == null || unitIds == null || target == null) return;

        if(net.server() && !netServer.admins.allowAction(player, ActionType.commandUnits, event -> {
            event.unitIDs = unitIds;
        })){
            throw new ValidateException(player, "Player cannot command units.");
        }

        recordPlayerAction(player);

        Vec2 safeTarget = sanitizeRemoteCommandTarget(target);
        if(safeTarget == null) return;

        for(int id : unitIds){
            Unit unit = Groups.unit.getByID(id);
            if(unit == null || unit.team != player.team() || !UnitTypes.isBattlecruiser(unit)) continue;
            if(UnitTypes.ravenMatrixDisabled(unit)) continue;
            UnitTypes.commandBattlecruiserWarp(unit, safeTarget);
            unit.lastCommanded = player.coloredName();
        }
    }

    @Remote(called = Loc.server, targets = Loc.both, forward = true)
    public static void commandAvertDeployTurret(Player player, int[] unitIds, @Nullable Vec2 target){
        if(player == null || unitIds == null || target == null) return;

        if(net.server() && !netServer.admins.allowAction(player, ActionType.commandUnits, event -> {
            event.unitIDs = unitIds;
        })){
            throw new ValidateException(player, "Player cannot command units.");
        }

        recordPlayerAction(player);

        Vec2 safeTarget = sanitizeRemoteCommandTarget(target);
        if(safeTarget == null) return;

        for(int id : unitIds){
            Unit unit = Groups.unit.getByID(id);
            if(unit == null || unit.team != player.team() || !UnitTypes.isRaven(unit)) continue;
            if(UnitTypes.ravenMatrixDisabled(unit)) continue;
            UnitTypes.commandRavenDeployTurret(unit, safeTarget);
            unit.lastCommanded = player.coloredName();
        }
    }

    @Remote(called = Loc.server, targets = Loc.both, forward = true)
    public static void commandAvertAntiArmor(Player player, int[] unitIds, @Nullable Vec2 target){
        if(player == null || unitIds == null || target == null) return;

        if(net.server() && !netServer.admins.allowAction(player, ActionType.commandUnits, event -> {
            event.unitIDs = unitIds;
        })){
            throw new ValidateException(player, "Player cannot command units.");
        }

        recordPlayerAction(player);

        Vec2 safeTarget = sanitizeRemoteCommandTarget(target);
        if(safeTarget == null) return;

        for(int id : unitIds){
            Unit unit = Groups.unit.getByID(id);
            if(unit == null || unit.team != player.team() || !UnitTypes.isRaven(unit)) continue;
            if(UnitTypes.ravenMatrixDisabled(unit)) continue;
            UnitTypes.commandRavenAntiArmor(unit, safeTarget);
            unit.lastCommanded = player.coloredName();
        }
    }

    @Remote(called = Loc.server, targets = Loc.both, forward = true)
    public static void commandAvertMatrix(Player player, int[] unitIds, int targetId){
        if(player == null || unitIds == null || targetId < 0) return;

        if(net.server() && !netServer.admins.allowAction(player, ActionType.commandUnits, event -> {
            event.unitIDs = unitIds;
        })){
            throw new ValidateException(player, "Player cannot command units.");
        }

        recordPlayerAction(player);

        Unit target = Groups.unit.getByID(targetId);
        if(target == null || !target.isValid()) return;

        for(int id : unitIds){
            Unit unit = Groups.unit.getByID(id);
            if(unit == null || unit.team != player.team() || !UnitTypes.isRaven(unit)) continue;
            if(UnitTypes.ravenMatrixDisabled(unit)) continue;
            UnitTypes.commandRavenMatrix(unit, target);
            unit.lastCommanded = player.coloredName();
        }
    }

    @Remote(called = Loc.server, targets = Loc.both, forward = true)
    public static void commandBunkerLoadUnits(Player player, int bunkerPos, int[] unitIds){
        if(player == null || unitIds == null || unitIds.length == 0) return;

        if(net.server() && !netServer.admins.allowAction(player, ActionType.commandUnits, event -> {
            event.unitIDs = unitIds;
        })){
            throw new ValidateException(player, "Player cannot command units.");
        }

        recordPlayerAction(player);

        Building building = world.build(bunkerPos);
        if(!(building instanceof BunkerBlock.BunkerBuild bunker) || bunker.team != player.team()) return;
        if(bunker.recycling) return;

        bunker.commandLoadUnits(unitIds);
        bunker.updateLastAccess(player);
    }

    @Remote(called = Loc.server, targets = Loc.both, forward = true)
    public static void commandBuilding(Player player, int[] buildings, Vec2 target){
        if(player == null || target == null) return;

        if(net.server() && !netServer.admins.allowAction(player, ActionType.commandBuilding, event -> {
            event.buildingPositions = buildings;
        })){
            throw new ValidateException(player, "Player cannot command buildings.");
        }

        recordPlayerAction(player);

        for(int pos : buildings){
            var build = world.build(pos);

            if(build == null || build.team() != player.team() || !build.isCommandable()) continue;

            build.onCommand(target);
            build.updateLastAccess(player);

            if(!state.isPaused() && player == Vars.player){
                Fx.moveCommand.at(target);
            }

            Events.fire(new BuildingCommandEvent(player, build, target));
        }

    }

    @Remote(called = Loc.server, targets = Loc.both, forward = true)
    public static void requestItem(Player player, Building build, Item item, int amount){
        if(player == null || build == null || !build.interactable(player.team()) || !player.within(build, itemTransferRange) || player.dead() || amount <= 0) return;

        if(net.server() && (!Units.canInteract(player, build) ||
        !netServer.admins.allowAction(player, ActionType.withdrawItem, build.tile, action -> {
            action.item = item;
            action.itemAmount = amount;
        }))){
            throw new ValidateException(player, "Player cannot request items.");
        }

        recordPlayerAction(player);

        Call.takeItems(build, item, Math.min(player.unit().maxAccepted(item), amount), player.unit());
        Events.fire(new WithdrawEvent(build, player, item, amount));
    }

    @Remote(targets = Loc.both, forward = true, called = Loc.server)
    public static void transferInventory(Player player, Building build){
        if(player == null || build == null || !player.within(build, itemTransferRange) || build.items == null || player.dead() || !build.allowDeposit()) return;

        if(net.server() && (player.unit().stack.amount <= 0 || !Units.canInteract(player, build) ||
        //to avoid rejecting deposit packets that happen to overlap due to packet speed differences, the actual cap is double the cooldown with 2 deposits.
        (!player.isLocal() && !player.itemDepositRate.allow((long)(state.rules.itemDepositCooldown * 1000 * 2), 2)) ||

        !netServer.admins.allowAction(player, ActionType.depositItem, build.tile, action -> {
            action.itemAmount = player.unit().stack.amount;
            action.item = player.unit().item();
        }))){
            throw new ValidateException(player, "Player cannot transfer an item.");
        }

        recordPlayerAction(player);

        var unit = player.unit();
        Item item = unit.item();
        int accepted = build.acceptStack(item, unit.stack.amount, unit);

        Call.transferItemTo(unit, item, accepted, unit.x, unit.y, build);

        Events.fire(new DepositEvent(build, player, item, accepted));
    }

    @Remote(variants = Variant.one)
    public static void removeQueueBlock(int x, int y, boolean breaking){
        if(!player.dead()){
            player.unit().removeBuild(x, y, breaking);
        }
    }

    @Remote(targets = Loc.both, called = Loc.server)
    public static void requestUnitPayload(Player player, Unit target){
        if(player == null || target == null) return;
        Unit unit = player.unit();
        if(!(unit instanceof Payloadc)) return;
        Payloadc pay = (Payloadc)unit;

        if(target.isAI() && target.isGrounded() && pay.canPickup(target)
        && target.within(unit, unit.type.hitSize * 2f + target.type.hitSize * 2f)){
            Call.pickedUnitPayload(unit, target);
        }
    }

    @Remote(targets = Loc.both, called = Loc.server)
    public static void requestBuildPayload(Player player, Building build){
        if(player == null || build == null) return;
        Unit unit = player.unit();
        if(!(unit instanceof Payloadc)) return;
        Payloadc pay = (Payloadc)unit;

        if(!unit.within(build, tilesize * build.block.size * 1.2f + tilesize * 5f)) return;

        if(net.server() && !netServer.admins.allowAction(player, ActionType.pickupBlock, build.tile, action -> {
            action.unit = unit;
        })){
            throw new ValidateException(player, "Player cannot pick up a block.");
        }

        if(state.teams.canInteract(unit.team, build.team)){
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

    @Remote(targets = Loc.server, called = Loc.server)
    public static void pickedUnitPayload(Unit unit, Unit target){
        if(target != null && unit instanceof Payloadc){
            ((Payloadc)unit).pickup(target);
        }else if(target != null){
            target.remove();
        }
    }

    @Remote(targets = Loc.server, called = Loc.server)
    public static void pickedBuildPayload(Unit unit, Building build, boolean onGround){
        if(build != null && unit instanceof Payloadc){
            Payloadc pay = (Payloadc)unit;
            if(onGround){
                if(build.block.buildVisibility != BuildVisibility.hidden && build.canPickup() && pay.canPickup(build)){
                    pay.pickup(build);
                }else{
                    Fx.unitPickup.at(build);
                    build.tile.remove();
                }
            }else{
                Payload current = build.getPayload();
                if(current != null && pay.canPickupPayload(current)){
                    Payload taken = build.takePayload();
                    if(taken != null){
                        pay.addPayload(taken);
                        Fx.unitPickup.at(build);
                    }
                }
            }

        }else if(build != null && onGround){
            Fx.unitPickup.at(build);
            build.tile.remove();
        }
    }

    @Remote(targets = Loc.both, called = Loc.server)
    public static void requestDropPayload(Player player, float x, float y){
        if(player == null || net.client() || player.dead()) return;

        Payloadc pay = (Payloadc)player.unit();

        if(pay.payloads().isEmpty()) return;

        if(net.server() && !netServer.admins.allowAction(player, ActionType.dropPayload, player.unit().tileOn(), action -> {
            action.payload = pay.payloads().peek();
        })){
            throw new ValidateException(player, "Player cannot drop a payload.");
        }

        //apply margin of error
        Tmp.v1.set(x, y).sub(pay).limit(tilesize * 4f).add(pay);
        float cx = Tmp.v1.x, cy = Tmp.v1.y;

        Call.payloadDropped(player.unit(), cx, cy);
    }

    @Remote(called = Loc.server, targets = Loc.server)
    public static void payloadDropped(Unit unit, float x, float y){
        if(unit instanceof Payloadc){
            Payloadc pay = (Payloadc)unit;
            float prevx = pay.x(), prevy = pay.y();
            pay.set(x, y);
            pay.dropLastPayload();
            pay.set(prevx, prevy);
        }
    }

    @Remote(called = Loc.server)
    public static void unitEnteredPayload(Unit unit, Building build){
        if(unit == null || build == null || unit.team != build.team) return;

        unit.remove();

        //reset the enter command
        if(unit.controller() instanceof CommandAI){
            CommandAI ai = (CommandAI)unit.controller();
            if(ai.command == UnitCommand.enterPayloadCommand){
                ai.clearCommands();
                ai.command = UnitCommand.moveCommand;
            }
        }

        //clear removed state of unit so it can be synced
        if(Vars.net.client()){
            Vars.netClient.clearRemovedEntity(unit.id);
        }

        UnitPayload unitPay = new UnitPayload(unit);

        if(build.acceptPayload(build, unitPay)){
            Fx.unitDrop.at(build);
            build.handlePayload(build, unitPay);
        }
    }

    @Remote(targets = Loc.client, called = Loc.server)
    public static void dropItem(Player player, float angle){
        if(player == null || player.unit() == null) return;

        if(net.server() && player.unit().stack.amount <= 0){
            throw new ValidateException(player, "Player cannot drop an item.");
        }

        var unit = player.unit();
        Fx.dropItem.at(unit.x, unit.y, angle, Color.white, unit.item());
        unit.clearItem();
    }

    @Remote(targets = Loc.both, called = Loc.server, forward = true, unreliable = true)
    public static void rotateBlock(@Nullable Player player, Building build, boolean direction){
        if(build == null) return;

        if(net.server() && (!Units.canInteract(player, build) ||
        !netServer.admins.allowAction(player, ActionType.rotate, build.tile, action -> action.rotation = Mathf.mod(build.rotation + Mathf.sign(direction), 4)))){
            throw new ValidateException(player, "Player cannot rotate a block.");
        }

        recordPlayerAction(player);
        if(player != null) build.updateLastAccess(player);
        int previous = build.rotation;
        build.rotation = Mathf.mod(build.rotation + Mathf.sign(direction), 4);
        build.updateProximity();
        build.noSleep();
        Fx.rotateBlock.at(build.x, build.y, build.block.size);
        if(!headless) Sounds.blockRotate.at(build, 1f + Mathf.range(0.1f), 1f);
        Events.fire(new BuildRotateEvent(build, player == null ? null : player.unit(), previous));
    }

    @Remote(targets = Loc.both, called = Loc.both, forward = true)
    public static void tileConfig(@Nullable Player player, Building build, @Nullable Object value){
        if(build == null && net.server()) throw new ValidateException(player, "building is null");
        if(build == null) return;

        if(net.server() && (!Units.canInteract(player, build) ||
        !netServer.admins.allowAction(player, ActionType.configure, build.tile, action -> action.config = value))){

            if(player.con != null){
                var packet = new TileConfigCallPacket(); //undo the config on the client
                packet.player = player;
                packet.build = build;
                packet.value = build.config();
                player.con.send(packet, true);
            }

            if(!player.isLocal()){
                throw new ValidateException(player, "Player cannot configure a tile.");
            }else{
                return;
            }
        }
        recordPlayerAction(player);
        if(player != null) build.updateLastAccess(player);
        build.configured(player == null || player.dead() ? null : player.unit(), value);
        Events.fire(new ConfigEvent(build, player, value));
    }

    //only useful for servers or local mods, and is not replicated across clients
    //uses unreliable packets due to high frequency
    @Remote(targets = Loc.both, called = Loc.both, unreliable = true)
    public static void tileTap(@Nullable Player player, Tile tile){
        if(tile == null) return;

        recordPlayerAction(player);
        Events.fire(new TapEvent(player, tile));
    }

    @Remote(targets = Loc.both, called = Loc.server, forward = true)
    public static void buildingControlSelect(Player player, Building build){
        if(player == null || build == null || player.dead()) return;

        //make sure player is allowed to control the building
        if(net.server() && !netServer.admins.allowAction(player, ActionType.buildSelect, action -> action.tile = build.tile)){
            throw new ValidateException(player, "Player cannot control a building.");
        }

        if(player.team() == build.team && build.canControlSelect(player.unit())){
            var before = player.unit();

            build.onControlSelect(player.unit());

            if(!before.dead && before.spawnedByCore && !before.isPlayer()){
                Call.unitDespawn(before);
            }
        }
    }

    @Remote(called = Loc.server)
    public static void unitBuildingControlSelect(Unit unit, Building build){
        if(unit == null || unit.dead()) return;

        //client skips checks to prevent ghost units
        if(unit.team() == build.team && (net.client() || build.canControlSelect(unit))){
            build.onControlSelect(unit);
        }
    }

    @Remote(targets = Loc.both, called = Loc.both, forward = true)
    public static void unitControl(Player player, @Nullable Unit unit){
        if(player == null) return;

        //make sure player is allowed to control the unit
        if(net.server() && (!state.rules.possessionAllowed || !netServer.admins.allowAction(player, ActionType.control, action -> action.unit = unit))){
            throw new ValidateException(player, "Player cannot control a unit.");
        }

        //clear player unit when they possess a core
        if(unit == null){ //just clear the unit (is this used?)
            player.clearUnit();
            //make sure it's AI controlled, so players can't overwrite each other
        }else if(unit.isAI() && unit.team == player.team() && !unit.dead && unit.playerControllable()){
            if(net.client() && player.isLocal()){
                player.justSwitchFrom = player.unit();
                player.justSwitchTo = unit;
            }

            //TODO range check for docking?
            var before = player.unit();

            player.unit(unit);

            if(before != null){
                if(before.spawnedByCore){
                    unit.dockedType = before.type;
                }else if(before.dockedType != null && before.dockedType.coreUnitDock){
                    //direct dock transfer???
                    unit.dockedType = before.dockedType;
                }

                if(before.spawnedByCore && !before.isPlayer()){
                    Call.unitDespawn(before);
                }
            }

            Time.run(Fx.unitSpirit.lifetime, () -> Fx.unitControl.at(unit.x, unit.y, 0f, unit));
            if(!player.dead()){
                Fx.unitSpirit.at(player.x, player.y, 0f, unit);
            }
        }else if(net.server()){
            //reject forwarding the packet if the unit was dead, AI or team
            throw new ValidateException(player, "Player attempted to control invalid unit.");
        }

        Events.fire(new UnitControlEvent(player, unit));
    }

    @Remote(targets = Loc.both, called = Loc.server, forward = true)
    public static void unitClear(Player player){
        //Disabled: Players cannot spawn or control core units
        //Core units are no longer generated
        return;
    }

    /** Adds an input lock; if this function returns true, input is locked. Used for mod 'cutscenes' or custom camera panning. */
    public void addLock(Boolp lock){
        inputLocks.add(lock);
    }

    /** @return whether most input is locked, for 'cutscenes' */
    public boolean locked(){
        return inputLocks.contains(Boolp::get);
    }

    public Eachable<BuildPlan> allPlans(){
        return allPlans;
    }

    public boolean isUsingSchematic(){
        return !selectPlans.isEmpty();
    }

    public void spectate(Unit unit){
        spectatingPlayer = -1;
        spectating = unit;
        camera.position.set(unit);
    }

    public void spectatePlayer(@Nullable Player target){
        if(target == null){
            spectatingPlayer = -1;
            spectating = null;
            return;
        }

        spectatingPlayer = target.id;
        if(!target.dead() && target.unit() != null && target.unit().isValid()){
            spectating = target.unit();
            camera.position.set(spectating);
        }else{
            spectating = null;
            camera.position.set(target.x, target.y);
        }
    }

    public @Nullable Player spectatingPlayer(){
        return spectatingPlayer < 0 ? null : Groups.player.getByID(spectatingPlayer);
    }

    private boolean isSpectatorMode(){
        if(player == null || player.team() == null || state == null || state.isMenu()) return false;
        if(!net.active()) return false;
        Team team = player.team();
        return team == Team.derelict || !team.data().isAlive();
    }

    public void reset(){
        logicCutscene = false;
        commandBuildings.clear();
        selectedUnits.clear();
        subgroupOrder.clear();
        subgroupSet.clear();
        subgroupUnits.clear();
        subgroupBuildings.clear();
        activeSubgroup = -1;
        selectedResource = null;
        itemDepositCooldown = 0f;
        Arrays.fill(controlGroups, null);
        lastUnit = null;
        spectatingPlayer = -1;
        spectating = null;
        preservedTransformSelection.clear();
        preservedTransformSelectionTime = 0f;
        lastPlans.clear();
        player.shooting = false;
    }

    public void preserveUnitSelection(int[] unitIds, float duration){
        if(unitIds == null || unitIds.length == 0) return;

        preservedTransformSelection.clear();
        for(int id : unitIds){
            if(id >= 0){
                preservedTransformSelection.add(id);
            }
        }
        preservedTransformSelectionTime = Math.max(preservedTransformSelectionTime, duration);
    }

    public void preserveUnitSelection(IntSeq unitIds, float duration){
        if(unitIds == null || unitIds.isEmpty()) return;
        preserveUnitSelection(unitIds.toArray(), duration);
    }

    public void clearPreservedUnitSelection(){
        preservedTransformSelection.clear();
        preservedTransformSelectionTime = 0f;
    }

    public void restorePreservedUnitSelection(){
        if(preservedTransformSelectionTime <= 0f){
            clearPreservedUnitSelection();
            return;
        }

        preservedTransformSelectionTime = Math.max(0f, preservedTransformSelectionTime - Time.delta);

        if(!commandMode || !selectedUnits.isEmpty() || !commandBuildings.isEmpty() || selectedResource != null){
            clearPreservedUnitSelection();
            return;
        }

        boolean changed = false;
        for(int i = 0; i < preservedTransformSelection.size; i++){
            Unit unit = Groups.unit.getByID(preservedTransformSelection.get(i));
            if(unit == null || !unit.isValid() || unit.team != player.team() || !unit.allowCommand()) continue;
            boolean alreadySelected = false;
            for(int j = 0; j < selectedUnits.size; j++){
                Unit selected = selectedUnits.get(j);
                if(selected != null && selected.id == unit.id){
                    alreadySelected = true;
                    break;
                }
            }
            if(alreadySelected) continue;
            selectedUnits.add(unit);
            changed = true;
        }

        if(changed){
            Events.fire(Trigger.unitCommandChange);
            clearPreservedUnitSelection();
        }else if(preservedTransformSelectionTime <= 0f){
            clearPreservedUnitSelection();
        }
    }

    private static void preserveLocalTransformSelection(Player player, int[] unitIds){
        if(player == null || unitIds == null || unitIds.length == 0) return;
        if(control == null || control.input == null) return;
        if(Vars.player != player) return;
        control.input.preserveUnitSelection(unitIds, transformSelectionPreserveDuration);
    }

    public void update(){
        if(spectatingPlayer >= 0){
            Player target = Groups.player.getByID(spectatingPlayer);
            if(target == null || !target.isAdded() || target.team() == null || (!isSpectatorMode() && target.team() != player.team())){
                spectatingPlayer = -1;
                spectating = null;
            }else if(!target.dead() && target.unit() != null && target.unit().isValid()){
                spectating = target.unit();
            }else{
                spectating = null;
            }
        }else if(spectating != null && (!spectating.isValid() || (!isSpectatorMode() && spectating.team != player.team()))){
            spectating = null;
        }

        if(isSpectatorMode() && (commandMode || commandRect || selectedResource != null || !selectedUnits.isEmpty() || !commandBuildings.isEmpty())){
            commandMode = false;
            commandRect = false;
            selectedUnits.clear();
            commandBuildings.clear();
            selectedResource = null;
            Events.fire(Trigger.unitCommandChange);
        }

        if(logicCutscene && !renderer.isCutscene()){
            Core.camera.position.lerpDelta(logicCamPan, logicCamSpeed);
        }else{
            logicCutsceneZoom = -1f;
        }

        itemDepositCooldown -= Time.delta / 60f;

        refreshSelectedBuildingsAfterReplace();
        if(selectedResource != null){
            if(!isResourceTile(selectedResource)){
                selectedResource = null;
            }
        }

        if(!commandMode){
            commandRect = false;
        }

        if(player.isBuilder()){
            var playerPlans = player.unit().plans;
            if(player.unit() != lastUnit && playerPlans.size <= 1){
                playerPlans.ensureCapacity(lastPlans.size);
                for(var plan : lastPlans){
                    playerPlans.addLast(plan);
                }
            }
            if(lastPlans.size != playerPlans.size || (lastPlans.size > 0 && playerPlans.size > 0 && lastPlans.first() != playerPlans.first())){
                lastPlans.clear();
                for(var plan : playerPlans){
                    lastPlans.addLast(plan);
                }
            }
        }

        lastUnit = player.unit();

        playerPlanTree.clear();
        if(!player.dead()){
            player.unit().plans.each(playerPlanTree::insert);
        }

        player.typing = ui.chatfrag.shown();

        if(player.dead()){
            droppingItem = false;
        }

        if(player.isBuilder()){
            player.unit().updateBuilding(isBuilding);
        }

        //you don't want selected blocks while locked, looks weird
        if(locked()){
            block = null;
        }

        wasShooting = player.shooting;

        //only reset the controlled type and control a unit after the timer runs out
        //essentially, this means the client waits for ~1 second after controlling something before trying to control something else automatically
        if(!player.dead() && (recentRespawnTimer -= Time.delta / 70f) <= 0f && player.justSwitchFrom != player.unit()){
            controlledType = player.unit().type;
        }

        if(controlledType != null && player.dead() && controlledType.playerControllable){
            Unit unit = Units.closest(player.team(), player.x, player.y, u -> !u.isPlayer() && u.type == controlledType && u.playerControllable() && !u.dead);

            if(unit != null){
                //only trying controlling once a second to prevent packet spam
                if(!net.client() || controlInterval.get(0, 70f)){
                    recentRespawnTimer = 1f;
                    Call.unitControl(player, unit);
                }
            }
        }
    }

    public void checkUnit(){
        if(controlledType != null && controlledType.playerControllable){
            Unit unit = Units.closest(player.team(), player.x, player.y, u -> !u.isPlayer() && u.type == controlledType && !u.dead);
            if(unit == null && controlledType == UnitTypes.block){
                Building build = world.buildWorld(player.x, player.y);
                if(build instanceof ControlBlock){
                    ControlBlock cont = (ControlBlock)build;
                    unit = cont.canControl() ? cont.unit() : null;
                }else{
                    unit = null;
                }
            }

            if(unit != null){
                if(net.client()){
                    Call.unitControl(player, unit);
                }else{
                    unit.controller(player);
                }
            }
        }
    }

    public void tryPickupPayload(){
        Unit unit = player.unit();
        if(!(unit instanceof Payloadc)) return;
        Payloadc pay = (Payloadc)unit;

        Unit target = Units.closest(player.team(), pay.x(), pay.y(), unit.type.hitSize * 2f, u -> u.isAI() && u.isGrounded() && pay.canPickup(u) && u.within(unit, u.hitSize + unit.hitSize));
        if(target != null){
            Call.requestUnitPayload(player, target);
        }else{
            Building build = world.buildWorld(pay.x(), pay.y());

            if(build != null && state.teams.canInteract(unit.team, build.team)){
                Call.requestBuildPayload(player, build);
            }
        }
    }

    public void tryDropPayload(){
        Unit unit = player.unit();
        if(!(unit instanceof Payloadc)) return;

        Call.requestDropPayload(player, player.x, player.y);
    }

    public float getMouseX(){
        return Core.input.mouseX();
    }

    public float getMouseY(){
        return Core.input.mouseY();
    }

    public Vec2 mouseWorld(){
        return mouseWorld(getMouseX(), getMouseY(), Tmp.v1);
    }

    public Vec2 mouseWorld(float screenX, float screenY){
        return mouseWorld(screenX, screenY, Tmp.v1);
    }

    public Vec2 mouseWorld(float screenX, float screenY, Vec2 out){
        if(renderer != null && !Mathf.zero(renderer.getViewRotation())){
            return renderer.screenToWorld(screenX, screenY, out);
        }
        return Core.input.mouseWorld(screenX, screenY);
    }

    public float mouseWorldX(){
        return mouseWorld(getMouseX(), getMouseY(), Tmp.v1).x;
    }

    public float mouseWorldY(){
        return mouseWorld(getMouseX(), getMouseY(), Tmp.v1).y;
    }

    private boolean useScreenRectSelection(){
        return renderer != null;
    }

    private Rect normalizeScreenRect(float x1, float y1, float x2, float y2, Rect out){
        return out.set(x1, y1, x2 - x1, y2 - y1).normalize();
    }

    private Rect screenRectToWorldBounds(Rect screenRect, Rect out){
        if(renderer == null) return out.set(0f, 0f, 0f, 0f);
        Vec2 p1 = renderer.screenToWorld(screenRect.x, screenRect.y, Tmp.v1);
        Vec2 p2 = renderer.screenToWorld(screenRect.x + screenRect.width, screenRect.y, Tmp.v2);
        Vec2 p3 = renderer.screenToWorld(screenRect.x, screenRect.y + screenRect.height, Tmp.v3);
        Vec2 p4 = renderer.screenToWorld(screenRect.x + screenRect.width, screenRect.y + screenRect.height, Tmp.v4);
        float minX = Math.min(Math.min(p1.x, p2.x), Math.min(p3.x, p4.x));
        float maxX = Math.max(Math.max(p1.x, p2.x), Math.max(p3.x, p4.x));
        float minY = Math.min(Math.min(p1.y, p2.y), Math.min(p3.y, p4.y));
        float maxY = Math.max(Math.max(p1.y, p2.y), Math.max(p3.y, p4.y));
        return out.set(minX, minY, maxX - minX, maxY - minY);
    }

    public void drawScreenSelectionRect(){
        if(!useScreenRectSelection() || !commandRect || !commandMode) return;
        float sx1 = commandRectScreenX, sy1 = commandRectScreenY;
        float sx2 = getMouseX(), sy2 = getMouseY();
        float sx = Math.min(sx1, sx2);
        float sy = Math.min(sy1, sy2);
        float sw = Math.abs(sx2 - sx1);
        float sh = Math.abs(sy2 - sy1);
        if(sw <= 0f && sh <= 0f) return;

        Mat prev = Tmp.m1.set(Draw.proj());
        Draw.proj(0f, 0f, Core.graphics.getWidth(), Core.graphics.getHeight());
        Color rectColor = Color.valueOf("5bff53");
        Draw.color(rectColor, 0.2f);
        Fill.crect(sx, sy, sw, sh);
        Draw.color(rectColor);
        Lines.stroke(1.5f);
        Lines.rect(sx, sy, sw, sh);
        Draw.reset();
        Draw.proj(prev);
    }

    public void buildPlacementUI(Table table){

    }

    public void buildUI(Group group){

    }

    public void updateState(){
        if(state.isMenu()){
            controlledType = null;
            logicCutscene = false;
            config.forceHide();
            commandMode = commandRect = false;
        }
    }

    //TODO when shift is held? ctrl?
    public boolean multiUnitSelect(){
        return false;
    }

    public void selectUnitsRect(){
        if(commandMode && commandRect){
            boolean useScreen = useScreenRectSelection();
            float dx;
            float dy;
            float dragThreshold;
            if(useScreen){
                dx = getMouseX() - commandRectScreenX;
                dy = getMouseY() - commandRectScreenY;
                dragThreshold = tilesize * 0.25f * (renderer == null ? 1f : renderer.camerascale);
            }else{
                dx = mouseWorldX() - commandRectX;
                dy = mouseWorldY() - commandRectY;
                dragThreshold = tilesize * 0.25f;
            }
            boolean dragged = dx * dx + dy * dy > dragThreshold * dragThreshold;
            if(!tappedOne || dragged){
                var units = useScreen ? selectedCommandUnitsScreen(commandRectScreenX, commandRectScreenY, getMouseX(), getMouseY()) :
                    selectedCommandUnits(commandRectX, commandRectY, mouseWorldX() - commandRectX, mouseWorldY() - commandRectY);
                boolean multi = multiUnitSelect();

                if(multi){
                    //tiny brain method of unique addition
                    selectedUnits.removeAll(units);
                }else if(!units.isEmpty()){
                    //Clear if we selected any units
                    selectedUnits.clear();
                    commandBuildings.clear();
                }

                selectedUnits.addAll(units);

                //Select buildings only if no units are in the box
                if(units.isEmpty()){
                    var buildings = useScreen ? selectedCommandBuildingsScreen(commandRectScreenX, commandRectScreenY, getMouseX(), getMouseY()) :
                        selectedCommandBuildings(commandRectX, commandRectY, mouseWorldX() - commandRectX, mouseWorldY() - commandRectY);
                    if(!buildings.isEmpty()){
                        if(multi){
                            commandBuildings.removeAll(buildings);
                        }else{
                            selectedUnits.clear();
                            commandBuildings.clear();
                        }
                        commandBuildings.addAll(buildings);
                    }else if(!multi && units.isEmpty()){
                        commandBuildings.clear();
                    }
                }

                if(units.isEmpty() && commandBuildings.isEmpty()){
                    Tile crystal = useScreen ? findCrystalInScreenRect(commandRectScreenX, commandRectScreenY, getMouseX(), getMouseY()) :
                        findCrystalInRect(commandRectX, commandRectY, mouseWorldX(), mouseWorldY());
                    if(crystal != null){
                        if(!multi){
                            selectedUnits.clear();
                            commandBuildings.clear();
                        }
                        selectedResource = selectedResource == crystal ? null : crystal;
                    }
                }else{
                    if(!multi){
                        selectedResource = null;
                    }
                }

                Events.fire(Trigger.unitCommandChange);
            }
            commandRect = false;
        }
    }

    public Seq<Unit> abilitySubgroupUnits(){
        refreshSubgroupSelection();
        return subgroupUnits;
    }

    public Seq<Building> abilitySubgroupBuildings(){
        refreshSubgroupSelection();
        return subgroupBuildings;
    }

    private void refreshSelectedBuildingsAfterReplace(){
        boolean changed = false;

        //Replace invalid buildings (construction/upgrade completion) with the new building at the same tile.
        for(int i = 0; i < commandBuildings.size; i++){
            Building build = commandBuildings.get(i);
            if(build == null || build.isValid()) continue;
            Building next = world.build(build.pos());
            if(next != null && next.isValid() && next.team == player.team()){
                commandBuildings.set(i, next);
                changed = true;
            }
        }

        //Remove invalid/enemy/duplicates while keeping stable order.
        commandBuildingDedup.clear();
        for(int i = commandBuildings.size - 1; i >= 0; i--){
            Building build = commandBuildings.get(i);
            if(build == null || !build.isValid() || build.team != player.team() || !commandBuildingDedup.add(build.id)){
                commandBuildings.remove(i);
                changed = true;
            }
        }

        if(changed){
            Events.fire(Trigger.unitCommandChange);
        }
    }

    public boolean replaceSelectedUnit(@Nullable Unit from, @Nullable Unit to){
        if(from == null || to == null) return false;

        boolean changed = false;
        boolean hasTarget = false;

        for(int i = 0; i < selectedUnits.size; i++){
            Unit unit = selectedUnits.get(i);
            if(unit == to || (unit != null && unit.id == to.id)){
                hasTarget = true;
                break;
            }
        }

        for(int i = 0; i < selectedUnits.size; i++){
            Unit unit = selectedUnits.get(i);
            if(unit != from && (unit == null || unit.id != from.id)) continue;

            if(hasTarget){
                selectedUnits.remove(i);
                i--;
            }else{
                selectedUnits.set(i, to);
                hasTarget = true;
            }
            changed = true;
        }

        if(changed){
            Events.fire(Trigger.unitCommandChange);
        }

        return changed;
    }

    public boolean isUnitInActiveAbilitySubgroup(@Nullable UnitType type){
        refreshSubgroupSelection();
        if(type == null || activeSubgroup < 0) return false;
        return unitSelectionGroup(type) == activeSubgroup;
    }

    public boolean isBuildingInActiveAbilitySubgroup(@Nullable Building build){
        refreshSubgroupSelection();
        if(build == null || activeSubgroup < 0) return false;
        return buildingSelectionGroup(build) == activeSubgroup;
    }

    public boolean cycleAbilitySubgroup(boolean forward){
        refreshSubgroupSelection();
        if(subgroupOrder.size <= 1) return false;
        int index = subgroupOrder.indexOf(activeSubgroup);
        if(index < 0) index = 0;
        int step = forward ? 1 : -1;
        int nextIndex = index + step;
        if(nextIndex < 0) nextIndex = subgroupOrder.size - 1;
        if(nextIndex >= subgroupOrder.size) nextIndex = 0;
        activeSubgroup = subgroupOrder.get(nextIndex);
        rebuildSubgroupUnits();
        return true;
    }

    private void refreshSubgroupSelection(){
        subgroupSet.clear();
        subgroupOrder.clear();
        for(Unit unit : selectedUnits){
            if(unit == null || !unit.isValid()) continue;
            int group = unitSelectionGroup(unit.type);
            if(subgroupSet.add(group)){
                subgroupOrder.add(group);
            }
        }
        for(Building build : commandBuildings){
            if(build == null || !build.isValid()) continue;
            int group = buildingSelectionGroup(build);
            if(subgroupSet.add(group)){
                subgroupOrder.add(group);
            }
        }
        subgroupOrder.sort();

        if(subgroupOrder.isEmpty()){
            activeSubgroup = -1;
            subgroupUnits.clear();
            subgroupBuildings.clear();
            return;
        }

        if(activeSubgroup < 0 || !subgroupSet.contains(activeSubgroup)){
            activeSubgroup = subgroupOrder.first();
        }
        rebuildSubgroupUnits();
    }

    private void rebuildSubgroupUnits(){
        subgroupUnits.clear();
        subgroupBuildings.clear();
        if(activeSubgroup < 0) return;
        if(isBuildingSubgroup(activeSubgroup)){
            for(Building build : commandBuildings){
                if(build == null || !build.isValid()) continue;
                if(buildingSelectionGroup(build) == activeSubgroup){
                    subgroupBuildings.add(build);
                }
            }
        }else{
            for(Unit unit : selectedUnits){
                if(unit == null || !unit.isValid()) continue;
                if(unitSelectionGroup(unit.type) == activeSubgroup){
                    subgroupUnits.add(unit);
                }
            }
        }
    }

    private int unitSelectionGroup(@Nullable UnitType type){
        if(type == null) return 9999;
        if(type == UnitTypes.avert) return 0;
        if(type == UnitTypes.ghost) return 1;
        if(type == UnitTypes.antumbra) return 2; //battlecruiser
        if(type == UnitTypes.dagger) return 3; //marine
        if(type == UnitTypes.fortress) return 4; //marauder
        if(type == UnitTypes.precept) return 5; //siege tank
        if(type == UnitTypes.liberator) return 6;
        if(type == UnitTypes.hurricane) return 7;
        if(type == UnitTypes.reaper) return 8;
        if(type == UnitTypes.flare) return 9; //viking
        if(type == UnitTypes.locus || type == UnitTypes.mace) return 10; //hellion/hellbat
        if(type == UnitTypes.horizon) return 11; //banshee
        if(type == UnitTypes.mega) return 12; //medivac
        if(type == UnitTypes.nova) return 13; //scv
        if(type == UnitTypes.crawler) return 14; //widow mine
        if(type == UnitTypes.scepter) return 15; //thor
        return 1000 + type.id;
    }

    private boolean isBuildingSubgroup(int group){
        return group >= 20000;
    }

    private int buildingSelectionGroup(@Nullable Building build){
        if(build == null || build.block == null) return 99999;
        int priority = buildingSelectionPriority(build);
        int major = 1000 - Math.max(priority, 0);
        return 20000 + major * 1000 + build.block.id;
    }

    public void selectTypedUnits(){
        if(commandMode){
            Unit unit = selectedCommandUnit(mouseWorldX(), mouseWorldY());
            if(unit != null){
                selectedUnits.clear();
                camera.bounds(Tmp.r1);
                selectedUnits.addAll(selectedCommandUnits(Tmp.r1.x, Tmp.r1.y, Tmp.r1.width, Tmp.r1.height, u -> u.type == unit.type));
                Events.fire(Trigger.unitCommandChange);
                return;
            }

            Building build = buildAt(mouseWorldX(), mouseWorldY());
            if(build != null && build.team == player.team()){
                selectedUnits.clear();
                commandBuildings.clear();
                selectedResource = null;
                camera.bounds(Tmp.r1);
                var buildings = selectedCommandBuildingsRaw(Tmp.r1.x, Tmp.r1.y, Tmp.r1.width, Tmp.r1.height);
                for(Building b : buildings){
                    if(b != null && b.isValid() && b.team == player.team() && b.block == build.block){
                        commandBuildings.add(b);
                    }
                }
                Events.fire(Trigger.unitCommandChange);
            }
        }
    }

    public void tapCommandUnit(){
        if(commandMode){

            Unit unit = selectedCommandUnit(mouseWorldX(), mouseWorldY());
            if(unit != null){
                boolean shiftHeld = Core.input.keyDown(KeyCode.shiftLeft) || Core.input.keyDown(KeyCode.shiftRight);
                boolean ctrlHeld = Core.input.keyDown(KeyCode.controlLeft) || Core.input.keyDown(KeyCode.controlRight);

                if(ctrlHeld){
                    //Ctrl held: select all units of same type on screen
                    selectedUnits.clear();
                    camera.bounds(Tmp.r1);
                    selectedUnits.addAll(selectedCommandUnits(Tmp.r1.x, Tmp.r1.y, Tmp.r1.width, Tmp.r1.height, u -> u.type == unit.type));
                }else if(shiftHeld){
                    //Shift held: toggle unit in selection
                    if(!selectedUnits.contains(unit)){
                        selectedUnits.add(unit);
                    }else{
                        selectedUnits.remove(unit);
                    }
                }else{
                    //No modifier: replace selection with only this unit
                    selectedUnits.clear();
                    selectedUnits.add(unit);
                }
                if(!shiftHeld){
                    commandBuildings.clear();
                }
                if(!shiftHeld){
                    selectedResource = null;
                }
                }else{
                    boolean shiftHeld = Core.input.keyDown(KeyCode.shiftLeft) || Core.input.keyDown(KeyCode.shiftRight);
                    boolean ctrlHeld = Core.input.keyDown(KeyCode.controlLeft) || Core.input.keyDown(KeyCode.controlRight);

                    //Only check for buildings if no unit was found
                    Building build = buildAt(mouseWorldX(), mouseWorldY());
                    if(build != null && build.team == player.team()){
                        if(!shiftHeld){
                            selectedUnits.clear();
                        }

                        if(ctrlHeld){
                            //Ctrl held: select all buildings of same type on screen
                            if(!shiftHeld){
                                commandBuildings.clear();
                            }
                            camera.bounds(Tmp.r1);
                            var buildings = selectedCommandBuildingsRaw(Tmp.r1.x, Tmp.r1.y, Tmp.r1.width, Tmp.r1.height);
                            for(var b : buildings){
                                if(b.block == build.block){
                                    commandBuildings.add(b);
                                }
                            }
                        }else if(shiftHeld){
                            //Shift held: toggle building in selection
                            if(!commandBuildings.contains(build)){
                                commandBuildings.add(build);
                            }else{
                                commandBuildings.remove(build);
                            }
                        }else{
                            //No modifier: replace selection with only this building
                            commandBuildings.clear();
                            commandBuildings.add(build);
                        }
                        if(!shiftHeld){
                            selectedResource = null;
                        }
                    }else{
                        Tile tile = world.tileWorld(mouseWorldX(), mouseWorldY());
                        trySelectResource(tile, shiftHeld);
                    }
                }
                Events.fire(Trigger.unitCommandChange);
            }
        }

    public void unassignBuildingsFromControl(Seq<Building> buildings){
        if(buildings.isEmpty()) return;
        tmpControlBuildings.clear();
        for(Building building : buildings){
            tmpControlBuildings.add(building.pos());
            tmpControlBuildings.add(building.id);
        }
        for(IntSeq group : controlGroups){
            if(group != null){
                group.removeAll(tmpControlBuildings);
            }
        }
    }

    public void commandTap(float screenX, float screenY){
        commandTap(screenX, screenY, false);
    }

    protected float clampCommandX(float worldX){
        float maxX = Math.max(world.unitWidth() - tilesize, 0f);
        if(Float.isNaN(worldX) || Float.isInfinite(worldX)){
            return maxX * 0.5f;
        }
        return Mathf.clamp(worldX, 0f, maxX);
    }

    protected float clampCommandY(float worldY){
        float maxY = Math.max(world.unitHeight() - tilesize, 0f);
        if(Float.isNaN(worldY) || Float.isInfinite(worldY)){
            return maxY * 0.5f;
        }
        return Mathf.clamp(worldY, 0f, maxY);
    }

    protected boolean isValidCommandWorld(float worldX, float worldY){
        if(Float.isNaN(worldX) || Float.isNaN(worldY) || Float.isInfinite(worldX) || Float.isInfinite(worldY)) return false;
        float width = Math.max(world.unitWidth(), tilesize);
        float height = Math.max(world.unitHeight(), tilesize);
        return worldX >= -width && worldX <= width * 2f && worldY >= -height && worldY <= height * 2f;
    }

    protected Vec2 clampCommandTarget(Vec2 target){
        target.set(clampCommandX(target.x), clampCommandY(target.y));
        return target;
    }

    private @Nullable Unit pickScvFromCandidates(){
        if(scvBuildCandidates.isEmpty()) return null;
        if(scvBuildAssignIndex >= scvBuildCandidates.size) scvBuildAssignIndex = 0;
        Unit chosen = scvBuildCandidates.get(scvBuildAssignIndex);
        scvBuildAssignIndex = (scvBuildAssignIndex + 1) % scvBuildCandidates.size;
        return chosen;
    }

    protected @Nullable Unit pickScvBuildUnit(boolean queue, boolean allowBusyFallback){
        scvBuildCandidates.clear();
        for(Unit unit : selectedUnits){
            if(unit == null || !unit.isValid() || unit.type != UnitTypes.nova || !unit.canBuild()) continue;
            if(!queue && unit.isBuilding()) continue;
            scvBuildCandidates.add(unit);
        }

        Unit chosen = pickScvFromCandidates();
        if(chosen != null || !allowBusyFallback) return chosen;

        scvBuildCandidates.clear();
        for(Unit unit : selectedUnits){
            if(unit == null || !unit.isValid() || unit.type != UnitTypes.nova || !unit.canBuild()) continue;
            scvBuildCandidates.add(unit);
        }

        return pickScvFromCandidates();
    }

    public void commandTap(float screenX, float screenY, boolean queue){
        if(commandMode){
            //right click: move to position

            //move to location - TODO right click instead?
            Vec2 rawTarget = mouseWorld(screenX, screenY);
            if(!isValidCommandWorld(rawTarget.x, rawTarget.y)) return;
            Vec2 target = clampCommandTarget(rawTarget.cpy());

            if(selectedUnits.size > 0){
                boolean hasNova = false;
                for(Unit unit : selectedUnits){
                    if(unit == null || !unit.isValid()) continue;
                    if(UnitTypes.isBattlecruiser(unit)){
                        UnitTypes.commandBattlecruiserCancelYamato(unit);
                    }
                    if(unit.type == UnitTypes.nova){
                        hasNova = true;
                    }
                }

                Building buildAtPos = world.buildWorld(rawTarget.x, rawTarget.y);
                if(buildAtPos == null){
                    Tile tileAt = world.tileWorld(rawTarget.x, rawTarget.y);
                    if(tileAt != null && tileAt.build != null){
                        buildAtPos = tileAt.build;
                    }
                }
                if(buildAtPos == null){
                    Tile tileAt = world.tileWorld(target.x, target.y);
                    if(tileAt != null && tileAt.build != null){
                        buildAtPos = tileAt.build;
                    }
                }
                if(buildAtPos == null){
                    buildAtPos = world.buildWorld(target.x, target.y);
                }

                if(hasNova && buildAtPos != null && buildAtPos.team() == player.team() && buildAtPos.block == Blocks.ventCondenser){
                    Tile ventTile = findVentTile(buildAtPos);
                    if(ventTile != null){
                        Vec2 harvestPos = Tmp.v1.set(ventTile.worldx(), ventTile.worldy());
                        for(Unit unit : selectedUnits){
                            if(unit.type != UnitTypes.nova) continue;
                            if(unit.controller() instanceof CommandAI){
                                ((CommandAI)unit.controller()).setHarvestTarget(harvestPos);
                            }else if(unit.controller() instanceof HarvestAI){
                                ((HarvestAI)unit.controller()).setHarvestTarget(harvestPos);
                            }
                        }
                        return;
                    }
                }

                // Check if clicking on a harvestable resource
                Tile tile = world.tileWorld(target.x, target.y);
                Tile resource = resolveResourceTile(tile);
                if(resource == null && hasNova){
                    Tile nearVent = findNearestVentCenter(target.x, target.y, 1);
                    if(nearVent != null){
                        resource = nearVent;
                    }
                }
                if(resource != null){
                    if(resource.floor() instanceof SteamVent){
                        if(hasNova && hasVentCondenser(resource)){
                            Vec2 harvestPos = Tmp.v1.set(resource.worldx(), resource.worldy());
                            for(Unit unit : selectedUnits){
                                if(unit.type != UnitTypes.nova) continue;
                                if(unit.controller() instanceof CommandAI){
                                    ((CommandAI)unit.controller()).setHarvestTarget(harvestPos);
                                }else if(unit.controller() instanceof HarvestAI){
                                    ((HarvestAI)unit.controller()).setHarvestTarget(harvestPos);
                                }
                            }
                            return;
                        }
                        boolean showRefineryNotice = false;
                        float resX = resource.worldx();
                        float resY = resource.worldy();
                        for(Unit unit : selectedUnits){
                            if(unit == null || !unit.isValid()) continue;
                            Tmp.v1.set(unit.x - resX, unit.y - resY);
                            if(Tmp.v1.len2() < 0.001f){
                                Tmp.v1.set(1f, 0f);
                            }
                            Tmp.v1.setLength(unit.hitSize / 2f + tilesize / 2f);
                            Vec2 movePos = Tmp.v2.set(resX + Tmp.v1.x, resY + Tmp.v1.y);
                            if(unit.isCommandable()){
                                unit.command().commandPosition(movePos);
                            }else if(unit.controller() instanceof CommandAI ai){
                                ai.commandPosition(movePos);
                            }
                            if(unit.type == UnitTypes.nova){
                                showRefineryNotice = true;
                            }
                        }
                        if(showRefineryNotice){
                            ui.hudfrag.showLeftNotice("必须建造精炼厂才能开采");
                        }
                        return;
                    }
                    if(resource.block() instanceof CrystalMineralWall){
                        // Switch units to harvest command and target this tile
                        for(Unit unit : selectedUnits){
                            if(unit.controller() instanceof CommandAI){
                                ((CommandAI)unit.controller()).setHarvestTarget(target);
                            }else if(unit.controller() instanceof HarvestAI){
                                ((HarvestAI)unit.controller()).setHarvestTarget(target);
                            }
                        }
                        return;
                    }
                }

                Teamc teamTarget = null;
                //SCV right-click on constructing building: assist construction by adding build plans.
                if(buildAtPos instanceof mindustry.world.blocks.ConstructBlock.ConstructBuild construct && construct.team == player.team()){
                    Block cur = construct.current;
                    if(cur != null && cur != Blocks.air){
                        Unit chosen = pickScvBuildUnit(queue, true);
                        if(chosen != null){
                            int tx = construct.tile.x, ty = construct.tile.y;
                            BuildPlan plan = new BuildPlan(tx, ty, construct.rotation, cur, cur.saveConfig ? construct.lastConfig : null);
                            plan.requireClose = true;
                            chosen.addBuild(plan);
                            chosen.updateBuilding(true);

                            float targetX = tx * tilesize + cur.offset;
                            float targetY = ty * tilesize + cur.offset;
                            Call.commandUnits(player, new int[]{chosen.id}, null, null, new Vec2(targetX, targetY), queue, true, false);
                            return;
                        }else if(hasNova){
                            return;
                        }
                    }
                }
                boolean attackableAnyTeamBuild = buildAtPos != null && Units.targetableAllTeams(buildAtPos);
                if(buildAtPos != null && (buildAtPos.team() != player.team() || attackableAnyTeamBuild)){
                    teamTarget = buildAtPos;
                }else{
                    Unit enemyUnit = selectedEnemyUnit(target.x, target.y);
                    if(enemyUnit != null){
                        teamTarget = enemyUnit;
                    }
                }

                if(teamTarget == null){
                    if(buildAtPos != null && buildAtPos.team() == player.team()){
                        teamTarget = buildAtPos;
                    }else{
                        Unit allyUnit = Units.closest(player.team(), target.x, target.y, 40f, u -> u.team == player.team() && !selectedUnits.contains(u));
                        if(allyUnit != null && allyUnit.within(target.x, target.y, allyUnit.hitSize / 2f)){
                            teamTarget = allyUnit;
                        }
                    }
                }

                //Medivac right-click on ally unit: use load command instead of follow.
                if(teamTarget instanceof Unit allyTarget && allyTarget.team() == player.team()){
                    IntSeq medivacIds = new IntSeq();
                    boolean onlyMedivacSelected = !selectedUnits.isEmpty();
                    for(Unit unit : selectedUnits){
                        if(!UnitTypes.isMedivac(unit)){
                            onlyMedivacSelected = false;
                            break;
                        }
                    }

                    if(onlyMedivacSelected){
                        for(Unit unit : selectedUnits){
                            if(unit == null || !unit.isValid() || !UnitTypes.medivacCanPickup(unit, allyTarget)) continue;
                            medivacIds.add(unit.id);
                        }
                    }

                    if(medivacIds.size > 0){
                        int[] ids = medivacIds.toArray();
                        Call.setUnitCommand(player, ids, UnitCommand.loadUnitsCommand);
                        Call.commandMedivacMovingUnload(player, ids, false);
                        Call.commandUnits(player, ids, null, allyTarget, target, queue, true, false);
                        return;
                    }
                }

                if(Core.input.keyDown(KeyCode.r) && teamTarget != null && teamTarget.team() == player.team()){
                    IntSeq scvIds = new IntSeq();
                    for(Unit unit : selectedUnits){
                        if(unit == null || !unit.isValid() || unit.type != UnitTypes.nova) continue;
                        scvIds.add(unit.id);
                    }
                    if(scvIds.size > 0){
                        Call.setUnitCommand(player, scvIds.toArray(), UnitCommand.repairCommand);
                    }
                }

                int[] ids = new int[selectedUnits.size];
                for(int i = 0; i < ids.length; i++){
                    ids[i] = selectedUnits.get(i).id;
                }

                boolean attackableAllyBuild = teamTarget instanceof Building b && Units.targetableAllTeams(b);
                if(teamTarget != null && (teamTarget.team() != player.team() || attackableAllyBuild)){
                    Events.fire(Trigger.unitCommandAttack);
                }else if(teamTarget == null){
                    Events.fire(Trigger.unitCommandPosition);
                }

                int maxChunkSize = 200;

                Building teamBuild = teamTarget instanceof Building ? (Building)teamTarget : null;
                Unit teamUnit = teamTarget instanceof Unit ? (Unit)teamTarget : null;

                if(ids.length > maxChunkSize){
                    for(int i = 0; i < ids.length; i += maxChunkSize){
                        int[] data = Arrays.copyOfRange(ids, i, Math.min(i + maxChunkSize, ids.length));
                        Call.commandUnits(player, data, teamBuild, teamUnit, target, queue, i + maxChunkSize >= ids.length, false);
                    }
                }else{
                    Call.commandUnits(player, ids, teamBuild, teamUnit, target, queue, true, false);
                }
            }

            if(commandBuildings.size > 0){
                Call.commandBuilding(player, commandBuildings.mapInt(b -> b.pos()).toArray(), target);
            }
        }
    }

    public void drawCommand(Unit sel){
        float radius = Math.max(1f, sel.hitSize / 2f + selectionSolidRadiusOffset);
        Lines.stroke(selectionRingStroke);
        Draw.color(Color.green);
        drawSelectionCircle(sel.x, sel.y, radius);
        Draw.reset();
    }

    public void drawCommand(Building build){
        float radius = Math.max(1f, build.hitSize() / 2f + selectionSolidRadiusOffset);
        Lines.stroke(selectionRingStroke);
        Draw.color(Color.green);
        drawSelectionCircle(build.x, build.y, radius);
        Draw.reset();
    }

    private void drawCommandDashed(Unit sel){
        float radius = Math.max(1f, sel.hitSize / 2f + selectionRotatingDashedRadiusOffset);
        Lines.stroke(selectionRingStroke);
        Draw.color(Color.green);
        float lenScale = 0.6f;
        int verts = 10 + (int)(radius * lenScale);
        if((verts & 1) == 1) verts++;
        float step = 360f / verts;
        float rot = (Time.time * 2f) % 360f;
        for(int i = 0; i < verts; i += 2){
            float a1 = rot + i * step + 90f;
            float a2 = rot + (i + 1) * step + 90f;
            Tmp.v1.set(radius, 0f).rotate(a1);
            Tmp.v2.set(radius, 0f).rotate(a2);
            Lines.line(sel.x + Tmp.v1.x, sel.y + Tmp.v1.y, sel.x + Tmp.v2.x, sel.y + Tmp.v2.y);
        }
        Draw.reset();
    }

    private void drawCommandDashed(Building build){
        float radius = Math.max(1f, build.hitSize() / 2f + selectionRotatingDashedRadiusOffset);
        Lines.stroke(selectionRingStroke);
        Draw.color(Color.green);
        float lenScale = 0.6f;
        int verts = 10 + (int)(radius * lenScale);
        if((verts & 1) == 1) verts++;

        float step = 360f / verts;
        float rot = (Time.time * 2f) % 360f;

        for(int i = 0; i < verts; i += 2){
            float a1 = rot + i * step + 90f;
            float a2 = rot + (i + 1) * step + 90f;

            Tmp.v1.set(radius, 0f).rotate(a1);
            Tmp.v2.set(radius, 0f).rotate(a2);
            Lines.line(build.x + Tmp.v1.x, build.y + Tmp.v1.y, build.x + Tmp.v2.x, build.y + Tmp.v2.y);
        }
        Draw.reset();
    }

    public void drawCommanded(){
        if(Core.settings.getBool("selectionringabove", true)) return;
        //Draw outer ring on top of units
        Draw.draw(selectionRingLayer(), () -> {
            drawCommandedRing(true);
            drawCommandedRing(false);
            drawCommandedBuildings();
        });

        Draw.draw(selectionRingLayer(), () -> {
            drawUnitWaypoints();
            drawCommandedTargets();
            drawCommandedRally();
        });
    }

    public void drawCommandedTop(){
        if(!Core.settings.getBool("selectionringabove", true)) return;
        Draw.z(selectionRingLayer());
        drawCommandedRing(true);
        Draw.z(selectionRingLayer());
        drawCommandedRing(false);
        Draw.z(selectionRingLayer());
        drawCommandedBuildings();
        Draw.z(selectionRingLayer());
        drawUnitWaypoints();
        Draw.z(selectionRingLayer());
        drawCommandedTargets();
        Draw.z(selectionRingLayer());
        drawCommandedRally();
        Draw.reset();
    }

    public void drawCommandedTargets(){
        if(commandMode){
            for(Unit unit : selectedUnits){
                if(unit.controller() instanceof CommandAI){
                    //Intentionally left blank: remove red attack target lock box.
                }
            }
        }
    }

    public void drawCommandedRing(boolean flying){
        if(commandMode){
            for(Unit unit : selectedUnits){
                if((unit.isFlying() || unit.type.allowLegStep) != flying) continue;

                float rad = unit.hitSize / 2f;

                //Draw only the outer ring on top of unit
                Lines.stroke(selectionRingStroke);
                Draw.color(Color.green);
                drawSelectionCircle(unit.x, unit.y, Math.max(1f, rad + selectionSolidRadiusOffset));
            }
            Draw.reset();
        }
    }

    private void drawCommandedBuildings(){
        if(commandMode && !commandBuildings.isEmpty()){
            Lines.stroke(selectionRingStroke);
            Draw.color(Color.green);
            for(Building build : commandBuildings){
                if(build == null || !build.isValid()) continue;
                drawSelectionCircle(build.x, build.y, Math.max(1f, build.hitSize() / 2f + selectionSolidRadiusOffset));
            }
            Draw.reset();
        }
    }

    private void drawSelectionCircle(float x, float y, float radius){
        int sides = Mathf.clamp((int)(radius * 3f), 32, 128);
        Lines.poly(x, y, sides, radius);
    }

    private void drawCommandedRally(){
        if(!commandMode || commandBuildings.isEmpty()) return;

        TextureRegion rally = ui.rallyPointRegion == null ? Icon.cancel.getRegion() : ui.rallyPointRegion;
        float size = tilesize;

        for(Building build : commandBuildings){
            if(build == null || !build.isValid()) continue;
            Vec2 cpos = build.getCommandPosition();
            if(cpos == null) continue;

            drawRallyLine(build.x, build.y, cpos.x, cpos.y);
            Draw.color();
            Draw.rect(rally, cpos.x, cpos.y, size, size);
        }
        Draw.reset();
    }

    private void drawRallyLine(float x1, float y1, float x2, float y2){
        float dst = Mathf.dst(x1, y1, x2, y2);
        if(dst < 1f) return;

        float spacing = tilesize * 0.6f;
        float speed = tilesize * 0.2f;
        float offset = (Time.time * speed) % spacing;

        Draw.color(rallyColor);
        for(float d = offset; d < dst; d += spacing){
            float t = d / dst;
            float px = Mathf.lerp(x1, x2, t);
            float py = Mathf.lerp(y1, y2, t);
            Fill.circle(px, py, 0.9f);
        }
    }

    private void drawWaypointLine(float x1, float y1, float x2, float y2, Color color, boolean moving, boolean fadeOut){
        float dst = Mathf.dst(x1, y1, x2, y2);
        if(dst < 1f) return;

        float spacing = tilesize * 0.6f;
        float offset = 0f;

        if(moving){
            float speed = tilesize * 0.04f;
            offset = (Time.time * speed) % spacing;
        }

        for(float d = offset; d < dst; d += spacing){
            float t = d / dst;
            float px = Mathf.lerp(x1, x2, t);
            float py = Mathf.lerp(y1, y2, t);
            float alpha = fadeOut ? Mathf.clamp(1f - t) : 1f;
            Draw.color(color.r, color.g, color.b, color.a * alpha);
            Fill.circle(px, py, 0.9f);
        }
        Draw.reset();
    }

    private void drawUnitWaypoints(){
        if(player == null) return;
        boolean spectator = isSpectatorMode();
        if(!spectator && selectedUnits.isEmpty()) return;

        Team focusTeam = null;
        if(spectator && ui != null && ui.hudfrag != null){
            Player focus = ui.hudfrag.spectatorCameraFocusedPlayer();
            if(focus != null){
                focusTeam = focus.team();
            }
        }

        Rect viewBounds = null;
        if(spectator){
            viewBounds = camera.bounds(r1);
            viewBounds.grow(tilesize * 2f);
        }

        TextureRegion waypoint = Core.atlas.find("wayPoint");
        if(!waypoint.found()) waypoint = Core.atlas.find("wayPoints/wayPoint");
        TextureRegion waypointRed = Core.atlas.find("wayPoint-red");
        if(!waypointRed.found()) waypointRed = Core.atlas.find("wayPoints/wayPoint-red");

        TextureRegion waypointBackground = Core.atlas.find("wayPoint-background");
        if(!waypointBackground.found()) waypointBackground = Core.atlas.find("wayPoints/wayPoint-background");
        TextureRegion waypointBackgroundRed = Core.atlas.find("wayPoint-background-red");
        if(!waypointBackgroundRed.found()) waypointBackgroundRed = Core.atlas.find("wayPoints/wayPoint-background-red");

        float waypointWidth = tilesize;
        float waypointHeight = tilesize;
        float waypointBackgroundWidth = tilesize;
        float waypointBackgroundHeight = tilesize;

        float timeSeconds = Time.time / 60f;
        float pulse = 0.5f + 0.5f * Mathf.sin(timeSeconds * Mathf.PI2 * 2f);
        float pulseScale = Mathf.lerp(1f, 1.12f, pulse);
        float bgExpandDuration = 0.2f;
        float bgGapDuration = 0.8f;
        float bgCycle = bgExpandDuration + bgGapDuration;
        float bgCycleTime = timeSeconds % bgCycle;
        boolean drawBgPulse = bgCycleTime <= bgExpandDuration;
        float bgScale = 0f;
        float bgAlpha = 0f;
        if(drawBgPulse){
            float bgProgress = Mathf.clamp(bgCycleTime / bgExpandDuration);
            bgScale = Mathf.lerp(0.5f, 2f, Interp.sineOut.apply(bgProgress));
            bgAlpha = 1f - Interp.pow2Out.apply(bgProgress);
        }

        Seq<Position> points = new Seq<>();
        BoolSeq attackPoints = new BoolSeq();
        Iterable<Unit> waypointUnits = spectator ? Groups.unit : selectedUnits;

        for(Unit unit : waypointUnits){
            if(unit == null || !unit.isValid()) continue;
            if(!spectator){
                if(unit.team != player.team()) continue;
                if(unit.inFogTo(player.team())) continue;
            }else{
                if(focusTeam != null && unit.team != focusTeam) continue;
                if(viewBounds != null && !viewBounds.contains(unit.x, unit.y)) continue;
            }
            if(!unit.allowCommand()) continue;
            if(!(unit.controller() instanceof CommandAI)) continue;

            CommandAI ai = (CommandAI)unit.controller();
            UnitCommand cmd = ai.currentCommand();
            if(cmd == UnitCommand.harvestCommand) continue;
            if(ai.targetPos == null && ai.followTarget == null && ai.attackTarget == null && ai.commandQueue.size == 0) continue;
            Position current = ai.targetPos;
            if(ai.followTarget instanceof Teamc){
                current = ai.followTarget;
            }else if(ai.attackTarget instanceof Building){
                current = ai.attackTarget;
            }
            int queueOffset = 0;
            if(current == null && ai.commandQueue.size > 0){
                current = ai.commandQueue.first();
                queueOffset = 1;
            }
            if(current == null) continue;

            points.clear();
            attackPoints.clear();
            points.add(current);
            boolean currentAttack = ai.attackTarget != null || (ai.targetPos != null && ai.attackMovePosition);
            if(ai.currentCommand() == UnitCommand.repairCommand){
                currentAttack = false;
            }
            if(current instanceof Teamc teamc){
                if(ai.currentCommand() != UnitCommand.repairCommand && ai.followTarget != teamc){
                    currentAttack |= ai.attackTarget == teamc || teamc.team() != unit.team;
                }
            }
            attackPoints.add(currentAttack);

            for(int i = queueOffset; i < ai.commandQueue.size; i++){
                Position next = ai.commandQueue.get(i);
                if(next == null) continue;
                points.add(next);
                boolean queuedAttack = false;
                if(next instanceof Teamc teamc){
                    if(ai.currentCommand() != UnitCommand.repairCommand){
                        queuedAttack = ai.attackTarget == teamc || teamc.team() != unit.team;
                    }
                }
                attackPoints.add(queuedAttack);
            }

            if(points.size > 1){
                drawWaypointLine(unit.x, unit.y, points.first().getX(), points.first().getY(),
                    attackPoints.get(0) ? Pal.remove : Color.green, true, false);
            }

            for(int i = 1; i < points.size; i++){
                Position prev = points.get(i - 1);
                Position next = points.get(i);
                boolean fadeLastSegment = points.size >= 2 && i == points.size - 1;
                drawWaypointLine(prev.getX(), prev.getY(), next.getX(), next.getY(),
                    attackPoints.get(i) ? Pal.remove : Color.green, true, fadeLastSegment);
            }

            for(int i = 0; i < points.size; i++){
                Position point = points.get(i);
                boolean attackPoint = attackPoints.get(i);
                TextureRegion pointRegion = attackPoint && waypointRed.found() ? waypointRed : waypoint;
                TextureRegion pointBackgroundRegion = attackPoint && waypointBackgroundRed.found() ? waypointBackgroundRed : waypointBackground;

                if(pointBackgroundRegion.found()){
                    if(drawBgPulse){
                        Draw.color(1f, 1f, 1f, bgAlpha);
                        Draw.rect(pointBackgroundRegion, point.getX(), point.getY(), waypointBackgroundWidth * bgScale, waypointBackgroundHeight * bgScale);
                    }
                }else{
                    if(drawBgPulse){
                        Drawf.square(point.getX(), point.getY(), (tilesize * 0.5f) * bgScale,
                            (attackPoint ? Pal.remove : Pal.accent).write(Tmp.c1).a(bgAlpha));
                    }
                }

                Draw.color();
                if(pointRegion.found()){
                    Draw.rect(pointRegion, point.getX(), point.getY(), waypointWidth * pulseScale, waypointHeight * pulseScale);
                }else{
                    Drawf.square(point.getX(), point.getY(), (tilesize * 0.5f) * pulseScale, attackPoint ? Pal.remove : Pal.accent);
                }
            }
        }

        Draw.reset();
    }

    public void drawCommanded(boolean flying){
        float lineLimit = 6.5f;
        int sides = 6;
        float alpha = 0.5f;

        if(commandMode){
            //happens sometimes
            selectedUnits.removeAll(u -> !u.allowCommand());

            for(Unit unit : selectedUnits){

                Color color = unit.controller() instanceof LogicAI ? Team.malis.color : Pal.accent;

                Position lastPos = null;

                if(unit.controller() instanceof CommandAI){
                    CommandAI ai = (CommandAI)unit.controller();
                    var cmd =  ai.currentCommand();
                    lastPos = ai.attackTarget != null ? ai.attackTarget : ai.targetPos;

                    if((unit.isFlying() || unit.type.allowLegStep) != flying) continue;

                    //draw target line (always show, even with a single waypoint)
                    if(ai.targetPos != null && (cmd.drawTarget || ai.commandQueue.size > 0)){
                        Position lineDest = ai.attackTarget != null ? ai.attackTarget : ai.targetPos;
                        Drawf.limitLine(unit, lineDest, unit.hitSize / unitSelectRadScl + 1f, lineLimit, color.write(Tmp.c1).a(alpha));

                        if(ai.attackTarget == null){
                            Drawf.square(lineDest.getX(), lineDest.getY(), 3.5f, color.write(Tmp.c1).a(alpha));

                            if(cmd == UnitCommand.enterPayloadCommand){
                                var build = buildAt(lineDest.getX(), lineDest.getY());
                                if(build != null && build.block.acceptsUnitPayloads && build.team == unit.team){
                                    Drawf.selected(build, color);
                                }else{
                                    Drawf.cross(lineDest.getX(), lineDest.getY(), 7f, Pal.remove);
                                }
                            }
                        }
                    }
                }

                if(lastPos == null){
                    lastPos = unit;
                }

                if(unit.controller() instanceof CommandAI){
                    CommandAI ai = (CommandAI)unit.controller();
                    //draw command queue
                    if((ai.currentCommand().drawTarget || ai.commandQueue.size > 0) && ai.commandQueue.size > 0){
                        for(var next : ai.commandQueue){
                            Drawf.limitLine(lastPos, next, lineLimit, lineLimit, color.write(Tmp.c1).a(alpha));
                            lastPos = next;

                            if(next instanceof Vec2){
                                Vec2 vec = (Vec2)next;
                                Drawf.square(vec.x, vec.y, 3.5f, color.write(Tmp.c1).a(alpha));
                            }
                        }
                    }

                    if(ai.targetPos != null && ai.currentCommand() == UnitCommand.loopPayloadCommand && unit instanceof Payloadc){
                        Payloadc pay = (Payloadc)unit;
                        Draw.color(color, 0.4f + Mathf.absin(5f, 0.5f));
                        TextureRegion region = pay.hasPayload() ? Icon.download.getRegion() : Icon.upload.getRegion();
                        float offset = 11f;
                        float size = 8f;
                        Draw.rect(region, ai.targetPos.x, ai.targetPos.y + offset, size, size / region.ratio());

                        if(ai.commandQueue.size > 0){
                            region = !pay.hasPayload() ? Icon.download.getRegion() : Icon.upload.getRegion();
                            Draw.rect(region, ai.commandQueue.first().getX(), ai.commandQueue.first().getY() + offset, size, size / region.ratio());
                        }
                        Draw.color();
                    }
                }
            }

            if(flying){
                for(var commandBuild : commandBuildings){
                    if(commandBuild != null){
                        var cpos = commandBuild.getCommandPosition();

                        if(cpos != null){
                            drawRallyLine(commandBuild.x, commandBuild.y, cpos.x, cpos.y);
                            TextureRegion rally = ui.rallyPointRegion == null ? Icon.cancel.getRegion() : ui.rallyPointRegion;
                            Draw.color();
                            float size = tilesize;
                            Draw.rect(rally, cpos.x, cpos.y, size, size);
                        }
                    }
                }
            }
        }

        Draw.reset();

    }

    public void drawUnitSelection(){
        if(Core.settings.getBool("selectionringabove", true)) return;
        if(commandRect && commandMode){
            float rotation = Time.time * 360f / (60f * 4f);
            if(useScreenRectSelection()){
                Draw.draw(selectionRingLayer(), () -> {
                    var units = selectedCommandUnitsScreen(commandRectScreenX, commandRectScreenY, getMouseX(), getMouseY());
                    var buildings = selectedCommandBuildingsScreenRaw(commandRectScreenX, commandRectScreenY, getMouseX(), getMouseY());
                    for(var unit : units){
                        float radius = Math.max(1f, unit.hitSize / 2f + selectionRotatingDashedRadiusOffset);
                        OverlayRenderer.drawHoverArcRing(unit.x, unit.y, radius, rotation, Color.green);
                    }
                    for(var build : buildings){
                        float radius = Math.max(1f, build.hitSize() / 2f + selectionRotatingDashedRadiusOffset);
                        OverlayRenderer.drawHoverArcRing(build.x, build.y, radius, rotation, Color.green);
                    }
                });
            }else{
                float x2 = mouseWorldX(), y2 = mouseWorldY();
                Draw.draw(selectionRingLayer(), () -> {
                    var units = selectedCommandUnits(commandRectX, commandRectY, x2 - commandRectX, y2 - commandRectY);
                    var buildings = selectedCommandBuildingsRaw(commandRectX, commandRectY, x2 - commandRectX, y2 - commandRectY);
                    for(var unit : units){
                        float radius = Math.max(1f, unit.hitSize / 2f + selectionRotatingDashedRadiusOffset);
                        OverlayRenderer.drawHoverArcRing(unit.x, unit.y, radius, rotation, Color.green);
                    }
                    for(var build : buildings){
                        float radius = Math.max(1f, build.hitSize() / 2f + selectionRotatingDashedRadiusOffset);
                        OverlayRenderer.drawHoverArcRing(build.x, build.y, radius, rotation, Color.green);
                    }
                });
            }
        }

        if(commandMode && !commandRect){
            //no hover selection ring
        }
    }

    public void drawUnitSelectionTop(){
        if(!Core.settings.getBool("selectionringabove", true)) return;
        if(commandRect && commandMode){
            float rotation = Time.time * 360f / (60f * 4f);
            if(useScreenRectSelection()){
                Draw.z(selectionRingLayer());
                var units = selectedCommandUnitsScreen(commandRectScreenX, commandRectScreenY, getMouseX(), getMouseY());
                var buildings = selectedCommandBuildingsScreenRaw(commandRectScreenX, commandRectScreenY, getMouseX(), getMouseY());
                for(var unit : units){
                    float radius = Math.max(1f, unit.hitSize / 2f + selectionRotatingDashedRadiusOffset);
                    OverlayRenderer.drawHoverArcRing(unit.x, unit.y, radius, rotation, Color.green);
                }
                for(var build : buildings){
                    float radius = Math.max(1f, build.hitSize() / 2f + selectionRotatingDashedRadiusOffset);
                    OverlayRenderer.drawHoverArcRing(build.x, build.y, radius, rotation, Color.green);
                }
                Draw.reset();
            }else{
                float x2 = mouseWorldX(), y2 = mouseWorldY();
                Draw.z(selectionRingLayer());
                var units = selectedCommandUnits(commandRectX, commandRectY, x2 - commandRectX, y2 - commandRectY);
                var buildings = selectedCommandBuildingsRaw(commandRectX, commandRectY, x2 - commandRectX, y2 - commandRectY);
                for(var unit : units){
                    float radius = Math.max(1f, unit.hitSize / 2f + selectionRotatingDashedRadiusOffset);
                    OverlayRenderer.drawHoverArcRing(unit.x, unit.y, radius, rotation, Color.green);
                }
                for(var build : buildings){
                    float radius = Math.max(1f, build.hitSize() / 2f + selectionRotatingDashedRadiusOffset);
                    OverlayRenderer.drawHoverArcRing(build.x, build.y, radius, rotation, Color.green);
                }
                Draw.reset();
            }
        }
    }

    public void drawBottom(){

    }

    public void drawTop(){
        if(ui.hudfrag == null || ui.hudfrag.abilityPanel == null) return;
        var panel = ui.hudfrag.abilityPanel;
        if(panel.activeCommand != mindustry.ui.UnitAbilityPanel.CommandMode.BUILD_PLACE
        && panel.activeCommand != mindustry.ui.UnitAbilityPanel.CommandMode.RAVEN_TURRET){
            return;
        }

        Block block = panel.activeCommand == mindustry.ui.UnitAbilityPanel.CommandMode.RAVEN_TURRET ? Blocks.ravenTurret : panel.getPlacingBlock();
        if(block == null) return;

        float worldX = mouseWorldX();
        float worldY = mouseWorldY();
        int tx;
        int ty;

        if(block == Blocks.ventCondenser){
            Tile snap = findNearestVentCenter(worldX, worldY, 30);
            if(snap != null){
                tx = snap.x;
                ty = snap.y;
            }else{
                Tmp.v1.set(worldX, worldY).sub(block.offset, block.offset);
                tx = World.toTile(Tmp.v1.x);
                ty = World.toTile(Tmp.v1.y);
            }
        }else{
            Tmp.v1.set(worldX, worldY).sub(block.offset, block.offset);
            tx = World.toTile(Tmp.v1.x);
            ty = World.toTile(Tmp.v1.y);
        }

        int placeRotation = 0;
        bplan.set(tx, ty, placeRotation, block);
        if(block.saveConfig){
            bplan.config = block.lastConfig;
        }
        bplan.animScale = 1f;
        boolean valid = panel.activeCommand == mindustry.ui.UnitAbilityPanel.CommandMode.RAVEN_TURRET
            ? Build.validPlaceIgnoreUnits(block, player.team(), tx, ty, placeRotation, false, false) && Build.checkNoUnitOverlap(block, tx, ty)
            : validPlace(tx, ty, block, placeRotation, null, true);
        block.drawPlan(bplan, allPlans(), valid, 0.5f);
        if(panel.activeCommand == mindustry.ui.UnitAbilityPanel.CommandMode.BUILD_PLACE){
            drawPlacementConstraintGrid(block, player.team(), tx, ty, placeRotation);
        }
    }

    public void drawOverSelect(){

    }

    public void drawSelected(int x, int y, Block block, Color color){
        Drawf.selected(x, y, block, color);
    }

    public void drawBreaking(BuildPlan plan){
        if(plan.breaking){
            drawBreaking(plan.x, plan.y);
        }else{
            drawSelected(plan.x, plan.y, plan.block, Pal.remove);
        }
    }

    public void drawOverlapCheck(Block block, int cursorX, int cursorY, boolean valid){
        if(!valid && state.rules.placeRangeCheck){
            var blocker = Build.getEnemyOverlap(block, player.team(), cursorX, cursorY);
            if(blocker != null && blocker.wasVisible){
                Drawf.selected(blocker, Pal.remove);
                Tmp.v1.set(cursorX, cursorY).scl(tilesize).add(block.offset, block.offset).sub(blocker).scl(-1f).nor();
                Drawf.dashLineDst(Pal.remove,
                cursorX * tilesize + block.offset + Tmp.v1.x * block.size * tilesize/2f,
                cursorY * tilesize + block.offset + Tmp.v1.y * block.size * tilesize/2f,
                blocker.x + Tmp.v1.x * -blocker.block.size * tilesize/2f,
                blocker.y + Tmp.v1.y * -blocker.block.size * tilesize/2f
                );
            }
        }
    }

    public boolean planMatches(BuildPlan plan){
        Tile tile = world.tile(plan.x, plan.y);
        if(tile == null || !(tile.build instanceof ConstructBuild)) return false;
        return ((ConstructBuild)tile.build).current == plan.block;
    }

    public void drawBreaking(int x, int y){
        Tile tile = world.tile(x, y);
        if(tile == null) return;
        Block block = tile.block();

        drawSelected(x, y, block, Pal.remove);
    }

    public void useSchematic(Schematic schem){
        useSchematic(schem, true);
    }

    public abstract void useSchematic(Schematic schem, boolean checkHidden);

    protected void showSchematicSave(){
        if(lastSchematic == null) return;

        var last = lastSchematic;

        ui.showTextInput("@schematic.add", "@name", 1000, "", text -> {
            Schematic replacement = schematics.all().find(s -> s.name().equals(text));
            if(replacement != null){
                ui.showConfirm("@confirm", "@schematic.replace", () -> {
                    schematics.overwrite(replacement, last);
                    ui.showInfoFade("@schematic.saved");
                    ui.schematics.showInfo(replacement);
                });
            }else{
                last.tags.put("name", text);
                last.tags.put("description", "");
                schematics.add(last);
                ui.showInfoFade("@schematic.saved");
                ui.schematics.showInfo(last);
                Events.fire(new SchematicCreateEvent(last));
            }
        });
    }

    public void rotatePlans(Seq<BuildPlan> plans, int direction){
        int ox = schemOriginX(), oy = schemOriginY();

        plans.each(plan -> {
            if(plan.breaking) return;

            float off = plan.block.size % 2 == 0 ? -0.5f : 0f;

            plan.pointConfig(p -> {
                float cx = p.x + off, cy = p.y + off;
                float lx = cx;

                if(direction >= 0){
                    cx = -cy;
                    cy = lx;
                }else{
                    cx = cy;
                    cy = -lx;
                }
                p.set(Mathf.floor(cx - off), Mathf.floor(cy - off));
            });

            //rotate actual plan, centered on its multiblock position
            float wx = (plan.x - ox) * tilesize + plan.block.offset, wy = (plan.y - oy) * tilesize + plan.block.offset;
            float x = wx;
            if(direction >= 0){
                wx = -wy;
                wy = x;
            }else{
                wx = wy;
                wy = -x;
            }
            plan.x = World.toTile(wx - plan.block.offset) + ox;
            plan.y = World.toTile(wy - plan.block.offset) + oy;
            plan.rotation = plan.block.planRotation(Mathf.mod(plan.rotation + direction, 4));
        });
    }

    public void flipPlans(Seq<BuildPlan> plans, boolean x){
        int origin = (x ? schemOriginX() : schemOriginY()) * tilesize;

        plans.each(plan -> {
            if(plan.breaking) return;

            float value = -((x ? plan.x : plan.y) * tilesize - origin + plan.block.offset) + origin;

            if(x){
                plan.x = (int)((value - plan.block.offset) / tilesize);
            }else{
                plan.y = (int)((value - plan.block.offset) / tilesize);
            }

            plan.pointConfig(p -> {
                if(x){
                    if(plan.block.size % 2 == 0) p.x --;
                    p.x = -p.x;
                }else{
                    if(plan.block.size % 2 == 0) p.y --;
                    p.y = -p.y;
                }
            });

            //flip rotation
            plan.block.flipRotation(plan, x);
        });
    }

    protected int schemOriginX(){
        return rawTileX();
    }

    protected int schemOriginY(){
        return rawTileY();
    }

    /** @return the selection plan that overlaps this position, or null. */
    protected @Nullable BuildPlan getPlan(int x, int y){
        return getPlan(x, y, 1, null);
    }

    /** Returns the selection plan that overlaps this position, or null. */
    protected @Nullable BuildPlan getPlan(int x, int y, int size, BuildPlan skip){
        float offset = ((size + 1) % 2) * tilesize / 2f;
        r2.setSize(tilesize * size);
        r2.setCenter(x * tilesize + offset, y * tilesize + offset);
        resultplan = null;

        Boolf<BuildPlan> test = plan -> {
            if(plan == skip) return false;
            Tile other = plan.tile();

            if(other == null) return false;

            if(!plan.breaking){
                r1.setSize(plan.block.size * tilesize);
                r1.setCenter(other.worldx() + plan.block.offset, other.worldy() + plan.block.offset);
            }else{
                r1.setSize(other.block().size * tilesize);
                r1.setCenter(other.worldx() + other.block().offset, other.worldy() + other.block().offset);
            }

            return r2.overlaps(r1);
        };

        if(!player.dead()){
            for(var plan : player.unit().plans()){
                if(test.get(plan)) return plan;
            }
        }

        return selectPlans.find(test);
    }

    protected void drawBreakSelection(int x1, int y1, int x2, int y2, int maxLength){
        drawBreakSelection(x1, y1, x2, y2, maxLength, true);
    }

    protected void drawBreakSelection(int x1, int y1, int x2, int y2, int maxLength, boolean useSelectPlans){
        NormalizeDrawResult result = Placement.normalizeDrawArea(Blocks.air, x1, y1, x2, y2, false, maxLength, 1f);
        NormalizeResult dresult = Placement.normalizeArea(x1, y1, x2, y2, rotation, false, maxLength);

        for(int x = dresult.x; x <= dresult.x2; x++){
            for(int y = dresult.y; y <= dresult.y2; y++){
                Tile tile = world.tileBuilding(x, y);
                if(tile == null || !validBreak(tile.x, tile.y)) continue;

                drawBreaking(tile.x, tile.y);
            }
        }

        Tmp.r1.set(result.x, result.y, result.x2 - result.x, result.y2 - result.y);

        Draw.color(Pal.remove);
        Lines.stroke(1f);

        if(!player.dead()){
            for(var plan : player.unit().plans()){
                if(!plan.breaking && plan.bounds(Tmp.r2).overlaps(Tmp.r1)){
                    drawBreaking(plan);
                }
            }
        }

        if(useSelectPlans){
            for(var plan : selectPlans){
                if(!plan.breaking && plan.bounds(Tmp.r2).overlaps(Tmp.r1)){
                    drawBreaking(plan);
                }
            }
        }

        for(BlockPlan plan : player.team().data().plans){
            Block block = plan.block;
            if(block.bounds(plan.x, plan.y, Tmp.r2).overlaps(Tmp.r1)){
                drawSelected(plan.x, plan.y, plan.block, Pal.remove);
            }
        }

        Lines.stroke(2f);

        Draw.color(Pal.removeBack);
        Lines.rect(result.x, result.y - 1, result.x2 - result.x, result.y2 - result.y);
        Draw.color(Pal.remove);
        Lines.rect(result.x, result.y, result.x2 - result.x, result.y2 - result.y);
    }

    protected void drawRebuildSelection(int x1, int y1, int x2, int y2){
        drawSelection(x1, y1, x2, y2, 0, Pal.sapBulletBack, Pal.sapBullet, false);

        NormalizeDrawResult result = Placement.normalizeDrawArea(Blocks.air, x1, y1, x2, y2, false, 0, 1f);

        Tmp.r1.set(result.x, result.y, result.x2 - result.x, result.y2 - result.y);

        for(BlockPlan plan : player.team().data().plans){
            Block block = plan.block;
            if(block.bounds(plan.x, plan.y, Tmp.r2).overlaps(Tmp.r1)){
                drawSelected(plan.x, plan.y, plan.block, Pal.sapBullet);
            }
        }

        NormalizeResult dresult = Placement.normalizeArea(x1, y1, x2, y2, rotation, false, 999999999);

        intSet.clear();
        for(int x = dresult.x; x <= dresult.x2; x++){
            for(int y = dresult.y; y <= dresult.y2; y++){

                Tile tile = world.tileBuilding(x, y);

                if(tile != null && intSet.add(tile.pos()) && canRepairDerelict(tile)){
                    drawSelected(tile.x, tile.y, tile.block(), Pal.sapBullet);
                }
            }
        }
    }

    protected void drawBreakSelection(int x1, int y1, int x2, int y2){
        drawBreakSelection(x1, y1, x2, y2, maxLength);
    }

    protected void drawSelection(int x1, int y1, int x2, int y2, int maxLength){
        drawSelection(x1, y1, x2, y2, maxLength, Pal.accentBack, Pal.accent, true);
    }

    protected void drawSelection(int x1, int y1, int x2, int y2, int maxLength, Color col1, Color col2, boolean withText){
        NormalizeDrawResult result = Placement.normalizeDrawArea(Blocks.air, x1, y1, x2, y2, false, maxLength, 1f);

        Lines.stroke(2f);

        Draw.color(col1);
        Lines.rect(result.x, result.y - 1, result.x2 - result.x, result.y2 - result.y);
        Draw.color(col2);
        Lines.rect(result.x, result.y, result.x2 - result.x, result.y2 - result.y);

        if(withText){
            Font font = Fonts.outline;
            font.setColor(col2);
            var ints = font.usesIntegerPositions();
            font.setUseIntegerPositions(false);
            var z = Draw.z();
            Draw.z(Layer.endPixeled);
            font.getData().setScale(1 / renderer.camerascale);
            var snapToCursor = Core.settings.getBool("selectionsizeoncursor");
            var textOffset = Core.settings.getInt("selectionsizeoncursoroffset", 5);
            int width = (int)((result.x2 - result.x) / 8);
            int height = (int)((result.y2 - result.y) / 8);
            int area = width * height;

            // FINISHME: When not snapping to cursor, perhaps it would be best to choose the corner closest to the cursor that's at least a block away?
            font.draw(width + "x" + height + " (" + area + ")",
            snapToCursor ? mouseWorldX() + textOffset * (4 / renderer.camerascale) : result.x2,
            snapToCursor ? mouseWorldY() - textOffset * (4 / renderer.camerascale) : result.y
            );
            font.setColor(Color.white);
            font.getData().setScale(1);
            font.setUseIntegerPositions(ints);
            Draw.z(z);
        }
    }

    protected void flushSelectPlans(Seq<BuildPlan> plans){
        for(BuildPlan plan : plans){
            if(plan.block != null && validPlace(plan.x, plan.y, plan.block, plan.rotation, null, true)){
                BuildPlan other = getPlan(plan.x, plan.y, plan.block.size, null);
                if(other == null){
                    selectPlans.add(plan.copy());
                }else if(!other.breaking && other.x == plan.x && other.y == plan.y && other.block.size == plan.block.size){
                    selectPlans.remove(other);
                    selectPlans.add(plan.copy());
                }
            }
        }
    }

    protected void flushPlansReverse(Seq<BuildPlan> plans){
        //reversed iteration.
        for(int i = plans.size - 1; i >= 0; i--){
            var plan = plans.get(i);
            if(plan.block != null && validPlace(plan.x, plan.y, plan.block, plan.rotation, null, true)){
                BuildPlan copy = plan.copy();
                plan.block.onNewPlan(copy);
                player.unit().addBuild(copy, false);
            }
        }
    }

    protected void flushPlans(Seq<BuildPlan> plans){
        for(var plan : plans){
            if(plan.block != null && validPlace(plan.x, plan.y, plan.block, plan.rotation, null, true)){
                BuildPlan copy = plan.copy();
                plan.block.onNewPlan(copy);
                player.unit().addBuild(copy);
            }
        }
    }

    protected void drawOverPlan(BuildPlan plan){
        drawOverPlan(plan, validPlace(plan.x, plan.y, plan.block, plan.rotation));
    }

    protected void drawOverPlan(BuildPlan plan, boolean valid){
        drawOverPlan(plan, valid, 1f);
    }

    protected void drawOverPlan(BuildPlan plan, boolean valid, float alpha){
        Draw.reset();
        Draw.mixcol(!valid ? Pal.breakInvalid : Color.white, (!valid ? 0.4f : 0.24f) + Mathf.absin(Time.globalTime, 6f, 0.28f));
        Draw.alpha(alpha);
        plan.block.drawPlanConfigTop(plan, allSelectLines);
        Draw.reset();
    }

    protected void drawPlan(BuildPlan plan){
        drawPlan(plan, plan.cachedValid = validPlace(plan.x, plan.y, plan.block, plan.rotation));
    }

    protected void drawPlan(BuildPlan plan, boolean valid){
        plan.block.drawPlan(plan, allPlans(), valid);
    }

    /** Draws a placement icon for a specific block. */
    protected void drawPlan(int x, int y, Block block, int rotation){
        bplan.set(x, y, rotation, block);
        if(block.saveConfig){
            bplan.config = block.lastConfig;
        }
        bplan.animScale = 1f;
        block.drawPlan(bplan, allPlans(), validPlace(x, y, block, rotation));
    }

    /** Draws a placement icon for a specific block with a custom alpha. */
    protected void drawPlan(int x, int y, Block block, int rotation, float alpha){
        bplan.set(x, y, rotation, block);
        if(block.saveConfig){
            bplan.config = block.lastConfig;
        }
        bplan.animScale = 1f;
        block.drawPlan(bplan, allPlans(), validPlace(x, y, block, rotation), alpha);
    }

    /** Remove everything from the queue in a selection. */
    protected void removeSelection(int x1, int y1, int x2, int y2){
        removeSelection(x1, y1, x2, y2, false);
    }

    /** Remove everything from the queue in a selection. */
    protected void removeSelection(int x1, int y1, int x2, int y2, int maxLength){
        removeSelection(x1, y1, x2, y2, false, maxLength);
    }

    /** Remove everything from the queue in a selection. */
    protected void removeSelection(int x1, int y1, int x2, int y2, boolean flush){
        removeSelection(x1, y1, x2, y2, flush, maxLength);
    }

    /** Remove everything from the queue in a selection. */
    protected void removeSelection(int x1, int y1, int x2, int y2, boolean flush, int maxLength){
        NormalizeResult result = Placement.normalizeArea(x1, y1, x2, y2, rotation, false, maxLength);
        for(int x = 0; x <= Math.abs(result.x2 - result.x); x++){
            for(int y = 0; y <= Math.abs(result.y2 - result.y); y++){
                int wx = x1 + x * Mathf.sign(x2 - x1);
                int wy = y1 + y * Mathf.sign(y2 - y1);

                Tile tile = world.tileBuilding(wx, wy);

                if(tile == null) continue;

                if(!flush){
                    tryBreakBlock(wx, wy);
                }else if(validBreak(tile.x, tile.y) && !selectPlans.contains(r -> r.tile() != null && r.tile() == tile)){
                    selectPlans.add(new BuildPlan(tile.x, tile.y));
                }
            }
        }

        //remove build plans
        Tmp.r1.set(result.x * tilesize, result.y * tilesize, (result.x2 - result.x) * tilesize, (result.y2 - result.y) * tilesize);

        if(!player.dead()){
            Iterator<BuildPlan> it = player.unit().plans().iterator();
            while(it.hasNext()){
                var plan = it.next();
                if(!plan.breaking && plan.bounds(Tmp.r2).overlaps(Tmp.r1)){
                    it.remove();
                }
            }

            //don't remove plans on desktop, where flushing is false
            if(flush){
                it = selectPlans.iterator();
                while(it.hasNext()){
                    var plan = it.next();
                    if(!plan.breaking && plan.bounds(Tmp.r2).overlaps(Tmp.r1)){
                        it.remove();
                    }
                }
            }
        }

        removed.clear();

        //remove blocks to rebuild
        Iterator<BlockPlan> broken = player.team().data().plans.iterator();
        while(broken.hasNext()){
            BlockPlan plan = broken.next();
            Block block = plan.block;
            if(block.bounds(plan.x, plan.y, Tmp.r2).overlaps(Tmp.r1)){
                removed.add(Point2.pack(plan.x, plan.y));
                plan.removed = true;
                broken.remove();
            }
        }

        //TODO array may be too large?
        if(removed.size > 0 && net.active()){
            Call.deletePlans(player, removed.toArray());
        }
    }

    protected void updateLine(int x1, int y1, int x2, int y2){
        linePlans.clear();
        iterateLine(x1, y1, x2, y2, l -> {
            rotation = l.rotation;
            var plan = new BuildPlan(l.x, l.y, l.rotation, block, block.nextConfig());
            plan.animScale = 1f;
            linePlans.add(plan);
        });

        if(Core.settings.getBool("blockreplace")){
            linePlans.each(plan -> {
                Block replace = plan.block.getReplacement(plan, linePlans);
                if(replace.unlockedNow()){
                    plan.block = replace;
                }
            });

            block.handlePlacementLine(linePlans);
        }
    }

    protected void updateLine(int x1, int y1){
        updateLine(x1, y1, tileX(getMouseX(), getMouseY()), tileY(getMouseX(), getMouseY()));
    }

    boolean checkConfigTap(){
        return config.isShown() && config.getSelected().onConfigureTapped(mouseWorldX(), mouseWorldY());
    }

    /** Handles tile tap events that are not platform specific. */
    boolean tileTapped(@Nullable Building build){
        planConfig.hide();
        if(build == null){
            inv.hide();
            config.hideConfig();
            commandBuildings.clear();
            return false;
        }
        selectedResource = null;
        boolean consumed = false, showedInventory = false;

        //select building for commanding
        if(build.isCommandable() && commandMode){
            //TODO handled in tap.
            consumed = true;
        }else if(build.block.configurable && build.interactable(player.team())){ //check if tapped block is configurable
            consumed = true;
            if((!config.isShown() && build.shouldShowConfigure(player)) //if the config fragment is hidden, show
            //alternatively, the current selected block can 'agree' to switch config tiles
            || (config.isShown() && config.getSelected().onConfigureBuildTapped(build) && build.shouldShowConfigure(player))){
                build.block.configureSound.at(build);
                config.showConfig(build);
            }
            //otherwise...
        }else if(!config.hasConfigMouse()){ //make sure a configuration fragment isn't on the cursor
            //then, if it's shown and the current block 'agrees' to hide, hide it.
            if(config.isShown() && config.getSelected().onConfigureBuildTapped(build)){
                consumed = true;
                config.hideConfig();
            }

            if(config.isShown()){
                consumed = true;
            }
        }

        //call tapped event
        if(!consumed && build.interactable(player.team())){
            build.tapped();
        }

        //consume tap event if necessary
        if(build.interactable(player.team()) && build.block.consumesTap){
            consumed = true;
        }else if(build.interactable(player.team()) && build.block.synthetic() && (!consumed || build.block.allowConfigInventory)){
            if(build.block.hasItems && build.items.total() > 0){
                inv.showFor(build);
                consumed = true;
                showedInventory = true;
            }
        }

        if(!showedInventory){
            inv.hide();
        }

        return consumed;
    }

    public boolean trySelectResource(Tile tile){
        return trySelectResource(tile, false);
    }

    public boolean trySelectResource(Tile tile, boolean additive){
        Tile resolved = resolveResourceTile(tile);
        if(resolved == null) return false;

        if(!additive){
            selectedUnits.clear();
            commandBuildings.clear();
        }
        selectedResource = selectedResource == resolved ? null : resolved;
        return true;
    }

    public boolean isResourceTile(Tile tile){
        return resolveResourceTile(tile) != null;
    }

    public @Nullable Tile resolveResourceTile(Tile tile){
        if(tile == null) return null;
        if(tile.block() instanceof CrystalMineralWall) return tile;
        if(tile.floor() instanceof SteamVent){
            SteamVent vent = (SteamVent)tile.floor();
            Tile dataTile = vent.dataTile(tile);
            if(dataTile == null || !vent.checkAdjacent(dataTile)) return null;
            Tile center = dataTile.nearby(-1, -1);
            if(center != null && center.floor() == vent) return center;
            return dataTile;
        }
        return null;
    }

    protected @Nullable Tile findNearestVentCenter(float worldX, float worldY, int rangeTiles){
        int cx = World.toTile(worldX);
        int cy = World.toTile(worldY);
        float range = rangeTiles * tilesize;
        float bestDst2 = range * range;
        Tile best = null;

        for(int dx = -rangeTiles; dx <= rangeTiles; dx++){
            for(int dy = -rangeTiles; dy <= rangeTiles; dy++){
                Tile tile = world.tile(cx + dx, cy + dy);
                if(tile == null || !(tile.floor() instanceof SteamVent)) continue;
                SteamVent vent = (SteamVent)tile.floor();
                boolean validCenter = true;
                for(int vx = -1; vx <= 1 && validCenter; vx++){
                    for(int vy = -1; vy <= 1; vy++){
                        Tile other = world.tile(tile.x + vx, tile.y + vy);
                        if(other == null || other.floor() != vent){
                            validCenter = false;
                            break;
                        }
                    }
                }
                if(!validCenter) continue;
                Tile center = tile;
                if(hasVentCondenser(center)) continue;

                float dst2 = Mathf.dst2(worldX, worldY, center.worldx(), center.worldy());
                if(dst2 <= bestDst2){
                    bestDst2 = dst2;
                    best = center;
                }
            }
        }

        return best;
    }

    private boolean hasVentCondenser(Tile center){
        if(center == null) return false;
        Building build = center.build;
        if(build == null) return false;
        if(build.block == Blocks.ventCondenser) return true;
        if(build instanceof ConstructBuild cons && cons.current == Blocks.ventCondenser) return true;
        return false;
    }

    @Nullable Tile findVentTile(Building build){
        if(build == null || build.tile == null) return null;
        int size = build.block.size;
        int bx = build.tile.x;
        int by = build.tile.y;

        for(int x = 0; x < size; x++){
            for(int y = 0; y < size; y++){
                Tile tile = world.tile(bx + x, by + y);
                if(tile == null || !(tile.floor() instanceof SteamVent)) continue;
                SteamVent vent = (SteamVent)tile.floor();
                Tile data = vent.dataTile(tile);
                if(data != null && vent.checkAdjacent(data)){
                    Tile center = data.nearby(-1, -1);
                    if(center != null && center.floor() == vent) return center;
                    return data;
                }
            }
        }
        return null;
    }

    public @Nullable Tile findCrystalInRect(float x1, float y1, float x2, float y2){
        int tx1 = World.toTile(Math.min(x1, x2));
        int ty1 = World.toTile(Math.min(y1, y2));
        int tx2 = World.toTile(Math.max(x1, x2));
        int ty2 = World.toTile(Math.max(y1, y2));

        float mx = mouseWorldX();
        float my = mouseWorldY();
        Tile best = null;
        float bestDst = Float.MAX_VALUE;

        for(int x = tx1; x <= tx2; x++){
            for(int y = ty1; y <= ty2; y++){
                Tile tile = world.tile(x, y);
                Tile resource = resolveResourceTile(tile);
                if(resource == null) continue;
                if(!(resource.block() instanceof CrystalMineralWall) && !(resource.floor() instanceof SteamVent)) continue;

                float dst = Mathf.dst2(mx, my, resource.worldx(), resource.worldy());
                if(dst < bestDst){
                    bestDst = dst;
                    best = resource;
                }
            }
        }

        return best;
    }

    public @Nullable Tile findCrystalInScreenRect(float x1, float y1, float x2, float y2){
        if(renderer == null) return null;
        Rect screenRect = normalizeScreenRect(x1, y1, x2, y2, Tmp.r1);
        Rect worldRect = screenRectToWorldBounds(screenRect, Tmp.r2);
        int tx1 = World.toTile(worldRect.x);
        int ty1 = World.toTile(worldRect.y);
        int tx2 = World.toTile(worldRect.x + worldRect.width);
        int ty2 = World.toTile(worldRect.y + worldRect.height);

        float mx = mouseWorldX();
        float my = mouseWorldY();
        Tile best = null;
        float bestDst = Float.MAX_VALUE;

        for(int x = tx1; x <= tx2; x++){
            for(int y = ty1; y <= ty2; y++){
                Tile tile = world.tile(x, y);
                Tile resource = resolveResourceTile(tile);
                if(resource == null) continue;
                if(!(resource.block() instanceof CrystalMineralWall) && !(resource.floor() instanceof SteamVent)) continue;

                Vec2 sp = renderer.worldToScreen(resource.worldx(), resource.worldy(), Tmp.v1);
                if(!screenRect.contains(sp)) continue;

                float dst = Mathf.dst2(mx, my, resource.worldx(), resource.worldy());
                if(dst < bestDst){
                    bestDst = dst;
                    best = resource;
                }
            }
        }

        return best;
    }

    /** Tries to select the player to drop off items, returns true if successful. */
    boolean tryTapPlayer(float x, float y){
        if(canTapPlayer(x, y)){
            droppingItem = true;
            return true;
        }
        return false;
    }

    boolean canTapPlayer(float x, float y){
        return player.within(x, y, playerSelectRange) && !player.dead() && player.unit().stack.amount > 0 && block == null;
    }

    /** Tries to begin mining a tile, returns true if successful. */
    boolean tryBeginMine(Tile tile){
        if(!player.dead() && canMine(tile)){
            player.unit().mineTile = tile;
            return true;
        }
        return false;
    }

    /** Tries to stop mining, returns true if mining was stopped. */
    boolean tryStopMine(){
        if(!player.dead() && player.unit().mining()){
            player.unit().mineTile = null;
            return true;
        }
        return false;
    }

    boolean tryStopMine(Tile tile){
        if(!player.dead() && player.unit().mineTile == tile){
            player.unit().mineTile = null;
            return true;
        }
        return false;
    }

    boolean tryRepairDerelict(Tile selected){
        if(!player.dead() && selected != null && !state.rules.editor && player.team() != Team.derelict && selected.build != null && selected.build.block.unlockedNow() && selected.build.team == Team.derelict &&
            Build.validPlace(selected.block(), player.team(), selected.build.tileX(), selected.build.tileY(), selected.build.rotation)){

            player.unit().addBuild(new BuildPlan(selected.build.tileX(), selected.build.tileY(), selected.build.rotation, selected.block(), selected.build.config()));
            return true;
        }
        return false;
    }

    boolean canRepairDerelict(Tile tile){
        return tile != null && tile.build != null && !player.dead() && !state.rules.editor && player.team() != Team.derelict && tile.build.team == Team.derelict && tile.build.block.unlockedNowHost() &&
            Build.validPlace(tile.block(), player.team(), tile.build.tileX(), tile.build.tileY(), tile.build.rotation);
    }

    boolean canMine(Tile tile){
        return !Core.scene.hasMouse()
        && !player.dead()
        && player.unit().validMine(tile)
        && player.unit().acceptsItem(player.unit().getMineResult(tile))
        && !((!Core.settings.getBool("doubletapmine") && tile.floor().playerUnmineable) && tile.overlay().itemDrop == null)
        && !((!Core.settings.getBool("doubletapmine") && tile.overlay().playerUnmineable) && tile.overlay().itemDrop != null);
    }

    /** Returns the tile at the specified MOUSE coordinates. */
    Tile tileAt(float x, float y){
        Vec2 vec = mouseWorld(x, y);
        if(selectedBlock()){
            vec.sub(block.offset, block.offset);
        }
        return world.tile(World.toTile(vec.x), World.toTile(vec.y));
    }

    int rawTileX(){
        return World.toTile(mouseWorld().x);
    }

    int rawTileY(){
        return World.toTile(mouseWorld().y);
    }

    int tileX(float cursorX){
        return tileX(cursorX, getMouseY());
    }

    int tileY(float cursorY){
        return tileY(getMouseX(), cursorY);
    }

    int tileX(float cursorX, float cursorY){
        Vec2 vec = mouseWorld(cursorX, cursorY);
        if(selectedBlock()){
            vec.sub(block.offset, block.offset);
        }
        return World.toTile(vec.x);
    }

    int tileY(float cursorX, float cursorY){
        Vec2 vec = mouseWorld(cursorX, cursorY);
        if(selectedBlock()){
            vec.sub(block.offset, block.offset);
        }
        return World.toTile(vec.y);
    }

    /** Forces the camera to a position and enables panning on desktop. */
    public void panCamera(Vec2 position){
        if(!locked()){
            camera.position.set(position);
            if(world.width() > 0 && world.height() > 0){
                float half = tilesize / 2f;
                float maxX = Math.max(world.unitWidth() - half, half);
                float maxY = Math.max(world.unitHeight() - half, half);
                float screenHeight = Core.graphics.getHeight();
                float centerOffsetY = screenHeight <= 0f ? 0f : (renderer.getUiBottomInsetPx() / 2f) * (camera.height / screenHeight);
                camera.position.x = Mathf.clamp(camera.position.x, half, maxX);
                camera.position.y = Mathf.clamp(camera.position.y + centerOffsetY, half, maxY) - centerOffsetY;
            }
        }
    }

    public boolean selectedBlock(){
        return isPlacing();
    }

    public boolean isPlacing(){
        return block != null;
    }

    public boolean isBreaking(){
        return false;
    }

    public boolean isRebuildSelecting(){
        return input.keyDown(Binding.rebuildSelect);
    }

    public float mouseAngle(float x, float y){
        return mouseWorld(getMouseX(), getMouseY()).sub(x, y).angle();
    }

    public @Nullable Unit selectedUnit(){
        Unit unit = Units.closest(player.team(), mouseWorld().x, mouseWorld().y, 40f, u -> u.isAI() && u.playerControllable());
        if(unit != null){
            unit.hitbox(Tmp.r1);
            Tmp.r1.grow(6f);
            if(Tmp.r1.contains(mouseWorld())){
                return unit;
            }
        }

        Building build = buildAt(mouseWorld().x, mouseWorld().y);
        if(build instanceof ControlBlock){
            ControlBlock cont = (ControlBlock)build;
            if(cont.canControl() && build.team == player.team() && cont.unit() != player.unit() && cont.unit().isAI()){
                return cont.unit();
            }
        }

        return null;
    }

    public @Nullable Building selectedControlBuild(){
        //Check if there's a unit first - if so, don't select buildings
        Unit unit = Units.closest(player.team(), mouseWorld().x, mouseWorld().y, 40f, u -> u.isAI() && u.playerControllable());
        if(unit != null){
            unit.hitbox(Tmp.r1);
            Tmp.r1.grow(6f);
            if(Tmp.r1.contains(mouseWorld())){
                return null; //Unit present, don't select building
            }
        }

        Building build = buildAt(mouseWorld().x, mouseWorld().y);
        if(build != null && !player.dead() && build.canControlSelect(player.unit()) && build.team == player.team()){
            return build;
        }
        return null;
    }

    protected @Nullable Building buildAt(float x, float y){
        Building build = world.buildWorld(x, y);
        if(build == null) return null;
        return build.within(x, y, build.hitSize() / 2f) ? build : null;
    }

    public HoverInfo updateHover(){
        return updateHover(true);
    }

    public HoverInfo updateHover(boolean checkUI){
        hover.clear();

        if(checkUI && Core.scene != null && Core.scene.hasMouse()) return hover;

        float x = mouseWorldX();
        float y = mouseWorldY();

        Unit bestUnit = null;
        float bestDst = Float.MAX_VALUE;

        for(Unit u : Groups.unit){
            if(u == null || !u.isValid()) continue;
            if(u.inFogTo(player.team())) continue;
            float rad = u.hitSize / 2f + 4f;
            float dst2 = u.dst2(x, y);
            if(dst2 <= rad * rad && dst2 < bestDst){
                bestDst = dst2;
                bestUnit = u;
            }
        }

        if(bestUnit != null){
            hover.unit = bestUnit;
            hover.team = bestUnit.team();
            hover.x = bestUnit.x;
            hover.y = bestUnit.y;
            hover.radius = bestUnit.hitSize / 2f;
            return hover;
        }

        Building build = buildAt(x, y);
        if(build != null && build.block == Blocks.ventSpout){
            build = null;
        }
        if(build != null && !build.inFogTo(player.team())){
            hover.build = build;
            hover.team = build.team;
            hover.x = build.x;
            hover.y = build.y;
            hover.radius = build.block.size * tilesize / 2f;
            return hover;
        }

        Tile tile = resolveResourceTile(world.tileWorld(x, y));
        if(tile != null && (!state.rules.fog || fogControl.isVisibleTile(player.team(), tile.x, tile.y))){
            hover.resource = tile;
            hover.team = Team.derelict;
            hover.x = tile.worldx();
            hover.y = tile.worldy();
            if(tile.floor() instanceof SteamVent vent){
                Tile dataTile = vent.dataTile(tile);
                hover.radius = dataTile != null && vent.checkAdjacent(dataTile) ? tilesize * 1.5f : tilesize / 2f;
            }else{
                hover.radius = tilesize / 2f;
            }
        }

        return hover;
    }

    public static class HoverInfo{
        public @Nullable Unit unit;
        public @Nullable Building build;
        public @Nullable Tile resource;
        public @Nullable Team team;
        public float x, y, radius;

        public void clear(){
            unit = null;
            build = null;
            resource = null;
            team = null;
            x = 0f;
            y = 0f;
            radius = 0f;
        }

        public boolean isValid(){
            return unit != null || build != null || resource != null;
        }
    }

    protected void drawPlacementConstraintGrid(Block block, Team team, int centerTileX, int centerTileY, int rotation){
        if(block == null || team == null) return;

        float radiusTiles = block.size / 2f + 3f;
        int range = Mathf.ceil(radiusTiles);
        float centerX = centerTileX * tilesize + block.offset;
        float centerY = centerTileY * tilesize + block.offset;
        float radiusWorld = radiusTiles * tilesize;
        float radiusWorld2 = radiusWorld * radiusWorld;
        float half = tilesize / 2f;
        boolean coreLike = block instanceof CoreBlock;

        Draw.z(Layer.overlayUI + 1.2f);
        Lines.stroke(0.6f);

        for(int dx = -range; dx <= range; dx++){
            for(int dy = -range; dy <= range; dy++){
                int tx = centerTileX + dx;
                int ty = centerTileY + dy;
                Tile tile = world.tile(tx, ty);
                if(tile == null) continue;

                float wx = tile.worldx();
                float wy = tile.worldy();
                if(Mathf.dst2(centerX, centerY, wx, wy) > radiusWorld2) continue;

                boolean markInvalid;
                if(coreLike){
                    markInvalid = CoreBlock.inResourceExclusion(wx + half, wy + half) || Build.hasSlopeInPlacementArea(block, tx, ty);
                }else{
                    markInvalid = !Build.validPlace(block, team, tx, ty, rotation, false);
                }

                if(markInvalid){
                    Draw.color(placementGridInvalid, 0.25f);
                    Fill.crect(wx - half, wy - half, tilesize, tilesize);
                }

                Draw.color(placementGridLine, 0.16f);
                Lines.rect(wx - half, wy - half, tilesize, tilesize);
            }
        }

        Draw.reset();
    }

    public @Nullable Unit selectedCommandUnit(float x, float y){
        var tree = player.team().data().tree();
        tmpUnits.clear();
        float rad = 4f;
        tree.intersect(x - rad/2f, y - rad/2f, rad, rad, tmpUnits);
        return tmpUnits.min(u -> u.isCommandable(), u -> u.dst(x, y) - u.hitSize/2f);
    }

    public @Nullable Unit selectedEnemyUnit(float x, float y){
        tmpUnits.clear();
        float rad = 4f;

        Seq<TeamData> data = state.teams.present;
        for(int i = 0; i < data.size; i++){
            if(data.items[i].team != player.team()){
                data.items[i].tree().intersect(x - rad / 2f, y - rad / 2f, rad, rad, tmpUnits);
            }
        }

        return tmpUnits.min(u -> !u.inFogTo(player.team()), u -> u.dst(x, y) - u.hitSize/2f);
    }

    public @Nullable Unit selectedAnyUnit(float x, float y){
        tmpUnits.clear();
        float rad = 4f;

        Seq<TeamData> data = state.teams.present;
        for(int i = 0; i < data.size; i++){
            data.items[i].tree().intersect(x - rad / 2f, y - rad / 2f, rad, rad, tmpUnits);
        }

        return tmpUnits.min(u -> !u.inFogTo(player.team()), u -> u.dst(x, y) - u.hitSize/2f);
    }

    public Seq<Building> selectedCommandBuildings(float x, float y, float w, float h){
        var tree = player.team().data().buildingTree;
        tmpBuildings.clear();
        if(tree == null) return tmpBuildings;
        float rad = 4f;
        Rect rect = Tmp.r1.set(x - rad/2f, y - rad/2f, rad*2f + w, rad*2f + h).normalize();
        tree.intersect(rect, b -> {
            //Allow all buildings to be selected for display, but only commandable ones can be controlled
            float radius = b.hitSize() / 2f;
            if(overlapsCircleRect(b.x, b.y, radius, rect)){
                tmpBuildings.add(b);
            }
        });
        applyBuildingSelectionPriority(tmpBuildings);
        return tmpBuildings;
    }

    public Seq<Building> selectedCommandBuildingsRaw(float x, float y, float w, float h){
        var tree = player.team().data().buildingTree;
        tmpBuildings.clear();
        if(tree == null) return tmpBuildings;
        float rad = 4f;
        Rect rect = Tmp.r1.set(x - rad/2f, y - rad/2f, rad*2f + w, rad*2f + h).normalize();
        tree.intersect(rect, b -> {
            float radius = b.hitSize() / 2f;
            if(overlapsCircleRect(b.x, b.y, radius, rect)){
                tmpBuildings.add(b);
            }
        });
        return tmpBuildings;
    }

    private void applyBuildingSelectionPriority(Seq<Building> buildings){
        if(buildings.isEmpty()) return;
        int best = Integer.MIN_VALUE;
        for(Building build : buildings){
            int priority = buildingSelectionPriority(build);
            if(priority > best) best = priority;
        }
        if(best <= 0) return;
        final int bestPriority = best;
        buildings.removeAll(build -> buildingSelectionPriority(build) != bestPriority);
    }

    private int buildingSelectionPriority(@Nullable Building build){
        if(build == null || build.block == null) return 0;
        Block block = build.block;

        if(block == Blocks.coreOrbital) return 190;
        if(block == Blocks.coreNucleus) return 180;
        if(block == Blocks.corePlanetaryFortress) return 170;

        if(block == Blocks.doorLarge || block == Blocks.doorLargeErekir) return 160;
        if(block == Blocks.groundFactory) return 150;
        if(block == Blocks.tankFabricator) return 140;
        if(block == Blocks.shipFabricator) return 130;
        if(block == Blocks.multiPress) return 120;
        if(block == Blocks.siliconCrucible) return 110;
        if(block == Blocks.swarmer) return 100;
        if(build instanceof BunkerBlock.BunkerBuild) return 90;
        if(block == Blocks.radar) return 80;
        if(block == Blocks.launchPad) return 70;
        if(block == Blocks.surgeCrucible) return 60;

        if(block == Blocks.memoryBank){
            UnitFactory.UnitFactoryBuild factory = attachedFactoryForTechLab(build);
            if(factory != null){
                if(factory.block == Blocks.groundFactory) return 55;
                if(factory.block == Blocks.tankFabricator) return 54;
                if(factory.block == Blocks.shipFabricator) return 53;
            }
            return 1;
        }

        if(block == Blocks.ravenTurret) return 40;
        if(block == Blocks.rotaryPump) return 30;
        if(block == Blocks.ventCondenser) return 20;

        return 0;
    }

    private @Nullable UnitFactory.UnitFactoryBuild attachedFactoryForTechLab(@Nullable Building techLab){
        if(techLab == null || !techLab.isValid() || techLab.block != Blocks.memoryBank) return null;

        for(Building build : Groups.build){
            if(!(build instanceof UnitFactory.UnitFactoryBuild factory)) continue;
            if(!factory.isValid() || factory.team != techLab.team || !factory.hasTechAddon()) continue;

            int size = factory.block.size;
            int baseX = factory.tile.x - (size - 1) / 2;
            int baseY = factory.tile.y - (size - 1) / 2;
            Tile addonTile = world.tile(baseX + size, baseY);
            if(addonTile == null) continue;
            if(addonTile.build == techLab){
                return factory;
            }
        }

        return null;
    }

    private boolean overlapsCircleRect(float cx, float cy, float radius, Rect rect){
        float closestX = Mathf.clamp(cx, rect.x, rect.x + rect.width);
        float closestY = Mathf.clamp(cy, rect.y, rect.y + rect.height);
        float dx = cx - closestX;
        float dy = cy - closestY;
        return dx * dx + dy * dy <= radius * radius;
    }

    public Seq<Building> selectedCommandBuildingsScreen(float x1, float y1, float x2, float y2){
        var buildings = selectedCommandBuildingsScreenRaw(x1, y1, x2, y2);
        applyBuildingSelectionPriority(buildings);
        return buildings;
    }

    public Seq<Building> selectedCommandBuildingsScreenRaw(float x1, float y1, float x2, float y2){
        var tree = player.team().data().buildingTree;
        tmpBuildings.clear();
        if(tree == null || renderer == null) return tmpBuildings;
        Rect screenRect = normalizeScreenRect(x1, y1, x2, y2, Tmp.r1);
        Rect worldRect = screenRectToWorldBounds(screenRect, Tmp.r2);
        float screenScale = renderer.camerascale;
        tree.intersect(worldRect, b -> {
            float radius = b.hitSize() / 2f;
            Vec2 sp = renderer.worldToScreen(b.x, b.y, Tmp.v1);
            float sr = radius * screenScale;
            if(overlapsCircleRect(sp.x, sp.y, sr, screenRect)){
                tmpBuildings.add(b);
            }
        });
        return tmpBuildings;
    }

    public Seq<Unit> selectedCommandUnits(float x, float y, float w, float h, Boolf<Unit> predicate){
        var tree = player.team().data().tree();
        tmpUnits.clear();
        float rad = 4f;
        tree.intersect(Tmp.r1.set(x - rad/2f, y - rad/2f, rad*2f + w, rad*2f + h).normalize(), tmpUnits);
        tmpUnits.removeAll(u -> !u.isCommandable() || !predicate.get(u));
        return tmpUnits;
    }

    public Seq<Unit> selectedCommandUnitsScreen(float x1, float y1, float x2, float y2, Boolf<Unit> predicate){
        var tree = player.team().data().tree();
        tmpUnits.clear();
        if(renderer == null) return tmpUnits;
        Rect screenRect = normalizeScreenRect(x1, y1, x2, y2, Tmp.r1);
        Rect worldRect = screenRectToWorldBounds(screenRect, Tmp.r2);
        tree.intersect(worldRect, tmpUnits);
        float screenScale = renderer.camerascale;
        tmpUnits.removeAll(u -> {
            if(!u.isCommandable() || !predicate.get(u)) return true;
            Vec2 sp = renderer.worldToScreen(u.x, u.y, Tmp.v1);
            float sr = Math.max(1f, u.hitSize / 2f) * screenScale;
            return !overlapsCircleRect(sp.x, sp.y, sr, screenRect);
        });
        return tmpUnits;
    }

    public Seq<Unit> selectedCommandUnitsScreen(float x1, float y1, float x2, float y2){
        return selectedCommandUnitsScreen(x1, y1, x2, y2, u -> true);
    }

    public Seq<Unit> selectedCommandUnits(float x, float y, float w, float h){
        return selectedCommandUnits(x, y, w, h, u -> true);
    }

    public void remove(){
        Core.input.removeProcessor(this);
        group.remove();
        if(Core.scene != null){
            Table table = (Table)Core.scene.find("inputTable");
            if(table != null){
                table.clear();
            }
        }
        if(detector != null){
            Core.input.removeProcessor(detector);
        }
        if(uiGroup != null){
            uiGroup.remove();
            uiGroup = null;
        }
    }

    public void add(){
        Core.input.getInputProcessors().remove(i -> i instanceof InputHandler || (i instanceof GestureDetector && ((GestureDetector)i).getListener() instanceof InputHandler));
        Core.input.addProcessor(detector = new GestureDetector(20, 0.5f, 0.3f, 0.15f, this));
        Core.input.addProcessor(this);
        if(Core.scene != null){
            Table table = (Table)Core.scene.find("inputTable");
            if(table != null){
                table.clear();
                buildPlacementUI(table);
            }

            uiGroup = new WidgetGroup();
            uiGroup.touchable = Touchable.childrenOnly;
            uiGroup.setFillParent(true);
            ui.hudGroup.addChild(uiGroup);
            uiGroup.toBack();
            buildUI(uiGroup);

            group.setFillParent(true);
            Vars.ui.hudGroup.addChildBefore(Core.scene.find("overlaymarker"), group);

            inv.build(group);
            config.build(group);
            planConfig.build(group);
        }
    }

    public boolean canShoot(){
        return block == null && !onConfigurable() && !isDroppingItem() && !player.unit().activelyBuilding() &&
        !(player.unit() instanceof Mechc && player.unit().isFlying()) && !player.unit().mining() && !commandMode;
    }

    public boolean onConfigurable(){
        return false;
    }

    public boolean isDroppingItem(){
        return droppingItem;
    }

    public boolean canDropItem(){
        return droppingItem && !canTapPlayer(mouseWorldX(), mouseWorldY());
    }

    public void tryDropItems(@Nullable Building build, float x, float y){
        if(player.dead()) return;

        if(!droppingItem || player.unit().stack.amount <= 0 || canTapPlayer(x, y) || state.isPaused() ){
            droppingItem = false;
            return;
        }

        droppingItem = false;

        ItemStack stack = player.unit().stack;

        if(build != null && build.acceptStack(stack.item, stack.amount, player.unit()) > 0 && build.interactable(player.team()) &&
        build.block.hasItems && player.unit().stack().amount > 0 && build.interactable(player.team())){

            if(build.allowDeposit() && canDepositItem(build)){
                Call.transferInventory(player, build);
                itemDepositCooldown = state.rules.itemDepositCooldown;
            }
        }else{
            Call.dropItem(player.angleTo(x, y));
        }
    }

    public boolean canDepositItem(Building build){
        //takes advantage of itemDepositCooldown being able to be negative, allows the cooldown to be different for each building
        if(build.block.depositCooldown >= 0){
            return itemDepositCooldown - state.rules.itemDepositCooldown <= -build.block.depositCooldown;
        }
        return itemDepositCooldown <= 0;
    }

    public void rebuildArea(int x1, int y1, int x2, int y2){
        NormalizeResult result = Placement.normalizeArea(x1, y1, x2, y2, rotation, false, 999999999);
        Tmp.r1.set(result.x * tilesize, result.y * tilesize, (result.x2 - result.x) * tilesize, (result.y2 - result.y) * tilesize);

        Iterator<BlockPlan> broken = player.team().data().plans.iterator();
        while(broken.hasNext()){
            BlockPlan plan = broken.next();
            Block block = plan.block;
            if(block.bounds(plan.x, plan.y, Tmp.r2).overlaps(Tmp.r1)){
                player.unit().addBuild(new BuildPlan(plan.x, plan.y, plan.rotation, plan.block, plan.config));
            }
        }

        intSet.clear();
        for(int x = result.x; x <= result.x2; x++){
            for(int y = result.y; y <= result.y2; y++){

                Tile tile = world.tileBuilding(x, y);

                if(tile != null && tile.build != null && intSet.add(tile.pos())){
                    tryRepairDerelict(tile);
                }
            }
        }
    }

    public void tryBreakBlock(int x, int y){
        if(validBreak(x, y)){
            breakBlock(x, y);
        }
    }

    public boolean validPlace(int x, int y, Block type, int rotation){
        return validPlace(x, y, type, rotation, null);
    }
    public boolean validPlace(int x, int y, Block type, int rotation, @Nullable BuildPlan ignore){
        return validPlace(x, y, type, rotation, ignore, false);
    }

    public boolean validPlace(int x, int y, Block type, int rotation, @Nullable BuildPlan ignore, boolean ignoreUnits){
        if(player.isBuilder() && player.unit().plans.size > 0){
            Tmp.r1.setCentered(x * tilesize + type.offset, y * tilesize + type.offset, type.size * tilesize);
            plansOut.clear();
            playerPlanTree.intersect(Tmp.r1, plansOut);

            for(int i = 0; i < plansOut.size; i++){
                var plan = plansOut.items[i];
                if(plan != ignore
                && !plan.breaking
                && plan.block.bounds(plan.x, plan.y, Tmp.r1).overlaps(type.bounds(x, y, Tmp.r2))
                && !(type.canReplace(plan.block) && Tmp.r1.equals(Tmp.r2))){
                    return false;
                }
            }
        }

        return ignoreUnits ? Build.validPlaceIgnoreUnits(type, player.team(), x, y, rotation, true, true) : Build.validPlace(type, player.team(), x, y, rotation);
    }

    public boolean validBreak(int x, int y){
        return Build.validBreak(player.team(), x, y);
    }

    public void breakBlock(int x, int y){
        if(!player.isBuilder()) return;

        Tile tile = world.tile(x, y);
        if(tile != null && tile.build != null) tile = tile.build.tile;
        player.unit().addBuild(new BuildPlan(tile.x, tile.y));
    }

    public void drawArrow(Block block, int x, int y, int rotation){
        drawArrow(block, x, y, rotation, validPlace(x, y, block, rotation));
    }

    public void drawArrow(Block block, int x, int y, int rotation, boolean valid){
        float trns = (block.size / 2) * tilesize;
        int dx = Geometry.d4(rotation).x, dy = Geometry.d4(rotation).y;
        float offsetx = x * tilesize + block.offset + dx*trns;
        float offsety = y * tilesize + block.offset + dy*trns;

        Draw.color(!valid ? Pal.removeBack : Pal.accentBack);
        TextureRegion regionArrow = Core.atlas.find("place-arrow");

        Draw.rect(regionArrow,
        offsetx,
        offsety - 1,
        regionArrow.width * regionArrow.scl(),
        regionArrow.height * regionArrow.scl(),
        rotation * 90 - 90);

        Draw.color(!valid ? Pal.remove : Pal.accent);
        Draw.rect(regionArrow,
        offsetx,
        offsety,
        regionArrow.width * regionArrow.scl(),
        regionArrow.height * regionArrow.scl(),
        rotation * 90 - 90);
    }

    void iterateLine(int startX, int startY, int endX, int endY, Cons<PlaceLine> cons){
        Seq<Point2> points;
        boolean diagonal = Core.input.keyDown(Binding.diagonalPlacement);

        if(Core.settings.getBool("swapdiagonal") && mobile){
            diagonal = !diagonal;
        }

        if(block != null && block.swapDiagonalPlacement){
            diagonal = !diagonal;
        }

        int endRotation = -1;
        var start = world.build(startX, startY);
        var end = world.build(endX, endY);
        if(diagonal && (block == null || block.allowDiagonal)){
            if(block != null && start instanceof ChainedBuilding && end instanceof ChainedBuilding
            && block.canReplace(end.block) && block.canReplace(start.block)){
                points = Placement.upgradeLine(startX, startY, endX, endY);
            }else{
                points = Placement.pathfindLine(block != null && block.conveyorPlacement, startX, startY, endX, endY);
            }
        }else if(block != null && block.allowRectanglePlacement){
            points = Placement.normalizeRectangle(startX, startY, endX, endY, block.size);
        }else{
            points = Placement.normalizeLine(startX, startY, endX, endY);
        }
        if(points.size > 1 && end instanceof ChainedBuilding){
            Point2 secondToLast = points.get(points.size - 2);
            if(!(world.build(secondToLast.x, secondToLast.y) instanceof ChainedBuilding)){
                endRotation = end.rotation;
            }
        }

        if(block != null){
            block.changePlacementPath(points, rotation, diagonal);
        }

        float angle = Angles.angle(startX, startY, endX, endY);
        int baseRotation = rotation;
        if(!overrideLineRotation || diagonal){
            baseRotation = (startX == endX && startY == endY) ? rotation : ((int)((angle + 45) / 90f)) % 4;
        }

        Tmp.r3.set(-1, -1, 0, 0);

        for(int i = 0; i < points.size; i++){
            Point2 point = points.get(i);

            if(block != null && Tmp.r2.setSize(block.size * tilesize).setCenter(point.x * tilesize + block.offset, point.y * tilesize + block.offset).overlaps(Tmp.r3)){
                continue;
            }

            Point2 next = i == points.size - 1 ? null : points.get(i + 1);
            line.x = point.x;
            line.y = point.y;
            if((!overrideLineRotation || diagonal) && !(block != null && block.ignoreLineRotation && !mobile)){
                int result = baseRotation;
                if(next != null){
                    result = Tile.relativeTo(point.x, point.y, next.x, next.y);
                }else if(endRotation != -1){
                    result = endRotation;
                }else if(block.conveyorPlacement && i > 0){
                    Point2 prev = points.get(i - 1);
                    result = Tile.relativeTo(prev.x, prev.y, point.x, point.y);
                }
                if(result != -1){
                    line.rotation = result;
                }
            }else{
                line.rotation = rotation;
            }
            line.last = next == null;
            cons.get(line);

            Tmp.r3.setSize(block.size * tilesize).setCenter(point.x * tilesize + block.offset, point.y * tilesize + block.offset);
        }
    }

    static class PlaceLine{
        public int x, y, rotation;
        public boolean last;
    }
}
