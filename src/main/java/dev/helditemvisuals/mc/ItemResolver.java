package dev.helditemvisuals.mc;

import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.helditem.CobblemonHeldItemManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Showdown item id → the stack to show, the way Cobblemon maps items to Showdown
 * ({@link CobblemonHeldItemManager#showdownId(ItemStack)}: the item's held item component, registered remaps,
 * then {@code cobblemon:} items by name without underscores).
 *
 * <ol>
 *   <li>a real held item of a Pokémon in the same battle with that id (keeps its components: a Trick partner's
 *       named item stays named),</li>
 *   <li>otherwise the first registered item Cobblemon maps to that id ({@code cobblemon:} items first),</li>
 *   <li>otherwise nothing is shown.</li>
 * </ol>
 * Stacks are remembered per battle, so an item still looks the same after Cobblemon took the original.
 */
public final class ItemResolver {
	private ItemResolver() {}

	private static final Map<UUID, Map<String, ItemStack>> PER_BATTLE = new ConcurrentHashMap<>();
	private static volatile Map<String, Item> byShowdownId;

	/** Remember the stack for {@code id} in this battle now, while its holder still has it. */
	public static void prime(PokemonBattle battle, String id) {
		stack(battle, id);
	}

	/** The stack to show for {@code id} in this battle (never mutate it). */
	public static ItemStack stack(PokemonBattle battle, String id) {
		if (id == null || id.isEmpty()) return ItemStack.EMPTY;
		Map<String, ItemStack> cache = PER_BATTLE.computeIfAbsent(battle.getBattleId(), b -> new ConcurrentHashMap<>());
		ItemStack cached = cache.get(id);
		if (cached != null) return cached;
		ItemStack found = fromBattle(battle, id);
		if (found.isEmpty()) {
			Item item = registry().get(id);
			if (item != null) found = new ItemStack(item);
		}
		cache.put(id, found);
		return found;
	}

	public static void forget(UUID battleId) {
		if (battleId != null) PER_BATTLE.remove(battleId);
	}

	public static void clearAll() {
		PER_BATTLE.clear();
	}

	private static ItemStack fromBattle(PokemonBattle battle, String id) {
		for (BattleActor actor : battle.getActors()) {
			for (BattlePokemon bp : actor.getPokemonList()) {
				for (Pokemon p : new Pokemon[] { bp.getEffectedPokemon(), bp.getOriginalPokemon() }) {
					ItemStack held = p.heldItem();
					if (!held.isEmpty() && id.equals(showdownId(held))) {
						held.setCount(1);
						return held;
					}
				}
			}
		}
		return ItemStack.EMPTY;
	}

	private static String showdownId(ItemStack stack) {
		try {
			return CobblemonHeldItemManager.INSTANCE.showdownId(stack);
		} catch (RuntimeException e) {
			return null;
		}
	}

	private static Map<String, Item> registry() {
		Map<String, Item> map = byShowdownId;
		if (map != null) return map;
		Map<String, Item> built = new HashMap<>();
		for (int pass = 0; pass < 2; pass++) {
			for (Item item : BuiltInRegistries.ITEM) {
				ResourceLocation key = BuiltInRegistries.ITEM.getKey(item);
				if ((pass == 0) != key.getNamespace().equals("cobblemon")) continue;
				String id = showdownId(new ItemStack(item));
				if (id != null && !id.isEmpty()) built.putIfAbsent(id, item);
			}
		}
		byShowdownId = built;
		return built;
	}
}
