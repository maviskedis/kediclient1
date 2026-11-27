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
		float dy = Mth.wrapDegrees(rot[0] - p.getYRot());
		float dp = Mth.clamp(rot[1] - p.getXRot(), -90f, 90f);
		double dist = Math.sqrt(dy * dy + dp * dp);

		float ease = (float) Mth.clamp(dist / 36.0, 0.25, 1.0);
		float jitterY = (float) ((rnd() - 0.5) * 1.15);
		float jitterP = (float) ((rnd() - 0.5) * 0.75);
		float step = Math.max(1.2f, maxStep * ease);

		float nextYaw = p.getYRot() + Mth.clamp(dy, -step, step) + jitterY;
		float nextPitch = Mth.clamp(p.getXRot() + Mth.clamp(dp, -step, step) + jitterP, -90f, 90f);

		p.setYRot(gcdYaw(p, nextYaw));
		p.setXRot(gcdPitch(p, nextPitch));
	}

	static void applySilentRot(LocalPlayer p, float[] rot, int holdTicks) {
		float jitterY = (float) ((rnd() - 0.5) * 1.2);
		float jitterP = (float) ((rnd() - 0.5) * 0.8);
		float sy = gcdYaw(p, rot[0] + jitterY);
		float sp = gcdPitch(p, rot[1] + jitterP);
		silentActive = true;
		silentYaw = sy;
		silentPitch = sp;
		silentHoldTicks = Math.max(1, holdTicks);
		p.connection.send(new ServerboundMovePlayerPacket.Rot(sy, sp, p.onGround()));
	}

	static boolean isBot(Minecraft mc, Player o) {
		return mc.getConnection() != null && mc.getConnection().getPlayerInfo(o.getUUID()) == null;
	}

	static boolean swapChest(Minecraft mc, LocalPlayer p, boolean toElytra, boolean hotbarMode) {
		if (hotbarMode) {
			for (int i = 0; i < 9; i++) {
				ItemStack s = p.getInventory().getItem(i);
				boolean match = toElytra ? s.is(Items.ELYTRA)
					: (s.getItem() instanceof ArmorItem a && a.getEquipmentSlot() == EquipmentSlot.CHEST);
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
			boolean match = toElytra ? s.is(Items.ELYTRA)
				: (s.getItem() instanceof ArmorItem a && a.getEquipmentSlot() == EquipmentSlot.CHEST);
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

	static boolean isWeapon(ItemStack st) {
		return st.is(ItemTags.SWORDS) || st.is(ItemTags.AXES);
	}

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
		mc.gameMode.handleInventoryMouseClick(p.inventoryMenu.containerId, slot, 0, ClickType.PICKUP, p);
	}

	static int accent(int i) {
		double t = (Math.sin(System.currentTimeMillis() / 500.0 + i * 0.5) + 1.0) / 2.0;
		int r = (int) (0xB3 + (0x4F - 0xB3) * t);
		int gr = (int) (0x88 + (0xC3 - 0x88) * t);
		int b = (int) (0xFF + (0xF7 - 0xFF) * t);
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
		Criticalss() {}
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
		FloatSetting minHeight = num("Min yukseklik", 4f
		
