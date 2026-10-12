package com.sumirelabs.quarryplus.gui.mui;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.LongSyncValue;
import com.cleanroommc.modularui.value.sync.StringSyncValue;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import com.yogpc.qp.PowerManager;
import com.yogpc.qp.QuarryPlusI;
import com.yogpc.qp.gui.TranslationKeys;
import com.yogpc.qp.tile.*;
import net.minecraft.enchantment.Enchantment;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

final class CorePanels {
    private CorePanels() {}

    static ModularPanel build(MuiContext ctx) {
        int id = ctx.data.guiId;
        if (id == QuarryPlusI.guiIdQuarryModule()) return modules(ctx);
        if (id == QuarryPlusI.guiIdQuarryYLevel() || id == QuarryPlusI.guiIdQuarry2YLevel() || id == QuarryPlusI.guiIdAdvQuarryYLevel()) return level(ctx);
        if (id == QuarryPlusI.guiIdStatus()) return status(ctx);
        return null;
    }

    private static String name(MuiContext ctx) {
        return ctx.tile.getBlockType().getTranslationKey() + ".name";
    }

    private static ModularPanel modules(MuiContext ctx) {
        ModularPanel panel = MuiWidgets.panel(ctx, name(ctx), 176, 166);
        if (ctx.tile instanceof TileQuarry2 || ctx.tile instanceof TileAdvQuarry) {
            MuiWidgets.text(ctx, panel, "module_y", 120, 6, 40, () -> "Y: " + getY(ctx));
        }
        panel.child(IKey.lang(TranslationKeys.CONTAINER_INVENTORY).asWidget().pos(8, 72));
        MuiWidgets.slots(ctx, panel);
        return panel;
    }

    static int getY(MuiContext ctx) {
        if (ctx.tile instanceof TileQuarry2 tile) return tile.yLevel();
        if (ctx.tile instanceof TileAdvQuarry tile) return tile.yLevel();
        return ((TileBasic) ctx.tile).yLevel;
    }

    static int maxY(MuiContext ctx) {
        int ceiling = ctx.tile instanceof TileQuarry tile ? tile.yMin
                : ctx.tile instanceof TileQuarry2 tile ? tile.area().yMin() : ctx.tile.getPos().getY();
        return Math.max(0, Math.min(255, ceiling - 1));
    }

    static void setY(MuiContext ctx, int value) {
        int clamped = Math.max(0, Math.min(maxY(ctx), value));
        if (ctx.tile instanceof TileQuarry2 tile) tile.yLevel_$eq(clamped);
        else if (ctx.tile instanceof TileAdvQuarry tile) tile.yLevel_$eq(clamped);
        else ((TileBasic) ctx.tile).setYLevel(clamped);
        ctx.tile.markDirty();
    }

    private static ModularPanel level(MuiContext ctx) {
        ModularPanel panel = MuiWidgets.panel(ctx, name(ctx), 176, 166);
        IntSyncValue y = new IntSyncValue(() -> getY(ctx), value -> {
            if (!ctx.sync.isClient() && QuarryGuiFactory.canInteract(ctx)) setY(ctx, value);
        });
        IntSyncValue max = new IntSyncValue(() -> maxY(ctx));
        ctx.sync.syncValue("y_level", 0, y);
        ctx.sync.syncValue("y_max", 0, max);
        MuiWidgets.actionButton(ctx, panel, "y_plus", 68, 18, 40, 18, IKey.str("+"), packet -> {
            packet.readInt(); packet.readBoolean(); boolean ctrl = packet.readBoolean();
            setY(ctx, getY(ctx) + (ctrl ? 10 : 1));
        });
        panel.child(new TextFieldWidget().value(y).setNumbers(() -> 0, max::getIntValue).pos(60, 39).size(56, 16));
        MuiWidgets.actionButton(ctx, panel, "y_minus", 68, 58, 40, 18, IKey.str("-"), packet -> {
            packet.readInt(); packet.readBoolean(); boolean ctrl = packet.readBoolean();
            setY(ctx, getY(ctx) - (ctrl ? 10 : 1));
        });
        panel.child(IKey.lang(TranslationKeys.CONTAINER_INVENTORY).asWidget().pos(8, 72));
        MuiWidgets.slots(ctx, panel);
        return panel;
    }

    private static ModularPanel status(MuiContext ctx) {
        TileQuarry2 tile = (TileQuarry2) ctx.tile;
        ModularPanel panel = MuiWidgets.panel(ctx, name(ctx), 176, 226);
        // Sync only display values. Do not invoke the old client-only getStatusStrings on a server.
        AtomicInteger page = new AtomicInteger();
        Supplier<List<int[]>> enchantments = () -> {
            List<int[]> entries = new ArrayList<>();
            scala.collection.Iterator<?> iterator = TileQuarry2.getEnchantmentMap(tile.enchantments()).iterator();
            while (iterator.hasNext()) {
                scala.Tuple2<?, ?> entry = (scala.Tuple2<?, ?>) iterator.next();
                int level = ((Number) entry._2()).intValue();
                if (level > 0) entries.add(new int[]{((Number) entry._1()).intValue(), level});
            }
            entries.sort(Comparator.comparingInt(entry -> entry[0]));
            return entries;
        };
        panel.child(IKey.lang(TranslationKeys.ENCHANTMENT).asWidget().pos(8, 20).width(120));
        MuiWidgets.actionButton(ctx, panel, "enchantment_previous", 132, 18, 16, 13, IKey.str("<"), packet -> {
            packet.readInt(); packet.readBoolean(); packet.readBoolean();
            page.set(Math.max(0, page.get() - 1));
        });
        MuiWidgets.actionButton(ctx, panel, "enchantment_next", 150, 18, 16, 13, IKey.str(">"), packet -> {
            packet.readInt(); packet.readBoolean(); packet.readBoolean();
            if ((page.get() + 1) * 4 < enchantments.get().size()) page.incrementAndGet();
        });
        for (int i = 0; i < 4; i++) {
            final int row = i;
            StringSyncValue value = new StringSyncValue(() -> {
                List<int[]> entries = enchantments.get();
                int currentPage = Math.min(page.get(), Math.max(0, (entries.size() - 1) / 4));
                int index = currentPage * 4 + row;
                if (index >= entries.size()) return "";
                int[] entry = entries.get(index);
                return entry[0] + ":" + entry[1];
            });
            ctx.sync.syncValue("status_enchantment", i, value);
            panel.child(IKey.dynamic(() -> {
                String encoded = value.getStringValue();
                if (encoded.isEmpty()) return "";
                String[] parts = encoded.split(":", 2);
                Enchantment enchantment = Enchantment.getEnchantmentByID(Integer.parseInt(parts[0]));
                String label = enchantment == null ? parts[0] : IKey.lang(enchantment.getName()).get();
                return label + " " + IKey.lang("enchantment.level." + parts[1]).get();
            }).asWidget().pos(8, 33 + i * 13).width(158));
        }
        MuiWidgets.text(ctx, panel, "required_energy", 8, 87, 160, () -> {
            double amount = 2D * PowerManager.calcEnergyBreak(1.5F, (Integer) TileQuarry2.enchantmentMode().apply(tile.enchantments()), tile.enchantments().unbreaking())
                    / APowerTile.MJToMicroMJ * (tile.enchantments().efficiency() + 1);
            return amount + " FE/t";
        });
        LongSyncValue items = new LongSyncValue(tile::getStoredItemCount);
        LongSyncValue fluids = new LongSyncValue(tile::getStoredFluidAmount);
        ctx.sync.syncValue("stored_items", 0, items);
        ctx.sync.syncValue("stored_fluids", 0, fluids);
        panel.child(IKey.lang(() -> items.getLongValue() > 0 ? TranslationKeys.CONTAIN_ITEM : TranslationKeys.EMPTY_ITEM,
                () -> new Object[]{items.getLongValue()}).asWidget().pos(8, 103).width(160));
        panel.child(IKey.lang(() -> fluids.getLongValue() > 0 ? TranslationKeys.CONTAIN_FLUID : TranslationKeys.EMPTY_FLUID,
                () -> new Object[]{fluids.getLongValue()}).asWidget().pos(8, 118).width(160));
        panel.child(IKey.lang(TranslationKeys.CONTAINER_INVENTORY).asWidget().pos(8, 133));
        MuiWidgets.slots(ctx, panel);
        return panel;
    }
}
