package com.swblimpopolis.myrandommod.datamap;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.util.ExtraCodecs;

/**
 * Value type for the {@code myrandommod:electric_fuel} data map: how long (in ticks) an item
 * powers the electric furnace. Modelled after NeoForge's built-in {@code FurnaceFuel} so the JSON
 * accepts either {@code {"burn_time": 1600}} or the shorthand {@code 1600}.
 *
 * @param burnTime how long (in ticks) the item will burn for
 */
public record ElectricFuel(int burnTime) {
    public static final Codec<ElectricFuel> BURN_TIME_CODEC = ExtraCodecs.POSITIVE_INT
            .xmap(ElectricFuel::new, ElectricFuel::burnTime);

    public static final Codec<ElectricFuel> CODEC = Codec.withAlternative(
            RecordCodecBuilder.create(in -> in.group(
                    ExtraCodecs.POSITIVE_INT.fieldOf("burn_time").forGetter(ElectricFuel::burnTime)
            ).apply(in, ElectricFuel::new)),
            BURN_TIME_CODEC);
}
