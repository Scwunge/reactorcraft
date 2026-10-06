"""Generates ReactorCraft's models, blockstates, lang, loot tables, tags, recipes and world gen, and copies the original
textures out of the release jar (reference/jar, local only) under their new names.

Run from the repository root: python tools/gen_assets.py
"""
import json
import shutil
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "src/main/resources"
ASSETS = RES / "assets/reactorcraft"
DATA = RES / "data/reactorcraft"
JAR = ROOT / "reference/jar"
ORIG_BLOCKS = JAR / "assets/reactorcraft/textures/blocks"
ITEM_SHEET = JAR / "Reika/ReactorCraft/Textures/Items/items1.png"
MOD = "reactorcraft"

lang = {}
written = []


def write_json(path: Path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + "\n", encoding="utf-8", newline="\n")
    written.append(path)


def m(name):
    return f"{MOD}:{name}"


def copy_png(src: Path, dst: Path):
    dst.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(src, dst)
    meta = src.with_name(src.name + ".mcmeta")
    if meta.exists():
        text = meta.read_text(encoding="utf-8").replace("\r\n", "\n")
        dst.with_name(dst.name + ".mcmeta").write_text(text, encoding="utf-8", newline="\n")


# ---------------------------------------------------------------------------------------------------------------- items
FLUORITE = ["blue", "pink", "orange", "magenta", "green", "red", "white", "yellow"]

# item id -> (sprite index on items1.png, English name)
ITEMS = {
    "heavy_water_bucket": (3, "Heavy Water Bucket"),
    "uranium_fuel_pellet": (1, "Uranium Fuel Pellet"),
    "depleted_uranium": (2, "Depleted Uranium"),
    "breeder_fuel": (98, "Breeder Reactor Fuel"),
    "triso_pellet": (102, "TRISO Fuel Pellet"),
    "depleted_triso_pellet": (103, "Depleted TRISO Fuel"),
    "remote_control": (101, "Remote Reactor Control"),
    "geiger_counter": (116, "Geiger Counter"),
    "radiation_cleaner": (99, "Radiation Cleanup Tool"),
    "radiation_goggles": (64, "Radiation Goggles"),
    "hazmat_helmet": (112, "Hazmat Helmet"),
    "hazmat_chestplate": (113, "Hazmat Chestplate"),
    "hazmat_leggings": (114, "Hazmat Leggings"),
    "hazmat_boots": (115, "Hazmat Boots"),
    "plutonium_fuel_pellet": (96, "Plutonium Fuel Pellet"),
    "hydrogen_fluoride": (4, "Hydrogen Fluoride"),
    "enriched_uranium_dust": (5, "Enriched Uranium Dust"),
    "depleted_uranium_dust": (6, "Depleted Uranium Dust"),
    "ammonium_chloride": (7, "Ammonium Chloride"),
    "quicklime": (8, "Quicklime"),
    "calcite_crystal": (9, "Calcite Crystal"),
    "magnetite": (10, "Lodestone"),
    "thorium_dust": (11, "Thorium Dust"),
    "emerald_dust": (12, "Emerald Dust"),
    "unprocessed_nuclear_waste": (13, "Unprocessed Nuclear Waste"),
    "uranium_ingot": (32, "Raw Uranium Ingot"),
    "cadmium_ingot": (33, "Cadmium Ingot"),
    "indium_ingot": (34, "Indium Ingot"),
    "silver_ingot": (35, "Silver Ingot"),
    "empty_canister": (48, "Empty Canister"),
    "uranium_hexafluoride_canister": (49, "Uranium Hexafluoride Canister"),
    "hydrofluoric_acid_canister": (50, "Hydrofluoric Acid Canister"),
    "ammonia_canister": (51, "Ammonia Canister"),
    "sodium_canister": (52, "Sodium Canister"),
    "deuterium_canister": (53, "Deuterium Canister"),
    "tritium_canister": (54, "Tritium Canister"),
    "chlorine_canister": (55, "Chlorine Canister"),
    "oxygen_canister": (56, "Oxygen Canister"),
    "co2_canister": (57, "Carbon Dioxide Canister"),
    "hot_co2_canister": (58, "Hot Carbon Dioxide Canister"),
    "hot_sodium_canister": (59, "Superheated Sodium Canister"),
    "lithium_canister": (60, "Lithium Canister"),
    "lifbe_canister": (61, "Lithium Beryllium Fluoride Canister"),
    "hot_lifbe_canister": (62, "Hot Lithium Beryllium Fluoride Canister"),
    "lifbe_fuel_canister": (63, "Thorium Fuel Canister"),
    "fuel_canister": (144, "Fuel Canister"),
    "absorption_rod": (145, "Absorption Rod"),
    "obsidian_tank": (146, "Obsidian Tank"),
    "cd_in_ag_alloy_ingot": (147, "Cd-In-Ag Alloy Ingot"),
    "hardened_backing_panel": (148, "Hardened Backing Panel"),
    "ferromagnetic_plate": (149, "Ferromagnetic Plate"),
    "magnetic_core": (150, "Magnetic Core"),
    "coolant_pack": (151, "Coolant Pack"),
    "gold_wiring": (152, "Gold Wiring"),
    "neutron_shielding": (153, "Neutron Shielding"),
    "ferromagnetic_ingot": (154, "Ferromagnetic Ingot"),
    "hysteresis_plate": (155, "Hysteresis Plate"),
    "hysteresis_ring": (156, "Hysteresis Ring"),
    "graphite": (157, "Graphite"),
    "uranium_dust": (158, "Uranium Dust"),
    "radiation_shielding_fabric": (159, "Radiation-Shielding Fabric"),
    "tungsten_carbide_flakes": (160, "Tungsten Carbide Flakes"),
    "tungsten_carbide_ingot": (161, "Tungsten Carbide Ingot"),
    "steam_turbine_core": (162, "Steam Turbine Core"),
}
for i, c in enumerate(FLUORITE):
    ITEMS[f"{c}_fluorite"] = (16 + i, f"{c.capitalize()} Fluorite Crystal")


def items():
    sheet = Image.open(ITEM_SHEET).convert("RGBA") if ITEM_SHEET.exists() else None
    for item, (index, english) in ITEMS.items():
        if sheet is not None:
            x, y = index % 16 * 16, index // 16 * 16
            out = ASSETS / f"textures/item/{item}.png"
            out.parent.mkdir(parents=True, exist_ok=True)
            sheet.crop((x, y, x + 16, y + 16)).save(out)
        write_json(ASSETS / f"models/item/{item}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": m(f"item/{item}")}})
        lang[f"item.{MOD}.{item}"] = english
    # nuclear waste: one item, the isotope is a data component; mixed waste uses another sprite (a client item property switches)
    if sheet is not None:
        for name, index in (("nuclear_waste", 0), ("mixed_nuclear_waste", 14)):
            x, y = index % 16 * 16, index // 16 * 16
            sheet.crop((x, y, x + 16, y + 16)).save(ASSETS / f"textures/item/{name}.png")
    write_json(ASSETS / "models/item/mixed_nuclear_waste.json", {"parent": "minecraft:item/generated", "textures": {"layer0": m("item/mixed_nuclear_waste")}})
    write_json(ASSETS / "models/item/nuclear_waste.json", {"parent": "minecraft:item/generated", "textures": {"layer0": m("item/nuclear_waste")},
                                                           "overrides": [{"predicate": {m("mixed"): 1}, "model": m("item/mixed_nuclear_waste")}]})
    lang[f"item.{MOD}.nuclear_waste"] = "Nuclear Waste"


# --------------------------------------------------------------------------------------------------------------- blocks
# ore id -> (original texture, English name, harvest level, product id, smelting xp)
ORES = {
    "pitchblende_ore": ("ore/pitchblende", "Pitchblende", 1, m("uranium_ingot"), 1.0),
    "cadmium_ore": ("ore/cadmium", "Cadmium Ore", 2, m("cadmium_ingot"), 0.7),
    "indium_ore": ("ore/indium", "Indium Ore", 2, m("indium_ingot"), 1.0),
    "silver_ore": ("ore/silver", "Silver Ore", 2, m("silver_ingot"), 0.5),
    "endblende_ore": ("ore/endblende", "Pitchblende", 1, m("uranium_ingot"), 1.0),
    "ammonium_chloride_ore": ("ore/ammonium", "Ammonium Chloride", 1, m("ammonium_chloride"), 0.8),
    "calcite_ore": ("ore/calcite", "Calcite", 0, m("calcite_crystal"), 0.4),
    "magnetite_ore": ("ore/magnetite", "Magnetite Ore", 1, m("magnetite"), 0.8),
    "thorite_ore": ("ore/thorium", "Thorite", 2, m("thorium_dust"), 0.8),
}
# ores that drop an item rather than themselves (ReactorOres.getOreDrop)
ORE_DROPS = {
    "calcite_ore": [m("calcite_crystal")],
    "ammonium_chloride_ore": [m("ammonium_chloride"), "minecraft:netherrack"],
    "magnetite_ore": [m("magnetite")],
}
MATS = {
    "concrete": ("mat/concrete", "Concrete"),
    "corium_block": ("mat/slag", "Corium"),
    "calcite_block": ("mat/calcite", "Calcite Block"),
    "steam_scrubber": (None, "Steam Scrubber"),
    "lodestone_block": ("mat/lodestone", "Lodestone Block"),
    "graphite_block": ("mat/graphite", "Graphite Block"),
}
SIDES = ["down", "up", "north", "south", "west", "east"]


def cube_block(block, texture, english):
    if ORIG_BLOCKS.exists() and texture:
        copy_png(ORIG_BLOCKS / f"{texture}.png", ASSETS / f"textures/block/{block}.png")
    write_json(ASSETS / f"models/block/{block}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": m(f"block/{block}")}})
    write_json(ASSETS / f"models/item/{block}.json", {"parent": m(f"block/{block}")})
    lang[f"block.{MOD}.{block}"] = english


def blocks():
    pickaxe, stone_tool, iron_tool = [], [], []
    for ore, (tex, english, level, product, xp) in ORES.items():
        cube_block(ore, tex, english)
        write_json(ASSETS / f"blockstates/{ore}.json", {"variants": {"": {"model": m(f"block/{ore}")}}})
        drops = ORE_DROPS.get(ore)
        if drops:
            entries = [{"type": "minecraft:alternatives", "children": [
                {"type": "minecraft:item", "name": m(ore), "conditions": [SILK]},
                {"type": "minecraft:item", "name": drops[0], "functions": [{"function": "minecraft:explosion_decay"}]}]}]
            pools = [{"rolls": 1, "entries": entries}]
            for extra in drops[1:]:
                pools.append({"rolls": 1, "entries": [{"type": "minecraft:item", "name": extra}], "conditions": [NO_SILK]})
            write_json(DATA / f"loot_table/blocks/{ore}.json", {"type": "minecraft:block", "pools": pools})
        else:
            self_drop(ore)
        smelting(f"{ore}_smelting", product, m(ore), xp)
        pickaxe.append(m(ore))
        (stone_tool if level == 1 else iron_tool if level == 2 else []).append(m(ore))
    for i, c in enumerate(FLUORITE):
        ore = f"{c}_fluorite_ore"
        cube_block(ore, f"ore/fluorite_{c}", f"{c.capitalize()} Fluorite")
        write_json(ASSETS / f"blockstates/{ore}.json", {"variants": {f"active={a}": {"model": m(f"block/{ore}")} for a in ("false", "true")}})
        crystal = m(f"{c}_fluorite")
        # 1-5 crystals, more with Fortune (1 + fortune + rand(5 + 2 * fortune) in the original)
        write_json(DATA / f"loot_table/blocks/{ore}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{
            "type": "minecraft:alternatives", "children": [
                {"type": "minecraft:item", "name": m(ore), "conditions": [SILK]},
                {"type": "minecraft:item", "name": crystal, "functions": [
                    {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 1, "max": 5}},
                    {"function": "minecraft:apply_bonus", "enchantment": "minecraft:fortune", "formula": "minecraft:uniform_bonus_count",
                     "parameters": {"bonusMultiplier": 3}},
                    {"function": "minecraft:explosion_decay"}]}]}]}]})
        smelting(f"{ore}_smelting", crystal, m(ore), 0.4)
        pickaxe.append(m(ore))
        block = f"{c}_fluorite_block"
        cube_block(block, f"mat/floblock_{c}", f"{c.capitalize()} Fluorite Block")
        write_json(ASSETS / f"blockstates/{block}.json", {"variants": {f"active={a}": {"model": m(f"block/{block}")} for a in ("false", "true")}})
        self_drop(block)
        shaped(f"{block}", m(block), ["CCC", "CCC", "CCC"], {"C": crystal})
        shapeless(f"{c}_fluorite_from_block", crystal, [m(block)], 9)
        pickaxe.append(m(block))
        lang[f"item.{MOD}.{c}_fluorite"] = f"{c.capitalize()} Fluorite Crystal"
    for block, (tex, english) in MATS.items():
        if block == "steam_scrubber":
            if ORIG_BLOCKS.exists():
                for i in range(6):
                    copy_png(ORIG_BLOCKS / f"mat/scrubber_{i}.png", ASSETS / f"textures/block/steam_scrubber_{i}.png")
            write_json(ASSETS / f"models/block/{block}.json", {
                "parent": "minecraft:block/cube", "render_type": "minecraft:cutout",
                "textures": {**{side: m(f"block/steam_scrubber_{i}") for i, side in enumerate(SIDES)}, "particle": m("block/steam_scrubber_2")}})
            write_json(ASSETS / f"models/item/{block}.json", {"parent": m(f"block/{block}")})
            lang[f"block.{MOD}.{block}"] = english
        else:
            cube_block(block, tex, english)
        write_json(ASSETS / f"blockstates/{block}.json", {"variants": {"": {"model": m(f"block/{block}")}}})
        self_drop(block)
        pickaxe.append(m(block))
    pickaxe.extend(machines())
    pickaxe.extend(core_blocks())
    pickaxe.extend(steam_blocks())
    pickaxe.extend(m6_blocks())
    write_json(RES / "data/minecraft/tags/block/mineable/pickaxe.json", {"replace": False, "values": pickaxe})
    write_json(RES / "data/minecraft/tags/block/needs_stone_tool.json", {"replace": False, "values": stone_tool})
    write_json(RES / "data/minecraft/tags/block/needs_iron_tool.json", {"replace": False, "values": iron_tool})


# ------------------------------------------------------------------------------------------------------------- machines
TILE_TEX = JAR / "Reika/ReactorCraft/Textures/TileEntity"
GUI_TEX = JAR / "Reika/ReactorCraft/Textures/GUI"
# the display transforms a block-shaped builtin/entity item model needs (vanilla's minecraft:block/block)
ENTITY_DISPLAY = {
    "gui": {"rotation": [30, 225, 0], "translation": [0, 0, 0], "scale": [0.625, 0.625, 0.625]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.25, 0.25, 0.25]},
    "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [0.5, 0.5, 0.5]},
    "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.375, 0.375, 0.375]},
    "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0], "scale": [0.4, 0.4, 0.4]},
    "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 0, 0], "scale": [0.4, 0.4, 0.4]},
}
# block id -> (English name, 3D model texture in the jar, GUI texture in the jar or None)
MODELLED = {
    "fluid_extractor": ("Centrifugal Fluid Extractor", "heavypump", None),
    "isotope_centrifuge": ("Isotope Centrifuge", "centrifuge", "centrifuge"),
    "uranium_processor": ("Uranium Processor", "processor", "processor"),
    "electrolyzer": ("Electrolyzer", "electrolyzer", "electrolyzer"),
    "gas_collector": ("Gas Collector", "co2collector", None),
    "waste_storage": ("Nuclear Waste Disposal Drum", "storage", "wastestorage"),
    "control_rod": ("Control Rod", "control", None),
    "steam_grate": ("Steam Grate", "steamgrate", None),
    "condenser": ("Condenser", "condenser", None),
    "reactor_pump": ("Pressurizer", "pump", None),
    "turbine_core": ("Turbine", "turbine", None),
}
# block id -> (English name, side texture, top and bottom texture, GUI texture)
CUBES = {
    "fluid_synthesizer": ("Fluid Synthesizer", "synthesizer", "synthesizer_top", "synthesizer"),
    "waste_container": ("Spent Fuel Container", "wastecontainer", "wastecontainer_top", "wastecontainer2"),
}
PARTICLE = "minecraft:block/iron_block"


def machines():
    out = []
    for block, (english, entity, gui) in MODELLED.items():
        if TILE_TEX.exists():
            copy_png(TILE_TEX / f"{entity}.png", ASSETS / f"textures/entity/{block}.png")
        if gui and GUI_TEX.exists():
            copy_png(GUI_TEX / f"{gui}.png", ASSETS / f"textures/gui/{block}.png")
        write_json(ASSETS / f"models/block/{block}.json", {"parent": "minecraft:block/block", "textures": {"particle": PARTICLE}})
        write_json(ASSETS / f"models/item/{block}.json", {"parent": "minecraft:builtin/entity", "display": ENTITY_DISPLAY})
        write_json(ASSETS / f"blockstates/{block}.json", {"variants": {"": {"model": m(f"block/{block}")}}})
        lang[f"block.{MOD}.{block}"] = english
        self_drop(block)
        out.append(m(block))
    for block, (english, side, top, gui) in CUBES.items():
        if ORIG_BLOCKS.exists():
            copy_png(ORIG_BLOCKS / f"{side}.png", ASSETS / f"textures/block/{block}.png")
            copy_png(ORIG_BLOCKS / f"{top}.png", ASSETS / f"textures/block/{block}_top.png")
        if GUI_TEX.exists():
            copy_png(GUI_TEX / f"{gui}.png", ASSETS / f"textures/gui/{block}.png")
        write_json(ASSETS / f"models/block/{block}.json", {"parent": "minecraft:block/cube_bottom_top", "textures": {
            "side": m(f"block/{block}"), "top": m(f"block/{block}_top"), "bottom": m(f"block/{block}_top")}})
        write_json(ASSETS / f"models/item/{block}.json", {"parent": m(f"block/{block}")})
        write_json(ASSETS / f"blockstates/{block}.json", {"variants": {"": {"model": m(f"block/{block}")}}})
        lang[f"block.{MOD}.{block}"] = english
        self_drop(block)
        out.append(m(block))
    # the decayer: tall stacks look like one chamber (state 0 alone, 1 one above, 2 both, 3 one below, 4 top and bottom)
    block = "waste_decayer"
    for state in range(5):
        if ORIG_BLOCKS.exists():
            copy_png(ORIG_BLOCKS / f"wastedecayer_#{state}.png", ASSETS / f"textures/block/{block}_{state}.png")
    for state in range(4):
        write_json(ASSETS / f"models/block/{block}_{state}.json", {"parent": "minecraft:block/cube_bottom_top", "textures": {
            "side": m(f"block/{block}_{state}"), "top": m(f"block/{block}_4"), "bottom": m(f"block/{block}_4")}})
    write_json(ASSETS / f"models/item/{block}.json", {"parent": m(f"block/{block}_0")})
    write_json(ASSETS / f"blockstates/{block}.json", {"variants": {
        "above=false,below=false": {"model": m(f"block/{block}_0")}, "above=true,below=false": {"model": m(f"block/{block}_1")},
        "above=true,below=true": {"model": m(f"block/{block}_2")}, "above=false,below=true": {"model": m(f"block/{block}_3")}}})
    if GUI_TEX.exists():
        copy_png(GUI_TEX / "wastedecayer.png", ASSETS / f"textures/gui/{block}.png")
    lang[f"block.{MOD}.{block}"] = "Forced Fission Chamber"
    self_drop(block)
    out.append(m(block))
    return out


CORE_TEXTURES = JAR / "assets/reactorcraft/textures/blocks"


def core_blocks():
    """The fission core's cube blocks: the fuel core (stackable), the coolant cell (look by coolant) and their GUI."""
    out = []
    block = "fuel_rod"
    for state in range(5):
        if ORIG_BLOCKS.exists():
            copy_png(ORIG_BLOCKS / f"fuel_#{state}.png", ASSETS / f"textures/block/{block}_{state}.png")
    for state in range(4):
        write_json(ASSETS / f"models/block/{block}_{state}.json", {"parent": "minecraft:block/cube_bottom_top", "textures": {
            "side": m(f"block/{block}_{state}"), "top": m(f"block/{block}_4"), "bottom": m(f"block/{block}_4")}})
    write_json(ASSETS / f"models/item/{block}.json", {"parent": m(f"block/{block}_0")})
    write_json(ASSETS / f"blockstates/{block}.json", {"variants": {
        "above=false,below=false": {"model": m(f"block/{block}_0")}, "above=true,below=false": {"model": m(f"block/{block}_1")},
        "above=true,below=true": {"model": m(f"block/{block}_2")}, "above=false,below=true": {"model": m(f"block/{block}_3")}}})
    if GUI_TEX.exists():
        copy_png(GUI_TEX / "fuelrod.png", ASSETS / f"textures/gui/{block}.png")
    lang[f"block.{MOD}.{block}"] = "Fuel Core"
    self_drop(block)
    out.append(m(block))
    block = "cpu"
    if ORIG_BLOCKS.exists():
        copy_png(ORIG_BLOCKS / "cpu.png", ASSETS / f"textures/block/{block}.png")
    write_json(ASSETS / f"models/block/{block}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": m(f"block/{block}")}})
    write_json(ASSETS / f"models/item/{block}.json", {"parent": m(f"block/{block}")})
    write_json(ASSETS / f"blockstates/{block}.json", {"variants": {"": {"model": m(f"block/{block}")}}})
    if GUI_TEX.exists():
        copy_png(GUI_TEX / "control2.png", ASSETS / f"textures/gui/{block}.png")
    lang[f"block.{MOD}.{block}"] = "Central Control"
    self_drop(block)
    out.append(m(block))
    block = "turbine_meter"
    for i, name in enumerate(("bottom", "side", "top")):
        if ORIG_BLOCKS.exists():
            copy_png(ORIG_BLOCKS / f"turbinemeter_#{i}.png", ASSETS / f"textures/block/{block}_{name}.png")
    write_json(ASSETS / f"models/block/{block}.json", {"parent": "minecraft:block/cube_bottom_top", "textures": {
        "side": m(f"block/{block}_side"), "top": m(f"block/{block}_top"), "bottom": m(f"block/{block}_bottom")}})
    write_json(ASSETS / f"models/item/{block}.json", {"parent": m(f"block/{block}")})
    write_json(ASSETS / f"blockstates/{block}.json", {"variants": {"": {"model": m(f"block/{block}")}}})
    lang[f"block.{MOD}.{block}"] = "Turbine Dynamometer"
    self_drop(block)
    out.append(m(block))
    block = "coolant_cell"
    states = ["empty", "water", "heavy", "sodium", "lithium"]
    for i, state in enumerate(states):
        if ORIG_BLOCKS.exists():
            copy_png(ORIG_BLOCKS / f"coolant_#{i}.png", ASSETS / f"textures/block/{block}_{state}.png")
        write_json(ASSETS / f"models/block/{block}_{state}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": m(f"block/{block}_{state}")}})
    write_json(ASSETS / f"models/item/{block}.json", {"parent": m(f"block/{block}_empty")})
    write_json(ASSETS / f"blockstates/{block}.json", {"variants": {f"coolant={st}": {"model": m(f"block/{block}_{st}")} for st in states}})
    lang[f"block.{MOD}.{block}"] = "Coolant Cell"
    self_drop(block)
    out.append(m(block))
    return out


def steam_blocks():
    """The steam side of the plant: the boiler (stackable), the steam line (a pipe that joins to what it can feed) and steam."""
    out = []
    block = "reactor_boiler"
    for state in range(4):
        if ORIG_BLOCKS.exists():
            copy_png(ORIG_BLOCKS / f"boiler_#{state}.png", ASSETS / f"textures/block/{block}_{state}.png")
    for state in range(4):
        write_json(ASSETS / f"models/block/{block}_{state}.json", {"parent": "minecraft:block/cube_bottom_top", "textures": {
            "side": m(f"block/{block}_{state}"), "top": m(f"block/{block}_0"), "bottom": m(f"block/{block}_0")}})
    write_json(ASSETS / f"models/item/{block}.json", {"parent": m(f"block/{block}_0")})
    write_json(ASSETS / f"blockstates/{block}.json", {"variants": {
        "above=false,below=false": {"model": m(f"block/{block}_0")}, "above=true,below=false": {"model": m(f"block/{block}_1")},
        "above=true,below=true": {"model": m(f"block/{block}_2")}, "above=false,below=true": {"model": m(f"block/{block}_3")}}})
    lang[f"block.{MOD}.{block}"] = "Steam Boiler"
    self_drop(block)
    out.append(m(block))
    block = "steam_line"
    wool = "minecraft:block/black_wool"
    write_json(ASSETS / f"models/block/{block}_core.json", {"textures": {"all": wool, "particle": wool}, "elements": [
        {"from": [5, 5, 5], "to": [11, 11, 11], "faces": {d: {"texture": "#all"} for d in ("north", "south", "east", "west", "up", "down")}}]})
    write_json(ASSETS / f"models/block/{block}_arm.json", {"textures": {"all": wool, "particle": wool}, "elements": [
        {"from": [5, 5, 0], "to": [11, 11, 5], "faces": {d: {"texture": "#all"} for d in ("north", "east", "west", "up", "down")}}]})
    write_json(ASSETS / f"models/item/{block}.json", {"parent": m(f"block/{block}_core")})
    arms = {"north": {}, "east": {"y": 90}, "south": {"y": 180}, "west": {"y": 270}, "up": {"x": 270}, "down": {"x": 90}}
    multipart = [{"apply": {"model": m(f"block/{block}_core")}}]
    for side, rotation in arms.items():
        multipart.append({"when": {side: "true"}, "apply": {"model": m(f"block/{block}_arm"), **rotation}})
    write_json(ASSETS / f"blockstates/{block}.json", {"multipart": multipart})
    lang[f"block.{MOD}.{block}"] = "Steam Line"
    self_drop(block)
    out.append(m(block))
    block = "steam"
    if ORIG_BLOCKS.exists():
        copy_png(ORIG_BLOCKS / "steam.png", ASSETS / f"textures/block/{block}.png")
    write_json(ASSETS / f"models/block/{block}.json", {"parent": "minecraft:block/cube_all", "render_type": "minecraft:translucent",
                                                       "textures": {"all": m(f"block/{block}")}})
    write_json(ASSETS / f"models/item/{block}.json", {"parent": m(f"block/{block}")})
    write_json(ASSETS / f"blockstates/{block}.json", {"variants": {"": {"model": m(f"block/{block}")}}})
    lang[f"block.{MOD}.{block}"] = "Steam"
    return out


def plain_cube(block, texture, english):
    """A cube machine with one texture on every side."""
    if ORIG_BLOCKS.exists():
        copy_png(ORIG_BLOCKS / f"{texture}.png", ASSETS / f"textures/block/{block}.png")
    write_json(ASSETS / f"models/block/{block}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": m(f"block/{block}")}})
    write_json(ASSETS / f"models/item/{block}.json", {"parent": m(f"block/{block}")})
    write_json(ASSETS / f"blockstates/{block}.json", {"variants": {"": {"model": m(f"block/{block}")}}})
    lang[f"block.{MOD}.{block}"] = english
    self_drop(block)
    return m(block)


def stacked_cube(block, texture, english, states=5, top_state=None, gui=None):
    """A machine that looks different when another is above or below it (states 0-3), with its own top and bottom."""
    top = top_state if top_state is not None else states - 1
    for state in range(states):
        if ORIG_BLOCKS.exists():
            copy_png(ORIG_BLOCKS / f"{texture}_#{state}.png", ASSETS / f"textures/block/{block}_{state}.png")
    for state in range(4):
        write_json(ASSETS / f"models/block/{block}_{state}.json", {"parent": "minecraft:block/cube_bottom_top", "textures": {
            "side": m(f"block/{block}_{state}"), "top": m(f"block/{block}_{top}"), "bottom": m(f"block/{block}_{top}")}})
    write_json(ASSETS / f"models/item/{block}.json", {"parent": m(f"block/{block}_0")})
    write_json(ASSETS / f"blockstates/{block}.json", {"variants": {
        "above=false,below=false": {"model": m(f"block/{block}_0")}, "above=true,below=false": {"model": m(f"block/{block}_1")},
        "above=true,below=true": {"model": m(f"block/{block}_2")}, "above=false,below=true": {"model": m(f"block/{block}_3")}}})
    if gui and GUI_TEX.exists():
        copy_png(GUI_TEX / f"{gui}.png", ASSETS / f"textures/gui/{block}.png")
    lang[f"block.{MOD}.{block}"] = english
    self_drop(block)
    return m(block)


def m6_blocks():
    out = [plain_cube("neutron_reflector", "reflector", "Neutron Reflector"), plain_cube("neutron_absorber", "absorber", "Neutron Absorber")]
    out.append(stacked_cube("breeder_core", "breeder", "Breeder Reactor Core", 5, gui="fuelrod"))
    out.append(stacked_cube("sodium_heater", "sodiumboiler", "Sodium Heater", 4, top_state=0))
    out.append(stacked_cube("thorium_core", "thorium", "Thorium Fuel Core", 5, gui="fuelpool"))
    out.append(stacked_cube("pebble_bed", "pebblebed", "Pebble Bed Reactor Core", 5, gui="pebblegui"))
    out.append(stacked_cube("co2_heater", "co2heater", "Carbon Dioxide Heat Exchanger", 4, top_state=0))
    block = "fuel_dump"
    if ORIG_BLOCKS.exists():
        for part, name in (("", "side"), ("_top", "top"), ("_bottom", "bottom")):
            copy_png(ORIG_BLOCKS / f"fueldump{part}.png", ASSETS / f"textures/block/{block}_{name}.png")
    write_json(ASSETS / f"models/block/{block}.json", {"parent": "minecraft:block/cube_bottom_top", "textures": {
        "side": m(f"block/{block}_side"), "top": m(f"block/{block}_top"), "bottom": m(f"block/{block}_bottom")}})
    write_json(ASSETS / f"models/item/{block}.json", {"parent": m(f"block/{block}")})
    write_json(ASSETS / f"blockstates/{block}.json", {"variants": {"": {"model": m(f"block/{block}")}}})
    lang[f"block.{MOD}.{block}"] = "Fuel Dump Valve"
    self_drop(block)
    out.append(m(block))
    # spilled fuel salt: up to eight layers
    block = "thorium_fuel"
    for level in range(1, 9):
        write_json(ASSETS / f"models/block/{block}_{level}.json", {"textures": {"all": m("block/fluid/lifbe_fuel"), "particle": m("block/fluid/lifbe_fuel")},
                   "elements": [{"from": [0, 0, 0], "to": [16, level * 2, 16], "faces": {
                       d: {"texture": "#all"} for d in ("north", "south", "east", "west", "up", "down")}}]})
    write_json(ASSETS / f"blockstates/{block}.json", {"variants": {f"level={level}": {"model": m(f"block/{block}_{level}")} for level in range(1, 9)}})
    write_json(ASSETS / f"models/item/{block}.json", {"parent": m(f"block/{block}_8")})
    lang[f"block.{MOD}.{block}"] = "Spilled Thorium Fuel"
    return out


SILK = {"condition": "minecraft:match_tool", "predicate": {"predicates": {"minecraft:enchantments": [
    {"enchantments": "minecraft:silk_touch", "levels": {"min": 1}}]}}}
NO_SILK = {"condition": "minecraft:inverted", "term": SILK}


def self_drop(block):
    write_json(DATA / f"loot_table/blocks/{block}.json", {"type": "minecraft:block", "pools": [
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": m(block)}], "conditions": [{"condition": "minecraft:survives_explosion"}]}]})


# --------------------------------------------------------------------------------------------------------------- fluids
FLUIDS = {
    "heavy_water": "Heavy Water", "hydrofluoric_acid": "Hydrofluoric Acid", "uranium_hexafluoride": "Uranium Hexafluoride",
    "ammonia": "Ammonia", "sodium": "Molten Sodium", "chlorine": "Chlorine Gas", "oxygen": "Oxygen Gas", "liquid_oxygen": "Liquid Oxygen",
    "deuterium": "Deuterium", "tritium": "Tritium", "fusion_plasma": "Fusion Plasma", "low_pressure_ammonia": "Low Pressure Ammonia",
    "low_pressure_water": "Low Pressure Water", "hot_sodium": "Superheated Sodium", "warm_sodium": "Hot Sodium", "co2": "Carbon Dioxide",
    "hot_co2": "Hot Carbon Dioxide", "corium": "Corium", "nuclear_waste": "Nuclear Waste", "lithium": "Molten Lithium",
    "lifbe": "Lithium Beryllium Fluoride", "lifbe_fuel": "Thorium Fuel Salt", "lifbe_fuel_preheat": "Preheated Thorium Fuel Salt",
    "hot_lifbe": "Hot Lithium Beryllium Fluoride",
}
FLUID_TEXTURES = ["heavywater", "hf", "uf6", "ammonia", "sodium", "sodiumhot", "chlorine", "oxygen", "deuterium", "tritium", "plasma",
                  "co2", "slag_flow", "lithium", "lifbe", "lifbe_fuel", "lifbe_hot"]


def fluids():
    if ORIG_BLOCKS.exists():
        for tex in FLUID_TEXTURES:
            copy_png(ORIG_BLOCKS / f"fluid/{tex}.png", ASSETS / f"textures/block/fluid/{tex}.png")
    for fluid, english in FLUIDS.items():
        lang[f"fluid_type.{MOD}.{fluid}"] = english


# -------------------------------------------------------------------------------------------------------------- recipes
def ing(v):
    return {"tag": v[1:]} if v.startswith("#") else {"item": v}


def shaped(name, result, pattern, key, count=1):
    write_json(DATA / f"recipe/{name}.json", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": pattern,
                                              "key": {k: ing(v) for k, v in key.items()}, "result": {"id": result, "count": count}})


def shapeless(name, result, ingredients, count=1):
    write_json(DATA / f"recipe/{name}.json", {"type": "minecraft:crafting_shapeless", "category": "misc",
                                              "ingredients": [ing(v) for v in ingredients], "result": {"id": result, "count": count}})


def smelting(name, result, ingredient, xp):
    write_json(DATA / f"recipe/{name}.json", {"type": "minecraft:smelting", "category": "misc", "ingredient": ing(ingredient),
                                              "result": {"id": result, "count": 1}, "experience": xp, "cookingtime": 200})


R = "rotarycraft:"
STEEL = "#c:ingots/steel"


def recipes():
    # ReactorRecipes.addMisc
    shaped("calcite_block", m("calcite_block"), ["CCC", "CCC", "CCC"], {"C": m("calcite_crystal")})
    shapeless("calcite_crystal_from_block", m("calcite_crystal"), [m("calcite_block")], 9)
    shaped("graphite_block", m("graphite_block"), ["CCC", "CCC", "CCC"], {"C": m("graphite")})
    shapeless("graphite_from_block", m("graphite"), [m("graphite_block")], 9)
    shaped("lodestone_block", m("lodestone_block"), ["CCC", "CCC", "CCC"], {"C": m("magnetite")})
    shapeless("magnetite_from_block", m("magnetite"), [m("lodestone_block")], 9)
    shaped("steam_scrubber", m("steam_scrubber"), ["IWI", "WPW", "IWI"], {"I": "minecraft:iron_bars", "W": "#minecraft:wool", "P": R + "pipe"})
    shapeless("concrete", m("concrete"), ["minecraft:clay", "#minecraft:sand", "minecraft:gravel", "minecraft:water_bucket"], 4)
    shapeless("leather_from_quicklime", "minecraft:leather", [m("quicklime"), "minecraft:rotten_flesh"])
    shapeless("paper_from_logs", "minecraft:paper", [m("quicklime"), "minecraft:water_bucket", "#minecraft:logs"], 16)
    shapeless("paper_from_planks", "minecraft:paper", [m("quicklime"), "minecraft:water_bucket", "#minecraft:planks"], 4)
    shapeless("paper_from_sticks", "minecraft:paper", [m("quicklime"), "minecraft:water_bucket", "#c:rods/wooden"], 1)
    smelting("quicklime_from_calcite", m("quicklime"), m("calcite_crystal"), 0.2)
    # ReactorRecipes.addCrafting
    shaped("fuel_canister", m("fuel_canister"), [" S ", "SCS", " S "], {"S": m("cd_in_ag_alloy_ingot"), "C": "#c:chests/wooden"})
    shaped("absorption_rod", m("absorption_rod"), ["SAS", "ACA", "SAS"], {"S": STEEL, "A": m("cd_in_ag_alloy_ingot"), "C": m("graphite")})
    shaped("ferromagnetic_plate", m("ferromagnetic_plate"), ["SSS"], {"S": m("ferromagnetic_ingot")}, 3)
    shaped("magnetic_core", m("magnetic_core"), ["CCC", "C C", "CCC"], {"C": m("ferromagnetic_plate")})
    shaped("hysteresis_ring", m("hysteresis_ring"), ["CCC", "C C", "CCC"], {"C": m("hysteresis_plate")})
    shaped("gold_wiring", m("gold_wiring"), ["  G", " G ", "G  "], {"G": "#c:ingots/gold"}, 2)
    # DifficultyEffects.PARTCRAFT is 3 on normal difficulty
    shaped("hysteresis_plate", m("hysteresis_plate"), ["ISI"], {"I": "#c:ingots/iron", "S": STEEL}, 3)
    shaped("coolant_pack", m("coolant_pack"), ["SPS", "S S", "SpS"], {"S": STEEL, "P": "minecraft:glass_pane", "p": R + "pipe"})
    shaped("radiation_shielding_fabric_from_lead", m("radiation_shielding_fabric"), ["LDL", "LDL", "LDL"],
           {"D": "#c:ingots/lead", "L": "minecraft:leather"}, 2)
    shaped("steam_turbine_core", m("steam_turbine_core"), ["CCC", "CTC", "CCC"], {"C": m("tungsten_carbide_ingot"), "T": R + "compound_turbine"})
    shaped("empty_canister", m("empty_canister"), [" i ", "igi", " i "], {"g": "#c:glass_blocks/colorless", "i": "#c:ingots/iron"}, 16)
    # the shapeless Cd-In-Ag alloy and ferromagnetic ingot are blast-furnace crafts in the original; the blast furnace's grid is
    # shaped, so every order of the row works
    from itertools import permutations
    for n, order in enumerate(permutations(["#c:ingots/cadmium", "#c:ingots/indium", "#c:ingots/silver"])):
        write_json(DATA / f"recipe/blast_crafting/cd_in_ag_alloy_ingot_{n}.json", {
            "type": R + "blast_crafting", "pattern": ["ABC"], "key": {k: ing(v) for k, v in zip("ABC", order)},
            "result": {"id": m("cd_in_ag_alloy_ingot"), "count": 3}, "temperature": 1600, "speed": 1, "xp": 0.8})
    for n, order in enumerate(permutations([STEEL, "#c:ingots/iron", m("magnetite")])):
        write_json(DATA / f"recipe/blast_crafting/ferromagnetic_ingot_{n}.json", {
            "type": R + "blast_crafting", "pattern": ["ABC"], "key": {k: ing(v) for k, v in zip("ABC", order)},
            "result": {"id": m("ferromagnetic_ingot"), "count": 1}, "temperature": 1200, "speed": 1, "xp": 1.0})
    # ReactorRecipes.addMachines (the machines in this milestone)
    P, PIPE, GLASS = R + "base_panel", R + "pipe", "#c:glass_blocks/colorless"
    shaped("fluid_extractor", m("fluid_extractor"), ["PpP", "GIG", "PSP"],
           {"P": P, "p": PIPE, "G": GLASS, "I": R + "impeller", "S": R + "shaft_core"})
    shaped("isotope_centrifuge", m("isotope_centrifuge"), ["SPS", "B B", "PGP"],
           {"B": R + "bedrock_ingot", "P": P, "S": STEEL, "G": R + "bedrock_gear_unit_16"})
    shaped("uranium_processor", m("uranium_processor"), ["POP", "OMO"], {"O": m("obsidian_tank"), "M": R + "mixer", "P": PIPE})
    shaped("waste_container", m("waste_container"), ["SCS", "CcC", "SCS"], {"S": STEEL, "C": R + "cooling_fin", "c": "#c:chests/wooden"})
    shaped("fluid_synthesizer", m("fluid_synthesizer"), ["SpS", "pMp", "ShS"], {"S": STEEL, "M": R + "mixer", "p": P, "h": R + "igniter"})
    shaped("waste_decayer", m("waste_decayer"), ["SHS", "GPG", "SHS"],
           {"H": "minecraft:hopper", "G": R + "blast_glass", "P": m("waste_container"), "S": R + "silumin_ingot"})
    shaped("electrolyzer", m("electrolyzer"), ["SPS", "PRP", "BPB"], {"P": PIPE, "B": P, "S": STEEL, "R": R + "reservoir"})
    shaped("waste_storage", m("waste_storage"), ["SPS", "PCP", "SPS"], {"S": STEEL, "P": P, "C": "#c:chests/wooden"})
    shaped("gas_collector", m("gas_collector"), [" p ", "SpS", "PpP"], {"p": PIPE, "P": P, "S": STEEL})
    # ReactorRecipes.addMachines: the fission core
    shaped("fuel_rod", m("fuel_rod"), ["SHS", "PCP", "SCS"], {"P": P, "S": STEEL, "C": m("fuel_canister"), "H": "minecraft:hopper"})
    shaped("cpu", m("cpu"), ["SCS", "CGC", "SCS"], {"S": P, "C": R + "circuit_board", "G": R + "steel_gear_unit_2"})
    shaped("control_rod", m("control_rod"), ["SGS", "RRR", "PPP"],
           {"S": STEEL, "P": P, "R": m("absorption_rod"), "G": R + "steel_gear_unit_2"})
    shaped("coolant_cell", m("coolant_cell"), ["SPS", "GRG", "SPS"], {"S": STEEL, "P": PIPE, "G": "minecraft:glass", "R": R + "reservoir"})
    # ReactorRecipes.addMachines: the steam side
    shaped("reactor_boiler", m("reactor_boiler"), ["SPS", "PrP", "SPS"], {"S": STEEL, "P": P, "r": R + "reservoir"})
    shaped("steam_line", m("steam_line"), ["NPN", "NPN", "NPN"], {"N": "#minecraft:wool", "P": PIPE}, 3)
    shaped("steam_grate", m("steam_grate"), ["SIS", "p p", "SPS"], {"S": STEEL, "I": "minecraft:iron_bars", "p": P, "P": PIPE})
    shaped("condenser", m("condenser"), ["SPS", "pRp", "FFF"], {"S": STEEL, "P": P, "p": PIPE, "R": R + "reservoir", "F": R + "cooling_fin"})
    shaped("reactor_pump", m("reactor_pump"), ["PpP", "gCg", "PsP"],
           {"P": P, "g": "minecraft:glass_pane", "p": PIPE, "C": R + "compressor", "s": R + "shaft_core"})
    shaped("turbine_core", m("turbine_core"), ["BBB", "BCB", "BBB"], {"B": R + "propeller", "C": m("steam_turbine_core")})
    shaped("turbine_meter", m("turbine_meter"), ["SrS", "PGP", "PCP"],
           {"S": STEEL, "r": "minecraft:redstone", "P": P, "G": "minecraft:glowstone", "C": R + "circuit_board"})
    shaped("remote_control", m("remote_control"), ["SES", "BCB", "BPB"],
           {"S": STEEL, "E": "minecraft:ender_pearl", "B": "minecraft:stone_button", "C": R + "circuit_board", "P": P})
    F = m("radiation_shielding_fabric")
    shaped("hazmat_helmet", m("hazmat_helmet"), ["FFF", "F F"], {"F": F})
    shaped("hazmat_chestplate", m("hazmat_chestplate"), ["F F", "FFF", "FFF"], {"F": F})
    shaped("hazmat_leggings", m("hazmat_leggings"), ["FFF", "F F", "F F"], {"F": F})
    shaped("hazmat_boots", m("hazmat_boots"), ["F F", "F F"], {"F": F})
    shaped("geiger_counter", m("geiger_counter"), [" r ", "sSs", "sgs"],
           {"r": R + "radar_unit", "s": STEEL, "S": R + "screen", "g": R + "steel_gear"})
    shaped("radiation_cleaner", m("radiation_cleaner"), [" sp", "sbs", "ss "], {"b": "minecraft:water_bucket", "s": STEEL, "p": R + "pipe"})
    # ReactorRecipes.addMachines: reflector and absorber
    shaped("neutron_reflector", m("neutron_reflector"), ["GGG", "GSG", "GGG"], {"G": m("graphite"), "S": "#c:storage_blocks/steel"})
    shaped("neutron_absorber_depleted", m("neutron_absorber"), [" P ", "PCP", " P "], {"C": "#c:storage_blocks/steel", "P": m("depleted_uranium")})
    # ReactorRecipes: the breeder
    shaped("uranium_fuel_pellet", m("uranium_fuel_pellet"), ["dd", "dd"], {"d": m("enriched_uranium_dust")}, 2)
    shaped("breeder_fuel", m("breeder_fuel"), [" D ", "DED", " D "], {"D": m("depleted_uranium"), "E": m("uranium_fuel_pellet")}, 4)
    shaped("breeder_core", m("breeder_core"), ["SPS", "PCP", "SPS"], {"P": P, "S": STEEL, "C": m("fuel_rod")})
    shaped("sodium_heater", m("sodium_heater"), [" i ", "ibi", " i "], {"b": m("reactor_boiler"), "i": "minecraft:iron_ingot"})
    shaped("thorium_core", m("thorium_core"), ["aSa", "PCP", "tPt"],
           {"t": R + "tungsten_ingot", "a": R + "silumin_ingot", "P": P, "S": STEEL, "C": m("fuel_rod")})
    shaped("fuel_dump", m("fuel_dump"), ["pIp", "BPB", "pbp"],
           {"b": "minecraft:iron_bars", "p": PIPE, "P": R + "bedrock_pipe", "B": P, "I": R + "impeller"})
    shaped("pebble_bed", m("pebble_bed"), ["SHS", "PCP", "SHS"], {"H": "minecraft:hopper", "P": P, "S": STEEL, "C": m("fuel_rod")})
    shaped("co2_heater", m("co2_heater"), [" i ", "ibi", " i "], {"b": m("reactor_boiler"), "i": P})
    write_json(DATA / "recipe/blast_crafting/triso_pellet.json", {
        "type": R + "blast_crafting", "pattern": [" G ", "GUG", " G "], "key": {"G": ing(m("graphite")), "U": ing(m("uranium_dust"))},
        "result": {"id": m("triso_pellet"), "count": 4}, "temperature": 750, "speed": 1, "xp": 0.5})
    shaped("neutron_absorber_triso", m("neutron_absorber"), ["PPP", "PCP", "PPP"], {"C": "#c:storage_blocks/steel", "P": m("depleted_triso_pellet")})
    shaped("radiation_shielding_fabric_from_pellets", m("radiation_shielding_fabric"), ["LDL", "LDL", "LDL"],
           {"D": m("depleted_triso_pellet"), "L": "minecraft:leather"}, 1)
    # RotaryCraft grinder
    write_json(DATA / "recipe/grinding/emerald_dust.json", {"type": R + "grinding", "ingredient": ing("#c:gems/emerald"),
                                                           "result": {"id": m("emerald_dust"), "count": 1}})
    write_json(DATA / "recipe/grinding/uranium_dust.json", {"type": R + "grinding", "ingredient": ing("#c:ingots/uranium"),
                                                           "result": {"id": m("uranium_dust"), "count": 1}})
    # RotaryCraft friction heater
    write_json(DATA / "recipe/friction_heating/graphite.json", {"type": R + "friction_heating", "ingredient": ing(R + "coal_dust"),
                                                               "result": {"id": m("graphite"), "count": 1}, "temperature": 400, "duration": 100})



def radiation_assets():
    armor = JAR / "assets/reactorcraft/textures/models/armor"
    if armor.exists():
        copy_png(armor / "haz_1.png", ASSETS / "textures/models/armor/hazmat_layer_1.png")
        copy_png(armor / "haz_2.png", ASSETS / "textures/models/armor/hazmat_layer_2.png")
    cloud = JAR / "Reika/ReactorCraft/Textures/radiation2.png"
    if cloud.exists():
        copy_png(cloud, ASSETS / "textures/entity/radiation.png")
    lang["entity.reactorcraft.radiation"] = "Radiation"
    lang["entity.reactorcraft.neutron"] = "Neutron"
    lang["entity.reactorcraft.nuclear_waste_item"] = "Nuclear Waste"


def radiation_data():
    """The radiation damage type (it ignores armor) and what protects against each strength of radiation."""
    write_json(DATA / "damage_type/radiation.json", {"message_id": "radiation", "scaling": "never", "exhaustion": 0.0})
    write_json(RES / "data/minecraft/tags/damage_type/bypasses_armor.json", {"replace": False, "values": [m("radiation")]})
    lang["death.attack.radiation"] = "%1$s died of radiation poisoning"
    lang["death.attack.radiation.player"] = "%1$s died of radiation poisoning while fleeing %2$s"
    lang["effect.reactorcraft.radiation"] = "Radiation Sickness"
    hazmat = [m("hazmat_helmet"), m("hazmat_chestplate"), m("hazmat_leggings"), m("hazmat_boots")]
    write_json(DATA / "tags/item/radiation_protection/hazmat.json", {"replace": False, "values": hazmat})
    optional = lambda ids: [{"id": i, "required": False} for i in ids]
    write_json(DATA / "tags/item/radiation_protection/bedrock.json", {"replace": False, "values": optional(
        [f"rotarycraft:bedrock_{part}" for part in ("helmet", "chestplate", "leggings", "boots")])})
    dense = [f"minecraft:{mat}_{part}" for mat in ("iron", "diamond", "netherite") for part in ("helmet", "chestplate", "leggings", "boots")]
    write_json(DATA / "tags/item/radiation_protection/dense.json", {"replace": False, "values": dense})


# ----------------------------------------------------------------------------------------------------------------- tags
# block tags for what stops neutrons and radiation (RadiationShield); other mods' entries are optional
SHIELDS = {
    "steel": ["#c:storage_blocks/steel"],
    "concrete": [m("concrete")],
    "water": ["minecraft:water"],
    "bedrock_ingot": ["#c:storage_blocks/bedrock"],
    "lead": ["#c:storage_blocks/lead"],
    "obsidian": ["minecraft:obsidian"],
    "blast_glass": ["rotarycraft:blast_glass"],
}


def tags():
    for name, values in SHIELDS.items():
        write_json(DATA / f"tags/block/shield/{name}.json", {"replace": False, "values": [
            v if not v.startswith(("#", "rotarycraft:")) else {"id": v, "required": False} for v in values]})
    item_tags = {
        "c:ingots/uranium": [m("uranium_ingot")], "c:ingots/cadmium": [m("cadmium_ingot")], "c:ingots/indium": [m("indium_ingot")],
        "c:ingots/silver": [m("silver_ingot")], "c:gems/fluorite": [m(f"{c}_fluorite") for c in FLUORITE],
        "c:gems/calcite": [m("calcite_crystal")], "c:dusts/thorium": [m("thorium_dust")], "c:dusts/emerald": [m("emerald_dust")],
        "c:dusts/uranium": [m("uranium_dust")],
        "c:ores/uranium": [m("pitchblende_ore"), m("endblende_ore")], "c:ores/cadmium": [m("cadmium_ore")], "c:ores/indium": [m("indium_ore")],
        "c:ores/silver": [m("silver_ore")], "c:ores/ammonium": [m("ammonium_chloride_ore")], "c:ores/calcite": [m("calcite_ore")],
        "c:ores/magnetite": [m("magnetite_ore")], "c:ores/thorium": [m("thorite_ore")],
        "c:ores/fluorite": [m(f"{c}_fluorite_ore") for c in FLUORITE],
        "c:storage_blocks/fluorite": [m(f"{c}_fluorite_block") for c in FLUORITE], "c:storage_blocks/graphite": [m("graphite_block")],
    }
    for tag, values in item_tags.items():
        ns, path = tag.split(":")
        if values:
            write_json(RES / f"data/{ns}/tags/item/{path}.json", {"replace": False, "values": values})
        if path.startswith(("ores/", "storage_blocks/")) and values:
            write_json(RES / f"data/{ns}/tags/block/{path}.json", {"replace": False, "values": values})
    write_json(RES / "data/c/tags/item/ores.json", {"replace": False, "values": [f"#c:{t.split(':')[1]}" for t in item_tags if t.startswith("c:ores/")]})


# ----------------------------------------------------------------------------------------------------------- world gen
# ore -> (min y, max y, vein size, veins per chunk, dimension, replace) from ReactorOres
GEN = {
    "pitchblende_ore": (8, 24, 16, 3, "overworld", "minecraft:stone_ore_replaceables"),
    "cadmium_ore": (12, 32, 9, 3, "overworld", "minecraft:stone_ore_replaceables"),
    "indium_ore": (0, 16, 7, 2, "overworld", "minecraft:stone_ore_replaceables"),
    "silver_ore": (16, 40, 9, 2, "overworld", "minecraft:stone_ore_replaceables"),
    "endblende_ore": (0, 64, 16, 6, "end", "end_stone"),
    "ammonium_chloride_ore": (32, 32, 8, 6, "nether", "minecraft:base_stone_nether"),
    "calcite_ore": (32, 60, 4, 12, "overworld", "minecraft:stone_ore_replaceables"),
    "magnetite_ore": (60, 128, 16, 7, "overworld", "minecraft:stone_ore_replaceables"),
    "thorite_ore": (0, 32, 24, 1, "nether", "minecraft:base_stone_nether"),
}
BIOMES = {"overworld": "#minecraft:is_overworld", "nether": "#minecraft:is_nether", "end": "#minecraft:is_end"}


def worldgen():
    for ore, (lo, hi, size, count, dim, replace) in GEN.items():
        target = ({"predicate_type": "minecraft:block_match", "block": "minecraft:end_stone"} if replace == "end_stone"
                  else {"predicate_type": "minecraft:tag_match", "tag": replace})
        write_json(DATA / f"worldgen/configured_feature/{ore}.json", {"type": "minecraft:ore", "config": {
            "size": size, "discard_chance_on_air_exposure": 0, "targets": [{"target": target, "state": {"Name": m(ore)}}]}})
        write_json(DATA / f"worldgen/placed_feature/{ore}.json", {"feature": m(ore), "placement": [
            {"type": "minecraft:count", "count": count}, {"type": "minecraft:in_square"},
            {"type": "minecraft:height_range", "height": {"type": "minecraft:uniform", "min_inclusive": {"absolute": lo},
                                                          "max_inclusive": {"absolute": hi}}},
            {"type": "minecraft:biome"}]})
        biomes = BIOMES[dim]
        if ore == "pitchblende_ore":
            # the original only generates pitchblende under oceans, rivers and mushroom islands
            biomes = {"type": "neoforge:or", "values": ["#minecraft:is_ocean", "#minecraft:is_river", ["minecraft:mushroom_fields"]]}
        write_json(DATA / f"neoforge/biome_modifier/{ore}.json", {"type": "neoforge:add_features", "biomes": biomes,
                                                                 "features": m(ore), "step": "underground_ores"})
    # fluorite: one colour per chunk, 12 veins of 8 between y 32 and 60
    write_json(DATA / "worldgen/configured_feature/fluorite_ore.json", {"type": m("fluorite_vein"), "config": {}})
    write_json(DATA / "worldgen/placed_feature/fluorite_ore.json", {"feature": m("fluorite_ore"), "placement": [
        {"type": "minecraft:count", "count": 12}, {"type": "minecraft:in_square"},
        {"type": "minecraft:height_range", "height": {"type": "minecraft:uniform", "min_inclusive": {"absolute": 32}, "max_inclusive": {"absolute": 60}}},
        {"type": "minecraft:biome"}]})
    write_json(DATA / "neoforge/biome_modifier/fluorite_ore.json", {"type": "neoforge:add_features", "biomes": "#minecraft:is_overworld",
                                                                   "features": m("fluorite_ore"), "step": "underground_ores"})


def nbt_payload(tag_type, value):
    """Minimal big-endian NBT encoder: compound=dict, list=('list', type, items), int=int, string=str."""
    import struct
    if tag_type == 3:
        return struct.pack(">i", value)
    if tag_type == 8:
        raw = value.encode("utf-8")
        return struct.pack(">H", len(raw)) + raw
    if tag_type == 9:
        _, item_type, items = value
        return struct.pack(">bi", item_type if items else 0, len(items)) + b"".join(nbt_payload(item_type, i) for i in items)
    if tag_type == 10:
        out = b""
        for key, (t, v) in value.items():
            raw = key.encode("utf-8")
            out += struct.pack(">bH", t, len(raw)) + raw + nbt_payload(t, v)
        return out + b"\x00"
    raise ValueError(tag_type)


def empty_structure(name, size=3):
    """An all-air structure template for GameTests."""
    import gzip
    import struct
    root = {
        "DataVersion": (3, 3955),
        "size": (9, ("list", 3, [size, size, size])),
        "palette": (9, ("list", 10, [{"Name": (8, "minecraft:air")}])),
        "blocks": (9, ("list", 10, [])),
        "entities": (9, ("list", 10, [])),
    }
    data = struct.pack(">bH", 10, 0) + nbt_payload(10, root)
    path = DATA / f"structure/{name}.nbt"
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(gzip.compress(data, mtime=0))
    written.append(path)


def misc_lang():
    lang["itemGroup.reactorcraft"] = "ReactorCraft"


items()
blocks()
fluids()
recipes()
tags()
radiation_data()
radiation_assets()
worldgen()
misc_lang()
empty_structure("gametest/empty", 5)
write_json(ASSETS / "lang/en_us.json", dict(sorted(lang.items())))
print(f"wrote {len(written)} files")
