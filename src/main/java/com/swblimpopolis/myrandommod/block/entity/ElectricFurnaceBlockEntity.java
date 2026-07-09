package com.swblimpopolis.myrandommod.block.entity;

import com.swblimpopolis.myrandommod.MyRandomMod;
import com.swblimpopolis.myrandommod.datamap.ElectricFuel;
import com.swblimpopolis.myrandommod.datamap.ModDataMaps;
import com.swblimpopolis.myrandommod.menu.ElectricFurnaceMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.FuelValues;
import net.minecraft.world.level.block.state.BlockState;

// A vanilla-style furnace that runs on charged batteries instead of vanilla fuel. Burning a charged
// battery deposits a spent (empty) battery into an extra 4th slot, so the battery is recovered rather
// than destroyed — closing the loop with the solar panel (which charges empties back up).
public class ElectricFurnaceBlockEntity extends AbstractFurnaceBlockEntity {
    // The 4th slot (beyond vanilla input=0, fuel=1, result=2): spent empty batteries collect here.
    public static final int SLOT_BATTERY_OUTPUT = 3;
    // The 5th slot: speed upgrades. Each upgrade in this slot adds one extra smelting pass per tick.
    public static final int SLOT_UPGRADE = 4;
    // Cap on how many upgrades take effect (and how many the slot holds): 5 upgrades = 6x speed.
    public static final int MAX_UPGRADES = 5;
    private static final int CONTAINER_SIZE = 5;
    // Hoppers below pull the smelt result, the spent batteries, then any fuel remainder.
    private static final int[] SLOTS_FOR_DOWN = {SLOT_RESULT, SLOT_BATTERY_OUTPUT, SLOT_FUEL};

    private static final Component DEFAULT_NAME = Component.translatable("container.myrandommod.electric_furnace");

    public ElectricFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(MyRandomMod.ELECTRIC_FURNACE_BE.get(), pos, state, RecipeType.SMELTING);
        // The parent constructor sized the inventory for 3 slots; grow it to hold the battery output.
        this.items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);
    }

    @Override
    public int getContainerSize() {
        return CONTAINER_SIZE;
    }

    // Ticker entry point used by the block: run the normal furnace logic, then recover empty batteries
    // for whatever fuel was consumed this tick. Snapshotting the fuel slot before/after is reliable
    // because menu interactions never run mid-tick — any decrease here is burn consumption.
    public static void electricServerTick(Level level, BlockPos pos, BlockState state, ElectricFurnaceBlockEntity be) {
        ItemStack fuelBefore = be.getItem(SLOT_FUEL).copy();
        // One vanilla tick normally, plus one extra pass per installed upgrade. Each pass advances the
        // cook timer and burns fuel by one, so more upgrades = faster smelting at the same energy/item.
        int passes = 1 + be.getUpgradeCount();
        for (int i = 0; i < passes; i++) {
            AbstractFurnaceBlockEntity.serverTick((ServerLevel) level, pos, state, be);
        }
        be.depositSpentBatteries(fuelBefore);
    }

    // Number of active speed upgrades: the count in the upgrade slot, clamped to MAX_UPGRADES. Anything
    // other than the upgrade item (or an empty slot) counts as zero.
    private int getUpgradeCount() {
        ItemStack upgrades = this.items.get(SLOT_UPGRADE);
        if (!upgrades.is(MyRandomMod.ELECTRIC_FURNACE_UPGRADE.get())) {
            return 0;
        }
        return Math.min(upgrades.getCount(), MAX_UPGRADES);
    }

    // If a charged battery was consumed as fuel this tick, add the same number of empty batteries to
    // the battery-output slot (spilling into the world only if that slot can't hold them).
    private void depositSpentBatteries(ItemStack fuelBefore) {
        if (!fuelBefore.is(MyRandomMod.CHARGED_BATTERY.get())) {
            return;
        }
        ItemStack fuelNow = this.items.get(SLOT_FUEL);
        int chargedNow = fuelNow.is(MyRandomMod.CHARGED_BATTERY.get()) ? fuelNow.getCount() : 0;
        int consumed = fuelBefore.getCount() - chargedNow;
        if (consumed <= 0) {
            return;
        }

        ItemStack output = this.items.get(SLOT_BATTERY_OUTPUT);
        if (output.isEmpty()) {
            this.items.set(SLOT_BATTERY_OUTPUT, new ItemStack(MyRandomMod.EMPTY_BATTERY.get(), consumed));
        } else if (output.is(MyRandomMod.EMPTY_BATTERY.get())) {
            int room = output.getMaxStackSize() - output.getCount();
            int added = Math.min(room, consumed);
            output.grow(added);
            spillOverflow(consumed - added);
        } else {
            spillOverflow(consumed);
        }
        this.setChanged();
    }

    // Drop empty batteries above the furnace if the output slot can't take them, so none are lost.
    private void spillOverflow(int count) {
        if (count > 0 && this.level != null) {
            Containers.dropItemStack(this.level,
                    this.worldPosition.getX() + 0.5, this.worldPosition.getY() + 1.0, this.worldPosition.getZ() + 0.5,
                    new ItemStack(MyRandomMod.EMPTY_BATTERY.get(), count));
        }
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

    // Fuel slot takes only electric fuel; the battery-output slot is extract-only (no insertion).
    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == SLOT_FUEL) {
            return isElectricFuel(stack);
        }
        if (slot == SLOT_BATTERY_OUTPUT) {
            return false;
        }
        if (slot == SLOT_UPGRADE) {
            return stack.is(MyRandomMod.ELECTRIC_FURNACE_UPGRADE.get());
        }
        return super.canPlaceItem(slot, stack);
    }

    // Let hoppers under the furnace also pull the spent batteries out of the new slot.
    @Override
    public int[] getSlotsForFace(Direction direction) {
        if (direction == Direction.DOWN) {
            return SLOTS_FOR_DOWN;
        }
        return super.getSlotsForFace(direction);
    }

    // Shared source of truth for "is this electric fuel?" — an item is electric fuel iff it has an
    // entry in the electric_fuel data map. Reused by the menu for slot validation and shift-clicking.
    public static boolean isElectricFuel(ItemStack stack) {
        return !stack.isEmpty() && stack.typeHolder().getData(ModDataMaps.ELECTRIC_FUEL) != null;
    }
}
