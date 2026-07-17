package jerozgen.gomoku;

import jerozgen.gomoku.game.GomokuConfig;
import jerozgen.gomoku.game.GomokuGame;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import xyz.nucleoid.plasmid.api.game.GameType;
import xyz.nucleoid.plasmid.api.game.GameTypes;

public class Gomoku implements ModInitializer {
    public static final String ID = "gomoku";

    @Override
    public void onInitialize() {
        GameTypes.register(Identifier.fromNamespaceAndPath(ID, "gomoku"), GomokuConfig.CODEC, GomokuGame::open);
    }
}
