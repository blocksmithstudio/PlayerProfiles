package com.muhammaddaffa.playerprofiles.manager.customgui;

import com.muhammaddaffa.mdlib.utils.Common;
import com.muhammaddaffa.mdlib.utils.Logger;
import com.muhammaddaffa.playerprofiles.PlayerProfiles;
import com.muhammaddaffa.playerprofiles.inventory.items.GUIItem;
import com.muhammaddaffa.playerprofiles.inventory.items.ItemsLoader;
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
        File directory = getMainDirectory();
        if (!directory.isDirectory() && !directory.mkdirs()) {
            Logger.severe("Cannot create the custom-gui directory.");
            return;
        }
        File[] files = directory.listFiles();
        if (files == null) {
            Logger.severe("Cannot read the custom-gui directory.");
            return;
        }
        if (files.length == 0) {
            plugin.saveResource("custom-gui/punish-gui.yml", false);
            files = directory.listFiles();
            if (files == null) {
                Logger.severe("Cannot read the custom-gui directory.");
                return;
            }
        }
        for (File file : files) {
            if (!file.isFile() || !file.getName().toLowerCase(Locale.ROOT).endsWith(".yml")) continue;
            try {
                YamlConfiguration config = new YamlConfiguration();
                config.load(file);
                ItemsLoader.validateMenu(config);
                List<GUIItem> items = ItemsLoader.readItems(config);
                guiMap.put(file.getName(), new CustomGUI(file.getName(),
                        Common.color(config.getString("title", "Inventory")), config.getInt("size"), config, items));
            } catch (Exception ex) {
                // YAML parser messages can include arbitrary configuration contents.
                Logger.severe("Skipping an invalid or unreadable custom GUI (" + ex.getClass().getSimpleName() + ").");
            }
        }
    }

    private File getMainDirectory() {
        return new File(plugin.getDataFolder(), "custom-gui");
    }

}
