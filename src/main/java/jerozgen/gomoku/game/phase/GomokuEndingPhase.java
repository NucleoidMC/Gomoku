package jerozgen.gomoku.game.phase;

import jerozgen.gomoku.game.GomokuGame;
import jerozgen.gomoku.game.GomokuTexts;
import net.minecraft.util.Util;
import xyz.nucleoid.plasmid.api.game.GameActivity;
import xyz.nucleoid.plasmid.api.game.GameCloseReason;
import xyz.nucleoid.plasmid.api.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.api.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.api.game.player.JoinOffer;
import xyz.nucleoid.plasmid.api.game.player.JoinOfferResult;

public class GomokuEndingPhase extends GomokuPhase {
    public static final long ENDING_DURATION = 10 * 1000;

    private long endTime;

    public GomokuEndingPhase(GomokuGame game) {
        super(game);
    }

    @Override
    protected void setupPhase(GameActivity activity) {
        activity.listen(GameActivityEvents.ENABLE, this::start);
        activity.listen(GameActivityEvents.TICK, this::tick);
        activity.listen(GamePlayerEvents.OFFER, this::offerPlayer);
    }

    private JoinOfferResult offerPlayer(JoinOffer offer) {
        return offer.reject(GomokuTexts.finishedError());
    }

    private void start() {
        endTime = Util.getMillis() + ENDING_DURATION;
    }

    private void tick() {
        if (Util.getMillis() >= endTime) {
            game.gameSpace().close(GameCloseReason.FINISHED);
        }
    }
}
