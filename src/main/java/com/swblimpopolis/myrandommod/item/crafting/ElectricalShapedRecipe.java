package com.swblimpopolis.myrandommod.item.crafting;

import java.util.List;
import java.util.Optional;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import com.swblimpopolis.myrandommod.MyRandomMod;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.NormalCraftingRecipe;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.level.Level;

// A shaped crafting recipe under the mod's own ELECTRICAL_CRAFTING type (not vanilla RecipeType.CRAFTING),
// so these recipes only work on the Electrical Bench and never in the vanilla crafting table. Mirrors
// vanilla ShapedRecipe but extends NormalCraftingRecipe directly (avoids ShapedRecipe's serializer being
// pinned to RecipeSerializer<ShapedRecipe>). Public accessors expose the pattern/result for the bench's
// in-GUI recipe browser and the JEI plugin.
public class ElectricalShapedRecipe extends NormalCraftingRecipe {
    final ShapedRecipePattern pattern;
    final ItemStackTemplate result;

    public static final MapCodec<ElectricalShapedRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(
            i -> i.group(
                    Recipe.CommonInfo.MAP_CODEC.forGetter(o -> o.commonInfo),
                    CraftingRecipe.CraftingBookInfo.MAP_CODEC.forGetter(o -> o.bookInfo),
                    ShapedRecipePattern.MAP_CODEC.forGetter(o -> o.pattern),
                    ItemStackTemplate.CODEC.fieldOf("result").forGetter(o -> o.result)
            ).apply(i, ElectricalShapedRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ElectricalShapedRecipe> STREAM_CODEC = StreamCodec.composite(
            Recipe.CommonInfo.STREAM_CODEC, o -> o.commonInfo,
            CraftingRecipe.CraftingBookInfo.STREAM_CODEC, o -> o.bookInfo,
            ShapedRecipePattern.STREAM_CODEC, o -> o.pattern,
            ItemStackTemplate.STREAM_CODEC, o -> o.result,
            ElectricalShapedRecipe::new);

    public ElectricalShapedRecipe(Recipe.CommonInfo commonInfo, CraftingRecipe.CraftingBookInfo bookInfo,
                                  ShapedRecipePattern pattern, ItemStackTemplate result) {
        super(commonInfo, bookInfo);
        this.pattern = pattern;
        this.result = result;
    }

    @Override
    public RecipeType<CraftingRecipe> getType() {
        return MyRandomMod.ELECTRICAL_CRAFTING.get();
    }

    @Override
    public RecipeSerializer<ElectricalShapedRecipe> getSerializer() {
        return MyRandomMod.ELECTRICAL_SHAPED.get();
    }

    @Override
    protected PlacementInfo createPlacementInfo() {
        return PlacementInfo.createFromOptionals(this.pattern.ingredients());
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return this.pattern.matches(input);
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        return this.result.create();
    }

    // ----- accessors for the bench recipe browser + JEI plugin -----

    public List<Optional<Ingredient>> ingredients() {
        return this.pattern.ingredients();
    }

    public int width() {
        return this.pattern.width();
    }

    public int height() {
        return this.pattern.height();
    }

    public ItemStack resultStack() {
        return this.result.create();
    }
}
