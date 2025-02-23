package com.artemis.the.gr8.playerstats.msg;

import com.artemis.the.gr8.playerstats.ShareManager;
import com.artemis.the.gr8.playerstats.config.ConfigHandler;
import com.artemis.the.gr8.playerstats.enums.StandardMessage;
import com.artemis.the.gr8.playerstats.statistic.request.RequestSettings;
import com.artemis.the.gr8.playerstats.msg.components.BukkitConsoleComponentFactory;
import com.artemis.the.gr8.playerstats.msg.components.PrideComponentFactory;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Statistic;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDate;
import java.time.Month;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;

import static com.artemis.the.gr8.playerstats.enums.StandardMessage.*;

/**
 * This class manages all PlayerStats output. It is the only
 * place where messages are sent. It gets its messages from a
 * {@link MessageBuilder} configured for either a Console or
 * for Players (mainly to deal with the lack of hover-text,
 * and for Bukkit consoles to make up for the lack of hex-colors).
 */
public final class OutputManager implements InternalFormatter {

    private static BukkitAudiences adventure;
    private static ConfigHandler config;
    private static ShareManager shareManager;
    private static MessageBuilder messageBuilder;
    private static MessageBuilder consoleMessageBuilder;

    private static EnumMap<StandardMessage, Function<MessageBuilder, TextComponent>> standardMessages;

    public OutputManager(BukkitAudiences adventure, ConfigHandler config, ShareManager shareManager) {
        OutputManager.adventure = adventure;
        OutputManager.config = config;
        OutputManager.shareManager = shareManager;

        getMessageBuilders();
        prepareFunctions();
    }

    public static void updateMessageBuilders() {
        getMessageBuilders();
    }

    @Override
    public TextComponent formatAndSavePlayerStat(@NotNull RequestSettings requestSettings, int playerStat) {
        BiFunction<Integer, CommandSender, TextComponent> playerStatFunction =
                getMessageBuilder(requestSettings).formattedPlayerStatFunction(playerStat, requestSettings);

        return processFunction(requestSettings.getCommandSender(), playerStatFunction);
    }

    @Override
    public TextComponent formatAndSaveServerStat(@NotNull RequestSettings requestSettings, long serverStat) {
        BiFunction<Integer, CommandSender, TextComponent> serverStatFunction =
                getMessageBuilder(requestSettings).formattedServerStatFunction(serverStat, requestSettings);

        return processFunction(requestSettings.getCommandSender(), serverStatFunction);
    }

    @Override
    public TextComponent formatAndSaveTopStat(@NotNull RequestSettings requestSettings, @NotNull LinkedHashMap<String, Integer> topStats) {
        BiFunction<Integer, CommandSender, TextComponent> topStatFunction =
                getMessageBuilder(requestSettings).formattedTopStatFunction(topStats, requestSettings);

        return processFunction(requestSettings.getCommandSender(), topStatFunction);
    }

    public void sendFeedbackMsg(@NotNull CommandSender sender, StandardMessage message) {
        if (message != null) {
            adventure.sender(sender).sendMessage(standardMessages.get(message)
                    .apply(getMessageBuilder(sender)));
        }
    }

    public void sendFeedbackMsgWaitAMoment(@NotNull CommandSender sender, boolean longWait) {
        adventure.sender(sender).sendMessage(getMessageBuilder(sender)
                .waitAMoment(longWait));
    }

    public void sendFeedbackMsgMissingSubStat(@NotNull CommandSender sender, Statistic.Type statType) {
        adventure.sender(sender).sendMessage(getMessageBuilder(sender)
                .missingSubStatName(statType));
    }

    public void sendFeedbackMsgWrongSubStat(@NotNull CommandSender sender, Statistic.Type statType, @Nullable String subStatName) {
        if (subStatName == null) {
            sendFeedbackMsgMissingSubStat(sender, statType);
        } else {
            adventure.sender(sender).sendMessage(getMessageBuilder(sender)
                    .wrongSubStatType(statType, subStatName));
        }
    }

    public void sendExamples(@NotNull CommandSender sender) {
        adventure.sender(sender).sendMessage(getMessageBuilder(sender)
                .usageExamples());
    }

    public void sendHelp(@NotNull CommandSender sender) {
        adventure.sender(sender).sendMessage(getMessageBuilder(sender)
                .helpMsg());
    }

    public void sendToAllPlayers(@NotNull TextComponent component) {
        adventure.players().sendMessage(component);
    }

    public void sendToCommandSender(@NotNull CommandSender sender, @NotNull TextComponent component) {
        adventure.sender(sender).sendMessage(component);
    }

    private TextComponent processFunction(CommandSender sender, @NotNull BiFunction<Integer, CommandSender, TextComponent> statResultFunction) {
        boolean saveOutput = !(sender instanceof ConsoleCommandSender) &&
                ShareManager.isEnabled() &&
                shareManager.senderHasPermission(sender);

        if (saveOutput) {
            int shareCode =
                    shareManager.saveStatResult(sender.getName(), statResultFunction.apply(null, sender));
            return statResultFunction.apply(shareCode, null);
        }
        else {
            return statResultFunction.apply(null, null);
        }
    }

    private MessageBuilder getMessageBuilder(CommandSender sender) {
        return sender instanceof ConsoleCommandSender ? consoleMessageBuilder : messageBuilder;
    }

    private MessageBuilder getMessageBuilder(RequestSettings requestSettings) {
        if (!requestSettings.isConsoleSender()) {
            return messageBuilder;
        } else {
            return consoleMessageBuilder;
        }
    }

    private static void getMessageBuilders() {
        messageBuilder = getClientMessageBuilder();
        consoleMessageBuilder = getConsoleMessageBuilder();
    }

    private static MessageBuilder getClientMessageBuilder() {
        if (useRainbowStyle()) {
            return MessageBuilder.fromComponentFactory(config, new PrideComponentFactory(config));
        }
        return MessageBuilder.defaultBuilder(config);
    }

    private static MessageBuilder getConsoleMessageBuilder() {
        MessageBuilder consoleBuilder;
        if (isBukkit()) {
            consoleBuilder = MessageBuilder.fromComponentFactory(config, new BukkitConsoleComponentFactory(config));
        } else {
            consoleBuilder = getClientMessageBuilder();
        }
        consoleBuilder.setConsoleBuilder(true);
        consoleBuilder.toggleHoverUse(false);
        return consoleBuilder;
    }

    private static boolean useRainbowStyle() {
        return config.useRainbowMode() || (config.useFestiveFormatting() && LocalDate.now().getMonth().equals(Month.JUNE));
    }

    private static boolean isBukkit() {
        return Bukkit.getName().equalsIgnoreCase("CraftBukkit");
    }

    private void prepareFunctions() {
        standardMessages = new EnumMap<>(StandardMessage.class);

        standardMessages.put(RELOADED_CONFIG, (MessageBuilder::reloadedConfig));
        standardMessages.put(STILL_RELOADING, (MessageBuilder::stillReloading));
        standardMessages.put(MISSING_STAT_NAME, (MessageBuilder::missingStatName));
        standardMessages.put(MISSING_PLAYER_NAME, (MessageBuilder::missingPlayerName));
        standardMessages.put(REQUEST_ALREADY_RUNNING, (MessageBuilder::requestAlreadyRunning));
        standardMessages.put(STILL_ON_SHARE_COOLDOWN, (MessageBuilder::stillOnShareCoolDown));
        standardMessages.put(RESULTS_ALREADY_SHARED, (MessageBuilder::resultsAlreadyShared));
        standardMessages.put(STAT_RESULTS_TOO_OLD, (MessageBuilder::statResultsTooOld));
        standardMessages.put(UNKNOWN_ERROR, (MessageBuilder::unknownError));
    }
}