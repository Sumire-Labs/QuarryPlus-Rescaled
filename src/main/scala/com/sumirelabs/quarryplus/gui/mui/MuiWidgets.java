package com.sumirelabs.quarryplus.gui.mui;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.value.sync.StringSyncValue;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import com.yogpc.qp.gui.TranslationKeys;
import net.minecraft.inventory.Slot;
import net.minecraft.network.PacketBuffer;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class MuiWidgets {
    private MuiWidgets() {}

    public static ModularPanel panel(MuiContext ctx, String title, int width, int height) {
        ModularPanel panel = ModularPanel.defaultPanel("quarryplus_" + ctx.data.guiId, width, height);
        panel.child(IKey.lang(title).asWidget().pos(8, 6).width(width - 28));
        panel.child(ButtonWidget.panelCloseButton());
        return panel;
    }

    public static void slots(MuiContext ctx, ModularPanel panel) {
        for (int index = 0; index < ctx.legacy.inventorySlots.size(); index++) {
            Slot source = ctx.legacy.inventorySlots.get(index);
            QuarryModularSlot adapted = new QuarryModularSlot(source, index);
            if (source.inventory == ctx.player.inventory)
                adapted.slotGroup(com.cleanroommc.modularui.widgets.slot.PlayerSlotGroup.NAME);
            ItemSlot widget = new ItemSlot().slot(adapted).pos(source.xPos - 1, source.yPos - 1);
            if (ctx.legacy instanceof com.yogpc.qp.container.ContainerWorkbench && index >= 27 && index < 45)
                widget.background(InventoryPanels.recipeBackground(ctx, index - 27));
            panel.child(widget);
        }
    }

    public static void text(MuiContext ctx, ModularPanel panel, String name, int x, int y, int width, Supplier<String> getter) {
        StringSyncValue value = new StringSyncValue(getter);
        ctx.sync.syncValue(name, 0, value);
        panel.child(IKey.dynamic(value::getStringValue).asWidget().pos(x, y).width(width));
    }

    /** Sync keys and arguments so each client renders text in its own language. */
    @SafeVarargs
    public static void localizedText(MuiContext ctx, ModularPanel panel, String name, int x, int y, int width,
                                     Supplier<String> key, Supplier<String>... arguments) {
        StringSyncValue translation = new StringSyncValue(key);
        ctx.sync.syncValue(name, 0, translation);
        StringSyncValue[] values = new StringSyncValue[arguments.length];
        for (int i = 0; i < arguments.length; i++) {
            values[i] = new StringSyncValue(arguments[i]);
            ctx.sync.syncValue(name, i + 1, values[i]);
        }
        panel.child(IKey.lang(translation::getStringValue, () -> {
            Object[] args = new Object[values.length];
            for (int i = 0; i < values.length; i++) args[i] = values[i].getStringValue();
            return args;
        }).asWidget().pos(x, y).width(width));
    }

    public static void button(MuiContext ctx, ModularPanel panel, String name, int x, int y,
                              int width, int height, String label, Consumer<PacketBuffer> action) {
        actionButton(ctx, panel, name, x, y, width, height, IKey.lang(label), action);
    }

    public static void actionButton(MuiContext ctx, ModularPanel panel, String name, int x, int y,
                                    int width, int height, IKey label, Consumer<PacketBuffer> action) {
        ctx.sync.registerSyncedAction(name, false, true, packet -> {
            if (QuarryGuiFactory.canInteract(ctx)) action.accept(packet);
        });
        panel.child(new ButtonWidget<>().name(name).pos(x, y).size(width, height).overlay(label)
                .onMousePressed(mouseButton -> {
                    if (mouseButton != 0 && mouseButton != 1) return false;
                    ctx.sync.callSyncedAction(name, packet -> MuiClientInput.write(packet, mouseButton));
                    return true;
                }));
    }
}
