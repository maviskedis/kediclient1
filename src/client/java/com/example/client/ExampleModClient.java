package com.example.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
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

	private static KeyMapping fullbrightKey;
	private static KeyMapping sprintKey;
	private static KeyMapping triggerKey;
	private static KeyMapping aimKey;

	@Override
	public void onInitializeClient() {
		fullbrightKey = reg("Fullbright", GLFW.GLFW_KEY_G);
		sprintKey = reg("AutoSprint", GLFW.GLFW_KEY_H);
		triggerKey = reg("TriggerBot", GLFW.GLFW_KEY_R);
		aimKey = reg("AimAssist", GLFW.GLFW_KEY_V);

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			Player p = client.player;
			if (p == null || client.level == null) return;

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
}
