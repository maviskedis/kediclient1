package com.example.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

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
	static Esp esp;
	static ResourceEsp resEsp;
	static BedEsp bedEsp;
	static InvisEsp invisEsp;
	static List<ItemEntity> resItems = new ArrayList<>();
	static List<BlockPos> beds = new ArrayList<>();
	static int eatTicks = 0;
	static int eatOld = 0;
	static NameTags nameTags;
	static Aura aura;
	static AntiCheat antiCheat;
	static int wtapTicks = 0;
	static final Map<UUID, Track> tracks = new HashMap<>();
	static final Map<String, String> suspects = new HashMap<>();
	static final Map<String, Long> suspectTick = new HashMap<>();
	static ItemEsp itemEsp;
	static HitParticles hitFx;
	static List<ItemEntity> allItems = new ArrayList<>();
	static final List<Long> leftClicks = new ArrayList<>();
	static final List<Long> rightClicks = new ArrayList<>();
	static boolean lastLeft = false;
	static boolean lastRight = false;

	// =====================================================
	//  BASLANGIC
	// =====================================================
	@Override
	public void onInitializeClient() {
		// Combat
		criticals = new Criticals();
		aura = new Aura();
		MODS.add(aura);
		MODS.add(new TriggerBot());
		MODS.add(criticals);
		MODS.add(new Velocity());
		MODS.add(new AutoTotem());
		MODS.add(new AutoClicker());
		MODS.add(new AutoGApple());
		MODS.add(new AutoPot());
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
		MODS.add(new SafeWalk());
		MODS.add(new Parkour());
		MODS.add(new Glide());
		MODS.add(new FastLadder());
		MODS.add(new InvMove());
		// Render
		MODS.add(new Fullbright());
		esp = new Esp();
		MODS.add(esp);
		MODS.add(new FovChanger());
		nameTags = new NameTags();
		itemEsp = new ItemEsp();
		hitFx = new HitParticles();
		MODS.add(nameTags);
		MODS.add(itemEsp);
		MODS.add(new Particles());
		MODS.add(hitFx);
		MODS.add(new TimeChanger());
		MODS.add(new NoWeather());
		MODS.add(new FpsBoost());
		// Player
		MODS.add(new AutoRespawn());
		MODS.add(new ChestStealer());
		MODS.add(new AntiBlind());
		MODS.add(new AntiAfk());
		MODS.add(new AutoWalk());
		MODS.add(new AutoTool());
		// Bedwars
		resEsp = new ResourceEsp();
		bedEsp = new BedEsp();
		invisEsp = new InvisEsp();
		MODS.add(new Scaffold());
		MODS.add(resEsp);
		MODS.add(bedEsp);
		MODS.add(invisEsp);
		MODS.add(new BlockIn());
		// Macro (Player sekmesinde)
		MODS.add(new MacroMod("Macro1", 1));
		MODS.add(new MacroMod("Macro2", 2));
		MODS.add(new MacroMod("Macro3", 3));
		MODS.add(new MacroMod("Macro4", 8));
		// Client
		MODS.add(new ClickGuiMod());
		hud = new HudMod();
		MODS.add(hud);
		antiCheat = new AntiCheat();
		MODS.add(antiCheat);

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
			long nowT = System.currentTimeMillis();
			boolean lDown = GLFW.glfwGetMouseButton(win, 0) == GLFW.GLFW_PRESS;
			boolean rDown = GLFW.glfwGetMouseButton(win, 1) == GLFW.GLFW_PRESS;
			if (client.screen == null) {
				if (lDown && !lastLeft) leftClicks.add(nowT);
				if (rDown && !lastRight) rightClicks.add(nowT);
			}
			lastLeft = lDown;
			lastRight = rDown;
			leftClicks.removeIf(t0 -> nowT - t0 > 1000);
			rightClicks.removeIf(t0 -> nowT - t0 > 1000);
			for (Mod m : MODS) {
				boolean down = isDown(win, m.key);
				if (down && !m.keyDown && client.screen == null) {
					m.onPress(client);
				}
				m.keyDown = down;
			}

				 			macroTick(client, p);

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

		HudRenderCallback.EVENT.register((g, delta) -> renderHud(g, delta.getGameTimeDeltaPartialTick(true)));
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
	//  EKRANA CIZIM (ESP): dunyadan ekrana donusum
	// =====================================================
	static double[] project(Vec3 cam, float yawDeg, float pitchDeg, double fovDeg, int w, int h, double tx, double ty, double tz) {
		double yaw = Math.toRadians(yawDeg);
		double pit = Math.toRadians(pitchDeg);
		double sy = Math.sin(yaw);
		double cy = Math.cos(yaw);
		double sp = Math.sin(pit);
		double cp = Math.cos(pit);
		double rx = tx - cam.x;
		double ry = ty - cam.y;
		double rz = tz - cam.z;
		double zc = rx * (-sy * cp) + ry * (-sp) + rz * (cy * cp);
		if (zc < 0.05) return null;
		double xc = rx * (-cy) + rz * (-sy);
		double yc = rx * (-sy * sp) + ry * cp + rz * (cy * sp);
		double th = Math.tan(Math.toRadians(fovDeg) / 2.0);
		double aspect = (double) w / (double) h;
		double sx = w / 2.0 + (xc / (zc * th * aspect)) * (w / 2.0);
		double sy2 = h / 2.0 - (yc / (zc * th)) * (h / 2.0);
		return new double[] { sx, sy2, zc };
	}

	static void drawLine(GuiGraphics g, int x0, int y0, int x1, int y1, int color) {
		int steps = Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0));
		if (steps > 1500) return;
		if (steps == 0) {
			g.fill(x0, y0, x0 + 1, y0 + 1, color);
			return;
		}
		for (int i = 0; i <= steps; i++) {
			int x = x0 + (x1 - x0) * i / steps;
			int y = y0 + (y1 - y0) * i / steps;
			g.fill(x, y, x + 1, y + 1, color);
		}
	}

	static void drawBox(GuiGraphics g, int x1, int y1, int x2, int y2, int color) {
		g.fill(x1, y1, x2 + 1, y1 + 1, color);
		g.fill(x1, y2, x2 + 1, y2 + 1, color);
		g.fill(x1, y1, x1 + 1, y2 + 1, color);
		g.fill(x2, y1, x2 + 1, y2 + 1, color);
	}

	static void espEntity(GuiGraphics g, Minecraft mc, Vec3 cam, float yaw, float pitch, double fov,
			int sw, int sh, double ex, double ey, double ez, double halfW, double height,
			String label, int color, float hpFrac) {
		double minX = 1e9;
		double minY = 1e9;
		double maxX = -1e9;
		double maxY = -1e9;
		for (int i = 0; i < 8; i++) {
			double cx = ex + ((i & 1) == 0 ? -halfW : halfW);
			double cyy = ey + ((i & 2) == 0 ? 0 : height);
			double cz = ez + ((i & 4) == 0 ? -halfW : halfW);
			double[] sp = project(cam, yaw, pitch, fov, sw, sh, cx, cyy, cz);
			if (sp == null) return;
			minX = Math.min(minX, sp[0]);
			minY = Math.min(minY, sp[1]);
			maxX = Math.max(maxX, sp[0]);
			maxY = Math.max(maxY, sp[1]);
		}
		if (maxX - minX > sw * 3 || maxY - minY > sh * 3) return;
		int x1 = (int) minX;
		int y1 = (int) minY;
		int x2 = (int) maxX;
		int y2 = (int) maxY;
		int midX = (x1 + x2) / 2;
		if (esp.boxes.value) {
			drawBox(g, x1, y1, x2, y2, color);
		}
		if (hpFrac >= 0f) {
			int bh = (int) ((y2 - y1) * hpFrac);
			g.fill(x1 - 4, y1, x1 - 2, y2 + 1, 0xFF2A2240);
			g.fill(x1 - 4, y2 + 1 - bh, x1 - 2, y2 + 1, hpFrac > 0.5f ? 0xFF7CFFB0 : 0xFFFF7C7C);
		}
		if (esp.tracers.value) {
			drawLine(g, sw / 2, sh, midX, y2, color);
		}
		if (label != null && !label.isEmpty()) {
			g.drawString(mc.font, label, midX - mc.font.width(label) / 2, y1 - 10, color);
		}
	}

		static boolean isValuable(ItemStack st) {
		return st.is(Items.DIAMOND) || st.is(Items.EMERALD) || st.is(Items.GOLD_INGOT)
			|| st.is(Items.NETHERITE_INGOT) || st.is(Items.TOTEM_OF_UNDYING)
			|| st.is(Items.GOLDEN_APPLE) || st.is(Items.ENCHANTED_GOLDEN_APPLE)
			|| st.is(Items.ENDER_PEARL) || st.is(Items.TNT) || st.is(Items.FIRE_CHARGE);
	}

	static void renderEsp(GuiGraphics g, Minecraft mc, LocalPlayer me, float pt, int sw, int sh) {
		if (esp == null || mc.level == null) return;
		boolean doAll = esp.enabled;
		boolean doInv = invisEsp.enabled;
		boolean doRes = resEsp.enabled;
		boolean doBed = bedEsp.enabled;
		boolean doTags = nameTags.enabled;
		boolean doItems = itemEsp.enabled;
		boolean doTarget = aura != null && aura.enabled && aura.showTarget.value
			&& auraTarget != null && auraTarget.isAlive();
		if (!doAll && !doInv && !doRes && !doBed && !doTags && !doItems && !doTarget) return;

		Vec3 cam = me.getEyePosition(pt);
		float yaw = me.getViewYRot(pt);
		float pitch = me.getViewXRot(pt);
		double fovMod = (me.getSpeed() / 0.1 + 1.0) / 2.0;
		if (me.getAbilities().flying) fovMod *= 1.1;
		fovMod = Math.max(0.7, Math.min(1.6, fovMod));
		double fov = mc.options.fov().get() * fovMod * esp.fovMul.value;

		if (doTarget) {
			LivingEntity tg = auraTarget;
			double tx = Mth.lerp(pt, tg.xo, tg.getX());
			double ty = Mth.lerp(pt, tg.yo, tg.getY());
			double tz = Mth.lerp(pt, tg.zo, tg.getZ());
			espEntity(g, mc, cam, yaw, pitch, fov, sw, sh, tx, ty, tz,
				tg.getBbWidth() / 2.0, tg.getBbHeight(), "", 0xFFFF4444, -1f);
		}

		if (doAll || doInv || doTags) {
			for (Player o : mc.level.players()) {
				if (o == me || !o.isAlive()) continue;
				boolean inv = o.isInvisible();
				double dist = me.distanceTo(o);
				double ox = Mth.lerp(pt, o.xo, o.getX());
				double oy = Mth.lerp(pt, o.yo, o.getY());
				double oz = Mth.lerp(pt, o.zo, o.getZ());
				float frac = Mth.clamp(o.getHealth() / Math.max(1f, o.getMaxHealth()), 0f, 1f);
				int color = ACCENT;
				if (inv) {
					color = 0xFFFFD27C;
				} else {
					int tc = o.getTeamColor();
					if (tc != 16777215 && tc != -1) color = 0xFF000000 | tc;
				}
				String oname = o.getName().getString();
				String sus = suspects.get(oname);
				boolean susOn = sus != null && tick - suspectTick.getOrDefault(oname, -9999L) < 1200;
				if (susOn) color = 0xFFFF4444;

				if ((doAll || (doInv && inv)) && dist <= esp.range.value) {
					String label = "";
					if (!doTags) {
						if (esp.names.value) label = o.getName().getString();
						if (esp.health.value) label = label + " " + (int) o.getHealth() + "hp";
						label = label + " " + (int) dist + "m";
						if (susOn) label = label + " [" + sus + "]";
					}
					espEntity(g, mc, cam, yaw, pitch, fov, sw, sh, ox, oy, oz,
						o.getBbWidth() / 2.0, o.getBbHeight(), label, color,
						esp.healthBar.value ? frac : -1f);
				}

				if (doTags && dist <= nameTags.range.value) {
					double[] sp = project(cam, yaw, pitch, fov, sw, sh, ox, oy + o.getBbHeight() + 0.35, oz);
					if (sp != null) {
						String t1 = o.getName().getString();
						if (nameTags.showHealth.value) t1 = t1 + " " + (int) o.getHealth() + "hp";
						if (nameTags.showDist.value) t1 = t1 + " " + (int) dist + "m";
						if (susOn) t1 = t1 + " [" + sus + "]";
						int tw = mc.font.width(t1) + 6;
						int x = (int) sp[0] - tw / 2;
						int y = (int) sp[1] - 12;
						g.fill(x, y, x + tw, y + 11, 0xB0000000);
						g.drawString(mc.font, t1, x + 3, y + 2, color);
						if (nameTags.showBar.value) {
							g.fill(x, y + 11, x + tw, y + 13, 0xFF2A2240);
							g.fill(x, y + 11, x + (int) (tw * frac), y + 13,
								frac > 0.5f ? 0xFF7CFFB0 : 0xFFFF7C7C);
						}
						if (nameTags.showItem.value) {
							ItemStack h = o.getMainHandItem();
							if (!h.isEmpty()) {
								String ht = h.getHoverName().getString();
								g.drawString(mc.font, ht, (int) sp[0] - mc.font.width(ht) / 2, y + 15, 0xFFDDDDDD);
							}
						}
					}
				}
			}
		}

		if (doRes) {
			for (ItemEntity it : resItems) {
				if (it.isRemoved()) continue;
				ItemStack st = it.getItem();
				String nm;
				int col;
				if (resEsp.diamond.value && st.is(Items.DIAMOND)) {
					nm = "Elmas";
					col = 0xFF55FFFF;
				} else if (resEsp.emerald.value && st.is(Items.EMERALD)) {
					nm = "Zumrut";
					col = 0xFF55FF55;
				} else if (resEsp.gold.value && st.is(Items.GOLD_INGOT)) {
					nm = "Altin";
					col = 0xFFFFAA00;
				} else if (resEsp.iron.value && st.is(Items.IRON_INGOT)) {
					nm = "Demir";
					col = 0xFFFFFFFF;
				} else {
					continue;
				}
				double dist = me.distanceTo(it);
				if (dist > resEsp.range.value) continue;
				double ix = Mth.lerp(pt, it.xo, it.getX());
				double iy = Mth.lerp(pt, it.yo, it.getY());
				double iz = Mth.lerp(pt, it.zo, it.getZ());
				double[] sp = project(cam, yaw, pitch, fov, sw, sh, ix, iy + 0.3, iz);
				if (sp == null) continue;
				int x = (int) sp[0];
				int y = (int) sp[1];
				g.fill(x - 2, y - 2, x + 3, y + 3, col);
				String label = nm + " x" + st.getCount() + " " + (int) dist + "m";
				g.drawString(mc.font, label, x - mc.font.width(label) / 2, y - 12, col);
			}
		}

		if (doItems) {
			for (ItemEntity it : allItems) {
				if (it.isRemoved()) continue;
				ItemStack st = it.getItem();
				boolean val = isValuable(st);
				if (itemEsp.onlyValuable.value && !val) continue;
				double dist = me.distanceTo(it);
				if (dist > itemEsp.range.value) continue;
				double ix = Mth.lerp(pt, it.xo, it.getX());
				double iy = Mth.lerp(pt, it.yo, it.getY());
				double iz = Mth.lerp(pt, it.zo, it.getZ());
				double[] sp = project(cam, yaw, pitch, fov, sw, sh, ix, iy + 0.3, iz);
				if (sp == null) continue;
				int x = (int) sp[0];
				int y = (int) sp[1];
				int col = val ? 0xFFFFD27C : 0xFFDDDDDD;
				g.fill(x - 2, y - 2, x + 3, y + 3, col);
				String label = st.getHoverName().getString() + " x" + st.getCount() + " " + (int) dist + "m";
				g.drawString(mc.font, label, x - mc.font.width(label) / 2, y - 12, col);
			}
		}

		if (doBed) {
			for (BlockPos bp : beds) {
				double dist = Math.sqrt(bp.distToCenterSqr(me.getX(), me.getY(), me.getZ()));
				double[] sp = project(cam, yaw, pitch, fov, sw, sh, bp.getX() + 0.5, bp.getY() + 0.5, bp.getZ() + 0.5);
				if (sp == null) continue;
				int x = (int) sp[0];
				int y = (int) sp[1];
				g.fill(x - 3, y - 3, x + 4, y + 4, 0xFFFF5555);
				String label = "YATAK " + (int) dist + "m";
				g.drawString(mc.font, label, x - mc.font.width(label) / 2, y - 13, 0xFFFF5555);
			}
		}
	}

	static void keyBox(GuiGraphics g, Minecraft mc, int x, int y, String t, boolean down) {
		g.fill(x, y, x + 16, y + 16, down ? 0xC0B388FF : 0x90000000);
		g.drawString(mc.font, t, x + 8 - mc.font.width(t) / 2, y + 4, 0xFFFFFFFF);
	}

	// =====================================================
	//  PARTIKUL YARDIMCILARI
	// =====================================================
	static final String[] PARTICLE_NAMES = {
		"Kritik", "Kalp", "Alev", "Yildiz", "Buyu", "Yesil", "Havai fisek", "Vurus"
	};

					static ParticleOptions particleFor(int i) {
		switch (i) {
			case 1:
				return ParticleTypes.HEART;
			case 2:
				return ParticleTypes.FLAME;
			case 3:
				return ParticleTypes.END_ROD;
			case 4:
				return ParticleTypes.ENCHANT;
			case 5:
				return ParticleTypes.HAPPY_VILLAGER;
			case 6:
				return ParticleTypes.FIREWORK;
			case 7:
				return ParticleTypes.ENCHANTED_HIT;
			default:
				return ParticleTypes.CRIT;
		}
	}

	static double rnd() {
		return java.util.concurrent.ThreadLocalRandom.current().nextDouble();
	}

	static void hitEffect(Minecraft mc, Entity e) {
		if (hitFx == null || !hitFx.enabled || mc.level == null) return;
		ParticleOptions opt = particleFor(hitFx.type.index);
		int n = (int) hitFx.amount.value;
		for (int i = 0; i < n; i++) {
			double rx = (rnd() - 0.5) * 0.8;
			double ry = rnd() * e.getBbHeight();
			double rz = (rnd() - 0.5) * 0.8;
			mc.level.addParticle(opt, e.getX() + rx, e.getY() + ry, e.getZ() + rz, 0, 0.1, 0);
		}
	}

	static boolean useDown(Minecraft mc, LocalPlayer p, Item item) {
		int slot = hotbarSlot(p, item);
		if (slot == -1) return false;
		float oldPitch = p.getXRot();
		int old = p.getInventory().selected;
		p.getInventory().selected = slot;
		p.setXRot(90f);
		mc.gameMode.useItem(p, InteractionHand.MAIN_HAND);
		p.setXRot(oldPitch);
		p.getInventory().selected = old;
		return true;
	}

	// =====================================================
	//  MAKRO EYLEMLERI
	// =====================================================
	static final String[] ACTIONS = {
		"Yok", "Pearl at", "Altin elma ye", "Totem tak", "Kilic sec",
		"Blok sec", "Roket kullan", "Elytra tak/cikar", "Su kovasi (asagi)",
		"Ates topu (asagi)", "Pot at (asagi)"
	};

	static int hotbarSlot(LocalPlayer p, Item item) {
		for (int i = 0; i < 9; i++) {
			if (p.getInventory().getItem(i).is(item)) return i;
		}
		return -1;
	}

	static boolean useSilently(Minecraft mc, LocalPlayer p, Item item) {
		int slot = hotbarSlot(p, item);
		if (slot == -1) return false;
		int old = p.getInventory().selected;
		p.getInventory().selected = slot;
		mc.gameMode.useItem(p, InteractionHand.MAIN_HAND);
		p.getInventory().selected = old;
		return true;
	}

	static void startEat(Minecraft mc, LocalPlayer p) {
		if (eatTicks > 0) return;
		int slot = hotbarSlot(p, Items.GOLDEN_APPLE);
		if (slot == -1) slot = hotbarSlot(p, Items.ENCHANTED_GOLDEN_APPLE);
		if (slot == -1) return;
		eatOld = p.getInventory().selected;
		p.getInventory().selected = slot;
		eatTicks = 36;
	}

	static void macroTick(Minecraft mc, LocalPlayer p) {
		if (eatTicks > 0) {
			mc.options.keyUse.setDown(true);
			eatTicks--;
			if (eatTicks == 0) {
				mc.options.keyUse.setDown(false);
				p.getInventory().selected = eatOld;
			}
		}
	}

	static void totemToOffhand(Minecraft mc, LocalPlayer p) {
		if (p.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) return;
		for (int i = 9; i <= 44; i++) {
			if (p.inventoryMenu.getSlot(i).getItem().is(Items.TOTEM_OF_UNDYING)) {
				mc.gameMode.handleInventoryMouseClick(
					p.inventoryMenu.containerId, i, 40, ClickType.SWAP, p);
				return;
			}
		}
	}

	static void selectBlock(LocalPlayer p) {
		for (int i = 0; i < 9; i++) {
			if (p.getInventory().getItem(i).getItem() instanceof BlockItem) {
				p.getInventory().selected = i;
				return;
			}
		}
	}

	static void waterClutch(Minecraft mc, LocalPlayer p) {
		int slot = hotbarSlot(p, Items.WATER_BUCKET);
		if (slot == -1) return;
		float oldPitch = p.getXRot();
		int old = p.getInventory().selected;
		p.getInventory().selected = slot;
		p.setXRot(90f);
		mc.gameMode.useItem(p, InteractionHand.MAIN_HAND);
		p.setXRot(oldPitch);
		p.getInventory().selected = old;
	}

	static void runMacro(Minecraft mc, int a) {
		LocalPlayer p = mc.player;
		if (p == null || mc.screen != null || mc.gameMode == null) return;
		switch (a) {
			case 1:
				useSilently(mc, p, Items.ENDER_PEARL);
				break;
			case 2:
				startEat(mc, p);
				break;
			case 3:
				totemToOffhand(mc, p);
				break;
			case 4:
				equipBestSword(p);
				break;
			case 5:
				selectBlock(p);
				break;
			case 6:
				useFirework(mc, p, true);
				break;
			case 7:
				swapChest(mc, p, !p.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA), false);
				break;
			case 8:
				useDown(mc, p, Items.WATER_BUCKET);
				break;
			case 9:
				useDown(mc, p, Items.FIRE_CHARGE);
				break;
			case 10:
				useDown(mc, p, Items.SPLASH_POTION);
				break;
			default:
				break;
		}
	}

	static Vec3 aimAt(LivingEntity e, int mode, float predict) {
		double f = mode == 1 ? 0.9 : (mode == 2 ? 0.15 : 0.6);
		Vec3 mv = e.getDeltaMovement();
		return e.position().add(mv.x * predict, 0, mv.z * predict).add(0, e.getBbHeight() * f, 0);
	}

	static boolean placeAt(Minecraft mc, LocalPlayer p, BlockPos target) {
		Direction[] order = {
			Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST,
			Direction.DOWN, Direction.UP
		};
		for (Direction d : order) {
			BlockPos np = target.relative(d);
			if (mc.level.getBlockState(np).isAir()) continue;
			Direction face = d.getOpposite();
			Vec3 hitVec = Vec3.atCenterOf(np).add(
				face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5);
			mc.gameMode.useItemOn(p, InteractionHand.MAIN_HAND, new BlockHitResult(hitVec, face, np, false));
			return true;
		}
		return false;
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
	static void renderHud(GuiGraphics g, float pt) {
		Minecraft mc = Minecraft.getInstance();
		frames++;
		long nowMs = System.currentTimeMillis();
		if (nowMs - fpsStart >= 1000) {
			fpsValue = frames;
			frames = 0;
			fpsStart = nowMs;
		}
		if (mc.player == null || mc.options.hideGui) return;
		LocalPlayer me = mc.player;
		int sw = mc.getWindow().getGuiScaledWidth();
		int sh = mc.getWindow().getGuiScaledHeight();
		renderEsp(g, mc, me, pt, sw, sh);
		if (hud == null || !hud.enabled) return;

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
		if (hud.cpsHud.value) {
			lines.add("CPS: " + leftClicks.size() + " | " + rightClicks.size());
		}
		if (hud.armor.value) {
			EquipmentSlot[] slots = { EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET };
			StringBuilder sb = new StringBuilder("Zirh:");
			for (EquipmentSlot es : slots) {
				ItemStack st = me.getItemBySlot(es);
				if (st.isEmpty()) {
					sb.append(" -");
				} else if (st.getMaxDamage() <= 0) {
					sb.append(" ok");
				} else {
					sb.append(" ").append((st.getMaxDamage() - st.getDamageValue()) * 100 / st.getMaxDamage()).append("%");
				}
			}
			lines.add(sb.toString());
		}
		if (antiCheat != null && antiCheat.enabled) {
			int shown = 0;
			for (Map.Entry<String, String> en : suspects.entrySet()) {
				if (shown >= 4) break;
				lines.add("! " + en.getKey() + ": " + en.getValue());
				shown++;
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

		if (hud.keys.value) {
			int kx = sw - 56;
			int ky = sh - 62;
			keyBox(g, mc, kx + 18, ky, "W", mc.options.keyUp.isDown());
			keyBox(g, mc, kx, ky + 18, "A", mc.options.keyLeft.isDown());
			keyBox(g, mc, kx + 18, ky + 18, "S", mc.options.keyDown.isDown());
			keyBox(g, mc, kx + 36, ky + 18, "D", mc.options.keyRight.isDown());
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
	static class Cand {
		final LivingEntity e;
		final double score;

		Cand(LivingEntity e, double score) {
			this.e = e;
			this.score = score;
		}
	}

	static class Aura extends Mod {
		FloatSetting targetRange = num("Hedef menzili", 5f, 3f, 8f);
		FloatSetting attackRange = num("Vurus menzili", 3.5f, 2f, 6f);
		ModeSetting attackMode = mode("Mod", 0, "Tek", "Degistir", "Coklu");
		ModeSetting priority = mode("Oncelik", 0, "Mesafe", "Can", "Aci");
		BoolSetting playersT = bool("Oyuncular", true);
		BoolSetting mobsT = bool("Canavarlar", false);
		BoolSetting animalsT = bool("Hayvanlar", false);
		BoolSetting skipBots = bool("Bot atla", false);
		BoolSetting teams = bool("Takim atla", true);
		BoolSetting walls = bool("Duvar arkasi atla", true);
		ModeSetting aimPoint = mode("Hedef noktasi", 0, "Govde", "Bas", "Ayak");
		FloatSetting predict = num("Tahmin", 0f, 0f, 5f);
		ModeSetting rotMode = mode("Rotasyon", 0, "Smooth", "Silent", "Yok");
		FloatSetting turnSpeed = num("Don hizi", 20f, 3f, 90f);
		FloatSetting aimAngle = num("Vurus acisi", 25f, 5f, 90f);
		FloatSetting fov = num("Hedef FOV", 360f, 30f, 360f);
		BoolSetting waitCd = bool("Cooldown bekle", true);
		FloatSetting cps = num("CPS", 12f, 1f, 20f);
		BoolSetting autoSword = bool("Oto kilic", true);
		BoolSetting wtap = bool("WTap (geri tepme)", true);
		BoolSetting critWait = bool("Kritik bekle", false);
		BoolSetting hurtWait = bool("Hurt bekle", false);
		BoolSetting showTarget = bool("Hedef kutusu", true);
		long lastAtk = 0;
		int switchIdx = 0;

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

			List<Cand> cands = new ArrayList<>();
			List<LivingEntity> list = mc.level.getEntitiesOfClass(
				LivingEntity.class, p.getBoundingBox().inflate(targetRange.value),
				x -> x != p && x.isAlive() && !x.isSpectator());
			for (LivingEntity e : list) {
				boolean isPlayer = e instanceof Player;
				boolean isMob = e instanceof Enemy;
				boolean isAnimal = e instanceof Animal;
				if (isPlayer) {
					if (!playersT.value) continue;
				} else if (isMob) {
					if (!mobsT.value) continue;
				} else if (isAnimal) {
					if (!animalsT.value) continue;
				} else {
					continue;
				}
				if (skipBots.value && e instanceof Player pl && isBot(mc, pl)) continue;
				if (teams.value && p.isAlliedTo(e)) continue;
				double d = p.distanceTo(e);
				if (d > targetRange.value) continue;
				if (walls.value && !p.hasLineOfSight(e)) continue;
				float[] r0 = rotationTo(p, aimAt(e, aimPoint.index, 0f));
				float diff = angleDiff(p, r0);
				if (fov.value < 360f && diff > fov.value / 2f) continue;
				double score;
				if (priority.index == 1) score = e.getHealth();
				else if (priority.index == 2) score = diff;
				else score = d;
				cands.add(new Cand(e, score));
			}
			if (cands.isEmpty()) return;
			cands.sort((a, b) -> Double.compare(a.score, b.score));

			LivingEntity primary = cands.get(0).e;
			if (attackMode.is("Degistir") && cands.size() > 1) {
				primary = cands.get(switchIdx % cands.size()).e;
			}
			auraTarget = primary;

			float[] rot = rotationTo(p, aimAt(primary, aimPoint.index, predict.value));
			boolean smooth = rotMode.is("Smooth");
			boolean silent = rotMode.is("Silent");
			if (smooth) {
				rotateToward(p, rot, turnSpeed.value);
			}

			List<LivingEntity> toHit = new ArrayList<>();
			if (attackMode.is("Coklu")) {
				for (Cand c : cands) {
					if (toHit.size() >= 4) break;
					if (p.distanceTo(c.e) <= attackRange.value) toHit.add(c.e);
				}
			} else if (p.distanceTo(primary) <= attackRange.value) {
				toHit.add(primary);
			}
			if (toHit.isEmpty()) return;

			long now = System.currentTimeMillis();
			boolean ready;
			if (waitCd.value) {
				ready = p.getAttackStrengthScale(0.5f) >= 1.0f;
			} else {
				ready = now - lastAtk >= (long) (1000.0 / cps.value);
			}
			if (!ready) return;
			if (smooth && angleDiff(p, rot) > aimAngle.value) return;
			if (critWait.value && (p.onGround() || p.fallDistance <= 0f)) return;
			if (hurtWait.value && primary.hurtTime > 0 && !attackMode.is("Coklu")) return;

			if (autoSword.value) equipBestSword(p);
			if (criticals != null && criticals.enabled) {
				criticals.doCrit(p);
			}
			if (silent) {
				p.connection.send(new ServerboundMovePlayerPacket.Rot(rot[0], rot[1], p.onGround()));
			}
			for (LivingEntity t : toHit) {
				mc.gameMode.attack(p, t);
				hitEffect(mc, t);
			}
			p.swing(InteractionHand.MAIN_HAND);
			if (silent) {
				p.connection.send(new ServerboundMovePlayerPacket.Rot(p.getYRot(), p.getXRot(), p.onGround()));
			}
			lastAtk = now;
			switchIdx++;
			if (wtap.value && p.isSprinting()) {
				p.setSprinting(false);
				wtapTicks = 2;
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
		FloatSetting minHealth = num("Min can", 20f, 2f, 20f);

		AutoTotem() {
			super("AutoTotem", "Combat", GLFW.GLFW_KEY_J);
		}

		@Override
		void onTick(Minecraft mc, LocalPlayer p) {
			if (tick % 4 != 0) return;
			if (p.getHealth() > minHealth.value) return;
			totemToOffhand(mc, p);
		}
	}

	static class AutoClicker extends Mod {
		FloatSetting cps = num("CPS", 10f, 1f, 20f);
		BoolSetting teams = bool("Takim atla", true);
		long last = 0;

		AutoClicker() {
			super("AutoClicker", "Combat", -1);
		}

		@Override
		void onTick(Minecraft mc, LocalPlayer p) {
			if (mc.screen != null || !mc.options.keyAttack.isDown()) return;
			if (mc.hitResult instanceof EntityHitResult hit) {
				Entity e = hit.getEntity();
				if (!(e instanceof LivingEntity) || !e.isAlive()) return;
				if (teams.value && p.isAlliedTo(e)) return;
				long now = System.currentTimeMillis();
				if (now - last >= (long) (1000.0 / cps.value)) {
					last = now;
					mc.gameMode.attack(p, e);
					p.swing(InteractionHand.MAIN_HAND);
					hitEffect(mc, e);
				}
			}
		}
	}

	static class AutoPot extends Mod {
		FloatSetting health = num("Can esigi", 10f, 2f, 18f);
		FloatSetting delay = num("Gecikme (tick)", 20f, 5f, 60f);
		int cd = 0;

		AutoPot() {
			super("AutoPot", "Combat", -1);
		}

		@Override
		void onTick(Minecraft mc, LocalPlayer p) {
			if (cd > 0) cd--;
			if (mc.screen != null || cd > 0) return;
			if (p.getHealth() > health.value) return;
			if (useDown(mc, p, Items.SPLASH_POTION)) {
				cd = (int) delay.value;
			}
		}
	}

	static class AutoGApple extends Mod {
		FloatSetting health = num("Can esigi", 8f, 2f, 18f);
		int cd = 0;

		AutoGApple() {
			super("AutoGApple", "Combat", -1);
		}

		@Override
		void onTick(Minecraft mc, LocalPlayer p) {
			if (cd > 0) cd--;
			if (mc.screen != null) return;
			if (p.getHealth() <= health.value && eatTicks == 0 && cd == 0) {
				startEat(mc, p);
				cd = 120;
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
			if (wtapTicks > 0) {
				wtapTicks--;
				return;
			}
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

	static class SafeWalk extends Mod {
		boolean forced = false;

		SafeWalk() {
			super("SafeWalk", "Move", -1);
		}

		@Override
		void onPreTick(Minecraft mc, LocalPlayer p) {
			if (mc.screen != null || !p.onGround() || p.getAbilities().flying) {
				if (forced) {
					mc.options.keyShift.setDown(false);
					forced = false;
				}
				return;
			}
			Vec3 v = p.getDeltaMovement();
			double hz = Math.sqrt(v.x * v.x + v.z * v.z);
			Vec3 ahead = p.position().add(v.x * 3.0, 0, v.z * 3.0);
			BlockPos bp = BlockPos.containing(ahead.x, p.getY() - 0.5, ahead.z);
			boolean edge = hz > 0.01 && mc.level.getBlockState(bp).isAir();
			if (edge) {
				mc.options.keyShift.setDown(true);
				forced = true;
			} else if (forced) {
				mc.options.keyShift.setDown(false);
				forced = false;
			}
		}

		@Override
		void onDisable(Minecraft mc) {
			if (forced) mc.options.keyShift.setDown(false);
			forced = false;
		}
	}

	static class Parkour extends Mod {
		Parkour() {
			super("Parkour", "Move", -1);
		}

		@Override
		void onPreTick(Minecraft mc, LocalPlayer p) {
			if (mc.screen != null || !p.onGround() || p.isShiftKeyDown() || p.getAbilities().flying) return;
			Vec3 v = p.getDeltaMovement();
			double hz = Math.sqrt(v.x * v.x + v.z * v.z);
			if (hz < 0.05) return;
			Vec3 ahead = p.position().add(v.x * 2.0, 0, v.z * 2.0);
			BlockPos bp = BlockPos.containing(ahead.x, p.getY() - 0.5, ahead.z);
			if (mc.level.getBlockState(bp).isAir()) {
				p.setDeltaMovement(v.x, 0.42, v.z);
			}
		}
	}

	static class Glide extends Mod {
		FloatSetting fall = num("Dusme hizi", 0.1f, 0.02f, 0.3f);

		Glide() {
			super("Glide", "Move", -1);
		}

		@Override
		void onTick(Minecraft mc, LocalPlayer p) {
			if (p.onGround() || p.isFallFlying() || p.isInWater() || p.getAbilities().flying) return;
			Vec3 v = p.getDeltaMovement();
			if (v.y < -fall.value) {
				p.setDeltaMovement(v.x, -fall.value, v.z);
			}
		}
	}

	static class FastLadder extends Mod {
		FastLadder() {
			super("FastLadder", "Move", -1);
		}

		@Override
		void onTick(Minecraft mc, LocalPlayer p) {
			if (mc.screen != null) return;
			if (p.onClimbable() && p.horizontalCollision) {
				Vec3 v = p.getDeltaMovement();
				p.setDeltaMovement(v.x, 0.3, v.z);
			}
		}
	}

	static class InvMove extends Mod {
		InvMove() {
			super("InvMove", "Move", -1);
		}

		@Override
		void onTick(Minecraft mc, LocalPlayer p) {
			if (!(mc.screen instanceof AbstractContainerScreen)) return;
			long win = mc.getWindow().getWindow();
			mc.options.keyUp.setDown(isDown(win, GLFW.GLFW_KEY_W));
			mc.options.keyDown.setDown(isDown(win, GLFW.GLFW_KEY_S));
			mc.options.keyLeft.setDown(isDown(win, GLFW.GLFW_KEY_A));
			mc.options.keyRight.setDown(isDown(win, GLFW.GLFW_KEY_D));
			mc.options.keyJump.setDown(isDown(win, GLFW.GLFW_KEY_SPACE));
			mc.options.keySprint.setDown(isDown(win, GLFW.GLFW_KEY_LEFT_CONTROL));
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
		BoolSetting boxes = bool("Kutu", true);
		BoolSetting healthBar = bool("Can cubugu", true);
		BoolSetting tracers = bool("Cizgi (tracer)", false);
		BoolSetting names = bool("Isim", true);
		BoolSetting health = bool("Can yazisi", true);
		FloatSetting range = num("Menzil", 120f, 20f, 300f);
		FloatSetting fovMul = num("FOV ayari", 1.0f, 0.7f, 1.3f);

					Esp() {
			super("ESP", "Render", -1);
		}
	}

	static class NameTags extends Mod {
		BoolSetting showHealth = bool("Can yazisi", true);
		BoolSetting showBar = bool("Can bari", true);
		BoolSetting showDist = bool("Mesafe", true);
		BoolSetting showItem = bool("Elindeki esya", true);
		FloatSetting range = num("Menzil", 100f, 10f, 300f);

		NameTags() {
			super("NameTags", "Render", -1);
		}
	}

	static class ItemEsp extends Mod {
		BoolSetting onlyValuable = bool("Sadece degerli", false);
		FloatSetting range = num("Menzil", 50f, 10f, 150f);

		ItemEsp() {
			super("ItemESP", "Render", -1);
		}

		@Override
		void onTick(Minecraft mc, LocalPlayer p) {
			if (tick % 5 != 0) return;
			allItems = mc.level.getEntitiesOfClass(
				ItemEntity.class, p.getBoundingBox().inflate(range.value), x -> true);
		}

		@Override
		void onDisable(Minecraft mc) {
			allItems = new ArrayList<>();
		}
	}

	static class Particles extends Mod {
		ModeSetting type = mode("Tur", 1, PARTICLE_NAMES);
		FloatSetting amount = num("Miktar", 3f, 1f, 15f);
		FloatSetting radius = num("Yaricap", 1.2f, 0.3f, 3f);
		BoolSetting onlyMoving = bool("Sadece hareketliyken", false);

		Particles() {
			super("Particles", "Render", -1);
		}

		@Override
		void onTick(Minecraft mc, LocalPlayer p) {
			if (onlyMoving.value) {
				Vec3 v = p.getDeltaMovement();
				if (v.x * v.x + v.z * v.z < 0.0004) return;
			}
			ParticleOptions opt = particleFor(type.index);
			int n = (int) amount.value;
			for (int i = 0; i < n; i++) {
				double rx = (rnd() - 0.5) * 2.0 * radius.value;
				double ry = rnd() * 2.0;
				double rz = (rnd() - 0.5) * 2.0 * radius.value;
				mc.level.addParticle(opt, p.getX() + rx, p.getY() + ry, p.getZ() + rz, 0, 0.02, 0);
			}
		}
	}

	static class HitParticles extends Mod {
		ModeSetting type = mode("Tur", 0, PARTICLE_NAMES);
		FloatSetting amount = num("Miktar", 8f, 1f, 30f);

		HitParticles() {
			super("HitParticles", "Render", -1);
		}
	}

	static class TimeChanger extends Mod {
		FloatSetting time = num("Zaman (0-24000)", 6000f, 0f, 24000f);

		TimeChanger() {
			super("TimeChanger", "Render", -1);
		}

		@Override
		void onTick(Minecraft mc, LocalPlayer p) {
			mc.level.setDayTime((long) time.value);
		}
	}

	static class NoWeather extends Mod {
		NoWeather() {
			super("NoWeather", "Render", -1);
		}

		@Override
		void onTick(Minecraft mc, LocalPlayer p) {
			mc.level.setRainLevel(0f);
			mc.level.setThunderLevel(0f);
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
		FloatSetting extend = num("Ileri uzatma", 0f, 0f, 3f);
		BoolSetting autoSwitch = bool("Blok sec", true);
		BoolSetting tower = bool("Kule (ziplayinca)", true);
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
			if (autoSwitch.value && !(p.getMainHandItem().getItem() instanceof BlockItem)) {
				selectBlock(p);
			}
			if (!(p.getMainHandItem().getItem() instanceof BlockItem)) return;

			Vec3 v = p.getDeltaMovement();
			double hz = Math.sqrt(v.x * v.x + v.z * v.z);
			List<BlockPos> targets = new ArrayList<>();
			targets.add(p.blockPosition().below());
			if (extend.value > 0.01f && hz > 0.02) {
				double dx = v.x / hz;
				double dz = v.z / hz;
				targets.add(BlockPos.containing(
					p.getX() + dx * extend.value, p.getY() - 1, p.getZ() + dz * extend.value));
			}
			for (BlockPos t : targets) {
				if (!mc.level.getBlockState(t).isAir()) continue;
				if (placeAt(mc, p, t)) {
					if (swing.value) p.swing(InteractionHand.MAIN_HAND);
					boolean towering = tower.value && mc.options.keyJump.isDown();
					cd = towering ? 0 : (int) delay.value;
					break;
				}
			}
		}
	}

	static class ResourceEsp extends Mod {
		BoolSetting diamond = bool("Elmas", true);
		BoolSetting emerald = bool("Zumrut", true);
		BoolSetting gold = bool("Altin", true);
		BoolSetting iron = bool("Demir", false);
		FloatSetting range = num("Menzil", 60f, 10f, 150f);

		ResourceEsp() {
			super("ResourceESP", "Bedwars", -1);
		}

		@Override
		void onTick(Minecraft mc, LocalPlayer p) {
			if (tick % 5 != 0) return;
			resItems = mc.level.getEntitiesOfClass(
				ItemEntity.class, p.getBoundingBox().inflate(range.value), x -> true);
		}

							@Override
		void onDisable(Minecraft mc) {
			resItems = new ArrayList<>();
		}
	}

	static class BedEsp extends Mod {
		FloatSetting radius = num("Yaricap", 32f, 8f, 64f);
		int sliceX = Integer.MIN_VALUE;
		BlockPos center = BlockPos.ZERO;
		List<BlockPos> building = new ArrayList<>();

		BedEsp() {
			super("BedESP", "Bedwars", -1);
		}

		@Override
		void onTick(Minecraft mc, LocalPlayer p) {
			int r = (int) radius.value;
			int ry = Math.min(r, 24);
			if (sliceX == Integer.MIN_VALUE) {
				center = p.blockPosition();
				sliceX = -r;
				building = new ArrayList<>();
			}
			for (int n = 0; n < 2 && sliceX <= r; n++) {
				for (int y = -ry; y <= ry; y++) {
					for (int z = -r; z <= r; z++) {
						BlockPos pos = center.offset(sliceX, y, z);
						if (mc.level.getBlockState(pos).is(BlockTags.BEDS)) {
							building.add(pos);
						}
					}
				}
				sliceX++;
			}
			if (sliceX > r) {
				List<BlockPos> kept = new ArrayList<>();
				for (BlockPos b : building) {
					boolean dup = false;
					for (BlockPos k : kept) {
						if (b.distSqr(k) <= 2) {
							dup = true;
							break;
						}
					}
					if (!dup) kept.add(b);
				}
				beds = kept;
				sliceX = Integer.MIN_VALUE;
			}
			if (tick % 10 == 0) {
				double best = Double.MAX_VALUE;
				BlockPos found = null;
				for (BlockPos b : beds) {
					double d = b.distToCenterSqr(p.getX(), p.getY(), p.getZ());
					if (d < best) {
						best = d;
						found = b;
					}
				}
				bedPos = found;
				bedDist = found == null ? -1 : Math.sqrt(best);
			}
		}

		@Override
		void onDisable(Minecraft mc) {
			beds = new ArrayList<>();
			bedPos = null;
			bedDist = -1;
			sliceX = Integer.MIN_VALUE;
		}
	}

	static class BlockIn extends Mod {
		int cd = 0;

		BlockIn() {
			super("BlockIn", "Bedwars", -1);
		}

		@Override
		void onTick(Minecraft mc, LocalPlayer p) {
			if (mc.screen != null) return;
			if (cd > 0) {
				cd--;
				return;
			}
			if (!(p.getMainHandItem().getItem() instanceof BlockItem)) selectBlock(p);
			if (!(p.getMainHandItem().getItem() instanceof BlockItem)) return;
			BlockPos f = p.blockPosition();
			Direction[] hs = { Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST };
			List<BlockPos> order = new ArrayList<>();
			for (Direction d : hs) order.add(f.relative(d));
			for (Direction d : hs) order.add(f.above().relative(d));
			order.add(f.above(2));
			boolean done = true;
			for (BlockPos t : order) {
				if (!mc.level.getBlockState(t).isAir()) continue;
				done = false;
				if (placeAt(mc, p, t)) {
					p.swing(InteractionHand.MAIN_HAND);
					cd = 1;
					return;
				}
			}
			if (done) toggle(mc);
		}
	}

	static class InvisEsp extends Mod {
		InvisEsp() {
			super("InvisESP", "Bedwars", -1);
		}
	}

	// =====================================================
	//  MACRO
	// =====================================================
	static class MacroMod extends Mod {
		ModeSetting action;

		MacroMod(String name, int defaultAction) {
			super(name, "Player", -1);
			action = mode("Eylem", defaultAction, ACTIONS);
		}

		@Override
		void onPress(Minecraft mc) {
			runMacro(mc, action.index);
		}

		@Override
		void toggle(Minecraft mc) {
			runMacro(mc, action.index);
		}
	}

	// =====================================================
	//  ANTICHEAT (baskalarinin hilesini tespit eder)
	// =====================================================
	static class Track {
		boolean init = false;
		double x;
		double y;
		double z;
		float yaw;
		int speedVl = 0;
		int hoverTicks = 0;
		int riseTicks = 0;
		int snapVl = 0;
		long snapStart = 0;
		long lastHurtTick = -1000;
		final Map<String, Long> lastAlert = new HashMap<>();
	}

	static class AntiCheat extends Mod {
		BoolSetting speedChk = bool("Speed kontrol", true);
		BoolSetting flyChk = bool("Fly kontrol", true);
		BoolSetting reachChk = bool("Reach kontrol", true);
		BoolSetting aimChk = bool("Aura/Aim kontrol", true);
		BoolSetting chat = bool("Sohbete yaz", true);
		FloatSetting speedLimit = num("Speed siniri", 0.75f, 0.4f, 1.5f);
		FloatSetting flyTicks = num("Fly suresi (tick)", 15f, 8f, 40f);
		FloatSetting reachLimit = num("Reach siniri", 3.9f, 3.0f, 6f);
		FloatSetting snapAngle = num("Donus siniri", 70f, 30f, 150f);
		int lastMeHurt = 0;

		AntiCheat() {
			super("AntiCheat", "Client", -1);
		}

		private void flag(LocalPlayer me, Player o, Track t, String type, String info) {
			Long last = t.lastAlert.get(type);
			if (last != null && tick - last < 100) return;
			t.lastAlert.put(type, (long) tick);
			String name = o.getName().getString();
			suspects.put(name, type);
			suspectTick.put(name, (long) tick);
			if (chat.value) {
				me.displayClientMessage(
					Component.literal("[AntiCheat] " + name + " -> " + type + " " + info), false);
			}
		}

		@Override
		void onTick(Minecraft mc, LocalPlayer p) {
			boolean meHurtNow = p.hurtTime > lastMeHurt;
			lastMeHurt = p.hurtTime;
			for (Player o : mc.level.players()) {
				if (o == p || !o.isAlive()) continue;
				Track t = tracks.computeIfAbsent(o.getUUID(), k -> new Track());
				double nx = o.getX();
				double ny = o.getY();
				double nz = o.getZ();
				float nyaw = o.getYRot();
				if (!t.init) {
					t.init = true;
					t.x = nx;
					t.y = ny;
					t.z = nz;
					t.yaw = nyaw;
					continue;
				}
				double dx = nx - t.x;
				double dy = ny - t.y;
				double dz = nz - t.z;
				double hz = Math.sqrt(dx * dx + dz * dz);
				float dyaw = Mth.wrapDegrees(nyaw - t.yaw);
				t.x = nx;
				t.y = ny;
				t.z = nz;
				t.yaw = nyaw;
				if (hz > 8 || Math.abs(dy) > 8) {
					t.speedVl = 0;
					t.hoverTicks = 0;
					t.riseTicks = 0;
					continue;
				}
				if (o.hurtTime > 0) t.lastHurtTick = tick;
				boolean exempt = o.isFallFlying() || o.isPassenger() || o.isSwimming()
					|| o.isInWater() || o.isInLava() || o.onClimbable()
					|| o.isSpectator() || o.isCreative() || tick - t.lastHurtTick < 30;

				if (speedChk.value) {
					if (!exempt && hz > speedLimit.value) t.speedVl++;
					else if (t.speedVl > 0) t.speedVl--;
					if (t.speedVl >= 5) {
						flag(p, o, t, "Speed", String.format("%.2f b/tick", hz));
						t.speedVl = 0;
					}
				}

				if (flyChk.value) {
					if (!o.onGround() && !exempt) {
						boolean openAir = mc.level.getBlockState(
							BlockPos.containing(nx, ny - 0.4, nz)).isAir();
						if (openAir && Math.abs(dy) < 0.02) t.hoverTicks++;
						else t.hoverTicks = 0;
						if (dy > 0.08) t.riseTicks++;
						else if (dy <= 0) t.riseTicks = 0;
						if (t.hoverTicks >= flyTicks.value) {
							flag(p, o, t, "Fly", "(havada duruyor)");
							t.hoverTicks = 0;
						}
						if (t.riseTicks >= 14) {
							flag(p, o, t, "Fly", "(yukari ucuyor)");
							t.riseTicks = 0;
						}
					} else {
						t.hoverTicks = 0;
						t.riseTicks = 0;
					}
				}

				if (aimChk.value && o.swinging && p.distanceTo(o) < 8
						&& Math.abs(dyaw) > snapAngle.value) {
					if (tick - t.snapStart > 60) {
						t.snapStart = tick;
						t.snapVl = 0;
					}
					t.snapVl++;
					if (t.snapVl >= 3) {
						flag(p, o, t, "Aura/Aim", "(ani donus)");
						t.snapVl = 0;
					}
				}

									if (reachChk.value && meHurtNow && o.swinging) {
					AABB box = p.getBoundingBox();
					Vec3 eye = o.getEyePosition();
					double cx = Mth.clamp(eye.x, box.minX, box.maxX);
					double cy = Mth.clamp(eye.y, box.minY, box.maxY);
					double cz = Mth.clamp(eye.z, box.minZ, box.maxZ);
					double ex = eye.x - cx;
					double ey = eye.y - cy;
					double ez = eye.z - cz;
					double d = Math.sqrt(ex * ex + ey * ey + ez * ez);
					if (d > reachLimit.value && d < 8) {
						flag(p, o, t, "Reach", String.format("%.1f blok", d));
					}
				}
			}

			if (tick % 200 == 0) {
				java.util.Set<UUID> present = new java.util.HashSet<>();
				for (Player o : mc.level.players()) present.add(o.getUUID());
				tracks.keySet().retainAll(present);
			}
			if (tick % 100 == 0) {
				suspectTick.entrySet().removeIf(en -> tick - en.getValue() > 1200);
				suspects.keySet().retainAll(suspectTick.keySet());
			}
		}

		@Override
		void onDisable(Minecraft mc) {
			tracks.clear();
			suspects.clear();
			suspectTick.clear();
		}
	}

	// =====================================================
	//  FPS BOOST
	// =====================================================
	static class FpsBoost extends Mod {
		FloatSetting dist = num("Gorus mesafesi", 8f, 2f, 32f);
		BoolSetting fast = bool("Hizli grafik", true);
		BoolSetting noClouds = bool("Bulut kapat", true);
		BoolSetting minParticles = bool("Partikul azalt", true);
		BoolSetting noShadows = bool("Golge kapat", true);
		BoolSetting noBlend = bool("Biyom gecisi kapat", true);
		FloatSetting entDist = num("Varlik mesafesi", 0.5f, 0.5f, 5f);
		boolean saved = false;
		int oldDist;
		GraphicsStatus oldG;
		CloudStatus oldC;
		ParticleStatus oldP;
		boolean oldS;
		int oldB;
		double oldE;

		FpsBoost() {
			super("FPSBoost", "Render", -1);
		}

		private void apply(Minecraft mc) {
			Options o = mc.options;
			o.renderDistance().set((int) dist.value);
			if (fast.value) o.graphicsMode().set(GraphicsStatus.FAST);
			if (noClouds.value) o.cloudStatus().set(CloudStatus.OFF);
			if (minParticles.value) o.particles().set(ParticleStatus.MINIMAL);
			if (noShadows.value) o.entityShadows().set(false);
			if (noBlend.value) o.biomeBlendRadius().set(0);
			o.entityDistanceScaling().set((double) entDist.value);
		}

		@Override
		void onEnable(Minecraft mc) {
			Options o = mc.options;
			oldDist = o.renderDistance().get();
			oldG = o.graphicsMode().get();
			oldC = o.cloudStatus().get();
			oldP = o.particles().get();
			oldS = o.entityShadows().get();
			oldB = o.biomeBlendRadius().get();
			oldE = o.entityDistanceScaling().get();
			saved = true;
			apply(mc);
		}

		@Override
		void onTick(Minecraft mc, LocalPlayer p) {
			if (tick % 40 == 0) apply(mc);
		}

		@Override
		void onDisable(Minecraft mc) {
			if (!saved) return;
			Options o = mc.options;
			o.renderDistance().set(oldDist);
			o.graphicsMode().set(oldG);
			o.cloudStatus().set(oldC);
			o.particles().set(oldP);
			o.entityShadows().set(oldS);
			o.biomeBlendRadius().set(oldB);
			o.entityDistanceScaling().set(oldE);
			saved = false;
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
		BoolSetting cpsHud = bool("CPS sayaci", true);
		BoolSetting keys = bool("Tuslar (WASD)", false);
		BoolSetting armor = bool("Zirh durumu", false);

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
