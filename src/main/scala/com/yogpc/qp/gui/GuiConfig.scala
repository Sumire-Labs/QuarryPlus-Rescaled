package com.yogpc.qp.gui

import com.yogpc.qp.{Config, QuarryPlus}
import net.minecraft.client.resources.I18n
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiScreen
import net.minecraftforge.fml.client.IModGuiFactory
import net.minecraftforge.fml.relauncher.{Side, SideOnly}

@SideOnly(Side.CLIENT)
class GuiConfig(parent: GuiScreen) extends net.minecraftforge.fml.client.config.GuiConfig(
  parent, Config.getElements, QuarryPlus.modID, false, false, I18n.format("qp.mui.config.title")
)

@SideOnly(Side.CLIENT)
class GuiFactory extends IModGuiFactory {
  override def createConfigGui(parentScreen: GuiScreen): GuiScreen = com.sumirelabs.quarryplus.gui.mui.MuiConfigScreen.create(parentScreen)

  override def hasConfigGui: Boolean = true

  override def runtimeGuiCategories() = null

  override def initialize(minecraftInstance: Minecraft): Unit = ()
}
