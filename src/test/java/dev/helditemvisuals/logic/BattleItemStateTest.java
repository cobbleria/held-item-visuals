package dev.helditemvisuals.logic;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The state a battle goes through, driven like the mod drives it (protocol rules applied to UUIDs). */
class BattleItemStateTest {
	private final UUID a = UUID.randomUUID();
	private final UUID b = UUID.randomUUID();

	/** applies the rule's changes with SUBJECT = subject, OF = of */
	private static void apply(BattleItemState state, List<ItemChange> changes, UUID subject, UUID of) {
		for (ItemChange c : changes) state.set(c.who() == ItemChange.Role.SUBJECT ? subject : of, c.itemId());
	}

	@Test
	void untouchedPokemonShowTheirRealItem() {
		BattleItemState state = new BattleItemState();
		state.reconcile(Map.of(a, "leftovers", b, ""));
		assertFalse(state.hasOverrides());
		assertEquals(Optional.empty(), state.overrideFor(a));
	}

	@Test
	void knockOff() {
		BattleItemState state = new BattleItemState();
		state.reconcile(Map.of(a, "leftovers", b, ""));
		apply(state, ItemMessageRules.endItem(), a, b);
		assertEquals(Optional.of(""), state.overrideFor(a));
		assertEquals(Optional.empty(), state.overrideFor(b));
		// next turn's request agrees: nothing new
		assertTrue(state.reconcile(Map.of(a, "", b, "")).isEmpty());
		assertEquals(Optional.of(""), state.overrideFor(a));
	}

	@Test
	void thiefBothSides() {
		BattleItemState state = new BattleItemState();
		state.reconcile(Map.of(a, "", b, "lifeorb"));
		apply(state, ItemMessageRules.endItem(), b, a); // victim b
		apply(state, ItemMessageRules.item("Life Orb", "move: Thief", false, true), a, b); // thief a
		assertEquals(Optional.of("lifeorb"), state.overrideFor(a));
		assertEquals(Optional.of(""), state.overrideFor(b));
	}

	@Test
	void trickSwap() {
		BattleItemState state = new BattleItemState();
		state.reconcile(Map.of(a, "choicescarf", b, "leftovers"));
		apply(state, ItemMessageRules.item("Choice Scarf", "move: Trick", false, false), b, null);
		apply(state, ItemMessageRules.item("Leftovers", "move: Trick", false, false), a, null);
		assertEquals(Optional.of("leftovers"), state.overrideFor(a));
		assertEquals(Optional.of("choicescarf"), state.overrideFor(b));
		assertTrue(state.reconcile(Map.of(a, "leftovers", b, "choicescarf")).isEmpty());
	}

	@Test
	void flingThenRecycle() {
		BattleItemState state = new BattleItemState();
		state.reconcile(Map.of(a, "ironball"));
		apply(state, ItemMessageRules.endItem(), a, null);
		assertEquals(Optional.of(""), state.overrideFor(a));
		apply(state, ItemMessageRules.item("Iron Ball", "move: Recycle", false, false), a, null);
		assertEquals(Optional.of("ironball"), state.overrideFor(a));
	}

	@Test
	void bugBiteOnABerry() {
		BattleItemState state = new BattleItemState();
		state.reconcile(Map.of(a, "", b, "sitrusberry"));
		// |-enditem|p2a: b|Sitrus Berry|[from] stealeat|[move] Bug Bite|[of] p1a: a
		apply(state, ItemMessageRules.endItem(), b, a);
		assertEquals(Optional.of(""), state.overrideFor(b));
		assertEquals(Optional.empty(), state.overrideFor(a)); // the eater does not hold the berry
	}

	@Test
	void consumedBerry() {
		BattleItemState state = new BattleItemState();
		state.reconcile(Map.of(a, "sitrusberry"));
		apply(state, ItemMessageRules.endItem(), a, null); // [eat]
		assertEquals(Optional.of(""), state.overrideFor(a));
	}

	@Test
	void friskChangesNothing() {
		BattleItemState state = new BattleItemState();
		state.reconcile(Map.of(a, "choicespecs", b, ""));
		apply(state, ItemMessageRules.item("Choice Specs", "ability: Frisk", true, true), a, b);
		assertFalse(state.hasOverrides());
	}

	@Test
	void stickyBarbMovesWithoutAProtocolLineAndIsCaughtByTheNextRequest() {
		BattleItemState state = new BattleItemState();
		state.reconcile(Map.of(a, "stickybarb"));
		state.reconcile(Map.of(b, ""));
		// b makes contact with a: Showdown moves the barb silently; the next requests show it
		assertEquals(List.of(a), state.reconcile(Map.of(a, "")));
		assertEquals(List.of(b), state.reconcile(Map.of(b, "stickybarb")));
		assertEquals(Optional.of(""), state.overrideFor(a));
		assertEquals(Optional.of("stickybarb"), state.overrideFor(b));
	}

	@Test
	void aRequestIsOnlyComparedAtTheTurnLine() {
		BattleItemState state = new BattleItemState();
		state.offerRequest(Map.of(a, "leftovers", b, ""));
		state.flushRequests(); // |turn|1: baseline
		// Showdown sends the next request (a holds nothing) before the log with the Knock Off
		state.offerRequest(Map.of(a, "", b, ""));
		assertFalse(state.hasOverrides()); // nothing vanishes before the message
		apply(state, ItemMessageRules.endItem(), a, b); // the Knock Off message plays
		assertEquals(Optional.of(""), state.overrideFor(a));
		assertTrue(state.flushRequests().isEmpty()); // |turn|2: the request agrees
		assertTrue(state.flushRequests().isEmpty()); // nothing kept twice
	}

	@Test
	void stickyBarbShowsAtTheTurnLine() {
		BattleItemState state = new BattleItemState();
		state.offerRequest(Map.of(a, "stickybarb"));
		state.offerRequest(Map.of(b, "")); // the other side's request
		state.flushRequests();
		state.offerRequest(Map.of(a, ""));
		state.offerRequest(Map.of(b, "stickybarb"));
		assertFalse(state.hasOverrides());
		assertEquals(2, state.flushRequests().size());
		assertEquals(Optional.of(""), state.overrideFor(a));
		assertEquals(Optional.of("stickybarb"), state.overrideFor(b));
	}

	@Test
	void itemsShowdownDoesNotKnowAreNeverTouched() {
		// a cosmetic held item: Showdown sees "" every turn, so no override ever appears
		BattleItemState state = new BattleItemState();
		state.reconcile(Map.of(a, ""));
		state.reconcile(Map.of(a, ""));
		assertFalse(state.hasOverrides());
	}

	@Test
	void battleEndClears() {
		OverrideRegistry registry = new OverrideRegistry();
		UUID battle = UUID.randomUUID();
		UUID other = UUID.randomUUID();
		registry.getOrCreate(battle).set(a, "");
		registry.getOrCreate(other).set(b, "leftovers");
		registry.end(battle);
		assertNull(registry.get(battle));
		assertEquals(1, registry.size());
		registry.removeIf(id -> id.equals(other)); // the periodic clean-up for battles that are gone
		assertEquals(0, registry.size());
		registry.end(null); // harmless
	}
}
