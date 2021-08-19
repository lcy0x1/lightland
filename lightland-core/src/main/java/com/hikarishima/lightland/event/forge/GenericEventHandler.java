package com.hikarishima.lightland.event.forge;

import com.hikarishima.lightland.command.BaseCommand;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.function.Consumer;

@SuppressWarnings("unused")
public class GenericEventHandler {

    @SubscribeEvent
    public void onCommandRegister(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSource> lightland = Commands.literal("lightland");
        for (Consumer<LiteralArgumentBuilder<CommandSource>> command : BaseCommand.LIST){
            command.accept(lightland);
        }
        event.getDispatcher().register(lightland);
    }

}
