package net.scwunge.reactorcraft.core;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.scwunge.reactorcraft.ReactorCraft;
import org.jetbrains.annotations.Nullable;

/**
 * Materials that stop neutrons and radiation (the original's RadiationShield). Which blocks count as each is a block tag
 * ({@code reactorcraft:shield/<name>}), so packs can add their own.
 */
public enum RadiationShield {
    STEEL("Steel", 90, 95),
    CONCRETE("Concrete", 60, 70),
    WATER("Water", 30, 10),
    BEDROCK_INGOT("Bedrock Ingot", 97.5, 100),
    LEAD("Lead", 75, 50),
    OBSIDIAN("Obsidian", 50, 80),
    BLAST_GLASS("Blast Glass", 80, 20);

    public final String displayName;
    /** Percent of neutrons absorbed passing through. */
    public final double neutronAbsorbChance;
    /** Percent of radiation kept in. */
    public final double radiationDeflectChance;
    private final TagKey<Block> tag;

    RadiationShield(String displayName, double neutronAbsorbChance, double radiationDeflectChance) {
        this.displayName = displayName;
        this.neutronAbsorbChance = neutronAbsorbChance;
        this.radiationDeflectChance = radiationDeflectChance;
        this.tag = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(ReactorCraft.MODID, "shield/" + name().toLowerCase()));
    }

    /** The shield material a block is, or null. */
    @Nullable
    public static RadiationShield of(BlockState state) {
        for (RadiationShield shield : values()) {
            if (state.is(shield.tag)) {
                return shield;
            }
        }
        return null;
    }
}
