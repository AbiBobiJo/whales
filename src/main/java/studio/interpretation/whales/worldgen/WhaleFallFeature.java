package studio.interpretation.whales.worldgen;

import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.storage.loot.LootTable;

import studio.interpretation.whales.WhalesMod;

/**
 * «Китовый скелет» (whale fall) — кости кита на дне океана:
 * череп с тайником, позвоночник, арки рёбер, хвост и плавник.
 *
 * Строится в локальных координатах: u — вдоль тела, v — поперёк, y — вверх.
 */
public class WhaleFallFeature extends Feature<NoneFeatureConfiguration> {
	public static final ResourceKey<LootTable> SKULL_LOOT =
			ResourceKey.create(Registries.LOOT_TABLE, WhalesMod.id("chests/whale_fall"));

	/** Высота арок рёбер по порядку от головы к хвосту. */
	private static final int[] RIB_HEIGHTS = {4, 4, 4, 3, 3, 2};

	public WhaleFallFeature(Codec<NoneFeatureConfiguration> codec) {
		super(codec);
	}

	@Override
	public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
		WorldGenLevel level = context.level();
		RandomSource random = context.random();
		BlockPos origin = context.origin();

		// Нужна вода минимум на 6 блоков над дном, иначе скелет будет торчать наружу
		for (int dy = 0; dy < 6; dy++) {
			if (!level.getFluidState(origin.above(dy)).is(FluidTags.WATER)) {
				return false;
			}
		}
		if (!level.getBlockState(origin.below()).isSolid()) {
			return false;
		}

		Builder b = new Builder(level, random, origin, random.nextBoolean(), random.nextBoolean());

		// Скелет немного утоплен в грунт
		int base = -1;

		// ---- череп: u = -7..-1, v = -3..3 ----
		for (int u = -7; u <= -1; u++) {
			for (int v = -3; v <= 3; v++) {
				for (int y = 0; y <= 3; y++) {
					boolean shell = u == -7 || u == -1 || Math.abs(v) == 3 || y == 0 || y == 3;
					if (!shell) {
						continue;
					}
					// глазницы по бокам и пролом сверху, как на эскизе
					boolean eye = Math.abs(v) == 3 && y == 2 && (u == -5 || u == -4);
					boolean topHole = y == 3 && Math.abs(v) <= 1 && (u == -4 || u == -3);
					boolean mouth = u == -7 && y == 1 && Math.abs(v) <= 1;
					if (eye || topHole || mouth) {
						continue;
					}
					b.bone(u, base + y, v, Direction.Axis.Y, 0.08F);
				}
			}
		}
		// нижняя челюсть уходит вперёд
		for (int u = -10; u <= -8; u++) {
			b.bone(u, base + 1, -2, Direction.Axis.Y, 0.15F);
			b.bone(u, base + 1, 2, Direction.Axis.Y, 0.15F);
		}
		// тайник внутри черепа
		b.chest(-4, base + 1, 0);

		// ---- позвоночник ----
		int spineEnd = 18;
		for (int u = 0; u <= spineEnd; u++) {
			b.bone(u, base + 1, 0, Direction.Axis.X, u > 14 ? 0.15F : 0.03F);
		}

		// ---- рёбра: арки ⊓, уменьшающиеся к хвосту ----
		for (int i = 0; i < RIB_HEIGHTS.length; i++) {
			int u = 1 + i * 2;
			int h = RIB_HEIGHTS[i];
			int half = h >= 4 ? 3 : 2;
			// сломанное ребро иногда отсутствует целиком
			if (random.nextFloat() < 0.12F) {
				continue;
			}
			for (int y = 0; y <= h; y++) {
				b.bone(u, base + y, -half, Direction.Axis.Y, 0.1F);
				b.bone(u, base + y, half, Direction.Axis.Y, 0.1F);
			}
			for (int v = -half + 1; v <= half - 1; v++) {
				b.bone(u, base + h, v, Direction.Axis.Z, 0.1F);
			}
		}

		// ---- хвостовой плавник ----
		int fin = spineEnd + 1;
		for (int v = -3; v <= 3; v++) {
			if (v == 0) {
				b.bone(fin, base + 1, v, Direction.Axis.X, 0.0F);
			} else {
				b.bone(fin + Math.abs(v) / 2, base + 1, v, Direction.Axis.Z, 0.25F);
			}
		}

		// ---- осколки костей и водоросли вокруг ----
		for (int i = 0; i < 6; i++) {
			int u = -8 + random.nextInt(28);
			int v = -5 + random.nextInt(11);
			if (Math.abs(v) > 3) {
				b.bone(u, base + 1, v, random.nextBoolean() ? Direction.Axis.X : Direction.Axis.Z, 0.0F);
			}
		}
		for (int i = 0; i < 14; i++) {
			b.seagrass(-9 + random.nextInt(32), -6 + random.nextInt(13));
		}

		return true;
	}

	/** Перевод локальных координат в мир с поворотом и отражением. */
	private static final class Builder {
		private final WorldGenLevel level;
		private final RandomSource random;
		private final BlockPos origin;
		private final boolean alongZ;
		private final boolean flip;

		Builder(WorldGenLevel level, RandomSource random, BlockPos origin, boolean alongZ, boolean flip) {
			this.level = level;
			this.random = random;
			this.origin = origin;
			this.alongZ = alongZ;
			this.flip = flip;
		}

		BlockPos pos(int u, int y, int v) {
			int du = this.flip ? -u : u;
			return this.alongZ ? this.origin.offset(v, y, du) : this.origin.offset(du, y, v);
		}

		/** Ось кости в локальных координатах: X — вдоль тела, Z — поперёк. */
		Direction.Axis worldAxis(Direction.Axis local) {
			if (local == Direction.Axis.Y || !this.alongZ) {
				return local;
			}
			return local == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
		}

		void bone(int u, int y, int v, Direction.Axis axis, float decay) {
			if (decay > 0 && this.random.nextFloat() < decay) {
				return;
			}
			BlockPos p = this.pos(u, y, v);
			if (!canReplace(this.level.getBlockState(p))) {
				return;
			}
			this.level.setBlock(p, Blocks.BONE_BLOCK.defaultBlockState().setValue(RotatedPillarBlock.AXIS, this.worldAxis(axis)), 2);
		}

		void chest(int u, int y, int v) {
			BlockPos p = this.pos(u, y, v);
			BlockState chest = Blocks.CHEST.defaultBlockState().setValue(ChestBlock.WATERLOGGED, true);
			this.level.setBlock(p, chest, 2);
			if (this.level.getBlockEntity(p) instanceof RandomizableContainerBlockEntity container) {
				container.setLootTable(SKULL_LOOT, this.random.nextLong());
			}
		}

		void seagrass(int u, int v) {
			BlockPos p = this.pos(u, 0, v);
			// подстраиваемся под рельеф: ищем дно в пределах пары блоков
			for (int dy = 2; dy >= -2; dy--) {
				BlockPos at = p.above(dy);
				if (this.level.getBlockState(at).is(Blocks.WATER) && this.level.getBlockState(at.below()).isSolid()) {
					this.level.setBlock(at, Blocks.SEAGRASS.defaultBlockState(), 2);
					return;
				}
			}
		}

		private static boolean canReplace(BlockState state) {
			return state.isAir() || state.is(Blocks.WATER) || state.is(Blocks.SEAGRASS)
					|| state.is(Blocks.TALL_SEAGRASS) || state.is(Blocks.KELP) || state.is(Blocks.KELP_PLANT)
					|| state.is(Blocks.SAND) || state.is(Blocks.GRAVEL) || state.is(Blocks.CLAY);
		}
	}
}
