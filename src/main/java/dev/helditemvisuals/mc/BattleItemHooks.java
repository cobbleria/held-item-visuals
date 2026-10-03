package dev.helditemvisuals.mc;

import com.cobblemon.mod.common.api.battles.interpreter.BattleMessage;
import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.battles.dispatch.InterpreterInstruction;
import com.cobblemon.mod.common.battles.interpreter.instructions.ActivateInstruction;
import com.cobblemon.mod.common.battles.interpreter.instructions.EndItemInstruction;
import com.cobblemon.mod.common.battles.interpreter.instructions.ItemInstruction;
import com.cobblemon.mod.common.battles.interpreter.instructions.RequestInstruction;
import com.cobblemon.mod.common.battles.interpreter.instructions.TurnInstruction;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import dev.helditemvisuals.HeldItemVisuals;
import dev.helditemvisuals.logic.BattleItemState;
import dev.helditemvisuals.logic.ItemChange;
import dev.helditemvisuals.logic.ItemMessageRules;
import dev.helditemvisuals.logic.RequestItems;
import kotlin.Unit;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Listens to the battle instructions (one place: {@code InstructionSet.execute}) and updates the overrides. The
 * update is queued on the battle's own dispatch queue right before the instruction, so the item disappears /
 * appears on the Pokémon at the moment the battle shows the message, not when Showdown sent it. Side requests
 * (every Pokémon's item, for changes without a message such as Sticky Barb) are compared at the next
 * {@code |turn|} line, after the turn's messages have played.
 */
public final class BattleItemHooks {
	private BattleItemHooks() {}

	private static boolean warned;

	/** Called for every instruction, before it runs. Never throws. */
	public static void beforeInstruction(InterpreterInstruction instruction, PokemonBattle battle) {
		try {
			if (instruction instanceof EndItemInstruction end) {
				queue(battle, end.getMessage(), ItemMessageRules.endItem());
			} else if (instruction instanceof ItemInstruction item) {
				BattleMessage m = item.getMessage();
				queue(battle, m, ItemMessageRules.item(m.argumentAt(1), m.optionalArgument("from"),
					m.hasOptionalArgument("identify"), m.hasOptionalArgument("of")));
			} else if (instruction instanceof ActivateInstruction activate) {
				BattleMessage m = activate.getMessage();
				queue(battle, m, ItemMessageRules.activate(m.argumentAt(1), m.argumentAt(2), m.hasOptionalArgument("of")));
			} else if (instruction instanceof RequestInstruction request) {
				// the request for the next turn comes before the log of this turn: keep it until |turn|
				Map<UUID, String> items = RequestItems.parse(request.getMessage().getRawMessage());
				if (!items.isEmpty()) HeldItemVisuals.OVERRIDES.getOrCreate(battle.getBattleId()).offerRequest(items);
			} else if (instruction instanceof TurnInstruction) {
				battle.dispatchGo(() -> {
					guarded(() -> flushRequests(battle));
					return Unit.INSTANCE;
				});
			}
		} catch (Throwable t) {
			warnOnce(t);
		}
	}

	private static void queue(PokemonBattle battle, BattleMessage message, List<ItemChange> changes) {
		if (changes.isEmpty()) return;
		battle.dispatchGo(() -> {
			guarded(() -> apply(battle, message, changes));
			return Unit.INSTANCE;
		});
	}

	private static void apply(PokemonBattle battle, BattleMessage message, List<ItemChange> changes) {
		BattleItemState state = HeldItemVisuals.OVERRIDES.getOrCreate(battle.getBattleId());
		for (ItemChange change : changes) {
			BattlePokemon who = change.who() == ItemChange.Role.SUBJECT
				? message.battlePokemon(0, battle)
				: message.battlePokemonFromOptional(battle, "of");
			if (who == null) continue;
			// remember the stack while its holder still has it (Cobblemon may take it right after)
			if (!change.isLoss()) ItemResolver.prime(battle, change.itemId());
			state.set(who.getUuid(), change.itemId());
		}
	}

	private static void flushRequests(PokemonBattle battle) {
		BattleItemState state = HeldItemVisuals.OVERRIDES.get(battle.getBattleId());
		if (state == null) return;
		for (UUID changed : state.flushRequests()) {
			state.overrideFor(changed).filter(id -> !id.isEmpty()).ifPresent(id -> ItemResolver.prime(battle, id));
		}
	}

	private static void guarded(Runnable r) {
		try {
			r.run();
		} catch (Throwable t) {
			warnOnce(t);
		}
	}

	static void warnOnce(Throwable t) {
		if (warned) return;
		warned = true;
		HeldItemVisuals.LOG.warn("Could not follow a held item change in battle; held items are shown as usual where this happens", t);
	}
}
