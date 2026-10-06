package net.scwunge.reactorcraft;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Client-side options (the original's client-only ReactorOptions). */
public final class ReactorClientConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue VISIBLE_NEUTRONS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        VISIBLE_NEUTRONS = builder.comment("Draw neutrons as little blue squares. Turning this off can help on slow computers.")
                .define("visibleNeutrons", true);
        SPEC = builder.build();
    }

    private ReactorClientConfig() {
    }
}
