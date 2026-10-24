package dev.helditemvisuals.logic;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * {@code config/held-item-visuals.json}. Missing keys keep their defaults; a broken file is reported and the
 * defaults are used (the file is not overwritten). Pure: plain Java and Gson.
 */
public final class ModConfig {
	/** Show what happened to items in battle (Knock Off, Thief, Trick, eaten berries...) on the Pokémon. */
	public boolean followBattleItemChanges = true;
	/** In battles where every side has a player, battling Pokémon show no held item at all. */
	public boolean hideHeldItemsInPvp = true;
	/** Battles other mods mark through the API (e.g. a battle facility) show no held item at all either. */
	public boolean hideHeldItemsWhenRequested = true;

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	/** Result of {@link #load}: the config and a problem to log, if any. */
	public record Loaded(ModConfig config, String problem) {}

	public static Loaded load(Path file) {
		ModConfig config = new ModConfig();
		if (!Files.exists(file)) {
			try {
				Files.createDirectories(file.getParent());
				Files.writeString(file, config.toJson(), StandardCharsets.UTF_8);
				return new Loaded(config, null);
			} catch (IOException e) {
				return new Loaded(config, "could not write " + file + ": " + e.getMessage());
			}
		}
		try {
			return new Loaded(fromJson(Files.readString(file, StandardCharsets.UTF_8)), null);
		} catch (IOException | RuntimeException e) {
			return new Loaded(config, "could not read " + file + ", using the defaults: " + e.getMessage());
		}
	}

	public static ModConfig fromJson(String json) {
		ModConfig config = new ModConfig();
		JsonElement root = JsonParser.parseString(json);
		if (!root.isJsonObject()) throw new IllegalArgumentException("not a JSON object");
		JsonObject o = root.getAsJsonObject();
		config.followBattleItemChanges = bool(o, "followBattleItemChanges", config.followBattleItemChanges);
		config.hideHeldItemsInPvp = bool(o, "hideHeldItemsInPvp", config.hideHeldItemsInPvp);
		config.hideHeldItemsWhenRequested = bool(o, "hideHeldItemsWhenRequested", config.hideHeldItemsWhenRequested);
		return config;
	}

	public String toJson() {
		return GSON.toJson(this) + "\n";
	}

	private static boolean bool(JsonObject o, String key, boolean fallback) {
		JsonElement e = o.get(key);
		if (e == null || !e.isJsonPrimitive() || !e.getAsJsonPrimitive().isBoolean()) return fallback;
		return e.getAsBoolean();
	}
}
