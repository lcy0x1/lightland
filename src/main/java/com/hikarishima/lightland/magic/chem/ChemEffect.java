package com.hikarishima.lightland.magic.chem;

import com.hikarishima.lightland.config.Translator;
import com.hikarishima.lightland.magic.MagicRegistry;
import com.hikarishima.lightland.magic.capabilities.MagicHandler;
import com.hikarishima.lightland.recipe.IMagicRecipe;
import com.lcy0x1.core.util.SerialClass;
import net.minecraft.potion.Effect;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.registries.ForgeRegistries;

@SerialClass
public class ChemEffect extends ChemObj<ChemEffect, Effect> {

    @SerialClass.SerialField
    public int lv;

    public ChemEffect() {
        super(State.LIQUID);
    }

    @Override
    public Effect get() {
        if (!ForgeRegistries.POTIONS.containsKey(id))
            return null;
        return ForgeRegistries.POTIONS.getValue(id);
    }

    @Override
    public boolean known(MagicHandler handler) {
        IMagicRecipe<?> r = IMagicRecipe.getMap(handler.world, MagicRegistry.MPT_EFF).get(get());
        return r == null || handler.magicHolder.getProduct(r).usable();
    }

    @Override
    public ITextComponent getDesc() {
        return new TranslationTextComponent(get().getDescriptionId()).append(" " + Translator.getNumber(lv + 1));
    }
}
