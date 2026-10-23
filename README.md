# Additional Enchanted Miner Rescaled 

## This mod is a fork of Additional Enchanted Miner, which is a fork of Quarry Plus

This mod is a Cleanroom mod, and needs Scalar 3.x and the original CleanroomMC ModularUI 3.1.6 or newer.

This mod adds automatic miner. All machines can accept RF, MJ, EU and Forge Energy.

### Machines:

* **QuarryPlus** : It digs to y = 1 in the area of max 256 x 256 (blocks). Enchantments allow improving fuel efficiency
  and enchanted mining.
* **PumpPlus** : It adds QuarryPlus a function to pump liquid. You can make the Nether dry.
* **WorkbenchPlus** : You need this to make machines by this mod.
* **LaserPlus** : Use this instead of 32 BC's Laser. It improves performance of your PC and simplify wiring.
* **ChunkDestroyer** : It takes less than an hour to flatten the area of 16 x 16 (chunks).
* **Advanced Pump** : A pump faster than BC's one.
* **Spawner Controller** : Change the content of neighbor spawner.
* **Mini Quarry** : Uses real tools to harvest a marker-defined area. Redstone rising edges start/stop a scan; tool enchantments and durability apply. Insert tools or a fuel module in its five slots. The block supports Efficiency and Unbreaking enchantments.

ChunkDestroyer and Advanced Pump will increase the number of old GC and lag.

## Changes

- Port to Cleanroom w/ Java 25 + Scala 3.x.
- Migrate MCP to 39-1.12
- Replace machine and configuration screens with ModularUI.
- Backport Void Module: right-click it to register up to 18 item samples without consuming them, then install it in the new QuarryPlus or Chunk Destroyer. Matching items (including metadata and NBT) are discarded as they are collected. Multiple modules combine their filters.
- Backport Repeat Tick Module: install up to 64 in one module slot of the new QuarryPlus or Chunk Destroyer for up to 65 work passes per tick. Existing acceleration and energy costs still apply; fuel generation and item output run once per tick.
- Mini Quarry scans the inside of linked markers from top to bottom (two passes per layer). Its allow/deny slots copy block samples without consuming them: left-click to register, right-click to clear. Empty allow slots permit all harvestable blocks; deny entries take priority. Air, fluids and unbreakable blocks are always skipped. Starting another scan revisits the saved area, suitable for externally clocked farms. It buffers drops for adjacent inventories and exposes tools/output through item-handler capabilities.

## Credits

yogpstop - Original Mod 
kotori316 - Additional Enchanted Miner

Void Module and Repeat Tick Module textures from [Additional Enchanted Miner](https://github.com/Kotori316/QuarryPlus/tree/v21.1.165), licensed under LGPL-3.0.
Mini Quarry textures and the original Mini Quarry design from [Additional Enchanted Miner for 1.20.1](https://github.com/Kotori316/QuarryPlus/tree/1.20.1), licensed under LGPL-3.0.
