package net.scwunge.reactorcraft.core;

/** The kinds of reactor, which decide how well heat passes between their parts and what neutrons they make. */
public enum ReactorType {
    FISSION,
    BREEDER,
    HTGR,
    THORIUM,
    FUSION,
    SOLAR,
    NONE;

    public double hpTurbineMultiplier() {
        return switch (this) {
            case FUSION -> 1.5F;
            case HTGR -> 0.35F;
            case SOLAR -> 0.15F;
            case NONE -> 0;
            default -> 1;
        };
    }

    public float controlCpuHeatEfficiency() {
        return switch (this) {
            case HTGR, SOLAR, FUSION -> typeMismatchHeatEfficiency();
            default -> 1;
        };
    }

    /** For conducting heat into parts of another reactor type. */
    public float typeMismatchHeatEfficiency() {
        return switch (this) {
            case FISSION -> 1;
            case HTGR -> 0.0625F;
            case SOLAR, FUSION, NONE -> 0;
            case THORIUM -> 0.25F;
            default -> 0.5F;
        };
    }

    public NeutronType neutronType() {
        return switch (this) {
            case BREEDER -> NeutronType.BREEDER;
            case FISSION -> NeutronType.FISSION;
            case FUSION -> NeutronType.FUSION;
            case THORIUM -> NeutronType.THORIUM;
            default -> null;
        };
    }
}
