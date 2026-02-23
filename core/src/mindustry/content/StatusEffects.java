package mindustry.content;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.game.EventType.*;
import mindustry.game.*;
import mindustry.graphics.*;
import mindustry.type.*;

import static mindustry.Vars.*;

public class StatusEffects{
    public static StatusEffect none, burning, freezing, unmoving, slow, fast, wet, muddy, melting, sapped, tarred, overdrive, overclock, shielded, shocked, blasted, corroded, boss, sporeSlowed, disarmed, electrified, invincible, dynamic,
    widowBurrowing, widowBuried, widowUnburrowing, widowReloading,
    preceptSieging, preceptSieged, preceptUnsieging, scepterSwitching,
    liberatorDeploying, liberatorDefending, liberatorUndeploying,
    medivacAfterburner, ravenAntiArmor, ravenMatrixLock, ravenTurretLifetime, bansheeCloak;

    public static void load(){

        none = new StatusEffect("none");

        burning = new StatusEffect("burning"){{
            color = Color.valueOf("ffc455");
            damage = 0.167f;
            effect = Fx.burning;
            transitionDamage = 8f;

            init(() -> {
                opposite(wet, freezing);
                affinity(tarred, (unit, result, time) -> {
                    unit.damagePierce(transitionDamage);
                    Fx.burning.at(unit.x + Mathf.range(unit.bounds() / 2f), unit.y + Mathf.range(unit.bounds() / 2f));
                    result.set(burning, Math.min(time + result.time, 300f));
                });
            });
        }};

        freezing = new StatusEffect("freezing"){{
            color = Color.valueOf("6ecdec");
            speedMultiplier = 0.6f;
            healthMultiplier = 0.8f;
            effect = Fx.freezing;
            transitionDamage = 18f;

            init(() -> {
                opposite(melting, burning);

                affinity(blasted, (unit, result, time) -> {
                    unit.damagePierce(transitionDamage);
                    if(unit.team == state.rules.waveTeam){
                        Events.fire(Trigger.blastFreeze);
                    }
                });
            });
        }};

        unmoving = new StatusEffect("unmoving"){{
            color = Pal.gray;
            speedMultiplier = 0f;
        }};

        widowBurrowing = new StatusEffect("widow-burrowing"){{
            show = false;
            color = Color.cyan;
            speedMultiplier = 0f;
            permanent = true;
        }};

        widowBuried = new StatusEffect("widow-buried"){{
            show = false;
            color = Color.valueOf("5c7a7a");
            speedMultiplier = 0f;
            permanent = true;
        }};

        widowUnburrowing = new StatusEffect("widow-unburrowing"){{
            show = false;
            color = Color.valueOf("7fb8ff");
            speedMultiplier = 0f;
            permanent = true;
        }};

        widowReloading = new StatusEffect("widow-reloading"){{
            show = false;
            color = Color.gray;
        }};

        preceptSieging = new StatusEffect("precept-sieging"){{
            show = false;
            color = Color.valueOf("ffb26b");
            speedMultiplier = 0f;
            disarm = true;
            permanent = true;
        }};

        preceptSieged = new StatusEffect("precept-sieged"){{
            show = false;
            color = Color.valueOf("ff8e4d");
            speedMultiplier = 0f;
            permanent = true;
        }};

        preceptUnsieging = new StatusEffect("precept-unsieging"){{
            show = false;
            color = Color.valueOf("8bc2ff");
            speedMultiplier = 0f;
            disarm = true;
            permanent = true;
        }};

        scepterSwitching = new StatusEffect("scepter-switching"){{
            show = false;
            color = Color.valueOf("ffcc7a");
            speedMultiplier = 0f;
            disarm = true;
            permanent = true;
        }};

        liberatorDeploying = new StatusEffect("liberator-deploying"){{
            show = false;
            color = Color.valueOf("ff6d6d");
            speedMultiplier = 0f;
            disarm = true;
            permanent = true;
        }};

        liberatorDefending = new StatusEffect("liberator-defending"){{
            show = false;
            color = Color.valueOf("d0d0d0");
            speedMultiplier = 0f;
            permanent = true;
        }};

        liberatorUndeploying = new StatusEffect("liberator-undeploying"){{
            show = false;
            color = Color.valueOf("8bc2ff");
            speedMultiplier = 0f;
            disarm = true;
            permanent = true;
        }};

        medivacAfterburner = new StatusEffect("medivac-afterburner"){{
            show = false;
            color = Color.valueOf("5fd7ff");
            speedMultiplier = 1.6971428f; // 3.5 -> 5.94 tiles/s
        }};

        ravenAntiArmor = new StatusEffect("raven-anti-armor"){{
            show = false;
            color = Color.valueOf("5a0f12");
            armorOffset = -2f;
            effectChance = 0.07f;
            effect = Fx.sapped;
        }
        @Override
        public void draw(Unit unit){
            Draw.z(Layer.effect);
            Draw.color(0.45f, 0.08f, 0.1f, 0.25f);
            Fill.circle(unit.x, unit.y, unit.hitSize * 0.68f);
            Draw.reset();
        }};

        ravenMatrixLock = new StatusEffect("raven-matrix-lock"){{
            show = false;
            color = Color.valueOf("171717");
            speedMultiplier = 0f;
            reloadMultiplier = 0f;
            buildSpeedMultiplier = 0f;
            disarm = true;
            effectChance = 0.12f;
            effect = new Effect(16f, 80f, e -> {
                Draw.color(Color.valueOf("66b8ff"), Color.white, e.fin());
                Lines.stroke(1.2f * e.fout());
                for(int i = 0; i < 2; i++){
                    float angle = Mathf.randomSeed(e.id * 13L + i, 360f);
                    float len = 3f + Mathf.randomSeed(e.id * 29L + i, 3f);
                    Lines.lineAngle(e.x, e.y, angle, len, false);
                }
                Draw.reset();
            });
        }
        @Override
        public void draw(Unit unit){
            Draw.z(Layer.effect);
            Draw.color(0f, 0f, 0f, 0.35f);
            Fill.circle(unit.x, unit.y, unit.hitSize * 0.72f);
            Draw.reset();
        }};

        ravenTurretLifetime = new StatusEffect("raven-turret-lifetime"){{
            show = false;
            color = Color.gray;
        }};

        bansheeCloak = new StatusEffect("banshee-cloak"){{
            show = false;
            color = Color.valueOf("3d4c66");
            permanent = true;
        }};

        slow = new StatusEffect("slow"){{
            color = Pal.lightishGray;
            speedMultiplier = 0.4f;

            init(() -> opposite(fast));
        }};

        fast = new StatusEffect("fast"){{
            color = Pal.boostTo;
            speedMultiplier = 1.6f;

            init(() -> opposite(slow));
        }};

        wet = new StatusEffect("wet"){{
            color = Color.royal;
            speedMultiplier = 0.94f;
            effect = Fx.wet;
            effectChance = 0.09f;
            transitionDamage = 14;

            init(() -> {
                affinity(shocked, (unit, result, time) -> {
                    unit.damage(transitionDamage);

                    if(unit.team == state.rules.waveTeam){
                        Events.fire(Trigger.shock);
                    }
                });
                opposite(burning, melting);
            });
        }};

        muddy = new StatusEffect("muddy"){{
            color = Color.valueOf("46382a");
            speedMultiplier = 0.94f;
            effect = Fx.muddy;
            effectChance = 0.09f;
            show = false;
        }};

        melting = new StatusEffect("melting"){{
            color = Color.valueOf("ffa166");
            speedMultiplier = 0.8f;
            healthMultiplier = 0.8f;
            damage = 0.3f;
            effect = Fx.melting;

            init(() -> {
                opposite(wet, freezing);
                affinity(tarred, (unit, result, time) -> {
                    unit.damagePierce(8f);
                    Fx.burning.at(unit.x + Mathf.range(unit.bounds() / 2f), unit.y + Mathf.range(unit.bounds() / 2f));
                    result.set(melting, Math.min(time + result.time, 200f));
                });
            });
        }};

        sapped = new StatusEffect("sapped"){{
            color = Pal.sap;
            speedMultiplier = 0.7f;
            healthMultiplier = 0.8f;
            effect = Fx.sapped;
            effectChance = 0.1f;
        }};

        electrified = new StatusEffect("electrified"){{
            color = Pal.heal;
            speedMultiplier = 0.7f;
            reloadMultiplier = 0.6f;
            effect = Fx.electrified;
            effectChance = 0.1f;
        }};

        sporeSlowed = new StatusEffect("spore-slowed"){{
            color = Pal.spore;
            speedMultiplier = 0.8f;
            effect = Fx.sapped;
            effectChance = 0.04f;
        }};

        tarred = new StatusEffect("tarred"){{
            color = Color.valueOf("313131");
            speedMultiplier = 0.6f;
            effect = Fx.oily;

            init(() -> {
                affinity(melting, (unit, result, time) -> result.set(melting, result.time + time));
                affinity(burning, (unit, result, time) -> result.set(burning, result.time + time));
            });
        }};

        overdrive = new StatusEffect("overdrive"){{
            color = Pal.accent;
            healthMultiplier = 0.95f;
            speedMultiplier = 1.15f;
            damageMultiplier = 1.4f;
            damage = -0.01f;
            effect = Fx.overdriven;
            permanent = true;
        }};

        overclock = new StatusEffect("overclock"){{
            color = Pal.accent;
            speedMultiplier = 1.15f;
            damageMultiplier = 1.15f;
            reloadMultiplier = 1.25f;
            effectChance = 0.07f;
            effect = Fx.overclocked;
        }};

        shielded = new StatusEffect("shielded"){{
            color = Pal.accent;
            healthMultiplier = 3f;
        }};

        boss = new StatusEffect("boss"){{
            color = Team.crux.color;
            permanent = true;
            damageMultiplier = 1.3f;
            healthMultiplier = 1.5f;
        }};

        shocked = new StatusEffect("shocked"){{
            color = Pal.lancerLaser;
            reactive = true;
        }};

        blasted = new StatusEffect("blasted"){{
            color = Color.valueOf("ff795e");
            reactive = true;
        }};

        corroded = new StatusEffect("corroded"){{
            color = Color.valueOf("e4ffd6");
            intervalDamage = 25f;
            intervalDamageTime = 30f;

            effectChance = 0.1f;
            effect = Fx.corrosionVapor;
        }};

        disarmed = new StatusEffect("disarmed"){{
            color = Color.valueOf("e9ead3");
            disarm = true;
        }};

        invincible = new StatusEffect("invincible"){{
            healthMultiplier = Float.POSITIVE_INFINITY;
        }};

        dynamic = new StatusEffect("dynamic"){{
            show = false;
            dynamic = true;
            permanent = true;
        }};
    }
}
