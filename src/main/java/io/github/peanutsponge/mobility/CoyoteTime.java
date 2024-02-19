package io.github.peanutsponge.mobility;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import org.quiltmc.qsl.lifecycle.api.client.event.ClientTickEvents;

import static io.github.peanutsponge.mobility.MobilityConfig.coyoteTime;

public class CoyoteTime implements ClientTickEvents.End{
	int fallingTicks = 0;
	private final double[] yValues = {0, 0};
	@Override
	public void endClientTick(MinecraftClient client) {
		ClientPlayerEntity player;
		player = client.player;

		if (player == null) {
			return;
		}

		if (player.isOnGround()){
			yValues[0] = player.getY();
			yValues[1] = player.getY();
		}

		if (!player.isOnGround()) {
			yValues[1] = player.getY();
			fallingTicks ++;
			if (fallingTicks < coyoteTime && player.input.jumping && yValues[1] < yValues[0]) {
				player.jump();
				fallingTicks = coyoteTime;
			}
		} else {
			fallingTicks = 0;
		}
	}
}
