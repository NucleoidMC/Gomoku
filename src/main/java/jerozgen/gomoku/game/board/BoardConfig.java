package jerozgen.gomoku.game.board;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.ExtraCodecs;

public record BoardConfig(int width, int height, WinConfig winConfig) {
    public static final Codec<BoardConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ExtraCodecs.POSITIVE_INT.fieldOf("width").forGetter(BoardConfig::width),
            ExtraCodecs.POSITIVE_INT.fieldOf("height").forGetter(BoardConfig::height),
            WinConfig.CODEC.fieldOf("win").forGetter(BoardConfig::winConfig)
    ).apply(instance, BoardConfig::new));

    public record WinConfig(int size, boolean exact) {
        public static final Codec<WinConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ExtraCodecs.POSITIVE_INT.fieldOf("size").forGetter(WinConfig::size),
                Codec.BOOL.optionalFieldOf("exact", false).forGetter(WinConfig::exact)
        ).apply(instance, WinConfig::new));
    }
}
