package com.swblimpopolis.myrandommod.block;

import com.mojang.serialization.MapCodec;

import com.swblimpopolis.myrandommod.menu.ElectricalBenchMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

// A crafting table for the mod's items only. Like the vanilla table it has no block entity: right-click
// opens a 3x3 crafting menu backed by a transient grid + ContainerLevelAccess (grid pops out on close).
// The menu resolves recipes against the mod's ELECTRICAL_CRAFTING type (see ElectricalBenchMenu).
public class ElectricalBenchBlock extends Block {
    public static final MapCodec<ElectricalBenchBlock> CODEC = simpleCodec(ElectricalBenchBlock::new);
    private static final Component CONTAINER_TITLE = Component.translatable("container.myrandommod.electrical_bench");

    public ElectricalBenchBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<ElectricalBenchBlock> codec() {
        return CODEC;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            player.openMenu(state.getMenuProvider(level, pos));
            player.awardStat(Stats.INTERACT_WITH_CRAFTING_TABLE);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        return new SimpleMenuProvider(
                (containerId, inventory, player) -> new ElectricalBenchMenu(containerId, inventory, ContainerLevelAccess.create(level, pos)),
                CONTAINER_TITLE);
    }
}
