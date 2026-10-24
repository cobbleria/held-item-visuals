package dev.helditemvisuals.logic;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/** Battles another mod asked to show no held items in (by battle id), until they end. Pure, thread safe. */
public final class HideMarks {
	private final Set<UUID> battles = ConcurrentHashMap.newKeySet();

	public void mark(UUID battleId) {
		if (battleId != null) battles.add(battleId);
	}

	public void unmark(UUID battleId) {
		if (battleId != null) battles.remove(battleId);
	}

	public boolean isMarked(UUID battleId) {
		return battleId != null && battles.contains(battleId);
	}

	/** Drops every battle that {@code isOver} says has ended (safety net for missed end hooks). */
	public void removeIf(Predicate<UUID> isOver) {
		battles.removeIf(isOver);
	}

	public void clear() {
		battles.clear();
	}

	public int size() {
		return battles.size();
	}

	/**
	 * Whether a battle shows no held items at all.
	 *
	 * @param pvp every side of the battle has a player
	 * @param requested another mod asked for it (a marked battle or one of its rules)
	 */
	public static boolean hides(ModConfig config, boolean pvp, boolean requested) {
		return (config.hideHeldItemsInPvp && pvp) || (config.hideHeldItemsWhenRequested && requested);
	}
}
