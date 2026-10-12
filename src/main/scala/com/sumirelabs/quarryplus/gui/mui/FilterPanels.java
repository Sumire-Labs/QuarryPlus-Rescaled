package com.sumirelabs.quarryplus.gui.mui;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.value.sync.StringSyncValue;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import com.yogpc.qp.Config;
import com.yogpc.qp.QuarryPlusI;
import com.yogpc.qp.block.BlockController;
import com.yogpc.qp.container.ContainerListTemplate;
import com.yogpc.qp.gui.TranslationKeys;
import com.yogpc.qp.item.ItemTemplate;
import com.yogpc.qp.packet.pump.Mappings;
import com.yogpc.qp.tile.TileBasic;
import com.yogpc.qp.tile.TilePump;
import com.yogpc.qp.utils.BlockData;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

/** Server-owned list editors. Selection, pagination and confirmation never trust client indices. */
public final class FilterPanels {
    public static final int PUMP_ID = 100;
    public static final int CONTROLLER_ID = 140;

    private FilterPanels() {}

    private static String blockLabel(BlockData data) {
        return data.getLocalizedName() + " (" + data + ")";
    }

    private static String fluidLabel(String name) {
        net.minecraftforge.fluids.Fluid fluid = FluidRegistry.getFluid(name);
        return fluid == null ? name : fluid.getLocalizedName(new net.minecraftforge.fluids.FluidStack(fluid, 1)) + " (" + name + ")";
    }

    public static ModularPanel build(MuiContext ctx) {
        int id = ctx.data.guiId;
        if ((id == QuarryPlusI.guiIdFList() || id == QuarryPlusI.guiIdSList()) && ctx.tile instanceof TileBasic)
            return enchantment(ctx, id == QuarryPlusI.guiIdFList());
        if (id == QuarryPlusI.guiIdListTemplate()) return template(ctx);
        if (id >= PUMP_ID && id < PUMP_ID + 6 && ctx.tile instanceof TilePump)
            return pump(ctx, EnumFacing.byIndex(id - PUMP_ID));
        if (id == CONTROLLER_ID) return controller(ctx);
        return null;
    }

    private static void literal(MuiContext c, ModularPanel p, String id, int x, int y, int w, String label, Runnable action) {
        MuiWidgets.actionButton(c, p, id, x, y, w, 18, IKey.str(label), b -> action.run());
    }

    private static void button(MuiContext c, ModularPanel p, String id, int x, int y, int w, String key, Runnable action) {
        MuiWidgets.button(c, p, id, x, y, w, 18, key, b -> action.run());
    }

    /** Fixed rows keep the widget tree identical while server lists change in size. */
    private static void rows(MuiContext c, ModularPanel p, String prefix, int x, int y, int w, int count,
                             Supplier<List<String>> source, Consumer<String> select) {
        int[] page = {0};
        for (int row = 0; row < count; row++) {
            final int r = row;
            MuiWidgets.text(c, p, prefix + "Text" + row, x + 22, y + row * 20 + 4, w - 22, () -> {
                List<String> items = source.get();
                page[0] = Math.min(page[0], Math.max(0, (items.size() - 1) / count));
                int i = page[0] * count + r;
                return i < items.size() ? items.get(i) : "";
            });
            literal(c, p, prefix + "Select" + row, x, y + row * 20, 20, ">", () -> {
                List<String> items = source.get();
                int i = page[0] * count + r;
                if (i >= 0 && i < items.size()) select.accept(items.get(i));
            });
        }
        literal(c, p, prefix + "Previous", x, y + count * 20, 30, "<", () -> page[0] = Math.max(0, page[0] - 1));
        MuiWidgets.text(c, p, prefix + "Page", x + 34, y + count * 20 + 4, w - 68,
                () -> (page[0] + 1) + " / " + Math.max(1, (source.get().size() + count - 1) / count));
        literal(c, p, prefix + "Next", x + w - 30, y + count * 20, 30, ">", () -> {
            if ((page[0] + 1) * count < source.get().size()) page[0]++;
        });
    }

    private static ModularPanel enchantment(MuiContext c, boolean fortune) {
        TileBasic tile = (TileBasic) c.tile;
        List<BlockData> data = fortune ? tile.fortuneList : tile.silktouchList;
        ModularPanel p = MuiWidgets.panel(c, fortune ? "enchantment.lootBonusDigger" : "enchantment.untouching", 330, 240);
        BlockData[] selected = {null};
        BlockData[] confirmation = {null};
        rows(c, p, "blocks", 8, 26, 310, 6, () -> data.stream().map(FilterPanels::blockLabel).collect(Collectors.toList()),
                name -> { selected[0] = data.stream().filter(b -> blockLabel(b).equals(name)).findFirst().orElse(null); confirmation[0] = null; });
        MuiWidgets.text(c, p, "selected", 8, 172, 310, () -> selected[0] == null ? "" : blockLabel(selected[0]));
        MuiWidgets.text(c, p, "mode", 8, 188, 310, () -> (fortune ? tile.fortuneInclude : tile.silktouchInclude) ? "Include" : "Exclude");
        button(c, p, "toggle", 8, 212, 100, "qp.mui.toggle_mode", () -> {
            if (fortune) tile.fortuneInclude = !tile.fortuneInclude; else tile.silktouchInclude = !tile.silktouchInclude;
            tile.markDirty();
        });
        button(c, p, "delete", 114, 212, 100, TranslationKeys.DELETE, () -> {
            if (selected[0] != null && data.contains(selected[0])) {
                if (confirmation[0] != null && selected[0].equals(confirmation[0])) { data.remove(selected[0]); selected[0] = null; confirmation[0] = null; tile.markDirty(); }
                else confirmation[0] = selected[0];
            }
        });
        MuiWidgets.text(c, p, "confirmation", 220, 210, 104, () -> confirmation[0] == null ? "" : "Delete again to confirm");
        return p;
    }

    private static ModularPanel template(MuiContext c) {
        ModularPanel p = MuiWidgets.panel(c, "item.template.name", 176, 217);
        final int slot = c.player.inventory.currentItem;
        final ItemStack openedStack = c.player.inventory.getStackInSlot(slot);
        Supplier<Boolean> editable = () -> c.player.inventory.currentItem == slot
                && c.player.inventory.getStackInSlot(slot) == openedStack
                && openedStack.getItem() == QuarryPlusI.itemTemplate();
        Supplier<ItemStack> stack = () -> editable.get() ? openedStack : ItemStack.EMPTY;
        Supplier<ItemTemplate.Template> state = () -> ItemTemplate.getTemplate(stack.get());
        BlockData[] selected = {null};
        rows(c, p, "template", 8, 22, 120, 3, () -> {
            List<String> names = new ArrayList<>();
            scala.collection.Iterator<BlockData> it = state.get().items().iterator();
            while (it.hasNext()) names.add(blockLabel(it.next()));
            return names;
        }, name -> {
            scala.collection.Iterator<BlockData> it = state.get().items().iterator();
            while (it.hasNext()) { BlockData b = it.next(); if (blockLabel(b).equals(name)) selected[0] = b; }
        });
        button(c, p, "add", 132, 110, 40, TranslationKeys.ADD, () -> {
            if (!editable.get() || !(c.legacy instanceof ContainerListTemplate)) return;
            ItemStack input = ((ContainerListTemplate) c.legacy).craftMatrix.getStackInSlot(0);
            if (!input.isEmpty() && input.getItem() instanceof ItemBlock) {
                BlockData b = BlockData.apply(input.getItem().getRegistryName(), input.getItemDamage());
                if (!state.get().items().contains(b)) ItemTemplate.setTemplate(stack.get(), state.get().add(b));
                c.player.inventory.markDirty();
            }
        });
        button(c, p, "remove", 132, 22, 40, TranslationKeys.DELETE, () -> {
            if (!editable.get()) return;
            if (selected[0] != null) ItemTemplate.setTemplate(stack.get(), state.get().remove(selected[0]));
            selected[0] = null;
            c.player.inventory.markDirty();
        });
        literal(c, p, "modeToggle", 132, 42, 40, "+/-", () -> {
            if (!editable.get()) return;
            ItemTemplate.setTemplate(stack.get(), state.get().toggle()); c.player.inventory.markDirty();
        });
        MuiWidgets.text(c, p, "mode", 8, 110, 120, () -> state.get().include() ? "Include" : "Exclude");
        MuiWidgets.slots(c, p);
        return p;
    }

    private static ModularPanel pump(MuiContext c, EnumFacing initialSide) {
        TilePump tile = (TilePump) c.tile;
        ModularPanel p = MuiWidgets.panel(c, "qp.mui.pump_filters", 360, 228);
        EnumFacing[] side = {initialSide};
        String[] selected = {""}, input = {""}, pending = {""};
        boolean[] browse = {false};
        Supplier<LinkedList<String>> mapping = () -> tile.mapping.get(side[0]);
        List<String> registered = new ArrayList<>(FluidRegistry.getRegisteredFluids().keySet());
        registered.sort(String::compareTo);
        for (EnumFacing facing : EnumFacing.VALUES) {
            button(c, p, "side" + facing.ordinal(), 8 + facing.ordinal() * 57, 24, 55, "FD." + facing, () -> {
                side[0] = facing; selected[0] = ""; pending[0] = "";
            });
            button(c, p, "copy" + facing.ordinal(), 8 + facing.ordinal() * 57, 202, 55, "FD." + facing, () -> {
                tile.mapping.put(side[0], new LinkedList<>(tile.mapping.get(facing)));
                selected[0] = ""; pending[0] = ""; tile.markDirty();
            });
        }
        Supplier<List<String>> visibleFluids = () -> browse[0]
                ? registered.stream().filter(n -> !mapping.get().contains(n)).collect(Collectors.toList())
                : new ArrayList<>(mapping.get());
        rows(c, p, "fluids", 8, 48, 232, 3, () -> visibleFluids.get().stream().map(FilterPanels::fluidLabel).collect(Collectors.toList()),
                label -> visibleFluids.get().stream().filter(name -> fluidLabel(name).equals(label)).findFirst().ifPresent(name -> {
                    selected[0] = name; input[0] = name; pending[0] = "";
                }));
        MuiWidgets.text(c, p, "sideName", 248, 48, 104, () -> side[0].toString());
        button(c, p, "browse", 248, 62, 104, TranslationKeys.FROM_LIST, () -> browse[0] = !browse[0]);
        Mappings.Type[] types = {Mappings.Type.Remove, Mappings.Type.Top, Mappings.Type.Up, Mappings.Type.Down, Mappings.Type.Bottom};
        String[] keys = {TranslationKeys.DELETE, TranslationKeys.GO_TOP, TranslationKeys.GO_UP, TranslationKeys.GO_DOWN, TranslationKeys.GO_BOTTOM};
        for (int i = 0; i < types.length; i++) {
            final Mappings.Type type = types[i];
            button(c, p, "edit" + i, 248, 84 + i * 20, 104, keys[i], () -> {
                if (!mapping.get().contains(selected[0])) return;
                if (type == Mappings.Type.Remove && !pending[0].equals("delete:" + selected[0])) { pending[0] = "delete:" + selected[0]; return; }
                Mappings.Update.typeAction(mapping.get(), selected[0], type); pending[0] = ""; tile.markDirty();
            });
        }
        StringSyncValue value = new StringSyncValue(() -> input[0], s -> {
            if (s == null || s.length() > 256 || s.chars().anyMatch(Character::isISOControl)) return;
            input[0] = s;
            pending[0] = "";
        });
        c.sync.syncValue("fluidInput", 0, value);
        p.child(new TextFieldWidget().setMaxLength(256).autoUpdateOnChange(true).pos(8, 132).size(180, 18).syncHandler("fluidInput", 0));
        button(c, p, "add", 192, 132, 48, TranslationKeys.ADD, () -> {
            String name = input[0].trim();
            if (name.isEmpty() || mapping.get().contains(name)) { pending[0] = "Already registered or empty"; return; }
            if (!pending[0].equals("add:" + name)) { pending[0] = "add:" + name; return; }
            mapping.get().add(name); pending[0] = ""; tile.markDirty();
        });
        MuiWidgets.text(c, p, "selected", 8, 154, 232, () -> selected[0]);
        MuiWidgets.text(c, p, "confirmation", 8, 168, 232, () -> pending[0].isEmpty() ? "" : pending[0] + " (repeat to confirm)");
        p.child(IKey.lang(TranslationKeys.COPY_FROM_OTHER_DIRECTION).asWidget().pos(8, 186).size(344, 12));
        return p;
    }

    private static ModularPanel controller(MuiContext c) {
        ModularPanel p = MuiWidgets.panel(c, "qp.mui.spawner", 330, 230);
        Supplier<List<String>> available = () -> ForgeRegistries.ENTITIES.getValuesCollection().stream()
                .filter(e -> e.getEntityClass() != null && !Modifier.isAbstract(e.getEntityClass().getModifiers()))
                .filter(e -> !Config.content().spawnerBlacklist().contains(e.getRegistryName()))
                .map(e -> e.getRegistryName().toString()).sorted(Comparator.naturalOrder()).collect(Collectors.toList());
        List<String> initial = available.get();
        String[] selected = {initial.isEmpty() ? "" : initial.get(0)};
        rows(c, p, "entities", 8, 24, 314, 7, () -> initial, s -> selected[0] = s);
        MuiWidgets.text(c, p, "selected", 8, 188, 314, () -> selected[0]);
        button(c, p, "done", 8, 206, 314, TranslationKeys.DONE, () -> {
            if (c.player.world.getBlockState(c.data.getBlockPos()).getBlock() instanceof BlockController
                    && !Config.content().disableMapJ().get(BlockController.SYMBOL) && available.get().contains(selected[0])) {
                BlockController.setSpawnerEntity(c.player.world, c.data.getBlockPos(), new ResourceLocation(selected[0]));
                c.player.closeScreen();
            }
        });
        return p;
    }
}
