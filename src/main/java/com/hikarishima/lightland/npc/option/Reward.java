package com.hikarishima.lightland.npc.option;

import com.lcy0x1.core.util.SerialClass;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

@SerialClass
public class Reward implements IOptionComponent {

    @SerialClass.SerialField
    public ItemStack[] item;

    @SerialClass.SerialField
    public int vanilla_exp;

    @SerialClass.SerialField
    public int lightland_exp;

    public void perform(PlayerEntity player) {
        //TODO sidedness
        player.giveExperiencePoints(vanilla_exp);
        for (ItemStack stack : item)
            player.addItem(stack);
    }

}
