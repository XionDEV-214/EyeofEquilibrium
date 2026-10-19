package com.deveisia.enceladus.eyeofequilibrium;

import com.deveisia.enceladus.eyeofequilibrium.client.EoEClient;

import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.registry.registrate.GTRegistrate;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.ParametersAreNonnullByDefault;

@Mod(EyeOfEquilibriumMod.MODID)
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class EyeOfEquilibriumMod {

    public static final String MODID = "eyeofequilibrium";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    public EyeOfEquilibriumMod(FMLJavaModLoadingContext context) {
        LOGGER.info("[EyeOfEquilibrium] Loading...");

        EoEMachines.REGISTRATE = GTRegistrate.create(MODID);

        IEventBus modBus = context.getModEventBus();
        EoEMachines.REGISTRATE.registerRegistrate();

        modBus.addGenericListener(GTRecipeType.class,
                (GTCEuAPI.RegisterEvent<ResourceLocation, GTRecipeType> event) -> EoERecipeTypes.init());

        modBus.addGenericListener(MachineDefinition.class,
                (GTCEuAPI.RegisterEvent<ResourceLocation, MachineDefinition> event) -> EoEMachines.init());

        modBus.addListener(this::onBuildCreativeTab);

        if (FMLEnvironment.dist.isClient()) {
            EoEClient.init(modBus);
        }

        LOGGER.info("[EyeOfEquilibrium] Loading complete.");
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MODID, path);
    }

    private void onBuildCreativeTab(BuildCreativeModeTabContentsEvent event) {
        ResourceLocation gtMachineTabId = new ResourceLocation("gtceu", "machine");
        if (!event.getTabKey().location().equals(gtMachineTabId)) return;
        if (EoEMachines.EYE_OF_EQUILIBRIUM != null) {
            event.accept(EoEMachines.EYE_OF_EQUILIBRIUM.getItem());
        }
    }
}
