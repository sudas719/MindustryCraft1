package mindustry.ui;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.input.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.*;
import arc.scene.event.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.fragments.HudFragment;

import static mindustry.Vars.*;

public class Minimap extends Table{
    private Element mapElement;
    private float currentSize = 140f;

    public Minimap(){
        background(Tex.pane);
        float margin = 5f;
        this.touchable = Touchable.enabled;

        mapElement = new Element(){
            {
                setSize(Scl.scl(140f));

                addListener(new ClickListener(KeyCode.mouseRight){
                    @Override
                    public void clicked(InputEvent event, float cx, float cy){
                        var region = renderer.minimap.getRegion();
                        if(region == null) return;

                        float
                        sx = (cx - x) / width,
                        sy = (cy - y) / height,
                        scaledX = Mathf.lerp(region.u, region.u2, sx) * world.width() * tilesize,
                        scaledY = Mathf.lerp(1f - region.v2, 1f - region.v, sy) * world.height() * tilesize;

                        //Check if units are selected
                        if(control.input.selectedUnits.size > 0 || control.input.commandBuildings.size > 0){
                            //Issue movement command to units
                            //Convert world coordinates back to screen coordinates for commandTap
                            Vec2 screenPos = Core.camera.project(scaledX, scaledY);
                            if(control.input instanceof mindustry.input.DesktopInput desktopInput){
                                desktopInput.commandTap(screenPos.x, screenPos.y);
                            }
                        }else{
                            //No units selected, pan camera
                            control.input.panCamera(Tmp.v1.set(scaledX, scaledY));
                        }
                    }
                });
            }

            @Override
            public void act(float delta){
                setPosition(Scl.scl(margin), Scl.scl(margin));

                super.act(delta);
            }

            @Override
            public void draw(){
                if(renderer.minimap.getRegion() == null) return;
                if(!clipBegin()) return;

                Draw.rect(renderer.minimap.getRegion(), x + width / 2f, y + height / 2f, width, height);

                if(renderer.minimap.getTexture() != null){
                    Draw.alpha(parentAlpha);
                    renderer.minimap.drawEntities(x, y, width, height, false);
                }

                boolean spectator = HudFragment.isLocalPlayerSpectator() && ui.hudfrag != null;
                if(spectator){
                    drawSpectatorViewBoxes(x, y, width, height);
                }
                drawLocalCameraBox(x, y, width, height);

                clipEnd();
            }
        };

        add(mapElement).size(140f);

        margin(margin);

        //Mouse wheel scrolling removed for RTS mode
        //addListener(new InputListener(){
        //    @Override
        //    public boolean scrolled(InputEvent event, float x, float y, float amountx, float amounty){
        //        renderer.minimap.zoomBy(amounty);
        //        return true;
        //    }
        //});

        addListener(new ClickListener(){
            {
                tapSquareSize = Scl.scl(11f);
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button){
                if(inTapSquare()){
                    super.touchUp(event, x, y, pointer, button);
                }else{
                    pressed = false;
                    pressedPointer = -1;
                    pressedButton = null;
                    cancelled = false;
                }
            }

            @Override
            public void touchDragged(InputEvent event, float x, float y, int pointer){
                if(!inTapSquare(x, y)){
                    invalidateTapSquare();
                }
                super.touchDragged(event, x, y, pointer);

                //Drag to move camera
                var region = renderer.minimap.getRegion();
                if(region == null) return;

                float
                sx = (x - mapElement.x) / mapElement.getWidth(),
                sy = (y - mapElement.y) / mapElement.getHeight(),
                scaledX = Mathf.lerp(region.u, region.u2, sx) * world.width() * tilesize,
                scaledY = Mathf.lerp(1f - region.v2, 1f - region.v, sy) * world.height() * tilesize;

                Core.camera.position.set(scaledX, scaledY);
            }

            @Override
            public void clicked(InputEvent event, float x, float y){
                //Left-click: check if in attack command mode, otherwise pan camera
                var region = renderer.minimap.getRegion();
                if(region == null) return;

                float
                sx = (x - mapElement.x) / mapElement.getWidth(),
                sy = (y - mapElement.y) / mapElement.getHeight(),
                scaledX = Mathf.lerp(region.u, region.u2, sx) * world.width() * tilesize,
                scaledY = Mathf.lerp(1f - region.v2, 1f - region.v, sy) * world.height() * tilesize;

                //Check if in attack command mode
                if(ui.hudfrag.abilityPanel != null &&
                   ui.hudfrag.abilityPanel.activeCommand != mindustry.ui.UnitAbilityPanel.CommandMode.NONE){
                    //Execute attack command at minimap location
                    if(control.input instanceof mindustry.input.DesktopInput desktopInput){
                        Vec2 screenPos = Core.camera.project(scaledX, scaledY);
                        desktopInput.executeActiveCommand(screenPos.x, screenPos.y);
                    }
                }else{
                    //Normal left-click: pan camera
                    Core.camera.position.set(scaledX, scaledY);
                }
            }
        });

        update(() -> {

            Element e = Core.scene.getHoverElement();
            if(e != null && e.isDescendantOf(this)){
                requestScroll();
            }else if(hasScroll()){
                Core.scene.setScrollFocus(null);
            }
        });
    }

    private void drawSpectatorViewBoxes(float mapX, float mapY, float mapW, float mapH){
        if(ui.hudfrag == null) return;

        Player focus = ui.hudfrag.spectatorCameraFocusedPlayer();
        int focusId = focus == null ? -1 : focus.id;

        for(Player other : Groups.player){
            if(other == null || other.team() == null) continue;
            if(HudFragment.isObserverPlayer(other)) continue;

            boolean highlighted = other.id == focusId;
            drawPlayerViewBox(mapX, mapY, mapW, mapH, other, TeamDisplayColors.playerDisplayColor(other), highlighted ? 2f : 1.6f, highlighted ? 0.9f : 0.82f);
        }

        if(focus != null && focus != player && HudFragment.isObserverPlayer(focus)){
            drawPlayerViewBox(mapX, mapY, mapW, mapH, focus, TeamDisplayColors.playerDisplayColor(focus), 2.8f, 0.72f);
        }
    }

    private void drawPlayerViewBox(float mapX, float mapY, float mapW, float mapH, Player target, Color color, float stroke, float alpha){
        float worldW = world.width() * tilesize;
        float worldH = world.height() * tilesize;
        if(worldW <= 0f || worldH <= 0f) return;

        if(!resolvePlayerView(target, Tmp.r1)) return;

        float left = Mathf.clamp(Tmp.r1.x / worldW, 0f, 1f);
        float right = Mathf.clamp((Tmp.r1.x + Tmp.r1.width) / worldW, 0f, 1f);
        float bottom = Mathf.clamp(Tmp.r1.y / worldH, 0f, 1f);
        float top = Mathf.clamp((Tmp.r1.y + Tmp.r1.height) / worldH, 0f, 1f);

        float boxLeft = mapX + left * mapW;
        float boxRight = mapX + right * mapW;
        float boxBottom = mapY + bottom * mapH;
        float boxTop = mapY + top * mapH;
        float boxW = Math.max(2f, boxRight - boxLeft);
        float boxH = Math.max(2f, boxTop - boxBottom);

        Lines.stroke(stroke);
        Draw.color(color, alpha);
        Lines.rect(boxLeft, boxBottom, boxW, boxH);
        Draw.reset();
    }

    private void drawLocalCameraBox(float mapX, float mapY, float mapW, float mapH){
        drawViewBox(mapX, mapY, mapW, mapH, Core.camera.position.x, Core.camera.position.y, Core.camera.width, Core.camera.height, Color.white, 2f, 0.8f);
    }

    private boolean resolvePlayerView(Player target, Rect out){
        if(target == null) return false;

        float viewX, viewY, viewW, viewH;
        if(target.isLocal() && !headless){
            viewX = Core.camera.position.x;
            viewY = Core.camera.position.y;
            viewW = Core.camera.width;
            viewH = Core.camera.height;
        }else if(control.input != null && control.input.hasRemoteSpectatorCameraState(target.id)){
            viewX = control.input.remoteSpectatorCameraX();
            viewY = control.input.remoteSpectatorCameraY();
            viewW = control.input.remoteSpectatorCameraWidth();
            viewH = control.input.remoteSpectatorCameraHeight();
        }else if(net.server() && target.con != null && target.con.viewWidth > 0f && target.con.viewHeight > 0f){
            viewX = target.con.viewX;
            viewY = target.con.viewY;
            viewW = target.con.viewWidth;
            viewH = target.con.viewHeight;
        }else{
            if(!target.dead() && target.unit() != null && target.unit().isValid()){
                viewX = target.unit().x;
                viewY = target.unit().y;
            }else{
                viewX = target.x;
                viewY = target.y;
            }

            viewW = Core.camera.width;
            viewH = Core.camera.height;
        }

        if(!Float.isFinite(viewX) || !Float.isFinite(viewY) || !Float.isFinite(viewW) || !Float.isFinite(viewH) || viewW <= 0f || viewH <= 0f){
            return false;
        }

        out.setCentered(viewX, viewY, viewW, viewH);
        return true;
    }

    private void drawViewBox(float mapX, float mapY, float mapW, float mapH, float viewX, float viewY, float viewW, float viewH, Color color, float stroke, float alpha){
        float worldW = world.width() * tilesize;
        float worldH = world.height() * tilesize;
        if(worldW <= 0f || worldH <= 0f) return;
        if(!Float.isFinite(viewX) || !Float.isFinite(viewY) || !Float.isFinite(viewW) || !Float.isFinite(viewH) || viewW <= 0f || viewH <= 0f) return;

        float left = Mathf.clamp((viewX - viewW / 2f) / worldW, 0f, 1f);
        float right = Mathf.clamp((viewX + viewW / 2f) / worldW, 0f, 1f);
        float bottom = Mathf.clamp((viewY - viewH / 2f) / worldH, 0f, 1f);
        float top = Mathf.clamp((viewY + viewH / 2f) / worldH, 0f, 1f);

        float boxLeft = mapX + left * mapW;
        float boxRight = mapX + right * mapW;
        float boxBottom = mapY + bottom * mapH;
        float boxTop = mapY + top * mapH;

        Lines.stroke(stroke);
        Draw.color(color, alpha);
        Lines.rect(boxLeft, boxBottom, Math.max(2f, boxRight - boxLeft), Math.max(2f, boxTop - boxBottom));
        Draw.reset();
    }

    public void setMinimapSize(float size){
        if(currentSize != size){
            currentSize = size;
            mapElement.setSize(size, size);
            getCell(mapElement).size(size, size);
            invalidate();
        }
    }
}
