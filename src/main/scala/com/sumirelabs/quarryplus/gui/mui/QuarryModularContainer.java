package com.sumirelabs.quarryplus.gui.mui;

import com.cleanroommc.modularui.screen.ModularContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import com.yogpc.qp.container.ContainerWorkbench;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Native MUI slots and windows, with the existing crafting/transfer domain rules. */
public final class QuarryModularContainer extends ModularContainer {
    private final Container original;
    private boolean originalClosed;
    private final Map<IContainerListener, IContainerListener> listenerAdapters = new IdentityHashMap<>();

    public QuarryModularContainer(Container original) { this.original = original; }
    public Container original() { original.windowId = windowId; return original; }

    private int originalIndex(int slotId) {
        if (slotId < 0) return slotId;
        if (slotId >= inventorySlots.size()) return -1;
        Slot slot = inventorySlots.get(slotId);
        return slot instanceof QuarryModularSlot adapted ? adapted.originalIndex : -1;
    }

    @Override public ItemStack slotClick(int slotId, int button, ClickType type, EntityPlayer player) {
        if (!canInteractWith(player)) return ItemStack.EMPTY;
        return original().slotClick(originalIndex(slotId), button, type, player);
    }

    @Override public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        int mapped = originalIndex(index);
        return mapped < 0 ? ItemStack.EMPTY : original().transferStackInSlot(player, mapped);
    }

    @Override public void addListener(IContainerListener listener) {
        // Keep legacy progress and specialized large-stack synchronization working.
        original().addListener(listener);
        if (original instanceof ContainerWorkbench) {
            IContainerListener filtered = new IContainerListener() {
                @Override public void sendAllContents(Container container, NonNullList<ItemStack> stacks) {
                    // Source and recipe counts are full integers in TileMessage, not vanilla byte counts.
                    for (int i = 45; i < stacks.size(); i++) listener.sendSlotContents(container, i, stacks.get(i));
                }
                @Override public void sendSlotContents(Container container, int index, ItemStack stack) {
                    if (index >= 45) listener.sendSlotContents(container, index, stack);
                }
                @Override public void sendWindowProperty(Container container, int id, int value) {
                    listener.sendWindowProperty(container, id, value);
                }
                @Override public void sendAllWindowProperties(Container container, net.minecraft.inventory.IInventory inventory) {
                    listener.sendAllWindowProperties(container, inventory);
                }
            };
            listenerAdapters.put(listener, filtered);
            super.addListener(filtered);
        } else super.addListener(listener);
    }

    @Override public void removeListener(IContainerListener listener) {
        original.removeListener(listener);
        super.removeListener(listenerAdapters.getOrDefault(listener, listener));
        listenerAdapters.remove(listener);
    }

    @Override public void detectAndSendChanges() {
        original().detectAndSendChanges();
        super.detectAndSendChanges();
    }

    @SideOnly(Side.CLIENT)
    @Override public void updateProgressBar(int id, int value) { original().updateProgressBar(id, value); }

    @Override public void onModularContainerClosed() {
        super.onModularContainerClosed();
        if (!originalClosed) {
            originalClosed = true;
            original().onContainerClosed(getPlayer());
        }
    }

    public static Container unwrap(Container container) {
        return container instanceof QuarryModularContainer modular ? modular.original() : container;
    }
}
