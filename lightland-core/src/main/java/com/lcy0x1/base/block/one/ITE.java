package com.lcy0x1.base.block.one;

import com.lcy0x1.base.block.type.IOneImpl;
import net.minecraft.block.BlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockReader;

public interface ITE extends IOneImpl {
    TileEntity createTileEntity(BlockState state, IBlockReader world);
}