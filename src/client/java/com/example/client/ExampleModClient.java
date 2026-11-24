package com.example.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ExampleModClient implements ClientModInitializer {
	static boolean fullbright = false;
	static boolean autoSprint = false;
	static boolean triggerBot = false;
	static boolean triggerPlayersOnly = true;
	static boolean aimAssist = false;
	static boolean aimOnlyWhileAttacking = true;
	static float aimRange = 4.5f;
	static float aimFov = 60f;
	static float aimSmooth = 6f;

	static KeyMapping fullbrightKey;
	static KeyMapping sprintKey;
	static KeyMapping triggerKey;
	static KeyMapping aimKey;
	static KeyMapping menuKey;

	@Override
	public void onInitializeClient() {
		fullbrightKey = reg("Fullbright", GLFW.GLFW_KEY_G);
		sprintKey = reg("AutoSprint", GLFW.GLFW_KEY_H);
		triggerKey = reg("TriggerBot", GLFW.GLFW_KEY_R);
		aimKey = reg("AimAssist", GLFW.GLFW_KEY_V);
		menuKey = reg("Menu", GLFW.GLFW_KEY_RIGHT_SHIFT);

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			Player p = client.player;
			if (p == null || client.level == null) return;

			while (menuKey.consumeClick()) {
				if (client.screen == null) client.setScreen(new ConfigScreen());
			}
			while (fullbrightKey.consumeClick()) {
				fullbright = !fullbright;
				msg(p, "Fullbright", fullbright);
				if (!fullbright) p.removeEffect(MobEffects.NIGHT_VISION);
			}
			while (sprintKey.consumeClick()) {
				autoSprint = !autoSprint;
				msg(p, "AutoSprint", autoSprint);
			}
			while (triggerKey.consumeClick()) {
				triggerBot = !triggerBot;
				msg(p, "TriggerBot", triggerBot);
			}
			while (aimKey.consumeClick()) {
				aimAssist = !aimAssist;
				msg(p, "AimAssist", aimAssist);
			}

			if (fullbright) {
				p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 1000, 0, false, false, false));
			}

			if (autoSprint && client.options.keyUp.isDown() && !p.isShiftKeyDown()
					&& !p.horizontalCollision && p.getFoodData().getFoodLevel() > 6) {
				p.setSprinting(true);
			}

			if (client.screen != null) return;

			if (triggerBot && client.hitResult instanceof EntityHitResult hit) {
				Entity e = hit.getEntity();
				boolean ok = e instanceof LivingEntity && e.isAlive()
						&& (!triggerPlayersOnly || e instanceof Player);
				if (ok && p.getAttackStrengthScale(0.5f) >= 1.0f) {
					client.gameMode.attack(p, e);
					p.swing(InteractionHand.MAIN_HAND);
				}
			}

			if (aimAssist && (!aimOnlyWhileAttacking || client.options.keyAttack.isDown())) {
				runAimAssist(client, p);
			}
		});
	}

	private static void runAimAssist(Minecraft client, Player p) {
		float bestDiff = aimFov;
		float[] bestRot = null;
		for (Player other : client.level.players()) {
			if (other == p || other.isSpectator() || !other.isAlive()) continue;
			if (p.distanceTo(other) > aimRange) continue;
			Vec3 target = other.position().add(0, other.getBbHeight() * 0.75, 0);
			float[] rot = rotationTo(p, target);
			float dy = Mth.wrapDegrees(rot[0] - p.getYRot());
			float dp = rot[1] - p.getXRot();
			float diff = (float) Math.sqrt(dy * dy + dp * dp);
			if (diff < bestDiff) {
				bestDiff = diff;
				bestRot = rot;
			}
		}
		if (bestRot == null) return;
		float smooth = Math.max(1f, aimSmooth);
		float newYaw = p.getYRot() + Mth.wrapDegrees(bestRot[0] - p.getYRot()) / smooth;
		float newPitch = Mth.clamp(p.getXRot() + (bestRot[1] - p.getXRot()) / smooth, -90f, 90f);
		p.setYRot(newYaw);
		p.setXRot(newPitch);
	}

	private static float[] rotationTo(Player p, Vec3 t) {
		Vec3 eye = p.getEyePosition();
		double dx = t.x - eye.x;
		double dy = t.y - eye.y;
		double dz = t.z - eye.z;
		double horiz = Math.sqrt(dx * dx + dz * dz);
		float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
		float pitch = (float) (-Math.toDegrees(Math.atan2(dy, horiz)));
		return new float[] { yaw, pitch };
	}

	private static KeyMapping reg(String name, int key) {
		return KeyBindingHelper.registerKeyBinding(
			new KeyMapping(name, InputConstants.Type.KEYSYM, key, "KediClient"));
	}

	private static void msg(Player p, String name, boolean on) {
		p.displayClientMessage(Component.literal(name + ": " + (on ? "ACIK" : "KAPALI")), true);
	}

	// ================= AYAR MENUSU =================
	public static class ConfigScreen extends Screen {
		private final List<Button> keyButtons = new ArrayList<>();
		private final List<KeyMapping> keyMaps = new ArrayList<>();
		private final List<String> keyPrefixes = new ArrayList<>();
		private KeyMapping listening = null;

		public ConfigScreen() {
			super(Component.literal("KediClient"));
		}

		@Override
		protected void init() {
			keyButtons.clear();
			keyMaps.clear();
			keyPrefixes.clear();
			int x = this.width / 2 - 155;
			int kx = x + 205;
			int y0 = 5;
			int step = 21;

			addRenderableWidget(toggle(x, y0, "Fullbright",
				() -> fullbright, v -> fullbright = v));
			addRenderableWidget(toggle(x, y0 + step, "AutoSprint",
				() -> autoSprint, v -> autoSprint = v));
			addRenderableWidget(toggle(x, y0 + step * 2, "TriggerBot",
				() -> triggerBot, v -> triggerBot = v));
			addRenderableWidget(toggle(x, y0 + step * 3, "Sadece oyuncu",
				() -> triggerPlayersOnly, v -> triggerPlayersOnly = v));
			addRenderableWidget(toggle(x, y0 + step * 4, "AimAssist",
				() -> aimAssist, v -> aimAssist = v));
			addRenderableWidget(toggle(x, y0 + step * 5, "Sadece vururken aim",
				() -> aimOnlyWhileAttacking, v -> aimOnlyWhileAttacking = v));
			addRenderableWidget(new Slider(x, y0 + step * 6, "Menzil", 3f, 8f,
				aimRange, v -> aimRange = v));
			addRenderableWidget(new Slider(x, y0 + step * 7, "Gorus acisi", 10f, 120f,
				aimFov, v -> aimFov = v));
			addRenderableWidget(new Slider(x, y0 + step * 8, "Yumusaklik", 1f, 20f,
				aimSmooth, v -> aimSmooth = v));

			addKey(kx, y0, "Tus", fullbrightKey);
			addKey(kx, y0 + step, "Tus", sprintKey);
			addKey(kx, y0 + step * 2, "Tus", triggerKey);
			addKey(kx, y0 + step * 4, "Tus", aimKey);
			addKey(kx, y0 + step * 5, "Menu", menuKey);

			addRenderableWidget(Button.builder(Component.literal("Kapat"), b -> onClose())
				.bounds(kx, y0 + step * 8, 110, 20).build());
		}

		private void addKey(int x, int y, String prefix, KeyMapping km) {
			Button b = Button.builder(keyLabel(prefix, km), btn -> {
				listening = km;
				refreshKeys();
			}).bounds(x, y, 110, 20).build();
			keyButtons.add(b);
			keyMaps.add(km);
			keyPrefixes.add(prefix);
			addRenderableWidget(b);
		}

		private Component keyLabel(String prefix, KeyMapping km) {
			if (km == listening) {
				return Component.literal("> bir tusa bas <");
			}
			return Component.literal(prefix + ": ").append(km.getTranslatedKeyMessage());
		}

		private void refreshKeys() {
			for (int i = 0; i < keyButtons.size(); i++) {
				keyButtons.get(i).setMessage(keyLabel(keyPrefixes.get(i), keyMaps.get(i)));
			}
		}

		private void bind(InputConstants.Key key) {
			Minecraft mc = Minecraft.getInstance();
			mc.options.setKey(listening, key);
			KeyMapping.resetMapping();
			mc.options.save();
			listening = null;
			refreshKeys();
		}

		@Override
		public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
			if (listening != null) {
				if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
					listening = null;
					refreshKeys();
				} else {
					bind(InputConstants.getKey(keyCode, scanCode));
				}
				return true;
			}
			return super.keyPressed(keyCode, scanCode, modifiers);
		}

		@Override
		public boolean mouseClicked(double mouseX, double mouseY, int button) {
			if (listening != null) {
				bind(InputConstants.Type.MOUSE.getOrCreate(button));
				return true;
			}
			return super.mouseClicked(mouseX, mouseY, button);
		}

		private Button toggle(int x, int y, String name, Supplier<Boolean> get, Consumer<Boolean> set) {
			return Button.builder(label(name, get.get()), b -> {
				set.accept(!get.get());
				b.setMessage(label(name, get.get()));
			}).bounds(x, y, 200, 20).build();
		}

		private static Component label(String name, boolean on) {
			return Component.literal(name + ": " + (on ? "ACIK" : "KAPALI"));
		}
	}

	private static class Slider extends AbstractSliderButton {
		private final String name;
		private final float min;
		private final float max;
		private final Consumer<Float> setter;

		Slider(int x, int y, String name, float min, float max, float current, Consumer<Float> setter) {
			super(x, y, 200, 20, Component.empty(), (current - min) / (max - min));
			this.name = name;
			this.min = min;
			this.max = max;
			this.setter = setter;
			updateMessage();
		}

		@Override
		protected void updateMessage() {
			float v = min + (float) this.value * (max - min);
			setMessage(Component.literal(name + ": " + String.format("%.1f", v)));
		}

		@Override
		protected void applyValue() {
			setter.accept(min + (float) this.value * (max - min));
		}
	}
	}
