package com.sumirelabs.quarryplus.gui.mui

import com.yogpc.qp.{Config, PowerManager, QuarryPlusI}
import com.yogpc.qp.modules.RepeatTickModule
import com.yogpc.qp.tile.{EnergyUsage, IModule, QuarryAction, TileAdvQuarry, TileQuarry2}
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.item.ItemStack
import net.minecraft.util.math.BlockPos

/** Only called by the opt-in development smoke world. */
object RepeatTickSmokeChecks {
  def verify(player: EntityPlayerMP, quarry: TileQuarry2): Unit = {
    val inventory = quarry.moduleInv
    val saved = inventory.serializeNBT()
    inventory.deserializeNBT(saved)
    assert(inventory.getStackInSlot(0).getCount == 64, "Repeat stack count was lost in NBT")
    assert(!inventory.isItemValidForSlot(2, new ItemStack(QuarryPlusI.repeatTickModule)), "Duplicate repeat slot accepted")

    val originalAction = quarry.action
    val originalTarget = quarry.target
    def run(count: Int, energy: Long, expected: Int): Unit = {
      inventory.setInventorySlotContents(0, if (count == 0) ItemStack.EMPTY else new ItemStack(QuarryPlusI.repeatTickModule, count))
      var work = 0
      var moduleTicks = 0
      val tickProbe = new IModule {
        override def id = "quarryplus:test_tick_probe"
        override val calledWhen: Set[IModule.ModuleType] = Set(IModule.TypeTick)
        override def action(when: IModule.CalledWhen): IModule.Result = {
          moduleTicks += 1
          IModule.NoAction
        }
        override def toString = id
      }
      quarry.modules = quarry.modules :+ tickProbe
      quarry.target = quarry.getPos.east()
      quarry.action = new QuarryAction {
        override def action(target: BlockPos): Unit = {
          if (PowerManager.useEnergy(quarry, 100L, EnergyUsage.BREAK_BLOCK)) work += 1
        }
        override def nextTarget() = quarry.getPos.east()
        override def nextAction(q: TileQuarry2): QuarryAction = this
        override def canGoNext(q: TileQuarry2) = false
        override def mode = TileQuarry2.breakBlock
      }
      quarry.configure(100000L, 100000L)
      quarry.setStoredEnergy(energy)
      quarry.update()
      assert(work == expected, s"Expected $expected work passes, got $work")
      assert(moduleTicks == 1, s"Module tick ran $moduleTicks times in one real tick")
      assert(quarry.getStoredEnergy == energy - expected * 100L, "Repeat work bypassed energy costs")
    }
    try {
      assert(!Config.content.noEnergy && !Config.content.fastQuarryHeadMove, "Smoke world must use default energy/head settings")
      run(0, 100000L, 1)
      run(1, 100000L, 2)
      run(64, 100000L, 65)
      run(64, 800L, 8)
    } finally {
      quarry.action = originalAction
      quarry.target = originalTarget
      inventory.deserializeNBT(saved)
    }

    val cdPos = quarry.getPos.east(2)
    val world = player.getServerWorld
    world.setBlockState(cdPos, QuarryPlusI.blockChunkDestroyer.getDefaultState)
    val cd = world.getTileEntity(cdPos).asInstanceOf[TileAdvQuarry]
    cd.moduleInv.setInventorySlotContents(0, new ItemStack(QuarryPlusI.repeatTickModule, 64))
    cd.moduleInv.deserializeNBT(cd.moduleInv.serializeNBT())
    assert(RepeatTickModule.workLimit(1, cd.modules) == 65, "Chunk Destroyer did not load its repeat stack")
    assert(!cd.moduleInv.isItemValidForSlot(1, new ItemStack(QuarryPlusI.repeatTickModule)), "Chunk Destroyer accepted duplicate repeat slots")

    val range = new TileAdvQuarry.DigRange(0, 70, 0, 11, 70, 11)
    val frameTargets = TileAdvQuarry.getFramePoses(range)
    def frames(count: Int): Int = {
      frameTargets.foreach(world.setBlockToAir)
      cd.digRange = range
      cd.framePoses = Nil
      cd.chunks = Nil
      cd.target = cdPos
      cd.moduleInv.setInventorySlotContents(0, if (count == 0) ItemStack.EMPTY else new ItemStack(QuarryPlusI.repeatTickModule, count))
      cd.mode.set(TileAdvQuarry.MAKE_FRAME)
      cd.configure(1000000000000L, 1000000000000L)
      cd.setStoredEnergy(1000000000000L)
      cd.update()
      frameTargets.count(p => world.getBlockState(p).getBlock == QuarryPlusI.blockFrame)
    }
    assert(frames(0) == 4, "Chunk Destroyer's baseline frame budget changed")
    assert(frames(1) == 8, "Repeat module did not accelerate Chunk Destroyer work")
    frameTargets.foreach(world.setBlockToAir)
    cd.mode.set(TileAdvQuarry.NONE)
  }
}
