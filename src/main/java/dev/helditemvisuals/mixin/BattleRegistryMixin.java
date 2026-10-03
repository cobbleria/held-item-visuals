package dev.helditemvisuals.mixin;

import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.battles.BattleRegistry;
import dev.helditemvisuals.HeldItemVisuals;
import dev.helditemvisuals.mc.ItemResolver;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Every battle end (win, flee, forfeit, stop) goes through {@code BattleRegistry.closeBattle}. */
@Mixin(value = BattleRegistry.class, remap = false)
abstract class BattleRegistryMixin {
	@Inject(method = "closeBattle", at = @At("HEAD"), require = 0)
	private static void helditemvisuals$closeBattle(PokemonBattle battle, CallbackInfo ci) {
		HeldItemVisuals.OVERRIDES.end(battle.getBattleId());
		ItemResolver.forget(battle.getBattleId());
	}
}
