package com.createtreasury.client;

import com.createtreasury.TreasuryMod;
import com.createtreasury.block.CardPressRenderer;
import com.createtreasury.block.MintingPressRenderer;
import com.createtreasury.client.ponder.TreasuryPonderPlugin;
import com.createtreasury.registry.ModBlockEntityTypes;
import com.createtreasury.registry.ModBlocks;
import com.simibubi.create.foundation.item.KineticStats;
import com.simibubi.create.foundation.item.TooltipModifier;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.ponder.foundation.PonderIndex;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = TreasuryMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class TreasuryClientEvents {

    // Declaring this field registers the partial model with Flywheel before model baking
    public static final PartialModel MINTING_PRESS_HEAD =
            PartialModel.of(new ResourceLocation(TreasuryMod.MOD_ID, "block/minting_press/head"));

    public static final PartialModel CARD_PRESS_HEAD =
            PartialModel.of(new ResourceLocation(TreasuryMod.MOD_ID, "block/card_press/head"));

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(
                ModBlockEntityTypes.MINTING_PRESS.get(),
                MintingPressRenderer::new
        );
        event.registerBlockEntityRenderer(
                ModBlockEntityTypes.CARD_PRESS.get(),
                CardPressRenderer::new
        );
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            PonderIndex.addPlugin(new TreasuryPonderPlugin());

            // Register the kinetic stress tooltip for the Minting Press.
            // Create does this automatically for its own blocks via Registrate;
            // addon mods must do it manually in client setup.
            TooltipModifier.REGISTRY.register(
                    ModBlocks.MINTING_PRESS.get().asItem(),
                    new KineticStats(ModBlocks.MINTING_PRESS.get())
            );
            TooltipModifier.REGISTRY.register(
                    ModBlocks.CARD_PRESS.get().asItem(),
                    new KineticStats(ModBlocks.CARD_PRESS.get())
            );
        });
    }
}
