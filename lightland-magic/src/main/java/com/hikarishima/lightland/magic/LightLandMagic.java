package com.hikarishima.lightland.magic;

import com.hikarishima.lightland.command.BaseCommand;
import com.hikarishima.lightland.magic.capabilities.MagicHandler;
import com.hikarishima.lightland.magic.capabilities.ToClientMsg;
import com.hikarishima.lightland.magic.capabilities.ToServerMsg;
import com.hikarishima.lightland.magic.command.ArcaneCommand;
import com.hikarishima.lightland.magic.command.MagicCommand;
import com.hikarishima.lightland.magic.event.ClientRenderEventHandler;
import com.hikarishima.lightland.magic.event.MagicMiscEventHandler;
import com.hikarishima.lightland.magic.event.MagicDamageEventHandler;
import com.hikarishima.lightland.magic.event.MagicEventHandler;
import com.hikarishima.lightland.magic.gui.container.ChemContainer;
import com.hikarishima.lightland.magic.gui.container.ChemPacket;
import com.hikarishima.lightland.magic.recipe.MagicRecipeRegistry;
import com.hikarishima.lightland.magic.registry.MagicContainerRegistry;
import com.hikarishima.lightland.magic.registry.MagicEntityRegistry;
import com.hikarishima.lightland.magic.registry.MagicItemRegistry;
import com.hikarishima.lightland.magic.registry.VanillaMagicRegistry;
import com.hikarishima.lightland.proxy.PacketHandler;
import com.hikarishima.lightland.registry.RegistryBase;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.network.NetworkDirection;

// The value here should match an entry in the META-INF/mods.toml file
@SuppressWarnings("unused")
@Mod("lightland-magic")
public class LightLandMagic {

    public static final String MODID = "lightland-magic";

    public LightLandMagic() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        bus.addListener(this::setup);
        bus.addListener(this::doClientStuff);
        MagicRegistry.initAllRegistries(bus);
        MinecraftForge.EVENT_BUS.register(new MagicEventHandler());
        MinecraftForge.EVENT_BUS.register(new ClientRenderEventHandler());
        MinecraftForge.EVENT_BUS.register(new MagicDamageEventHandler());
        MinecraftForge.EVENT_BUS.register(new MagicMiscEventHandler());
        RegistryBase.REGISTRIES.add(MagicRecipeRegistry.class);
        PacketHandler.reg(ToClientMsg.class, ToClientMsg::handle, NetworkDirection.PLAY_TO_CLIENT);
        PacketHandler.reg(ToServerMsg.class, ToServerMsg::handle, NetworkDirection.PLAY_TO_SERVER);
        PacketHandler.reg(ChemPacket.class, ChemContainer.class, NetworkDirection.PLAY_TO_SERVER);
        PacketHandler.reg(ClientRenderEventHandler.EffectToClient.class, ClientRenderEventHandler::handle, NetworkDirection.PLAY_TO_CLIENT);
        BaseCommand.LIST.add(ArcaneCommand::new);
        BaseCommand.LIST.add(MagicCommand::new);
    }

    private void setup(final FMLCommonSetupEvent event) {
        MagicHandler.register();
        ClientRenderEventHandler.init();
    }

    private void doClientStuff(final FMLClientSetupEvent event) {
        MagicContainerRegistry.registerScreens();
        MagicEntityRegistry.registerClient();
    }

}
