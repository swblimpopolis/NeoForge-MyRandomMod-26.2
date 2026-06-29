package com.swblimpopolis.myrandommod.block.entity;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

import com.swblimpopolis.myrandommod.MyRandomMod;
import com.swblimpopolis.myrandommod.menu.SolarPanelMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import com.swblimpopolis.myrandommod.block.SolarPanelBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

// Holds the shared battery output and runs the daylight generation. Only the cluster's lead panel
// (lowest block position) generates and stores items; the rest just feed their share to the lead.
public class SolarPanelBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
    public static final int OUTPUT_SLOT = 0;
    // "Charge" needed for one battery at a single panel. Cluster size is added each tick, so N panels
    // make a battery N times faster (one battery every MAX_PROGRESS / N ticks of daylight).
    public static final int MAX_PROGRESS = 1200;
    // Effective sky brightness (0-15) required to generate. Night and heavy weather fall below this.
    private static final int GEN_LIGHT_THRESHOLD = 12;
    // Safety cap so an enormous connected field can never make the flood fill run away.
    private static final int MAX_CLUSTER = 256;
    private static final int CLUSTER_RECHECK_TICKS = 20;
    private static final Component DEFAULT_NAME = Component.translatable("container.myrandommod.solar_panel");
    private static final int[] OUTPUT_SLOTS = {OUTPUT_SLOT};

    private NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);
    private int progress;
    private int clusterSize = 1;
    // How many cluster panels currently have enough sky access to generate. This (not clusterSize)
    // drives the speed, so shaded/buried panels simply don't contribute. Maintained on the lead.
    private int litCount = 1;
    private boolean generating;
    private @Nullable BlockPos leadPos;

    // Synced to the open screen so it can show cluster size and whether the sun is up. Reads forward
    // to the lead so opening a non-lead panel's menu still shows the shared cluster's live status.
    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int id) {
            SolarPanelBlockEntity lead = resolveLead();
            return switch (id) {
                case 0 -> lead.progress;
                case 1 -> lead.clusterSize;
                case 2 -> lead.generating ? 1 : 0;
                case 3 -> lead.litCount;
                default -> 0;
            };
        }

        @Override
        public void set(int id, int value) {
            switch (id) {
                case 0 -> progress = value;
                case 1 -> clusterSize = value;
                case 2 -> generating = value != 0;
                case 3 -> litCount = value;
            }
        }

        @Override
        public int getCount() {
            return 4;
        }
    };

    public SolarPanelBlockEntity(BlockPos pos, BlockState state) {
        super(MyRandomMod.SOLAR_PANEL_BE.get(), pos, state);
    }

    // ----- generation / clustering -----

    public static void serverTick(Level level, BlockPos pos, BlockState state, SolarPanelBlockEntity be) {
        // Recompute which panel leads the cluster (and which panels see sky) periodically; cheap and
        // self-healing when panels are added, removed, shaded, or a field gets split in two.
        boolean recheck = be.leadPos == null || level.getGameTime() % CLUSTER_RECHECK_TICKS == 0L;
        Cluster cluster = null;
        if (recheck) {
            cluster = computeCluster(level, pos);
            be.leadPos = cluster.lead();
            be.clusterSize = cluster.size();
            // Reflect this panel's own sky access and lead status in its block state so the
            // on/off and lead textures track reality. BlockStates are interned, so the identity
            // check skips the (networked) setBlock when nothing actually changed.
            boolean lit = isPanelLit(level, pos);
            boolean lead = pos.equals(be.leadPos);
            BlockState desired = state.setValue(SolarPanelBlock.LIT, lit).setValue(SolarPanelBlock.LEAD, lead);
            if (desired != state) {
                level.setBlock(pos, desired, Block.UPDATE_CLIENTS);
                state = desired;
            }
        }

        if (!pos.equals(be.leadPos)) {
            // Not the lead: push anything we still hold toward the lead so extraction stays in one place.
            be.generating = false;
            if (!be.items.get(OUTPUT_SLOT).isEmpty()) {
                be.pushToLead(level);
            }
            return;
        }

        // Lead: tally how many panels currently have sky access — only those contribute to the speed.
        // A shaded panel (even the lead itself) just doesn't count, rather than stalling the cluster.
        if (recheck) {
            be.litCount = countLit(level, cluster.members());
        }

        ItemStack output = be.items.get(OUTPUT_SLOT);
        boolean hasRoom = output.isEmpty()
                || (output.is(MyRandomMod.CHARGED_BATTERY.get()) && output.getCount() < output.getMaxStackSize());
        be.generating = hasRoom && be.litCount > 0;

        if (be.generating) {
            be.progress += be.litCount;
            if (be.progress >= MAX_PROGRESS) {
                be.progress -= MAX_PROGRESS;
                be.addBattery();
            }
            be.setChanged();
        }
    }

    // Count cluster members that can currently generate.
    private static int countLit(Level level, Set<BlockPos> members) {
        int lit = 0;
        for (BlockPos member : members) {
            if (isPanelLit(level, member)) {
                lit++;
            }
        }
        return lit;
    }

    // A panel generates only if it has a clear vertical line of sight to the sky (canSeeSky: a block
    // directly above blocks it, unlike diffuse light level) AND it's bright enough — the effective
    // sky brightness check still gates out night and heavy weather.
    private static boolean isPanelLit(Level level, BlockPos pos) {
        return level.canSeeSky(pos) && level.getEffectiveSkyBrightness(pos) >= GEN_LIGHT_THRESHOLD;
    }

    private void addBattery() {
        ItemStack output = this.items.get(OUTPUT_SLOT);
        if (output.isEmpty()) {
            this.items.set(OUTPUT_SLOT, new ItemStack(MyRandomMod.CHARGED_BATTERY.get()));
        } else {
            output.grow(1);
        }
    }

    // Move our buffered batteries into the lead's output slot (merging stacks).
    private void pushToLead(Level level) {
        if (this.leadPos == null || this.leadPos.equals(this.worldPosition)) {
            return;
        }
        if (!(level.getBlockEntity(this.leadPos) instanceof SolarPanelBlockEntity lead)) {
            return;
        }
        ItemStack mine = this.items.get(OUTPUT_SLOT);
        ItemStack leadStack = lead.items.get(OUTPUT_SLOT);
        if (leadStack.isEmpty()) {
            lead.items.set(OUTPUT_SLOT, mine.copy());
            this.items.set(OUTPUT_SLOT, ItemStack.EMPTY);
        } else if (ItemStack.isSameItemSameComponents(leadStack, mine)) {
            int canAccept = leadStack.getMaxStackSize() - leadStack.getCount();
            int moved = Math.min(canAccept, mine.getCount());
            if (moved > 0) {
                leadStack.grow(moved);
                mine.shrink(moved);
            }
        }
        lead.setChanged();
        this.setChanged();
    }

    // Resolve the panel that actually holds this cluster's shared batteries. Falls back to this panel
    // if the lead can't be resolved yet (e.g. before the first server tick computes it).
    private SolarPanelBlockEntity resolveLead() {
        if (this.level == null) {
            return this;
        }
        BlockPos lead = this.leadPos;
        if (lead == null) {
            lead = computeCluster(this.level, this.worldPosition).lead();
            this.leadPos = lead;
        }
        if (lead.equals(this.worldPosition)) {
            return this;
        }
        return this.level.getBlockEntity(lead) instanceof SolarPanelBlockEntity resolved ? resolved : this;
    }

    // Flood-fill the connected panels and report the cluster size plus its deterministic lead
    // (the lowest block position). Pure read-only over block states — safe to call any time.
    public static Cluster computeCluster(Level level, BlockPos start) {
        BlockPos origin = start.immutable();
        Set<BlockPos> visited = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        visited.add(origin);
        queue.add(origin);
        BlockPos lead = origin;

        while (!queue.isEmpty() && visited.size() < MAX_CLUSTER) {
            BlockPos current = queue.poll();
            for (Direction direction : Direction.values()) {
                BlockPos neighbor = current.relative(direction);
                if (visited.contains(neighbor)) {
                    continue;
                }
                if (level.getBlockState(neighbor).getBlock() instanceof SolarPanelBlock) {
                    BlockPos immutableNeighbor = neighbor.immutable();
                    visited.add(immutableNeighbor);
                    queue.add(immutableNeighbor);
                    if (compare(immutableNeighbor, lead) < 0) {
                        lead = immutableNeighbor;
                    }
                }
            }
        }
        return new Cluster(lead, visited);
    }

    // Any stable total ordering works; pick y, then x, then z.
    private static int compare(BlockPos a, BlockPos b) {
        if (a.getY() != b.getY()) {
            return Integer.compare(a.getY(), b.getY());
        }
        if (a.getX() != b.getX()) {
            return Integer.compare(a.getX(), b.getX());
        }
        return Integer.compare(a.getZ(), b.getZ());
    }

    public record Cluster(BlockPos lead, Set<BlockPos> members) {
        public int size() {
            return members.size();
        }
    }

    // ----- container / menu plumbing -----

    @Override
    public int getContainerSize() {
        return 1;
    }

    // getItems() stays the panel's OWN storage — this is what gets serialized and dropped on break,
    // so a panel never persists or drops another panel's batteries. The read/extract methods below
    // are the ones that forward to the lead so the whole cluster shares one stash.
    @Override
    protected NonNullList<ItemStack> getItems() {
        return this.items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    // ----- shared extraction: any panel reads/draws from the lead's stash -----
    // Hoppers (and the menu) call these, so a hopper under ANY panel in the cluster pulls batteries
    // out of the single shared buffer held by the lead.

    @Override
    public boolean isEmpty() {
        return resolveLead().items.get(OUTPUT_SLOT).isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return resolveLead().items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        SolarPanelBlockEntity lead = resolveLead();
        ItemStack result = ContainerHelper.removeItem(lead.items, slot, count);
        if (!result.isEmpty()) {
            lead.setChanged();
        }
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        SolarPanelBlockEntity lead = resolveLead();
        ItemStack result = ContainerHelper.takeItem(lead.items, slot);
        if (!result.isEmpty()) {
            lead.setChanged();
        }
        return result;
    }

    // Drop only our own batteries when broken (not the shared stash, which lives on the lead). The lead
    // panel's own stash IS the shared one, so breaking the lead correctly spills the collected batteries.
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (this.level != null) {
            Containers.dropContents(this.level, pos, this.items);
        }
    }

    @Override
    protected Component getDefaultName() {
        return DEFAULT_NAME;
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new SolarPanelMenu(containerId, inventory, this, this.dataAccess);
    }

    // Output only: nothing can be inserted into a solar panel, but the battery can be pulled out.
    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return false;
    }

    @Override
    public int[] getSlotsForFace(Direction direction) {
        return OUTPUT_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction direction) {
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
        return true;
    }

    // ----- persistence -----

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, this.items);
        this.progress = input.getIntOr("Progress", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, this.items);
        output.putInt("Progress", this.progress);
    }
}
