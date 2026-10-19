package com.deveisia.enceladus.eyeofequilibrium.structure;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class EoEBlocks {

    private EoEBlocks() {}

    public static final String DIMENSIONAL_CASING = "dimensional_casing";
    public static final String DIMENSIONAL_TRANSCENDENT_CASING = "dimensional_transcendent_casing";

    public static final String[] SPACETIME_COMPRESSION = tiered("spacetime_compression_field_generator_");
    public static final String[] TIME_ACCELERATION = tiered("time_acceleration_field_generator_");
    public static final String[] STABILISATION = tiered("stabilisation_field_generator_");

    private static String[] tiered(String prefix) {
        String[] out = new String[9];
        for (int i = 0; i < 9; i++) out[i] = prefix + (i + 1);
        return out;
    }

    public static Block block(String path) {
        ResourceLocation id = new ResourceLocation("gtceu", path);
        if (!ForgeRegistries.BLOCKS.containsKey(id)) {
            throw new IllegalStateException("[EyeOfEquilibrium] Block '" + id + "' is not registered. " +
                    "Check that the KubeJS startup script registering it is loaded.");
        }
        return ForgeRegistries.BLOCKS.getValue(id);
    }

    public static Block[] blocks(String[] paths) {
        Block[] out = new Block[paths.length];
        for (int i = 0; i < paths.length; i++) out[i] = block(paths[i]);
        return out;
    }

    public static Block dimensionalCasing() {
        return block(DIMENSIONAL_CASING);
    }

    public static Block dimensionalTranscendentCasing() {
        return block(DIMENSIONAL_TRANSCENDENT_CASING);
    }
}
