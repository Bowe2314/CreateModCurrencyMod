package com.createtreasury.block;

import com.createtreasury.registry.ModBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/**
 * Stores the optional linked bank name for a Brass ATM.
 * When linked, the ATM only accepts cards from that bank, only accepts
 * tagged deposits from that bank, and stamps withdrawn coins with the bank name.
 */
public class BrassATMBlockEntity extends BlockEntity {

    private static final String TAG_LINKED_BANK = "LinkedBank";

    @Nullable
    private String linkedBank;

    public BrassATMBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.BRASS_ATM.get(), pos, state);
    }

    @Nullable
    public String getLinkedBank() {
        return linkedBank;
    }

    public void setLinkedBank(@Nullable String bank) {
        this.linkedBank = (bank == null || bank.isEmpty()) ? null : bank;
        setChanged();
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (linkedBank != null && !linkedBank.isEmpty()) {
            tag.putString(TAG_LINKED_BANK, linkedBank);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains(TAG_LINKED_BANK)) {
            linkedBank = tag.getString(TAG_LINKED_BANK);
            if (linkedBank.isEmpty()) linkedBank = null;
        } else {
            linkedBank = null;
        }
    }
}
