package com.createtreasury.economy;

import com.createtreasury.config.TreasuryServerConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BankAccountManager extends SavedData {

    private static final String SAVE_NAME = "createtreasury_accounts";
    private static final String TAG_ACCOUNTS = "accounts";

    // bank name -> (player UUID string -> balance)
    private final Map<String, Map<UUID, Long>> accounts = new HashMap<>();

    // ── Factory ───────────────────────────────────────────────────────────────

    public static BankAccountManager get(MinecraftServer server) {
        DimensionDataStorage storage = server.overworld().getDataStorage();
        return storage.computeIfAbsent(BankAccountManager::load, BankAccountManager::new, SAVE_NAME);
    }

    // ── Public API ────────────────────────────────────────────────────────────

    public long getBalance(String bank, UUID player) {
        Map<UUID, Long> bankMap = accounts.get(bank);
        if (bankMap == null) return 0L;
        return bankMap.getOrDefault(player, 0L);
    }

    public void setBalance(String bank, UUID player, long amount) {
        long clamped = Math.max(0L, amount);
        accounts.computeIfAbsent(bank, k -> new HashMap<>()).put(player, clamped);
        setDirty();
    }

    /**
     * Adds {@code amount} to the account, capped by the configured maxBalance.
     * Returns the amount actually credited (may be less than requested if cap is hit).
     */
    public long addBalance(String bank, UUID player, long amount) {
        long current = getBalance(bank, player);
        long maxBal  = 0L;
        try { maxBal = TreasuryServerConfig.SERVER.maxBalance.get(); } catch (Exception ignored) {}

        long toAdd = amount;
        if (maxBal > 0) {
            long headroom = maxBal - current;
            if (headroom <= 0) return 0L;
            toAdd = Math.min(toAdd, headroom);
        }
        setBalance(bank, player, current + toAdd);
        return toAdd;
    }

    public boolean withdraw(String bank, UUID player, long amount) {
        long current = getBalance(bank, player);
        if (current < amount) return false;
        setBalance(bank, player, current - amount);
        return true;
    }

    public void ensureAccount(String bank, UUID player) {
        Map<UUID, Long> bankMap = accounts.computeIfAbsent(bank, k -> new HashMap<>());
        if (!bankMap.containsKey(player)) {
            long starting = 0L;
            try { starting = TreasuryServerConfig.SERVER.startingBalance.get(); } catch (Exception ignored) {}
            bankMap.put(player, Math.max(0L, starting));
            setDirty();
        }
    }

    // ── SavedData persistence ─────────────────────────────────────────────────

    @Override
    public CompoundTag save(CompoundTag tag) {
        CompoundTag accountsTag = new CompoundTag();
        for (Map.Entry<String, Map<UUID, Long>> bankEntry : accounts.entrySet()) {
            CompoundTag bankTag = new CompoundTag();
            for (Map.Entry<UUID, Long> playerEntry : bankEntry.getValue().entrySet()) {
                bankTag.putLong(playerEntry.getKey().toString(), playerEntry.getValue());
            }
            accountsTag.put(bankEntry.getKey(), bankTag);
        }
        tag.put(TAG_ACCOUNTS, accountsTag);
        return tag;
    }

    public static BankAccountManager load(CompoundTag tag) {
        BankAccountManager manager = new BankAccountManager();
        CompoundTag accountsTag = tag.getCompound(TAG_ACCOUNTS);
        for (String bankName : accountsTag.getAllKeys()) {
            CompoundTag bankTag = accountsTag.getCompound(bankName);
            Map<UUID, Long> bankMap = new HashMap<>();
            for (String uuidStr : bankTag.getAllKeys()) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    long balance = Math.max(0L, bankTag.getLong(uuidStr));
                    bankMap.put(uuid, balance);
                } catch (IllegalArgumentException ignored) {
                    // skip malformed UUID keys
                }
            }
            manager.accounts.put(bankName, bankMap);
        }
        return manager;
    }
}
