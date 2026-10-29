package com.muhammaddaffa.playerprofiles.manager.profile;

import com.muhammaddaffa.mdlib.utils.Config;
import com.muhammaddaffa.playerprofiles.PlayerProfiles;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ProfileManager {

    private final Map<String, Profile> profileMap = new HashMap<>();

    public Profile getOrCreate(Player player){
        return getOrCreate(player.getUniqueId());
    }

    public Profile getOrCreate(UUID uuid){
        return getOrCreate(uuid.toString());
    }

    public Profile getOrCreate(String uuid){
        return profileMap.computeIfAbsent(uuid, Profile::new);
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
        // Get the file configuration of data.yml
        FileConfiguration config = PlayerProfiles.DATA_DEFAULT.getConfig();
        // If there is no data, just stop the code
        if(!config.isConfigurationSection("data")) return;
        // Loop through all data config section
        for(String uuid : config.getConfigurationSection("data").getKeys(false)){
            // Get the lock status from the data
            boolean lockedStatus = config.getBoolean("data." + uuid);
            // Create a new profile object and store them into the hash map
            profileMap.put(uuid, new Profile(uuid, lockedStatus));
        }
    }

    public void saveProfileData(){
        // Get the File of data.yml
        Config data = PlayerProfiles.DATA_DEFAULT;
        // Get the file configuration of data.yml
        FileConfiguration config = data.getConfig();
        for (Profile profile : profileMap.values()) {
            config.set("data." + profile.getUUID(), profile.isLocked());
        }
        // After all data has been set, save the config
        data.saveConfig();
    }

}
