package dev.helditemvisuals.logic;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/** The {@link BattleItemState} of every running battle, by battle id. Pure. */
public final class OverrideRegistry {
	private final Map<UUID, BattleItemState> battles = new ConcurrentHashMap<>();

	public BattleItemState getOrCreate(UUID battleId) {
		return battles.computeIfAbsent(battleId, id -> new BattleItemState());
	}

	/** {@code null} when nothing happened in that battle yet. */
	public BattleItemState get(UUID battleId) {
		return battleId == null ? null : battles.get(battleId);
	}

	/** The battle is over: everything goes back to the real held items. */
	public void end(UUID battleId) {
		if (battleId != null) battles.remove(battleId);
	}

	/** Drops every battle that {@code isOver} says has ended (safety net for missed end hooks). */
	public void removeIf(Predicate<UUID> isOver) {
		battles.keySet().removeIf(isOver);
	}

	public void clear() {
		battles.clear();
	}

	public int size() {
		return battles.size();
	}
}
