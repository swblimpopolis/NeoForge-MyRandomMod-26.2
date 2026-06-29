package com.swblimpopolis.myrandommod.block.entity;

import com.swblimpopolis.myrandommod.MyRandomMod;
import com.swblimpopolis.myrandommod.menu.ElectricFurnaceMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
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
}
