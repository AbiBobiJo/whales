package studio.interpretation.whales;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;

import studio.interpretation.whales.block.ModBlocks;
import studio.interpretation.whales.entity.ModEntities;
import studio.interpretation.whales.item.ModItems;
import studio.interpretation.whales.worldgen.WhaleFallFeature;

public class WhalesMod implements ModInitializer {
	public static final String MOD_ID = "whales";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final WhaleFallFeature WHALE_FALL = new WhaleFallFeature(NoneFeatureConfiguration.CODEC);

	public static final ResourceKey<PlacedFeature> WHALE_FALL_PLACED =
			ResourceKey.create(Registries.PLACED_FEATURE, id("whale_fall"));

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		ModEntities.init();
		ModBlocks.init();
		ModItems.init();

		Registry.register(BuiltInRegistries.FEATURE, id("whale_fall"), WHALE_FALL);

		// Скелеты китов лежат на дне любых океанов
		BiomeModifications.addFeature(
				BiomeSelectors.tag(BiomeTags.IS_OCEAN),
				GenerationStep.Decoration.LOCAL_MODIFICATIONS,
				WHALE_FALL_PLACED
		);

		// Живые киты плавают в глубоких океанах
		BiomeModifications.addSpawn(
				BiomeSelectors.tag(BiomeTags.IS_DEEP_OCEAN),
				MobCategory.WATER_CREATURE,
				ModEntities.WHALE,
				3, 1, 2
		);

		LOGGER.info("Whales loaded");
	}
}
