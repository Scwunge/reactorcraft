package net.scwunge.reactorcraft.content.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.scwunge.reactorcraft.core.ReactorBlockEntity;
import net.scwunge.reactorcraft.core.SteamConnectable;

import java.util.function.Supplier;

/** A steam grate: steam lines join to any side of it, and shift-right-clicking it toggles its redstone requirement. */
public class SteamGrateBlock extends ReactorMachineBlock implements SteamConnectable {
    public SteamGrateBlock(Properties properties, Supplier<? extends BlockEntityType<? extends ReactorBlockEntity>> type) {
        super(properties, type, true, false);
    }

    @Override
    public boolean connectsSteam(BlockState state, Direction side) {
        return true;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isShiftKeyDown() && level.getBlockEntity(pos) instanceof SteamGrateBlockEntity grate) {
            if (!level.isClientSide) {
                boolean needs = grate.toggleRequireRedstone();
                player.displayClientMessage(Component.literal(needs ? "Works only with a redstone signal" : "Works only without a redstone signal"), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }
}
