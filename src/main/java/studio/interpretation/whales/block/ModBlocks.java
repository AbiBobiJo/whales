package studio.interpretation.whales.block;

import java.util.function.Function;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;

import studio.interpretation.whales.WhalesMod;

public class ModBlocks {
	public static final Block BARNACLE_SPIKE = register(
			"barnacle_spike",
			BarnacleSpikeBlock::new,
			BlockBehaviour.Properties.of()
					.mapColor(MapColor.STONE)
					.strength(2.0F, 6.0F)
					.requiresCorrectToolForDrops()
					.sound(SoundType.BONE_BLOCK)
	);

	private static Block register(String name, Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties properties) {
		ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, WhalesMod.id(name));
		Block block = factory.apply(properties.setId(blockKey));

		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, WhalesMod.id(name));
		Registry.register(BuiltInRegistries.ITEM, itemKey,
				new BlockItem(block, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()));

		return Registry.register(BuiltInRegistries.BLOCK, blockKey, block);
	}

	public static void init() {
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.REDSTONE_BLOCKS).register(entries -> entries.accept(BARNACLE_SPIKE));
	}
}
