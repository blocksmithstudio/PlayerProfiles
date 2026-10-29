package com.muhammaddaffa.playerprofiles.hooks.combatlogx;

import com.github.sirblobman.combatlogx.api.ICombatLogX;
import com.github.sirblobman.combatlogx.api.manager.ICombatManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class HCombatLogX {

    public static boolean isInCombat(Player player){
        var plugin = Bukkit.getPluginManager().getPlugin("CombatLogX");
        if (!(plugin instanceof ICombatLogX combatLogX) || !plugin.isEnabled()) return false;
        ICombatManager combatManager = combatLogX.getCombatManager();
        return combatManager != null && combatManager.isInCombat(player);
    }

}
