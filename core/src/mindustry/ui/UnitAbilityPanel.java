package mindustry.ui;

import arc.*;
import arc.graphics.*;
import arc.input.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.event.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
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

import static mindustry.Vars.*;

public class UnitAbilityPanel extends Table{
    private static final int COLS = 5;
    private static final int ROWS = 3;

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
        String description;
        CommandMode mode;

        RTSCommand(String name, String key, String description, CommandMode mode){
            this.name = name;
            this.key = key;
            this.description = description;
            this.mode = mode;
        }
    }

    private static class BuildInfo{
        Block block;
        String key;
        String name;
        int crystalCost;
        int gasCost;
        int timeSeconds;
    }

    private RTSCommand[] commands = {
        new RTSCommand("Move", "m", "Commands the selected unit to move to a target area or follow a target unit. Units that are moving will not engage enemies.", CommandMode.MOVE),
        new RTSCommand("Stop", "s", "Commands the selected unit to stop executing any commands and halt movement.", CommandMode.STOP),
        new RTSCommand("Hold", "h", "Commands the selected unit to stay in place and attack enemy targets within range. Units receiving this command will not chase enemies or move toward them to engage.", CommandMode.HOLD),
        new RTSCommand("Patrol", "p", "Commands the selected unit to patrol between its current position and a target area. Patrolling units will attack enemies or move toward nearby enemies to engage.", CommandMode.PATROL),
        new RTSCommand("Attack", "a", "Commands the selected unit to move to a target location and attack enemies encountered along the way. After receiving an attack command on a target, the unit will continue attacking that target until it is destroyed.", CommandMode.ATTACK)
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
                button(b -> {
                    b.table(t -> {
                        t.add("[" + cmd.key + "]").style(Styles.outlineLabel).color(Color.yellow).row();
                        t.add(cmd.name).style(Styles.outlineLabel);
                    }).pad(4f);
                }, Styles.clearTogglei, () -> {
                    if(cmd.mode == CommandMode.STOP){
                        executeStopCommand();
                    }else if(cmd.mode == CommandMode.HOLD){
                        executeHoldCommand();
                    }else{
                        enterCommandMode(cmd.mode);
                    }
                }).size(64f).pad(2f);
            }
            row();
        }

        //Second row: Unit-specific abilities
        int col = 0;
        Seq<String> addedAbilities = new Seq<>();

        for(Unit unit : control.input.selectedUnits){
            if(unit.isValid()){
                if(unit.type.canBoost && !addedAbilities.contains("Boost")){
                    addAbilityButton("Boost", "Boost speed", () -> {});
                    addedAbilities.add("Boost");
                    col++;
                }
                if(unit instanceof Payloadc && !addedAbilities.contains("Pickup")){
                    addAbilityButton("Pickup", "Pick up units/blocks", () -> {});
                    addedAbilities.add("Pickup");
                    col++;
                }
                if(unit instanceof Payloadc && !addedAbilities.contains("Drop")){
                    addAbilityButton("Drop", "Drop payload", () -> {});
                    addedAbilities.add("Drop");
                    col++;
                }
                if(unit.type.mineTier >= 0 && !addedAbilities.contains("Mine")){
                    addAbilityButton("Mine", "Mine resources", () -> {});
                    addedAbilities.add("Mine");
                    col++;
                }
                //Remove build ability for air support units (poly, mega, quad, oct)
                if(unit.type.buildSpeed > 0 && !addedAbilities.contains("Build") &&
                   !unit.type.name.equals("poly") && !unit.type.name.equals("mega") &&
                   !unit.type.name.equals("quad") && !unit.type.name.equals("oct")){
                    addAbilityButton("Build", "Construct buildings", () -> {});
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
                add().size(64f).pad(2f);
                col++;
            }
            row();
        }

        //Third row: Additional abilities or empty
        //Fill third row with empty slots to maintain 3-row layout
        for(int i = 0; i < COLS; i++){
            add().size(64f).pad(2f);
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
            grid.button(b -> {
                b.table(t -> {
                    t.add("[" + cmd.key + "]").style(Styles.outlineLabel).color(Color.yellow).row();
                    t.add(cmd.name).style(Styles.outlineLabel);
                }).pad(4f);
            }, Styles.clearTogglei, () -> {
                if(cmd.mode == CommandMode.STOP){
                    executeStopCommand();
                }else if(cmd.mode == CommandMode.HOLD){
                    executeHoldCommand();
                }else{
                    enterCommandMode(cmd.mode);
                }
            }).size(64f).pad(2f);
        }
        grid.row();

        //Row 2
        addGridButton(grid, 1, 0, "g", Core.bundle.get("ability.mine", "Mine"), () -> enterCommandMode(CommandMode.HARVEST));
        fillRow(grid, 1, 1);
        grid.row();

        //Row 3
        addGridButton(grid, 2, 0, "b", Core.bundle.get("ability.build", "Build"), () -> novaPanel = NovaPanel.BUILD_BASIC);
        addGridButton(grid, 2, 1, "v", Core.bundle.get("ability.build.advanced", "Advanced"), () -> novaPanel = NovaPanel.BUILD_ADV);
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
        addBuildButton(grid, 0, 0, "c", Blocks.coreNucleus.localizedName, Blocks.coreNucleus, () -> startPlacement(Blocks.coreNucleus));
        addBuildButton(grid, 0, 1, "r", Blocks.ventCondenser.localizedName, Blocks.ventCondenser, () -> startPlacement(Blocks.ventCondenser));
        addBuildButton(grid, 0, 2, "s", Blocks.doorLarge.localizedName, Blocks.doorLarge, () -> startPlacement(Blocks.doorLarge));
        fillRow(grid, 0, 3);
        grid.row();

        //Row 2
        addBuildButton(grid, 1, 0, "b", Blocks.groundFactory.localizedName, Blocks.groundFactory, () -> startPlacement(Blocks.groundFactory));
        addBuildButton(grid, 1, 1, "e", Blocks.multiPress.localizedName, Blocks.multiPress, () -> startPlacement(Blocks.multiPress));
        fillRow(grid, 1, 2);
        grid.row();

        //Row 3
        addBuildButton(grid, 2, 0, "u", Blocks.atmosphericConcentrator.localizedName, Blocks.atmosphericConcentrator, () -> startPlacement(Blocks.atmosphericConcentrator));
        addBuildButton(grid, 2, 1, "t", Blocks.swarmer.localizedName, Blocks.swarmer, () -> startPlacement(Blocks.swarmer));
        addBuildButton(grid, 2, 2, "n", Blocks.hail.localizedName, Blocks.hail, () -> startPlacement(Blocks.hail));
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
        addBuildButton(grid, 0, 0, "g", Blocks.launchPad.localizedName, Blocks.launchPad, () -> startPlacement(Blocks.launchPad));
        fillRow(grid, 0, 1);
        grid.row();

        //Row 2
        addBuildButton(grid, 1, 0, "f", Blocks.tankFabricator.localizedName, Blocks.tankFabricator, () -> startPlacement(Blocks.tankFabricator));
        addBuildButton(grid, 1, 1, "a", Blocks.siliconCrucible.localizedName, Blocks.siliconCrucible, () -> startPlacement(Blocks.siliconCrucible));
        fillRow(grid, 1, 2);
        grid.row();

        //Row 3
        addBuildButton(grid, 2, 0, "s", Blocks.shipFabricator.localizedName, Blocks.shipFabricator, () -> startPlacement(Blocks.shipFabricator));
        addBuildButton(grid, 2, 1, "c", Blocks.surgeCrucible.localizedName, Blocks.surgeCrucible, () -> startPlacement(Blocks.surgeCrucible));
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
                        grid.button(b -> {
                            b.table(t -> {
                                t.add("[q]").style(Styles.outlineLabel).color(Color.yellow).row();
                                t.add("Select").style(Styles.outlineLabel);
                            }).pad(4f);
                        }, Styles.clearTogglei, () -> selectBuilder(builder)).size(64f).pad(2f);
                    }else{
                        grid.add().size(64f).pad(2f);
                    }
                }else if(r == 2 && c == 3 && incomplete){
                    Unit builder = findActiveBuilder(cons);
                    if(builder != null){
                        grid.button(b -> {
                            b.table(t -> {
                                t.add("[t]").style(Styles.outlineLabel).color(Color.yellow).row();
                                t.add("Pause").style(Styles.outlineLabel);
                            }).pad(4f);
                        }, Styles.clearTogglei, () -> pauseBuilder(builder)).size(64f).pad(2f);
                    }else{
                        grid.add().size(64f).pad(2f);
                    }
                }else if(r == 2 && c == 4 && incomplete){
                    grid.button(b -> {
                        b.table(t -> {
                            t.add("[Esc]").style(Styles.outlineLabel).color(Color.yellow).row();
                            t.add("Cancel").style(Styles.outlineLabel);
                        }).pad(4f);
                    }, Styles.clearTogglei, () -> cancelConstruct(cons)).size(64f).pad(2f);
                }else{
                    grid.add().size(64f).pad(2f);
                }
            }
            grid.row();
        }

        add(grid);
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
        addGridButton(grid, 0, 0, "s", "SCV", () -> {
            queueCoreUnit(core);
            corePanel = CorePanel.BUILD;
        });
        addEmpty(grid);
        addEmpty(grid);
        addGridButton(grid, 0, 3, "b", "Starport", this::showNotImplemented);
        addGridButton(grid, 0, 4, "p", "Planetary Fortress", this::showNotImplemented);
        grid.row();

        //Row 2
        addEmpty(grid);
        addEmpty(grid);
        addEmpty(grid);
        addEmpty(grid);
        addGridButton(grid, 1, 4, "y", "Rally", () -> enterCommandMode(CommandMode.RALLY));
        grid.row();

        //Row 3
        addGridButton(grid, 2, 0, "o", "Load", this::showNotImplemented);
        addEmpty(grid);
        addEmpty(grid);
        addGridButton(grid, 2, 3, "l", "Launch", () -> launchCore(core));
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

    private void addAbilityButton(String name, String desc, Runnable action){
        button(b -> {
            b.add(name).style(Styles.outlineLabel);
        }, Styles.clearTogglei, action).size(64f).pad(2f);
    }

    private void addGridButton(Table grid, int row, int col, String key, String label, Runnable action){
        grid.button(b -> {
            b.table(t -> {
                t.add("[" + key + "]").style(Styles.outlineLabel).color(Color.yellow).row();
                t.add(label).style(Styles.outlineLabel);
            }).pad(4f);
        }, Styles.clearTogglei, action).size(64f).pad(2f);
    }

    private void addBuildButton(Table grid, int row, int col, String key, String label, Block block, Runnable action){
        var button = grid.button(b -> {
            b.table(t -> {
                t.add("[" + key + "]").style(Styles.outlineLabel).color(Color.yellow).row();
                t.add(label).style(Styles.outlineLabel);
            }).pad(4f);
        }, Styles.clearTogglei, action).size(64f).pad(2f).get();
        BuildInfo info = makeBuildInfo(block, key);
        button.update(() -> {
            if(button.isOver()){
                hoverBuildInfo = info;
            }else if(hoverBuildInfo == info){
                hoverBuildInfo = null;
            }
        });
    }

    private Table buildBuildInfoTable(){
        Table info = new Table();
        info.background(Styles.black6);
        info.visible(() -> hoverBuildInfo != null);
        info.update(() -> info.touchable = hoverBuildInfo != null ? Touchable.enabled : Touchable.disabled);
        info.defaults().pad(2f).left();

        info.label(() -> {
            if(hoverBuildInfo == null) return "";
            return "建造" + hoverBuildInfo.name + " (" + hoverBuildInfo.key + ")";
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
            return "时间 " + hoverBuildInfo.timeSeconds + "s";
        }).left();

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

    private void addEmpty(Table grid){
        grid.add().size(64f).pad(2f);
    }

    private void addEscButton(Table grid, Runnable action){
        grid.button(b -> {
            b.table(t -> {
                t.add("[Esc]").style(Styles.outlineLabel).color(Color.yellow).row();
                t.add("Back").style(Styles.outlineLabel);
            }).pad(4f);
        }, Styles.clearTogglei, action).size(64f).pad(2f);
    }

    private void addStopBuildButton(Table grid){
        grid.button(b -> {
            b.table(t -> {
                t.add("[t]").style(Styles.outlineLabel).color(Color.yellow).row();
                t.add("Stop").style(Styles.outlineLabel);
            }).pad(4f);
        }, Styles.clearTogglei, this::stopSelectedBuilders).size(64f).pad(2f);
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
            if(core.unitQueue != null && core.unitQueue.size >= CoreBlock.maxUnitQueue){
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
                buttonGrid.button("[Esc]", Styles.cleart, () -> exitCommandMode()).size(64f).pad(2f);
            }else{
                buttonGrid.add().size(64f).pad(2f);
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
