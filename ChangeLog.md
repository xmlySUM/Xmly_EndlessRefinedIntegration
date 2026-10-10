# ChangeLog

This is the 2.0 line of **Endless Inventory: Refined Integration**. It came about by taking
Endless Inventory's sources and folding the Endless Refined addon into them, so that what used to
be a mod plus an addon is one mod. It is published under the addon's id, `xmly_endless_refined`.

This file records what was merged and what was changed. For the mod's features, see `README.md`;
for a short list of what 2.0 adds, see `changelog.txt`.

---

## Brought in from Endless Inventory

The whole mod, by Kay_Zhang, under the MIT licence kept in `Endless-Inventory_LICENSE.txt`:

| Path | What it is |
|---|---|
| `common/src/main/java/com/kwwsyk/endinv/common/` | The mod itself: the store (`SourceInventory`, `EndlessInventory`, `EndlessInventoryData`), the menu, the pages and their renderers, the screen framework, the network payloads, the config interfaces, the page registry, the auto-pick system, the commands |
| `common/src/main/resources/assets/endless_inventory/` | Item models, GUI textures, translations |
| `forge/src/main/java/com/kwwsyk/endinv/forge/` | The Forge wiring: initialisers, configs, events, the packet handler, the player NBT capabilities, and the JEI, Curios and Cloth Config integrations |
| `build.gradle`, `settings.gradle`, `gradle.properties`, `gradle/`, `gradlew` | The build, with `fabric/` commented out of `settings.gradle` |

Two mixins came with it — `ServerPlaceRecipeMixin` and `RecipeBookComponentMixin` — as did the two
Forge capabilities a player carries its Endless Inventory id and synced config in.

The Fabric and NeoForge modules are not part of this branch; see `.gitignore`.

## Moved in from Endless Refined

The addon was a separate project that depended on the mod above. Its sources now live in the same
modules as the rest:

| Now at | Was |
|---|---|
| `common/…/common/hotbar/`, `common/…/common/client/hotbar/HotbarPanelState` | `hotbar/HotbarTable`, `HotbarGroups`, `HotbarDisplayMode`, `client/HotbarPanelState` |
| `forge/…/forge/hotbar/` | `hotbar/HotbarServerState`, `event/PlayerTickEvents`, `inventory/InventoryTransfer` |
| `forge/…/forge/client/hotbar/` | `hotbar/engine/` and `client/HotbarPanel`, `client/HotbarPanelState` |
| `forge/…/forge/network/payloads/Hotbar*.java`, `ServerSupportPayload` | the addon's eight messages and its own channel |
| `forge/…/forge/mixin/hotbar/` | the addon's three inventory mixins and its two QuadHotbar ones |
| `forge/…/forge/integrates/refinedstorage/`, `forge/…/forge/mixin/refinedstorage/` | `compat/refinedstorage/` and the two Refined Storage mixins |
| `common/…/common/util/UiTrace` | new: the layout diagnostics the addon did not have |

What the addon did is now the mod's own behaviour: items picked up are swept into the inventory,
items that fit nowhere go there instead of the ground, the hotbar reaches into it, and a Refined
Storage grid — portable or a whole network — can draw on it.

## Changed in Endless Inventory's own code

### Items kept what they carry

- `util/ItemKey`, `util/ItemStackLike`, `util/ItemState`, `data/EndInvCodecStrategy` and the
  payloads now carry an item's whole data. Two things were being lost. The tag was dropped on the
  wire, because `FriendlyByteBuf.writeItem` only writes one for an item that can be depleted;
  renamed items and enchanted books arrived as plain items. And Forge keeps an item's capability
  data *beside* its tag rather than inside it, under `ForgeCaps`, which nothing here looked at -
  so anything whose data lives in a capability lost all of it on the way in.
- `util/ItemState.toStack` built stacks with `new ItemStack(item, count, tag)`. On Forge that
  constructor's third argument is the *capability* NBT, not the item's tag, so every stack built
  there came out with no tag at all: the icons showed no enchantment glow and the tooltips showed
  no lines, however well the data had survived the trip.
- One place now reads a serialized stack back (`ItemStackCodec.fromNbt`); the save format writes
  the whole stack rather than three fields by hand.
- `EndInvAffinities` matched bookmarks by stack instead of by key, so an item with a tag could be
  starred but never unstarred.

### The screen and the menu agree with each other

- `menu/EndlessInventoryMenu`: closing the crafter handed the result slot to the player along with
  the ingredients it was made from - a free copy of every recipe left in the grid. The row count
  and the crafter flag are now sent through data slots that wrap the fields; they were written to
  slots nothing read, so the client's page was drawn over the crafter.
- `client/gui/ScreenFramework`, `client/gui/EndlessInventoryScreen`: the page is sized from the
  menu's row count rather than the configured one, the screen notices the crafter being turned on
  by something other than its own button, and the background is rebuilt when the page is resized -
  it had kept the size it was built with.
- `integrate/jei/EIMRecipeTranHandler`: a recipe transfer now tells the server that the crafter is
  showing; it had turned it on for the client alone.

### Clicks do what they say

- `client/gui/page/ItemPage`, `network/payloads/toServer/ItemClickPayload`: the client used to
  decide a click from its own cursor and the server from its own. When the two disagreed - the
  client's cache accepts a larger stack than the server's inventory - a click meant to put
  something in was taken as a request to take something out, and the item under the click vanished.
  The server decides alone now, and the client says whether it had anything on the cursor.
- `EndlessInventory.broadcastChanges` was an empty stub; it is implemented, and sends only what
  changed rather than the whole inventory. Sending the whole thing on every change made moving
  items quickly a stall.
- `client/gui/page/ItemPage`: clicking an empty cell, or the space between cells, now puts what is
  on the cursor into the inventory instead of falling through to the screen behind and dropping it.

### Things that broke a dedicated server, or the build

- `options/SpecifiedMenuAttachingConfig` named a client-only menu class from common code. A
  dedicated server does not have that class, so every packet that opened the panel took it down.
- `forge/ModInitializer` passed `ForgeHooks.onItemStackedOn` its two stacks in the wrong order, so
  listeners were told about the wrong item.
- `build.gradle`, `forge/build.gradle`: the mixin annotation processor's output is kept out of the
  build cache and the reobfuscation mapping merge is ordered after compilation. Without both, a
  clean build produced a jar whose mixin members were never remapped, which fails at run time
  rather than at build time.

### Smaller things

- `Forge/ClientConfig`, `forge/ServerConfig`: options for the features above. The config files keep
  their `endless_inventory-*.toml` names so that existing settings are not abandoned.
- `forge/build.gradle`: the Refined Storage jar is looked for in `forge/libs`, which is ignored.
  A clone that does not have it - including the one CI checks out - builds anyway: the compat
  sources are left out and their entries are dropped from the packaged mixin config to match.
- `common/…/util/UiTrace`: layout diagnostics, off unless the client's `Screen debug` is on or the
  game is started with `-Dendinv.uiTrace=true`.

---

## Addon history

The addon's own history, for reference:

- **1.0.0** — first release: the disk-less portable grid, the inventory sweep and overflow, item
  NBT kept on the way to the client, and the multi-group hotbar forked from QuadHotbar.
- **1.0.1-alpha** — a dedicated server no longer crashed when the panel was opened.
