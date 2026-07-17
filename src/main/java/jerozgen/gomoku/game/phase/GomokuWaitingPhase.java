package jerozgen.gomoku.game.phase;

import jerozgen.gomoku.game.GomokuGame;
import jerozgen.gomoku.game.GomokuTexts;
import net.minecraft.world.level.GameType;
import xyz.nucleoid.plasmid.api.game.GameActivity;
import xyz.nucleoid.plasmid.api.game.GameResult;
import xyz.nucleoid.plasmid.api.game.common.GameWaitingLobby;
import xyz.nucleoid.plasmid.api.game.common.config.PlayerLimiterConfig;
import xyz.nucleoid.plasmid.api.game.common.config.WaitingLobbyConfig;
import xyz.nucleoid.plasmid.api.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.api.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.api.game.player.JoinAcceptor;
import xyz.nucleoid.plasmid.api.game.player.JoinAcceptorResult;

import java.util.List;
import java.util.OptionalInt;

public class GomokuWaitingPhase extends GomokuPhase {
    public GomokuWaitingPhase(GomokuGame game) {
        super(game);
    }

    @Override
    protected void setupPhase(GameActivity activity) {
        var blockCount = game.config().blocks().stream().mapToInt(List::size).sum();
        var playerConfig = game.config().players();
        var newPlayerConfig = new WaitingLobbyConfig(
                new PlayerLimiterConfig(
                        OptionalInt.of(Math.min(blockCount, playerConfig.playerConfig().maxPlayers().orElse(Integer.MAX_VALUE))),
                        playerConfig.playerConfig().allowSpectators()
                ),
                Math.min(playerConfig.minPlayers(), blockCount),
                Math.min(playerConfig.thresholdPlayers(), blockCount),
                playerConfig.countdown());
        GameWaitingLobby.addTo(activity, newPlayerConfig);

        activity.listen(GamePlayerEvents.ACCEPT, this::acceptPlayer);
        activity.listen(GameActivityEvents.REQUEST_START, this::requestStart);
    }

    private JoinAcceptorResult acceptPlayer(JoinAcceptor offer) {
        return offer.teleport(game.world(), game.board().spawnPos())
                .thenRunForEach(player -> {
                    player.sendSystemMessage(GomokuTexts.description(game), false);
                    player.setGameMode(GameType.ADVENTURE);
                });
    }

    private GameResult requestStart() {
        var activePhase = new GomokuActivePhase(game);
        game.gameSpace().setActivity(activePhase::setup);
        return GameResult.ok();
    }
}
