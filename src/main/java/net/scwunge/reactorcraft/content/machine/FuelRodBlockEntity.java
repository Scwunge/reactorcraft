package net.scwunge.reactorcraft.content.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.scwunge.reactorcraft.content.entity.NeutronEntity;
import net.scwunge.reactorcraft.content.waste.WasteManager;
import net.scwunge.reactorcraft.core.Chance;
import net.scwunge.reactorcraft.core.ReactorFuel;
import net.scwunge.reactorcraft.core.ReactorType;
import net.scwunge.reactorcraft.registry.ReactorBlockEntities;
import net.scwunge.reactorcraft.registry.ReactorItems;
import org.jetbrains.annotations.Nullable;

/**
 * Fuel Core (TileEntityFuelRod): holds uranium or plutonium pellets. A neutron hitting it (and not soaked up by waste
 * poisoning) may cause a fission: the pellet wears down a stage, waste may build up, three more neutrons fly out and the core
 * heats up. Stack cores into a column to feed fuel down through them.
 */
public class FuelRodBlockEntity extends NuclearCoreBlockEntity {
    public FuelRodBlockEntity(BlockPos pos, BlockState state) {
        super(ReactorBlockEntities.FUEL_ROD.get(), pos, state);
    }

    @Nullable
    private ReactorFuel fuel() {
        return ReactorFuel.of(stack(3));
    }

    @Override
    public boolean isFissile() {
        return fuel() != null;
    }

    @Override
    public boolean onNeutron(NeutronEntity neutron, Level level, BlockPos pos) {
        super.onNeutron(neutron, level, pos);
        if (!level.isClientSide && neutron.neutronType().canTriggerFission(level.random)
                && Chance.of(level.random, neutron.neutronSpeed().interactionMultiplier())) {
            if (isPoisoned()) {
                return true;
            }
            ReactorFuel fuel = fuel();
            if (fuel != null && Chance.of(level.random, fuel.fissionChance + fuel.voidCoefficient * (temperature - 100))) {
                if (Chance.of(level.random, fuel.consumeChance)) {
                    ItemStack old = stack(3);
                    ItemStack product = fuel.fissionProduct(old);
                    setStack(3, product);
                    if (!product.isEmpty() && !product.is(old.getItem())) {
                        pushSpentFuel(3);
                    }
                    if (Chance.of(level.random, fuel.wasteChance)) {
                        addWaste();
                    }
                }
                spawnNeutronBurst();
                temperature += fuel.temperatureStep;
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        if (!stack(slot).isEmpty() || slot >= FUEL_SLOTS) {
            return false;
        }
        return ReactorFuel.of(stack) != null || stack.is(ReactorItems.DEPLETED.get());
    }

    @Override
    protected boolean canRemove(ItemStack stack) {
        return WasteManager.isWaste(stack) || stack.is(ReactorItems.DEPLETED.get());
    }

    @Override
    public ReactorType getReactorType() {
        return ReactorType.FISSION;
    }
}
