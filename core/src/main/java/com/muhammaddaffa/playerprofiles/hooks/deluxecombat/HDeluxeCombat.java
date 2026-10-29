package com.muhammaddaffa.playerprofiles.hooks.deluxecombat;

import com.muhammaddaffa.playerprofiles.manager.DependencyManager;
import org.bukkit.entity.Player;

public class HDeluxeCombat {

    public static boolean isInCombat(Player player){
        return org.bukkit.Bukkit.getPluginManager().isPluginEnabled("DeluxeCombat") &&
                DependencyManager.getDeluxeAPI() != null && DependencyManager.getDeluxeAPI().isInCombat(player);
    }

}
