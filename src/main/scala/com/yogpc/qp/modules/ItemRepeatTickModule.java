package com.yogpc.qp.modules;

import java.util.List;
import java.util.function.Function;
import com.yogpc.qp.QuarryPlus;
import com.yogpc.qp.QuarryPlusI;
import com.yogpc.qp.tile.APowerTile;
import com.yogpc.qp.tile.IModule;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import scala.Symbol;

public final class ItemRepeatTickModule extends Item implements IModuleItem {
    public ItemRepeatTickModule() {
        setRegistryName(QuarryPlus.modID, QuarryPlus.Names.repeatTickModule);
        setTranslationKey(QuarryPlus.Names.repeatTickModule);
        setCreativeTab(QuarryPlusI.creativeTab());
        setMaxStackSize(RepeatTickBudget.MAX_MODULES);
    }

    @Override public Symbol getSymbol() { return Symbol.apply("ModuleRepeatTick"); }

    @Override public <T extends APowerTile> Function<T, IModule> getModule(ItemStack stack) {
        int count = Math.max(0, Math.min(RepeatTickBudget.MAX_MODULES, stack.getCount()));
        return tile -> new RepeatTickModule(count);
    }

    @SideOnly(Side.CLIENT)
    @Override public void addInformation(ItemStack stack, World world, List<String> tooltip, ITooltipFlag flag) {
        tooltip.add(I18n.format("quarryplus.tooltip.repeat_tick_module", RepeatTickBudget.repetitions(stack.getCount())));
        tooltip.add(I18n.format("quarryplus.tooltip.repeat_tick_energy"));
    }
}
