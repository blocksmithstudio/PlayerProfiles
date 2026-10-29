package com.muhammaddaffa.playerprofiles.manager.customgui;

import com.muhammaddaffa.mdlib.utils.Logger;
import com.muhammaddaffa.playerprofiles.ConfigValue;
import com.muhammaddaffa.playerprofiles.PlayerProfiles;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

public class CustomGuiCreator {
    private final PlayerProfiles plugin;

    public CustomGuiCreator(PlayerProfiles plugin) {
        this.plugin = plugin;
    }

    public void createCustomGui() {
        FileConfiguration config = PlayerProfiles.GUI_CREATOR.getConfig();
        if (!config.isConfigurationSection("menu")) return;
        File directory = new File(plugin.getDataFolder(), "custom-gui");
        if (!directory.isDirectory() && !directory.mkdirs()) {
            Logger.severe("Cannot create the custom-gui directory.");
            return;
        }
        for (String section : config.getConfigurationSection("menu").getKeys(false)) {
            String name = config.getString("menu." + section + ".fileName");
            // Creator entries name files in this directory, never arbitrary paths.
            if (name == null || !name.endsWith(".yml") || name.contains("/") || name.contains("\\") || name.equals(".yml")) {
                Logger.severe("Custom GUI file names must be plain .yml file names.");
                continue;
            }
            File file = new File(directory, name);
            try {
                if (!file.getCanonicalFile().getParentFile().equals(directory.getCanonicalFile())) {
                    Logger.severe("Custom GUI file must remain inside the custom-gui directory.");
                    continue;
                }
                if (Files.exists(file.toPath(), java.nio.file.LinkOption.NOFOLLOW_LINKS)) continue;
                Files.writeString(file.toPath(), ConfigValue.template(), StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
                Logger.info("Created a custom GUI.");
            } catch (IOException ex) {
                Logger.severe("Failed to create a custom GUI (" + ex.getClass().getSimpleName() + ").");
            }
        }
    }
}
