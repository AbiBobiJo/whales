package studio.interpretation.whales.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.AABB;

import studio.interpretation.whales.WhalesMod;

/**
 * Ракушечный шип — редстоун-ловушка.
 * Без сигнала шип спрятан и блок безопасен. Получив сигнал (нажимная плита,
 * рычаг, растяжка) шип выдвигается, ранит всех, кто стоит сверху, и продолжает
 * колоть, пока сигнал не пропадёт.
 */
public class BarnacleSpikeBlock extends Block {
	public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

	public static final ResourceKey<DamageType> SPIKE_DAMAGE =
			ResourceKey.create(Registries.DAMAGE_TYPE, WhalesMod.id("barnacle_spike"));

	private static final float EXTEND_DAMAGE = 6.0F;
	private static final float STAND_DAMAGE = 3.0F;

	public BarnacleSpikeBlock(Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any().setValue(POWERED, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(POWERED);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return this.defaultBlockState().setValue(POWERED, context.getLevel().hasNeighborSignal(context.getClickedPos()));
	}

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, Orientation orientation, boolean movedByPiston) {
		if (!(level instanceof ServerLevel serverLevel)) {
			return;
		}
		boolean powered = level.hasNeighborSignal(pos);
		if (powered == state.getValue(POWERED)) {
			return;
		}
		level.setBlock(pos, state.setValue(POWERED, powered), Block.UPDATE_ALL);

		if (powered) {
			level.playSound(null, pos, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 0.6F, 1.6F);
			serverLevel.sendParticles(ParticleTypes.CRIT, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5,
					8, 0.25, 0.2, 0.25, 0.1);
			AABB area = new AABB(pos.above());
			for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, area)) {
				victim.hurtServer(serverLevel, spikeDamage(level), EXTEND_DAMAGE);
				victim.setDeltaMovement(victim.getDeltaMovement().add(0.0, 0.35, 0.0));
			}
		} else {
			level.playSound(null, pos, SoundEvents.PISTON_CONTRACT, SoundSource.BLOCKS, 0.5F, 1.6F);
		}
	}

	@Override
	public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
		if (state.getValue(POWERED) && entity instanceof LivingEntity && level instanceof ServerLevel serverLevel) {
			entity.hurtServer(serverLevel, spikeDamage(level), STAND_DAMAGE);
		}
		super.stepOn(level, pos, state, entity);
	}

	private static DamageSource spikeDamage(Level level) {
		return new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(SPIKE_DAMAGE));
	}
}
