package com.lcy0x1.base.block;

import com.lcy0x1.base.block.impl.TEPvd;
import com.lcy0x1.base.block.mult.*;
import com.lcy0x1.base.block.one.*;
import com.lcy0x1.base.block.type.IImpl;
import com.lcy0x1.base.block.type.IMultImpl;
import com.lcy0x1.base.block.type.IOneImpl;
import com.lcy0x1.base.block.type.STE;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.inventory.container.Container;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.state.StateContainer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;
import java.util.stream.Stream;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ImplBaseBlock extends BaseBlock {

    private static final ThreadLocal<BlockImplementor> TEMP = new ThreadLocal<>();

    private BlockImplementor impl;

    protected ImplBaseBlock(BlockProp p, IImpl... impl) {
        super(handler(construct(p).addImpls(impl)));
        registerDefaultState(this.impl.execute(IDefaultState.class).reduce(defaultBlockState(),
                (state, def) -> def.getDefaultState(state), (a, b) -> a));
    }

    public static BlockImplementor construct(BlockProp bb) {
        return new BlockImplementor(bb.getProps());
    }

    private static Properties handler(BlockImplementor bi) {
        if (TEMP.get() != null)
            throw new RuntimeException("concurrency error");
        TEMP.set(bi);
        return bi.props;
    }

    @Override
    public final boolean isSignalSource(BlockState bs) {
        return impl.one(IPower.class).isPresent();
    }

    @Override
    public final TileEntity createTileEntity(BlockState state, IBlockReader world) {
        return impl.one(ITE.class).map(e -> e.createTileEntity(state, world)).orElse(null);
    }

    @Override
    public final int getAnalogOutputSignal(BlockState blockState, World worldIn, BlockPos pos) {
        return impl.one(ITE.class).map(e -> Optional.ofNullable(worldIn.getBlockEntity(pos))
                .map(Container::getRedstoneSignalFromBlockEntity).orElse(0)).orElse(0);
    }

    @Override
    public final int getLightValue(BlockState bs, IBlockReader w, BlockPos pos) {
        return impl.one(ILight.class).map(e -> e.getLightValue(bs, w, pos))
                .orElse(super.getLightValue(bs, w, pos));
    }

    @Override
    public final BlockState getStateForPlacement(BlockItemUseContext context) {
        return impl.execute(IPlacement.class).reduce(defaultBlockState(),
                (state, impl) -> impl.getStateForPlacement(state, context), (a, b) -> a);
    }

    @Override
    public final int getSignal(BlockState bs, IBlockReader r, BlockPos pos, Direction d) {
        return impl.one(IPower.class)
                .map(e -> e.getSignal(bs, r, pos, d))
                .orElse(0);
    }

    @Override
    public final boolean hasTileEntity(BlockState state) {
        return impl.one(ITE.class).isPresent();
    }

    @Override
    public final BlockState mirror(BlockState state, Mirror mirrorIn) {
        return impl.one(IRotMir.class).map(e -> e.mirror(state, mirrorIn)).orElse(state);
    }

    @Override
    public final ActionResultType use(BlockState bs, World w, BlockPos pos, PlayerEntity pl, Hand h, BlockRayTraceResult r) {
        return impl.execute(IClick.class)
                .map(e -> e.onClick(bs, w, pos, pl, h, r))
                .filter(e -> e != ActionResultType.PASS)
                .findFirst().orElse(ActionResultType.PASS);
    }

    @Override
    public final void onRemove(BlockState state, World worldIn, BlockPos pos, BlockState newState, boolean isMoving) {
        if (impl.one(ITE.class).isPresent() && state.getBlock() != newState.getBlock()) {
            TileEntity tileentity = worldIn.getBlockEntity(pos);
            if (tileentity != null) {
                if (tileentity instanceof IInventory) {
                    InventoryHelper.dropContents(worldIn, pos, (IInventory) tileentity);
                    worldIn.updateNeighbourForOutputSignal(pos, this);
                }
                worldIn.removeBlockEntity(pos);
            }
        }
        impl.execute(IRep.class).forEach(e -> e.onReplaced(state, worldIn, pos, newState, isMoving));
    }

    @Override
    public final BlockState rotate(BlockState state, Rotation rot) {
        return impl.one(IRotMir.class).map(e -> e.rotate(state, rot)).orElse(state);
    }

    @Override
    protected final void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> builder) {
        impl = TEMP.get();
        TEMP.set(null);
        impl.execute(IState.class).forEach(e -> e.createBlockStateDefinition(builder));
    }

    @Override
    public final void neighborChanged(BlockState state, World world, BlockPos pos, Block nei_block, BlockPos nei_pos, boolean moving) {
        impl.execute(INeighborUpdate.class).forEach(e -> e.neighborChanged(this, state, world, pos, nei_block, nei_pos, moving));
        super.neighborChanged(state, world, pos, nei_block, nei_pos, moving);
    }

    @Override
    public final void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        impl.execute(IRandomTick.class).forEach(e -> e.randomTick(state, world, pos, random));
    }

    @Override
    public final void tick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        impl.execute(IScheduleTick.class).forEach(e -> e.tick(state, world, pos, random));
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void animateTick(BlockState state, World world, BlockPos pos, Random r) {
        impl.execute(IAnimateTick.class).forEach(e -> e.animateTick(state, world, pos, r));
    }

    public static class BlockImplementor {

        private final Properties props;
        private final List<IMultImpl> list = new ArrayList<>();
        private final HashMap<Class<?>, IOneImpl> map = new HashMap<>();

        public BlockImplementor(Properties p) {
            props = p;
        }

        public BlockImplementor addImpls(IImpl... impls) {
            for (IImpl impl : impls) {
                IImpl i = impl;
                if (i instanceof STE)
                    i = new TEPvd((STE) impl);
                if (i instanceof IMultImpl)
                    list.add((IMultImpl) i);
                if (i instanceof IOneImpl) {
                    IOneImpl one = (IOneImpl) i;
                    List<Class<?>> list = new ArrayList<>();
                    addOneImpl(one.getClass(), list);
                    for (Class<?> cls : list) {
                        if (map.containsKey(cls)) {
                            throw new RuntimeException("class " + cls + " is implemented twice with " + map.get(cls) + " and " + i);
                        } else {
                            map.put(cls, one);
                        }
                    }
                }
            }
            return this;
        }

        private void addOneImpl(Class<?> cls, List<Class<?>> list) {
            for (Class<?> ci : cls.getInterfaces()) {
                if (ci == IOneImpl.class) {
                    throw new RuntimeException("class " + cls + " should not implement IOneImpl directly");
                }
                if (IOneImpl.class.isAssignableFrom(ci)) {
                    Class<?>[] arr = ci.getInterfaces();
                    if (arr.length == 1 && arr[0] == IOneImpl.class) {
                        list.add(ci);
                    } else {
                        addOneImpl(ci, list);
                    }
                }
            }
        }

        @SuppressWarnings("unchecked")
        public <T extends IMultImpl> Stream<T> execute(Class<T> cls) {
            return list.stream().filter(cls::isInstance).map(e -> (T) e);
        }

        @SuppressWarnings("unchecked")
        public <T extends IOneImpl> Optional<T> one(Class<T> cls) {
            return Optional.ofNullable((T) map.get(cls));
        }

    }

}