package com.swblimpopolis.myrandommod.client.screen;

import com.swblimpopolis.myrandommod.MyRandomMod;
import com.swblimpopolis.myrandommod.menu.SolarPanelMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

// Simple container screen: the background plus two status lines (cluster size and sun state).
// The progress is synced but shown as text here to keep the GUI art minimal/placeholder-friendly.
public class SolarPanelScreen extends AbstractContainerScreen<SolarPanelMenu> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(MyRandomMod.MODID, "textures/gui/solar_panel.png");
    private static final int LABEL_COLOR = 0xFF404040;

    public SolarPanelScreen(SolarPanelMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractLabels(graphics, mouseX, mouseY);
        Component panels = Component.translatable("gui.myrandommod.solar_panel.panels",
                this.menu.getLitCount(), this.menu.getClusterSize());
        Component status = this.menu.isGenerating()
                ? Component.translatable("gui.myrandommod.solar_panel.generating")
                : Component.translatable("gui.myrandommod.solar_panel.idle");
        graphics.text(this.font, panels, this.titleLabelX, 18, LABEL_COLOR, false);
        graphics.text(this.font, status, this.titleLabelX, 28, LABEL_COLOR, false);
    }
}
