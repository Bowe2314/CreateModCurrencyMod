package com.createtreasury.block;

import com.createtreasury.economy.ATMSessionTracker;
import com.createtreasury.economy.BankAccountManager;
import com.createtreasury.economy.CardUtils;
import com.createtreasury.network.OpenBankingScreenPacket;
import com.createtreasury.network.TreasuryNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.UUID;

public class BrassATMBlock extends HorizontalDirectionalBlock implements EntityBlock {

    public BrassATMBlock(Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    // ── EntityBlock ───────────────────────────────────────────────────────────

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BrassATMBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                   BlockEntityType<T> type) {
        return null; // no per-tick logic needed
    }

    // ── Interaction ───────────────────────────────────────────────────────────

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                  InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            ItemStack heldCard = findActiveCard(player);
            if (!heldCard.isEmpty()) {
                handleBankingAccess(serverPlayer, heldCard, pos, level);
            } else {
                serverPlayer.sendSystemMessage(Component.literal(
                        "§eInsert an active debit card to use this ATM."));
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /**
     * Validates the card and ATM state, registers the session, then opens the banking screen.
     */
    private static void handleBankingAccess(ServerPlayer player, ItemStack card,
                                             BlockPos atmPos, Level level) {
        if (CardUtils.isLocked(card)) {
            player.sendSystemMessage(Component.literal(
                    "§cYour card is locked. Contact your bank to have it unlocked."));
            return;
        }

        UUID holderUUID = CardUtils.getHolderUUID(card);
        if (holderUUID == null || !holderUUID.equals(player.getUUID())) {
            player.sendSystemMessage(Component.literal("§cThis card belongs to someone else."));
            return;
        }

        String bank = CardUtils.getIssuedBy(card);
        if (bank == null || bank.isEmpty()) {
            player.sendSystemMessage(Component.literal("§cThis card has no associated bank."));
            return;
        }

        // If the ATM is linked to a specific bank, only that bank's cards are accepted.
        BlockEntity be = level.getBlockEntity(atmPos);
        if (be instanceof BrassATMBlockEntity atmBE) {
            String linkedBank = atmBE.getLinkedBank();
            if (linkedBank != null && !linkedBank.equals(bank)) {
                player.sendSystemMessage(Component.literal(
                        "§cThis ATM is linked to §f" + linkedBank
                        + "§c. Your card is issued by §f" + bank + "§c."));
                return;
            }
        }

        // Register the session so deposit/withdraw packets can find this ATM.
        ATMSessionTracker.start(player.getUUID(), atmPos);

        BankAccountManager mgr = BankAccountManager.get(player.getServer());
        mgr.ensureAccount(bank, player.getUUID());
        long balance = mgr.getBalance(bank, player.getUUID());
        boolean hasPin = CardUtils.hasPin(card);

        TreasuryNetwork.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new OpenBankingScreenPacket(bank, balance, hasPin));
    }

    private static ItemStack findActiveCard(Player player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (CardUtils.isActive(stack)) return stack;
        }
        return ItemStack.EMPTY;
    }
}
