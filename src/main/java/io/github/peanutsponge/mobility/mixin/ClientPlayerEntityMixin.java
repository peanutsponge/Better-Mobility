package io.github.peanutsponge.mobility.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.block.*;
import net.minecraft.client.input.Input;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
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

	@Unique
	private Optional<BlockPos> climbingPos;

    @Unique
    private Vec3d velocity;

	public ClientPlayerEntityMixin(World world, BlockPos pos, float yaw, GameProfile gameProfile) {
		super(world, pos, yaw, gameProfile);
	}

	@Override
	public void jump(){
        this.velocity = this.getVelocity();
        System.out.println("get: " + this.velocity);
        velocity = new Vec3d(velocity.x, (double)this.getJumpVelocity(), velocity.z);
        float f = this.getYaw() * 0.017453292F;
        if (this.isSprinting())
            this.velocity = this.velocity.add((double)(-MathHelper.sin(f) * sprintJumpHorizontalVelocityMultiplier),
				0.0, (double)(MathHelper.cos(f) * sprintJumpHorizontalVelocityMultiplier));
        else
            this.velocity = this.velocity.add((double)(-MathHelper.sin(f) * jumpHorizontalVelocityMultiplier),
				0.0, (double)(MathHelper.cos(f) * jumpHorizontalVelocityMultiplier));
        System.out.println("add: " + this.velocity);
		super.jump();
        System.out.println("set: "+ this.velocity);
        this.setVelocity(this.velocity);
        this.velocityDirty = true;
    }
    @Override
    protected float getJumpVelocity() {
        return jumpStrength * this.getJumpVelocityMultiplier() + this.getJumpBoostVelocityModifier();
    }


	@Shadow private boolean canSprint(){return true;}
	@Shadow private boolean canVehicleSprint(Entity entity){return true;}

	@Shadow
	 private boolean canStartSprinting() {
		 return !this.isSprinting() && this.isWalking() && this.canSprint() && !this.isUsingItem() && !this.hasStatusEffect(StatusEffects.BLINDNESS) && (!this.hasVehicle() || this.canVehicleSprint(this.getVehicle())) && !this.isFallFlying();
	 }
	 @Shadow public Input input;

	/**
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
	@Unique
	private boolean blockSprintingBlock = false;
	@Inject(method = "tickMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;setSprinting(Z)V", ordinal = 2))
	private void removeSprintingLogic(CallbackInfo info) {
		System.out.println("2: blockSprintingBlock" + this.blockSprintingBlock);
		System.out.println("2: walkcan" + this.isWalking() + this.canSprint());
		this.blockSprintingBlock = this.isWalking() && this.canSprint();
		System.out.println("2: blockSprintingBlock" + this.blockSprintingBlock);
	}
	@Inject(method = "tickMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;setSprinting(Z)V", ordinal = 3))
	private void removeSprintingLogic2(CallbackInfo info) {
		System.out.println("3 : blockSprintingBlock" + this.blockSprintingBlock);
		System.out.println("3: walkcan" + this.isWalking() + this.canSprint());
		this.blockSprintingBlock = this.isWalking() && this.canSprint();
		System.out.println("3 : blockSprintingBlock" + this.blockSprintingBlock);
	}

	@Override
	public void setSprinting(boolean sprinting) {
		System.out.println("sprinting: " + sprinting + "blockSprintingBlock" + this.blockSprintingBlock);
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


	private boolean canClimbTrapdoor(BlockPos pos, BlockState state) {
		if ((Boolean)state.get(TrapdoorBlock.OPEN)) {
			BlockState blockState = this.getWorld().getBlockState(pos.down());
			if (blockState.isOf(Blocks.LADDER) && blockState.get(LadderBlock.FACING) == state.get(TrapdoorBlock.FACING)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean isClimbing() {
		if (this.isSpectator()||(!forwardToClimb && this.isOnGround())) {
				return false;
			}

		BlockPos blockPos = this.getBlockPos();
		BlockState blockState = this.getBlockStateAtPos();
		if (blockState.isIn(BlockTags.CLIMBABLE) || (blockState.getBlock() instanceof TrapdoorBlock && this.canClimbTrapdoor(blockPos, blockState))){
			this.climbingPos = Optional.of(blockPos);
			return true;
		} else if (climbOnAllBlocks){
			World world = this.getWorld();
			double dx = (double)blockPos.getX() + 0.5 - this.getX();
			double dz = (double)blockPos.getZ() + 0.5 - this.getZ();
			double threshold = (double)(this.getWidth() / 2.0F) - 0.1F - 1.0E-7;
			if ((world.isDirectionSolid( blockPos.east(),this, Direction.WEST) && -dx > threshold)||
				(world.isDirectionSolid( blockPos.west(),this, Direction.EAST) && dx > threshold) ||
				(world.isDirectionSolid( blockPos.north(),this, Direction.SOUTH) && dz > threshold)||
				(world.isDirectionSolid( blockPos.south(),this, Direction.NORTH) && -dz > threshold)){
				this.climbingPos = Optional.of(blockPos);
				return true;
			}
		}
		return false;
	}
	//used in fall damage calculations
	@Override
	public Optional<BlockPos> getClimbingPos() {
		if (this.climbingPos.isEmpty())
			return super.getClimbingPos();
		return this.climbingPos;
	}
	@Override
	public Vec3d handleFrictionAndCalculateMovement(Vec3d movementInput, float slipperiness) {
		this.updateVelocity(this.getMovementSpeed(slipperiness), movementInput);
		this.setVelocity(this.applyClimbingSpeed(this.getVelocity()));
		this.move(MovementType.SELF, this.getVelocity());
		Vec3d vec3d = this.getVelocity();
		if ((this.jumping) && (this.isClimbing() || this.getBlockStateAtPos().isOf(Blocks.POWDER_SNOW) && PowderSnowBlock.canWalkOnPowderSnow(this))) {
			vec3d = new Vec3d(vec3d.x, 0.2, vec3d.z);
		}

		return vec3d;
	}
	@Unique
	private Vec3d applyClimbingSpeed(Vec3d motion) {
		if (this.isClimbing()) {
			this.resetFallDistance();
			float f = 0.15F;
			double d = MathHelper.clamp(motion.x, -0.15000000596046448, 0.15000000596046448);
			double e = MathHelper.clamp(motion.z, -0.15000000596046448, 0.15000000596046448);
			double g = Math.max(motion.y, -0.15000000596046448);
			if (g < 0.0 && !this.getBlockStateAtPos().isOf(Blocks.SCAFFOLDING) && this.isHoldingOntoLadder() && this instanceof PlayerEntity) {
				g = 0.0;
			}

			motion = new Vec3d(d, g, e);
		}

		return motion;
	}

	@Unique
	private float getMovementSpeed(float slipperiness) {
		return this.isOnGround() ? this.getMovementSpeed() * (0.21600002F / (slipperiness * slipperiness * slipperiness)) : this.getAirSpeed();
	}
	@Override
	public boolean hasNoDrag() {
		return !hasDrag;
	}
	@Override
	public boolean hasNoGravity() {
		return !hasGravity;
	}
}
