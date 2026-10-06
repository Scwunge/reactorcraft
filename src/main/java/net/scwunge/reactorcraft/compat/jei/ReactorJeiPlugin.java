package net.scwunge.reactorcraft.compat.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.scwunge.reactorcraft.ReactorCraft;
import net.scwunge.reactorcraft.content.machine.ElectrolyzerBlockEntity;
import net.scwunge.reactorcraft.content.machine.FluidSynthesizerBlockEntity;
import net.scwunge.reactorcraft.content.machine.HeatExchangerBlockEntity;
import net.scwunge.reactorcraft.content.machine.IsotopeCentrifugeBlockEntity;
import net.scwunge.reactorcraft.content.machine.ItemMatch;
import net.scwunge.reactorcraft.content.machine.TritizerBlockEntity;
import net.scwunge.reactorcraft.content.machine.UraniumProcessorBlockEntity;
import net.scwunge.reactorcraft.registry.ReactorBlocks;
import net.scwunge.reactorcraft.registry.ReactorItems;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * JEI pages for the machines whose recipes are in their code, not in recipe files (the original's NEI handlers for the centrifuge, electrolyzer,
 * synthesizer and uranium processor, and the same for the tritizer and heat exchanger): what goes in, what comes out, and the temperature,
 * chance or speed that matters.
 */
@JeiPlugin
public class ReactorJeiPlugin implements IModPlugin {
    private static final ResourceLocation UID = ReactorCraft.id("jei");

    /** A fluid with an amount (mB); an empty fluid means nothing. */
    record Fluids(@Nullable Fluid fluid, int amount) {
        FluidStack stack() {
            return new FluidStack(fluid, amount);
        }
    }

    /** One page: items and fluids in, items and fluids out, and a line or two of notes. */
    record Page(List<List<ItemStack>> itemsIn, List<Fluids> fluidsIn, List<ItemStack> itemsOut, List<Fluids> fluidsOut, List<String> notes) {
    }

    private static final RecipeType<Page> CENTRIFUGE = RecipeType.create(ReactorCraft.MODID, "isotope_centrifuge", Page.class);
    private static final RecipeType<Page> ELECTROLYZER = RecipeType.create(ReactorCraft.MODID, "electrolyzer", Page.class);
    private static final RecipeType<Page> SYNTHESIZER = RecipeType.create(ReactorCraft.MODID, "fluid_synthesizer", Page.class);
    private static final RecipeType<Page> PROCESSOR = RecipeType.create(ReactorCraft.MODID, "uranium_processor", Page.class);
    private static final RecipeType<Page> TRITIZER = RecipeType.create(ReactorCraft.MODID, "tritizer", Page.class);
    private static final RecipeType<Page> EXCHANGER = RecipeType.create(ReactorCraft.MODID, "heat_exchanger", Page.class);

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper gui = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(
                new PageCategory(gui, CENTRIFUGE, "Isotope Centrifuge", ReactorBlocks.ISOTOPE_CENTRIFUGE.get()),
                new PageCategory(gui, ELECTROLYZER, "Electrolyzer", ReactorBlocks.ELECTROLYZER.get()),
                new PageCategory(gui, SYNTHESIZER, "Fluid Synthesizer", ReactorBlocks.FLUID_SYNTHESIZER.get()),
                new PageCategory(gui, PROCESSOR, "Uranium Processor", ReactorBlocks.URANIUM_PROCESSOR.get()),
                new PageCategory(gui, TRITIZER, "Tritizer", ReactorBlocks.TRITIZER.get()),
                new PageCategory(gui, EXCHANGER, "Heat Exchanger", ReactorBlocks.HEAT_EXCHANGER.get()));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(ReactorBlocks.ISOTOPE_CENTRIFUGE.get()), CENTRIFUGE);
        registration.addRecipeCatalyst(new ItemStack(ReactorBlocks.ELECTROLYZER.get()), ELECTROLYZER);
        registration.addRecipeCatalyst(new ItemStack(ReactorBlocks.FLUID_SYNTHESIZER.get()), SYNTHESIZER);
        registration.addRecipeCatalyst(new ItemStack(ReactorBlocks.URANIUM_PROCESSOR.get()), PROCESSOR);
        registration.addRecipeCatalyst(new ItemStack(ReactorBlocks.TRITIZER.get()), TRITIZER);
        registration.addRecipeCatalyst(new ItemStack(ReactorBlocks.HEAT_EXCHANGER.get()), EXCHANGER);
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(CENTRIFUGE, List.of(new Page(List.of(), List.of(new Fluids(net.scwunge.reactorcraft.registry.ReactorFluids.URANIUM_HEXAFLUORIDE.get(),
                IsotopeCentrifugeBlockEntity.UF6_PER_DUST)), List.of(new ItemStack(ReactorItems.ENRICHED_URANIUM_DUST.get()),
                new ItemStack(ReactorItems.DEPLETED_URANIUM_DUST.get())), List.of(), List.of(
                "Enriched dust " + IsotopeCentrifugeBlockEntity.FUEL_CHANCE + "% of the time, else depleted",
                "Needs " + IsotopeCentrifugeBlockEntity.MINSPEED + " rad/s; faster is quicker"))));

        List<Page> electrolysis = new ArrayList<>();
        for (ElectrolyzerBlockEntity.Electrolysis e : ElectrolyzerBlockEntity.Electrolysis.values()) {
            List<List<ItemStack>> items = e.inputItem() == null ? List.of() : List.of(e.inputItem().examples());
            List<Fluids> in = e.inputFluid() == null ? List.of() : List.of(new Fluids(e.inputFluid(), e.requiredFluidAmount));
            electrolysis.add(new Page(items, in, List.of(), List.of(new Fluids(e.upperFluid(), e.upperAmount), new Fluids(e.lowerFluid(), e.lowerAmount)),
                    List.of(e.requiredTemperature > 0 ? "Must be above " + e.requiredTemperature + " C" : "Powered by a Van de Graaff discharge",
                            "Needs a discharge of " + ElectrolyzerBlockEntity.MIN_DISCHARGE + " or more")));
        }
        registration.addRecipes(ELECTROLYZER, electrolysis);

        List<Page> synthesis = new ArrayList<>();
        for (FluidSynthesizerBlockEntity.Synthesis s : FluidSynthesizerBlockEntity.Synthesis.values()) {
            List<List<ItemStack>> items = new ArrayList<>();
            addExamples(items, s.firstItem());
            addExamples(items, s.secondItem());
            synthesis.add(new Page(items, List.of(new Fluids(s.inputFluid(), s.fluidConsumed)), List.of(), List.of(new Fluids(s.outputFluid(), s.fluidProduced)),
                    List.of("Needs " + s.minTemperature + " C or more", "Takes " + s.baseDuration + " ticks at that temperature, less when hotter")));
        }
        registration.addRecipes(SYNTHESIZER, synthesis);

        List<Page> processing = new ArrayList<>();
        for (UraniumProcessorBlockEntity.Process p : UraniumProcessorBlockEntity.Process.values()) {
            List<Fluids> out = new ArrayList<>();
            if (p.hasIntermediate()) {
                out.add(new Fluids(p.intermediateFluid(), p.intermediateFluidProduced));
            }
            out.add(new Fluids(p.outputFluid(), p.outputFluidProduced));
            processing.add(new Page(List.of(p.itemMatch().examples()), List.of(new Fluids(p.inputFluid(), p.inputFluidConsumed)), List.of(), out,
                    List.of(p.hasIntermediate() ? "First " + p.intermediateFluid().getFluidType().getDescription().getString() + ", using " + p.intermediateFluidConsumed
                            + " mB of it for the second stage" : "One stage", "Second stage takes " + p.outputTime + " ticks")));
        }
        registration.addRecipes(PROCESSOR, processing);

        List<Page> tritiation = new ArrayList<>();
        for (TritizerBlockEntity.Reaction r : TritizerBlockEntity.Reaction.values()) {
            tritiation.add(new Page(List.of(), List.of(new Fluids(r.input(), r.amount)), List.of(), List.of(new Fluids(r.output(), r.amount)),
                    List.of(r.chance + "% for each neutron that hits it")));
        }
        registration.addRecipes(TRITIZER, tritiation);

        List<Page> exchange = new ArrayList<>();
        for (HeatExchangerBlockEntity.Exchange e : HeatExchangerBlockEntity.Exchange.values()) {
            exchange.add(new Page(List.of(), List.of(new Fluids(e.hotFluid(), HeatExchangerBlockEntity.COOL_AMOUNT)), List.of(),
                    List.of(new Fluids(e.coldFluid(), HeatExchangerBlockEntity.COOL_AMOUNT * e.expansionRatio)),
                    List.of("Heat goes to the steam boilers beside it", "Works up to " + e.maxTemperature + " C", "Needs shaft power below")));
        }
        registration.addRecipes(EXCHANGER, exchange);
    }

    private static void addExamples(List<List<ItemStack>> items, @Nullable ItemMatch match) {
        if (match != null) {
            items.add(match.examples());
        }
    }

    /** The one layout every page uses: inputs on the left, an arrow, outputs on the right, notes underneath. */
    private static final class PageCategory implements IRecipeCategory<Page> {
        private static final int WIDTH = 150;
        private static final int HEIGHT = 70;

        private final RecipeType<Page> type;
        private final Component title;
        private final IDrawable background;
        private final IDrawable icon;

        PageCategory(IGuiHelper gui, RecipeType<Page> type, String title, ItemLike icon) {
            this.type = type;
            this.title = Component.literal(title);
            this.background = gui.createBlankDrawable(WIDTH, HEIGHT);
            this.icon = gui.createDrawableItemStack(new ItemStack(icon));
        }

        @Override
        public RecipeType<Page> getRecipeType() {
            return type;
        }

        @Override
        public Component getTitle() {
            return title;
        }

        @Override
        public IDrawable getBackground() {
            return background;
        }

        @Override
        public IDrawable getIcon() {
            return icon;
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, Page page, IFocusGroup focuses) {
            int x = 2;
            for (List<ItemStack> stacks : page.itemsIn()) {
                builder.addSlot(RecipeIngredientRole.INPUT, x, 2).addItemStacks(stacks);
                x += 20;
            }
            for (Fluids fluid : page.fluidsIn()) {
                if (fluid.fluid() != null) {
                    builder.addSlot(RecipeIngredientRole.INPUT, x, 2).addIngredient(NeoForgeTypes.FLUID_STACK, fluid.stack()).setFluidRenderer(fluid.amount(), false, 16, 16);
                    x += 20;
                }
            }
            int out = WIDTH - 22;
            for (ItemStack stack : page.itemsOut()) {
                builder.addSlot(RecipeIngredientRole.OUTPUT, out, 2).addItemStack(stack);
                out -= 20;
            }
            for (Fluids fluid : page.fluidsOut()) {
                if (fluid.fluid() != null) {
                    builder.addSlot(RecipeIngredientRole.OUTPUT, out, 2).addIngredient(NeoForgeTypes.FLUID_STACK, fluid.stack()).setFluidRenderer(fluid.amount(), false, 16, 16);
                    out -= 20;
                }
            }
        }

        @Override
        public void draw(Page page, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
            Minecraft mc = Minecraft.getInstance();
            graphics.drawString(mc.font, "->", WIDTH / 2 - 4, 7, 0x808080, false);
            int y = 24;
            for (String note : page.notes()) {
                graphics.drawString(mc.font, note, 2, y, 0x404040, false);
                y += 11;
            }
        }
    }
}
