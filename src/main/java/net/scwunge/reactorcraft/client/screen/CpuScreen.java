package net.scwunge.reactorcraft.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.scwunge.reactorcraft.content.machine.ControlLayout;
import net.scwunge.reactorcraft.content.machine.ControlRodBlockEntity;
import net.scwunge.reactorcraft.content.machine.CpuBlockEntity;
import net.scwunge.reactorcraft.content.machine.ReactorMenu;

/**
 * The Central Control screen (GuiCPU): the control rods seen from above as little squares (green lowered, red raised, grey if
 * the CPU has too little power), a button to raise or lower them all, and arrows to move up and down through the layers.
 * Clicking a square toggles that rod.
 */
public class CpuScreen extends ReactorMachineScreen {
    private static final int SQUARE = 3;
    private static final int SPACING = 5;

    private final CpuBlockEntity cpu;
    private int layer;

    public CpuScreen(ReactorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.cpu = (CpuBlockEntity) menu.machine();
        this.imageHeight = 210;
        this.inventoryLabelY = imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.literal("Retract All"), b -> press(CpuBlockEntity.BUTTON_RAISE_ALL))
                .bounds(leftPos + 8, topPos + 18, 72, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Insert All"), b -> press(CpuBlockEntity.BUTTON_LOWER_ALL))
                .bounds(leftPos + 96, topPos + 18, 72, 20).build());
        addRenderableWidget(Button.builder(Component.literal("▲"), b -> {
            if (cpu.layout().minY() < layer) {
                layer--;
            }
        }).bounds(leftPos + 7, topPos + 84, 12, 20).build());
        addRenderableWidget(Button.builder(Component.literal("▼"), b -> {
            if (cpu.layout().maxY() > layer) {
                layer++;
            }
        }).bounds(leftPos + 157, topPos + 84, 12, 20).build());
    }

    private void press(int id) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameMode != null) {
            mc.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        super.renderBg(graphics, partialTick, mouseX, mouseY);
        ControlLayout layout = cpu.layout();
        int originX = leftPos + 1 + imageWidth / 2 - SPACING / 2 - 1;
        int originY = topPos + imageHeight / 2 - SPACING / 2 + 5;
        boolean powered = cpu.shaft().power() >= layout.minPower();
        for (int a = layout.minX(); a <= layout.maxX(); a++) {
            for (int b = layout.minZ(); b <= layout.maxZ(); b++) {
                if (a == 0 && b == 0) {
                    continue;
                }
                int color = colorAt(layout, a, layer, b, powered);
                int x = originX + a * SPACING;
                int y = originY + b * SPACING;
                graphics.fill(x, y, x + SQUARE, y + SQUARE, 0xFF000000 | color);
            }
        }
    }

    private int colorAt(ControlLayout layout, int a, int y, int b, boolean powered) {
        ControlRodBlockEntity rod = layout.rodAt(cpu.getLevel(), a, y, b);
        if (rod == null) {
            return 0x6A6A6A;
        }
        return powered ? rod.isActive() ? 0x00FF00 : 0xFF0000 : 0xA0A0A0;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        int originX = leftPos + 1 + imageWidth / 2 - SPACING / 2 - 1;
        int originY = topPos + imageHeight / 2 - SPACING / 2 + 5;
        int a = (int) Math.floor((mouseX - originX) / SPACING);
        int b = (int) Math.floor((mouseY - originY) / SPACING);
        if (cpu.layout().rodAt(cpu.getLevel(), a, layer, b) != null) {
            press(CpuBlockEntity.toggleButton(a, layer, b));
            Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                    net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 0.9F));
            return true;
        }
        return false;
    }
}
