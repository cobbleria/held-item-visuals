package dev.helditemvisuals.logic;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * What the Pokémon of one battle are shown holding. Pure: Pokémon are battle UUIDs, items are Showdown ids
 * ({@code ""} = nothing).
 *
 * <p>Two maps:
 * <ul>
 *   <li><b>override</b>: only Pokémon whose item changed in this battle. Pokémon without an entry keep showing
 *       their real held item (Cobblemon's own logic), so items Showdown does not know (cosmetic held items) are
 *       never touched.</li>
 *   <li><b>known</b>: the item Showdown says each Pokémon holds, from the side requests (every turn) and from the
 *       item lines. A request that disagrees with it means the item moved without a protocol line (Sticky Barb)
 *       and becomes an override too.</li>
 * </ul>
 *
 * <p>Showdown sends the request for the next turn <i>before</i> the log of the turn that just ended, so a request
 * is only {@linkplain #offerRequest kept} and {@linkplain #flushRequests compared} when that log has played (at
 * the {@code |turn|} line). Otherwise an item would vanish before the message that takes it.
 */
public final class BattleItemState {
	private final Map<UUID, String> override = new ConcurrentHashMap<>();
	private final Map<UUID, String> known = new ConcurrentHashMap<>();

	/** The Pokémon now holds {@code itemId} ({@code ""} = nothing). */
	public void set(UUID pokemon, String itemId) {
		String id = itemId == null ? "" : itemId;
		override.put(pokemon, id);
		known.put(pokemon, id);
	}

	/** The item to show instead of the real one, or empty when the real one is shown. */
	public Optional<String> overrideFor(UUID pokemon) {
		return Optional.ofNullable(override.get(pokemon));
	}

	private final Map<UUID, String> pending = new ConcurrentHashMap<>();

	/** A side request arrived: keep its items until the turn's log has played. */
	public void offerRequest(Map<UUID, String> items) {
		pending.putAll(items);
	}

	/**
	 * The turn's log has played: compare the kept request items with what is known.
	 *
	 * @return the Pokémon whose shown item changed
	 */
	public List<UUID> flushRequests() {
		if (pending.isEmpty()) return List.of();
		Map<UUID, String> items = Map.copyOf(pending);
		pending.keySet().removeAll(items.keySet());
		return reconcile(items);
	}

	public boolean hasOverrides() {
		return !override.isEmpty();
	}

	/**
	 * A side request listed {@code items} (Pokémon → Showdown item id) for its Pokémon. The first time a Pokémon
	 * is seen its item is only remembered; later a different item than the remembered one becomes an override.
	 *
	 * @return the Pokémon whose shown item changed
	 */
	public List<UUID> reconcile(Map<UUID, String> items) {
		List<UUID> changed = new ArrayList<>();
		for (Map.Entry<UUID, String> e : items.entrySet()) {
			String now = e.getValue() == null ? "" : e.getValue();
			String before = known.putIfAbsent(e.getKey(), now);
			if (before != null && !before.equals(now)) {
				set(e.getKey(), now);
				changed.add(e.getKey());
			}
		}
		return changed;
	}
}
