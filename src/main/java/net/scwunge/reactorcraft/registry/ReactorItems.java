package net.scwunge.reactorcraft.registry;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.scwunge.reactorcraft.ReactorCraft;
import net.scwunge.reactorcraft.content.item.FluidContainerItem;
import net.scwunge.reactorcraft.content.item.NuclearWasteItem;
import net.scwunge.reactorcraft.content.material.FluoriteColor;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** ReactorCraft's items, in the order of the original's item sheet. */
public final class ReactorItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ReactorCraft.MODID);
    /** Everything for the creative tab, in order (blocks are added by ReactorBlocks). */
    public static final List<DeferredItem<? extends Item>> TAB = new ArrayList<>();

    public static final DeferredItem<Item> HEAVY_WATER_BUCKET = add(ITEMS.register("heavy_water_bucket",
            () -> new FluidContainerItem(ReactorFluids.HEAVY_WATER.source, () -> Items.BUCKET,
                    new Item.Properties().stacksTo(1).craftRemainder(Items.BUCKET))));

    // raw materials (ReactorStacks order)
    public static final DeferredItem<Item> HYDROGEN_FLUORIDE = simple("hydrogen_fluoride");
    public static final DeferredItem<Item> ENRICHED_URANIUM_DUST = simple("enriched_uranium_dust");
    public static final DeferredItem<Item> DEPLETED_URANIUM_DUST = simple("depleted_uranium_dust");
    public static final DeferredItem<Item> AMMONIUM_CHLORIDE = simple("ammonium_chloride");
    public static final DeferredItem<Item> QUICKLIME = simple("quicklime");
    public static final DeferredItem<Item> CALCITE_CRYSTAL = simple("calcite_crystal");
    public static final DeferredItem<Item> LODESTONE = simple("magnetite");
    public static final DeferredItem<Item> THORIUM_DUST = simple("thorium_dust");
    public static final DeferredItem<Item> EMERALD_DUST = simple("emerald_dust");
    public static final DeferredItem<Item> UNPROCESSED_WASTE = simple("unprocessed_nuclear_waste");
    /** One isotope (or a mixed lot) of fission waste; the creative tab lists every variant, see ReactorTabs. */
    public static final DeferredItem<NuclearWasteItem> NUCLEAR_WASTE = ITEMS.register("nuclear_waste",
            () -> new NuclearWasteItem(new Item.Properties()));

    private static final Map<FluoriteColor, DeferredItem<Item>> FLUORITE = new EnumMap<>(FluoriteColor.class);

    static {
        for (FluoriteColor color : FluoriteColor.values()) {
            FLUORITE.put(color, simple(color.getSerializedName() + "_fluorite"));
        }
    }

    public static final DeferredItem<Item> URANIUM_INGOT = simple("uranium_ingot");
    public static final DeferredItem<Item> CADMIUM_INGOT = simple("cadmium_ingot");
    public static final DeferredItem<Item> INDIUM_INGOT = simple("indium_ingot");
    public static final DeferredItem<Item> SILVER_INGOT = simple("silver_ingot");

    // canisters (ReactorStacks order)
    public static final DeferredItem<Item> EMPTY_CANISTER = add(ITEMS.register("empty_canister",
            () -> new FluidContainerItem(new Item.Properties())));
    public static final DeferredItem<Item> UF6_CANISTER = canister("uranium_hexafluoride", ReactorFluids.URANIUM_HEXAFLUORIDE);
    public static final DeferredItem<Item> HF_CANISTER = canister("hydrofluoric_acid", ReactorFluids.HYDROFLUORIC_ACID);
    public static final DeferredItem<Item> AMMONIA_CANISTER = canister("ammonia", ReactorFluids.AMMONIA);
    public static final DeferredItem<Item> SODIUM_CANISTER = canister("sodium", ReactorFluids.SODIUM);
    public static final DeferredItem<Item> DEUTERIUM_CANISTER = canister("deuterium", ReactorFluids.DEUTERIUM);
    public static final DeferredItem<Item> TRITIUM_CANISTER = canister("tritium", ReactorFluids.TRITIUM);
    public static final DeferredItem<Item> CHLORINE_CANISTER = canister("chlorine", ReactorFluids.CHLORINE);
    public static final DeferredItem<Item> OXYGEN_CANISTER = canister("oxygen", ReactorFluids.OXYGEN);
    public static final DeferredItem<Item> CO2_CANISTER = canister("co2", ReactorFluids.CO2);
    public static final DeferredItem<Item> HOT_CO2_CANISTER = canister("hot_co2", ReactorFluids.HOT_CO2);
    public static final DeferredItem<Item> HOT_SODIUM_CANISTER = canister("hot_sodium", ReactorFluids.HOT_SODIUM);
    public static final DeferredItem<Item> LITHIUM_CANISTER = canister("lithium", ReactorFluids.LITHIUM);
    public static final DeferredItem<Item> LIFBE_CANISTER = canister("lifbe", ReactorFluids.LIFBE);
    public static final DeferredItem<Item> HOT_LIFBE_CANISTER = canister("hot_lifbe", ReactorFluids.HOT_LIFBE);
    public static final DeferredItem<Item> LIFBE_FUEL_CANISTER = canister("lifbe_fuel", ReactorFluids.LIFBE_FUEL);

    // crafting parts (CraftingItems order)
    public static final DeferredItem<Item> FUEL_CANISTER = simple("fuel_canister");
    public static final DeferredItem<Item> ABSORPTION_ROD = simple("absorption_rod");
    public static final DeferredItem<Item> OBSIDIAN_TANK = simple("obsidian_tank");
    public static final DeferredItem<Item> ALLOY_INGOT = simple("cd_in_ag_alloy_ingot");
    public static final DeferredItem<Item> HARDENED_BACKING = simple("hardened_backing_panel");
    public static final DeferredItem<Item> FERROMAGNETIC_PLATE = simple("ferromagnetic_plate");
    public static final DeferredItem<Item> MAGNETIC_CORE = simple("magnetic_core");
    public static final DeferredItem<Item> COOLANT_PACK = simple("coolant_pack");
    public static final DeferredItem<Item> GOLD_WIRING = simple("gold_wiring");
    public static final DeferredItem<Item> NEUTRON_SHIELDING = simple("neutron_shielding");
    public static final DeferredItem<Item> FERROMAGNETIC_INGOT = simple("ferromagnetic_ingot");
    public static final DeferredItem<Item> HYSTERESIS_PLATE = simple("hysteresis_plate");
    public static final DeferredItem<Item> HYSTERESIS_RING = simple("hysteresis_ring");
    public static final DeferredItem<Item> GRAPHITE = simple("graphite");
    public static final DeferredItem<Item> URANIUM_DUST = simple("uranium_dust");
    public static final DeferredItem<Item> SHIELDING_FABRIC = simple("radiation_shielding_fabric");
    public static final DeferredItem<Item> CARBIDE_FLAKES = simple("tungsten_carbide_flakes");
    public static final DeferredItem<Item> CARBIDE_INGOT = simple("tungsten_carbide_ingot");
    public static final DeferredItem<Item> TURBINE_CORE = simple("steam_turbine_core");

    private ReactorItems() {
    }

    public static Item fluorite(FluoriteColor color) {
        return FLUORITE.get(color).get();
    }

    private static DeferredItem<Item> simple(String name) {
        return add(ITEMS.registerSimpleItem(name));
    }

    private static DeferredItem<Item> canister(String fluidName, ReactorFluids.Entry fluid) {
        DeferredItem<Item> item = add(ITEMS.register(fluidName + "_canister",
                () -> new FluidContainerItem(fluid.source, ReactorItems.EMPTY_CANISTER, new Item.Properties().craftRemainder(EMPTY_CANISTER.get()))));
        FluidContainerItem.register(fluid.source, item);
        return item;
    }

    static <T extends Item> DeferredItem<T> add(DeferredItem<T> item) {
        TAB.add(item);
        return item;
    }
}
