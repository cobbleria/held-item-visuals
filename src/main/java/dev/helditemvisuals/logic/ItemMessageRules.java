package dev.helditemvisuals.logic;

import java.util.List;
import java.util.Set;

/**
 * Which Showdown protocol lines change who holds which item. Pure: works on the raw protocol strings.
 *
 * <ul>
 *   <li>{@code |-enditem|POKEMON|ITEM|...} always means POKEMON no longer holds ITEM: eaten / used
 *       ({@code [eat]}, gems, Air Balloon, seeds, Red Card ...), knocked off, burnt (Incinerate), eaten by the
 *       attacker (Bug Bite / Pluck: {@code [from] stealeat}), flung, corroded, or the losing side of
 *       Thief / Trick / Switcheroo / Pickpocket ({@code [silent]}).</li>
 *   <li>{@code |-item|POKEMON|ITEM|[from] EFFECT|...} means POKEMON now holds ITEM (Trick, Switcheroo, Thief,
 *       Covet, Magician, Pickpocket, Bestow, Recycle, Harvest, Pickup, G-Max Replenish, ...) — except a pure
 *       reveal: no {@code [from]} at all (Air Balloon announcing itself on switch-in), {@code [identify]}, or
 *       {@code [from] ability: Frisk}.</li>
 *   <li>Covet, Magician and Bestow print only the {@code -item} line of the taker; the Pokémon in {@code [of]}
 *       lost the item without its own {@code -enditem}. Thief and Pickpocket print both lines; handling the
 *       {@code [of]} side again is harmless.</li>
 *   <li>{@code |-activate|SOURCE|ability: Symbiosis|ITEM|[of] POKEMON}: SOURCE passes ITEM to POKEMON.</li>
 * </ul>
 *
 * Sticky Barb changing hands prints nothing at all; it is caught by {@link BattleItemState#reconcile}.
 */
public final class ItemMessageRules {
	private ItemMessageRules() {}

	/** {@code -item} effects whose {@code [of]} Pokémon is the one that lost the item. */
	static final Set<String> TAKEN_FROM_OF = Set.of("thief", "covet", "magician", "pickpocket", "bestow");

	/** {@code -item} effects that only reveal an item that was already held. */
	static final Set<String> REVEALS = Set.of("frisk");

	/** {@code |-enditem|POKEMON|ITEM|...}: POKEMON holds nothing now. */
	public static List<ItemChange> endItem() {
		return List.of(ItemChange.lose(ItemChange.Role.SUBJECT));
	}

	/**
	 * {@code |-item|POKEMON|ITEM|[from] EFFECT|[of] OTHER|[identify]}.
	 *
	 * @param itemName  the ITEM argument (an item name such as {@code "Sitrus Berry"} or an id)
	 * @param from      the {@code [from]} value ({@code "move: Trick"}, {@code "ability: Frisk"}), or {@code null}
	 * @param identify  whether {@code [identify]} is present
	 * @param hasOf     whether {@code [of]} is present
	 */
	public static List<ItemChange> item(String itemName, String from, boolean identify, boolean hasOf) {
		String itemId = ShowdownIds.toId(itemName);
		if (itemId.isEmpty() || from == null || from.isBlank() || identify) return List.of();
		String effect = ShowdownIds.toId(from);
		if (REVEALS.contains(effect)) return List.of();
		if (hasOf && TAKEN_FROM_OF.contains(effect)) {
			return List.of(ItemChange.gain(ItemChange.Role.SUBJECT, itemId), ItemChange.lose(ItemChange.Role.OF));
		}
		return List.of(ItemChange.gain(ItemChange.Role.SUBJECT, itemId));
	}

	/**
	 * {@code |-activate|SOURCE|EFFECT|ARG...|[of] OTHER}. Only Symbiosis moves an item: SOURCE loses it and
	 * OTHER gets it.
	 *
	 * @param effect   the EFFECT argument ({@code "ability: Symbiosis"})
	 * @param itemName the argument after the effect (the item passed on), may be {@code null}
	 */
	public static List<ItemChange> activate(String effect, String itemName, boolean hasOf) {
		if (!ShowdownIds.toId(effect).equals("symbiosis") || !hasOf) return List.of();
		String itemId = ShowdownIds.toId(itemName);
		if (itemId.isEmpty()) return List.of();
		return List.of(ItemChange.lose(ItemChange.Role.SUBJECT), ItemChange.gain(ItemChange.Role.OF, itemId));
	}
}
