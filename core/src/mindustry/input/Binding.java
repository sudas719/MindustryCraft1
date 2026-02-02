package mindustry.input;

import arc.input.*;
import arc.input.KeyBind.*;
import mindustry.*;

public class Binding{
    public static final KeyBind

    moveX = KeyBind.add("move_x", KeyCode.unset, "general"),
    moveY = KeyBind.add("move_y", KeyCode.unset),
    mouseMove = KeyBind.add("mouse_move", KeyCode.mouseBack),
    pan = KeyBind.add("pan", KeyCode.mouseForward),

    boost = KeyBind.add("boost", KeyCode.shiftLeft),
    respawn = KeyBind.add("respawn", KeyCode.v),
    control = KeyBind.add("control", KeyCode.controlLeft),
    select = KeyBind.add("select", KeyCode.mouseLeft),
    deselect = KeyBind.add("deselect", KeyCode.mouseRight),
    breakBlock = KeyBind.add("break_block", KeyCode.mouseRight),

    pickupCargo = KeyBind.add("pickupCargo", KeyCode.leftBracket),
    dropCargo = KeyBind.add("dropCargo", KeyCode.rightBracket),

    clearBuilding = KeyBind.add("clear_building", KeyCode.unset),
    pauseBuilding = KeyBind.add("pause_building", KeyCode.unset),
    rotate = KeyBind.add("rotate", new Axis(KeyCode.scroll)),
    rotatePlaced = KeyBind.add("rotateplaced", KeyCode.unset),
    diagonalPlacement = KeyBind.add("diagonal_placement", KeyCode.controlLeft),
    pick = KeyBind.add("pick", KeyCode.mouseMiddle),

    rebuildSelect = KeyBind.add("rebuild_select", KeyCode.unset),
    schematicSelect = KeyBind.add("schematic_select", KeyCode.unset),
    schematicFlipX = KeyBind.add("schematic_flip_x", KeyCode.unset),
    schematicFlipY = KeyBind.add("schematic_flip_y", KeyCode.unset),
    schematicMenu = KeyBind.add("schematic_menu", KeyCode.unset),

    commandMode = KeyBind.add("command_mode", KeyCode.shiftLeft, "command"),
    commandQueue = KeyBind.add("command_queue", KeyCode.mouseMiddle),
    createControlGroup = KeyBind.add("create_control_group", KeyCode.controlLeft),

    selectAllUnits = KeyBind.add("select_all_units", KeyCode.g),
    selectAllUnitFactories = KeyBind.add("select_all_unit_factories", KeyCode.unset),  //Disabled: conflicts with RTS Hold command
    selectAllUnitTransport = KeyBind.add("select_all_unit_transport", KeyCode.unset),
    selectAcrossScreen = KeyBind.add("select_across_screen", KeyCode.altLeft),

    cancelOrders = KeyBind.add("cancel_orders", KeyCode.unset),

    //New RTS commands (StarCraft 2 style)
    rtsCommandMove = KeyBind.add("rts_command_move", KeyCode.m),
    rtsCommandStop = KeyBind.add("rts_command_stop", KeyCode.s),
    rtsCommandHold = KeyBind.add("rts_command_hold", KeyCode.h),
    rtsCommandPatrol = KeyBind.add("rts_command_patrol", KeyCode.p),
    rtsCommandAttack = KeyBind.add("rts_command_attack", KeyCode.a),

    unitStanceShoot = KeyBind.add("unit_stance_shoot", KeyCode.unset),
    unitStanceHoldFire = KeyBind.add("unit_stance_hold_fire", KeyCode.unset),
    unitStancePursueTarget = KeyBind.add("unit_stance_pursue_target", KeyCode.unset),
    unitStancePatrol = KeyBind.add("unit_stance_patrol", KeyCode.unset),
    unitStanceRam = KeyBind.add("unit_stance_ram", KeyCode.unset),

    unitCommandMove = KeyBind.add("unit_command_move", KeyCode.unset),
    unitCommandRepair = KeyBind.add("unit_command_repair", KeyCode.unset),
    unitCommandRebuild = KeyBind.add("unit_command_rebuild", KeyCode.unset),
    unitCommandAssist = KeyBind.add("unit_command_assist", KeyCode.unset),
    unitCommandMine = KeyBind.add("unit_command_mine", KeyCode.unset),
    unitCommandHarvest = KeyBind.add("unit_command_harvest", KeyCode.g),
    unitCommandBoost = KeyBind.add("unit_command_boost", KeyCode.unset),
    unitCommandEnterPayload = KeyBind.add("unit_command_enter_payload", KeyCode.unset),
    unitCommandLoadUnits = KeyBind.add("unit_command_load_units", KeyCode.unset),
    unitCommandLoadBlocks = KeyBind.add("unit_command_load_blocks", KeyCode.unset),
    unitCommandUnloadPayload = KeyBind.add("unit_command_unload_payload", KeyCode.unset),
    unitCommandLoopPayload = KeyBind.add("unit_command_loop_payload", KeyCode.unset),

    // Grid command bindings - 3x5 layout (StarCraft II style)
    commandGrid01 = KeyBind.add("command_grid_01", KeyCode.q, "command"),
    commandGrid02 = KeyBind.add("command_grid_02", KeyCode.w),
    commandGrid03 = KeyBind.add("command_grid_03", KeyCode.e),
    commandGrid04 = KeyBind.add("command_grid_04", KeyCode.r),
    commandGrid05 = KeyBind.add("command_grid_05", KeyCode.t),
    commandGrid06 = KeyBind.add("command_grid_06", KeyCode.a),
    commandGrid07 = KeyBind.add("command_grid_07", KeyCode.s),
    commandGrid08 = KeyBind.add("command_grid_08", KeyCode.d),
    commandGrid09 = KeyBind.add("command_grid_09", KeyCode.f),
    commandGrid10 = KeyBind.add("command_grid_10", KeyCode.g),
    commandGrid11 = KeyBind.add("command_grid_11", KeyCode.z),
    commandGrid12 = KeyBind.add("command_grid_12", KeyCode.x),
    commandGrid13 = KeyBind.add("command_grid_13", KeyCode.c),
    commandGrid14 = KeyBind.add("command_grid_14", KeyCode.v),
    commandGrid15 = KeyBind.add("command_grid_15", KeyCode.b),

    categoryPrev = KeyBind.add("category_prev", KeyCode.comma, "blocks"),
    categoryNext = KeyBind.add("category_next", KeyCode.period),

    blockSelectLeft = KeyBind.add("block_select_left", KeyCode.left),
    blockSelectRight = KeyBind.add("block_select_right", KeyCode.right),
    blockSelectUp = KeyBind.add("block_select_up", KeyCode.up),
    blockSelectDown = KeyBind.add("block_select_down", KeyCode.down),
    blockSelect01 = KeyBind.add("block_select_01", KeyCode.num1),
    blockSelect02 = KeyBind.add("block_select_02", KeyCode.num2),
    blockSelect03 = KeyBind.add("block_select_03", KeyCode.num3),
    blockSelect04 = KeyBind.add("block_select_04", KeyCode.num4),
    blockSelect05 = KeyBind.add("block_select_05", KeyCode.num5),
    blockSelect06 = KeyBind.add("block_select_06", KeyCode.num6),
    blockSelect07 = KeyBind.add("block_select_07", KeyCode.num7),
    blockSelect08 = KeyBind.add("block_select_08", KeyCode.num8),
    blockSelect09 = KeyBind.add("block_select_09", KeyCode.num9),
    blockSelect10 = KeyBind.add("block_select_10", KeyCode.num0),

    zoom = KeyBind.add("zoom", new Axis(KeyCode.scroll), "view"),
    detachCamera = KeyBind.add("detach_camera", KeyCode.unset),

    //View presets: Ctrl+F1-F4 to save, F1-F4 to jump
    viewPreset1 = KeyBind.add("view_preset_1", KeyCode.f1),
    viewPreset2 = KeyBind.add("view_preset_2", KeyCode.f2),
    viewPreset3 = KeyBind.add("view_preset_3", KeyCode.f3),
    viewPreset4 = KeyBind.add("view_preset_4", KeyCode.f4),

    menu = KeyBind.add("menu", Vars.android ? KeyCode.back : KeyCode.f10),
    fullscreen = KeyBind.add("fullscreen", KeyCode.f11),
    pause = KeyBind.add("pause", KeyCode.space),
    skipWave = KeyBind.add("skip_wave", KeyCode.unset),
    minimap = KeyBind.add("minimap", KeyCode.unset),  //Disabled: conflicts with RTS Move command
    research = KeyBind.add("research", KeyCode.j),
    planetMap = KeyBind.add("planet_map", KeyCode.unset),  //Disabled: conflicts with RTS commands
    blockInfo = KeyBind.add("block_info", KeyCode.unset),  //Disabled to avoid conflict with view presets
    toggleMenus = KeyBind.add("toggle_menus", KeyCode.unset),
    screenshot = KeyBind.add("screenshot", KeyCode.unset),  //Disabled: conflicts with RTS Patrol command
    togglePowerLines = KeyBind.add("toggle_power_lines", KeyCode.f5),
    toggleBlockStatus = KeyBind.add("toggle_block_status", KeyCode.f6),
    playerList = KeyBind.add("player_list", KeyCode.tab, "multiplayer"),
    chat = KeyBind.add("chat", KeyCode.enter),
    chatHistoryPrev = KeyBind.add("chat_history_prev", KeyCode.up),
    chatHistoryNext = KeyBind.add("chat_history_next", KeyCode.down),
    chatScroll = KeyBind.add("chat_scroll", new Axis(KeyCode.scroll)),
    chatMode = KeyBind.add("chat_mode", KeyCode.tab),
    console = KeyBind.add("console", KeyCode.f8),
    debugHitboxes = KeyBind.add("debug_hitboxes", KeyCode.unset)
    ;

    //dummy static class initializer
    public static void init(){}
}
