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
import arc.graphics.g2d.Lines;
import mindustry.content.*;
import mindustry.entities.units.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.input.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.ConstructBlock.*;
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

    //RTS command mode state
    public enum CommandMode{
        NONE,
        MOVE,
        STOP,
        HOLD,
        PATROL,
        ATTACK,
        HARVEST,
        RALLY,
        BUILD_PLACE
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

    public CommandMode activeCommand = CommandMode.NONE;
    private NovaPanel novaPanel = NovaPanel.MAIN;
    private CorePanel corePanel = CorePanel.MAIN;
    private @Nullable Block placingBlock;
    private @Nullable BuildInfo hoverBuildInfo;
    private Table mainPanel;
    private Table commandModePanel;

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
        int crystalCost;
        int gasCost;
        int timeSeconds;
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
        margin(4f);

        mainPanel = new Table();
        commandModePanel = new Table();

        update(() -> {
            if(control.input.selectedUnits.size > 0 || control.input.commandBuildings.size > 0){
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
                    boolean allowRtsKeys = !control.input.selectedUnits.isEmpty();
                    if(isOnlyNovaSelected() && (novaPanel != NovaPanel.MAIN || activeCommand == CommandMode.BUILD_PLACE)){
                        allowRtsKeys = false;
                    }
                    if(allowRtsKeys){
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

                    if(isOnlyNovaSelected()){
                        handleNovaHotkeys();
                    }else if(control.input.selectedUnits.isEmpty() && control.input.commandBuildings.size > 0){
                        if(coreSelected){
                            handleCoreHotkeys();
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

    private void rebuild(){
        clearChildren();

        if(control.input.selectedUnits.isEmpty() && control.input.commandBuildings.isEmpty()){
            add("No units selected").color(Color.lightGray).pad(10f);
            return;
        }

        if(control.input.selectedUnits.isEmpty() && control.input.commandBuildings.size > 0){
            buildBuildingPanel();
            return;
        }

        if(activeCommand != CommandMode.NONE && activeCommand != CommandMode.HARVEST && activeCommand != CommandMode.BUILD_PLACE){
            buildCommandModePanel();
        }else{
            buildMainPanel();
        }
    }

    private void buildMainPanel(){
        if(isOnlyNovaSelected() && activeCommand == CommandMode.BUILD_PLACE){
            buildNovaPlacementPanel();
        }else if(isOnlyNovaSelected()){
            buildNovaPanel();
        }else{
            buildDefaultPanel();
        }
    }

    private void buildDefaultPanel(){
        //Check if we have any units selected (not just buildings)
        boolean hasUnits = control.input.selectedUnits.size > 0;

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

        for(Unit unit : control.input.selectedUnits){
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
                add().size(abilityButtonSize).pad(2f);
                col++;
            }
            row();
        }

        //Third row: Additional abilities or empty
        //Fill third row with empty slots to maintain 3-row layout
        for(int i = 0; i < COLS; i++){
            add().size(abilityButtonSize).pad(2f);
        }
    }

    private void buildNovaPanel(){
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
        fillRow(grid, 1, 1);
        grid.row();

        //Row 3
        addIconButton(grid, "b", Icon.hammer, () -> true, () -> novaPanel = NovaPanel.BUILD_BASIC);
        addIconButton(grid, "v", Icon.wrench, () -> true, () -> novaPanel = NovaPanel.BUILD_ADV);
        fillRow(grid, 2, 2);

        add(grid);
    }

    private void buildNovaPlacementPanel(){
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
        Table info = buildBuildInfoTable();
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
        addBuildButton(grid, "n", Blocks.hail, () -> Build.meetsPrerequisites(Blocks.hail, player.team()), () -> startPlacement(Blocks.hail));
        addEmpty(grid);
        addEscButton(grid, () -> novaPanel = NovaPanel.MAIN);

        Table root = new Table();
        root.add(info).growX().padBottom(4f).row();
        root.add(grid);
        add(root);
    }

    private void buildNovaAdvancedPanel(){
        Table info = buildBuildInfoTable();
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

        Table root = new Table();
        root.add(info).growX().padBottom(4f).row();
        root.add(grid);
        add(root);
    }

    private void buildBuildingPanel(){
        if(control.input.commandBuildings.isEmpty()){
            add("No units selected").color(Color.lightGray).pad(10f);
            return;
        }

        Building build = control.input.commandBuildings.first();
        if(build instanceof CoreBuild core){
            buildCorePanel(core);
            return;
        }

        if(build instanceof UnitFactory.UnitFactoryBuild factory && factory.sc2QueueEnabled()){
            buildFactoryPanel(factory);
            return;
        }

        if(!(build instanceof ConstructBuild)){
            add(build.block.localizedName).color(Color.lightGray).pad(10f);
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
                        grid.add().size(abilityButtonSize).pad(2f);
                    }
                }else if(r == 2 && c == 3 && incomplete){
                    Unit builder = findActiveBuilder(cons);
                    if(builder != null){
                        addIconButton(grid, "t", Icon.pause, () -> true, () -> pauseBuilder(builder));
                    }else{
                        grid.add().size(abilityButtonSize).pad(2f);
                    }
                }else if(r == 2 && c == 4 && incomplete){
                    addIconButton(grid, "Esc", Icon.cancel, () -> true, () -> cancelConstruct(cons));
                }else{
                    grid.add().size(abilityButtonSize).pad(2f);
                }
            }
            grid.row();
        }

        add(grid);
    }

    private void buildFactoryPanel(UnitFactory.UnitFactoryBuild factory){
        Table info = buildBuildInfoTable();
        Table grid = new Table();
        boolean showAddonButtons = factory.canShowAddonButtons();
        UnitFactory block = (UnitFactory)factory.block;
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
        addEmpty(grid);
        addCancelButton(grid, () -> factory.configure(UnitFactory.sc2AddonCancelConfig));

        Table root = new Table();
        root.add(info).growX().padBottom(4f).row();
        root.add(grid);
        add(root);
    }

    private void buildCorePanel(CoreBuild core){
        if(activeCommand == CommandMode.RALLY){
            buildCoreRallyPanel();
            return;
        }
        buildCoreMainPanel(core);
    }

    private void buildCoreMainPanel(CoreBuild core){
        Table grid = new Table();

        //Row 1
        addIconButton(grid, "s", new TextureRegionDrawable(UnitTypes.nova.uiIcon), () -> true, () -> {
            queueCoreUnit(core);
            corePanel = CorePanel.BUILD;
        });
        addEmpty(grid);
        addEmpty(grid);
        addIconButton(grid, "b", new TextureRegionDrawable(Blocks.shipFabricator.uiIcon), () -> true, this::showNotImplemented);
        addIconButton(grid, "p", new TextureRegionDrawable(Blocks.coreNucleus.uiIcon), () -> true, this::showNotImplemented);
        grid.row();

        //Row 2
        addEmpty(grid);
        addEmpty(grid);
        addEmpty(grid);
        addEmpty(grid);
        addIconButton(grid, "y", Icon.commandRally, () -> true, () -> enterCommandMode(CommandMode.RALLY));
        grid.row();

        //Row 3
        addIconButton(grid, "o", Icon.upload, () -> true, this::showNotImplemented);
        addEmpty(grid);
        addEmpty(grid);
        addIconButton(grid, "l", Icon.export, () -> true, () -> launchCore(core));
        addEmpty(grid);

        add(grid);
    }

    private void buildCoreRallyPanel(){
        add("Rally Point").style(Styles.outlineLabel).color(Pal.accent).pad(4f).row();
        add("Left-click to set rally point").color(Color.lightGray).pad(2f).row();
        add("Right-click or Esc to cancel").color(Color.lightGray).pad(2f).row();

        Table grid = new Table();
        for(int i = 0; i < ROWS * COLS; i++){
            if(i == 14){
                addEscButton(grid, this::exitCommandMode);
            }else{
                addEmpty(grid);
            }
            if((i + 1) % COLS == 0){
                grid.row();
            }
        }
        add(grid);
    }

    private Element borderElement(){
        return new Element(){
            @Override
            public void draw(){
                Draw.color(abilityBorderColor);
                Lines.stroke(1.5f);
                float inset = 1.5f;
                float innerInset = 4.5f;
                Lines.rect(x + inset, y + inset, width - inset * 2f, height - inset * 2f);
                Lines.rect(x + innerInset, y + innerInset, width - innerInset * 2f, height - innerInset * 2f);
                Draw.reset();
            }
        };
    }

    private Button addIconButton(Table grid, String key, Drawable icon, Boolp enabled, Runnable action){
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

        button.add(stack).size(abilityButtonSize);
        grid.add(button).size(abilityButtonSize).pad(2f);
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

        return info;
    }

    private BuildInfo makeAddonInfo(Block block, String key, String name, int crystalCost, int gasCost, float buildTime){
        BuildInfo info = new BuildInfo();
        info.block = block;
        info.key = key;
        info.name = name;
        info.crystalCost = crystalCost;
        info.gasCost = gasCost;
        info.timeSeconds = Math.round(buildTime / 60f);
        return info;
    }

    private BuildInfo makeBuildInfo(Block block, String key){
        BuildInfo info = new BuildInfo();
        info.block = block;
        info.key = key;
        info.name = sc2Name(block);
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
        info.crystalCost = getCost(plan.requirements, Items.graphite);
        info.gasCost = getCost(plan.requirements, Items.highEnergyGas);
        info.timeSeconds = Math.round(plan.time / 60f);
        return info;
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
        if(block == Blocks.coreNucleus) return "基地";
        if(block == Blocks.ventCondenser) return "精炼厂";
        if(block == Blocks.doorLarge) return "补给站";
        if(block == Blocks.groundFactory) return "兵营";
        if(block == Blocks.multiPress) return "工程站";
        if(block == Blocks.atmosphericConcentrator) return "地堡";
        if(block == Blocks.swarmer) return "导弹塔";
        if(block == Blocks.hail) return "感应塔";
        if(block == Blocks.launchPad) return "幽灵军校";
        if(block == Blocks.tankFabricator) return "重工厂";
        if(block == Blocks.siliconCrucible) return "军械库";
        if(block == Blocks.shipFabricator) return "星港";
        if(block == Blocks.surgeCrucible) return "聚变芯体";
        return block.localizedName;
    }

    private String sc2Name(UnitType unit){
        if(unit == UnitTypes.dagger) return "枪兵";
        if(unit == UnitTypes.reaper) return "死神";
        if(unit == UnitTypes.fortress) return "劫掠者";
        if(unit == UnitTypes.ghost) return "幽灵";
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
        grid.add().size(abilityButtonSize).pad(2f);
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
        if(control.input.selectedUnits.isEmpty()) return false;
        for(Unit unit : control.input.selectedUnits){
            if(unit == null || !unit.isValid() || !unit.type.name.equals("nova")) return false;
        }
        return true;
    }

    private boolean isOnlyCoreSelected(){
        if(!control.input.selectedUnits.isEmpty() || control.input.commandBuildings.isEmpty()) return false;
        for(Building build : control.input.commandBuildings){
            if(!(build instanceof CoreBuild)) return false;
        }
        return true;
    }

    private @Nullable CoreBuild selectedCore(){
        if(!isOnlyCoreSelected()) return null;
        return (CoreBuild)control.input.commandBuildings.first();
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
                    startPlacement(Blocks.hail);
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

    private void handleCoreHotkeys(){
        var core = selectedCore();
        if(core == null) return;

        if(activeCommand == CommandMode.RALLY){
            if(Core.input.keyTap(KeyCode.escape)){
                exitCommandMode();
            }
            return;
        }

        if(Core.input.keyTap(KeyCode.s)){
            queueCoreUnit(core);
            corePanel = CorePanel.BUILD;
        }else if(Core.input.keyTap(KeyCode.b)){
            showNotImplemented();
        }else if(Core.input.keyTap(KeyCode.p)){
            showNotImplemented();
        }else if(Core.input.keyTap(KeyCode.y)){
            enterCommandMode(CommandMode.RALLY);
        }else if(Core.input.keyTap(KeyCode.o)){
            showNotImplemented();
        }else if(Core.input.keyTap(KeyCode.l)){
            launchCore(core);
        }

        if(Core.input.keyTap(KeyCode.escape) && corePanel == CorePanel.BUILD){
            if(core.unitQueue != null && !core.unitQueue.isEmpty()){
                cancelCoreUnit(core);
            }else{
                corePanel = CorePanel.MAIN;
            }
        }
    }

    private void handleBuildingHotkeys(){
        if(control.input.commandBuildings.isEmpty()) return;
        Building build = control.input.commandBuildings.first();
        if(build instanceof UnitFactory.UnitFactoryBuild factory && factory.sc2QueueEnabled()){
            UnitFactory block = (UnitFactory)factory.block;
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

    private void launchCore(CoreBuild core){
        if(core == null) return;
        Call.coreLaunch(player, core.pos());
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
        if(control.input.selectedUnits.isEmpty()) return;
        for(Unit unit : control.input.selectedUnits){
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
        //Find the command description
        RTSCommand currentCmd = null;
        for(RTSCommand cmd : commands){
            if(cmd.mode == activeCommand){
                currentCmd = cmd;
                break;
            }
        }

        if(currentCmd == null){
            exitCommandMode();
            return;
        }

        //Display command description
        add(currentCmd.name).style(Styles.outlineLabel).color(Pal.accent).pad(4f).row();
        add(currentCmd.description).width(300f).wrap().pad(4f).row();
        add("Left-click to set target").color(Color.lightGray).pad(4f).row();
        add("Hold Shift to queue commands").color(Color.yellow).pad(4f).row();
        add("Right-click to cancel").color(Color.lightGray).pad(4f).row();

        //Add cancel button at row 3, column 5 (index 14)
        Table buttonGrid = new Table();
        for(int i = 0; i < ROWS * COLS; i++){
            if(i == 14){ //Row 3, Column 5 (0-indexed: row 2, col 4)
                addIconButton(buttonGrid, "Esc", Icon.cancel, () -> true, this::exitCommandMode);
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
        int[] ids = new int[control.input.selectedUnits.size];
        for(int i = 0; i < ids.length; i++){
            ids[i] = control.input.selectedUnits.get(i).id;
        }
        if(ids.length > 0){
            //Send stop command (move to current position)
            for(Unit unit : control.input.selectedUnits){
                if(unit.isValid()){
                    Call.commandUnits(player, new int[]{unit.id}, null, null, new Vec2(unit.x, unit.y), false, true);
                }
            }
        }
        exitCommandMode();
    }

    private void executeHoldCommand(){
        //Hold command executes immediately - units hold position
        for(Unit unit : control.input.selectedUnits){
            if(unit.isValid()){
                //Send hold command (move to current position, will be interpreted as hold)
                Call.commandUnits(player, new int[]{unit.id}, null, null, new Vec2(unit.x, unit.y), false, true);
            }
        }
        exitCommandMode();
    }
}
