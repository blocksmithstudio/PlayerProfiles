import com.muhammaddaffa.playerprofiles.inventory.items.GUIItem;
import com.muhammaddaffa.playerprofiles.inventory.items.ItemsLoader;
import com.muhammaddaffa.playerprofiles.manager.profile.Profile;
import com.muhammaddaffa.playerprofiles.manager.profile.ProfileManager;
import com.muhammaddaffa.playerprofiles.utils.ItemManager;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.List;
import java.util.UUID;

/** Run with assertions enabled; no test framework or server is required. */
public class BehaviorChecks {
    public static void main(String[] args) throws Exception {
        ProfileManager manager = new ProfileManager();
        UUID uuid = UUID.fromString("00000000-0000-0000-0000-000000000001");
        Profile profile = manager.getOrCreate(uuid);
        assert !profile.isLocked();
        profile.setLocked(true);
        assert manager.getOrCreate(uuid.toString()) == profile;
        assert manager.getOrCreate(uuid).isLocked();
        assert manager.getOrCreate(UUID.randomUUID()) != profile;
        assert new Profile(uuid.toString(), true).isLocked();

        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString("""
                items:
                  first:
                    type: HELMET
                    material: DIAMOND_HELMET
                    amount: 2
                    name: '&a{target}'
                    slots: [0, 8]
                    glowing: true
                    hideAttributes: true
                    usePermission: true
                    permission: example.permission
                    lore: ['{target_status}', '%player_name%']
                    leftClickCommands: ['[PLAYER] profile {target}']
                    rightClickCommands: ['[CLOSE]']
                    customModelData: 123
                    itemModel: minecraft:diamond_helmet
                    onlyOwner: true
                    onlyVisitor: false
                    priority: 7
                  defaults: {}
                """);
        List<GUIItem> items = ItemsLoader.readItems(config);
        assert items.size() == 2;
        assert items.getFirst().equals(new GUIItem("HELMET", "DIAMOND_HELMET", 2,
                "&a{target}", List.of(0, 8), true, true, true, "example.permission",
                List.of("{target_status}", "%player_name%"),
                List.of("[PLAYER] profile {target}"), List.of("[CLOSE]"),
                123, "minecraft:diamond_helmet", true, false, 7));
        GUIItem defaults = items.get(1);
        assert defaults.type().equals("DUMMY");
        assert defaults.material().equals("BARRIER");
        assert defaults.amount() == 0;
        assert defaults.priority() == 100;
        assert defaults.permission().equals("invalid.permission");
        assert defaults.itemModel().isEmpty();
        assert defaults.slots().isEmpty();
        assert defaults.leftCommands().isEmpty();
        assert defaults.rightCommands().isEmpty();
        assert ItemsLoader.readItems(new YamlConfiguration()).isEmpty();
        for (String resource : List.of("gui.yml", "custom-gui/punish-gui.yml")) {
            List<GUIItem> bundled = ItemsLoader.readItems(YamlConfiguration.loadConfiguration(
                    new File("core/src/main/resources", resource)));
            assert !bundled.isEmpty() : resource;
            assert bundled.stream().allMatch(item -> !item.slots().isEmpty());
        }
        for (String version : List.of("1.21.5-R0.1-SNAPSHOT", "26.3-R0.1-SNAPSHOT", "26.3.1-R0.1")) {
            assert ItemManager.isVersionAtLeast(version, 1, 9, 0);
            assert ItemManager.isVersionAtLeast(version, 1, 14, 0);
        }
        assert !ItemManager.isVersionAtLeast("1.8.8-R0.1", 1, 9, 0);
        assert !ItemManager.isVersionAtLeast("1.7.10-R0.1", 1, 9, 0);
        assert !ItemManager.isVersionAtLeast("1.13.2-R0.1", 1, 14, 0);
        assert !ItemManager.isVersionAtLeast("invalid", 1, 14, 0);
        assert !ItemManager.isVersionAtLeast("1.x-R0.1", 1, 14, 0);
        for (String bad : List.of("size: 0", "size: 10", "size: 63", "size: '27'",
                "size: 9\nitems: false", "size: 9\nitems: {bad: false}",
                "size: 9\nitems: {bad: {slots: [-1]}}", "size: 9\nitems: {bad: {slots: [9]}}",
                "size: 9\nitems: {bad: {slots: ['0']}}")) {
            config.loadFromString(bad);
            boolean rejected = false;
            try { ItemsLoader.validateMenu(config); }
            catch (IllegalArgumentException expected) { rejected = true; }
            assert rejected : bad;
        }
        config.loadFromString("size: 9");
        ItemsLoader.validateMenu(config);
        config.loadFromString("size: 9\nitems: {first: {slots: [0, 8]}}");
        ItemsLoader.validateMenu(config);
        System.out.println("Behavior checks passed: profile state, item defaults/order, version boundaries and menu validation.");
    }
}
