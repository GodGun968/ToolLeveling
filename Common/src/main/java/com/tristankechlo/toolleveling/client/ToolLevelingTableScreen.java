package com.tristankechlo.toolleveling.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.tristankechlo.toolleveling.ToolLeveling;
import com.tristankechlo.toolleveling.config.ToolLevelingConfig;
import com.tristankechlo.toolleveling.menu.ToolLevelingTableMenu;
import com.tristankechlo.toolleveling.network.NetworkHelper;
import com.tristankechlo.toolleveling.util.ComponentUtil;
import com.tristankechlo.toolleveling.util.Util;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;
import java.util.stream.Collectors;

public class ToolLevelingTableScreen extends AbstractContainerScreen<ToolLevelingTableMenu> {

    private static final ResourceLocation GUI_TEXTURE = new ResourceLocation(ToolLeveling.MOD_ID, "textures/gui/tool_leveling_table.png");
    private static boolean shouldRenderPercentages = false;
    private static boolean shouldRenderHelp = false;
    private final InfoFieldRenderer percentagesField = new InfoFieldRenderer(0xD9080808, 0xFF8c045f, 0xFFD82FA0);
    private final InfoFieldRenderer successChanceField = new InfoFieldRenderer(0xD9080808, 0xFF3B51BF, 0xFF4F80FF);
    private final InfoFieldRenderer bonusItemField = new InfoFieldRenderer(0xD9080808, 0xFF007F0E, 0xFF00CC17);
    private final InfoFieldRenderer helpField = new InfoFieldRenderer(0xFF212121, 0xFF000000, 0xFF555555);
    private Button startButton;
    private float successChance = 0.0F;
    private byte ticksSinceUpdate = 0;
    private Component minChanceText;
    private Component maxChanceText;

    public ToolLevelingTableScreen(ToolLevelingTableMenu container, Inventory inv, Component name) {
        super(container, inv, name);
        this.imageWidth = 176;
        this.imageHeight = 200;
        this.inventoryLabelY += 35;
        this.titleLabelX -= 2;
    }

    @Override
    protected void init() {
        super.init();

        int buttonWidth = (this.imageWidth - 2) / 2; // width of the help and info button

        // button to toggle the help field
        this.addRenderableWidget(Button.builder(ComponentUtil.HELP_BUTTON_TEXT, (b) -> shouldRenderHelp = !shouldRenderHelp)
                .pos(leftPos, topPos - 17).size(buttonWidth, 16)
                .tooltip(ComponentUtil.HELP_BUTTON_TOOLTIP.get()).build());
        // button to toggle the percentages field
        this.addRenderableWidget(Button.builder(ComponentUtil.INFO_BUTTON_TEXT, (b) -> shouldRenderPercentages = !shouldRenderPercentages)
                .pos(leftPos + 2 + buttonWidth, topPos - 17).size(buttonWidth, 16)
                .tooltip(ComponentUtil.INFO_BUTTON_TOOLTIP.get()).build());
        // button to start upgrade process
        this.startButton = this.addRenderableWidget(new Button.Builder(ComponentUtil.START_BUTTON_TEXT, (b) -> {
            NetworkHelper.INSTANCE.startUpgradeProcess(this.getMenu().getPos());
        }).pos(this.leftPos + 64, this.topPos + 73).size(48, 16).tooltip(ComponentUtil.START_BUTTON_TOOLTIP.get()).build());

        // setup the info fields
        this.helpField.setLines(List.of(ComponentUtil.TITLE_HELP_FIELD, ComponentUtil.TEXT_HELP_FIELD), font, this.leftPos - 10);

        // setup the min and max success chance text
        this.minChanceText = ComponentUtil.makeChance(".success_chance.min", ToolLevelingConfig.minSuccessChance);
        this.maxChanceText = ComponentUtil.makeChance(".success_chance.max", ToolLevelingConfig.maxSuccessChance);
    }

    @Override
    protected void containerTick() {
        super.containerTick();

        this.successChance = Util.getSuccessChance(this.getMenu());
        this.startButton.active = Util.canUpgradeProcessBegin(this.getMenu());

        if (!shouldRenderPercentages) {
            return;
        }
        if (this.ticksSinceUpdate % 4 == 0) {
            this.ticksSinceUpdate = 0;

            // update percentages
            var percentages = this.getMenu().getPercentages();
            var components = percentages.stream().map(ComponentUtil::makePercentage).collect(Collectors.toList());
            components.add(0, ComponentUtil.TITLE_PERCENTAGES);
            this.percentagesField.setLines(components);

            // update success chance
            Component chanceText = ComponentUtil.makePercentage("screen.toolleveling.tool_leveling_table.success_chance", this.successChance);
            this.successChanceField.setLines(List.of(ComponentUtil.TITLE_SUCCESS_CHANCE, chanceText, minChanceText, maxChanceText));

            // update bonus items
            var iterations = this.getMenu().getCycles();
            Component iterationsText = ComponentUtil.makeBonus("screen.toolleveling.tool_leveling_table.bonuses.iterations", iterations);
            var strength = this.getMenu().getLevels();
            Component strengthText = ComponentUtil.makeBonus("screen.toolleveling.tool_leveling_table.bonuses.strength", strength);
            this.bonusItemField.setLines(List.of(ComponentUtil.TITLE_BONUSES, iterationsText, strengthText));
        }
        this.ticksSinceUpdate++;
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(poseStack); // render translucent grey background
        super.render(poseStack, mouseX, mouseY, partialTicks);
        this.renderTooltip(poseStack, mouseX, mouseY); // render tooltips if hovered

        if (shouldRenderPercentages) {
            int x = this.leftPos + this.imageWidth;
            int y = this.topPos;
            int fieldWidth = this.calcFieldWidth();
            if (this.getMenu().hasAnyBooks()) {
                this.percentagesField.render(poseStack, this.font, x, y, fieldWidth);
                y += this.percentagesField.calcHeight() + 1;
            }
            this.successChanceField.render(poseStack, this.font, x, y, fieldWidth);
            y += this.successChanceField.calcHeight() + 1;
            this.bonusItemField.render(poseStack, this.font, x, y, fieldWidth);
        }

        if (shouldRenderHelp) {
            int x = this.leftPos - this.helpField.calcWidth(this.font);
            int y = this.topPos;
            this.helpField.render(poseStack, this.font, x, y);
        }
    }

    private int calcFieldWidth() {
        int width1 = this.percentagesField.calcWidth(this.font);
        int width2 = this.successChanceField.calcWidth(this.font);
        int width3 = this.bonusItemField.calcWidth(this.font);
        int targetWidth = Math.max(width1, Math.max(width2, width3)); // get the widest field
        int widthFree = this.width - this.leftPos - this.imageWidth - 3; // get the free space on the right side
        targetWidth = Math.min(targetWidth, widthFree); // limit width to screen width
        return targetWidth;
    }

    @Override
    protected void renderBg(PoseStack poseStack, float partialTicks, int mX, int mY) {
        RenderSystem.setShaderTexture(0, GUI_TEXTURE);
        blit(poseStack, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        // render progress bar
        int targetX = this.leftPos + 16;
        int targetY = this.topPos + 95;
        int targetWidth = (int) (144 * this.successChance);
        int targetHeight = 5;
        blit(poseStack, targetX, targetY, 0, 251, targetWidth, targetHeight);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (var widget : this.children()) {
            if (!widget.isMouseOver(mouseX, mouseY)) {
                widget.setFocused(false);   // unfocus all widgets if clicked outside of them
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

}
