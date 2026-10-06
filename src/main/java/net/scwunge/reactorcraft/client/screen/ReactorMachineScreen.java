package net.scwunge.reactorcraft.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.scwunge.reactorcraft.ReactorCraft;
import net.scwunge.reactorcraft.content.machine.ReactorMenu;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/** One screen for all the machine GUIs: the original's panel texture, with fluid gauges and progress bars from {@link MachineLayout}. */
public class ReactorMachineScreen extends AbstractContainerScreen<ReactorMenu> {
    private static final int TITLE_COLOR = 0x404040;

    private final MachineLayout layout;
    private final ResourceLocation texture;

    public ReactorMachineScreen(ReactorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        String block = BuiltInRegistries.BLOCK.getKey(menu.machine().getBlockState().getBlock()).getPath();
        this.layout = MachineLayout.BY_BLOCK.getOrDefault(block, new MachineLayout(166, true, List.of(), List.of(), List.of()));
        this.texture = ReactorCraft.id("textures/gui/" + block + ".png");
        this.imageWidth = layout.width();
        this.imageHeight = layout.height();
        this.inventoryLabelY = layout.height() - 94;
        this.titleLabelY = 5;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTankTooltips(graphics, mouseX, mouseY);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(texture, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        for (MachineLayout.TankBox box : layout.tanks()) {
            drawFluid(graphics, box);
        }
        for (MachineLayout.Overlay overlay : layout.overlays()) {
            graphics.blit(texture, leftPos + overlay.x(), topPos + overlay.y(), overlay.u(), overlay.v(), overlay.width(), overlay.height());
        }
        for (MachineLayout.Bar bar : layout.bars()) {
            drawBar(graphics, bar);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        Component name = title;
        graphics.drawString(font, name, (imageWidth - font.width(name)) / 2, titleLabelY, TITLE_COLOR, false);
        if (layout.inventoryLabel()) {
            graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TITLE_COLOR, false);
        }
    }

    private void drawBar(GuiGraphics graphics, MachineLayout.Bar bar) {
        int cap = menu.value(bar.capValue());
        if (cap <= 0) {
            return;
        }
        int tick = Math.min(cap, menu.value(bar.tickValue()));
        int v = bar.itemSlot() >= 0 && menu.getSlot(bar.itemSlot()).hasItem() ? bar.vWithItem() : bar.v();
        if (bar.horizontal()) {
            int w = bar.width() * tick / cap;
            if (w > 0) {
                graphics.blit(texture, leftPos + bar.x(), topPos + bar.y(), bar.u(), v, w, bar.height());
            }
        } else {
            int h = bar.height() * tick / cap;
            if (h > 0) {
                graphics.blit(texture, leftPos + bar.x(), topPos + bar.y(), bar.u(), v, bar.width(), h);
            }
        }
    }

    private void drawFluid(GuiGraphics graphics, MachineLayout.TankBox box) {
        Fluid fluid = menu.tankFluid(box.tank());
        int capacity = menu.tankCapacity(box.tank());
        int amount = menu.tankAmount(box.tank());
        if (fluid == null || amount <= 0 || capacity <= 0) {
            return;
        }
        int height = Math.min(box.height(), box.height() * amount / capacity);
        if (height <= 0) {
            return;
        }
        IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(fluid);
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(ext.getStillTexture());
        int tint = ext.getTintColor();
        float r = ((tint >> 16) & 0xFF) / 255F;
        float g = ((tint >> 8) & 0xFF) / 255F;
        float b = (tint & 0xFF) / 255F;
        int x = leftPos + box.x();
        int bottom = topPos + box.y() + box.height();
        // tile the sprite upwards from the bottom of the gauge, cutting the last tile
        int drawn = 0;
        while (drawn < height) {
            int tileHeight = Math.min(16, height - drawn);
            int tileWidth = box.width();
            for (int tx = 0; tx < tileWidth; tx += 16) {
                blitSlice(graphics, sprite, x + tx, bottom - drawn - tileHeight, Math.min(16, tileWidth - tx), tileHeight, r, g, b);
            }
            drawn += tileHeight;
        }
    }

    /** Draws the bottom {@code h} rows of the sprite (and the left {@code w} columns) at (x, y). */
    private static void blitSlice(GuiGraphics graphics, TextureAtlasSprite sprite, int x, int y, int w, int h, float r, float g, float b) {
        float u0 = sprite.getU0();
        float u1 = u0 + (sprite.getU1() - u0) * w / 16F;
        float v1 = sprite.getV1();
        float v0 = v1 - (v1 - sprite.getV0()) * h / 16F;
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, InventoryMenu.BLOCK_ATLAS);
        RenderSystem.enableBlend();
        Matrix4f pose = graphics.pose().last().pose();
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        buffer.addVertex(pose, x, y + h, 0).setUv(u0, v1).setColor(r, g, b, 1F);
        buffer.addVertex(pose, x + w, y + h, 0).setUv(u1, v1).setColor(r, g, b, 1F);
        buffer.addVertex(pose, x + w, y, 0).setUv(u1, v0).setColor(r, g, b, 1F);
        buffer.addVertex(pose, x, y, 0).setUv(u0, v0).setColor(r, g, b, 1F);
        BufferUploader.drawWithShader(buffer.buildOrThrow());
        RenderSystem.disableBlend();
    }

    private void renderTankTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
        for (MachineLayout.TankBox box : layout.tanks()) {
            int x = leftPos + box.x();
            int y = topPos + box.y();
            if (mouseX >= x - 1 && mouseX <= x + box.width() && mouseY >= y - 1 && mouseY <= y + box.height()) {
                Fluid fluid = menu.tankFluid(box.tank());
                List<Component> lines = new ArrayList<>();
                if (fluid == null || menu.tankAmount(box.tank()) <= 0) {
                    lines.add(Component.literal("Empty"));
                } else {
                    lines.add(fluid.getFluidType().getDescription());
                    lines.add(Component.literal(menu.tankAmount(box.tank()) + " / " + menu.tankCapacity(box.tank()) + " mB"));
                }
                graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
            }
        }
    }
}
