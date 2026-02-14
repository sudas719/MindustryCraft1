package mindustry.ui;

import arc.*;
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
import mindustry.world.blocks.storage.*;
import mindustry.world.blocks.storage.CoreBlock.*;
import mindustry.world.blocks.units.*;

import static mindustry.Vars.*;

public class UnitSelectionGrid extends Table{
    private static final int COLS = 8;
    private static final int ROWS = 3;
    private static final int UNITS_PER_PAGE = COLS * ROWS;
    private static final float GRID_PORTRAIT_PAD = 2f;

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
                }else{
                    lastCoreId = -1;
                    lastCoreBuildPage = false;
                    lastCoreQueueHash = -1;
                    lastFactoryId = -1;
                    lastFactoryQueueHash = -1;
                }
            }else{
                lastCoreId = -1;
                lastCoreBuildPage = false;
                lastCoreQueueHash = -1;
                lastFactoryId = -1;
                lastFactoryQueueHash = -1;
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
            int typeCmp = Integer.compare(ua.type.id, ub.type.id);
            if(typeCmp != 0) return typeCmp;
            return Integer.compare(ua.id, ub.id);
        }
        Building ba = ((BuildingDisplay)a).building;
        Building bb = ((BuildingDisplay)b).building;
        int blockCmp = Integer.compare(ba.block.id, bb.block.id);
        if(blockCmp != 0) return blockCmp;
        return Integer.compare(ba.id, bb.id);
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

            //Create portrait button
            Table portrait = new Table();
            Stack stack = new Stack();
            stack.add(portraitBorderElement());
            Image icon = new Image(item.icon());
            icon.setScaling(Scaling.fit);
            Table iconTable = new Table();
            iconTable.add(icon).size(UnitAbilityPanel.abilityIconSize);
            stack.add(iconTable);
            portrait.add(stack).size(gridPortraitSize());

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
            panel.background(Styles.black6);
            panel.margin(8f);

            //Left half: Unit icon and HP
            panel.table(leftHalf -> {
                leftHalf.image(unit.type.uiIcon).size(80f).row();
                //HP display updates in real-time
                leftHalf.label(() -> Strings.autoFixed(unit.health, 2) + "/" + Strings.autoFixed(unit.maxHealth, 2))
                    .color(Color.white).style(Styles.outlineLabel).padTop(4f);
                if(unit.type.energyCapacity > 0f){
                    leftHalf.row();
                    leftHalf.label(() -> Mathf.round(unit.energy) + "/" + Mathf.round(unit.type.energyCapacity))
                        .color(Color.valueOf("b57aff")).style(Styles.outlineLabel).padTop(2f);
                }
                float remaining = PulsarDrops.remainingFraction(unit);
                if(remaining > 0f){
                    leftHalf.row();
                    Bar lifeBar = new Bar(() -> "", () -> Color.gray, () -> PulsarDrops.remainingFraction(unit));
                    leftHalf.add(lifeBar).width(80f).height(4f).padTop(4f);
                }
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
                    String armorLabel = Core.bundle.get("ui.armor", "Armor");
                    String weaponLabel = Core.bundle.get("ui.weapon", "Weapon");
                    String typeLabel = Core.bundle.get("ui.type", "Type");
                    String valueLabel = Core.bundle.get("ui.value", "Value");
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
                        armorIcon.image(Icon.defense).color(Color.orange);
                        armorIcon.addListener(new Tooltip(t -> {
                            t.background(Styles.black6);
                            t.add(armorLabel).color(Color.yellow).row();
                            t.image().color(Pal.accent).height(3f).growX().pad(4f).row();
                            t.add(typeLabel + ": " + armorTypeName(unit.type.armorType)).left().row();
                            t.add(valueLabel + ": " + (int)unit.type.armor).left().row();
                            t.add(speedLabel + ": " + String.format("%.1f", unit.type.speed) + " tiles/s").left().row();
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
                                t.add(weaponLabel).color(Color.yellow).row();
                                t.image().color(Pal.accent).height(3f).growX().pad(4f).row();

                                //Find first weapon for stats
                                for(var weapon : unit.type.weapons){
                                    if(weapon.bullet != null){
                                        float damage = weapon.bullet.damage;
                                        if(weapon.bullet.splashDamage > damage) damage = weapon.bullet.splashDamage;

                                        t.add(damageLabel + ": " + (int)damage).left().row();
                                        t.add(rangeLabel + ": " + (int)(weapon.range() / 8f) + " tiles").left().row();

                                        if(weapon.reload > 0){
                                            t.add(speedLabel + ": " + String.format("%.1f", weapon.reload / 60f) + "s").left().row();
                                        }

                                        if(weapon.bullet.fragBullets > 1){
                                            t.add(multiAttackLabel + ": " + weapon.bullet.fragBullets + "x").left().row();
                                        }

                                        String target = canAttackAir && canAttackGround ? targetGroundAir :
                                                       canAttackAir ? targetAir : targetGround;
                                        t.add(targetLabel + ": " + target).left().row();
                                        break;
                                    }
                                }
                            }));
                        });
                    }
                }).padTop(8f).row();

                //Bottom row: armor type and unit class
                rightHalf.table(bottomRow -> {
                    String armorName = armorTypeName(unit.type.armorType);
                    String className = unitClassName(unit.type.unitClass);
                    if(unit.type.energyCapacity > 0f && unit.type.unitClass != UnitClass.psionic){
                        className += " " + unitClassName(UnitClass.psionic);
                    }
                    bottomRow.add(armorName + " " + className).color(Color.white);
                }).padTop(8f);
            }).grow().center();
        }).growX().height(singlePanelHeight());
    }

    private void buildBuildingInfoPanel(Building building){
        gridTable.clear();
        Block display = displayBlock(building);
        float maxHealth = displayMaxHealth(building);
        CoreBuild core = building instanceof CoreBuild ? (CoreBuild)building : null;
        ConstructBlock.ConstructBuild cons = building instanceof ConstructBlock.ConstructBuild ? (ConstructBlock.ConstructBuild)building : null;
        boolean incomplete = cons != null && cons.current != null && cons.current != Blocks.air && cons.progress < 1f;
        boolean showCoreQueue = core != null && ui.hudfrag.abilityPanel != null && ui.hudfrag.abilityPanel.isCoreBuildPage();
        UnitFactory.UnitFactoryBuild factory = building instanceof UnitFactory.UnitFactoryBuild ? (UnitFactory.UnitFactoryBuild)building : null;
        boolean showFactoryQueue = factory != null && factory.sc2QueueEnabled();

        gridTable.table(panel -> {
            panel.background(Styles.black6);
            panel.margin(8f);

            //Left half: Building icon
            panel.table(leftHalf -> {
                leftHalf.defaults().center();
                leftHalf.image(display.uiIcon).size(80f).row();
                leftHalf.label(() -> {
                    if(incomplete){
                        return Mathf.round(building.health) + "/" + Mathf.round(maxHealth);
                    }
                    return Strings.autoFixed(building.health, 2) + "/" + Strings.autoFixed(maxHealth, 2);
                })
                    .style(Styles.outlineLabel).color(Color.lightGray).padTop(6f).row();
                if(core != null && core.block == Blocks.coreOrbital){
                    leftHalf.label(() -> Mathf.round(Math.max(core.orbitalEnergy, 0f)) + "/" + Mathf.round(CoreBlock.orbitalEnergyCap))
                        .style(Styles.outlineLabel).color(Color.valueOf("b57aff")).padTop(4f).row();
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
                        }).style(Styles.outlineLabel).color(Color.lightGray).padTop(6f);
                    }
                }
            }).width(120f).padRight(16f);

            //Right half: Building info (centered)
            panel.table(rightHalf -> {
                rightHalf.defaults().center();

                if(showCoreQueue){
                    buildCoreQueuePanel(rightHalf, core);
                }else if(showFactoryQueue){
                    buildFactoryQueuePanel(rightHalf, factory);
                }else if(incomplete){
                    buildConstructProgressPanel(rightHalf, cons);
                }else{
                    //Building name
                    rightHalf.add(display.localizedName).color(Color.white).style(Styles.outlineLabel).row();

                    //Icons row: armor and weapons
                    rightHalf.table(iconsRow -> {
                        iconsRow.defaults().size(28f).pad(4f);
                        String armorLabel = Core.bundle.get("ui.armor", "Armor");
                        String weaponLabel = Core.bundle.get("ui.weapon", "Weapon");
                        String valueLabel = Core.bundle.get("ui.value", "Value");
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
                            armorIcon.image(Icon.defense).color(Color.orange);
                            armorIcon.addListener(new Tooltip(t -> {
                                t.background(Styles.black6);
                                t.add(armorLabel).color(Color.yellow).row();
                                t.image().color(Pal.accent).height(3f).growX().pad(4f).row();
                                t.add(valueLabel + ": " + (int)display.armor).left().row();
                                t.add(healthLabel + ": " + (int)maxHealth).left().row();
                            }));
                        });

                        //Weapon icon (only if this block can attack)
                        if(building.block.attacks){
                            iconsRow.table(weaponIcon -> {
                                weaponIcon.image(Icon.units);
                                weaponIcon.addListener(new Tooltip(t -> {
                                    t.background(Styles.black6);
                                    t.add(weaponLabel).color(Color.yellow).row();
                                    t.image().color(Pal.accent).height(3f).growX().pad(4f).row();

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
                                        t.add(damageLabel + ": " + (int)(float)damage).left().row();
                                    }
                                    if(range >= 0f){
                                        t.add(rangeLabel + ": " + (int)(range / 8f) + " tiles").left().row();
                                    }
                                    if(reload > 0f){
                                        t.add(speedLabel + ": " + String.format("%.1f", reload / 60f) + "s").left().row();
                                    }
                                    if(target != null){
                                        t.add(targetLabel + ": " + target).left().row();
                                    }
                                }));
                            });
                        }
                    }).padTop(8f).row();

                    //Bottom row: armor type and building category
                    String armorName = armorTypeName(armorTypeFor(display));
                    String className = unitClassName(UnitClass.mechanical);
                    String buildingLabel = Core.bundle.get("ui.building", "Building");
                    rightHalf.add(armorName + " " + className + " " + buildingLabel)
                        .color(Color.white).padTop(8f);
                }
            }).grow().center();
        }).growX().height(singlePanelHeight());
    }

    private void buildCoreQueuePanel(Table rightHalf, CoreBuild core){
        float slotSize = 34f;
        float barWidth = 180f;
        float barHeight = 3f;

        rightHalf.table(queueRoot -> {
            queueRoot.defaults().center();

            int activeSlots = Math.max(1, core.activeUnitSlots());
            int totalSlots = Math.max(activeSlots, core.queueSlots());

            Table row1 = new Table();
            row1.defaults().size(slotSize).pad(2f);
            row1.add(buildQueueSlot(core.queuedUnit(0), slotSize));
            row1.add(buildQueueProgress(core, 0, barWidth, barHeight)).size(barWidth, barHeight).bottom().left().pad(0f).padBottom(2f);

            if(activeSlots > 1){
                row1.add(buildQueueSlot(core.queuedUnit(1), slotSize));
                row1.add(buildQueueProgress(core, 1, barWidth, barHeight)).size(barWidth, barHeight).bottom().left().pad(0f).padBottom(2f);
            }

            Table row2 = new Table();
            row2.defaults().size(slotSize).pad(2f);
            for(int i = activeSlots; i < totalSlots; i++){
                row2.add(buildQueueSlot(core.queuedUnit(i), slotSize));
            }

            queueRoot.add(row1).row();
            if(totalSlots > activeSlots){
                queueRoot.add(row2);
            }
        });
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
        float slotSize = 34f;
        float barWidth = 180f;
        float barHeight = 3f;

        rightHalf.table(queueRoot -> {
            queueRoot.defaults().center();

            if(factory.isAddonBuilding()){
                Block addon = factory.addonBuildingBlock();
                if(addon != null){
                    Table row1 = new Table();
                    row1.defaults().size(slotSize).pad(2f);
                    row1.add(buildQueueSlot(addon, slotSize));
                    row1.add(buildAddonProgress(factory, barWidth, barHeight)).size(barWidth, barHeight).bottom().left().pad(0f).padBottom(2f);
                    queueRoot.add(row1);
                }
                return;
            }

            int activeSlots = Math.max(1, factory.activeUnitSlots());
            int totalSlots = Math.max(activeSlots, factory.queueSlots());

            Table row1 = new Table();
            row1.defaults().size(slotSize).pad(2f);
            row1.add(buildQueueSlot(factory.queuedUnit(0), slotSize));
            row1.add(buildQueueProgress(factory, 0, barWidth, barHeight)).size(barWidth, barHeight).bottom().left().pad(0f).padBottom(2f);

            if(activeSlots > 1){
                row1.add(buildQueueSlot(factory.queuedUnit(1), slotSize));
                row1.add(buildQueueProgress(factory, 1, barWidth, barHeight)).size(barWidth, barHeight).bottom().left().pad(0f).padBottom(2f);
            }

            Table row2 = new Table();
            row2.defaults().size(slotSize).pad(2f);
            for(int i = activeSlots; i < totalSlots; i++){
                row2.add(buildQueueSlot(factory.queuedUnit(i), slotSize));
            }

            queueRoot.add(row1).row();
            if(totalSlots > activeSlots){
                queueRoot.add(row2);
            }
        });
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

    private Table buildQueueSlot(UnitType type, float size){
        Table slot = new Table(Styles.black6);
        if(type != null){
            Image icon = new Image(type.uiIcon);
            slot.add(icon).size(size - 6f);
            slot.addListener(new Tooltip(t -> {
                t.background(Styles.black6);
                t.add(unitDisplayName(type)).style(Styles.outlineLabel).color(Color.white);
            }));
        }
        slot.setSize(size, size);
        return slot;
    }

    private Table buildQueueSlot(Block block, float size){
        Table slot = new Table(Styles.black6);
        if(block != null){
            Image icon = new Image(block.uiIcon);
            slot.add(icon).size(size - 6f);
            slot.addListener(new Tooltip(t -> {
                t.background(Styles.black6);
                t.add(block.localizedName).style(Styles.outlineLabel).color(Color.white);
            }));
        }
        slot.setSize(size, size);
        return slot;
    }

    private void buildConstructProgressPanel(Table rightHalf, ConstructBlock.ConstructBuild cons){
        if(cons == null || cons.current == null || cons.current == Blocks.air) return;
        float boxSize = 54f;
        float barWidth = (boxSize - 12f) * 2f;
        float barHeight = 2.5f;

        rightHalf.table(panel -> {
            Table box = new Table(Styles.black6);
            Image icon = new Image(cons.current.uiIcon);

            box.add(icon).size(boxSize);
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
                }).style(Styles.outlineLabel).color(Color.lightGray).width(barWidth).right().padBottom(2f).row();

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

    private String formatTimePair(float current, float total){
        return formatTimeValue(current) + "/" + formatTimeValue(total);
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
                Lines.stroke(1.5f);
                float inset = 1.5f;
                float innerInset = 4.5f;
                Lines.rect(x + inset, y + inset, width - inset * 2f, height - inset * 2f);
                Lines.rect(x + innerInset, y + innerInset, width - innerInset * 2f, height - innerInset * 2f);
                Draw.reset();
            }
        };
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

    private void buildCrystalInfoPanel(Tile tile){
        if(tile == null || !(tile.block() instanceof CrystalMineralWall)) return;

        CrystalMineralWall crystal = (CrystalMineralWall)tile.block();
        String collectingLabel = Core.bundle.get("resource.collecting", "Collecting units");
        String remainingLabel = Core.bundle.get("crystalmineral.remaining", "Remaining");
        String infiniteLabel = Core.bundle.get("ui.infinite", "Infinite");

        gridTable.clear();
        gridTable.table(panel -> {
            panel.background(Styles.black6);
            panel.margin(8f);
            panel.defaults().center();

            panel.label(() -> collectingLabel + ": " + HarvestAI.getActiveNovaCount(tile))
                .style(Styles.outlineLabel).color(Color.white).row();

            panel.image(tile.block().uiIcon).size(80f).padTop(6f).row();

            panel.label(() -> {
                if(crystal.isInfinite(tile)){
                    return remainingLabel + ": " + infiniteLabel;
                }
                return remainingLabel + ": " + crystal.getReserves(tile);
            }).style(Styles.outlineLabel).color(Color.lightGray).padTop(6f);
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
            panel.margin(8f);
            panel.defaults().center();

            panel.label(() -> collectingLabel + ": " + HarvestAI.getActiveNovaCount(data))
                .style(Styles.outlineLabel).color(Color.white).row();

            panel.image(tile.floor().uiIcon).size(80f).padTop(6f).row();

            panel.label(() -> {
                if(vent.isInfinite(data)){
                    return remainingLabel + ": " + infiniteLabel;
                }
                return remainingLabel + ": " + vent.getReserves(data);
            }).style(Styles.outlineLabel).color(Color.lightGray).padTop(6f);
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

    private ArmorType armorTypeFor(Block block){
        if(block == null) return ArmorType.none;
        return ArmorType.heavy;
    }
}

