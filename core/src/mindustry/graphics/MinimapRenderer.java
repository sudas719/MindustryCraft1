package mindustry.graphics;

import arc.*;
import arc.graphics.*;
import arc.graphics.Texture.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.pooling.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.game.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.io.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.environment.*;

import static mindustry.Vars.*;

public class MinimapRenderer{
    private static final float baseSize = 16f, updateInterval = 2f;
    private static final float radarIntelRange = 23f * tilesize;
    private static final float radarCircleStroke = 1.15f;
    private static final float minimapFogOpacity = 0.5f;
    private static final float minimapDynamicFogAlpha = 0.2f;
    private static final float minimapStaticFogAlpha = (minimapFogOpacity - minimapDynamicFogAlpha) / (1f - minimapDynamicFogAlpha);
    private static final Color sc2ResourceMarkerColor = Color.valueOf("87cefa");
    private static final float sc2ResourceMarkerStroke = 1.1f;
    private static final float sc2MineralMarkerSize = tilesize * 0.82f;
    private static final float sc2GasMarkerSize = tilesize * 3f;
    private static final Color sc2DerelictLogoColor = Color.valueOf("d6d6d6");
    private static final float sc2DerelictLogoSize = tilesize * 2.1f;

    private final Seq<Unit> units = new Seq<>();
    private final IntSeq sc2MineralMarkers = new IntSeq();
    private final IntSeq sc2GasMarkers = new IntSeq();
    private final FloatSeq sc2DerelictScrapCenters = new FloatSeq();
    private Pixmap pixmap;
    private Texture texture;
    private TextureRegion region;
    private Rect rect = new Rect();
    private float zoom = 4;

    private IntSet updates = new IntSet();
    private float updateCounter = 0f;
    private boolean sc2ResourceMarkersDirty = true;

    public MinimapRenderer(){
        Events.on(WorldLoadEvent.class, event -> {
            reset();
            updateAll();
            markSc2ResourceMarkersDirty();
        });

        Events.on(TileChangeEvent.class, event -> {
            if(!ui.editor.isShown()){
                update(event.tile);

                //update floor below block.
                if(event.tile.block().solid && event.tile.y > 0 && event.tile.isCenter()){
                    event.tile.getLinkedTiles(t -> {
                        Tile tile = world.tile(t.x, t.y - 1);
                        if(tile != null && tile.block() == Blocks.air){
                            update(tile);
                        }
                    });
                }
            }
        });

        Events.on(TilePreChangeEvent.class, e -> {
            //update floor below a *recently removed* block.
            if(e.tile.block().solid && e.tile.y > 0){
                Tile tile = world.tile(e.tile.x, e.tile.y - 1);
                if(tile.block() == Blocks.air){
                    Time.run(0f, () -> update(tile));
                }
            }
        });

        Events.on(TileFloorChangeEvent.class, event -> markSc2ResourceMarkersDirty());
        Events.on(TileChangeEvent.class, event -> markSc2ResourceMarkersDirty());
        Events.on(BuildTeamChangeEvent.class, event -> {
            update(event.build.tile);
            markSc2ResourceMarkersDirty();
        });
    }

    public void update(){
        //updates are batched to occur every 2 frames
        if((updateCounter += Time.delta) >= updateInterval){
            updateCounter %= updateInterval;

            updates.each(pos -> {
                Tile tile = world.tile(pos);
                if(tile == null) return;

                int color = colorFor(tile);
                pixmap.set(tile.x, pixmap.height - 1 - tile.y, color);

                //yes, this calls glTexSubImage2D every time, with a 1x1 region
                Pixmaps.drawPixel(texture, tile.x, pixmap.height - 1 - tile.y, color);
            });

            updates.clear();
        }
    }

    public Pixmap getPixmap(){
        return pixmap;
    }

    public @Nullable Texture getTexture(){
        return texture;
    }

    public void zoomBy(float amount){
        zoom += amount;
        setZoom(zoom);
    }

    public void setZoom(float amount){
        zoom = Mathf.clamp(amount, 1f, Math.min(world.width(), world.height()) / baseSize / 2f);
    }

    public float getZoom(){
        return zoom;
    }

    public void reset(){
        updates.clear();
        sc2MineralMarkers.clear();
        sc2GasMarkers.clear();
        sc2DerelictScrapCenters.clear();
        sc2ResourceMarkersDirty = true;
        if(pixmap != null){
            pixmap.dispose();
            texture.dispose();
        }
        //Set zoom to show entire map by default
        float maxZoom = Math.min(world.width(), world.height()) / baseSize / 2f;
        setZoom(maxZoom);
        pixmap = new Pixmap(world.width(), world.height());
        texture = new Texture(pixmap);
        region = new TextureRegion(texture);
    }

    public void drawEntities(float x, float y, float w, float h, boolean fullView){

        if(!fullView){
            updateUnitArray();
        }else{
            units.clear();
            Groups.unit.copy(units);
        }

        float sz = baseSize * zoom;
        float dx = (Core.camera.position.x / tilesize);
        float dy = (Core.camera.position.y / tilesize);
        dx = Mathf.clamp(dx, sz, world.width() - sz);
        dy = Mathf.clamp(dy, sz, world.height() - sz);

        rect.set((dx - sz) * tilesize, (dy - sz) * tilesize, sz * 2 * tilesize, sz * 2 * tilesize);

        Tmp.m2.set(Draw.trans());

        float scaleFactor;
        var trans = Tmp.m1.idt();
        trans.translate(x, y);
        if(!fullView){
            trans.scl(Tmp.v1.set(scaleFactor = w / rect.width, h / rect.height));
            trans.translate(-rect.x, -rect.y);
        }else{
            trans.scl(Tmp.v1.set(scaleFactor = w / world.unitWidth(), h / world.unitHeight()));
        }
        trans.translate(tilesize / 2f, tilesize / 2f);
        Draw.trans(trans);

        scaleFactor = 1f / scaleFactor;
        ensureSc2ResourceMarkers();

        Team viewer = ViewerPerspective.team();

        for(Unit unit : units){
            if(viewer != null && unit.inFogTo(viewer) || !unit.type.drawMinimap) continue;

            float scale = Scl.scl(1f) * tilesize * 3;
            var region = unit.icon();

            Draw.mixcol(unit.team.color, 1f);
            Draw.rect(region, unit.x, unit.y, scale, scale * (float)region.height / region.width, unit.rotation() - 90);
            Draw.reset();
        }

        //Show radar intel ring on minimap for all radar buildings regardless team.
        float pulse = 0.68f + Mathf.absin(Time.time, 8f, 0.12f);
        Lines.stroke(Scl.scl(radarCircleStroke) * scaleFactor);
        Draw.color(1f, 1f, 1f, 0.72f * pulse);
        Groups.build.each(build -> {
            if(build == null || !build.isValid() || build.block != Blocks.radar) return;
            Lines.circle(build.x, build.y, radarIntelRange);
        });
        Draw.reset();

        if(fullView && net.active()){
            for(Player player : Groups.player){
                if(!player.dead()){
                    drawLabel(player.x, player.y, player.name, player.color, scaleFactor);
                }
            }
        }

        Draw.reset();

        boolean spectatorView = net.active() && ViewerPerspective.isSpectatorTeam(viewer);
        if(state.rules.fog && !spectatorView){
            if(fullView){
                float z = zoom;
                //max zoom out fixes everything, somehow?
                setZoom(99999f);
                getRegion();
                zoom = z;
            }
            Draw.shader(Shaders.fog);
            Texture staticTex = renderer.fog.getStaticTexture(), dynamicTex = renderer.fog.getDynamicTexture();

            //crisp pixels
            dynamicTex.setFilter(TextureFilter.nearest);

            Tmp.tr1.set(dynamicTex);
            Tmp.tr1.set(0f, 1f, 1f, 0f);

            float wf = world.width() * tilesize;
            float hf = world.height() * tilesize;

            Draw.color(state.rules.dynamicColor, minimapDynamicFogAlpha);
            Draw.rect(Tmp.tr1, wf / 2, hf / 2, wf, hf);

            if(state.rules.staticFog){
                staticTex.setFilter(TextureFilter.nearest);

                Tmp.tr1.texture = staticTex;
                //must be black to fit with borders
                Draw.color(0f, 0f, 0f, minimapStaticFogAlpha);
                Draw.rect(Tmp.tr1, wf / 2, hf / 2, wf, hf);
            }

            Draw.color();
            Draw.shader();
        }

        drawSc2ResourceMarkers(scaleFactor);

        //TODO might be useful in the standard minimap too
        if(fullView){
            drawSpawns();

            if(!mobile){
                //draw bounds for camera - not drawn on mobile because you can't shift it by tapping anyway
                Rect r = Core.camera.bounds(Tmp.r1);
                Lines.stroke(Scl.scl(3f) * scaleFactor);
                Draw.color(Pal.accent);
                Lines.rect(r.x, r.y, r.width, r.height);
                Draw.reset();
            }
        }

        LongSeq indicators = control.indicators.list();
        float fin = ((Time.globalTime / 30f) % 1f);
        float rad = fin * 5f + tilesize - 2f;
        Lines.stroke(Scl.scl((1f - fin) * 4f + 0.5f));

        for(int i = 0; i < indicators.size; i++){
            long ind = indicators.items[i];
            int
                pos = Indicator.pos(ind),
                ix = Point2.x(pos),
                iy = Point2.y(pos);
            float time = Indicator.time(ind), offset = 0f;

            //fix multiblock offset - this is suboptimal
            Building build = world.build(pos);
            if(build != null){
                offset = build.block.offset / tilesize;
            }

            Draw.color(Color.orange, Color.scarlet, Mathf.clamp(time / 70f));

            Lines.square((ix + 0.5f + offset) * tilesize, (iy + 0.5f + offset) * tilesize, rad);
        }

        Draw.reset();

        //TODO autoscale markers
        state.rules.objectives.eachRunning(obj -> {
            for(var marker : obj.markers){
                if(marker.minimap){
                    marker.draw(1);
                }
            }
        });

        for(var marker : state.markers){
            if(marker.minimap){
                marker.draw(1);
            }
        }

        Draw.trans(Tmp.m2);
    }

    private void markSc2ResourceMarkersDirty(){
        sc2ResourceMarkersDirty = true;
    }

    private void ensureSc2ResourceMarkers(){
        if(!sc2ResourceMarkersDirty || world.tiles == null) return;

        sc2ResourceMarkersDirty = false;
        sc2MineralMarkers.clear();
        sc2GasMarkers.clear();
        sc2DerelictScrapCenters.clear();

        IntSet derelictVisited = new IntSet();
        IntSeq derelictQueue = new IntSeq();

        for(Tile tile : world.tiles){
            if(tile == null) continue;

            if(tile.block() instanceof CrystalMineralWall){
                sc2MineralMarkers.add(tile.pos());
            }

            if(tile.floor() instanceof SteamVent vent && vent.isCenterVent(tile)){
                sc2GasMarkers.add(tile.pos());
            }

            if(tile.block() == Blocks.scrapWall && tile.team() == Team.derelict && derelictVisited.add(tile.pos())){
                derelictQueue.clear();
                derelictQueue.add(tile.pos());

                float sumx = 0f, sumy = 0f;
                int count = 0;

                while(derelictQueue.size > 0){
                    Tile next = world.tile(derelictQueue.pop());
                    if(next == null || next.block() != Blocks.scrapWall || next.team() != Team.derelict) continue;

                    sumx += next.worldx();
                    sumy += next.worldy();
                    count++;

                    for(Point2 point : Geometry.d4){
                        Tile nearby = world.tile(next.x + point.x, next.y + point.y);
                        if(nearby != null && nearby.block() == Blocks.scrapWall && nearby.team() == Team.derelict && derelictVisited.add(nearby.pos())){
                            derelictQueue.add(nearby.pos());
                        }
                    }
                }

                if(count > 0){
                    sc2DerelictScrapCenters.add(sumx / count, sumy / count);
                }
            }
        }
    }

    private void drawSc2ResourceMarkers(float scaleFactor){
        if(sc2MineralMarkers.isEmpty() && sc2GasMarkers.isEmpty() && sc2DerelictScrapCenters.isEmpty()) return;

        float stroke = Scl.scl(sc2ResourceMarkerStroke) * scaleFactor;
        float mineralHalf = sc2MineralMarkerSize / 2f;
        float gasHalf = sc2GasMarkerSize / 2f;

        Draw.color(sc2ResourceMarkerColor, 0.18f);
        for(int i = 0; i < sc2MineralMarkers.size; i++){
            Tile tile = world.tile(sc2MineralMarkers.get(i));
            if(tile == null) continue;
            Fill.crect(tile.worldx() - mineralHalf, tile.worldy() - mineralHalf, sc2MineralMarkerSize, sc2MineralMarkerSize);
        }
        for(int i = 0; i < sc2GasMarkers.size; i++){
            Tile tile = world.tile(sc2GasMarkers.get(i));
            if(tile == null) continue;
            Fill.crect(tile.worldx() - gasHalf, tile.worldy() - gasHalf, sc2GasMarkerSize, sc2GasMarkerSize);
        }

        Draw.color(sc2ResourceMarkerColor);
        Lines.stroke(stroke);
        for(int i = 0; i < sc2MineralMarkers.size; i++){
            Tile tile = world.tile(sc2MineralMarkers.get(i));
            if(tile == null) continue;
            Lines.rect(tile.worldx() - mineralHalf, tile.worldy() - mineralHalf, sc2MineralMarkerSize, sc2MineralMarkerSize);
        }
        for(int i = 0; i < sc2GasMarkers.size; i++){
            Tile tile = world.tile(sc2GasMarkers.get(i));
            if(tile == null) continue;
            Lines.rect(tile.worldx() - gasHalf, tile.worldy() - gasHalf, sc2GasMarkerSize, sc2GasMarkerSize);
        }

        TextureRegion derelictLogo = Core.atlas.find("team-derelict", Icon.warning.getRegion());
        float derelictLogoHeight = sc2DerelictLogoSize * (float)derelictLogo.height / Math.max(1f, derelictLogo.width);
        Draw.color(sc2DerelictLogoColor);
        for(int i = 0; i < sc2DerelictScrapCenters.size; i += 2){
            Draw.rect(derelictLogo, sc2DerelictScrapCenters.get(i), sc2DerelictScrapCenters.get(i + 1), sc2DerelictLogoSize, derelictLogoHeight);
        }
        Draw.reset();
    }

    public void drawSpawns(){
        if(!state.rules.showSpawns || !state.hasSpawns() || !state.rules.waves) return;

        TextureRegion icon = Icon.units.getRegion();

        Lines.stroke(Scl.scl(3f));

        Draw.color(state.rules.waveTeam.color, Tmp.c2.set(state.rules.waveTeam.color).value(1.2f), Mathf.absin(Time.time, 16f, 1f));

        float rad = state.rules.dropZoneRadius;
        float curve = Mathf.curve(Time.time % 240f, 120f, 240f);

        for(Tile tile : spawner.getSpawns()){
            float tx = tile.worldx();
            float ty = tile.worldy();

            Draw.rect(icon, tx, ty, icon.width, icon.height);
            Lines.circle(tx, ty, rad);
            if(curve > 0f) Lines.circle(tx, ty, rad * Interp.pow3Out.apply(curve));
        }

        Draw.reset();
    }

    public @Nullable TextureRegion getRegion(){
        if(texture == null) return null;

        float sz = Mathf.clamp(baseSize * zoom, baseSize, Math.min(world.width(), world.height()));
        float dx = (Core.camera.position.x / tilesize);
        float dy = (Core.camera.position.y / tilesize);
        dx = Mathf.clamp(dx, sz, world.width() - sz);
        dy = Mathf.clamp(dy, sz, world.height() - sz);
        float invTexWidth = 1f / texture.width;
        float invTexHeight = 1f / texture.height;
        float x = dx - sz, y = world.height() - dy - sz, width = sz * 2, height = sz * 2;
        region.set(x * invTexWidth, y * invTexHeight, (x + width) * invTexWidth, (y + height) * invTexHeight);
        return region;
    }

    public void updateAll(){
        if(pixmap.isDisposed() || texture.isDisposed()) return;
        for(Tile tile : world.tiles){
            pixmap.set(tile.x, pixmap.height - 1 - tile.y, colorFor(tile));
        }
        texture.draw(pixmap);
    }

    public void update(Tile tile){
        if(world.isGenerating() || !state.isGame()) return;

        if(tile.build != null && tile.isCenter()){
            tile.getLinkedTiles(other -> {
                if(!other.isCenter()){
                    updatePixel(other);
                }

                if(tile.block().solid && other.y > 0){
                    Tile low = world.tile(other.x, other.y - 1);
                    if(!low.solid()){
                        updatePixel(low);
                    }
                }
            });
        }

        updatePixel(tile);
    }

    public void updatePixel(Tile tile){
        updates.add(tile.pos());
    }

    public void updateUnitArray(){
        float sz = baseSize * zoom;
        float dx = (Core.camera.position.x / tilesize);
        float dy = (Core.camera.position.y / tilesize);
        dx = Mathf.clamp(dx, sz, world.width() - sz);
        dy = Mathf.clamp(dy, sz, world.height() - sz);

        units.clear();
        Units.nearby((dx - sz) * tilesize, (dy - sz) * tilesize, sz * 2 * tilesize, sz * 2 * tilesize, units::add);
    }

    private Block realBlock(Tile tile){
        //TODO doesn't work properly until player goes and looks at block
        return tile.build == null ? tile.block() : state.rules.fog && !tile.build.wasVisible ? Blocks.air : tile.block();
    }

    private int colorFor(Tile tile){
        if(tile == null) return 0;
        Block real = realBlock(tile);
        int bc = real.minimapColor(tile);
        if(bc == 0 && tile.block() == Blocks.air && tile.overlay() == Blocks.air) bc = tile.floor().minimapColor(tile);

        Color color = Tmp.c1.set(bc == 0 ? MapIO.colorFor(real, tile.floor(), tile.overlay(), tile.team()) : bc);
        color.mul(1f - Mathf.clamp(world.getDarkness(tile.x, tile.y) / 4f));

        if(real == Blocks.air && tile.y < world.height() - 1 && realBlock(world.tile(tile.x, tile.y + 1)).solid){
            color.mul(0.7f);
        }else if(tile.floor().isLiquid && (tile.y >= world.height() - 1 || !world.tile(tile.x, tile.y + 1).floor().isLiquid)){
            color.mul(0.84f, 0.84f, 0.9f, 1f);
        }

        return color.rgba();
    }

    public void drawLabel(float x, float y, String text, Color color, float scaleFactor){
        Font font = Fonts.outline;
        GlyphLayout l = Pools.obtain(GlyphLayout.class, GlyphLayout::new);
        boolean ints = font.usesIntegerPositions();
        font.getData().setScale(1 / 1.25f / Scl.scl(1f) * scaleFactor * 1f);
        font.setUseIntegerPositions(false);

        l.setText(font, text, color, 90f * scaleFactor, Align.left, false);
        float yOffset = 20f;
        float margin = 3f * scaleFactor;

        Draw.color(0f, 0f, 0f, 0.2f);
        Fill.rect(x, y + yOffset - l.height/2f, l.width + margin, l.height + margin);
        Draw.color();
        font.setColor(color);
        font.draw(text, x - l.width/2f, y + yOffset, 90f * scaleFactor, Align.left, false);
        font.setUseIntegerPositions(ints);

        font.getData().setScale(1f);

        Pools.free(l);
    }
}
