package com.swblimpopolis.myrandommod.menu;

import com.swblimpopolis.myrandommod.MyRandomMod;
import com.swblimpopolis.myrandommod.block.entity.SolarPanelBlockEntity;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

// Container GUI for the solar panel: an input slot (empty batteries) and an extract-only output slot
// (charged batteries), plus the player inventory. The synced ContainerData lets the screen show the
// cluster size and whether it's charging.
public class SolarPanelMenu extends AbstractContainerMenu {
    private static final int INPUT_SLOT = 0;
    private static final int OUTPUT_SLOT = 1;
    private static final int INV_START = 2;
    private static final int INV_END = 38; // exclusive: 2 machine slots + 36 player slots

    private final Container container;
    private final ContainerData data;

    // Client-side constructor: empty stand-ins; the real contents arrive via sync.
    public SolarPanelMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(2), new SimpleContainerData(4));
    }

    // Server-side constructor: backed by the lead block entity's container and synced data.
    public SolarPanelMenu(int containerId, Inventory inventory, Container container, ContainerData data) {
        super(MyRandomMod.SOLAR_PANEL_MENU.get(), containerId);
        checkContainerSize(container, 2);
        this.container = container;
        this.data = data;

        // Input slot (left): only empty batteries may be placed here.
        this.addSlot(new Slot(container, INPUT_SLOT, 51, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(MyRandomMod.EMPTY_BATTERY.get());
            }
        });
        // Output slot (right): charged batteries; players may take but not place.
        this.addSlot(new Slot(container, OUTPUT_SLOT, 115, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        // Player inventory (3 rows) and hotbar, standard 176-wide layout.
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventory, col, 8 + col * 18, 142));
        }

        this.addDataSlots(data);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index == INPUT_SLOT || index == OUTPUT_SLOT) {
                // Machine slot -> player inventory.
                if (!this.moveItemStackTo(stack, INV_START, INV_END, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (stack.is(MyRandomMod.EMPTY_BATTERY.get())) {
                // Empty batteries from inventory -> the input slot.
                if (!this.moveItemStackTo(stack, INPUT_SLOT, INPUT_SLOT + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Anything else: shuffle between main inventory and hotbar.
                int mainEnd = INV_START + 27;
                if (index < mainEnd) {
                    if (!this.moveItemStackTo(stack, mainEnd, INV_END, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!this.moveItemStackTo(stack, INV_START, mainEnd, false)) {
                    return ItemStack.EMPTY;
                }
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
        }
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }

    public int getClusterSize() {
        return this.data.get(1);
    }

    // Panels currently in sunlight — the ones actually contributing to the charging speed.
    public int getLitCount() {
        return this.data.get(3);
    }

    // Charge progress scaled to `pixels` wide (for the arrow overlay). Because progress climbs by the
    // lit-panel count each tick, a bigger/sunnier cluster visibly fills the arrow faster.
    public int getChargeProgress(int pixels) {
        int progress = this.data.get(0);
        int max = SolarPanelBlockEntity.MAX_PROGRESS;
        if (max <= 0 || progress <= 0) {
            return 0;
        }
        return Math.min(pixels, progress * pixels / max);
    }
}
