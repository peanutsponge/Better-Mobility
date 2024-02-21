package io.github.peanutsponge.mobility.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static io.github.peanutsponge.mobility.MobilityConfig.*;


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
