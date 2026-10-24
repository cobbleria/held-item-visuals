package dev.helditemvisuals.api;

import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import dev.helditemvisuals.HeldItemVisuals;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

/**
 * For other mods: battles in which the battling Pokémon show <b>no held item at all</b>, the same way PvP battles
 * do (decided on the server, so the item is never sent to anyone's game — neither the opponent, the spectators nor
 * the owner see it). Use it for battles where the items are meant to stay secret, such as a battle facility
 * against NPC trainers.
 *
 * <p>Two ways, both followed only while {@code hideHeldItemsWhenRequested} in {@code config/held-item-visuals.json}
 * is on (the default):
 * <ul>
 *   <li>{@link #hideHeldItems(UUID)}: mark one battle by its id. The mark goes away by itself when the battle
 *       ends. Mark it right after starting the battle (before its first turn); Pokémon sent out before that may
 *       show their item for a moment.</li>
 *   <li>{@link #addHideRule(Predicate)}: a rule asked about every running battle (server thread, every tick for
 *       every battling Pokémon: keep it cheap, never throw).</li>
 * </ul>
 * Visual only: the real held items and the battle are never changed.
 */
public final class HeldItemVisualsApi {
	private HeldItemVisualsApi() {}

	/** Version of this API; goes up when calls are added. 1 = Held Item Visuals 1.1.0. */
	public static final int VERSION = 1;

	private static final List<Predicate<PokemonBattle>> RULES = new CopyOnWriteArrayList<>();

	/** The battle with this id shows no held items until it ends. */
	public static void hideHeldItems(UUID battleId) {
		HeldItemVisuals.HIDDEN.mark(battleId);
	}

	/** Takes back {@link #hideHeldItems(UUID)} (PvP hiding and rules still apply). */
	public static void showHeldItems(UUID battleId) {
		HeldItemVisuals.HIDDEN.unmark(battleId);
	}

	/** Whether {@link #hideHeldItems(UUID)} was called for this running battle. */
	public static boolean isMarked(UUID battleId) {
		return HeldItemVisuals.HIDDEN.isMarked(battleId);
	}

	/** Battles this rule says {@code true} for show no held items. */
	public static void addHideRule(Predicate<PokemonBattle> rule) {
		if (rule != null) RULES.add(rule);
	}

	public static void removeHideRule(Predicate<PokemonBattle> rule) {
		RULES.remove(rule);
	}

	/**
	 * Whether another mod asked to hide the held items in this battle (a mark or a rule; a rule that throws counts
	 * as no). Says nothing about PvP or the config.
	 */
	public static boolean isRequested(PokemonBattle battle) {
		if (battle == null) return false;
		if (HeldItemVisuals.HIDDEN.isMarked(battle.getBattleId())) return true;
		for (Predicate<PokemonBattle> rule : RULES) {
			try {
				if (rule.test(battle)) return true;
			} catch (Throwable t) {
				HeldItemVisuals.LOG.warn("A held item hiding rule failed; ignored for this battle", t);
			}
		}
		return false;
	}
}
