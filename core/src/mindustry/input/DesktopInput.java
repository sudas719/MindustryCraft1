package mindustry.input;

import arc.*;
import arc.Graphics.*;
import arc.Graphics.Cursor.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.input.*;
import arc.input.KeyCode.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.ai.*;
import mindustry.ai.types.*;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.entities.units.*;
import mindustry.game.EventType.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.defense.*;
import mindustry.world.blocks.environment.*;
import mindustry.world.blocks.payloads.*;
import mindustry.world.blocks.storage.*;

import static arc.Core.*;
import static mindustry.Vars.*;
import static mindustry.input.PlaceMode.*;

public class DesktopInput extends InputHandler{
    public Vec2 movement = new Vec2();
    /** Current cursor type. */
    public Cursor cursorType = SystemCursor.arrow;
    /** Position where the player started dragging a line. */
    public int selectX = -1, selectY = -1, schemX = -1, schemY = -1;
    /** Last known line positions.*/
    public int lastLineX, lastLineY, schematicX, schematicY;
    /** Whether selecting mode is active. */
    public PlaceMode mode;
    /** Animation scale for line. */
    public float selectScale;
    /** Selected build plan for movement. */
    public @Nullable BuildPlan splan;
    /** Landing placement ghost for flying cores. */
    public @Nullable BuildPlan landConfirmPlan;
    public int landConfirmUnitId = -1;
    /** Whether player is currently deleting removal plans. */
    public boolean deleting = false, shouldShoot = false, panning = false, movedPlan = false;
    /** Mouse pan speed. */
    public float panScale = 0.005f, panSpeed = 4.5f, panBoostSpeed = 15f;
    /** Edge scrolling state */
    public boolean edgeScrolling = false;
    public float edgeScrollX = 0f, edgeScrollY = 0f;
    /** Delta time between consecutive clicks. */
    public long selectMillis = 0;
    /** Previously selected tile. */
    public Tile prevSelected;
    /** Unit selection long press tracking */
    public long unitSelectPressTime = 0;
    public static final long UNIT_SELECT_LONG_PRESS_MS = 300;

    /** Most recently selected control group by index */
    public int lastCtrlGroup;
    /** Time of most recent control group selection */
    public long lastCtrlGroupSelectMillis;

    /** Time of most recent payload pickup/drop key press*/
    public long lastPayloadKeyTapMillis;
    /** Time of most recent payload pickup/drop key hold*/
    public long lastPayloadKeyHoldMillis;

    /** View presets: camera positions for F1-F4 */
    public Vec2[] viewPresets = new Vec2[4];

    private int lastOrbitalCoreId = -1;

    /** Shift key command queuing */
    private boolean shiftWasPressed = false;
    private Seq<Vec2> queuedCommandTargets = new Seq<>();
    private mindustry.ui.UnitAbilityPanel.CommandMode queuedCommandMode = mindustry.ui.UnitAbilityPanel.CommandMode.NONE;
    /** Guards against ghost clicks immediately after refocus/large frame hitch. */
    private float commandFocusGuardTime = 0f;
    /** Timestamp of the latest left-click consumed by ability targeting. */
    private long abilityTargetConsumeMillis = -1L;

    private float buildPlanMouseOffsetX, buildPlanMouseOffsetY;
    private boolean changedCursor, pressedCommandRect;

    private boolean abilityTargetingActive(){
        return ui.hudfrag.abilityPanel != null && ui.hudfrag.abilityPanel.activeCommand != mindustry.ui.UnitAbilityPanel.CommandMode.NONE;
    }

    private boolean suppressSelectionTap(){
        return abilityTargetConsumeMillis > 0L && Time.timeSinceMillis(abilityTargetConsumeMillis) <= 200L;
    }

    boolean showHint(){
        return ui.hudfrag.shown && Core.settings.getBool("hints") && selectPlans.isEmpty() && !player.dead() &&
            (!isBuilding && !Core.settings.getBool("buildautopause") || player.unit().isBuilding() || !player.dead() && !player.unit().spawnedByCore());
    }

    @Override
    public void reset(){
        super.reset();
        shouldShoot = false;
        deleting = false;
    }

    @Override
    public void buildUI(Group group){
        //building and respawn hints
        group.fill(t -> {
            t.color.a = 0f;
            t.visible(() -> (t.color.a = Mathf.lerpDelta(t.color.a, Mathf.num(showHint()), 0.15f)) > 0.001f);
            t.bottom();
            t.table(Styles.black6, b -> {
                StringBuilder str = new StringBuilder();
                b.defaults().left();
                b.label(() -> {
                    if(!showHint()) return str;
                    str.setLength(0);
                    if(!isBuilding && !Core.settings.getBool("buildautopause") && !player.unit().isBuilding()){
                        str.append(Core.bundle.format("enablebuilding", Binding.pauseBuilding.value.key.toString()));
                    }else if(player.unit().isBuilding()){
                        str.append(Core.bundle.format(isBuilding ? "pausebuilding" : "resumebuilding", Binding.pauseBuilding.value.key.toString()))
                            .append("\n").append(Core.bundle.format("cancelbuilding", Binding.clearBuilding.value.key.toString()))
                            .append("\n").append(Core.bundle.format("selectschematic", Binding.schematicSelect.value.key.toString()));
                    }
                    if(!player.dead() && !player.unit().spawnedByCore()){
                        str.append(str.length() != 0 ? "\n" : "").append(Core.bundle.format("respawn", Binding.respawn.value.key.toString()));
                    }
                    return str;
                }).style(Styles.outlineLabel);
            }).margin(10f);
        });

        //schematic controls
        group.fill(t -> {
            t.visible(() -> ui.hudfrag.shown && lastSchematic != null && !selectPlans.isEmpty());
            t.bottom();
            t.table(Styles.black6, b -> {
                b.defaults().left();
                b.label(() -> Core.bundle.format("schematic.flip",
                    Binding.schematicFlipX.value.key.toString(),
                    Binding.schematicFlipY.value.key.toString())).style(Styles.outlineLabel).visible(() -> Core.settings.getBool("hints"));
                b.row();
                b.table(a -> {
                    a.button("@schematic.add", Icon.save, this::showSchematicSave).colspan(2).size(250f, 50f).disabled(f -> lastSchematic == null || lastSchematic.file != null);
                });
            }).margin(6f);
        });
    }

    @Override
    public void drawTop(){
        if(cursorType != SystemCursor.arrow && scene.hasMouse()){
           graphics.cursor(cursorType = SystemCursor.arrow);
        }

        Lines.stroke(1f);
        int cursorX = tileX(Core.input.mouseX());
        int cursorY = tileY(Core.input.mouseY());

        //draw break selection
        if(mode == breaking){
            drawBreakSelection(selectX, selectY, cursorX, cursorY, !(Core.input.keyDown(Binding.schematicSelect) && schemX != -1 && schemY != -1) ? maxLength : Vars.maxSchematicSize, false);
        }

        if(!Core.scene.hasKeyboard() && mode != breaking){

            if(Core.input.keyDown(Binding.schematicSelect) && schemX != -1 && schemY != -1){
                drawSelection(schemX, schemY, cursorX, cursorY, Vars.maxSchematicSize);
            }else if(Core.input.keyDown(Binding.rebuildSelect)){
                drawRebuildSelection(schemX, schemY, cursorX, cursorY);
            }
        }

        if(ui.hudfrag.abilityPanel != null && ui.hudfrag.abilityPanel.activeCommand == mindustry.ui.UnitAbilityPanel.CommandMode.LIBERATOR_ZONE){
            float wx = clampCommandX(Core.input.mouseWorldX());
            float wy = clampCommandY(Core.input.mouseWorldY());
            Draw.z(Layer.effect);
            Lines.stroke(1.5f, Pal.remove);
            Lines.circle(wx, wy, UnitTypes.liberatorZoneRadius());
            Draw.reset();
        }

        super.drawTop();
        Draw.reset();
    }

    @Override
    public void drawBottom(){
        float cursorAlpha = 0.5f;
        int cursorX = tileX(Core.input.mouseX());
        int cursorY = tileY(Core.input.mouseY());
        if(isPlacing() && block == Blocks.ventCondenser){
            Tile snap = findNearestVentCenter(Core.input.mouseWorldX(), Core.input.mouseWorldY(), 30);
            if(snap != null){
                cursorX = snap.x;
                cursorY = snap.y;
            }
        }

        //draw plan being moved
        if(splan != null){
            boolean valid = validPlace(splan.x, splan.y, splan.block, splan.rotation, splan);
            if(splan.block.rotate && splan.block.drawArrow){
                drawArrow(splan.block, splan.x, splan.y, splan.rotation, valid);
            }

            splan.block.drawPlan(splan, allPlans(), valid);

            drawSelected(splan.x, splan.y, splan.block, getPlan(splan.x, splan.y, splan.block.size, splan) != null ? Pal.remove : Pal.accent);
        }

        if(landConfirmPlan != null){
            drawPlan(landConfirmPlan);
            drawOverPlan(landConfirmPlan, landConfirmPlan.cachedValid);
        }

        //draw hover plans
        if(mode == none && !isPlacing()){
            var plan = getPlan(cursorX, cursorY);
            if(plan != null){
                drawSelected(plan.x, plan.y, plan.breaking ? plan.tile().block() : plan.block, Pal.accent);
            }
        }

        var items = selectPlans.items;
        int size = selectPlans.size;

        //draw schematic plans
        for(int i = 0; i < size; i++){
            var plan = items[i];
            plan.animScale = 1f;
            drawPlan(plan);
        }

        //draw schematic plans - over version, cached results
        for(int i = 0; i < size; i++){
            var plan = items[i];
            //use cached value from previous invocation
            drawOverPlan(plan, plan.cachedValid);
        }

        //draw things that may be placed soon
            if(ui.hudfrag.abilityPanel != null && ui.hudfrag.abilityPanel.activeCommand == mindustry.ui.UnitAbilityPanel.CommandMode.LAND){
                BuildPayload payload = selectedCoreFlyerPayload();
                if(payload != null){
                    Block landBlock = payload.build.block;
                    int rot = landBlock.planRotation(payload.build.rotation);
                    float offset = landBlock.offset;
                    int placeX = World.toTile(Core.input.mouseWorldX() - offset);
                    int placeY = World.toTile(Core.input.mouseWorldY() - offset);
                    boolean valid = Build.validPlace(landBlock, player.team(), placeX, placeY, rot, false);
                    if(landBlock.rotate && landBlock.drawArrow){
                        drawArrow(landBlock, placeX, placeY, rot, valid);
                    }
                    drawPlacementConstraintGrid(landBlock, player.team(), placeX, placeY, rot);
                    Draw.color();
                    drawPlan(placeX, placeY, landBlock, rot, cursorAlpha);
                    landBlock.drawPlace(placeX, placeY, rot, valid);
                    drawOverlapCheck(landBlock, placeX, placeY, valid);
                }
            }
            if(mode == placing && block != null){
                for(int i = 0; i < linePlans.size; i++){
                    var plan = linePlans.get(i);
                    if(i == linePlans.size - 1 && plan.block.rotate && plan.block.drawArrow){
                        drawArrow(block, plan.x, plan.y, plan.rotation);
                    }
                    boolean valid = validPlace(plan.x, plan.y, plan.block, plan.rotation);
                    plan.cachedValid = valid;
                    plan.block.drawPlan(plan, allPlans(), valid, cursorAlpha);
                }
                for(int i = 0; i < linePlans.size; i++){
                    var plan = linePlans.get(i);
                    drawOverPlan(plan, plan.cachedValid, cursorAlpha);
                }
            }else if(isPlacing()){
                int rot = block == null ? rotation : block.planRotation(rotation);
                int placeX = cursorX;
                int placeY = cursorY;
                if(block == Blocks.ventCondenser){
                    Tile snap = findNearestVentCenter(Core.input.mouseWorldX(), Core.input.mouseWorldY(), 30);
                    if(snap != null){
                        placeX = snap.x;
                        placeY = snap.y;
                    }
                }
                if(block.rotate && block.drawArrow){
                    drawArrow(block, placeX, placeY, rot);
                }
                Draw.color();
                boolean valid = validPlace(placeX, placeY, block, rot);
                drawPlan(placeX, placeY, block, rot, cursorAlpha);
                block.drawPlace(placeX, placeY, rot, valid);

                if(block.saveConfig){
                    Draw.mixcol(!valid ? Pal.breakInvalid : Color.white, (!valid ? 0.4f : 0.24f) + Mathf.absin(Time.globalTime, 6f, 0.28f));
                    Draw.alpha(cursorAlpha);
                    bplan.set(placeX, placeY, rot, block);
                    bplan.config = block.lastConfig;
                    block.drawPlanConfig(bplan, allPlans());
                bplan.config = null;
                Draw.reset();
            }

            drawOverlapCheck(block, placeX, placeY, valid);
        }

        Draw.reset();
    }

    @Override
    public void update(){
        super.update();

        float frameDelta = Core.graphics.getDeltaTime();
        if(frameDelta > 0.2f){
            commandFocusGuardTime = Math.max(commandFocusGuardTime, 0.35f);
        }else if(commandFocusGuardTime > 0f){
            commandFocusGuardTime = Math.max(commandFocusGuardTime - frameDelta, 0f);
        }

        if(landConfirmUnitId != -1){
            Unit unit = Groups.unit.getByID(landConfirmUnitId);
            if(unit == null || !unit.isValid() || unit.type != UnitTypes.coreFlyer){
                landConfirmUnitId = -1;
                landConfirmPlan = null;
            }
        }

        //Legacy queued command buffer is no longer executed on Shift release.
        //Queued waypoints are applied immediately on each Shift+click.
        boolean shiftPressed = Core.input.keyDown(KeyCode.shiftLeft) || Core.input.keyDown(KeyCode.shiftRight);
        if(shiftWasPressed && !shiftPressed && !queuedCommandTargets.isEmpty()){
            queuedCommandTargets.clear();
            queuedCommandMode = mindustry.ui.UnitAbilityPanel.CommandMode.NONE;
        }
        shiftWasPressed = shiftPressed;

        if(net.active() && Core.input.keyTap(Binding.playerList) && (scene.getKeyboardFocus() == null || scene.getKeyboardFocus().isDescendantOf(ui.listfrag.content) || scene.getKeyboardFocus().isDescendantOf(ui.minimapfrag.elem))){
            ui.listfrag.toggle();
        }

        boolean locked = locked();
        boolean panCam = false;
        float camSpeed = (!Core.input.keyDown(Binding.boost) ? panSpeed : panBoostSpeed) * Time.delta;
        boolean detached = settings.getBool("detach-camera", false);
        float arrowCamX = 0f, arrowCamY = 0f;
        boolean arrowCam = false;

        if(!scene.hasField() && !scene.hasDialog()){
            if(input.keyTap(Binding.debugHitboxes)){
                drawDebugHitboxes = !drawDebugHitboxes;
            }

            if(input.keyTap(Binding.detachCamera)){
                settings.put("detach-camera", detached = !detached);
                if(!detached){
                    panning = false;
                }
                spectating = null;
            }

            if(input.keyDown(Binding.pan)){
                panCam = true;
                panning = true;
                spectating = null;
            }

            if((Math.abs(Core.input.axis(Binding.moveX)) > 0 || Math.abs(Core.input.axis(Binding.moveY)) > 0 || input.keyDown(Binding.mouseMove))){
                panning = false;
                spectating = null;
            }

            if(!ui.chatfrag.shown()){
                if(Core.input.keyDown(KeyCode.left)) arrowCamX -= 1f;
                if(Core.input.keyDown(KeyCode.right)) arrowCamX += 1f;
                if(Core.input.keyDown(KeyCode.up)) arrowCamY += 1f;
                if(Core.input.keyDown(KeyCode.down)) arrowCamY -= 1f;
                arrowCam = arrowCamX != 0f || arrowCamY != 0f;
                if(arrowCam){
                    panning = true;
                    spectating = null;
                }
            }
        }

        panning |= detached;


        if(!locked){
            if(((player.dead() || state.isPaused() || detached) && !ui.chatfrag.shown()) && !scene.hasField() && !scene.hasDialog()){
                if(input.keyDown(Binding.mouseMove)){
                    panCam = true;
                }

                Core.camera.position.add(Tmp.v1.setZero().add(Core.input.axis(Binding.moveX), Core.input.axis(Binding.moveY)).nor().scl(camSpeed));
            }

            if(arrowCam && !scene.hasField() && !scene.hasDialog() && !ui.chatfrag.shown()){
                Core.camera.position.add(Tmp.v1.set(arrowCamX, arrowCamY).nor().scl(camSpeed));
            }else if((!player.dead() || spectating != null) && !panning){
                //TODO do not pan
                Team corePanTeam = state.won ? state.rules.waveTeam : player.team();
                Position coreTarget = state.gameOver && !state.rules.pvp && corePanTeam.data().lastCore != null ? corePanTeam.data().lastCore : null;
                Position panTarget = coreTarget != null ? coreTarget : spectating != null ? spectating : player;

                Core.camera.position.lerpDelta(panTarget, Core.settings.getBool("smoothcamera") ? 0.08f : 1f);
            }

            if(panCam){
                Core.camera.position.x += Mathf.clamp((Core.input.mouseX() - Core.graphics.getWidth() / 2f) * panScale, -1, 1) * camSpeed;
                Core.camera.position.y += Mathf.clamp((Core.input.mouseY() - Core.graphics.getHeight() / 2f) * panScale, -1, 1) * camSpeed;
            }

            //edge scrolling
            if(Core.settings.getBool("edgescrolling") && !scene.hasDialog() && !scene.hasField()){
                float edgeDist = Core.settings.getInt("edgescrolldistance", 20);
                float edgeSpeed = Core.settings.getInt("edgescrollspeed", 10) * Time.delta;

                float mouseX = Core.input.mouseX();
                float mouseY = Core.input.mouseY();
                float screenWidth = Core.graphics.getWidth();
                float screenHeight = Core.graphics.getHeight();

                edgeScrollX = 0f;
                edgeScrollY = 0f;

                //check if mouse near edges
                if(mouseX < edgeDist){
                    edgeScrollX = -edgeSpeed * (1f - mouseX / edgeDist);
                }else if(mouseX > screenWidth - edgeDist){
                    edgeScrollX = edgeSpeed * ((mouseX - (screenWidth - edgeDist)) / edgeDist);
                }

                if(mouseY < edgeDist){
                    edgeScrollY = -edgeSpeed * (1f - mouseY / edgeDist);
                }else if(mouseY > screenHeight - edgeDist){
                    edgeScrollY = edgeSpeed * ((mouseY - (screenHeight - edgeDist)) / edgeDist);
                }

                //apply camera movement
                if(edgeScrollX != 0f || edgeScrollY != 0f){
                    Core.camera.position.add(edgeScrollX, edgeScrollY);
                    edgeScrolling = true;
                }else{
                    edgeScrolling = false;
                }
            }
        }

        shouldShoot = !scene.hasMouse() && !locked && !state.isEditor();

        //Command mode is always enabled - no toggle needed
        commandMode = true;

        //validate commanding units
        selectedUnits.removeAll(u -> !u.allowCommand() || !u.isValid() || u.team != player.team());

        if(commandMode && !scene.hasField() && !scene.hasDialog()){
            if(input.keyTap(Binding.selectAllUnits)){
                selectedUnits.clear();
                commandBuildings.clear();
                if(input.keyDown(Binding.selectAcrossScreen)){
                    camera.bounds(Tmp.r1);
                    selectedUnits.set(selectedCommandUnits(Tmp.r1.x, Tmp.r1.y, Tmp.r1.width, Tmp.r1.height, u -> u.type.controlSelectGlobal && u.type != UnitTypes.nova && u.type != UnitTypes.pulsar));
                }else {
                    for(var unit : player.team().data().units){
                        if(unit.isCommandable() && unit.type.controlSelectGlobal && unit.type != UnitTypes.nova && unit.type != UnitTypes.pulsar){
                            selectedUnits.add(unit);
                        }
                    }
                }
            }

            if(input.keyTap(Binding.selectIdleWorkers)){
                selectedUnits.clear();
                commandBuildings.clear();
                if(input.keyDown(Binding.selectAcrossScreen)){
                    camera.bounds(Tmp.r1);
                    selectedUnits.set(selectedCommandUnits(Tmp.r1.x, Tmp.r1.y, Tmp.r1.width, Tmp.r1.height, this::isIdleWorker));
                }else{
                    for(var unit : player.team().data().units){
                        if(isIdleWorker(unit)){
                            selectedUnits.add(unit);
                        }
                    }
                }
            }

            if(input.keyTap(Binding.selectAllUnitTransport)){
                selectedUnits.clear();
                commandBuildings.clear();
                if(input.keyDown(Binding.selectAcrossScreen)){
                    camera.bounds(Tmp.r1);
                    selectedUnits.set(selectedCommandUnits(Tmp.r1.x, Tmp.r1.y, Tmp.r1.width, Tmp.r1.height, u -> u instanceof Payloadc));
                }else {
                    for(var unit : player.team().data().units){
                        if(unit.isCommandable() && unit instanceof Payloadc){
                            selectedUnits.add(unit);
                        }
                    }
                }
            }

            if(input.keyTap(Binding.selectAllUnitFactories)){
                selectedUnits.clear();
                commandBuildings.clear();
                for(var build : player.team().data().buildings){
                    if(build.isCommandable()){
                        commandBuildings.add(build);
                    }
                }
                if(input.keyDown(Binding.selectAcrossScreen)){
                    camera.bounds(Tmp.r1);
                    commandBuildings.retainAll(b -> Tmp.r1.overlaps(b.x - (b.hitSize() /2), b.y - (b.hitSize() /2), b.hitSize(), b.hitSize()));
                }
            }

            for(int i = 0; i < controlGroupBindings.length; i++){
                if(input.keyTap(controlGroupBindings[i])){

                    //create control group if it doesn't exist yet
                    if(controlGroups[i] == null) controlGroups[i] = new IntSeq();

                    IntSeq group = controlGroups[i];
                    boolean creating = input.keyDown(Binding.createControlGroup);
                    boolean adding = Core.input.keyDown(KeyCode.shiftLeft) || Core.input.keyDown(KeyCode.shiftRight);

                    //clear existing if making a new control group
                    //if any of the control group edit buttons are pressed take the current selection
                    if(creating){
                        group.clear();

                        IntSeq selectedUnitIds = selectedUnits.mapInt(u -> u.id);
                        IntSeq selectedBuildingIds = commandBuildings.mapInt(b -> b.id);
                        if(Core.settings.getBool("distinctcontrolgroups", true)){
                            for(IntSeq cg : controlGroups){
                                if(cg != null){
                                    cg.removeAll(selectedUnitIds);
                                    cg.removeAll(selectedBuildingIds);
                                }
                            }
                        }
                        group.addAll(selectedUnitIds);
                        group.addAll(selectedBuildingIds);
                    }else if(adding){
                        //Shift+number: Add currently selected units to this formation
                        IntSeq selectedUnitIds = selectedUnits.mapInt(u -> u.id);
                        IntSeq selectedBuildingIds = commandBuildings.mapInt(b -> b.id);

                        if(Core.settings.getBool("distinctcontrolgroups", true)){
                            for(IntSeq cg : controlGroups){
                                if(cg != null && cg != group){
                                    cg.removeAll(selectedUnitIds);
                                    cg.removeAll(selectedBuildingIds);
                                }
                            }
                        }

                        group.addAll(selectedUnitIds);
                        group.addAll(selectedBuildingIds);
                    }

                    //remove invalid units and buildings
                    for(int j = 0; j < group.size; j++){
                        int id = group.get(j);
                        Unit u = Groups.unit.getByID(id);
                        Building b = null;

                        //Buildings don't have ID mapping, search manually
                        if(u == null){
                            for(Building building : Groups.build){
                                if(building.id == id){
                                    b = building;
                                    break;
                                }
                            }
                        }

                        if((u == null || !u.isCommandable() || !u.isValid()) && (b == null || !b.isCommandable() || !b.isValid())){
                            group.removeIndex(j);
                            j --;
                        }
                    }

                    //replace the selected units/buildings with the current control group
                    if(!group.isEmpty() && !creating && !adding){
                        selectedUnits.clear();
                        commandBuildings.clear();

                        group.each(id -> {
                            var unit = Groups.unit.getByID(id);
                            Building building = null;

                            //Buildings don't have ID mapping, search manually
                            if(unit == null){
                                for(Building b : Groups.build){
                                    if(b.id == id){
                                        building = b;
                                        break;
                                    }
                                }
                            }

                            if(unit != null){
                                selectedUnits.addAll(unit);
                            }else if(building != null){
                                commandBuildings.add(building);
                            }
                        });

                        //double tap to center camera
                        if(lastCtrlGroup == i && Time.timeSinceMillis(lastCtrlGroupSelectMillis) < 400){
                            float totalX = 0, totalY = 0;
                            int count = 0;
                            for(Unit unit : selectedUnits){
                                totalX += unit.x;
                                totalY += unit.y;
                                count++;
                            }
                            for(Building building : commandBuildings){
                                totalX += building.x;
                                totalY += building.y;
                                count++;
                            }
                            if(count > 0){
                                panning = true;
                                Core.camera.position.set(totalX / count, totalY / count);
                            }
                        }
                        lastCtrlGroup = i;
                        lastCtrlGroupSelectMillis = Time.millis();
                    }
                }
            }

            //grid command keybindings (StarCraft II style)
            KeyBind[] gridKeys = {
                Binding.commandGrid01, Binding.commandGrid02, Binding.commandGrid03,
                Binding.commandGrid04, Binding.commandGrid05,
                Binding.commandGrid06, Binding.commandGrid07, Binding.commandGrid08,
                Binding.commandGrid09, Binding.commandGrid10,
                Binding.commandGrid11, Binding.commandGrid12, Binding.commandGrid13,
                Binding.commandGrid14, Binding.commandGrid15
            };

            for(int i = 0; i < gridKeys.length; i++){
                if(input.keyTap(gridKeys[i])){
                    //TODO: Map grid position to available commands
                    //This would require accessing the command list from PlacementFragment
                    //For now, this is a placeholder for the keybinding system
                }
            }
        }

        //View presets: bindable save + jump
        KeyBind[] viewPresetKeys = {Binding.viewPreset1, Binding.viewPreset2, Binding.viewPreset3, Binding.viewPreset4};
        KeyBind[] viewPresetSetKeys = {Binding.viewPresetSet1, Binding.viewPresetSet2, Binding.viewPresetSet3, Binding.viewPresetSet4};
        for(int i = 0; i < viewPresetKeys.length; i++){
            boolean ctrlDown = Core.input.keyDown(KeyCode.controlLeft) || Core.input.keyDown(KeyCode.controlRight);
            if(input.keyTap(viewPresetSetKeys[i]) || (ctrlDown && input.keyTap(viewPresetKeys[i]))){
                viewPresets[i] = new Vec2(Core.camera.position.x, Core.camera.position.y);
            }else if(input.keyTap(viewPresetKeys[i]) && viewPresets[i] != null){
                Core.camera.position.set(viewPresets[i]);
            }
        }

        //Possession is completely disabled - players cannot control units directly
        /*
        if(!scene.hasMouse() && !locked && state.rules.possessionAllowed){
            //Original Ctrl+Click selection still works
            if(Core.input.keyDown(Binding.control) && Core.input.keyTap(Binding.select)){
                Unit on = selectedUnit();
                var build = selectedControlBuild();
                if(on != null){
                    Call.unitControl(player, on);
                    shouldShoot = false;
                    recentRespawnTimer = 1f;
                }else if(build != null){
                    Call.buildingControlSelect(player, build);
                    recentRespawnTimer = 1f;
                }
            }

            //New left-click selection: direct in single-player, long press in multiplayer
            if(!Core.input.keyDown(Binding.control)){
                if(Core.input.keyDown(Binding.select)){
                    if(unitSelectPressTime == 0){
                        unitSelectPressTime = Time.millis();
                    }

                    //In single-player, select immediately; in multiplayer, require long press
                    boolean shouldSelect = !net.active() || Time.timeSinceMillis(unitSelectPressTime) >= UNIT_SELECT_LONG_PRESS_MS;

                    if(shouldSelect && Time.timeSinceMillis(unitSelectPressTime) >= (net.active() ? UNIT_SELECT_LONG_PRESS_MS : 0)){
                        Unit on = selectedUnit();
                        var build = selectedControlBuild();
                        if(on != null){
                            Call.unitControl(player, on);
                            shouldShoot = false;
                            recentRespawnTimer = 1f;
                            unitSelectPressTime = -1; //Mark as consumed
                        }else if(build != null && on == null){
                            //Only select building if no unit is present
                            Call.buildingControlSelect(player, build);
                            recentRespawnTimer = 1f;
                            unitSelectPressTime = -1; //Mark as consumed
                        }
                    }
                }else{
                    unitSelectPressTime = 0;
                }
            }
        }
        */

        if(!player.dead() && !state.isPaused() && !scene.hasField() && !locked){
            updateMovement(player.unit());

            if(Core.input.keyTap(Binding.respawn)){
                controlledType = null;
                recentRespawnTimer = 1f;
                Call.unitClear(player);
            }
        }

        if(state.isGame() && !scene.hasDialog() && !scene.hasField()){
            if(Core.input.keyTap(Binding.minimap)) ui.minimapfrag.toggle();
            if(Core.input.keyTap(Binding.planetMap) && state.isCampaign()) ui.planet.toggle();
            if(Core.input.keyTap(Binding.research) && state.isCampaign()) ui.research.toggle();
            if(Core.input.keyTap(Binding.schematicMenu)) ui.schematics.toggle();

            if(Core.input.keyTap(Binding.toggleBlockStatus)){
                Core.settings.put("blockstatus", !Core.settings.getBool("blockstatus"));
            }

            if(Core.input.keyTap(Binding.togglePowerLines)){
                if(Core.settings.getInt("lasersopacity") == 0){
                    Core.settings.put("lasersopacity", Core.settings.getInt("preferredlaseropacity", 100));
                }else{
                    Core.settings.put("preferredlaseropacity", Core.settings.getInt("lasersopacity"));
                    Core.settings.put("lasersopacity", 0);
                }
            }
        }

        if(state.isMenu() || Core.scene.hasDialog()) return;

        //zoom camera
        if((!Core.scene.hasScroll() || Core.input.keyDown(Binding.diagonalPlacement)) && !ui.chatfrag.shown() && !ui.consolefrag.shown() && Math.abs(Core.input.axisTap(Binding.zoom)) > 0
            && !Core.input.keyDown(Binding.rotatePlaced) && (Core.input.keyDown(Binding.diagonalPlacement) ||
                !Binding.zoom.value.equals(Binding.rotate.value) || ((!player.isBuilder() || !isPlacing() || !block.rotate) && selectPlans.isEmpty()))){
            renderer.scaleCamera(Core.input.axisTap(Binding.zoom));
        }

        if(Core.input.keyTap(Binding.select) && !Core.scene.hasMouse() && !abilityTargetingActive() && !suppressSelectionTap()){
            Tile selected = world.tileWorld(input.mouseWorldX(), input.mouseWorldY());
            if(selected != null){
                Call.tileTap(player, selected);
            }
        }

        if(Core.input.keyRelease(Binding.select) && commandRect){
            selectUnitsRect();
        }

        if(player.dead() || locked){
            cursorType = SystemCursor.arrow;
            if(!locked){
                pollInputNoPlayer();
            }
        }else{
            pollInputPlayer();
        }

        HoverInfo hover = updateHover(false);
        if(useAbilityTargetCursor()){
            cursorType = targetCursor(hover);
        }else if(hover.isValid()){
            cursorType = hoverCursor(hover);
        }

        if(Core.input.keyRelease(Binding.select)){
            player.shooting = false;
        }

        boolean hoverCursor = cursorType == ui.hoverGreenCursor || cursorType == ui.hoverRedCursor || cursorType == ui.hoverYellowCursor;
        if((!Core.scene.hasMouse() || hoverCursor) && !ui.minimapfrag.shown()){
            Core.graphics.cursor(cursorType);
            changedCursor = cursorType != SystemCursor.arrow;
        }else{
            cursorType = SystemCursor.arrow;
            if(changedCursor){
                graphics.cursor(SystemCursor.arrow);
                changedCursor = false;
            }
        }
    }

    @Override
    public void useSchematic(Schematic schem, boolean checkHidden){
        block = null;
        schematicX = tileX(getMouseX());
        schematicY = tileY(getMouseY());

        selectPlans.clear();
        selectPlans.addAll(schematics.toPlans(schem, schematicX, schematicY, checkHidden));
        mode = none;
    }

    @Override
    public boolean isBreaking(){
        return mode == breaking;
    }

    @Override
    public void buildPlacementUI(Table table){
        table.left().margin(0f).defaults().size(48f).left();

        table.button(Icon.paste, Styles.clearNonei, () -> {
            ui.schematics.show();
        }).tooltip("@schematics");

        table.button(Icon.book, Styles.clearNonei, () -> {
            ui.database.show();
        }).tooltip("@database");

        table.button(Icon.tree, Styles.clearNonei, () -> {
            ui.research.show();
        }).visible(() -> state.isCampaign()).tooltip("@research");

        table.button(Icon.map, Styles.clearNonei, () -> {
            ui.planet.show();
        }).visible(() -> state.isCampaign()).tooltip("@planetmap");
    }

    void pollInputNoPlayer(){
        if(Core.input.keyTap(Binding.select) && !Core.scene.hasMouse() && !abilityTargetingActive() && !suppressSelectionTap()){
            tappedOne = false;

            Tile selected = tileAt(Core.input.mouseX(), Core.input.mouseY());

            if(commandMode){
                commandRect = true;
                commandRectX = input.mouseWorldX();
                commandRectY = input.mouseWorldY();
            }else if(selected != null){
                tileTapped(selected.build);
            }
        }
    }

    //player input: for controlling the player unit (will crash if the unit is not present)
    void pollInputPlayer(){
        if(scene.hasField()) return;

        Tile selected = tileAt(Core.input.mouseX(), Core.input.mouseY());
        int cursorX = tileX(Core.input.mouseX());
        int cursorY = tileY(Core.input.mouseY());
        int rawCursorX = World.toTile(Core.input.mouseWorld().x), rawCursorY = World.toTile(Core.input.mouseWorld().y);
        if(isPlacing() && block == Blocks.ventCondenser){
            Tile snap = findNearestVentCenter(Core.input.mouseWorldX(), Core.input.mouseWorldY(), 30);
            if(snap != null){
                cursorX = snap.x;
                cursorY = snap.y;
            }
        }

        //automatically pause building if the current build queue is empty
        if(Core.settings.getBool("buildautopause") && isBuilding && !player.unit().isBuilding()){
            isBuilding = false;
            buildWasAutoPaused = true;
        }

        if(!selectPlans.isEmpty()){
            int shiftX = rawCursorX - schematicX, shiftY = rawCursorY - schematicY;

            selectPlans.each(s -> {
                s.x += shiftX;
                s.y += shiftY;
            });

            schematicX += shiftX;
            schematicY += shiftY;
        }

        if(Core.input.keyTap(Binding.deselect) && !ui.minimapfrag.shown() && !isPlacing() && player.unit().plans.isEmpty() && !commandMode){
            player.unit().mineTile = null;
            selectedResource = null;
        }

        if(Core.input.keyTap(Binding.clearBuilding) && !player.dead()){
            player.unit().clearBuilding();
        }

        if((Core.input.keyTap(Binding.schematicSelect) || Core.input.keyTap(Binding.rebuildSelect)) && !Core.scene.hasKeyboard() && mode != breaking){
            schemX = rawCursorX;
            schemY = rawCursorY;
        }

        if(Core.input.keyTap(Binding.clearBuilding) || isPlacing()){
            lastSchematic = null;
            selectPlans.clear();
        }

        if(!Core.scene.hasKeyboard() && selectX == -1 && selectY == -1 && schemX != -1 && schemY != -1){
            if(Core.input.keyRelease(Binding.schematicSelect)){
                lastSchematic = schematics.create(schemX, schemY, rawCursorX, rawCursorY);
                useSchematic(lastSchematic);
                if(selectPlans.isEmpty()){
                    lastSchematic = null;
                }
                schemX = -1;
                schemY = -1;
            }else if(input.keyRelease(Binding.rebuildSelect)){

                rebuildArea(schemX, schemY, rawCursorX, rawCursorY);
                schemX = -1;
                schemY = -1;
            }
        }

        if(!selectPlans.isEmpty()){
            if(Core.input.keyTap(Binding.schematicFlipX)){
                flipPlans(selectPlans, true);
            }

            if(Core.input.keyTap(Binding.schematicFlipY)){
                flipPlans(selectPlans, false);
            }
        }

        if(splan != null){
            int x = Math.round((Core.input.mouseWorld().x + buildPlanMouseOffsetX) / tilesize);
            int y = Math.round((Core.input.mouseWorld().y + buildPlanMouseOffsetY) / tilesize);
            if(splan.x != x || splan.y != y){
                splan.x = x;
                splan.y = y;
                movedPlan = true;
            }
        }

        if(block == null || mode != placing){
            linePlans.clear();
        }

        if(Core.input.keyTap(Binding.pauseBuilding)){
            isBuilding = !isBuilding;
            buildWasAutoPaused = false;

            if(isBuilding){
                player.shooting = false;
            }
        }

        if(isPlacing() && mode == placing && (cursorX != lastLineX || cursorY != lastLineY || Core.input.keyTap(Binding.diagonalPlacement) || Core.input.keyRelease(Binding.diagonalPlacement))){
            updateLine(selectX, selectY, cursorX, cursorY);
            lastLineX = cursorX;
            lastLineY = cursorY;
        }

        if(Core.input.keyRelease(Binding.select) && !Core.scene.hasMouse()){
            BuildPlan plan = getPlan(cursorX, cursorY);

            if(plan != null && !movedPlan){
                //move selected to front
                int index = player.unit().plans.indexOf(plan, true);
                if(index != -1){
                    player.unit().plans.removeIndex(index);
                    player.unit().plans.addFirst(plan);
                }
            }
        }

        if(Core.input.keyTap(Binding.select) && !Core.scene.hasMouse() && !abilityTargetingActive() && !suppressSelectionTap()){
            if(ui.hudfrag.abilityPanel != null && ui.hudfrag.abilityPanel.activeCommand != mindustry.ui.UnitAbilityPanel.CommandMode.NONE){
                //don't change selection while choosing a command target
                selectMillis = Time.millis();
                prevSelected = selected;
            }else{
                tappedOne = false;
                BuildPlan plan = getPlan(cursorX, cursorY);

                if(Core.input.keyDown(Binding.breakBlock)){
                    mode = none;
                }else if(!selectPlans.isEmpty()){
                    flushPlans(selectPlans);
                    movedPlan = true;
                }else if(isPlacing()){
                    selectX = cursorX;
                    selectY = cursorY;
                    lastLineX = cursorX;
                    lastLineY = cursorY;
                    mode = placing;
                    updateLine(selectX, selectY, cursorX, cursorY);
                }else if(plan != null && !plan.breaking && mode == none && !plan.initialized && plan.progress <= 0f){
                    splan = plan;
                    movedPlan = false;
                    buildPlanMouseOffsetX = splan.x * tilesize - Core.input.mouseWorld().x;
                    buildPlanMouseOffsetY = splan.y * tilesize - Core.input.mouseWorld().y;
                }else if(plan != null && plan.breaking){
                    deleting = true;
                }else if(commandMode && ui.hudfrag.abilityPanel != null && ui.hudfrag.abilityPanel.activeCommand == mindustry.ui.UnitAbilityPanel.CommandMode.NONE){
                    //Only allow box selection if NOT in an active RTS command mode
                    commandRect = true;
                    commandRectX = input.mouseWorldX();
                    commandRectY = input.mouseWorldY();
                }else if(!checkConfigTap() && selected != null && !tryRepairDerelict(selected)){
                    if(trySelectResource(selected)){
                        //resource selection consumes the tap
                    }else{
                        selectedResource = null;
                        //only begin shooting if there's no cursor event
                        if(!tryTapPlayer(Core.input.mouseWorld().x, Core.input.mouseWorld().y) && !tileTapped(selected.build) && !player.unit().activelyBuilding() && !droppingItem
                            && !(tryStopMine(selected) || (!settings.getBool("doubletapmine") || selected == prevSelected && Time.timeSinceMillis(selectMillis) < 500) && tryBeginMine(selected)) && !Core.scene.hasKeyboard()){
                            player.shooting = shouldShoot;
                        }
                    }
                }else if(!Core.scene.hasKeyboard()){ //if it's out of bounds, shooting is just fine
                    player.shooting = shouldShoot;
                }
                selectMillis = Time.millis();
                prevSelected = selected;
            }
        }else if(Core.input.keyTap(Binding.deselect) && isPlacing()){
            block = null;
            mode = none;
        }else if(Core.input.keyTap(Binding.deselect) && !selectPlans.isEmpty()){
            selectPlans.clear();
            lastSchematic = null;
        }else if(Core.input.keyTap(Binding.breakBlock) && !Core.scene.hasMouse() && player.isBuilder() && !commandMode){
            //is recalculated because setting the mode to breaking removes potential multiblock cursor offset
            deleting = false;
            mode = breaking;
            selectX = tileX(Core.input.mouseX());
            selectY = tileY(Core.input.mouseY());
            schemX = rawCursorX;
            schemY = rawCursorY;
        }

        if(Core.input.keyDown(Binding.select) && mode == none && !isPlacing() && deleting){
            var plan = getPlan(cursorX, cursorY);
            if(plan != null && plan.breaking){
                player.unit().plans().remove(plan);
            }
        }else{
            deleting = false;
        }

        if(mode == placing && block != null){
            if(!overrideLineRotation && !Core.input.keyDown(Binding.diagonalPlacement) && (selectX != cursorX || selectY != cursorY) && ((int)Core.input.axisTap(Binding.rotate) != 0)){
                rotation = ((int)((Angles.angle(selectX, selectY, cursorX, cursorY) + 45) / 90f)) % 4;
                overrideLineRotation = true;
            }
        }else{
            overrideLineRotation = false;
        }

        if(Core.input.keyRelease(Binding.breakBlock) && Core.input.keyDown(Binding.schematicSelect) && mode == breaking){
            lastSchematic = schematics.create(schemX, schemY, rawCursorX, rawCursorY);
            schemX = -1;
            schemY = -1;
        }

        if(Core.input.keyRelease(Binding.breakBlock) || Core.input.keyRelease(Binding.select)){

            if(mode == placing && block != null){ //touch up while placing, place everything in selection
                if(input.keyDown(Binding.boost)){
                    flushPlansReverse(linePlans);
                }else{
                    flushPlans(linePlans);
                }

                linePlans.clear();
                Events.fire(new LineConfirmEvent());
            }else if(mode == breaking){ //touch up while breaking, break everything in selection
                removeSelection(selectX, selectY, cursorX, cursorY, !Core.input.keyDown(Binding.schematicSelect) ? maxLength : Vars.maxSchematicSize);
                if(lastSchematic != null){
                    useSchematic(lastSchematic);
                    lastSchematic = null;
                }
            }
            selectX = -1;
            selectY = -1;

            tryDropItems(selected == null ? null : selected.build, Core.input.mouseWorld().x, Core.input.mouseWorld().y);

            if(splan != null){
                if(getPlan(splan.x, splan.y, splan.block.size, splan) != null){
                    player.unit().plans().remove(splan, true);
                }

                if(input.ctrl()){
                    inv.hide();
                    config.hideConfig();
                    planConfig.showConfig(splan);
                }else{
                    planConfig.hide();
                }

                splan = null;
            }

            mode = none;
        }


        //deselect if not placing
        if(!isPlacing() && mode == placing){
            mode = none;
        }

        if(player.shooting && !canShoot()){
            player.shooting = false;
        }

        if(isPlacing() && player.isBuilder()){
            cursorType = SystemCursor.hand;
            selectScale = Mathf.lerpDelta(selectScale, 1f, 0.2f);
        }else{
            selectScale = 0f;
        }

        if(!Core.input.keyDown(Binding.diagonalPlacement) && Math.abs((int)Core.input.axisTap(Binding.rotate)) > 0){
            rotation = Mathf.mod(rotation + (int)Core.input.axisTap(Binding.rotate), 4);

            if(splan != null){
                splan.rotation = Mathf.mod(splan.rotation + (int)Core.input.axisTap(Binding.rotate), 4);
            }

            if(isPlacing() && mode == placing){
                updateLine(selectX, selectY, cursorX, cursorY);
            }else if(!selectPlans.isEmpty() && !ui.chatfrag.shown()){
                rotatePlans(selectPlans, Mathf.sign(Core.input.axisTap(Binding.rotate)));
            }
        }

        Tile cursor = tileAt(Core.input.mouseX(), Core.input.mouseY());

        cursorType = SystemCursor.arrow;

        if(cursor != null){
            if(cursor.build != null && cursor.build.interactable(player.team())){
                cursorType = cursor.build.getCursor();
            }

            if(canRepairDerelict(cursor) && !player.dead() && player.unit().canBuild()){
                cursorType = ui.repairCursor;
            }

            if((isPlacing() && player.isBuilder()) || !selectPlans.isEmpty()){
                cursorType = SystemCursor.hand;
            }

            if(!isPlacing() && canMine(cursor)){
                cursorType = ui.drillCursor;
            }

            if(commandMode && selectedUnits.any()){
                if(input.keyTap(Binding.commandQueue) && Binding.commandQueue.value.key.type != KeyType.mouse){
                    if(commandFocusGuardTime <= 0f){
                        commandTap(input.mouseX(), input.mouseY(), true);
                    }
                }
            }

            if(getPlan(cursor.x, cursor.y) != null && mode == none){
                cursorType = SystemCursor.hand;
            }

            if(canTapPlayer(Core.input.mouseWorld().x, Core.input.mouseWorld().y)){
                cursorType = ui.unloadCursor;
            }

            if(cursor.build != null && cursor.interactable(player.team()) && !isPlacing() && Math.abs(Core.input.axisTap(Binding.rotate)) > 0 && Core.input.keyDown(Binding.rotatePlaced) && cursor.block().rotate && cursor.block().quickRotate){
                Call.rotateBlock(player, cursor.build, Core.input.axisTap(Binding.rotate) > 0);
            }
        }

    }

    private boolean isIdleWorker(Unit unit){
        if(unit == null || !unit.isCommandable()) return false;
        if(unit.type != UnitTypes.nova) return false;
        if(unit.controller() instanceof HarvestAI) return false;
        if(unit.activelyBuilding() || unit.isBuilding()) return false;
        if(unit.controller() instanceof CommandAI){
            CommandAI ai = (CommandAI)unit.controller();
            if(ai.hasCommand() || ai.commandQueue.any() || ai.attackTarget != null || ai.followTarget != null || ai.pendingHarvestTarget != null ||
                ai.queuedCommandPos != null || ai.queuedCommandTarget != null || ai.queuedFollowTarget != null){
                return false;
            }
        }
        return true;
    }

    private Cursor hoverCursor(HoverInfo hover){
        if(hover.resource != null) return ui.hoverYellowCursor;
        Team team = hover.team;
        if(team == null) return SystemCursor.arrow;
        if(team == player.team()) return ui.hoverGreenCursor;
        if(team == Team.derelict) return ui.hoverYellowCursor;
        return team != player.team() ? ui.hoverRedCursor : ui.hoverGreenCursor;
    }

    private boolean useAbilityTargetCursor(){
        if(ui.hudfrag.abilityPanel == null) return false;
        var mode = ui.hudfrag.abilityPanel.activeCommand;
        return mode == mindustry.ui.UnitAbilityPanel.CommandMode.RALLY
        || mode == mindustry.ui.UnitAbilityPanel.CommandMode.DROP_PULSAR
        || mode == mindustry.ui.UnitAbilityPanel.CommandMode.EXTRA_SUPPLY
        || mode == mindustry.ui.UnitAbilityPanel.CommandMode.SCAN
        || mode == mindustry.ui.UnitAbilityPanel.CommandMode.LAND
        || mode == mindustry.ui.UnitAbilityPanel.CommandMode.LIBERATOR_ZONE
        || mode == mindustry.ui.UnitAbilityPanel.CommandMode.MEDIVAC_HEAL
        || mode == mindustry.ui.UnitAbilityPanel.CommandMode.MEDIVAC_LOAD
        || mode == mindustry.ui.UnitAbilityPanel.CommandMode.MEDIVAC_UNLOAD
        || mode == mindustry.ui.UnitAbilityPanel.CommandMode.BATTLECRUISER_YAMATO
        || mode == mindustry.ui.UnitAbilityPanel.CommandMode.BATTLECRUISER_WARP
        || mode == mindustry.ui.UnitAbilityPanel.CommandMode.GHOST_TACTICAL_NUKE
        || mode == mindustry.ui.UnitAbilityPanel.CommandMode.GHOST_STABLE_AIM
        || mode == mindustry.ui.UnitAbilityPanel.CommandMode.GHOST_EMP
        || mode == mindustry.ui.UnitAbilityPanel.CommandMode.RAVEN_ANTI_ARMOR
        || mode == mindustry.ui.UnitAbilityPanel.CommandMode.RAVEN_MATRIX
        || mode == mindustry.ui.UnitAbilityPanel.CommandMode.BUNKER_ATTACK
        || mode == mindustry.ui.UnitAbilityPanel.CommandMode.BUNKER_LOAD
        || mode == mindustry.ui.UnitAbilityPanel.CommandMode.HARVEST
        || mode == mindustry.ui.UnitAbilityPanel.CommandMode.MOVE
        || mode == mindustry.ui.UnitAbilityPanel.CommandMode.ATTACK
        || mode == mindustry.ui.UnitAbilityPanel.CommandMode.PATROL;
    }

    private Cursor targetCursor(HoverInfo hover){
        if(hover == null || !hover.isValid()) return ui.targetYellowCursor;
        if(hover.resource != null) return ui.targetYellowCursor;
        Team team = hover.team;
        if(team == null) return ui.targetYellowCursor;
        if(team == player.team()) return ui.targetGreenCursor;
        if(team == Team.derelict) return ui.targetYellowCursor;
        return team != player.team() ? ui.targetRedCursor : ui.targetGreenCursor;
    }

    @Override
    public boolean tap(float x, float y, int count, KeyCode button){
        if(scene.hasMouse() || !commandMode) return false;
        if(button == KeyCode.mouseLeft && (abilityTargetingActive() || suppressSelectionTap())){
            return true;
        }

        //Command mode is now handled in touchDown, not tap
        //This prevents double execution

        tappedOne = true;

        //click: select a single unit
        if(button == KeyCode.mouseLeft){
            if(count >= 2){
                selectTypedUnits();
            }else{
                tapCommandUnit();
            }

        }

        return super.tap(x, y, count, button);
    }

    public void executeActiveCommand(float screenX, float screenY){
        if(ui.hudfrag.abilityPanel == null) return;
        if(commandFocusGuardTime > 0f) return;

        var mode = ui.hudfrag.abilityPanel.activeCommand;
        Vec2 world = Core.camera.unproject(screenX, screenY);
        if(!isValidCommandWorld(world.x, world.y)) return;
        float worldX = clampCommandX(world.x);
        float worldY = clampCommandY(world.y);
        boolean shiftHeld = Core.input.keyDown(KeyCode.shiftLeft) || Core.input.keyDown(KeyCode.shiftRight);

        if(mode == mindustry.ui.UnitAbilityPanel.CommandMode.HARVEST){
            if(executeHarvestCommand(worldX, worldY)){
                ui.hudfrag.abilityPanel.exitCommandMode();
            }
            return;
        }

        if(mode == mindustry.ui.UnitAbilityPanel.CommandMode.RALLY){
            executeRallyCommand(worldX, worldY);
            ui.hudfrag.abilityPanel.exitCommandMode();
            return;
        }

        if(mode == mindustry.ui.UnitAbilityPanel.CommandMode.DROP_PULSAR){
            if(executeDropPulsarCommand(worldX, worldY)){
                if(!shiftHeld){
                    ui.hudfrag.abilityPanel.exitCommandMode();
                }
            }
            return;
        }

        if(mode == mindustry.ui.UnitAbilityPanel.CommandMode.EXTRA_SUPPLY){
            if(executeExtraSupplyCommand(worldX, worldY)){
                if(!shiftHeld){
                    ui.hudfrag.abilityPanel.exitCommandMode();
                }
            }
            return;
        }

        if(mode == mindustry.ui.UnitAbilityPanel.CommandMode.SCAN){
            if(executeScanCommand(worldX, worldY)){
                if(!shiftHeld){
                    ui.hudfrag.abilityPanel.exitCommandMode();
                }
            }
            return;
        }

        if(mode == mindustry.ui.UnitAbilityPanel.CommandMode.LAND){
            if(executeLandCommand(worldX, worldY)){
                ui.hudfrag.abilityPanel.exitCommandMode();
            }
            return;
        }

        if(mode == mindustry.ui.UnitAbilityPanel.CommandMode.LIBERATOR_ZONE){
            if(executeLiberatorZoneCommand(worldX, worldY) && !shiftHeld){
                ui.hudfrag.abilityPanel.exitCommandMode();
            }
            return;
        }

        if(mode == mindustry.ui.UnitAbilityPanel.CommandMode.MEDIVAC_HEAL){
            if(executeMedivacHealCommand(worldX, worldY) && !shiftHeld){
                ui.hudfrag.abilityPanel.exitCommandMode();
            }
            return;
        }

        if(mode == mindustry.ui.UnitAbilityPanel.CommandMode.MEDIVAC_LOAD){
            if(executeMedivacLoadCommand(worldX, worldY) && !shiftHeld){
                ui.hudfrag.abilityPanel.exitCommandMode();
            }
            return;
        }

        if(mode == mindustry.ui.UnitAbilityPanel.CommandMode.MEDIVAC_UNLOAD){
            if(executeMedivacUnloadCommand(worldX, worldY) && !shiftHeld){
                ui.hudfrag.abilityPanel.exitCommandMode();
            }
            return;
        }

        if(mode == mindustry.ui.UnitAbilityPanel.CommandMode.BATTLECRUISER_YAMATO){
            if(executeBattlecruiserYamatoCommand(worldX, worldY) && !shiftHeld){
                ui.hudfrag.abilityPanel.exitCommandMode();
            }
            return;
        }

        if(mode == mindustry.ui.UnitAbilityPanel.CommandMode.BATTLECRUISER_WARP){
            if(executeBattlecruiserWarpCommand(worldX, worldY) && !shiftHeld){
                ui.hudfrag.abilityPanel.exitCommandMode();
            }
            return;
        }

        if(mode == mindustry.ui.UnitAbilityPanel.CommandMode.GHOST_TACTICAL_NUKE){
            if(executeGhostTacticalNukeCommand(worldX, worldY) && !shiftHeld){
                ui.hudfrag.abilityPanel.exitCommandMode();
            }
            return;
        }

        if(mode == mindustry.ui.UnitAbilityPanel.CommandMode.GHOST_STABLE_AIM){
            if(executeGhostStableAimCommand(worldX, worldY) && !shiftHeld){
                ui.hudfrag.abilityPanel.exitCommandMode();
            }
            return;
        }

        if(mode == mindustry.ui.UnitAbilityPanel.CommandMode.GHOST_EMP){
            if(executeGhostEmpCommand(worldX, worldY) && !shiftHeld){
                ui.hudfrag.abilityPanel.exitCommandMode();
            }
            return;
        }

        if(mode == mindustry.ui.UnitAbilityPanel.CommandMode.RAVEN_ANTI_ARMOR){
            if(executeRavenAntiArmorCommand(worldX, worldY) && !shiftHeld){
                ui.hudfrag.abilityPanel.exitCommandMode();
            }
            return;
        }

        if(mode == mindustry.ui.UnitAbilityPanel.CommandMode.RAVEN_MATRIX){
            if(executeRavenMatrixCommand(worldX, worldY) && !shiftHeld){
                ui.hudfrag.abilityPanel.exitCommandMode();
            }
            return;
        }

        if(mode == mindustry.ui.UnitAbilityPanel.CommandMode.BUNKER_ATTACK){
            if(executeBunkerAttackCommand(worldX, worldY)){
                ui.hudfrag.abilityPanel.exitCommandMode();
            }
            return;
        }

        if(mode == mindustry.ui.UnitAbilityPanel.CommandMode.BUNKER_LOAD){
            if(executeBunkerLoadCommand(worldX, worldY)){
                ui.hudfrag.abilityPanel.exitCommandMode();
            }
            return;
        }

        if(mode == mindustry.ui.UnitAbilityPanel.CommandMode.BUILD_PLACE){
            executeBuildPlacement(worldX, worldY);
            return;
        }

        //Shift queues commands immediately so units start moving right away.
        if(shiftHeld){
            executeCommandAtTarget(mode, worldX, worldY, true);
            return;
        }

        //Execute immediately if Shift not held
        executeCommandAtTarget(mode, worldX, worldY, false);

        //Exit command mode after executing
        ui.hudfrag.abilityPanel.exitCommandMode();
    }

    private boolean executeHarvestCommand(float worldX, float worldY){
        Tile tile = world.tileWorld(worldX, worldY);
        Tile resource = resolveResourceTile(tile);
        if(resource == null || !(resource.block() instanceof CrystalMineralWall)) return false;

        Vec2 target = Tmp.v1.set(resource.worldx(), resource.worldy());
        for(Unit unit : selectedUnits){
            if(unit == null || !unit.isValid()) continue;
            if(unit.controller() instanceof CommandAI){
                ((CommandAI)unit.controller()).setHarvestTarget(target);
            }else if(unit.controller() instanceof HarvestAI){
                ((HarvestAI)unit.controller()).setHarvestTarget(target);
            }
        }
        return true;
    }

    private void executeRallyCommand(float worldX, float worldY){
        if(commandBuildings.isEmpty()){
            if(selectedUnits.isEmpty()) return;
            for(Unit unit : selectedUnits){
                if(unit == null || unit.type != UnitTypes.coreFlyer || !(unit instanceof Payloadc payload)) continue;
                if(payload.payloads().isEmpty()) continue;
                Payload top = payload.payloads().peek();
                if(top instanceof BuildPayload buildPayload){
                    if(buildPayload.build instanceof CoreBlock.CoreBuild core){
                        core.onCommand(new Vec2(worldX, worldY));
                    }
                }
            }
            return;
        }
        int[] builds = commandBuildings.mapInt(b -> b.pos()).toArray();
        Call.commandBuilding(player, builds, new Vec2(worldX, worldY));
    }

    private boolean executeDropPulsarCommand(float worldX, float worldY){
        Seq<CoreBlock.CoreBuild> cores = selectedOrbitalCores();
        if(cores.isEmpty()) return false;

        Tile target = world.tileWorld(worldX, worldY);
        if(target == null) return false;

        Tile resource = resolveResourceTile(target);
        Tile harvestTarget = null;

        if(resource != null && resource.block() instanceof CrystalMineralWall){
            harvestTarget = resource;
        }else{
            if(target.build != null || target.solid()) return false;
        }

        int start = nextOrbitalCoreStartIndex(cores);
        for(int i = 0; i < cores.size; i++){
            CoreBlock.CoreBuild core = cores.get((start + i) % cores.size);
            Tile spawnTile = resource != null && resource.block() instanceof CrystalMineralWall ? findSpawnTileNearCore(core, resource) : target;
            if(spawnTile == null || spawnTile.build != null || spawnTile.solid()) continue;
            if(!core.consumeOrbitalEnergy(CoreBlock.orbitalAbilityCost)) continue;

            lastOrbitalCoreId = core.id;

            float spawnX = spawnTile.worldx();
            float spawnY = spawnTile.worldy();
            final Team team = core.team;
            final float spawnXFinal = spawnX;
            final float spawnYFinal = spawnY;
            final Tile harvestTargetFinal = harvestTarget;

            Fx.sc2DropPod.at(spawnXFinal, spawnYFinal);

            Time.run(3f * 60f, () -> {
                Tile check = world.tileWorld(spawnXFinal, spawnYFinal);
                if(check != null && (check.build != null || check.solid())) return;

                Unit unit = UnitTypes.pulsar.create(team);
                unit.set(spawnXFinal, spawnYFinal);
                unit.add();
                Fx.launchPod.at(spawnXFinal, spawnYFinal);
                PulsarDrops.register(unit);

                if(harvestTargetFinal != null){
                    if(unit.controller() instanceof CommandAI ai){
                        ai.setHarvestTarget(Tmp.v3.set(harvestTargetFinal.worldx(), harvestTargetFinal.worldy()));
                    }else if(unit.controller() instanceof HarvestAI ai){
                        ai.setHarvestTarget(Tmp.v3.set(harvestTargetFinal.worldx(), harvestTargetFinal.worldy()));
                    }
                }

                Time.run(PulsarDrops.lifetime, () -> {
                    if(unit != null && unit.isValid()){
                        unit.kill();
                    }
                    PulsarDrops.remove(unit);
                });
            });

            return true;
        }

        return false;
    }

    private boolean executeExtraSupplyCommand(float worldX, float worldY){
        Seq<CoreBlock.CoreBuild> cores = selectedOrbitalCores();
        if(cores.isEmpty()) return false;

        Building build = world.buildWorld(worldX, worldY);
        if(build == null || build.team != player.team()) return false;
        if(build.block != Blocks.doorLarge && build.block != Blocks.doorLargeErekir) return false;
        if(!(build instanceof Door.DoorBuild)) return false;

        int start = nextOrbitalCoreStartIndex(cores);
        for(int i = 0; i < cores.size; i++){
            CoreBlock.CoreBuild core = cores.get((start + i) % cores.size);
            if(!core.consumeOrbitalEnergy(CoreBlock.orbitalAbilityCost)) continue;
            lastOrbitalCoreId = core.id;
            applyExtraSupply(build.tile);
            return true;
        }
        return false;
    }

    private boolean executeScanCommand(float worldX, float worldY){
        Seq<CoreBlock.CoreBuild> cores = selectedOrbitalCores();
        if(cores.isEmpty()) return false;

        int start = nextOrbitalCoreStartIndex(cores);
        for(int i = 0; i < cores.size; i++){
            CoreBlock.CoreBuild core = cores.get((start + i) % cores.size);
            if(!core.consumeOrbitalEnergy(CoreBlock.orbitalAbilityCost)) continue;
            lastOrbitalCoreId = core.id;

            Unit unit = UnitTypes.scanProbe.create(player.team());
            unit.set(worldX, worldY);
            unit.add();
            Fx.padlaunch.at(worldX, worldY);
            Fx.sc2Scan.at(worldX, worldY, 10f * tilesize);
            Time.run(9f * 60f, () -> {
                if(unit != null && unit.isValid()){
                    unit.remove();
                }
            });
            return true;
        }
        return false;
    }

    private Seq<CoreBlock.CoreBuild> selectedOrbitalCores(){
        Seq<CoreBlock.CoreBuild> cores = new Seq<>();
        for(Building build : commandBuildings){
            if(build instanceof CoreBlock.CoreBuild core && core.block == Blocks.coreOrbital){
                cores.add(core);
            }
        }
        return cores;
    }

    private int nextOrbitalCoreStartIndex(Seq<CoreBlock.CoreBuild> cores){
        if(lastOrbitalCoreId == -1) return 0;
        for(int i = 0; i < cores.size; i++){
            if(cores.get(i).id == lastOrbitalCoreId){
                return (i + 1) % cores.size;
            }
        }
        return 0;
    }

    private void applyExtraSupply(Tile tile){
        if(tile == null || tile.build == null) return;
        Building build = tile.build;
        if(!(build instanceof Door.DoorBuild)) return;
        boolean open = ((Door.DoorBuild)build).open;
        tile.setBlock(Blocks.doorLargeErekir, build.team, build.rotation);
        if(tile.build instanceof Door.DoorBuild door){
            door.health = door.block.health;
            door.configure(open);
        }
    }

    private @Nullable BuildPayload selectedCoreFlyerPayload(){
        if(selectedUnits.isEmpty()) return null;
        for(Unit unit : selectedUnits){
            if(unit == null || unit.type != UnitTypes.coreFlyer || !(unit instanceof Payloadc payload)) continue;
            if(payload.payloads().isEmpty()) continue;
            Payload top = payload.payloads().peek();
            if(top instanceof BuildPayload buildPayload){
                return buildPayload;
            }
        }
        return null;
    }

    private boolean executeLandCommand(float worldX, float worldY){
        if(selectedUnits.isEmpty()) return false;
        boolean any = false;
        for(Unit unit : selectedUnits){
            if(unit == null || unit.type != UnitTypes.coreFlyer || !(unit instanceof Payloadc payload)) continue;
            if(payload.payloads().isEmpty()) continue;
            Payload top = payload.payloads().peek();
            if(!(top instanceof BuildPayload buildPayload)) continue;
            Block block = buildPayload.build.block;

            float offset = block.offset;
            int tx = World.toTile(worldX - offset);
            int ty = World.toTile(worldY - offset);
            if(!Build.validPlace(block, unit.team, tx, ty, buildPayload.build.rotation, false)){
                continue;
            }

            float landX = tx * tilesize + offset;
            float landY = ty * tilesize + offset;

            UnitTypes.CoreFlyerData data = UnitTypes.getCoreFlyerData(unit);
            data.target.set(landX, landY);
            data.active = true;
            data.landing = false;
            data.landTime = 0f;
            data.returnRotation = buildPayload.build.rotation * 90f;

            if(unit.isCommandable()){
                unit.command().commandPosition(data.target);
            }

            landConfirmPlan = new BuildPlan(tx, ty, buildPayload.build.rotation, block);
            landConfirmUnitId = unit.id;
            any = true;
        }
        return any;
    }

    private int[] selectedMedivacIds(Boolf<Unit> filter){
        IntSeq ids = new IntSeq();
        for(Unit unit : selectedUnits){
            if(unit == null || !unit.isValid() || !UnitTypes.isMedivac(unit)) continue;
            if(filter != null && !filter.get(unit)) continue;
            ids.add(unit.id);
        }
        return ids.toArray();
    }

    private int[] selectedLiberatorIds(Boolf<Unit> filter){
        IntSeq ids = new IntSeq();
        for(Unit unit : selectedUnits){
            if(unit == null || !unit.isValid() || !UnitTypes.isLiberator(unit)) continue;
            if(filter != null && !filter.get(unit)) continue;
            ids.add(unit.id);
        }
        return ids.toArray();
    }

    private int[] selectedRavenIds(Boolf<Unit> filter){
        IntSeq ids = new IntSeq();
        for(Unit unit : selectedUnits){
            if(unit == null || !unit.isValid() || !UnitTypes.isRaven(unit)) continue;
            if(filter != null && !filter.get(unit)) continue;
            ids.add(unit.id);
        }
        return ids.toArray();
    }

    private int[] selectedGhostIds(Boolf<Unit> filter){
        IntSeq ids = new IntSeq();
        for(Unit unit : selectedUnits){
            if(unit == null || !unit.isValid() || !UnitTypes.isGhost(unit)) continue;
            if(filter != null && !filter.get(unit)) continue;
            ids.add(unit.id);
        }
        return ids.toArray();
    }

    private int[] selectedBattlecruiserIds(Boolf<Unit> filter){
        IntSeq ids = new IntSeq();
        for(Unit unit : selectedUnits){
            if(unit == null || !unit.isValid() || !UnitTypes.isBattlecruiser(unit)) continue;
            if(filter != null && !filter.get(unit)) continue;
            ids.add(unit.id);
        }
        return ids.toArray();
    }

    private boolean executeLiberatorZoneCommand(float worldX, float worldY){
        int[] ids = selectedLiberatorIds(UnitTypes::liberatorCanEnterDefense);
        if(ids.length == 0) return false;
        Call.commandLiberatorMode(player, ids, true, new Vec2(worldX, worldY));
        return true;
    }

    private boolean executeMedivacHealCommand(float worldX, float worldY){
        Unit target = selectedAnyUnit(worldX, worldY);
        if(!UnitTypes.medivacCanHealTarget(target, player.team())) return false;

        int[] ids = selectedMedivacIds(u -> true);
        if(ids.length == 0) return false;

        Call.setUnitCommand(player, ids, UnitCommand.moveCommand);
        Call.commandMedivacMovingUnload(player, ids, false);
        Call.commandUnits(player, ids, null, target, new Vec2(target.x, target.y), false, true, false);
        return true;
    }

    private boolean executeMedivacLoadCommand(float worldX, float worldY){
        Unit target = selectedAnyUnit(worldX, worldY);
        if(target == null || target.team != player.team()) return false;

        int[] ids = selectedMedivacIds(u -> UnitTypes.medivacCanPickup(u, target));
        if(ids.length == 0) return false;

        Call.setUnitCommand(player, ids, UnitCommand.loadUnitsCommand);
        Call.commandMedivacMovingUnload(player, ids, false);
        Call.commandUnits(player, ids, null, target, new Vec2(target.x, target.y), false, true, false);
        return true;
    }

    private boolean executeMedivacUnloadCommand(float worldX, float worldY){
        int[] ids = selectedMedivacIds(u -> u instanceof Payloadc pay && !pay.payloads().isEmpty());
        if(ids.length == 0) return false;

        Call.setUnitCommand(player, ids, UnitCommand.unloadPayloadCommand);

        Unit clicked = selectedAnyUnit(worldX, worldY);
        boolean selfTarget = clicked != null && clicked.team == player.team() && selectedUnits.contains(clicked) && UnitTypes.isMedivac(clicked);
        if(selfTarget){
            Call.commandMedivacMovingUnload(player, ids, true);
            return true;
        }

        Call.commandMedivacMovingUnload(player, ids, false);
        Call.commandUnits(player, ids, null, null, new Vec2(worldX, worldY), false, true, false);
        return true;
    }

    private boolean executeRavenAntiArmorCommand(float worldX, float worldY){
        int[] ids = selectedRavenIds(UnitTypes::ravenCanUseAntiArmor);
        if(ids.length == 0) return false;
        Call.commandAvertAntiArmor(player, ids, new Vec2(worldX, worldY));
        return true;
    }

    private boolean executeBattlecruiserYamatoCommand(float worldX, float worldY){
        if(!UnitTypes.battlecruiserHasYamatoTech(player.team())) return false;

        Building build = world.buildWorld(worldX, worldY);
        Teamc target = (build != null && build.within(worldX, worldY, build.hitSize() / 2f)) ? build : null;
        if(target == null){
            target = selectedAnyUnit(worldX, worldY);
        }
        if(target == null) return false;

        int[] ids = selectedBattlecruiserIds(UnitTypes::battlecruiserCanUseYamato);
        if(ids.length == 0) return false;

        int targetId = target instanceof Unit u ? u.id : -1;
        int buildPos = target instanceof Building b ? b.pos() : -1;
        Call.commandBattlecruiserYamato(player, ids, targetId, buildPos);
        return true;
    }

    private boolean executeBattlecruiserWarpCommand(float worldX, float worldY){
        int[] ids = selectedBattlecruiserIds(UnitTypes::battlecruiserCanUseWarp);
        if(ids.length == 0) return false;
        Call.commandBattlecruiserWarp(player, ids, new Vec2(worldX, worldY));
        return true;
    }

    private boolean executeGhostTacticalNukeCommand(float worldX, float worldY){
        int[] ids = selectedGhostIds(u -> UnitTypes.ghostCanUseTacticalNuke(u, worldX, worldY));
        if(ids.length == 0) return false;
        Call.commandGhostTacticalNuke(player, ids, new Vec2(worldX, worldY));
        return true;
    }

    private boolean executeGhostStableAimCommand(float worldX, float worldY){
        Unit target = selectedAnyUnit(worldX, worldY);
        if(!UnitTypes.ghostStableAimValidTarget(target)) return false;
        int[] ids = selectedGhostIds(UnitTypes::ghostCanUseStableAim);
        if(ids.length == 0) return false;
        Call.commandGhostStableAim(player, ids, target.id);
        return true;
    }

    private boolean executeGhostEmpCommand(float worldX, float worldY){
        int[] ids = selectedGhostIds(UnitTypes::ghostCanUseEmp);
        if(ids.length == 0) return false;
        Call.commandGhostEmp(player, ids, new Vec2(worldX, worldY));
        return true;
    }

    private boolean executeRavenMatrixCommand(float worldX, float worldY){
        Unit target = selectedAnyUnit(worldX, worldY);
        if(target == null || !target.isValid()) return false;

        int[] ids = selectedRavenIds(u -> UnitTypes.ravenCanUseMatrix(u) && UnitTypes.ravenMatrixValidTarget(target, u.team));
        if(ids.length == 0) return false;

        Call.commandAvertMatrix(player, ids, target.id);
        return true;
    }

    private boolean executeBunkerAttackCommand(float worldX, float worldY){
        if(commandBuildings.isEmpty()) return false;
        int[] buildings = commandBuildings.mapInt(b -> b.pos()).toArray();
        if(buildings.length == 0) return false;
        Call.commandBuilding(player, buildings, new Vec2(worldX, worldY));
        return true;
    }

    private boolean executeBunkerLoadCommand(float worldX, float worldY){
        if(commandBuildings.isEmpty()) return false;
        Building bunker = null;
        for(Building build : commandBuildings){
            if(build instanceof BunkerBlock.BunkerBuild){
                bunker = build;
                break;
            }
        }
        if(!(bunker instanceof BunkerBlock.BunkerBuild bunkerBuild) || bunkerBuild.recycling) return false;

        Unit target = selectedAnyUnit(worldX, worldY);
        if(target == null || !target.isValid() || target.team != player.team()) return false;
        if(!bunkerBuild.canLoadType(target.type)) return false;

        Call.commandBunkerLoadUnits(player, bunker.pos(), new int[]{target.id});
        return true;
    }

    private @Nullable Tile findSpawnTileNearCore(CoreBlock.CoreBuild core, Tile resource){
        if(resource == null || core == null) return null;
        int dx = core.tile.x - resource.x;
        int dy = core.tile.y - resource.y;
        int stepX = Math.abs(dx) >= Math.abs(dy) ? (dx >= 0 ? 1 : -1) : 0;
        int stepY = Math.abs(dy) > Math.abs(dx) ? (dy >= 0 ? 1 : -1) : 0;

        int[] xs = {stepX, 1, -1, 0, 0};
        int[] ys = {stepY, 0, 0, 1, -1};
        for(int i = 0; i < xs.length; i++){
            int nx = resource.x + xs[i];
            int ny = resource.y + ys[i];
            Tile tile = world.tile(nx, ny);
            if(tile != null && !tile.solid()){
                return tile;
            }
        }
        return null;
    }

    private void executeBuildPlacement(float worldX, float worldY){
        if(ui.hudfrag.abilityPanel == null) return;
        Block block = ui.hudfrag.abilityPanel.getPlacingBlock();
        if(block == null){
            ui.hudfrag.abilityPanel.exitCommandMode();
            return;
        }

        Tmp.v1.set(worldX, worldY).sub(block.offset, block.offset);
        int tx = World.toTile(Tmp.v1.x);
        int ty = World.toTile(Tmp.v1.y);
        if(block == Blocks.ventCondenser){
            Tile snap = findNearestVentCenter(worldX, worldY, 30);
            if(snap != null){
                tx = snap.x;
                ty = snap.y;
                worldX = snap.worldx();
                worldY = snap.worldy();
            }
        }

        int placeRotation = 0;
        if(!validPlace(tx, ty, block, placeRotation, null, true)){
            return;
        }

        if(!mindustry.ui.UnitAbilityPanel.canAfford(block)){
            ui.hudfrag.setHudText(Core.bundle.get("bar.noresources", "Not enough resources"));
            return;
        }

        Unit chosen = null;
        float bestDst = Float.MAX_VALUE;
        for(Unit unit : selectedUnits){
            if(unit == null || !unit.isValid() || !unit.canBuild()) continue;
            float dst = unit.dst2(worldX, worldY);
            if(dst < bestDst){
                bestDst = dst;
                chosen = unit;
            }
        }

        if(chosen == null){
            return;
        }

        boolean queueCommand = chosen.isBuilding();
        BuildPlan plan = new BuildPlan(tx, ty, placeRotation, block, block.saveConfig ? block.lastConfig : null);
        plan.requireClose = true;
        chosen.addBuild(plan);
        chosen.updateBuilding(true);

        if(!state.rules.infiniteResources && !player.team().rules().infiniteResources){
            mindustry.ui.UnitAbilityPanel.payPlacementCost(block);
            if(!block.instantBuild){
                mindustry.world.blocks.ConstructBlock.markPrepaid(Point2.pack(tx, ty));
            }
        }
        mindustry.world.blocks.ConstructBlock.markForceBuildTime(Point2.pack(tx, ty));

        float targetX = tx * tilesize + block.offset;
        float targetY = ty * tilesize + block.offset;
        Call.commandUnits(player, new int[]{chosen.id}, null, null, new Vec2(targetX, targetY), queueCommand, true, false);

        ui.hudfrag.abilityPanel.exitCommandMode();
    }

    private void executeQueuedCommands(){
        if(queuedCommandTargets.isEmpty() || queuedCommandMode == mindustry.ui.UnitAbilityPanel.CommandMode.NONE){
            queuedCommandTargets.clear();
            queuedCommandMode = mindustry.ui.UnitAbilityPanel.CommandMode.NONE;
            return;
        }

        //For patrol, add waypoints in a loop
        if(queuedCommandMode == mindustry.ui.UnitAbilityPanel.CommandMode.PATROL && queuedCommandTargets.size > 0){
            //For patrol, we need to create a loop: current position -> waypoints -> back to start
            //First, add current position of units as starting point (if not queuing)
            if(selectedUnits.size > 0){
                //Get average position of selected units as patrol start
                float avgX = 0, avgY = 0;
                for(Unit unit : selectedUnits){
                    avgX += unit.x;
                    avgY += unit.y;
                }
                avgX /= selectedUnits.size;
                avgY /= selectedUnits.size;

                //Add starting position
                executeCommandAtTarget(queuedCommandMode, avgX, avgY, false);
            }

            //Execute patrol with looping waypoints
            for(int i = 0; i < queuedCommandTargets.size; i++){
                Vec2 target = queuedCommandTargets.get(i);
                executeCommandAtTarget(queuedCommandMode, target.x, target.y, true);
            }

            //Add first waypoint again to create infinite loop
            if(queuedCommandTargets.size > 0){
                Vec2 firstTarget = queuedCommandTargets.get(0);
                executeCommandAtTarget(queuedCommandMode, firstTarget.x, firstTarget.y, true);
            }
        }else{
            //Execute all queued commands in sequence for non-patrol commands
            //All commands are queued (appended) so units complete current objective first
            for(int i = 0; i < queuedCommandTargets.size; i++){
                Vec2 target = queuedCommandTargets.get(i);
                executeCommandAtTarget(queuedCommandMode, target.x, target.y, true); //Always queue
            }
        }

        //Clear queue
        queuedCommandTargets.clear();
        queuedCommandMode = mindustry.ui.UnitAbilityPanel.CommandMode.NONE;

        //Exit command mode
        if(ui.hudfrag.abilityPanel != null){
            ui.hudfrag.abilityPanel.exitCommandMode();
        }
    }

    private void executeCommandAtTarget(mindustry.ui.UnitAbilityPanel.CommandMode mode, float worldX, float worldY, boolean queue){
        worldX = clampCommandX(worldX);
        worldY = clampCommandY(worldY);

        int[] ids = new int[selectedUnits.size];
        for(int i = 0; i < ids.length; i++){
            ids[i] = selectedUnits.get(i).id;
        }

        switch(mode){
            case MOVE:
                //Move command: units move to location without engaging
                Call.commandUnits(player, ids, null, null, new Vec2(worldX, worldY), queue, true, false);
                break;

            case PATROL:
                //Patrol command: units patrol between waypoints with patrol AI
                //Each click adds a patrol waypoint
                Call.commandUnits(player, ids, null, null, new Vec2(worldX, worldY), queue, true, false);
                break;

            case ATTACK:
                //Attack command: units move and attack
                //Check if clicking on any unit or building, including allies when forced by attack command mode.
                Building build = world.buildWorld(worldX, worldY);
                Teamc attack = (build != null && build.within(worldX, worldY, build.hitSize() / 2f)) ? build : null;
                if(attack == null){
                    attack = selectedAnyUnit(worldX, worldY);
                }

                boolean followOnlySelection = selectedUnits.size > 0;
                for(Unit unit : selectedUnits){
                    if(unit == null || !unit.isValid() || !unit.type.followEnemyWhenUnarmed){
                        followOnlySelection = false;
                        break;
                    }
                }

                if(followOnlySelection){
                    if(attack != null){
                        Call.commandUnits(player, ids, attack instanceof Building b ? b : null, attack instanceof Unit u ? u : null, new Vec2(worldX, worldY), queue, true, false);
                    }else{
                        Call.commandUnits(player, ids, null, null, new Vec2(worldX, worldY), queue, true, false);
                    }
                    break;
                }

                if(attack != null){
                    Call.commandUnits(player, ids, attack instanceof Building b ? b : null, attack instanceof Unit u ? u : null, new Vec2(worldX, worldY), queue, true, true);
                }else{
                    Call.commandUnits(player, ids, null, null, new Vec2(worldX, worldY), queue, true, true);
                }
                break;
        }
    }

    @Override
    public boolean multiUnitSelect(){
        //Shift key enables additive selection
        return Core.input.keyDown(KeyCode.shiftLeft) || Core.input.keyDown(KeyCode.shiftRight);
    }

    @Override
    public boolean touchDown(float x, float y, int pointer, KeyCode button){
        if(scene.hasMouse() || !commandMode) return false;

        //If in active RTS command mode, handle left-click on press (not release)
        if(ui.hudfrag.abilityPanel != null && ui.hudfrag.abilityPanel.activeCommand != mindustry.ui.UnitAbilityPanel.CommandMode.NONE){
            if(button == KeyCode.mouseLeft){
                //Execute command immediately on mouse press
                abilityTargetConsumeMillis = Time.millis();
                executeActiveCommand(x, y);
                return true;
            }
            //Right-click cancels command mode
            if(button == KeyCode.mouseRight){
                ui.hudfrag.abilityPanel.exitCommandMode();
                return true;
            }
            return false;
        }

        if(button == KeyCode.mouseRight){
            if(commandFocusGuardTime > 0f) return true;
            //Check if Shift is held for waypoint queuing
            boolean shiftHeld = Core.input.keyDown(KeyCode.shiftLeft) || Core.input.keyDown(KeyCode.shiftRight);
            if(shiftHeld && (selectedUnits.size > 0 || commandBuildings.size > 0)){
                commandTap(x, y, true);
            }else{
                commandTap(x, y);
            }
        }

        if(button == Binding.commandQueue.value.key){
            if(commandFocusGuardTime <= 0f){
                commandTap(x, y, true);
            }
        }

        return super.touchDown(x, y, pointer, button);
    }

    @Override
    public boolean selectedBlock(){
        return isPlacing() && mode != breaking;
    }

    @Override
    public float getMouseX(){
        return Core.input.mouseX();
    }

    @Override
    public float getMouseY(){
        return Core.input.mouseY();
    }

    @Override
    public void updateState(){
        super.updateState();

        if(state.isMenu()){
            lastSchematic = null;
            droppingItem = false;
            mode = none;
            block = null;
            splan = null;
            selectPlans.clear();
        }
    }

    @Override
    public void panCamera(Vec2 position){
        if(!locked()){
            panning = true;
            camera.position.set(position);
        }
    }

    protected void updateMovement(Unit unit){
        boolean omni = unit.type.omniMovement;

        float speed = unit.speed();
        float xa = Core.input.axis(Binding.moveX);
        float ya = Core.input.axis(Binding.moveY);
        boolean boosted = (unit instanceof Mechc && unit.isFlying());

        if(settings.getBool("detach-camera")){
            Vec2 targetPos = camera.position;

            movement.set(targetPos).sub(player).limit(speed);

            if(player.within(targetPos, 15f)){
                movement.setZero();
                unit.vel.approachDelta(Vec2.ZERO, unit.speed() * unit.type().accel / 2f);
            }
        }else{
            movement.set(xa, ya).nor().scl(speed);
            if(Core.input.keyDown(Binding.mouseMove)){
                movement.add(input.mouseWorld().sub(player).scl(1f / 25f * speed)).limit(speed);
            }
        }

        float mouseAngle = Angles.mouseAngle(unit.x, unit.y);
        boolean aimCursor = omni && player.shooting && unit.type.hasWeapons() && unit.type.faceTarget && !boosted;

        if(aimCursor){
            unit.lookAt(mouseAngle);
        }else{
            unit.lookAt(unit.prefRotation());
        }

        unit.movePref(movement);

        unit.aim(Core.input.mouseWorld());
        unit.controlWeapons(true, player.shooting && !boosted);

        player.boosting = Core.input.keyDown(Binding.boost);
        player.mouseX = unit.aimX();
        player.mouseY = unit.aimY();

        //update payload input
        if(unit instanceof Payloadc){
            if(Core.input.keyTap(Binding.pickupCargo)){
                tryPickupPayload();
                lastPayloadKeyTapMillis = Time.millis();
            }

            if(Core.input.keyDown(Binding.pickupCargo)
            && Time.timeSinceMillis(lastPayloadKeyHoldMillis) > 20
            && Time.timeSinceMillis(lastPayloadKeyTapMillis) > 200){
                tryPickupPayload();
                lastPayloadKeyHoldMillis = Time.millis();
            }

            if(Core.input.keyTap(Binding.dropCargo)){
                tryDropPayload();
                lastPayloadKeyTapMillis = Time.millis();
            }

            if(Core.input.keyDown(Binding.dropCargo)
            && Time.timeSinceMillis(lastPayloadKeyHoldMillis) > 20
            && Time.timeSinceMillis(lastPayloadKeyTapMillis) > 200){
                tryDropPayload();
                lastPayloadKeyHoldMillis = Time.millis();
            }
        }
    }
}
