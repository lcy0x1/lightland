package com.hikarishima.lightland.magic.capabilities;

import com.hikarishima.lightland.magic.products.MagicProduct;
import com.hikarishima.lightland.proxy.PacketHandler;
import com.lcy0x1.core.util.SerialClass;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

@SerialClass
public class ToServerMsg extends PacketHandler.BaseSerialMsg {

    public enum Action {
        HEX((handler, tag) -> {
            MagicHolder holder = handler.magicHolder;
            String str = tag.getString("product");
            MagicProduct<?, ?> prod = holder.getProduct(holder.getRecipe(new ResourceLocation(str)));
            prod.tag.tag.put("hex", tag.getCompound("hex"));
            prod.getBase().tag.putInt("cost", tag.getInt("cost"));
        });

        private final BiConsumer<MagicHandler, CompoundNBT> cons;

        Action(BiConsumer<MagicHandler, CompoundNBT> cons) {
            this.cons = cons;
        }
    }

    public static void sendHexUpdate(MagicProduct<?, ?> prod) {
        CompoundNBT tag = new CompoundNBT();
        tag.putString("product", prod.recipe.id.toString());
        tag.putInt("cost", prod.getBase().tag.getInt("cost"));
        tag.put("hex", prod.tag.tag.getCompound("hex"));
        ToServerMsg msg = new ToServerMsg(Action.HEX, tag);
        PacketHandler.send(msg);
    }

    @SerialClass.SerialField
    public Action action;
    @SerialClass.SerialField
    public CompoundNBT tag;

    @Deprecated
    public ToServerMsg() {

    }

    private ToServerMsg(Action act, CompoundNBT tag) {
        this.action = act;
        this.tag = tag;
    }

    public static void handle(ToServerMsg msg, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayerEntity player = ctx.get().getSender();
        if (player == null)
            return;
        MagicHandler handler = MagicHandler.get(player);
        msg.action.cons.accept(handler, msg.tag);
        ctx.get().setPacketHandled(true);
    }

}