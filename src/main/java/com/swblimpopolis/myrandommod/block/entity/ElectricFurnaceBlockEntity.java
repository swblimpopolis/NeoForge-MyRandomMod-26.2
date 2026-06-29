package com.swblimpopolis.myrandommod.block.entity;

import com.swblimpopolis.myrandommod.MyRandomMod;
import com.swblimpopolis.myrandommod.datamap.ElectricFuel;
import com.swblimpopolis.myrandommod.datamap.ModDataMaps;
import com.swblimpopolis.myrandommod.menu.ElectricFurnaceMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.FuelValues;
import net.minecraft.world.level.block.state.BlockState;

// Reuses the vanilla furnace block entity wholesale, smelting the same RecipeType.SMELTING recipes
// as a regular furnace. Only the type, display name and menu differ.
public class ElectricFurnaceBlockEntity extends AbstractFurnaceBlockEntity {
    private static final Component DEFAULT_NAME = Component.translatable("container.myrandommod.electric_furnace");

    public ElectricFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(MyRandomMod.ELECTRIC_FURNACE_BE.get(), pos, state, RecipeType.SMELTING);
    }

    @Override
    protected Component getDefaultName() {
        return DEFAULT_NAME;
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new ElectricFurnaceMenu(containerId, inventory, this, this.dataAccess);
    }

    // The electric furnace runs on electric fuel only: ignore vanilla fuel values entirely and
    // derive the burn time from our electric_fuel data map (0 ticks = not valid fuel here).
    @Override
    protected int getBurnDuration(FuelValues fuelValues, ItemStack stack) {
        ElectricFuel fuel = stack.typeHolder().getData(ModDataMaps.ELECTRIC_FUEL);
        return fuel != null ? fuel.burnTime() : 0;
    }

    // Restrict the fuel slot to electric fuel (the input/result slots keep their default rules).
    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == SLOT_FUEL) {
            return isElectricFuel(stack);
        }
        return super.canPlaceItem(slot, stack);
    }

    // Shared source of truth for "is this electric fuel?" — an item is electric fuel iff it has an
    // entry in the electric_fuel data map. Reused by the menu for slot validation and shift-clicking.
    public static boolean isElectricFuel(ItemStack stack) {
        return !stack.isEmpty() && stack.typeHolder().getData(ModDataMaps.ELECTRIC_FUEL) != null;
    }
}
