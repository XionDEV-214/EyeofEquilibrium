package com.deveisia.enceladus.eyeofequilibrium;

import com.gregtechceu.gtceu.api.registry.registrate.SoundEntryBuilder;
import com.gregtechceu.gtceu.api.sound.SoundEntry;

public final class EoESounds {

    private EoESounds() {}

    public static SoundEntry EYE_OF_EQUILIBRIUM;

    public static void init() {
        if (EYE_OF_EQUILIBRIUM != null) return;
        EYE_OF_EQUILIBRIUM = new SoundEntryBuilder(EyeOfEquilibriumMod.id("eye_of_equilibrium"))
                .subtitle("Eye of Equilibrium hums")
                .build();
    }
}
