# Catalogue coverage — 0.1.6

Source: user-supplied OSRS Wiki Construction/Experience table HTML, inspected 23 October 2026. The raw HTML is not redistributed. The factual row snapshot is `tools/construction-table.json`; every row's disposition is in `tools/table-audit.json`.

| Source disposition | Rows |
| --- | ---: |
| Included recipe/build actions | 921 |
| Cumulative totals represented by upgrade chains | 33 |
| Activities without material recipes | 6 |
| Total | 960 |

The six exclusions are Brazier (Broken), Bridge (Mausoleum), Curved bone, Long bone, Mast (Tempoross), and Totem pole. All 33 cumulative totals were independently compared with expanded upgrade chains. Existing furniture prerequisites never become GE shopping-list items. All 262 previous IDs remain valid.

The catalogue contains 182 Sailing entries and 739 other Construction entries. Search's Construction filter includes house furniture and other activities. This is complete for the supplied table's material-bearing rows, not for every game recipe, every update, room purchase or unlisted unlock. Alternate methods and cosmetic variants are counted individually. Actions can produce multiple items; quantities do not assert output counts. Intermediate components remain inputs unless an explicit upgrade chain applies.

649 entries have named game-icon mappings, including explicit heraldic family mappings. 272 entries lack verified mappings and show that status in the panel. No substitute artwork is used for those entries. Live visual verification of all mapped icons remains outstanding. Existing curated descriptions/stats are retained; newer descriptions summarize the action and its facility rather than inventing gameplay effects.

326 distinct consumed-material names are mapped to inventory item IDs. Coins use 1 gp face value. Plain sail is an existing cosmetic state, not an inventory item; coloured sails consume dye only. See https://oldschool.runescape.wiki/w/Plain_sail and https://oldschool.runescape.wiki/w/Sail_patterns .

Four previous recipes had reusable requirements removed from consumed materials: Crystal extractor (Heart of Ithell), Wind catcher and Gale catcher (Captured wind mote), and Keg (Barrel stand). These remain visible under Tools / unlocks. Exact changes are in `tools/table-corrections.json`.

Run `python3 tools/catalogue.py` to regenerate and `python3 tools/check_table.py` to check source coverage and totals. The named-icon lookup snapshot is derived from RuneLite 1.13.3 `runelite-api/src/main/java/net/runelite/api/gameval/ItemID.java`; only relevant names are retained. Per-recipe `iconSymbol` and `sourceRow` preserve mapping provenance. See DATA-LICENSE.md.
