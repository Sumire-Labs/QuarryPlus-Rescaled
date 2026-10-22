package com.yogpc.qp.modules;

import java.util.List;
import java.util.function.Function;
import com.sumirelabs.quarryplus.gui.mui.QuarryGuiFactory;
import com.yogpc.qp.QuarryPlus;
import com.yogpc.qp.QuarryPlusI;
import com.yogpc.qp.tile.APowerTile;
import com.yogpc.qp.tile.IModule;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import scala.Symbol;

public final class ItemVoidModule extends Item implements IModuleItem {
    public ItemVoidModule() {
        setRegistryName(QuarryPlus.modID, QuarryPlus.Names.voidModule);
        setTranslationKey(QuarryPlus.Names.voidModule);
        setCreativeTab(QuarryPlusI.creativeTab());
        setMaxStackSize(1);
    }

    @Override public Symbol getSymbol() { return Symbol.apply("ModuleVoid"); }

    @Override public <T extends APowerTile> Function<T, IModule> getModule(ItemStack stack) {
        return tile -> new VoidModule(stack);
    }

    @Override public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (!enabled() || hand != EnumHand.MAIN_HAND) return new ActionResult<>(EnumActionResult.PASS, stack);
        if (!world.isRemote) QuarryGuiFactory.open(player, QuarryPlusI.guiIdVoidModule(), world, player.getPosition());
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    @Override public EnumActionResult onItemUseFirst(EntityPlayer player, World world, BlockPos pos, EnumFacing side,
                                                    float hitX, float hitY, float hitZ, EnumHand hand) {
        return onItemRightClick(world, player, hand).getType();
    }

    @SideOnly(Side.CLIENT)
    @Override public void addInformation(ItemStack stack, World world, List<String> tooltip, ITooltipFlag flag) {
        tooltip.add(I18n.format("quarryplus.tooltip.void_module"));
        tooltip.add(I18n.format("quarryplus.tooltip.void_module_match"));
        List<ItemStack> samples = VoidFilter.read(stack);
        long count = samples.stream().filter(s -> !s.isEmpty()).count();
        tooltip.add(I18n.format("quarryplus.tooltip.void_module_count", count, VoidFilter.SIZE));
        samples.stream().filter(s -> !s.isEmpty()).forEach(s -> tooltip.add("  " + s.getDisplayName()));
    }
}
