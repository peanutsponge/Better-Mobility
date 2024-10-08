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

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static io.github.peanutsponge.mobility.MobilityConfig.*;


@Mixin(value = ClientPlayerEntity.class, priority = 999)
public abstract class ClientPlayerEntityMixin extends PlayerEntity {

	public ClientPlayerEntityMixin(World world, BlockPos pos, float yaw, GameProfile gameProfile) {
		super(world, pos, yaw, gameProfile);
	}
	@Shadow
	protected abstract boolean canSprint();
	@Shadow public Input input;

	/**
	 * Removes sprinting loss on collision, strafing, and more.
	 */
	@ModifyReceiver(method = "tickMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;setSprinting(Z)V"))
	private ClientPlayerEntity removeSprintingLogic(ClientPlayerEntity clientPlayerEntity, boolean sprinting) {
		if (sprinting ||
			!this.canSprint() ||
			!(this.input.forwardMovement > 1.0E-5F ||
				(Math.abs(this.input.sidewaysMovement) > 1.0E-5F &&
					this.input.forwardMovement > -1.0E-5F && sidewaysSprint) ||
				(this.input.forwardMovement < -1.0E-5F && backwardsSprint))){
			this.setSprinting(sprinting);
		}
		return clientPlayerEntity;
	}
}
