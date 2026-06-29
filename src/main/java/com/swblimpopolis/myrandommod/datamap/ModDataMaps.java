package com.swblimpopolis.myrandommod.datamap;

import com.swblimpopolis.myrandommod.MyRandomMod;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;

// Data maps owned by this mod. RegisterDataMapTypesEvent is an IModBusEvent, so @EventBusSubscriber
// routes it to the mod bus automatically.
@EventBusSubscriber(modid = MyRandomMod.MODID)
public final class ModDataMaps {
    /**
     * Item -> electric fuel burn time, loaded from {@code data/<ns>/data_maps/item/electric_fuel.json}.
     * Having an entry here is what makes an item valid fuel for the electric furnace, and the entry's
     * {@code burn_time} is how long it burns — exactly how vanilla treats {@code neoforge:furnace_fuels}.
     */
    public static final DataMapType<Item, ElectricFuel> ELECTRIC_FUEL = DataMapType
            .builder(Identifier.fromNamespaceAndPath(MyRandomMod.MODID, "electric_fuel"), Registries.ITEM, ElectricFuel.CODEC)
            .synced(ElectricFuel.BURN_TIME_CODEC, false)
            .build();

    private ModDataMaps() {}

    @SubscribeEvent
    static void onRegisterDataMaps(RegisterDataMapTypesEvent event) {
        event.register(ELECTRIC_FUEL);
    }
}
