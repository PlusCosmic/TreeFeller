package dev.pluscosmic.treefeller.events;

import dev.pluscosmic.treefeller.TreeFeller;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.*;

@EventBusSubscriber(modid = TreeFeller.MOD_ID)
public class EventHandler {

    private static boolean felling = false;

    private static final List<Vec3i> relativeBlockPositions = Arrays.asList(
            new Vec3i(0, 1, 0),
            new Vec3i(0, 0, 1),
            new Vec3i(0, 0, -1),
            new Vec3i(1, 0, 0),
            new Vec3i(-1, 0, 0),
            new Vec3i(1, 0, 1),
            new Vec3i(1, 0, -1),
            new Vec3i(-1, 0, 1),
            new Vec3i(-1, 0, -1),
            new Vec3i(1, 1, 0),
            new Vec3i(-1, 1, 0),
            new Vec3i(0, 1, 1),
            new Vec3i(0, 1, -1),
            new Vec3i(1, 1, 1),
            new Vec3i(1, 1, -1),
            new Vec3i(-1, 1, 1),
            new Vec3i(-1, 1, -1)
    );


    private static final TagKey<Block> LOGS_TAG = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("minecraft", "logs"));
    private static final TagKey<Block> LEAVES_TAG = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("minecraft", "leaves"));
    private static final TagKey<Block> DIRT_TAG = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("minecraft", "dirt"));

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {

        if(felling) {
            return;
        }

        if(event.isCanceled()) {
            return;
        }

        if(event.getLevel().isClientSide()) {
            return;
        }

        if(event.getPlayer().isCrouching()) {
            return;
        }

        BlockState brokenBlock = event.getState();

        boolean isLog = brokenBlock.is(LOGS_TAG);

        BlockState blockBelow = event.getLevel().getBlockState(event.getPos().below());
        boolean isDirtBelow = blockBelow.is(DIRT_TAG);
        boolean isHoldingAxe = event.getPlayer().getMainHandItem().isCorrectToolForDrops(brokenBlock);
        boolean isInSurvival = !event.getPlayer().isCreative() && !event.getPlayer().isSpectator();

        if(isLog && isDirtBelow && isHoldingAxe && isInSurvival) {
            felling = true;
            fellTree(event.getPos(), (ServerPlayer) event.getPlayer(), event.getLevel());
            felling = false;
        }
    }

    private static void fellTree(BlockPos blockPos, ServerPlayer player, LevelAccessor level) {
        Queue<BlockPos> treeParts = new ArrayDeque<>();
        treeParts.add(blockPos);

        while(!treeParts.isEmpty()) {
            BlockPos treePart = treeParts.poll();
            if(isTreePart(treePart, level)) {

                player.gameMode.destroyBlock(treePart);
                for (Vec3i relativeBlockPosition : relativeBlockPositions) {
                    BlockPos relativeBlock = treePart.offset(relativeBlockPosition);
                    if(isTreePart(relativeBlock, level)) {
                        treeParts.add(relativeBlock);
                    }
                }
            }
        }
    }

    private static boolean isTreePart(BlockPos pos, LevelAccessor level) {
        return level.getBlockState(pos).is(LOGS_TAG) || level.getBlockState(pos).is(LEAVES_TAG);
    }
}
