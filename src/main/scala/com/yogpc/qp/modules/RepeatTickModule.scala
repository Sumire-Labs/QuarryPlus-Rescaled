package com.yogpc.qp.modules

import com.yogpc.qp.tile.IModule

final class RepeatTickModule(val stackSize: Int) extends IModule {
  override def id: String = "quarryplus:module_repeat_tick"
  override val calledWhen: Set[IModule.ModuleType] = Set.empty
  override def action(when: IModule.CalledWhen): IModule.Result = IModule.NoAction
  override def toString: String = s"RepeatTickModule($stackSize)"
}

object RepeatTickModule {
  def count(modules: List[IModule]): Int =
    modules.collectFirst { case module: RepeatTickModule => module.stackSize }.getOrElse(0)

  def workLimit(base: Int, modules: List[IModule]): Int = RepeatTickBudget.workLimit(base, count(modules))
}
