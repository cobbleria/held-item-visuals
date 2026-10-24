package dev.helditemvisuals.mc;

import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.battles.model.actor.ActorType;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.api.tags.CobblemonItemTags;
import com.cobblemon.mod.common.battles.BattleRegistry;
import com.cobblemon.mod.common.battles.BattleSide;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import dev.helditemvisuals.HeldItemVisuals;
import dev.helditemvisuals.api.HeldItemVisualsApi;
import dev.helditemvisuals.logic.BattleItemState;
import dev.helditemvisuals.logic.HideMarks;
import dev.helditemvisuals.logic.ModConfig;
import dev.helditemvisuals.logic.PvpRule;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Decides the item a Pokémon entity shows while it is in a battle. Server side, every tick. */
public final class ShownItem {
	private ShownItem() {}

	private static final int NEW_ENTITY_TICKS = 40;

	/**
	 * @param vanilla what Cobblemon would show
	 * @return what to show instead ({@code vanilla} outside battles and when nothing changed)
	 */
	public static ItemStack adjust(PokemonEntity entity, ItemStack vanilla) {
		ModConfig config = HeldItemVisuals.config();
		boolean mayHide = config.hideHeldItemsInPvp || config.hideHeldItemsWhenRequested;
		if (!config.followBattleItemChanges && !mayHide) return vanilla;
		Pokemon pokemon = entity.getPokemon();
		PokemonBattle battle = battleOf(entity, pokemon, mayHide);
		if (battle == null || battle.getEnded()) return vanilla;

		if (mayHide && hides(config, battle)) return ItemStack.EMPTY;
		if (!config.followBattleItemChanges || !pokemon.getHeldItemVisible()) return vanilla;

		BattleItemState state = HeldItemVisuals.OVERRIDES.get(battle.getBattleId());
		if (state == null || !state.hasOverrides()) return vanilla;
		Optional<String> override = state.overrideFor(pokemon.getUuid());
		if (override.isEmpty()) {
			BattlePokemon bp = find(battle, pokemon);
			if (bp != null) override = state.overrideFor(bp.getUuid());
		}
		if (override.isEmpty()) return vanilla;

		ItemStack shown = ItemResolver.stack(battle, override.get());
		// the same rules as Cobblemon's own: hidden items and wild alphas show nothing
		if (shown.is(CobblemonItemTags.HIDDEN_ITEMS) || (pokemon.isAlpha() && pokemon.isWild())) return ItemStack.EMPTY;
		return shown;
	}

	/**
	 * The battle the entity's Pokémon is in. Normally the entity's battle id; for hiding (PvP, marked battles),
	 * during its first two seconds in the world also the battle of its owner (covers a send-out before the entity is
	 * tagged with the battle; limited to new entities because looking a player's battle up walks all battles).
	 */
	private static PokemonBattle battleOf(PokemonEntity entity, Pokemon pokemon, boolean viaOwner) {
		UUID battleId = entity.getBattleId();
		if (battleId != null) return BattleRegistry.getBattle(battleId);
		if (!viaOwner || entity.tickCount > NEW_ENTITY_TICKS) return null;
		UUID owner = entity.getOwnerUUID();
		if (owner == null) return null;
		PokemonBattle battle = BattleRegistry.getBattleByParticipatingPlayerId(owner);
		return battle != null && find(battle, pokemon) != null ? battle : null;
	}

	private static BattlePokemon find(PokemonBattle battle, Pokemon pokemon) {
		for (BattleActor actor : battle.getActors()) {
			for (BattlePokemon bp : actor.getPokemonList()) {
				if (bp.getOriginalPokemon() == pokemon || bp.getEffectedPokemon() == pokemon
					|| bp.getOriginalPokemon().getUuid().equals(pokemon.getUuid())) {
					return bp;
				}
			}
		}
		return null;
	}

	/** PvP or asked for by another mod, as far as the config follows them (the cheap check first). */
	private static boolean hides(ModConfig config, PokemonBattle battle) {
		boolean pvp = config.hideHeldItemsInPvp && isPvp(battle);
		boolean requested = !pvp && config.hideHeldItemsWhenRequested && HeldItemVisualsApi.isRequested(battle);
		return HideMarks.hides(config, pvp, requested);
	}

	static boolean isPvp(PokemonBattle battle) {
		List<List<Boolean>> sides = new ArrayList<>();
		for (BattleSide side : battle.getSides()) {
			List<Boolean> players = new ArrayList<>();
			for (BattleActor actor : side.getActors()) players.add(actor.getType() == ActorType.PLAYER);
			sides.add(players);
		}
		return PvpRule.isPvp(sides);
	}
}
