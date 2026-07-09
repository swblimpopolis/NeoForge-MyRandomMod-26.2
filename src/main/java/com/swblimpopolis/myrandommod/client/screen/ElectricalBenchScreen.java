package com.swblimpopolis.myrandommod.client.screen;

import java.util.List;

import com.swblimpopolis.myrandommod.MyRandomMod;
import com.swblimpopolis.myrandommod.item.crafting.ElectricalBenchRecipes;
import com.swblimpopolis.myrandommod.item.crafting.ElectricalShapedRecipe;
import com.swblimpopolis.myrandommod.menu.ElectricalBenchMenu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;

// Screen for the Electrical Bench: the crafting GUI plus a built-in recipe browser panel to the left
// (a stonecutter-style grid of clickable result icons), so recipes are browsable without JEI. The recipe
// list is read from the single-player integrated server, in the same id-sorted order the menu uses, so a
// clicked icon's index matches the server's recipe list. Click a recipe to place one set into the 3x3 grid;
// shift-click to fill it with as many sets as your inventory allows. Recipes you lack materials for are
// dimmed (checked with the same StackedItemContents logic the vanilla recipe book uses).
public class ElectricalBenchScreen extends AbstractContainerScreen<ElectricalBenchMenu> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(MyRandomMod.MODID, "textures/gui/electrical_bench.png");
    // Reuse vanilla stonecutter cell sprites for the browser buttons (drawn at their native 16x18 size).
    private static final Identifier CELL_SPRITE = Identifier.withDefaultNamespace("container/stonecutter/recipe");
    private static final Identifier CELL_HOVER_SPRITE = Identifier.withDefaultNamespace("container/stonecutter/recipe_highlighted");
    private static final int COLS = 4;
    private static final int CELL_W = 16;
    private static final int CELL_H = 18;
    private static final int PAD = 7;
    private static final int DIM_OVERLAY = 0xB0202020; // translucent dark wash over non-craftable cells

    // Bench recipes, index-aligned with the server's recipe list; result icons derived from them.
    private List<RecipeHolder<CraftingRecipe>> recipes = List.of();
    private List<ItemStack> recipeResults = List.of();

    public ElectricalBenchScreen(ElectricalBenchMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
    }

    @Override
    protected void init() {
        super.init();
        MinecraftServer server = Minecraft.getInstance().getSingleplayerServer();
        this.recipes = server == null ? List.of() : ElectricalBenchRecipes.all(server);
        this.recipeResults = this.recipes.stream()
                .map(holder -> holder.value() instanceof ElectricalShapedRecipe recipe ? recipe.resultStack() : ItemStack.EMPTY)
                .toList();
    }

    private int panelWidth() {
        return PAD * 2 + COLS * CELL_W;
    }

    private int panelHeight() {
        int rows = Math.max(1, (this.recipeResults.size() + COLS - 1) / COLS);
        return PAD * 2 + rows * CELL_H;
    }

    private int panelX() {
        return Math.max(2, this.leftPos - panelWidth() - 4);
    }

    private int panelY() {
        return this.topPos;
    }

    private int cellX(int index) {
        return panelX() + PAD + index % COLS * CELL_W;
    }

    private int cellY(int index) {
        return panelY() + PAD + index / COLS * CELL_H;
    }

    // Which of this.recipes the player currently has the materials to craft (recipe-book highlight logic).
    private boolean[] computeCraftable() {
        boolean[] craftable = new boolean[this.recipes.size()];
        if (this.minecraft == null || this.minecraft.player == null) {
            return craftable;
        }
        StackedItemContents contents = new StackedItemContents();
        this.minecraft.player.getInventory().fillStackedContents(contents);
        for (int i = 0; i < this.recipes.size(); i++) {
            craftable[i] = contents.canCraft(this.recipes.get(i).value(), null);
        }
        return craftable;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);

        if (this.recipeResults.isEmpty()) {
            return;
        }
        boolean[] craftable = computeCraftable();
        int px = panelX();
        int py = panelY();
        int pw = panelWidth();
        int ph = panelHeight();
        graphics.fill(px, py, px + pw, py + ph, 0xFF373737);
        graphics.fill(px + 1, py + 1, px + pw - 1, py + ph - 1, 0xFFC6C6C6);
        for (int i = 0; i < this.recipeResults.size(); i++) {
            int cx = cellX(i);
            int cy = cellY(i);
            boolean hover = mouseX >= cx && mouseX < cx + CELL_W && mouseY >= cy && mouseY < cy + CELL_H;
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, hover ? CELL_HOVER_SPRITE : CELL_SPRITE, cx, cy, CELL_W, CELL_H);
            graphics.item(this.recipeResults.get(i), cx, cy + 1);
            if (!craftable[i]) {
                graphics.fill(cx, cy, cx + CELL_W, cy + CELL_H, DIM_OVERLAY);
            }
        }
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        for (int i = 0; i < this.recipeResults.size(); i++) {
            int cx = cellX(i);
            int cy = cellY(i);
            if (mouseX >= cx && mouseX < cx + CELL_W && mouseY >= cy && mouseY < cy + CELL_H) {
                graphics.setTooltipForNextFrame(this.font, this.recipeResults.get(i), mouseX, mouseY);
            }
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        for (int i = 0; i < this.recipeResults.size(); i++) {
            int cx = cellX(i);
            int cy = cellY(i);
            if (event.x() >= cx && event.x() < cx + CELL_W && event.y() >= cy && event.y() < cy + CELL_H) {
                if (this.minecraft != null && this.minecraft.player != null && this.minecraft.gameMode != null) {
                    this.minecraft.getSoundManager().play(
                            SimpleSoundInstance.forUI(SoundEvents.UI_STONECUTTER_SELECT_RECIPE, 1.0F));
                    // Shift-click fills the grid with as many sets as possible; plain click places one set.
                    int buttonId = event.hasShiftDown() ? i + ElectricalBenchMenu.SHIFT_FLAG : i;
                    this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, buttonId);
                }
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }
}
