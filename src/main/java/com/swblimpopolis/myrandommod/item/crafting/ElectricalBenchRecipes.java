package com.swblimpopolis.myrandommod.item.crafting;

import java.util.Comparator;
import java.util.List;

import com.swblimpopolis.myrandommod.MyRandomMod;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;

// Shared source of the bench's recipe list, in a stable (id-sorted) order. Used by the JEI plugin, the
// bench menu (server-side, for click-to-place), and the in-GUI browser (client-side, via the integrated
// server) — the identical ordering keeps the browser's clicked index aligned with the server's list.
public final class ElectricalBenchRecipes {
    private ElectricalBenchRecipes() {
    }

    @SuppressWarnings("unchecked")
    public static List<RecipeHolder<CraftingRecipe>> all(MinecraftServer server) {
        return server.getRecipeManager().getRecipes().stream()
                .filter(holder -> holder.value().getType() == MyRandomMod.ELECTRICAL_CRAFTING.get())
                .map(holder -> (RecipeHolder<CraftingRecipe>) (RecipeHolder<?>) holder)
                .sorted(Comparator.comparing(holder -> holder.id().identifier().toString()))
                .toList();
    }
}
