package com.muhammaddaffa.playerprofiles.commands.subcommands;

import com.muhammaddaffa.playerprofiles.ConfigValue;
import com.muhammaddaffa.playerprofiles.PlayerProfiles;
import com.muhammaddaffa.playerprofiles.commands.abstraction.SubCommand;
import com.muhammaddaffa.playerprofiles.inventory.InventoryManager;
import com.muhammaddaffa.playerprofiles.manager.customgui.CustomGUI;
import com.muhammaddaffa.playerprofiles.manager.customgui.CustomGUIManager;
import com.muhammaddaffa.mdlib.utils.Common;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class OpenGUICommand extends SubCommand {

    @Override
    public @Nullable String getPermission() {
        return "playerprofiles.admin";
    }

    @Override
    public List<String> parseTabCompletion(PlayerProfiles plugin, CommandSender sender, String[] args) {
        if(args.length == 2){
            return null;
        }
        if(args.length == 3){
            return null;
        }
        if(args.length == 4){
            return plugin.getCustomGUIManager().getListName();
        }
        return new ArrayList<>();
    }

    @Override
    public void execute(PlayerProfiles plugin, CommandSender sender, String[] args) {
        // The full command is /playerprofiles opengui (player) (target) (gui-name) - args length = 4
        // So we want to tell the command sender if the the args doesn't enough
        if(args.length < 4){
            sender.sendMessage(Common.color("&cUsage: /playerprofiles opengui (player) (target) (gui-name)"));
            return;
        }
        // First get the player object from args[1]
        Player player = Bukkit.getPlayer(args[1]);
        // Check if the player is not valid
        if(player == null){
            // If the player isn't valid, we want to tell the command sender
            sender.sendMessage(Common.color(ConfigValue.INVALID_PLAYER
                    .replace("{prefix}", ConfigValue.PREFIX)
                    .replace("{player}", args[1])));
            // Stop the code here
            return;
        }
        // After the check above, we already guaranteed the Player would not be null
        // Now we want to get the Player object as target from args[2]
        Player target = Bukkit.getPlayer(args[2]);
        // Check if the target is not valid player
        if(target == null){
            // If the target is not valid player, we want to warn the command sender
            sender.sendMessage(Common.color(ConfigValue.INVALID_PLAYER
                    .replace("{prefix}", ConfigValue.PREFIX)
                    .replace("{player}", args[2])));
            return;
        }
        // After the check above, we already guaranteed the target would not be null
        // then we want to get the Custom GUI from args[3]
        // First, we get the CustomGUIManager
        CustomGUIManager customGUIManager = plugin.getCustomGUIManager();
        // And get the CustomGUI object
        CustomGUI customGUI = customGUIManager.getByName(args[3]);
        // And we check if the custom gui is null or doesn't exist
        if(customGUI == null){
            // We want to warn the sender if the custom gui isn't exist
            sender.sendMessage(Common.color(ConfigValue.INVALID_GUI_NAME
                    .replace("{prefix}", ConfigValue.PREFIX)
                    .replace("{gui}", args[3])));
            // And stop the code here
            return;
        }
        // After the check above we guaranteed the custom gui is not null
        // And now we're gonna try to open the custom gui
        // But first, we need to get the inventory manager
        InventoryManager inventoryManager = plugin.getInventoryManager();
        // Finally, we open the inventory for the player
        inventoryManager.openInventory(customGUI, player, target, true);
    }

}
