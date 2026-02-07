package mindustry.world.blocks.units;

import arc.*;
import arc.audio.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.*;
import mindustry.ai.*;
import mindustry.content.*;
import mindustry.ctype.*;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.io.*;
import mindustry.logic.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.blocks.*;
import mindustry.world.blocks.payloads.*;
import mindustry.world.blocks.storage.CoreBlock.*;
import mindustry.world.consumers.*;
import mindustry.world.meta.*;
import mindustry.world.*;

import static mindustry.Vars.*;

public class UnitFactory extends UnitBlock{
    public int[] capacities = {};
    public boolean sc2Queue = false;
    public boolean sc2AddonSupport = false;
    public int sc2QueueSlots = 6;
    public int sc2QueueSlotsAddon = 8;
    public static final int sc2AddonTechConfig = -2;
    public static final int sc2AddonDoubleConfig = -3;
    public static final int sc2AddonCancelConfig = -1;
    public static final int sc2AddonCrystalCost = 50;
    public static final int sc2AddonDoubleGasCost = 50;
    public static final int sc2AddonTechGasCost = 25;
    public static final float sc2AddonDoubleTime = 36f * 60f;
    public static final float sc2AddonTechTime = 18f * 60f;

    public Seq<UnitPlan> plans = new Seq<>(4);
    public Sound createSound = Sounds.unitCreate;
    public float createSoundVolume = 1f;

    public UnitFactory(String name){
        super(name);
        update = true;
        hasPower = true;
        hasItems = true;
        solid = true;
        configurable = true;
        clearOnDoubleTap = true;
        outputsPayload = true;
        rotate = true;
        regionRotated1 = 1;
        commandable = true;
        ambientSound = Sounds.loopUnitBuilding;
        ambientSoundVolume = 0.09f;

        config(Integer.class, (UnitFactoryBuild build, Integer i) -> {
            if(!configurable) return;

            if(sc2Queue){
                if(i == sc2AddonTechConfig){
                    build.startAddonBuild(Blocks.memoryBank, sc2AddonCrystalCost, sc2AddonTechGasCost, sc2AddonTechTime);
                    return;
                }
                if(i == sc2AddonDoubleConfig){
                    build.startAddonBuild(Blocks.rotaryPump, sc2AddonCrystalCost, sc2AddonDoubleGasCost, sc2AddonDoubleTime);
                    return;
                }
                if(i == sc2AddonCancelConfig){
                    if(build.isAddonBuilding()){
                        build.cancelAddonBuild(true);
                    }else{
                        build.cancelLastQueued();
                    }
                    return;
                }
                if(i < 0) return;
                if(build.isAddonBuilding()) return;
                if(i >= plans.size) return;
                build.queuePlan(i);
                return;
            }

            if(build.currentPlan == i) return;
            build.currentPlan = i < 0 || i >= plans.size ? -1 : i;
            build.progress = 0;
            if(build.command != null && (build.unit() == null || !build.unit().commands.contains(build.command))){
                build.command = null;
            }
        });

        config(UnitType.class, (UnitFactoryBuild build, UnitType val) -> {
            if(!configurable) return;

            if(sc2Queue){
                int next = plans.indexOf(p -> p.unit == val);
                if(next != -1){
                    build.queuePlan(next);
                }
                return;
            }

            int next = plans.indexOf(p -> p.unit == val);
            if(build.currentPlan == next) return;
            build.currentPlan = next;
            build.progress = 0;
            if(build.command != null && !val.commands.contains(build.command)){
                build.command = null;
            }
        });

        config(UnitCommand.class, (UnitFactoryBuild build, UnitCommand command) -> build.command = command);
        configClear((UnitFactoryBuild build) -> build.command = null);

        consume(new ConsumeItemDynamic((UnitFactoryBuild e) -> e.currentPlan != -1 ? plans.get(Math.min(e.currentPlan, plans.size - 1)).requirements : ItemStack.empty));
    }

    @Override
    public void init(){
        initCapacities();
        super.init();
    }

    @Override
    public void afterPatch(){
        initCapacities();
        super.afterPatch();
    }

    public void initCapacities(){
        capacities = new int[Vars.content.items().size];
        itemCapacity = 10; //unit factories can't control their own capacity externally, setting the value does nothing
        for(UnitPlan plan : plans){
            for(ItemStack stack : plan.requirements){
                capacities[stack.item.id] = Math.max(capacities[stack.item.id], stack.amount * 2);
                itemCapacity = Math.max(itemCapacity, stack.amount * 2);
            }
        }

        consumeBuilder.each(c -> c.multiplier = b -> state.rules.unitCost(b.team));
    }

    @Override
    public void setBars(){
        super.setBars();
        addBar("progress", (UnitFactoryBuild e) -> new Bar("bar.progress", Pal.ammo, e::fraction));

        addBar("units", (UnitFactoryBuild e) ->
        new Bar(
            () -> e.unit() == null ? "[lightgray]" + Iconc.cancel :
                Core.bundle.format("bar.unitcap",
                    Fonts.getUnicodeStr(e.unit().name),
                    e.team.data().countType(e.unit()),
                    e.unit() == null ? Units.getStringCap(e.team) : (e.unit().useUnitCap ? Units.getStringCap(e.team) : "∞")
                ),
            () -> Pal.power,
            () -> e.unit() == null ? 0f : (e.unit().useUnitCap ? (float)e.team.data().countType(e.unit()) / Units.getCap(e.team) : 1f)
        ));
    }

    @Override
    public boolean outputsItems(){
        return false;
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.remove(Stat.itemCapacity);

        stats.add(Stat.output, table -> {
            table.row();

            for(var plan : plans){
                table.table(Styles.grayPanel, t -> {

                    if(plan.unit.isBanned()){
                        t.image(Icon.cancel).color(Pal.remove).size(40);
                        return;
                    }

                    if(plan.unit.unlockedNow()){
                        t.image(plan.unit.uiIcon).size(40).pad(10f).left().scaling(Scaling.fit).with(i -> StatValues.withTooltip(i, plan.unit));
                        t.table(info -> {
                            info.add(plan.unit.localizedName).left();
                            info.row();
                            info.add(Strings.autoFixed(plan.time / 60f, 1) + " " + Core.bundle.get("unit.seconds")).color(Color.lightGray);
                        }).left();

                        t.table(req -> {
                            req.right();
                            for(int i = 0; i < plan.requirements.length; i++){
                                if(i % 6 == 0){
                                    req.row();
                                }

                                ItemStack stack = plan.requirements[i];
                                req.add(StatValues.displayItem(stack.item, stack.amount, plan.time, true)).pad(5);
                            }
                        }).right().grow().pad(10f);
                    }else{
                        t.image(Icon.lock).color(Pal.darkerGray).size(40);
                    }
                }).growX().pad(5);
                table.row();
            }
        });
    }

    @Override
    public TextureRegion[] icons(){
        return new TextureRegion[]{region, outRegion, topRegion};
    }

    @Override
    public void drawPlanRegion(BuildPlan plan, Eachable<BuildPlan> list){
        Draw.rect(region, plan.drawx(), plan.drawy());
        Draw.rect(outRegion, plan.drawx(), plan.drawy(), plan.rotation * 90);
        Draw.rect(topRegion, plan.drawx(), plan.drawy());
    }

    @Override
    public void getPlanConfigs(Seq<UnlockableContent> options){
        for(var plan : plans){
            if(!plan.unit.isBanned()){
                options.add(plan.unit);
            }
        }
    }

    public static class UnitPlan{
        public UnitType unit;
        public ItemStack[] requirements;
        public float time;

        public UnitPlan(UnitType unit, float time, ItemStack[] requirements){
            this.unit = unit;
            this.time = time;
            this.requirements = requirements;
        }

        UnitPlan(){}
    }

    public class UnitFactoryBuild extends UnitBuild{
        public @Nullable Vec2 commandPos;
        public @Nullable Teamc commandTarget;
        public int commandTargetUnit = -1;
        public int commandTargetBuilding = -1;
        private final Vec2 commandPosDynamic = new Vec2();
        public @Nullable UnitCommand command;
        public int currentPlan = -1;
        public int queued = 0;
        public float progress2 = 0f;
        public float speedScl2 = 0f;
        public int addonBuildPos = -1;
        public int addonBuildBlock = -1;
        public float addonBuildTime = 0f;
        public int addonCrystalCost = 0;
        public int addonGasCost = 0;
        public boolean hadDoubleAddon = false;

        public float fraction(){
            if(sc2Queue && queued <= 0) return 0f;
            return currentPlan == -1 ? 0 : progress / plans.get(currentPlan).time;
        }

        public boolean canSetCommand(){
            var output = unit();
            return output != null && output.commands.size > 1 && output.allowChangeCommands &&
                //to avoid cluttering UI, don't show command selection for "standard" units that only have two commands.
                !(output.commands.size == 2 && output.commands.get(1) == UnitCommand.enterPayloadCommand);
        }

        @Override
        public void created(){
            //auto-set to the first plan, it's better than nothing.
            if(currentPlan == -1){
                currentPlan = plans.indexOf(u -> u.unit.unlockedNow());
            }
        }

        @Override
        public void drawSelect(){
            super.drawSelect();
            if(plans.size > 1 && currentPlan != -1 && currentPlan < plans.size){
                drawItemSelection(plans.get(currentPlan).unit);
            }
        }

        @Override
        public Vec2 getCommandPosition(){
            Teamc target = resolveCommandTarget();
            if(target != null){
                return commandPosDynamic.set(target.getX(), target.getY());
            }
            return commandPos;
        }

        @Override
        public void onCommand(Vec2 target){
            Teamc found = findCommandTarget(target);
            if(found != null){
                setCommandTarget(found);
                commandPos = null;
            }else{
                clearCommandTarget();
                commandPos = target;
            }
        }

        @Override
        public Object senseObject(LAccess sensor){
            if(sensor == LAccess.config) return currentPlan == -1 ? null : plans.get(currentPlan).unit;
            return super.senseObject(sensor);
        }

        @Override
        public double sense(LAccess sensor){
            if(sensor == LAccess.progress) return Mathf.clamp(fraction());
            if(sensor == LAccess.itemCapacity) return Mathf.round(itemCapacity * state.rules.unitCost(team));
            return super.sense(sensor);
        }

        @Override
        public void buildConfiguration(Table table){
            Seq<UnitType> units = Seq.with(plans).map(u -> u.unit).retainAll(u -> u.unlockedNow() && !u.isBanned());

            if(units.any()){
                ItemSelection.buildTable(UnitFactory.this, table, units, () -> currentPlan == -1 ? null : plans.get(currentPlan).unit, unit -> configure(plans.indexOf(u -> u.unit == unit)), selectionRows, selectionColumns);

                table.row();

                Table commands = new Table();
                commands.top().left();

                Runnable rebuildCommands = () -> {
                    commands.clear();
                    commands.background(null);
                    var unit = unit();
                    if(unit != null && canSetCommand()){
                        commands.background(Styles.black6);
                        var group = new ButtonGroup<ImageButton>();
                        group.setMinCheckCount(0);
                        int i = 0, columns = Mathf.clamp(units.size, 2, selectionColumns);
                        var list = unit.commands;

                        commands.image(Tex.whiteui, Pal.gray).height(4f).growX().colspan(columns).row();

                        for(var item : list){
                            if(item.hidden) continue;
                            ImageButton button = commands.button(item.getIcon(), Styles.clearNoneTogglei, 40f, () -> {
                                configure(item);
                            }).tooltip(item.localized()).group(group).get();

                            button.update(() -> button.setChecked(command == item || (command == null && unit.defaultCommand == item)));

                            if(++i % columns == 0){
                                commands.row();
                            }
                        }

                        if(i > 0 && i < columns){
                            for(int j = 0; j < (columns - i); j++){
                                commands.add().size(40f);
                            }
                        }
                    }
                };

                rebuildCommands.run();

                //Since the menu gets hidden when a new unit is selected, this is unnecessary.
                /*
                UnitType[] lastUnit = {unit()};

                commands.update(() -> {
                    if(lastUnit[0] != unit()){
                        lastUnit[0] = unit();
                        rebuildCommands.run();
                    }
                });*/

                table.row();

                table.add(commands).fillX().left();

            }else{
                table.table(Styles.black3, t -> t.add("@none").color(Color.lightGray));
            }
        }

        @Override
        public boolean acceptPayload(Building source, Payload payload){
            return false;
        }

        @Override
        public void display(Table table){
            super.display(table);

            TextureRegionDrawable reg = new TextureRegionDrawable();

            table.row();
            table.table(t -> {
                t.left();
                t.image().update(i -> {
                    i.setDrawable(currentPlan == -1 ? Icon.cancel : reg.set(plans.get(currentPlan).unit.uiIcon));
                    i.setScaling(Scaling.fit);
                    i.setColor(currentPlan == -1 ? Color.lightGray : Color.white);
                }).size(32).padBottom(-4).padRight(2);
                t.label(() -> currentPlan == -1 ? "@none" : plans.get(currentPlan).unit.localizedName).wrap().width(230f).color(Color.lightGray);
            }).left();
        }

        @Override
        public Object config(){
            return currentPlan;
        }

        @Override
        public void draw(){
            Draw.rect(region, x, y);
            Draw.rect(outRegion, x, y, rotdeg());

            if(currentPlan != -1){
                UnitPlan plan = plans.get(currentPlan);
                Draw.draw(Layer.blockOver, () -> Drawf.construct(this, plan.unit, rotdeg() - 90f, progress / plan.time, speedScl, time));
            }

            Draw.z(Layer.blockOver);

            payRotation = rotdeg();
            drawPayload();

            Draw.z(Layer.blockOver + 0.1f);

            Draw.rect(topRegion, x, y);
        }

        @Override
        public void updateTile(){
            if(!configurable){
                currentPlan = 0;
            }

            if(currentPlan < 0 || currentPlan >= plans.size){
                currentPlan = -1;
            }

            updateCommandTarget();

            moveOutPayload();

            if(sc2Queue){
                updateSc2Queue();
                return;
            }

            if(efficiency > 0 && currentPlan != -1){
                time += edelta() * speedScl * Vars.state.rules.unitBuildSpeed(team);
                progress += edelta() * Vars.state.rules.unitBuildSpeed(team);
                speedScl = Mathf.lerpDelta(speedScl, 1f, 0.05f);
            }else{
                speedScl = Mathf.lerpDelta(speedScl, 0f, 0.05f);
            }

            if(currentPlan != -1 && payload == null){
                UnitPlan plan = plans.get(currentPlan);

                //make sure to reset plan when the unit got banned after placement
                if(plan.unit.isBanned()){
                    currentPlan = -1;
                    return;
                }

                if(progress >= plan.time){
                    progress %= 1f;

                    spawnUnit(plan);
                }

                progress = Mathf.clamp(progress, 0, plan.time);
            }else{
                progress = 0f;
            }
        }

        @Override
        public boolean shouldConsume(){
            if(sc2Queue){
                if(isAddonBuilding()) return enabled;
                if(currentPlan == -1 || queued <= 0) return false;
            }else{
                if(currentPlan == -1) return false;
            }
            return enabled && payload == null;
        }

        @Override
        public boolean consumeTriggerValid(){
            if(sc2Queue && queued > 0) return true;
            return super.consumeTriggerValid();
        }

        @Override
        public int getMaximumAccepted(Item item){
            return Mathf.round(capacities[item.id] * state.rules.unitCost(team));
        }

        @Override
        public boolean acceptItem(Building source, Item item){
            return currentPlan != -1 && items.get(item) < getMaximumAccepted(item) &&
                Structs.contains(plans.get(currentPlan).requirements, stack -> stack.item == item);
        }

        public @Nullable UnitType unit(){
            return currentPlan == - 1 ? null : plans.get(currentPlan).unit;
        }

        @Override
        public byte version(){
            return 7;
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.f(progress);
            write.s(currentPlan);
            TypeIO.writeVecNullable(write, commandPos);
            TypeIO.writeCommand(write, command);
            write.i(commandTargetUnit);
            write.i(commandTargetBuilding);
            write.i(queued);
            write.f(progress2);
            write.i(addonBuildPos);
            write.i(addonBuildBlock);
            write.f(addonBuildTime);
            write.i(addonCrystalCost);
            write.i(addonGasCost);
            write.bool(hadDoubleAddon);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            progress = read.f();
            currentPlan = read.s();
            if(revision >= 2){
                commandPos = TypeIO.readVecNullable(read);
            }

            if(revision >= 3){
                command = TypeIO.readCommand(read);
            }
            if(revision >= 4){
                commandTargetUnit = read.i();
                commandTargetBuilding = read.i();
            }
            if(revision >= 5){
                queued = read.i();
                progress2 = read.f();
            }else{
                queued = currentPlan == -1 ? 0 : 1;
                progress2 = 0f;
            }
            if(revision >= 6){
                addonBuildPos = read.i();
                addonBuildBlock = read.i();
                addonBuildTime = read.f();
                addonCrystalCost = read.i();
                addonGasCost = read.i();
            }else{
                addonBuildPos = -1;
                addonBuildBlock = -1;
                addonBuildTime = 0f;
                addonCrystalCost = 0;
                addonGasCost = 0;
            }
            if(revision >= 7){
                hadDoubleAddon = read.bool();
            }else{
                hadDoubleAddon = false;
            }
        }

        private void updateCommandTarget(){
            boolean hadTarget = commandTargetUnit != -1 || commandTargetBuilding != -1 || commandTarget != null;
            if(hadTarget && resolveCommandTarget() == null){
                clearCommandTarget();
                commandPos = new Vec2(x, y);
            }
        }

        private @Nullable Teamc resolveCommandTarget(){
            Teamc target = commandTarget;
            if(target instanceof Unit){
                Unit u = (Unit)target;
                if(!u.isValid() || u.team != team) target = null;
            }else if(target instanceof Building){
                Building b = (Building)target;
                if(!b.isValid() || b.team != team) target = null;
            }

            if(target == null){
                if(commandTargetUnit != -1){
                    Unit u = Groups.unit.getByID(commandTargetUnit);
                    if(u != null && u.team == team){
                        target = u;
                    }else{
                        commandTargetUnit = -1;
                    }
                }else if(commandTargetBuilding != -1){
                    Building b = world.build(commandTargetBuilding);
                    if(b != null && b.team == team){
                        target = b;
                    }else{
                        commandTargetBuilding = -1;
                    }
                }
                commandTarget = target;
            }

            return target;
        }

        private void clearCommandTarget(){
            commandTarget = null;
            commandTargetUnit = -1;
            commandTargetBuilding = -1;
        }

        private void setCommandTarget(Teamc target){
            clearCommandTarget();
            commandTarget = target;
            if(target instanceof Unit){
                commandTargetUnit = ((Unit)target).id;
            }else if(target instanceof Building){
                commandTargetBuilding = ((Building)target).pos();
            }
        }

        private @Nullable Teamc findCommandTarget(Vec2 target){
            Building build = world.buildWorld(target.x, target.y);
            if(build != null && build.team == team && build.within(target.x, target.y, build.hitSize() / 2f)){
                return build;
            }

            Unit unit = Units.closest(team, target.x, target.y, 40f, u -> u.team == team);
            if(unit != null && unit.within(target.x, target.y, unit.hitSize / 2f)){
                return unit;
            }

            return null;
        }

        public boolean queuePlan(int planIndex){
            if(!canQueuePlan(planIndex)) return false;
            UnitPlan plan = plans.get(planIndex);
            if(queued == 0){
                currentPlan = planIndex;
                progress = 0f;
                progress2 = 0f;
                speedScl = 0f;
                speedScl2 = 0f;
            }
            payForPlan(plan, 1);
            queued++;
            return true;
        }

        public boolean canQueuePlan(int planIndex){
            if(!sc2Queue) return false;
            if(planIndex < 0 || planIndex >= plans.size) return false;
            UnitPlan plan = plans.get(planIndex);
            if(plan.unit.isBanned()) return false;
            if(!canProduce(plan.unit)) return false;
            if(queued > 0 && currentPlan != planIndex) return false;
            if(queued >= queueSlots()) return false;
            if(!canAffordPlan(plan, 1)) return false;
            return true;
        }

        public boolean canProduce(@Nullable UnitType unit){
            if(unit == null) return false;
            if(unit == UnitTypes.fortress || unit == UnitTypes.ghost){
                if(!hasTechAddon()) return false;
            }
            if(unit == UnitTypes.ghost){
                if(team.data().getCount(Blocks.launchPad) <= 0) return false;
            }
            return Units.canCreate(team, unit);
        }

        public boolean cancelLastQueued(){
            if(!sc2Queue || queued <= 0) return false;
            if(currentPlan != -1){
                refundPlan(plans.get(currentPlan), 1);
            }
            queued--;
            if(queued < 2){
                progress2 = 0f;
            }
            if(queued == 0){
                currentPlan = -1;
                progress = 0f;
                speedScl = 0f;
                speedScl2 = 0f;
            }
            return true;
        }

        public boolean sc2QueueEnabled(){
            return UnitFactory.this.sc2Queue;
        }

        public boolean canShowAddonButtons(){
            if(!sc2Queue || !UnitFactory.this.sc2AddonSupport) return false;
            return !isAddonBuilding() && !hasDoubleAddon() && !hasTechAddon();
        }

        public int activeUnitSlots(){
            if(!sc2Queue || !hasDoubleAddon()) return 1;
            return 2;
        }

        public int queueSlots(){
            if(!sc2Queue) return 0;
            return hasDoubleAddon() ? UnitFactory.this.sc2QueueSlotsAddon : UnitFactory.this.sc2QueueSlots;
        }

        public @Nullable UnitType queuedUnit(int index){
            if(!sc2Queue || currentPlan == -1) return null;
            if(index < 0) return null;
            UnitType unit = plans.get(currentPlan).unit;
            boolean slot2Running = activeUnitSlots() > 1 && progress2 > 0f;
            if(index == 0){
                if(slot2Running && queued == 1 && progress <= 0f) return null;
                if(queued >= 1 || progress > 0f) return unit;
                return null;
            }
            if(index == 1){
                if(activeUnitSlots() > 1 && (queued >= 2 || progress2 > 0f)) return unit;
                return null;
            }
            int total = queued;
            if(index >= total) return null;
            return unit;
        }

        public float unitProgressFraction(int slot){
            if(currentPlan == -1 || queued <= 0) return 0f;
            UnitPlan plan = plans.get(currentPlan);
            if(plan.time <= 0f) return 0f;
            if(slot == 0) return Mathf.clamp(progress / plan.time);
            if(slot == 1) return Mathf.clamp(progress2 / plan.time);
            return 0f;
        }

        public float unitProgressSeconds(int slot){
            if(currentPlan == -1) return 0f;
            UnitPlan plan = plans.get(currentPlan);
            if(slot == 0) return progress / 60f;
            if(slot == 1) return progress2 / 60f;
            return 0f;
        }

        public float unitProgressTotalSeconds(int slot){
            if(currentPlan == -1) return 0f;
            UnitPlan plan = plans.get(currentPlan);
            return plan.time / 60f;
        }

        public boolean isAddonBuilding(){
            if(addonBuildPos == -1 || addonBuildBlock == -1) return false;
            Tile tile = world.tile(addonBuildPos);
            if(tile == null || tile.build == null) return false;
            if(tile.build instanceof ConstructBlock.ConstructBuild cons){
                return cons.current != null && cons.current.id == addonBuildBlock && cons.progress < 1f;
            }
            return false;
        }

        public @Nullable Block addonBuildingBlock(){
            return addonBuildBlock == -1 ? null : content.block(addonBuildBlock);
        }

        public float addonBuildFraction(){
            if(!isAddonBuilding() || addonBuildTime <= 0f) return 0f;
            Tile tile = world.tile(addonBuildPos);
            if(tile == null || !(tile.build instanceof ConstructBlock.ConstructBuild cons)) return 0f;
            return Mathf.clamp(cons.progress);
        }

        public float addonBuildSeconds(){
            if(!isAddonBuilding() || addonBuildTime <= 0f) return 0f;
            return addonBuildFraction() * (addonBuildTime / 60f);
        }

        public float addonBuildTotalSeconds(){
            if(addonBuildTime <= 0f) return 0f;
            return addonBuildTime / 60f;
        }

        public boolean hasDoubleAddon(){
            if(!sc2Queue || !UnitFactory.this.sc2AddonSupport) return false;
            return hasAddon(Blocks.rotaryPump);
        }

        public boolean hasTechAddon(){
            if(!sc2Queue || !UnitFactory.this.sc2AddonSupport) return false;
            return hasAddon(Blocks.memoryBank);
        }

        public boolean canAffordAddon(int crystal, int gas){
            if(state.rules.infiniteResources || team.rules().infiniteResources) return true;
            CoreBuild core = team.core();
            if(core == null) return false;
            if(crystal > 0 && !core.items.has(Items.graphite, crystal)) return false;
            if(gas > 0 && !core.items.has(Items.highEnergyGas, gas)) return false;
            return true;
        }

        public boolean startAddonBuild(Block addon, int crystalCost, int gasCost, float buildTime){
            if(!sc2Queue || !UnitFactory.this.sc2AddonSupport) return false;
            if(addon == null) return false;
            if(isAddonBuilding() || hasDoubleAddon() || hasTechAddon()) return false;
            if(!canAffordAddon(crystalCost, gasCost)) return false;

            Tile addonTile = addonTile();
            if(addonTile == null) return false;
            if(!Build.validPlaceIgnoreUnits(addon, team, addonTile.x, addonTile.y, 0, true, true)) return false;

            if(!state.rules.infiniteResources && !team.rules().infiniteResources){
                CoreBuild core = team.core();
                if(core == null) return false;
                if(crystalCost > 0) core.items.remove(Items.graphite, crystalCost);
                if(gasCost > 0) core.items.remove(Items.highEnergyGas, gasCost);
            }

            if(!net.client()){
                Call.beginPlace(null, addon, team, addonTile.x, addonTile.y, 0, null);
                int pos = Point2.pack(addonTile.x, addonTile.y);
                ConstructBlock.markPrepaid(pos);
                ConstructBlock.markForceBuildTime(pos);
            }

            addonBuildPos = Point2.pack(addonTile.x, addonTile.y);
            addonBuildBlock = addon.id;
            addonBuildTime = buildTime;
            addonCrystalCost = crystalCost;
            addonGasCost = gasCost;
            return true;
        }

        public void cancelAddonBuild(boolean refund){
            if(addonBuildPos == -1 || addonBuildBlock == -1) return;
            Tile tile = world.tile(addonBuildPos);
            Block addon = addonBuildingBlock();
            if(tile != null && tile.build instanceof ConstructBlock.ConstructBuild cons && addon != null){
                if(cons.current != null && cons.current.id == addonBuildBlock && cons.progress < 1f){
                    ConstructBlock.consumePrepaid(tile.pos());
                    ConstructBlock.clearForceBuildTime(tile.pos());
                    if(refund) refundAddonCost();
                    ConstructBlock.deconstructFinish(tile, addon, null);
                }
            }
            clearAddonBuildState();
        }

        private boolean hasAddon(Block addon){
            int size = block.size;
            int baseX = tile.x - (size - 1) / 2;
            int baseY = tile.y - (size - 1) / 2;
            Tile other = world.tile(baseX + size, baseY);
            if(other == null || other.build == null) return false;
            return other.build.block == addon && other.build.team == team;
        }

        private void updateSc2Queue(){
            if(isAddonBuilding()){
                updateAddonBuild();
                return;
            }

            boolean doubleAddon = hasDoubleAddon();
            if(hadDoubleAddon && !doubleAddon){
                handleDoubleAddonLoss();
            }
            hadDoubleAddon = doubleAddon;

            int maxQueue = queueSlots();
            if(queued > maxQueue){
                if(currentPlan != -1){
                    refundPlan(plans.get(currentPlan), queued - maxQueue);
                }
                queued = maxQueue;
            }

            if(currentPlan == -1 || queued <= 0){
                queued = 0;
                progress = 0f;
                progress2 = 0f;
                speedScl = Mathf.lerpDelta(speedScl, 0f, 0.05f);
                speedScl2 = Mathf.lerpDelta(speedScl2, 0f, 0.05f);
                return;
            }

            UnitPlan plan = plans.get(currentPlan);
            if(plan.unit.isBanned()){
                if(queued <= 0){
                    queued = 0;
                    currentPlan = -1;
                    progress = 0f;
                    progress2 = 0f;
                    return;
                }
            }

            boolean canBuild = efficiency > 0 && payload == null;
            float speed = Vars.state.rules.unitBuildSpeed(team);
            int activeSlots = activeUnitSlots();
            if(activeSlots <= 1){
                progress2 = 0f;
            }

            boolean slot2Active = activeSlots > 1 && (queued >= 2 || progress2 > 0f);
            boolean slot1Active = queued >= 1 && (progress > 0f || !slot2Active);

            if(canBuild){
                if(slot1Active){
                    time += edelta() * speedScl * speed;
                    progress += edelta() * speed;
                    speedScl = Mathf.lerpDelta(speedScl, 1f, 0.05f);
                }else{
                    speedScl = Mathf.lerpDelta(speedScl, 0f, 0.05f);
                }

                if(slot2Active){
                    progress2 += edelta() * speed;
                    speedScl2 = Mathf.lerpDelta(speedScl2, 1f, 0.05f);
                }else{
                    speedScl2 = Mathf.lerpDelta(speedScl2, 0f, 0.05f);
                }
            }else{
                speedScl = Mathf.lerpDelta(speedScl, 0f, 0.05f);
                speedScl2 = Mathf.lerpDelta(speedScl2, 0f, 0.05f);
            }

            if(payload == null){
                if(slot1Active && progress >= plan.time){
                    spawnUnit(plan);
                    queued--;
                    progress = 0f;
                    speedScl = 0f;
                }

                if(payload == null && slot2Active && progress2 >= plan.time){
                    spawnUnit(plan);
                    queued--;
                    progress2 = 0f;
                    speedScl2 = 0f;
                }
            }

            if(queued <= 0){
                currentPlan = -1;
                progress = 0f;
                progress2 = 0f;
            }else if(queued < 2){
            }

            progress = Mathf.clamp(progress, 0, plan.time);
            progress2 = Mathf.clamp(progress2, 0, plan.time);
        }

        private void updateAddonBuild(){
            if(addonBuildPos == -1 || addonBuildBlock == -1) return;
            if(!enabled) return;
            Tile tile = world.tile(addonBuildPos);
            if(tile == null || tile.build == null){
                clearAddonBuildState();
                return;
            }

            if(tile.build.block.id == addonBuildBlock){
                clearAddonBuildState();
                return;
            }

            if(tile.build instanceof ConstructBlock.ConstructBuild cons && cons.current != null && cons.current.id == addonBuildBlock){
                float time = addonBuildTime <= 0f ? 1f : addonBuildTime;
                cons.construct(null, null, delta() / time, null);
                if(cons.progress >= 1f && tile.build.block.id == addonBuildBlock){
                    clearAddonBuildState();
                }
            }else{
                clearAddonBuildState();
            }
        }

        private void refundAddonCost(){
            CoreBuild core = team.core();
            if(core == null) return;
            int refundCrystal = (int)Mathf.ceil(addonCrystalCost * 0.75f);
            int refundGas = (int)Mathf.ceil(addonGasCost * 0.75f);
            if(refundCrystal > 0) core.items.add(Items.graphite, refundCrystal);
            if(refundGas > 0) core.items.add(Items.highEnergyGas, refundGas);
        }

        private void clearAddonBuildState(){
            addonBuildPos = -1;
            addonBuildBlock = -1;
            addonBuildTime = 0f;
            addonCrystalCost = 0;
            addonGasCost = 0;
        }

        private @Nullable Tile addonTile(){
            int size = block.size;
            int baseX = tile.x - (size - 1) / 2;
            int baseY = tile.y - (size - 1) / 2;
            return world.tile(baseX + size, baseY);
        }

        private int planItemCost(ItemStack stack){
            return Math.round(stack.amount * state.rules.unitCost(team));
        }

        private boolean canAffordPlan(UnitPlan plan, int count){
            if(state.rules.infiniteResources || team.rules().infiniteResources) return true;
            CoreBuild core = team.core();
            if(core == null) return false;
            for(ItemStack stack : plan.requirements){
                int amount = planItemCost(stack) * count;
                if(amount > 0 && !core.items.has(stack.item, amount)){
                    return false;
                }
            }
            return true;
        }

        private void payForPlan(UnitPlan plan, int count){
            if(state.rules.infiniteResources || team.rules().infiniteResources) return;
            CoreBuild core = team.core();
            if(core == null) return;
            for(ItemStack stack : plan.requirements){
                int amount = planItemCost(stack) * count;
                if(amount > 0){
                    core.items.remove(stack.item, amount);
                }
            }
        }

        private void refundPlan(UnitPlan plan, int count){
            if(count <= 0 || state.rules.infiniteResources || team.rules().infiniteResources) return;
            CoreBuild core = team.core();
            for(ItemStack stack : plan.requirements){
                int amount = planItemCost(stack) * count;
                if(amount <= 0) continue;
                if(core != null){
                    core.items.add(stack.item, amount);
                }else if(items != null){
                    items.add(stack.item, amount);
                }
            }
        }

        private void handleDoubleAddonLoss(){
            progress2 = 0f;
            speedScl2 = 0f;
            if(currentPlan == -1 || queued <= 0) return;

            UnitPlan plan = plans.get(currentPlan);
            int refund = 0;

            if(queued >= 2){
                queued--;
                refund++;
            }

            int maxQueue = UnitFactory.this.sc2QueueSlots;
            if(queued > maxQueue){
                refund += queued - maxQueue;
                queued = maxQueue;
            }

            refundPlan(plan, refund);
        }

        private void spawnUnit(UnitPlan plan){
            Unit unit = plan.unit.create(team);
            if(unit.isCommandable()){
                unit.command().command(command == null && unit.type.defaultCommand != null ? unit.type.defaultCommand : command);
                Teamc target = resolveCommandTarget();
                if(target != null){
                    unit.command().commandFollow(target);
                }else if(commandPos != null){
                    unit.command().commandPosition(commandPos);
                }
            }

            createSound.at(this, 1f + Mathf.range(0.06f), createSoundVolume);
            Vec2 spawn = getSpawnPosition(unit);
            unit.set(spawn.x, spawn.y);
            unit.add();
            if(!sc2Queue){
                consume();
            }
            Events.fire(new UnitCreateEvent(unit, this));
        }

        private Vec2 getSpawnPosition(Unit unit){
            float offset = hitSize() / 2f + unit.hitSize / 2f + 2f;
            Vec2 dir = Tmp.v1.set(0f, -1f);
            Vec2 command = getCommandPosition();
            if(command != null){
                dir.set(command).sub(x, y);
                if(dir.len2() < 0.001f){
                    dir.set(0f, -1f);
                }
            }
            dir.setLength(offset);
            return Tmp.v2.set(x + dir.x, y + dir.y);
        }
    }
}
