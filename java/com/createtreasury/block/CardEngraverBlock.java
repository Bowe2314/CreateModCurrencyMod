package com.createtreasury.block;

import com.createtreasury.economy.CardUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

public class CardEngraverBlock extends HorizontalDirectionalBlock {

    public CardEngraverBlock(Properties properties) {
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

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                  InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;

        if (level.isClientSide) {
            ItemStack card = player.getMainHandItem();
            if (CardUtils.isIssued(card) && !CardUtils.isEngraved(card)) {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                        net.minecraft.client.Minecraft.getInstance().setScreen(
                                new com.createtreasury.gui.CardEngraverScreen(card)));
            } else if (CardUtils.isEngraved(card)) {
                player.displayClientMessage(
                        net.minecraft.network.chat.Component.literal(
                                "§eThis card is already engraved for §f"
                                + CardUtils.getHolderName(card) + "§e."), true);
            } else {
                player.displayClientMessage(
                        net.minecraft.network.chat.Component.literal(
                                "§cHold an issued (unpersonalised) debit card."), true);
            }
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
