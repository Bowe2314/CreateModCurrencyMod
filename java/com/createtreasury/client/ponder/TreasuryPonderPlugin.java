package com.createtreasury.client.ponder;

import com.createtreasury.TreasuryMod;
import com.createtreasury.registry.ModBlocks;
import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;

public class TreasuryPonderPlugin implements PonderPlugin {

    @Override
    public String getModId() {
        return TreasuryMod.MOD_ID;
    }

    @Override
    public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        // Map Block → ResourceLocation so we can pass block instances directly
        PonderSceneRegistrationHelper<Block> blockHelper =
                helper.withKeyFunction(b -> ForgeRegistries.BLOCKS.getKey(b));

        blockHelper.forComponents(ModBlocks.MINTING_PRESS.get())
                .addStoryBoard("minting_press", MintingPressPonder::mintCoins)
                .addStoryBoard("minting_press", MintingPressPonder::linkToBank);

        blockHelper.forComponents(ModBlocks.COMPANY_TERMINAL.get())
                .addStoryBoard("company_terminal", CompanyTerminalPonder::howItWorks);
    }
}
