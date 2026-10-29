package com.muhammaddaffa.playerprofiles.commands;

import com.muhammaddaffa.mdlib.utils.Common;
import com.muhammaddaffa.mdlib.utils.Logger;
import com.muhammaddaffa.playerprofiles.ConfigValue;
import com.muhammaddaffa.playerprofiles.PlayerProfiles;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class ToggleCommand implements CommandExecutor, TabCompleter {

    private static final String COMMAND_NAME = "toggleprofile";

    private final PlayerProfiles plugin;
    public ToggleCommand(PlayerProfiles plugin){
        this.plugin = plugin;
    }

    public void registerThisCommand(){
        plugin.getCommand(COMMAND_NAME).setExecutor(this);
        plugin.getCommand(COMMAND_NAME).setTabCompleter(this);
        // Get the file configuration of config.yml
        // Trying to register the aliases from the config
        try{
            final Field bukkitCommandMap = Bukkit.getServer().getClass().getDeclaredField("commandMap");

            bukkitCommandMap.setAccessible(true);
            ((CommandMap) bukkitCommandMap.get(Bukkit.getServer())).register(COMMAND_NAME, plugin.getCommand(COMMAND_NAME));

            bukkitCommandMap.setAccessible(false);
        } catch (Exception ex){
            Logger.info("&cFailed to register /toggleprofile command");
            ex.printStackTrace();
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (args.length != 0) {
            sender.sendMessage(Common.color("&cUsage: /toggleprofile"));
            return true;
        }
        // Check if the args length is 0 (/lockprofile)
        if(args.length == 0){
            // Permission: playerprofiles.lockprofile
            if(!(sender.hasPermission("playerprofiles.toggleprofile"))){
                sender.sendMessage(Common.color(ConfigValue.NO_PERMISSION
                        .replace("{prefix}", ConfigValue.PREFIX)
                        .replace("{permission}", "playerprofiles.toggleprofile")));
                return true;
            }
            // If the sender is not a player (the sender is console)
            if(!(sender instanceof Player)){
                // Send console a usage message and return the code
                sender.sendMessage(Common.color("&cOnly players can use /toggleprofile"));
                return true;
            }
            // After the check above, we can get the Player object from the sender
            Player player = (Player) sender;
            // Check if player is locked
            if (plugin.getProfileManager().getOrCreate(player).isLocked()) {
                plugin.getProfileManager().unlockProfile(player);
                player.sendMessage(Common.color(ConfigValue.UNLOCK_PROFILE
                        .replace("{prefix}", ConfigValue.PREFIX)));

            } else {
                plugin.getProfileManager().lockProfile(player);
                player.sendMessage(Common.color(ConfigValue.LOCK_PROFILE
                        .replace("{prefix}", ConfigValue.PREFIX)));

            }
        }
        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        return new ArrayList<>();
    }

}
