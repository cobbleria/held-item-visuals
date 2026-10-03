package dev.helditemvisuals.logic;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PvpRuleTest {
	@Test
	void playerAgainstPlayer() {
		assertTrue(PvpRule.isPvp(List.of(List.of(true), List.of(true))));
		assertTrue(PvpRule.isPvp(List.of(List.of(true, true), List.of(true, true)))); // 2v2 multi
		assertTrue(PvpRule.isPvp(List.of(List.of(true, false), List.of(true)))); // player + NPC partner vs player
	}

	@Test
	void notPvp() {
		assertFalse(PvpRule.isPvp(List.of(List.of(true), List.of(false)))); // wild / NPC
		assertFalse(PvpRule.isPvp(List.of(List.of(true, true), List.of(false)))); // two players vs an NPC
		assertFalse(PvpRule.isPvp(List.of(List.of(true))));
		assertFalse(PvpRule.isPvp(List.of()));
		assertFalse(PvpRule.isPvp(null));
	}
}
