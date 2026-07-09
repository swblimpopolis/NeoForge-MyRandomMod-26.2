package com.swblimpopolis.myrandommod.item;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.Consumer;

import com.swblimpopolis.myrandommod.MyRandomMod;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.util.Unit;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

// Base + shared logic for the battery-powered tools/weapons. The plain-Item variants (sword, pickaxe)
// extend this directly; the axe and shovel extend AxeItem/ShovelItem instead (to keep stripping/paths)
// and reuse the static helpers here. All are diamond-tier, unbreakable, and run on the CHARGE component:
// each hit/mined block spends a charge, at zero charge they do nothing, and right-click with a charged
// battery refills them (returning an empty battery).
public class ElectricToolItem extends Item implements ElectricTool {
    public static final int CHARGE_PER_BATTERY = 250;
    public static final int MAX_BATTERIES = 4;
    public static final int MAX_CHARGE = CHARGE_PER_BATTERY * MAX_BATTERIES;
    public static final int CHARGE_PER_USE = 1;

    private static final int BAR_WIDTH = 13;

    // Diamond-equivalent stats (mining speed, attack bonus, harvest tier -> can mine obsidian). Durability
    // is unused because the item is unbreakable.
    public static final ToolMaterial MATERIAL = new ToolMaterial(
            BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 1, 8.0F, 3.0F, 10, ItemTags.DIAMOND_TOOL_MATERIALS);

    public ElectricToolItem(Properties properties) {
        super(properties);
    }

    // ----- registration property factories -----

    // Unbreakable + hides the "Unbreakable" tooltip line. The base for tools whose own class (AxeItem/
    // ShovelItem) applies the tool component; the sword/pickaxe factories add it themselves.
    public static Properties toolBaseProperties() {
        LinkedHashSet<DataComponentType<?>> hidden = new LinkedHashSet<>();
        hidden.add(DataComponents.UNBREAKABLE);
        return new Properties()
                .component(DataComponents.UNBREAKABLE, Unit.INSTANCE)
                .component(DataComponents.TOOLTIP_DISPLAY, new TooltipDisplay(false, hidden));
    }

    public static Properties swordProperties() {
        return toolBaseProperties().sword(MATERIAL, 3.0F, -2.4F);
    }

    public static Properties pickaxeProperties() {
        return toolBaseProperties().pickaxe(MATERIAL, 1.0F, -2.8F);
    }

    // ----- charge state -----

    public static int getCharge(ItemStack stack) {
        return stack.getOrDefault(MyRandomMod.CHARGE.get(), 0);
    }

    // Sets the charge (clamped) and keeps the custom-model-data flag in sync so the item swaps between the
    // charged and empty textures (flag 0 = has charge). See assets/myrandommod/items/electric_*.json.
    public static void setCharge(ItemStack stack, int value) {
        int clamped = Mth.clamp(value, 0, MAX_CHARGE);
        stack.set(MyRandomMod.CHARGE.get(), clamped);
        stack.set(DataComponents.CUSTOM_MODEL_DATA,
                new CustomModelData(List.of(), List.of(clamped > 0), List.of(), List.of()));
    }

    // ----- shared behaviour (called by this class and by ElectricAxeItem/ElectricShovelItem) -----

    // Right-click recharge: consume one charged battery from the inventory, top up, return an empty battery.
    public static InteractionResult tryRecharge(Level level, Player player, InteractionHand hand) {
        ItemStack tool = player.getItemInHand(hand);
        if (getCharge(tool) >= MAX_CHARGE || !hasChargedBattery(player)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            consumeOneChargedBattery(player);
            setCharge(tool, getCharge(tool) + CHARGE_PER_BATTERY);
            giveEmptyBattery(player);
            level.playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP,
                    SoundSource.PLAYERS, 0.7F, 1.4F);
        }
        return InteractionResult.SUCCESS;
    }

    // Spend a charge when a non-instant block is mined.
    public static boolean spendMineCharge(ItemStack stack, Level level, BlockState state, BlockPos pos) {
        if (!level.isClientSide() && state.getDestroySpeed(level, pos) != 0.0F) {
            setCharge(stack, getCharge(stack) - CHARGE_PER_USE);
        }
        return true;
    }

    public static boolean barVisible(ItemStack stack) {
        return getCharge(stack) < MAX_CHARGE;
    }

    public static int barWidth(ItemStack stack) {
        return Mth.clamp(Math.round(getCharge(stack) * (float) BAR_WIDTH / MAX_CHARGE), 0, BAR_WIDTH);
    }

    public static int barColor(ItemStack stack) {
        float fraction = getCharge(stack) / (float) MAX_CHARGE;
        return Mth.hsvToRgb(fraction / 3.0F, 1.0F, 1.0F);
    }

    public static void appendChargeTooltip(ItemStack stack, Consumer<Component> adder) {
        adder.accept(Component.translatable("gui.myrandommod.electric_tool.charge", getCharge(stack), MAX_CHARGE)
                .withStyle(ChatFormatting.GRAY));
    }

    private static boolean hasChargedBattery(Player player) {
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i).is(MyRandomMod.CHARGED_BATTERY.get())) {
                return true;
            }
        }
        return false;
    }

    private static void consumeOneChargedBattery(Player player) {
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.is(MyRandomMod.CHARGED_BATTERY.get())) {
                stack.shrink(1);
                return;
            }
        }
    }

    private static void giveEmptyBattery(Player player) {
        ItemStack empty = new ItemStack(MyRandomMod.EMPTY_BATTERY.get());
        if (!player.getInventory().add(empty)) {
            player.drop(empty, false);
        }
    }

    // ----- instance overrides (sword / pickaxe) delegate to the shared helpers -----

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        return tryRecharge(level, player, hand);
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity owner) {
        return spendMineCharge(stack, level, state, pos);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return barVisible(stack);
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return barWidth(stack);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return barColor(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> adder, TooltipFlag flag) {
        appendChargeTooltip(stack, adder);
        super.appendHoverText(stack, context, display, adder, flag);
    }
}
