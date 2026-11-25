package com.example.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class ExampleModClient implements ClientModInitializer {
	static final int ACCENT = 0xFFB388FF;
	static final List<Mod> MODS = new ArrayList<>();

	static Criticals criticals;
	static HudMod hud;
	static LocalPlayer lastPlayer = null;
	static long splashStart = 0;
	static int tick = 0;
	static LivingEntity auraTarget = null;
	static BlockPos bedPos = null;
	static double bedDist = -1;
	static double speedBps = 0;
	static double lastPx = 0;
	static double lastPz = 0;
	static int frames = 0;
	static int fpsValue = 0;
	static long fpsStart = 0;

	// =====================================================
	//  BASLANGIC
	// =====================================================
	@Override
	public void onInitializeClient() {
		// Combat
		criticals = new Criticals();
		MODS.add(new Aura());
		MODS.add(new TriggerBot());
		MODS.add(criticals);
		MODS.add(new Velocity());
		MODS.add(new AutoTotem());
		// Elytra
		MODS.add(new ElytraBooster());
		MODS.add(new ElytraTarget());
		MODS.add(new ElytraHelper());
		MODS.add(new ElytraFirework());
		// Move
		MODS.add(new AutoSprint());
		MODS.add(new NoFall());
		MODS.add(new Flight());
		MODS.add(new Speed());
		// Render
		MODS.add(new Fullbright());
		MODS.add(new Esp());
		MODS.add(new FovChanger());
		// Player
		MODS.add(new AutoRespawn());
		MODS.add(new ChestStealer());
		MODS.add(new AntiBlind());
		MODS.add(new AntiAfk());
		MODS.add(new AutoWalk());
		MODS.add(new AutoTool());
		// Bedwars
		MODS.add(new Scaffold());
		MODS.add(new ResourceGlow());
		MODS.add(new BedFinder());
		MODS.add(new InvisEsp());
		// Client
		MODS.add(new ClickGuiMod());
		hud = new HudMod();
		MODS.add(hud);

		loadConfig();

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			LocalPlayer p = client.player;
			if (p == null || client.level == null || client.gameMode == null) {
				lastPlayer = null;
				return;
			}
			if (p != lastPlayer) {
				lastPlayer = p;
				splashStart = System.currentTimeMillis();
				p.displayClientMessage(Component.literal("KediClient yuklendi | Sag Shift = menu"), false);
			}
			tick++;
			double ddx = p.getX() - lastPx;
			double ddz = p.getZ() - lastPz;
			speedBps = Math.sqrt(ddx * ddx + ddz * ddz) * 20.0;
			lastPx = p.getX();
			lastPz = p.getZ();

			long win = client.getWindow().getWindow();
			for (Mod m : MODS) {
				boolean down = isDown(win, m.key);
				if (down && !m.keyDown && client.screen == null) {
					m.onPress(client);
				}
				m.keyDown = down;
			}

			for (Mod m : MODS) {
				if (!m.enabled) continue;
				try {
					m.onTick(client, p);
				} catch (Exception ex) {
					m.enabled = false;
				}
			}
		});

		ClientTickEvents.START_CLIENT_TICK.register(client -> {
			LocalPlayer p = client.player;
			if (p == null || client.level == null) return;
			for (Mod m : MODS) {
				if (!m.enabled) continue;
				try {
					m.onPreTick(client, p);
				} catch (Exception ex) {
					m.enabled = false;
				}
			}
		});

		HudRenderCallback.EVENT.register((g, delta) -> renderHud(g));
	}

	// =====================================================
	//  YARDIMCI FONKSIYONLAR
	// =====================================================
	static boolean isDown(long window, int code) {
		if (code < 0) return false;
		if (code >= 1000) return GLFW.glfwGetMouseButton(window, code - 1000) == GLFW.GLFW_PRESS;
		return GLFW.glfwGetKey(window, code) == GLFW.GLFW_PRESS;
	}

	static String keyName(int code) {
		if (code < 0) return "NONE";
		if (code >= 1000) {
			int b = code - 1000;
			return b == 2 ? "Orta tik" : "Mouse" + (b + 1);
		}
		return InputConstants.Type.KEYSYM.getOrCreate(code).getDisplayName().getString();
	}

	static float[] rotationTo(Player p, Vec3 t) {
		Vec3 eye = p.getEyePosition();
		double dx = t.x - eye.x;
		double dy = t.y - eye.y;
		double dz = t.z - eye.z;
		double horiz = Math.sqrt(dx * dx + dz * dz);
		float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
		float pitch = (float) (-Math.toDegrees(Math.atan2(dy, horiz)));
		return new float[] { yaw, pitch };
	}

	static Vec3 aimPoint(LivingEntity e) {
		return e.position().add(0, e.getBbHeight() * 0.6, 0);
	}

	static float angleDiff(Player p, float[] rot) {
		float dy = Mth.wrapDegrees(rot[0] - p.getYRot());
		float dp = rot[1] - p.getXRot();
		return (float) Math.sqrt(dy * dy + dp * dp);
	}

	static void rotateToward(LocalPlayer p, float[] rot, float maxStep) {
		float step = Math.max(1f, maxStep);
		float dy = Mth.wrapDegrees(rot[0] - p.getYRot());
		float dp = rot[1] - p.getXRot();
		p.setYRot(p.getYRot() + Mth.clamp(dy, -step, step));
		p.setXRot(Mth.clamp(p.getXRot() + Mth.clamp(dp, -step, step), -90f, 90f));
	}

	static boolean isBot(Minecraft mc, Player o) {
		return mc.getConnection() != null && mc.getConnection().getPlayerInfo(o.getUUID()) == null;
	}

	static boolean swapChest(Minecraft mc, LocalPlayer p, boolean toElytra, boolean hotbarMode) {
		if (hotbarMode) {
			for (int i = 0; i < 9; i++) {
				ItemStack s = p.getInventory().getItem(i);
				boolean match;
				if (toElytra) {
					match = s.is(Items.ELYTRA);
				} else {
					match = s.getItem() instanceof ArmorItem a
						&& a.getEquipmentSlot() == EquipmentSlot.CHEST;
				}
				if (match) {
					mc.gameMode.handleInventoryMouseClick(
						p.inventoryMenu.containerId, 6, i, ClickType.SWAP, p);
					return true;
				}
			}
			return false;
		}
		int src = -1;
		for (int i = 9; i <= 44; i++) {
			ItemStack s = p.inventoryMenu.getSlot(i).getItem();
			boolean match;
			if (toElytra) {
				match = s.is(Items.ELYTRA);
			} else {
				match = s.getItem() instanceof ArmorItem a
					&& a.getEquipmentSlot() == EquipmentSlot.CHEST;
			}
			if (match) {
				src = i;
				break;
			}
		}
		if (src == -1) return false;
		slotClick(mc, p, src);
		slotClick(mc, p, 6);
		slotClick(mc, p, src);
		return true;
	}

							static boolean useFirework(Minecraft mc, LocalPlayer p, boolean silent) {
		if (p.getOffhandItem().is(Items.FIREWORK_ROCKET)) {
			mc.gameMode.useItem(p, InteractionHand.OFF_HAND);
			return true;
		}
		int slot = -1;
		for (int i = 0; i < 9; i++) {
			if (p.getInventory().getItem(i).is(Items.FIREWORK_ROCKET)) {
				slot = i;
				break;
			}
		}
		if (slot == -1) return false;
		int old = p.getInventory().selected;
		p.getInventory().selected = slot;
		mc.gameMode.useItem(p, InteractionHand.MAIN_HAND);
		if (silent) p.getInventory().selected = old;
		return true;
	}

	static boolean airBelow(Minecraft mc, LocalPlayer p, int n) {
		BlockPos base = p.blockPosition();
		for (int k = 1; k <= n; k++) {
			if (!mc.level.getBlockState(base.below(k)).isAir()) return false;
		}
		return true;
	}

	static final Item[] SWORDS = {
		Items.NETHERITE_SWORD, Items.DIAMOND_SWORD, Items.IRON_SWORD,
		Items.STONE_SWORD, Items.GOLDEN_SWORD, Items.WOODEN_SWORD
	};

	static void equipBestSword(LocalPlayer p) {
		for (Item sw : SWORDS) {
			for (int i = 0; i < 9; i++) {
				if (p.getInventory().getItem(i).is(sw)) {
					p.getInventory().selected = i;
					return;
				}
			}
		}
	}

	static void slotClick(Minecraft mc, LocalPlayer p, int slot) {
		mc.gameMode.handleInventoryMouseClick(
			p.inventoryMenu.containerId, slot, 0, ClickType.PICKUP, p);
	}

	static int accent(int i) {
		double t = (Math.sin(System.currentTimeMillis() / 500.0 + i * 0.5) + 1.0) / 2.0;
		int r = (int) (0xB3 + (0x4F - 0xB3) * t);
		int gr = (int) (0x88 + (0xC3 - 0x88) * t);
		int b = (int) (0xFF + (0xF7 - 0xFF) * t);
		return 0xFF000000 | (r << 16) | (gr << 8) | b;
	}

	// =====================================================
	//  CONFIG
	// =====================================================
	static Path cfgPath() {
		return FabricLoader.getInstance().getConfigDir().resolve("kediclient.properties");
	}

	static void saveConfig() {
		try {
			Properties pr = new Properties();
			for (Mod m : MODS) {
				pr.setProperty(m.name + ".key", String.valueOf(m.key));
				for (Setting s : m.settings) {
					pr.setProperty(m.name + "." + s.name, s.get());
				}
			}
			pr.setProperty("HUD.enabled", String.valueOf(hud != null && hud.enabled));
			try (OutputStream o = Files.newOutputStream(cfgPath())) {
				pr.store(o, "KediClient");
			}
		} catch (Exception ignored) {
		}
	}

	static void loadConfig() {
		try {
			Path f = cfgPath();
			if (!Files.exists(f)) return;
			Properties pr = new Properties();
			try (InputStream in = Files.newInputStream(f)) {
				pr.load(in);
			}
			for (Mod m : MODS) {
				try {
					String k = pr.getProperty(m.name + ".key");
					if (k != null) m.key = Integer.parseInt(k.trim());
				} catch (Exception ignored) {
				}
				for (Setting s : m.settings) {
					String v = pr.getProperty(m.name + "." + s.name);
					if (v != null) s.set(v);
				}
			}
			String h = pr.getProperty("HUD.enabled");
			if (h != null && hud != null) hud.enabled = Boolean.parseBoolean(h.trim());
		} catch (Exception ignored) {
		}
	}

	// =====================================================
	//  HUD (isim, modul listesi, acilis yazisi)
	// =====================================================
	static void renderHud(GuiGraphics g) {
		Minecraft mc = Minecraft.getInstance();
		frames++;
		long nowMs = System.currentTimeMillis();
		if (nowMs - fpsStart >= 1000) {
			fpsValue = frames;
			frames = 0;
			fpsStart = nowMs;
		}
		if (mc.player == null || mc.options.hideGui || hud == null || !hud.enabled) return;
		LocalPlayer me = mc.player;
		int sw = mc.getWindow().getGuiScaledWidth();
		int sh = mc.getWindow().getGuiScaledHeight();

		if (hud.watermark.value) {
			String t = "KediClient";
			String info = " | " + mc.getUser().getName();
			int w = mc.font.width(t + info) + 8;
			g.fill(3, 3, 3 + w, 15, 0xA0140C28);
			g.fill(3, 3, 5, 15, ACCENT);
			g.drawString(mc.font, t, 8, 6, ACCENT);
			g.drawString(mc.font, info, 8 + mc.font.width(t), 6, 0xFFCCCCCC);
		}

		// bilgi satirlari
		List<String> lines = new ArrayList<>();
		if (hud.coords.value) {
			lines.add(String.format("XYZ: %.0f %.0f %.0f", me.getX(), me.getY(), me.getZ()));
		}
		if (hud.dir.value) {
			lines.add("Yon: " + me.getDirection().getName());
		}
		if (hud.fps.value) {
			lines.add("FPS: " + fpsValue);
		}
		if (hud.ping.value && mc.getConnection() != null) {
			var pi = mc.getConnection().getPlayerInfo(me.getUUID());
			if (pi != null) lines.add("Ping: " + pi.getLatency() + " ms");
		}
		if (hud.speed.value) {
			lines.add(String.format("Hiz: %.1f b/s", speedBps));
		}
		if (hud.time.value) {
			java.time.LocalTime lt = java.time.LocalTime.now();
			lines.add(String.format("Saat: %02d:%02d", lt.getHour(), lt.getMinute()));
		}
		if (hud.counters.value) {
			int totems = 0;
			int rockets = 0;
			for (int i = 0; i < me.getInventory().getContainerSize(); i++) {
				ItemStack st = me.getInventory().getItem(i);
				if (st.is(Items.TOTEM_OF_UNDYING)) totems += st.getCount();
				if (st.is(Items.FIREWORK_ROCKET)) rockets += st.getCount();
			}
			lines.add("Totem: " + totems + "  Roket: " + rockets);
		}
		if (hud.bed.value && bedPos != null) {
			lines.add(String.format("Yatak: %.0f blok (%d %d %d)",
				bedDist, bedPos.getX(), bedPos.getY(), bedPos.getZ()));
		}
		int iy = 20;
		for (String line : lines) {
			int w = mc.font.width(line) + 6;
			g.fill(3, iy, 3 + w, iy + 11, 0x90000000);
			g.fill(3, iy, 5, iy + 11, ACCENT);
			g.drawString(mc.font, line, 8, iy + 2, 0xFFE0E0E0);
			iy += 11;
		}

		if (hud.arrayList.value) {
			List<Mod> on = new ArrayList<>();
			for (Mod m : MODS) {
				if (m.enabled && m != hud) on.add(m);
			}
			on.sort((a, b) -> Integer.compare(mc.font.width(b.name), mc.font.width(a.name)));
			int y = 3;
			int i = 0;
			for (Mod m : on) {
				int w = mc.font.width(m.name);
				int x = sw - w - 8;
				int c = accent(i);
				g.fill(x - 2, y, sw - 2, y + 11, 0x90000000);
				g.fill(sw - 4, y, sw - 2, y + 11, c);
				g.drawString(mc.font, m.name, x, y + 2, c);
				y += 11;
				i++;
			}
		}

		if (hud.targetHud.value && auraTarget != null && auraTarget.isAlive()) {
			LivingEntity t = auraTarget;
			String nm = t.getName().getString();
			float hp = t.getHealth();
			float mx = Math.max(1f, t.getMaxHealth());
			int bw = 110;
			int bx = sw / 2 - bw / 2;
			int by = sh / 2 + 24;
			g.fill(bx - 3, by - 3, bx + bw + 3, by + 31, 0xA0140C28);
			g.fill(bx - 3, by - 3, bx - 1, by + 31, ACCENT);
			g.drawString(mc.font, nm, bx + 2, by, 0xFFFFFFFF);
			g.fill(bx + 2, by + 11, bx + bw - 2, by + 17, 0xFF2A2240);
			int fw = (int) ((bw - 4) * Math.min(1f, hp / mx));
			g.fill(bx + 2, by + 11, bx + 2 + fw, by + 17, hp / mx > 0.5f ? 0xFF7CFFB0 : 0xFFFF7C7C);
			g.drawString(mc.font, String.format("%.1f / %.1f", hp, mx), bx + 2, by + 20, 0xFFCCCCCC);
		}

		if (hud.splash.value && splashStart > 0) {
			long dt = System.currentTimeMillis() - splashStart;
			if (dt < 5000) {
				float alpha = 1f;
				if (dt < 600) alpha = dt / 600f;
				else if (dt > 4000) alpha = (5000 - dt) / 1000f;
				int a = (int) (alpha * 255f);
				if (a > 10) {
					int color = (a << 24) | 0xB388FF;
					int sub = (a << 24) | 0xDDDDDD;
					String title = "KediClient";
					String subtitle = "Sag Shift = menu";
					g.pose().pushPose();
					g.pose().scale(3f, 3f, 1f);
					int tw = mc.font.width(title);
					g.drawString(mc.font, title, (int) (sw / 6f - tw / 2f), (int) (sh / 12f), color);
					g.pose().popPose();
					int sw2 = mc.font.width(subtitle);
					g.drawString(mc.font, subtitle, sw / 2 - sw2 / 2, sh / 4 + 34, sub);
				}
			}
		}
	}

				// =====================================================
	//  AYAR SINIFLARI
	// =====================================================
	static abstract class Setting {
		final String name;

		Setting(String name) {
			this.name = name;
		}

		abstract String get();

		abstract void set(String v);
	}

	static class BoolSetting extends Setting {
		boolean value;

		BoolSetting(String name, boolean value) {
			super(name);
			this.value = value;
		}

		@Override
		String get() {
			return String.valueOf(value);
		}

		@Override
		void set(String v) {
			value = Boolean.parseBoolean(v.trim());
		}
	}

	static class FloatSetting extends Setting {
		float value;
		final float min;
		final float max;

		FloatSetting(String name, float value, float min, float max) {
			super(name);
			this.value = value;
			this.min = min;
			this.max = max;
		}

		@Override
		String get() {
			return String.valueOf(value);
		}

		@Override
		void set(String v) {
			try {
				float f = Float.parseFloat(v.trim());
				value = Math.max(min, Math.min(max, f));
			} catch (Exception ignored) {
			}
		}
	}

	static class ModeSetting extends Setting {
		final String[] options;
		int index;

		ModeSetting(String name, int index, String... options) {
			super(name);
			this.options = options;
			this.index = index;
		}

		boolean is(String o) {
			return options[index].equals(o);
		}

		@Override
		String get() {
			return String.valueOf(index);
		}

		@Override
		void set(String v) {
			try {
				int i = Integer.parseInt(v.trim());
				if (i >= 0 && i < options.length) index = i;
			} catch (Exception ignored) {
			}
		}
	}

	// =====================================================
	//  MODUL TEMELI
	// =====================================================
	static abstract class Mod {
		final String name;
		final String category;
		boolean enabled = false;
		boolean expanded = false;
		boolean keyDown = false;
		int key;
		final List<Setting> settings = new ArrayList<>();

		Mod(String name, String category, int key) {
			this.name = name;
			this.category = category;
			this.key = key;
		}

		BoolSetting bool(String n, boolean def) {
			BoolSetting s = new BoolSetting(n, def);
			settings.add(s);
			return s;
		}

		FloatSetting num(String n, float def, float min, float max) {
			FloatSetting s = new FloatSetting(n, def, min, max);
			settings.add(s);
			return s;
		}

		ModeSetting mode(String n, int def, String... opts) {
			ModeSetting s = new ModeSetting(n, def, opts);
			settings.add(s);
			return s;
		}

		void onPreTick(Minecraft mc, LocalPlayer p) {
		}

		void onEnable(Minecraft mc) {
		}

		void onDisable(Minecraft mc) {
		}

		void onTick(Minecraft mc, LocalPlayer p) {
		}

		void onPress(Minecraft mc) {
			toggle(mc);
		}

		void toggle(Minecraft mc) {
			enabled = !enabled;
			if (enabled) onEnable(mc);
			else onDisable(mc);
			if (mc.player != null) {
				mc.player.displayClientMessage(
					Component.literal(name + ": " + (enabled ? "ACIK" : "KAPALI")), true);
			}
		}
	}

	// =====================================================
	//  COMBAT
	// =====================================================
	static class Aura extends Mod {
		FloatSetting targetRange = num("Hedef menzili", 5f, 3f, 8f);
		FloatSetting attackRange = num("Vurus menzili", 3.5f, 2f, 6f);
		BoolSetting players = bool("Sadece oyuncu", true);
		BoolSetting skipBots = bool("Bot atla", false);
		BoolSetting teams = bool("Takim atla", true);
		ModeSetting rotMode = mode("Rotasyon", 0, "Smooth", "Silent", "Yok");
		FloatSetting turnSpeed = num("Don hizi", 20f, 3f, 90f);
		FloatSetting aimAngle = num("Vurus acisi", 25f, 5f, 90f);
		FloatSetting fov = num("Hedef FOV", 360f, 30f, 360f);
		BoolSetting autoSword = bool("Oto kilic", true);

		Aura() {
			super("Aura", "Combat", GLFW.GLFW_KEY_K);
		}

		@Override
		void onDisable(Minecraft mc) {
			auraTarget = null;
		}

		@Override
		void onTick(Minecraft mc, LocalPlayer p) {
			auraTarget = null;
			if (mc.screen != null || !p.isAlive()) return;
			LivingEntity best = null;
			double bestDist = targetRange.value;
			List<LivingEntity> list = mc.level.getEntitiesOfClass(
				LivingEntity.class, p.getBoundingBox().inflate(targetRange.value),
				x -> x != p && x.isAlive() && !x.isSpectator());
			for (LivingEntity e : list) {
				if (players.value && !(e instanceof Player)) continue;
				if (skipBots.value && e instanceof Player pl && isBot(mc, pl)) continue;
				if (teams.value && p.isAlliedTo(e)) continue;
				double d = p.distanceTo(e);
				if (d > targetRange.value) continue;
				if (fov.value < 360f) {
					float[] r0 = rotationTo(p, aimPoint(e));
					if (angleDiff(p, r0) > fov.value / 2f) continue;
				}
				if (d < bestDist) {
					bestDist = d;
					best = e;
				}
			}
			if (best == null) return;
			auraTarget = best;

			float[] rot = rotationTo(p, aimPoint(best));
			boolean smooth = rotMode.is("Smooth");
			boolean silent = rotMode.is("Silent");
			if (smooth) {
				rotateToward(p, rot, turnSpeed.value);
			}
			if (p.distanceTo(best) > attackRange.value) return;
			if (p.getAttackStrengthScale(0.5f) < 1.0f) return;
			if (smooth && angleDiff(p, rot) > aimAngle.value) return;

			if (autoSword.value) equipBestSword(p);
			if (criticals != null && criticals.enabled) {
				criticals.doCrit(p);
			}
			if (silent) {
				p.connection.send(new ServerboundMovePlayerPacket.Rot(rot[0], rot[1], p.onGround()));
			}
			mc.gameMode.attack(p, best);
			p.swing(InteractionHand.MAIN_HAND);
			if (silent) {
				p.connection.send(new ServerboundMovePlayerPacket.Rot(p.getYRot(), p.getXRot(), p.onGround()));
			}
		}
	}

	static class TriggerBot extends Mod {
		FloatSetting range = num("Menzil", 3f, 2f, 6f);
		BoolSetting players = bool("Sadece oyuncu", true);
		BoolSetting teams = bool("Takim atla", true);
		BoolSetting autoSword = bool("Oto kilic", true);

		TriggerBot() {
			super("TriggerBot", "Combat", GLFW.GLFW_KEY_R);
		}

		@Override
		void onTick(Minecraft mc, LocalPlayer p) {
			if (mc.screen != null) return;
			if (mc.hitResult instanceof EntityHitResult hit) {
				Entity e = hit.getEntity();
				double dist = p.getEyePosition().distanceTo(hit.getLocation());
				boolean ok = e instanceof LivingEntity && e.isAlive()
						&& (!players.value || e instanceof Player)
						&& !(teams.value && p.isAlliedTo(e))
						&& dist <= range.value;
				if (ok && p.getAttackStrengthScale(0.5f) >= 1.0f) {
					if (autoSword.value) equipBestSword(p);
					mc.gameMode.attack(p, e);
					p.swing(InteractionHand.MAIN_HAND);
				}
			}
		}
	}

	static class Velocity extends Mod {
		FloatSetting horizontal = num("Yatay %", 0f, 0f, 100f);
		FloatSetting vertical = num("Dikey %", 100f, 0f, 100f);
		int lastHurt = 0;
		int armed = 0;
		double lastHz = 0;
		double lastY = 0;

		Velocity() {
			super("Velocity", "Combat", -1);
		}

		@Override
		void onPreTick(Minecraft mc, LocalPlayer p) {
			int ht = p.hurtTime;
			if (ht > lastHurt) armed = 3;
			lastHurt = ht;
			Vec3 v = p.getDeltaMovement();
			double hz = Math.sqrt(v.x * v.x + v.z * v.z);
			if (armed > 0) {
				if (hz > lastHz + 0.08 || v.y > lastY + 0.1) {
					double h = horizontal.value / 100.0;
					double vt = vertical.value / 100.0;
					p.setDeltaMovement(v.x * h, v.y * vt, v.z * h);
					armed = 0;
					v = p.getDeltaMovement();
					hz = Math.sqrt(v.x * v.x + v.z * v.z);
				} else {
					armed--;
				}
			}
			lastHz = hz;
			lastY = v.y;
		}
	}

					static class Criticals extends Mod {
		Criticals() {
			super("Criticals", "Combat", -1);
		}

		void doCrit(LocalPlayer p) {
			if (!p.onGround() || p.isInWater() || p.onClimbable()) return;
			double x = p.getX();
			double y = p.getY();
			double z = p.getZ();
			p.connection.send(new ServerboundMovePlayerPacket.Pos(x, y + 0.0625, z, false));
			p.connection.send(new ServerboundMovePlayerPacket.Pos(x, y, z, false));
		}
	}

	static class AutoTotem extends Mod {
		AutoTotem() {
			super("AutoTotem", "Combat", GLFW.GLFW_KEY_J);
		}

		@Override
		void onTick(Minecraft mc, LocalPlayer p) {
			if (tick % 4 != 0) return;
			if (p.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) return;
			for (int i = 9; i <= 44; i++) {
				if (p.inventoryMenu.getSlot(i).getItem().is(Items.TOTEM_OF_UNDYING)) {
					mc.gameMode.handleInventoryMouseClick(
						p.inventoryMenu.containerId, i, 40, ClickType.SWAP, p);
					break;
				}
			}
		}
	}

	// =====================================================
	//  ELYTRA
	// =====================================================
	static class ElytraBooster extends Mod {
		FloatSetting accel = num("Hizlanma", 0.06f, 0.01f, 0.3f);
		FloatSetting max = num("Max hiz", 2.0f, 0.5f, 4.0f);
		BoolSetting vertical = bool("Ziplayinca yuksel", true);
		FloatSetting climb = num("Yukselme", 0.08f, 0.02f, 0.3f);

		ElytraBooster() {
			super("ElytraBooster", "Elytra", -1);
		}

		@Override
		void onTick(Minecraft mc, LocalPlayer p) {
			if (mc.screen != null || !p.isFallFlying()) return;
			Vec3 v = p.getDeltaMovement();
			if (mc.options.keyUp.isDown()) {
				Vec3 nv = v.add(p.getLookAngle().scale(accel.value));
				if (nv.length() > max.value) {
					nv = nv.normalize().scale(max.value);
				}
				p.setDeltaMovement(nv);
			} else if (mc.options.keyShift.isDown()) {
				p.setDeltaMovement(v.scale(0.9));
			}
			if (vertical.value && mc.options.keyJump.isDown()) {
				Vec3 c = p.getDeltaMovement();
				p.setDeltaMovement(c.x, c.y + climb.value, c.z);
			}
		}
	}

	static class ElytraTarget extends Mod {
		FloatSetting range = num("Menzil", 60f, 10f, 120f);
		FloatSetting smooth = num("Yumusaklik", 5f, 1f, 20f);
		FloatSetting predict = num("Tahmin", 3f, 0f, 10f);
		BoolSetting onlyForward = bool("Sadece ileri tusu", true);

		ElytraTarget() {
			super("ElytraTarget", "Elytra", -1);
		}

		@Override
		void onTick(Minecraft mc, LocalPlayer p) {
			if (mc.screen != null || !p.isFallFlying()) return;
			if (onlyForward.value && !mc.options.keyUp.isDown()) return;
			Player best = null;
			double bd = range.value;
			for (Player o : mc.level.players()) {
				if (o == p || o.isSpectator() || !o.isAlive()) continue;
				double d = p.distanceTo(o);
				if (d < bd) {
					bd = d;
					best = o;
				}
			}
			if (best == null) return;
			Vec3 tp = best.position()
				.add(best.getDeltaMovement().scale(predict.value))
				.add(0, best.getBbHeight() * 0.5, 0);
			float[] rot = rotationTo(p, tp);
			float sm = Math.max(1f, smooth.value);
			p.setYRot(p.getYRot() + Mth.wrapDegrees(rot[0] - p.getYRot()) / sm);
			p.setXRot(Mth.clamp(p.getXRot() + (rot[1] - p.getXRot()) / sm, -90f, 90f));
		}
	}

	static class ElytraHelper extends Mod {
		BoolSetting autoSwap = bool("Oto takas", true);
		ModeSetting swapMode = mode("Takas modu", 0, "Envanter", "Hotbar (tek tik)");
		BoolSetting swapBack = bool("Inince geri tak", true);
		FloatSetting minHeight = num("Min yukseklik", 4f, 1f, 20f);
		FloatSetting fallSpeed = num("Dusme hizi", 0.4f, 0.1f, 1.5f);
		ModeSetting rocket = mode("Roket", 0, "Kapali", "Hiz dusunce", "Surekli");
		FloatSetting fwDelay = num("Roket araligi", 40f, 10f, 100f);
		FloatSetting fwSpeed = num("Roket min hiz", 0.8f, 0.2f, 2f);
		BoolSetting silentFw = bool("Silent roket", true);
		int cd = 0;
		int fwCd = 0;

		ElytraHelper() {
			super("ElytraHelper", "Elytra", -1);
		}

		@Override
		void onTick(Minecraft mc, LocalPlayer p) {
			if (mc.screen != null) return;
			if (cd > 0) cd--;
			if (fwCd > 0) fwCd--;
			boolean hotbar = swapMode.index == 1;
			boolean elytraOn = p.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA);
			boolean falling = !p.onGround() && !p.isFallFlying() && !p.isInWater()
				&& p.getDeltaMovement().y < -fallSpeed.value;

			if (autoSwap.value && falling && cd == 0 && airBelow(mc, p, (int) minHeight.value)) {
				if (elytraOn) {
					if (p.tryToStartFallFlying()) {
						p.connection.send(new ServerboundPlayerCommandPacket(
							p, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
					}
					cd = 6;
				} else if (swapChest(mc, p, true, hotbar)) {
					cd = 4;
				} else {
					cd = 20;
				}
			}

			if (swapBack.value && elytraOn && p.onGround() && cd == 0) {
				swapChest(mc, p, false, hotbar);
				cd = 20;
			}

			if (rocket.index != 0 && p.isFallFlying() && fwCd == 0) {
				boolean need = rocket.index == 2 || p.getDeltaMovement().length() < fwSpeed.value;
				if (need && useFirework(mc, p, silentFw.value)) {
					fwCd = (int) fwDelay.value;
				}
			}
		}
	}

	static class ElytraFirework extends Mod {
		BoolSetting silent = bool("Silent", true);

		ElytraFirework() {
			super("ElytraFirework", "Elytra", -1);
		}

		@Override
		void onPress(Minecraft mc) {
			LocalPlayer p = mc.player;
			if (p == null || mc.screen != null || !p.isFallFlying()) return;
			if (useFirework(mc, p, silent.value) && mc.player != null) {
				mc.player.displayClientMessage(Component.literal("Roket kullanildi"), true);
			}
		}

		@Override
		void toggle(Minecraft mc) {
			onPress(mc);
		}
	}

	// =====================================================
	//  MOVE
	// =====================================================
	static class AutoSprint extends Mod {
		AutoSprint() {
			super("AutoSprint", "Move", GLFW.GLFW_KEY_H);
		}

		@Override
		void onTick(Minecraft mc, LocalPlayer p) {
			if (mc.screen != null) return;
			if (mc.options.keyUp.isDown() && !p.isShiftKeyDown()
					&& !p.horizontalCollision && p.getFoodData().getFoodLevel() > 6) {
				p.setSprinting(true);
			}
		}
	}

	static class NoFall extends Mod {
		NoFall() {
			super("NoFall", "Move", -1);
		}

		@Override
		void onTick(Minecraft mc, LocalPlayer p) {
			if (p.fallDistance > 2.5f && !p.isFallFlying()) {
				p.connection.send(new ServerboundMovePlayerPacket.Pos(p.getX(), p.getY(), p.getZ(), true));
			}
		}
	}

	static class Speed extends Mod {
		FloatSetting speed = num("Hiz", 0.28f, 0.1f, 1.0f);
		BoolSetting bhop = bool("Bhop (ziplama)", true);

		Speed() {
			super("Speed", "Move", -1);
		}

		@Override
		void onTick(Minecraft mc, LocalPlayer p) {
			if (mc.screen != null || p.isFallFlying() || p.isInWater()) return;
			if (p.getAbilities().flying) return;
			float fwd = (mc.options.keyUp.isDown() ? 1f : 0f) - (mc.options.keyDown.isDown() ? 1f : 0f);
			float str = (mc.options.keyLeft.isDown() ? 1f : 0f) - (mc.options.keyRight.isDown() ? 1f : 0f);
			if (fwd == 0f && str == 0f) return;
			double len = Math.sqrt(fwd * fwd + str * str);
			double f = fwd / len;
			double st = str / len;
			double yaw = Math.toRadians(p.getYRot());
			double sin = Math.sin(yaw);
			double cos = Math.cos(yaw);
			double mx = st * cos - f * sin;
			double mz = f * cos + st * sin;
			Vec3 v = p.getDeltaMovement();
			double y = v.y;
			if (bhop.value && p.onGround()) y = 0.42;
			p.setDeltaMovement(mx * speed.value, y, mz * speed.value);
		}
	}

							static class Flight extends Mod {
		FloatSetting speed = num("Hiz", 0.1f, 0.05f, 0.5f);

		Flight() {
			super("Flight", "Move", -1);
		}

		@Override
		void onTick(Minecraft mc, LocalPlayer p) {
			p.getAbilities().mayfly = true;
			p.getAbilities().flying = true;
			p.getAbilities().setFlyingSpeed(speed.value);
		}

		@Override
		void onDisable(Minecraft mc) {
			LocalPlayer p = mc.player;
			if (p == null) return;
			p.getAbilities().setFlyingSpeed(0.05f);
			if (!p.isCreative() && !p.isSpectator()) {
				p.getAbilities().mayfly = false;
				p.getAbilities().flying = false;
			}
		}
	}

	// =====================================================
	//  RENDER
	// =====================================================
	static class Fullbright extends Mod {
		Fullbright() {
			super("Fullbright", "Render", GLFW.GLFW_KEY_G);
		}

		@Override
		void onTick(Minecraft mc, LocalPlayer p) {
			p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 1000, 0, false, false, false));
		}

		@Override
		void onDisable(Minecraft mc) {
			if (mc.player != null) mc.player.removeEffect(MobEffects.NIGHT_VISION);
		}
	}

	static class FovChanger extends Mod {
		FloatSetting fov = num("FOV", 30f, 5f, 110f);
		int old = -1;

		FovChanger() {
			super("Zoom", "Render", -1);
		}

		@Override
		void onEnable(Minecraft mc) {
			old = mc.options.fov().get();
		}

		@Override
		void onTick(Minecraft mc, LocalPlayer p) {
			mc.options.fov().set((int) fov.value);
		}

		@Override
		void onDisable(Minecraft mc) {
			if (old > 0) mc.options.fov().set(old);
		}
	}

	static class Esp extends Mod {
		Esp() {
			super("ESP", "Render", -1);
		}

		@Override
		void onTick(Minecraft mc, LocalPlayer p) {
			for (Player o : mc.level.players()) {
				if (o != p) o.setGlowingTag(true);
			}
		}

		@Override
		void onDisable(Minecraft mc) {
			if (mc.level == null) return;
			for (Player o : mc.level.players()) {
				o.setGlowingTag(false);
			}
		}
	}

	// =====================================================
	//  PLAYER
	// =====================================================
	static class AutoRespawn extends Mod {
		AutoRespawn() {
			super("AutoRespawn", "Player", -1);
		}

		@Override
		void onTick(Minecraft mc, LocalPlayer p) {
			if (p.isDeadOrDying()) p.respawn();
		}
	}

	static class ChestStealer extends Mod {
		FloatSetting speed = num("Hiz", 1f, 1f, 6f);

		ChestStealer() {
			super("ChestStealer", "Player", -1);
		}

		@Override
		void onTick(Minecraft mc, LocalPlayer p) {
			if (!(p.containerMenu instanceof ChestMenu menu)) return;
			int total = menu.getRowCount() * 9;
			int moved = 0;
			int limit = (int) speed.value;
			for (int i = 0; i < total && moved < limit; i++) {
				if (!menu.getSlot(i).getItem().isEmpty()) {
					mc.gameMode.handleInventoryMouseClick(
						menu.containerId, i, 0, ClickType.QUICK_MOVE, p);
					moved++;
				}
			}
		}
	}

	static class AntiAfk extends Mod {
		AntiAfk() {
			super("AntiAFK", "Player", -1);
		}

		@Override
		void onTick(Minecraft mc, LocalPlayer p) {
			if (mc.screen != null) return;
			if (tick % 400 == 0) {
				p.swing(InteractionHand.MAIN_HAND);
				p.setYRot(p.getYRot() + 4f);
			}
		}
	}

	static class AutoWalk extends Mod {
		AutoWalk() {
			super("AutoWalk", "Player", -1);
		}

		@Override
		void onTick(Minecraft mc, LocalPlayer p) {
			if (mc.screen == null) mc.options.keyUp.setDown(true);
		}

		@Override
		void onDisable(Minecraft mc) {
			mc.options.keyUp.setDown(false);
		}
	}

	static class AntiBlind extends Mod {
		AntiBlind() {
			super("AntiBlind", "Player", -1);
		}

		@Override
		void onTick(Minecraft mc, LocalPlayer p) {
			p.removeEffect(MobEffects.BLINDNESS);
			p.removeEffect(MobEffects.DARKNESS);
		}
	}

	static class AutoTool extends Mod {
		AutoTool() {
			super("AutoTool", "Player", -1);
		}

		@Override
		void onTick(Minecraft mc, LocalPlayer p) {
			if (mc.screen != null || !mc.options.keyAttack.isDown()) return;
			if (!(mc.hitResult instanceof BlockHitResult bhr) || bhr.getType() != HitResult.Type.BLOCK) return;
			BlockState st = mc.level.getBlockState(bhr.getBlockPos());
			float bestSpeed = p.getInventory().getItem(p.getInventory().selected).getDestroySpeed(st);
			int bestSlot = -1;
			for (int i = 0; i < 9; i++) {
				float sp = p.getInventory().getItem(i).getDestroySpeed(st);
				if (sp > bestSpeed) {
					bestSpeed = sp;
					bestSlot = i;
				}
			}
			if (bestSlot != -1) p.getInventory().selected = bestSlot;
		}
	}

	// =====================================================
	//  BEDWARS
	// =====================================================
	static class Scaffold extends Mod {
		FloatSetting delay = num("Gecikme (tick)", 1f, 0f, 6f);
		BoolSetting autoSwitch = bool("Blok sec", true);
		BoolSetting swing = bool("El salla", true);
		int cd = 0;

		Scaffold() {
			super("Scaffold", "Bedwars", -1);
		}

		@Override
		void onTick(Minecraft mc, LocalPlayer p) {
			if (mc.screen != null) return;
			if (cd > 0) {
				cd--;
				return;
			}
			BlockPos below = p.blockPosition().below();
			if (!mc.level.getBlockState(below).isAir()) return;

			if (autoSwitch.value) {
				for (int i = 0; i < 9; i++) {
					if (p.getInventory().getItem(i).getItem() instanceof BlockItem) {
						p.getInventory().selected = i;
						break;
					}
				}
			}
			if (!(p.getMainHandItem().getItem() instanceof BlockItem)) return;

			for (Direction d : Direction.values()) {
				BlockPos np = below.relative(d);
				if (mc.level.getBlockState(np).isAir()) continue;
				Direction face = d.getOpposite();
				Vec3 hitVec = Vec3.atCenterOf(np).add(
					face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5);
				BlockHitResult hr = new BlockHitResult(hitVec, face, np, false);
				mc.gameMode.useItemOn(p, InteractionHand.MAIN_HAND, hr);
				if (swing.value) p.swing(InteractionHand.MAIN_HAND);
				cd = (int) delay.value;
				break;
			}
		}
	}

	static class ResourceGlow extends Mod {
		BoolSetting diamond = bool("Elmas", true);
		BoolSetting emerald = bool("Zumrut", true);
		BoolSetting gold = bool("Altin", true);
		BoolSetting iron = bool("Demir", false);
		FloatSetting range = num("Menzil", 40f, 10f, 100f);

		ResourceGlow() {
			super("ResourceGlow", "Bedwars", -1);
		}

		private boolean wanted(ItemStack st) {
			return (diamond.value && st.is(Items.DIAMOND))
				|| (emerald.value && st.is(Items.EMERALD))
				|| (gold.value && st.is(Items.GOLD_INGOT))
				|| (iron.value && st.is(Items.IRON_INGOT));
		}

		@Override
		void onTick(Minecraft mc, LocalPlayer p) {
			if (tick % 5 != 0) return;
			List<ItemEntity> list = mc.level.getEntitiesOfClass(
				ItemEntity.class, p.getBoundingBox().inflate(range.value), x -> true);
			for (ItemEntity it : list) {
				it.setGlowingTag(wanted(it.getItem()));
			}
		}

		@Override
		void onDisable(Minecraft mc) {
			if (mc.level == null || mc.player == null) return;
			List<ItemEntity> list = mc.level.getEntitiesOfClass(
				ItemEntity.class, mc.player.getBoundingBox().inflate(range.value), x -> true);
			for (ItemEntity it : list) {
				it.setGlowingTag(false);
			}
		}
	}

				 	static class BedFinder extends Mod {
		FloatSetting radius = num("Yaricap", 16f, 4f, 32f);

		BedFinder() {
			super("BedFinder", "Bedwars", -1);
		}

		@Override
		void onTick(Minecraft mc, LocalPlayer p) {
			if (tick % 20 != 0) return;
			int r = (int) radius.value;
			int ry = Math.min(r, 10);
			BlockPos c = p.blockPosition();
			double best = Double.MAX_VALUE;
			BlockPos found = null;
			for (int x = -r; x <= r; x++) {
				for (int y = -ry; y <= ry; y++) {
					for (int z = -r; z <= r; z++) {
						BlockPos pos = c.offset(x, y, z);
						if (mc.level.getBlockState(pos).is(BlockTags.BEDS)) {
							double d = pos.distSqr(c);
							if (d < best) {
								best = d;
								found = pos;
							}
						}
					}
				}
			}
			bedPos = found;
			bedDist = found == null ? -1 : Math.sqrt(best);
		}

		@Override
		void onDisable(Minecraft mc) {
			bedPos = null;
			bedDist = -1;
		}
	}

	static class InvisEsp extends Mod {
		InvisEsp() {
			super("InvisESP", "Bedwars", -1);
		}

		@Override
		void onTick(Minecraft mc, LocalPlayer p) {
			for (Player o : mc.level.players()) {
				if (o != p && o.isInvisible()) o.setGlowingTag(true);
			}
		}

		@Override
		void onDisable(Minecraft mc) {
			if (mc.level == null) return;
			for (Player o : mc.level.players()) {
				if (o.isInvisible()) o.setGlowingTag(false);
			}
		}
	}

	// =====================================================
	//  CLIENT
	// =====================================================
	static class ClickGuiMod extends Mod {
		ClickGuiMod() {
			super("ClickGUI", "Client", GLFW.GLFW_KEY_RIGHT_SHIFT);
		}

		@Override
		void onPress(Minecraft mc) {
			if (mc.screen == null) mc.setScreen(new ClickScreen());
		}

		@Override
		void toggle(Minecraft mc) {
			mc.setScreen(new ClickScreen());
		}
	}

	static class HudMod extends Mod {
		BoolSetting watermark = bool("Isim yazisi", true);
		BoolSetting arrayList = bool("Modul listesi", true);
		BoolSetting splash = bool("Acilis yazisi", true);
		BoolSetting targetHud = bool("Hedef paneli", true);
		BoolSetting coords = bool("Koordinat", true);
		BoolSetting dir = bool("Yon", false);
		BoolSetting fps = bool("FPS", true);
		BoolSetting ping = bool("Ping", true);
		BoolSetting speed = bool("Hiz (blok/sn)", false);
		BoolSetting time = bool("Saat", false);
		BoolSetting counters = bool("Totem/Roket sayaci", true);
		BoolSetting bed = bool("Yatak bilgisi", true);

		HudMod() {
			super("HUD", "Client", -1);
			enabled = true;
		}
	}

	// =====================================================
	//  CLICKGUI MENU (ThunderHack tarzi)
	// =====================================================
	static class Row {
		final int type; // 0 modul, 1 bool, 2 slider, 3 bind
		final Mod m;
		final Setting s;
		final int y;
		final int h;

		Row(int type, Mod m, Setting s, int y, int h) {
			this.type = type;
			this.m = m;
			this.s = s;
			this.y = y;
			this.h = h;
		}
	}

	public static class ClickScreen extends Screen {
		static final String[] TABS = { "Combat", "Elytra", "Move", "Render", "Player", "Bedwars", "Client" };
		static String tab = "Combat";
		static double scroll = 0;

		Mod listening = null;
		FloatSetting dragSetting = null;
		double downX = 0;
		double downY = 0;
		boolean moved = false;
		boolean ignoreRelease = false;
		int pw = 0;
		int px = 0;
		int top = 38;
		int bottom = 100;
		int contentH = 0;

		public ClickScreen() {
			super(Component.literal("KediClient"));
		}

		@Override
		public boolean isPauseScreen() {
			return false;
		}

		private String fit(String t, int w) {
			if (this.font.width(t) <= w - 2) return t;
			return t.substring(0, Math.min(3, t.length()));
		}

		private void calc() {
			pw = Math.min(310, this.width - 8);
			px = (this.width - pw) / 2;
			top = 38;
			bottom = this.height - 6;
		}

		private List<Row> buildRows() {
			List<Row> list = new ArrayList<>();
			int y = 0;
			for (Mod m : MODS) {
				if (!m.category.equals(tab)) continue;
				list.add(new Row(0, m, null, y, 16));
				y += 16;
				if (m.expanded) {
					for (Setting s : m.settings) {
						list.add(new Row(s instanceof FloatSetting ? 2 : (s instanceof ModeSetting ? 4 : 1), m, s, y, 14));
						y += 14;
					}
					list.add(new Row(3, m, null, y, 14));
					y += 14;
					y += 2;
				}
			}
			contentH = y;
			return list;
		}

		private void clampScroll() {
			double maxScroll = Math.max(0, contentH - (bottom - top));
			if (scroll > maxScroll) scroll = maxScroll;
			if (scroll < 0) scroll = 0;
		}

		@Override
		public void render(GuiGraphics g, int mx, int my, float pt) {
			calc();
			g.fill(0, 0, this.width, this.height, 0x99000000);

			// baslik
			g.fill(px, 4, px + pw, 20, 0xF0140C28);
			g.fill(px, 4, px + 2, 20, ACCENT);
			g.drawString(this.font, "KediClient", px + 8, 8, ACCENT);
			g.drawString(this.font, "x", px + pw - 10, 8, 0xFFFFFFFF);

			// sekmeler
			int tw = pw / TABS.length;
			for (int i = 0; i < TABS.length; i++) {
				int tx = px + i * tw;
				boolean sel = TABS[i].equals(tab);
				g.fill(tx, 22, tx + tw - 1, 35, sel ? 0xFF3A2670 : 0xE0181028);
				g.drawCenteredString(this.font, fit(TABS[i], tw), tx + tw / 2, 25,
					sel ? 0xFFFFFFFF : 0xFFAAAAAA);
			}

			// icerik
			g.fill(px, top, px + pw, bottom, 0xD00C0816);
			List<Row> rows = buildRows();
			clampScroll();
			g.enableScissor(px, top, px + pw, bottom);
			for (Row r : rows) {
				int ry = top + r.y - (int) scroll;
				if (ry + r.h < top || ry > bottom) continue;
				drawRow(g, r, ry);
			}
			g.disableScissor();
		}

		private void drawRow(GuiGraphics g, Row r, int ry) {
			if (r.type == 0) {
				g.fill(px + 2, ry, px + pw - 2, ry + 15, r.m.enabled ? 0xFF2D1F55 : 0xFF14102A);
				if (r.m.enabled) g.fill(px + 2, ry, px + 4, ry + 15, ACCENT);
				g.drawString(this.font, r.m.name, px + 8, ry + 4,
					r.m.enabled ? 0xFFFFFFFF : 0xFFBBBBBB);
				String b = r.m.key < 0 ? "" : "[" + keyName(r.m.key) + "]";
				int bw = this.font.width(b);
				g.drawString(this.font, b, px + pw - 22 - bw, ry + 4, 0xFF8888AA);
				g.drawString(this.font, r.m.expanded ? "-" : "+", px + pw - 12, ry + 4, 0xFFFFFFFF);
			} else if (r.type == 1) {
				BoolSetting b = (BoolSetting) r.s;
				g.fill(px + 6, ry, px + pw - 6, ry + 13, 0xFF100C20);
				g.drawString(this.font, b.name, px + 10, ry + 3, 0xFFCCCCCC);
				String t = b.value ? "ON" : "OFF";
				g.drawString(this.font, t, px + pw - 12 - this.font.width(t), ry + 3,
					b.value ? 0xFF7CFFB0 : 0xFFFF7C7C);
			} else if (r.type == 2) {
				FloatSetting f = (FloatSetting) r.s;
				int x0 = px + 6;
				int x1 = px + pw - 6;
				g.fill(x0, ry, x1, ry + 13, 0xFF100C20);
				float frac = (f.value - f.min) / (f.max - f.min);
				g.fill(x0, ry, x0 + (int) ((x1 - x0) * frac), ry + 13, 0xFF4B2F9E);
				g.drawString(this.font, f.name + ": " + String.format("%.2f", f.value),
					x0 + 4, ry + 3, 0xFFFFFFFF);
			} else if (r.type == 4) {
				ModeSetting ms = (ModeSetting) r.s;
				g.fill(px + 6, ry, px + pw - 6, ry + 13, 0xFF100C20);
				g.drawString(this.font, ms.name, px + 10, ry + 3, 0xFFCCCCCC);
				String cur = ms.options[ms.index];
				g.drawString(this.font, cur, px + pw - 12 - this.font.width(cur), ry + 3, ACCENT);
			} else {
				g.fill(px + 6, ry, px + pw - 6, ry + 13, 0xFF100C20);
				String t = listening == r.m
					? "Bind: > tusa veya orta tiga bas <"
					: "Bind: " + keyName(r.m.key);
				g.drawString(this.font, t, px + 10, ry + 3, 0xFFFFD27C);
			}
		}

				 		private void setSlider(FloatSetting fs, double mx) {
			double x0 = px + 6;
			double x1 = px + pw - 6;
			double f = Math.max(0.0, Math.min(1.0, (mx - x0) / (x1 - x0)));
			float v = (float) (fs.min + f * (fs.max - fs.min));
			fs.value = Math.round(v * 100f) / 100f;
		}

		@Override
		public boolean mouseClicked(double mx, double my, int button) {
			calc();
			downX = mx;
			downY = my;
			moved = false;
			ignoreRelease = false;
			dragSetting = null;

			if (listening != null) {
				if (button >= 2) listening.key = 1000 + button;
				listening = null;
				ignoreRelease = true;
				return true;
			}
			if (my >= 4 && my < 20 && mx >= px + pw - 18 && mx < px + pw) {
				ignoreRelease = true;
				onClose();
				return true;
			}
			if (my >= 22 && my < 35 && mx >= px && mx < px + pw) {
				int i = (int) ((mx - px) / (pw / TABS.length));
				if (i >= 0 && i < TABS.length) {
					tab = TABS[i];
					scroll = 0;
				}
				ignoreRelease = true;
				return true;
			}
			if (my >= top && my < bottom && mx >= px && mx < px + pw) {
				for (Row r : buildRows()) {
					int ry = top + r.y - (int) scroll;
					if (r.type == 2 && my >= ry && my < ry + r.h) {
						dragSetting = (FloatSetting) r.s;
						setSlider(dragSetting, mx);
						ignoreRelease = true;
						return true;
					}
				}
			}
			return true;
		}

		@Override
		public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
			if (dragSetting != null) {
				setSlider(dragSetting, mx);
				return true;
			}
			if (Math.abs(mx - downX) > 3 || Math.abs(my - downY) > 3) moved = true;
			if (moved) {
				scroll -= dy;
				clampScroll();
			}
			return true;
		}

		@Override
		public boolean mouseReleased(double mx, double my, int button) {
			if (dragSetting != null) {
				dragSetting = null;
				saveConfig();
				return true;
			}
			if (ignoreRelease || moved) {
				ignoreRelease = false;
				return true;
			}
			if (my >= top && my < bottom && mx >= px && mx < px + pw) {
				for (Row r : buildRows()) {
					int ry = top + r.y - (int) scroll;
					if (my >= ry && my < ry + r.h) {
						if (r.type == 0) {
							if (button == 1 || mx >= px + pw - 24) {
								r.m.expanded = !r.m.expanded;
							} else {
								r.m.toggle(this.minecraft);
							}
						} else if (r.type == 1) {
							BoolSetting b = (BoolSetting) r.s;
							b.value = !b.value;
						} else if (r.type == 4) {
							ModeSetting ms = (ModeSetting) r.s;
							ms.index = (ms.index + 1) % ms.options.length;
						} else if (r.type == 3) {
							listening = r.m;
						}
						break;
					}
				}
			}
			return true;
		}

		public boolean mouseScrolled(double mx, double my, double sx, double sy) {
			scroll -= sy * 14;
			clampScroll();
			return true;
		}

		@Override
		public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
			if (listening != null) {
				if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
					listening = null;
				} else if (keyCode == GLFW.GLFW_KEY_DELETE || keyCode == GLFW.GLFW_KEY_BACKSPACE) {
					listening.key = -1;
					listening = null;
				} else {
					listening.key = keyCode;
					listening = null;
				}
				return true;
			}
			return super.keyPressed(keyCode, scanCode, modifiers);
		}

		@Override
		public void onClose() {
			saveConfig();
			super.onClose();
		}
	}
					}
