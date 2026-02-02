package mindustry.ui;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import mindustry.ai.types.HarvestAI;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.environment.CrystalMineralWall;

import static mindustry.Vars.*;

public class UnitSelectionGrid extends Table{
    private static final int COLS = 8;
    private static final int ROWS = 3;
    private static final int UNITS_PER_PAGE = COLS * ROWS;

    private int currentPage = 0;
    private Seq<Displayable> displayedItems = new Seq<>();
    private Table gridTable;
    private Table paginationTable;
    private Tile displayedResource;

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
            return building.block.uiIcon;
        }

        @Override
        public float health(){
            return building.health;
        }

        @Override
        public float maxHealth(){
            return building.maxHealth;
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
            return building.block.localizedName;
        }
    }

    public UnitSelectionGrid(){
        background(Styles.black6);
        margin(4f);

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
        contentRow.add(paginationTable).left().padRight(4f);

        //Right side: unit grid
        gridTable = new Table();
        contentRow.add(gridTable).grow();

        update(() -> {
            //Update formation numbers display - show all 10 buttons (1-0)
            formationTable.clear();
            formationTable.defaults().size(32f).pad(2f);

            //Draw all 10 formation buttons (1-0)
            for(int i = 0; i < 10; i++){
                final int formationNum = i;
                IntSeq group = control.input.controlGroups[i];
                boolean hasUnits = group != null && !group.isEmpty();

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
                                control.input.selectedUnits.clear();
                                control.input.commandBuildings.clear();

                                control.input.controlGroups[formationNum].each(id -> {
                                    Unit unit = Groups.unit.getByID(id);
                                    Building building = null;

                                    if(unit == null){
                                        for(Building b : Groups.build){
                                            if(b.id == id){
                                                building = b;
                                                break;
                                            }
                                        }
                                    }

                                    if(unit != null){
                                        control.input.selectedUnits.add(unit);
                                    }else if(building != null){
                                        control.input.commandBuildings.add(building);
                                    }
                                });
                                control.input.unassignBuildingsFromControl(control.input.commandBuildings);
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

            Tile currentResource = control.input.selectedResource;
            boolean showResource = current.isEmpty() && currentResource != null && currentResource.block() instanceof CrystalMineralWall;

            if(showResource){
                if(displayedResource != currentResource || !displayedItems.isEmpty()){
                    displayedItems.clear();
                    displayedResource = currentResource;
                    rebuild();
                }
            }else{
                displayedResource = null;
                if(current.size != displayedItems.size || !itemsEqual(current, displayedItems)){
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

    private void rebuild(){
        gridTable.clear();
        paginationTable.clear();

        if(displayedResource != null){
            buildCrystalInfoPanel(displayedResource);
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

            //Create portrait button
            Table portrait = new Table(Styles.black6);

            portrait.stack(
                new Image(item.icon()),
                new Table(t -> {
                    t.top();
                    t.add(item.name()).style(Styles.outlineLabel).pad(2f);
                })
            ).size(48f);
            portrait.row();

            //Health bar
            portrait.add(new Bar(
                () -> "",
                () -> Pal.health,
                () -> item.health() / item.maxHealth()
            )).height(4f).growX();

            portrait.clicked(() -> {
                //Shift+click removes from selection
                if(Core.input.keyDown(arc.input.KeyCode.shiftLeft) || Core.input.keyDown(arc.input.KeyCode.shiftRight)){
                    if(item instanceof UnitDisplay){
                        control.input.selectedUnits.remove(((UnitDisplay)item).unit);
                    }else if(item instanceof BuildingDisplay){
                        control.input.commandBuildings.remove(((BuildingDisplay)item).building);
                        control.input.unassignBuildingsFromControl(control.input.commandBuildings);
                    }
                }else{
                    //Normal click: select only this unit/building, deselect all others
                    control.input.selectedUnits.clear();
                    control.input.commandBuildings.clear();

                    if(item instanceof UnitDisplay){
                        control.input.selectedUnits.add(((UnitDisplay)item).unit);
                    }else if(item instanceof BuildingDisplay){
                        control.input.commandBuildings.add(((BuildingDisplay)item).building);
                        control.input.unassignBuildingsFromControl(control.input.commandBuildings);
                    }
                }
            });

            gridTable.add(portrait).size(56f).pad(2f);

            if(col == COLS - 1){
                gridTable.row();
            }
        }

        //Fill remaining cells with empty space to maintain grid size
        int totalCells = endIdx - startIdx;
        int remainingCells = (ROWS * COLS) - totalCells;
        for(int i = 0; i < remainingCells; i++){
            gridTable.add().size(56f).pad(2f);
            if((totalCells + i + 1) % COLS == 0){
                gridTable.row();
            }
        }

        //Add pagination with direct page number selection (vertical layout on left)
        if(totalPages > 1){
            paginationTable.defaults().size(32f).pad(2f);
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
            if(item instanceof UnitDisplay){
                UnitDisplay unitDisplay = (UnitDisplay)item;
                buildSingleUnitPanel(unitDisplay.unit);
            }else if(item instanceof BuildingDisplay){
                BuildingDisplay buildingDisplay = (BuildingDisplay)item;
                buildBuildingInfoPanel(buildingDisplay.building);
            }
        }
    }

    private void buildSingleUnitPanel(Unit unit){
        //Clear the grid and show single unit info instead
        gridTable.clear();

        gridTable.table(panel -> {
            panel.background(Styles.black8);
            panel.margin(8f);

            //Left half: Unit icon and HP
            panel.table(leftHalf -> {
                leftHalf.image(unit.type.uiIcon).size(80f).row();
                //HP display updates in real-time
                leftHalf.label(() -> String.format("%.0f/%.0f", unit.health, unit.maxHealth))
                    .color(Color.white).style(Styles.outlineLabel).padTop(4f);
            }).width(120f).padRight(16f);

            //Right half: Unit info (centered)
            panel.table(rightHalf -> {
                rightHalf.defaults().center();

                //Unit name
                rightHalf.add(unit.type.localizedName).color(Color.white).style(Styles.outlineLabel).row();

                //Eliminated count
                rightHalf.add("Eliminated: " + (int)unit.stack.amount).color(Color.lightGray).padTop(4f).row();

                //Icons row: armor and weapons
                rightHalf.table(iconsRow -> {
                    iconsRow.defaults().size(28f).pad(4f);

                    //Armor icon
                    iconsRow.table(armorIcon -> {
                        armorIcon.image(Icon.defense).color(Color.orange);
                        armorIcon.addListener(new Tooltip(t -> {
                            t.background(Styles.black6);
                            t.add("Armor").color(Color.yellow).row();
                            t.image().color(Pal.accent).height(3f).growX().pad(4f).row();
                            t.add("Type: " + unit.type.armorType.displayName).left().row();
                            t.add("Value: " + (int)unit.type.armor).left().row();
                    t.add("Speed: " + String.format("%.1f", unit.type.speed) + " tiles/s").left().row();
                        }));
                    });

                    //Weapon icon (single icon if unit has weapons)
                    if(unit.type.weapons.size > 0){
                        //Determine if unit can attack both air and ground
                        boolean hasAntiAir = false;
                        boolean hasAntiGround = false;

                        for(var weapon : unit.type.weapons){
                            if(weapon.bullet != null){
                                if(weapon.bullet.collidesAir) hasAntiAir = true;
                                if(weapon.bullet.collidesGround) hasAntiGround = true;
                            }
                        }

                        final boolean canAttackAir = hasAntiAir;
                        final boolean canAttackGround = hasAntiGround;

                        iconsRow.table(weaponIcon -> {
                            //Use appropriate icon
                            weaponIcon.image(canAttackAir && canAttackGround ? Icon.units : Icon.defense);
                            weaponIcon.addListener(new Tooltip(t -> {
                                t.background(Styles.black6);
                                t.add("Weapon").color(Color.yellow).row();
                                t.image().color(Pal.accent).height(3f).growX().pad(4f).row();

                                //Find first weapon for stats
                                for(var weapon : unit.type.weapons){
                                    if(weapon.bullet != null){
                                        float damage = weapon.bullet.damage;
                                        if(weapon.bullet.splashDamage > damage) damage = weapon.bullet.splashDamage;

                                        t.add("Damage: " + (int)damage).left().row();
                                        t.add("Range: " + (int)(weapon.range() / 8f) + " tiles").left().row();

                                        if(weapon.reload > 0){
                                            t.add("Speed: " + String.format("%.1f", weapon.reload / 60f) + "s").left().row();
                                        }

                                        if(weapon.bullet.fragBullets > 1){
                                            t.add("Multi-attack: " + weapon.bullet.fragBullets + "x").left().row();
                                        }

                                        String target = canAttackAir && canAttackGround ? "Ground & Air" :
                                                       canAttackAir ? "Air" : "Ground";
                                        t.add("Target: " + target).left().row();
                                        break;
                                    }
                                }
                            }));
                        });
                    }
                }).padTop(8f).row();

                //Bottom row: armor type and unit class
                rightHalf.table(bottomRow -> {
                    bottomRow.add(unit.type.armorType.displayName).color(Color.white).padRight(12f);
                    bottomRow.add(unit.type.unitClass.displayName).color(Color.white);
                }).padTop(8f);
            }).grow().center();
        }).growX().height(120f);
    }

    private void buildBuildingInfoPanel(Building building){
        gridTable.row();
        gridTable.table(infoTable -> {
            infoTable.background(Styles.black8);
            infoTable.margin(4f);
            infoTable.add("Building: " + building.block.localizedName).color(Color.white).pad(4f);
        }).growX();
    }

    private void buildCrystalInfoPanel(Tile tile){
        if(tile == null || !(tile.block() instanceof CrystalMineralWall)) return;

        CrystalMineralWall crystal = (CrystalMineralWall)tile.block();

        gridTable.clear();
        gridTable.table(panel -> {
            panel.background(Styles.black8);
            panel.margin(8f);
            panel.defaults().center();

            panel.label(() -> "采集单位: " + HarvestAI.getActiveNovaCount(tile))
                .style(Styles.outlineLabel).color(Color.white).row();

            panel.image(tile.block().uiIcon).size(80f).padTop(6f).row();

            panel.label(() -> {
                if(crystal.isInfinite(tile)){
                    return "剩余: ∞";
                }
                return "剩余: " + crystal.getReserves(tile);
            }).style(Styles.outlineLabel).color(Color.lightGray).padTop(6f);
        }).growX().height(120f);
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
}
