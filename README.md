# Endless Inventory: Refined Integration

**Minecraft 1.20.1 · Forge 47.x**

An RPG-like inventory with no upper bound, plus the things that make it behave like somewhere to
keep items rather than somewhere to look at them: a hotbar that reaches into it, a Refined Storage
network that can draw on it, and the rest of the game treating it as inventory space.

---

## Requirements

| | |
|---|---|
| **Minecraft** | 1.20.1 |
| **Forge** | 47.x |
| Refined Storage | Optional — for the grid and network integration |
| JEI | Optional — for recipe transfer into the crafter |
| Cloth Config | Optional — for a generated settings screen |
| **QuadHotbar (standalone)** | **Incompatible** — the multi-group hotbar is a fork of it and the two patch the same instruction; the hotbar switches itself off rather than failing to start |

---

## Features

### An inventory with no upper bound

Open it with the accessor item or the `I` key. Items are stored by kind rather than by slot, so an
item stacks into the thousands instead of a chest's worth at a time, and the pages are searched,
sorted and categorised rather than scrolled.

### It behaves like inventory space

- **What you pick up goes in.** Items that arrive in the part of your inventory the hotbar is not
  showing are moved into the Endless Inventory instead of filling the slots behind it.
- **Nothing falls on the floor.** When something would not fit anywhere else, it goes here.
- **Items keep what they carry.** Enchantments, custom names, potion contents and Forge capability
  data all survive the trip, both into the store and on the way to the screen.

### A hotbar that reaches into it

The nine slots become up to four groups of nine, selectable across the thirty-six inventory slots.

| Key | Action |
|---|---|
| `Ctrl`+`1`..`9` | Show a page of the Endless Inventory table |
| `Ctrl`+`0` | Swap the hotbar between your inventory and your ender chest |
| Layout key | Cycle how the groups are laid out |
| The 9×9 panel | Shows the whole table; right-drag to move, shift-click a cell to take an item off the hotbar |

Ender chest contents are put back where they belong before a death or a logout, so the swap cannot
cost you them.

### Refined Storage

- **A portable grid with no disk** opens on your Endless Inventory instead of refusing to work.
  A grid with a disk behaves exactly as it did.
- **A whole network can draw on it.** Point an External Storage bus at a Security Manager and the
  network gains the Endless Inventory of the first player that manager gives every permission to.
  The bus is what puts the manager on the network, so nothing else has to be placed.

### A panel you can move

The Endless Inventory panel that appears beside a container screen can be dragged by its frame, and
it comes back where you left it.

### Commands

`/endinv` administers the inventories a world holds: `backup` writes a copy of the data file,
`new` makes one, and `ofIndex` reaches a particular one to open it, set who its owner is, add or
remove someone from its allow-list, change who may open it, or remove it.

### Textures

The panel's texture mode is a client setting. `FromResource` draws it from the vanilla container
texture, `Transparent` draws no background, and `DedicatedLocation` takes one from a resource pack
at `assets/endless_inventory/textures/gui/` - `item_grid.png` for the page, `tabs.png` for the page
tabs, and `item_entry.png` for the row the enchanted-book page uses.

### Configuration

Server behaviour is in `config/endless_inventory-server.toml`: the sweep and overflow switches,
the hotbar, the Refined Storage integration. Client settings — rows, columns, texture, hotbar
groups, panel positions — are in `config/endless_inventory-client.toml`, reachable from the mod
list, from the gear on the panel, or from a key of your choosing in Controls.

---

## Licence and acknowledgements

The mod is distributed under **LGPL-3.0**, because part of it is derived from QuadHotbar. The rest
is offered under MIT as well. `Endless-Inventory_LICENSE.txt`, `lgpl-3.0_LICENSE.txt` and
`THIRD-PARTY.md` set out what applies to what.

- **Endless Inventory** by **Kay_Zhang** — the mod this one is built from; its sources and its
  licence are kept here, and the Endless Inventory itself is his work.
- **QuadHotbar** by **ArchangelD** — the multi-group hotbar is a fork of it, under LGPL-3.0. See
  `THIRD-PARTY.md` for what was changed and why it had to be a fork.
- **Refined Storage** by **Refined Mods** — the storage network and the portable grid.
- **JEI** by **mezz** — the recipe overlay the crafter transfers from.
- **Cloth Config** by **shedaniel** — the generated settings screen.
