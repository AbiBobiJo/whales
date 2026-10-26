package studio.interpretation.whales.client;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRenderers;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;

import studio.interpretation.whales.WhalesMod;
import studio.interpretation.whales.entity.ModEntities;

public class WhalesClient implements ClientModInitializer {
	public static final ModelLayerLocation WHALE_LAYER = new ModelLayerLocation(WhalesMod.id("whale"), "main");

	@Override
	public void onInitializeClient() {
		EntityModelLayerRegistry.registerModelLayer(WHALE_LAYER, WhaleModel::createBodyLayer);
		EntityRenderers.register(ModEntities.WHALE, WhaleRenderer::new);
	}
}
