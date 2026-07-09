package com.swblimpopolis.myrandommod.client.jei;

import java.util.List;

import com.swblimpopolis.myrandommod.MyRandomMod;
import com.swblimpopolis.myrandommod.item.crafting.ElectricalBenchRecipes;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;

// JEI integration for the Electrical Bench. Since MC 1.21.2 the client has no recipes, so we read them
// from the single-player integrated server (available when JEI registers recipes) and register them
// under our own category with the bench as the crafting station.
@JeiPlugin
public class ElectricalBenchJeiPlugin implements IModPlugin {
    // Created from a plain identifier (NOT MyRandomMod.ELECTRICAL_CRAFTING.get()) because JEI loads this
    // plugin class before the recipe-type registry is bound — touching the registry here would NPE.
    @SuppressWarnings("removal")
    public static final IRecipeType<RecipeHolder<CraftingRecipe>> JEI_TYPE =
            RecipeType.createRecipeHolderType(Identifier.fromNamespaceAndPath(MyRandomMod.MODID, "electrical_bench"));

    @Override
    public Identifier getPluginUid() {
        return Identifier.fromNamespaceAndPath(MyRandomMod.MODID, "jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new ElectricalBenchJeiCategory(registration.getJeiHelpers()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        MinecraftServer server = Minecraft.getInstance().getSingleplayerServer();
        if (server != null) {
            List<RecipeHolder<CraftingRecipe>> recipes = ElectricalBenchRecipes.all(server);
            registration.addRecipes(JEI_TYPE, recipes);
        }
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addCraftingStation(JEI_TYPE, MyRandomMod.ELECTRICAL_BENCH.get());
    }
}
