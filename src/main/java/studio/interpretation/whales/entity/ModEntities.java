package studio.interpretation.whales.entity;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.level.levelgen.Heightmap;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityType;

import studio.interpretation.whales.WhalesMod;

public class ModEntities {
	public static final ResourceKey<EntityType<?>> WHALE_KEY =
			ResourceKey.create(Registries.ENTITY_TYPE, WhalesMod.id("whale"));

	public static final EntityType<WhaleEntity> WHALE = Registry.register(
			BuiltInRegistries.ENTITY_TYPE,
			WHALE_KEY,
			FabricEntityType.Builder.createMob(WhaleEntity::new, MobCategory.WATER_CREATURE, mob -> mob
					.defaultAttributes(WhaleEntity::createAttributes)
					.spawnRestriction(
							SpawnPlacementTypes.IN_WATER,
							Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
							(type, level, reason, pos, random) ->
									level.getFluidState(pos).is(FluidTags.WATER)
											&& level.getFluidState(pos.below()).is(FluidTags.WATER)
											&& level.getFluidState(pos.above()).is(FluidTags.WATER)
					))
					.sized(2.6f, 1.4f)
					.eyeHeight(0.9f)
					.clientTrackingRange(10)
					.build(WHALE_KEY)
	);

	public static void init() {
	}
}
