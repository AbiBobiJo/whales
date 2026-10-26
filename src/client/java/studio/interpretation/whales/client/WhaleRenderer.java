package studio.interpretation.whales.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

import studio.interpretation.whales.WhalesMod;
import studio.interpretation.whales.entity.WhaleEntity;

public class WhaleRenderer extends MobRenderer<WhaleEntity, WhaleRenderState, WhaleModel> {
	private static final Identifier TEXTURE = WhalesMod.id("textures/entity/whale.png");

	public WhaleRenderer(EntityRendererProvider.Context context) {
		super(context, new WhaleModel(context.bakeLayer(WhalesClient.WHALE_LAYER)), 1.6F);
	}

	@Override
	public WhaleRenderState createRenderState() {
		return new WhaleRenderState();
	}

	@Override
	public void extractRenderState(WhaleEntity entity, WhaleRenderState state, float partialTick) {
		super.extractRenderState(entity, state, partialTick);
		state.barnacles = entity.getBarnacles();
		state.inWater = entity.isInWater();
	}

	@Override
	public Identifier getTextureLocation(WhaleRenderState state) {
		return TEXTURE;
	}
}
