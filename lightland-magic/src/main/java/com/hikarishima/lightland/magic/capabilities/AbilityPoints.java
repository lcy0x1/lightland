package com.hikarishima.lightland.magic.capabilities;

import com.hikarishima.lightland.magic.IAbilityPoints;
import com.hikarishima.lightland.magic.profession.Profession;
import com.lcy0x1.core.util.SerialClass;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

@SerialClass
public class AbilityPoints implements IAbilityPoints {

    public static final int MAX_LV = 20;

    public static int expRequirement(int lv) {
        return (int) Math.round(100 * Math.pow(1.5, lv));
    }

    private final MagicHandler parent;
    @SerialClass.SerialField
    public int general, body, magic, element, arcane;
    @SerialClass.SerialField
    public int health, strength, speed, level, exp;

    @SerialClass.SerialField
    public Profession profession = null;

    AbilityPoints(MagicHandler parent) {
        this.parent = parent;
    }

    public boolean canLevelArcane() {
        return arcane > 0 || magic > 0 || general > 0;
    }

    public boolean canLevelMagic() {
        return magic > 0 || general > 0;
    }

    public boolean canLevelBody() {
        return body > 0 || general > 0;
    }

    public boolean canLevelElement() {
        return element > 0;
    }

    public boolean levelArcane() {
        if (arcane > 0) arcane--;
        else if (magic > 0) magic--;
        else if (general > 0) general--;
        else return false;
        return true;
    }

    public boolean levelMagic() {
        if (magic > 0) magic--;
        else if (general > 0) general--;
        else return false;
        return true;
    }

    public boolean levelBody() {
        if (body > 0) body--;
        else if (general > 0) general--;
        else return false;
        return true;
    }

    public void levelElement() {
        if (element > 0) element--;
    }

    public Profession getProfession() {
        return profession;
    }

    public boolean setProfession(Profession prof) {
        if (profession != null) {
            return false;
        }
        profession = prof;
        prof.init(parent);
        addExp(0);
        updateAttribute();
        return true;
    }

    @Override
    public void addExp(int xp) {
        if (xp < 0)
            return;
        exp += xp;
        while (profession != null && level < MAX_LV && exp >= expRequirement(level)) {
            exp -= expRequirement(level);
            level++;
            profession.levelUp(parent);
        }
    }

    public void updateAttribute() {
        BodyAttribute.resetModifiers(this, parent.player);
    }

    public enum LevelType {
        HEALTH((h) -> h.abilityPoints.canLevelBody(), (h) -> {
            if (h.abilityPoints.levelBody())
                h.abilityPoints.health++;
        }, h -> h.abilityPoints.health),
        STRENGTH((h) -> h.abilityPoints.canLevelBody(), (h) -> {
            if (h.abilityPoints.levelBody())
                h.abilityPoints.strength++;
        }, h -> h.abilityPoints.strength),
        SPEED((h) -> h.abilityPoints.canLevelBody(), (h) -> {
            if (h.abilityPoints.levelBody())
                h.abilityPoints.speed++;
        }, h -> h.abilityPoints.speed),
        MANA((h) -> h.abilityPoints.canLevelMagic(), (h) -> {
            if (h.abilityPoints.levelMagic())
                h.magicAbility.magic_level++;
        }, h -> h.magicAbility.magic_level),
        SPELL((h) -> h.abilityPoints.canLevelMagic(), (h) -> {
            if (h.abilityPoints.levelMagic())
                h.magicAbility.spell_level++;
        }, h -> h.magicAbility.spell_level);

        private final Predicate<MagicHandler> check;
        private final Consumer<MagicHandler> run;
        public final Function<MagicHandler, Integer> level;

        LevelType(Predicate<MagicHandler> check, Consumer<MagicHandler> run, Function<MagicHandler, Integer> level) {
            this.check = check;
            this.run = run;
            this.level = level;
        }

        public String checkLevelUp(MagicHandler handler) {
            if (!check.test(handler)) {
                return "screen.ability.ability.error.no_point";
            }
            Profession prof = handler.abilityPoints.getProfession();
            if (prof == null) {
                return "screen.ability.ability.error.no_prof";
            } else {
                return prof.allowLevel(this, handler);
            }
        }

        public void doLevelUp(MagicHandler handler) {
            if (checkLevelUp(handler) != null)
                return;
            run.accept(handler);
            handler.abilityPoints.updateAttribute();
        }

    }

}
