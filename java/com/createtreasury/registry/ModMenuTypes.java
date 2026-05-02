package com.createtreasury.registry;

import com.createtreasury.TreasuryMod;
import com.createtreasury.gui.CompanyTerminalMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModMenuTypes {

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, TreasuryMod.MOD_ID);

    public static final RegistryObject<MenuType<CompanyTerminalMenu>> COMPANY_TERMINAL =
            MENUS.register("company_terminal", () ->
                    IForgeMenuType.create(CompanyTerminalMenu::new));
}
