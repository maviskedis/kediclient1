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
import net.minecraft.tags.ItemTags;
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

	static boolean silentActive = false;
	static float silentYaw = 0f;
	static float silentPitch = 0f;
	static int silentHoldTicks = 0;

	@Override
	public void onInitializeClient() {
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
		MODS.add(new ElytraBooster());
		MODS.add(new ElytraTarget());
		MODS.add(new ElytraHelper());
		MODS.add(new ElytraFirework());
		MODS.add(new AutoSprint());
		MODS.add(new NoFall());
		MODS.add(new Flight());
		MODS.add(new Speed());
		MODS.add(new SafeWalk());
		MODS.add(new Parkour());
		MODS.add(new Glide());
		MODS.add(new FastLadder());
		MODS.add(new InvMove());
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
		MODS.add(new AutoRespawn());
		MODS.add(new ChestStealer());
		MODS.add(new AntiBlind());
		MODS.add(new AntiAfk());
		MODS.add(new AutoWalk());
		MODS.add(new AutoTool());
		resEsp = new ResourceEsp();
		bedEsp = new BedEsp();
		invisEsp = new InvisEsp();
		MODS.add(new Scaffold());
		MODS.add(resEsp);
		MODS.add(bedEsp);
		MODS.add(invisEsp);
		MODS.add(new BlockIn());
		MODS.add(new Nuker());
		MODS.add(new MacroMod("Macro1", 1));
		MODS.add(new MacroMod("Macro2", 2));
		MODS.add(new MacroMod("Macro3", 3));
		MODS.add(new MacroMod("Macro4", 8));
		MODS.add(new ClickGuiMod());
		hud = new HudMod();
		MODS.add(hud);
		antiCheat = new AntiCheat();
		MODS.add(antiCheat);
		MODS.add(new CfgMod());

		loadConfig();

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			LocalPlayer p = client.player;
			if (p == null || client.level == null || client.gameMode == null) {
				lastPlayer = null;
				silentActive = false;
				return;
			}
			if (p != lastPlayer) {
				lastPlayer = p;
				splashStart = System.currentTimeMillis();
				silentActive = false;
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

			if (silentActive) {
				if (silentHoldTicks > 0) {
					silentHoldTicks--;
					p.connection.send(new ServerboundMovePlayerPacket.Rot(
						gcdYaw(p, silentYaw), gcdPitch(p, silentPitch), p.onGround()));
				} else {
					float dy = Mth.wrapDegrees(p.getYRot() - silentYaw);
					float dp = p.getXRot() - silentPitch;
					silentYaw += Mth.clamp(dy, -22f, 22f);
					silentPitch = Mth.clamp(silentPitch + Mth.clamp(dp, -18f, 18f), -90f, 90f);
					p.connection.send(new ServerboundMovePlayerPacket.Rot(
						gcdYaw(p, silentYaw), gcdPitch(p, silentPitch), p.onGround()));
					if (Math.abs(Mth.wrapDegrees(p.getYRot() - silentYaw)) < 1.5f
							&& Math.abs(p.getXRot() - silentPitch) < 1.5f) {
						silentActive = false;
					}
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

static double rnd() {
		return java.util.concurrent.ThreadLocalRandom.current().nextDouble();
	}

	static double rndRange(double min, double max) {
		return min + (max - min) * rnd();
	}

	static float gcdStep(Minecraft mc) {
		double sens = mc.options.sensitivity().get();
		double f = sens * 0.6 + 0.2;
		double gcd = f * f * f * 8.0 * 0.15;
		return (float) Math.max(0.01, gcd);
	}

	static float gcdYaw(LocalPlayer p, float targetYaw) {
		float step = gcdStep(Minecraft.getInstance());
		float delta = Mth.wrapDegrees(targetYaw - p.getYRot());
		float snapped = Math.round(delta / step) * step;
		return p.getYRot() + snapped;
	}

	static float gcdPitch(LocalPlayer p, float targetPitch) {
		float step = gcdStep(Minecraft.getInstance());
		float clamped = Mth.clamp(targetPitch, -90f, 90f);
		float delta = clamped - p.getXRot();
		float snapped = Math.round(delta / step) * step;
		return Mth.clamp(p.getXRot() + snapped, -90f, 90f);
	}

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
		double dx = t.x - eye.x, dy = t.y - eye.y, dz = t.z - eye.z;
		double horiz = Math.sqrt(dx * dx + dz * dz);
		return new float[] { (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0), (float) (-Math.toDegrees(Math.atan2(dy, horiz))) };
	}

	static Vec3 aimPoint(LivingEntity e) { return e.position().add(0, e.getBbHeight() * 0.6, 0); }

	static float angleDiff(Player p, float[] rot) {
		float dy = Mth.wrapDegrees(rot[0] - p.getYRot()), dp = rot[1] - p.getXRot();
		return (float) Math.sqrt(dy * dy + dp * dp);
	}

	static void rotateToward(LocalPlayer p, float[] rot, float maxStep) {
		float dy = Mth.wrapDegrees(rot[0] - p.getYRot());
		float dp = Mth.clamp(rot[1] - p.getXRot(), -90f, 90f);
		double dist = Math.sqrt(dy * dy + dp * dp);
		float ease = (float) Mth.clamp(dist / 36.0, 0.25, 1.0);
		float step = Math.max(1.2f, maxStep * ease);
		p.setYRot(gcdYaw(p, p.getYRot() + Mth.clamp(dy, -step, step) + (float) ((rnd() - 0.5) * 1.15)));
		p.setXRot(gcdPitch(p, Mth.clamp(p.getXRot() + Mth.clamp(dp, -step, step) + (float) ((rnd() - 0.5) * 0.75), -90f, 90f)));
	}

	static void applySilentRot(LocalPlayer p, float[] rot, int holdTicks) {
		float sy = gcdYaw(p, rot[0] + (float) ((rnd() - 0.5) * 1.2));
		float sp = gcdPitch(p, rot[1] + (float) ((rnd() - 0.5) * 0.8));
		silentActive = true; silentYaw = sy; silentPitch = sp; silentHoldTicks = Math.max(1, holdTicks);
		p.connection.send(new ServerboundMovePlayerPacket.Rot(sy, sp, p.onGround()));
	}

	static boolean isBot(Minecraft mc, Player o) {
		return mc.getConnection() != null && mc.getConnection().getPlayerInfo(o.getUUID()) == null;
	}

	static boolean swapChest(Minecraft mc, LocalPlayer p, boolean toElytra, boolean hotbarMode) {
		if (hotbarMode) {
			for (int i = 0; i < 9; i++) {
				ItemStack s = p.getInventory().getItem(i);
				boolean match = toElytra ? s.is(Items.ELYTRA) : (s.getItem() instanceof ArmorItem a && a.getEquipmentSlot() == EquipmentSlot.CHEST);
				if (match) {
					mc.gameMode.handleInventoryMouseClick(p.inventoryMenu.containerId, 6, i, ClickType.SWAP, p);
					return true;
				}
			}
			return false;
		}
		int src = -1;
		for (int i = 9; i <= 44; i++) {
			ItemStack s = p.inventoryMenu.getSlot(i).getItem();
			boolean match = toElytra ? s.is(Items.ELYTRA) : (s.getItem() instanceof ArmorItem a && a.getEquipmentSlot() == EquipmentSlot.CHEST);
			if (match) { src = i; break; }
		}
		if (src == -1) return false;
		slotClick(mc, p, src); slotClick(mc, p, 6); slotClick(mc, p, src);
		return true;
	}

	static boolean useFirework(Minecraft mc, LocalPlayer p, boolean silent) {
		if (p.getOffhandItem().is(Items.FIREWORK_ROCKET)) {
			mc.gameMode.useItem(p, InteractionHand.OFF_HAND);
			return true;
		}
		int slot = -1;
		for (int i = 0; i < 9; i++) if (p.getInventory().getItem(i).is(Items.FIREWORK_ROCKET)) { slot = i; break; }
		if (slot == -1) return false;
		int old = p.getInventory().selected;
		p.getInventory().selected = slot;
		mc.gameMode.useItem(p, InteractionHand.MAIN_HAND);
		if (silent) p.getInventory().selected = old;
		return true;
	}

	static boolean airBelow(Minecraft mc, LocalPlayer p, int n) {
		BlockPos base = p.blockPosition();
		for (int k = 1; k <= n; k++) if (!mc.level.getBlockState(base.below(k)).isAir()) return false;
		return true;
	}

	static final Item[] SWORDS = {
		Items.NETHERITE_SWORD, Items.DIAMOND_SWORD, Items.IRON_SWORD,
		Items.STONE_SWORD, Items.GOLDEN_SWORD, Items.WOODEN_SWORD
	};

	static boolean isWeapon(ItemStack st) { return st.is(ItemTags.SWORDS) || st.is(ItemTags.AXES); }

	static void equipBestSword(LocalPlayer p) {
		for (Item sw : SWORDS) for (int i = 0; i < 9; i++) if (p.getInventory().getItem(i).is(sw)) { p.getInventory().selected = i; return; }
	}

	static void slotClick(Minecraft mc, LocalPlayer p, int slot) {
		mc.gameMode.handleInventoryMouseClick(p.inventoryMenu.containerId, slot, 0, ClickType.PICKUP, p);
	}

	static int accent(int i) {
		double t = (Math.sin(System.currentTimeMillis() / 500.0 + i * 0.5) + 1.0) / 2.0;
		int r = (int) (0xB3 + (0x4F - 0xB3) * t), gr = (int) (0x88 + (0xC3 - 0x88) * t), b = (int) (0xFF + (0xF7 - 0xFF) * t);
		return 0xFF000000 | (r << 16) | (gr << 8) | b;
	}

														 	static boolean placeAt(Minecraft mc, LocalPlayer p, BlockPos target) {
		Direction[] order = {
			Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.UP
		};
		for (Direction d : order) {
			BlockPos np = target.relative(d);
			BlockState ns = mc.level.getBlockState(np);
			if (ns.isAir() || !ns.getFluidState().isEmpty()) continue;
			Direction face = d.getOpposite();
			Vec3 hitVec = Vec3.atCenterOf(np).add(
				face.getStepX() * 0.45 + (rnd() - 0.5) * 0.1,
				face.getStepY() * 0.45 + (rnd() - 0.5) * 0.1,
				face.getStepZ() * 0.45 + (rnd() - 0.5) * 0.1);
			float[] r = rotationTo(p, hitVec);
			p.connection.send(new ServerboundMovePlayerPacket.Rot(gcdYaw(p, r[0]), gcdPitch(p, r[1]), p.onGround()));
			mc.gameMode.useItemOn(p, InteractionHand.MAIN_HAND, new BlockHitResult(hitVec, face, np, false));
			return true;
		}
		return false;
	}

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
		double minX = 1e9, minY = 1e9, maxX = -1e9, maxY = -1e9;
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
		int x1 = (int) minX, y1 = (int) minY, x2 = (int) maxX, y2 = (int) maxY;
		int midX = (x1 + x2) / 2;
		if (esp.boxes.value) drawBox(g, x1, y1, x2, y2, color);
		if (hpFrac >= 0f) {
			int bh = (int) ((y2 - y1) * hpFrac);
			g.fill(x1 - 4, y1, x1 - 2, y2 + 1, 0xFF2A2240);
			g.fill(x1 - 4, y2 + 1 - bh, x1 - 2, y2 + 1, hpFrac > 0.5f ? 0xFF7CFFB0 : 0xFFFF7C7C);
		}
		if (esp.tracers.value) drawLine(g, sw / 2, sh, midX, y2, color);
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
		boolean doAll = esp.enabled, doInv = invisEsp.enabled, doRes = resEsp.enabled;
		boolean doBed = bedEsp.enabled, doTags = nameTags.enabled, doItems = itemEsp.enabled;
		boolean doTarget = aura != null && aura.enabled && aura.showTarget.value && auraTarget != null && auraTarget.isAlive();
		if (!doAll && !doInv && !doRes && !doBed && !doTags && !doItems && !doTarget) return;

		Vec3 cam = me.getEyePosition(pt);
		float yaw = me.getViewYRot(pt), pitch = me.getViewXRot(pt);
		double fovMod = Math.max(0.7, Math.min(1.6, ((me.getSpeed() / 0.1 + 1.0) / 2.0) * (me.getAbilities().flying ? 1.1 : 1.0)));
		double fov = mc.options.fov().get() * fovMod * esp.fovMul.value;

		if (doTarget) {
			LivingEntity tg = auraTarget;
			espEntity(g, mc, cam, yaw, pitch, fov, sw, sh,
				Mth.lerp(pt, tg.xo, tg.getX()), Mth.lerp(pt, tg.yo, tg.getY()), Mth.lerp(pt, tg.zo, tg.getZ()),
				tg.getBbWidth() / 2.0, tg.getBbHeight(), "", 0xFFFF4444, -1f);
		}

		if (doAll || doInv || doTags) {
			for (Player o : mc.level.players()) {
				if (o == me || !o.isAlive()) continue;
				boolean inv = o.isInvisible();
				double dist = me.distanceTo(o);
				double ox = Mth.lerp(pt, o.xo, o.getX()), oy = Mth.lerp(pt, o.yo, o.getY()), oz = Mth.lerp(pt, o.zo, o.getZ());
				float frac = Mth.clamp(o.getHealth() / Math.max(1f, o.getMaxHealth()), 0f, 1f);
				int color = inv ? 0xFFFFD27C : ((o.getTeamColor() != 16777215 && o.getTeamColor() != -1) ? (0xFF000000 | o.getTeamColor()) : ACCENT);
				String oname = o.getName().getString(), sus = suspects.get(oname);
				boolean susOn = sus != null && tick - suspectTick.getOrDefault(oname, -9999L) < 1200;
				if (susOn) color = 0xFFFF4444;

				if ((doAll || (doInv && inv)) && dist <= esp.range.value) {
					String label = "";
					if (!doTags) {
						if (esp.names.value) label = o.getName().getString();
						if (esp.health.value) label += " " + (int) o.getHealth() + "hp";
						label += " " + (int) dist + "m";
						if (susOn) label += " [" + sus + "]";
					}
					espEntity(g, mc, cam, yaw, pitch, fov, sw, sh, ox, oy, oz,
						o.getBbWidth() / 2.0, o.getBbHeight(), label, color, esp.healthBar.value ? frac : -1f);
				}

				if (doTags && dist <= nameTags.range.value) {
					double[] sp = project(cam, yaw, pitch, fov, sw, sh, ox, oy + o.getBbHeight() + 0.35, oz);
					if (sp != null) {
						String t1 = o.getName().getString();
						if (nameTags.showHealth.value) t1 += " " + (int) o.getHealth() + "hp";
						if (nameTags.showDist.value) t1 += " " + (int) dist + "m";
						if (susOn) t1 += " [" + sus + "]";
						int tw = mc.font.width(t1) + 6, x = (int) sp[0] - tw / 2, y = (int) sp[1] - 12;
						g.fill(x, y, x + tw, y + 11, 0xB0000000);
						g.drawString(mc.font, t1, x + 3, y + 2, color);
						if (nameTags.showBar.value) {
							g.fill(x, y + 11, x + tw, y + 13, 0xFF2A2240);
							g.fill(x, y + 11, x + (int) (tw * frac), y + 13, frac > 0.5f ? 0xFF7CFFB0 : 0xFFFF7C7C);
						}
						if (nameTags.showItem.value && !o.getMainHandItem().isEmpty()) {
							String ht = o.getMainHandItem().getHoverName().getString();
							g.drawString(mc.font, ht, (int) sp[0] - mc.font.width(ht) / 2, y + 15, 0xFFDDDDDD);
						}
					}
				}
			}
		}

						if (doRes) {
			for (ItemEntity it : resItems) {
				if (it.isRemoved()) continue;
				ItemStack st = it.getItem();
				String nm; int col;
				if (resEsp.diamond.value && st.is(Items.DIAMOND)) { nm = "Elmas"; col = 0xFF55FFFF; }
				else if (resEsp.emerald.value && st.is(Items.EMERALD)) { nm = "Zumrut"; col = 0xFF55FF55; }
				else if (resEsp.gold.value && st.is(Items.GOLD_INGOT)) { nm = "Altin"; col = 0xFFFFAA00; }
				else if (resEsp.iron.value && st.is(Items.IRON_INGOT)) { nm = "Demir"; col = 0xFFFFFFFF; }
				else continue;
				double dist = me.distanceTo(it);
				if (dist > resEsp.range.value) continue;
				double[] sp = project(cam, yaw, pitch, fov, sw, sh, Mth.lerp(pt, it.xo, it.getX()), Mth.lerp(pt, it.yo, it.getY()) + 0.3, Mth.lerp(pt, it.zo, it.getZ()));
				if (sp == null) continue;
				int x = (int) sp[0], y = (int) sp[1];
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
				double[] sp = project(cam, yaw, pitch, fov, sw, sh, Mth.lerp(pt, it.xo, it.getX()), Mth.lerp(pt, it.yo, it.getY()) + 0.3, Mth.lerp(pt, it.zo, it.getZ()));
				if (sp == null) continue;
				int x = (int) sp[0], y = (int) sp[1], col = val ? 0xFFFFD27C : 0xFFDDDDDD;
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
				int x = (int) sp[0], y = (int) sp[1];
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

	static final String[] PARTICLE_NAMES = {
		"Kritik", "Kalp", "Alev", "Yildiz", "Buyu", "Yesil", "Havai fisek", "Vurus"
	};

	static ParticleOptions particleFor(int i) {
		switch (i) {
			case 1: return ParticleTypes.HEART;
			case 2: return ParticleTypes.FLAME;
			case 3: return ParticleTypes.END_ROD;
			case 4: return ParticleTypes.ENCHANT;
			case 5: return ParticleTypes.HAPPY_VILLAGER;
			case 6: return ParticleTypes.FIREWORK;
			case 7: return ParticleTypes.ENCHANTED_HIT;
			default: return ParticleTypes.CRIT;
		}
	}

	static void hitEffect(Minecraft mc, Entity e) {
		if (hitFx == null || !hitFx.enabled || mc.level == null) return;
		ParticleOptions opt = particleFor(hitFx.type.index);
		int n = (int) hitFx.amount.value;
		for (int i = 0; i < n; i++) {
			mc.level.addParticle(opt,
				e.getX() + (rnd() - 0.5) * 0.8, e.getY() + rnd() * e.getBbHeight(), e.getZ() + (rnd() - 0.5) * 0.8, 0, 0.1, 0);
		}
	}

	static boolean useDown(Minecraft mc, LocalPlayer p, Item item) {
		int slot = hotbarSlot(p, item);
		if (slot == -1) return false;
		float oldPitch = p.getXRot();
		int old = p.getInventory().selected;
		p.getInventory().selected = slot;
		float downPitch = gcdPitch(p, 89.5f);
		p.setXRot(downPitch);
		p.connection.send(new ServerboundMovePlayerPacket.Rot(p.getYRot(), downPitch, p.onGround()));
		mc.gameMode.useItem(p, InteractionHand.MAIN_HAND);
		p.setXRot(oldPitch);
		p.getInventory().selected = old;
		return true;
	}

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
		eatTicks = 34 + (int) rndRange(0, 4);
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
				mc.gameMode.handleInventoryMouseClick(p.inventoryMenu.containerId, i, 40, ClickType.SWAP, p);
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
		useDown(mc, p, Items.WATER_BUCKET);
	}

	static void runMacro(Minecraft mc, int a) {
		LocalPlayer p = mc.player;
		if (p == null || mc.screen != null || mc.gameMode == null) return;
		switch (a) {
			case 1: useSilently(mc, p, Items.ENDER_PEARL); break;
			case 2: startEat(mc, p); break;
			case 3: totemToOffhand(mc, p); break;
			case 4: equipBestSword(p); break;
			case 5: selectBlock(p); break;
			case 6: useFirework(mc, p, true); break;
			case 7: swapChest(mc, p, !p.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA), false); break;
			case 8: useDown(mc, p, Items.WATER_BUCKET); break;
			case 9: useDown(mc, p, Items.FIRE_CHARGE); break;
			case 10: useDown(mc, p, Items.SPLASH_POTION); break;
			default: break;
		}
	}

	static Vec3 aimAt(LivingEntity e, int mode, float predict) {
		double f = mode == 1 ? 0.88 : (mode == 2 ? 0.18 : 0.58);
		Vec3 mv = e.getDeltaMovement();
		return e.position().add(mv.x * predict, 0, mv.z * predict).add(0, e.getBbHeight() * f, 0);
	}

	static Path cfgPath() {
		return FabricLoader.getInstance().getConfigDir().resolve("kediclient.properties");
	}

			static Path slotPath(int i) {
		return FabricLoader.getInstance().getConfigDir().resolve("kediclient_cfg" + (i + 1) + ".properties");
	}

	static void saveTo(Path f) {
		try {
			Properties pr = new Properties();
			for (Mod m : MODS) {
				if (m instanceof CfgMod || m instanceof ClickGuiMod) continue;
				pr.setProperty(m.name + ".key", String.valueOf(m.key));
				pr.setProperty(m.name + ".enabled", String.valueOf(m.enabled));
				for (Setting s : m.settings) pr.setProperty(m.name + "." + s.name, s.get());
			}
			try (OutputStream o = Files.newOutputStream(f)) {
				pr.store(o, "KediClient cfg");
			}
		} catch (Exception ignored) {
		}
	}

static boolean loadFrom(Path f, Minecraft mc) {
		try {
			if (!Files.exists(f)) return false;
			Properties pr = new Properties();
			try (InputStream in = Files.newInputStream(f)) {
				pr.load(in);
			}
			for (Mod m : MODS) {
				if (m instanceof CfgMod || m instanceof ClickGuiMod) continue;
				try {
					String k = pr.getProperty(m.name + ".key");
					if (k != null) m.key = Integer.parseInt(k.trim());
				} catch (Exception ignored) {
				}
				for (Setting s : m.settings) {
					String v = pr.getProperty(m.name + "." + s.name);
					if (v != null) s.set(v);
				}
				String en = pr.getProperty(m.name + ".enabled");
				if (en != null) {
					boolean want = Boolean.parseBoolean(en.trim());
					if (want != m.enabled) {
						m.enabled = want;
						if (want) m.onEnable(mc);
						else m.onDisable(mc);
					}
				}
			}
			return true;
		} catch (Exception ex) {
			return false;
		}
	}

	static void saveConfig() {
		try {
			Properties pr = new Properties();
			for (Mod m : MODS) {
				pr.setProperty(m.name + ".key", String.valueOf(m.key));
				for (Setting s : m.settings) pr.setProperty(m.name + "." + s.name, s.get());
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
			String t = "KediClient", info = " | " + mc.getUser().getName();
			int w = mc.font.width(t + info) + 8;
			g.fill(3, 3, 3 + w, 15, 0xA0140C28);
			g.fill(3, 3, 5, 15, ACCENT);
			g.drawString(mc.font, t, 8, 6, ACCENT);
			g.drawString(mc.font, info, 8 + mc.font.width(t), 6, 0xFFCCCCCC);
		}

		List<String> lines = new ArrayList<>();
		if (hud.coords.value) lines.add(String.format("XYZ: %.0f %.0f %.0f", me.getX(), me.getY(), me.getZ()));
		if (hud.dir.value) lines.add("Yon: " + me.getDirection().getName());
		if (hud.fps.value) lines.add("FPS: " + fpsValue);
		if (hud.ping.value && mc.getConnection() != null) {
			var pi = mc.getConnection().getPlayerInfo(me.getUUID());
			if (pi != null) lines.add("Ping: " + pi.getLatency() + " ms");
		}
		if (hud.speed.value) lines.add(String.format("Hiz: %.1f b/s", speedBps));
		if (hud.time.value) {
			java.time.LocalTime lt = java.time.LocalTime.now();
			lines.add(String.format("Saat: %02d:%02d", lt.getHour(), lt.getMinute()));
		}
		if (hud.counters.value) {
			int totems = 0, rockets = 0;
			for (int i = 0; i < me.getInventory().getContainerSize(); i++) {
				ItemStack st = me.getInventory().getItem(i);
				if (st.is(Items.TOTEM_OF_UNDYING)) totems += st.getCount();
				if (st.is(Items.FIREWORK_ROCKET)) rockets += st.getCount();
			}
			lines.add("Totem: " + totems + "  Roket: " + rockets);
		}
		if (hud.bed.value && bedPos != null) {
			lines.add(String.format("Yatak: %.0f blok (%d %d %d)", bedDist, bedPos.getX(), bedPos.getY(), bedPos.getZ()));
		}
		if (hud.cpsHud.value) lines.add("CPS: " + leftClicks.size() + " | " + rightClicks.size());
		if (hud.armor.value) {
			EquipmentSlot[] slots = { EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET };
			StringBuilder sb = new StringBuilder("Zirh:");
			for (EquipmentSlot es : slots) {
				ItemStack st = me.getItemBySlot(es);
				if (st.isEmpty()) sb.append(" -");
				else if (st.getMaxDamage() <= 0) sb.append(" ok");
				else sb.append(" ").append((st.getMaxDamage() - st.getDamageValue()) * 100 / st.getMaxDamage()).append("%");
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
			int kx = sw - 56, ky = sh - 62;
			keyBox(g, mc, kx + 18, ky, "W", mc.options.keyUp.isDown());
			keyBox(g, mc, kx, ky + 18, "A", mc.options.keyLeft.isDown());
			keyBox(g, mc, kx + 18, ky + 18, "S", mc.options.keyDown.isDown());
			keyBox(g, mc, kx + 36, ky + 18, "D", mc.options.keyRight.isDown());
		}

		if (hud.arrayList.value) {
			List<Mod> on = new ArrayList<>();
			for (Mod m : MODS) if (m.enabled && m != hud) on.add(m);
			on.sort((a, b) -> Integer.compare(mc.font.width(b.name), mc.font.width(a.name)));
			int y = 3, i = 0;
			for (Mod m : on) {
				int w = mc.font.width(m.name), x = sw - w - 8, c = accent(i);
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
			float hp = t.getHealth(), mx = Math.max(1f, t.getMaxHealth());
			int bw = 110, bx = sw / 2 - bw / 2, by = sh / 2 + 24;
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
				float alpha = dt < 600 ? dt / 600f : (dt > 4000 ? (5000 - dt) / 1000f : 1f);
				int a = (int) (alpha * 255f);
				if (a > 10) {
					int color = (a << 24) | 0xB388FF, sub = (a << 24) | 0xDDDDDD;
					String title = "KediClient", subtitle = "Sag Shift = menu";
					g.pose().pushPose();
					g.pose().scale(3f, 3f, 1f);
					g.drawString(mc.font, title, (int) (sw / 6f - mc.font.width(title) / 2f), (int) (sh / 12f), color);
					g.pose().popPose();
					g.drawString(mc.font, subtitle, sw / 2 - mc.font.width(subtitle) / 2, sh / 4 + 34, sub);
				}
			}
		}
	}

static abstract class Setting {
		final String name;
		Setting(String name) { this.name = name; }
		abstract String get();
		abstract void set(String v);
	}

	static class BoolSetting extends Setting {
		boolean value;
		BoolSetting(String name, boolean value) { super(name); this.value = value; }
		@Override String get() { return String.valueOf(value); }
		@Override void set(String v) { value = Boolean.parseBoolean(v.trim()); }
	}

	static class FloatSetting extends Setting {
		float value, min, max;
		FloatSetting(String name, float value, float min, float max) { super(name); this.value = value; this.min = min; this.max = max; }
		@Override String get() { return String.valueOf(value); }
		@Override void set(String v) {
			try { value = Math.max(min, Math.min(max, Float.parseFloat(v.trim()))); } catch (Exception ignored) {}
		}
	}

	static class ModeSetting extends Setting {
		int index;
		final String[] options;
		ModeSetting(String name, int index, String... options) { super(name); this.index = index; this.options = options; }
		@Override String get() { return String.valueOf(index); }
		@Override void set(String v) {
			try { index = Math.max(0, Math.min(options.length - 1, Integer.parseInt(v.trim()))); } catch (Exception ignored) {}
		}
	}

	static class Mod {
		final String name, category;
		int key;
		boolean enabled = false, expanded = false, keyDown = false;
		final List<Setting> settings = new ArrayList<>();
		Mod(String name, String category, int key) { this.name = name; this.category = category; this.key = key; }
		BoolSetting bool(String n, boolean v) { BoolSetting s = new BoolSetting(n, v); settings.add(s); return s; }
		FloatSetting num(String n, float v, float min, float max) { FloatSetting s = new FloatSetting(n, v, min, max); settings.add(s); return s; }
		ModeSetting mode(String n, int idx, String... opts) { ModeSetting s = new ModeSetting(n, idx, opts); settings.add(s); return s; }
		void toggle(Minecraft mc) {
			enabled = !enabled;
			if (enabled) onEnable(mc); else onDisable(mc);
			saveConfig();
		}
		void onPress(Minecraft mc) { toggle(mc); }
		void onEnable(Minecraft mc) {}
		void onDisable(Minecraft mc) {}
		void onPreTick(Minecraft mc, LocalPlayer p) {}
		void onTick(Minecraft mc, LocalPlayer p) {}
	}

	static class Aura extends Mod {
		FloatSetting range = num("Menzil", 3.8f, 2.5f, 5.5f);
		FloatSetting cps = num("CPS", 11f, 4f, 18f);
		FloatSetting smooth = num("Donus hizi", 18f, 4f, 45f);
		FloatSetting fov = num("FOV", 110f, 30f, 180f);
		ModeSetting aimPos = mode("Nisan noktasi", 0, "Govde", "Bas", "Ayak");
		FloatSetting predict = num("Hareket tahmini", 1.2f, 0f, 3f);
		BoolSetting playersOnly = bool("Sadece oyuncu", true);
		BoolSetting mobs = bool("Canavarlar", false);
		BoolSetting animals = bool("Hayvanlar", false);
		BoolSetting invis = bool("Gorunmezler", true);
		BoolSetting teamCheck = bool("Takim korumasi", true);
		BoolSetting autoSword = bool("Oto kilic", true);
		BoolSetting wtap = bool("W-Tap (kombo)", true);
		BoolSetting showTarget = bool("Hedef kutusu", true);
		BoolSetting silentRot = bool("Silent rotasyon", true);
		int cd = 0;

		Aura() { super("Aura", "Combat", GLFW.GLFW_KEY_R); }

		private boolean valid(Minecraft mc, LocalPlayer me, Entity e) {
			if (!(e instanceof LivingEntity le) || !le.isAlive() || e == me) return false;
			if (!invis.value && e.isInvisible()) return false;
			if (e instanceof Player pl) {
				if (!playersOnly.value || pl.isSpectator() || pl.isCreative() || isBot(mc, pl)) return false;
				if (teamCheck.value && me.getTeam() != null && pl.getTeam() != null && me.getTeam().isAlliedTo(pl.getTeam())) return false;
				return true;
			}
			if (e instanceof Enemy) return mobs.value;
			if (e instanceof Animal) return animals.value;
			return false;
		}

		@Override void onTick(Minecraft mc, LocalPlayer p) {
			if (cd > 0) cd--;
			LivingEntity best = null;
			double bd = range.value;
			for (Entity e : mc.level.entitiesForRendering()) {
				if (!valid(mc, p, e)) continue;
				double d = p.distanceTo(e);
				if (d < bd && angleDiff(p, rotationTo(p, aimAt((LivingEntity) e, aimPos.index, predict.value))) <= fov.value) {
					bd = d; best = (LivingEntity) e;
				}
			}
			auraTarget = best;
			if (best == null) return;
			float[] rot = rotationTo(p, aimAt(best, aimPos.index, predict.value));
			if (silentRot.value) applySilentRot(p, rot, 2);
			else rotateToward(p, rot, smooth.value);
			if (autoSword.value && isWeapon(p.getMainHandItem())) equipBestSword(p);
			if (cd <= 0 && angleDiff(p, rot) <= 28f) {
				if (wtap.value && p.isSprinting() && rnd() < 0.35) { p.setSprinting(false); wtapTicks = 2; }
				if (criticals != null && criticals.enabled && p.onGround() && !p.isInWater() && !p.isInLava() && !p.onClimbable()) {
					p.setDeltaMovement(p.getDeltaMovement().x, 0.42, p.getDeltaMovement().z);
				}
				mc.gameMode.attack(p, best);
				p.swing(InteractionHand.MAIN_HAND);
				hitEffect(mc, best);
				double baseDelay = 20.0 / Math.max(4.0, Math.min(18.0, cps.value));
				cd = Math.max(1, (int) Math.round(baseDelay + rndRange(-0.8, 1.2)));
			}
		}

		@Override void onDisable(Minecraft mc) { auraTarget = null; silentActive = false; }
	}

	static class TriggerBot extends Mod {
		FloatSetting cps = num("CPS", 11f, 4f, 18f);
		BoolSetting playersOnly = bool("Sadece oyuncu", true);
		int cd = 0;
		TriggerBot() { super("TriggerBot", "Combat", -1); }

		@Override void onTick(Minecraft mc, LocalPlayer p) {
			if (cd > 0) { cd--; return; }
			if (mc.screen != null || !(mc.hitResult instanceof EntityHitResult ehr)) return;
			Entity e = ehr.getEntity();
			if (!(e instanceof LivingEntity le) || !le.isAlive() || e == p) return;
			if (playersOnly.value && !(e instanceof Player)) return;
			if (criticals != null && criticals.enabled && p.onGround() && !p.isInWater() && !p.isInLava() && !p.onClimbable()) {
				p.setDeltaMovement(p.getDeltaMovement().x, 0.42, p.getDeltaMovement().z);
			}
			mc.gameMode.attack(p, le);
			p.swing(InteractionHand.MAIN_HAND);
			hitEffect(mc, le);
			double baseDelay = 20.0 / Math.max(4.0, Math.min(18.0, cps.value));
			cd = Math.max(1, (int) Math.round(baseDelay + rndRange(-0.7, 1.1)));
		}
	}

	static class Criticals extends Mod {
		BoolSetting autoJump = bool("Oto ziplama", true);
		Criticals() { super("Criticals", "Combat", -1); }
	}

								   	static class Velocity extends Mod {
		FloatSetting hPct = num("Yatay (%)", 85f, 0f, 100f);
		FloatSetting vPct = num("Dikey (%)", 90f, 0f, 100f);
		int hurtCd = 0;
		Velocity() { super("Velocity", "Combat", -1); }

		@Override void onTick(Minecraft mc, LocalPlayer p) {
			if (hurtCd > 0) hurtCd--;
			if (p.hurtTime > 0 && hurtCd == 0) {
				Vec3 v = p.getDeltaMovement();
				double hs = Math.max(0.55, Math.min(1.0, hPct.value / 100.0)) * rndRange(0.95, 1.04);
				double vs = Math.max(0.60, Math.min(1.0, vPct.value / 100.0)) * rndRange(0.95, 1.04);
				p.setDeltaMovement(v.x * hs, v.y * vs, v.z * hs);
				hurtCd = 3 + (int) rndRange(0, 3);
			}
		}
	}

	static class AutoTotem extends Mod {
		FloatSetting health = num("Can esigi", 10f, 2f, 20f);
		int cd = 0;
		AutoTotem() { super("AutoTotem", "Combat", -1); }

		@Override void onTick(Minecraft mc, LocalPlayer p) {
			if (cd > 0) { cd--; return; }
			if (p.getHealth() <= health.value && !p.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) {
				totemToOffhand(mc, p);
				cd = 3 + (int) rndRange(0, 3);
			}
		}
	}

	static class AutoClicker extends Mod {
		FloatSetting cps = num("CPS", 11f, 4f, 18f);
		BoolSetting rightClick = bool("Sag tik", false);
		int cd = 0;
		AutoClicker() { super("AutoClicker", "Combat", -1); }

		@Override void onTick(Minecraft mc, LocalPlayer p) {
			if (cd > 0) { cd--; return; }
			if (mc.screen != null) return;
			double baseDelay = 20.0 / Math.max(4.0, Math.min(18.0, cps.value));
			if (mc.options.keyAttack.isDown()) {
				if (mc.hitResult instanceof EntityHitResult ehr) {
					mc.gameMode.attack(p, ehr.getEntity());
					hitEffect(mc, ehr.getEntity());
				}
				p.swing(InteractionHand.MAIN_HAND);
				cd = Math.max(1, (int) Math.round(baseDelay + rndRange(-0.8, 1.2)));
			} else if (rightClick.value && mc.options.keyUse.isDown()) {
				mc.gameMode.useItem(p, InteractionHand.MAIN_HAND);
				cd = Math.max(1, (int) Math.round(baseDelay + rndRange(-0.8, 1.2)));
			}
		}
	}

	static class AutoGApple extends Mod {
		FloatSetting health = num("Can esigi", 12f, 2f, 20f);
		int cd = 0;
		AutoGApple() { super("AutoGApple", "Combat", -1); }

		@Override void onTick(Minecraft mc, LocalPlayer p) {
			if (cd > 0) { cd--; return; }
			if (p.getHealth() <= health.value && eatTicks == 0) {
				startEat(mc, p);
				cd = 45 + (int) rndRange(0, 15);
			}
		}
	}

	static class AutoPot extends Mod {
		FloatSetting health = num("Can esigi", 10f, 2f, 20f);
		int cd = 0;
		AutoPot() { super("AutoPot", "Combat", -1); }

		@Override void onTick(Minecraft mc, LocalPlayer p) {
			if (cd > 0) { cd--; return; }
			if (p.getHealth() <= health.value && useDown(mc, p, Items.SPLASH_POTION)) {
				cd = 25 + (int) rndRange(0, 10);
			}
		}
	}

static class ElytraBooster extends Mod {
		FloatSetting accel = num("Hizlanma", 0.06f, 0.01f, 0.3f);
		FloatSetting max = num("Max hiz", 2.0f, 0.5f, 4.0f);
		BoolSetting vertical = bool("Ziplayinca yuksel", true);
		FloatSetting climb = num("Yukselme", 0.08f, 0.02f, 0.3f);
		ElytraBooster() { super("ElytraBooster", "Elytra", -1); }

		@Override void onTick(Minecraft mc, LocalPlayer p) {
			if (mc.screen != null || !p.isFallFlying()) return;
			Vec3 v = p.getDeltaMovement();
			float safeAccel = Math.min(accel.value, 0.09f), safeMax = Math.min(max.value, 2.2f);
			if (mc.options.keyUp.isDown()) {
				Vec3 nv = v.add(p.getLookAngle().scale(safeAccel));
				if (nv.length() > safeMax) nv = nv.normalize().scale(safeMax);
				p.setDeltaMovement(nv);
			} else if (mc.options.keyShift.isDown()) p.setDeltaMovement(v.scale(0.92));
			if (vertical.value && mc.options.keyJump.isDown()) {
				Vec3 c = p.getDeltaMovement();
				p.setDeltaMovement(c.x, Math.min(c.y + Math.min(climb.value, 0.09f), 0.75), c.z);
			}
		}
	}

	static class ElytraTarget extends Mod {
		FloatSetting range = num("Menzil", 60f, 10f, 120f);
		FloatSetting smooth = num("Yumusaklik", 5f, 1f, 20f);
		FloatSetting predict = num("Tahmin", 3f, 0f, 10f);
		BoolSetting onlyForward = bool("Sadece ileri tusu", true);
		ElytraTarget() { super("ElytraTarget", "Elytra", -1); }

		@Override void onTick(Minecraft mc, LocalPlayer p) {
			if (mc.screen != null || !p.isFallFlying() || (onlyForward.value && !mc.options.keyUp.isDown())) return;
			Player best = null; double bd = range.value;
			for (Player o : mc.level.players()) {
				if (o == p || o.isSpectator() || !o.isAlive()) continue;
				double d = p.distanceTo(o);
				if (d < bd) { bd = d; best = o; }
			}
			if (best == null) return;
			float[] rot = rotationTo(p, best.position().add(best.getDeltaMovement().scale(predict.value)).add(0, best.getBbHeight() * 0.5, 0));
			float sm = Math.max(2.5f, smooth.value);
			p.setYRot(gcdYaw(p, p.getYRot() + Mth.wrapDegrees(rot[0] - p.getYRot()) / sm + (float) ((rnd() - 0.5) * 0.7)));
			p.setXRot(gcdPitch(p, Mth.clamp(p.getXRot() + (rot[1] - p.getXRot()) / sm + (float) ((rnd() - 0.5) * 0.5), -90f, 90f)));
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
		int cd = 0, fwCd = 0;
		ElytraHelper() { super("ElytraHelper", "Elytra", -1); }

		@Override void onTick(Minecraft mc, LocalPlayer p) {
			if (mc.screen != null) return;
			if (cd > 0) cd--; if (fwCd > 0) fwCd--;
			boolean hotbar = swapMode.index == 1, elytraOn = p.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA);
			boolean falling = !p.onGround() && !p.isFallFlying() && !p.isInWater() && p.getDeltaMovement().y < -fallSpeed.value;
			if (autoSwap.value && falling && cd == 0 && airBelow(mc, p, (int) minHeight.value)) {
				if (elytraOn) {
					if (p.tryToStartFallFlying()) p.connection.send(new ServerboundPlayerCommandPacket(p, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
					cd = 6;
				} else cd = swapChest(mc, p, true, hotbar) ? 5 : 20;
			}
			if (swapBack.value && elytraOn && p.onGround() && cd == 0) { swapChest(mc, p, false, hotbar); cd = 20; }
			if (rocket.index != 0 && p.isFallFlying() && fwCd == 0) {
				if ((rocket.index == 2 || p.getDeltaMovement().length() < fwSpeed.value) && useFirework(mc, p, silentFw.value)) {
					fwCd = Math.max(25, (int) fwDelay.value + (int) rndRange(-4, 6));
				}
			}
		}
	}

	static class ElytraFirework extends Mod {
		BoolSetting silent = bool("Silent", true);
		ElytraFirework() { super("ElytraFirework", "Elytra", -1); }
		@Override void onPress(Minecraft mc) {
			LocalPlayer p = mc.player;
			if (p != null && mc.screen == null && p.isFallFlying() && useFirework(mc, p, silent.value)) {
				p.displayClientMessage(Component.literal("Roket kullanildi"), true);
			}
		}
		@Override void toggle(Minecraft mc) { onPress(mc); }
	}

					static class AutoSprint extends Mod {
		AutoSprint() { super("AutoSprint", "Move", GLFW.GLFW_KEY_H); }
		@Override void onTick(Minecraft mc, LocalPlayer p) {
			if (mc.screen != null) return;
			if (wtapTicks > 0) { wtapTicks--; return; }
			if (mc.options.keyUp.isDown() && !p.isShiftKeyDown() && !p.horizontalCollision && p.getFoodData().getFoodLevel() > 6) p.setSprinting(true);
		}
	}

	static class NoFall extends Mod {
		NoFall() { super("NoFall", "Move", -1); }
		@Override void onTick(Minecraft mc, LocalPlayer p) {
			if (p.fallDistance > 3.2f && !p.isFallFlying() && !useDown(mc, p, Items.WATER_BUCKET)) {
				p.connection.send(new ServerboundMovePlayerPacket.Pos(p.getX(), p.getY(), p.getZ(), p.onGround()));
			}
		}
	}

	static class Speed extends Mod {
		FloatSetting speed = num("Hiz", 0.28f, 0.1f, 1.0f);
		BoolSetting bhop = bool("Bhop (ziplama)", true);
		Speed() { super("Speed", "Move", -1); }
		@Override void onTick(Minecraft mc, LocalPlayer p) {
			if (mc.screen != null || p.isFallFlying() || p.isInWater() || p.getAbilities().flying) return;
			float fwd = (mc.options.keyUp.isDown() ? 1f : 0f) - (mc.options.keyDown.isDown() ? 1f : 0f);
			float str = (mc.options.keyLeft.isDown() ? 1f : 0f) - (mc.options.keyRight.isDown() ? 1f : 0f);
			if (fwd == 0f && str == 0f) return;
			double len = Math.sqrt(fwd * fwd + str * str), f = fwd / len, st = str / len, yaw = Math.toRadians(p.getYRot());
			double mx = st * Math.cos(yaw) - f * Math.sin(yaw), mz = f * Math.cos(yaw) + st * Math.sin(yaw);
			Vec3 v = p.getDeltaMovement(); double y = v.y, cap = Math.min(speed.value, 0.34) * rndRange(0.95, 1.03);
			if (bhop.value && p.onGround()) { y = 0.42; p.setSprinting(true); }
			p.setDeltaMovement(mx * cap, y, mz * cap);
		}
	}

	static class SafeWalk extends Mod {
		boolean forced = false;
		SafeWalk() { super("SafeWalk", "Move", -1); }
		@Override void onPreTick(Minecraft mc, LocalPlayer p) {
			if (mc.screen != null || !p.onGround() || p.getAbilities().flying) { if (forced) { mc.options.keyShift.setDown(false); forced = false; } return; }
			Vec3 v = p.getDeltaMovement(); double hz = Math.sqrt(v.x * v.x + v.z * v.z);
			Vec3 ahead = p.position().add(v.x * 2.5, 0, v.z * 2.5);
			boolean edge = hz > 0.01 && mc.level.getBlockState(BlockPos.containing(ahead.x, p.getY() - 0.5, ahead.z)).isAir();
			if (edge) { mc.options.keyShift.setDown(true); forced = true; } else if (forced) { mc.options.keyShift.setDown(false); forced = false; }
		}
		@Override void onDisable(Minecraft mc) { if (forced) mc.options.keyShift.setDown(false); forced = false; }
	}

	static class Parkour extends Mod {
		int jumpCd = 0;
		Parkour() { super("Parkour", "Move", -1); }
		@Override void onPreTick(Minecraft mc, LocalPlayer p) {
			if (jumpCd > 0) jumpCd--;
			if (mc.screen != null || !p.onGround() || p.isShiftKeyDown() || p.getAbilities().flying || jumpCd > 0) return;
			Vec3 v = p.getDeltaMovement();
			if (Math.sqrt(v.x * v.x + v.z * v.z) < 0.05) return;
			Vec3 ahead = p.position().add(v.x * 1.8, 0, v.z * 1.8);
			if (mc.level.getBlockState(BlockPos.containing(ahead.x, p.getY() - 0.5, ahead.z)).isAir()) { p.setDeltaMovement(v.x, 0.42, v.z); jumpCd = 6; }
		}
	}

	static class Glide extends Mod {
		FloatSetting fall = num("Dusme hizi", 0.1f, 0.02f, 0.3f);
		Glide() { super("Glide", "Move", -1); }
		@Override void onTick(Minecraft mc, LocalPlayer p) {
			if (p.onGround() || p.isFallFlying() || p.isInWater() || p.getAbilities().flying) return;
			Vec3 v = p.getDeltaMovement(); double safeFall = Math.max(fall.value, 0.075);
			if (v.y < -safeFall) p.setDeltaMovement(v.x, -safeFall * rndRange(0.95, 1.05), v.z);
		}
	}

	static class FastLadder extends Mod {
		FastLadder() { super("FastLadder", "Move", -1); }
		@Override void onTick(Minecraft mc, LocalPlayer p) {
			if (mc.screen != null) return;
			if (p.onClimbable() && p.horizontalCollision && mc.options.keyUp.isDown()) {
				Vec3 v = p.getDeltaMovement();
				if (v.y > 0.05) p.setDeltaMovement(v.x, Math.min(0.22, v.y * 1.25), v.z);
			}
		}
	}

	static class InvMove extends Mod {
		InvMove() { super("InvMove", "Move", -1); }
		@Override void onTick(Minecraft mc, LocalPlayer p) {
			if (!(mc.screen instanceof AbstractContainerScreen)) return;
			long win = mc.getWindow().getWindow();
			mc.options.keyUp.setDown(isDown(win, GLFW.GLFW_KEY_W)); mc.options.keyDown.setDown(isDown(win, GLFW.GLFW_KEY_S));
			mc.options.keyLeft.setDown(isDown(win, GLFW.GLFW_KEY_A)); mc.options.keyRight.setDown(isDown(win, GLFW.GLFW_KEY_D));
			mc.options.keyJump.setDown(isDown(win, GLFW.GLFW_KEY_SPACE)); mc.options.keySprint.setDown(isDown(win, GLFW.GLFW_KEY_LEFT_CONTROL));
		}
	}

	static class Flight extends Mod {
		FloatSetting speed = num("Hiz", 0.1f, 0.05f, 0.5f);
		Flight() { super("Flight", "Move", -1); }
		@Override void onTick(Minecraft mc, LocalPlayer p) {
			if (p.isCreative() || p.isSpectator()) { p.getAbilities().setFlyingSpeed(speed.value); return; }
			Vec3 v = p.getDeltaMovement(); double y = v.y;
			if (mc.options.keyJump.isDown()) y = Math.min(0.18, y + 0.04);
			else if (mc.options.keyShift.isDown()) y = Math.max(-0.22, y - 0.04);
			else if (y < -0.08) y = -0.075 * rndRange(0.92, 1.08);
			double cap = Math.min(speed.value * 2.2, 0.28);
			p.setDeltaMovement(v.x * 0.96, y, v.z * 0.96);
			if (Math.hypot(v.x, v.z) > cap) { double s = cap / Math.hypot(v.x, v.z); p.setDeltaMovement(v.x * s, y, v.z * s); }
		}
		@Override void onDisable(Minecraft mc) {
			LocalPlayer p = mc.player;
			if (p == null) return;
			p.getAbilities().setFlyingSpeed(0.05f);
			if (!p.isCreative() && !p.isSpectator()) { p.getAbilities().mayfly = false; p.getAbilities().flying = false; }
		}
	}

	static class Fullbright extends Mod {
		Fullbright() { super("Fullbright", "Render", GLFW.GLFW_KEY_G); }
		@Override void onTick(Minecraft mc, LocalPlayer p) { p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 1000, 0, false, false, false)); }
		@Override void onDisable(Minecraft mc) { if (mc.player != null) mc.player.removeEffect(MobEffects.NIGHT_VISION); }
	}

	static class FovChanger extends Mod {
		FloatSetting fov = num("FOV", 30f, 5f, 110f); int old = -1;
		FovChanger() { super("Zoom", "Render", -1); }
		@Override void onEnable(Minecraft mc) { old = mc.options.fov().get(); }
		@Override void onTick(Minecraft mc, LocalPlayer p) { mc.options.fov().set((int) fov.value); }
		@Override void onDisable(Minecraft mc) { if (old > 0) mc.options.fov().set(old); }
	}

	static class Esp extends Mod {
		BoolSetting boxes = bool("Kutu", true), healthBar = bool("Can cubugu", true), tracers = bool("Cizgi (tracer)", false);
		BoolSetting names = bool("Isim", true), health = bool("Can yazisi", true);
		FloatSetting range = num("Menzil", 120f, 20f, 300f), fovMul = num("FOV ayari", 1.0f, 0.7f, 1.3f);
		Esp() { super("ESP", "Render", -1); }
	}

	static class NameTags extends Mod {
		BoolSetting showHealth = bool("Can yazisi", true), showBar = bool("Can bari", true), showDist = bool("Mesafe", true), showItem = bool("Elindeki esya", true);
		FloatSetting range = num("Menzil", 100f, 10f, 300f);
		NameTags() { super("NameTags", "Render", -1); }
	}

					static class ItemEsp extends Mod {
		BoolSetting onlyValuable = bool("Sadece degerli", false); FloatSetting range = num("Menzil", 50f, 10f, 150f);
		ItemEsp() { super("ItemESP", "Render", -1); }
		@Override void onTick(Minecraft mc, LocalPlayer p) { if (tick % 5 == 0) allItems = mc.level.getEntitiesOfClass(ItemEntity.class, p.getBoundingBox().inflate(range.value), x -> true); }
		@Override void onDisable(Minecraft mc) { allItems = new ArrayList<>(); }
	}

	static class Particles extends Mod {
		ModeSetting type = mode("Tur", 1, PARTICLE_NAMES); FloatSetting amount = num("Miktar", 3f, 1f, 15f);
		FloatSetting radius = num("Yaricap", 1.2f, 0.3f, 3f); BoolSetting onlyMoving = bool("Sadece hareketliyken", false);
		Particles() { super("Particles", "Render", -1); }
		@Override void onTick(Minecraft mc, LocalPlayer p) {
			if (onlyMoving.value && p.getDeltaMovement().x * p.getDeltaMovement().x + p.getDeltaMovement().z * p.getDeltaMovement().z < 0.0004) return;
			ParticleOptions opt = particleFor(type.index);
			for (int i = 0; i < (int) amount.value; i++) {
				mc.level.addParticle(opt, p.getX() + (rnd() - 0.5) * 2.0 * radius.value, p.getY() + rnd() * 2.0, p.getZ() + (rnd() - 0.5) * 2.0 * radius.value, 0, 0.02, 0);
			}
		}
	}

	static class HitParticles extends Mod {
		ModeSetting type = mode("Tur", 0, PARTICLE_NAMES); FloatSetting amount = num("Miktar", 8f, 1f, 30f);
		HitParticles() { super("HitParticles", "Render", -1); }
	}

	static class TimeChanger extends Mod {
		FloatSetting time = num("Zaman (0-24000)", 6000f, 0f, 24000f);
		TimeChanger() { super("TimeChanger", "Render", -1); }
		@Override void onTick(Minecraft mc, LocalPlayer p) { mc.level.setDayTime((long) time.value); }
	}

	static class NoWeather extends Mod {
		NoWeather() { super("NoWeather", "Render", -1); }
		@Override void onTick(Minecraft mc, LocalPlayer p) { mc.level.setRainLevel(0f); mc.level.setThunderLevel(0f); }
	}

	static class AutoRespawn extends Mod {
		AutoRespawn() { super("AutoRespawn", "Player", -1); }
		@Override void onTick(Minecraft mc, LocalPlayer p) { if (p.isDeadOrDying()) p.respawn(); }
	}

	static class ChestStealer extends Mod {
		FloatSetting speed = num("Hiz", 1f, 1f, 6f); int waitTicks = 0;
		ChestStealer() { super("ChestStealer", "Player", -1); }
		@Override void onTick(Minecraft mc, LocalPlayer p) {
			if (waitTicks > 0) { waitTicks--; return; }
			if (!(p.containerMenu instanceof ChestMenu menu)) return;
			int total = menu.getRowCount() * 9, moved = 0, limit = Math.min((int) speed.value, 3);
			for (int i = 0; i < total && moved < limit; i++) {
				if (!menu.getSlot(i).getItem().isEmpty()) { mc.gameMode.handleInventoryMouseClick(menu.containerId, i, 0, ClickType.QUICK_MOVE, p); moved++; }
			}
			waitTicks = 2 + (int) rndRange(0, 3);
		}
	}

	static class AntiAfk extends Mod {
		AntiAfk() { super("AntiAFK", "Player", -1); }
		@Override void onTick(Minecraft mc, LocalPlayer p) {
			if (mc.screen != null) return;
			if (tick % 400 == 0) { p.swing(InteractionHand.MAIN_HAND); p.setYRot(gcdYaw(p, p.getYRot() + (float) rndRange(2.5, 5.0))); }
		}
	}

	static class AutoWalk extends Mod {
		AutoWalk() { super("AutoWalk", "Player", -1); }
		@Override void onTick(Minecraft mc, LocalPlayer p) { if (mc.screen == null) mc.options.keyUp.setDown(true); }
		@Override void onDisable(Minecraft mc) { mc.options.keyUp.setDown(false); }
	}

	static class AntiBlind extends Mod {
		AntiBlind() { super("AntiBlind", "Player", -1); }
		@Override void onTick(Minecraft mc, LocalPlayer p) { p.removeEffect(MobEffects.BLINDNESS); p.removeEffect(MobEffects.DARKNESS); }
	}

	static class AutoTool extends Mod {
		AutoTool() { super("AutoTool", "Player", -1); }
		@Override void onTick(Minecraft mc, LocalPlayer p) {
			if (mc.screen != null || !mc.options.keyAttack.isDown() || !(mc.hitResult instanceof BlockHitResult bhr) || bhr.getType() != HitResult.Type.BLOCK) return;
			BlockState st = mc.level.getBlockState(bhr.getBlockPos());
			float bestSpeed = p.getInventory().getItem(p.getInventory().selected).getDestroySpeed(st); int bestSlot = -1;
			for (int i = 0; i < 9; i++) { float sp = p.getInventory().getItem(i).getDestroySpeed(st); if (sp > bestSpeed) { bestSpeed = sp; bestSlot = i; } }
			if (bestSlot != -1) p.getInventory().selected = bestSlot;
		}
	}

	static class Scaffold extends Mod {
		FloatSetting delay = num("Gecikme (tick)", 1f, 0f, 6f), extend = num("Ileri uzatma", 0f, 0f, 3f);
		BoolSetting autoSwitch = bool("Blok sec", true), tower = bool("Kule (ziplayinca)", true), swing = bool("El salla", true);
		int cd = 0;
		Scaffold() { super("Scaffold", "Bedwars", -1); }
		@Override void onTick(Minecraft mc, LocalPlayer p) {
			if (mc.screen != null) return;
			if (cd > 0) { cd--; return; }
			if (autoSwitch.value && !(p.getMainHandItem().getItem() instanceof BlockItem)) selectBlock(p);
			if (!(p.getMainHandItem().getItem() instanceof BlockItem)) return;
			Vec3 v = p.getDeltaMovement(); double hz = Math.sqrt(v.x * v.x + v.z * v.z);
			List<BlockPos> targets = new ArrayList<>(); targets.add(p.blockPosition().below());
			if (extend.value > 0.01f && hz > 0.02) {
				float safeExt = Math.min(extend.value, 1.2f);
				targets.add(BlockPos.containing(p.getX() + (v.x / hz) * safeExt, p.getY() - 1, p.getZ() + (v.z / hz) * safeExt));
			}
			for (BlockPos t : targets) {
				if (!mc.level.getBlockState(t).isAir()) continue;
				if (placeAt(mc, p, t)) {
					if (swing.value) p.swing(InteractionHand.MAIN_HAND);
					cd = (tower.value && mc.options.keyJump.isDown()) ? 2 : (Math.max(2, (int) delay.value) + (rnd() < 0.35 ? 1 : 0));
					break;
				}
			}
		}
	}

	static class ResourceEsp extends Mod {
		BoolSetting diamond = bool("Elmas", true), emerald = bool("Zumrut", true), gold = bool("Altin", true), iron = bool("Demir", false);
		FloatSetting range = num("Menzil", 60f, 10f, 150f);
		ResourceEsp() { super("ResourceESP", "Bedwars", -1); }
		@Override void onTick(Minecraft mc, LocalPlayer p) { if (tick % 5 == 0) resItems = mc.level.getEntitiesOfClass(ItemEntity.class, p.getBoundingBox().inflate(range.value), x -> true); }
		@Override void onDisable(Minecraft mc) { resItems = new ArrayList<>(); }
	}

	static class BedEsp extends Mod {
		FloatSetting radius = num("Yaricap", 32f, 8f, 64f); int sliceX = Integer.MIN_VALUE; BlockPos center = BlockPos.ZERO; List<BlockPos> building = new ArrayList<>();
		BedEsp() { super("BedESP", "Bedwars", -1); }
		@Override void onTick(Minecraft mc, LocalPlayer p) {
			int r = (int) radius.value, ry = Math.min(r, 24);
			if (sliceX == Integer.MIN_VALUE) { center = p.blockPosition(); sliceX = -r; building = new ArrayList<>(); }
			for (int n = 0; n < 2 && sliceX <= r; n++) {
				for (int y = -ry; y <= ry; y++) for (int z = -r; z <= r; z++) {
					BlockPos pos = center.offset(sliceX, y, z);
					if (mc.level.getBlockState(pos).is(BlockTags.BEDS)) building.add(pos);
				}
				sliceX++;
			}
			if (sliceX > r) {
				List<BlockPos> kept = new ArrayList<>();
				for (BlockPos b : building) { boolean dup = false; for (BlockPos k : kept) if (b.distSqr(k) <= 2) { dup = true; break; } if (!dup) kept.add(b); }
				beds = kept; sliceX = Integer.MIN_VALUE;
			}
			if (tick % 10 == 0) {
				double best = Double.MAX_VALUE; BlockPos found = null;
				for (BlockPos b : beds) { double d = b.distToCenterSqr(p.getX(), p.getY(), p.getZ()); if (d < best) { best = d; found = b; } }
				bedPos = found; bedDist = found == null ? -1 : Math.sqrt(best);
			}
		}
		@Override void onDisable(Minecraft mc) { beds = new ArrayList<>(); bedPos = null; bedDist = -1; sliceX = Integer.MIN_VALUE; }
	}

		static class Nuker extends Mod {
		FloatSetting range = num("Menzil", 4.5f, 2f, 6f); BoolSetting onlyBeds = bool("Sadece yatak", true), swing = bool("El salla", true);
		Nuker() { super("Nuker", "Bedwars", -1); }
		@Override void onTick(Minecraft mc, LocalPlayer p) {
			if (mc.screen != null || mc.gameMode == null) return;
			float safeR = Math.min(range.value, 4.5f); int r = (int) Math.ceil(safeR); BlockPos c = p.blockPosition(); Vec3 eye = p.getEyePosition();
			double maxSq = safeR * safeR, best = Double.MAX_VALUE; BlockPos found = null;
			for (int x = -r; x <= r; x++) for (int y = -r; y <= r; y++) for (int z = -r; z <= r; z++) {
				BlockPos pos = c.offset(x, y, z); BlockState st = mc.level.getBlockState(pos);
				if (st.isAir()) continue;
				if (onlyBeds.value) { if (!st.is(BlockTags.BEDS)) continue; } else if (st.getDestroySpeed(mc.level, pos) < 0f) continue;
				double d = eye.distanceToSqr(Vec3.atCenterOf(pos));
				if (d <= maxSq && d < best) { best = d; found = pos; }
			}
			if (found == null) return;
			rotateToward(p, rotationTo(p, Vec3.atCenterOf(found)), 35f);
			Vec3 diff = eye.subtract(Vec3.atCenterOf(found)); double ax = Math.abs(diff.x), ay = Math.abs(diff.y), az = Math.abs(diff.z);
			Direction face = (ay >= ax && ay >= az) ? (diff.y > 0 ? Direction.UP : Direction.DOWN) : (ax >= az ? (diff.x > 0 ? Direction.EAST : Direction.WEST) : (diff.z > 0 ? Direction.SOUTH : Direction.NORTH));
			mc.gameMode.continueDestroyBlock(found, face);
			if (swing.value) p.swing(InteractionHand.MAIN_HAND);
		}
	}

	static class BlockIn extends Mod {
		int cd = 0;
		BlockIn() { super("BlockIn", "Bedwars", -1); }
		@Override void onTick(Minecraft mc, LocalPlayer p) {
			if (mc.screen != null) return;
			if (cd > 0) { cd--; return; }
			if (!(p.getMainHandItem().getItem() instanceof BlockItem)) selectBlock(p);
			if (!(p.getMainHandItem().getItem() instanceof BlockItem)) return;
			BlockPos f = p.blockPosition(); Direction[] hs = { Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST };
			List<BlockPos> order = new ArrayList<>();
			for (Direction d : hs) order.add(f.relative(d));
			for (Direction d : hs) order.add(f.above().relative(d));
			order.add(f.above(2));
			boolean done = true;
			for (BlockPos t : order) {
				if (!mc.level.getBlockState(t).isAir()) continue;
				done = false;
				if (placeAt(mc, p, t)) { p.swing(InteractionHand.MAIN_HAND); cd = 2 + (rnd() < 0.4 ? 1 : 0); return; }
			}
			if (done) toggle(mc);
		}
	}

	static class InvisEsp extends Mod { InvisEsp() { super("InvisESP", "Bedwars", -1); } }

	static class MacroMod extends Mod {
		ModeSetting action;
		MacroMod(String name, int defaultAction) { super(name, "Player", -1); action = mode("Eylem", defaultAction, ACTIONS); }
		@Override void onPress(Minecraft mc) { runMacro(mc, action.index); }
		@Override void toggle(Minecraft mc) { runMacro(mc, action.index); }
	}

	static class Track {
		boolean init = false; double x, y, z; float yaw;
		int speedVl = 0, hoverTicks = 0, riseTicks = 0, snapVl = 0; long snapStart = 0, lastHurtTick = -1000;
		final Map<String, Long> lastAlert = new HashMap<>();
	}

	static class AntiCheat extends Mod {
		BoolSetting speedChk = bool("Speed kontrol", true), flyChk = bool("Fly kontrol", true), reachChk = bool("Reach kontrol", true), aimChk = bool("Aura/Aim kontrol", true), chat = bool("Sohbete yaz", true);
		FloatSetting speedLimit = num("Speed siniri", 0.75f, 0.4f, 1.5f), flyTicks = num("Fly suresi (tick)", 15f, 8f, 40f), reachLimit = num("Reach siniri", 3.9f, 3.0f, 6f), snapAngle = num("Donus siniri", 70f, 30f, 150f);
		int lastMeHurt = 0;
		AntiCheat() { super("AntiCheat", "Client", -1); }
		private void flag(LocalPlayer me, Player o, Track t, String type, String info) {
			Long last = t.lastAlert.get(type);
			if (last != null && tick - last < 100) return;
			t.lastAlert.put(type, (long) tick);
			String name = o.getName().getString(); suspects.put(name, type); suspectTick.put(name, (long) tick);
			if (chat.value) me.displayClientMessage(Component.literal("[AntiCheat] " + name + " -> " + type + " " + info), false);
		}
		@Override void onTick(Minecraft mc, LocalPlayer p) {
			boolean meHurtNow = p.hurtTime > lastMeHurt; lastMeHurt = p.hurtTime;
			for (Player o : mc.level.players()) {
				if (o == p || !o.isAlive()) continue;
				Track t = tracks.computeIfAbsent(o.getUUID(), k -> new Track());
				double nx = o.getX(), ny = o.getY(), nz = o.getZ(); float nyaw = o.getYRot();
				if (!t.init) { t.init = true; t.x = nx; t.y = ny; t.z = nz; t.yaw = nyaw; continue; }
				double dx = nx - t.x, dy = ny - t.y, dz = nz - t.z, hz = Math.sqrt(dx * dx + dz * dz);
				float dyaw = Mth.wrapDegrees(nyaw - t.yaw); t.x = nx; t.y = ny; t.z = nz; t.yaw = nyaw;
				if (hz > 8 || Math.abs(dy) > 8) { t.speedVl = 0; t.hoverTicks = 0; t.riseTicks = 0; continue; }
				if (o.hurtTime > 0) t.lastHurtTick = tick;
				boolean exempt = o.isFallFlying() || o.isPassenger() || o.isSwimming() || o.isInWater() || o.isInLava() || o.onClimbable() || o.isSpectator() || o.isCreative() || tick - t.lastHurtTick < 30;
				if (speedChk.value) { if (!exempt && hz > speedLimit.value) t.speedVl++; else if (t.speedVl > 0) t.speedVl--; if (t.speedVl >= 5) { flag(p, o, t, "Speed", String.format("%.2f b/tick", hz)); t.speedVl = 0; } }
				if (flyChk.value) {
					if (!o.onGround() && !exempt) {
						boolean openAir = mc.level.getBlockState(BlockPos.containing(nx, ny - 0.4, nz)).isAir();
						if (openAir && Math.abs(dy) < 0.02) t.hoverTicks++; else t.hoverTicks = 0;
						if (dy > 0.08) t.riseTicks++; else if (dy <= 0) t.riseTicks = 0;
						if (t.hoverTicks >= flyTicks.value) { flag(p, o, t, "Fly", "(havada duruyor)"); t.hoverTicks = 0; }
						if (t.riseTicks >= 14) { flag(p, o, t, "Fly", "(yukari ucuyor)"); t.riseTicks = 0; }
					} else { t.hoverTicks = 0; t.riseTicks = 0; }
				}
				if (aimChk.value && o.swinging && p.distanceTo(o) < 8 && Math.abs(dyaw) > snapAngle.value) {
					if (tick - t.snapStart > 60) { t.snapStart = tick; t.snapVl = 0; }
					t.snapVl++; if (t.snapVl >= 3) { flag(p, o, t, "Aura/Aim", "(ani donus)"); t.snapVl = 0; }
				}
				if (reachChk.value && meHurtNow && o.swinging) {
					AABB box = p.getBoundingBox(); Vec3 eye = o.getEyePosition();
					double d = Math.sqrt(Math.pow(eye.x - Mth.clamp(eye.x, box.minX, box.maxX), 2) + Math.pow(eye.y - Mth.clamp(eye.y, box.minY, box.maxY), 2) + Math.pow(eye.z - Mth.clamp(eye.z, box.minZ, box.maxZ), 2));
					if (d > reachLimit.value && d < 8) flag(p, o, t, "Reach", String.format("%.1f blok", d));
				}
			}
			if (tick % 200 == 0) { java.util.Set<UUID> present = new java.util.HashSet<>(); for (Player o : mc.level.players()) present.add(o.getUUID()); tracks.keySet().retainAll(present); }
			if (tick % 100 == 0) { suspectTick.entrySet().removeIf(en -> tick - en.getValue() > 1200); suspects.keySet().retainAll(suspectTick.keySet()); }
		}
		@Override void onDisable(Minecraft mc) { tracks.clear(); suspects.clear(); suspectTick.clear(); }
	}

						static class FpsBoost extends Mod {
		FloatSetting dist = num("Gorus mesafesi", 8f, 2f, 32f), entDist = num("Varlik mesafesi", 0.5f, 0.5f, 5f);
		BoolSetting fast = bool("Hizli grafik", true), noClouds = bool("Bulut kapat", true), minParticles = bool("Partikul azalt", true), noShadows = bool("Golge kapat", true), noBlend = bool("Biyom gecisi kapat", true);
		boolean saved = false, oldS; int oldDist, oldB; GraphicsStatus oldG; CloudStatus oldC; ParticleStatus oldP; double oldE;
		FpsBoost() { super("FPSBoost", "Render", -1); }
		private void apply(Minecraft mc) {
			Options o = mc.options; o.renderDistance().set((int) dist.value);
			if (fast.value) o.graphicsMode().set(GraphicsStatus.FAST); if (noClouds.value) o.cloudStatus().set(CloudStatus.OFF);
			if (minParticles.value) o.particles().set(ParticleStatus.MINIMAL); if (noShadows.value) o.entityShadows().set(false);
			if (noBlend.value) o.biomeBlendRadius().set(0); o.entityDistanceScaling().set((double) entDist.value);
		}
		@Override void onEnable(Minecraft mc) {
			Options o = mc.options; oldDist = o.renderDistance().get(); oldG = o.graphicsMode().get(); oldC = o.cloudStatus().get();
			oldP = o.particles().get(); oldS = o.entityShadows().get(); oldB = o.biomeBlendRadius().get(); oldE = o.entityDistanceScaling().get(); saved = true; apply(mc);
		}
		@Override void onTick(Minecraft mc, LocalPlayer p) { if (tick % 40 == 0) apply(mc); }
		@Override void onDisable(Minecraft mc) {
			if (!saved) return; Options o = mc.options;
			o.renderDistance().set(oldDist); o.graphicsMode().set(oldG); o.cloudStatus().set(oldC); o.particles().set(oldP);
			o.entityShadows().set(oldS); o.biomeBlendRadius().set(oldB); o.entityDistanceScaling().set(oldE); saved = false;
		}
	}

	static class ClickGuiMod extends Mod {
		ClickGuiMod() { super("ClickGUI", "Client", GLFW.GLFW_KEY_RIGHT_SHIFT); }
		@Override void onPress(Minecraft mc) { if (mc.screen == null) mc.setScreen(new ClickScreen()); }
		@Override void toggle(Minecraft mc) { mc.setScreen(new ClickScreen()); }
	}

	static class CfgMod extends Mod {
		ModeSetting slot = mode("Slot", 0, "Config 1", "Config 2", "Config 3", "Config 4", "Config 5");
		BoolSetting save = bool("Kaydet (dokun)", false), load = bool("Yukle (dokun)", false), del = bool("Sil (dokun)", false);
		CfgMod() { super("Config", "Client", -1); enabled = true; }
		@Override void toggle(Minecraft mc) {}
		@Override void onTick(Minecraft mc, LocalPlayer p) {
			int i = slot.index;
			if (save.value) { save.value = false; saveTo(slotPath(i)); p.displayClientMessage(Component.literal("Config " + (i + 1) + " kaydedildi"), true); }
			if (load.value) { load.value = false; boolean ok = loadFrom(slotPath(i), mc); p.displayClientMessage(Component.literal(ok ? "Config " + (i + 1) + " yuklendi" : "Config " + (i + 1) + " bos (kayit yok)"), true); }
			if (del.value) { del.value = false; try { Files.deleteIfExists(slotPath(i)); } catch (Exception ignored) {} p.displayClientMessage(Component.literal("Config " + (i + 1) + " silindi"), true); }
		}
	}

	static class HudMod extends Mod {
		BoolSetting watermark = bool("Isim yazisi", true), arrayList = bool("Modul listesi", true), splash = bool("Acilis yazisi", true), targetHud = bool("Hedef paneli", true);
		BoolSetting coords = bool("Koordinat", true), dir = bool("Yon", false), fps = bool("FPS", true), ping = bool("Ping", true), speed = bool("Hiz (blok/sn)", false), time = bool("Saat", false);
		BoolSetting counters = bool("Totem/Roket sayaci", true), bed = bool("Yatak bilgisi", true), cpsHud = bool("CPS sayaci", true), keys = bool("Tuslar (WASD)", false), armor = bool("Zirh durumu", false);
		HudMod() { super("HUD", "Client", -1); enabled = true; }
	}

	static class Row {
		final int type, y, h; final Mod m; final Setting s;
		Row(int type, Mod m, Setting s, int y, int h) { this.type = type; this.m = m; this.s = s; this.y = y; this.h = h; }
	}

	public static class ClickScreen extends Screen {
		static final String[] TABS = { "Combat", "Elytra", "Move", "Render", "Player", "Bedwars", "Client" };
		static String tab = "Combat"; static double scroll = 0;
		Mod listening = null; FloatSetting dragSetting = null; double downX = 0, downY = 0; boolean moved = false, ignoreRelease = false;
		int pw = 0, px = 0, top = 38, bottom = 100, contentH = 0;
		public ClickScreen() { super(Component.literal("KediClient")); }
		@Override public boolean isPauseScreen() { return false; }
		private String fit(String t, int w) { return this.font.width(t) <= w - 2 ? t : t.substring(0, Math.min(3, t.length())); }
		private void calc() { pw = Math.min(310, this.width - 8); px = (this.width - pw) / 2; top = 38; bottom = this.height - 6; }

		private List<Row> buildRows() {
			List<Row> list = new ArrayList<>(); int y = 0;
			for (Mod m : MODS) {
				if (!m.category.equals(tab)) continue;
				list.add(new Row(0, m, null, y, 16)); y += 16;
				if (m.expanded) {
					for (Setting s : m.settings) { list.add(new Row(s instanceof FloatSetting ? 2 : (s instanceof ModeSetting ? 4 : 1), m, s, y, 14)); y += 14; }
					list.add(new Row(3, m, null, y, 14)); y += 16;
				}
			}
			contentH = y; return list;
		}

		private void clampScroll() { double maxScroll = Math.max(0, contentH - (bottom - top)); if (scroll > maxScroll) scroll = maxScroll; if (scroll < 0) scroll = 0; }

		@Override public void render(GuiGraphics g, int mx, int my, float pt) {
			calc(); g.fill(0, 0, this.width, this.height, 0x99000000);
			g.fill(px, 4, px + pw, 20, 0xF0140C28); g.fill(px, 4, px + 2, 20, ACCENT);
			g.drawString(this.font, "KediClient", px + 8, 8, ACCENT); g.drawString(this.font, "x", px + pw - 10, 8, 0xFFFFFFFF);
			int tw = pw / TABS.length;
			for (int i = 0; i < TABS.length; i++) {
				int tx = px + i * tw; boolean sel = TABS[i].equals(tab);
				g.fill(tx, 22, tx + tw - 1, 35, sel ? 0xFF3A2670 : 0xE0181028);
				g.drawCenteredString(this.font, fit(TABS[i], tw), tx + tw / 2, 25, sel ? 0xFFFFFFFF : 0xFFAAAAAA);
			}
			g.fill(px, top, px + pw, bottom, 0xD00C0816);
			List<Row> rows = buildRows(); clampScroll();
			g.enableScissor(px, top, px + pw, bottom);
			for (Row r : rows) { int ry = top + r.y - (int) scroll; if (ry + r.h < top || ry > bottom) continue; drawRow(g, r, ry); }
			g.disableScissor();
		}

		private void drawRow(GuiGraphics g, Row r, int ry) {
			if (r.type == 0) {
				g.fill(px + 2, ry, px + pw - 2, ry + 15, r.m.enabled ? 0xFF2D1F55 : 0xFF14102A);
				if (r.m.enabled) g.fill(px + 2, ry, px + 4, ry + 15, ACCENT);
				g.drawString(this.font, r.m.name, px + 8, ry + 4, r.m.enabled ? 0xFFFFFFFF : 0xFFBBBBBB);
				String b = r.m.key < 0 ? "" : "[" + keyName(r.m.key) + "]";
				g.drawString(this.font, b, px + pw - 22 - this.font.width(b), ry + 4, 0xFF8888AA);
				g.drawString(this.font, r.m.expanded ? "-" : "+", px + pw - 12, ry + 4, 0xFFFFFFFF);
			} else if (r.type == 1) {
				BoolSetting b = (BoolSetting) r.s; g.fill(px + 6, ry, px + pw - 6, ry + 13, 0xFF100C20);
				g.drawString(this.font, b.name, px + 10, ry + 3, 0xFFCCCCCC);
				String t = b.value ? "ON" : "OFF"; g.drawString(this.font, t, px + pw - 12 - this.font.width(t), ry + 3, b.value ? 0xFF7CFFB0 : 0xFFFF7C7C);
			} else if (r.type == 2) {
				FloatSetting f = (FloatSetting) r.s; int x0 = px + 6, x1 = px + pw - 6;
				g.fill(x0, ry, x1, ry + 13, 0xFF100C20);
				g.fill(x0, ry, x0 + (int) ((x1 - x0) * ((f.value - f.min) / (f.max - f.min))), ry + 13, 0xFF4B2F9E);
				g.drawString(this.font, f.name + ": " + String.format("%.2f", f.value), x0 + 4, ry + 3, 0xFFFFFFFF);
			} else if (r.type == 4) {
				ModeSetting ms = (ModeSetting) r.s; g.fill(px + 6, ry, px + pw - 6, ry + 13, 0xFF100C20);
				g.drawString(this.font, ms.name, px + 10, ry + 3, 0xFFCCCCCC);
				String cur = ms.options[ms.index]; g.drawString(this.font, cur, px + pw - 12 - this.font.width(cur), ry + 3, ACCENT);
			} else {
				g.fill(px + 6, ry, px + pw - 6, ry + 13, 0xFF100C20);
				g.drawString(this.font, listening == r.m ? "Bind: > tusa veya orta tiga bas <" : "Bind: " + keyName(r.m.key), px + 10, ry + 3, 0xFFFFD27C);
			}
		}

																																								 		private void setSlider(FloatSetting fs, double mx) {
			double x0 = px + 6, x1 = px + pw - 6;
			fs.value = Math.round((float) (fs.min + Math.max(0.0, Math.min(1.0, (mx - x0) / (x1 - x0))) * (fs.max - fs.min)) * 100f) / 100f;
		}

		@Override public boolean mouseClicked(double mx, double my, int button) {
			calc(); downX = mx; downY = my; moved = false; ignoreRelease = false; dragSetting = null;
			if (listening != null) { if (button >= 2) listening.key = 1000 + button; listening = null; ignoreRelease = true; return true; }
			if (my >= 4 && my < 20 && mx >= px + pw - 18 && mx < px + pw) { ignoreRelease = true; onClose(); return true; }
			if (my >= 22 && my < 35 && mx >= px && mx < px + pw) {
				int i = (int) ((mx - px) / (pw / TABS.length));
				if (i >= 0 && i < TABS.length) { tab = TABS[i]; scroll = 0; }
				ignoreRelease = true; return true;
			}
			if (my >= top && my < bottom && mx >= px && mx < px + pw) {
				for (Row r : buildRows()) {
					int ry = top + r.y - (int) scroll;
					if (r.type == 2 && my >= ry && my < ry + r.h) { dragSetting = (FloatSetting) r.s; setSlider(dragSetting, mx); ignoreRelease = true; return true; }
				}
			}
			return true;
		}

		@Override public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
			if (dragSetting != null) { setSlider(dragSetting, mx); return true; }
			if (Math.abs(mx - downX) > 3 || Math.abs(my - downY) > 3) moved = true;
			if (moved) { scroll -= dy; clampScroll(); }
			return true;
		}

		@Override public boolean mouseReleased(double mx, double my, int button) {
			if (dragSetting != null) { dragSetting = null; saveConfig(); return true; }
			if (ignoreRelease || moved) { ignoreRelease = false; return true; }
			if (my >= top && my < bottom && mx >= px && mx < px + pw) {
				for (Row r : buildRows()) {
					int ry = top + r.y - (int) scroll;
					if (my >= ry && my < ry + r.h) {
						if (r.type == 0) { if (button == 1 || mx >= px + pw - 24) r.m.expanded = !r.m.expanded; else r.m.toggle(this.minecraft); }
						else if (r.type == 1) { BoolSetting b = (BoolSetting) r.s; b.value = !b.value; }
						else if (r.type == 4) { ModeSetting ms = (ModeSetting) r.s; ms.index = (ms.index + 1) % ms.options.length; }
						else if (r.type == 3) listening = r.m;
						break;
					}
				}
			}
			return true;
		}

		public boolean mouseScrolled(double mx, double my, double sx, double sy) { scroll -= sy * 14; clampScroll(); return true; }

		@Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
			if (listening != null) {
				if (keyCode == GLFW.GLFW_KEY_ESCAPE) listening = null;
				else if (keyCode == GLFW.GLFW_KEY_DELETE || keyCode == GLFW.GLFW_KEY_BACKSPACE) { listening.key = -1; listening = null; }
				else { listening.key = keyCode; listening = null; }
				return true;
			}
			return super.keyPressed(keyCode, scanCode, modifiers);
		}

		@Override public void onClose() { saveConfig(); super.onClose(); }
	}
}
