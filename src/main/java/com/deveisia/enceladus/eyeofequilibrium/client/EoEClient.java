package com.deveisia.enceladus.eyeofequilibrium.client;

import com.deveisia.enceladus.eyeofequilibrium.EyeOfEquilibriumMod;

import com.gregtechceu.gtceu.client.renderer.machine.DynamicRenderManager;

import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.IEventBus;

@OnlyIn(Dist.CLIENT)
public final class EoEClient {

    private EoEClient() {}

    public static void init(IEventBus modBus) {
        DynamicRenderManager.register(EyeOfEquilibriumMod.id("eye_of_equilibrium"), EoERender.TYPE);

        modBus.addListener(EoEShaders::onRegisterShaders);
        modBus.addListener((RegisterClientReloadListenersEvent event) ->
                event.registerReloadListener((ResourceManagerReloadListener) rm -> EoEMeshes.release()));
    }
}
