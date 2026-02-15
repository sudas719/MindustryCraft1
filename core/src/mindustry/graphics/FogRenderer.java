package mindustry.graphics;

import arc.*;
import arc.graphics.*;
import arc.graphics.Texture.*;
import arc.graphics.g2d.*;
import arc.graphics.gl.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.core.*;
import mindustry.game.EventType.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.world.*;
import mindustry.world.HeightLayerData;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public final class FogRenderer{
    private FrameBuffer staticFog = new FrameBuffer(), dynamicFog = new FrameBuffer();
    private LongSeq events = new LongSeq();
    private Rect rect = new Rect();
    private @Nullable Team lastTeam;

    public FogRenderer(){
        Events.on(WorldLoadEvent.class, event -> {
            lastTeam = null;
            events.clear();
        });
    }

    public void handleEvent(long event){
        events.add(event);
    }

    public Texture getStaticTexture(){
        return staticFog.getTexture();
    }

    public Texture getDynamicTexture(){
        return dynamicFog.getTexture();
    }

    public void drawFog(){
        //there is no fog.
        if(fogControl.getDiscovered(player.team()) == null) return;

        //resize if world size changes
        boolean clearStatic = staticFog.resizeCheck(world.width(), world.height());

        dynamicFog.resize(world.width(), world.height());

        if(state.rules.staticFog && player.team() != lastTeam){
            copyFromCpu();
            lastTeam = player.team();
            clearStatic = false;
        }

        //draw dynamic fog every frame
        {
            Draw.proj(0, 0, staticFog.getWidth() * tilesize, staticFog.getHeight() * tilesize);
            dynamicFog.begin(Color.black);
            ScissorStack.push(rect.set(1, 1, staticFog.getWidth() - 2, staticFog.getHeight() - 2));

            Team team = player.team();

            for(var build : indexer.getFlagged(team, BlockFlag.hasFogRadius)){
                revealDynamic(build.x, build.y, build.fogRadius(), false, HeightLayerData.edgeLayer(build.x, build.y, build.hitSize() / 2f));
            }

            for(var unit : team.data().units){
                revealDynamic(unit.x, unit.y, unit.type.fogRadius, unit.isFlying(), HeightLayerData.edgeLayer(unit.x, unit.y, unit.hitSize / 2f));
            }

            dynamicFog.end();
            ScissorStack.pop();
            Draw.proj(Core.camera);
        }

        //grab static events
        if(state.rules.staticFog && (clearStatic || events.size > 0)){
            //set projection to whole map
            Draw.proj(0, 0, staticFog.getWidth(), staticFog.getHeight());

            //if the buffer resized, it contains garbage now, clearStatic it.
            if(clearStatic){
                staticFog.begin(Color.black);
            }else{
                staticFog.begin();
            }

            ScissorStack.push(rect.set(1, 1, staticFog.getWidth() - 2, staticFog.getHeight() - 2));

            Draw.color(Color.white);

            //process new static fog events
            for(int i = 0; i < events.size; i++){
                renderEvent(events.items[i]);
            }
            events.clear();

            staticFog.end();
            ScissorStack.pop();
            Draw.proj(Core.camera);
        }

        if(state.rules.staticFog){
            staticFog.getTexture().setFilter(TextureFilter.linear);
        }
        dynamicFog.getTexture().setFilter(TextureFilter.linear);

        Draw.shader(Shaders.fog);
        Draw.color(state.rules.dynamicColor, Float.isNaN(state.rules.dynamicColor.a) ? 0.5f : Math.max(0.5f, state.rules.dynamicColor.a));
        Draw.fbo(dynamicFog.getTexture(), world.width(), world.height(), tilesize);
        //TODO ai check?
        if(state.rules.staticFog){
            //TODO why does this require a half-tile offset while dynamic does not
            Draw.color(state.rules.staticColor, 1f);
            Draw.fbo(staticFog.getTexture(), world.width(), world.height(), tilesize, tilesize/2f);
        }
        Draw.shader();
    }

    void renderEvent(long e){
        Tile tile = world.tile(FogEvent.x(e), FogEvent.y(e));
        int encodedRadius = FogEvent.radius(e);
        int radius = FogControl.decodeFogRadius(encodedRadius);
        boolean ignoreHeight = FogControl.decodeFogIgnoreHeight(encodedRadius);
        if(radius <= 0) return;

        if(ignoreHeight){
            float o = 0f;
            //visual offset for uneven blocks; this is not reflected on the CPU, but it doesn't really matter
            if(tile != null && tile.block().size % 2 == 0 && tile.isCenter()){
                o = 0.5f;
            }
            Fill.poly(FogEvent.x(e) + 0.5f + o, FogEvent.y(e) + 0.5f + o, 20, radius + 0.3f);
            return;
        }

        int viewerHeight = FogControl.decodeFogViewerHeight(encodedRadius);
        revealStaticByHeight(FogEvent.x(e), FogEvent.y(e), radius, viewerHeight);
    }

    void revealStaticByHeight(int cx, int cy, int radius, int viewerHeight){
        float revealRadius = radius + 0.3f;
        float radius2 = revealRadius * revealRadius;
        int minx = Math.max(1, cx - radius), maxx = Math.min(world.width() - 2, cx + radius);
        int miny = Math.max(1, cy - radius), maxy = Math.min(world.height() - 2, cy + radius);

        for(int x = minx; x <= maxx; x++){
            float dx = x + 0.5f - (cx + 0.5f);
            for(int y = miny; y <= maxy; y++){
                float dy = y + 0.5f - (cy + 0.5f);
                if(dx * dx + dy * dy > radius2) continue;
                if(!FogControl.hasVisionPath(cx, cy, x, y, viewerHeight)) continue;

                Tile tile = world.tile(x, y);
                if(HeightLayerData.fogLayer(tile) <= viewerHeight){
                    Fill.rect(x + 0.5f, y + 0.5f, 1f, 1f);
                }
            }
        }
    }

    void revealDynamic(float wx, float wy, float radiusTiles, boolean ignoreHeight, int viewerHeight){
        if(radiusTiles <= 0f) return;
        if(ignoreHeight){
            poly(wx, wy, radiusTiles * tilesize);
            return;
        }

        float radiusWorld = radiusTiles * tilesize;
        float radius2 = radiusWorld * radiusWorld;
        int sourceX = Math.max(0, Math.min(world.width() - 1, World.toTile(wx)));
        int sourceY = Math.max(0, Math.min(world.height() - 1, World.toTile(wy)));
        int minx = Math.max(0, World.toTile(wx - radiusWorld)), maxx = Math.min(world.width() - 1, World.toTile(wx + radiusWorld));
        int miny = Math.max(0, World.toTile(wy - radiusWorld)), maxy = Math.min(world.height() - 1, World.toTile(wy + radiusWorld));

        for(int x = minx; x <= maxx; x++){
            float tx = x * tilesize;
            float dx = tx - wx;
            for(int y = miny; y <= maxy; y++){
                float ty = y * tilesize;
                float dy = ty - wy;
                if(dx * dx + dy * dy > radius2) continue;
                if(!FogControl.hasVisionPath(sourceX, sourceY, x, y, viewerHeight)) continue;

                Tile tile = world.tile(x, y);
                if(HeightLayerData.fogLayer(tile) <= viewerHeight){
                    Fill.rect(tx, ty, tilesize, tilesize);
                }
            }
        }
    }

    void poly(float x, float y, float rad){
        Fill.poly(x, y, 20, rad);
    }

    public void copyFromCpu(){
        staticFog.resize(world.width(), world.height());
        staticFog.begin(Color.black);
        Draw.proj(0, 0, staticFog.getWidth(), staticFog.getHeight());
        Draw.color();
        int ww = world.width(), wh = world.height();

        var data = fogControl.getDiscovered(player.team());
        int len = world.width() * world.height();
        if(data != null){
            for(int i = 0; i < len; i++){
                if(data.get(i)){
                    //TODO slow, could do scanlines instead at the very least.
                    int x = i % ww, y = i / ww;

                    //manually clip with 1 pixel of padding so the borders are never fully revealed
                    if(x > 0 && y > 0 && x < ww - 1 && y < wh - 1){
                        Fill.rect(x + 0.5f, y + 0.5f, 1f, 1f);
                    }
                }
            }
        }

        staticFog.end();
        Draw.proj(Core.camera);
    }
}
