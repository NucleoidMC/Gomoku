package jerozgen.gomoku.game;

import jerozgen.gomoku.Gomoku;
import net.minecraft.resources.Identifier;
import xyz.nucleoid.plasmid.api.game.stats.StatisticKey;

public final class GomokuStatisticKeys {
    // global
    public static final StatisticKey<Float> TIME_ELAPSED = StatisticKey.floatKey(id("time_elapsed"));

    // per-player
    public static final StatisticKey<Float> TIME_SPENT = StatisticKey.floatKey(id("time_spent"));
    public static final StatisticKey<Integer> GAMES_WITH_A_DRAW =  StatisticKey.intKey(id("games_with_a_draw"));
    public static final StatisticKey<Integer> BLOCKS_PLACED = StatisticKey.intKey(id("blocks_placed"));

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(Gomoku.ID, path);
    }

    private GomokuStatisticKeys() {}
}
