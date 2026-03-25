package mindustry.graphics;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.input.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.pooling.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.ai.types.*;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.game.EventType.*;
import mindustry.game.*;
import mindustry.game.Teams.*;
import mindustry.gen.*;
import mindustry.input.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.ConstructBlock.*;
import mindustry.world.blocks.defense.BunkerBlock;
import mindustry.world.blocks.defense.Radar;
import mindustry.world.blocks.payloads.*;
import mindustry.world.blocks.storage.CoreBlock;
import mindustry.world.blocks.storage.CoreBlock.*;
import mindustry.world.blocks.units.UnitFactory.*;

import static mindustry.Vars.*;

public class OverlayRenderer{
    private static final float radarIntelRange = 23f * tilesize;
    private static final float radarDotRadius = 1.9f;
    private static final float radarCircleStroke = 1.15f;
    private static final float radarEnemyDashStroke = 1.45f;
    private static final float radarEnemyDashArc = 10f;
    private static final float radarEnemyDashStep = 20f;
    private static final int radarEnemyDashSegments = 18;
    private static final float overheadBarHeight = 3.5f * 2f / 3f / 2f;
    private static final float overheadBarSpacing = overheadBarHeight + 1.05f * 2f / 3f;
    private static final float overheadBarBaseOffset = 3.25f;
    private static final float overheadBarStackShift = overheadBarSpacing * 2f;
    private static final float overheadBarCullMargin = 24f;
    private static final float overheadBorderStroke = 0.17f;
    private static final float overheadInnerInset = 0.12f;
    private static final float overheadSegmentIdeal = 3.2f / 2f;
    private static final int overheadSegmentMax = 24;
    private static final float overheadGroundBarLayer = Layer.flyingUnitLow - 0.25f;
    private static final float overheadAirBarLayer = Layer.light - 0.25f;
    private static final float overheadBorderLayerOffset = 0.02f;
    private static final Color overheadHealthLow = Color.valueOf("6f1616");
    private static final Color overheadHealthHigh = Color.valueOf("38d667");
    private static final Color overheadEnemyHealthColor = Color.valueOf("ff0000");
    private static final Color overheadEnergyColor = Color.valueOf("b57aff");
    private static final Color overheadProgressColor = Color.valueOf("66e7ff");
    private static final Color overheadRemainingColor = Color.valueOf("d8d8d8");
    private static final float hoverOwnerFontScale = 0.25f;
    private static final float hoverOwnerPadX = 3f;
    private static final float hoverOwnerPadY = 2f;
    private static final float hoverOwnerGap = 2.5f;
    private static final float hoverOwnerBorderStroke = 0.45f;

    private static final float indicatorLength = 14f;
    private static final float spawnerMargin = tilesize*11f;
    private static final Rect rect = new Rect();

    private float buildFade, unitFade;
    private Sized lastSelect;
    private Seq<CoreEdge> cedges = new Seq<>();
    private boolean updatedCores;
    private Object hoverPulseTarget;
    private float hoverPulseStart;
    private float hoverPulseUntil;
    private float hoverPulseX, hoverPulseY, hoverPulseRadius;
    private final Color hoverPulseColor = new Color();

    private void drawPlans(InputHandler input, Queue<BuildPlan> plans, Team team){
        float alpha = 0.7f;
        for(int i = 0; i < 2; i++){
            for(BuildPlan plan : plans){
                if(i == 0){
                    if(plan.breaking){
                        input.drawBreaking(plan);
                    }else{
                        plan.block.drawPlan(plan, input.allPlans(),
                        Build.validPlace(plan.block, team, plan.x, plan.y, plan.rotation) || input.planMatches(plan),
                        alpha);
                    }
                }else if(!plan.breaking){
                    Draw.reset();
                    Draw.mixcol(Color.white, 0.24f + Mathf.absin(Time.globalTime, 6f, 0.28f));
                    Draw.alpha(alpha);
                    plan.block.drawPlanConfigTop(plan, plans);
                }
            }
        }

        Draw.reset();
    }

    public OverlayRenderer(){
        Events.on(WorldLoadEvent.class, e -> {
            updatedCores = true;
        });

        Events.on(CoreChangeEvent.class, e -> {
            updatedCores = true;
        });
    }

    private void updateCoreEdges(){
        if(!updatedCores){
            return;
        }

        updatedCores = false;
        cedges.clear();

        Seq<Vec2> pos = new Seq<>();
        Seq<CoreBuild> teams = new Seq<>();
        for(TeamData data : state.teams.active){
            if(!data.team.rules().protectCores){
                continue;
            }

            for(CoreBuild b : data.cores){
                teams.add(b);
                pos.add(new Vec2(b.x, b.y));
            }
        }

        if(pos.isEmpty()){
            return;
        }

        //if this is laggy, it could be shoved in another thread.
        var result = Voronoi.generate(pos.toArray(Vec2.class), 0, world.unitWidth(), 0, world.unitHeight());
        for(var edge : result){
            cedges.add(new CoreEdge(edge.x1, edge.y1, edge.x2, edge.y2, teams.get(edge.site1).team, teams.get(edge.site2).team));
        }
    }

    public void drawBottom(){
        InputHandler input = control.input;

        if(input != null){
            for(Unit unit : Groups.unit){
                if(unit != null && unit.isValid() && unit.team == player.team() && unit.plans.size > 0){
                    unit.drawBuildPlans();
                }
            }
            if(!player.isBuilder() && input.lastPlans.size > 0){
                drawPlans(input, input.lastPlans, player.team());
            }
        }else if(player.isBuilder()){
            player.unit().drawBuildPlans();
        }

        if(input != null){
            input.drawBottom();
        }
    }

    public void drawTop(){

        if(!player.dead() && ui.hudfrag.shown){
            if(Core.settings.getBool("playerindicators")){
                for(Player player : Groups.player){
                    if(Vars.player != player && Vars.player.team() == player.team()){
                        if(!rect.setSize(Core.camera.width * 0.9f, Core.camera.height * 0.9f)
                        .setCenter(Core.camera.position.x, Core.camera.position.y).contains(player.x, player.y)){

                            Tmp.v1.set(player).sub(Vars.player).setLength(indicatorLength);

                            Lines.stroke(2f, Vars.player.team().color);
                            Lines.lineAngle(Vars.player.x + Tmp.v1.x, Vars.player.y + Tmp.v1.y, Tmp.v1.angle(), 4f);
                            Draw.reset();
                        }
                    }
                }
            }

            if(Core.settings.getBool("indicators") && !state.rules.fog){
                Groups.unit.each(unit -> {
                    if(!unit.isLocal() && unit.team != player.team() && !rect.setSize(Core.camera.width * 0.9f, Core.camera.height * 0.9f)
                    .setCenter(Core.camera.position.x, Core.camera.position.y).contains(unit.x, unit.y)){
                        Tmp.v1.set(unit.x, unit.y).sub(player).setLength(indicatorLength);

                        Lines.stroke(1f, unit.team().color);
                        Lines.lineAngle(player.x + Tmp.v1.x, player.y + Tmp.v1.y, Tmp.v1.angle(), 3f);
                        Draw.reset();
                    }
                });
            }
        }

        InputHandler input = control.input;

        //Removed Ctrl hover effects - no longer showing yellow highlight and rotation on units/buildings

        //draw config selected block
        if(input.config.isShown()){
            Building tile = input.config.getSelected();
            tile.drawConfigure();
        }

        input.drawTop();
        input.drawUnitSelection();

        boolean dead = player.dead();

        if(!dead){
            buildFade = Mathf.lerpDelta(buildFade, input.isPlacing() || input.isUsingSchematic() ? 1f : 0f, 0.06f);

            Draw.reset();
            Lines.stroke(buildFade * 2f);

            if(buildFade > 0.005f){
                if(state.rules.polygonCoreProtection){
                    updateCoreEdges();
                    Draw.color(Pal.accent);

                    for(int i = 0; i < 2; i++){
                        float offset = (i == 0 ? -2f : 0f);
                        for(CoreEdge edge : cedges){
                            Team displayed = edge.displayed();
                            if(displayed != null){
                                Draw.color(i == 0 ? Color.darkGray : Tmp.c1.set(displayed.color).lerp(Pal.accent, Mathf.absin(Time.time, 10f, 0.2f)));
                                Lines.line(edge.x1, edge.y1 + offset, edge.x2, edge.y2 + offset);
                            }
                        }
                    }

                    Draw.color();
                }else{
                    state.teams.eachEnemyCore(player.team(), core -> {
                        //it must be clear that there is a core here.
                        float br = state.rules.buildRadius(core.team);
                        if(/*core.wasVisible && */br > 0f && Core.camera.bounds(Tmp.r1).overlaps(Tmp.r2.setCentered(core.x, core.y, br * 2f))){
                            Draw.color(Color.darkGray);
                            Lines.circle(core.x, core.y - 2,br);
                            Draw.color(Pal.accent, core.team.color, 0.5f + Mathf.absin(Time.time, 10f, 0.5f));
                            Lines.circle(core.x, core.y, br);
                        }
                    });
                }
            }

            Lines.stroke(2f);
            Draw.color(Color.gray, Color.lightGray, Mathf.absin(Time.time, 8f, 1f));

            if(state.hasSpawns()){
                for(Tile tile : spawner.getSpawns()){
                    if(tile.within(player.x, player.y, state.rules.dropZoneRadius + spawnerMargin)){
                        Draw.alpha(Mathf.clamp(1f - (player.dst(tile) - state.rules.dropZoneRadius) / spawnerMargin));
                        Lines.dashCircle(tile.worldx(), tile.worldy(), state.rules.dropZoneRadius);
                    }
                }
            }

            Draw.reset();
        }

        //draw selected block
        if(!Core.scene.hasMouse()){
            Vec2 vec = control.input.mouseWorld(input.getMouseX(), input.getMouseY());
            Building build = world.buildWorld(vec.x, vec.y);

            if(build != null && build.team == player.team()){
                build.drawSelect();
                if(!build.enabled && build.block.drawDisabled){
                   build.drawDisabled();
                }

                if(Core.input.keyDown(Binding.rotatePlaced) && build.block.rotate && build.block.quickRotate && build.interactable(player.team())){
                    control.input.drawArrow(build.block, build.tileX(), build.tileY(), build.rotation, true);
                    Draw.color(Pal.accent, 0.3f + Mathf.absin(4f, 0.2f));
                    Fill.square(build.x, build.y, build.block.size * tilesize/2f);
                    Draw.color();
                }
            }
        }

        input.drawOverSelect();

        if(!Core.settings.getBool("selectionringabove", true)){
            Draw.draw(InputHandler.selectionRingLayer(), this::drawHoverRing);
        }

        if(dead) return; //dead players don't draw the rest

        if(ui.hudfrag.blockfrag.hover() instanceof Unit unit && unit.controller() instanceof LogicAI ai && ai.controller != null && ai.controller.isValid() && (state.isEditor() || !ai.controller.block.privileged)){
            var build = ai.controller;
            Drawf.square(build.x, build.y, build.block.size * tilesize/2f + 2f);
            if(!unit.within(build, unit.hitSize * 2f)){
                Drawf.arrow(unit.x, unit.y, build.x, build.y, unit.hitSize *2f, 4f);
            }
        }

        //draw selection overlay when dropping item
        if(input.isDroppingItem()){
            Vec2 v = control.input.mouseWorld(input.getMouseX(), input.getMouseY());
            float size = 8;
            Draw.rect(player.unit().item().fullIcon, v.x, v.y, size, size);
            Draw.color(Pal.accent);
            Lines.circle(v.x, v.y, 6 + Mathf.absin(Time.time, 5f, 1f));
            Draw.reset();

            Building build = world.buildWorld(v.x, v.y);
            if(input.canDropItem() && build != null && build.interactable(player.team()) && build.acceptStack(player.unit().item(), player.unit().stack.amount, player.unit()) > 0 && player.within(build, itemTransferRange) &&
                input.canDepositItem(build)){

                boolean invalid = !build.allowDeposit();

                Lines.stroke(3f, Pal.gray);
                Lines.square(build.x, build.y, build.block.size * tilesize / 2f + 3 + Mathf.absin(Time.time, 5f, 1f));
                Lines.stroke(1f, invalid ? Pal.remove : Pal.place);
                Lines.square(build.x, build.y, build.block.size * tilesize / 2f + 2 + Mathf.absin(Time.time, 5f, 1f));
                Draw.reset();

                if(invalid){
                    build.block.drawPlaceText(Core.bundle.get("bar.onlycoredeposit"), build.tileX(), build.tileY(), false);
                }
            }
        }
    }

    public void drawRadarIntelPostFog(){
        Team viewer = ViewerPerspective.team();
        if(viewer == null || state == null || state.isMenu()){
            return;
        }
        float pulse = 0.68f + Mathf.absin(Time.time, 8f, 0.12f);

        Draw.z(Layer.fogOfWar + 0.01f);

        //Enemy radars use white circles as well.
        Lines.stroke(radarCircleStroke);
        Draw.color(1f, 1f, 1f, 0.72f * pulse);
        Groups.build.each(build -> {
            if(build == null || !build.isValid() || build.block != Blocks.radar || build.team == viewer) return;
            Lines.circle(build.x, build.y, radarIntelRange);
        });

        //Friendly radars mark fogged enemy units with red dots; this does not reveal fog.
        Draw.color(Pal.remove, 0.95f);
        Groups.unit.each(unit -> {
            if(unit == null || !unit.isValid() || unit.team == viewer) return;
            if(fogControl.isVisible(viewer, unit.x, unit.y)) return;

            boolean inRadar = false;
            for(Building build : viewer.data().buildings){
                if(build == null || !build.isValid() || build.block != Blocks.radar) continue;
                if(build.within(unit, radarIntelRange)){
                    inRadar = true;
                    break;
                }
            }
            if(inRadar){
                Fill.circle(unit.x, unit.y, radarDotRadius);
            }
        });

        Draw.reset();
    }

    private void drawGroundProgressBars(){
        Team viewer = ViewerPerspective.team();
        if(viewer == null || state == null || state.isMenu()) return;

        Rect bounds = Core.camera.bounds(Tmp.r1);
        float vx = bounds.x - overheadBarCullMargin;
        float vy = bounds.y - overheadBarCullMargin;
        float vw = bounds.width + overheadBarCullMargin * 2f;
        float vh = bounds.height + overheadBarCullMargin * 2f;
        float maxX = vx + vw;
        float maxY = vy + vh;
        int healthBarDisplay = Core.settings.getInt("healthbardisplay", 0);

        Draw.draw(overheadGroundBarLayer, () -> {
            Groups.build.each(build -> {
                if(build == null || !build.isValid() || build.inFogTo(viewer)) return;
                if(build.x < vx || build.x > maxX || build.y < vy || build.y > maxY) return;
                boolean sameSide = !viewer.isEnemy(build.team);
                drawBuildingOverheadBars(build, healthBarDisplay, sameSide, sameSide, overheadGroundBarLayer);
            });

            Groups.unit.intersect(vx, vy, vw, vh, unit -> {
                if(unit == null || !unit.isValid() || unit.dead() || unit.inFogTo(viewer) || unit.isFlying()) return;
                boolean sameSide = !viewer.isEnemy(unit.team);
                drawUnitOverheadBars(unit, healthBarDisplay, sameSide, sameSide, overheadGroundBarLayer);
            });

            Draw.reset();
        });
    }

    private void drawAirProgressBars(){
        Team viewer = ViewerPerspective.team();
        if(viewer == null || state == null || state.isMenu()) return;

        Rect bounds = Core.camera.bounds(Tmp.r1);
        float vx = bounds.x - overheadBarCullMargin;
        float vy = bounds.y - overheadBarCullMargin;
        float vw = bounds.width + overheadBarCullMargin * 2f;
        float vh = bounds.height + overheadBarCullMargin * 2f;
        int healthBarDisplay = Core.settings.getInt("healthbardisplay", 0);

        Draw.draw(overheadAirBarLayer, () -> {
            Groups.unit.intersect(vx, vy, vw, vh, unit -> {
                if(unit == null || !unit.isValid() || unit.dead() || unit.inFogTo(viewer) || !unit.isFlying()) return;
                boolean sameSide = !viewer.isEnemy(unit.team);
                drawUnitOverheadBars(unit, healthBarDisplay, sameSide, sameSide, overheadAirBarLayer);
            });

            Draw.reset();
        });
    }

    public void drawWorldProgressBars(){
        drawGroundProgressBars();
        drawAirProgressBars();
    }

    private void drawUnitOverheadBars(Unit unit, int healthBarDisplay, boolean showLoadBar, boolean showProgressBar, float barLayer){
        float width = entityWidth(unit);
        if(width <= 0.001f) return;

        LoadBarData load = showLoadBar ? unitLoadData(unit) : null;
        float energy = unit.type.energyCapacity > 0f ? Mathf.clamp(unit.energy / Math.max(unit.type.energyCapacity, 0.001f)) : -1f;
        Color progressColor = Tmp.c2.set(overheadProgressColor);
        float progress = showProgressBar ? unitProgressFraction(unit, progressColor) : -1f;
        float remaining = showProgressBar ? unitRemainingFraction(unit) : -1f;
        float bottomY = unit.y + entityHeight(unit) / 2f + overheadBarBaseOffset - overheadBarStackShift;

        drawOverheadStack(unit.x, bottomY, width,
        Mathf.clamp(unit.healthf()),
        load,
        energy,
        progress, progressColor,
        remaining,
        healthBarDisplay,
        showLoadBar,
        barLayer);
    }

    private void drawBuildingOverheadBars(Building build, int healthBarDisplay, boolean showLoadBar, boolean showProgressBar, float barLayer){
        float width = entityWidth(build);
        if(width <= 0.001f) return;

        LoadBarData load = showLoadBar ? buildingLoadData(build) : null;
        float energy = buildingEnergyFraction(build);
        Color progressColor = Tmp.c2.set(overheadProgressColor);
        float progress = showProgressBar ? buildingProgressFraction(build, progressColor) : -1f;
        float remaining = showProgressBar ? buildingRemainingFraction(build) : -1f;
        float bottomY = build.y + entityHeight(build) / 2f + overheadBarBaseOffset;

        drawOverheadStack(build.x, bottomY, width,
        Mathf.clamp(build.healthf()),
        load,
        energy,
        progress, progressColor,
        remaining,
        healthBarDisplay,
        showLoadBar,
        barLayer);
    }

    private void drawOverheadStack(float x, float bottomY, float width, float health, @Nullable LoadBarData load, float energy, float progress, Color progressColor, float remaining, int healthBarDisplay, boolean sameSide, float barLayer){
        float y = bottomY;
        boolean drewRegularBar = false;
        int segments = segmentedBarSegments(width);

        if(energy >= 0f){
            drawSegmentedBar(x, y, width, energy, overheadEnergyColor, segments, barLayer);
            y += overheadBarSpacing;
            drewRegularBar = true;
        }
        if(load != null && load.totalSlots > 0){
            drawLoadBar(x, y, width, load, barLayer);
            y += overheadBarSpacing;
            drewRegularBar = true;
        }

        if(shouldDrawHealthBar(healthBarDisplay, health)){
            drawSegmentedBar(x, y, width, health, healthColor(health, sameSide), segments, barLayer);
            drewRegularBar = true;
        }

        int lowerBars = 0;
        if(progress >= 0f){
            float progressY = drewRegularBar ? bottomY - overheadBarSpacing : bottomY;
            drawSolidBar(x, progressY, width, progress, progressColor, false, barLayer);
            lowerBars = 1;
        }

        if(remaining >= 0f){
            float remainingY = drewRegularBar ? bottomY - overheadBarSpacing * (lowerBars + 1f) : bottomY;
            drawSolidBar(x, remainingY, width, remaining, overheadRemainingColor, false, barLayer);
        }
    }

    private boolean shouldDrawHealthBar(int mode, float health){
        if(mode == 1) return true;
        return mode == 2 && health < 0.999f;
    }

    private float buildingProgressFraction(Building build, Color colorOut){
        if(build instanceof ConstructBuild cons && cons.current != null && cons.current != Blocks.air && cons.progress < 1f){
            colorOut.set(overheadProgressColor);
            return Mathf.clamp(cons.progress);
        }

        if(build instanceof CoreBuild core){
            if(core.isUpgrading()){
                colorOut.set(overheadProgressColor);
                return Mathf.clamp(core.isUpgradingOrbital() ? core.orbitalUpgradeFraction() : core.fortressUpgradeFraction());
            }
            if(core.unitQueue != null && !core.unitQueue.isEmpty()){
                colorOut.set(overheadProgressColor);
                return Mathf.clamp(core.unitProgressFraction());
            }
        }

        if(build instanceof UnitFactoryBuild factory){
            if(factory.currentPlan != -1){
                colorOut.set(overheadProgressColor);
                return Mathf.clamp(factory.fraction());
            }

            if(factory.hasTechAddon()){
                Sc2ResearchSpec spec = ResearchQueueService.techLabActiveResearch(build.team, factory.block);
                if(spec != null){
                    colorOut.set(overheadProgressColor);
                    return Mathf.clamp(spec.progress(build.team));
                }
            }
        }

        if(build.block == Blocks.launchPad){
            if(UnitTypes.ghostWarheadProducing(build)){
                colorOut.set(overheadProgressColor);
                return Mathf.clamp(UnitTypes.ghostWarheadProductionProgress(build));
            }
            if(UnitTypes.ghostCamoAnyResearching(build.team)){
                colorOut.set(overheadProgressColor);
                return Mathf.clamp(UnitTypes.ghostCamoResearchProgress(build.team));
            }
        }

        Sc2ResearchSpec research = buildingResearchSpec(build);
        if(research != null){
            colorOut.set(overheadProgressColor);
            return Mathf.clamp(research.progress(build.team));
        }

        return -1f;
    }

    private float buildingRemainingFraction(Building build){
        if(build instanceof BunkerBlock.BunkerBuild bunker && bunker.isRecycling()){
            return Mathf.clamp(bunker.recycleRemainingFraction());
        }
        if(build instanceof Radar.RadarBuild radar && radar.isRecycling()){
            return Mathf.clamp(radar.recycleRemainingFraction());
        }
        return -1f;
    }

    private @Nullable Sc2ResearchSpec buildingResearchSpec(Building build){
        Team team = build.team;

        if(ResearchQueueService.armoryActiveResearchBlock(team) == build.block){
            Sc2ResearchSpec spec = ResearchQueueService.armoryActiveResearch(team);
            if(spec != null) return spec;
        }
        if(ResearchQueueService.engineeringActiveResearchBlock(team) == build.block){
            Sc2ResearchSpec spec = ResearchQueueService.engineeringActiveResearch(team);
            if(spec != null) return spec;
        }
        if(build.block == Blocks.surgeCrucible){
            Sc2ResearchSpec spec = ResearchQueueService.fusionCoreActiveResearch(team);
            if(spec != null) return spec;
        }

        return null;
    }

    private float unitProgressFraction(Unit unit, Color colorOut){
        float progress = UnitTypes.widowReloadProgress(unit);
        if(progress > 0f){
            colorOut.set(Color.gray);
            return progress;
        }

        progress = UnitTypes.battlecruiserYamatoChargeProgress(unit);
        if(progress > 0f){
            colorOut.set(overheadProgressColor);
            return progress;
        }

        progress = UnitTypes.battlecruiserWarpChargeProgress(unit);
        if(progress > 0f){
            colorOut.set(Color.valueOf("b9f7ff"));
            return progress;
        }

        return -1f;
    }

    private float unitRemainingFraction(Unit unit){
        float remaining = PulsarDrops.remainingFraction(unit);
        if(remaining > 0f) return remaining;

        remaining = UnitTypes.ravenTurretLifeProgress(unit);
        if(remaining > 0f) return remaining;

        if(UnitTypes.medivacAfterburnerActive(unit)){
            return Mathf.clamp(unit.getDuration(StatusEffects.medivacAfterburner) / Math.max(UnitTypes.medivacAfterburnerDuration(), 0.001f));
        }

        if(unit.hasEffect(StatusEffects.barracksStimpackMarine) || unit.hasEffect(StatusEffects.barracksStimpackMarauder)){
            float time = Math.max(unit.getDuration(StatusEffects.barracksStimpackMarine), unit.getDuration(StatusEffects.barracksStimpackMarauder));
            return Mathf.clamp(time / Math.max(UnitTypes.barracksStimpackDuration(), 0.001f));
        }

        if(UnitTypes.hurricaneLockActive(unit)){
            return Mathf.clamp(UnitTypes.getHurricaneLockData(unit).activeTime / Math.max(UnitTypes.hurricaneLockDuration(), 0.001f));
        }

        if(unit.hasEffect(StatusEffects.ravenAntiArmor)){
            return Mathf.clamp(unit.getDuration(StatusEffects.ravenAntiArmor) / Math.max(UnitTypes.ravenAntiArmorDuration(), 0.001f));
        }

        if(unit.hasEffect(StatusEffects.ravenMatrixLock)){
            return Mathf.clamp(unit.getDuration(StatusEffects.ravenMatrixLock) / Math.max(UnitTypes.ravenMatrixDuration(), 0.001f));
        }

        return -1f;
    }

    private @Nullable LoadBarData unitLoadData(Unit unit){
        if(!UnitTypes.isMedivac(unit) || !(unit instanceof Payloadc payload)) return null;

        LoadBarData data = new LoadBarData(8);
        Seq<Payload> payloads = payload.payloads();
        for(int i = 0; i < payloads.size && data.hasFreeSlots(); i++){
            data.occupy(i + 1, payloadSlotCost(payloads.get(i)));
        }
        return data;
    }

    private @Nullable LoadBarData buildingLoadData(Building build){
        if(!(build instanceof BunkerBlock.BunkerBuild bunker)) return null;

        int totalSlots = bunker.usedSlots() + bunker.freeSlots();
        if(totalSlots <= 0) return null;

        LoadBarData data = new LoadBarData(totalSlots);
        for(int i = 0; i < bunker.garrison.size && data.hasFreeSlots(); i++){
            BunkerBlock.GarrisonEntry entry = bunker.garrison.get(i);
            data.occupy(i + 1, BunkerBlock.unitSlotCost(content.unit(entry.typeId)));
        }
        return data;
    }

    private float buildingEnergyFraction(Building build){
        if(build instanceof CoreBuild core && build.block == Blocks.coreOrbital && core.orbitalEnergy >= 0f){
            return Mathf.clamp(core.orbitalEnergy / CoreBlock.orbitalEnergyCap);
        }
        return -1f;
    }

    private float entityWidth(Unit unit){
        TextureRegion region = unit.type.region != null && unit.type.region.found() ? unit.type.region : unit.type.fullIcon;
        float width = region != null && region.found() ? region.width * region.scl() : unit.hitSize;
        return Math.max(unit.hitSize, width);
    }

    private float entityHeight(Unit unit){
        TextureRegion region = unit.type.region != null && unit.type.region.found() ? unit.type.region : unit.type.fullIcon;
        float height = region != null && region.found() ? region.height * region.scl() : unit.hitSize;
        return Math.max(unit.hitSize, height);
    }

    private float entityWidth(Building build){
        TextureRegion region = build.block.fullIcon != null && build.block.fullIcon.found() ? build.block.fullIcon : build.block.region;
        float width = region != null && region.found() ? region.width * region.scl() : build.hitSize();
        return Math.max(build.block.size * tilesize, width);
    }

    private float entityHeight(Building build){
        TextureRegion region = build.block.fullIcon != null && build.block.fullIcon.found() ? build.block.fullIcon : build.block.region;
        float height = region != null && region.found() ? region.height * region.scl() : build.hitSize();
        return Math.max(build.block.size * tilesize, height);
    }

    private int segmentedBarSegments(float width){
        return segmentCount(width, overheadSegmentIdeal, overheadSegmentMax, 0f);
    }

    private int segmentCount(float width, float idealWidth, int maxSegments, float gap){
        return Math.max(1, Math.min(maxSegments, Math.round((Math.max(width, 1f) + gap) / Math.max(idealWidth + gap, 0.001f))));
    }

    private Color healthColor(float fraction, boolean sameSide){
        if(!sameSide) return Tmp.c1.set(overheadEnemyHealthColor);
        return Tmp.c1.set(overheadHealthLow).lerp(overheadHealthHigh, Mathf.clamp(fraction));
    }

    private void drawSegmentedBar(float x, float y, float width, float progress, Color color, int segments, float barLayer){
        float clamped = Mathf.clamp(progress);
        float totalWidth = Math.max(width, 1f);
        int count = Math.max(1, segments);
        float segmentWidth = totalWidth / count;
        float left = x - totalWidth / 2f;
        float innerHeight = Math.max(0.2f, overheadBarHeight - overheadInnerInset * 2f);

        Draw.z(barLayer);
        Draw.color(color);

        for(int i = 0; i < count; i++){
            float segProgress = clamped * count - i;
            float fill = Mathf.clamp(segProgress);
            if(fill <= 0.001f) continue;

            float segLeft = left + i * segmentWidth;
            float fillWidth = Math.max(0f, (segmentWidth - overheadInnerInset * 2f) * fill);
            if(fillWidth <= 0.001f) continue;

            Fill.rect(segLeft + overheadInnerInset + fillWidth / 2f, y, fillWidth, innerHeight);
        }

        Draw.z(barLayer + overheadBorderLayerOffset);
        Draw.color(Color.black);
        Lines.stroke(overheadBorderStroke);
        float bottom = y - overheadBarHeight / 2f;
        float top = y + overheadBarHeight / 2f;
        Lines.rect(left, bottom, totalWidth, overheadBarHeight);

        for(int i = 1; i < count; i++){
            float split = left + i * segmentWidth;
            Lines.line(split, bottom, split, top);
        }

        Draw.reset();
    }

    private int payloadSlotCost(Payload payload){
        if(payload instanceof UnitPayload up){
            return Math.max(1, UnitTypes.medivacUnitSlotCost(up.unit == null ? null : up.unit.type));
        }
        return Math.max(1, Mathf.ceil(payload.size() * payload.size() / tilePayload));
    }

    private void drawLoadBar(float x, float y, float width, LoadBarData load, float barLayer){
        float totalWidth = Math.max(width, 1f);
        float slotWidth = totalWidth / load.totalSlots;
        float slotLeft = x - totalWidth / 2f;
        float left = x - totalWidth / 2f;
        float innerHeight = Math.max(0.2f, overheadBarHeight - overheadInnerInset * 2f);

        Draw.z(barLayer);
        Draw.color(Color.white);

        for(int i = 0; i < load.totalSlots; ){
            int owner = load.slotOwners.get(i);
            int start = i;
            while(i < load.totalSlots && load.slotOwners.get(i) == owner){
                i++;
            }

            if(owner <= 0) continue;

            float fillWidth = Math.max(0f, (i - start) * slotWidth - overheadInnerInset * 2f);
            if(fillWidth <= 0.001f) continue;

            float fillLeft = slotLeft + start * slotWidth + overheadInnerInset;
            Fill.rect(fillLeft + fillWidth / 2f, y, fillWidth, innerHeight);
        }

        Draw.z(barLayer + overheadBorderLayerOffset);
        Draw.color(Color.black);
        Lines.stroke(overheadBorderStroke);
        float bottom = y - overheadBarHeight / 2f;
        float top = y + overheadBarHeight / 2f;
        Lines.rect(left, bottom, totalWidth, overheadBarHeight);

        for(int i = 1; i < load.totalSlots; i++){
            if(!load.divider(i - 1)) continue;
            float split = slotLeft + i * slotWidth;
            Lines.line(split, bottom, split, top);
        }

        Draw.reset();
    }

    private void drawSolidBar(float x, float y, float width, float progress, Color color, boolean reverse, float barLayer){
        float clamped = Mathf.clamp(progress);
        float totalWidth = Math.max(width, 1f);
        float left = x - totalWidth / 2f;
        float right = x + totalWidth / 2f;
        float innerHeight = Math.max(0.2f, overheadBarHeight - overheadInnerInset * 2f);
        float innerWidth = Math.max(0f, (totalWidth - overheadInnerInset * 2f) * clamped);

        if(innerWidth > 0.001f){
            Draw.z(barLayer);
            Draw.color(color);
            float fillCenter = reverse ? right - overheadInnerInset - innerWidth / 2f : left + overheadInnerInset + innerWidth / 2f;
            Fill.rect(fillCenter, y, innerWidth, innerHeight);
        }

        Draw.z(barLayer + overheadBorderLayerOffset);
        Draw.color(Color.black);
        Lines.stroke(overheadBorderStroke);
        Lines.rect(left, y - overheadBarHeight / 2f, totalWidth, overheadBarHeight);
        Draw.reset();
    }

    private static class LoadBarData{
        final IntSeq slotOwners;
        final int totalSlots;
        int nextSlot;

        LoadBarData(int totalSlots){
            this.totalSlots = Math.max(0, totalSlots);
            this.slotOwners = new IntSeq(this.totalSlots);

            for(int i = 0; i < this.totalSlots; i++){
                slotOwners.add(-(i + 1));
            }
        }

        boolean hasFreeSlots(){
            return nextSlot < totalSlots;
        }

        void occupy(int owner, int slots){
            int amount = Math.max(1, slots);
            for(int i = 0; i < amount && nextSlot < totalSlots; i++){
                slotOwners.set(nextSlot++, owner);
            }
        }

        boolean filled(int slot){
            return slotOwners.get(slot) > 0;
        }

        boolean divider(int leftSlot){
            return slotOwners.get(leftSlot) != slotOwners.get(leftSlot + 1);
        }
    }

    public void drawHoverRing(){
        if(control.input == null) return;

        if(control.input instanceof DesktopInput input && input.isMiddleMousePanning()) return;

        var hover = control.input.updateHover(false);
        if(!hover.isValid()) return;

        if(Core.input.keyTap(KeyCode.mouseRight)){
            Object target = hover.unit != null ? hover.unit : (hover.build != null ? hover.build : hover.resource);
            boolean selectedTarget = false;
            if(hover.unit != null){
                selectedTarget = control.input.selectedUnits.contains(hover.unit);
            }else if(hover.build != null){
                selectedTarget = control.input.commandBuildings.contains(hover.build);
            }
            if(!selectedTarget){
                hoverPulseTarget = target;
                hoverPulseStart = Time.time;
                hoverPulseUntil = Time.time + 60f;
                hoverPulseX = hover.x;
                hoverPulseY = hover.y;
                hoverPulseRadius = hoverPulseRadius(hover.radius);
                hoverPulseColor.set(hoverColor(hover));
            }
        }

        Lines.stroke(InputHandler.selectionRingStroke);
        Draw.color(hoverColor(hover));

        if(hover.resource != null){
            Lines.circle(hover.x, hover.y, Math.max(1f, hover.radius + InputHandler.selectionSolidRadiusOffset));
        }else{
            float radius = hoverRotatingRadius(hover.radius);
            drawHoverArcRing(hover.x, hover.y, radius, Time.time * 360f / (60f * 4f), hoverColor(hover));
        }

        if(hoverPulseTarget != null && Time.time < hoverPulseUntil){
            float arcDeg = 31.5f;
            float step = 45f;
            boolean valid = true;
            if(hoverPulseTarget instanceof Unit unit){
                valid = unit.isValid();
                if(valid){
                    hoverPulseX = unit.x;
                    hoverPulseY = unit.y;
                    hoverPulseRadius = hoverPulseRadius(unit.hitSize / 2f);
                }
            }else if(hoverPulseTarget instanceof Building build){
                valid = build.isValid();
                if(valid){
                    hoverPulseX = build.x;
                    hoverPulseY = build.y;
                    hoverPulseRadius = hoverPulseRadius(build.hitSize() / 2f);
                }
            }else if(hoverPulseTarget instanceof Tile tile){
                hoverPulseX = tile.worldx();
                hoverPulseY = tile.worldy();
                hoverPulseRadius = hoverPulseRadius(tilesize / 2f);
            }
            if(valid){
                float pulseRot = (Time.time - hoverPulseStart) * 360f / 60f;
                drawHoverArcRing(hoverPulseX, hoverPulseY, hoverPulseRadius, pulseRot, hoverPulseColor);
            }
        }
        if(hoverPulseTarget != null && Time.time >= hoverPulseUntil){
            hoverPulseTarget = null;
        }

        drawHoverOwnerBox(hover);
        Draw.reset();
    }

    private float hoverRotatingRadius(float baseRadius){
        return Math.max(1f, baseRadius + InputHandler.selectionRotatingDashedRadiusOffset);
    }

    private float hoverPulseRadius(float baseRadius){
        return Math.max(1f, baseRadius + InputHandler.selectionRotatingDashedRadiusOffset * 2f);
    }

    public static void drawHoverArcRing(float x, float y, float radius, float rotationDeg, Color color){
        float arcDeg = 37.5f;
        float step = 45f;
        Lines.stroke(InputHandler.selectionRingStroke);
        Draw.color(color);
        for(int i = 0; i < 8; i++){
            Lines.arc(x, y, radius, arcDeg / 360f, rotationDeg + i * step);
        }
    }

    private void drawHoverOwnerBox(InputHandler.HoverInfo hover){
        if(!net.active() || Groups.player.size() <= 1) return;
        if(hover == null || !hover.isValid() || hover.resource != null) return;

        String owner = hoverOwnerText(hover);
        if(owner == null || owner.isEmpty()) return;

        float radius = hover.resource != null ? Math.max(1f, hover.radius + InputHandler.selectionSolidRadiusOffset) : hoverRotatingRadius(hover.radius);
        Font font = Fonts.outline;
        GlyphLayout layout = Pools.obtain(GlyphLayout.class, GlyphLayout::new);
        boolean ints = font.usesIntegerPositions();
        font.setUseIntegerPositions(false);
        font.getData().setScale(hoverOwnerFontScale / Scl.scl(1f));
        layout.setText(font, owner);

        float boxW = layout.width + hoverOwnerPadX * 2f;
        float boxH = layout.height + hoverOwnerPadY * 2f;
        float boxX = hover.x;
        float boxY = hover.y - radius - hoverOwnerGap - boxH / 2f;

        Draw.z(Layer.playerName);
        Draw.color(0f, 0f, 0f, 0.6f);
        Fill.rect(boxX, boxY, boxW, boxH);
        Draw.color(hoverColor(hover));
        Lines.stroke(hoverOwnerBorderStroke);
        Lines.rect(boxX - boxW / 2f, boxY - boxH / 2f, boxW, boxH);

        float prev = Drawf.text();
        font.setColor(Color.white);
        font.draw(owner, boxX, boxY + layout.height / 2f, 0f, Align.center, false);
        Draw.z(prev);

        font.getData().setScale(1f);
        font.setUseIntegerPositions(ints);
        Draw.reset();
        Pools.free(layout);
    }

    private @Nullable String hoverOwnerText(InputHandler.HoverInfo hover){
        if(hover.unit != null){
            String owner = hover.unit.getControllerName();
            if(owner == null) owner = hover.unit.ownerName;
            if(owner == null && hover.unit.team != null) owner = hover.unit.team.coloredName();
            return owner;
        }
        if(hover.build != null){
            String owner = hover.build.ownerName;
            if(owner == null) owner = hover.build.lastAccessed;
            if(owner == null && hover.build.team != null) owner = hover.build.team.coloredName();
            return owner;
        }
        return null;
    }

    private Color hoverColor(InputHandler.HoverInfo hover){
        if(hover.resource != null) return Color.yellow;
        Team team = hover.team;
        if(team == null) return Color.white;
        if(ViewerPerspective.isFriendly(team)) return Color.green;
        if(team == Team.derelict) return Color.yellow;
        return Color.red;
    }

    public void checkApplySelection(Unit u){
        if(unitFade > 0.001f && lastSelect == u){
            Color prev = Draw.getMixColor();
            Draw.mixcol(prev.a > 0.001f ? prev.lerp(Pal.accent, unitFade) : Pal.accent, Math.max(unitFade, prev.a));
        }
    }

    private static class CoreEdge{
        float x1, y1, x2, y2;
        Team t1, t2;

        public CoreEdge(float x1, float y1, float x2, float y2, Team t1, Team t2){
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
            this.t1 = t1;
            this.t2 = t2;
        }

        @Nullable
        Team displayed(){
            Team viewer = ViewerPerspective.team();
            return
                t1 == t2 ? null :
                t1 == viewer ? t2 :
                t2 == viewer ? t1 :
                t2.id == 0 ? t1 :
                t1.id < t2.id && t1.id != 0 ? t1 : t2;
        }
    }
}
