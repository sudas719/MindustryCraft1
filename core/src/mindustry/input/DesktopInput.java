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
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.defense.*;
import mindustry.world.blocks.environment.*;
import mindustry.world.blocks.payloads.*;
import mindustry.world.blocks.storage.*;

import java.util.*;

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
    private static boolean windowFocusInit = false;
    private static boolean windowFocusSupported = false;
    private static Class<?> sdlAppClass;
    private static java.lang.reflect.Field sdlWindowField;
    private static java.lang.reflect.Method sdlGetWindowFlagsMethod;
    private static long sdlWindowFocusFlag;

    private static Class<?> cursorWarpInputClass;
    private static java.lang.reflect.Method cursorWarpMethod;
    private static Class<?> cursorCatchInputClass;
    private static java.lang.reflect.Method cursorCatchMethod;
    private static Class<?> cursorDeltaInputClass;
    private static java.lang.reflect.Method cursorDeltaXMethod;
    private static java.lang.reflect.Method cursorDeltaYMethod;
    private static Class<?> cursorWarpGraphicsClass;
    private static java.lang.reflect.Method cursorWarpGraphicsMethod;
    private static Class<?> cursorCatchGraphicsClass;
    private static java.lang.reflect.Method cursorCatchGraphicsMethod;
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
    private boolean spectatorHighView = false;
    private float spectatorBaseScale = -1f;

    private boolean middleMousePanning = false;
    private int middleMouseStartX, middleMouseStartY;
    private int middleMouseLastX, middleMouseLastY;
    private boolean middleMouseCaptured = false;
    private Cursor blankCursor;

    private boolean abilityTargetingActive(){
        return ui.hudfrag.abilityPanel != null && ui.hudfrag.abilityPanel.activeCommand != mindustry.ui.UnitAbilityPanel.CommandMode.NONE;
    }

    public boolean isMiddleMousePanning(){
        return middleMousePanning;
    }

    private Cursor blankCursor(){
        if(blankCursor == null){
            blankCursor = Core.graphics.newCursor("blank", Fonts.cursorScale());
        }
        return blankCursor;
    }

    private static java.lang.reflect.Method findCursorCatchMethod(Class<?> type){
        for(Class<?> c = type; c != null; c = c.getSuperclass()){
            for(var method : c.getDeclaredMethods()){
                var params = method.getParameterTypes();
                if(params.length != 1 || params[0] != boolean.class) continue;

                String name = method.getName().toLowerCase(Locale.ROOT);
                if(!(name.contains("cursor") || name.contains("mouse"))) continue;
                if(name.contains("catch") || name.contains("captur") || name.contains("grab") || name.contains("lock") || name.contains("relative")){
                    try{
                        method.setAccessible(true);
                    }catch(Throwable ignored){
                    }
                    return method;
                }
            }
        }
        return null;
    }

    private static boolean setCursorCatched(boolean catched){
        boolean invoked = false;

        if(Core.input != null){
            Class<?> inputClass = Core.input.getClass();
            if(cursorCatchInputClass != inputClass){
                cursorCatchInputClass = inputClass;
                cursorCatchMethod = null;

                try{
                    cursorCatchMethod = inputClass.getMethod("setCursorCatched", boolean.class);
                }catch(Throwable ignored){
                    try{
                        cursorCatchMethod = inputClass.getMethod("setCursorCaptured", boolean.class);
                    }catch(Throwable ignored2){
                        cursorCatchMethod = findCursorCatchMethod(inputClass);
                    }
                }
            }

            if(cursorCatchMethod != null){
                try{
                    cursorCatchMethod.invoke(Core.input, catched);
                    invoked = true;
                }catch(Throwable ignored){
                }
            }
        }

        if(!invoked && Core.graphics != null){
            Class<?> graphicsClass = Core.graphics.getClass();
            if(cursorCatchGraphicsClass != graphicsClass){
                cursorCatchGraphicsClass = graphicsClass;
                cursorCatchGraphicsMethod = findCursorCatchMethod(graphicsClass);
            }

            if(cursorCatchGraphicsMethod != null){
                try{
                    cursorCatchGraphicsMethod.invoke(Core.graphics, catched);
                    invoked = true;
                }catch(Throwable ignored){
                }
            }
        }

        return invoked;
    }

    private static java.lang.reflect.Method findCursorDeltaMethod(Class<?> inputClass, String suffix){
        for(var method : inputClass.getMethods()){
            if(method.getParameterTypes().length != 0) continue;

            Class<?> ret = method.getReturnType();
            if(ret != float.class && ret != int.class && ret != double.class) continue;

            String name = method.getName().toLowerCase(Locale.ROOT);
            if(name.endsWith(suffix) || name.contains("delt" + suffix) || name.contains("mouse" + suffix) || name.contains("cursor" + suffix)){
                return method;
            }
        }
        return null;
    }

    private static void resolveCursorDeltas(){
        if(Core.input == null) return;

        Class<?> inputClass = Core.input.getClass();
        if(cursorDeltaInputClass == inputClass) return;

        cursorDeltaInputClass = inputClass;
        cursorDeltaXMethod = null;
        cursorDeltaYMethod = null;

        try{
            cursorDeltaXMethod = inputClass.getMethod("deltaX");
            cursorDeltaYMethod = inputClass.getMethod("deltaY");
        }catch(Throwable ignored){
            cursorDeltaXMethod = findCursorDeltaMethod(inputClass, "x");
            cursorDeltaYMethod = findCursorDeltaMethod(inputClass, "y");
        }
    }

    private static java.lang.reflect.Method findCursorWarpMethod(Class<?> type){
        for(Class<?> c = type; c != null; c = c.getSuperclass()){
            for(var method : c.getDeclaredMethods()){
                String name = method.getName().toLowerCase(Locale.ROOT);
                if(!name.startsWith("set")) continue;
                if(!(name.contains("cursor") || name.contains("mouse"))) continue;

                var params = method.getParameterTypes();
                if(params.length != 2) continue;
                if((params[0] == int.class && params[1] == int.class) || (params[0] == float.class && params[1] == float.class)){
                    try{
                        method.setAccessible(true);
                    }catch(Throwable ignored){
                    }
                    return method;
                }
            }
        }
        return null;
    }

    private static boolean setCursorPosition(int x, int y){
        boolean invoked = false;

        if(Core.input != null){
            Class<?> inputClass = Core.input.getClass();
            if(cursorWarpInputClass != inputClass){
                cursorWarpInputClass = inputClass;
                cursorWarpMethod = null;

                try{
                    cursorWarpMethod = inputClass.getMethod("setCursorPosition", int.class, int.class);
                }catch(Throwable ignored){
                    try{
                        cursorWarpMethod = inputClass.getMethod("setMousePosition", int.class, int.class);
                    }catch(Throwable ignored2){
                        try{
                            cursorWarpMethod = inputClass.getMethod("setCursorPosition", float.class, float.class);
                        }catch(Throwable ignored3){
                            try{
                                cursorWarpMethod = inputClass.getMethod("setMousePosition", float.class, float.class);
                            }catch(Throwable ignored4){
                                cursorWarpMethod = findCursorWarpMethod(inputClass);
                            }
                        }
                    }
                }
            }

            if(cursorWarpMethod != null){
                try{
                    if(cursorWarpMethod.getParameterTypes()[0] == int.class){
                        cursorWarpMethod.invoke(Core.input, x, y);
                    }else{
                        cursorWarpMethod.invoke(Core.input, (float)x, (float)y);
                    }
                    invoked = true;
                }catch(Throwable ignored){
                }
            }
        }

        if(!invoked && Core.graphics != null){
            Class<?> graphicsClass = Core.graphics.getClass();
            if(cursorWarpGraphicsClass != graphicsClass){
                cursorWarpGraphicsClass = graphicsClass;
                cursorWarpGraphicsMethod = findCursorWarpMethod(graphicsClass);
            }

            if(cursorWarpGraphicsMethod != null){
                try{
                    if(cursorWarpGraphicsMethod.getParameterTypes()[0] == int.class){
                        cursorWarpGraphicsMethod.invoke(Core.graphics, x, y);
                    }else{
                        cursorWarpGraphicsMethod.invoke(Core.graphics, (float)x, (float)y);
                    }
                    invoked = true;
                }catch(Throwable ignored){
                }
            }
        }

        return invoked;
    }

    private boolean suppressSelectionTap(){
        return abilityTargetConsumeMillis > 0L && Time.timeSinceMillis(abilityTargetConsumeMillis) <= 200L;
    }

    private boolean isLocalSpectatorMode(){
        if(player == null || player.team() == null || state == null || state.isMenu()) return false;
        if(!net.active()) return false;
        Team team = player.team();
        return team == Team.derelict || !team.data().isAlive();
    }

    private void restoreSpectatorViewScale(){
        if(spectatorHighView){
            renderer.clearSpectatorMaxVisibleTiles();
            if(spectatorBaseScale > 0f){
                renderer.setScale(spectatorBaseScale);
            }
        }
        spectatorHighView = false;
        spectatorBaseScale = -1f;
    }

    private static boolean windowFocused(){
        if(Core.graphics.isHidden()) return false;
        if(!windowFocusInit){
            windowFocusInit = true;
            try{
                sdlAppClass = Class.forName("arc.backend.sdl.SdlApplication");
                if(sdlAppClass.isInstance(Core.app)){
                    sdlWindowField = sdlAppClass.getDeclaredField("window");
                    sdlWindowField.setAccessible(true);
                    windowFocusSupported = false;

                    //SDL3 (LWJGL) backend
                    try{
                        Class<?> sdlClass = Class.forName("org.lwjgl.sdl.SDLVideo");
                        sdlGetWindowFlagsMethod = sdlClass.getMethod("SDL_GetWindowFlags", long.class);
                        Object value = sdlClass.getField("SDL_WINDOW_INPUT_FOCUS").get(null);
                        sdlWindowFocusFlag = value instanceof Number ? ((Number)value).longValue() : 0L;
                        windowFocusSupported = true;
                    }catch(Throwable ignored){
                        //SDL2 (JNI) backend
                        try{
                            Class<?> sdlClass = Class.forName("arc.backend.sdl.jni.SDL");
                            sdlGetWindowFlagsMethod = sdlClass.getMethod("SDL_GetWindowFlags", long.class);
                            Object value = sdlClass.getField("SDL_WINDOW_INPUT_FOCUS").get(null);
                            sdlWindowFocusFlag = value instanceof Number ? ((Number)value).longValue() : 0L;
                            windowFocusSupported = true;
                        }catch(Throwable ignored2){
                            windowFocusSupported = false;
                        }
                    }
                }else{
                    windowFocusSupported = false;
                }
            }catch(Throwable t){
                windowFocusSupported = false;
            }
        }

        if(!windowFocusSupported) return true;
        try{
            if(!sdlAppClass.isInstance(Core.app)) return true;
            long window = sdlWindowField.getLong(Core.app);
            if(window == 0L) return true;
            Object result = sdlGetWindowFlagsMethod.invoke(null, window);
            long flags = result instanceof Number ? ((Number)result).longValue() : 0L;
            return (flags & sdlWindowFocusFlag) != 0L;
        }catch(Throwable t){
            return true;
        }
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
        int cursorX = tileX(Core.input.mouseX(), Core.input.mouseY());
        int cursorY = tileY(Core.input.mouseX(), Core.input.mouseY());

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
            float wx = clampCommandX(mouseWorldX());
            float wy = clampCommandY(mouseWorldY());
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
        int cursorX = tileX(Core.input.mouseX(), Core.input.mouseY());
        int cursorY = tileY(Core.input.mouseX(), Core.input.mouseY());
        if(isPlacing() && block == Blocks.ventCondenser){
            Tile snap = findNearestVentCenter(mouseWorldX(), mouseWorldY(), 30);
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
                    int placeX = World.toTile(mouseWorldX() - offset);
                    int placeY = World.toTile(mouseWorldY() - offset);
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
                    Tile snap = findNearestVentCenter(mouseWorldX(), mouseWorldY(), 30);
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

        if(middleMousePanning){
            Core.graphics.cursor(blankCursor());
        }

        if(!isLocalSpectatorMode()){
            restoreSpectatorViewScale();
        }

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

        if(!ui.chatfrag.shown() && !scene.hasField() && !scene.hasDialog() && Core.input.keyTap(KeyCode.tab)){
            if(!selectedUnits.isEmpty()){
                boolean forward = !(Core.input.keyDown(KeyCode.shiftLeft) || Core.input.keyDown(KeyCode.shiftRight));
                if(cycleAbilitySubgroup(forward)){
                    Events.fire(Trigger.unitCommandChange);
                }
            }
        }

        boolean locked = locked();
        boolean panCam = false;
        float camSpeed = (!Core.input.keyDown(Binding.boost) ? panSpeed : panBoostSpeed) * Time.delta;
        boolean detached = settings.getBool("detach-camera", false);
        float arrowCamX = 0f, arrowCamY = 0f;
        boolean arrowCam = false;
        boolean middlePan = false;

        if(middleMousePanning && !Core.input.keyDown(KeyCode.mouseMiddle)){
            middleMousePanning = false;
            if(middleMouseCaptured){
                setCursorCatched(false);
                middleMouseCaptured = false;
            }
            setCursorPosition(middleMouseStartX, middleMouseStartY);
            Core.graphics.restoreCursor();
        }

        if(!scene.hasField() && !scene.hasDialog()){
            boolean midDown = Core.input.keyDown(KeyCode.mouseMiddle);
            if(midDown && !middleMousePanning && !ui.chatfrag.shown()){
                middleMousePanning = true;
                middleMouseStartX = (int)Core.input.mouseX();
                middleMouseStartY = (int)Core.input.mouseY();
                middleMouseLastX = middleMouseStartX;
                middleMouseLastY = middleMouseStartY;

                //lock/hide cursor + use relative deltas when supported (prevents edge clamping)
                middleMouseCaptured = setCursorCatched(true);
            }else if(!midDown && middleMousePanning){
                middleMousePanning = false;
                if(middleMouseCaptured){
                    setCursorCatched(false);
                    middleMouseCaptured = false;
                }
                setCursorPosition(middleMouseStartX, middleMouseStartY);
                Core.graphics.restoreCursor();
            }

            if(middleMousePanning){
                middlePan = true;
                panning = true;
                spectating = null;
                spectatingPlayer = -1;

                //ensure cursor stays hidden/locked even if some other UI code changes it
                if(middleMouseCaptured){
                    setCursorCatched(true);
                }

                int mx = (int)Core.input.mouseX();
                int my = (int)Core.input.mouseY();
                int dx = 0, dy = 0;

                if(middleMouseCaptured){
                    dx = Core.input.deltaX();
                    dy = Core.input.deltaY();
                }else{
                    //fallback: no capture, use raw movement (will still hit screen edges)
                    dx = mx - middleMouseLastX;
                    dy = my - middleMouseLastY;
                }

                if(dx != 0 || dy != 0){
                    float sx = Core.graphics.getWidth() <= 0 ? 0f : (Core.camera.width / Core.graphics.getWidth());
                    float sy = Core.graphics.getHeight() <= 0 ? 0f : (Core.camera.height / Core.graphics.getHeight());
                    Core.camera.position.x += dx * sx;
                    Core.camera.position.y += dy * sy;
                }

                if(!middleMouseCaptured){
                    //keep cursor position unchanged if possible
                    if(setCursorPosition(middleMouseStartX, middleMouseStartY)){
                        middleMouseLastX = middleMouseStartX;
                        middleMouseLastY = middleMouseStartY;
                    }else{
                        middleMouseLastX = mx;
                        middleMouseLastY = my;
                    }
                }else{
                    //cursor is captured, OS cursor position does not matter
                }
            }

            if(input.keyTap(Binding.debugHitboxes)){
                drawDebugHitboxes = !drawDebugHitboxes;
            }

            if(input.keyTap(Binding.detachCamera)){
                settings.put("detach-camera", detached = !detached);
                if(!detached){
                    panning = false;
                }
                spectating = null;
                spectatingPlayer = -1;
            }

            if(!middlePan && input.keyDown(Binding.pan)){
                panCam = true;
                panning = true;
                spectating = null;
                spectatingPlayer = -1;
            }

            if((Math.abs(Core.input.axis(Binding.moveX)) > 0 || Math.abs(Core.input.axis(Binding.moveY)) > 0 || input.keyDown(Binding.mouseMove))){
                panning = false;
                spectating = null;
                spectatingPlayer = -1;
            }

            if(!ui.chatfrag.shown()){
                if(Core.input.keyDown(KeyCode.left)) arrowCamX -= 1f;
                if(Core.input.keyDown(KeyCode.right)) arrowCamX += 1f;
                if(Core.input.keyDown(KeyCode.up)) arrowCamY += 1f;
                if(Core.input.keyDown(KeyCode.down)) arrowCamY -= 1f;
                arrowCam = !middlePan && (arrowCamX != 0f || arrowCamY != 0f);
                if(arrowCam){
                    panning = true;
                    spectating = null;
                    spectatingPlayer = -1;
                }
            }
        }

        panning |= detached;


        if(!locked){
            if(((player.dead() || state.isPaused() || detached) && !ui.chatfrag.shown()) && !scene.hasField() && !scene.hasDialog()){
                if(input.keyDown(Binding.mouseMove)){
                    panCam = true;
                }

                rotateCameraMove(Tmp.v1.setZero().add(Core.input.axis(Binding.moveX), Core.input.axis(Binding.moveY)).nor().scl(camSpeed));
                Core.camera.position.add(Tmp.v1);
            }

            if(arrowCam && !scene.hasField() && !scene.hasDialog() && !ui.chatfrag.shown()){
                rotateCameraMove(Tmp.v1.set(arrowCamX, arrowCamY).nor().scl(camSpeed));
                Core.camera.position.add(Tmp.v1);
            }else if((!player.dead() || spectating != null || spectatingPlayer() != null) && !panning){
                //TODO do not pan
                Team corePanTeam = state.won ? state.rules.waveTeam : player.team();
                Position coreTarget = state.gameOver && !state.rules.pvp && corePanTeam.data().lastCore != null ? corePanTeam.data().lastCore : null;
                Player spectatePlayer = spectatingPlayer();
                Position panTarget = coreTarget != null ? coreTarget : spectating != null ? spectating : spectatePlayer != null ? spectatePlayer : player;

                Core.camera.position.lerpDelta(panTarget, Core.settings.getBool("smoothcamera") ? 0.08f : 1f);
            }

            if(panCam && !middlePan){
                float panX = Mathf.clamp((Core.input.mouseX() - Core.graphics.getWidth() / 2f) * panScale, -1, 1) * camSpeed;
                float panY = Mathf.clamp((Core.input.mouseY() - renderer.getGameScreenCenterYPx()) * panScale, -1, 1) * camSpeed;
                rotateCameraMove(Tmp.v1.set(panX, panY));
                Core.camera.position.add(Tmp.v1);
            }

            //edge scrolling
            if(!middlePan && Core.settings.getBool("edgescrolling") && windowFocused() && !scene.hasDialog() && !scene.hasField()){
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
                    rotateCameraMove(Tmp.v1.set(edgeScrollX, edgeScrollY));
                    Core.camera.position.add(Tmp.v1);
                    edgeScrolling = true;
                }else{
                    edgeScrolling = false;
                }
            }else{
                edgeScrollX = 0f;
                edgeScrollY = 0f;
                edgeScrolling = false;
            }
        }

        if(player.dead()){
            player.set(Core.camera.position.x, Core.camera.position.y);
        }

        shouldShoot = !scene.hasMouse() && !locked && !state.isEditor();

        //Command mode is always enabled - no toggle needed
        commandMode = true;

        //validate commanding units
        selectedUnits.removeAll(u -> !u.allowCommand() || !u.isValid() || u.team != player.team());
        restorePreservedUnitSelection();

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
                    boolean hasSelection = !selectedUnits.isEmpty() || !commandBuildings.isEmpty();
                    if(adding && hasSelection && group.isEmpty()){
                        //Shift+number on an empty group should behave like creating a new group.
                        creating = true;
                        adding = false;
                    }
                    IntSeq selectedUnitIds = null;
                    IntSeq selectedBuildingPos = null;
                    IntSeq selectedBuildingIds = null;
                    if(creating || adding){
                        selectedUnitIds = selectedUnits.mapInt(u -> u.id);
                        selectedBuildingPos = commandBuildings.mapInt(b -> b.pos());
                        selectedBuildingIds = commandBuildings.mapInt(b -> b.id);
                        if(selectedUnitIds.isEmpty() && selectedBuildingPos.isEmpty()){
                            Building hoverBuild = buildAt(mouseWorldX(), mouseWorldY());
                            if(hoverBuild != null && hoverBuild.team == player.team()){
                                selectedBuildingPos.add(hoverBuild.pos());
                                selectedBuildingIds.add(hoverBuild.id);
                            }
                        }
                    }

                    //clear existing if making a new control group
                    //if any of the control group edit buttons are pressed take the current selection
                    if(creating){
                        if(selectedUnitIds.isEmpty() && selectedBuildingPos.isEmpty()){
                            continue;
                        }
                        group.clear();

                        if(Core.settings.getBool("distinctcontrolgroups", true)){
                            for(IntSeq cg : controlGroups){
                                if(cg != null){
                                    cg.removeAll(selectedUnitIds);
                                    cg.removeAll(selectedBuildingPos);
                                    cg.removeAll(selectedBuildingIds);
                                }
                            }
                        }
                        group.addAll(selectedUnitIds);
                        group.addAll(selectedBuildingPos);
                    }else if(adding){
                        //Shift+number: Add currently selected units to this formation
                        if(selectedUnitIds.isEmpty() && selectedBuildingPos.isEmpty()){
                            continue;
                        }

                        if(Core.settings.getBool("distinctcontrolgroups", true)){
                            for(IntSeq cg : controlGroups){
                                if(cg != null && cg != group){
                                    cg.removeAll(selectedUnitIds);
                                    cg.removeAll(selectedBuildingPos);
                                    cg.removeAll(selectedBuildingIds);
                                }
                            }
                        }

                        group.addAll(selectedUnitIds);
                        group.addAll(selectedBuildingPos);
                    }

                    //remove invalid units and buildings
                    for(int j = 0; j < group.size; j++){
                        int id = group.get(j);
                        Unit u = Groups.unit.getByID(id);
                        Building b = null;

                        //Buildings don't have ID mapping, search manually
                        if(u == null){
                            b = world.build(id);
                            if(b == null){
                                for(Building building : Groups.build){
                                    if(building.id == id){
                                        b = building;
                                        break;
                                    }
                                }
                            }
                        }

                        if((u == null || !u.isValid()) && (b == null || !b.isValid())){
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
                                building = world.build(id);
                                if(building == null){
                                    for(Building b : Groups.build){
                                        if(b.id == id){
                                            building = b;
                                            break;
                                        }
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

            if(!ui.chatfrag.shown() && !ui.consolefrag.shown() && Core.input.keyTap(KeyCode.z) && isLocalSpectatorMode()){
                if(!spectatorHighView){
                    spectatorBaseScale = renderer.getScale();
                    float targetTiles = 70f;
                    renderer.setSpectatorMaxVisibleTiles(targetTiles);
                    float screenWidth = Core.graphics.getWidth();
                    float targetScale = screenWidth <= 0 ? spectatorBaseScale / 1.5f : screenWidth / (targetTiles * tilesize);
                    renderer.setScale(targetScale);
                    spectatorHighView = true;
                }else{
                    restoreSpectatorViewScale();
                }
            }

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
            Tile selected = world.tileWorld(mouseWorldX(), mouseWorldY());
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

        if(middleMousePanning){
            cursorType = blankCursor();
        }

        if(Core.input.keyRelease(Binding.select)){
            player.shooting = false;
        }

        boolean hoverCursor = cursorType == ui.hoverGreenCursor || cursorType == ui.hoverRedCursor || cursorType == ui.hoverYellowCursor;
        if(middleMousePanning){
            Core.graphics.cursor(blankCursor());
            changedCursor = true;
        }else if((!Core.scene.hasMouse() || hoverCursor) && !ui.minimapfrag.shown()){
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
        schematicX = tileX(getMouseX(), getMouseY());
        schematicY = tileY(getMouseX(), getMouseY());

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
                commandRectX = mouseWorldX();
                commandRectY = mouseWorldY();
                commandRectScreenX = getMouseX();
                commandRectScreenY = getMouseY();
            }else if(selected != null){
                tileTapped(selected.build);
            }
        }
    }

    //player input: for controlling the player unit (will crash if the unit is not present)
    void pollInputPlayer(){
        if(scene.hasField()) return;

        Tile selected = tileAt(Core.input.mouseX(), Core.input.mouseY());
        int cursorX = tileX(Core.input.mouseX(), Core.input.mouseY());
        int cursorY = tileY(Core.input.mouseX(), Core.input.mouseY());
        int rawCursorX = World.toTile(mouseWorld().x), rawCursorY = World.toTile(mouseWorld().y);
        if(isPlacing() && block == Blocks.ventCondenser){
            Tile snap = findNearestVentCenter(mouseWorldX(), mouseWorldY(), 30);
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
            int x = Math.round((mouseWorld().x + buildPlanMouseOffsetX) / tilesize);
            int y = Math.round((mouseWorld().y + buildPlanMouseOffsetY) / tilesize);
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
                    buildPlanMouseOffsetX = splan.x * tilesize - mouseWorld().x;
                    buildPlanMouseOffsetY = splan.y * tilesize - mouseWorld().y;
                }else if(plan != null && plan.breaking){
                    deleting = true;
                }else if(commandMode && ui.hudfrag.abilityPanel != null && ui.hudfrag.abilityPanel.activeCommand == mindustry.ui.UnitAbilityPanel.CommandMode.NONE){
                    //Only allow box selection if NOT in an active RTS command mode
                    commandRect = true;
                    commandRectX = mouseWorldX();
                    commandRectY = mouseWorldY();
                    commandRectScreenX = getMouseX();
                    commandRectScreenY = getMouseY();
                }else if(!checkConfigTap() && selected != null && !tryRepairDerelict(selected)){
                    if(trySelectResource(selected)){
                        //resource selection consumes the tap
                    }else{
                        selectedResource = null;
                        //only begin shooting if there's no cursor event
                        if(!tryTapPlayer(mouseWorld().x, mouseWorld().y) && !tileTapped(selected.build) && !player.unit().activelyBuilding() && !droppingItem
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
            selectX = tileX(Core.input.mouseX(), Core.input.mouseY());
            selectY = tileY(Core.input.mouseX(), Core.input.mouseY());
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

            tryDropItems(selected == null ? null : selected.build, mouseWorld().x, mouseWorld().y);

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

            if(canTapPlayer(mouseWorld().x, mouseWorld().y)){
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
        || mode == mindustry.ui.UnitAbilityPanel.CommandMode.REAPER_KD8
        || mode == mindustry.ui.UnitAbilityPanel.CommandMode.RAVEN_ANTI_ARMOR
        || mode == mindustry.ui.UnitAbilityPanel.CommandMode.RAVEN_MATRIX
        || mode == mindustry.ui.UnitAbilityPanel.CommandMode.BUNKER_ATTACK
        || mode == mindustry.ui.UnitAbilityPanel.CommandMode.BUNKER_LOAD
        || mode == mindustry.ui.UnitAbilityPanel.CommandMode.HARVEST
        || mode == mindustry.ui.UnitAbilityPanel.CommandMode.REPAIR
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

        screenY = clampScreenY(screenY);
        var mode = ui.hudfrag.abilityPanel.activeCommand;
        Vec2 world = mouseWorld(screenX, screenY);
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

        if(mode == mindustry.ui.UnitAbilityPanel.CommandMode.REPAIR){
            if(executeNovaRepairCommand(worldX, worldY, shiftHeld) && !shiftHeld){
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
            if(executeMedivacHealCommand(worldX, worldY, shiftHeld) && !shiftHeld){
                ui.hudfrag.abilityPanel.exitCommandMode();
            }
            return;
        }

        if(mode == mindustry.ui.UnitAbilityPanel.CommandMode.MEDIVAC_LOAD){
            if(executeMedivacLoadCommand(worldX, worldY, shiftHeld) && !shiftHeld){
                ui.hudfrag.abilityPanel.exitCommandMode();
            }
            return;
        }

        if(mode == mindustry.ui.UnitAbilityPanel.CommandMode.MEDIVAC_UNLOAD){
            if(executeMedivacUnloadCommand(worldX, worldY, shiftHeld) && !shiftHeld){
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

        if(mode == mindustry.ui.UnitAbilityPanel.CommandMode.REAPER_KD8){
            if(executeReaperKd8Command(worldX, worldY) && !shiftHeld){
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

        if(mode == mindustry.ui.UnitAbilityPanel.CommandMode.RAVEN_TURRET){
            if(executeRavenTurretCommand(worldX, worldY) && !shiftHeld){
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
            executeBuildPlacement(worldX, worldY, shiftHeld);
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

    private boolean executeNovaRepairCommand(float worldX, float worldY, boolean queue){
        if(selectedUnits.isEmpty()) return false;

        Building build = world.buildWorld(worldX, worldY);
        if(build instanceof mindustry.world.blocks.ConstructBlock.ConstructBuild construct && construct.team == player.team()){
            Block cur = construct.current;
            if(cur == null || cur == Blocks.air) return false;

            int tx = construct.tile.x, ty = construct.tile.y;
            Unit chosen = pickScvBuildUnit(queue, true);
            if(chosen == null) return false;
            BuildPlan plan = new BuildPlan(tx, ty, construct.rotation, cur, cur.saveConfig ? construct.lastConfig : null);
            plan.requireClose = true;
            chosen.addBuild(plan);
            chosen.updateBuilding(true);

            float targetX = tx * tilesize + cur.offset;
            float targetY = ty * tilesize + cur.offset;
            Call.commandUnits(player, new int[]{chosen.id}, null, null, new Vec2(targetX, targetY), queue, true, false);
            return true;
        }

        Teamc target = (build != null && build.team == player.team() && build.health < build.maxHealth() - 0.001f) ? build : null;
        if(target == null){
            Unit unit = selectedAnyUnit(worldX, worldY);
            if(unit != null && unit.team == player.team() && !selectedUnits.contains(unit) && unit.type.unitClasses.contains(UnitClass.mechanical) && unit.health < unit.maxHealth() - 0.001f){
                target = unit;
            }
        }

        if(target == null) return false;

        IntSeq idsSeq = new IntSeq();
        for(Unit unit : selectedUnits){
            if(unit == null || !unit.isValid() || unit.type != UnitTypes.nova) continue;
            idsSeq.add(unit.id);
        }
        if(idsSeq.isEmpty()) return false;

        int[] ids = idsSeq.toArray();
        Call.commandUnits(player, ids, target instanceof Building b ? b : null, target instanceof Unit u ? u : null,
            new Vec2(target.getX(), target.getY()), queue, true, false);
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
        int targetPos = build.pos();
        Team targetTeam = build.team;
        float fxX = build.x, fxY = build.y;

        int start = nextOrbitalCoreStartIndex(cores);
        for(int i = 0; i < cores.size; i++){
            CoreBlock.CoreBuild core = cores.get((start + i) % cores.size);
            if(!core.consumeOrbitalEnergy(CoreBlock.orbitalAbilityCost)) continue;
            lastOrbitalCoreId = core.id;

            Fx.sc2DropPod.at(fxX, fxY);
            Time.run(3f * 60f, () -> {
                Building current = world.build(targetPos);
                if(current == null || !current.isValid() || current.team != targetTeam) return;
                if(!(current instanceof Door.DoorBuild)) return;
                if(current.block != Blocks.doorLarge && current.block != Blocks.doorLargeErekir) return;

                applyExtraSupply(current.tile);
                Fx.launchPod.at(current.x, current.y);
            });
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

    private @Nullable Unit selectSingleUnit(Boolf<Unit> filter, float worldX, float worldY){
        Unit best = null;
        float bestDst = Float.MAX_VALUE;
        for(Unit unit : selectedUnits){
            if(unit == null || !unit.isValid()) continue;
            if(filter != null && !filter.get(unit)) continue;
            float dst = unit.dst2(worldX, worldY);
            if(dst < bestDst){
                bestDst = dst;
                best = unit;
            }
        }
        return best;
    }

    private boolean executeLiberatorZoneCommand(float worldX, float worldY){
        Unit chosen = selectSingleUnit(u -> UnitTypes.isLiberator(u) && UnitTypes.liberatorCanEnterDefense(u), worldX, worldY);
        if(chosen == null) return false;
        Call.commandLiberatorMode(player, new int[]{chosen.id}, true, new Vec2(worldX, worldY));
        return true;
    }

    private boolean executeMedivacHealCommand(float worldX, float worldY, boolean queue){
        Unit target = selectedAnyUnit(worldX, worldY);
        if(!UnitTypes.medivacCanHealTarget(target, player.team())) return false;

        int[] ids = selectedMedivacIds(u -> true);
        if(ids.length == 0) return false;

        Call.setUnitCommand(player, ids, UnitCommand.moveCommand);
        Call.commandMedivacMovingUnload(player, ids, false);
        Call.commandUnits(player, ids, null, target, new Vec2(target.x, target.y), queue, true, false);
        return true;
    }

    private boolean executeMedivacLoadCommand(float worldX, float worldY, boolean queue){
        Unit target = selectedAnyUnit(worldX, worldY);
        if(target == null || target.team != player.team()) return false;

        int[] ids = selectedMedivacIds(u -> UnitTypes.medivacCanPickup(u, target));
        if(ids.length == 0) return false;

        Call.setUnitCommand(player, ids, UnitCommand.loadUnitsCommand);
        Call.commandMedivacMovingUnload(player, ids, false);
        Call.commandUnits(player, ids, null, target, new Vec2(target.x, target.y), queue, true, false);
        return true;
    }

    private boolean executeMedivacUnloadCommand(float worldX, float worldY, boolean queue){
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
        Call.commandUnits(player, ids, null, null, new Vec2(worldX, worldY), queue, true, false);
        return true;
    }

    private boolean executeRavenAntiArmorCommand(float worldX, float worldY){
        Unit chosen = selectSingleUnit(u -> UnitTypes.isRaven(u) && UnitTypes.ravenCanUseAntiArmor(u), worldX, worldY);
        if(chosen == null) return false;
        Call.commandAvertAntiArmor(player, new int[]{chosen.id}, new Vec2(worldX, worldY));
        return true;
    }

    private boolean executeRavenTurretCommand(float worldX, float worldY){
        Block block = Blocks.ravenTurret;
        if(block == null) return false;

        Tmp.v1.set(worldX, worldY).sub(block.offset, block.offset);
        int tx = World.toTile(Tmp.v1.x);
        int ty = World.toTile(Tmp.v1.y);
        if(tx < 0 || ty < 0 || tx >= world.width() || ty >= world.height()) return false;
        if(!Build.validPlaceIgnoreUnits(block, player.team(), tx, ty, 0, false, false) || !Build.checkNoUnitOverlap(block, tx, ty)){
            return false;
        }

        Unit chosen = selectSingleUnit(u -> UnitTypes.isRaven(u) && UnitTypes.ravenCanDeployTurret(u), worldX, worldY);
        if(chosen == null) return false;
        Call.commandAvertDeployTurret(player, new int[]{chosen.id}, new Vec2(worldX, worldY));
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

        Unit chosen = selectSingleUnit(u -> UnitTypes.isBattlecruiser(u) && UnitTypes.battlecruiserCanUseYamato(u), target.getX(), target.getY());
        if(chosen == null) return false;

        int targetId = target instanceof Unit u ? u.id : -1;
        int buildPos = target instanceof Building b ? b.pos() : -1;
        Call.commandBattlecruiserYamato(player, new int[]{chosen.id}, targetId, buildPos);
        return true;
    }

    private boolean executeBattlecruiserWarpCommand(float worldX, float worldY){
        int[] ids = selectedBattlecruiserIds(UnitTypes::battlecruiserCanUseWarp);
        if(ids.length == 0) return false;
        Call.commandBattlecruiserWarp(player, ids, new Vec2(worldX, worldY));
        return true;
    }

    private boolean executeGhostTacticalNukeCommand(float worldX, float worldY){
        Unit chosen = selectSingleUnit(u -> UnitTypes.isGhost(u) && UnitTypes.ghostCanUseTacticalNuke(u), worldX, worldY);
        if(chosen == null) return false;
        Call.commandGhostTacticalNuke(player, new int[]{chosen.id}, new Vec2(worldX, worldY));
        return true;
    }

    private boolean executeGhostStableAimCommand(float worldX, float worldY){
        Unit target = selectedAnyUnit(worldX, worldY);
        if(!UnitTypes.ghostStableAimValidTarget(target)) return false;
        Unit chosen = selectSingleUnit(u -> UnitTypes.isGhost(u) && UnitTypes.ghostCanUseStableAim(u), target.x, target.y);
        if(chosen == null) return false;
        Call.commandGhostStableAim(player, new int[]{chosen.id}, target.id);
        return true;
    }

    private boolean executeGhostEmpCommand(float worldX, float worldY){
        Unit chosen = selectSingleUnit(u -> UnitTypes.isGhost(u) && UnitTypes.ghostCanUseEmp(u), worldX, worldY);
        if(chosen == null) return false;
        Call.commandGhostEmp(player, new int[]{chosen.id}, new Vec2(worldX, worldY));
        return true;
    }

    private boolean executeRavenMatrixCommand(float worldX, float worldY){
        Unit target = selectedAnyUnit(worldX, worldY);
        if(target == null || !target.isValid()) return false;

        Unit chosen = selectSingleUnit(u -> UnitTypes.isRaven(u) && UnitTypes.ravenCanUseMatrix(u) && UnitTypes.ravenMatrixValidTarget(target, u.team), target.x, target.y);
        if(chosen == null) return false;
        Call.commandAvertMatrix(player, new int[]{chosen.id}, target.id);
        return true;
    }

    private boolean executeReaperKd8Command(float worldX, float worldY){
        Unit chosen = selectSingleUnit(u -> u.type == UnitTypes.reaper && UnitTypes.reaperCanUseKd8(u), worldX, worldY);
        if(chosen == null) return false;
        Call.commandReaperKd8(player, new int[]{chosen.id}, new Vec2(worldX, worldY));
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

    private void executeBuildPlacement(float worldX, float worldY, boolean shiftHeld){
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
        boolean hasScvBuilder = false;
        for(Unit unit : selectedUnits){
            if(unit == null || !unit.isValid() || !unit.canBuild()) continue;
            if(unit.type == UnitTypes.nova){
                hasScvBuilder = true;
                break;
            }
        }

        if(hasScvBuilder){
            chosen = pickScvBuildUnit(shiftHeld, false);
        }else{
            float bestDst = Float.MAX_VALUE;
            for(Unit unit : selectedUnits){
                if(unit == null || !unit.isValid() || !unit.canBuild()) continue;
                float dst = unit.dst2(worldX, worldY);
                if(dst < bestDst){
                    bestDst = dst;
                    chosen = unit;
                }
            }
        }

        if(chosen == null){
            return;
        }

        boolean queueCommand = shiftHeld;
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

        if(!shiftHeld){
            ui.hudfrag.abilityPanel.exitCommandMode();
        }
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
                Teamc attack = build;
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

        float clampedY = clampScreenY(y);

        //If in active RTS command mode, handle left-click on press (not release)
        if(ui.hudfrag.abilityPanel != null && ui.hudfrag.abilityPanel.activeCommand != mindustry.ui.UnitAbilityPanel.CommandMode.NONE){
            if(button == KeyCode.mouseLeft){
                //Execute command immediately on mouse press
                abilityTargetConsumeMillis = Time.millis();
                executeActiveCommand(x, clampedY);
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
                commandTap(x, clampedY, true);
            }else{
                commandTap(x, clampedY);
            }
        }

        if(button == Binding.commandQueue.value.key){
            if(commandFocusGuardTime <= 0f){
                commandTap(x, clampedY, true);
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
        return clampScreenY(Core.input.mouseY());
    }

    private void rotateCameraMove(Vec2 vec){
        float rot = renderer.getViewRotation();
        if(Mathf.zero(rot)) return;
        float rad = -rot * Mathf.degRad;
        float cos = Mathf.cos(rad);
        float sin = Mathf.sin(rad);
        float x = vec.x * cos - vec.y * sin;
        float y = vec.x * sin + vec.y * cos;
        vec.set(x, y);
    }

    private float clampScreenY(float screenY){
        float inset = renderer.getUiBottomInsetPx();
        if(inset > 0f && screenY < inset){
            return inset;
        }
        return screenY;
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
                movement.add(mouseWorld().sub(player).scl(1f / 25f * speed)).limit(speed);
            }
        }

        Vec2 mw = mouseWorld();
        float mouseAngle = Angles.angle(unit.x, unit.y, mw.x, mw.y);
        boolean aimCursor = omni && player.shooting && unit.type.hasWeapons() && unit.type.faceTarget && !boosted;

        if(aimCursor){
            unit.lookAt(mouseAngle);
        }else{
            unit.lookAt(unit.prefRotation());
        }

        unit.movePref(movement);

        unit.aim(mw);
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
