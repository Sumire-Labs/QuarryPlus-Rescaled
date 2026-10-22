package com.yogpc.qp.modules

import com.yogpc.qp.tile.IModule
import net.minecraft.item.ItemStack

final class VoidModule(stack: ItemStack) extends IModule {
  private val samples = VoidFilter.read(stack)

  override def id: String = "quarryplus:module_void"
  override val calledWhen: Set[IModule.ModuleType] = Set.empty
  override def action(when: IModule.CalledWhen): IModule.Result = IModule.NoAction
  override def shouldVoid(drop: ItemStack): Boolean = VoidFilter.matches(samples, drop)
  override def toString: String = s"VoidModule($samples)"
}
