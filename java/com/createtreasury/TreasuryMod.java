package com.createtreasury;

import com.createtreasury.config.TreasuryConfig;
import com.createtreasury.config.TreasuryServerConfig;
import com.simibubi.create.api.stress.BlockStressValues;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraft.world.item.CreativeModeTab;
import com.createtreasury.gui.CompanyTerminalMenu;
import com.createtreasury.gui.CompanyTerminalScreen;
import com.createtreasury.recipe.ModRecipeTypes;
import com.createtreasury.registry.ModBlockEntityTypes;
import com.createtreasury.registry.ModBlocks;
import com.createtreasury.registry.ModItems;
import com.createtreasury.registry.ModMenuTypes;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

@Mod(TreasuryMod.MOD_ID)
public class TreasuryMod {

    public static final String MOD_ID = "createtreasury";
    public static final Logger LOGGER = LogUtils.getLogger();

    // Register for creative tabs
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);

    // The tab itself
    public static final RegistryObject<CreativeModeTab> TREASURY_TAB = CREATIVE_TABS.register("treasury_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.createtreasury"))
                    .icon(() -> ModBlocks.COMPANY_TERMINAL.get().asItem().getDefaultInstance()) // placeholder icon for now
                    .displayItems((params, output) -> {
                        output.accept(ModItems.BRASS_COIN.get());
                        output.accept(ModItems.ZINC_COIN.get());
                        output.accept(ModItems.ANDESITE_COIN.get());
                        output.accept(ModItems.DEBIT_CARD.get());
                        output.accept(ModBlocks.COMPANY_TERMINAL.get().asItem());
                        output.accept(ModBlocks.MINTING_PRESS.get().asItem());
                        output.accept(ModBlocks.CARD_PRESS.get().asItem());
                        output.accept(ModBlocks.CARD_ENGRAVER.get().asItem());
                        output.accept(ModBlocks.BRASS_ATM.get().asItem());
                        output.accept(ModBlocks.BRASS_CASH_REGISTER.get().asItem());
                        output.accept(ModItems.COMPANY_LINKER.get());
                    })
                    .build());

    public TreasuryMod(FMLJavaModLoadingContext context) {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, TreasuryConfig.CLIENT_SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, TreasuryServerConfig.SERVER_SPEC);

        IEventBus modEventBus = context.getModEventBus();

        modEventBus.addListener(this::commonSetup);

        // Register blocks, items and creative tab
        ModItems.ITEMS.register(modEventBus);
        ModBlocks.BLOCKS.register(modEventBus);
        ModBlockEntityTypes.BLOCK_ENTITIES.register(modEventBus);
        ModRecipeTypes.SERIALIZERS.register(modEventBus);
        ModRecipeTypes.TYPES.register(modEventBus);
        ModMenuTypes.MENUS.register(modEventBus);
        CREATIVE_TABS.register(modEventBus);

        modEventBus.addListener(this::clientSetup);

        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            com.createtreasury.network.TreasuryNetwork.register();
            // Register stress impact (same as Mechanical Press: 8 SU)
            BlockStressValues.IMPACTS.register(ModBlocks.MINTING_PRESS.get(), () -> 8.0);
            BlockStressValues.IMPACTS.register(ModBlocks.CARD_PRESS.get(),    () -> 8.0);
        });
        LOGGER.info("Create: Treasury initialized.");
    }

    private void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() ->
                MenuScreens.register(ModMenuTypes.COMPANY_TERMINAL.get(), CompanyTerminalScreen::new));
    }
}
