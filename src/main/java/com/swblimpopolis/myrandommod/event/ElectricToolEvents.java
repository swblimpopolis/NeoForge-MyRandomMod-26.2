package com.swblimpopolis.myrandommod.event;

import com.swblimpopolis.myrandommod.MyRandomMod;
import com.swblimpopolis.myrandommod.item.ElectricTool;
import com.swblimpopolis.myrandommod.item.ElectricToolItem;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

// Ties the electric tools' combat and mining to their charge: a landed hit or a mined block spends a
// charge, and a fully drained tool can neither attack nor mine until it's recharged. (@EventBusSubscriber
// with no bus auto-routes to the game bus in 26.2, since these are game events.)
@EventBusSubscriber(modid = MyRandomMod.MODID)
public final class ElectricToolEvents {
    private ElectricToolEvents() {
    }

    // Attacking: empty tools deal nothing; charged tools spend one charge per hit.
    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        Player player = event.getEntity();
        ItemStack weapon = player.getMainHandItem();
        if (!(weapon.getItem() instanceof ElectricTool)) {
            return;
        }
        int charge = ElectricToolItem.getCharge(weapon);
        if (charge <= 0) {
            event.setCanceled(true);
            return;
        }
        if (!player.level().isClientSide()) {
            ElectricToolItem.setCharge(weapon, charge - ElectricToolItem.CHARGE_PER_USE);
        }
    }

    // Mining: an empty tool can't break anything (charge is spent in ElectricToolItem.mineBlock once it
    // actually breaks a block, so we only gate here).
    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        ItemStack tool = event.getEntity().getMainHandItem();
        if (tool.getItem() instanceof ElectricTool && ElectricToolItem.getCharge(tool) <= 0) {
            event.setCanceled(true);
        }
    }
}
