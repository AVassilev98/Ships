package me.antonvassilev.ships;

import org.bukkit.plugin.java.JavaPlugin;

public final class Ships extends JavaPlugin {

    @Override
    public void onEnable() {
        // Plugin startup logic
        System.out.println("Ships plugin has started, hello!");
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        System.out.println("Ships plugin shutting off, goodbye!");
    }
}
