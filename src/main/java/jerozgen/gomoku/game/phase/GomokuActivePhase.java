package jerozgen.gomoku.game.phase;

import jerozgen.gomoku.game.GomokuGame;
import jerozgen.gomoku.game.GomokuStatisticKeys;
import jerozgen.gomoku.game.GomokuTexts;
import net.minecraft.util.Util;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import xyz.nucleoid.plasmid.api.game.GameActivity;
import xyz.nucleoid.plasmid.api.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.api.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.api.game.player.JoinAcceptor;
import xyz.nucleoid.plasmid.api.game.player.JoinAcceptorResult;
import xyz.nucleoid.plasmid.api.game.rule.GameRuleType;
import xyz.nucleoid.plasmid.api.game.stats.StatisticKeys;
import xyz.nucleoid.plasmid.api.util.PlayerUtil;
import xyz.nucleoid.stimuli.event.EventResult;
import xyz.nucleoid.stimuli.event.block.BlockUseEvent;
import xyz.nucleoid.stimuli.event.entity.EntityUseEvent;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class GomokuActivePhase extends GomokuPhase {
    private static final long REQUEST_DURATION = 30 * 1000;

    private final Map<ServerPlayer, Block> playerToBlock = new HashMap<>();
    private final LinkedList<ServerPlayer> queue = new LinkedList<>();
    private final Map<ServerPlayer, Map<ServerPlayer, Long>> swapRequests = new HashMap<>();

    private int currentMovePlaces;
    private long moveStartTime;
    private int moveSeconds;

    private long startTime;

    public GomokuActivePhase(GomokuGame game) {
        super(game);
    }

    @Override
    protected void setupPhase(GameActivity activity) {
        activity.listen(GameActivityEvents.ENABLE, this::start);
        activity.listen(GameActivityEvents.TICK, this::tick);
        activity.listen(GamePlayerEvents.ACCEPT, this::acceptPlayer);
        activity.listen(GamePlayerEvents.LEAVE, this::removePlayer);
        activity.listen(BlockUseEvent.EVENT, this::onBlockUse);
        activity.listen(EntityUseEvent.EVENT, this::onEntityUse);

        activity.deny(GameRuleType.THROW_ITEMS);
        activity.deny(GameRuleType.MODIFY_INVENTORY);
        activity.deny(GameRuleType.SWAP_OFFHAND);
    }

    private void start() {
        var players = game.gameSpace().getPlayers().stream().collect(Collectors.toList());
        Collections.shuffle(players);
        var blocks = game.config().blocks().stream()
                .map(ArrayList::new)
                .flatMap(list -> {
                    Collections.shuffle(list);
                    return list.stream();
                }).iterator();
        playerToBlock.putAll(players.stream().collect(Collectors.toMap(Function.identity(), __ -> blocks.next())));
        queue.addAll(players);
        currentMovePlaces = game.config().firstMovePlaces();
        startTime = Util.getMillis();
        startTurn(currentPlayer());
    }

    private void tick() {
        for (var player : queue) {
            var hit = player.pick(8, 0, false);
            player.sendSystemMessage(game.board().raycastText(hit), true);
        }
        updateTimer(currentPlayer());
    }

    private JoinAcceptorResult acceptPlayer(JoinAcceptor offer) {
        return offer.teleport(game.world(), game.board().spawnPos())
                .thenRunForEach(player -> player.setGameMode(GameType.SPECTATOR));
    }

    private void removePlayer(ServerPlayer player) {
        if (player.equals(currentPlayer())) nextTurn();
        player.setGameMode(GameType.SPECTATOR);
        queue.remove(player);
        playerToBlock.remove(player);
        swapRequests.remove(player);
        swapRequests.forEach((__, requests) -> requests.remove(player));
        game.stat(statistics -> statistics.forPlayer(player).increment(StatisticKeys.GAMES_PLAYED, 1));
        game.stat(statistics -> statistics.forPlayer(player).increment(StatisticKeys.GAMES_LOST, 1));
        if (queue.size() == 1) win(currentPlayer());
    }

    private InteractionResult onBlockUse(ServerPlayer player, InteractionHand hand, BlockHitResult hit) {
        if (!player.equals(currentPlayer())) return InteractionResult.PASS;
        if (hand != InteractionHand.OFF_HAND) return InteractionResult.PASS;
        var pos = hit.getBlockPos();
        if (pos.getY() != 0 || game.board().isOut(pos.getX(), pos.getZ())) return InteractionResult.PASS;

        switch (game.board().block(pos.getX(), pos.getZ(), playerToBlock.get(player), game.world())) {
            case WIN -> win(player);
            case DRAW -> draw();
            case SUCCESS -> {
                game.world().playSound(null, pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5,
                        playerToBlock.get(player).defaultBlockState().getSoundType().getPlaceSound(),
                        SoundSource.BLOCKS, 1, 1);
                game.stat(statistics -> statistics.forPlayer(player).increment(GomokuStatisticKeys.BLOCKS_PLACED, 1));
                currentMovePlaces -= 1;
                if (currentMovePlaces <= 0) nextTurn();
            }
            case FAIL -> {
                return InteractionResult.PASS;
            }
        }
        return InteractionResult.SUCCESS;
    }

    private EventResult onEntityUse(ServerPlayer player, Entity entity, InteractionHand hand, EntityHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) return EventResult.PASS;
        if (!player.isShiftKeyDown()) return EventResult.PASS;
        if (hit == null) return EventResult.PASS;
        if (hit.getEntity().getType() != EntityTypes.PLAYER) return EventResult.PASS;
        if (!queue.contains(player)) return EventResult.PASS;
        var target = (ServerPlayer) hit.getEntity();
        if (!queue.contains(target)) return EventResult.PASS;

        // accept request
        var targetRequests = swapRequests.get(target);
        if (targetRequests != null && targetRequests.getOrDefault(player, Long.MAX_VALUE) < Util.getMillis() + REQUEST_DURATION) {
            targetRequests.remove(player);
            var playerIndex = queue.indexOf(player);
            var targetIndex = queue.indexOf(target);
            var playerBlock = playerToBlock.get(player);
            var targetBlock = playerToBlock.get(target);
            if (playerIndex == 0 || targetIndex == 0) endTurn(currentPlayer());
            queue.set(playerIndex, target);
            queue.set(targetIndex, player);
            playerToBlock.put(target, playerBlock);
            playerToBlock.put(player, targetBlock);
            game.gameSpace().getPlayers().sendMessage(GomokuTexts.swap(player, target));
            if (playerIndex == 0 || targetIndex == 0) startTurn(currentPlayer());
            return EventResult.ALLOW;
        }

        // send request
        var requests = swapRequests.computeIfAbsent(player, __ -> new HashMap<>());
        var previousTime = requests.put(target, Util.getMillis());
        if (previousTime == null || Util.getMillis() - previousTime > REQUEST_DURATION) {
            player.sendSystemMessage(GomokuTexts.swapRequest$toExecutor(target), false);
            target.sendSystemMessage(GomokuTexts.swapRequest$toTarget(player), false);
            return EventResult.ALLOW;
        }

        return EventResult.PASS;
    }

    private void win(ServerPlayer winner) {
        var players = game.gameSpace().getPlayers();
        players.sendMessage(GomokuTexts.win(winner));
        players.playSound(SoundEvents.PLAYER_LEVELUP);
        game.stat(statistics -> {
            for (var player : queue) {
                if (player.equals(winner)) continue;
                statistics.forPlayer(player).increment(StatisticKeys.GAMES_LOST, 1);
            }
            statistics.forPlayer(winner).increment(StatisticKeys.GAMES_WON, 1);
        });
        end();
    }

    private void draw() {
        var players = game.gameSpace().getPlayers();
        players.sendMessage(GomokuTexts.draw());
        players.playSound(SoundEvents.PLAYER_LEVELUP);
        game.stat(statistics -> {
            for (var player : queue) {
                statistics.forPlayer(player).increment(GomokuStatisticKeys.GAMES_WITH_A_DRAW, 1);
            }
        });
        end();
    }

    private void end() {
        if (currentPlayer() != null) endTurn(currentPlayer());
        var timeElapsed = (float) ((Util.getMillis() - startTime) / 100) / 10;
        game.stat(statistics -> {
            for (var player : queue) {
                statistics.forPlayer(player).increment(StatisticKeys.GAMES_PLAYED, 1);
            }
            statistics.global().set(GomokuStatisticKeys.TIME_ELAPSED, timeElapsed);
        });
        var endingPhase = new GomokuEndingPhase(game);
        game.gameSpace().setActivity(endingPhase::setup);
    }

    private ServerPlayer currentPlayer() {
        return queue.peek();
    }

    private void nextTurn() {
        endTurn(currentPlayer());
        queue.add(queue.remove());
        currentMovePlaces = game.config().eachMovePlaces();
        startTurn(currentPlayer());
    }

    private void endTurn(ServerPlayer player) {
        player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        var timeSpent = (float) ((Util.getMillis() - moveStartTime) / 100) / 10;
        game.stat(statistics -> statistics.forPlayer(player).increment(GomokuStatisticKeys.TIME_SPENT, timeSpent));
        clearTimer(player);
    }

    private void startTurn(ServerPlayer player) {
        moveStartTime = Util.getMillis();
        moveSeconds = game.config().moveDuration();
        PlayerUtil.playSoundToPlayer(player, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 1, 1);
        PlayerUtil.playSoundToPlayer(player, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 1, 1);
        player.setItemInHand(InteractionHand.OFF_HAND, playerToBlock.get(player).asItem().getDefaultInstance());
        updateTimer(player);
    }

    private void updateTimer(ServerPlayer player) {
        var time = Util.getMillis();
        var moveDuration = game.config().moveDuration() * 1000;
        var progress = 1f - (float) (time - moveStartTime) / moveDuration;
        var seconds = (int) Math.max(moveDuration - (time - moveStartTime), 0) / 1000;

        player.experienceProgress = progress;
        player.setExperienceLevels(seconds);

        if (moveSeconds != seconds) {
            if (moveSeconds == -1) {
                game.gameSpace().getPlayers().sendMessage(GomokuTexts.timeIsOver(player));
                removePlayer(player);
            } else {
                moveSeconds = seconds;
                if (seconds <= 10) {
                    PlayerUtil.playSoundToPlayer(player, SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, .6f, 1);
                    if (seconds == 0) moveSeconds = -1;
                }
            }
        }
    }

    private void clearTimer(ServerPlayer player) {
        player.experienceProgress = 0;
        player.setExperienceLevels(0);
    }
}
