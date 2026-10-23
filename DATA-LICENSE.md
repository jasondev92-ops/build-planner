# Recipe data attribution

Recipe facts were researched from the **Old School RuneScape Wiki**, written
by its contributors: https://oldschool.runescape.wiki/ . Per-recipe source links
are stored in `src/main/resources/com/buildplanner/recipes.json` and listed in
`docs/CATALOGUE.md`. Source families and adaptations are documented in
`docs/SOURCES.md`.

The Wiki states that its content is available under **Creative Commons
Attribution–NonCommercial–ShareAlike 3.0**, with additional terms for game
assets. To the extent this curated dataset and the recipe-data portions of
`tools/catalogue.py` are adaptations of that content, they are supplied under
that same license:
https://creativecommons.org/licenses/by-nc-sa/3.0/

Adaptations: selected recipe facts were reformatted into JSON, item names
normalised, boat-specific variants split into separate entries, boat access
minimums incorporated, upgrade prerequisites represented as dependencies, and
original explanatory notes added. Data checked 22 October 2026. No claim of
endorsement by the Wiki, Jagex or RuneLite is made.

The plugin loads item artwork from the running game through RuneLite ItemManager; it does not redistribute a sprite library. Game artwork belongs to Jagex. Documentation previews may show a game icon; the oak-chair preview uses the sprite mirrored at https://github.com/0xNeffarion/osrsreboxed-db/blob/master/docs/items-icons/8312.png . The sidebar icon is a simple original drawing.
RuneScape and Old School RuneScape are trademarks of Jagex Limited.

This data notice does not replace the BSD-2-Clause license for original plugin
logic. Keep both notices when redistributing the combined package. Resolve
any additional Plugin Hub distribution requirements with its reviewers before
public release.

The 23 October 2026 expansion uses the supplied Construction experience table (960 rows), available at https://oldschool.runescape.wiki/w/Construction/Experience_table . `tools/construction-table.json`, the audit and generated recipe facts retain the Wiki attribution and terms above. Upgrade totals are folded into chains, no-material activities excluded, tools separated and existing project IDs retained. Named game definitions are drawn from RuneLite 1.13.3; RuneLite source is available at https://github.com/runelite/runelite/tree/runelite-parent-1.13.3 under its BSD-2-Clause license. The filtered lookup contains item names, integer IDs and constant names, not game sprites.
