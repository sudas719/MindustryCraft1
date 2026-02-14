package mindustry.world.blocks.environment;

import arc.*;
import arc.graphics.g2d.*;
import mindustry.world.*;
import mindustry.world.meta.*;

public class CliffLayerMarker extends OverlayFloor{
    public final int cliffValue;
    public final String spriteName;

    public CliffLayerMarker(String name, String spriteName, int cliffValue){
        super(name);
        this.spriteName = spriteName;
        this.cliffValue = cliffValue;

        inEditor = true;
        buildVisibility = BuildVisibility.hidden;
        variants = 0;
        saveData = false;
        saveConfig = false;
        editorConfigurable = false;
        rotate = false;
        drawArrow = false;
    }

    @Override
    public void drawBase(Tile tile){
        //Editor marker only; drawn by editor overlay.
    }

    @Override
    public void load(){
        super.load();

        TextureRegion found = Core.atlas.find(spriteName);
        if(!found.found()) found = Core.atlas.find(spriteName.toLowerCase(), found);
        if(!found.found()) found = Core.atlas.find("cliff-" + spriteName, found);
        if(found.found()){
            region = found;
            fullIcon = found;
            uiIcon = found;
        }
    }
}

