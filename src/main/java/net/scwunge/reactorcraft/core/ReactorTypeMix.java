package net.scwunge.reactorcraft.core;

import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

/** How much of something came from each kind of reactor (DragonAPI's Proportionality), for steam that has passed several. */
public final class ReactorTypeMix {
    private final Map<ReactorType, Double> values = new EnumMap<>(ReactorType.class);

    public void add(ReactorType type, double amount) {
        if (type == null || Double.isNaN(amount) || Double.isInfinite(amount)) {
            return;
        }
        values.merge(type, amount, Double::sum);
    }

    public void clear() {
        values.clear();
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }

    public Iterable<ReactorType> types() {
        return java.util.List.copyOf(values.keySet());
    }

    public double value(ReactorType type) {
        return values.getOrDefault(type, 0D);
    }

    public double total() {
        double total = 0;
        for (double v : values.values()) {
            total += Math.max(0, v);
        }
        return total;
    }

    /** The share of the whole that came from {@code type}. */
    public double fraction(ReactorType type) {
        double total = total();
        return total <= 0 ? 0 : Math.max(0, value(type)) / total;
    }

    /** The kind with the most, or null if there is nothing. */
    @Nullable
    public ReactorType largest() {
        ReactorType best = null;
        double most = Double.NEGATIVE_INFINITY;
        for (Map.Entry<ReactorType, Double> e : values.entrySet()) {
            if (e.getValue() > most) {
                most = e.getValue();
                best = e.getKey();
            }
        }
        return best;
    }

    public ReactorTypeMix copy() {
        ReactorTypeMix mix = new ReactorTypeMix();
        mix.values.putAll(values);
        return mix;
    }

    public void save(CompoundTag tag) {
        for (Map.Entry<ReactorType, Double> e : values.entrySet()) {
            tag.putDouble(e.getKey().name(), e.getValue());
        }
    }

    public void load(CompoundTag tag) {
        values.clear();
        for (ReactorType type : ReactorType.values()) {
            if (tag.contains(type.name())) {
                values.put(type, tag.getDouble(type.name()));
            }
        }
    }
}
