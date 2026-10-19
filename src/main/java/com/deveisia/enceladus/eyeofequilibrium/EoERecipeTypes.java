package com.deveisia.enceladus.eyeofequilibrium;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import com.gregtechceu.gtceu.common.data.GTSoundEntries;

import com.lowdragmc.lowdraglib.gui.texture.ProgressTexture;

public final class EoERecipeTypes {

    private EoERecipeTypes() {}

    public static GTRecipeType EYE_OF_EQUILIBRIUM;

    public static void init() {
        EYE_OF_EQUILIBRIUM = GTRecipeTypes.register("eye_of_equilibrium", GTRecipeTypes.MULTIBLOCK)
                .setMaxIOSize(1, 1, 1, 4)
                .setEUIO(IO.IN)
                .setProgressBar(GuiTextures.PROGRESS_BAR_REPLICATOR, ProgressTexture.FillDirection.LEFT_TO_RIGHT)
                .setSound(EoESounds.EYE_OF_EQUILIBRIUM != null ? EoESounds.EYE_OF_EQUILIBRIUM :
                        GTSoundEntries.REPLICATOR);
    }
}
