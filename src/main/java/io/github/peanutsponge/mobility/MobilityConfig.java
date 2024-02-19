package io.github.peanutsponge.mobility;
import eu.midnightdust.lib.config.MidnightConfig;

public class MobilityConfig extends MidnightConfig{
	@Entry(category = "Walk Settings") public static boolean alwaysSprint = false;
	@Entry(category = "Walk Settings") public static boolean hasDrag = true;
	@Entry(category = "Walk Settings") public static boolean hasGravity = true;
//	@Entry(category = "Walk Settings") public static boolean sidewaysSprint = false;
	@Entry(category = "Walk Settings") public static float defaultGenericMovementSpeed = 0.1F;
	@Entry(category = "Walk Settings") public static float sprintMovementSpeedMultiplier = 0.3F;
//	@Entry(category = "Walk Settings") public static float sneakMovementSpeedMultiplier = -0.5F;
	@Entry(category = "Walk Settings") public static float stepHeight = 0.6F;
	@Entry(category = "Walk Settings") public static float boatStepHeight = 0.0F;

	// Jump Settings
	@Entry(category = "Jump Settings") public static float jumpStrength = 0.42F;
	@Entry(category = "Jump Settings") public static int coyoteTime = 10;

	@Entry(category = "Jump Settings") public static boolean smoothJumps = false;
	@Entry(category = "Jump Settings") public static float jumpHorizontalVelocityMultiplier = 0.2F;
	@Entry(category = "Jump Settings") public static float sprintJumpHorizontalVelocityMultiplier = 0.0F;

}
