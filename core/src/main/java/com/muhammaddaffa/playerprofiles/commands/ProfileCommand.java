package com.muhammaddaffa.playerprofiles.commands;

import com.muhammaddaffa.mdlib.utils.Common;
import com.muhammaddaffa.mdlib.utils.Logger;
import com.muhammaddaffa.playerprofiles.ConfigValue;
import com.muhammaddaffa.playerprofiles.PlayerProfiles;
import com.muhammaddaffa.playerprofiles.inventory.InventoryManager;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class ProfileCommand implements CommandExecutor, TabCompleter {

    private static final String COMMAND_NAME = "profile";

    private final PlayerProfiles plugin;
    public ProfileCommand(PlayerProfiles plugin){
        this.plugin = plugin;
    }

    public void registerThisCommand(){
        plugin.getCommand(COMMAND_NAME).setExecutor(this);
        plugin.getCommand(COMMAND_NAME).setTabCompleter(this);
        // Get the file configuration of config.yml
        FileConfiguration config = PlayerProfiles.CONFIG_DEFAULT.getConfig();
        // Get the aliases from the config
        List<String> aliases = config.getStringList("commandAliases.profile");
        // Add all aliases to the command
        plugin.getCommand(COMMAND_NAME).getAliases().addAll(aliases);
        // Trying to register the aliases from the config
        try{
            final Field bukkitCommandMap = Bukkit.getServer().getClass().getDeclaredField("commandMap");

            bukkitCommandMap.setAccessible(true);
            ((CommandMap) bukkitCommandMap.get(Bukkit.getServer())).register(COMMAND_NAME, plugin.getCommand(COMMAND_NAME));

            bukkitCommandMap.setAccessible(false);
        } catch (Exception ex){
            Logger.info("&cFailed to register /profile command");
            ex.printStackTrace();
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        // If the command sender is not player, return the code
        if(!(sender instanceof Player)){
            Logger.info("&cOnly players can execute /profile command");
            return true;
        }
        // Get the player object from the sender
        Player player = (Player) sender;
        // Get the InventoryManager
        InventoryManager inventoryManager = plugin.getInventoryManager();
        // Check if args length is 0 (/profile)
        if (args.length == 0) {
            // Permission: playerprofiles.profile
            if(!(player.hasPermission("playerprofiles.profile"))){
                player.sendMessage(Common.color(ConfigValue.NO_PERMISSION
                        .replace("{prefix}", ConfigValue.PREFIX)
                        .replace("{permission}", "playerprofiles.profile")));
                return true;
            }
            // If the player has permission, open the profile
            inventoryManager.openInventory(null, player, player);
            // Stop the code here
            return true;
        }
        // Check if args length is 1 (/profile (player))
        if (args.length == 1) {
            // Permission: playerprofiles.profile.others
            if(!(player.hasPermission("playerprofiles.profile.others"))){
                player.sendMessage(Common.color(ConfigValue.NO_PERMISSION
                        .replace("{prefix}", ConfigValue.PREFIX)
                        .replace("{permission}", "playerprofiles.profile.others")));
                return true;
            }
            // If the player has permission, open the profile of the target
            // First, get the target as Player object
            Player target = Bukkit.getPlayer(args[0]);
            // If the target is invalid, return the code
            if(target == null){
                sender.sendMessage(Common.color(ConfigValue.INVALID_PLAYER
                        .replace("{prefix}", ConfigValue.PREFIX)
                        .replace("{player}", args[0])));
                return true;
            }
            // If the target is valid, we open the target's profile for player
            inventoryManager.openInventory(null, player, target);
        }
        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if(args.length == 1 && sender.hasPermission("playerprofiles.profile.others")){
            return null;
        }
        return new ArrayList<>();
    }
}
