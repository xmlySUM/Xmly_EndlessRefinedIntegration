# Licence of this repository

This repository is mixed. Which licence applies depends on the file:

| Files | Licence |
|---|---|
| Everything not listed below | MIT, as declared in `gradle.properties` (`mod_license`) |
| `forge/src/main/java/com/kwwsyk/endinv/forge/client/hotbar/HotbarEngineClientEvents.java` | LGPL-3.0 |
| `forge/src/main/java/com/kwwsyk/endinv/forge/client/hotbar/HotbarEngineKeyMappings.java` | LGPL-3.0 |
| `forge/src/main/java/com/kwwsyk/endinv/forge/mixin/hotbar/engine/InventoryMixin.java` | LGPL-3.0 |
| `forge/src/main/java/com/kwwsyk/endinv/forge/mixin/hotbar/engine/ServerGamePacketListenerImplMixin.java` | LGPL-3.0 |

Each LGPL-3.0 file carries a notice at the top saying so. Because of them the mod as a whole is
distributed under LGPL-3.0, not MIT alone. Where a download page offers a single licence field,
fill in LGPL-3.0. This does not restrict modpacks: LGPL-3.0 permits being included in and
distributed with one.

The text of LGPL-3.0 must travel with those files. It is in this repository as `lgpl-3.0_LICENSE.txt`
at the top level, and ship that file alongside any distribution of the jar.

---

# Third-party code

## QuadHotbar

The files listed above are derived from **QuadHotbar** by ArchangelD (`mod_id: quadhotbar`,
originally licensed **LGPL-3.0**). They came here through the Endless Refined addon, which forked
QuadHotbar rather than depending on it; that addon has since been merged into this repository,
which now publishes as that addon's 2.0 line.

They remain under LGPL-3.0. Everything else in this repository is under the licence declared in
`gradle.properties`.

### Why they were taken rather than depended on

The multi-group hotbar *is* QuadHotbar: its client rendering, its extended `Inventory.selected`
range, and the two mixins that lift vanilla's selection clamp. There is no seam to build on from
outside - every class in it is `final` and it exposes no API - and the row model had to change
anyway (see below).

The two mods also cannot coexist at all: both must redirect the same
`Inventory#getSelectionSize()` call inside `ServerGamePacketListenerImpl#handleSetCarriedItem`,
and two `@Redirect`s on one instruction throw `InvalidInjectionException`. So this is a fork, not
an add-on. Rather than refuse to start, the two mixins are skipped when the standalone
`quadhotbar` is installed (`EndInvMixinPlugin`) and `HotbarEngine` notices the same thing and
leaves the hotbar alone.

### What was changed

- Package moved to `com.kwwsyk.endinv.forge.client.hotbar` /
  `com.kwwsyk.endinv.forge.mixin.hotbar.engine`; the `@Mod` entry point, mod id and config
  registration were folded into Endless Inventory.
- `getRows()` was split. It used to answer two different questions with one number - how many
  groups can be selected, and how many rows are drawn - which made the requested display modes
  impossible. They are now `selectionGroups()` and `windowCounts()`, driven by `HotbarDisplayMode`.
- `KeyMapping.TOGGLE_HOTBARS` (grave accent) became a seven-state cycle over `HotbarDisplayMode`
  instead of a two-state toggle.
- The selection-size mixin dropped its duplicate SRG-targeted `@Redirect`. QuadHotbar has no
  refmap, so it needed both a named and an SRG variant to work in both environments; this project
  does have one, so the named form alone is remapped correctly.
- The server-to-client selected-slot message moved onto this mod's own channel rather than opening
  a second one.
- The hotbar's config was folded into this mod's client config; the addon's own config screen was
  dropped in favour of the existing one.
