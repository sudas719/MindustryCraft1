package mindustry.graphics;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.input.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
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
import mindustry.world.*;
import mindustry.world.blocks.ConstructBlock.*;
import mindustry.world.blocks.storage.CoreBlock.*;
import mindustry.world.blocks.units.UnitFactory.*;

import static mindustry.Vars.*;

public class OverlayRenderer{
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
        drawProgressBars();

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
            Vec2 vec = Core.input.mouseWorld(input.getMouseX(), input.getMouseY());
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

        drawHoverRing();

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
            Vec2 v = Core.input.mouseWorld(input.getMouseX(), input.getMouseY());
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

    private void drawProgressBars(){
        InputHandler input = control.input;
        if(input == null) return;
        var hover = input.updateHover(false);
        Building hoverBuild = hover != null && hover.build != null && hover.build.isValid() ? hover.build : null;
        Unit hoverUnit = hover != null && hover.unit != null && hover.unit.isValid() ? hover.unit : null;

        Draw.draw(Layer.blockOver + 1f, () -> {
            if(hoverBuild != null){
                if(hoverBuild instanceof ConstructBuild cons && cons.current != null && cons.current != Blocks.air && cons.progress < 1f){
                    float size = cons.current.size * tilesize;
                    drawProgressBar(hoverBuild.x, hoverBuild.y, size, cons.progress);
                }else if(hoverBuild instanceof CoreBuild core){
                    if(core.unitQueue != null && !core.unitQueue.isEmpty()){
                        drawProgressBar(hoverBuild.x, hoverBuild.y, hoverBuild.hitSize(), core.unitProgressFraction());
                    }
                }else if(hoverBuild instanceof UnitFactoryBuild factory){
                    if(factory.currentPlan != -1){
                        drawProgressBar(hoverBuild.x, hoverBuild.y, hoverBuild.hitSize(), factory.fraction());
                    }
                }
            }
            if(hoverUnit != null && hoverUnit.type.energyCapacity > 0f){
                drawEnergyBar(hoverUnit.x, hoverUnit.y, hoverUnit.hitSize, hoverUnit.energy / hoverUnit.type.energyCapacity);
            }
            Draw.reset();
        });
    }

    private void drawProgressBar(float x, float y, float size, float progress){
        float barWidth = size;
        float barHeight = 3.5f;
        float offset = size / 2f + 4f;
        float clamped = Mathf.clamp(progress);

        Draw.color(Color.black, 0.6f);
        Fill.rect(x, y + offset, barWidth, barHeight);
        Draw.color(Pal.accent);
        Fill.rect(x - barWidth / 2f + barWidth * clamped / 2f, y + offset, barWidth * clamped, barHeight);
    }

    private void drawEnergyBar(float x, float y, float size, float progress){
        float barWidth = size;
        float barHeight = 3.5f;
        float offset = size / 2f + 8f;
        float clamped = Mathf.clamp(progress);

        Draw.color(Color.black, 0.6f);
        Fill.rect(x, y + offset, barWidth, barHeight);
        Draw.color(Color.valueOf("b57aff"));
        Fill.rect(x - barWidth / 2f + barWidth * clamped / 2f, y + offset, barWidth * clamped, barHeight);
    }

    private void drawHoverRing(){
        if(control.input == null) return;

        var hover = control.input.updateHover(false);
        if(!hover.isValid()) return;

        if(Core.input.keyTap(KeyCode.mouseRight)){
            hoverPulseTarget = hover.unit != null ? hover.unit : (hover.build != null ? hover.build : hover.resource);
            hoverPulseStart = Time.time;
            hoverPulseUntil = Time.time + 60f;
            hoverPulseX = hover.x;
            hoverPulseY = hover.y;
            hoverPulseRadius = hover.radius + 6f;
            hoverPulseColor.set(hoverColor(hover));
        }

        Draw.z(Layer.overlayUI + 0.01f);
        Lines.stroke(1.6f);
        Draw.color(hoverColor(hover));

        if(hover.resource != null){
            Lines.circle(hover.x, hover.y, hover.radius);
        }else{
            float rotation = Time.time * 360f / (60f * 4f);
            float radius = hover.radius + 2f;
            float arcDeg = 31.5f;
            float step = 45f;
            for(int i = 0; i < 8; i++){
                Lines.arc(hover.x, hover.y, radius, arcDeg / 360f, rotation + i * step);
            }
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
                    hoverPulseRadius = unit.hitSize / 2f + 6f;
                }
            }else if(hoverPulseTarget instanceof Building build){
                valid = build.isValid();
                if(valid){
                    hoverPulseX = build.x;
                    hoverPulseY = build.y;
                    hoverPulseRadius = build.hitSize() / 2f + 6f;
                }
            }else if(hoverPulseTarget instanceof Tile tile){
                hoverPulseX = tile.worldx();
                hoverPulseY = tile.worldy();
                hoverPulseRadius = tilesize / 2f + 6f;
            }
            if(valid){
                float pulseRot = (Time.time - hoverPulseStart) * 360f / 60f;
                Draw.color(hoverPulseColor);
                for(int i = 0; i < 8; i++){
                    Lines.arc(hoverPulseX, hoverPulseY, hoverPulseRadius, arcDeg / 360f, pulseRot + i * step);
                }
            }
        }
        if(hoverPulseTarget != null && Time.time >= hoverPulseUntil){
            hoverPulseTarget = null;
        }

        Draw.reset();
    }

    private Color hoverColor(InputHandler.HoverInfo hover){
        if(hover.resource != null) return Color.yellow;
        Team team = hover.team;
        if(team == null) return Color.white;
        if(team == player.team()) return Color.green;
        if(team == Team.derelict) return Color.yellow;
        return team != player.team() ? Color.red : Color.green;
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
            return
                t1 == t2 ? null :
                t1 == player.team() ? t2 :
                t2 == player.team() ? t1 :
                t2.id == 0 ? t1 :
                t1.id < t2.id && t1.id != 0 ? t1 : t2;
        }
    }
}
