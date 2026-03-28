package mindustry.ui;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.ai.types.HarvestAI;
import mindustry.content.*;
import mindustry.ctype.*;
import mindustry.gen.*;
import mindustry.game.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.entities.bullet.*;
import mindustry.world.blocks.defense.turrets.*;
import mindustry.world.blocks.environment.CrystalMineralWall;
import mindustry.world.blocks.environment.SteamVent;
import mindustry.world.blocks.ConstructBlock;
import mindustry.world.blocks.payloads.*;
import mindustry.world.blocks.storage.*;
import mindustry.world.blocks.storage.CoreBlock.*;
import mindustry.world.blocks.units.*;

import static mindustry.Vars.*;

public class UnitSelectionGrid extends Table{
    private static final int COLS = 8;
    private static final int ROWS = 3;
    private static final int UNITS_PER_PAGE = COLS * ROWS;
    private static final float SELECTION_PANEL_SCALE = 1.2f;
    private static final float GRID_PORTRAIT_PAD = 2f * SELECTION_PANEL_SCALE;
    private static final String detectorTag = "[gold]\u4fa6\u6d4b\u5355\u4f4d";
    private static final int PAYLOAD_GRID_COLS = 4;
    private static final int PAYLOAD_GRID_ROWS = 2;
    private static final float PAYLOAD_SLOT_SIZE = 38f * SELECTION_PANEL_SCALE;
    private static final float PAYLOAD_SLOT_PAD = 3f * SELECTION_PANEL_SCALE;
    private static final float QUEUE_SLOT_SIZE = 40f * SELECTION_PANEL_SCALE;

    private int currentPage = 0;
    private Seq<Displayable> displayedItems = new Seq<>();
    private Table gridTable;
    private Table paginationTable;
    private Tile displayedResource;
    private boolean lastCoreBuildPage;
    private int lastCoreQueueHash = -1;
    private int lastCoreId = -1;
    private int lastFactoryId = -1;
    private int lastFactoryQueueHash = -1;
    private int lastArmoryId = -1;
    private int lastArmoryQueueHash = -1;
    private int lastFusionCoreId = -1;
    private int lastFusionCoreQueueHash = -1;
    private int lastEngineeringId = -1;
    private int lastEngineeringQueueHash = -1;
    private int lastGhostAcademyId = -1;
    private int lastGhostAcademyQueueHash = -1;
    private int lastTechLabId = -1;
    private int lastTechLabQueueHash = -1;
    private int lastMedivacId = -1;
    private int lastMedivacPayloadHash = -1;
    private final GlyphLayout addonBadgeLayout = new GlyphLayout();

    private static float ui(float value){
        return value * SELECTION_PANEL_SCALE;
    }

    //Interface for units and buildings
    private interface Displayable{
        TextureRegion icon();
        float health();
        float maxHealth();
        boolean isValid();
        float x();
        float y();
        String name();
    }

    private static Block displayBlock(Building building){
        if(building instanceof ConstructBlock.ConstructBuild cons && cons.current != null && cons.current != Blocks.air){
            return cons.current;
        }
        return building.block;
    }

    private static float displayMaxHealth(Building building){
        if(building instanceof ConstructBlock.ConstructBuild cons && cons.current != null && cons.current != Blocks.air){
            return cons.current.health;
        }
        return building.maxHealth;
    }

    private static float displayArmor(Building building, Block display){
        if(building != null && display == Blocks.scrapWall && building.team == Team.derelict){
            return 1f;
        }
        return display == null ? 0f : display.armor;
    }

    //StarCraft 2-style unit command
    public static class UnitCommand{
        public String name;
        public TextureRegion icon;
        public Runnable action;

        public UnitCommand(String name, TextureRegion icon, Runnable action){
            this.name = name;
            this.icon = icon;
            this.action = action;
        }
    }

    private static class UnitDisplay implements Displayable{
        Unit unit;

        UnitDisplay(Unit unit){
            this.unit = unit;
        }

        @Override
        public TextureRegion icon(){
            return unit.type.uiIcon;
        }

        @Override
        public float health(){
            return unit.health;
        }

        @Override
        public float maxHealth(){
            return unit.maxHealth;
        }

        @Override
        public boolean isValid(){
            return unit.isValid();
        }

        @Override
        public float x(){
            return unit.x;
        }

        @Override
        public float y(){
            return unit.y;
        }

        @Override
        public String name(){
            return unit.type.localizedName;
        }
    }

    private static class BuildingDisplay implements Displayable{
        Building building;

        BuildingDisplay(Building building){
            this.building = building;
        }

        @Override
        public TextureRegion icon(){
            return displayBlock(building).uiIcon;
        }

        @Override
        public float health(){
            return building.health;
        }

        @Override
        public float maxHealth(){
            return displayMaxHealth(building);
        }

        @Override
        public boolean isValid(){
            return building.isValid();
        }

        @Override
        public float x(){
            return building.x;
        }

        @Override
        public float y(){
            return building.y;
        }

        @Override
        public String name(){
            return displayBlock(building).localizedName;
        }
    }

    private static class PayloadLayoutEntry{
        Payload payload;
        UnlockableContent content;
        int col, row, colSpan, rowSpan;
    }

    public UnitSelectionGrid(){
        background(Styles.black6);
        margin(ui(4f));

        //Main layout - vertical stack
        defaults().center();

        //Formation numbers (top) - now at the top of the panel
        Table formationTable = new Table();
        add(formationTable).growX().row();

        //Content row: pagination (left) + unit grid (right)
        Table contentRow = new Table();
        add(contentRow).grow().row();

        //Left side: pagination (vertical)
        paginationTable = new Table();
        contentRow.add(paginationTable).left().padRight(ui(4f));

        //Right side: unit grid
        gridTable = new Table();
        contentRow.add(gridTable).grow();

        update(() -> {
            //Update formation numbers display - show all 10 buttons (1-0)
            formationTable.clear();
            formationTable.defaults().size(ui(32f)).pad(ui(2f));

            //Draw all 10 formation buttons (1-0)
            for(int i = 0; i < 10; i++){
                final int formationNum = i;
                IntSeq group = control.input.controlGroups[i];
                boolean hasUnits = group != null;
                if(hasUnits){
                    control.input.sanitizeControlGroup(group);
                    hasUnits = !group.isEmpty();
                }

                String buttonText = String.valueOf((i + 1) % 10); //1-9, then 0

                if(hasUnits){
                    //Active formation - yellow button
                    formationTable.button(buttonText, Styles.cleart, () -> {
                        //Check if Shift is held for adding to formation
                        if(Core.input.keyDown(arc.input.KeyCode.shiftLeft) || Core.input.keyDown(arc.input.KeyCode.shiftRight)){
                            //Shift+Click: Add currently selected units to this formation
                            if(control.input.controlGroups[formationNum] == null){
                                control.input.controlGroups[formationNum] = new IntSeq();
                            }

                            IntSeq formGroup = control.input.controlGroups[formationNum];
                            IntSeq selectedUnitIds = control.input.selectedUnits.mapInt(u -> u.id);
                            IntSeq selectedBuildingIds = control.input.commandBuildings.mapInt(b -> b.id);

                            //Remove from other groups if distinct control groups is enabled
                            if(Core.settings.getBool("distinctcontrolgroups", true)){
                                for(IntSeq cg : control.input.controlGroups){
                                    if(cg != null && cg != formGroup){
                                        cg.removeAll(selectedUnitIds);
                                        cg.removeAll(selectedBuildingIds);
                                    }
                                }
                            }

                            //Add to this formation
                            formGroup.addAll(selectedUnitIds);
                            formGroup.addAll(selectedBuildingIds);
                        }else{
                            //Normal click: Switch to this formation
                            if(control.input.controlGroups[formationNum] != null){
                                control.input.sanitizeControlGroup(control.input.controlGroups[formationNum]);
                                control.input.selectedUnits.clear();
                                control.input.commandBuildings.clear();

                                control.input.controlGroups[formationNum].each(id -> {
                                    Unit unit = Groups.unit.getByID(id);
                                    Building building = null;

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

                                    if(unit != null && unit.team == player.team()){
                                        control.input.selectedUnits.add(unit);
                                    }else if(building != null && building.team == player.team()){
                                        control.input.commandBuildings.add(building);
                                    }
                                });
                            }
                        }
                    }).color(Color.yellow).tooltip("Formation " + buttonText + "\nShift+Click to add units");
                }else{
                    //Empty formation - grayed out button
                    formationTable.button(buttonText, Styles.cleart, () -> {
                        //Empty - do nothing
                    }).color(Color.gray).disabled(true).tooltip("Formation " + buttonText + " (empty)");
                }
            }

            //Check if selected units/buildings changed
            Seq<Displayable> current = new Seq<>();
            for(Unit unit : control.input.selectedUnits){
                if(unit.isValid()){
                    current.add(new UnitDisplay(unit));
                }
            }
            for(Building building : control.input.commandBuildings){
                if(building.isValid()){
                    current.add(new BuildingDisplay(building));
                }
            }
            if(current.isEmpty()){
                if(control.input.hasSpectatorSyncedSelection()){
                    for(Unit unit : control.input.spectatorSyncedUnits()){
                        if(unit != null && unit.isValid()){
                            current.add(new UnitDisplay(unit));
                        }
                    }
                    for(Building building : control.input.spectatorSyncedBuildings()){
                        if(building != null && building.isValid()){
                            current.add(new BuildingDisplay(building));
                        }
                    }
                }else{
                    Unit readOnlyUnit = control.input.readOnlySelectedUnit();
                    Building readOnlyBuilding = control.input.readOnlySelectedBuilding();
                    if(readOnlyUnit != null && readOnlyUnit.isValid()){
                        current.add(new UnitDisplay(readOnlyUnit));
                    }else if(readOnlyBuilding != null && readOnlyBuilding.isValid()){
                        current.add(new BuildingDisplay(readOnlyBuilding));
                    }
                }
            }
            if(current.size > 1){
                current.sort(this::compareDisplayable);
            }

            Tile currentResource = control.input.selectedResource;
            boolean showResource = current.isEmpty() && currentResource != null &&
                (currentResource.block() instanceof CrystalMineralWall || currentResource.floor() instanceof SteamVent);
            boolean forceRebuild = false;

            if(control.input.selectedUnits.isEmpty() && control.input.commandBuildings.size == 1){
                Building selected = control.input.commandBuildings.first();
                if(selected instanceof CoreBuild core){
                    boolean coreBuildPage = ui.hudfrag.abilityPanel != null && ui.hudfrag.abilityPanel.isCoreBuildPage();
                    int queueHash = coreQueueHash(core);
                    if(core.id != lastCoreId || coreBuildPage != lastCoreBuildPage || queueHash != lastCoreQueueHash){
                        forceRebuild = true;
                    }
                    lastCoreId = core.id;
                    lastCoreBuildPage = coreBuildPage;
                    lastCoreQueueHash = queueHash;
                    lastFactoryId = -1;
                    lastFactoryQueueHash = -1;
                    lastArmoryId = -1;
                    lastArmoryQueueHash = -1;
                    lastFusionCoreId = -1;
                    lastFusionCoreQueueHash = -1;
                    lastEngineeringId = -1;
                    lastEngineeringQueueHash = -1;
                    lastGhostAcademyId = -1;
                    lastGhostAcademyQueueHash = -1;
                    lastTechLabId = -1;
                    lastTechLabQueueHash = -1;
                    lastMedivacId = -1;
                    lastMedivacPayloadHash = -1;
                }else if(selected instanceof UnitFactory.UnitFactoryBuild factory && factory.sc2QueueEnabled()){
                    int queueHash = factoryQueueHash(factory);
                    if(factory.id != lastFactoryId || queueHash != lastFactoryQueueHash){
                        forceRebuild = true;
                    }
                    lastFactoryId = factory.id;
                    lastFactoryQueueHash = queueHash;
                    lastCoreId = -1;
                    lastCoreBuildPage = false;
                    lastCoreQueueHash = -1;
                    lastArmoryId = -1;
                    lastArmoryQueueHash = -1;
                    lastFusionCoreId = -1;
                    lastFusionCoreQueueHash = -1;
                    lastEngineeringId = -1;
                    lastEngineeringQueueHash = -1;
                    lastGhostAcademyId = -1;
                    lastGhostAcademyQueueHash = -1;
                    lastTechLabId = -1;
                    lastTechLabQueueHash = -1;
                    lastMedivacId = -1;
                    lastMedivacPayloadHash = -1;
                }else if(selected.block == Blocks.siliconCrucible){
                    int queueHash = armoryQueueHash(selected.team);
                    if(selected.id != lastArmoryId || queueHash != lastArmoryQueueHash){
                        forceRebuild = true;
                    }
                    lastArmoryId = selected.id;
                    lastArmoryQueueHash = queueHash;
                    lastFusionCoreId = -1;
                    lastFusionCoreQueueHash = -1;
                    lastCoreId = -1;
                    lastCoreBuildPage = false;
                    lastCoreQueueHash = -1;
                    lastFactoryId = -1;
                    lastFactoryQueueHash = -1;
                    lastEngineeringId = -1;
                    lastEngineeringQueueHash = -1;
                    lastGhostAcademyId = -1;
                    lastGhostAcademyQueueHash = -1;
                    lastTechLabId = -1;
                    lastTechLabQueueHash = -1;
                    lastMedivacId = -1;
                    lastMedivacPayloadHash = -1;
                }else if(selected.block == Blocks.surgeCrucible){
                    int queueHash = fusionCoreQueueHash(selected.team);
                    if(selected.id != lastFusionCoreId || queueHash != lastFusionCoreQueueHash){
                        forceRebuild = true;
                    }
                    lastFusionCoreId = selected.id;
                    lastFusionCoreQueueHash = queueHash;
                    lastCoreId = -1;
                    lastCoreBuildPage = false;
                    lastCoreQueueHash = -1;
                    lastFactoryId = -1;
                    lastFactoryQueueHash = -1;
                    lastArmoryId = -1;
                    lastArmoryQueueHash = -1;
                    lastEngineeringId = -1;
                    lastEngineeringQueueHash = -1;
                    lastGhostAcademyId = -1;
                    lastGhostAcademyQueueHash = -1;
                    lastTechLabId = -1;
                    lastTechLabQueueHash = -1;
                    lastMedivacId = -1;
                    lastMedivacPayloadHash = -1;
                }else if(selected.block == Blocks.multiPress){
                    int queueHash = engineeringQueueHash(selected.team);
                    if(selected.id != lastEngineeringId || queueHash != lastEngineeringQueueHash){
                        forceRebuild = true;
                    }
                    lastEngineeringId = selected.id;
                    lastEngineeringQueueHash = queueHash;
                    lastCoreId = -1;
                    lastCoreBuildPage = false;
                    lastCoreQueueHash = -1;
                    lastFactoryId = -1;
                    lastFactoryQueueHash = -1;
                    lastArmoryId = -1;
                    lastArmoryQueueHash = -1;
                    lastFusionCoreId = -1;
                    lastFusionCoreQueueHash = -1;
                    lastGhostAcademyId = -1;
                    lastGhostAcademyQueueHash = -1;
                    lastTechLabId = -1;
                    lastTechLabQueueHash = -1;
                    lastMedivacId = -1;
                    lastMedivacPayloadHash = -1;
                }else if(selected.block == Blocks.launchPad){
                    int queueHash = ghostAcademyQueueHash(selected);
                    if(selected.id != lastGhostAcademyId || queueHash != lastGhostAcademyQueueHash){
                        forceRebuild = true;
                    }
                    lastGhostAcademyId = selected.id;
                    lastGhostAcademyQueueHash = queueHash;
                    lastCoreId = -1;
                    lastCoreBuildPage = false;
                    lastCoreQueueHash = -1;
                    lastFactoryId = -1;
                    lastFactoryQueueHash = -1;
                    lastArmoryId = -1;
                    lastArmoryQueueHash = -1;
                    lastFusionCoreId = -1;
                    lastFusionCoreQueueHash = -1;
                    lastEngineeringId = -1;
                    lastEngineeringQueueHash = -1;
                    lastTechLabId = -1;
                    lastTechLabQueueHash = -1;
                    lastMedivacId = -1;
                    lastMedivacPayloadHash = -1;
                }else if(selected.block == Blocks.memoryBank){
                    int queueHash = techLabQueueHash(selected);
                    if(selected.id != lastTechLabId || queueHash != lastTechLabQueueHash){
                        forceRebuild = true;
                    }
                    lastTechLabId = selected.id;
                    lastTechLabQueueHash = queueHash;
                    lastCoreId = -1;
                    lastCoreBuildPage = false;
                    lastCoreQueueHash = -1;
                    lastFactoryId = -1;
                    lastFactoryQueueHash = -1;
                    lastArmoryId = -1;
                    lastArmoryQueueHash = -1;
                    lastFusionCoreId = -1;
                    lastFusionCoreQueueHash = -1;
                    lastEngineeringId = -1;
                    lastEngineeringQueueHash = -1;
                    lastGhostAcademyId = -1;
                    lastGhostAcademyQueueHash = -1;
                    lastMedivacId = -1;
                    lastMedivacPayloadHash = -1;
                }else{
                    lastCoreId = -1;
                    lastCoreBuildPage = false;
                    lastCoreQueueHash = -1;
                    lastFactoryId = -1;
                    lastFactoryQueueHash = -1;
                    lastArmoryId = -1;
                    lastArmoryQueueHash = -1;
                    lastFusionCoreId = -1;
                    lastFusionCoreQueueHash = -1;
                    lastEngineeringId = -1;
                    lastEngineeringQueueHash = -1;
                    lastGhostAcademyId = -1;
                    lastGhostAcademyQueueHash = -1;
                    lastTechLabId = -1;
                    lastTechLabQueueHash = -1;
                    lastMedivacId = -1;
                    lastMedivacPayloadHash = -1;
                }
            }else{
                lastCoreId = -1;
                lastCoreBuildPage = false;
                lastCoreQueueHash = -1;
                lastFactoryId = -1;
                lastFactoryQueueHash = -1;
                lastArmoryId = -1;
                lastArmoryQueueHash = -1;
                lastFusionCoreId = -1;
                lastFusionCoreQueueHash = -1;
                lastEngineeringId = -1;
                lastEngineeringQueueHash = -1;
                lastGhostAcademyId = -1;
                lastGhostAcademyQueueHash = -1;
                lastTechLabId = -1;
                lastTechLabQueueHash = -1;

                if(control.input.selectedUnits.size == 1 && control.input.commandBuildings.isEmpty()){
                    Unit unit = control.input.selectedUnits.first();
                    if(unit != null && unit.isValid() && UnitTypes.isMedivac(unit)){
                        int payloadHash = medivacPayloadHash(unit);
                        if(unit.id != lastMedivacId || payloadHash != lastMedivacPayloadHash){
                            forceRebuild = true;
                        }
                        lastMedivacId = unit.id;
                        lastMedivacPayloadHash = payloadHash;
                    }else{
                        lastMedivacId = -1;
                        lastMedivacPayloadHash = -1;
                    }
                }else{
                    lastMedivacId = -1;
                    lastMedivacPayloadHash = -1;
                }
            }

            if(showResource){
                if(displayedResource != currentResource || !displayedItems.isEmpty()){
                    displayedItems.clear();
                    displayedResource = currentResource;
                    rebuild();
                }
            }else{
                displayedResource = null;
                if(forceRebuild || current.size != displayedItems.size || !itemsEqual(current, displayedItems)){
                    displayedItems.clear();
                    displayedItems.addAll(current);
                    rebuild();
                }
            }
        });
    }

    private boolean itemsEqual(Seq<Displayable> a, Seq<Displayable> b){
        if(a.size != b.size) return false;
        for(int i = 0; i < a.size; i++){
            if(!a.get(i).isValid() || !b.get(i).isValid()) return false;
            if(a.get(i).x() != b.get(i).x() || a.get(i).y() != b.get(i).y()) return false;
        }
        return true;
    }

    private int compareDisplayable(Displayable a, Displayable b){
        boolean aUnit = a instanceof UnitDisplay;
        boolean bUnit = b instanceof UnitDisplay;
        if(aUnit != bUnit) return aUnit ? -1 : 1;
        if(aUnit){
            Unit ua = ((UnitDisplay)a).unit;
            Unit ub = ((UnitDisplay)b).unit;
            int groupCmp = Integer.compare(unitSelectionGroup(ua.type), unitSelectionGroup(ub.type));
            if(groupCmp != 0) return groupCmp;
            int typeCmp = Integer.compare(ua.type.id, ub.type.id);
            if(typeCmp != 0) return typeCmp;
            return Integer.compare(ua.id, ub.id);
        }
        Building ba = ((BuildingDisplay)a).building;
        Building bb = ((BuildingDisplay)b).building;
        if(isAddonSortableFactory(ba) && isAddonSortableFactory(bb)){
            int addonCmp = Integer.compare(factoryAddonCategory(ba), factoryAddonCategory(bb));
            if(addonCmp != 0) return addonCmp;
        }
        int blockCmp = Integer.compare(ba.block.id, bb.block.id);
        if(blockCmp != 0) return blockCmp;
        return Integer.compare(ba.id, bb.id);
    }

    private boolean isAddonSortableFactory(@Nullable Building build){
        return build instanceof UnitFactory.UnitFactoryBuild factory && factory.sc2QueueEnabled();
    }

    private int factoryAddonCategory(@Nullable Building build){
        if(!(build instanceof UnitFactory.UnitFactoryBuild factory) || !factory.sc2QueueEnabled()) return 1;
        if(factory.hasDoubleAddon()) return 0;
        if(factory.hasTechAddon()) return 2;
        return 1;
    }

    private @Nullable String factoryAddonLetter(@Nullable Building build){
        if(!(build instanceof UnitFactory.UnitFactoryBuild factory) || !factory.sc2QueueEnabled()) return null;
        if(factory.hasDoubleAddon()) return "R";
        if(factory.hasTechAddon()) return "T";
        return null;
    }

    private int unitSelectionGroup(@Nullable UnitType type){
        if(type == null) return 999;
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
        return 999;
    }

    private void rebuild(){
        gridTable.clear();
        paginationTable.clear();

        if(displayedResource != null){
            if(displayedResource.block() instanceof CrystalMineralWall){
                buildCrystalInfoPanel(displayedResource);
            }else if(displayedResource.floor() instanceof SteamVent){
                buildSteamVentInfoPanel(displayedResource);
            }
            return;
        }

        if(displayedItems.isEmpty()) return;

        int totalPages = (int)Math.ceil((float)displayedItems.size / UNITS_PER_PAGE);
        currentPage = Math.min(currentPage, Math.max(0, totalPages - 1));

        //Build grid - always maintain fixed size
        int startIdx = currentPage * UNITS_PER_PAGE;
        int endIdx = Math.min(startIdx + UNITS_PER_PAGE, displayedItems.size);

        //Calculate how many rows we need to fill
        int itemsToShow = endIdx - startIdx;
        int rowsNeeded = (int)Math.ceil((float)itemsToShow / COLS);

        for(int i = startIdx; i < endIdx; i++){
            Displayable item = displayedItems.get(i);
            int col = (i - startIdx) % COLS;
            boolean readOnly = control.input.isSelectionReadOnly();

            //Create portrait button
            Table portrait = new Table();
            Stack stack = new Stack();
            stack.add(portraitBorderElement());
            if(item instanceof UnitDisplay unitDisplay){
                stack.add(subgroupHighlightElement(unitDisplay.unit.type));
            }else if(item instanceof BuildingDisplay buildingDisplay){
                stack.add(subgroupHighlightElement(buildingDisplay.building));
            }
            Image icon = new Image(item.icon());
            icon.setScaling(Scaling.fit);
            Table iconTable = new Table();
            iconTable.add(icon).size(UnitAbilityPanel.abilityIconSize);
            stack.add(iconTable);
            if(item instanceof BuildingDisplay buildingDisplay){
                Building build = buildingDisplay.building;
                boolean friendlyReadOnly = readOnly && ViewerPerspective.isFriendly(build.team);
                if(build.team == player.team() || friendlyReadOnly){
                    if(build instanceof UnitFactory.UnitFactoryBuild factory && factory.sc2QueueEnabled()){
                        stack.add(factoryQueueSlotsElement(factory));
                    }else if(build instanceof CoreBuild core){
                        stack.add(coreQueueSlotsElement(core));
                    }
                }
                stack.add(factoryAddonBadgeElement(build));
            }
            portrait.add(stack).size(gridPortraitSize());

            portrait.clicked(() -> {
                if(readOnly) return;
                //Shift+click removes from selection
                if(Core.input.keyDown(arc.input.KeyCode.shiftLeft) || Core.input.keyDown(arc.input.KeyCode.shiftRight)){
                    if(item instanceof UnitDisplay){
                        control.input.selectedUnits.remove(((UnitDisplay)item).unit);
                    }else if(item instanceof BuildingDisplay){
                        control.input.commandBuildings.remove(((BuildingDisplay)item).building);
                    }
                }else{
                    //Normal click: select only this unit/building, deselect all others
                    control.input.selectedUnits.clear();
                    control.input.commandBuildings.clear();

                    if(item instanceof UnitDisplay){
                        control.input.selectedUnits.add(((UnitDisplay)item).unit);
                    }else if(item instanceof BuildingDisplay){
                        control.input.commandBuildings.add(((BuildingDisplay)item).building);
                    }
                }
            });

            gridTable.add(portrait).size(gridPortraitSize()).pad(GRID_PORTRAIT_PAD);

            if(col == COLS - 1){
                gridTable.row();
            }
        }

        //Fill remaining cells with empty space to maintain grid size
        int totalCells = endIdx - startIdx;
        int remainingCells = (ROWS * COLS) - totalCells;
        for(int i = 0; i < remainingCells; i++){
            gridTable.add().size(gridPortraitSize()).pad(GRID_PORTRAIT_PAD);
            if((totalCells + i + 1) % COLS == 0){
                gridTable.row();
            }
        }

        //Add pagination with direct page number selection (vertical layout on left)
        if(totalPages > 1){
            paginationTable.defaults().size(ui(32f)).pad(ui(2f));
            for(int i = 0; i < totalPages; i++){
                final int pageNum = i;
                paginationTable.button(String.valueOf(i + 1), () -> {
                    currentPage = pageNum;
                    rebuild();
                }).disabled(b -> currentPage == pageNum).row(); //.row() makes it vertical
            }
        }

        //Show unit info when only one unit/building is selected
        if(displayedItems.size == 1){
            Displayable item = displayedItems.get(0);
            boolean readOnly = control.input.isSelectionReadOnly();
            if(item instanceof UnitDisplay){
                UnitDisplay unitDisplay = (UnitDisplay)item;
                buildSingleUnitPanel(unitDisplay.unit, readOnly);
            }else if(item instanceof BuildingDisplay){
                BuildingDisplay buildingDisplay = (BuildingDisplay)item;
                buildBuildingInfoPanel(buildingDisplay.building, readOnly);
            }
        }
    }

    private boolean friendlyReadOnly(@Nullable Team team, boolean readOnly){
        return readOnly && ViewerPerspective.isFriendly(team);
    }

    private void buildSingleUnitPanel(Unit unit, boolean readOnly){
        //Clear the grid and show single unit info instead
        gridTable.clear();
        boolean friendlyReadOnly = friendlyReadOnly(unit.team, readOnly);

        gridTable.table(panel -> {
            panel.background(Styles.black6);
            panel.margin(ui(8f));

            //Left half: Unit icon and HP
            panel.table(leftHalf -> {
                Stack unitIconStack = new Stack();
                unitIconStack.add(portraitBorderElement());
                Table iconWrap = new Table();
                iconWrap.image(unit.type.uiIcon).size(ui(76f));
                unitIconStack.add(iconWrap);
                leftHalf.add(unitIconStack).size(ui(80f)).row();
                //HP display updates in real-time
                leftHalf.label(() -> Strings.autoFixed(unit.health, 2) + "/" + Strings.autoFixed(unit.maxHealth, 2))
                    .color(Color.white).style(Styles.outlineLabel).padTop(ui(4f));
                if(unit.type.energyCapacity > 0f){
                    leftHalf.row();
                    leftHalf.label(() -> Mathf.round(unit.energy) + "/" + Mathf.round(unit.type.energyCapacity))
                        .color(Color.valueOf("b57aff")).style(Styles.outlineLabel).padTop(ui(2f));
                }
                float remaining = PulsarDrops.remainingFraction(unit);
                if(remaining > 0f){
                    leftHalf.row();
                    Bar lifeBar = new Bar(() -> "", () -> Color.gray, () -> PulsarDrops.remainingFraction(unit));
                    leftHalf.add(lifeBar).width(ui(80f)).height(ui(4f)).padTop(ui(4f));
                }
                if(unit.hasEffect(StatusEffects.ravenAntiArmor)){
                    leftHalf.row();
                    Bar armorBreakBar = new Bar(() -> "", () -> Color.valueOf("7a1d22"),
                    () -> Mathf.clamp(unit.getDuration(StatusEffects.ravenAntiArmor) / UnitTypes.ravenAntiArmorDuration()));
                    leftHalf.add(armorBreakBar).width(ui(80f)).height(ui(4f)).padTop(ui(3f));
                }
                if(unit.hasEffect(StatusEffects.ravenMatrixLock)){
                    leftHalf.row();
                    Bar matrixBar = new Bar(() -> "", () -> Color.valueOf("1e1e1e"),
                    () -> Mathf.clamp(unit.getDuration(StatusEffects.ravenMatrixLock) / UnitTypes.ravenMatrixDuration()));
                    leftHalf.add(matrixBar).width(ui(80f)).height(ui(4f)).padTop(ui(3f));
                }
                if(UnitTypes.battlecruiserYamatoCharging(unit)){
                    leftHalf.row();
                    Bar yamatoCharge = new Bar(() -> "", () -> Color.valueOf("66e7ff"), () -> UnitTypes.battlecruiserYamatoChargeProgress(unit));
                    leftHalf.add(yamatoCharge).width(ui(80f)).height(ui(4f)).padTop(ui(3f));
                }
                if(UnitTypes.isRavenTurret(unit)){
                    leftHalf.row();
                    Bar turretLife = new Bar(() -> "", () -> Color.gray, () -> UnitTypes.ravenTurretLifeProgress(unit));
                    leftHalf.add(turretLife).width(ui(80f)).height(ui(4f)).padTop(ui(3f));
                }
            }).width(ui(120f)).padRight(ui(16f));

            //Right half: Unit info (centered)
            boolean medivacCargoOnly = (!readOnly || friendlyReadOnly) && UnitTypes.isMedivac(unit) && unit instanceof Payloadc pay && !pay.payloads().isEmpty();
            panel.table(rightHalf -> {
                rightHalf.defaults().center();
                if(medivacCargoOnly){
                    buildMedivacCargoPanel(rightHalf, unit, !readOnly);
                    return;
                }

                if(UnitTypes.isRaven(unit)){
                    rightHalf.add(detectorTag).style(Styles.outlineLabel).row();
                }

                //Unit name
                rightHalf.add(unit.type.localizedName).color(Color.white).style(Styles.outlineLabel).row();

                //Eliminated count
                rightHalf.add("Eliminated: " + (int)unit.stack.amount).color(Color.lightGray).padTop(ui(4f)).row();

                //Icons row: armor and weapons
                rightHalf.table(iconsRow -> {
                    iconsRow.defaults().size(ui(28f)).pad(ui(4f));
                    String armorLabel = Core.bundle.get("ui.armor", "Armor");
                    String weaponLabel = Core.bundle.get("ui.weapon", "Weapon");
                    String speedLabel = Core.bundle.get("ui.speed", "Speed");
                    String damageLabel = Core.bundle.get("ui.damage", "Damage");
                    String rangeLabel = Core.bundle.get("ui.range", "Range");
                    String targetLabel = Core.bundle.get("ui.target", "Target");
                    String targetAir = Core.bundle.get("ui.target.air", "Air");
                    String targetGround = Core.bundle.get("ui.target.ground", "Ground");
                    String targetGroundAir = Core.bundle.get("ui.target.groundair", "Ground & Air");
                    String multiAttackLabel = Core.bundle.get("ui.multiact", "Multi-attack");

                    //Armor icon
                    iconsRow.table(armorIcon -> {
                        Stack iconStack = new Stack();
                        iconStack.add(portraitBorderElement());
                        Image armorImage = new Image(Icon.defense);
                        armorImage.setColor(Color.orange);
                        armorImage.setScaling(Scaling.fit);
                        Table iconInner = new Table();
                        iconInner.add(armorImage).size(ui(20f));
                        iconStack.add(iconInner);
                        armorIcon.add(iconStack).size(ui(28f));
                        armorIcon.addListener(new Tooltip(t -> {
                            t.background(Styles.black6);
                            t.add(armorLabel).color(Color.yellow).row();
                            t.image().color(Pal.accent).height(ui(3f)).growX().pad(ui(4f)).row();
                            t.add(armorLabel + ": " + fixed2(unit.type.armor)).left().row();
                            if(UnitTypes.isMedivac(unit) && UnitTypes.medivacAfterburnerActive(unit)){
                                t.add(speedLabel + ": " + fixed2(UnitTypes.medivacBaseSpeed()) + "[#6fff6f](+" + fixed2(UnitTypes.medivacAfterburnerBonusSpeed()) + ")[] tiles/s").left().row();
                            }else{
                                t.add(speedLabel + ": " + fixed2(unit.type.speed) + " tiles/s").left().row();
                            }
                        }));
                    });

                    Seq<Weapon> shownWeapons = displayedWeapons(unit);
                    for(Weapon shownWeapon : shownWeapons){
                        if(shownWeapon == null || shownWeapon.bullet == null) continue;

                        boolean weaponTargetsAir = shownWeapon.bullet.collidesAir;
                        boolean weaponTargetsGround = shownWeapon.bullet.collidesGround;
                        boolean showSiegeClock = UnitTypes.isSiegeTank(unit)
                        && "precept-siege-weapon".equals(shownWeapon.name)
                        && UnitTypes.preceptIsSieged(unit);

                        iconsRow.table(weaponIcon -> {
                            Stack iconStack = new Stack();
                            iconStack.add(portraitBorderElement());

                            Image weaponImage = new Image(weaponTargetsGround ? Icon.defense : Icon.units);
                            if(weaponTargetsAir && weaponTargetsGround){
                                weaponImage = new Image(Icon.units);
                            }
                            weaponImage.setScaling(Scaling.fit);
                            Table iconInner = new Table();
                            iconInner.add(weaponImage).size(ui(20f));
                            iconStack.add(iconInner);

                            if(showSiegeClock){
                                iconStack.add(new Element(){
                                    @Override
                                    public void draw(){
                                        float cooldown = UnitTypes.preceptSiegeCooldown(unit);
                                        if(cooldown <= 0.001f) return;

                                        float total = Math.max(UnitTypes.preceptSiegeShotCooldownDuration(), 0.001f);
                                        float cx = x + width / 2f;
                                        float cy = y + height / 2f;
                                        float fixedAngle = 90f;
                                        float progress = 1f - Mathf.clamp(cooldown / total);
                                        float movingAngle = fixedAngle - progress * 360f;
                                        float handLen = width * 0.20f;

                                        Draw.color(Color.valueOf("b6bcc5"));
                                        Lines.stroke(ui(1.25f));
                                        Lines.line(cx, cy, cx + Angles.trnsx(fixedAngle, handLen), cy + Angles.trnsy(fixedAngle, handLen));
                                        Lines.line(cx, cy, cx + Angles.trnsx(movingAngle, handLen), cy + Angles.trnsy(movingAngle, handLen));

                                        Draw.reset();
                                    }
                                });
                            }

                            weaponIcon.add(iconStack).size(ui(28f));
                            weaponIcon.addListener(new Tooltip(t -> {
                                t.background(Styles.black6);
                                t.add(weaponLabel).color(Color.yellow).row();
                                t.image().color(Pal.accent).height(ui(3f)).growX().pad(ui(4f)).row();

                                float damage = displayedWeaponDamage(unit, shownWeapon);
                                float range = displayedWeaponRange(unit, shownWeapon);
                                Seq<String> bonusLines = displayedDamageBonusLines(unit, shownWeapon);

                                t.add(damageLabel + ": " + fixed2(damage)).left().row();
                                for(String line : bonusLines){
                                    t.add(line).left().row();
                                }
                                t.add(rangeLabel + ": " + fixed2(range / 8f) + " tiles").left().row();

                                if(shownWeapon.reload > 0){
                                    t.add(speedLabel + ": " + fixed2(shownWeapon.reload / 60f) + "s").left().row();
                                }

                                int multiAttack = displayedMultiAttack(shownWeapon);
                                if(multiAttack > 1){
                                    t.add(multiAttackLabel + ": " + multiAttack + "x").left().row();
                                }

                                String target = weaponTargetsAir && weaponTargetsGround ? targetGroundAir :
                                weaponTargetsAir ? targetAir : targetGround;
                                t.add(targetLabel + ": " + target).left().row();
                            }));
                        });
                    }
                }).padTop(ui(8f)).row();

                //Bottom row: armor type and unit class
                rightHalf.table(bottomRow -> {
                    String armorName = armorTypeName(displayedArmorType(unit.type));
                    String className = displayedUnitClassNames(unit.type);
                    if(UnitTypes.isRavenTurret(unit)){
                        bottomRow.add(armorName + " " + className + " Building Summon").color(Color.white);
                    }else{
                        bottomRow.add(armorName + " " + className).color(Color.white);
                    }
                }).padTop(ui(8f));

                if((!readOnly || friendlyReadOnly) && UnitTypes.isMedivac(unit)){
                    rightHalf.row();
                    buildMedivacCargoPanel(rightHalf, unit, !readOnly);
                }
            }).grow().center();
        }).growX().height(singlePanelHeight());
    }

    private void buildMedivacCargoPanel(Table rightHalf, Unit unit, boolean interactive){
        if(!(unit instanceof Payloadc payload) || payload.payloads().isEmpty()) return;
        WidgetGroup cargoGrid = buildPayloadGrid(unit, payload.payloads());
        if(cargoGrid == null) return;
        rightHalf.add(cargoGrid).size(cargoGrid.getWidth(), cargoGrid.getHeight()).padTop(ui(8f));
    }

    private @Nullable WidgetGroup buildPayloadGrid(Unit carrier, Seq<Payload> payloads){
        if(payloads == null || payloads.isEmpty()) return null;

        float gridWidth = PAYLOAD_GRID_COLS * PAYLOAD_SLOT_SIZE + (PAYLOAD_GRID_COLS - 1) * PAYLOAD_SLOT_PAD;
        float gridHeight = PAYLOAD_GRID_ROWS * PAYLOAD_SLOT_SIZE + (PAYLOAD_GRID_ROWS - 1) * PAYLOAD_SLOT_PAD;
        WidgetGroup grid = new WidgetGroup();
        grid.setTransform(false);
        grid.setSize(gridWidth, gridHeight);

        for(int col = 0; col < PAYLOAD_GRID_COLS; col++){
            for(int row = 0; row < PAYLOAD_GRID_ROWS; row++){
                Table cell = new Table(Styles.black6);
                Stack stack = new Stack();
                stack.add(portraitBorderElement());
                cell.add(stack).size(PAYLOAD_SLOT_SIZE);
                cell.setSize(PAYLOAD_SLOT_SIZE, PAYLOAD_SLOT_SIZE);
                cell.setPosition(payloadGridX(col), payloadGridY(row, 1));
                grid.addChild(cell);
            }
        }

        Seq<Payload> orderedPayloads = payloads.copy();
        orderedPayloads.sort((a, b) -> Integer.compare(payloadDisplaySlots(carrier, b), payloadDisplaySlots(carrier, a)));

        boolean[][] occupied = new boolean[PAYLOAD_GRID_COLS][PAYLOAD_GRID_ROWS];
        for(int i = 0; i < orderedPayloads.size; i++){
            PayloadLayoutEntry entry = layoutPayloadEntry(carrier, orderedPayloads.get(i), occupied);
            if(entry == null) continue;
            Table cell = buildPayloadEntryCell(entry);
            cell.setPosition(payloadGridX(entry.col), payloadGridY(entry.row, entry.rowSpan));
            grid.addChild(cell);
        }

        return grid;
    }

    private @Nullable PayloadLayoutEntry layoutPayloadEntry(Unit carrier, Payload payload, boolean[][] occupied){
        UnlockableContent content = payloadDisplayContent(payload);
        if(content == null) return null;

        int slots = Mathf.clamp(payloadDisplaySlots(carrier, payload), 1, PAYLOAD_GRID_COLS * PAYLOAD_GRID_ROWS);
        int rowSpan = payloadDisplayRowSpan(slots);
        int colSpan = Math.min(PAYLOAD_GRID_COLS, Math.max(1, Mathf.ceil((float)slots / rowSpan)));

        if(rowSpan == 1){
            for(int col = 0; col < PAYLOAD_GRID_COLS; col++){
                for(int row = 0; row < PAYLOAD_GRID_ROWS; row++){
                    if(!payloadRectFree(occupied, col, row, colSpan, rowSpan)) continue;
                    fillPayloadRect(occupied, col, row, colSpan, rowSpan);
                    PayloadLayoutEntry entry = new PayloadLayoutEntry();
                    entry.payload = payload;
                    entry.content = content;
                    entry.col = col;
                    entry.row = row;
                    entry.colSpan = colSpan;
                    entry.rowSpan = rowSpan;
                    return entry;
                }
            }
        }else{
            for(int col = 0; col <= PAYLOAD_GRID_COLS - colSpan; col++){
                if(!payloadRectFree(occupied, col, 0, colSpan, rowSpan)) continue;
                fillPayloadRect(occupied, col, 0, colSpan, rowSpan);
                PayloadLayoutEntry entry = new PayloadLayoutEntry();
                entry.payload = payload;
                entry.content = content;
                entry.col = col;
                entry.row = 0;
                entry.colSpan = colSpan;
                entry.rowSpan = rowSpan;
                return entry;
            }
        }

        return null;
    }

    private boolean payloadRectFree(boolean[][] occupied, int startCol, int startRow, int colSpan, int rowSpan){
        if(startCol < 0 || startRow < 0) return false;
        if(startCol + colSpan > PAYLOAD_GRID_COLS) return false;
        if(startRow + rowSpan > PAYLOAD_GRID_ROWS) return false;

        for(int col = startCol; col < startCol + colSpan; col++){
            for(int row = startRow; row < startRow + rowSpan; row++){
                if(occupied[col][row]) return false;
            }
        }
        return true;
    }

    private void fillPayloadRect(boolean[][] occupied, int startCol, int startRow, int colSpan, int rowSpan){
        for(int col = startCol; col < startCol + colSpan; col++){
            for(int row = startRow; row < startRow + rowSpan; row++){
                occupied[col][row] = true;
            }
        }
    }

    private float payloadGridX(int col){
        return col * (PAYLOAD_SLOT_SIZE + PAYLOAD_SLOT_PAD);
    }

    private float payloadGridY(int row, int rowSpan){
        float height = rowSpan * PAYLOAD_SLOT_SIZE + (rowSpan - 1) * PAYLOAD_SLOT_PAD;
        float topOffset = row * (PAYLOAD_SLOT_SIZE + PAYLOAD_SLOT_PAD);
        float totalHeight = PAYLOAD_GRID_ROWS * PAYLOAD_SLOT_SIZE + (PAYLOAD_GRID_ROWS - 1) * PAYLOAD_SLOT_PAD;
        return totalHeight - topOffset - height;
    }

    private int payloadDisplaySlots(Unit carrier, Payload payload){
        if(UnitTypes.isMedivac(carrier) && payload instanceof UnitPayload up && up.unit != null){
            return UnitTypes.medivacUnitSlotCost(up.unit.type);
        }

        float tiles = Math.max(1f, payload.size() / tilesize);
        int area = Mathf.ceil(tiles * tiles);
        if(area >= 8) return 8;
        if(area >= 4) return 4;
        if(area >= 2) return 2;
        return 1;
    }

    private int payloadDisplayRowSpan(int slots){
        return slots <= 1 ? 1 : 2;
    }

    private @Nullable UnlockableContent payloadDisplayContent(Payload payload){
        if(payload instanceof UnitPayload up && up.unit != null && up.unit.type != null){
            return up.unit.type;
        }
        if(payload instanceof BuildPayload bp && bp.build != null){
            return displayBlock(bp.build);
        }
        return null;
    }

    private String payloadDisplayName(UnlockableContent content){
        if(content instanceof UnitType type){
            return unitDisplayName(type);
        }
        return content.localizedName;
    }

    private TextureRegion payloadDisplayIcon(UnlockableContent content){
        if(content instanceof UnitType type){
            return type.uiIcon;
        }
        if(content instanceof Block block){
            return block.uiIcon;
        }
        return content.uiIcon;
    }

    private Table buildPayloadEntryCell(PayloadLayoutEntry entry){
        float width = entry.colSpan * PAYLOAD_SLOT_SIZE + (entry.colSpan - 1) * PAYLOAD_SLOT_PAD;
        float height = entry.rowSpan * PAYLOAD_SLOT_SIZE + (entry.rowSpan - 1) * PAYLOAD_SLOT_PAD;
        Table cell = new Table(Styles.black6);
        Stack stack = new Stack();
        stack.add(portraitBorderElement());

        TextureRegion iconRegion = payloadDisplayIcon(entry.content);
        if(iconRegion != null){
            Image icon = new Image(iconRegion);
            icon.setScaling(Scaling.fit);
            Table iconWrap = new Table();
            iconWrap.add(icon).size(Math.max(0f, width - ui(8f)), Math.max(0f, height - ui(8f)));
            stack.add(iconWrap);
        }

        cell.add(stack).size(width, height);
        cell.setSize(width, height);
        cell.addListener(new Tooltip(t -> {
            t.background(Styles.black6);
            t.add(payloadDisplayName(entry.content)).style(Styles.outlineLabel).color(Color.white);
        }));
        return cell;
    }

    private void buildBuildingInfoPanel(Building building, boolean readOnly){
        gridTable.clear();
        Block display = displayBlock(building);
        float armor = displayArmor(building, display);
        float maxHealth = displayMaxHealth(building);
        CoreBuild core = building instanceof CoreBuild ? (CoreBuild)building : null;
        ConstructBlock.ConstructBuild cons = building instanceof ConstructBlock.ConstructBuild ? (ConstructBlock.ConstructBuild)building : null;
        boolean incomplete = cons != null && cons.current != null && cons.current != Blocks.air && cons.progress < 1f;
        boolean friendlyReadOnly = friendlyReadOnly(building.team, readOnly);
        boolean queueReadable = !readOnly || friendlyReadOnly;
        boolean showCoreQueue = core != null && ((!readOnly && ui.hudfrag.abilityPanel != null && ui.hudfrag.abilityPanel.isCoreBuildPage()) || (friendlyReadOnly && core.unitQueue != null && !core.unitQueue.isEmpty()));
        UnitFactory.UnitFactoryBuild factory = building instanceof UnitFactory.UnitFactoryBuild ? (UnitFactory.UnitFactoryBuild)building : null;
        boolean showFactoryQueue = queueReadable && factory != null && factory.sc2QueueEnabled();
        boolean showArmoryQueue = queueReadable && building.block == Blocks.siliconCrucible && armoryQueueActive(building.team);
        boolean showFusionCoreQueue = queueReadable && building.block == Blocks.surgeCrucible && fusionCoreQueueActive(building.team);
        boolean showEngineeringQueue = queueReadable && building.block == Blocks.multiPress && engineeringQueueActive(building.team);
        boolean showGhostAcademyQueue = queueReadable && building.block == Blocks.launchPad && ghostAcademyQueueActive(building);
        boolean showTechLabQueue = queueReadable && techLabQueueActive(building);

        gridTable.table(panel -> {
            panel.background(Styles.black6);
            panel.margin(ui(8f));

            //Left half: Building icon
            panel.table(leftHalf -> {
                leftHalf.defaults().center();
                Stack buildIconStack = new Stack();
                buildIconStack.add(portraitBorderElement());
                Table iconWrap = new Table();
                iconWrap.image(display.uiIcon).size(ui(76f));
                buildIconStack.add(iconWrap);
                leftHalf.add(buildIconStack).size(ui(80f)).row();
                leftHalf.label(() -> {
                    if(incomplete){
                        return Mathf.round(building.health) + "/" + Mathf.round(maxHealth);
                    }
                    return Strings.autoFixed(building.health, 2) + "/" + Strings.autoFixed(maxHealth, 2);
                })
                    .style(Styles.outlineLabel).color(Color.lightGray).padTop(ui(6f)).row();
                if(core != null && core.block == Blocks.coreOrbital){
                    leftHalf.label(() -> Mathf.round(Math.max(core.orbitalEnergy, 0f)) + "/" + Mathf.round(CoreBlock.orbitalEnergyCap))
                        .style(Styles.outlineLabel).color(Color.valueOf("b57aff")).padTop(ui(4f)).row();
                }

                if(display == Blocks.ventCondenser){
                    Tile tile = building.tile;
                    if(tile != null && tile.floor() instanceof SteamVent vent){
                        Tile data = vent.dataTile(tile);
                        String remainingLabel = Core.bundle.get("steamvent.remaining", "Remaining");
                        String infiniteLabel = Core.bundle.get("ui.infinite", "Infinite");
                        leftHalf.label(() -> {
                            if(vent.isInfinite(data)){
                                return remainingLabel + ": " + infiniteLabel;
                            }
                            return remainingLabel + ": " + vent.getReserves(data);
                        }).style(Styles.outlineLabel).color(Color.lightGray).padTop(ui(6f));
                    }
                }
            }).width(ui(120f)).padRight(ui(16f));

            //Right half: Building info (centered)
            panel.table(rightHalf -> {
                rightHalf.defaults().center();

                if(showCoreQueue){
                    buildCoreQueuePanel(rightHalf, core);
                }else if(showFactoryQueue){
                    buildFactoryQueuePanel(rightHalf, factory);
                }else if(showArmoryQueue){
                    buildArmoryQueuePanel(rightHalf, building.team);
                }else if(showFusionCoreQueue){
                    buildFusionCoreQueuePanel(rightHalf, building.team);
                }else if(showEngineeringQueue){
                    buildEngineeringQueuePanel(rightHalf, building.team);
                }else if(showGhostAcademyQueue){
                    buildGhostAcademyQueuePanel(rightHalf, building);
                }else if(showTechLabQueue){
                    buildTechLabQueuePanel(rightHalf, building);
                }else if(incomplete){
                    buildConstructProgressPanel(rightHalf, cons);
                }else{
                    if(display.stealthDetectionRange > 0f){
                        rightHalf.add(detectorTag).style(Styles.outlineLabel).row();
                    }
                    //Building name
                    rightHalf.add(display.localizedName).color(Color.white).style(Styles.outlineLabel).row();

                    //Icons row: armor and weapons
                    rightHalf.table(iconsRow -> {
                        iconsRow.defaults().size(ui(28f)).pad(ui(4f));
                        String armorLabel = Core.bundle.get("ui.armor", "Armor");
                        String weaponLabel = Core.bundle.get("ui.weapon", "Weapon");
                        String speedLabel = Core.bundle.get("ui.speed", "Speed");
                        String damageLabel = Core.bundle.get("ui.damage", "Damage");
                        String rangeLabel = Core.bundle.get("ui.range", "Range");
                        String targetLabel = Core.bundle.get("ui.target", "Target");
                        String healthLabel = Core.bundle.get("ui.health", "Health");
                        String targetAir = Core.bundle.get("ui.target.air", "Air");
                        String targetGround = Core.bundle.get("ui.target.ground", "Ground");
                        String targetGroundAir = Core.bundle.get("ui.target.groundair", "Ground & Air");
                        //Armor icon
                        iconsRow.table(armorIcon -> {
                            Stack iconStack = new Stack();
                            iconStack.add(portraitBorderElement());
                            Image armorImage = new Image(Icon.defense);
                            armorImage.setColor(Color.orange);
                            armorImage.setScaling(Scaling.fit);
                            Table iconInner = new Table();
                            iconInner.add(armorImage).size(ui(20f));
                            iconStack.add(iconInner);
                            armorIcon.add(iconStack).size(ui(28f));
                            armorIcon.addListener(new Tooltip(t -> {
                                t.background(Styles.black6);
                                t.add(armorLabel).color(Color.yellow).row();
                                t.image().color(Pal.accent).height(ui(3f)).growX().pad(ui(4f)).row();
                                t.add(armorLabel + ": " + fixed2(armor)).left().row();
                                t.add(healthLabel + ": " + fixed2(maxHealth)).left().row();
                            }));
                        });

                        //Weapon icon (only if this block can attack)
                        if(building.block.attacks){
                            iconsRow.table(weaponIcon -> {
                                Stack iconStack = new Stack();
                                iconStack.add(portraitBorderElement());
                                Image weaponImage = new Image(Icon.units);
                                weaponImage.setScaling(Scaling.fit);
                                Table iconInner = new Table();
                                iconInner.add(weaponImage).size(ui(20f));
                                iconStack.add(iconInner);
                                weaponIcon.add(iconStack).size(ui(28f));
                                weaponIcon.addListener(new Tooltip(t -> {
                                    t.background(Styles.black6);
                                    t.add(weaponLabel).color(Color.yellow).row();
                                    t.image().color(Pal.accent).height(ui(3f)).growX().pad(ui(4f)).row();

                                    float range = -1f;
                                    float reload = -1f;
                                    Float damage = null;
                                    String target = null;

                                    if(building instanceof BaseTurret.BaseTurretBuild baseBuild){
                                        range = baseBuild.range();
                                    }

                                    if(building.block instanceof Turret turret && building instanceof Turret.TurretBuild turretBuild){
                                        BulletType bullet = turretBuild.peekAmmo();
                                        if(bullet != null){
                                            float dmg = bullet.damage;
                                            if(bullet.splashDamage > dmg) dmg = bullet.splashDamage;
                                            damage = dmg;
                                        }

                                        reload = turret.reload;
                                        target = turret.targetAir && turret.targetGround ? targetGroundAir :
                                            turret.targetAir ? targetAir : targetGround;
                                    }else if(building.block instanceof TractorBeamTurret tractor){
                                        if(tractor.damage > 0f){
                                            damage = tractor.damage * 60f;
                                        }
                                        target = tractor.targetAir && tractor.targetGround ? targetGroundAir :
                                            tractor.targetAir ? targetAir : targetGround;
                                    }else if(building.block instanceof BaseTurret){
                                        target = targetGround;
                                    }

                                    if(damage != null){
                                        t.add(damageLabel + ": " + fixed2(damage)).left().row();
                                    }
                                    if(range >= 0f){
                                        t.add(rangeLabel + ": " + fixed2(range / 8f) + " tiles").left().row();
                                    }
                                    if(reload > 0f){
                                        t.add(speedLabel + ": " + fixed2(reload / 60f) + "s").left().row();
                                    }
                                    if(target != null){
                                        t.add(targetLabel + ": " + target).left().row();
                                    }
                                }));
                            });
                        }
                    }).padTop(ui(8f)).row();

                    //Bottom row: armor type and building category
                    String armorName = armorTypeName(armorTypeFor(display));
                    String className = unitClassName(UnitClass.mechanical);
                    String buildingLabel = Core.bundle.get("ui.building", "Building");
                    rightHalf.add(armorName + " " + className + " " + buildingLabel)
                        .color(Color.white).padTop(ui(8f));
                }
            }).grow().center();
        }).growX().height(singlePanelHeight());
    }

    private void buildCoreQueuePanel(Table rightHalf, CoreBuild core){
        float slotSize = queueTopSlotSize();
        float barWidth = queueBarWidth(slotSize);
        float barHeight = queueBarHeight();

        rightHalf.table(queueRoot -> {
            queueRoot.left();
            queueRoot.defaults().left();

            int activeSlots = Math.max(1, core.activeUnitSlots());
            int totalSlots = Math.max(activeSlots, core.queueSlots());
            int queuedSlots = Math.max(0, totalSlots - activeSlots);

            Table row1 = new Table();
            row1.left();
            row1.add(buildQueueSlot(core.queuedUnit(0), slotSize)).size(slotSize, slotSize);
            row1.add(buildQueueProgress(core, 0, barWidth, barHeight)).size(barWidth, barHeight).bottom().left().pad(0f).padBottom(ui(2f));

            if(activeSlots > 1){
                row1.add(buildQueueSlot(core.queuedUnit(1), slotSize)).size(slotSize, slotSize);
                row1.add(buildQueueProgress(core, 1, barWidth, barHeight)).size(barWidth, barHeight).bottom().left().pad(0f).padBottom(ui(2f));
            }

            queueRoot.add(row1).left().row();
            if(queuedSlots > 0){
                float queuedSlotSize = queueBottomSlotSize(activeSlots, queuedSlots, slotSize, barWidth);
                Table row2 = new Table();
                row2.left();
                for(int i = activeSlots; i < totalSlots; i++){
                    row2.add(buildQueueSlot(core.queuedUnit(i), queuedSlotSize)).size(queuedSlotSize, queuedSlotSize);
                }
                queueRoot.add(row2).left();
            }
        }).left();
    }

    private Bar buildQueueProgress(CoreBuild core, int slot, float width, float height){
        Bar progress = new Bar(() -> "", () -> Pal.accent, () -> core.unitProgressFraction(slot));
        progress.addListener(new Tooltip(t -> {
            t.background(Styles.black6);
            t.label(() -> formatTimePair(core.unitProgressSeconds(slot), core.unitProgressTotalSeconds(slot)))
                .style(Styles.outlineLabel).color(Color.white);
        }));
        progress.setSize(width, height);
        return progress;
    }

    private void buildFactoryQueuePanel(Table rightHalf, UnitFactory.UnitFactoryBuild factory){
        float slotSize = queueTopSlotSize();
        float barWidth = queueBarWidth(slotSize);
        float barHeight = queueBarHeight();

        rightHalf.table(queueRoot -> {
            queueRoot.left();
            queueRoot.defaults().left();

            if(factory.isAddonBuilding()){
                Block addon = factory.addonBuildingBlock();
                if(addon != null){
                    Table row1 = new Table();
                    row1.left();
                    row1.add(buildQueueSlot(addon, slotSize)).size(slotSize, slotSize);
                    row1.add(buildAddonProgress(factory, barWidth, barHeight)).size(barWidth, barHeight).bottom().left().pad(0f).padBottom(ui(2f));
                    queueRoot.add(row1).left();
                }
                return;
            }

            int activeSlots = Math.max(1, factory.activeUnitSlots());
            int totalSlots = Math.max(activeSlots, factory.queueSlots());
            int queuedSlots = Math.max(0, totalSlots - activeSlots);

            Table row1 = new Table();
            row1.left();
            row1.add(buildQueueSlot(factory.queuedUnit(0), slotSize)).size(slotSize, slotSize);
            row1.add(buildQueueProgress(factory, 0, barWidth, barHeight)).size(barWidth, barHeight).bottom().left().pad(0f).padBottom(ui(2f));

            if(activeSlots > 1){
                row1.add(buildQueueSlot(factory.queuedUnit(1), slotSize)).size(slotSize, slotSize);
                row1.add(buildQueueProgress(factory, 1, barWidth, barHeight)).size(barWidth, barHeight).bottom().left().pad(0f).padBottom(ui(2f));
            }

            queueRoot.add(row1).left().row();
            if(queuedSlots > 0){
                float queuedSlotSize = queueBottomSlotSize(activeSlots, queuedSlots, slotSize, barWidth);
                Table row2 = new Table();
                row2.left();
                for(int i = activeSlots; i < totalSlots; i++){
                    row2.add(buildQueueSlot(factory.queuedUnit(i), queuedSlotSize)).size(queuedSlotSize, queuedSlotSize);
                }
                queueRoot.add(row2).left();
            }
        }).left();
    }

    private Bar buildQueueProgress(UnitFactory.UnitFactoryBuild factory, int slot, float width, float height){
        Bar progress = new Bar(() -> "", () -> Pal.accent, () -> factory.unitProgressFraction(slot));
        progress.addListener(new Tooltip(t -> {
            t.background(Styles.black6);
            t.label(() -> formatTimePair(factory.unitProgressSeconds(slot), factory.unitProgressTotalSeconds(slot)))
                .style(Styles.outlineLabel).color(Color.white);
        }));
        progress.setSize(width, height);
        return progress;
    }

    private Bar buildAddonProgress(UnitFactory.UnitFactoryBuild factory, float width, float height){
        Bar progress = new Bar(() -> "", () -> Pal.accent, () -> factory.addonBuildFraction());
        progress.addListener(new Tooltip(t -> {
            t.background(Styles.black6);
            t.label(() -> formatTimePair(factory.addonBuildSeconds(), factory.addonBuildTotalSeconds()))
                .style(Styles.outlineLabel).color(Color.white);
        }));
        progress.setSize(width, height);
        return progress;
    }

    private boolean armoryQueueActive(@Nullable Team team){
        return ResearchQueueService.armoryAnyResearching(team);
    }

    private boolean fusionCoreQueueActive(@Nullable Team team){
        return ResearchQueueService.fusionCoreAnyResearching(team);
    }

    private void buildArmoryQueuePanel(Table rightHalf, Team team){
        float slotSize = queueTopSlotSize();
        float barWidth = queueBarWidth(slotSize);
        float barHeight = queueBarHeight();
        int queuedSlots = 4;
        float queuedSlotSize = queueBottomSlotSize(1, queuedSlots, slotSize, barWidth);

        rightHalf.table(queueRoot -> {
            queueRoot.left();
            queueRoot.defaults().left();

            Block activeBlock = ResearchQueueService.armoryActiveResearchBlock(team);
            Floatp activeProgress = () -> ResearchQueueService.armoryActiveResearchProgress(team);
            float activeTotal = ResearchQueueService.armoryActiveResearchDuration(team) / 60f;

            Table row1 = new Table();
            row1.left();
            Floatp progress = activeProgress;
            float total = activeTotal;
            row1.add(buildQueueSlot(activeBlock, slotSize)).size(slotSize, slotSize);
            row1.add(buildQueueProgress(
                progress,
                () -> progress.get() * total,
                () -> total,
                barWidth, barHeight
            )).size(barWidth, barHeight).bottom().left().pad(0f).padBottom(ui(2f));

            Table row2 = new Table();
            row2.left();
            int queued = ResearchQueueService.armoryQueuedCount(team);
            for(int i = 0; i < queuedSlots; i++){
                Block queuedBlock = i < queued ? ResearchQueueService.armoryQueuedBlock(team, i) : null;
                row2.add(buildQueueSlot(queuedBlock, queuedSlotSize)).size(queuedSlotSize, queuedSlotSize);
            }

            queueRoot.add(row1).left().row();
            queueRoot.add(row2).left();
        }).left();
    }

    private void buildFusionCoreQueuePanel(Table rightHalf, Team team){
        float slotSize = queueTopSlotSize();
        float barWidth = queueBarWidth(slotSize);
        float barHeight = queueBarHeight();
        int queuedSlots = 4;
        float queuedSlotSize = queueBottomSlotSize(1, queuedSlots, slotSize, barWidth);

        rightHalf.table(queueRoot -> {
            queueRoot.left();
            queueRoot.defaults().left();

            Sc2ResearchSpec activeSpec = ResearchQueueService.fusionCoreActiveResearch(team);
            Floatp progress = activeSpec == null ? () -> 0f : () -> activeSpec.progress(team);
            float total = activeSpec == null ? 0f : activeSpec.duration(team) / 60f;

            Table row1 = new Table();
            row1.left();
            row1.add(buildQueueSlot(activeSpec == null ? null : activeSpec.iconUnit(), slotSize)).size(slotSize, slotSize);
            row1.add(buildQueueProgress(
                progress,
                () -> progress.get() * total,
                () -> total,
                barWidth, barHeight
            )).size(barWidth, barHeight).bottom().left().pad(0f).padBottom(ui(2f));

            Table row2 = new Table();
            row2.left();
            int queued = ResearchQueueService.fusionCoreQueuedCount(team);
            for(int i = 0; i < queuedSlots; i++){
                UnitType queuedType = i < queued ? ResearchQueueService.fusionCoreQueuedUnit(team, i) : null;
                row2.add(buildQueueSlot(queuedType, queuedSlotSize)).size(queuedSlotSize, queuedSlotSize);
            }

            queueRoot.add(row1).left().row();
            queueRoot.add(row2).left();
        }).left();
    }

    private boolean engineeringQueueActive(@Nullable Team team){
        return ResearchQueueService.engineeringAnyResearching(team);
    }

    private boolean ghostAcademyQueueActive(@Nullable Building build){
        return build != null
            && build.block == Blocks.launchPad
            && (UnitTypes.ghostCamoAnyResearching(build.team) || UnitTypes.ghostWarheadProducing(build));
    }

    private void buildEngineeringQueuePanel(Table rightHalf, Team team){
        float slotSize = queueTopSlotSize();
        float barWidth = queueBarWidth(slotSize);
        float barHeight = queueBarHeight();
        int queuedSlots = 4;
        float queuedSlotSize = queueBottomSlotSize(1, queuedSlots, slotSize, barWidth);

        rightHalf.table(queueRoot -> {
            queueRoot.left();
            queueRoot.defaults().left();

            Block activeBlock = ResearchQueueService.engineeringActiveResearchBlock(team);
            Floatp activeProgress = () -> ResearchQueueService.engineeringActiveResearchProgress(team);
            float activeTotal = ResearchQueueService.engineeringActiveResearchDuration(team) / 60f;

            Table row1 = new Table();
            row1.left();
            Floatp progress = activeProgress;
            float total = activeTotal;
            row1.add(buildQueueSlot(activeBlock, slotSize)).size(slotSize, slotSize);
            row1.add(buildQueueProgress(
                progress,
                () -> progress.get() * total,
                () -> total,
                barWidth, barHeight
            )).size(barWidth, barHeight).bottom().left().pad(0f).padBottom(ui(2f));

            Table row2 = new Table();
            row2.left();
            int queued = ResearchQueueService.engineeringQueuedCount(team);
            for(int i = 0; i < queuedSlots; i++){
                Block queuedBlock = i < queued ? ResearchQueueService.engineeringQueuedBlock(team, i) : null;
                row2.add(buildQueueSlot(queuedBlock, queuedSlotSize)).size(queuedSlotSize, queuedSlotSize);
            }

            queueRoot.add(row1).left().row();
            queueRoot.add(row2).left();
        }).left();
    }

    private boolean techLabQueueActive(@Nullable Building build){
        if(build == null || build.block != Blocks.memoryBank) return false;
        UnitFactory.UnitFactoryBuild attached = attachedFactoryForTechLab(build);
        if(attached == null) return false;
        return ResearchQueueService.techLabAnyResearching(build.team, attached.block);
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

    private void buildTechLabQueuePanel(Table rightHalf, Building build){
        float slotSize = queueTopSlotSize();
        float barWidth = queueBarWidth(slotSize);
        float barHeight = queueBarHeight();
        int queuedSlots = 4;
        float queuedSlotSize = queueBottomSlotSize(1, queuedSlots, slotSize, barWidth);
        Team team = build.team;
        UnitFactory.UnitFactoryBuild attached = attachedFactoryForTechLab(build);
        Block attachedFactory = attached == null ? null : attached.block;

        rightHalf.table(queueRoot -> {
            queueRoot.left();
            queueRoot.defaults().left();

            Sc2ResearchSpec activeSpec = ResearchQueueService.techLabActiveResearch(team, attachedFactory);
            Floatp progress = activeSpec == null ? () -> 0f : () -> activeSpec.progress(team);
            float total = activeSpec == null ? 0f : activeSpec.duration(team) / 60f;

            Table row1 = new Table();
            row1.left();
            row1.add(buildQueueSlot(activeSpec == null ? null : activeSpec.iconUnit(), slotSize)).size(slotSize, slotSize);
            row1.add(buildQueueProgress(
                progress,
                () -> progress.get() * total,
                () -> total,
                barWidth, barHeight
            )).size(barWidth, barHeight).bottom().left().pad(0f).padBottom(ui(2f));

            Table row2 = new Table();
            row2.left();
            int queued = ResearchQueueService.techLabQueuedCount(team, attachedFactory);
            for(int i = 0; i < queuedSlots; i++){
                UnitType queuedType = i < queued ? ResearchQueueService.techLabQueuedUnit(team, attachedFactory, i) : null;
                row2.add(buildQueueSlot(queuedType, queuedSlotSize)).size(queuedSlotSize, queuedSlotSize);
            }

            queueRoot.add(row1).left().row();
            queueRoot.add(row2).left();
        }).left();
    }

    private void buildGhostAcademyQueuePanel(Table rightHalf, Building build){
        float slotSize = queueTopSlotSize();
        float barWidth = queueBarWidth(slotSize);
        float barHeight = queueBarHeight();

        rightHalf.table(queueRoot -> {
            queueRoot.defaults().center();
            boolean added = false;

            if(UnitTypes.ghostCamoAnyResearching(build.team)){
                Table row = new Table();
                row.defaults().size(slotSize).pad(ui(2f));
                row.add(buildQueueSlot(Blocks.launchPad, slotSize));
                row.add(buildQueueProgress(
                    () -> UnitTypes.ghostCamoResearchProgress(build.team),
                    () -> UnitTypes.ghostCamoResearchProgress(build.team) * (UnitTypes.ghostCamoResearchDuration() / 60f),
                    () -> UnitTypes.ghostCamoResearchDuration() / 60f,
                    barWidth, barHeight
                )).size(barWidth, barHeight).bottom().left().pad(0f).padBottom(ui(2f));
                queueRoot.add(row).row();
                added = true;
            }

            if(UnitTypes.ghostWarheadProducing(build)){
                Table row = new Table();
                row.defaults().size(slotSize).pad(ui(2f));
                row.add(buildQueueSlot(Blocks.launchPad, slotSize));
                row.add(buildQueueProgress(
                    () -> UnitTypes.ghostWarheadProductionProgress(build),
                    () -> UnitTypes.ghostWarheadProductionProgress(build) * (UnitTypes.ghostWarheadBuildDuration() / 60f),
                    () -> UnitTypes.ghostWarheadBuildDuration() / 60f,
                    barWidth, barHeight
                )).size(barWidth, barHeight).bottom().left().pad(0f).padBottom(ui(2f));
                queueRoot.add(row);
                added = true;
            }

            if(!added){
                queueRoot.add();
            }
        });
    }

    private Bar buildQueueProgress(Floatp progressValue, Floatp currentSeconds, Floatp totalSeconds, float width, float height){
        Bar progress = new Bar(() -> "", () -> Pal.accent, () -> Mathf.clamp(progressValue.get()));
        progress.addListener(new Tooltip(t -> {
            t.background(Styles.black6);
            t.label(() -> formatTimePair(currentSeconds.get(), totalSeconds.get()))
                .style(Styles.outlineLabel).color(Color.white);
        }));
        progress.setSize(width, height);
        return progress;
    }

    private float queueTopSlotSize(){
        return QUEUE_SLOT_SIZE;
    }

    private float queueBarWidth(float slotSize){
        return slotSize * 2f;
    }

    private float queueBarHeight(){
        return ui(3f);
    }

    private float queueBottomSlotSize(int activeSlots, int queuedSlots, float slotSize, float barWidth){
        return slotSize;
    }

    private Table buildQueueSlot(UnitType type, float size){
        return buildQueueSlot(type, size, size);
    }

    private Table buildQueueSlot(@Nullable UnitType type, float width, float height){
        Table slot = new Table(Styles.black6);
        Stack stack = new Stack();
        stack.add(portraitBorderElement());
        if(type != null){
            Image icon = new Image(type.uiIcon);
            icon.setScaling(Scaling.fit);
            Table iconWrap = new Table();
            iconWrap.add(icon).size(Math.max(0f, Math.min(width, height) - ui(8f)));
            stack.add(iconWrap);
            slot.addListener(new Tooltip(t -> {
                t.background(Styles.black6);
                t.add(unitDisplayName(type)).style(Styles.outlineLabel).color(Color.white);
            }));
        }
        slot.add(stack).size(width, height);
        slot.setSize(width, height);
        return slot;
    }

    private Table buildQueueSlot(Block block, float size){
        return buildQueueSlot(block, size, size);
    }

    private Table buildQueueSlot(@Nullable Block block, float width, float height){
        Table slot = new Table(Styles.black6);
        Stack stack = new Stack();
        stack.add(portraitBorderElement());
        if(block != null){
            Image icon = new Image(block.uiIcon);
            icon.setScaling(Scaling.fit);
            Table iconWrap = new Table();
            iconWrap.add(icon).size(Math.max(0f, Math.min(width, height) - ui(8f)));
            stack.add(iconWrap);
            slot.addListener(new Tooltip(t -> {
                t.background(Styles.black6);
                t.add(block.localizedName).style(Styles.outlineLabel).color(Color.white);
            }));
        }
        slot.add(stack).size(width, height);
        slot.setSize(width, height);
        return slot;
    }

    private void buildConstructProgressPanel(Table rightHalf, ConstructBlock.ConstructBuild cons){
        if(cons == null || cons.current == null || cons.current == Blocks.air) return;
        float boxSize = ui(54f);
        float barWidth = (boxSize - ui(12f)) * 2f;
        float barHeight = ui(2.5f);

        rightHalf.table(panel -> {
            Table box = new Table(Styles.black6);
            Image icon = new Image(cons.current.uiIcon);
            icon.setScaling(Scaling.fit);
            Stack iconStack = new Stack();
            iconStack.add(portraitBorderElement());
            Table iconWrap = new Table();
            iconWrap.add(icon).size(boxSize - ui(4f));
            iconStack.add(iconWrap);
            box.add(iconStack).size(boxSize);
            box.addListener(new Tooltip(t -> {
                t.background(Styles.black6);
                t.label(() -> {
                    float total = cons.current.buildTime / 60f;
                    float current = total * cons.progress;
                    return formatTimePair(current, total);
                }).style(Styles.outlineLabel).color(Color.white);
            }));

            Table progressCol = new Table();
            progressCol.bottom().left();
            progressCol.table(barTable -> {
                barTable.left();
                barTable.label(() -> {
                    float total = cons.current.buildTime / 60f;
                    float current = total * cons.progress;
                    return formatTimePair(current, total);
                }).style(Styles.outlineLabel).color(Color.lightGray).width(barWidth).right().padBottom(ui(2f)).row();

                Bar bar = new Bar(() -> "", () -> Pal.accent, () -> cons.progress);
                barTable.add(bar).size(barWidth, barHeight).left();
            }).bottom().left();

            panel.add(box);
            panel.add(progressCol).bottom().left();
        });
    }

    private String unitDisplayName(UnitType type){
        if(type == UnitTypes.nova) return "SCV";
        return type.localizedName;
    }

    private int medivacPayloadHash(Unit unit){
        if(unit == null || !unit.isValid() || !UnitTypes.isMedivac(unit) || !(unit instanceof Payloadc payload)){
            return -1;
        }

        int hash = payload.payloads().size;
        for(int i = 0; i < payload.payloads().size; i++){
            Payload p = payload.payloads().get(i);
            if(p instanceof UnitPayload up && up.unit != null && up.unit.type != null){
                hash = 31 * hash + up.unit.id;
                hash = 31 * hash + up.unit.type.id;
            }else{
                hash = 31 * hash + i * 17;
            }
        }
        return hash;
    }

    private String formatTimePair(float current, float total){
        return formatTimeValue(current) + "/" + formatTimeValue(total);
    }

    private String fixed2(float value){
        return Strings.autoFixed(value, 2);
    }

    private void addDisplayedWeapon(Seq<Weapon> shown, @Nullable Weapon weapon){
        if(weapon == null || weapon.bullet == null) return;
        if(!shown.contains(weapon, true)) shown.add(weapon);
    }

    private Seq<Weapon> displayedWeapons(Unit unit){
        Seq<Weapon> shown = new Seq<>(2);
        if(unit == null || unit.type == null || unit.type.weapons == null || unit.type.weapons.isEmpty()) return shown;

        if(UnitTypes.isGhost(unit)){
            addDisplayedWeapon(shown, unit.type.weapons.find(w -> w.bullet != null));
            return shown;
        }

        if(UnitTypes.isSiegeTank(unit)){
            if(UnitTypes.preceptIsSieged(unit)){
                addDisplayedWeapon(shown, unit.type.weapons.find(w -> "precept-siege-weapon".equals(w.name) && w.bullet != null));
            }else{
                addDisplayedWeapon(shown, unit.type.weapons.find(w -> !"precept-siege-weapon".equals(w.name) && w.bullet != null));
            }
            return shown;
        }

        if(UnitTypes.isThor(unit)){
            addDisplayedWeapon(shown, unit.type.weapons.find(w -> "scepter-weapon".equals(w.name) && w.bullet != null));
            if(UnitTypes.scepterDisplayImpactMode(unit)){
                addDisplayedWeapon(shown, unit.type.weapons.find(w -> "disperse-mid".equals(w.name) && w.bullet != null));
            }else{
                addDisplayedWeapon(shown, unit.type.weapons.find(w -> "scepter-mount".equals(w.name) && w.bullet != null));
            }
            if(!shown.isEmpty()) return shown;
        }

        if(UnitTypes.isViking(unit)){
            if(UnitTypes.vikingIsMechMode(unit)){
                addDisplayedWeapon(shown, unit.type.weapons.find(w -> "viking-gatling".equals(w.name) && w.bullet != null));
            }else{
                addDisplayedWeapon(shown, unit.type.weapons.find(w -> w.bullet != null && w.bullet.collidesAir && !w.bullet.collidesGround));
            }
            if(!shown.isEmpty()) return shown;
        }

        Weapon battlecruiserGround = unit.type.weapons.find(w -> "battlecruiser-ground-laser".equals(w.name) && w.bullet != null);
        Weapon battlecruiserAir = unit.type.weapons.find(w -> "battlecruiser-air-laser".equals(w.name) && w.bullet != null);
        if(battlecruiserGround != null || battlecruiserAir != null){
            addDisplayedWeapon(shown, battlecruiserGround);
            addDisplayedWeapon(shown, battlecruiserAir);
            if(!shown.isEmpty()) return shown;
        }

        Weapon ground = unit.type.weapons.find(w -> w.bullet != null && w.bullet.collidesGround && !w.bullet.collidesAir);
        Weapon air = unit.type.weapons.find(w -> w.bullet != null && w.bullet.collidesAir && !w.bullet.collidesGround);
        if(ground == null){
            ground = unit.type.weapons.find(w -> w.bullet != null && w.bullet.collidesGround);
        }
        if(air == null){
            Weapon finalGround = ground;
            air = unit.type.weapons.find(w -> w.bullet != null && w.bullet.collidesAir && w != finalGround);
        }

        addDisplayedWeapon(shown, ground);
        addDisplayedWeapon(shown, air);
        if(shown.isEmpty()){
            addDisplayedWeapon(shown, unit.type.weapons.find(w -> w.bullet != null));
        }
        if(shown.size > 2){
            shown.truncate(2);
        }
        return shown;
    }

    private @Nullable Weapon displayedWeapon(Unit unit){
        Seq<Weapon> shown = displayedWeapons(unit);
        return shown.isEmpty() ? null : shown.first();
    }

    private float displayedWeaponDamage(Unit unit, Weapon weapon){
        if(weapon == null || weapon.bullet == null) return 0f;

        if(unit.type == UnitTypes.mace){
            return weapon.bullet.damage + UnitTypes.vehicleWeaponMaceBaseBonus(unit.team);
        }
        if(unit.type == UnitTypes.locus){
            return weapon.bullet.damage + UnitTypes.vehicleWeaponLocusBaseBonus(unit.team);
        }
        if(UnitTypes.isGhost(unit)){
            return weapon.bullet.damage + UnitTypes.infantryWeaponBaseDamageBonus(unit.team);
        }
        if(unit.type == UnitTypes.fortress){
            return weapon.bullet.damage + UnitTypes.infantryWeaponBaseDamageBonus(unit.team);
        }

        if(UnitTypes.isHurricane(unit)){
            return UnitTypes.hurricaneMissileDamage(unit.team, UnitTypes.hurricaneLockActive(unit));
        }
        if(UnitTypes.isSiegeTank(unit)){
            if("precept-siege-weapon".equals(weapon.name)){
                return 40f + UnitTypes.vehicleWeaponPreceptSiegeBaseBonus(unit.team);
            }
            return 15f + UnitTypes.vehicleWeaponPreceptMobileBaseBonus(unit.team);
        }

        if(UnitTypes.isThor(unit)){
            if("scepter-mount".equals(weapon.name)){
                return weapon.bullet.damage + UnitTypes.vehicleWeaponScepterBurstBaseBonus(unit.team);
            }
            if("disperse-mid".equals(weapon.name)){
                return weapon.bullet.damage + UnitTypes.vehicleWeaponScepterImpactBaseBonus(unit.team);
            }
            if("scepter-weapon".equals(weapon.name)){
                return weapon.bullet.damage + UnitTypes.vehicleWeaponScepterGroundBaseBonus(unit.team);
            }
        }

        if(UnitTypes.isViking(unit)){
            if("viking-gatling".equals(weapon.name)){
                return weapon.bullet.damage + UnitTypes.shipWeaponVikingMechBaseBonus(unit.team);
            }
            return weapon.bullet.damage + UnitTypes.shipWeaponVikingFighterBaseBonus(unit.team);
        }

        return Math.max(weapon.bullet.damage, weapon.bullet.splashDamage);
    }

    private Seq<String> displayedDamageBonusLines(Unit unit, Weapon weapon){
        Seq<String> lines = new Seq<>();
        if(unit == null || weapon == null || weapon.bullet == null) return lines;

        if(unit.type == UnitTypes.mace){
            float special = weapon.bullet.damage + UnitTypes.vehicleWeaponMaceBaseBonus(unit.team) + UnitTypes.infernoPreheaterMaceLightBonus(unit.team);
            lines.add("vs " + armorTypeName(ArmorType.light) + ": " + fixed2(special));
            return lines;
        }

        if(unit.type == UnitTypes.locus && "locus-weapon".equals(weapon.name)){
            float special = 14f + UnitTypes.vehicleWeaponLocusLightBonus(unit.team) + UnitTypes.infernoPreheaterLocusLightBonus(unit.team);
            lines.add("vs " + armorTypeName(ArmorType.light) + ": " + fixed2(special));
            return lines;
        }

        if(UnitTypes.isGhost(unit)){
            float special = weapon.bullet.damage + 10f + UnitTypes.infantryWeaponGhostLightBonus(unit.team);
            lines.add("vs " + armorTypeName(ArmorType.light) + ": " + fixed2(special));
            return lines;
        }

        if(unit.type == UnitTypes.fortress){
            float special = weapon.bullet.damage + 10f + UnitTypes.infantryWeaponFortressHeavyBonus(unit.team);
            lines.add("vs " + armorTypeName(ArmorType.heavy) + ": " + fixed2(special));
            return lines;
        }

        if(UnitTypes.isSiegeTank(unit)){
            float special;
            if("precept-siege-weapon".equals(weapon.name)){
                special = 70f + UnitTypes.vehicleWeaponPreceptSiegeHeavyBonus(unit.team);
            }else{
                special = 25f + UnitTypes.vehicleWeaponPreceptMobileHeavyBonus(unit.team);
            }
            lines.add("vs " + armorTypeName(ArmorType.heavy) + ": " + fixed2(special));
            return lines;
        }

        if(UnitTypes.isThor(unit)){
            if("scepter-mount".equals(weapon.name)){
                float special = weapon.bullet.damage + 6f + UnitTypes.vehicleWeaponScepterBurstLightBonus(unit.team);
                lines.add("vs " + armorTypeName(ArmorType.light) + ": " + fixed2(special));
            }else if("disperse-mid".equals(weapon.name)){
                float special = weapon.bullet.damage + 10f + UnitTypes.vehicleWeaponScepterImpactHeavyBonus(unit.team);
                lines.add("vs " + unitClassName(UnitClass.heavy) + " units: " + fixed2(special));
            }
            return lines;
        }

        if(UnitTypes.isViking(unit)){
            if("viking-gatling".equals(weapon.name)){
                float special = 20f + UnitTypes.shipWeaponVikingMechMechanicalBonus(unit.team);
                lines.add("vs " + unitClassName(UnitClass.mechanical) + " units: " + fixed2(special));
            }else{
                float special = 14f + UnitTypes.shipWeaponVikingFighterHeavyBonus(unit.team);
                lines.add("vs " + armorTypeName(ArmorType.heavy) + ": " + fixed2(special));
            }
            return lines;
        }

        if(unit.type == UnitTypes.locus && "locus-weapon".equals(weapon.name)){
            float special = 14f + UnitTypes.vehicleWeaponLocusLightBonus(unit.team) + UnitTypes.infernoPreheaterLocusLightBonus(unit.team);
            lines.add("vs " + armorTypeName(ArmorType.light) + " (armor): " + fixed2(special));
            return lines;
        }

        if(UnitTypes.isGhost(unit)){
            float special = weapon.bullet.damage + UnitTypes.infantryWeaponGhostLightBonus(unit.team);
            lines.add("vs " + armorTypeName(ArmorType.light) + " (armor): " + fixed2(special));
            return lines;
        }

        if(UnitTypes.isSiegeTank(unit)){
            float special;
            if("precept-siege-weapon".equals(weapon.name)){
                special = 70f + UnitTypes.vehicleWeaponPreceptSiegeHeavyBonus(unit.team);
            }else{
                special = 25f + UnitTypes.vehicleWeaponPreceptMobileHeavyBonus(unit.team);
            }
            lines.add("vs " + armorTypeName(ArmorType.heavy) + " (armor): " + fixed2(special));
            return lines;
        }

        if(UnitTypes.isThor(unit)){
            if("scepter-mount".equals(weapon.name)){
                float special = weapon.bullet.damage + 6f + UnitTypes.vehicleWeaponScepterBurstLightBonus(unit.team);
                lines.add("vs " + armorTypeName(ArmorType.light) + " (armor): " + fixed2(special));
            }else if("disperse-mid".equals(weapon.name)){
                float special = weapon.bullet.damage + 10f + UnitTypes.vehicleWeaponScepterImpactHeavyBonus(unit.team);
                lines.add("vs " + unitClassName(UnitClass.heavy) + " (units): " + fixed2(special));
            }
            return lines;
        }

        if(UnitTypes.isViking(unit)){
            if("viking-gatling".equals(weapon.name)){
                float special = 20f + UnitTypes.shipWeaponVikingMechMechanicalBonus(unit.team);
                lines.add("vs " + unitClassName(UnitClass.mechanical) + " (units): " + fixed2(special));
            }else{
                float special = 14f + UnitTypes.shipWeaponVikingFighterHeavyBonus(unit.team);
                lines.add("vs " + armorTypeName(ArmorType.heavy) + " (armor): " + fixed2(special));
            }
            return lines;
        }

        return lines;
    }

    private float displayedWeaponRange(Unit unit, Weapon weapon){
        if(weapon == null || weapon.bullet == null) return 0f;
        if(UnitTypes.isHurricane(unit)){
            return UnitTypes.hurricaneLockActive(unit) ? UnitTypes.hurricaneLockRange() : UnitTypes.hurricaneBaseRange();
        }
        return weapon.range();
    }

    private int displayedMultiAttack(Weapon weapon){
        if(weapon == null || weapon.shoot == null) return 1;
        return Math.max(1, weapon.shoot.shots);
    }

    private String formatTimeValue(float value){
        float rounded = Math.round(value * 10f) / 10f;
        if(Mathf.equal(rounded, (int)rounded)) return Integer.toString((int)rounded);
        return String.format("%.1f", rounded);
    }

    private float gridPortraitSize(){
        return UnitAbilityPanel.abilityButtonSize;
    }

    private float singlePanelHeight(){
        return ROWS * (gridPortraitSize() + GRID_PORTRAIT_PAD * 2f);
    }

    private Element portraitBorderElement(){
        return new Element(){
            @Override
            public void draw(){
                Draw.color(UnitAbilityPanel.abilityBorderColor);
                Lines.stroke(ui(1.5f));
                float inset = ui(1.5f);
                float innerInset = ui(4.5f);
                Lines.rect(x + inset, y + inset, width - inset * 2f, height - inset * 2f);
                Lines.rect(x + innerInset, y + innerInset, width - innerInset * 2f, height - innerInset * 2f);
                Draw.reset();
            }
        };
    }

    private Element subgroupHighlightElement(@Nullable UnitType type){
        return new Element(){
            @Override
            public void draw(){
                if(type == null) return;
                if(!control.input.isUnitInActiveAbilitySubgroup(type)) return;
                Draw.color(UnitAbilityPanel.abilityBorderColor);
                Lines.stroke(ui(1.5f * 3f));
                float outerInset = -ui(2f);
                float innerInset = outerInset + ui(3f);
                Lines.rect(x + outerInset, y + outerInset, width - outerInset * 2f, height - outerInset * 2f);
                Lines.rect(x + innerInset, y + innerInset, width - innerInset * 2f, height - innerInset * 2f);
                Draw.reset();
            }
        };
    }

    private Element subgroupHighlightElement(@Nullable Building build){
        return new Element(){
            @Override
            public void draw(){
                if(build == null) return;
                if(!control.input.isBuildingInActiveAbilitySubgroup(build)) return;
                Draw.color(UnitAbilityPanel.abilityBorderColor);
                Lines.stroke(ui(1.5f * 3f));
                float outerInset = -ui(2f);
                float innerInset = outerInset + ui(3f);
                Lines.rect(x + outerInset, y + outerInset, width - outerInset * 2f, height - outerInset * 2f);
                Lines.rect(x + innerInset, y + innerInset, width - innerInset * 2f, height - innerInset * 2f);
                Draw.reset();
            }
        };
    }

    private Element factoryAddonBadgeElement(@Nullable Building build){
        return new Element(){
            @Override
            public void draw(){
                String letter = factoryAddonLetter(build);
                if(letter == null) return;

                Fonts.outline.getData().setScale(0.45f * SELECTION_PANEL_SCALE);
                addonBadgeLayout.setText(Fonts.outline, letter);

                float pad = ui(2f);
                float textPadX = ui(2f);
                float textPadY = ui(1f);
                float badgeWidth = addonBadgeLayout.width + textPadX * 2f;
                float badgeHeight = addonBadgeLayout.height + textPadY * 2f;
                float badgeX = x + width - badgeWidth - pad;
                float badgeY = y + pad;

                Draw.color(Color.black, 0.8f);
                Fill.crect(badgeX, badgeY, badgeWidth, badgeHeight);
                Draw.color(Color.white, 0.9f);
                Lines.stroke(ui(1f));
                Lines.rect(badgeX, badgeY, badgeWidth, badgeHeight);
                Draw.reset();

                Fonts.outline.setColor(Color.white);
                Fonts.outline.draw(letter, badgeX + badgeWidth - textPadX, badgeY + textPadY + addonBadgeLayout.height, Align.right);
                Fonts.outline.setColor(Color.white);
                Fonts.outline.getData().setScale(1f);
            }
        };
    }

    private Element factoryQueueSlotsElement(UnitFactory.UnitFactoryBuild factory){
        final Color empty = Color.valueOf("6a6a6a");
        final Color border = Color.black;
        final Color filled = Color.white;
        return new Element(){
            @Override
            public void draw(){
                if(factory == null || !factory.isValid() || !factory.sc2QueueEnabled()) return;
                int slots = Math.min(8, factory.queueSlots());
                if(slots <= 0) return;
                int queued = Mathf.clamp(factory.queued, 0, slots);
                float badgeReserve = factoryAddonLetter(factory) == null ? 0f : ui(12f);
                drawBottomQueueSlots(slots, queued, badgeReserve, empty, border, filled);
            }
        };
    }

    private Element coreQueueSlotsElement(CoreBuild core){
        final Color empty = Color.valueOf("6a6a6a");
        final Color border = Color.black;
        final Color filled = Color.white;
        return new Element(){
            @Override
            public void draw(){
                if(core == null || !core.isValid()) return;
                int slots = Math.min(8, core.queueSlots());
                if(slots <= 0) return;
                int queued = core.unitQueue == null ? 0 : Mathf.clamp(core.unitQueue.size, 0, slots);
                drawBottomQueueSlots(slots, queued, 0f, empty, border, filled);
            }
        };
    }

    private void drawBottomQueueSlots(int slots, int queued, float rightReserve, Color empty, Color border, Color filled){
        float leftMargin = ui(2f);
        float rightMargin = ui(2f) + rightReserve;
        float avail = Math.max(0f, width - leftMargin - rightMargin);
        if(avail <= 0f) return;

        float box = avail / slots;
        box = Mathf.clamp(box, ui(3.5f), ui(6.5f));

        float totalWidth = box * slots;
        if(totalWidth > avail){
            box = avail / slots;
            totalWidth = box * slots;
        }

        float startX = x + leftMargin + Math.max(0f, (avail - totalWidth) / 2f);
        float baseY = y + ui(1.5f);

        Lines.stroke(ui(1f));
        for(int i = 0; i < slots; i++){
            float bx = startX + i * box;
            Draw.color(i < queued ? filled : empty);
            Fill.crect(bx, baseY, box, box);
            Draw.color(border);
            Lines.rect(bx, baseY, box, box);
        }
        Draw.reset();
    }

    private int coreQueueHash(CoreBuild core){
        if(core == null || core.unitQueue == null) return 0;
        int hash = core.unitQueue.size;
        for(int i = 0; i < core.unitQueue.size; i++){
            hash = 31 * hash + core.unitQueue.get(i);
        }
        return hash;
    }

    private int factoryQueueHash(UnitFactory.UnitFactoryBuild factory){
        if(factory == null || !factory.sc2QueueEnabled()) return 0;
        int hash = factory.queued;
        hash = 31 * hash + factory.currentPlan;
        hash = 31 * hash + factory.activeUnitSlots();
        hash = 31 * hash + factory.queueSlots();
        hash = 31 * hash + (factory.isAddonBuilding() ? 1 : 0);
        Block addon = factory.addonBuildingBlock();
        hash = 31 * hash + (addon == null ? 0 : addon.id);
        return hash;
    }

    private int armoryQueueHash(@Nullable Team team){
        if(team == null) return 0;
        return ResearchQueueService.armoryQueueHash(team, 1);
    }

    private int fusionCoreQueueHash(@Nullable Team team){
        if(team == null) return 0;
        return ResearchQueueService.fusionCoreQueueHash(team, 1);
    }

    private int engineeringQueueHash(@Nullable Team team){
        if(team == null) return 0;
        return ResearchQueueService.engineeringQueueHash(team, 1);
    }

    private int techLabQueueHash(@Nullable Building build){
        if(build == null || build.block != Blocks.memoryBank) return 0;
        UnitFactory.UnitFactoryBuild attached = attachedFactoryForTechLab(build);
        if(attached == null) return 0;
        return ResearchQueueService.techLabQueueHash(build.team, attached.block, build.pos());
    }

    private int ghostAcademyQueueHash(@Nullable Building build){
        if(build == null || build.block != Blocks.launchPad) return 0;
        int hash = build.pos();
        hash = 31 * hash + (UnitTypes.ghostCamoAnyResearching(build.team) ? 1 : 0);
        hash = 31 * hash + (UnitTypes.ghostWarheadProducing(build) ? 1 : 0);
        hash = 31 * hash + (UnitTypes.ghostWarheadArmed(build) ? 1 : 0);
        return hash;
    }

    private void buildCrystalInfoPanel(Tile tile){
        if(tile == null || !(tile.block() instanceof CrystalMineralWall)) return;

        CrystalMineralWall crystal = (CrystalMineralWall)tile.block();
        String collectingLabel = Core.bundle.get("resource.collecting", "Collecting units");
        String remainingLabel = Core.bundle.get("crystalmineral.remaining", "Remaining");
        String infiniteLabel = Core.bundle.get("ui.infinite", "Infinite");

        gridTable.clear();
        gridTable.table(panel -> {
            panel.background(Styles.black6);
            panel.margin(ui(8f));
            panel.defaults().center();

            panel.label(() -> collectingLabel + ": " + HarvestAI.getActiveNovaCount(tile))
                .style(Styles.outlineLabel).color(Color.white).row();

            panel.image(tile.block().uiIcon).size(ui(80f)).padTop(ui(6f)).row();

            panel.label(() -> {
                if(crystal.isInfinite(tile)){
                    return remainingLabel + ": " + infiniteLabel;
                }
                return remainingLabel + ": " + crystal.getReserves(tile);
            }).style(Styles.outlineLabel).color(Color.lightGray).padTop(ui(6f));
        }).growX().height(singlePanelHeight());
    }

    private void buildSteamVentInfoPanel(Tile tile){
        if(tile == null || !(tile.floor() instanceof SteamVent vent)) return;

        Tile data = vent.dataTile(tile);
        String collectingLabel = Core.bundle.get("resource.collecting", "Collecting units");
        String remainingLabel = Core.bundle.get("steamvent.remaining", "Remaining");
        String infiniteLabel = Core.bundle.get("ui.infinite", "Infinite");

        gridTable.clear();
        gridTable.table(panel -> {
            panel.background(Styles.black6);
            panel.margin(ui(8f));
            panel.defaults().center();

            panel.label(() -> collectingLabel + ": " + HarvestAI.getActiveNovaCount(data))
                .style(Styles.outlineLabel).color(Color.white).row();

            panel.image(tile.floor().uiIcon).size(ui(80f)).padTop(ui(6f)).row();

            panel.label(() -> {
                if(vent.isInfinite(data)){
                    return remainingLabel + ": " + infiniteLabel;
                }
                return remainingLabel + ": " + vent.getReserves(data);
            }).style(Styles.outlineLabel).color(Color.lightGray).padTop(ui(6f));
        }).growX().height(singlePanelHeight());
    }

    private int getControlGroupNumber(Displayable item){
        int id = -1;
        if(item instanceof UnitDisplay){
            id = ((UnitDisplay)item).unit.id;
        }else if(item instanceof BuildingDisplay){
            id = ((BuildingDisplay)item).building.id;
        }

        if(id < 0) return -1;

        //Check all control groups
        for(int i = 0; i < control.input.controlGroups.length; i++){
            if(control.input.controlGroups[i] != null && control.input.controlGroups[i].contains(id)){
                return i;
            }
        }
        return -1;
    }

    private String armorTypeName(ArmorType type){
        String key = "armor." + type.name();
        return Core.bundle.get(key, type.displayName);
    }

    private String unitClassName(UnitClass type){
        String key = "unitclass." + type.name();
        return Core.bundle.get(key, type.displayName);
    }

    private ArmorType displayedArmorType(UnitType type){
        return type.armorType;
    }

    private String displayedUnitClassNames(UnitType type){
        if(type == UnitTypes.mega){
            return unitClassName(UnitClass.mechanical);
        }

        StringBuilder classes = new StringBuilder();
        for(UnitClass unitClass : UnitClass.values()){
            if(type.unitClasses.contains(unitClass)){
                if(classes.length() > 0) classes.append(" ");
                classes.append(unitClassName(unitClass));
            }
        }

        if(classes.length() == 0){
            return unitClassName(UnitClass.mechanical);
        }
        return classes.toString();
    }

    private ArmorType armorTypeFor(Block block){
        if(block == null) return ArmorType.none;
        return ArmorType.heavy;
    }
}


