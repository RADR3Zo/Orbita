package dev.orbita.client.hud;

import dev.orbita.client.ui.theme.OrbitaTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Un módulo del HUD de Órbita (al estilo Lunar: escala, fondo, corchetes, sombra y color por módulo).
 * Se dibuja en coordenadas locales (0,0); HudManager aplica posición y escala.
 * Las coordenadas negativas significan "anclado al borde derecho/inferior" hasta que se arrastra.
 */
public abstract class HudModule {
    public enum Category {
        HUD("HUD"), MECHANIC("Mecánicas");

        public final String label;

        Category(String label) {
            this.label = label;
        }
    }

    /** Colores de texto disponibles; el último es arcoíris animado (chroma). */
    public static final int[] COLORS = {0xFFFFFFFF, 0xFFB3A8FF, 0xFF7FD3FF, 0xFF7CFFB2, 0xFFFFD27C, 0xFFFF8FD8, 0};
    public static final String[] COLOR_NAMES = {"Blanco", "Violeta", "Cian", "Verde", "Dorado", "Rosa", "Arcoíris"};

    private final String id;
    private final String name;
    private final String description;
    private final String glyph;
    private final Category category;
    private boolean enabled;
    protected int x;
    protected int y;
    private float scale = 1f;
    private boolean background = true;
    private boolean brackets = false;
    private boolean shadow = true;
    private int colorIndex = 0;
    private boolean showIcon = true;
    private Item iconItem = Items.PAPER;
    private ItemStack iconStack;

    protected int width = 40;
    protected int height = 18;
    private int lastX;
    private int lastY;

    protected HudModule(String id, String name, String description, String glyph, Category category,
                        boolean enabledByDefault, int x, int y) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.glyph = glyph;
        this.category = category;
        this.enabled = enabledByDefault;
        this.x = x;
        this.y = y;
    }

    /** Dibuja el módulo en (0,0) y actualiza width/height (sin escala). */
    public abstract void render(GuiGraphics g, Minecraft mc);

    /** false cuando no hay nada que mostrar (p. ej. sin efectos de poción activos). */
    public boolean hasContent(Minecraft mc) {
        return true;
    }

    /** true si el módulo dibuja algo en el HUD (los módulos de mecánica pueden no hacerlo). */
    public boolean hasHud() {
        return true;
    }

    /** true si el módulo dibuja a pantalla completa (sin posición ni escala del editor), p. ej. letras en 3D. */
    public boolean freeform() {
        return false;
    }

    /** Dibujo a pantalla completa para los módulos freeform. */
    public void renderFreeform(GuiGraphics g, Minecraft mc, int width, int height) {
    }

    /** true si admite las opciones de estilo de texto (fondo, corchetes, color...). */
    public boolean hasTextStyle() {
        return true;
    }

    /** Lo que se ve en el editor de HUD cuando el módulo no tiene contenido. */
    public void renderPlaceholder(GuiGraphics g, Minecraft mc) {
        width = Math.max(56, mc.font.width(name) + 10);
        height = 18;
        g.fill(0, 0, width, height, 0x70000000);
        g.drawString(mc.font, name, (width - mc.font.width(name)) / 2, 5, OrbitaTheme.TEXT_MUTED, false);
    }

    /** Se llama al activar o desactivar el módulo. */
    protected void onToggle(boolean enabled) {
    }

    public int textColor() {
        int c = COLORS[Mth.clamp(colorIndex, 0, COLORS.length - 1)];
        if (c != 0) {
            return c;
        }
        float hue = (System.currentTimeMillis() % 4000L) / 4000f + lastX * 0.002f;
        return 0xFF000000 | Mth.hsvToRgb(hue % 1f, 0.55f, 1f);
    }

    public int screenX(int guiWidth) {
        int w = scaledWidth();
        int ex = x < 0 ? guiWidth - w - 4 : x;
        return Mth.clamp(ex, 0, Math.max(0, guiWidth - w));
    }

    public int screenY(int guiHeight) {
        int h = scaledHeight();
        int ey = y < 0 ? guiHeight - h - 4 : y;
        return Mth.clamp(ey, 0, Math.max(0, guiHeight - h));
    }

    public int scaledWidth() {
        return Math.round(width * scale);
    }

    public int scaledHeight() {
        return Math.round(height * scale);
    }

    void setLastPosition(int x, int y) {
        this.lastX = x;
        this.lastY = y;
    }

    public boolean contains(double mx, double my) {
        return mx >= lastX && mx < lastX + scaledWidth() && my >= lastY && my < lastY + scaledHeight();
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public String glyph() {
        return glyph;
    }

    public Category category() {
        return category;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        boolean changed = this.enabled != enabled;
        this.enabled = enabled;
        if (changed) {
            onToggle(enabled);
        }
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int lastX() {
        return lastX;
    }

    public int lastY() {
        return lastY;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public float scale() {
        return scale;
    }

    public void setScale(float scale) {
        this.scale = Mth.clamp(Math.round(scale * 20f) / 20f, 0.5f, 2.5f);
    }

    public boolean background() {
        return background;
    }

    public void setBackground(boolean background) {
        this.background = background;
    }

    public boolean brackets() {
        return brackets;
    }

    public void setBrackets(boolean brackets) {
        this.brackets = brackets;
    }

    public boolean shadow() {
        return shadow;
    }

    public void setShadow(boolean shadow) {
        this.shadow = shadow;
    }

    /** Objeto de Minecraft que representa al módulo (tarjeta del menú e icono opcional en el HUD). */
    public HudModule icon(Item item) {
        this.iconItem = item;
        this.iconStack = null;
        return this;
    }

    public ItemStack iconStack() {
        if (iconStack == null) {
            iconStack = new ItemStack(iconItem);
        }
        return iconStack;
    }

    public boolean showIcon() {
        return showIcon;
    }

    public void setShowIcon(boolean showIcon) {
        this.showIcon = showIcon;
    }

    public int colorIndex() {
        return colorIndex;
    }

    public void setColorIndex(int colorIndex) {
        this.colorIndex = Math.floorMod(colorIndex, COLORS.length);
    }
}
