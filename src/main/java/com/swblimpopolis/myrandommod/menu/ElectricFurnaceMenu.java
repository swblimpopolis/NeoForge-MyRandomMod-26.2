package com.swblimpopolis.myrandommod.menu;

import com.swblimpopolis.myrandommod.MyRandomMod;
import com.swblimpopolis.myrandommod.block.entity.ElectricFurnaceBlockEntity;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.RecipeBookType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipePropertySet;

// A furnace menu with one extra slot: the spent-battery output. Everything else (input, fuel, result,
// recipe book, shift-click routing) is the vanilla furnace behaviour; we only add the 4th slot and
// teach quick-move how to pull batteries out of it.
public class ElectricFurnaceMenu extends AbstractFurnaceMenu {
    private static final int SLOT_BATTERY_OUTPUT = 3;
    // Menu slot index of the battery output: it's added after the 3 furnace slots + 36 inventory slots.
    private static final int BATTERY_MENU_SLOT = 39;
    private static final int INV_FIRST = 3;
    private static final int INV_END = 39; // exclusive; the player inventory occupies 3..38

    // Client-side constructor: stand-in 4-slot container + data; the real contents arrive via sync.
    public ElectricFurnaceMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(4), new SimpleContainerData(4));
    }

    // Server-side constructor: backed by the block entity's 4-slot container and synced data.
    public ElectricFurnaceMenu(int containerId, Inventory inventory, Container container, ContainerData data) {
        super(MyRandomMod.ELECTRIC_FURNACE_MENU.get(), RecipePropertySet.FURNACE_INPUT, RecipeBookType.FURNACE,
                containerId, inventory, container, data);
        // Battery output (right of the smelt result): extract-only.
        this.addSlot(new Slot(container, SLOT_BATTERY_OUTPUT, 147, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
    }

    // Only electric fuel counts as fuel here — this gates the fuel slot and shift-click routing.
    @Override
    protected boolean isFuel(ItemStack stack) {
        return ElectricFurnaceBlockEntity.isElectricFuel(stack);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        // The battery output slot sits past every slot vanilla's quickMoveStack knows about, so handle
        // it ourselves (move its contents to the player inventory). Everything else is vanilla furnace.
        if (index != BATTERY_MENU_SLOT) {
            return super.quickMoveStack(player, index);
        }
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack result = stack.copy();
        if (!this.moveItemStackTo(stack, INV_FIRST, INV_END, true)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == result.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return result;
    }
}
