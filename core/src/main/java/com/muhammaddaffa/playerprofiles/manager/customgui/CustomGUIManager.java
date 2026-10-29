package com.muhammaddaffa.playerprofiles.manager.customgui;

import com.muhammaddaffa.mdlib.utils.Common;
import com.muhammaddaffa.playerprofiles.PlayerProfiles;
import com.muhammaddaffa.playerprofiles.inventory.items.GUIItem;
import com.muhammaddaffa.playerprofiles.inventory.items.ItemsLoader;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public class CustomGUIManager {

    private final Map<String, CustomGUI> guiMap = new HashMap<>();

    private final PlayerProfiles plugin;
    public CustomGUIManager(PlayerProfiles plugin){
        this.plugin = plugin;
    }

    public CustomGUI getByName(String name){
        return guiMap.get(name);
    }

    public List<String> getListName(){
        return new ArrayList<>(guiMap.keySet());
    }

    public void clearCustomGUI(){
        guiMap.clear();
    }

    public void reloadCustomGUI(){
        clearCustomGUI();
        loadCustomGUI();
    }

    public void loadCustomGUI() {
        // Get the directory path
        // Get the custom-gui directory
        File directory = getMainDirectory();
        // Check if the directory is not exist
        if(!directory.exists())
            // Finally create the directory
            directory.mkdirs();
        // Get all files in custom-gui directory
        File[] files = directory.listFiles();
        // Check if there is no files
        if(files.length <= 0){
            // Create the file onto the server directory
            plugin.saveResource("custom-gui/punish-gui.yml", false);
        }
        // Now get the final list of files in the directory
        File[] finalFiles = directory.listFiles();
        // Loop through all files
        for (File file : finalFiles){
            // Get the file configuration
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            // Get the file name
            String fileName = file.getName();
            // Get the inventory title
            String title = config.getString("title");
            // Get the final title, null-safe
            String finalTitle = title == null ? "Inventory" : title;
            // Get the inventory size
            int size = config.getInt("size");
            List<GUIItem> guiItems = ItemsLoader.readItems(config);
            // After items are loaded, now we create the CustomGUI object
            CustomGUI customGUI = new CustomGUI(fileName, Common.color(finalTitle), size, config, guiItems);
            // Now we store the custom gui onto the list
            this.guiMap.put(fileName, customGUI);
        }
        // That's it
    }

    private File getMainDirectory() {
        return new File(PlayerProfiles.getInstance().getDataFolder() + File.separator + "custom-gui");
    }

}
