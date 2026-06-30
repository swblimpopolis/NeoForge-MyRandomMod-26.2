package com.swblimpopolis.myrandommod.event;

import com.swblimpopolis.myrandommod.MyRandomMod;
import com.swblimpopolis.myrandommod.item.ElectricSwordItem;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;

// Game-bus handler that ties the electric sword's combat to its charge: each landed hit spends a charge,
// and a fully drained sword can't attack at all until it's recharged. (@EventBusSubscriber with no bus
// auto-routes to the game bus in 26.2, since AttackEntityEvent is a game event.)
@EventBusSubscriber(modid = MyRandomMod.MODID)
public final class CombatEvents {
    private CombatEvents() {
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        Player player = event.getEntity();
        ItemStack weapon = player.getMainHandItem();
        if (!(weapon.getItem() instanceof ElectricSwordItem)) {
            return;
        }

        int charge = ElectricSwordItem.getCharge(weapon);
        if (charge <= 0) {
            // Empty: cancel the attack on both sides so it deals nothing until recharged.
            event.setCanceled(true);
            return;
        }

        // Spend charge for the hit (server-authoritative; the client's value is synced).
        if (!player.level().isClientSide()) {
            ElectricSwordItem.setCharge(weapon, charge - ElectricSwordItem.CHARGE_PER_HIT);
        }
    }
}
