package com.swblimpopolis.myrandommod.client.screen;

import com.swblimpopolis.myrandommod.MyRandomMod;
import com.swblimpopolis.myrandommod.menu.CoalGeneratorMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

// Furnace-style screen: background, a flame gauge over the fuel slot (burn time remaining) and a charge
// arrow that fills toward the output (progress on the current battery).
public class CoalGeneratorScreen extends AbstractContainerScreen<CoalGeneratorMenu> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(MyRandomMod.MODID, "textures/gui/coal_generator.png");
    private static final Identifier FLAME = Identifier.fromNamespaceAndPath(MyRandomMod.MODID, "textures/gui/coal_generator_flame.png");
    private static final Identifier ARROW = Identifier.fromNamespaceAndPath(MyRandomMod.MODID, "textures/gui/coal_generator_arrow.png");

    // Flame: 14x14 over the fuel slot, growing bottom-up (same anchor as a vanilla furnace).
    private static final int FLAME_X = 56;
    private static final int FLAME_Y = 36;
    private static final int FLAME_SIZE = 14;
    // Charge arrow: 24x16 at the furnace burn-arrow position.
    private static final int ARROW_X = 79;
    private static final int ARROW_Y = 34;
    private static final int ARROW_W = 24;
    private static final int ARROW_H = 16;

    public CoalGeneratorScreen(CoalGeneratorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);

        // Flame gauge: reveal the bottom portion of the flame based on remaining burn time.
        float lit = this.menu.getLitProgress();
        if (lit > 0.0F) {
            int h = Mth.ceil(lit * (FLAME_SIZE - 1)) + 1;
            graphics.blit(RenderPipelines.GUI_TEXTURED, FLAME, x + FLAME_X, y + FLAME_Y + (FLAME_SIZE - h),
                    0.0F, (float) (FLAME_SIZE - h), FLAME_SIZE, h, FLAME_SIZE, FLAME_SIZE);
        }

        // Charge arrow: fill left-to-right with the current battery's charge progress.
        int filled = Mth.ceil(this.menu.getChargeProgress() * ARROW_W);
        if (filled > 0) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, ARROW, x + ARROW_X, y + ARROW_Y,
                    0.0F, 0.0F, filled, ARROW_H, ARROW_W, ARROW_H);
        }
    }
}
