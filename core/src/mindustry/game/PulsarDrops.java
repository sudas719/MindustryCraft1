package mindustry.game;

import arc.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;

public class PulsarDrops{
    public static final float lifetime = 64f * 60f;

    private static final IntFloatMap expires = new IntFloatMap();

    static{
        Events.on(ResetEvent.class, e -> expires.clear());
    }

    public static void register(Unit unit){
        if(unit == null) return;
        expires.put(unit.id, Time.time + lifetime);
    }

    public static void remove(Unit unit){
        if(unit == null) return;
        expires.remove(unit.id, 0f);
    }

    public static float remainingFraction(Unit unit){
        if(unit == null || !unit.isValid()){
            remove(unit);
            return 0f;
        }
        float end = expires.get(unit.id, -1f);
        if(end < 0f) return 0f;
        float remaining = (end - Time.time) / lifetime;
        if(remaining <= 0f){
            remove(unit);
            return 0f;
        }
        return Mathf.clamp(remaining);
    }
}
