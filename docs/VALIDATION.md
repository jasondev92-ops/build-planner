# 0.1.9 update — 16 November 2026

61 JUnit tests pass, compiled to Java 11 bytecode against RuneLite 1.13.3. New tests exercise real Swing release-notice dismissal and acknowledgement, automatic material cards with unknown and known counts/prices, manual Have all controls, and catalogue-wide placeholder removal with functional-description spot checks. The original 57 tests still pass.

The independent source audit still covers all 960 table rows, 33 upgrade totals, 326 material names and 500 room mappings. A before/after catalogue comparison confirms only description strings changed; IDs, materials, requirements, sources, stats and dependencies are identical. Description overrides are explicit and reapplied by the catalogue generator.

Real Swing previews were inspected at 242 × 800. No live game login or full Gradle build was run here. The verification harness uses pinned RuneLite 1.13.3 dependencies; the development Gradle launcher continues to resolve latest.release. The existing launcher unchecked-varargs warning remains.

Only the separate lastSeenVersion configuration key is added. Project persistence keys and schema are unchanged; no account information or saved plans are included.

# 0.1.8 update — 15 November 2026

57 JUnit tests pass with Java 11 bytecode against RuneLite 1.13.3. New tests exercise actual Swing room selection, category reset, independent per-build materials, quantities, full upgrade chains, merged additions and completion/undo. Collapse/expand tests verify independent cards, preserved collapse state after merged additions and room filtering, and unchanged project requirements. Catalogue checks cover shared-room furniture, excluded world/Sailing recipes and the full unfiltered catalogue. The independent data audit validates 500 room mappings across 24 room types alongside the existing 960-row, 33-upgrade-total and 326-material checks.

Real Swing Plan and Find previews inspected at 242 × 800. No live-game or full Gradle run was performed here. Existing launcher unchecked-varargs warning remains. Saved-state schema, recipe IDs and configuration keys are unchanged.

# 0.1.7 update

52 tests pass. A populated saved plan restores when its profile data is supplied; null, empty and whitespace profile data each produce a separate empty My builds project with no entries or owned counts. Production resources contain only recipes, material mappings and release metadata. No personal saved plans are bundled. Removed the obsolete unreviewed notice; saved-state keys and schema are unchanged.

# 0.1.6 update

0.1.6: 51 JUnit tests pass against RuneLite 1.13.3 with Java 11 bytecode. Independent source audit accounts for all 960 rows and checks all 33 upgrade totals and 326 material mappings. Real Swing preview inspected at 242 × 800. No full Gradle dependency-resolution run or live-game run performed here; the launcher retains its existing unchecked-varargs warning.

# Validation — 22 October 2026

## Completed

- Compiled all production and test Java against the real RuneLite client/API 1.13.3 artifacts using Eclipse ECJ 3.38.0, targeting Java 11 (class-file major version 55).
- 28 JUnit tests passed on OpenJDK 17.0.20 in headless mode.
- Tests cover bundled catalogue validation, boat-specific materials, search, no-result handling, combined quantities, upgrade dependencies, missing counts, large totals, independent projects, saved-state round trips, malformed state, schema rejection, cycles, missing dependencies and actual Swing search/add controls.
- Rendered and inspected all four real Swing tabs at 242 × 800 pixels (RuneLite's panel plus scrollbar allowance). Fixed title/control clipping and wrapped-text sizing.
- All 225 IDs unique, all material quantities positive, all declared dependencies resolvable and acyclic.
- RuneLite `ProfileChanged` handling added to reload the selected configuration's projects.
- Compiled JAR includes production classes and bundled recipes only; no test classes, dependency JARs, credentials or account data.

- Search regression verifies one batched insertion when clearing a no-match query, shared renderer reuse, fixed row geometry, and repeated backspacing to the full catalogue.
- User reported the previous version successfully built via Gradle on Temurin 11, launched in game, updated material colours, and retained lists after restarting.
- The 0.1.1 search fix needs live responsiveness confirmation on the user's PC; no frame-time or input-latency measurements were taken there.

## Environment limits

The full Gradle build of this update was not run in this environment. This environment has a Java runtime but no JDK compiler, and a JDK download was unavailable. The alternate verification script uses checksum-verified ECJ and actual RuneLite dependencies; it is not a stub API compilation. This verifies Java compilation and tests, not the entire Gradle dependency graph or Plugin Hub CI.

No live game/client login by the build environment, bank reading, actual construction, Jagex Launcher integration or Plugin Hub review was performed. Headless rendering verifies component layout, not RuneLite's exact live theme or screen-height behavior.

## Local acceptance steps

1. Run `gradlew.bat test`, then `gradlew.bat run` with a JDK installed.
2. Enable/disable/re-enable the plugin; confirm only one sidebar icon appears.
3. Search for a sloop hull, helm and salvaging station; compare displayed materials with the in-game build interface.
4. Add one cargo hold and two hooks; confirm duplicate materials combine correctly.
5. Test a wardrobe/pool upgrade with and without `Build from scratch`.
6. Enter manual counts, close/reopen RuneLite and confirm persistence.
7. Switch RuneLite configuration profiles and confirm projects do not bleed into another profile.
8. Check the four tabs at your normal window size; scroll lengthy recipes and projects.
9. Copy a shopping list and open a source link.
10. Expand catalogue coverage before describing the plugin as an all-recipes encyclopedia.

## Version 0.1.2

- All 225 records have a description, an explicit game-icon ID and a specific item-page target. RuneLite 1.13.3 gameval ItemID names ground icon mappings; ambiguous flatpacks, bedroom/costume wardrobes, ship variants, and ship/house teleport focuses are mapped explicitly.
- Regression tests cover cargo capacities with each boat filter, actual Wiki button target, earlier-step control visibility, bounded proportional image scaling, and out-of-order asynchronous image completion.
- Existing recipe IDs/config keys are preserved. Saved-state round-trip and project tests still pass.
- Rendered the real panel at 242 × 800, including an oak-chair preview using the game sprite mirrored by osrsreboxed. Live game-cache rendering of all 225 icons remains a user acceptance check.
- Some specific Wiki targets are constructed from established item titles because indexed page bodies were unavailable. These have not all been HTTP/click verified. Descriptions are concise functional summaries; detailed effects are only added where supported. This is not an exhaustive item-stat database.
- Hemp trawling net Construction corrected from 70 to 65, based on its current Wiki page and the listed 7 January 2026 change.

## Version 0.1.3 — automatic supplies and price estimates

- 38 tests pass against RuneLite 1.13.3, targeting Java 11. This includes 10 new supplies tests.
- Tested all 112 material mappings, specific duplicate-name IDs, exact jewellery charges, bank+inventory totals, manual independence, generic-nail allocation, unknown bank state, missing prices, non-GE materials, and costs exceeding signed-long range.
- Exercised the actual LiveSupplies adapter with proxy RuneLite API interfaces: normalized noted items; rejected positive-quantity placeholders; waited for visible-bank observation; combined withdrawal/deposit events once per game tick; cleared quantities on account change and logout.
- Tested old-save loading, manual-count-mode persistence, and copied shopping-list consistency.
- Inspected actual Swing layout at 242 × 800 with both an unknown-bank state and clearly labelled sample prices/counts. These previews are simulated data, not an in-game bank capture.
- No authenticated RuneLite/game session is available here. Real bank visibility (including PIN, tabs and other bank plugins), price-cache availability, and live event timing remain local acceptance checks. No Plugin Hub review or full Gradle build of 0.1.3 was performed here.


## 0.1.4 beta — 23 October 2026

48 JUnit tests pass against pinned RuneLite 1.13.3 client/API using the ECJ Java 11 compiler. A clean class output directory is now used for every verification run. New tests cover completion/undo, merge limits, old save decoding, project isolation, boosted/unknown level marks, live/manual material filters, RuneLite settings metadata, planned bank item selection, and rendered overlay opt-in/placeholder behavior. Actual Swing panels rendered at 242 x 800 and inspected for layout.

Live acceptance still required: plugin gear opens its checkbox; toggle highlights immediately; selected-project changes and completion remove relevant highlights; level marks change as boosts expire; completed builds survive restart and undo restores material quantities. Check new icons and specific Wiki links. Existing 0.1.3 live success does not establish these checks.

The full Gradle graph and a real RuneLite session cannot be exercised by this headless verification script. Recipe coverage remains incomplete. The new 37 numeric recipes were checked against indexed Wiki tables; this is not an exhaustive audit.


## 0.1.5 preparation build

User confirmed all 0.1.4 features work in-game. Header wording updated; the release version is shown at the top right and in Info. Three GitHub forms prepared; feedback button awaits a verified repository URL and remains hidden until configured. No recipe data changed. Full recipe coverage and publication remain unfinished.
