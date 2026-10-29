package com.muhammaddaffa.playerprofiles.listeners;

import com.muhammaddaffa.mdlib.utils.Common;
import com.muhammaddaffa.playerprofiles.ConfigValue;
import com.muhammaddaffa.playerprofiles.PlayerProfiles;
import com.muhammaddaffa.playerprofiles.hooks.combatlogx.HCombatLogX;
import com.muhammaddaffa.playerprofiles.hooks.deluxecombat.HDeluxeCombat;
import com.muhammaddaffa.playerprofiles.inventory.InventoryManager;
import com.muhammaddaffa.playerprofiles.manager.DependencyManager;
import com.muhammaddaffa.playerprofiles.manager.profile.ProfileManager;
import com.muhammaddaffa.playerprofiles.utils.Utils;
import com.muhammaddaffa.playerprofiles.worldguardwrapper.WorldGuardWrapper;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.HashMap;
import java.util.Map;

public class PlayerInteract implements Listener {

    private final PlayerProfiles plugin;
    public PlayerInteract(PlayerProfiles plugin){
        this.plugin = plugin;
    }

    // This is for the cooldown feature
    private final Map<Player, Long> mapCooldown = new HashMap<>();

    @EventHandler
    public void onPlayerRightClickEntity(PlayerInteractAtEntityEvent event){
        // First of all we want to return the code if the right clicked entity is not player
        if(!(event.getRightClicked() instanceof Player)) return;
        // Get the player object from this event
        Player player = event.getPlayer();
        // We check if the server is 1.9+, means they have off hand
        // This event will be fired twice for both main hand and off hand
        // So we want to stop the code if the interact hand is an off hand
        if(Utils.hasOffHand() && event.getHand() == EquipmentSlot.OFF_HAND){
            return;
        }
        // Check if the player must shift click to open the profile
        if (ConfigValue.MUST_SHIFT_CLICK && !event.getPlayer().isSneaking()) {
            return;
        }
        // Now, we get the right clicked entity as Player
        Player target = (Player) event.getRightClicked();
        // Now we check for the NPC option, should we open the profile of the NPC?
        if(ConfigValue.DISABLE_NPC_PROFILE && target.hasMetadata("NPC")){
            return;
        }
        // Profile locked feature, basically every player can lock their profile
        // so no one can open their profile. First, we need to get the ProfileManager
        ProfileManager profileManager = plugin.getProfileManager();
        if(profileManager.isProfileLocked(target)){
            // Send a message to the player
            player.sendMessage(Common.color(ConfigValue.LOCKED_PROFILE
                    .replace("{prefix}", ConfigValue.PREFIX)));
            // Stop the code
            return;
        }
        // First, check for the cooldown feature, and now we check if the player is in cooldown
        if(mapCooldown.containsKey(player)){
            // Get the time left
            long timeLeft = this.getCooldownTimeLeft(player);
            // We remove the player from the map if the time left is equals to below 0
            if(timeLeft <= 0){
                mapCooldown.remove(player);
            }
            // And if the time left is greater than 0, we stop the code here
            if(timeLeft > 0){
                // Send player message
                player.sendMessage(Common.color(ConfigValue.COOLDOWN_MESSAGE
                        .replace("{prefix}", ConfigValue.PREFIX)
                        .replace("{time}", timeLeft + "")));
                // Stop the code
                return;
            }
        }
        // Check for the disabled worlds, if the player is in disabled worlds
        // they can't open others profile
        if(ConfigValue.DISABLED_WORLDS.contains(player.getWorld().getName())){
            player.sendMessage(Common.color(ConfigValue.DISABLED_WORLD_MESSAGE
                    .replace("{prefix}", ConfigValue.PREFIX)));
            return;
        }
        // Check for the world guard regions, if the player or the target is inside the disabled
        // regions, the player cannot open the profile
        // First of all, check if the world guard is enabled
        if(DependencyManager.WORLD_GUARD){
            // First, we check for the player location, if the region is listed on the disabled regions
            // we stopped the code
            for(String region : WorldGuardWrapper.getInstance().getRegionFinder().getRegions(player.getLocation())){
                if(ConfigValue.DISABLED_REGIONS.contains(region)){
                    player.sendMessage(Common.color(ConfigValue.PLAYER_DISABLED_REGIONS
                            .replace("{prefix}", ConfigValue.PREFIX)));
                    return;
                }
            }
            // Now, we check for the target location
            for(String region : WorldGuardWrapper.getInstance().getRegionFinder().getRegions(target.getLocation())){
                if(ConfigValue.DISABLED_REGIONS.contains(region)){
                    player.sendMessage(Common.color(ConfigValue.TARGET_DISABLED_REGIONS
                            .replace("{prefix}", ConfigValue.PREFIX)));
                    return;
                }
            }
        }
        // Now we check for the combat part, this feature is to prevent player from
        // opening profiles while on combat (Require: CombatLogX or DeluxeCombat)
        if(ConfigValue.DISABLE_IN_COMBAT_ENABLED){
            // Check if CombatLogX is enabled
            if(DependencyManager.COMBAT_LOG_X){
                // Check if the player is in combat, if true we return the code
                if(HCombatLogX.isInCombat(player)){
                    player.sendMessage(Common.color(ConfigValue.DISABLE_IN_COMBAT_MESSAGE
                            .replace("{prefix}", ConfigValue.PREFIX)));
                    return;
                }
            }
            // Check if DeluxeCombat is enabled
            if(DependencyManager.DELUXE_COMBAT){
                // Check if the player is in combat, if true we return the code
                if(HDeluxeCombat.isInCombat(player)){
                    player.sendMessage(Common.color(ConfigValue.DISABLE_IN_COMBAT_MESSAGE
                            .replace("{prefix}", ConfigValue.PREFIX)));
                    return;
                }
            }
        }
        // Now this feature is a interact cooldown message, that means player cannot
        // spam open others profile, this option is recommended
        if(ConfigValue.COOLDOWN_ENABLED){
            mapCooldown.put(player, System.currentTimeMillis());
        }
        // After the checks, open the profile
        // for the player, first get the InventoryManager class
        InventoryManager inventoryManager = plugin.getInventoryManager();
        // Now we open the inventory for the player
        inventoryManager.openInventory(null, player, target);
        // Play sound to the player
        Utils.playSound(player, "onProfileOpen");
    }

    private Long getCooldownTimeLeft(Player player){
        return ((mapCooldown.get(player) / 1000) + ConfigValue.COOLDOWN_TIME) - (System.currentTimeMillis() / 1000);
    }

}
