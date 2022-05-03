package me.antonvassilev.ships;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;

public final class Ships extends JavaPlugin {

    // Keep a map of vessels keyed by their name.
    // The name will be present in the vessels' blocks metadata.
    private static final HashMap<String, Vessel> vessels = new HashMap<>();

    @Override
    public void onEnable() {
        // Plugin startup logic
        this.getLogger().info("Ships plugin has started, hello!");

        this.getServer()
                .getPluginManager()
                .registerEvents(new SignWriteEventHandler(this, vessels), this);
        this.getServer()
                .getPluginManager()
                .registerEvents(new SignClickEventHandler(this, vessels), this);
        this.getCommand("info").setExecutor(new CommandInfo(this));
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        this.getLogger().info("Ships plugin shutting off, goodbye!");
    }
}
