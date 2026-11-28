package com.example.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.client.AttackIndicatorStatus;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.EntityHitResult;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class KediClient implements ClientModInitializer {
	static final int ACCENT = 0xFF7CD6FF;
	static KeyMapping menuKey;
	static KeyMapping zoomKey;
	static KeyMapping quickArmorKey;
	static KeyMapping quickWeaponKey;

	static HudCat hud;
	static CombatCat combat;
	static RenderCat render;
	static MiscCat misc;

	static int frames = 0;
	static int fpsValue = 0;
	static long fpsStart = 0;
	static final List<Long> leftClicks = new ArrayList<>();
	static final List<Long> rightClicks = new ArrayList<>();
	static boolean lastLeft = false;
	static boolean lastRight = false;
	static boolean zooming = false;
	static int normalFov = -1;

	@Override
	public void onInitializeClient() {
		menuKey = key("Ana Menu", GLFW.GLFW_KEY_0);
		zoomKey = key("Zoom", GLFW.GLFW_KEY_C);
		quickArmorKey = key("Hizli Zirh", GLFW.GLFW_KEY_RIGHT_ALT);
		quickWeaponKey = key("Hizli Silah", GLFW.GLFW_KEY_X);

		hud = new HudCat();
		combat = new CombatCat();
		render = new RenderCat();
		misc = new MiscCat();

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			LocalPlayer p = client.player;
			if (p == null) return;

			long win = client.getWindow().getWindow();
			long now = System.currentTimeMillis();
			boolean lDown = GLFW.glfwGetMouseButton(win, 0) == GLFW.GLFW_PRESS;
			boolean rDown = GLFW.glfwGetMouseButton(win, 1) == GLFW.GLFW_PRESS;
			if (client.screen == null) {
				if (lDown && !lastLeft) leftClicks.add(now);
				if (rDown && !lastRight) rightClicks.add(now);
			}
			lastLeft = lDown;
			lastRight = rDown;
			leftClicks.removeIf(t -> now - t > 1000);
			rightClicks.removeIf(t -> now - t > 1000);

			while (menuKey.consumeClick()) {
				if (client.screen == null) client.setScreen(new KediScreen());
			}
			if (client.screen == null) {
				while (quickArmorKey.consumeClick()) {
					quickEquipArmor(client, p);
				}
				while (quickWeaponKey.consumeClick()) {
					quickEquipWeapon(p);
				}
			}

			boolean zoomDown = isDown(win, zoomKey) && client.screen == null;
			if (zoomDown && !zooming) {
				normalFov = client.options.fov().get();
				client.options.fov().set((int) render.zoomFov.value);
				zooming = true;
			} else if (!zoomDown && zooming) {
				if (normalFov > 0) client.options.fov().set(normalFov);
				zooming = false;
			}

			if (render.fullbright.value) {
				p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 1000, 0, false, false, false));
			}
		});

		HudRenderCallback.EVENT.register((g, delta) -> renderHud(g));

		AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
			if (world.isClientSide()) {
				if (render.hitParticles.value) spawnHitParticles(entity);
				if (misc.clickSounds.value) playClick(true);
			}
			return InteractionResult.PASS;
		});

		UseItemCallback.EVENT.register((player, world, hand) -> {
			if (world.isClientSide() && misc.clickSounds.value) {
				playClick(false);
			}
			return InteractionResultHolder.pass(player.getItemInHand(hand));
		});
	}

	// =====================================================
	//  YARDIMCILAR
	// =====================================================
	static KeyMapping key(String name, int code) {
		return KeyBindingHelper.registerKeyBinding(new KeyMapping(
			name, InputConstants.Type.KEYSYM, code, "KediClient"));
	}

	static boolean isDown(long win, KeyMapping km) {
		InputConstants.Key k = km.getKey();
		if (k.getType() != InputConstants.Type.KEYSYM) return km.isDown();
		return GLFW.glfwGetKey(win, k.getValue()) == GLFW.GLFW_PRESS;
	}

	static void playClick(boolean attack) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) return;
		mc.getSoundManager().play(SimpleSoundInstance.forUI(
			attack ? SoundEvents.UI_BUTTON_CLICK.value() : SoundEvents.UI_BUTTON_CLICK.value(), 1.4f, 0.6f));
	}

	static void spawnHitParticles(net.minecraft.world.entity.Entity e) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) return;
		ParticleOptions opt = particleFor(render.particleType.index);
		for (int i = 0; i < (int) render.particleAmount.value; i++) {
			double rx = (Math.random() - 0.5) * 0.8;
			double ry = Math.random() * e.getBbHeight();
			double rz = (Math.random() - 0.5) * 0.8;
			mc.level.addParticle(opt, e.getX() + rx, e.getY() + ry, e.getZ() + rz, 0, 0.1, 0);
		}
	}

	static final String[] PARTICLE_NAMES = { "Kritik", "Kalp", "Yildiz", "Buyu", "Yesil", "Alev" };

	static ParticleOptions particleFor(int i) {
		switch (i) {
			case 1:
				return ParticleTypes.HEART;
			case 2:
				return ParticleTypes.END_ROD;
			case 3:
				return ParticleTypes.ENCHANT;
			case 4:
				return ParticleTypes.HAPPY_VILLAGER;
			case 5:
				return ParticleTypes.FLAME;
			default:
				return ParticleTypes.CRIT;
		}
	}

				static void quickEquipWeapon(LocalPlayer p) {
		Item[] order = {
			Items.NETHERITE_SWORD, Items.DIAMOND_SWORD, Items.IRON_SWORD,
			Items.STONE_SWORD, Items.GOLDEN_SWORD, Items.WOODEN_SWORD
		};
		for (Item it : order) {
			for (int i = 0; i < 9; i++) {
				if (p.getInventory().getItem(i).is(it)) {
					p.getInventory().selected = i;
					p.displayClientMessage(Component.literal("Silah secildi"), true);
					return;
				}
			}
		}
		p.displayClientMessage(Component.literal("Hotbar'da kilic yok"), true);
	}

	static int armorScore(ItemStack st) {
		if (!(st.getItem() instanceof ArmorItem a)) return -1;
		return a.getDefense() * 100 + EnchantmentHelper.getDamageProtection(List.of(st), null).hashCode() % 10;
	}

	static void quickEquipArmor(Minecraft mc, LocalPlayer p) {
		int[] slotIdx = { 5, 6, 7, 8 };
		EquipmentSlot[] slots = { EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET };
		boolean any = false;
		for (int s = 0; s < 4; s++) {
			ItemStack cur = p.getItemBySlot(slots[s]);
			int curScore = cur.isEmpty() ? -1 : baseDefense(cur);
			int bestIdx = -1;
			int bestScore = curScore;
			for (int i = 9; i <= 44; i++) {
				ItemStack st = p.inventoryMenu.getSlot(i).getItem();
				if (st.getItem() instanceof ArmorItem a && a.getEquipmentSlot() == slots[s]) {
					int sc = baseDefense(st);
					if (sc > bestScore) {
						bestScore = sc;
						bestIdx = i;
					}
				}
			}
			if (bestIdx != -1) {
				mc.gameMode.handleInventoryMouseClick(
					p.inventoryMenu.containerId, bestIdx, slotIdx[s], ClickType.SWAP, p);
				any = true;
			}
		}
		p.displayClientMessage(Component.literal(any ? "Zirh guncellendi" : "Daha iyi zirh yok"), true);
	}

	static int baseDefense(ItemStack st) {
		if (!(st.getItem() instanceof ArmorItem a)) return -1;
		return a.getDefense();
	}

	// =====================================================
	//  AYAR SINIFLARI
	// =====================================================
	static class BoolSetting {
		final String name;
		boolean value;

		BoolSetting(String name, boolean value) {
			this.name = name;
			this.value = value;
		}
	}

	static class IntSetting {
		final String name;
		float value;
		final float min;
		final float max;

		IntSetting(String name, float value, float min, float max) {
			this.name = name;
			this.value = value;
			this.min = min;
			this.max = max;
		}
	}

	static class ModeSetting {
		final String name;
		final String[] options;
		int index;

		ModeSetting(String name, int index, String... options) {
			this.name = name;
			this.options = options;
			this.index = index;
		}
	}

	static abstract class Cat {
		final String title;
		final List<Object> rows = new ArrayList<>();

		Cat(String title) {
			this.title = title;
		}

		BoolSetting b(String n, boolean def) {
			BoolSetting s = new BoolSetting(n, def);
			rows.add(s);
			return s;
		}

		IntSetting f(String n, float def, float min, float max) {
			IntSetting s = new IntSetting(n, def, min, max);
			rows.add(s);
			return s;
		}

		ModeSetting m(String n, int def, String... opts) {
			ModeSetting s = new ModeSetting(n, def, opts);
			rows.add(s);
			return s;
		}
	}

	static class HudCat extends Cat {
		BoolSetting coords = b("Koordinat", true);
		BoolSetting direction = b("Yon", false);
		BoolSetting timeDay = b("Saat/Gun", false);
		BoolSetting fps = b("FPS", true);
		BoolSetting cps = b("CPS", true);
		BoolSetting keystrokes = b("Tuslar (WASD)", true);
		BoolSetting ping = b("Ping", true);
		BoolSetting serverInfo = b("Sunucu adi", false);
		BoolSetting heldItem = b("Elindeki esya", true);
		BoolSetting durability = b("Dayaniklilik", true);
		BoolSetting potions = b("Iksir etkileri", true);
		BoolSetting healthNum = b("Can (sayi)", true);
		BoolSetting healthBar = b("Can cubugu", true);
		BoolSetting armorNum = b("Zirh puani", false);
		BoolSetting hungerNum = b("Acliik", false);
		BoolSetting watermark = b("Isim yazisi", true);

		HudCat() {
			super("HUD");
		}
	}

	static class CombatCat extends Cat {
		BoolSetting critIndicator = b("Kritik gostergesi", true);
		BoolSetting reachInfo = b("Hedefe mesafe", true);
		BoolSetting cooldownBar = b("Saldiri soguma barı", true);

		CombatCat() {
			super("Combat");
		}
	}

	static class RenderCat extends Cat {
		BoolSetting fullbright = b("Fullbright", false);
		BoolSetting hitParticles = b("Vurus partikulu", true);
		ModeSetting particleType = new ModeSetting("Partikul turu", 0, PARTICLE_NAMES);
		IntSetting particleAmount = f("Partikul miktari", 8f, 1f, 25f);
		IntSetting zoomFov = f("Zoom FOV", 25f, 5f, 70f);

		RenderCat() {
			super("Render");
			rows.add(particleType);
		}
	}

	static class MiscCat extends Cat {
		BoolSetting clickSounds = b("Tiklama sesi", false);

		MiscCat() {
			super("Diger");
		}
	}

	// =====================================================
	//  HUD CIZIMI
	// =====================================================
	static void renderHud(GuiGraphics g) {
		Minecraft mc = Minecraft.getInstance();
		frames++;
		long now = System.currentTimeMillis();
		if (now - fpsStart >= 1000) {
			fpsValue = frames;
			frames = 0;
			fpsStart = now;
		}
		if (mc.player == null || mc.options.hideGui) return;
		LocalPlayer me = mc.player;
		int sw = mc.getWindow().getGuiScaledWidth();
		int sh = mc.getWindow().getGuiScaledHeight();

		if (hud.watermark.value) {
			String t = "KediClient";
			g.fill(3, 3, 3 + mc.font.width(t) + 8, 15, 0xA0101828);
			g.fill(3, 3, 5, 15, ACCENT);
			g.drawString(mc.font, t, 8, 6, ACCENT);
		}

		List<String> lines = new ArrayList<>();
		if (hud.coords.value) {
			lines.add(String.format("XYZ: %.0f %.0f %.0f", me.getX(), me.getY(), me.getZ()));
		}
		if (hud.direction.value) {
			lines.add("Yon: " + me.getDirection().getName());
		}
		if (hud.timeDay.value && mc.level != null) {
			long t = mc.level.getDayTime() % 24000;
			long day = mc.level.getDayTime() / 24000;
			lines.add(String.format("Gun %d | %02d:%02d", day, ((t / 1000 + 6) % 24), (t % 1000) * 60 / 1000));
		}
		if (hud.fps.value) {
			lines.add("FPS: " + fpsValue);
		}
		if (hud.cps.value) {
			lines.add("CPS: " + leftClicks.size() + " | " + rightClicks.size());
		}
		if (hud.ping.value && mc.getConnection() != null) {
			PlayerInfo pi = mc.getConnection().getPlayerInfo(me.getUUID());
			if (pi != null) lines.add("Ping: " + pi.getLatency() + " ms");
		}
		if (hud.serverInfo.value && mc.getCurrentServer() != null) {
			lines.add("Sunucu: " + mc.getCurrentServer().name);
		}
		if (hud.heldItem.value) {
			ItemStack st = me.getMainHandItem();
			if (!st.isEmpty()) {
				String t = st.getHoverName().getString();
				if (hud.durability.value && st.isDamageableItem()) {
					t = t + " (" + (st.getMaxDamage() - st.getDamageValue()) + "/" + st.getMaxDamage() + ")";
				}
				lines.add("Esya: " + t);
			}
		}
		if (hud.healthNum.value) {
			lines.add(String.format("Can: %.1f / %.1f", me.getHealth(), me.getMaxHealth()));
		}
		if (hud.armorNum.value) {
			lines.add("Zirh: " + me.getArmorValue());
		}
		if (hud.hungerNum.value) {
			lines.add("Acliik: " + me.getFoodData().getFoodLevel() + " / 20");
		}
		if (hud.potions.value) {
			for (MobEffectInstance ef : me.getActiveEffects()) {
				lines.add(ef.getEffect().value().getDisplayName().getString()
					+ " " + (ef.getAmplifier() + 1) + " (" + (ef.getDuration() / 20) + "s)");
			}
		}
		int iy = 20;
		for (String line : lines) {
			int w = mc.font.width(line) + 6;
			g.fill(3, iy, 3 + w, iy + 11, 0x90000000);
			g.fill(3, iy, 5, iy + 11, ACCENT);
			g.drawString(mc.font, line, 8, iy + 2, 0xFFE0E0E0);
			iy += 11;
		}

				if (hud.healthBar.value) {
			int bw = 100;
			int bx = sw / 2 - bw / 2;
			int by = sh - 48;
			float frac = Math.max(0f, Math.min(1f, me.getHealth() / Math.max(1f, me.getMaxHealth())));
			g.fill(bx - 2, by - 2, bx + bw + 2, by + 8, 0x90000000);
			g.fill(bx, by, bx + bw, by + 6, 0xFF401010);
			g.fill(bx, by, bx + (int) (bw * frac), by + 6, frac > 0.5f ? 0xFF55FF55 : 0xFFFF5555);
		}

		if (hud.keystrokes.value) {
			int kx = sw - 56;
			int ky = sh - 70;
			keyBox(g, mc, kx + 18, ky, "W", mc.options.keyUp.isDown());
			keyBox(g, mc, kx, ky + 18, "A", mc.options.keyLeft.isDown());
			keyBox(g, mc, kx + 18, ky + 18, "S", mc.options.keyDown.isDown());
			keyBox(g, mc, kx + 36, ky + 18, "D", mc.options.keyRight.isDown());
			keyBox(g, mc, kx + 18, ky + 36, "SP", mc.options.keyJump.isDown());
		}

		if (combat.critIndicator.value) {
			boolean ready = me.getAttackStrengthScale(0.5f) >= 1.0f;
			boolean crit = ready && !me.onGround() && me.fallDistance > 0f && !me.isInWater();
			int cx = sw / 2;
			int cy = sh / 2;
			int col = crit ? 0xFFFF5555 : (ready ? 0xFF55FF55 : 0xFFAAAAAA);
			g.fill(cx - 10, cy + 10, cx + 10, cy + 12, 0x60000000);
			float scale = Math.min(1f, me.getAttackStrengthScale(0.5f));
			g.fill(cx - 10, cy + 10, cx - 10 + (int) (20 * scale), cy + 12, col);
		}

		if (combat.reachInfo.value && mc.hitResult instanceof EntityHitResult eh) {
			double dist = me.getEyePosition().distanceTo(eh.getLocation());
			String t = String.format("%.2f m", dist);
			g.drawString(mc.font, t, sw / 2 - mc.font.width(t) / 2, sh / 2 + 14, ACCENT);
		}
	}

	static void keyBox(GuiGraphics g, Minecraft mc, int x, int y, String t, boolean down) {
		g.fill(x, y, x + 16, y + 16, down ? 0xC07CD6FF : 0x90000000);
		g.drawString(mc.font, t, x + 8 - mc.font.width(t) / 2, y + 4, 0xFFFFFFFF);
	}

	// =====================================================
	//  MENU
	// =====================================================
	static Cat[] CATS() {
		return new Cat[] { hud, combat, render, misc };
	}

	public static class KediScreen extends Screen {
		static int tabIndex = 0;

		public KediScreen() {
			super(Component.literal("KediClient"));
		}

		@Override
		public boolean isPauseScreen() {
			return false;
		}

		@Override
		protected void init() {
			Cat[] cats = CATS();
			int pw = Math.min(260, this.width - 8);
			int px = (this.width - pw) / 2;
			int tabW = pw / cats.length;
			for (int t = 0; t < cats.length; t++) {
				final int idx = t;
				addRenderableWidget(Button.builder(Component.literal(cats[t].title), b -> {
					tabIndex = idx;
					rebuild();
				}).bounds(px + t * tabW, 20, tabW - 2, 18).build());
			}

			Cat cat = cats[tabIndex];
			int y = 44;
			int s = 22;
			for (Object row : cat.rows) {
				if (row instanceof BoolSetting bs) {
					addRenderableWidget(toggle(px, y, pw, bs));
				} else if (row instanceof IntSetting is) {
					addRenderableWidget(new Slider(px, y, pw, is));
				} else if (row instanceof ModeSetting ms) {
					addRenderableWidget(modeButton(px, y, pw, ms));
				}
				y += s;
			}
			addRenderableWidget(Button.builder(Component.literal("Kapat"), btn -> onClose())
				.bounds(px, Math.min(y + 4, this.height - 24), pw, 20).build());
		}

		private void rebuild() {
			clearWidgets();
			init();
		}

		private Button toggle(int x, int y, int w, BoolSetting b) {
			return Button.builder(blabel(b), btn -> {
				b.value = !b.value;
				btn.setMessage(blabel(b));
			}).bounds(x, y, w, 20).build();
		}

		private Button modeButton(int x, int y, int w, ModeSetting ms) {
			return Button.builder(mlabel(ms), btn -> {
				ms.index = (ms.index + 1) % ms.options.length;
				btn.setMessage(mlabel(ms));
			}).bounds(x, y, w, 20).build();
		}

		private static Component blabel(BoolSetting b) {
			return Component.literal(b.name + ": " + (b.value ? "ACIK" : "KAPALI"));
		}

		private static Component mlabel(ModeSetting m) {
			return Component.literal(m.name + ": " + m.options[m.index]);
		}
	}

	static class Slider extends AbstractSliderButton {
		final IntSetting s;

		Slider(int x, int y, int w, IntSetting s) {
			super(x, y, w, 20, Component.empty(), (s.value - s.min) / (s.max - s.min));
			this.s = s;
			updateMessage();
		}

		@Override
		protected void updateMessage() {
			setMessage(Component.literal(s.name + ": " + String.format("%.0f", s.value)));
		}

		@Override
		protected void applyValue() {
			s.value = (float) (s.min + this.value * (s.max - s.min));
		}
	}
				  }
