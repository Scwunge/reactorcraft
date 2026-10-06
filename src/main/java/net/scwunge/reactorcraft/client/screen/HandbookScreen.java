package net.scwunge.reactorcraft.client.screen;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.scwunge.reactorcraft.ReactorCraft;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * The handbook (GuiReactorBook): a row of chapter tabs, and in each chapter a page for every part and item, with what it does and the numbers that
 * matter. Long entries run over onto further pages. The text comes from {@code assets/reactorcraft/handbook/en_us.json}, made from the original text.
 */
public class HandbookScreen extends Screen {
    private static final int WIDTH = 256;
    private static final int HEIGHT = 230;
    private static final int TEXT_X = 14;
    private static final int PAGE_LINES = 18;

    private record Entry(String title, ItemStack icon, String text, String notes) {
    }

    private record Chapter(String title, ItemStack icon, String intro, List<Entry> entries) {
    }

    private static List<Chapter> chapters;

    private int chapter;
    private int entry = -1;
    private int subpage;
    private List<FormattedCharSequence> lines = List.of();
    private List<FormattedCharSequence> notes = List.of();
    private int left;
    private int top;

    public HandbookScreen() {
        super(Component.translatable("item.reactorcraft.handbook"));
    }

    public static void open() {
        Minecraft.getInstance().setScreen(new HandbookScreen());
    }

    private static ItemStack stack(String id) {
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
        return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }

    private static List<Chapter> load() {
        if (chapters != null) {
            return chapters;
        }
        List<Chapter> list = new ArrayList<>();
        try (Reader reader = new InputStreamReader(Minecraft.getInstance().getResourceManager()
                .getResourceOrThrow(ReactorCraft.id("handbook/en_us.json")).open(), StandardCharsets.UTF_8)) {
            JsonArray array = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("chapters");
            for (JsonElement e : array) {
                JsonObject c = e.getAsJsonObject();
                List<Entry> entries = new ArrayList<>();
                for (JsonElement p : c.getAsJsonArray("pages")) {
                    JsonObject page = p.getAsJsonObject();
                    entries.add(new Entry(page.get("title").getAsString(), stack(page.get("icon").getAsString()), page.get("text").getAsString(),
                            page.get("notes").getAsString()));
                }
                list.add(new Chapter(c.get("title").getAsString(), stack(c.get("icon").getAsString()), c.get("intro").getAsString(), entries));
            }
        } catch (Exception e) {
            ReactorCraft.LOGGER.error("Could not read the handbook", e);
        }
        chapters = list;
        return list;
    }

    @Override
    protected void init() {
        left = (width - WIDTH) / 2;
        top = (height - HEIGHT) / 2;
        List<Chapter> all = load();
        // the chapter tabs: a small button for each, with the chapter icon drawn over it
        for (int i = 0; i < all.size(); i++) {
            int index = i;
            addRenderableWidget(Button.builder(Component.empty(), b -> {
                chapter = index;
                entry = -1;
                subpage = 0;
                layout();
            }).bounds(left + 4 + i * 22, top - 22, 20, 20).tooltip(Tooltip.create(Component.literal(all.get(i).title()))).build());
        }
        addRenderableWidget(Button.builder(Component.literal("<"), b -> turn(-1)).bounds(left + 8, top + HEIGHT - 24, 24, 16).build());
        addRenderableWidget(Button.builder(Component.literal(">"), b -> turn(1)).bounds(left + WIDTH - 32, top + HEIGHT - 24, 24, 16).build());
        layout();
    }

    /** Re-wraps the text of the page being read. */
    private void layout() {
        List<Chapter> all = load();
        if (all.isEmpty()) {
            return;
        }
        Chapter c = all.get(chapter);
        String text = entry < 0 ? c.intro() : c.entries().get(entry).text();
        String note = entry < 0 ? "" : c.entries().get(entry).notes();
        int wrap = WIDTH - 2 * TEXT_X;
        lines = font.split(FormattedText.of(text), wrap);
        List<FormattedCharSequence> wrapped = new ArrayList<>();
        for (String line : note.split("\n")) {
            if (!line.isBlank()) {
                wrapped.addAll(font.split(FormattedText.of(line), wrap));
            }
        }
        notes = wrapped;
    }

    private int subpages() {
        int total = lines.size() + (notes.isEmpty() ? 0 : notes.size() + 1);
        return Math.max(1, (total + PAGE_LINES - 1) / PAGE_LINES);
    }

    /** Next or previous page: through the pages of the entry, then to the neighbouring entry (the chapter index page comes first). */
    private void turn(int direction) {
        List<Chapter> all = load();
        if (all.isEmpty()) {
            return;
        }
        int next = subpage + direction;
        if (next >= 0 && next < subpages()) {
            subpage = next;
            return;
        }
        int entries = all.get(chapter).entries().size();
        int target = entry + direction;
        if (target < -1 || target >= entries) {
            return;
        }
        entry = target;
        layout();
        subpage = direction > 0 ? 0 : subpages() - 1;
    }

    private int contentsRowY(int row) {
        return top + 32 + lines.size() * 9 + 8 + row * 10;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fill(left - 2, top - 2, left + WIDTH + 2, top + HEIGHT + 2, 0xFF3B2A1A);
        graphics.fill(left, top, left + WIDTH, top + HEIGHT, 0xFFE9DDB8);
        super.render(graphics, mouseX, mouseY, partialTick);
        List<Chapter> all = load();
        for (int i = 0; i < all.size(); i++) {
            graphics.renderItem(all.get(i).icon(), left + 6 + i * 22, top - 20);
        }
        if (all.isEmpty()) {
            graphics.drawString(font, "The handbook could not be read.", left + TEXT_X, top + 20, 0x600000, false);
            return;
        }
        Chapter c = all.get(chapter);
        String title = entry < 0 ? c.title() : c.entries().get(entry).title();
        ItemStack icon = entry < 0 ? c.icon() : c.entries().get(entry).icon();
        if (!icon.isEmpty()) {
            graphics.renderItem(icon, left + TEXT_X, top + 8);
        }
        graphics.drawString(font, title, left + TEXT_X + 22, top + 12, 0x3B2A1A, false);
        int y = top + 32;
        int first = subpage * PAGE_LINES;
        int shown = 0;
        for (int i = first; i < lines.size() && shown < PAGE_LINES; i++, shown++) {
            graphics.drawString(font, lines.get(i), left + TEXT_X, y, 0x202020, false);
            y += 9;
        }
        if (!notes.isEmpty()) {
            int noteStart = lines.size() + 1;
            for (int i = Math.max(first, noteStart) - noteStart; i < notes.size() && shown < PAGE_LINES; i++, shown++) {
                if (i == 0 || shown == 0) {
                    y += 3;
                }
                graphics.drawString(font, notes.get(i), left + TEXT_X, y, 0x7A3A00, false);
                y += 9;
            }
        }
        String where = entry < 0 ? "Contents" : (entry + 1) + "/" + c.entries().size();
        graphics.drawCenteredString(font, where + (subpages() > 1 ? "  (" + (subpage + 1) + "/" + subpages() + ")" : ""), left + WIDTH / 2, top + HEIGHT - 20, 0x3B2A1A);
        if (entry < 0) {
            int row = 0;
            for (Entry e : c.entries()) {
                graphics.drawString(font, "- " + e.title(), left + TEXT_X, contentsRowY(row), 0x204080, false);
                row++;
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (entry < 0 && load().size() > chapter) {
            Chapter c = load().get(chapter);
            for (int row = 0; row < c.entries().size(); row++) {
                int rowY = contentsRowY(row);
                if (mouseX >= left + TEXT_X && mouseX <= left + WIDTH - TEXT_X && mouseY >= rowY - 1 && mouseY < rowY + 9) {
                    entry = row;
                    subpage = 0;
                    layout();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /** Jumps to an entry (-1 for the chapter contents); for the development harness. */
    public void select(int chapterIndex, int entryIndex, int page) {
        chapter = chapterIndex;
        entry = entryIndex;
        layout();
        subpage = Math.min(page, subpages() - 1);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
