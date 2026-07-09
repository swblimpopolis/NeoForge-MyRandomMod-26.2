package com.swblimpopolis.myrandommod.menu;

import java.util.List;
import java.util.Optional;

import com.swblimpopolis.myrandommod.MyRandomMod;
import com.swblimpopolis.myrandommod.item.crafting.ElectricalBenchRecipes;

import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractCraftingMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.RecipeBookType;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;

// A themed crafting station: functionally a crafting table (vanilla crafting recipes + the recipe book),
// just backed by the Electrical Bench block and its own GUI. Cloned from vanilla CraftingMenu, but with
// our MenuType and a stillValid check against our block.
public class ElectricalBenchMenu extends AbstractCraftingMenu {
    // The browser sends a recipe index via the vanilla button-click packet, which carries no shift state.
    // We encode "shift held → fill the grid with as many sets as possible" by offsetting the index by this
    // flag (the packet's buttonId is a VarInt, so the offset is safe).
    public static final int SHIFT_FLAG = 1000;

    private final ContainerLevelAccess access;
    private final Player player;
    private boolean placingRecipe;

    // Client-side constructor.
    public ElectricalBenchMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, ContainerLevelAccess.NULL);
    }

    // Server-side constructor (bound to the bench's world position).
    public ElectricalBenchMenu(int containerId, Inventory inventory, ContainerLevelAccess access) {
        super(MyRandomMod.ELECTRICAL_BENCH_MENU.get(), containerId, 3, 3);
        this.access = access;
        this.player = inventory.player;
        // Custom result slot so ingredient consumption uses our ELECTRICAL_CRAFTING type (vanilla
        // ResultSlot hardcodes RecipeType.CRAFTING, which breaks/duplicates consumption for our recipes).
        this.addSlot(new ElectricalResultSlot(this.player, this.craftSlots, this.resultSlots, 0, 124, 35));
        this.addCraftingGridSlots(30, 17);
        this.addStandardInventorySlots(inventory, 8, 84);
    }

    private static void slotChangedCraftingGrid(AbstractCraftingMenu menu, ServerLevel level, Player player,
                                                CraftingContainer container, ResultContainer resultSlots) {
        CraftingInput input = container.asCraftInput();
        ServerPlayer serverPlayer = (ServerPlayer) player;
        ItemStack result = ItemStack.EMPTY;
        Optional<RecipeHolder<CraftingRecipe>> maybeRecipe =
                level.getServer().getRecipeManager().getRecipeFor(MyRandomMod.ELECTRICAL_CRAFTING.get(), input, level);
        if (maybeRecipe.isPresent()) {
            RecipeHolder<CraftingRecipe> holder = maybeRecipe.get();
            if (resultSlots.setRecipeUsed(serverPlayer, holder)) {
                ItemStack assembled = holder.value().assemble(input);
                if (assembled.isItemEnabled(level.enabledFeatures())) {
                    result = assembled;
                }
            }
        }
        resultSlots.setItem(0, result);
        menu.setRemoteSlot(0, result);
        serverPlayer.connection.send(new ClientboundContainerSetSlotPacket(menu.containerId, menu.incrementStateId(), 0, result));
    }

    @Override
    public void slotsChanged(Container container) {
        if (!this.placingRecipe) {
            this.access.execute((level, pos) -> {
                if (level instanceof ServerLevel serverLevel) {
                    slotChangedCraftingGrid(this, serverLevel, this.player, this.craftSlots, this.resultSlots);
                }
            });
        }
    }

    @Override
    protected void beginPlacingRecipe() {
        this.placingRecipe = true;
    }

    @Override
    protected void finishPlacingRecipe(ServerLevel level, RecipeHolder<CraftingRecipe> recipe) {
        this.placingRecipe = false;
        slotChangedCraftingGrid(this, level, this.player, this.craftSlots, this.resultSlots);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.access.execute((level, pos) -> this.clearContainer(player, this.craftSlots));
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, MyRandomMod.ELECTRICAL_BENCH.get());
    }

    // The in-GUI recipe browser sends the clicked recipe's index here (via the vanilla button-click
    // packet). We look up that index in the bench's recipe list (same id-sorted order the client used)
    // and auto-fill the 3x3 grid from the player's inventory, exactly like the recipe book "place" button.
    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        boolean useMaxItems = buttonId >= SHIFT_FLAG;
        int index = useMaxItems ? buttonId - SHIFT_FLAG : buttonId;
        return this.access.evaluate((level, pos) -> {
            if (!(level instanceof ServerLevel serverLevel)) {
                return false;
            }
            List<RecipeHolder<CraftingRecipe>> recipes = ElectricalBenchRecipes.all(serverLevel.getServer());
            if (index < 0 || index >= recipes.size()) {
                return false;
            }
            // useMaxItems=true fills every grid slot with as many sets as the inventory allows (shift-click
            // in the vanilla recipe book); false places a single set.
            this.handlePlacement(useMaxItems, false, recipes.get(index), serverLevel, player.getInventory());
            return true;
        }, false);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        ItemStack clicked = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            clicked = stack.copy();
            if (slotIndex == 0) {
                stack.getItem().onCraftedBy(stack, player);
                if (!this.moveItemStackTo(stack, 10, 46, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(stack, clicked);
            } else if (slotIndex >= 10 && slotIndex < 46) {
                if (!this.moveItemStackTo(stack, 1, 10, false)) {
                    if (slotIndex < 37) {
                        if (!this.moveItemStackTo(stack, 37, 46, false)) {
                            return ItemStack.EMPTY;
                        }
                    } else if (!this.moveItemStackTo(stack, 10, 37, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            } else if (!this.moveItemStackTo(stack, 10, 46, false)) {
                return ItemStack.EMPTY;
            }

            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            if (stack.getCount() == clicked.getCount()) {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, stack);
            if (slotIndex == 0) {
                player.drop(stack, false);
            }
        }
        return clicked;
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack carried, Slot target) {
        return target.container != this.resultSlots && super.canTakeItemForPickAll(carried, target);
    }

    @Override
    public Slot getResultSlot() {
        return this.slots.get(0);
    }

    @Override
    public List<Slot> getInputGridSlots() {
        return this.slots.subList(1, 10);
    }

    @Override
    public RecipeBookType getRecipeBookType() {
        return RecipeBookType.CRAFTING;
    }

    @Override
    protected Player owner() {
        return this.player;
    }
}
