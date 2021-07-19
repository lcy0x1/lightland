package com.hikarishima.lightland.registry.item;

import com.hikarishima.lightland.event.combat.ISpecialWeapon;
import com.hikarishima.lightland.event.combat.MagicDamageEntry;
import com.hikarishima.lightland.event.combat.MagicDamageSource;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

public class AntiMagicAxe extends AxeItem implements ISpecialWeapon {

    public final float prob, pen;

    public AntiMagicAxe(AntiMagicItemTier tier, int atk, float speed, Properties props) {
        super(tier, atk, speed, props);
        prob = tier.getProb() * 2;
        pen = tier.getPenetrate();
    }

    @Override
    public MagicDamageSource getSource(ItemStack stack, LivingHurtEvent event) {
        Entity e = event.getSource().getDirectEntity();
        if (e instanceof LivingEntity) {
            LivingEntity le = (LivingEntity) e;
            for (ItemStack armor : le.getArmorSlots()) {
                if (AntiMagicArmor.disenchant(event.getEntityLiving().level, armor, prob))
                    break;
            }
        }
        MagicDamageSource source = new MagicDamageSource(event.getSource().getDirectEntity());
        if (pen < 1) source.add(new MagicDamageEntry(event.getSource(), event.getAmount() * (1 - pen)));
        if (pen > 0) source.add(new MagicDamageEntry(event.getSource(), event.getAmount() * pen).setBypassMagic());
        return source;
    }
}
