package net.scwunge.reactorcraft.core;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.level.BlockEvent;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Checks before ReactorCraft changes the world on its own (meltdowns, radiation, hot reactor parts): the mobGriefing game
 * rule, and a block-break event in the name of the machine's owner so claim and protection mods can say no.
 */
public final class WorldSafety {
    private static final UUID ANONYMOUS = UUID.nameUUIDFromBytes("reactorcraft".getBytes(StandardCharsets.UTF_8));

    private WorldSafety() {
    }

    /** A stand-in player for {@code owner} (or an anonymous one), for permission checks. */
    public static Player actingPlayer(ServerLevel level, @Nullable UUID owner) {
        return FakePlayerFactory.get(level, new GameProfile(owner == null ? ANONYMOUS : owner, "[ReactorCraft]"));
    }

    public static boolean griefingAllowed(Level level) {
        return level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
    }

    /** Whether a machine owned by {@code owner} may change the block at {@code pos}. */
    public static boolean mayChange(Level level, BlockPos pos, @Nullable UUID owner) {
        if (!(level instanceof ServerLevel server) || !griefingAllowed(level) || !level.isLoaded(pos)) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        var event = new BlockEvent.BreakEvent(server, pos, state, actingPlayer(server, owner));
        return !NeoForge.EVENT_BUS.post(event).isCanceled();
    }
}
