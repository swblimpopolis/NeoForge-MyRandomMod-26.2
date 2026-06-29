package com.swblimpopolis.myrandommod;

import net.neoforged.neoforge.common.ModConfigSpec;

// The mod's config class. This is not required, but it's a good idea to have one to keep your config organized.
// It currently defines no options; the commented examples below show how to use Neo's config APIs when you need them.
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // --- Example config options (kept as a template) -------------------------------------------------
    // Uncomment and adapt these, then read them with e.g. LOG_DIRT_BLOCK.getAsBoolean() in your code.
    //
    // public static final ModConfigSpec.BooleanValue LOG_DIRT_BLOCK = BUILDER
    //         .comment("Whether to log the dirt block on common setup")
    //         .define("logDirtBlock", true);
    //
    // public static final ModConfigSpec.IntValue MAGIC_NUMBER = BUILDER
    //         .comment("A magic number")
    //         .defineInRange("magicNumber", 42, 0, Integer.MAX_VALUE);
    //
    // public static final ModConfigSpec.ConfigValue<String> MAGIC_NUMBER_INTRODUCTION = BUILDER
    //         .comment("What you want the introduction message to be for the magic number")
    //         .define("magicNumberIntroduction", "The magic number is... ");
    //
    // // a list of strings that are treated as resource locations for items
    // public static final ModConfigSpec.ConfigValue<List<? extends String>> ITEM_STRINGS = BUILDER
    //         .comment("A list of items to log on common setup.")
    //         .defineListAllowEmpty("items", List.of("minecraft:iron_ingot"), () -> "", Config::validateItemName);
    //
    // private static boolean validateItemName(final Object obj) {
    //     return obj instanceof String itemName && BuiltInRegistries.ITEM.containsKey(Identifier.parse(itemName));
    // }
    // -------------------------------------------------------------------------------------------------

    static final ModConfigSpec SPEC = BUILDER.build();
}
