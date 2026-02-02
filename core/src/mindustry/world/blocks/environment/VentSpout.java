package mindustry.world.blocks.environment;

import arc.*;
import arc.graphics.g2d.*;
import mindustry.world.*;
import mindustry.world.meta.*;

public class VentSpout extends Block{
    public VentSpout(String name){
        super(name);
        solid = true;
        update = false;
        destructible = false;
        breakable = false;
        alwaysReplace = true;
        replaceable = true;
        unitMoveBreakable = false;
        hasShadow = false;
        size = 1;
        targetable = false;
        inEditor = false;
        buildVisibility = BuildVisibility.hidden;
        generateIcons = false;
        allowRectanglePlacement = false;
    }

    @Override
    public void drawBase(Tile tile){
        // Intentionally no draw; vent floor handles visuals.
    }

    @Override
    public void load(){
        super.load();
        region = Core.atlas.find("clear");
        fullIcon = uiIcon = region;
    }

    @Override
    public boolean isHidden(){
        return true;
    }
}
