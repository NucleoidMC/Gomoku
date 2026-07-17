package jerozgen.gomoku.game;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import jerozgen.gomoku.game.board.BoardConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.block.Block;
import xyz.nucleoid.plasmid.api.game.common.config.WaitingLobbyConfig;
import xyz.nucleoid.plasmid.api.game.stats.GameStatisticBundle;

import java.util.List;
import java.util.Optional;

import static net.minecraft.world.level.block.Blocks.*;

public record GomokuConfig(
        WaitingLobbyConfig players,
        BoardConfig board,
        List<List<Block>> blocks,
        int firstMovePlaces,
        int eachMovePlaces,
        int moveDuration,
        Optional<String> statisticBundleNamespace
) {
    public static final MapCodec<GomokuConfig> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            WaitingLobbyConfig.CODEC.fieldOf("players").forGetter(GomokuConfig::players),
            BoardConfig.CODEC.fieldOf("board").forGetter(GomokuConfig::board),
            BuiltInRegistries.BLOCK.byNameCodec().listOf().listOf().fieldOf("blocks").orElseGet(GomokuConfig::defaultBlocks).forGetter(GomokuConfig::blocks),
            ExtraCodecs.POSITIVE_INT.optionalFieldOf("first_move_places", 1).forGetter(GomokuConfig::firstMovePlaces),
            ExtraCodecs.POSITIVE_INT.optionalFieldOf("each_move_places", 1).forGetter(GomokuConfig::eachMovePlaces),
            ExtraCodecs.POSITIVE_INT.optionalFieldOf("move_duration", 90).forGetter(GomokuConfig::eachMovePlaces),
            GameStatisticBundle.NAMESPACE_CODEC.optionalFieldOf("statistic_bundle").forGetter(GomokuConfig::statisticBundleNamespace)
    ).apply(instance, GomokuConfig::new));

    public static List<List<Block>> defaultBlocks() {
        return List.of(
                WOOL.asList(),
                DYED_TERRACOTTA.asList(),
                List.of(BEDROCK));
    }
}
