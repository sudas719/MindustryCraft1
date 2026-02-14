package mindustry.world.blocks.environment;

import arc.*;
import arc.graphics.g2d.*;
import mindustry.world.*;
import mindustry.world.meta.*;

public class HeightLayerMarker extends OverlayFloor{
    public final int layerValue;
    public final boolean slopeValue;
    public final boolean preserveLayer;
    public final String spriteName;

    public HeightLayerMarker(String name, String spriteName, int layerValue, boolean slopeValue, boolean preserveLayer){
        super(name);
        this.spriteName = spriteName;
        this.layerValue = layerValue;
        this.slopeValue = slopeValue;
        this.preserveLayer = preserveLayer;

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
        //Pure editor marker; game-world rendering is handled by editor overlay drawing.
    }

    @Override
    public void load(){
        super.load();

        TextureRegion found = Core.atlas.find(spriteName);
        if(!found.found()) found = Core.atlas.find("layer-" + spriteName, found);
        if(!found.found()) found = Core.atlas.find("layer/" + spriteName, found);
        if(found.found()){
            region = found;
            fullIcon = found;
            uiIcon = found;
        }
    }
}
