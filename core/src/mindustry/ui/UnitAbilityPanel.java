package mindustry.ui;

import arc.*;
import arc.graphics.*;
import arc.input.*;
import arc.math.geom.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.input.*;

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
        ATTACK
    }

    public CommandMode activeCommand = CommandMode.NONE;
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
            //Check for command hotkeys
            if(control.input.selectedUnits.size > 0 || control.input.commandBuildings.size > 0){
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

                //Cancel command mode with Esc or right-click
                if(activeCommand != CommandMode.NONE){
                    if(Core.input.keyTap(KeyCode.escape) || Core.input.keyTap(KeyCode.mouseRight)){
                        exitCommandMode();
                    }
                }
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

        if(activeCommand != CommandMode.NONE){
            buildCommandModePanel();
        }else{
            buildMainPanel();
        }
    }

    private void buildMainPanel(){
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

    private void addAbilityButton(String name, String desc, Runnable action){
        button(b -> {
            b.add(name).style(Styles.outlineLabel);
        }, Styles.clearTogglei, action).size(64f).pad(2f);
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
