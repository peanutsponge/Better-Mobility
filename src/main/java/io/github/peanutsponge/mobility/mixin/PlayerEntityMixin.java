package io.github.peanutsponge.mobility.mixin;

import net.minecraft.block.Blocks;
import net.minecraft.block.PowderSnowBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.Input;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.stat.Stats;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static io.github.peanutsponge.mobility.MobilityConfig.*;


@Mixin(value = PlayerEntity.class)
public abstract class PlayerEntityMixin extends LivingEntity {
	protected PlayerEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
		super(entityType, world);
	}

	@Shadow
	public abstract PlayerAbilities getAbilities();

	@Shadow
	public abstract HungerManager getHungerManager();

	@Shadow
	public abstract void incrementStat(Identifier stat);

	@Shadow
	public abstract void addExhaustion(float exhaustion);

	@Unique
	private boolean canStartSprinting() {
		return !this.isSprinting() && this.isWalking() && this.canSprint() && !this.isUsingItem() && !this.hasStatusEffect(StatusEffects.BLINDNESS) && (!this.hasVehicle() || this.canVehicleSprint(this.getVehicle())) && !this.isFallFlying();
	}

	@Unique
	private boolean canVehicleSprint(Entity entity) {
		return entity.canSprintAsVehicle() && entity.isLogicalSideForUpdatingMovement();
	}

	@Unique
	private boolean isWalking() {
		if (MinecraftClient.getInstance().player == null){
			return false;
		}
		Input input = MinecraftClient.getInstance().player.input;
		return this.isSubmergedInWater() ? input.hasForwardMovement() : (double)input.forwardMovement >= 0.8;
	}

	@Unique
	private boolean canSprint() {
		return this.hasVehicle() || (float)this.getHungerManager().getFoodLevel() > 6.0F || this.getAbilities().allowFlying;
	}

	/**
	 * Executes a jump.
	 * Edited s.t. variables can be configured
	 */
	@Inject(method = "jump", at = @At("HEAD"), cancellable = true)
	public void jump(CallbackInfo ci){
		Vec3d velocity = this.getVelocity();
		float jumpVelocity = jumpStrength * this.getJumpVelocityMultiplier() + this.getJumpBoostVelocityModifier();
		velocity = new Vec3d(velocity.x, jumpVelocity, velocity.z);
		float f = this.getYaw() * 0.017453292F;
		if (this.isSprinting())
			velocity = velocity.add(-MathHelper.sin(f) * sprintJumpHorizontalVelocityMultiplier,
				0.0, MathHelper.cos(f) * sprintJumpHorizontalVelocityMultiplier);
		else
			velocity = velocity.add(-MathHelper.sin(f) * jumpHorizontalVelocityMultiplier,
				0.0, MathHelper.cos(f) * jumpHorizontalVelocityMultiplier);
		this.setVelocity(velocity);
		this.velocityDirty = true;
		this.incrementStat(Stats.JUMP);
		if (this.isSprinting()) {
			this.addExhaustion(0.2F);
		} else {
			this.addExhaustion(0.05F);
		}
		ci.cancel();
	}



@Inject(method = "travel", at = @At("HEAD"))
public void travelHead(Vec3d movementInput, CallbackInfo ci){
		this.horizontalCollision = false;
		if (this.canStartSprinting())
			this.setSprinting(alwaysSprint||isSprinting());
		if (this.isSneaking()) {
			this.setStepHeight(0.6F);
		} else {
			this.setStepHeight(stepHeight);
		}
		this.getAbilities().setWalkSpeed(defaultGenericMovementSpeed);
		Objects.requireNonNull(this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED)).setBaseValue(this.getAbilities().getWalkSpeed());
	}


	/**
	 * Sets sprinting to boolean.
	 * Edited s.t. sprinting multiplier can be configured.
	 * Edited s.t. sprint loss on collision is removed and sideways sprinting is possible.
	 */
	@Override
	public void setSprinting(boolean sprinting) {
		super.setSprinting(sprinting);
		EntityAttributeInstance entityAttributeInstance = this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
		EntityAttributeModifier SPRINTING_SPEED_BOOST = new EntityAttributeModifier(UUID.fromString("662A6B8D-DA3E-4C1C-8813-96EA6097278D"), "Sprinting speed boost", sprintMovementSpeedMultiplier, EntityAttributeModifier.Operation.MULTIPLY_TOTAL);
		assert entityAttributeInstance != null;
		entityAttributeInstance.removeModifier(SPRINTING_SPEED_BOOST.getId());
		if (sprinting) {
			entityAttributeInstance.addTemporaryModifier(SPRINTING_SPEED_BOOST);
		}
	}



	/**
	 * Overrides the default friction behavior to add wall sliding.
	 */
	@Override
	public Vec3d handleFrictionAndCalculateMovement(Vec3d movementInput, float slipperiness) {
		this.updateVelocity(this.getMovementSpeed(slipperiness), movementInput);

		if (super.isClimbing())
			this.setVelocity(this.applyClimbingSpeed(this.getVelocity()));

		else if (wallMovement && !this.isSpectator())
			this.setVelocity(this.applyWallMovement(this.getVelocity()));

		this.move(MovementType.SELF, this.getVelocity());

		return this.getVelocity();
	}
	@Unique
	private float getMovementSpeed(float slipperiness) {
		return (this.isOnGround()) ? this.getMovementSpeed() * (0.21600002F / (slipperiness * slipperiness * slipperiness)) : this.getAirSpeed();
	}

	@Unique private boolean isWalling = false;
	/**
	 * Overrides the default friction behavior to add wall sliding, wall running, wall climbing, and wall sticking.
	 */
	@Unique
	private Vec3d applyWallMovement(Vec3d motion) {
		if (MinecraftClient.getInstance().player == null){
			return motion;
		}
		Input input = MinecraftClient.getInstance().player.input;
		if (this.isOnGround()){
			this.isWalling = false;
			return motion;
		}

		BlockPos blockPos = this.getBlockPos().up();
		World world = this.getWorld();

		double dx = (double)blockPos.getX() + 0.5 - this.getX();
		double dz = (double)blockPos.getZ() + 0.5 - this.getZ();
		double threshold = (double)(this.getWidth() / 2.0F) - 0.1F - wallDistance - 1.0E-7;

		boolean east = (world.isDirectionSolid( blockPos.east(),this, Direction.WEST) && -dx > threshold);
		boolean west = (world.isDirectionSolid( blockPos.west(),this, Direction.EAST) && dx > threshold);
		boolean north = (world.isDirectionSolid( blockPos.north(),this, Direction.SOUTH) && dz > threshold);
		boolean south = (world.isDirectionSolid( blockPos.south(),this, Direction.NORTH) && -dz > threshold);
		int wallsTouching = (east?1:0)+(west?1:0)+(north?1:0)+(south?1:0);

		if (wallsTouching == 0 && isWalling){ //Start only using head, continue with feet.
			blockPos = this.getBlockPos();
			east = (world.isDirectionSolid( blockPos.east(),this, Direction.WEST) && -dx > threshold);
			west = (world.isDirectionSolid( blockPos.west(),this, Direction.EAST) && dx > threshold);
			north = (world.isDirectionSolid( blockPos.north(),this, Direction.SOUTH) && dz > threshold);
			south = (world.isDirectionSolid( blockPos.south(),this, Direction.NORTH) && -dz > threshold);
			wallsTouching = (east?1:0)+(west?1:0)+(north?1:0)+(south?1:0);
		}

		float yaw = this.getYaw();
		float pitch = this.getPitch() * -1;
		yaw += (90.0F * ((east?1:0)-(west?1:0) + (north?(east?2:-2):0)) / wallsTouching);
		yaw = MathHelper.wrapDegrees(yaw);
		yaw = Math.abs(yaw);

		double motionX = motion.x;
		double motionZ = motion.z;
		double motionY = motion.y;

		if (this.isWalling && ((wallJumping && !input.jumping && yaw > minimumYawToJump) || (jumpOnLeavingWall && wallsTouching == 0))) {// Do a wall jump
			float f = this.getYaw() * 0.017453292F;
			motionX += -MathHelper.sin(f) * wallJumpVelocityMultiplier;
			motionZ += MathHelper.cos(f) * wallJumpVelocityMultiplier;
			motionY += wallJumpHeight * this.getJumpVelocityMultiplier() + this.getJumpBoostVelocityModifier();
			this.isWalling = false;
			this.slidingPos = Optional.of(this.getBlockPos());
			this.resetFallDistance();
			return new Vec3d(motionX, motionY, motionZ);
		}

		if (wallsTouching == 0 || !input.jumping || (yaw > 90 && !this.isWalling)) { // Stop all wall movement
			this.isWalling = false;
			return motion;
		}


		this.isWalling = true;
		if (wallRunning && input.hasForwardMovement() && yaw > yawToRun && (Math.abs(motionX) > minimumWallRunSpeed|| Math.abs(motionZ) > minimumWallRunSpeed)) { // Wall Running
			motionY = Math.max(motion.y, -wallRunSlidingSpeed);
			motionX = motion.x * (1 + wallRunSpeedBonus);
			motionZ = motion.z * (1 + wallRunSpeedBonus);
		}else if (wallClimbing && pitch > pitchToClimb && yaw < 90 && input.hasForwardMovement()) { // Wall Climbing
			motionY = climbingSpeed;
			motionX = MathHelper.clamp(motionX, -climbingSpeed, climbingSpeed);
			motionZ = MathHelper.clamp(motionZ, -climbingSpeed, climbingSpeed);
		} else if (wallSticking && motionY < 0.0 && this.isHoldingOntoLadder()) { //Shifting
			motionY = 0.0;
			motionX = MathHelper.clamp(motionX, -climbingSpeed, climbingSpeed);
			motionZ = MathHelper.clamp(motionZ, -climbingSpeed, climbingSpeed);
		} else if (wallSliding){ // Wall Sliding
			motionY = Math.max(motion.y, -slidingSpeed);
			motionX = MathHelper.clamp(motionX, -climbingSpeed, climbingSpeed);
			motionZ = MathHelper.clamp(motionZ, -climbingSpeed, climbingSpeed);
		} else {
			this.isWalling = false;
			return motion;
		}

		if (stickyMovement){ // Disable falling off the wall accidentally
			motionX *= ((north?1:0)+(south?1:0));
			motionZ *= ((east?1:0)+(west?1:0));
		}
		this.slidingPos = Optional.of(this.getBlockPos());
		this.resetFallDistance();
		return new Vec3d(motionX, motionY, motionZ);
	}

	@Unique
	private Vec3d applyClimbingSpeed(Vec3d motion) {
		this.resetFallDistance();
		float climbingSpeed = 0.15000000596046448F;
		double d = MathHelper.clamp(motion.x, -climbingSpeed, climbingSpeed);
		double e = MathHelper.clamp(motion.z, -climbingSpeed, climbingSpeed);
		double g = Math.max(motion.y, -climbingSpeed);
		if ((this.horizontalCollision || this.jumping) && (this.isClimbing() || this.getBlockStateAtPos().isOf(Blocks.POWDER_SNOW) && PowderSnowBlock.canWalkOnPowderSnow(this))) {
			g = 0.2;
		} else if (g < 0.0 && !this.getBlockStateAtPos().isOf(Blocks.SCAFFOLDING) && this.isHoldingOntoLadder()) {
			g = 0.0;
		}

		motion = new Vec3d(d, g, e);
		return motion;
	}

	/**
	 * Used in fall damage calculations, probably currently broken
	 */
	@Unique
	private Optional<BlockPos> slidingPos = Optional.empty();
	@Override
	public Optional<BlockPos> getClimbingPos() {
		if (this.slidingPos.isEmpty())
			return super.getClimbingPos();
		return this.slidingPos;
	}
	@Override
	public boolean isClimbing() {
		return super.isClimbing() || this.isWalling;
	}


}
