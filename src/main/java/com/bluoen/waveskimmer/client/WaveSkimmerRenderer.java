package com.bluoen.waveskimmer.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.bluoen.waveskimmer.WaveSkimmerBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;

/** Renders the skimmer model gently bobbing, pitching and rolling with the swell. */
public class WaveSkimmerRenderer implements BlockEntityRenderer<WaveSkimmerBlockEntity> {
    private final BlockRenderDispatcher blockRenderer;

    public WaveSkimmerRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderer = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(WaveSkimmerBlockEntity skimmer, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int packedLight, int packedOverlay) {
        Level level = skimmer.getLevel();
        if (level == null) {
            return;
        }
        BlockState state = skimmer.getBlockState();
        BlockPos pos = skimmer.getBlockPos();
        double time = level.getGameTime() + partialTick;
        double phase = WaveSkimmerBlockEntity.wavePhase(pos);

        float bob = (float) (WaveSkimmerBlockEntity.swell(time, pos) * 0.035);
        float pitch = (float) (Math.sin(time / 30.0 + phase + 1.2) * 3.0);
        float roll = (float) (Math.cos(time / 23.0 + phase) * 2.0);

        poseStack.pushPose();
        poseStack.translate(0.5, bob, 0.5);
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
        poseStack.mulPose(Axis.ZP.rotationDegrees(roll));
        poseStack.translate(-0.5, 0, -0.5);

        BakedModel model = blockRenderer.getBlockModel(state);
        RenderType renderType = RenderType.cutout();
        blockRenderer.getModelRenderer().renderModel(poseStack.last(), buffers.getBuffer(renderType), state, model,
                1.0F, 1.0F, 1.0F, packedLight, packedOverlay, ModelData.EMPTY, renderType);
        poseStack.popPose();
    }
}
