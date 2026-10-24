package dev.helditemvisuals.logic;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HideMarksTest {
	@Test
	void marksLastUntilTheBattleIsGone() {
		HideMarks marks = new HideMarks();
		UUID a = UUID.randomUUID(), b = UUID.randomUUID();
		marks.mark(a);
		marks.mark(b);
		marks.mark(null);
		assertTrue(marks.isMarked(a));
		assertFalse(marks.isMarked(UUID.randomUUID()));
		assertFalse(marks.isMarked(null));
		marks.unmark(a);
		assertFalse(marks.isMarked(a));
		marks.removeIf(b::equals); // the sweep: b ended
		assertEquals(0, marks.size());
	}

	@Test
	void pvpAndRequestedBattlesFollowTheirOwnSwitch() {
		ModConfig on = new ModConfig();
		assertTrue(HideMarks.hides(on, true, false)); // PvP
		assertTrue(HideMarks.hides(on, false, true)); // a battle facility marked it
		assertFalse(HideMarks.hides(on, false, false)); // wild, NPC trainers

		ModConfig noPvp = ModConfig.fromJson("{\"hideHeldItemsInPvp\": false}");
		assertFalse(HideMarks.hides(noPvp, true, false));
		assertTrue(HideMarks.hides(noPvp, false, true));

		ModConfig noRequests = ModConfig.fromJson("{\"hideHeldItemsWhenRequested\": false}");
		assertTrue(HideMarks.hides(noRequests, true, false));
		assertFalse(HideMarks.hides(noRequests, false, true));
	}
}
