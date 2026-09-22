package com.bluoen.waveskimmer.client;

import com.bluoen.waveskimmer.WaveSkimmerMod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = WaveSkimmerMod.MOD_ID, value = Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() {}

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(WaveSkimmerMod.WAVE_SKIMMER_BE.get(), WaveSkimmerRenderer::new);
    }
}
