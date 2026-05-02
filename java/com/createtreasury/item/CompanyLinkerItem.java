package com.createtreasury.item;

import com.createtreasury.block.BrassATMBlock;
import com.createtreasury.block.BrassCashRegisterBlock;
import com.createtreasury.block.CardPressBlock;
import com.createtreasury.block.CompanyTerminalBlock;
import com.createtreasury.block.MintingPressBlock;
import com.createtreasury.company.Company;
import com.createtreasury.company.CompanyManager;
import com.createtreasury.network.LinkATMPacket;
import com.createtreasury.network.LinkCashRegisterPacket;
import com.createtreasury.network.LinkCardPressPacket;
import com.createtreasury.network.LinkMintingPressPacket;
import com.createtreasury.network.OpenLinkerScreenPacket;
import com.createtreasury.network.SyncCompanyPacket;
import com.createtreasury.network.TreasuryNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.List;
import java.util.stream.Collectors;

public class CompanyLinkerItem extends Item {

    public static final String TAG_SELECTED_TYPE = "SelectedType";
    public static final String TAG_SELECTED_NAME = "SelectedName";

    public CompanyLinkerItem(Properties properties) {
        super(properties);
    }

    /** Sneak + right-click on a Minting Press or Card Press → send link packet to server. */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        if (!player.isShiftKeyDown()) return InteractionResult.PASS;

        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof MintingPressBlock) {
            if (level.isClientSide) {
                TreasuryNetwork.CHANNEL.sendToServer(new LinkMintingPressPacket(pos));
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (state.getBlock() instanceof CardPressBlock) {
            if (level.isClientSide) {
                TreasuryNetwork.CHANNEL.sendToServer(new LinkCardPressPacket(pos));
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (state.getBlock() instanceof BrassATMBlock) {
            if (level.isClientSide) {
                TreasuryNetwork.CHANNEL.sendToServer(new LinkATMPacket(pos));
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (state.getBlock() instanceof BrassCashRegisterBlock) {
            if (level.isClientSide) {
                TreasuryNetwork.CHANNEL.sendToServer(new LinkCashRegisterPacket(pos));
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        return InteractionResult.PASS;
    }

    /**
     * Right-click in air: the SERVER syncs company data to the client first,
     * then sends {@link OpenLinkerScreenPacket} to open the screen.
     * This guarantees {@link com.createtreasury.client.ClientCompanyCache} is
     * populated before the screen renders no more missing Select buttons.
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            // 1. Push fresh company/bank data (arrives at client before the next packet)
            syncCompanyData(serverPlayer);
            // 2. Tell the client to open the screen (arrives after the sync packets)
            TreasuryNetwork.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> serverPlayer),
                    new OpenLinkerScreenPacket());
        }

        return InteractionResultHolder.success(stack);
    }

    /** Mirrors the sync logic in {@link CompanyTerminalBlock}. */
    private static void syncCompanyData(ServerPlayer player) {
        CompanyManager mgr = CompanyManager.get(player.getServer());
        String pendingFrom = mgr.getPendingInviteName(player.getUUID());

        for (String type : new String[]{"Company", "Bank"}) {
            Company entity = mgr.getEntityByPlayer(player.getUUID(), type);
            SyncCompanyPacket packet;
            if (entity != null) {
                List<String> memberNames = entity.getMembers().stream()
                        .map(uuid -> {
                            ServerPlayer p = player.getServer().getPlayerList().getPlayer(uuid);
                            return p != null ? p.getName().getString()
                                    : uuid.toString().substring(0, 8) + "…";
                        })
                        .collect(Collectors.toList());
                ServerPlayer ownerPlayer = player.getServer().getPlayerList()
                        .getPlayer(entity.getOwnerUUID());
                String ownerName = ownerPlayer != null ? ownerPlayer.getName().getString()
                        : entity.getOwnerUUID().toString().substring(0, 8) + "…";
                packet = new SyncCompanyPacket(
                        entity.getName(), entity.getTagline(), entity.getIndustry(),
                        entity.getType(), entity.getOwnerUUID().equals(player.getUUID()),
                        ownerName, memberNames, pendingFrom);
            } else {
                packet = new SyncCompanyPacket(null, null, null, type, false, null,
                        List.of(), pendingFrom);
            }
            TreasuryNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(TAG_SELECTED_NAME)) {
            String name = tag.getString(TAG_SELECTED_NAME);
            String type = tag.getString(TAG_SELECTED_TYPE);
            tooltip.add(Component.literal("§7Linked: §f" + name + " §7[" + type + "]"));
        } else {
            tooltip.add(Component.literal("§7Linked: §8None"));
        }
        tooltip.add(Component.literal("§8Right-click to select an entity"));
        tooltip.add(Component.literal("§8Sneak + right-click a Minting/Card Press, Brass ATM, or Cash Register to link"));
    }

    @Nullable
    public static String getSelectedName(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return (tag != null && tag.contains(TAG_SELECTED_NAME)) ? tag.getString(TAG_SELECTED_NAME) : null;
    }

    @Nullable
    public static String getSelectedType(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return (tag != null && tag.contains(TAG_SELECTED_TYPE)) ? tag.getString(TAG_SELECTED_TYPE) : null;
    }
}
