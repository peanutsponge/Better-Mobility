package io.github.peanutsponge.mobility.mixin;

import net.minecraft.block.Blocks;
import net.minecraft.block.PowderSnowBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.Input;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.player.PlayerEntity;
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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

import static io.github.peanutsponge.mobility.MobilityConfig.*;

@Mixin(value = LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {
    public LivingEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }
	@Shadow
	public abstract boolean isClimbing();
	//getAirSpeed
	@Shadow
	protected abstract float getAirSpeed();
	// getMovementSpeed
	@Shadow
	public abstract float getMovementSpeed();
	// getJumpBoostVelocityModifier
	@Shadow
	public abstract float getJumpBoostVelocityModifier();
	//isHoldingOntoLadder
	@Shadow
	public abstract boolean isHoldingOntoLadder();
	// jumping
	@Shadow
	protected boolean jumping;

	@Shadow
	public abstract void remove(RemovalReason reason);

	// input
	@Unique
	private Input input;

	{
		assert MinecraftClient.getInstance().player != null;
		input = MinecraftClient.getInstance().player.input;
	}

	/**
	 * Injects the default friction behavior to add wall sliding.
	 */
	@Inject(method = "handleFrictionAndCalculateMovement", at = @At("HEAD"), cancellable = true)
	public void handleFrictionAndCalculateMovement(Vec3d movementInput, float slipperiness, CallbackInfoReturnable<Vec3d> cir) {
		this.updateVelocity(this.getMovementSpeedUnique(slipperiness), movementInput);

		if (this.isClimbing())
			this.setVelocity(this.applyClimbingSpeed(this.getVelocity()));

		else if (wallMovement && !this.isSpectator())
			this.setVelocity(this.applyWallMovement(this.getVelocity()));

		this.move(MovementType.SELF, this.getVelocity());

		cir.setReturnValue(this.getVelocity());
	}

	@Unique
	private float getMovementSpeedUnique(float slipperiness) {
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
	@Inject(method = "getClimbingPos", at = @At("HEAD"), cancellable = true)
	public void getClimbingPosHead(CallbackInfoReturnable<Optional<BlockPos>> cir) {
		if (this.slidingPos.isEmpty())
			return;
		cir.setReturnValue(this.slidingPos);
	}
	@Inject(method = "isClimbing", at = @At("HEAD"), cancellable = true)
	public void isClimbingHead(CallbackInfoReturnable<Boolean> cir) {
		if (this.isWalling){
			cir.setReturnValue(true);
		}
	}
}
