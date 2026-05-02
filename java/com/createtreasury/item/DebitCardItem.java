package com.createtreasury.item;

import com.createtreasury.client.ClientCompanyCache;
import com.createtreasury.economy.CardUtils;
import com.createtreasury.network.TreasuryNetwork;
import com.createtreasury.network.UnlockCardPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

import javax.annotation.Nullable;
import java.util.List;

public class DebitCardItem extends Item {

    public DebitCardItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide) {
            if (CardUtils.isLocked(stack)) {
                String issuedBy = CardUtils.getIssuedBy(stack);
                boolean isBankMember = ClientCompanyCache.hasBank()
                        && issuedBy != null
                        && issuedBy.equals(ClientCompanyCache.bankName);
                if (isBankMember) {
                    TreasuryNetwork.CHANNEL.sendToServer(new UnlockCardPacket());
                    return InteractionResultHolder.success(stack);
                }
                player.displayClientMessage(
                        Component.literal("§cThis card is locked. Contact your bank to unlock it."),
                        true);
                return InteractionResultHolder.fail(stack);
            }
            if (CardUtils.isEngraved(stack) && !CardUtils.isActive(stack)) {
                // Check if this card was engraved for this player
                String engravedFor = CardUtils.getHolderName(stack);
                String myName = Minecraft.getInstance().player != null
                        ? Minecraft.getInstance().player.getName().getString() : "";
                if (engravedFor == null || engravedFor.equalsIgnoreCase(myName)) {
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                            Minecraft.getInstance().setScreen(
                                    new com.createtreasury.gui.ActivateCardScreen(hand, stack)));
                } else {
                    player.displayClientMessage(
                            Component.literal("§cThis card was engraved for §f" + engravedFor + "§c.")
                                    .withStyle(ChatFormatting.RED), true);
                }
            } else if (CardUtils.isIssued(stack) && !CardUtils.isEngraved(stack)) {
                player.displayClientMessage(
                        Component.literal("§eThis card needs to be engraved first at a §fCard Engraver§e."),
                        true);
            } else if (CardUtils.isBlank(stack)) {
                player.displayClientMessage(
                        Component.translatable("createtreasury.debit_card.blank_hint")
                                .withStyle(ChatFormatting.YELLOW), true);
            }
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        if (CardUtils.isActive(stack)) {
            tooltip.add(Component.literal("§7Bank: §f" + CardUtils.getIssuedBy(stack)));
            tooltip.add(Component.literal("§7Holder: §f" + CardUtils.getHolderName(stack)));
            if (CardUtils.isLocked(stack)) {
                tooltip.add(Component.literal("§c[LOCKED - contact your bank]"));
            } else {
                if (CardUtils.hasPin(stack)) {
                    tooltip.add(Component.literal("§8[PIN protected]"));
                }
                tooltip.add(Component.literal("§aActive - use at a Brass ATM"));
            }
        } else if (CardUtils.isEngraved(stack)) {
            tooltip.add(Component.literal("§7Bank: §f" + CardUtils.getIssuedBy(stack)));
            tooltip.add(Component.literal("§7Engraved for: §f" + CardUtils.getHolderName(stack)));
            tooltip.add(Component.literal("§eRight-click to activate"));
        } else if (CardUtils.isIssued(stack)) {
            tooltip.add(Component.literal("§7Issued by: §f" + CardUtils.getIssuedBy(stack)));
            tooltip.add(Component.literal("§eNeeds engraving - use a Card Engraver"));
        } else {
            tooltip.add(Component.literal("§8Blank - press in a Card Press to issue"));
        }
    }
}
