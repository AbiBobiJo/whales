package studio.interpretation.whales.entity;

import java.util.List;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Shearable;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.SmoothSwimmingLookControl;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.TryFindWaterGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

import studio.interpretation.whales.item.ModItems;

/**
 * Кит. Обрастает ракушками; если их срезать ножницами, кит в благодарность
 * выпрыгивает из воды и разбрасывает вокруг улов (таблица лута рыбалки).
 */
public class WhaleEntity extends PathfinderMob implements Shearable {
	public static final int MAX_BARNACLES = 4;

	private static final EntityDataAccessor<Integer> BARNACLES =
			SynchedEntityData.defineId(WhaleEntity.class, EntityDataSerializers.INT);

	/** Шанс (1 из N за тик) отрастить новую ракушку: в среднем раз в ~5 минут. */
	private static final int BARNACLE_GROWTH_CHANCE = 6000;
	private static final int THANKS_DURATION = 40;
	private static final int THANKS_DROP_TICK = 22;
	private static final int DRY_OUT_GRACE = 600;

	private int thanksTicks = 0;
	private int outOfWaterTicks = 0;

	public WhaleEntity(EntityType<? extends WhaleEntity> type, Level level) {
		super(type, level);
		this.moveControl = new SmoothSwimmingMoveControl(this, 40, 6, 0.02F, 0.1F, true);
		this.lookControl = new SmoothSwimmingLookControl(this, 6);
		// Новый кит появляется уже с ракушками (при загрузке из сохранения значение перезапишется)
		this.setBarnacles(2 + this.random.nextInt(MAX_BARNACLES - 1));
	}

	public static AttributeSupplier.Builder createAttributes() {
		return PathfinderMob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, 60.0)
				.add(Attributes.MOVEMENT_SPEED, 0.9)
				.add(Attributes.TEMPT_RANGE, 12.0)
				.add(Attributes.KNOCKBACK_RESISTANCE, 0.9);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new TryFindWaterGoal(this));
		this.goalSelector.addGoal(1, new PanicGoal(this, 1.6));
		this.goalSelector.addGoal(2, new TemptGoal(this, 1.2, Ingredient.of(Items.SHEARS), false));
		this.goalSelector.addGoal(3, new RandomSwimmingGoal(this, 1.0, 60));
		this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 10.0F));
		this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
	}

	@Override
	protected PathNavigation createNavigation(Level level) {
		return new WaterBoundPathNavigation(this, level);
	}

	// ---------- ракушки ----------

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(BARNACLES, 0);
	}

	public int getBarnacles() {
		return this.entityData.get(BARNACLES);
	}

	public void setBarnacles(int count) {
		this.entityData.set(BARNACLES, Math.max(0, Math.min(MAX_BARNACLES, count)));
	}

	@Override
	public boolean readyForShearing() {
		return this.isAlive() && this.getBarnacles() > 0 && this.thanksTicks == 0;
	}

	@Override
	public void shear(ServerLevel level, SoundSource source, ItemStack shears) {
		level.playSound(null, this, SoundEvents.SHEEP_SHEAR, source, 1.0F, 0.7F);
		level.playSound(null, this, SoundEvents.TURTLE_EGG_CRACK, source, 1.0F, 1.2F);

		this.setBarnacles(this.getBarnacles() - 1);
		this.spawnAtLocation(level, new ItemStack(ModItems.BARNACLE, 1 + this.random.nextInt(2)));
		level.sendParticles(ParticleTypes.BUBBLE, this.getX(), this.getY() + 1.2, this.getZ(),
				12, 0.8, 0.4, 0.8, 0.05);

		if (this.getBarnacles() == 0) {
			this.startThanks(level);
		}
	}

	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (stack.is(Items.SHEARS)) {
			if (this.level() instanceof ServerLevel serverLevel && this.readyForShearing()) {
				this.shear(serverLevel, SoundSource.PLAYERS, stack);
				this.gameEvent(GameEvent.SHEAR, player);
				stack.hurtAndBreak(1, player, hand.asEquipmentSlot());
				return InteractionResult.SUCCESS_SERVER;
			}
			return InteractionResult.CONSUME;
		}
		return super.mobInteract(player, hand);
	}

	// ---------- благодарность ----------

	private void startThanks(ServerLevel level) {
		this.thanksTicks = THANKS_DURATION;
		level.playSound(null, this, SoundEvents.DOLPHIN_PLAY, SoundSource.NEUTRAL, 2.0F, 0.5F);
		// Кит делает рывок вверх и выпрыгивает из воды
		Vec3 motion = this.getDeltaMovement();
		this.setDeltaMovement(motion.x, 0.75, motion.z);
	}

	private void throwThanksLoot(ServerLevel level) {
		level.playSound(null, this, SoundEvents.GENERIC_SPLASH, SoundSource.NEUTRAL, 3.0F, 0.6F);
		level.playSound(null, this, SoundEvents.PLAYER_SPLASH_HIGH_SPEED, SoundSource.NEUTRAL, 2.0F, 0.7F);
		level.sendParticles(ParticleTypes.SPLASH, this.getX(), this.getY() + 1.0, this.getZ(),
				120, 2.0, 0.6, 2.0, 0.3);
		level.sendParticles(ParticleTypes.BUBBLE_POP, this.getX(), this.getY() + 1.0, this.getZ(),
				40, 1.5, 0.5, 1.5, 0.1);

		LootTable table = level.getServer().reloadableRegistries().getLootTable(BuiltInLootTables.FISHING);
		LootParams params = new LootParams.Builder(level)
				.withParameter(LootContextParams.ORIGIN, this.position())
				.withParameter(LootContextParams.TOOL, new ItemStack(Items.FISHING_ROD))
				.withLuck(1.0F)
				.create(LootContextParamSets.FISHING);

		int rolls = 5 + this.random.nextInt(4);
		for (int i = 0; i < rolls; i++) {
			List<ItemStack> items = table.getRandomItems(params, this.random.nextLong());
			for (ItemStack item : items) {
				this.flingItem(level, item);
			}
		}

		// Небольшой бонус: иногда кит отдаёт что-то со дна
		if (this.random.nextInt(4) == 0) {
			this.flingItem(level, new ItemStack(Items.NAUTILUS_SHELL));
		}
		if (this.random.nextInt(3) == 0) {
			this.flingItem(level, new ItemStack(Items.PRISMARINE_CRYSTALS, 1 + this.random.nextInt(3)));
		}
	}

	private void flingItem(ServerLevel level, ItemStack stack) {
		if (stack.isEmpty()) {
			return;
		}
		ItemEntity entity = new ItemEntity(level, this.getX(), this.getY() + 1.4, this.getZ(), stack);
		double angle = this.random.nextDouble() * Math.PI * 2.0;
		double power = 0.25 + this.random.nextDouble() * 0.3;
		entity.setDeltaMovement(Math.cos(angle) * power, 0.45 + this.random.nextDouble() * 0.35, Math.sin(angle) * power);
		entity.setDefaultPickUpDelay();
		level.addFreshEntity(entity);
	}

	// ---------- тик ----------

	@Override
	public void tick() {
		super.tick();

		if (this.level() instanceof ServerLevel level) {
			if (this.thanksTicks > 0) {
				this.thanksTicks--;
				if (this.thanksTicks == THANKS_DROP_TICK) {
					this.throwThanksLoot(level);
				}
			}

			if (this.getBarnacles() < MAX_BARNACLES && this.random.nextInt(BARNACLE_GROWTH_CHANCE) == 0) {
				this.setBarnacles(this.getBarnacles() + 1);
			}

			if (this.isInWaterOrRain()) {
				this.outOfWaterTicks = 0;
			} else {
				this.outOfWaterTicks++;
				if (this.outOfWaterTicks > DRY_OUT_GRACE && this.tickCount % 20 == 0) {
					this.hurtServer(level, this.damageSources().dryOut(), 1.0F);
				}
			}
		}
	}

	@Override
	public void travel(Vec3 input) {
		if (this.isInWater() && !this.level().isClientSide()) {
			this.moveRelative(this.getSpeed(), input);
			this.move(MoverType.SELF, this.getDeltaMovement());
			this.setDeltaMovement(this.getDeltaMovement().scale(0.9));
		} else {
			super.travel(input);
		}
	}

	@Override
	public boolean canBreatheUnderwater() {
		return true;
	}

	@Override
	public int getMaxHeadXRot() {
		return 1;
	}

	@Override
	public int getMaxHeadYRot() {
		return 1;
	}

	// ---------- звуки ----------

	@Override
	protected SoundEvent getAmbientSound() {
		return this.isInWater() ? SoundEvents.DOLPHIN_AMBIENT_WATER : SoundEvents.DOLPHIN_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.DOLPHIN_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.DOLPHIN_DEATH;
	}

	@Override
	public float getVoicePitch() {
		return 0.35F + this.random.nextFloat() * 0.1F;
	}

	@Override
	protected float getSoundVolume() {
		return 2.0F;
	}

	@Override
	public int getAmbientSoundInterval() {
		return 400;
	}

	// ---------- сохранение ----------

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("barnacles", this.getBarnacles());
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.setBarnacles(input.getInt("barnacles").orElse(0));
	}
}
