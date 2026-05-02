package com.createtreasury.block;

import com.createtreasury.company.Company;
import com.createtreasury.company.CompanyManager;
import com.createtreasury.gui.CompanyTerminalMenu;
import com.createtreasury.network.SyncCompanyPacket;
import com.createtreasury.network.TreasuryNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PacketDistributor;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class CompanyTerminalBlock extends HorizontalDirectionalBlock {

    public CompanyTerminalBlock(Properties properties) {
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
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            syncCompanyData(serverPlayer);
            NetworkHooks.openScreen(serverPlayer, new MenuProvider() {
                @Override
                public Component getDisplayName() {
                    return Component.translatable("block.createtreasury.company_terminal");
                }
                @Override
                public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
                    return new CompanyTerminalMenu(id, inv);
                }
            }, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static void syncCompanyData(ServerPlayer player) {
        CompanyManager mgr = CompanyManager.get(player.getServer());

        // Resolve pending invite name once
        String pendingFrom = mgr.getPendingInviteName(player.getUUID());

        // Sync Company slot and Bank slot separately (each packet routes to its own client cache slot)
        for (String type : new String[]{"Company", "Bank"}) {
            Company entity = mgr.getEntityByPlayer(player.getUUID(), type);
            SyncCompanyPacket packet;
            if (entity != null) {
                List<String> memberNames = entity.getMembers().stream()
                        .map(uuid -> {
                            ServerPlayer p = player.getServer().getPlayerList().getPlayer(uuid);
                            return p != null ? p.getName().getString() : uuid.toString().substring(0, 8) + "…";
                        })
                        .collect(Collectors.toList());
                ServerPlayer ownerPlayer = player.getServer().getPlayerList().getPlayer(entity.getOwnerUUID());
                String ownerName = ownerPlayer != null ? ownerPlayer.getName().getString()
                        : entity.getOwnerUUID().toString().substring(0, 8) + "…";
                packet = new SyncCompanyPacket(entity.getName(), entity.getTagline(), entity.getIndustry(),
                        entity.getType(), entity.getOwnerUUID().equals(player.getUUID()),
                        ownerName, memberNames, pendingFrom);
            } else {
                // Clear this slot; preserve pending invite on the client
                packet = new SyncCompanyPacket(null, null, null, type, false, null, List.of(), pendingFrom);
            }
            TreasuryNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
        }
    }
}
