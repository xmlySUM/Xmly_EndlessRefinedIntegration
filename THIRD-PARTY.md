# Third-party code

## QuadHotbar

Files under `src/main/java/com/xmly/endlessrefined/hotbar/engine/` and
`src/main/java/com/xmly/endlessrefined/mixin/hotbar/` are derived from
**QuadHotbar** by ArchangelD (`mod_id: quadhotbar`, originally licensed
**LGPL-3.0**).

Those files remain under LGPL-3.0. Everything else in this repository is under the
license declared in `gradle.properties`.

### Why they were taken rather than depended on

The multi-group hotbar *is* QuadHotbar — its client rendering, its extended
`Inventory.selected` range and the two mixins that lift vanilla's selection clamp.
There is no seam to build on from outside: every class in it is `final`, it exposes
no API, and the row model had to change anyway (see below). The two mods also cannot
coexist at all: both must redirect the same
`Inventory#getSelectionSize()` call inside `ServerGamePacketListenerImpl#handleSetCarriedItem`,
and two `@Redirect`s on one instruction throw `InvalidInjectionException`. So this is
a fork, not an add-on, and `EndlessRefined` refuses to start when the standalone
`quadhotbar` is present.

### What was changed

- Package moved to `com.xmly.endlessrefined.hotbar.engine` / `...mixin.hotbar`;
  `@Mod` entry point, mod id and config registration folded into `EndlessRefined`.
- `getRows()` was split. It used to answer two different questions with one number —
  how many groups can be selected, and how many rows are drawn — which made the
  requested display modes impossible. They are now `selectionGroups()` and
  `renderedRows()`, driven by `HotbarDisplayMode`.
- `KeyMapping.TOGGLE_HOTBARS` (grave accent) became a four-state cycle over
  `HotbarDisplayMode` instead of a two-state toggle.
- The selection-size mixin dropped its duplicate SRG-targeted `@Redirect`. QuadHotbar
  has no refmap, so it needed both a named and an SRG variant to work in both
  environments; this project does have one, so the named form alone is remapped
  correctly.
- The server→client selected-slot message moved onto this mod's own channel rather
  than opening a second one.
