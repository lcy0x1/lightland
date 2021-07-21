package com.hikarishima.lightland;

import com.hikarishima.lightland.event.forge.DamageEventHandler;
import com.hikarishima.lightland.event.forge.GenericEventHandler;
import com.hikarishima.lightland.event.forge.ItemUseEventHandler;
import com.hikarishima.lightland.event.forge.WorldGenEventHandler;
import com.hikarishima.lightland.proxy.PacketHandler;
import com.hikarishima.lightland.registry.ContainerRegistry;
import com.hikarishima.lightland.world.LightLandWorldType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

// The value here should match an entry in the META-INF/mods.toml file
@SuppressWarnings("unused")
@Mod("lightland")
public class LightLand {

    public static final String MODID = "lightland";
    public static final String NETWORK_VERSION = "1";
    public static boolean generate = false;

    public static LightLandWorldType WORLD_TYPE = new LightLandWorldType();

    public LightLand() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        bus.addListener(this::setup);
        bus.addListener(this::doClientStuff);
        if (generate) MinecraftForge.EVENT_BUS.register(new WorldGenEventHandler());
        MinecraftForge.EVENT_BUS.register(new ItemUseEventHandler());
        MinecraftForge.EVENT_BUS.register(new GenericEventHandler());
        MinecraftForge.EVENT_BUS.register(new DamageEventHandler());
        PacketHandler.registerPackets();
    }

    private void setup(final FMLCommonSetupEvent event) {
        WorldGenEventHandler.mod_setup();
    }

    private void doClientStuff(final FMLClientSetupEvent event) {
        ContainerRegistry.registerScreens();
    }

}
