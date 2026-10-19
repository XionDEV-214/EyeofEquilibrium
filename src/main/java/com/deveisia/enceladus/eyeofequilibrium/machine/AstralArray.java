package com.deveisia.enceladus.eyeofequilibrium.machine;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

import org.jetbrains.annotations.Nullable;

public final class AstralArray {

    private AstralArray() {}

    public static final ResourceLocation ID = new ResourceLocation("gtceu", "astral_array");

    @Nullable
    private static Item cached;

    @Nullable
    public static Item item() {
        if (cached == null) {
            Item item = ForgeRegistries.ITEMS.getValue(ID);
            if (item != null && item != Items.AIR) cached = item;
        }
        return cached;
    }
}
