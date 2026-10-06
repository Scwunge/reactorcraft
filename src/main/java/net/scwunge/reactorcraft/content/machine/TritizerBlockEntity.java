package net.scwunge.reactorcraft.content.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.scwunge.reactorcraft.content.entity.NeutronEntity;
import net.scwunge.reactorcraft.core.Chance;
import net.scwunge.reactorcraft.core.NeutronTile;
import net.scwunge.reactorcraft.registry.ReactorBlockEntities;
import net.scwunge.reactorcraft.registry.ReactorFluids;
import net.scwunge.rotarycraft.pipe.FluidAccess;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Tritizer (TileEntityTritizer): a column of these in a reactor turns the fluid in them under neutron fire: deuterium into tritium (25 mB at a time, 75% of
 * the neutrons that hit it), and water into heavy water (100 mB, 25%). Fluid goes in at the top, comes out of the bottom, and passes down through the
 * stack.
 */
public class TritizerBlockEntity extends ReactorMachineBlockEntity implements NeutronTile {
    public static final int CAPACITY = 1000;

    /** What a neutron can turn a fluid into (TileEntityTritizer.Reactions). */
    public enum Reaction {
        TRITIUM(() -> ReactorFluids.DEUTERIUM.get(), () -> ReactorFluids.TRITIUM.get(), 75, 25),
        HEAVY_WATER(() -> Fluids.WATER, () -> ReactorFluids.HEAVY_WATER.get(), 25, 100);

        private final Supplier<Fluid> input;
        private final Supplier<Fluid> output;
        public final int chance;
        public final int amount;

        Reaction(Supplier<Fluid> input, Supplier<Fluid> output, int chance, int amount) {
            this.input = input;
            this.output = output;
            this.chance = chance;
            this.amount = amount;
        }

        public Fluid input() {
            return input.get();
        }

        public Fluid output() {
            return output.get();
        }

        @Nullable
        public static Reaction from(Fluid fluid) {
            for (Reaction r : values()) {
                if (r.input() == fluid) {
                    return r;
                }
            }
            return null;
        }
    }

    private final FluidTank input;
    private final FluidTank output;
    private final IFluidHandler fillIn;
    private final IFluidHandler drainOut;
    private final IFluidHandler anySide;

    public TritizerBlockEntity(BlockPos pos, BlockState state) {
        super(ReactorBlockEntities.TRITIZER.get(), pos, state, 0);
        input = addTank("Input", CAPACITY, s -> Reaction.from(s.getFluid()) != null);
        output = addTank("Output", CAPACITY, s -> false);
        fillIn = FluidAccess.fillOnly(input);
        drainOut = FluidAccess.drainOnly(output);
        anySide = new TankView(java.util.List.of(input), java.util.List.of(output));
    }

    @Override
    protected void tickServer() {
        passDown();
        if (thermalStep()) {
            updateTemperature();
        }
    }

    /** What is in the tank passes down into the tritizer below. */
    private void passDown() {
        if (level.getBlockEntity(worldPosition.below()) instanceof TritizerBlockEntity below) {
            if (!input.isEmpty()) {
                int amount = below.feedIn(fluidOf(input), input.getFluidAmount(), false);
                if (amount > 0) {
                    removeLiquid(input, amount);
                }
            }
            if (!output.isEmpty()) {
                int amount = below.feedIn(fluidOf(output), output.getFluidAmount(), true);
                if (amount > 0) {
                    removeLiquid(output, amount);
                }
            }
        }
    }

    private int feedIn(Fluid fluid, int available, boolean toOutput) {
        FluidTank tank = toOutput ? output : input;
        if (!toOutput && Reaction.from(fluid) == null) {
            return 0;
        }
        if (!tank.isEmpty() && !tank.getFluid().is(fluid)) {
            return 0;
        }
        int add = Math.min(tank.getCapacity() - tank.getFluidAmount(), available);
        if (add > 0) {
            addLiquid(tank, fluid, add);
        }
        return Math.max(0, add);
    }

    @Override
    public boolean onNeutron(NeutronEntity neutron, Level level, BlockPos pos) {
        if (input.isEmpty() || !neutron.neutronType().canIrradiateMaterials()) {
            return false;
        }
        Reaction reaction = Reaction.from(input.getFluid().getFluid());
        if (reaction != null && canMake(reaction) && Chance.of(level.random, reaction.chance)) {
            removeLiquid(input, reaction.amount);
            addLiquid(output, reaction.output(), reaction.amount);
            return true;
        }
        return false;
    }

    private boolean canMake(Reaction reaction) {
        return input.getFluidAmount() >= reaction.amount && canTakeIn(output, reaction.output(), reaction.amount);
    }

    public FluidTank inputTank() {
        return input;
    }

    public FluidTank outputTank() {
        return output;
    }

    /** In at the top, out of the bottom. */
    @Nullable
    @Override
    public IFluidHandler fluidHandler(@Nullable Direction side) {
        if (side == null) {
            return anySide;
        }
        return side == Direction.UP ? fillIn : side == Direction.DOWN ? drainOut : null;
    }
}
