package com.sumirelabs.quarryplus.gui.mui;

import com.cleanroommc.modularui.screen.GuiContainerWrapper;
import com.cleanroommc.modularui.screen.ModularContainer;
import com.cleanroommc.modularui.screen.ModularScreen;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Distinct native wrappers let JEI retain each machine's recipe click area. */
@SideOnly(Side.CLIENT)
public final class MuiRecipeGui {
    private MuiRecipeGui() {}

    public static final class Workbench extends GuiContainerWrapper {
        public Workbench(ModularContainer container, ModularScreen screen) { super(container, screen); }
    }

    public static final class BookMover extends GuiContainerWrapper {
        public BookMover(ModularContainer container, ModularScreen screen) { super(container, screen); }
    }
}
