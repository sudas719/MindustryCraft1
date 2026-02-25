package mindustry.world.blocks.defense;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.struct.*;
import arc.util.io.*;
import mindustry.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.*;
import mindustry.world.meta.*;
import mindustry.world.blocks.storage.CoreBlock.*;

import static mindustry.Vars.*;

public class Radar extends Block{
    public static final int configRecycle = 1;

    public float discoveryTime = 60f * 10f;
    public float rotateSpeed = 2f;
    public float recycleTime = 4f * 60f;
    public int recycleCrystalRefund = 75;
    public int recycleGasRefund = 38;

    public @Load("@-base") TextureRegion baseRegion;
    public @Load("@-glow") TextureRegion glowRegion;

    public Color glowColor = Pal.turretHeat;
    public float glowScl = 5f, glowMag = 0.6f;

    public Radar(String name){
        super(name);

        update = solid = true;
        commandable = true;
        configurable = true;
        flags = EnumSet.of(BlockFlag.hasFogRadius);
        outlineIcon = true;
        fogRadius = 10;

        config(Integer.class, (RadarBuild build, Integer value) -> {
            if(value == null) return;
            if(value == configRecycle){
                build.startRecycle();
            }
        });
    }

    @Override
    public TextureRegion[] icons(){
        return new TextureRegion[]{baseRegion, region};
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        super.drawPlace(x, y, rotation, valid);

        Drawf.dashCircle(x * tilesize + offset, y * tilesize + offset, fogRadius * tilesize, Pal.accent);
    }

    public class RadarBuild extends Building{
        public float progress;
        public float lastRadius = 0f;
        public float smoothEfficiency = 1f;
        public float totalProgress;
        public boolean recycling = false;
        public float recycleProgress = 0f;

        @Override
        public byte version(){
            return 1;
        }

        @Override
        public float fogRadius(){
            return fogRadius * progress * smoothEfficiency;
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

            smoothEfficiency = Mathf.lerpDelta(smoothEfficiency, efficiency, 0.05f);

            if(Math.abs(fogRadius() - lastRadius) >= 0.5f){
                Vars.fogControl.forceUpdate(team, this);
                lastRadius = fogRadius();
            }

            progress += edelta() / discoveryTime;
            progress = Mathf.clamp(progress);

            totalProgress += efficiency * edelta();
        }

        public boolean isRecycling(){
            return recycling;
        }

        public float recycleRemainingFraction(){
            if(!recycling || recycleTime <= 0f) return 0f;
            return Mathf.clamp(1f - recycleProgress / recycleTime);
        }

        public void startRecycle(){
            if(recycling) return;
            recycling = true;
            recycleProgress = 0f;
        }

        private void finishRecycle(){
            if(net.client()) return;
            if(!isValid()) return;

            CoreBuild core = team.core();
            if(core != null){
                if(recycleCrystalRefund > 0){
                    core.items.add(Items.graphite, recycleCrystalRefund);
                }
                if(recycleGasRefund > 0){
                    core.items.add(Items.highEnergyGas, recycleGasRefund);
                }
            }

            Fx.blockExplosionSmoke.at(x, y);
            Fx.dynamicExplosion.at(x, y, block.size);
            kill();
        }

        @Override
        public boolean canPickup(){
            return false;
        }

        @Override
        public void drawSelect(){
            Drawf.dashCircle(x, y, fogRadius() * tilesize, Pal.accent);
        }

        @Override
        public void draw(){
            Draw.rect(baseRegion, x, y);
            Draw.rect(region, x, y, rotateSpeed * totalProgress);

            Drawf.additive(glowRegion, glowColor, glowColor.a * (1f - glowMag + Mathf.absin(glowScl, glowMag)), x, y, rotateSpeed * totalProgress, Layer.blockAdditive);
        }

        @Override
        public float progress(){
            return progress;
        }

        @Override
        public void write(Writes write){
            super.write(write);

            write.f(progress);
            write.bool(recycling);
            write.f(recycleProgress);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);

            progress = read.f();
            if(revision >= 1){
                recycling = read.bool();
                recycleProgress = read.f();
            }else{
                recycling = false;
                recycleProgress = 0f;
            }
        }
    }
}
