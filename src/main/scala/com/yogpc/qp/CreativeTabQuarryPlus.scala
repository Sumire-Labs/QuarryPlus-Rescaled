package com.yogpc.qp

import net.minecraft.creativetab.CreativeTabs
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.relauncher.{Side, SideOnly}

class CreativeTabQuarryPlus extends CreativeTabs(QuarryPlus.Mod_Name) {

  @SideOnly(Side.CLIENT)
  override def createIcon = new ItemStack(QuarryPlusI.blockQuarry)

}
