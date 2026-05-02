package com.createtreasury.client;

import com.createtreasury.TreasuryMod;
import com.createtreasury.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client-side events registered on the FORGE event bus (as opposed to the MOD bus).
 * Handles dynamic item tooltips that depend on item NBT at runtime.
 */
@Mod.EventBusSubscriber(modid = TreasuryMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class TreasuryClientForgeEvents {

    /**
     * Appends "Minted by: BankName" to coins that carry the MintedBy NBT tag.
     * This tag is written by {@link com.createtreasury.block.MintingPressBlockEntity}
     * when a linked Minting Press stamps the coins.
     */
    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (!isCoin(stack.getItem())) return;

        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains("MintedBy")) return;

        String bank = tag.getString("MintedBy");
        if (!bank.isBlank()) {
            event.getToolTip().add(Component.literal("§7Minted by: §f" + bank));
        }
    }

    private static boolean isCoin(Item item) {
        return item == ModItems.BRASS_COIN.get()
                || item == ModItems.ZINC_COIN.get()
                || item == ModItems.ANDESITE_COIN.get();
    }
}
