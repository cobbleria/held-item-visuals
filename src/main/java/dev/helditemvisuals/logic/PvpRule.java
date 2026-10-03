package dev.helditemvisuals.logic;

import java.util.List;

/** Is a battle player versus player? Pure. */
public final class PvpRule {
	private PvpRule() {}

	/**
	 * @param sides for each side of the battle, one flag per actor: {@code true} = a player
	 * @return {@code true} when there are at least two sides and every side has a player on it (1v1, 2v2,
	 *         players plus NPC partners against players). Players teaming up against NPCs or wild Pokémon are
	 *         not PvP.
	 */
	public static boolean isPvp(List<List<Boolean>> sides) {
		if (sides == null || sides.size() < 2) return false;
		for (List<Boolean> side : sides) {
			if (side == null || !side.contains(Boolean.TRUE)) return false;
		}
		return true;
	}
}
