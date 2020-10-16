package com.tristankechlo.toolleveling.client.screen;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.tristankechlo.toolleveling.ToolLeveling;
import com.tristankechlo.toolleveling.config.ToolLevelingConfig;
import com.tristankechlo.toolleveling.container.ToolLevelingTableContainer;
import com.tristankechlo.toolleveling.network.PacketHandler;
import com.tristankechlo.toolleveling.network.packets.SyncToolLevelingEnchantment;
import com.tristankechlo.toolleveling.tileentity.ToolLevelingTableTileEntity;

import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.IReorderingProcessor;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ToolLevelingTableScreen extends ContainerScreen<ToolLevelingTableContainer> {

    private ResourceLocation GUI_TEXTURE = new ResourceLocation(ToolLeveling.MOD_ID, "textures/gui/tool_leveling_table.png");
    private ToolLevelingTableTileEntity entity;
    private BlockPos pos;
    private byte ticksSinceUpdate = 0;
    private byte currentPage = 1;
    private byte maxPages = 1;
    private boolean renderHelp = false;
    private byte helpDelayCounter = 0;
    private Button EnchantmentButton0;
    private Button EnchantmentButton1;
    private Button EnchantmentButton2;
    private Button EnchantmentButton3;
    private Button pageForward;
    private Button pageBackward;
    private List<ButtonData> buttonData = new ArrayList<>();

    public ToolLevelingTableScreen(ToolLevelingTableContainer container, PlayerInventory inv, ITextComponent name) {
        super(container, inv, name);
        this.pos = container.getEntityPos();
        this.entity = container.getEntity();
        //texture size
        this.xSize = 180;
        this.ySize = 216;
        //offset for inv title
        this.playerInventoryTitleX += 2;
        this.playerInventoryTitleY += 49;
    }
    
    @Override
    protected void init() {
    	super.init();
    	//create basic buttons
    	this.EnchantmentButton0 = new Button(this.guiLeft + 37, this.guiTop + 21, 130, 20, new StringTextComponent(""), (button) -> { handleButtonClick(0); });
    	this.EnchantmentButton1 = new Button(this.guiLeft + 37, this.guiTop + 45, 130, 20, new StringTextComponent(""), (button) -> { handleButtonClick(1); });
    	this.EnchantmentButton2 = new Button(this.guiLeft + 37, this.guiTop + 69, 130, 20, new StringTextComponent(""), (button) -> { handleButtonClick(2); });
    	this.EnchantmentButton3 = new Button(this.guiLeft + 37, this.guiTop + 93, 130, 20, new StringTextComponent(""), (button) -> { handleButtonClick(3); });
    	this.pageBackward = new Button(this.guiLeft + 37, this.guiTop + 93, 20, 20, new StringTextComponent("<"), (button) -> { pageBackward(); });
    	this.pageForward = new Button(this.guiLeft + 147, this.guiTop + 93, 20, 20, new StringTextComponent(">"), (button) -> { pageForward(); });

    	//hide all buttons before rendering
		this.EnchantmentButton0.visible = false;
		this.EnchantmentButton1.visible = false;
		this.EnchantmentButton2.visible = false;
		this.EnchantmentButton3.visible = false;
		this.pageForward.visible = false;
		this.pageBackward.visible = false;
		
		//add Buttons to screen
    	this.addButton(this.EnchantmentButton0);
    	this.addButton(this.EnchantmentButton1);
    	this.addButton(this.EnchantmentButton2);
    	this.addButton(this.EnchantmentButton3);
    	this.addButton(this.pageForward);
    	this.addButton(this.pageBackward);
    }    
    
    @Override
    public void tick() {
    	super.tick();
    	this.ticksSinceUpdate++;
    	if(this.ticksSinceUpdate % 2 == 0) {
    		this.ticksSinceUpdate = 0;
    		updateEnchantmentList();
    	}
    	if(this.buttonData.isEmpty()) {
    		this.currentPage = 1;
    		hideAllButtons();
    		
    		//show the help instructions after a small delay, to hide them when screen is opened and an item is already in the table
    		//was visible a few ticks until it got hidden
    		if(!this.renderHelp) {
    			this.helpDelayCounter++;
    			if(this.helpDelayCounter >= 15) {
    	    		this.renderHelp = true;
    	    		this.helpDelayCounter = 0;
    			}
    		}
    	} else {
    		this.renderHelp = false;
    		updateButtonData();
    	}
    }
    
    /**
     * get information about the current item in the table
     */
    private void updateEnchantmentList() {
    	ItemStack stack = this.entity.getStackToEnchant();
    	if(!stack.getItem().equals(Items.AIR)) {
    		//reset previous data
    		this.buttonData.clear();
    		
			Map<Enchantment, Integer> enchantments = EnchantmentHelper.getEnchantments(stack);
			for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
				
				List<? extends String> EnchantmentsBlacklist = ToolLevelingConfig.SERVER.EnchantmentsBlacklist.get();
				boolean leveled = true;
				byte extra = 0;
				
				//only list enchantments that are not on the blacklist
				if(EnchantmentsBlacklist.contains(entry.getKey().getRegistryName().toString())) {
					leveled = false;
					extra = 1;
				}
				
				//leveling these enchantments will do absolutely nothing
				if(entry.getKey().getMaxLevel() == 1 && extra == 0) {
					leveled = false;
					extra = 2;
				}
				
				//although the level is defined as an integer, the actual maximum is a short
				//a higher enchantment level than a short will result in a negative level
				if(entry.getValue() >= Short.MAX_VALUE && extra == 0) {
					leveled = false;
					extra = 3;
				}
				
				//check if the enchantment can still be leveled
				//some enchantments will break when leveled to high
				if(canStillBeLeveled(entry.getKey(), entry.getValue()) == false && extra == 0) {
					leveled = false;
					extra = 4;
				}
				
				this.buttonData.add(new ButtonData(entry.getKey().getRegistryName(), entry.getKey().getName(), entry.getValue(), leveled, extra));
			}
    	} else {
    		this.buttonData.clear();
    	}
    }
    
    /**
     * change the data stored in the buttons
     */
    private void updateButtonData(){
    	//get the number of required pages
		this.maxPages = (byte)Math.ceil(this.buttonData.size() / 3.0);
		
		for(int n = 0; n < 3; n++) {
			
			Button button = null;
			if(n == 0) {
				button = this.EnchantmentButton0;
			} else if(n == 1) {
				button = this.EnchantmentButton1;
			} else if (n == 2) {
				button = this.EnchantmentButton2;
			}
			
			//check if n-th button is needed on current page
			if(this.buttonData.size() >= n + 1 + ((this.currentPage - 1) * 3)) {
	    		button.visible = true;
	    		button.setMessage(getButtonText(this.getButtonByIndex(n)));
	    		boolean active = true;
	    		if(this.getButtonByIndex(n).UPGRADE_COST > this.entity.getPaymentAmount()){
	    			active = false;
	    		}
	    		if(!this.getButtonByIndex(n).CAN_BE_LEVELED || this.getButtonByIndex(n).EXTRA != (byte)0) {
	    			active = false;
	    		}
	    		button.active = active;
			} else {
	    		button.visible = false;
			}			
		}
		
		//either show 4th button or page selection
    	if(this.buttonData.size() == 4) {
    		this.EnchantmentButton3.visible = true;
    		this.EnchantmentButton3.setMessage(getButtonText(this.buttonData.get(3)));
    		boolean active = (this.getButtonByIndex(3).UPGRADE_COST <= this.entity.getPaymentAmount());
    		this.EnchantmentButton3.active = active;
    		this.pageForward.visible = false;
    		this.pageBackward.visible = false;
    	} else if(this.buttonData.size() > 4) {
    		this.EnchantmentButton3.visible = false;
    		this.pageForward.visible = true;
    		this.pageBackward.visible = true;
    	}
    	
    }
    
    /**
     * handle button click for page forward
     */
    private void pageForward() {
    	if(this.currentPage == this.maxPages) {
    		this.currentPage = 1;
    	} else {
    		this.currentPage++;
    	}
    	updateButtonData();
    }

    /**
     * handle button click for page backwards
     */
    private void pageBackward() {
    	if(this.currentPage == 1) {
    		this.currentPage = this.maxPages;
    	} else {
    		this.currentPage--;
    	}
    	updateButtonData();
    }
    
    /**
     * handle button click for enchantment buttons
     * @param id
     */
    private void handleButtonClick(int id) {

    	//next level must be below 32767
    	int nextLevel = this.getButtonByIndex(id).NEXT_LEVEL;
    	if(nextLevel >= 1 && nextLevel <= Short.MAX_VALUE) {

        	//send new data to server
    		PacketHandler.INSTANCE.sendToServer(
    				new SyncToolLevelingEnchantment(this.pos,
    						this.getButtonByIndex(id).ENCHANTMENT,
    						this.getButtonByIndex(id).NEXT_LEVEL
    				)
    		);
    	}
    }
    
    /**
     * hides all buttons when called
     */
    private void hideAllButtons() {
		for (Widget button : this.buttons) {
			button.visible = false;
		}
    }

    /**
     * get the n-th button of the current page, does not check if there is actually a button
     * @param id
     * @return
     */
    private ButtonData getButtonByIndex(int id) {
    	return this.buttonData.get(id + ((this.currentPage - 1) * 3));
    }
    
    /**
     * returns true if the enchantment should still be leveled, because some enchantments will break when leveled to high
     * @param enchantment
     * @param level
     * @return
     */
    private boolean canStillBeLeveled(Enchantment enchantment, int level) {
    	if(enchantment.equals(Enchantments.LUCK_OF_THE_SEA)) {
    		return level < 84;
    	}
    	if(enchantment.equals(Enchantments.THORNS)) {
    		return level < 7;
    	}
    	if(enchantment.equals(Enchantments.QUICK_CHARGE)) {
    		return level < 5;
    	}
    	if(enchantment.equals(Enchantments.LURE)) {
    		return level < 5;
    	}
    	return true;
    }
    
    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(matrixStack);
        super.render(matrixStack, mouseX, mouseY, partialTicks);
        this.renderHoveredTooltip(matrixStack, mouseX, mouseY);
        
        this.renderPersonalTooltips(matrixStack, mouseX, mouseY);

    	//draw string for page overview
        if(this.buttonData.size() > 4) {
        	ITextComponent text = new TranslationTextComponent("container.toolleveling.tool_leveling_table.page", this.currentPage, this.maxPages);
            IReorderingProcessor ireorderingprocessor = text.func_241878_f();
            float left = this.guiLeft + 102;
            float top = this.guiTop + 99;
            this.font.drawString(matrixStack, text.getString(), (float)(left - this.font.func_243245_a(ireorderingprocessor) / 2), top, 0);	
        }
        
        //draw instructions how to use the table in buttonview, when slot is empty
        if(this.buttonData.size() == 0 && this.renderHelp) {        	

        	ITextComponent textline = new TranslationTextComponent("container.toolleveling.tool_leveling_table.help_slot0");
            float left1 = this.guiLeft + 38;
            float top1 = this.guiTop + 23;
            this.font.func_243248_b(matrixStack, textline, left1, top1, 0);

        	for(int i = 0; i < 4; i++) {
            	ITextComponent textlinehelp = new TranslationTextComponent("container.toolleveling.tool_leveling_table.help_other_slot", new TranslationTextComponent(ToolLevelingTableContainer.PAYMENT_ITEM.getTranslationKey()));
                float l = this.guiLeft + 38;
                float t = this.guiTop + (50 + (i * 18));
                this.font.func_243248_b(matrixStack, textlinehelp, l, t, 0);
        	}
        }
    }
    
	@SuppressWarnings("deprecation")
	@Override
	protected void drawGuiContainerBackgroundLayer(MatrixStack matrixStack, float partialTicks, int x, int y) {
	      RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
	      this.minecraft.getTextureManager().bindTexture(GUI_TEXTURE);
	      int i = (this.width - this.xSize) / 2;
	      int j = (this.height - this.ySize) / 2;
	      blit(matrixStack, i, j, 0, 0, this.xSize, this.ySize, this.xSize, this.ySize);
	}
	
	/**
	 * @param matrixStack
	 * @param mouseX
	 * @param mouseY
	 */
	private void renderPersonalTooltips(MatrixStack matrixStack, int mouseX, int mouseY) {

		//tooltip for button 0-2
		for(int n = 0; n < 3; n++) {
			if ((this.buttonData.size() >= n + 1 + ((this.currentPage - 1) * 3)) && this.isPointInRegion(37, 21 + (n * 24), 130, 20, (double) mouseX, (double) mouseY)) {
				List<ITextComponent> tooltip = Lists.newArrayList();
				ButtonData data = this.getButtonByIndex(n);
				this.addToolTips(tooltip, data);
				this.func_243308_b(matrixStack, tooltip, mouseX, mouseY);
			}			
		}
		
		if(this.buttonData.size() > 4) {

    		//tooltip previous page
    		if (this.isPointInRegion(37, 93, 20, 20, (double) mouseX, (double) mouseY)) {
    			ITextComponent tooltip = (new TranslationTextComponent("spectatorMenu.previous_page")).mergeStyle(TextFormatting.AQUA);
    			this.renderTooltip(matrixStack, tooltip, mouseX, mouseY);
    		}

    		//tooltip next page
    		if (this.isPointInRegion(147, 93, 20, 20, (double) mouseX, (double) mouseY)) {
    			ITextComponent tooltip = (new TranslationTextComponent("spectatorMenu.next_page")).mergeStyle(TextFormatting.AQUA);
    			this.renderTooltip(matrixStack, tooltip, mouseX, mouseY);
    		}
    		
		} else if(this.buttonData.size() == 4 && this.isPointInRegion(37, 93, 130, 20, (double) mouseX, (double) mouseY)) {
			
        	//render tooltip button4
			List<ITextComponent> tooltip = Lists.newArrayList();
			ButtonData data = this.getButtonByIndex(3);
			this.addToolTips(tooltip, data);
			this.func_243308_b(matrixStack, tooltip, mouseX, mouseY);
		}
	}
    
    private ITextComponent getButtonText(ButtonData data) {
    	TextFormatting format = TextFormatting.RESET;
    	if(data.CAN_BE_LEVELED == false || data.EXTRA != (byte)0) {
    		format = TextFormatting.DARK_RED;
    	}
    	return new TranslationTextComponent(data.NAME).mergeStyle(format);
    }
    
    private void addToolTips(List<ITextComponent> tooltip, ButtonData data) {
		tooltip.add(new TranslationTextComponent(data.NAME).mergeStyle(TextFormatting.AQUA));
		if(data.CAN_BE_LEVELED && data.EXTRA == 0) {
			tooltip.add(new TranslationTextComponent("container.toolleveling.tool_leveling_table.current_level", (data.NEXT_LEVEL-1)).mergeStyle(TextFormatting.DARK_GRAY));
			tooltip.add(new TranslationTextComponent("container.toolleveling.tool_leveling_table.next_level", data.NEXT_LEVEL).mergeStyle(TextFormatting.DARK_GRAY));
			tooltip.add(new TranslationTextComponent("container.toolleveling.tool_leveling_table.cost", data.UPGRADE_COST).mergeStyle(TextFormatting.DARK_GRAY));
		} else {
			if(data.EXTRA == 1) {
				tooltip.add(new TranslationTextComponent("container.toolleveling.tool_leveling_table.error_blacklist").mergeStyle(TextFormatting.DARK_RED));
			} else if(data.EXTRA == 2) {
				tooltip.add(new TranslationTextComponent("container.toolleveling.tool_leveling_table.error_leveling_useless").mergeStyle(TextFormatting.DARK_RED));
			} else if(data.EXTRA == 3) {
				tooltip.add(new TranslationTextComponent("container.toolleveling.tool_leveling_table.error_level_at_max").mergeStyle(TextFormatting.DARK_RED));
			} else if(data.EXTRA == 4) {
				tooltip.add(new TranslationTextComponent("container.toolleveling.tool_leveling_table.error_item_will_break").mergeStyle(TextFormatting.DARK_RED));
			}
		}
    }
	
	//helper class
	private class ButtonData {
		
		public ResourceLocation ENCHANTMENT;
		public String NAME;
		public int NEXT_LEVEL;
		public int UPGRADE_COST;
		public boolean CAN_BE_LEVELED;
		public byte EXTRA;
		
		public ButtonData (ResourceLocation enchantment, String name, int level, boolean canBeLeveled, byte extra) {
			this.ENCHANTMENT = enchantment;
			this.NEXT_LEVEL = level + 1;
			this.NAME = name;
			this.CAN_BE_LEVELED = canBeLeveled;
			this.EXTRA = extra;
			double modifier = Math.max(0.0D, ToolLevelingConfig.SERVER.upgradeCostMultiplier.get());
			int minCost = Math.max(0, ToolLevelingConfig.SERVER.minUpgradeCost.get());
			this.UPGRADE_COST = (int) Math.max(minCost, ((4.5D * level) - 12) * modifier);
		}
	}

}