# Release acceptance — 0.1.8

Automated: 57 tests pass; all existing source-table and material checks pass. Room metadata covers 500 recipes and 24 POH room types.

Before submitting the Plugin Hub update:

- Open an existing saved project. Each Plan card should show that build's materials beneath its name.
- Collapse and expand individual cards; check room labels and right-aligned material amounts. Filter Plan to a room and back; List should still total the whole project.
- Change a quantity, add another copy and remove a build; confirm each card and List update correctly.
- Compare an upgrade-only recipe with Include earlier upgrades; confirm the latter includes its prerequisites.
- Mark a build complete and undo it. Completed cards retain a labelled reference; List excludes them until undone.
- Select Kitchen, Costume room and Superior garden in Find. Search and level/material shortcuts should still work with the room selection.
- Check a shared bookcase in Parlour, Quest hall and Study. Switch to Sailing and confirm All rooms resets automatically.
- Check layout and typing responsiveness in the live sidebar; confirm v0.1.8-beta in the header.

Source: https://github.com/jasondev92-ops/build-planner . Plugin Hub updates require a new manifest commit and review; changing this source repository alone does not update installed clients.
