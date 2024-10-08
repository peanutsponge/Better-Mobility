package io.github.peanutsponge.mobility;

import eu.midnightdust.lib.config.MidnightConfig;
import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.qsl.base.api.entrypoint.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MobilityMod implements ModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger("Mobility Mod");

	@Override
	public void onInitialize(ModContainer mod) {
		LOGGER.info("Hello Quilt world from {}!", mod.metadata().name());
		MidnightConfig.init(mod.metadata().id(), MobilityConfig.class);
	}
}
