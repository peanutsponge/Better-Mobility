package io.github.peanutsponge.mobility.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.*;

import static io.github.peanutsponge.mobility.MobilityConfig.*;


@Mixin(value = ClientPlayerEntity.class)
public abstract class ClientPlayerEntityMixin extends PlayerEntity {


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

	@Shadow private boolean isWalking(){return true;}
	@Shadow private boolean canSprint(){return true;}
	@Shadow private boolean canVehicleSprint(Entity entity){return true;}

	 /**
	  * @author
	  * @reason
	  */
	 @Overwrite
	 private boolean canStartSprinting() {
		 return !this.isSprinting() && this.isWalking() && this.canSprint() && !this.isUsingItem() && !this.hasStatusEffect(StatusEffects.BLINDNESS) && (!this.hasVehicle() || this.canVehicleSprint(this.getVehicle())) && !this.isFallFlying();
	 }
    @Override
	public void travel(Vec3d movementInput){
		if (canStartSprinting())
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

	@Override
	public boolean hasNoDrag() {
		return !hasDrag;
	}
	@Override
	public boolean hasNoGravity() {
		return !hasGravity;
	}

}
