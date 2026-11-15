package com.angelofdeath.boss.client;

import com.angelofdeath.boss.AngelOfDeathBoss;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.ResourceLocation;

public final class ModModelLayers {
    public static final ModelLayerLocation WINGS = layer("angel_wings");
    public static final ModelLayerLocation BOSS = layer("angel_of_death");
    public static final ModelLayerLocation CROW = layer("void_crow");

    private ModModelLayers() {
    }

    private static ModelLayerLocation layer(String name) {
        return new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(AngelOfDeathBoss.MOD_ID, name), "main");
    }
}
