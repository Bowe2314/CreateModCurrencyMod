package com.createtreasury.block;

import com.createtreasury.registry.ModBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

public class BrassCashRegisterBlockEntity extends BlockEntity {

    public static final int SHOP_SLOTS = 3;

    private final ItemStack[] shopItems = {ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY};
    private final long[]      prices    = new long[SHOP_SLOTS];
    private long   coinStash   = 0;
    @Nullable private String linkedCompany;

    public BrassCashRegisterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.CASH_REGISTER.get(), pos, state);
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    public ItemStack[] getShopItems() { return shopItems; }
    public long[]      getPrices()    { return prices; }
    public long        getCoinStash() { return coinStash; }
    @Nullable public String getLinkedCompany() { return linkedCompany; }

    public void setShopItem(int slot, ItemStack stack) {
        shopItems[slot] = stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
        setChanged();
    }

    public void setPrice(int slot, long price) {
        prices[slot] = Math.max(0, price);
        setChanged();
    }

    public void setPrices(long[] newPrices) {
        for (int i = 0; i < SHOP_SLOTS; i++) {
            prices[i] = Math.max(0, newPrices[i]);
        }
        setChanged();
    }

    public void addToStash(long amount) {
        coinStash += amount;
        setChanged();
    }

    public long drainStash() {
        long amount = coinStash;
        coinStash = 0;
        setChanged();
        return amount;
    }

    public void setLinkedCompany(@Nullable String name) {
        linkedCompany = (name == null || name.isEmpty()) ? null : name;
        setChanged();
    }

    // ── Persistence ───────────────────────────────────────────────────────────

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (linkedCompany != null) tag.putString("LinkedCompany", linkedCompany);
        tag.putLong("CoinStash", coinStash);
        tag.putLongArray("Prices", prices);
        CompoundTag items = new CompoundTag();
        for (int i = 0; i < SHOP_SLOTS; i++) {
            if (!shopItems[i].isEmpty()) {
                items.put(String.valueOf(i), shopItems[i].save(new CompoundTag()));
            }
        }
        tag.put("ShopItems", items);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        linkedCompany = tag.contains("LinkedCompany") ? tag.getString("LinkedCompany") : null;
        if (linkedCompany != null && linkedCompany.isEmpty()) linkedCompany = null;
        coinStash = tag.getLong("CoinStash");
        long[] saved = tag.getLongArray("Prices");
        for (int i = 0; i < SHOP_SLOTS; i++) {
            prices[i] = (saved != null && i < saved.length) ? Math.max(0, saved[i]) : 0;
        }
        CompoundTag items = tag.getCompound("ShopItems");
        for (int i = 0; i < SHOP_SLOTS; i++) {
            String key = String.valueOf(i);
            shopItems[i] = items.contains(key) ? ItemStack.of(items.getCompound(key)) : ItemStack.EMPTY;
        }
    }
}
