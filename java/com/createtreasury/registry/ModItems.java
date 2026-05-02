package com.createtreasury.registry;

import com.createtreasury.TreasuryMod;
import com.createtreasury.item.CompanyLinkerItem;
import com.createtreasury.item.DebitCardItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, TreasuryMod.MOD_ID);

    public static final RegistryObject<Item> BRASS_COIN = ITEMS.register("brass_coin",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> ZINC_COIN = ITEMS.register("zinc_coin",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> ANDESITE_COIN = ITEMS.register("andesite_coin",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> DEBIT_CARD = ITEMS.register("debit_card",
            () -> new DebitCardItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> COMPANY_LINKER = ITEMS.register("company_linker",
            () -> new CompanyLinkerItem(new Item.Properties().stacksTo(1)));
}
