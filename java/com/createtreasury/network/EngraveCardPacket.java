package com.createtreasury.network;

import com.createtreasury.company.Company;
import com.createtreasury.company.CompanyManager;
import com.createtreasury.config.TreasuryServerConfig;
import com.createtreasury.economy.CardUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Sent client → server when a player confirms engraving in the Card Engraver screen.
 * Writes {@link CardUtils#TAG_HOLDER_NAME} onto the issued card in the player's main hand.
 */
public class EngraveCardPacket {

    private final String username;

    public EngraveCardPacket(String username) {
        this.username = username;
    }

    public static void encode(EngraveCardPacket pkt, FriendlyByteBuf buf) {
        buf.writeUtf(pkt.username, 16);
    }

    public static EngraveCardPacket decode(FriendlyByteBuf buf) {
        return new EngraveCardPacket(buf.readUtf(16));
    }

    public static void handle(EngraveCardPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            String username = pkt.username.trim();
            if (username.isEmpty() || username.length() > 16) return;

            // Only valid Minecraft username characters
            if (!username.matches("[a-zA-Z0-9_]+")) return;

            ItemStack card = player.getMainHandItem();
            if (card.isEmpty()) return;
            if (!CardUtils.isIssued(card)) {
                player.sendSystemMessage(Component.literal(
                        "§cYou must hold an issued debit card to engrave it."));
                return;
            }
            if (CardUtils.isEngraved(card) && !TreasuryServerConfig.SERVER.allowReEngrave.get()) {
                player.sendSystemMessage(Component.literal(
                        "§cThis card is already engraved. Re-engraving is disabled on this server."));
                return;
            }

            // Enforce bank-member-only engraving if configured
            if (TreasuryServerConfig.SERVER.requireBankMemberToEngrave.get()) {
                String cardBank = CardUtils.getIssuedBy(card);
                Company playerBank = CompanyManager.get(player.getServer())
                        .getEntityByPlayer(player.getUUID(), "Bank");
                if (playerBank == null || !playerBank.getName().equals(cardBank)) {
                    player.sendSystemMessage(Component.literal(
                            "§cOnly members of §f" + cardBank + "§c may engrave this card."));
                    return;
                }
            }

            // Validate the target player is currently online
            net.minecraft.server.level.ServerPlayer target =
                    player.getServer().getPlayerList().getPlayerByName(username);
            if (target == null) {
                player.sendSystemMessage(Component.literal(
                        "§cPlayer '§f" + username + "§c' is not online. Ask them to join first."));
                return;
            }

            card.getOrCreateTag().putString(CardUtils.TAG_HOLDER_NAME, target.getName().getString());

            player.sendSystemMessage(Component.literal(
                    "§aCard engraved for §f" + target.getName().getString()
                    + "§a. Hand it to them to activate."));
        });
        ctx.get().setPacketHandled(true);
    }
}
