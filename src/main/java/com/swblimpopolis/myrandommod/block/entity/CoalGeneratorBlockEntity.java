package com.swblimpopolis.myrandommod.block.entity;

import com.swblimpopolis.myrandommod.MyRandomMod;
import com.swblimpopolis.myrandommod.block.CoalGeneratorBlock;
import com.swblimpopolis.myrandommod.menu.CoalGeneratorMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

// A coal-fired battery charger. Burns coal/charcoal/coal blocks to charge empty batteries into charged
// ones. Fuel energy is only spent while a battery is actually being charged (it pauses when idle), so
// the counts are exact: coal/charcoal each charge 2 batteries, a coal block charges 18 (9x coal).
public class CoalGeneratorBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
    public static final int SLOT_INPUT = 0;   // empty batteries
    public static final int SLOT_FUEL = 1;    // coal / charcoal / coal block
    public static final int SLOT_OUTPUT = 2;  // charged batteries
    private static final int CONTAINER_SIZE = 3;

    // Ticks of burning needed to charge one battery. Fuel burn times are multiples of this so a fuel
    // charges a whole number of batteries.
    public static final int CHARGE_TIME = 200;

    private static final int[] SLOTS_FOR_UP = {SLOT_INPUT};
    private static final int[] SLOTS_FOR_DOWN = {SLOT_OUTPUT};
    private static final int[] SLOTS_FOR_SIDES = {SLOT_FUEL};

    private static final Component DEFAULT_NAME = Component.translatable("container.myrandommod.coal_generator");

    private NonNullList<ItemStack> items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);
    // Remaining burn ticks of the fuel currently loaded, and how long that fuel burns in total (for the
    // flame gauge). Progress toward the battery being charged right now.
    private int litTime;
    private int litDuration;
    private int chargeProgress;

    // Synced to the screen: 0 = litTime, 1 = litDuration, 2 = chargeProgress, 3 = CHARGE_TIME.
    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int id) {
            return switch (id) {
                case 0 -> litTime;
                case 1 -> litDuration;
                case 2 -> chargeProgress;
                case 3 -> CHARGE_TIME;
                default -> 0;
            };
        }

        @Override
        public void set(int id, int value) {
            switch (id) {
                case 0 -> litTime = value;
                case 1 -> litDuration = value;
                case 2 -> chargeProgress = value;
            }
        }

        @Override
        public int getCount() {
            return 4;
        }
    };

    public CoalGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(MyRandomMod.COAL_GENERATOR_BE.get(), pos, state);
    }

    // ----- fuel -----

    // Burn ticks a fuel item provides. Coal/charcoal power two batteries; a coal block powers eighteen.
    public static int burnTicksFor(ItemStack fuel) {
        if (fuel.isEmpty()) {
            return 0;
        }
        if (fuel.is(Items.COAL_BLOCK)) {
            return 18 * CHARGE_TIME;
        }
        if (fuel.is(ItemTags.COALS)) {
            return 2 * CHARGE_TIME;
        }
        return 0;
    }

    public static boolean isFuel(ItemStack stack) {
        return burnTicksFor(stack) > 0;
    }

    private boolean isLit() {
        return this.litTime > 0;
    }

    // ----- tick -----

    public static void serverTick(Level level, BlockPos pos, BlockState state, CoalGeneratorBlockEntity be) {
        boolean wasLit = be.isLit();
        boolean changed = false;

        ItemStack input = be.items.get(SLOT_INPUT);
        boolean canCharge = input.is(MyRandomMod.EMPTY_BATTERY.get()) && be.hasOutputRoom();

        if (canCharge) {
            // Light a fresh fuel item if we've run out (only ever consumed with a battery waiting).
            if (!be.isLit()) {
                int burn = burnTicksFor(be.items.get(SLOT_FUEL));
                if (burn > 0) {
                    be.litTime = burn;
                    be.litDuration = burn;
                    be.items.get(SLOT_FUEL).shrink(1);
                    changed = true;
                }
            }
            // Charge only while lit; burn is spent one tick per charging tick (so it never wastes fuel).
            if (be.isLit()) {
                be.litTime--;
                be.chargeProgress++;
                if (be.chargeProgress >= CHARGE_TIME) {
                    be.chargeProgress = 0;
                    be.chargeOneBattery();
                    changed = true;
                }
            }
        } else if (be.chargeProgress > 0) {
            // Nothing to charge: don't burn fuel, and let progress bleed back down.
            be.chargeProgress = Math.max(0, be.chargeProgress - 2);
        }

        if (wasLit != be.isLit()) {
            changed = true;
            level.setBlock(pos, state.setValue(CoalGeneratorBlock.LIT, be.isLit()), Block.UPDATE_ALL);
        }
        if (changed) {
            be.setChanged();
        }
    }

    private boolean hasOutputRoom() {
        ItemStack output = this.items.get(SLOT_OUTPUT);
        return output.isEmpty()
                || (output.is(MyRandomMod.CHARGED_BATTERY.get()) && output.getCount() < output.getMaxStackSize());
    }

    private void chargeOneBattery() {
        this.items.get(SLOT_INPUT).shrink(1);
        ItemStack output = this.items.get(SLOT_OUTPUT);
        if (output.isEmpty()) {
            this.items.set(SLOT_OUTPUT, new ItemStack(MyRandomMod.CHARGED_BATTERY.get()));
        } else {
            output.grow(1);
        }
    }

    // ----- container / menu -----

    @Override
    public int getContainerSize() {
        return CONTAINER_SIZE;
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return this.items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    protected Component getDefaultName() {
        return DEFAULT_NAME;
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new CoalGeneratorMenu(containerId, inventory, this, this.dataAccess);
    }

    // Input takes only empty batteries; fuel takes only coal fuels; output is extract-only.
    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_INPUT -> stack.is(MyRandomMod.EMPTY_BATTERY.get());
            case SLOT_FUEL -> isFuel(stack);
            default -> false;
        };
    }

    @Override
    public int[] getSlotsForFace(Direction direction) {
        return switch (direction) {
            case UP -> SLOTS_FOR_UP;
            case DOWN -> SLOTS_FOR_DOWN;
            default -> SLOTS_FOR_SIDES;
        };
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction direction) {
        return canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
        return slot == SLOT_OUTPUT;
    }

    // ----- persistence -----

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, this.items);
        this.litTime = input.getIntOr("LitTime", 0);
        this.litDuration = input.getIntOr("LitDuration", 0);
        this.chargeProgress = input.getIntOr("ChargeProgress", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, this.items);
        output.putInt("LitTime", this.litTime);
        output.putInt("LitDuration", this.litDuration);
        output.putInt("ChargeProgress", this.chargeProgress);
    }

    // Drop contents when broken.
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (this.level != null) {
            Containers.dropContents(this.level, pos, this.items);
        }
    }
}
