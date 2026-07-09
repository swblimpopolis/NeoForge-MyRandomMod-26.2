package com.swblimpopolis.myrandommod.item;

// Marker for all battery-powered tools/weapons. Lets the event handlers gate attacking/mining on charge
// regardless of which vanilla item class the tool extends (Item, AxeItem, ShovelItem, ...). The shared
// charge behaviour lives as static helpers in ElectricToolItem.
public interface ElectricTool {
}
