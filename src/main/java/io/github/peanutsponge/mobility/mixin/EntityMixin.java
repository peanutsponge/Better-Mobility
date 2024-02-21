package io.github.peanutsponge.mobility.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static io.github.peanutsponge.mobility.MobilityConfig.smoothJumps;


@Mixin(value = Entity.class)
public abstract class EntityMixin {
	@Shadow boolean onGround;
	@Unique boolean realOnGround;
	@Inject(method = "adjustMovementForCollisions", at = @At("HEAD"))
	void allowJumpStep(Vec3d movement, CallbackInfoReturnable<Vec3d> cir) {
		this.realOnGround = this.onGround;
		this.onGround |= smoothJumps;
	}
	@Inject(method = "adjustMovementForCollisions", at = @At("TAIL"))
	void allowJumpStepTail(Vec3d movement, CallbackInfoReturnable<Vec3d> cir) {
		this.onGround = this.realOnGround;
	}
}
