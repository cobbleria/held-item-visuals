package dev.helditemvisuals.logic;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequestItemsTest {
	@Test
	void readsItemsByUuid() {
		UUID a = UUID.fromString("0b6c5c1e-5f43-4a3b-9a7e-2a0c6c1f4d11");
		UUID b = UUID.fromString("7d0a4f6e-1111-4c22-8d33-0e9f8a7b6c55");
		String raw = "|request|{\"active\":[],\"side\":{\"name\":\"x\",\"id\":\"p1\",\"pokemon\":["
			+ "{\"ident\":\"p1: " + a + "\",\"details\":\"Pikachu, " + a + ", L50, M\",\"item\":\"lightball\"},"
			+ "{\"ident\":\"p1: " + b + "\",\"details\":\"Snorlax, " + b + ", L60\",\"item\":\"\"}]}}";
		Map<UUID, String> items = RequestItems.parse(raw);
		assertEquals(Map.of(a, "lightball", b, ""), items);
	}

	@Test
	void ignoresAnythingElse() {
		assertTrue(RequestItems.parse(null).isEmpty());
		assertTrue(RequestItems.parse("|request|").isEmpty());
		assertTrue(RequestItems.parse("|request|{\"wait\":true}").isEmpty());
		assertTrue(RequestItems.parse("|request|{not json").isEmpty());
	}
}
