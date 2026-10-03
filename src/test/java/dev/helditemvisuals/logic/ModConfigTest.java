package dev.helditemvisuals.logic;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModConfigTest {
	@Test
	void writesDefaultsWhenMissing(@TempDir Path dir) throws Exception {
		Path file = dir.resolve("config/held-item-visuals.json");
		ModConfig.Loaded loaded = ModConfig.load(file);
		assertNull(loaded.problem());
		assertTrue(loaded.config().followBattleItemChanges);
		assertTrue(loaded.config().hideHeldItemsInPvp);
		String written = Files.readString(file);
		assertTrue(written.contains("\"followBattleItemChanges\": true"));
		assertTrue(written.contains("\"hideHeldItemsInPvp\": true"));
	}

	@Test
	void missingKeysKeepTheirDefaults() {
		ModConfig config = ModConfig.fromJson("{\"hideHeldItemsInPvp\": false}");
		assertTrue(config.followBattleItemChanges);
		assertFalse(config.hideHeldItemsInPvp);
		assertTrue(ModConfig.fromJson("{\"followBattleItemChanges\": \"yes\"}").followBattleItemChanges);
	}

	@Test
	void brokenFileUsesDefaultsAndIsKept(@TempDir Path dir) throws Exception {
		Path file = dir.resolve("held-item-visuals.json");
		Files.writeString(file, "{ oops");
		ModConfig.Loaded loaded = ModConfig.load(file);
		assertNotNull(loaded.problem());
		assertTrue(loaded.config().hideHeldItemsInPvp);
		assertEquals("{ oops", Files.readString(file));
	}
}
