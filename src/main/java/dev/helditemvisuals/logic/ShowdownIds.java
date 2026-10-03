package dev.helditemvisuals.logic;

import java.util.Locale;

/** Showdown naming helpers. Pure: no Minecraft or Cobblemon classes. */
public final class ShowdownIds {
	private ShowdownIds() {}

	/**
	 * Showdown's {@code toID}: lower case, only {@code a-z} and {@code 0-9}. A leading effect type
	 * ({@code "move: "}, {@code "ability: "}, {@code "item: "}) is dropped first, so
	 * {@code "move: Knock Off"} and {@code "Knock Off"} both become {@code "knockoff"}.
	 * {@code null} becomes {@code ""}.
	 */
	public static String toId(String text) {
		if (text == null) return "";
		String s = text.trim();
		int colon = s.indexOf(':');
		if (colon >= 0) {
			String prefix = s.substring(0, colon).trim().toLowerCase(Locale.ROOT);
			if (prefix.equals("move") || prefix.equals("ability") || prefix.equals("item")) {
				s = s.substring(colon + 1);
			}
		}
		StringBuilder out = new StringBuilder(s.length());
		for (int i = 0; i < s.length(); i++) {
			char c = Character.toLowerCase(s.charAt(i));
			if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) out.append(c);
		}
		return out.toString();
	}
}
