package me.antonvassilev.ships;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public final class Ships extends JavaPlugin {

    @Override
    public void onEnable() {
        // Plugin startup logic
        this.getLogger().info("Ships plugin has started, hello!");

        this.getServer().getPluginManager().registerEvents(new SignEventHandler(this), this);
        this.getCommand("info").setExecutor(new CommandInfo(this));
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        this.getLogger().info("Ships plugin shutting off, goodbye!");
    }
}
