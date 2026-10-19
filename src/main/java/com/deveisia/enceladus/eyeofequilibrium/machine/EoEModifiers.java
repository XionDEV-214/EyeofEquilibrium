package com.deveisia.enceladus.eyeofequilibrium.machine;

import com.deveisia.enceladus.eyeofequilibrium.vein.VeinUtil;

import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.chance.logic.ChanceLogic;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier;
import com.gregtechceu.gtceu.api.recipe.ingredient.SizedIngredient;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;
import com.gregtechceu.gtceu.api.recipe.modifier.RecipeModifier;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EoEModifiers {

    private EoEModifiers() {}

    public static final RecipeModifier VEIN_OUTPUT = EoEModifiers::veinOutput;
    public static final RecipeModifier EQUILIBRIUM = EoEModifiers::equilibrium;

    public static @NotNull ModifierFunction veinOutput(@NotNull MetaMachine machine, @NotNull GTRecipe recipe) {
        if (!(machine instanceof EyeOfEquilibriumMachine eoe)) return ModifierFunction.IDENTITY;

        double oreBonus = eoe.oreBonusMultiplier();
        Map<Material, Long> perMaterial = new LinkedHashMap<>();
        for (ResourceLocation id : eoe.getSelectedVeinSet()) {
            for (Material material : VeinUtil.materials(VeinUtil.get(id, false))) {
                perMaterial.merge(material, EyeOfEquilibriumMachine.ORE_PER_VEIN, Long::sum);
            }
        }
        if (perMaterial.isEmpty()) return ModifierFunction.IDENTITY;

        List<Content> ores = new ArrayList<>();
        int maxChance = ChanceLogic.getMaxChancedValue();
        for (Map.Entry<Material, Long> entry : perMaterial.entrySet()) {
            int count = (int) Math.min(Integer.MAX_VALUE, Math.floor(entry.getValue() * oreBonus));
            ItemStack stack = ChemicalHelper.get(TagPrefix.rawOre, entry.getKey(), count);
            if (stack.isEmpty()) continue;
            ores.add(new Content(SizedIngredient.create(stack), maxChance, maxChance, 0));
        }
        if (ores.isEmpty()) return ModifierFunction.IDENTITY;

        return input -> {
            GTRecipe copy = input.copy();
            List<Content> items = new ArrayList<>(copy.outputs.getOrDefault(ItemRecipeCapability.CAP, List.of()));
            items.addAll(ores);
            copy.outputs.put(ItemRecipeCapability.CAP, items);
            return copy;
        };
    }

    public static @NotNull ModifierFunction equilibrium(@NotNull MetaMachine machine, @NotNull GTRecipe recipe) {
        if (!(machine instanceof EyeOfEquilibriumMachine eoe)) return ModifierFunction.IDENTITY;

        double sTier = eoe.spacetimeTier();
        double tTier = eoe.timeTier();
        double aTier = eoe.stabilityTier();
        double astral = eoe.astralMultiplier();

        if (sTier <= 0 && tTier <= 0 && aTier <= 0 && astral == 1) return ModifierFunction.IDENTITY;

        double output = eoe.structureOutputMultiplier() * astral;
        double input = eoe.inputMultiplier();

        return ModifierFunction.builder()
                .outputModifier(new ContentModifier(output, 0.5))
                .tickOutputModifier(new ContentModifier(output, 0.5))
                .inputModifier(new ContentModifier(input, 0.5))
                .tickInputModifier(new ContentModifier(input, 0.5))
                .durationMultiplier(eoe.durationMultiplier())
                .build();
    }
}
