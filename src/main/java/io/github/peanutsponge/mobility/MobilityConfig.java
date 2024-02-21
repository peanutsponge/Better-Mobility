package io.github.peanutsponge.mobility;
import eu.midnightdust.lib.config.MidnightConfig;

public class MobilityConfig extends MidnightConfig{
	// Walk Settings
	@Entry(category = "Walking") public static boolean alwaysSprint = false;
	@Entry(category = "Walking") public static boolean sidewaysSprint = false;
	@Entry(category = "Walking") public static boolean backwardsSprint = false;
	@Entry(category = "Walking") public static float defaultGenericMovementSpeed = 0.1F;
	@Entry(category = "Walking") public static float sprintMovementSpeedMultiplier = 0.3F;
//	@Entry(category = "Walk Settings") public static float sneakMovementSpeedMultiplier = -0.5F;
	@Entry(category = "Walking") public static float stepHeight = 0.6F;


	// Jump Settings
	@Entry(category = "Jumping") public static float jumpStrength = 0.42F;
	@Entry(category = "Jumping") public static int coyoteTime = 10;

	@Entry(category = "Jumping") public static boolean smoothJumps = false;
	@Entry(category = "Jumping") public static float jumpHorizontalVelocityMultiplier = 0.0F;
	@Entry(category = "Jumping") public static float sprintJumpHorizontalVelocityMultiplier = 0.2F;

	// Jump Settings
	@Entry(category = "Wall") public static boolean wallSliding = false;
	@Entry(category = "Wall") public static boolean forwardToClimb = true;

	// Experimental Settings
	@Entry(category = "Experimental") public static float boatStepHeight = 0.0F;
	@Entry(category = "Experimental") public static boolean hasDrag = true;
	@Entry(category = "Experimental") public static boolean hasGravity = true;

}
