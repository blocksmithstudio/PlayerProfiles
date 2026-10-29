package com.muhammaddaffa.playerprofiles.inventory.items;

import com.muhammaddaffa.playerprofiles.PlayerProfiles;
import com.muhammaddaffa.mdlib.utils.Logger;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;

public class ItemsLoader {

    private final List<GUIItem> mainMenuItems = new ArrayList<>();

    public ItemsLoader(PlayerProfiles plugin){
    }

    public void loadItems(){
        loadMainMenuItems();
    }

    public void reloadItems(){
        mainMenuItems.clear();

        loadMainMenuItems();
    }

    private void loadMainMenuItems(){
        FileConfiguration config = PlayerProfiles.GUI_DEFAULT.getConfig();
        try {
            validateMenu(config);
            mainMenuItems.addAll(readItems(config));
        } catch (IllegalArgumentException ex) {
            Logger.severe("Cannot load gui.yml: " + ex.getMessage());
        }
    }

    public static List<GUIItem> readItems(FileConfiguration config) {
        List<GUIItem> items = new ArrayList<>();
        // Return if there is no items
        if(!config.isConfigurationSection("items")) return items;
        // Loop through all items
        for(String configKey : config.getConfigurationSection("items").getKeys(false)){
            String path = "items." + configKey;
            if (!config.isConfigurationSection(path)) {
                throw new IllegalArgumentException("Each menu item must be a configuration section.");
            }
            String type = config.getString(path + ".type");
            String material = config.getString(path + ".material");
            int amount = config.getInt(path + ".amount");
            String name = config.getString(path + ".name");
            List<Integer> slots = config.getIntegerList(path + ".slots");
            boolean glowing = config.getBoolean(path + ".glowing");
            boolean hideAttributes = config.getBoolean(path + ".hideAttributes");
            boolean usePermission = config.getBoolean(path + ".usePermission");
            String permission = config.getString(path + ".permission");
            List<String> lore = config.getStringList(path + ".lore");
            List<String> leftCommands = config.getStringList(path + ".leftClickCommands");
            List<String> rightCommands = config.getStringList(path + ".rightClickCommands");
            int customModelData = config.getInt(path + ".customModelData");
            String itemModel = config.getString(path + ".itemModel");
            boolean onlyOwner = config.getBoolean(path + ".onlyOwner");
            boolean onlyVisitor = config.getBoolean(path + ".onlyVisitor");
            int priority = config.getInt(path + ".priority", 0);

            // Null-safe for item model
            if (itemModel == null || itemModel.isEmpty()) {
                itemModel = "";
            }
            items.add(new GUIItem(type, material, amount, name, slots, glowing, hideAttributes, usePermission,
                    permission, lore, leftCommands, rightCommands, customModelData, itemModel, onlyOwner, onlyVisitor, priority));
        }
        return items;
    }

    public static void validateMenu(FileConfiguration config) {
        int size = config.getInt("size");
        if (!config.isInt("size") || size < 9 || size > 54 || size % 9 != 0) {
            throw new IllegalArgumentException("Menu size must be a multiple of 9 between 9 and 54.");
        }
        if (config.contains("items") && !config.isConfigurationSection("items")) {
            throw new IllegalArgumentException("Menu items must be a configuration section.");
        }
        if (!config.isConfigurationSection("items")) return;
        for (String key : config.getConfigurationSection("items").getKeys(false)) {
            String path = "items." + key;
            if (!config.isConfigurationSection(path) || !config.isList(path + ".slots")) {
                throw new IllegalArgumentException("Each menu item needs a list of integer slots.");
            }
            for (Object slot : config.getList(path + ".slots")) {
                if (!(slot instanceof Integer value) || value < 0 || value >= size) {
                    throw new IllegalArgumentException("Menu slots must be integers within the inventory size.");
                }
            }
        }
    }

    public List<GUIItem> getMainMenuItems(){
        return mainMenuItems;
    }

}
