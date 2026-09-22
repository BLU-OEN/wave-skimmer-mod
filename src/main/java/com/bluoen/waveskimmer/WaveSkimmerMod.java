package com.bluoen.waveskimmer;

import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(WaveSkimmerMod.MOD_ID)
public class WaveSkimmerMod {
    public static final String MOD_ID = "waveskimmer";

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MOD_ID);

    public static final DeferredBlock<WaveSkimmerBlock> WAVE_SKIMMER = BLOCKS.register("wave_skimmer",
            () -> new WaveSkimmerBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(3.5F, 6.0F)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()));

    public static final DeferredItem<WaveSkimmerItem> WAVE_SKIMMER_ITEM = ITEMS.register("wave_skimmer",
            () -> new WaveSkimmerItem(WAVE_SKIMMER.get(), new Item.Properties()));

    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WaveSkimmerBlockEntity>> WAVE_SKIMMER_BE =
            BLOCK_ENTITIES.register("wave_skimmer",
                    () -> BlockEntityType.Builder.of(WaveSkimmerBlockEntity::new, WAVE_SKIMMER.get()).build(null));

    public WaveSkimmerMod(IEventBus modBus, ModContainer container) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        modBus.addListener(this::addToCreativeTabs);
        modBus.addListener(this::registerCapabilities);
        container.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void addToCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.REDSTONE_BLOCKS
                || event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(WAVE_SKIMMER_ITEM);
        }
    }

    /** Exposes FE on every face except the hull underneath, so cables can hook into the side or engine ports. */
    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, WAVE_SKIMMER_BE.get(),
                (skimmer, side) -> side == Direction.DOWN ? null : skimmer.getEnergyStorage());
    }
}
