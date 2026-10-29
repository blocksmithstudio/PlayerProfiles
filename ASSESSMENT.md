# PlayerProfiles assessment and fixes

Reviewed and fixed 2026-10-29. The first pass updated MDLib and reported existing
issues while preserving behavior. The owner subsequently requested fixes for all
reported issues and explicitly retained the admin forced-open bypass.

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

## Fixes to the reported issues

| Original finding | Resolution |
| --- | --- |
| Null players in admin commands | Return immediately with the correct argument in the error message; shared menu entry also guards null/offline players. |
| Stale items and click handlers | Clear contents and handlers before every refresh, check visibility before creating items, and recheck permissions at click time. Reload and shutdown close existing menus so old configuration/tasks cannot keep running. |
| Malformed item specifications | Validate identifiers, missing values and inventory slot bounds; invalid specifications produce an error item. Invalid textures use an ordinary player head; blank materials and AIR fillers cannot crash item construction. Equipment/slot previews clone the target's item. |
| Wrong custom-menu filler | Each menu passes its own configuration to the renderer. |
| Null/disconnected distance target | Close safely, cancel timers and use the captured display name. Auto-refresh also stops on disconnect. |
| Unsafe GUI configuration/files | Validate chest dimensions and integer slot bounds; skip malformed YAML and non-YAML files/directories; handle unreadable directories and absent creator sections. Creator file names cannot escape the GUI directory. |
| Cooldown retention/reload | Store UUIDs with expiry times, prune expired entries, remove entries on quit, and clear/ignore existing cooldowns when the feature is disabled. |
| Inconsistent opening restrictions | Normal entry points share permission, lock, world, region, NPC, combat and visibility checks. **Admin forced-open commands retain their explicit bypass**, as requested, including subsequent menu navigation. Owners may view their own locked profiles. |
| Hidden players / right-click permission | Normal viewing checks `canSee` and uses the existing profile permission nodes. |
| Shutdown-only saves/startup failure | Lock changes persist immediately, including changes through managed Profile objects. Write a complete temporary file and replace data.yml; revert the in-memory change if saving fails. Failed/corrupt loads never save an empty replacement. Shutdown saves before MDLib cleanup. |
| Dependency detection / WorldGuard version | Check enabled plugins, reset optional API state and assign WorldGuard versions with correctly scoped branches. Disabled combat hooks return safely. |
| Command messages | Fix the permission placeholder and invalid-player substitutions; toggle usage no longer advertises an unsupported target argument. Acknowledgements follow successful saves. |
| Legacy version helpers | Use numeric version comparison with tested legacy and 26.x boundaries. Invalid refresh periods/distances are normalized before scheduling. |

An additional packaging regression was found during verification: incremental dist
builds could reuse previously shaded class entries. The dist JAR is now recreated
before shading, so the distribution includes the current core classes.

The repository's existing license restricts redistribution. Making the repository
public does not change that license or grant redistribution rights.

## Verification

- Default seven-module Maven build/install and `./checks/run.sh`: passed on JDK 25.
- Clean `paper-26.3` build against `26.3.build.230-stable`: passed. All 43 plugin
  class files match the default artifact used by the runtime tests.
- Paper 26.3 build 230: **85 regression checks passed**, followed by **86 checks
  after an actual server restart**, including restored profile lock state.
- Runtime coverage includes command registration, owner/visitor/custom menus,
  regular access checks, forced admin navigation, immediate permission revocation,
  invalid item specifications and AIR fillers, disconnects, cooldown expiry-state
  cleanup/reload, malformed GUI files, creator path restrictions, immediate saves,
  corrupt-load preservation and save-failure rollback of memory/cache/disk state.
- The distribution contains relocated MDLib, excludes server and optional plugin
  APIs, and has no duplicate ZIP entries. Incremental packaging was also exercised.
- Shell syntax and exact staged content/credential-pattern checks passed; private
  test worlds, logs, local tools and caches remain ignored and unpublished.
  The disposable servers stopped after the checks.

Runtime regression checks use synthetic players on a real Paper server. Live
client sessions and installed PlaceholderAPI/WorldGuard/WorldEdit/CombatLogX/
DeluxeCombat combinations remain outside the automated integration coverage.
Folia compatibility is not claimed.
