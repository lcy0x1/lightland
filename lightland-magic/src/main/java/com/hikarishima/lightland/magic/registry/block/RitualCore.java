package com.hikarishima.lightland.magic.registry.block;

import com.hikarishima.lightland.magic.recipe.IMagicCraftRecipe;
import com.hikarishima.lightland.magic.recipe.MagicRecipeRegistry;
import com.hikarishima.lightland.magic.registry.MagicContainerRegistry;
import com.lcy0x1.base.BaseBlock;
import com.lcy0x1.base.BaseRecipe;
import com.lcy0x1.base.BlockProp;
import com.lcy0x1.base.proxy.block.IImpl;
import com.lcy0x1.base.proxy.block.IScheduleTick;
import com.lcy0x1.base.proxy.block.STE;
import com.lcy0x1.core.util.SerialClass;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class RitualCore {

    public static class Activate implements IScheduleTick {

        @Override
        public void tick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
            TileEntity te = world.getBlockEntity(pos);
            if (te instanceof TE) {
                ((TE) te).activate();
            }
        }

    }

    public static final IImpl CLICK = new RitualTE.RitualPlace();
    public static final IImpl ACTIVATE = new Activate();

    @SerialClass
    public static class TE extends RitualTE {

        public static final int[][] POS = {{-1, -1}, {-2, 0}, {-1, 1}, {0, -2}, {0, 2}, {1, -1}, {2, 0}, {1, 1}};

        public TE() {
            super(MagicContainerRegistry.TE_RITUAL_CORE);
        }

        public void activate() {
            if (level != null && !level.isClientSide()) {
                List<RitualSide.TE> list = new ArrayList<>();
                for (int[] dire : POS) {
                    TileEntity te = level.getBlockEntity(worldPosition.offset(dire[0], 0, dire[1]));
                    if (te instanceof RitualSide.TE) {
                        list.add((RitualSide.TE) te);
                    } else return;
                }
                Inv inv = new Inv(this, list);
                Optional<IMagicCraftRecipe<?>> r = level.getRecipeManager().getRecipeFor(MagicRecipeRegistry.RT_CRAFT, inv, level);
                r.ifPresent(e -> e.assemble(inv));
            }
        }

    }

    public static class Inv implements BaseRecipe.RecInv<IMagicCraftRecipe<?>> {

        private final RitualCore.TE core;
        private final List<RitualSide.TE> sides;

        private Inv(TE core, List<RitualSide.TE> sides) {
            this.core = core;
            this.sides = sides;
        }

        private SyncedSingleItemTE getSlot(int slot) {
            return slot < 5 ? sides.get(slot) : slot == 5 ? core : sides.get(slot - 1);
        }

        @Override
        public int getContainerSize() {
            return 0;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public ItemStack getItem(int slot) {
            return getSlot(slot).getItem(0);
        }

        @Override
        public ItemStack removeItem(int slot, int count) {
            return getSlot(slot).removeItem(0, count);
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            return getSlot(slot).removeItemNoUpdate(0);
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            getSlot(slot).setItem(0, stack);
        }

        @Override
        public void setChanged() {

        }

        @Override
        public boolean stillValid(PlayerEntity player) {
            return true;
        }

        @Override
        public void clearContent() {
            core.clearContent();
            for (RitualSide.TE te : sides)
                te.clearContent();
        }
    }

}
