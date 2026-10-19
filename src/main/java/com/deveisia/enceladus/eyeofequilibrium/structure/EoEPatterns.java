package com.deveisia.enceladus.eyeofequilibrium.structure;

import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.pattern.BlockPattern;
import com.gregtechceu.gtceu.api.pattern.FactoryBlockPattern;
import com.gregtechceu.gtceu.api.pattern.Predicates;
import com.gregtechceu.gtceu.api.pattern.TraceabilityPredicate;
import com.gregtechceu.gtceu.api.pattern.predicates.PredicateBlocks;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class EoEPatterns {

    private EoEPatterns() {}

    public static final String SPACETIME = "S";
    public static final String TIME = "T";
    public static final String STABILITY = "A";

    public static BlockPattern build(MultiblockMachineDefinition definition) {
        FactoryBlockPattern builder = FactoryBlockPattern.start();
        for (String[] aisle : EoEStructure.AISLES) {
            builder.aisle(aisle);
        }
        return builder
                .where('K', Predicates.controller(Predicates.blocks(definition.getBlock())))
                .where('D', Predicates.blocks(EoEBlocks.dimensionalCasing())
                        .or(Predicates.autoAbilities(definition.getRecipeTypes()))
                        .or(Predicates.abilities(PartAbility.MAINTENANCE).setExactLimit(1)))
                .where('E', Predicates.blocks(EoEBlocks.dimensionalTranscendentCasing()))
                .where('S', tierAveraging(SPACETIME, EoEBlocks.blocks(EoEBlocks.SPACETIME_COMPRESSION)))
                .where('T', tierAveraging(TIME, EoEBlocks.blocks(EoEBlocks.TIME_ACCELERATION)))
                .where('A', tierAveraging(STABILITY, EoEBlocks.blocks(EoEBlocks.STABILISATION)))
                .where(' ', Predicates.any())
                .build();
    }

    private static TraceabilityPredicate tierAveraging(String prefix, Block[] blocks) {
        PredicateBlocks predicate = new PredicateBlocks(blocks);
        String sumKey = prefix + "_sum";
        String countKey = prefix + "_count";
        predicate.predicate = state -> {
            BlockState blockState = state.getBlockState();
            for (int i = 0; i < blocks.length; i++) {
                if (blockState.getBlock() == blocks[i]) {
                    state.getMatchContext().increment(sumKey, i + 1);
                    state.getMatchContext().increment(countKey, 1);
                    return true;
                }
            }
            return false;
        };
        return new TraceabilityPredicate(predicate);
    }
}
