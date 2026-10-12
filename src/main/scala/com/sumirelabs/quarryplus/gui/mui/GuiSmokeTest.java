package com.sumirelabs.quarryplus.gui.mui;

import com.cleanroommc.modularui.screen.GuiContainerWrapper;
import com.yogpc.qp.QuarryPlus;
import com.yogpc.qp.QuarryPlusI;
import com.yogpc.qp.tile.TileWorkbench;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Opt-in development smoke test: creates its own disposable world and captures every UI. */
@SideOnly(Side.CLIENT)
public final class GuiSmokeTest {
    private static final int[] IDS = {1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,100,101,102,103,104,105,120,121,122,123,124,125,140};
    private int stage;
    private int index;
    private int ticks;
    private boolean pending;
    private boolean requested;
    private int interactionStage;
    private java.util.concurrent.Future<?> interactionTask;
    private java.util.concurrent.Future<?> worldTask;

    public static void install() {
        if (Boolean.getBoolean("quarryplus.guiSmokeTest") && Boolean.TRUE.equals(net.minecraft.launchwrapper.Launch.blackboard.get("fml.deobfuscatedEnvironment")))
            MinecraftForge.EVENT_BUS.register(new GuiSmokeTest());
    }

    @SubscribeEvent
    public void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        try {
            if (stage == 0) {
                if (!(mc.currentScreen instanceof GuiMainMenu)) return;
                com.cleanroommc.modularui.ModularUIConfig.guiDebugMode = false;
                stage = 1;
                WorldSettings settings = new WorldSettings(1, GameType.CREATIVE, false, false, WorldType.FLAT).enableCommands();
                mc.launchIntegratedServer("quarryplus-ui-smoke-" + System.currentTimeMillis(), "QuarryPlus UI Smoke", settings);
                return;
            }
            if (mc.player == null || mc.world == null || mc.getIntegratedServer() == null) return;
            if (stage == 1) {
                if (++ticks < 40) return;
                stage = 2;
                ticks = 0;
            }
            if (stage == 2 && !pending) {
                if (index >= IDS.length) {
                    mc.player.closeScreen();
                    mc.displayGuiScreen(MuiConfigScreen.create(null));
                    stage = 4;
                    ticks = 0;
                    return;
                }
                int id = IDS[index];
                pending = true;
                requested = false;
                interactionStage = 0;
                IntegratedServer server = mc.getIntegratedServer();
                java.util.UUID uuid = mc.player.getUniqueID();
                worldTask = server.addScheduledTask(() -> open(server, uuid, id));
                stage = 3;
                ticks = 0;
                return;
            }
            if (stage == 3) {
                if (++ticks > 200) throw new IllegalStateException("Timed out opening GUI " + IDS[index]);
                if (worldTask != null) {
                    if (!worldTask.isDone()) return;
                    worldTask.get(); worldTask = null;
                }
                if (interactionStage != 0) {
                    if (interactionTask != null) {
                        if (!interactionTask.isDone()) return;
                        interactionTask.get(); interactionTask = null;
                    }
                    if (ticks < 10) return;
                    int id = IDS[index];
                    if (interactionStage == 1) {
                        ScreenShotHelper.saveScreenshot(mc.gameDir, "quarryplus-ui-" + id + ".png", mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
                        GuiSmokeInteractions.click(mc, id);
                        interactionStage = 2; ticks = 0;
                    } else if (interactionStage == 2) {
                        java.util.UUID uuid = mc.player.getUniqueID();
                        IntegratedServer server = mc.getIntegratedServer();
                        interactionTask = server.addScheduledTask(() -> GuiSmokeInteractions.verify(server.getPlayerList().getPlayerByUUID(uuid), id));
                        interactionStage = 3; ticks = 0;
                    } else {
                        QuarryPlus.LOGGER.info("MUI smoke passed GUI {} with interaction assertions", id);
                        index++; pending = false; stage = 2;
                    }
                    return;
                }
                if (!requested && ticks >= 10 && !(mc.currentScreen instanceof GuiContainerWrapper)
                        && QuarryGuiFactory.INSTANCE.canInteractWith(mc.player,
                        new QuarryGuiData(mc.player, new BlockPos(2, 64, 2), IDS[index]))) {
                    // Let chunk, tile and held-item packets arrive before requesting the window.
                    requested = true;
                    QuarryGuiFactory.open(mc.player, IDS[index], mc.world, new BlockPos(2, 64, 2));
                }
                if (!(mc.currentScreen instanceof GuiContainerWrapper) || !(mc.player.openContainer instanceof QuarryModularContainer container)
                        || !(container.getGuiData() instanceof QuarryGuiData data) || data.guiId != IDS[index] || ticks < 20) return;
                if (IDS[index] == 1) {
                    TileWorkbench tile = (TileWorkbench) mc.world.getTileEntity(new BlockPos(2, 64, 2));
                    if (tile == null || tile.getStackInSlot(0).getCount() != 1500)
                        throw new IllegalStateException("Workbench large-stack synchronization lost the count");
                }
                int id = IDS[index];
                java.util.UUID uuid = mc.player.getUniqueID();
                IntegratedServer server = mc.getIntegratedServer();
                interactionTask = server.addScheduledTask(() -> GuiSmokeInteractions.prepare(server.getPlayerList().getPlayerByUUID(uuid), id));
                interactionStage = 1; ticks = 0;
            } else if (stage == 4 && ++ticks >= 20) {
                ScreenShotHelper.saveScreenshot(mc.gameDir, "quarryplus-ui-config.png", mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
                GuiSmokeInteractions.configuration(mc);
                QuarryPlus.LOGGER.info("MUI smoke passed all {} machine/auxiliary screens and configuration", IDS.length);
                stage = 5;
                mc.shutdown();
            }
        } catch (Throwable error) {
            stage = 5;
            QuarryPlus.LOGGER.error("MUI smoke failed at GUI index " + index, error);
            mc.shutdown();
        }
    }

    private static void open(IntegratedServer server, java.util.UUID uuid, int id) {
        EntityPlayerMP player = server.getPlayerList().getPlayerByUUID(uuid);
        if (player == null) throw new IllegalStateException("Smoke player not connected");
        net.minecraft.world.WorldServer world = player.getServerWorld();
        BlockPos pos = new BlockPos(2, 64, 2);
        player.closeScreen();
        for (int x = -1; x <= 4; x++) for (int z = -1; z <= 4; z++) world.setBlockState(new BlockPos(x, 63, z), Blocks.STONE.getDefaultState());
        player.connection.setPlayerLocation(0.5, 64, 0.5, 0, 0);
        Block block;
        if (id == 1) block = QuarryPlusI.blockWorkbench();
        else if (id == 2) block = QuarryPlusI.blockMover();
        else if (id == 3 || id == 4 || id == 10) block = QuarryPlusI.blockQuarry();
        else if (id == 5) block = QuarryPlusI.blockPlacer();
        else if (id == 6 || id == 11 || id >= 120 && id < 126) block = QuarryPlusI.blockChunkDestroyer();
        else if (id == 7) block = QuarryPlusI.blockStandalonePump();
        else if (id == 8) block = QuarryPlusI.blockBookMover();
        else if (id == 9) block = QuarryPlusI.blockSolidQuarry();
        else if (id == 16) block = QuarryPlusI.blockFiller();
        else if (id >= 100 && id < 106) block = QuarryPlusI.blockPump();
        else if (id == 140) block = QuarryPlusI.blockController();
        else block = QuarryPlusI.blockQuarry2();
        world.setBlockToAir(pos);
        world.setBlockState(pos, block.getDefaultState());
        if (id == 12) {
            player.inventory.currentItem = 0;
            player.inventory.setInventorySlotContents(0, new ItemStack(QuarryPlusI.itemTemplate()));
        }
        if (id == 1) ((TileWorkbench) world.getTileEntity(pos)).setInventorySlotContents(0, new ItemStack(Items.IRON_INGOT, 1500));
        // A newly placed test block must reach the client before MUI constructs its panel.
        player.connection.sendPacket(new net.minecraft.network.play.server.SPacketBlockChange(world, pos));
        net.minecraft.tileentity.TileEntity tile = world.getTileEntity(pos);
        if (tile != null && tile.getUpdatePacket() != null) player.connection.sendPacket(tile.getUpdatePacket());
    }
}
