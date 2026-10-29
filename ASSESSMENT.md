# PlayerProfiles assessment

Reviewed 2026-10-29. This pass updates dependencies and packaging and makes a
small cleanup. Existing commands, permissions, configuration keys, item priority,
menu layout, click actions and profile data format are preserved. Existing logic
issues below are reported separately to respect the request to preserve behavior.

## Changes

- MDLib 2.3.3 → 2.4.7, using the published Blocksmith Maven coordinates.
- Update Maven Shade to 3.6.2 to handle MDLib's Java 25 bytecode.
- Correct the outdated MDLib relocation pattern and exclude the original MDLib
  artifact from the final distribution's second shading pass.
- Replace the unavailable provided CombatLogX API 11.4 snapshot with 11.7.
  The hook still calls the same `ICombatLogX#getCombatManager()` and
  `ICombatManager#isInCombat(Player)` methods; no combat plugin is bundled.
- Derive `plugin.yml` version from Maven (previously 8.0.3 versus Maven 8.0.4).
  Filter only the plugin descriptor; bundled GUI/configuration placeholders stay literal.
- Remove a duplicate WorldGuard dependency, repeated build properties, unused
  private code/fields/imports, and tracked IDE metadata.
- Reuse the same item loader for main and custom menus; retain field defaults,
  YAML iteration order and the existing zero-priority-to-100 rule.
- Simplify profile lookup with `computeIfAbsent` and save directly from map values.
- Add an explicit Paper 26.3 compilation profile and framework-free behavior checks.

## Existing issues retained

| Priority | Location | Finding |
| --- | --- | --- |
| High | `commands/subcommands/OpenProfileCommand.java`, `OpenGUICommand.java` | Missing returns after unsuccessful online-player lookup allow null players into menu creation. The openprofile target error also uses the recipient's argument. |
| High | `inventory/ProfileInventory.java#setAllItems` | Refresh overwrites visible slots without clearing old items and click handlers. An item hidden after permission loss can retain its old action. |
| High | `utils/ItemManager.java#createItem`, `createGUIItem` | Malformed `SLOT;`/`HEAD;` values, nonnumeric or out-of-range slots, and unsupported semicolon identifiers can throw during menu creation/refresh. The fallback head texture is truncated and invalid. |
| Medium | `inventory/ProfileInventory.java#setAllItems` | Custom menus always use the main `gui.yml` filler settings, ignoring their own `fillItems` configuration. |
| Medium | `inventory/ProfileInventory.java#checkDistance` | The null-target branch dereferences the target; disconnected nonnull targets are not checked with `isOnline()`. |
| Medium | `manager/customgui/CustomGUIManager.java#loadCustomGUI`, `CustomGuiCreator.java#createCustomGui` | Unreadable directories (`listFiles()` returns null), non-YAML files/directories, missing `menu` sections, and invalid inventory dimensions/slots are not validated consistently. |
| Medium | `listeners/PlayerInteract.java` | The cooldown map holds strong references to players and has no quit cleanup. Disabling cooldown on reload still enforces entries already in the map. |
| Medium | `commands/ProfileCommand.java`, `listeners/PlayerInteract.java` | Profile locking, disabled-world/region and combat restrictions apply to right-click interaction but are not enforced by `/profile <player>` or admin-open commands. Clarify intended command bypass policy before changing it. |
| Medium | `listeners/PlayerInteract.java` | Right-click opening has no permission or `canSee(target)` check. Hidden/vanished player handling depends on other plugins intercepting interaction. |
| Medium | `manager/profile/ProfileManager.java`, `PlayerProfiles.java#onDisable` | Lock state is persisted only on normal plugin shutdown. A process crash loses changes since the previous shutdown; a startup failure before config initialization can also cause a shutdown null dereference. |
| Low | `manager/DependencyManager.java#checkDependency` | Missing braces assign WorldGuard version 7 regardless of detected version. The field is currently unused; the wrapper selects the API independently. Optional hooks check plugin presence rather than enabled state. |
| Low | `commands/ProfileCommand.java`, `LockCommand.java`, `UnlockCommand.java`, `ToggleCommand.java` | The profile-others denial message has a malformed permission placeholder; lock/unlock invalid-player messages omit player substitution; toggle's console usage advertises an unsupported player argument. |
| Low | `utils/Utils.java` | `hasOffHand()` uses OR between negated version checks and therefore always returns true. The unused `hasCustomModelData()` version whitelist does not include 26.x. |

The repository's existing license restricts redistribution. Making the repository
public does not change that license or grant redistribution rights.

## Verification

- Default Maven reactor build/install: passed for all seven modules on JDK 25.
- Clean `paper-26.3` build: passed against Paper API `26.3.build.230-stable`.
  Project class bytecode matched the default build.
- `./checks/run.sh`: passed profile identity/lock-state and GUI field/default/order checks,
  including both bundled menu configurations.
- Paper 26.3 build 230 runtime smoke test: 31 checks passed. Verified enable/disable,
  descriptor version, all command aliases, profile lock/unlock, YAML serialization,
  reload, owner/visitor menus, custom menus, console list/reload and relocated MDLib.
  Confirmed the synthetic locked profile remained in `data.yml` after shutdown.
- Packaged GUI/config/data resources match the repository originals byte-for-byte.
  The JAR contains relocated MDLib, excludes its original namespace, and does not
  bundle Bukkit/Paper or the optional plugin APIs. No duplicate ZIP entries.
- Reachable Git history, historical JAR contents, and the exact staged files were
  checked locally for credential patterns and unintended private/build artifacts;
  no candidates were found. This is a bounded review, not a complete security audit.

The runtime checks used synthetic players on a real Paper server. Authenticated
client interaction, real inventories, PlaceholderAPI expansions, WorldGuard,
WorldEdit, CombatLogX and DeluxeCombat combinations still require live integration
validation. Folia compatibility is not claimed. There are no intentional gameplay
or permission-policy changes in this pass.
