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
                boolean drawLocalCameraBox = !(spectator && ui.hudfrag.isSpectatorCameraAll());

                if(drawLocalCameraBox){
                    //Draw camera view box
                    var region = renderer.minimap.getRegion();
                    if(region != null){
                        //Calculate camera bounds in world coordinates
                        float camWidth = Core.camera.width;
                        float camHeight = Core.camera.height;
                        float camX = Core.camera.position.x;
                        float camY = Core.camera.position.y;

                        //Convert to minimap coordinates
                        float worldWidth = world.width() * tilesize;
                        float worldHeight = world.height() * tilesize;

                        //Camera bounds in world space
                        float camLeft = camX - camWidth / 2f;
                        float camRight = camX + camWidth / 2f;
                        float camBottom = camY - camHeight / 2f;
                        float camTop = camY + camHeight / 2f;

                        //Normalize to 0-1 range
                        float normLeft = Mathf.clamp(camLeft / worldWidth, 0f, 1f);
                        float normRight = Mathf.clamp(camRight / worldWidth, 0f, 1f);
                        float normBottom = Mathf.clamp(camBottom / worldHeight, 0f, 1f);
                        float normTop = Mathf.clamp(camTop / worldHeight, 0f, 1f);

                        //Convert to minimap pixel coordinates
                        float boxLeft = x + normLeft * width;
                        float boxRight = x + normRight * width;
                        float boxBottom = y + normBottom * height;
                        float boxTop = y + normTop * height;

                        //Draw the camera view box
                        Lines.stroke(2f);
                        Draw.color(Color.white, 0.8f);
                        Lines.rect(boxLeft, boxBottom, boxRight - boxLeft, boxTop - boxBottom);
                        Draw.reset();
                    }
                }

                if(spectator){
                    drawSpectatorViewBoxes(x, y, width, height);
                }

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

        if(ui.hudfrag.isSpectatorCameraAll()){
            for(Player other : Groups.player){
                if(other == null || other.team() == null) continue;
                if(HudFragment.isObserverPlayer(other)) continue;
                drawPlayerViewBox(mapX, mapY, mapW, mapH, other, other.team().color, 1.6f, 0.82f);
            }
        }else{
            Player target = Groups.player.getByID(ui.hudfrag.getSpectatorCameraFocusPlayer());
            if(target == null || target.team() == null) return;
            Color color = HudFragment.isObserverPlayer(target) ? Color.white : target.team().color;
            drawPlayerViewBox(mapX, mapY, mapW, mapH, target, color, 2f, 0.9f);
        }
    }

    private void drawPlayerViewBox(float mapX, float mapY, float mapW, float mapH, Player target, Color color, float stroke, float alpha){
        float worldW = world.width() * tilesize;
        float worldH = world.height() * tilesize;
        if(worldW <= 0f || worldH <= 0f) return;

        float cx, cy;
        if(!target.dead() && target.unit() != null && target.unit().isValid()){
            cx = target.unit().x;
            cy = target.unit().y;
        }else{
            cx = target.x;
            cy = target.y;
        }

        float camW = Core.camera.width;
        float camH = Core.camera.height;
        float left = Mathf.clamp((cx - camW / 2f) / worldW, 0f, 1f);
        float right = Mathf.clamp((cx + camW / 2f) / worldW, 0f, 1f);
        float bottom = Mathf.clamp((cy - camH / 2f) / worldH, 0f, 1f);
        float top = Mathf.clamp((cy + camH / 2f) / worldH, 0f, 1f);

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

    public void setMinimapSize(float size){
        if(currentSize != size){
            currentSize = size;
            mapElement.setSize(size, size);
            getCell(mapElement).size(size, size);
            invalidate();
        }
    }
}
