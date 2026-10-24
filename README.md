# Build Planner — 0.1.7 beta

A RuneLite sidebar for Construction and Sailing recipes, saved projects, combined materials, bank/inventory counts and estimated costs.

**921 recipe/build-action entries**, covering every material-bearing row in the supplied OSRS Wiki Construction experience table. Alternate methods are separate entries; 33 cumulative totals use upgrade chains instead of duplicate recipes. Six activities without material recipes are excluded. This is coverage of that source snapshot, not a claim to include every craftable item in OSRS.

The version appears in the header's upper-right corner. The subtitle is **Construction & Sailing**. Use **Info → Suggestions & bug reports** to open [the feedback forms](https://github.com/jasondev92-ops/build-planner/issues/new/choose).

**Validation:** Java 11 bytecode compiled against RuneLite 1.13.3; 52 automated tests pass. All 33 source upgrade totals match the planner's expanded requirements; all 326 material names have bank-item mappings. 649 entries have named game-icon mappings; 272 explicitly show “Image not yet verified”. The previously tested features remain, but this expanded catalogue still needs live-game acceptance testing. Available in the RuneLite Plugin Hub following review of the initial release.

## Changes in 0.1.7

Removed the outdated unreviewed notice and clarified that saved projects belong to the RuneLite configuration profile. Development and public installations using the same profile restore the same plans. New profiles start with an empty “My builds” project; no personal plans are bundled. Existing saves are preserved.

## Changes in 0.1.6

- Expanded from 262 to 921 entries, preserving all existing recipe IDs and saved projects.
- Added components, cosmetics, decorations and other material-bearing Construction activities in the supplied table.
- Separated reusable tools/unlocks from consumed materials; displayed facilities and other skill requirements. Automatic checks still cover Construction and Sailing levels only.
- Treated coin requirements at face value and existing sail colour as a prerequisite, not a purchasable item.
- Kept quantities as build/crafting actions: a recipe may produce multiple output items. Components are not recursively crafted automatically.
- Added public suggestion, bug and recipe-correction forms, opened through Info.

## Updating from an earlier version

Close the development RuneLite client, extract this archive to a new folder, and open its `build-planner` folder in IntelliJ. Select your existing Temurin 11 SDK if asked. Run the Gradle `test` task, then `run`, as before. No Java reinstall or account setup change is needed. Projects remain in the same RuneLite configuration profile; existing project IDs and manual quantities are preserved. Automatic mode is the default; Manual mode restores your set-aside counts.

Version 0.1.2 adds descriptions and game-icon mappings to all 225 recipes, with the item name above the image. Icons load asynchronously from RuneLite's game cache, are scaled proportionally within 96 × 64 pixels, and cannot overwrite a newer selection when a previous request finishes late. The old search performance fix remains in place.

Cargo holds show capacity for the boat selected in Find (or all three with All boats). Selected other items have supported stats and effects. Wiki buttons target individual item pages, with explicit disambiguation for Sailing facilities. “Include earlier upgrades” only appears on a recipe with prerequisites and adds those earlier materials to the total. Recipe IDs and saved-project configuration keys are unchanged.

The image identifiers were checked against RuneLite's named game definitions and available Wiki icon IDs. Full visual inspection of all live game sprites and HTTP confirmation of every Wiki target were not possible here; the Wiki blocks direct requests and some pages are absent from the search index. See `docs/ITEM-DETAILS.md` for coverage and acceptance checks.

## New controls (0.1.4)

- Find shows a green check or red cross beside Construction and Sailing requirements, using your current boosted levels. Offline levels show a neutral question mark. Tool bonuses, quests, other skills and operating requirements are not automatically checked.
- Find has **Within my levels** and **Have materials** filters. The material filter checks one copy of the selected recipe step, using your project's automatic/manual count mode; it does not include previous upgrades or reserve materials for other builds.
- Plan has **Mark complete** and **Undo completion** for each line. Completion covers the whole quantity, retains a saved record, and excludes its materials and bank highlights. Manual stock is not deducted automatically.
- In RuneLite's plugin settings, search **Build Planner**, click its gear, then check **Automatic bank highlighting**. It defaults off. Highlights show materials for unfinished builds in the selected project, even when you already own enough; it is a gathering aid, not a missing-only filter. Placeholders are excluded.
- Adds 27 house entries and ten island amenity types, bringing that release to 262. See `docs/COVERAGE-AUDIT.md` for additions and remaining gaps.

## Automatic counts and prices (0.1.3)

1. Start the plugin as before and log in.
2. Open your normal bank once (after entering your bank PIN if needed).
3. In **List**, leave **Automatic: bank + inventory** selected.
4. The list shows required, bank, inventory and missing quantities, estimated full material cost and cost to finish. Withdraw/deposit some materials: combined availability should stay unchanged.
5. Choose **Manual: set aside** to use your saved manual quantities instead. This choice is saved per project.

Bank counts are held in memory for the current login session and cleared on logout, account change, world hopping or connection loss. Reopen the bank to refresh afterward. The panel labels its last bank observation. Inventory updates while logged in. Bank and inventory changes are combined once per game tick to avoid double-counting transfers. Noted items count as their unnoted form; placeholders do not count.

Prices use RuneLite ItemManager and follow RuneLite's GE-guide/Wiki-price setting. The plugin checks that price cache once per minute (also when the source setting changes); this is **not** a guarantee that market data refreshes every minute. Allow up to one minute for initially unavailable prices to appear. “Cache checked” is the plugin's lookup time, not the market quote's timestamp. Prices are estimates, not instant-buy offers. Non-GE items and unavailable prices are explicitly unpriced; totals become labelled subtotals when incomplete.

Ship cargo, equipment, rune pouches, plank sacks, potion storage and other special storage are excluded. Charged jewellery and potions match the exact dose/charge version required by the recipe. Generic nails use leftover stock after fixed-metal requirements reserve their share; estimates name the chosen nail type. Automatic stock is independently available to each project, not reserved across projects. Completed builds are not automatically removed from the plan.

See `docs/AUTOMATIC-SUPPLIES.md` for the acceptance checklist and mapping sources.

## What works

- Search names and materials; filter Construction/Sailing and boat size.
- 921 bundled entries: 182 Sailing and 739 other Construction entries.
- Separate raft/skiff/sloop quantities for core boat parts.
- Create, rename, switch and delete saved projects.
- Choose quantities; combine shared materials across builds.
- Plan house upgrades as one step or include their full prerequisite chain.
- Enter materials you have set aside, or mark a material fully gathered.
- Copy a shopping list with required, owned and missing counts.
- Open each recipe's Wiki source.
- Save in the active RuneLite configuration profile; reload when profiles switch.
- Preserve unreadable saved data and offer an explicit backup/reset action.

## What is not finished

The catalogue covers the supplied table's material-bearing rows. It does not automatically track future game updates or every room purchase, unlock or output quantity. 272 entries still need verified icon mappings. Live acceptance of the expansion remains outstanding.

Hover lookup, special-storage scanning and automatic recipe updates are not implemented. Specific unlocks, rooms, crew requirements and operating levels may need the linked Wiki page. There is no gameplay automation.

## Later: run on your Windows PC

Nothing needs installing today. When ready:

1. Extract this entire folder.
2. Install a Java **JDK** (not just a JRE). RuneLite's official guide recommends Java 11; this project targets Java 11 bytecode. IntelliJ IDEA can install a suitable JDK for you.
3. Open `build.gradle` in IntelliJ as a Gradle project and let dependencies download.
4. Run the Gradle `test` task, then the `run` task. Alternatively, from a terminal in this folder: `gradlew.bat test` followed by `gradlew.bat run`.
5. In the development client, enable **Build Planner** and open its book icon in the sidebar.
6. For Jagex Account login, follow RuneLite's current official instructions: https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts . Do not share account credentials with anyone helping you test.

The `run` task starts a development RuneLite client. It does not inject anything into an already-running client. A JAR produced by the build is a compiled plugin artifact, **not a standalone application**. Do not expect double-clicking it or dropping it into a random plugin folder to install it. Use the included development launcher until Plugin Hub review is complete.

Build is pinned to RuneLite 1.13.3 for reproducibility. If that client is outdated when you test, use `gradlew.bat -PruneliteVersion=latest.release test run` and recheck compatibility. The Unix equivalent is `./gradlew`.

## Planning correctly

- `Include earlier upgrades` includes all earlier steps for supported house upgrade chains. Otherwise the previous furniture must already exist.
- Each line represents a separate build. Planning a full chain AND one of its lower steps means two builds and intentionally adds both material requirements.
- Ship hull/keel parts are ready-made inputs. The planner does not expand them into planks or bars.
- Generic house nails are listed separately from fixed-metal Sailing nails. House nail totals are minimums; bending can consume extras.
- Room costs, portal/nexus teleport attunement runes and optional fees are excluded.
- Build/access levels do not imply permission to operate a facility below its required level. Check boosts and crew rules separately.
- Manual counts mean materials set aside for that project, not live bank contents. Completing a build does not automatically consume these counts.

## Project layout

- `src/main/java/com/buildplanner`: plugin, Swing UI, catalogue and planning logic.
- `src/main/resources/com/buildplanner/recipes.json`: bundled recipes with source URLs.
- `src/test/java/com/buildplanner`: 52 tests, development launcher and real Swing preview renderer.
- `tools/catalogue.py`: reproducible catalogue generator (Python standard library only).
- `tools/verify.py`: alternate compiler/test route used here with a Java 17 runtime and checksum-verified dependencies.
- `docs/VALIDATION.md`: exact checks and remaining local acceptance steps.
- `docs/SOURCES.md`: data attribution and source decisions.
- `docs/previews`: rendered real Swing panels, not web mockups.
- `dist`: locally generated plugin JAR and checksum (not committed).

To update the catalogue, edit `tools/catalogue.py`, run it, and rerun tests. Preserve existing recipe IDs, since saved plans refer to them. Adding/removing entries requires updating the catalogue-count assertions and UI coverage text. Changing an existing recipe changes totals in saved projects when the new version loads.

## Privacy and persistence

The plugin reads bank/inventory quantities and the client account hash to isolate live snapshots. Snapshots and that hash stay in memory and are not saved or uploaded by this plugin. Prices come from RuneLite’s existing price service; the plugin adds no external HTTP service. Wiki links open only when clicked. Projects, manual quantities and count-mode preferences use RuneLite ConfigManager and may sync according to its settings. Different game accounts using the same RuneLite profile share project plans/manual counts, but live bank counts are cleared when accounts change. Use named projects or separate profiles for separate plans.

## Public release

Before submission, complete live-client testing and confirm data licensing/attribution requirements with reviewers. The initial release was accepted in Plugin Hub PR #19284. Subsequent updates use separate review requests. Official process: https://github.com/runelite/plugin-hub .

Original code is BSD-2-Clause. Wiki-derived data has separate attribution and licensing in `DATA-LICENSE.md`. RuneScape is owned by Jagex; this project is unaffiliated with Jagex or RuneLite.
