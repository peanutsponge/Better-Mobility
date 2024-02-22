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
	@Entry(category = "Wall") public static boolean wallMovement = false;

	@Entry(category = "Wall") public static float wallDistance = 0.05F;
	@Entry(category = "Wall") public static boolean wallSliding = true;
	@Entry(category = "Wall") public static float slidingSpeed = 0.05F;
	@Entry(category = "Wall") public static boolean wallClimbing = true;
	@Entry(category = "Wall") public static float pitchToClimb = 0.0F;
	@Entry(category = "Wall") public static float climbingSpeed = 0.05F;
	@Entry(category = "Wall") public static boolean wallRunning = true;
	@Entry(category = "Wall") public static float yawToRun = 50.0F;
	@Entry(category = "Wall") public static boolean stickyMovement = true;
	@Entry(category = "Wall") public static float translationSpeed = 0.05F;
	@Entry(category = "Wall") public static boolean wallSticking = true;
	@Entry(category = "Wall") public static boolean wallJumping = true;
	@Entry(category = "Wall") public static int wallJumpingTime = 10;

	// Experimental Settings
	@Entry(category = "?") public static float boatStepHeight = 0.0F;
	@Entry(category = "?") public static boolean hasDrag = true;
	@Entry(category = "?") public static boolean hasGravity = true;

}
