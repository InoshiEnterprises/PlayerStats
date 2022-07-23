package com.gmail.artemis.the.gr8.playerstats;

import com.gmail.artemis.the.gr8.playerstats.api.PlayerStats;
import com.gmail.artemis.the.gr8.playerstats.api.PlayerStatsAPI;
import com.gmail.artemis.the.gr8.playerstats.commands.ReloadCommand;
import com.gmail.artemis.the.gr8.playerstats.commands.ShareCommand;
import com.gmail.artemis.the.gr8.playerstats.commands.StatCommand;
import com.gmail.artemis.the.gr8.playerstats.commands.TabCompleter;
import com.gmail.artemis.the.gr8.playerstats.config.ConfigHandler;
import com.gmail.artemis.the.gr8.playerstats.listeners.JoinListener;
import com.gmail.artemis.the.gr8.playerstats.msg.OutputManager;
import com.gmail.artemis.the.gr8.playerstats.statistic.StatManager;
import com.gmail.artemis.the.gr8.playerstats.utils.OfflinePlayerHandler;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public class Main extends JavaPlugin {

    private static BukkitAudiences adventure;
    private static PlayerStats playerStatsAPI;

    public static @NotNull BukkitAudiences adventure() throws IllegalStateException {
        if (adventure == null) {
            throw new IllegalStateException("Tried to access Adventure without PlayerStats being enabled!");
        }
        return adventure;
    }

    public static @NotNull PlayerStats getPlayerStatsAPI() throws IllegalStateException {
        if (playerStatsAPI == null) {
            throw new IllegalStateException("PlayerStats does not seem to be loaded!");
        }
        return playerStatsAPI;
    }

    @Override
    public void onEnable() {
        //first get an instance of all the classes that need to be passed along to different classes
        ConfigHandler config = new ConfigHandler(this);
        OfflinePlayerHandler offlinePlayerHandler = new OfflinePlayerHandler();

        OutputManager outputManager = OutputManager.getInstance(config);
        StatManager statManager = StatManager.getInstance(outputManager, offlinePlayerHandler);
        ThreadManager threadManager = ThreadManager.getInstance(config, outputManager, statManager, offlinePlayerHandler);
        ShareManager shareManager = ShareManager.getInstance(config);

        //initialize the Adventure library and the API
        adventure = BukkitAudiences.create(this);
        playerStatsAPI = PlayerStatsAPI.load(this, threadManager, outputManager, statManager);

        //register all commands and the tabCompleter
        PluginCommand statcmd = this.getCommand("statistic");
        if (statcmd != null) {
            statcmd.setExecutor(new StatCommand(outputManager, threadManager, statManager));
            statcmd.setTabCompleter(new TabCompleter(offlinePlayerHandler));
        }
        PluginCommand reloadcmd = this.getCommand("statisticreload");
        if (reloadcmd != null) reloadcmd.setExecutor(new ReloadCommand(threadManager));
        PluginCommand sharecmd = this.getCommand("statisticshare");
        if (sharecmd != null) sharecmd.setExecutor(new ShareCommand(shareManager, outputManager));

        //register the listener
        Bukkit.getPluginManager().registerEvents(new JoinListener(threadManager), this);
        
        //finish up
        this.getLogger().info("Enabled PlayerStats!");
    }

    @Override
    public void onDisable() {
        if (adventure != null) {
            adventure.close();
            adventure = null;
        }
        this.getLogger().info("Disabled PlayerStats!");
    }
}