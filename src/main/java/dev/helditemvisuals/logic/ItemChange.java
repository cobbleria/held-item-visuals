package dev.helditemvisuals.logic;

/**
 * One change to what a battling Pokémon is shown holding.
 *
 * @param who    which Pokémon of the protocol line: the line's own Pokémon ({@link Role#SUBJECT}) or the
 *               one named by its {@code [of]} argument ({@link Role#OF})
 * @param itemId the Showdown id of the item it holds from now on; {@code ""} = holds nothing
 */
public record ItemChange(Role who, String itemId) {
	public enum Role { SUBJECT, OF }

	public static ItemChange lose(Role who) {
		return new ItemChange(who, "");
	}

	public static ItemChange gain(Role who, String itemId) {
		return new ItemChange(who, itemId);
	}

	public boolean isLoss() {
		return itemId.isEmpty();
	}
}
