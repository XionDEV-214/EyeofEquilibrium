package com.deveisia.enceladus.eyeofequilibrium.ui;

import com.deveisia.enceladus.eyeofequilibrium.machine.AstralArray;
import com.deveisia.enceladus.eyeofequilibrium.machine.EyeOfEquilibriumMachine;

import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.utils.FormattingUtil;

import com.lowdragmc.lowdraglib.gui.util.DrawerHelper;
import com.lowdragmc.lowdraglib.gui.widget.Widget;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;

public class AstralArraySlotWidget extends Widget {

    private static final int CLICK = 1;

    private final EyeOfEquilibriumMachine machine;

    public AstralArraySlotWidget(int x, int y, EyeOfEquilibriumMachine machine) {
        super(x, y, 18, 18);
        this.machine = machine;
        setBackground(GuiTextures.SLOT);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void drawInBackground(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.drawInBackground(graphics, mouseX, mouseY, partialTicks);
        int x = getPositionX() + 1;
        int y = getPositionY() + 1;
        Item item = AstralArray.item();
        long count = machine.getAstralArrayCount();
        if (item != null) {
            ItemStack stack = new ItemStack(item);
            if (count > 0) {
                DrawerHelper.drawItemStack(graphics, stack, x, y, -1, compact(count));
            } else {
                DrawerHelper.drawItemStack(graphics, stack, x, y, 0x55FFFFFF, "");
            }
        }
        if (isMouseOverElement(mouseX, mouseY)) {
            graphics.fill(x, y, x + 16, y + 16, 0x80FFFFFF);
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void drawInForeground(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        tooltipTexts.clear();
        long count = machine.getAstralArrayCount();
        tooltipTexts.add(Component.translatable("eyeofequilibrium.gui.astral_slot.title")
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltipTexts.add(Component.translatable("eyeofequilibrium.gui.astral_slot.count",
                FormattingUtil.formatNumbers(count)));
        tooltipTexts.add(Component.translatable("eyeofequilibrium.gui.astral_slot.bonus",
                String.format("%.3f", count * EyeOfEquilibriumMachine.ASTRAL_BONUS_PER_ITEM * 100)));
        tooltipTexts.add(Component.translatable("eyeofequilibrium.gui.astral_slot.help.0")
                .withStyle(ChatFormatting.GRAY));
        tooltipTexts.add(Component.translatable("eyeofequilibrium.gui.astral_slot.help.1")
                .withStyle(ChatFormatting.GRAY));
        tooltipTexts.add(Component.translatable("eyeofequilibrium.gui.astral_slot.help.2")
                .withStyle(ChatFormatting.GRAY));
        super.drawInForeground(graphics, mouseX, mouseY, partialTicks);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isMouseOverElement(mouseX, mouseY)) {
            boolean shift = isShiftDown();
            writeClientAction(CLICK, buf -> {
                buf.writeVarInt(button);
                buf.writeBoolean(shift);
            });
            playButtonClickSound();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void handleClientAction(int id, FriendlyByteBuf buffer) {
        if (id == CLICK) {
            int button = buffer.readVarInt();
            boolean shift = buffer.readBoolean();
            if (gui != null && gui.entityPlayer != null) {
                machine.handleAstralSlotClick(gui.entityPlayer, button, shift);
            }
            return;
        }
        super.handleClientAction(id, buffer);
    }

    private static String compact(long n) {
        if (n >= 1_000_000_000L) return String.format("%.1fB", n / 1_000_000_000.0);
        if (n >= 1_000_000L) return String.format("%.1fM", n / 1_000_000.0);
        if (n >= 10_000L) return String.format("%.0fK", n / 1_000.0);
        if (n >= 1_000L) return String.format("%.1fK", n / 1_000.0);
        return Long.toString(n);
    }
}
