package com.createtreasury.network;

import com.createtreasury.config.TreasuryServerConfig;
import com.createtreasury.economy.BankAccountManager;
import com.createtreasury.economy.CardUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Sent client → server when the player confirms card activation.
 * Validates that the activating player's name matches the engraved holder name,
 * then writes HolderUUID / Active / PIN to the card's NBT.
 */
public class ActivateCardPacket {

    private final InteractionHand hand;
    private final String          pin; // empty string = no PIN

    public ActivateCardPacket(InteractionHand hand, String pin) {
        this.hand = hand;
        this.pin  = pin;
    }

    public static void encode(ActivateCardPacket pkt, FriendlyByteBuf buf) {
        buf.writeEnum(pkt.hand);
        buf.writeUtf(pkt.pin, 32);
    }

    public static ActivateCardPacket decode(FriendlyByteBuf buf) {
        return new ActivateCardPacket(buf.readEnum(InteractionHand.class), buf.readUtf(32));
    }

    public static void handle(ActivateCardPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            ItemStack stack = player.getItemInHand(pkt.hand);
            if (stack.isEmpty()) return;

            // Card must be engraved but not yet active
            if (!CardUtils.isEngraved(stack) || CardUtils.isActive(stack)) return;

            String bank = CardUtils.getIssuedBy(stack);
            if (bank == null || bank.isEmpty()) return;

            // The card was engraved for a specific player enforce that only they can activate it
            String engravedFor = CardUtils.getHolderName(stack);
            if (engravedFor != null && !engravedFor.equalsIgnoreCase(player.getName().getString())) {
                player.sendSystemMessage(Component.literal(
                        "§cThis card was engraved for §f" + engravedFor + "§c, not you."));
                return;
            }

            // Multiple-cards-per-bank guard
            if (!TreasuryServerConfig.SERVER.allowMultipleCardsPerBank.get()) {
                String bankName = CardUtils.getIssuedBy(stack);
                if (bankName != null) {
                    for (ItemStack invStack : player.getInventory().items) {
                        if (invStack == stack) continue;
                        if (CardUtils.isActive(invStack) && bankName.equals(CardUtils.getIssuedBy(invStack))) {
                            player.sendSystemMessage(Component.literal(
                                    "§cYou already have an active card for §f" + bankName + "§c."));
                            return;
                        }
                    }
                }
            }

            // PIN gate
            String pin = pkt.pin.trim();
            boolean pinsEnabled = TreasuryServerConfig.SERVER.enablePins.get();
            if (pinsEnabled) {
                if (TreasuryServerConfig.SERVER.requirePinOnActivation.get() && pin.isEmpty()) {
                    player.sendSystemMessage(Component.literal("§cA PIN is required to activate this card."));
                    return;
                }
                if (!pin.isEmpty()) {
                    int minLen = TreasuryServerConfig.SERVER.pinMinLength.get();
                    int maxLen = TreasuryServerConfig.SERVER.pinMaxLength.get();
                    if (pin.length() < minLen || pin.length() > maxLen) {
                        player.sendSystemMessage(Component.literal(
                                "§cPIN must be §f" + minLen + "–" + maxLen + "§c digits."));
                        return;
                    }
                }
            } else {
                pin = ""; // PINs disabled discard any supplied value
            }
            // Write UUID (name already on card from engraving)
            stack.getOrCreateTag().putString(CardUtils.TAG_HOLDER_UUID, player.getUUID().toString());
            // Normalise the stored name to match actual capitalisation
            stack.getOrCreateTag().putString(CardUtils.TAG_HOLDER_NAME, player.getName().getString());
            stack.getOrCreateTag().putBoolean(CardUtils.TAG_ACTIVE, true);
            if (!pin.isEmpty()) {
                stack.getOrCreateTag().putString(CardUtils.TAG_PIN, pin);
            }

            BankAccountManager.get(player.getServer()).ensureAccount(bank, player.getUUID());

            player.sendSystemMessage(Component.literal(
                    "§aDebit card activated! Use it at a §fBrass ATM§a to access your account."));
        });
        ctx.get().setPacketHandled(true);
    }
}
