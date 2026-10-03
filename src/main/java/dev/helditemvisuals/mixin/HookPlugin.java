package dev.helditemvisuals.mixin;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Notes which hooks really landed in Cobblemon's code (all of them are optional, {@code require = 0}), so a
 * Cobblemon update that moves things around gives one clear warning instead of a crash.
 *
 * <p>{@code @WrapOperation} (MixinExtras) is applied after the config plugin's {@code postApply}, so the class
 * nodes are kept and looked at once the server has started (they are the nodes the finished classes were written
 * from), then dropped.
 */
public final class HookPlugin implements IMixinConfigPlugin {
	private static final Logger LOG = LoggerFactory.getLogger("Held Item Visuals");

	/** handler name → [target class, what it does] */
	private static final Map<String, String[]> HOOKS = new LinkedHashMap<>();
	static {
		HOOKS.put("helditemvisuals$beforeInstruction", new String[] {
			"com.cobblemon.mod.common.battles.dispatch.InstructionSet", "following item changes in battle" });
		HOOKS.put("helditemvisuals$shownItem", new String[] {
			"com.cobblemon.mod.common.entity.pokemon.PokemonServerDelegate", "changing the item shown on Pokémon" });
		HOOKS.put("helditemvisuals$closeBattle", new String[] {
			"com.cobblemon.mod.common.battles.BattleRegistry", "clearing at battle end (a periodic clean-up still runs)" });
	}

	/** target class name → its class node as Mixin transformed it */
	private static final Map<String, ClassNode> TRANSFORMED = new ConcurrentHashMap<>();
	private static final Set<String> WARNED = ConcurrentHashMap.newKeySet();

	/** Loads the hooked classes (which applies the mixins) and warns once about every hook that did not apply. */
	public static void reportMissingHooks() {
		int applied = 0;
		for (Map.Entry<String, String[]> hook : HOOKS.entrySet()) {
			String target = hook.getValue()[0];
			try {
				Class.forName(target, true, HookPlugin.class.getClassLoader());
			} catch (Throwable ignored) {
				// reported below
			}
			ClassNode node = TRANSFORMED.get(target);
			if (node != null && calls(node, hook.getKey())) {
				applied++;
			} else {
				LOG.warn("Hook into {} did not apply (Cobblemon version changed?): {} is off", target, hook.getValue()[1]);
			}
		}
		TRANSFORMED.clear();
		if (applied == HOOKS.size()) LOG.info("Held Item Visuals: all {} hooks applied", applied);
	}

	public static void warnOnce(String what, Throwable t) {
		if (WARNED.add(what)) LOG.warn("Held Item Visuals: {} failed; the usual item is shown", what, t);
	}

	@Override
	public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
		TRANSFORMED.put(targetClassName.replace('/', '.'), targetClass);
	}

	/** Some method of the class (other than the handler itself) calls the handler: the injection is in place. */
	private static boolean calls(ClassNode node, String handler) {
		for (MethodNode method : node.methods) {
			if (method.name.contains(handler)) continue;
			for (AbstractInsnNode insn : method.instructions) {
				if (insn instanceof MethodInsnNode call && call.name.contains(handler)) return true;
			}
		}
		return false;
	}

	@Override public void onLoad(String mixinPackage) {}
	@Override public String getRefMapperConfig() { return null; }
	@Override public boolean shouldApplyMixin(String targetClassName, String mixinClassName) { return true; }
	@Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}
	@Override public List<String> getMixins() { return null; }
	@Override public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}
