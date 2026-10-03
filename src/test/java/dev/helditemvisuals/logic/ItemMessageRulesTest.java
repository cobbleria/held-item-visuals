package dev.helditemvisuals.logic;

import org.junit.jupiter.api.Test;

import java.util.List;

import static dev.helditemvisuals.logic.ItemChange.Role.OF;
import static dev.helditemvisuals.logic.ItemChange.Role.SUBJECT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The protocol lines below are the ones Cobblemon's bundled Showdown prints (data/moves.js, data/items.js,
 * data/abilities.js, sim/pokemon.js).
 */
class ItemMessageRulesTest {
	private static ItemChange lose(ItemChange.Role who) {
		return ItemChange.lose(who);
	}

	private static ItemChange gain(ItemChange.Role who, String id) {
		return ItemChange.gain(who, id);
	}

	@Test
	void knockOffRemovesTheTargetsItem() {
		// |-enditem|p2a: X|Leftovers|[from] move: Knock Off|[of] p1a: Y
		assertEquals(List.of(lose(SUBJECT)), ItemMessageRules.endItem());
	}

	@Test
	void thiefVictimLosesAndThiefGains() {
		// |-enditem|p2a: target|Life Orb|[silent]|[from] move: Thief|[of] p1a: source
		assertEquals(List.of(lose(SUBJECT)), ItemMessageRules.endItem());
		// |-item|p1a: source|Life Orb|[from] move: Thief|[of] p2a: target
		assertEquals(List.of(gain(SUBJECT, "lifeorb"), lose(OF)),
			ItemMessageRules.item("Life Orb", "move: Thief", false, true));
	}

	@Test
	void covetMagicianAndBestowTakeFromTheOfPokemonWithoutAnEnditem() {
		assertEquals(List.of(gain(SUBJECT, "sitrusberry"), lose(OF)),
			ItemMessageRules.item("Sitrus Berry", "move: Covet", false, true));
		assertEquals(List.of(gain(SUBJECT, "choiceband"), lose(OF)),
			ItemMessageRules.item("Choice Band", "ability: Magician", false, true));
		// |-item|p2a: target|Leftovers|[from] move: Bestow|[of] p1a: source -> the target gets it, the source loses it
		assertEquals(List.of(gain(SUBJECT, "leftovers"), lose(OF)),
			ItemMessageRules.item("Leftovers", "move: Bestow", false, true));
		// Pickpocket: |-item|p2a: holder|Item|[from] ability: Pickpocket|[of] p1a: attacker
		assertEquals(List.of(gain(SUBJECT, "rockyhelmet"), lose(OF)),
			ItemMessageRules.item("Rocky Helmet", "ability: Pickpocket", false, true));
	}

	@Test
	void trickSwapGivesEachSideTheOthersItem() {
		// |-item|p2a: target|Choice Scarf|[from] move: Trick
		assertEquals(List.of(gain(SUBJECT, "choicescarf")), ItemMessageRules.item("Choice Scarf", "move: Trick", false, false));
		// |-item|p1a: source|Flame Orb|[from] move: Trick
		assertEquals(List.of(gain(SUBJECT, "flameorb")), ItemMessageRules.item("Flame Orb", "move: Trick", false, false));
		assertEquals(List.of(gain(SUBJECT, "lifeorb")), ItemMessageRules.item("Life Orb", "move: Switcheroo", false, false));
	}

	@Test
	void trickWithAnEmptyHandedSideEndsTheOtherItem() {
		// |-enditem|p2a: target|Leftovers|[silent]|[from] move: Trick  (the user held nothing)
		assertEquals(List.of(lose(SUBJECT)), ItemMessageRules.endItem());
	}

	@Test
	void flingIncinerateBugBiteAndConsumedBerriesEndTheItem() {
		// |-enditem|p1a: X|Iron Ball|[from] move: Fling
		// |-enditem|p2a: X|Sitrus Berry|[from] move: Incinerate
		// |-enditem|p2a: target|Sitrus Berry|[from] stealeat|[move] Bug Bite|[of] p1a: source
		// |-enditem|p1a: X|Sitrus Berry|[eat]
		// |-enditem|p1a: X|Occa Berry|[weaken]
		// |-enditem|p1a: X|Normal Gem|[from] gem
		// |-enditem|p1a: X|Air Balloon
		assertEquals(List.of(lose(SUBJECT)), ItemMessageRules.endItem());
	}

	@Test
	void bugBiteDoesNotGiveTheBerryToTheEater() {
		// the eater gets the berry's effect, not the berry: no -item line follows, so nothing is gained
		assertTrue(ItemMessageRules.item("Sitrus Berry", null, false, false).isEmpty());
	}

	@Test
	void recycleHarvestPickupAndReplenishGiveTheItemBack() {
		assertEquals(List.of(gain(SUBJECT, "sitrusberry")), ItemMessageRules.item("Sitrus Berry", "move: Recycle", false, false));
		assertEquals(List.of(gain(SUBJECT, "lumberry")), ItemMessageRules.item("Lum Berry", "ability: Harvest", false, false));
		assertEquals(List.of(gain(SUBJECT, "whiteherb")), ItemMessageRules.item("White Herb", "ability: Pickup", false, false));
		assertEquals(List.of(gain(SUBJECT, "sitrusberry")), ItemMessageRules.item("Sitrus Berry", "move: G-Max Replenish", false, false));
	}

	@Test
	void friskOnlyRevealsAnItem() {
		// |-item|p2a: foe|Choice Specs|[from] ability: Frisk|[of] p1a: frisker|[identify]
		assertTrue(ItemMessageRules.item("Choice Specs", "ability: Frisk", true, true).isEmpty());
		// also without [identify] (older generations)
		assertTrue(ItemMessageRules.item("Choice Specs", "ability: Frisk", false, true).isEmpty());
	}

	@Test
	void airBalloonAnnouncementOnSwitchInIsAReveal() {
		// |-item|p1a: X|Air Balloon
		assertTrue(ItemMessageRules.item("Air Balloon", null, false, false).isEmpty());
	}

	@Test
	void symbiosisPassesTheItemToTheAlly() {
		// |-activate|p1a: source|ability: Symbiosis|Sitrus Berry|[of] p1b: ally
		assertEquals(List.of(lose(SUBJECT), gain(OF, "sitrusberry")),
			ItemMessageRules.activate("ability: Symbiosis", "Sitrus Berry", true));
		assertTrue(ItemMessageRules.activate("move: Protect", null, false).isEmpty());
		assertTrue(ItemMessageRules.activate("ability: Symbiosis", "Sitrus Berry", false).isEmpty());
	}

	@Test
	void ids() {
		assertEquals("knockoff", ShowdownIds.toId("move: Knock Off"));
		assertEquals("kingsrock", ShowdownIds.toId("King's Rock"));
		assertEquals("sitrusberry", ShowdownIds.toId("item: Sitrus Berry"));
		assertEquals("", ShowdownIds.toId(null));
	}
}
