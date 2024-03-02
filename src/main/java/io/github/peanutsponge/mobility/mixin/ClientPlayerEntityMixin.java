package io.github.peanutsponge.mobility.mixin;

import com.llamalad7.mixinextras.injector.ModifyReceiver;
import com.mojang.authlib.GameProfile;
import net.minecraft.block.Blocks;
import net.minecraft.block.PowderSnowBlock;
import net.minecraft.client.input.Input;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Optional;
import java.util.UUID;

import static io.github.peanutsponge.mobility.MobilityConfig.*;


@Mixin(value = ClientPlayerEntity.class, priority = 999)
public abstract class ClientPlayerEntityMixin extends PlayerEntity {

	public ClientPlayerEntityMixin(World world, BlockPos pos, float yaw, GameProfile gameProfile) {
		super(world, pos, yaw, gameProfile);
	}
	@Shadow private boolean canSprint(){return true;}
	@Shadow private boolean canStartSprinting() {return true;}
	@Shadow public Input input;

	@Shadow
	public abstract float getYaw(float tickDelta);

	@Shadow
	public abstract float getPitch(float tickDelta);

	@Shadow
	public abstract boolean isHoldingOntoLadder();

	/**
	 * Executes a jump.
	 * Edited s.t. variables can be configured
	 */
	@Override
	public void jump(){
		Vec3d velocity = this.getVelocity();
        velocity = new Vec3d(velocity.x, (double)this.getJumpVelocity(), velocity.z);
        float f = this.getYaw() * 0.017453292F;
        if (this.isSprinting())
            velocity = velocity.add((double)(-MathHelper.sin(f) * sprintJumpHorizontalVelocityMultiplier),
				0.0, (double)(MathHelper.cos(f) * sprintJumpHorizontalVelocityMultiplier));
        else
            velocity = velocity.add((double)(-MathHelper.sin(f) * jumpHorizontalVelocityMultiplier),
				0.0, (double)(MathHelper.cos(f) * jumpHorizontalVelocityMultiplier));
		super.jump();
        this.setVelocity(velocity);
        this.velocityDirty = true;
    }
    @Override
    protected float getJumpVelocity() {
        return jumpStrength * this.getJumpVelocityMultiplier() + this.getJumpBoostVelocityModifier();
    }

    @Override
	public void travel(Vec3d movementInput){
		this.horizontalCollision = false;
		if (this.canStartSprinting())
        	this.setSprinting(alwaysSprint||isSprinting());
        if (this.isSneaking()) {
            this.setStepHeight(0.6F);
        } else {
            this.setStepHeight(stepHeight);
        }
        this.getAbilities().setWalkSpeed(defaultGenericMovementSpeed);
        this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue((double) this.getAbilities().getWalkSpeed());
		super.travel(movementInput);
    }

	@Unique
	private boolean isWalking2() {
		return this.input.forwardMovement > 1.0E-5F || (Math.abs(this.input.sidewaysMovement) > 1.0E-5F && this.input.forwardMovement > -1.0E-5F&& sidewaysSprint) || (this.input.forwardMovement < -1.0E-5F && backwardsSprint);
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
        entityAttributeInstance.removeModifier(SPRINTING_SPEED_BOOST.getId());
		if (sprinting) {
			entityAttributeInstance.addTemporaryModifier(SPRINTING_SPEED_BOOST);
		}
	}
	/**
	 * Removes sprinting loss on collision, strafing, and more.
	 */
	@ModifyReceiver(method = "tickMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;setSprinting(Z)V"))
	private ClientPlayerEntity removeSprintingLogic(ClientPlayerEntity clientPlayerEntity, boolean sprinting) {
		if (sprinting || !this.isWalking2() || !this.canSprint()){
			this.setSprinting(sprinting);
		}
		return clientPlayerEntity;
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

		if (this.isWalling && ((wallJumping && !this.input.jumping && yaw > minimumYawToJump) || (jumpOnLeavingWall && wallsTouching == 0))) {// Do a wall jump
			float f = this.getYaw() * 0.017453292F;
			motionX += -MathHelper.sin(f) * wallJumpVelocityMultiplier;
			motionZ += MathHelper.cos(f) * wallJumpVelocityMultiplier;
			motionY += wallJumpHeight * this.getJumpVelocityMultiplier() + this.getJumpBoostVelocityModifier();
			this.isWalling = false;
			this.slidingPos = Optional.of(this.getBlockPos());
			this.resetFallDistance();
			return new Vec3d(motionX, motionY, motionZ);
		}

		if (wallsTouching == 0 || !this.input.jumping || (yaw > 90 && !this.isWalling)) { // Stop all wall movement
			this.isWalling = false;
			return motion;
		}


		this.isWalling = true;
		if (wallRunning && this.input.hasForwardMovement() && yaw > yawToRun && (Math.abs(motionX) > minimumWallRunSpeed|| Math.abs(motionZ) > minimumWallRunSpeed)) { // Wall Running
			motionY = Math.max(motion.y, -wallRunSlidingSpeed);
			motionX = motion.x * (1 + wallRunSpeedBonus);
			motionZ = motion.z * (1 + wallRunSpeedBonus);
		}else if (wallClimbing && pitch > pitchToClimb && yaw < 90 && this.input.hasForwardMovement()) { // Wall Climbing
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

	/**
	 * Experimental settings
	 */
	@Override
	public boolean hasNoDrag() {
		return !hasDrag;
	}
	@Override
	public boolean hasNoGravity() {
		return !hasGravity;
	}
}
