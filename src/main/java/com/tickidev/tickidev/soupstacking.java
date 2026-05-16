package com.tickidev.tickidev;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod(soupstacking.MODID)
public class soupstacking {
    public static final String MODID = "soupstacking";
    public static final Logger LOGGER = LogUtils.getLogger();

    public soupstacking(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("Soup Stacking Mod initialized!");
        LOGGER.info("All items with 'soup' (or 'суп') in their name will stack up to 16!");
    }
}
