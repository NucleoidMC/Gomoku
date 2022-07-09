package jerozgen.gomoku.game;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import jerozgen.gomoku.game.board.BoardConfig;
import net.minecraft.block.Block;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.util.registry.Registry;
import xyz.nucleoid.plasmid.game.common.config.PlayerConfig;
import xyz.nucleoid.plasmid.game.stats.GameStatisticBundle;

import java.util.List;
import java.util.Optional;

import static net.minecraft.block.Blocks.*;

public record GomokuConfig(PlayerConfig playerConfig, BoardConfig boardConfig, List<List<Block>> blocks,
                           int firstMovePlaces, int eachMovePlaces, int moveDuration,
                           Optional<String> statisticBundleNamespace) {
    public static final Codec<GomokuConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            PlayerConfig.CODEC.fieldOf("players").forGetter(GomokuConfig::playerConfig),
            BoardConfig.CODEC.fieldOf("board").forGetter(GomokuConfig::boardConfig),
            Registry.BLOCK.getCodec().listOf().listOf().fieldOf("blocks").orElseGet(GomokuConfig::defaultBlocks).forGetter(GomokuConfig::blocks),
            Codecs.POSITIVE_INT.optionalFieldOf("first_move_places", 1).forGetter(GomokuConfig::firstMovePlaces),
            Codecs.POSITIVE_INT.optionalFieldOf("each_move_places", 1).forGetter(GomokuConfig::eachMovePlaces),
            Codecs.POSITIVE_INT.optionalFieldOf("move_duration", 90).forGetter(GomokuConfig::eachMovePlaces),
            GameStatisticBundle.NAMESPACE_CODEC.optionalFieldOf("statistic_bundle").forGetter(GomokuConfig::statisticBundleNamespace)
    ).apply(instance, GomokuConfig::new));

    public static List<List<Block>> defaultBlocks() {
        return List.of(
                List.of(WHITE_WOOL, ORANGE_WOOL, MAGENTA_WOOL, LIGHT_BLUE_WOOL, YELLOW_WOOL, LIME_WOOL, PINK_WOOL,
                        GRAY_WOOL, CYAN_WOOL, PURPLE_WOOL, BLUE_WOOL, BROWN_WOOL, GREEN_WOOL, RED_WOOL, BLACK_WOOL),
                List.of(WHITE_TERRACOTTA, ORANGE_TERRACOTTA, MAGENTA_TERRACOTTA, LIGHT_BLUE_TERRACOTTA,
                        YELLOW_TERRACOTTA, LIME_TERRACOTTA, PINK_TERRACOTTA, GRAY_TERRACOTTA, LIGHT_GRAY_TERRACOTTA,
                        CYAN_TERRACOTTA, PURPLE_TERRACOTTA, BLUE_TERRACOTTA, BROWN_TERRACOTTA, GREEN_TERRACOTTA,
                        RED_TERRACOTTA, BLACK_TERRACOTTA),
                List.of(BEDROCK));
    }
}
