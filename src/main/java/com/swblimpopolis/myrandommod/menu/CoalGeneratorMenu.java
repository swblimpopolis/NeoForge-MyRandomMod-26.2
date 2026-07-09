package com.swblimpopolis.myrandommod.menu;

import com.swblimpopolis.myrandommod.MyRandomMod;
import com.swblimpopolis.myrandommod.block.entity.CoalGeneratorBlockEntity;

import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

// Container GUI for the coal generator: empty-battery input, coal fuel slot, and a charged-battery
// output, laid out like a furnace. The synced ContainerData drives the flame and charge-arrow gauges.
public class CoalGeneratorMenu extends AbstractContainerMenu {
    private static final int INPUT_SLOT = 0;
    private static final int FUEL_SLOT = 1;
    private static final int OUTPUT_SLOT = 2;
    private static final int INV_START = 3;
    private static final int INV_END = 39; // exclusive: 3 machine slots + 36 player slots

    private final Container container;
    private final ContainerData data;

    // Client-side constructor: empty stand-ins; the real contents arrive via sync.
    public CoalGeneratorMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(3), new SimpleContainerData(4));
    }

    // Server-side constructor: backed by the block entity's container and synced data.
    public CoalGeneratorMenu(int containerId, Inventory inventory, Container container, ContainerData data) {
        super(MyRandomMod.COAL_GENERATOR_MENU.get(), containerId);
        checkContainerSize(container, 3);
        this.container = container;
        this.data = data;

        // Input (empty batteries) top-left, fuel (coal) bottom-left, output (charged) right — furnace layout.
        this.addSlot(new Slot(container, INPUT_SLOT, 55, 17) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(MyRandomMod.EMPTY_BATTERY.get());
            }
        });
        this.addSlot(new Slot(container, FUEL_SLOT, 56, 53) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return CoalGeneratorBlockEntity.isFuel(stack);
            }
        });
        this.addSlot(new Slot(container, OUTPUT_SLOT, 115, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

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
            if (index == OUTPUT_SLOT || index == INPUT_SLOT || index == FUEL_SLOT) {
                // Machine slot -> player inventory.
                if (!this.moveItemStackTo(stack, INV_START, INV_END, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (stack.is(MyRandomMod.EMPTY_BATTERY.get())) {
                // Empty batteries -> input slot.
                if (!this.moveItemStackTo(stack, INPUT_SLOT, INPUT_SLOT + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (CoalGeneratorBlockEntity.isFuel(stack)) {
                // Coal fuels -> fuel slot.
                if (!this.moveItemStackTo(stack, FUEL_SLOT, FUEL_SLOT + 1, false)) {
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

    // Fraction of the current fuel's burn time remaining (drives the flame gauge).
    public float getLitProgress() {
        int litTime = this.data.get(0);
        int litDuration = this.data.get(1);
        return litDuration > 0 ? Mth.clamp((float) litTime / litDuration, 0.0F, 1.0F) : 0.0F;
    }

    // Fraction of the current battery's charge complete (drives the arrow).
    public float getChargeProgress() {
        int progress = this.data.get(2);
        int total = this.data.get(3);
        return total > 0 && progress > 0 ? Mth.clamp((float) progress / total, 0.0F, 1.0F) : 0.0F;
    }
}
