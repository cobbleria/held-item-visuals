package dev.helditemvisuals.mixin;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.entity.pokemon.PokemonServerDelegate;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.helditemvisuals.mc.ShownItem;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/** The item written to {@code PokemonEntity.SHOWN_HELD_ITEM} each tick goes through {@link ShownItem}. */
@Mixin(value = PokemonServerDelegate.class, remap = false)
abstract class PokemonServerDelegateMixin {
	@Shadow
	public abstract PokemonEntity getEntity();

	@WrapOperation(
		method = "updateShownItem",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/network/syncher/SynchedEntityData;set(Lnet/minecraft/network/syncher/EntityDataAccessor;Ljava/lang/Object;)V", remap = true),
		require = 0
	)
	private void helditemvisuals$shownItem(SynchedEntityData data, EntityDataAccessor<Object> key, Object value, Operation<Void> original) {
		Object shown = value;
		if ((Object) key == PokemonEntity.getSHOWN_HELD_ITEM() && value instanceof ItemStack vanilla) {
			try {
				shown = ShownItem.adjust(getEntity(), vanilla);
			} catch (Throwable t) {
				HookPlugin.warnOnce("shown item", t);
			}
		}
		original.call(data, key, shown);
	}
}
