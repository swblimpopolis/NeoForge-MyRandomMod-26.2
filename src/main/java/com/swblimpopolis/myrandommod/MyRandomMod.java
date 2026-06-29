package com.swblimpopolis.myrandommod;

import java.util.Set;
import java.util.function.Supplier;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import com.swblimpopolis.myrandommod.block.ElectricFurnaceBlock;
import com.swblimpopolis.myrandommod.block.entity.ElectricFurnaceBlockEntity;
import com.swblimpopolis.myrandommod.menu.ElectricFurnaceMenu;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
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

    // Creates a new simple item with the id "myrandommod:battery"
    public static final DeferredItem<Item> BATTERY = ITEMS.registerSimpleItem("battery");

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

    // Creates a creative tab with the id "myrandommod:random_mod_tab" using the battery as its icon
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> RANDOM_MOD_TAB = CREATIVE_MODE_TABS.register("random_mod_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.myrandommod.random_mod")) // The language key for the title of this CreativeModeTab
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> BATTERY.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(BATTERY.get()); // Add the battery to the Random Mod tab
                output.accept(ELECTRIC_FURNACE_ITEM.get()); // Add the electric furnace to the Random Mod tab
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
