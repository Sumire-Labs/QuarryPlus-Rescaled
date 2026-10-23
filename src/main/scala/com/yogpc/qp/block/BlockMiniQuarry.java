package com.yogpc.qp.block;

import com.sumirelabs.quarryplus.gui.mui.QuarryGuiFactory;
import com.yogpc.qp.QuarryPlus;
import com.yogpc.qp.QuarryPlusI;
import com.yogpc.qp.item.ItemBlockEnchantable;
import com.yogpc.qp.tile.IEnchantableTile;
import com.yogpc.qp.tile.TileMiniQuarry;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Enchantments;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class BlockMiniQuarry extends ADismCBlock {
    public BlockMiniQuarry() {
        super(Material.IRON, QuarryPlus.Names.miniQuarry, MiniItem::new);
        setHardness(1.5F); setResistance(10F);
        setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH).withProperty(ACTING, false));
    }
    @Override protected BlockStateContainer createBlockState() { return new BlockStateContainer(this, FACING, ACTING); }
    @Override public int getMetaFromState(IBlockState state) { return state.getValue(FACING).getIndex() | (state.getValue(ACTING) ? 8 : 0); }
    @Override public IBlockState getStateFromMeta(int meta) { return getDefaultState().withProperty(FACING, EnumFacing.byIndex(meta & 7)).withProperty(ACTING, (meta & 8) != 0); }
    @Override protected boolean canRotate() { return true; }
    @Override public TileEntity createNewTileEntity(World world, int meta) { return new TileMiniQuarry(); }

    @Override public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        if (!world.isRemote) {
            world.setBlockState(pos, state.withProperty(FACING, placer.getHorizontalFacing().getOpposite()), 2);
            if (world.getTileEntity(pos) instanceof TileMiniQuarry tile) {
                IEnchantableTile.Util.init(tile, stack.getEnchantmentTagList());
                tile.redstoneChanged(world.isBlockPowered(pos));
            }
        }
    }
    @Override public void neighborChanged(IBlockState state, World world, BlockPos pos, net.minecraft.block.Block block, BlockPos from) {
        if (!world.isRemote && world.getTileEntity(pos) instanceof TileMiniQuarry tile) tile.redstoneChanged(world.isBlockPowered(pos));
    }
    @Override public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player,
                                              EnumHand hand, EnumFacing side, float x, float y, float z) {
        if (super.onBlockActivated(world, pos, state, player, hand, side, x, y, z)) return true;
        if (player.isSneaking()) return false;
        QuarryGuiFactory.open(player, QuarryPlusI.guiIdMiniQuarry(), world, pos);
        return true;
    }
    @Override public void breakBlock(World world, BlockPos pos, IBlockState state) {
        if (!world.isRemote && world.getTileEntity(pos) instanceof TileMiniQuarry tile) tile.dropInventories();
        super.breakBlock(world, pos, state);
    }
    @Override public void harvestBlock(World world, EntityPlayer player, BlockPos pos, IBlockState state, TileEntity tile, ItemStack tool) {
        if (!world.isRemote && tile instanceof TileMiniQuarry mini) {
            ItemStack drop = new ItemStack(itemBlock());
            IEnchantableTile.Util.enchantmentToIS(mini, drop);
            spawnAsEntity(world, pos, drop);
        } else super.harvestBlock(world, player, pos, state, tile, tool);
    }
    @Override public void getDrops(net.minecraft.util.NonNullList<ItemStack> drops, net.minecraft.world.IBlockAccess world,
                                   BlockPos pos, IBlockState state, int fortune) {
        ItemStack stack = new ItemStack(itemBlock());
        if (world.getTileEntity(pos) instanceof TileMiniQuarry tile) IEnchantableTile.Util.enchantmentToIS(tile, stack);
        drops.add(stack);
    }

    private static final class MiniItem extends ItemBlockEnchantable {
        MiniItem(QPBlock block) { super(block); }
        @Override public boolean canMove(ItemStack stack, Enchantment enchantment) {
            return enchantment == Enchantments.EFFICIENCY || enchantment == Enchantments.UNBREAKING;
        }
    }
}
