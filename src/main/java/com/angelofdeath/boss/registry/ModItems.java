package com.angelofdeath.boss.registry;

import com.angelofdeath.boss.AngelOfDeathBoss;
import com.angelofdeath.boss.item.AngelWingsItem;
import com.angelofdeath.boss.item.WhiteHoleSpearItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.Unbreakable;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(AngelOfDeathBoss.MOD_ID);
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, AngelOfDeathBoss.MOD_ID);

    public static final DeferredItem<Item> THE_SINGULARITY =
            ITEMS.register("the_singularity", () -> new Item(new Item.Properties().rarity(Rarity.EPIC).stacksTo(1)));
    public static final DeferredItem<Item> BLACK_FEATHER =
            ITEMS.register("black_feather", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> WHITE_FEATHER =
            ITEMS.register("white_feather", () -> new Item(new Item.Properties()));
    public static final DeferredItem<AngelWingsItem> ANGEL_OF_DEATH_WINGS = ITEMS.register("angel_of_death_wings",
            () -> new AngelWingsItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    public static final DeferredItem<WhiteHoleSpearItem> WHITE_HOLE_SPEAR = ITEMS.register("white_hole_spear",
            () -> new WhiteHoleSpearItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()
                    .component(DataComponents.UNBREAKABLE, new Unbreakable(true))
                    .attributes(WhiteHoleSpearItem.attributes())));

    public static final DeferredItem<DeferredSpawnEggItem> ANGEL_OF_DEATH_SPAWN_EGG = ITEMS.register(
            "angel_of_death_spawn_egg", () -> new DeferredSpawnEggItem(ModEntities.ANGEL_OF_DEATH, 0xF2F2F2, 0x111111,
                    new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> BLACK_CROW_SPAWN_EGG = ITEMS.register(
            "black_crow_spawn_egg", () -> new DeferredSpawnEggItem(ModEntities.BLACK_CROW, 0x111111, 0x4B2A6B,
                    new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> WHITE_CROW_SPAWN_EGG = ITEMS.register(
            "white_crow_spawn_egg", () -> new DeferredSpawnEggItem(ModEntities.WHITE_CROW, 0xF2F2F2, 0xC9D6FF,
                    new Item.Properties()));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("angel_of_death",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.angelofdeathboss"))
                    .icon(() -> THE_SINGULARITY.get().getDefaultInstance())
                    .displayItems((params, output) -> ITEMS.getEntries().forEach(e -> output.accept(e.get())))
                    .build());

    private ModItems() {
    }
}
