package io.github.peanutsponge.mobility.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;
import java.util.UUID;

import static io.github.peanutsponge.mobility.MobilityConfig.*;

@Mixin(value = LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {
    public LivingEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @Shadow
    public abstract EntityAttributeInstance getAttributeInstance(net.minecraft.entity.attribute.EntityAttribute attribute);

    /**
     * @author peanutsponge
     * @reason easy option
     */
    @Overwrite
	public void setSprinting(boolean sprinting) {
		super.setSprinting(sprinting);
		EntityAttributeInstance entityAttributeInstance = this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
		EntityAttributeModifier SPRINTING_SPEED_BOOST = new EntityAttributeModifier(UUID.fromString("662A6B8D-DA3E-4C1C-8813-96EA6097278D"), "Sprinting speed boost", sprintMovementSpeedMultiplier, EntityAttributeModifier.Operation.MULTIPLY_TOTAL);
		entityAttributeInstance.removeModifier(SPRINTING_SPEED_BOOST.getId());
		if (sprinting) {
			entityAttributeInstance.addTemporaryModifier(SPRINTING_SPEED_BOOST);
		}
	}

	@Shadow
	private Optional<BlockPos> climbingPos;

    @Shadow
    private boolean canClimbTrapdoor(BlockPos pos, BlockState state) {
        return false;
    }


	@Shadow
	public abstract void remove(RemovalReason reason);

	@Shadow
	protected abstract void fall(double fallDistance, boolean onGround, BlockState landedState, BlockPos landedPosition);

	/**
		 * @author peanutsponge
		 * @reason idk
		 */
	@Overwrite
	public boolean isClimbing() {
		if (this.getType() == EntityType.PLAYER){
			if (this.isSpectator()||(!forwardToClimb && this.isOnGround())) {
				return false;
			}
		} else if (onlyPlayersCanClimb){
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
//	@Unique
//	boolean realHorizontalCollision;
//	@Inject(method = "handleFrictionAndCalculateMovement", at = @At("HEAD"))
//	void removeForwardClimbingHead(Vec3d movementInput, float slipperiness, CallbackInfoReturnable<Vec3d> cir){
//		this.realHorizontalCollision = this.horizontalCollision;
//		this.horizontalCollision = false;
//	}
//	@Inject(method = "handleFrictionAndCalculateMovement", at = @At("TAIL"))
//	void removeForwardClimbingTail(Vec3d movementInput, float slipperiness, CallbackInfoReturnable<Vec3d> cir){
//		this.horizontalCollision = this.realHorizontalCollision;
//	}


}
