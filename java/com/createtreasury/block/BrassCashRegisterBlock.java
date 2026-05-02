package com.createtreasury.block;

import com.createtreasury.company.CompanyManager;
import com.createtreasury.network.OpenCashRegisterPacket;
import com.createtreasury.network.TreasuryNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
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

public class BrassCashRegisterBlock extends HorizontalDirectionalBlock implements EntityBlock {

    public BrassCashRegisterBlock(Properties properties) {
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
        return new BrassCashRegisterBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                    BlockEntityType<T> type) {
        return null;
    }

    // ── Interaction ───────────────────────────────────────────────────────────

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                  InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            BlockEntity be = level.getBlockEntity(pos);
            if (!(be instanceof BrassCashRegisterBlockEntity cr)) return InteractionResult.PASS;
            boolean owner = isOwner(sp, cr.getLinkedCompany());
            TreasuryNetwork.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> sp),
                    new OpenCashRegisterPacket(pos, cr, owner));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /**
     * Returns true if the player is considered an owner of the register.
     * Any player is an owner when no company is linked; otherwise they must
     * be a member of the linked company.
     */
    public static boolean isOwner(ServerPlayer player, @Nullable String linkedCompany) {
        if (linkedCompany == null || linkedCompany.isEmpty()) return true;
        CompanyManager mgr = CompanyManager.get(player.getServer());
        var company = mgr.getEntityByPlayer(player.getUUID(), "Company");
        return company != null && company.getName().equals(linkedCompany);
    }
}
