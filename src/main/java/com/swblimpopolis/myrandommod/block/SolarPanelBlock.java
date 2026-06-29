package com.swblimpopolis.myrandommod.block;

import com.mojang.serialization.MapCodec;

import com.swblimpopolis.myrandommod.MyRandomMod;
import com.swblimpopolis.myrandommod.block.entity.SolarPanelBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

// A daylight-detector-shaped slab that generates batteries during the day. Panels touching each
// other form a soft "cluster": one panel (the lowest position) is the lead that holds the shared
// output and generates faster the more panels are connected. Right-clicking any panel opens the
// lead's inventory, so you always extract from one place.
public class SolarPanelBlock extends BaseEntityBlock {
    public static final MapCodec<SolarPanelBlock> CODEC = simpleCodec(SolarPanelBlock::new);
    // LIT = this panel currently has sky access (drives the on/off texture). LEAD = this panel is the
    // cluster's lead (the one holding the shared output). Both are kept in sync by the block entity tick.
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public static final BooleanProperty LEAD = BooleanProperty.create("lead");
    // Same 16x16 footprint, 6 pixels tall — exactly the vanilla daylight detector shape.
    private static final VoxelShape SHAPE = Block.column(16.0, 0.0, 6.0);

    public SolarPanelBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(LIT, false).setValue(LEAD, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT, LEAD);
    }

    @Override
    protected MapCodec<SolarPanelBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SolarPanelBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        // Only tick on the server, and only in dimensions that actually have a sky (no Nether solar power).
        if (level.isClientSide() || !level.dimensionType().hasSkyLight()) {
            return null;
        }
        return createTickerHelper(type, MyRandomMod.SOLAR_PANEL_BE.get(), SolarPanelBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            // Open the clicked panel's own menu. Its slot and status data forward to the cluster's
            // lead, so you still see the shared stash — but the menu's reach check is measured from
            // the panel in front of you, not the (possibly distant) lead. Opening the lead's menu
            // directly would slam shut the moment you stood more than ~8 blocks from the lead.
            if (level.getBlockEntity(pos) instanceof SolarPanelBlockEntity be) {
                player.openMenu(be);
            }
        }
        return InteractionResult.SUCCESS;
    }
}
