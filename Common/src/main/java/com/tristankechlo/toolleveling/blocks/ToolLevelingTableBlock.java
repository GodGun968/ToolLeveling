package com.tristankechlo.toolleveling.blocks;

import com.tristankechlo.toolleveling.blockentity.ToolLevelingTableBlockEntity;
import com.tristankechlo.toolleveling.network.ServerNetworkHelper;
import com.tristankechlo.toolleveling.platform.PlatformHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ToolLevelingTableBlock extends BaseEntityBlock {

    private static final VoxelShape SHAPE = Shapes.or(
            box(1, 0, 1, 15, 1, 15),
            box(1.5, 1, 1.5, 14.5, 2, 14.5),
            box(3, 2, 3, 13, 9, 13),
            box(1.5, 9, 1.5, 14.5, 13, 14.5)
    );

    public ToolLevelingTableBlock() {
        super(Block.Properties.of().mapColor(MapColor.COLOR_GRAY).strength(4.5f, 1000.0f)
                .sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()
                .pushReaction(PushReaction.IGNORE));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide()) {
            ServerNetworkHelper.INSTANCE.openMenu(state, level, pos, player, hand, hit);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return PlatformHelper.INSTANCE.newBlockEntity(pos, state);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockentity = level.getBlockEntity(pos);
            if (blockentity instanceof ToolLevelingTableBlockEntity) {
                Containers.dropContents(level, pos, (ToolLevelingTableBlockEntity) blockentity);
                level.updateNeighbourForOutputSignal(pos, this);
            }
            super.onRemove(state, level, pos, newState, isMoving); // remove block entity
        }
    }
}
