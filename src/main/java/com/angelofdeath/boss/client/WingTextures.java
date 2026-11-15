package com.angelofdeath.boss.client;

import com.angelofdeath.boss.AngelOfDeathBoss;
import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

/**
 * The only wing artwork is the shipped {@code textures/entity/wings.png}. The boss needs a black, a white and an
 * ascended-grey version of it, so those are produced at load time by remapping that texture's luminance
 * (alpha and shape untouched) instead of shipping extra files. Rebuilt on every resource reload.
 */
public final class WingTextures {
    public static final ResourceLocation ORIGINAL =
            ResourceLocation.fromNamespaceAndPath(AngelOfDeathBoss.MOD_ID, "textures/entity/wings.png");
    public static final ResourceLocation BLACK =
            ResourceLocation.fromNamespaceAndPath(AngelOfDeathBoss.MOD_ID, "dynamic/wings_black");
    public static final ResourceLocation WHITE =
            ResourceLocation.fromNamespaceAndPath(AngelOfDeathBoss.MOD_ID, "dynamic/wings_white");
    public static final ResourceLocation GREY =
            ResourceLocation.fromNamespaceAndPath(AngelOfDeathBoss.MOD_ID, "dynamic/wings_luminous_grey");

    private WingTextures() {
    }

    public static void rebuild(ResourceManager manager) {
        try (InputStream in = manager.getResourceOrThrow(ORIGINAL).open(); NativeImage source = NativeImage.read(in)) {
            register(BLACK, remap(source, 0));
            register(WHITE, remap(source, 1));
            register(GREY, remap(source, 2));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void register(ResourceLocation id, NativeImage image) {
        Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(image));
    }

    private static NativeImage remap(NativeImage source, int variant) {
        NativeImage out = new NativeImage(NativeImage.Format.RGBA, source.getWidth(), source.getHeight(), false);
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                int abgr = source.getPixelRGBA(x, y);
                int a = (abgr >>> 24) & 0xFF;
                if (a == 0) {
                    out.setPixelRGBA(x, y, 0);
                    continue;
                }
                int b = (abgr >> 16) & 0xFF;
                int g = (abgr >> 8) & 0xFF;
                int r = abgr & 0xFF;
                float luma = (0.299F * r + 0.587F * g + 0.114F * b) / 255.0F;
                int nr;
                int ng;
                int nb;
                switch (variant) {
                    case 0 -> { // void black with a faint violet sheen
                        float v = 6.0F + luma * 64.0F;
                        nr = (int) v;
                        ng = (int) v;
                        nb = (int) (v + 8.0F);
                    }
                    case 1 -> { // radiant white: the original's black feathers become pearl grey
                        float v = 150.0F + luma * 105.0F;
                        nr = (int) v;
                        ng = (int) v;
                        nb = (int) Math.min(255.0F, v + 4.0F);
                    }
                    default -> { // ascended luminous grey
                        float v = 112.0F + luma * 84.0F;
                        nr = (int) v;
                        ng = (int) (v + 4.0F);
                        nb = (int) Math.min(255.0F, v + 14.0F);
                    }
                }
                out.setPixelRGBA(x, y, (a << 24) | (nb << 16) | (ng << 8) | nr);
            }
        }
        return out;
    }
}
