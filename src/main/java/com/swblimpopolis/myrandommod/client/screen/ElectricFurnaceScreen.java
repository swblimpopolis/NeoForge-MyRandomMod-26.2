package com.swblimpopolis.myrandommod.client.screen;

import java.util.List;

import com.swblimpopolis.myrandommod.MyRandomMod;
import com.swblimpopolis.myrandommod.menu.ElectricFurnaceMenu;

import net.minecraft.client.gui.screens.inventory.AbstractFurnaceScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.SearchRecipeBookCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeBookCategories;

// Same screen as the vanilla furnace, just pointed at our own background and progress sprites.
public class ElectricFurnaceScreen extends AbstractFurnaceScreen<ElectricFurnaceMenu> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(MyRandomMod.MODID, "textures/gui/electric_furnace.png");
    private static final Identifier LIT_PROGRESS_SPRITE = Identifier.fromNamespaceAndPath(MyRandomMod.MODID, "container/electric_furnace/lit_progress");
    private static final Identifier BURN_PROGRESS_SPRITE = Identifier.fromNamespaceAndPath(MyRandomMod.MODID, "container/electric_furnace/burn_progress");
    private static final Component FILTER_NAME = Component.translatable("gui.recipebook.toggleRecipes.smeltable");

    // Reuse the vanilla furnace recipe-book tabs so smeltable items show up just like a furnace.
    private static final List<RecipeBookComponent.TabInfo> TABS = List.of(
            new RecipeBookComponent.TabInfo(SearchRecipeBookCategory.FURNACE),
            new RecipeBookComponent.TabInfo(Items.PORKCHOP, RecipeBookCategories.FURNACE_FOOD),
            new RecipeBookComponent.TabInfo(Items.STONE, RecipeBookCategories.FURNACE_BLOCKS),
            new RecipeBookComponent.TabInfo(Items.LAVA_BUCKET, Items.EMERALD, RecipeBookCategories.FURNACE_MISC));

    public ElectricFurnaceScreen(ElectricFurnaceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, FILTER_NAME, TEXTURE, LIT_PROGRESS_SPRITE, BURN_PROGRESS_SPRITE, TABS);
    }
}
