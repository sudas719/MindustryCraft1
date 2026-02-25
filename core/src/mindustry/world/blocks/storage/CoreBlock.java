package mindustry.world.blocks.storage;

import arc.*;
import arc.audio.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.actions.*;
import arc.scene.event.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.*;
import mindustry.annotations.Annotations.*;
import mindustry.ai.types.*;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.ctype.*;
import mindustry.entities.*;
import mindustry.game.EventType.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.io.*;
import mindustry.logic.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.*;
import mindustry.world.blocks.environment.*;
import mindustry.world.meta.*;
import mindustry.world.modules.*;

import static mindustry.Vars.*;

public class CoreBlock extends StorageBlock{
    public static final float cloudScaling = 1700f, cfinScl = -2f, cfinOffset = 0.3f, calphaFinOffset = 0.25f, cloudAlpha = 0.81f;
    public static final float[] cloudAlphas = {0, 0.5f, 1f, 0.1f, 0, 0f};
    public static final int maxUnitQueue = 5;
    public static final int scvCost = 50;
    public static final float scvBuildTime = 12f * 60f;
    public static final int scvStorageCapacity = 5;
    public static final float orbitalEnergyCap = 200f;
    public static final float orbitalEnergyInit = 50f;
    public static final float orbitalEnergyRegen = 0.8f;
    public static final float orbitalAbilityCost = 50f;
    public static final int orbitalUpgradeCost = 150;
    public static final float orbitalUpgradeTime = 25f * 60f;
    public static final int fortressUpgradeCost = 150;
    public static final int fortressUpgradeGasCost = 150;
    public static final float fortressUpgradeTime = 36f * 60f;
    public static final int resourceExclusionRadiusTiles = 5;

    public int unitQueueSlots = maxUnitQueue;
    public int activeUnitSlots = 1;

    //hacky way to pass item modules between methods
    private static ItemModule nextItems;
    public static final float[] thrusterSizes = {0f, 0f, 0f, 0f, 0.3f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 0f};

    public @Load(value = "@-thruster1", fallback = "clear-effect") TextureRegion thruster1; //top right
    public @Load(value = "@-thruster2", fallback = "clear-effect") TextureRegion thruster2; //bot left
    public float thrusterLength = 14f/4f, thrusterOffset = 0f;
    public boolean isFirstTier;
    /** If false, players can't respawn at this core. */
    public boolean allowSpawn = true;
    /** If true, this core type requires a core zone to upgrade. */
    public boolean requiresCoreZone;
    public boolean incinerateNonBuildable = false;

    public UnitType unitType = UnitTypes.alpha;
    public float landDuration = 160f;
    public Music landMusic = Musics.land;
    public float launchSoundVolume = 1f, landSoundVolume = 1f;
    public Sound launchSound = Sounds.coreLaunch;
    public Sound landSound = Sounds.coreLand;
    public Effect launchEffect = Fx.launch;

    public Interp landZoomInterp = Interp.pow3;
    public float landZoomFrom = 0.02f, landZoomTo = 4f;

    public float captureInvicibility = 60f * 15f;

    public CoreBlock(String name){
        super(name);

        solid = true;
        update = true;
        hasItems = true;
        alwaysAllowDeposit = true;
        priority = TargetPriority.core;
        flags = EnumSet.of(BlockFlag.core);
        unitCapModifier = 10;
        sync = false; //core items are synced elsewhere
        drawDisabled = false;
        canOverdrive = false;
        commandable = true;
        envEnabled |= Env.space;

        //support everything
        replaceable = false;
        destroySound = Sounds.explosionCore;
        destroySoundVolume = 1.6f;
    }

    @Remote(called = Loc.server)
    public static void playerSpawn(Tile tile, Player player){
        //Disabled: Core units are no longer spawned
        //Players cannot possess units anymore
        return;
    }

    @Remote(called = Loc.server, targets = Loc.both, forward = true)
    public static void coreQueueUnit(Player player, int buildPos, int unitId){
        if(player == null) return;
        Building build = world.build(buildPos);
        if(!(build instanceof CoreBuild core) || build.team() != player.team()) return;
        if(unitId != UnitTypes.nova.id) return;
        core.queueUnit(unitId);
    }

    @Remote(called = Loc.server, targets = Loc.both, forward = true)
    public static void coreCancelUnit(Player player, int buildPos){
        if(player == null) return;
        Building build = world.build(buildPos);
        if(!(build instanceof CoreBuild core) || build.team() != player.team()) return;
        core.cancelCurrentUnit();
    }

    @Remote(called = Loc.server, targets = Loc.both, forward = true)
    public static void coreLaunch(Player player, int buildPos){
        if(player == null) return;
        Building build = world.build(buildPos);
        if(!(build instanceof CoreBuild core) || build.team() != player.team()) return;
        if(!headless){
            renderer.showLaunch(core);
        }
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.add(Stat.unitType, table -> {
            table.row();
            table.table(Styles.grayPanel, b -> {
                b.image(unitType.uiIcon).size(40).pad(10f).left().scaling(Scaling.fit);
                b.table(info -> {
                    info.add(unitType.localizedName).left();
                    if(Core.settings.getBool("console")){
                        info.row();
                        info.add(unitType.name).left().color(Color.lightGray);
                    }
                });
                b.button("?", Styles.flatBordert, () -> ui.content.show(unitType)).size(40f).pad(10).right().grow().visible(() -> unitType.unlockedNow());
            }).growX().pad(5).row();
        });
    }

    @Override
    public void setBars(){
        super.setBars();

        addBar("capacity", (CoreBuild e) -> new Bar(
            () -> Core.bundle.format("bar.capacity", UI.formatAmount(e.storageCapacity)),
            () -> Pal.items,
            () -> e.items.total() / ((float)e.storageCapacity * content.items().count(UnlockableContent::unlockedNow))
        ));

        addBar("orbital-energy", (CoreBuild e) -> {
            if(e.block != Blocks.coreOrbital) return null;
            return new Bar(
                () -> "Energy " + Strings.autoFixed(e.orbitalEnergy, 1) + "/" + Strings.autoFixed(orbitalEnergyCap, 1),
                () -> Color.valueOf("b57aff"),
                () -> Mathf.clamp(e.orbitalEnergy / orbitalEnergyCap)
            );
        });
    }

    @Override
    public void init(){
        //assign to update clipSize internally
        lightRadius = 30f + 20f * size;
        fogRadius = 12;
        emitLight = true;

        super.init();
    }

    @Override
    public void postInit(){
        super.postInit();

        //sync shown planets with unit spawned
        unitType.shownPlanets.addAll(shownPlanets);
    }

    @Override
    public boolean canBreak(Tile tile){
        return state.isEditor();
    }

    @Override
    public boolean canReplace(Block other){
        //coreblocks can upgrade smaller cores
        return super.canReplace(other) || (other instanceof CoreBlock && size >= other.size && other != this);
    }

    @Override
    public boolean canPlaceOn(Tile tile, Team team, int rotation){
        if(tile == null) return false;
        //in the editor, you can place them anywhere for convenience
        if(state.isEditor()) return true;
        if(tile.block() instanceof CoreBlock){
            return size > tile.block().size;
        }
        if(blockedByResourceNode(tile)) return false;

        CoreBuild core = team.core();

        //special floor upon which cores can be placed
        tile.getLinkedTilesAs(this, tempTiles);
        if(!tempTiles.contains(o -> !o.floor().allowCorePlacement || o.block() instanceof CoreBlock)){
            return true;
        }

        //must have all requirements (unless infinite)
        if(core == null || (!state.rules.infiniteResources && !core.items.has(requirements, state.rules.buildCostMultiplier))){
            return false;
        }

        //allow placing cores on any valid floor if resources are available
        return true;
    }

    private boolean blockedByResourceNode(Tile tile){
        if(tile == null) return false;
        tile.getLinkedTilesAs(this, tempTiles);
        float half = tilesize / 2f;
        for(Tile linked : tempTiles){
            if(inResourceExclusion(linked.worldx() + half, linked.worldy() + half)){
                return true;
            }
        }
        return false;
    }

    @Override
    public void placeBegan(Tile tile, Block previous, Unit builder){
        //finish placement immediately when a block is replaced.
        if(previous instanceof CoreBlock){
            tile.setBlock(this, tile.team());
            tile.block().placeEffect.at(tile, tile.block().size);
            Fx.upgradeCore.at(tile.drawx(), tile.drawy(), 0f, tile.block());
            Fx.upgradeCoreBloom.at(tile, tile.block().size);

            //set up the correct items
            if(nextItems != null){
                //force-set the total items
                if(tile.team().core() != null){
                    tile.team().core().items.set(nextItems);
                }

                nextItems = null;
            }

            Events.fire(new BlockBuildEndEvent(tile, builder, tile.team(), false, null));
        }
    }

    @Override
    public void beforePlaceBegan(Tile tile, Block previous){
        if(tile.build instanceof CoreBuild){
            //right before placing, create a "destination" item array which is all the previous items minus core requirements
            ItemModule items = tile.build.items.copy();
            if(!state.rules.infiniteResources){
                items.remove(ItemStack.mult(requirements, state.rules.buildCostMultiplier));
            }

            nextItems = items;
        }
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        if(world.tile(x, y) == null) return;
        if(!(world.tile(x, y).block() instanceof CoreBlock) && blockedByResourceNode(world.tile(x, y))){
            drawPlaceText("Too close to resource node", x, y, valid);
            return;
        }

        if(!canPlaceOn(world.tile(x, y), player.team(), rotation)){

            drawPlaceText(Core.bundle.get(
                isFirstTier ?
                    //TODO better message
                    "bar.corefloor" :
                    (player.team().core() != null && player.team().core().items.has(requirements, state.rules.buildCostMultiplier)) || state.rules.infiniteResources ?
                    "bar.corereq" :
                    "bar.noresources"
            ), x, y, valid);
        }
    }

    public class CoreBuild extends Building implements LaunchAnimator{
        public int storageCapacity;
        public boolean noEffect = false;
        public Team lastDamage = Team.derelict;
        public float iframes = -1f;
        public float thrusterTime = 0f;
        public @Nullable Vec2 commandPos;
        public IntSeq unitQueue = new IntSeq();
        public float unitProgress = 0f;
        public int storedScvs = 0;
        public IntSeq loadingScvs = new IntSeq();
        public float orbitalEnergy = -1f;
        public boolean upgradingOrbital = false;
        public float orbitalUpgradeProgress = 0f;
        public boolean upgradingFortress = false;
        public float fortressUpgradeProgress = 0f;

        protected float cloudSeed, landParticleTimer;

        @Override
        public boolean isCommandable(){
            return block.commandable;
        }

        @Override
        public Vec2 getCommandPosition(){
            return commandPos;
        }

        @Override
        public void onCommand(Vec2 target){
            if(target == null) return;
            Tile tile = world.tileWorld(target.x, target.y);
            Tile resource = resolveResourceTile(tile);
            if(resource != null){
                commandPos = new Vec2(resource.worldx(), resource.worldy());
                return;
            }

            Building build = world.buildWorld(target.x, target.y);
            if(build != null && build.block == Blocks.ventCondenser && build.team == team){
                Tile ventTile = findVentTile(build);
                if(ventTile != null){
                    commandPos = new Vec2(ventTile.worldx(), ventTile.worldy());
                    return;
                }
            }

            if(build != null){
                commandPos = new Vec2(build.x, build.y);
                return;
            }

            commandPos = new Vec2(target);
        }

        @Override
        public boolean canUnload(){
            return block.unloadable && state.rules.allowCoreUnloaders;
        }

        @Override
        public void draw(){
            //draw thrusters when just landed
            if(thrusterTime > 0){
                float frame = thrusterTime;

                Draw.alpha(1f);
                drawThrusters(frame);
                Draw.rect(block.region, x, y);
                Draw.alpha(Interp.pow4In.apply(frame));
                drawThrusters(frame);
                Draw.reset();

                drawTeamTop();
            }else{
                super.draw();
            }
        }

        @Override
        public float launchDuration(){
            return landDuration;
        }

        @Override
        public Music landMusic(){
            return landMusic;
        }

        @Override
        public void beginLaunch(boolean launching){
            cloudSeed = Mathf.random(1f);
            if(launching){
                Fx.coreLaunchConstruct.at(x, y, size);
            }

            if(!headless){
                (launching ? launchSound : landSound).at(Core.camera.position, 1f, (launching ? launchSoundVolume : landSoundVolume));
                // Add fade-in and fade-out foreground when landing or launching.
                if(renderer.isLaunching()){
                    float margin = 30f;

                    Image image = new Image();
                    image.color.a = 0f;
                    image.touchable = Touchable.disabled;
                    image.setFillParent(true);
                    image.actions(Actions.delay((launchDuration() - margin) / 60f), Actions.fadeIn(margin / 60f, Interp.pow2In), Actions.delay(6f / 60f), Actions.remove());
                    image.update(() -> {
                        image.toFront();
                        ui.loadfrag.toFront();
                        if(state.isMenu()){
                            image.remove();
                        }
                    });
                    Core.scene.add(image);
                }else{
                    Image image = new Image();
                    image.color.a = 1f;
                    image.touchable = Touchable.disabled;
                    image.setFillParent(true);
                    image.actions(Actions.fadeOut(35f / 60f), Actions.remove());
                    image.update(() -> {
                        image.toFront();
                        ui.loadfrag.toFront();
                        if(state.isMenu()){
                            image.remove();
                        }
                    });
                    Core.scene.add(image);

                    Time.run(launchDuration(), () -> {
                        launchEffect.at(this);
                        Effect.shake(5f, 5f, this);
                        thrusterTime = 1f;

                        if(state.isCampaign() && Vars.showSectorLandInfo && (state.rules.sector.preset == null || state.rules.sector.preset.showSectorLandInfo)){
                            ui.announce("[accent]" + state.rules.sector.name() + "\n" +
                                (state.rules.sector.info.resources.any() ? "[lightgray]" + Core.bundle.get("sectors.resources") + "[white] " +
                                    state.rules.sector.info.resources.toString(" ", UnlockableContent::emoji) : ""), 5);
                        }
                    });
                }
            }
        }

        @Override
        public void endLaunch(){}

        @Override
        public void drawLaunch(){
            var clouds = Core.assets.get("sprites/clouds.png", Texture.class);

            float fin = renderer.getLandTimeIn();
            float cameraScl = renderer.getDisplayScale();

            float fout = 1f - fin;
            float scl = Scl.scl(4f) / cameraScl;
            float pfin = Interp.pow3Out.apply(fin), pf = Interp.pow2In.apply(fout);

            //draw particles
            Draw.color(Pal.lightTrail);
            Angles.randLenVectors(1, pfin, 100, 800f * scl * pfin, (ax, ay, ffin, ffout) -> {
                Lines.stroke(scl * ffin * pf * 3f);
                Lines.lineAngle(x + ax, y + ay, Mathf.angle(ax, ay), (ffin * 20 + 1f) * scl);
            });
            Draw.color();

            drawLanding(x, y);

            Draw.color();
            Draw.mixcol(Color.white, Interp.pow5In.apply(fout));

            //accent tint indicating that the core was just constructed
            if(renderer.isLaunching()){
                float f = Mathf.clamp(1f - fout * 12f);
                if(f > 0.001f){
                    Draw.mixcol(Pal.accent, f);
                }
            }

            //draw clouds
            if(state.rules.cloudColor.a > 0.0001f){
                float scaling = cloudScaling;
                float sscl = Math.max(1f + Mathf.clamp(fin + cfinOffset) * cfinScl, 0f) * cameraScl;

                Tmp.tr1.set(clouds);
                Tmp.tr1.set(
                    (Core.camera.position.x - Core.camera.width/2f * sscl) / scaling,
                    (Core.camera.position.y - Core.camera.height/2f * sscl) / scaling,
                    (Core.camera.position.x + Core.camera.width/2f * sscl) / scaling,
                    (Core.camera.position.y + Core.camera.height/2f * sscl) / scaling);

                Tmp.tr1.scroll(10f * cloudSeed, 10f * cloudSeed);

                Draw.alpha(Mathf.sample(cloudAlphas, fin + calphaFinOffset) * cloudAlpha);
                Draw.mixcol(state.rules.cloudColor, state.rules.cloudColor.a);
                Draw.rect(Tmp.tr1, Core.camera.position.x, Core.camera.position.y, Core.camera.width, Core.camera.height);
                Draw.reset();
            }
        }

        public void drawLanding(float x, float y){
            float fin = renderer.getLandTimeIn();
            float fout = 1f - fin;

            float scl = Scl.scl(4f) / renderer.getDisplayScale();
            float shake = 0f;
            float s = region.width * region.scl() * scl * 3.6f * Interp.pow2Out.apply(fout);
            float rotation = Interp.pow2In.apply(fout) * 135f;
            x += Mathf.range(shake);
            y += Mathf.range(shake);
            float thrustOpen = 0.25f;
            float thrusterFrame = fin >= thrustOpen ? 1f : fin / thrustOpen;
            float thrusterSize = Mathf.sample(thrusterSizes, fin);

            //when launching, thrusters stay out the entire time.
            if(renderer.isLaunching()){
                Interp i = Interp.pow2Out;
                thrusterFrame = i.apply(Mathf.clamp(fout*13f));
                thrusterSize = i.apply(Mathf.clamp(fout*9f));
            }

            Draw.color(Pal.lightTrail);
            //TODO spikier heat
            Draw.rect("circle-shadow", x, y, s, s);

            Draw.scl(scl);

            //draw thruster flame
            float strength = (1f + (size - 3)/2.5f) * scl * thrusterSize * (0.95f + Mathf.absin(2f, 0.1f));
            float offset = (size - 3) * 3f * scl;

            for(int i = 0; i < 4; i++){
                Tmp.v1.trns(i * 90 + rotation, 1f);

                Tmp.v1.setLength((size * tilesize/2f + 1f)*scl + strength*2f + offset);
                Draw.color(team.color);
                Fill.circle(Tmp.v1.x + x, Tmp.v1.y + y, 6f * strength);

                Tmp.v1.setLength((size * tilesize/2f + 1f)*scl + strength*0.5f + offset);
                Draw.color(Color.white);
                Fill.circle(Tmp.v1.x + x, Tmp.v1.y + y, 3.5f * strength);
            }

            drawLandingThrusters(x, y, rotation, thrusterFrame);

            Drawf.spinSprite(region, x, y, rotation);

            Draw.alpha(Interp.pow4In.apply(thrusterFrame));
            drawLandingThrusters(x, y, rotation, thrusterFrame);
            Draw.alpha(1f);

            if(teamRegions[team.id] == teamRegion) Draw.color(team.color);

            Drawf.spinSprite(teamRegions[team.id], x, y, rotation);

            Draw.color();
            Draw.scl();
            Draw.reset();
        }

        protected void drawLandingThrusters(float x, float y, float rotation, float frame){
            float length = thrusterLength * (frame - 1f) - 1f/4f;
            float alpha = Draw.getColorAlpha();

            //two passes for consistent lighting
            for(int j = 0; j < 2; j++){
                for(int i = 0; i < 4; i++){
                    var reg = i >= 2 ? thruster2 : thruster1;
                    float rot = (i * 90) + rotation % 90f;
                    Tmp.v1.trns(rot, length * Draw.xscl);

                    //second pass applies extra layer of shading
                    if(j == 1){
                        Tmp.v1.rotate(-90f);
                        Draw.alpha((rotation % 90f) / 90f * alpha);
                        rot -= 90f;
                        Draw.rect(reg, x + Tmp.v1.x, y + Tmp.v1.y, rot);
                    }else{
                        Draw.alpha(alpha);
                        Draw.rect(reg, x + Tmp.v1.x, y + Tmp.v1.y, rot);
                    }
                }
            }
            Draw.alpha(1f);
        }

        public void drawThrusters(float frame){
            float length = thrusterLength * (frame - 1f) - 1f/4f;
            for(int i = 0; i < 4; i++){
                var reg = i >= 2 ? thruster2 : thruster1;
                float dx = Geometry.d4x[i] * length, dy = Geometry.d4y[i] * length;
                Draw.rect(reg, x + dx, y + dy, i * 90);
            }
        }

        @Override
        public void damage(@Nullable Team source, float damage){
            if(iframes > 0) return;

            if(source != null && source != team){
                lastDamage = source;
            }
            super.damage(source, damage);
        }

        @Override
        public void created(){
            super.created();

            Events.fire(new CoreChangeEvent(this));
        }

        @Override
        public void changeTeam(Team next){
            if(this.team == next) return;

            onRemoved();

            super.changeTeam(next);

            onProximityUpdate();

            Events.fire(new CoreChangeEvent(this));
        }

        @Override
        public double sense(LAccess sensor){
            if(sensor == LAccess.itemCapacity) return storageCapacity;
            if(sensor == LAccess.maxUnits) return Units.getCap(team);
            return super.sense(sensor);
        }

        @Override
        public double sense(Content content){
            if(content instanceof UnitType type) return team.data().countType(type);
            return super.sense(content);
        }

        @Override
        public boolean canControlSelect(Unit player){
            return player.isPlayer();
        }

        @Override
        public void onControlSelect(Unit unit){
            if(!unit.isPlayer() || !allowSpawn) return;
            Player player = unit.getPlayer();

            Fx.spawn.at(player);
            if(net.client() && player == Vars.player){
                control.input.controlledType = null;
            }

            player.clearUnit();
            player.deathTimer = Player.deathDelay + 1f;
            requestSpawn(player);
        }

        public void requestSpawn(Player player){
            //do not try to respawn in unsupported environments at all
            if(!unitType.supportsEnv(state.rules.env) || !allowSpawn) return;

            Call.playerSpawn(tile, player);
        }

        @Override
        public void updateTile(){
            iframes -= Time.delta;
            thrusterTime -= Time.delta/90f;
            updateOrbitalUpgrade();
            updateFortressUpgrade();
            updateUnitQueue();
            updateScvLoading();
            updateOrbitalEnergy();
        }

        public boolean canQueueUnit(UnitType type){
            if(type == null) return false;
            if(isUpgrading()) return false;
            if(unitQueue.size >= queueSlots()) return false;
            if(!Units.canCreate(team, type)) return false;
            if(state.rules.infiniteResources || team.rules().infiniteResources) return true;
            return items.has(Items.graphite, scvCost);
        }

        public boolean queueUnit(int unitId){
            if(unitQueue == null) unitQueue = new IntSeq();
            if(unitQueue.size >= queueSlots()) return false;
            if(unitId != UnitTypes.nova.id) return false;
            UnitType type = content.unit(unitId);
            if(type == null || !canQueueUnit(type)) return false;

            if(!state.rules.infiniteResources && !team.rules().infiniteResources){
                items.remove(Items.graphite, scvCost);
            }

            unitQueue.add(unitId);
            return true;
        }

        public boolean cancelCurrentUnit(){
            if(unitQueue == null || unitQueue.isEmpty()) return false;
            int index = unitQueue.size - 1;
            unitQueue.removeIndex(index);
            if(index == 0){
                unitProgress = 0f;
            }

            if(!state.rules.infiniteResources && !team.rules().infiniteResources){
                items.add(Items.graphite, scvCost);
            }
            return true;
        }

        public int activeUnitSlots(){
            return ((CoreBlock)block).activeUnitSlots;
        }

        public int queueSlots(){
            return ((CoreBlock)block).unitQueueSlots;
        }

        public float unitProgressFraction(int slot){
            if(slot != 0) return 0f;
            return unitProgressFraction();
        }

        public float unitProgressSeconds(int slot){
            if(slot != 0) return 0f;
            return unitProgressSeconds();
        }

        public float unitProgressTotalSeconds(int slot){
            if(slot != 0) return 0f;
            return unitProgressTotalSeconds();
        }

        public @Nullable UnitType queuedUnit(int index){
            if(unitQueue == null || index < 0 || index >= unitQueue.size) return null;
            return content.unit(unitQueue.get(index));
        }

        public float unitProgressFraction(){
            if(unitQueue == null || unitQueue.isEmpty()) return 0f;
            float time = unitBuildTime(queuedUnit(0));
            if(time <= 0f) return 0f;
            return Mathf.clamp(unitProgress / time);
        }

        public float unitProgressSeconds(){
            return unitProgress / 60f;
        }

        public float unitProgressTotalSeconds(){
            return unitBuildTime(queuedUnit(0)) / 60f;
        }

        public float unitBuildTime(@Nullable UnitType type){
            return scvBuildTime;
        }

        private void updateUnitQueue(){
            if(isUpgrading()){
                unitProgress = 0f;
                return;
            }
            if(unitQueue == null || unitQueue.isEmpty()){
                unitProgress = 0f;
                return;
            }

            UnitType type = queuedUnit(0);
            if(type == null || type.isBanned()){
                unitQueue.removeIndex(0);
                unitProgress = 0f;
                return;
            }

            unitProgress += edelta() * Vars.state.rules.unitBuildSpeed(team);
            float time = unitBuildTime(type);

            if(unitProgress >= time){
                unitProgress = 0f;
                unitQueue.removeIndex(0);
                spawnUnit(type);
            }
        }

        private void updateScvLoading(){
            if(loadingScvs.isEmpty()) return;
            for(int i = loadingScvs.size - 1; i >= 0; i--){
                int id = loadingScvs.get(i);
                Unit unit = Groups.unit.getByID(id);
                if(unit == null || !unit.isValid() || unit.team != team || unit.type != UnitTypes.nova){
                    loadingScvs.removeIndex(i);
                    continue;
                }
                if(storedScvs >= scvStorageLimit()){
                    loadingScvs.removeIndex(i);
                    continue;
                }
                if(unit.within(this, hitSize() / 2f + 4f)){
                    unit.remove();
                    storedScvs++;
                    loadingScvs.removeIndex(i);
                }
            }
        }

        public boolean hasStoredScvs(){
            return storedScvs > 0;
        }

        public int scvStorageLimit(){
            if(block == Blocks.coreOrbital || block == Blocks.corePlanetaryFortress){
                return UnitTypes.steelArmorUpgradedCoreScvCapacity(team);
            }
            return scvStorageCapacity;
        }

        public boolean requestLoadScvs(){
            int free = scvStorageLimit() - storedScvs - loadingScvs.size;
            if(free <= 0) return false;

            Seq<Unit> candidates = new Seq<>();
            for(Unit unit : team.data().units){
                if(unit == null || !unit.isValid()) continue;
                if(unit.type != UnitTypes.nova) continue;
                if(loadingScvs.contains(unit.id)) continue;
                candidates.add(unit);
            }

            candidates.sort(Structs.comparingFloat(u -> u.dst2(this)));

            int added = 0;
            for(int i = 0; i < candidates.size && added < free; i++){
                Unit unit = candidates.get(i);
                if(!unit.isCommandable()){
                    unit.controller(new CommandAI());
                }
                unit.command().commandPosition(Tmp.v1.set(x, y));
                loadingScvs.add(unit.id);
                added++;
            }

            return added > 0;
        }

        public boolean unloadScvs(){
            if(storedScvs <= 0) return false;
            int count = storedScvs;
            storedScvs = 0;
            for(int i = 0; i < count; i++){
                spawnUnit(UnitTypes.nova);
            }
            return true;
        }

        private void spawnUnit(UnitType type){
            if(type == null) return;
            Unit unit = type.create(team);
            Vec2 spawn = getSpawnPosition(unit);
            unit.set(spawn.x, spawn.y);
            unit.add();
            applyRally(unit);
            Events.fire(new UnitCreateEvent(unit, this));
        }

        private Vec2 getSpawnPosition(Unit unit){
            float offset = hitSize() / 2f + unit.hitSize / 2f + 2f;
            Vec2 dir = Tmp.v1.set(0f, -1f);
            if(commandPos != null){
                dir.set(commandPos).sub(x, y);
                if(dir.len2() < 0.001f){
                    dir.set(0f, -1f);
                }
            }
            dir.setLength(offset);
            return Tmp.v2.set(x + dir.x, y + dir.y);
        }

        private void applyRally(Unit unit){
            if(commandPos == null || unit == null) return;
            Tile tile = world.tileWorld(commandPos.x, commandPos.y);
            Tile resource = resolveResourceTile(tile);
            if(resource != null){
                setHarvestTarget(unit, resource.worldx(), resource.worldy());
                return;
            }

            Building build = world.buildWorld(commandPos.x, commandPos.y);
            if(build != null && build.block == Blocks.ventCondenser && build.team == team){
                Tile ventTile = findVentTile(build);
                if(ventTile != null){
                    setHarvestTarget(unit, ventTile.worldx(), ventTile.worldy());
                    return;
                }
            }

            if(unit.controller() instanceof CommandAI ai){
                if(build != null){
                    if(build.team == team){
                        ai.commandFollow(build);
                    }else if(unit.type.targetGround){
                        ai.commandTarget(build);
                    }else{
                        ai.commandPosition(commandPos);
                    }
                }else{
                    ai.commandPosition(commandPos);
                }
            }
        }

        private void setHarvestTarget(Unit unit, float worldx, float worldy){
            if(unit.controller() instanceof CommandAI ai){
                ai.setHarvestTarget(Tmp.v3.set(worldx, worldy));
            }else if(unit.controller() instanceof HarvestAI ai){
                ai.setHarvestTarget(Tmp.v3.set(worldx, worldy));
            }
        }

        public boolean canLift(){
            if(block == Blocks.corePlanetaryFortress) return false;
            return (unitQueue == null || unitQueue.isEmpty()) && !isUpgrading();
        }

        public @Nullable Unit lift(){
            if(!canLift()) return null;
            Unit unit = UnitTypes.coreFlyer.create(team);
            unit.set(x, y);
            unit.rotation(225f);
            unit.add();
            if(unit instanceof Payloadc payload){
                payload.pickup(this);
                UnitTypes.CoreFlyerData data = UnitTypes.getCoreFlyerData(unit);
                data.active = false;
                data.landing = false;
                data.landTime = 0f;
                data.returnRotation = payload.payloads().peek() instanceof mindustry.world.blocks.payloads.BuildPayload build ? build.build.rotation * 90f : unit.rotation();
            }
            return unit;
        }

        public boolean canStartOrbitalUpgrade(){
            if(block != Blocks.coreNucleus) return false;
            if(isUpgrading()) return false;
            if(unitQueue != null && !unitQueue.isEmpty()) return false;
            if(unitProgress > 0f) return false;
            if(state.rules.infiniteResources || team.rules().infiniteResources) return true;
            return items.has(Items.graphite, orbitalUpgradeCost);
        }

        public boolean startOrbitalUpgrade(){
            if(!canStartOrbitalUpgrade()) return false;
            if(!state.rules.infiniteResources && !team.rules().infiniteResources){
                items.remove(Items.graphite, orbitalUpgradeCost);
            }
            upgradingOrbital = true;
            orbitalUpgradeProgress = 0f;
            return true;
        }

        public boolean cancelOrbitalUpgrade(){
            if(!upgradingOrbital) return false;
            if(!state.rules.infiniteResources && !team.rules().infiniteResources){
                items.add(Items.graphite, orbitalUpgradeCost);
            }
            upgradingOrbital = false;
            orbitalUpgradeProgress = 0f;
            return true;
        }

        public boolean isUpgradingOrbital(){
            return upgradingOrbital;
        }

        public boolean canStartFortressUpgrade(){
            if(block != Blocks.coreNucleus) return false;
            if(isUpgrading()) return false;
            if(unitQueue != null && !unitQueue.isEmpty()) return false;
            if(unitProgress > 0f) return false;
            if(!hasEngineeringStation()) return false;
            if(state.rules.infiniteResources || team.rules().infiniteResources) return true;
            return items.has(Items.graphite, fortressUpgradeCost) && items.has(Items.highEnergyGas, fortressUpgradeGasCost);
        }

        public boolean startFortressUpgrade(){
            if(!canStartFortressUpgrade()) return false;
            if(!state.rules.infiniteResources && !team.rules().infiniteResources){
                items.remove(Items.graphite, fortressUpgradeCost);
                items.remove(Items.highEnergyGas, fortressUpgradeGasCost);
            }
            upgradingFortress = true;
            fortressUpgradeProgress = 0f;
            return true;
        }

        public boolean cancelFortressUpgrade(){
            if(!upgradingFortress) return false;
            if(!state.rules.infiniteResources && !team.rules().infiniteResources){
                items.add(Items.graphite, fortressUpgradeCost);
                items.add(Items.highEnergyGas, fortressUpgradeGasCost);
            }
            upgradingFortress = false;
            fortressUpgradeProgress = 0f;
            return true;
        }

        public boolean isUpgradingFortress(){
            return upgradingFortress;
        }

        public float fortressUpgradeFraction(){
            if(!upgradingFortress || fortressUpgradeTime <= 0f) return 0f;
            return Mathf.clamp(fortressUpgradeProgress / fortressUpgradeTime);
        }

        public boolean isUpgrading(){
            return upgradingOrbital || upgradingFortress;
        }

        public boolean hasEngineeringStation(){
            return team.data().buildings.contains(b -> b != null && b.isValid() && b.block == Blocks.multiPress);
        }

        public float orbitalUpgradeFraction(){
            if(!upgradingOrbital || orbitalUpgradeTime <= 0f) return 0f;
            return Mathf.clamp(orbitalUpgradeProgress / orbitalUpgradeTime);
        }

        private void updateFortressUpgrade(){
            if(!upgradingFortress) return;
            fortressUpgradeProgress += edelta() * state.rules.buildSpeed(team);
            if(fortressUpgradeProgress >= fortressUpgradeTime){
                upgradingFortress = false;
                fortressUpgradeProgress = 0f;
                finishFortressUpgrade();
            }
        }

        private void updateOrbitalUpgrade(){
            if(!upgradingOrbital) return;
            orbitalUpgradeProgress += edelta() * state.rules.buildSpeed(team);
            if(orbitalUpgradeProgress >= orbitalUpgradeTime){
                upgradingOrbital = false;
                orbitalUpgradeProgress = 0f;
                finishOrbitalUpgrade();
            }
        }

        private boolean finishFortressUpgrade(){
            if(block != Blocks.coreNucleus) return false;

            ItemModule items = this.items.copy();
            IntSeq queue = new IntSeq();
            if(unitQueue != null){
                queue.addAll(unitQueue);
            }
            Vec2 cmd = commandPos == null ? null : new Vec2(commandPos);
            int stored = storedScvs;
            IntSeq loading = new IntSeq();
            loading.addAll(loadingScvs);
            float progress = unitProgress;

            Tile tile = this.tile;
            int rot = rotation;
            tile.setBlock(Blocks.corePlanetaryFortress, team, rot);
            if(tile.build instanceof CoreBuild next){
                next.items.set(items);
                next.unitQueue.clear();
                next.unitQueue.addAll(queue);
                next.unitProgress = progress;
                next.commandPos = cmd;
                next.storedScvs = stored;
                next.loadingScvs.clear();
                next.loadingScvs.addAll(loading);
                next.orbitalEnergy = -1f;
            }
            return true;
        }

        private boolean finishOrbitalUpgrade(){
            if(block != Blocks.coreNucleus) return false;

            ItemModule items = this.items.copy();
            IntSeq queue = new IntSeq();
            if(unitQueue != null){
                queue.addAll(unitQueue);
            }
            Vec2 cmd = commandPos == null ? null : new Vec2(commandPos);
            int stored = storedScvs;
            IntSeq loading = new IntSeq();
            loading.addAll(loadingScvs);
            float progress = unitProgress;

            Tile tile = this.tile;
            int rot = rotation;
            tile.setBlock(Blocks.coreOrbital, team, rot);
            if(tile.build instanceof CoreBuild next){
                next.items.set(items);
                next.unitQueue.clear();
                next.unitQueue.addAll(queue);
                next.unitProgress = progress;
                next.commandPos = cmd;
                next.storedScvs = stored;
                next.loadingScvs.clear();
                next.loadingScvs.addAll(loading);
                next.orbitalEnergy = orbitalEnergyInit;
            }
            return true;
        }

        @Override
        public byte version(){
            return 5;
        }

        @Override
        public void write(Writes write){
            super.write(write);
            TypeIO.writeVecNullable(write, commandPos);
            write.f(unitProgress);
            write.s(unitQueue == null ? 0 : unitQueue.size);
            if(unitQueue != null){
                for(int i = 0; i < unitQueue.size; i++){
                    write.s(unitQueue.get(i));
                }
            }
            write.i(storedScvs);
            write.f(orbitalEnergy);
            write.bool(upgradingOrbital);
            write.f(orbitalUpgradeProgress);
            write.bool(upgradingFortress);
            write.f(fortressUpgradeProgress);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);

            if(revision >= 1){
                commandPos = TypeIO.readVecNullable(read);
                unitProgress = read.f();
                int count = read.s();
                if(unitQueue == null) unitQueue = new IntSeq();
                unitQueue.clear();
                for(int i = 0; i < count; i++){
                    unitQueue.add(read.s());
                }
                if(revision >= 2){
                    storedScvs = read.i();
                }else{
                    storedScvs = 0;
                }
                if(revision >= 3){
                    orbitalEnergy = read.f();
                }else{
                    orbitalEnergy = block == Blocks.coreOrbital ? orbitalEnergyInit : -1f;
                }
                if(revision >= 4){
                    upgradingOrbital = read.bool();
                    orbitalUpgradeProgress = read.f();
                }else{
                    upgradingOrbital = false;
                    orbitalUpgradeProgress = 0f;
                }
                if(revision >= 5){
                    upgradingFortress = read.bool();
                    fortressUpgradeProgress = read.f();
                }else{
                    upgradingFortress = false;
                    fortressUpgradeProgress = 0f;
                }
            }else{
                if(unitQueue == null) unitQueue = new IntSeq();
                unitQueue.clear();
                unitProgress = 0f;
                commandPos = null;
                storedScvs = 0;
                orbitalEnergy = block == Blocks.coreOrbital ? orbitalEnergyInit : -1f;
                upgradingOrbital = false;
                orbitalUpgradeProgress = 0f;
                upgradingFortress = false;
                fortressUpgradeProgress = 0f;
            }
            loadingScvs.clear();
            if(block != Blocks.coreNucleus){
                upgradingOrbital = false;
                orbitalUpgradeProgress = 0f;
                upgradingFortress = false;
                fortressUpgradeProgress = 0f;
            }
        }

        private void updateOrbitalEnergy(){
            if(block != Blocks.coreOrbital) return;
            if(orbitalEnergy < 0f) orbitalEnergy = orbitalEnergyInit;
            if(orbitalEnergy < orbitalEnergyCap){
                orbitalEnergy = Math.min(orbitalEnergy + orbitalEnergyRegen * Time.delta / 60f, orbitalEnergyCap);
            }
        }

        public boolean hasOrbitalEnergy(float amount){
            if(block != Blocks.coreOrbital) return true;
            if(orbitalEnergy < 0f) orbitalEnergy = orbitalEnergyInit;
            return orbitalEnergy >= amount;
        }

        public boolean consumeOrbitalEnergy(float amount){
            if(block != Blocks.coreOrbital) return true;
            if(orbitalEnergy < 0f) orbitalEnergy = orbitalEnergyInit;
            if(orbitalEnergy < amount) return false;
            orbitalEnergy -= amount;
            return true;
        }

        /** @return Camera zoom while landing or launching. May optionally do other things such as setting camera position to itself. */
        @Override
        public float zoomLaunch(){
            Core.camera.position.set(this);
            return landZoomInterp.apply(Scl.scl(landZoomFrom), Scl.scl(landZoomTo), renderer.getLandTimeIn());
        }

        @Override
        public void updateLaunch(){
            float in = renderer.getLandTimeIn() * launchDuration();
            float tsize = Mathf.sample(thrusterSizes, (in + 35f) / launchDuration());

            landParticleTimer += tsize * Time.delta;
            if(landParticleTimer >= 1f){
                tile.getLinkedTiles(t -> {
                    if(Mathf.chance(0.4f)){
                        Fx.coreLandDust.at(t.worldx(), t.worldy(), angleTo(t.worldx(), t.worldy()) + Mathf.range(30f), Tmp.c1.set(t.floor().mapColor).mul(1.5f + Mathf.range(0.15f)));
                    }
                });

                landParticleTimer = 0f;
            }
        }

        @Override
        public boolean canPickup(){
            //cores can never be picked up
            return false;
        }

        @Override
        public void onDestroyed(){
            if(state.rules.coreCapture){
                //just create an explosion, no fire. this prevents immediate recapture
                Damage.dynamicExplosion(x, y, 0, 0, 0, tilesize * block.size / 2f, state.rules.damageExplosions);
                Fx.commandSend.at(x, y, 140f);

                //make sure the sound still plays
                if(!headless){
                    playDestroySound();
                }
            }else{
                super.onDestroyed();
            }

            Effect.shockwaveDust(x, y, 40f + block.size * tilesize, 0.5f);
            Fx.coreExplosion.at(x, y, team.color);

            //add a spawn to the map for future reference - waves should be disabled, so it shouldn't matter
            if(state.isCampaign() && team == state.rules.waveTeam && team.cores().size <= 1 && spawner.getSpawns().size == 0 && state.rules.sector.planet.enemyCoreSpawnReplace){
                //do not recache
                tile.setOverlayQuiet(Blocks.spawn);

                if(!spawner.getSpawns().contains(tile)){
                    spawner.getSpawns().add(tile);
                }
            }

            Events.fire(new CoreChangeEvent(this));
        }

        @Override
        public void playDestroySound(){
            if(team.data().cores.size <= 1 && player != null && player.team() == team && state.rules.canGameOver){
                //play at full volume when doing a game over
                block.destroySound.play(block.destroySoundVolume, Mathf.random(block.destroyPitchMin, block.destroyPitchMax), 0f);
            }else{
                super.playDestroySound();
            }
        }

        @Override
        public void afterDestroyed(){
            super.afterDestroyed();
            if(state.rules.coreCapture){
                if(!net.client()){
                    tile.setBlock(block, lastDamage);

                    //core is invincible for several seconds to prevent recapture
                    ((CoreBuild)tile.build).iframes = captureInvicibility;

                    if(net.server()){
                        //delay so clients don't destroy it afterwards
                        Time.run(0f, () -> {
                            tile.setNet(block, lastDamage, 0);
                        });
                    }
                }
            }
        }

        @Override
        public void drawLight(){
            Drawf.light(x, y, lightRadius, Pal.accent, 0.65f + Mathf.absin(20f, 0.1f));
        }

        @Override
        public boolean acceptItem(Building source, Item item){
            return state.rules.coreIncinerates || items.get(item) < getMaximumAccepted(item);
        }

        @Override
        public int getMaximumAccepted(Item item){
            return state.rules.coreIncinerates ? Integer.MAX_VALUE/2 : storageCapacity;
        }

        @Override
        public void onProximityUpdate(){
            super.onProximityUpdate();

            for(Building other : state.teams.cores(team)){
                if(other.tile != tile){
                    this.items = other.items;
                }
            }
            state.teams.registerCore(this);

            storageCapacity = itemCapacity + proximity.sum(e -> owns(e) ? e.block.itemCapacity : 0);
            proximity.each(this::owns, t -> {
                t.items = items;
                ((StorageBuild)t).linkedCore = this;
            });

            for(Building other : state.teams.cores(team)){
                if(other.tile == tile) continue;
                storageCapacity += other.block.itemCapacity + other.proximity.sum(e -> owns(other, e) ? e.block.itemCapacity : 0);
            }

            if(!world.isGenerating()){
                for(Item item : content.items()){
                    items.set(item, Math.min(items.get(item), storageCapacity));
                }
            }

            for(CoreBuild other : state.teams.cores(team)){
                other.storageCapacity = storageCapacity;
            }
        }

        @Override
        public void handleStack(Item item, int amount, Teamc source){
            boolean incinerate = incinerateNonBuildable && !item.buildable;
            int realAmount = incinerate ? 0 : Math.min(amount, storageCapacity - items.get(item));
            super.handleStack(item, realAmount, source);

            if(team == state.rules.defaultTeam && state.isCampaign()){
                if(!incinerate){
                    state.rules.sector.info.handleCoreItem(item, amount);
                }

                if(realAmount == 0 && wasVisible){
                    Fx.coreBurn.at(x, y);
                }
            }
        }

        @Override
        public int removeStack(Item item, int amount){
            int result = super.removeStack(item, amount);

            if(team == state.rules.defaultTeam && state.isCampaign()){
                state.rules.sector.info.handleCoreItem(item, -result);
            }

            return result;
        }

        @Override
        public void drawSelect(){
            //do not draw a pointless single outline when there's no storage
            if(team.cores().size <= 1 && !proximity.contains(storage -> storage.items == items)) return;

            Lines.stroke(1f, Pal.accent);
            Cons<Building> outline = b -> {
                for(int i = 0; i < 4; i++){
                    Point2 p = Geometry.d8edge[i];
                    float offset = -Math.max(b.block.size - 1, 0) / 2f * tilesize;
                    Draw.rect("block-select", b.x + offset * p.x, b.y + offset * p.y, i * 90);
                }
            };
            team.cores().each(core -> {
                outline.get(core);
                core.proximity.each(storage -> storage.items == items, outline);
            });
            Draw.reset();
        }

        public boolean owns(Building tile){
            return owns(this, tile);
        }

        public boolean owns(Building core, Building tile){
            return tile instanceof StorageBuild b && ((StorageBlock)b.block).coreMerge && (b.linkedCore == core || b.linkedCore == null);
        }

        @Override
        public void damage(float amount){
            if(player != null && team == player.team()){
                Events.fire(Trigger.teamCoreDamage);
            }
            super.damage(amount);
        }

        @Override
        public void onRemoved(){
            int totalCapacity = proximity.sum(e -> e.items != null && e.items == items ? e.block.itemCapacity : 0);

            proximity.each(e -> owns(e) && e.items == items && owns(e), t -> {
                StorageBuild ent = (StorageBuild)t;
                ent.linkedCore = null;
                ent.items = new ItemModule();
                for(Item item : content.items()){
                    ent.items.set(item, (int)Math.min(ent.block.itemCapacity, items.get(item) * (float)ent.block.itemCapacity / totalCapacity));
                }
            });

            state.teams.unregisterCore(this);

            for(CoreBuild other : state.teams.cores(team)){
                other.onProximityUpdate();
            }
        }

        @Override
        public void placed(){
            super.placed();
            state.teams.registerCore(this);
        }

        @Override
        public void itemTaken(Item item){
            if(state.isCampaign() && team == state.rules.defaultTeam){
                //update item taken amount
                state.rules.sector.info.handleCoreItem(item, -1);
            }
        }

        @Override
        public void handleItem(Building source, Item item){
            boolean incinerate = incinerateNonBuildable && !item.buildable;

            if(team == state.rules.defaultTeam){
                state.stats.coreItemCount.increment(item);
            }

            if(net.server() || !net.active()){
                if(team == state.rules.defaultTeam && state.isCampaign() && !incinerate){
                    state.rules.sector.info.handleCoreItem(item, 1);
                }

                if(items.get(item) >= storageCapacity || incinerate){
                    //create item incineration effect at random intervals
                    if(!noEffect){
                        incinerateEffect(this, source);
                    }
                    noEffect = false;
                }else{
                    super.handleItem(source, item);
                }
            }else if(((state.rules.coreIncinerates && items.get(item) >= storageCapacity) || incinerate) && !noEffect){
                //create item incineration effect at random intervals
                incinerateEffect(this, source);
                noEffect = false;
            }
        }
    }

    static @Nullable Tile resolveResourceTile(Tile tile){
        if(tile == null) return null;
        if(tile.block() instanceof CrystalMineralWall) return tile;
        if(tile.floor() instanceof SteamVent vent){
            Tile dataTile = vent.dataTile(tile);
            if(dataTile == null || !vent.checkAdjacent(dataTile)) return null;
            Tile center = dataTile.nearby(-1, -1);
            if(center != null && center.floor() == vent) return center;
            return dataTile;
        }
        return null;
    }

    public static boolean inResourceExclusion(float worldX, float worldY){
        float radius = resourceExclusionRadiusTiles * tilesize;
        float radius2 = radius * radius;
        float half = tilesize / 2f;
        int cx = World.toTile(worldX);
        int cy = World.toTile(worldY);
        int range = resourceExclusionRadiusTiles + 4;

        for(int dx = -range; dx <= range; dx++){
            for(int dy = -range; dy <= range; dy++){
                Tile other = world.tile(cx + dx, cy + dy);
                Tile resource = resolveResourceTile(other);
                if(resource == null) continue;
                if(Mathf.dst2(worldX, worldY, resource.worldx() + half, resource.worldy() + half) <= radius2){
                    return true;
                }
            }
        }

        return false;
    }

    static @Nullable Tile findVentTile(Building build){
        if(build == null || build.tile == null) return null;
        int size = build.block.size;
        int bx = build.tile.x;
        int by = build.tile.y;

        for(int x = 0; x < size; x++){
            for(int y = 0; y < size; y++){
                Tile tile = world.tile(bx + x, by + y);
                if(tile == null || !(tile.floor() instanceof SteamVent)) continue;
                SteamVent vent = (SteamVent)tile.floor();
                Tile data = vent.dataTile(tile);
                if(data != null && vent.checkAdjacent(data)){
                    Tile center = data.nearby(-1, -1);
                    if(center != null && center.floor() == vent) return center;
                    return data;
                }
            }
        }
        return null;
    }
}
