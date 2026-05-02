package com.createtreasury.company;

import com.createtreasury.config.TreasuryServerConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nullable;
import java.util.*;

public class CompanyManager extends SavedData {

    private final Map<UUID, Company> companies       = new HashMap<>();
    private final Map<UUID, UUID>    playerToCompany = new HashMap<>(); // playerUUID → companyUUID (type="Company")
    private final Map<UUID, UUID>    playerToBank    = new HashMap<>(); // playerUUID → companyUUID (type="Bank")
    private final Map<UUID, UUID>    pendingInvites  = new HashMap<>(); // playerUUID → companyUUID

    private boolean allowDual() {
        try { return TreasuryServerConfig.SERVER.allowDualMembership.get(); }
        catch (Exception e) { return false; }
    }

    /** Always returns the type-specific map Banks go to playerToBank, everything else to playerToCompany. */
    private Map<UUID, UUID> mapFor(String type) {
        return "Bank".equals(type) ? playerToBank : playerToCompany;
    }

    /**
     * Returns true if the player is allowed to join an entity of the given type.
     * With dual OFF: player must not be in any entity at all.
     * With dual ON: player must not already be in an entity of this specific type.
     */
    public boolean canJoinType(UUID playerUUID, String type) {
        if (mapFor(type).containsKey(playerUUID)) return false; // already in this type
        if (!allowDual()) {
            // Dual off also block if they're in the other type
            Map<UUID, UUID> other = "Bank".equals(type) ? playerToCompany : playerToBank;
            if (other.containsKey(playerUUID)) return false;
        }
        return true;
    }

    public static CompanyManager get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                CompanyManager::load,
                CompanyManager::new,
                "createtreasury_companies"
        );
    }

    // ---- create ----

    public Company createCompany(String name, String tagline, String industry, String type, UUID ownerUUID) {
        Company c = new Company(UUID.randomUUID(), name, tagline, industry, type, ownerUUID);
        companies.put(c.getId(), c);
        mapFor(type).put(ownerUUID, c.getId());
        setDirty();
        return c;
    }

    // ---- lookup ----

    /**
     * Returns the entity the player belongs to for the given type slot ("Company" or "Bank"),
     * or null if they don't belong to one of that type.
     */
    @Nullable
    public Company getEntityByPlayer(UUID playerUUID, String type) {
        UUID cid = mapFor(type).get(playerUUID);
        return cid != null ? companies.get(cid) : null;
    }

    /** Backward-compat helper looks up in the "Company" slot. */
    @Nullable
    public Company getCompanyByPlayer(UUID playerUUID) {
        return getEntityByPlayer(playerUUID, "Company");
    }

    @Nullable
    public Company getCompanyById(UUID companyId) {
        return companies.get(companyId);
    }

    /** Returns the first Company / Bank whose name matches, or null. */
    @Nullable
    public Company getEntityByName(String name) {
        for (Company c : companies.values()) {
            if (c.getName().equals(name)) return c;
        }
        return null;
    }

    @Nullable
    public UUID getPendingInviteCompanyId(UUID playerUUID) {
        return pendingInvites.get(playerUUID);
    }

    /** Returns the name of the company that has sent the player a pending invite, or null. */
    @Nullable
    public String getPendingInviteName(UUID playerUUID) {
        UUID cid = pendingInvites.get(playerUUID);
        if (cid == null) return null;
        Company c = companies.get(cid);
        return c != null ? c.getName() : null;
    }

    // ---- invite flow ----

    /**
     * Returns false if target already has a pending invite or cannot join an entity of the company's type.
     */
    public boolean invitePlayer(UUID companyId, UUID targetUUID) {
        Company c = companies.get(companyId);
        if (c == null) return false;
        if (pendingInvites.containsKey(targetUUID))  return false;
        if (!canJoinType(targetUUID, c.getType()))   return false;
        c.addPendingInvite(targetUUID);
        pendingInvites.put(targetUUID, companyId);
        setDirty();
        return true;
    }

    /**
     * Accepts the pending invite for the player.
     * Returns the accepted Company, or null if none existed.
     */
    @Nullable
    public Company acceptInvite(UUID playerUUID) {
        UUID cid = pendingInvites.remove(playerUUID);
        if (cid == null) return null;
        Company c = companies.get(cid);
        if (c == null) return null;
        c.removePendingInvite(playerUUID);
        c.addMember(playerUUID);
        mapFor(c.getType()).put(playerUUID, cid);
        setDirty();
        return c;
    }

    public void declineInvite(UUID playerUUID) {
        UUID cid = pendingInvites.remove(playerUUID);
        if (cid != null) {
            Company c = companies.get(cid);
            if (c != null) c.removePendingInvite(playerUUID);
            setDirty();
        }
    }

    // ---- kick ----

    /**
     * Removes targetUUID from the entity owned by ownerUUID of the given type.
     * Returns false if not allowed.
     */
    public boolean kickMember(UUID ownerUUID, UUID targetUUID, String type) {
        Map<UUID, UUID> map = mapFor(type);
        UUID cid = map.get(ownerUUID);
        if (cid == null) return false;
        Company c = companies.get(cid);
        if (c == null || !c.getOwnerUUID().equals(ownerUUID)) return false;
        if (targetUUID.equals(ownerUUID)) return false;
        if (!c.getMembers().contains(targetUUID)) return false;
        c.removeMember(targetUUID);
        map.remove(targetUUID);
        setDirty();
        return true;
    }

    // ---- leave / disband ----

    public void leaveCompany(UUID playerUUID, String type) {
        Map<UUID, UUID> map = mapFor(type);
        UUID cid = map.remove(playerUUID);
        if (cid == null) return;
        Company c = companies.get(cid);
        if (c == null) return;
        c.removeMember(playerUUID);
        setDirty();
    }

    public void disbandCompany(UUID ownerUUID, String type) {
        Map<UUID, UUID> map = mapFor(type);
        UUID cid = map.remove(ownerUUID);
        if (cid == null) return;
        Company c = companies.remove(cid);
        if (c == null) return;
        for (UUID m : c.getMembers())        map.remove(m);
        for (UUID p : c.getPendingInvites()) pendingInvites.remove(p);
        setDirty();
    }

    // ---- persistence ----

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Company c : companies.values()) list.add(c.save());
        tag.put("companies", list);
        return tag;
    }

    public static CompanyManager load(CompoundTag tag) {
        CompanyManager mgr = new CompanyManager();
        ListTag list = tag.getList("companies", 10);
        for (int i = 0; i < list.size(); i++) {
            Company c = Company.load(list.getCompound(i));
            mgr.companies.put(c.getId(), c);
            // Route owner to the correct map based on stored type
            Map<UUID, UUID> map = "Bank".equals(c.getType()) ? mgr.playerToBank : mgr.playerToCompany;
            map.put(c.getOwnerUUID(), c.getId());
            for (UUID m : c.getMembers())        map.put(m, c.getId());
            for (UUID p : c.getPendingInvites()) mgr.pendingInvites.put(p, c.getId());
        }
        return mgr;
    }
}
