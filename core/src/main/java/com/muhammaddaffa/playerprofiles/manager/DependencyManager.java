package com.muhammaddaffa.playerprofiles.manager;

import com.muhammaddaffa.mdlib.utils.Logger;
import nl.marido.deluxecombat.api.DeluxeCombatAPI;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

public class DependencyManager {

    public static boolean PLACEHOLDER_API;
    public static boolean COMBAT_LOG_X;

    public static boolean DELUXE_COMBAT;
    private static DeluxeCombatAPI deluxeAPI;

    public static boolean WORLD_GUARD;
    public static int WORLD_GUARD_VERSION;

    public static void checkDependency(){
        PluginManager pm = Bukkit.getPluginManager();

        PLACEHOLDER_API = pm.isPluginEnabled("PlaceholderAPI");
        COMBAT_LOG_X = pm.isPluginEnabled("CombatLogX");
        DELUXE_COMBAT = pm.isPluginEnabled("DeluxeCombat");
        WORLD_GUARD = pm.isPluginEnabled("WorldGuard");

        deluxeAPI = DELUXE_COMBAT ? new DeluxeCombatAPI() : null;
        WORLD_GUARD_VERSION = 0;
        if (WORLD_GUARD) {
            Plugin plugin = pm.getPlugin("WorldGuard");
            if (plugin.getDescription().getVersion().startsWith("6")) {
                WORLD_GUARD_VERSION = 6;
            } else if (plugin.getDescription().getVersion().startsWith("7")) {
                WORLD_GUARD_VERSION = 7;
            }
            if (WORLD_GUARD_VERSION != 0) {
                Logger.info("Found WorldGuard! Using WorldGuard API version " + WORLD_GUARD_VERSION);
            } else {
                WORLD_GUARD = false;
                Logger.severe("Unsupported WorldGuard API version; region integration is unavailable.");
            }
        }
    }

    public static DeluxeCombatAPI getDeluxeAPI() {
        return deluxeAPI;
    }
}
