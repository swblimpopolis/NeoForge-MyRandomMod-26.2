package com.swblimpopolis.myrandommod.item;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.Consumer;

import com.swblimpopolis.myrandommod.MyRandomMod;

import net.minecraft.ChatFormatting;
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
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

// A melee weapon that runs on battery charge instead of durability. Each hit on a mob spends one charge
// (see CombatEvents, which also blocks attacks when empty). Right-click while holding a charged battery to
// refill: it consumes one charged battery, tops up the charge, and returns an empty battery. The sword is
// flagged unbreakable so vanilla durability never applies — charge is the only resource.
public class ElectricSwordItem extends Item {
    // One charged battery is worth this much charge; the reservoir holds several batteries (MAX_BATTERIES).
    public static final int CHARGE_PER_BATTERY = 250;
    public static final int MAX_BATTERIES = 4;
    public static final int MAX_CHARGE = CHARGE_PER_BATTERY * MAX_BATTERIES;
    // Charge spent per successful hit on an entity.
    public static final int CHARGE_PER_HIT = 1;

    private static final int BAR_WIDTH = 13;

    // Stats roughly on par with a diamond sword; durability here is unused (the item is unbreakable).
    private static final ToolMaterial MATERIAL = new ToolMaterial(
            BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 1, 8.0F, 3.0F, 10, ItemTags.DIAMOND_TOOL_MATERIALS);

    public ElectricSwordItem(Properties properties) {
        super(properties);
    }

    // Builds the registration properties: a diamond-tier sword that can never wear out or break, so its
    // only resource is the custom CHARGE component. UNBREAKABLE makes isDamageableItem() false.
    public static Properties baseProperties() {
        // Hide the "Unbreakable" tooltip line — charge is what matters, not durability.
        LinkedHashSet<DataComponentType<?>> hidden = new LinkedHashSet<>();
        hidden.add(DataComponents.UNBREAKABLE);
        return new Properties()
                .sword(MATERIAL, 3.0F, -2.4F)
                .component(DataComponents.UNBREAKABLE, Unit.INSTANCE)
                .component(DataComponents.TOOLTIP_DISPLAY, new TooltipDisplay(false, hidden));
    }

    // ----- charge accessors -----

    public static int getCharge(ItemStack stack) {
        return stack.getOrDefault(MyRandomMod.CHARGE.get(), 0);
    }

    // Sets the charge (clamped) and keeps the custom-model-data flag in sync so the item swaps between the
    // charged and empty textures (flag 0 = has charge). See assets/myrandommod/items/electric_sword.json.
    public static void setCharge(ItemStack stack, int value) {
        int clamped = Mth.clamp(value, 0, MAX_CHARGE);
        stack.set(MyRandomMod.CHARGE.get(), clamped);
        stack.set(DataComponents.CUSTOM_MODEL_DATA,
                new CustomModelData(List.of(), List.of(clamped > 0), List.of(), List.of()));
    }

    // ----- recharge on right-click -----

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack sword = player.getItemInHand(hand);
        if (getCharge(sword) >= MAX_CHARGE || !hasChargedBattery(player)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            consumeOneChargedBattery(player);
            setCharge(sword, getCharge(sword) + CHARGE_PER_BATTERY);
            giveEmptyBattery(player);
            level.playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP,
                    SoundSource.PLAYERS, 0.7F, 1.4F);
        }
        return InteractionResult.SUCCESS;
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

    // ----- charge bar (green when full -> red when low) -----

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getCharge(stack) < MAX_CHARGE;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Mth.clamp(Math.round(getCharge(stack) * (float) BAR_WIDTH / MAX_CHARGE), 0, BAR_WIDTH);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        float fraction = getCharge(stack) / (float) MAX_CHARGE;
        return Mth.hsvToRgb(fraction / 3.0F, 1.0F, 1.0F);
    }

    // ----- tooltip -----

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> adder, TooltipFlag flag) {
        adder.accept(Component.translatable("gui.myrandommod.electric_sword.charge", getCharge(stack), MAX_CHARGE)
                .withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, context, display, adder, flag);
    }
}
