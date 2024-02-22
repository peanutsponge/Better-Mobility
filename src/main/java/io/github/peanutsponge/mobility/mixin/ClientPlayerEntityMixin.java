package io.github.peanutsponge.mobility.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.block.*;
import net.minecraft.client.input.Input;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;
import java.util.UUID;

import static io.github.peanutsponge.mobility.MobilityConfig.*;


@Mixin(value = ClientPlayerEntity.class)
public abstract class ClientPlayerEntityMixin extends PlayerEntity {

	public ClientPlayerEntityMixin(World world, BlockPos pos, float yaw, GameProfile gameProfile) {
		super(world, pos, yaw, gameProfile);
	}
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


	@Shadow private boolean canSprint(){return true;}
	@Shadow private boolean canStartSprinting() {return true;}
	@Shadow public Input input;

	@Shadow
	public abstract float getYaw(float tickDelta);

	@Shadow
	public abstract float getPitch(float tickDelta);

	/**
	 * Allows sideways sprinting
	 * @author peanutsponge
	 * @reason simplest implementation
	 */
	@Overwrite
	private boolean isWalking() {
		return this.input.forwardMovement > 1.0E-5F || (Math.abs(this.input.sidewaysMovement) > 1.0E-5F && this.input.forwardMovement > -1.0E-5F&& sidewaysSprint) || (this.input.forwardMovement < -1.0E-5F && backwardsSprint);
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
	/**
	 * Sets sprinting to boolean.
	 * Edited s.t. sprinting multiplier can be configured.
	 * Edited s.t. sprint loss on collision is removed and sideways sprinting is possible.
	 */
	@Override
	public void setSprinting(boolean sprinting) {
		if (this.blockSprintingBlock) {
			this.blockSprintingBlock = false;
			return;
		}
		super.setSprinting(sprinting);
		EntityAttributeInstance entityAttributeInstance = this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
		EntityAttributeModifier SPRINTING_SPEED_BOOST = new EntityAttributeModifier(UUID.fromString("662A6B8D-DA3E-4C1C-8813-96EA6097278D"), "Sprinting speed boost", sprintMovementSpeedMultiplier, EntityAttributeModifier.Operation.MULTIPLY_TOTAL);
		entityAttributeInstance.removeModifier(SPRINTING_SPEED_BOOST.getId());
		if (sprinting) {
			entityAttributeInstance.addTemporaryModifier(SPRINTING_SPEED_BOOST);
		}
	}
	@Unique
	private boolean blockSprintingBlock = false;
	@Inject(method = "tickMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;setSprinting(Z)V", ordinal = 2))
	private void removeSprintingLogic(CallbackInfo info) {
		this.blockSprintingBlock = this.isWalking() && this.canSprint();
	}
	@Inject(method = "tickMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;setSprinting(Z)V", ordinal = 3))
	private void removeSprintingLogic2(CallbackInfo info) {
		this.blockSprintingBlock = this.isWalking() && this.canSprint();
	}



	/**
	 * Overrides the default friction behavior to add wall sliding.
	 */
	@Override
	public Vec3d handleFrictionAndCalculateMovement(Vec3d movementInput, float slipperiness) {
		this.updateVelocity(this.getMovementSpeed(slipperiness), movementInput);

		if (this.isClimbing())
			this.setVelocity(this.applyClimbingSpeed(this.getVelocity()));

		else if (wallSliding && !this.isSpectator() && !this.isOnGround())
			this.setVelocity(this.applySlidingSpeed(this.getVelocity()));

		this.move(MovementType.SELF, this.getVelocity());

		return this.getVelocity();
	}

	@Unique
	private Vec3d applySlidingSpeed(Vec3d motion) {
		if (!this.input.jumping) // add logic for when they let go of jump
			return motion;

		BlockPos blockPos = this.getBlockPos();
		World world = this.getWorld();
		double dx = (double)blockPos.getX() + 0.5 - this.getX();
		double dz = (double)blockPos.getZ() + 0.5 - this.getZ();
		double threshold = (double)(this.getWidth() / 2.0F) - 0.2F - 1.0E-7;

		boolean east = (world.isDirectionSolid( blockPos.east(),this, Direction.WEST) && -dx > threshold);
		boolean west = (world.isDirectionSolid( blockPos.west(),this, Direction.EAST) && dx > threshold);
		boolean north = (world.isDirectionSolid( blockPos.north(),this, Direction.SOUTH) && dz > threshold);
		boolean south = (world.isDirectionSolid( blockPos.south(),this, Direction.NORTH) && -dz > threshold);
		int wallsTouching = (east?1:0)+(west?1:0)+(north?1:0)+(south?1:0);
		if (wallsTouching == 0)
			return motion;


		float yaw = this.getYaw();
		float pitch = this.getPitch() * -1;
		System.out.println("yaw_raw: " + yaw);
		yaw += (90.0F * ((east?1:0)-(west?1:0) + (north?2:0)) / wallsTouching);
		System.out.println("yaw_adjust: " + yaw);
		yaw += 360.0F;
		yaw %= 360.0F;
		yaw -= 180.0F;
		yaw = Math.abs(yaw);
		yaw = 180.0F - yaw;
		System.out.println("yaw_wrapped: " + yaw);

		double motionX = motion.x;
		double motionZ = motion.z;
		double motionY = motion.y;

		motionX = MathHelper.clamp(motionX, -translationSpeed, translationSpeed);
		motionZ = MathHelper.clamp(motionZ, -translationSpeed, translationSpeed);

		if (this.input.hasForwardMovement() && yaw > yawToRun) { // Wall Running
			System.out.println("RUN");
			motionY = 0.0;
			motionX = motion.x;
			motionZ = motion.z;
		}else if (this.input.hasForwardMovement() && pitch > pitchToClimb) { // Wall Climbing
			System.out.println("CLIMB");
			motionY = climbingSpeed;
		} else if (motionY < 0.0 && this.isHoldingOntoLadder()) { //Shifting
			System.out.println("STICK");
			motionY = 0.0;
		} else{ // Wall Sliding
			System.out.println("SLIDE");
			 motionY = Math.max(motion.y, -slidingSpeed);
		}

		if (stickyClimbing){
			motionX *= ((north?1:0)+(south?1:0));
			motionZ *= ((east?1:0)+(west?1:0));
		}

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
	private Optional<BlockPos> slidingPos;
	@Override
	public Optional<BlockPos> getClimbingPos() {
		if (this.slidingPos.isEmpty())
			return super.getClimbingPos();
		return this.slidingPos;
	}

	@Unique
	private float getMovementSpeed(float slipperiness) {
		return this.isOnGround() ? this.getMovementSpeed() * (0.21600002F / (slipperiness * slipperiness * slipperiness)) : this.getAirSpeed();
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
