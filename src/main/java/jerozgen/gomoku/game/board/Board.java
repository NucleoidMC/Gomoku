package jerozgen.gomoku.game.board;

import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.map_templates.MapTemplate;

import java.util.List;
import java.util.function.IntUnaryOperator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;


public class Board {
    private static final Block DEFAULT_BLOCK = Blocks.LODESTONE;
    private static final Block EDGE_BLOCK = Blocks.OAK_LOG;

    private final BoardConfig config;
    private final Block[][] board;
    private final int winSize;
    private final Vec3 spawnPos;

    private int emptySpace;

    public Board(BoardConfig config) {
        this.config = config;
        this.board = new Block[config.width()][config.height()];
        for (int x = 0; x < config.width(); x++) {
            for (int z = 0; z < config.height(); z++) {
                board[z][x] = DEFAULT_BLOCK;
            }
        }
        this.emptySpace = config.width() * config.width();
        this.winSize = config.winConfig().size();
        this.spawnPos = new Vec3(config.width() / 2.0, 1, config.height() / 2.0);
    }

    public boolean isOut(int x, int z) {
        return x < 0 || z < 0 || x >= config.width() || z >= config.height();
    }

    @Nullable
    public Block block(int x, int z) {
        if (isOut(x, z)) return null;
        return board[z][x];
    }

    public BlockChangeResult block(int x, int z, Block block, LevelAccessor world) {
        if (isOut(x, z)) return BlockChangeResult.FAIL;
        if (!DEFAULT_BLOCK.equals(block(x, z))) return BlockChangeResult.FAIL;

        board[z][x] = block;
        emptySpace -= 1;
        world.setBlock(new BlockPos(x, 0, z), block.defaultBlockState(), Block.UPDATE_CLIENTS);

        if (emptySpace <= 0) return BlockChangeResult.DRAW;

        if (checkWin(block, d -> x + d, d -> z    ) ||
            checkWin(block, d -> x    , d -> z + d) ||
            checkWin(block, d -> x + d, d -> z + d) ||
            checkWin(block, d -> x - d, d -> z + d)) return BlockChangeResult.WIN;

        return BlockChangeResult.SUCCESS;
    }

    private boolean checkWin(Block block, IntUnaryOperator x, IntUnaryOperator z) {
        var max = 0;
        var current = 0;
        for (int d = -winSize; d <= winSize; d++) {
            if (!block.equals(block(x.applyAsInt(d), z.applyAsInt(d)))) current = 0;
            else max = Math.max(max, ++current);
        }
        return !config.winConfig().exact() && max >= winSize || max == winSize;
    }

    public Vec3 spawnPos() {
        return spawnPos;
    }

    public Component raycastText(HitResult hit) {
        if (hit.getType() != HitResult.Type.BLOCK) return Component.empty();

        var pos = ((BlockHitResult) hit).getBlockPos();
        if (pos.getY() != 0) return Component.empty();

        var block = block(pos.getX(), pos.getZ());
        if (block == null) return Component.empty();
        if (block.equals(DEFAULT_BLOCK)) return Component.empty();

        return block.getName();
    }

    public MapTemplate template() {
        MapTemplate template = MapTemplate.createEmpty();
        var width = config.width();
        var height = config.height();

        for (BlockPos pos : BlockPos.betweenClosed(0, 0, 0, width - 1, 0, height - 1)) {
            template.setBlockState(pos, DEFAULT_BLOCK.defaultBlockState());
        }

        var edge = EDGE_BLOCK.defaultBlockState();
        for (var iterable : List.of(
                BlockPos.betweenClosed(-1, 0, -1, width - 1, 0, -1),
                BlockPos.betweenClosed(0, 0, height, width, 0, height))) {
            iterable.forEach(pos -> template.setBlockState(pos, edge.setValue(RotatedPillarBlock.AXIS, Direction.Axis.X)));
        }
        for (var iterable : List.of(
                BlockPos.betweenClosed(-1, 0, 0, -1, 0, height),
                BlockPos.betweenClosed(width, 0, -1, width, 0, height - 1))) {
            iterable.forEach(pos -> template.setBlockState(pos, edge.setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z)));
        }

        return template;
    }

    public enum BlockChangeResult {
        WIN,
        DRAW,
        SUCCESS,
        FAIL
    }
}
