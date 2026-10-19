package com.deveisia.enceladus.eyeofequilibrium;

import com.gregtechceu.gtceu.api.addon.GTAddon;
import com.gregtechceu.gtceu.api.addon.IGTAddon;
import com.gregtechceu.gtceu.api.registry.registrate.GTRegistrate;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.data.recipes.FinishedRecipe;

import java.util.function.Consumer;

import javax.annotation.ParametersAreNonnullByDefault;

@GTAddon
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class EyeOfEquilibriumAddon implements IGTAddon {

    @Override
    public GTRegistrate getRegistrate() {
        return EoEMachines.REGISTRATE;
    }

    @Override
    public void initializeAddon() {
        EyeOfEquilibriumMod.LOGGER.info("[EyeOfEquilibrium] Addon initialized.");
    }

    @Override
    public String addonModId() {
        return EyeOfEquilibriumMod.MODID;
    }

    @Override
    public void registerSounds() {
        EoESounds.init();
    }

    @Override
    public void addRecipes(Consumer<FinishedRecipe> provider) {
        EoERecipes.init(provider);
    }
}
