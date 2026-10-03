package dev.helditemvisuals;

import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.battles.BattleRegistry;
import dev.helditemvisuals.logic.ModConfig;
import dev.helditemvisuals.logic.OverrideRegistry;
import dev.helditemvisuals.mc.ItemResolver;
import dev.helditemvisuals.mixin.HookPlugin;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

/**
 * Server side: what battling Pokémon are shown holding follows the battle (Knock Off, Thief, Trick, eaten berries
 * ...), and PvP battles can hide held items. Visual only — the real held items and the battle are never touched.
 */
public final class HeldItemVisuals implements ModInitializer {
	public static final String MOD_ID = "helditemvisuals";
	public static final Logger LOG = LoggerFactory.getLogger("Held Item Visuals");

	/** Overrides of every running battle. */
	public static final OverrideRegistry OVERRIDES = new OverrideRegistry();

	private static volatile ModConfig config = new ModConfig();
	private static int sweepTimer;

	public static ModConfig config() {
		return config;
	}

	@Override
	public void onInitialize() {
		reloadConfig();
		ServerLifecycleEvents.SERVER_STARTING.register(server -> reloadConfig());
		ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resources, success) -> reloadConfig());
		ServerLifecycleEvents.SERVER_STARTED.register(server -> HookPlugin.reportMissingHooks());
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			OVERRIDES.clear();
			ItemResolver.clearAll();
		});
		// safety net: drop the state of battles that ended without passing BattleRegistry.closeBattle
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (++sweepTimer < 100) return;
			sweepTimer = 0;
			OVERRIDES.removeIf(id -> {
				PokemonBattle battle = BattleRegistry.getBattle(id);
				boolean over = battle == null || battle.getEnded();
				if (over) ItemResolver.forget(id);
				return over;
			});
		});
	}

	/** Reads {@code config/held-item-visuals.json} (written with the defaults when missing). */
	public static void reloadConfig() {
		Path file = FabricLoader.getInstance().getConfigDir().resolve("held-item-visuals.json");
		ModConfig.Loaded loaded = ModConfig.load(file);
		if (loaded.problem() != null) LOG.warn(loaded.problem());
		config = loaded.config();
	}
}
