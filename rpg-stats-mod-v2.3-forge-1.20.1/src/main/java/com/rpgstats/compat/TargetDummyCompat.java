package com.rpgstats.compat;

import net.minecraft.entity.Entity;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

/** Optional training target identity; no dummy classes or runtime dependency are required. */
public final class TargetDummyCompat {
    private static final Identifier TARGET = new Identifier("dummmmmmy", "target_dummy");
    public static boolean isTrainingTarget(Entity entity) {
        return entity != null && TARGET.equals(Registries.ENTITY_TYPE.getId(entity.getType()));
    }
    private TargetDummyCompat() {}
}
