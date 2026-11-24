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
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ExampleModClient implements ClientModInitializer {
	// ---------- Temel ozellikler ----------
	static boolean fullbright = false;
	static boolean autoSprint = false;
	static boolean triggerBot = false;
	static boolean triggerPlayersOnly = true;
	static float triggerRange = 3.0f;
	static boolean aimAssist = false;
	static boolean aimOnlyWhileAttacking = false;
	static boolean aimMobs = true;
	static float aimRange = 4.5f;
	static float aimFov = 90f;
	static float aimSmooth = 4f;

	// ---------- Ekstra ozellikler ----------
	static boolean elytraTakeoff = false;
	static boolean elytraSpeed = false;
	static float elytraSpeedVal = 1.5f;
	static boolean autoTotem = false;
	static boolean aura = false;
	static float auraRange = 3.5f;
	static boolean auraPlayersOnly = true;
	static boolean criticals = false;

	// ---------- Tuslar ----------
	static KeyMapping fullbrightKey;
	static KeyMapping sprintKey;
	static KeyMapping triggerKey;
	static KeyMapping aimKey;
	static KeyMapping menuKey;
	static KeyMapping swapKey;
	static KeyMapping speedKey;
	static KeyMapping auraKey;
	static KeyMapping totemKey;
	static KeyMapping extrasKey;

	static int tick = 0;

	@Override
	public void onInitializeClient() {
		fullbrightKey = reg("Fullbright", GLFW.GLFW_KEY_G);
		sprintKey = reg("AutoSprint", GLFW.GLFW_KEY_H);
		triggerKey = reg("TriggerBot", GLFW.GLFW_KEY_R);
		aimKey = reg("AimAssist", GLFW.GLFW_KEY_V);
		menuKey = reg("Menu", GLFW.GLFW_KEY_RIGHT_SHIFT);
		swapKey = reg("Elytra Swap", GLFW.GLFW_KEY_B);
		speedKey = reg("Elytra Speed", GLFW.GLFW_KEY_N);
		auraKey = reg("Aura", GLFW.GLFW_KEY_K);
		totemKey = reg("AutoTotem", GLFW.GLFW_KEY_J);
		extrasKey = reg("Extras Menu", GLFW.GLFW_KEY_RIGHT_CONTROL);

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			LocalPlayer p = client.player;
			if (p == null || client.level == null || client.gameMode == null) return;
			tick++;

			while (menuKey.consumeClick()) {
				if (client.screen == null) client.setScreen(new ConfigScreen());
			}
			while (extrasKey.consumeClick()) {
				if (client.screen == null) client.setScreen(new ExtrasScreen());
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
			while (swapKey.consumeClick()) {
				swapElytra(client, p);
			}
			while (speedKey.consumeClick()) {
				elytraSpeed = !elytraSpeed;
				msg(p, "Elytra Speed", elytraSpeed);
			}
			while (auraKey.consumeClick()) {
				aura = !aura;
				msg(p, "Aura", aura);
			}
			while (totemKey.consumeClick()) {
				autoTotem = !autoTotem;
				msg(p, "AutoTotem", autoTotem);
			}

			if (fullbright) {
				p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 1000, 0, false, false, false));
			}

			if (autoSprint && client.options.keyUp.isDown() && !p.isShiftKeyDown()
					&& !p.horizontalCollision && p.getFoodData().getFoodLevel() > 6) {
				p.setSprinting(true);
			}

			if (client.screen != null) return;

			// Elytra otomatik kalkis
			if (elytraTakeoff && !p.onGround() && !p.isFallFlying() && !p.isInWater()
					&& p.getDeltaMovement().y < 0
					&& p.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA)) {
				p.tryToStartFallFlying();
			}

			// Elytra hiz
			if (elytraSpeed && p.isFallFlying()) {
				if (client.options.keyUp.isDown()) {
					p.setDeltaMovement(p.getLookAngle().scale(elytraSpeedVal));
				} else if (client.options.keyShift.isDown()) {
					p.setDeltaMovement(Vec3.ZERO);
				}
			}

			// AutoTotem
			if (autoTotem && tick % 4 == 0 && !p.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) {
				for (int i = 9; i <= 44; i++) {
					if (p.inventoryMenu.getSlot(i).getItem().is(Items.TOTEM_OF_UNDYING)) {
						client.gameMode.handleInventoryMouseClick(
							p.inventoryMenu.containerId, i, 40, ClickType.SWAP, p);
						break;
					}
				}
			}

			// TriggerBot
			if (triggerBot && client.hitResult instanceof EntityHitResult hit) {
				Entity e = hit.getEntity();
				double dist = p.getEyePosition().distanceTo(hit.getLocation());
				boolean ok = e instanceof LivingEntity && e.isAlive()
						&& (!triggerPlayersOnly || e instanceof Player)
						&& dist <= triggerRange;
				if (ok && p.getAttackStrengthScale(0.5f) >= 1.0f) {
					client.gameMode.attack(p, e);
					p.swing(InteractionHand.MAIN_HAND);
				}
			}

			// AimAssist
			if (aimAssist && (!aimOnlyWhileAttacking || client.options.keyAttack.isDown())) {
				runAimAssist(client, p);
			}

			// Aura
			if (aura && p.isAlive()) {
				runAura(client, p);
			}
		});
	}

	// ---------- AimAssist ----------
	private static void runAimAssist(Minecraft client, Player p) {
		float bestDiff = aimFov;
		float[] bestRot = null;
		List<LivingEntity> list = client.level.getEntitiesOfClass(
			LivingEntity.class, p.getBoundingBox().inflate(aimRange), e -> e != p && e.isAlive());
		for (LivingEntity other : list) {
			if (other.isSpectator()) continue;
			if (!(other instanceof Player) && !aimMobs) continue;
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

	// ---------- Aura ----------
	private static void runAura(Minecraft client, LocalPlayer p) {
		if (p.getAttackStrengthScale(0.5f) < 1.0f) return;
		LivingEntity best = null;
		double bestDist = auraRange;
		List<LivingEntity> list = client.level.getEntitiesOfClass(
			LivingEntity.class, p.getBoundingBox().inflate(auraRange),
			x -> x != p && x.isAlive() && !x.isSpectator());
		for (LivingEntity e : list) {
			if (auraPlayersOnly && !(e instanceof Player)) continue;
			double d = p.distanceTo(e);
			if (d < bestDist) {
				bestDist = d;
				best = e;
			}
		}
		if (best == null) return;
		Vec3 tp = best.position().add(0, best.getBbHeight() * 0.6, 0);
		float[] rot = rotationTo(p, tp);
		p.setYRot(rot[0]);
		p.setXRot(rot[1]);
		crit(p);
		client.gameMode.attack(p, best);
		p.swing(InteractionHand.MAIN_HAND);
	}

	private static void crit(LocalPlayer p) {
		if (!criticals || !p.onGround() || p.isInWater() || p.onClimbable()) return;
		double x = p.getX();
		double y = p.getY();
		double z = p.getZ();
		p.connection.send(new ServerboundMovePlayerPacket.Pos(x, y + 0.0625, z, false));
		p.connection.send(new ServerboundMovePlayerPacket.Pos(x, y, z, false));
	}

	// ---------- Elytra degistir ----------
	static void swapElytra(Minecraft client, LocalPlayer p) {
		boolean wearing = p.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA);
		int src = -1;
		for (int i = 9; i <= 44; i++) {
			ItemStack s = p.inventoryMenu.getSlot(i).getItem();
			boolean match;
			if (wearing) {
				match = s.getItem() instanceof ArmorItem a
					&& a.getEquipmentSlot() == EquipmentSlot.CHEST;
			} else {
				match = s.is(Items.ELYTRA);
			}
			if (match) {
				src = i;
				break;
			}
		}
		if (src == -1) {
			p.displayClientMessage(Component.literal("Takas edilecek esya yok"), true);
			return;
		}
		click(client, p, src);
		click(client, p, 6);
		click(client, p, src);
	}

	private static void click(Minecraft client, LocalPlayer p, int slot) {
		client.gameMode.handleInventoryMouseClick(
			p.inventoryMenu.containerId, slot, 0, ClickType.PICKUP, p);
	}

	// ---------- Yardimcilar ----------
	private static KeyMapping reg(String name, int key) {
		return KeyBindingHelper.registerKeyBinding(
			new KeyMapping(name, InputConstants.Type.KEYSYM, key, "KediClient"));
	}

	private static void msg(Player p, String name, boolean on) {
		p.displayClientMessage(Component.literal(name + ": " + (on ? "ACIK" : "KAPALI")), true);
	}

	private static Component label(String name, boolean on) {
		return Component.literal(name + ": " + (on ? "ACIK" : "KAPALI"));
	}

	private static Button toggle(int x, int y, String name, Supplier<Boolean> get, Consumer<Boolean> set) {
		return Button.builder(label(name, get.get()), b -> {
			set.accept(!get.get());
			b.setMessage(label(name, get.get()));
		}).bounds(x, y, 200, 20).build();
	}

	// ================= ANA MENU =================
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
			int s = 21;

			addRenderableWidget(toggle(x, y0, "Fullbright",
				() -> fullbright, v -> fullbright = v));
			addRenderableWidget(toggle(x, y0 + s, "AutoSprint",
				() -> autoSprint, v -> autoSprint = v));
			addRenderableWidget(toggle(x, y0 + s * 2, "TriggerBot",
				() -> triggerBot, v -> triggerBot = v));
			addRenderableWidget(toggle(x, y0 + s * 3, "Sadece oyuncu",
				() -> triggerPlayersOnly, v -> triggerPlayersOnly = v));
			addRenderableWidget(new Slider(x, y0 + s * 4, "Trigger menzil", 2f, 6f,
				triggerRange, v -> triggerRange = v));
			addRenderableWidget(toggle(x, y0 + s * 5, "AimAssist",
				() -> aimAssist, v -> aimAssist = v));
			addRenderableWidget(toggle(x, y0 + s * 6, "Sadece vururken aim",
				() -> aimOnlyWhileAttacking, v -> aimOnlyWhileAttacking = v));
			addRenderableWidget(toggle(x, y0 + s * 7, "Moblari da hedefle",
				() -> aimMobs, v -> aimMobs = v));
			addRenderableWidget(new Slider(x, y0 + s * 8, "Aim menzil", 3f, 8f,
				aimRange, v -> aimRange = v));
			addRenderableWidget(new Slider(x, y0 + s * 9, "Gorus acisi", 10f, 180f,
				aimFov, v -> aimFov = v));
			addRenderableWidget(new Slider(x, y0 + s * 10, "Yumusaklik", 1f, 20f,
				aimSmooth, v -> aimSmooth = v));

			addKey(kx, y0, "Tus", fullbrightKey);
			addKey(kx, y0 + s, "Tus", sprintKey);
			addKey(kx, y0 + s * 2, "Tus", triggerKey);
			addKey(kx, y0 + s * 5, "Tus", aimKey);
			addKey(kx, y0 + s * 6, "Menu", menuKey);

			addRenderableWidget(Button.builder(Component.literal("Extras menu >"),
				b -> this.minecraft.setScreen(new ExtrasScreen()))
				.bounds(kx, y0 + s * 9, 110, 20).build());
			addRenderableWidget(Button.builder(Component.literal("Kapat"), b -> onClose())
				.bounds(kx, y0 + s * 10, 110, 20).build());
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
	}

	// ================= EXTRAS MENUSU =================
	public static class ExtrasScreen extends Screen {
		public ExtrasScreen() {
			super(Component.literal("KediClient Extras"));
		}

		@Override
		protected void init() {
			int x = this.width / 2 - 100;
			int y = 5;
			int s = 21;
			addRenderableWidget(toggle(x, y, "Elytra oto kalkis",
				() -> elytraTakeoff, v -> elytraTakeoff = v));
			addRenderableWidget(toggle(x, y + s, "Elytra hiz",
				() -> elytraSpeed, v -> elytraSpeed = v));
			addRenderableWidget(new Slider(x, y + s * 2, "Elytra hizi", 0.5f, 3f,
				elytraSpeedVal, v -> elytraSpeedVal = v));
			addRenderableWidget(toggle(x, y + s * 3, "AutoTotem",
				() -> autoTotem, v -> autoTotem = v));
			addRenderableWidget(toggle(x, y + s * 4, "Aura",
				() -> aura, v -> aura = v));
			addRenderableWidget(new Slider(x, y + s * 5, "Aura menzil", 2f, 6f,
				auraRange, v -> auraRange = v));
			addRenderableWidget(toggle(x, y + s * 6, "Aura sadece oyuncu",
				() -> auraPlayersOnly, v -> auraPlayersOnly = v));
			addRenderableWidget(toggle(x, y + s * 7, "Criticals",
				() -> criticals, v -> criticals = v));
			addRenderableWidget(Button.builder(Component.literal("Elytra degistir"), b -> {
				Minecraft mc = Minecraft.getInstance();
				if (mc.player != null) swapElytra(mc, mc.player);
			}).bounds(x, y + s * 8, 200, 20).build());
			addRenderableWidget(Button.builder(Component.literal("< Ana menu"),
				b -> this.minecraft.setScreen(new ConfigScreen()))
				.bounds(x, y + s * 9, 98, 20).build());
			addRenderableWidget(Button.builder(Component.literal("Kapat"), b -> onClose())
				.bounds(x + 102, y + s * 9, 98, 20).build());
		}
	}

	// ================= KAYDIRICI =================
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
			
