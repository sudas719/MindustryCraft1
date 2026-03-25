package mindustry.entities.bullet;

import arc.func.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.ai.types.CommandAI;
import mindustry.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.world.*;

public class PointBulletType extends BulletType{
     private static float cdist = 0f;
     private static Unit result;

     public float trailSpacing = 10f;

     public PointBulletType(){
         scaleLife = true;
         lifetime = 100f;
         collides = false;
         reflectable = false;
         keepVelocity = false;
     }

    private @Nullable Teamc forcedFriendlyTarget(Bullet b){
        if(!(b.owner instanceof Unit unit)) return null;

        if(unit.controller() instanceof CommandAI ai){
            Teamc forced = ai.attackTarget;
            if(forced != null && forced.team() == b.team){
                return forced;
            }
        }

        if(unit.controller() instanceof Player player){
            float mx = player.mouseX, my = player.mouseY;
            Building build = Vars.world.buildWorld(mx, my);
            if(build != null && build.team == b.team && Units.canTargetBuilding(collidesAir, collidesGround, build)){
                return build;
            }

            float range = Math.max(8f, unit.hitSize);
            Unit target = Units.closest(b.team, mx, my, range, u -> u != unit && u.isValid()
                && u.checkTarget(collidesAir, collidesGround)
                && u.within(mx, my, u.hitSize / 2f));
            if(target != null){
                return target;
            }
        }

        return null;
    }

    private boolean canHitUnit(Bullet b, Unit unit, @Nullable Teamc forced){
        if(unit == null || unit.dead() || !unit.isValid() || !unit.checkTarget(collidesAir, collidesGround) || !unit.hittable()) return false;
        return unit.team != b.team || forced == unit || collidesTeam;
    }

    private boolean canHitBuild(Bullet b, Building build, @Nullable Teamc forced){
        if(build == null || !build.isValid() || !build.within(b.x, b.y, build.hitSize() / 2f)) return false;
        if(!Units.canTargetBuilding(collidesAir, collidesGround, build)) return false;
        return build.team != b.team || Units.targetableAllTeams(build) || forced == build || collidesTeam;
    }

    @Override
    public void init(Bullet b){
        super.init(b);

        float px, py,
            rot = b.rotation();
        Teamc target = b.data instanceof Teamc t ? t : null;
        if(target instanceof Healthc h && !h.isValid()){
            target = null;
        }
        if(target != null){
            px = target.x();
            py = target.y();
        }else if(!(Float.isNaN(b.aimX) || Float.isNaN(b.aimY)) && !(b.aimX == -1f && b.aimY == -1f)){
            px = b.aimX;
            py = b.aimY;
        }else{
            px = b.x + b.lifetime * b.vel.x;
            py = b.y + b.lifetime * b.vel.y;
        }

        Geometry.iterateLine(0f, b.x, b.y, px, py, trailSpacing, (x, y) -> {
            trailEffect.at(x, y, rot);
        });

        b.time = b.lifetime;
        b.set(px, py);

        //calculate hit entity

        cdist = 0f;
        result = null;
        float range = 1f;
        Teamc forced = forcedFriendlyTarget(b);
        boolean includeOwn = collidesTeam || forced != null;

        Cons<Unit> unitCons = e -> {
            if(!canHitUnit(b, e, forced)) return;

            e.hitbox(Tmp.r1);
            if(!Tmp.r1.contains(px, py)) return;

            float dst = e.dst(px, py) - e.hitSize;
            if((result == null || dst < cdist)){
                result = e;
                cdist = dst;
            }
        };

        if(includeOwn){
            Units.nearby(px - range, py - range, range * 2f, range * 2f, unitCons);
        }else{
            Units.nearbyEnemies(b.team, px - range, py - range, range * 2f, range * 2f, unitCons);
        }

        if(result != null){
            b.collision(result, px, py);
        }else if(collidesTiles){
            Building build = Vars.world.buildWorld(px, py);
            if(build != null){
                float health = build.health;
                boolean remove = false;

                if(canHitBuild(b, build, forced) && build.collide(b)){
                    remove = build.collision(b);
                }

                if(remove || collidesTeam){
                    hit(b, px, py);
                    b.hit = true;
                }

                if(testCollision(b, build)){
                    hitTile(b, build, px, py, health, true);
                }
            }
        }

        b.remove();

        b.vel.setZero();
    }
}
