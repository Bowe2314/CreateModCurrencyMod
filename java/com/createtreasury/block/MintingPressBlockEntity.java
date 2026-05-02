package com.createtreasury.block;

import com.createtreasury.config.TreasuryServerConfig;
import com.createtreasury.recipe.MintingRecipe;
import com.createtreasury.recipe.ModRecipeTypes;
import com.createtreasury.registry.ModItems;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.content.kinetics.press.MechanicalPressBlockEntity;
import com.simibubi.create.content.kinetics.press.PressingRecipe;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.wrapper.RecipeWrapper;
import net.minecraftforge.server.ServerLifecycleHooks;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class MintingPressBlockEntity extends MechanicalPressBlockEntity {

    private static final Object mintingRecipesKey = new Object();
    private static final RecipeWrapper mintingInv  = new RecipeWrapper(new ItemStackHandler(1));

    /**
     * Server-side registry: bank name → set of (loaded) BlockPos that are linked to it.
     * Used to enforce {@code maxLinksPerBank} without scanning every chunk every time.
     * Populated by {@link #onLoad()} and updated by {@link #setLinkedBank(String)}.
     */
    private static final Map<String, Set<BlockPos>> LINK_REGISTRY = new ConcurrentHashMap<>();

    /** Returns how many currently-loaded presses are linked to {@code bankName}. */
    public static int getLoadedLinkCount(String bankName) {
        Set<BlockPos> set = LINK_REGISTRY.get(bankName);
        return set == null ? 0 : set.size();
    }

    /** Name of the bank linked to this press via a Company Linker. Null means unlinked. */
    @Nullable
    private String linkedBankName = null;

    public MintingPressBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    // ── Lifecycle (registry maintenance) ─────────────────────────────────────

    @Override
    public void onLoad() {
        super.onLoad();
        if (linkedBankName != null && level != null && !level.isClientSide) {
            LINK_REGISTRY.computeIfAbsent(linkedBankName, k -> ConcurrentHashMap.newKeySet())
                         .add(worldPosition);
        }
    }

    /**
     * Called by Create's (final) {@code setRemoved()} we clean up the link registry here
     * because {@code setRemoved()} itself is {@code final} in {@code SmartBlockEntity}.
     */
    @Override
    public void invalidate() {
        if (linkedBankName != null && level != null && !level.isClientSide) {
            Set<BlockPos> set = LINK_REGISTRY.get(linkedBankName);
            if (set != null) set.remove(worldPosition);
        }
        super.invalidate();
    }

    // ── Linking ──────────────────────────────────────────────────────────────

    public void setLinkedBank(@Nullable String bankName) {
        // Maintain the static registry server-side
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
        // sendData() syncs the block entity NBT (including LinkedBank) to nearby clients
        // via Create's SmartBlockEntity network update mechanism
        sendData();
    }

    @Nullable
    public String getLinkedBankName() {
        return linkedBankName;
    }

    // ── NBT persistence & client sync ─────────────────────────────────────────

    /**
     * Create's SmartBlockEntity calls {@code write(tag, false)} for disk and
     * {@code write(tag, true)} for network sync (via {@link #sendData()}).
     * We persist the linked bank name in both cases.
     */
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

    /**
     * Appends the linked-bank status to the Engineer's Goggles tooltip.
     * Uses plain Minecraft {@link Component} API no direct Catnip import 
     * to avoid classloader issues with Catnip's package layout changing between versions.
     */
    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        boolean result = super.addToGoggleTooltip(tooltip, isPlayerSneaking);

        // Blank separator line (mirrors Create's own goggle style)
        tooltip.add(Component.literal("  "));

        if (linkedBankName != null) {
            // "  Linked Bank:"  (gray label)
            tooltip.add(Component.literal("  ").append(
                    Component.translatable("createtreasury.gui.goggles.linked_bank")
                            .withStyle(ChatFormatting.GRAY)));
            // "    <bank name>"  (white value, extra indent)
            tooltip.add(Component.literal("    ").append(
                    Component.literal(linkedBankName)
                            .withStyle(ChatFormatting.WHITE)));
        } else {
            // "  No bank linked"  (dark-gray hint)
            tooltip.add(Component.literal("  ").append(
                    Component.translatable("createtreasury.gui.goggles.no_bank")
                            .withStyle(ChatFormatting.DARK_GRAY)));
        }

        return true;
    }

    // ── Coin tagging ──────────────────────────────────────────────────────────

    /**
     * Intercept the world-pressing step to stamp coins with {@code MintedBy} NBT.
     * <p>
     * Create's {@link com.simibubi.create.foundation.recipe.RecipeApplier} converts
     * the input {@link ItemEntity} in-place (single item) or spawns new entities
     * (stacked items) during the super call.  We snapshot entity IDs before the call
     * and tag every coin entity that is either newly spawned or is the modified input.
     */
    @Override
    public boolean tryProcessInWorld(ItemEntity inputEntity, boolean simulate) {
        // Skip tagging entirely when disabled or no bank linked
        if (linkedBankName == null || simulate || level == null || level.isClientSide
                || !TreasuryServerConfig.SERVER.coinTaggingEnabled.get()) {
            return super.tryProcessInWorld(inputEntity, simulate);
        }

        // Snapshot existing item-entity IDs in a generous area
        AABB searchZone = new AABB(inputEntity.blockPosition()).inflate(2);
        Set<Integer> existingIds = new HashSet<>();
        for (ItemEntity e : level.getEntitiesOfClass(ItemEntity.class, searchZone)) {
            existingIds.add(e.getId());
        }

        boolean success = super.tryProcessInWorld(inputEntity, simulate);

        if (success) {
            final String bank = linkedBankName; // capture for lambda
            level.getEntitiesOfClass(ItemEntity.class, searchZone).forEach(entity -> {
                boolean isNew      = !existingIds.contains(entity.getId());
                boolean isModified = entity.getId() == inputEntity.getId();
                if ((isNew || isModified) && isCoin(entity.getItem())) {
                    entity.getItem().getOrCreateTag().putString("MintedBy", bank);
                }
            });
        }
        return success;
    }

    /**
     * Intercept the belt/depot pressing path to stamp coins with {@code MintedBy} NBT.
     * <p>
     * When a Depot (or belt with depot) is directly below the press,
     * {@link com.simibubi.create.content.kinetics.press.PressingBehaviour#tick()} routes
     * items through {@code BeltProcessingBehaviour} which calls THIS method not
     * {@link #tryProcessInWorld}. The output {@link ItemStack}s land in {@code outputList};
     * we tag them after the super call.
     */
    @Override
    public boolean tryProcessOnBelt(TransportedItemStack transported,
                                    List<ItemStack> outputList,
                                    boolean simulate) {
        boolean success = super.tryProcessOnBelt(transported, outputList, simulate);

        if (success && !simulate && linkedBankName != null
                && TreasuryServerConfig.SERVER.coinTaggingEnabled.get()) {
            final String bank = linkedBankName;
            for (ItemStack stack : outputList) {
                if (isCoin(stack)) {
                    stack.getOrCreateTag().putString("MintedBy", bank);
                }
            }
        }
        return success;
    }

    private boolean isCoin(ItemStack stack) {
        return stack.getItem() == ModItems.ZINC_COIN.get()
                || stack.getItem() == ModItems.BRASS_COIN.get()
                || stack.getItem() == ModItems.ANDESITE_COIN.get();
    }

    // ── Recipe filtering ─────────────────────────────────────────────────────

    // Only match MintingRecipe blocks basin compacting and all other pressing recipes
    @Override
    protected <C extends Container> boolean matchStaticFilters(Recipe<C> recipe) {
        return recipe instanceof MintingRecipe;
    }

    // Look up only in the minting recipe type bucket
    @SuppressWarnings("unchecked")
    @Override
    public Optional<PressingRecipe> getRecipe(ItemStack item) {
        // Config gate: block minting entirely when a bank link is required but absent
        if (TreasuryServerConfig.SERVER.requireLinkedBank.get() && linkedBankName == null) {
            return Optional.empty();
        }

        // Config gate: block minting when no bank member is online (server-side only)
        if (TreasuryServerConfig.SERVER.requireOwnerOnline.get()
                && linkedBankName != null
                && level != null && !level.isClientSide) {
            if (!isBankMemberOnline(linkedBankName)) {
                return Optional.empty();
            }
        }

        mintingInv.setItem(0, item);
        Optional<MintingRecipe> found = level.getRecipeManager()
                .getRecipeFor((net.minecraft.world.item.crafting.RecipeType<MintingRecipe>)
                        ModRecipeTypes.MINTING.getType(), mintingInv, level);
        // Safe cast: MintingRecipe extends PressingRecipe
        return (Optional<PressingRecipe>) (Optional<?>) found;
    }

    /**
     * Returns true if at least one member of {@code bankName} is currently online.
     * Falls back to true when the server or bank cannot be found (fail-open).
     */
    private boolean isBankMemberOnline(String bankName) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return true;
        com.createtreasury.company.CompanyManager mgr =
                com.createtreasury.company.CompanyManager.get(server);
        com.createtreasury.company.Company bank = mgr.getEntityByName(bankName);
        if (bank == null) return true; // bank deleted fail open, let getRecipe return normally
        for (java.util.UUID memberId : bank.getMembers()) {
            if (server.getPlayerList().getPlayer(memberId) != null) return true;
        }
        return false;
    }

    @Override
    protected Object getRecipeCacheKey() {
        return mintingRecipesKey;
    }
}
