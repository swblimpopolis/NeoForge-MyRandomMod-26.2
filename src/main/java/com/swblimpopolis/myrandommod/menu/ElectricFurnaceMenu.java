package com.swblimpopolis.myrandommod.menu;

import com.swblimpopolis.myrandommod.MyRandomMod;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.RecipeBookType;
import net.minecraft.world.item.crafting.RecipePropertySet;

// A furnace menu in every respect: same slots, same accepted inputs (FURNACE_INPUT) and the
// regular furnace recipe book. Only the MenuType differs so the right screen is opened.
public class ElectricFurnaceMenu extends AbstractFurnaceMenu {
    // Client-side constructor: the MenuType supplier builds the menu with an empty container/data.
    public ElectricFurnaceMenu(int containerId, Inventory inventory) {
        super(MyRandomMod.ELECTRIC_FURNACE_MENU.get(), RecipePropertySet.FURNACE_INPUT, RecipeBookType.FURNACE, containerId, inventory);
    }

    // Server-side constructor: backed by the block entity's container and synced data.
    public ElectricFurnaceMenu(int containerId, Inventory inventory, Container container, ContainerData data) {
        super(MyRandomMod.ELECTRIC_FURNACE_MENU.get(), RecipePropertySet.FURNACE_INPUT, RecipeBookType.FURNACE, containerId, inventory, container, data);
    }
}
