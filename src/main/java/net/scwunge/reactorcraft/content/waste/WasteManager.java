package net.scwunge.reactorcraft.content.waste;

import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.scwunge.reactorcraft.registry.ReactorComponents;
import net.scwunge.reactorcraft.registry.ReactorItems;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Which isotopes fission waste is made of and how often (the original's WasteManager), and the waste items for them.
 * A waste item carries its isotope as {@link ReactorComponents#WASTE}: the isotope's number, or 1000 plus the element
 * group's number for a mixed-waste item (what the original kept in the item's damage).
 */
public final class WasteManager {
    /** Where mixed-waste ids start. */
    public static final int MIXED_BASE = 1000;

    private static final Map<Isotope, Double> URANIUM = new LinkedHashMap<>();
    private static final Map<Isotope, Double> THORIUM = new LinkedHashMap<>();

    static {
        uranium(Isotope.CS134, 6.79);
        uranium(Isotope.XE135, 6.33);
        uranium(Isotope.ZR93, 6.30);
        uranium(Isotope.MO99, 6.10);
        uranium(Isotope.CS137, 6.09);
        uranium(Isotope.TC99, 6.05);
        uranium(Isotope.SR90, 5.75);
        uranium(Isotope.I131, 2.83);
        uranium(Isotope.PM147, 2.27);
        uranium(Isotope.I129, 0.66);
        uranium(Isotope.SM151, 0.42);
        uranium(Isotope.RU106, 0.39);
        uranium(Isotope.KR85, 0.27);
        uranium(Isotope.PD107, 0.16);
        uranium(Isotope.SE79, 0.05);
        uranium(Isotope.GD155, 0.03);
        uranium(Isotope.SB125, 0.03);
        uranium(Isotope.SN126, 0.02);
        // the stable products of thorium fission are left out, as in the original
        thorium(Isotope.CS137, 6.84);
        thorium(Isotope.I135, 5.03);
        thorium(Isotope.TC99, 4.92);
        thorium(Isotope.XE131, 3.60);
        thorium(Isotope.PM147, 1.74);
        thorium(Isotope.RU103, 1.57);
        thorium(Isotope.XE135, 1.23);
        thorium(Isotope.PM149, 0.78);
        thorium(Isotope.RH105, 0.50);
        thorium(Isotope.SM151, 0.32);
        thorium(Isotope.RU106, 0.25);
    }

    private WasteManager() {
    }

    private static void uranium(Isotope isotope, double percent) {
        URANIUM.put(isotope, percent);
    }

    private static void thorium(Isotope isotope, double percent) {
        THORIUM.put(isotope, percent);
    }

    /** The isotopes uranium fission makes, with their weights. */
    public static Map<Isotope, Double> uraniumYields() {
        return Collections.unmodifiableMap(URANIUM);
    }

    public static Map<Isotope, Double> thoriumYields() {
        return Collections.unmodifiableMap(THORIUM);
    }

    public static List<Isotope> wasteList() {
        return List.copyOf(URANIUM.keySet());
    }

    public static Isotope randomWaste(RandomSource random) {
        return pick(URANIUM, random);
    }

    public static Isotope randomThoriumWaste(RandomSource random) {
        return pick(THORIUM, random);
    }

    public static ItemStack randomWasteItem(RandomSource random) {
        return waste(randomWaste(random));
    }

    public static ItemStack randomThoriumWasteItem(RandomSource random) {
        return waste(randomThoriumWaste(random));
    }

    /** getFullyRandomWasteItem: every uranium waste isotope equally likely. */
    public static ItemStack fullyRandomWasteItem(RandomSource random) {
        List<Isotope> list = wasteList();
        return waste(list.get(random.nextInt(list.size())));
    }

    private static Isotope pick(Map<Isotope, Double> weights, RandomSource random) {
        double total = 0;
        for (double w : weights.values()) {
            total += w;
        }
        double roll = random.nextDouble() * total;
        Isotope last = null;
        for (Map.Entry<Isotope, Double> e : weights.entrySet()) {
            last = e.getKey();
            roll -= e.getValue();
            if (roll < 0) {
                return last;
            }
        }
        return last;
    }

    // ---- the items ----

    public static ItemStack waste(Isotope isotope) {
        return waste(isotope, 1);
    }

    public static ItemStack waste(Isotope isotope, int count) {
        ItemStack stack = new ItemStack(ReactorItems.NUCLEAR_WASTE.get(), count);
        stack.set(ReactorComponents.WASTE.get(), isotope.ordinal());
        return stack;
    }

    public static ItemStack mixedWaste(Isotope.ElementGroup group) {
        ItemStack stack = new ItemStack(ReactorItems.NUCLEAR_WASTE.get());
        stack.set(ReactorComponents.WASTE.get(), MIXED_BASE + group.ordinal());
        return stack;
    }

    public static boolean isWaste(ItemStack stack) {
        return stack.is(ReactorItems.NUCLEAR_WASTE.get());
    }

    public static boolean isMixed(ItemStack stack) {
        return isWaste(stack) && stack.getOrDefault(ReactorComponents.WASTE.get(), 0) >= MIXED_BASE;
    }

    /** The isotope of a waste item, or null for anything else (and for mixed waste). */
    @Nullable
    public static Isotope isotope(ItemStack stack) {
        if (!isWaste(stack)) {
            return null;
        }
        int id = stack.getOrDefault(ReactorComponents.WASTE.get(), 0);
        return id >= 0 && id < Isotope.count() ? Isotope.byId(id) : null;
    }

    @Nullable
    public static Isotope.ElementGroup group(ItemStack stack) {
        if (!isMixed(stack)) {
            return null;
        }
        int id = stack.getOrDefault(ReactorComponents.WASTE.get(), 0) - MIXED_BASE;
        Isotope.ElementGroup[] groups = Isotope.ElementGroup.values();
        return id >= 0 && id < groups.length ? groups[id] : null;
    }

    /** Half-life in ticks of a waste item, 0 for anything else. */
    public static double halfLife(ItemStack stack) {
        Isotope isotope = isotope(stack);
        return isotope == null ? 0 : isotope.halfLifeTicks();
    }

    public static boolean isLongLived(ItemStack stack) {
        Isotope isotope = isotope(stack);
        return isotope != null && isotope.isLongLived();
    }

    /** Every waste item for the creative tab: the uranium isotopes, then the four mixed-waste groups. */
    public static List<ItemStack> creativeStacks() {
        List<ItemStack> out = new ArrayList<>();
        for (Isotope isotope : URANIUM.keySet()) {
            out.add(waste(isotope));
        }
        for (Isotope.ElementGroup group : Isotope.ElementGroup.values()) {
            out.add(mixedWaste(group));
        }
        return out;
    }
}
