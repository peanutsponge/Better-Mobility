package io.github.peanutsponge.mobility.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
//
//import static io.github.peanutsponge.mobility.MobilityConfig.sprintMovementSpeedMultiplier;


@Mixin(value = LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {
    public LivingEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }
//
//    @Shadow
//    public abstract EntityAttributeInstance getAttributeInstance(net.minecraft.entity.attribute.EntityAttribute attribute);

//    /**
//     * @author peanutsponge
//     * @reason easy option
//     */
//    @Overwrite
//    public void setSprinting(boolean sprinting) {
//        super.setSprinting(sprinting);
//        EntityAttributeInstance entityAttributeInstance = this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
//        EntityAttributeModifier SPRINTING_SPEED_BOOST = new EntityAttributeModifier(UUID.fromString("662A6B8D-DA3E-4C1C-8813-96EA6097278D"), "Sprinting speed boost", sprintMovementSpeedMultiplier, EntityAttributeModifier.Operation.MULTIPLY_TOTAL);
//        entityAttributeInstance.removeModifier(SPRINTING_SPEED_BOOST.getId());
//        if (sprinting) {
//            entityAttributeInstance.addTemporaryModifier(SPRINTING_SPEED_BOOST);
//        }
//    }

}
