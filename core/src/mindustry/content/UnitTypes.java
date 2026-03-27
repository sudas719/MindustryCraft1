package mindustry.content;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.func.*;
import arc.struct.*;
import arc.util.*;
import arc.Events;
import arc.Core;
import mindustry.ai.*;
import mindustry.ai.types.*;
import mindustry.annotations.Annotations.*;
import mindustry.entities.*;
import mindustry.entities.units.AIController;
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
import mindustry.world.Block;
import mindustry.world.Build;
import mindustry.world.Tile;
import mindustry.world.blocks.ConstructBlock;
import mindustry.core.World;
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
    private static final float reaperKd8RangeTiles = 5f;
    private static final float reaperKd8Cooldown = 14f * 60f;
    private static final float reaperKd8ArmTime = 1.5f * 60f;
    private static final float reaperKd8Damage = 5f;
    private static final float reaperKd8ExplosionRadiusTiles = 1.5f;
    private static final float reaperKd8KnockbackTiles = 1.5f;
    private static final int reaperKd8KnockbackSteps = 5;
    private static final float reaperKd8KnockbackStepInterval = 3f;
    private static final float reaperKd8ExplosionVisualScale = 0.6f;
    private static final float reaperKd8ThrowSpeed = 6f;
    private static final float reaperKd8StunDuration = 0.6f * 60f;
    private static final IntMap<ReaperKd8Data> reaperKd8Data = new IntMap<>();
    private static final float hurricaneBaseRangeTiles = 5f;
    private static final float hurricaneLockCastRangeTiles = 6f;
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
    private static final float liberatorDefenseAcquireBonusTiles = 1f;
    private static final IntMap<LiberatorData> liberatorData = Sc2State.liberatorData;
    private static final IntMap<VikingData> vikingData = Sc2State.vikingData;
    private static final IntMap<MaceLocusTransformData> maceLocusTransformData = Sc2State.maceLocusTransformData;
    private static final float scaledTankVisualScale = 0.65f;
    private static final float scaledTankShadowScale = scaledTankVisualScale * 0.85f;
    private static final float medivacAfterburnerDuration = 6f * 60f;
    private static final float medivacBaseSpeed = 3.5f;
    private static final float medivacAfterburnerBonusSpeed = 2.44f;
    private static final float medivacLoadRange = 1.5f * tilesize;
    private static final float medivacHealRange = 5.5f * tilesize;
    private static final int medivacMaxSlots = 8;
    private static final IntSet medivacMovingUnload = new IntSet();
    private static final float ravenTurretDeployRange = 3f * tilesize;
    private static final float ravenTurretLifetime = 8f * 60f;
    private static final float ravenAntiArmorRange = 11f * tilesize;
    private static final float ravenAntiArmorRadius = 3f * tilesize;
    private static final float ravenAntiArmorDuration = 21f * 60f;
    private static final float ravenMatrixRange = 10f * tilesize;
    private static final float ravenMatrixDuration = 11f * 60f;
    private static final float ravenTurretCost = 50f;
    private static final float ravenAntiArmorCost = 75f;
    private static final float ravenMatrixCost = 75f;
    private static final float vikingTransformDuration = 2f * 60f;
    private static final float vikingMechSpeed = 3.15f;
    private static final float vikingMechRange = 6f * tilesize;
    private static final float battlecruiserWeaponRange = 6f * tilesize;
    private static final float battlecruiserYamatoRange = 11f * tilesize;
    private static final float battlecruiserYamatoChargeTime = 2f * 60f;
    private static final float battlecruiserYamatoCooldown = 71f * 60f;
    private static final float battlecruiserWarpChargeTime = 1f * 60f;
    private static final float battlecruiserWarpTransitTime = 4f * 60f;
    private static final float battlecruiserWarpVisionDelay = 1f * 60f;
    private static final float battlecruiserWarpVisionRadius = 8f;
    // visual-only departure animation window; does not affect warp invulnerability/timing
    private static final float battlecruiserWarpDepartureTime = 0.225f * 60f;
    private static final float battlecruiserWarpDepartureBackPhase = 0.46f;
    private static final float battlecruiserWarpCooldown = 71f * 60f;
    private static final float battlecruiserWarpAppearTime = 0.9f * 60f;
    private static final float battlecruiserWarpEmergenceStart = 0.74f;
    private static final float battlecruiserBodyScale = 0.60f;
    private static final float battlecruiserTextureXScale = 0.8f;
    private static final float battlecruiserGhostScale = battlecruiserBodyScale;
    private static final float scepterVisualScale = 0.8f;
    private static final float battlecruiserMaterializeFrontDelay = 0f;
    private static final float battlecruiserMaterializeFrontDuration = 0.45f;
    private static final int battlecruiserMaterializeSlices = 30;
    private static final float fortressBodyKickMaxAngle = 36f;
    private static final float fortressBodyKickInitialOmega = 136f;
    private static final float fortressBodyKickAngularAccel = 260f;
    private static final IntMap<FortressBodyKickData> fortressBodyKickData = new IntMap<>();
    private static final FloatSeq battlecruiserSpotMaskLeft = new FloatSeq();
    private static final FloatSeq battlecruiserSpotMaskRight = new FloatSeq();
    private static float battlecruiserSpotMaxWorldRadius = 2.6f;
    private static final Seq<BattlecruiserAfterDraw> battlecruiserAfterDrawQueue = new Seq<>();
    private static int battlecruiserAfterDrawCount = 0;
    private static boolean battlecruiserAfterDrawHooked = false;
    private static final Effect battlecruiserWarpDisintegrateEffect = new Effect(16f, e -> {
        Draw.z(Layer.effect + 0.2f);
        float rot = e.rotation;
        float size = e.data instanceof Float ? Math.max((Float)e.data, 20f) : 30f;
        float fin = e.fin();
        float fout = e.fout();
        float fx = Angles.trnsx(rot, 1f), fy = Angles.trnsy(rot, 1f);
        float nx = Angles.trnsx(rot + 90f, 1f), ny = Angles.trnsy(rot + 90f, 1f);
        TextureRegion white = Core.atlas.find("whiteui");
        if(!white.found()) return;

        Fx.rand.setSeed(e.id * 911L + 17L);
        for(int i = 0; i < 76; i++){
            // converge strips toward center lane
            float lane = Fx.rand.range(size * 0.28f) * Fx.rand.random(1f) * Fx.rand.random(1f);
            float alongStart = Fx.rand.random(-size * 0.2f, size * 0.16f);
            boolean slowGroup = Fx.rand.random(1f) < 0.58f;
            // many strips barely move; others move forward up to current max speed
            float moveSpeed = slowGroup ? Fx.rand.random(0.02f, 0.42f) : Fx.rand.random(4.8f, 18f);
            float along = alongStart + fin * moveSpeed;
            float px = e.x + nx * lane + fx * along;
            float py = e.y + ny * lane + fy * along;

            // raise short-strip ratio
            boolean shortShape = Fx.rand.random(1f) < 0.62f;
            float width = Fx.rand.random(0.85f, 1.65f) * (0.82f + fout * 0.38f);
            float length = shortShape ? Fx.rand.random(width * 0.95f, width * 1.35f) : Fx.rand.random(width * 4.8f, width * 10.8f);
            // fixed direction
            float drawRot = rot;
            float alpha = (0.12f + 0.45f * fout) * Fx.rand.random(0.6f, 1f);

            // glowing green frame
            Draw.blend(Blending.additive);
            Draw.color(0.36f, 1f, 0.44f, alpha * 0.4f);
            Lines.stroke(Math.max(0.14f, Math.min(width, length) * 0.16f));
            drawBattlecruiserRectOutline(px, py, length + 1.1f, width + 1.1f, drawRot);
            Draw.blend();

            Draw.color(0.38f, 1f, 0.5f, alpha * 0.95f);
            Lines.stroke(Math.max(0.11f, Math.min(width, length) * 0.11f));
            drawBattlecruiserRectOutline(px, py, length + 0.66f, width + 0.66f, drawRot);

            // very transparent green center
            Draw.color(0.22f, 1f, 0.34f, alpha * 0.028f);
            Draw.rect(white, px, py, Math.max(0.14f, length - 1.32f), Math.max(0.14f, width - 1.32f), drawRot);
        }

        Drawf.light(e.x, e.y, 16f + size * 0.58f, Color.valueOf("54ff8b"), 0.12f * fout);
        Draw.reset();
    });
    private static final Effect battlecruiserWarpRippleEffect = new Effect(1f, e -> {
        // visual rings disabled; keep only shader-based distortion triggered in updateBattlecruiser()
    });
    private static final Effect preceptMuzzleSmokeEffect = new Effect(16f, 80f, e -> {
        Draw.z(Layer.effect + 0.04f);
        float fin = e.fin();
        float fout = e.fout();
        float dirX = Angles.trnsx(e.rotation, 1f);
        float dirY = Angles.trnsy(e.rotation, 1f);
        float sideX = Angles.trnsx(e.rotation + 90f, 1f);
        float sideY = Angles.trnsy(e.rotation + 90f, 1f);

        Fx.rand.setSeed(e.id * 1427L + 59L);
        for(int i = 0; i < 7; i++){
            float alongBase = Fx.rand.random(-0.9f, 2.6f);
            float alongSpeed = Fx.rand.random(0.35f, 2.4f);
            float side = Fx.rand.range(1.35f) * (0.95f - fin * 0.35f);
            float px = e.x + dirX * (alongBase + alongSpeed * fin * 5.4f) + sideX * side;
            float py = e.y + dirY * (alongBase + alongSpeed * fin * 5.4f) + sideY * side;
            float radius = Fx.rand.random(0.75f, 1.7f) * (0.7f + fin * 1.15f);
            float alpha = fout * Fx.rand.random(0.08f, 0.2f);

            Draw.color(0f, 0f, 0f, alpha);
            Fill.circle(px, py, radius);
            Draw.color(0.08f, 0.08f, 0.08f, alpha * 0.35f);
            Fill.circle(px, py, radius * 0.55f);
        }

        Draw.reset();
    });
    private static final Effect infantryMuzzleFlashEffect = new Effect(12f, 48f, e -> {
        Draw.z(Layer.effect + 0.06f);
        float fout = e.fout();
        Draw.color(Color.valueOf("ffdca0"), Color.valueOf("ff9f43"), e.fin());
        Fill.circle(e.x, e.y, 0.55f + 0.8f * fout);
        Lines.stroke(1.25f * fout);
        Angles.randLenVectors(e.id, 7, 1.1f + 4f * e.fin(), e.rotation, 24f, (x, y) -> {
            Lines.lineAngle(e.x + x * 0.3f, e.y + y * 0.3f, Angles.angle(x, y), 1.2f + 2.1f * fout);
        });
        Drawf.light(e.x, e.y, 10f * fout, Color.valueOf("ffb66a"), 0.45f * fout);
        Draw.reset();
    });
    private static final Effect thorGroundHitEffect = new WrapEffect(Fx.dynamicExplosion, Color.white, 0.2f);
    private static final Effect thorAirHitEffect = new MultiEffect(
        Fx.reactorsmoke,
        new RadialEffect(Fx.reactorsmoke, 2, 180f, 2.4f)
    );
    private static final float bansheeCloakCost = 25f;
    private static final float bansheeCloakDrain = 1.3f;
    private static final int barracksBlastShieldCrystalCost = 100;
    private static final int barracksBlastShieldGasCost = 100;
    private static final float barracksBlastShieldResearchTime = 79f * 60f;
    private static final float barracksBlastShieldHpBonus = 10f;
    private static final int barracksStimpackCrystalCost = 100;
    private static final int barracksStimpackGasCost = 100;
    private static final float barracksStimpackResearchTime = 100f * 60f;
    private static final float barracksStimpackDuration = 10f * 60f;
    private static final float barracksStimpackCooldown = 1f * 60f;
    private static final float barracksStimpackMarineHealthCost = 10f;
    private static final float barracksStimpackMarauderHealthCost = 20f;
    private static final float barracksStimpackFlashDuration = 1f * 60f;
    private static final int barracksConcussiveCrystalCost = 50;
    private static final int barracksConcussiveGasCost = 50;
    private static final float barracksConcussiveResearchTime = 43f * 60f;
    private static final float barracksConcussiveDuration = 1f * 60f;
    private static final float barracksConcussiveSpeedPenalty = 1.57f;
    private static final int infernoPreheaterCrystalCost = 100;
    private static final int infernoPreheaterGasCost = 100;
    private static final float infernoPreheaterResearchTime = 79f * 60f;
    private static final int infernoPreheaterLocusLightBonusAmount = 5;
    private static final int infernoPreheaterMaceLightBonusAmount = 12;
    private static final int electromagneticFieldAcceleratorCrystalCost = 100;
    private static final int electromagneticFieldAcceleratorGasCost = 100;
    private static final float electromagneticFieldAcceleratorResearchTime = 100f * 60f;
    private static final int drillClawCrystalCost = 75;
    private static final int drillClawGasCost = 75;
    private static final float drillClawResearchTime = 79f * 60f;
    private static final int smartServosCrystalCost = 100;
    private static final int smartServosGasCost = 100;
    private static final float smartServosResearchTime = 79f * 60f;
    private static final int bansheeCloakFieldCrystalCost = 100;
    private static final int bansheeCloakFieldGasCost = 100;
    private static final float bansheeCloakFieldResearchTime = 79f * 60f;
    private static final int bansheeAfterburnerCrystalCost = 125;
    private static final int bansheeAfterburnerGasCost = 125;
    private static final float bansheeAfterburnerResearchTime = 79f * 60f;
    private static final int ravenMatrixTechCrystalCost = 50;
    private static final int ravenMatrixTechGasCost = 50;
    private static final float ravenMatrixTechResearchTime = 57f * 60f;
    private static final int battlecruiserWeaponRefitCrystalCost = 150;
    private static final int battlecruiserWeaponRefitGasCost = 150;
    private static final float battlecruiserWeaponRefitResearchTime = 100f * 60f;
    private static final int medivacCaduceusReactorCrystalCost = 100;
    private static final int medivacCaduceusReactorGasCost = 100;
    private static final float medivacCaduceusReactorResearchTime = 50f * 60f;
    private static final int liberatorAdvancedBallisticsCrystalCost = 150;
    private static final int liberatorAdvancedBallisticsGasCost = 150;
    private static final float liberatorAdvancedBallisticsResearchTime = 79f * 60f;
    private static final float maceLocusTransformBaseTime = 3f * 60f;
    private static final float maceLocusTransformSmartServoTime = 1f * 60f;
    private static final float vikingTransformSmartServoTime = 1f * 60f;
    private static final float scepterSwitchSmartServoTime = 1f * 60f;
    private static final float liberatorDeploySmartServoTime = 2f * 60f;
    private static final float liberatorUndeploySmartServoTime = 2f * 60f;
    private static final float widowBurrowDrillClawTime = 1f * 60f;
    private static final float widowUnburrowDrillClawTime = 1f * 60f;
    private static final float hurricaneLockDamage = 20f;
    private static final float hurricaneLockUpgradedDamage = 30f;
    private static final float hurricaneBaseMissileDamage = 18f;
    private static final int armoryQueueVehicleWeapon = 1;
    private static final int armoryQueueVehicleArmor = 2;
    private static final int armoryQueueShipWeapon = 3;
    private static final int engineeringQueueInfantryWeapon = 1;
    private static final int engineeringQueueInfantryArmor = 2;
    private static final int engineeringQueueInstantTracking = 3;
    private static final int engineeringQueueSteelArmor = 4;
    private static final int barracksQueueBlastShield = 1;
    private static final int barracksQueueStimpack = 2;
    private static final int barracksQueueConcussive = 3;
    private static final int heavyFactoryQueueInfernoPreheater = 1;
    private static final int heavyFactoryQueueElectromagneticFieldAccelerator = 2;
    private static final int heavyFactoryQueueDrillClaw = 3;
    private static final int heavyFactoryQueueSmartServos = 4;
    private static final int starportQueueCloakField = 1;
    private static final int starportQueueAfterburner = 2;
    private static final int starportQueueMatrix = 3;
    private static final int fusionCoreQueueWeaponRefit = 1;
    private static final int fusionCoreQueueCaduceusReactor = 2;
    private static final int fusionCoreQueueAdvancedBallistics = 3;
    private static final int infantryWeaponMaxLevel = 3;
    private static final int[] infantryWeaponCrystalCost = {0, 100, 150, 200};
    private static final int[] infantryWeaponGasCost = {0, 100, 150, 200};
    private static final float[] infantryWeaponResearchTime = {0f, 114f * 60f, 136f * 60f, 157f * 60f};
    private static final int vehicleWeaponMaxLevel = 3;
    private static final int[] vehicleWeaponCrystalCost = {0, 100, 175, 250};
    private static final int[] vehicleWeaponGasCost = {0, 100, 175, 250};
    private static final float[] vehicleWeaponResearchTime = {0f, 114f * 60f, 136f * 60f, 157f * 60f};
    private static final int shipWeaponMaxLevel = 3;
    private static final int[] shipWeaponCrystalCost = {0, 100, 175, 250};
    private static final int[] shipWeaponGasCost = {0, 100, 175, 250};
    private static final float[] shipWeaponResearchTime = {0f, 114f * 60f, 136f * 60f, 157f * 60f};
    private static final int vehicleArmorMaxLevel = 3;
    private static final int[] vehicleArmorCrystalCost = {0, 100, 175, 250};
    private static final int[] vehicleArmorGasCost = {0, 100, 175, 250};
    private static final float[] vehicleArmorResearchTime = {0f, 114f * 60f, 136f * 60f, 157f * 60f};
    private static final int instantTrackingCrystalCost = 100;
    private static final int instantTrackingGasCost = 100;
    private static final float instantTrackingResearchTime = 57f * 60f;
    private static final int steelArmorCrystalCost = 150;
    private static final int steelArmorGasCost = 150;
    private static final float steelArmorResearchTime = 100f * 60f;
    private static final int ghostCamoCrystalCost = 150;
    private static final int ghostCamoGasCost = 150;
    private static final float ghostCamoResearchTime = 86f * 60f;
    private static final int ghostWarheadCrystalCost = 100;
    private static final int ghostWarheadGasCost = 100;
    private static final float ghostWarheadBuildTime = 43f * 60f;
    private static final float ghostTacticalNukeRange = 12f * tilesize;
    private static final float ghostTacticalNukeDelay = 14f * 60f;
    private static final float ghostTacticalNukeMissileFallTime = 0.9f * 60f;
    private static final float ghostTacticalNukeDamageRadius = 3f * tilesize;
    private static final float ghostTacticalNukeCenterDamage = 300f;
    private static final float ghostTacticalNukeEdgeDamage = 100f;
    private static final float ghostTacticalNukeBuildingBonus = 200f;
    private static final float ghostTacticalNukeMarkerRadius = tilesize;
    private static final float ghostStableAimRange = 10f * tilesize;
    private static final float ghostStableAimAimTime = 1.43f * 60f;
    private static final float ghostStableAimEnergyCost = 50f;
    private static final float ghostStableAimDamage = 130f;
    private static final float ghostStableAimPsionicBonus = 40f;
    private static final float ghostEmpRange = 10f * tilesize;
    private static final float ghostEmpRadius = 1.5f * tilesize;
    private static final float ghostEmpEnergyCost = 75f;
    private static final float ghostEmpShieldDamage = 100f;
    private static final float ghostEmpPsionicEnergyBurn = 100f;
    private static final float ghostEmpRevealDuration = 5f * 60f;
    private static final float ghostEmpProjectileSpeed = 5.2f;
    private static final float ghostEmpAfterglowDelay = 22f;
    private static final float targetedPointAbilityFacingTolerance = 5f;
    private static final Effect ghostEmpImpactEffect = new Effect(42f, 260f, e -> {
        Draw.z(Layer.effect + 0.25f);
        float fin = e.fin();

        float brightLife = 0.42f;
        float brightFin = Mathf.clamp(fin / brightLife);
        float brightAlpha = Mathf.clamp(1f - brightFin);
        float brightRadius = Mathf.lerp(1.8f, 5.8f, Interp.pow2Out.apply(brightFin));

        float darkLife = 0.78f;
        float darkFin = Mathf.clamp(fin / darkLife);
        float darkAlpha = Mathf.clamp(1f - darkFin);
        float darkRadius = Mathf.lerp(3.4f, 10.6f, Interp.pow2Out.apply(darkFin));

        // central small bright-blue dust/fog sphere (disappears first)
        Draw.color(0.62f, 0.88f, 1f, 0.26f * brightAlpha);
        Fill.circle(e.x, e.y, brightRadius * (0.72f + 0.28f * brightAlpha));
        Fx.rand.setSeed(e.id * 733L + 11L);
        for(int i = 0; i < 24; i++){
            float ang = Fx.rand.random(360f);
            float rr = brightRadius * Mathf.sqrt(Fx.rand.random(1f));
            float wobble = Mathf.sin(Time.time * (0.28f + Fx.rand.random(0.26f)) + Fx.rand.random(Mathf.PI2)) * 0.45f * fin;
            float px = e.x + Angles.trnsx(ang, rr + wobble);
            float py = e.y + Angles.trnsy(ang, rr + wobble);
            float size = Fx.rand.random(0.32f, 1.05f) * (0.6f + 0.45f * brightAlpha);
            Draw.color(0.72f, 0.94f, 1f, (0.06f + Fx.rand.random(0.16f)) * brightAlpha);
            Fill.circle(px, py, size);
        }

        // larger, darker blue dust/fog sphere
        Draw.color(0.18f, 0.42f, 0.78f, 0.2f * darkAlpha);
        Fill.circle(e.x, e.y, darkRadius * (0.8f + 0.2f * darkAlpha));
        Fx.rand.setSeed(e.id * 991L + 37L);
        for(int i = 0; i < 42; i++){
            float ang = Fx.rand.random(360f);
            float rr = darkRadius * Mathf.sqrt(Fx.rand.random(1f));
            float wobble = Mathf.sin(Time.time * (0.19f + Fx.rand.random(0.19f)) + Fx.rand.random(Mathf.PI2)) * 0.62f * fin;
            float px = e.x + Angles.trnsx(ang, rr + wobble);
            float py = e.y + Angles.trnsy(ang, rr + wobble);
            float size = Fx.rand.random(0.42f, 1.35f) * (0.62f + 0.42f * darkAlpha);
            Draw.color(0.24f, 0.58f, 0.96f, (0.045f + Fx.rand.random(0.11f)) * darkAlpha);
            Fill.circle(px, py, size);
        }

        // dark blue ring expands and fades
        float ringFin = Interp.pow2Out.apply(fin);
        Draw.color(0.15f, 0.3f, 0.58f, 0.48f * (1f - fin));
        Lines.stroke((1.95f - 1.25f * fin) * (0.82f + 0.18f * darkAlpha));
        Lines.circle(e.x, e.y, 2.4f + ringFin * 17.5f);

        // large electric arcs: active from start until dark sphere nearly gone
        float arcFade = Mathf.clamp((darkLife - fin) / darkLife);
        if(arcFade > 0.001f){
            float spin = Time.time * 3.2f;
            Draw.color(0.5f, 0.84f, 1f, (0.18f + 0.32f * Mathf.absin(Time.time + e.id * 0.07f, 1.9f, 1f)) * arcFade);
            Lines.stroke((0.85f + 0.55f * Mathf.absin(Time.time + e.id * 0.13f, 2.3f, 1f)) * arcFade);
            int arcs = 6;
            for(int i = 0; i < arcs; i++){
                float angle = spin + i * (360f / arcs) + Mathf.sin(Time.time * (1.6f + i * 0.16f) + i * 11f) * 13f;
                float arcDeg = 36f + Mathf.absin(Time.time + i * 6f, 2.6f, 26f);
                float radius = darkRadius * (0.68f + 0.2f * Mathf.sin(Time.time * 0.87f + i * 1.7f));
                Lines.arc(e.x, e.y, Math.max(1.8f, radius), arcDeg / 360f, angle);
            }
        }

        Drawf.light(e.x, e.y, 18f + darkRadius * 1.8f, Color.valueOf("66bfff"), 0.2f * darkAlpha);
        Draw.reset();
    });
    private static final Effect ghostEmpAfterglowEffect = new Effect(34f, 180f, e -> {
        Draw.z(Layer.effect + 0.21f);
        float fin = e.fin();
        float fout = e.fout();

        // small, slower arc flicker after the sphere vanishes
        float flicker = Mathf.absin(Time.time + e.id * 0.19f, 7.5f, 1f);
        if(flicker > 0.32f){
            Draw.color(0.38f, 0.78f, 1f, 0.24f * fout * flicker);
            Lines.stroke((0.35f + 0.42f * flicker) * fout);
            float spin = Time.time * 1.05f;
            for(int i = 0; i < 3; i++){
                float angle = spin + i * 120f + Mathf.sin(Time.time * (0.62f + i * 0.13f) + i * 9f) * 11f;
                float arcDeg = 22f + Mathf.absin(Time.time + i * 9f, 8f, 19f);
                float radius = 2.2f + i * 1.45f + Mathf.absin(Time.time + i * 5f, 6f, 0.9f);
                Lines.arc(e.x, e.y, radius, arcDeg / 360f, angle);
            }
        }

        // faint ghost-blue floating particles that slowly disappear
        Fx.rand.setSeed(e.id * 1733L + 71L);
        for(int i = 0; i < 14; i++){
            float ang = Fx.rand.random(360f);
            float base = Fx.rand.random(0.8f, 4.6f);
            float speed = Fx.rand.random(0.06f, 0.28f);
            float drift = base + fin * (2f + 11f * speed);
            float wobble = Mathf.sin(Time.time * (0.45f + Fx.rand.random(0.35f)) + Fx.rand.random(Mathf.PI2)) * 0.85f * fin;
            float px = e.x + Angles.trnsx(ang, drift + wobble);
            float py = e.y + Angles.trnsy(ang, drift) + fin * 1.8f;
            float size = Fx.rand.random(0.2f, 0.68f) * (0.62f + 0.38f * fout);
            Draw.color(0.38f, 0.82f, 1f, (0.04f + Fx.rand.random(0.1f)) * fout);
            Fill.circle(px, py, size);
        }

        Drawf.light(e.x, e.y, 10f + 6f * fout, Color.valueOf("59b9ff"), 0.13f * fout);
        Draw.reset();
    });
    private static final IntMap<RavenData> ravenData = new IntMap<>();
    private static final IntMap<BattlecruiserData> battlecruiserData = new IntMap<>();
    private static final IntMap<InfantryWeaponData> infantryWeaponData = Sc2State.infantryWeaponData;
    private static final IntMap<VehicleWeaponData> vehicleWeaponData = Sc2State.vehicleWeaponData;
    private static final IntMap<ShipWeaponData> shipWeaponData = Sc2State.shipWeaponData;
    private static final IntMap<VehicleArmorData> vehicleArmorData = Sc2State.vehicleArmorData;
    private static final IntMap<InfantryArmorData> infantryArmorData = Sc2State.infantryArmorData;
    private static final IntMap<InstantTrackingData> instantTrackingData = Sc2State.instantTrackingData;
    private static final IntMap<SteelArmorData> steelArmorData = Sc2State.steelArmorData;
    private static final IntMap<GhostCamoData> ghostCamoData = Sc2State.ghostCamoData;
    private static final IntMap<BarracksBlastShieldData> barracksBlastShieldData = Sc2State.barracksBlastShieldData;
    private static final IntMap<BarracksStimpackData> barracksStimpackData = Sc2State.barracksStimpackData;
    private static final IntMap<BarracksConcussiveData> barracksConcussiveData = Sc2State.barracksConcussiveData;
    private static final IntMap<InfernoPreheaterData> infernoPreheaterData = Sc2State.infernoPreheaterData;
    private static final IntMap<ElectromagneticFieldAcceleratorData> electromagneticFieldAcceleratorData = Sc2State.electromagneticFieldAcceleratorData;
    private static final IntMap<DrillClawData> drillClawData = Sc2State.drillClawData;
    private static final IntMap<SmartServosData> smartServosData = Sc2State.smartServosData;
    private static final IntMap<BansheeCloakFieldData> bansheeCloakFieldData = Sc2State.bansheeCloakFieldData;
    private static final IntMap<BansheeAfterburnerData> bansheeAfterburnerData = Sc2State.bansheeAfterburnerData;
    private static final IntMap<RavenMatrixTechData> ravenMatrixTechData = Sc2State.ravenMatrixTechData;
    private static final IntMap<BattlecruiserWeaponRefitData> battlecruiserWeaponRefitData = Sc2State.battlecruiserWeaponRefitData;
    private static final IntMap<MedivacCaduceusReactorData> medivacCaduceusReactorData = Sc2State.medivacCaduceusReactorData;
    private static final IntMap<LiberatorAdvancedBallisticsData> liberatorAdvancedBallisticsData = Sc2State.liberatorAdvancedBallisticsData;
    private static final IntMap<IntSeq> armoryResearchQueue = Sc2State.armoryResearchQueue;
    private static final IntMap<IntSeq> engineeringResearchQueue = Sc2State.engineeringResearchQueue;
    private static final IntMap<IntSeq> barracksResearchQueue = Sc2State.barracksResearchQueue;
    private static final IntMap<IntSeq> heavyFactoryResearchQueue = Sc2State.heavyFactoryResearchQueue;
    private static final IntMap<IntSeq> starportResearchQueue = Sc2State.starportResearchQueue;
    private static final IntMap<IntSeq> fusionCoreResearchQueue = Sc2State.fusionCoreResearchQueue;
    private static final IntFloatMap barracksStimpackCooldowns = Sc2State.barracksStimpackCooldowns;
    private static final IntFloatMap barracksStimpackActiveUnits = new IntFloatMap();
    private static final IntFloatMap barracksStimpackStartFlashes = new IntFloatMap();
    private static final IntFloatMap barracksStimpackEndFlashes = new IntFloatMap();
    private static final IntMap<GhostWarheadSiloData> ghostWarheadSiloData = new IntMap<>();
    private static final IntMap<GhostTacticalNukeData> ghostTacticalNukeData = new IntMap<>();
    private static final IntMap<GhostStableAimData> ghostStableAimData = new IntMap<>();
    private static final IntMap<GhostEmpData> ghostEmpData = new IntMap<>();
    private static boolean infantryUpgradeHooksInitialized = false;
    private static boolean vikingHooksInitialized = false;
    private static BulletType battlecruiserYamatoBullet;
    private static BulletType ghostStableAimBullet;
    private static BulletType ghostEmpBullet;
    private static BulletType reaperKd8Bullet;
    private static TextureRegion barracksStimpackLightRegion;
    private static final Effect reaperKd8ArmEffect = new Effect(reaperKd8ArmTime, e -> {
        Draw.z(Layer.effect + 0.05f);
        float fout = e.fout();
        float fin = e.fin();

        Draw.color(Color.black, 0.85f * fout);
        Fill.circle(e.x, e.y, 1.7f);
        Draw.color(Color.valueOf("ff3b3b"), 0.9f * fout);
        Fill.circle(e.x, e.y, 1.1f);

        int segments = Mathf.clamp((int)(1f + fin * 7.999f), 1, 8);
        float radius = 4f;
        float len = 2.6f;
        float step = 360f / 8f;
        Lines.stroke(1.1f);
        Draw.color(Color.valueOf("ff3b3b"), 0.55f * fout);
        for(int i = 0; i < segments; i++){
            float ang = i * step;
            float sx = e.x + Angles.trnsx(ang, radius);
            float sy = e.y + Angles.trnsy(ang, radius);
            Lines.lineAngle(sx, sy, ang, len);
        }
        Draw.reset();
    });

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

    public static class ReaperKd8Data{
        public final Vec2 target = new Vec2();
        public boolean active = false;
        public float cooldown = 0f;
    }

    public static class HurricaneLockData{
        public int targetId = -1;
        public int targetBuildPos = -1;
        public float activeTime = 0f;
        public float cooldown = 0f;
        public float flash = 0f;
    }

    public static class HurricaneMissileData{
        public Teamc target;
        public boolean lockedShot;

        public HurricaneMissileData(@Nullable Teamc target, boolean lockedShot){
            this.target = target;
            this.lockedShot = lockedShot;
        }
    }

    public static class PreceptSiegeData{
        public float cooldown = 0f;
        public float flash = 0f;
        public boolean siegeMode = false;
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

    public static class VikingData{
        public boolean mechMode = false;
        public boolean transforming = false;
        public boolean toMech = false;
        public float transformTime = 0f;
    }

    public static class MaceLocusTransformData{
        public boolean transforming = false;
        public boolean toLocus = false;
        public float transformTime = 0f;
    }

    public static class RavenData{
        public final Vec2 antiArmorTarget = new Vec2();
        public final Vec2 turretTarget = new Vec2();
        public int matrixTargetId = -1;
        public boolean pendingAntiArmor = false;
        public boolean pendingMatrix = false;
        public boolean pendingTurret = false;
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
        public float warpDepartureTime = 0f;
        public boolean warpDepartureBurstTriggered = false;
        public float warpVisionTime = 0f;
        public @Nullable Unit warpVisionProbe;
    }

    public static class InfantryWeaponData{
        public int level = 0;
        public int researchingLevel = 0;
        public float researchTime = 0f;
    }

    public static class VehicleWeaponData{
        public int level = 0;
        public int researchingLevel = 0;
        public float researchTime = 0f;
    }

    public static class ShipWeaponData{
        public int level = 0;
        public int researchingLevel = 0;
        public float researchTime = 0f;
    }

    public static class VehicleArmorData{
        public int level = 0;
        public int researchingLevel = 0;
        public float researchTime = 0f;
    }

    public static class InfantryArmorData{
        public int level = 0;
        public int researchingLevel = 0;
        public float researchTime = 0f;
    }

    public static class InstantTrackingData{
        public int level = 0;
        public boolean researching = false;
        public float researchTime = 0f;
    }

    public static class SteelArmorData{
        public int level = 0;
        public boolean researching = false;
        public float researchTime = 0f;
    }

    public static class GhostCamoData{
        public int level = 0;
        public boolean researching = false;
        public float researchTime = 0f;
    }

    public static class BarracksBlastShieldData{
        public int level = 0;
        public boolean researching = false;
        public float researchTime = 0f;
    }

    public static class BarracksStimpackData{
        public int level = 0;
        public boolean researching = false;
        public float researchTime = 0f;
    }

    public static class BarracksConcussiveData{
        public int level = 0;
        public boolean researching = false;
        public float researchTime = 0f;
    }

    public static class InfernoPreheaterData{
        public int level = 0;
        public boolean researching = false;
        public float researchTime = 0f;
    }

    public static class ElectromagneticFieldAcceleratorData{
        public int level = 0;
        public boolean researching = false;
        public float researchTime = 0f;
    }

    public static class DrillClawData{
        public int level = 0;
        public boolean researching = false;
        public float researchTime = 0f;
    }

    public static class SmartServosData{
        public int level = 0;
        public boolean researching = false;
        public float researchTime = 0f;
    }

    public static class BansheeCloakFieldData{
        public int level = 0;
        public boolean researching = false;
        public float researchTime = 0f;
    }

    public static class BansheeAfterburnerData{
        public int level = 0;
        public boolean researching = false;
        public float researchTime = 0f;
    }

    public static class RavenMatrixTechData{
        public int level = 0;
        public boolean researching = false;
        public float researchTime = 0f;
    }

    public static class BattlecruiserWeaponRefitData{
        public int level = 0;
        public boolean researching = false;
        public float researchTime = 0f;
    }

    public static class MedivacCaduceusReactorData{
        public int level = 0;
        public boolean researching = false;
        public float researchTime = 0f;
    }

    public static class LiberatorAdvancedBallisticsData{
        public int level = 0;
        public boolean researching = false;
        public float researchTime = 0f;
    }

    public static class GhostWarheadSiloData{
        public int buildPos = -1;
        public boolean producing = false;
        public boolean armed = false;
        public float buildTime = 0f;
    }

    public static class GhostTacticalNukeData{
        public final Vec2 target = new Vec2();
        public int teamId = -1;
        public int reservedSiloPos = -1;
        public float delayTime = 0f;
        public float missileTime = 0f;
        public boolean active = false;
        public boolean missileFalling = false;
    }

    public static class GhostStableAimData{
        public int targetId = -1;
        public boolean active = false;
        public boolean aiming = false;
        public float aimTime = 0f;
        public float startHealth = 0f;
        public float startHitTime = 0f;
    }

    public static class GhostEmpData{
        public final Vec2 target = new Vec2();
        public boolean active = false;
    }

    private static boolean faceTargetedAbilityPoint(@Nullable Unit unit, float x, float y){
        if(unit == null) return false;
        if(unit.within(x, y, 0.01f)) return true;
        unit.lookAt(x, y);
        return Angles.within(unit.rotation, unit.angleTo(x, y), targetedPointAbilityFacingTolerance);
    }

    private static void holdForTargetedAbility(@Nullable Unit unit){
        if(unit == null) return;
        unit.vel.setZero();
        if(unit.controller() instanceof CommandAI ai){
            ai.clearCommands();
        }
    }

    public static class FortressShellData{
        public Teamc target;
        public float originX, originY;
        public float lastInRangeX = Float.NaN, lastInRangeY = Float.NaN;
    }

    public static class FortressBodyKickData{
        public float offset;
        public float omega;
        public float sign;
        public boolean active;
        public float frozenMount0 = Float.NaN;
        public float frozenMount1 = Float.NaN;
    }

    private static class BattlecruiserAfterDraw{
        Unit unit;
        float x;
        float y;
        float rotation;
        float scanFin;
        boolean drawWeapons;
        boolean targetGhost;
        float ghostFade;

        BattlecruiserAfterDraw set(Unit unit, float x, float y, float rotation, float scanFin, boolean drawWeapons){
            this.unit = unit;
            this.x = x;
            this.y = y;
            this.rotation = rotation;
            this.scanFin = scanFin;
            this.drawWeapons = drawWeapons;
            this.targetGhost = false;
            this.ghostFade = 1f;
            return this;
        }

        BattlecruiserAfterDraw setTargetGhost(Unit unit, float x, float y, float rotation, float scanFin, float ghostFade){
            this.unit = unit;
            this.x = x;
            this.y = y;
            this.rotation = rotation;
            this.scanFin = scanFin;
            this.drawWeapons = false;
            this.targetGhost = true;
            this.ghostFade = ghostFade;
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

                if(entry.targetGhost){
                    drawBattlecruiserTargetScanGhost(entry.unit, entry.x, entry.y, entry.rotation, entry.scanFin, entry.ghostFade);
                }else{
                    drawBattlecruiserArrivalStrips(entry.unit, entry.x, entry.y, entry.rotation, entry.scanFin);
                    drawBattlecruiserMaterialization(entry.unit, entry.x, entry.y, entry.rotation, entry.scanFin, entry.drawWeapons);
                }
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

    private static void queueBattlecruiserTargetGhostAfterDraw(Unit unit, float x, float y, float rotation, float scanFin, float fade){
        ensureBattlecruiserAfterDrawHook();

        int index = battlecruiserAfterDrawCount++;
        if(index >= battlecruiserAfterDrawQueue.size){
            battlecruiserAfterDrawQueue.add(new BattlecruiserAfterDraw());
        }

        battlecruiserAfterDrawQueue.get(index).setTargetGhost(unit, x, y, rotation, scanFin, fade);
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
        return widowBurrowDuration(null);
    }

    public static float widowBurrowDuration(@Nullable Team team){
        if(drillClawLevel(team) > 0){
            return widowBurrowDrillClawTime;
        }
        return widowBurrowTime;
    }

    private static float widowUnburrowDuration(@Nullable Team team){
        if(drillClawLevel(team) > 0){
            return widowUnburrowDrillClawTime;
        }
        return widowUnburrowTime;
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
        return Mathf.clamp(1f - unit.getDuration(StatusEffects.widowBurrowing) / widowBurrowDuration(unit == null ? null : unit.team));
    }

    public static float widowReloadProgress(@Nullable Unit unit){
        if(!widowIsReloading(unit)) return 0f;
        return Mathf.clamp(unit.getDuration(StatusEffects.widowReloading) / widowReloadTime);
    }

    public static float widowUnburrowProgress(@Nullable Unit unit){
        if(!widowIsUnburrowing(unit)) return 0f;
        return Mathf.clamp(1f - unit.getDuration(StatusEffects.widowUnburrowing) / widowUnburrowDuration(unit == null ? null : unit.team));
    }

    public static void commandWidowBurrow(@Nullable Unit unit){
        if(!isWidow(unit)) return;
        if(widowIsBuried(unit) || widowIsBurrowing(unit) || widowIsUnburrowing(unit)) return;
        clearWidowLockData(unit);
        unit.unapply(StatusEffects.widowUnburrowing);
        unit.unapply(StatusEffects.widowBuried);
        unit.apply(StatusEffects.widowBurrowing, widowBurrowDuration(unit.team));
    }

    public static void commandWidowUnburrow(@Nullable Unit unit){
        if(!isWidow(unit)) return;
        if(widowIsUnburrowing(unit)) return;
        if(!widowIsBuried(unit) && !widowIsBurrowing(unit)) return;
        clearWidowLockData(unit);
        unit.unapply(StatusEffects.widowBurrowing);
        unit.unapply(StatusEffects.widowBuried);
        unit.apply(StatusEffects.widowUnburrowing, widowUnburrowDuration(unit.team));
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
        for(Building other : viewer.data().buildings){
            if(other == null || !other.isValid()) continue;
            float detectRange = other.block.stealthDetectionRange;
            if(detectRange > 0f && other.within(unit, detectRange)){
                return true;
            }
        }
        return false;
    }

    public static boolean widowHiddenFrom(@Nullable Unit unit, Team viewer){
        if(unit == null) return false;
        if(unit.hasEffect(StatusEffects.ghostEmpReveal)) return false;
        if(!widowIsStealthed(unit) && !bansheeCloaked(unit) && !ghostCloaked(unit)) return false;
        if(unit.team == viewer) return false;
        return !widowDetectedBy(unit, viewer);
    }

    private static @Nullable ReaperKd8Data getReaperKd8Data(@Nullable Unit unit, boolean create){
        if(unit == null || unit.type != reaper) return null;
        ReaperKd8Data data = reaperKd8Data.get(unit.id);
        if(data == null && create){
            data = new ReaperKd8Data();
            reaperKd8Data.put(unit.id, data);
        }
        return data;
    }

    public static void clearReaperKd8Data(@Nullable Unit unit){
        if(unit == null) return;
        reaperKd8Data.remove(unit.id);
    }

    public static boolean reaperCanUseKd8(@Nullable Unit unit){
        if(unit == null || unit.type != reaper) return false;
        if(ravenMatrixDisabled(unit)) return false;
        ReaperKd8Data data = getReaperKd8Data(unit, true);
        return data != null && !data.active && data.cooldown <= 0.001f;
    }

    public static float reaperKd8Cooldown(@Nullable Unit unit){
        ReaperKd8Data data = getReaperKd8Data(unit, false);
        return data == null ? 0f : data.cooldown;
    }

    public static float reaperKd8CooldownDuration(){
        return reaperKd8Cooldown;
    }

    public static float reaperKd8ArmTimeDuration(){
        return reaperKd8ArmTime;
    }

    public static float reaperKd8Range(){
        return reaperKd8RangeTiles * tilesize;
    }

    public static boolean commandReaperKd8(@Nullable Unit unit, @Nullable Vec2 target){
        if(unit == null || unit.type != reaper || target == null) return false;
        if(!reaperCanUseKd8(unit) || ravenMatrixDisabled(unit)) return false;
        ReaperKd8Data data = getReaperKd8Data(unit, true);
        if(data == null) return false;
        data.active = true;
        data.target.set(target);
        return true;
    }

    private static void updateReaperKd8(@Nullable Unit unit){
        if(unit == null || unit.type != reaper) return;
        ReaperKd8Data data = getReaperKd8Data(unit, false);
        if(data == null) return;

        if(data.cooldown > 0f){
            data.cooldown = Math.max(0f, data.cooldown - Time.delta);
        }

        if(!data.active) return;
        if(ravenMatrixDisabled(unit)){
            data.active = false;
            return;
        }

        Vec2 target = data.target;
        boolean facing = faceTargetedAbilityPoint(unit, target.x, target.y);

        float range = reaperKd8Range();
        if(unit.within(target, range)){
            holdForTargetedAbility(unit);
            if(!facing) return;

            if(reaperKd8Bullet != null){
                Bullet bullet = reaperKd8Bullet.create(unit, unit.team, unit.x, unit.y, unit.angleTo(target));
                if(bullet != null){
                    float dist = Mathf.dst(unit.x, unit.y, target.x, target.y);
                    bullet.lifetime = dist / Math.max(reaperKd8Bullet.speed, 0.001f);
                }
            }else{
                spawnReaperKd8Bomb(target.x, target.y, unit.team);
            }
            data.cooldown = reaperKd8Cooldown;
            data.active = false;
        }else if(unit.controller() instanceof CommandAI ai){
            ai.command(UnitCommand.moveCommand);
            ai.commandPosition(Tmp.v2.set(target.x, target.y), false);
        }
    }

    private static void spawnReaperKd8Bomb(float x, float y, Team team){
        reaperKd8ArmEffect.at(x, y);
        Time.run(reaperKd8ArmTime, () -> impactReaperKd8Bomb(team, x, y));
    }

    private static void applyReaperKd8Knockback(Unit unit, float fromX, float fromY){
        if(unit == null || !unit.isValid()) return;
        float dx = unit.x - fromX;
        float dy = unit.y - fromY;
        if(Mathf.len2(dx, dy) <= 0.001f) return;

        float len = reaperKd8KnockbackTiles * tilesize;
        float inv = 1f / (float)Math.sqrt(dx * dx + dy * dy);
        float floorDrag = unit.isGrounded() ? Math.max(unit.floorOn().dragMultiplier, 0.05f) : 1f;
        float drag = Math.max(unit.type.drag * floorDrag * state.rules.dragMultiplier, 0.05f);
        float frames = Math.max(reaperKd8KnockbackSteps * reaperKd8KnockbackStepInterval, 1f);
        float decay = Mathf.clamp(1f - drag, 0f, 0.999f);
        float velocity = (decay >= 0.999f ? len / frames : len * drag / Math.max(1f - Mathf.pow(decay, frames), 0.001f)) * 0.75f;

        unit.velAddNet(dx * inv * velocity, dy * inv * velocity);
    }

    private static void impactReaperKd8Bomb(@Nullable Team team, float x, float y){
        float radius = reaperKd8ExplosionRadiusTiles * tilesize;

        Units.nearby((Team)null, x - radius, y - radius, radius * 2f, radius * 2f, u -> {
            if(u == null || !u.isValid() || !u.hittable() || u.isFlying()) return;
            float maxDst = radius + u.hitSize / 2f;
            if(Mathf.dst(x, y, u.x, u.y) > maxDst) return;

            u.damagePierce(reaperKd8Damage);

            boolean heavy = u.type.unitClasses.contains(UnitClass.heavy);
            if(!heavy && !preceptIsSieged(u)){
                boolean blocked = World.raycast(World.toTile(x), World.toTile(y), u.tileX(), u.tileY(), (wx, wy) -> world.solid(wx, wy));
                if(!blocked){
                    applyReaperKd8Knockback(u, x, y);
                    u.apply(StatusEffects.unmoving, reaperKd8StunDuration);
                    u.apply(StatusEffects.disarmed, reaperKd8StunDuration);
                }
            }
        });

        Units.nearbyBuildings(x, y, radius + 16f, build -> {
            if(build == null || !build.isValid()) return;
            float maxDst = radius + build.hitSize() / 2f;
            if(Mathf.dst(x, y, build.x, build.y) > maxDst) return;
            build.damagePierce(reaperKd8Damage);
        });

        float visualRadius = radius / tilesize * reaperKd8ExplosionVisualScale;
        Fx.explosion.at(x, y);
        Fx.dynamicExplosion.at(x, y, visualRadius, Color.valueOf("ff3b3b"));
        Effect.shake(2f, 2f, x, y);
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

    public static boolean isViking(@Nullable Unit unit){
        return unit != null && flare != null && unit.type == flare;
    }

    public static boolean isMace(@Nullable Unit unit){
        return unit != null && mace != null && unit.type == mace;
    }

    public static boolean isLocus(@Nullable Unit unit){
        return unit != null && locus != null && unit.type == locus;
    }

    private static MaceLocusTransformData getMaceLocusTransformData(@Nullable Unit unit){
        if(unit == null){
            return new MaceLocusTransformData();
        }
        MaceLocusTransformData data = maceLocusTransformData.get(unit.id);
        if(data == null){
            data = new MaceLocusTransformData();
            maceLocusTransformData.put(unit.id, data);
        }
        return data;
    }

    private static void clearMaceLocusTransformData(@Nullable Unit unit){
        if(unit == null) return;
        maceLocusTransformData.remove(unit.id);
    }

    public static boolean maceLocusTransforming(@Nullable Unit unit){
        return (isMace(unit) || isLocus(unit)) && getMaceLocusTransformData(unit).transforming;
    }

    public static float maceLocusTransformProgress(@Nullable Unit unit){
        if(!maceLocusTransforming(unit)) return 0f;
        return Mathf.clamp(1f - getMaceLocusTransformData(unit).transformTime / Math.max(maceLocusTransformDuration(unit == null ? null : unit.team), 0.001f));
    }

    public static boolean maceCanTransformToLocus(@Nullable Unit unit){
        if(!isMace(unit)) return false;
        if(!infantryWeaponHasArmory(unit.team)) return false;
        if(ravenMatrixDisabled(unit)) return false;
        return !maceLocusTransforming(unit);
    }

    public static boolean locusCanTransformToMace(@Nullable Unit unit){
        if(!isLocus(unit)) return false;
        if(!infantryWeaponHasArmory(unit.team)) return false;
        if(ravenMatrixDisabled(unit)) return false;
        return !maceLocusTransforming(unit);
    }

    public static boolean commandMaceLocusMode(@Nullable Unit unit, boolean toLocus){
        if(toLocus){
            if(!maceCanTransformToLocus(unit)) return false;
        }else{
            if(!locusCanTransformToMace(unit)) return false;
        }

        MaceLocusTransformData data = getMaceLocusTransformData(unit);
        data.transforming = true;
        data.toLocus = toLocus;
        data.transformTime = maceLocusTransformDuration(unit == null ? null : unit.team);
        return true;
    }

    private static void ensureVikingHooks(){
        if(vikingHooksInitialized) return;
        vikingHooksInitialized = true;
        Events.on(WorldLoadEvent.class, e -> vikingData.clear());
    }

    public static VikingData getVikingData(@Nullable Unit unit){
        ensureVikingHooks();
        if(unit == null){
            return new VikingData();
        }
        VikingData data = vikingData.get(unit.id);
        if(data == null){
            data = new VikingData();
            vikingData.put(unit.id, data);
        }
        return data;
    }

    public static void clearVikingData(@Nullable Unit unit){
        if(unit == null) return;
        vikingData.remove(unit.id);
    }

    public static boolean vikingIsMechMode(@Nullable Unit unit){
        return isViking(unit) && getVikingData(unit).mechMode;
    }

    public static boolean vikingIsFighterMode(@Nullable Unit unit){
        return isViking(unit) && !getVikingData(unit).mechMode;
    }

    public static boolean vikingIsTransforming(@Nullable Unit unit){
        return isViking(unit) && getVikingData(unit).transforming;
    }

    public static float vikingTransformProgress(@Nullable Unit unit){
        if(!vikingIsTransforming(unit)) return 0f;
        return Mathf.clamp(1f - getVikingData(unit).transformTime / Math.max(vikingTransformDuration(unit == null ? null : unit.team), 0.001f));
    }

    public static boolean usesFlyingRules(@Nullable Unit unit){
        if(unit == null || unit.type == null) return false;
        return unit.type.flying && !vikingIsMechMode(unit);
    }

    private static boolean vikingHasLandingArea(@Nullable Unit unit){
        if(!isViking(unit)) return false;
        Tile center = world.tileWorld(unit.x, unit.y);
        if(center == null) return false;

        for(int dx = -1; dx <= 1; dx++){
            for(int dy = -1; dy <= 1; dy++){
                Tile tile = world.tile(center.x + dx, center.y + dy);
                if(tile == null) return false;
                if(tile.solid()) return false;
                if(tile.floor().isDeep()) return false;
                if(tile.build != null) return false;
            }
        }

        return true;
    }

    public static boolean vikingCanTransformToMech(@Nullable Unit unit){
        if(!isViking(unit)) return false;
        VikingData data = getVikingData(unit);
        if(data.transforming || data.mechMode) return false;
        if(ravenMatrixDisabled(unit)) return false;
        return vikingHasLandingArea(unit);
    }

    public static boolean vikingCanTransformToFighter(@Nullable Unit unit){
        if(!isViking(unit)) return false;
        VikingData data = getVikingData(unit);
        return data.mechMode && !data.transforming && !ravenMatrixDisabled(unit);
    }

    private static float vikingTransformDurationForTeam(@Nullable Team team){
        if(smartServosLevel(team) > 0){
            return vikingTransformSmartServoTime;
        }
        return vikingTransformDuration;
    }

    public static float vikingTransformDuration(@Nullable Team team){
        return vikingTransformDurationForTeam(team);
    }

    public static float maceLocusTransformDuration(@Nullable Team team){
        if(smartServosLevel(team) > 0){
            return maceLocusTransformSmartServoTime;
        }
        return maceLocusTransformBaseTime;
    }

    private static float liberatorDeployDuration(@Nullable Team team){
        if(smartServosLevel(team) > 0){
            return liberatorDeploySmartServoTime;
        }
        return liberatorDeployTime;
    }

    private static float liberatorUndeployDuration(@Nullable Team team){
        if(smartServosLevel(team) > 0){
            return liberatorUndeploySmartServoTime;
        }
        return liberatorUndeployTime;
    }

    public static float scepterSwitchDuration(@Nullable Team team){
        if(smartServosLevel(team) > 0){
            return scepterSwitchSmartServoTime;
        }
        return scepterSwitchTime;
    }

    public static float hurricaneMissileDamage(@Nullable Team team, boolean lockedShot){
        if(!lockedShot) return hurricaneBaseMissileDamage;
        return electromagneticFieldAcceleratorLevel(team) > 0 ? hurricaneLockUpgradedDamage : hurricaneLockDamage;
    }

    public static boolean commandVikingMode(@Nullable Unit unit, boolean mechMode){
        if(!isViking(unit)) return false;
        VikingData data = getVikingData(unit);

        if(mechMode){
            if(!vikingCanTransformToMech(unit)) return false;
            data.transforming = true;
            data.toMech = true;
            data.transformTime = vikingTransformDurationForTeam(unit.team);
            return true;
        }

        if(!vikingCanTransformToFighter(unit)) return false;
        data.mechMode = false;
        data.transforming = false;
        data.toMech = false;
        data.transformTime = 0f;
        return true;
    }

    public static void updateViking(@Nullable Unit unit){
        if(!isViking(unit)) return;
        VikingData data = getVikingData(unit);

        if(data.transforming){
            data.transformTime = Math.max(0f, data.transformTime - Time.delta);
            unit.vel.setZero();
            if(data.transformTime <= 0.001f){
                data.transforming = false;
                if(data.toMech){
                    data.mechMode = true;
                }
            }
        }

        if(data.mechMode){
            unit.elevation = 0f;
            unit.vel.limit(vikingMechSpeed);
        }else{
            unit.elevation = 1f;
        }
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

    public static float liberatorDefenseAcquireRange(@Nullable Team team){
        float base = liberatorDefenseRange();
        if(liberatorAdvancedBallisticsLevel(team) > 0){
            return base + liberatorDefenseAcquireBonusTiles * tilesize;
        }
        return base;
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

    public static float liberatorTransitionProgress(@Nullable Unit unit){
        if(!isLiberator(unit)) return 0f;
        LiberatorData data = getLiberatorData(unit);
        if(data.deploying){
            return Mathf.clamp(1f - data.transitionTime / Math.max(liberatorDeployDuration(unit.team), 0.001f));
        }
        if(data.undeploying){
            return Mathf.clamp(1f - data.transitionTime / Math.max(liberatorUndeployDuration(unit.team), 0.001f));
        }
        return 0f;
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
        float duration = liberatorDeployDuration(unit.team);
        data.pendingDeploy = false;
        data.deploying = true;
        data.undeploying = false;
        data.transitionTime = duration;
        unit.unapply(StatusEffects.liberatorUndeploying);
        unit.unapply(StatusEffects.liberatorDefending);
        unit.apply(StatusEffects.liberatorDeploying, duration);
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
        float duration = liberatorUndeployDuration(unit.team);
        data.pendingDeploy = false;
        data.deploying = false;
        data.undeploying = true;
        data.transitionTime = duration;
        unit.unapply(StatusEffects.liberatorDeploying);
        unit.unapply(StatusEffects.liberatorDefending);
        unit.apply(StatusEffects.liberatorUndeploying, duration);
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
            float progress = 1f - Mathf.clamp(data.transitionTime / Math.max(liberatorDeployDuration(unit.team), 0.001f));
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
                holdForTargetedAbility(unit);
                if(faceTargetedAbilityPoint(unit, data.zone.x, data.zone.y)){
                    startLiberatorDeploy(unit);
                }
            }else{
                if(unit.controller() instanceof CommandAI ai){
                    ai.command(UnitCommand.moveCommand);
                    if(ai.targetPos == null || !Mathf.within(ai.targetPos.x, ai.targetPos.y, data.zone.x, data.zone.y, 2f)){
                        ai.commandPosition(data.zone, false);
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
        return scepterSwitchDuration(null);
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

    public static float scepterSwitchProgress(@Nullable Unit unit){
        if(!scepterIsSwitching(unit)) return 0f;
        return Mathf.clamp(1f - getScepterModeData(unit).switchTime / Math.max(scepterSwitchDuration(unit == null ? null : unit.team), 0.001f));
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

        float duration = scepterSwitchDuration(unit.team);
        data.switching = true;
        data.switchToImpact = impactMode;
        data.switchTime = duration;
        unit.apply(StatusEffects.scepterSwitching, duration);
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

    public static float hurricaneLockCastRange(){
        return hurricaneLockCastRangeTiles * tilesize;
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

    public static boolean preceptSiegeMode(@Nullable Unit unit){
        if(!isSiegeTank(unit)) return false;
        return getPreceptSiegeData(unit).siegeMode;
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

    public static Vec2 preceptImpactPoint(@Nullable Bullet bullet, float x, float y){
        Tmp.v1.set(x, y);
        if(bullet == null) return Tmp.v1;
        if(!(bullet.data instanceof Teamc target) || !(target instanceof Sized sized)) return Tmp.v1;

        float radius = sized.hitSize() / 2f;
        if(radius <= 0.001f) return Tmp.v1.set(target.x(), target.y());

        float angle = Angles.angle(bullet.originX, bullet.originY, target.x(), target.y());
        float inset = Math.min(1f, radius * 0.5f);
        float offset = Math.max(0f, radius - inset);
        return Tmp.v1.set(target.x() - Angles.trnsx(angle, offset), target.y() - Angles.trnsy(angle, offset));
    }

    public static boolean preceptIsSieging(@Nullable Unit unit){
        return isSiegeTank(unit) && unit.hasEffect(StatusEffects.preceptSieging);
    }

    public static boolean preceptIsSieged(@Nullable Unit unit){
        return preceptSiegeMode(unit) && unit.hasEffect(StatusEffects.preceptSieged) && !preceptIsSieging(unit) && !preceptIsUnsieging(unit);
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
            data.siegeMode = true;
            data.cooldown = 0f;
            data.flash = 0f;
            unit.unapply(StatusEffects.preceptUnsieging);
            unit.unapply(StatusEffects.preceptSieged);
            unit.apply(StatusEffects.preceptSieging, preceptSiegeTransitionTime);
        }else{
            if(!preceptCanExitSiege(unit)) return;
            PreceptSiegeData data = getPreceptSiegeData(unit);
            data.siegeMode = false;
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
        if(data.activeTime <= 0.001f) return null;
        Teamc target = hurricaneResolveLockedTarget(data);
        if(target == null || target.inFogTo(unit.team) || Units.invalidateTarget(target, unit, Float.MAX_VALUE) || !hurricaneWithinLockRange(unit, target)) return null;
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
        if(unit.moving()) return false;
        HurricaneLockData data = getHurricaneLockData(unit);
        return data.cooldown <= 0.001f && data.activeTime <= 0.001f;
    }

    private static boolean hurricaneWithinLockRange(@Nullable Unit unit, @Nullable Teamc target){
        return unit != null
            && target != null
            && Mathf.within(unit.x, unit.y, target.getX(), target.getY(), hurricaneLockRange());
    }

    private static boolean hurricaneWithinCastRange(@Nullable Unit unit, @Nullable Teamc target){
        return unit != null
            && target != null
            && Mathf.within(unit.x, unit.y, target.getX(), target.getY(), hurricaneLockCastRange());
    }

    private static @Nullable Teamc hurricaneResolveLockedTarget(HurricaneLockData data){
        if(data == null) return null;

        if(data.targetId >= 0){
            Unit unit = Groups.unit.getByID(data.targetId);
            if(unit != null && unit.isValid()){
                return unit;
            }
        }

        if(data.targetBuildPos >= 0){
            Building build = world.build(data.targetBuildPos);
            if(build != null && build.isValid()){
                return build;
            }
        }

        return null;
    }

    public static @Nullable Teamc hurricaneFindTarget(@Nullable Unit unit){
        if(!isHurricane(unit)) return null;

        Team team = unit.team;
        float range = hurricaneLockCastRange();
        Teamc[] unitResult = {null};
        float[] unitDst2 = {Float.MAX_VALUE};

        Units.nearbyEnemies(team, unit.x, unit.y, range, other -> {
            if(other == null || other.dead() || !other.isValid() || other.team == Team.derelict) return;
            if(!other.checkTarget(true, true) || !other.hittable() || !other.targetable(team) || other.inFogTo(team)) return;
            if(!hurricaneWithinCastRange(unit, other)) return;

            float dst2 = Mathf.dst2(unit.x, unit.y, other.x, other.y);
            if(unitResult[0] == null || dst2 < unitDst2[0]){
                unitResult[0] = other;
                unitDst2[0] = dst2;
            }
        });

        if(unitResult[0] != null){
            return unitResult[0];
        }

        Building[] buildResult = {null};
        float[] buildDst2 = {Float.MAX_VALUE};

        Units.nearbyBuildings(unit.x, unit.y, range, build -> {
            if(build == null || !build.isValid() || build.inFogTo(team)) return;

            boolean enemy = build.team != team && (build.team != Team.derelict || state.rules.coreCapture);
            if(!enemy && !Units.targetableAllTeams(build)) return;
            if(!Units.canTargetBuilding(true, true, build) || !hurricaneWithinCastRange(unit, build)) return;

            float dst2 = Mathf.dst2(unit.x, unit.y, build.x, build.y);
            if(buildResult[0] == null || dst2 < buildDst2[0]){
                buildResult[0] = build;
                buildDst2[0] = dst2;
            }
        });

        return buildResult[0];
    }

    public static boolean hurricaneHasTarget(@Nullable Unit unit){
        return hurricaneFindTarget(unit) != null;
    }

    public static boolean commandHurricaneLock(@Nullable Unit unit){
        if(!hurricaneCanLock(unit)) return false;
        Teamc target = hurricaneFindTarget(unit);
        if(target == null) return false;

        HurricaneLockData data = getHurricaneLockData(unit);
        if(target instanceof Unit u){
            data.targetId = u.id;
            data.targetBuildPos = -1;
        }else if(target instanceof Building b){
            data.targetId = -1;
            data.targetBuildPos = b.pos();
        }else{
            return false;
        }
        data.activeTime = hurricaneLockTime;
        data.cooldown = 0f;
        data.flash = 0f;
        return true;
    }

    private static void finishHurricaneLock(HurricaneLockData data, boolean startCooldown){
        data.activeTime = 0f;
        data.targetId = -1;
        data.targetBuildPos = -1;
        if(startCooldown){
            data.cooldown = hurricaneLockCooldown;
            data.flash = 0f;
        }
    }

    public static void updateHurricaneLock(@Nullable Unit unit){
        if(!isHurricane(unit)) return;

        HurricaneLockData data = getHurricaneLockData(unit);
        if(data.activeTime > 0f){
            data.activeTime = Math.max(0f, data.activeTime - Time.delta);
            Teamc target = hurricaneResolveLockedTarget(data);
            if(target == null || target.inFogTo(unit.team) || Units.invalidateTarget(target, unit, Float.MAX_VALUE) || !hurricaneWithinLockRange(unit, target)){
                finishHurricaneLock(data, true);
                return;
            }

            if(data.activeTime <= 0.001f){
                finishHurricaneLock(data, true);
            }
        }else{
            data.targetId = -1;
            data.targetBuildPos = -1;

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
        }
    }

    private static @Nullable Teamc forcedFriendlyAttackTarget(@Nullable Bullet bullet){
        if(bullet == null || bullet.owner == null) return null;
        if(bullet.owner instanceof Unit owner && owner.controller() instanceof CommandAI ai){
            Teamc forced = ai.attackTarget;
            if(forced != null && forced.team() == bullet.team){
                return forced;
            }
        }
        if(bullet.owner instanceof Unit owner && owner.controller() instanceof Player player){
            float mx = player.mouseX, my = player.mouseY;
            Building build = world.buildWorld(mx, my);
            if(build != null && build.team == bullet.team && Units.canTargetBuilding(bullet.type.collidesAir, bullet.type.collidesGround, build)){
                return build;
            }

            float range = Math.max(8f, owner.hitSize);
            Unit target = Units.closest(bullet.team, mx, my, range, u -> u != owner && u.isValid()
                && u.checkTarget(bullet.type.collidesAir, bullet.type.collidesGround)
                && u.within(mx, my, u.hitSize / 2f));
            if(target != null){
                return target;
            }
        }
        return null;
    }

    private static boolean canDamageFriendlyOnlyWhenForced(@Nullable Bullet bullet, @Nullable Teamc target){
        if(bullet == null || target == null) return false;
        if(target.team() != bullet.team) return true;
        Teamc forced = forcedFriendlyAttackTarget(bullet);
        return forced != null && forced == target;
    }

    private static boolean canTrackFriendlyOnlyWhenForced(@Nullable Bullet bullet, @Nullable Teamc target){
        return target != null && (!(target instanceof Healthc h) || h.isValid()) && canDamageFriendlyOnlyWhenForced(bullet, target);
    }

    private static @Nullable DrawPart copyObviatePart(@Nullable DrawPart part){
        if(!(part instanceof RegionPart r)) return null;
        if((r.name != null && r.name.endsWith("-side")) || "-side".equals(r.suffix)){
            return null;
        }
        RegionPart copy = new RegionPart();
        copy.suffix = r.suffix;
        copy.name = r.name;
        copy.mirror = r.mirror;
        copy.outline = r.outline;
        copy.replaceOutline = r.replaceOutline;
        copy.drawRegion = r.drawRegion;
        copy.heatLight = r.heatLight;
        copy.clampProgress = r.clampProgress;
        copy.progress = r.progress;
        copy.growProgress = r.growProgress;
        copy.heatProgress = r.heatProgress;
        copy.blending = r.blending;
        copy.layer = r.layer;
        copy.layerOffset = r.layerOffset;
        copy.heatLayerOffset = r.heatLayerOffset;
        copy.turretHeatLayer = r.turretHeatLayer;
        copy.outlineLayerOffset = r.outlineLayerOffset;
        copy.x = r.x;
        copy.y = r.y;
        copy.xScl = r.xScl;
        copy.yScl = r.yScl;
        copy.rotation = r.rotation;
        copy.originX = r.originX;
        copy.originY = r.originY;
        copy.moveX = r.moveX;
        copy.moveY = r.moveY;
        copy.growX = r.growX;
        copy.growY = r.growY;
        copy.moveRot = r.moveRot;
        copy.heatLightOpacity = r.heatLightOpacity;
        copy.color = r.color == null ? null : r.color.cpy();
        copy.colorTo = r.colorTo == null ? null : r.colorTo.cpy();
        copy.mixColor = r.mixColor == null ? null : r.mixColor.cpy();
        copy.mixColorTo = r.mixColorTo == null ? null : r.mixColorTo.cpy();
        copy.heatColor = r.heatColor == null ? null : r.heatColor.cpy();

        copy.under = r.under;
        copy.weaponIndex = r.weaponIndex;
        copy.recoilIndex = r.recoilIndex;

        if(r.moves != null){
            for(int i = 0; i < r.moves.size; i++){
                DrawPart.PartMove move = r.moves.get(i);
                copy.moves.add(new DrawPart.PartMove(move.progress, move.x, move.y, move.gx, move.gy, move.rot));
            }
        }

        if(r.children != null){
            for(int i = 0; i < r.children.size; i++){
                DrawPart childCopy = copyObviatePart(r.children.get(i));
                if(childCopy != null){
                    copy.children.add(childCopy);
                }
            }
        }

        return copy;
    }

    public static boolean isBanshee(@Nullable Unit unit){
        return unit != null && horizon != null && unit.type == horizon;
    }

    public static boolean isGhost(@Nullable Unit unit){
        return unit != null && ghost != null && unit.type == ghost;
    }

    public static boolean ghostCloaked(@Nullable Unit unit){
        return isGhost(unit) && unit.hasEffect(StatusEffects.bansheeCloak);
    }

    public static boolean ghostCanToggleCloak(@Nullable Unit unit){
        if(!isGhost(unit)) return false;
        if(ghostCloaked(unit)) return true;
        return !ravenMatrixDisabled(unit)
            && unit.energy >= bansheeCloakCost
            && ghostCamoLevel(unit.team) > 0;
    }

    public static boolean commandGhostCloak(@Nullable Unit unit){
        if(!isGhost(unit)) return false;
        if(ghostCloaked(unit)){
            unit.unapply(StatusEffects.bansheeCloak);
            return true;
        }
        if(ravenMatrixDisabled(unit) || unit.energy < bansheeCloakCost || ghostCamoLevel(unit.team) <= 0) return false;
        unit.energy = Math.max(0f, unit.energy - bansheeCloakCost);
        unit.apply(StatusEffects.bansheeCloak, 1f);
        return true;
    }

    public static void updateGhost(@Nullable Unit unit){
        if(ghostCloaked(unit)){
            unit.energy = Math.max(0f, unit.energy - bansheeCloakDrain * Time.delta / 60f);
            if(unit.energy <= 0.001f){
                unit.unapply(StatusEffects.bansheeCloak);
            }
        }
        updateGhostStableAim(unit);
        updateGhostEmp(unit);
    }

    private static @Nullable GhostStableAimData getGhostStableAimData(@Nullable Unit unit, boolean create){
        if(!isGhost(unit)) return null;
        GhostStableAimData data = ghostStableAimData.get(unit.id);
        if(data == null && create){
            data = new GhostStableAimData();
            ghostStableAimData.put(unit.id, data);
        }
        return data;
    }

    public static void clearGhostStableAimData(@Nullable Unit unit){
        if(unit == null) return;
        ghostStableAimData.remove(unit.id);
    }

    public static @Nullable Unit ghostStableAimTarget(@Nullable Unit unit){
        GhostStableAimData data = getGhostStableAimData(unit, false);
        if(data == null || !data.active || data.targetId < 0) return null;
        Unit target = Groups.unit.getByID(data.targetId);
        return ghostStableAimValidTarget(target) ? target : null;
    }

    public static boolean ghostStableAimValidTarget(@Nullable Unit target){
        return target != null
            && target.isValid()
            && target.type.unitClasses.contains(UnitClass.biological);
    }

    public static boolean ghostStableAimPending(@Nullable Unit unit){
        GhostStableAimData data = getGhostStableAimData(unit, false);
        return data != null && data.active;
    }

    public static boolean ghostStableAimAiming(@Nullable Unit unit){
        GhostStableAimData data = getGhostStableAimData(unit, false);
        return data != null && data.active && data.aiming;
    }

    public static boolean ghostCanUseStableAim(@Nullable Unit unit){
        return isGhost(unit)
            && !ravenMatrixDisabled(unit)
            && !ghostStableAimPending(unit)
            && !ghostTacticalNukePending(unit)
            && !ghostEmpPending(unit)
            && unit.energy >= ghostStableAimEnergyCost;
    }

    public static float ghostStableAimRange(){
        return ghostStableAimRange;
    }

    public static boolean commandGhostStableAim(@Nullable Unit unit, @Nullable Unit target){
        if(!ghostCanUseStableAim(unit) || !ghostStableAimValidTarget(target)) return false;
        GhostStableAimData data = getGhostStableAimData(unit, true);
        if(data == null) return false;
        data.active = true;
        data.aiming = false;
        data.targetId = target.id;
        data.aimTime = 0f;
        data.startHealth = unit.health;
        data.startHitTime = unit.hitTime;
        return true;
    }

    public static boolean commandGhostCancelStableAim(@Nullable Unit unit){
        if(!isGhost(unit)) return false;
        GhostStableAimData data = getGhostStableAimData(unit, false);
        if(data == null || !data.active) return false;
        data.active = false;
        data.aiming = false;
        data.targetId = -1;
        data.aimTime = 0f;
        return true;
    }

    private static void updateGhostStableAim(@Nullable Unit unit){
        if(!isGhost(unit)) return;
        GhostStableAimData data = getGhostStableAimData(unit, false);
        if(data == null || !data.active) return;

        Unit target = Groups.unit.getByID(data.targetId);
        if(!ghostStableAimValidTarget(target) || ravenMatrixDisabled(unit)){
            commandGhostCancelStableAim(unit);
            return;
        }

        if(!data.aiming){
            unit.lookAt(target);
            if(unit.within(target, ghostStableAimRange)){
                if(unit.energy < ghostStableAimEnergyCost){
                    commandGhostCancelStableAim(unit);
                    return;
                }
                unit.energy = Math.max(0f, unit.energy - ghostStableAimEnergyCost);
                data.aiming = true;
                data.aimTime = 0f;
                data.startHealth = unit.health;
                data.startHitTime = unit.hitTime;
                unit.vel.setZero();
                if(unit.controller() instanceof CommandAI ai){
                    ai.clearCommands();
                }
            }else if(unit.controller() instanceof CommandAI ai){
                ai.command(UnitCommand.moveCommand);
                ai.commandPosition(Tmp.v2.set(target.x, target.y), false);
            }
            return;
        }

        //Channel state: lock beam + immobilized aiming.
        for(WeaponMount mount : unit.mounts){
            mount.shoot = false;
            mount.target = null;
        }
        unit.isShooting = false;
        unit.vel.setZero();
        unit.lookAt(target);

        if(unit.health < data.startHealth - 0.001f || unit.hitTime > data.startHitTime + 0.001f){
            unit.health = Math.max(unit.health, data.startHealth);
            commandGhostCancelStableAim(unit);
            return;
        }

        data.aimTime += Time.delta;
        if(data.aimTime >= ghostStableAimAimTime){
            if(ghostStableAimBullet != null){
                Bullet bullet = ghostStableAimBullet.create(unit, unit.team, unit.x, unit.y, unit.angleTo(target));
                if(bullet != null){
                    bullet.data = target;
                }
            }
            commandGhostCancelStableAim(unit);
        }
    }

    private static void drawGhostStableAimBeam(@Nullable Unit unit){
        Unit target = ghostStableAimTarget(unit);
        if(target == null || !ghostStableAimAiming(unit)) return;

        Draw.z(Layer.effect);
        Draw.color(Color.valueOf("ff2f2f"));
        Lines.stroke(0.625f);
        Lines.line(unit.x, unit.y, target.x, target.y);
        Draw.reset();
    }

    private static @Nullable GhostEmpData getGhostEmpData(@Nullable Unit unit, boolean create){
        if(!isGhost(unit)) return null;
        GhostEmpData data = ghostEmpData.get(unit.id);
        if(data == null && create){
            data = new GhostEmpData();
            ghostEmpData.put(unit.id, data);
        }
        return data;
    }

    public static void clearGhostEmpData(@Nullable Unit unit){
        if(unit == null) return;
        ghostEmpData.remove(unit.id);
    }

    public static boolean ghostEmpPending(@Nullable Unit unit){
        GhostEmpData data = getGhostEmpData(unit, false);
        return data != null && data.active;
    }

    public static boolean ghostCanUseEmp(@Nullable Unit unit){
        return isGhost(unit)
            && !ravenMatrixDisabled(unit)
            && !ghostStableAimPending(unit)
            && !ghostTacticalNukePending(unit)
            && !ghostEmpPending(unit)
            && unit.energy >= ghostEmpEnergyCost;
    }

    public static float ghostEmpRange(){
        return ghostEmpRange;
    }

    public static boolean commandGhostEmp(@Nullable Unit unit, @Nullable Vec2 target){
        if(!ghostCanUseEmp(unit) || target == null) return false;
        GhostEmpData data = getGhostEmpData(unit, true);
        if(data == null) return false;

        data.active = true;
        data.target.set(target);
        return true;
    }

    public static boolean commandGhostCancelEmp(@Nullable Unit unit){
        if(!isGhost(unit)) return false;
        GhostEmpData data = getGhostEmpData(unit, false);
        if(data == null || !data.active) return false;
        data.active = false;
        return true;
    }

    private static void updateGhostEmp(@Nullable Unit unit){
        if(!isGhost(unit)) return;
        GhostEmpData data = getGhostEmpData(unit, false);
        if(data == null || !data.active) return;

        if(ravenMatrixDisabled(unit) || ghostStableAimPending(unit) || ghostTacticalNukePending(unit)){
            commandGhostCancelEmp(unit);
            return;
        }

        if(unit.energy < ghostEmpEnergyCost){
            commandGhostCancelEmp(unit);
            return;
        }

        Vec2 target = data.target;
        boolean facing = faceTargetedAbilityPoint(unit, target.x, target.y);

        if(unit.within(target, ghostEmpRange)){
            holdForTargetedAbility(unit);
            if(!facing) return;

            unit.energy = Math.max(0f, unit.energy - ghostEmpEnergyCost);
            if(ghostEmpBullet != null){
                Bullet bullet = ghostEmpBullet.create(unit, unit.team, unit.x, unit.y, unit.angleTo(target));
                if(bullet != null){
                    bullet.data = new Vec2(target);
                }
            }else{
                impactGhostEmp(unit, target.x, target.y);
            }
            data.active = false;
        }else if(unit.controller() instanceof CommandAI ai){
            ai.command(UnitCommand.moveCommand);
            ai.commandPosition(Tmp.v2.set(target.x, target.y), false);
        }
    }

    private static void impactGhostEmp(@Nullable Unit caster, float x, float y){
        float radius = ghostEmpRadius;
        Units.nearby((Team)null, x - radius, y - radius, radius * 2f, radius * 2f, other -> {
            if(other == null || !other.isValid()) return;
            if(!other.within(x, y, radius + other.hitSize / 2f)) return;

            if(other.shield > 0.001f){
                other.shield = Math.max(0f, other.shield - ghostEmpShieldDamage);
            }

            if(other.type.unitClasses.contains(UnitClass.psionic) && other.type.energyCapacity > 0f){
                other.energy = Math.max(0f, other.energy - ghostEmpPsionicEnergyBurn);
            }

            if(widowIsStealthed(other) || bansheeCloaked(other) || ghostCloaked(other)){
                other.apply(StatusEffects.ghostEmpReveal, ghostEmpRevealDuration);
            }
        });

        if(Shaders.shockwave != null){
            float lensRadius = Math.max(ghostEmpRadius * 0.95f, 8.6f);
            Shaders.shockwave.addLensSphere(x, y, lensRadius, 17f, 0.96f);
        }

        ghostEmpImpactEffect.at(x, y, 0f, Color.valueOf("8ad9ff"));
        Time.run(ghostEmpAfterglowDelay, () -> ghostEmpAfterglowEffect.at(x, y, 0f, Color.valueOf("8ad9ff")));
    }

    private static @Nullable GhostWarheadSiloData getGhostWarheadSiloData(@Nullable Building build, boolean create){
        if(build == null || build.block != Blocks.launchPad) return null;
        int pos = build.pos();
        GhostWarheadSiloData data = ghostWarheadSiloData.get(pos);
        if(data == null && create){
            data = new GhostWarheadSiloData();
            data.buildPos = pos;
            ghostWarheadSiloData.put(pos, data);
        }
        return data;
    }

    private static @Nullable GhostTacticalNukeData getGhostTacticalNukeData(@Nullable Unit unit, boolean create){
        if(!isGhost(unit)) return null;
        GhostTacticalNukeData data = ghostTacticalNukeData.get(unit.id);
        if(data == null && create){
            data = new GhostTacticalNukeData();
            ghostTacticalNukeData.put(unit.id, data);
        }
        return data;
    }

    public static boolean ghostWarheadHasHeavyFactory(@Nullable Team team){
        return team != null && team.data().getCount(Blocks.tankFabricator) > 0;
    }

    private static boolean ghostWarheadCanAfford(@Nullable Team team){
        if(team == null) return false;
        Building core = team.core();
        if(core == null || core.items == null) return false;
        return core.items.has(Items.graphite, ghostWarheadCrystalCost)
            && core.items.has(Items.highEnergyGas, ghostWarheadGasCost);
    }

    private static void ghostWarheadConsume(@Nullable Team team){
        if(team == null) return;
        Building core = team.core();
        if(core == null || core.items == null) return;
        core.items.remove(Items.graphite, ghostWarheadCrystalCost);
        core.items.remove(Items.highEnergyGas, ghostWarheadGasCost);
    }

    public static boolean ghostWarheadProducing(@Nullable Building build){
        GhostWarheadSiloData data = getGhostWarheadSiloData(build, false);
        return data != null && data.producing;
    }

    public static boolean ghostWarheadArmed(@Nullable Building build){
        GhostWarheadSiloData data = getGhostWarheadSiloData(build, false);
        return data != null && data.armed;
    }

    public static float ghostWarheadProductionProgress(@Nullable Building build){
        GhostWarheadSiloData data = getGhostWarheadSiloData(build, false);
        if(data == null || !data.producing) return 0f;
        return Mathf.clamp(data.buildTime / ghostWarheadBuildTime);
    }

    public static boolean ghostWarheadCanStartProduction(@Nullable Building build){
        if(build == null || build.block != Blocks.launchPad) return false;
        if(!ghostWarheadHasHeavyFactory(build.team)) return false;
        GhostWarheadSiloData data = getGhostWarheadSiloData(build, false);
        if(data != null && (data.armed || data.producing)) return false;
        return ghostWarheadCanAfford(build.team);
    }

    public static boolean ghostWarheadStartProduction(@Nullable Building build){
        if(!ghostWarheadCanStartProduction(build)) return false;
        ghostWarheadConsume(build.team);
        GhostWarheadSiloData data = getGhostWarheadSiloData(build, true);
        if(data == null) return false;
        data.producing = true;
        data.armed = false;
        data.buildTime = 0f;
        return true;
    }

    private static int reserveGhostWarhead(@Nullable Team team){
        if(team == null) return -1;
        for(IntMap.Entry<GhostWarheadSiloData> entry : ghostWarheadSiloData.entries()){
            GhostWarheadSiloData data = entry.value;
            if(data == null || !data.armed) continue;
            Building build = world.build(entry.key);
            if(build == null || !build.isValid() || build.block != Blocks.launchPad || build.team != team) continue;
            data.armed = false;
            return entry.key;
        }
        return -1;
    }

    private static void refundReservedGhostWarhead(int teamId, int siloPos){
        if(teamId < 0 || siloPos < 0) return;
        Team team = Team.get(teamId);
        if(team == null) return;
        Building build = world.build(siloPos);
        if(build == null || !build.isValid() || build.block != Blocks.launchPad || build.team != team) return;
        GhostWarheadSiloData data = getGhostWarheadSiloData(build, true);
        if(data == null || data.producing || data.armed) return;
        data.armed = true;
    }

    public static int ghostWarheadCount(@Nullable Team team){
        if(team == null) return 0;
        int count = 0;
        for(IntMap.Entry<GhostWarheadSiloData> entry : ghostWarheadSiloData.entries()){
            GhostWarheadSiloData data = entry.value;
            if(data == null || !data.armed) continue;
            Building build = world.build(entry.key);
            if(build == null || !build.isValid() || build.block != Blocks.launchPad || build.team != team) continue;
            count++;
        }
        return count;
    }

    public static boolean ghostTacticalNukePending(@Nullable Unit unit){
        GhostTacticalNukeData data = getGhostTacticalNukeData(unit, false);
        return data != null && data.active;
    }

    public static boolean ghostCanUseTacticalNuke(@Nullable Unit unit){
        return isGhost(unit)
            && !ravenMatrixDisabled(unit)
            && !ghostTacticalNukePending(unit)
            && !ghostStableAimPending(unit)
            && !ghostEmpPending(unit)
            && ghostWarheadCount(unit.team) > 0;
    }

    public static boolean ghostCanUseTacticalNuke(@Nullable Unit unit, float targetX, float targetY){
        return ghostCanUseTacticalNuke(unit)
            && unit.within(targetX, targetY, ghostTacticalNukeRange);
    }

    public static float ghostTacticalNukeRange(){
        return ghostTacticalNukeRange;
    }

    public static boolean commandGhostTacticalNuke(@Nullable Unit unit, @Nullable Vec2 target){
        if(unit == null || target == null) return false;
        if(!ghostCanUseTacticalNuke(unit)) return false;

        int reservedSilo = reserveGhostWarhead(unit.team);
        if(reservedSilo < 0) return false;

        GhostTacticalNukeData data = getGhostTacticalNukeData(unit, true);
        if(data == null) return false;

        float tx = Mathf.clamp(target.x, 0f, Math.max(world.unitWidth() - tilesize, 0f));
        float ty = Mathf.clamp(target.y, 0f, Math.max(world.unitHeight() - tilesize, 0f));

        data.active = true;
        data.missileFalling = false;
        data.delayTime = ghostTacticalNukeDelay;
        data.missileTime = 0f;
        data.target.set(tx, ty);
        data.teamId = unit.team.id;
        data.reservedSiloPos = reservedSilo;
        unit.lookAt(tx, ty);
        return true;
    }

    public static boolean commandGhostCancelTacticalNuke(@Nullable Unit unit){
        if(!isGhost(unit)) return false;
        GhostTacticalNukeData data = getGhostTacticalNukeData(unit, false);
        if(data == null || !data.active) return false;

        if(!data.missileFalling){
            refundReservedGhostWarhead(data.teamId, data.reservedSiloPos);
        }

        data.active = false;
        data.missileFalling = false;
        data.delayTime = 0f;
        data.missileTime = 0f;
        data.reservedSiloPos = -1;
        return true;
    }

    public static boolean bansheeCloaked(@Nullable Unit unit){
        return isBanshee(unit) && unit.hasEffect(StatusEffects.bansheeCloak);
    }

    public static boolean bansheeCanToggleCloak(@Nullable Unit unit){
        if(!isBanshee(unit)) return false;
        if(bansheeCloaked(unit)) return true;
        return !ravenMatrixDisabled(unit)
            && unit.energy >= bansheeCloakCost
            && bansheeCloakFieldLevel(unit.team) > 0;
    }

    public static boolean commandBansheeCloak(@Nullable Unit unit){
        if(!isBanshee(unit)) return false;
        if(bansheeCloaked(unit)){
            unit.unapply(StatusEffects.bansheeCloak);
            return true;
        }
        if(ravenMatrixDisabled(unit) || unit.energy < bansheeCloakCost || bansheeCloakFieldLevel(unit.team) <= 0) return false;
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

    public static void updateBansheeAfterburner(@Nullable Unit unit){
        if(!isBanshee(unit)) return;
        if(bansheeAfterburnerLevel(unit.team) > 0){
            unit.apply(StatusEffects.bansheeAfterburner, 2f);
        }else{
            unit.unapply(StatusEffects.bansheeAfterburner);
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
        BattlecruiserData data = battlecruiserData.get(unit.id);
        if(data != null && data.warpVisionProbe != null && data.warpVisionProbe.isValid()){
            data.warpVisionProbe.remove();
            data.warpVisionProbe = null;
        }
        battlecruiserData.remove(unit.id);
    }

    public static boolean battlecruiserHasYamatoTech(@Nullable Team team){
        return battlecruiserWeaponRefitLevel(team) > 0;
    }

    public static float battlecruiserYamatoCooldownDuration(){
        return battlecruiserYamatoCooldown;
    }

    public static float battlecruiserWarpCooldownDuration(){
        return battlecruiserWarpCooldown;
    }

    public static float battlecruiserWarpVisionDelay(){
        return battlecruiserWarpVisionDelay;
    }

    public static float battlecruiserWarpVisionRadius(){
        return battlecruiserWarpVisionRadius;
    }

    public static float battlecruiserYamatoCooldown(@Nullable Unit unit){
        if(!isBattlecruiser(unit)) return 0f;
        return getBattlecruiserData(unit).yamatoCooldown;
    }

    public static float battlecruiserWarpCooldown(@Nullable Unit unit){
        if(!isBattlecruiser(unit)) return 0f;
        return getBattlecruiserData(unit).warpCooldown;
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

    public static boolean commandBattlecruiserCancelYamato(@Nullable Unit unit){
        if(!isBattlecruiser(unit)) return false;
        BattlecruiserData data = getBattlecruiserData(unit);
        if(!data.pendingYamato && !data.yamatoCharging && data.yamatoTargetId < 0 && data.yamatoBuildPos < 0) return false;
        data.pendingYamato = false;
        data.yamatoCharging = false;
        data.yamatoChargeTime = 0f;
        data.yamatoTargetId = -1;
        data.yamatoBuildPos = -1;
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

        if(data.pendingWarp || data.warpCharging || data.warping){
            data.warpVisionTime += Time.delta;
        }else{
            data.warpVisionTime = 0f;
        }

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
            data.warpDepartureTime = 0f;
            data.warpDepartureBurstTriggered = false;
            data.warpVisionTime = 0f;
            data.yamatoTargetId = -1;
            data.yamatoBuildPos = -1;
            updateBattlecruiserWarpVision(unit, data);
            return;
        }

        updateBattlecruiserWarpVision(unit, data);

        if(data.warping){
            for(WeaponMount mount : unit.mounts){
                mount.shoot = false;
                mount.target = null;
            }
            unit.isShooting = false;
            unit.vel.setZero();

            if(data.warpDepartureTime > 0f){
                float last = data.warpDepartureTime;
                data.warpDepartureTime = Math.max(0f, data.warpDepartureTime - Time.delta);
                float burstAt = battlecruiserWarpDepartureTime * (1f - battlecruiserWarpDepartureBackPhase);
                if(!data.warpDepartureBurstTriggered && last > burstAt && data.warpDepartureTime <= burstAt){
                    data.warpDepartureBurstTriggered = true;
                    battlecruiserWarpDisintegrateEffect.at(data.warpFrom.x, data.warpFrom.y, data.warpRotation, Color.valueOf("54ff8b"), Float.valueOf(unit.hitSize));
                }
            }

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
                data.warpDepartureTime = 0f;
                data.warpAppearTime = 0f;
                data.warpVisionTime = 0f;
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
                data.warpDepartureTime = battlecruiserWarpDepartureTime;
                data.warpDepartureBurstTriggered = false;
                if(Shaders.shockwave != null){
                    float lensLife = Math.max(1f, battlecruiserWarpDepartureTime * battlecruiserWarpDepartureBackPhase);
                    Shaders.shockwave.addLensSphere(unit.x, unit.y, Math.max(unit.hitSize * 0.58f, 13f), lensLife, 0.92f);
                }
            }
            return;
        }

        if(data.pendingWarp){
            holdForTargetedAbility(unit);
            if(!faceTargetedAbilityPoint(unit, data.warpTarget.x, data.warpTarget.y)) return;

            data.pendingWarp = false;
            data.pendingYamato = false;
            data.yamatoCharging = false;
            data.yamatoTargetId = -1;
            data.yamatoBuildPos = -1;
            data.warpChargeTime = 0f;
            data.warpCharging = true;
            data.warpCooldown = battlecruiserWarpCooldown;
            data.warpRippleTriggered = false;
            data.warpDepartureTime = 0f;
            data.warpDepartureBurstTriggered = false;
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

            rotateBattlecruiserWeapons(unit, yamatoTarget);
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

    private static void updateBattlecruiserWarpVision(Unit unit, BattlecruiserData data){
        if(net.client()) return;

        boolean active = data.pendingWarp || data.warpCharging || data.warping;
        if(!active || data.warpVisionTime < battlecruiserWarpVisionDelay){
            if(data.warpVisionProbe != null && data.warpVisionProbe.isValid()){
                data.warpVisionProbe.remove();
            }
            data.warpVisionProbe = null;
            return;
        }

        if(data.warpVisionProbe == null || !data.warpVisionProbe.isValid()){
            Unit probe = warpProbe.create(unit.team());
            probe.set(data.warpTarget.x, data.warpTarget.y);
            probe.add();
            data.warpVisionProbe = probe;
        }else{
            data.warpVisionProbe.set(data.warpTarget.x, data.warpTarget.y);
        }
    }

    private static void rotateBattlecruiserWeapons(Unit unit, Teamc target){
        for(WeaponMount mount : unit.mounts){
            Weapon weapon = mount.weapon;
            mount.shoot = false;
            mount.target = null;
            if(!weapon.rotate){
                mount.rotate = false;
                continue;
            }

            mount.rotate = true;
            float mountX = unit.x + Angles.trnsx(unit.rotation - 90f, weapon.x, weapon.y);
            float mountY = unit.y + Angles.trnsy(unit.rotation - 90f, weapon.x, weapon.y);
            Units.aimPoint(target, mountX, mountY, target.getX(), target.getY(), Tmp.v1);
            mount.aimX = Tmp.v1.x;
            mount.aimY = Tmp.v1.y;
            mount.targetRotation = Angles.angle(mountX, mountY, mount.aimX, mount.aimY) - unit.rotation;
            mount.rotation = Angles.moveToward(mount.rotation, mount.targetRotation, weapon.rotateSpeed * Time.delta);
        }
    }

    private static void drawBattlecruiserWarpGhost(Unit unit, BattlecruiserData data){
        Draw.z(Math.max(Layer.flyingUnit, unit.type.flyingLayer) + 0.4f);
        float fin = Mathf.clamp(1f - data.warpTransitTime / battlecruiserWarpTransitTime);
        TextureRegion ghostRegion = unit.type.region != null && unit.type.region.found() ? unit.type.region : unit.type.fullIcon;

        // target-point ghost starts immediately when entering warp space: back -> front scan reveal
        float targetScan = Mathf.clamp(fin / 0.18f);
        float targetFade = Mathf.clamp(1f - Math.max(0f, fin - 0.82f) / 0.18f);
        if(targetFade > 0.001f){
            queueBattlecruiserTargetGhostAfterDraw(unit, data.warpTarget.x, data.warpTarget.y, data.warpRotation, targetScan, targetFade);
        }

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

    private static void drawBattlecruiserTargetScanGhost(Unit unit, float x, float y, float rotation, float scan, float fade){
        TextureRegion bodyRegion = unit.type.region != null && unit.type.region.found() ? unit.type.region : unit.type.fullIcon;
        TextureRegion outlineRegion = unit.type.outlineRegion != null && unit.type.outlineRegion.found() ? unit.type.outlineRegion : bodyRegion;
        TextureRegion cellRegion = unit.type.cellRegion != null && unit.type.cellRegion.found() ? unit.type.cellRegion : bodyRegion;
        if(bodyRegion == null || !bodyRegion.found()) return;

        float z = Math.max(Layer.flyingUnit, unit.type.flyingLayer) + 0.36f;
        float a = Mathf.clamp(fade);
        Color ghostColor = Tmp.c1.set(unit.team.color).lerp(Color.white, 0.45f);

        Draw.z(z);
        if(scan >= 0.999f){
            // after scan completes: transparent interior + team-color edge + tiled scanlines (masked by alpha)
            drawBattlecruiserTargetPostScanGhost(bodyRegion, x, y, rotation, ghostColor, 0.72f * a);
            drawBattlecruiserTargetPostScanGhost(outlineRegion, x, y, rotation, ghostColor, 0.84f * a);
            drawBattlecruiserTargetPostScanGhost(cellRegion, x, y, rotation, ghostColor, 0.42f * a);
        }else{
            drawBattlecruiserRearRevealSliceScaled(bodyRegion, x, y, rotation, scan, 1f, 1f, ghostColor, 0.58f * a);
            drawBattlecruiserRearRevealSliceScaled(outlineRegion, x, y, rotation, scan, 1f, 1f, ghostColor, 0.68f * a);
            drawBattlecruiserRearRevealSliceScaled(cellRegion, x, y, rotation, scan, 1f, 1f, ghostColor, 0.34f * a);
        }

        Draw.reset();
    }

    private static void drawBattlecruiserTargetPostScanGhost(TextureRegion source, float x, float y, float rotation, Color color, float alpha){
        if(source == null || !source.found() || alpha <= 0.001f) return;

        float drawW = battlecruiserRegionWidth(source);
        float drawH = battlecruiserRegionHeight(source);
        if(drawW <= 0.001f || drawH <= 0.001f) return;

        float rot = rotation - 90f;
        if(Shaders.unitGhost != null){
            Shaders.unitGhost.region = source;
            Shaders.unitGhost.mode = 1f;
            Shaders.unitGhost.color.set(color);
            Shaders.unitGhost.color.a = alpha;
            Shaders.unitGhost.time = Time.time / 60f;
            // denser than 1-tile spacing
            Shaders.unitGhost.lineStep = Mathf.clamp(tilesize * 0.55f / drawH, 0.008f, 0.32f);
            Shaders.unitGhost.lineWidth = 0.06f;
            Draw.shader(Shaders.unitGhost);
            Draw.rect(source, x, y, drawW, drawH, rot);
            Draw.shader();
        }else{
            Draw.color(color.r, color.g, color.b, alpha * 0.28f);
            Draw.rect(source, x, y, drawW, drawH, rot);
            Draw.color();
        }
    }

    private static void drawBattlecruiserWarpDeparture(Unit unit, BattlecruiserData data){
        if(data.warpDepartureTime <= 0.001f) return;

        TextureRegion bodyRegion = unit.type.region != null && unit.type.region.found() ? unit.type.region : unit.type.fullIcon;
        TextureRegion outlineRegion = unit.type.outlineRegion != null && unit.type.outlineRegion.found() ? unit.type.outlineRegion : bodyRegion;
        TextureRegion cellRegion = unit.type.cellRegion != null && unit.type.cellRegion.found() ? unit.type.cellRegion : bodyRegion;
        if(bodyRegion == null || !bodyRegion.found()) return;

        float fin = Mathf.clamp(1f - data.warpDepartureTime / battlecruiserWarpDepartureTime);
        float split = battlecruiserWarpDepartureBackPhase;
        float back = Mathf.clamp(fin / Math.max(split, 0.001f));
        float forward = Mathf.clamp((fin - split) / Math.max(1f - split, 0.001f));

        float lengthScale;
        float widthScale;
        float alpha;

        if(fin <= split){
            float t = Interp.pow3Out.apply(back);
            lengthScale = Mathf.lerp(1f, 0.78f, t);
            // keep departure phase visually within the new 0.8x body width baseline
            widthScale = Mathf.lerp(1f, 0.9f, t);
            alpha = 1f;
            drawBattlecruiserDepartureSphereFog(unit, data.warpFrom.x, data.warpFrom.y, t);
        }else{
            float t = Interp.pow2Out.apply(forward);
            lengthScale = Mathf.lerp(0.78f, 1.42f, t);
            widthScale = Mathf.lerp(0.9f, 0.62f, t);
            alpha = Mathf.clamp(1f - t * 1.14f);
        }

        float baseW = battlecruiserRegionWidth(bodyRegion);
        float baseH = battlecruiserRegionHeight(bodyRegion);
        float moveForward;
        if(fin <= split){
            float t = Interp.pow2Out.apply(back);
            moveForward = Mathf.lerp(0f, baseH * 0.18f, t);
        }else{
            float t = Interp.pow2Out.apply(forward);
            moveForward = Mathf.lerp(baseH * 0.18f, baseH * 0.36f, t);
        }

        float along = (lengthScale - 1f) * baseH * 0.5f + moveForward;
        float x = data.warpFrom.x + Angles.trnsx(data.warpRotation, along);
        float y = data.warpFrom.y + Angles.trnsy(data.warpRotation, along);
        float rot = data.warpRotation - 90f;
        float z = Math.max(Layer.flyingUnit, unit.type.flyingLayer) + 0.45f;

        Draw.z(z);
        if(fin <= split){
            float scan = Interp.pow2Out.apply(back);

            //unscanned back section keeps normal rendering
            drawBattlecruiserBackSliceScaled(bodyRegion, x, y, data.warpRotation, scan, widthScale, lengthScale, alpha);
            drawBattlecruiserBackSliceScaled(outlineRegion, x, y, data.warpRotation, scan, widthScale, lengthScale, alpha * 0.95f);
            drawBattlecruiserBackSliceScaled(cellRegion, x, y, data.warpRotation, scan, widthScale, lengthScale, alpha * 0.36f);

            //front scanned section turns green + transparent
            float scannedAlpha = alpha * Mathf.lerp(0.14f, 0.34f, scan);
            Draw.mixcol(Color.valueOf("54ff8b"), 0.88f);
            drawBattlecruiserFrontSliceScaled(bodyRegion, x, y, data.warpRotation, scan, widthScale, lengthScale, scannedAlpha);
            drawBattlecruiserFrontSliceScaled(outlineRegion, x, y, data.warpRotation, scan, widthScale, lengthScale, scannedAlpha * 1.05f);
            drawBattlecruiserFrontSliceScaled(cellRegion, x, y, data.warpRotation, scan, widthScale, lengthScale, scannedAlpha * 0.92f);
            Draw.mixcol();
        }else{
            //after full scan, keep only a thin green transparent shell before final disappear
            Draw.mixcol(Color.valueOf("54ff8b"), 0.9f);
            Draw.color(1f, 1f, 1f, alpha * 0.34f);
            Draw.rect(bodyRegion, x, y, baseW * widthScale, baseH * lengthScale, rot);
            Draw.color(1f, 1f, 1f, alpha * 0.33f);
            Draw.rect(outlineRegion, x, y, battlecruiserRegionWidth(outlineRegion) * widthScale, battlecruiserRegionHeight(outlineRegion) * lengthScale, rot);
            Draw.color(1f, 1f, 1f, alpha * 0.20f);
            Draw.rect(cellRegion, x, y, battlecruiserRegionWidth(cellRegion) * widthScale, battlecruiserRegionHeight(cellRegion) * lengthScale, rot);
            Draw.mixcol();
        }
        Draw.reset();
    }

    private static void drawBattlecruiserDepartureSphereFog(Unit unit, float x, float y, float fin){
        float p = Mathf.clamp(fin);
        float fade = Mathf.clamp(1f - p * 0.95f);
        if(fade <= 0.001f) return;

        TextureRegion softCircle = Core.atlas.find("circle-shadow", "circle");
        if(!softCircle.found()) return;

        float sphere = Mathf.lerp(Math.max(unit.hitSize * 0.42f, 8f), Math.max(unit.hitSize * 0.65f, 14f), Interp.pow2Out.apply(p));
        Draw.z(Math.max(Layer.flyingUnit, unit.type.flyingLayer) + 0.3f);
        Draw.blend(Blending.additive);
        Draw.color(0.22f, 1f, 0.52f, (0.08f + 0.16f * (1f - p)) * fade);
        Draw.rect(softCircle, x, y, sphere * 2f, sphere * 2f);

        int fogCount = Math.max(10, (int)Mathf.lerp(10f, 28f, p));
        float fogRadius = Mathf.lerp(Math.max(unit.hitSize * 0.26f, 4f), Math.max(unit.hitSize * 0.56f, 10f), p);
        Fx.rand.setSeed(((long)unit.id << 32) ^ ((long)(Time.time * 10f) * 131L + 73L));
        for(int i = 0; i < fogCount; i++){
            float ang = Fx.rand.random(360f);
            float rr = fogRadius * Mathf.sqrt(Fx.rand.random(1f));
            float px = x + Angles.trnsx(ang, rr);
            float py = y + Angles.trnsy(ang, rr);
            float size = Fx.rand.random(0.35f, 1.18f) * (0.65f + (1f - rr / Math.max(fogRadius, 0.001f)) * 0.75f);
            float a = (0.02f + 0.09f * (1f - rr / Math.max(fogRadius, 0.001f))) * fade * Fx.rand.random(0.55f, 1f);

            Draw.color(0.30f, 1f, 0.54f, a);
            Fill.circle(px, py, size);
            if((i & 3) == 0){
                Draw.color(0.62f, 1f, 0.82f, a * 0.45f);
                Fill.circle(px, py, size * 0.45f);
            }
        }

        Draw.blend();
        Draw.reset();
    }

    private static float battlecruiserRegionWidth(TextureRegion region){
        return region.width * region.scale / 4f * battlecruiserTextureXScale;
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

    private static void drawBattlecruiserFrontSliceScaled(TextureRegion source, float x, float y, float rotation, float reveal, float widthScale, float lengthScale, float alpha){
        if(source == null || !source.found()) return;
        if(reveal <= 0.001f || alpha <= 0.001f) return;

        float drawW = battlecruiserRegionWidth(source) * widthScale;
        float drawH = battlecruiserRegionHeight(source) * lengthScale;
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

            float edgeForward = (0.5f - r) * drawH;
            float ex = x + Angles.trnsx(rotation, edgeForward);
            float ey = y + Angles.trnsy(rotation, edgeForward);
            Draw.blend(Blending.additive);
            Draw.color(0.40f, 1f, 0.46f, alpha * (0.36f + 0.4f * (1f - r)));
            Lines.stroke(1.1f);
            Lines.lineAngleCenter(ex, ey, rotation + 90f, drawW * 0.96f);
            Draw.blend();
            Draw.color();
        }
        Draw.color();
    }

    private static void drawBattlecruiserRearRevealSliceScaled(TextureRegion source, float x, float y, float rotation, float reveal, float widthScale, float lengthScale, Color color, float alpha){
        if(source == null || !source.found()) return;
        if(reveal <= 0.001f || alpha <= 0.001f) return;

        float drawW = battlecruiserRegionWidth(source) * widthScale;
        float drawH = battlecruiserRegionHeight(source) * lengthScale;
        if(drawW <= 0.001f || drawH <= 0.001f) return;

        float r = Mathf.clamp(reveal);
        float rot = rotation - 90f;

        TextureRegion drawRegion = source;
        float outW = drawW;
        float outH = drawH;
        float px = x;
        float py = y;

        if(r < 0.999f){
            // build from rear to front
            float vEdge = Mathf.lerp(source.v2, source.v, r);
            Tmp.tr1.set(source);
            Tmp.tr1.set(source.u, vEdge, source.u2, source.v2);
            drawRegion = Tmp.tr1;
            outH = drawH * r;
            px = x + Angles.trnsx(rotation, (-0.5f + r * 0.5f) * drawH);
            py = y + Angles.trnsy(rotation, (-0.5f + r * 0.5f) * drawH);
        }

        // pure-color alpha-mask ghost: no base texture detail, no build stripes
        if(Shaders.unitGhost != null){
            Shaders.unitGhost.region = drawRegion;
            Shaders.unitGhost.mode = 0f;
            Shaders.unitGhost.time = Time.time / 60f;
            Shaders.unitGhost.color.set(color);
            Shaders.unitGhost.color.a = alpha;
            Draw.shader(Shaders.unitGhost);
            Draw.rect(drawRegion, px, py, outW, outH, rot);
            Draw.shader();
        }else{
            Draw.color(color.r, color.g, color.b, alpha);
            Draw.rect(drawRegion, px, py, outW, outH, rot);
            Draw.color();
        }
    }

    private static void drawBattlecruiserBackSliceScaled(TextureRegion source, float x, float y, float rotation, float reveal, float widthScale, float lengthScale, float alpha){
        if(source == null || !source.found()) return;
        if(alpha <= 0.001f) return;

        float drawW = battlecruiserRegionWidth(source) * widthScale;
        float drawH = battlecruiserRegionHeight(source) * lengthScale;
        if(drawW <= 0.001f || drawH <= 0.001f) return;

        float r = Mathf.clamp(reveal);
        if(r >= 0.999f) return;

        float rot = rotation - 90f;
        Draw.color(1f, 1f, 1f, alpha);
        if(r <= 0.001f){
            Draw.rect(source, x, y, drawW, drawH, rot);
        }else{
            float vEdge = Mathf.lerp(source.v, source.v2, r);
            Tmp.tr1.set(source);
            Tmp.tr1.set(source.u, vEdge, source.u2, source.v2);

            float remain = 1f - r;
            float localForward = -r * 0.5f * drawH;
            float px = x + Angles.trnsx(rotation, localForward);
            float py = y + Angles.trnsy(rotation, localForward);
            Draw.rect(Tmp.tr1, px, py, drawW, drawH * remain, rot);
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
            drawBattlecruiserWarpDeparture(unit, data);
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
                float bodyW = battlecruiserRegionWidth(bodyRegion);
                float bodyH = battlecruiserRegionHeight(bodyRegion);
                Draw.color(0.36f, 0.62f, 1f, 0.16f);
                Draw.rect(bodyRegion, unit.x, unit.y, bodyW, bodyH, unit.rotation - 90f);
                Draw.color(0.52f, 0.74f, 1f, 0.1f);
                Draw.rect(bodyRegion, unit.x, unit.y, bodyW, bodyH, unit.rotation - 90f);

                TextureRegion edge = unit.type.outlineRegion != null && unit.type.outlineRegion.found() ? unit.type.outlineRegion : bodyRegion;
                float edgeW = battlecruiserRegionWidth(edge);
                float edgeH = battlecruiserRegionHeight(edge);
                Draw.color(0.58f, 0.8f, 1f, 0.36f);
                Draw.rect(edge, unit.x, unit.y, edgeW, edgeH, unit.rotation - 90f);
            }
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

    private static void updateMedivacCaduceusReactor(@Nullable Unit unit){
        if(!isMedivac(unit) || unit == null) return;
        if(medivacCaduceusReactorLevel(unit.team) <= 0) return;
        float capacity = unit.type.energyCapacity;
        if(unit.energy >= capacity - 0.001f) return;
        unit.energy = Math.min(capacity, unit.energy + unit.type.energyRegen * Time.delta);
    }

    public static boolean isBarracksStimpackUnit(@Nullable Unit unit){
        if(unit == null || !unit.isValid()) return false;
        return unit.type == dagger || unit.type == fortress;
    }

    public static float barracksStimpackCooldown(@Nullable Unit unit){
        if(!isBarracksStimpackUnit(unit)) return 0f;
        return Math.max(0f, barracksStimpackCooldowns.get(unit.id, 0f));
    }

    public static float barracksStimpackCooldownDuration(){
        return barracksStimpackCooldown;
    }

    public static float barracksStimpackDuration(){
        return barracksStimpackDuration;
    }

    public static float barracksStimpackHealthCost(@Nullable Unit unit){
        if(unit == null) return barracksStimpackMarineHealthCost;
        return unit.type == fortress ? barracksStimpackMarauderHealthCost : barracksStimpackMarineHealthCost;
    }

    public static boolean barracksStimpackCanUse(@Nullable Unit unit){
        if(!isBarracksStimpackUnit(unit)) return false;
        if(ravenMatrixDisabled(unit)) return false;
        if(barracksStimpackLevel(unit.team) <= 0) return false;
        if(barracksStimpackCooldown(unit) > 0.001f) return false;

        float healthCost = barracksStimpackHealthCost(unit);
        return unit.health() > healthCost;
    }

    public static boolean commandBarracksStimpack(@Nullable Unit unit){
        if(!barracksStimpackCanUse(unit)) return false;

        float healthCost = barracksStimpackHealthCost(unit);
        float nextHealth = unit.health() - healthCost;
        if(nextHealth <= 0f) return false;

        unit.health(nextHealth);
        if(unit.type == dagger){
            unit.apply(StatusEffects.barracksStimpackMarine, barracksStimpackDuration);
        }else{
            unit.apply(StatusEffects.barracksStimpackMarauder, barracksStimpackDuration);
        }
        barracksStimpackCooldowns.put(unit.id, barracksStimpackCooldown);
        return true;
    }

    private static boolean barracksStimpackActive(@Nullable Unit unit){
        return isBarracksStimpackUnit(unit) && (unit.hasEffect(StatusEffects.barracksStimpackMarine) || unit.hasEffect(StatusEffects.barracksStimpackMarauder));
    }

    private static TextureRegion barracksStimpackLightRegion(){
        if(barracksStimpackLightRegion == null){
            barracksStimpackLightRegion = Core.atlas.find("status-shocked");
        }
        return barracksStimpackLightRegion;
    }

    private static void updateBarracksStimpackFlashMap(IntFloatMap timers){
        IntSeq remove = new IntSeq();
        IntSeq updateKeys = new IntSeq();
        FloatSeq updateValues = new FloatSeq();

        for(IntFloatMap.Entry entry : timers.entries()){
            float next = Math.max(0f, entry.value - Time.delta);
            if(next <= 0.001f){
                remove.add(entry.key);
            }else{
                updateKeys.add(entry.key);
                updateValues.add(next);
            }
        }

        for(int i = 0; i < updateKeys.size; i++){
            timers.put(updateKeys.get(i), updateValues.get(i));
        }
        for(int i = 0; i < remove.size; i++){
            timers.remove(remove.get(i), 0f);
        }
    }

    private static void updateBarracksStimpackVisuals(){
        IntSeq removeActive = new IntSeq();
        for(IntFloatMap.Entry entry : barracksStimpackActiveUnits.entries()){
            int unitId = entry.key;
            Unit unit = Groups.unit.getByID(unitId);
            if(unit == null || !unit.isValid() || !barracksStimpackActive(unit)){
                if(unit != null && unit.isValid() && isBarracksStimpackUnit(unit)){
                    barracksStimpackEndFlashes.put(unitId, barracksStimpackFlashDuration);
                }
                removeActive.add(unitId);
            }
        }

        for(int i = 0; i < removeActive.size; i++){
            barracksStimpackActiveUnits.remove(removeActive.get(i), 0f);
        }

        for(Unit unit : Groups.unit){
            if(unit == null || !unit.isValid() || !barracksStimpackActive(unit)) continue;
            if(barracksStimpackActiveUnits.get(unit.id, 0f) > 0.5f) continue;

            barracksStimpackActiveUnits.put(unit.id, 1f);
            barracksStimpackStartFlashes.put(unit.id, barracksStimpackFlashDuration);
            barracksStimpackEndFlashes.remove(unit.id, 0f);
        }

        updateBarracksStimpackFlashMap(barracksStimpackStartFlashes);
        updateBarracksStimpackFlashMap(barracksStimpackEndFlashes);
    }

    private static void drawBarracksStimpackFlash(@Nullable Unit unit){
        if(!isBarracksStimpackUnit(unit) || unit == null) return;

        float start = barracksStimpackStartFlashes.get(unit.id, 0f);
        float end = barracksStimpackEndFlashes.get(unit.id, 0f);
        if(start <= 0.001f && end <= 0.001f) return;

        boolean showStart = start >= end;
        float remaining = showStart ? start : end;
        float alpha = Mathf.clamp(remaining / barracksStimpackFlashDuration);
        float fin = 1f - alpha;
        TextureRegion region = barracksStimpackLightRegion();
        if(region == null || !region.found()) return;

        float size = Math.max(unit.hitSize * 0.8f, 8f);
        float y = unit.y + unit.hitSize * 0.72f + fin * 2.5f;

        Draw.z(Layer.effect + 0.11f);
        Draw.color(showStart ? Color.valueOf("66ff66") : Color.valueOf("ff5a5a"), alpha * 0.95f);
        Draw.rect(region, unit.x, y, size, size, 0f);
        Draw.reset();
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

    private static void ensureInfantryUpgradeHooks(){
        if(infantryUpgradeHooksInitialized) return;
        infantryUpgradeHooksInitialized = true;

        Events.run(Trigger.update, UnitTypes::updateInfantryUpgrades);
        Events.run(Trigger.uiDrawBegin, UnitTypes::drawGhostTacticalNukeOverlay);
        Events.on(WorldLoadEvent.class, e -> {
            Sc2State.clearTechState();
            Sc2State.clearTransformState();
            ghostWarheadSiloData.clear();
            ghostTacticalNukeData.clear();
            ghostStableAimData.clear();
            ghostEmpData.clear();
        });
    }

    private static @Nullable InfantryWeaponData getInfantryWeaponData(@Nullable Team team, boolean create){
        if(team == null) return null;
        InfantryWeaponData data = infantryWeaponData.get(team.id);
        if(data == null && create){
            data = new InfantryWeaponData();
            infantryWeaponData.put(team.id, data);
        }
        return data;
    }

    private static @Nullable VehicleWeaponData getVehicleWeaponData(@Nullable Team team, boolean create){
        if(team == null) return null;
        VehicleWeaponData data = vehicleWeaponData.get(team.id);
        if(data == null && create){
            data = new VehicleWeaponData();
            vehicleWeaponData.put(team.id, data);
        }
        return data;
    }

    private static @Nullable ShipWeaponData getShipWeaponData(@Nullable Team team, boolean create){
        if(team == null) return null;
        ShipWeaponData data = shipWeaponData.get(team.id);
        if(data == null && create){
            data = new ShipWeaponData();
            shipWeaponData.put(team.id, data);
        }
        return data;
    }

    private static @Nullable VehicleArmorData getVehicleArmorData(@Nullable Team team, boolean create){
        if(team == null) return null;
        VehicleArmorData data = vehicleArmorData.get(team.id);
        if(data == null && create){
            data = new VehicleArmorData();
            vehicleArmorData.put(team.id, data);
        }
        return data;
    }

    private static @Nullable InfantryArmorData getInfantryArmorData(@Nullable Team team, boolean create){
        if(team == null) return null;
        InfantryArmorData data = infantryArmorData.get(team.id);
        if(data == null && create){
            data = new InfantryArmorData();
            infantryArmorData.put(team.id, data);
        }
        return data;
    }

    private static @Nullable InstantTrackingData getInstantTrackingData(@Nullable Team team, boolean create){
        if(team == null) return null;
        InstantTrackingData data = instantTrackingData.get(team.id);
        if(data == null && create){
            data = new InstantTrackingData();
            instantTrackingData.put(team.id, data);
        }
        return data;
    }

    private static @Nullable SteelArmorData getSteelArmorData(@Nullable Team team, boolean create){
        if(team == null) return null;
        SteelArmorData data = steelArmorData.get(team.id);
        if(data == null && create){
            data = new SteelArmorData();
            steelArmorData.put(team.id, data);
        }
        return data;
    }

    private static @Nullable GhostCamoData getGhostCamoData(@Nullable Team team, boolean create){
        if(team == null) return null;
        GhostCamoData data = ghostCamoData.get(team.id);
        if(data == null && create){
            data = new GhostCamoData();
            ghostCamoData.put(team.id, data);
        }
        return data;
    }

    private static @Nullable BansheeCloakFieldData getBansheeCloakFieldData(@Nullable Team team, boolean create){
        if(team == null) return null;
        BansheeCloakFieldData data = bansheeCloakFieldData.get(team.id);
        if(data == null && create){
            data = new BansheeCloakFieldData();
            bansheeCloakFieldData.put(team.id, data);
        }
        return data;
    }

    private static @Nullable BansheeAfterburnerData getBansheeAfterburnerData(@Nullable Team team, boolean create){
        if(team == null) return null;
        BansheeAfterburnerData data = bansheeAfterburnerData.get(team.id);
        if(data == null && create){
            data = new BansheeAfterburnerData();
            bansheeAfterburnerData.put(team.id, data);
        }
        return data;
    }

    private static @Nullable RavenMatrixTechData getRavenMatrixTechData(@Nullable Team team, boolean create){
        if(team == null) return null;
        RavenMatrixTechData data = ravenMatrixTechData.get(team.id);
        if(data == null && create){
            data = new RavenMatrixTechData();
            ravenMatrixTechData.put(team.id, data);
        }
        return data;
    }

    private static @Nullable BattlecruiserWeaponRefitData getBattlecruiserWeaponRefitData(@Nullable Team team, boolean create){
        if(team == null) return null;
        BattlecruiserWeaponRefitData data = battlecruiserWeaponRefitData.get(team.id);
        if(data == null && create){
            data = new BattlecruiserWeaponRefitData();
            battlecruiserWeaponRefitData.put(team.id, data);
        }
        return data;
    }

    private static @Nullable MedivacCaduceusReactorData getMedivacCaduceusReactorData(@Nullable Team team, boolean create){
        if(team == null) return null;
        MedivacCaduceusReactorData data = medivacCaduceusReactorData.get(team.id);
        if(data == null && create){
            data = new MedivacCaduceusReactorData();
            medivacCaduceusReactorData.put(team.id, data);
        }
        return data;
    }

    private static @Nullable LiberatorAdvancedBallisticsData getLiberatorAdvancedBallisticsData(@Nullable Team team, boolean create){
        if(team == null) return null;
        LiberatorAdvancedBallisticsData data = liberatorAdvancedBallisticsData.get(team.id);
        if(data == null && create){
            data = new LiberatorAdvancedBallisticsData();
            liberatorAdvancedBallisticsData.put(team.id, data);
        }
        return data;
    }

    private static @Nullable BarracksBlastShieldData getBarracksBlastShieldData(@Nullable Team team, boolean create){
        if(team == null) return null;
        BarracksBlastShieldData data = barracksBlastShieldData.get(team.id);
        if(data == null && create){
            data = new BarracksBlastShieldData();
            barracksBlastShieldData.put(team.id, data);
        }
        return data;
    }

    private static @Nullable BarracksStimpackData getBarracksStimpackData(@Nullable Team team, boolean create){
        if(team == null) return null;
        BarracksStimpackData data = barracksStimpackData.get(team.id);
        if(data == null && create){
            data = new BarracksStimpackData();
            barracksStimpackData.put(team.id, data);
        }
        return data;
    }

    private static @Nullable BarracksConcussiveData getBarracksConcussiveData(@Nullable Team team, boolean create){
        if(team == null) return null;
        BarracksConcussiveData data = barracksConcussiveData.get(team.id);
        if(data == null && create){
            data = new BarracksConcussiveData();
            barracksConcussiveData.put(team.id, data);
        }
        return data;
    }

    private static @Nullable InfernoPreheaterData getInfernoPreheaterData(@Nullable Team team, boolean create){
        if(team == null) return null;
        InfernoPreheaterData data = infernoPreheaterData.get(team.id);
        if(data == null && create){
            data = new InfernoPreheaterData();
            infernoPreheaterData.put(team.id, data);
        }
        return data;
    }

    private static @Nullable ElectromagneticFieldAcceleratorData getElectromagneticFieldAcceleratorData(@Nullable Team team, boolean create){
        if(team == null) return null;
        ElectromagneticFieldAcceleratorData data = electromagneticFieldAcceleratorData.get(team.id);
        if(data == null && create){
            data = new ElectromagneticFieldAcceleratorData();
            electromagneticFieldAcceleratorData.put(team.id, data);
        }
        return data;
    }

    private static @Nullable DrillClawData getDrillClawData(@Nullable Team team, boolean create){
        if(team == null) return null;
        DrillClawData data = drillClawData.get(team.id);
        if(data == null && create){
            data = new DrillClawData();
            drillClawData.put(team.id, data);
        }
        return data;
    }

    private static @Nullable SmartServosData getSmartServosData(@Nullable Team team, boolean create){
        if(team == null) return null;
        SmartServosData data = smartServosData.get(team.id);
        if(data == null && create){
            data = new SmartServosData();
            smartServosData.put(team.id, data);
        }
        return data;
    }

    private static int encodeQueueCode(int type, int level){
        return (type << 8) | (level & 0xff);
    }

    private static int queueCodeType(int code){
        return (code >>> 8) & 0xff;
    }

    private static int queueCodeLevel(int code){
        return code & 0xff;
    }

    private static @Nullable IntSeq getResearchQueue(IntMap<IntSeq> queues, @Nullable Team team, boolean create){
        if(team == null) return null;
        IntSeq queue = queues.get(team.id);
        if(queue == null && create){
            queue = new IntSeq();
            queues.put(team.id, queue);
        }
        return queue;
    }

    private static void trimEmptyQueue(IntMap<IntSeq> queues, @Nullable Team team){
        if(team == null) return;
        IntSeq queue = queues.get(team.id);
        if(queue != null && queue.size == 0){
            queues.remove(team.id);
        }
    }

    private static boolean startSingleResearchWithQueue(@Nullable Team team, Boolf<Team> canStart, Cons<Team> consume, Boolf<Team> activeResearching, IntMap<IntSeq> queueMap, int queueCode, Boolf<Team> startNow, Cons<Team> refund){
        if(!canStart.get(team)) return false;
        consume.get(team);

        IntSeq queue = getResearchQueue(queueMap, team, false);
        if(activeResearching.get(team) || (queue != null && queue.size > 0)){
            if(queue == null){
                queue = getResearchQueue(queueMap, team, true);
                if(queue == null){
                    refund.get(team);
                    return false;
                }
            }
            queue.add(queueCode);
            return true;
        }

        if(startNow.get(team)) return true;
        refund.get(team);
        return false;
    }

    private static boolean startLeveledResearchWithQueue(@Nullable Team team, int level, Func2<Team, Integer, Boolean> canStart, Cons2<Team, Integer> consume, Boolf<Team> activeResearching, IntMap<IntSeq> queueMap, int queueType, Func2<Team, Integer, Boolean> startNow, Cons2<Team, Integer> refund){
        if(!canStart.get(team, level)) return false;
        consume.get(team, level);

        IntSeq queue = getResearchQueue(queueMap, team, false);
        if(activeResearching.get(team) || (queue != null && queue.size > 0)){
            if(queue == null){
                queue = getResearchQueue(queueMap, team, true);
                if(queue == null){
                    refund.get(team, level);
                    return false;
                }
            }
            queue.add(encodeQueueCode(queueType, level));
            return true;
        }

        if(startNow.get(team, level)) return true;
        refund.get(team, level);
        return false;
    }

    private static <T> boolean cancelLeveledResearch(@Nullable Team team, @Nullable T data, Sc2TechService.IntGetter<T> researchingLevel, Sc2TechService.IntSetter<T> setResearchingLevel, Sc2TechService.FloatSetter<T> setResearchTime, Cons2<Team, Integer> refund){
        if(team == null || data == null) return false;
        int level = researchingLevel.get(data);
        if(level <= 0) return false;
        setResearchingLevel.set(data, 0);
        setResearchTime.set(data, 0f);
        refund.get(team, level);
        return true;
    }

    private static <T> boolean cancelSingleResearch(@Nullable Team team, @Nullable T data, Sc2TechService.BoolGetter<T> researching, Sc2TechService.BoolSetter<T> setResearching, Sc2TechService.FloatSetter<T> setResearchTime, Cons<Team> refund){
        if(team == null || data == null || !researching.get(data)) return false;
        setResearching.set(data, false);
        setResearchTime.set(data, 0f);
        refund.get(team);
        return true;
    }

    private static int highestQueuedLevel(@Nullable IntSeq queue, int type){
        if(queue == null || queue.size == 0) return 0;
        int result = 0;
        for(int i = 0; i < queue.size; i++){
            int code = queue.get(i);
            if(queueCodeType(code) != type) continue;
            result = Math.max(result, queueCodeLevel(code));
        }
        return result;
    }

    private static boolean queueContainsType(@Nullable IntSeq queue, int type){
        if(queue == null || queue.size == 0) return false;
        for(int i = 0; i < queue.size; i++){
            if(queueCodeType(queue.get(i)) == type) return true;
        }
        return false;
    }

    private static int plannedVehicleWeaponLevel(@Nullable Team team){
        int level = vehicleWeaponLevel(team);
        level = Math.max(level, vehicleWeaponResearchingLevel(team));
        return Math.max(level, highestQueuedLevel(getResearchQueue(armoryResearchQueue, team, false), armoryQueueVehicleWeapon));
    }

    private static int plannedVehicleArmorLevel(@Nullable Team team){
        int level = vehicleArmorLevel(team);
        level = Math.max(level, vehicleArmorResearchingLevel(team));
        return Math.max(level, highestQueuedLevel(getResearchQueue(armoryResearchQueue, team, false), armoryQueueVehicleArmor));
    }

    private static int plannedShipWeaponLevel(@Nullable Team team){
        int level = shipWeaponLevel(team);
        level = Math.max(level, shipWeaponResearchingLevel(team));
        return Math.max(level, highestQueuedLevel(getResearchQueue(armoryResearchQueue, team, false), armoryQueueShipWeapon));
    }

    private static int plannedInfantryWeaponLevel(@Nullable Team team){
        int level = infantryWeaponLevel(team);
        level = Math.max(level, infantryWeaponResearchingLevel(team));
        return Math.max(level, highestQueuedLevel(getResearchQueue(engineeringResearchQueue, team, false), engineeringQueueInfantryWeapon));
    }

    private static int plannedInfantryArmorLevel(@Nullable Team team){
        int level = infantryArmorLevel(team);
        level = Math.max(level, infantryArmorResearchingLevel(team));
        return Math.max(level, highestQueuedLevel(getResearchQueue(engineeringResearchQueue, team, false), engineeringQueueInfantryArmor));
    }

    private static boolean instantTrackingPlanned(@Nullable Team team){
        return instantTrackingLevel(team) > 0 || instantTrackingResearching(team)
            || queueContainsType(getResearchQueue(engineeringResearchQueue, team, false), engineeringQueueInstantTracking);
    }

    private static boolean steelArmorPlanned(@Nullable Team team){
        return steelArmorLevel(team) > 0 || steelArmorResearching(team)
            || queueContainsType(getResearchQueue(engineeringResearchQueue, team, false), engineeringQueueSteelArmor);
    }

    private static boolean barracksBlastShieldPlanned(@Nullable Team team){
        return barracksBlastShieldLevel(team) > 0 || barracksBlastShieldResearching(team)
            || queueContainsType(getResearchQueue(barracksResearchQueue, team, false), barracksQueueBlastShield);
    }

    private static boolean barracksStimpackPlanned(@Nullable Team team){
        return barracksStimpackLevel(team) > 0 || barracksStimpackResearching(team)
            || queueContainsType(getResearchQueue(barracksResearchQueue, team, false), barracksQueueStimpack);
    }

    private static boolean barracksConcussivePlanned(@Nullable Team team){
        return barracksConcussiveLevel(team) > 0 || barracksConcussiveResearching(team)
            || queueContainsType(getResearchQueue(barracksResearchQueue, team, false), barracksQueueConcussive);
    }

    private static boolean infernoPreheaterPlanned(@Nullable Team team){
        return infernoPreheaterLevel(team) > 0 || infernoPreheaterResearching(team)
            || queueContainsType(getResearchQueue(heavyFactoryResearchQueue, team, false), heavyFactoryQueueInfernoPreheater);
    }

    private static boolean electromagneticFieldAcceleratorPlanned(@Nullable Team team){
        return electromagneticFieldAcceleratorLevel(team) > 0 || electromagneticFieldAcceleratorResearching(team)
            || queueContainsType(getResearchQueue(heavyFactoryResearchQueue, team, false), heavyFactoryQueueElectromagneticFieldAccelerator);
    }

    private static boolean drillClawPlanned(@Nullable Team team){
        return drillClawLevel(team) > 0 || drillClawResearching(team)
            || queueContainsType(getResearchQueue(heavyFactoryResearchQueue, team, false), heavyFactoryQueueDrillClaw);
    }

    private static boolean smartServosPlanned(@Nullable Team team){
        return smartServosLevel(team) > 0 || smartServosResearching(team)
            || queueContainsType(getResearchQueue(heavyFactoryResearchQueue, team, false), heavyFactoryQueueSmartServos);
    }

    private static boolean bansheeCloakFieldPlanned(@Nullable Team team){
        return bansheeCloakFieldLevel(team) > 0 || bansheeCloakFieldResearching(team)
            || queueContainsType(getResearchQueue(starportResearchQueue, team, false), starportQueueCloakField);
    }

    private static boolean bansheeAfterburnerPlanned(@Nullable Team team){
        return bansheeAfterburnerLevel(team) > 0 || bansheeAfterburnerResearching(team)
            || queueContainsType(getResearchQueue(starportResearchQueue, team, false), starportQueueAfterburner);
    }

    private static boolean ravenMatrixTechPlanned(@Nullable Team team){
        return ravenMatrixTechLevel(team) > 0 || ravenMatrixTechResearching(team)
            || queueContainsType(getResearchQueue(starportResearchQueue, team, false), starportQueueMatrix);
    }

    private static boolean battlecruiserWeaponRefitPlanned(@Nullable Team team){
        return battlecruiserWeaponRefitLevel(team) > 0 || battlecruiserWeaponRefitResearching(team)
            || queueContainsType(getResearchQueue(fusionCoreResearchQueue, team, false), fusionCoreQueueWeaponRefit);
    }

    private static boolean medivacCaduceusReactorPlanned(@Nullable Team team){
        return medivacCaduceusReactorLevel(team) > 0 || medivacCaduceusReactorResearching(team)
            || queueContainsType(getResearchQueue(fusionCoreResearchQueue, team, false), fusionCoreQueueCaduceusReactor);
    }

    private static boolean liberatorAdvancedBallisticsPlanned(@Nullable Team team){
        return liberatorAdvancedBallisticsLevel(team) > 0 || liberatorAdvancedBallisticsResearching(team)
            || queueContainsType(getResearchQueue(fusionCoreResearchQueue, team, false), fusionCoreQueueAdvancedBallistics);
    }

    private static boolean armoryActiveResearching(@Nullable Team team){
        return vehicleWeaponResearching(team) || shipWeaponResearching(team) || vehicleArmorResearching(team);
    }

    private static boolean engineeringActiveResearching(@Nullable Team team){
        return infantryWeaponResearching(team) || infantryArmorResearching(team) || instantTrackingResearching(team) || steelArmorResearching(team);
    }

    private static boolean barracksActiveResearching(@Nullable Team team){
        return barracksBlastShieldResearching(team) || barracksStimpackResearching(team) || barracksConcussiveResearching(team);
    }

    private static boolean heavyFactoryActiveResearching(@Nullable Team team){
        return infernoPreheaterResearching(team)
            || electromagneticFieldAcceleratorResearching(team)
            || drillClawResearching(team)
            || smartServosResearching(team);
    }

    private static boolean starportActiveResearching(@Nullable Team team){
        return bansheeCloakFieldResearching(team)
            || bansheeAfterburnerResearching(team)
            || ravenMatrixTechResearching(team);
    }

    private static boolean fusionCoreActiveResearching(@Nullable Team team){
        return battlecruiserWeaponRefitResearching(team)
            || medivacCaduceusReactorResearching(team)
            || liberatorAdvancedBallisticsResearching(team);
    }

    private static @Nullable Block armoryQueueBlockByCode(int code){
        int type = queueCodeType(code);
        if(type == armoryQueueVehicleWeapon) return Blocks.siliconCrucible;
        if(type == armoryQueueVehicleArmor) return Blocks.surgeCrucible;
        if(type == armoryQueueShipWeapon) return Blocks.shipFabricator;
        return null;
    }

    private static @Nullable Block engineeringQueueBlockByCode(int code){
        int type = queueCodeType(code);
        if(type == engineeringQueueInfantryWeapon) return Blocks.siliconCrucible;
        if(type == engineeringQueueInfantryArmor) return Blocks.multiPress;
        if(type == engineeringQueueInstantTracking) return Blocks.swarmer;
        if(type == engineeringQueueSteelArmor) return Blocks.atmosphericConcentrator;
        return null;
    }

    private static @Nullable UnitType barracksQueueUnitByCode(int code){
        int type = queueCodeType(code);
        if(type == barracksQueueBlastShield) return dagger;
        if(type == barracksQueueStimpack) return fortress;
        if(type == barracksQueueConcussive) return fortress;
        return null;
    }

    private static @Nullable UnitType heavyFactoryQueueUnitByCode(int code){
        int type = queueCodeType(code);
        if(type == heavyFactoryQueueInfernoPreheater) return locus;
        if(type == heavyFactoryQueueElectromagneticFieldAccelerator) return hurricane;
        if(type == heavyFactoryQueueDrillClaw) return crawler;
        if(type == heavyFactoryQueueSmartServos) return mace;
        return null;
    }

    private static @Nullable UnitType starportQueueUnitByCode(int code){
        int type = queueCodeType(code);
        if(type == starportQueueCloakField) return horizon;
        if(type == starportQueueAfterburner) return horizon;
        if(type == starportQueueMatrix) return avert;
        return null;
    }

    private static @Nullable UnitType fusionCoreQueueUnitByCode(int code){
        int type = queueCodeType(code);
        if(type == fusionCoreQueueWeaponRefit) return antumbra;
        if(type == fusionCoreQueueCaduceusReactor) return mega;
        if(type == fusionCoreQueueAdvancedBallistics) return liberator;
        return null;
    }

    public static int armoryQueuedCount(@Nullable Team team){
        IntSeq queue = getResearchQueue(armoryResearchQueue, team, false);
        return queue == null ? 0 : queue.size;
    }

    public static int armoryQueuedCode(@Nullable Team team, int index){
        IntSeq queue = getResearchQueue(armoryResearchQueue, team, false);
        if(queue == null || index < 0 || index >= queue.size) return 0;
        return queue.get(index);
    }

    public static @Nullable Block armoryQueuedBlock(@Nullable Team team, int index){
        return armoryQueueBlockByCode(armoryQueuedCode(team, index));
    }

    public static int engineeringQueuedCount(@Nullable Team team){
        IntSeq queue = getResearchQueue(engineeringResearchQueue, team, false);
        return queue == null ? 0 : queue.size;
    }

    public static int engineeringQueuedCode(@Nullable Team team, int index){
        IntSeq queue = getResearchQueue(engineeringResearchQueue, team, false);
        if(queue == null || index < 0 || index >= queue.size) return 0;
        return queue.get(index);
    }

    public static @Nullable Block engineeringQueuedBlock(@Nullable Team team, int index){
        return engineeringQueueBlockByCode(engineeringQueuedCode(team, index));
    }

    public static int barracksQueuedCount(@Nullable Team team){
        IntSeq queue = getResearchQueue(barracksResearchQueue, team, false);
        return queue == null ? 0 : queue.size;
    }

    public static int barracksQueuedCode(@Nullable Team team, int index){
        IntSeq queue = getResearchQueue(barracksResearchQueue, team, false);
        if(queue == null || index < 0 || index >= queue.size) return 0;
        return queue.get(index);
    }

    public static @Nullable UnitType barracksQueuedUnit(@Nullable Team team, int index){
        return barracksQueueUnitByCode(barracksQueuedCode(team, index));
    }

    public static int heavyFactoryQueuedCount(@Nullable Team team){
        IntSeq queue = getResearchQueue(heavyFactoryResearchQueue, team, false);
        return queue == null ? 0 : queue.size;
    }

    public static int heavyFactoryQueuedCode(@Nullable Team team, int index){
        IntSeq queue = getResearchQueue(heavyFactoryResearchQueue, team, false);
        if(queue == null || index < 0 || index >= queue.size) return 0;
        return queue.get(index);
    }

    public static @Nullable UnitType heavyFactoryQueuedUnit(@Nullable Team team, int index){
        return heavyFactoryQueueUnitByCode(heavyFactoryQueuedCode(team, index));
    }

    public static int starportQueuedCount(@Nullable Team team){
        IntSeq queue = getResearchQueue(starportResearchQueue, team, false);
        return queue == null ? 0 : queue.size;
    }

    public static int starportQueuedCode(@Nullable Team team, int index){
        IntSeq queue = getResearchQueue(starportResearchQueue, team, false);
        if(queue == null || index < 0 || index >= queue.size) return 0;
        return queue.get(index);
    }

    public static @Nullable UnitType starportQueuedUnit(@Nullable Team team, int index){
        return starportQueueUnitByCode(starportQueuedCode(team, index));
    }

    public static int fusionCoreQueuedCount(@Nullable Team team){
        IntSeq queue = getResearchQueue(fusionCoreResearchQueue, team, false);
        return queue == null ? 0 : queue.size;
    }

    public static int fusionCoreQueuedCode(@Nullable Team team, int index){
        IntSeq queue = getResearchQueue(fusionCoreResearchQueue, team, false);
        if(queue == null || index < 0 || index >= queue.size) return 0;
        return queue.get(index);
    }

    public static @Nullable UnitType fusionCoreQueuedUnit(@Nullable Team team, int index){
        return fusionCoreQueueUnitByCode(fusionCoreQueuedCode(team, index));
    }

    private static void updateInfantryUpgrades(){
        if(state == null || !state.isGame()) return;
        completeSandboxResearch();
        updateInfantryWeaponResearch();
        updateVehicleWeaponResearch();
        updateShipWeaponResearch();
        updateVehicleArmorResearch();
        updateInfantryArmorResearch();
        updateInstantTrackingResearch();
        updateSteelArmorResearch();
        updateGhostCamoResearch();
        updateBarracksBlastShieldResearch();
        updateBarracksStimpackResearch();
        updateBarracksConcussiveResearch();
        updateInfernoPreheaterResearch();
        updateElectromagneticFieldAcceleratorResearch();
        updateDrillClawResearch();
        updateSmartServosResearch();
        updateBansheeCloakFieldResearch();
        updateBansheeAfterburnerResearch();
        updateRavenMatrixTechResearch();
        updateBattlecruiserWeaponRefitResearch();
        updateMedivacCaduceusReactorResearch();
        updateLiberatorAdvancedBallisticsResearch();
        advanceQueuedResearch();
        updateMaceLocusTransforms();
        updateBarracksStimpackVisuals();
        updateBarracksStimpackCooldowns();
        updateGhostWarheadProduction();
        updateGhostTacticalNukes();
        updateBarracksBlastShieldBonus();
        updateBarracksConcussiveSlowEffect();
        updateInfantryArmorBonus();
    }

    private static void completeSandboxResearch(){
        if(state == null || !state.isGame()) return;

        completeSandboxAllResearchOnce();

        int guard = 0;
        int before = queuedResearchTotal();
        while(guard++ < 256 && before > 0){
            advanceQueuedResearch();
            completeSandboxAllResearchOnce();

            int after = queuedResearchTotal();
            if(after == before) break;
            before = after;
        }
    }

    private static void completeSandboxAllResearchOnce(){
        completeSandboxLeveledResearch(
            infantryWeaponData, infantryWeaponMaxLevel,
            data -> data.researchingLevel, (data, value) -> data.researchingLevel = value,
            data -> data.level, (data, value) -> data.level = value,
            (data, value) -> data.researchTime = value
        );

        completeSandboxLeveledResearch(
            vehicleWeaponData, vehicleWeaponMaxLevel,
            data -> data.researchingLevel, (data, value) -> data.researchingLevel = value,
            data -> data.level, (data, value) -> data.level = value,
            (data, value) -> data.researchTime = value
        );

        completeSandboxLeveledResearch(
            shipWeaponData, shipWeaponMaxLevel,
            data -> data.researchingLevel, (data, value) -> data.researchingLevel = value,
            data -> data.level, (data, value) -> data.level = value,
            (data, value) -> data.researchTime = value
        );

        completeSandboxLeveledResearch(
            vehicleArmorData, vehicleArmorMaxLevel,
            data -> data.researchingLevel, (data, value) -> data.researchingLevel = value,
            data -> data.level, (data, value) -> data.level = value,
            (data, value) -> data.researchTime = value
        );

        completeSandboxLeveledResearch(
            infantryArmorData, infantryWeaponMaxLevel,
            data -> data.researchingLevel, (data, value) -> data.researchingLevel = value,
            data -> data.level, (data, value) -> data.level = value,
            (data, value) -> data.researchTime = value
        );

        completeSandboxSingleResearch(
            instantTrackingData,
            data -> data.researching, (data, value) -> data.researching = value,
            data -> data.level, (data, value) -> data.level = value,
            (data, value) -> data.researchTime = value
        );

        completeSandboxSingleResearch(
            steelArmorData,
            data -> data.researching, (data, value) -> data.researching = value,
            data -> data.level, (data, value) -> data.level = value,
            (data, value) -> data.researchTime = value
        );

        completeSandboxSingleResearch(
            ghostCamoData,
            data -> data.researching, (data, value) -> data.researching = value,
            data -> data.level, (data, value) -> data.level = value,
            (data, value) -> data.researchTime = value
        );

        completeSandboxSingleResearch(
            barracksBlastShieldData,
            data -> data.researching, (data, value) -> data.researching = value,
            data -> data.level, (data, value) -> data.level = value,
            (data, value) -> data.researchTime = value
        );

        completeSandboxSingleResearch(
            barracksStimpackData,
            data -> data.researching, (data, value) -> data.researching = value,
            data -> data.level, (data, value) -> data.level = value,
            (data, value) -> data.researchTime = value
        );

        completeSandboxSingleResearch(
            barracksConcussiveData,
            data -> data.researching, (data, value) -> data.researching = value,
            data -> data.level, (data, value) -> data.level = value,
            (data, value) -> data.researchTime = value
        );

        completeSandboxSingleResearch(
            infernoPreheaterData,
            data -> data.researching, (data, value) -> data.researching = value,
            data -> data.level, (data, value) -> data.level = value,
            (data, value) -> data.researchTime = value
        );

        completeSandboxSingleResearch(
            electromagneticFieldAcceleratorData,
            data -> data.researching, (data, value) -> data.researching = value,
            data -> data.level, (data, value) -> data.level = value,
            (data, value) -> data.researchTime = value
        );

        completeSandboxSingleResearch(
            drillClawData,
            data -> data.researching, (data, value) -> data.researching = value,
            data -> data.level, (data, value) -> data.level = value,
            (data, value) -> data.researchTime = value
        );

        completeSandboxSingleResearch(
            smartServosData,
            data -> data.researching, (data, value) -> data.researching = value,
            data -> data.level, (data, value) -> data.level = value,
            (data, value) -> data.researchTime = value
        );

        completeSandboxSingleResearch(
            bansheeCloakFieldData,
            data -> data.researching, (data, value) -> data.researching = value,
            data -> data.level, (data, value) -> data.level = value,
            (data, value) -> data.researchTime = value
        );

        completeSandboxSingleResearch(
            bansheeAfterburnerData,
            data -> data.researching, (data, value) -> data.researching = value,
            data -> data.level, (data, value) -> data.level = value,
            (data, value) -> data.researchTime = value
        );

        completeSandboxSingleResearch(
            ravenMatrixTechData,
            data -> data.researching, (data, value) -> data.researching = value,
            data -> data.level, (data, value) -> data.level = value,
            (data, value) -> data.researchTime = value
        );

        completeSandboxSingleResearch(
            battlecruiserWeaponRefitData,
            data -> data.researching, (data, value) -> data.researching = value,
            data -> data.level, (data, value) -> data.level = value,
            (data, value) -> data.researchTime = value
        );

        completeSandboxSingleResearch(
            medivacCaduceusReactorData,
            data -> data.researching, (data, value) -> data.researching = value,
            data -> data.level, (data, value) -> data.level = value,
            (data, value) -> data.researchTime = value
        );

        completeSandboxSingleResearch(
            liberatorAdvancedBallisticsData,
            data -> data.researching, (data, value) -> data.researching = value,
            data -> data.level, (data, value) -> data.level = value,
            (data, value) -> data.researchTime = value
        );
    }

    private static <T> void completeSandboxLeveledResearch(IntMap<T> dataMap, int maxLevel,
        Sc2TechService.IntGetter<T> researchingLevel, Sc2TechService.IntSetter<T> setResearchingLevel,
        Sc2TechService.IntGetter<T> levelGetter, Sc2TechService.IntSetter<T> setLevel,
        Sc2TechService.FloatSetter<T> setResearchTime){

        for(IntMap.Entry<T> entry : dataMap.entries()){
            Team team = Team.get(entry.key);
            T data = entry.value;
            if(team == null || data == null) continue;
            if(!sandboxInstantForTeam(team)) continue;

            int researching = researchingLevel.get(data);
            if(researching <= 0) continue;

            int level = Mathf.clamp(researching, 1, maxLevel);
            if(levelGetter.get(data) < level){
                setLevel.set(data, level);
            }
            setResearchingLevel.set(data, 0);
            setResearchTime.set(data, 0f);
        }
    }

    private static <T> void completeSandboxSingleResearch(IntMap<T> dataMap,
        Sc2TechService.BoolGetter<T> researching, Sc2TechService.BoolSetter<T> setResearching,
        Sc2TechService.IntGetter<T> levelGetter, Sc2TechService.IntSetter<T> setLevel,
        Sc2TechService.FloatSetter<T> setResearchTime){

        for(IntMap.Entry<T> entry : dataMap.entries()){
            Team team = Team.get(entry.key);
            T data = entry.value;
            if(team == null || data == null) continue;
            if(!sandboxInstantForTeam(team)) continue;

            if(!researching.get(data)) continue;

            if(levelGetter.get(data) < 1){
                setLevel.set(data, 1);
            }
            setResearching.set(data, false);
            setResearchTime.set(data, 0f);
        }
    }

    private static int queuedResearchTotal(){
        return queuedResearchSize(armoryResearchQueue)
            + queuedResearchSize(engineeringResearchQueue)
            + queuedResearchSize(barracksResearchQueue)
            + queuedResearchSize(heavyFactoryResearchQueue)
            + queuedResearchSize(starportResearchQueue)
            + queuedResearchSize(fusionCoreResearchQueue);
    }

    private static int queuedResearchSize(IntMap<IntSeq> map){
        int total = 0;
        for(IntMap.Entry<IntSeq> entry : map.entries()){
            IntSeq queue = entry.value;
            if(queue != null){
                total += queue.size;
            }
        }
        return total;
    }

    private static boolean sandboxInstantForTeam(Team team){
        return state.rules.infiniteResources || team.rules().infiniteResources;
    }

    private static void updateInfantryWeaponResearch(){
        Sc2TechService.tickLeveledResearch(
            infantryWeaponData, infantryWeaponMaxLevel,
            data -> data.researchingLevel, (data, value) -> data.researchingLevel = value,
            data -> data.level, (data, value) -> data.level = value,
            data -> data.researchTime, (data, value) -> data.researchTime = value,
            level -> infantryWeaponResearchTime[level]
        );
    }

    private static void updateVehicleWeaponResearch(){
        Sc2TechService.tickLeveledResearch(
            vehicleWeaponData, vehicleWeaponMaxLevel,
            data -> data.researchingLevel, (data, value) -> data.researchingLevel = value,
            data -> data.level, (data, value) -> data.level = value,
            data -> data.researchTime, (data, value) -> data.researchTime = value,
            level -> vehicleWeaponResearchTime[level]
        );
    }

    private static void updateShipWeaponResearch(){
        Sc2TechService.tickLeveledResearch(
            shipWeaponData, shipWeaponMaxLevel,
            data -> data.researchingLevel, (data, value) -> data.researchingLevel = value,
            data -> data.level, (data, value) -> data.level = value,
            data -> data.researchTime, (data, value) -> data.researchTime = value,
            level -> shipWeaponResearchTime[level]
        );
    }

    private static void updateVehicleArmorResearch(){
        Sc2TechService.tickLeveledResearch(
            vehicleArmorData, vehicleArmorMaxLevel,
            data -> data.researchingLevel, (data, value) -> data.researchingLevel = value,
            data -> data.level, (data, value) -> data.level = value,
            data -> data.researchTime, (data, value) -> data.researchTime = value,
            level -> vehicleArmorResearchTime[level]
        );
    }

    private static void updateInfantryArmorResearch(){
        Sc2TechService.tickLeveledResearch(
            infantryArmorData, infantryWeaponMaxLevel,
            data -> data.researchingLevel, (data, value) -> data.researchingLevel = value,
            data -> data.level, (data, value) -> data.level = value,
            data -> data.researchTime, (data, value) -> data.researchTime = value,
            level -> infantryWeaponResearchTime[level]
        );
    }

    private static void updateInstantTrackingResearch(){
        Sc2TechService.tickSingleResearch(
            instantTrackingData, instantTrackingResearchTime,
            data -> data.researching, (data, value) -> data.researching = value,
            data -> data.level, (data, value) -> data.level = value,
            data -> data.researchTime, (data, value) -> data.researchTime = value
        );
    }

    private static void updateSteelArmorResearch(){
        Sc2TechService.tickSingleResearch(
            steelArmorData, steelArmorResearchTime,
            data -> data.researching, (data, value) -> data.researching = value,
            data -> data.level, (data, value) -> data.level = value,
            data -> data.researchTime, (data, value) -> data.researchTime = value
        );
    }

    private static void updateGhostCamoResearch(){
        Sc2TechService.tickSingleResearch(
            ghostCamoData, ghostCamoResearchTime,
            data -> data.researching, (data, value) -> data.researching = value,
            data -> data.level, (data, value) -> data.level = value,
            data -> data.researchTime, (data, value) -> data.researchTime = value
        );
    }

    private static void updateBarracksBlastShieldResearch(){
        Sc2TechService.tickSingleResearch(
            barracksBlastShieldData, barracksBlastShieldResearchTime,
            data -> data.researching, (data, value) -> data.researching = value,
            data -> data.level, (data, value) -> data.level = value,
            data -> data.researchTime, (data, value) -> data.researchTime = value
        );
    }

    private static void updateBarracksStimpackResearch(){
        Sc2TechService.tickSingleResearch(
            barracksStimpackData, barracksStimpackResearchTime,
            data -> data.researching, (data, value) -> data.researching = value,
            data -> data.level, (data, value) -> data.level = value,
            data -> data.researchTime, (data, value) -> data.researchTime = value
        );
    }

    private static void updateBarracksConcussiveResearch(){
        Sc2TechService.tickSingleResearch(
            barracksConcussiveData, barracksConcussiveResearchTime,
            data -> data.researching, (data, value) -> data.researching = value,
            data -> data.level, (data, value) -> data.level = value,
            data -> data.researchTime, (data, value) -> data.researchTime = value
        );
    }

    private static void updateInfernoPreheaterResearch(){
        Sc2TechService.tickSingleResearch(
            infernoPreheaterData, infernoPreheaterResearchTime,
            data -> data.researching, (data, value) -> data.researching = value,
            data -> data.level, (data, value) -> data.level = value,
            data -> data.researchTime, (data, value) -> data.researchTime = value
        );
    }

    private static void updateElectromagneticFieldAcceleratorResearch(){
        Sc2TechService.tickSingleResearch(
            electromagneticFieldAcceleratorData, electromagneticFieldAcceleratorResearchTime,
            data -> data.researching, (data, value) -> data.researching = value,
            data -> data.level, (data, value) -> data.level = value,
            data -> data.researchTime, (data, value) -> data.researchTime = value
        );
    }

    private static void updateDrillClawResearch(){
        Sc2TechService.tickSingleResearch(
            drillClawData, drillClawResearchTime,
            data -> data.researching, (data, value) -> data.researching = value,
            data -> data.level, (data, value) -> data.level = value,
            data -> data.researchTime, (data, value) -> data.researchTime = value
        );
    }

    private static void updateSmartServosResearch(){
        Sc2TechService.tickSingleResearch(
            smartServosData, smartServosResearchTime,
            data -> data.researching, (data, value) -> data.researching = value,
            data -> data.level, (data, value) -> data.level = value,
            data -> data.researchTime, (data, value) -> data.researchTime = value
        );
    }

    private static void updateBansheeCloakFieldResearch(){
        Sc2TechService.tickSingleResearch(
            bansheeCloakFieldData, bansheeCloakFieldResearchTime,
            data -> data.researching, (data, value) -> data.researching = value,
            data -> data.level, (data, value) -> data.level = value,
            data -> data.researchTime, (data, value) -> data.researchTime = value
        );
    }

    private static void updateBansheeAfterburnerResearch(){
        Sc2TechService.tickSingleResearch(
            bansheeAfterburnerData, bansheeAfterburnerResearchTime,
            data -> data.researching, (data, value) -> data.researching = value,
            data -> data.level, (data, value) -> data.level = value,
            data -> data.researchTime, (data, value) -> data.researchTime = value
        );
    }

    private static void updateRavenMatrixTechResearch(){
        Sc2TechService.tickSingleResearch(
            ravenMatrixTechData, ravenMatrixTechResearchTime,
            data -> data.researching, (data, value) -> data.researching = value,
            data -> data.level, (data, value) -> data.level = value,
            data -> data.researchTime, (data, value) -> data.researchTime = value
        );
    }

    private static void updateBattlecruiserWeaponRefitResearch(){
        Sc2TechService.tickSingleResearch(
            battlecruiserWeaponRefitData, battlecruiserWeaponRefitResearchTime,
            data -> data.researching, (data, value) -> data.researching = value,
            data -> data.level, (data, value) -> data.level = value,
            data -> data.researchTime, (data, value) -> data.researchTime = value
        );
    }

    private static void updateMedivacCaduceusReactorResearch(){
        Sc2TechService.tickSingleResearch(
            medivacCaduceusReactorData, medivacCaduceusReactorResearchTime,
            data -> data.researching, (data, value) -> data.researching = value,
            data -> data.level, (data, value) -> data.level = value,
            data -> data.researchTime, (data, value) -> data.researchTime = value
        );
    }

    private static void updateLiberatorAdvancedBallisticsResearch(){
        Sc2TechService.tickSingleResearch(
            liberatorAdvancedBallisticsData, liberatorAdvancedBallisticsResearchTime,
            data -> data.researching, (data, value) -> data.researching = value,
            data -> data.level, (data, value) -> data.level = value,
            data -> data.researchTime, (data, value) -> data.researchTime = value
        );
    }

    private static @Nullable Unit transformMaceLocusUnit(Unit unit, UnitType targetType){
        if(unit == null || targetType == null || unit.type == targetType) return unit;
        if(net.client()) return unit;

        float healthf = Mathf.clamp(unit.healthf());
        float rotation = unit.rotation;
        float x = unit.x;
        float y = unit.y;
        float shield = unit.shield;
        float elevation = unit.elevation;
        var controller = unit.controller();

        Unit transformed = targetType.create(unit.team);
        transformed.set(x, y);
        transformed.rotation(rotation);
        transformed.elevation = elevation;
        transformed.shield = shield;
        transformed.health = Math.max(transformed.maxHealth * healthf, 1f);
        if(controller != null){
            transformed.controller(controller);
        }
        transformed.add();
        if(control != null && control.input != null){
            control.input.replaceSelectedUnit(unit, transformed);
            control.input.preserveUnitSelection(new int[]{transformed.id}, 60f * 5f);
        }
        unit.remove();
        return transformed;
    }

    private static void updateMaceLocusTransforms(){
        Sc2TransformService.updateMaceLocusTransforms(
            maceLocusTransformData,
            UnitTypes::isMace,
            UnitTypes::isLocus,
            locus,
            mace,
            UnitTypes::transformMaceLocusUnit
        );
    }

    private static boolean infantryWeaponCanBeginNow(@Nullable Team team, int level){
        if(team == null || level < 1 || level > infantryWeaponMaxLevel) return false;
        if(level != infantryWeaponLevel(team) + 1) return false;
        return infantryWeaponHasArmory(team);
    }

    private static boolean vehicleWeaponCanBeginNow(@Nullable Team team, int level){
        if(team == null || level < 1 || level > vehicleWeaponMaxLevel) return false;
        if(level != vehicleWeaponLevel(team) + 1) return false;
        if(!vehicleWeaponHasArmory(team)) return false;
        return infantryWeaponLevel(team) >= level;
    }

    private static boolean vehicleArmorCanBeginNow(@Nullable Team team, int level){
        if(team == null || level < 1 || level > vehicleArmorMaxLevel) return false;
        if(level != vehicleArmorLevel(team) + 1) return false;
        return vehicleWeaponHasArmory(team);
    }

    private static boolean shipWeaponCanBeginNow(@Nullable Team team, int level){
        if(team == null || level < 1 || level > shipWeaponMaxLevel) return false;
        if(level != shipWeaponLevel(team) + 1) return false;
        return vehicleWeaponHasArmory(team);
    }

    private static boolean infantryArmorCanBeginNow(@Nullable Team team, int level){
        if(team == null || level < 1 || level > infantryWeaponMaxLevel) return false;
        if(level != infantryArmorLevel(team) + 1) return false;
        return infantryWeaponHasArmory(team);
    }

    private static boolean instantTrackingCanBeginNow(@Nullable Team team){
        if(team == null) return false;
        return instantTrackingLevel(team) <= 0 && !instantTrackingResearching(team);
    }

    private static boolean steelArmorCanBeginNow(@Nullable Team team){
        if(team == null) return false;
        return steelArmorLevel(team) <= 0 && !steelArmorResearching(team);
    }

    private static boolean barracksBlastShieldCanBeginNow(@Nullable Team team){
        if(team == null) return false;
        return barracksBlastShieldLevel(team) <= 0 && !barracksBlastShieldResearching(team) && barracksTeamHasTechAddon(team);
    }

    private static boolean barracksStimpackCanBeginNow(@Nullable Team team){
        if(team == null) return false;
        return barracksStimpackLevel(team) <= 0 && !barracksStimpackResearching(team) && barracksTeamHasTechAddon(team);
    }

    private static boolean barracksConcussiveCanBeginNow(@Nullable Team team){
        if(team == null) return false;
        return barracksConcussiveLevel(team) <= 0 && !barracksConcussiveResearching(team) && barracksTeamHasTechAddon(team);
    }

    private static boolean infernoPreheaterCanBeginNow(@Nullable Team team){
        if(team == null) return false;
        return infernoPreheaterLevel(team) <= 0 && !infernoPreheaterResearching(team) && heavyFactoryTeamHasTechAddon(team);
    }

    private static boolean electromagneticFieldAcceleratorCanBeginNow(@Nullable Team team){
        if(team == null) return false;
        return electromagneticFieldAcceleratorLevel(team) <= 0
            && !electromagneticFieldAcceleratorResearching(team)
            && heavyFactoryTeamHasTechAddon(team);
    }

    private static boolean drillClawCanBeginNow(@Nullable Team team){
        if(team == null) return false;
        return drillClawLevel(team) <= 0 && !drillClawResearching(team) && heavyFactoryTeamHasTechAddon(team);
    }

    private static boolean smartServosCanBeginNow(@Nullable Team team){
        if(team == null) return false;
        return smartServosLevel(team) <= 0 && !smartServosResearching(team) && heavyFactoryTeamHasTechAddon(team);
    }

    private static boolean bansheeCloakFieldCanBeginNow(@Nullable Team team){
        if(team == null) return false;
        return bansheeCloakFieldLevel(team) <= 0 && !bansheeCloakFieldResearching(team) && starportTeamHasTechAddon(team);
    }

    private static boolean bansheeAfterburnerCanBeginNow(@Nullable Team team){
        if(team == null) return false;
        return bansheeAfterburnerLevel(team) <= 0 && !bansheeAfterburnerResearching(team) && starportTeamHasTechAddon(team);
    }

    private static boolean ravenMatrixTechCanBeginNow(@Nullable Team team){
        if(team == null) return false;
        return ravenMatrixTechLevel(team) <= 0 && !ravenMatrixTechResearching(team) && starportTeamHasTechAddon(team);
    }

    private static boolean battlecruiserWeaponRefitCanBeginNow(@Nullable Team team){
        if(team == null) return false;
        return battlecruiserWeaponRefitLevel(team) <= 0 && !battlecruiserWeaponRefitResearching(team) && fusionCoreHas(team);
    }

    private static boolean medivacCaduceusReactorCanBeginNow(@Nullable Team team){
        if(team == null) return false;
        return medivacCaduceusReactorLevel(team) <= 0 && !medivacCaduceusReactorResearching(team) && fusionCoreHas(team);
    }

    private static boolean liberatorAdvancedBallisticsCanBeginNow(@Nullable Team team){
        if(team == null) return false;
        return liberatorAdvancedBallisticsLevel(team) <= 0 && !liberatorAdvancedBallisticsResearching(team) && fusionCoreHas(team);
    }

    private static boolean startInfantryWeaponResearchNow(@Nullable Team team, int level){
        if(!infantryWeaponCanBeginNow(team, level)) return false;
        InfantryWeaponData data = getInfantryWeaponData(team, true);
        if(data == null) return false;
        data.researchingLevel = level;
        data.researchTime = 0f;
        return true;
    }

    private static boolean startVehicleWeaponResearchNow(@Nullable Team team, int level){
        if(!vehicleWeaponCanBeginNow(team, level)) return false;
        VehicleWeaponData data = getVehicleWeaponData(team, true);
        if(data == null) return false;
        data.researchingLevel = level;
        data.researchTime = 0f;
        return true;
    }

    private static boolean startVehicleArmorResearchNow(@Nullable Team team, int level){
        if(!vehicleArmorCanBeginNow(team, level)) return false;
        VehicleArmorData data = getVehicleArmorData(team, true);
        if(data == null) return false;
        data.researchingLevel = level;
        data.researchTime = 0f;
        return true;
    }

    private static boolean startShipWeaponResearchNow(@Nullable Team team, int level){
        if(!shipWeaponCanBeginNow(team, level)) return false;
        ShipWeaponData data = getShipWeaponData(team, true);
        if(data == null) return false;
        data.researchingLevel = level;
        data.researchTime = 0f;
        return true;
    }

    private static boolean startInfantryArmorResearchNow(@Nullable Team team, int level){
        if(!infantryArmorCanBeginNow(team, level)) return false;
        InfantryArmorData data = getInfantryArmorData(team, true);
        if(data == null) return false;
        data.researchingLevel = level;
        data.researchTime = 0f;
        return true;
    }

    private static boolean startInstantTrackingResearchNow(@Nullable Team team){
        if(!instantTrackingCanBeginNow(team)) return false;
        InstantTrackingData data = getInstantTrackingData(team, true);
        if(data == null) return false;
        data.researching = true;
        data.researchTime = 0f;
        return true;
    }

    private static boolean startSteelArmorResearchNow(@Nullable Team team){
        if(!steelArmorCanBeginNow(team)) return false;
        SteelArmorData data = getSteelArmorData(team, true);
        if(data == null) return false;
        data.researching = true;
        data.researchTime = 0f;
        return true;
    }

    private static boolean startBarracksBlastShieldResearchNow(@Nullable Team team){
        if(!barracksBlastShieldCanBeginNow(team)) return false;
        BarracksBlastShieldData data = getBarracksBlastShieldData(team, true);
        if(data == null) return false;
        data.researching = true;
        data.researchTime = 0f;
        return true;
    }

    private static boolean startBarracksStimpackResearchNow(@Nullable Team team){
        if(!barracksStimpackCanBeginNow(team)) return false;
        BarracksStimpackData data = getBarracksStimpackData(team, true);
        if(data == null) return false;
        data.researching = true;
        data.researchTime = 0f;
        return true;
    }

    private static boolean startBarracksConcussiveResearchNow(@Nullable Team team){
        if(!barracksConcussiveCanBeginNow(team)) return false;
        BarracksConcussiveData data = getBarracksConcussiveData(team, true);
        if(data == null) return false;
        data.researching = true;
        data.researchTime = 0f;
        return true;
    }

    private static boolean startInfernoPreheaterResearchNow(@Nullable Team team){
        if(!infernoPreheaterCanBeginNow(team)) return false;
        InfernoPreheaterData data = getInfernoPreheaterData(team, true);
        if(data == null) return false;
        data.researching = true;
        data.researchTime = 0f;
        return true;
    }

    private static boolean startElectromagneticFieldAcceleratorResearchNow(@Nullable Team team){
        if(!electromagneticFieldAcceleratorCanBeginNow(team)) return false;
        ElectromagneticFieldAcceleratorData data = getElectromagneticFieldAcceleratorData(team, true);
        if(data == null) return false;
        data.researching = true;
        data.researchTime = 0f;
        return true;
    }

    private static boolean startDrillClawResearchNow(@Nullable Team team){
        if(!drillClawCanBeginNow(team)) return false;
        DrillClawData data = getDrillClawData(team, true);
        if(data == null) return false;
        data.researching = true;
        data.researchTime = 0f;
        return true;
    }

    private static boolean startSmartServosResearchNow(@Nullable Team team){
        if(!smartServosCanBeginNow(team)) return false;
        SmartServosData data = getSmartServosData(team, true);
        if(data == null) return false;
        data.researching = true;
        data.researchTime = 0f;
        return true;
    }

    private static boolean startBansheeCloakFieldResearchNow(@Nullable Team team){
        if(!bansheeCloakFieldCanBeginNow(team)) return false;
        BansheeCloakFieldData data = getBansheeCloakFieldData(team, true);
        if(data == null) return false;
        data.researching = true;
        data.researchTime = 0f;
        return true;
    }

    private static boolean startBansheeAfterburnerResearchNow(@Nullable Team team){
        if(!bansheeAfterburnerCanBeginNow(team)) return false;
        BansheeAfterburnerData data = getBansheeAfterburnerData(team, true);
        if(data == null) return false;
        data.researching = true;
        data.researchTime = 0f;
        return true;
    }

    private static boolean startRavenMatrixTechResearchNow(@Nullable Team team){
        if(!ravenMatrixTechCanBeginNow(team)) return false;
        RavenMatrixTechData data = getRavenMatrixTechData(team, true);
        if(data == null) return false;
        data.researching = true;
        data.researchTime = 0f;
        return true;
    }

    private static boolean startBattlecruiserWeaponRefitResearchNow(@Nullable Team team){
        if(!battlecruiserWeaponRefitCanBeginNow(team)) return false;
        BattlecruiserWeaponRefitData data = getBattlecruiserWeaponRefitData(team, true);
        if(data == null) return false;
        data.researching = true;
        data.researchTime = 0f;
        return true;
    }

    private static boolean startMedivacCaduceusReactorResearchNow(@Nullable Team team){
        if(!medivacCaduceusReactorCanBeginNow(team)) return false;
        MedivacCaduceusReactorData data = getMedivacCaduceusReactorData(team, true);
        if(data == null) return false;
        data.researching = true;
        data.researchTime = 0f;
        return true;
    }

    private static boolean startLiberatorAdvancedBallisticsResearchNow(@Nullable Team team){
        if(!liberatorAdvancedBallisticsCanBeginNow(team)) return false;
        LiberatorAdvancedBallisticsData data = getLiberatorAdvancedBallisticsData(team, true);
        if(data == null) return false;
        data.researching = true;
        data.researchTime = 0f;
        return true;
    }

    private static void refundArmoryQueueCode(@Nullable Team team, int code){
        int type = queueCodeType(code);
        int level = queueCodeLevel(code);
        if(type == armoryQueueVehicleWeapon){
            vehicleWeaponRefund(team, level);
        }else if(type == armoryQueueVehicleArmor){
            vehicleArmorRefund(team, level);
        }else if(type == armoryQueueShipWeapon){
            shipWeaponRefund(team, level);
        }
    }

    private static void refundEngineeringQueueCode(@Nullable Team team, int code){
        int type = queueCodeType(code);
        int level = queueCodeLevel(code);
        if(type == engineeringQueueInfantryWeapon || type == engineeringQueueInfantryArmor){
            infantryUpgradeRefund(team, level);
        }else if(type == engineeringQueueInstantTracking){
            instantTrackingRefund(team);
        }else if(type == engineeringQueueSteelArmor){
            steelArmorRefund(team);
        }
    }

    private static void refundBarracksQueueCode(@Nullable Team team, int code){
        int type = queueCodeType(code);
        if(type == barracksQueueBlastShield){
            barracksTechRefund(team, barracksBlastShieldCrystalCost, barracksBlastShieldGasCost);
        }else if(type == barracksQueueStimpack){
            barracksTechRefund(team, barracksStimpackCrystalCost, barracksStimpackGasCost);
        }else if(type == barracksQueueConcussive){
            barracksTechRefund(team, barracksConcussiveCrystalCost, barracksConcussiveGasCost);
        }
    }

    private static void refundHeavyFactoryQueueCode(@Nullable Team team, int code){
        int type = queueCodeType(code);
        if(type == heavyFactoryQueueInfernoPreheater){
            barracksTechRefund(team, infernoPreheaterCrystalCost, infernoPreheaterGasCost);
        }else if(type == heavyFactoryQueueElectromagneticFieldAccelerator){
            barracksTechRefund(team, electromagneticFieldAcceleratorCrystalCost, electromagneticFieldAcceleratorGasCost);
        }else if(type == heavyFactoryQueueDrillClaw){
            barracksTechRefund(team, drillClawCrystalCost, drillClawGasCost);
        }else if(type == heavyFactoryQueueSmartServos){
            barracksTechRefund(team, smartServosCrystalCost, smartServosGasCost);
        }
    }

    private static void refundStarportQueueCode(@Nullable Team team, int code){
        int type = queueCodeType(code);
        if(type == starportQueueCloakField){
            barracksTechRefund(team, bansheeCloakFieldCrystalCost, bansheeCloakFieldGasCost);
        }else if(type == starportQueueAfterburner){
            barracksTechRefund(team, bansheeAfterburnerCrystalCost, bansheeAfterburnerGasCost);
        }else if(type == starportQueueMatrix){
            barracksTechRefund(team, ravenMatrixTechCrystalCost, ravenMatrixTechGasCost);
        }
    }

    private static void refundFusionCoreQueueCode(@Nullable Team team, int code){
        int type = queueCodeType(code);
        if(type == fusionCoreQueueWeaponRefit){
            barracksTechRefund(team, battlecruiserWeaponRefitCrystalCost, battlecruiserWeaponRefitGasCost);
        }else if(type == fusionCoreQueueCaduceusReactor){
            barracksTechRefund(team, medivacCaduceusReactorCrystalCost, medivacCaduceusReactorGasCost);
        }else if(type == fusionCoreQueueAdvancedBallistics){
            barracksTechRefund(team, liberatorAdvancedBallisticsCrystalCost, liberatorAdvancedBallisticsGasCost);
        }
    }

    private static void advanceQueuedResearch(){
        advanceQueuedArmoryResearch();
        advanceQueuedEngineeringResearch();
        advanceQueuedBarracksResearch();
        advanceQueuedHeavyFactoryResearch();
        advanceQueuedStarportResearch();
        advanceQueuedFusionCoreResearch();
    }

    private static void advanceQueuedArmoryResearch(){
        Sc2TechService.advanceQueuedResearch(armoryResearchQueue, UnitTypes::armoryActiveResearching, new Sc2TechService.QueueEntryOps(){
            @Override
            public boolean canHandle(int code){
                int type = queueCodeType(code);
                return type == armoryQueueVehicleWeapon || type == armoryQueueVehicleArmor || type == armoryQueueShipWeapon;
            }

            @Override
            public boolean canStart(Team team, int code){
                int type = queueCodeType(code);
                int level = queueCodeLevel(code);
                if(type == armoryQueueVehicleWeapon) return vehicleWeaponCanBeginNow(team, level);
                if(type == armoryQueueVehicleArmor) return vehicleArmorCanBeginNow(team, level);
                if(type == armoryQueueShipWeapon) return shipWeaponCanBeginNow(team, level);
                return false;
            }

            @Override
            public boolean start(Team team, int code){
                int type = queueCodeType(code);
                int level = queueCodeLevel(code);
                if(type == armoryQueueVehicleWeapon) return startVehicleWeaponResearchNow(team, level);
                if(type == armoryQueueVehicleArmor) return startVehicleArmorResearchNow(team, level);
                if(type == armoryQueueShipWeapon) return startShipWeaponResearchNow(team, level);
                return false;
            }

            @Override
            public void refund(Team team, int code){
                refundArmoryQueueCode(team, code);
            }
        });
    }

    private static void advanceQueuedEngineeringResearch(){
        Sc2TechService.advanceQueuedResearch(engineeringResearchQueue, UnitTypes::engineeringActiveResearching, new Sc2TechService.QueueEntryOps(){
            @Override
            public boolean canHandle(int code){
                int type = queueCodeType(code);
                return type == engineeringQueueInfantryWeapon
                    || type == engineeringQueueInfantryArmor
                    || type == engineeringQueueInstantTracking
                    || type == engineeringQueueSteelArmor;
            }

            @Override
            public boolean canStart(Team team, int code){
                int type = queueCodeType(code);
                int level = queueCodeLevel(code);
                if(type == engineeringQueueInfantryWeapon) return infantryWeaponCanBeginNow(team, level);
                if(type == engineeringQueueInfantryArmor) return infantryArmorCanBeginNow(team, level);
                if(type == engineeringQueueInstantTracking) return instantTrackingCanBeginNow(team);
                if(type == engineeringQueueSteelArmor) return steelArmorCanBeginNow(team);
                return false;
            }

            @Override
            public boolean start(Team team, int code){
                int type = queueCodeType(code);
                int level = queueCodeLevel(code);
                if(type == engineeringQueueInfantryWeapon) return startInfantryWeaponResearchNow(team, level);
                if(type == engineeringQueueInfantryArmor) return startInfantryArmorResearchNow(team, level);
                if(type == engineeringQueueInstantTracking) return startInstantTrackingResearchNow(team);
                if(type == engineeringQueueSteelArmor) return startSteelArmorResearchNow(team);
                return false;
            }

            @Override
            public void refund(Team team, int code){
                refundEngineeringQueueCode(team, code);
            }
        });
    }

    private static void advanceQueuedBarracksResearch(){
        Sc2TechService.advanceQueuedResearch(barracksResearchQueue, UnitTypes::barracksActiveResearching, new Sc2TechService.QueueEntryOps(){
            @Override
            public boolean canHandle(int code){
                int type = queueCodeType(code);
                return type == barracksQueueBlastShield || type == barracksQueueStimpack || type == barracksQueueConcussive;
            }

            @Override
            public boolean canStart(Team team, int code){
                int type = queueCodeType(code);
                if(type == barracksQueueBlastShield) return barracksBlastShieldCanBeginNow(team);
                if(type == barracksQueueStimpack) return barracksStimpackCanBeginNow(team);
                if(type == barracksQueueConcussive) return barracksConcussiveCanBeginNow(team);
                return false;
            }

            @Override
            public boolean start(Team team, int code){
                int type = queueCodeType(code);
                if(type == barracksQueueBlastShield) return startBarracksBlastShieldResearchNow(team);
                if(type == barracksQueueStimpack) return startBarracksStimpackResearchNow(team);
                if(type == barracksQueueConcussive) return startBarracksConcussiveResearchNow(team);
                return false;
            }

            @Override
            public void refund(Team team, int code){
                refundBarracksQueueCode(team, code);
            }
        });
    }

    private static void advanceQueuedHeavyFactoryResearch(){
        Sc2TechService.advanceQueuedResearch(heavyFactoryResearchQueue, UnitTypes::heavyFactoryActiveResearching, new Sc2TechService.QueueEntryOps(){
            @Override
            public boolean canHandle(int code){
                int type = queueCodeType(code);
                return type == heavyFactoryQueueInfernoPreheater
                    || type == heavyFactoryQueueElectromagneticFieldAccelerator
                    || type == heavyFactoryQueueDrillClaw
                    || type == heavyFactoryQueueSmartServos;
            }

            @Override
            public boolean canStart(Team team, int code){
                int type = queueCodeType(code);
                if(type == heavyFactoryQueueInfernoPreheater) return infernoPreheaterCanBeginNow(team);
                if(type == heavyFactoryQueueElectromagneticFieldAccelerator) return electromagneticFieldAcceleratorCanBeginNow(team);
                if(type == heavyFactoryQueueDrillClaw) return drillClawCanBeginNow(team);
                if(type == heavyFactoryQueueSmartServos) return smartServosCanBeginNow(team);
                return false;
            }

            @Override
            public boolean start(Team team, int code){
                int type = queueCodeType(code);
                if(type == heavyFactoryQueueInfernoPreheater) return startInfernoPreheaterResearchNow(team);
                if(type == heavyFactoryQueueElectromagneticFieldAccelerator) return startElectromagneticFieldAcceleratorResearchNow(team);
                if(type == heavyFactoryQueueDrillClaw) return startDrillClawResearchNow(team);
                if(type == heavyFactoryQueueSmartServos) return startSmartServosResearchNow(team);
                return false;
            }

            @Override
            public void refund(Team team, int code){
                refundHeavyFactoryQueueCode(team, code);
            }
        });
    }

    private static void advanceQueuedStarportResearch(){
        Sc2TechService.advanceQueuedResearch(starportResearchQueue, UnitTypes::starportActiveResearching, new Sc2TechService.QueueEntryOps(){
            @Override
            public boolean canHandle(int code){
                int type = queueCodeType(code);
                return type == starportQueueCloakField || type == starportQueueAfterburner || type == starportQueueMatrix;
            }

            @Override
            public boolean canStart(Team team, int code){
                int type = queueCodeType(code);
                if(type == starportQueueCloakField) return bansheeCloakFieldCanBeginNow(team);
                if(type == starportQueueAfterburner) return bansheeAfterburnerCanBeginNow(team);
                if(type == starportQueueMatrix) return ravenMatrixTechCanBeginNow(team);
                return false;
            }

            @Override
            public boolean start(Team team, int code){
                int type = queueCodeType(code);
                if(type == starportQueueCloakField) return startBansheeCloakFieldResearchNow(team);
                if(type == starportQueueAfterburner) return startBansheeAfterburnerResearchNow(team);
                if(type == starportQueueMatrix) return startRavenMatrixTechResearchNow(team);
                return false;
            }

            @Override
            public void refund(Team team, int code){
                refundStarportQueueCode(team, code);
            }
        });
    }

    private static void advanceQueuedFusionCoreResearch(){
        Sc2TechService.advanceQueuedResearch(fusionCoreResearchQueue, UnitTypes::fusionCoreActiveResearching, new Sc2TechService.QueueEntryOps(){
            @Override
            public boolean canHandle(int code){
                int type = queueCodeType(code);
                return type == fusionCoreQueueWeaponRefit || type == fusionCoreQueueCaduceusReactor || type == fusionCoreQueueAdvancedBallistics;
            }

            @Override
            public boolean canStart(Team team, int code){
                int type = queueCodeType(code);
                if(type == fusionCoreQueueWeaponRefit) return battlecruiserWeaponRefitCanBeginNow(team);
                if(type == fusionCoreQueueCaduceusReactor) return medivacCaduceusReactorCanBeginNow(team);
                if(type == fusionCoreQueueAdvancedBallistics) return liberatorAdvancedBallisticsCanBeginNow(team);
                return false;
            }

            @Override
            public boolean start(Team team, int code){
                int type = queueCodeType(code);
                if(type == fusionCoreQueueWeaponRefit) return startBattlecruiserWeaponRefitResearchNow(team);
                if(type == fusionCoreQueueCaduceusReactor) return startMedivacCaduceusReactorResearchNow(team);
                if(type == fusionCoreQueueAdvancedBallistics) return startLiberatorAdvancedBallisticsResearchNow(team);
                return false;
            }

            @Override
            public void refund(Team team, int code){
                refundFusionCoreQueueCode(team, code);
            }
        });
    }

    private static boolean cancelLastQueuedArmoryResearch(@Nullable Team team){
        IntSeq queue = getResearchQueue(armoryResearchQueue, team, false);
        if(queue == null || queue.size == 0) return false;
        int code = queue.pop();
        refundArmoryQueueCode(team, code);
        trimEmptyQueue(armoryResearchQueue, team);
        return true;
    }

    private static boolean cancelLastQueuedEngineeringResearch(@Nullable Team team){
        IntSeq queue = getResearchQueue(engineeringResearchQueue, team, false);
        if(queue == null || queue.size == 0) return false;
        int code = queue.pop();
        refundEngineeringQueueCode(team, code);
        trimEmptyQueue(engineeringResearchQueue, team);
        return true;
    }

    private static boolean cancelLastQueuedBarracksResearch(@Nullable Team team){
        IntSeq queue = getResearchQueue(barracksResearchQueue, team, false);
        if(queue == null || queue.size == 0) return false;
        int code = queue.pop();
        refundBarracksQueueCode(team, code);
        trimEmptyQueue(barracksResearchQueue, team);
        return true;
    }

    private static boolean cancelLastQueuedHeavyFactoryResearch(@Nullable Team team){
        IntSeq queue = getResearchQueue(heavyFactoryResearchQueue, team, false);
        if(queue == null || queue.size == 0) return false;
        int code = queue.pop();
        refundHeavyFactoryQueueCode(team, code);
        trimEmptyQueue(heavyFactoryResearchQueue, team);
        return true;
    }

    private static boolean cancelLastQueuedStarportResearch(@Nullable Team team){
        IntSeq queue = getResearchQueue(starportResearchQueue, team, false);
        if(queue == null || queue.size == 0) return false;
        int code = queue.pop();
        refundStarportQueueCode(team, code);
        trimEmptyQueue(starportResearchQueue, team);
        return true;
    }

    private static boolean cancelLastQueuedFusionCoreResearch(@Nullable Team team){
        IntSeq queue = getResearchQueue(fusionCoreResearchQueue, team, false);
        if(queue == null || queue.size == 0) return false;
        int code = queue.pop();
        refundFusionCoreQueueCode(team, code);
        trimEmptyQueue(fusionCoreResearchQueue, team);
        return true;
    }

    private static void updateBarracksStimpackCooldowns(){
        IntSeq remove = new IntSeq();
        IntSeq updateKeys = new IntSeq();
        FloatSeq updateValues = new FloatSeq();
        for(IntFloatMap.Entry entry : barracksStimpackCooldowns.entries()){
            int unitId = entry.key;
            float cooldown = entry.value;
            Unit unit = Groups.unit.getByID(unitId);
            if(unit == null || !unit.isValid() || !isBarracksStimpackUnit(unit)){
                remove.add(unitId);
                continue;
            }

            cooldown = Math.max(0f, cooldown - Time.delta);
            if(cooldown <= 0.001f){
                remove.add(unitId);
            }else{
                updateKeys.add(unitId);
                updateValues.add(cooldown);
            }
        }

        for(int i = 0; i < updateKeys.size; i++){
            barracksStimpackCooldowns.put(updateKeys.get(i), updateValues.get(i));
        }
        for(int i = 0; i < remove.size; i++){
            barracksStimpackCooldowns.remove(remove.get(i), 0f);
        }
    }

    private static void updateBarracksBlastShieldBonus(){
        for(Unit unit : Groups.unit){
            if(unit == null || !unit.isValid() || unit.type != dagger) continue;
            float targetMax = dagger.health + barracksBlastShieldHpBonus(unit.team);
            float prevMax = unit.maxHealth();
            if(Mathf.equal(prevMax, targetMax, 0.001f)) continue;

            float currentHealth = unit.health();
            float delta = targetMax - prevMax;
            unit.maxHealth(targetMax);
            if(delta > 0f){
                unit.health(Math.min(targetMax, currentHealth + delta));
            }else{
                unit.health(Math.min(currentHealth, targetMax));
            }
        }
    }

    private static void updateBarracksConcussiveSlowEffect(){
        for(Unit unit : Groups.unit){
            if(unit == null || !unit.isValid()) continue;
            if(!unit.hasEffect(StatusEffects.barracksConcussiveSlow)) continue;

            float cappedSpeed = Math.max(unit.speed() - barracksConcussiveSpeedPenalty, 0.1f);
            float velocity = unit.vel.len();
            if(velocity > cappedSpeed){
                unit.vel.setLength(cappedSpeed);
            }
        }
    }

    private static void updateGhostWarheadProduction(){
        IntSeq remove = new IntSeq();

        for(IntMap.Entry<GhostWarheadSiloData> entry : ghostWarheadSiloData.entries()){
            int pos = entry.key;
            GhostWarheadSiloData data = entry.value;
            Building build = world.build(pos);

            if(build == null || !build.isValid() || build.block != Blocks.launchPad){
                remove.add(pos);
                continue;
            }

            if(data == null || !data.producing) continue;
            if(sandboxInstantForTeam(build.team)){
                data.buildTime = 0f;
                data.producing = false;
                data.armed = true;
                continue;
            }
            data.buildTime += Time.delta;
            if(data.buildTime >= ghostWarheadBuildTime){
                data.buildTime = 0f;
                data.producing = false;
                data.armed = true;
            }
        }

        for(int i = 0; i < remove.size; i++){
            ghostWarheadSiloData.remove(remove.get(i));
        }
    }

    private static void updateGhostTacticalNukes(){
        IntSeq remove = new IntSeq();

        for(IntMap.Entry<GhostTacticalNukeData> entry : ghostTacticalNukeData.entries()){
            int unitId = entry.key;
            GhostTacticalNukeData data = entry.value;
            Unit unit = Groups.unit.getByID(unitId);
            Team team = Team.get(data.teamId);

            if(unit == null || !unit.isValid() || !isGhost(unit)){
                if(data.active && !data.missileFalling){
                    refundReservedGhostWarhead(data.teamId, data.reservedSiloPos);
                }
                remove.add(unitId);
                continue;
            }

            if(!data.active){
                remove.add(unitId);
                continue;
            }

            if(ravenMatrixDisabled(unit) && !data.missileFalling){
                refundReservedGhostWarhead(data.teamId, data.reservedSiloPos);
                data.active = false;
                remove.add(unitId);
                continue;
            }

            if(!data.missileFalling){
                boolean facing = faceTargetedAbilityPoint(unit, data.target.x, data.target.y);

                if(!unit.within(data.target, ghostTacticalNukeRange)){
                    data.delayTime = ghostTacticalNukeDelay;

                    if(unit.controller() instanceof CommandAI ai){
                        ai.command(UnitCommand.moveCommand);
                        ai.commandPosition(Tmp.v2.set(data.target), false);
                    }

                    continue;
                }

                holdForTargetedAbility(unit);
                if(!facing) continue;

                data.delayTime = Math.max(0f, data.delayTime - Time.delta);
                if(data.delayTime <= 0.001f){
                    data.missileFalling = true;
                    data.missileTime = ghostTacticalNukeMissileFallTime;
                }
            }else{
                data.missileTime = Math.max(0f, data.missileTime - Time.delta);
                if(data.missileTime <= 0.001f){
                    impactGhostTacticalNuke(team, data.target.x, data.target.y);
                    data.active = false;
                    remove.add(unitId);
                }
            }
        }

        for(int i = 0; i < remove.size; i++){
            ghostTacticalNukeData.remove(remove.get(i));
        }
    }

    private static void impactGhostTacticalNuke(@Nullable Team team, float x, float y){
        float radius = ghostTacticalNukeDamageRadius;
        float safeRadius = Math.max(radius, 0.001f);

        Units.nearby(x - radius, y - radius, radius * 2f, radius * 2f, u -> {
            if(u == null || !u.isValid()) return;
            if(!u.checkTarget(true, true) || !u.hittable()) return;
            float maxDst = radius + u.hitSize / 2f;
            float dst = Mathf.dst(x, y, u.x, u.y);
            if(dst > maxDst) return;
            float frac = Mathf.clamp(dst / safeRadius);
            float damage = Mathf.lerp(ghostTacticalNukeCenterDamage, ghostTacticalNukeEdgeDamage, frac);
            u.damage(damage);
        });

        Units.nearbyBuildings(x, y, radius + 16f, build -> {
            if(build == null || !build.isValid()) return;
            float maxDst = radius + build.hitSize() / 2f;
            float dst = Mathf.dst(x, y, build.x, build.y);
            if(dst > maxDst) return;
            float frac = Mathf.clamp(dst / safeRadius);
            float damage = Mathf.lerp(ghostTacticalNukeCenterDamage, ghostTacticalNukeEdgeDamage, frac) + ghostTacticalNukeBuildingBonus;
            build.damage(damage);
        });

        Fx.massiveExplosion.at(x, y);
        Fx.dynamicExplosion.at(x, y, radius / tilesize, Color.valueOf("ff5656"));
        Effect.shake(8f, 8f, x, y);
    }

    private static void drawGhostTacticalNukeOverlay(){
        if(ghostTacticalNukeData.isEmpty()) return;

        Tmp.m1.set(Draw.proj());
        Draw.proj(Core.camera);
        Draw.sort(false);

        TextureRegion missileRegion = Core.atlas.find("missile-large");

        for(IntMap.Entry<GhostTacticalNukeData> entry : ghostTacticalNukeData.entries()){
            GhostTacticalNukeData data = entry.value;
            if(data == null || !data.active) continue;

            float x = data.target.x;
            float y = data.target.y;
            float pulse = 0.78f + Mathf.absin(Time.time, 5f, 0.22f);

            Draw.z(Layer.effect + 5f);
            Draw.color(1f, 0.12f, 0.12f, 0.25f * pulse);
            Fill.circle(x, y, ghostTacticalNukeMarkerRadius);
            Lines.stroke(1.45f);
            Draw.color(1f, 0.22f, 0.22f, 0.95f);
            Lines.circle(x, y, ghostTacticalNukeMarkerRadius);

            if(data.missileFalling && missileRegion.found()){
                float fin = 1f - Mathf.clamp(data.missileTime / ghostTacticalNukeMissileFallTime);
                float drop = Mathf.lerp(230f, 0f, Interp.pow2In.apply(fin));
                float scale = 1.35f;
                Draw.z(Layer.effect + 6f);
                Draw.color(Color.white);
                Draw.rect(missileRegion, x, y + drop,
                    missileRegion.width * missileRegion.scl() * scale,
                    missileRegion.height * missileRegion.scl() * scale,
                    -90f);
                Drawf.light(x, y + drop, 34f, Pal.missileYellowBack, 0.55f * (1f - fin));
            }
        }

        Draw.flush();
        Draw.proj(Tmp.m1);
    }

    private static void updateInfantryArmorBonus(){
        for(Unit unit : Groups.unit){
            if(unit == null || !unit.isValid()) continue;
            if(isBarracksInfantryType(unit.type)){
                unit.statusArmor(unit.type.armor + infantryArmorBonus(unit.team));
            }else if(isHeavyFactoryOrStarportType(unit.type)){
                unit.statusArmor(unit.type.armor + vehicleArmorBonus(unit.team));
            }
        }
    }

    private static boolean isBarracksInfantryType(@Nullable UnitType type){
        return type == dagger || type == reaper || type == fortress || type == ghost;
    }

    private static boolean isHeavyFactoryOrStarportType(@Nullable UnitType type){
        return type == mace || type == locus || type == hurricane || type == precept || type == scepter || type == crawler
            || type == flare || type == mega || type == liberator || type == avert || type == horizon || type == antumbra;
    }

    public static boolean infantryWeaponHasArmory(@Nullable Team team){
        return team != null && team.data().getCount(Blocks.siliconCrucible) > 0;
    }

    public static boolean vehicleWeaponHasArmory(@Nullable Team team){
        return infantryWeaponHasArmory(team);
    }

    public static boolean ghostCamoHasAcademy(@Nullable Team team){
        return team != null && team.data().getCount(Blocks.launchPad) > 0;
    }

    public static boolean barracksTeamHasTechAddon(@Nullable Team team){
        if(team == null) return false;
        for(Building build : Groups.build){
            if(build == null || !build.isValid() || build.team != team) continue;
            if(!(build instanceof UnitFactory.UnitFactoryBuild factory)) continue;
            if(factory.block != Blocks.groundFactory) continue;
            if(factory.hasTechAddon()) return true;
        }
        return false;
    }

    public static boolean heavyFactoryTeamHasTechAddon(@Nullable Team team){
        if(team == null) return false;
        for(Building build : Groups.build){
            if(build == null || !build.isValid() || build.team != team) continue;
            if(!(build instanceof UnitFactory.UnitFactoryBuild factory)) continue;
            if(factory.block != Blocks.tankFabricator) continue;
            if(factory.hasTechAddon()) return true;
        }
        return false;
    }

    public static boolean starportTeamHasTechAddon(@Nullable Team team){
        if(team == null) return false;
        for(Building build : Groups.build){
            if(build == null || !build.isValid() || build.team != team) continue;
            if(!(build instanceof UnitFactory.UnitFactoryBuild factory)) continue;
            if(factory.block != Blocks.shipFabricator) continue;
            if(factory.hasTechAddon()) return true;
        }
        return false;
    }

    public static boolean fusionCoreHas(@Nullable Team team){
        return team != null && team.data().getCount(Blocks.surgeCrucible) > 0;
    }

    public static boolean barracksTechAnyResearching(@Nullable Team team){
        return barracksActiveResearching(team) || barracksQueuedCount(team) > 0;
    }

    public static boolean heavyFactoryTechAnyResearching(@Nullable Team team){
        return heavyFactoryActiveResearching(team) || heavyFactoryQueuedCount(team) > 0;
    }

    public static boolean starportTechAnyResearching(@Nullable Team team){
        return starportActiveResearching(team) || starportQueuedCount(team) > 0;
    }

    public static boolean fusionCoreAnyResearching(@Nullable Team team){
        return fusionCoreActiveResearching(team) || fusionCoreQueuedCount(team) > 0;
    }

    public static boolean factoryTechResearching(@Nullable UnitFactory.UnitFactoryBuild factory){
        if(factory == null || !factory.isValid() || !factory.hasTechAddon()) return false;
        if(factory.block == Blocks.groundFactory) return barracksTechAnyResearching(factory.team);
        if(factory.block == Blocks.tankFabricator) return heavyFactoryTechAnyResearching(factory.team);
        if(factory.block == Blocks.shipFabricator) return starportTechAnyResearching(factory.team);
        return false;
    }

    public static boolean infantryAnyResearching(@Nullable Team team){
        return engineeringActiveResearching(team) || engineeringQueuedCount(team) > 0;
    }

    public static boolean engineeringAnyResearching(@Nullable Team team){
        return infantryAnyResearching(team);
    }

    public static int infantryWeaponLevel(@Nullable Team team){
        InfantryWeaponData data = getInfantryWeaponData(team, false);
        return data == null ? 0 : Mathf.clamp(data.level, 0, infantryWeaponMaxLevel);
    }

    public static int vehicleWeaponLevel(@Nullable Team team){
        VehicleWeaponData data = getVehicleWeaponData(team, false);
        return data == null ? 0 : Mathf.clamp(data.level, 0, vehicleWeaponMaxLevel);
    }

    public static int shipWeaponLevel(@Nullable Team team){
        ShipWeaponData data = getShipWeaponData(team, false);
        return data == null ? 0 : Mathf.clamp(data.level, 0, shipWeaponMaxLevel);
    }

    public static int vehicleArmorLevel(@Nullable Team team){
        VehicleArmorData data = getVehicleArmorData(team, false);
        return data == null ? 0 : Mathf.clamp(data.level, 0, vehicleArmorMaxLevel);
    }

    public static int infantryArmorLevel(@Nullable Team team){
        InfantryArmorData data = getInfantryArmorData(team, false);
        return data == null ? 0 : Mathf.clamp(data.level, 0, infantryWeaponMaxLevel);
    }

    public static int instantTrackingLevel(@Nullable Team team){
        InstantTrackingData data = getInstantTrackingData(team, false);
        return data == null ? 0 : Mathf.clamp(data.level, 0, 1);
    }

    public static int barracksBlastShieldLevel(@Nullable Team team){
        BarracksBlastShieldData data = getBarracksBlastShieldData(team, false);
        return data == null ? 0 : Mathf.clamp(data.level, 0, 1);
    }

    public static int barracksStimpackLevel(@Nullable Team team){
        BarracksStimpackData data = getBarracksStimpackData(team, false);
        return data == null ? 0 : Mathf.clamp(data.level, 0, 1);
    }

    public static int barracksConcussiveLevel(@Nullable Team team){
        BarracksConcussiveData data = getBarracksConcussiveData(team, false);
        return data == null ? 0 : Mathf.clamp(data.level, 0, 1);
    }

    public static int infernoPreheaterLevel(@Nullable Team team){
        InfernoPreheaterData data = getInfernoPreheaterData(team, false);
        return data == null ? 0 : Mathf.clamp(data.level, 0, 1);
    }

    public static int electromagneticFieldAcceleratorLevel(@Nullable Team team){
        ElectromagneticFieldAcceleratorData data = getElectromagneticFieldAcceleratorData(team, false);
        return data == null ? 0 : Mathf.clamp(data.level, 0, 1);
    }

    public static int drillClawLevel(@Nullable Team team){
        DrillClawData data = getDrillClawData(team, false);
        return data == null ? 0 : Mathf.clamp(data.level, 0, 1);
    }

    public static int smartServosLevel(@Nullable Team team){
        SmartServosData data = getSmartServosData(team, false);
        return data == null ? 0 : Mathf.clamp(data.level, 0, 1);
    }

    public static int bansheeCloakFieldLevel(@Nullable Team team){
        BansheeCloakFieldData data = getBansheeCloakFieldData(team, false);
        return data == null ? 0 : Mathf.clamp(data.level, 0, 1);
    }

    public static int bansheeAfterburnerLevel(@Nullable Team team){
        BansheeAfterburnerData data = getBansheeAfterburnerData(team, false);
        return data == null ? 0 : Mathf.clamp(data.level, 0, 1);
    }

    public static int ravenMatrixTechLevel(@Nullable Team team){
        RavenMatrixTechData data = getRavenMatrixTechData(team, false);
        return data == null ? 0 : Mathf.clamp(data.level, 0, 1);
    }

    public static int battlecruiserWeaponRefitLevel(@Nullable Team team){
        BattlecruiserWeaponRefitData data = getBattlecruiserWeaponRefitData(team, false);
        return data == null ? 0 : Mathf.clamp(data.level, 0, 1);
    }

    public static int medivacCaduceusReactorLevel(@Nullable Team team){
        MedivacCaduceusReactorData data = getMedivacCaduceusReactorData(team, false);
        return data == null ? 0 : Mathf.clamp(data.level, 0, 1);
    }

    public static int liberatorAdvancedBallisticsLevel(@Nullable Team team){
        LiberatorAdvancedBallisticsData data = getLiberatorAdvancedBallisticsData(team, false);
        return data == null ? 0 : Mathf.clamp(data.level, 0, 1);
    }

    public static int steelArmorLevel(@Nullable Team team){
        SteelArmorData data = getSteelArmorData(team, false);
        return data == null ? 0 : Mathf.clamp(data.level, 0, 1);
    }

    public static int ghostCamoLevel(@Nullable Team team){
        GhostCamoData data = getGhostCamoData(team, false);
        return data == null ? 0 : Mathf.clamp(data.level, 0, 1);
    }

    public static int infantryWeaponResearchingLevel(@Nullable Team team){
        InfantryWeaponData data = getInfantryWeaponData(team, false);
        if(data == null) return 0;
        return data.researchingLevel >= 1 && data.researchingLevel <= infantryWeaponMaxLevel ? data.researchingLevel : 0;
    }

    public static int vehicleWeaponResearchingLevel(@Nullable Team team){
        VehicleWeaponData data = getVehicleWeaponData(team, false);
        if(data == null) return 0;
        return data.researchingLevel >= 1 && data.researchingLevel <= vehicleWeaponMaxLevel ? data.researchingLevel : 0;
    }

    public static int shipWeaponResearchingLevel(@Nullable Team team){
        ShipWeaponData data = getShipWeaponData(team, false);
        if(data == null) return 0;
        return data.researchingLevel >= 1 && data.researchingLevel <= shipWeaponMaxLevel ? data.researchingLevel : 0;
    }

    public static int vehicleArmorResearchingLevel(@Nullable Team team){
        VehicleArmorData data = getVehicleArmorData(team, false);
        if(data == null) return 0;
        return data.researchingLevel >= 1 && data.researchingLevel <= vehicleArmorMaxLevel ? data.researchingLevel : 0;
    }

    public static int infantryArmorResearchingLevel(@Nullable Team team){
        InfantryArmorData data = getInfantryArmorData(team, false);
        if(data == null) return 0;
        return data.researchingLevel >= 1 && data.researchingLevel <= infantryWeaponMaxLevel ? data.researchingLevel : 0;
    }

    public static boolean infantryWeaponResearching(@Nullable Team team){
        return infantryWeaponResearchingLevel(team) > 0;
    }

    public static boolean vehicleWeaponResearching(@Nullable Team team){
        return vehicleWeaponResearchingLevel(team) > 0;
    }

    public static boolean shipWeaponResearching(@Nullable Team team){
        return shipWeaponResearchingLevel(team) > 0;
    }

    public static boolean vehicleArmorResearching(@Nullable Team team){
        return vehicleArmorResearchingLevel(team) > 0;
    }

    public static boolean armoryAnyResearching(@Nullable Team team){
        return armoryActiveResearching(team) || armoryQueuedCount(team) > 0;
    }

    public static boolean infantryArmorResearching(@Nullable Team team){
        return infantryArmorResearchingLevel(team) > 0;
    }

    public static boolean instantTrackingResearching(@Nullable Team team){
        InstantTrackingData data = getInstantTrackingData(team, false);
        return data != null && data.researching;
    }

    public static boolean steelArmorResearching(@Nullable Team team){
        SteelArmorData data = getSteelArmorData(team, false);
        return data != null && data.researching;
    }

    public static boolean ghostCamoResearching(@Nullable Team team){
        GhostCamoData data = getGhostCamoData(team, false);
        return data != null && data.researching;
    }

    public static boolean bansheeCloakFieldResearching(@Nullable Team team){
        BansheeCloakFieldData data = getBansheeCloakFieldData(team, false);
        return data != null && data.researching;
    }

    public static boolean bansheeAfterburnerResearching(@Nullable Team team){
        BansheeAfterburnerData data = getBansheeAfterburnerData(team, false);
        return data != null && data.researching;
    }

    public static boolean ravenMatrixTechResearching(@Nullable Team team){
        RavenMatrixTechData data = getRavenMatrixTechData(team, false);
        return data != null && data.researching;
    }

    public static boolean battlecruiserWeaponRefitResearching(@Nullable Team team){
        BattlecruiserWeaponRefitData data = getBattlecruiserWeaponRefitData(team, false);
        return data != null && data.researching;
    }

    public static boolean medivacCaduceusReactorResearching(@Nullable Team team){
        MedivacCaduceusReactorData data = getMedivacCaduceusReactorData(team, false);
        return data != null && data.researching;
    }

    public static boolean liberatorAdvancedBallisticsResearching(@Nullable Team team){
        LiberatorAdvancedBallisticsData data = getLiberatorAdvancedBallisticsData(team, false);
        return data != null && data.researching;
    }

    public static boolean barracksBlastShieldResearching(@Nullable Team team){
        BarracksBlastShieldData data = getBarracksBlastShieldData(team, false);
        return data != null && data.researching;
    }

    public static boolean barracksStimpackResearching(@Nullable Team team){
        BarracksStimpackData data = getBarracksStimpackData(team, false);
        return data != null && data.researching;
    }

    public static boolean barracksConcussiveResearching(@Nullable Team team){
        BarracksConcussiveData data = getBarracksConcussiveData(team, false);
        return data != null && data.researching;
    }

    public static boolean infernoPreheaterResearching(@Nullable Team team){
        InfernoPreheaterData data = getInfernoPreheaterData(team, false);
        return data != null && data.researching;
    }

    public static boolean electromagneticFieldAcceleratorResearching(@Nullable Team team){
        ElectromagneticFieldAcceleratorData data = getElectromagneticFieldAcceleratorData(team, false);
        return data != null && data.researching;
    }

    public static boolean drillClawResearching(@Nullable Team team){
        DrillClawData data = getDrillClawData(team, false);
        return data != null && data.researching;
    }

    public static boolean smartServosResearching(@Nullable Team team){
        SmartServosData data = getSmartServosData(team, false);
        return data != null && data.researching;
    }

    public static float infantryWeaponResearchProgress(@Nullable Team team){
        InfantryWeaponData data = getInfantryWeaponData(team, false);
        if(data == null) return 0f;
        int level = infantryWeaponResearchingLevel(team);
        if(level <= 0) return 0f;
        return Mathf.clamp(data.researchTime / infantryWeaponResearchTime[level]);
    }

    public static float vehicleWeaponResearchProgress(@Nullable Team team){
        VehicleWeaponData data = getVehicleWeaponData(team, false);
        if(data == null) return 0f;
        int level = vehicleWeaponResearchingLevel(team);
        if(level <= 0) return 0f;
        return Mathf.clamp(data.researchTime / vehicleWeaponResearchTime[level]);
    }

    public static float shipWeaponResearchProgress(@Nullable Team team){
        ShipWeaponData data = getShipWeaponData(team, false);
        if(data == null) return 0f;
        int level = shipWeaponResearchingLevel(team);
        if(level <= 0) return 0f;
        return Mathf.clamp(data.researchTime / shipWeaponResearchTime[level]);
    }

    public static float vehicleArmorResearchProgress(@Nullable Team team){
        VehicleArmorData data = getVehicleArmorData(team, false);
        if(data == null) return 0f;
        int level = vehicleArmorResearchingLevel(team);
        if(level <= 0) return 0f;
        return Mathf.clamp(data.researchTime / vehicleArmorResearchTime[level]);
    }

    public static float infantryArmorResearchProgress(@Nullable Team team){
        InfantryArmorData data = getInfantryArmorData(team, false);
        if(data == null) return 0f;
        int level = infantryArmorResearchingLevel(team);
        if(level <= 0) return 0f;
        return Mathf.clamp(data.researchTime / infantryWeaponResearchTime[level]);
    }

    public static float instantTrackingResearchProgress(@Nullable Team team){
        InstantTrackingData data = getInstantTrackingData(team, false);
        if(data == null || !data.researching) return 0f;
        return Mathf.clamp(data.researchTime / instantTrackingResearchTime);
    }

    public static float steelArmorResearchProgress(@Nullable Team team){
        SteelArmorData data = getSteelArmorData(team, false);
        if(data == null || !data.researching) return 0f;
        return Mathf.clamp(data.researchTime / steelArmorResearchTime);
    }

    public static float ghostCamoResearchProgress(@Nullable Team team){
        GhostCamoData data = getGhostCamoData(team, false);
        if(data == null || !data.researching) return 0f;
        return Mathf.clamp(data.researchTime / ghostCamoResearchTime);
    }

    public static float bansheeCloakFieldResearchProgress(@Nullable Team team){
        BansheeCloakFieldData data = getBansheeCloakFieldData(team, false);
        if(data == null || !data.researching) return 0f;
        return Mathf.clamp(data.researchTime / bansheeCloakFieldResearchTime);
    }

    public static float bansheeAfterburnerResearchProgress(@Nullable Team team){
        BansheeAfterburnerData data = getBansheeAfterburnerData(team, false);
        if(data == null || !data.researching) return 0f;
        return Mathf.clamp(data.researchTime / bansheeAfterburnerResearchTime);
    }

    public static float ravenMatrixTechResearchProgress(@Nullable Team team){
        RavenMatrixTechData data = getRavenMatrixTechData(team, false);
        if(data == null || !data.researching) return 0f;
        return Mathf.clamp(data.researchTime / ravenMatrixTechResearchTime);
    }

    public static float battlecruiserWeaponRefitResearchProgress(@Nullable Team team){
        BattlecruiserWeaponRefitData data = getBattlecruiserWeaponRefitData(team, false);
        if(data == null || !data.researching) return 0f;
        return Mathf.clamp(data.researchTime / battlecruiserWeaponRefitResearchTime);
    }

    public static float medivacCaduceusReactorResearchProgress(@Nullable Team team){
        MedivacCaduceusReactorData data = getMedivacCaduceusReactorData(team, false);
        if(data == null || !data.researching) return 0f;
        return Mathf.clamp(data.researchTime / medivacCaduceusReactorResearchTime);
    }

    public static float liberatorAdvancedBallisticsResearchProgress(@Nullable Team team){
        LiberatorAdvancedBallisticsData data = getLiberatorAdvancedBallisticsData(team, false);
        if(data == null || !data.researching) return 0f;
        return Mathf.clamp(data.researchTime / liberatorAdvancedBallisticsResearchTime);
    }

    public static float barracksBlastShieldResearchProgress(@Nullable Team team){
        BarracksBlastShieldData data = getBarracksBlastShieldData(team, false);
        if(data == null || !data.researching) return 0f;
        return Mathf.clamp(data.researchTime / barracksBlastShieldResearchTime);
    }

    public static float barracksStimpackResearchProgress(@Nullable Team team){
        BarracksStimpackData data = getBarracksStimpackData(team, false);
        if(data == null || !data.researching) return 0f;
        return Mathf.clamp(data.researchTime / barracksStimpackResearchTime);
    }

    public static float barracksConcussiveResearchProgress(@Nullable Team team){
        BarracksConcussiveData data = getBarracksConcussiveData(team, false);
        if(data == null || !data.researching) return 0f;
        return Mathf.clamp(data.researchTime / barracksConcussiveResearchTime);
    }

    public static float infernoPreheaterResearchProgress(@Nullable Team team){
        InfernoPreheaterData data = getInfernoPreheaterData(team, false);
        if(data == null || !data.researching) return 0f;
        return Mathf.clamp(data.researchTime / infernoPreheaterResearchTime);
    }

    public static float electromagneticFieldAcceleratorResearchProgress(@Nullable Team team){
        ElectromagneticFieldAcceleratorData data = getElectromagneticFieldAcceleratorData(team, false);
        if(data == null || !data.researching) return 0f;
        return Mathf.clamp(data.researchTime / electromagneticFieldAcceleratorResearchTime);
    }

    public static float drillClawResearchProgress(@Nullable Team team){
        DrillClawData data = getDrillClawData(team, false);
        if(data == null || !data.researching) return 0f;
        return Mathf.clamp(data.researchTime / drillClawResearchTime);
    }

    public static float smartServosResearchProgress(@Nullable Team team){
        SmartServosData data = getSmartServosData(team, false);
        if(data == null || !data.researching) return 0f;
        return Mathf.clamp(data.researchTime / smartServosResearchTime);
    }

    public static int infantryWeaponDisplayLevel(@Nullable Team team){
        int level = plannedInfantryWeaponLevel(team);
        if(level >= infantryWeaponMaxLevel) return -1;
        return level + 1;
    }

    public static int vehicleWeaponDisplayLevel(@Nullable Team team){
        int level = plannedVehicleWeaponLevel(team);
        if(level >= vehicleWeaponMaxLevel) return -1;
        return level + 1;
    }

    public static int shipWeaponDisplayLevel(@Nullable Team team){
        int level = plannedShipWeaponLevel(team);
        if(level >= shipWeaponMaxLevel) return -1;
        return level + 1;
    }

    public static int vehicleArmorDisplayLevel(@Nullable Team team){
        int level = plannedVehicleArmorLevel(team);
        if(level >= vehicleArmorMaxLevel) return -1;
        return level + 1;
    }

    public static int infantryArmorDisplayLevel(@Nullable Team team){
        int level = plannedInfantryArmorLevel(team);
        if(level >= infantryWeaponMaxLevel) return -1;
        return level + 1;
    }

    public static boolean instantTrackingDisplayAvailable(@Nullable Team team){
        return !instantTrackingPlanned(team);
    }

    public static boolean steelArmorDisplayAvailable(@Nullable Team team){
        return !steelArmorPlanned(team);
    }

    public static boolean barracksBlastShieldDisplayAvailable(@Nullable Team team){
        return !barracksBlastShieldPlanned(team);
    }

    public static boolean barracksStimpackDisplayAvailable(@Nullable Team team){
        return !barracksStimpackPlanned(team);
    }

    public static boolean barracksConcussiveDisplayAvailable(@Nullable Team team){
        return !barracksConcussivePlanned(team);
    }

    public static boolean infernoPreheaterDisplayAvailable(@Nullable Team team){
        return !infernoPreheaterPlanned(team);
    }

    public static boolean electromagneticFieldAcceleratorDisplayAvailable(@Nullable Team team){
        return !electromagneticFieldAcceleratorPlanned(team);
    }

    public static boolean drillClawDisplayAvailable(@Nullable Team team){
        return !drillClawPlanned(team);
    }

    public static boolean smartServosDisplayAvailable(@Nullable Team team){
        return !smartServosPlanned(team);
    }

    public static boolean bansheeCloakFieldDisplayAvailable(@Nullable Team team){
        return !bansheeCloakFieldPlanned(team);
    }

    public static boolean bansheeAfterburnerDisplayAvailable(@Nullable Team team){
        return !bansheeAfterburnerPlanned(team);
    }

    public static boolean ravenMatrixTechDisplayAvailable(@Nullable Team team){
        return !ravenMatrixTechPlanned(team);
    }

    public static boolean battlecruiserWeaponRefitDisplayAvailable(@Nullable Team team){
        return !battlecruiserWeaponRefitPlanned(team);
    }

    public static boolean medivacCaduceusReactorDisplayAvailable(@Nullable Team team){
        return !medivacCaduceusReactorPlanned(team);
    }

    public static boolean liberatorAdvancedBallisticsDisplayAvailable(@Nullable Team team){
        return !liberatorAdvancedBallisticsPlanned(team);
    }

    public static int infantryWeaponCrystalCost(int level){
        return level >= 1 && level <= infantryWeaponMaxLevel ? infantryWeaponCrystalCost[level] : 0;
    }

    public static int vehicleWeaponCrystalCost(int level){
        return level >= 1 && level <= vehicleWeaponMaxLevel ? vehicleWeaponCrystalCost[level] : 0;
    }

    public static int shipWeaponCrystalCost(int level){
        return level >= 1 && level <= shipWeaponMaxLevel ? shipWeaponCrystalCost[level] : 0;
    }

    public static int vehicleArmorCrystalCost(int level){
        return level >= 1 && level <= vehicleArmorMaxLevel ? vehicleArmorCrystalCost[level] : 0;
    }

    public static int infantryWeaponGasCost(int level){
        return level >= 1 && level <= infantryWeaponMaxLevel ? infantryWeaponGasCost[level] : 0;
    }

    public static int vehicleWeaponGasCost(int level){
        return level >= 1 && level <= vehicleWeaponMaxLevel ? vehicleWeaponGasCost[level] : 0;
    }

    public static int shipWeaponGasCost(int level){
        return level >= 1 && level <= shipWeaponMaxLevel ? shipWeaponGasCost[level] : 0;
    }

    public static int vehicleArmorGasCost(int level){
        return level >= 1 && level <= vehicleArmorMaxLevel ? vehicleArmorGasCost[level] : 0;
    }

    public static float infantryWeaponResearchDuration(int level){
        return level >= 1 && level <= infantryWeaponMaxLevel ? infantryWeaponResearchTime[level] : 0f;
    }

    public static float vehicleWeaponResearchDuration(int level){
        return level >= 1 && level <= vehicleWeaponMaxLevel ? vehicleWeaponResearchTime[level] : 0f;
    }

    public static float shipWeaponResearchDuration(int level){
        return level >= 1 && level <= shipWeaponMaxLevel ? shipWeaponResearchTime[level] : 0f;
    }

    public static float vehicleArmorResearchDuration(int level){
        return level >= 1 && level <= vehicleArmorMaxLevel ? vehicleArmorResearchTime[level] : 0f;
    }

    private static boolean infantryUpgradeCanAfford(@Nullable Team team, int level){
        if(team == null) return false;
        Building core = team.core();
        if(core == null) return false;
        return core.items.has(Items.graphite, infantryWeaponCrystalCost(level))
            && core.items.has(Items.highEnergyGas, infantryWeaponGasCost(level));
    }

    private static boolean vehicleWeaponCanAfford(@Nullable Team team, int level){
        if(team == null) return false;
        Building core = team.core();
        if(core == null) return false;
        return core.items.has(Items.graphite, vehicleWeaponCrystalCost(level))
            && core.items.has(Items.highEnergyGas, vehicleWeaponGasCost(level));
    }

    private static boolean shipWeaponCanAfford(@Nullable Team team, int level){
        if(team == null) return false;
        Building core = team.core();
        if(core == null) return false;
        return core.items.has(Items.graphite, shipWeaponCrystalCost(level))
            && core.items.has(Items.highEnergyGas, shipWeaponGasCost(level));
    }

    private static boolean vehicleArmorCanAfford(@Nullable Team team, int level){
        if(team == null) return false;
        Building core = team.core();
        if(core == null) return false;
        return core.items.has(Items.graphite, vehicleArmorCrystalCost(level))
            && core.items.has(Items.highEnergyGas, vehicleArmorGasCost(level));
    }

    private static boolean instantTrackingCanAfford(@Nullable Team team){
        if(team == null) return false;
        Building core = team.core();
        if(core == null) return false;
        return core.items.has(Items.graphite, instantTrackingCrystalCost)
            && core.items.has(Items.highEnergyGas, instantTrackingGasCost);
    }

    private static boolean steelArmorCanAfford(@Nullable Team team){
        if(team == null) return false;
        Building core = team.core();
        if(core == null) return false;
        return core.items.has(Items.graphite, steelArmorCrystalCost)
            && core.items.has(Items.highEnergyGas, steelArmorGasCost);
    }

    private static boolean ghostCamoCanAfford(@Nullable Team team){
        if(team == null) return false;
        Building core = team.core();
        if(core == null) return false;
        return core.items.has(Items.graphite, ghostCamoCrystalCost)
            && core.items.has(Items.highEnergyGas, ghostCamoGasCost);
    }

    private static boolean barracksTechCanAfford(@Nullable Team team, int crystalCost, int gasCost){
        if(team == null) return false;
        Building core = team.core();
        if(core == null) return false;
        return core.items.has(Items.graphite, crystalCost)
            && core.items.has(Items.highEnergyGas, gasCost);
    }

    private static void infantryUpgradeConsume(@Nullable Team team, int level){
        if(team == null) return;
        Building core = team.core();
        if(core == null) return;
        core.items.remove(Items.graphite, infantryWeaponCrystalCost(level));
        core.items.remove(Items.highEnergyGas, infantryWeaponGasCost(level));
    }

    private static void vehicleWeaponConsume(@Nullable Team team, int level){
        if(team == null) return;
        Building core = team.core();
        if(core == null) return;
        core.items.remove(Items.graphite, vehicleWeaponCrystalCost(level));
        core.items.remove(Items.highEnergyGas, vehicleWeaponGasCost(level));
    }

    private static void shipWeaponConsume(@Nullable Team team, int level){
        if(team == null) return;
        Building core = team.core();
        if(core == null) return;
        core.items.remove(Items.graphite, shipWeaponCrystalCost(level));
        core.items.remove(Items.highEnergyGas, shipWeaponGasCost(level));
    }

    private static void vehicleArmorConsume(@Nullable Team team, int level){
        if(team == null) return;
        Building core = team.core();
        if(core == null) return;
        core.items.remove(Items.graphite, vehicleArmorCrystalCost(level));
        core.items.remove(Items.highEnergyGas, vehicleArmorGasCost(level));
    }

    private static void infantryUpgradeRefund(@Nullable Team team, int level){
        if(team == null) return;
        Building core = team.core();
        if(core == null) return;
        core.items.add(Items.graphite, infantryWeaponCrystalCost(level));
        core.items.add(Items.highEnergyGas, infantryWeaponGasCost(level));
    }

    private static void vehicleWeaponRefund(@Nullable Team team, int level){
        if(team == null) return;
        Building core = team.core();
        if(core == null) return;
        core.items.add(Items.graphite, vehicleWeaponCrystalCost(level));
        core.items.add(Items.highEnergyGas, vehicleWeaponGasCost(level));
    }

    private static void shipWeaponRefund(@Nullable Team team, int level){
        if(team == null) return;
        Building core = team.core();
        if(core == null) return;
        core.items.add(Items.graphite, shipWeaponCrystalCost(level));
        core.items.add(Items.highEnergyGas, shipWeaponGasCost(level));
    }

    private static void vehicleArmorRefund(@Nullable Team team, int level){
        if(team == null) return;
        Building core = team.core();
        if(core == null) return;
        core.items.add(Items.graphite, vehicleArmorCrystalCost(level));
        core.items.add(Items.highEnergyGas, vehicleArmorGasCost(level));
    }

    private static void instantTrackingConsume(@Nullable Team team){
        if(team == null) return;
        Building core = team.core();
        if(core == null) return;
        core.items.remove(Items.graphite, instantTrackingCrystalCost);
        core.items.remove(Items.highEnergyGas, instantTrackingGasCost);
    }

    private static void instantTrackingRefund(@Nullable Team team){
        if(team == null) return;
        Building core = team.core();
        if(core == null) return;
        core.items.add(Items.graphite, instantTrackingCrystalCost);
        core.items.add(Items.highEnergyGas, instantTrackingGasCost);
    }

    private static void steelArmorConsume(@Nullable Team team){
        if(team == null) return;
        Building core = team.core();
        if(core == null) return;
        core.items.remove(Items.graphite, steelArmorCrystalCost);
        core.items.remove(Items.highEnergyGas, steelArmorGasCost);
    }

    private static void steelArmorRefund(@Nullable Team team){
        if(team == null) return;
        Building core = team.core();
        if(core == null) return;
        core.items.add(Items.graphite, steelArmorCrystalCost);
        core.items.add(Items.highEnergyGas, steelArmorGasCost);
    }

    private static void ghostCamoConsume(@Nullable Team team){
        if(team == null) return;
        Building core = team.core();
        if(core == null) return;
        core.items.remove(Items.graphite, ghostCamoCrystalCost);
        core.items.remove(Items.highEnergyGas, ghostCamoGasCost);
    }

    private static void ghostCamoRefund(@Nullable Team team){
        if(team == null) return;
        Building core = team.core();
        if(core == null) return;
        core.items.add(Items.graphite, ghostCamoCrystalCost);
        core.items.add(Items.highEnergyGas, ghostCamoGasCost);
    }

    private static void barracksTechConsume(@Nullable Team team, int crystalCost, int gasCost){
        if(team == null) return;
        Building core = team.core();
        if(core == null) return;
        core.items.remove(Items.graphite, crystalCost);
        core.items.remove(Items.highEnergyGas, gasCost);
    }

    private static void barracksTechRefund(@Nullable Team team, int crystalCost, int gasCost){
        if(team == null) return;
        Building core = team.core();
        if(core == null) return;
        core.items.add(Items.graphite, crystalCost);
        core.items.add(Items.highEnergyGas, gasCost);
    }

    public static boolean infantryWeaponCanStartResearch(@Nullable Team team, int level){
        if(team == null || level < 1 || level > infantryWeaponMaxLevel) return false;
        if(level != plannedInfantryWeaponLevel(team) + 1) return false;
        if(!infantryWeaponHasArmory(team)) return false;
        return infantryUpgradeCanAfford(team, level);
    }

    public static boolean vehicleWeaponCanStartResearch(@Nullable Team team, int level){
        if(team == null || level < 1 || level > vehicleWeaponMaxLevel) return false;
        if(level != plannedVehicleWeaponLevel(team) + 1) return false;
        if(!vehicleWeaponHasArmory(team)) return false;
        return vehicleWeaponCanAfford(team, level);
    }

    public static boolean vehicleArmorCanStartResearch(@Nullable Team team, int level){
        if(team == null || level < 1 || level > vehicleArmorMaxLevel) return false;
        if(level != plannedVehicleArmorLevel(team) + 1) return false;
        if(!vehicleWeaponHasArmory(team)) return false;
        return vehicleArmorCanAfford(team, level);
    }

    public static boolean shipWeaponCanStartResearch(@Nullable Team team, int level){
        if(team == null || level < 1 || level > shipWeaponMaxLevel) return false;
        if(level != plannedShipWeaponLevel(team) + 1) return false;
        if(!vehicleWeaponHasArmory(team)) return false;
        return shipWeaponCanAfford(team, level);
    }

    public static boolean infantryArmorCanStartResearch(@Nullable Team team, int level){
        if(team == null || level < 1 || level > infantryWeaponMaxLevel) return false;
        if(level != plannedInfantryArmorLevel(team) + 1) return false;
        if(!infantryWeaponHasArmory(team)) return false;
        return infantryUpgradeCanAfford(team, level);
    }

    public static boolean instantTrackingCanStartResearch(@Nullable Team team){
        if(team == null) return false;
        if(instantTrackingPlanned(team)) return false;
        return instantTrackingCanAfford(team);
    }

    public static boolean steelArmorCanStartResearch(@Nullable Team team){
        if(team == null) return false;
        if(steelArmorPlanned(team)) return false;
        return steelArmorCanAfford(team);
    }

    public static boolean ghostCamoCanStartResearch(@Nullable Team team){
        if(team == null) return false;
        GhostCamoData data = getGhostCamoData(team, false);
        if(data != null && (data.level >= 1 || data.researching)) return false;
        if(!ghostCamoHasAcademy(team)) return false;
        return ghostCamoCanAfford(team);
    }

    public static boolean barracksBlastShieldCanStartResearch(@Nullable Team team){
        if(team == null) return false;
        if(barracksBlastShieldPlanned(team)) return false;
        if(!barracksTeamHasTechAddon(team)) return false;
        return barracksTechCanAfford(team, barracksBlastShieldCrystalCost, barracksBlastShieldGasCost);
    }

    public static boolean barracksStimpackCanStartResearch(@Nullable Team team){
        if(team == null) return false;
        if(barracksStimpackPlanned(team)) return false;
        if(!barracksTeamHasTechAddon(team)) return false;
        return barracksTechCanAfford(team, barracksStimpackCrystalCost, barracksStimpackGasCost);
    }

    public static boolean barracksConcussiveCanStartResearch(@Nullable Team team){
        if(team == null) return false;
        if(barracksConcussivePlanned(team)) return false;
        if(!barracksTeamHasTechAddon(team)) return false;
        return barracksTechCanAfford(team, barracksConcussiveCrystalCost, barracksConcussiveGasCost);
    }

    public static boolean infernoPreheaterCanStartResearch(@Nullable Team team){
        if(team == null) return false;
        if(infernoPreheaterPlanned(team)) return false;
        if(!heavyFactoryTeamHasTechAddon(team)) return false;
        return barracksTechCanAfford(team, infernoPreheaterCrystalCost, infernoPreheaterGasCost);
    }

    public static boolean electromagneticFieldAcceleratorCanStartResearch(@Nullable Team team){
        if(team == null) return false;
        if(electromagneticFieldAcceleratorPlanned(team)) return false;
        if(!heavyFactoryTeamHasTechAddon(team)) return false;
        return barracksTechCanAfford(team, electromagneticFieldAcceleratorCrystalCost, electromagneticFieldAcceleratorGasCost);
    }

    public static boolean drillClawCanStartResearch(@Nullable Team team){
        if(team == null) return false;
        if(drillClawPlanned(team)) return false;
        if(!heavyFactoryTeamHasTechAddon(team)) return false;
        return barracksTechCanAfford(team, drillClawCrystalCost, drillClawGasCost);
    }

    public static boolean smartServosCanStartResearch(@Nullable Team team){
        if(team == null) return false;
        if(smartServosPlanned(team)) return false;
        if(!heavyFactoryTeamHasTechAddon(team)) return false;
        return barracksTechCanAfford(team, smartServosCrystalCost, smartServosGasCost);
    }

    public static boolean bansheeCloakFieldCanStartResearch(@Nullable Team team){
        if(team == null) return false;
        if(bansheeCloakFieldPlanned(team)) return false;
        if(!starportTeamHasTechAddon(team)) return false;
        return barracksTechCanAfford(team, bansheeCloakFieldCrystalCost, bansheeCloakFieldGasCost);
    }

    public static boolean bansheeAfterburnerCanStartResearch(@Nullable Team team){
        if(team == null) return false;
        if(bansheeAfterburnerPlanned(team)) return false;
        if(!starportTeamHasTechAddon(team)) return false;
        return barracksTechCanAfford(team, bansheeAfterburnerCrystalCost, bansheeAfterburnerGasCost);
    }

    public static boolean ravenMatrixTechCanStartResearch(@Nullable Team team){
        if(team == null) return false;
        if(ravenMatrixTechPlanned(team)) return false;
        if(!starportTeamHasTechAddon(team)) return false;
        return barracksTechCanAfford(team, ravenMatrixTechCrystalCost, ravenMatrixTechGasCost);
    }

    public static boolean battlecruiserWeaponRefitCanStartResearch(@Nullable Team team){
        if(team == null) return false;
        if(battlecruiserWeaponRefitPlanned(team)) return false;
        if(!fusionCoreHas(team)) return false;
        return barracksTechCanAfford(team, battlecruiserWeaponRefitCrystalCost, battlecruiserWeaponRefitGasCost);
    }

    public static boolean medivacCaduceusReactorCanStartResearch(@Nullable Team team){
        if(team == null) return false;
        if(medivacCaduceusReactorPlanned(team)) return false;
        if(!fusionCoreHas(team)) return false;
        return barracksTechCanAfford(team, medivacCaduceusReactorCrystalCost, medivacCaduceusReactorGasCost);
    }

    public static boolean liberatorAdvancedBallisticsCanStartResearch(@Nullable Team team){
        if(team == null) return false;
        if(liberatorAdvancedBallisticsPlanned(team)) return false;
        if(!fusionCoreHas(team)) return false;
        return barracksTechCanAfford(team, liberatorAdvancedBallisticsCrystalCost, liberatorAdvancedBallisticsGasCost);
    }

    public static boolean infantryWeaponStartResearch(@Nullable Team team, int level){
        return startLeveledResearchWithQueue(team, level,
        UnitTypes::infantryWeaponCanStartResearch,
        UnitTypes::infantryUpgradeConsume,
        UnitTypes::engineeringActiveResearching,
        engineeringResearchQueue,
        engineeringQueueInfantryWeapon,
        UnitTypes::startInfantryWeaponResearchNow,
        UnitTypes::infantryUpgradeRefund);
    }

    public static boolean vehicleWeaponStartResearch(@Nullable Team team, int level){
        return startLeveledResearchWithQueue(team, level,
        UnitTypes::vehicleWeaponCanStartResearch,
        UnitTypes::vehicleWeaponConsume,
        UnitTypes::armoryActiveResearching,
        armoryResearchQueue,
        armoryQueueVehicleWeapon,
        UnitTypes::startVehicleWeaponResearchNow,
        UnitTypes::vehicleWeaponRefund);
    }

    public static boolean vehicleArmorStartResearch(@Nullable Team team, int level){
        return startLeveledResearchWithQueue(team, level,
        UnitTypes::vehicleArmorCanStartResearch,
        UnitTypes::vehicleArmorConsume,
        UnitTypes::armoryActiveResearching,
        armoryResearchQueue,
        armoryQueueVehicleArmor,
        UnitTypes::startVehicleArmorResearchNow,
        UnitTypes::vehicleArmorRefund);
    }

    public static boolean shipWeaponStartResearch(@Nullable Team team, int level){
        return startLeveledResearchWithQueue(team, level,
        UnitTypes::shipWeaponCanStartResearch,
        UnitTypes::shipWeaponConsume,
        UnitTypes::armoryActiveResearching,
        armoryResearchQueue,
        armoryQueueShipWeapon,
        UnitTypes::startShipWeaponResearchNow,
        UnitTypes::shipWeaponRefund);
    }

    public static boolean infantryArmorStartResearch(@Nullable Team team, int level){
        return startLeveledResearchWithQueue(team, level,
        UnitTypes::infantryArmorCanStartResearch,
        UnitTypes::infantryUpgradeConsume,
        UnitTypes::engineeringActiveResearching,
        engineeringResearchQueue,
        engineeringQueueInfantryArmor,
        UnitTypes::startInfantryArmorResearchNow,
        UnitTypes::infantryUpgradeRefund);
    }

    public static boolean instantTrackingStartResearch(@Nullable Team team){
        return startSingleResearchWithQueue(team,
        UnitTypes::instantTrackingCanStartResearch,
        UnitTypes::instantTrackingConsume,
        UnitTypes::engineeringActiveResearching,
        engineeringResearchQueue,
        encodeQueueCode(engineeringQueueInstantTracking, 1),
        UnitTypes::startInstantTrackingResearchNow,
        UnitTypes::instantTrackingRefund);
    }

    public static boolean steelArmorStartResearch(@Nullable Team team){
        return startSingleResearchWithQueue(team,
        UnitTypes::steelArmorCanStartResearch,
        UnitTypes::steelArmorConsume,
        UnitTypes::engineeringActiveResearching,
        engineeringResearchQueue,
        encodeQueueCode(engineeringQueueSteelArmor, 1),
        UnitTypes::startSteelArmorResearchNow,
        UnitTypes::steelArmorRefund);
    }

    public static boolean ghostCamoStartResearch(@Nullable Team team){
        if(!ghostCamoCanStartResearch(team)) return false;
        GhostCamoData data = getGhostCamoData(team, true);
        if(data == null) return false;
        ghostCamoConsume(team);
        data.researching = true;
        data.researchTime = 0f;
        return true;
    }

    public static boolean barracksBlastShieldStartResearch(@Nullable Team team){
        return startSingleResearchWithQueue(team,
        UnitTypes::barracksBlastShieldCanStartResearch,
        t -> barracksTechConsume(t, barracksBlastShieldCrystalCost, barracksBlastShieldGasCost),
        UnitTypes::barracksActiveResearching,
        barracksResearchQueue,
        encodeQueueCode(barracksQueueBlastShield, 1),
        UnitTypes::startBarracksBlastShieldResearchNow,
        t -> barracksTechRefund(t, barracksBlastShieldCrystalCost, barracksBlastShieldGasCost));
    }

    public static boolean barracksStimpackStartResearch(@Nullable Team team){
        return startSingleResearchWithQueue(team,
        UnitTypes::barracksStimpackCanStartResearch,
        t -> barracksTechConsume(t, barracksStimpackCrystalCost, barracksStimpackGasCost),
        UnitTypes::barracksActiveResearching,
        barracksResearchQueue,
        encodeQueueCode(barracksQueueStimpack, 1),
        UnitTypes::startBarracksStimpackResearchNow,
        t -> barracksTechRefund(t, barracksStimpackCrystalCost, barracksStimpackGasCost));
    }

    public static boolean barracksConcussiveStartResearch(@Nullable Team team){
        return startSingleResearchWithQueue(team,
        UnitTypes::barracksConcussiveCanStartResearch,
        t -> barracksTechConsume(t, barracksConcussiveCrystalCost, barracksConcussiveGasCost),
        UnitTypes::barracksActiveResearching,
        barracksResearchQueue,
        encodeQueueCode(barracksQueueConcussive, 1),
        UnitTypes::startBarracksConcussiveResearchNow,
        t -> barracksTechRefund(t, barracksConcussiveCrystalCost, barracksConcussiveGasCost));
    }

    public static boolean infernoPreheaterStartResearch(@Nullable Team team){
        return startSingleResearchWithQueue(team,
        UnitTypes::infernoPreheaterCanStartResearch,
        t -> barracksTechConsume(t, infernoPreheaterCrystalCost, infernoPreheaterGasCost),
        UnitTypes::heavyFactoryActiveResearching,
        heavyFactoryResearchQueue,
        encodeQueueCode(heavyFactoryQueueInfernoPreheater, 1),
        UnitTypes::startInfernoPreheaterResearchNow,
        t -> barracksTechRefund(t, infernoPreheaterCrystalCost, infernoPreheaterGasCost));
    }

    public static boolean electromagneticFieldAcceleratorStartResearch(@Nullable Team team){
        return startSingleResearchWithQueue(team,
        UnitTypes::electromagneticFieldAcceleratorCanStartResearch,
        t -> barracksTechConsume(t, electromagneticFieldAcceleratorCrystalCost, electromagneticFieldAcceleratorGasCost),
        UnitTypes::heavyFactoryActiveResearching,
        heavyFactoryResearchQueue,
        encodeQueueCode(heavyFactoryQueueElectromagneticFieldAccelerator, 1),
        UnitTypes::startElectromagneticFieldAcceleratorResearchNow,
        t -> barracksTechRefund(t, electromagneticFieldAcceleratorCrystalCost, electromagneticFieldAcceleratorGasCost));
    }

    public static boolean drillClawStartResearch(@Nullable Team team){
        return startSingleResearchWithQueue(team,
        UnitTypes::drillClawCanStartResearch,
        t -> barracksTechConsume(t, drillClawCrystalCost, drillClawGasCost),
        UnitTypes::heavyFactoryActiveResearching,
        heavyFactoryResearchQueue,
        encodeQueueCode(heavyFactoryQueueDrillClaw, 1),
        UnitTypes::startDrillClawResearchNow,
        t -> barracksTechRefund(t, drillClawCrystalCost, drillClawGasCost));
    }

    public static boolean smartServosStartResearch(@Nullable Team team){
        return startSingleResearchWithQueue(team,
        UnitTypes::smartServosCanStartResearch,
        t -> barracksTechConsume(t, smartServosCrystalCost, smartServosGasCost),
        UnitTypes::heavyFactoryActiveResearching,
        heavyFactoryResearchQueue,
        encodeQueueCode(heavyFactoryQueueSmartServos, 1),
        UnitTypes::startSmartServosResearchNow,
        t -> barracksTechRefund(t, smartServosCrystalCost, smartServosGasCost));
    }

    public static boolean bansheeCloakFieldStartResearch(@Nullable Team team){
        return startSingleResearchWithQueue(team,
        UnitTypes::bansheeCloakFieldCanStartResearch,
        t -> barracksTechConsume(t, bansheeCloakFieldCrystalCost, bansheeCloakFieldGasCost),
        UnitTypes::starportActiveResearching,
        starportResearchQueue,
        encodeQueueCode(starportQueueCloakField, 1),
        UnitTypes::startBansheeCloakFieldResearchNow,
        t -> barracksTechRefund(t, bansheeCloakFieldCrystalCost, bansheeCloakFieldGasCost));
    }

    public static boolean bansheeAfterburnerStartResearch(@Nullable Team team){
        return startSingleResearchWithQueue(team,
        UnitTypes::bansheeAfterburnerCanStartResearch,
        t -> barracksTechConsume(t, bansheeAfterburnerCrystalCost, bansheeAfterburnerGasCost),
        UnitTypes::starportActiveResearching,
        starportResearchQueue,
        encodeQueueCode(starportQueueAfterburner, 1),
        UnitTypes::startBansheeAfterburnerResearchNow,
        t -> barracksTechRefund(t, bansheeAfterburnerCrystalCost, bansheeAfterburnerGasCost));
    }

    public static boolean ravenMatrixTechStartResearch(@Nullable Team team){
        return startSingleResearchWithQueue(team,
        UnitTypes::ravenMatrixTechCanStartResearch,
        t -> barracksTechConsume(t, ravenMatrixTechCrystalCost, ravenMatrixTechGasCost),
        UnitTypes::starportActiveResearching,
        starportResearchQueue,
        encodeQueueCode(starportQueueMatrix, 1),
        UnitTypes::startRavenMatrixTechResearchNow,
        t -> barracksTechRefund(t, ravenMatrixTechCrystalCost, ravenMatrixTechGasCost));
    }

    public static boolean battlecruiserWeaponRefitStartResearch(@Nullable Team team){
        return startSingleResearchWithQueue(team,
        UnitTypes::battlecruiserWeaponRefitCanStartResearch,
        t -> barracksTechConsume(t, battlecruiserWeaponRefitCrystalCost, battlecruiserWeaponRefitGasCost),
        UnitTypes::fusionCoreActiveResearching,
        fusionCoreResearchQueue,
        encodeQueueCode(fusionCoreQueueWeaponRefit, 1),
        UnitTypes::startBattlecruiserWeaponRefitResearchNow,
        t -> barracksTechRefund(t, battlecruiserWeaponRefitCrystalCost, battlecruiserWeaponRefitGasCost));
    }

    public static boolean medivacCaduceusReactorStartResearch(@Nullable Team team){
        return startSingleResearchWithQueue(team,
        UnitTypes::medivacCaduceusReactorCanStartResearch,
        t -> barracksTechConsume(t, medivacCaduceusReactorCrystalCost, medivacCaduceusReactorGasCost),
        UnitTypes::fusionCoreActiveResearching,
        fusionCoreResearchQueue,
        encodeQueueCode(fusionCoreQueueCaduceusReactor, 1),
        UnitTypes::startMedivacCaduceusReactorResearchNow,
        t -> barracksTechRefund(t, medivacCaduceusReactorCrystalCost, medivacCaduceusReactorGasCost));
    }

    public static boolean liberatorAdvancedBallisticsStartResearch(@Nullable Team team){
        return startSingleResearchWithQueue(team,
        UnitTypes::liberatorAdvancedBallisticsCanStartResearch,
        t -> barracksTechConsume(t, liberatorAdvancedBallisticsCrystalCost, liberatorAdvancedBallisticsGasCost),
        UnitTypes::fusionCoreActiveResearching,
        fusionCoreResearchQueue,
        encodeQueueCode(fusionCoreQueueAdvancedBallistics, 1),
        UnitTypes::startLiberatorAdvancedBallisticsResearchNow,
        t -> barracksTechRefund(t, liberatorAdvancedBallisticsCrystalCost, liberatorAdvancedBallisticsGasCost));
    }

    public static boolean infantryWeaponCancelResearch(@Nullable Team team){
        return cancelLeveledResearch(team, getInfantryWeaponData(team, false),
        data -> data.researchingLevel,
        (data, value) -> data.researchingLevel = value,
        (data, value) -> data.researchTime = value,
        UnitTypes::infantryUpgradeRefund);
    }

    public static boolean vehicleWeaponCancelResearch(@Nullable Team team){
        return cancelLeveledResearch(team, getVehicleWeaponData(team, false),
        data -> data.researchingLevel,
        (data, value) -> data.researchingLevel = value,
        (data, value) -> data.researchTime = value,
        UnitTypes::vehicleWeaponRefund);
    }

    public static boolean vehicleArmorCancelResearch(@Nullable Team team){
        return cancelLeveledResearch(team, getVehicleArmorData(team, false),
        data -> data.researchingLevel,
        (data, value) -> data.researchingLevel = value,
        (data, value) -> data.researchTime = value,
        UnitTypes::vehicleArmorRefund);
    }

    public static boolean shipWeaponCancelResearch(@Nullable Team team){
        return cancelLeveledResearch(team, getShipWeaponData(team, false),
        data -> data.researchingLevel,
        (data, value) -> data.researchingLevel = value,
        (data, value) -> data.researchTime = value,
        UnitTypes::shipWeaponRefund);
    }

    public static boolean infantryArmorCancelResearch(@Nullable Team team){
        return cancelLeveledResearch(team, getInfantryArmorData(team, false),
        data -> data.researchingLevel,
        (data, value) -> data.researchingLevel = value,
        (data, value) -> data.researchTime = value,
        UnitTypes::infantryUpgradeRefund);
    }

    public static boolean instantTrackingCancelResearch(@Nullable Team team){
        return cancelSingleResearch(team, getInstantTrackingData(team, false),
        data -> data.researching,
        (data, value) -> data.researching = value,
        (data, value) -> data.researchTime = value,
        UnitTypes::instantTrackingRefund);
    }

    public static boolean steelArmorCancelResearch(@Nullable Team team){
        return cancelSingleResearch(team, getSteelArmorData(team, false),
        data -> data.researching,
        (data, value) -> data.researching = value,
        (data, value) -> data.researchTime = value,
        UnitTypes::steelArmorRefund);
    }

    public static boolean ghostCamoCancelResearch(@Nullable Team team){
        return cancelSingleResearch(team, getGhostCamoData(team, false),
        data -> data.researching,
        (data, value) -> data.researching = value,
        (data, value) -> data.researchTime = value,
        UnitTypes::ghostCamoRefund);
    }

    public static boolean barracksBlastShieldCancelResearch(@Nullable Team team){
        return cancelSingleResearch(team, getBarracksBlastShieldData(team, false),
        data -> data.researching,
        (data, value) -> data.researching = value,
        (data, value) -> data.researchTime = value,
        t -> barracksTechRefund(t, barracksBlastShieldCrystalCost, barracksBlastShieldGasCost));
    }

    public static boolean barracksStimpackCancelResearch(@Nullable Team team){
        return cancelSingleResearch(team, getBarracksStimpackData(team, false),
        data -> data.researching,
        (data, value) -> data.researching = value,
        (data, value) -> data.researchTime = value,
        t -> barracksTechRefund(t, barracksStimpackCrystalCost, barracksStimpackGasCost));
    }

    public static boolean barracksConcussiveCancelResearch(@Nullable Team team){
        return cancelSingleResearch(team, getBarracksConcussiveData(team, false),
        data -> data.researching,
        (data, value) -> data.researching = value,
        (data, value) -> data.researchTime = value,
        t -> barracksTechRefund(t, barracksConcussiveCrystalCost, barracksConcussiveGasCost));
    }

    public static boolean infernoPreheaterCancelResearch(@Nullable Team team){
        return cancelSingleResearch(team, getInfernoPreheaterData(team, false),
        data -> data.researching,
        (data, value) -> data.researching = value,
        (data, value) -> data.researchTime = value,
        t -> barracksTechRefund(t, infernoPreheaterCrystalCost, infernoPreheaterGasCost));
    }

    public static boolean electromagneticFieldAcceleratorCancelResearch(@Nullable Team team){
        return cancelSingleResearch(team, getElectromagneticFieldAcceleratorData(team, false),
        data -> data.researching,
        (data, value) -> data.researching = value,
        (data, value) -> data.researchTime = value,
        t -> barracksTechRefund(t, electromagneticFieldAcceleratorCrystalCost, electromagneticFieldAcceleratorGasCost));
    }

    public static boolean drillClawCancelResearch(@Nullable Team team){
        return cancelSingleResearch(team, getDrillClawData(team, false),
        data -> data.researching,
        (data, value) -> data.researching = value,
        (data, value) -> data.researchTime = value,
        t -> barracksTechRefund(t, drillClawCrystalCost, drillClawGasCost));
    }

    public static boolean smartServosCancelResearch(@Nullable Team team){
        return cancelSingleResearch(team, getSmartServosData(team, false),
        data -> data.researching,
        (data, value) -> data.researching = value,
        (data, value) -> data.researchTime = value,
        t -> barracksTechRefund(t, smartServosCrystalCost, smartServosGasCost));
    }

    public static boolean bansheeCloakFieldCancelResearch(@Nullable Team team){
        return cancelSingleResearch(team, getBansheeCloakFieldData(team, false),
        data -> data.researching,
        (data, value) -> data.researching = value,
        (data, value) -> data.researchTime = value,
        t -> barracksTechRefund(t, bansheeCloakFieldCrystalCost, bansheeCloakFieldGasCost));
    }

    public static boolean bansheeAfterburnerCancelResearch(@Nullable Team team){
        return cancelSingleResearch(team, getBansheeAfterburnerData(team, false),
        data -> data.researching,
        (data, value) -> data.researching = value,
        (data, value) -> data.researchTime = value,
        t -> barracksTechRefund(t, bansheeAfterburnerCrystalCost, bansheeAfterburnerGasCost));
    }

    public static boolean ravenMatrixTechCancelResearch(@Nullable Team team){
        return cancelSingleResearch(team, getRavenMatrixTechData(team, false),
        data -> data.researching,
        (data, value) -> data.researching = value,
        (data, value) -> data.researchTime = value,
        t -> barracksTechRefund(t, ravenMatrixTechCrystalCost, ravenMatrixTechGasCost));
    }

    public static boolean battlecruiserWeaponRefitCancelResearch(@Nullable Team team){
        return cancelSingleResearch(team, getBattlecruiserWeaponRefitData(team, false),
        data -> data.researching,
        (data, value) -> data.researching = value,
        (data, value) -> data.researchTime = value,
        t -> barracksTechRefund(t, battlecruiserWeaponRefitCrystalCost, battlecruiserWeaponRefitGasCost));
    }

    public static boolean medivacCaduceusReactorCancelResearch(@Nullable Team team){
        return cancelSingleResearch(team, getMedivacCaduceusReactorData(team, false),
        data -> data.researching,
        (data, value) -> data.researching = value,
        (data, value) -> data.researchTime = value,
        t -> barracksTechRefund(t, medivacCaduceusReactorCrystalCost, medivacCaduceusReactorGasCost));
    }

    public static boolean liberatorAdvancedBallisticsCancelResearch(@Nullable Team team){
        return cancelSingleResearch(team, getLiberatorAdvancedBallisticsData(team, false),
        data -> data.researching,
        (data, value) -> data.researching = value,
        (data, value) -> data.researchTime = value,
        t -> barracksTechRefund(t, liberatorAdvancedBallisticsCrystalCost, liberatorAdvancedBallisticsGasCost));
    }

    public static boolean ghostCamoAnyResearching(@Nullable Team team){
        return ghostCamoResearching(team);
    }

    public static boolean ghostCamoCancelAnyResearch(@Nullable Team team){
        return ghostCamoCancelResearch(team);
    }

    public static boolean armoryCancelAnyResearch(@Nullable Team team){
        if(vehicleWeaponCancelResearch(team) || vehicleArmorCancelResearch(team) || shipWeaponCancelResearch(team)){
            return true;
        }
        return cancelLastQueuedArmoryResearch(team);
    }

    public static boolean infantryCancelAnyResearch(@Nullable Team team){
        if(infantryWeaponCancelResearch(team) || infantryArmorCancelResearch(team) || instantTrackingCancelResearch(team) || steelArmorCancelResearch(team)){
            return true;
        }
        return cancelLastQueuedEngineeringResearch(team);
    }

    public static boolean barracksTechCancelAnyResearch(@Nullable Team team){
        if(barracksBlastShieldCancelResearch(team) || barracksStimpackCancelResearch(team) || barracksConcussiveCancelResearch(team)){
            return true;
        }
        return cancelLastQueuedBarracksResearch(team);
    }

    public static boolean heavyFactoryTechCancelAnyResearch(@Nullable Team team){
        if(infernoPreheaterCancelResearch(team)
            || electromagneticFieldAcceleratorCancelResearch(team)
            || drillClawCancelResearch(team)
            || smartServosCancelResearch(team)){
            return true;
        }
        return cancelLastQueuedHeavyFactoryResearch(team);
    }

    public static boolean starportTechCancelAnyResearch(@Nullable Team team){
        if(bansheeCloakFieldCancelResearch(team)
            || bansheeAfterburnerCancelResearch(team)
            || ravenMatrixTechCancelResearch(team)){
            return true;
        }
        return cancelLastQueuedStarportResearch(team);
    }

    public static boolean fusionCoreCancelAnyResearch(@Nullable Team team){
        if(battlecruiserWeaponRefitCancelResearch(team)
            || medivacCaduceusReactorCancelResearch(team)
            || liberatorAdvancedBallisticsCancelResearch(team)){
            return true;
        }
        return cancelLastQueuedFusionCoreResearch(team);
    }

    public static int infantryWeaponBaseDamageBonus(@Nullable Team team){
        return infantryWeaponLevel(team);
    }

    public static int vehicleWeaponMaceBaseBonus(@Nullable Team team){
        return vehicleWeaponLevel(team) * 2;
    }

    public static int vehicleWeaponLocusBaseBonus(@Nullable Team team){
        return vehicleWeaponLevel(team);
    }

    public static int vehicleWeaponLocusLightBonus(@Nullable Team team){
        return vehicleWeaponLevel(team) * 2;
    }

    public static int infernoPreheaterLocusLightBonus(@Nullable Team team){
        return infernoPreheaterLevel(team) > 0 ? infernoPreheaterLocusLightBonusAmount : 0;
    }

    public static int infernoPreheaterMaceLightBonus(@Nullable Team team){
        return infernoPreheaterLevel(team) > 0 ? infernoPreheaterMaceLightBonusAmount : 0;
    }

    public static int vehicleWeaponHurricaneBaseBonus(@Nullable Team team){
        return vehicleWeaponLevel(team) * 2;
    }

    public static int vehicleWeaponPreceptMobileBaseBonus(@Nullable Team team){
        return vehicleWeaponLevel(team) * 2;
    }

    public static int vehicleWeaponPreceptMobileHeavyBonus(@Nullable Team team){
        return vehicleWeaponLevel(team) * 3;
    }

    public static int vehicleWeaponPreceptSiegeBaseBonus(@Nullable Team team){
        return vehicleWeaponLevel(team) * 4;
    }

    public static int vehicleWeaponPreceptSiegeHeavyBonus(@Nullable Team team){
        return vehicleWeaponLevel(team) * 5;
    }

    public static int vehicleWeaponScepterGroundBaseBonus(@Nullable Team team){
        return vehicleWeaponLevel(team) * 3;
    }

    public static int vehicleWeaponScepterBurstBaseBonus(@Nullable Team team){
        return vehicleWeaponLevel(team);
    }

    public static int vehicleWeaponScepterBurstLightBonus(@Nullable Team team){
        return vehicleWeaponLevel(team) * 2;
    }

    public static int vehicleWeaponScepterImpactBaseBonus(@Nullable Team team){
        return vehicleWeaponLevel(team) * 3;
    }

    public static int vehicleWeaponScepterImpactHeavyBonus(@Nullable Team team){
        return vehicleWeaponLevel(team) * 4;
    }

    public static int shipWeaponBattlecruiserGroundBonus(@Nullable Team team){
        return shipWeaponLevel(team);
    }

    public static int shipWeaponBattlecruiserAirBonus(@Nullable Team team){
        return shipWeaponLevel(team);
    }

    public static int shipWeaponBansheeBonus(@Nullable Team team){
        return shipWeaponLevel(team);
    }

    public static int shipWeaponVikingFighterBaseBonus(@Nullable Team team){
        return shipWeaponLevel(team);
    }

    public static int shipWeaponVikingFighterHeavyBonus(@Nullable Team team){
        return shipWeaponLevel(team);
    }

    public static int shipWeaponVikingMechBaseBonus(@Nullable Team team){
        return shipWeaponLevel(team);
    }

    public static int shipWeaponVikingMechMechanicalBonus(@Nullable Team team){
        return shipWeaponLevel(team) * 2;
    }

    public static int shipWeaponLiberatorFighterBonus(@Nullable Team team){
        return shipWeaponLevel(team);
    }

    public static int shipWeaponLiberatorDefenseBonus(@Nullable Team team){
        return shipWeaponLevel(team) * 5;
    }

    public static int infantryWeaponFortressHeavyBonus(@Nullable Team team){
        return infantryWeaponLevel(team) * 2;
    }

    public static int infantryWeaponGhostLightBonus(@Nullable Team team){
        return infantryWeaponLevel(team) * 2;
    }

    public static int infantryArmorBonus(@Nullable Team team){
        return infantryArmorLevel(team);
    }

    public static float barracksBlastShieldHpBonus(@Nullable Team team){
        return barracksBlastShieldLevel(team) > 0 ? barracksBlastShieldHpBonus : 0f;
    }

    public static int vehicleArmorBonus(@Nullable Team team){
        return vehicleArmorLevel(team);
    }

    public static float instantTrackingRangeBonusTiles(@Nullable Team team){
        return instantTrackingLevel(team) > 0 ? 1f : 0f;
    }

    public static float instantTrackingRangeBonus(@Nullable Team team){
        return instantTrackingRangeBonusTiles(team) * tilesize;
    }

    public static float steelArmorBuildingArmorBonus(@Nullable Team team){
        return steelArmorLevel(team) > 0 ? 2f : 0f;
    }

    public static int steelArmorBunkerSlotBonus(@Nullable Team team){
        return steelArmorLevel(team) > 0 ? 2 : 0;
    }

    public static int steelArmorUpgradedCoreScvCapacity(@Nullable Team team){
        return steelArmorLevel(team) > 0 ? 10 : 5;
    }

    public static int instantTrackingCrystalCost(){
        return instantTrackingCrystalCost;
    }

    public static int instantTrackingGasCost(){
        return instantTrackingGasCost;
    }

    public static float instantTrackingResearchDuration(){
        return instantTrackingResearchTime;
    }

    public static int steelArmorCrystalCost(){
        return steelArmorCrystalCost;
    }

    public static int steelArmorGasCost(){
        return steelArmorGasCost;
    }

    public static float steelArmorResearchDuration(){
        return steelArmorResearchTime;
    }

    public static int ghostCamoCrystalCost(){
        return ghostCamoCrystalCost;
    }

    public static int ghostCamoGasCost(){
        return ghostCamoGasCost;
    }

    public static float ghostCamoResearchDuration(){
        return ghostCamoResearchTime;
    }

    public static int ghostWarheadCrystalCost(){
        return ghostWarheadCrystalCost;
    }

    public static int ghostWarheadGasCost(){
        return ghostWarheadGasCost;
    }

    public static float ghostWarheadBuildDuration(){
        return ghostWarheadBuildTime;
    }

    public static int barracksBlastShieldCrystalCost(){
        return barracksBlastShieldCrystalCost;
    }

    public static int barracksBlastShieldGasCost(){
        return barracksBlastShieldGasCost;
    }

    public static float barracksBlastShieldResearchDuration(){
        return barracksBlastShieldResearchTime;
    }

    public static int barracksStimpackCrystalCost(){
        return barracksStimpackCrystalCost;
    }

    public static int barracksStimpackGasCost(){
        return barracksStimpackGasCost;
    }

    public static float barracksStimpackResearchDuration(){
        return barracksStimpackResearchTime;
    }

    public static int barracksConcussiveCrystalCost(){
        return barracksConcussiveCrystalCost;
    }

    public static int barracksConcussiveGasCost(){
        return barracksConcussiveGasCost;
    }

    public static float barracksConcussiveResearchDuration(){
        return barracksConcussiveResearchTime;
    }

    public static int infernoPreheaterCrystalCost(){
        return infernoPreheaterCrystalCost;
    }

    public static int infernoPreheaterGasCost(){
        return infernoPreheaterGasCost;
    }

    public static float infernoPreheaterResearchDuration(){
        return infernoPreheaterResearchTime;
    }

    public static int electromagneticFieldAcceleratorCrystalCost(){
        return electromagneticFieldAcceleratorCrystalCost;
    }

    public static int electromagneticFieldAcceleratorGasCost(){
        return electromagneticFieldAcceleratorGasCost;
    }

    public static float electromagneticFieldAcceleratorResearchDuration(){
        return electromagneticFieldAcceleratorResearchTime;
    }

    public static int drillClawCrystalCost(){
        return drillClawCrystalCost;
    }

    public static int drillClawGasCost(){
        return drillClawGasCost;
    }

    public static float drillClawResearchDuration(){
        return drillClawResearchTime;
    }

    public static int smartServosCrystalCost(){
        return smartServosCrystalCost;
    }

    public static int smartServosGasCost(){
        return smartServosGasCost;
    }

    public static float smartServosResearchDuration(){
        return smartServosResearchTime;
    }

    public static int bansheeCloakFieldCrystalCost(){
        return bansheeCloakFieldCrystalCost;
    }

    public static int bansheeCloakFieldGasCost(){
        return bansheeCloakFieldGasCost;
    }

    public static float bansheeCloakFieldResearchDuration(){
        return bansheeCloakFieldResearchTime;
    }

    public static int bansheeAfterburnerCrystalCost(){
        return bansheeAfterburnerCrystalCost;
    }

    public static int bansheeAfterburnerGasCost(){
        return bansheeAfterburnerGasCost;
    }

    public static float bansheeAfterburnerResearchDuration(){
        return bansheeAfterburnerResearchTime;
    }

    public static int ravenMatrixTechCrystalCost(){
        return ravenMatrixTechCrystalCost;
    }

    public static int ravenMatrixTechGasCost(){
        return ravenMatrixTechGasCost;
    }

    public static float ravenMatrixTechResearchDuration(){
        return ravenMatrixTechResearchTime;
    }

    public static int battlecruiserWeaponRefitCrystalCost(){
        return battlecruiserWeaponRefitCrystalCost;
    }

    public static int battlecruiserWeaponRefitGasCost(){
        return battlecruiserWeaponRefitGasCost;
    }

    public static float battlecruiserWeaponRefitResearchDuration(){
        return battlecruiserWeaponRefitResearchTime;
    }

    public static int medivacCaduceusReactorCrystalCost(){
        return medivacCaduceusReactorCrystalCost;
    }

    public static int medivacCaduceusReactorGasCost(){
        return medivacCaduceusReactorGasCost;
    }

    public static float medivacCaduceusReactorResearchDuration(){
        return medivacCaduceusReactorResearchTime;
    }

    public static int liberatorAdvancedBallisticsCrystalCost(){
        return liberatorAdvancedBallisticsCrystalCost;
    }

    public static int liberatorAdvancedBallisticsGasCost(){
        return liberatorAdvancedBallisticsGasCost;
    }

    public static float liberatorAdvancedBallisticsResearchDuration(){
        return liberatorAdvancedBallisticsResearchTime;
    }

    public static boolean ravenCanDeployTurret(@Nullable Unit unit){
        return isRaven(unit)
            && !ravenMatrixDisabled(unit)
            && unit.energy >= ravenTurretCost
            && Blocks.ravenTurret != null;
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
            && ravenMatrixTechLevel(unit.team) > 0;
    }

    private static boolean ravenCanPlaceTurret(@Nullable Unit unit, float worldX, float worldY){
        if(unit == null || Blocks.ravenTurret == null) return false;
        Block block = Blocks.ravenTurret;

        Tmp.v1.set(worldX, worldY).sub(block.offset, block.offset);
        int tx = World.toTile(Tmp.v1.x);
        int ty = World.toTile(Tmp.v1.y);
        if(tx < 0 || ty < 0 || tx >= world.width() || ty >= world.height()) return false;

        return Build.validPlaceIgnoreUnits(block, unit.team, tx, ty, 0, false, false)
            && Build.checkNoUnitOverlap(block, tx, ty);
    }

    private static boolean ravenPlaceTurret(@Nullable Unit unit, float worldX, float worldY){
        if(unit == null || Blocks.ravenTurret == null) return false;
        Block block = Blocks.ravenTurret;

        Tmp.v1.set(worldX, worldY).sub(block.offset, block.offset);
        int tx = World.toTile(Tmp.v1.x);
        int ty = World.toTile(Tmp.v1.y);
        if(tx < 0 || ty < 0 || tx >= world.width() || ty >= world.height()) return false;

        Tile tile = world.tile(tx, ty);
        if(tile == null) return false;

        ConstructBlock.constructFinish(tile, block, unit, (byte)0, unit.team, null);
        if(tile.build != null){
            tile.build.placed();
        }
        Fx.spawn.at(tx * tilesize + block.offset, ty * tilesize + block.offset, 0f, unit.team.color);
        return true;
    }

    public static boolean ravenMatrixValidTarget(@Nullable Unit target, Team team){
        return target != null
            && target.isValid()
            && (target.type.unitClasses.contains(UnitClass.mechanical) || target.type.unitClasses.contains(UnitClass.psionic));
    }

    public static boolean allowFireWhileMoving(@Nullable Unit unit){
        return unit != null && unit.hasEffect(StatusEffects.combatMobility);
    }

    public static boolean allowRotateWhileMoving(@Nullable Unit unit){
        return unit != null && unit.hasEffect(StatusEffects.combatMobility);
    }

    public static boolean allowMoveWhileShooting(@Nullable Unit unit){
        return unit != null && unit.hasEffect(StatusEffects.combatMobility);
    }

    public static float ravenTurretLifeProgress(@Nullable Unit unit){
        if(!isRavenTurret(unit)) return 0f;
        return Mathf.clamp(unit.getDuration(StatusEffects.ravenTurretLifetime) / ravenTurretLifetime);
    }

    public static float ravenTurretLifetime(){
        return ravenTurretLifetime;
    }

    public static boolean commandRavenDeployTurret(@Nullable Unit unit, @Nullable Vec2 target){
        if(!ravenCanDeployTurret(unit) || target == null) return false;

        RavenData data = getRavenData(unit);
        data.pendingTurret = true;
        data.pendingAntiArmor = false;
        data.pendingMatrix = false;
        data.matrixTargetId = -1;
        data.turretTarget.set(target);
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
            data.pendingTurret = false;
            return;
        }

        if(data.pendingTurret){
            if(!ravenCanDeployTurret(unit) || Blocks.ravenTurret == null){
                data.pendingTurret = false;
            }else{
                Vec2 target = data.turretTarget;
                boolean facing = faceTargetedAbilityPoint(unit, target.x, target.y);

                if(unit.within(target, ravenTurretDeployRange)){
                    holdForTargetedAbility(unit);
                    if(!facing) return;

                    if(ravenCanPlaceTurret(unit, target.x, target.y)){
                        if(ravenPlaceTurret(unit, target.x, target.y)){
                            unit.energy = Math.max(0f, unit.energy - ravenTurretCost);
                        }
                    }
                    data.pendingTurret = false;
                }else if(unit.controller() instanceof CommandAI ai){
                    ai.command(UnitCommand.moveCommand);
                    ai.commandPosition(target, false);
                }
            }
        }

        if(data.pendingAntiArmor){
            if(!ravenCanUseAntiArmor(unit)){
                data.pendingAntiArmor = false;
            }else{
                Vec2 target = data.antiArmorTarget;
                boolean facing = faceTargetedAbilityPoint(unit, target.x, target.y);

                if(unit.within(target, ravenAntiArmorRange)){
                    holdForTargetedAbility(unit);
                    if(!facing) return;

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
        if(unit.controller() instanceof CommandAI ai && ai.followTarget instanceof Unit followed && medivacCanHealTarget(followed, unit.team)){
            return followed;
        }

        float searchRange = medivacHealRange * 4f;
        float radius2 = searchRange * searchRange;
        Unit[] best = {null};
        float[] bestHealthf = {Float.MAX_VALUE};
        float[] bestDst2 = {Float.MAX_VALUE};

        Units.nearby(unit.team, unit.x - searchRange, unit.y - searchRange, searchRange * 2f, searchRange * 2f, other -> {
            if(other == null || other == unit || !medivacCanHealTarget(other, unit.team)) return;

            float dst2 = unit.dst2(other);
            if(dst2 > radius2) return;

            float healthf = other.healthf();
            if(best[0] == null || healthf < bestHealthf[0] - 0.0001f || (Mathf.equal(healthf, bestHealthf[0], 0.0001f) && dst2 < bestDst2[0])){
                best[0] = other;
                bestHealthf[0] = healthf;
                bestDst2[0] = dst2;
            }
        });

        return best[0];
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
    public static @EntityDef({Unitc.class}) UnitType scanProbe, warpProbe;

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

    public static boolean isSc2DatabaseUnit(@Nullable UnitType type){
        return type == nova
        || type == dagger
        || type == reaper
        || type == fortress
        || type == ghost
        || type == mace
        || type == locus
        || type == crawler
        || type == hurricane
        || type == precept
        || type == scepter
        || type == flare
        || type == mega
        || type == liberator
        || type == avert
        || type == horizon
        || type == antumbra;
    }

    private static void markSc2DatabaseUnits(){
        for(UnitType type : content.units()){
            if(type == null || !isSc2DatabaseUnit(type)) continue;
            type.databaseCategory = "unit";
            type.databaseTag = "sc2";
            type.allDatabaseTabs = true;
        }
    }

    public static void load(){
        ensureInfantryUpgradeHooks();
        //region ground attack

        dagger = new UnitType("dagger"){
            TextureRegion rightGunRegion;

            @Override
            public void load(){
                super.load();
                rightGunRegion = Core.atlas.find("dagger-weapon");
                if(!rightGunRegion.found()) rightGunRegion = Core.atlas.find("unit-dagger-weapon");
                if(!rightGunRegion.found()) rightGunRegion = Core.atlas.find("weapon");
                if(rightGunRegion.found() && !Mathf.equal(appliedSpriteScale, 1f, 0.0001f)){
                    rightGunRegion = copyScaledRegion(rightGunRegion, appliedSpriteScale);
                }
            }

            @Override
            public void draw(Unit unit){
                super.draw(unit);
                drawBarracksStimpackFlash(unit);
                if(rightGunRegion == null || !rightGunRegion.found()) return;

                float rot = unit.rotation - 90f;
                float swayX = 0f, swayY = 0f;
                if(unit instanceof Mechc mech){
                    rot = mech.baseRotation() - 90f;
                    float e = unit.elevation;
                    float side = Mathf.lerp(Mathf.sin(mech.walkExtend(true), 2f / Mathf.PI, 1f) * mechSideSway, 0f, e);
                    float front = Mathf.lerp(Mathf.sin(mech.walkExtend(true), 1f / Mathf.PI, 1f) * mechFrontSway, 0f, e);
                    Tmp.v1.trns(mech.baseRotation(), 0f, side);
                    Tmp.v2.trns(mech.baseRotation() + 90f, 0f, front);
                    swayX = Tmp.v1.x + Tmp.v2.x;
                    swayY = Tmp.v1.y + Tmp.v2.y;
                }

                float ox = 3.8f, oy = 0.8f;
                if(!weapons.isEmpty()){
                    Weapon w = weapons.first();
                    ox = w.x;
                    oy = w.y;
                }

                float wx = unit.x + swayX + Angles.trnsx(rot, ox, oy);
                float wy = unit.y + swayY + Angles.trnsy(rot, ox, oy);

                Draw.z(Layer.groundUnit + 0.05f);
                Draw.rect(rightGunRegion, wx, wy, rot);
                Draw.reset();
            }

            {
            speed = 3.15f;
            hitSizeFromRegion = false;
            spriteHitSizeRatio = 1.5f;
            hitSize = 0.875f * tilesize;
            health = 45f;
            armor = 0f;
            rotateSpeed = 6f;
            omniMovement = true;
            rotateMoveFirst = false;
                  range = maxRange = 5.5f * tilesize;
            targetAir = true;
            targetGround = true;
            armorType = ArmorType.light;
            unitClasses = EnumSet.of(UnitClass.biological);
            population = 1;
            fogRadius = 9f;

            weapons.add(new Weapon(){{
                mirror = false;
                x = 3.8f;
                y = 0.8f;
                shootX = 0f;
                shootY = 2.2f;
                reload = 0.61f * 60f;
                bullet = new PointBulletType(){
                    {
                        damage = 6f;
                        rangeOverride = 5f * tilesize;
                        shootEffect = infantryMuzzleFlashEffect;
                        smokeEffect = Fx.none;
                        hitEffect = Fx.none;
                        despawnEffect = Fx.none;
                        trailEffect = Fx.none;
                    }

                    @Override
                    public void hitEntity(Bullet b, Hitboxc entity, float health){
                        Unit unit = entity instanceof Unit ? (Unit)entity : null;
                        float baseDamage = b.damage + infantryWeaponBaseDamageBonus(b.team);
                        float armor = unit != null ? unit.armor() : 0f;
                        float effective = Math.max(baseDamage - armor, 0.5f);
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

                    @Override
                    public float buildingDamage(Bullet b){
                        return b.damage + infantryWeaponBaseDamageBonus(b.team);
                    }
                };
            }});
            }
        };

        reaper = new UnitType("reaper"){{
            speed = 5.25f;
            hitSizeFromRegion = false;
            spriteHitSizeRatio = 1.1f * 1.25f * 1.2f;
            hitSize = 0.875f * tilesize;
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
            fogRadius = 9f;
            canPassWalls = true;
            regenDelay = 5f;
            regenRate = 2f;

            reaperKd8Bullet = new BasicBulletType(reaperKd8ThrowSpeed, 0f){
                {
                    width = 6f;
                    height = 6f;
                    lifetime = 60f;
                    hitSize = 2f;
                    keepVelocity = false;
                    drag = 0f;
                    collides = false;
                    collidesTiles = false;
                    collidesAir = false;
                    collidesGround = false;
                    hittable = false;
                    absorbable = false;
                    reflectable = false;
                    shootEffect = Fx.none;
                    smokeEffect = Fx.none;
                    hitEffect = Fx.none;
                    despawnEffect = Fx.none;
                }

                @Override
                public void draw(Bullet b){
                    Draw.z(Layer.bullet);
                    Draw.color(Color.black, 0.9f);
                    Fill.circle(b.x, b.y, 1.6f);
                    Draw.color(Color.valueOf("52e0ff"));
                    Fill.circle(b.x, b.y, 1.0f);
                    Draw.reset();
                }

                @Override
                public void despawned(Bullet b){
                    spawnReaperKd8Bomb(b.x, b.y, b.team);
                }
            };

            weapons.add(new Weapon(){{
                reload = 0.79f * 60f;
                alternate = false;
                shootY = 2.5f;
                bullet = new PointBulletType(){
                    {
                        damage = 4f;
                        rangeOverride = 5f * tilesize;
                        shootEffect = infantryMuzzleFlashEffect;
                        smokeEffect = Fx.none;
                        hitEffect = Fx.none;
                        despawnEffect = Fx.none;
                        trailEffect = Fx.none;
                    }

                    @Override
                    public void hitEntity(Bullet b, Hitboxc entity, float health){
                        float prev = b.damage;
                        b.damage = prev + infantryWeaponBaseDamageBonus(b.team);
                        super.hitEntity(b, entity, health);
                        b.damage = prev;
                    }

                    @Override
                    public float buildingDamage(Bullet b){
                        return b.damage + infantryWeaponBaseDamageBonus(b.team);
                    }
                };
            }});
        }
        @Override
        public void update(Unit unit){
            super.update(unit);
            updateReaperKd8(unit);
        }
        @Override
        public void killed(Unit unit){
            clearReaperKd8Data(unit);
        }
        @Override
        public void load(){
            float prevRatio = spriteHitSizeRatio;
            spriteHitSizeRatio = -1f;
            super.load();
            spriteHitSizeRatio = prevRatio;
            region = Core.atlas.find("alpha");
            outlineRegion = region;
            baseRegion = Core.atlas.find("nova-base", region);
            fullIcon = Core.atlas.find("unit-alpha-full", region);
            uiIcon = Core.atlas.find("unit-alpha-ui", fullIcon);
            shadowRegion = fullIcon;
            clipSize = Math.max(region.width * 2f, clipSize);
            hitSize = 0.825f * tilesize;
            applySpriteHitSizeRatio();
        }
        @Override
        public void drawMech(Mechc mech){
            // Reaper: hide mech legs entirely.
        }
        };

        mace = new UnitType("mace"){
              @Override
              public void update(Unit unit){
                  super.update(unit);
                  PreceptSiegeData siegeData = getPreceptSiegeData(unit);
                  if(!siegeData.siegeMode){
                      unit.unapply(StatusEffects.preceptSieged);
                  }
                  if(maceLocusTransforming(unit)){
                      unit.vel.setZero();
                      if(unit.controller() instanceof CommandAI ai){
                          ai.clearCommands();
                      }
                }
            }

            @Override
            public void killed(Unit unit){
                clearMaceLocusTransformData(unit);
            }

            {
            speed = 3.15f;
            hitSizeFromRegion = false;
                spriteHitSizeRatio = 1.5f;
            hitSize = 0.975f * tilesize;
            fogRadius = 10f;
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
                    float baseDamage = (b.damage + vehicleWeaponMaceBaseBonus(b.team)) * b.damageMultiplier();

                    Units.nearby((Team)null, b.x - coneRange, b.y - coneRange, coneRange * 2f, coneRange * 2f, u -> {
                        if(!canDamageFriendlyOnlyWhenForced(b, u)) return;
                        if(!u.checkTarget(false, true) || !u.hittable()) return;
                        if(!u.within(b.x, b.y, coneRange + u.hitSize / 2f)) return;
                        if(!Angles.within(b.rotation(), b.angleTo(u), halfAngle)) return;

                        float damage = baseDamage;
                        if(u.type.armorType == ArmorType.light){
                            damage += infernoPreheaterMaceLightBonus(b.team);
                        }
                        u.damage(damage);
                        Fx.hitFlameSmall.at(u.x, u.y);
                    });

                    Units.nearbyBuildings(b.x, b.y, coneRange + 8f, build -> {
                        if(!canDamageFriendlyOnlyWhenForced(b, build) || !build.collide(b)) return;
                        if(!b.checkUnderBuild(build, build.x, build.y)) return;
                        if(Mathf.dst(b.x, b.y, build.x, build.y) > coneRange + build.hitSize() / 2f) return;
                        if(!Angles.within(b.rotation(), Angles.angle(b.x, b.y, build.x, build.y), halfAngle)) return;

                        build.damage(baseDamage * b.type.buildingDamageMultiplier);
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
            }
        };

        ravenTurret = new UnitType("raven-turret"){
            @Override
            public void load(){
                super.load();
                region = Core.atlas.find("salvo", region);
                previewRegion = Core.atlas.find("salvo-preview", region);
                outlineRegion = Core.atlas.find("salvo-outline", outlineRegion);
                baseRegion = Core.atlas.find("salvo-base", baseRegion);
                uiIcon = Core.atlas.find("salvo-ui", uiIcon);
                fullIcon = Core.atlas.find("salvo-full", fullIcon);
                shadowRegion = Core.atlas.find("salvo-shadow", shadowRegion);
                updateHitSizeFromRegion();
            }

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
                fogRadius = 7f;
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

                weapons.add(new Weapon("raven-turret-weapon"){
                    {
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
                    }

                    @Override
                    public void update(Unit unit, WeaponMount mount){
                        float baseRange = 6f * tilesize + instantTrackingRangeBonus(unit.team);
                        float prevRange = bullet.range;
                        float prevOverride = bullet.rangeOverride;
                        bullet.range = baseRange;
                        bullet.rangeOverride = baseRange;
                        super.update(unit, mount);
                        bullet.range = prevRange;
                        bullet.rangeOverride = prevOverride;
                    }
                });
            }
        };

        fortress = new UnitType("fortress"){
            @Override
            public void load(){
                float prevRatio = spriteHitSizeRatio;
                spriteHitSizeRatio = -1f;
                super.load();
                spriteHitSizeRatio = prevRatio;
                if(!weapons.isEmpty()){
                    Weapon weapon = weapons.first();
                    if(weapon.region == null || !weapon.region.found()){
                        weapon.region = Core.atlas.find("artillery-mount");
                    }
                    if(weapon.outlineRegion == null || !weapon.outlineRegion.found()){
                        weapon.outlineRegion = Core.atlas.find("artillery-mount-outline");
                    }
                    if(weapon.region == null || !weapon.region.found()){
                        weapon.region = Core.atlas.find("small-mount-weapon");
                    }
                    if(weapon.outlineRegion == null || !weapon.outlineRegion.found()){
                        weapon.outlineRegion = Core.atlas.find("small-mount-weapon-outline");
                    }
                }
                applySpriteHitSizeRatio();
            }

            @Override
            public void draw(Unit unit){
                float prevX = Draw.xscl, prevY = Draw.yscl;
                Draw.scl(prevX * 0.7f, prevY * 0.7f);
                super.draw(unit);
                Draw.scl(prevX, prevY);
                drawBarracksStimpackFlash(unit);
            }

            @Override
            public void update(Unit unit){
                super.update(unit);
                FortressBodyKickData data = fortressBodyKickData.get(unit.id);
                if(data == null || !data.active){
                    if(data != null){
                        fortressBodyKickData.remove(unit.id);
                    }
                    return;
                }

                if(!Float.isFinite(unit.rotation) || !Float.isFinite(data.offset) || !Float.isFinite(data.omega) || !Float.isFinite(data.sign) || Mathf.zero(data.sign, 0.001f)){
                    if(Float.isFinite(unit.rotation) == false) unit.rotation = 0f;
                    fortressBodyKickData.remove(unit.id);
                    return;
                }

                float dt = Math.min(Time.delta, 2f);
                float prevOffset = data.offset;
                float accel = -Mathf.sign(data.sign) * fortressBodyKickAngularAccel;
                data.omega += accel * dt;
                data.offset += data.omega * dt;
                data.offset = Mathf.clamp(data.offset, -fortressBodyKickMaxAngle, fortressBodyKickMaxAngle);
                unit.rotation += data.offset - prevOffset;

                if(unit.mounts != null){
                    if(unit.mounts.length > 0 && Float.isFinite(data.frozenMount0)){
                        unit.mounts[0].rotation = data.frozenMount0;
                    }
                    if(unit.mounts.length > 1 && Float.isFinite(data.frozenMount1)){
                        unit.mounts[1].rotation = data.frozenMount1;
                    }
                }

                // Returned to zero with reverse angular speed: end kick phase.
                if(data.sign * data.offset <= 0f && data.sign * data.omega < 0f){
                    unit.rotation -= data.offset;
                    data.offset = 0f;
                    data.omega = 0f;
                    data.active = false;
                    fortressBodyKickData.remove(unit.id);
                }
            }

            {
            visualHitSizeScale = 0.7f;
            spriteDrawScale = 0.7f;
            hitSizeFromRegion = false;
            spriteHitSizeRatio = 1.5f;
            hitSize = 0.975f * tilesize;
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
            fogRadius = 10f;

            weapons.add(new Weapon("artillery-mount"){{
                x = 6.8f;
                y = 0f;
                shootY = 2.8f;
                mirror = true;
                alternate = true;
                rotate = true;
                rotateSpeed = 6f;
                reload = 1.07f * 60f;
                recoil = 26f;
                recoilTime = 14f;
                shake = 1.1f;
                bullet = new ArtilleryBulletType(3f, 10f, "shell"){
                    {
                        hitEffect = Fx.none;
                        despawnEffect = Fx.none;
                        knockback = 0.8f;
                        lifetime = 16f;
                        scaleLife = false;
                        rangeOverride = 6f * tilesize;
                        width = height = 8f;
                        collidesTiles = false;
                        splashDamageRadius = 0f;
                        splashDamage = 0f;
                        trailEffect = Fx.none;
                        trailMult = 0f;
                    }

                    @Override
                    public void hitEntity(Bullet b, Hitboxc entity, float health){
                        float prev = b.damage;
                        b.damage = prev + infantryWeaponBaseDamageBonus(b.team);
                        if(entity instanceof Unit u && u.type.armorType == ArmorType.heavy){
                            b.damage = prev + 10f + infantryWeaponFortressHeavyBonus(b.team);
                        }
                        super.hitEntity(b, entity, health);
                        if(entity instanceof Unit u && u.team != b.team && barracksConcussiveLevel(b.team) > 0){
                            u.apply(StatusEffects.barracksConcussiveSlow, barracksConcussiveDuration);
                        }
                        b.damage = prev;
                    }

                    @Override
                    public float buildingDamage(Bullet b){
                        return b.damage + infantryWeaponBaseDamageBonus(b.team);
                    }

                    private void applyImpactTargetDamage(Bullet b, FortressShellData data, float x, float y){
                        Teamc target = data.target;
                        if(!canTrackFriendlyOnlyWhenForced(b, target)) return;

                        if(target instanceof Unit unit){
                            if(!unit.hittable() || !unit.checkTarget(collidesAir, collidesGround)) return;
                            if(!unit.within(x, y, unit.hitSize / 2f + 3f)) return;
                            unit.collision(b, x, y);
                            hitEntity(b, unit, unit.health());
                            return;
                        }

                        if(target instanceof Building build){
                            if(!build.isValid() || !build.collide(b)) return;
                            if(!build.within(x, y, build.hitSize() / 2f + 3f)) return;
                            build.collision(b);
                        }
                    }

                    @Override
                    public void update(Bullet b){
                        b.keepAlive = true;
                        FortressShellData data;
                        if(b.data instanceof FortressShellData d){
                            data = d;
                        }else{
                            data = new FortressShellData();
                            data.target = b.data instanceof Teamc t ? t : null;
                            data.originX = b.x;
                            data.originY = b.y;
                            if(!Float.isNaN(b.aimX) && !Float.isNaN(b.aimY)){
                                data.lastInRangeX = b.aimX;
                                data.lastInRangeY = b.aimY;
                            }
                            b.data = data;
                        }

                        float maxRange = rangeOverride > 0f ? rangeOverride : 6f * tilesize;
                        Teamc target = data.target;
                        boolean validTarget = canTrackFriendlyOnlyWhenForced(b, target);

                        if(validTarget){
                            float tx = target.getX(), ty = target.getY();
                            if(Mathf.within(data.originX, data.originY, tx, ty, maxRange)){
                                data.lastInRangeX = tx;
                                data.lastInRangeY = ty;
                                b.aimX = tx;
                                b.aimY = ty;
                            }
                        }else{
                            data.target = null;
                        }

                        float tx = data.lastInRangeX, ty = data.lastInRangeY;
                        if(Float.isNaN(tx) || Float.isNaN(ty)){
                            b.remove();
                            return;
                        }

                        b.aimX = tx;
                        b.aimY = ty;
                        float trackingTurn = data.target != null ? 240f : 160f;
                        b.vel.setAngle(Angles.moveToward(b.rotation(), b.angleTo(tx, ty), trackingTurn * Time.delta));
                        b.vel.setLength(speed);
                        b.rotation(b.vel.angle());

                        float hitRange = Math.max(0.65f, b.vel.len() * Time.delta * 0.85f);
                        if(Mathf.within(b.x, b.y, tx, ty, hitRange)){
                            applyImpactTargetDamage(b, data, tx, ty);
                            hit(b, tx, ty);
                            b.remove();
                        }
                    }
                };
            }
            @Override
            public void draw(Unit unit, WeaponMount mount){
                float prevX = Draw.xscl, prevY = Draw.yscl;
                Draw.scl(prevX * 1.12f, prevY * 1.12f);
                super.draw(unit, mount);
                Draw.scl(prevX, prevY);
            }

            @Override
            public void drawOutline(Unit unit, WeaponMount mount){
                float prevX = Draw.xscl, prevY = Draw.yscl;
                Draw.scl(prevX * 1.12f, prevY * 1.12f);
                super.drawOutline(unit, mount);
                Draw.scl(prevX, prevY);
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

                float forwardX = Angles.trnsx(unit.rotation, 1f);
                float forwardY = Angles.trnsy(unit.rotation, 1f);
                float relX = bulletX - unit.x;
                float relY = bulletY - unit.y;
                float sideCross = forwardX * relY - forwardY * relX;
                float side = Float.isFinite(sideCross) && sideCross != 0f ? Mathf.sign(sideCross) : (mount.side ? 1f : -1f);
                FortressBodyKickData kick = fortressBodyKickData.get(unit.id);
                if(kick == null){
                    kick = new FortressBodyKickData();
                    fortressBodyKickData.put(unit.id, kick);
                }
                if(!kick.active){
                    kick.offset = 0f;
                    kick.omega = 0f;
                    kick.sign = side;
                }else if(kick.sign != side){
                    //Switching fire side mid-kick starts a fresh kick from current orientation.
                    kick.offset = 0f;
                    kick.omega = 0f;
                    kick.sign = side;
                }

                kick.active = true;
                kick.omega += side * fortressBodyKickInitialOmega;
                kick.omega = Mathf.clamp(kick.omega, -fortressBodyKickInitialOmega * 1.4f, fortressBodyKickInitialOmega * 1.4f);
                if(unit.mounts != null){
                    if(unit.mounts.length > 0) kick.frozenMount0 = unit.mounts[0].rotation;
                    if(unit.mounts.length > 1) kick.frozenMount1 = unit.mounts[1].rotation;
                }
            }
            });
            }
        };

        ghost = new UnitType("ghost"){{
            visualHitSizeScale = 0.7f;
            speed = 3.15f;
            spriteDrawScale = 0.7f;
            hitSizeFromRegion = false;
            spriteHitSizeRatio = 1.5f;
            hitSize = 0.875f * tilesize;
            fogRadius = 11f;
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
            energyInit = 75f;
            energyRegen = 1f;

            ghostStableAimBullet = new BasicBulletType(18f, ghostStableAimDamage){
                {
                    width = 7f;
                    height = 11f;
                    lifetime = 24f;
                    hitSize = 3.5f;
                    keepVelocity = false;
                    collides = false;
                    collidesTiles = false;
                    collidesAir = true;
                    collidesGround = true;
                    hittable = false;
                    absorbable = false;
                    reflectable = false;
                    pierceArmor = true;
                    despawnHit = false;
                    hitColor = backColor = trailColor = Color.valueOf("ff5d5d");
                    frontColor = Color.white;
                    trailWidth = 2.1f;
                    trailLength = 7;
                    shootEffect = Fx.shootSmall;
                    smokeEffect = Fx.shootSmallSmoke;
                    hitEffect = despawnEffect = Fx.hitBulletColor;
                }

                @Override
                public void update(Bullet b){
                    Unit target = b.data instanceof Unit u ? u : null;
                    if(!ghostStableAimValidTarget(target)){
                        b.remove();
                        return;
                    }

                    float tx = target.x, ty = target.y;
                    b.vel.setAngle(Angles.moveToward(b.rotation(), b.angleTo(tx, ty), 80f * Time.delta));
                    b.vel.setLength(speed);
                    b.rotation(b.vel.angle());

                    float hitRange = 4f + target.hitSize / 2f;
                    if(Mathf.within(b.x, b.y, tx, ty, hitRange)){
                        hit(b, tx, ty);
                        float amount = b.damage;
                        if(target.type.unitClasses.contains(UnitClass.psionic)){
                            amount += ghostStableAimPsionicBonus;
                        }
                        target.damagePierce(amount);
                        b.remove();
                    }
                }

                @Override
                public void createSplashDamage(Bullet b, float x, float y){
                    //Target-only damage.
                }
            };

            ghostEmpBullet = new BasicBulletType(ghostEmpProjectileSpeed, 0f){
                {
                    width = 7f;
                    height = 10f;
                    lifetime = 42f;
                    hitSize = 3.6f;
                    keepVelocity = false;
                    collides = false;
                    collidesTiles = false;
                    collidesAir = false;
                    collidesGround = false;
                    hittable = false;
                    absorbable = false;
                    reflectable = false;
                    despawnHit = false;
                    hitColor = backColor = trailColor = Color.valueOf("7ecaff");
                    frontColor = Color.white;
                    trailWidth = 1.8f;
                    trailLength = 10;
                    shootEffect = Fx.shootSmall;
                    smokeEffect = Fx.shootSmallSmoke;
                    hitEffect = Fx.none;
                    despawnEffect = Fx.none;
                }

                @Override
                public void update(Bullet b){
                    Vec2 target = b.data instanceof Vec2 v ? v : null;
                    if(target == null){
                        b.remove();
                        return;
                    }

                    float tx = target.x, ty = target.y;
                    b.vel.setAngle(Angles.moveToward(b.rotation(), b.angleTo(tx, ty), 45f * Time.delta));
                    b.vel.setLength(speed);
                    b.rotation(b.vel.angle());

                    float hitRange = 4f;
                    if(Mathf.within(b.x, b.y, tx, ty, hitRange) || b.time >= b.lifetime - 0.01f){
                        hit(b, tx, ty);
                        impactGhostEmp(b.owner instanceof Unit u ? u : null, tx, ty);
                        b.remove();
                    }
                }

                @Override
                public void createSplashDamage(Bullet b, float x, float y){
                    //No bullet splash; EMP logic is handled manually.
                }
            };

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
                    float baseDamage = prev + infantryWeaponBaseDamageBonus(b.team);
                    b.damage = baseDamage;
                    if(entity instanceof Unit && ((Unit)entity).type.armorType == ArmorType.light){
                        b.damage = prev + 10f + infantryWeaponGhostLightBonus(b.team);
                    }
                    super.hitEntity(b, entity, health);
                    b.damage = prev;
                }

                @Override
                public float buildingDamage(Bullet b){
                    return b.damage + infantryWeaponBaseDamageBonus(b.team);
                }
                };
            }});
        }
        @Override
        public void update(Unit unit){
            super.update(unit);
            updateGhost(unit);
        }
        @Override
        public void draw(Unit unit){
            float prevX = Draw.xscl, prevY = Draw.yscl;
            Draw.scl(prevX * 0.7f, prevY * 0.7f);
            super.draw(unit);
            Draw.scl(prevX, prevY);
            drawGhostStableAimBeam(unit);
        }
        @Override
        public void killed(Unit unit){
            clearGhostStableAimData(unit);
            clearGhostEmpData(unit);
        }
        @Override
        public void load(){
            float prevRatio = spriteHitSizeRatio;
            spriteHitSizeRatio = -1f;
            super.load();
            spriteHitSizeRatio = prevRatio;
            region = Core.atlas.find("atrax");
            outlineRegion = region;
            baseRegion = Core.atlas.find("nova-base", region);
            legRegion = Core.atlas.find("dagger-leg", legRegion);
            fullIcon = Core.atlas.find("unit-atrax-full", region);
            uiIcon = Core.atlas.find("unit-atrax-ui", fullIcon);
            shadowRegion = fullIcon;
            clipSize = Math.max(region.width * 2f, clipSize);
            hitSize = 0.825f * tilesize;
            applySpriteHitSizeRatio();
        }
        };

        scepter = new UnitType("scepter"){
            @Override
            public void draw(Unit unit){
                float prevX = Draw.xscl, prevY = Draw.yscl;
                Draw.scl(prevX * scepterVisualScale, prevY * scepterVisualScale);
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
                visualHitSizeScale = scepterVisualScale;
                speed = 2.62f;
                spriteDrawScale = scepterVisualScale;
                hitSizeFromRegion = false;
                spriteHitSizeRatio = 1.1f / scepterVisualScale * 1.2f;
                hitSize = 1.875f * tilesize;
                // This footprint still fits 2-tile corridors; large-ground pathing is too conservative here.
                flowfieldPathType = Pathfinder.costGround;
                pathCost = ControlPathfinder.costGround;
                fogRadius = 11f;
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
                    Draw.scl(prevX * 1.35f * scepterVisualScale, prevY * 1.35f * scepterVisualScale);
                    super.draw(unit, mount);
                    Draw.scl(prevX, prevY);
                }

                @Override
                public void drawOutline(Unit unit, WeaponMount mount){
                    if(!scepterDisplayBurstMode(unit)) return;
                    float prevX = Draw.xscl, prevY = Draw.yscl;
                    Draw.scl(prevX * 1.35f * scepterVisualScale, prevY * 1.35f * scepterVisualScale);
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
                    y = 1f * scepterVisualScale;
                    x = 10.5f * scepterVisualScale;
                    shootY = 5f * scepterVisualScale;
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
                        shots = 2;
                        shotDelay = 3f;
                    }};

                    bullet = new MissileBulletType(4f, 6f, "missile-large"){
                        {
                            rangeOverride = 10f * tilesize;
                            width = 4f;
                            height = 20f / 3f;
                            lifetime = 35f;
                            hitSize = 6f;
                            homingPower = 0f;
                            weaveMag = 0f;
                            weaveScale = 0f;
                            hitColor = backColor = trailColor = Color.valueOf("feb380");
                            frontColor = Color.white;
                            trailWidth = 4f / 3f;
                            trailLength = 9;
                            hitEffect = thorAirHitEffect;
                            despawnEffect = Fx.none;
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

                            if(!canTrackFriendlyOnlyWhenForced(b, target)) target = null;
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

                                float amount = damage + vehicleWeaponScepterBurstBaseBonus(b.team);
                                if(unit.type.armorType == ArmorType.light){
                                    amount = damage + 6f + vehicleWeaponScepterBurstLightBonus(b.team);
                                }
                                unit.damagePierce(Math.max(amount - unit.armor(), 0.5f));

                                float radius = splashDamageRadius;
                                if(radius > 0f && splashDamage > 0f){
                                    Cons<Unit> splash = other -> {
                                        if(other == unit || !other.isFlying()) return;
                                        if(!other.within(tx, ty, radius + other.hitSize / 2f)) return;
                                        other.damagePierce(Math.max(splashDamage - other.armor(), 0.5f));
                                    };
                                    if(forcedFriendlyAttackTarget(b) != null){
                                        Units.nearby((Team)null, tx - radius, ty - radius, radius * 2f, radius * 2f, splash);
                                    }else{
                                        Units.nearbyEnemies(b.team, tx - radius, ty - radius, radius * 2f, radius * 2f, splash);
                                    }
                                }

                                b.remove();
                            }
                        }

                        @Override
                        public void createSplashDamage(Bullet b, float x, float y){
                            // Thor explosive payload uses flat area damage, not Mindustry splash falloff.
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
                    Draw.scl(prevX * scepterVisualScale, prevY * scepterVisualScale);
                    super.draw(unit, mount);
                    Draw.scl(prevX, prevY);
                }

                @Override
                public void drawOutline(Unit unit, WeaponMount mount){
                    if(!scepterDisplayImpactMode(unit)) return;
                    float prevX = Draw.xscl, prevY = Draw.yscl;
                    Draw.scl(prevX * scepterVisualScale, prevY * scepterVisualScale);
                    super.drawOutline(unit, mount);
                    Draw.scl(prevX, prevY);
                }

                @Override
                protected void handleBullet(Unit unit, WeaponMount mount, Bullet bullet){
                    super.handleBullet(unit, mount, bullet);
                    bullet.data = mount.target;
                }

                @Override
                protected void shoot(Unit unit, WeaponMount mount, float shootX, float shootY, float rotation){
                    if(!flipSprite){
                        super.shoot(unit, mount, shootX, shootY, rotation);
                        return;
                    }

                    float
                    weaponRotation = unit.rotation - 90f + (rotate ? mount.rotation : baseRotation),
                    mountX = unit.x + Angles.trnsx(unit.rotation - 90f, x, y),
                    mountY = unit.y + Angles.trnsy(unit.rotation - 90f, x, y),
                    bulletX = mountX + Angles.trnsx(weaponRotation, this.shootX, this.shootY),
                    bulletY = mountY + Angles.trnsy(weaponRotation, this.shootX, this.shootY),
                    shootAngle = bulletRotation(unit, mount, bulletX, bulletY),
                    angle = shootAngle + Mathf.range(inaccuracy + bullet.inaccuracy);

                    shootSound.at(bulletX, bulletY, Mathf.random(soundPitchMin, soundPitchMax), shootSoundVolume);
                    ejectEffect.at(mountX, mountY, angle * Mathf.sign(this.x));
                    bullet.shootEffect.at(bulletX, bulletY, angle, bullet.hitColor, unit);
                    bullet.smokeEffect.at(bulletX, bulletY, angle, bullet.hitColor, unit);

                    unit.vel.add(Tmp.v1.trns(shootAngle + 180f, bullet.recoil));
                    Effect.shake(shake, shake, bulletX, bulletY);
                    mount.recoil = 1f;
                    if(recoils > 0){
                        mount.recoils[mount.barrelCounter % recoils] = 1f;
                    }
                    mount.heat = 1f;
                }

                {
                    top = false;
                    y = -2f * scepterVisualScale;
                    x = 5f;
                    shootY = 5f * scepterVisualScale;
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

                    bullet = new PointBulletType(){
                        {
                            damage = 25f;
                            rangeOverride = 11f * tilesize;
                            hitEffect = thorAirHitEffect;
                            despawnEffect = Fx.none;
                            shootEffect = Fx.shootSmall;
                            smokeEffect = Fx.none;
                            trailEffect = Fx.none;

                            collides = false;
                            collidesTiles = false;
                            collidesAir = true;
                            collidesGround = false;
                        }

                        @Override
                        public void hitEntity(Bullet b, Hitboxc entity, float health){
                            float prev = b.damage;
                            float amount = damage + vehicleWeaponScepterImpactBaseBonus(b.team);
                            if(entity instanceof Unit unit && unit.type.unitClasses.contains(UnitClass.heavy)){
                                amount = damage + 10f + vehicleWeaponScepterImpactHeavyBonus(b.team);
                            }
                            b.damage = amount;
                            super.hitEntity(b, entity, health);
                            b.damage = prev;

                            if(!(entity instanceof Unit unit)) return;

                            float tx = unit.x, ty = unit.y;
                            float radius = 0.5f * tilesize;
                            float splashDamage = 25f;
                            Cons<Unit> splash = other -> {
                                if(other == unit || !other.isFlying()) return;
                                if(!other.within(tx, ty, radius + other.hitSize / 2f)) return;
                                other.damagePierce(Math.max(splashDamage - other.armor(), 0.5f));
                            };

                            if(forcedFriendlyAttackTarget(b) != null){
                                Units.nearby((Team)null, tx - radius, ty - radius, radius * 2f, radius * 2f, splash);
                            }else{
                                Units.nearbyEnemies(b.team, tx - radius, ty - radius, radius * 2f, radius * 2f, splash);
                            }
                        }

                        @Override
                        public float buildingDamage(Bullet b){
                            return 0f;
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
                    Draw.scl(prevX * 1.35f * scepterVisualScale, prevY * 1.35f * scepterVisualScale);
                    super.draw(unit, mount);
                    Draw.scl(prevX, prevY);
                }

                @Override
                public void drawOutline(Unit unit, WeaponMount mount){
                    float prevX = Draw.xscl, prevY = Draw.yscl;
                    Draw.scl(prevX * 1.35f * scepterVisualScale, prevY * 1.35f * scepterVisualScale);
                    super.drawOutline(unit, mount);
                    Draw.scl(prevX, prevY);
                }

                {
                reload = 0.91f * 60f;
                x = 12.5f * scepterVisualScale;
                y = 1f * scepterVisualScale;
                shootY = 8f * scepterVisualScale;
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

                bullet = new PointBulletType(){
                    {
                        damage = 30f;
                        rangeOverride = 7f * tilesize;
                        collidesAir = false;
                        collidesGround = true;
                        hitEffect = thorGroundHitEffect;
                        despawnEffect = Fx.none;
                        shootEffect = Fx.hitBulletBig;
                        smokeEffect = Fx.none;
                        trailEffect = Fx.none;
                    }

                    @Override
                    public void hitEntity(Bullet b, Hitboxc entity, float health){
                        float prev = b.damage;
                        b.damage = prev + vehicleWeaponScepterGroundBaseBonus(b.team);
                        super.hitEntity(b, entity, health);
                        b.damage = prev;
                    }

                    @Override
                    public float buildingDamage(Bullet b){
                        return b.damage + vehicleWeaponScepterGroundBaseBonus(b.team);
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
            accel = 0.1f;
            drag = 0.11f;
            hitSizeFromRegion = false;
            spriteHitSizeRatio = 1.5f;
            hitSize = 0.875f * tilesize;
            float scvMeleeRange = 0.01f * tilesize;
            range = scvMeleeRange;
            maxRange = scvMeleeRange;
            fogRadius = 8f;
            health = 45f;
            armor = 1f;
            armorType = ArmorType.light;
            unitClasses = EnumSet.of(UnitClass.biological, UnitClass.mechanical);
            omniMovement = true;
            rotateMoveFirst = false;

            buildSpeed = 1f;
            commands = Seq.with(UnitCommand.moveCommand, UnitCommand.harvestCommand, UnitCommand.repairCommand);

            ammoType = new PowerAmmoType(1000);

            weapons.add(new Weapon("scv-touch-weapon"){{
                reload = 1.07f * 60f;
                shootCone = 20f;
                mirror = false;
                rotate = true;
                top = false;
                x = 0f;
                y = 0f;
                shootY = 0f;
                recoil = 0f;
                shake = 0f;
                shootSound = Sounds.none;
                ejectEffect = Fx.none;
                targetAir = false;
                targetGround = true;

                bullet = new PointBulletType(){{
                    damage = 5f;
                    rangeOverride = scvMeleeRange;
                    collidesAir = false;
                    collidesGround = true;
                    collidesTiles = true;
                    shootEffect = Fx.none;
                    smokeEffect = Fx.none;
                    hitEffect = Fx.none;
                    despawnEffect = Fx.none;
                    trailEffect = Fx.none;
                    hitSound = Sounds.none;
                }};
            }});
        }};

        pulsar = new UnitType("pulsar"){{
            speed = 3.94f;
            accel = 10f;
            hitSizeFromRegion = false;
            spriteHitSizeRatio = 1.1f;
            hitSize = 0.825f * tilesize;
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
            hitSizeFromRegion = false;
            spriteHitSizeRatio = 1.5f;
            hitSize = 0.875f * tilesize;
            fogRadius = 7f;
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
                    if(!canTrackFriendlyOnlyWhenForced(b, target)){
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
                        }else if(target instanceof Building build && canDamageFriendlyOnlyWhenForced(b, build)){
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

                    float amount = splashDamage * b.damageMultiplier();
                    Units.nearby(b.team, x - splashDamageRadius, y - splashDamageRadius, splashDamageRadius * 2f, splashDamageRadius * 2f, u -> {
                        if(!u.checkTarget(collidesAir, collidesGround) || !u.hittable()) return;
                        if(!u.within(x, y, splashDamageRadius + u.hitSize / 2f)) return;

                        float dist = scaledSplashDamage ? Math.max(0f, u.dst(x, y) - u.hitSize / 2f) : u.dst(x, y);
                        float scaled = splashDamageRadius <= 0.00001f ? 1f : Mathf.lerp(1f - dist / splashDamageRadius, 1f, 0.4f);
                        u.damage(amount * scaled);

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
            TextureRegion mechRegion, mechOutlineRegion, mechCellRegion, mechLegRegion, mechBaseRegion;

            private void drawVikingMechFeet(Unit unit){
                if(mechLegRegion == null || !mechLegRegion.found()) return;

                float baseRot = unit.rotation;
                float drawRot = baseRot - 90f;
                float walk = unit.vel.len() > 0.05f ? Mathf.sin(Time.time / 5.5f + unit.id * 1.37f) : 0f;
                float extension = walk * 2.35f;
                float strideNorm = Math.max(Math.abs(extension) / 2.35f, unit.hitTime);
                float prev = Draw.z();

                Draw.z(Layer.groundUnit - 0.025f);

                for(int side : Mathf.signs){
                    Draw.mixcol(Tmp.c1.set(mechLegColor).lerp(Color.white, Mathf.clamp(unit.hitTime)), strideNorm);

                    float legX = unit.x + Angles.trnsx(baseRot, extension * side, -0.35f * side);
                    float legY = unit.y + Angles.trnsy(baseRot, extension * side, -0.35f * side) - 1.05f;
                    float legHeight = mechLegRegion.height * mechLegRegion.scl() * (1f - Math.max(-walk * side, 0f) * 0.45f);

                    Draw.rect(mechLegRegion,
                        legX, legY,
                        mechLegRegion.width * mechLegRegion.scl() * side,
                        legHeight,
                        drawRot
                    );
                }

                Draw.mixcol(Color.white, unit.hitTime);
                applyColor(unit);
                if(mechBaseRegion != null && mechBaseRegion.found()){
                    Draw.rect(mechBaseRegion, unit.x, unit.y - 0.95f, drawRot);
                }

                Draw.z(prev);
                Draw.reset();
            }

            @Override
            public void load(){
                super.load();
                mechRegion = copyScaledRegion(Core.atlas.find("flare-round", region), appliedSpriteScale);
                mechOutlineRegion = copyScaledRegion(Core.atlas.find("flare-round-outline", "blank"), appliedSpriteScale);
                mechCellRegion = copyScaledRegion(Core.atlas.find("flare-round-cell", "blank"), appliedSpriteScale);
                mechLegRegion = copyScaledRegion(Core.atlas.find("dagger-leg", "blank"), appliedSpriteScale * 0.92f);
                mechBaseRegion = copyScaledRegion(Core.atlas.find("dagger-base", "blank"), appliedSpriteScale * 0.92f);
                clipSize = Math.max(clipSize, mechRegion.width * mechRegion.scl() * 2f);
            }

            @Override
            public void update(Unit unit){
                super.update(unit);
                updateViking(unit);
            }

            @Override
            public void draw(Unit unit){
                if(vikingIsMechMode(unit)){
                    drawVikingMechFeet(unit);
                }
                super.draw(unit);
            }

            @Override
            public void drawOutline(Unit unit){
                Draw.reset();

                TextureRegion drawRegion = vikingIsMechMode(unit) ? mechOutlineRegion : outlineRegion;
                if(Core.atlas.isFound(drawRegion)){
                    applyColor(unit);
                    applyOutlineColor(unit);
                    Draw.rect(drawRegion, unit.x, unit.y, unit.rotation - 90f);
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

                TextureRegion drawRegion = vikingIsMechMode(unit) ? mechRegion : region;
                Draw.rect(drawRegion, unit.x, unit.y, unit.rotation - 90f);
                Draw.reset();
            }

            @Override
            public void drawCell(Unit unit){
                applyColor(unit);

                TextureRegion drawRegion = vikingIsMechMode(unit) ? mechCellRegion : cellRegion;
                if(Core.atlas.isFound(drawRegion)){
                    Draw.color(cellColor(unit));
                    Draw.rect(drawRegion, unit.x, unit.y, unit.rotation - 90f);
                }
                Draw.reset();
            }

            @Override
            public void drawShadow(Unit unit){
                if(vikingIsMechMode(unit)) return;
                super.drawShadow(unit);
            }

            @Override
            public void killed(Unit unit){
                clearVikingData(unit);
            }

            {
                visualHitSizeScale = 1f;
                researchCostMultiplier = 0.5f;
                speed = 3.85f;
                accel = 0.08f;
                drag = 0.04f;
                flying = true;
                health = 135f;
                fogRadius = 10f;
                armor = 0f;
                armorType = ArmorType.heavy;
                unitClasses = EnumSet.of(UnitClass.mechanical);
                population = 2;
                engineOffset = 5.75f;
                targetFlags = new BlockFlag[]{BlockFlag.generator, null};
                hitSizeFromRegion = false;
                spriteHitSizeRatio = 1f;
                hitSize = 1.875f * tilesize;
                itemCapacity = 10;
                circleTarget = false;
                omniMovement = false;
                rotateSpeed = 3f; // 180 deg/sec
                circleTargetRadius = 60f;
                wreckSoundVolume = 0.7f;
                range = maxRange = 9f * tilesize;
                targetAir = true;
                targetGround = true;

                moveSound = Sounds.loopThruster;
                moveSoundPitchMin = 0.3f;
                moveSoundPitchMax = 1.5f;
                moveSoundVolume = 0.2f;

                weapons.add(new Weapon(){
                    @Override
                    public void drawOutline(Unit unit, WeaponMount mount){
                        if(vikingIsMechMode(unit) || vikingIsTransforming(unit)) return;
                        super.drawOutline(unit, mount);
                    }

                    @Override
                    public void draw(Unit unit, WeaponMount mount){
                        if(vikingIsMechMode(unit) || vikingIsTransforming(unit)) return;
                        super.draw(unit, mount);
                    }

                    @Override
                    public void update(Unit unit, WeaponMount mount){
                        if(vikingIsMechMode(unit) || vikingIsTransforming(unit)){
                            mount.shoot = false;
                            mount.rotate = false;
                            mount.warmup = Mathf.approachDelta(mount.warmup, 0f, 0.1f);
                            mount.heat = Mathf.approachDelta(mount.heat, 0f, 0.1f);
                            return;
                        }
                        super.update(unit, mount);
                    }

                    @Override
                    protected void handleBullet(Unit unit, WeaponMount mount, Bullet bullet){
                        super.handleBullet(unit, mount, bullet);
                        Teamc lockedTarget = hurricaneTarget(unit);
                        Teamc trackedTarget = mount.target != null ? mount.target : lockedTarget;
                        bullet.data = new HurricaneMissileData(trackedTarget, lockedTarget != null);
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
                                    if(canDamageFriendlyOnlyWhenForced(b, target)){
                                        float amount = damage + shipWeaponVikingFighterBaseBonus(b.team);
                                        if(target instanceof Unit u && u.type.armorType == ArmorType.heavy){
                                            amount = 14f + shipWeaponVikingFighterHeavyBonus(b.team);
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

                weapons.add(new Weapon("viking-gatling"){
                    @Override
                    public void drawOutline(Unit unit, WeaponMount mount){
                        if(!vikingIsMechMode(unit) || vikingIsTransforming(unit)) return;
                        super.drawOutline(unit, mount);
                    }

                    @Override
                    public void draw(Unit unit, WeaponMount mount){
                        if(!vikingIsMechMode(unit) || vikingIsTransforming(unit)) return;
                        super.draw(unit, mount);
                    }

                    @Override
                    public void update(Unit unit, WeaponMount mount){
                        if(!vikingIsMechMode(unit) || vikingIsTransforming(unit)){
                            mount.shoot = false;
                            mount.rotate = false;
                            mount.warmup = Mathf.approachDelta(mount.warmup, 0f, 0.1f);
                            mount.heat = Mathf.approachDelta(mount.heat, 0f, 0.1f);
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
                        x = 0f;
                        y = 1f;
                        minShootVelocity = -1f;
                        mirror = false;
                        shootCone = 10f;
                        reload = 0.71f * 60f;
                        targetAir = false;
                        targetGround = true;
                        ejectEffect = Fx.none;
                        shootSound = Sounds.shoot;

                        bullet = new BulletType(0f, 12f){
                            @Override
                            public void init(Bullet b){
                                super.init(b);

                                Teamc target = b.data instanceof Teamc t ? t : null;
                                if(target instanceof Healthc h && h.isValid() && canDamageFriendlyOnlyWhenForced(b, target)){
                                    float damage = 12f + shipWeaponVikingMechBaseBonus(b.team);
                                    if(target instanceof Unit u && u.type.unitClasses.contains(UnitClass.mechanical)){
                                        damage = 20f + shipWeaponVikingMechMechanicalBonus(b.team);
                                    }
                                    h.damage(damage);
                                }
                                b.remove();
                            }

                            {
                                instantDisappear = true;
                                lifetime = 1f;
                                rangeOverride = vikingMechRange;
                                collides = false;
                                collidesTiles = false;
                                collidesAir = false;
                                collidesGround = true;
                                keepVelocity = false;
                                hittable = false;
                                absorbable = false;
                                reflectable = false;
                                shootEffect = Fx.none;
                                smokeEffect = Fx.none;
                                hitEffect = Fx.none;
                                despawnEffect = Fx.none;
                            }
                        };
                    }
                });
            }
        };

        liberator = new UnitType("liberator"){
            @Override
            public void load(){
                float prevRatio = spriteHitSizeRatio;
                spriteHitSizeRatio = -1f;
                super.load();
                spriteHitSizeRatio = prevRatio;
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
                    if(obviate != null){
                        parts.clear();
                        for(var part : obviate.parts){
                            DrawPart partCopy = copyObviatePart(part);
                            if(partCopy != null){
                                parts.add(partCopy);
                            }
                        }
                        for(var part : parts){
                            part.load(copy);
                        }
                        for(Weapon weapon : weapons){
                            if("elude-weapon".equals(weapon.name)){
                                weapon.name = "obviate-weapon";
                                weapon.load();
                            }
                        }
                    }
                    applySpriteHitSizeRatio();
                    engineSize = 2.15f * appliedSpriteScale;
                    engineOffset = (54f / 4f) * appliedSpriteScale;
                }

            {
                speed = 4.72f;
                accel = 0.09f;
                drag = 0.03f;
                flying = true;
                lowAltitude = true;
                rotateSpeed = 8f;
                health = 180f;
                fogRadius = 9f;
                armor = 0f;
                armorType = ArmorType.heavy;
                unitClasses = EnumSet.of(UnitClass.mechanical);
                fullOverride = "obviate";
                population = 3;
                hitSizeFromRegion = false;
                spriteHitSizeRatio = 1.2f;
                hitSize = 1.875f * tilesize;
                  engineSize = 2.15f;
                engineOffset = 54f / 4f;
                range = maxRange = liberatorFighterRange();
                targetAir = true;
                targetGround = true;
                faceTarget = false;
                omniMovement = false;
                itemCapacity = 0;

                engines.clear();

                  weapons.add(new Weapon("elude-weapon"){
                      @Override
                      public void drawOutline(Unit unit, WeaponMount mount){
                          if(!isLiberator(unit) || getLiberatorData(unit).defenseMode) return;
                          float prevRot = mount.rotation;
                          float prevTarget = mount.targetRotation;
                          mount.rotation = baseRotation;
                          mount.targetRotation = baseRotation;
                          super.drawOutline(unit, mount);
                          mount.rotation = prevRot;
                          mount.targetRotation = prevTarget;
                      }

                      @Override
                      public void draw(Unit unit, WeaponMount mount){
                          if(!isLiberator(unit) || getLiberatorData(unit).defenseMode) return;
                          float prevRot = mount.rotation;
                          float prevTarget = mount.targetRotation;
                          mount.rotation = baseRotation;
                          mount.targetRotation = baseRotation;
                          super.draw(unit, mount);
                          mount.rotation = prevRot;
                          mount.targetRotation = prevTarget;
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
                          y = 0f;
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
                                    if(canDamageFriendlyOnlyWhenForced(b, target)){
                                        float amount = damage + shipWeaponLiberatorFighterBonus(b.team);
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
                          float prevRot = mount.rotation;
                          float prevTarget = mount.targetRotation;
                          mount.rotation = baseRotation;
                          mount.targetRotation = baseRotation;
                          super.drawOutline(unit, mount);
                          mount.rotation = prevRot;
                          mount.targetRotation = prevTarget;
                      }

                      @Override
                      public void draw(Unit unit, WeaponMount mount){
                          if(!isLiberator(unit) || !getLiberatorData(unit).defenseMode) return;
                          float prevRot = mount.rotation;
                          float prevTarget = mount.targetRotation;
                          mount.rotation = baseRotation;
                          mount.targetRotation = baseRotation;
                          super.draw(unit, mount);
                          mount.rotation = prevRot;
                          mount.targetRotation = prevTarget;
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
                        if(mount.target != null && !liberatorTargetInZone(unit, mount.target)){
                            mount.target = null;
                            mount.shoot = false;
                            mount.rotate = false;
                        }
                    }

                    @Override
                    protected Teamc findTarget(Unit unit, float x, float y, float range, boolean air, boolean ground){
                        if(!liberatorIsDefending(unit)) return null;
                        if(unit.controller() instanceof CommandAI ai){
                            Teamc forced = ai.attackTarget;
                            if(forced != null && forced.team() == unit.team && liberatorTargetInZone(unit, forced)){
                                return forced;
                            }
                        }
                        return Units.closestEnemy(unit.team, unit.x, unit.y, liberatorDefenseAcquireRange(unit.team),
                        u -> !u.isFlying() && liberatorTargetInZone(unit, u));
                    }

                    @Override
                    protected boolean checkTarget(Unit unit, Teamc target, float x, float y, float range){
                        if(!liberatorIsDefending(unit)) return true;
                        if(target instanceof Building) return true;
                        if(target != null && target.team() == unit.team){
                            if(unit.controller() instanceof CommandAI ai && ai.attackTarget == target){
                                return !liberatorTargetInZone(unit, target);
                            }
                            return true;
                        }
                        if(!(target instanceof Unit u) || u.isFlying()) return true;
                        if(!liberatorTargetInZone(unit, target)) return true;
                        return super.checkTarget(unit, target, x, y, range);
                    }

                    {
                        shootSound = Sounds.shoot;
                        x = 4f;
                          y = 0f;
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

                        bullet = new BasicBulletType(13.333f, 75f){
                            private final Color advancedColor = Color.valueOf("5fb4ff");

                            @Override
                            public void hitEntity(Bullet b, Hitboxc entity, float health){
                                float prev = b.damage;
                                b.damage = prev + shipWeaponLiberatorDefenseBonus(b.team);
                                super.hitEntity(b, entity, health);
                                b.damage = prev;
                            }

                            @Override
                            public float buildingDamage(Bullet b){
                                return 0f;
                            }

                            @Override
                            public void draw(Bullet b){
                                Color prevBack = backColor;
                                Color prevFront = frontColor;
                                Color prevTrail = trailColor;
                                Color prevHit = hitColor;

                                if(liberatorAdvancedBallisticsLevel(b.team) > 0){
                                    backColor = advancedColor;
                                    trailColor = advancedColor;
                                    hitColor = advancedColor;
                                    frontColor = Color.white;
                                }

                                super.draw(b);

                                backColor = prevBack;
                                frontColor = prevFront;
                                trailColor = prevTrail;
                                hitColor = prevHit;
                            }

                            {
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
                            }
                        };
                    }
                });
            }

            @Override
            public void update(Unit unit){
                super.update(unit);
                updateLiberator(unit);
            }

            @Override
            public boolean allowCommand(Unit unit, UnitCommand command){
                if(command == UnitCommand.moveCommand && liberatorIsDefending(unit)){
                    return false;
                }
                return super.allowCommand(unit, command);
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
                updateBansheeAfterburner(unit);
            }

            {
                health = 140f;
                speed = 3.85f;
                accel = 0.09f;
                drag = 0.08f;
                flying = true;
                hitSizeFromRegion = false;
                spriteHitSizeRatio = 1f;
                hitSize = 1.875f * tilesize;
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
                fogRadius = 10f;
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

                    bullet = new MissileBulletType(4.2f, 12f, "missile"){
                        @Override
                        public void hitEntity(Bullet b, Hitboxc entity, float health){
                            float prev = b.damage;
                            b.damage = prev + shipWeaponBansheeBonus(b.team);
                            super.hitEntity(b, entity, health);
                            b.damage = prev;
                        }

                        @Override
                        public float buildingDamage(Bullet b){
                            return b.damage + shipWeaponBansheeBonus(b.team);
                        }

                        {
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
                        }
                    };
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
                float prevRatio = spriteHitSizeRatio;
                spriteHitSizeRatio = -1f;
                super.load();
                spriteHitSizeRatio = prevRatio;
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
                applySpriteHitSizeRatio();

                float offsetScale = appliedSpriteScale;
                if(!Mathf.equal(offsetScale, 1f, 0.0001f)){
                    engineOffset *= offsetScale;
                    engineSize *= offsetScale;
                    for(Weapon weapon : weapons){
                        if(weapon == null || weapon.name == null) continue;
                        if(weapon.name.equals("battlecruiser-ground-laser") || weapon.name.equals("battlecruiser-air-laser")){
                            weapon.x *= offsetScale;
                            weapon.y *= offsetScale;
                            weapon.shootY *= offsetScale;
                        }
                    }
                }
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
            public void drawOutline(Unit unit){
                Draw.reset();

                if(Core.atlas.isFound(outlineRegion)){
                    applyColor(unit);
                    applyOutlineColor(unit);
                    Draw.rect(outlineRegion, unit.x, unit.y,
                    outlineRegion.width * outlineRegion.scl() * battlecruiserTextureXScale,
                    outlineRegion.height * outlineRegion.scl(),
                    unit.rotation - 90f);
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

                Draw.rect(region, unit.x, unit.y,
                region.width * region.scl() * battlecruiserTextureXScale,
                region.height * region.scl(),
                unit.rotation - 90f);

                Draw.reset();
            }

            @Override
            public void drawCell(Unit unit){
                applyColor(unit);

                Draw.color(cellColor(unit));
                Draw.rect(cellRegion, unit.x, unit.y,
                cellRegion.width * cellRegion.scl() * battlecruiserTextureXScale,
                cellRegion.height * cellRegion.scl(),
                unit.rotation - 90f);
                Draw.reset();
            }

            @Override
            public void drawShadow(Unit unit){
                float e = Mathf.clamp(unit.elevation, shadowElevation, 1f) * shadowElevationScl * (1f - unit.drownTime);
                float x = unit.x + shadowTX * e, y = unit.y + shadowTY * e;
                mindustry.world.blocks.environment.Floor floor = world.floorWorld(x, y);

                float dest = floor.canShadow ? 1f : 0f;
                unit.shadowAlpha = unit.shadowAlpha < 0 ? dest : Mathf.approachDelta(unit.shadowAlpha, dest, 0.11f);
                Draw.color(Pal.shadow, Pal.shadow.a * unit.shadowAlpha);

                Draw.rect(shadowRegion, x, y,
                shadowRegion.width * shadowRegion.scl() * battlecruiserTextureXScale,
                shadowRegion.height * shadowRegion.scl(),
                unit.rotation - 90f);
                Draw.color();
            }

            @Override
            public void drawSoftShadow(Unit unit){
                Draw.color(0f, 0f, 0f, 0.4f);
                float rad = 1.6f;
                float size = Math.max(region.width, region.height) * region.scl() * softShadowScl;
                Draw.rect(softShadowRegion, unit.x, unit.y,
                size * rad * battlecruiserTextureXScale,
                size * rad,
                unit.rotation - 90f);
                Draw.color();
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
                hitSizeFromRegion = false;
                spriteHitSizeRatio = 1f;
                hitSize = 3.225f * tilesize;
                fogRadius = 12f;
                range = maxRange = battlecruiserWeaponRange;
                targetAir = true;
                targetGround = true;
                singleTarget = true;
                alwaysShootWhenMoving = true;

                loopSound = Sounds.loopHover;

                BulletType laserGround = new LaserBoltBulletType(8f, 8f){
                    private boolean validTarget(@Nullable Teamc target){
                        if(target == null) return false;
                        if(target instanceof Healthc h && !h.isValid()) return false;
                        if(target instanceof Unit u){
                            return u.hittable() && u.checkTarget(false, true);
                        }
                        return target instanceof Building;
                    }

                    @Override
                    public void update(Bullet b){
                        b.keepAlive = true;
                        Teamc target = b.data instanceof Teamc t ? t : null;
                        boolean hadTarget = target != null;

                        if(validTarget(target)){
                            b.aimX = target.getX();
                            b.aimY = target.getY();
                        }else if(hadTarget){
                            float tx = b.aimX, ty = b.aimY;
                            if(Float.isNaN(tx) || Float.isNaN(ty)){
                                tx = b.x;
                                ty = b.y;
                            }
                            hit(b, tx, ty);
                            b.remove();
                            return;
                        }else if(Float.isNaN(b.aimX) || Float.isNaN(b.aimY)){
                            b.remove();
                            return;
                        }

                        float tx = b.aimX, ty = b.aimY;
                        b.vel.setAngle(Angles.moveToward(b.rotation(), b.angleTo(tx, ty), 90f * Time.delta));
                        b.vel.setLength(speed);
                        b.rotation(b.vel.angle());

                        float hitRange = 3f + (target instanceof Sized s ? s.hitSize() / 2f : 0f);
                        if(Mathf.within(b.x, b.y, tx, ty, hitRange)){
                            hit(b, tx, ty);
                            float amount = b.damage + shipWeaponBattlecruiserGroundBonus(b.team);
                            if(target instanceof Unit u){
                                u.damage(amount);
                            }else if(target instanceof Building build){
                                build.damage(amount * buildingDamageMultiplier);
                            }
                            b.remove();
                        }
                    }

                    @Override
                    public void createSplashDamage(Bullet b, float x, float y){
                        //Target-only collision.
                    }

                    {
                        width = 8f;
                        height = 8f;
                        lifetime = 60f;
                        rangeOverride = battlecruiserWeaponRange;
                        collides = false;
                        collidesTiles = false;
                        collidesAir = false;
                        collidesGround = true;
                        hittable = false;
                        absorbable = false;
                        reflectable = false;
                        keepVelocity = false;
                        despawnHit = false;
                        backColor = Color.valueOf("ff5a5a");
                        frontColor = Color.white;
                        trailColor = Color.valueOf("ff5a5a");
                        trailWidth = 2.1f;
                        trailLength = 12;
                        hitEffect = Fx.hitBulletColor;
                        despawnEffect = Fx.none;
                    }
                };

                BulletType laserAir = new LaserBoltBulletType(8f, 5f){
                    private boolean validTarget(@Nullable Teamc target){
                        if(target == null) return false;
                        if(target instanceof Healthc h && !h.isValid()) return false;
                        return target instanceof Unit u && u.hittable() && u.checkTarget(true, false);
                    }

                    @Override
                    public void update(Bullet b){
                        b.keepAlive = true;
                        Teamc target = b.data instanceof Teamc t ? t : null;
                        boolean hadTarget = target != null;

                        if(validTarget(target)){
                            b.aimX = target.getX();
                            b.aimY = target.getY();
                        }else if(hadTarget){
                            float tx = b.aimX, ty = b.aimY;
                            if(Float.isNaN(tx) || Float.isNaN(ty)){
                                tx = b.x;
                                ty = b.y;
                            }
                            hit(b, tx, ty);
                            b.remove();
                            return;
                        }else if(Float.isNaN(b.aimX) || Float.isNaN(b.aimY)){
                            b.remove();
                            return;
                        }

                        float tx = b.aimX, ty = b.aimY;
                        b.vel.setAngle(Angles.moveToward(b.rotation(), b.angleTo(tx, ty), 90f * Time.delta));
                        b.vel.setLength(speed);
                        b.rotation(b.vel.angle());

                        float hitRange = 3f + (target instanceof Sized s ? s.hitSize() / 2f : 0f);
                        if(Mathf.within(b.x, b.y, tx, ty, hitRange)){
                            hit(b, tx, ty);
                            float amount = b.damage + shipWeaponBattlecruiserAirBonus(b.team);
                            if(target instanceof Unit u){
                                u.damage(amount);
                            }
                            b.remove();
                        }
                    }

                    @Override
                    public void createSplashDamage(Bullet b, float x, float y){
                        //Target-only collision.
                    }

                    {
                        width = 8f;
                        height = 8f;
                        lifetime = 60f;
                        rangeOverride = battlecruiserWeaponRange;
                        collides = false;
                        collidesTiles = false;
                        collidesAir = true;
                        collidesGround = false;
                        hittable = false;
                        absorbable = false;
                        reflectable = false;
                        keepVelocity = false;
                        despawnHit = false;
                        backColor = Color.valueOf("ff5a5a");
                        frontColor = Color.white;
                        trailColor = Color.valueOf("ff5a5a");
                        trailWidth = 2.1f;
                        trailLength = 12;
                        hitEffect = Fx.hitBulletColor;
                        despawnEffect = Fx.none;
                    }
                };

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
                new Weapon("battlecruiser-ground-laser"){
                    @Override
                    protected void handleBullet(Unit unit, WeaponMount mount, Bullet bullet){
                        super.handleBullet(unit, mount, bullet);
                        bullet.data = mount.target;
                    }
                    {
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
                new Weapon("battlecruiser-air-laser"){
                    @Override
                    protected void handleBullet(Unit unit, WeaponMount mount, Bullet bullet){
                        super.handleBullet(unit, mount, bullet);
                        bullet.data = mount.target;
                    }
                    {
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
            hitSizeFromRegion = false;
            spriteHitSizeRatio = 1f;
            hitSize = 1.875f * tilesize;
            fogRadius = 11f;
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
                    beamWidth = 0.1f;
                    pulseRadius = 4f;
                    pulseStroke = 1.25f;
                    widthSinMag = 0f;
                    shootCone = 360f;
                    targetUnits = true;
                    targetBuildings = false;
                    controllable = false;
                    autoTarget = true;
                    repairSpeed = 16f / 60f; // 16 HP/s -> 4 energy/s at 1 energy : 4 HP
                    fractionRepairSpeed = 0f;
                    laserColor = Color.valueOf("9df7ff").a(0.2f);
                    laserTopColor = Color.white.cpy().a(0.2f);
                    healColor = Color.valueOf("9df7ff").a(0.2f);

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
                updateMedivacCaduceusReactor(unit);
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
            float weaponRegionScale = 0.49f;
            float bodyScaleX = 0.8f;
            float bodyScaleY = 1.1f;

            @Override
            public void load(){
                super.load();
                if(!Mathf.equal(weaponRegionScale, 1f, 0.0001f)){
                    for(var weapon : weapons){
                        weapon.region = copyScaledRegion(weapon.region, weaponRegionScale);
                        weapon.outlineRegion = copyScaledRegion(weapon.outlineRegion, weaponRegionScale);
                        weapon.heatRegion = copyScaledRegion(weapon.heatRegion, weaponRegionScale);
                        weapon.cellRegion = copyScaledRegion(weapon.cellRegion, weaponRegionScale);
                        if(weapon.shadow > 0f) weapon.shadow *= weaponRegionScale;
                    }
                }
            }

            @Override
            public void drawOutline(Unit unit){
                Draw.reset();

                if(Core.atlas.isFound(outlineRegion)){
                    applyColor(unit);
                    applyOutlineColor(unit);
                    Draw.rect(outlineRegion, unit.x, unit.y,
                        outlineRegion.width * outlineRegion.scale * scaledTankVisualScale / 4f * bodyScaleX,
                        outlineRegion.height * outlineRegion.scale * scaledTankVisualScale / 4f * bodyScaleY,
                        unit.rotation - 90f);
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

                Draw.rect(region, unit.x, unit.y,
                    region.width * region.scale * scaledTankVisualScale / 4f * bodyScaleX,
                    region.height * region.scale * scaledTankVisualScale / 4f * bodyScaleY,
                    unit.rotation - 90f);
                Draw.reset();
            }

            @Override
            public void drawCell(Unit unit){
                applyColor(unit);
                Draw.color(cellColor(unit));
                Draw.rect(cellRegion, unit.x, unit.y,
                    cellRegion.width * cellRegion.scale * scaledTankVisualScale / 4f * bodyScaleX,
                    cellRegion.height * cellRegion.scale * scaledTankVisualScale / 4f * bodyScaleY,
                    unit.rotation - 90f);
                Draw.reset();
            }

            @Override
            public <T extends Unit & Tankc> void drawTank(T unit){
            }

            @Override
            public void drawShadow(Unit unit){
                drawShadowExplicit(shadowRegion, unit, shadowElevation, shadowElevationScl);
            }

            @Override
            public void update(Unit unit){
                super.update(unit);
                if(maceLocusTransforming(unit)){
                    unit.vel.setZero();
                    if(unit.controller() instanceof CommandAI ai){
                        ai.clearCommands();
                    }
                }
            }

            @Override
            public void killed(Unit unit){
                clearMaceLocusTransformData(unit);
            }

            {
                visualHitSizeScale = scaledTankVisualScale;
                hitSizeFromRegion = false;
                spriteHitSizeRatio = 2f;
                hitSize = 0.975f * tilesize;
                fogRadius = 10f;
                speed = 5.95f;
                rotateSpeed = 6f; // 360 deg/sec
                health = 90f;
                armor = 0f;
                armorType = ArmorType.light;
                unitClasses = EnumSet.of(UnitClass.mechanical);
                range = maxRange = 5f * tilesize;
                targetAir = false;
                targetGround = true;
                requireBodyAimToShoot = false;
                faceTarget = false;

                weapons.add(new Weapon("locus-weapon"){
                    private static final float postFireStiffDuration = 0.1f * 60f;
                    private static final float weaponBackOffset = 2f;

                    private boolean shouldCancelCharge(Unit unit, WeaponMount mount, float queuedMoveX, float queuedMoveY, int queuedQueueSize){
                        if(!unit.isAdded() || mount == null) return true;

                        //Simple rule: if player gives a new move command during lock-on, cancel this shot.
                          if(unit.controller() instanceof CommandAI ai && ai.currentCommand() == UnitCommand.moveCommand && ai.attackTarget == null){
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
                        shootY = 10f + weaponBackOffset;
                        rotate = true;
                        rotateSpeed = 6f; // 360 deg/sec
                        mirror = false;
                        x = 0f;
                        y = -weaponBackOffset;
                        reload = 1.79f * 60f;
                        shoot.firstShotDelay = 0.7f * 60f;
                        shootStatus = StatusEffects.none;
                        shootStatusDuration = 0f;

                          bullet = new ContinuousFlameBulletType(8f){
                    final float lightArmorDamage = 14f;

                    {
                            length = 6f * tilesize;
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
                    public void applyDamage(Bullet b){
                        float x1 = b.x, y1 = b.y;
                        float radius = 0.2f * tilesize;
                        Tmp.v1.trnsExact(b.rotation(), currentLength(b));
                        float x2 = x1 + Tmp.v1.x, y2 = y1 + Tmp.v1.y;

                        Rect unitRect = Tmp.r1.setPosition(x1, y1).setSize(Tmp.v1.x, Tmp.v1.y).normalize().grow(radius * 2f);

                        Units.nearby((Team)null, unitRect.x, unitRect.y, unitRect.width, unitRect.height, u -> {
                            if(!canDamageFriendlyOnlyWhenForced(b, u)) return;
                            if(!u.checkTarget(collidesAir, collidesGround) || !u.hittable()) return;
                            u.hitbox(Tmp.r2);
                            Vec2 hit = Geometry.raycastRect(x1, y1, x2, y2, Tmp.r2.grow(radius * 2f));
                            if(hit != null){
                                u.collision(b, hit.x, hit.y);
                                b.collision(u, hit.x, hit.y);
                            }
                        });

                        Units.nearbyBuildings((x1 + x2) / 2f, (y1 + y2) / 2f, currentLength(b) / 2f + radius + 8f, build -> {
                            if(!canDamageFriendlyOnlyWhenForced(b, build) || !build.collide(b)) return;
                            if(!b.checkUnderBuild(build, build.x, build.y)) return;
                            if(!intersectsCircle(x1, y1, x2, y2, build.x, build.y, build.hitSize() / 2f + radius)) return;

                            build.collision(b);
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
                        float baseDamage = prev + vehicleWeaponLocusBaseBonus(b.team);
                        b.damage = baseDamage;
                        if(entity instanceof Unit && ((Unit)entity).type.armorType == ArmorType.light){
                            b.damage = lightArmorDamage + vehicleWeaponLocusLightBonus(b.team) + infernoPreheaterLocusLightBonus(b.team);
                        }
                        super.hitEntity(b, entity, health);
                        b.damage = prev;
                    }

                    @Override
                    public float buildingDamage(Bullet b){
                        float base = b.damage + vehicleWeaponLocusBaseBonus(b.team);
                        return base * buildingDamageMultiplier;
                    }
                        };
                    }
                });
            }
        };

        precept = new TankUnitType("precept"){
            TextureRegion siegeLegRegion, siegeFootRegion;
            float bodyScaleX = 0.81f;
            float bodyScaleY = 0.9f;
            float weaponRegionScale = 0.64f;

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
                treadRegion = Core.atlas.find("blank");
                siegeLegRegion = Core.atlas.find("anthicus-leg");
                siegeFootRegion = Core.atlas.find("anthicus-leg-base", siegeLegRegion);
                if(!Mathf.equal(weaponRegionScale, 1f, 0.0001f)){
                    for(var weapon : weapons){
                        weapon.region = copyScaledRegion(weapon.region, weaponRegionScale);
                        weapon.outlineRegion = copyScaledRegion(weapon.outlineRegion, weaponRegionScale);
                        weapon.heatRegion = copyScaledRegion(weapon.heatRegion, weaponRegionScale);
                        weapon.cellRegion = copyScaledRegion(weapon.cellRegion, weaponRegionScale);
                        if(weapon.shadow > 0f) weapon.shadow *= weaponRegionScale;
                    }
                }
            }

            {
                visualHitSizeScale = scaledTankVisualScale;
                hitSizeFromRegion = false;
                spriteHitSizeRatio = 1.1f * 1.3f * 1.3f * 2f * 0.5f * 1.2f;
                hitSize = 1.575f * tilesize;
                // This footprint still fits 2-tile corridors; large-ground pathing is too conservative here.
                flowfieldPathType = Pathfinder.costGround;
                pathCost = ControlPathfinder.costGround;
                fogRadius = 11f;
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

                    @Override
                    public void draw(Unit unit, WeaponMount mount){
                        if(preceptIsSieged(unit)) return;
                        super.draw(unit, mount);
                    }

                    @Override
                    public void drawOutline(Unit unit, WeaponMount mount){
                        if(preceptIsSieged(unit)) return;
                        super.drawOutline(unit, mount);
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
                                hitEffect = new WrapEffect(Fx.dynamicExplosion, Color.white, 0.25f);
                                despawnEffect = Fx.none;
                                shootEffect = preceptMuzzleSmokeEffect;
                                smokeEffect = Fx.none;
                                trailEffect = Fx.none;
                            }

                            @Override
                            public void hit(Bullet b, float x, float y){
                                Vec2 point = preceptImpactPoint(b, x, y);
                                super.hit(b, point.x, point.y);
                            }

                            @Override
                            public void hitEntity(Bullet b, Hitboxc entity, float health){
                                float amount = 15f + vehicleWeaponPreceptMobileBaseBonus(b.team);
                                if(entity instanceof Unit u && u.type.armorType == ArmorType.heavy){
                                    amount = 25f + vehicleWeaponPreceptMobileHeavyBonus(b.team);
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

                            @Override
                            public float buildingDamage(Bullet b){
                                return 15f + vehicleWeaponPreceptMobileBaseBonus(b.team);
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
                        if(!preceptIsSieged(unit)) return;
                        super.draw(unit, mount);
                    }

                    @Override
                    public void drawOutline(Unit unit, WeaponMount mount){
                        if(!preceptIsSieged(unit)) return;
                        super.drawOutline(unit, mount);
                    }

                    @Override
                    public void load(){
                        super.load();

                        var file = Core.files.internal("sprites/units/weapons/precept-siege-weapon.png");
                        if(file.exists()){
                            region = new TextureRegion(new Texture(file));
                            heatRegion = Core.atlas.find("precept-weapon-heat");
                            cellRegion = Core.atlas.find("precept-weapon-cell");
                        }
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
                        shoot.firstShotDelay = 0.2f * 60f;
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
                                hitEffect = new WrapEffect(Fx.dynamicExplosion, Color.white, 0.75f);
                                despawnEffect = Fx.none;
                                shootEffect = preceptMuzzleSmokeEffect;
                                smokeEffect = Fx.none;
                                trailEffect = Fx.none;
                            }

                            @Override
                            public void hit(Bullet b, float x, float y){
                                Vec2 point = preceptImpactPoint(b, x, y);
                                super.hit(b, point.x, point.y);
                            }

                            @Override
                            public void hitEntity(Bullet b, Hitboxc entity, float health){
                                float amount = 40f + vehicleWeaponPreceptSiegeBaseBonus(b.team);
                                if(entity instanceof Unit u && u.type.armorType == ArmorType.heavy){
                                    amount = 70f + vehicleWeaponPreceptSiegeHeavyBonus(b.team);
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

                                applySplashExcluding(b, entity.x(), entity.y(), entity instanceof Teamc t ? t : null);
                                handlePierce(b, health, entity.x(), entity.y());
                            }

                            @Override
                            public void hitTile(Bullet b, Building build, float x, float y, float initialHealth, boolean direct){
                                super.hitTile(b, build, x, y, initialHealth, direct);
                                applySplashExcluding(b, x, y, build);
                            }

                            @Override
                            public float buildingDamage(Bullet b){
                                return 40f + vehicleWeaponPreceptSiegeBaseBonus(b.team);
                            }

                            @Override
                            public void createSplashDamage(Bullet b, float x, float y){
                                //Splash is applied explicitly in hitEntity/hitTile so the primary target can be excluded.
                            }

                            private void applySplashExcluding(Bullet b, float x, float y, @Nullable Teamc primary){
                                if(splashDamageRadius <= 0f || b.absorbed) return;

                                float radius = splashDamageRadius;
                                float rawDamage = splashDamage * b.damageMultiplier();
                                float area = radius * 2f;

                                Units.nearby(x - radius, y - radius, area, area, u -> {
                                    if(primary == u) return;
                                    if(!u.checkTarget(collidesAir, collidesGround) || !u.hittable()) return;
                                    if(!u.within(x, y, radius + (scaledSplashDamage ? u.hitSize / 2f : 0f))) return;

                                    float dist = scaledSplashDamage ? Math.max(0f, u.dst(x, y) - u.hitSize / 2f) : u.dst(x, y);
                                    float scaled = radius <= 0.00001f ? 1f : Mathf.lerp(1f - dist / radius, 1f, 0.4f);
                                    u.damage(rawDamage * scaled);
                                });

                                if(collidesGround){
                                    Units.nearbyBuildings(x, y, radius, build -> {
                                        if(primary == build || build == null || !build.isValid()) return;
                                        if(!build.within(x, y, radius + (scaledSplashDamage ? build.hitSize() / 2f : 0f))) return;

                                        float dist = scaledSplashDamage ? Math.max(0f, Mathf.dst(build.x, build.y, x, y) - build.hitSize() / 2f) : Mathf.dst(build.x, build.y, x, y);
                                        float scaled = radius <= 0.00001f ? 1f : Mathf.lerp(1f - dist / radius, 1f, 0.4f);
                                        build.damage(rawDamage * scaled * buildingDamageMultiplier);
                                    });
                                }

                                if(status != StatusEffects.none){
                                    Units.nearby(x - radius, y - radius, area, area, u -> {
                                        if(primary == u) return;
                                        if(!u.checkTarget(collidesAir, collidesGround) || !u.hittable()) return;
                                        if(!u.within(x, y, radius + u.hitSize / 2f)) return;
                                        u.apply(status, statusDuration);
                                    });
                                }
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
                          getPreceptSiegeData(unit).siegeMode = true;
                      }
                  }

                  if(preceptIsUnsieging(unit)){
                      unit.vel.setZero();
                      if(unit.controller() instanceof CommandAI ai){
                          ai.clearCommands();
                      }
                      if(unit.getDuration(StatusEffects.preceptUnsieging) <= 0.001f){
                          unit.unapply(StatusEffects.preceptUnsieging);
                          getPreceptSiegeData(unit).siegeMode = false;
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
                    Draw.rect(outlineRegion, unit.x, unit.y,
                        outlineRegion.width * outlineRegion.scale * scaledTankVisualScale / 4f * bodyScaleX,
                        outlineRegion.height * outlineRegion.scale * scaledTankVisualScale / 4f * bodyScaleY,
                        unit.rotation - 90f);
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

                Draw.rect(region, unit.x, unit.y,
                    region.width * region.scale * scaledTankVisualScale / 4f * bodyScaleX,
                    region.height * region.scale * scaledTankVisualScale / 4f * bodyScaleY,
                    unit.rotation - 90f);
                Draw.reset();
            }

            @Override
            public void drawCell(Unit unit){
                applyColor(unit);
                Draw.color(cellColor(unit));
                Draw.rect(cellRegion, unit.x, unit.y,
                    cellRegion.width * cellRegion.scale * scaledTankVisualScale / 4f * bodyScaleX,
                    cellRegion.height * cellRegion.scale * scaledTankVisualScale / 4f * bodyScaleY,
                    unit.rotation - 90f);
                Draw.reset();
            }

            @Override
            public <T extends Unit & Tankc> void drawTank(T unit){
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
            float weaponRegionScale = 0.8f;

            @Override
            public void load(){
                float prevRatio = spriteHitSizeRatio;
                spriteHitSizeRatio = -1f;
                super.load();
                spriteHitSizeRatio = prevRatio;

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
                treadRegion = Core.atlas.find("blank");
                legBaseRegion = Core.atlas.find(copy + "-leg-base", copy + "-leg");
                baseRegion = Core.atlas.find(copy + "-base");
                cellRegion = Core.atlas.find(copy + "-cell", Core.atlas.find("power-cell"));
                outlineRegion = Core.atlas.find(copy + "-outline");
                wreckRegions = new TextureRegion[3];
                for(int i = 0; i < wreckRegions.length; i++){
                    wreckRegions[i] = Core.atlas.find(copy + "-wreck" + i);
                }
                clipSize = Math.max(region.width * 2f, clipSize);
                hitSize = 0.975f * tilesize;
                applySpriteHitSizeRatio();
                if(!Mathf.equal(weaponRegionScale, 1f, 0.0001f)){
                    for(var weapon : weapons){
                        weapon.region = copyScaledRegion(weapon.region, weaponRegionScale);
                        weapon.outlineRegion = copyScaledRegion(weapon.outlineRegion, weaponRegionScale);
                        weapon.heatRegion = copyScaledRegion(weapon.heatRegion, weaponRegionScale);
                        weapon.cellRegion = copyScaledRegion(weapon.cellRegion, weaponRegionScale);
                        if(weapon.shadow > 0f) weapon.shadow *= weaponRegionScale;
                    }
                }
            }

            {
                visualHitSizeScale = scaledTankVisualScale;
                fullOverride = "precept";
                hitSizeFromRegion = false;
                spriteHitSizeRatio = 1.1f * 1.3f * 1.2f * 2f * 1.1f * 1.2f * 0.5f;
                hitSize = 0.4875f * tilesize;
                fogRadius = 11f;
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
                        boolean prevShoot = mount.shoot;
                        boolean prevRotate = mount.rotate;

                        Teamc target = hurricaneTarget(unit);
                        boolean locked = target != null;

                        float dynamicRange = locked ? hurricaneLockRange() : hurricaneBaseRange();
                        float prevRange = bullet.range;
                        float prevOverride = bullet.rangeOverride;
                        bullet.range = dynamicRange;
                        bullet.rangeOverride = dynamicRange;

                        boolean allowShoot = true;
                        if(unit.controller() instanceof CommandAI cmd){
                            if(cmd.hasStance(UnitStance.holdFire)){
                                allowShoot = false;
                            }else if(cmd.moveOnlyCommandActive() && !UnitTypes.allowFireWhileMoving(unit)){
                                //permit firing while moving only when lock is active
                                allowShoot = locked;
                            }
                        }else if(unit.controller() instanceof AIController ai && !ai.shouldFire()){
                            allowShoot = false;
                        }

                        if(!locked){
                            Teamc current = mount.target;
                            if(current == null || Units.invalidateTarget(current, unit, hurricaneBaseRange())){
                                current = hurricaneFindTarget(unit);
                            }
                            target = current;
                        }

                        if(target != null){
                            mount.target = target;
                            mount.aimX = target.getX();
                            mount.aimY = target.getY();
                        }

                        if(allowShoot){
                            if(locked || target != null){
                                mount.shoot = true;
                                mount.rotate = true;
                            }else{
                                mount.shoot = prevShoot;
                                mount.rotate = prevRotate;
                            }
                        }else{
                            mount.shoot = false;
                            mount.rotate = false;
                        }

                        boolean previous = unit.type.alwaysShootWhenMoving;
                        if(locked){
                            unit.type.alwaysShootWhenMoving = true;
                        }

                        super.update(unit, mount);

                        bullet.range = prevRange;
                        bullet.rangeOverride = prevOverride;
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
                        Teamc lockedTarget = hurricaneTarget(unit);
                        Teamc trackedTarget = mount.target != null ? mount.target : (lockedTarget != null ? lockedTarget : hurricaneFindTarget(unit));
                        if(trackedTarget == null){
                            float ax = mount.aimX, ay = mount.aimY;
                            if(!Float.isNaN(ax) && !Float.isNaN(ay)){
                                trackedTarget = Units.closestTarget(unit.team, ax, ay, 24f,
                                    u -> u.checkTarget(true, true),
                                    b -> Units.canTargetBuilding(true, true, b));
                            }
                        }
                        boolean lockedShot = lockedTarget != null;
                        bullet.data = new HurricaneMissileData(trackedTarget, lockedShot);
                        bullet.damage = hurricaneMissileDamage(bullet.team, lockedShot) * bullet.damageMultiplier();
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
                                ballisticTracking = false;
                                rangeOverride = hurricaneBaseRange();
                                width = 3f;
                                height = 5f;
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

                                collides = true;
                                collidesTiles = true;
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
                                Teamc target = null;
                                if(b.data instanceof HurricaneMissileData d){
                                    target = d.target;
                                }else if(b.data instanceof Teamc t){
                                    target = t;
                                }

                                if(!canTrackFriendlyOnlyWhenForced(b, target)) target = null;

                                float tx, ty;
                                if(target != null){
                                    tx = target.getX();
                                    ty = target.getY();
                                    b.aimX = tx;
                                    b.aimY = ty;
                                }else{
                                    tx = b.aimX;
                                    ty = b.aimY;
                                    if(Float.isNaN(tx) || Float.isNaN(ty) || (tx == -1f && ty == -1f)){
                                        tx = b.x + Angles.trnsx(b.rotation(), 8f);
                                        ty = b.y + Angles.trnsy(b.rotation(), 8f);
                                        b.aimX = tx;
                                        b.aimY = ty;
                                    }
                                }
                                b.vel.setAngle(Angles.moveToward(b.rotation(), b.angleTo(tx, ty), 35f * Time.delta));
                                b.vel.setLength(speed);
                                b.rotation(b.vel.angle());
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
                hitSizeFromRegion = false;
                spriteHitSizeRatio = 1f;
                hitSize = 1.875f * tilesize;
                engineSize = 3f;
                engineOffset = 38f / 4f;
                fogRadius = 11f;
                itemCapacity = 0;
                canAttack = false;
                targetAir = false;
                targetGround = false;
                followEnemyWhenUnarmed = true;
                energyCapacity = 200f;
                energyInit = 50f;
                stealthDetectionRange = 12f * tilesize;

                engines.clear();
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
            speed = 1.31f;
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
        public void drawShadow(Unit unit){
            //Lifted buildings already render their payload body directly; the carrier should stay visually invisible.
        }

        @Override
        public void load(){
            super.load();
            region = Core.atlas.find("core-nucleus");
            uiIcon = Core.atlas.find("core-nucleus");
            fullIcon = Core.atlas.find("core-nucleus");
            updateHitSizeFromRegion();
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

        warpProbe = new UnitType("warp-probe"){{
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
            fogRadius = battlecruiserWarpVisionRadius;
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

        markSc2DatabaseUnits();

        //All energy units regenerate at a unified rate.
        for(UnitType type : content.units()){
            if(type != null && type.energyCapacity > 0f){
                type.energyRegen = 0.8f;
            }
        }

        //endregion
    }
}





