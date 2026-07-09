package com.swblimpopolis.myrandommod.item;

// The battery-powered sword. All behaviour lives in ElectricToolItem; this subclass just exists so the
// sword keeps its own type/registration. Diamond-tier melee that does nothing at zero charge.
public class ElectricSwordItem extends ElectricToolItem {
    public ElectricSwordItem(Properties properties) {
        super(properties);
    }

    public static Properties baseProperties() {
        return swordProperties();
    }
}
