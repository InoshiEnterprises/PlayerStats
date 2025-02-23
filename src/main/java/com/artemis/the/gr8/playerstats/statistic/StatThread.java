package com.artemis.the.gr8.playerstats.statistic;

import com.artemis.the.gr8.playerstats.ThreadManager;
import com.artemis.the.gr8.playerstats.msg.OutputManager;
import com.artemis.the.gr8.playerstats.utils.MyLogger;
import com.artemis.the.gr8.playerstats.enums.StandardMessage;
import com.artemis.the.gr8.playerstats.enums.Target;
import com.artemis.the.gr8.playerstats.statistic.request.RequestSettings;
import com.artemis.the.gr8.playerstats.reload.ReloadThread;
import net.kyori.adventure.text.TextComponent;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * The Thread that is in charge of getting and calculating statistics.
 */
public final class StatThread extends Thread {

    private static OutputManager outputManager;
    private static StatCalculator statCalculator;

    private final ReloadThread reloadThread;
    private final RequestSettings requestSettings;

    public StatThread(OutputManager m, StatCalculator t, int ID, RequestSettings s, @Nullable ReloadThread r) {
        outputManager = m;
        statCalculator = t;

        reloadThread = r;
        requestSettings = s;

        this.setName("StatThread-" + requestSettings.getCommandSender().getName() + "-" + ID);
        MyLogger.logHighLevelMsg(this.getName() + " created!");
    }

    @Override
    public void run() throws IllegalStateException, NullPointerException {
        MyLogger.logHighLevelMsg(this.getName() + " started!");

        if (requestSettings == null) {
            throw new NullPointerException("No statistic requestSettings was found!");
        }
        if (reloadThread != null && reloadThread.isAlive()) {
            try {
                MyLogger.logLowLevelMsg(this.getName() + ": Waiting for " + reloadThread.getName() + " to finish up...");
                outputManager.sendFeedbackMsg(requestSettings.getCommandSender(), StandardMessage.STILL_RELOADING);
                reloadThread.join();

            } catch (InterruptedException e) {
                MyLogger.logException(e, "StatThread", "Trying to join " + reloadThread.getName());
                throw new RuntimeException(e);
            }
        }

        long lastCalc = ThreadManager.getLastRecordedCalcTime();
        if (lastCalc > 2000) {
            outputManager.sendFeedbackMsgWaitAMoment(requestSettings.getCommandSender(), lastCalc > 20000);
        }

        Target selection = requestSettings.getTarget();
        try {
            TextComponent statResult = switch (selection) {
                case PLAYER -> outputManager.formatAndSavePlayerStat(requestSettings, statCalculator.getPlayerStat(requestSettings));
                case TOP -> outputManager.formatAndSaveTopStat(requestSettings, statCalculator.getTopStats(requestSettings));
                case SERVER -> outputManager.formatAndSaveServerStat(requestSettings, statCalculator.getServerStat(requestSettings));
            };
            outputManager.sendToCommandSender(requestSettings.getCommandSender(), statResult);
        }
        catch (ConcurrentModificationException e) {
            if (!requestSettings.isConsoleSender()) {
                outputManager.sendFeedbackMsg(requestSettings.getCommandSender(), StandardMessage.UNKNOWN_ERROR);
            }
        }
    }
}