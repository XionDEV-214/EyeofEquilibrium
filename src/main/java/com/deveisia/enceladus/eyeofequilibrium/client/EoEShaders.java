package com.deveisia.enceladus.eyeofequilibrium.client;

import com.deveisia.enceladus.eyeofequilibrium.EyeOfEquilibriumMod;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;

import net.minecraft.client.renderer.ShaderInstance;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterShadersEvent;

import org.jetbrains.annotations.Nullable;

import java.io.IOException;

@OnlyIn(Dist.CLIENT)
public final class EoEShaders {

    private EoEShaders() {}

    @Nullable private static ShaderInstance textured;
    @Nullable private static ShaderInstance orbit;

    public static void onRegisterShaders(RegisterShadersEvent event) {
        try {
            event.registerShader(new ShaderInstance(event.getResourceProvider(),
                    EyeOfEquilibriumMod.id("eoe_textured"), DefaultVertexFormat.POSITION_TEX),
                    s -> textured = s);
            event.registerShader(new ShaderInstance(event.getResourceProvider(),
                    EyeOfEquilibriumMod.id("eoe_orbit"), DefaultVertexFormat.POSITION_TEX),
                    s -> orbit = s);
        } catch (IOException e) {
            throw new RuntimeException("[EyeOfEquilibrium] Failed to load shaders", e);
        }
    }

    public static boolean ready() {
        return textured != null && orbit != null;
    }

    public static ShaderInstance textured() { return textured; }
    public static ShaderInstance orbit() { return orbit; }
}
