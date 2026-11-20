package dev.orbita.client.nova.gui;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Base de las pantallas de Nova. Minecraft cambia como llegan clics y teclas entre versiones;
 * las pantallas usan onClick/onDrag/onRelease/onKey/onChar y solo este archivo cambia por version.
 */
public abstract class NovaScreen extends Screen {
    protected NovaScreen(Component title) { super(title); }

    protected boolean onClick(double mx, double my, int button) { return superClick(mx, my, button); }
    protected boolean onDrag(double mx, double my, int button, double dx, double dy) { return superDrag(mx, my, button, dx, dy); }
    protected boolean onRelease(double mx, double my, int button) { return superRelease(mx, my, button); }
    protected boolean onKey(int key, int scan, int mods) { return superKey(key, scan, mods); }
    protected boolean onChar(char chr, int mods) { return superChar(chr, mods); }

    private static net.minecraft.client.input.MouseButtonEvent click(double mx, double my, int b) { return new net.minecraft.client.input.MouseButtonEvent(mx, my, new net.minecraft.client.input.MouseButtonInfo(b, 0)); }

    protected final boolean superClick(double mx, double my, int button) { return super.mouseClicked(click(mx, my, button), false); }
    protected final boolean superDrag(double mx, double my, int button, double dx, double dy) { return super.mouseDragged(click(mx, my, button), dx, dy); }
    protected final boolean superRelease(double mx, double my, int button) { return super.mouseReleased(click(mx, my, button)); }
    protected final boolean superKey(int key, int scan, int mods) { return super.keyPressed(new net.minecraft.client.input.KeyEvent(key, scan, mods)); }
    protected final boolean superChar(char chr, int mods) { return super.charTyped(new net.minecraft.client.input.CharacterEvent(chr, mods)); }

    @Override public final boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent c, boolean doubled) { return onClick(c.x(), c.y(), c.button()); }
    @Override public final boolean mouseDragged(net.minecraft.client.input.MouseButtonEvent c, double dx, double dy) { return onDrag(c.x(), c.y(), c.button(), dx, dy); }
    @Override public final boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent c) { return onRelease(c.x(), c.y(), c.button()); }
    @Override public final boolean keyPressed(net.minecraft.client.input.KeyEvent k) { return onKey(k.key(), k.scancode(), k.modifiers()); }
    @Override public final boolean charTyped(net.minecraft.client.input.CharacterEvent ch) { return Character.isValidCodePoint(ch.codepoint()) && ch.codepoint() < 0x10000 ? onChar((char) ch.codepoint(), ch.modifiers()) : false; }
}
