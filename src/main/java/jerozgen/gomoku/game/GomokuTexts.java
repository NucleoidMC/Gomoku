package jerozgen.gomoku.game;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import xyz.nucleoid.plasmid.api.game.config.GameConfig;

public final class GomokuTexts {
    public static MutableComponent description(GomokuGame game) {
        var chainSize = game.config().board().winConfig().size();
        var gameName = GameConfig.name(game.gameSpace().getMetadata().sourceConfig());
        return Component.empty().withStyle(ChatFormatting.GRAY)
                .append("\n").append(gameName.copy()
                        .withStyle(ChatFormatting.BOLD)
                        .withStyle(ChatFormatting.WHITE))
                .append("\n").append(Component.translatable("text.gomoku.desc" + (chainSize == 5 ? "" : ".custom_chain"),
                        chainSize))
                .append("\n")
                .append("\n").append(Component.translatable("text.gomoku.desc.controls.swap",
                        Component.translatable("text.gomoku.combined_keybind",
                                        Component.keybind("key.sneak"),
                                        Component.keybind("key.use"))
                                .withStyle(ChatFormatting.WHITE)))
                .append("\n");

    }

    public static MutableComponent swapRequest$toExecutor(ServerPlayer target) {
        return Component.translatable("text.gomoku.swap.request.to_executor",
                target.getDisplayName())
                .withStyle(ChatFormatting.GRAY);
    }

    public static MutableComponent swapRequest$toTarget(ServerPlayer executor) {
        return Component.translatable("text.gomoku.swap.request.to_target",
                        executor.getDisplayName(),
                        Component.translatable("text.gomoku.combined_keybind",
                                        Component.keybind("key.sneak"),
                                        Component.keybind("key.use"))
                                .withStyle(ChatFormatting.WHITE))
                .withStyle(ChatFormatting.GRAY);
    }

    public static MutableComponent timeIsOver(ServerPlayer player) {
        return Component.translatable("text.gomoku.time_is_over",
                player.getDisplayName())
                .withStyle(ChatFormatting.RED);
    }

    public static MutableComponent swap(ServerPlayer player, ServerPlayer target) {
        return Component.translatable("text.gomoku.swap.success",
                player.getDisplayName(),
                target.getDisplayName())
                .withStyle(ChatFormatting.GRAY);
    }

    public static MutableComponent win(ServerPlayer player) {
        return Component.translatable("text.gomoku.end.winner",
                player.getDisplayName())
                .withStyle(ChatFormatting.GOLD);
    }

    public static MutableComponent draw() {
        return Component.translatable("text.gomoku.end.no_winner")
                .withStyle(ChatFormatting.GOLD);
    }

    public static MutableComponent finishedError() {
        return Component.translatable("text.gomoku.error.finished");
    }

    private GomokuTexts() {}
}
