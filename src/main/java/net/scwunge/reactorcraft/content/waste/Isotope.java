package net.scwunge.reactorcraft.content.waste;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The isotopes nuclear waste can be (DragonAPI's Isotopes), in the original's order. Half-lives are in the
 * original's Minecraft time: a year is 1,152,000 ticks (8 real hours), a day 24,000 ticks, an hour 500.
 */
public enum Isotope {
    C14(5730, TimeUnit.YEAR, "Carbon-14", ElementGroup.NONMETAL, false),
    U235(704e6, TimeUnit.YEAR, "Uranium-235", ElementGroup.LANTHACTINIDE, false),
    U238(4.468e9, TimeUnit.YEAR, "Uranium-238", ElementGroup.LANTHACTINIDE, false),
    PU239(2410, TimeUnit.YEAR, "Plutonium-239", ElementGroup.LANTHACTINIDE, true),
    PU244(80.8e6, TimeUnit.YEAR, "Plutonium-244", ElementGroup.LANTHACTINIDE, false),
    TH232(14.05e9, TimeUnit.YEAR, "Thorium-232", ElementGroup.LANTHACTINIDE, false),
    RN222(3.8235, TimeUnit.DAY, "Radon-222", ElementGroup.NONMETAL, true),
    RA226(1601, TimeUnit.YEAR, "Radium-226", ElementGroup.ALKALI, true),
    SR90(28.9, TimeUnit.YEAR, "Strontium-90", ElementGroup.ALKALI, true),
    PO210(138.376, TimeUnit.DAY, "Polonium-210", ElementGroup.TRANSITION, true),
    CS134(2.065, TimeUnit.YEAR, "Cesium-134", ElementGroup.ALKALI, false),
    XE135(6.57, TimeUnit.HOUR, "Xenon-135", ElementGroup.NONMETAL, false),
    ZR93(1.53e6, TimeUnit.YEAR, "Zirconium-93", ElementGroup.TRANSITION, false),
    MO99(65.94, TimeUnit.HOUR, "Molybdenum-99", ElementGroup.TRANSITION, false),
    CS137(30.17, TimeUnit.YEAR, "Cesium-137", ElementGroup.ALKALI, true),
    TC99(211000, TimeUnit.YEAR, "Technetium-99", ElementGroup.TRANSITION, false),
    I131(8.02, TimeUnit.DAY, "Iodine-131", ElementGroup.NONMETAL, true),
    PM147(2.62, TimeUnit.YEAR, "Promethium-147", ElementGroup.LANTHACTINIDE, false),
    I129(15.7e6, TimeUnit.YEAR, "Iodine-129", ElementGroup.NONMETAL, false),
    SM151(90, TimeUnit.YEAR, "Samarium-151", ElementGroup.LANTHACTINIDE, false),
    RU106(373.6, TimeUnit.DAY, "Ruthenium-106", ElementGroup.TRANSITION, false),
    KR85(10.78, TimeUnit.YEAR, "Krypton-85", ElementGroup.NONMETAL, false),
    PD107(6.5e6, TimeUnit.YEAR, "Palladium-107", ElementGroup.TRANSITION, false),
    SE79(327000, TimeUnit.YEAR, "Selenium-79", ElementGroup.NONMETAL, false),
    GD155(4.76, TimeUnit.YEAR, "Gadolinium-155", ElementGroup.LANTHACTINIDE, false),
    SB125(2.76, TimeUnit.YEAR, "Antimony-125", ElementGroup.TRANSITION, false),
    SN126(230000, TimeUnit.YEAR, "Tin-126", ElementGroup.TRANSITION, false),
    /** Basically stable. */
    XE136(10e21, TimeUnit.YEAR, "Xenon-136", ElementGroup.NONMETAL, false),
    I135(6.6, TimeUnit.HOUR, "Iodine-135", ElementGroup.NONMETAL, false),
    XE131(12, TimeUnit.DAY, "Xenon-131", ElementGroup.NONMETAL, false),
    /** The original's comment on this one: "actually millis". */
    RU103(1.69, TimeUnit.TICK, "Ruthenium-103", ElementGroup.TRANSITION, false),
    PM149(53.08, TimeUnit.HOUR, "Promethium-149", ElementGroup.LANTHACTINIDE, false),
    RH105(35.36, TimeUnit.HOUR, "Rhodium-105", ElementGroup.TRANSITION, false);

    private static final Isotope[] LIST = values();
    /** The lead isotopes the decay chains end in; they have no item of their own. */
    private static final int[] LEAD_WEIGHTS = {204, 206, 207, 208};

    private final double half;
    private final TimeUnit unit;
    private final String displayName;
    public final boolean extraDanger;
    public final ElementGroup group;
    public final int atomicWeight;
    public final String symbol;
    @Nullable
    private DecayData decay;

    Isotope(double half, TimeUnit unit, String displayName, ElementGroup group, boolean extraDanger) {
        this.half = half;
        this.unit = unit;
        this.displayName = displayName;
        this.group = group;
        this.extraDanger = extraDanger;
        String s = name();
        int start = 0;
        while (!Character.isDigit(s.charAt(start))) {
            start++;
        }
        this.atomicWeight = Integer.parseInt(s.substring(start));
        String letters = s.substring(0, start);
        this.symbol = letters.charAt(0) + letters.substring(1).toLowerCase();
    }

    public static Isotope byId(int id) {
        return LIST[id];
    }

    public static int count() {
        return LIST.length;
    }

    /** The half-life in Minecraft ticks. */
    public double halfLifeTicks() {
        return half * unit.ticks;
    }

    /** "704.000My": the half-life as the original's tooltip showed it. */
    public String halfLifeText() {
        double value = half;
        int thousands = 0;
        while (value >= 1000) {
            value /= 1000;
            thousands++;
        }
        String[] prefixes = {"", "k", "M", "G", "T", "P", "E", "Z", "Y"};
        return String.format("%.3f%s%s", value, prefixes[Math.min(thousands, prefixes.length - 1)], unit.abbreviation);
    }

    /** More than six years: what the storage block takes and the container refuses. */
    public boolean isLongLived() {
        return halfLifeTicks() > 6 * TimeUnit.YEAR.ticks;
    }

    @Override
    public String toString() {
        return displayName;
    }

    public Component displayName() {
        Component name = Component.literal(displayName);
        return extraDanger ? name.copy().withStyle(ChatFormatting.RED) : name;
    }

    /** What it decays into when forced to (in a waste decayer); null means nothing is left. */
    @Nullable
    public DecayData decay() {
        return decay;
    }

    /** One decay product: an isotope, or lead (isotope null) with its mass number. */
    public record Product(@Nullable Isotope isotope, String symbol, int weight) {
    }

    public record DecayData(Product product, double amount) {
    }

    static {
        List<Product> products = new ArrayList<>();
        for (Isotope i : LIST) {
            products.add(new Product(i, i.symbol, i.atomicWeight));
        }
        for (int lead : LEAD_WEIGHTS) {
            products.add(new Product(null, "Pb", lead));
        }
        for (Isotope i : LIST) {
            i.decay = computeDecay(i, products);
        }
    }

    /** One more mass: all of it; two more: half of it; otherwise the heaviest thing at least three lighter. */
    @Nullable
    private static DecayData computeDecay(Isotope s, List<Product> products) {
        for (Product p : products) {
            if (p.weight == s.atomicWeight + 1) {
                return new DecayData(p, 1);
            }
        }
        for (Product p : products) {
            if (p.weight == s.atomicWeight + 2) {
                return new DecayData(p, 0.5);
            }
        }
        Product use = null;
        for (Product p : products) {
            if (p.isotope == s || p.weight > s.atomicWeight - 3) {
                continue;
            }
            if (use == null || p.weight > use.weight) {
                use = p;
            }
        }
        return use == null ? null : new DecayData(use, s.atomicWeight / (double) use.weight);
    }

    private enum TimeUnit {
        YEAR(1_152_000, "y"),
        DAY(24_000, "d"),
        HOUR(500, "h"),
        TICK(1, "t");

        final int ticks;
        final String abbreviation;

        TimeUnit(int ticks, String abbreviation) {
            this.ticks = ticks;
            this.abbreviation = abbreviation;
        }
    }

    public enum ElementGroup {
        ALKALI("Alkali Metals"),
        TRANSITION("Transition Metals"),
        LANTHACTINIDE("Lanthanides and Actinides"),
        NONMETAL("Nonmetals");

        public final String displayName;

        ElementGroup(String displayName) {
            this.displayName = displayName;
        }
    }
}
