package com.swblimpopolis.myrandommod.item;

import java.util.function.Consumer;

import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

// Battery-powered diamond shovel. Extends ShovelItem so it keeps grass-path making, and reuses the shared
// charge logic. Making a path is blocked while empty, like mining/attacking.
public class ElectricShovelItem extends ShovelItem implements ElectricTool {
    public ElectricShovelItem(Properties properties) {
        super(ElectricToolItem.MATERIAL, 1.5F, -3.0F, properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        return ElectricToolItem.tryRecharge(level, player, hand);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        // A dead shovel makes no paths (mirrors "empty = does nothing").
        if (ElectricToolItem.getCharge(context.getItemInHand()) <= 0) {
            return InteractionResult.PASS;
        }
        return super.useOn(context);
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity owner) {
        return ElectricToolItem.spendMineCharge(stack, level, state, pos);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return ElectricToolItem.barVisible(stack);
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return ElectricToolItem.barWidth(stack);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return ElectricToolItem.barColor(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
                                Consumer<Component> adder, TooltipFlag flag) {
        ElectricToolItem.appendChargeTooltip(stack, adder);
        super.appendHoverText(stack, context, display, adder, flag);
    }
}
