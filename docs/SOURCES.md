# Recipe and implementation sources

Checked 22 October 2026. Recipe facts were curated from indexed OSRS Wiki tables; direct bulk HTML/API retrieval returned access errors. Each JSON record retains its source URL and check date. No missing recipe was filled with guessed materials. The catalogue is intentionally labelled incomplete.

## Sailing

- https://oldschool.runescape.wiki/w/Hull — all seven hull tiers and three boat-size material formulas. Boat access minimums retained.
- https://oldschool.runescape.wiki/w/Mast_and_sails — logs/nails/fabric amounts per boat; tier levels.
- https://oldschool.runescape.wiki/w/Helm — plank/metal quantities and tier levels; apply the boat's access minimum.
- https://oldschool.runescape.wiki/w/Keel — all skiff/sloop keels; no raft keel invented.
- https://oldschool.runescape.wiki/w/Salvaging_hook — all hooks; notably rune hooks require BOTH lead and cupronickel bars.
- https://oldschool.runescape.wiki/w/Cannon_(facility) — cannon construction requirements.
- https://oldschool.runescape.wiki/w/Dragon_cannon — dragon barrel and schematic requirement.
- https://oldschool.runescape.wiki/w/Boat_facility — cargo holds, stations, wind and miscellaneous facilities.
- https://oldschool.runescape.wiki/w/Trawling_net — net materials and build levels.
- https://oldschool.runescape.wiki/w/Rope_trawling_net — rope-net materials cross-check.

Quantities refer to building each selected component, not buying a whole boat. Sloop and skiff access floors are combined with component levels. Facilities have specific compatible hotspots; the catalogue's Any boat designation means the recipe has no material-size variant, not that every hotspot accepts it.

## House

- https://oldschool.runescape.wiki/w/Construction/Experience_table — named independent furniture recipes. Mahogany Homes contracts and incomplete extracted rows were excluded.
- https://oldschool.runescape.wiki/w/Costume_room — storage prerequisite chains; toy boxes are independent builds.
- https://oldschool.runescape.wiki/w/Pool_space — five-step pool chain.
- https://oldschool.runescape.wiki/w/Jewellery_box_space — exact jewellery charges and prerequisite chain.
- https://oldschool.runescape.wiki/w/Portal_nexus — frame upgrades, mounted amulets; attunement runes excluded.
- https://oldschool.runescape.wiki/w/Altar_space — chapel altars.
- https://oldschool.runescape.wiki/w/Workbench — direct workbenches and vice/lathe dependencies.
- https://oldschool.runescape.wiki/w/Construction — crude wooden chair.
- https://oldschool.runescape.wiki/w/Achievement_gallery — quest list.

## Decisions and unresolved coverage

- XP differs between some Wiki summary/detail tables (e.g. dragon cannon and linen trawling net), so this version does not display XP.
- No prices: they change frequently and are not needed for material planning.
- Built furniture prerequisites are explicit recipe dependencies, never shopping-list items.
- Optional fees, tools, unlocks and operation requirements are not merged into consumed materials.
- Many house decorations, room purchases, island construction, cosmetic boat parts and intermediate recipes remain absent. Do not infer absence in the game from absence here.
- Generic house nails can be any metal; Sailing's fixed-tier nails remain separate to avoid falsely treating them as interchangeable.

## RuneLite

- https://github.com/runelite/plugin-hub — development/review requirements and bundled-resource guidance.
- https://github.com/runelite/example-plugin — Gradle wrapper and development-launcher pattern.
- https://github.com/runelite/runelite — PluginPanel, NavigationButton, ClientToolbar, ConfigManager, ProfileChanged and LinkBrowser APIs.
- https://github.com/runelite/runelite/wiki/Rejected-or-Rolled-Back-Features — reviewed for relevant restrictions; no acceptance guarantee is implied.
- https://repo.runelite.net/net/runelite/client/1.13.3/client-1.13.3.pom — dependency/version verification.

Original code only references the APIs; no RuneLite game-client source is copied into this project. Gradle wrapper files retain their upstream notices.
