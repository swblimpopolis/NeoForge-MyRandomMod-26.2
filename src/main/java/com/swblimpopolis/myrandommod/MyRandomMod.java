package com.swblimpopolis.myrandommod;

import java.util.Set;
import java.util.function.Supplier;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import com.swblimpopolis.myrandommod.block.CoalGeneratorBlock;
import com.swblimpopolis.myrandommod.block.ElectricalBenchBlock;
import com.swblimpopolis.myrandommod.block.ElectricFurnaceBlock;
import com.swblimpopolis.myrandommod.block.SolarPanelBlock;
import com.swblimpopolis.myrandommod.item.crafting.ElectricalShapedRecipe;
import com.swblimpopolis.myrandommod.menu.ElectricalBenchMenu;
import com.swblimpopolis.myrandommod.block.entity.CoalGeneratorBlockEntity;
import com.swblimpopolis.myrandommod.block.entity.ElectricFurnaceBlockEntity;
import com.swblimpopolis.myrandommod.block.entity.SolarPanelBlockEntity;
import com.swblimpopolis.myrandommod.item.ElectricAxeItem;
import com.swblimpopolis.myrandommod.item.ElectricShovelItem;
import com.swblimpopolis.myrandommod.item.ElectricSwordItem;
import com.swblimpopolis.myrandommod.item.ElectricToolItem;
import com.swblimpopolis.myrandommod.menu.CoalGeneratorMenu;
import com.swblimpopolis.myrandommod.menu.ElectricFurnaceMenu;
import com.swblimpopolis.myrandommod.menu.SolarPanelMenu;

import com.mojang.serialization.Codec;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(MyRandomMod.MODID)
public class MyRandomMod {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "myrandommod";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();
    // Create a Deferred Register to hold Blocks which will all be registered under the "myrandommod" namespace
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    // Create a Deferred Register to hold Items which will all be registered under the "myrandommod" namespace
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    // Create a Deferred Register to hold CreativeModeTabs which will all be registered under the "myrandommod" namespace
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    // Create a Deferred Register to hold BlockEntityTypes which will all be registered under the "myrandommod" namespace
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);
    // Create a Deferred Register to hold MenuTypes which will all be registered under the "myrandommod" namespace
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(Registries.MENU, MODID);
    // Create a Deferred Register to hold custom data component types under the "myrandommod" namespace
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, MODID);
    // Registers for the Electrical Bench's own crafting recipe type + serializer
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, MODID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, MODID);

    // The Electrical Bench's crafting recipe type: recipes of this type are only matched by the bench, so
    // they can't be crafted in the vanilla table. All the bench's (shaped) recipes share this type.
    public static final Supplier<RecipeType<CraftingRecipe>> ELECTRICAL_CRAFTING = RECIPE_TYPES.register("electrical_crafting",
            () -> new RecipeType<CraftingRecipe>() {
                @Override
                public String toString() {
                    return MODID + ":electrical_crafting";
                }
            });
    // The shaped serializer (recipe JSON "type": "myrandommod:electrical_crafting").
    public static final Supplier<RecipeSerializer<ElectricalShapedRecipe>> ELECTRICAL_SHAPED = RECIPE_SERIALIZERS.register("electrical_crafting",
            () -> new RecipeSerializer<>(ElectricalShapedRecipe.MAP_CODEC, ElectricalShapedRecipe.STREAM_CODEC));

    // The "charge" stored on a battery-powered item (e.g. the electric sword). Persisted to disk and
    // synced to the client so the charge bar and charged/empty model render correctly.
    public static final Supplier<DataComponentType<Integer>> CHARGE = DATA_COMPONENTS.register("charge",
            () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.INT)
                    .build());

    // The charged battery: produced by the solar panel and usable as fuel in the electric furnace.
    public static final DeferredItem<Item> CHARGED_BATTERY = ITEMS.registerSimpleItem("charged_battery");
    // The empty battery: a crafting component for future recipes. Deliberately not a fuel.
    public static final DeferredItem<Item> EMPTY_BATTERY = ITEMS.registerSimpleItem("empty_battery");

    // The electric sword: a melee weapon that runs on battery charge (its own CHARGE component) rather
    // than wearing out. Right-click with a charged battery to refill; unusable at zero charge. See ElectricSwordItem.
    public static final DeferredItem<ElectricSwordItem> ELECTRIC_SWORD = ITEMS.registerItem("electric_sword",
            ElectricSwordItem::new, ElectricSwordItem::baseProperties);
    // Battery-powered diamond-tier tools that share the sword's charge mechanics (see ElectricToolItem).
    public static final DeferredItem<ElectricToolItem> ELECTRIC_PICKAXE = ITEMS.registerItem("electric_pickaxe",
            ElectricToolItem::new, ElectricToolItem::pickaxeProperties);
    public static final DeferredItem<ElectricAxeItem> ELECTRIC_AXE = ITEMS.registerItem("electric_axe",
            ElectricAxeItem::new, ElectricToolItem::toolBaseProperties);
    public static final DeferredItem<ElectricShovelItem> ELECTRIC_SHOVEL = ITEMS.registerItem("electric_shovel",
            ElectricShovelItem::new, ElectricToolItem::toolBaseProperties);

    // The electric furnace block. For now it behaves exactly like a vanilla furnace (same smelting,
    // fuel and automation), emitting light when lit just like the real thing.
    public static final DeferredBlock<ElectricFurnaceBlock> ELECTRIC_FURNACE = BLOCKS.registerBlock("electric_furnace",
            ElectricFurnaceBlock::new,
            p -> p.mapColor(MapColor.STONE).strength(3.5f).requiresCorrectToolForDrops()
                    .lightLevel(state -> state.getValue(AbstractFurnaceBlock.LIT) ? 13 : 0));
    // The item form of the electric furnace block
    public static final DeferredItem<BlockItem> ELECTRIC_FURNACE_ITEM = ITEMS.registerSimpleBlockItem("electric_furnace", ELECTRIC_FURNACE);
    // The block entity that runs the smelting logic for the electric furnace
    public static final Supplier<BlockEntityType<ElectricFurnaceBlockEntity>> ELECTRIC_FURNACE_BE = BLOCK_ENTITY_TYPES.register("electric_furnace",
            () -> new BlockEntityType<>(ElectricFurnaceBlockEntity::new, Set.of(ELECTRIC_FURNACE.get())));
    // The menu (container GUI) for the electric furnace
    public static final Supplier<MenuType<ElectricFurnaceMenu>> ELECTRIC_FURNACE_MENU = MENU_TYPES.register("electric_furnace",
            () -> IMenuTypeExtension.create((windowId, inventory, data) -> new ElectricFurnaceMenu(windowId, inventory)));

    // The solar panel block: a daylight-detector-shaped slab that generates batteries during the day.
    // Touching panels form a cluster that generates faster and shares a single output (see the block entity).
    public static final DeferredBlock<SolarPanelBlock> SOLAR_PANEL = BLOCKS.registerBlock("solar_panel",
            SolarPanelBlock::new,
            p -> p.mapColor(MapColor.METAL).strength(2.0f).requiresCorrectToolForDrops().noOcclusion());
    // The item form of the solar panel block
    public static final DeferredItem<BlockItem> SOLAR_PANEL_ITEM = ITEMS.registerSimpleBlockItem("solar_panel", SOLAR_PANEL);
    // The block entity that runs the daylight generation and clustering for the solar panel
    public static final Supplier<BlockEntityType<SolarPanelBlockEntity>> SOLAR_PANEL_BE = BLOCK_ENTITY_TYPES.register("solar_panel",
            () -> new BlockEntityType<>(SolarPanelBlockEntity::new, Set.of(SOLAR_PANEL.get())));
    // The menu (container GUI) for the solar panel
    public static final Supplier<MenuType<SolarPanelMenu>> SOLAR_PANEL_MENU = MENU_TYPES.register("solar_panel",
            () -> IMenuTypeExtension.create((windowId, inventory, data) -> new SolarPanelMenu(windowId, inventory)));

    // The coal generator: burns coal/charcoal/coal blocks to charge empty batteries into charged ones.
    // Glows while it has fuel energy loaded (see CoalGeneratorBlockEntity).
    public static final DeferredBlock<CoalGeneratorBlock> COAL_GENERATOR = BLOCKS.registerBlock("coal_generator",
            CoalGeneratorBlock::new,
            p -> p.mapColor(MapColor.STONE).strength(3.5f).requiresCorrectToolForDrops()
                    .lightLevel(state -> state.getValue(CoalGeneratorBlock.LIT) ? 13 : 0));
    // The item form of the coal generator block
    public static final DeferredItem<BlockItem> COAL_GENERATOR_ITEM = ITEMS.registerSimpleBlockItem("coal_generator", COAL_GENERATOR);
    // The block entity that runs the burn/charge logic for the coal generator
    public static final Supplier<BlockEntityType<CoalGeneratorBlockEntity>> COAL_GENERATOR_BE = BLOCK_ENTITY_TYPES.register("coal_generator",
            () -> new BlockEntityType<>(CoalGeneratorBlockEntity::new, Set.of(COAL_GENERATOR.get())));
    // The menu (container GUI) for the coal generator
    public static final Supplier<MenuType<CoalGeneratorMenu>> COAL_GENERATOR_MENU = MENU_TYPES.register("coal_generator",
            () -> IMenuTypeExtension.create((windowId, inventory, data) -> new CoalGeneratorMenu(windowId, inventory)));

    // The electrical bench: a crafting-table-style block that crafts the mod's items exclusively.
    public static final DeferredBlock<ElectricalBenchBlock> ELECTRICAL_BENCH = BLOCKS.registerBlock("electrical_bench",
            ElectricalBenchBlock::new,
            p -> p.mapColor(MapColor.METAL).strength(2.5f).requiresCorrectToolForDrops());
    // The item form of the electrical bench block
    public static final DeferredItem<BlockItem> ELECTRICAL_BENCH_ITEM = ITEMS.registerSimpleBlockItem("electrical_bench", ELECTRICAL_BENCH);
    // The menu (3x3 crafting GUI) for the electrical bench
    public static final Supplier<MenuType<ElectricalBenchMenu>> ELECTRICAL_BENCH_MENU = MENU_TYPES.register("electrical_bench",
            () -> IMenuTypeExtension.create((windowId, inventory, data) -> new ElectricalBenchMenu(windowId, inventory)));

    // Creates a creative tab with the id "myrandommod:random_mod_tab" using the charged battery as its icon
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> RANDOM_MOD_TAB = CREATIVE_MODE_TABS.register("random_mod_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.myrandommod.random_mod")) // The language key for the title of this CreativeModeTab
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> CHARGED_BATTERY.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(CHARGED_BATTERY.get()); // Add the charged battery to the Random Mod tab
                output.accept(EMPTY_BATTERY.get()); // Add the empty battery to the Random Mod tab
                output.accept(ELECTRIC_SWORD.get()); // Add the electric sword to the Random Mod tab
                output.accept(ELECTRIC_PICKAXE.get()); // Add the electric pickaxe to the Random Mod tab
                output.accept(ELECTRIC_AXE.get()); // Add the electric axe to the Random Mod tab
                output.accept(ELECTRIC_SHOVEL.get()); // Add the electric shovel to the Random Mod tab
                output.accept(ELECTRIC_FURNACE_ITEM.get()); // Add the electric furnace to the Random Mod tab
                output.accept(SOLAR_PANEL_ITEM.get()); // Add the solar panel to the Random Mod tab
                output.accept(COAL_GENERATOR_ITEM.get()); // Add the coal generator to the Random Mod tab
                output.accept(ELECTRICAL_BENCH_ITEM.get()); // Add the electrical bench to the Random Mod tab
            }).build());

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public MyRandomMod(IEventBus modEventBus, ModContainer modContainer) {
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register the Deferred Register to the mod event bus so blocks get registered
        BLOCKS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so items get registered
        ITEMS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so tabs get registered
        CREATIVE_MODE_TABS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so block entity types get registered
        BLOCK_ENTITY_TYPES.register(modEventBus);
        // Register the Deferred Register to the mod event bus so menu types get registered
        MENU_TYPES.register(modEventBus);
        // Register the Deferred Register to the mod event bus so data component types get registered
        DATA_COMPONENTS.register(modEventBus);
        // Register the Electrical Bench recipe type + serializer registers
        RECIPE_TYPES.register(modEventBus);
        RECIPE_SERIALIZERS.register(modEventBus);

        // Register ourselves for server and other game events we are interested in.
        // Note that this is necessary if and only if we want *this* class (MyRandomMod) to respond directly to events.
        // Do not add this line if there are no @SubscribeEvent-annotated functions in this class, like onServerStarting() below.
        NeoForge.EVENT_BUS.register(this);

        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        // Some common setup code
        LOGGER.info("HELLO FROM COMMON SETUP");
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
    }
}
