package com.createtreasury.economy;

import com.createtreasury.block.BrassATMBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Server-side only: tracks which ATM block position each player currently has open.
 * Cleared when a session ends (player closes screen or uses another ATM).
 * Deposit and withdraw packets use this to find the ATM's BlockEntity.
 */
public class ATMSessionTracker {

    private static final Map<UUID, BlockPos> sessions = new HashMap<>();

    /** Called when a player opens the banking screen at an ATM. */
    public static void start(UUID playerUUID, BlockPos atmPos) {
        sessions.put(playerUUID, atmPos);
    }

    /** Called when a player's banking session is known to have ended. */
    public static void end(UUID playerUUID) {
        sessions.remove(playerUUID);
    }

    /**
     * Returns the BrassATMBlockEntity the player has open, or null if there is no
     * active session or the block entity is gone/unloaded.
     */
    @Nullable
    public static BrassATMBlockEntity getATM(ServerPlayer player) {
        BlockPos pos = sessions.get(player.getUUID());
        if (pos == null) return null;
        BlockEntity be = player.level().getBlockEntity(pos);
        return be instanceof BrassATMBlockEntity atm ? atm : null;
    }
}
