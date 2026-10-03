package dev.helditemvisuals.logic;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Reads the held items out of a Showdown side request ({@code |request|{"side":{"pokemon":[...]}}}). Pure.
 *
 * <p>Cobblemon's Showdown puts each Pokémon's UUID in {@code details} ({@code "Species, <uuid>, L50, M"}); the
 * item is the Showdown id ({@code ""} when it holds nothing).
 */
public final class RequestItems {
	private RequestItems() {}

	private static final String MARKER = "|request|";

	/** Pokémon UUID → Showdown item id. Empty for anything that is not a readable request. */
	public static Map<UUID, String> parse(String raw) {
		Map<UUID, String> out = new LinkedHashMap<>();
		if (raw == null) return out;
		int at = raw.indexOf(MARKER);
		String json = (at >= 0 ? raw.substring(at + MARKER.length()) : raw).trim();
		if (!json.startsWith("{")) return out;
		try {
			JsonObject root = JsonParser.parseString(json).getAsJsonObject();
			JsonObject side = root.getAsJsonObject("side");
			if (side == null) return out;
			JsonArray pokemon = side.getAsJsonArray("pokemon");
			if (pokemon == null) return out;
			for (JsonElement element : pokemon) {
				if (!element.isJsonObject()) continue;
				JsonObject p = element.getAsJsonObject();
				UUID uuid = uuidFromDetails(string(p, "details"));
				if (uuid == null || !p.has("item")) continue;
				out.put(uuid, ShowdownIds.toId(string(p, "item")));
			}
		} catch (RuntimeException ignored) {
			// not JSON / not a request: nothing to learn from it
		}
		return out;
	}

	static UUID uuidFromDetails(String details) {
		if (details == null) return null;
		for (String part : details.split(",")) {
			String s = part.trim();
			if (s.length() == 36 && s.charAt(8) == '-') {
				try {
					return UUID.fromString(s);
				} catch (IllegalArgumentException ignored) {
					// not this part
				}
			}
		}
		return null;
	}

	private static String string(JsonObject o, String key) {
		JsonElement e = o.get(key);
		return e == null || e.isJsonNull() ? null : e.getAsString();
	}
}
