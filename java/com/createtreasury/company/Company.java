package com.createtreasury.company;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

import java.util.*;

public class Company {

    private final UUID id;
    private String name;
    private String tagline;
    private String industry;
    private String type;          // "Company" or "Bank"
    private final UUID ownerUUID;
    private final List<UUID> members        = new ArrayList<>();
    private final List<UUID> pendingInvites = new ArrayList<>();

    public Company(UUID id, String name, String tagline, String industry, String type, UUID ownerUUID) {
        this.id        = id;
        this.name      = name;
        this.tagline   = tagline;
        this.industry  = industry;
        this.type      = type;
        this.ownerUUID = ownerUUID;
    }

    public UUID         getId()             { return id; }
    public String       getName()           { return name; }
    public String       getTagline()        { return tagline; }
    public String       getIndustry()       { return industry; }
    public String       getType()           { return type != null ? type : "Company"; }
    public UUID         getOwnerUUID()      { return ownerUUID; }
    public List<UUID>   getMembers()        { return Collections.unmodifiableList(members); }
    public List<UUID>   getPendingInvites() { return Collections.unmodifiableList(pendingInvites); }

    public void addMember(UUID uuid)           { if (!members.contains(uuid))        members.add(uuid); }
    public void removeMember(UUID uuid)        { members.remove(uuid); }
    public void addPendingInvite(UUID uuid)    { if (!pendingInvites.contains(uuid)) pendingInvites.add(uuid); }
    public void removePendingInvite(UUID uuid) { pendingInvites.remove(uuid); }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("id",       id);
        tag.putString("name",     name);
        tag.putString("tagline",  tagline);
        tag.putString("industry", industry);
        tag.putString("type",     getType());
        tag.putUUID("owner",    ownerUUID);

        ListTag memberList = new ListTag();
        for (UUID m : members)        { CompoundTag t = new CompoundTag(); t.putUUID("uuid", m); memberList.add(t); }
        tag.put("members", memberList);

        ListTag inviteList = new ListTag();
        for (UUID m : pendingInvites) { CompoundTag t = new CompoundTag(); t.putUUID("uuid", m); inviteList.add(t); }
        tag.put("pendingInvites", inviteList);

        return tag;
    }

    public static Company load(CompoundTag tag) {
        String type = tag.contains("type") ? tag.getString("type") : "Company";
        Company c = new Company(
                tag.getUUID("id"),
                tag.getString("name"),
                tag.getString("tagline"),
                tag.getString("industry"),
                type,
                tag.getUUID("owner")
        );
        ListTag ml = tag.getList("members",        10);
        for (int i = 0; i < ml.size(); i++) c.members.add(ml.getCompound(i).getUUID("uuid"));
        ListTag il = tag.getList("pendingInvites", 10);
        for (int i = 0; i < il.size(); i++) c.pendingInvites.add(il.getCompound(i).getUUID("uuid"));
        return c;
    }
}
