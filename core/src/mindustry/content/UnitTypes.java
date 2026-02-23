package mindustry.content;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import arc.Events;
import arc.Core;
import mindustry.ai.*;
import mindustry.ai.types.*;
import mindustry.annotations.Annotations.*;
import mindustry.entities.*;
import mindustry.entities.units.WeaponMount;
import mindustry.entities.abilities.*;
import mindustry.entities.bullet.*;
import mindustry.entities.effect.*;
import mindustry.entities.part.*;
import mindustry.entities.pattern.*;
import mindustry.game.EventType.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.type.ammo.*;
import mindustry.type.unit.*;
import mindustry.type.weapons.*;
import mindustry.world.Tile;
import mindustry.world.blocks.payloads.*;
import mindustry.world.blocks.units.*;
import mindustry.world.meta.*;

import static arc.graphics.g2d.Draw.*;
import static arc.graphics.g2d.Lines.*;
import static arc.math.Angles.*;
import static mindustry.Vars.*;

public class UnitTypes{
    //region standard

    private static final UnitDamageEvent unitDamageEvent = new UnitDamageEvent();
    private static final float coreFlyerLandTime = 60f;
    private static final IntMap<CoreFlyerData> coreFlyerData = new IntMap<>();
    private static final float widowBurrowTime = 2f * 60f;
    private static final float widowUnburrowTime = 2f * 60f;
    private static final float widowLockTime = 3f * 60f;
    private static final float widowReloadTime = 29f * 60f;
    private static final float widowRangeTiles = 5.5f;
    private static final IntMap<WidowLockData> widowLockData = new IntMap<>();
    private static final IntIntMap widowTargetLocks = new IntIntMap();
    private static final float hurricaneBaseRangeTiles = 5f;
    private static final float hurricaneLockRangeTiles = 15f;
    private static final float hurricaneLockTime = 14f * 60f;
    private static final float hurricaneLockCooldown = 4f * 60f;
    private static final float hurricaneLockFlashDuration = 24f;
    private static final IntMap<HurricaneLockData> hurricaneLockData = new IntMap<>();
    private static final float preceptSiegeTransitionTime = 3f * 60f;
    private static final float preceptMobileRangeTiles = 7f;
    private static final float preceptSiegeRangeTiles = 13f;
    private static final float preceptMobileReload = 0.74f * 60f;
    private static final float preceptSiegeReload = 2.14f * 60f;
    private static final float preceptSiegeFlashDuration = 24f;
    private static final IntMap<PreceptSiegeData> preceptSiegeData = new IntMap<>();
    private static final float scepterSwitchTime = 2f * 60f;
    private static final IntMap<ScepterModeData> scepterModeData = new IntMap<>();
    private static final float liberatorDeployTime = 4f * 60f;
    private static final float liberatorUndeployTime = 1.5f * 60f;
    private static final float liberatorZoneRadiusTiles = 5f;
    private static final float liberatorZoneSelectTiles = 8f;
    private static final float liberatorFighterRangeTiles = 5f;
    private static final float liberatorDefenseRangeTiles = 10f;
    private static final IntMap<LiberatorData> liberatorData = new IntMap<>();
    private static final float scaledTankVisualScale = 0.65f;
    private static final float scaledTankShadowScale = scaledTankVisualScale * 0.85f;
    private static final float medivacAfterburnerDuration = 6f * 60f;
    private static final float medivacBaseSpeed = 3.5f;
    private static final float medivacAfterburnerBonusSpeed = 2.44f;
    private static final float medivacLoadRange = 1.5f * tilesize;
    private static final float medivacHealRange = 5.5f * tilesize;
    private static final int medivacMaxSlots = 8;
    private static final IntSet medivacMovingUnload = new IntSet();
    private static final float ravenTurretDeployRange = 3.5f * tilesize;
    private static final float ravenTurretLifetime = 8f * 60f;
    private static final float ravenAntiArmorRange = 11f * tilesize;
    private static final float ravenAntiArmorRadius = 3f * tilesize;
    private static final float ravenAntiArmorDuration = 21f * 60f;
    private static final float ravenMatrixRange = 10f * tilesize;
    private static final float ravenMatrixDuration = 11f * 60f;
    private static final float ravenTurretCost = 50f;
    private static final float ravenAntiArmorCost = 75f;
    private static final float ravenMatrixCost = 75f;
    private static final float battlecruiserWeaponRange = 6f * tilesize;
    private static final float battlecruiserYamatoRange = 11f * tilesize;
    private static final float battlecruiserYamatoChargeTime = 2f * 60f;
    private static final float battlecruiserYamatoCooldown = 71f * 60f;
    private static final float battlecruiserWarpChargeTime = 1f * 60f;
    private static final float battlecruiserWarpTransitTime = 4f * 60f;
    private static final float battlecruiserWarpCooldown = 71f * 60f;
    private static final float battlecruiserWarpAppearTime = 0.9f * 60f;
    private static final float battlecruiserWarpEmergenceStart = 0.74f;
    private static final float battlecruiserBodyScale = 0.60f;
    private static final float battlecruiserGhostScale = battlecruiserBodyScale;
    private static final float battlecruiserMaterializeFrontDelay = 0f;
    private static final float battlecruiserMaterializeFrontDuration = 0.45f;
    private static final int battlecruiserMaterializeSlices = 30;
    private static final FloatSeq battlecruiserSpotMaskLeft = new FloatSeq();
    private static final FloatSeq battlecruiserSpotMaskRight = new FloatSeq();
    private static float battlecruiserSpotMaxWorldRadius = 2.6f;
    private static final Seq<BattlecruiserAfterDraw> battlecruiserAfterDrawQueue = new Seq<>();
    private static int battlecruiserAfterDrawCount = 0;
    private static boolean battlecruiserAfterDrawHooked = false;
    private static final Effect battlecruiserWarpDisintegrateEffect = new Effect(24f, e -> {
        Draw.z(Layer.effect + 0.2f);
        float rot = e.rotation;
        float size = e.data instanceof Float ? Math.max((Float)e.data, 20f) : 30f;
        float fin = e.fin();
        float fout = e.fout();
        float fx = Angles.trnsx(rot, 1f), fy = Angles.trnsy(rot, 1f);
        float nx = Angles.trnsx(rot + 90f, 1f), ny = Angles.trnsy(rot + 90f, 1f);

        Fx.rand.setSeed(e.id);
        for(int i = 0; i < 64; i++){
            float lane = Fx.rand.range(size * 0.55f);
            float along = Fx.rand.range(size * 0.45f) + fin * Fx.rand.random(5f, 18f);
            float lx = e.x + nx * lane + fx * along;
            float ly = e.y + ny * lane + fy * along;
            float segment = Fx.rand.random(3.2f, 11.5f) * (0.35f + fout * 0.9f);
            float alpha = (0.2f + 0.62f * fout) * Fx.rand.random(0.65f, 1f);

            Draw.color(0.3f, 1f, 0.45f, alpha);
            Lines.stroke((0.3f + Fx.rand.random(0.7f)) * fout + 0.08f);
            Lines.lineAngleCenter(lx, ly, rot + Fx.rand.range(5f), segment);
        }

        Fx.rand.setSeed(e.id * 37L + 5L);
        for(int i = 0; i < 56; i++){
            float lane = Fx.rand.range(size * 0.58f);
            float along = Fx.rand.range(size * 0.52f) + fin * Fx.rand.random(2f, 14f);
            float px = e.x + nx * lane + fx * along;
            float py = e.y + ny * lane + fy * along;
            float alpha = (0.12f + 0.5f * fout) * Fx.rand.random(0.55f, 1f);

            Draw.color(0.35f, 1f, 0.5f, alpha);
            Fill.circle(px, py, (0.2f + Fx.rand.random(0.5f)) * fout + 0.04f);
        }

        Drawf.light(e.x, e.y, 18f + size * 0.65f, Color.valueOf("54ff8b"), 0.15f * fout);
        Draw.reset();
    });
    private static final Effect battlecruiserWarpRippleEffect = new Effect(1f, e -> {
        // visual rings disabled; keep only shader-based distortion triggered in updateBattlecruiser()
    });
    private static final float bansheeCloakCost = 25f;
    private static final float bansheeCloakDrain = 1.3f;
    private static final IntMap<RavenData> ravenData = new IntMap<>();
    private static final IntMap<BattlecruiserData> battlecruiserData = new IntMap<>();
    private static BulletType battlecruiserYamatoBullet;

    public static class CoreFlyerData{
        public final Vec2 target = new Vec2();
        public boolean active = false;
        public boolean landing = false;
        public float landTime = 0f;
        public float returnRotation = 0f;
    }

    public static class WidowLockData{
        public int targetId = -1;
        public float lockTime = 0f;
    }

    public static class HurricaneLockData{
        public int targetId = -1;
        public float activeTime = 0f;
        public float cooldown = 0f;
        public float flash = 0f;
    }

    public static class PreceptSiegeData{
        public float cooldown = 0f;
        public float flash = 0f;
    }

    public static class ScepterModeData{
        public boolean impactMode = false;
        public boolean switching = false;
        public boolean switchToImpact = false;
        public float switchTime = 0f;
    }

    public static class LiberatorData{
        public final Vec2 zone = new Vec2();
        public final Vec2 approach = new Vec2();
        public boolean zoneSet = false;
        public boolean defenseMode = false;
        public boolean pendingDeploy = false;
        public boolean deploying = false;
        public boolean undeploying = false;
        public float transitionTime = 0f;
    }

    public static class RavenData{
        public final Vec2 antiArmorTarget = new Vec2();
        public int matrixTargetId = -1;
        public boolean pendingAntiArmor = false;
        public boolean pendingMatrix = false;
    }

    public static class BattlecruiserData{
        public float yamatoCooldown = 0f;
        public float warpCooldown = 0f;
        public int yamatoTargetId = -1;
        public int yamatoBuildPos = -1;
        public boolean pendingYamato = false;
        public boolean yamatoCharging = false;
        public float yamatoChargeTime = 0f;
        public final Vec2 warpTarget = new Vec2();
        public final Vec2 warpFrom = new Vec2();
        public boolean pendingWarp = false;
        public boolean warpCharging = false;
        public float warpChargeTime = 0f;
        public boolean warping = false;
        public float warpTransitTime = 0f;
        public float warpRotation = 0f;
        public float warpAppearTime = 0f;
        public boolean warpRippleTriggered = false;
    }

    private static class BattlecruiserAfterDraw{
        Unit unit;
        float x;
        float y;
        float rotation;
        float scanFin;
        boolean drawWeapons;

        BattlecruiserAfterDraw set(Unit unit, float x, float y, float rotation, float scanFin, boolean drawWeapons){
            this.unit = unit;
            this.x = x;
            this.y = y;
            this.rotation = rotation;
            this.scanFin = scanFin;
            this.drawWeapons = drawWeapons;
            return this;
        }
    }

    private static void ensureBattlecruiserAfterDrawHook(){
        if(battlecruiserAfterDrawHooked) return;
        battlecruiserAfterDrawHooked = true;

        Events.run(Trigger.uiDrawBegin, () -> {
            if(battlecruiserAfterDrawCount <= 0) return;

            Tmp.m1.set(Draw.proj());
            Draw.proj(Core.camera);
            Draw.sort(false);

            for(int i = 0; i < battlecruiserAfterDrawCount; i++){
                BattlecruiserAfterDraw entry = battlecruiserAfterDrawQueue.get(i);
                if(entry.unit == null) continue;
                if(entry.unit.dead || !entry.unit.isAdded()) continue;

                drawBattlecruiserArrivalStrips(entry.unit, entry.x, entry.y, entry.rotation, entry.scanFin);
                drawBattlecruiserMaterialization(entry.unit, entry.x, entry.y, entry.rotation, entry.scanFin, entry.drawWeapons);
            }

            Draw.flush();
            Draw.proj(Tmp.m1);
            battlecruiserAfterDrawCount = 0;
        });
    }

    private static void queueBattlecruiserAfterDraw(Unit unit, float x, float y, float rotation, float scanFin, boolean drawWeapons){
        ensureBattlecruiserAfterDrawHook();

        int index = battlecruiserAfterDrawCount++;
        if(index >= battlecruiserAfterDrawQueue.size){
            battlecruiserAfterDrawQueue.add(new BattlecruiserAfterDraw());
        }

        battlecruiserAfterDrawQueue.get(index).set(unit, x, y, rotation, scanFin, drawWeapons);
    }

    private static void drawRegionExplicit(TextureRegion region, float x, float y, float rotation){
        if(region == null || !region.found()) return;
        Draw.rect(region, x, y, region.width * region.scale * scaledTankVisualScale / 4f, region.height * region.scale * scaledTankVisualScale / 4f, rotation);
    }

    private static void scaleRegion(TextureRegion region, float factor){
        if(region == null || !region.found()) return;
        region.scale *= factor;
    }

    private static void rebuildBattlecruiserSpotMask(TextureRegion region){
        battlecruiserSpotMaskLeft.clear();
        battlecruiserSpotMaskRight.clear();
        battlecruiserSpotMaxWorldRadius = 2.2f;
        if(region == null || !region.found()) return;

        try{
            PixmapRegion pix = Core.atlas.getPixmap(region);
            if(pix == null || pix.width <= 0 || pix.height <= 0) return;

            int half = pix.width / 2;
            int chosenRadius = -1;
            int[] radii = {16, 12, 9, 7, 5};

            for(int radius : radii){
                FloatSeq left = new FloatSeq();
                FloatSeq right = new FloatSeq();
                int step = radius >= 12 ? 2 : 1;

                for(int y = radius; y < pix.height - radius; y += step){
                    int startX = Math.max(half, radius);
                    int endX = pix.width - radius;
                    for(int x = startX; x < endX; x += step){
                        if(pix.getA(x, y) < 220) continue;

                        boolean okRight = true;
                        for(int oy = -radius; oy <= radius && okRight; oy += 2){
                            for(int ox = -radius; ox <= radius; ox += 2){
                                if(ox * ox + oy * oy > radius * radius) continue;
                                if(pix.getA(x + ox, y + oy) < 170){
                                    okRight = false;
                                    break;
                                }
                            }
                        }
                        if(!okRight) continue;

                        int mx = pix.width - 1 - x;
                        if(mx < radius || mx >= half) continue;

                        boolean okLeft = true;
                        for(int oy = -radius; oy <= radius && okLeft; oy += 2){
                            for(int ox = -radius; ox <= radius; ox += 2){
                                if(ox * ox + oy * oy > radius * radius) continue;
                                if(pix.getA(mx + ox, y + oy) < 170){
                                    okLeft = false;
                                    break;
                                }
                            }
                        }
                        if(!okLeft) continue;

                        float v = (y + 0.5f) / (float)pix.height;
                        right.add((x + 0.5f) / (float)pix.width, v);
                        left.add((mx + 0.5f) / (float)pix.width, v);
                    }
                }

                if(left.size >= 24 && right.size >= 24){
                    battlecruiserSpotMaskLeft.addAll(left);
                    battlecruiserSpotMaskRight.addAll(right);
                    chosenRadius = radius;
                    break;
                }
            }

            if(chosenRadius > 0){
                battlecruiserSpotMaxWorldRadius = Math.max(1.5f, chosenRadius / 4f * 0.72f);
            }
        }catch(Throwable ignored){
            //fallback handled below
        }

        //no fallback points: if mask extraction fails, skip spot drawing to avoid leaking into transparent areas
    }

    private static void drawShadowExplicit(TextureRegion shadowRegion, Unit unit, float shadowElevation, float shadowElevationScl){
        if(shadowRegion == null || !shadowRegion.found()) return;
        float e = Mathf.clamp(unit.elevation, shadowElevation, 1f) * shadowElevationScl * (1f - unit.drownTime);
        float sx = unit.x + UnitType.shadowTX * e, sy = unit.y + UnitType.shadowTY * e;
        var floor = world.floorWorld(sx, sy);
        float dest = floor.canShadow ? 1f : 0f;
        unit.shadowAlpha = unit.shadowAlpha < 0f ? dest : Mathf.approachDelta(unit.shadowAlpha, dest, 0.11f);
        Draw.color(Pal.shadow, Pal.shadow.a * unit.shadowAlpha);
        Draw.rect(shadowRegion, sx, sy, shadowRegion.width * shadowRegion.scale * scaledTankShadowScale / 4f, shadowRegion.height * shadowRegion.scale * scaledTankShadowScale / 4f, unit.rotation - 90f);
        Draw.color();
    }

    public static CoreFlyerData getCoreFlyerData(Unit unit){
        CoreFlyerData data = coreFlyerData.get(unit.id);
        if(data == null){
            data = new CoreFlyerData();
            coreFlyerData.put(unit.id, data);
        }
        return data;
    }

    public static void clearCoreFlyerData(Unit unit){
        coreFlyerData.remove(unit.id);
    }

    public static float widowBurrowDuration(){
        return widowBurrowTime;
    }

    public static float widowReloadDuration(){
        return widowReloadTime;
    }

    public static float widowRange(){
        return widowRangeTiles * tilesize;
    }

    public static WidowLockData getWidowLockData(Unit unit){
        WidowLockData data = widowLockData.get(unit.id);
        if(data == null){
            data = new WidowLockData();
            widowLockData.put(unit.id, data);
        }
        return data;
    }

    public static void clearWidowLockData(Unit unit){
        if(unit == null) return;
        WidowLockData data = widowLockData.get(unit.id);
        if(data != null && data.targetId != -1){
            int owner = widowTargetLocks.get(data.targetId, -1);
            if(owner == unit.id){
                widowTargetLocks.remove(data.targetId, -1);
            }
        }
        widowLockData.remove(unit.id);
    }

    public static boolean isWidow(@Nullable Unit unit){
        return unit != null && crawler != null && unit.type == crawler;
    }

    public static boolean widowIsBurrowing(@Nullable Unit unit){
        return isWidow(unit) && unit.hasEffect(StatusEffects.widowBurrowing);
    }

    public static boolean widowIsBuried(@Nullable Unit unit){
        return isWidow(unit) && unit.hasEffect(StatusEffects.widowBuried) && !unit.hasEffect(StatusEffects.widowBurrowing);
    }

    public static boolean widowIsReloading(@Nullable Unit unit){
        return isWidow(unit) && unit.hasEffect(StatusEffects.widowReloading);
    }

    public static boolean widowIsUnburrowing(@Nullable Unit unit){
        return isWidow(unit) && unit.hasEffect(StatusEffects.widowUnburrowing);
    }

    public static boolean widowIsStealthed(@Nullable Unit unit){
        return widowIsBuried(unit) && !widowIsReloading(unit);
    }

    public static float widowBurrowProgress(@Nullable Unit unit){
        if(!widowIsBurrowing(unit)) return 0f;
        return Mathf.clamp(1f - unit.getDuration(StatusEffects.widowBurrowing) / widowBurrowTime);
    }

    public static float widowReloadProgress(@Nullable Unit unit){
        if(!widowIsReloading(unit)) return 0f;
        return Mathf.clamp(unit.getDuration(StatusEffects.widowReloading) / widowReloadTime);
    }

    public static void commandWidowBurrow(@Nullable Unit unit){
        if(!isWidow(unit)) return;
        if(widowIsBuried(unit) || widowIsBurrowing(unit) || widowIsUnburrowing(unit)) return;
        clearWidowLockData(unit);
        unit.unapply(StatusEffects.widowUnburrowing);
        unit.unapply(StatusEffects.widowBuried);
        unit.apply(StatusEffects.widowBurrowing, widowBurrowTime);
    }

    public static void commandWidowUnburrow(@Nullable Unit unit){
        if(!isWidow(unit)) return;
        if(widowIsUnburrowing(unit)) return;
        if(!widowIsBuried(unit) && !widowIsBurrowing(unit)) return;
        clearWidowLockData(unit);
        unit.unapply(StatusEffects.widowBurrowing);
        unit.unapply(StatusEffects.widowBuried);
        unit.apply(StatusEffects.widowUnburrowing, widowUnburrowTime);
    }

    public static boolean widowCanReserveTarget(@Nullable Unit unit, int targetId){
        if(unit == null || targetId < 0) return false;
        int owner = widowTargetLocks.get(targetId, -1);
        return owner == -1 || owner == unit.id;
    }

    public static boolean widowReserveTarget(@Nullable Unit unit, int targetId){
        if(unit == null || targetId < 0) return false;
        if(!widowCanReserveTarget(unit, targetId)) return false;
        widowTargetLocks.put(targetId, unit.id);
        return true;
    }

    public static void widowReleaseTarget(@Nullable Unit unit, int targetId){
        if(unit == null || targetId < 0) return;
        if(widowTargetLocks.get(targetId, -1) == unit.id){
            widowTargetLocks.remove(targetId, -1);
        }
    }

    private static boolean widowDetectedBy(Unit unit, Team viewer){
        for(Unit other : viewer.data().units){
            if(other == null || !other.isValid()) continue;
            float detectRange = other.type.stealthDetectionRange;
            if(detectRange > 0f && other.within(unit, detectRange)){
                return true;
            }
        }
        return false;
    }

    public static boolean widowHiddenFrom(@Nullable Unit unit, Team viewer){
        if(unit == null) return false;
        if(!widowIsStealthed(unit) && !bansheeCloaked(unit)) return false;
        if(unit.team == viewer) return false;
        return !widowDetectedBy(unit, viewer);
    }

    public static boolean isHurricane(@Nullable Unit unit){
        return unit != null && hurricane != null && unit.type == hurricane;
    }

    public static boolean isSiegeTank(@Nullable Unit unit){
        return unit != null && precept != null && unit.type == precept;
    }

    public static boolean isThor(@Nullable Unit unit){
        return unit != null && scepter != null && unit.type == scepter;
    }

    public static boolean isLiberator(@Nullable Unit unit){
        return unit != null && liberator != null && unit.type == liberator;
    }

    public static float liberatorZoneRadius(){
        return liberatorZoneRadiusTiles * tilesize;
    }

    public static float liberatorZoneSelectRange(){
        return liberatorZoneSelectTiles * tilesize;
    }

    public static float liberatorFighterRange(){
        return liberatorFighterRangeTiles * tilesize;
    }

    public static float liberatorDefenseRange(){
        return liberatorDefenseRangeTiles * tilesize;
    }

    public static LiberatorData getLiberatorData(@Nullable Unit unit){
        if(unit == null){
            return new LiberatorData();
        }
        LiberatorData data = liberatorData.get(unit.id);
        if(data == null){
            data = new LiberatorData();
            liberatorData.put(unit.id, data);
        }
        return data;
    }

    public static void clearLiberatorData(@Nullable Unit unit){
        if(unit == null) return;
        liberatorData.remove(unit.id);
    }

    public static boolean liberatorIsDeploying(@Nullable Unit unit){
        return isLiberator(unit) && getLiberatorData(unit).deploying;
    }

    public static boolean liberatorIsDefending(@Nullable Unit unit){
        return isLiberator(unit) && getLiberatorData(unit).defenseMode && !liberatorIsDeploying(unit) && !liberatorIsUndeploying(unit);
    }

    public static boolean liberatorIsUndeploying(@Nullable Unit unit){
        return isLiberator(unit) && getLiberatorData(unit).undeploying;
    }

    public static boolean liberatorCanEnterDefense(@Nullable Unit unit){
        if(!isLiberator(unit)) return false;
        LiberatorData data = getLiberatorData(unit);
        return !data.defenseMode && !data.deploying && !data.undeploying;
    }

    public static boolean liberatorCanExitDefense(@Nullable Unit unit){
        if(!isLiberator(unit)) return false;
        LiberatorData data = getLiberatorData(unit);
        return data.defenseMode && !data.deploying && !data.undeploying;
    }

    private static void startLiberatorDeploy(Unit unit){
        LiberatorData data = getLiberatorData(unit);
        data.pendingDeploy = false;
        data.deploying = true;
        data.undeploying = false;
        data.transitionTime = liberatorDeployTime;
        unit.unapply(StatusEffects.liberatorUndeploying);
        unit.unapply(StatusEffects.liberatorDefending);
        unit.apply(StatusEffects.liberatorDeploying, liberatorDeployTime);
    }

    public static boolean commandLiberatorDefense(@Nullable Unit unit, @Nullable Vec2 zone){
        if(!liberatorCanEnterDefense(unit) || zone == null) return false;
        LiberatorData data = getLiberatorData(unit);
        data.zone.set(zone);
        data.zoneSet = true;
        data.defenseMode = false;
        data.pendingDeploy = true;
        data.deploying = false;
        data.undeploying = false;
        data.transitionTime = 0f;
        unit.unapply(StatusEffects.liberatorUndeploying);
        unit.unapply(StatusEffects.liberatorDeploying);
        unit.unapply(StatusEffects.liberatorDefending);
        unit.lookAt(data.zone.x, data.zone.y);
        return true;
    }

    public static boolean commandLiberatorFighter(@Nullable Unit unit){
        if(!liberatorCanExitDefense(unit)) return false;
        LiberatorData data = getLiberatorData(unit);
        data.pendingDeploy = false;
        data.deploying = false;
        data.undeploying = true;
        data.transitionTime = liberatorUndeployTime;
        unit.unapply(StatusEffects.liberatorDeploying);
        unit.unapply(StatusEffects.liberatorDefending);
        unit.apply(StatusEffects.liberatorUndeploying, liberatorUndeployTime);
        return true;
    }

    public static boolean liberatorTargetInZone(@Nullable Unit unit, @Nullable Teamc target){
        if(!isLiberator(unit) || target == null) return false;
        LiberatorData data = getLiberatorData(unit);
        if(!data.zoneSet) return false;

        float radius = liberatorZoneRadius();
        return Mathf.within(target.getX(), target.getY(), data.zone.x, data.zone.y, radius);
    }

    private static void drawLiberatorZone(@Nullable Unit unit){
        if(!isLiberator(unit)) return;
        LiberatorData data = getLiberatorData(unit);
        if(!data.zoneSet) return;

        float zoneRadius = liberatorZoneRadius();

        if(data.deploying){
            float progress = 1f - Mathf.clamp(data.transitionTime / liberatorDeployTime);
            float drawRadius = zoneRadius * progress;
            float alpha = (1f - progress) * 0.35f;
            Draw.z(Layer.effect);
            Draw.color(1f, 0.2f, 0.2f, alpha);
            Fill.circle(data.zone.x, data.zone.y, drawRadius);
            Lines.stroke(1.2f, Color.valueOf("ff5959"));
            Lines.circle(data.zone.x, data.zone.y, drawRadius);
            Draw.reset();
        }

        if(data.defenseMode){
            Draw.z(Layer.effect);
            Lines.stroke(1.5f, Color.valueOf("9c9c9c"));
            int arcs = 9;
            float fraction = 1f / (arcs * 2f);
            for(int i = 0; i < arcs; i++){
                float angle = i * (360f / arcs);
                Lines.arc(data.zone.x, data.zone.y, zoneRadius, fraction, angle);
            }
            Draw.reset();
        }
    }

    public static void updateLiberator(@Nullable Unit unit){
        if(!isLiberator(unit)) return;
        LiberatorData data = getLiberatorData(unit);

        if(data.zoneSet && (data.pendingDeploy || data.deploying || data.defenseMode)){
            unit.lookAt(data.zone.x, data.zone.y);
        }

        float selectRange = liberatorZoneSelectRange();
        if(data.pendingDeploy){
            float dist = Mathf.dst(unit.x, unit.y, data.zone.x, data.zone.y);
            if(dist <= selectRange + 1f){
                startLiberatorDeploy(unit);
            }else{
                Tmp.v1.set(unit.x - data.zone.x, unit.y - data.zone.y);
                if(Tmp.v1.isZero(0.001f)){
                    Tmp.v1.trns(unit.rotation, selectRange);
                }else{
                    Tmp.v1.setLength(selectRange);
                }
                data.approach.set(data.zone.x + Tmp.v1.x, data.zone.y + Tmp.v1.y);

                if(unit.controller() instanceof CommandAI ai){
                    ai.command(UnitCommand.moveCommand);
                    if(ai.targetPos == null || !Mathf.within(ai.targetPos.x, ai.targetPos.y, data.approach.x, data.approach.y, 2f)){
                        ai.commandPosition(data.approach, false);
                    }
                }
            }
        }

        if(data.deploying){
            unit.vel.setZero();
            if(unit.controller() instanceof CommandAI ai){
                ai.clearCommands();
            }
            data.transitionTime = Math.max(0f, data.transitionTime - Time.delta);
            if(data.transitionTime <= 0.001f || unit.getDuration(StatusEffects.liberatorDeploying) <= 0.001f){
                data.deploying = false;
                data.defenseMode = true;
                data.pendingDeploy = false;
                unit.unapply(StatusEffects.liberatorDeploying);
                unit.apply(StatusEffects.liberatorDefending, 1f);
            }
        }

        if(data.undeploying){
            unit.vel.setZero();
            if(unit.controller() instanceof CommandAI ai){
                ai.clearCommands();
            }
            data.transitionTime = Math.max(0f, data.transitionTime - Time.delta);
            if(data.transitionTime <= 0.001f || unit.getDuration(StatusEffects.liberatorUndeploying) <= 0.001f){
                data.undeploying = false;
                data.defenseMode = false;
                unit.unapply(StatusEffects.liberatorUndeploying);
                unit.unapply(StatusEffects.liberatorDefending);
            }
        }

        if(data.defenseMode){
            unit.vel.setZero();
            if(!unit.hasEffect(StatusEffects.liberatorDefending)){
                unit.apply(StatusEffects.liberatorDefending, 1f);
            }
        }else if(!data.deploying && !data.undeploying){
            unit.unapply(StatusEffects.liberatorDefending);
        }
    }

    public static float scepterSwitchDuration(){
        return scepterSwitchTime;
    }

    public static ScepterModeData getScepterModeData(@Nullable Unit unit){
        if(unit == null){
            return new ScepterModeData();
        }
        ScepterModeData data = scepterModeData.get(unit.id);
        if(data == null){
            data = new ScepterModeData();
            scepterModeData.put(unit.id, data);
        }
        return data;
    }

    public static void clearScepterModeData(@Nullable Unit unit){
        if(unit == null) return;
        scepterModeData.remove(unit.id);
    }

    public static boolean scepterIsSwitching(@Nullable Unit unit){
        return isThor(unit) && getScepterModeData(unit).switching;
    }

    public static boolean scepterUsingImpactMode(@Nullable Unit unit){
        return isThor(unit) && !scepterIsSwitching(unit) && getScepterModeData(unit).impactMode;
    }

    public static boolean scepterUsingBurstMode(@Nullable Unit unit){
        return isThor(unit) && !scepterIsSwitching(unit) && !getScepterModeData(unit).impactMode;
    }

    public static boolean scepterDisplayImpactMode(@Nullable Unit unit){
        return isThor(unit) && getScepterModeData(unit).impactMode;
    }

    public static boolean scepterDisplayBurstMode(@Nullable Unit unit){
        return isThor(unit) && !getScepterModeData(unit).impactMode;
    }

    public static boolean scepterCanSwitchToImpact(@Nullable Unit unit){
        return isThor(unit) && !scepterIsSwitching(unit) && !getScepterModeData(unit).impactMode;
    }

    public static boolean scepterCanSwitchToBurst(@Nullable Unit unit){
        return isThor(unit) && !scepterIsSwitching(unit) && getScepterModeData(unit).impactMode;
    }

    public static boolean commandScepterAirMode(@Nullable Unit unit, boolean impactMode){
        if(!isThor(unit)) return false;
        ScepterModeData data = getScepterModeData(unit);
        if(data.switching || data.impactMode == impactMode) return false;

        data.switching = true;
        data.switchToImpact = impactMode;
        data.switchTime = scepterSwitchTime;
        unit.apply(StatusEffects.scepterSwitching, scepterSwitchTime);
        return true;
    }

    public static void updateScepterAirMode(@Nullable Unit unit){
        if(!isThor(unit)) return;
        ScepterModeData data = getScepterModeData(unit);
        if(!data.switching){
            if(unit.hasEffect(StatusEffects.scepterSwitching)){
                unit.unapply(StatusEffects.scepterSwitching);
            }
            return;
        }

        data.switchTime = Math.max(0f, data.switchTime - Time.delta);
        if(data.switchTime <= 0.001f){
            data.switching = false;
            data.impactMode = data.switchToImpact;
            unit.unapply(StatusEffects.scepterSwitching);
        }
    }

    public static float hurricaneBaseRange(){
        return hurricaneBaseRangeTiles * tilesize;
    }

    public static float hurricaneLockRange(){
        return hurricaneLockRangeTiles * tilesize;
    }

    public static float hurricaneLockDuration(){
        return hurricaneLockTime;
    }

    public static float hurricaneLockCooldownDuration(){
        return hurricaneLockCooldown;
    }

    public static float hurricaneLockFlashDuration(){
        return hurricaneLockFlashDuration;
    }

    public static HurricaneLockData getHurricaneLockData(@Nullable Unit unit){
        if(unit == null){
            return new HurricaneLockData();
        }
        HurricaneLockData data = hurricaneLockData.get(unit.id);
        if(data == null){
            data = new HurricaneLockData();
            hurricaneLockData.put(unit.id, data);
        }
        return data;
    }

    public static void clearHurricaneLockData(@Nullable Unit unit){
        if(unit == null) return;
        hurricaneLockData.remove(unit.id);
    }

    public static PreceptSiegeData getPreceptSiegeData(@Nullable Unit unit){
        if(unit == null){
            return new PreceptSiegeData();
        }
        PreceptSiegeData data = preceptSiegeData.get(unit.id);
        if(data == null){
            data = new PreceptSiegeData();
            preceptSiegeData.put(unit.id, data);
        }
        return data;
    }

    public static void clearPreceptSiegeData(@Nullable Unit unit){
        if(unit == null) return;
        preceptSiegeData.remove(unit.id);
    }

    public static float preceptTransitionDuration(){
        return preceptSiegeTransitionTime;
    }

    public static float preceptSiegeShotCooldownDuration(){
        return preceptSiegeReload;
    }

    public static float preceptSiegeFlashDuration(){
        return preceptSiegeFlashDuration;
    }

    public static float preceptMobileRange(){
        return preceptMobileRangeTiles * tilesize;
    }

    public static float preceptSiegeRange(){
        return preceptSiegeRangeTiles * tilesize;
    }

    public static boolean preceptIsSieging(@Nullable Unit unit){
        return isSiegeTank(unit) && unit.hasEffect(StatusEffects.preceptSieging);
    }

    public static boolean preceptIsSieged(@Nullable Unit unit){
        return isSiegeTank(unit) && unit.hasEffect(StatusEffects.preceptSieged) && !preceptIsSieging(unit) && !preceptIsUnsieging(unit);
    }

    public static boolean preceptIsUnsieging(@Nullable Unit unit){
        return isSiegeTank(unit) && unit.hasEffect(StatusEffects.preceptUnsieging);
    }

    public static boolean preceptCanEnterSiege(@Nullable Unit unit){
        return isSiegeTank(unit) && !preceptIsSieged(unit) && !preceptIsSieging(unit) && !preceptIsUnsieging(unit);
    }

    public static boolean preceptCanExitSiege(@Nullable Unit unit){
        return preceptIsSieged(unit) && !preceptIsSieging(unit) && !preceptIsUnsieging(unit);
    }

    public static float preceptTransitionProgress(@Nullable Unit unit){
        if(preceptIsSieging(unit)){
            return Mathf.clamp(1f - unit.getDuration(StatusEffects.preceptSieging) / preceptSiegeTransitionTime);
        }
        if(preceptIsUnsieging(unit)){
            return Mathf.clamp(unit.getDuration(StatusEffects.preceptUnsieging) / preceptSiegeTransitionTime);
        }
        return preceptIsSieged(unit) ? 1f : 0f;
    }

    public static float preceptSiegeCooldown(@Nullable Unit unit){
        if(!isSiegeTank(unit)) return 0f;
        return getPreceptSiegeData(unit).cooldown;
    }

    public static float preceptSiegeFlash(@Nullable Unit unit){
        if(!isSiegeTank(unit)) return 0f;
        return getPreceptSiegeData(unit).flash;
    }

    public static void markPreceptSiegeShot(@Nullable Unit unit){
        if(!isSiegeTank(unit)) return;
        PreceptSiegeData data = getPreceptSiegeData(unit);
        data.cooldown = preceptSiegeReload;
        data.flash = 0f;
    }

    public static void updatePreceptSiegeTimers(@Nullable Unit unit){
        if(!isSiegeTank(unit)) return;
        PreceptSiegeData data = getPreceptSiegeData(unit);
        float prev = data.cooldown;
        if(data.cooldown > 0f){
            data.cooldown = Math.max(0f, data.cooldown - Time.delta);
        }
        if(prev > 0.001f && data.cooldown <= 0.001f){
            data.flash = preceptSiegeFlashDuration;
        }
        if(data.flash > 0f){
            data.flash = Math.max(0f, data.flash - Time.delta);
        }
    }

    public static void commandPreceptSiege(@Nullable Unit unit, boolean siege){
        if(!isSiegeTank(unit)) return;
        if(siege){
            if(!preceptCanEnterSiege(unit)) return;
            PreceptSiegeData data = getPreceptSiegeData(unit);
            data.cooldown = 0f;
            data.flash = 0f;
            unit.unapply(StatusEffects.preceptUnsieging);
            unit.unapply(StatusEffects.preceptSieged);
            unit.apply(StatusEffects.preceptSieging, preceptSiegeTransitionTime);
        }else{
            if(!preceptCanExitSiege(unit)) return;
            unit.unapply(StatusEffects.preceptSieging);
            unit.unapply(StatusEffects.preceptSieged);
            unit.apply(StatusEffects.preceptUnsieging, preceptSiegeTransitionTime);
        }
    }

    public static @Nullable Teamc resolveTarget(int targetId){
        if(targetId < 0) return null;
        Syncc entity = Groups.sync.getByID(targetId);
        if(entity instanceof Teamc target){
            if(target instanceof Healthc health && !health.isValid()) return null;
            return target;
        }
        return null;
    }

    public static @Nullable Teamc hurricaneTarget(@Nullable Unit unit){
        if(!isHurricane(unit)) return null;
        HurricaneLockData data = getHurricaneLockData(unit);
        if(data.activeTime <= 0.001f || data.targetId < 0) return null;
        Teamc target = resolveTarget(data.targetId);
        if(target == null || Units.invalidateTarget(target, unit, hurricaneLockRange())) return null;
        return target;
    }

    public static boolean hurricaneLockActive(@Nullable Unit unit){
        return hurricaneTarget(unit) != null;
    }

    public static float hurricaneLockCooldown(@Nullable Unit unit){
        if(!isHurricane(unit)) return 0f;
        return getHurricaneLockData(unit).cooldown;
    }

    public static float hurricaneLockFlash(@Nullable Unit unit){
        if(!isHurricane(unit)) return 0f;
        return getHurricaneLockData(unit).flash;
    }

    public static boolean hurricaneCanLock(@Nullable Unit unit){
        if(!isHurricane(unit)) return false;
        HurricaneLockData data = getHurricaneLockData(unit);
        return data.cooldown <= 0.001f && data.activeTime <= 0.001f;
    }

    public static @Nullable Teamc hurricaneFindTarget(@Nullable Unit unit){
        if(!isHurricane(unit)) return null;
        return Units.closestTarget(unit.team, unit.x, unit.y, hurricaneLockRange(), unit.hitSize / 2f,
        u -> u.checkTarget(true, true) && u.hittable(),
        b -> true);
    }

    public static boolean hurricaneHasTarget(@Nullable Unit unit){
        return hurricaneFindTarget(unit) != null;
    }

    public static boolean commandHurricaneLock(@Nullable Unit unit){
        if(!hurricaneCanLock(unit)) return false;
        Teamc target = hurricaneFindTarget(unit);
        if(target == null) return false;

        HurricaneLockData data = getHurricaneLockData(unit);
        data.targetId = target.id();
        data.activeTime = hurricaneLockTime;
        data.cooldown = hurricaneLockCooldown;
        data.flash = 0f;
        return true;
    }

    public static void updateHurricaneLock(@Nullable Unit unit){
        if(!isHurricane(unit)) return;

        HurricaneLockData data = getHurricaneLockData(unit);
        float prevCooldown = data.cooldown;
        if(data.cooldown > 0f){
            data.cooldown = Math.max(0f, data.cooldown - Time.delta);
        }
        if(prevCooldown > 0.001f && data.cooldown <= 0.001f){
            data.flash = hurricaneLockFlashDuration;
        }
        if(data.flash > 0f){
            data.flash = Math.max(0f, data.flash - Time.delta);
        }

        if(data.activeTime > 0f){
            data.activeTime = Math.max(0f, data.activeTime - Time.delta);
            Teamc target = resolveTarget(data.targetId);
            if(target == null || Units.invalidateTarget(target, unit, hurricaneLockRange())){
                data.activeTime = 0f;
                data.targetId = -1;
                return;
            }

            if(!Units.withinTargetRange(target, unit.x, unit.y, hurricaneLockRange(), unit.hitSize / 2f) || data.activeTime <= 0.001f){
                data.activeTime = 0f;
                data.targetId = -1;
            }
        }else{
            data.targetId = -1;
        }
    }

    public static boolean isBanshee(@Nullable Unit unit){
        return unit != null && horizon != null && unit.type == horizon;
    }

    public static boolean bansheeCloaked(@Nullable Unit unit){
        return isBanshee(unit) && unit.hasEffect(StatusEffects.bansheeCloak);
    }

    public static boolean bansheeCanToggleCloak(@Nullable Unit unit){
        if(!isBanshee(unit)) return false;
        if(bansheeCloaked(unit)) return true;
        return !ravenMatrixDisabled(unit)
            && unit.energy >= bansheeCloakCost
            && ravenTeamHasTechAddon(unit.team);
    }

    public static boolean commandBansheeCloak(@Nullable Unit unit){
        if(!isBanshee(unit)) return false;
        if(bansheeCloaked(unit)){
            unit.unapply(StatusEffects.bansheeCloak);
            return true;
        }
        if(ravenMatrixDisabled(unit) || unit.energy < bansheeCloakCost || !ravenTeamHasTechAddon(unit.team)) return false;
        unit.energy = Math.max(0f, unit.energy - bansheeCloakCost);
        unit.apply(StatusEffects.bansheeCloak, 1f);
        return true;
    }

    public static void updateBanshee(@Nullable Unit unit){
        if(!bansheeCloaked(unit)) return;
        unit.energy = Math.max(0f, unit.energy - bansheeCloakDrain * Time.delta / 60f);
        if(unit.energy <= 0.001f){
            unit.unapply(StatusEffects.bansheeCloak);
        }
    }

    public static boolean isBattlecruiser(@Nullable Unit unit){
        return unit != null && antumbra != null && unit.type == antumbra;
    }

    public static BattlecruiserData getBattlecruiserData(@Nullable Unit unit){
        if(unit == null){
            return new BattlecruiserData();
        }
        BattlecruiserData data = battlecruiserData.get(unit.id);
        if(data == null){
            data = new BattlecruiserData();
            battlecruiserData.put(unit.id, data);
        }
        return data;
    }

    public static void clearBattlecruiserData(@Nullable Unit unit){
        if(unit == null) return;
        battlecruiserData.remove(unit.id);
    }

    public static boolean battlecruiserHasYamatoTech(@Nullable Team team){
        return team != null
            && team.data().getCount(Blocks.surgeCrucible) > 0
            && ravenTeamHasTechAddon(team);
    }

    public static boolean battlecruiserYamatoCharging(@Nullable Unit unit){
        return isBattlecruiser(unit) && getBattlecruiserData(unit).yamatoCharging;
    }

    public static float battlecruiserYamatoChargeProgress(@Nullable Unit unit){
        if(!battlecruiserYamatoCharging(unit)) return 0f;
        return Mathf.clamp(getBattlecruiserData(unit).yamatoChargeTime / battlecruiserYamatoChargeTime);
    }

    public static boolean battlecruiserWarpCharging(@Nullable Unit unit){
        return isBattlecruiser(unit) && getBattlecruiserData(unit).warpCharging;
    }

    public static float battlecruiserWarpChargeProgress(@Nullable Unit unit){
        if(!battlecruiserWarpCharging(unit)) return 0f;
        return Mathf.clamp(getBattlecruiserData(unit).warpChargeTime / battlecruiserWarpChargeTime);
    }

    public static boolean battlecruiserWarping(@Nullable Unit unit){
        return isBattlecruiser(unit) && getBattlecruiserData(unit).warping;
    }

    public static boolean battlecruiserCanUseYamato(@Nullable Unit unit){
        if(!isBattlecruiser(unit)) return false;
        BattlecruiserData data = getBattlecruiserData(unit);
        return !ravenMatrixDisabled(unit)
            && !data.warping
            && !data.warpCharging
            && !data.yamatoCharging
            && data.yamatoCooldown <= 0.001f
            && battlecruiserHasYamatoTech(unit.team);
    }

    public static boolean battlecruiserCanUseWarp(@Nullable Unit unit){
        if(!isBattlecruiser(unit)) return false;
        BattlecruiserData data = getBattlecruiserData(unit);
        return !ravenMatrixDisabled(unit)
            && !data.warping
            && !data.warpCharging
            && !data.yamatoCharging
            && data.warpCooldown <= 0.001f;
    }

    private static @Nullable Teamc resolveBattlecruiserYamatoTarget(@Nullable Unit unit, BattlecruiserData data){
        if(unit == null) return null;
        Teamc target = null;
        if(data.yamatoTargetId >= 0){
            Unit targetUnit = Groups.unit.getByID(data.yamatoTargetId);
            if(targetUnit != null && targetUnit.isValid()){
                target = targetUnit;
            }
        }else if(data.yamatoBuildPos >= 0){
            Building build = world.build(data.yamatoBuildPos);
            if(build != null && build.isValid()){
                target = build;
            }
        }

        if(target == null) return null;
        if(target instanceof Healthc h && !h.isValid()) return null;
        return target;
    }

    public static boolean commandBattlecruiserYamato(@Nullable Unit unit, @Nullable Teamc target){
        if(!battlecruiserCanUseYamato(unit) || target == null) return false;

        BattlecruiserData data = getBattlecruiserData(unit);
        data.pendingYamato = true;
        data.yamatoCharging = false;
        data.yamatoChargeTime = 0f;
        data.yamatoTargetId = target instanceof Unit u ? u.id : -1;
        data.yamatoBuildPos = target instanceof Building b ? b.pos() : -1;
        return true;
    }

    public static boolean commandBattlecruiserWarp(@Nullable Unit unit, @Nullable Vec2 target){
        if(!battlecruiserCanUseWarp(unit) || target == null) return false;
        BattlecruiserData data = getBattlecruiserData(unit);
        data.pendingWarp = true;
        data.warpTarget.set(Mathf.clamp(target.x, 0f, Math.max(world.unitWidth() - tilesize, 0f)),
        Mathf.clamp(target.y, 0f, Math.max(world.unitHeight() - tilesize, 0f)));
        data.warpRotation = unit.within(data.warpTarget, 0.01f) ? unit.rotation : unit.angleTo(data.warpTarget);
        return true;
    }

    public static void updateBattlecruiser(@Nullable Unit unit){
        if(!isBattlecruiser(unit)) return;
        BattlecruiserData data = getBattlecruiserData(unit);

        if(data.warpAppearTime > 0f){
            data.warpAppearTime = Math.max(0f, data.warpAppearTime - Time.delta);
        }

        if(data.yamatoCooldown > 0f){
            data.yamatoCooldown = Math.max(0f, data.yamatoCooldown - Time.delta);
        }
        if(data.warpCooldown > 0f){
            data.warpCooldown = Math.max(0f, data.warpCooldown - Time.delta);
        }

        if(ravenMatrixDisabled(unit)){
            data.pendingYamato = false;
            data.pendingWarp = false;
            data.yamatoCharging = false;
            data.warpCharging = false;
            data.yamatoTargetId = -1;
            data.yamatoBuildPos = -1;
            return;
        }

        if(data.warping){
            for(WeaponMount mount : unit.mounts){
                mount.shoot = false;
                mount.target = null;
            }
            unit.isShooting = false;
            unit.vel.setZero();
            data.warpTransitTime = Math.max(0f, data.warpTransitTime - Time.delta);
            float fin = Mathf.clamp(1f - data.warpTransitTime / battlecruiserWarpTransitTime);
            if(!data.warpRippleTriggered && fin >= battlecruiserWarpEmergenceStart){
                data.warpRippleTriggered = true;
                float behind = Math.max(unit.hitSize * 0.75f, 16f);
                float bx = data.warpTarget.x - Angles.trnsx(data.warpRotation, behind);
                float by = data.warpTarget.y - Angles.trnsy(data.warpRotation, behind);
                battlecruiserWarpRippleEffect.at(bx, by, data.warpRotation - 90f, Color.valueOf("66ff9c"));
                if(Shaders.shockwave != null){
                    float ellipseRx = 46f;
                    float horizontal = Math.abs(Angles.trnsx(data.warpRotation, 1f)); //1 when near horizontal
                    // flatter ellipse; keep total thickness >= 2 tiles -> ry >= 1 tile
                    float ellipseRy = Math.max(tilesize, ellipseRx * Mathf.lerp(0.12f, 0.28f, 1f - horizontal));

                    // exactly two distortion entities: one ellipse lens + one sphere lens
                    Shaders.shockwave.addLensEllipse(bx, by, ellipseRx, ellipseRy, data.warpRotation - 90f, 18f, 1.05f);
                    Shaders.shockwave.addLensSphere(bx, by, 18f, 14f, 0.92f);
                }
            }
            if(data.warpTransitTime <= 0.001f){
                unit.set(data.warpTarget.x, data.warpTarget.y);
                unit.rotation(data.warpRotation);
                unit.snapInterpolation();
                data.warping = false;
                data.warpAppearTime = 0f;
            }
            return;
        }

        if(data.warpCharging){
            for(WeaponMount mount : unit.mounts){
                mount.shoot = false;
                mount.target = null;
            }
            unit.isShooting = false;
            unit.vel.setZero();
            unit.lookAt(data.warpTarget);
            data.warpRotation = unit.rotation;
            data.warpChargeTime += Time.delta;
            if(data.warpChargeTime >= battlecruiserWarpChargeTime){
                data.warpCharging = false;
                data.warpChargeTime = 0f;
                data.warping = true;
                data.warpTransitTime = battlecruiserWarpTransitTime;
                data.warpFrom.set(unit.x, unit.y);
                battlecruiserWarpDisintegrateEffect.at(unit.x, unit.y, unit.rotation, Color.valueOf("54ff8b"), Float.valueOf(unit.hitSize));
            }
            return;
        }

        if(data.pendingWarp){
            data.pendingWarp = false;
            data.pendingYamato = false;
            data.yamatoCharging = false;
            data.yamatoTargetId = -1;
            data.yamatoBuildPos = -1;
            data.warpChargeTime = 0f;
            data.warpCharging = true;
            data.warpCooldown = battlecruiserWarpCooldown;
            data.warpRippleTriggered = false;
            unit.lookAt(data.warpTarget);
            data.warpRotation = unit.rotation;
            return;
        }

        Teamc yamatoTarget = resolveBattlecruiserYamatoTarget(unit, data);
        if(data.yamatoCharging){
            if(yamatoTarget == null){
                data.yamatoCharging = false;
                data.yamatoChargeTime = 0f;
                data.yamatoTargetId = -1;
                data.yamatoBuildPos = -1;
                return;
            }

            for(WeaponMount mount : unit.mounts){
                mount.shoot = false;
                mount.target = null;
            }
            unit.isShooting = false;
            unit.vel.setZero();
            unit.lookAt(yamatoTarget);
            data.yamatoChargeTime += Time.delta;
            if(data.yamatoChargeTime >= battlecruiserYamatoChargeTime){
                data.yamatoCharging = false;
                data.yamatoChargeTime = 0f;
                data.yamatoTargetId = -1;
                data.yamatoBuildPos = -1;

                if(yamatoTarget.team() == unit.team){
                    if(yamatoTarget instanceof Healthc h){
                        h.damagePierce(240f);
                    }
                }else if(battlecruiserYamatoBullet != null){
                    battlecruiserYamatoBullet.create(unit, unit.team, unit.x, unit.y, unit.angleTo(yamatoTarget), 1f, 1f);
                }
                Fx.pointBeam.at(unit.x, unit.y, 0f, Color.valueOf("ff5d5d"), new Vec2(yamatoTarget.getX(), yamatoTarget.getY()));
            }
            return;
        }

        if(data.pendingYamato){
            if(yamatoTarget == null){
                data.pendingYamato = false;
                data.yamatoTargetId = -1;
                data.yamatoBuildPos = -1;
                return;
            }

            unit.lookAt(yamatoTarget);
            if(unit.within(yamatoTarget, battlecruiserYamatoRange)){
                data.pendingYamato = false;
                data.yamatoCharging = true;
                data.yamatoChargeTime = 0f;
                data.yamatoCooldown = battlecruiserYamatoCooldown;
            }else if(unit.controller() instanceof CommandAI ai){
                ai.command(UnitCommand.moveCommand);
                ai.commandTarget(yamatoTarget, false);
            }
        }
    }

    private static void drawBattlecruiserWarpGhost(Unit unit, BattlecruiserData data){
        Draw.z(Math.max(Layer.flyingUnit, unit.type.flyingLayer) + 0.4f);
        float fin = Mathf.clamp(1f - data.warpTransitTime / battlecruiserWarpTransitTime);
        TextureRegion ghostRegion = unit.type.region != null && unit.type.region.found() ? unit.type.region : unit.type.fullIcon;

        float emerge = Mathf.clamp((fin - battlecruiserWarpEmergenceStart) / (1f - battlecruiserWarpEmergenceStart));
        if(emerge > 0f){
            float eased = Interp.pow3Out.apply(emerge);
            float fast = Mathf.clamp(eased * 2f);
            float back = 42f * (1f - fast);
            float ghostX = data.warpTarget.x - Angles.trnsx(data.warpRotation, back);
            float ghostY = data.warpTarget.y - Angles.trnsy(data.warpRotation, back);

            // play emergence visuals before true placement (during warp transit)
            float scanFin = Mathf.clamp(emerge * 2f);
            drawBattlecruiserArrivalLensInner(unit, ghostX, ghostY, data.warpRotation, emerge);
            queueBattlecruiserAfterDraw(unit, ghostX, ghostY, data.warpRotation, scanFin, false);
        }

        Draw.reset();
    }

    private static float battlecruiserRegionWidth(TextureRegion region){
        return region.width * region.scale / 4f;
    }

    private static float battlecruiserRegionHeight(TextureRegion region){
        return region.height * region.scale / 4f;
    }

    private static void drawBattlecruiserSlicedRegion(TextureRegion source, float x, float y, float rotation, float reveal, float widthScale, float alpha){
        if(source == null || !source.found()) return;
        if(reveal <= 0.001f || alpha <= 0.001f) return;

        float drawW = battlecruiserRegionWidth(source) * widthScale;
        float drawH = battlecruiserRegionHeight(source);
        if(drawW <= 0.001f || drawH <= 0.001f) return;

        float r = Mathf.clamp(reveal);
        float vFront = source.v;
        float vBack = source.v2;
        float rot = rotation - 90f;

        Draw.color(1f, 1f, 1f, alpha);
        if(r >= 0.999f){
            Draw.rect(source, x, y, drawW, drawH, rot);
        }else{
            float vEdge = Mathf.lerp(vFront, vBack, r);
            Tmp.tr1.set(source);
            Tmp.tr1.set(source.u, vFront, source.u2, vEdge);

            float localForward = (0.5f - r * 0.5f) * drawH;
            float px = x + Angles.trnsx(rotation, localForward);
            float py = y + Angles.trnsy(rotation, localForward);
            Draw.rect(Tmp.tr1, px, py, drawW, drawH * r, rot);

            //bright scan edge so reveal is clearly visible
            float edgeForward = (0.5f - r) * drawH;
            float ex = x + Angles.trnsx(rotation, edgeForward);
            float ey = y + Angles.trnsy(rotation, edgeForward);
            Draw.blend(Blending.additive);
            Draw.color(0.40f, 1f, 0.46f, alpha * (0.32f + 0.36f * (1f - r)));
            Lines.stroke(1.1f);
            Lines.lineAngleCenter(ex, ey, rotation + 90f, drawW * 0.96f);
            Draw.blend();
            Draw.color();
        }
        Draw.color();
    }

    private static void drawBattlecruiserArrivalLensInner(Unit unit, float x, float y, float rotation, float fin){
        float p = Mathf.clamp(fin / 0.78f);
        float fade = Mathf.clamp(1f - fin / 0.92f);
        if(p <= 0.001f || fade <= 0.001f) return;

        float behind = Math.max(unit.hitSize * 0.75f, 16f);
        float bx = x - Angles.trnsx(rotation, behind);
        float by = y - Angles.trnsy(rotation, behind);
        float horizontal = Math.abs(Angles.trnsx(rotation, 1f));

        float e = Interp.pow3Out.apply(Mathf.clamp(p * 1.15f));
        float baseRx = 32f;
        float baseRy = Math.max(tilesize, baseRx * Mathf.lerp(0.11f, 0.24f, 1f - horizontal));
        float ellipseW = Mathf.lerp(baseRx * 1.1f, baseRx * 2.05f, e) * 2f;
        float ellipseH = Mathf.lerp(baseRy * 1.35f, baseRy * 0.68f, e) * 2f;

        TextureRegion softCircle = Core.atlas.find("circle-shadow", "circle");
        if(!softCircle.found()) return;

        Draw.z(Math.max(Layer.flyingUnit, unit.type.flyingLayer) + 0.22f);
        Draw.blend(Blending.additive);

        Draw.color(0.26f, 1f, 0.50f, (0.2f + 0.34f * (1f - e)) * fade);
        Draw.rect(softCircle, bx, by, ellipseW, ellipseH, rotation - 90f);

        Draw.color(0.96f, 1f, 0.96f, (0.16f + 0.24f * (1f - e)) * fade);
        Draw.rect(softCircle, bx, by, ellipseW * 0.54f, ellipseH * 0.54f, rotation - 90f);

        float sphereP = Interp.pow2Out.apply(p);
        float fogRadius = Mathf.lerp(4.8f, 16.5f, sphereP);
        int fogCount = Math.max(10, (int)Mathf.lerp(10f, 34f, sphereP));

        Fx.rand.setSeed(((long)unit.id << 32) ^ ((long)(fin * 1000f) * 131L + 17L));
        for(int i = 0; i < fogCount; i++){
            float ang = Fx.rand.random(360f);
            float rr = fogRadius * Mathf.sqrt(Fx.rand.random(1f));
            float layer = Fx.rand.random(1f);

            float px = bx + Angles.trnsx(ang, rr);
            float py = by + Angles.trnsy(ang, rr);
            float size = Fx.rand.random(0.35f, 1.24f) * (0.6f + (1f - layer) * 0.8f);
            float alpha = (0.025f + 0.11f * (1f - rr / fogRadius)) * fade * Fx.rand.random(0.5f, 1f);

            Draw.color(0.34f, 1f, 0.58f, alpha);
            Fill.circle(px, py, size);
            if((i & 3) == 0){
                Draw.color(0.62f, 1f, 0.82f, alpha * 0.5f);
                Fill.circle(px, py, size * 0.45f);
            }
        }

        Draw.blend();
        Draw.reset();
    }

    private static void drawBattlecruiserArrivalStrips(Unit unit, float x, float y, float rotation, float fin){
        float p = Mathf.clamp(fin / 0.72f);
        // particles should end before final scan lock-in
        float lifeFade = Mathf.clamp((0.78f - p) / 0.28f);
        if(p <= 0.001f || lifeFade <= 0.001f) return;

        TextureRegion white = Core.atlas.find("whiteui");
        if(!white.found()) return;

        float behind = Math.max(unit.hitSize * 0.75f, 16f);
        float bx = x - Angles.trnsx(rotation, behind * (1f - p * 0.16f));
        float by = y - Angles.trnsy(rotation, behind * (1f - p * 0.16f));
        int particles = 40;
        float drawRot = rotation - 90f;

        Draw.z(Math.max(Layer.flyingUnit, unit.type.flyingLayer) + 0.31f);

        for(int i = 0; i < particles; i++){
            long seed = (long)unit.id * 1315423911L + i * 998244353L;
            Fx.rand.setSeed(seed);

            // fixed particle set, no looping regeneration
            float start = Fx.rand.random(0f, 0.34f);
            if(p < start) continue;

            float local = Mathf.clamp((p - start) / Math.max(0.001f, 0.78f - start));
            if(local >= 1f) continue;

            boolean slowGroup = Fx.rand.random(1f) < 0.58f;
            float speed = slowGroup ? Fx.rand.random(0.03f, 0.09f) : Fx.rand.random(0.70f, 1.45f);
            float alongBase = Fx.rand.random(-16f, -2f);
            float along = alongBase + local * speed * 22f;

            // single merged stream around center axis (not split into two lanes)
            float lane = Fx.rand.range(2.8f);
            float wave = Mathf.sin(Time.time * (0.06f + speed * 0.02f) + Fx.rand.random(Mathf.PI2)) * Fx.rand.random(0.03f, 0.22f);

            // stronger length contrast: very short squares + clearly long sticks
            boolean shortShape = Fx.rand.random(1f) < 0.56f;
            float width = Fx.rand.random(2.0f, 3.8f);
            float length = shortShape ? Fx.rand.random(0.7f, 1.35f) : (width + Fx.rand.random(0f, 3.8f));
            float alpha = (0.12f + 0.26f * (1f - local)) * lifeFade;

            float localSide = lane + wave;
            float px = bx + Angles.trnsx(rotation, along) + Angles.trnsx(rotation + 90f, localSide);
            float py = by + Angles.trnsy(rotation, along) + Angles.trnsy(rotation + 90f, localSide);

            // glowing green border
            Draw.blend(Blending.additive);
            Draw.color(0.34f, 1f, 0.42f, alpha * 0.35f);
            Lines.stroke(Math.max(0.147f, Math.min(width, length) * 0.077f));
            drawBattlecruiserRectOutline(px, py, width + 1.35f, length + 1.35f, drawRot);

            Draw.blend();
            Draw.color(0.36f, 1f, 0.46f, alpha * 0.92f);
            Lines.stroke(Math.max(0.112f, Math.min(width, length) * 0.056f));
            drawBattlecruiserRectOutline(px, py, width + 0.88f, length + 0.88f, drawRot);

            // almost transparent green interior
            Draw.color(0.24f, 1f, 0.34f, alpha * 0.028f);
            Draw.rect(white, px, py, Math.max(0.16f, width - 1.62f), Math.max(0.16f, length - 1.62f), drawRot);
        }

        Draw.blend();
        Draw.reset();
    }

    private static void drawBattlecruiserRectOutline(float x, float y, float width, float height, float rotation){
        float hw = width * 0.5f, hh = height * 0.5f;
        float cos = Mathf.cosDeg(rotation), sin = Mathf.sinDeg(rotation);

        float x1 = x + (-hw) * cos - (-hh) * sin, y1 = y + (-hw) * sin + (-hh) * cos;
        float x2 = x + ( hw) * cos - (-hh) * sin, y2 = y + ( hw) * sin + (-hh) * cos;
        float x3 = x + ( hw) * cos - ( hh) * sin, y3 = y + ( hw) * sin + ( hh) * cos;
        float x4 = x + (-hw) * cos - ( hh) * sin, y4 = y + (-hw) * sin + ( hh) * cos;

        Lines.line(x1, y1, x2, y2);
        Lines.line(x2, y2, x3, y3);
        Lines.line(x3, y3, x4, y4);
        Lines.line(x4, y4, x1, y1);
    }

    private static void drawBattlecruiserMaterialization(Unit unit, float x, float y, float rotation, float fin, boolean drawWeapons){
        TextureRegion bodyRegion = unit.type.region != null && unit.type.region.found() ? unit.type.region : unit.type.fullIcon;
        TextureRegion outlineRegion = unit.type.outlineRegion != null && unit.type.outlineRegion.found() ? unit.type.outlineRegion : bodyRegion;
        TextureRegion cellRegion = unit.type.cellRegion != null && unit.type.cellRegion.found() ? unit.type.cellRegion : bodyRegion;

        float reveal = Mathf.clamp((fin - battlecruiserMaterializeFrontDelay) / battlecruiserMaterializeFrontDuration);
        float widthScale = Mathf.lerp(0.42f, 1f, Interp.pow3Out.apply(Mathf.clamp(fin / 0.52f)));
        float z = Math.max(Layer.flyingUnit, unit.type.flyingLayer) + 0.52f;

        Draw.z(Math.min(z - 0.01f, Layer.bullet - 1f));
        TextureRegion softCircle = Core.atlas.find("circle-shadow", "circle");
        if(softCircle.found()){
            float shadowW = battlecruiserRegionWidth(bodyRegion) * widthScale * 1.32f;
            float shadowH = battlecruiserRegionHeight(bodyRegion) * 0.55f;
            Draw.color(0f, 0f, 0f, Mathf.lerp(0.08f, 0.22f, fin));
            Draw.rect(softCircle, x, y, shadowW, shadowH, rotation - 90f);
            Draw.color();
        }
        Draw.z(z);

        float bodyW = battlecruiserRegionWidth(bodyRegion) * widthScale;
        float bodyH = battlecruiserRegionHeight(bodyRegion);

        drawBattlecruiserSlicedRegion(bodyRegion, x, y, rotation, reveal, widthScale, 1f);
        drawBattlecruiserSlicedRegion(outlineRegion, x, y, rotation, reveal, widthScale, 0.95f);
        drawBattlecruiserSlicedRegion(cellRegion, x, y, rotation, reveal, widthScale, 0.25f + 0.45f * reveal);

        float centerGlow = Mathf.clamp(1f - fin / 0.5f);
        if(centerGlow > 0.001f){
            Draw.z(z + 0.01f);
            Draw.blend(Blending.additive);
            Draw.color(0.42f, 1f, 0.66f, 0.22f * centerGlow);
            Lines.stroke((1.6f + 2.6f * centerGlow) * widthScale);
            Lines.lineAngleCenter(x, y, rotation, bodyH * 0.9f);
            Draw.color(0.78f, 1f, 0.9f, 0.16f * centerGlow);
            Fill.circle(x, y, (1.2f + 1.7f * centerGlow) * widthScale);
            Draw.blend();
            Draw.reset();
        }

        float weaponAlpha = drawWeapons ? Mathf.clamp((reveal - 0.55f) / 0.45f) : 0f;
        if(drawWeapons && weaponAlpha > 0.001f){
            Draw.alpha(weaponAlpha);
            unit.type.drawWeaponOutlines(unit);
            if(unit.type.engines.size > 0){
                unit.type.drawEngines(unit);
            }
            unit.type.drawWeapons(unit);
            if(unit.type.drawItems){
                unit.type.drawItems(unit);
            }
            unit.type.drawLight(unit);
            Draw.alpha(1f);
        }
    }

    private static void drawBattlecruiserOverlay(Unit unit){
        if(!isBattlecruiser(unit)) return;
        BattlecruiserData data = getBattlecruiserData(unit);

        if(data.warping){
            drawBattlecruiserWarpGhost(unit, data);
            return;
        }

        if(data.warpCharging){
            Draw.z(Layer.effect);
            float fin = Mathf.clamp(data.warpChargeTime / battlecruiserWarpChargeTime);
            TextureRegion bodyRegion = unit.type.region != null && unit.type.region.found() ? unit.type.region : unit.type.fullIcon;

            if(fin < 0.4f){
                float phase = fin / 0.4f;
                int countL = battlecruiserSpotMaskLeft.size / 2;
                int countR = battlecruiserSpotMaskRight.size / 2;
                if(countL > 0 && countR > 0){
                    float drawW = bodyRegion.width * bodyRegion.scale / 4f;
                    float drawH = bodyRegion.height * bodyRegion.scale / 4f;
                    float rot = unit.rotation - 90f;
                    float cos = Mathf.cosDeg(rot), sin = Mathf.sinDeg(rot);
                    float time = Time.time * 5.4f;
                    int pairs = Math.min(countL, countR);
                    int spotCount = Math.min(6, pairs);

                    for(int i = 0; i < spotCount; i++){
                        float seed = unit.id * 23.17f + i * 13.73f;
                        int base = (int)(Math.abs(Mathf.sin(time * (2.2f + i * 0.12f) + seed * 1.31f)) * 100000f);
                        int idx = (base + i * 47) % pairs;

                        float uR = battlecruiserSpotMaskRight.items[idx * 2];
                        float vR = battlecruiserSpotMaskRight.items[idx * 2 + 1];
                        float lxBase = (uR - 0.5f) * drawW;
                        float lyBase = (vR - 0.5f) * drawH;

                        //wave-like symmetric motion
                        float waveSide = Mathf.sin(time * (3.4f + i * 0.15f) + seed) * drawW * 0.012f;
                        float waveForward = Mathf.sin(time * (4.8f + i * 0.19f) + seed * 0.77f) * drawH * 0.048f;

                        float lxR = lxBase + waveSide;
                        float ly = lyBase + waveForward;
                        float lxL = -lxBase - waveSide;

                        float pxL = unit.x + lxL * cos - ly * sin;
                        float pyL = unit.y + lxL * sin + ly * cos;
                        float pxR = unit.x + lxR * cos - ly * sin;
                        float pyR = unit.y + lxR * sin + ly * cos;

                        float t = 1f - i / (float)Math.max(spotCount, 1);
                        float size = Mathf.lerp(1.95f, battlecruiserSpotMaxWorldRadius, t) * (0.95f + phase * 0.16f);
                        float a = Mathf.lerp(0.3f, 0.6f, t);

                        Draw.color(0.52f, 0.78f, 1f, a);
                        Fill.circle(pxR, pyR, size);
                        Fill.circle(pxL, pyL, size);

                        Draw.color(1f, 1f, 1f, a * 0.68f);
                        Fill.circle(pxR, pyR, size * 0.43f);
                        Fill.circle(pxL, pyL, size * 0.43f);
                    }
                }
            }else{
                // after 0.4s: stable blue transparent shell with stronger edge opacity
                Draw.color(0.36f, 0.62f, 1f, 0.16f);
                Draw.rect(bodyRegion, unit.x, unit.y, unit.rotation - 90f);
                Draw.color(0.52f, 0.74f, 1f, 0.1f);
                Draw.rect(bodyRegion, unit.x, unit.y, unit.rotation - 90f);

                TextureRegion edge = unit.type.outlineRegion != null && unit.type.outlineRegion.found() ? unit.type.outlineRegion : bodyRegion;
                Draw.color(0.58f, 0.8f, 1f, 0.36f);
                Draw.rect(edge, unit.x, unit.y, unit.rotation - 90f);
            }
            Draw.reset();
        }

        if(data.yamatoCharging){
            float progress = Mathf.clamp(data.yamatoChargeTime / battlecruiserYamatoChargeTime);
            float width = Math.max(34f, unit.hitSize * 1.2f);
            float height = 4f;
            float bx = unit.x - width / 2f;
            float by = unit.y + unit.hitSize / 2f + 10f;
            Draw.z(Layer.effect + 0.1f);
            Draw.color(0f, 0f, 0f, 0.55f);
            Fill.rect(unit.x, by, width, height);
            Draw.color(Color.valueOf("66e7ff"));
            Fill.rect(bx + width * progress / 2f, by, width * progress, height - 0.6f);
            Draw.reset();
        }
    }

    public static boolean isMedivac(@Nullable Unit unit){
        return unit != null && mega != null && unit.type == mega;
    }

    public static float medivacAfterburnerDuration(){
        return medivacAfterburnerDuration;
    }

    public static float medivacBaseSpeed(){
        return medivacBaseSpeed;
    }

    public static float medivacAfterburnerBonusSpeed(){
        return medivacAfterburnerBonusSpeed;
    }

    public static float medivacLoadRange(){
        return medivacLoadRange;
    }

    public static boolean medivacAfterburnerActive(@Nullable Unit unit){
        return isMedivac(unit) && unit.hasEffect(StatusEffects.medivacAfterburner);
    }

    public static void commandMedivacAfterburner(@Nullable Unit unit){
        if(!isMedivac(unit)) return;
        unit.apply(StatusEffects.medivacAfterburner, medivacAfterburnerDuration);
    }

    public static void setMedivacMovingUnload(@Nullable Unit unit, boolean enabled){
        if(!isMedivac(unit)) return;
        if(enabled){
            medivacMovingUnload.add(unit.id);
        }else{
            medivacMovingUnload.remove(unit.id);
        }
    }

    public static boolean medivacMovingUnload(@Nullable Unit unit){
        return isMedivac(unit) && medivacMovingUnload.contains(unit.id);
    }

    public static void clearMedivacData(@Nullable Unit unit){
        if(unit == null) return;
        medivacMovingUnload.remove(unit.id);
    }

    public static float ravenAntiArmorDuration(){
        return ravenAntiArmorDuration;
    }

    public static float ravenMatrixDuration(){
        return ravenMatrixDuration;
    }

    public static RavenData getRavenData(@Nullable Unit unit){
        if(unit == null){
            return new RavenData();
        }
        RavenData data = ravenData.get(unit.id);
        if(data == null){
            data = new RavenData();
            ravenData.put(unit.id, data);
        }
        return data;
    }

    public static void clearRavenData(@Nullable Unit unit){
        if(unit == null) return;
        ravenData.remove(unit.id);
    }

    public static boolean isRaven(@Nullable Unit unit){
        return unit != null && avert != null && unit.type == avert;
    }

    public static boolean isRavenTurret(@Nullable Unit unit){
        return unit != null && ravenTurret != null && unit.type == ravenTurret;
    }

    public static boolean ravenMatrixDisabled(@Nullable Unit unit){
        return unit != null && unit.hasEffect(StatusEffects.ravenMatrixLock);
    }

    public static boolean ravenTeamHasTechAddon(@Nullable Team team){
        if(team == null) return false;
        for(Building build : Groups.build){
            if(build == null || !build.isValid() || build.team != team) continue;
            if(!(build instanceof UnitFactory.UnitFactoryBuild factory)) continue;
            if(factory.block != Blocks.shipFabricator) continue;
            if(factory.hasTechAddon()) return true;
        }
        return false;
    }

    public static boolean ravenCanDeployTurret(@Nullable Unit unit){
        return isRaven(unit)
            && !ravenMatrixDisabled(unit)
            && unit.energy >= ravenTurretCost;
    }

    public static boolean ravenCanUseAntiArmor(@Nullable Unit unit){
        return isRaven(unit)
            && !ravenMatrixDisabled(unit)
            && unit.energy >= ravenAntiArmorCost;
    }

    public static boolean ravenCanUseMatrix(@Nullable Unit unit){
        return isRaven(unit)
            && !ravenMatrixDisabled(unit)
            && unit.energy >= ravenMatrixCost
            && ravenTeamHasTechAddon(unit.team);
    }

    public static boolean ravenMatrixValidTarget(@Nullable Unit target, Team team){
        return target != null
            && target.isValid()
            && (target.type.unitClasses.contains(UnitClass.mechanical) || target.type.unitClasses.contains(UnitClass.psionic));
    }

    public static float ravenTurretLifeProgress(@Nullable Unit unit){
        if(!isRavenTurret(unit)) return 0f;
        return Mathf.clamp(unit.getDuration(StatusEffects.ravenTurretLifetime) / ravenTurretLifetime);
    }

    public static boolean commandRavenDeployTurret(@Nullable Unit unit){
        if(!ravenCanDeployTurret(unit) || ravenTurret == null) return false;

        unit.energy = Math.max(0f, unit.energy - ravenTurretCost);

        float spawnX = unit.x;
        float spawnY = unit.y;
        for(int i = 0; i < 12; i++){
            float angle = (Time.time + i * 31f) % 360f;
            float dist = ravenTurretDeployRange * (0.25f + i / 12f * 0.75f);
            float tx = Mathf.clamp(unit.x + Angles.trnsx(angle, dist), 0f, Math.max(world.unitWidth() - tilesize, 0f));
            float ty = Mathf.clamp(unit.y + Angles.trnsy(angle, dist), 0f, Math.max(world.unitHeight() - tilesize, 0f));
            Tile tile = world.tileWorld(tx, ty);
            if(tile != null && !tile.solid()){
                spawnX = tx;
                spawnY = ty;
                break;
            }
        }

        Unit turret = ravenTurret.create(unit.team);
        turret.set(spawnX, spawnY);
        turret.rotation(unit.rotation);
        turret.add();
        turret.apply(StatusEffects.ravenTurretLifetime, ravenTurretLifetime);
        Fx.spawn.at(spawnX, spawnY, 0f, unit.team.color);
        return true;
    }

    public static boolean commandRavenAntiArmor(@Nullable Unit unit, @Nullable Vec2 target){
        if(!ravenCanUseAntiArmor(unit) || target == null) return false;
        RavenData data = getRavenData(unit);
        data.pendingAntiArmor = true;
        data.pendingMatrix = false;
        data.matrixTargetId = -1;
        data.antiArmorTarget.set(target);
        return true;
    }

    public static boolean commandRavenMatrix(@Nullable Unit unit, @Nullable Unit target){
        if(!ravenCanUseMatrix(unit) || !ravenMatrixValidTarget(target, unit.team)) return false;
        RavenData data = getRavenData(unit);
        data.pendingMatrix = true;
        data.pendingAntiArmor = false;
        data.matrixTargetId = target.id;
        return true;
    }

    public static void updateRaven(@Nullable Unit unit){
        if(!isRaven(unit)) return;
        RavenData data = getRavenData(unit);

        if(ravenMatrixDisabled(unit)){
            data.pendingAntiArmor = false;
            data.pendingMatrix = false;
            data.matrixTargetId = -1;
            return;
        }

        if(data.pendingAntiArmor){
            if(!ravenCanUseAntiArmor(unit)){
                data.pendingAntiArmor = false;
            }else{
                Vec2 target = data.antiArmorTarget;
                unit.lookAt(target);

                if(unit.within(target, ravenAntiArmorRange)){
                    unit.energy = Math.max(0f, unit.energy - ravenAntiArmorCost);
                    float radius = ravenAntiArmorRadius;
                    Units.nearby((Team)null, target.x - radius, target.y - radius, radius * 2f, radius * 2f, other -> {
                        if(other == null || !other.isValid()) return;
                        if(!other.within(target, radius + other.hitSize / 2f)) return;
                        other.apply(StatusEffects.ravenAntiArmor, ravenAntiArmorDuration);
                    });
                    Fx.pointBeam.at(unit.x, unit.y, 0f, Color.valueOf("a84444"), target);
                    Fx.pointHit.at(target.x, target.y, 0f, Color.valueOf("a84444"));
                    data.pendingAntiArmor = false;
                }else if(unit.controller() instanceof CommandAI ai){
                    ai.command(UnitCommand.moveCommand);
                    ai.commandPosition(target, false);
                }
            }
        }

        if(data.pendingMatrix){
            Unit target = Groups.unit.getByID(data.matrixTargetId);
            if(!ravenCanUseMatrix(unit) || !ravenMatrixValidTarget(target, unit.team)){
                data.pendingMatrix = false;
                data.matrixTargetId = -1;
            }else{
                unit.lookAt(target);

                if(unit.within(target, ravenMatrixRange)){
                    unit.energy = Math.max(0f, unit.energy - ravenMatrixCost);
                    target.apply(StatusEffects.ravenMatrixLock, ravenMatrixDuration);
                    Fx.pointBeam.at(unit.x, unit.y, 0f, Color.valueOf("66b8ff"), target);
                    Fx.chainEmp.at(target.x, target.y, 0f, Color.valueOf("66b8ff"));
                    data.pendingMatrix = false;
                    data.matrixTargetId = -1;
                }else if(unit.controller() instanceof CommandAI ai){
                    ai.command(UnitCommand.moveCommand);
                    ai.commandPosition(Tmp.v1.set(target.x, target.y), false);
                }
            }
        }
    }

    public static boolean medivacCanHealTarget(@Nullable Unit target, Team team){
        return target != null
            && target.isValid()
            && target.team == team
            && target.damaged()
            && target.type.unitClasses.contains(UnitClass.biological);
    }

    public static @Nullable Unit medivacFindHealTarget(@Nullable Unit unit){
        if(!isMedivac(unit)) return null;
        return Units.closest(unit.team, unit.x, unit.y, medivacHealRange * 3f,
        u -> medivacCanHealTarget(u, unit.team));
    }

    public static int medivacUnitSlotCost(@Nullable UnitType type){
        if(type == null) return medivacMaxSlots;
        if(type == scepter) return 8;
        if(type == mace || type == hurricane || type == precept) return 4;
        if(type == ghost || type == fortress || type == locus || type == crawler) return 2;
        if(type == nova || type == dagger || type == reaper) return 1;
        return 1;
    }

    public static int medivacPayloadSlotsUsed(@Nullable Unit unit){
        if(!isMedivac(unit) || !(unit instanceof Payloadc payload)) return 0;
        return medivacPayloadSlotsUsed(payload.payloads());
    }

    public static int medivacPayloadSlotsUsed(@Nullable Seq<Payload> payloads){
        if(payloads == null || payloads.isEmpty()) return 0;
        int used = 0;
        for(Payload payload : payloads){
            if(!(payload instanceof UnitPayload up) || up.unit == null || up.unit.type == null) continue;
            used += medivacUnitSlotCost(up.unit.type);
        }
        return used;
    }

    public static int medivacPayloadSlotsFree(@Nullable Unit unit){
        return Math.max(0, medivacMaxSlots - medivacPayloadSlotsUsed(unit));
    }

    public static boolean medivacCanPickup(@Nullable Unit carrier, @Nullable Unit target){
        return medivacCanPickup(carrier, target, carrier instanceof Payloadc pay ? pay.payloads() : null);
    }

    public static boolean medivacCanPickup(@Nullable Unit carrier, @Nullable Unit target, @Nullable Seq<Payload> currentPayloads){
        if(!isMedivac(carrier) || target == null || !target.isValid()) return false;
        if(target == carrier) return false;
        if(target.team != carrier.team) return false;
        if(!target.isGrounded()) return false;
        if(!target.type.allowedInPayloads) return false;
        if(target.type == precept && preceptIsSieged(target)) return false;

        int used = medivacPayloadSlotsUsed(currentPayloads);
        int need = medivacUnitSlotCost(target.type);
        return used + need <= medivacMaxSlots;
    }

    //mech
    public static @EntityDef({Unitc.class, Mechc.class}) UnitType mace, dagger, reaper, crawler, fortress, ghost, scepter, reign, vela, ravenTurret;

    //mech, legacy
    public static @EntityDef(value = {Unitc.class, Mechc.class}, legacy = true) UnitType nova, pulsar, quasar;

    //legs
    public static @EntityDef({Unitc.class, Legsc.class}) UnitType corvus, atrax,
    merui, cleroi, anthicus,
    tecta, collaris;

    //legs, legacy
    public static @EntityDef(value = {Unitc.class, Legsc.class}, legacy = true) UnitType spiroct, arkyid, toxopid;

    //hover
    public static @EntityDef({Unitc.class, ElevationMovec.class}) UnitType elude;

    //air
    public static @EntityDef({Unitc.class}) UnitType flare, eclipse, horizon, zenith, antumbra,
    avert, obviate, liberator;

    //air, legacy
    public static @EntityDef(value = {Unitc.class}, legacy = true) UnitType mono;

    //air, legacy
    public static @EntityDef(value = {Unitc.class}, legacy = true) UnitType poly;

    //air + payload
    public static @EntityDef({Unitc.class, Payloadc.class}) UnitType coreFlyer, mega,
    evoke, incite, emanate, quell, disrupt;

    //air + payload, legacy
    public static @EntityDef(value = {Unitc.class, Payloadc.class}, legacy = true) UnitType quad;

    //air + payload + legacy (different branch)
    public static @EntityDef(value = {Unitc.class, Payloadc.class}, legacy = true) UnitType oct;

    //air, legacy
    public static @EntityDef(value = {Unitc.class}, legacy = true) UnitType alpha, beta, gamma;

    //naval
    public static @EntityDef({Unitc.class, WaterMovec.class}) UnitType risso, minke, bryde, sei, omura, retusa, oxynoe, cyerce, aegires, navanax;

    //special block unit type
    public static @EntityDef({Unitc.class, BlockUnitc.class}) UnitType block;

    //special internal unit for fog reveal
    public static @EntityDef({Unitc.class}) UnitType scanProbe;

    //special building tethered (has payload capability, because it's necessary sometimes)
    public static @EntityDef({Unitc.class, BuildingTetherc.class, Payloadc.class}) UnitType manifold, assemblyDrone;

    //tank
    public static @EntityDef({Unitc.class, Tankc.class}) UnitType stell, locus, precept, hurricane, vanquish, conquer;

    //endregion

    //missile definition, unused here but needed for codegen
    public static @EntityDef({Unitc.class, TimedKillc.class}) UnitType missile;

    //region neoplasm

    public static @EntityDef({Unitc.class, Crawlc.class}) UnitType latum, renale;

    //endregion

    public static void load(){
        //region ground attack

        dagger = new UnitType("dagger"){{
            speed = 3.15f;
            health = 45f;
            armor = 0f;
            rotateSpeed = 6f;
            omniMovement = true;
            rotateMoveFirst = false;
            range = maxRange = 5f * tilesize;
            targetAir = true;
            targetGround = true;
            armorType = ArmorType.light;
            unitClasses = EnumSet.of(UnitClass.biological);
            population = 1;

            weapons.add(new Weapon(){{
                reload = 0.61f * 60f;
                bullet = new PointBulletType(){
                    {
                        damage = 6f;
                        rangeOverride = 5f * tilesize;
                        shootEffect = Fx.none;
                        smokeEffect = Fx.none;
                        hitEffect = Fx.none;
                        despawnEffect = Fx.none;
                        trailEffect = Fx.none;
                    }

                @Override
                public void hitEntity(Bullet b, Hitboxc entity, float health){
                    Unit unit = entity instanceof Unit ? (Unit)entity : null;
                    float armor = unit != null ? unit.armor : 0f;
                    float effective = Math.max(b.damage - armor, 0.5f);
                    if(entity instanceof Healthc){
                        ((Healthc)entity).damagePierce(effective);
                    }
                    if(unit != null){
                        Tmp.v3.set(unit).sub(b).nor().scl(knockback * 80f);
                        if(impact) Tmp.v3.setAngle(b.rotation() + (knockback < 0 ? 180f : 0f));
                        unit.impulse(Tmp.v3);
                        unit.apply(status, statusDuration);
                        Events.fire(unitDamageEvent.set(unit, b));
                    }
                    handlePierce(b, health, entity.x(), entity.y());
                }
                };
            }});
        }};

        reaper = new UnitType("reaper"){{
            speed = 5.25f;
            health = 60f;
            armor = 0f;
            rotateSpeed = 6f;
            omniMovement = true;
            rotateMoveFirst = false;
            range = maxRange = 5f * tilesize;
            targetAir = false;
            targetGround = true;
            armorType = ArmorType.light;
            unitClasses = EnumSet.of(UnitClass.biological);
            population = 1;
            canPassWalls = true;
            regenDelay = 3f;
            regenRate = 2.5f;

            weapons.add(new Weapon(){{
                reload = 0.79f * 60f;
                shoot.shots = 2;
                bullet = new PointBulletType(){{
                    damage = 4f;
                    rangeOverride = 5f * tilesize;
                    shootEffect = Fx.none;
                    smokeEffect = Fx.none;
                    hitEffect = Fx.none;
                    despawnEffect = Fx.none;
                    trailEffect = Fx.none;
                }};
            }});
        }
        @Override
        public void load(){
            super.load();
            region = Core.atlas.find("alpha");
            outlineRegion = region;
            baseRegion = Core.atlas.find("nova-base", region);
            fullIcon = Core.atlas.find("unit-alpha-full", region);
            uiIcon = Core.atlas.find("unit-alpha-ui", fullIcon);
            shadowRegion = fullIcon;
            clipSize = Math.max(region.width * 2f, clipSize);
        }
        };

        mace = new UnitType("mace"){{
            speed = 3.15f;
            hitSize = 10f;
            health = 135f;
            armor = 0f;
            armorType = ArmorType.light;
            unitClasses = EnumSet.of(UnitClass.biological, UnitClass.mechanical);
            omniMovement = true;
            rotateMoveFirst = false;
            rotateSpeed = 6f; //360 deg/sec
            range = maxRange = 2f * tilesize;
            targetAir = false;
            targetGround = true;

                weapons.add(new Weapon("flamethrower"){
                private void applyConeDamage(Bullet b){
                    float coneRange = 2f * tilesize;
                    float halfAngle = 60f;
                    float damage = b.damage * b.damageMultiplier();

                    Units.nearbyEnemies(b.team, b.x - coneRange, b.y - coneRange, coneRange * 2f, coneRange * 2f, u -> {
                        if(!u.checkTarget(false, true) || !u.hittable()) return;
                        if(!u.within(b.x, b.y, coneRange + u.hitSize / 2f)) return;
                        if(!Angles.within(b.rotation(), b.angleTo(u), halfAngle)) return;

                        u.damage(damage);
                        Fx.hitFlameSmall.at(u.x, u.y);
                    });

                    Units.nearbyBuildings(b.x, b.y, coneRange + 8f, build -> {
                        if(build.team == b.team || !build.collide(b)) return;
                        if(!b.checkUnderBuild(build, build.x, build.y)) return;
                        if(Mathf.dst(b.x, b.y, build.x, build.y) > coneRange + build.hitSize() / 2f) return;
                        if(!Angles.within(b.rotation(), Angles.angle(b.x, b.y, build.x, build.y), halfAngle)) return;

                        build.damage(damage * b.type.buildingDamageMultiplier);
                        Fx.hitFlameSmall.at(build.x, build.y);
                    });
                }

                @Override
                protected void handleBullet(Unit unit, WeaponMount mount, Bullet bullet){
                    super.handleBullet(unit, mount, bullet);
                    applyConeDamage(bullet);
                }

                {
                    top = false;
                    shootSound = Sounds.shootFlame;
                    shootY = 2f;
                    rotate = true;
                    rotateSpeed = 6f; //360 deg/sec
                    mirror = false;
                    x = 0f;
                    y = 0f;
                    reload = 1.43f * 60f;
                    shoot.firstShotDelay = 0.5f * 60f;
                    shootCone = 60f;

                    bullet = new BulletType(0f, 18f){{
                        instantDisappear = true;
                        lifetime = 1f;
                        rangeOverride = 2f * tilesize;
                        collides = false;
                        collidesTiles = false;
                        collidesAir = false;
                        collidesGround = true;
                        keepVelocity = false;
                        hittable = false;
                        absorbable = false;
                        reflectable = false;
                        shootEffect = new Effect(24f, 96f, e -> {
                            float fin = e.fin();
                            float fout = e.fout();
                            float sweep = Mathf.lerp(-60f, 60f, fin);
                            float angle = e.rotation + sweep;
                            float flameLength = 2f * tilesize * (0.78f + fin * 0.22f);
                            float flameWidth = 0.5f * tilesize * fout;

                            Draw.color(Pal.lightFlame, Pal.darkFlame, fin);
                            Drawf.flame(e.x, e.y, 28, angle, flameLength, flameWidth, 0.35f);
                            Draw.color(Color.white, Pal.lightFlame, fin);
                            Drawf.flame(e.x, e.y, 18, angle, flameLength * 0.72f, flameWidth * 0.62f, 0.35f);
                            Draw.reset();
                        });
                        hitEffect = Fx.hitFlameSmall;
                        despawnEffect = Fx.none;
                    }};
                }
            });
        }};

        ravenTurret = new UnitType("raven-turret"){
            @Override
            public void update(Unit unit){
                super.update(unit);
                if(unit.getDuration(StatusEffects.ravenTurretLifetime) <= 0.001f){
                    unit.kill();
                }
            }

            {
                speed = 0f;
                accel = 0f;
                drag = 1f;
                hitSize = 9f;
                health = 140f;
                armor = 0f;
                armorType = ArmorType.heavy;
                unitClasses = EnumSet.of(UnitClass.mechanical);
                rotateSpeed = 6f;
                omniMovement = true;
                rotateMoveFirst = false;
                range = maxRange = 6f * tilesize;
                targetAir = true;
                targetGround = true;
                useUnitCap = false;
                isEnemy = false;
                hidden = true;
                deathExplosionEffect = Fx.blastExplosion;

                weapons.add(new Weapon("raven-turret-weapon"){{
                    x = 0f;
                    y = 0f;
                    shootY = 4f;
                    mirror = false;
                    rotate = true;
                    rotateSpeed = 6f;
                    shootCone = 12f;
                    reload = 0.57f * 60f;
                    shootSound = Sounds.shootDuo;

                    bullet = new BasicBulletType(7f, 18f){{
                        width = 7f;
                        height = 9f;
                        lifetime = 45f;
                        rangeOverride = 6f * tilesize;
                        collidesAir = true;
                        collidesGround = true;
                        shootEffect = Fx.shootSmall;
                        smokeEffect = Fx.shootSmallSmoke;
                        hitEffect = Fx.hitBulletColor;
                        despawnEffect = Fx.none;
                        trailLength = 5;
                        trailWidth = 1.2f;
                    }};
                }});
            }
        };

        fortress = new UnitType("fortress"){{
            rotateSpeed = 3f; // 180 deg/sec
            targetAir = false;
            speed = 3.15f;
            health = 125f;
            armor = 1f;
            rotateSpeed = 6f;
            omniMovement = true;
            rotateMoveFirst = false;
            range = maxRange = 6f * tilesize;
            targetGround = true;
            armorType = ArmorType.heavy;
            unitClasses = EnumSet.of(UnitClass.biological, UnitClass.heavy);
            population = 2;

            weapons.add(new Weapon(){{
                reload = 1.07f * 60f;
                bullet = new BasicBulletType(10f * tilesize / 60f, 10f){
                    float heavyDamage = 20f;

                    {
                        collides = false;
                        collidesTiles = false;
                        pierce = true;
                        pierceBuilding = true;
                        keepVelocity = true;
                        rangeOverride = 6f * tilesize;
                        lifetime = 60f;
                    }

                    @Override
                    public float buildingDamage(Bullet b){
                        return heavyDamage;
                    }

                    @Override
                    public void update(Bullet b){
                        super.update(b);
                        Teamc target = b.data instanceof Teamc ? (Teamc)b.data : null;
                        if(target == null) return;
                        if(target instanceof Healthc && !((Healthc)target).isValid()){
                            b.remove();
                            return;
                        }
                        if(target instanceof Teamc && ((Teamc)target).team() == b.team){
                            b.remove();
                            return;
                        }
                        if(target instanceof Position){
                            Position p = (Position)target;
                            b.vel.setAngle(Angles.moveToward(b.vel.angle(), b.angleTo(p), 0.08f * Time.delta * 50f));
                            b.vel.setLength(speed);
                            float hitRange = (target instanceof Sized ? ((Sized)target).hitSize() / 2f : 0f) + hitSize;
                            if(b.within(p, hitRange)){
                                if(target instanceof Unit){
                                    Unit u = (Unit)target;
                                    hitEntity(b, u, u.health());
                                }else if(target instanceof Building){
                                    Building build = (Building)target;
                                    if(build.team != b.team){
                                        build.collision(b);
                                        hit(b);
                                    }
                                }
                                b.remove();
                            }
                        }
                    }

                    @Override
                    public void hitEntity(Bullet b, Hitboxc entity, float health){
                        float prev = b.damage;
                        if(entity instanceof Unit && ((Unit)entity).type.armorType == ArmorType.heavy){
                            b.damage = heavyDamage;
                        }
                        super.hitEntity(b, entity, health);
                        b.damage = prev;
                    }
                };
            }
            @Override
            protected void bullet(Unit unit, WeaponMount mount, float xOffset, float yOffset, float angleOffset, Mover mover){
                if(!unit.isAdded()) return;

                mount.charging = false;
                float
                xSpread = Mathf.range(xRand),
                ySpread = Mathf.range(yRand),
                weaponRotation = unit.rotation - 90 + (rotate ? mount.rotation : baseRotation),
                mountX = unit.x + Angles.trnsx(unit.rotation - 90, x, y),
                mountY = unit.y + Angles.trnsy(unit.rotation - 90, x, y),
                bulletX = mountX + Angles.trnsx(weaponRotation, this.shootX + xOffset + xSpread, this.shootY + yOffset + ySpread),
                bulletY = mountY + Angles.trnsy(weaponRotation, this.shootX + xOffset + xSpread, this.shootY + yOffset + ySpread),
                shootAngle = bulletRotation(unit, mount, bulletX, bulletY) + angleOffset,
                lifeScl = bullet.scaleLife ? Mathf.clamp(Mathf.dst(bulletX, bulletY, mount.aimX, mount.aimY) / bullet.range) : 1f,
                angle = shootAngle + Mathf.range(inaccuracy + bullet.inaccuracy);

                Entityc shooter = unit;
                if(unit.controller() instanceof MissileAI){
                    shooter = ((MissileAI)unit.controller()).shooter;
                }
                mount.bullet = bullet.create(unit, shooter, unit.team, bulletX, bulletY, angle, -1f, (1f - velocityRnd) + Mathf.random(velocityRnd) + extraVelocity, lifeScl, mount.target, mover, mount.aimX, mount.aimY, mount.target);
            }
            });
        }};

        ghost = new UnitType("ghost"){{
            speed = 3.15f;
            health = 100f;
            armor = 0f;
            rotateSpeed = 6f;
            omniMovement = true;
            rotateMoveFirst = false;
            range = maxRange = 6f * tilesize;
            targetAir = true;
            targetGround = true;
            armorType = ArmorType.light;
            unitClasses = EnumSet.of(UnitClass.biological);
            population = 2;
            energyCapacity = 200f;
            energyInit = 20f;
            energyRegen = 1f;

            weapons.add(new Weapon(){{
                reload = 1.07f * 60f;
                bullet = new PointBulletType(){
                    {
                        damage = 10f;
                        rangeOverride = 6f * tilesize;
                        shootEffect = Fx.none;
                        smokeEffect = Fx.none;
                        hitEffect = Fx.none;
                        despawnEffect = Fx.none;
                        trailEffect = Fx.none;
                    }

                @Override
                public void hitEntity(Bullet b, Hitboxc entity, float health){
                    float prev = b.damage;
                    if(entity instanceof Unit && ((Unit)entity).type.armorType == ArmorType.light){
                        b.damage = 20f;
                    }
                    super.hitEntity(b, entity, health);
                    b.damage = prev;
                }
                };
            }});
        }
        @Override
        public void load(){
            super.load();
            region = Core.atlas.find("atrax");
            outlineRegion = region;
            baseRegion = Core.atlas.find("nova-base", region);
            fullIcon = Core.atlas.find("unit-atrax-full", region);
            uiIcon = Core.atlas.find("unit-atrax-ui", fullIcon);
            shadowRegion = fullIcon;
            clipSize = Math.max(region.width * 2f, clipSize);
        }
        };

        scepter = new UnitType("scepter"){
            @Override
            public void draw(Unit unit){
                float prevX = Draw.xscl, prevY = Draw.yscl;
                Draw.scl(prevX * 0.5f, prevY * 0.5f);
                super.draw(unit);
                Draw.scl(prevX, prevY);
            }

            @Override
            public void update(Unit unit){
                super.update(unit);
                updateScepterAirMode(unit);

                if(scepterIsSwitching(unit)){
                    unit.vel.setZero();
                    if(unit.controller() instanceof CommandAI ai){
                        ai.clearCommands();
                    }
                }
            }

            @Override
            public void killed(Unit unit){
                clearScepterModeData(unit);
            }

            {
                speed = 2.62f;
                hitSize = 0.9125f * tilesize;
                rotateSpeed = 6f; // 360 deg/sec
                omniMovement = true;
                rotateMoveFirst = false;
                health = 400f;
                armor = 1f;
                armorType = ArmorType.heavy;
                unitClasses = EnumSet.of(UnitClass.mechanical);
                range = maxRange = 10f * tilesize;
                targetAir = true;
                targetGround = true;
                requireBodyAimToShoot = true;
                population = 6;
                mechFrontSway = 1f;
                ammoType = new ItemAmmoType(Items.thorium);

                mechStepParticles = true;
                stepShake = 0.15f;
                singleTarget = true;
                drownTimeMultiplier = 1.5f;
                stepSound = Sounds.mechStep;
                stepSoundPitch = 0.9f;
                stepSoundVolume = 0.35f;

                weapons.add(
                new Weapon("scepter-mount"){
                @Override
                public void update(Unit unit, WeaponMount mount){
                    if(!scepterUsingBurstMode(unit)){
                        mount.shoot = false;
                        mount.rotate = false;
                        mount.warmup = Mathf.approachDelta(mount.warmup, 0f, 0.08f);
                        mount.heat = Mathf.approachDelta(mount.heat, 0f, 0.08f);
                        return;
                    }

                    if(!(mount.target instanceof Unit target) || !target.isFlying()){
                        mount.shoot = false;
                        mount.rotate = false;
                        mount.warmup = Mathf.approachDelta(mount.warmup, 0f, 0.08f);
                        mount.heat = Mathf.approachDelta(mount.heat, 0f, 0.08f);
                        return;
                    }

                    super.update(unit, mount);
                }

                @Override
                public void draw(Unit unit, WeaponMount mount){
                    if(!scepterDisplayBurstMode(unit)) return;
                    float prevX = Draw.xscl, prevY = Draw.yscl;
                    Draw.scl(prevX * 1.35f, prevY * 1.35f);
                    super.draw(unit, mount);
                    Draw.scl(prevX, prevY);
                }

                @Override
                public void drawOutline(Unit unit, WeaponMount mount){
                    if(!scepterDisplayBurstMode(unit)) return;
                    float prevX = Draw.xscl, prevY = Draw.yscl;
                    Draw.scl(prevX * 1.35f, prevY * 1.35f);
                    super.drawOutline(unit, mount);
                    Draw.scl(prevX, prevY);
                }

                @Override
                protected void handleBullet(Unit unit, WeaponMount mount, Bullet bullet){
                    super.handleBullet(unit, mount, bullet);
                    bullet.data = mount.target;
                }

                {
                    top = false;
                    y = 1f;
                    x = 10.5f;
                    shootY = 5f;
                    reload = 2.14f * 60f;
                    recoil = 0.5f;
                    rotate = false;
                    rotateSpeed = 6f; // 360 deg/sec
                    targetAir = true;
                    targetGround = false;
                    mirror = true;
                    alternate = false;
                    shootCone = 8f;
                    shootSound = Sounds.shootMissileLarge;
                    shootSoundVolume = 0.95f;
                    cooldownTime = 45f;

                    shoot = new ShootPattern(){{
                        shots = 4;
                        shotDelay = 3f;
                    }};

                    bullet = new MissileBulletType(8f, 6f, "missile-large"){
                        {
                            rangeOverride = 10f * tilesize;
                            width = 12f;
                            height = 20f;
                            lifetime = 35f;
                            hitSize = 6f;
                            homingPower = 0f;
                            weaveMag = 0f;
                            weaveScale = 0f;
                            hitColor = backColor = trailColor = Color.valueOf("feb380");
                            frontColor = Color.white;
                            trailWidth = 4f;
                            trailLength = 9;
                            hitEffect = despawnEffect = Fx.hitBulletColor;
                            shootEffect = Fx.shootSmall;
                            smokeEffect = Fx.shootSmallSmoke;

                            collides = false;
                            collidesTiles = false;
                            collidesAir = true;
                            collidesGround = false;
                            hittable = false;
                            absorbable = false;
                            reflectable = false;
                            keepVelocity = false;
                            despawnHit = false;

                            splashDamageRadius = 1f * tilesize;
                            splashDamage = 6f;
                            fragBullets = 0;
                        }

                        @Override
                        public void update(Bullet b){
                            Teamc target = b.data instanceof Teamc t ? t : null;

                            if(target instanceof Healthc h && !h.isValid()) target = null;
                            if(target != null && target.team() == b.team) target = null;
                            if(!(target instanceof Unit unit)){
                                b.remove();
                                return;
                            }

                            float tx = unit.x, ty = unit.y;
                            b.aimX = tx;
                            b.aimY = ty;
                            b.vel.setAngle(Angles.moveToward(b.rotation(), b.angleTo(tx, ty), 35f * Time.delta));
                            b.vel.setLength(speed);
                            b.rotation(b.vel.angle());

                            float hitRange = 4f + unit.hitSize / 2f;
                            if(Mathf.within(b.x, b.y, tx, ty, hitRange)){
                                hit(b, tx, ty);

                                float amount = damage;
                                if(unit.type.armorType == ArmorType.light){
                                    amount = 12f;
                                }
                                unit.damage(amount);

                                float radius = splashDamageRadius;
                                if(radius > 0f && splashDamage > 0f){
                                    Units.nearbyEnemies(b.team, tx - radius, ty - radius, radius * 2f, radius * 2f, other -> {
                                        if(other == unit || !other.isFlying()) return;
                                        if(!other.within(tx, ty, radius + other.hitSize / 2f)) return;
                                        other.damage(splashDamage);
                                    });
                                }

                                b.remove();
                            }
                        }
                    };
                }
            },

            new Weapon("disperse-mid"){
                @Override
                public void update(Unit unit, WeaponMount mount){
                    if(!scepterUsingImpactMode(unit)){
                        mount.shoot = false;
                        mount.rotate = false;
                        mount.warmup = Mathf.approachDelta(mount.warmup, 0f, 0.08f);
                        mount.heat = Mathf.approachDelta(mount.heat, 0f, 0.08f);
                        return;
                    }

                    if(!(mount.target instanceof Unit target) || !target.isFlying()){
                        mount.shoot = false;
                        mount.rotate = false;
                        mount.warmup = Mathf.approachDelta(mount.warmup, 0f, 0.08f);
                        mount.heat = Mathf.approachDelta(mount.heat, 0f, 0.08f);
                        return;
                    }

                    super.update(unit, mount);
                }

                @Override
                public void draw(Unit unit, WeaponMount mount){
                    if(!scepterDisplayImpactMode(unit)) return;
                    float prevX = Draw.xscl, prevY = Draw.yscl;
                    Draw.scl(prevX * 1.35f, prevY * 1.35f);
                    super.draw(unit, mount);
                    Draw.scl(prevX, prevY);
                }

                @Override
                public void drawOutline(Unit unit, WeaponMount mount){
                    if(!scepterDisplayImpactMode(unit)) return;
                    float prevX = Draw.xscl, prevY = Draw.yscl;
                    Draw.scl(prevX * 1.35f, prevY * 1.35f);
                    super.drawOutline(unit, mount);
                    Draw.scl(prevX, prevY);
                }

                @Override
                protected void handleBullet(Unit unit, WeaponMount mount, Bullet bullet){
                    super.handleBullet(unit, mount, bullet);
                    bullet.data = mount.target;
                }

                {
                    top = false;
                    y = -2f;
                    x = 10.5f;
                    shootY = 5f;
                    reload = 0.91f * 60f;
                    recoil = 0.5f;
                    rotate = false;
                    rotateSpeed = 6f; // 360 deg/sec
                    targetAir = true;
                    targetGround = false;
                    mirror = true;
                    alternate = false;
                    shootCone = 8f;
                    shootSound = Sounds.shootMissileLarge;
                    shootSoundVolume = 0.95f;
                    cooldownTime = 45f;

                    bullet = new MissileBulletType(10f, 25f, "missile-large"){
                        {
                            rangeOverride = 11f * tilesize;
                            width = 12f;
                            height = 20f;
                            lifetime = 35f;
                            hitSize = 6f;
                            homingPower = 0f;
                            weaveMag = 0f;
                            weaveScale = 0f;
                            hitColor = backColor = trailColor = Color.valueOf("ffd58f");
                            frontColor = Color.white;
                            trailWidth = 4f;
                            trailLength = 9;
                            hitEffect = despawnEffect = Fx.hitBulletColor;
                            shootEffect = Fx.shootSmall;
                            smokeEffect = Fx.shootSmallSmoke;

                            collides = false;
                            collidesTiles = false;
                            collidesAir = true;
                            collidesGround = false;
                            hittable = false;
                            absorbable = false;
                            reflectable = false;
                            keepVelocity = false;
                            despawnHit = false;

                            splashDamageRadius = 0.5f * tilesize;
                            splashDamage = 25f;
                            fragBullets = 0;
                        }

                        @Override
                        public void update(Bullet b){
                            Teamc target = b.data instanceof Teamc t ? t : null;

                            if(target instanceof Healthc h && !h.isValid()) target = null;
                            if(target != null && target.team() == b.team) target = null;
                            if(!(target instanceof Unit unit)){
                                b.remove();
                                return;
                            }

                            float tx = unit.x, ty = unit.y;
                            b.aimX = tx;
                            b.aimY = ty;
                            b.vel.setAngle(Angles.moveToward(b.rotation(), b.angleTo(tx, ty), 35f * Time.delta));
                            b.vel.setLength(speed);
                            b.rotation(b.vel.angle());

                            float hitRange = 4f + unit.hitSize / 2f;
                            if(Mathf.within(b.x, b.y, tx, ty, hitRange)){
                                hit(b, tx, ty);

                                float amount = damage;
                                if(unit.type.unitClasses.contains(UnitClass.heavy)){
                                    amount = 35f;
                                }
                                unit.damage(amount);

                                float radius = splashDamageRadius;
                                if(radius > 0f && splashDamage > 0f){
                                    Units.nearbyEnemies(b.team, tx - radius, ty - radius, radius * 2f, radius * 2f, other -> {
                                        if(other == unit || !other.isFlying()) return;
                                        if(!other.within(tx, ty, radius + other.hitSize / 2f)) return;
                                        other.damage(splashDamage);
                                    });
                                }

                                b.remove();
                            }
                        }
                    };
                }
            },

            new Weapon("scepter-weapon"){
                @Override
                public void update(Unit unit, WeaponMount mount){
                    if(scepterIsSwitching(unit)){
                        mount.shoot = false;
                        mount.rotate = false;
                        mount.warmup = Mathf.approachDelta(mount.warmup, 0f, 0.08f);
                        mount.heat = Mathf.approachDelta(mount.heat, 0f, 0.08f);
                        return;
                    }

                    if(mount.target instanceof Unit target && target.isFlying()){
                        mount.shoot = false;
                        mount.rotate = false;
                        mount.warmup = Mathf.approachDelta(mount.warmup, 0f, 0.08f);
                        mount.heat = Mathf.approachDelta(mount.heat, 0f, 0.08f);
                        return;
                    }

                    super.update(unit, mount);
                }

                @Override
                public void draw(Unit unit, WeaponMount mount){
                    float prevX = Draw.xscl, prevY = Draw.yscl;
                    Draw.scl(prevX * 1.35f, prevY * 1.35f);
                    super.draw(unit, mount);
                    Draw.scl(prevX, prevY);
                }

                @Override
                public void drawOutline(Unit unit, WeaponMount mount){
                    float prevX = Draw.xscl, prevY = Draw.yscl;
                    Draw.scl(prevX * 1.35f, prevY * 1.35f);
                    super.drawOutline(unit, mount);
                    Draw.scl(prevX, prevY);
                }

                {
                reload = 0.91f * 60f;
                x = 12.5f;
                y = 1f;
                shootY = 8f;
                rotate = false;
                rotateSpeed = 6f; // 360 deg/sec
                targetAir = false;
                targetGround = true;
                mirror = true;
                alternate = false;
                shootCone = 10f;
                shootSound = Sounds.shootScepterSecondary;
                cooldownTime = 25f;
                recoil = 0.9f;
                recoilTime = 18f;

                shoot = new ShootPattern(){{
                    shots = 2;
                    shotDelay = 3f;
                }};

                bullet = new PointBulletType(){
                    {
                        damage = 30f;
                        rangeOverride = 7f * tilesize;
                        collidesAir = false;
                        collidesGround = true;
                        hitEffect = Fx.none;
                        despawnEffect = Fx.none;
                        shootEffect = Fx.none;
                        smokeEffect = Fx.none;
                        trailEffect = Fx.none;
                    }
                };
                }
            }
                );
            }
        };

        reign = new UnitType("reign"){{
            speed = 3f;
            hitSize = 30f;
            rotateSpeed = 3f; // 180 deg/sec
            health = 24000;
            armor = 18f;
            mechStepParticles = true;
            stepShake = 0.75f;
            drownTimeMultiplier = 1.6f;
            mechFrontSway = 1.9f;
            mechSideSway = 0.6f;
            ammoType = new ItemAmmoType(Items.thorium);
            stepSound = Sounds.mechStepHeavy;
            stepSoundPitch = 0.9f;
            stepSoundVolume = 0.45f;

            weapons.add(
            new Weapon("reign-weapon"){{
                top = false;
                y = 1f;
                x = 21.5f;
                shootY = 11f;
                reload = 9f;
                recoil = 5f;
                shake = 2f;
                ejectEffect = Fx.casing4;
                shootSound = Sounds.shootReign;

                bullet = new BasicBulletType(13f, 80){{
                    pierce = true;
                    pierceCap = 10;
                    width = 14f;
                    height = 33f;
                    lifetime = 15f;
                    shootEffect = Fx.shootBig;
                    fragVelocityMin = 0.4f;

                    hitEffect = Fx.blastExplosion;
                    splashDamage = 18f;
                    splashDamageRadius = 13f;

                    fragBullets = 3;
                    fragLifeMin = 0f;
                    fragRandomSpread = 30f;
                    despawnSound = Sounds.explosion;

                    fragBullet = new BasicBulletType(9f, 20){{
                        width = 10f;
                        height = 10f;
                        pierce = true;
                        pierceBuilding = true;
                        pierceCap = 3;

                        lifetime = 20f;
                        hitEffect = Fx.flakExplosion;
                        splashDamage = 15f;
                        splashDamageRadius = 10f;
                    }};
                }};
            }}

            );
        }};

        //endregion
        //region ground support

        nova = new UnitType("nova"){{
            speed = 3.94f;
            accel = 10f;
            hitSize = 8f;
            health = 45f;
            armor = 1f;
            armorType = ArmorType.light;
            unitClasses = EnumSet.of(UnitClass.biological, UnitClass.mechanical);
            omniMovement = true;
            rotateMoveFirst = false;

            buildSpeed = 1f;
            commands = Seq.with(UnitCommand.moveCommand, UnitCommand.harvestCommand);

            ammoType = new PowerAmmoType(1000);

            weapons.add(new Weapon("heal-weapon"){{
                top = false;
                shootY = 2f;
                reload = 1.07f * 60f;
                x = 4.5f;
                alternate = false;
                ejectEffect = Fx.none;
                recoil = 1f;
                shootCone = 40f;
                shootSound = Sounds.shootSap;

                bullet = new ShrapnelBulletType(){{
                    length = 22f;
                    damage = 5f;
                    width = 10f;
                    serrations = 0;
                    fromColor = Pal.heal;
                    toColor = Color.white;
                    shootEffect = smokeEffect = Fx.none;
                }};
            }});
        }};

        pulsar = new UnitType("pulsar"){{
            speed = 3.94f;
            accel = 10f;
            hitSize = 11f;
            health = 320f;
            armor = 4f;

            mineTier = 2;
            mineSpeed = 0.32f;

            commands = Seq.with(UnitCommand.moveCommand, UnitCommand.harvestCommand);

            ammoType = new PowerAmmoType(1300);

            weapons.add(new Weapon("heal-shotgun-weapon"){{
                top = false;
                x = 5f;
                shake = 2.2f;
                y = 0.5f;
                shootY = 2.5f;

                reload = 36f;
                inaccuracy = 35;

                shoot.shots = 3;
                shoot.shotDelay = 0.5f;

                ejectEffect = Fx.none;
                recoil = 2.5f;
                shootSound = Sounds.shootPulsar;

                bullet = new LightningBulletType(){{
                    lightningColor = hitColor = Pal.heal;
                    damage = 15f;
                    lightningLength = 8;
                    lightningLengthRand = 7;
                    shootEffect = Fx.shootHeal;
                    //Does not actually do anything; Just here to make stats work
                    healPercent = 2f;

                    lightningType = new BulletType(0.0001f, 0f){{
                        lifetime = Fx.lightning.lifetime;
                        hitEffect = Fx.hitLancer;
                        despawnEffect = Fx.none;
                        status = StatusEffects.shocked;
                        statusDuration = 10f;
                        hittable = false;
                        healPercent = 1.6f;
                        collidesTeam = true;
                    }};
                }};
            }});
        }};

        quasar = new UnitType("quasar"){{
            boostMultiplier = 2f;
            health = 640f;
            buildSpeed = 1.1f;
            canBoost = true;
            armor = 9f;
            mechLandShake = 2f;
            riseSpeed = 0.05f;

            mechFrontSway = 0.55f;
            ammoType = new PowerAmmoType(1500);
            stepSound = Sounds.mechStepSmall;
            stepSoundPitch = 0.9f;
            stepSoundVolume = 0.6f;

            speed = 3.75f;
            hitSize = 13f;

            drawShields = false;

            weapons.add(new Weapon("beam-weapon"){{
                top = false;
                shake = 2f;
                shootY = 4f;
                x = 6.5f;
                reload = 55f;
                recoil = 4f;
                shootSound = Sounds.shootLancer;

                bullet = new LaserBulletType(){{
                    damage = 45f;
                    recoil = 0f;
                    sideAngle = 45f;
                    sideWidth = 1f;
                    sideLength = 70f;
                    healPercent = 10f;
                    collidesTeam = true;
                    length = 150f;
                    colors = new Color[]{Pal.heal.cpy().a(0.4f), Pal.heal, Color.white};
                }};
            }});
        }};

        vela = new UnitType("vela"){{
            hitSize = 24f;

            rotateSpeed = 3f; // 180 deg/sec
            mechFrontSway = 1f;
            buildSpeed = 3f;

            mechStepParticles = true;
            stepShake = 0.15f;
            ammoType = new PowerAmmoType(2500);
            drownTimeMultiplier = 1.3f;

            speed = 3.3f;
            boostMultiplier = 2.4f;
            engineOffset = 12f;
            engineSize = 6f;
            lowAltitude = true;
            riseSpeed = 0.02f;

            health = 8200f;
            armor = 9f;
            canBoost = true;
            mechLandShake = 4f;
            immunities = ObjectSet.with(StatusEffects.burning);

            singleTarget = true;
            stepSound = Sounds.mechStep;
            stepSoundPitch = 0.9f;
            stepSoundVolume = 0.25f;

            weapons.add(new Weapon("vela-weapon"){{
                mirror = false;
                top = false;
                shake = 4f;
                shootY = 14f;
                x = y = 0f;

                shoot.firstShotDelay = Fx.greenLaserChargeSmall.lifetime - 1f;
                parentizeEffects = true;

                reload = 155f;
                recoil = 0f;
                chargeSound = Sounds.chargeVela;
                shootSound = Sounds.beamPlasma;
                initialShootSound = Sounds.shootBeamPlasma;
                continuous = true;
                cooldownTime = 200f;

                bullet = new ContinuousLaserBulletType(){{
                    damage = 35f;
                    length = 180f;
                    hitEffect = Fx.hitMeltHeal;
                    drawSize = 420f;
                    lifetime = 160f;
                    shake = 1f;
                    despawnEffect = Fx.smokeCloud;
                    smokeEffect = Fx.none;

                    chargeEffect = Fx.greenLaserChargeSmall;

                    incendChance = 0.1f;
                    incendSpread = 5f;
                    incendAmount = 1;

                    //constant healing
                    healPercent = 1f;
                    collidesTeam = true;

                    colors = new Color[]{Pal.heal.cpy().a(.2f), Pal.heal.cpy().a(.5f), Pal.heal.cpy().mul(1.2f), Color.white};
                }};

                shootStatus = StatusEffects.slow;
                shootStatusDuration = bullet.lifetime + shoot.firstShotDelay;
            }});

            weapons.add(new RepairBeamWeapon("repair-beam-weapon-center-large"){{
                x = 44 / 4f;
                y = -30f / 4f;
                shootY = 6f;
                beamWidth = 0.8f;
                repairSpeed = 1.4f;

                bullet = new BulletType(){{
                    maxRange = 120f;
                }};
            }});
        }};

        corvus = new UnitType("corvus"){{
            hitSize = 29f;
            health = 18000f;
            armor = 9f;
            stepShake = 1.5f;
            rotateSpeed = 3f; // 180 deg/sec
            drownTimeMultiplier = 1.6f;

            stepSound = Sounds.walkerStep;
            stepSoundVolume = 1.1f;
            stepSoundPitch = 0.9f;

            legCount = 4;
            legLength = 14f;
            legBaseOffset = 11f;
            legMoveSpace = 1.5f;
            legForwardScl = 0.58f;
            hovering = true;
            shadowElevation = 0.2f;
            ammoType = new PowerAmmoType(4000);
            groundLayer = Layer.legUnit;

            speed = 2.25f;

            drawShields = false;

            weapons.add(new Weapon("corvus-weapon"){{
                shootSound = Sounds.shootCorvus;
                chargeSound = Sounds.chargeCorvus;
                soundPitchMin = 1f;
                top = false;
                mirror = false;
                shake = 14f;
                shootY = 5f;
                x = y = 0;
                reload = 350f;
                recoil = 0f;

                cooldownTime = 350f;

                shootStatusDuration = 60f * 2f;
                shootStatus = StatusEffects.unmoving;
                shoot.firstShotDelay = Fx.greenLaserCharge.lifetime;
                parentizeEffects = true;

                bullet = new LaserBulletType(){{
                    length = 460f;
                    damage = 560f;
                    width = 75f;

                    lifetime = 65f;

                    lightningSpacing = 35f;
                    lightningLength = 5;
                    lightningDelay = 1.1f;
                    lightningLengthRand = 15;
                    lightningDamage = 50;
                    lightningAngleRand = 40f;
                    largeHit = true;
                    lightColor = lightningColor = Pal.heal;

                    chargeEffect = Fx.greenLaserCharge;

                    healPercent = 25f;
                    collidesTeam = true;

                    sideAngle = 15f;
                    sideWidth = 0f;
                    sideLength = 0f;
                    colors = new Color[]{Pal.heal.cpy().a(0.4f), Pal.heal, Color.white};
                }};
            }});
        }};

        //endregion
        //region ground legs

        crawler = new UnitType("crawler"){{
            researchCostMultiplier = 0.5f;
            aiController = GroundAI::new;

            speed = 3.94f;
            hitSize = 8f;
            health = 90f;
            armor = 0f;
            armorType = ArmorType.light;
            unitClasses = EnumSet.of(UnitClass.mechanical);
            omniMovement = true;
            rotateMoveFirst = false;
            population = 2;
            mechSideSway = 0.25f;
            range = maxRange = widowRange();
            targetAir = true;
            targetGround = true;
            ammoType = new ItemAmmoType(Items.coal);
            stepSound = Sounds.walkerStepTiny;
            stepSoundVolume = 0.2f;

            weapons.add(new Weapon("crawler-widow-weapon"){{
                mirror = false;
                top = false;
                rotate = true;
                rotateSpeed = 4.5f;
                x = y = shootX = shootY = 0f;
                reload = 1f;
                shootCone = 360f;
                ejectEffect = Fx.none;
                shootSound = Sounds.shootMissileSmall;

                bullet = new MissileBulletType(14f, 125f, "missile"){{
                    width = 6f;
                    height = 8f;
                    lifetime = 42f;
                    homingPower = 0f;
                    hitColor = backColor = trailColor = Color.valueOf("ff5a5a");
                    frontColor = Color.white;
                    trailWidth = 1.1f;
                    trailLength = 8;
                    weaveMag = 0f;
                    weaveScale = 0f;
                    collides = false;
                    collidesTiles = false;
                    collidesAir = true;
                    collidesGround = true;
                    hittable = false;
                    absorbable = false;
                    reflectable = false;
                    keepVelocity = false;
                    splashDamage = 40f;
                    splashDamageRadius = 1.5f * tilesize;
                    despawnHit = false;
                    hitEffect = Fx.massiveExplosion;
                    despawnEffect = Fx.none;
                    shootEffect = Fx.shootBigColor;
                }

                @Override
                public void update(Bullet b){
                    Teamc target = null;
                    if(b.data instanceof Teamc t){
                        target = t;
                    }

                    if(target instanceof Healthc h && !h.isValid()){
                        target = null;
                    }
                    if(target != null && target.team() == b.team){
                        target = null;
                    }

                    if(target != null){
                        b.aimX = target.getX();
                        b.aimY = target.getY();
                    }

                    float tx = b.aimX, ty = b.aimY;
                    if(Float.isNaN(tx) || Float.isNaN(ty)){
                        b.remove();
                        return;
                    }

                    b.vel.setAngle(Angles.moveToward(b.rotation(), b.angleTo(tx, ty), 35f * Time.delta));
                    b.vel.setLength(speed);
                    b.rotation(b.vel.angle());

                    float hitRange = 4f;
                    if(target instanceof Sized s){
                        hitRange += s.hitSize() / 2f;
                    }

                    if(Mathf.within(b.x, b.y, tx, ty, hitRange)){
                        hit(b, tx, ty);

                        if(target instanceof Unit u){
                            boolean shielded = u.shield > 0.001f;
                            u.damage(b.damage);
                            if(shielded){
                                u.damagePierce(35f);
                            }
                        }else if(target instanceof Building build && build.team != b.team){
                            build.damage(b.damage * buildingDamageMultiplier);
                        }

                        b.remove();
                    }
                }

                @Override
                public void createSplashDamage(Bullet b, float x, float y){
                    super.createSplashDamage(b, x, y);
                    if(splashDamageRadius <= 0f || b.absorbed) return;

                    Units.nearbyEnemies(b.team, x - splashDamageRadius, y - splashDamageRadius, splashDamageRadius * 2f, splashDamageRadius * 2f, u -> {
                        if(!u.within(x, y, splashDamageRadius + u.hitSize / 2f)) return;
                        if(u.shield > 0.001f){
                            u.damagePierce(25f);
                        }
                    });
                }
                };

                mountType = WidowMount::new;
            }

            class WidowMount extends WeaponMount{
                int lockedTargetId = -1;
                float lockTime = 0f;
                boolean drawingLock = false;

                WidowMount(Weapon weapon){
                    super(weapon);
                }
            }

            private @Nullable Teamc resolveTarget(int targetId){
                if(targetId < 0) return null;
                Syncc entity = Groups.sync.getByID(targetId);
                return entity instanceof Teamc t ? t : null;
            }

            private void clearLock(Unit unit, WidowMount mount){
                if(!net.client()){
                    widowReleaseTarget(unit, mount.lockedTargetId);
                }
                mount.lockedTargetId = -1;
                mount.lockTime = 0f;
                mount.drawingLock = false;
            }

            private @Nullable Teamc validateTarget(Unit unit, WidowMount mount){
                Teamc target = resolveTarget(mount.lockedTargetId);
                if(target == null || Units.invalidateTarget(target, unit.team, unit.x, unit.y, widowRange(), unit.hitSize / 2f)){
                    clearLock(unit, mount);
                    return null;
                }

                if(!net.client() && !widowReserveTarget(unit, target.id())){
                    clearLock(unit, mount);
                    return null;
                }

                return target;
            }

            private @Nullable Teamc acquireTarget(Unit unit, WidowMount mount){
                Teamc target = Units.closestTarget(unit.team, unit.x, unit.y, widowRange(), unit.hitSize / 2f,
                u -> u.checkTarget(true, true) && (net.client() || widowCanReserveTarget(unit, u.id)),
                b -> net.client() || widowCanReserveTarget(unit, b.id));

                if(target == null) return null;
                if(!net.client() && !widowReserveTarget(unit, target.id())) return null;

                mount.lockedTargetId = target.id();
                mount.lockTime = 0f;
                return target;
            }

            private void fire(Unit unit, WidowMount mount, Teamc target){
                float
                mountX = unit.x + Angles.trnsx(unit.rotation - 90f, x, y),
                mountY = unit.y + Angles.trnsy(unit.rotation - 90f, x, y),
                weaponRotation = unit.rotation - 90f + mount.rotation,
                bulletX = mountX + Angles.trnsx(weaponRotation, shootX, shootY),
                bulletY = mountY + Angles.trnsy(weaponRotation, shootX, shootY),
                angle = unit.angleTo(target);

                Entityc shooter = unit.controller() instanceof MissileAI ai ? ai.shooter : unit;
                bullet.create(unit, shooter, unit.team, bulletX, bulletY, angle, -1f, 1f, 1f, target, null, target.getX(), target.getY(), target);

                shootSound.at(bulletX, bulletY, 1f, shootSoundVolume);
                bullet.shootEffect.at(bulletX, bulletY, angle, bullet.hitColor, unit);
            }

            @Override
            public void update(Unit unit, WeaponMount raw){
                if(!(raw instanceof WidowMount mount)) return;

                mount.drawingLock = false;
                mount.reload = 0f;
                mount.heat = Mathf.approachDelta(mount.heat, 0f, 0.08f);

                if(!widowIsBuried(unit) || widowIsBurrowing(unit) || widowIsUnburrowing(unit) || widowIsReloading(unit)){
                    clearLock(unit, mount);
                    return;
                }

                float mountX = unit.x + Angles.trnsx(unit.rotation - 90f, x, y);
                float mountY = unit.y + Angles.trnsy(unit.rotation - 90f, x, y);

                Teamc target = validateTarget(unit, mount);
                if(target == null){
                    target = acquireTarget(unit, mount);
                }
                if(target == null){
                    return;
                }

                mount.aimX = target.getX();
                mount.aimY = target.getY();
                mount.targetRotation = Angles.angle(mountX, mountY, mount.aimX, mount.aimY) - unit.rotation;
                mount.rotation = Angles.moveToward(mount.rotation, mount.targetRotation, rotateSpeed * Time.delta);

                mount.drawingLock = true;
                mount.lockTime += Time.delta;
                mount.warmup = Mathf.clamp(mount.lockTime / widowLockTime);

                if(mount.lockTime >= widowLockTime){
                    if(!net.client()){
                        fire(unit, mount, target);
                        unit.apply(StatusEffects.widowReloading, widowReloadTime);
                    }
                    clearLock(unit, mount);
                    mount.heat = 1f;
                    mount.warmup = 0f;
                }
            }

            @Override
            public void draw(Unit unit, WeaponMount raw){
                super.draw(unit, raw);
                if(!(raw instanceof WidowMount mount)) return;
                if(!mount.drawingLock) return;

                Teamc target = resolveTarget(mount.lockedTargetId);
                if(target == null) return;

                Draw.z(Layer.effect);
                Lines.stroke(1.25f);
                Draw.color(Color.valueOf("ff2f2f"));
                Lines.line(unit.x, unit.y, target.getX(), target.getY());
                Draw.reset();
            }
            });
        }
        @Override
        public void update(Unit unit){
            super.update(unit);

            if(widowIsBurrowing(unit)){
                unit.vel.setZero();
                if(unit.controller() instanceof CommandAI ai){
                    ai.clearCommands();
                }
                if(unit.getDuration(StatusEffects.widowBurrowing) <= 0.001f){
                    unit.unapply(StatusEffects.widowBurrowing);
                    unit.apply(StatusEffects.widowBuried, 1f);
                }
            }

            if(widowIsUnburrowing(unit)){
                unit.vel.setZero();
                if(unit.controller() instanceof CommandAI ai){
                    ai.clearCommands();
                }
                if(unit.getDuration(StatusEffects.widowUnburrowing) <= 0.001f){
                    unit.unapply(StatusEffects.widowUnburrowing);
                }
            }

            if(widowIsBuried(unit)){
                unit.vel.setZero();
                if(unit.controller() instanceof CommandAI ai){
                    ai.clearCommands();
                }
            }else if(!widowIsBurrowing(unit) && !widowIsUnburrowing(unit)){
                clearWidowLockData(unit);
            }
        }

        @Override
        public void killed(Unit unit){
            clearWidowLockData(unit);
        }
        };

        atrax = new UnitType("atrax"){{
            speed = 4.5f;
            drag = 0.4f;
            hitSize = 13f;
            rotateSpeed = 3f; // 180 deg/sec
            targetAir = false;
            health = 600;
            immunities = ObjectSet.with(StatusEffects.burning, StatusEffects.melting);

            stepSound = Sounds.walkerStepSmall;
            stepSoundPitch = 1f;
            stepSoundVolume = 0.25f;

            legCount = 4;
            legLength = 9f;
            legForwardScl = 0.6f;
            legMoveSpace = 1.4f;
            hovering = true;
            armor = 3f;
            ammoType = new ItemAmmoType(Items.coal);

            shadowElevation = 0.2f;
            groundLayer = Layer.legUnit - 1f;

            weapons.add(new Weapon("atrax-weapon"){{
                top = false;
                shootY = 3f;
                reload = 9f;
                ejectEffect = Fx.none;
                recoil = 1f;
                x = 7f;
                shootSound = Sounds.shootAtrax;

                bullet = new LiquidBulletType(Liquids.slag){{
                    damage = 13;
                    speed = 2.5f;
                    drag = 0.009f;
                    shootEffect = Fx.shootSmall;
                    lifetime = 57f;
                    collidesAir = false;
                }};
            }});
        }};

        spiroct = new UnitType("spiroct"){{
            speed = 4.05f;
            drag = 0.4f;
            hitSize = 15f;
            rotateSpeed = 3f; // 180 deg/sec
            health = 1000;
            legCount = 6;
            legLength = 13f;
            legForwardScl = 0.8f;
            legMoveSpace = 1.4f;
            legBaseOffset = 2f;
            hovering = true;
            armor = 5f;
            ammoType = new PowerAmmoType(1000);

            shadowElevation = 0.3f;
            groundLayer = Layer.legUnit;

            stepSound = Sounds.walkerStepSmall;
            stepSoundPitch = 0.7f;
            stepSoundVolume = 0.35f;

            weapons.add(new Weapon("spiroct-weapon"){{
                shootY = 4f;
                reload = 14f;
                ejectEffect = Fx.none;
                recoil = 2f;
                rotate = true;
                shootSound = Sounds.shootSap;

                x = 8.5f;
                y = -1.5f;

                bullet = new SapBulletType(){{
                    sapStrength = 0.5f;
                    length = 75f;
                    damage = 23;
                    shootEffect = Fx.shootSmall;
                    hitColor = color = Color.valueOf("bf92f9");
                    despawnEffect = Fx.none;
                    width = 0.54f;
                    lifetime = 35f;
                    knockback = -1.24f;
                }};
            }});

            weapons.add(new Weapon("mount-purple-weapon"){{
                reload = 18f;
                rotate = true;
                x = 4f;
                y = 3f;
                shootSound = Sounds.shootSap;

                bullet = new SapBulletType(){{
                    sapStrength = 0.8f;
                    length = 40f;
                    damage = 18;
                    shootEffect = Fx.shootSmall;
                    hitColor = color = Color.valueOf("bf92f9");
                    despawnEffect = Fx.none;
                    width = 0.4f;
                    lifetime = 25f;
                    knockback = -0.65f;
                }};
            }});
        }};

        arkyid = new UnitType("arkyid"){{
            drag = 0.1f;
            speed = 4.65f;
            hitSize = 23f;
            health = 8000;
            armor = 6f;

            rotateSpeed = 3f; // 180 deg/sec

            legCount = 6;
            legMoveSpace = 1f;
            legPairOffset = 3;
            legLength = 30f;
            legExtension = -15;
            legBaseOffset = 10f;
            stepShake = 1f;
            legLengthScl = 0.96f;
            rippleScale = 2f;
            legSpeed = 0.2f;
            ammoType = new PowerAmmoType(2000);

            stepSound = Sounds.walkerStep;
            stepSoundVolume = 0.85f;
            stepSoundPitch = 1.1f;

            legSplashDamage = 32;
            legSplashRange = 30;

            hovering = true;
            shadowElevation = 0.65f;
            groundLayer = Layer.legUnit;

            BulletType sapper = new SapBulletType(){{
                sapStrength = 0.85f;
                length = 55f;
                damage = 40;
                shootEffect = Fx.shootSmall;
                hitColor = color = Color.valueOf("bf92f9");
                despawnEffect = Fx.none;
                width = 0.55f;
                lifetime = 30f;
                knockback = -1f;
            }};

            weapons.add(
            new Weapon("spiroct-weapon"){{
                reload = 9f;
                x = 4f;
                y = 8f;
                rotate = true;
                bullet = sapper;
                shootSound = Sounds.shootSap;
            }},
            new Weapon("spiroct-weapon"){{
                reload = 14f;
                x = 9f;
                y = 6f;
                rotate = true;
                bullet = sapper;
                shootSound = Sounds.shootSap;
            }},
            new Weapon("spiroct-weapon"){{
                reload = 22f;
                x = 14f;
                y = 0f;
                rotate = true;
                bullet = sapper;
                shootSound = Sounds.shootSap;
            }},
            new Weapon("large-purple-mount"){{
                y = -7f;
                x = 9f;
                shootY = 7f;
                reload = 45;
                shake = 3f;
                rotateSpeed = 3f; // 180 deg/sec
                ejectEffect = Fx.casing1;
                shootSound = Sounds.shootArtillerySap;
                rotate = true;
                shadow = 8f;
                recoil = 1f;

                bullet = new ArtilleryBulletType(2f, 12){{
                    hitEffect = Fx.sapExplosion;
                    despawnSound = Sounds.explosionArtilleryShock;
                    knockback = 0.8f;
                    lifetime = 70f;
                    width = height = 19f;
                    collidesTiles = true;
                    ammoMultiplier = 4f;
                    splashDamageRadius = 70f;
                    splashDamage = 65f;
                    backColor = Pal.sapBulletBack;
                    frontColor = lightningColor = Pal.sapBullet;
                    lightning = 3;
                    lightningLength = 10;
                    smokeEffect = Fx.shootBigSmoke2;
                    shake = 5f;

                    status = StatusEffects.sapped;
                    statusDuration = 60f * 10;
                }};
            }});
        }};

        toxopid = new UnitType("toxopid"){{
            drag = 0.1f;
            speed = 3.75f;
            hitSize = 26f;
            health = 22000;
            armor = 13f;
            lightRadius = 140f;
            stepSound = Sounds.walkerStep;
            stepSoundVolume = 1.1f;

            rotateSpeed = 3f; // 180 deg/sec

            legCount = 8;
            legMoveSpace = 0.8f;
            legPairOffset = 3;
            legLength = 75f;
            legExtension = -20;
            legBaseOffset = 8f;
            stepShake = 1f;
            legLengthScl = 0.93f;
            rippleScale = 3f;
            legSpeed = 0.19f;
            ammoType = new ItemAmmoType(Items.graphite, 8);

            legSplashDamage = 80;
            legSplashRange = 60;

            hovering = true;
            shadowElevation = 0.95f;
            groundLayer = Layer.legUnit;

            weapons.add(
            new Weapon("large-purple-mount"){{
                y = -5f;
                x = 11f;
                shootY = 7f;
                reload = 30;
                shake = 4f;
                rotateSpeed = 3f; // 180 deg/sec
                ejectEffect = Fx.casing1;
                shootSound = Sounds.shootToxopidShotgun;
                shootSoundVolume = 0.8f;
                rotate = true;
                shadow = 12f;
                recoil = 3f;

                shoot = new ShootSpread(2, 17f);

                bullet = new ShrapnelBulletType(){{
                    length = 90f;
                    damage = 110f;
                    width = 25f;
                    serrationLenScl = 7f;
                    serrationSpaceOffset = 60f;
                    serrationFadeOffset = 0f;
                    serrations = 10;
                    serrationWidth = 6f;
                    fromColor = Pal.sapBullet;
                    toColor = Pal.sapBulletBack;
                    shootEffect = smokeEffect = Fx.sparkShoot;
                }};
            }});

            weapons.add(new Weapon("toxopid-cannon"){{
                y = -14f;
                x = 0f;
                shootY = 22f;
                mirror = false;
                reload = 210;
                shake = 10f;
                recoil = 10f;
                rotateSpeed = 3f; // 180 deg/sec
                ejectEffect = Fx.casing3;
                shootSound = Sounds.shootArtillerySapBig;
                rotate = true;
                shadow = 30f;

                rotationLimit = 80f;

                bullet = new ArtilleryBulletType(3f, 50){{
                    despawnSound = Sounds.explosionArtilleryShockBig;
                    hitEffect = Fx.sapExplosion;
                    knockback = 0.8f;
                    lifetime = 80f;
                    width = height = 25f;
                    collidesTiles = collides = true;
                    ammoMultiplier = 4f;
                    splashDamageRadius = 80f;
                    splashDamage = 75f;
                    backColor = Pal.sapBulletBack;
                    frontColor = lightningColor = Pal.sapBullet;
                    lightning = 5;
                    lightningLength = 20;
                    smokeEffect = Fx.shootBigSmoke2;
                    hitShake = 10f;
                    lightRadius = 40f;
                    lightColor = Pal.sap;
                    lightOpacity = 0.6f;

                    status = StatusEffects.sapped;
                    statusDuration = 60f * 10;

                    fragLifeMin = 0.3f;
                    fragBullets = 9;

                    fragBullet = new ArtilleryBulletType(2.3f, 30){{
                        despawnSound = Sounds.explosionArtilleryShock;
                        hitEffect = Fx.sapExplosion;
                        knockback = 0.8f;
                        lifetime = 90f;
                        width = height = 20f;
                        collidesTiles = false;
                        splashDamageRadius = 70f;
                        splashDamage = 40f;
                        backColor = Pal.sapBulletBack;
                        frontColor = lightningColor = Pal.sapBullet;
                        lightning = 2;
                        lightningLength = 5;
                        smokeEffect = Fx.shootBigSmoke2;
                        hitShake = 5f;
                        lightRadius = 30f;
                        lightColor = Pal.sap;
                        lightOpacity = 0.5f;

                        status = StatusEffects.sapped;
                        statusDuration = 60f * 10;
                    }};
                }};
            }});
        }};

        //endregion
        //region air attack

        flare = new UnitType("flare"){
            @Override
            public void draw(Unit unit){
                float prevX = Draw.xscl, prevY = Draw.yscl;
                Draw.scl(prevX * 2f, prevY * 2f);
                super.draw(unit);
                Draw.scl(prevX, prevY);
            }

            {
                researchCostMultiplier = 0.5f;
                speed = 3.85f;
                accel = 0.08f;
                drag = 0.04f;
                flying = true;
                health = 135f;
                armor = 0f;
                armorType = ArmorType.heavy;
                unitClasses = EnumSet.of(UnitClass.mechanical);
                population = 2;
                engineOffset = 5.75f;
                targetFlags = new BlockFlag[]{BlockFlag.generator, null};
                hitSize = 9f;
                itemCapacity = 10;
                circleTarget = false;
                omniMovement = false;
                rotateSpeed = 3f; // 180 deg/sec
                circleTargetRadius = 60f;
                wreckSoundVolume = 0.7f;
                range = maxRange = 9f * tilesize;
                targetAir = true;
                targetGround = false;

                moveSound = Sounds.loopThruster;
                moveSoundPitchMin = 0.3f;
                moveSoundPitchMax = 1.5f;
                moveSoundVolume = 0.2f;

                weapons.add(new Weapon(){
                    @Override
                    protected void handleBullet(Unit unit, WeaponMount mount, Bullet bullet){
                        super.handleBullet(unit, mount, bullet);
                        bullet.data = mount.target;
                    }

                    {
                        y = 1f;
                        x = 0f;
                        minShootVelocity = -1f;
                        shootCone = 10f;
                        reload = 1.43f * 60f;
                        shoot.shots = 2;
                        shoot.shotDelay = 0f;
                        ejectEffect = Fx.casing1;
                        mirror = false;
                        targetAir = true;
                        targetGround = false;
                        bullet = new MissileBulletType(6f, 10f, "missile"){
                            {
                                inaccuracy = 2f;
                                width = 7f;
                                height = 9f;
                                lifetime = 24f;
                                rangeOverride = 9f * tilesize;
                                homingPower = 0f;
                                weaveMag = 0f;
                                weaveScale = 0f;
                                trailColor = backColor;
                                trailWidth = 1.3f;
                                trailLength = 8;
                                collides = false;
                                collidesTiles = false;
                                collidesAir = true;
                                collidesGround = false;
                                hittable = false;
                                absorbable = false;
                                reflectable = false;
                                keepVelocity = false;
                                despawnHit = false;
                                shootEffect = Fx.shootSmall;
                                smokeEffect = Fx.shootSmallSmoke;
                                hitEffect = Fx.hitBulletColor;
                                despawnEffect = Fx.none;
                                ammoMultiplier = 2f;
                            }

                            @Override
                            public void update(Bullet b){
                                Teamc target = b.data instanceof Teamc t ? t : null;
                                if(target instanceof Healthc h && !h.isValid()) target = null;
                                if(target != null){
                                    b.aimX = target.x();
                                    b.aimY = target.y();
                                }

                                float tx = b.aimX, ty = b.aimY;
                                if(Float.isNaN(tx) || Float.isNaN(ty)){
                                    tx = b.x + Angles.trnsx(b.rotation(), 8f);
                                    ty = b.y + Angles.trnsy(b.rotation(), 8f);
                                    b.aimX = tx;
                                    b.aimY = ty;
                                }
                                b.vel.setAngle(Angles.moveToward(b.rotation(), b.angleTo(tx, ty), 120f * Time.delta));
                                b.vel.setLength(speed);
                                b.rotation(b.vel.angle());

                                if(target == null){
                                    if(Mathf.within(b.x, b.y, tx, ty, 2f)){
                                        b.remove();
                                    }
                                    return;
                                }

                                float hitRange = 2f + (target instanceof Sized s ? s.hitSize() / 2f : 0f);
                                if(Mathf.within(b.x, b.y, tx, ty, hitRange)){
                                    if(target.team() != b.team){
                                        float amount = damage;
                                        if(target instanceof Unit u && u.type.armorType == ArmorType.heavy){
                                            amount = 14f;
                                        }
                                        if(target instanceof Unit u){
                                            u.damage(amount);
                                        }else if(target instanceof Building build){
                                            build.damage(amount * buildingDamageMultiplier);
                                        }
                                    }
                                    hit(b, tx, ty);
                                    b.remove();
                                }
                            }
                        };
                    }
                });
            }
        };

        liberator = new UnitType("liberator"){
            @Override
            public void load(){
                super.load();
                String copy = "obviate";
                region = Core.atlas.find(copy, region);
                previewRegion = Core.atlas.find(copy + "-preview", copy);
                outlineRegion = Core.atlas.find(copy + "-outline", outlineRegion);
                cellRegion = Core.atlas.find(copy + "-cell", cellRegion);
                shadowRegion = Core.atlas.find(copy + "-shadow", shadowRegion);
                wreckRegions = new TextureRegion[3];
                for(int i = 0; i < wreckRegions.length; i++){
                    wreckRegions[i] = Core.atlas.find(copy + "-wreck" + i);
                }
            }

            {
                speed = 4.72f;
                accel = 0.09f;
                drag = 0.03f;
                flying = true;
                lowAltitude = true;
                rotateSpeed = 8f;
                health = 180f;
                armor = 0f;
                armorType = ArmorType.heavy;
                unitClasses = EnumSet.of(UnitClass.mechanical);
                fullOverride = "obviate";
                population = 3;
                hitSize = 25f;
                engineSize = 4.3f;
                engineOffset = 54f / 4f;
                range = maxRange = liberatorFighterRange();
                targetAir = true;
                targetGround = true;
                faceTarget = false;
                omniMovement = false;
                itemCapacity = 0;

                setEnginesMirror(
                new UnitEngine(38f / 4f, -46f / 4f, 3.1f, 315f)
                );

                weapons.add(new Weapon("elude-weapon"){
                    @Override
                    public void drawOutline(Unit unit, WeaponMount mount){
                        if(!isLiberator(unit) || getLiberatorData(unit).defenseMode) return;
                        super.drawOutline(unit, mount);
                    }

                    @Override
                    public void draw(Unit unit, WeaponMount mount){
                        if(!isLiberator(unit) || getLiberatorData(unit).defenseMode) return;
                        super.draw(unit, mount);
                    }

                    @Override
                    public void update(Unit unit, WeaponMount mount){
                        if(liberatorIsDefending(unit) || liberatorIsDeploying(unit) || liberatorIsUndeploying(unit)){
                            mount.shoot = false;
                            mount.rotate = false;
                            mount.warmup = Mathf.approachDelta(mount.warmup, 0f, 0.08f);
                            mount.heat = Mathf.approachDelta(mount.heat, 0f, 0.08f);
                            return;
                        }
                        super.update(unit, mount);
                    }

                    @Override
                    protected void handleBullet(Unit unit, WeaponMount mount, Bullet bullet){
                        super.handleBullet(unit, mount, bullet);
                        bullet.data = mount.target;
                    }

                    {
                        shootSound = Sounds.shootElude;
                        x = 4f;
                        y = -2f;
                        top = true;
                        mirror = true;
                        reload = 1.29f * 60f;
                        baseRotation = -35f;
                        shootCone = 360f;
                        targetAir = true;
                        targetGround = false;

                        bullet = new MissileBulletType(6.5f, 5f, "missile"){{
                            width = 7f;
                            height = 9f;
                            lifetime = 24f;
                            rangeOverride = liberatorFighterRange();
                            homingPower = 0f;
                            weaveMag = 0f;
                            weaveScale = 0f;
                            trailColor = backColor;
                            trailWidth = 1.3f;
                            trailLength = 8;
                            collides = false;
                            collidesTiles = false;
                            collidesAir = true;
                            collidesGround = false;
                            hittable = false;
                            absorbable = false;
                            reflectable = false;
                            keepVelocity = false;
                            despawnHit = false;
                            shootEffect = Fx.shootSmall;
                            smokeEffect = Fx.shootSmallSmoke;
                            hitEffect = Fx.hitBulletColor;
                            despawnEffect = Fx.none;
                            splashDamage = 5f;
                            splashDamageRadius = 0.5f * tilesize;
                        }

                        @Override
                        public void update(Bullet b){
                            Teamc target = b.data instanceof Teamc t ? t : null;
                            if(target instanceof Healthc h && !h.isValid()) target = null;
                            if(target != null){
                                b.aimX = target.x();
                                b.aimY = target.y();
                            }

                            float tx = b.aimX, ty = b.aimY;
                            if(Float.isNaN(tx) || Float.isNaN(ty)){
                                tx = b.x + Angles.trnsx(b.rotation(), 8f);
                                ty = b.y + Angles.trnsy(b.rotation(), 8f);
                                b.aimX = tx;
                                b.aimY = ty;
                            }
                            b.vel.setAngle(Angles.moveToward(b.rotation(), b.angleTo(tx, ty), 220f * Time.delta));
                            b.vel.setLength(speed);
                            b.rotation(b.vel.angle());

                            if(target == null){
                                if(Mathf.within(b.x, b.y, tx, ty, 2f)){
                                    b.remove();
                                }
                                return;
                            }

                            float hitRange = 2f + (target instanceof Sized s ? s.hitSize() / 2f : 0f);
                            if(Mathf.within(b.x, b.y, tx, ty, hitRange)){
                                if(target.team() != b.team){
                                    if(target instanceof Unit u){
                                        u.damage(damage);
                                    }else if(target instanceof Building build){
                                        build.damage(damage * buildingDamageMultiplier);
                                    }
                                }
                                hit(b, tx, ty);
                                b.remove();
                            }
                        }

                        @Override
                        public void createSplashDamage(Bullet b, float x, float y){
                            //Direct-hit only for liberator air missiles.
                        }
                        };
                    }
                });

                weapons.add(new Weapon("elude-weapon"){
                    @Override
                    public void drawOutline(Unit unit, WeaponMount mount){
                        if(!isLiberator(unit) || !getLiberatorData(unit).defenseMode) return;
                        super.drawOutline(unit, mount);
                    }

                    @Override
                    public void draw(Unit unit, WeaponMount mount){
                        if(!isLiberator(unit) || !getLiberatorData(unit).defenseMode) return;
                        super.draw(unit, mount);
                    }

                    @Override
                    public void update(Unit unit, WeaponMount mount){
                        if(!liberatorIsDefending(unit) || liberatorIsDeploying(unit) || liberatorIsUndeploying(unit)){
                            mount.shoot = false;
                            mount.rotate = false;
                            mount.warmup = Mathf.approachDelta(mount.warmup, 0f, 0.08f);
                            mount.heat = Mathf.approachDelta(mount.heat, 0f, 0.08f);
                            return;
                        }
                        if(!getLiberatorData(unit).zoneSet){
                            mount.shoot = false;
                            mount.rotate = false;
                            return;
                        }
                        super.update(unit, mount);
                    }

                    @Override
                    protected Teamc findTarget(Unit unit, float x, float y, float range, boolean air, boolean ground){
                        if(!liberatorIsDefending(unit)) return null;
                        return Units.closestEnemy(unit.team, unit.x, unit.y, liberatorDefenseRange(),
                        u -> !u.isFlying() && liberatorTargetInZone(unit, u));
                    }

                    @Override
                    protected boolean checkTarget(Unit unit, Teamc target, float x, float y, float range){
                        if(!liberatorIsDefending(unit)) return true;
                        if(!(target instanceof Unit u) || u.isFlying()) return true;
                        if(!liberatorTargetInZone(unit, target)) return true;
                        return super.checkTarget(unit, target, x, y, range);
                    }

                    {
                        shootSound = Sounds.shoot;
                        x = 4f;
                        y = -2f;
                        shootY = 9f;
                        top = true;
                        mirror = true;
                        controllable = false;
                        autoTarget = true;
                        reload = 1.14f * 60f;
                        baseRotation = -35f;
                        rotate = true;
                        rotateSpeed = 6f;
                        shootCone = 8f;
                        targetAir = false;
                        targetGround = true;
                        layerOffset = 0.0001f;
                        recoil = 1f;

                        bullet = new BasicBulletType(13.333f, 75f){{
                            width = 7f;
                            height = 20f;
                            lifetime = 9f;
                            rangeOverride = liberatorDefenseRange();
                            collidesAir = false;
                            collidesGround = true;
                            hitEffect = Fx.hitBulletColor;
                            despawnEffect = Fx.none;
                            shootEffect = Fx.shootBig;
                            smokeEffect = Fx.shootBigSmoke;
                            sprite = "bullet";
                        }};
                    }
                });
            }

            @Override
            public void update(Unit unit){
                super.update(unit);
                updateLiberator(unit);
            }

            @Override
            public void draw(Unit unit){
                super.draw(unit);
                drawLiberatorZone(unit);
            }

            @Override
            public void killed(Unit unit){
                clearLiberatorData(unit);
            }
        };

        horizon = new UnitType("horizon"){
            @Override
            public void update(Unit unit){
                super.update(unit);
                updateBanshee(unit);
            }

            {
                health = 140f;
                speed = 3.85f;
                accel = 0.09f;
                drag = 0.08f;
                flying = true;
                hitSize = 12f;
                population = 2;
                targetAir = false;
                targetGround = true;
                engineOffset = 7.8f;
                range = maxRange = 6f * tilesize;
                faceTarget = true;
                armor = 0f;
                armorType = ArmorType.light;
                unitClasses = EnumSet.of(UnitClass.mechanical);
                itemCapacity = 0;
                omniMovement = true;
                rotateMoveFirst = false;
                rotateSpeed = 6f; // 360 deg/sec
                energyCapacity = 200f;
                energyInit = 50f;

                moveSound = Sounds.loopThruster;
                moveSoundPitchMin = 0.6f;
                moveSoundVolume = 0.4f;

                weapons.add(new Weapon("horizon-rocket"){{
                    x = 3f;
                    y = 0f;
                    shootY = 0f;
                    mirror = true;
                    rotate = true;
                    rotateSpeed = 6f;
                    reload = 0.89f * 60f;
                    shoot.shots = 2;
                    shoot.shotDelay = 0.11f * 60f;
                    shootCone = 18f;
                    inaccuracy = 2f;
                    velocityRnd = 0f;
                    targetAir = false;
                    targetGround = true;
                    shootSound = Sounds.shootMissileSmall;

                    bullet = new MissileBulletType(4.2f, 12f, "missile"){{
                        width = 8f;
                        height = 8f;
                        shrinkY = 0f;
                        lifetime = 24f;
                        rangeOverride = 6f * tilesize;
                        collidesAir = false;
                        collidesGround = true;
                        keepVelocity = false;
                        splashDamage = 0f;
                        splashDamageRadius = 0f;
                        weaveMag = 0.5f;
                        weaveScale = 7f;
                        homingPower = 0.06f;
                        trailColor = Pal.unitBack;
                        backColor = Pal.unitBack;
                        frontColor = Color.white;
                        hitEffect = Fx.hitBulletColor;
                        despawnEffect = Fx.none;
                    }};
                }});
            }
        };

        zenith = new UnitType("zenith"){{
            health = 700;
            speed = 12.75f;
            accel = 0.04f;
            drag = 0.016f;
            flying = true;
            range = 140f;
            hitSize = 20f;
            lowAltitude = true;
            forceMultiTarget = true;
            armor = 5f;

            targetFlags = new BlockFlag[]{BlockFlag.launchPad, BlockFlag.storage, BlockFlag.battery, null};
            engineOffset = 12f;
            engineSize = 3f;
            ammoType = new ItemAmmoType(Items.graphite);

            weapons.add(new Weapon("zenith-missiles"){{
                reload = 40f;
                x = 7f;
                rotate = true;
                shake = 1f;
                shoot.shots = 2;
                inaccuracy = 5f;
                velocityRnd = 0.2f;
                shootSound = Sounds.shootMissileLong;

                bullet = new MissileBulletType(3f, 14){{
                    width = 8f;
                    height = 8f;
                    shrinkY = 0f;
                    drag = -0.003f;
                    homingRange = 60f;
                    keepVelocity = false;
                    splashDamageRadius = 25f;
                    splashDamage = 15f;
                    lifetime = 50f;
                    trailColor = Pal.unitBack;
                    backColor = Pal.unitBack;
                    frontColor = Pal.unitFront;
                    hitEffect = Fx.blastExplosion;
                    despawnEffect = Fx.blastExplosion;
                    weaveScale = 6f;
                    weaveMag = 1f;
                }};
            }});
        }};

        antumbra = new UnitType("antumbra"){
            private boolean regionsScaled = false;

            @Override
            public void load(){
                super.load();
                if(regionsScaled) return;
                regionsScaled = true;

                scaleRegion(region, battlecruiserBodyScale);
                scaleRegion(outlineRegion, battlecruiserBodyScale);
                scaleRegion(cellRegion, battlecruiserBodyScale);
                scaleRegion(shadowRegion, battlecruiserBodyScale);

                for(TextureRegion wreck : wreckRegions){
                    scaleRegion(wreck, battlecruiserBodyScale);
                }

                rebuildBattlecruiserSpotMask(region);
            }

            @Override
            public void update(Unit unit){
                super.update(unit);
                updateBattlecruiser(unit);
            }

            @Override
            public boolean targetable(Unit unit, Team targeter){
                return !battlecruiserWarping(unit) && super.targetable(unit, targeter);
            }

            @Override
            public boolean hittable(Unit unit){
                return !battlecruiserWarping(unit) && super.hittable(unit);
            }

            @Override
            public void draw(Unit unit){
                BattlecruiserData data = getBattlecruiserData(unit);
                if(data.warping){
                    drawBattlecruiserOverlay(unit);
                    return;
                }

                if(data.warpAppearTime > 0f){
                    float fin = Mathf.clamp(1f - data.warpAppearTime / battlecruiserWarpAppearTime);
                    drawBattlecruiserArrivalLensInner(unit, unit.x, unit.y, unit.rotation, fin);
                    queueBattlecruiserAfterDraw(unit, unit.x, unit.y, unit.rotation, fin, true);
                    drawBattlecruiserOverlay(unit);
                    return;
                }

                super.draw(unit);
                drawBattlecruiserOverlay(unit);
            }

            @Override
            public void killed(Unit unit){
                clearBattlecruiserData(unit);
            }

            {
                speed = 2.62f;
                accel = 0.05f;
                drag = 0.05f;
                rotateSpeed = 6f; // 360 deg/sec
                flying = true;
                lowAltitude = true;
                health = 550f;
                armor = 3f;
                armorType = ArmorType.heavy;
                unitClasses = EnumSet.of(UnitClass.mechanical, UnitClass.heavy);
                population = 6;
                engineOffset = 21f * battlecruiserBodyScale;
                engineSize = 5.3f * battlecruiserBodyScale;
                hitSize = 40f;
                range = maxRange = battlecruiserWeaponRange;
                targetAir = true;
                targetGround = true;
                singleTarget = true;
                alwaysShootWhenMoving = true;

                loopSound = Sounds.loopHover;

                BulletType laserGround = new LaserBoltBulletType(8f, 8f){{
                    width = 8f;
                    height = 8f;
                    lifetime = battlecruiserWeaponRange / 8f;
                    rangeOverride = battlecruiserWeaponRange;
                    collidesAir = false;
                    collidesGround = true;
                    backColor = Color.valueOf("ff5a5a");
                    frontColor = Color.white;
                    trailColor = Color.valueOf("ff5a5a");
                    trailWidth = 2.1f;
                    trailLength = 12;
                    hitEffect = Fx.hitBulletColor;
                    despawnEffect = Fx.none;
                }};

                BulletType laserAir = new LaserBoltBulletType(8f, 5f){{
                    width = 8f;
                    height = 8f;
                    lifetime = battlecruiserWeaponRange / 8f;
                    rangeOverride = battlecruiserWeaponRange;
                    collidesAir = true;
                    collidesGround = false;
                    backColor = Color.valueOf("ff5a5a");
                    frontColor = Color.white;
                    trailColor = Color.valueOf("ff5a5a");
                    trailWidth = 2.1f;
                    trailLength = 12;
                    hitEffect = Fx.hitBulletColor;
                    despawnEffect = Fx.none;
                }};

                battlecruiserYamatoBullet = new LaserBoltBulletType(6f, 240f){{
                    width = 24f;
                    height = 24f;
                    pierceArmor = true;
                    lifetime = 240f;
                    rangeOverride = 99999f;
                    homingPower = 0.35f;
                    homingRange = 99999f;
                    backColor = Color.valueOf("ff4f4f");
                    frontColor = Color.white;
                    trailColor = Color.valueOf("ff4f4f");
                    trailWidth = 4.5f;
                    trailLength = 28;
                    hitEffect = Fx.hitBulletColor;
                    despawnEffect = Fx.massiveExplosion;
                }};

                weapons.add(
                new Weapon("battlecruiser-ground-laser"){{
                    x = 12f * battlecruiserBodyScale;
                    y = 5f * battlecruiserBodyScale;
                    shootY = 4f * battlecruiserBodyScale;
                    mirror = true;
                    rotate = true;
                    rotateSpeed = 6f;
                    reload = 0.16f * 60f;
                    targetAir = false;
                    targetGround = true;
                    shootSound = Sounds.shootLaser;
                    bullet = laserGround;
                }},
                new Weapon("battlecruiser-air-laser"){{
                    x = 12f * battlecruiserBodyScale;
                    y = -5f * battlecruiserBodyScale;
                    shootY = 4f * battlecruiserBodyScale;
                    mirror = true;
                    rotate = true;
                    rotateSpeed = 6f;
                    reload = 0.16f * 60f;
                    targetAir = true;
                    targetGround = false;
                    shootSound = Sounds.shootLaser;
                    bullet = laserAir;
                }});
            }
        };

        eclipse = new UnitType("eclipse"){{
            speed = 4.05f;
            accel = 0.04f;
            drag = 0.04f;
            rotateSpeed = 3f; // 180 deg/sec
            flying = true;
            lowAltitude = true;
            health = 22000;
            engineOffset = 38;
            engineSize = 7.3f;
            hitSize = 58f;
            armor = 13f;
            targetFlags = new BlockFlag[]{BlockFlag.reactor, BlockFlag.battery, BlockFlag.core, null};
            ammoType = new ItemAmmoType(Items.thorium);

            loopSound = Sounds.loopHover;

            BulletType fragBullet = new FlakBulletType(4f, 15){{
                shootEffect = Fx.shootBig;
                ammoMultiplier = 4f;
                splashDamage = 65f;
                splashDamageRadius = 25f;
                collidesGround = true;
                lifetime = 47f;

                status = StatusEffects.blasted;
                statusDuration = 60f;
            }};

            weapons.add(
            new Weapon("large-laser-mount"){{
                shake = 4f;
                shootY = 9f;
                x = 18f;
                y = 5f;
                rotateSpeed = 3f; // 180 deg/sec
                reload = 45f;
                recoil = 4f;
                shootSound = Sounds.shootEclipse;
                shadow = 20f;
                rotate = true;

                bullet = new LaserBulletType(){{
                    damage = 115f;
                    sideAngle = 20f;
                    sideWidth = 1.5f;
                    sideLength = 80f;
                    width = 25f;
                    length = 230f;
                    shootEffect = Fx.shockwave;
                    colors = new Color[]{Color.valueOf("ec7458aa"), Color.valueOf("ff9c5a"), Color.white};
                }};
            }},
            new Weapon("large-artillery"){{
                x = 11f;
                y = 27f;
                rotateSpeed = 3f; // 180 deg/sec
                reload = 9f;
                shootSound = Sounds.shootCyclone;
                shadow = 7f;
                rotate = true;
                recoil = 0.5f;
                shootY = 7.25f;
                bullet = fragBullet;
            }},
            new Weapon("large-artillery"){{
                y = -13f;
                x = 20f;
                reload = 12f;
                ejectEffect = Fx.casing1;
                rotateSpeed = 3f; // 180 deg/sec
                shake = 1f;
                shootSound = Sounds.shootCyclone;
                rotate = true;
                shadow = 12f;
                shootY = 7.25f;
                bullet = fragBullet;
            }});
        }};

        //endregion
        //region air support

        mono = new UnitType("mono"){{
            defaultCommand = UnitCommand.mineCommand;

            flying = true;
            drag = 0.06f;
            accel = 0.12f;
            speed = 11.25f;
            health = 100;
            engineSize = 1.8f;
            engineOffset = 5.7f;
            range = 50f;
            isEnemy = false;
            controlSelectGlobal = false;
            wreckSoundVolume = deathSoundVolume = 0.7f;

            ammoType = new PowerAmmoType(500);
        }};

        poly = new UnitType("poly"){{
            defaultCommand = UnitCommand.moveCommand;

            flying = true;
            drag = 0.05f;
            speed = 19.5f;
            rotateSpeed = 3f; // 180 deg/sec
            accel = 0.1f;
            range = 130f;
            health = 400;
            buildSpeed = 0.5f;
            engineOffset = 6.5f;
            hitSize = 9f;
            lowAltitude = true;

            ammoType = new PowerAmmoType(900);
            wreckSoundVolume = 0.9f;

            abilities.add(new RepairFieldAbility(5f, 60f * 8, 50f));

            weapons.add(new Weapon("poly-weapon"){{
                top = false;
                y = -2.5f;
                x = 3.75f;
                reload = 30f;
                ejectEffect = Fx.none;
                recoil = 2f;
                shootSound = Sounds.shootMissilePlasmaShort;
                velocityRnd = 0.5f;
                inaccuracy = 15f;
                alternate = true;

                bullet = new MissileBulletType(4f, 12){{
                    homingPower = 0.08f;
                    weaveMag = 4;
                    weaveScale = 4;
                    lifetime = 50f;
                    keepVelocity = false;
                    shootEffect = Fx.shootHeal;
                    smokeEffect = Fx.hitLaser;
                    hitEffect = despawnEffect = Fx.hitLaser;
                    frontColor = Color.white;
                    hitSound = Sounds.none;

                    healPercent = 5.5f;
                    collidesTeam = true;
                    reflectable = false;
                    backColor = Pal.heal;
                    trailColor = Pal.heal;
                }};
            }});
        }};

        mega = new UnitType("mega"){
            {
            defaultCommand = UnitCommand.moveCommand;
            commands = Seq.with(UnitCommand.moveCommand, UnitCommand.loadUnitsCommand, UnitCommand.unloadPayloadCommand);

            health = 150f;
            armor = 1f;
            armorType = ArmorType.heavy;
            unitClasses = EnumSet.of(UnitClass.mechanical);
            population = 2;
            speed = medivacBaseSpeed;
            accel = 0.08f;
            drag = 0.02f;
            lowAltitude = true;
            flying = true;
            engineOffset = 10.5f;
            faceTarget = false;
            hitSize = 16.05f;
            engineSize = 3f;
            range = maxRange = medivacHealRange;
            payloadCapacity = medivacMaxSlots * tilePayload;
            buildSpeed = -1f;
            isEnemy = false;
            canAttack = false;
            targetAir = false;
            targetGround = false;

            energyCapacity = 200f;
            energyInit = 50f;
            energyRegen = 0.8f;

            ammoType = new PowerAmmoType(1100);

            weapons.add(new RepairBeamWeapon("mega-heal-beam"){
                {
                    x = 0f;
                    y = 1f;
                    mirror = false;
                    rotate = true;
                    rotateSpeed = 6f;
                    shootY = 0f;
                    reload = 1f;
                    beamWidth = 0.72f;
                    pulseRadius = 4f;
                    pulseStroke = 1.25f;
                    widthSinMag = 0.08f;
                    shootCone = 360f;
                    targetUnits = true;
                    targetBuildings = false;
                    controllable = false;
                    autoTarget = true;
                    repairSpeed = 16f / 60f; // 16 HP/s -> 4 energy/s at 1 energy : 4 HP
                    fractionRepairSpeed = 0f;
                    laserColor = Color.valueOf("9df7ff");
                    laserTopColor = Color.white;
                    healColor = Color.valueOf("9df7ff");

                    bullet = new BulletType(){{
                        maxRange = medivacHealRange;
                    }};
                }

                @Override
                protected Teamc findTarget(Unit unit, float x, float y, float range, boolean air, boolean ground){
                    return Units.closest(unit.team, x, y, range, u -> medivacCanHealTarget(u, unit.team) && u != unit);
                }

                @Override
                protected boolean checkTarget(Unit unit, Teamc target, float x, float y, float range){
                    if(!(target instanceof Unit u)) return true;
                    return !(u.within(unit, range + unit.hitSize / 2f)
                        && u.team == unit.team
                        && u.isValid()
                        && u.damaged()
                        && u.type.unitClasses.contains(UnitClass.biological));
                }

                @Override
                public void update(Unit unit, WeaponMount mount){
                    float baseRepair = repairSpeed;
                    float maxPerTickByEnergy = Math.max(unit.energy, 0f) * 4f;
                    if(maxPerTickByEnergy <= 0.0001f){
                        repairSpeed = 0f;
                    }else{
                        repairSpeed = Math.min(baseRepair, maxPerTickByEnergy / Math.max(Time.delta, 0.0001f));
                    }

                    Healthc healTarget = mount.target instanceof Healthc h ? h : null;
                    float before = healTarget == null ? 0f : healTarget.health();

                    super.update(unit, mount);
                    repairSpeed = baseRepair;

                    if(healTarget == null || !healTarget.isValid()) return;
                    float healed = Math.max(0f, healTarget.health() - before);
                    if(healed > 0f){
                        unit.energy = Math.max(0f, unit.energy - healed / 4f);
                    }
                }
            });
            }

            @Override
            public void update(Unit unit){
                super.update(unit);
                if(unit.controller() instanceof CommandAI ai && ai.command != UnitCommand.unloadPayloadCommand){
                    clearMedivacData(unit);
                }
            }

            @Override
            public void killed(Unit unit){
                clearMedivacData(unit);
            }
        };

        quad = new UnitType("quad"){{
            armor = 8f;
            health = 6000;
            speed = 9f;
            rotateSpeed = 3f; // 180 deg/sec
            accel = 0.05f;
            drag = 0.017f;
            lowAltitude = false;
            flying = true;
            autoDropBombs = true;
            circleTarget = true;
            engineOffset = 13f;
            engineSize = 7f;
            faceTarget = false;
            hitSize = 36f;
            payloadCapacity = (3 * 3) * tilePayload;
            buildSpeed = 2.5f;
            buildBeamOffset = 23;
            range = 140f;
            targetAir = false;
            targetFlags = new BlockFlag[]{BlockFlag.battery, BlockFlag.factory, null};

            ammoType = new PowerAmmoType(3000);

            loopSound = Sounds.loopHover;

            weapons.add(
            new Weapon(){{
                x = y = 0f;
                mirror = false;
                reload = 55f;
                minShootVelocity = 0.01f;

                soundPitchMin = 1f;
                shootSound = Sounds.shootQuad;

                bullet = new BasicBulletType(){{
                    sprite = "large-bomb";
                    width = height = 120/4f;

                    maxRange = 30f;
                    ignoreRotation = true;

                    backColor = Pal.heal;
                    frontColor = Color.white;
                    mixColorTo = Color.white;

                    hitSound = Sounds.explosionQuad;
                    hitSoundVolume = 0.9f;

                    shootCone = 180f;
                    ejectEffect = Fx.none;
                    hitShake = 4f;

                    collidesAir = false;

                    lifetime = 70f;

                    despawnEffect = Fx.greenBomb;
                    hitEffect = Fx.massiveExplosion;
                    keepVelocity = false;
                    spin = 2f;

                    shrinkX = shrinkY = 0.7f;

                    speed = 0f;
                    collides = false;

                    healPercent = 15f;
                    splashDamage = 220f;
                    splashDamageRadius = 80f;
                    damage = splashDamage * 0.7f;
                }};
            }});
        }};

        oct = new UnitType("oct"){{
            aiController = DefenderAI::new;

            armor = 16f;
            health = 24000;
            speed = 6f;
            rotateSpeed = 3f; // 180 deg/sec
            accel = 0.04f;
            drag = 0.018f;
            flying = true;
            engineOffset = 46f;
            engineSize = 7.8f;
            faceTarget = false;
            hitSize = 66f;
            payloadCapacity = (5.5f * 5.5f) * tilePayload;
            buildSpeed = 4f;
            drawShields = false;
            lowAltitude = true;
            buildBeamOffset = 43;
            ammoCapacity = 1;

            loopSound = Sounds.loopHover;

            abilities.add(new RepairFieldAbility(130f, 60f * 2, 140f));
        }};

        //endregion
        //region naval attack

        risso = new UnitType("risso"){{
            speed = 8.25f;
            drag = 0.13f;
            hitSize = 10f;
            health = 280;
            armor = 2f;
            accel = 0.4f;
            rotateSpeed = 3f; // 180 deg/sec
            faceTarget = false;

            trailLength = 20;
            waveTrailX = 4f;
            trailScl = 1.3f;

            moveSoundVolume = 0.4f;
            moveSound = Sounds.shipMove;

            weapons.add(new Weapon("mount-weapon"){{
                reload = 13f;
                x = 4f;
                shootY = 4f;
                y = 1.5f;
                rotate = true;
                ejectEffect = Fx.casing1;
                bullet = new BasicBulletType(2.5f, 9){{
                    width = 7f;
                    height = 9f;
                    lifetime = 60f;
                    ammoMultiplier = 2;
                }};
            }});

            weapons.add(new Weapon("missiles-mount"){{
                mirror = false;
                reload = 25f;
                x = 0f;
                y = -5f;
                rotate = true;
                ejectEffect = Fx.casing1;
                shootSound = Sounds.shootMissileShort;
                bullet = new MissileBulletType(2.7f, 12, "missile"){{
                    keepVelocity = true;
                    width = 8f;
                    height = 8f;
                    shrinkY = 0f;
                    drag = -0.003f;
                    homingRange = 60f;
                    splashDamageRadius = 25f;
                    splashDamage = 10f;
                    lifetime = 65f;
                    trailColor = Color.gray;
                    backColor = Pal.bulletYellowBack;
                    frontColor = Pal.bulletYellow;
                    hitEffect = Fx.blastExplosion;
                    despawnEffect = Fx.blastExplosion;
                    weaveScale = 8f;
                    weaveMag = 2f;
                }};
            }});
        }};

        minke = new UnitType("minke"){{
            health = 600;
            speed = 6.75f;
            drag = 0.15f;
            hitSize = 13f;
            armor = 4f;
            accel = 0.3f;
            rotateSpeed = 3f; // 180 deg/sec
            faceTarget = false;
            ammoType = new ItemAmmoType(Items.graphite);

            moveSoundVolume = 0.55f;
            moveSoundPitchMin = moveSoundPitchMax = 0.9f;
            moveSound = Sounds.shipMove;

            trailLength = 20;
            waveTrailX = 5.5f;
            waveTrailY = -4f;
            trailScl = 1.9f;

            weapons.add(new Weapon("mount-weapon"){{
                reload = 10f;
                x = 5f;
                y = 3.5f;
                rotate = true;
                rotateSpeed = 3f; // 180 deg/sec
                inaccuracy = 8f;
                ejectEffect = Fx.casing1;
                shootSound = Sounds.shootDuo;
                bullet = new FlakBulletType(4.2f, 3){{
                    lifetime = 52.5f;
                    ammoMultiplier = 4f;
                    shootEffect = Fx.shootSmall;
                    width = 6f;
                    height = 8f;
                    hitEffect = Fx.flakExplosion;
                    splashDamage = 27f * 1.5f;
                    splashDamageRadius = 15f;
                }};
            }});

            weapons.add(new Weapon("artillery-mount"){{
                reload = 30f;
                x = 5f;
                y = -5f;
                rotate = true;
                inaccuracy = 2f;
                rotateSpeed = 3f; // 180 deg/sec
                shake = 1.5f;
                ejectEffect = Fx.casing2;
                shootSound = Sounds.shootArtillerySmall;
                bullet = new ArtilleryBulletType(3f, 20, "shell"){{
                    hitEffect = Fx.flakExplosion;
                    knockback = 0.8f;
                    lifetime = 73.5f;
                    width = height = 11f;
                    collidesTiles = false;
                    splashDamageRadius = 30f * 0.75f;
                    splashDamage = 40f;
                }};
            }});
        }};

        bryde = new UnitType("bryde"){{
            health = 910;
            speed = 6.375f;
            accel = 0.2f;
            rotateSpeed = 3f; // 180 deg/sec
            drag = 0.17f;
            hitSize = 20f;
            armor = 7f;
            faceTarget = false;
            ammoType = new ItemAmmoType(Items.graphite);

            moveSoundVolume = 0.7f;
            moveSoundPitchMin = moveSoundPitchMax = 0.77f;
            moveSound = Sounds.shipMove;

            trailLength = 22;
            waveTrailX = 7f;
            waveTrailY = -9f;
            trailScl = 1.5f;


            weapons.add(new Weapon("large-artillery"){{
                reload = 65f;
                mirror = false;
                x = 0f;
                y = -3.5f;
                rotateSpeed = 3f; // 180 deg/sec
                rotate = true;
                shootY = 7f;
                shake = 5f;
                recoil = 4f;
                shadow = 12f;

                inaccuracy = 3f;
                ejectEffect = Fx.casing3;
                shootSound = Sounds.shootArtillery;

                bullet = new ArtilleryBulletType(3.2f, 15){{
                    trailMult = 0.8f;
                    hitEffect = Fx.massiveExplosion;
                    knockback = 1.5f;
                    lifetime = 84f;
                    height = 15.5f;
                    width = 15f;
                    collidesTiles = false;
                    splashDamageRadius = 40f;
                    splashDamage = 70f;
                    backColor = Pal.missileYellowBack;
                    frontColor = Pal.missileYellow;
                    trailEffect = Fx.artilleryTrail;
                    trailSize = 6f;
                    hitShake = 4f;

                    shootEffect = Fx.shootBig2;

                    status = StatusEffects.blasted;
                    statusDuration = 60f;
                }};
            }});

            weapons.add(new Weapon("missiles-mount"){{
                reload = 20f;
                x = 8.5f;
                y = -9f;

                shadow = 6f;

                rotateSpeed = 3f; // 180 deg/sec
                rotate = true;
                shoot.shots = 2;
                shoot.shotDelay = 3f;

                inaccuracy = 5f;
                velocityRnd = 0.1f;
                shootSound = Sounds.shootMissileShort;
                ammoType = new ItemAmmoType(Items.thorium);

                ejectEffect = Fx.none;
                bullet = new MissileBulletType(2.7f, 12){{
                    width = 8f;
                    height = 8f;
                    shrinkY = 0f;
                    drag = -0.003f;
                    homingRange = 60f;
                    keepVelocity = false;
                    splashDamageRadius = 25f;
                    splashDamage = 10f;
                    lifetime = 70f;
                    trailColor = Color.gray;
                    backColor = Pal.bulletYellowBack;
                    frontColor = Pal.bulletYellow;
                    hitEffect = Fx.blastExplosion;
                    despawnEffect = Fx.blastExplosion;
                    weaveScale = 8f;
                    weaveMag = 1f;
                }};
            }});
        }};

        sei = new UnitType("sei"){{
            health = 11000;
            armor = 12f;

            speed = 5.475f;
            drag = 0.17f;
            hitSize = 39f;
            accel = 0.2f;
            rotateSpeed = 3f; // 180 deg/sec
            faceTarget = false;
            ammoType = new ItemAmmoType(Items.thorium);

            moveSoundVolume = 1f;
            moveSound = Sounds.shipMoveBig;
            moveSoundPitchMin = moveSoundPitchMax = 0.95f;

            trailLength = 50;
            waveTrailX = 18f;
            waveTrailY = -21f;
            trailScl = 3f;

            weapons.add(new Weapon("sei-launcher"){{

                x = 0f;
                y = 0f;
                rotate = true;
                rotateSpeed = 3f; // 180 deg/sec
                mirror = false;

                shadow = 20f;

                shootY = 4.5f;
                recoil = 4f;
                reload = 45f;
                velocityRnd = 0.4f;
                inaccuracy = 7f;
                ejectEffect = Fx.none;
                shake = 1f;
                shootSound = Sounds.shootMissileLong;

                shoot = new ShootAlternate(){{
                    shots = 6;
                    shotDelay = 1.5f;
                    spread = 4f;
                    barrels = 3;
                }};

                bullet = new MissileBulletType(4.2f, 42){{
                    homingPower = 0.12f;
                    width = 8f;
                    height = 8f;
                    shrinkX = shrinkY = 0f;
                    drag = -0.003f;
                    homingRange = 80f;
                    keepVelocity = false;
                    splashDamageRadius = 35f;
                    splashDamage = 45f;
                    lifetime = 62f;
                    trailColor = Pal.bulletYellowBack;
                    backColor = Pal.bulletYellowBack;
                    frontColor = Pal.bulletYellow;
                    hitEffect = Fx.blastExplosion;
                    despawnEffect = Fx.blastExplosion;
                    weaveScale = 8f;
                    weaveMag = 2f;
                }};
            }});

            weapons.add(new Weapon("large-bullet-mount"){{
                reload = 60f;
                cooldownTime = 90f;
                x = 70f/4f;
                y = -66f/4f;
                rotateSpeed = 3f; // 180 deg/sec
                rotate = true;
                shootY = 7f;
                shake = 2f;
                recoil = 3f;
                shadow = 12f;
                ejectEffect = Fx.casing3;
                shootSound = Sounds.shootSpectre;

                shoot.shots = 3;
                shoot.shotDelay = 4f;

                inaccuracy = 1f;
                bullet = new BasicBulletType(7f, 57){{
                    width = 13f;
                    height = 19f;
                    shootEffect = Fx.shootBig;
                    lifetime = 35f;
                }};
            }});
        }};

        omura = new UnitType("omura"){{
            health = 22000;
            speed = 4.65f;
            drag = 0.18f;
            hitSize = 58f;
            armor = 16f;
            accel = 0.19f;
            rotateSpeed = 3f; // 180 deg/sec
            faceTarget = false;
            ammoType = new PowerAmmoType(4000);

            moveSoundVolume = 1.1f;
            moveSound = Sounds.shipMoveBig;
            moveSoundPitchMin = moveSoundPitchMax = 0.9f;

            trailLength = 70;
            waveTrailX = 23f;
            waveTrailY = -32f;
            trailScl = 3.5f;

            weapons.add(new Weapon("omura-cannon"){{
                reload = 110f;
                cooldownTime = 90f;
                mirror = false;
                x = 0f;
                y = -3.5f;
                rotateSpeed = 3f; // 180 deg/sec
                rotate = true;
                shootY = 23f;
                shake = 6f;
                recoil = 10.5f;
                shadow = 50f;
                shootSound = Sounds.shootOmura;

                ejectEffect = Fx.none;

                bullet = new RailBulletType(){{
                    shootEffect = Fx.railShoot;
                    length = 500;
                    pointEffectSpace = 60f;
                    pierceEffect = Fx.railHit;
                    pointEffect = Fx.railTrail;
                    hitEffect = Fx.massiveExplosion;
                    smokeEffect = Fx.shootBig2;
                    damage = 1250;
                    pierceDamageFactor = 0.5f;
                }};
            }});
        }};

        //endregion
        //region naval support
        retusa = new UnitType("retusa"){{
            speed = 6.75f;
            drag = 0.14f;
            hitSize = 11f;
            health = 270;
            accel = 0.4f;
            rotateSpeed = 3f; // 180 deg/sec
            trailLength = 20;
            waveTrailX = 5f;
            trailScl = 1.3f;
            faceTarget = false;
            range = 100f;
            ammoType = new PowerAmmoType(900);
            armor = 3f;

            moveSoundVolume = 0.4f;
            moveSound = Sounds.shipMove;

            buildSpeed = 1.5f;
            rotateToBuilding = false;

            weapons.add(new RepairBeamWeapon("repair-beam-weapon-center"){{
                x = 0f;
                y = -5.5f;
                shootY = 6f;
                beamWidth = 0.8f;
                mirror = false;
                repairSpeed = 0.75f;

                bullet = new BulletType(){{
                    maxRange = 120f;
                }};
            }});

            weapons.add(new Weapon("retusa-weapon"){{
                shootSound = Sounds.shootLaser;
                reload = 22f;
                x = 4.5f;
                y = -3.5f;
                rotateSpeed = 3f; // 180 deg/sec
                mirror = true;
                rotate = true;
                bullet = new LaserBoltBulletType(5.2f, 12){{
                    lifetime = 30f;
                    healPercent = 5.5f;
                    collidesTeam = true;
                    backColor = Pal.heal;
                    frontColor = Color.white;
                }};
            }});

            weapons.add(new Weapon(){{
                mirror = false;
                rotate = true;
                reload = 90f;
                x = y = shootX = shootY = 0f;
                shootSound = Sounds.shootRetusa;
                rotateSpeed = 3f; // 180 deg/sec
                shootSoundVolume = 0.9f;

                shoot.shots = 3;
                shoot.shotDelay = 7f;

                bullet = new BasicBulletType(){{
                    sprite = "mine-bullet";
                    width = height = 8f;
                    layer = Layer.scorch;
                    shootEffect = smokeEffect = Fx.none;

                    maxRange = 50f;
                    ignoreRotation = true;
                    healPercent = 4f;

                    backColor = Pal.heal;
                    frontColor = Color.white;
                    mixColorTo = Color.white;

                    hitSound = Sounds.explosionPlasmaSmall;
                    underwater = true;

                    ejectEffect = Fx.none;
                    hitSize = 22f;

                    collidesAir = false;

                    lifetime = 87f;

                    hitEffect = new MultiEffect(Fx.blastExplosion, Fx.greenCloud);
                    keepVelocity = false;

                    shrinkX = shrinkY = 0f;

                    inaccuracy = 2f;
                    weaveMag = 5f;
                    weaveScale = 4f;
                    speed = 0.7f;
                    drag = -0.017f;
                    homingPower = 0.05f;
                    collideFloor = true;
                    trailColor = Pal.heal;
                    trailWidth = 3f;
                    trailLength = 8;

                    splashDamage = 40f;
                    splashDamageRadius = 32f;
                }};
            }});
        }};

        oxynoe = new UnitType("oxynoe"){{
            health = 560;
            speed = 6.225f;
            drag = 0.14f;
            hitSize = 14f;
            armor = 4f;
            accel = 0.4f;
            rotateSpeed = 3f; // 180 deg/sec
            faceTarget = false;

            moveSoundVolume = 0.55f;
            moveSoundPitchMin = moveSoundPitchMax = 0.9f;
            moveSound = Sounds.shipMove;

            trailLength = 22;
            waveTrailX = 5.5f;
            waveTrailY = -4f;
            trailScl = 1.9f;
            ammoType = new ItemAmmoType(Items.coal);

            abilities.add(new StatusFieldAbility(StatusEffects.overclock, 60f * 6, 60f * 6f, 60f));

            buildSpeed = 2f;
            rotateToBuilding = false;

            weapons.add(new Weapon("plasma-mount-weapon"){{

                reload = 5f;
                x = 4.5f;
                y = 6.5f;
                rotate = true;
                rotateSpeed = 3f; // 180 deg/sec
                inaccuracy = 10f;
                ejectEffect = Fx.casing1;
                shootSound = Sounds.shootFlamePlasma;
                shootSoundVolume = 0.9f;
                shootCone = 30f;

                bullet = new BulletType(3.4f, 23f){{
                    healPercent = 1.5f;
                    collidesTeam = true;
                    ammoMultiplier = 3f;
                    hitSize = 7f;
                    lifetime = 18f;
                    pierce = true;
                    collidesAir = false;
                    statusDuration = 60f * 4;
                    hitEffect = Fx.hitFlamePlasma;
                    ejectEffect = Fx.none;
                    despawnEffect = Fx.none;
                    status = StatusEffects.burning;
                    keepVelocity = false;
                    hittable = false;
                    shootEffect = new Effect(32f, 80f, e -> {
                        color(Color.white, Pal.heal, Color.gray, e.fin());

                        randLenVectors(e.id, 8, e.finpow() * 60f, e.rotation, 10f, (x, y) -> {
                            Fill.circle(e.x + x, e.y + y, 0.65f + e.fout() * 1.5f);
                            Drawf.light(e.x + x, e.y + y, 16f * e.fout(), Pal.heal, 0.6f);
                        });
                    });
                }};
            }});

            weapons.add(new PointDefenseWeapon("point-defense-mount"){{
                mirror = false;
                x = 0f;
                y = 1f;
                reload = 9f;
                targetInterval = 10f;
                targetSwitchInterval = 15f;

                bullet = new BulletType(){{
                    shootEffect = Fx.sparkShoot;
                    hitEffect = Fx.pointHit;
                    maxRange = 100f;
                    damage = 17f;
                }};
            }});

        }};

        cyerce = new UnitType("cyerce"){{
            health = 870;
            speed = 6.45f;
            accel = 0.22f;
            rotateSpeed = 3f; // 180 deg/sec
            drag = 0.16f;
            hitSize = 20f;
            armor = 6f;
            faceTarget = false;
            ammoType = new ItemAmmoType(Items.graphite);

            moveSoundVolume = 0.7f;
            moveSoundPitchMin = moveSoundPitchMax = 0.77f;
            moveSound = Sounds.shipMove;

            trailLength = 23;
            waveTrailX = 9f;
            waveTrailY = -9f;
            trailScl = 2f;

            buildSpeed = 2f;
            rotateToBuilding = false;

            weapons.add(new RepairBeamWeapon("repair-beam-weapon-center"){{
                x = 11f;
                y = -10f;
                shootY = 6f;
                beamWidth = 0.8f;
                repairSpeed = 0.7f;

                bullet = new BulletType(){{
                    maxRange = 130f;
                }};
            }});

            weapons.add(new Weapon("plasma-missile-mount"){{
                reload = 60f;
                x = 9f;
                y = 3f;

                shadow = 5f;

                rotateSpeed = 3f; // 180 deg/sec
                rotate = true;
                inaccuracy = 1f;
                velocityRnd = 0.1f;
                shootSound = Sounds.shootMissilePlasma;

                ejectEffect = Fx.none;
                bullet = new FlakBulletType(2.5f, 25){{
                    sprite = "missile-large";
                    //for targeting
                    collidesGround = collidesAir = true;
                    explodeRange = 40f;
                    width = height = 12f;
                    shrinkY = 0f;
                    drag = -0.003f;
                    homingRange = 60f;
                    keepVelocity = false;
                    lightRadius = 60f;
                    lightOpacity = 0.7f;
                    lightColor = Pal.heal;
                    despawnSound = Sounds.explosion;

                    splashDamageRadius = 30f;
                    splashDamage = 25f;

                    lifetime = 80f;
                    backColor = Pal.heal;
                    frontColor = Color.white;

                    hitEffect = new ExplosionEffect(){{
                        lifetime = 28f;
                        waveStroke = 6f;
                        waveLife = 10f;
                        waveRadBase = 7f;
                        waveColor = Pal.heal;
                        waveRad = 30f;
                        smokes = 6;
                        smokeColor = Color.white;
                        sparkColor = Pal.heal;
                        sparks = 6;
                        sparkRad = 35f;
                        sparkStroke = 1.5f;
                        sparkLen = 4f;
                    }};

                    weaveScale = 8f;
                    weaveMag = 1f;

                    trailColor = Pal.heal;
                    trailWidth = 4.5f;
                    trailLength = 29;

                    fragBullets = 7;
                    fragVelocityMin = 0.3f;

                    fragBullet = new MissileBulletType(3.9f, 11){{
                        homingPower = 0.2f;
                        weaveMag = 4;
                        weaveScale = 4;
                        lifetime = 60f;
                        keepVelocity = false;
                        shootEffect = Fx.shootHeal;
                        smokeEffect = Fx.hitLaser;
                        splashDamage = 13f;
                        splashDamageRadius = 20f;
                        frontColor = Color.white;
                        hitSound = Sounds.none;

                        lightColor = Pal.heal;
                        lightRadius = 40f;
                        lightOpacity = 0.7f;

                        trailColor = Pal.heal;
                        trailWidth = 2.5f;
                        trailLength = 20;
                        trailChance = -1f;

                        healPercent = 2.8f;
                        collidesTeam = true;
                        backColor = Pal.heal;

                        despawnEffect = Fx.none;
                        hitEffect = new ExplosionEffect(){{
                            lifetime = 20f;
                            waveStroke = 2f;
                            waveColor = Pal.heal;
                            waveRad = 12f;
                            smokeSize = 0f;
                            smokeSizeBase = 0f;
                            sparkColor = Pal.heal;
                            sparks = 9;
                            sparkRad = 35f;
                            sparkLen = 4f;
                            sparkStroke = 1.5f;
                        }};
                    }};
                }};
            }});
        }};

        aegires = new UnitType("aegires"){{
            health = 12000;
            armor = 12f;

            speed = 5.25f;
            drag = 0.17f;
            hitSize = 44f;
            accel = 0.2f;
            rotateSpeed = 3f; // 180 deg/sec
            faceTarget = false;
            ammoType = new PowerAmmoType(3500);
            ammoCapacity = 40;

            moveSoundVolume = 1f;
            moveSound = Sounds.shipMoveBig;
            moveSoundPitchMin = moveSoundPitchMax = 0.95f;

            //clip size is massive due to energy field
            clipSize = 250f;

            trailLength = 50;
            waveTrailX = 18f;
            waveTrailY = -17f;
            trailScl = 3.2f;

            buildSpeed = 3f;
            rotateToBuilding = false;
            range = maxRange = 180f;

            abilities.add(new EnergyFieldAbility(40f, 65f, 180f){{
                statusDuration = 60f * 6f;
                maxTargets = 25;
                healPercent = 1.5f;
                sameTypeHealMult = 0.5f;
            }});

            for(float mountY : new float[]{-18f, 14}){
                weapons.add(new PointDefenseWeapon("point-defense-mount"){{
                    x = 12.5f;
                    y = mountY;
                    reload = 4f;
                    targetInterval = 8f;
                    targetSwitchInterval = 8f;

                    bullet = new BulletType(){{
                        shootEffect = Fx.sparkShoot;
                        hitEffect = Fx.pointHit;
                        maxRange = 180f;
                        damage = 30f;
                    }};
                }});
            }
        }};

        navanax = new UnitType("navanax"){{
            health = 20000;
            speed = 4.875f;
            drag = 0.17f;
            hitSize = 58f;
            armor = 16f;
            accel = 0.2f;
            rotateSpeed = 3f; // 180 deg/sec
            faceTarget = false;
            ammoType = new PowerAmmoType(4500);

            moveSoundVolume = 1.1f;
            moveSound = Sounds.shipMoveBig;
            moveSoundPitchMin = moveSoundPitchMax = 0.9f;

            trailLength = 70;
            waveTrailX = 23f;
            waveTrailY = -32f;
            trailScl = 3.5f;

            buildSpeed = 3.5f;
            rotateToBuilding = false;

            for(float mountY : new float[]{-117/4f, 50/4f}){
                for(float sign : Mathf.signs){
                    weapons.add(new Weapon("plasma-laser-mount"){{
                        shadow = 20f;
                        controllable = false;
                        autoTarget = true;
                        mirror = false;
                        shake = 3f;
                        shootY = 7f;
                        rotate = true;
                        x = 84f/4f * sign;
                        y = mountY;

                        targetInterval = 20f;
                        targetSwitchInterval = 35f;

                        rotateSpeed = 3f; // 180 deg/sec
                        reload = 170f;
                        recoil = 1f;
                        shootSound = Sounds.beamPlasmaSmall;
                        initialShootSound = Sounds.shootBeamPlasmaSmall;
                        continuous = true;
                        cooldownTime = reload;
                        immunities.add(StatusEffects.burning);

                        bullet = new ContinuousLaserBulletType(){{
                            maxRange = 90f;
                            damage = 27f;
                            length = 95f;
                            hitEffect = Fx.hitMeltHeal;
                            drawSize = 200f;
                            lifetime = 155f;
                            shake = 1f;

                            shootEffect = Fx.shootHeal;
                            smokeEffect = Fx.none;
                            width = 4f;
                            largeHit = false;

                            incendChance = 0.03f;
                            incendSpread = 5f;
                            incendAmount = 1;

                            healPercent = 0.4f;
                            collidesTeam = true;

                            colors = new Color[]{Pal.heal.cpy().a(.2f), Pal.heal.cpy().a(.5f), Pal.heal.cpy().mul(1.2f), Color.white};
                        }};
                    }});
                }
            }
            abilities.add(new SuppressionFieldAbility(){{
                orbRadius = 5;
                particleSize = 3;
                y = -10f;
                particles = 10;
                color = particleColor = effectColor = Pal.heal;
            }});
            weapons.add(new Weapon("emp-cannon-mount"){{
                rotate = true;

                x = 70f/4f;
                y = -26f/4f;

                reload = 65f;
                shake = 3f;
                rotateSpeed = 3f; // 180 deg/sec
                shadow = 30f;
                shootY = 7f;
                recoil = 4f;
                cooldownTime = reload - 10f;
                shootSound = Sounds.shootNavanax;

                bullet = new EmpBulletType(){{
                    float rad = 100f;

                    scaleLife = true;
                    lightOpacity = 0.7f;
                    unitDamageScl = 0.8f;
                    healPercent = 20f;
                    timeIncrease = 3f;
                    timeDuration = 60f * 20f;
                    powerDamageScl = 3f;
                    damage = 60;
                    hitColor = lightColor = Pal.heal;
                    lightRadius = 70f;
                    clipSize = 250f;
                    shootEffect = Fx.hitEmpSpark;
                    smokeEffect = Fx.shootBigSmoke2;
                    lifetime = 60f;
                    sprite = "circle-bullet";
                    backColor = Pal.heal;
                    frontColor = Color.white;
                    width = height = 12f;
                    shrinkY = 0f;
                    speed = 5f;
                    trailLength = 20;
                    trailWidth = 6f;
                    trailColor = Pal.heal;
                    trailInterval = 3f;
                    splashDamage = 70f;
                    splashDamageRadius = rad;
                    hitShake = 4f;
                    trailRotation = true;
                    status = StatusEffects.electrified;
                    hitSound = Sounds.explosionNavanax;

                    trailEffect = new Effect(16f, e -> {
                        color(Pal.heal);
                        for(int s : Mathf.signs){
                            Drawf.tri(e.x, e.y, 4f, 30f * e.fslope(), e.rotation + 90f*s);
                        }
                    });

                    hitEffect = new Effect(50f, 100f, e -> {
                        e.scaled(7f, b -> {
                            color(Pal.heal, b.fout());
                            Fill.circle(e.x, e.y, rad);
                        });

                        color(Pal.heal);
                        stroke(e.fout() * 3f);
                        Lines.circle(e.x, e.y, rad);

                        int points = 10;
                        float offset = Mathf.randomSeed(e.id, 360f);
                        for(int i = 0; i < points; i++){
                            float angle = i* 360f / points + offset;
                            //for(int s : Mathf.zeroOne){
                                Drawf.tri(e.x + Angles.trnsx(angle, rad), e.y + Angles.trnsy(angle, rad), 6f, 50f * e.fout(), angle/* + s*180f*/);
                            //}
                        }

                        Fill.circle(e.x, e.y, 12f * e.fout());
                        color();
                        Fill.circle(e.x, e.y, 6f * e.fout());
                        Drawf.light(e.x, e.y, rad * 1.6f, Pal.heal, e.fout());
                    });
                }};
            }});
        }};

        //endregion
        //region core

        alpha = new UnitType("alpha"){{
            controller = u -> u.team.isAI() ? new BuilderAI(true, 400f) : new CommandAI();
            isEnemy = false;

            targetBuildingsMobile = false;
            lowAltitude = true;
            flying = true;
            mineSpeed = 6.5f;
            mineTier = 1;
            buildSpeed = 0.5f;
            drag = 0.05f;
            speed = 22.5f;
            rotateSpeed = 3f; // 180 deg/sec
            accel = 0.1f;
            fogRadius = 0f;
            itemCapacity = 30;
            health = 150f;
            engineOffset = 6f;
            hitSize = 8f;
            alwaysUnlocked = true;
            wreckSoundVolume = 0.8f;
            deathSoundVolume = 0.7f;

            weapons.add(new Weapon("small-basic-weapon"){{
                reload = 17f;
                x = 2.75f;
                y = 1f;
                top = false;
                shootSound = Sounds.shootAlpha;

                bullet = new LaserBoltBulletType(2.5f, 11){{
                    keepVelocity = false;
                    width = 1.5f;
                    height = 4.5f;
                    hitEffect = despawnEffect = Fx.hitBulletColor;
                    trailWidth = 1.2f;
                    trailLength = 3;
                    shootEffect = Fx.shootSmallColor;
                    smokeEffect = Fx.hitLaserColor;
                    backColor = trailColor = Pal.yellowBoltFront;
                    hitColor = Pal.yellowBoltFront;
                    frontColor = Color.white;

                    lifetime = 60f;
                    buildingDamageMultiplier = 0.01f;
                }};
            }});
        }};

        beta = new UnitType("beta"){{
            controller = u -> u.team.isAI() ? new BuilderAI(true, 400f) : new CommandAI();
            isEnemy = false;

            targetBuildingsMobile = false;
            flying = true;
            mineSpeed = 7f;
            mineTier = 1;
            buildSpeed = 0.75f;
            drag = 0.05f;
            speed = 24.75f;
            rotateSpeed = 3f; // 180 deg/sec
            accel = 0.1f;
            fogRadius = 0f;
            itemCapacity = 50;
            health = 170f;
            engineOffset = 6f;
            hitSize = 9f;
            lowAltitude = true;

            weapons.add(new Weapon("small-mount-weapon"){{
                top = false;
                reload = 20f;
                x = 3f;
                y = 1f;
                recoil = 1f;
                shoot.shots = 2;
                shoot.shotDelay = 4f;
                shootSound = Sounds.shootAlpha;

                bullet = new LaserBoltBulletType(3f, 11){{
                    keepVelocity = false;
                    width = 1.5f;
                    height = 4.5f;
                    hitEffect = despawnEffect = Fx.hitBulletColor;
                    trailWidth = 1.2f;
                    trailLength = 3;
                    shootEffect = Fx.shootSmallColor;
                    smokeEffect = Fx.hitLaserColor;
                    backColor = trailColor = Pal.yellowBoltFront;
                    hitColor = Pal.yellowBoltFront;
                    frontColor = Color.white;

                    lifetime = 60f;
                    buildingDamageMultiplier = 0.01f;
                }};
            }});
        }};

        gamma = new UnitType("gamma"){{
            controller = u -> u.team.isAI() ? new BuilderAI(true, 400f) : new CommandAI();
            isEnemy = false;

            targetBuildingsMobile = false;
            lowAltitude = true;
            flying = true;
            mineSpeed = 8f;
            mineTier = 2;
            buildSpeed = 1f;
            drag = 0.05f;
            speed = 26.625f;
            rotateSpeed = 3f; // 180 deg/sec
            accel = 0.11f;
            fogRadius = 0f;
            itemCapacity = 70;
            health = 220f;
            engineOffset = 6f;
            hitSize = 11f;

            weapons.add(new Weapon("small-mount-weapon"){{
                top = false;
                reload = 15f;
                x = 1f;
                y = 2f;
                shoot = new ShootSpread(){{
                    shots = 2;
                    shotDelay = 3f;
                    spread = 2f;
                }};

                inaccuracy = 3f;
                shootSound = Sounds.shootAlpha;

                bullet = new LaserBoltBulletType(3.5f, 11){{
                    keepVelocity = false;
                    width = 1.5f;
                    height = 5f;
                    hitEffect = despawnEffect = Fx.hitBulletColor;
                    trailWidth = 1.2f;
                    trailLength = 4;
                    shootEffect = Fx.shootSmallColor;
                    smokeEffect = Fx.hitLaserColor;
                    backColor = trailColor = Pal.yellowBoltFront;
                    hitColor = Pal.yellowBoltFront;
                    frontColor = Color.white;

                    lifetime = 70f;
                    buildingDamageMultiplier = 0.01f;
                    homingPower = 0.04f;
                }};
            }});
        }};

        //endregion
        //region erekir - tank

        stell = new TankUnitType("stell"){{
            hitSize = 12f;
            treadPullOffset = 3;
            speed = 5.625f;
            rotateSpeed = 3f; // 180 deg/sec
            health = 850;
            armor = 6f;
            itemCapacity = 0;
            floorMultiplier = 0.95f;
            treadRects = new Rect[]{new Rect(12 - 32f, 7 - 32f, 14, 51)};
            researchCostMultiplier = 0f;

            tankMoveVolume *= 0.32f;
            tankMoveSound = Sounds.tankMoveSmall;

            weapons.add(new Weapon("stell-weapon"){{
                shootSound = Sounds.shootStell;
                layerOffset = 0.0001f;
                reload = 50f;
                shootY = 4.5f;
                recoil = 1f;
                rotate = true;
                rotateSpeed = 3f; // 180 deg/sec
                mirror = false;
                x = 0f;
                y = -0.75f;
                heatColor = Color.valueOf("f9350f");
                cooldownTime = 30f;

                bullet = new BasicBulletType(4f, 40){{
                    sprite = "missile-large";
                    smokeEffect = Fx.shootBigSmoke;
                    shootEffect = Fx.shootBigColor;
                    width = 5f;
                    height = 7f;
                    lifetime = 40f;
                    hitSize = 4f;
                    hitColor = backColor = trailColor = Color.valueOf("feb380");
                    frontColor = Color.white;
                    trailWidth = 1.7f;
                    trailLength = 5;
                    despawnEffect = hitEffect = Fx.hitBulletColor;
                }};
            }});
        }};

        locus = new TankUnitType("locus"){
            @Override
            public void drawOutline(Unit unit){
                Draw.reset();

                if(Core.atlas.isFound(outlineRegion)){
                    applyColor(unit);
                    applyOutlineColor(unit);
                    drawRegionExplicit(outlineRegion, unit.x, unit.y, unit.rotation - 90f);
                    Draw.reset();
                }
            }

            @Override
            public void drawBody(Unit unit){
                applyColor(unit);

                if(unit instanceof UnderwaterMovec){
                    Draw.alpha(1f);
                    Draw.mixcol(unit.floorOn().mapColor.write(Tmp.c1).mul(0.9f), 1f);
                }

                drawRegionExplicit(region, unit.x, unit.y, unit.rotation - 90f);
                Draw.reset();
            }

            @Override
            public void drawCell(Unit unit){
                applyColor(unit);
                Draw.color(cellColor(unit));
                drawRegionExplicit(cellRegion, unit.x, unit.y, unit.rotation - 90f);
                Draw.reset();
            }

            @Override
            public <T extends Unit & Tankc> void drawTank(T unit){
                applyColor(unit);
                drawRegionExplicit(treadRegion, unit.x, unit.y, unit.rotation - 90f);

                if(treadRegion.found()){
                    int frame = (int)(unit.treadTime()) % treadFrames;
                    for(int i = 0; i < treadRects.length; i++){
                        var region = treadRegions[i][frame];
                        var treadRect = treadRects[i];
                        float xOffset = -(treadRect.x + treadRect.width / 2f);
                        float yOffset = -(treadRect.y + treadRect.height / 2f);

                        for(int side : Mathf.signs){
                            Tmp.v1.set(xOffset * side, yOffset).rotate(unit.rotation - 90f);
                            Draw.rect(region, unit.x + Tmp.v1.x * scaledTankVisualScale / 4f, unit.y + Tmp.v1.y * scaledTankVisualScale / 4f, treadRect.width * scaledTankVisualScale / 4f, region.height * region.scale * scaledTankVisualScale / 4f, unit.rotation - 90f);
                        }
                    }
                }
            }

            @Override
            public void drawWeaponOutlines(Unit unit){
                float prevX = Draw.xscl, prevY = Draw.yscl;
                Draw.scl(prevX * scaledTankVisualScale, prevY * scaledTankVisualScale);
                super.drawWeaponOutlines(unit);
                Draw.scl(prevX, prevY);
            }

            @Override
            public void drawWeapons(Unit unit){
                float prevX = Draw.xscl, prevY = Draw.yscl;
                Draw.scl(prevX * scaledTankVisualScale, prevY * scaledTankVisualScale);
                super.drawWeapons(unit);
                Draw.scl(prevX, prevY);
            }

            @Override
            public void drawShadow(Unit unit){
                drawShadowExplicit(shadowRegion, unit, shadowElevation, shadowElevationScl);
            }

            {
                hitSize = 18f;
                speed = 5.95f;
                rotateSpeed = 6f; // 360 deg/sec
                health = 90f;
                armor = 0f;
                armorType = ArmorType.light;
                unitClasses = EnumSet.of(UnitClass.mechanical);
                range = maxRange = 5f * tilesize;
                targetAir = false;
                targetGround = true;

                weapons.add(new Weapon("locus-weapon"){
                    private static final float postFireStiffDuration = 0.1f * 60f;

                    private boolean shouldCancelCharge(Unit unit, WeaponMount mount, float queuedMoveX, float queuedMoveY, int queuedQueueSize){
                        if(!unit.isAdded() || mount == null) return true;

                        //Simple rule: if player gives a new move command during lock-on, cancel this shot.
                        if(unit.controller() instanceof CommandAI ai && ai.currentCommand() == UnitCommand.moveCommand){
                            Position current = ai.targetPos;
                            float currentX = current == null ? Float.NaN : current.getX();
                            float currentY = current == null ? Float.NaN : current.getY();

                            boolean posChanged = (Float.isNaN(queuedMoveX) != Float.isNaN(currentX)) ||
                            (!Float.isNaN(currentX) && (!Mathf.equal(currentX, queuedMoveX, 0.01f) || !Mathf.equal(currentY, queuedMoveY, 0.01f)));

                            boolean queueChanged = ai.commandQueue.size != queuedQueueSize;

                            if(posChanged || queueChanged){
                                return true;
                            }
                        }
                        return false;
                    }

                    @Override
                    protected void shoot(Unit unit, WeaponMount mount, float shootX, float shootY, float rotation){
                        if(shoot.firstShotDelay <= 0f){
                            super.shoot(unit, mount, shootX, shootY, rotation);
                            return;
                        }

                        mount.charging = true;
                        mount.totalShots++;
                        int barrel = mount.barrelCounter++;
                        float queuedMoveX = Float.NaN, queuedMoveY = Float.NaN;
                        int queuedQueueSize = -1;
                        if(unit.controller() instanceof CommandAI ai){
                            queuedQueueSize = ai.commandQueue.size;
                            Position pos = ai.targetPos;
                            if(pos != null){
                                queuedMoveX = pos.getX();
                                queuedMoveY = pos.getY();
                            }
                        }
                        final float finalQueuedMoveX = queuedMoveX;
                        final float finalQueuedMoveY = queuedMoveY;
                        final int finalQueuedQueueSize = queuedQueueSize;

                        if(chargeSound != Sounds.none){
                            chargeSound.at(shootX, shootY, Mathf.random(soundPitchMin, soundPitchMax));
                        }
                        bullet.chargeEffect.at(shootX, shootY, rotation, bullet.keepVelocity || parentizeEffects ? unit : null);

                        Time.run(shoot.firstShotDelay, () -> {
                            if(shouldCancelCharge(unit, mount, finalQueuedMoveX, finalQueuedMoveY, finalQueuedQueueSize)){
                                mount.charging = false;
                                mount.reload = 0f;
                                return;
                            }

                            int prev = mount.barrelCounter;
                            mount.barrelCounter = barrel;
                            bullet(unit, mount, 0f, 0f, 0f, null);
                            mount.barrelCounter = prev;
                        });
                    }

                    @Override
                    protected void handleBullet(Unit unit, WeaponMount mount, Bullet bullet){
                        super.handleBullet(unit, mount, bullet);
                        unit.apply(StatusEffects.unmoving, postFireStiffDuration);
                    }

                    {
                        top = false;
                        shootSound = Sounds.shootFlame;
                        shootY = 10f;
                        rotate = true;
                        rotateSpeed = 6f; // 360 deg/sec
                        mirror = false;
                        x = 0f;
                        y = 0f;
                        reload = 1.79f * 60f;
                        shoot.firstShotDelay = 0.7f * 60f;
                        shootStatus = StatusEffects.none;
                        shootStatusDuration = 0f;

                        bullet = new ContinuousFlameBulletType(8f){
                    final float lightArmorDamage = 14f;

                    {
                        length = 5f * tilesize;
                        width = 0.2f * tilesize;
                        lifetime = 60f;
                        damageInterval = 61f;
                        lengthInterp = new Interp(){
                            @Override
                            public float apply(float a){
                                return 1f;
                            }
                        };
                        drawFlare = false;
                        collidesAir = false;
                        collidesGround = true;
                        pierce = true;
                        pierceBuilding = true;
                        pierceCap = -1;
                        hitSize = 0.4f * tilesize;
                        shootEffect = Fx.shootSmallFlame;
                        hitEffect = Fx.hitFlameSmall;
                        despawnEffect = Fx.none;
                        keepVelocity = false;
                        hittable = false;
                    }

                    @Override
                    public void init(Bullet b){
                        super.init(b);
                        applyDamage(b);
                    }

                    @Override
                    public void applyDamage(Bullet b){
                        float x1 = b.x, y1 = b.y;
                        float radius = 0.2f * tilesize;
                        Tmp.v1.trnsExact(b.rotation(), currentLength(b));
                        float x2 = x1 + Tmp.v1.x, y2 = y1 + Tmp.v1.y;

                        Rect unitRect = Tmp.r1.setPosition(x1, y1).setSize(Tmp.v1.x, Tmp.v1.y).normalize().grow(radius * 2f);

                        Units.nearbyEnemies(b.team, unitRect, u -> {
                            if(!u.checkTarget(collidesAir, collidesGround) || !u.hittable()) return;
                            u.hitbox(Tmp.r2);
                            Vec2 hit = Geometry.raycastRect(x1, y1, x2, y2, Tmp.r2.grow(radius * 2f));
                            if(hit != null){
                                u.collision(b, hit.x, hit.y);
                                b.collision(u, hit.x, hit.y);
                            }
                        });

                        Units.nearbyBuildings((x1 + x2) / 2f, (y1 + y2) / 2f, currentLength(b) / 2f + radius + 8f, build -> {
                            if(build.team == b.team || !build.collide(b)) return;
                            if(!b.checkUnderBuild(build, build.x, build.y)) return;
                            if(!intersectsCircle(x1, y1, x2, y2, build.x, build.y, build.hitSize() / 2f + radius)) return;

                            float health = build.health;
                            build.collision(b);
                            hit(b, build.x, build.y);
                            hitTile(b, build, build.x, build.y, health, false);
                        });
                    }

                    @Override
                    public float currentLength(Bullet b){
                        return length;
                    }

                    private boolean intersectsCircle(float x1, float y1, float x2, float y2, float cx, float cy, float radius){
                        float rs = radius * radius;
                        if(Mathf.dst2(x1, y1, cx, cy) <= rs || Mathf.dst2(x2, y2, cx, cy) <= rs){
                            return true;
                        }

                        float dx = x2 - x1, dy = y2 - y1;
                        float len2 = dx * dx + dy * dy;
                        if(len2 < 0.0001f) return false;

                        float t = ((cx - x1) * dx + (cy - y1) * dy) / len2;
                        t = Mathf.clamp(t, 0f, 1f);
                        float px = x1 + dx * t, py = y1 + dy * t;
                        return Mathf.dst2(px, py, cx, cy) <= rs;
                    }

                    @Override
                    public void hitEntity(Bullet b, Hitboxc entity, float health){
                        float prev = b.damage;
                        if(entity instanceof Unit && ((Unit)entity).type.armorType == ArmorType.light){
                            b.damage = lightArmorDamage;
                        }
                        super.hitEntity(b, entity, health);
                        b.damage = prev;
                    }
                        };
                    }
                });
            }
        };

        precept = new TankUnitType("precept"){
            TextureRegion siegeLegRegion, siegeFootRegion;

            private float siegeLegProgress(Unit unit){
                return preceptTransitionProgress(unit);
            }

            private void drawSiegeLegs(Unit unit){
                float progress = siegeLegProgress(unit);
                if(progress <= 0.001f || !siegeLegRegion.found()) return;

                float prev = Draw.z();
                Draw.z(Layer.groundUnit - 0.015f);

                float inner = unit.hitSize * 0.14f;
                float reach = unit.hitSize * (0.22f + 0.32f * progress) * 1.5f;
                float legScl = 0.52f + 0.28f * progress;
                float footScl = 0.48f + 0.24f * progress;
                float baseXscl = Draw.xscl, baseYscl = Draw.yscl;

                for(int i = 0; i < 6; i++){
                    float angle = unit.rotation - 90f + i * 60f;
                    float sx = unit.x + Angles.trnsx(angle, inner);
                    float sy = unit.y + Angles.trnsy(angle, inner);
                    float fx = unit.x + Angles.trnsx(angle, inner + reach);
                    float fy = unit.y + Angles.trnsy(angle, inner + reach);
                    float mx = (sx + fx) * 0.5f;
                    float my = (sy + fy) * 0.5f;

                    Draw.scl(baseXscl * legScl, baseYscl * legScl);
                    Draw.rect(siegeLegRegion, mx, my, angle);
                    Draw.scl(baseXscl, baseYscl);

                    if(siegeFootRegion.found()){
                        Draw.scl(baseXscl * footScl, baseYscl * footScl);
                        Draw.rect(siegeFootRegion, fx, fy, angle);
                        Draw.scl(baseXscl, baseYscl);
                    }
                }

                Draw.z(prev);
                Draw.reset();
            }

            private void playSiegeFootEffect(Unit unit){
                float inner = unit.hitSize * 0.14f;
                float reach = unit.hitSize * 0.81f;

                for(int i = 0; i < 6; i++){
                    float angle = unit.rotation - 90f + i * 60f;
                    float fx = unit.x + Angles.trnsx(angle, inner + reach);
                    float fy = unit.y + Angles.trnsy(angle, inner + reach);
                    Fx.breakBlock.at(fx, fy, 0.55f);
                }
            }

            private @Nullable WeaponMount findWeaponMount(Unit unit, String weaponName){
                if(unit == null || unit.mounts == null) return null;
                for(WeaponMount mount : unit.mounts){
                    if(mount != null && mount.weapon != null && weaponName.equals(mount.weapon.name)){
                        return mount;
                    }
                }
                return null;
            }

            @Override
            public void load(){
                super.load();
                siegeLegRegion = Core.atlas.find("anthicus-leg");
                siegeFootRegion = Core.atlas.find("anthicus-leg-base", siegeLegRegion);
            }

            {
                hitSize = 12f;
                treadPullOffset = 5;
                speed = 3.15f;
                rotateSpeed = 6f; // 360 deg/sec
                health = 175f;
                armor = 1f;
                armorType = ArmorType.heavy;
                unitClasses = EnumSet.of(UnitClass.mechanical);
                range = maxRange = preceptMobileRange();
                targetAir = false;
                targetGround = true;
                alwaysShootWhenMoving = true;
                requireBodyAimToShoot = false;
                population = 3;
                itemCapacity = 0;
                floorMultiplier = 0.65f;
                drownTimeMultiplier = 1.2f;
                immunities.addAll(StatusEffects.burning, StatusEffects.melting);
                treadRects = new Rect[]{new Rect(16 - 60f, 48 - 70f, 30, 75), new Rect(44 - 60f, 17 - 70f, 17, 60)};
                crushFragile = true;
                researchCostMultiplier = 0f;

                weapons.add(new Weapon("precept-weapon"){
                    @Override
                    public void update(Unit unit, WeaponMount mount){
                        if(preceptIsSieged(unit) || preceptIsSieging(unit) || preceptIsUnsieging(unit)){
                            if(preceptIsSieged(unit)){
                                WeaponMount siegeMount = findWeaponMount(unit, "precept-siege-weapon");
                                if(siegeMount != null){
                                    mount.rotation = siegeMount.rotation;
                                    mount.targetRotation = siegeMount.targetRotation;
                                }
                            }
                            mount.shoot = false;
                            mount.rotate = false;
                            mount.warmup = Mathf.approachDelta(mount.warmup, 0f, 0.08f);
                            mount.heat = Mathf.approachDelta(mount.heat, 0f, 0.08f);
                            return;
                        }
                        super.update(unit, mount);
                    }

                    {
                        shootSound = Sounds.explosionDull;
                        layerOffset = 0.0001f;
                        reload = preceptMobileReload;
                        shootY = 16f;
                        recoil = 1f;
                        rotate = true;
                        rotateSpeed = 6f; // 360 deg/sec
                        mirror = false;
                        shootCone = 8f;
                        x = 0f;
                        y = -1f;
                        heatColor = Color.valueOf("f9350f");
                        cooldownTime = 30f;
                        bullet = new PointBulletType(){
                            {
                                damage = 15f;
                                rangeOverride = preceptMobileRange();
                                collidesAir = false;
                                collidesGround = true;
                                hitEffect = Fx.none;
                                despawnEffect = Fx.none;
                                shootEffect = Fx.none;
                                smokeEffect = Fx.none;
                                trailEffect = Fx.none;
                            }

                            @Override
                            public void hitEntity(Bullet b, Hitboxc entity, float health){
                                float amount = damage;
                                if(entity instanceof Unit u && u.type.armorType == ArmorType.heavy){
                                    amount = 25f;
                                }

                                if(entity instanceof Healthc h){
                                    h.damagePierce(amount);
                                }

                                if(entity instanceof Unit unit){
                                    Tmp.v3.set(unit).sub(b).nor().scl(knockback * 80f);
                                    if(impact) Tmp.v3.setAngle(b.rotation() + (knockback < 0 ? 180f : 0f));
                                    unit.impulse(Tmp.v3);
                                    unit.apply(status, statusDuration);
                                    Events.fire(unitDamageEvent.set(unit, b));
                                }

                                handlePierce(b, health, entity.x(), entity.y());
                            }
                        };
                    }
                });

                weapons.add(new Weapon("precept-siege-weapon"){
                    @Override
                    public void update(Unit unit, WeaponMount mount){
                        if(!preceptIsSieged(unit)){
                            mount.shoot = false;
                            mount.rotate = false;
                            mount.warmup = Mathf.approachDelta(mount.warmup, 0f, 0.08f);
                            mount.heat = Mathf.approachDelta(mount.heat, 0f, 0.08f);
                            return;
                        }
                        super.update(unit, mount);
                    }

                    @Override
                    public void draw(Unit unit, WeaponMount mount){
                        //Uses base tank turret sprite from precept-weapon.
                    }

                    @Override
                    public void drawOutline(Unit unit, WeaponMount mount){
                        //Uses base tank turret sprite from precept-weapon.
                    }

                    @Override
                    protected void handleBullet(Unit unit, WeaponMount mount, Bullet bullet){
                        super.handleBullet(unit, mount, bullet);
                        markPreceptSiegeShot(unit);
                    }

                    {
                        shootSound = Sounds.explosionDull;
                        layerOffset = 0.0001f;
                        reload = preceptSiegeReload;
                        shootY = 16f;
                        recoil = 1f;
                        rotate = true;
                        rotateSpeed = 6f; // 360 deg/sec
                        mirror = false;
                        shootCone = 8f;
                        x = 0f;
                        y = -1f;
                        heatColor = Color.valueOf("f9350f");
                        cooldownTime = 30f;
                        targetAir = false;
                        targetGround = true;
                        noAttack = false;
                        bullet = new PointBulletType(){
                            {
                                damage = 40f;
                                splashDamage = 40f;
                                splashDamageRadius = 1.5f * tilesize;
                                rangeOverride = preceptSiegeRange();
                                collidesAir = false;
                                collidesGround = true;
                                hitEffect = Fx.none;
                                despawnEffect = Fx.none;
                                shootEffect = Fx.none;
                                smokeEffect = Fx.none;
                                trailEffect = Fx.none;
                            }

                            @Override
                            public void hitEntity(Bullet b, Hitboxc entity, float health){
                                float amount = damage;
                                if(entity instanceof Unit u && u.type.armorType == ArmorType.heavy){
                                    amount = 70f;
                                }

                                if(entity instanceof Healthc h){
                                    h.damagePierce(amount);
                                }

                                if(entity instanceof Unit unit){
                                    Tmp.v3.set(unit).sub(b).nor().scl(knockback * 80f);
                                    if(impact) Tmp.v3.setAngle(b.rotation() + (knockback < 0 ? 180f : 0f));
                                    unit.impulse(Tmp.v3);
                                    unit.apply(status, statusDuration);
                                    Events.fire(unitDamageEvent.set(unit, b));
                                }

                                handlePierce(b, health, entity.x(), entity.y());
                            }
                        };
                    }
                });
            }

            @Override
            public void update(Unit unit){
                super.update(unit);
                updatePreceptSiegeTimers(unit);

                if(preceptIsSieging(unit)){
                    unit.vel.setZero();
                    if(unit.controller() instanceof CommandAI ai){
                        ai.clearCommands();
                    }
                    if(unit.getDuration(StatusEffects.preceptSieging) <= 0.001f){
                        unit.unapply(StatusEffects.preceptSieging);
                        unit.apply(StatusEffects.preceptSieged, 1f);
                        playSiegeFootEffect(unit);
                    }
                }

                if(preceptIsUnsieging(unit)){
                    unit.vel.setZero();
                    if(unit.controller() instanceof CommandAI ai){
                        ai.clearCommands();
                    }
                    if(unit.getDuration(StatusEffects.preceptUnsieging) <= 0.001f){
                        unit.unapply(StatusEffects.preceptUnsieging);
                    }
                }

                if(preceptIsSieged(unit)){
                    unit.vel.setZero();
                }
            }

            @Override
            public void drawOutline(Unit unit){
                Draw.reset();

                if(Core.atlas.isFound(outlineRegion)){
                    applyColor(unit);
                    applyOutlineColor(unit);
                    drawRegionExplicit(outlineRegion, unit.x, unit.y, unit.rotation - 90f);
                    Draw.reset();
                }
            }

            @Override
            public void drawBody(Unit unit){
                applyColor(unit);

                if(unit instanceof UnderwaterMovec){
                    Draw.alpha(1f);
                    Draw.mixcol(unit.floorOn().mapColor.write(Tmp.c1).mul(0.9f), 1f);
                }

                drawRegionExplicit(region, unit.x, unit.y, unit.rotation - 90f);
                Draw.reset();
            }

            @Override
            public void drawCell(Unit unit){
                applyColor(unit);
                Draw.color(cellColor(unit));
                drawRegionExplicit(cellRegion, unit.x, unit.y, unit.rotation - 90f);
                Draw.reset();
            }

            @Override
            public <T extends Unit & Tankc> void drawTank(T unit){
                applyColor(unit);
                drawRegionExplicit(treadRegion, unit.x, unit.y, unit.rotation - 90f);

                if(treadRegion.found()){
                    int frame = (int)(unit.treadTime()) % treadFrames;
                    for(int i = 0; i < treadRects.length; i++){
                        var region = treadRegions[i][frame];
                        var treadRect = treadRects[i];
                        float xOffset = -(treadRect.x + treadRect.width / 2f);
                        float yOffset = -(treadRect.y + treadRect.height / 2f);

                        for(int side : Mathf.signs){
                            Tmp.v1.set(xOffset * side, yOffset).rotate(unit.rotation - 90f);
                            Draw.rect(region, unit.x + Tmp.v1.x * scaledTankVisualScale / 4f, unit.y + Tmp.v1.y * scaledTankVisualScale / 4f, treadRect.width * scaledTankVisualScale / 4f, region.height * region.scale * scaledTankVisualScale / 4f, unit.rotation - 90f);
                        }
                    }
                }
            }

            @Override
            public void drawWeaponOutlines(Unit unit){
                float prevX = Draw.xscl, prevY = Draw.yscl;
                Draw.scl(prevX * scaledTankVisualScale, prevY * scaledTankVisualScale);
                super.drawWeaponOutlines(unit);
                Draw.scl(prevX, prevY);
            }

            @Override
            public void drawWeapons(Unit unit){
                float prevX = Draw.xscl, prevY = Draw.yscl;
                Draw.scl(prevX * scaledTankVisualScale, prevY * scaledTankVisualScale);
                super.drawWeapons(unit);
                Draw.scl(prevX, prevY);
            }

            @Override
            public void drawShadow(Unit unit){
                drawShadowExplicit(shadowRegion, unit, shadowElevation, shadowElevationScl);
            }

            @Override
            public void draw(Unit unit){
                super.draw(unit);
                drawSiegeLegs(unit);
            }

            @Override
            public void killed(Unit unit){
                clearPreceptSiegeData(unit);
            }
        };

        hurricane = new TankUnitType("hurricane"){
            @Override
            public void load(){
                super.load();

                String copy = "precept";
                region = Core.atlas.find(copy);
                previewRegion = Core.atlas.find(copy + "-preview", copy);
                treadRegion = Core.atlas.find(copy + "-treads");
                if(treadRegion.found()){
                    treadRegions = new TextureRegion[treadRects.length][treadFrames];
                    for(int r = 0; r < treadRects.length; r++){
                        for(int i = 0; i < treadFrames; i++){
                            treadRegions[r][i] = Core.atlas.find(copy + "-treads" + r + "-" + i);
                        }
                    }
                }
                legBaseRegion = Core.atlas.find(copy + "-leg-base", copy + "-leg");
                baseRegion = Core.atlas.find(copy + "-base");
                cellRegion = Core.atlas.find(copy + "-cell", Core.atlas.find("power-cell"));
                outlineRegion = Core.atlas.find(copy + "-outline");
                wreckRegions = new TextureRegion[3];
                for(int i = 0; i < wreckRegions.length; i++){
                    wreckRegions[i] = Core.atlas.find(copy + "-wreck" + i);
                }
                clipSize = Math.max(region.width * 2f, clipSize);
            }

            {
                fullOverride = "precept";
                hitSize = 12f;
                treadPullOffset = 5;
                speed = 4.72f;
                rotateSpeed = 6f; // 360 deg/sec
                omniMovement = false;
                rotateMoveFirst = true;
                health = 120f;
                armor = 1f;
                armorType = ArmorType.heavy;
                unitClasses = EnumSet.of(UnitClass.mechanical);
                range = maxRange = hurricaneBaseRange();
                targetAir = true;
                targetGround = true;
                population = 3;
                itemCapacity = 0;
                floorMultiplier = 0.65f;
                drownTimeMultiplier = 1.2f;
                immunities.addAll(StatusEffects.burning, StatusEffects.melting);
                treadRects = new Rect[]{new Rect(16 - 60f, 48 - 70f, 30, 75), new Rect(44 - 60f, 17 - 70f, 17, 60)};
                crushFragile = true;
                researchCostMultiplier = 0f;

                weapons.add(new Weapon("anthicus"){
                    @Override
                    public void update(Unit unit, WeaponMount mount){
                        Teamc target = hurricaneTarget(unit);
                        boolean locked = target != null;
                        if(locked){
                            mount.target = target;
                            mount.aimX = target.getX();
                            mount.aimY = target.getY();
                            mount.shoot = true;
                            mount.rotate = true;
                        }

                        boolean previous = unit.type.alwaysShootWhenMoving;
                        if(locked){
                            unit.type.alwaysShootWhenMoving = true;
                        }

                        super.update(unit, mount);
                        unit.type.alwaysShootWhenMoving = previous;
                    }

                    @Override
                    public void draw(Unit unit, WeaponMount mount){
                        super.draw(unit, mount);

                        Teamc target = hurricaneTarget(unit);
                        if(target == null) return;

                        Draw.z(Layer.effect);
                        Draw.color(Color.valueOf("ff2f2f"));
                        Lines.stroke(0.625f);
                        Lines.line(unit.x, unit.y, target.getX(), target.getY());
                        Draw.reset();
                    }

                    @Override
                    protected void handleBullet(Unit unit, WeaponMount mount, Bullet bullet){
                        super.handleBullet(unit, mount, bullet);
                        bullet.data = mount.target;
                    }

                    {
                        shootSound = Sounds.explosionDull;
                        layerOffset = 0.0001f;
                        reload = 0.71f * 60f;
                        shootY = 16f;
                        recoil = 1.5f;
                        rotate = true;
                        rotateSpeed = 6f; // 360 deg/sec
                        mirror = false;
                        shootCone = 8f;
                        x = 0f;
                        y = -1f;
                        heatColor = Color.valueOf("f9350f");
                        cooldownTime = 30f;

                        bullet = new MissileBulletType(8f, 18f, "missile-large"){
                            {
                                damage = 18f;
                                rangeOverride = hurricaneLockRange();
                                width = 12f;
                                height = 20f;
                                lifetime = 35f;
                                hitSize = 6f;
                                homingPower = 0f;
                                weaveMag = 0f;
                                weaveScale = 0f;
                                hitColor = backColor = trailColor = Color.valueOf("feb380");
                                frontColor = Color.white;
                                trailWidth = 4f;
                                trailLength = 9;
                                hitEffect = despawnEffect = Fx.hitBulletColor;
                                shootEffect = Fx.shootSmall;
                                smokeEffect = Fx.shootSmallSmoke;

                                collides = false;
                                collidesTiles = false;
                                collidesAir = true;
                                collidesGround = true;
                                hittable = false;
                                absorbable = false;
                                reflectable = false;
                                keepVelocity = false;
                                despawnHit = false;

                                splashDamageRadius = 0f;
                                splashDamage = 0f;
                                fragBullets = 0;
                            }

                            @Override
                            public void update(Bullet b){
                                Teamc target = b.data instanceof Teamc t ? t : null;

                                if(target instanceof Healthc h && !h.isValid()) target = null;
                                if(target != null && target.team() == b.team) target = null;
                                if(target == null){
                                    b.remove();
                                    return;
                                }

                                float tx = target.getX(), ty = target.getY();
                                b.aimX = tx;
                                b.aimY = ty;
                                b.vel.setAngle(Angles.moveToward(b.rotation(), b.angleTo(tx, ty), 35f * Time.delta));
                                b.vel.setLength(speed);
                                b.rotation(b.vel.angle());

                                float hitRange = 4f;
                                if(target instanceof Sized s){
                                    hitRange += s.hitSize() / 2f;
                                }

                                if(Mathf.within(b.x, b.y, tx, ty, hitRange)){
                                    hit(b, tx, ty);
                                    if(target instanceof Unit u){
                                        u.damage(b.damage);
                                    }else if(target instanceof Building build && build.team != b.team){
                                        build.damage(b.damage * buildingDamageMultiplier);
                                    }
                                    b.remove();
                                }
                            }

                            @Override
                            public void createSplashDamage(Bullet b, float x, float y){
                                //No area damage.
                            }
                        };
                    }
                });
            }

            @Override
            public void update(Unit unit){
                super.update(unit);
                updateHurricaneLock(unit);

                Teamc target = hurricaneTarget(unit);
                if(target != null){
                    unit.rotation(Angles.moveToward(unit.rotation(), unit.angleTo(target), unit.type.rotateSpeed * Time.delta * unit.speedMultiplier()));
                }
            }

            @Override
            public void drawOutline(Unit unit){
                Draw.reset();

                if(Core.atlas.isFound(outlineRegion)){
                    applyColor(unit);
                    applyOutlineColor(unit);
                    drawRegionExplicit(outlineRegion, unit.x, unit.y, unit.rotation - 90f);
                    Draw.reset();
                }
            }

            @Override
            public void drawBody(Unit unit){
                applyColor(unit);

                if(unit instanceof UnderwaterMovec){
                    Draw.alpha(1f);
                    Draw.mixcol(unit.floorOn().mapColor.write(Tmp.c1).mul(0.9f), 1f);
                }

                drawRegionExplicit(region, unit.x, unit.y, unit.rotation - 90f);
                Draw.reset();
            }

            @Override
            public void drawCell(Unit unit){
                applyColor(unit);
                Draw.color(cellColor(unit));
                drawRegionExplicit(cellRegion, unit.x, unit.y, unit.rotation - 90f);
                Draw.reset();
            }

            @Override
            public <T extends Unit & Tankc> void drawTank(T unit){
                applyColor(unit);
                drawRegionExplicit(treadRegion, unit.x, unit.y, unit.rotation - 90f);

                if(treadRegion.found()){
                    int frame = (int)(unit.treadTime()) % treadFrames;
                    for(int i = 0; i < treadRects.length; i++){
                        var region = treadRegions[i][frame];
                        var treadRect = treadRects[i];
                        float xOffset = -(treadRect.x + treadRect.width / 2f);
                        float yOffset = -(treadRect.y + treadRect.height / 2f);

                        for(int side : Mathf.signs){
                            Tmp.v1.set(xOffset * side, yOffset).rotate(unit.rotation - 90f);
                            Draw.rect(region, unit.x + Tmp.v1.x * scaledTankVisualScale / 4f, unit.y + Tmp.v1.y * scaledTankVisualScale / 4f, treadRect.width * scaledTankVisualScale / 4f, region.height * region.scale * scaledTankVisualScale / 4f, unit.rotation - 90f);
                        }
                    }
                }
            }

            @Override
            public void drawWeaponOutlines(Unit unit){
                float prevX = Draw.xscl, prevY = Draw.yscl;
                Draw.scl(prevX * scaledTankVisualScale, prevY * scaledTankVisualScale);
                super.drawWeaponOutlines(unit);
                Draw.scl(prevX, prevY);
            }

            @Override
            public void drawWeapons(Unit unit){
                float prevX = Draw.xscl, prevY = Draw.yscl;
                Draw.scl(prevX * scaledTankVisualScale, prevY * scaledTankVisualScale);
                super.drawWeapons(unit);
                Draw.scl(prevX, prevY);
            }

            @Override
            public void drawShadow(Unit unit){
                drawShadowExplicit(shadowRegion, unit, shadowElevation, shadowElevationScl);
            }

            @Override
            public void draw(Unit unit){
                super.draw(unit);
            }

            @Override
            public void killed(Unit unit){
                clearHurricaneLockData(unit);
            }
        };

        vanquish = new TankUnitType("vanquish"){{
            hitSize = 28f;
            treadPullOffset = 4;
            speed = 4.725f;
            health = 11000;
            armor = 20f;
            itemCapacity = 0;
            crushDamage = 13f / 5f;
            floorMultiplier = 0.5f;
            drownTimeMultiplier = 1.25f;
            immunities.addAll(StatusEffects.burning, StatusEffects.melting);
            crushFragile = true;
            treadRects = new Rect[]{new Rect(22 - 154f/2f, 16 - 154f/2f, 28, 130)};

            tankMoveVolume *= 1.25f;
            tankMoveSound = Sounds.tankMoveHeavy;

            weapons.add(new Weapon("vanquish-weapon"){{
                shootSound = Sounds.shootTank;
                layerOffset = 0.0001f;
                reload = 70f;
                shootY = 71f / 4f;
                shake = 5f;
                recoil = 4f;
                rotate = true;
                rotateSpeed = 3f; // 180 deg/sec
                mirror = false;
                x = 0f;
                y = 0;
                shadow = 28f;
                heatColor = Color.valueOf("f9350f");
                cooldownTime = 80f;

                bullet = new BasicBulletType(8f, 190){{
                    sprite = "missile-large";
                    width = 9.5f;
                    height = 13f;
                    lifetime = 18f;
                    hitSize = 6f;
                    shootEffect = Fx.shootTitan;
                    smokeEffect = Fx.shootSmokeTitan;
                    pierceCap = 2;
                    pierce = true;
                    pierceBuilding = true;
                    hitColor = backColor = trailColor = Color.valueOf("feb380");
                    frontColor = Color.white;
                    trailWidth = 3.1f;
                    trailLength = 8;
                    hitEffect = despawnEffect = Fx.blastExplosion;
                    splashDamageRadius = 20f;
                    splashDamage = 50f;

                    fragOnHit = false;
                    fragRandomSpread = 0f;
                    fragSpread = 10f;
                    fragBullets = 5;
                    fragVelocityMin = 1f;
                    despawnSound = Sounds.explosionDull;

                    fragBullet = new BasicBulletType(8f, 35){{
                        sprite = "missile-large";
                        width = 8f;
                        height = 12f;
                        lifetime = 15f;
                        hitSize = 4f;
                        hitColor = backColor = trailColor = Color.valueOf("feb380");
                        frontColor = Color.white;
                        trailWidth = 2.8f;
                        trailLength = 6;
                        hitEffect = despawnEffect = Fx.blastExplosion;
                        splashDamageRadius = 10f;
                        splashDamage = 20f;
                    }};
                }};
            }});

            int i = 0;
            for(float f : new float[]{34f / 4f, -36f / 4f}){
                int fi = i ++;
                weapons.add(new Weapon("vanquish-point-weapon"){{
                    reload = 35f + fi * 5;
                    x = 48f / 4f;
                    y = f;
                    shootY = 5.5f;
                    recoil = 2f;
                    rotate = true;
                    rotateSpeed = 3f; // 180 deg/sec
                    shootSound = Sounds.shootStell;

                    bullet = new BasicBulletType(4.5f, 25){{
                        width = 6.5f;
                        height = 11f;
                        shootEffect = Fx.sparkShoot;
                        smokeEffect = Fx.shootBigSmoke;
                        hitColor = backColor = trailColor = Color.valueOf("feb380");
                        frontColor = Color.white;
                        trailWidth = 1.5f;
                        trailLength = 4;
                        hitEffect = despawnEffect = Fx.hitBulletColor;
                    }};
                }});
            }
        }};

        conquer = new TankUnitType("conquer"){{
            hitSize = 46f;
            treadPullOffset = 1;
            speed = 3.6f;
            health = 22000;
            armor = 26f;
            crushDamage = 25f / 5f;
            rotateSpeed = 3f; // 180 deg/sec
            floorMultiplier = 0.3f;
            immunities.addAll(StatusEffects.burning, StatusEffects.melting);

            tankMoveVolume *= 1.5f;
            tankMoveSound = Sounds.tankMoveHeavy;
            crushFragile = true;

            float xo = 231f/2f, yo = 231f/2f;
            treadRects = new Rect[]{new Rect(27 - xo, 152 - yo, 56, 73), new Rect(24 - xo, 51 - 9 - yo, 29, 17), new Rect(59 - xo, 18 - 9 - yo, 39, 19)};

            weapons.add(new Weapon("conquer-weapon"){{
                shootSound = Sounds.shootConquer;
                layerOffset = 0.1f;
                reload = 100f;
                shootY = 32.5f;
                shake = 5f;
                recoil = 5f;
                rotate = true;
                rotateSpeed = 3f; // 180 deg/sec
                mirror = false;
                x = 0f;
                y = -2f;
                shadow = 50f;
                heatColor = Color.valueOf("f9350f");
                shootWarmupSpeed = 0.06f;
                cooldownTime = 110f;
                heatColor = Color.valueOf("f9350f");
                minWarmup = 0.9f;

                parts.addAll(
                new RegionPart("-glow"){{
                    color = Color.red;
                    blending = Blending.additive;
                    outline = mirror = false;
                }},
                new RegionPart("-sides"){{
                    progress = PartProgress.warmup;
                    mirror = true;
                    under = true;
                    moveX = 0.75f;
                    moveY = 0.75f;
                    moveRot = 82f;
                    x = 37 / 4f;
                    y = 8 / 4f;
                }},
                new RegionPart("-sinks"){{
                    progress = PartProgress.warmup;
                    mirror = true;
                    under = true;
                    heatColor = new Color(1f, 0.1f, 0.1f);
                    moveX = 17f / 4f;
                    moveY = -15f / 4f;
                    x = 32 / 4f;
                    y = -34 / 4f;
                }},
                new RegionPart("-sinks-heat"){{
                    blending = Blending.additive;
                    progress = PartProgress.warmup;
                    mirror = true;
                    outline = false;
                    colorTo = new Color(1f, 0f, 0f, 0.5f);
                    color = colorTo.cpy().a(0f);
                    moveX = 17f / 4f;
                    moveY = -15f / 4f;
                    x = 32 / 4f;
                    y = -34 / 4f;
                }}
                );

                for(int i = 1; i <= 3; i++){
                    int fi = i;
                    parts.add(new RegionPart("-blade"){{
                        progress = PartProgress.warmup.delay((3 - fi) * 0.3f).blend(PartProgress.reload, 0.3f);
                        heatProgress = PartProgress.heat.add(0.3f).min(PartProgress.warmup);
                        heatColor = new Color(1f, 0.1f, 0.1f);
                        mirror = true;
                        under = true;
                        moveRot = -40f * fi;
                        moveX = 3f;
                        layerOffset = -0.002f;

                        x = 11 / 4f;
                    }});
                }

                bullet = new BasicBulletType(8f, 360f){{
                    sprite = "missile-large";
                    width = 12f;
                    height = 20f;
                    lifetime = 35f;
                    hitSize = 6f;

                    smokeEffect = Fx.shootSmokeTitan;
                    pierceCap = 3;
                    pierce = true;
                    pierceBuilding = true;
                    hitColor = backColor = trailColor = Color.valueOf("feb380");
                    frontColor = Color.white;
                    trailWidth = 4f;
                    trailLength = 9;
                    hitEffect = despawnEffect = Fx.massiveExplosion;

                    shootEffect = new ExplosionEffect(){{
                        lifetime = 40f;
                        waveStroke = 4f;
                        waveColor = sparkColor = trailColor;
                        waveRad = 15f;
                        smokeSize = 5f;
                        smokes = 8;
                        smokeSizeBase = 0f;
                        smokeColor = trailColor;
                        sparks = 8;
                        sparkRad = 40f;
                        sparkLen = 4f;
                        sparkStroke = 3f;
                    }};

                    int count = 6;
                    for(int j = 0; j < count; j++){
                        int s = j;
                        for(int i : Mathf.signs){
                            float fin = 0.05f + (j + 1) / (float)count;
                            float spd = speed;
                            float life = lifetime / Mathf.lerp(fin, 1f, 0.5f);
                            spawnBullets.add(new BasicBulletType(spd * fin, 60){{
                                drag = 0.002f;
                                width = 12f;
                                height = 11f;
                                lifetime = life + 5f;
                                weaveRandom = false;
                                hitSize = 5f;
                                pierceCap = 2;
                                pierce = true;
                                pierceBuilding = true;
                                hitColor = backColor = trailColor = Color.valueOf("feb380");
                                frontColor = Color.white;
                                trailWidth = 2.5f;
                                trailLength = 7;
                                weaveScale = (3f + s/2f) / 1.2f;
                                weaveMag = i * (4f - fin * 2f);

                                splashDamage = 65f;
                                splashDamageRadius = 30f;
                                despawnEffect = new ExplosionEffect(){{
                                    lifetime = 50f;
                                    waveStroke = 4f;
                                    waveColor = sparkColor = trailColor;
                                    waveRad = 30f;
                                    smokeSize = 7f;
                                    smokes = 6;
                                    smokeSizeBase = 0f;
                                    smokeColor = trailColor;
                                    sparks = 5;
                                    sparkRad = 30f;
                                    sparkLen = 3f;
                                    sparkStroke = 1.5f;
                                }};
                            }});
                        }
                    }
                }};
            }});

            parts.add(new RegionPart("-glow"){{
                color = Color.red;
                blending = Blending.additive;
                layer = -1f;
                outline = false;
            }});
        }};

        //endregion
        //region erekir - mech

        merui = new ErekirUnitType("merui"){{
            speed = 5.4f;
            drag = 0.11f;
            hitSize = 9f;
            rotateSpeed = 3f; // 180 deg/sec
            health = 680;
            armor = 4f;
            legStraightness = 0.3f;
            stepShake = 0f;
            stepSound = Sounds.walkerStepTiny;
            stepSoundVolume = 0.4f;

            legCount = 6;
            legLength = 8f;
            lockLegBase = true;
            legContinuousMove = true;
            legExtension = -2f;
            legBaseOffset = 3f;
            legMaxLength = 1.1f;
            legMinLength = 0.2f;
            legLengthScl = 0.96f;
            legForwardScl = 1.1f;
            legGroupSize = 3;
            rippleScale = 0.2f;

            legMoveSpace = 1f;
            allowLegStep = true;
            hovering = true;
            legPhysicsLayer = false;

            shadowElevation = 0.1f;
            groundLayer = Layer.legUnit - 1f;
            targetAir = false;
            researchCostMultiplier = 0f;

            weapons.add(new Weapon("merui-weapon"){{
                shootSound = Sounds.shootMerui;
                mirror = false;
                showStatSprite = false;
                x = 0f;
                y = 1f;
                shootY = 4f;
                reload = 63f;
                cooldownTime = 42f;
                heatColor = Pal.turretHeat;

                bullet = new ArtilleryBulletType(3f, 40){{
                    shootEffect = new MultiEffect(Fx.shootSmallColor, new Effect(9, e -> {
                        color(Color.white, e.color, e.fin());
                        stroke(0.7f + e.fout());
                        Lines.square(e.x, e.y, e.fin() * 5f, e.rotation + 45f);

                        Drawf.light(e.x, e.y, 23f, e.color, e.fout() * 0.7f);
                    }));

                    collidesTiles = true;
                    backColor = hitColor = Pal.techBlue;
                    frontColor = Color.white;

                    knockback = 0.8f;
                    lifetime = 46f;
                    width = height = 9f;
                    splashDamageRadius = 19f;
                    splashDamage = 30f;

                    trailLength = 27;
                    trailWidth = 2.5f;
                    trailEffect = Fx.none;
                    trailColor = backColor;

                    trailInterp = Interp.slope;

                    shrinkX = 0.6f;
                    shrinkY = 0.2f;

                    hitEffect = despawnEffect = new MultiEffect(Fx.hitSquaresColor, new WaveEffect(){{
                        colorFrom = colorTo = Pal.techBlue;
                        sizeTo = splashDamageRadius + 2f;
                        lifetime = 9f;
                        strokeFrom = 2f;
                    }});
                }};
            }});

        }};

        cleroi = new ErekirUnitType("cleroi"){{
            speed = 4.5f;
            drag = 0.1f;
            hitSize = 14f;
            rotateSpeed = 3f; // 180 deg/sec
            health = 1100;
            armor = 5f;
            stepShake = 0f;

            stepSound = Sounds.walkerStepSmall;

            legCount = 4;
            legLength = 14f;
            lockLegBase = true;
            legContinuousMove = true;
            legExtension = -3f;
            legBaseOffset = 5f;
            legMaxLength = 1.1f;
            legMinLength = 0.2f;
            legLengthScl = 0.95f;
            legForwardScl = 0.7f;

            legMoveSpace = 1f;
            hovering = true;

            shadowElevation = 0.2f;
            groundLayer = Layer.legUnit - 1f;

            for(int i = 0; i < 5; i++){
                int fi = i;
                parts.add(new RegionPart("-spine"){{
                    y = 21f / 4f - 45f / 4f * fi / 4f;
                    moveX = 21f / 4f + Mathf.slope(fi / 4f) * 1.25f;
                    moveRot = 10f - fi * 14f;
                    float fin = fi  / 4f;
                    progress = PartProgress.reload.inv().mul(1.3f).add(0.1f).sustain(fin * 0.34f, 0.14f, 0.14f);
                    layerOffset = -0.001f;
                    mirror = true;
                }});
            }

            weapons.add(new Weapon("cleroi-weapon"){{
                shootSound = Sounds.shootCleroi;
                x = 14f / 4f;
                y = 33f / 4f;
                reload = 33f;
                layerOffset = -0.002f;
                alternate = false;
                heatColor = Color.red;
                cooldownTime = 25f;
                smoothReloadSpeed = 0.15f;
                recoil = 2f;

                bullet = new BasicBulletType(3.5f, 30){{
                    backColor = trailColor = hitColor = Pal.techBlue;
                    frontColor = Color.white;
                    width = 7.5f;
                    height = 10f;
                    lifetime = 40f;
                    trailWidth = 2f;
                    trailLength = 4;
                    shake = 1f;

                    trailEffect = Fx.missileTrail;
                    trailParam = 1.8f;
                    trailInterval = 6f;

                    splashDamageRadius = 30f;
                    splashDamage = 43f;

                    despawnSound = Sounds.explosionCleroi;

                    hitEffect = despawnEffect = new MultiEffect(Fx.hitBulletColor, new WaveEffect(){{
                        colorFrom = colorTo = Pal.techBlue;
                        sizeTo = splashDamageRadius + 3f;
                        lifetime = 9f;
                        strokeFrom = 3f;
                    }});

                    shootEffect = new MultiEffect(Fx.shootBigColor, new Effect(9, e -> {
                        color(Color.white, e.color, e.fin());
                        stroke(0.7f + e.fout());
                        Lines.square(e.x, e.y, e.fin() * 5f, e.rotation + 45f);

                        Drawf.light(e.x, e.y, 23f, e.color, e.fout() * 0.7f);
                    }));
                    smokeEffect = Fx.shootSmokeSquare;
                    ammoMultiplier = 2;
                }};
            }});

            weapons.add(new PointDefenseWeapon("cleroi-point-defense"){{
                x = 16f / 4f;
                y = -20f / 4f;
                reload = 9f;

                targetInterval = 9f;
                targetSwitchInterval = 12f;
                recoil = 0.5f;

                bullet = new BulletType(){{
                    shootSound = Sounds.shootLaser;
                    shootEffect = Fx.sparkShoot;
                    hitEffect = Fx.pointHit;
                    maxRange = 100f;
                    damage = 38f;
                }};
            }});
        }};

        anthicus = new ErekirUnitType("anthicus"){{
            speed = 4.875f;
            drag = 0.1f;
            hitSize = 21f;
            rotateSpeed = 3f; // 180 deg/sec
            health = 2900;
            armor = 7f;
            population = 3;
            fogRadius = 40f;
            stepShake = 0f;

            stepSound = Sounds.walkerStepSmall;
            stepSoundPitch = 0.78f;

            legCount = 6;
            legLength = 18f;
            legGroupSize = 3;
            lockLegBase = true;
            legContinuousMove = true;
            legExtension = -3f;
            legBaseOffset = 7f;
            legMaxLength = 1.1f;
            legMinLength = 0.2f;
            legLengthScl = 0.95f;
            legForwardScl = 0.9f;

            legMoveSpace = 1f;
            hovering = true;

            shadowElevation = 0.2f;
            groundLayer = Layer.legUnit - 1f;

            for(int j = 0; j < 3; j++){
                int i = j;
                parts.add(new RegionPart("-blade"){{
                    layerOffset = -0.01f;
                    heatLayerOffset = 0.005f;
                    x = 2f;
                    moveX = 6f + i * 1.9f;
                    moveY = 8f + -4f * i;
                    moveRot = 40f - i * 25f;
                    mirror = true;
                    progress = PartProgress.warmup.delay(i * 0.2f);
                    heatProgress = p -> Mathf.absin(Time.time + i * 14f, 7f, 1f);

                    heatColor = Pal.techBlue;
                }});
            }

            weapons.add(new Weapon("anthicus-weapon"){{
                shootSound = Sounds.shootMissileLarge;
                shootSoundVolume = 0.5f;
                x = 29f / 4f;
                y = -11f / 4f;
                shootY = 1.5f;
                showStatSprite = false;
                reload = 130f;
                layerOffset = 0.01f;
                heatColor = Color.red;
                cooldownTime = 60f;
                smoothReloadSpeed = 0.15f;
                shootWarmupSpeed = 0.05f;
                minWarmup = 0.9f;
                rotationLimit = 70f;
                rotateSpeed = 3f; // 180 deg/sec
                inaccuracy = 20f;
                shootStatus = StatusEffects.slow;
                alwaysShootWhenMoving = true;

                rotate = true;

                shoot = new ShootPattern(){{
                    shots = 2;
                    shotDelay = 6f;
                }};

                parts.add(new RegionPart("-blade"){{
                    mirror = true;
                    moveRot = -25f;
                    under = true;
                    moves.add(new PartMove(PartProgress.reload, 1f, 0f, 0f));

                    heatColor = Color.red;
                    cooldownTime = 60f;
                }});

                parts.add(new RegionPart("-blade"){{
                    mirror = true;
                    moveRot = -50f;
                    moveY = -2f;
                    moves.add(new PartMove(PartProgress.reload.shorten(0.5f), 1f, 0f, -15f));
                    under = true;

                    heatColor = Color.red;
                    cooldownTime = 60f;
                }});

                bullet = new BulletType(){{
                    shootEffect = new MultiEffect(Fx.shootBigColor, new Effect(9, e -> {
                        color(Color.white, e.color, e.fin());
                        stroke(0.7f + e.fout());
                        Lines.square(e.x, e.y, e.fin() * 5f, e.rotation + 45f);

                        Drawf.light(e.x, e.y, 23f, e.color, e.fout() * 0.7f);
                    }), new WaveEffect(){{
                        colorFrom = colorTo = Pal.techBlue;
                        sizeTo = 15f;
                        lifetime = 12f;
                        strokeFrom = 3f;
                    }});

                    smokeEffect = Fx.shootBigSmoke2;
                    shake = 2f;
                    speed = 0f;
                    keepVelocity = false;
                    inaccuracy = 2f;

                    spawnUnit = new MissileUnitType("anthicus-missile"){{
                        trailColor = engineColor = Pal.techBlue;
                        engineSize = 1.75f;
                        engineLayer = Layer.effect;
                        speed = 3.7f;
                        maxRange = 6f;
                        lifetime = 60f * 1.5f;
                        outlineColor = Pal.darkOutline;
                        health = 55;
                        lowAltitude = true;

                        parts.add(new FlarePart(){{
                            progress = PartProgress.life.slope().curve(Interp.pow2In);
                            radius = 0f;
                            radiusTo = 35f;
                            stroke = 3f;
                            rotation = 45f;
                            y = -5f;
                            followRotation = true;
                        }});

                        weapons.add(new Weapon(){{
                            shootSound = Sounds.none;
                            shootCone = 360f;
                            mirror = false;
                            reload = 1f;
                            shootOnDeath = true;
                            bullet = new ExplosionBulletType(140f, 25f){{
                                shootEffect = new MultiEffect(Fx.massiveExplosion, new WrapEffect(Fx.dynamicSpikes, Pal.techBlue, 24f), new WaveEffect(){{
                                    colorFrom = colorTo = Pal.techBlue;
                                    sizeTo = 40f;
                                    lifetime = 12f;
                                    strokeFrom = 4f;
                                }});
                            }};
                        }});
                    }};
                }};
            }});
        }};

        tecta = new ErekirUnitType("tecta"){{
            drag = 0.1f;
            speed = 4.5f;
            hitSize = 30f;
            health = 6500;
            armor = 5f;

            lockLegBase = true;
            legContinuousMove = true;
            legGroupSize = 3;
            legStraightness = 0.4f;
            baseLegStraightness = 0.5f;
            legMaxLength = 1.3f;
            researchCostMultiplier = 0f;

            stepSound = Sounds.walkerStep;
            stepSoundVolume = 1f;
            stepSoundPitch = 1f;

            rotateSpeed = 3f; // 180 deg/sec

            legCount = 6;
            legLength = 15f;
            legForwardScl = 0.45f;
            legMoveSpace = 1.4f;
            rippleScale = 2f;
            stepShake = 0.5f;
            legExtension = -5f;
            legBaseOffset = 5f;

            ammoType = new PowerAmmoType(2000);

            legSplashDamage = 32;
            legSplashRange = 30;
            drownTimeMultiplier = 0.5f;

            hovering = true;
            shadowElevation = 0.4f;
            groundLayer = Layer.legUnit;

            weapons.add(new Weapon("tecta-weapon"){{
                shootSound = Sounds.shootMalign;
                mirror = true;
                top = false;

                x = 62/4f;
                y = 1f;
                shootY = 47 / 4f;
                recoil = 3f;
                reload = 40f;
                shake = 3f;
                cooldownTime = 40f;

                shoot.shots = 3;
                inaccuracy = 3f;
                velocityRnd = 0.33f;
                heatColor = Color.red;

                bullet = new MissileBulletType(4.2f, 60){{
                    homingPower = 0.2f;
                    weaveMag = 4;
                    weaveScale = 4;
                    lifetime = 55f;
                    shootEffect = Fx.shootBig2;
                    smokeEffect = Fx.shootSmokeTitan;
                    splashDamage = 70f;
                    splashDamageRadius = 30f;
                    frontColor = Color.white;
                    hitSound = Sounds.none;
                    width = height = 10f;

                    lightColor = trailColor = backColor = Pal.techBlue;
                    lightRadius = 40f;
                    lightOpacity = 0.7f;

                    trailWidth = 2.8f;
                    trailLength = 20;
                    trailChance = -1f;
                    despawnSound = Sounds.explosionDull;

                    despawnEffect = Fx.none;
                    hitEffect = new ExplosionEffect(){{
                        lifetime = 20f;
                        waveStroke = 2f;
                        waveColor = sparkColor = trailColor;
                        waveRad = 12f;
                        smokeSize = 0f;
                        smokeSizeBase = 0f;
                        sparks = 10;
                        sparkRad = 35f;
                        sparkLen = 4f;
                        sparkStroke = 1.5f;
                    }};
                }};
            }});
        }};

        collaris = new ErekirUnitType("collaris"){{
            drag = 0.1f;
            speed = 8.25f;
            hitSize = 44f;
            health = 18000;
            armor = 9f;
            rotateSpeed = 3f; // 180 deg/sec
            lockLegBase = true;
            legContinuousMove = true;
            legStraightness = 0.6f;
            baseLegStraightness = 0.5f;

            stepSound = Sounds.walkerStep;
            stepSoundVolume = 1.1f;
            stepSoundPitch = 0.9f;

            legCount = 8;
            legLength = 30f;
            legForwardScl = 2.1f;
            legMoveSpace = 1.05f;
            rippleScale = 1.2f;
            stepShake = 0.5f;
            legGroupSize = 2;
            legExtension = -6f;
            legBaseOffset = 19f;
            legStraightLength = 0.9f;
            legMaxLength = 1.2f;

            ammoType = new PowerAmmoType(2000);

            legSplashDamage = 32;
            legSplashRange = 32;
            drownTimeMultiplier = 0.5f;

            hovering = true;
            shadowElevation = 0.4f;
            groundLayer = Layer.legUnit;

            targetAir = false;
            alwaysShootWhenMoving = true;

            weapons.add(new Weapon("collaris-weapon"){{
                shootSound = Sounds.shootCollaris;
                mirror = true;
                rotationLimit = 30f;
                rotateSpeed = 3f; // 180 deg/sec
                rotate = true;

                x = 48 / 4f;
                y = -28f / 4f;
                shootY = 64f / 4f;
                recoil = 4f;
                reload = 130f;
                cooldownTime = reload * 1.2f;
                shake = 7f;
                layerOffset = 0.02f;
                shadow = 10f;

                shootStatus = StatusEffects.slow;
                shootStatusDuration = reload + 1f;

                shoot.shots = 1;
                heatColor = Color.red;

                for(int i = 0; i < 5; i++){
                    int fi = i;
                    parts.add(new RegionPart("-blade"){{
                        under = true;
                        layerOffset = -0.001f;
                        heatColor = Pal.techBlue;
                        heatProgress = PartProgress.heat.add(0.2f).min(PartProgress.warmup);
                        progress = PartProgress.warmup.blend(PartProgress.reload, 0.1f);
                        x = 13.5f / 4f;
                        y = 10f / 4f - fi * 2f;
                        moveY = 1f - fi * 1f;
                        moveX = fi * 0.3f;
                        moveRot = -45f - fi * 17f;

                        moves.add(new PartMove(PartProgress.reload.inv().mul(1.8f).inv().curve(fi / 5f, 0.2f), 0f, 0f, 36f));
                    }});
                }

                bullet = new ArtilleryBulletType(5.5f, 260){{
                    collidesTiles = collides = true;
                    lifetime = 60f;
                    shootEffect = Fx.shootBigColor;
                    smokeEffect = Fx.shootSmokeSquareBig;
                    frontColor = Color.white;
                    trailEffect = new MultiEffect(Fx.artilleryTrail, Fx.artilleryTrailSmoke);
                    hitSound = Sounds.none;
                    width = 18f;
                    height = 24f;
                    rangeOverride = 385f;

                    lightColor = trailColor = hitColor = backColor = Pal.techBlue;
                    lightRadius = 40f;
                    lightOpacity = 0.7f;

                    trailWidth = 4.5f;
                    trailLength = 19;
                    trailChance = -1f;

                    despawnEffect = Fx.none;
                    despawnSound = Sounds.explosionDull;

                    hitEffect = despawnEffect = new ExplosionEffect(){{
                        lifetime = 50f;
                        waveStroke = 5f;
                        waveColor = sparkColor = trailColor;
                        waveRad = 45f;
                        smokeSize = 0f;
                        smokeSizeBase = 0f;
                        sparks = 10;
                        sparkRad = 25f;
                        sparkLen = 8f;
                        sparkStroke = 3f;
                    }};

                    splashDamage = 120f;
                    splashDamageRadius = 36f;

                    fragBullets = 15;
                    fragVelocityMin = 0.5f;
                    fragRandomSpread = 130f;
                    fragLifeMin = 0.3f;
                    despawnShake = 5f;

                    fragBullet = new BasicBulletType(5.5f, 37){{
                        pierceCap = 2;
                        pierceBuilding = true;

                        homingPower = 0.09f;
                        homingRange = 150f;

                        lifetime = 40f;
                        shootEffect = Fx.shootBigColor;
                        smokeEffect = Fx.shootSmokeSquareBig;
                        frontColor = Color.white;
                        hitSound = Sounds.none;
                        width = 12f;
                        height = 20f;

                        lightColor = trailColor = hitColor = backColor = Pal.techBlue;
                        lightRadius = 40f;
                        lightOpacity = 0.7f;

                        trailWidth = 2.2f;
                        trailLength = 7;
                        trailChance = -1f;

                        collidesAir = false;

                        despawnEffect = Fx.none;
                        splashDamage = 35f;
                        splashDamageRadius = 30f;

                        hitEffect = despawnEffect = new MultiEffect(new ExplosionEffect(){{
                            lifetime = 30f;
                            waveStroke = 2f;
                            waveColor = sparkColor = trailColor;
                            waveRad = 5f;
                            smokeSize = 0f;
                            smokeSizeBase = 0f;
                            sparks = 5;
                            sparkRad = 20f;
                            sparkLen = 6f;
                            sparkStroke = 2f;
                        }}, Fx.blastExplosion);
                    }};
                }};
            }});
        }};

        //endregion
        //region erekir - flying

        elude = new ErekirUnitType("elude"){{
            hovering = true;
            canDrown = false;
            shadowElevation = 0.1f;

            drag = 0.07f;
            speed = 13.5f;
            rotateSpeed = 3f; // 180 deg/sec

            accel = 0.09f;
            health = 600f;
            armor = 1f;
            hitSize = 11f;
            engineOffset = 7f;
            engineSize = 2f;
            itemCapacity = 0;
            useEngineElevation = false;
            researchCostMultiplier = 0f;
            moveSound = Sounds.loopExtract;
            moveSoundVolume = 0.25f;
            moveSoundPitchMin = 0.7f;
            moveSoundPitchMax = 1.5f;

            abilities.add(new MoveEffectAbility(0f, -7f, Pal.sapBulletBack, Fx.missileTrailShort, 4f){{
                teamColor = true;
            }});

            for(float f : new float[]{-3f, 3f}){
                parts.add(new HoverPart(){{
                    x = 3.9f;
                    y = f;
                    mirror = true;
                    radius = 6f;
                    phase = 90f;
                    stroke = 2f;
                    layerOffset = -0.001f;
                    color = Color.valueOf("bf92f9");
                }});
            }

            weapons.add(new Weapon("elude-weapon"){{
                shootSound = Sounds.shootElude;
                y = -2f;
                x = 4f;
                top = true;
                mirror = true;
                reload = 40f;
                baseRotation = -35f;
                shootCone = 360f;

                shoot = new ShootSpread(2, 11f);

                bullet = new BasicBulletType(5f, 16){{
                    homingPower = 0.19f;
                    homingDelay = 4f;
                    width = 7f;
                    height = 12f;
                    lifetime = 30f;
                    shootEffect = Fx.sparkShoot;
                    smokeEffect = Fx.shootBigSmoke;
                    hitColor = backColor = trailColor = Pal.suppress;
                    frontColor = Color.white;
                    trailWidth = 1.5f;
                    trailLength = 5;
                    hitEffect = despawnEffect = Fx.hitBulletColor;
                }};
            }});
        }};

        avert = new ErekirUnitType("avert"){
            @Override
            public void update(Unit unit){
                super.update(unit);
                updateRaven(unit);
            }

            @Override
            public void killed(Unit unit){
                clearRavenData(unit);
            }

            {
                lowAltitude = false;
                flying = true;
                drag = 0.08f;
                speed = 4.13f;
                rotateSpeed = 3f; // 180 deg/sec
                accel = 0.09f;
                health = 140f;
                armor = 1f;
                armorType = ArmorType.light;
                unitClasses = EnumSet.of(UnitClass.mechanical);
                population = 2;
                hitSize = 12f;
                engineSize = 0f;
                fogRadius = 25f;
                itemCapacity = 0;
                canAttack = false;
                targetAir = false;
                targetGround = false;
                followEnemyWhenUnarmed = true;
                energyCapacity = 200f;
                energyInit = 50f;
                stealthDetectionRange = 12f * tilesize;

                setEnginesMirror(
                new UnitEngine(35 / 4f, -38 / 4f, 3f, 315f),
                new UnitEngine(39 / 4f, -16 / 4f, 3f, 315f)
                );
            }
        };

        obviate = new ErekirUnitType("obviate"){{
            flying = true;
            drag = 0.08f;
            speed = 13.5f;
            rotateSpeed = 3f; // 180 deg/sec
            accel = 0.09f;
            health = 2300f;
            armor = 6f;
            population = 3;
            hitSize = 25f;
            engineSize = 4.3f;
            engineOffset = 54f / 4f;
            fogRadius = 25;
            itemCapacity = 0;
            lowAltitude = true;

            setEnginesMirror(
            new UnitEngine(38 / 4f, -46 / 4f, 3.1f, 315f)
            );

            parts.add(
            new RegionPart("-blade"){{
                moveRot = -10f;
                moveX = -1f;
                moves.add(new PartMove(PartProgress.reload, 2f, 1f, -5f));
                progress = PartProgress.warmup;
                mirror = true;

                children.add(new RegionPart("-side"){{
                    moveX = 2f;
                    moveY = -2f;
                    progress = PartProgress.warmup;
                    under = true;
                    mirror = true;
                    moves.add(new PartMove(PartProgress.reload, -2f, 2f, 0f));
                }});
            }});

            weapons.add(new Weapon(){{
                shootSound = Sounds.explosionObviate;
                x = 0f;
                y = -2f;
                shootY = 0f;
                reload = 140f;
                mirror = false;
                minWarmup = 0.95f;
                shake = 3f;
                cooldownTime = reload - 10f;

                bullet = new BasicBulletType(){{
                    shoot = new ShootHelix(){{
                        mag = 1f;
                        scl = 5f;
                    }};

                    shootEffect = new MultiEffect(Fx.shootTitan, new WaveEffect(){{
                        colorTo = Pal.sapBulletBack;
                        sizeTo = 26f;
                        lifetime = 14f;
                        strokeFrom = 4f;
                    }});
                    smokeEffect = Fx.shootSmokeTitan;
                    hitColor = Pal.sapBullet;
                    despawnSound = Sounds.explosionArtilleryShock;

                    sprite = "large-orb";
                    trailEffect = Fx.missileTrail;
                    trailInterval = 3f;
                    trailParam = 4f;
                    speed = 3f;
                    damage = 75f;
                    lifetime = 60f;
                    width = height = 15f;
                    backColor = Pal.sapBulletBack;
                    frontColor = Pal.sapBullet;
                    shrinkX = shrinkY = 0f;
                    trailColor = Pal.sapBulletBack;
                    trailLength = 12;
                    trailWidth = 2.2f;
                    despawnEffect = hitEffect = new ExplosionEffect(){{
                        waveColor = Pal.sapBullet;
                        smokeColor = Color.gray;
                        sparkColor = Pal.sap;
                        waveStroke = 4f;
                        waveRad = 40f;
                    }};

                    intervalBullet = new LightningBulletType(){{
                        damage = 16;
                        collidesAir = false;
                        ammoMultiplier = 1f;
                        lightningColor = Pal.sapBullet;
                        lightningLength = 3;
                        lightningLengthRand = 6;

                        //for visual stats only.
                        buildingDamageMultiplier = 0.25f;

                        lightningType = new BulletType(0.0001f, 0f){{
                            lifetime = Fx.lightning.lifetime;
                            hitEffect = Fx.hitLancer;
                            despawnEffect = Fx.none;
                            status = StatusEffects.shocked;
                            statusDuration = 10f;
                            hittable = false;
                            lightColor = Color.white;
                            buildingDamageMultiplier = 0.25f;
                        }};
                    }};

                    bulletInterval = 4f;

                    lightningColor = Pal.sapBullet;
                    lightningDamage = 17;
                    lightning = 8;
                    lightningLength = 2;
                    lightningLengthRand = 8;
                }};

            }});
        }};

        quell = new ErekirUnitType("quell"){{
            aiController = FlyingFollowAI::new;
            envDisabled = 0;

            lowAltitude = false;
            flying = true;
            drag = 0.06f;
            speed = 8.25f;
            rotateSpeed = 3f; // 180 deg/sec
            accel = 0.1f;
            health = 6000f;
            armor = 4f;
            hitSize = 36f;
            payloadCapacity = Mathf.sqr(3f) * tilePayload;
            researchCostMultiplier = 0f;
            targetAir = false;

            engineSize = 4.8f;
            engineOffset = 61 / 4f;
            range = 4.3f * 60f * 1.4f;

            loopSoundVolume = 0.85f;
            loopSound = Sounds.loopHover;

            abilities.add(new SuppressionFieldAbility(){{
                reload = 60f * 8f;
                orbRadius = 5.3f;
                y = 1f;
            }});

            weapons.add(new Weapon("quell-weapon"){{
                shootSound = Sounds.shootMissileSmall;
                x = 51 / 4f;
                y = 5 / 4f;
                rotate = true;
                rotateSpeed = 3f; // 180 deg/sec
                reload = 55f;
                layerOffset = -0.001f;
                recoil = 1f;
                rotationLimit = 60f;

                bullet = new BasicBulletType(4.3f, 70f, "missile-large"){{
                    shootEffect = Fx.shootBig;
                    smokeEffect = Fx.shootBigSmoke2;
                    shake = 1f;
                    lifetime = 60 * 0.496f;
                    rangeOverride = 361.2f;
                    followAimSpeed = 5f;

                    width = 12f;
                    height = 22f;
                    hitSize = 7f;
                    hitColor = backColor = trailColor = Pal.sapBulletBack;
                    trailWidth = 3f;
                    trailLength = 12;
                    hitEffect = despawnEffect = Fx.hitBulletColor;

                    keepVelocity = false;
                    collidesGround = true;
                    collidesAir = false;

                    //workaround to get the missile to behave like in spawnUnit while still spawning on death
                    fragRandomSpread = 0;
                    fragBullets = 1;
                    fragVelocityMin = 1f;
                    fragOffsetMax = 1f;

                    fragBullet = new BulletType(){{
                        speed = 0f;
                        keepVelocity = false;
                        collidesAir = false;
                        spawnUnit = new MissileUnitType("quell-missile"){{
                            targetAir = false;
                            speed = 4.3f;
                            maxRange = 6f;
                            lifetime = 60f * (1.4f - 0.496f);
                            outlineColor = Pal.darkOutline;
                            engineColor = trailColor = Pal.sapBulletBack;
                            engineLayer = Layer.effect;
                            health = 45;
                            loopSoundVolume = 0.1f;

                            weapons.add(new Weapon() {{
                                shootSound = Sounds.none;
                                shootCone = 360f;
                                mirror = false;
                                reload = 1f;
                                shootOnDeath = true;
                                bullet = new ExplosionBulletType(110f, 25f) {{
                                    shootEffect = Fx.massiveExplosion;
                                    collidesAir = false;
                                }};
                            }});
                        }};
                    }};
                }};
            }});

            setEnginesMirror(
            new UnitEngine(62 / 4f, -60 / 4f, 3.9f, 315f),
            new UnitEngine(72 / 4f, -29 / 4f, 3f, 315f)
            );
        }};

        disrupt = new ErekirUnitType("disrupt"){{
            aiController = FlyingFollowAI::new;
            envDisabled = 0;

            lowAltitude = false;
            flying = true;
            drag = 0.07f;
            speed = 7.5f;
            rotateSpeed = 3f; // 180 deg/sec
            accel = 0.1f;
            health = 12000f;
            armor = 9f;
            hitSize = 46f;
            payloadCapacity = Mathf.sqr(6f) * tilePayload;
            targetAir = false;

            engineSize = 6f;
            engineOffset = 25.25f;

            loopSound = Sounds.loopHover;

            float orbRad = 5f, partRad = 3f;
            int parts = 10;

            abilities.add(new SuppressionFieldAbility(){{
                reload = 60 * 15f;
                range = 320f;
                orbRadius = orbRad;
                particleSize = partRad;
                y = 10f;
                particles = parts;
            }});

            for(int i : Mathf.signs){
                abilities.add(new SuppressionFieldAbility(){{
                    orbRadius = orbRad;
                    particleSize = partRad;
                    y = -32f / 4f;
                    x = 43f * i / 4f;
                    particles = parts;
                    //visual only, the middle one does the actual suppressing
                    active = false;
                }});
            }

            weapons.add(new Weapon("disrupt-weapon"){{
                shootSound = Sounds.shootMissileLarge;
                shootSoundVolume = 0.6f;
                x = 78f / 4f;
                y = -10f / 4f;
                mirror = true;
                rotate = true;
                rotateSpeed = 3f; // 180 deg/sec
                reload = 70f;
                layerOffset = -20f;
                recoil = 1f;
                rotationLimit = 22f;
                minWarmup = 0.95f;
                shootWarmupSpeed = 0.1f;
                shootY = 2f;
                shootCone = 40f;
                shoot.shots = 3;
                shoot.shotDelay = 5f;
                inaccuracy = 28f;

                parts.add(new RegionPart("-blade"){{
                    heatProgress = PartProgress.warmup;
                    progress = PartProgress.warmup.blend(PartProgress.reload, 0.15f);
                    heatColor = Color.valueOf("9c50ff");
                    x = 5 / 4f;
                    y = 0f;
                    moveRot = -33f;
                    moveY = -1f;
                    moveX = -1f;
                    under = true;
                    mirror = true;
                }});

                bullet = new BulletType(){{
                    shootEffect = Fx.sparkShoot;
                    smokeEffect = Fx.shootSmokeTitan;
                    hitColor = Pal.suppress;
                    shake = 1f;
                    speed = 0f;
                    keepVelocity = false;
                    collidesAir = false;

                    spawnUnit = new MissileUnitType("disrupt-missile"){{
                        targetAir = false;
                        speed = 4.6f;
                        maxRange = 5f;
                        outlineColor = Pal.darkOutline;
                        health = 70;
                        homingDelay = 10f;
                        lowAltitude = true;
                        engineSize = 3f;
                        engineColor = trailColor = Pal.sapBulletBack;
                        engineLayer = Layer.effect;
                        deathExplosionEffect = Fx.none;
                        loopSoundVolume = 0.1f;

                        parts.add(new ShapePart(){{
                            layer = Layer.effect;
                            circle = true;
                            y = -0.25f;
                            radius = 1.5f;
                            color = Pal.suppress;
                            colorTo = Color.white;
                            progress = PartProgress.life.curve(Interp.pow5In);
                        }});

                        parts.add(new RegionPart("-fin"){{
                            mirror = true;
                            progress = PartProgress.life.mul(3f).curve(Interp.pow5In);
                            moveRot = 32f;
                            rotation = -6f;
                            moveY = 1.5f;
                            x = 3f / 4f;
                            y = -6f / 4f;
                        }});

                        weapons.add(new Weapon(){{
                            shootCone = 360f;
                            mirror = false;
                            reload = 1f;
                            shootOnDeath = true;
                            bullet = new ExplosionBulletType(140f, 25f){{
                                collidesAir = false;
                                suppressionRange = 140f;
                                shootEffect = new ExplosionEffect(){{
                                    lifetime = 50f;
                                    waveStroke = 5f;
                                    waveLife = 8f;
                                    waveColor = Color.white;
                                    sparkColor = smokeColor = Pal.suppress;
                                    waveRad = 40f;
                                    smokeSize = 4f;
                                    smokes = 7;
                                    smokeSizeBase = 0f;
                                    sparks = 10;
                                    sparkRad = 40f;
                                    sparkLen = 6f;
                                    sparkStroke = 2f;
                                }};
                            }};
                        }});
                    }};
                }};
            }});

            setEnginesMirror(
            new UnitEngine(95 / 4f, -56 / 4f, 5f, 330f),
            new UnitEngine(89 / 4f, -95 / 4f, 4f, 315f)
            );
        }};

        //endregion
        //region erekir - neoplasm

        renale = new NeoplasmUnitType("renale"){{
            health = 500;
            armor = 2;
            hitSize = 9f;
            omniMovement = false;
            rotateSpeed = 3f; // 180 deg/sec
            drownTimeMultiplier = 1.75f;
            segments = 3;
            drawBody = false;
            hidden = true;
            crushDamage = 0.5f;
            aiController = HugAI::new;
            targetAir = false;

            segmentScl = 3f;
            segmentPhase = 5f;
            segmentMag = 0.5f;
            speed = 9f;
        }};

        latum = new NeoplasmUnitType("latum"){{
            health = 20000;
            armor = 12;
            hitSize = 48f;
            omniMovement = false;
            rotateSpeed = 3f; // 180 deg/sec
            segments = 4;
            drawBody = false;
            hidden = true;
            crushDamage = 2f;
            aiController = HugAI::new;
            targetAir = false;

            segmentScl = 4f;
            segmentPhase = 5f;
            speed = 7.5f;

            abilities.add(new SpawnDeathAbility(renale, 5, 11f));
        }};

        //endregion
        //region erekir - core

        float coreFleeRange = 500f;

        evoke = new ErekirUnitType("evoke"){{
            coreUnitDock = true;
            controller = u -> new BuilderAI(true, coreFleeRange);
            isEnemy = false;
            envDisabled = 0;

            range = 60f;
            faceTarget = true;
            targetPriority = -2;
            lowAltitude = false;
            mineWalls = true;
            mineFloor = false;
            mineHardnessScaling = false;
            flying = true;
            mineSpeed = 6f;
            mineTier = 3;
            buildSpeed = 1.2f;
            drag = 0.08f;
            speed = 42f;
            rotateSpeed = 3f; // 180 deg/sec
            accel = 0.09f;
            itemCapacity = 60;
            health = 300f;
            armor = 1f;
            hitSize = 9f;
            engineSize = 0;
            payloadCapacity = 2f * 2f * tilesize * tilesize;
            pickupUnits = false;
            vulnerableWithPayloads = true;

            fogRadius = 0f;
            targetable = false;
            hittable = false;

            setEnginesMirror(
            new UnitEngine(21 / 4f, 19 / 4f, 2.2f, 45f),
            new UnitEngine(23 / 4f, -22 / 4f, 2.2f, 315f)
            );

            weapons.add(new RepairBeamWeapon(){{
                widthSinMag = 0.11f;
                reload = 20f;
                x = 0f;
                y = 6.5f;
                rotate = false;
                shootY = 0f;
                beamWidth = 0.7f;
                repairSpeed = 3.1f;
                fractionRepairSpeed = 0.06f;
                aimDst = 0f;
                shootCone = 15f;
                mirror = false;

                targetUnits = false;
                targetBuildings = true;
                autoTarget = false;
                controllable = true;
                laserColor = Pal.accent;
                healColor = Pal.accent;

                bullet = new BulletType(){{
                    maxRange = 60f;
                }};
            }});
        }};

        incite = new ErekirUnitType("incite"){{
            coreUnitDock = true;
            controller = u -> new BuilderAI(true, coreFleeRange);
            isEnemy = false;
            envDisabled = 0;

            range = 60f;
            targetPriority = -2;
            lowAltitude = false;
            faceTarget = true;
            mineWalls = true;
            mineFloor = false;
            mineHardnessScaling = false;
            flying = true;
            mineSpeed = 8f;
            mineTier = 3;
            buildSpeed = 1.4f;
            drag = 0.08f;
            speed = 52.5f;
            rotateSpeed = 3f; // 180 deg/sec
            accel = 0.09f;
            itemCapacity = 90;
            health = 500f;
            armor = 2f;
            hitSize = 11f;
            payloadCapacity = 2f * 2f * tilesize * tilesize;
            pickupUnits = false;
            vulnerableWithPayloads = true;

            fogRadius = 0f;
            targetable = false;
            hittable = false;

            engineOffset = 7.2f;
            engineSize = 3.1f;

            setEnginesMirror(
            new UnitEngine(25 / 4f, -1 / 4f, 2.4f, 300f)
            );

            weapons.add(new RepairBeamWeapon(){{
                widthSinMag = 0.11f;
                reload = 20f;
                x = 0f;
                y = 7.5f;
                rotate = false;
                shootY = 0f;
                beamWidth = 0.7f;
                aimDst = 0f;
                shootCone = 15f;
                mirror = false;

                repairSpeed = 3.3f;
                fractionRepairSpeed = 0.06f;

                targetUnits = false;
                targetBuildings = true;
                autoTarget = false;
                controllable = true;
                laserColor = Pal.accent;
                healColor = Pal.accent;

                bullet = new BulletType(){{
                    maxRange = 60f;
                }};
            }});

            drawBuildBeam = false;

            weapons.add(new BuildWeapon("build-weapon"){{
                rotate = true;
                rotateSpeed = 3f; // 180 deg/sec
                x = 14/4f;
                y = 15/4f;
                layerOffset = -0.001f;
                shootY = 3f;
            }});
        }};

        emanate = new ErekirUnitType("emanate"){{
            coreUnitDock = true;
            controller = u -> new BuilderAI(true, coreFleeRange);
            isEnemy = false;
            envDisabled = 0;

            range = 65f;
            faceTarget = true;
            targetPriority = -2;
            lowAltitude = false;
            mineWalls = true;
            mineFloor = false;
            mineHardnessScaling = false;
            flying = true;
            mineSpeed = 9f;
            mineTier = 3;
            buildSpeed = 1.5f;
            drag = 0.08f;
            speed = 56.25f;
            rotateSpeed = 3f; // 180 deg/sec
            accel = 0.08f;
            itemCapacity = 110;
            health = 700f;
            armor = 3f;
            hitSize = 12f;
            buildBeamOffset = 8f;
            payloadCapacity = 2f * 2f * tilesize * tilesize;
            pickupUnits = false;
            vulnerableWithPayloads = true;

            fogRadius = 0f;
            targetable = false;
            hittable = false;

            engineOffset = 7.5f;
            engineSize = 3.4f;

            setEnginesMirror(
            new UnitEngine(35 / 4f, -13 / 4f, 2.7f, 315f),
            new UnitEngine(28 / 4f, -35 / 4f, 2.7f, 315f)
            );

            weapons.add(new RepairBeamWeapon(){{
                widthSinMag = 0.11f;
                reload = 20f;
                x = 19f/4f;
                y = 19f/4f;
                rotate = false;
                shootY = 0f;
                beamWidth = 0.7f;
                aimDst = 0f;
                shootCone = 40f;
                mirror = true;

                repairSpeed = 3.6f / 2f;
                fractionRepairSpeed = 0.03f;

                targetUnits = false;
                targetBuildings = true;
                autoTarget = false;
                controllable = true;
                laserColor = Pal.accent;
                healColor = Pal.accent;

                bullet = new BulletType(){{
                    maxRange = 65f;
                }};
            }});
        }};

        //endregion
        //region internal + special

        coreFlyer = new UnitType("core-flyer"){{
            flying = true;
            speed = 2.6f;
            //Keep high accel for near-instant response; drag must stay low here,
            //as high drag also amplifies effective speed in the unit movement model.
            accel = 1f;
            drag = 0.05f;
            hitSize = 40f;
            health = 1000f;
            armor = 1f;
            rotateSpeed = 6f;

            payloadCapacity = Mathf.sqr(6f) * tilePayload;
            pickupUnits = false;
            allowedInPayloads = false;
            useUnitCap = false;

            canAttack = false;
            targetAir = false;
            targetGround = false;
            omniMovement = false;
            rotateMoveFirst = true;

            drawBody = false;
            drawSoftShadow = false;
            drawCell = false;
            hidden = true;
        }

        @Override
        public void update(Unit unit){
            if(!(unit instanceof Payloadc payload) || payload.payloads().isEmpty()) return;
            CoreFlyerData data = getCoreFlyerData(unit);
            if(!data.active) return;

            if(!data.landing){
                float alignThreshold = 0.01f;
                float dx = data.target.x - unit.x;
                float dy = data.target.y - unit.y;
                float dst2 = dx * dx + dy * dy;
                float align2 = alignThreshold * alignThreshold;

                //For landing tasks, drive movement manually so center reaches target without inertial oscillation.
                if(dst2 > align2){
                    float dst = Mathf.sqrt(dst2);
                    float step = unit.speed() * Time.delta;
                    if(step >= dst){
                        unit.set(data.target.x, data.target.y);
                    }else{
                        float scl = step / dst;
                        unit.set(unit.x + dx * scl, unit.y + dy * scl);
                    }
                    unit.vel.setZero();
                    unit.rotation(Angles.moveToward(unit.rotation(), Mathf.angle(dx, dy), unit.type.rotateSpeed * Time.delta));
                    return;
                }

                unit.vel.setZero();

                //Rotate back to source building angle before beginning landing.
                float next = Angles.moveToward(unit.rotation(), data.returnRotation, unit.type.rotateSpeed * Time.delta);
                unit.rotation(next);
                if(Angles.angleDist(next, data.returnRotation) <= 0.6f){
                    unit.rotation(data.returnRotation);
                    data.landing = true;
                    data.landTime = coreFlyerLandTime;
                }
                return;
            }

            data.landTime -= Time.delta;
            if(data.landTime > 0f) return;

            unit.set(data.target.x, data.target.y);
            if(payload.dropLastPayload()){
                Fx.unitDrop.at(data.target.x, data.target.y);
                unit.remove();
                clearCoreFlyerData(unit);
            }else{
                data.landing = false;
                data.active = false;
            }
        }

        @Override
        public void killed(Unit unit){
            clearCoreFlyerData(unit);
        }

        @Override
        public void load(){
            super.load();
            region = Core.atlas.find("core-nucleus");
            uiIcon = Core.atlas.find("core-nucleus");
            fullIcon = Core.atlas.find("core-nucleus");
        }
        };

        scanProbe = new UnitType("scan-probe"){{
            flying = true;
            speed = 0f;
            accel = 1f;
            drag = 1f;
            hitSize = 4f;
            health = 1f;
            rotateSpeed = 6f;

            canAttack = false;
            targetAir = false;
            targetGround = false;
            useUnitCap = false;
            isEnemy = false;

            targetable = false;
            hittable = false;
            killable = false;
            physics = false;
            bounded = false;
            drawBody = false;
            drawSoftShadow = false;
            drawCell = false;
            drawMinimap = false;
            hidden = true;
            internal = true;
            fogRadius = 10f;
        }};

        block = new UnitType("block"){{
            speed = 0f;
            hitSize = 0f;
            health = 1;
            rotateSpeed = 3f; // 180 deg/sec
            itemCapacity = 0;
            hidden = true;
            internal = true;
        }};

        manifold = new ErekirUnitType("manifold"){{
            controller = u -> new CargoAI();
            isEnemy = false;
            allowedInPayloads = false;
            logicControllable = false;
            playerControllable = false;
            envDisabled = 0;
            payloadCapacity = 0f;

            lowAltitude = false;
            flying = true;
            drag = 0.06f;
            speed = 26.25f;
            rotateSpeed = 3f; // 180 deg/sec
            accel = 0.1f;
            itemCapacity = 100;
            health = 200f;
            hitSize = 11f;
            engineSize = 2.3f;
            engineOffset = 6.5f;
            hidden = true;

            setEnginesMirror(
                new UnitEngine(24 / 4f, -24 / 4f, 2.3f, 315f)
            );
        }};

        assemblyDrone = new ErekirUnitType("assembly-drone"){{
            controller = u -> new AssemblerAI();

            flying = true;
            drag = 0.06f;
            accel = 0.11f;
            speed = 9.75f;
            health = 90;
            engineSize = 2f;
            engineOffset = 6.5f;
            payloadCapacity = 0f;
            targetable = false;
            bounded = false;

            outlineColor = Pal.darkOutline;
            isEnemy = false;
            hidden = true;
            useUnitCap = false;
            logicControllable = false;
            playerControllable = false;
            allowedInPayloads = false;
            createWreck = false;
            envEnabled = Env.any;
            envDisabled = Env.none;
        }};

        //All energy units regenerate at a unified rate.
        for(UnitType type : content.units()){
            if(type != null && type.energyCapacity > 0f){
                type.energyRegen = 0.8f;
            }
        }

        //endregion
    }
}





