package com.angelofdeath.boss.logic;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/** Stasis: pins entities in place while registered and restores their flags when released. */
public class FreezeTracker {
    private static final class Frozen {
        Vec3 pos;
        boolean hadNoAi;
        boolean hadNoGravity;
    }

    private final Map<UUID, Frozen> frozen = new HashMap<>();

    public void freeze(Entity entity) {
        Frozen f = this.frozen.get(entity.getUUID());
        if (f == null) {
            f = new Frozen();
            f.pos = entity.position();
            f.hadNoAi = entity instanceof Mob mob && mob.isNoAi();
            f.hadNoGravity = entity.isNoGravity();
            this.frozen.put(entity.getUUID(), f);
        }
        if (entity instanceof Mob mob) {
            mob.setNoAi(true);
        }
        entity.setNoGravity(true);
        entity.setDeltaMovement(Vec3.ZERO);
        entity.fallDistance = 0.0F;
        if (entity instanceof ServerPlayer player) {
            if (player.position().distanceToSqr(f.pos) > 0.01D) {
                player.connection.teleport(f.pos.x, f.pos.y, f.pos.z, player.getYRot(), player.getXRot());
            }
        } else if (!(entity instanceof Mob)) {
            entity.setPos(f.pos.x, f.pos.y, f.pos.z);
        }
        entity.hurtMarked = true;
    }

    public void keepOnly(ServerLevel level, Set<UUID> keep) {
        Iterator<Map.Entry<UUID, Frozen>> it = this.frozen.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Frozen> entry = it.next();
            if (!keep.contains(entry.getKey())) {
                restore(level, entry.getKey(), entry.getValue());
                it.remove();
            }
        }
    }

    public void releaseAll(ServerLevel level) {
        this.frozen.forEach((id, f) -> restore(level, id, f));
        this.frozen.clear();
    }

    private static void restore(ServerLevel level, UUID id, Frozen f) {
        Entity entity = level.getEntity(id);
        if (entity == null) {
            return;
        }
        if (entity instanceof Mob mob) {
            mob.setNoAi(f.hadNoAi);
        }
        entity.setNoGravity(f.hadNoGravity);
    }

    public void save(CompoundTag tag, String key) {
        ListTag list = new ListTag();
        this.frozen.forEach((id, f) -> {
            CompoundTag e = new CompoundTag();
            e.putUUID("Id", id);
            e.putDouble("X", f.pos.x);
            e.putDouble("Y", f.pos.y);
            e.putDouble("Z", f.pos.z);
            e.putBoolean("NoAi", f.hadNoAi);
            e.putBoolean("NoGrav", f.hadNoGravity);
            list.add(e);
        });
        tag.put(key, list);
    }

    public void load(CompoundTag tag, String key) {
        this.frozen.clear();
        ListTag list = tag.getList(key, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag e = list.getCompound(i);
            Frozen f = new Frozen();
            f.pos = new Vec3(e.getDouble("X"), e.getDouble("Y"), e.getDouble("Z"));
            f.hadNoAi = e.getBoolean("NoAi");
            f.hadNoGravity = e.getBoolean("NoGrav");
            this.frozen.put(e.getUUID("Id"), f);
        }
    }
}
