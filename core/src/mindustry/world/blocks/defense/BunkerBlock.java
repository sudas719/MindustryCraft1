package mindustry.world.blocks.defense;

import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.ai.*;
import mindustry.ai.types.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.bullet.*;
import mindustry.entities.pattern.*;
import mindustry.gen.*;
import mindustry.io.*;
import mindustry.type.*;
import mindustry.world.blocks.production.*;
import mindustry.world.blocks.storage.CoreBlock.*;
import mindustry.world.blocks.units.*;

import static mindustry.Vars.*;

public class BunkerBlock extends HeatCrafter{
    public static final int configStopAttack = 1;
    public static final int configUnloadAll = 2;
    public static final int configRecycle = 3;

    public int slotCapacity = 4;
    public float rangeBonus = tilesize;
    public float recycleTime = 4f * 60f;
    public int recycleCrystalRefund = 75;
    public float turnSpeed = 6f;
    public float loadRange = 18f;

    public BunkerBlock(String name){
        super(name);

        update = true;
        solid = true;
        commandable = true;
        configurable = true;
        hasItems = false;
        hasLiquids = false;
        hasPower = false;
        itemCapacity = 0;
        liquidCapacity = 0f;

        config(Integer.class, (BunkerBuild build, Integer value) -> {
            if(value == null) return;
            if(value == configStopAttack){
                build.stopForcedAttack();
            }else if(value == configUnloadAll){
                build.unloadAll(false);
            }else if(value == configRecycle){
                build.startRecycle();
            }
        });
    }

    @Override
    public void setBars(){
        super.setBars();
        removeBar("heat");
    }

    public static boolean isBarracksUnit(UnitType type){
        if(type == null) return false;
        if(Blocks.groundFactory instanceof UnitFactory factory){
            for(UnitFactory.UnitPlan plan : factory.plans){
                if(plan.unit == type) return true;
            }
        }
        return type == UnitTypes.nova || type == UnitTypes.dagger || type == UnitTypes.reaper || type == UnitTypes.fortress || type == UnitTypes.ghost;
    }

    public static int unitSlotCost(UnitType type){
        if(type == null) return 0;
        return Math.max(type.population, 1);
    }

    public class BunkerBuild extends HeatCrafterBuild{
        public Seq<GarrisonEntry> garrison = new Seq<>();
        public @Nullable Vec2 rallyPoint;
        public int forcedUnitId = -1;
        public int forcedBuildPos = -1;
        public float turretRotation = 90f;
        public boolean recycling = false;
        public float recycleProgress = 0f;

        private final Seq<GarrisonEntry> removeEntries = new Seq<>();

        @Override
        public byte version(){
            return 1;
        }

        @Override
        public boolean isCommandable(){
            return true;
        }

        @Override
        public @Nullable Vec2 getCommandPosition(){
            return rallyPoint;
        }

        @Override
        public void onCommand(Vec2 target){
            if(target == null) return;
            if(recycling) return;

            Teamc enemy = resolveEnemyAt(target.x, target.y);
            if(enemy != null){
                setForcedTarget(enemy);
            }else{
                stopForcedAttack();
                rallyPoint = new Vec2(target);
            }
        }

        @Override
        public void updateTile(){
            if(recycling){
                recycleProgress += edelta();
                if(recycleProgress >= recycleTime){
                    finishRecycle();
                }
                return;
            }

            loadFollowingUnits();
            updateCombat();
        }

        public boolean hasGarrison(){
            return garrison.size > 0;
        }

        public int usedSlots(){
            int used = 0;
            for(GarrisonEntry entry : garrison){
                UnitType type = content.unit(entry.typeId);
                used += unitSlotCost(type);
            }
            return used;
        }

        public int freeSlots(){
            return Math.max(slotCapacity + UnitTypes.steelArmorBunkerSlotBonus(team) - usedSlots(), 0);
        }

        public boolean canLoadType(UnitType type){
            if(recycling) return false;
            if(!isBarracksUnit(type)) return false;
            return unitSlotCost(type) <= freeSlots();
        }

        public boolean canLoadUnit(Unit unit){
            return unit != null && unit.isValid() && unit.team == team && canLoadType(unit.type);
        }

        public boolean isRecycling(){
            return recycling;
        }

        public float recycleRemainingFraction(){
            if(!recycling || recycleTime <= 0f) return 0f;
            return Mathf.clamp(1f - recycleProgress / recycleTime);
        }

        public void commandLoadUnits(int[] unitIds){
            if(unitIds == null || unitIds.length == 0) return;
            if(recycling) return;

            for(int id : unitIds){
                Unit unit = Groups.unit.getByID(id);
                if(unit == null || !unit.isValid() || unit.team != team) continue;
                if(!(unit.controller() instanceof CommandAI ai)) continue;
                if(!canLoadType(unit.type)) continue;

                ai.command(UnitCommand.moveCommand);
                ai.commandFollow(this);
                unit.lastCommanded = "Bunker";
                if(unit.within(this, loadRadius(unit))){
                    tryLoadUnit(unit);
                }
            }
        }

        public void stopForcedAttack(){
            forcedUnitId = -1;
            forcedBuildPos = -1;
        }

        public void unloadAll(boolean centerSpawn){
            if(net.client()) return;
            if(garrison.isEmpty()) return;
            stopForcedAttack();

            int total = garrison.size;
            for(int i = 0; i < total; i++){
                GarrisonEntry entry = garrison.get(i);
                UnitType type = content.unit(entry.typeId);
                if(type == null) continue;

                Unit unit = type.create(team);
                if(centerSpawn){
                    unit.set(x, y);
                }else{
                    float baseAngle = i * (360f / Math.max(total, 1));
                    float dist = 6f + block.size * tilesize * 0.5f;
                    unit.set(x + Angles.trnsx(baseAngle, dist), y + Angles.trnsy(baseAngle, dist));
                }
                unit.rotation = turretRotation;
                unit.add();

                if(rallyPoint != null && unit.controller() instanceof CommandAI ai){
                    ai.command(UnitCommand.moveCommand);
                    ai.commandPosition(rallyPoint);
                }
            }

            garrison.clear();
        }

        public void startRecycle(){
            if(recycling) return;
            recycling = true;
            recycleProgress = 0f;
            stopForcedAttack();
        }

        private void finishRecycle(){
            if(net.client()) return;
            if(!isValid()) return;
            unloadAll(true);

            CoreBuild core = team.core();
            if(core != null && recycleCrystalRefund > 0){
                core.items.add(Items.graphite, recycleCrystalRefund);
            }

            Fx.blockExplosionSmoke.at(x, y);
            Fx.dynamicExplosion.at(x, y, block.size);
            kill();
        }

        private void loadFollowingUnits(){
            if(freeSlots() <= 0) return;
            float radius = loadRange + block.size * tilesize * 0.5f;
            Units.nearby(team, x - radius, y - radius, radius * 2f, radius * 2f, unit -> {
                if(unit == null || !unit.isValid()) return;
                if(!(unit.controller() instanceof CommandAI ai)) return;
                if(ai.followTarget != this && ai.attackTarget != this) return;
                if(!unit.within(this, loadRadius(unit))) return;
                tryLoadUnit(unit);
            });
        }

        private boolean tryLoadUnit(Unit unit){
            if(net.client()) return false;
            if(!canLoadUnit(unit)) return false;

            GarrisonEntry entry = new GarrisonEntry();
            entry.typeId = unit.type.id;
            entry.reload = 0f;
            garrison.add(entry);

            if(unit.isPlayer()){
                unit.getPlayer().clearUnit();
            }
            unit.remove();
            Fx.unitDrop.at(x, y);
            return true;
        }

        private float loadRadius(Unit unit){
            return block.size * tilesize * 0.55f + unit.hitSize * 0.55f;
        }

        private void updateCombat(){
            Teamc forced = resolveForcedTarget();
            removeEntries.clear();

            for(GarrisonEntry entry : garrison){
                UnitType type = content.unit(entry.typeId);
                if(type == null || !isBarracksUnit(type)){
                    removeEntries.add(entry);
                    continue;
                }

                Weapon weapon = primaryWeapon(type);
                if(weapon == null || weapon.noAttack || weapon.bullet == null) continue;

                entry.reload += edelta();

                float range = effectiveRange(type, weapon);
                Teamc target = forcedTargetFor(type, forced, range);
                if(target == null){
                    target = closestTargetFor(type, range);
                }
                if(target == null) continue;

                float targetAngle = angleTo(target);
                turretRotation = Angles.moveToward(turretRotation, targetAngle, turnSpeed * edelta());

                if(entry.reload >= weapon.reload){
                    fireWeapon(type, weapon, target);
                    entry.reload = 0f;
                }
            }

            if(removeEntries.any()){
                garrison.removeAll(removeEntries);
            }
        }

        private float effectiveRange(UnitType type, Weapon weapon){
            float base = weapon.range();
            if(base <= 0f){
                base = type.range;
            }
            return Math.max(base, 0f) + rangeBonus;
        }

        private @Nullable Teamc forcedTargetFor(UnitType type, @Nullable Teamc forced, float range){
            if(forced == null) return null;
            if(!canTypeTarget(type, forced)) return null;
            return withinRange(forced, range) ? forced : null;
        }

        private @Nullable Teamc closestTargetFor(UnitType type, float range){
            return Units.closestTarget(team, x, y, range,
                unit -> unit != null && unit.isValid() && unit.checkTarget(type.targetAir, type.targetGround),
                build -> type.targetGround && build != null && build.isValid()
            );
        }

        private boolean canTypeTarget(UnitType type, Teamc target){
            if(target == null || target.team() == team) return false;
            if(target instanceof Unit unit){
                return unit.checkTarget(type.targetAir, type.targetGround);
            }
            return target instanceof Building && type.targetGround;
        }

        private boolean withinRange(Teamc target, float range){
            float extra = target instanceof Sized s ? s.hitSize() * 0.5f : 4f;
            return Mathf.dst(x, y, target.getX(), target.getY()) <= range + extra;
        }

        private @Nullable Teamc resolveForcedTarget(){
            if(forcedUnitId >= 0){
                Unit unit = Groups.unit.getByID(forcedUnitId);
                if(unit != null && unit.isValid() && unit.team != team){
                    return unit;
                }
                forcedUnitId = -1;
            }

            if(forcedBuildPos >= 0){
                Building build = world.build(forcedBuildPos);
                if(build != null && build.isValid() && build.team != team){
                    return build;
                }
                forcedBuildPos = -1;
            }

            return null;
        }

        private void setForcedTarget(Teamc target){
            if(target instanceof Unit unit){
                forcedUnitId = unit.id;
                forcedBuildPos = -1;
            }else if(target instanceof Building build){
                forcedBuildPos = build.pos();
                forcedUnitId = -1;
            }else{
                stopForcedAttack();
            }
        }

        private @Nullable Teamc resolveEnemyAt(float wx, float wy){
            Building build = world.buildWorld(wx, wy);
            if(build != null && build.team != team && build.isValid() && build.within(wx, wy, build.hitSize() * 0.6f)){
                return build;
            }

            Unit unit = Units.closestEnemy(team, wx, wy, 14f, u ->
                u != null && u.isValid() && u.within(wx, wy, Math.max(8f, u.hitSize * 0.6f))
            );
            if(unit != null && unit.team != team){
                return unit;
            }
            return null;
        }

        private @Nullable Weapon primaryWeapon(UnitType type){
            for(Weapon weapon : type.weapons){
                if(weapon == null || weapon.noAttack || weapon.bullet == null) continue;
                return weapon;
            }
            return null;
        }

        private void fireWeapon(UnitType type, Weapon weapon, Teamc target){
            float baseAngle = angleTo(target);
            weapon.shoot.shoot(1, (xOffset, yOffset, rotation, delay, mover) -> {
                Time.run(delay, () -> fireShot(type, weapon, target, baseAngle + rotation, mover));
            }, null);
        }

        private void fireShot(UnitType type, Weapon weapon, Teamc target, float angle, @Nullable Mover mover){
            if(!isValid() || recycling) return;
            if(target == null || target.team() == team) return;
            if(target instanceof Healthc health && !health.isValid()) return;

            float range = effectiveRange(type, weapon);
            if(!withinRange(target, range)) return;

            float shotAngle = angle + Mathf.range(weapon.inaccuracy + weapon.bullet.inaccuracy);
            float shootX = x + Angles.trnsx(shotAngle, weapon.shootY);
            float shootY = y + Angles.trnsy(shotAngle, weapon.shootY);
            float lifeScl = weapon.bullet.scaleLife ? Mathf.clamp(Mathf.dst(shootX, shootY, target.getX(), target.getY()) / Math.max(weapon.bullet.range, 1f)) : 1f;
            float velocityScl = (1f - weapon.velocityRnd) + Mathf.random(weapon.velocityRnd);

            weapon.bullet.create(this, this, team, shootX, shootY, shotAngle, -1f, velocityScl, lifeScl, target, mover, target.getX(), target.getY(), target);

            if(weapon.shootSound != Sounds.none && !headless){
                weapon.shootSound.at(shootX, shootY, Mathf.random(weapon.soundPitchMin, weapon.soundPitchMax), weapon.shootSoundVolume);
            }
            if(weapon.shake > 0f){
                Effect.shake(weapon.shake, weapon.shake, shootX, shootY);
            }
        }

        @Override
        public void write(Writes write){
            super.write(write);

            write.i(garrison.size);
            for(GarrisonEntry entry : garrison){
                write.i(entry.typeId);
                write.f(entry.reload);
            }

            TypeIO.writeVecNullable(write, rallyPoint);
            write.i(forcedUnitId);
            write.i(forcedBuildPos);
            write.f(turretRotation);
            write.bool(recycling);
            write.f(recycleProgress);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);

            garrison.clear();
            if(revision >= 1){
                int amount = Math.max(read.i(), 0);
                for(int i = 0; i < amount; i++){
                    GarrisonEntry entry = new GarrisonEntry();
                    entry.typeId = read.i();
                    entry.reload = read.f();
                    garrison.add(entry);
                }
                rallyPoint = TypeIO.readVecNullable(read);
                forcedUnitId = read.i();
                forcedBuildPos = read.i();
                turretRotation = read.f();
                recycling = read.bool();
                recycleProgress = read.f();
            }else{
                rallyPoint = null;
                forcedUnitId = -1;
                forcedBuildPos = -1;
                turretRotation = 90f;
                recycling = false;
                recycleProgress = 0f;
            }
        }
    }

    public static class GarrisonEntry{
        public int typeId;
        public float reload;
    }
}
