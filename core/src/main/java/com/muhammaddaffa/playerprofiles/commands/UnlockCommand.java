package com.muhammaddaffa.playerprofiles.commands;

import com.muhammaddaffa.mdlib.utils.Common;
import com.muhammaddaffa.mdlib.utils.Logger;
import com.muhammaddaffa.playerprofiles.ConfigValue;
import com.muhammaddaffa.playerprofiles.PlayerProfiles;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class UnlockCommand implements CommandExecutor, TabCompleter {

    private static final String COMMAND_NAME = "unlockprofile";

    private final PlayerProfiles plugin;
    public UnlockCommand(PlayerProfiles plugin){
        this.plugin = plugin;
    }

    public void registerThisCommand(){
        plugin.getCommand(COMMAND_NAME).setExecutor(this);
        plugin.getCommand(COMMAND_NAME).setTabCompleter(this);
        // Get the file configuration of config.yml
        FileConfiguration config = PlayerProfiles.CONFIG_DEFAULT.getConfig();
        // Get the aliases from the config
        List<String> aliases = config.getStringList("commandAliases.unlockProfile");
        // Add all aliases to the command
        plugin.getCommand(COMMAND_NAME).getAliases().addAll(aliases);
        // Trying to register the aliases from the config
        try{
            final Field bukkitCommandMap = Bukkit.getServer().getClass().getDeclaredField("commandMap");

            bukkitCommandMap.setAccessible(true);
            ((CommandMap) bukkitCommandMap.get(Bukkit.getServer())).register(COMMAND_NAME, plugin.getCommand(COMMAND_NAME));

            bukkitCommandMap.setAccessible(false);
        } catch (Exception ex){
            Logger.info("&cFailed to register /unlockprofile command");
            ex.printStackTrace();
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        // Check if the args length is 0 (/unlockprofile)
        if(args.length == 0){
            // Permission: playerprofiles.unlockprofile
            if(!(sender.hasPermission("playerprofiles.unlockprofile"))){
                sender.sendMessage(Common.color(ConfigValue.NO_PERMISSION
                        .replace("{prefix}", ConfigValue.PREFIX)
                        .replace("{permission}", "playerprofiles.unlockprofile")));
                return true;
            }
            // If the sender is not a player (the sender is console)
            if(!(sender instanceof Player)){
                // Send console a usage message and return the code
                sender.sendMessage(Common.color("&cUsage: /unlockprofile (player)"));
                return true;
            }
            // After the check above, we can get the Player object from the sender
            Player player = (Player) sender;
            plugin.getProfileManager().unlockProfile(player);
            // Send a lock profile message to player
            player.sendMessage(Common.color(ConfigValue.UNLOCK_PROFILE
                    .replace("{prefix}", ConfigValue.PREFIX)));

        }
        // Check if args length is 1 (/unlockprofile (player))
        if(args.length == 1){
            // Permission: playerprofiles.unlockprofile.others
            if(!(sender.hasPermission("playerprofiles.unlockprofile.others"))){
                sender.sendMessage(Common.color(ConfigValue.NO_PERMISSION
                        .replace("{prefix}", ConfigValue.PREFIX)
                        .replace("{permission}", "playerprofiles.unlockprofile.others")));
                return true;
            }
            // Get the player object from args[0]
            Player player = Bukkit.getPlayer(args[0]);
            // Check if the player is not valid or online
            if(player == null){
                // Send an invalid player message
                sender.sendMessage(Common.color(ConfigValue.INVALID_PLAYER
                        .replace("{prefix}", ConfigValue.PREFIX)
                        .replace("{player}", args[0])));
                // Stop the code
                return true;
            }
            // The player is valid and everything's going well
            // Now, we finally locked the player's profile
            plugin.getProfileManager().unlockProfile(player);
            // And send the message to the command sender
            sender.sendMessage(Common.color(ConfigValue.UNLOCK_PROFILE_OTHERS
                    .replace("{prefix}", ConfigValue.PREFIX)
                    .replace("{player}", player.getName())));
        }
        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if(args.length == 1 && sender.hasPermission("playerprofiles.unlockprofile.others")){
            return null;
        }
        return new ArrayList<>();
    }
}
