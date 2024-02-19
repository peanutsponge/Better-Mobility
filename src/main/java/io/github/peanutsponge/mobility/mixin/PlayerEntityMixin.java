package io.github.peanutsponge.mobility.mixin;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static io.github.peanutsponge.mobility.MobilityConfig.*;


@Mixin(value = PlayerEntity.class)
public abstract class PlayerEntityMixin extends LivingEntity {

    protected PlayerEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }
    @Unique
    private Vec3d velocity;

    @Inject(method = "jump", at = @At("HEAD"))
    void jumpSpeedGet(CallbackInfo ci) {
        this.velocity = this.getVelocity();
        System.out.println("get: " + this.velocity);
        velocity = new Vec3d(velocity.x, (double)this.getJumpVelocity(), velocity.z);
        float f = this.getYaw() * 0.017453292F;
        if (this.isSprinting())
            this.velocity = this.velocity.add((double)(-MathHelper.sin(f) * sprintJumpHorizontalVelocityMultiplier), 0.0, (double)(MathHelper.cos(f) * sprintJumpHorizontalVelocityMultiplier));
        else
            this.velocity = this.velocity.add((double)(-MathHelper.sin(f) * jumpHorizontalVelocityMultiplier), 0.0, (double)(MathHelper.cos(f) * jumpHorizontalVelocityMultiplier));
        System.out.println("add: " + this.velocity);

    }

    @Inject(method = "jump", at = @At("TAIL"))
    void jumpSpeedSet(CallbackInfo ci) {
        System.out.println("set: "+ this.velocity);
        this.setVelocity(this.velocity);
        this.velocityDirty = true;
    }
    @Override
    protected float getJumpVelocity() {
        return jumpStrength * this.getJumpVelocityMultiplier() + this.getJumpBoostVelocityModifier();
    }

    @Inject(method = "travel", at = @At("HEAD"))
    void alwaysSprint(CallbackInfo ci) {
        this.setSprinting(alwaysSprint||isSprinting());
        if (this.isSneaking()) {
            this.setStepHeight(0.6F);
        } else {
            this.setStepHeight(stepHeight);
        }
        this.abilities.setWalkSpeed(defaultGenericMovementSpeed);
        this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue((double)this.abilities.getWalkSpeed());
    }
    @Final
    @Shadow
    private PlayerAbilities abilities;
    }
