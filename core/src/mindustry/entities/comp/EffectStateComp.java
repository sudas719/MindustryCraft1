package mindustry.entities.comp;

import arc.graphics.*;
import mindustry.annotations.Annotations.*;
import mindustry.entities.*;
import mindustry.game.*;
import mindustry.gen.*;

import static mindustry.Vars.*;

@EntityDef(value = {EffectStatec.class, Childc.class}, pooled = true, serialize = false)
@Component(base = true)
abstract class EffectStateComp implements Posc, Drawc, Timedc, Rotc, Childc{
    @Import float time, lifetime, rotation, x, y;
    @Import int id;

    Color color = new Color(Color.white);
    Effect effect;
    Object data;

    @Override
    public void draw(){
        Team viewer = ViewerPerspective.team();
        boolean spectatorView = net.active() && ViewerPerspective.isSpectatorTeam(viewer);
        if(state != null && state.rules != null && state.rules.fog && renderer != null && !spectatorView &&
            player != null && player.team() != null && fogControl != null && !fogControl.isVisible(player.team(), x, y)){
            return;
        }
        lifetime = effect.render(id, color, time, lifetime, rotation, x, y, data);
    }

    @Replace
    public float clipSize(){
        return effect.clip;
    }
}
