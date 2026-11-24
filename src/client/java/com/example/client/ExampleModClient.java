package com.example.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import org.lwjgl.glfw.GLFW;

public class ExampleModClient implements ClientModInitializer {
	static boolean fullbright = false;
	static boolean autoSprint = false;

	private static KeyMapping fullbrightKey;
	private static KeyMapping sprintKey;

	@Override
	public void onInitializeClient() {
		fullbrightKey = reg("Fullbright", GLFW.GLFW_KEY_G);
		sprintKey = reg("AutoSprint", GLFW.GLFW_KEY_H);

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			Player p = client.player;
			if (p == null) return;

			while (fullbrightKey.consumeClick()) {
				fullbright = !fullbright;
				msg(p, "Fullbright", fullbright);
				if (!fullbright) p.removeEffect(MobEffects.NIGHT_VISION);
			}
			while (sprintKey.consumeClick()) {
				autoSprint = !autoSprint;
				msg(p, "AutoSprint", autoSprint);
			}

			if (fullbright) {
				p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 1000, 0, false, false, false));
			}

			if (autoSprint && client.options.keyUp.isDown() && !p.isShiftKeyDown()
					&& !p.horizontalCollision && p.getFoodData().getFoodLevel() > 6) {
				p.setSprinting(true);
			}
		});
	}

	private static KeyMapping reg(String name, int key) {
		return KeyBindingHelper.registerKeyBinding(
			new KeyMapping(name, InputConstants.Type.KEYSYM, key, "KediClient"));
	}

	private static void msg(Player p, String name, boolean on) {
		p.displayClientMessage(Component.literal(name + ": " + (on ? "ACIK" : "KAPALI")), true);
	}
}
