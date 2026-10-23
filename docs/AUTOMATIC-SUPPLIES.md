# 0.1.6 update

0.1.6 expands mappings to all 326 consumed-material names. Coins cost their face value. Existing sail appearance and reusable tools/unlocks are excluded from consumed totals. Earlier tables below describe earlier releases.

# Automatic supplies and estimates — 0.1.3

## Acceptance checklist

- Open List before opening bank: bank shows ? and asks for refresh, not zero holdings.
- Open normal bank; compare several required items with actual quantities.
- Withdraw/deposit a stack, including notes: combined ownership and missing amounts stay unchanged.
- Leave an empty bank placeholder: it must not count as owned.
- Close the bank and consume/drop an inventory material: inventory/missing amounts update.
- Switch to Manual: set aside: previous manual numbers reappear; automatic updates do not overwrite them.
- Logout/world-hop/account-switch: bank counts clear until the bank is opened again.
- Try bank search, bank tags, PIN entry, and tab changes; the whole bank container should be counted once accessible.
- Inspect both full material estimate and cost to finish. A non-GE item must show Not sold on GE.
- Switch RuneLite’s price-source setting; the next game tick updates the displayed source.
- Copy the shopping list and compare it to the panel.

## Scope and data

Snapshots are session-only, immutable copies of item-ID quantities. Account hashes are not persisted. Counts are read on the client thread and delivered to Swing on the EDT. Live updates do not rebuild Find. No bank actions, trades, purchases or gameplay clicks are automated.

Only bank and inventory are counted. Other storage/equipment is excluded. Exact charged/dose variants are required. Generic nails reserve fixed-metal requirements first and explicitly label their price assumption. Manual quantities remain independent. Price estimates exclude any unpriced materials and identify incomplete totals. Arithmetic for monetary totals uses BigInteger.

Mappings checked against RuneLite 1.13.3 [named game item definitions](https://github.com/runelite/runelite/blob/runelite-parent-1.13.3/runelite-api/src/main/java/net/runelite/api/gameval/ItemID.java). Placeholder and note normalization follows [ItemComposition](https://static.runelite.net/runelite-api/apidocs/net/runelite/api/ItemComposition.html). Price lookup follows [ItemManager](https://github.com/runelite/runelite/blob/runelite-parent-1.13.3/runelite-client/src/main/java/net/runelite/client/game/ItemManager.java).

## Material mapping

| Material | Accepted unnoted IDs |
|---|---|
| Adamant keel parts | 32011 |
| Adamantite bar | 2361 |
| Adamantite nails | 4823 |
| Air rune | 556 |
| Amulet of glory(4) | 1712 |
| Anti-venom(4) | 12905 |
| Barrel stand | 31733 |
| Blood rune | 565 |
| Body rune | 559 |
| Bolt of canvas | 31475 |
| Bolt of cloth | 8790 |
| Bolt of cotton | 31478 |
| Bolt of linen | 31472 |
| Bottled storm | 31949 |
| Broken dragon hook | 31961 |
| Bronze bar | 2349 |
| Bronze keel parts | 31999 |
| Bronze nails | 4819 |
| Bucket of milk | 1927 |
| Bucket of water | 1929 |
| Camphor hull parts | 32053 |
| Camphor logs | 32904 |
| Camphor plank | 31432 |
| Captured wind mote | 31745 |
| Charcoal | 973 |
| Clockwork | 8792 |
| Combat bracelet(4) | 11118 |
| Cotton yarn | 31469 |
| Cupronickel bar | 32892 |
| Curator's medallion | 22710 |
| Dragon cannon barrel | 32115 |
| Dragon keel parts | 32017 |
| Dragon metal sheet | 31996 |
| Dragon nails | 31406 |
| Echo pearl | 31946 |
| Fishing bait | 313 |
| Games necklace(8) | 3853 |
| Gold leaf | 8784 |
| Heart of Ithell | 31757 |
| Hemp yarn | 31466 |
| Iron bar | 2351 |
| Iron keel parts | 32002 |
| Iron nails | 4820 |
| Ironwood hull parts | 32056 |
| Ironwood logs | 32907 |
| Ironwood plank | 31435 |
| Knife | 946 |
| Large adamant keel parts | 32032 |
| Large bronze keel parts | 32020 |
| Large camphor hull parts | 32074 |
| Large dragon keel parts | 32038 |
| Large iron keel parts | 32023 |
| Large ironwood hull parts | 32077 |
| Large mahogany hull parts | 32071 |
| Large mithril keel parts | 32029 |
| Large oak hull parts | 32065 |
| Large rosewood hull parts | 32080 |
| Large rune keel parts | 32035 |
| Large steel keel parts | 32026 |
| Large teak hull parts | 32068 |
| Large wooden hull parts | 32062 |
| Law rune | 563 |
| Lead bar | 32889 |
| Limestone brick | 3420 |
| Linen yarn | 31463 |
| Lizardman fang | 13391 |
| Logs | 1511 |
| Magic stone | 8788 |
| Mahogany hull parts | 32050 |
| Mahogany logs | 6332 |
| Mahogany plank | 8782 |
| Marble block | 8786 |
| Mithril bar | 2359 |
| Mithril keel parts | 32008 |
| Mithril nails | 4822 |
| Molten glass | 1775 |
| Nails (any metal) | 4819, 4820, 1539, 4821, 4822, 4823, 4824, 31406 |
| Narwhal horn knife | 31957 |
| Oak hull parts | 32044 |
| Oak logs | 1521 |
| Oak plank | 8778 |
| Papyrus | 970 |
| Plank | 960 |
| Prayer potion(4) | 2434 |
| Ray barbs | 31959 |
| Relicym's balm(4) | 4842 |
| Ring of dueling(8) | 2552 |
| Ring of wealth (5) | 11980 |
| Rope | 954 |
| Rosewood hull parts | 32059 |
| Rosewood logs | 32910 |
| Rosewood plank | 31438 |
| Rune keel parts | 32014 |
| Rune nails | 4824 |
| Runite bar | 2363 |
| Skills necklace(4) | 11105 |
| Soft clay | 1761 |
| Soul rune | 566 |
| Stamina potion(4) | 12625 |
| Steel bar | 2353 |
| Steel keel parts | 32005 |
| Steel nails | 1539 |
| Super restore(4) | 3024 |
| Swamp tar | 1939 |
| Swift albatross feather | 31952 |
| Teak hull parts | 32047 |
| Teak logs | 6333 |
| Teak plank | 8780 |
| Tinderbox | 590 |
| Water rune | 555 |
| Wooden hull parts | 32041 |
| Xeric's talisman (inert) | 13392 |


0.1.4 adds mappings: Bucket 1925, Ball of wool 1759, Coal 453, Swamp paste 1941, Bucket of sand 1783 and Unpowered orb 567. Completed lines no longer contribute to requirements.
