package mindustry.ui;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.input.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.*;
import arc.scene.event.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Font;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.GlyphLayout;
import arc.graphics.g2d.Lines;
import mindustry.ai.*;
import mindustry.ai.types.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.game.EventType.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.input.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.ConstructBlock.*;
import mindustry.world.blocks.defense.*;
import mindustry.world.blocks.storage.*;
import mindustry.world.blocks.storage.CoreBlock.*;
import mindustry.world.blocks.units.*;

import static mindustry.Vars.*;

public class UnitAbilityPanel extends Table{
    private static final int COLS = 5;
    private static final int ROWS = 3;
    public static float abilityButtonSize = 64f;
    public static float abilityIconSize = 40f;
    public static float abilityKeyScale = 0.6f;
    public static final Color abilityBorderColor = Color.valueOf("2f5f2f");
    private static final float ABILITY_BUTTON_PAD = 2f;
    private static final float PANEL_MARGIN = 0f;

    //RTS command mode state
    public enum CommandMode{
        NONE,
        MOVE,
        STOP,
        HOLD,
        PATROL,
        ATTACK,
        REPAIR,
        HARVEST,
        RALLY,
        BUILD_PLACE,
        DROP_PULSAR,
        EXTRA_SUPPLY,
        SCAN,
        LAND,
        LIBERATOR_ZONE,
        MEDIVAC_HEAL,
        MEDIVAC_LOAD,
        MEDIVAC_UNLOAD,
        GHOST_TACTICAL_NUKE,
        GHOST_STABLE_AIM,
        GHOST_EMP,
        REAPER_KD8,
        BATTLECRUISER_YAMATO,
        BATTLECRUISER_WARP,
        RAVEN_ANTI_ARMOR,
        RAVEN_TURRET,
        RAVEN_MATRIX,
        BUNKER_ATTACK,
        BUNKER_LOAD
    }

    private enum NovaPanel{
        MAIN,
        BUILD_BASIC,
        BUILD_ADV
    }

    private enum CorePanel{
        MAIN,
        BUILD
    }

    private enum AutoCastSkill{
        hurricaneLock,
        medivacHeal
    }

    private static final int autoCastHurricaneLock = 1;
    private static final int autoCastMedivacHeal = 1 << 1;

    public CommandMode activeCommand = CommandMode.NONE;
    private NovaPanel novaPanel = NovaPanel.MAIN;
    private CorePanel corePanel = CorePanel.MAIN;
    private @Nullable Block placingBlock;
    private @Nullable BuildInfo hoverBuildInfo;
    private @Nullable AbilityInfo hoverAbilityInfo;
    private final GlyphLayout hoverInfoLayout = new GlyphLayout();
    private final AbilityInfo targetHintInfo = new AbilityInfo();
    private Table mainPanel;
    private Table commandModePanel;
    private float forcedMinWidth = -1f;
    private float forcedMinHeight = -1f;
    private final IntIntMap autoCastFlags = new IntIntMap();
    private float nextAutoCastUpdate = 0f;

    //Command definitions
    private static class RTSCommand{
        String name;
        String key;
        Drawable icon;
        String description;
        CommandMode mode;

        RTSCommand(String name, String key, Drawable icon, String description, CommandMode mode){
            this.name = name;
            this.key = key;
            this.icon = icon;
            this.description = description;
            this.mode = mode;
        }
    }

    private static class BuildInfo{
        Block block;
        UnitType unit;
        String key;
        String name;
        String description = "";
        String action = "";
        int crystalCost;
        int gasCost;
        int timeSeconds;
        int population = -1;
        @Nullable Floatp progress;
        @Nullable Boolp progressVisible;
        @Nullable Drawable progressIcon;
        @Nullable Color progressColor;
    }

    private static class AbilityInfo{
        String key = "";
        String name = "";
        String description = "";
        int crystalCost = -1;
        int gasCost = -1;
        int timeSeconds = -1;
        int population = -1;
        String action = "";
        @Nullable UnitType unit;
        @Nullable Block block;
        boolean hintOnly = false;
    }

    private RTSCommand[] commands = {
        new RTSCommand("Move", "m", Icon.move, "Commands the selected unit to move to a target area or follow a target unit. Units that are moving will not engage enemies.", CommandMode.MOVE),
        new RTSCommand("Stop", "s", Icon.cancel, "Commands the selected unit to stop executing any commands and halt movement.", CommandMode.STOP),
        new RTSCommand("Hold", "h", Icon.pause, "Commands the selected unit to stay in place and attack enemy targets within range. Units receiving this command will not chase enemies or move toward them to engage.", CommandMode.HOLD),
        new RTSCommand("Patrol", "p", Icon.refresh, "Commands the selected unit to patrol between its current position and a target area. Patrolling units will attack enemies or move toward nearby enemies to engage.", CommandMode.PATROL),
        new RTSCommand("Attack", "a", Icon.warning, "Commands the selected unit to move to a target location and attack enemies encountered along the way. After receiving an attack command on a target, the unit will continue attacking that target until it is destroyed.", CommandMode.ATTACK)
    };

    public UnitAbilityPanel(){
        background(Styles.black6);
        margin(0f);
        targetHintInfo.hintOnly = true;
        targetHintInfo.description = "Left-click select target\nRight-click return";

        mainPanel = new Table();
        commandModePanel = new Table();

        update(() -> {
            updateAutoCast();

            if(hasAbilityUnits() || hasAbilityBuildings()){
                boolean allowKeys = !Core.scene.hasKeyboard();
                boolean coreSelected = isOnlyCoreSelected();
                if(!coreSelected){
                    corePanel = CorePanel.MAIN;
                }else{
                    CoreBuild core = selectedCore();
                    if(core != null && corePanel == CorePanel.MAIN && core.unitQueue != null && !core.unitQueue.isEmpty()){
                        corePanel = CorePanel.BUILD;
                    }
                }

                if(allowKeys){
                    boolean allowRtsKeys = hasAbilityUnits();
                    if(isOnlyNovaSelected() && (novaPanel != NovaPanel.MAIN || activeCommand == CommandMode.BUILD_PLACE)){
                        allowRtsKeys = false;
                    }
                    if(allowRtsKeys){
                        boolean preceptTransition = isOnlySiegeTankSelected() && anyPreceptTransitioning();
                        boolean preceptSiegedLayout = isOnlySiegeTankSelected() && allSelectedPreceptSieged();
                        if(preceptTransition){
                            //No RTS action during mode transition.
                        }else if(preceptSiegedLayout){
                            if(Core.input.keyTap(Binding.rtsCommandStop)){
                                executeStopCommand();
                            }else if(Core.input.keyTap(Binding.rtsCommandAttack)){
                                enterCommandMode(CommandMode.ATTACK);
                            }
                        }else{
                            if(Core.input.keyTap(Binding.rtsCommandMove)){
                                enterCommandMode(CommandMode.MOVE);
                            }else if(Core.input.keyTap(Binding.rtsCommandStop)){
                                executeStopCommand();
                            }else if(Core.input.keyTap(Binding.rtsCommandHold)){
                                executeHoldCommand();
                            }else if(Core.input.keyTap(Binding.rtsCommandPatrol)){
                                enterCommandMode(CommandMode.PATROL);
                            }else if(Core.input.keyTap(Binding.rtsCommandAttack)){
                                enterCommandMode(CommandMode.ATTACK);
                            }
                        }
                    }

                    if(isOnlyNovaSelected()){
                        handleNovaHotkeys();
                    }else if(isOnlyWidowSelected()){
                        handleWidowHotkeys();
                    }else if(isOnlyMaceLocusSelected()){
                        handleMaceLocusHotkeys();
                    }else if(isOnlySiegeTankSelected()){
                        handlePreceptHotkeys();
                    }else if(isOnlyHurricaneSelected()){
                        handleHurricaneHotkeys();
                    }else if(isOnlyScepterSelected()){
                        handleScepterHotkeys();
                    }else if(isOnlyLiberatorSelected()){
                        handleLiberatorHotkeys();
                    }else if(isOnlyMedivacSelected()){
                        handleMedivacHotkeys();
                    }else if(isOnlyGhostSelected()){
                        handleGhostHotkeys();
                    }else if(isOnlyReaperSelected()){
                        handleReaperHotkeys();
                    }else if(isOnlyVikingSelected()){
                        handleVikingHotkeys();
                    }else if(isOnlyBattlecruiserSelected()){
                        handleBattlecruiserHotkeys();
                    }else if(isOnlyBansheeSelected()){
                        handleBansheeHotkeys();
                    }else if(isOnlyRavenSelected()){
                        handleRavenHotkeys();
                    }else if(isOnlyBarracksStimpackSelected()){
                        handleBarracksStimpackHotkeys();
                    }else if(isOnlyCoreFlyerSelected()){
                        handleCoreFlyerHotkeys();
                    }else if(!hasAbilityUnits() && hasAbilityBuildings()){
                        if(coreSelected){
                            var core = selectedCore();
                            if(core == null || !core.isUpgrading()){
                                handleCoreHotkeys();
                            }else if(Core.input.keyTap(KeyCode.escape)){
                                if(core.isUpgradingOrbital()){
                                    cancelCoreOrbitalUpgrade(core);
                                }else if(core.isUpgradingFortress()){
                                    cancelCoreFortressUpgrade(core);
                                }
                            }
                        }else{
                            handleBuildingHotkeys();
                        }
                    }
                }

                //Cancel command mode with Esc or right-click
                if(activeCommand != CommandMode.NONE){
                    if(Core.input.keyTap(KeyCode.escape) || Core.input.keyTap(KeyCode.mouseRight)){
                        exitCommandMode();
                    }
                }
            }else{
                novaPanel = NovaPanel.MAIN;
                corePanel = CorePanel.MAIN;
                placingBlock = null;
                activeCommand = CommandMode.NONE;
            }

            rebuild();
        });
    }

    private Seq<Unit> abilityUnits(){
        return control.input.abilitySubgroupUnits();
    }

    private boolean hasAbilityUnits(){
        return !abilityUnits().isEmpty();
    }

    private Seq<Building> abilityBuildings(){
        return control.input.abilitySubgroupBuildings();
    }

    private boolean hasAbilityBuildings(){
        return !abilityBuildings().isEmpty();
    }

    private void rebuild(){
        clearChildren();
        clearPanelSize();
        hoverAbilityInfo = null;

        if(!hasAbilityUnits() && !hasAbilityBuildings()){
            buildEmptyPanel();
            setPanelRows(ROWS);
            return;
        }

        if(!hasAbilityUnits() && hasAbilityBuildings()){
            buildBuildingPanel();
            return;
        }

        if(activeCommand != CommandMode.NONE && activeCommand != CommandMode.HARVEST && activeCommand != CommandMode.BUILD_PLACE
        && activeCommand != CommandMode.RALLY && activeCommand != CommandMode.DROP_PULSAR
        && activeCommand != CommandMode.EXTRA_SUPPLY && activeCommand != CommandMode.SCAN
        && activeCommand != CommandMode.LAND
        && activeCommand != CommandMode.LIBERATOR_ZONE
        && activeCommand != CommandMode.MEDIVAC_HEAL
        && activeCommand != CommandMode.MEDIVAC_LOAD
        && activeCommand != CommandMode.MEDIVAC_UNLOAD
        && activeCommand != CommandMode.GHOST_TACTICAL_NUKE
        && activeCommand != CommandMode.GHOST_STABLE_AIM
        && activeCommand != CommandMode.GHOST_EMP
        && activeCommand != CommandMode.REAPER_KD8
        && activeCommand != CommandMode.BATTLECRUISER_YAMATO
        && activeCommand != CommandMode.BATTLECRUISER_WARP
        && activeCommand != CommandMode.RAVEN_ANTI_ARMOR
        && activeCommand != CommandMode.RAVEN_TURRET
        && activeCommand != CommandMode.RAVEN_MATRIX
        && activeCommand != CommandMode.BUNKER_ATTACK
        && activeCommand != CommandMode.BUNKER_LOAD){
            buildCommandModePanel();
        }else{
            buildMainPanel();
        }
    }

    private void buildMainPanel(){
        if(isOnlyMedivacSelected()){
            buildMedivacPanel();
        }else if(isOnlyGhostSelected() && activeCommand == CommandMode.GHOST_TACTICAL_NUKE){
            buildCoreTargetPanel("Tactical Nuke", "Left-click target point");
        }else if(isOnlyGhostSelected() && activeCommand == CommandMode.GHOST_STABLE_AIM){
            buildCoreTargetPanel("Stable Aim", "Left-click biological unit target");
        }else if(isOnlyGhostSelected() && activeCommand == CommandMode.GHOST_EMP){
            buildCoreTargetPanel("EMP", "Left-click target point");
        }else if(isOnlyGhostSelected()){
            buildGhostPanel();
        }else if(isOnlyReaperSelected() && activeCommand == CommandMode.REAPER_KD8){
            buildCoreTargetPanel("KD8 Bomb", "Left-click target point");
        }else if(isOnlyReaperSelected()){
            buildReaperPanel();
        }else if(isOnlyVikingSelected()){
            buildVikingPanel();
        }else if(isOnlyBattlecruiserSelected() && activeCommand == CommandMode.BATTLECRUISER_YAMATO){
            buildCoreTargetPanel("Yamato Cannon", "Left-click enemy target");
        }else if(isOnlyBattlecruiserSelected() && activeCommand == CommandMode.BATTLECRUISER_WARP){
            buildCoreTargetPanel("Tactical Warp", "Left-click warp destination");
        }else if(isOnlyBattlecruiserSelected()){
            buildBattlecruiserPanel();
        }else if(isOnlyBansheeSelected()){
            buildBansheePanel();
        }else if(isOnlyRavenSelected() && activeCommand == CommandMode.RAVEN_ANTI_ARMOR){
            buildCoreTargetPanel("Anti-Armor Missile", "Left-click target area");
        }else if(isOnlyRavenSelected() && activeCommand == CommandMode.RAVEN_TURRET){
            buildCoreTargetPanel("Auto Turret", "Left-click placement location");
        }else if(isOnlyRavenSelected() && activeCommand == CommandMode.RAVEN_MATRIX){
            buildCoreTargetPanel("Interference Matrix", "Left-click mechanical/psionic target");
        }else if(isOnlyRavenSelected()){
            buildRavenPanel();
        }else if(isOnlyRavenTurretSelected()){
            buildRavenTurretPanel();
        }else if(isOnlyLiberatorSelected() && activeCommand == CommandMode.LIBERATOR_ZONE){
            buildCoreTargetPanel("Defense Mode", "Left-click defense zone");
        }else if(isOnlyLiberatorSelected()){
            buildLiberatorPanel();
        }else if(isOnlyBarracksStimpackSelected()){
            buildBarracksStimpackPanel();
        }else if(isOnlyCoreFlyerSelected()){
            buildCoreFlyerPanel();
        }else if(isOnlyWidowSelected()){
            buildWidowPanel();
        }else if(isOnlyMaceLocusSelected()){
            buildMaceLocusPanel();
        }else if(isOnlySiegeTankSelected()){
            buildPreceptPanel();
        }else if(isOnlyHurricaneSelected()){
            buildHurricanePanel();
        }else if(isOnlyScepterSelected()){
            buildScepterPanel();
        }else if(isOnlyNovaSelected() && activeCommand == CommandMode.BUILD_PLACE){
            buildNovaPlacementPanel();
        }else if(isOnlyNovaSelected()){
            buildNovaPanel();
        }else{
            buildDefaultPanel();
        }
    }

    private void buildDefaultPanel(){
        setPanelRows(3);
        //Check if we have any units selected (not just buildings)
        boolean hasUnits = hasAbilityUnits();

        //First row: RTS commands (Move, Stop, Hold, Patrol, Attack) - only for units
        if(hasUnits){
            for(int i = 0; i < commands.length; i++){
                final RTSCommand cmd = commands[i];
                addIconButton(this, cmd.key, cmd.icon, () -> true, () -> {
                    if(cmd.mode == CommandMode.STOP){
                        executeStopCommand();
                    }else if(cmd.mode == CommandMode.HOLD){
                        executeHoldCommand();
                    }else{
                        enterCommandMode(cmd.mode);
                    }
                });
            }
            row();
        }

        //Second row: Unit-specific abilities
        int col = 0;
        Seq<String> addedAbilities = new Seq<>();

        for(Unit unit : abilityUnits()){
                if(unit.isValid()){
                    if(unit.type.canBoost && !addedAbilities.contains("Boost")){
                    addAbilityButton("", Icon.upOpen, () -> true, () -> {});
                    addedAbilities.add("Boost");
                    col++;
                }
                if(unit instanceof Payloadc && !addedAbilities.contains("Pickup")){
                    addAbilityButton("", Icon.upload, () -> true, () -> {});
                    addedAbilities.add("Pickup");
                    col++;
                }
                if(unit instanceof Payloadc && !addedAbilities.contains("Drop")){
                    addAbilityButton("", Icon.download, () -> true, () -> {});
                    addedAbilities.add("Drop");
                    col++;
                }
                if(unit.type.mineTier >= 0 && !addedAbilities.contains("Mine")){
                    addAbilityButton("", Icon.terrain, () -> true, () -> {});
                    addedAbilities.add("Mine");
                    col++;
                }
                //Remove build ability for air support units (poly, mega, quad, oct)
                if(unit.type.buildSpeed > 0 && !addedAbilities.contains("Build") &&
                   !unit.type.name.equals("poly") && !unit.type.name.equals("mega") &&
                   !unit.type.name.equals("quad") && !unit.type.name.equals("oct")){
                    addAbilityButton("", Icon.hammer, () -> true, () -> {});
                    addedAbilities.add("Build");
                    col++;
                }
            }

            if(col >= COLS){
                break;
            }
        }

        //Fill remaining slots in second row if we have abilities
        if(col > 0){
            while(col < COLS){
                addEmpty(this);
                col++;
            }
            row();
        }

        //Third row: Additional abilities or empty
        //Fill third row with empty slots to maintain 3-row layout
        for(int i = 0; i < COLS; i++){
            addEmpty(this);
        }
    }

    private void buildBarracksStimpackPanel(){
        setPanelRows(3);
        Table grid = new Table();

        for(int i = 0; i < commands.length; i++){
            final RTSCommand cmd = commands[i];
            addIconButton(grid, cmd.key, cmd.icon, () -> true, () -> {
                if(cmd.mode == CommandMode.STOP){
                    executeStopCommand();
                }else if(cmd.mode == CommandMode.HOLD){
                    executeHoldCommand();
                }else{
                    enterCommandMode(cmd.mode);
                }
            });
        }
        grid.row();

        fillRow(grid, 1, 0);
        grid.row();

        addBattlecruiserCooldownButton(grid, "t", Icon.upOpen, this::anyBarracksStimpackSelectedCanUse,
        this::issueBarracksStimpackCommand,
        this::selectedBarracksStimpackCooldown, UnitTypes::barracksStimpackCooldownDuration);
        addEmpty(grid);
        addEmpty(grid);
        addEmpty(grid);
        addEmpty(grid);

        add(grid);
    }

    private void buildWidowPanel(){
        setPanelRows(3);
        Table grid = new Table();

        //Row 1: M/S/H/P/A
        if(anyWidowShowCommandRow1()){
            for(int i = 0; i < commands.length; i++){
                final RTSCommand cmd = commands[i];
                addIconButton(grid, cmd.key, cmd.icon, () -> true, () -> {
                    if(cmd.mode == CommandMode.STOP){
                        executeStopCommand();
                    }else if(cmd.mode == CommandMode.HOLD){
                        executeHoldCommand();
                    }else{
                        enterCommandMode(cmd.mode);
                    }
                });
            }
        }else{
            fillRow(grid, 1, 0);
        }
        grid.row();

        //Row 2: unused
        fillRow(grid, 1, 0);
        grid.row();

        //Row 3: col2/col3 for burrow/unburrow
        addEmpty(grid);

        if(anyWidowCanBurrow()){
            Button burrowButton = addIconButton(grid, "e", Icon.downOpen, this::anyWidowCanBurrow, () -> issueWidowBurrowCommand(true));
            BuildInfo burrowInfo = makeWidowActionInfo("e", "Widow Burrow", Color.cyan, this::selectedWidowBurrowProgress, this::anyWidowBurrowing);
            burrowButton.update(() -> {
                if(burrowButton.isOver()){
                    hoverBuildInfo = burrowInfo;
                }else if(hoverBuildInfo == burrowInfo){
                    hoverBuildInfo = null;
                }
            });
        }else{
            addEmpty(grid);
        }

        if(anyWidowCanUnburrow()){
            Button unburrowButton = addIconButton(grid, "d", Icon.upOpen, this::anyWidowCanUnburrow, () -> issueWidowBurrowCommand(false));
            BuildInfo reloadInfo = makeWidowActionInfo("d", "Widow Reload", Color.gray, this::selectedWidowReloadProgress, this::anyWidowReloading);
            unburrowButton.update(() -> {
                if(unburrowButton.isOver()){
                    hoverBuildInfo = reloadInfo;
                }else if(hoverBuildInfo == reloadInfo){
                    hoverBuildInfo = null;
                }
            });
        }else{
            addEmpty(grid);
        }

        addEmpty(grid);
        addEmpty(grid);

        add(grid);
    }

    private void buildReaperPanel(){
        setPanelRows(3);
        Table grid = new Table();

        for(int i = 0; i < commands.length; i++){
            final RTSCommand cmd = commands[i];
            addIconButton(grid, cmd.key, cmd.icon, () -> true, () -> {
                if(cmd.mode == CommandMode.STOP){
                    executeStopCommand();
                }else if(cmd.mode == CommandMode.HOLD){
                    executeHoldCommand();
                }else{
                    enterCommandMode(cmd.mode);
                }
            });
        }
        grid.row();

        fillRow(grid, 1, 0);
        grid.row();

        AbilityInfo kd8Info = makeAbilityInfo("d", "KD8 Bomb", "Throw a timed bomb that detonates after 1.5s. Deals 5 pierce damage and knocks back light targets.");
        kd8Info.timeSeconds = Math.round(UnitTypes.reaperKd8ArmTimeDuration() / 60f);
        addCooldownIconButton(grid, "d", Icon.warning, this::anyReaperCanUseKd8, () -> enterCommandMode(CommandMode.REAPER_KD8),
        this::selectedReaperKd8Cooldown, UnitTypes::reaperKd8CooldownDuration, kd8Info);
        addEmpty(grid);
        addEmpty(grid);
        addEmpty(grid);
        addEmpty(grid);

        add(grid);
    }

    private void buildHurricanePanel(){
        setPanelRows(3);
        Table grid = new Table();

        for(int i = 0; i < commands.length; i++){
            final RTSCommand cmd = commands[i];
            addIconButton(grid, cmd.key, cmd.icon, () -> true, () -> {
                if(cmd.mode == CommandMode.STOP){
                    executeStopCommand();
                }else if(cmd.mode == CommandMode.HOLD){
                    executeHoldCommand();
                }else{
                    enterCommandMode(cmd.mode);
                }
            });
        }
        grid.row();

        fillRow(grid, 1, 0);
        grid.row();

        addHurricaneLockButton(grid);
        addEmpty(grid);
        addEmpty(grid);
        addEmpty(grid);
        addEmpty(grid);

        add(grid);
    }

    private void buildScepterPanel(){
        setPanelRows(3);
        Table grid = new Table();

        for(int i = 0; i < commands.length; i++){
            final RTSCommand cmd = commands[i];
            addIconButton(grid, cmd.key, cmd.icon, () -> true, () -> {
                if(cmd.mode == CommandMode.STOP){
                    executeStopCommand();
                }else if(cmd.mode == CommandMode.HOLD){
                    executeHoldCommand();
                }else{
                    enterCommandMode(cmd.mode);
                }
            });
        }
        grid.row();

        fillRow(grid, 1, 0);
        grid.row();

        if(anyScepterCanSwitchToImpact()){
            addIconButton(grid, "e", Icon.upOpen, this::anyScepterCanSwitchToImpact, () -> issueScepterAirModeCommand(true));
        }else{
            addEmpty(grid);
        }

        if(anyScepterCanSwitchToBurst()){
            addIconButton(grid, "d", Icon.downOpen, this::anyScepterCanSwitchToBurst, () -> issueScepterAirModeCommand(false));
        }else{
            addEmpty(grid);
        }

        addEmpty(grid);
        addEmpty(grid);
        addEmpty(grid);

        add(grid);
    }

    private void buildMedivacPanel(){
        setPanelRows(3);
        Table grid = new Table();

        for(int i = 0; i < commands.length; i++){
            final RTSCommand cmd = commands[i];
            addIconButton(grid, cmd.key, cmd.icon, () -> true, () -> {
                if(cmd.mode == CommandMode.STOP){
                    executeStopCommand();
                }else if(cmd.mode == CommandMode.HOLD){
                    executeHoldCommand();
                }else{
                    enterCommandMode(cmd.mode);
                }
            });
        }
        grid.row();

        fillRow(grid, 1, 0);
        grid.row();

        addAutoCastIconButton(grid, "e", Icon.add, () -> true,
        () -> enterCommandMode(CommandMode.MEDIVAC_HEAL),
        this::selectedMedivacHealAutoCastEnabled, this::toggleSelectedMedivacHealAutoCast);
        addIconButton(grid, "b", Icon.upOpen, () -> true, this::issueMedivacAfterburnerCommand);

        if(anyMedivacCanLoadMore()){
            addIconButton(grid, "l", Icon.upload, this::anyMedivacCanLoadMore, () -> enterCommandMode(CommandMode.MEDIVAC_LOAD));
        }else{
            addEmpty(grid);
        }

        if(anyMedivacHasPayload()){
            addIconButton(grid, "d", Icon.download, this::anyMedivacHasPayload, () -> enterCommandMode(CommandMode.MEDIVAC_UNLOAD));
        }else{
            addEmpty(grid);
        }

        addEmpty(grid);
        add(grid);
    }

    private void buildGhostPanel(){
        setPanelRows(3);
        Table grid = new Table();

        for(int i = 0; i < commands.length; i++){
            final RTSCommand cmd = commands[i];
            addIconButton(grid, cmd.key, cmd.icon, () -> true, () -> {
                if(cmd.mode == CommandMode.STOP){
                    executeStopCommand();
                }else if(cmd.mode == CommandMode.HOLD){
                    executeHoldCommand();
                }else{
                    enterCommandMode(cmd.mode);
                }
            });
        }
        grid.row();

        addCountedIconButton(grid, "n", Icon.warning, this::anyGhostCanUseTacticalNuke, () -> enterCommandMode(CommandMode.GHOST_TACTICAL_NUKE), this::selectedGhostWarheadCount);
        addEmpty(grid);
        addEmpty(grid);
        addEmpty(grid);
        addEmpty(grid);
        grid.row();

        addIconButton(grid, "r", Icon.warning, this::anyGhostCanUseStableAim, () -> enterCommandMode(CommandMode.GHOST_STABLE_AIM));
        addIconButton(grid, "e", Icon.warning, this::anyGhostCanUseEmp, () -> enterCommandMode(CommandMode.GHOST_EMP));
        addIconButton(grid, "c", Icon.eyeSmall, this::anyGhostCanToggleCloak, this::issueGhostCloakCommand);
        addEmpty(grid);
        addEmpty(grid);

        add(grid);
    }

    private void buildVikingPanel(){
        setPanelRows(3);
        Table grid = new Table();

        for(int i = 0; i < commands.length; i++){
            final RTSCommand cmd = commands[i];
            addIconButton(grid, cmd.key, cmd.icon, () -> true, () -> {
                if(cmd.mode == CommandMode.STOP){
                    executeStopCommand();
                }else if(cmd.mode == CommandMode.HOLD){
                    executeHoldCommand();
                }else{
                    enterCommandMode(cmd.mode);
                }
            });
        }
        grid.row();

        fillRow(grid, 1, 0);
        grid.row();

        if(anyVikingCanSwitchToFighter()){
            AbilityInfo fighterInfo = makeAbilityInfo("e", "Fighter Mode", "Transform to Fighter Mode.");
            fighterInfo.timeSeconds = Math.round(UnitTypes.vikingTransformDuration(player.team()) / 60f);
            addIconButton(grid, "e", Icon.upOpen, this::anyVikingCanSwitchToFighter, () -> issueVikingModeCommand(false), fighterInfo);
        }else{
            addEmpty(grid);
        }
        if(anyVikingCanSwitchToMech()){
            AbilityInfo mechInfo = makeAbilityInfo("d", "Mech Mode", "Transform to Mech Mode.");
            mechInfo.timeSeconds = Math.round(UnitTypes.vikingTransformDuration(player.team()) / 60f);
            addIconButton(grid, "d", Icon.downOpen, this::anyVikingCanSwitchToMech, () -> issueVikingModeCommand(true), mechInfo);
        }else{
            addEmpty(grid);
        }
        addEmpty(grid);
        addEmpty(grid);
        addEmpty(grid);

        add(grid);
    }

    private void buildMaceLocusPanel(){
        setPanelRows(3);
        Table grid = new Table();

        for(int i = 0; i < commands.length; i++){
            final RTSCommand cmd = commands[i];
            addIconButton(grid, cmd.key, cmd.icon, () -> true, () -> {
                if(cmd.mode == CommandMode.STOP){
                    executeStopCommand();
                }else if(cmd.mode == CommandMode.HOLD){
                    executeHoldCommand();
                }else{
                    enterCommandMode(cmd.mode);
                }
            });
        }
        grid.row();

        fillRow(grid, 1, 0);
        grid.row();

        if(anyMaceSelected()){
            AbilityInfo locusInfo = makeAbilityInfo("e", "Locus Mode", "Transform to Locus. Requires Armory.");
            locusInfo.timeSeconds = Math.round(UnitTypes.maceLocusTransformDuration(player.team()) / 60f);
            addIconButton(
                grid, "e", Icon.upOpen, this::anyMaceCanTransformToLocus,
                () -> issueMaceLocusModeCommand(true),
                locusInfo
            );
        }else{
            addEmpty(grid);
        }

        if(anyLocusSelected()){
            AbilityInfo maceInfo = makeAbilityInfo("d", "Mace Mode", "Transform to Mace. Requires Armory.");
            maceInfo.timeSeconds = Math.round(UnitTypes.maceLocusTransformDuration(player.team()) / 60f);
            addIconButton(
                grid, "d", Icon.downOpen, this::anyLocusCanTransformToMace,
                () -> issueMaceLocusModeCommand(false),
                maceInfo
            );
        }else{
            addEmpty(grid);
        }

        addEmpty(grid);
        addEmpty(grid);
        addEmpty(grid);

        add(grid);
    }

    private void buildMacePanel(){
        setPanelRows(3);
        Table grid = new Table();

        for(int i = 0; i < commands.length; i++){
            final RTSCommand cmd = commands[i];
            addIconButton(grid, cmd.key, cmd.icon, () -> true, () -> {
                if(cmd.mode == CommandMode.STOP){
                    executeStopCommand();
                }else if(cmd.mode == CommandMode.HOLD){
                    executeHoldCommand();
                }else{
                    enterCommandMode(cmd.mode);
                }
            });
        }
        grid.row();

        fillRow(grid, 1, 0);
        grid.row();

        AbilityInfo locusInfo = makeAbilityInfo("e", "Locus Mode", "Transform to Locus. Requires Armory.");
        locusInfo.timeSeconds = Math.round(UnitTypes.maceLocusTransformDuration(player.team()) / 60f);
        addIconButton(
            grid, "e", Icon.upOpen, this::anyMaceCanTransformToLocus,
            () -> issueMaceLocusModeCommand(true),
            locusInfo
        );
        addEmpty(grid);
        addEmpty(grid);
        addEmpty(grid);
        addEmpty(grid);

        add(grid);
    }

    private void buildLocusPanel(){
        setPanelRows(3);
        Table grid = new Table();

        for(int i = 0; i < commands.length; i++){
            final RTSCommand cmd = commands[i];
            addIconButton(grid, cmd.key, cmd.icon, () -> true, () -> {
                if(cmd.mode == CommandMode.STOP){
                    executeStopCommand();
                }else if(cmd.mode == CommandMode.HOLD){
                    executeHoldCommand();
                }else{
                    enterCommandMode(cmd.mode);
                }
            });
        }
        grid.row();

        fillRow(grid, 1, 0);
        grid.row();

        addEmpty(grid);
        AbilityInfo maceInfo = makeAbilityInfo("d", "Mace Mode", "Transform to Mace. Requires Armory.");
        maceInfo.timeSeconds = Math.round(UnitTypes.maceLocusTransformDuration(player.team()) / 60f);
        addIconButton(
            grid, "d", Icon.downOpen, this::anyLocusCanTransformToMace,
            () -> issueMaceLocusModeCommand(false),
            maceInfo
        );
        addEmpty(grid);
        addEmpty(grid);
        addEmpty(grid);

        add(grid);
    }

    private void buildRavenPanel(){
        setPanelRows(3);
        Table grid = new Table();

        for(int i = 0; i < commands.length; i++){
            final RTSCommand cmd = commands[i];
            addIconButton(grid, cmd.key, cmd.icon, () -> true, () -> {
                if(cmd.mode == CommandMode.STOP){
                    executeStopCommand();
                }else if(cmd.mode == CommandMode.HOLD){
                    executeHoldCommand();
                }else{
                    enterCommandMode(cmd.mode);
                }
            });
        }
        grid.row();

        fillRow(grid, 1, 0);
        grid.row();

        addIconButton(grid, "t", Icon.add, this::anyRavenCanDeployTurret, () -> enterCommandMode(CommandMode.RAVEN_TURRET));
        addIconButton(grid, "r", Icon.downOpen, this::anyRavenCanUseAntiArmor, () -> enterCommandMode(CommandMode.RAVEN_ANTI_ARMOR));
        addIconButton(grid, "c", Icon.warning, this::anyRavenCanUseMatrix, () -> enterCommandMode(CommandMode.RAVEN_MATRIX));
        addEmpty(grid);
        addEmpty(grid);

        add(grid);
    }

    private void buildBansheePanel(){
        setPanelRows(3);
        Table grid = new Table();

        for(int i = 0; i < commands.length; i++){
            final RTSCommand cmd = commands[i];
            addIconButton(grid, cmd.key, cmd.icon, () -> true, () -> {
                if(cmd.mode == CommandMode.STOP){
                    executeStopCommand();
                }else if(cmd.mode == CommandMode.HOLD){
                    executeHoldCommand();
                }else{
                    enterCommandMode(cmd.mode);
                }
            });
        }
        grid.row();

        fillRow(grid, 1, 0);
        grid.row();

        addIconButton(grid, "c", Icon.eyeSmall, this::anyBansheeCanToggleCloak, this::issueBansheeCloakCommand);
        addEmpty(grid);
        addEmpty(grid);
        addEmpty(grid);
        addEmpty(grid);

        add(grid);
    }

    private void buildBattlecruiserPanel(){
        setPanelRows(3);
        Table grid = new Table();

        for(int i = 0; i < commands.length; i++){
            final RTSCommand cmd = commands[i];
            addIconButton(grid, cmd.key, cmd.icon, () -> true, () -> {
                if(cmd.mode == CommandMode.STOP){
                    executeStopCommand();
                }else if(cmd.mode == CommandMode.HOLD){
                    executeHoldCommand();
                }else{
                    enterCommandMode(cmd.mode);
                }
            });
        }
        grid.row();

        fillRow(grid, 1, 0);
        grid.row();

        addBattlecruiserCooldownButton(grid, "y", Icon.warning, this::anyBattlecruiserCanUseYamato,
        () -> enterCommandMode(CommandMode.BATTLECRUISER_YAMATO),
        this::selectedBattlecruiserYamatoCooldown, UnitTypes::battlecruiserYamatoCooldownDuration);
        addBattlecruiserCooldownButton(grid, "t", Icon.effect, this::anyBattlecruiserCanUseWarp,
        () -> enterCommandMode(CommandMode.BATTLECRUISER_WARP),
        this::selectedBattlecruiserWarpCooldown, UnitTypes::battlecruiserWarpCooldownDuration);
        addEmpty(grid);
        addEmpty(grid);
        addEmpty(grid);

        add(grid);
    }

    private void buildRavenTurretPanel(){
        setPanelRows(3);
        Table grid = new Table();

        addEmpty(grid);
        addIconButton(grid, "s", Icon.cancel, () -> true, this::executeStopCommand);
        addEmpty(grid);
        addEmpty(grid);
        addIconButton(grid, "a", Icon.warning, () -> true, () -> enterCommandMode(CommandMode.ATTACK));
        grid.row();

        fillRow(grid, 1, 0);
        grid.row();

        fillRow(grid, 2, 0);

        add(grid);
    }

    private void buildLiberatorPanel(){
        setPanelRows(3);
        Table grid = new Table();

        boolean defenseLayout = allSelectedLiberatorDefending();
        boolean transitioning = anyLiberatorTransitioning();

        if(transitioning){
            fillRow(grid, 0, 0);
        }else if(defenseLayout){
            addEmpty(grid);
            addIconButton(grid, "s", Icon.cancel, () -> true, this::executeStopCommand);
            addEmpty(grid);
            addEmpty(grid);
            addIconButton(grid, "a", Icon.warning, () -> true, () -> enterCommandMode(CommandMode.ATTACK));
        }else{
            for(int i = 0; i < commands.length; i++){
                final RTSCommand cmd = commands[i];
                addIconButton(grid, cmd.key, cmd.icon, () -> true, () -> {
                    if(cmd.mode == CommandMode.STOP){
                        executeStopCommand();
                    }else if(cmd.mode == CommandMode.HOLD){
                        executeHoldCommand();
                    }else{
                        enterCommandMode(cmd.mode);
                    }
                });
            }
        }
        grid.row();

        fillRow(grid, 1, 0);
        grid.row();

        if(anyLiberatorCanEnterDefense()){
            addIconButton(grid, "e", Icon.downOpen, this::anyLiberatorCanEnterDefense, () -> enterCommandMode(CommandMode.LIBERATOR_ZONE));
        }else{
            addEmpty(grid);
        }

        if(anyLiberatorCanExitDefense()){
            addIconButton(grid, "d", Icon.upOpen, this::anyLiberatorCanExitDefense, this::issueLiberatorFighterCommand);
        }else{
            addEmpty(grid);
        }

        addEmpty(grid);
        addEmpty(grid);
        addEmpty(grid);

        add(grid);
    }

    private void buildPreceptPanel(){
        setPanelRows(3);
        Table grid = new Table();

        if(anyPreceptTransitioning()){
            fillRow(grid, 0, 0);
        }else if(allSelectedPreceptSieged()){
            addEmpty(grid);
            addIconButton(grid, "s", Icon.cancel, () -> true, this::executeStopCommand);
            addEmpty(grid);
            addEmpty(grid);
            addIconButton(grid, "a", Icon.warning, () -> true, () -> enterCommandMode(CommandMode.ATTACK));
        }else{
            for(int i = 0; i < commands.length; i++){
                final RTSCommand cmd = commands[i];
                addIconButton(grid, cmd.key, cmd.icon, () -> true, () -> {
                    if(cmd.mode == CommandMode.STOP){
                        executeStopCommand();
                    }else if(cmd.mode == CommandMode.HOLD){
                        executeHoldCommand();
                    }else{
                        enterCommandMode(cmd.mode);
                    }
                });
            }
        }
        grid.row();

        fillRow(grid, 1, 0);
        grid.row();

        if(anyPreceptCanSiege()){
            addIconButton(grid, "e", Icon.downOpen, this::anyPreceptCanSiege, () -> issuePreceptSiegeCommand(true));
        }else{
            addEmpty(grid);
        }

        if(anyPreceptCanTankMode()){
            addIconButton(grid, "d", Icon.upOpen, this::anyPreceptCanTankMode, () -> issuePreceptSiegeCommand(false));
        }else{
            addEmpty(grid);
        }

        addEmpty(grid);
        addEmpty(grid);
        addEmpty(grid);

        add(grid);
    }

    private void buildNovaPanel(){
        setPanelRows(3);
        switch(novaPanel){
            case BUILD_BASIC:
                buildNovaBasicPanel();
                break;
            case BUILD_ADV:
                buildNovaAdvancedPanel();
                break;
            default:
                buildNovaMainPanel();
                break;
        }
    }

    private void buildNovaMainPanel(){
        setPanelRows(3);
        Table grid = new Table();
        //Row 1: RTS commands M/S/H/P/A
        for(int i = 0; i < COLS; i++){
            final RTSCommand cmd = commands[i];
            addIconButton(grid, cmd.key, cmd.icon, () -> true, () -> {
                if(cmd.mode == CommandMode.STOP){
                    executeStopCommand();
                }else if(cmd.mode == CommandMode.HOLD){
                    executeHoldCommand();
                }else{
                    enterCommandMode(cmd.mode);
                }
            });
        }
        grid.row();

        //Row 2
        addIconButton(grid, "g", Icon.terrain, () -> true, () -> enterCommandMode(CommandMode.HARVEST));
        addIconButton(grid, "r", UnitCommand.repairCommand.getIcon(), () -> true, () -> enterCommandMode(CommandMode.REPAIR));
        fillRow(grid, 1, 2);
        grid.row();

        //Row 3
        addIconButton(grid, "b", Icon.hammer, () -> true, () -> novaPanel = NovaPanel.BUILD_BASIC);
        addIconButton(grid, "v", Icon.wrench, () -> true, () -> novaPanel = NovaPanel.BUILD_ADV);
        fillRow(grid, 2, 2);

        add(grid);
    }

    private void buildNovaPlacementPanel(){
        setPanelRows(3);
        Table grid = new Table();
        for(int r = 0; r < ROWS; r++){
            for(int c = 0; c < COLS; c++){
                if(r == 2 && c == 3){
                    addStopBuildButton(grid);
                }else if(r == 2 && c == 4){
                    addEscButton(grid, this::exitCommandMode);
                }else{
                    addEmpty(grid);
                }
            }
            grid.row();
        }
        add(grid);
    }

    private void buildNovaBasicPanel(){
        setPanelRows(3);
        Table grid = new Table();
        //Row 1
        addBuildButton(grid, "c", Blocks.coreNucleus, () -> true, () -> startPlacement(Blocks.coreNucleus));
        addBuildButton(grid, "r", Blocks.ventCondenser, () -> true, () -> startPlacement(Blocks.ventCondenser));
        addBuildButton(grid, "s", Blocks.doorLarge, () -> Build.meetsPrerequisites(Blocks.doorLarge, player.team()), () -> startPlacement(Blocks.doorLarge));
        fillRow(grid, 0, 3);
        grid.row();

        //Row 2
        addBuildButton(grid, "b", Blocks.groundFactory, () -> Build.meetsPrerequisites(Blocks.groundFactory, player.team()), () -> startPlacement(Blocks.groundFactory));
        addBuildButton(grid, "e", Blocks.multiPress, () -> Build.meetsPrerequisites(Blocks.multiPress, player.team()), () -> startPlacement(Blocks.multiPress));
        fillRow(grid, 1, 2);
        grid.row();

        //Row 3
        addBuildButton(grid, "u", Blocks.atmosphericConcentrator, () -> Build.meetsPrerequisites(Blocks.atmosphericConcentrator, player.team()), () -> startPlacement(Blocks.atmosphericConcentrator));
        addBuildButton(grid, "t", Blocks.swarmer, () -> Build.meetsPrerequisites(Blocks.swarmer, player.team()), () -> startPlacement(Blocks.swarmer));
        addBuildButton(grid, "n", Blocks.radar, () -> Build.meetsPrerequisites(Blocks.radar, player.team()), () -> startPlacement(Blocks.radar));
        addEmpty(grid);
        addEscButton(grid, () -> novaPanel = NovaPanel.MAIN);
        add(grid);
    }

    private void buildNovaAdvancedPanel(){
        setPanelRows(3);
        Table grid = new Table();
        //Row 1
        addBuildButton(grid, "g", Blocks.launchPad, () -> Build.meetsPrerequisites(Blocks.launchPad, player.team()), () -> startPlacement(Blocks.launchPad));
        fillRow(grid, 0, 1);
        grid.row();

        //Row 2
        addBuildButton(grid, "f", Blocks.tankFabricator, () -> Build.meetsPrerequisites(Blocks.tankFabricator, player.team()), () -> startPlacement(Blocks.tankFabricator));
        addBuildButton(grid, "a", Blocks.siliconCrucible, () -> Build.meetsPrerequisites(Blocks.siliconCrucible, player.team()), () -> startPlacement(Blocks.siliconCrucible));
        fillRow(grid, 1, 2);
        grid.row();

        //Row 3
        addBuildButton(grid, "s", Blocks.shipFabricator, () -> Build.meetsPrerequisites(Blocks.shipFabricator, player.team()), () -> startPlacement(Blocks.shipFabricator));
        addBuildButton(grid, "c", Blocks.surgeCrucible, () -> Build.meetsPrerequisites(Blocks.surgeCrucible, player.team()), () -> startPlacement(Blocks.surgeCrucible));
        addEmpty(grid);
        addEmpty(grid);
        addEscButton(grid, () -> novaPanel = NovaPanel.MAIN);
        add(grid);
    }

    private void buildBuildingPanel(){
        if(abilityBuildings().isEmpty()){
            buildEmptyPanel();
            setPanelRows(ROWS);
            return;
        }

        Building build = abilityBuildings().first();
        if(build instanceof CoreBuild core){
            buildCorePanel(core);
            return;
        }

        if(isOnlySupplySelected()){
            buildSupplyPanel();
            return;
        }

        if(isOnlyRadarSelected()){
            buildRadarPanel();
            return;
        }

        if(isOnlyBunkerSelected()){
            buildBunkerPanel();
            return;
        }

        if(isOnlyArmorySelected()){
            buildArmoryPanel();
            return;
        }

        if(isOnlyFusionCoreSelected()){
            buildFusionCorePanel();
            return;
        }

        if(isOnlyEngineeringSelected()){
            buildEngineeringPanel();
            return;
        }

        if(isOnlyGhostAcademySelected()){
            buildGhostAcademyPanel();
            return;
        }

        if(isOnlyTechLabSelected()){
            buildTechLabPanel();
            return;
        }

        if(build instanceof UnitFactory.UnitFactoryBuild factory && factory.sc2QueueEnabled()){
            buildFactoryPanel(factory);
            return;
        }

        if(!(build instanceof ConstructBuild)){
            buildEmptyPanel();
            setPanelRows(2);
            return;
        }

        ConstructBuild cons = (ConstructBuild)build;
        boolean incomplete = cons.current != null && cons.current != Blocks.air && cons.progress < 1f;

        Table grid = new Table();
        for(int r = 0; r < ROWS; r++){
            for(int c = 0; c < COLS; c++){
                if(r == 1 && c == 3 && incomplete){
                    Unit builder = findActiveBuilder(cons);
                    if(builder != null){
                        addIconButton(grid, "q", Icon.zoom, () -> true, () -> selectBuilder(builder));
                    }else{
                        addEmpty(grid);
                    }
                }else if(r == 2 && c == 3 && incomplete){
                    Unit builder = findActiveBuilder(cons);
                    if(builder != null){
                        addIconButton(grid, "t", Icon.pause, () -> true, () -> pauseBuilder(builder));
                    }else{
                        addEmpty(grid);
                    }
                }else if(r == 2 && c == 4 && incomplete){
                    addIconButton(grid, "Esc", Icon.cancel, () -> true, () -> cancelConstruct(cons));
                }else{
                    addEmpty(grid);
                }
            }
            grid.row();
        }

        setPanelRows(3);
        add(grid);
    }

    private void buildFactoryPanel(UnitFactory.UnitFactoryBuild factory){
        if(activeCommand == CommandMode.RALLY){
            buildCoreRallyPanel();
            return;
        }
        setPanelRows(3);
        Table grid = new Table();
        boolean showAddonButtons = factory.canShowAddonButtons();
        UnitFactory block = (UnitFactory)factory.block;
        if(block == Blocks.tankFabricator){
            int locusIndex = block.plans.indexOf(p -> p.unit == UnitTypes.locus);
            int crawlerIndex = block.plans.indexOf(p -> p.unit == UnitTypes.crawler);
            int hurricaneIndex = block.plans.indexOf(p -> p.unit == UnitTypes.hurricane);
            int preceptIndex = block.plans.indexOf(p -> p.unit == UnitTypes.precept);
            int maceIndex = block.plans.indexOf(p -> p.unit == UnitTypes.mace);
            int scepterIndex = block.plans.indexOf(p -> p.unit == UnitTypes.scepter);

            //Row 1
            if(locusIndex != -1){
                addUnitButton(grid, "e", block.plans.get(locusIndex), () -> factory.canQueuePlan(locusIndex), () -> factory.configure(locusIndex));
            }else{
                addEmpty(grid);
            }
            if(crawlerIndex != -1){
                addUnitButton(grid, "d", block.plans.get(crawlerIndex), () -> factory.canQueuePlan(crawlerIndex), () -> factory.configure(crawlerIndex));
            }else{
                addEmpty(grid);
            }
            if(hurricaneIndex != -1){
                addUnitButton(grid, "n", block.plans.get(hurricaneIndex), () -> factory.canQueuePlan(hurricaneIndex), () -> factory.configure(hurricaneIndex));
            }else{
                addEmpty(grid);
            }
            if(preceptIndex != -1){
                addUnitButton(grid, "s", block.plans.get(preceptIndex), () -> factory.canQueuePlan(preceptIndex), () -> factory.configure(preceptIndex));
            }else{
                addEmpty(grid);
            }
            addEmpty(grid);
            grid.row();

            //Row 2
            if(maceIndex != -1){
                addUnitButton(grid, "r", block.plans.get(maceIndex), () -> factory.canQueuePlan(maceIndex), () -> factory.configure(maceIndex));
            }else{
                addEmpty(grid);
            }
            if(scepterIndex != -1){
                addUnitButton(grid, "t", block.plans.get(scepterIndex), () -> factory.canQueuePlan(scepterIndex), () -> factory.configure(scepterIndex));
            }else{
                addEmpty(grid);
            }
            addEmpty(grid);
            addEmpty(grid);
            addIconButton(grid, "y", Icon.commandRally, () -> true, () -> enterCommandMode(CommandMode.RALLY));
            grid.row();
        }else if(block == Blocks.shipFabricator){
            int flareIndex = block.plans.indexOf(p -> p.unit == UnitTypes.flare);
            int megaIndex = block.plans.indexOf(p -> p.unit == UnitTypes.mega);
            int liberatorIndex = block.plans.indexOf(p -> p.unit == UnitTypes.liberator);
            int avertIndex = block.plans.indexOf(p -> p.unit == UnitTypes.avert);
            int horizonIndex = block.plans.indexOf(p -> p.unit == UnitTypes.horizon);
            int antumbraIndex = block.plans.indexOf(p -> p.unit == UnitTypes.antumbra);

            //Row 1
            if(flareIndex != -1){
                addUnitButton(grid, "v", block.plans.get(flareIndex), () -> factory.canQueuePlan(flareIndex), () -> factory.configure(flareIndex));
            }else{
                addEmpty(grid);
            }
            if(megaIndex != -1){
                addUnitButton(grid, "d", block.plans.get(megaIndex), () -> factory.canQueuePlan(megaIndex), () -> factory.configure(megaIndex));
            }else{
                addEmpty(grid);
            }
            if(liberatorIndex != -1){
                addUnitButton(grid, "n", block.plans.get(liberatorIndex), () -> factory.canQueuePlan(liberatorIndex), () -> factory.configure(liberatorIndex));
            }else{
                addEmpty(grid);
            }
            if(avertIndex != -1){
                addUnitButton(grid, "r", block.plans.get(avertIndex), () -> factory.canQueuePlan(avertIndex), () -> factory.configure(avertIndex));
            }else{
                addEmpty(grid);
            }
            if(horizonIndex != -1){
                addUnitButton(grid, "e", block.plans.get(horizonIndex), () -> factory.canQueuePlan(horizonIndex), () -> factory.configure(horizonIndex));
            }else{
                addEmpty(grid);
            }
            grid.row();

            //Row 2
            if(antumbraIndex != -1){
                addUnitButton(grid, "b", block.plans.get(antumbraIndex), () -> factory.canQueuePlan(antumbraIndex), () -> factory.configure(antumbraIndex));
            }else{
                addEmpty(grid);
            }
            addEmpty(grid);
            addEmpty(grid);
            addEmpty(grid);
            addIconButton(grid, "y", Icon.commandRally, () -> true, () -> enterCommandMode(CommandMode.RALLY));
            grid.row();
        }else if(block == Blocks.groundFactory){
            int daggerIndex = block.plans.indexOf(p -> p.unit == UnitTypes.dagger);
            int reaperIndex = block.plans.indexOf(p -> p.unit == UnitTypes.reaper);
            int fortressIndex = block.plans.indexOf(p -> p.unit == UnitTypes.fortress);
            int ghostIndex = block.plans.indexOf(p -> p.unit == UnitTypes.ghost);

            //Row 1
            if(daggerIndex != -1){
                addUnitButton(grid, "a", block.plans.get(daggerIndex), () -> factory.canQueuePlan(daggerIndex), () -> factory.configure(daggerIndex));
            }else{
                addEmpty(grid);
            }
            if(reaperIndex != -1){
                addUnitButton(grid, "r", block.plans.get(reaperIndex), () -> factory.canQueuePlan(reaperIndex), () -> factory.configure(reaperIndex));
            }else{
                addEmpty(grid);
            }
            if(fortressIndex != -1){
                addUnitButton(grid, "d", block.plans.get(fortressIndex), () -> factory.canQueuePlan(fortressIndex), () -> factory.configure(fortressIndex));
            }else{
                addEmpty(grid);
            }
            if(ghostIndex != -1){
                addUnitButton(grid, "g", block.plans.get(ghostIndex), () -> factory.canQueuePlan(ghostIndex), () -> factory.configure(ghostIndex));
            }else{
                addEmpty(grid);
            }
            addEmpty(grid);
            grid.row();

            //Row 2
            fillRow(grid, 1, 0);
            grid.row();
        }else{
            String[] row1Keys = {"a", "r", "d", "g"};
            String[] row2Keys = {"f", "t"};

            for(int i = 0; i < row1Keys.length; i++){
                if(i < block.plans.size){
                    int planIndex = i;
                    addUnitButton(grid, row1Keys[i], block.plans.get(planIndex), () -> factory.canQueuePlan(planIndex), () -> factory.configure(planIndex));
                }else{
                    addEmpty(grid);
                }
            }
            addEmpty(grid);
            grid.row();

            for(int i = 0; i < row2Keys.length; i++){
                int planIndex = i + row1Keys.length;
                if(planIndex < block.plans.size){
                    addUnitButton(grid, row2Keys[i], block.plans.get(planIndex), () -> factory.canQueuePlan(planIndex), () -> factory.configure(planIndex));
                }else{
                    addEmpty(grid);
                }
            }
            addEmpty(grid);
            addEmpty(grid);
            addEmpty(grid);
            grid.row();
        }

        //Row 3
        if(showAddonButtons){
            addAddonBuildButton(grid, "x", "Tech Addon", Blocks.memoryBank,
                UnitFactory.sc2AddonCrystalCost, UnitFactory.sc2AddonTechGasCost, UnitFactory.sc2AddonTechTime,
                () -> true, () -> factory.configure(UnitFactory.sc2AddonTechConfig));
            addAddonBuildButton(grid, "c", "Double Addon", Blocks.rotaryPump,
                UnitFactory.sc2AddonCrystalCost, UnitFactory.sc2AddonDoubleGasCost, UnitFactory.sc2AddonDoubleTime,
                () -> true, () -> factory.configure(UnitFactory.sc2AddonDoubleConfig));
        }else{
            addEmpty(grid);
            addEmpty(grid);
        }
        addEmpty(grid);
        addIconButton(grid, "l", Icon.export, () -> factory.canLift(), () -> queueFactoryLift(factory));
        addCancelButton(grid, () -> factory.configure(UnitFactory.sc2AddonCancelConfig));
        add(grid);
    }

    private void buildSupplyPanel(){
        setPanelRows(3);
        Table grid = new Table();
        boolean anyClosed = supplyAnyClosed();
        Drawable icon = anyClosed ? Icon.downOpen : Icon.upOpen;

        for(int r = 0; r < ROWS; r++){
            for(int c = 0; c < COLS; c++){
                if(r == 2 && c == 0){
                    addIconButton(grid, "r", icon, () -> true, this::toggleSupplyDoors);
                }else{
                    addEmpty(grid);
                }
            }
            grid.row();
        }

        add(grid);
    }

    private void buildArmoryPanel(){
        setPanelRows(3);
        Table grid = new Table();

        Seq<Sc2ResearchSpec> specs = ResearchQueueService.armorySpecs();
        Block[] icons = {Blocks.siliconCrucible, Blocks.surgeCrucible, Blocks.shipFabricator};
        int[] slots = {0, 1, 5};

        for(int slot = 0; slot < COLS * ROWS; slot++){
            if(slot == COLS * (ROWS - 1) + (COLS - 1)){
                if(ResearchQueueService.armoryAnyResearching(player.team())){
                    BuildInfo researchInfo = makeArmoryAnyResearchInfo("Esc", specs, icons);
                    Button cancelButton = addIconButton(grid, "Esc", Icon.cancel, () -> true, this::cancelArmoryResearch);
                    cancelButton.update(() -> {
                        if(cancelButton.isOver()){
                            hoverBuildInfo = researchInfo;
                        }else if(hoverBuildInfo == researchInfo){
                            hoverBuildInfo = null;
                        }
                    });
                }else{
                    addEmpty(grid);
                }
            }else{
                int specIndex = specSlotIndex(slot, slots);
                if(specIndex >= 0 && specIndex < specs.size){
                    addCatalogResearchButton(grid, specs.get(specIndex), icons[specIndex]);
                }else{
                    addEmpty(grid);
                }
            }

            if(slot % COLS == COLS - 1){
                grid.row();
            }
        }

        add(grid);
    }

    private void buildEngineeringPanel(){
        setPanelRows(3);
        Table grid = new Table();

        Seq<Sc2ResearchSpec> specs = ResearchQueueService.engineeringSpecs();
        Block[] icons = {Blocks.siliconCrucible, Blocks.multiPress, Blocks.swarmer, Blocks.atmosphericConcentrator};
        int[] slots = {0, 1, 5, 6};

        for(int slot = 0; slot < COLS * ROWS; slot++){
            if(slot == COLS * (ROWS - 1) + (COLS - 1)){
                if(ResearchQueueService.engineeringAnyResearching(player.team())){
                    BuildInfo researchInfo = makeInfantryAnyResearchInfo("Esc", specs, icons);
                    Button cancelButton = addIconButton(grid, "Esc", Icon.cancel, () -> true, this::cancelInfantryResearch);
                    cancelButton.update(() -> {
                        if(cancelButton.isOver()){
                            hoverBuildInfo = researchInfo;
                        }else if(hoverBuildInfo == researchInfo){
                            hoverBuildInfo = null;
                        }
                    });
                }else{
                    addEmpty(grid);
                }
            }else{
                int specIndex = specSlotIndex(slot, slots);
                if(specIndex >= 0 && specIndex < specs.size){
                    addCatalogResearchButton(grid, specs.get(specIndex), icons[specIndex]);
                }else{
                    addEmpty(grid);
                }
            }

            if(slot % COLS == COLS - 1){
                grid.row();
            }
        }

        add(grid);
    }

    private int specSlotIndex(int slot, int[] slots){
        for(int i = 0; i < slots.length; i++){
            if(slots[i] == slot) return i;
        }
        return -1;
    }

    private void addCatalogResearchButton(Table grid, Sc2ResearchSpec spec, Block iconBlock){
        if(spec.displayAvailable(player.team())){
            BuildInfo upgradeInfo = makeCatalogResearchInfo(spec, spec.hotkey, iconBlock);
            Button upgradeButton = addIconButton(grid, spec.hotkey, new TextureRegionDrawable(iconBlock.uiIcon),
                () -> spec.canStart(player.team()),
                () -> tryStartResearchSpec(spec));
            upgradeButton.update(() -> {
                if(upgradeButton.isOver()){
                    hoverBuildInfo = upgradeInfo;
                }else if(hoverBuildInfo == upgradeInfo){
                    hoverBuildInfo = null;
                }
            });
        }else{
            addEmpty(grid);
        }
    }

    private BuildInfo makeCatalogResearchInfo(Sc2ResearchSpec spec, String key, Block iconBlock){
        BuildInfo info = new BuildInfo();
        info.block = iconBlock;
        info.key = key;
        info.name = spec.name(player.team());
        info.action = "Research";
        info.crystalCost = spec.crystalCost(player.team());
        info.gasCost = spec.gasCost(player.team());
        info.timeSeconds = Math.round(spec.duration(player.team()) / 60f);
        info.progress = () -> spec.progress(player.team());
        info.progressVisible = () -> spec.researching(player.team());
        info.progressColor = Color.cyan;
        info.progressIcon = new TextureRegionDrawable(iconBlock.uiIcon);
        return info;
    }

    private void buildGhostAcademyPanel(){
        setPanelRows(3);
        Table grid = new Table();

        if(UnitTypes.ghostCamoLevel(player.team()) <= 0){
            BuildInfo upgradeInfo = makeGhostCamoUpgradeInfo("c");
            Button upgradeButton = addIconButton(grid, "c", new TextureRegionDrawable(Blocks.launchPad.uiIcon),
                () -> UnitTypes.ghostCamoCanStartResearch(player.team()),
                this::tryStartGhostCamoResearch);
            upgradeButton.update(() -> {
                if(upgradeButton.isOver()){
                    hoverBuildInfo = upgradeInfo;
                }else if(hoverBuildInfo == upgradeInfo){
                    hoverBuildInfo = null;
                }
            });
        }else{
            addEmpty(grid);
        }
        addEmpty(grid);
        addEmpty(grid);
        addEmpty(grid);
        addEmpty(grid);
        grid.row();

        fillRow(grid, 1, 0);
        grid.row();

        BuildInfo warheadInfo = makeGhostWarheadBuildInfo("n");
        Button warheadButton = addIconButton(grid, "n", Icon.warning, this::anyGhostAcademyCanBuildWarhead, this::tryStartGhostWarheadProduction);
        warheadButton.update(() -> {
            if(warheadButton.isOver()){
                hoverBuildInfo = warheadInfo;
            }else if(hoverBuildInfo == warheadInfo){
                hoverBuildInfo = null;
            }
        });
        addEmpty(grid);
        addEmpty(grid);
        addEmpty(grid);
        if(UnitTypes.ghostCamoAnyResearching(player.team())){
            BuildInfo researchInfo = makeGhostCamoResearchInfo("Esc");
            Button cancelButton = addIconButton(grid, "Esc", Icon.cancel, () -> true, this::cancelGhostCamoResearch);
            cancelButton.update(() -> {
                if(cancelButton.isOver()){
                    hoverBuildInfo = researchInfo;
                }else if(hoverBuildInfo == researchInfo){
                    hoverBuildInfo = null;
                }
            });
        }else{
            addEmpty(grid);
        }
        add(grid);
    }

    private void buildFusionCorePanel(){
        setPanelRows(3);
        Table grid = new Table();
        Team team = player.team();

        Seq<Sc2ResearchSpec> specs = ResearchQueueService.fusionCoreSpecs();
        int col = 0;
        for(int i = 0; i < specs.size && col < COLS; i++, col++){
            Sc2ResearchSpec spec = specs.get(i);
            if(spec.displayAvailable(team)){
                BuildInfo researchInfo = makeFusionCoreResearchInfo(spec, spec.hotkey);
                UnitType iconUnit = spec.iconUnit();
                Drawable icon = new TextureRegionDrawable(iconUnit == null ? Blocks.surgeCrucible.uiIcon : iconUnit.uiIcon);
                Button researchButton = addIconButton(grid, spec.hotkey, icon,
                    () -> spec.canStart(team),
                    () -> tryStartResearchSpec(spec));
                researchButton.update(() -> {
                    if(researchButton.isOver()){
                        hoverBuildInfo = researchInfo;
                    }else if(hoverBuildInfo == researchInfo){
                        hoverBuildInfo = null;
                    }
                });
            }else{
                addEmpty(grid);
            }
        }
        for(; col < COLS; col++){
            addEmpty(grid);
        }

        fillRow(grid, 1, 0);
        grid.row();

        addEmpty(grid);
        addEmpty(grid);
        addEmpty(grid);
        addEmpty(grid);
        if(ResearchQueueService.fusionCoreAnyResearching(team)){
            BuildInfo cancelInfo = makeFusionCoreAnyResearchInfo("Esc");
            Button cancelButton = addIconButton(grid, "Esc", Icon.cancel, () -> true, this::cancelFusionCoreResearch);
            cancelButton.update(() -> {
                if(cancelButton.isOver()){
                    hoverBuildInfo = cancelInfo;
                }else if(hoverBuildInfo == cancelInfo){
                    hoverBuildInfo = null;
                }
            });
        }else{
            addEmpty(grid);
        }
        add(grid);
    }

    private void buildTechLabPanel(){
        Block attached = selectedTechLabAttachedFactoryBlock();
        Seq<Sc2ResearchSpec> specs = ResearchQueueService.techLabSpecs(attached);
        if(specs.any()){
            buildTechLabPanelForFactory(attached, specs);
            return;
        }

        setPanelRows(3);
        Table grid = new Table();
        fillRow(grid, 0, 0);
        grid.row();
        fillRow(grid, 1, 0);
        grid.row();
        fillRow(grid, 2, 0);
        add(grid);
    }

    private void buildTechLabPanelForFactory(@Nullable Block attachedFactory, Seq<Sc2ResearchSpec> specs){
        setPanelRows(3);
        Table grid = new Table();
        Team team = player.team();

        int col = 0;
        for(int i = 0; i < specs.size && col < COLS; i++, col++){
            Sc2ResearchSpec spec = specs.get(i);
            if(spec.displayAvailable(team)){
                BuildInfo researchInfo = makeTechLabResearchInfo(spec, spec.hotkey);
                UnitType iconUnit = spec.iconUnit();
                Drawable icon = new TextureRegionDrawable(iconUnit == null ? Blocks.memoryBank.uiIcon : iconUnit.uiIcon);
                Button researchButton = addIconButton(grid, spec.hotkey, icon,
                    () -> spec.canStart(team),
                    () -> tryStartTechLabResearch(attachedFactory, spec));
                researchButton.update(() -> {
                    if(researchButton.isOver()){
                        hoverBuildInfo = researchInfo;
                    }else if(hoverBuildInfo == researchInfo){
                        hoverBuildInfo = null;
                    }
                });
            }else{
                addEmpty(grid);
            }
        }
        for(; col < COLS; col++){
            addEmpty(grid);
        }

        grid.row();
        fillRow(grid, 1, 0);
        grid.row();

        addEmpty(grid);
        addEmpty(grid);
        addEmpty(grid);
        addEmpty(grid);
        if(ResearchQueueService.techLabAnyResearching(team, attachedFactory)){
            BuildInfo cancelInfo = makeTechLabAnyResearchInfo(attachedFactory, "Esc");
            Button cancelButton = addIconButton(grid, "Esc", Icon.cancel, () -> true, () -> cancelTechLabResearch(attachedFactory));
            cancelButton.update(() -> {
                if(cancelButton.isOver()){
                    hoverBuildInfo = cancelInfo;
                }else if(hoverBuildInfo == cancelInfo){
                    hoverBuildInfo = null;
                }
            });
        }else{
            addEmpty(grid);
        }

        add(grid);
    }

    private void buildRadarPanel(){
        setPanelRows(3);
        Table grid = new Table();

        fillRow(grid, 0, 0);
        grid.row();
        fillRow(grid, 1, 0);
        grid.row();

        addEmpty(grid);
        addEmpty(grid);
        addEmpty(grid);
        addIconButton(grid, "v", Icon.cancel, this::anyRadarCanStartRecycle, this::issueRadarRecycle);
        addEmpty(grid);

        add(grid);
    }

    private void buildBunkerPanel(){
        if(activeCommand == CommandMode.RALLY){
            buildCoreRallyPanel();
            return;
        }
        if(activeCommand == CommandMode.BUNKER_ATTACK){
            buildCoreTargetPanel("Attack Target", "Left-click enemy target");
            return;
        }
        if(activeCommand == CommandMode.BUNKER_LOAD){
            buildCoreTargetPanel("Load Units", "Left-click a Barracks unit");
            return;
        }

        setPanelRows(3);
        Table grid = new Table();
        boolean hasUnits = anyBunkerHasGarrison();

        //Row 1: S ... A (shown only when garrisoned)
        if(hasUnits){
            addIconButton(grid, "s", Icon.cancel, () -> true, this::issueBunkerStopAttack);
        }else{
            addEmpty(grid);
        }
        addEmpty(grid);
        addEmpty(grid);
        addEmpty(grid);
        if(hasUnits){
            addIconButton(grid, "a", Icon.warning, () -> true, () -> enterCommandMode(CommandMode.BUNKER_ATTACK));
        }else{
            addEmpty(grid);
        }
        grid.row();

        //Row 2: col5 Y rally for unload target
        addEmpty(grid);
        addEmpty(grid);
        addEmpty(grid);
        addEmpty(grid);
        addIconButton(grid, "y", Icon.commandRally, () -> true, () -> enterCommandMode(CommandMode.RALLY));
        grid.row();

        //Row 3: col2 L, col3 D, col4 V
        addEmpty(grid);
        if(anyBunkerHasSpace()){
            addIconButton(grid, "l", Icon.upload, () -> true, () -> enterCommandMode(CommandMode.BUNKER_LOAD));
        }else{
            addEmpty(grid);
        }
        if(hasUnits){
            addIconButton(grid, "d", Icon.download, () -> true, this::issueBunkerUnloadAll);
        }else{
            addEmpty(grid);
        }
        addIconButton(grid, "v", Icon.cancel, this::anyBunkerCanStartRecycle, this::issueBunkerRecycle);
        addEmpty(grid);

        add(grid);
    }

    private boolean supplyAnyClosed(){
        for(Building build : abilityBuildings()){
            if(build instanceof Door.DoorBuild door && isSupplyDoor(build) && !door.open){
                return true;
            }
        }
        return false;
    }

    private void toggleSupplyDoors(){
        boolean open = supplyAnyClosed();
        for(Building build : abilityBuildings()){
            if(build instanceof Door.DoorBuild door && isSupplyDoor(build)){
                door.configure(open);
            }
        }
    }

    private void buildCorePanel(CoreBuild core){
        if(core.isUpgrading()){
            activeCommand = CommandMode.NONE;
            buildCoreUpgradePanel(core);
            return;
        }
        if(activeCommand == CommandMode.RALLY){
            buildCoreRallyPanel();
            return;
        }
        if(activeCommand == CommandMode.DROP_PULSAR){
            buildCoreTargetPanel("Drop Miner", "Left-click ground");
            return;
        }
        if(activeCommand == CommandMode.EXTRA_SUPPLY){
            buildCoreTargetPanel("Extra Supply", "Left-click a supply depot");
            return;
        }
        if(activeCommand == CommandMode.SCAN){
            buildCoreTargetPanel("Scan", "Left-click to scan area");
            return;
        }
        buildCoreMainPanel(core);
    }

    private void buildCoreUpgradePanel(CoreBuild core){
        setPanelRows(3);
        buildEmptyPanel();
    }

    private void buildCoreMainPanel(CoreBuild core){
        setPanelRows(3);
        Table grid = new Table();
        boolean orbital = core.block == Blocks.coreOrbital;

        //Row 1
        addIconButton(grid, "s", new TextureRegionDrawable(UnitTypes.nova.uiIcon), () -> core.canQueueUnit(UnitTypes.nova), () -> {
            queueCoreUnit(core);
            corePanel = CorePanel.BUILD;
        });
        addEmpty(grid);
        addEmpty(grid);
        Button orbitalButton = addHoverableIconButton(grid, "b", new TextureRegionDrawable(Blocks.coreOrbital.uiIcon), () -> core.canStartOrbitalUpgrade(), () -> {
            if(core.block != Blocks.coreNucleus){
                ui.hudfrag.setHudText("Already upgraded");
            }else if(core.unitQueue != null && !core.unitQueue.isEmpty()){
                ui.hudfrag.setHudText("Cannot upgrade while training");
            }else if(core.isUpgrading()){
                ui.hudfrag.setHudText("Upgrade already in progress");
            }else if(!startCoreOrbitalUpgrade(core)){
                ui.hudfrag.setHudText("Not enough crystals");
            }
        });
        BuildInfo orbitalInfo = makeOrbitalUpgradeInfo(core, "b");
        orbitalButton.update(() -> {
            if(orbitalButton.isOver()){
                hoverBuildInfo = orbitalInfo;
            }else if(hoverBuildInfo == orbitalInfo){
                hoverBuildInfo = null;
            }
        });
        Drawable fortressIcon = Blocks.corePlanetaryFortress == null || Blocks.corePlanetaryFortress.uiIcon == null ? Icon.warning : new TextureRegionDrawable(Blocks.corePlanetaryFortress.uiIcon);
        Button fortressButton = addIconButton(grid, "p", fortressIcon, () -> core.canStartFortressUpgrade(), () -> {
            if(core.block != Blocks.coreNucleus){
                ui.hudfrag.setHudText("Already upgraded");
            }else if(core.unitQueue != null && !core.unitQueue.isEmpty()){
                ui.hudfrag.setHudText("Cannot upgrade while training");
            }else if(core.isUpgrading()){
                ui.hudfrag.setHudText("Upgrade already in progress");
            }else if(!core.hasEngineeringStation()){
                ui.hudfrag.setHudText("Requires Engineering Station");
            }else if(!startCoreFortressUpgrade(core)){
                ui.hudfrag.setHudText("Not enough crystals or gas");
            }
        });
        BuildInfo fortressInfo = makeFortressUpgradeInfo(core, "p");
        fortressButton.update(() -> {
            if(fortressButton.isOver()){
                hoverBuildInfo = fortressInfo;
            }else if(hoverBuildInfo == fortressInfo){
                hoverBuildInfo = null;
            }
        });
        grid.row();

        //Row 2
        addEmpty(grid);
        addEmpty(grid);
        addEmpty(grid);
        addEmpty(grid);
        addIconButton(grid, "y", Icon.commandRally, () -> true, () -> enterCommandMode(CommandMode.RALLY));
        grid.row();

        //Row 3
        if(orbital){
            Boolp energyAvailable = () -> anySelectedOrbitalHasEnergy(CoreBlock.orbitalAbilityCost);
            addIconButton(grid, "e", new TextureRegionDrawable(UnitTypes.pulsar.uiIcon), energyAvailable, () -> enterCommandMode(CommandMode.DROP_PULSAR));
            addIconButton(grid, "x", Icon.add, energyAvailable, () -> enterCommandMode(CommandMode.EXTRA_SUPPLY));
            addIconButton(grid, "c", Icon.zoom, energyAvailable, () -> enterCommandMode(CommandMode.SCAN));
            if(core.hasStoredScvs()){
                addIconButton(grid, "d", Icon.download, () -> true, () -> unloadCoreScvs(core));
            }else{
                addEmpty(grid);
            }
            addIconButton(grid, "l", Icon.export, () -> core.canLift(), () -> queueCoreLift(core));
        }else{
            addEmpty(grid);
            if(core.hasStoredScvs()){
                addIconButton(grid, "d", Icon.download, () -> true, () -> unloadCoreScvs(core));
            }else{
                addIconButton(grid, "o", Icon.upload, () -> true, () -> {
                    if(!requestCoreLoadScvs(core)){
                        ui.hudfrag.setHudText("No available SCVs or storage full");
                    }
                });
            }
            addEmpty(grid);
            addEmpty(grid);
            addIconButton(grid, "l", Icon.export, () -> core.canLift(), () -> queueCoreLift(core));
        }

        add(grid);
    }

    private void buildCoreFlyerPanel(){
        setPanelRows(3);
        if(activeCommand == CommandMode.RALLY){
            buildCoreRallyPanel();
            return;
        }
        if(activeCommand == CommandMode.LAND){
            buildCoreTargetPanel("Land", "Left-click to land");
            return;
        }

        Table grid = new Table();

        //Row 1: M/S/H/P
        addIconButton(grid, "m", Icon.move, () -> true, () -> enterCommandMode(CommandMode.MOVE));
        addIconButton(grid, "s", Icon.cancel, () -> true, this::executeStopCommand);
        addIconButton(grid, "h", Icon.pause, () -> true, this::executeHoldCommand);
        addIconButton(grid, "p", Icon.refresh, () -> true, () -> enterCommandMode(CommandMode.PATROL));
        addEmpty(grid);
        grid.row();

        //Row 2
        addEmpty(grid);
        addEmpty(grid);
        addEmpty(grid);
        addEmpty(grid);
        addIconButton(grid, "y", Icon.commandRally, () -> true, () -> enterCommandMode(CommandMode.RALLY));
        grid.row();

        //Row 3
        addEmpty(grid);
        addEmpty(grid);
        addEmpty(grid);
        addIconButton(grid, "l", Icon.export, () -> true, () -> enterCommandMode(CommandMode.LAND));
        addEmpty(grid);

        add(grid);
    }

    private void buildCoreRallyPanel(){
        setPanelRows(3);
        Table grid = new Table();
        for(int i = 0; i < ROWS * COLS; i++){
            if(i == 14){
                addIconButton(grid, "Esc", Icon.left, () -> true, this::exitCommandMode, targetHintInfo);
            }else{
                addEmpty(grid);
            }
            if((i + 1) % COLS == 0){
                grid.row();
            }
        }
        add(grid);
    }

    private void buildCoreTargetPanel(String title, String hint){
        setPanelRows(3);
        targetHintInfo.description = hint + "\nRight-click return";
        Table grid = new Table();
        for(int i = 0; i < ROWS * COLS; i++){
            if(i == 14){
                addIconButton(grid, "Esc", Icon.left, () -> true, this::exitCommandMode, targetHintInfo);
            }else{
                addEmpty(grid);
            }
            if((i + 1) % COLS == 0){
                grid.row();
            }
        }
        add(grid);
    }

    @Override
    public void draw(){
        super.draw();
        drawHoverInfoBox();
    }

    private @Nullable AbilityInfo currentHoverInfo(){
        AbilityInfo info = null;
        if(hoverBuildInfo != null){
            BuildInfo build = hoverBuildInfo;
            AbilityInfo next = new AbilityInfo();
            next.key = build.key == null ? "" : build.key;
            next.name = build.name == null ? "Action" : build.name;
            next.description = build.description == null ? "" : build.description;
            next.crystalCost = build.crystalCost;
            next.gasCost = build.gasCost;
            next.timeSeconds = build.timeSeconds;
            next.population = build.population;
            next.action = build.action == null ? "" : build.action;
            next.unit = build.unit;
            next.block = build.block;
            info = next;
        }else{
            info = hoverAbilityInfo;
        }

        if(info == null && activeCommand != CommandMode.NONE && hasMouse()){
            return targetHintInfo;
        }
        return info;
    }

    private void drawHoverInfoBox(){
        AbilityInfo info = currentHoverInfo();
        if(info == null) return;

        StringBuilder text = new StringBuilder();
        if(info.hintOnly){
            text.append("Left-click select target\nRight-click return");
        }else{
            String titleName = info.name == null ? "Action" : info.name;
            String action = info.action == null ? "" : info.action;
            String title = action.isEmpty() ? titleName : (action + " " + titleName);
            if(info.key != null && !info.key.isEmpty()){
                title += " (" + info.key + ")";
            }
            text.append(title);

            boolean hasCost = info.crystalCost >= 0 || info.gasCost > 0 || info.population > 0;
            if(hasCost){
                text.append("\nCost: ");
                boolean wrote = false;
                if(info.crystalCost >= 0){
                    text.append(Math.max(info.crystalCost, 0)).append(" crystal");
                    wrote = true;
                }
                if(info.gasCost > 0){
                    if(wrote) text.append(" / ");
                    text.append(info.gasCost).append(" gas");
                    wrote = true;
                }
                if(info.population > 0){
                    if(wrote) text.append(" / ");
                    text.append("Pop ").append(info.population);
                    wrote = true;
                }
                if(!wrote){
                    text.append("-");
                }
            }

            if(info.description != null && !info.description.isEmpty()){
                text.append("\n").append(info.description);
            }

            String targetText = unitTargetText(info);
            if(info.timeSeconds >= 0 || targetText != null){
                text.append("\n");
                boolean wrote = false;
                if(info.timeSeconds >= 0){
                    text.append("Time ").append(info.timeSeconds).append("s");
                    wrote = true;
                }
                if(targetText != null){
                    if(wrote) text.append(" / ");
                    text.append(targetText);
                }
            }
        }

        Font font = Fonts.outline;
        boolean prevInts = font.usesIntegerPositions();
        font.setUseIntegerPositions(false);

        float pad = 6f;
        float maxTextWidth = Math.max(10f, width - pad * 2f);
        hoverInfoLayout.setText(font, text, Color.white, maxTextWidth, Align.left, true);

        float boxW = width;
        float boxH = hoverInfoLayout.height + pad * 2f;
        float px = x;
        float py = y + height - boxH;

        Draw.color(0.07f, 0.08f, 0.10f, 0.95f);
        Fill.rect(px + boxW / 2f, py + boxH / 2f, boxW, boxH);
        Draw.color(abilityBorderColor);
        Lines.stroke(1.5f);
        Lines.rect(px, py, boxW, boxH);
        Draw.reset();

        font.setColor(Color.white);
        font.draw(text, px + pad, py + boxH - pad, boxW - pad * 2f, Align.left, true);
        font.setUseIntegerPositions(prevInts);
        Draw.reset();
    }

    private @Nullable String unitTargetText(AbilityInfo info){
        if(info == null || info.unit == null) return null;
        if(info.action == null || !info.action.equals("Train")) return null;
        boolean air = info.unit.targetAir;
        boolean ground = info.unit.targetGround;
        if(air && ground) return "Targets: ground/air";
        if(ground) return "Targets: ground";
        if(air) return "Targets: air";
        return "Targets: none";
    }

    private Element borderElement(){
        return borderElement(null);
    }

    private Element borderElement(@Nullable Boolp autoEnabled){
        return new Element(){
            @Override
            public void draw(){
                Draw.color(abilityBorderColor);
                Lines.stroke(1.5f);
                float inset = 1.5f;
                float innerInset = 4.5f;
                Lines.rect(x + inset, y + inset, width - inset * 2f, height - inset * 2f);
                Lines.rect(x + innerInset, y + innerInset, width - innerInset * 2f, height - innerInset * 2f);

                if(autoEnabled != null && autoEnabled.get()){
                    float outerX = x + inset;
                    float outerY = y + inset;
                    float outerW = width - inset * 2f;
                    float outerH = height - inset * 2f;
                    float perimeter = Math.max(1f, (outerW + outerH) * 2f);
                    float distance = (Time.time * 1.8f) % perimeter;

                    Draw.color(Color.valueOf("ffd84a"));
                    drawPerimeterDot(outerX, outerY, outerW, outerH, distance, 2.4f);
                    drawPerimeterDot(outerX, outerY, outerW, outerH, (distance + perimeter * 0.5f) % perimeter, 2.4f);
                }

                Draw.reset();
            }
        };
    }

    private AbilityInfo makeAbilityInfo(String key, String name, String description){
        AbilityInfo info = new AbilityInfo();
        info.key = key == null ? "" : key;
        info.name = name == null ? "Ability" : name;
        info.description = description == null ? "" : description;
        info.action = "Ability";
        return info;
    }

    private AbilityInfo defaultAbilityInfo(String key){
        String name = key == null || key.isEmpty() ? "Ability" : ("Ability " + key);
        return makeAbilityInfo(key, name, "Left-click acts as hotkey.");
    }

    private void bindAbilityHover(Button button, @Nullable AbilityInfo info){
        if(button == null || info == null) return;
        button.update(() -> {
            if(button.isOver()){
                hoverAbilityInfo = info;
            }else if(hoverAbilityInfo == info){
                hoverAbilityInfo = null;
            }
        });
    }

    private Button addIconButton(Table grid, String key, Drawable icon, Boolp enabled, Runnable action){
        return addIconButton(grid, key, icon, enabled, action, null, null, null);
    }

    private Button addIconButton(Table grid, String key, Drawable icon, Boolp enabled, Runnable action, @Nullable AbilityInfo info){
        return addIconButton(grid, key, icon, enabled, action, null, null, info);
    }

    private Button addAutoCastIconButton(Table grid, String key, Drawable icon, Boolp enabled, Runnable action, Boolp autoEnabled, Runnable toggleAuto){
        return addIconButton(grid, key, icon, enabled, action, autoEnabled, toggleAuto, null);
    }

    private Button addCountedIconButton(Table grid, String key, Drawable icon, Boolp enabled, Runnable action, Intp count){
        Boolp allowed = enabled == null ? () -> true : enabled;
        Button button = new Button(Styles.clearNoneTogglei);
        button.clicked(() -> {
            if(allowed.get()) action.run();
        });
        button.update(() -> button.setDisabled(!allowed.get()));

        Stack stack = new Stack();
        stack.add(borderElement());

        Image image = new Image(icon);
        image.setScaling(Scaling.fit);
        image.update(() -> image.setColor(allowed.get() ? Color.white : Color.gray));

        Table iconTable = new Table();
        iconTable.add(image).size(abilityIconSize);
        stack.add(iconTable);

        if(key != null && !key.isEmpty()){
            Table keyTable = new Table();
            keyTable.top().left();
            Label keyLabel = new Label(key);
            keyLabel.setFontScale(abilityKeyScale);
            keyLabel.update(() -> keyLabel.setColor(allowed.get() ? Color.white : Color.gray));
            keyTable.add(keyLabel).pad(3f);
            stack.add(keyTable);
        }

        if(count != null){
            Table countTable = new Table();
            countTable.bottom().right();
            Label countLabel = new Label("0");
            countLabel.setAlignment(Align.right);
            countLabel.setFontScale(0.62f);
            countLabel.update(() -> {
                int value = Math.max(count.get(), 0);
                countLabel.setText(Integer.toString(value));
                countLabel.setColor(allowed.get() ? Color.white : Color.gray);
            });
            countTable.add(countLabel).padRight(5f).padBottom(3f);
            stack.add(countTable);
        }

        button.add(stack).size(abilityButtonSize);
        grid.add(button).size(abilityButtonSize).pad(2f);
        bindAbilityHover(button, defaultAbilityInfo(key));
        return button;
    }

    private Button addIconButton(Table grid, String key, Drawable icon, Boolp enabled, Runnable action, @Nullable Boolp autoEnabled, @Nullable Runnable toggleAuto){
        return addIconButton(grid, key, icon, enabled, action, autoEnabled, toggleAuto, null);
    }

    private Button addIconButton(Table grid, String key, Drawable icon, Boolp enabled, Runnable action, @Nullable Boolp autoEnabled, @Nullable Runnable toggleAuto, @Nullable AbilityInfo info){
        Boolp allowed = enabled == null ? () -> true : enabled;
        Button button = new Button(Styles.clearNoneTogglei);
        button.clicked(() -> {
            if(allowed.get()) action.run();
        });
        button.update(() -> button.setDisabled(!allowed.get()));
        if(toggleAuto != null){
            button.addListener(new InputListener(){
                @Override
                public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button){
                    if(button == KeyCode.mouseRight){
                        toggleAuto.run();
                        event.stop();
                        return true;
                    }
                    return false;
                }
            });
        }

        Stack stack = new Stack();
        stack.add(borderElement(autoEnabled));

        Image image = new Image(icon);
        image.setScaling(Scaling.fit);
        image.update(() -> image.setColor(allowed.get() ? Color.white : Color.gray));

        Table iconTable = new Table();
        iconTable.add(image).size(abilityIconSize);
        stack.add(iconTable);

        if(key != null && !key.isEmpty()){
            Table keyTable = new Table();
            keyTable.top().left();
            Label keyLabel = new Label(key);
            keyLabel.setFontScale(abilityKeyScale);
            keyLabel.update(() -> keyLabel.setColor(allowed.get() ? Color.white : Color.gray));
            keyTable.add(keyLabel).pad(3f);
            stack.add(keyTable);
        }

        button.add(stack).size(abilityButtonSize);
        grid.add(button).size(abilityButtonSize).pad(2f);
        bindAbilityHover(button, info == null ? defaultAbilityInfo(key) : info);
        return button;
    }

    private Button addBattlecruiserCooldownButton(Table grid, String key, Drawable icon, Boolp enabled, Runnable action, Floatp cooldownValue, Floatp cooldownTotal){
        Boolp allowed = enabled == null ? () -> true : enabled;
        Button button = new Button(Styles.clearNoneTogglei);
        button.clicked(() -> {
            if(allowed.get()) action.run();
        });
        button.update(() -> button.setDisabled(!allowed.get()));

        Stack stack = new Stack();
        stack.add(borderElement());

        Image image = new Image(icon);
        image.setScaling(Scaling.fit);
        image.update(() -> image.setColor(allowed.get() ? Color.white : Color.gray));

        Table iconTable = new Table();
        iconTable.add(image).size(abilityIconSize);
        stack.add(iconTable);

        Table keyTable = new Table();
        keyTable.top().left();
        Label keyLabel = new Label(key);
        keyLabel.setFontScale(abilityKeyScale);
        keyLabel.update(() -> keyLabel.setColor(allowed.get() ? Color.white : Color.gray));
        keyTable.add(keyLabel).pad(3f);
        stack.add(keyTable);

        stack.add(yamatoCooldownOverlay(cooldownValue, cooldownTotal));

        button.add(stack).size(abilityButtonSize);
        grid.add(button).size(abilityButtonSize).pad(2f);
        bindAbilityHover(button, makeAbilityInfo(key, "Skill Cooldown", "Left-click acts as hotkey."));
        return button;
    }

    private Button addCooldownIconButton(Table grid, String key, Drawable icon, Boolp enabled, Runnable action, Floatp cooldownValue, Floatp cooldownTotal, @Nullable AbilityInfo info){
        Boolp allowed = enabled == null ? () -> true : enabled;
        Button button = new Button(Styles.clearNoneTogglei);
        button.clicked(() -> {
            if(allowed.get()) action.run();
        });
        button.update(() -> button.setDisabled(!allowed.get()));

        Stack stack = new Stack();
        stack.add(borderElement());

        Image image = new Image(icon);
        image.setScaling(Scaling.fit);
        image.update(() -> image.setColor(allowed.get() ? Color.white : Color.gray));

        Table iconTable = new Table();
        iconTable.add(image).size(abilityIconSize);
        stack.add(iconTable);

        if(key != null && !key.isEmpty()){
            Table keyTable = new Table();
            keyTable.top().left();
            Label keyLabel = new Label(key);
            keyLabel.setFontScale(abilityKeyScale);
            keyLabel.update(() -> keyLabel.setColor(allowed.get() ? Color.white : Color.gray));
            keyTable.add(keyLabel).pad(3f);
            stack.add(keyTable);
        }

        stack.add(yamatoCooldownOverlay(cooldownValue, cooldownTotal));

        button.add(stack).size(abilityButtonSize);
        grid.add(button).size(abilityButtonSize).pad(2f);
        bindAbilityHover(button, info == null ? defaultAbilityInfo(key) : info);
        return button;
    }

    private Element yamatoCooldownOverlay(@Nullable Floatp cooldownValue, @Nullable Floatp cooldownTotal){
        return new Element(){
            @Override
            public void draw(){
                float cooldown = cooldownValue == null ? 0f : cooldownValue.get();
                if(cooldown <= 0.001f) return;

                float total = cooldownTotal == null ? 1f : Math.max(cooldownTotal.get(), 0.001f);
                float cx = x + width / 2f;
                float cy = y + height / 2f;
                float fixedAngle = 90f;
                float progress = 1f - Mathf.clamp(cooldown / total);
                float movingAngle = fixedAngle - progress * 360f;
                float handLen = width * 0.20f;

                Draw.color(Color.valueOf("b6bcc5"));
                Lines.stroke(1.25f);
                Lines.line(cx, cy, cx + Angles.trnsx(fixedAngle, handLen), cy + Angles.trnsy(fixedAngle, handLen));
                Lines.line(cx, cy, cx + Angles.trnsx(movingAngle, handLen), cy + Angles.trnsy(movingAngle, handLen));
                Draw.reset();
            }
        };
    }

    private void drawPerimeterDot(float x, float y, float width, float height, float distance, float radius){
        float perimeter = Math.max(1f, (width + height) * 2f);
        float d = distance % perimeter;
        float px, py;

        if(d <= width){
            px = x + d;
            py = y + height;
        }else if(d <= width + height){
            px = x + width;
            py = y + height - (d - width);
        }else if(d <= width + height + width){
            px = x + width - (d - width - height);
            py = y;
        }else{
            px = x;
            py = y + (d - width - height - width);
        }

        Fill.circle(px, py, radius);
    }

    private Button addHurricaneLockButton(Table grid){
        Boolp enabled = this::anyHurricaneCanLock;
        Button button = new Button(Styles.clearNoneTogglei);
        button.clicked(() -> {
            if(enabled.get()){
                issueHurricaneLockCommand();
            }
        });
        button.update(() -> button.setDisabled(!enabled.get()));
        button.addListener(new InputListener(){
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button){
                if(button == KeyCode.mouseRight){
                    toggleSelectedHurricaneAutoCast();
                    event.stop();
                    return true;
                }
                return false;
            }
        });

        Stack stack = new Stack();
        stack.add(borderElement(this::selectedHurricaneAutoCastEnabled));

        Image image = new Image(Icon.warning);
        image.setScaling(Scaling.fit);
        image.update(() -> {
            if(anyHurricaneLockActive()){
                image.setColor(Color.valueOf("7a7a7a"));
            }else if(selectedHurricaneLockCooldown() > 0.001f){
                image.setColor(Color.valueOf("3f3f3f"));
            }else{
                image.setColor(Color.white);
            }
        });

        Table iconTable = new Table();
        iconTable.add(image).size(abilityIconSize);
        stack.add(iconTable);

        Table keyTable = new Table();
        keyTable.top().left();
        Label keyLabel = new Label("c");
        keyLabel.setFontScale(abilityKeyScale);
        keyLabel.update(() -> keyLabel.setColor(enabled.get() ? Color.white : Color.gray));
        keyTable.add(keyLabel).pad(3f);
        stack.add(keyTable);

        stack.add(yamatoCooldownOverlay(this::selectedHurricaneLockCooldown, UnitTypes::hurricaneLockCooldownDuration));

        button.add(stack).size(abilityButtonSize);
        grid.add(button).size(abilityButtonSize).pad(2f);
        bindAbilityHover(button, makeAbilityInfo("c", "Hurricane Lock", "Left-click locks target, right-click toggles auto-cast."));
        return button;
    }

    private Button addHoverableIconButton(Table grid, String key, Drawable icon, Boolp enabled, Runnable action){
        Boolp allowed = enabled == null ? () -> true : enabled;
        Button button = new Button(Styles.clearNoneTogglei);
        button.clicked(() -> {
            if(allowed.get()) action.run();
        });

        Stack stack = new Stack();
        stack.add(borderElement());

        Image image = new Image(icon);
        image.setScaling(Scaling.fit);
        image.update(() -> image.setColor(allowed.get() ? Color.white : Color.gray));

        Table iconTable = new Table();
        iconTable.add(image).size(abilityIconSize);
        stack.add(iconTable);

        if(key != null && !key.isEmpty()){
            Table keyTable = new Table();
            keyTable.top().left();
            Label keyLabel = new Label(key);
            keyLabel.setFontScale(abilityKeyScale);
            keyLabel.update(() -> keyLabel.setColor(allowed.get() ? Color.white : Color.gray));
            keyTable.add(keyLabel).pad(3f);
            stack.add(keyTable);
        }

        button.add(stack).size(abilityButtonSize);
        grid.add(button).size(abilityButtonSize).pad(2f);
        bindAbilityHover(button, defaultAbilityInfo(key));
        return button;
    }

    private void addAbilityButton(String key, Drawable icon, Boolp enabled, Runnable action){
        addIconButton(this, key, icon, enabled, action);
    }

    private Button addBuildButton(Table grid, String key, Block block, Boolp enabled, Runnable action){
        Button button = addIconButton(grid, key, new TextureRegionDrawable(block.uiIcon), enabled, action);
        BuildInfo info = makeBuildInfo(block, key);
        button.update(() -> {
            if(button.isOver()){
                hoverBuildInfo = info;
            }else if(hoverBuildInfo == info){
                hoverBuildInfo = null;
            }
        });
        return button;
    }

    private Button addAddonBuildButton(Table grid, String key, String label, Block block, int crystalCost, int gasCost, float buildTime, Boolp enabled, Runnable action){
        Button button = addIconButton(grid, key, new TextureRegionDrawable(block.uiIcon), enabled, action);
        BuildInfo info = makeAddonInfo(block, key, label, crystalCost, gasCost, buildTime);
        button.update(() -> {
            if(button.isOver()){
                hoverBuildInfo = info;
            }else if(hoverBuildInfo == info){
                hoverBuildInfo = null;
            }
        });
        return button;
    }

    private Button addUnitButton(Table grid, String key, UnitFactory.UnitPlan plan, Boolp enabled, Runnable action){
        Button button = addIconButton(grid, key, new TextureRegionDrawable(plan.unit.uiIcon), enabled, action);
        BuildInfo info = makeUnitInfo(plan, key);
        button.update(() -> {
            if(button.isOver()){
                hoverBuildInfo = info;
            }else if(hoverBuildInfo == info){
                hoverBuildInfo = null;
            }
        });
        return button;
    }

    private Table buildBuildInfoTable(){
        Table info = new Table();
        info.background(Styles.black6);
        info.visible(() -> hoverBuildInfo != null);
        info.update(() -> info.touchable = hoverBuildInfo != null ? Touchable.enabled : Touchable.disabled);
        info.defaults().pad(2f).left();

        info.label(() -> {
            if(hoverBuildInfo == null) return "";
            return "Build " + hoverBuildInfo.name + " (" + hoverBuildInfo.key + ")";
        }).left().row();

        info.table(t -> {
            t.left();
            Image crystalIcon = new Image(Items.graphite.uiIcon);
            t.add(crystalIcon).size(16f).padRight(4f);
            t.label(() -> {
                if(hoverBuildInfo == null) return "";
                return Integer.toString(hoverBuildInfo.crystalCost);
            }).padRight(8f);
            Image gasIcon = new Image(Items.highEnergyGas.uiIcon);
            gasIcon.visible(() -> hoverBuildInfo != null && hoverBuildInfo.gasCost > 0);
            t.add(gasIcon).size(16f).padRight(4f);
            t.label(() -> {
                if(hoverBuildInfo == null || hoverBuildInfo.gasCost <= 0) return "";
                return Integer.toString(hoverBuildInfo.gasCost);
            });
        }).left().row();

        info.label(() -> {
            if(hoverBuildInfo == null) return "";
            return "Time " + hoverBuildInfo.timeSeconds + "s";
        }).left();

        Boolp showProgress = () -> hoverBuildInfo != null && hoverBuildInfo.progress != null
        && (hoverBuildInfo.progressVisible == null || hoverBuildInfo.progressVisible.get());

        Table progressTable = new Table(){
            @Override
            public float getPrefWidth(){
                return showProgress.get() ? super.getPrefWidth() : 0f;
            }

            @Override
            public float getPrefHeight(){
                return showProgress.get() ? super.getPrefHeight() : 0f;
            }
        };
        progressTable.visible(showProgress);

        Image progressIcon = new Image();
        progressIcon.visible(() -> hoverBuildInfo != null && hoverBuildInfo.progressIcon != null && showProgress.get());
        progressIcon.update(() -> {
            if(hoverBuildInfo == null || hoverBuildInfo.progressIcon == null) return;
            progressIcon.setDrawable(hoverBuildInfo.progressIcon);
            float alpha = hoverBuildInfo.progress == null ? 0f : Mathf.clamp(hoverBuildInfo.progress.get());
            progressIcon.setColor(1f, 1f, 1f, alpha);
        });
        progressTable.add(progressIcon).size(40f).left().row();

        Bar progressBar = new Bar(
            () -> "",
            () -> hoverBuildInfo == null || hoverBuildInfo.progressColor == null ? Color.cyan : hoverBuildInfo.progressColor,
            () -> {
                if(hoverBuildInfo == null || hoverBuildInfo.progress == null) return 0f;
                return Mathf.clamp(hoverBuildInfo.progress.get());
            }
        );
        progressBar.visible(showProgress);
        progressTable.add(progressBar).growX().height(8f).left();

        info.row();
        info.add(progressTable).growX().left();

        return info;
    }

    private BuildInfo makeAddonInfo(Block block, String key, String name, int crystalCost, int gasCost, float buildTime){
        BuildInfo info = new BuildInfo();
        info.block = block;
        info.key = key;
        info.name = name;
        info.action = "Build";
        info.crystalCost = crystalCost;
        info.gasCost = gasCost;
        info.timeSeconds = Math.round(buildTime / 60f);
        return info;
    }

    private BuildInfo makeUpgradeInfo(Block block, String key, int crystalCost, int gasCost, float buildTime, Floatp progress, Boolp progressVisible){
        BuildInfo info = new BuildInfo();
        info.block = block;
        info.key = key;
        info.name = sc2Name(block);
        info.action = "Research";
        info.crystalCost = crystalCost;
        info.gasCost = gasCost;
        info.timeSeconds = Math.round(buildTime / 60f);
        info.progress = progress;
        info.progressVisible = progressVisible;
        info.progressIcon = new TextureRegionDrawable(block.uiIcon);
        info.progressColor = Color.cyan;
        return info;
    }

    private BuildInfo makeOrbitalUpgradeInfo(CoreBuild core, String key){
        return makeUpgradeInfo(Blocks.coreOrbital, key, CoreBlock.orbitalUpgradeCost, 0, CoreBlock.orbitalUpgradeTime, core::orbitalUpgradeFraction, core::isUpgradingOrbital);
    }

    private BuildInfo makeFortressUpgradeInfo(CoreBuild core, String key){
        return makeUpgradeInfo(Blocks.corePlanetaryFortress, key, CoreBlock.fortressUpgradeCost, CoreBlock.fortressUpgradeGasCost, CoreBlock.fortressUpgradeTime, core::fortressUpgradeFraction, core::isUpgradingFortress);
    }

    private @Nullable Block findCatalogIconBlock(Sc2ResearchSpec active, Seq<Sc2ResearchSpec> specs, Block[] icons){
        for(int i = 0; i < specs.size; i++){
            if(specs.get(i) == active){
                return i < icons.length ? icons[i] : null;
            }
        }
        return null;
    }

    private BuildInfo makeArmoryAnyResearchInfo(String key, Seq<Sc2ResearchSpec> specs, Block[] icons){
        BuildInfo info = new BuildInfo();
        info.block = Blocks.siliconCrucible;
        info.key = key;
        info.action = "Research";
        info.progressColor = Color.cyan;

        Team team = player.team();
        Sc2ResearchSpec active = ResearchQueueService.armoryActiveResearch(team);
        Block iconBlock = active == null ? Blocks.siliconCrucible : findCatalogIconBlock(active, specs, icons);
        if(iconBlock == null) iconBlock = Blocks.siliconCrucible;

        if(active != null){
            info.name = active.name(team);
            info.crystalCost = active.crystalCost(team);
            info.gasCost = active.gasCost(team);
            info.timeSeconds = Math.round(active.duration(team) / 60f);
            info.progressIcon = new TextureRegionDrawable(iconBlock.uiIcon);
        }else{
            info.name = "Armory Upgrade";
            info.crystalCost = 0;
            info.gasCost = 0;
            info.timeSeconds = 0;
            info.progressIcon = new TextureRegionDrawable(Blocks.siliconCrucible.uiIcon);
        }

        info.progress = () -> {
            Sc2ResearchSpec spec = ResearchQueueService.armoryActiveResearch(player.team());
            return spec == null ? 0f : spec.progress(player.team());
        };
        info.progressVisible = () -> ResearchQueueService.armoryAnyResearching(player.team());
        return info;
    }

    private BuildInfo makeInfantryAnyResearchInfo(String key, Seq<Sc2ResearchSpec> specs, Block[] icons){
        BuildInfo info = new BuildInfo();
        info.block = Blocks.multiPress;
        info.key = key;
        info.action = "Research";
        info.progressColor = Color.cyan;

        Team team = player.team();
        Sc2ResearchSpec active = ResearchQueueService.engineeringActiveResearch(team);
        Block iconBlock = active == null ? Blocks.multiPress : findCatalogIconBlock(active, specs, icons);
        if(iconBlock == null) iconBlock = Blocks.multiPress;

        if(active != null){
            info.name = active.name(team);
            info.crystalCost = active.crystalCost(team);
            info.gasCost = active.gasCost(team);
            info.timeSeconds = Math.round(active.duration(team) / 60f);
            info.progressIcon = new TextureRegionDrawable(iconBlock.uiIcon);
        }else{
            info.name = "Engineering Upgrade";
            info.crystalCost = 0;
            info.gasCost = 0;
            info.timeSeconds = 0;
            info.progressIcon = new TextureRegionDrawable(Blocks.multiPress.uiIcon);
        }

        info.progress = () -> {
            Sc2ResearchSpec spec = ResearchQueueService.engineeringActiveResearch(player.team());
            return spec == null ? 0f : spec.progress(player.team());
        };
        info.progressVisible = () -> ResearchQueueService.engineeringAnyResearching(player.team());
        return info;
    }

    private BuildInfo makeInfantryWeaponUpgradeInfo(String key, int level){
        BuildInfo info = new BuildInfo();
        info.block = Blocks.multiPress;
        info.key = key;
        info.action = "Research";
        info.name = "Upgrade";
        info.crystalCost = UnitTypes.infantryWeaponCrystalCost(level);
        info.gasCost = UnitTypes.infantryWeaponGasCost(level);
        info.timeSeconds = Math.round(UnitTypes.infantryWeaponResearchDuration(level) / 60f);
        return info;
    }

    private BuildInfo makeInfantryWeaponResearchInfo(String key){
        int level = UnitTypes.infantryWeaponResearchingLevel(player.team());
        BuildInfo info = new BuildInfo();
        info.block = Blocks.multiPress;
        info.key = key;
        info.action = "Research";
        info.name = level > 0 ? "Infantry Weapons Lv." + level : "Infantry Weapons";
        info.crystalCost = level > 0 ? UnitTypes.infantryWeaponCrystalCost(level) : 0;
        info.gasCost = level > 0 ? UnitTypes.infantryWeaponGasCost(level) : 0;
        info.timeSeconds = level > 0 ? Math.round(UnitTypes.infantryWeaponResearchDuration(level) / 60f) : 0;
        info.progress = () -> UnitTypes.infantryWeaponResearchProgress(player.team());
        info.progressVisible = () -> UnitTypes.infantryWeaponResearching(player.team());
        info.progressColor = Color.cyan;
        info.progressIcon = new TextureRegionDrawable(Blocks.multiPress.uiIcon);
        return info;
    }

    private BuildInfo makeInfantryArmorUpgradeInfo(String key, int level){
        BuildInfo info = new BuildInfo();
        info.block = Blocks.multiPress;
        info.key = key;
        info.action = "Research";
        info.name = "Upgrade";
        info.crystalCost = UnitTypes.infantryWeaponCrystalCost(level);
        info.gasCost = UnitTypes.infantryWeaponGasCost(level);
        info.timeSeconds = Math.round(UnitTypes.infantryWeaponResearchDuration(level) / 60f);
        return info;
    }

    private BuildInfo makeInstantTrackingUpgradeInfo(String key){
        BuildInfo info = new BuildInfo();
        info.block = Blocks.swarmer;
        info.key = key;
        info.action = "Research";
        info.name = "Instant Tracking";
        info.crystalCost = UnitTypes.instantTrackingCrystalCost();
        info.gasCost = UnitTypes.instantTrackingGasCost();
        info.timeSeconds = Math.round(UnitTypes.instantTrackingResearchDuration() / 60f);
        return info;
    }

    private BuildInfo makeSteelArmorUpgradeInfo(String key){
        BuildInfo info = new BuildInfo();
        info.block = Blocks.atmosphericConcentrator;
        info.key = key;
        info.action = "Research";
        info.name = "Steel Armor";
        info.crystalCost = UnitTypes.steelArmorCrystalCost();
        info.gasCost = UnitTypes.steelArmorGasCost();
        info.timeSeconds = Math.round(UnitTypes.steelArmorResearchDuration() / 60f);
        return info;
    }

    private BuildInfo makeGhostCamoUpgradeInfo(String key){
        BuildInfo info = new BuildInfo();
        info.block = Blocks.launchPad;
        info.key = key;
        info.action = "Research";
        info.name = "Ghost Camouflage";
        info.crystalCost = UnitTypes.ghostCamoCrystalCost();
        info.gasCost = UnitTypes.ghostCamoGasCost();
        info.timeSeconds = Math.round(UnitTypes.ghostCamoResearchDuration() / 60f);
        return info;
    }

    private BuildInfo makeGhostCamoResearchInfo(String key){
        BuildInfo info = new BuildInfo();
        info.block = Blocks.launchPad;
        info.key = key;
        info.action = "Research";
        info.name = "Ghost Camouflage";
        info.crystalCost = UnitTypes.ghostCamoCrystalCost();
        info.gasCost = UnitTypes.ghostCamoGasCost();
        info.timeSeconds = Math.round(UnitTypes.ghostCamoResearchDuration() / 60f);
        info.progress = () -> UnitTypes.ghostCamoResearchProgress(player.team());
        info.progressVisible = () -> UnitTypes.ghostCamoAnyResearching(player.team());
        info.progressColor = Color.cyan;
        info.progressIcon = new TextureRegionDrawable(Blocks.launchPad.uiIcon);
        return info;
    }

    private BuildInfo makeGhostWarheadBuildInfo(String key){
        BuildInfo info = new BuildInfo();
        info.block = Blocks.launchPad;
        info.key = key;
        info.action = "Deploy";
        info.name = "Tactical Nuke";
        info.crystalCost = UnitTypes.ghostWarheadCrystalCost();
        info.gasCost = UnitTypes.ghostWarheadGasCost();
        info.timeSeconds = Math.round(UnitTypes.ghostWarheadBuildDuration() / 60f);
        info.progress = this::selectedGhostWarheadBuildProgress;
        info.progressVisible = this::anyGhostAcademyProducingWarhead;
        info.progressColor = Color.cyan;
        info.progressIcon = new TextureRegionDrawable(Blocks.launchPad.uiIcon);
        return info;
    }

    /* Legacy hardcoded tech-lab build info helpers retained for migration reference.
    private BuildInfo makeBarracksBlastShieldInfo(String key){
        BuildInfo info = new BuildInfo();
        info.block = Blocks.memoryBank;
        info.key = key;
        info.name = "闂傚啳灏欓崹搴ㄥ箮閵堝洦绂?;
        info.crystalCost = UnitTypes.barracksBlastShieldCrystalCost();
        info.gasCost = UnitTypes.barracksBlastShieldGasCost();
        info.timeSeconds = Math.round(UnitTypes.barracksBlastShieldResearchDuration() / 60f);
        info.progress = () -> UnitTypes.barracksBlastShieldResearchProgress(player.team());
        info.progressVisible = () -> UnitTypes.barracksBlastShieldResearching(player.team());
        info.progressColor = Color.cyan;
        info.progressIcon = new TextureRegionDrawable(UnitTypes.dagger.uiIcon);
        return info;
    }

    private BuildInfo makeBarracksStimpackResearchInfo(String key){
        BuildInfo info = new BuildInfo();
        info.block = Blocks.memoryBank;
        info.key = key;
        info.name = "鐎殿喖鎼€垫煡宕?;
        info.crystalCost = UnitTypes.barracksStimpackCrystalCost();
        info.gasCost = UnitTypes.barracksStimpackGasCost();
        info.timeSeconds = Math.round(UnitTypes.barracksStimpackResearchDuration() / 60f);
        info.progress = () -> UnitTypes.barracksStimpackResearchProgress(player.team());
        info.progressVisible = () -> UnitTypes.barracksStimpackResearching(player.team());
        info.progressColor = Color.cyan;
        info.progressIcon = new TextureRegionDrawable(UnitTypes.fortress.uiIcon);
        return info;
    }

    private BuildInfo makeBarracksConcussiveResearchInfo(String key){
        BuildInfo info = new BuildInfo();
        info.block = Blocks.memoryBank;
        info.key = key;
        info.name = "闂傚洤娲╁畷鍗烆嚕?;
        info.crystalCost = UnitTypes.barracksConcussiveCrystalCost();
        info.gasCost = UnitTypes.barracksConcussiveGasCost();
        info.timeSeconds = Math.round(UnitTypes.barracksConcussiveResearchDuration() / 60f);
        info.progress = () -> UnitTypes.barracksConcussiveResearchProgress(player.team());
        info.progressVisible = () -> UnitTypes.barracksConcussiveResearching(player.team());
        info.progressColor = Color.cyan;
        info.progressIcon = new TextureRegionDrawable(UnitTypes.fortress.uiIcon);
        return info;
    }

    private BuildInfo makeBarracksAnyResearchInfo(String key){
        BuildInfo info = new BuildInfo();
        info.block = Blocks.memoryBank;
        info.key = key;
        info.progressColor = Color.cyan;

        if(UnitTypes.barracksBlastShieldResearching(player.team())){
            info.name = "闂傚啳灏欓崹搴ㄥ箮閵堝洦绂?;
            info.crystalCost = UnitTypes.barracksBlastShieldCrystalCost();
            info.gasCost = UnitTypes.barracksBlastShieldGasCost();
            info.timeSeconds = Math.round(UnitTypes.barracksBlastShieldResearchDuration() / 60f);
            info.progressIcon = new TextureRegionDrawable(UnitTypes.dagger.uiIcon);
        }else if(UnitTypes.barracksStimpackResearching(player.team())){
            info.name = "鐎殿喖鎼€垫煡宕?;
            info.crystalCost = UnitTypes.barracksStimpackCrystalCost();
            info.gasCost = UnitTypes.barracksStimpackGasCost();
            info.timeSeconds = Math.round(UnitTypes.barracksStimpackResearchDuration() / 60f);
            info.progressIcon = new TextureRegionDrawable(UnitTypes.fortress.uiIcon);
        }else if(UnitTypes.barracksConcussiveResearching(player.team())){
            info.name = "闂傚洤娲╁畷鍗烆嚕?;
            info.crystalCost = UnitTypes.barracksConcussiveCrystalCost();
            info.gasCost = UnitTypes.barracksConcussiveGasCost();
            info.timeSeconds = Math.round(UnitTypes.barracksConcussiveResearchDuration() / 60f);
            info.progressIcon = new TextureRegionDrawable(UnitTypes.fortress.uiIcon);
        }else{
            info.name = "Barracks Tech";
            info.crystalCost = 0;
            info.gasCost = 0;
            info.timeSeconds = 0;
            info.progressIcon = new TextureRegionDrawable(Blocks.memoryBank.uiIcon);
        }

        info.progress = () -> {
            if(UnitTypes.barracksBlastShieldResearching(player.team())) return UnitTypes.barracksBlastShieldResearchProgress(player.team());
            if(UnitTypes.barracksStimpackResearching(player.team())) return UnitTypes.barracksStimpackResearchProgress(player.team());
            if(UnitTypes.barracksConcussiveResearching(player.team())) return UnitTypes.barracksConcussiveResearchProgress(player.team());
            return 0f;
        };
        info.progressVisible = () -> UnitTypes.barracksTechAnyResearching(player.team());
        return info;
    }

    private BuildInfo makeInfernoPreheaterResearchInfo(String key){
        BuildInfo info = new BuildInfo();
        info.block = Blocks.memoryBank;
        info.key = key;
        info.action = "Research";
        info.name = "Inferno Pre-Igniter";
        info.crystalCost = UnitTypes.infernoPreheaterCrystalCost();
        info.gasCost = UnitTypes.infernoPreheaterGasCost();
        info.timeSeconds = Math.round(UnitTypes.infernoPreheaterResearchDuration() / 60f);
        info.progress = () -> UnitTypes.infernoPreheaterResearchProgress(player.team());
        info.progressVisible = () -> UnitTypes.infernoPreheaterResearching(player.team());
        info.progressColor = Color.cyan;
        info.progressIcon = new TextureRegionDrawable(UnitTypes.locus.uiIcon);
        return info;
    }

    private BuildInfo makeElectromagneticFieldAcceleratorResearchInfo(String key){
        BuildInfo info = new BuildInfo();
        info.block = Blocks.memoryBank;
        info.key = key;
        info.action = "Research";
        info.name = "Electromagnetic Field Accelerator";
        info.crystalCost = UnitTypes.electromagneticFieldAcceleratorCrystalCost();
        info.gasCost = UnitTypes.electromagneticFieldAcceleratorGasCost();
        info.timeSeconds = Math.round(UnitTypes.electromagneticFieldAcceleratorResearchDuration() / 60f);
        info.progress = () -> UnitTypes.electromagneticFieldAcceleratorResearchProgress(player.team());
        info.progressVisible = () -> UnitTypes.electromagneticFieldAcceleratorResearching(player.team());
        info.progressColor = Color.cyan;
        info.progressIcon = new TextureRegionDrawable(UnitTypes.hurricane.uiIcon);
        return info;
    }

    private BuildInfo makeDrillClawResearchInfo(String key){
        BuildInfo info = new BuildInfo();
        info.block = Blocks.memoryBank;
        info.key = key;
        info.action = "Research";
        info.name = "Drilling Claws";
        info.crystalCost = UnitTypes.drillClawCrystalCost();
        info.gasCost = UnitTypes.drillClawGasCost();
        info.timeSeconds = Math.round(UnitTypes.drillClawResearchDuration() / 60f);
        info.progress = () -> UnitTypes.drillClawResearchProgress(player.team());
        info.progressVisible = () -> UnitTypes.drillClawResearching(player.team());
        info.progressColor = Color.cyan;
        info.progressIcon = new TextureRegionDrawable(UnitTypes.crawler.uiIcon);
        return info;
    }

    private BuildInfo makeSmartServosResearchInfo(String key){
        BuildInfo info = new BuildInfo();
        info.block = Blocks.memoryBank;
        info.key = key;
        info.action = "Research";
        info.name = "Smart Servos";
        info.crystalCost = UnitTypes.smartServosCrystalCost();
        info.gasCost = UnitTypes.smartServosGasCost();
        info.timeSeconds = Math.round(UnitTypes.smartServosResearchDuration() / 60f);
        info.progress = () -> UnitTypes.smartServosResearchProgress(player.team());
        info.progressVisible = () -> UnitTypes.smartServosResearching(player.team());
        info.progressColor = Color.cyan;
        info.progressIcon = new TextureRegionDrawable(UnitTypes.mace.uiIcon);
        return info;
    }

    private BuildInfo makeHeavyFactoryAnyResearchInfo(String key){
        BuildInfo info = new BuildInfo();
        info.block = Blocks.memoryBank;
        info.key = key;
        info.action = "Research";
        info.progressColor = Color.cyan;

        if(UnitTypes.infernoPreheaterResearching(player.team())){
            info.name = "Inferno Pre-Igniter";
            info.crystalCost = UnitTypes.infernoPreheaterCrystalCost();
            info.gasCost = UnitTypes.infernoPreheaterGasCost();
            info.timeSeconds = Math.round(UnitTypes.infernoPreheaterResearchDuration() / 60f);
            info.progressIcon = new TextureRegionDrawable(UnitTypes.locus.uiIcon);
        }else if(UnitTypes.electromagneticFieldAcceleratorResearching(player.team())){
            info.name = "Electromagnetic Field Accelerator";
            info.crystalCost = UnitTypes.electromagneticFieldAcceleratorCrystalCost();
            info.gasCost = UnitTypes.electromagneticFieldAcceleratorGasCost();
            info.timeSeconds = Math.round(UnitTypes.electromagneticFieldAcceleratorResearchDuration() / 60f);
            info.progressIcon = new TextureRegionDrawable(UnitTypes.hurricane.uiIcon);
        }else if(UnitTypes.drillClawResearching(player.team())){
            info.name = "Drilling Claws";
            info.crystalCost = UnitTypes.drillClawCrystalCost();
            info.gasCost = UnitTypes.drillClawGasCost();
            info.timeSeconds = Math.round(UnitTypes.drillClawResearchDuration() / 60f);
            info.progressIcon = new TextureRegionDrawable(UnitTypes.crawler.uiIcon);
        }else if(UnitTypes.smartServosResearching(player.team())){
            info.name = "Smart Servos";
            info.crystalCost = UnitTypes.smartServosCrystalCost();
            info.gasCost = UnitTypes.smartServosGasCost();
            info.timeSeconds = Math.round(UnitTypes.smartServosResearchDuration() / 60f);
            info.progressIcon = new TextureRegionDrawable(UnitTypes.mace.uiIcon);
        }else{
            info.name = "Heavy Factory Tech";
            info.crystalCost = 0;
            info.gasCost = 0;
            info.timeSeconds = 0;
            info.progressIcon = new TextureRegionDrawable(Blocks.memoryBank.uiIcon);
        }

        info.progress = () -> {
            if(UnitTypes.infernoPreheaterResearching(player.team())) return UnitTypes.infernoPreheaterResearchProgress(player.team());
            if(UnitTypes.electromagneticFieldAcceleratorResearching(player.team())) return UnitTypes.electromagneticFieldAcceleratorResearchProgress(player.team());
            if(UnitTypes.drillClawResearching(player.team())) return UnitTypes.drillClawResearchProgress(player.team());
            if(UnitTypes.smartServosResearching(player.team())) return UnitTypes.smartServosResearchProgress(player.team());
            return 0f;
        };
        info.progressVisible = () -> UnitTypes.heavyFactoryTechAnyResearching(player.team());
        return info;
    }

    */
    private BuildInfo makeTechLabResearchInfo(Sc2ResearchSpec spec, String key){
        BuildInfo info = new BuildInfo();
        UnitType iconUnit = spec.iconUnit();
        info.block = Blocks.memoryBank;
        info.unit = iconUnit;
        info.key = key;
        info.name = spec.name(player.team());
        info.action = "Research";
        info.crystalCost = spec.crystalCost(player.team());
        info.gasCost = spec.gasCost(player.team());
        info.timeSeconds = Math.round(spec.duration(player.team()) / 60f);
        info.progress = () -> spec.progress(player.team());
        info.progressVisible = () -> spec.researching(player.team());
        info.progressColor = Color.cyan;
        info.progressIcon = new TextureRegionDrawable(iconUnit == null ? Blocks.memoryBank.uiIcon : iconUnit.uiIcon);
        return info;
    }

    private BuildInfo makeFusionCoreResearchInfo(Sc2ResearchSpec spec, String key){
        BuildInfo info = new BuildInfo();
        UnitType iconUnit = spec.iconUnit();
        info.block = Blocks.surgeCrucible;
        info.unit = iconUnit;
        info.key = key;
        info.name = spec.name(player.team());
        info.action = "Research";
        info.crystalCost = spec.crystalCost(player.team());
        info.gasCost = spec.gasCost(player.team());
        info.timeSeconds = Math.round(spec.duration(player.team()) / 60f);
        info.progress = () -> spec.progress(player.team());
        info.progressVisible = () -> spec.researching(player.team());
        info.progressColor = Color.cyan;
        info.progressIcon = new TextureRegionDrawable(iconUnit == null ? Blocks.surgeCrucible.uiIcon : iconUnit.uiIcon);
        return info;
    }

    private BuildInfo makeTechLabAnyResearchInfo(@Nullable Block attachedFactory, String key){
        BuildInfo info = new BuildInfo();
        info.block = Blocks.memoryBank;
        info.key = key;
        info.action = "Research";
        info.progressColor = Color.cyan;
        Sc2ResearchSpec active = ResearchQueueService.techLabActiveResearch(player.team(), attachedFactory);

        if(active != null){
            UnitType iconUnit = active.iconUnit();
            info.name = active.name(player.team());
            info.crystalCost = active.crystalCost(player.team());
            info.gasCost = active.gasCost(player.team());
            info.timeSeconds = Math.round(active.duration(player.team()) / 60f);
            info.progressIcon = new TextureRegionDrawable(iconUnit == null ? Blocks.memoryBank.uiIcon : iconUnit.uiIcon);
        }else{
            info.name = attachedFactory == Blocks.groundFactory ? "Barracks Tech" : "Heavy Factory Tech";
            info.crystalCost = 0;
            info.gasCost = 0;
            info.timeSeconds = 0;
            info.progressIcon = new TextureRegionDrawable(Blocks.memoryBank.uiIcon);
        }

        info.progress = () -> {
            Sc2ResearchSpec spec = ResearchQueueService.techLabActiveResearch(player.team(), attachedFactory);
            return spec == null ? 0f : spec.progress(player.team());
        };
        info.progressVisible = () -> ResearchQueueService.techLabAnyResearching(player.team(), attachedFactory);
        return info;
    }

    private BuildInfo makeFusionCoreAnyResearchInfo(String key){
        BuildInfo info = new BuildInfo();
        info.block = Blocks.surgeCrucible;
        info.key = key;
        info.action = "Research";
        info.progressColor = Color.cyan;
        Sc2ResearchSpec active = ResearchQueueService.fusionCoreActiveResearch(player.team());

        if(active != null){
            UnitType iconUnit = active.iconUnit();
            info.name = active.name(player.team());
            info.crystalCost = active.crystalCost(player.team());
            info.gasCost = active.gasCost(player.team());
            info.timeSeconds = Math.round(active.duration(player.team()) / 60f);
            info.progressIcon = new TextureRegionDrawable(iconUnit == null ? Blocks.surgeCrucible.uiIcon : iconUnit.uiIcon);
        }else{
            info.name = "Fusion Core Upgrade";
            info.crystalCost = 0;
            info.gasCost = 0;
            info.timeSeconds = 0;
            info.progressIcon = new TextureRegionDrawable(Blocks.surgeCrucible.uiIcon);
        }

        info.progress = () -> {
            Sc2ResearchSpec spec = ResearchQueueService.fusionCoreActiveResearch(player.team());
            return spec == null ? 0f : spec.progress(player.team());
        };
        info.progressVisible = () -> ResearchQueueService.fusionCoreAnyResearching(player.team());
        return info;
    }

    private BuildInfo makeInfantryAnyResearchInfo(String key){
        BuildInfo info = new BuildInfo();
        info.block = Blocks.multiPress;
        info.key = key;
        info.action = "Research";

        int weaponLevel = UnitTypes.infantryWeaponResearchingLevel(player.team());
        int armorLevel = UnitTypes.infantryArmorResearchingLevel(player.team());
        boolean weaponResearch = weaponLevel > 0;
        boolean armorResearch = armorLevel > 0;
        boolean instantResearch = UnitTypes.instantTrackingResearching(player.team());
        boolean steelResearch = UnitTypes.steelArmorResearching(player.team());

        if(steelResearch){
            info.name = "Instant Tracking";
            info.crystalCost = UnitTypes.steelArmorCrystalCost();
            info.gasCost = UnitTypes.steelArmorGasCost();
            info.timeSeconds = Math.round(UnitTypes.steelArmorResearchDuration() / 60f);
            info.progressIcon = new TextureRegionDrawable(Blocks.atmosphericConcentrator.uiIcon);
        }else if(weaponResearch){
            info.name = "Instant Tracking";
            info.crystalCost = UnitTypes.infantryWeaponCrystalCost(weaponLevel);
            info.gasCost = UnitTypes.infantryWeaponGasCost(weaponLevel);
            info.timeSeconds = Math.round(UnitTypes.infantryWeaponResearchDuration(weaponLevel) / 60f);
            info.progressIcon = new TextureRegionDrawable(Blocks.multiPress.uiIcon);
        }else if(armorResearch){
            info.name = "Instant Tracking";
            info.crystalCost = UnitTypes.infantryWeaponCrystalCost(armorLevel);
            info.gasCost = UnitTypes.infantryWeaponGasCost(armorLevel);
            info.timeSeconds = Math.round(UnitTypes.infantryWeaponResearchDuration(armorLevel) / 60f);
            info.progressIcon = new TextureRegionDrawable(Blocks.multiPress.uiIcon);
        }else if(instantResearch){
            info.name = "Instant Tracking";
            info.crystalCost = UnitTypes.instantTrackingCrystalCost();
            info.gasCost = UnitTypes.instantTrackingGasCost();
            info.timeSeconds = Math.round(UnitTypes.instantTrackingResearchDuration() / 60f);
            info.progressIcon = new TextureRegionDrawable(Blocks.swarmer.uiIcon);
        }else{
            info.name = "Engineering Upgrade";
            info.crystalCost = 0;
            info.gasCost = 0;
            info.timeSeconds = 0;
            info.progressIcon = new TextureRegionDrawable(Blocks.multiPress.uiIcon);
        }

        info.progress = () -> {
            if(UnitTypes.steelArmorResearching(player.team())) return UnitTypes.steelArmorResearchProgress(player.team());
            if(UnitTypes.infantryWeaponResearching(player.team())) return UnitTypes.infantryWeaponResearchProgress(player.team());
            if(UnitTypes.infantryArmorResearching(player.team())) return UnitTypes.infantryArmorResearchProgress(player.team());
            if(UnitTypes.instantTrackingResearching(player.team())) return UnitTypes.instantTrackingResearchProgress(player.team());
            return 0f;
        };
        info.progressVisible = () -> UnitTypes.infantryAnyResearching(player.team());
        info.progressColor = Color.cyan;
        return info;
    }

    private BuildInfo makeBuildInfo(Block block, String key){
        BuildInfo info = new BuildInfo();
        info.block = block;
        info.key = key;
        info.name = sc2Name(block);
        info.action = "Build";
        info.crystalCost = getCost(block, Items.graphite);
        info.gasCost = getCost(block, Items.highEnergyGas);
        info.timeSeconds = Math.round(block.buildTime / 60f);
        return info;
    }

    private BuildInfo makeUnitInfo(UnitFactory.UnitPlan plan, String key){
        BuildInfo info = new BuildInfo();
        info.unit = plan.unit;
        info.key = key;
        info.name = sc2Name(plan.unit);
        info.action = "Train";
        info.population = plan.unit.population;
        info.crystalCost = getCost(plan.requirements, Items.graphite);
        info.gasCost = getCost(plan.requirements, Items.highEnergyGas);
        info.timeSeconds = Math.round(plan.time / 60f);
        return info;
    }

    private BuildInfo makeWidowActionInfo(String key, String name, Color color, Floatp progress, Boolp visible){
        BuildInfo info = new BuildInfo();
        info.unit = UnitTypes.crawler;
        info.key = key;
        info.name = name;
        info.action = "Ability";
        info.crystalCost = 0;
        info.gasCost = 0;
        info.timeSeconds = 0;
        info.progress = progress;
        info.progressVisible = visible;
        info.progressColor = color;
        info.progressIcon = new TextureRegionDrawable(UnitTypes.crawler.uiIcon);
        return info;
    }

    private int autoCastBit(AutoCastSkill skill){
        return switch(skill){
            case hurricaneLock -> autoCastHurricaneLock;
            case medivacHeal -> autoCastMedivacHeal;
        };
    }

    private boolean isAutoCastEnabled(Unit unit, AutoCastSkill skill){
        int defaultFlags = 0;
        if(UnitTypes.isHurricane(unit)) defaultFlags |= autoCastHurricaneLock;
        if(UnitTypes.isMedivac(unit)) defaultFlags |= autoCastMedivacHeal;
        int flags = autoCastFlags.get(unit.id, defaultFlags);
        return (flags & autoCastBit(skill)) != 0;
    }

    private void setAutoCastEnabled(Unit unit, AutoCastSkill skill, boolean enabled){
        int defaultFlags = 0;
        if(UnitTypes.isHurricane(unit)) defaultFlags |= autoCastHurricaneLock;
        if(UnitTypes.isMedivac(unit)) defaultFlags |= autoCastMedivacHeal;
        int flags = autoCastFlags.get(unit.id, defaultFlags);
        int bit = autoCastBit(skill);
        int next = enabled ? (flags | bit) : (flags & ~bit);
        if(next == defaultFlags){
            autoCastFlags.remove(unit.id, 0);
        }else{
            autoCastFlags.put(unit.id, next);
        }
    }

    private boolean selectedMedivacHealAutoCastEnabled(){
        for(Unit unit : abilityUnits()){
            if(unit == null || !unit.isValid() || !UnitTypes.isMedivac(unit)) continue;
            if(isAutoCastEnabled(unit, AutoCastSkill.medivacHeal)) return true;
        }
        return false;
    }

    private void toggleSelectedMedivacHealAutoCast(){
        boolean hasAny = false;
        boolean allEnabled = true;
        for(Unit unit : abilityUnits()){
            if(unit == null || !unit.isValid() || !UnitTypes.isMedivac(unit)) continue;
            hasAny = true;
            if(!isAutoCastEnabled(unit, AutoCastSkill.medivacHeal)){
                allEnabled = false;
            }
        }
        if(!hasAny) return;

        boolean nextEnabled = !allEnabled;
        for(Unit unit : abilityUnits()){
            if(unit == null || !unit.isValid() || !UnitTypes.isMedivac(unit)) continue;
            setAutoCastEnabled(unit, AutoCastSkill.medivacHeal, nextEnabled);
        }
    }

    private boolean selectedHurricaneAutoCastEnabled(){
        for(Unit unit : abilityUnits()){
            if(unit == null || !unit.isValid() || !UnitTypes.isHurricane(unit)) continue;
            if(isAutoCastEnabled(unit, AutoCastSkill.hurricaneLock)) return true;
        }
        return false;
    }

    private void toggleSelectedHurricaneAutoCast(){
        boolean hasAny = false;
        boolean allEnabled = true;
        for(Unit unit : abilityUnits()){
            if(unit == null || !unit.isValid() || !UnitTypes.isHurricane(unit)) continue;
            hasAny = true;
            if(!isAutoCastEnabled(unit, AutoCastSkill.hurricaneLock)){
                allEnabled = false;
            }
        }
        if(!hasAny) return;

        boolean nextEnabled = !allEnabled;
        for(Unit unit : abilityUnits()){
            if(unit == null || !unit.isValid() || !UnitTypes.isHurricane(unit)) continue;
            setAutoCastEnabled(unit, AutoCastSkill.hurricaneLock, nextEnabled);
        }
    }

    private boolean anyHurricaneCanLock(){
        for(Unit unit : abilityUnits()){
            if(unit == null || !unit.isValid() || !UnitTypes.isHurricane(unit)) continue;
            if(UnitTypes.hurricaneCanLock(unit) && UnitTypes.hurricaneHasTarget(unit)) return true;
        }
        return false;
    }

    private boolean anyHurricaneLockActive(){
        for(Unit unit : abilityUnits()){
            if(unit == null || !unit.isValid() || !UnitTypes.isHurricane(unit)) continue;
            if(UnitTypes.hurricaneLockActive(unit)) return true;
        }
        return false;
    }

    private boolean anyReaperCanUseKd8(){
        for(Unit unit : abilityUnits()){
            if(unit == null || !unit.isValid() || unit.type != UnitTypes.reaper) continue;
            if(UnitTypes.reaperCanUseKd8(unit)) return true;
        }
        return false;
    }

    private float selectedHurricaneLockCooldown(){
        float result = 0f;
        for(Unit unit : abilityUnits()){
            if(unit == null || !unit.isValid() || !UnitTypes.isHurricane(unit)) continue;
            result = Math.max(result, UnitTypes.hurricaneLockCooldown(unit));
        }
        return result;
    }

    private float selectedReaperKd8Cooldown(){
        float result = 0f;
        for(Unit unit : abilityUnits()){
            if(unit == null || !unit.isValid() || unit.type != UnitTypes.reaper) continue;
            result = Math.max(result, UnitTypes.reaperKd8Cooldown(unit));
        }
        return result;
    }

    private void issueHurricaneLockCommand(){
        for(Unit unit : abilityUnits()){
            if(unit == null || !unit.isValid() || !UnitTypes.isHurricane(unit)) continue;
            if(!UnitTypes.hurricaneCanLock(unit) || !UnitTypes.hurricaneHasTarget(unit)) continue;
            Call.commandHurricaneLock(player, new int[]{unit.id});
            return;
        }
    }

    private boolean anyBarracksStimpackSelectedCanUse(){
        for(Unit unit : abilityUnits()){
            if(unit == null || !unit.isValid() || !UnitTypes.isBarracksStimpackUnit(unit)) continue;
            if(UnitTypes.barracksStimpackCanUse(unit)) return true;
        }
        return false;
    }

    private float selectedBarracksStimpackCooldown(){
        float result = 0f;
        for(Unit unit : abilityUnits()){
            if(unit == null || !unit.isValid() || !UnitTypes.isBarracksStimpackUnit(unit)) continue;
            result = Math.max(result, UnitTypes.barracksStimpackCooldown(unit));
        }
        return result;
    }

    private void issueBarracksStimpackCommand(){
        IntSeq ids = new IntSeq();
        for(Unit unit : abilityUnits()){
            if(unit == null || !unit.isValid() || !UnitTypes.isBarracksStimpackUnit(unit)) continue;
            if(!UnitTypes.barracksStimpackCanUse(unit)) continue;
            ids.add(unit.id);
        }
        if(ids.size > 0){
            Call.commandBarracksStimpack(player, ids.toArray());
        }else if(UnitTypes.barracksStimpackLevel(player.team()) <= 0){
            ui.hudfrag.setHudText("Requires Stimpack");
        }
    }

    private void updateAutoCast(){
        if(player == null || player.team() == null || player.team().data() == null) return;
        if(Time.time < nextAutoCastUpdate) return;
        nextAutoCastUpdate = Time.time + 10f;

        IntSeq hurricaneIds = new IntSeq();
        for(Unit unit : player.team().data().units){
            if(unit == null || !unit.isValid() || !UnitTypes.isHurricane(unit)) continue;

            int flags = autoCastFlags.get(unit.id, autoCastHurricaneLock);
            if((flags & autoCastHurricaneLock) == 0) continue;
            if(!UnitTypes.hurricaneCanLock(unit) || !UnitTypes.hurricaneHasTarget(unit)) continue;
            hurricaneIds.add(unit.id);
        }

        if(hurricaneIds.size > 0){
            Call.commandHurricaneLock(player, hurricaneIds.toArray());
        }

        for(Unit unit : player.team().data().units){
            if(unit == null || !unit.isValid() || !UnitTypes.isMedivac(unit)) continue;

            int flags = autoCastFlags.get(unit.id, autoCastMedivacHeal);
            if((flags & autoCastMedivacHeal) == 0) continue;
            if(unit.energy <= 0.001f) continue;

            if(unit.controller() instanceof CommandAI ai){
                if(ai.command == UnitCommand.loadUnitsCommand || ai.command == UnitCommand.unloadPayloadCommand){
                    continue;
                }
            }

            Unit target = UnitTypes.medivacFindHealTarget(unit);
            if(target == null) continue;

            if(unit.controller() instanceof CommandAI ai && ai.followTarget == target){
                continue;
            }

            Call.setUnitCommand(player, new int[]{unit.id}, UnitCommand.moveCommand);
            Call.commandMedivacMovingUnload(player, new int[]{unit.id}, false);
            Call.commandUnits(player, new int[]{unit.id}, null, target, new Vec2(target.x, target.y), false, true, false);
        }
    }

    private boolean allSelectedPreceptSieged(){
        if(abilityUnits().isEmpty()) return false;
        for(Unit unit : abilityUnits()){
            if(unit == null || !unit.isValid() || !UnitTypes.isSiegeTank(unit)) return false;
            if(!UnitTypes.preceptIsSieged(unit)) return false;
        }
        return true;
    }

    private boolean anyPreceptTransitioning(){
        for(Unit unit : abilityUnits()){
            if(unit == null || !unit.isValid() || !UnitTypes.isSiegeTank(unit)) continue;
            if(UnitTypes.preceptIsSieging(unit) || UnitTypes.preceptIsUnsieging(unit)) return true;
        }
        return false;
    }

    private boolean anyPreceptCanSiege(){
        for(Unit unit : abilityUnits()){
            if(unit == null || !unit.isValid() || !UnitTypes.isSiegeTank(unit)) continue;
            if(UnitTypes.preceptCanEnterSiege(unit)) return true;
        }
        return false;
    }

    private boolean anyPreceptCanTankMode(){
        for(Unit unit : abilityUnits()){
            if(unit == null || !unit.isValid() || !UnitTypes.isSiegeTank(unit)) continue;
            if(UnitTypes.preceptCanExitSiege(unit)) return true;
        }
        return false;
    }

    private void issuePreceptSiegeCommand(boolean siege){
        IntSeq ids = new IntSeq();
        for(Unit unit : abilityUnits()){
            if(unit == null || !unit.isValid() || !UnitTypes.isSiegeTank(unit)) continue;
            if(siege){
                if(!UnitTypes.preceptCanEnterSiege(unit)) continue;
            }else{
                if(!UnitTypes.preceptCanExitSiege(unit)) continue;
            }
            ids.add(unit.id);
        }
        if(ids.size > 0){
            Call.commandPreceptSiege(player, ids.toArray(), siege);
        }
    }

    private boolean anyScepterCanSwitchToImpact(){
        for(Unit unit : abilityUnits()){
            if(unit == null || !unit.isValid() || !UnitTypes.isThor(unit)) continue;
            if(UnitTypes.scepterCanSwitchToImpact(unit)) return true;
        }
        return false;
    }

    private boolean anyScepterCanSwitchToBurst(){
        for(Unit unit : abilityUnits()){
            if(unit == null || !unit.isValid() || !UnitTypes.isThor(unit)) continue;
            if(UnitTypes.scepterCanSwitchToBurst(unit)) return true;
        }
        return false;
    }

    private void issueScepterAirModeCommand(boolean impactMode){
        IntSeq ids = new IntSeq();
        for(Unit unit : abilityUnits()){
            if(unit == null || !unit.isValid() || !UnitTypes.isThor(unit)) continue;
            if(impactMode){
                if(!UnitTypes.scepterCanSwitchToImpact(unit)) continue;
            }else{
                if(!UnitTypes.scepterCanSwitchToBurst(unit)) continue;
            }
            ids.add(unit.id);
        }
        if(ids.size > 0){
            Call.commandScepterAirMode(player, ids.toArray(), impactMode);
        }
    }

    private boolean allSelectedLiberatorDefending(){
        if(abilityUnits().isEmpty()) return false;
        for(Unit unit : abilityUnits()){
            if(unit == null || !unit.isValid() || !UnitTypes.isLiberator(unit)) return false;
            if(!UnitTypes.liberatorIsDefending(unit)) return false;
        }
        return true;
    }

    private boolean anyLiberatorTransitioning(){
        for(Unit unit : abilityUnits()){
            if(unit == null || !unit.isValid() || !UnitTypes.isLiberator(unit)) continue;
            if(UnitTypes.liberatorIsDeploying(unit) || UnitTypes.liberatorIsUndeploying(unit)) return true;
        }
        return false;
    }

    private boolean anyLiberatorCanEnterDefense(){
        for(Unit unit : abilityUnits()){
            if(unit == null || !unit.isValid() || !UnitTypes.isLiberator(unit)) continue;
            if(UnitTypes.liberatorCanEnterDefense(unit)) return true;
        }
        return false;
    }

    private boolean anyLiberatorCanExitDefense(){
        for(Unit unit : abilityUnits()){
            if(unit == null || !unit.isValid() || !UnitTypes.isLiberator(unit)) continue;
            if(UnitTypes.liberatorCanExitDefense(unit)) return true;
        }
        return false;
    }

    private void issueLiberatorFighterCommand(){
        IntSeq ids = new IntSeq();
        for(Unit unit : abilityUnits()){
            if(unit == null || !unit.isValid() || !UnitTypes.isLiberator(unit)) continue;
            if(!UnitTypes.liberatorCanExitDefense(unit)) continue;
            ids.add(unit.id);
        }
        if(ids.size > 0){
            Call.commandLiberatorMode(player, ids.toArray(), false, null);
        }
    }

    private boolean anyWidowCanBurrow(){
        for(Unit unit : abilityUnits()){
            if(!UnitTypes.isWidow(unit)) continue;
            if(!UnitTypes.widowIsBuried(unit) && !UnitTypes.widowIsBurrowing(unit) && !UnitTypes.widowIsUnburrowing(unit)){
                return true;
            }
        }
        return false;
    }

    private boolean anyWidowShowCommandRow1(){
        for(Unit unit : abilityUnits()){
            if(!UnitTypes.isWidow(unit)) continue;
            if(!UnitTypes.widowIsBuried(unit) && !UnitTypes.widowIsBurrowing(unit) && !UnitTypes.widowIsUnburrowing(unit)){
                return true;
            }
        }
        return false;
    }

    private boolean anyWidowCanUnburrow(){
        for(Unit unit : abilityUnits()){
            if(!UnitTypes.isWidow(unit)) continue;
            if((UnitTypes.widowIsBuried(unit) || UnitTypes.widowIsBurrowing(unit)) && !UnitTypes.widowIsUnburrowing(unit)){
                return true;
            }
        }
        return false;
    }

    private boolean anyWidowBurrowing(){
        for(Unit unit : abilityUnits()){
            if(UnitTypes.widowIsBurrowing(unit)) return true;
        }
        return false;
    }

    private boolean anyWidowReloading(){
        for(Unit unit : abilityUnits()){
            if(UnitTypes.widowIsReloading(unit)) return true;
        }
        return false;
    }

    private float selectedWidowBurrowProgress(){
        float progress = 0f;
        for(Unit unit : abilityUnits()){
            progress = Math.max(progress, UnitTypes.widowBurrowProgress(unit));
        }
        return progress;
    }

    private float selectedWidowReloadProgress(){
        float progress = 0f;
        for(Unit unit : abilityUnits()){
            progress = Math.max(progress, UnitTypes.widowReloadProgress(unit));
        }
        return progress;
    }

    private void issueWidowBurrowCommand(boolean burrow){
        IntSeq ids = new IntSeq();
        for(Unit unit : abilityUnits()){
            if(unit == null || !unit.isValid() || !UnitTypes.isWidow(unit)) continue;
            if(burrow){
                if(UnitTypes.widowIsBuried(unit) || UnitTypes.widowIsBurrowing(unit) || UnitTypes.widowIsUnburrowing(unit)) continue;
            }else{
                if((!UnitTypes.widowIsBuried(unit) && !UnitTypes.widowIsBurrowing(unit)) || UnitTypes.widowIsUnburrowing(unit)) continue;
            }
            ids.add(unit.id);
        }
        if(ids.size > 0){
            Call.commandWidowMine(player, ids.toArray(), burrow);
        }
    }

    private boolean anyMedivacHasPayload(){
        for(Unit unit : abilityUnits()){
            if(unit == null || !unit.isValid() || !UnitTypes.isMedivac(unit) || !(unit instanceof Payloadc pay)) continue;
            if(!pay.payloads().isEmpty()) return true;
        }
        return false;
    }

    private boolean anyMedivacCanLoadMore(){
        for(Unit unit : abilityUnits()){
            if(unit == null || !unit.isValid() || !UnitTypes.isMedivac(unit)) continue;
            if(UnitTypes.medivacPayloadSlotsFree(unit) > 0) return true;
        }
        return false;
    }

    private void issueMedivacAfterburnerCommand(){
        IntSeq ids = new IntSeq();
        for(Unit unit : abilityUnits()){
            if(unit == null || !unit.isValid() || !UnitTypes.isMedivac(unit)) continue;
            ids.add(unit.id);
        }
        if(ids.size > 0){
            Call.commandMedivacAfterburner(player, ids.toArray());
        }
    }

    private boolean anyMaceSelected(){
        for(Unit unit : abilityUnits()){
            if(unit == null || !unit.isValid() || !UnitTypes.isMace(unit)) continue;
            return true;
        }
        return false;
    }

    private boolean anyLocusSelected(){
        for(Unit unit : abilityUnits()){
            if(unit == null || !unit.isValid() || !UnitTypes.isLocus(unit)) continue;
            return true;
        }
        return false;
    }

    private boolean anyMaceCanTransformToLocus(){
        for(Unit unit : abilityUnits()){
            if(unit == null || !unit.isValid() || !UnitTypes.isMace(unit)) continue;
            if(UnitTypes.maceCanTransformToLocus(unit)) return true;
        }
        return false;
    }

    private boolean anyLocusCanTransformToMace(){
        for(Unit unit : abilityUnits()){
            if(unit == null || !unit.isValid() || !UnitTypes.isLocus(unit)) continue;
            if(UnitTypes.locusCanTransformToMace(unit)) return true;
        }
        return false;
    }

    private void issueMaceLocusModeCommand(boolean toLocus){
        IntSeq ids = new IntSeq();
        for(Unit unit : abilityUnits()){
            if(unit == null || !unit.isValid()) continue;
            if(toLocus){
                if(!UnitTypes.isMace(unit) || !UnitTypes.maceCanTransformToLocus(unit)) continue;
            }else{
                if(!UnitTypes.isLocus(unit) || !UnitTypes.locusCanTransformToMace(unit)) continue;
            }
            ids.add(unit.id);
        }
        if(ids.size > 0){
            Call.commandMaceLocusMode(player, ids.toArray(), toLocus);
        }
    }

    private boolean anyVikingCanSwitchToMech(){
        for(Unit unit : abilityUnits()){
            if(unit == null || !unit.isValid() || !UnitTypes.isViking(unit)) continue;
            if(UnitTypes.vikingCanTransformToMech(unit)) return true;
        }
        return false;
    }

    private boolean anyVikingCanSwitchToFighter(){
        for(Unit unit : abilityUnits()){
            if(unit == null || !unit.isValid() || !UnitTypes.isViking(unit)) continue;
            if(UnitTypes.vikingCanTransformToFighter(unit)) return true;
        }
        return false;
    }

    private void issueVikingModeCommand(boolean mechMode){
        IntSeq ids = new IntSeq();
        for(Unit unit : abilityUnits()){
            if(unit == null || !unit.isValid() || !UnitTypes.isViking(unit)) continue;
            if(mechMode){
                if(!UnitTypes.vikingCanTransformToMech(unit)) continue;
            }else{
                if(!UnitTypes.vikingCanTransformToFighter(unit)) continue;
            }
            ids.add(unit.id);
        }
        if(ids.size > 0){
            Call.commandVikingMode(player, ids.toArray(), mechMode);
        }
    }

    private boolean anyGhostCanToggleCloak(){
        for(Unit unit : abilityUnits()){
            if(unit == null || !unit.isValid() || !UnitTypes.isGhost(unit)) continue;
            if(UnitTypes.ghostCanToggleCloak(unit)) return true;
        }
        return false;
    }

    private boolean anyGhostCanUseStableAim(){
        for(Unit unit : abilityUnits()){
            if(unit == null || !unit.isValid() || !UnitTypes.isGhost(unit)) continue;
            if(UnitTypes.ghostCanUseStableAim(unit)) return true;
        }
        return false;
    }

    private boolean anyGhostCanUseEmp(){
        for(Unit unit : abilityUnits()){
            if(unit == null || !unit.isValid() || !UnitTypes.isGhost(unit)) continue;
            if(UnitTypes.ghostCanUseEmp(unit)) return true;
        }
        return false;
    }

    private boolean anyGhostStableAimPending(){
        for(Unit unit : abilityUnits()){
            if(unit == null || !unit.isValid() || !UnitTypes.isGhost(unit)) continue;
            if(UnitTypes.ghostStableAimPending(unit)) return true;
        }
        return false;
    }

    private boolean anyGhostEmpPending(){
        for(Unit unit : abilityUnits()){
            if(unit == null || !unit.isValid() || !UnitTypes.isGhost(unit)) continue;
            if(UnitTypes.ghostEmpPending(unit)) return true;
        }
        return false;
    }

    private void issueGhostCloakCommand(){
        IntSeq ids = new IntSeq();
        for(Unit unit : abilityUnits()){
            if(unit == null || !unit.isValid() || !UnitTypes.isGhost(unit)) continue;
            if(!UnitTypes.ghostCanToggleCloak(unit)) continue;
            ids.add(unit.id);
        }
        if(ids.size > 0){
            Call.commandGhostCloak(player, ids.toArray());
        }
    }

    private void issueGhostStableAimCancelCommand(){
        IntSeq ids = new IntSeq();
        for(Unit unit : abilityUnits()){
            if(unit == null || !unit.isValid() || !UnitTypes.isGhost(unit)) continue;
            if(!UnitTypes.ghostStableAimPending(unit)) continue;
            ids.add(unit.id);
        }
        if(ids.size > 0){
            Call.commandGhostStableAimCancel(player, ids.toArray());
        }
    }

    private void issueGhostEmpCancelCommand(){
        IntSeq ids = new IntSeq();
        for(Unit unit : abilityUnits()){
            if(unit == null || !unit.isValid() || !UnitTypes.isGhost(unit)) continue;
            if(!UnitTypes.ghostEmpPending(unit)) continue;
            ids.add(unit.id);
        }
        if(ids.size > 0){
            Call.commandGhostEmpCancel(player, ids.toArray());
        }
    }

    private boolean anyGhostCanUseTacticalNuke(){
        for(Unit unit : abilityUnits()){
            if(unit == null || !unit.isValid() || !UnitTypes.isGhost(unit)) continue;
            if(UnitTypes.ghostCanUseTacticalNuke(unit)) return true;
        }
        return false;
    }

    private boolean anyGhostTacticalNukePending(){
        for(Unit unit : abilityUnits()){
            if(unit == null || !unit.isValid() || !UnitTypes.isGhost(unit)) continue;
            if(UnitTypes.ghostTacticalNukePending(unit)) return true;
        }
        return false;
    }

    private int selectedGhostWarheadCount(){
        return UnitTypes.ghostWarheadCount(player.team());
    }

    private void issueGhostTacticalNukeCancelCommand(){
        IntSeq ids = new IntSeq();
        for(Unit unit : abilityUnits()){
            if(unit == null || !unit.isValid() || !UnitTypes.isGhost(unit)) continue;
            if(!UnitTypes.ghostTacticalNukePending(unit)) continue;
            ids.add(unit.id);
        }
        if(ids.size > 0){
            Call.commandGhostTacticalNukeCancel(player, ids.toArray());
        }
    }

    private boolean anyRavenCanDeployTurret(){
        for(Unit unit : abilityUnits()){
            if(unit == null || !unit.isValid() || !UnitTypes.isRaven(unit)) continue;
            if(UnitTypes.ravenCanDeployTurret(unit)) return true;
        }
        return false;
    }

    private boolean anyBansheeCanToggleCloak(){
        for(Unit unit : abilityUnits()){
            if(unit == null || !unit.isValid() || !UnitTypes.isBanshee(unit)) continue;
            if(UnitTypes.bansheeCanToggleCloak(unit)) return true;
        }
        return false;
    }

    private boolean anyBattlecruiserCanUseYamato(){
        for(Unit unit : abilityUnits()){
            if(unit == null || !unit.isValid() || !UnitTypes.isBattlecruiser(unit)) continue;
            if(UnitTypes.battlecruiserCanUseYamato(unit)) return true;
        }
        return false;
    }

    private boolean anyBattlecruiserCanUseWarp(){
        for(Unit unit : abilityUnits()){
            if(unit == null || !unit.isValid() || !UnitTypes.isBattlecruiser(unit)) continue;
            if(UnitTypes.battlecruiserCanUseWarp(unit)) return true;
        }
        return false;
    }

    private float selectedBattlecruiserYamatoCooldown(){
        float result = 0f;
        for(Unit unit : abilityUnits()){
            if(unit == null || !unit.isValid() || !UnitTypes.isBattlecruiser(unit)) continue;
            result = Math.max(result, UnitTypes.battlecruiserYamatoCooldown(unit));
        }
        return result;
    }

    private float selectedBattlecruiserWarpCooldown(){
        float result = 0f;
        for(Unit unit : abilityUnits()){
            if(unit == null || !unit.isValid() || !UnitTypes.isBattlecruiser(unit)) continue;
            result = Math.max(result, UnitTypes.battlecruiserWarpCooldown(unit));
        }
        return result;
    }

    private void issueBansheeCloakCommand(){
        IntSeq ids = new IntSeq();
        for(Unit unit : abilityUnits()){
            if(unit == null || !unit.isValid() || !UnitTypes.isBanshee(unit)) continue;
            if(!UnitTypes.bansheeCanToggleCloak(unit)) continue;
            ids.add(unit.id);
        }
        if(ids.size > 0){
            Call.commandBansheeCloak(player, ids.toArray());
        }
    }

    private boolean anyRavenCanUseAntiArmor(){
        for(Unit unit : abilityUnits()){
            if(unit == null || !unit.isValid() || !UnitTypes.isRaven(unit)) continue;
            if(UnitTypes.ravenCanUseAntiArmor(unit)) return true;
        }
        return false;
    }

    private boolean anyRavenCanUseMatrix(){
        if(UnitTypes.ravenMatrixTechLevel(player.team()) <= 0){
            return false;
        }
        for(Unit unit : abilityUnits()){
            if(unit == null || !unit.isValid() || !UnitTypes.isRaven(unit)) continue;
            if(UnitTypes.ravenCanUseMatrix(unit)) return true;
        }
        return false;
    }

    private void issueRavenDeployTurretCommand(){
        if(anyRavenCanDeployTurret()){
            enterCommandMode(CommandMode.RAVEN_TURRET);
        }
    }

    private int getCost(Block block, Item item){
        if(block == null || item == null) return 0;
        int total = 0;
        for(ItemStack stack : block.requirements){
            if(stack.item == item){
                total += stack.amount;
            }
        }
        return total;
    }

    private int getCost(ItemStack[] requirements, Item item){
        if(requirements == null || item == null) return 0;
        int total = 0;
        for(ItemStack stack : requirements){
            if(stack.item == item){
                total += stack.amount;
            }
        }
        return total;
    }

    /*
    private String sc2Name(Block block){
        if(block == Blocks.coreNucleus) return "闁糕晛鎼﹢?;
        if(block == Blocks.ventCondenser) return "缂侇喖澧介崑褔宕?;
        if(block == Blocks.doorLarge) return "閻炴稏鍎崇划鎵博?;
        if(block == Blocks.groundFactory) return "闁稿繋绲婚幆鈧?;
        if(block == Blocks.multiPress) return "鐎规悶鍎抽埢鑲╃博?;
        if(block == Blocks.atmosphericConcentrator) return "闁革附婢橀悧?;
        if(block == Blocks.swarmer) return "閻庣數鍘ч懘濠冪箙?;
        if(block == Blocks.hail) return "闁规壆鍠庣花鍙夌箙?;
        if(block == Blocks.launchPad) return "妤犵偟鏅导鎺楀礃濞戞瑧澧?;
        if(block == Blocks.tankFabricator) return "闂佹彃绉存导鎰板储?;
        if(block == Blocks.siliconCrucible) return "闁告劖绋掗～顐ｆ償?;
        if(block == Blocks.shipFabricator) return "闁哄嫮鍠愰懙?;
        if(block == Blocks.surgeCrucible) return "闁艰鲸鑹捐ぐ澶愭嚍椤栨瑧绉?;
        return block.localizedName;
    }

    private String sc2Name(UnitType unit){
        if(unit == UnitTypes.dagger) return "闁哄浜滈崣?;
        if(unit == UnitTypes.reaper) return "婵繆宕甸〃?;
        if(unit == UnitTypes.fortress) return "闁告棏鍋呯敮顒勬嚀?;
        if(unit == UnitTypes.ghost) return "妤犵偟鏅导?;
        return unit.localizedName;
    }

    */
    private String sc2Name(Block block){
        return block == null ? "" : block.localizedName;
    }

    private String sc2Name(UnitType unit){
        return unit == null ? "" : unit.localizedName;
    }

    private void addEmpty(Table grid){
        grid.add().size(abilityButtonSize).pad(ABILITY_BUTTON_PAD);
    }

    private void buildEmptyPanel(){
        Table grid = new Table();
        for(int r = 0; r < ROWS; r++){
            for(int c = 0; c < COLS; c++){
                addEmpty(grid);
            }
            grid.row();
        }
        add(grid);
    }

    private void setPanelRows(int rows){
        float cell = abilityButtonSize + ABILITY_BUTTON_PAD * 2f;
        forcedMinWidth = COLS * cell + PANEL_MARGIN * 2f;
        forcedMinHeight = ROWS * cell + PANEL_MARGIN * 2f;
    }

    private void clearPanelSize(){
        setPanelRows(ROWS);
    }

    @Override
    public float getMinWidth(){
        return forcedMinWidth > 0f ? forcedMinWidth : super.getMinWidth();
    }

    @Override
    public float getMinHeight(){
        return forcedMinHeight > 0f ? forcedMinHeight : super.getMinHeight();
    }

    @Override
    public float getPrefWidth(){
        return forcedMinWidth > 0f ? forcedMinWidth : super.getPrefWidth();
    }

    @Override
    public float getPrefHeight(){
        return forcedMinHeight > 0f ? forcedMinHeight : super.getPrefHeight();
    }

    private void addCancelButton(Table grid, Runnable action){
        addIconButton(grid, "Esc", Icon.cancel, () -> true, action);
    }

    private void addEscButton(Table grid, Runnable action){
        addIconButton(grid, "Esc", Icon.left, () -> true, action);
    }

    private void addStopBuildButton(Table grid){
        addIconButton(grid, "t", Icon.pause, () -> true, this::stopSelectedBuilders);
    }

    private void fillRow(Table grid, int row, int startCol){
        for(int i = startCol; i < COLS; i++){
            addEmpty(grid);
        }
    }

    private boolean isOnlyNovaSelected(){
        if(abilityUnits().isEmpty()) return false;
        for(Unit unit : abilityUnits()){
            if(unit == null || !unit.isValid() || !unit.type.name.equals("nova")) return false;
        }
        return true;
    }

    private boolean isOnlyWidowSelected(){
        if(abilityUnits().isEmpty()) return false;
        for(Unit unit : abilityUnits()){
            if(unit == null || !unit.isValid() || !UnitTypes.isWidow(unit)) return false;
        }
        return true;
    }

    private boolean isOnlyHurricaneSelected(){
        if(abilityUnits().isEmpty()) return false;
        for(Unit unit : abilityUnits()){
            if(unit == null || !unit.isValid() || unit.type != UnitTypes.hurricane) return false;
        }
        return true;
    }

    private boolean isOnlyScepterSelected(){
        if(abilityUnits().isEmpty()) return false;
        for(Unit unit : abilityUnits()){
            if(unit == null || !unit.isValid() || !UnitTypes.isThor(unit)) return false;
        }
        return true;
    }

    private boolean isOnlyMedivacSelected(){
        if(abilityUnits().isEmpty()) return false;
        for(Unit unit : abilityUnits()){
            if(unit == null || !unit.isValid() || !UnitTypes.isMedivac(unit)) return false;
        }
        return true;
    }

    private boolean isOnlyGhostSelected(){
        if(abilityUnits().isEmpty()) return false;
        for(Unit unit : abilityUnits()){
            if(unit == null || !unit.isValid() || !UnitTypes.isGhost(unit)) return false;
        }
        return true;
    }

    private boolean isOnlyReaperSelected(){
        if(abilityUnits().isEmpty()) return false;
        for(Unit unit : abilityUnits()){
            if(unit == null || !unit.isValid() || unit.type != UnitTypes.reaper) return false;
        }
        return true;
    }

    private boolean isOnlyVikingSelected(){
        if(abilityUnits().isEmpty()) return false;
        for(Unit unit : abilityUnits()){
            if(unit == null || !unit.isValid() || !UnitTypes.isViking(unit)) return false;
        }
        return true;
    }

    private boolean isOnlyMaceLocusSelected(){
        if(abilityUnits().isEmpty()) return false;
        for(Unit unit : abilityUnits()){
            if(unit == null || !unit.isValid() || (!UnitTypes.isMace(unit) && !UnitTypes.isLocus(unit))) return false;
        }
        return true;
    }

    private boolean isOnlyMaceSelected(){
        if(abilityUnits().isEmpty()) return false;
        for(Unit unit : abilityUnits()){
            if(unit == null || !unit.isValid() || !UnitTypes.isMace(unit)) return false;
        }
        return true;
    }

    private boolean isOnlyLocusSelected(){
        if(abilityUnits().isEmpty()) return false;
        for(Unit unit : abilityUnits()){
            if(unit == null || !unit.isValid() || !UnitTypes.isLocus(unit)) return false;
        }
        return true;
    }

    private boolean isOnlyBattlecruiserSelected(){
        if(abilityUnits().isEmpty()) return false;
        for(Unit unit : abilityUnits()){
            if(unit == null || !unit.isValid() || !UnitTypes.isBattlecruiser(unit)) return false;
        }
        return true;
    }

    private boolean isOnlyBansheeSelected(){
        if(abilityUnits().isEmpty()) return false;
        for(Unit unit : abilityUnits()){
            if(unit == null || !unit.isValid() || !UnitTypes.isBanshee(unit)) return false;
        }
        return true;
    }

    private boolean isOnlyRavenSelected(){
        if(abilityUnits().isEmpty()) return false;
        for(Unit unit : abilityUnits()){
            if(unit == null || !unit.isValid() || !UnitTypes.isRaven(unit)) return false;
        }
        return true;
    }

    private boolean isOnlyRavenTurretSelected(){
        if(abilityUnits().isEmpty()) return false;
        for(Unit unit : abilityUnits()){
            if(unit == null || !unit.isValid() || !UnitTypes.isRavenTurret(unit)) return false;
        }
        return true;
    }

    private boolean isOnlyLiberatorSelected(){
        if(abilityUnits().isEmpty()) return false;
        for(Unit unit : abilityUnits()){
            if(unit == null || !unit.isValid() || !UnitTypes.isLiberator(unit)) return false;
        }
        return true;
    }

    private boolean isOnlySiegeTankSelected(){
        if(abilityUnits().isEmpty()) return false;
        for(Unit unit : abilityUnits()){
            if(unit == null || !unit.isValid() || unit.type != UnitTypes.precept) return false;
        }
        return true;
    }

    private boolean isOnlyCoreFlyerSelected(){
        if(abilityUnits().isEmpty()) return false;
        for(Unit unit : abilityUnits()){
            if(unit == null || !unit.isValid() || unit.type != UnitTypes.coreFlyer) return false;
        }
        return true;
    }

    private boolean isOnlyBarracksStimpackSelected(){
        if(abilityUnits().isEmpty()) return false;
        for(Unit unit : abilityUnits()){
            if(unit == null || !unit.isValid() || !UnitTypes.isBarracksStimpackUnit(unit)) return false;
        }
        return true;
    }

    private boolean isOnlySupplySelected(){
        if(hasAbilityUnits() || abilityBuildings().isEmpty()) return false;
        for(Building build : abilityBuildings()){
            if(!isSupplyDoor(build)) return false;
        }
        return true;
    }

    private boolean isOnlyBunkerSelected(){
        if(hasAbilityUnits() || abilityBuildings().isEmpty()) return false;
        for(Building build : abilityBuildings()){
            if(!(build instanceof BunkerBlock.BunkerBuild)) return false;
        }
        return true;
    }

    private boolean isOnlyRadarSelected(){
        if(hasAbilityUnits() || abilityBuildings().isEmpty()) return false;
        for(Building build : abilityBuildings()){
            if(!(build instanceof Radar.RadarBuild)) return false;
        }
        return true;
    }

    private boolean isOnlyEngineeringSelected(){
        if(hasAbilityUnits() || abilityBuildings().isEmpty()) return false;
        for(Building build : abilityBuildings()){
            if(build == null || !build.isValid() || build.block != Blocks.multiPress) return false;
        }
        return true;
    }

    private boolean isOnlyArmorySelected(){
        if(hasAbilityUnits() || abilityBuildings().isEmpty()) return false;
        for(Building build : abilityBuildings()){
            if(build == null || !build.isValid() || build.block != Blocks.siliconCrucible) return false;
        }
        return true;
    }

    private boolean isOnlyFusionCoreSelected(){
        if(hasAbilityUnits() || abilityBuildings().isEmpty()) return false;
        for(Building build : abilityBuildings()){
            if(build == null || !build.isValid() || build.block != Blocks.surgeCrucible) return false;
        }
        return true;
    }

    private boolean isOnlyGhostAcademySelected(){
        if(hasAbilityUnits() || abilityBuildings().isEmpty()) return false;
        for(Building build : abilityBuildings()){
            if(build == null || !build.isValid() || build.block != Blocks.launchPad) return false;
        }
        return true;
    }

    private boolean isOnlyTechLabSelected(){
        if(hasAbilityUnits() || abilityBuildings().isEmpty()) return false;
        for(Building build : abilityBuildings()){
            if(build == null || !build.isValid() || build.block != Blocks.memoryBank) return false;
        }
        return true;
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

    private @Nullable Block selectedTechLabAttachedFactoryBlock(){
        if(!isOnlyTechLabSelected()) return null;
        Block block = null;
        for(Building build : abilityBuildings()){
            UnitFactory.UnitFactoryBuild factory = attachedFactoryForTechLab(build);
            if(factory == null) return null;
            if(block == null){
                block = factory.block;
            }else if(block != factory.block){
                return null;
            }
        }
        return block;
    }

    private boolean anyRadarCanStartRecycle(){
        for(Building build : abilityBuildings()){
            if(build instanceof Radar.RadarBuild radar && !radar.recycling){
                return true;
            }
        }
        return false;
    }

    private boolean anyBunkerHasGarrison(){
        for(Building build : abilityBuildings()){
            if(build instanceof BunkerBlock.BunkerBuild bunker && bunker.hasGarrison()){
                return true;
            }
        }
        return false;
    }

    private boolean anyBunkerHasSpace(){
        for(Building build : abilityBuildings()){
            if(build instanceof BunkerBlock.BunkerBuild bunker && bunker.freeSlots() > 0 && !bunker.recycling){
                return true;
            }
        }
        return false;
    }

    private boolean anyBunkerCanStartRecycle(){
        for(Building build : abilityBuildings()){
            if(build instanceof BunkerBlock.BunkerBuild bunker && !bunker.recycling){
                return true;
            }
        }
        return false;
    }

    private boolean isSupplyDoor(@Nullable Building build){
        if(build == null) return false;
        return (build.block == Blocks.doorLarge || build.block == Blocks.doorLargeErekir) && build instanceof Door.DoorBuild;
    }

    private boolean isOnlyCoreSelected(){
        if(hasAbilityUnits() || abilityBuildings().isEmpty()) return false;
        for(Building build : abilityBuildings()){
            if(!(build instanceof CoreBuild)) return false;
        }
        return true;
    }

    private @Nullable CoreBuild selectedCore(){
        if(!isOnlyCoreSelected()) return null;
        return (CoreBuild)abilityBuildings().first();
    }

    private boolean anySelectedOrbitalHasEnergy(float amount){
        if(!isOnlyCoreSelected()) return false;
        for(Building build : abilityBuildings()){
            if(build instanceof CoreBuild core && core.block == Blocks.coreOrbital && core.hasOrbitalEnergy(amount)){
                return true;
            }
        }
        return false;
    }

    public boolean isCoreBuildPage(){
        var core = selectedCore();
        if(core == null) return false;
        return corePanel == CorePanel.BUILD || (core.unitQueue != null && !core.unitQueue.isEmpty());
    }

    private void handleNovaHotkeys(){
        if(activeCommand == CommandMode.BUILD_PLACE){
            if(Core.input.keyTap(KeyCode.t)){
                stopSelectedBuilders();
            }else if(Core.input.keyTap(KeyCode.escape)){
                exitCommandMode();
            }
            return;
        }

        switch(novaPanel){
            case MAIN:
                if(Core.input.keyTap(KeyCode.g)){
                    enterCommandMode(CommandMode.HARVEST);
                }else if(Core.input.keyTap(KeyCode.r)){
                    enterCommandMode(CommandMode.REPAIR);
                }else if(Core.input.keyTap(KeyCode.b)){
                    novaPanel = NovaPanel.BUILD_BASIC;
                }else if(Core.input.keyTap(KeyCode.v)){
                    novaPanel = NovaPanel.BUILD_ADV;
                }
                break;
            case BUILD_BASIC:
                if(Core.input.keyTap(KeyCode.escape)){
                    novaPanel = NovaPanel.MAIN;
                    break;
                }
                if(Core.input.keyTap(KeyCode.c)){
                    startPlacement(Blocks.coreNucleus);
                }else if(Core.input.keyTap(KeyCode.r)){
                    startPlacement(Blocks.ventCondenser);
                }else if(Core.input.keyTap(KeyCode.s)){
                    startPlacement(Blocks.doorLarge);
                }else if(Core.input.keyTap(KeyCode.b)){
                    startPlacement(Blocks.groundFactory);
                }else if(Core.input.keyTap(KeyCode.e)){
                    startPlacement(Blocks.multiPress);
                }else if(Core.input.keyTap(KeyCode.u)){
                    startPlacement(Blocks.atmosphericConcentrator);
                }else if(Core.input.keyTap(KeyCode.t)){
                    startPlacement(Blocks.swarmer);
                }else if(Core.input.keyTap(KeyCode.n)){
                    startPlacement(Blocks.radar);
                }
                break;
            case BUILD_ADV:
                if(Core.input.keyTap(KeyCode.escape)){
                    novaPanel = NovaPanel.MAIN;
                    break;
                }
                if(Core.input.keyTap(KeyCode.g)){
                    startPlacement(Blocks.launchPad);
                }else if(Core.input.keyTap(KeyCode.f)){
                    startPlacement(Blocks.tankFabricator);
                }else if(Core.input.keyTap(KeyCode.a)){
                    startPlacement(Blocks.siliconCrucible);
                }else if(Core.input.keyTap(KeyCode.s)){
                    startPlacement(Blocks.shipFabricator);
                }else if(Core.input.keyTap(KeyCode.c)){
                    startPlacement(Blocks.surgeCrucible);
                }
                break;
        }
    }

    private void handleWidowHotkeys(){
        if(Core.input.keyTap(KeyCode.e)){
            issueWidowBurrowCommand(true);
        }else if(Core.input.keyTap(KeyCode.d)){
            issueWidowBurrowCommand(false);
        }
    }

    private void handleMaceLocusHotkeys(){
        if(Core.input.keyTap(KeyCode.e)){
            if(anyMaceCanTransformToLocus()){
                issueMaceLocusModeCommand(true);
            }else if(!UnitTypes.infantryWeaponHasArmory(player.team())){
                ui.hudfrag.setHudText("Requires Armory");
            }
        }else if(Core.input.keyTap(KeyCode.d)){
            if(anyLocusCanTransformToMace()){
                issueMaceLocusModeCommand(false);
            }else if(!UnitTypes.infantryWeaponHasArmory(player.team())){
                ui.hudfrag.setHudText("Requires Armory");
            }
        }
    }

    private void handleMaceHotkeys(){
        if(Core.input.keyTap(KeyCode.e)){
            if(anyMaceCanTransformToLocus()){
                issueMaceLocusModeCommand(true);
            }else if(!UnitTypes.infantryWeaponHasArmory(player.team())){
                ui.hudfrag.setHudText("Requires Armory");
            }
        }
    }

    private void handleLocusHotkeys(){
        if(Core.input.keyTap(KeyCode.d)){
            if(anyLocusCanTransformToMace()){
                issueMaceLocusModeCommand(false);
            }else if(!UnitTypes.infantryWeaponHasArmory(player.team())){
                ui.hudfrag.setHudText("Requires Armory");
            }
        }
    }

    private void handlePreceptHotkeys(){
        if(Core.input.keyTap(KeyCode.e)){
            issuePreceptSiegeCommand(true);
        }else if(Core.input.keyTap(KeyCode.d)){
            issuePreceptSiegeCommand(false);
        }
    }

    private void handleHurricaneHotkeys(){
        if(Core.input.keyTap(KeyCode.c)){
            issueHurricaneLockCommand();
        }
    }

    private void handleScepterHotkeys(){
        if(Core.input.keyTap(KeyCode.e)){
            issueScepterAirModeCommand(true);
        }else if(Core.input.keyTap(KeyCode.d)){
            issueScepterAirModeCommand(false);
        }
    }

    private void handleLiberatorHotkeys(){
        if(activeCommand == CommandMode.LIBERATOR_ZONE){
            if(Core.input.keyTap(KeyCode.escape)){
                exitCommandMode();
            }
            return;
        }

        if(Core.input.keyTap(KeyCode.e) && anyLiberatorCanEnterDefense()){
            enterCommandMode(CommandMode.LIBERATOR_ZONE);
        }else if(Core.input.keyTap(KeyCode.d) && anyLiberatorCanExitDefense()){
            issueLiberatorFighterCommand();
        }
    }

    private void handleMedivacHotkeys(){
        if(activeCommand == CommandMode.MEDIVAC_HEAL || activeCommand == CommandMode.MEDIVAC_LOAD || activeCommand == CommandMode.MEDIVAC_UNLOAD){
            if(Core.input.keyTap(KeyCode.escape)){
                exitCommandMode();
            }
            return;
        }

        if(Core.input.keyTap(KeyCode.e)){
            enterCommandMode(CommandMode.MEDIVAC_HEAL);
        }else if(Core.input.keyTap(KeyCode.b)){
            issueMedivacAfterburnerCommand();
        }else if(Core.input.keyTap(KeyCode.l) && anyMedivacCanLoadMore()){
            enterCommandMode(CommandMode.MEDIVAC_LOAD);
        }else if(Core.input.keyTap(KeyCode.d) && anyMedivacHasPayload()){
            enterCommandMode(CommandMode.MEDIVAC_UNLOAD);
        }
    }

    private void handleVikingHotkeys(){
        if(Core.input.keyTap(KeyCode.e) && anyVikingCanSwitchToFighter()){
            issueVikingModeCommand(false);
        }else if(Core.input.keyTap(KeyCode.d) && anyVikingCanSwitchToMech()){
            issueVikingModeCommand(true);
        }
    }

    private void handleGhostHotkeys(){
        if(activeCommand == CommandMode.GHOST_TACTICAL_NUKE || activeCommand == CommandMode.GHOST_STABLE_AIM || activeCommand == CommandMode.GHOST_EMP){
            if(Core.input.keyTap(KeyCode.escape)){
                exitCommandMode();
            }
            return;
        }

        if(Core.input.keyTap(KeyCode.n)){
            if(anyGhostCanUseTacticalNuke()){
                enterCommandMode(CommandMode.GHOST_TACTICAL_NUKE);
            }else{
                ui.hudfrag.setHudText("No available warhead");
            }
            return;
        }

        if(Core.input.keyTap(KeyCode.r)){
            if(anyGhostCanUseStableAim()){
                enterCommandMode(CommandMode.GHOST_STABLE_AIM);
            }else{
                ui.hudfrag.setHudText("Cannot use Stable Aim");
            }
            return;
        }

        if(Core.input.keyTap(KeyCode.e)){
            if(anyGhostCanUseEmp()){
                enterCommandMode(CommandMode.GHOST_EMP);
            }else{
                ui.hudfrag.setHudText("Cannot use EMP");
            }
            return;
        }

        if(Core.input.keyTap(KeyCode.c)){
            if(anyGhostCanToggleCloak()){
                issueGhostCloakCommand();
            }else if(UnitTypes.ghostCamoLevel(player.team()) <= 0){
                ui.hudfrag.setHudText("Requires Ghost Camouflage");
            }
        }else if(Core.input.keyTap(KeyCode.escape)){
            if(anyGhostTacticalNukePending()){
                issueGhostTacticalNukeCancelCommand();
            }
            if(anyGhostStableAimPending()){
                issueGhostStableAimCancelCommand();
            }
            if(anyGhostEmpPending()){
                issueGhostEmpCancelCommand();
            }
        }
    }

    private void handleReaperHotkeys(){
        if(activeCommand == CommandMode.REAPER_KD8){
            if(Core.input.keyTap(KeyCode.escape)){
                exitCommandMode();
            }
            return;
        }

        if(Core.input.keyTap(KeyCode.d)){
            if(anyReaperCanUseKd8()){
                enterCommandMode(CommandMode.REAPER_KD8);
            }else{
                ui.hudfrag.setHudText("Cannot use KD8 Bomb");
            }
        }
    }

    private void handleBattlecruiserHotkeys(){
        if(activeCommand == CommandMode.BATTLECRUISER_YAMATO || activeCommand == CommandMode.BATTLECRUISER_WARP){
            if(Core.input.keyTap(KeyCode.escape)){
                exitCommandMode();
            }
            return;
        }

        if(Core.input.keyTap(KeyCode.y)){
            if(anyBattlecruiserCanUseYamato()){
                enterCommandMode(CommandMode.BATTLECRUISER_YAMATO);
            }else if(!UnitTypes.battlecruiserHasYamatoTech(player.team())){
                ui.hudfrag.setHudText("Requires Weapon Refit");
            }
        }else if(Core.input.keyTap(KeyCode.t) && anyBattlecruiserCanUseWarp()){
            enterCommandMode(CommandMode.BATTLECRUISER_WARP);
        }
    }

    private void handleBansheeHotkeys(){
        if(Core.input.keyTap(KeyCode.c)){
            if(anyBansheeCanToggleCloak()){
                issueBansheeCloakCommand();
            }else if(UnitTypes.bansheeCloakFieldLevel(player.team()) <= 0){
                ui.hudfrag.setHudText("Requires Cloaking Field");
            }
        }
    }

    private void handleRavenHotkeys(){
        if(activeCommand == CommandMode.RAVEN_ANTI_ARMOR || activeCommand == CommandMode.RAVEN_TURRET || activeCommand == CommandMode.RAVEN_MATRIX){
            if(Core.input.keyTap(KeyCode.escape)){
                exitCommandMode();
            }
            return;
        }

        if(Core.input.keyTap(KeyCode.t) && anyRavenCanDeployTurret()){
            enterCommandMode(CommandMode.RAVEN_TURRET);
        }else if(Core.input.keyTap(KeyCode.r) && anyRavenCanUseAntiArmor()){
            enterCommandMode(CommandMode.RAVEN_ANTI_ARMOR);
        }else if(Core.input.keyTap(KeyCode.c)){
            if(anyRavenCanUseMatrix()){
                enterCommandMode(CommandMode.RAVEN_MATRIX);
            }else if(UnitTypes.ravenMatrixTechLevel(player.team()) <= 0){
                ui.hudfrag.setHudText("Requires Interference Matrix");
            }
        }
    }

    private void handleBarracksStimpackHotkeys(){
        if(Core.input.keyTap(KeyCode.t)){
            issueBarracksStimpackCommand();
        }
    }

    private void handleCoreHotkeys(){
        var core = selectedCore();
        if(core == null) return;

        if(activeCommand == CommandMode.RALLY || activeCommand == CommandMode.DROP_PULSAR
        || activeCommand == CommandMode.EXTRA_SUPPLY || activeCommand == CommandMode.SCAN){
            if(Core.input.keyTap(KeyCode.escape)){
                exitCommandMode();
            }
            return;
        }

        if(Core.input.keyTap(KeyCode.s)){
            queueCoreUnit(core);
            corePanel = CorePanel.BUILD;
        }else if(Core.input.keyTap(KeyCode.b)){
            if(core.block != Blocks.coreNucleus){
                ui.hudfrag.setHudText("Already upgraded");
            }else if(core.unitQueue != null && !core.unitQueue.isEmpty()){
                ui.hudfrag.setHudText("Cannot upgrade while training");
            }else if(core.isUpgrading()){
                ui.hudfrag.setHudText("Upgrade already in progress");
            }else if(!startCoreOrbitalUpgrade(core)){
                ui.hudfrag.setHudText("Not enough crystals");
            }
        }else if(Core.input.keyTap(KeyCode.p)){
            if(core.block != Blocks.coreNucleus){
                ui.hudfrag.setHudText("Already upgraded");
            }else if(core.unitQueue != null && !core.unitQueue.isEmpty()){
                ui.hudfrag.setHudText("Cannot upgrade while training");
            }else if(core.isUpgrading()){
                ui.hudfrag.setHudText("Upgrade already in progress");
            }else if(!core.hasEngineeringStation()){
                ui.hudfrag.setHudText("Requires Engineering Station");
            }else if(!startCoreFortressUpgrade(core)){
                ui.hudfrag.setHudText("Not enough crystals or gas");
            }
        }else if(Core.input.keyTap(KeyCode.y)){
            enterCommandMode(CommandMode.RALLY);
        }else if(Core.input.keyTap(KeyCode.e) && core.block == Blocks.coreOrbital){
            if(anySelectedOrbitalHasEnergy(CoreBlock.orbitalAbilityCost)){
                enterCommandMode(CommandMode.DROP_PULSAR);
            }
        }else if(Core.input.keyTap(KeyCode.x) && core.block == Blocks.coreOrbital){
            if(anySelectedOrbitalHasEnergy(CoreBlock.orbitalAbilityCost)){
                enterCommandMode(CommandMode.EXTRA_SUPPLY);
            }
        }else if(Core.input.keyTap(KeyCode.c) && core.block == Blocks.coreOrbital){
            if(anySelectedOrbitalHasEnergy(CoreBlock.orbitalAbilityCost)){
                enterCommandMode(CommandMode.SCAN);
            }
        }else if(Core.input.keyTap(KeyCode.o) && core.block != Blocks.coreOrbital){
            if(!requestCoreLoadScvs(core)){
                ui.hudfrag.setHudText("No available SCVs or storage full");
            }
        }else if(Core.input.keyTap(KeyCode.d)){
            unloadCoreScvs(core);
        }else if(Core.input.keyTap(KeyCode.l)){
            if(core.canLift()){
                queueCoreLift(core);
            }else{
                ui.hudfrag.setHudText("Cannot lift while training");
            }
        }

        if(Core.input.keyTap(KeyCode.escape)){
            if(core.isUpgradingOrbital()){
                cancelCoreOrbitalUpgrade(core);
            }else if(core.isUpgradingFortress()){
                cancelCoreFortressUpgrade(core);
            }else if(core.unitQueue != null && !core.unitQueue.isEmpty()){
                cancelCoreUnit(core);
            }else{
                corePanel = CorePanel.MAIN;
            }
        }
    }

    private void handleCoreFlyerHotkeys(){
        if(activeCommand == CommandMode.RALLY || activeCommand == CommandMode.LAND){
            if(Core.input.keyTap(KeyCode.escape)){
                exitCommandMode();
            }
            return;
        }

        if(Core.input.keyTap(KeyCode.y)){
            enterCommandMode(CommandMode.RALLY);
        }else if(Core.input.keyTap(KeyCode.l)){
            enterCommandMode(CommandMode.LAND);
        }
    }

    private void issueBunkerStopAttack(){
        for(Building build : abilityBuildings()){
            if(build instanceof BunkerBlock.BunkerBuild){
                build.configure(BunkerBlock.configStopAttack);
            }
        }
    }

    private void issueRadarRecycle(){
        for(Building build : abilityBuildings()){
            if(build instanceof Radar.RadarBuild){
                build.configure(Radar.configRecycle);
            }
        }
    }

    private void issueBunkerUnloadAll(){
        for(Building build : abilityBuildings()){
            if(build instanceof BunkerBlock.BunkerBuild){
                build.configure(BunkerBlock.configUnloadAll);
            }
        }
    }

    private void issueBunkerRecycle(){
        for(Building build : abilityBuildings()){
            if(build instanceof BunkerBlock.BunkerBuild){
                build.configure(BunkerBlock.configRecycle);
            }
        }
    }

    private boolean anyGhostAcademyCanBuildWarhead(){
        for(Building build : abilityBuildings()){
            if(build == null || !build.isValid() || build.block != Blocks.launchPad) continue;
            if(UnitTypes.ghostWarheadCanStartProduction(build)) return true;
        }
        return false;
    }

    private boolean anyGhostAcademyProducingWarhead(){
        for(Building build : abilityBuildings()){
            if(build == null || !build.isValid() || build.block != Blocks.launchPad) continue;
            if(UnitTypes.ghostWarheadProducing(build)) return true;
        }
        return false;
    }

    private float selectedGhostWarheadBuildProgress(){
        float result = 0f;
        for(Building build : abilityBuildings()){
            if(build == null || !build.isValid() || build.block != Blocks.launchPad) continue;
            result = Math.max(result, UnitTypes.ghostWarheadProductionProgress(build));
        }
        return result;
    }

    private void tryStartGhostWarheadProduction(){
        int started = 0;
        for(Building build : abilityBuildings()){
            if(build == null || !build.isValid() || build.block != Blocks.launchPad) continue;
            if(UnitTypes.ghostWarheadStartProduction(build)){
                started++;
            }
        }
        if(started <= 0){
            ui.hudfrag.setHudText("Cannot deploy warhead");
        }
    }


    private void cancelArmoryResearch(){
        if(UnitTypes.armoryCancelAnyResearch(player.team())){
            ui.hudfrag.setHudText("Research cancelled");
        }
    }

    private void cancelFusionCoreResearch(){
        if(ResearchQueueService.fusionCoreCancelAnyResearch(player.team())){
            ui.hudfrag.setHudText("Research cancelled");
        }
    }

    private void tryStartGhostCamoResearch(){
        if(UnitTypes.ghostCamoLevel(player.team()) > 0){
            ui.hudfrag.setHudText("Already researched");
            return;
        }
        if(UnitTypes.ghostCamoAnyResearching(player.team())){
            ui.hudfrag.setHudText("Research already in progress");
            return;
        }
        if(!UnitTypes.ghostCamoStartResearch(player.team())){
            ui.hudfrag.setHudText(Core.bundle.get("bar.noresources", "Not enough resources"));
        }
    }

    private void tryStartResearchSpec(Sc2ResearchSpec spec){
        Team team = player.team();
        if(!spec.displayAvailable(team)){
            ui.hudfrag.setHudText(spec.alreadyMessage);
            return;
        }

        String blocked = spec.blockedReason(team);
        if(blocked != null && !blocked.isEmpty()){
            ui.hudfrag.setHudText(blocked);
            return;
        }

        if(!spec.startResearch(team)){
            ui.hudfrag.setHudText(Core.bundle.get("bar.noresources", "Not enough resources"));
        }
    }

    private void tryStartTechLabResearch(@Nullable Block attachedFactory, Sc2ResearchSpec spec){
        if(selectedTechLabAttachedFactoryBlock() != attachedFactory){
            ui.hudfrag.setHudText(ResearchQueueService.techLabRequirementText(attachedFactory));
            return;
        }
        tryStartResearchSpec(spec);
    }

    private void cancelGhostCamoResearch(){
        if(UnitTypes.ghostCamoCancelAnyResearch(player.team())){
            ui.hudfrag.setHudText("Research cancelled");
        }
    }

    private void cancelTechLabResearch(@Nullable Block attachedFactory){
        if(ResearchQueueService.techLabCancelAny(player.team(), attachedFactory)){
            ui.hudfrag.setHudText("Research cancelled");
        }
    }

    private void cancelInfantryResearch(){
        if(UnitTypes.infantryCancelAnyResearch(player.team())){
            ui.hudfrag.setHudText("Research cancelled");
        }
    }

    private boolean researchHotkeyTapped(String hotkey){
        if(hotkey == null || hotkey.isEmpty()) return false;
        if(hotkey.equalsIgnoreCase("esc")) return Core.input.keyTap(KeyCode.escape);
        if(hotkey.length() != 1) return false;

        try{
            return Core.input.keyTap(KeyCode.valueOf(hotkey.toLowerCase()));
        }catch(IllegalArgumentException ignored){
            return false;
        }
    }

    private void handleBuildingHotkeys(){
        if(abilityBuildings().isEmpty()) return;
        if(activeCommand == CommandMode.RALLY || activeCommand == CommandMode.BUNKER_ATTACK || activeCommand == CommandMode.BUNKER_LOAD){
            if(Core.input.keyTap(KeyCode.escape)){
                exitCommandMode();
            }
            return;
        }
        Building build = abilityBuildings().first();
        if(isOnlyRadarSelected()){
            if(Core.input.keyTap(KeyCode.v) && anyRadarCanStartRecycle()){
                issueRadarRecycle();
            }
            return;
        }
        if(isOnlyBunkerSelected()){
            if(Core.input.keyTap(KeyCode.s) && anyBunkerHasGarrison()){
                issueBunkerStopAttack();
            }else if(Core.input.keyTap(KeyCode.a) && anyBunkerHasGarrison()){
                enterCommandMode(CommandMode.BUNKER_ATTACK);
            }else if(Core.input.keyTap(KeyCode.y)){
                enterCommandMode(CommandMode.RALLY);
            }else if(Core.input.keyTap(KeyCode.l) && anyBunkerHasSpace()){
                enterCommandMode(CommandMode.BUNKER_LOAD);
            }else if(Core.input.keyTap(KeyCode.d) && anyBunkerHasGarrison()){
                issueBunkerUnloadAll();
            }else if(Core.input.keyTap(KeyCode.v) && anyBunkerCanStartRecycle()){
                issueBunkerRecycle();
            }
            return;
        }
        if(isOnlyArmorySelected()){
            Seq<Sc2ResearchSpec> specs = ResearchQueueService.armorySpecs();
            for(int i = 0; i < specs.size; i++){
                Sc2ResearchSpec spec = specs.get(i);
                if(researchHotkeyTapped(spec.hotkey)){
                    tryStartResearchSpec(spec);
                    break;
                }
            }
            if(Core.input.keyTap(KeyCode.escape)){
                cancelArmoryResearch();
            }
            return;
        }
        if(isOnlyFusionCoreSelected()){
            Seq<Sc2ResearchSpec> specs = ResearchQueueService.fusionCoreSpecs();
            for(int i = 0; i < specs.size; i++){
                Sc2ResearchSpec spec = specs.get(i);
                if(researchHotkeyTapped(spec.hotkey)){
                    tryStartResearchSpec(spec);
                    break;
                }
            }
            if(Core.input.keyTap(KeyCode.escape)){
                cancelFusionCoreResearch();
            }
            return;
        }
        if(isOnlyEngineeringSelected()){
            Seq<Sc2ResearchSpec> specs = ResearchQueueService.engineeringSpecs();
            for(int i = 0; i < specs.size; i++){
                Sc2ResearchSpec spec = specs.get(i);
                if(researchHotkeyTapped(spec.hotkey)){
                    tryStartResearchSpec(spec);
                    break;
                }
            }
            if(Core.input.keyTap(KeyCode.escape)){
                cancelInfantryResearch();
            }
            return;
        }
        if(isOnlyGhostAcademySelected()){
            if(Core.input.keyTap(KeyCode.c)){
                tryStartGhostCamoResearch();
            }else if(Core.input.keyTap(KeyCode.n)){
                tryStartGhostWarheadProduction();
            }
            if(Core.input.keyTap(KeyCode.escape)){
                cancelGhostCamoResearch();
            }
            return;
        }
        if(isOnlyTechLabSelected()){
            Block attached = selectedTechLabAttachedFactoryBlock();
            Seq<Sc2ResearchSpec> specs = ResearchQueueService.techLabSpecs(attached);
            for(int i = 0; i < specs.size; i++){
                Sc2ResearchSpec spec = specs.get(i);
                if(researchHotkeyTapped(spec.hotkey)){
                    tryStartTechLabResearch(attached, spec);
                    break;
                }
            }
            if(Core.input.keyTap(KeyCode.escape)){
                cancelTechLabResearch(attached);
            }
            return;
        }
        if(build instanceof UnitFactory.UnitFactoryBuild factory && factory.sc2QueueEnabled()){
            UnitFactory block = (UnitFactory)factory.block;
            if(block == Blocks.groundFactory){
                int daggerIndex = block.plans.indexOf(p -> p.unit == UnitTypes.dagger);
                int reaperIndex = block.plans.indexOf(p -> p.unit == UnitTypes.reaper);
                int fortressIndex = block.plans.indexOf(p -> p.unit == UnitTypes.fortress);
                int ghostIndex = block.plans.indexOf(p -> p.unit == UnitTypes.ghost);

                if(Core.input.keyTap(KeyCode.a) && daggerIndex != -1 && factory.canQueuePlan(daggerIndex)){
                    factory.configure(daggerIndex);
                }else if(Core.input.keyTap(KeyCode.r) && reaperIndex != -1 && factory.canQueuePlan(reaperIndex)){
                    factory.configure(reaperIndex);
                }else if(Core.input.keyTap(KeyCode.d) && fortressIndex != -1 && factory.canQueuePlan(fortressIndex)){
                    factory.configure(fortressIndex);
                }else if(Core.input.keyTap(KeyCode.g) && ghostIndex != -1 && factory.canQueuePlan(ghostIndex)){
                    factory.configure(ghostIndex);
                }
            }else if(block == Blocks.tankFabricator){
                int locusIndex = block.plans.indexOf(p -> p.unit == UnitTypes.locus);
                int crawlerIndex = block.plans.indexOf(p -> p.unit == UnitTypes.crawler);
                int hurricaneIndex = block.plans.indexOf(p -> p.unit == UnitTypes.hurricane);
                int preceptIndex = block.plans.indexOf(p -> p.unit == UnitTypes.precept);
                int maceIndex = block.plans.indexOf(p -> p.unit == UnitTypes.mace);
                int scepterIndex = block.plans.indexOf(p -> p.unit == UnitTypes.scepter);

                if(Core.input.keyTap(KeyCode.e) && locusIndex != -1 && factory.canQueuePlan(locusIndex)){
                    factory.configure(locusIndex);
                }else if(Core.input.keyTap(KeyCode.d) && crawlerIndex != -1 && factory.canQueuePlan(crawlerIndex)){
                    factory.configure(crawlerIndex);
                }else if(Core.input.keyTap(KeyCode.n) && hurricaneIndex != -1 && factory.canQueuePlan(hurricaneIndex)){
                    factory.configure(hurricaneIndex);
                }else if(Core.input.keyTap(KeyCode.s) && preceptIndex != -1 && factory.canQueuePlan(preceptIndex)){
                    factory.configure(preceptIndex);
                }else if(Core.input.keyTap(KeyCode.r) && maceIndex != -1 && factory.canQueuePlan(maceIndex)){
                    factory.configure(maceIndex);
                }else if(Core.input.keyTap(KeyCode.t) && scepterIndex != -1 && factory.canQueuePlan(scepterIndex)){
                    factory.configure(scepterIndex);
                }else if(Core.input.keyTap(KeyCode.y)){
                    enterCommandMode(CommandMode.RALLY);
                }
            }else if(block == Blocks.shipFabricator){
                int flareIndex = block.plans.indexOf(p -> p.unit == UnitTypes.flare);
                int megaIndex = block.plans.indexOf(p -> p.unit == UnitTypes.mega);
                int liberatorIndex = block.plans.indexOf(p -> p.unit == UnitTypes.liberator);
                int avertIndex = block.plans.indexOf(p -> p.unit == UnitTypes.avert);
                int horizonIndex = block.plans.indexOf(p -> p.unit == UnitTypes.horizon);
                int antumbraIndex = block.plans.indexOf(p -> p.unit == UnitTypes.antumbra);

                if(Core.input.keyTap(KeyCode.v) && flareIndex != -1 && factory.canQueuePlan(flareIndex)){
                    factory.configure(flareIndex);
                }else if(Core.input.keyTap(KeyCode.d) && megaIndex != -1 && factory.canQueuePlan(megaIndex)){
                    factory.configure(megaIndex);
                }else if(Core.input.keyTap(KeyCode.n) && liberatorIndex != -1 && factory.canQueuePlan(liberatorIndex)){
                    factory.configure(liberatorIndex);
                }else if(Core.input.keyTap(KeyCode.r) && avertIndex != -1 && factory.canQueuePlan(avertIndex)){
                    factory.configure(avertIndex);
                }else if(Core.input.keyTap(KeyCode.e) && horizonIndex != -1 && factory.canQueuePlan(horizonIndex)){
                    factory.configure(horizonIndex);
                }else if(Core.input.keyTap(KeyCode.b) && antumbraIndex != -1 && factory.canQueuePlan(antumbraIndex)){
                    factory.configure(antumbraIndex);
                }else if(Core.input.keyTap(KeyCode.y)){
                    enterCommandMode(CommandMode.RALLY);
                }
            }else{
                KeyCode[] keyCodes = {KeyCode.a, KeyCode.r, KeyCode.d, KeyCode.g, KeyCode.f, KeyCode.t};
                for(int i = 0; i < keyCodes.length && i < block.plans.size; i++){
                    if(Core.input.keyTap(keyCodes[i]) && factory.canQueuePlan(i)){
                        factory.configure(i);
                        break;
                    }
                }
            }

            if(Core.input.keyTap(KeyCode.l)){
                if(factory.canLift()){
                    queueFactoryLift(factory);
                }else{
                    showFactoryCannotLiftReason(factory);
                }
            }

            if(factory.canShowAddonButtons()){
                if(Core.input.keyTap(KeyCode.x)){
                    factory.configure(UnitFactory.sc2AddonTechConfig);
                }else if(Core.input.keyTap(KeyCode.c)){
                    factory.configure(UnitFactory.sc2AddonDoubleConfig);
                }
            }
            if(Core.input.keyTap(KeyCode.escape)){
                factory.configure(UnitFactory.sc2AddonCancelConfig);
            }
            return;
        }

        if(isOnlySupplySelected()){
            if(Core.input.keyTap(KeyCode.r)){
                toggleSupplyDoors();
            }
            return;
        }
        if(!(build instanceof ConstructBuild)) return;

        ConstructBuild cons = (ConstructBuild)build;
        boolean incomplete = cons.current != null && cons.current != Blocks.air && cons.progress < 1f;
        if(!incomplete) return;

        Unit builder = findActiveBuilder(cons);
        if(builder != null){
            if(Core.input.keyTap(KeyCode.q)){
                selectBuilder(builder);
            }else if(Core.input.keyTap(KeyCode.t)){
                pauseBuilder(builder);
            }
        }

        if(Core.input.keyTap(KeyCode.escape)){
            cancelConstruct(cons);
        }
    }

    private void queueCoreUnit(CoreBuild core){
        if(core == null) return;
        if(core.isUpgrading()){
            ui.hudfrag.setHudText("Cannot train while upgrading");
            return;
        }
        if(!core.canQueueUnit(UnitTypes.nova)){
            if(core.unitQueue != null && core.unitQueue.size >= core.queueSlots()){
                ui.hudfrag.setHudText("Queue full");
            }else{
                ui.hudfrag.setHudText(Core.bundle.get("bar.noresources", "Not enough resources"));
            }
            return;
        }
        Call.coreQueueUnit(player, core.pos(), UnitTypes.nova.id);
    }

    private void cancelCoreUnit(CoreBuild core){
        if(core == null || core.unitQueue == null || core.unitQueue.isEmpty()) return;
        Call.coreCancelUnit(player, core.pos());
    }

    private boolean requestCoreLoadScvs(CoreBuild core){
        if(core == null) return false;
        int free = core.scvStorageLimit() - core.storedScvs - core.loadingScvs.size;
        if(free <= 0) return false;
        if(!core.team.data().units.contains(unit -> unit != null && unit.isValid() && unit.type == UnitTypes.nova && !core.loadingScvs.contains(unit.id))){
            return false;
        }
        Call.coreRequestLoadScvs(player, core.pos());
        return true;
    }

    private void unloadCoreScvs(CoreBuild core){
        if(core == null || core.storedScvs <= 0) return;
        Call.coreUnloadScvs(player, core.pos());
    }

    private boolean startCoreOrbitalUpgrade(CoreBuild core){
        if(core == null || !core.canStartOrbitalUpgrade()) return false;
        Call.coreStartOrbitalUpgrade(player, core.pos());
        return true;
    }

    private void cancelCoreOrbitalUpgrade(CoreBuild core){
        if(core == null || !core.isUpgradingOrbital()) return;
        Call.coreCancelOrbitalUpgrade(player, core.pos());
    }

    private boolean startCoreFortressUpgrade(CoreBuild core){
        if(core == null || !core.canStartFortressUpgrade()) return false;
        Call.coreStartFortressUpgrade(player, core.pos());
        return true;
    }

    private void cancelCoreFortressUpgrade(CoreBuild core){
        if(core == null || !core.isUpgradingFortress()) return;
        Call.coreCancelFortressUpgrade(player, core.pos());
    }

    private void launchCore(CoreBuild core){
        if(core == null) return;
        Call.coreLaunch(player, core.pos());
    }

    private void queueCoreLift(CoreBuild core){
        if(core == null) return;
        triggerCoreLiftPrep(core);
        Time.run(60f, () -> {
            if(core.isValid() && core.canLift()){
                float x = core.x, y = core.y;
                int size = core.block.size;
                Unit unit = core.lift();
                if(unit != null){
                    triggerLiftTakeoffFx(x, y, size);
                    control.input.commandBuildings.clear();
                    control.input.selectedUnits.clear();
                    control.input.selectedUnits.add(unit);
                }
            }
        });
    }

    private void showFactoryCannotLiftReason(UnitFactory.UnitFactoryBuild factory){
        if(factory == null) return;
        if(UnitTypes.factoryTechResearching(factory)){
            ui.hudfrag.setHudText("Cannot lift while tech research is in progress");
        }else{
            ui.hudfrag.setHudText("Cannot lift while training");
        }
    }

    private void queueFactoryLift(UnitFactory.UnitFactoryBuild factory){
        if(factory == null) return;
        if(!factory.canLift()){
            showFactoryCannotLiftReason(factory);
            return;
        }
        triggerFactoryLiftPrep(factory);
        Time.run(60f, () -> {
            if(factory.isValid() && factory.canLift()){
                float x = factory.x, y = factory.y;
                int size = factory.block.size;
                Unit unit = factory.lift();
                if(unit != null){
                    triggerLiftTakeoffFx(x, y, size);
                    control.input.commandBuildings.clear();
                    control.input.selectedUnits.clear();
                    control.input.selectedUnits.add(unit);
                }
            }
        });
    }

    private void triggerCoreLiftPrep(CoreBuild core){
        if(core == null || !core.isValid()) return;
        core.thrusterTime = Math.max(core.thrusterTime, 1f);
        Fx.coreLaunchConstruct.at(core.x, core.y, core.block.size);
    }

    private void triggerFactoryLiftPrep(UnitFactory.UnitFactoryBuild factory){
        if(factory == null || !factory.isValid()) return;
        factory.liftThrusterTime = Math.max(factory.liftThrusterTime, 1f);
        Fx.coreLaunchConstruct.at(factory.x, factory.y, factory.block.size);
    }

    private void triggerLiftTakeoffFx(float x, float y, int size){
        Fx.coreLaunchConstruct.at(x, y, size);
        Effect.shake(5f, 5f, x, y);
        if(!headless){
            Sounds.coreLaunch.at(x, y, 1f, 1f);
        }
    }

    private void showNotImplemented(){
        ui.hudfrag.setHudText("Not implemented");
    }

    private void startPlacement(Block block){
        if(block == null) return;
        if(!Build.meetsPrerequisites(block, player.team())) return;
        if(!canAfford(block)){
            ui.hudfrag.setHudText(Core.bundle.get("bar.noresources", "Not enough resources"));
            return;
        }
        placingBlock = block;
        activeCommand = CommandMode.BUILD_PLACE;
        novaPanel = NovaPanel.MAIN;
    }

    public @Nullable Block getPlacingBlock(){
        return placingBlock;
    }

    private Unit findActiveBuilder(ConstructBuild cons){
        if(cons == null) return null;
        Unit builder = cons.lastBuilder;
        if(builder != null && builder.isValid() && builder.activelyBuilding()){
            BuildPlan plan = builder.buildPlan();
            if(plan != null && plan.x == cons.tile.x && plan.y == cons.tile.y){
                return builder;
            }
        }
        for(Unit u : Groups.unit){
            if(u.team == cons.team && u.isValid() && u.activelyBuilding()){
                BuildPlan plan = u.buildPlan();
                if(plan != null && plan.x == cons.tile.x && plan.y == cons.tile.y){
                    return u;
                }
            }
        }
        return null;
    }

    private void selectBuilder(Unit builder){
        if(builder == null) return;
        control.input.selectedUnits.clear();
        control.input.commandBuildings.clear();
        control.input.selectedUnits.add(builder);
        Events.fire(Trigger.unitCommandChange);
    }

    private void pauseBuilder(Unit builder){
        if(builder == null) return;
        builder.clearBuilding();
    }

    private void stopSelectedBuilders(){
        if(abilityUnits().isEmpty()) return;
        for(Unit unit : abilityUnits()){
            if(unit == null || !unit.isValid() || !unit.canBuild()) continue;
            unit.clearBuilding();
            unit.updateBuilding(false);
        }
    }

    private void cancelConstruct(ConstructBuild cons){
        if(cons == null) return;
        mindustry.world.blocks.ConstructBlock.consumePrepaid(cons.tile.pos());
        mindustry.world.blocks.ConstructBlock.clearForceBuildTime(cons.tile.pos());
        Block block = cons.current;
        Building core = cons.team.core();
        if(core != null && block != null){
            for(ItemStack stack : block.requirements){
                int amount = Mathf.round(stack.amount * state.rules.buildCostMultiplier);
                int refund = Mathf.ceil(amount * 0.75f);
                if(refund > 0){
                    core.items.add(stack.item, refund);
                }
            }
        }
        Fx.blockExplosionSmoke.at(cons.x, cons.y);
        cons.tile.remove();
        control.input.commandBuildings.clear();
    }

    public static boolean canAfford(Block block){
        if(block == null) return false;
        Building core = player.core();
        if(core == null) return false;
        for(ItemStack stack : block.requirements){
            int amount = Mathf.round(stack.amount * state.rules.buildCostMultiplier);
            if(amount > 0 && !core.items.has(stack.item, amount)){
                return false;
            }
        }
        return true;
    }

    public static void payPlacementCost(Block block){
        if(block == null) return;
        Building core = player.core();
        if(core == null) return;
        for(ItemStack stack : block.requirements){
            int amount = Mathf.round(stack.amount * state.rules.buildCostMultiplier);
            if(amount > 0){
                core.items.remove(stack.item, amount);
            }
        }
    }

    private void buildCommandModePanel(){
        setPanelRows(3);
        //Add cancel button at row 3, column 5 (index 14)
        Table buttonGrid = new Table();
        for(int i = 0; i < ROWS * COLS; i++){
            if(i == 14){ //Row 3, Column 5 (0-indexed: row 2, col 4)
                addIconButton(buttonGrid, "Esc", Icon.cancel, () -> true, this::exitCommandMode, targetHintInfo);
            }else{
                addEmpty(buttonGrid);
            }

            if((i + 1) % COLS == 0){
                buttonGrid.row();
            }
        }
        add(buttonGrid);
    }

    private void enterCommandMode(CommandMode mode){
        activeCommand = mode;
    }

    public void exitCommandMode(){
        if(activeCommand == CommandMode.BUILD_PLACE){
            placingBlock = null;
            novaPanel = NovaPanel.MAIN;
        }
        activeCommand = CommandMode.NONE;
    }

    private void executeStopCommand(){
        //Stop command executes immediately - clear all unit commands
        int[] ids = new int[abilityUnits().size];
        for(int i = 0; i < ids.length; i++){
            ids[i] = abilityUnits().get(i).id;
        }
        if(ids.length > 0){
            Call.setUnitCommand(player, ids, UnitCommand.moveCommand);
            Call.commandMedivacMovingUnload(player, ids, false);
            //Send stop command (move to current position)
            for(Unit unit : abilityUnits()){
                if(unit.isValid()){
                    Call.commandUnits(player, new int[]{unit.id}, null, null, new Vec2(unit.x, unit.y), false, true, false);
                }
            }
        }
        exitCommandMode();
    }

    private void executeHoldCommand(){
        //Hold command executes immediately - units hold position
        int[] ids = new int[abilityUnits().size];
        for(int i = 0; i < ids.length; i++){
            ids[i] = abilityUnits().get(i).id;
        }
        if(ids.length > 0){
            Call.setUnitCommand(player, ids, UnitCommand.moveCommand);
            Call.commandMedivacMovingUnload(player, ids, false);
        }
        for(Unit unit : abilityUnits()){
            if(unit.isValid()){
                //Send hold command (move to current position, will be interpreted as hold)
                Call.commandUnits(player, new int[]{unit.id}, null, null, new Vec2(unit.x, unit.y), false, true, false);
            }
        }
        exitCommandMode();
    }
}


