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
    private static final int SLOT_UPGRADE = 4;
    // Menu slot index of the battery output: it's added after the 3 furnace slots + 36 inventory slots.
    private static final int BATTERY_MENU_SLOT = 39;
    // Menu slot index of the upgrade slot: added right after the battery output.
    private static final int UPGRADE_MENU_SLOT = 40;
    // How many upgrades the slot accepts (matches ElectricFurnaceBlockEntity.MAX_UPGRADES).
    private static final int MAX_UPGRADES = 5;
    private static final int INV_FIRST = 3;
    private static final int INV_END = 39; // exclusive; the player inventory occupies 3..38

    // Client-side constructor: stand-in 5-slot container + data; the real contents arrive via sync.
    public ElectricFurnaceMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(5), new SimpleContainerData(4));
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
        // Speed upgrade slot (top-left corner, well clear of the recipe-book icon): only accepts
        // upgrade items, capped at MAX_UPGRADES.
        this.addSlot(new Slot(container, SLOT_UPGRADE, 13, 12) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(MyRandomMod.ELECTRIC_FURNACE_UPGRADE.get());
            }

            @Override
            public int getMaxStackSize() {
                return MAX_UPGRADES;
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
        // The battery output and upgrade slots sit past every slot vanilla's quickMoveStack knows about,
        // so shift-clicking them pulls their contents back into the player inventory.
        if (index == BATTERY_MENU_SLOT || index == UPGRADE_MENU_SLOT) {
            return moveBetween(player, index, INV_FIRST, INV_END, true);
        }
        // Shift-clicking an upgrade item out of the player inventory routes it into the upgrade slot
        // (vanilla furnace quick-move would otherwise leave it, since it's neither smeltable nor fuel).
        if (index >= INV_FIRST && index < INV_END) {
            Slot slot = this.slots.get(index);
            if (slot != null && slot.hasItem() && slot.getItem().is(MyRandomMod.ELECTRIC_FURNACE_UPGRADE.get())) {
                return moveBetween(player, index, UPGRADE_MENU_SLOT, UPGRADE_MENU_SLOT + 1, false);
            }
        }
        // Everything else is vanilla furnace behaviour (input, fuel, result, inventory shuffling).
        return super.quickMoveStack(player, index);
    }

    // Shared quick-move helper: move the stack in `index` into the [destStart, destEnd) slot range,
    // returning a copy of what moved (following vanilla's quick-move conventions), or EMPTY if nothing did.
    private ItemStack moveBetween(Player player, int index, int destStart, int destEnd, boolean reverse) {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack result = stack.copy();
        if (!this.moveItemStackTo(stack, destStart, destEnd, reverse)) {
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
