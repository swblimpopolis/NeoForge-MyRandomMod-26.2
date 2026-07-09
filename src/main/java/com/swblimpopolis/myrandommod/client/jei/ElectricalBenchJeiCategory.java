package com.swblimpopolis.myrandommod.client.jei;

import java.util.List;

import com.swblimpopolis.myrandommod.MyRandomMod;
import com.swblimpopolis.myrandommod.item.crafting.ElectricalShapedRecipe;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.ICraftingGridHelper;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jspecify.annotations.Nullable;

// JEI category showing the Electrical Bench's shaped recipes as a normal 3x3 crafting layout. Icon is
// null so JEI uses the registered catalyst (the bench block) as the tab icon.
public class ElectricalBenchJeiCategory implements IRecipeCategory<RecipeHolder<CraftingRecipe>> {
    private final ICraftingGridHelper gridHelper;

    public ElectricalBenchJeiCategory(IJeiHelpers helpers) {
        this.gridHelper = helpers.getGuiHelper().createCraftingGridHelper();
    }

    @Override
    public IRecipeType<RecipeHolder<CraftingRecipe>> getRecipeType() {
        return ElectricalBenchJeiPlugin.JEI_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("container.myrandommod.electrical_bench");
    }

    @Override
    public int getWidth() {
        return 116;
    }

    @Override
    public int getHeight() {
        return 54;
    }

    @Override
    public @Nullable IDrawable getIcon() {
        return null;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<CraftingRecipe> holder, IFocusGroup focuses) {
        if (holder.value() instanceof ElectricalShapedRecipe recipe) {
            List<List<ItemStack>> inputs = recipe.ingredients().stream()
                    .map(opt -> opt.map(ing -> ing.items().map(ItemStack::new).toList()).orElse(List.of()))
                    .toList();
            this.gridHelper.createAndSetInputs(builder, inputs, recipe.width(), recipe.height());
            this.gridHelper.createAndSetOutputs(builder, List.of(recipe.resultStack()));
        }
    }
}
