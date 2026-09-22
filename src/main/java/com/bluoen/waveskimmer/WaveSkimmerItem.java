package com.bluoen.waveskimmer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PlaceOnWaterBlockItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.List;

/** Placed like a lily pad: aim at a water source and it floats on the surface above it. */
public class WaveSkimmerItem extends PlaceOnWaterBlockItem {
    public WaveSkimmerItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.waveskimmer.wave_skimmer.place").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.waveskimmer.wave_skimmer.output").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.waveskimmer.wave_skimmer.bonus").withStyle(ChatFormatting.DARK_GRAY));
    }
}
