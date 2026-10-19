package com.deveisia.enceladus.eyeofequilibrium;

import com.deveisia.enceladus.eyeofequilibrium.machine.EoEModifiers;
import com.deveisia.enceladus.eyeofequilibrium.machine.EyeOfEquilibriumMachine;
import com.deveisia.enceladus.eyeofequilibrium.structure.EoEBlocks;
import com.deveisia.enceladus.eyeofequilibrium.structure.EoEPatterns;

import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.recipe.OverclockingLogic;
import com.gregtechceu.gtceu.api.registry.registrate.GTRegistrate;
import com.gregtechceu.gtceu.common.data.GTRecipeModifiers;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class EoEMachines {

    private EoEMachines() {}

    public static GTRegistrate REGISTRATE;

    public static MultiblockMachineDefinition EYE_OF_EQUILIBRIUM;

    public static void init() {
        EYE_OF_EQUILIBRIUM = registerEyeOfEquilibrium();
    }

    private static MultiblockMachineDefinition registerEyeOfEquilibrium() {
        return REGISTRATE
                .multiblock("eye_of_equilibrium", EyeOfEquilibriumMachine::new)
                .langValue("Eye of Equilibrium")
                .rotationState(RotationState.NON_Y_AXIS)
                .recipeType(EoERecipeTypes.EYE_OF_EQUILIBRIUM)
                .recipeModifiers(true,
                        EoEModifiers.VEIN_OUTPUT,
                        GTRecipeModifiers.BATCH_MODE,
                        GTRecipeModifiers.ELECTRIC_OVERCLOCK.apply(OverclockingLogic.PERFECT_OVERCLOCK_SUBTICK),
                        EoEModifiers.EQUILIBRIUM)
                .appearanceBlock(EoEBlocks::dimensionalCasing)
                .hasBER(true)
                .tooltips(
                        Component.translatable("block.eyeofequilibrium.eye_of_equilibrium.tooltip.0"),
                        Component.translatable("block.eyeofequilibrium.eye_of_equilibrium.tooltip.1"),
                        Component.translatable("block.eyeofequilibrium.eye_of_equilibrium.tooltip.2"),
                        Component.translatable("block.eyeofequilibrium.eye_of_equilibrium.tooltip.3"),
                        Component.translatable("block.eyeofequilibrium.eye_of_equilibrium.tooltip.4"),
                        Component.translatable("block.eyeofequilibrium.eye_of_equilibrium.tooltip.5"),
                        Component.translatable("block.eyeofequilibrium.eye_of_equilibrium.tooltip.6"),
                        Component.translatable("block.eyeofequilibrium.eye_of_equilibrium.tooltip.7"))
                .pattern(EoEPatterns::build)
                .register();
    }
}
