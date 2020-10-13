package com.tristankechlo.toolleveling.init;

import com.tristankechlo.toolleveling.ToolLeveling;
import com.tristankechlo.toolleveling.blocks.EnchantmentPillarBlock;
import com.tristankechlo.toolleveling.blocks.ToolLevelingTableBlock;

import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.Item.Properties;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;


public class ModBlocks {
	
	public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, ToolLeveling.MOD_ID);

	private static final Properties std_properties = new Item.Properties().group(ModItemGroups.General).maxStackSize(64);
	
	
	
	//BLOCKS
	public static final RegistryObject<Block> TOOL_LEVELING_TABLE = BLOCKS.register("tool_leveling_table", () -> new ToolLevelingTableBlock());
	public static final RegistryObject<Block> ENCHANTMENT_PILLAR = BLOCKS.register("enchantment_pillar", () -> new EnchantmentPillarBlock());
	
	
	
	//BLOCK - ITEMS
	public static final RegistryObject<Item> TOOL_LEVELING_TABLE_ITEM = ModItems.ITEMS.register("tool_leveling_table", () -> new BlockItem(TOOL_LEVELING_TABLE.get(), std_properties));
	public static final RegistryObject<Item> ENCHANTMENT_PILLAR_ITEM = ModItems.ITEMS.register("enchantment_pillar", () -> new BlockItem(ENCHANTMENT_PILLAR.get(), std_properties));
	 
	
}
