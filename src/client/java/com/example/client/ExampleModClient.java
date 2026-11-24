package com.example.client;

import com.mojang.blaze3d.platform.InputConstants;
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
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class Extras {
	static boolean elytraTakeoff = false;
	static boolean elytraSpeed = false;
	static float elytraSpeedVal = 1.5f;
	static boolean autoTotem = false;
	static boolean aura = false;
	static float auraRange = 3.5f;
	static boolean auraPlayersOnly = true;
	static boolean criticals = false;

	static KeyMapping swapKey;
	static KeyMapping speedKey;
	static KeyMapping auraKey;
	static KeyMapping totemKey;
	static KeyMapping menuKey;
	static int tick = 0;

	public static void init() {
		swapKey = reg("Elytra Swap", GLFW.GLFW_KEY_B);
		speedKey = reg("Elytra Speed", GLFW.GLFW_KEY_N);
		auraKey = reg("Aura", GLFW.GLFW_KEY_K);
		totemKey = reg("AutoTotem", GLFW.GLFW_KEY_J);
		menuKey = reg("Extras Menu", GLFW.GLFW_KEY_RIGHT_CONTROL);

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			LocalPlayer p = client.player;
			if (p == null || client.level == null || client.gameMode == null) return;
			tick++;

			while (menuKey.consumeClick()) {
				if (client.screen == null) client.setScreen(new ExtrasScreen());
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

			if (client.screen != null) return;

			if (elytraTakeoff && !p.onGround() && !p.isFallFlying() && !p.isInWater()
					&& p.getDeltaMovement().y < 0
					&& p.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA)) {
				if (p.tryToStartFallFlying()) {
					p.connection.send(new ServerboundPlayerCommandPacket(
						p, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
				}
			}

			if (elytraSpeed && p.isFallFlying()) {
				if (client.options.keyUp.isDown()) {
					p.setDeltaMovement(p.getLookAngle().scale(elytraSpeedVal));
				} else if (client.options.keyShift.isDown()) {
					p.setDeltaMovement(Vec3.ZERO);
				}
			}

			if (autoTotem && tick % 4 == 0 && !p.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) {
				for (int i = 9; i <= 44; i++) {
					if (p.inventoryMenu.getSlot(i).getItem().is(Items.TOTEM_OF_UNDYING)) {
						client.gameMode.handleInventoryMouseClick(
							p.inventoryMenu.containerId, i, 40, ClickType.SWAP, p);
						break;
					}
				}
			}

			if (aura && p.isAlive()) {
				runAura(client, p);
			}
		});
	}

	private static void runAura(Minecraft client, LocalPlayer p) {
		if (p.getAttackStrengthScale(0.5f) < 1.0f) return;
		LivingEntity best = null;
		double bestDist = auraRange;
		for (LivingEntity e : client.level.getEntitiesOfClass(
				LivingEntity.class, p.getBoundingBox().inflate(auraRange),
				x -> x != p && x.isAlive() && !x.isSpectator())) {
			if (auraPlayersOnly && !(e instanceof Player)) continue;
			double d = p.distanceTo(e);
			if (d < bestDist) {
				bestDist = d;
				best = e;
			}
		}
		if (best == null) return;
		face(p, best);
		crit(p);
		client.gameMode.attack(p, best);
		p.swing(InteractionHand.MAIN_HAND);
	}

	private static void face(LocalPlayer p, LivingEntity t) {
		Vec3 eye = p.getEyePosition();
		Vec3 tp = t.position().add(0, t.getBbHeight() * 0.6, 0);
		double dx = tp.x - eye.x;
		double dy = tp.y - eye.y;
		double dz = tp.z - eye.z;
		double horiz = Math.sqrt(dx * dx + dz * dz);
		p.setYRot((float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0));
		p.setXRot((float) (-Math.toDegrees(Math.atan2(dy, horiz))));
	}

	private static void crit(LocalPlayer p) {
		if (!criticals || !p.onGround() || p.isInWater() || p.onClimbable()) return;
		double x = p.getX();
		double y = p.getY();
		double z = p.getZ();
		p.connection.send(new ServerboundMovePlayerPacket.Pos(x, y + 0.0625, z, false));
		p.connection.send(new ServerboundMovePlayerPacket.Pos(x, y, z, false));
	}

	static void swapElytra(Minecraft client, LocalPlayer p) {
		boolean wearingElytra = p.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA);
		int src = -1;
		for (int i = 9; i <= 44; i++) {
			ItemStack s = p.inventoryMenu.getSlot(i).getItem();
			boolean match;
			if (wearingElytra) {
				match = s.getItem() instanceof ArmorItem a && a.getEquipmentSlot() == EquipmentSlot.CHEST;
			} else {
				match = s.is(Items.ELYTRA);
			}
			if (match) {
				src = i;
				break;
			}
		}
		if (src == -1) {
			p.displayClientMessage(Component.literal("Envanterde takas edilecek esya yok"), true);
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

	private static KeyMapping reg(String name, int key) {
		return KeyBindingHelper.registerKeyBinding(
			new KeyMapping(name, InputConstants.Type.KEYSYM, key, "KediClient"));
	}

	private static void msg(Player p, String name, boolean on) {
		p.displayClientMessage(Component.literal(name + ": " + (on ? "ACIK" : "KAPALI")), true);
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
			addRenderableWidget(Button.builder(Component.literal("Kapat"), b -> onClose())
				.bounds(x, y + s * 9, 200, 20).build());
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
