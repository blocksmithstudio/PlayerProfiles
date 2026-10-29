package com.muhammaddaffa.playerprofiles.manager.profile;

import com.muhammaddaffa.mdlib.utils.Config;
import com.muhammaddaffa.playerprofiles.PlayerProfiles;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.charset.StandardCharsets;
import java.io.IOException;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ProfileManager {

    private final Map<String, Profile> profileMap = new HashMap<>();
    private boolean loaded;

    public Profile getOrCreate(Player player){
        return getOrCreate(player.getUniqueId());
    }

    public Profile getOrCreate(UUID uuid){
        return getOrCreate(uuid.toString());
    }

    public Profile getOrCreate(String uuid){
        return profileMap.computeIfAbsent(uuid, key -> new Profile(key, false, this::saveProfileData));
    }

    public boolean isProfileLocked(Player player){
        return getOrCreate(player).isLocked();
    }

    public void lockProfile(Player player){
        getOrCreate(player).setLocked(true);
    }

    public void unlockProfile(Player player){
        getOrCreate(player).setLocked(false);
    }

    public void loadProfileData(){
        loaded = false;
        Config data = PlayerProfiles.DATA_DEFAULT;
        if (data == null) return;
        YamlConfiguration config = new YamlConfiguration();
        try {
            config.load(data.getFile());
            if (config.contains("data") && !config.isConfigurationSection("data")) {
                throw new IllegalArgumentException("Invalid profile data section");
            }
            Map<String, Profile> restored = new HashMap<>();
            if (config.isConfigurationSection("data")) {
                for (String uuid : config.getConfigurationSection("data").getKeys(false)) {
                    if (!config.isBoolean("data." + uuid)) throw new IllegalArgumentException("Invalid lock state");
                    restored.put(uuid, new Profile(uuid, config.getBoolean("data." + uuid), this::saveProfileData));
                }
            }
            data.getConfig().loadFromString(config.saveToString());
            profileMap.clear();
            profileMap.putAll(restored);
            loaded = true;
        } catch (Exception ex) {
            // Never replace unreadable/corrupt recovery data with an empty map.
            throw new IllegalStateException("Cannot load data.yml; the original file has been preserved.");
        }
    }

    public void saveProfileData() {
        Config data = PlayerProfiles.DATA_DEFAULT;
        if (!loaded || data == null) return;
        FileConfiguration config = data.getConfig();
        Map<String, Object> previous = config.isConfigurationSection("data")
                ? new HashMap<>(config.getConfigurationSection("data").getValues(false)) : null;
        for (Profile profile : profileMap.values()) {
            config.set("data." + profile.getUUID(), profile.isLocked());
        }
        Path file = data.getFile().toPath();
        Path temporary = null;
        try {
            // A complete replacement prevents a crash from leaving partially written YAML.
            temporary = Files.createTempFile(file.toAbsolutePath().getParent(), ".profiles-", ".tmp");
            Files.writeString(temporary, config.saveToString(), StandardCharsets.UTF_8);
            try {
                Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            config.set("data", null);
            if (previous != null) config.createSection("data", previous);
            throw new IllegalStateException("Unable to save profile data; lock state was not changed.");
        } finally {
            if (temporary != null) {
                try { Files.deleteIfExists(temporary); } catch (IOException ignored) { }
            }
        }
    }
}
