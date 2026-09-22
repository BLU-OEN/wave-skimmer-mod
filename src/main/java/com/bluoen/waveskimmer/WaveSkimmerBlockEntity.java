package com.bluoen.waveskimmer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

public class WaveSkimmerBlockEntity extends BlockEntity {
    private static final int SCAN_RADIUS = 2;
    private static final int SCAN_AREA = (SCAN_RADIUS * 2 + 1) * (SCAN_RADIUS * 2 + 1);

    private final GeneratorEnergyStorage energy =
            new GeneratorEnergyStorage(Config.CAPACITY.get(), Config.MAX_OUTPUT.get());

    private int currentGeneration;
    private int openWater = -1; // water sources in the 5x5 patch below; -1 = not scanned yet
    private boolean inOcean;

    public WaveSkimmerBlockEntity(BlockPos pos, BlockState state) {
        super(WaveSkimmerMod.WAVE_SKIMMER_BE.get(), pos, state);
    }

    /** Per-block phase offset so neighbouring skimmers don't move in lockstep. */
    public static double wavePhase(BlockPos pos) {
        return pos.getX() * 0.35 + pos.getZ() * 0.2;
    }

    /** Swell height in [-1, 1]: a long rolling wave plus a shorter chop. */
    public static double swell(double time, BlockPos pos) {
        double phase = wavePhase(pos);
        return Math.sin(time / 30.0 + phase) * 0.6 + Math.sin(time / 11.0 + phase * 1.7) * 0.4;
    }

    /** How hard the current wave is driving the skimmer, in [0.5, 1]. */
    public static double waveStrength(double time, BlockPos pos) {
        return 0.75 + 0.25 * swell(time, pos);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, WaveSkimmerBlockEntity be) {
        if (be.openWater < 0 || level.getGameTime() % 100 == 0) {
            be.scanSurroundings(level, pos);
        }

        be.currentGeneration = be.computeGeneration(level, pos);
        int before = be.energy.getEnergyStored();
        be.energy.generate(be.currentGeneration);
        be.pushEnergy(level, pos);
        if (be.energy.getEnergyStored() != before) {
            be.setChanged();
        }
    }

    private void scanSurroundings(Level level, BlockPos pos) {
        int count = 0;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -SCAN_RADIUS; dx <= SCAN_RADIUS; dx++) {
            for (int dz = -SCAN_RADIUS; dz <= SCAN_RADIUS; dz++) {
                cursor.set(pos.getX() + dx, pos.getY() - 1, pos.getZ() + dz);
                FluidState fluid = level.getFluidState(cursor);
                if (fluid.is(FluidTags.WATER) && fluid.isSource()) {
                    count++;
                }
            }
        }
        openWater = count;
        inOcean = level.getBiome(pos).is(BiomeTags.IS_OCEAN);
    }

    private int computeGeneration(Level level, BlockPos pos) {
        double output = Config.BASE_GENERATION.get()
                * waveStrength(level.getGameTime(), pos)
                * ((double) openWater / SCAN_AREA);
        if (inOcean) {
            output *= Config.OCEAN_MULTIPLIER.get();
        }
        if (level.isRainingAt(pos.above())) {
            output *= level.isThundering() ? Config.THUNDER_MULTIPLIER.get() : Config.RAIN_MULTIPLIER.get();
        }
        return (int) Math.round(output);
    }

    /** Pushes power out through every face except the hull underneath. */
    private void pushEnergy(Level level, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            if (dir == Direction.DOWN || energy.getEnergyStored() <= 0) {
                continue;
            }
            IEnergyStorage target = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos.relative(dir), dir.getOpposite());
            if (target != null && target.canReceive()) {
                int offered = energy.extractEnergy(Config.MAX_OUTPUT.get(), true);
                int accepted = target.receiveEnergy(offered, false);
                energy.extractEnergy(accepted, false);
            }
        }
    }

    /** Bubbles pulled in under the nose, churned by the turbine and flushed out the tail as a wake. */
    public static void clientTick(Level level, BlockPos pos, BlockState state, WaveSkimmerBlockEntity be) {
        if (!level.getFluidState(pos.below()).is(FluidTags.WATER)) {
            return;
        }
        RandomSource random = level.random;
        Direction facing = state.getValue(WaveSkimmerBlock.FACING);
        double fx = facing.getStepX();
        double fz = facing.getStepZ();
        double cx = pos.getX() + 0.5;
        double cz = pos.getZ() + 0.5;
        double y = pos.getY();
        double strength = waveStrength(level.getGameTime(), pos);

        // Water flowing under the hull, front to back
        for (int i = 0; i < 2; i++) {
            if (random.nextDouble() < strength) {
                double along = random.nextDouble() - 0.5;
                double across = (random.nextDouble() - 0.5) * 0.8;
                level.addParticle(ParticleTypes.BUBBLE,
                        cx + fx * along - fz * across, y - 0.25 - random.nextDouble() * 0.4, cz + fz * along + fx * across,
                        -fx * 0.4, 0.05, -fz * 0.4);
            }
        }

        // Turbine churn rising from beneath the centre
        if (random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.BUBBLE_COLUMN_UP,
                    cx + (random.nextDouble() - 0.5) * 0.4, y - 0.9, cz + (random.nextDouble() - 0.5) * 0.4,
                    0, 0.04, 0);
        }

        // Froth popping at the waterline
        if (random.nextInt(4) == 0) {
            level.addParticle(ParticleTypes.BUBBLE_POP,
                    cx + (random.nextDouble() - 0.5), y - 0.1, cz + (random.nextDouble() - 0.5), 0, 0.02, 0);
        }

        // Wake spraying out of the tail outlet
        double tailX = cx - fx * 0.6;
        double tailZ = cz - fz * 0.6;
        if (random.nextDouble() < strength * 0.4) {
            double across = (random.nextDouble() - 0.5) * 0.4;
            level.addParticle(ParticleTypes.SPLASH, tailX - fz * across, y + 0.05, tailZ + fx * across,
                    -fx * 0.15, 0.1, -fz * 0.15);
        }
        if (random.nextInt(5) == 0) {
            level.addParticle(ParticleTypes.FISHING, tailX, y - 0.05, tailZ, -fx * 0.05, 0, -fz * 0.05);
        }

        if (random.nextInt(80) == 0) {
            level.playLocalSound(pos, SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT, SoundSource.BLOCKS,
                    0.25F, 0.9F + random.nextFloat() * 0.2F, false);
        }
    }

    public IEnergyStorage getEnergyStorage() {
        return energy;
    }

    public int getCurrentGeneration() {
        return currentGeneration;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Energy", energy.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Energy")) {
            energy.deserializeNBT(registries, tag.get("Energy"));
        }
    }
}
