package com.createtreasury.block;

import com.createtreasury.economy.CardUtils;
import com.createtreasury.recipe.CardPressRecipe;
import com.createtreasury.recipe.ModRecipeTypes;
import com.createtreasury.registry.ModItems;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.content.kinetics.press.MechanicalPressBlockEntity;
import com.simibubi.create.content.kinetics.press.PressingRecipe;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.wrapper.RecipeWrapper;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class CardPressBlockEntity extends MechanicalPressBlockEntity {

    private static final Object cardRecipesKey = new Object();
    private static final RecipeWrapper cardInv  = new RecipeWrapper(new ItemStackHandler(1));

    /**
     * Server-side registry: bank name → set of (loaded) BlockPos that are linked to it.
     * Enforces max-links-per-bank without chunk scanning.
     */
    private static final Map<String, Set<BlockPos>> LINK_REGISTRY = new ConcurrentHashMap<>();

    /** Returns how many currently-loaded card presses are linked to {@code bankName}. */
    public static int getLoadedLinkCount(String bankName) {
        Set<BlockPos> set = LINK_REGISTRY.get(bankName);
        return set == null ? 0 : set.size();
    }

    /** Name of the bank this press is linked to. Null means unlinked. */
    @Nullable
    private String linkedBankName = null;

    public CardPressBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    public void onLoad() {
        super.onLoad();
        if (linkedBankName != null && level != null && !level.isClientSide) {
            LINK_REGISTRY.computeIfAbsent(linkedBankName, k -> ConcurrentHashMap.newKeySet())
                         .add(worldPosition);
        }
    }

    @Override
    public void invalidate() {
        if (linkedBankName != null && level != null && !level.isClientSide) {
            Set<BlockPos> set = LINK_REGISTRY.get(linkedBankName);
            if (set != null) set.remove(worldPosition);
        }
        super.invalidate();
    }

    // ── Linking ───────────────────────────────────────────────────────────────

    public void setLinkedBank(@Nullable String bankName) {
        if (level != null && !level.isClientSide) {
            if (linkedBankName != null) {
                Set<BlockPos> old = LINK_REGISTRY.get(linkedBankName);
                if (old != null) old.remove(worldPosition);
            }
            if (bankName != null) {
                LINK_REGISTRY.computeIfAbsent(bankName, k -> ConcurrentHashMap.newKeySet())
                             .add(worldPosition);
            }
        }
        this.linkedBankName = bankName;
        setChanged();
        sendData();
    }

    @Nullable
    public String getLinkedBankName() {
        return linkedBankName;
    }

    // ── NBT persistence ───────────────────────────────────────────────────────

    @Override
    protected void write(CompoundTag tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        if (linkedBankName != null) {
            tag.putString("LinkedBank", linkedBankName);
        }
    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        linkedBankName = tag.contains("LinkedBank") ? tag.getString("LinkedBank") : null;
    }

    // ── Engineer's Goggles overlay ────────────────────────────────────────────

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        boolean result = super.addToGoggleTooltip(tooltip, isPlayerSneaking);

        tooltip.add(Component.literal("  "));

        if (linkedBankName != null) {
            tooltip.add(Component.literal("  ").append(
                    Component.translatable("createtreasury.gui.goggles.linked_bank")
                            .withStyle(ChatFormatting.GRAY)));
            tooltip.add(Component.literal("    ").append(
                    Component.literal(linkedBankName)
                            .withStyle(ChatFormatting.WHITE)));
        } else {
            tooltip.add(Component.literal("  ").append(
                    Component.translatable("createtreasury.gui.goggles.no_bank")
                            .withStyle(ChatFormatting.DARK_GRAY)));
        }

        return true;
    }

    // ── Card issuing (world path) ─────────────────────────────────────────────

    /**
     * After the press completes, stamp output debit cards with {@code IssuedBy} NBT
     * so the card knows which bank it belongs to.
     */
    @Override
    public boolean tryProcessInWorld(ItemEntity inputEntity, boolean simulate) {
        if (linkedBankName == null || simulate || level == null || level.isClientSide) {
            return super.tryProcessInWorld(inputEntity, simulate);
        }

        AABB searchZone = new AABB(inputEntity.blockPosition()).inflate(2);
        Set<Integer> existingIds = new HashSet<>();
        for (ItemEntity e : level.getEntitiesOfClass(ItemEntity.class, searchZone)) {
            existingIds.add(e.getId());
        }

        boolean success = super.tryProcessInWorld(inputEntity, simulate);

        if (success) {
            final String bank = linkedBankName;
            level.getEntitiesOfClass(ItemEntity.class, searchZone).forEach(entity -> {
                boolean isNew      = !existingIds.contains(entity.getId());
                boolean isModified = entity.getId() == inputEntity.getId();
                if ((isNew || isModified) && isDebitCard(entity.getItem())) {
                    entity.getItem().getOrCreateTag().putString(CardUtils.TAG_ISSUED_BY, bank);
                }
            });
        }
        return success;
    }

    // ── Card issuing (belt/depot path) ────────────────────────────────────────

    @Override
    public boolean tryProcessOnBelt(TransportedItemStack transported,
                                    List<ItemStack> outputList,
                                    boolean simulate) {
        boolean success = super.tryProcessOnBelt(transported, outputList, simulate);

        if (success && !simulate && linkedBankName != null) {
            final String bank = linkedBankName;
            for (ItemStack stack : outputList) {
                if (isDebitCard(stack)) {
                    stack.getOrCreateTag().putString(CardUtils.TAG_ISSUED_BY, bank);
                }
            }
        }
        return success;
    }

    private boolean isDebitCard(ItemStack stack) {
        return stack.getItem() == ModItems.DEBIT_CARD.get();
    }

    // ── Recipe filtering ──────────────────────────────────────────────────────

    @Override
    protected <C extends Container> boolean matchStaticFilters(Recipe<C> recipe) {
        return recipe instanceof CardPressRecipe;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Optional<PressingRecipe> getRecipe(ItemStack item) {
        // Card press requires a linked bank to stamp cards
        if (linkedBankName == null) {
            return Optional.empty();
        }

        // Only match blank (non-issued) debit cards
        if (!isDebitCard(item) || CardUtils.isIssued(item)) {
            return Optional.empty();
        }

        cardInv.setItem(0, item);
        Optional<CardPressRecipe> found = level.getRecipeManager()
                .getRecipeFor((net.minecraft.world.item.crafting.RecipeType<CardPressRecipe>)
                        ModRecipeTypes.CARD_PRESSING.getType(), cardInv, level);
        return (Optional<PressingRecipe>) (Optional<?>) found;
    }

    @Override
    protected Object getRecipeCacheKey() {
        return cardRecipesKey;
    }
}
