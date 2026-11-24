package com.example.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ExampleModClient implements ClientModInitializer {

    static boolean fullbright = true;
    static boolean autoSprint = false;
    static boolean esp = true;
    static boolean tracers = true;
    static boolean nametags = true;
    static boolean targetHUD = true;
    static boolean hitParticles = true;
    static boolean keystrokes = true;
    static boolean cpsCounter = true;
    static boolean fpsCounter = true;
    static boolean armorHUD = true;
    static boolean potionHUD = true;
    static boolean zoom = false;
    static boolean coordinates = true;
    static boolean toggleSneak = false;
    static boolean inventoryHUD = true;
    static boolean hitbox = false;

    static float espRange = 64f;
    static float zoomLevel = 3f;

    static KeyMapping menuKey;
    static KeyMapping fullbrightKey;
    static KeyMapping sprintKey;
    static KeyMapping zoomKey;

    @Override
    public void onInitializeClient() {

        menuKey = reg("ClickGUI", GLFW.GLFW_KEY_RIGHT_SHIFT);
        fullbrightKey = reg("Fullbright", GLFW.GLFW_KEY_G);
        sprintKey = reg("AutoSprint", GLFW.GLFW_KEY_H);
        zoomKey = reg("Zoom", GLFW.GLFW_KEY_Z);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {

            LocalPlayer p = client.player;
            if (p == null || client.level == null) return;

            while (menuKey.consumeClick()) {
                client.setScreen(new ConfigScreen());
            }

            while (fullbrightKey.consumeClick()) {
                fullbright = !fullbright;
                if (!fullbright) p.removeEffect(MobEffects.NIGHT_VISION);
            }

            while (sprintKey.consumeClick()) {
                autoSprint = !autoSprint;
            }

            while (zoomKey.consumeClick()) {
                zoom = !zoom;
            }

            if (fullbright) {
                p.addEffect(new MobEffectInstance(
                    MobEffects.NIGHT_VISION, 1000, 0, false, false, false
                ));
            }

            if (autoSprint && client.options.keyUp.isDown()
                    && !p.isShiftKeyDown() && !p.horizontalCollision) {
                p.setSprinting(true);
            }
        });
    }

    private static KeyMapping reg(String name, int key) {
        return KeyBindingHelper.registerKeyBinding(
            new KeyMapping(name, InputConstants.Type.KEYSYM,
                key, "KediClient")
        );
    }

    private static Component label(String name, boolean on) {
        return Component.literal(name + ": " + (on ? "ON" : "OFF"));
    }

    private static Button toggle(
            int x, int y, String name,
            Supplier<Boolean> get,
            Consumer<Boolean> set) {

        return Button.builder(label(name, get.get()), b -> {
            set.accept(!get.get());
            b.setMessage(label(name, get.get()));
        }).bounds(x, y, 200, 20).build();
    }

    public static class ConfigScreen extends Screen {

        public ConfigScreen() {
            super(Component.literal("KediClient BoxPvP"));
        }

        @Override
        protected void init() {

            int x = width / 2 - 100;
            int y = 10;
            int s = 23;

            addRenderableWidget(toggle(x,y,"Fullbright",
                () -> fullbright,v -> fullbright=v));

            addRenderableWidget(toggle(x,y+s,"AutoSprint",
                () -> autoSprint,v -> autoSprint=v));

            addRenderableWidget(toggle(x,y+s*2,"ESP",
                () -> esp,v -> esp=v));

            addRenderableWidget(toggle(x,y+s*3,"Tracers",
                () -> tracers,v -> tracers=v));

            addRenderableWidget(toggle(x,y+s*4,"Nametags",
                () -> nametags,v -> nametags=v));

            addRenderableWidget(toggle(x,y+s*5,"TargetHUD",
                () -> targetHUD,v -> targetHUD=v));

            addRenderableWidget(toggle(x,y+s*6,"HitParticles",
                () -> hitParticles,v -> hitParticles=v));

            addRenderableWidget(toggle(x,y+s*7,"Keystrokes",
                () -> keystrokes,v -> keystrokes=v));

            addRenderableWidget(toggle(x,y+s*8,"CPS Counter",
                () -> cpsCounter,v -> cpsCounter=v));

            addRenderableWidget(toggle(x,y+s*9,"FPS Counter",
                () -> fpsCounter,v -> fpsCounter=v));

            addRenderableWidget(toggle(x,y+s*10,"Armor HUD",
                () -> armorHUD,v -> armorHUD=v));

            addRenderableWidget(toggle(x,y+s*11,"Potion HUD",
                () -> potionHUD,v -> potionHUD=v));

            addRenderableWidget(toggle(x,y+s*12,"Coordinates",
                () -> coordinates,v -> coordinates=v));

            addRenderableWidget(toggle(x,y+s*13,"Inventory HUD",
                () -> inventoryHUD,v -> inventoryHUD=v));

            addRenderableWidget(toggle(x,y+s*14,"Hitbox",
                () -> hitbox,v -> hitbox=v));

            addRenderableWidget(toggle(x,y+s*15,"Zoom",
                () -> zoom,v -> zoom=v));

            addRenderableWidget(Button.builder(
                Component.literal("Close"), b -> onClose()
            ).bounds(x,y+s*16,200,20).build());
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }
    }
}
