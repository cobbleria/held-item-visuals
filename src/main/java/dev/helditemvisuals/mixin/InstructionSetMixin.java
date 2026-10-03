package dev.helditemvisuals.mixin;

import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.battles.dispatch.InstructionSet;
import com.cobblemon.mod.common.battles.dispatch.InterpreterInstruction;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.helditemvisuals.mc.BattleItemHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Every battle instruction passes here: the one place the item lines and side requests are read. */
@Mixin(value = InstructionSet.class, remap = false)
abstract class InstructionSetMixin {
	@WrapOperation(
		method = "execute",
		at = @At(value = "INVOKE", target = "Lcom/cobblemon/mod/common/battles/dispatch/InterpreterInstruction;invoke(Lcom/cobblemon/mod/common/api/battles/model/PokemonBattle;)V"),
		require = 0
	)
	private void helditemvisuals$beforeInstruction(InterpreterInstruction instruction, PokemonBattle battle, Operation<Void> original) {
		BattleItemHooks.beforeInstruction(instruction, battle);
		original.call(instruction, battle);
	}
}
