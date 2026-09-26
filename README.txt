Endless Refined
===============
Integration between Endless Inventory and Refined Storage, plus a multi-group hotbar.

For Minecraft 1.20.1 / Forge 47.x.


Requirements
------------
- Endless Inventory 1.1.x            (required)
- Refined Storage 1.12.x             (optional)
- JEI 15.x                           (optional)

NOT compatible with the standalone QuadHotbar mod. This mod includes a fork of it, and
both have to patch the same Minecraft instruction, so only one of the two may be
installed. See THIRD-PARTY.md.


What it does
------------

1. A Refined Storage portable grid with no disk
   An empty portable grid opens instead of showing greyed-out slots, and its contents are
   the player's Endless Inventory. Taking items out reduces the stock; putting them back
   increases it. Item counts beyond a stack show as they do elsewhere in Refined Storage,
   because Refined Storage carries counts as ints.

2. Endless Inventory that behaves like inventory space
   - Items that arrive in slots 9-35 are moved to Endless Inventory (`inventory.sweep`).
     Only passively gained items: items moved around inside a GUI are left alone, so
     shift-clicking is safe.
   - Items that fit nowhere go to Endless Inventory rather than to the ground
     (`inventory.overflow`).
   - Slots a hotbar row is showing are never touched by either.

3. A multi-group hotbar
   Vanilla's nine-slot hotbar becomes up to four groups of nine, selectable across the
   extended inventory slots.
   - Ctrl+1 .. Ctrl+9 show pages of the hotbar table: eighty-one cells of Endless
     Inventory items, nine per page, three pages per band.
   - Ctrl+0 swaps the hotbar between the player's own inventory and their ender chest.
     The ender chest's twenty-seven slots move in and out of the hotbar as they are; a
     death or a logout puts them back where they belong first.
   - The grave key cycles how the groups are laid out: one row of groups, two rows, or one
     row per group.
   - A nine by nine panel over any container screen shows the whole table. Click the tab to
     open it, right-drag to move it, shift-click a cell to take that item off the hotbar,
     and shift-click an item in the Endless Inventory panel to put it on.


Where it is configured
----------------------
`config/endless_refined-server.toml`
    inventory.sweep            move passively gained items from slots 9-35 to Endless
    inventory.overflow         send items that fit nowhere to Endless
    refined_storage.enabled    the portable grid fallback
    refined_storage.ignoreEnergy, hotbar.enabled, hotbar.enderChest, debug.traceNbt

Client settings, including the panel position and the hotbar group count, are in the
mod's own client config, reachable from the Config button in the mod list.


Known limitations
-----------------
- The Endless Inventory panel's own position cannot be moved. Its layout is worked out once
  when it opens, from numbers it does not offer to recompute, so there is no way to move it
  as a whole from outside. The nine by nine panel above can be moved instead.
- A hotbar row shows at most one stack of an item, and refills from Endless Inventory as it
  is drawn down. Rows are refilled while no screen is open, so they do not change under the
  player's hands.
- Item icons in the Endless Inventory screens keep their NBT; this mod patches three points
  in Endless Inventory's network encoding to make that so. With `debug.traceNbt` enabled the
  log says whether it is working.
