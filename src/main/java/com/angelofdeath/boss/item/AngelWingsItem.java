package com.angelofdeath.boss.item;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Chest-slot wings. Flight, fall immunity and the boost live in WingsEvents. */
public class AngelWingsItem extends Item implements Equipable {
    public AngelWingsItem(Properties properties) {
        super(properties);
    }

    @Override
    public EquipmentSlot getEquipmentSlot() {
        return EquipmentSlot.CHEST;
    }

    @Override
    public EquipmentSlot getEquipmentSlot(ItemStack stack) {
        return EquipmentSlot.CHEST;
    }

    public static boolean isWearing(net.minecraft.world.entity.LivingEntity entity) {
        return entity.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof AngelWingsItem;
    }
}
