package net.scwunge.reactorcraft.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.scwunge.reactorcraft.ReactorCraft;

import java.util.ArrayList;
import java.util.List;

/**
 * The 24 ReactorCraft fluids with the original's density, viscosity, temperature (K), gas flag and glow. They live in
 * tanks, pipes and canisters; none of them is placed in the world as a fluid block (corium, steam and the poison gases
 * have their own blocks).
 */
public final class ReactorFluids {
    public static final DeferredRegister<FluidType> TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, ReactorCraft.MODID);
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, ReactorCraft.MODID);

    public static final List<Entry> ALL = new ArrayList<>();

    public static final Entry HEAVY_WATER = liquid("heavy_water", "heavywater", 1100, 1050, 300, 0);
    public static final Entry HYDROFLUORIC_ACID = gas("hydrofluoric_acid", "hf", 115, 10, 300, 0);
    public static final Entry URANIUM_HEXAFLUORIDE = gas("uranium_hexafluoride", "uf6", 15, 10, 300, 0);
    public static final Entry AMMONIA = liquid("ammonia", "ammonia", 682, 600, 300, 0);
    public static final Entry SODIUM = liquid("sodium", "sodium", 927, 700, 800, 0);
    public static final Entry CHLORINE = gas("chlorine", "chlorine", 320, 12, 300, 0);
    public static final Entry OXYGEN = gas("oxygen", "oxygen", 138, 20, 300, 0);
    public static final Entry LIQUID_OXYGEN = liquid("liquid_oxygen", "oxygen", 1141, 195, 90, 0);
    public static final Entry DEUTERIUM = gas("deuterium", "deuterium", -1, 10, 300, 0);
    public static final Entry TRITIUM = gas("tritium", "tritium", -1, 10, 300, 0);
    public static final Entry FUSION_PLASMA = gas("fusion_plasma", "plasma", -1, 100, 150_000_000, 15);
    public static final Entry LOW_PRESSURE_AMMONIA = liquid("low_pressure_ammonia", "ammonia", 200, 600, 300, 0);
    public static final Entry LOW_PRESSURE_WATER = liquid("low_pressure_water", null, 800, 800, 300, 0);
    public static final Entry HOT_SODIUM = liquid("hot_sodium", "sodiumhot", 720, 650, 2000, 8);
    public static final Entry WARM_SODIUM = liquid("warm_sodium", "sodiumhot", 864, 650, 1100, 6);
    public static final Entry CO2 = gas("co2", "co2", 2, 7, 300, 0);
    public static final Entry HOT_CO2 = gas("hot_co2", "co2", 1, 5, 300, 2);
    public static final Entry CORIUM = liquid("corium", "slag_flow", 5000, 8000, 2173, 0);
    public static final Entry NUCLEAR_WASTE = liquid("nuclear_waste", "slag_flow", 4000, 12000, 800, 0);
    public static final Entry LITHIUM = liquid("lithium", "lithium", 516, 645, 454, 6);
    public static final Entry LIFBE = liquid("lifbe", "lifbe", 6300, 800 * 3, 773, 0);
    public static final Entry LIFBE_FUEL = liquid("lifbe_fuel", "lifbe_fuel", 6750, 850 * 3, 473, 0);
    public static final Entry LIFBE_FUEL_PREHEAT = liquid("lifbe_fuel_preheat", "lifbe_fuel", 6700, 850 * 5 / 2, 673, 0);
    public static final Entry HOT_LIFBE = liquid("hot_lifbe", "lifbe_hot", 6000, 800 * 3, 1273, 8);

    private ReactorFluids() {
    }

    public static final class Entry {
        public final String name;
        /** Texture under textures/block/fluid, or null for vanilla water's. */
        public final String texture;
        public final boolean gas;
        public final DeferredHolder<FluidType, FluidType> type;
        public final DeferredHolder<Fluid, BaseFlowingFluid.Source> source;
        public final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> flowing;

        private Entry(String name, String texture, boolean gas, int density, int viscosity, int temperature, int light) {
            this.name = name;
            this.texture = texture;
            this.gas = gas;
            // the original gave gases a negative or tiny density; FluidType wants gases to have a negative one
            int typeDensity = gas ? -Math.max(1, Math.abs(density)) : density;
            type = TYPES.register(name, () -> new FluidType(FluidType.Properties.create()
                    .descriptionId("fluid_type.reactorcraft." + name).density(typeDensity).viscosity(viscosity)
                    .temperature(temperature).lightLevel(light).canExtinguish(false).canConvertToSource(false)
                    .canSwim(false).canDrown(false)));
            BaseFlowingFluid.Properties[] props = new BaseFlowingFluid.Properties[1];
            source = FLUIDS.register(name, () -> new BaseFlowingFluid.Source(props[0]));
            flowing = FLUIDS.register("flowing_" + name, () -> new BaseFlowingFluid.Flowing(props[0]));
            props[0] = new BaseFlowingFluid.Properties(type, source, flowing);
            if (name.equals("heavy_water")) {
                // lets an empty bucket take heavy water out of a tank, as the original's fluid container registration did
                props[0].bucket(() -> ReactorItems.HEAVY_WATER_BUCKET.get());
            }
        }

        public Fluid get() {
            return source.get();
        }
    }

    private static Entry liquid(String name, String texture, int density, int viscosity, int temperature, int light) {
        Entry e = new Entry(name, texture, false, density, viscosity, temperature, light);
        ALL.add(e);
        return e;
    }

    private static Entry gas(String name, String texture, int density, int viscosity, int temperature, int light) {
        Entry e = new Entry(name, texture, true, density, viscosity, temperature, light);
        ALL.add(e);
        return e;
    }
}
