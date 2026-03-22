package mindustry.core;

import arc.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.ai.*;
import mindustry.ai.types.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.core.GameState.*;
import mindustry.ctype.*;
import mindustry.entities.*;
import mindustry.game.EventType.*;
import mindustry.game.*;
import mindustry.game.Teams.*;
import mindustry.gen.*;
import mindustry.maps.*;
import mindustry.type.*;
import mindustry.type.Weather.*;
import mindustry.world.*;
import mindustry.world.blocks.environment.*;
import mindustry.world.blocks.storage.*;
import mindustry.world.blocks.storage.CoreBlock.*;

import java.util.*;

import static mindustry.Vars.*;

/**
 * Logic module.
 * Handles all logic for entities and waves.
 * Handles game state events.
 * Does not store any game state itself.
 * <p>
 * This class should <i>not</i> call any outside methods to change state of modules, but instead fire events.
 */
public class Logic implements ApplicationListener{

    private static final String pvpScvStartTag = "pvp-scv-started";
    private static final int pvpInitialScvs = 0;
    private static final int pvpRingScvs = 12;
    private static final int pvpHarvestSearchRadiusTiles = 40;

    public Logic(){

        Events.on(BlockDestroyEvent.class, event -> {
            //skip if rule is off
            if(!state.rules.ghostBlocks) return;

            //blocks that get broken are appended to the team's broken block queue
            Tile tile = event.tile;
            //skip null entities or un-rebuildables, for obvious reasons
            if(tile.build == null || !tile.block().rebuildable) return;

            tile.build.addPlan(true);
        });

        Events.on(BlockBuildEndEvent.class, event -> {
            if(!event.breaking){
                checkOverlappingPlans(event.team, event.tile);

                if(event.team == state.rules.defaultTeam){
                    state.stats.placedBlockCount.increment(event.tile.block());
                }
            }
        });

        Events.on(PayloadDropEvent.class, e -> {
            if(e.build != null){
                checkOverlappingPlans(e.build.team, e.build.tile);
            }
        });

        //when loading a 'damaged' sector, propagate the damage
        Events.on(SaveLoadEvent.class, e -> {
            if(state.isCampaign()){
                state.rules.coreIncinerates = true;
                state.rules.canGameOver = true;
                state.rules.allowEditRules = false;

                //fresh map has no sector info
                if(!e.isMap){
                    SectorInfo info = state.rules.sector.info;
                    info.write();

                    state.getSector().planet.applyRules(state.rules);

                    info.hasCore = true;

                    state.rules.sector.saveInfo();
                }
            }
        });

        Events.on(PlayEvent.class, e -> {
            //reset weather on play
            var randomWeather = state.rules.weather.copy().shuffle();
            float sum = 0f;
            for(var weather : randomWeather){
                weather.cooldown = sum + Mathf.random(weather.maxFrequency);
                sum += weather.cooldown;
            }
            //tick resets on new save play
            state.tick = 0f;
        });

        Events.on(WorldLoadEvent.class, e -> {
            //enable infinite ammo for wave team by default
            state.rules.waveTeam.rules().infiniteAmmo = true;

            if(state.isCampaign()){
                //enable building AI on campaign unless the preset disables it

                state.rules.coreIncinerates = true;
                state.rules.infiniteResources = false;
                state.rules.allowEditRules = false;
                state.rules.allowEditWorldProcessors = false;
                if(state.getPlanet().enemyInfiniteItems){
                    state.rules.waveTeam.rules().infiniteResources = true;
                    state.rules.waveTeam.rules().fillItems = true;
                }
                state.rules.waveTeam.rules().buildSpeedMultiplier *= state.getPlanet().enemyBuildSpeedMultiplier;
            }

            //save settings
            Core.settings.manualSave();
        });

        Events.on(WorldLoadEndEvent.class, e -> {
            if(net.client()) return;
            if(state.isEditor()) return;

            CoreBlock.resetCoreCommandPositionsToNearestCrystal();

            if(!state.rules.pvp) return;

            //prevent duplication for reloaded saves
            if(state.rules.tags.containsKey(pvpScvStartTag)) return;
            state.rules.tags.put(pvpScvStartTag, "true");

            spawnPvpStartScvs();
        });

        //sync research
        Events.on(UnlockEvent.class, e -> {
            if(net.server()){
                Call.researched(e.content);
            }
        });

        Events.on(SectorCaptureEvent.class, e -> {
            if(!net.client() && e.sector == state.getSector() && e.sector.isBeingPlayed()){
                state.rules.waveTeam.data().destroyToDerelict();
            }

            if(!net.client() && e.sector.planet.generator != null){
                e.sector.planet.generator.onSectorCaptured(e.sector);
            }
        });

        Events.on(SectorLoseEvent.class, e -> {
            if(!net.client() && e.sector.planet.generator != null){
                e.sector.planet.generator.onSectorLost(e.sector);
            }
        });

        Events.on(BlockDestroyEvent.class, e -> {
            if(e.tile.build instanceof CoreBuild core && core.team.isAI() && state.rules.coreDestroyClear){
                Core.app.post(() -> core.team.data().timeDestroy(core.x, core.y, state.rules.enemyCoreBuildRadius));
            }
        });

        //listen to core changes; if all cores have been destroyed, set to derelict.
        Events.on(CoreChangeEvent.class, e -> Core.app.post(() -> {
            if(state.rules.cleanupDeadTeams && state.rules.pvp && !e.core.isAdded() && e.core.team != Team.derelict && e.core.team.cores().isEmpty()){
                e.core.team.data().destroyToDerelict();
            }
        }));

        Events.on(BlockBuildEndEvent.class, e -> {
            if(e.team == state.rules.defaultTeam){
                if(e.breaking){
                    state.stats.buildingsDeconstructed++;
                }else{
                    state.stats.buildingsBuilt++;
                }
            }
        });

        Events.on(BlockDestroyEvent.class, e -> {
            if(e.tile.team() == state.rules.defaultTeam){
                state.stats.buildingsDestroyed ++;
            }
        });

        Events.on(BlockDestroyEvent.class, e -> {
            if(e.tile.team() != state.rules.defaultTeam){
                state.stats.destroyedBlockCount.increment(e.tile.block());
            }
        });

        Events.on(UnitDestroyEvent.class, e -> {
            if(e.unit.team() != state.rules.defaultTeam){
                state.stats.enemyUnitsDestroyed ++;
            }
        });

        Events.on(UnitCreateEvent.class, e -> {
            if(e.unit.team == state.rules.defaultTeam){
                state.stats.unitsCreated++;
            }
        });
    }

    private void checkOverlappingPlans(Team team, Tile tile){
        TeamData data = team.data();
        Iterator<BlockPlan> it = data.plans.iterator();
        var bounds = tile.block().bounds(tile.x, tile.y, Tmp.r1);
        while(it.hasNext()){
            BlockPlan b = it.next();
            if(bounds.overlaps(b.block.bounds(b.x, b.y, Tmp.r2))){
                b.removed = true;
                it.remove();
            }
        }
    }

    /** Adds starting items, resets wave time, and sets state to playing. */
    public void play(){
        state.set(State.playing);
        //grace period of 2x wave time before game starts
        state.wavetime = (state.rules.initialWaveSpacing <= 0 ? state.rules.waveSpacing * 2 : state.rules.initialWaveSpacing) * (state.isCampaign() ? state.getPlanet().campaignRules.difficulty.waveTimeMultiplier : 1f);
        state.stats = new GameStats();
        Events.fire(new PlayEvent());

        //add starting items
        if(!state.isCampaign() || !state.rules.sector.planet.allowLaunchLoadout || (state.rules.sector.preset != null && state.rules.sector.preset.addStartingItems)){
            for(TeamData team : state.teams.getActive()){
                if(team.hasCore()){
                    CoreBuild entity = team.core();
                    entity.items.clear();

                    for(ItemStack stack : state.rules.loadout){
                        //make sure to cap storage
                        entity.items.add(stack.item, Math.min(stack.amount, entity.storageCapacity - entity.items.get(stack.item)));
                    }
                }
            }
        }

        //heal all cores on game start
        for(TeamData team : state.teams.getActive()){
            for(var entity : team.cores){
                entity.heal();
            }
        }
    }

    public void reset(){
        State prev = state.getState();
        state.patcher.unapply();
        //recreate gamestate - sets state to menu
        state = new GameState();
        //fire change event, since it was technically changed
        Events.fire(new StateChangeEvent(prev, State.menu));

        Groups.clear();
        Time.clear();
        Events.fire(new ResetEvent());
        world.tiles = new Tiles(0, 0);

        //save settings on reset
        Core.settings.manualSave();
    }

    public void skipWave(){
        runWave();
    }

    public void runWave(){
        spawner.spawnEnemies();
        state.wave++;
        state.wavetime = state.rules.waveSpacing * (state.isCampaign() ? state.getPlanet().campaignRules.difficulty.waveTimeMultiplier : 1f);

        Events.fire(new WaveEvent());
    }

    private void checkGameState(){
        boolean playerHasBuildings = state.rules.defaultTeam.data().buildings.size > 0;
        //campaign maps do not have a 'win' state!
        if(state.isCampaign()){
            //gameover only when cores are dead
            if(state.teams.playerCores().size == 0 && !playerHasBuildings && !state.gameOver){
                state.gameOver = true;
                Events.fire(new GameOverEvent(state.rules.waveTeam));
            }

            //check if there are no enemy spawns
            if(state.rules.waves && spawner.countSpawns() + state.teams.cores(state.rules.waveTeam).size <= 0){
                //if yes, waves get disabled
                state.rules.waves = false;
            }

            //if there's a "win" wave and no enemies are present, win automatically
            if(state.rules.waves && (state.enemies == 0 && state.rules.winWave > 0 && state.wave >= state.rules.winWave && !spawner.isSpawning()) ||
                (state.rules.attackMode && !state.rules.waveTeam.isAlive())){

                if(state.rules.sector.preset != null && state.rules.sector.preset.attackAfterWaves && !state.rules.attackMode){
                    //activate attack mode to destroy cores after waves are done.
                    state.rules.attackMode = true;
                    state.rules.waves = false;
                    Call.setRules(state.rules);
                }else{
                    Call.sectorCapture();
                }
            }
        }else{
            if(!state.rules.attackMode && state.teams.playerCores().size == 0 && !playerHasBuildings && !state.gameOver){
                state.gameOver = true;
                Events.fire(new GameOverEvent(state.rules.waveTeam));
            }else if(state.rules.attackMode){
                //count # of teams alive
                int countAlive = state.teams.getActive().count(t -> t.isAlive() && t.team != Team.derelict);

                if((countAlive <= 1 || (!state.rules.pvp && state.rules.defaultTeam.core() == null)) && !state.gameOver){
                    //find team that won
                    TeamData left = state.teams.getActive().find(t -> t.isAlive() && t.team != Team.derelict);
                    Events.fire(new GameOverEvent(left == null ? Team.derelict : left.team));
                    state.gameOver = true;
                }
            }else if(!state.gameOver && state.rules.waves && (state.enemies == 0 && state.rules.winWave > 0 && state.wave >= state.rules.winWave && !spawner.isSpawning())){
                state.gameOver = true;
                Events.fire(new GameOverEvent(state.rules.defaultTeam));
            }
        }
    }

    protected void updateWeather(){
        state.rules.weather.removeAll(w -> w.weather == null);

        for(WeatherEntry entry : state.rules.weather){
            //update cooldown
            entry.cooldown -= Time.delta;

            //create new event when not active
            if((entry.cooldown < 0 || entry.always) && !entry.weather.isActive()){
                float duration = entry.always ? Float.POSITIVE_INFINITY : Mathf.random(entry.minDuration, entry.maxDuration);
                entry.cooldown = duration + Mathf.random(entry.minFrequency, entry.maxFrequency);
                Tmp.v1.setToRandomDirection();
                Call.createWeather(entry.weather, entry.intensity, duration, Tmp.v1.x, Tmp.v1.y);
            }
        }
    }

    @Remote(called = Loc.server)
    public static void sectorCapture(){
        //the sector has been conquered - waves get disabled
        state.rules.waves = false;

        if(state.rules.sector == null){
            //disable attack mode
            state.rules.attackMode = false;
            return;
        }

        boolean initial = !state.rules.sector.info.wasCaptured;

        state.rules.sector.info.wasCaptured = true;

        //fire capture event
        Events.fire(new SectorCaptureEvent(state.rules.sector, initial));

        //disable attack mode
        state.rules.attackMode = false;

        //map is over, no more world processor objective stuff
        state.rules.disableWorldProcessors = true;

        Call.clearObjectives();

        //save, just in case
        if(!headless && !net.client()){
            control.saves.saveSector(state.rules.sector);
        }
    }

    @Remote(called = Loc.both)
    public static void updateGameOver(Team winner){
        state.gameOver = true;
        if(!headless){
            state.won = player.team() == winner;
        }
    }

    @Remote(called = Loc.both)
    public static void gameOver(Team winner){
        state.stats.wavesLasted = state.wave;
        state.won = player.team() == winner;
        Time.run(60f * 3f, () -> ui.restart.show(winner));
        netClient.setQuiet();
    }

    //called when the remote server researches something
    @Remote
    public static void researched(Content content){
        if(!(content instanceof UnlockableContent u)) return;

        boolean was = u.unlockedNowHost();
        state.rules.researched.add(u);

        if(!was){
            Events.fire(new UnlockEvent(u));
        }
    }

    @Override
    public void dispose(){
        //save the settings before quitting
        if(netServer != null){
            netServer.admins.forceSave();
        }
        Core.settings.manualSave();
    }

    @Override
    public void update(){
        PerfCounter.frame.end();
        PerfCounter.frame.begin();

        Events.fire(Trigger.update);
        universe.updateGlobal();

        if(Core.settings.modified() && !state.isPlaying()){
            netServer.admins.forceSave();
            Core.settings.forceSave();
        }

        boolean runStateCheck = !net.client() && !world.isInvalidMap() && !state.isEditor() && state.rules.canGameOver;

        if(state.isGame()){
            if(!net.client()){
                state.enemies = Groups.unit.count(u -> u.team() == state.rules.waveTeam && u.isEnemy());
            }

            if(!state.isPaused()){
                Events.fire(Trigger.beforeGameUpdate);

                float delta = Core.graphics.getDeltaTime();
                state.tick += Float.isNaN(delta) || Float.isInfinite(delta) ? 0f : delta * 60f;
                state.updateId ++;
                state.teams.updateTeamStats();
                MapPreviewLoader.checkPreviews();

                if(state.rules.fog){
                    fogControl.update();
                }

                if(state.isCampaign()){
                    state.rules.sector.info.update();
                }

                if(state.isCampaign()){
                    universe.update();
                }
                Time.update();

                logicVars.update();

                //weather is serverside
                if(!net.client() && !state.isEditor()){
                    updateWeather();

                    for(TeamData data : state.teams.getActive()){
                        var rules = data.team.rules();
                        if(rules.fillItems && data.cores.size > 0){
                            var core = data.cores.first();
                            content.items().each(i -> {
                                if(i.isOnPlanet(Vars.state.getPlanet()) && !i.isHidden()){
                                    core.items.set(i, core.getMaximumAccepted(i));
                                }
                            });
                        }
                        //does not work on PvP so built-in attack maps can have it on by default without issues
                        if(rules.buildAi && !state.rules.pvp){
                            if(data.buildAi == null) data.buildAi = new BaseBuilderAI(data);
                            data.buildAi.update();
                        }

                        if(rules.rtsAi){
                            if(data.rtsAi == null) data.rtsAi = new RtsAI(data);
                            data.rtsAi.update();
                        }

                        //spawn units for prebuild AI cores
                        if(rules.prebuildAi && !state.isEditor()){
                            for(var core : data.cores){
                                var units = data.getUnits(((CoreBlock)core.block).unitType);
                                if(units == null || !units.contains(u -> u.flag == core.pos())){
                                    Unit unit = ((CoreBlock)core.block).unitType.spawn(core, data.team);
                                    unit.flag = core.pos();
                                    unit.add();
                                    Units.notifyUnitSpawn(unit);
                                    Fx.spawn.at(unit);
                                }
                            }
                        }
                    }
                }

                if(!state.isEditor()){
                    state.rules.objectives.update();
                }

                if(state.rules.waves && state.rules.waveTimer && !state.gameOver){
                    if(!isWaitingWave()){
                        state.wavetime = Math.max(state.wavetime - Time.delta, 0);
                    }
                }

                if(!net.client() && state.wavetime <= 0 && state.rules.waves){
                    runWave();
                }

                //apply weather attributes
                state.envAttrs.clear();
                state.envAttrs.add(state.rules.attributes);
                Groups.weather.each(w -> state.envAttrs.add(w.weather.attrs, w.opacity));

                PerfCounter.entityUpdate.begin();
                Groups.update();
                PerfCounter.entityUpdate.end();

                Events.fire(Trigger.afterGameUpdate);
            }

            if(runStateCheck){
                checkGameState();
            }
        }else if(netServer.isWaitingForPlayers() && runStateCheck){
            checkGameState();
        }
    }

    private void spawnPvpStartScvs(){
        float centerX = world.width() * tilesize / 2f;
        float centerY = world.height() * tilesize / 2f;

        for(Team team : Team.all){
            CoreBuild core = team.core();
            if(core == null || !core.isValid()) continue;

            Vec2 fallbackDir = new Vec2(core.x - centerX, core.y - centerY);
            if(fallbackDir.isZero(0.001f)) fallbackDir.set(0f, 1f);
            fallbackDir.nor();

            Vec2 harvestDir = new Vec2();
            Tile harvestTarget = scanHarvestTarget(core, fallbackDir, harvestDir);

            if(pvpInitialScvs > 0){
                spawnScvsAroundCore(core, pvpInitialScvs, harvestTarget);
            }
            spawnScvsOnCoreRing(core, pvpRingScvs, harvestDir, harvestTarget);
        }
    }

    private void spawnScvsAroundCore(CoreBuild core, int amount, @Nullable Tile harvestTarget){
        if(amount <= 0) return;
        UnitType type = UnitTypes.nova;
        float offset = core.hitSize() / 2f + type.hitSize / 2f + 6f;

        for(int i = 0; i < amount; i++){
            float angle = i * 360f / amount;
            float x = core.x + Angles.trnsx(angle, offset);
            float y = core.y + Angles.trnsy(angle, offset);
            Unit unit = type.create(core.team);
            unit.set(x, y);
            unit.add();
            applyHarvestCommand(unit, harvestTarget);
        }
    }

    private void spawnScvsOnCoreRing(CoreBuild core, int amount, Vec2 preferDir, @Nullable Tile harvestTarget){
        if(amount <= 0) return;

        Seq<Tile> ring = coreRingTiles(core);
        if(ring.isEmpty()) return;

        float half = tilesize / 2f;
        Vec2 dir = Tmp.v1.set(preferDir);
        if(dir.isZero(0.001f)){
            dir.set(0f, 1f);
        }else{
            dir.nor();
        }

        ring.sort(Structs.comparingFloat(t -> {
            float dx = (t.worldx() + half) - core.x;
            float dy = (t.worldy() + half) - core.y;
            float len = Mathf.sqrt(dx * dx + dy * dy);
            if(len < 0.001f) return 0f;
            dx /= len;
            dy /= len;
            return -(dx * dir.x + dy * dir.y);
        }));

        ObjectSet<Tile> used = new ObjectSet<>();
        int spawned = 0;
        for(int i = 0; i < ring.size && spawned < amount; i++){
            Tile tile = ring.get(i);
            if(tile == null || used.contains(tile)) continue;
            if(tile.build != null) continue;
            if(tile.solid()) continue;

            Unit unit = UnitTypes.nova.create(core.team);
            unit.set(tile.worldx() + half, tile.worldy() + half);
            unit.add();
            applyHarvestCommand(unit, harvestTarget);
            used.add(tile);
            spawned++;
        }
    }

    private void applyHarvestCommand(Unit unit, @Nullable Tile harvestTarget){
        if(unit == null || !unit.isValid()) return;
        if(harvestTarget == null) return;

        if(!(unit.controller() instanceof CommandAI)){
            unit.controller(new CommandAI());
        }

        if(unit.controller() instanceof CommandAI ai){
            ai.setHarvestTarget(Tmp.v2.set(harvestTarget.worldx(), harvestTarget.worldy()));
        }
    }

    private Seq<Tile> coreRingTiles(CoreBuild core){
        Seq<Tile> out = new Seq<>();
        if(core == null || core.tile == null) return out;

        int bx = core.tile.x;
        int by = core.tile.y;
        int size = core.block.size;

        for(int x = bx - 1; x <= bx + size; x++){
            for(int y = by - 1; y <= by + size; y++){
                if(x == bx - 1 || x == bx + size || y == by - 1 || y == by + size){
                    Tile tile = world.tile(x, y);
                    if(tile != null) out.add(tile);
                }
            }
        }

        return out;
    }

    private @Nullable Tile scanHarvestTarget(CoreBuild core, Vec2 fallbackDir, Vec2 outDir){
        outDir.set(fallbackDir);

        if(core == null || core.tile == null){
            outDir.nor();
            return null;
        }

        int cx = core.tile.x + core.block.size / 2;
        int cy = core.tile.y + core.block.size / 2;

        ObjectSet<Tile> resources = new ObjectSet<>();
        int radius = Math.min(pvpHarvestSearchRadiusTiles, Math.min(world.width(), world.height()) / 2);

        for(int dx = -radius; dx <= radius; dx++){
            for(int dy = -radius; dy <= radius; dy++){
                Tile tile = world.tile(cx + dx, cy + dy);
                if(tile == null) continue;

                Tile resolved = resolveHarvestTile(tile);
                if(resolved == null) continue;
                if(!isValidHarvestTarget(resolved)) continue;
                resources.add(resolved);
            }
        }

        if(resources.isEmpty()){
            outDir.nor();
            return null;
        }

        float half = tilesize / 2f;

        Vec2 sum = Tmp.v1.setZero();
        for(Tile tile : resources){
            float dx = (tile.worldx() + half) - core.x;
            float dy = (tile.worldy() + half) - core.y;
            float dst = Mathf.sqrt(dx * dx + dy * dy);
            float w = 1f / (dst / tilesize + 1f);
            sum.add(dx * w, dy * w);
        }

        if(sum.isZero(0.001f)){
            outDir.set(fallbackDir).nor();
        }else{
            outDir.set(sum).nor();
        }

        //pick a target near the "middle" of the preferred direction
        float sumDist = 0f;
        int distCount = 0;
        for(Tile tile : resources){
            float dx = (tile.worldx() + half) - core.x;
            float dy = (tile.worldy() + half) - core.y;
            float dst = Mathf.sqrt(dx * dx + dy * dy);
            if(dst < 0.001f) continue;
            float align = (dx * outDir.x + dy * outDir.y) / dst;
            if(align > 0.5f){
                sumDist += dst;
                distCount++;
            }
        }

        float avgDist = distCount <= 0 ? 0f : sumDist / distCount;

        Tile best = null;
        float bestScore = -999999f;
        for(Tile tile : resources){
            float dx = (tile.worldx() + half) - core.x;
            float dy = (tile.worldy() + half) - core.y;
            float dst = Mathf.sqrt(dx * dx + dy * dy);
            if(dst < 0.001f) continue;

            float align = (dx * outDir.x + dy * outDir.y) / dst;
            float middle = avgDist <= 0f ? 0f : -Math.abs(dst - avgDist) / (avgDist + 1f);
            float score = align * 3f + middle;
            if(score > bestScore){
                bestScore = score;
                best = tile;
            }
        }

        if(best != null) return best;

        //fallback: nearest valid resource
        float bestDst = Float.MAX_VALUE;
        for(Tile tile : resources){
            float dst = Mathf.dst(tile.worldx() + half, tile.worldy() + half, core.x, core.y);
            if(dst < bestDst){
                bestDst = dst;
                best = tile;
            }
        }

        return best;
    }

    private static boolean isValidHarvestTarget(Tile tile){
        if(tile == null) return false;
        if(tile.block() instanceof CrystalMineralWall) return true;
        if(tile.floor() instanceof SteamVent vent){
            Tile data = vent.dataTile(tile);
            if(data == null || !vent.checkAdjacent(data)) return false;
            return vent.isInfinite(data) || vent.getReserves(data) > 0;
        }
        return false;
    }

    private static @Nullable Tile resolveHarvestTile(@Nullable Tile tile){
        if(tile == null) return null;
        if(tile.block() instanceof CrystalMineralWall) return tile;
        if(tile.floor() instanceof SteamVent vent){
            Tile data = vent.dataTile(tile);
            if(data == null || !vent.checkAdjacent(data)) return null;
            Tile center = data.nearby(-1, -1);
            if(center != null && center.floor() == vent) return center;
            return data;
        }
        return null;
    }

    /** @return whether the wave timer is paused due to enemies */
    public boolean isWaitingWave(){
        return (state.rules.waitEnemies || (state.wave >= state.rules.winWave && state.rules.winWave > 0)) && state.enemies > 0;
    }
}
