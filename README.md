# Held Item Visuals

[English](#english) | [日本語](#日本語)

Server-side Fabric mod for [Cobblemon](https://cobblemon.com/) 1.8 on Minecraft 1.21.1: the item a Pokémon is
shown holding follows what happens to it in battle, and PvP battles can hide held items.

---

## English

In Cobblemon a Pokémon in the world shows its held item. During a battle that item stays on the model even when
the battle took it away: a Knocked Off Leftovers is still in its hand, a stolen Life Orb never shows up on the
thief. This mod makes the model follow the battle:

- **Losing an item** — Knock Off, Thief / Covet / Magician / Pickpocket (the victim), Trick / Switcheroo, Fling,
  Incinerate, Bug Bite / Pluck, Corrosive Gas, Bestow (the giver), eaten berries, used gems, popped Air Balloons,
  seeds, Weakness Policy, Red Card and every other used-up item: the item disappears from the Pokémon.
- **Getting an item** — Thief / Covet / Magician / Pickpocket (the taker), Trick / Switcheroo (both sides),
  Bestow (the receiver), Recycle, Harvest, Pickup, G-Max Replenish, Symbiosis: the new item appears.
- **Sticky Barb** jumping to the attacker (Showdown prints no message for it) shows at the end of that turn.
- **Reveals are not changes:** Frisk and an Air Balloon announcing itself do nothing.
- The change happens when the battle shows the message, not earlier.
- **After the battle** every Pokémon shows its real held item again.
- **PvP:** in a battle where every side has a player (1v1, 2v2, a player with an NPC partner against a player),
  the battling Pokémon show **no held item at all** for the whole battle. This is decided on the server, so the
  item is never sent to anyone's game: it cannot be read from the client.

**Visual only.** The real held items and the battle itself are never changed — what Cobblemon gives back,
consumes or keeps after the battle is exactly the same as without this mod. Items Showdown does not know (a
cosmetic held item) are never hidden by the item tracking. Cobblemon's own rules still apply: a Pokémon whose
owner turned *Show held item* off shows nothing, items in `cobblemon:hidden_items` and wild alphas show nothing.

### Installation

Put the jar in the server's `mods` folder next to Cobblemon 1.8.x and Fabric API. Players do not need it. It also
works in singleplayer / LAN (the integrated server); on a client that joins another server it does nothing.

### Settings

`config/held-item-visuals.json` (created with the defaults on first start, read again at every start and on
`/reload`; missing keys keep their default, a broken file is reported in the log and the defaults are used):

```json
{
  "followBattleItemChanges": true,
  "hideHeldItemsInPvp": true
}
```

| Key | Default | |
|---|---|---|
| `followBattleItemChanges` | `true` | the model follows items lost and gained in battle |
| `hideHeldItemsInPvp` | `true` | battling Pokémon show no held item in player-versus-player battles |

### How it works

- One hook reads every battle instruction (`InstructionSet.execute`): `-enditem`, `-item`, `-activate`
  (Symbiosis) and the per-turn side requests (which list every Pokémon's item, to catch Sticky Barb). The change is
  queued on the battle's own dispatch queue, so it plays in step with the battle messages. Showdown sends the next
  turn's request before the log of the turn that just ended, so requests are only compared at the `|turn|` line.
- One hook changes the value Cobblemon writes into the synced entity data `PokemonEntity.SHOWN_HELD_ITEM` each tick
  (`PokemonServerDelegate.updateShownItem`). Outside battles that value is never touched.
- One hook clears a battle's state when it ends (`BattleRegistry.closeBattle`: wins, fleeing, forfeits, stopped
  battles); a clean-up every 5 seconds removes anything left over.
- Showdown item ids are turned into Minecraft items the way Cobblemon does it (its held item manager); an item
  taken from another Pokémon in the same battle keeps its exact look.
- All hooks are optional: if a Cobblemon update moves things, the server log says which hook did not apply and the
  game runs on without that part.

### Building

```sh
./gradlew build      # Java 21; the jar is build/libs/held-item-visuals-<version>.jar, unit tests included
```

### License

MIT — see [LICENSE](LICENSE).

---

## 日本語

[Cobblemon](https://cobblemon.com/) 1.8（Minecraft 1.21.1）用のサーバー側 Fabric MOD です。バトル中にポケモンが
持っている道具の見た目を、バトルの展開に合わせて変えます。PvP では持ち物を見えなくすることもできます。

Cobblemon では、ワールドにいるポケモンは持ち物を手に持って表示します。ところがバトル中は、道具がなくなっても
表示がそのまま残ります（はたき落とされたたべのこしが手に残る、どろぼうで奪ったいのちのたまが表示されない、など）。
この MOD を入れると、表示がバトルに合わせて変わります。

- **道具がなくなる** — はたきおとす、どろぼう / ほしがる / マジシャン / わるいてぐせ（取られた側）、トリック /
  すりかえ（相手側）、なげつける、やきつくす、むしくい / ついばむ、ふしょくガス、ギフトパス（渡した側）、
  食べたきのみ、使ったジュエル、割れたふうせん、シード、じゃくてんほけん、レッドカードなど使い切った道具：
  ポケモンの手から消えます。
- **道具を手に入れる** — どろぼう / ほしがる / マジシャン / わるいてぐせ（取った側）、トリック / すりかえ（両方）、
  ギフトパス（受け取った側）、リサイクル、しゅうかく、ものひろい、キョダイサイセイ、きょうせい：新しい道具が
  表示されます。
- **くっつきバリ**が攻撃した側に移ったとき（Showdown はメッセージを出しません）は、そのターンの終わりに反映します。
- **見せるだけのものは変化ではありません:** おみとおしや、ふうせんの登場メッセージでは何も変わりません。
- 表示が変わるのは、バトルでそのメッセージが出たタイミングです。
- **バトルが終わると**、どのポケモンも本当の持ち物の表示に戻ります。
- **PvP:** すべての陣営にプレイヤーがいるバトル（1対1、2対2、NPC と組んだプレイヤー対プレイヤー）では、
  バトル中のポケモンは最初から最後まで**持ち物を表示しません**。サーバー側で決めているので、道具の情報は
  誰のゲームにも送られず、クライアント側から読み取ることもできません。

**見た目だけです。** 本当の持ち物やバトルの内容は一切変えません。バトル後に道具が戻る・なくなる・残るかは、
この MOD がないときとまったく同じです。Showdown が知らない道具（見た目用の持ち物など）は、道具の追跡で
隠されることはありません。Cobblemon の元のルールもそのままです（持ち物を表示しない設定のポケモン、
`cobblemon:hidden_items` の道具、野生のオヤブンは何も表示しません）。

### 導入

サーバーの `mods` フォルダに、Cobblemon 1.8.x と Fabric API と一緒に入れてください。プレイヤー側には不要です。
シングルプレイ / LAN（内部サーバー）でも動きます。ほかのサーバーに接続しているクライアントでは何もしません。

### 設定

`config/held-item-visuals.json`（初回起動時に既定値で作られ、起動のたびと `/reload` で読み直します。
書かれていない項目は既定値のまま、壊れたファイルはログに出して既定値で動きます）:

| キー | 既定値 | |
|---|---|---|
| `followBattleItemChanges` | `true` | バトルで道具がなくなったり手に入ったりしたら表示に反映する |
| `hideHeldItemsInPvp` | `true` | プレイヤー同士のバトルでは、バトル中のポケモンの持ち物を表示しない |

### ライセンス

MIT（[LICENSE](LICENSE) を参照）。
