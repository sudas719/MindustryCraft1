package mindustry.world.blocks.defense;

import arc.*;
import arc.audio.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.game.*;
import mindustry.game.EventType.*;
import mindustry.graphics.*;
import mindustry.world.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class Wall extends Block{
    private static final float derelictScrapSharedHealth = 2000f;
    private static final float derelictScrapArmor = 1f;

    /** Lighting chance. -1 to disable */
    public float lightningChance = -1f;
    public float lightningDamage = 20f;
    public int lightningLength = 17;
    public Color lightningColor = Pal.surge;
    public Sound lightningSound = Sounds.shootArc;

    /** Bullet deflection chance. -1 to disable */
    public float chanceDeflect = -1f;
    public boolean flashHit;
    public Color flashColor = Color.white;
    public Sound deflectSound = Sounds.none;

    public Wall(String name){
        super(name);
        solid = true;
        destructible = true;
        group = BlockGroup.walls;
        buildCostMultiplier = 6f;
        canOverdrive = false;
        drawDisabled = false;
        crushDamageMultiplier = 5f;
        priority = TargetPriority.wall;

        //it's a wall of course it's supported everywhere
        envEnabled = Env.any;
    }

    @Override
    public void setStats(){
        super.setStats();

        if(chanceDeflect > 0f) stats.add(Stat.baseDeflectChance, chanceDeflect, StatUnit.none);
        if(lightningChance > 0f){
            stats.add(Stat.lightningChance, lightningChance * 100f, StatUnit.percent);
            stats.add(Stat.lightningDamage, lightningDamage, StatUnit.none);
        }
    }

    @Override
    public void init(){
        if(size == 2 && destroySound == Sounds.unset) destroySound = Sounds.blockExplodeWall;
        super.init();
    }

    @Override
    public TextureRegion[] icons(){
        return new TextureRegion[]{Core.atlas.find(Core.atlas.has(name) ? name : name + "1")};
    }

    public class WallBuild extends Building{
        public float hit;

        private boolean isDerelictSharedScrapWall(){
            return block == Blocks.scrapWall && team == Team.derelict;
        }

        private void collectDerelictSharedScrapWalls(Seq<WallBuild> out){
            out.clear();
            if(!isDerelictSharedScrapWall()) return;

            IntSet visited = new IntSet();
            IntSeq queue = new IntSeq();
            queue.add(pos());
            visited.add(pos());

            while(queue.size > 0){
                Tile next = world.tile(queue.pop());
                if(next == null || !(next.build instanceof WallBuild wall) || !wall.isDerelictSharedScrapWall()) continue;

                out.add(wall);

                for(Point2 point : Geometry.d4){
                    Tile nearby = world.tile(next.x + point.x, next.y + point.y);
                    if(nearby != null && visited.add(nearby.pos())){
                        queue.add(nearby.pos());
                    }
                }
            }
        }

        private float derelictSharedScrapHealth(Seq<WallBuild> group){
            boolean initialized = false;
            float result = derelictScrapSharedHealth;

            for(var wall : group){
                if(wall.maxHealth > wall.block.health + 0.001f || wall.health > wall.block.health + 0.001f){
                    initialized = true;
                    result = Math.min(result, Mathf.clamp(wall.health, 0f, derelictScrapSharedHealth));
                }
            }

            return initialized ? result : derelictScrapSharedHealth;
        }

        private void setDerelictSharedScrapHealth(Seq<WallBuild> group, float health){
            float clamped = Mathf.clamp(health, 0f, derelictScrapSharedHealth);
            for(var wall : group){
                wall.maxHealth = derelictScrapSharedHealth;
                wall.health = clamped;
                wall.healthChanged();
            }
        }

        private void syncDerelictSharedScrapGroup(){
            if(!isDerelictSharedScrapWall()) return;

            Seq<WallBuild> group = new Seq<>();
            collectDerelictSharedScrapWalls(group);
            if(group.isEmpty()) return;

            setDerelictSharedScrapHealth(group, derelictSharedScrapHealth(group));
        }

        private void destroyDerelictSharedScrapGroup(Seq<WallBuild> group){
            for(var wall : group){
                wall.maxHealth = derelictScrapSharedHealth;
                wall.health = 0f;
                wall.healthChanged();
            }

            for(var wall : group){
                if(wall.isValid() && !wall.dead()){
                    Call.buildDestroyed(wall);
                }
            }
        }

        private void restoreNormalScrapWallHealth(float previousHealth){
            maxHealth = block.health;
            health = Mathf.clamp(previousHealth, 0f, maxHealth);
            healthChanged();
        }

        private void damageDerelictSharedScrap(float damage){
            if(dead()) return;

            float dm = state.rules.blockHealth(team);
            lastDamageTime = Time.time;

            if(Mathf.zero(dm)){
                damage = derelictScrapSharedHealth + 1f;
            }else{
                damage /= dm;
            }

            if(!net.client()){
                Seq<WallBuild> group = new Seq<>();
                collectDerelictSharedScrapWalls(group);
                if(group.isEmpty()) return;

                float nextHealth = derelictSharedScrapHealth(group) - Damage.applyArmor(damage, derelictScrapArmor);
                if(nextHealth > 0f){
                    setDerelictSharedScrapHealth(group, nextHealth);
                }else{
                    destroyDerelictSharedScrapGroup(group);
                }
            }else{
                healthChanged();
            }
        }

        @Override
        public void draw(){
            super.draw();

            //draw flashing white overlay if enabled
            if(flashHit){
                if(hit < 0.0001f) return;

                Draw.color(flashColor);
                Draw.alpha(hit * 0.5f);
                Draw.blend(Blending.additive);
                Fill.rect(x, y, tilesize * size, tilesize * size);
                Draw.blend();
                Draw.reset();

                if(!state.isPaused()){
                    hit = Mathf.clamp(hit - Time.delta / 10f);
                }
            }
        }

        @Override
        public void onProximityAdded(){
            super.onProximityAdded();
            syncDerelictSharedScrapGroup();
        }

        @Override
        public void onProximityUpdate(){
            super.onProximityUpdate();
            syncDerelictSharedScrapGroup();
        }

        @Override
        public void changeTeam(Team next){
            boolean wasShared = isDerelictSharedScrapWall();
            float previousHealth = health;

            super.changeTeam(next);

            if(block != Blocks.scrapWall) return;

            if(team == Team.derelict){
                syncDerelictSharedScrapGroup();
                updateProximity();
            }else if(wasShared){
                restoreNormalScrapWallHealth(previousHealth);
                updateProximity();
            }
        }

        @Override
        public boolean collision(Bullet bullet){
            if(isDerelictSharedScrapWall()){
                boolean wasDead = health <= 0f;
                float damage = bullet.type.buildingDamage(bullet);
                if(!bullet.type.pierceArmor){
                    damage = Damage.applyArmor(damage, derelictScrapArmor * bullet.type.armorMultiplier);
                }

                if(!net.client()){
                    Seq<WallBuild> group = new Seq<>();
                    collectDerelictSharedScrapWalls(group);
                    if(!group.isEmpty()){
                        float nextHealth = derelictSharedScrapHealth(group) - damage;
                        if(nextHealth > 0f){
                            setDerelictSharedScrapHealth(group, nextHealth);
                        }else{
                            destroyDerelictSharedScrapGroup(group);
                        }
                    }
                }else{
                    healthChanged();
                }

                Events.fire(new BuildDamageEvent().set(self(), bullet));

                if(health <= 0f && !wasDead){
                    Events.fire(new BuildingBulletDestroyEvent(self(), bullet));
                }

                hit = 1f;

                if(lightningChance > 0f && Mathf.chance(lightningChance)){
                    Lightning.create(team, lightningColor, lightningDamage, x, y, bullet.rotation() + 180f, lightningLength);
                    lightningSound.at(tile, Mathf.random(0.9f, 1.1f));
                }

                if(chanceDeflect > 0f){
                    if(bullet.vel.len() <= 0.1f || !bullet.type.reflectable) return true;
                    if(!Mathf.chance(chanceDeflect / bullet.damage())) return true;

                    deflectSound.at(tile, Mathf.random(0.9f, 1.1f));
                    bullet.trns(-bullet.vel.x, -bullet.vel.y);

                    float penX = Math.abs(x - bullet.x), penY = Math.abs(y - bullet.y);
                    if(penX > penY){
                        bullet.vel.x *= -1;
                    }else{
                        bullet.vel.y *= -1;
                    }

                    bullet.owner = this;
                    bullet.team = team;
                    bullet.time += 1f;
                    return false;
                }

                return true;
            }

            super.collision(bullet);

            hit = 1f;

            //create lightning if necessary
            if(lightningChance > 0f){
                if(Mathf.chance(lightningChance)){
                    Lightning.create(team, lightningColor, lightningDamage, x, y, bullet.rotation() + 180f, lightningLength);
                    lightningSound.at(tile, Mathf.random(0.9f, 1.1f));
                }
            }

            //deflect bullets if necessary
            if(chanceDeflect > 0f){
                //slow bullets are not deflected
                if(bullet.vel.len() <= 0.1f || !bullet.type.reflectable) return true;

                //bullet reflection chance depends on bullet damage
                if(!Mathf.chance(chanceDeflect / bullet.damage())) return true;

                //make sound
                deflectSound.at(tile, Mathf.random(0.9f, 1.1f));

                //translate bullet back to where it was upon collision
                bullet.trns(-bullet.vel.x, -bullet.vel.y);

                float penX = Math.abs(x - bullet.x), penY = Math.abs(y - bullet.y);

                if(penX > penY){
                    bullet.vel.x *= -1;
                }else{
                    bullet.vel.y *= -1;
                }

                bullet.owner = this;
                bullet.team = team;
                bullet.time += 1f;

                //disable bullet collision by returning false
                return false;
            }

            return true;
        }

        @Override
        public void damage(float damage){
            if(isDerelictSharedScrapWall()){
                damageDerelictSharedScrap(damage);
            }else{
                super.damage(damage);
            }
        }
    }
}
