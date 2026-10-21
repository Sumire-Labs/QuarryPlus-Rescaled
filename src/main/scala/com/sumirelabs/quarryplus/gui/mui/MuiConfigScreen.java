package com.sumirelabs.quarryplus.gui.mui;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.screen.GuiScreenWrapper;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.value.StringValue;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonParseException;
import com.yogpc.qp.Config;
import com.yogpc.qp.QuarryPlus;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.config.ConfigGuiType;
import net.minecraftforge.fml.client.config.IConfigElement;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Client-only configuration editor. Edits are staged until every value passes validation. */
@SideOnly(Side.CLIENT)
public final class MuiConfigScreen {
    private static final Gson JSON = new Gson();
    private static final int ROWS = 5;
    private final List<Entry> entries = new ArrayList<>();
    private int page;
    private String error = "";
    private final GuiScreen parent;

    private MuiConfigScreen(GuiScreen parent) {
        this.parent = parent;
        flatten(Config.getElements(), "");
    }

    public static GuiScreen create(GuiScreen parent) {
        MuiConfigScreen editor = new MuiConfigScreen(parent);
        ModularScreen screen = new ModularScreen(QuarryPlus.modID, editor.panel()).openParentOnClose(true);
        // JEI queries UI settings during GuiOpenEvent, before the wrapper's initGui runs.
        screen.getContext().setSettings(new com.cleanroommc.modularui.screen.UISettings());
        return new GuiScreenWrapper(screen);
    }

    private void flatten(List<IConfigElement> elements, String path) {
        for (IConfigElement element : elements) {
            if (!element.showInGui()) continue;
            if (element.isProperty()) entries.add(new Entry(element, path));
            else flatten(element.getChildElements(), path + label(element) + " / ");
        }
    }

    private ModularPanel panel() {
        ModularPanel panel = ModularPanel.defaultPanel("quarryplus_config", 384, 230);
        panel.child(IKey.lang("qp.mui.config.title").asWidget().pos(8, 7).size(280, 12));
        panel.child(IKey.dynamic(() -> (page + 1) + " / " + pages()).asWidget().pos(320, 7).size(56, 12));
        for (int i = 0; i < entries.size(); i++) {
            Entry entry = entries.get(i);
            final int entryPage = i / ROWS;
            int y = 29 + i % ROWS * 28;
            String label = label(entry.element);
            if (entry.element.requiresMcRestart()) label += " *";
            else if (entry.element.requiresWorldRestart()) label += " **";
            final String fullLabel = entry.path + label;
            panel.child(IKey.str(label).asWidget().pos(8, y + 2).size(156, 22)
                    .tooltipBuilder(t -> t.addLine(IKey.str(fullLabel))
                            .addLine(IKey.str(entry.element.getComment() == null ? "" : entry.element.getComment())))
                    .setEnabledIf(w -> page == entryPage));
            if (!entry.element.isList() && entry.element.getType() == ConfigGuiType.BOOLEAN) {
                panel.child(new ButtonWidget<>().pos(170, y).size(160, 20)
                        .overlay(IKey.lang(() -> Boolean.parseBoolean(entry.value) ? "qp.mui.config.true" : "qp.mui.config.false"))
                        .onMousePressed(button -> {
                            if (button != 0) return false;
                            entry.value = Boolean.toString(!Boolean.parseBoolean(entry.value));
                            return true;
                        }).setEnabledIf(w -> page == entryPage));
            } else {
                panel.child(new TextFieldWidget().pos(170, y).size(160, 20)
                        .value(new StringValue.Dynamic(() -> entry.value, value -> entry.value = value))
                        .autoUpdateOnChange(true).setEnabledIf(w -> page == entryPage)
                        .tooltipBuilder(t -> t.addLine(IKey.str(entry.element.isList()
                                ? I18n.format("qp.mui.config.list_hint") : entry.element.getQualifiedName()))));
            }
            panel.child(new ButtonWidget<>().pos(334, y).size(45, 20).overlay(IKey.lang("gui.reset"))
                    .onMousePressed(button -> {
                        if (button != 0) return false;
                        entry.reset();
                        return true;
                    }).setEnabledIf(w -> page == entryPage));
        }
        panel.child(IKey.dynamic(() -> error).asWidget().pos(8, 168).size(368, 24));
        panel.child(IKey.lang("qp.mui.config.restart_hint").asWidget().pos(8, 193).size(368, 10));
        button(panel, 8, 205, 32, "<", () -> page = Math.max(0, page - 1));
        button(panel, 44, 205, 32, ">", () -> page = Math.min(pages() - 1, page + 1));
        button(panel, 82, 205, 86, I18n.format("qp.mui.config.defaults"), () -> entries.forEach(Entry::reset));
        button(panel, 194, 205, 88, I18n.format("gui.done"), this::save);
        button(panel, 288, 205, 88, I18n.format("gui.cancel"), () -> Minecraft.getMinecraft().displayGuiScreen(parent));
        return panel;
    }

    private int pages() { return Math.max(1, (entries.size() + ROWS - 1) / ROWS); }

    private static String label(IConfigElement element) {
        String key = element.getLanguageKey();
        if (key == null || key.isEmpty()) return element.getName();
        String translated = I18n.format(key);
        return translated.equals(key) ? element.getName() : translated;
    }

    private static void button(ModularPanel panel, int x, int y, int width, String label, Runnable action) {
        panel.child(new ButtonWidget<>().name("config_" + x).pos(x, y).size(width, 20).overlay(IKey.str(label)).onMousePressed(button -> {
            if (button != 0) return false;
            action.run();
            return true;
        }));
    }

    static void validateSmokeValues() {
        MuiConfigScreen editor = new MuiConfigScreen(null);
        for (Entry entry : editor.entries) {
            entry.parse();
            String previous = entry.value;
            if (entry.element.isList() || entry.element.getType() == ConfigGuiType.INTEGER
                    || entry.element.getType() == ConfigGuiType.DOUBLE || entry.element.getType() == ConfigGuiType.BOOLEAN) {
                entry.value = "not-a-valid-value";
                boolean rejected = false;
                try { entry.parse(); } catch (RuntimeException expected) { rejected = true; }
                finally { entry.value = previous; }
                if (!rejected) throw new IllegalStateException("Invalid configuration value accepted: " + entry.element.getName());
            }
        }
    }

    private void save() {
        List<Object> parsed = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            Entry entry = entries.get(i);
            try {
                parsed.add(entry.parse());
            } catch (RuntimeException exception) {
                page = i / ROWS;
                error = label(entry.element) + ": " + exception.getMessage();
                return;
            }
        }
        for (int i = 0; i < entries.size(); i++) {
            IConfigElement element = entries.get(i).element;
            if (element.isList()) element.set((Object[]) parsed.get(i));
            else element.set(parsed.get(i));
        }
        MinecraftForge.EVENT_BUS.post(new ConfigChangedEvent.OnConfigChangedEvent(QuarryPlus.modID, null, false, false));
        Minecraft.getMinecraft().displayGuiScreen(parent);
    }

    private static final class Entry {
        final IConfigElement element;
        final String path;
        String value;

        Entry(IConfigElement element, String path) {
            this.element = element;
            this.path = path;
            value = element.isList() ? JSON.toJson(element.getList()) : String.valueOf(element.get());
        }

        void reset() {
            value = element.isList() ? JSON.toJson(element.getDefaults()) : String.valueOf(element.getDefault());
        }

        Object parse() {
            if (!element.isList()) return scalar(value);
            JsonElement json;
            try {
                json = new JsonParser().parse(value);
            } catch (JsonParseException exception) {
                throw invalid("json_array");
            }
            if (!json.isJsonArray()) throw invalid("json_array");
            JsonArray array = json.getAsJsonArray();
            if (element.isListLengthFixed() && array.size() != element.getDefaults().length)
                throw invalid("list_length", element.getDefaults().length);
            if (element.getMaxListLength() >= 0 && array.size() > element.getMaxListLength())
                throw invalid("list_max_length", element.getMaxListLength());
            Object[] result = new Object[array.size()];
            for (int i = 0; i < array.size(); i++) {
                if (!array.get(i).isJsonPrimitive()) throw invalid("scalar_array");
                result[i] = scalar(array.get(i).getAsString());
            }
            return result;
        }

        Object scalar(String input) {
            Object parsed = input;
            if (element.getType() == ConfigGuiType.BOOLEAN) {
                if (!"true".equalsIgnoreCase(input) && !"false".equalsIgnoreCase(input))
                    throw invalid("boolean");
                parsed = Boolean.valueOf(input);
            } else if (element.getType() == ConfigGuiType.INTEGER) {
                try { parsed = Integer.valueOf(input.trim()); }
                catch (NumberFormatException exception) { throw invalid("integer"); }
            }
            else if (element.getType() == ConfigGuiType.DOUBLE) {
                double number;
                try { number = Double.parseDouble(input.trim()); }
                catch (NumberFormatException exception) { throw invalid("number"); }
                if (!Double.isFinite(number)) throw invalid("number");
                parsed = number;
            }
            if (parsed instanceof Number) {
                double number = ((Number) parsed).doubleValue();
                if (element.getMinValue() instanceof Number && number < ((Number) element.getMinValue()).doubleValue())
                    throw invalid("minimum", element.getMinValue());
                if (element.getMaxValue() instanceof Number && number > ((Number) element.getMaxValue()).doubleValue())
                    throw invalid("maximum", element.getMaxValue());
            }
            if (element.getValidationPattern() != null && !element.getValidationPattern().matcher(input).matches())
                throw invalid("pattern", element.getValidationPattern());
            if (element.getValidValues() != null && element.getValidValues().length > 0
                    && !Arrays.asList(element.getValidValues()).contains(input))
                throw invalid("allowed", Arrays.toString(element.getValidValues()));
            return parsed;
        }

        private static IllegalArgumentException invalid(String key, Object... args) {
            return new IllegalArgumentException(I18n.format("qp.mui.config.error." + key, args));
        }
    }
}
