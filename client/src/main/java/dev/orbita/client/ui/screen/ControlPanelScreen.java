package dev.orbita.client.ui.screen;

import dev.orbita.client.OrbitaClient;
import dev.orbita.client.config.OrbitaConfig;
import dev.orbita.client.hud.HudManager;
import dev.orbita.client.hud.HudModule;
import dev.orbita.client.hud.TextHudModule;
import dev.orbita.client.hud.modules.SpotifyModule;
import dev.orbita.client.hud.modules.ToggleSprintModule;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.input.KeyEvent;
import dev.orbita.client.ui.anim.Animation;
import dev.orbita.client.ui.render.Starfield;
import dev.orbita.client.ui.render.UiDraw;
import dev.orbita.client.ui.theme.OrbitaFonts;
import dev.orbita.client.ui.theme.OrbitaTheme;
import dev.orbita.client.ui.widget.OrbitaButton;
import dev.orbita.client.ui.widget.ToggleSwitch;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import dev.orbita.client.ui.screen.PanelEntries.PanelCard;
import dev.orbita.client.ui.screen.PanelEntries.PanelFilter;
import dev.orbita.client.ui.screen.PanelEntries.ModuleEntry;
import dev.orbita.client.ui.screen.PanelEntries.NovaEntry;

import static dev.orbita.client.ui.screen.PanelEntries.FREELOOK;
import static dev.orbita.client.ui.screen.PanelEntries.PROFILE;
import static dev.orbita.client.ui.screen.PanelEntries.ZOOM;
import static dev.orbita.client.ui.screen.PanelEntries.saveNova;

/**
 * Menú de mods de Órbita al estilo Lunar: buscador, categorías y una cuadrícula de tarjetas
 * con interruptor ACTIVADO/DESACTIVADO. Al pulsar una tarjeta se abren sus opciones.
 */
public class ControlPanelScreen extends Screen {
    private static PanelFilter lastFilter = PanelFilter.ALL;
    /** Solo pruebas: abre directamente las opciones de este módulo de Nova. */
    @Nullable
    public static dev.orbita.client.nova.module.Module optionsForNova;

    private final Screen parent;
    private final Animation open = new Animation(0f, 9f);
    private final List<PanelCard> all = new ArrayList<>();
    private final List<PanelCard> visible = new ArrayList<>();
    private final List<int[]> rowBoxes = new ArrayList<>();
    private PanelFilter filter = lastFilter;
    private String search = "";
    private int scrollRow;
    @Nullable
    private PanelCard options;
    private int px, py, pw, ph, gx, gy, gw, gh, cols, rows, cardW;
    private int cardH = 82;
    private float scrollFrom;
    private long scrollAt;
    private static final float SCROLL_MS = 220f;

    /** Desplazamiento vertical actual de la animación de cambio de página (0 cuando terminó). */
    private float scrollShift() {
        float k = (net.minecraft.util.Util.getMillis() - scrollAt) / SCROLL_MS;
        return k >= 1f ? 0f : scrollFrom * (1f - Animation.easeOutCubic(k));
    }
    private static final int GAP = 6;

    public ControlPanelScreen(@Nullable Screen parent) {
        this(parent, null);
    }

    /** Abre directamente las opciones de un módulo (clic derecho en el editor de HUD). */
    public ControlPanelScreen(@Nullable Screen parent, @Nullable HudModule optionsFor) {
        super(Component.literal("Mods de Órbita"));
        this.parent = parent;
        open.setTarget(1f);
        for (HudModule m : HudManager.get().modules()) {
            ModuleEntry e = new ModuleEntry(m);
            all.add(e);
            if (m == optionsFor) {
                options = e;
            }
        }
        all.add(ZOOM);
        all.add(FREELOOK);
        if (dev.orbita.client.nova.NovaClient.modules != null) {
            for (dev.orbita.client.nova.module.Module m : dev.orbita.client.nova.NovaClient.modules.all) {
                NovaEntry e = NovaEntry.of(m);
                all.add(e);
                if (optionsForNova == m) {
                    options = e;
                }
            }
        }
        optionsForNova = null;
        all.add(PROFILE);
        refilter();
    }

    private void refilter() {
        visible.clear();
        String q = search.toLowerCase(Locale.ROOT).trim();
        for (PanelCard e : all) {
            if (filter != PanelFilter.ALL && e.filter() != filter) {
                continue;
            }
            if (!q.isEmpty() && !e.name().toLowerCase(Locale.ROOT).contains(q)
                    && !e.description().toLowerCase(Locale.ROOT).contains(q)) {
                continue;
            }
            visible.add(e);
        }
        scrollRow = 0;
    }

    void showOptions(@Nullable PanelCard entry) {
        options = entry;
        optionScroll = 0;
        rebuildWidgets();
    }

    @Override
    protected void init() {
        rowBoxes.clear();
        pw = Math.min(560, width - 16);
        ph = Math.min(330, height - 16);
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        gx = px + 10;
        gy = py + 58;
        gw = pw - 20;
        gh = ph - 66;

        addRenderableWidget(new OrbitaButton(px + pw - 22, py + 7, 16, 16, Component.literal("×"), b -> onClose())
                .style(OrbitaButton.Style.GHOST).noIntro());

        if (options != null) {
            initOptions();
            optionRowsTotal = optionRow;
            return;
        }

        EditBox box = new EditBox(font, px + pw - 196, py + 9, 120, 12, Component.literal("Buscar"));
        box.setBordered(false);
        box.setHint(Component.literal("Buscar mods…").withColor(0xFF5F6478));
        box.setValue(search);
        box.setResponder(s -> {
            search = s;
            refilter();
        });
        addRenderableWidget(box);

        OrbitaButton edit = new OrbitaButton(px + pw - 70, py + 30, 60, 18, OrbitaFonts.menu("Editar HUD"),
                b -> minecraft.setScreen(new HudEditorScreen(this)));
        edit.active = minecraft.level != null;
        addRenderableWidget(edit.noIntro());

        PanelFilter[] filters = PanelFilter.values();
        // Ancho de cada pestaña según su texto (ahora son 7 y deben caber junto a "Editar HUD").
        int[] tabW = new int[filters.length];
        int totalW = 0;
        for (int i = 0; i < filters.length; i++) {
            tabW[i] = font.width(OrbitaFonts.menu(filters[i].label)) + 18;
            totalW += tabW[i] + 3;
        }
        float squeeze = Math.min(1f, (pw - 20 - 66f) / totalW);
        int tabX = px + 10;
        for (int i = 0; i < filters.length; i++) {
            PanelFilter f = filters[i];
            int w = Math.round(tabW[i] * squeeze);
            int x = tabX;
            tabX += w + Math.round(3 * squeeze);
            addRenderableWidget(new OrbitaButton(x, py + 30, w, 18, OrbitaFonts.menu(f.label), b -> {
                filter = f;
                lastFilter = f;
                refilter();
                rebuildWidgets();
            }).style(OrbitaButton.Style.TAB).selected(f == filter).noIntro());
        }

        cols = Math.max(2, (gw + GAP) / (88 + GAP));
        cardW = (gw - (cols - 1) * GAP) / cols;
        cardH = gh >= 2 * 82 + GAP ? 82 : Math.max(60, (gh - GAP) / 2);
        rows = Math.max(1, (gh + GAP) / (cardH + GAP));
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                addRenderableWidget(new Card(gx + c * (cardW + GAP), gy + r * (cardH + GAP), cardW, cardH, r * cols + c));
            }
        }
    }

    // --------------------------------------------------------------- opciones de una tarjeta

    private int optionRow;

    private void initOptions() {
        optionRow = 0;
        addRenderableWidget(new OrbitaButton(px + 10, py + 30, 60, 18, OrbitaFonts.menu("‹ Volver"), b -> showOptions(null))
                .style(OrbitaButton.Style.GHOST).noIntro());
        if (options instanceof NovaEntry ne) {
            novaOptions(ne);
            return;
        }
        if (options instanceof ModuleEntry me) {
            HudModule m = me.module();
            toggleRow("Activado", "Muestra u oculta el módulo", m::isEnabled, v -> m.setEnabled(v));
            if (m == HudManager.get().modules().stream().filter(x -> x instanceof ToggleSprintModule).findFirst().orElse(null)) {
                toggleRow("Agacharse alternado", "También alterna la tecla de agacharse",
                        () -> OrbitaConfig.get().toggleSneak, v -> {
                            OrbitaConfig.get().toggleSneak = v;
                            OrbitaConfig.save();
                            ToggleSprintModule.requestApply();
                        });
            }
            toggleRow("Fondo", "Caja translúcida detrás del texto", m::background, m::setBackground);
            if (m instanceof dev.orbita.client.hud.modules.LyricsModule) {
                OrbitaConfig cfg = OrbitaConfig.get();
                String[] whereIds = {"SCATTER", "SKY", "FRONT", "HUD"};
                String[] whereNames = {"Dispersas", "En el cielo", "Delante de ti", "En una caja"};
                cycleRow("Dónde", "Dispersas: cada palabra aparece donde miras · Cielo: líneas fijas",
                        () -> whereNames[Math.max(0, java.util.Arrays.asList(whereIds).indexOf(cfg.lyricsWhere))],
                        () -> cfg.lyricsWhere = whereIds[(java.util.Arrays.asList(whereIds).indexOf(cfg.lyricsWhere) + 1) % whereIds.length]);
                toggleRow("Palabra a palabra", "Cada palabra aparece cuando se canta",
                        () -> cfg.lyricsWordByWord, v -> cfg.lyricsWordByWord = v);
                toggleRow("Sonando ahora", "Título y artista flotando al empezar la canción",
                        () -> cfg.lyricsNowPlaying, v -> cfg.lyricsNowPlaying = v);
                cycleRow("Estilo", "Colores y contorno de las letras",
                        () -> dev.orbita.client.hud.lyrics.LyricsStyle.byName(cfg.lyricsStyle).displayName,
                        () -> {
                            var all = dev.orbita.client.hud.lyrics.LyricsStyle.values();
                            cfg.lyricsStyle = all[(dev.orbita.client.hud.lyrics.LyricsStyle.byName(cfg.lyricsStyle).ordinal() + 1) % all.length].name();
                        });
                cycleRow("Fuente", "Tipografía de las letras",
                        () -> dev.orbita.client.hud.lyrics.LyricsStyle.LyricsFont.byName(cfg.lyricsFont).displayName,
                        () -> {
                            var all = dev.orbita.client.hud.lyrics.LyricsStyle.LyricsFont.values();
                            cfg.lyricsFont = all[(dev.orbita.client.hud.lyrics.LyricsStyle.LyricsFont.byName(cfg.lyricsFont).ordinal() + 1) % all.length].name();
                        });
                stepperRow("Tamaño", () -> String.format(Locale.ROOT, "%.1fx", cfg.lyricsSize),
                        d -> cfg.lyricsSize = Mth.clamp(cfg.lyricsSize + d * 0.2f, 0.8f, 4f));
                toggleRow("Línea siguiente", "Solo en la caja: la próxima línea, atenuada",
                        () -> cfg.lyricsShowNext, v -> cfg.lyricsShowNext = v);
                stepperRow("Desfase", () -> String.format(Locale.ROOT, "%+.2f s", cfg.lyricsOffsetMs / 1000f),
                        d -> cfg.lyricsOffsetMs = Mth.clamp(cfg.lyricsOffsetMs + d * 250, -10_000, 10_000));
                return;
            }
            if (m instanceof SpotifyModule) {
                OrbitaConfig cfg = OrbitaConfig.get();
                toggleRow("Portadas", "Busca la portada en iTunes/Deezer por el nombre",
                        () -> cfg.spotifyCovers, v -> cfg.spotifyCovers = v);
                toggleRow("Estilo píldora", "Barra compacta con la letra que suena (palabra actual en verde)",
                        () -> cfg.spotifyPill, v -> cfg.spotifyPill = v);
                keyRow("Teclas", "Anterior · Pausa · Siguiente",
                        new String[]{"⏮ ", "⏯ ", "⏭ "},
                        OrbitaClient.spotifyPrevKey(), OrbitaClient.spotifyPlayKey(), OrbitaClient.spotifyNextKey());
                stepperRow("Escala", () -> String.format(Locale.ROOT, "%.2fx", m.scale()),
                        d -> m.setScale(m.scale() + d * 0.05f));
                return;
            }
            if (m instanceof TextHudModule) {
                toggleRow("Corchetes", "Muestra el texto como [144 FPS]", m::brackets, m::setBrackets);
            }
            if (m instanceof TextHudModule) {
                toggleRow("Icono", "Muestra el icono del módulo junto al texto", m::showIcon, m::setShowIcon);
            }
            toggleRow("Sombra del texto", "Sombra para leer mejor sobre fondos claros", m::shadow, m::setShadow);
            int y = nextRowY();
            if (inView(y)) {
            rowBoxes.add(new int[]{gx, y, gw, 24, -1});
            addRenderableWidget(new OrbitaButton(gx + gw - 92, y + 4, 86, 16,
                    Component.literal(HudModule.COLOR_NAMES[m.colorIndex()]), b -> {
                m.setColorIndex(m.colorIndex() + 1);
                HudManager.get().save();
                b.setMessage(Component.literal(HudModule.COLOR_NAMES[m.colorIndex()]));
            }).noIntro());
            }
            stepperRow("Escala", () -> String.format(Locale.ROOT, "%.2fx", m.scale()),
                    d -> m.setScale(m.scale() + d * 0.05f));
        } else if (options == FREELOOK) {
            OrbitaConfig cfg = OrbitaConfig.get();
            toggleRow("Activado", "Mira alrededor sin girar a tu personaje",
                    () -> cfg.freelookEnabled, v -> cfg.freelookEnabled = v);
            keyRow("Tecla", "Mantén (o pulsa, en modo alternar) para activar", new String[]{""}, OrbitaClient.freelookKey());
            toggleRow("Modo alternar", "Pulsa una vez para activar y otra para desactivar",
                    () -> cfg.freelookToggle, v -> cfg.freelookToggle = v);
            toggleRow("Invertir eje vertical", "Mover el ratón arriba mira hacia abajo",
                    () -> cfg.freelookInvert, v -> cfg.freelookInvert = v);
        } else if (options == ZOOM) {
            OrbitaConfig cfg = OrbitaConfig.get();
            toggleRow("Activado", "Acerca la vista manteniendo una tecla",
                    () -> cfg.zoomEnabled, v -> cfg.zoomEnabled = v);
            keyRow("Tecla", "Mantén para hacer zoom", new String[]{""}, OrbitaClient.zoomKey());
            toggleRow("Animación suave", "Transición fluida al acercar", () -> cfg.zoomAnimated, v -> cfg.zoomAnimated = v);
            toggleRow("Cámara cinemática", "Suaviza la cámara durante el zoom", () -> cfg.zoomSmoothCamera, v -> cfg.zoomSmoothCamera = v);
            toggleRow("Ajustar con la rueda", "La rueda del ratón acerca o aleja", () -> cfg.zoomScroll, v -> cfg.zoomScroll = v);
            stepperRow("Nivel de zoom", () -> String.format(Locale.ROOT, "%.1fx", cfg.zoomLevel),
                    d -> cfg.zoomLevel = Mth.clamp(cfg.zoomLevel + d * 0.5, 1.5, 50.0));
        }
    }

    /** Filas de opciones desplazadas con la rueda (cuando un módulo tiene más de las que caben). */
    private int optionScroll;
    private int optionRowsTotal;

    private int visibleRows() {
        return Math.max(1, (gh - 8) / 28);
    }

    private int nextRowY() {
        return gy + 4 + (optionRow++ - optionScroll) * 28;
    }

    private boolean inView(int y) {
        return y >= gy && y + 24 <= gy + gh;
    }

    // --------------------------------------------------------------- opciones de los módulos de Nova

    private void novaOptions(NovaEntry ne) {
        dev.orbita.client.nova.module.Module m = ne.module();
        if (!m.alwaysOn) {
            toggleRow("Activado", ne.description(), () -> m.enabled, v -> {
                m.setEnabled(v);
                saveNova();
            });
        }
        boolean saving = m instanceof dev.orbita.client.nova.module.ClientSettings;
        for (dev.orbita.client.nova.core.Setting s : m.settings) {
            if (!s.isVisible()) {
                continue;
            }
            // De los ajustes del cliente de Nova solo se trajo el modo ahorro (idioma, tema y menús son de Órbita).
            if (saving && !s.name.equals("Saving mode") && !s.name.equals("Beta boost (experimental)")) {
                continue;
            }
            if (s.name.equals("Nova menu")) {
                continue; // el panel de Nova no existe en Órbita
            }
            String title = dev.orbita.client.nova.core.Lang.t(s.name);
            if (s instanceof dev.orbita.client.nova.core.Setting.Bool b) {
                toggleRow(title, "", () -> b.value, v -> {
                    b.value = v;
                    novaChanged();
                });
            } else if (s instanceof dev.orbita.client.nova.core.Setting.Choice c) {
                cycleRow(title, "", () -> dev.orbita.client.nova.core.Lang.t(c.get()), () -> {
                    c.next();
                    novaChanged();
                });
            } else if (s instanceof dev.orbita.client.nova.core.Setting.Num n) {
                double step = n.step > 0 ? n.step : (n.max - n.min) / 20;
                stepperRow(title, () -> n.step >= 1 ? String.valueOf(n.i()) : String.format(Locale.ROOT, "%.2f", n.value), d -> {
                    n.set(n.value + d * step);
                    novaChanged();
                });
            } else if (s instanceof dev.orbita.client.nova.core.Setting.Col col) {
                stepperRow(title + " · tono", () -> Math.round(col.hue * 360) + "°", d -> {
                    col.hue = (col.hue + d * 0.05f + 1f) % 1f;
                    novaChanged();
                });
                stepperRow(title + " · luz", () -> Math.round(col.light * 100) + "%", d -> {
                    col.light = Mth.clamp(col.light + d * 0.1f, 0f, 1f);
                    novaChanged();
                });
            } else if (s instanceof dev.orbita.client.nova.core.Setting.Pick p) {
                cycleRow(title, p.hint, () -> p.value.isEmpty() ? dev.orbita.client.nova.core.Lang.t(p.noneLabel) : p.value, () -> {
                    List<String> opts = new ArrayList<>();
                    opts.add("");
                    opts.addAll(p.options.get());
                    p.value = opts.get((Math.max(0, opts.indexOf(p.value)) + 1) % opts.size());
                    novaChanged();
                });
            } else if (s instanceof dev.orbita.client.nova.core.Setting.Str str) {
                textRow(title, str);
            } else if (s instanceof dev.orbita.client.nova.core.Setting.Key k) {
                novaKeyRow(title, k);
            } else if (s instanceof dev.orbita.client.nova.core.Setting.Action a) {
                actionRow(title, dev.orbita.client.nova.core.Lang.t(a.button), () -> {
                    a.run.run();
                    novaChanged();
                });
            }
        }
        if (m instanceof dev.orbita.client.nova.module.Emotes) {
            keyRow("Tecla de emotes", "Abre la rueda de emotes", new String[]{""}, dev.orbita.client.nova.NovaClient.emoteKey);
        }
    }

    /** Guarda y vuelve a armar las filas (algunas opciones aparecen o se ocultan según otras). */
    private void novaChanged() {
        saveNova();
        rebuildWidgets();
    }

    /** Fila con un campo de texto (por ejemplo, el nombre falso de "Cambiar nombre"). */
    private void textRow(String title, dev.orbita.client.nova.core.Setting.Str str) {
        int y = nextRowY();
        if (!inView(y)) {
            return;
        }
        rowBoxes.add(new int[]{gx, y, gw, 24, rowBoxes.size()});
        titles.add(new String[]{title, ""});
        net.minecraft.client.gui.components.EditBox box = new net.minecraft.client.gui.components.EditBox(font, gx + gw - 160, y + 5, 154, 14, Component.literal(title));
        box.setMaxLength(str.max);
        box.setValue(str.value);
        box.setResponder(v -> {
            str.value = v;
            saveNova();
        });
        addRenderableWidget(box);
    }

    /** Fila con un botón que hace algo (abrir carpeta, restablecer...). */
    private void actionRow(String title, String button, Runnable run) {
        int y = nextRowY();
        if (!inView(y)) {
            return;
        }
        rowBoxes.add(new int[]{gx, y, gw, 24, rowBoxes.size()});
        titles.add(new String[]{title, ""});
        addRenderableWidget(new OrbitaButton(gx + gw - 112, y + 4, 106, 16, Component.literal(button), b -> run.run()).noIntro());
    }

    /** Ajuste de tecla de Nova (código GLFW): clic y pulsa la tecla nueva. */
    @Nullable
    private dev.orbita.client.nova.core.Setting.Key capturingNova;

    private void novaKeyRow(String title, dev.orbita.client.nova.core.Setting.Key k) {
        int y = nextRowY();
        if (!inView(y)) {
            return;
        }
        rowBoxes.add(new int[]{gx, y, gw, 24, rowBoxes.size()});
        titles.add(new String[]{title, ""});
        String label = k == capturingNova ? "> Pulsa una tecla <"
                : k.code < 0 ? "Sin tecla" : InputConstants.Type.KEYSYM.getOrCreate(k.code).getDisplayName().getString();
        addRenderableWidget(new OrbitaButton(gx + gw - 102, y + 4, 96, 16, Component.literal(label), b -> {
            capturingNova = k;
            rebuildWidgets();
        }).selected(k == capturingNova).noIntro());
    }

    private void toggleRow(String title, String desc, BooleanSupplier get, Consumer<Boolean> set) {
        int y = nextRowY();
        if (!inView(y)) {
            return;
        }
        rowBoxes.add(new int[]{gx, y, gw, 24, rowBoxes.size()});
        titles.add(new String[]{title, desc});
        addRenderableWidget(new ToggleSwitch(gx + gw - 34, y + 5, get, v -> {
            set.accept(v);
            HudManager.get().save();
        }));
    }

    private final List<String[]> titles = new ArrayList<>();

    /** Tecla que se está reasignando (espera la siguiente pulsación), o null. */
    @Nullable
    private KeyMapping capturing;

    /** Fila con uno o varios botones para reasignar teclas (clic → pulsa la nueva tecla). */
    private void keyRow(String title, String desc, String[] prefixes, KeyMapping... mappings) {
        int y = nextRowY();
        if (!inView(y)) {
            return;
        }
        rowBoxes.add(new int[]{gx, y, gw, 24, rowBoxes.size()});
        String conflict = conflictText(mappings);
        titles.add(conflict == null ? new String[]{title, desc} : new String[]{title, conflict, "warn"});
        int bw = mappings.length == 1 ? 96 : 62;
        int x = gx + gw - 6 - mappings.length * (bw + 4) + 4;
        for (int i = 0; i < mappings.length; i++) {
            KeyMapping km = mappings[i];
            String label = km == capturing ? "> Pulsa una tecla <"
                    : prefixes[i] + (km.isUnbound() ? "Sin tecla" : km.getTranslatedKeyMessage().getString());
            addRenderableWidget(new OrbitaButton(x + i * (bw + 4), y + 4, bw, 16, Component.literal(label), b -> {
                capturing = km;
                rebuildWidgets();
            }).selected(km == capturing).noIntro());
        }
    }

    @Nullable
    private String conflictText(KeyMapping... mappings) {
        for (KeyMapping km : mappings) {
            if (km.isUnbound()) {
                continue;
            }
            for (KeyMapping other : minecraft.options.keyMappings) {
                if (other != km && other.same(km)) {
                    return "⚠ " + km.getTranslatedKeyMessage().getString() + " ya se usa en: "
                            + Component.translatable(other.getName()).getString();
                }
            }
        }
        return null;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (capturingNova != null) {
            if (event.key() == InputConstants.KEY_BACKSPACE || event.key() == InputConstants.KEY_DELETE) {
                capturingNova.code = -1;
            } else if (event.key() != InputConstants.KEY_ESCAPE) {
                capturingNova.code = event.key();
            }
            capturingNova = null;
            saveNova();
            rebuildWidgets();
            return true;
        }
        if (capturing != null) {
            if (event.key() == InputConstants.KEY_ESCAPE) {
                capturing = null;
            } else if (event.key() == InputConstants.KEY_BACKSPACE || event.key() == InputConstants.KEY_DELETE) {
                capturing.setKey(InputConstants.UNKNOWN);
            } else {
                capturing.setKey(InputConstants.getKey(event));
            }
            capturing = null;
            KeyMapping.resetMapping();
            minecraft.options.save();
            rebuildWidgets();
            return true;
        }
        return super.keyPressed(event);
    }

    /** Fila con "−  valor  +". Puede haber varias en la misma pantalla. */
    private void stepperRow(String title, java.util.function.Supplier<String> value, java.util.function.IntConsumer step) {
        int y = nextRowY();
        if (!inView(y)) {
            return;
        }
        rowBoxes.add(new int[]{gx, y, gw, 24, -2});
        steppers.add(new Stepper(title, value, y));
        addRenderableWidget(new OrbitaButton(gx + gw - 92, y + 4, 18, 16, Component.literal("-"), b -> {
            step.accept(-1);
            HudManager.get().save();
        }).noIntro());
        addRenderableWidget(new OrbitaButton(gx + gw - 24, y + 4, 18, 16, Component.literal("+"), b -> {
            step.accept(1);
            HudManager.get().save();
        }).noIntro());
    }

    /** Fila con un botón que pasa a la siguiente opción en cada clic (estilo, fuente, posición...). */
    private void cycleRow(String title, String desc, java.util.function.Supplier<String> label, Runnable next) {
        int y = nextRowY();
        if (!inView(y)) {
            return;
        }
        rowBoxes.add(new int[]{gx, y, gw, 24, rowBoxes.size()});
        titles.add(new String[]{title, desc});
        addRenderableWidget(new OrbitaButton(gx + gw - 112, y + 4, 106, 16, Component.literal(label.get() + "  ›"), b -> {
            next.run();
            HudManager.get().save();
            b.setMessage(Component.literal(label.get() + "  ›"));
        }).noIntro());
    }

    private record Stepper(String title, java.util.function.Supplier<String> value, int y) {
    }

    private final List<Stepper> steppers = new ArrayList<>();

    @Override
    protected void rebuildWidgets() {
        titles.clear();
        steppers.clear();
        super.rebuildWidgets();
    }

    // --------------------------------------------------------------- dibujo

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderStart = System.nanoTime();
        if (minecraft.level == null) {
            Starfield.SHARED.render(g, width, height, mouseX, mouseY);
        } else {
            g.fill(0, 0, width, height, 0xA0000000);
        }
        float t = Animation.easeOutCubic(open.update());
        float time = dev.orbita.client.ui.anim.Animation.seconds();
        // Resplandor de nebulosa detrás del panel.
        float breathe = 0.75f + 0.25f * Mth.sin(time * 0.6f);
        dev.orbita.client.ui.render.SpaceScene.glow(g, px + pw * 0.15f, py + ph * 0.1f, pw * 0.55f, ph * 0.6f,
                OrbitaTheme.withAlpha(0x508B7CFF, t * breathe));
        dev.orbita.client.ui.render.SpaceScene.glow(g, px + pw * 0.9f, py + ph * 0.95f, pw * 0.5f, ph * 0.55f,
                OrbitaTheme.withAlpha(0x405CAAFF, t * (1.5f - breathe)));
        UiDraw.roundedRect(g, px - 1, py - 1, pw + 2, ph + 2, 7, OrbitaTheme.withAlpha(0x34FFFFFF, t));
        UiDraw.roundedRect(g, px, py, pw, ph, 6, OrbitaTheme.withAlpha(0xF20D0F17, t));
        // Cabecera con degradado del color de acento y destello que recorre el borde.
        g.fillGradient(px + 1, py + 1, px + pw - 1, py + 52, OrbitaTheme.withAlpha(0x603A2E8C, t), OrbitaTheme.withAlpha(0x000D0F17, t));
        dev.orbita.client.ui.render.Fx.runningBorder(g, px - 1, py - 1, pw + 2, ph + 2, time, OrbitaTheme.withAlpha(0xC0B7A8FF, t));
        // Constelación tenue en la cabecera.
        for (int i = 0; i < 18; i++) {
            int sx = px + 140 + (i * 97 % Math.max(1, pw - 360));
            int sy = py + 4 + (i * 37 % 20);
            float tw = 0.4f + 0.6f * (0.5f + 0.5f * Mth.sin(time * (1.3f + i * 0.17f) + i));
            g.fill(sx, sy, sx + 1, sy + 1, OrbitaTheme.withAlpha(0xFFFFFFFF, t * tw * 0.6f));
        }
        if (options == null) {
            UiDraw.roundedRect(g, px + pw - 202, py + 6, 128, 17, 8, OrbitaTheme.withAlpha(0xFF1B1E29, t));
        }
        g.fill(px + 10, py + 52, px + pw - 10, py + 53, OrbitaTheme.withAlpha(0x22FFFFFF, t));
        for (int[] row : rowBoxes) {
            UiDraw.roundedRect(g, row[0], row[1], row[2], row[3], 4, OrbitaTheme.withAlpha(0xC01B1E29, t));
        }
        dev.orbita.client.DevShots.part(0, System.nanoTime() - renderStart);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        long overlayStart = System.nanoTime();
        try {
            renderOverlay(g, mouseX, mouseY);
            dev.orbita.client.DevShots.part(4, System.nanoTime() - overlayStart);
        } finally {
            if (dev.orbita.client.DevShots.enabled()) {
                dev.orbita.client.DevShots.panelFrame(System.nanoTime() - renderStart);
            }
        }
    }

    private long renderStart;

    private void renderOverlay(GuiGraphics g, int mouseX, int mouseY) {
        float time = dev.orbita.client.ui.anim.Animation.seconds();
        g.drawString(font, OrbitaFonts.bold("ÓRBITA"), px + 12, py + 11, OrbitaTheme.ACCENT, false);
        dev.orbita.client.ui.render.Fx.shimmerText(g, font, "ÓRBITA", OrbitaFonts.MENU_BOLD, px + 12, py + 11,
                0x00FFFFFF, 0xFFFFFFFF, time, 0.7f, false);
        Component titleC = OrbitaFonts.menu(options == null ? "Mods" : options.name());
        int titleX = px + 16 + font.width(OrbitaFonts.bold("ÓRBITA"));
        g.drawString(font, titleC, titleX, py + 11, OrbitaTheme.TEXT, false);
        if (options == null) {
            // Contador de módulos activos.
            long on = all.stream().filter(e -> e.toggleable() && e.enabled()).count();
            long total = all.stream().filter(PanelCard::toggleable).count();
            String chip = on + "/" + total + " activos";
            int chipX = titleX + font.width(titleC) + 8;
            int chipW = font.width(chip) + 12;
            UiDraw.roundedRect(g, chipX, py + 8, chipW, 13, 6, 0x3023A35F);
            UiDraw.circle(g, chipX + 6, py + 14, 2, OrbitaTheme.withAlpha(0xFF7CFFB2, 0.6f + 0.4f * Mth.sin(time * 3f)));
            g.drawString(font, chip, chipX + 10, py + 11, 0xFFBDF5D3, false);

            // Puntos de página.
            int totalRows = (visible.size() + Math.max(1, cols) - 1) / Math.max(1, cols);
            int pageCount = Math.max(1, totalRows - rows + 1);
            if (pageCount > 1) {
                int dotsX = px + 12;
                for (int i = 0; i < pageCount; i++) {
                    boolean cur = i == scrollRow;
                    UiDraw.roundedRect(g, dotsX, py + ph - 7, cur ? 12 : 4, 4, 2, cur ? OrbitaTheme.ACCENT : 0x50FFFFFF);
                    dotsX += (cur ? 12 : 4) + 4;
                }
            }
        }

        if (options == null) {
            int pages = Math.max(1, (visible.size() + cols - 1) / cols - rows + 1);
            if (pages > 1) {
                String s = "Rueda para ver más (" + (scrollRow + 1) + "/" + pages + ")";
                g.drawString(font, s, px + pw - 10 - font.width(s), py + ph - 9, 0xFF5F6478, false);
            }
            if (visible.isEmpty()) {
                Component c = OrbitaFonts.menu("No hay mods que coincidan con \"" + search + "\"");
                g.drawCenteredString(font, c, px + pw / 2, gy + 40, OrbitaTheme.TEXT_MUTED);
            }
            dev.orbita.client.ui.render.Fx.cursorTrail(mouseX, mouseY);
            dev.orbita.client.ui.render.Fx.renderParticles(g);
            return;
        }

        g.drawString(font, OrbitaFonts.menu(options.description()), px + 76, py + 35, OrbitaTheme.TEXT_MUTED, false);
        int ti = 0;
        for (int[] row : rowBoxes) {
            if (row[4] >= 0 && ti < titles.size()) {
                String[] td = titles.get(ti++);
                g.drawString(font, OrbitaFonts.menu(td[0]), row[0] + 10, row[1] + 4, OrbitaTheme.TEXT, false);
                g.drawString(font, td[1], row[0] + 10, row[1] + 14, td.length > 2 ? 0xFFFFB347 : 0xFF6F748A, false);
            } else if (row[4] == -1 && options instanceof ModuleEntry me) {
                g.drawString(font, OrbitaFonts.menu("Color del texto"), row[0] + 10, row[1] + 8, OrbitaTheme.TEXT, false);
                int c = me.module().textColor();
                UiDraw.circle(g, gx + gw - 104, row[1] + 12, 5, c);
            }
        }
        for (Stepper sp : steppers) {
            g.drawString(font, OrbitaFonts.menu(sp.title()), gx + 10, sp.y() + 8, OrbitaTheme.TEXT, false);
            String v = sp.value().get();
            g.drawString(font, v, gx + gw - 49 - font.width(v) / 2, sp.y() + 8, OrbitaTheme.TEXT, false);
        }
        if (optionRowsTotal > visibleRows()) {
            String s = "Rueda para ver más (" + (optionScroll + visibleRows()) + "/" + optionRowsTotal + ")";
            g.drawString(font, s, px + pw - 10 - font.width(s), py + ph - 9, 0xFF5F6478, false);
        }
        dev.orbita.client.ui.render.Fx.renderParticles(g);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (options != null && optionRowsTotal > visibleRows()) {
            int before = optionScroll;
            optionScroll = Mth.clamp(optionScroll - (int) Math.signum(scrollY), 0, optionRowsTotal - visibleRows());
            if (optionScroll != before) {
                rebuildWidgets();
            }
            return true;
        }
        if (options == null && cols > 0) {
            int totalRows = (visible.size() + cols - 1) / cols;
            int before = scrollRow;
            scrollRow = Mth.clamp(scrollRow - (int) Math.signum(scrollY), 0, Math.max(0, totalRows - rows));
            if (scrollRow != before) {
                // Las tarjetas nuevas entran deslizándose desde el lado hacia el que te mueves.
                scrollFrom = (scrollRow - before) * (cardH + GAP) * 0.6f;
                scrollAt = net.minecraft.util.Util.getMillis();
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void onClose() {
        if (options != null) {
            showOptions(null);
            return;
        }
        minecraft.setScreen(parent);
    }

    // --------------------------------------------------------------- tarjeta

    private final class Card extends AbstractWidget {
        private static final int BAR = 15;
        private final int slot;
        private final Animation hover = new Animation(0f, 14f);
        private final Animation state = new Animation(0f, 12f);
        private final long bornAt = net.minecraft.util.Util.getMillis();

        Card(int x, int y, int w, int h, int slot) {
            super(x, y, w, h, Component.empty());
            this.slot = slot;
        }

        @Nullable
        private PanelCard entry() {
            int i = scrollRow * cols + slot;
            List<PanelCard> list = ControlPanelScreen.this.visible;
            return i < list.size() ? list.get(i) : null;
        }

        @Override
        public void onClick(MouseButtonEvent event, boolean doubleClick) {
            PanelCard e = entry();
            if (e == null) {
                return;
            }
            if (e.toggleable() && event.y() >= getY() + getHeight() - BAR) {
                e.setEnabled(!e.enabled());
                dev.orbita.client.ui.render.Fx.burst(getX() + getWidth() / 2f, getY() + getHeight() * 0.29f,
                        e.enabled() ? 0xFF7CFFB2 : 0xFFFF7C7C, e.enabled() ? 26 : 12, e.enabled() ? 90f : 50f);
            } else {
                e.open(ControlPanelScreen.this);
            }
        }

        @Override
        public boolean isMouseOver(double mouseX, double mouseY) {
            return entry() != null && super.isMouseOver(mouseX, mouseY);
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            PanelCard e = entry();
            if (e == null) {
                return;
            }
            long t0 = System.nanoTime();
            Font f = font;
            hover.setTarget(isHovered() ? 1f : 0f);
            state.setTarget(e.enabled() ? 1f : 0f);
            float h = hover.update();
            float s = state.update();
            float time = dev.orbita.client.ui.anim.Animation.seconds();
            // Entrada escalonada: cada tarjeta aparece un poco después que la anterior.
            float in = Animation.easeOutCubic(Mth.clamp((net.minecraft.util.Util.getMillis() - bornAt - slot * 28L) / 260f, 0f, 1f));
            if (in <= 0f) {
                return;
            }
            int x = getX();
            float shift = scrollShift();
            int y = getY() - Math.round(h * 2) + Math.round((1f - in) * 10f) + Math.round(shift);
            int w = getWidth();
            int ht = getHeight();
            float fadeScroll = 1f - Math.min(1f, Math.abs(shift) / Math.max(1f, cardH));
            if (shift != 0f) {
                g.enableScissor(gx - 8, gy - 6, gx + gw + 8, gy + gh + 2);
            }
            try {
                renderCard(g, e, x(), y, w, ht, h, s, time, fadeScroll, t0);
            } finally {
                if (shift != 0f) {
                    g.disableScissor();
                }
            }
        }

        private int x() {
            return getX();
        }

        private void renderCard(GuiGraphics g, PanelCard e, int x, int y, int w, int ht, float h, float s, float time,
                                float fadeScroll, long t0) {
            Font f = font;

            if (h > 0.01f) {
                dev.orbita.client.ui.render.SpaceScene.glow(g, x + w / 2f, y + ht / 2f, w * 0.85f, ht * 0.85f,
                        OrbitaTheme.withAlpha(0x508B7CFF, h));
            }
            UiDraw.roundedRect(g, x - 1, y - 1, w + 2, ht + 2, 6, OrbitaTheme.lerpColor(0x26FFFFFF, OrbitaTheme.ACCENT, h * 0.8f));
            UiDraw.roundedRect(g, x, y, w, ht, 5, OrbitaTheme.lerpColor(0xFF161925, 0xFF1D2131, h));
            // Degradado interior: violeta si está activado, neutro si no.
            int topTint = OrbitaTheme.lerpColor(0x12FFFFFF, 0x704A3AB0, Math.max(s, h * 0.6f));
            // Una sola pieza, un poco hacia dentro para respetar las esquinas redondeadas.
            g.fillGradient(x + 2, y + 2, x + w - 2, y + ht - BAR, topTint, 0x00161925);
            if (h > 0.01f) {
                dev.orbita.client.ui.render.Fx.sweep(g, x, y, w, ht - BAR, (time * 0.9f) % 1.4f, OrbitaTheme.withAlpha(0x26FFFFFF, h));
            }

            // Icono: círculo con el glifo del módulo
            int cx = x + w / 2;
            int cy = y + Math.round(ht * 0.29f);
            int radius = ht >= 80 ? 17 : 14;
            if (s > 0.01f) {
                float pulse = 0.7f + 0.3f * Mth.sin(time * 2.2f + slot);
                dev.orbita.client.ui.render.SpaceScene.glow(g, cx, cy, radius * 2.1f, radius * 2.1f,
                        OrbitaTheme.withAlpha(0x708B7CFF, s * pulse));
            }
            UiDraw.circle(g, cx, cy, radius + 1, OrbitaTheme.lerpColor(0x18FFFFFF, 0x408B7CFF, Math.max(s * 0.6f, h)));
            UiDraw.circle(g, cx, cy, radius, OrbitaTheme.lerpColor(0xFF1E2231, 0xFF2A2550, s));
            // Anillo orbital que gira alrededor del icono.
            float ringA = Math.max(s * 0.55f, h);
            if (ringA > 0.01f) {
                float spin = time * (1.2f + h * 2.5f) + slot;
                // Dos arcos de una sola pieza cada uno (antes eran ~90 puntos sueltos).
                if (!UiDraw.orbitArc(g, cx, cy, radius + 3.5f, spin + Mth.PI * 1.1f, OrbitaTheme.withAlpha(OrbitaTheme.ACCENT, ringA))) {
                    dev.orbita.client.ui.render.Fx.arc(g, cx, cy, radius + 3.5f, spin, Mth.PI * 1.1f, 1f,
                            OrbitaTheme.withAlpha(OrbitaTheme.ACCENT, ringA), true);
                }
                UiDraw.orbitArc(g, cx, cy, radius + 3.5f, spin + Mth.PI * 0.1f, OrbitaTheme.withAlpha(OrbitaTheme.ACCENT_2, ringA * 0.55f));
            }
            // Categoría en la esquina.
            String cat = e.filter().label.toUpperCase(Locale.ROOT);
            g.pose().pushMatrix();
            g.pose().translate(x + 5, y + 5);
            g.pose().scale(0.6f, 0.6f);
            int catW = font.width(cat);
            UiDraw.roundedRect(g, -3, -2, catW + 6, 11, 4, OrbitaTheme.withAlpha(0x308B7CFF, 0.6f + h * 0.4f));
            g.drawString(font, cat, 0, 0, OrbitaTheme.withAlpha(0xFFB7A8FF, 0.7f + h * 0.3f), false);
            g.pose().popMatrix();
            long t1 = System.nanoTime();
            dev.orbita.client.DevShots.part(1, t1 - t0);
            float is = (ht >= 80 ? 1.5f : 1.25f) * (1f + h * 0.08f);
            g.pose().pushMatrix();
            g.pose().translate(cx - 8 * is, cy - 8 * is);
            g.pose().scale(is, is);
            net.minecraft.resources.Identifier modIcon = dev.orbita.client.ui.render.ModIcons.get(e.iconKey());
            if (modIcon != null) {
                // icono propio del mod: blanco si está activado, gris si no
                int tint = OrbitaTheme.lerpColor(0xFFA9ADC0, 0xFFFFFFFF, Math.max(s, h));
                g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, modIcon, 0, 0, 0f, 0f, 16, 16,
                        dev.orbita.client.ui.render.ModIcons.SIZE, dev.orbita.client.ui.render.ModIcons.SIZE,
                        dev.orbita.client.ui.render.ModIcons.SIZE, dev.orbita.client.ui.render.ModIcons.SIZE, tint);
            } else {
                g.renderItem(e.icon(), 0, 0);
            }
            g.pose().popMatrix();
            long t2 = System.nanoTime();
            dev.orbita.client.DevShots.part(2, t2 - t1);

            Component name = OrbitaFonts.menu(e.name());
            int nw = f.width(name);
            if (nw > w - 8) {
                g.drawString(f, f.plainSubstrByWidth(e.name(), w - 8), x + 4, y + ht - BAR - 13, OrbitaTheme.TEXT, false);
            } else {
                g.drawString(f, name, cx - nw / 2, y + ht - BAR - 13, OrbitaTheme.TEXT, false);
            }
            if (!e.toggleable() || e instanceof ModuleEntry || e == ZOOM || e == FREELOOK) {
                g.drawString(f, "⚙", x + w - 11, y + 4, OrbitaTheme.withAlpha(OrbitaTheme.TEXT_MUTED, 0.5f + h * 0.5f), false);
            }

            int barY = y + ht - BAR;
            int barColor;
            String label;
            if (e.toggleable()) {
                barColor = OrbitaTheme.lerpColor(0xFFB8333F, 0xFF23A35F, s);
                label = e.enabled() ? "ACTIVADO" : "DESACTIVADO";
            } else {
                barColor = 0xFF5B3FC4;
                label = "ABRIR";
            }
            UiDraw.roundedBottom(g, x, barY, w, BAR, 5, barColor);
            // Brillo superior de la barra y mini interruptor.
            g.fillGradient(x, barY, x + w, barY + 6, 0x30FFFFFF, 0x00FFFFFF);
            if (e.toggleable()) {
                int kx = x + w - 17;
                int ky = barY + 4;
                UiDraw.roundedRect(g, kx, ky, 12, 7, 3, 0x40000000);
                int knob = kx + 1 + Math.round(s * 5);
                UiDraw.circle(g, knob + 3, ky + 3 + 1, 3, 0xFFFFFFFF);
            }
            g.pose().pushMatrix();
            g.pose().translate(cx - f.width(label) * 0.75f / 2f, barY + 5);
            g.pose().scale(0.75f, 0.75f);
            g.drawString(f, label, 0, 0, 0xFFFFFFFF, false);
            g.pose().popMatrix();
            dev.orbita.client.DevShots.part(3, System.nanoTime() - t2);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            PanelCard e = entry();
            if (e != null) {
                output.add(net.minecraft.client.gui.narration.NarratedElementType.TITLE, Component.literal(e.name()));
            }
        }
    }
}
