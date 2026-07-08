package com.swblimpopolis.myrandommod.client.screen;

import com.swblimpopolis.myrandommod.MyRandomMod;
import com.swblimpopolis.myrandommod.menu.SolarPanelMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

// Container screen for the solar panel: the background, a charge-progress arrow that fills toward the
// output (faster with a bigger/sunnier cluster), and two status lines placed in the clear band between
// the machine slots and the player inventory so they never overlap the slots or the arrow.
public class SolarPanelScreen extends AbstractContainerScreen<SolarPanelMenu> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(MyRandomMod.MODID, "textures/gui/solar_panel.png");
    private static final Identifier CHARGE_PROGRESS = Identifier.fromNamespaceAndPath(MyRandomMod.MODID, "textures/gui/charge_progress.png");
    private static final int LABEL_COLOR = 0xFF404040;
    // Progress arrow: same position/size as the vanilla furnace's burn arrow (24x16 at 79,34).
    private static final int ARROW_X = 79;
    private static final int ARROW_Y = 34;
    private static final int ARROW_W = 24;
    private static final int ARROW_H = 16;
    // "Panels: lit/total" sits up top, just under the "Solar Panel" title (the slot boxes are tall
    // enough that a line below them would clip into the input box).
    private static final int PANELS_LABEL_Y = 18;

    public SolarPanelScreen(SolarPanelMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);

        // Charge arrow fills left-to-right with the current progress.
        int filled = this.menu.getChargeProgress(ARROW_W);
        if (filled > 0) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, CHARGE_PROGRESS, x + ARROW_X, y + ARROW_Y,
                    0.0F, 0.0F, filled, ARROW_H, ARROW_W, ARROW_H);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractLabels(graphics, mouseX, mouseY);
        Component panels = Component.translatable("gui.myrandommod.solar_panel.panels",
                this.menu.getLitCount(), this.menu.getClusterSize());
        graphics.text(this.font, panels, this.titleLabelX, PANELS_LABEL_Y, LABEL_COLOR, false);
    }
}
