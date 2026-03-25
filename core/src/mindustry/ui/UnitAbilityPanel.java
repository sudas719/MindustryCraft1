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
import mindustry.input.Sc2AbilityHotkeys.Ability;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.ConstructBlock.*;
import mindustry.world.blocks.defense.*;
import mindustry.world.blocks.storage.*;
import mindustry.world.blocks.storage.CoreBlock.*;
import mindustry.world.blocks.units.*;

import java.util.Locale;

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
    private static final float HOVER_INFO_GAP = 6f;

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
    private int lastRebuildHash = Integer.MIN_VALUE;
    private final IntIntMap autoCastFlags = new IntIntMap();
    private float nextAutoCastUpdate = 0f;

    //Factory multi-selection distribution state (SC2-like)
    private static final int factoryCatDouble = 0;
    private static final int factoryCatNone = 1;
    private static final int factoryCatTech = 2;
    private final IntIntMap factoryDistributeLastFactoryId = new IntIntMap();
    private final IntIntMap factoryDistributeLastDoubleId = new IntIntMap();
    private final IntIntMap factoryDistributeLastNoneId = new IntIntMap();
    private final IntIntMap factoryDistributeLastTechId = new IntIntMap();
    private final IntSeq factoryDistributeHistoryBlock = new IntSeq();
    private final IntSeq factoryDistributeHistoryFactory = new IntSeq();

    //Core (base) multi-selection distribution state (SC2-like)
    private final IntSeq coreDistributeHistoryCore = new IntSeq();
    private static final int coreUpgradeOrbital = 0;
    private static final int coreUpgradeFortress = 1;
    private final IntSeq coreDistributeHistoryUpgradeCore = new IntSeq();
    private final IntSeq coreDistributeHistoryUpgradeType = new IntSeq();
    private final IntIntMap factoryDistributeLastAddonFactoryId = new IntIntMap();
    private final IntIntMap coreDistributeLastUpgradeCoreId = new IntIntMap();

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
        float timeSeconds = -1f;
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
        @Nullable String costLineOverride;
        int crystalCost = -1;
        int gasCost = -1;
        float timeSeconds = -1f;
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

            rebuildIfNeeded();
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

    private int rebuildHash(){
        int hash = 1;
        hash = hash * 31 + activeCommand.ordinal();
        hash = hash * 31 + novaPanel.ordinal();
        hash = hash * 31 + corePanel.ordinal();
        hash = hash * 31 + (placingBlock == null ? -1 : placingBlock.id);
        hash = hash * 31 + Sc2AbilityHotkeys.revision();

        Seq<Unit> units = abilityUnits();
        hash = hash * 31 + units.size;
        for(int i = 0; i < units.size; i++){
            Unit unit = units.get(i);
            hash = hash * 31 + unit.id;
            hash = hash * 31 + (unit.type == null ? -1 : unit.type.id);
            hash = hash * 31 + unitPanelStateHash(unit);
        }

        Seq<Building> builds = abilityBuildings();
        hash = hash * 31 + builds.size;
        for(int i = 0; i < builds.size; i++){
            Building build = builds.get(i);
            hash = hash * 31 + build.id;
            hash = hash * 31 + (build.block == null ? -1 : build.block.id);
            hash = hash * 31 + build.rotation;
        }

        return hash;
    }

    private int unitPanelStateHash(@Nullable Unit unit){
        if(unit == null || !unit.isValid()) return 0;

        int hash = 1;

        if(UnitTypes.isWidow(unit)){
            hash = hash * 31 + (UnitTypes.widowIsBuried(unit) ? 1 : 0);
            hash = hash * 31 + (UnitTypes.widowIsBurrowing(unit) ? 1 : 0);
            hash = hash * 31 + (UnitTypes.widowIsUnburrowing(unit) ? 1 : 0);
        }

        if(UnitTypes.isViking(unit)){
            hash = hash * 31 + (UnitTypes.vikingIsMechMode(unit) ? 1 : 0);
            hash = hash * 31 + (UnitTypes.vikingIsTransforming(unit) ? 1 : 0);
        }

        if(UnitTypes.isMace(unit) || UnitTypes.isLocus(unit)){
            hash = hash * 31 + (UnitTypes.maceLocusTransforming(unit) ? 1 : 0);
        }

        if(UnitTypes.isLiberator(unit)){
            hash = hash * 31 + (UnitTypes.liberatorIsDefending(unit) ? 1 : 0);
            hash = hash * 31 + (UnitTypes.liberatorIsDeploying(unit) ? 1 : 0);
            hash = hash * 31 + (UnitTypes.liberatorIsUndeploying(unit) ? 1 : 0);
        }

        if(UnitTypes.isSiegeTank(unit)){
            hash = hash * 31 + (UnitTypes.preceptIsSieged(unit) ? 1 : 0);
            hash = hash * 31 + (UnitTypes.preceptIsSieging(unit) ? 1 : 0);
            hash = hash * 31 + (UnitTypes.preceptIsUnsieging(unit) ? 1 : 0);
        }

        return hash;
    }

    private void rebuildIfNeeded(){
        int next = rebuildHash();
        if(next != lastRebuildHash){
            lastRebuildHash = next;
            rebuild();
        }
    }

    private void rebuild(){
        clearChildren();
        clearPanelSize();
        hoverAbilityInfo = null;
        hoverBuildInfo = null;

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

        AbilityInfo stimpackInfo = makeAbilityInfo(Ability.stimpack, "Stimpack", "Consumes health to temporarily boost Marine and Marauder mobility and attack output.");
        stimpackInfo.costLineOverride = selectedBarracksStimpackCostLine();
        addBattlecruiserCooldownButton(grid, hotkey(Ability.stimpack), Icon.upOpen, this::anyBarracksStimpackSelectedCanUse,
        this::issueBarracksStimpackCommand,
        this::selectedBarracksStimpackCooldown, UnitTypes::barracksStimpackCooldownDuration, stimpackInfo);
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
            Button burrowButton = addIconButton(grid, hotkey(Ability.widowBurrow), Icon.downOpen, this::anyWidowCanBurrow, () -> issueWidowBurrowCommand(true));
            BuildInfo burrowInfo = makeWidowActionInfo(hotkey(Ability.widowBurrow), "Widow Burrow", Color.cyan, this::selectedWidowBurrowProgress, this::anyWidowBurrowing, UnitTypes.widowBurrowDuration() / 60f);
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
            Button unburrowButton = addIconButton(grid, hotkey(Ability.widowUnburrow), Icon.upOpen, this::anyWidowCanUnburrow, () -> issueWidowBurrowCommand(false));
            BuildInfo reloadInfo = makeWidowActionInfo(hotkey(Ability.widowUnburrow), "Widow Reload", Color.gray, this::selectedWidowReloadProgress, this::anyWidowReloading, UnitTypes.widowReloadDuration() / 60f);
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

        AbilityInfo kd8Info = makeAbilityInfo(Ability.reaperKd8, "KD8 Bomb", "Throw a timed bomb that detonates after 1.5s. Deals 5 pierce damage and knocks back light targets.");
        kd8Info.timeSeconds = UnitTypes.reaperKd8ArmTimeDuration() / 60f;
        addCooldownIconButton(grid, hotkey(Ability.reaperKd8), Icon.warning, this::anyReaperCanUseKd8, () -> enterCommandMode(CommandMode.REAPER_KD8),
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
            AbilityInfo impactInfo = makeAbilityInfo(Ability.thorHighImpact, "High Impact Payload", "Thor switches to high-impact anti-air payload for stronger single-target air damage.");
            impactInfo.timeSeconds = UnitTypes.scepterSwitchDuration(player.team()) / 60f;
            addIconButton(grid, hotkey(Ability.thorHighImpact), Icon.upOpen, this::anyScepterCanSwitchToImpact, () -> issueScepterAirModeCommand(true), impactInfo);
        }else{
            addEmpty(grid);
        }

        if(anyScepterCanSwitchToBurst()){
            AbilityInfo burstInfo = makeAbilityInfo(Ability.thorExplosive, "Explosive Payload", "Thor switches to explosive anti-air payload, better against light air units.");
            burstInfo.timeSeconds = UnitTypes.scepterSwitchDuration(player.team()) / 60f;
            addIconButton(grid, hotkey(Ability.thorExplosive), Icon.downOpen, this::anyScepterCanSwitchToBurst, () -> issueScepterAirModeCommand(false), burstInfo);
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

        addAutoCastIconButton(grid, hotkey(Ability.medivacHeal), Icon.add, () -> true,
        () -> enterCommandMode(CommandMode.MEDIVAC_HEAL),
        this::selectedMedivacHealAutoCastEnabled, this::toggleSelectedMedivacHealAutoCast,
        makeAbilityInfo(Ability.medivacHeal, "Heal", "Continuously restores health to biological allied units. Right-click toggles autocast."));
        addIconButton(grid, hotkey(Ability.medivacAfterburners), Icon.upOpen, () -> true, this::issueMedivacAfterburnerCommand,
        makeAbilityInfo(Ability.medivacAfterburners, "Afterburners", "Grants a short burst of movement speed for pursuit, retreat, or repositioning."));

        if(anyMedivacCanLoadMore()){
            addIconButton(grid, hotkey(Ability.medivacLoad), Icon.upload, this::anyMedivacCanLoadMore, () -> enterCommandMode(CommandMode.MEDIVAC_LOAD),
            makeAbilityInfo(Ability.medivacLoad, "Load", "Loads nearby friendly ground units into the transport bay."));
        }else{
            addEmpty(grid);
        }

        if(anyMedivacHasPayload()){
            addIconButton(grid, hotkey(Ability.medivacUnload), Icon.download, this::anyMedivacHasPayload, () -> enterCommandMode(CommandMode.MEDIVAC_UNLOAD),
            makeAbilityInfo(Ability.medivacUnload, "Unload", "Unloads units currently carried in the transport bay."));
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

        AbilityInfo nukeInfo = makeAbilityInfo(Ability.ghostNuke, "Tactical Nuke", "Calls down a nuclear strike on the target area. Requires a prepared warhead.");
        nukeInfo.timeSeconds = 14f;
        addCountedIconButton(grid, hotkey(Ability.ghostNuke), Icon.warning, this::anyGhostCanUseTacticalNuke, () -> enterCommandMode(CommandMode.GHOST_TACTICAL_NUKE), this::selectedGhostWarheadCount, nukeInfo);
        addEmpty(grid);
        addEmpty(grid);
        addEmpty(grid);
        addEmpty(grid);
        grid.row();

        AbilityInfo stableAimInfo = makeAbilityInfo(Ability.ghostStableAim, "Stable Aim", "Aims, then fires a high-damage snipe with extra effect against psionic targets.");
        stableAimInfo.timeSeconds = 1.43f;
        addIconButton(grid, hotkey(Ability.ghostStableAim), Icon.warning, this::anyGhostCanUseStableAim, () -> enterCommandMode(CommandMode.GHOST_STABLE_AIM), stableAimInfo);
        addIconButton(grid, hotkey(Ability.ghostEmp), Icon.warning, this::anyGhostCanUseEmp, () -> enterCommandMode(CommandMode.GHOST_EMP),
        makeAbilityInfo(Ability.ghostEmp, "EMP Round", "Fires an EMP round that removes shields, burns psionic energy, and reveals cloaked targets."));
        addIconButton(grid, hotkey(Ability.ghostCloak), Icon.eyeSmall, this::anyGhostCanToggleCloak, this::issueGhostCloakCommand,
        makeAbilityInfo(Ability.ghostCloak, "Cloak", "Enters cloak and continuously drains energy."));
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
            AbilityInfo fighterInfo = makeAbilityInfo(Ability.vikingFighterMode, "Fighter Mode", "Transform to Fighter Mode.");
            fighterInfo.timeSeconds = UnitTypes.vikingTransformDuration(player.team()) / 60f;
            addIconButton(grid, hotkey(Ability.vikingFighterMode), Icon.upOpen, this::anyVikingCanSwitchToFighter, () -> issueVikingModeCommand(false), fighterInfo);
        }else{
            addEmpty(grid);
        }
        if(anyVikingCanSwitchToMech()){
            AbilityInfo mechInfo = makeAbilityInfo(Ability.vikingMechMode, "Mech Mode", "Transform to Mech Mode.");
            mechInfo.timeSeconds = UnitTypes.vikingTransformDuration(player.team()) / 60f;
            addIconButton(grid, hotkey(Ability.vikingMechMode), Icon.downOpen, this::anyVikingCanSwitchToMech, () -> issueVikingModeCommand(true), mechInfo);
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
            AbilityInfo locusInfo = makeAbilityInfo(Ability.hellionToHellbat, "Locus Mode", "Transform to Locus. Requires Armory.");
            locusInfo.timeSeconds = UnitTypes.maceLocusTransformDuration(player.team()) / 60f;
            addIconButton(
                grid, hotkey(Ability.hellionToHellbat), Icon.upOpen, this::anyMaceCanTransformToLocus,
                () -> issueMaceLocusModeCommand(true),
                locusInfo
            );
        }else{
            addEmpty(grid);
        }

        if(anyLocusSelected()){
            AbilityInfo maceInfo = makeAbilityInfo(Ability.hellbatToHellion, "Mace Mode", "Transform to Mace. Requires Armory.");
            maceInfo.timeSeconds = UnitTypes.maceLocusTransformDuration(player.team()) / 60f;
            addIconButton(
                grid, hotkey(Ability.hellbatToHellion), Icon.downOpen, this::anyLocusCanTransformToMace,
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

        AbilityInfo locusInfo = makeAbilityInfo(Ability.hellionToHellbat, "Locus Mode", "Transform to Locus. Requires Armory.");
        locusInfo.timeSeconds = UnitTypes.maceLocusTransformDuration(player.team()) / 60f;
        addIconButton(
            grid, hotkey(Ability.hellionToHellbat), Icon.upOpen, this::anyMaceCanTransformToLocus,
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
        AbilityInfo maceInfo = makeAbilityInfo(Ability.hellbatToHellion, "Mace Mode", "Transform to Mace. Requires Armory.");
        maceInfo.timeSeconds = UnitTypes.maceLocusTransformDuration(player.team()) / 60f;
        addIconButton(
            grid, hotkey(Ability.hellbatToHellion), Icon.downOpen, this::anyLocusCanTransformToMace,
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

        addIconButton(grid, hotkey(Ability.ravenTurret), Icon.add, this::anyRavenCanDeployTurret, () -> enterCommandMode(CommandMode.RAVEN_TURRET),
        makeAbilityInfo(Ability.ravenTurret, "Auto Turret", "Deploys a temporary auto turret at the target point."));
        addIconButton(grid, hotkey(Ability.ravenAntiArmor), Icon.downOpen, this::anyRavenCanUseAntiArmor, () -> enterCommandMode(CommandMode.RAVEN_ANTI_ARMOR),
        makeAbilityInfo(Ability.ravenAntiArmor, "Anti-Armor Missile", "Launches an anti-armor missile that makes units in the area take extra damage."));
        addIconButton(grid, hotkey(Ability.ravenMatrix), Icon.warning, this::anyRavenCanUseMatrix, () -> enterCommandMode(CommandMode.RAVEN_MATRIX),
        makeAbilityInfo(Ability.ravenMatrix, "Interference Matrix", "Disables an enemy mechanical unit for a short duration."));
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

        addIconButton(grid, hotkey(Ability.bansheeCloak), Icon.eyeSmall, this::anyBansheeCanToggleCloak, this::issueBansheeCloakCommand,
        makeAbilityInfo(Ability.bansheeCloak, "Cloak", "Enters cloak and continuously drains energy."));
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

        AbilityInfo yamatoInfo = makeAbilityInfo(Ability.battlecruiserYamato, "Yamato Cannon", "Charges up and deals massive damage to a single target.");
        yamatoInfo.timeSeconds = 2f;
        addBattlecruiserCooldownButton(grid, hotkey(Ability.battlecruiserYamato), Icon.warning, this::anyBattlecruiserCanUseYamato,
        () -> enterCommandMode(CommandMode.BATTLECRUISER_YAMATO),
        this::selectedBattlecruiserYamatoCooldown, UnitTypes::battlecruiserYamatoCooldownDuration, yamatoInfo);
        AbilityInfo warpInfo = makeAbilityInfo(Ability.battlecruiserJump, "Tactical Jump", "Charges briefly, then warps to the selected location.");
        warpInfo.timeSeconds = 1f;
        addBattlecruiserCooldownButton(grid, hotkey(Ability.battlecruiserJump), Icon.effect, this::anyBattlecruiserCanUseWarp,
        () -> enterCommandMode(CommandMode.BATTLECRUISER_WARP),
        this::selectedBattlecruiserWarpCooldown, UnitTypes::battlecruiserWarpCooldownDuration, warpInfo);
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
            AbilityInfo defenseInfo = makeAbilityInfo(Ability.liberatorDefenseMode, "Defense Mode", "Deploys into defense mode and can only attack ground targets inside the defense circle.");
            defenseInfo.timeSeconds = UnitTypes.smartServosLevel(player.team()) > 0 ? 2f : 4f;
            addIconButton(grid, hotkey(Ability.liberatorDefenseMode), Icon.downOpen, this::anyLiberatorCanEnterDefense, () -> enterCommandMode(CommandMode.LIBERATOR_ZONE), defenseInfo);
        }else{
            addEmpty(grid);
        }

        if(anyLiberatorCanExitDefense()){
            AbilityInfo fighterInfo = makeAbilityInfo(Ability.liberatorFighterMode, "Fighter Mode", "Switches into a mobile fighter configuration.");
            fighterInfo.timeSeconds = UnitTypes.smartServosLevel(player.team()) > 0 ? 2f : 1.5f;
            addIconButton(grid, hotkey(Ability.liberatorFighterMode), Icon.upOpen, this::anyLiberatorCanExitDefense, this::issueLiberatorFighterCommand, fighterInfo);
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
            AbilityInfo siegeInfo = makeAbilityInfo(Ability.siegeTankSiegeMode, "Siege Mode", "Deploys into siege mode for long-range anti-ground splash fire, but the tank cannot move.");
            siegeInfo.timeSeconds = UnitTypes.preceptTransitionDuration() / 60f;
            addIconButton(grid, hotkey(Ability.siegeTankSiegeMode), Icon.downOpen, this::anyPreceptCanSiege, () -> issuePreceptSiegeCommand(true), siegeInfo);
        }else{
            addEmpty(grid);
        }

        if(anyPreceptCanTankMode()){
            AbilityInfo tankInfo = makeAbilityInfo(Ability.siegeTankTankMode, "Tank Mode", "Packs up and returns to the mobile tank configuration.");
            tankInfo.timeSeconds = UnitTypes.preceptTransitionDuration() / 60f;
            addIconButton(grid, hotkey(Ability.siegeTankTankMode), Icon.upOpen, this::anyPreceptCanTankMode, () -> issuePreceptSiegeCommand(false), tankInfo);
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
        addIconButton(grid, hotkey(Ability.novaHarvest), Icon.terrain, () -> true, () -> enterCommandMode(CommandMode.HARVEST));
        addIconButton(grid, hotkey(Ability.novaRepair), UnitCommand.repairCommand.getIcon(), () -> true, () -> enterCommandMode(CommandMode.REPAIR));
        fillRow(grid, 1, 2);
        grid.row();

        //Row 3
        addIconButton(grid, hotkey(Ability.novaBuildBasic), Icon.hammer, () -> true, () -> novaPanel = NovaPanel.BUILD_BASIC);
        addIconButton(grid, hotkey(Ability.novaBuildAdvanced), Icon.wrench, () -> true, () -> novaPanel = NovaPanel.BUILD_ADV);
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
        addBuildButton(grid, hotkey(Ability.novaBuildCommandCenter), Blocks.coreNucleus, () -> true, () -> startPlacement(Blocks.coreNucleus));
        addBuildButton(grid, hotkey(Ability.novaBuildRefinery), Blocks.ventCondenser, () -> true, () -> startPlacement(Blocks.ventCondenser));
        addBuildButton(grid, hotkey(Ability.novaBuildSupplyDepot), Blocks.doorLarge, () -> Build.meetsPrerequisites(Blocks.doorLarge, player.team()), () -> startPlacement(Blocks.doorLarge));
        fillRow(grid, 0, 3);
        grid.row();

        //Row 2
        addBuildButton(grid, hotkey(Ability.novaBuildBarracks), Blocks.groundFactory, () -> Build.meetsPrerequisites(Blocks.groundFactory, player.team()), () -> startPlacement(Blocks.groundFactory));
        addBuildButton(grid, hotkey(Ability.novaBuildEngineeringBay), Blocks.multiPress, () -> Build.meetsPrerequisites(Blocks.multiPress, player.team()), () -> startPlacement(Blocks.multiPress));
        fillRow(grid, 1, 2);
        grid.row();

        //Row 3
        addBuildButton(grid, hotkey(Ability.novaBuildBunker), Blocks.atmosphericConcentrator, () -> Build.meetsPrerequisites(Blocks.atmosphericConcentrator, player.team()), () -> startPlacement(Blocks.atmosphericConcentrator));
        addBuildButton(grid, hotkey(Ability.novaBuildMissileTurret), Blocks.swarmer, () -> Build.meetsPrerequisites(Blocks.swarmer, player.team()), () -> startPlacement(Blocks.swarmer));
        addBuildButton(grid, hotkey(Ability.novaBuildSensorTower), Blocks.radar, () -> Build.meetsPrerequisites(Blocks.radar, player.team()), () -> startPlacement(Blocks.radar));
        addEmpty(grid);
        addEscButton(grid, () -> novaPanel = NovaPanel.MAIN);
        add(grid);
    }

    private void buildNovaAdvancedPanel(){
        setPanelRows(3);
        Table grid = new Table();
        //Row 1
        addBuildButton(grid, hotkey(Ability.novaBuildGhostAcademy), Blocks.launchPad, () -> Build.meetsPrerequisites(Blocks.launchPad, player.team()), () -> startPlacement(Blocks.launchPad));
        fillRow(grid, 0, 1);
        grid.row();

        //Row 2
        addBuildButton(grid, hotkey(Ability.novaBuildFactory), Blocks.tankFabricator, () -> Build.meetsPrerequisites(Blocks.tankFabricator, player.team()), () -> startPlacement(Blocks.tankFabricator));
        addBuildButton(grid, hotkey(Ability.novaBuildArmory), Blocks.siliconCrucible, () -> Build.meetsPrerequisites(Blocks.siliconCrucible, player.team()), () -> startPlacement(Blocks.siliconCrucible));
        fillRow(grid, 1, 2);
        grid.row();

        //Row 3
        addBuildButton(grid, hotkey(Ability.novaBuildStarport), Blocks.shipFabricator, () -> Build.meetsPrerequisites(Blocks.shipFabricator, player.team()), () -> startPlacement(Blocks.shipFabricator));
        addBuildButton(grid, hotkey(Ability.novaBuildFusionCore), Blocks.surgeCrucible, () -> Build.meetsPrerequisites(Blocks.surgeCrucible, player.team()), () -> startPlacement(Blocks.surgeCrucible));
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
        boolean showAddonButtons = anyAbilityFactoryCanShowAddon(factory, Blocks.memoryBank) || anyAbilityFactoryCanShowAddon(factory, Blocks.rotaryPump);
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
                addUnitButton(grid, "e", block.plans.get(locusIndex), () -> anyAbilityFactoryCanQueue(factory, locusIndex), () -> queueAbilityFactoryPlan(factory, locusIndex));
            }else{
                addEmpty(grid);
            }
            if(crawlerIndex != -1){
                addUnitButton(grid, "d", block.plans.get(crawlerIndex), () -> anyAbilityFactoryCanQueue(factory, crawlerIndex), () -> queueAbilityFactoryPlan(factory, crawlerIndex));
            }else{
                addEmpty(grid);
            }
            if(hurricaneIndex != -1){
                addUnitButton(grid, "n", block.plans.get(hurricaneIndex), () -> anyAbilityFactoryCanQueue(factory, hurricaneIndex), () -> queueAbilityFactoryPlan(factory, hurricaneIndex));
            }else{
                addEmpty(grid);
            }
            if(preceptIndex != -1){
                addUnitButton(grid, "s", block.plans.get(preceptIndex), () -> anyAbilityFactoryCanQueue(factory, preceptIndex), () -> queueAbilityFactoryPlan(factory, preceptIndex));
            }else{
                addEmpty(grid);
            }
            addEmpty(grid);
            grid.row();

            //Row 2
            if(maceIndex != -1){
                addUnitButton(grid, "r", block.plans.get(maceIndex), () -> anyAbilityFactoryCanQueue(factory, maceIndex), () -> queueAbilityFactoryPlan(factory, maceIndex));
            }else{
                addEmpty(grid);
            }
            if(scepterIndex != -1){
                addUnitButton(grid, "t", block.plans.get(scepterIndex), () -> anyAbilityFactoryCanQueue(factory, scepterIndex), () -> queueAbilityFactoryPlan(factory, scepterIndex));
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
                addUnitButton(grid, "v", block.plans.get(flareIndex), () -> anyAbilityFactoryCanQueue(factory, flareIndex), () -> queueAbilityFactoryPlan(factory, flareIndex));
            }else{
                addEmpty(grid);
            }
            if(megaIndex != -1){
                addUnitButton(grid, "d", block.plans.get(megaIndex), () -> anyAbilityFactoryCanQueue(factory, megaIndex), () -> queueAbilityFactoryPlan(factory, megaIndex));
            }else{
                addEmpty(grid);
            }
            if(liberatorIndex != -1){
                addUnitButton(grid, "n", block.plans.get(liberatorIndex), () -> anyAbilityFactoryCanQueue(factory, liberatorIndex), () -> queueAbilityFactoryPlan(factory, liberatorIndex));
            }else{
                addEmpty(grid);
            }
            if(avertIndex != -1){
                addUnitButton(grid, "r", block.plans.get(avertIndex), () -> anyAbilityFactoryCanQueue(factory, avertIndex), () -> queueAbilityFactoryPlan(factory, avertIndex));
            }else{
                addEmpty(grid);
            }
            if(horizonIndex != -1){
                addUnitButton(grid, "e", block.plans.get(horizonIndex), () -> anyAbilityFactoryCanQueue(factory, horizonIndex), () -> queueAbilityFactoryPlan(factory, horizonIndex));
            }else{
                addEmpty(grid);
            }
            grid.row();

            //Row 2
            if(antumbraIndex != -1){
                addUnitButton(grid, "b", block.plans.get(antumbraIndex), () -> anyAbilityFactoryCanQueue(factory, antumbraIndex), () -> queueAbilityFactoryPlan(factory, antumbraIndex));
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
                addUnitButton(grid, "a", block.plans.get(daggerIndex), () -> anyAbilityFactoryCanQueue(factory, daggerIndex), () -> queueAbilityFactoryPlan(factory, daggerIndex));
            }else{
                addEmpty(grid);
            }
            if(reaperIndex != -1){
                addUnitButton(grid, "r", block.plans.get(reaperIndex), () -> anyAbilityFactoryCanQueue(factory, reaperIndex), () -> queueAbilityFactoryPlan(factory, reaperIndex));
            }else{
                addEmpty(grid);
            }
            if(fortressIndex != -1){
                addUnitButton(grid, "d", block.plans.get(fortressIndex), () -> anyAbilityFactoryCanQueue(factory, fortressIndex), () -> queueAbilityFactoryPlan(factory, fortressIndex));
            }else{
                addEmpty(grid);
            }
            if(ghostIndex != -1){
                addUnitButton(grid, "g", block.plans.get(ghostIndex), () -> anyAbilityFactoryCanQueue(factory, ghostIndex), () -> queueAbilityFactoryPlan(factory, ghostIndex));
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
                    addUnitButton(grid, row1Keys[i], block.plans.get(planIndex), () -> anyAbilityFactoryCanQueue(factory, planIndex), () -> queueAbilityFactoryPlan(factory, planIndex));
                }else{
                    addEmpty(grid);
                }
            }
            addEmpty(grid);
            grid.row();

            for(int i = 0; i < row2Keys.length; i++){
                int planIndex = i + row1Keys.length;
                if(planIndex < block.plans.size){
                    addUnitButton(grid, row2Keys[i], block.plans.get(planIndex), () -> anyAbilityFactoryCanQueue(factory, planIndex), () -> queueAbilityFactoryPlan(factory, planIndex));
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
                () -> anyAbilityFactoryCanStartAddon(factory, Blocks.memoryBank, UnitFactory.sc2AddonCrystalCost, UnitFactory.sc2AddonTechGasCost),
                () -> queueAbilityFactoryAddon(factory, UnitFactory.sc2AddonTechConfig, Blocks.memoryBank, UnitFactory.sc2AddonCrystalCost, UnitFactory.sc2AddonTechGasCost));
            addAddonBuildButton(grid, "c", "Double Addon", Blocks.rotaryPump,
                UnitFactory.sc2AddonCrystalCost, UnitFactory.sc2AddonDoubleGasCost, UnitFactory.sc2AddonDoubleTime,
                () -> anyAbilityFactoryCanStartAddon(factory, Blocks.rotaryPump, UnitFactory.sc2AddonCrystalCost, UnitFactory.sc2AddonDoubleGasCost),
                () -> queueAbilityFactoryAddon(factory, UnitFactory.sc2AddonDoubleConfig, Blocks.rotaryPump, UnitFactory.sc2AddonCrystalCost, UnitFactory.sc2AddonDoubleGasCost));
        }else{
            addEmpty(grid);
            addEmpty(grid);
        }
        addEmpty(grid);
        addIconButton(grid, "l", Icon.export, () -> factory.canLift(), () -> queueFactoryLift(factory));
        addCancelButton(grid, () -> cancelAbilityFactoryQueued(factory));
        add(grid);
    }

    private boolean anyAbilityFactoryCanQueue(UnitFactory.UnitFactoryBuild reference, int planIndex){
        if(reference == null || reference.block == null) return false;
        for(Building build : abilityBuildings()){
            if(build instanceof UnitFactory.UnitFactoryBuild f && f.isValid() && f.block == reference.block && f.sc2QueueEnabled()){
                if(f.canQueuePlan(planIndex)){
                    return true;
                }
            }
        }
        return false;
    }

    private boolean anyAbilityFactoryCanShowAddon(UnitFactory.UnitFactoryBuild reference, Block addon){
        if(reference == null || reference.block == null || addon == null) return false;
        for(Building build : abilityBuildings()){
            if(!(build instanceof UnitFactory.UnitFactoryBuild f)) continue;
            if(!f.isValid() || f.block != reference.block || !f.sc2QueueEnabled()) continue;
            if(!f.canShowAddonButtons()) continue;
            if(!factoryAddonPlaceValid(f, addon)) continue;
            return true;
        }
        return false;
    }

    private boolean anyAbilityFactoryCanStartAddon(UnitFactory.UnitFactoryBuild reference, Block addon, int crystalCost, int gasCost){
        if(reference == null || reference.block == null || addon == null) return false;
        for(Building build : abilityBuildings()){
            if(!(build instanceof UnitFactory.UnitFactoryBuild f)) continue;
            if(!f.isValid() || f.block != reference.block || !f.sc2QueueEnabled()) continue;
            if(!f.canShowAddonButtons()) continue;
            if(!f.canAffordAddon(crystalCost, gasCost)) continue;
            if(!factoryAddonPlaceValid(f, addon)) continue;
            return true;
        }
        return false;
    }

    private boolean factoryAddonPlaceValid(UnitFactory.UnitFactoryBuild factory, Block addon){
        if(factory == null || addon == null || factory.tile == null || factory.block == null) return false;
        int size = factory.block.size;
        int baseX = factory.tile.x - (size - 1) / 2;
        int baseY = factory.tile.y - (size - 1) / 2;
        Tile addonTile = world.tile(baseX + size, baseY);
        if(addonTile == null) return false;
        return Build.validPlaceIgnoreUnits(addon, factory.team, addonTile.x, addonTile.y, 0, true, true);
    }

    private void queueAbilityFactoryAddon(UnitFactory.UnitFactoryBuild reference, int config, Block addon, int crystalCost, int gasCost){
        if(reference == null || reference.block == null || addon == null) return;

        int blockId = reference.block.id;
        Seq<UnitFactory.UnitFactoryBuild> eligible = new Seq<>();
        for(Building build : abilityBuildings()){
            if(!(build instanceof UnitFactory.UnitFactoryBuild f)) continue;
            if(!f.isValid() || f.block != reference.block || !f.sc2QueueEnabled()) continue;
            if(!f.canShowAddonButtons()) continue;
            if(!f.canAffordAddon(crystalCost, gasCost)) continue;
            if(!factoryAddonPlaceValid(f, addon)) continue;
            eligible.add(f);
        }
        sortFactoriesByBuildOrder(eligible);

        int lastId = factoryDistributeLastAddonFactoryId.get(blockId, -1);
        UnitFactory.UnitFactoryBuild chosen = chooseFactoryRoundRobin(eligible, lastId);
        if(chosen == null) return;

        chosen.configure(config);
        factoryDistributeHistoryBlock.add(blockId);
        factoryDistributeHistoryFactory.add(chosen.id);
        if(factoryDistributeHistoryFactory.size > 1024){
            factoryDistributeHistoryBlock.removeIndex(0);
            factoryDistributeHistoryFactory.removeIndex(0);
        }
        factoryDistributeLastAddonFactoryId.put(blockId, chosen.id);
        factoryDistributeLastFactoryId.put(blockId, chosen.id);
        if(chosen.hasDoubleAddon()){
            factoryDistributeLastDoubleId.put(blockId, chosen.id);
        }else if(chosen.hasTechAddon()){
            factoryDistributeLastTechId.put(blockId, chosen.id);
        }else{
            factoryDistributeLastNoneId.put(blockId, chosen.id);
        }
    }

    private float factoryEffectiveQueue(UnitFactory.UnitFactoryBuild factory){
        if(factory == null) return Float.POSITIVE_INFINITY;
        int active = Math.max(1, factory.activeUnitSlots());
        return (float)factory.queued / (float)active;
    }

    private @Nullable UnitFactory.UnitFactoryBuild findFactoryById(Seq<UnitFactory.UnitFactoryBuild> list, int id){
        if(id < 0) return null;
        for(UnitFactory.UnitFactoryBuild f : list){
            if(f != null && f.id == id){
                return f;
            }
        }
        return null;
    }

    private IntIntMap categoryLastIdMap(int category){
        if(category == factoryCatDouble) return factoryDistributeLastDoubleId;
        if(category == factoryCatTech) return factoryDistributeLastTechId;
        return factoryDistributeLastNoneId;
    }

    private @Nullable UnitFactory.UnitFactoryBuild chooseFactoryRoundRobin(Seq<UnitFactory.UnitFactoryBuild> list, int lastId){
        if(list.isEmpty()) return null;
        for(UnitFactory.UnitFactoryBuild f : list){
            if(f != null && f.id > lastId) return f;
        }
        return list.first();
    }

    private @Nullable UnitFactory.UnitFactoryBuild chooseFactoryAtLoad(Seq<UnitFactory.UnitFactoryBuild> list, int lastId, float load){
        if(list.isEmpty() || Float.isInfinite(load)) return null;

        for(UnitFactory.UnitFactoryBuild f : list){
            if(f != null && f.id > lastId && Mathf.equal(factoryEffectiveQueue(f), load, 0.0001f)) return f;
        }
        for(UnitFactory.UnitFactoryBuild f : list){
            if(f != null && Mathf.equal(factoryEffectiveQueue(f), load, 0.0001f)) return f;
        }
        return null;
    }

    private void sortFactoriesByBuildOrder(Seq<UnitFactory.UnitFactoryBuild> list){
        if(list.size <= 1) return;
        list.sort((a, b) -> Integer.compare(a.id, b.id));
    }

    private @Nullable CoreBuild chooseCoreRoundRobin(Seq<CoreBuild> list, int lastId){
        if(list.isEmpty()) return null;
        for(CoreBuild core : list){
            if(core != null && core.id > lastId) return core;
        }
        return list.first();
    }

    private void sortCoresByBuildOrder(Seq<CoreBuild> list){
        if(list.size <= 1) return;
        list.sort((a, b) -> Integer.compare(a.id, b.id));
    }

    private boolean cancelAbilityFactoryQueued(UnitFactory.UnitFactoryBuild reference){
        if(reference == null || reference.block == null) return false;
        int blockId = reference.block.id;

        for(int i = factoryDistributeHistoryFactory.size - 1; i >= 0; i--){
            if(factoryDistributeHistoryBlock.get(i) != blockId) continue;

            int factoryId = factoryDistributeHistoryFactory.get(i);
            factoryDistributeHistoryBlock.removeIndex(i);
            factoryDistributeHistoryFactory.removeIndex(i);

            UnitFactory.UnitFactoryBuild found = null;
            for(Building build : abilityBuildings()){
                if(!(build instanceof UnitFactory.UnitFactoryBuild f)) continue;
                if(!f.isValid() || f.block != reference.block || !f.sc2QueueEnabled()) continue;
                if(f.id == factoryId){
                    found = f;
                    break;
                }
            }

            if(found != null && (found.isAddonBuilding() || found.queued > 0)){
                found.configure(UnitFactory.sc2AddonCancelConfig);
                factoryDistributeLastFactoryId.put(blockId, found.id);
                if(found.hasDoubleAddon()){
                    factoryDistributeLastDoubleId.put(blockId, found.id);
                }else if(found.hasTechAddon()){
                    factoryDistributeLastTechId.put(blockId, found.id);
                }else{
                    factoryDistributeLastNoneId.put(blockId, found.id);
                }
                return true;
            }
        }

        UnitFactory.UnitFactoryBuild best = null;
        float bestLoad = -1f;
        for(Building build : abilityBuildings()){
            if(!(build instanceof UnitFactory.UnitFactoryBuild f)) continue;
            if(!f.isValid() || f.block != reference.block || !f.sc2QueueEnabled()) continue;
            if(!f.isAddonBuilding() && f.queued <= 0) continue;
            float load = f.isAddonBuilding() ? 9999f : factoryEffectiveQueue(f);
            if(load > bestLoad){
                best = f;
                bestLoad = load;
            }
        }

        if(best != null){
            best.configure(UnitFactory.sc2AddonCancelConfig);
            factoryDistributeLastFactoryId.put(blockId, best.id);
            if(best.hasDoubleAddon()){
                factoryDistributeLastDoubleId.put(blockId, best.id);
            }else if(best.hasTechAddon()){
                factoryDistributeLastTechId.put(blockId, best.id);
            }else{
                factoryDistributeLastNoneId.put(blockId, best.id);
            }
            return true;
        }

        //Fallback: cancel on reference (may cancel addon build)
        reference.configure(UnitFactory.sc2AddonCancelConfig);
        return true;
    }

    private void queueAbilityFactoryPlan(UnitFactory.UnitFactoryBuild reference, int planIndex){
        if(reference == null || reference.block == null) return;

        int blockId = reference.block.id;
        Seq<UnitFactory.UnitFactoryBuild> doubles = new Seq<>();
        Seq<UnitFactory.UnitFactoryBuild> none = new Seq<>();
        Seq<UnitFactory.UnitFactoryBuild> tech = new Seq<>();
        Seq<UnitFactory.UnitFactoryBuild> eligible = new Seq<>();

        for(Building build : abilityBuildings()){
            if(!(build instanceof UnitFactory.UnitFactoryBuild f)) continue;
            if(!f.isValid() || f.block != reference.block || !f.sc2QueueEnabled()) continue;
            if(!f.canQueuePlan(planIndex)) continue;

            eligible.add(f);
            if(f.hasDoubleAddon()){
                doubles.add(f);
            }else if(f.hasTechAddon()){
                tech.add(f);
            }else{
                none.add(f);
            }
        }

        if(eligible.isEmpty()) return;

        //Keep consistent build order within each group: earlier built factories get chosen first when loads tie.
        sortFactoriesByBuildOrder(doubles);
        sortFactoriesByBuildOrder(none);
        sortFactoriesByBuildOrder(tech);

        float minDouble = Float.POSITIVE_INFINITY;
        float minNone = Float.POSITIVE_INFINITY;
        float minTech = Float.POSITIVE_INFINITY;
        for(UnitFactory.UnitFactoryBuild f : doubles){
            minDouble = Math.min(minDouble, factoryEffectiveQueue(f));
        }
        for(UnitFactory.UnitFactoryBuild f : none){
            minNone = Math.min(minNone, factoryEffectiveQueue(f));
        }
        for(UnitFactory.UnitFactoryBuild f : tech){
            minTech = Math.min(minTech, factoryEffectiveQueue(f));
        }

        //SC2-like group selection:
        //Prefer higher-priority factories, but once they're >=1 "effective queue" ahead, start filling the next group.
        int chosenCategory = !doubles.isEmpty() ? factoryCatDouble : (!none.isEmpty() ? factoryCatNone : factoryCatTech);
        float chosenMin = chosenCategory == factoryCatDouble ? minDouble : (chosenCategory == factoryCatNone ? minNone : minTech);
        float groupThreshold = 1f;
        float eps = 0.0001f;

        for(;;){
            int nextCategory = -1;
            float nextMin = Float.POSITIVE_INFINITY;

            if(chosenCategory == factoryCatDouble){
                if(!none.isEmpty()){
                    nextCategory = factoryCatNone;
                    nextMin = minNone;
                }else if(!tech.isEmpty()){
                    nextCategory = factoryCatTech;
                    nextMin = minTech;
                }
            }else if(chosenCategory == factoryCatNone){
                if(!tech.isEmpty()){
                    nextCategory = factoryCatTech;
                    nextMin = minTech;
                }
            }

            if(nextCategory == -1 || Float.isInfinite(nextMin)) break;
            if(chosenMin + eps >= nextMin + groupThreshold){
                chosenCategory = nextCategory;
                chosenMin = nextMin;
                continue;
            }
            break;
        }

        //Global sticky: fill the last-used factory's free active slot before distributing elsewhere.
        int lastOverallId = factoryDistributeLastFactoryId.get(blockId, -1);
        UnitFactory.UnitFactoryBuild stickyOverall = findFactoryById(eligible, lastOverallId);
        if(stickyOverall != null && stickyOverall.canQueuePlan(planIndex)){
            boolean stickyDouble = stickyOverall.hasDoubleAddon();
            boolean stickyTech = stickyOverall.hasTechAddon();
            int stickyCat = stickyDouble ? factoryCatDouble : (stickyTech ? factoryCatTech : factoryCatNone);
            int stickyActive = Math.max(1, stickyOverall.activeUnitSlots());
            boolean stickyForcePair = stickyActive > 1 && (stickyOverall.queued < stickyActive || (stickyOverall.queued % stickyActive != 0));
            boolean stickyFreeActive = stickyOverall.queued < stickyActive;

            //For multi-slot factories (reactor/double addon), always enqueue in pairs to keep both active slots busy.
            //Otherwise, only fill the free active slot when it matches the chosen group.
            if(stickyForcePair || (stickyFreeActive && stickyCat == chosenCategory)){
                stickyOverall.configure(planIndex);
                factoryDistributeHistoryBlock.add(blockId);
                factoryDistributeHistoryFactory.add(stickyOverall.id);
                if(factoryDistributeHistoryFactory.size > 1024){
                    factoryDistributeHistoryBlock.removeIndex(0);
                    factoryDistributeHistoryFactory.removeIndex(0);
                }
                factoryDistributeLastFactoryId.put(blockId, stickyOverall.id);
                if(stickyOverall.hasDoubleAddon()){
                    factoryDistributeLastDoubleId.put(blockId, stickyOverall.id);
                }else if(stickyOverall.hasTechAddon()){
                    factoryDistributeLastTechId.put(blockId, stickyOverall.id);
                }else{
                    factoryDistributeLastNoneId.put(blockId, stickyOverall.id);
                }
                return;
            }
        }
        Seq<UnitFactory.UnitFactoryBuild> chosenList = chosenCategory == factoryCatDouble ? doubles : (chosenCategory == factoryCatTech ? tech : none);
        IntIntMap lastIdMap = categoryLastIdMap(chosenCategory);
        int lastId = lastIdMap.get(blockId, -1);

        UnitFactory.UnitFactoryBuild chosen = chooseFactoryAtLoad(chosenList, lastId, chosenMin);
        if(chosen == null){
            chosen = chooseFactoryRoundRobin(chosenList, lastId);
        }
        if(chosen == null) return;

        chosen.configure(planIndex);
        factoryDistributeHistoryBlock.add(blockId);
        factoryDistributeHistoryFactory.add(chosen.id);
        if(factoryDistributeHistoryFactory.size > 1024){
            factoryDistributeHistoryBlock.removeIndex(0);
            factoryDistributeHistoryFactory.removeIndex(0);
        }
        factoryDistributeLastFactoryId.put(blockId, chosen.id);
        lastIdMap.put(blockId, chosen.id);
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
        addIconButton(grid, "s", new TextureRegionDrawable(UnitTypes.nova.uiIcon), this::anySelectedCoreCanQueueScv, () -> {
            queueSelectedCoreUnit();
            corePanel = CorePanel.BUILD;
        });
        addEmpty(grid);
        addEmpty(grid);
        Button orbitalButton = addHoverableIconButton(grid, "b", new TextureRegionDrawable(Blocks.coreOrbital.uiIcon), this::anySelectedCoreCanStartOrbitalUpgrade, () -> {
            queueSelectedCoreUpgrade(coreUpgradeOrbital);
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
        Button fortressButton = addIconButton(grid, "p", fortressIcon, this::anySelectedCoreCanStartFortressUpgrade, () -> {
            queueSelectedCoreUpgrade(coreUpgradeFortress);
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

        String text = buildHoverText(info);

        Font font = Fonts.outline;
        boolean prevInts = font.usesIntegerPositions();
        font.setUseIntegerPositions(false);

        float pad = 6f;
        float maxTextWidth = Math.max(10f, width - pad * 2f);
        hoverInfoLayout.setText(font, text, Color.white, maxTextWidth, Align.left, true);

        float boxW = width;
        float boxH = hoverInfoLayout.height + pad * 2f;
        float px = Mathf.clamp(x, 4f, Core.scene.getWidth() - boxW - 4f);
        float py = y + height + HOVER_INFO_GAP;

        if(py + boxH > Core.scene.getHeight() - 4f){
            py = Math.max(4f, y - boxH - HOVER_INFO_GAP);
        }

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

    private String buildHoverText(AbilityInfo info){
        if(info.hintOnly){
            return targetHintText();
        }

        StringBuilder text = new StringBuilder();
        appendHoverLine(text, hoverTitle(info));
        appendHoverLine(text, hoverCostLine(info));
        appendHoverLine(text, hoverDescription(info));
        appendHoverLine(text, hoverDetailLine(info));
        return text.toString();
    }

    private void appendHoverLine(StringBuilder out, @Nullable String line){
        if(line == null || line.isEmpty()) return;
        if(out.length() > 0) out.append('\n');
        out.append(line);
    }

    private String targetHintText(){
        return tr("左键选择目标\n右键返回", "Left-click select target\nRight-click return");
    }

    private String hoverTitle(AbilityInfo info){
        String name = info.name == null || info.name.isEmpty() ? tr("技能", "Ability") : localizeDisplayName(info.name);
        if(info.key == null || info.key.isEmpty()) return name;
        return name + " (" + formatHotkey(info.key) + ")";
    }

    private String hoverCostLine(AbilityInfo info){
        if(info.costLineOverride != null && !info.costLineOverride.isEmpty()){
            return info.costLineOverride;
        }

        StringBuilder line = new StringBuilder();

        if(info.crystalCost > 0){
            appendInfoPart(line, Items.graphite.emoji() + " " + info.crystalCost);
        }
        if(info.gasCost > 0){
            appendInfoPart(line, Items.highEnergyGas.emoji() + " " + info.gasCost);
        }
        if(info.population > 0){
            appendInfoPart(line, tr("人口", "Pop") + " " + info.population);
        }
        if(showsProductionTime(info.action) && info.timeSeconds > 0){
            appendInfoPart(line, productionTimeLabel(info.action) + " " + formatSeconds(info.timeSeconds));
        }

        if(line.length() == 0){
            return tr("无晶体/瓦斯消耗", "No crystal/gas cost");
        }

        return line.toString();
    }

    private void appendInfoPart(StringBuilder out, String part){
        if(part == null || part.isEmpty()) return;
        if(out.length() > 0) out.append("   ");
        out.append(part);
    }

    private String hoverDescription(AbilityInfo info){
        String rawName = info.name == null ? "" : info.name;

        if("Train".equals(info.action) && info.unit != null){
            return unitDescription(info.unit);
        }
        if("Build".equals(info.action) && info.block != null){
            return blockDescription(info.block);
        }
        if("Research".equals(info.action)){
            String research = researchDescription(rawName);
            if(research != null) return research;
        }

        String named = namedDescription(rawName);
        if(named != null) return named;

        if(info.description != null && !info.description.isEmpty()){
            return localizeFallbackDescription(info.description);
        }
        if(info.unit != null){
            return unitDescription(info.unit);
        }
        if(info.block != null){
            return blockDescription(info.block);
        }

        return tr("左键点击等同于快捷键。", "Left-click acts as the hotkey.");
    }

    private @Nullable String hoverDetailLine(AbilityInfo info){
        if(showsProductionTime(info.action)){
            return unitTargetText(info);
        }
        if(info.timeSeconds > 0f){
            return abilityTimeLabel(info) + " " + formatSeconds(info.timeSeconds);
        }
        return null;
    }

    private @Nullable String unitTargetText(AbilityInfo info){
        if(info == null || info.unit == null) return null;
        if(info.action == null || !info.action.equals("Train")) return null;
        boolean air = unitDefaultTargetsAir(info.unit);
        boolean ground = unitDefaultTargetsGround(info.unit);
        if(air && ground) return tr("攻击目标: 对空对地", "Targets: air / ground");
        if(air) return tr("攻击目标: 对空", "Targets: air");
        if(ground) return tr("攻击目标: 对地", "Targets: ground");
        return tr("攻击目标: 无", "Targets: none");
    }

    private boolean unitDefaultTargetsAir(UnitType unit){
        if(unit == null) return false;
        if(unit == UnitTypes.flare) return true;
        if(unit == UnitTypes.liberator) return true;
        return unit.targetAir;
    }

    private boolean unitDefaultTargetsGround(UnitType unit){
        if(unit == null) return false;
        if(unit == UnitTypes.flare) return false;
        if(unit == UnitTypes.liberator) return false;
        return unit.targetGround;
    }

    private boolean showsProductionTime(@Nullable String action){
        return "Train".equals(action) || "Build".equals(action) || "Research".equals(action) || "Deploy".equals(action);
    }

    private String productionTimeLabel(@Nullable String action){
        return switch(action == null ? "" : action){
            case "Train" -> tr("训练", "Train");
            case "Build" -> tr("建造", "Build");
            case "Research" -> tr("研究", "Research");
            case "Deploy" -> tr("部署", "Deploy");
            default -> tr("时间", "Time");
        };
    }

    private String abilityTimeLabel(AbilityInfo info){
        String rawName = info.name == null ? "" : info.name;
        if(rawName.contains("Mode") || rawName.contains("Siege") || rawName.contains("Tank")){
            return tr("变形时间", "Morph Time");
        }
        if(rawName.contains("Burrow") || rawName.contains("Reload") || rawName.contains("Defense")){
            return tr("准备时间", "Setup Time");
        }
        return tr("技能时间", "Ability Time");
    }

    private String formatSeconds(float seconds){
        float value = Math.max(seconds, 0f);
        if(Math.abs(value - Math.round(value)) < 0.01f){
            return Math.round(value) + "s";
        }
        return Strings.fixed(value, 1) + "s";
    }

    private String formatHotkey(String key){
        if(key == null || key.isEmpty()) return "";
        if(key.length() == 1) return key.toUpperCase(Locale.ROOT);
        return key;
    }

    private boolean isZh(){
        Locale locale = Core.bundle == null ? Locale.getDefault() : Core.bundle.getLocale();
        return locale != null && locale.getLanguage() != null && locale.getLanguage().startsWith("zh");
    }

    private String tr(String zh, String en){
        return isZh() ? zh : en;
    }

    private String localizeFallbackDescription(String english){
        if(english == null || english.isEmpty()) return "";
        if(english.equals("Left-click acts as hotkey.")){
            return tr("左键点击等同于快捷键。", "Left-click acts as the hotkey.");
        }
        return english;
    }

    private String localizeDisplayName(String rawName){
        if(rawName == null || rawName.isEmpty()) return "";

        String levelName;
        if((levelName = localizeLevelName(rawName, "Infantry Weapons", "步兵武器")) != null) return levelName;
        if((levelName = localizeLevelName(rawName, "Infantry Armor", "步兵装甲")) != null) return levelName;
        if((levelName = localizeLevelName(rawName, "Vehicle Weapons", "载具武器")) != null) return levelName;
        if((levelName = localizeLevelName(rawName, "Vehicle/Ship Plating", "载具/舰船装甲")) != null) return levelName;
        if((levelName = localizeLevelName(rawName, "Ship Weapons", "舰船武器")) != null) return levelName;

        return switch(rawName){
            case "SCV" -> "SCV";
            case "Marine" -> tr("枪兵", "Marine");
            case "Reaper" -> tr("收割者", "Reaper");
            case "Marauder" -> tr("劫掠者", "Marauder");
            case "Ghost" -> tr("鬼兵", "Ghost");
            case "Hellion" -> tr("恶火", "Hellion");
            case "Hellbat" -> tr("恶蝠", "Hellbat");
            case "Widow Mine" -> tr("寡妇雷", "Widow Mine");
            case "Cyclone" -> tr("飓风", "Cyclone");
            case "Siege Tank" -> tr("攻城坦克", "Siege Tank");
            case "Thor" -> tr("雷神", "Thor");
            case "Viking" -> tr("维京战机", "Viking");
            case "Medivac" -> tr("医疗运输机", "Medivac");
            case "Liberator" -> tr("解放者", "Liberator");
            case "Raven" -> tr("铁鸦", "Raven");
            case "Banshee" -> tr("女妖", "Banshee");
            case "Battlecruiser" -> tr("战列巡航舰", "Battlecruiser");
            case "Command Center" -> tr("指挥中心", "Command Center");
            case "Orbital Command" -> tr("轨道指挥部", "Orbital Command");
            case "Planetary Fortress" -> tr("行星要塞", "Planetary Fortress");
            case "Refinery" -> tr("精炼厂", "Refinery");
            case "Supply Depot" -> tr("补给站", "Supply Depot");
            case "Barracks" -> tr("兵营", "Barracks");
            case "Engineering Bay" -> tr("工程站", "Engineering Bay");
            case "Bunker" -> tr("地堡", "Bunker");
            case "Missile Turret" -> tr("导弹塔", "Missile Turret");
            case "Sensor Tower" -> tr("雷达塔", "Sensor Tower");
            case "Ghost Academy" -> tr("幽灵学院", "Ghost Academy");
            case "Factory" -> tr("重工厂", "Factory");
            case "Armory" -> tr("军械库", "Armory");
            case "Starport" -> tr("星港", "Starport");
            case "Fusion Core" -> tr("聚变核心", "Fusion Core");
            case "Tech Lab" -> tr("科技实验室", "Tech Lab");
            case "Reactor", "Double Addon" -> tr("反应堆", "Reactor");
            case "Tech Addon" -> tr("科技实验室", "Tech Lab");
            case "KD8 Bomb" -> tr("KD8炸弹", "KD8 Bomb");
            case "Stimpack" -> tr("兴奋剂", "Stimpack");
            case "Hurricane Lock" -> tr("锁定", "Hurricane Lock");
            case "Heal" -> tr("治疗", "Heal");
            case "Afterburners" -> tr("点燃加力燃烧器", "Afterburners");
            case "Load" -> tr("装载", "Load");
            case "Unload" -> tr("卸载", "Unload");
            case "Tactical Nuke" -> tr("战术核打击", "Tactical Nuke");
            case "Stable Aim" -> tr("稳固瞄准", "Stable Aim");
            case "EMP Round" -> tr("EMP弹", "EMP Round");
            case "Cloak" -> tr("隐形", "Cloak");
            case "Fighter Mode" -> tr("战机模式", "Fighter Mode");
            case "Mech Mode" -> tr("机甲模式", "Mech Mode");
            case "Locus Mode" -> tr("恶蝠模式", "Hellbat Mode");
            case "Mace Mode" -> tr("恶火模式", "Hellion Mode");
            case "High Impact Payload" -> tr("高冲击弹头", "High Impact Payload");
            case "Explosive Payload" -> tr("爆裂弹头", "Explosive Payload");
            case "Auto Turret" -> tr("自动机炮台", "Auto Turret");
            case "Anti-Armor Missile" -> tr("反装甲导弹", "Anti-Armor Missile");
            case "Interference Matrix" -> tr("干扰矩阵", "Interference Matrix");
            case "Yamato Cannon" -> tr("大和炮", "Yamato Cannon");
            case "Tactical Jump" -> tr("战术跃迁", "Tactical Jump");
            case "Defense Mode" -> tr("防卫模式", "Defense Mode");
            case "Siege Mode" -> tr("攻城模式", "Siege Mode");
            case "Tank Mode" -> tr("坦克模式", "Tank Mode");
            case "Widow Burrow" -> tr("寡妇雷下潜", "Widow Burrow");
            case "Widow Reload" -> tr("寡妇雷装填", "Widow Reload");
            case "Instant Tracking" -> tr("即时追踪", "Instant Tracking");
            case "Steel Armor" -> tr("钢铁装甲", "Steel Armor");
            case "Ghost Camouflage" -> tr("幽灵迷彩", "Ghost Camouflage");
            case "Blast Shield" -> tr("防爆护盾", "Blast Shield");
            case "Concussive Shells" -> tr("震荡弹", "Concussive Shells");
            case "Inferno Pre-Igniter" -> tr("地狱预燃器", "Inferno Pre-Igniter");
            case "Electromagnetic Field Accelerator" -> tr("电磁场加速器", "Electromagnetic Field Accelerator");
            case "Drilling Claws" -> tr("钻地爪", "Drilling Claws");
            case "Smart Servos" -> tr("智能伺服", "Smart Servos");
            case "Cloaking Field" -> tr("隐形力场", "Cloaking Field");
            case "Afterburner Rotors" -> tr("加力旋翼", "Afterburner Rotors");
            case "Weapon Refit" -> tr("武器重构", "Weapon Refit");
            case "Caduceus Reactor" -> tr("卡杜修斯反应堆", "Caduceus Reactor");
            case "Advanced Ballistics" -> tr("先进弹道学", "Advanced Ballistics");
            case "Armory Upgrade" -> tr("军械库升级", "Armory Upgrade");
            case "Engineering Upgrade" -> tr("工程升级", "Engineering Upgrade");
            case "Barracks Tech" -> tr("兵营科技", "Barracks Tech");
            case "Heavy Factory Tech" -> tr("重工厂科技", "Heavy Factory Tech");
            case "Fusion Core Upgrade" -> tr("聚变核心升级", "Fusion Core Upgrade");
            case "Skill Cooldown" -> tr("技能冷却", "Skill Cooldown");
            default -> rawName;
        };
    }

    private @Nullable String localizeLevelName(String rawName, String enBase, String zhBase){
        if(rawName.equals(enBase)) return tr(zhBase, enBase);
        String prefix = enBase + " Lv.";
        if(!rawName.startsWith(prefix)) return null;
        String level = rawName.substring(prefix.length());
        return isZh() ? zhBase + level + "级" : enBase + " Lv." + level;
    }

    private String unitDescription(UnitType unit){
        if(unit == UnitTypes.nova) return tr("工兵单位。采集晶体与瓦斯、建造建筑，并为机械单位维修。", "Worker unit. Gathers crystal and gas, constructs structures, and repairs mechanical units.");
        if(unit == UnitTypes.dagger) return tr("基础步兵。可对空对地攻击；研究兴奋剂后可短时提升机动与输出。", "Basic infantry. Attacks air and ground; Stimpack temporarily boosts mobility and firepower.");
        if(unit == UnitTypes.reaper) return tr("高机动骚扰步兵。使用KD8炸弹驱散轻型地面单位。", "Fast harassment infantry. Uses KD8 charges to displace light ground targets.");
        if(unit == UnitTypes.fortress) return tr("重装步兵。对重甲目标有额外伤害，研究震荡弹后可减速敌人。", "Heavy infantry. Deals bonus damage to armored targets and can slow enemies with Concussive Shells.");
        if(unit == UnitTypes.ghost) return tr("精英特战步兵。可狙击、发射EMP并隐形，还能引导战术核打击。", "Elite special-ops infantry. Uses snipe, EMP, and cloak, and can call down tactical nukes.");
        if(unit == UnitTypes.mace) return tr("高速喷火载具。擅长清理轻甲地面单位，可变形为恶蝠。", "Fast flame vehicle specialized against light ground units. Can transform into Hellbat mode.");
        if(unit == UnitTypes.locus) return tr("近战重甲喷火单位。对轻甲地面有压制力，可变形回恶火。", "Armored close-range flame unit that excels versus light ground. Can transform back to Hellion mode.");
        if(unit == UnitTypes.crawler) return tr("伏击型地雷。埋地后锁定并发射高伤害范围导弹。", "Ambush mine. Burrows, locks on, and fires a high-damage splash missile.");
        if(unit == UnitTypes.hurricane) return tr("机动导弹载具。可锁定单个目标并在移动中持续发射追踪导弹。", "Mobile missile vehicle. Locks onto a target and keeps firing tracking missiles while moving.");
        if(unit == UnitTypes.precept) return tr("可切换模式的火炮载具。坦克模式机动，攻城模式提供远程范围火力。", "Artillery vehicle with two modes. Mobile in tank mode, long-range splash fire in siege mode.");
        if(unit == UnitTypes.scepter) return tr("重型机械单位。具备强力对地火力，并可切换两种对空弹药模式。", "Heavy walker with powerful anti-ground weapons and switchable anti-air payload modes.");
        if(unit == UnitTypes.flare) return tr("可变形空优战机。战机模式主打对空，机甲模式可对地支援。", "Transforming air-superiority fighter. Fighter mode focuses on air targets, mech mode supports against ground.");
        if(unit == UnitTypes.mega) return tr("空中医疗运输单位。可治疗生物单位并装载友军地面单位。", "Airborne medical transport. Heals biological allies and can load friendly ground units.");
        if(unit == UnitTypes.liberator) return tr("双模式炮艇。战机模式主要对空，防卫模式只能打击圈内地面目标。", "Dual-mode gunship. Fighter mode handles air, while defense mode only attacks ground targets inside its zone.");
        if(unit == UnitTypes.avert) return tr("电子战支援机。可部署自动机炮台、发射反装甲导弹并施放干扰矩阵。", "Electronic warfare support craft. Deploys auto turrets, fires anti-armor missiles, and uses Interference Matrix.");
        if(unit == UnitTypes.horizon) return tr("隐形对地攻击机。擅长打击地面目标，研究后可持续隐形。", "Cloaked ground-attack aircraft specialized against ground targets.");
        if(unit == UnitTypes.antumbra) return tr("重型主力舰。拥有全面火力，并可施放大和炮与战术跃迁。", "Capital ship with broad firepower, plus Yamato Cannon and Tactical Jump.");
        return unit.localizedName;
    }

    private String blockDescription(Block block){
        if(block == Blocks.coreNucleus) return tr("主基地。训练SCV、回收资源，并可升级为轨道指挥部或行星要塞。", "Main base. Trains SCVs, stores resources, and can upgrade into Orbital Command or Planetary Fortress.");
        if(block == Blocks.coreOrbital) return tr("高级主基地。可执行扫描并提供额外补给能力。", "Advanced base that can scan and provide additional supply support.");
        if(block == Blocks.corePlanetaryFortress) return tr("武装要塞化主基地。具备地面火炮和更高防御。", "Fortified base with a ground cannon and much heavier defenses.");
        if(block == Blocks.ventCondenser) return tr("建在气泉上的瓦斯采集建筑。", "Gas harvesting structure built on a geyser.");
        if(block == Blocks.doorLarge) return tr("提供人口上限的补给建筑。", "Supply structure that increases your population cap.");
        if(block == Blocks.groundFactory) return tr("训练步兵单位，可加装科技实验室或反应堆。", "Produces infantry units and can add a Tech Lab or Reactor.");
        if(block == Blocks.multiPress) return tr("提供步兵升级和部分建筑科技。", "Unlocks infantry upgrades and key structure technologies.");
        if(block == Blocks.atmosphericConcentrator) return tr("可装载步兵的防御工事，为内部单位提供掩护输出。", "Defensive bunker that loads infantry and lets them fire safely from inside.");
        if(block == Blocks.swarmer) return tr("静态对空防御塔，可压制敌方空军。", "Static anti-air turret for controlling hostile aircraft.");
        if(block == Blocks.radar) return tr("侦测附近来袭敌军，提前提供预警。", "Detects nearby incoming enemy units and provides early warning.");
        if(block == Blocks.launchPad) return tr("研究幽灵迷彩并制造战术核弹头。", "Researches Ghost camouflage and produces tactical warheads.");
        if(block == Blocks.tankFabricator) return tr("训练载具单位，可加装科技实验室或反应堆。", "Produces vehicle units and can add a Tech Lab or Reactor.");
        if(block == Blocks.siliconCrucible) return tr("提供载具与舰船升级，并解锁部分形态切换强化。", "Provides vehicle and ship upgrades and unlocks several transformation improvements.");
        if(block == Blocks.shipFabricator) return tr("训练空军单位，可加装科技实验室或反应堆。", "Produces air units and can add a Tech Lab or Reactor.");
        if(block == Blocks.surgeCrucible) return tr("解锁后期星港科技，如解放者、医疗运输机和战巡强化。", "Unlocks late-game starport upgrades for Liberators, Medivacs, and Battlecruisers.");
        if(block == Blocks.memoryBank) return tr("科技附件。解锁高级单位与研究项目。", "Tech add-on that unlocks advanced units and research.");
        if(block == Blocks.rotaryPump) return tr("反应堆附件。给予生产建筑双队列产能。", "Reactor add-on that grants a second production queue.");
        return block.localizedName;
    }

    private @Nullable String researchDescription(String rawName){
        if(rawName == null || rawName.isEmpty()) return null;
        if(rawName.startsWith("Infantry Weapons")) return tr("提升枪兵、劫掠者和鬼兵的武器伤害；鬼兵对轻甲额外伤害也会提高。", "Increases Marine, Marauder, and Ghost weapon damage, including Ghost bonus damage versus light targets.");
        if(rawName.startsWith("Infantry Armor")) return tr("提升枪兵、劫掠者和鬼兵的护甲。", "Increases armor for Marines, Marauders, and Ghosts.");
        if(rawName.startsWith("Vehicle Weapons")) return tr("提升恶火、恶蝠、寡妇雷、飓风和攻城坦克的武器伤害。", "Increases weapon damage for Hellions, Hellbats, Widow Mines, Cyclones, and Siege Tanks.");
        if(rawName.startsWith("Vehicle/Ship Plating")) return tr("提升载具与舰船单位的护甲。", "Increases armor for vehicle and ship units.");
        if(rawName.startsWith("Ship Weapons")) return tr("提升维京、解放者、铁鸦、女妖和战列巡航舰的武器伤害。", "Increases weapon damage for Vikings, Liberators, Ravens, Banshees, and Battlecruisers.");

        return switch(rawName){
            case "Instant Tracking" -> tr("使飓风的锁定额外获得1格射程。", "Grants Cyclone lock-on +1 tile of range.");
            case "Steel Armor" -> tr("使建筑额外获得2点护甲，地堡额外获得2个装载位，并提高基地SCV容量。", "Adds 2 armor to structures, gives bunkers 2 extra slots, and increases base SCV capacity.");
            case "Ghost Camouflage" -> tr("解锁鬼兵的隐形能力。", "Unlocks Ghost cloak.");
            case "Blast Shield" -> tr("使枪兵额外获得10点生命值。", "Gives Marines +10 maximum health.");
            case "Stimpack" -> tr("解锁枪兵与劫掠者的兴奋剂，用生命换取短时间更强战斗力。", "Unlocks Stimpack for Marines and Marauders, trading health for a short combat boost.");
            case "Concussive Shells" -> tr("使劫掠者攻击短暂减速目标。", "Causes Marauder attacks to briefly slow targets.");
            case "Inferno Pre-Igniter" -> tr("提升恶火与恶蝠对轻甲单位的额外伤害。", "Increases Hellion and Hellbat bonus damage against light units.");
            case "Electromagnetic Field Accelerator" -> tr("强化飓风的锁定导弹伤害。", "Upgrades Cyclone lock-on missile damage.");
            case "Drilling Claws" -> tr("显著缩短寡妇雷埋地与起爆准备时间。", "Greatly reduces Widow Mine burrow and setup time.");
            case "Smart Servos" -> tr("缩短恶火/恶蝠、维京、攻城坦克和解放者的变形部署时间。", "Reduces transformation and deployment time for Hellions/Hellbats, Vikings, Siege Tanks, and Liberators.");
            case "Cloaking Field" -> tr("解锁女妖的隐形能力。", "Unlocks Banshee cloak.");
            case "Afterburner Rotors" -> tr("永久提升女妖的移动速度。", "Permanently increases Banshee movement speed.");
            case "Interference Matrix" -> tr("解锁铁鸦的干扰矩阵技能。", "Unlocks Raven Interference Matrix.");
            case "Weapon Refit" -> tr("解锁战列巡航舰的大和炮。", "Unlocks Battlecruiser Yamato Cannon.");
            case "Caduceus Reactor" -> tr("提升医疗运输机的能量恢复效率。", "Improves Medivac energy regeneration.");
            case "Advanced Ballistics" -> tr("提升解放者防卫模式的攻击范围。", "Increases Liberator defense mode range.");
            case "Armory Upgrade" -> tr("显示军械库当前正在进行的升级进度。", "Displays the Armory upgrade currently in progress.");
            case "Engineering Upgrade" -> tr("显示工程站当前正在进行的升级进度。", "Displays the Engineering Bay upgrade currently in progress.");
            case "Barracks Tech" -> tr("显示兵营科技实验室当前研究的项目。", "Displays the current Barracks Tech Lab research.");
            case "Heavy Factory Tech" -> tr("显示重工厂科技实验室当前研究的项目。", "Displays the current Factory Tech Lab research.");
            case "Fusion Core Upgrade" -> tr("显示聚变核心当前进行中的升级项目。", "Displays the Fusion Core upgrade currently in progress.");
            default -> null;
        };
    }

    private @Nullable String namedDescription(String rawName){
        if(rawName == null || rawName.isEmpty()) return null;

        return switch(rawName){
            case "Stimpack" -> tr("消耗生命值，短时间提高枪兵与劫掠者的移动和攻击效率。", "Consumes health to briefly boost Marine and Marauder mobility and attack output.");
            case "KD8 Bomb" -> tr("投掷延时炸弹。1.5秒后爆炸，造成5点穿透伤害并击退轻型目标。", "Throws a timed explosive. Detonates after 1.5 seconds for 5 pierce damage and knockback against light targets.");
            case "Hurricane Lock" -> tr("锁定当前目标并在移动中持续发射追踪导弹。右键可切换自动施放。", "Locks the current target and keeps firing tracking missiles while moving. Right-click toggles autocast.");
            case "Heal" -> tr("为生物友军持续恢复生命。右键可切换自动施放。", "Continuously restores health to biological allied units. Right-click toggles autocast.");
            case "Afterburners" -> tr("短时间获得爆发移速，用于追击、撤离或转场。", "Grants a short burst of movement speed for pursuit, retreat, or repositioning.");
            case "Load" -> tr("装载附近友军地面单位进入运输舱。", "Loads nearby friendly ground units into the transport bay.");
            case "Unload" -> tr("卸载当前运输舱内的单位。", "Unloads units currently carried in the transport bay.");
            case "Tactical Nuke" -> tr("在目标区域引导一枚核弹。需要先制造弹头。", "Calls down a nuclear strike on the target area. Requires a prepared warhead.");
            case "Stable Aim" -> tr("瞄准后进行高伤害狙击，对灵能目标有额外效果。", "Aims, then fires a high-damage snipe with extra effect against psionic targets.");
            case "EMP Round" -> tr("发射EMP弹，削减护盾、烧毁灵能能量并揭示隐形。", "Fires an EMP round that removes shields, burns psionic energy, and reveals cloaked targets.");
            case "Cloak" -> tr("进入隐形状态并持续消耗能量。", "Enters cloak and continuously drains energy.");
            case "Fighter Mode" -> tr("切换为机动战机形态。", "Switches into a mobile fighter configuration.");
            case "Mech Mode" -> tr("切换为机甲地面作战形态。", "Switches into a ground mech combat configuration.");
            case "Locus Mode" -> tr("变形成恶蝠形态以进行近距离火焰压制。", "Transforms into Hellbat mode for close-range flame combat.");
            case "Mace Mode" -> tr("变形成恶火形态以恢复高速机动。", "Transforms into Hellion mode to regain high mobility.");
            case "High Impact Payload" -> tr("雷神切换为高冲击对空弹药，强化单体对空伤害。", "Thor switches to high-impact anti-air payload for stronger single-target air damage.");
            case "Explosive Payload" -> tr("雷神切换为爆裂对空弹药，更适合压制轻型空军。", "Thor switches to explosive anti-air payload, better against light air units.");
            case "Auto Turret" -> tr("在目标点部署临时自动机炮台。", "Deploys a temporary auto turret at the target point.");
            case "Anti-Armor Missile" -> tr("发射反装甲导弹，对区域内目标施加易伤效果。", "Launches an anti-armor missile that makes units in the area take extra damage.");
            case "Interference Matrix" -> tr("短时间禁用敌方机械单位。", "Disables an enemy mechanical unit for a short duration.");
            case "Yamato Cannon" -> tr("蓄力后对单一目标造成极高伤害。", "Charges up and deals massive damage to a single target.");
            case "Tactical Jump" -> tr("短暂蓄力后跃迁到指定位置。", "Charges briefly, then warps to the selected location.");
            case "Defense Mode" -> tr("展开为防卫模式，只能攻击防卫圈内的地面目标。", "Deploys into defense mode and can only attack ground targets inside the defense circle.");
            case "Siege Mode" -> tr("展开为攻城模式，获得超远程对地范围火力，但无法移动。", "Deploys into siege mode for long-range anti-ground splash fire, but the tank cannot move.");
            case "Tank Mode" -> tr("收起炮架并恢复机动坦克形态。", "Packs up and returns to the mobile tank configuration.");
            case "Widow Burrow" -> tr("埋地进入隐蔽状态，准备伏击敌军。", "Burrows underground to hide and prepare an ambush.");
            case "Widow Reload" -> tr("重新装填寡妇雷导弹并准备再次伏击。", "Reloads the Widow Mine missile and prepares for another ambush.");
            case "Tech Addon" -> tr("为生产建筑加装科技实验室，解锁高级单位与研究。", "Adds a Tech Lab to unlock advanced units and research.");
            case "Double Addon" -> tr("为生产建筑加装反应堆，获得双队列生产能力。", "Adds a Reactor to grant dual production queues.");
            case "Skill Cooldown" -> tr("该技能仍在冷却中。", "This skill is still on cooldown.");
            default -> null;
        };
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

    private AbilityInfo makeAbilityInfo(Ability key, String name, String description){
        return makeAbilityInfo(hotkey(key), name, description);
    }

    private String hotkey(Ability key){
        return Sc2AbilityHotkeys.label(key);
    }

    private boolean hotkeyTapped(Ability key){
        return Sc2AbilityHotkeys.tapped(key);
    }

    private float hotkeyScale(String key){
        if(key == null || key.isEmpty()) return abilityKeyScale;
        if(key.length() <= 1) return abilityKeyScale;
        if(key.length() <= 3) return 0.52f;
        if(key.length() <= 6) return 0.44f;
        if(key.length() <= 9) return 0.36f;
        return 0.30f;
    }

    private AbilityInfo defaultAbilityInfo(String key){
        return makeAbilityInfo(key, "Ability", "Left-click acts as hotkey.");
    }

    private void bindAbilityHover(Button button, @Nullable AbilityInfo info, @Nullable Boolp enabled){
        if(button == null || info == null) return;
        button.update(() -> {
            boolean active = enabled == null || enabled.get();
            if(active && button.visible && button.isOver()){
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
        return addAutoCastIconButton(grid, key, icon, enabled, action, autoEnabled, toggleAuto, null);
    }

    private Button addAutoCastIconButton(Table grid, String key, Drawable icon, Boolp enabled, Runnable action, Boolp autoEnabled, Runnable toggleAuto, @Nullable AbilityInfo info){
        return addIconButton(grid, key, icon, enabled, action, autoEnabled, toggleAuto, info);
    }

    private Button addCountedIconButton(Table grid, String key, Drawable icon, Boolp enabled, Runnable action, Intp count){
        return addCountedIconButton(grid, key, icon, enabled, action, count, null);
    }

    private Button addCountedIconButton(Table grid, String key, Drawable icon, Boolp enabled, Runnable action, Intp count, @Nullable AbilityInfo info){
        Boolp allowed = enabled == null ? () -> true : enabled;
        Button button = new Button(Styles.clearNonei);
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
            keyLabel.setFontScale(hotkeyScale(key));
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
        bindAbilityHover(button, info == null ? defaultAbilityInfo(key) : info, allowed);
        return button;
    }

    private Button addIconButton(Table grid, String key, Drawable icon, Boolp enabled, Runnable action, @Nullable Boolp autoEnabled, @Nullable Runnable toggleAuto){
        return addIconButton(grid, key, icon, enabled, action, autoEnabled, toggleAuto, null);
    }

    private Button addIconButton(Table grid, String key, Drawable icon, Boolp enabled, Runnable action, @Nullable Boolp autoEnabled, @Nullable Runnable toggleAuto, @Nullable AbilityInfo info){
        Boolp allowed = enabled == null ? () -> true : enabled;
        Button button = new Button(Styles.clearNonei);
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
            keyLabel.setFontScale(hotkeyScale(key));
            keyLabel.update(() -> keyLabel.setColor(allowed.get() ? Color.white : Color.gray));
            keyTable.add(keyLabel).pad(3f);
            stack.add(keyTable);
        }

        button.add(stack).size(abilityButtonSize);
        grid.add(button).size(abilityButtonSize).pad(2f);
        bindAbilityHover(button, info == null ? defaultAbilityInfo(key) : info, allowed);
        return button;
    }

    private Button addBattlecruiserCooldownButton(Table grid, String key, Drawable icon, Boolp enabled, Runnable action, Floatp cooldownValue, Floatp cooldownTotal){
        return addBattlecruiserCooldownButton(grid, key, icon, enabled, action, cooldownValue, cooldownTotal, null);
    }

    private Button addBattlecruiserCooldownButton(Table grid, String key, Drawable icon, Boolp enabled, Runnable action, Floatp cooldownValue, Floatp cooldownTotal, @Nullable AbilityInfo info){
        Boolp allowed = enabled == null ? () -> true : enabled;
        Button button = new Button(Styles.clearNonei);
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
        keyLabel.setFontScale(hotkeyScale(key));
        keyLabel.update(() -> keyLabel.setColor(allowed.get() ? Color.white : Color.gray));
        keyTable.add(keyLabel).pad(3f);
        stack.add(keyTable);

        stack.add(yamatoCooldownOverlay(cooldownValue, cooldownTotal));

        button.add(stack).size(abilityButtonSize);
        grid.add(button).size(abilityButtonSize).pad(2f);
        bindAbilityHover(button, info == null ? makeAbilityInfo(key, "Skill Cooldown", "Left-click acts as hotkey.") : info, allowed);
        return button;
    }

    private Button addCooldownIconButton(Table grid, String key, Drawable icon, Boolp enabled, Runnable action, Floatp cooldownValue, Floatp cooldownTotal, @Nullable AbilityInfo info){
        Boolp allowed = enabled == null ? () -> true : enabled;
        Button button = new Button(Styles.clearNonei);
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
            keyLabel.setFontScale(hotkeyScale(key));
            keyLabel.update(() -> keyLabel.setColor(allowed.get() ? Color.white : Color.gray));
            keyTable.add(keyLabel).pad(3f);
            stack.add(keyTable);
        }

        stack.add(yamatoCooldownOverlay(cooldownValue, cooldownTotal));

        button.add(stack).size(abilityButtonSize);
        grid.add(button).size(abilityButtonSize).pad(2f);
        bindAbilityHover(button, info == null ? defaultAbilityInfo(key) : info, allowed);
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
        Button button = new Button(Styles.clearNonei);
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
        Label keyLabel = new Label(hotkey(Ability.hurricaneLock));
        keyLabel.setFontScale(hotkeyScale(hotkey(Ability.hurricaneLock)));
        keyLabel.update(() -> keyLabel.setColor(enabled.get() ? Color.white : Color.gray));
        keyTable.add(keyLabel).pad(3f);
        stack.add(keyTable);

        stack.add(yamatoCooldownOverlay(this::selectedHurricaneLockCooldown, UnitTypes::hurricaneLockCooldownDuration));

        button.add(stack).size(abilityButtonSize);
        grid.add(button).size(abilityButtonSize).pad(2f);
        bindAbilityHover(button, makeAbilityInfo(Ability.hurricaneLock, "Hurricane Lock", "Left-click locks target, right-click toggles auto-cast."), enabled);
        return button;
    }

    private Button addHoverableIconButton(Table grid, String key, Drawable icon, Boolp enabled, Runnable action){
        Boolp allowed = enabled == null ? () -> true : enabled;
        Button button = new Button(Styles.clearNonei);
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
            keyLabel.setFontScale(hotkeyScale(key));
            keyLabel.update(() -> keyLabel.setColor(allowed.get() ? Color.white : Color.gray));
            keyTable.add(keyLabel).pad(3f);
            stack.add(keyTable);
        }

        button.add(stack).size(abilityButtonSize);
        grid.add(button).size(abilityButtonSize).pad(2f);
        bindAbilityHover(button, defaultAbilityInfo(key), allowed);
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

    private BuildInfo makeWidowActionInfo(String key, String name, Color color, Floatp progress, Boolp visible, float timeSeconds){
        BuildInfo info = new BuildInfo();
        info.unit = UnitTypes.crawler;
        info.key = key;
        info.name = name;
        info.action = "Ability";
        info.crystalCost = 0;
        info.gasCost = 0;
        info.timeSeconds = timeSeconds;
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

    private String selectedBarracksStimpackCostLine(){
        boolean marine = false;
        boolean marauder = false;
        for(Unit unit : abilityUnits()){
            if(unit == null || !unit.isValid() || !UnitTypes.isBarracksStimpackUnit(unit)) continue;
            if(unit.type == UnitTypes.fortress){
                marauder = true;
            }else{
                marine = true;
            }
        }

        String label = tr("\u751f\u547d\u503c", "HP");
        if(marine && marauder) return label + " 10 / 20";
        if(marauder) return label + " 20";
        if(marine) return label + " 10";
        return label;
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
        if(block == null) return "";
        if(block == Blocks.coreNucleus) return "Command Center";
        if(block == Blocks.coreOrbital) return "Orbital Command";
        if(block == Blocks.corePlanetaryFortress) return "Planetary Fortress";
        if(block == Blocks.ventCondenser) return "Refinery";
        if(block == Blocks.doorLarge) return "Supply Depot";
        if(block == Blocks.groundFactory) return "Barracks";
        if(block == Blocks.multiPress) return "Engineering Bay";
        if(block == Blocks.atmosphericConcentrator) return "Bunker";
        if(block == Blocks.swarmer) return "Missile Turret";
        if(block == Blocks.radar) return "Sensor Tower";
        if(block == Blocks.launchPad) return "Ghost Academy";
        if(block == Blocks.tankFabricator) return "Factory";
        if(block == Blocks.siliconCrucible) return "Armory";
        if(block == Blocks.shipFabricator) return "Starport";
        if(block == Blocks.surgeCrucible) return "Fusion Core";
        if(block == Blocks.memoryBank) return "Tech Lab";
        if(block == Blocks.rotaryPump) return "Reactor";
        return block.localizedName;
    }

    private String sc2Name(UnitType unit){
        if(unit == null) return "";
        if(unit == UnitTypes.dagger) return "Marine";
        if(unit == UnitTypes.reaper) return "Reaper";
        if(unit == UnitTypes.fortress) return "Marauder";
        if(unit == UnitTypes.ghost) return "Ghost";
        if(unit == UnitTypes.mace) return "Hellion";
        if(unit == UnitTypes.locus) return "Hellbat";
        if(unit == UnitTypes.crawler) return "Widow Mine";
        if(unit == UnitTypes.hurricane) return "Cyclone";
        if(unit == UnitTypes.precept) return "Siege Tank";
        if(unit == UnitTypes.scepter) return "Thor";
        if(unit == UnitTypes.flare) return "Viking";
        if(unit == UnitTypes.mega) return "Medivac";
        if(unit == UnitTypes.liberator) return "Liberator";
        if(unit == UnitTypes.avert) return "Raven";
        if(unit == UnitTypes.horizon) return "Banshee";
        if(unit == UnitTypes.antumbra) return "Battlecruiser";
        if(unit == UnitTypes.nova) return "SCV";
        return unit.localizedName;
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

    private boolean anySelectedCoreCanQueueScv(){
        if(control == null || control.input == null) return false;
        for(Building build : control.input.commandBuildings){
            if(build instanceof CoreBuild core && core.isValid() && core.team == player.team()){
                if(core.canQueueUnit(UnitTypes.nova)){
                    return true;
                }
            }
        }
        return false;
    }

    private boolean anySelectedCoreHasQueue(){
        if(control == null || control.input == null) return false;
        for(Building build : control.input.commandBuildings){
            if(build instanceof CoreBuild core && core.isValid() && core.team == player.team()){
                if(core.unitQueue != null && !core.unitQueue.isEmpty()){
                    return true;
                }
            }
        }
        return false;
    }

    private float coreEffectiveQueue(CoreBuild core){
        if(core == null) return Float.POSITIVE_INFINITY;
        int active = Math.max(1, core.activeUnitSlots());
        int queued = core.unitQueue == null ? 0 : core.unitQueue.size;
        return (float)queued / (float)active;
    }

    private void queueSelectedCoreUnit(){
        if(control == null || control.input == null) return;

        CoreBuild chosen = null;
        float min = Float.POSITIVE_INFINITY;
        boolean anyCore = false;
        boolean anyNotUpgrading = false;
        boolean anyNotFull = false;

        for(Building build : control.input.commandBuildings){
            if(!(build instanceof CoreBuild core) || !core.isValid() || core.team != player.team()) continue;
            anyCore = true;
            if(core.isUpgrading()) continue;
            anyNotUpgrading = true;
            if(core.unitQueue == null || core.unitQueue.size < core.queueSlots()){
                anyNotFull = true;
            }
            if(!core.canQueueUnit(UnitTypes.nova)) continue;

            float load = coreEffectiveQueue(core);
            if(load < min - 0.0001f || (Mathf.equal(load, min, 0.0001f) && (chosen == null || core.id < chosen.id))){
                min = load;
                chosen = core;
            }
        }

        if(!anyCore) return;
        if(chosen == null){
            if(!anyNotUpgrading){
                ui.hudfrag.setHudText("Cannot train while upgrading");
            }else if(!anyNotFull){
                ui.hudfrag.setHudText("Queue full");
            }else{
                ui.hudfrag.setHudText(Core.bundle.get("bar.noresources", "Not enough resources"));
            }
            return;
        }

        Call.coreQueueUnit(player, chosen.pos(), UnitTypes.nova.id);
        coreDistributeHistoryCore.add(chosen.id);
        if(coreDistributeHistoryCore.size > 1024){
            coreDistributeHistoryCore.removeIndex(0);
        }
    }

    private void cancelSelectedCoreUnit(){
        if(control == null || control.input == null) return;

        for(int i = coreDistributeHistoryCore.size - 1; i >= 0; i--){
            int coreId = coreDistributeHistoryCore.get(i);
            coreDistributeHistoryCore.removeIndex(i);

            CoreBuild found = null;
            for(Building build : control.input.commandBuildings){
                if(build instanceof CoreBuild core && core.isValid() && core.team == player.team() && core.id == coreId){
                    found = core;
                    break;
                }
            }
            if(found != null && found.unitQueue != null && !found.unitQueue.isEmpty()){
                Call.coreCancelUnit(player, found.pos());
                return;
            }
        }

        CoreBuild best = null;
        float bestLoad = -1f;
        for(Building build : control.input.commandBuildings){
            if(!(build instanceof CoreBuild core) || !core.isValid() || core.team != player.team()) continue;
            if(core.unitQueue == null || core.unitQueue.isEmpty()) continue;
            float load = coreEffectiveQueue(core);
            if(load > bestLoad){
                bestLoad = load;
                best = core;
            }
        }
        if(best != null){
            Call.coreCancelUnit(player, best.pos());
        }
    }

    private boolean anySelectedCoreCanStartOrbitalUpgrade(){
        if(control == null || control.input == null) return false;
        for(Building build : control.input.commandBuildings){
            if(build instanceof CoreBuild core && core.isValid() && core.team == player.team()){
                if(core.canStartOrbitalUpgrade()){
                    return true;
                }
            }
        }
        return false;
    }

    private boolean anySelectedCoreCanStartFortressUpgrade(){
        if(control == null || control.input == null) return false;
        for(Building build : control.input.commandBuildings){
            if(build instanceof CoreBuild core && core.isValid() && core.team == player.team()){
                if(core.canStartFortressUpgrade()){
                    return true;
                }
            }
        }
        return false;
    }

    private boolean anySelectedCoreUpgradingOrbital(){
        if(control == null || control.input == null) return false;
        for(Building build : control.input.commandBuildings){
            if(build instanceof CoreBuild core && core.isValid() && core.team == player.team()){
                if(core.isUpgradingOrbital()){
                    return true;
                }
            }
        }
        return false;
    }

    private boolean anySelectedCoreUpgradingFortress(){
        if(control == null || control.input == null) return false;
        for(Building build : control.input.commandBuildings){
            if(build instanceof CoreBuild core && core.isValid() && core.team == player.team()){
                if(core.isUpgradingFortress()){
                    return true;
                }
            }
        }
        return false;
    }

    private void queueSelectedCoreUpgrade(int type){
        if(control == null || control.input == null) return;

        Seq<CoreBuild> eligible = new Seq<>();
        boolean anyNucleus = false;
        boolean anyTraining = false;
        boolean anyUpgrading = false;
        boolean anyMissingEngineering = false;
        boolean anyResources = false;
        for(Building build : control.input.commandBuildings){
            if(!(build instanceof CoreBuild core) || !core.isValid() || core.team != player.team()) continue;
            if(core.block == Blocks.coreNucleus){
                anyNucleus = true;
            }
            if(core.isUpgrading()){
                anyUpgrading = true;
            }
            if(core.unitQueue != null && !core.unitQueue.isEmpty()){
                anyTraining = true;
            }
            if(type == coreUpgradeFortress && !core.hasEngineeringStation()){
                anyMissingEngineering = true;
            }
            if(state.rules.infiniteResources || core.team.rules().infiniteResources){
                anyResources = true;
            }else if(core.items != null){
                if(type == coreUpgradeOrbital){
                    if(core.items.has(Items.graphite, CoreBlock.orbitalUpgradeCost)){
                        anyResources = true;
                    }
                }else{
                    if(core.items.has(Items.graphite, CoreBlock.fortressUpgradeCost) && core.items.has(Items.highEnergyGas, CoreBlock.fortressUpgradeGasCost)){
                        anyResources = true;
                    }
                }
            }
            boolean can = type == coreUpgradeOrbital ? core.canStartOrbitalUpgrade() : core.canStartFortressUpgrade();
            if(!can) continue;
            eligible.add(core);
        }
        sortCoresByBuildOrder(eligible);
        CoreBuild chosen = chooseCoreRoundRobin(eligible, coreDistributeLastUpgradeCoreId.get(type, -1));
        if(chosen == null){
            if(!anyNucleus){
                ui.hudfrag.setHudText("Already upgraded");
            }else if(anyTraining){
                ui.hudfrag.setHudText("Cannot upgrade while training");
            }else if(anyUpgrading){
                ui.hudfrag.setHudText("Upgrade already in progress");
            }else if(type == coreUpgradeFortress && anyMissingEngineering){
                ui.hudfrag.setHudText("Requires Engineering Station");
            }else if(!anyResources){
                ui.hudfrag.setHudText(type == coreUpgradeOrbital ? "Not enough crystals" : "Not enough crystals or gas");
            }else{
                ui.hudfrag.setHudText("No eligible base");
            }
            return;
        }

        if(type == coreUpgradeOrbital){
            Call.coreStartOrbitalUpgrade(player, chosen.pos());
        }else{
            Call.coreStartFortressUpgrade(player, chosen.pos());
        }

        coreDistributeLastUpgradeCoreId.put(type, chosen.id);
        coreDistributeHistoryUpgradeCore.add(chosen.id);
        coreDistributeHistoryUpgradeType.add(type);
        if(coreDistributeHistoryUpgradeCore.size > 1024){
            coreDistributeHistoryUpgradeCore.removeIndex(0);
            coreDistributeHistoryUpgradeType.removeIndex(0);
        }
    }

    private void cancelSelectedCoreUpgrade(int type){
        if(control == null || control.input == null) return;

        for(int i = coreDistributeHistoryUpgradeCore.size - 1; i >= 0; i--){
            if(coreDistributeHistoryUpgradeType.get(i) != type) continue;

            int coreId = coreDistributeHistoryUpgradeCore.get(i);
            coreDistributeHistoryUpgradeCore.removeIndex(i);
            coreDistributeHistoryUpgradeType.removeIndex(i);

            CoreBuild found = null;
            for(Building build : control.input.commandBuildings){
                if(build instanceof CoreBuild core && core.isValid() && core.team == player.team() && core.id == coreId){
                    found = core;
                    break;
                }
            }
            if(found == null) continue;

            if(type == coreUpgradeOrbital && found.isUpgradingOrbital()){
                Call.coreCancelOrbitalUpgrade(player, found.pos());
                return;
            }
            if(type == coreUpgradeFortress && found.isUpgradingFortress()){
                Call.coreCancelFortressUpgrade(player, found.pos());
                return;
            }
        }

        for(Building build : control.input.commandBuildings){
            if(!(build instanceof CoreBuild core) || !core.isValid() || core.team != player.team()) continue;
            if(type == coreUpgradeOrbital && core.isUpgradingOrbital()){
                Call.coreCancelOrbitalUpgrade(player, core.pos());
                return;
            }
            if(type == coreUpgradeFortress && core.isUpgradingFortress()){
                Call.coreCancelFortressUpgrade(player, core.pos());
                return;
            }
        }
    }

    private boolean anySelectedCoreUpgrading(){
        return anySelectedCoreUpgradingOrbital() || anySelectedCoreUpgradingFortress();
    }

    private void cancelSelectedCoreAnyUpgrade(){
        if(control == null || control.input == null) return;

        for(int i = coreDistributeHistoryUpgradeCore.size - 1; i >= 0; i--){
            int type = coreDistributeHistoryUpgradeType.get(i);
            int coreId = coreDistributeHistoryUpgradeCore.get(i);
            coreDistributeHistoryUpgradeCore.removeIndex(i);
            coreDistributeHistoryUpgradeType.removeIndex(i);

            CoreBuild found = null;
            for(Building build : control.input.commandBuildings){
                if(build instanceof CoreBuild core && core.isValid() && core.team == player.team() && core.id == coreId){
                    found = core;
                    break;
                }
            }
            if(found == null) continue;

            if(type == coreUpgradeOrbital && found.isUpgradingOrbital()){
                Call.coreCancelOrbitalUpgrade(player, found.pos());
                return;
            }
            if(type == coreUpgradeFortress && found.isUpgradingFortress()){
                Call.coreCancelFortressUpgrade(player, found.pos());
                return;
            }
        }

        for(Building build : control.input.commandBuildings){
            if(build instanceof CoreBuild core && core.isValid() && core.team == player.team()){
                if(core.isUpgradingOrbital()){
                    Call.coreCancelOrbitalUpgrade(player, core.pos());
                    return;
                }
            }
        }
        for(Building build : control.input.commandBuildings){
            if(build instanceof CoreBuild core && core.isValid() && core.team == player.team()){
                if(core.isUpgradingFortress()){
                    Call.coreCancelFortressUpgrade(player, core.pos());
                    return;
                }
            }
        }
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
                if(hotkeyTapped(Ability.novaHarvest)){
                    enterCommandMode(CommandMode.HARVEST);
                }else if(hotkeyTapped(Ability.novaRepair)){
                    enterCommandMode(CommandMode.REPAIR);
                }else if(hotkeyTapped(Ability.novaBuildBasic)){
                    novaPanel = NovaPanel.BUILD_BASIC;
                }else if(hotkeyTapped(Ability.novaBuildAdvanced)){
                    novaPanel = NovaPanel.BUILD_ADV;
                }
                break;
            case BUILD_BASIC:
                if(Core.input.keyTap(KeyCode.escape)){
                    novaPanel = NovaPanel.MAIN;
                    break;
                }
                if(hotkeyTapped(Ability.novaBuildCommandCenter)){
                    startPlacement(Blocks.coreNucleus);
                }else if(hotkeyTapped(Ability.novaBuildRefinery)){
                    startPlacement(Blocks.ventCondenser);
                }else if(hotkeyTapped(Ability.novaBuildSupplyDepot)){
                    startPlacement(Blocks.doorLarge);
                }else if(hotkeyTapped(Ability.novaBuildBarracks)){
                    startPlacement(Blocks.groundFactory);
                }else if(hotkeyTapped(Ability.novaBuildEngineeringBay)){
                    startPlacement(Blocks.multiPress);
                }else if(hotkeyTapped(Ability.novaBuildBunker)){
                    startPlacement(Blocks.atmosphericConcentrator);
                }else if(hotkeyTapped(Ability.novaBuildMissileTurret)){
                    startPlacement(Blocks.swarmer);
                }else if(hotkeyTapped(Ability.novaBuildSensorTower)){
                    startPlacement(Blocks.radar);
                }
                break;
            case BUILD_ADV:
                if(Core.input.keyTap(KeyCode.escape)){
                    novaPanel = NovaPanel.MAIN;
                    break;
                }
                if(hotkeyTapped(Ability.novaBuildGhostAcademy)){
                    startPlacement(Blocks.launchPad);
                }else if(hotkeyTapped(Ability.novaBuildFactory)){
                    startPlacement(Blocks.tankFabricator);
                }else if(hotkeyTapped(Ability.novaBuildArmory)){
                    startPlacement(Blocks.siliconCrucible);
                }else if(hotkeyTapped(Ability.novaBuildStarport)){
                    startPlacement(Blocks.shipFabricator);
                }else if(hotkeyTapped(Ability.novaBuildFusionCore)){
                    startPlacement(Blocks.surgeCrucible);
                }
                break;
        }
    }

    private void handleWidowHotkeys(){
        if(hotkeyTapped(Ability.widowBurrow)){
            issueWidowBurrowCommand(true);
        }else if(hotkeyTapped(Ability.widowUnburrow)){
            issueWidowBurrowCommand(false);
        }
    }

    private void handleMaceLocusHotkeys(){
        if(hotkeyTapped(Ability.hellionToHellbat)){
            if(anyMaceCanTransformToLocus()){
                issueMaceLocusModeCommand(true);
            }else if(!UnitTypes.infantryWeaponHasArmory(player.team())){
                ui.hudfrag.setHudText("Requires Armory");
            }
        }else if(hotkeyTapped(Ability.hellbatToHellion)){
            if(anyLocusCanTransformToMace()){
                issueMaceLocusModeCommand(false);
            }else if(!UnitTypes.infantryWeaponHasArmory(player.team())){
                ui.hudfrag.setHudText("Requires Armory");
            }
        }
    }

    private void handleMaceHotkeys(){
        if(hotkeyTapped(Ability.hellionToHellbat)){
            if(anyMaceCanTransformToLocus()){
                issueMaceLocusModeCommand(true);
            }else if(!UnitTypes.infantryWeaponHasArmory(player.team())){
                ui.hudfrag.setHudText("Requires Armory");
            }
        }
    }

    private void handleLocusHotkeys(){
        if(hotkeyTapped(Ability.hellbatToHellion)){
            if(anyLocusCanTransformToMace()){
                issueMaceLocusModeCommand(false);
            }else if(!UnitTypes.infantryWeaponHasArmory(player.team())){
                ui.hudfrag.setHudText("Requires Armory");
            }
        }
    }

    private void handlePreceptHotkeys(){
        if(hotkeyTapped(Ability.siegeTankSiegeMode)){
            issuePreceptSiegeCommand(true);
        }else if(hotkeyTapped(Ability.siegeTankTankMode)){
            issuePreceptSiegeCommand(false);
        }
    }

    private void handleHurricaneHotkeys(){
        if(hotkeyTapped(Ability.hurricaneLock)){
            issueHurricaneLockCommand();
        }
    }

    private void handleScepterHotkeys(){
        if(hotkeyTapped(Ability.thorHighImpact)){
            issueScepterAirModeCommand(true);
        }else if(hotkeyTapped(Ability.thorExplosive)){
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

        if(hotkeyTapped(Ability.liberatorDefenseMode) && anyLiberatorCanEnterDefense()){
            enterCommandMode(CommandMode.LIBERATOR_ZONE);
        }else if(hotkeyTapped(Ability.liberatorFighterMode) && anyLiberatorCanExitDefense()){
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

        if(hotkeyTapped(Ability.medivacHeal)){
            enterCommandMode(CommandMode.MEDIVAC_HEAL);
        }else if(hotkeyTapped(Ability.medivacAfterburners)){
            issueMedivacAfterburnerCommand();
        }else if(hotkeyTapped(Ability.medivacLoad) && anyMedivacCanLoadMore()){
            enterCommandMode(CommandMode.MEDIVAC_LOAD);
        }else if(hotkeyTapped(Ability.medivacUnload) && anyMedivacHasPayload()){
            enterCommandMode(CommandMode.MEDIVAC_UNLOAD);
        }
    }

    private void handleVikingHotkeys(){
        if(hotkeyTapped(Ability.vikingFighterMode) && anyVikingCanSwitchToFighter()){
            issueVikingModeCommand(false);
        }else if(hotkeyTapped(Ability.vikingMechMode) && anyVikingCanSwitchToMech()){
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

        if(hotkeyTapped(Ability.ghostNuke)){
            if(anyGhostCanUseTacticalNuke()){
                enterCommandMode(CommandMode.GHOST_TACTICAL_NUKE);
            }else{
                ui.hudfrag.setHudText("No available warhead");
            }
            return;
        }

        if(hotkeyTapped(Ability.ghostStableAim)){
            if(anyGhostCanUseStableAim()){
                enterCommandMode(CommandMode.GHOST_STABLE_AIM);
            }else{
                ui.hudfrag.setHudText("Cannot use Stable Aim");
            }
            return;
        }

        if(hotkeyTapped(Ability.ghostEmp)){
            if(anyGhostCanUseEmp()){
                enterCommandMode(CommandMode.GHOST_EMP);
            }else{
                ui.hudfrag.setHudText("Cannot use EMP");
            }
            return;
        }

        if(hotkeyTapped(Ability.ghostCloak)){
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

        if(hotkeyTapped(Ability.reaperKd8)){
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

        if(hotkeyTapped(Ability.battlecruiserYamato)){
            if(anyBattlecruiserCanUseYamato()){
                enterCommandMode(CommandMode.BATTLECRUISER_YAMATO);
            }else if(!UnitTypes.battlecruiserHasYamatoTech(player.team())){
                ui.hudfrag.setHudText("Requires Weapon Refit");
            }
        }else if(hotkeyTapped(Ability.battlecruiserJump) && anyBattlecruiserCanUseWarp()){
            enterCommandMode(CommandMode.BATTLECRUISER_WARP);
        }
    }

    private void handleBansheeHotkeys(){
        if(hotkeyTapped(Ability.bansheeCloak)){
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

        if(hotkeyTapped(Ability.ravenTurret) && anyRavenCanDeployTurret()){
            enterCommandMode(CommandMode.RAVEN_TURRET);
        }else if(hotkeyTapped(Ability.ravenAntiArmor) && anyRavenCanUseAntiArmor()){
            enterCommandMode(CommandMode.RAVEN_ANTI_ARMOR);
        }else if(hotkeyTapped(Ability.ravenMatrix)){
            if(anyRavenCanUseMatrix()){
                enterCommandMode(CommandMode.RAVEN_MATRIX);
            }else if(UnitTypes.ravenMatrixTechLevel(player.team()) <= 0){
                ui.hudfrag.setHudText("Requires Interference Matrix");
            }
        }
    }

    private void handleBarracksStimpackHotkeys(){
        if(hotkeyTapped(Ability.stimpack)){
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
            queueSelectedCoreUnit();
            corePanel = CorePanel.BUILD;
        }else if(Core.input.keyTap(KeyCode.b)){
            queueSelectedCoreUpgrade(coreUpgradeOrbital);
        }else if(Core.input.keyTap(KeyCode.p)){
            queueSelectedCoreUpgrade(coreUpgradeFortress);
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
            if(anySelectedCoreUpgrading()){
                cancelSelectedCoreAnyUpgrade();
            }else if(anySelectedCoreHasQueue()){
                cancelSelectedCoreUnit();
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

                if(Core.input.keyTap(KeyCode.a) && daggerIndex != -1 && anyAbilityFactoryCanQueue(factory, daggerIndex)){
                    queueAbilityFactoryPlan(factory, daggerIndex);
                }else if(Core.input.keyTap(KeyCode.r) && reaperIndex != -1 && anyAbilityFactoryCanQueue(factory, reaperIndex)){
                    queueAbilityFactoryPlan(factory, reaperIndex);
                }else if(Core.input.keyTap(KeyCode.d) && fortressIndex != -1 && anyAbilityFactoryCanQueue(factory, fortressIndex)){
                    queueAbilityFactoryPlan(factory, fortressIndex);
                }else if(Core.input.keyTap(KeyCode.g) && ghostIndex != -1 && anyAbilityFactoryCanQueue(factory, ghostIndex)){
                    queueAbilityFactoryPlan(factory, ghostIndex);
                }
            }else if(block == Blocks.tankFabricator){
                int locusIndex = block.plans.indexOf(p -> p.unit == UnitTypes.locus);
                int crawlerIndex = block.plans.indexOf(p -> p.unit == UnitTypes.crawler);
                int hurricaneIndex = block.plans.indexOf(p -> p.unit == UnitTypes.hurricane);
                int preceptIndex = block.plans.indexOf(p -> p.unit == UnitTypes.precept);
                int maceIndex = block.plans.indexOf(p -> p.unit == UnitTypes.mace);
                int scepterIndex = block.plans.indexOf(p -> p.unit == UnitTypes.scepter);

                if(Core.input.keyTap(KeyCode.e) && locusIndex != -1 && anyAbilityFactoryCanQueue(factory, locusIndex)){
                    queueAbilityFactoryPlan(factory, locusIndex);
                }else if(Core.input.keyTap(KeyCode.d) && crawlerIndex != -1 && anyAbilityFactoryCanQueue(factory, crawlerIndex)){
                    queueAbilityFactoryPlan(factory, crawlerIndex);
                }else if(Core.input.keyTap(KeyCode.n) && hurricaneIndex != -1 && anyAbilityFactoryCanQueue(factory, hurricaneIndex)){
                    queueAbilityFactoryPlan(factory, hurricaneIndex);
                }else if(Core.input.keyTap(KeyCode.s) && preceptIndex != -1 && anyAbilityFactoryCanQueue(factory, preceptIndex)){
                    queueAbilityFactoryPlan(factory, preceptIndex);
                }else if(Core.input.keyTap(KeyCode.r) && maceIndex != -1 && anyAbilityFactoryCanQueue(factory, maceIndex)){
                    queueAbilityFactoryPlan(factory, maceIndex);
                }else if(Core.input.keyTap(KeyCode.t) && scepterIndex != -1 && anyAbilityFactoryCanQueue(factory, scepterIndex)){
                    queueAbilityFactoryPlan(factory, scepterIndex);
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

                if(Core.input.keyTap(KeyCode.v) && flareIndex != -1 && anyAbilityFactoryCanQueue(factory, flareIndex)){
                    queueAbilityFactoryPlan(factory, flareIndex);
                }else if(Core.input.keyTap(KeyCode.d) && megaIndex != -1 && anyAbilityFactoryCanQueue(factory, megaIndex)){
                    queueAbilityFactoryPlan(factory, megaIndex);
                }else if(Core.input.keyTap(KeyCode.n) && liberatorIndex != -1 && anyAbilityFactoryCanQueue(factory, liberatorIndex)){
                    queueAbilityFactoryPlan(factory, liberatorIndex);
                }else if(Core.input.keyTap(KeyCode.r) && avertIndex != -1 && anyAbilityFactoryCanQueue(factory, avertIndex)){
                    queueAbilityFactoryPlan(factory, avertIndex);
                }else if(Core.input.keyTap(KeyCode.e) && horizonIndex != -1 && anyAbilityFactoryCanQueue(factory, horizonIndex)){
                    queueAbilityFactoryPlan(factory, horizonIndex);
                }else if(Core.input.keyTap(KeyCode.b) && antumbraIndex != -1 && anyAbilityFactoryCanQueue(factory, antumbraIndex)){
                    queueAbilityFactoryPlan(factory, antumbraIndex);
                }else if(Core.input.keyTap(KeyCode.y)){
                    enterCommandMode(CommandMode.RALLY);
                }
            }else{
                KeyCode[] keyCodes = {KeyCode.a, KeyCode.r, KeyCode.d, KeyCode.g, KeyCode.f, KeyCode.t};
                for(int i = 0; i < keyCodes.length && i < block.plans.size; i++){
                    if(Core.input.keyTap(keyCodes[i]) && anyAbilityFactoryCanQueue(factory, i)){
                        queueAbilityFactoryPlan(factory, i);
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

            if(Core.input.keyTap(KeyCode.x) && anyAbilityFactoryCanStartAddon(factory, Blocks.memoryBank, UnitFactory.sc2AddonCrystalCost, UnitFactory.sc2AddonTechGasCost)){
                queueAbilityFactoryAddon(factory, UnitFactory.sc2AddonTechConfig, Blocks.memoryBank, UnitFactory.sc2AddonCrystalCost, UnitFactory.sc2AddonTechGasCost);
            }else if(Core.input.keyTap(KeyCode.c) && anyAbilityFactoryCanStartAddon(factory, Blocks.rotaryPump, UnitFactory.sc2AddonCrystalCost, UnitFactory.sc2AddonDoubleGasCost)){
                queueAbilityFactoryAddon(factory, UnitFactory.sc2AddonDoubleConfig, Blocks.rotaryPump, UnitFactory.sc2AddonCrystalCost, UnitFactory.sc2AddonDoubleGasCost);
            }
            if(Core.input.keyTap(KeyCode.escape)){
                cancelAbilityFactoryQueued(factory);
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
        if(!core.canLift()) return;

        Unit unit = core.lift();
        if(unit != null){
            control.input.commandBuildings.clear();
            control.input.selectedUnits.clear();
            control.input.selectedUnits.add(unit);
        }
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
        Unit unit = factory.lift();
        if(unit != null){
            control.input.commandBuildings.clear();
            control.input.selectedUnits.clear();
            control.input.selectedUnits.add(unit);
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
                Call.commandHoldPosition(player, new int[]{unit.id}, new Vec2(unit.x, unit.y));
            }
        }
        exitCommandMode();
    }
}


