# Changelog

All notable changes to Held Item Visuals. Versions follow [Semantic Versioning](https://semver.org/).

## [1.1.0] - 2026-10-24

- API for other mods (`dev.helditemvisuals.api.HeldItemVisualsApi`): `hideHeldItems(battleId)` or
  `addHideRule(predicate)` makes a battle show no held items, exactly like a PvP battle (decided on the server,
  never sent; the mark goes away when the battle ends). For battles whose teams are meant to stay secret, such as a
  battle facility against NPC trainers.
- New setting `hideHeldItemsWhenRequested` (on by default): whether those requests are followed. A 1.0 config
  without the key keeps it on.

## [1.0.0] - 2026-10-03

First release, for Cobblemon 1.8.1 on Minecraft 1.21.1 (server side).

- Pokémon in battle visibly lose their held item when the battle takes it (Knock Off, Thief, Covet, Magician,
  Pickpocket, Trick, Switcheroo, Fling, Incinerate, Bug Bite, Pluck, Corrosive Gas, Bestow, eaten berries and other
  used-up items) and show the item they get (Thief, Covet, Magician, Pickpocket, Trick, Switcheroo, Bestow, Recycle,
  Harvest, Pickup, G-Max Replenish, Symbiosis). Sticky Barb moving to the attacker shows at the end of the turn. Frisk
  only reveals and changes nothing. After the battle the real held items show again.
- PvP battles (a player on every side) show no held items on the battling Pokémon; the item never reaches the
  clients.
- Visual only: the real held items and the battle are never changed.
- `config/held-item-visuals.json`: `followBattleItemChanges`, `hideHeldItemsInPvp` (both on by default).
