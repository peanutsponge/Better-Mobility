package io.github.peanutsponge.mobility.mixin;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static io.github.peanutsponge.mobility.MobilityConfig.smoothJumps;
import static io.github.peanutsponge.mobility.MobilityMod.LOGGER;

@Mixin(value = Block.class)
public abstract class BlockMixin extends AbstractBlock {


	@Shadow
	public abstract BlockState getDefaultState();

	public BlockMixin(Settings settings) {
		super(settings);
	}

	@Override
	public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity){
		if (this.isSliding(state, world,pos, entity)) {
			this.updateSlidingVelocity(entity);
			LOGGER.info("slideOnBlocks: SLIDE");
		}
			super.onEntityCollision(state, world, pos, entity);
	}

	@Unique
	private boolean isSliding(BlockState state, World world,BlockPos pos, Entity entity) {
		if (this.collidable){
			return false;
//		} else if (this.isSideSolid()) {
//
//		} else if (!this.isFullCube(world, pos)) {
//			return false;
		} else if (entity.isOnGround()) {
			return false;
		}
		else if (entity.getY() > (double)pos.getY() + 0.9375 - 1.0E-7) {
			return false;
		}
		else if (entity.getVelocity().y >= -0.08) {
			return false;}
//		else {
//			double d = Math.abs((double)pos.getX() + 0.5 - entity.getX());
//			double e = Math.abs((double)pos.getZ() + 0.5 - entity.getZ());
//			double f = 0.4375 + (double)(entity.getWidth() / 2.0F);
//			return d + 1.0E-7 > f || e + 1.0E-7 > f;
//		}
		return true;
	}

	@Unique
	private void updateSlidingVelocity(Entity entity) {
		Vec3d vec3d = entity.getVelocity();
		if (vec3d.y < -0.13) {
			double d = -0.05 / vec3d.y;
			entity.setVelocity(new Vec3d(vec3d.x * d, -0.05, vec3d.z * d));
		} else {
			entity.setVelocity(new Vec3d(vec3d.x, -0.05, vec3d.z));
		}

		entity.resetFallDistance();
	}



}
