package dev.orbita.client.ui.screen;

import dev.orbita.client.OrbitaClient;
import dev.orbita.client.ui.anim.Animation;
import dev.orbita.client.ui.render.SpaceScene;
import dev.orbita.client.ui.render.Starfield;
import dev.orbita.client.ui.render.UiDraw;
import dev.orbita.client.ui.theme.OrbitaFonts;
import dev.orbita.client.ui.theme.OrbitaTheme;
import dev.orbita.client.ui.widget.AccountCard;
import dev.orbita.client.ui.widget.OrbitaButton;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.options.LanguageSelectScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * Menú principal de Órbita (reemplaza al TitleScreen vanilla, ver MinecraftMixin):
 * entradas de texto a la izquierda y un planeta con anillo, luna y cohete en órbita.
 */
public class OrbitaMainMenu extends Screen {
    private static final String[] TIPS = {
            "Pulsa Shift derecho dentro del juego para abrir el panel de Órbita",
            "Mantén Z para hacer zoom; la rueda del ratón ajusta el aumento",
            "Arrastra los módulos del HUD desde Mods → Editar HUD",
            "Elige una capa espacial en tu perfil",
            "Toggle Sprint: toca la tecla de correr una vez y olvídate"
    };
    private static final String[] GREETINGS = {
            "Los astros te esperan, %s",
            "Listo para el despegue, %s",
            "Nueva órbita, nuevas aventuras, %s",
            "El universo es tuyo, %s"
    };
    private static final int TIP = (int) (System.currentTimeMillis() / 1000 % TIPS.length);
    private static final int GREETING = (int) (System.currentTimeMillis() / 7000 % GREETINGS.length);

    private final Animation intro = new Animation(0f, 3.5f);
    private int left;
    private int top;

    public OrbitaMainMenu() {
        super(Component.literal("Órbita"));
    }

    @Override
    protected void init() {
        intro.setTarget(1f);
        left = Math.max(22, (int) (width * 0.075f));
        top = Math.max(84, (int) (height * 0.36f));
        int bx = left - 12;
        int bw = 150;
        int bh = 16;
        int step = 19;
        float ts = 1.3f;

        addRenderableWidget(new OrbitaButton(bx, top, bw, bh, OrbitaFonts.menu("Un jugador"),
                b -> minecraft.setScreen(new SelectWorldScreen(this))).style(OrbitaButton.Style.MENU).textScale(ts).delay(150));
        addRenderableWidget(new OrbitaButton(bx, top + step, bw, bh, OrbitaFonts.menu("Multijugador"),
                b -> minecraft.setScreen(new JoinMultiplayerScreen(this))).style(OrbitaButton.Style.MENU).textScale(ts).delay(210));
        addRenderableWidget(new OrbitaButton(bx, top + step * 2, bw, bh, OrbitaFonts.menu("Mods"),
                b -> minecraft.setScreen(new ControlPanelScreen(this))).style(OrbitaButton.Style.MENU).textScale(ts).delay(270));
        addRenderableWidget(new OrbitaButton(bx, top + step * 3, bw, bh, OrbitaFonts.menu("Opciones"),
                b -> minecraft.setScreen(new OptionsScreen(this, minecraft.options))).style(OrbitaButton.Style.MENU).textScale(ts).delay(330));
        addRenderableWidget(new OrbitaButton(bx, top + step * 4 + 10, bw, bh, OrbitaFonts.menu("Salir del juego"),
                b -> minecraft.stop()).style(OrbitaButton.Style.MENU).textScale(ts).muted(true).delay(390));

        // Cuentas arriba a la izquierda: al pulsar se despliega el apartado para elegir, quitar y añadir cuentas.
        addRenderableWidget(new AccountCard(12, 10, 170, 28, () -> minecraft.setScreen(new AccountsScreen(this))));
        addRenderableWidget(new OrbitaButton(width - 60, height - 30, 52, 12, OrbitaFonts.menu("Idioma"),
                b -> minecraft.setScreen(new LanguageSelectScreen(this, minecraft.options, minecraft.getLanguageManager())))
                .style(OrbitaButton.Style.GHOST).noIntro());
    }

    private float planetX, planetY, planetR;
    private float shockAt = -100f;

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        float time = dev.orbita.client.ui.anim.Animation.seconds();
        // Video de fondo (traído de Nova): si hay uno elegido, sustituye a las estrellas; la escena sigue encima.
        if (!dev.orbita.client.nova.module.VideoBackground.drawMainMenu(g, width, height)) {
            Starfield.SHARED.render(g, width, height, mouseX, mouseY);
            SpaceScene.aurora(g, width, height, time);
        }
        float px = (mouseX / (float) Math.max(1, width) - 0.5f) * 8f;
        float py = (mouseY / (float) Math.max(1, height) - 0.5f) * 6f;
        // Planeta lejano: se mueve menos con el ratón (está más lejos).
        SpaceScene.distantPlanet(g, width * 0.43f + px * 0.3f, height * 0.15f + py * 0.3f, Math.max(6f, height * 0.028f), time);
        SpaceScene.meteors(g, width, height, time);
        SpaceScene.fog(g, width, height, time);
        // Zoom suave hacia el valor elegido con la rueda.
        long nowNs = System.nanoTime();
        float dt = lastFrameNs == 0 ? 0f : Math.min(0.05f, (nowNs - lastFrameNs) / 1_000_000_000f);
        lastFrameNs = nowNs;
        zoom += (zoomTarget - zoom) * Math.min(1f, dt * 7f);

        float r = height * 0.21f;
        float bob = Mth.sin(time * 0.6f) * 3f;
        // El planeta también deriva despacio por la escena.
        float driftX = Mth.sin(time * 0.11f) * width * 0.012f;
        float driftY = Mth.cos(time * 0.09f) * height * 0.010f;
        float pcx = width * 0.64f + px + driftX;
        float pcy = height * 0.47f + bob + py + driftY;
        planetX = pcx;
        planetY = pcy;
        planetR = r * zoom;
        sceneCx = pcx;
        sceneCy = pcy;

        // Luna y planeta lejano: se acercan/alejan menos que el planeta (están más lejos).
        float far = 1f + (zoom - 1f) * 0.45f;
        g.pose().pushMatrix();
        g.pose().translate(pcx, pcy);
        g.pose().scale(far, far);
        g.pose().translate(-pcx, -pcy);
        float mr = Math.max(16f, height * 0.085f);
        SpaceScene.moon(g, width * 0.87f, height * 0.17f, mr);
        SpaceScene.satellite(g, width * 0.87f, height * 0.17f, mr, time);
        g.pose().popMatrix();

        g.pose().pushMatrix();
        g.pose().translate(pcx, pcy);
        g.pose().scale(zoom, zoom);
        g.pose().translate(-pcx, -pcy);
        // Atmósfera que late suavemente.
        float pulse = 0.5f + 0.5f * Mth.sin(time * 0.9f);
        SpaceScene.glow(g, pcx, pcy, r * (1.9f + pulse * 0.15f), r * (1.9f + pulse * 0.15f),
                OrbitaTheme.withAlpha(0x305CAAFF, 0.6f + pulse * 0.4f));
        float tilt = -0.21f;
        updateRocket(pcx, pcy, r, tilt, time, dt, mouseX, mouseY);
        SpaceScene.planetBack(g, pcx, pcy, r, tilt);
        if (rocketBehind) {
            drawRocket(g);
        }
        SpaceScene.planetFront(g, pcx, pcy, r, tilt, time, 36f);
        SpaceScene.ringSparkles(g, pcx, pcy, r, tilt, time);
        if (!rocketBehind) {
            drawRocket(g);
        }
        SpaceScene.shockwave(g, pcx, pcy, r, time - shockAt);
        g.pose().popMatrix();
        sceneCx = pcx;
        sceneCy = pcy;
    }

    // ------------------------------------------------------------------ zoom y cohete

    private float zoom = 1f;
    private float zoomTarget = 1f;
    private long lastFrameNs;
    private float zoomShownAt = -100f;
    private float sceneCx, sceneCy;

    private enum RocketMode { ORBIT, DRAG, RETURN }

    private RocketMode rocketMode = RocketMode.ORBIT;
    private float rocketX, rocketY, rocketAngle, rocketDepth = 1f, rocketThrust;
    private boolean rocketBehind;
    private boolean forceFront;
    private float releaseX, releaseY, releaseAngle, releaseAt;
    private float hoverRocket;
    private static final int TRAIL = 18;
    private final float[] trailX = new float[TRAIL];
    private final float[] trailY = new float[TRAIL];
    private int trailCount;

    /** Coordenadas de la escena (antes del zoom) a partir de las de la pantalla. */
    private float toSceneX(double sx) {
        return (float) ((sx - sceneCx) / zoom + sceneCx);
    }

    private float toSceneY(double sy) {
        return (float) ((sy - sceneCy) / zoom + sceneCy);
    }

    private void updateRocket(float cx, float cy, float r, float tilt, float time, float dt, int mouseX, int mouseY) {
        float theta = time * 0.32f;
        float[] o = SpaceScene.rocketOrbit(cx, cy, r, tilt, theta);
        float[] o2 = SpaceScene.rocketOrbit(cx, cy, r, tilt, theta + 0.01f);
        float orbitAngle = (float) Math.atan2(o2[1] - o[1], o2[0] - o[0]);
        float orbitDepth = 0.85f + 0.25f * Mth.sin(theta);
        boolean orbitBehind = Mth.sin(theta) < 0;
        float prevX = rocketX;
        float prevY = rocketY;

        switch (rocketMode) {
            case ORBIT -> {
                rocketX = o[0];
                rocketY = o[1];
                rocketAngle = orbitAngle;
                rocketDepth = orbitDepth;
                rocketThrust = 0f;
                if (forceFront && !orbitBehind) {
                    forceFront = false;
                }
                rocketBehind = orbitBehind && !forceFront;
            }
            case DRAG -> {
                float tx = toSceneX(mouseX);
                float ty = toSceneY(mouseY);
                float k = Math.min(1f, dt * 16f);
                rocketX += (tx - rocketX) * k;
                rocketY += (ty - rocketY) * k;
                float vx = rocketX - prevX;
                float vy = rocketY - prevY;
                float speed = (float) Math.sqrt(vx * vx + vy * vy) / Math.max(0.001f, dt);
                if (speed > 20f) {
                    rocketAngle = lerpAngle(rocketAngle, (float) Math.atan2(vy, vx), Math.min(1f, dt * 14f));
                }
                rocketThrust += (Math.min(1f, speed / 300f) - rocketThrust) * Math.min(1f, dt * 8f);
                rocketDepth += (1.15f - rocketDepth) * Math.min(1f, dt * 6f);
                rocketBehind = false;
            }
            case RETURN -> {
                float k = Math.min(1f, (time - releaseAt) / 1.6f);
                float e = k < 0.5f ? 4 * k * k * k : 1 - (float) Math.pow(-2 * k + 2, 3) / 2;
                // Vuelve haciendo una curva suave hasta engancharse otra vez a la órbita.
                float mx = (releaseX + o[0]) / 2f + (o[1] - releaseY) * 0.25f;
                float my = (releaseY + o[1]) / 2f - (o[0] - releaseX) * 0.25f;
                float a = 1 - e;
                rocketX = a * a * releaseX + 2 * a * e * mx + e * e * o[0];
                rocketY = a * a * releaseY + 2 * a * e * my + e * e * o[1];
                float vx = rocketX - prevX;
                float vy = rocketY - prevY;
                float moveAngle = vx * vx + vy * vy > 0.01f ? (float) Math.atan2(vy, vx) : rocketAngle;
                rocketAngle = lerpAngle(moveAngle, orbitAngle, e * e);
                rocketDepth = 1.15f + (orbitDepth - 1.15f) * e;
                rocketThrust = 0.8f * (1f - e);
                rocketBehind = false;
                if (k >= 1f) {
                    rocketMode = RocketMode.ORBIT;
                    forceFront = orbitBehind; // sin saltos: pasa por detrás en la próxima vuelta
                }
            }
        }

        // Estela: guarda una posición nueva cuando el cohete se ha movido lo suficiente.
        float dx = rocketX - (trailCount > 0 ? trailX[0] : rocketX + 10);
        float dy = rocketY - (trailCount > 0 ? trailY[0] : rocketY);
        if (dx * dx + dy * dy > 2.2f) {
            System.arraycopy(trailX, 0, trailX, 1, TRAIL - 1);
            System.arraycopy(trailY, 0, trailY, 1, TRAIL - 1);
            trailX[0] = rocketX;
            trailY[0] = rocketY;
            trailCount = Math.min(TRAIL, trailCount + 1);
        }
        if (rocketMode != RocketMode.ORBIT && rocketThrust > 0.35f && Math.random() < 0.5) {
            float sx = (rocketX - Mth.cos(rocketAngle) * 10f - sceneCx) * zoom + sceneCx;
            float sy = (rocketY - Mth.sin(rocketAngle) * 10f - sceneCy) * zoom + sceneCy;
            dev.orbita.client.ui.render.Fx.burst(sx, sy, 0xFFFFB347, 1, 25f);
        }
        float hx = (rocketX - sceneCx) * zoom + sceneCx - mouseX;
        float hy = (rocketY - sceneCy) * zoom + sceneCy - mouseY;
        boolean over = hx * hx + hy * hy < 196f * zoom * zoom && !rocketBehind;
        hoverRocket += ((over || rocketMode == RocketMode.DRAG ? 1f : 0f) - hoverRocket) * Math.min(1f, dt * 10f);
    }

    private void drawRocket(GuiGraphics g) {
        SpaceScene.trail(g, trailX, trailY, trailCount, rocketDepth);
        // Halo suave para que el cohete se distinga sobre el fondo oscuro.
        SpaceScene.glow(g, rocketX, rocketY, 11 * rocketDepth, 11 * rocketDepth, 0x40B7A8FF);
        if (hoverRocket > 0.01f) {
            SpaceScene.glow(g, rocketX, rocketY, 16 * rocketDepth, 16 * rocketDepth, OrbitaTheme.withAlpha(0x708B7CFF, hoverRocket));
        }
        SpaceScene.rocketAt(g, rocketX, rocketY, rocketAngle, rocketDepth * (1f + hoverRocket * 0.12f), rocketThrust);
    }

    /** Solo pruebas: suelta el cohete lejos del planeta como si lo hubieras arrastrado hasta ahí. */
    public void devThrowRocket() {
        rocketMode = RocketMode.RETURN;
        releaseX = sceneCx - planetR * 2.2f;
        releaseY = sceneCy + planetR * 0.9f;
        rocketX = releaseX;
        rocketY = releaseY;
        releaseAt = dev.orbita.client.ui.anim.Animation.seconds();
    }

    private static float lerpAngle(float from, float to, float t) {
        float d = (to - from) % Mth.TWO_PI;
        if (d > Mth.PI) d -= Mth.TWO_PI;
        if (d < -Mth.PI) d += Mth.TWO_PI;
        return from + d * t;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY != 0) {
            zoomTarget = Mth.clamp(zoomTarget * (scrollY > 0 ? 1.12f : 1f / 1.12f), 0.45f, 1.9f);
            zoomShownAt = dev.orbita.client.ui.anim.Animation.seconds();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent event) {
        if (rocketMode == RocketMode.DRAG) {
            rocketMode = RocketMode.RETURN;
            releaseX = rocketX;
            releaseY = rocketY;
            releaseAngle = rocketAngle;
            releaseAt = dev.orbita.client.ui.anim.Animation.seconds();
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) {
            dev.orbita.client.ui.render.Fx.burst((float) event.x(), (float) event.y(), OrbitaTheme.ACCENT, 14, 60f);
            return true;
        }
        // ¿Clic sobre el cohete? Se agarra y sigue al ratón.
        float hx = (rocketX - sceneCx) * zoom + sceneCx - (float) event.x();
        float hy = (rocketY - sceneCy) * zoom + sceneCy - (float) event.y();
        if (event.button() == 0 && !rocketBehind && hx * hx + hy * hy < 196f * zoom * zoom) {
            rocketMode = RocketMode.DRAG;
            dev.orbita.client.ui.render.Fx.burst((float) event.x(), (float) event.y(), 0xFFFFB347, 16, 70f);
            return true;
        }
        if (doubleClick) {
            zoomTarget = 1f; // doble clic: vuelve al tamaño normal
            zoomShownAt = dev.orbita.client.ui.anim.Animation.seconds();
        }
        float dx = (float) event.x() - planetX;
        float dy = (float) event.y() - planetY;
        if (dx * dx + dy * dy < planetR * planetR) {
            shockAt = dev.orbita.client.ui.anim.Animation.seconds();
            dev.orbita.client.ui.render.Fx.burst((float) event.x(), (float) event.y(), 0xFF7FE8DA, 30, 110f);
        } else {
            dev.orbita.client.ui.render.Fx.burst((float) event.x(), (float) event.y(), OrbitaTheme.ACCENT, 10, 45f);
        }
        return false;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        float t = Animation.easeOutCubic(intro.update());

        // Logo: planeta pequeño con anillo + ÓRBITA
        int logoY = top - 50;
        int lx = left;
        float slide = (1f - t) * -10f;
        g.pose().pushMatrix();
        g.pose().translate(0, slide);
        float time = dev.orbita.client.ui.anim.Animation.seconds();
        SpaceScene.glow(g, lx + 9, logoY + 10, 20, 20, OrbitaTheme.withAlpha(0x708B7CFF, t * (0.7f + 0.3f * Mth.sin(time * 1.4f))));
        // Luna diminuta que orbita el logo (por detrás cuando sube, por delante cuando baja).
        float lo = time * 1.3f;
        boolean front = Mth.sin(lo) > 0;
        int ox = Math.round(lx + 9 + Mth.cos(lo) * 15);
        int oy = Math.round(logoY + 11 + Mth.sin(lo) * 4);
        if (!front) {
            UiDraw.circle(g, ox, oy, 2, OrbitaTheme.withAlpha(0xFF7FE8DA, t * 0.7f));
        }
        UiDraw.circle(g, lx + 9, logoY + 10, 9, OrbitaTheme.withAlpha(0xFF5B3FC4, t));
        UiDraw.circle(g, lx + 7, logoY + 8, 6, OrbitaTheme.withAlpha(0xFF8B7CFF, t));
        g.fill(lx - 3, logoY + 12, lx + 21, logoY + 13, OrbitaTheme.withAlpha(0xFFB7A8FF, t));
        if (front) {
            SpaceScene.glow(g, ox, oy, 5, 5, OrbitaTheme.withAlpha(0x907FE8DA, t));
            UiDraw.circle(g, ox, oy, 2, OrbitaTheme.withAlpha(0xFF7FE8DA, t));
        }
        g.pose().pushMatrix();
        g.pose().translate(lx + 25, logoY + 1);
        g.pose().scale(2.1f, 2.1f);
        g.drawString(font, OrbitaFonts.bold("ÓRBITA"), 0, 0, OrbitaTheme.withAlpha(0xFFFFFFFF, t), false);
        // Reflejo de color que recorre el título.
        dev.orbita.client.ui.render.Fx.shimmerText(g, font, "ÓRBITA", OrbitaFonts.MENU_BOLD, 0, 0,
                0x00FFFFFF, 0xFFB7A8FF, time, t * 0.85f, false);
        g.pose().popMatrix();
        // Línea de luz bajo el título.
        int titleW = Math.round(font.width(OrbitaFonts.bold("ÓRBITA")) * 2.1f);
        float sweepK = (time * 0.35f) % 1f;
        g.fillGradient(lx + 25, logoY + 21, lx + 25 + titleW, logoY + 22, OrbitaTheme.withAlpha(0x608B7CFF, t), OrbitaTheme.withAlpha(0x205CAAFF, t));
        int sx = lx + 25 + Math.round(titleW * sweepK);
        SpaceScene.glow(g, sx, logoY + 21.5f, 10, 3, OrbitaTheme.withAlpha(0xC0FFFFFF, t * Mth.sin(sweepK * Mth.PI)));
        String greeting = String.format(GREETINGS[GREETING], minecraft.getUser().getName());
        g.drawString(font, OrbitaFonts.menu(greeting), lx, logoY + 27, OrbitaTheme.withAlpha(OrbitaTheme.TEXT_MUTED, t), false);
        g.pose().popMatrix();

        Component version = OrbitaFonts.menu("Órbita " + OrbitaClient.version() + " · " + OrbitaClient.minecraftVersion());
        int vw = font.width(version);
        g.drawString(font, version, width - 8 - vw, height - 14, OrbitaTheme.withAlpha(0xFF5F6478, t), false);

        // Consejo centrado abajo, solo si cabe entre la tarjeta de cuenta y la versión
        Component tip = OrbitaFonts.menu(TIPS[TIP]);
        int tw = font.width(tip);
        int tx = (width - tw) / 2;
        int cardRight = 0; // la tarjeta de cuenta ahora está arriba
        if (tx - 12 > cardRight + 8 && tx + tw < width - 8 - Math.max(vw, 60) - 8) {
            UiDraw.circle(g, tx - 8, height - 12, 3, OrbitaTheme.withAlpha(OrbitaTheme.ACCENT, t));
            g.drawString(font, tip, tx, height - 16, OrbitaTheme.withAlpha(OrbitaTheme.TEXT_MUTED, t * 0.9f), false);
        }

        renderNews(g, mouseX, mouseY, t);
        renderSceneHints(g);
        dev.orbita.client.ui.render.Fx.cursorTrail(mouseX, mouseY);
        dev.orbita.client.ui.render.Fx.renderParticles(g);
    }

    /** Indicador de zoom al usar la rueda y "¡Arrástrame!" al pasar por encima del cohete. */
    private void renderSceneHints(GuiGraphics g) {
        float time = dev.orbita.client.ui.anim.Animation.seconds();
        float age = time - zoomShownAt;
        if (age < 2.2f) {
            float a = Math.min(1f, Math.min(age * 6f, (2.2f - age) * 3f));
            String s = "Zoom " + Math.round(zoomTarget * 100) + "%  ·  doble clic para volver";
            int w = font.width(OrbitaFonts.menu(s)) + 16;
            int x = Math.round(planetX - w / 2f);
            int y = 14;
            UiDraw.roundedRect(g, x, y, w, 15, 7, OrbitaTheme.withAlpha(0xC0141625, a));
            int barW = Math.round((w - 12) * Mth.clamp((zoomTarget - 0.45f) / (1.9f - 0.45f), 0f, 1f));
            g.fill(x + 6, y + 13, x + 6 + barW, y + 14, OrbitaTheme.withAlpha(OrbitaTheme.ACCENT, a));
            g.drawString(font, OrbitaFonts.menu(s), x + 8, y + 3, OrbitaTheme.withAlpha(OrbitaTheme.TEXT, a), false);
        }
        if (hoverRocket > 0.05f && rocketMode != RocketMode.DRAG) {
            float sx = (rocketX - sceneCx) * zoom + sceneCx;
            float sy = (rocketY - sceneCy) * zoom + sceneCy;
            String s = "¡Arrástrame!";
            int w = font.width(OrbitaFonts.menu(s)) + 10;
            int x = Math.round(sx - w / 2f);
            int y = Math.round(sy - 26 * zoom - 6);
            UiDraw.roundedRect(g, x, y, w, 13, 6, OrbitaTheme.withAlpha(0xD0201A3A, hoverRocket));
            g.drawString(font, OrbitaFonts.menu(s), x + 5, y + 3, OrbitaTheme.withAlpha(0xFFFFD9A0, hoverRocket), false);
        }
    }

    private static final String[] NEWS = {
            "Planeta que gira y cohete que puedes arrastrar",
            "Rueda del ratón: acércate o aléjate",
            "Letras dispersas en 3D",
            "Módulos nuevos: Sesión, Luz y Mirando"
    };

    /** Tarjeta de cristal con las novedades y los módulos activos, abajo a la derecha. */
    private void renderNews(GuiGraphics g, int mouseX, int mouseY, float t) {
        int cw = 0;
        for (String n : NEWS) {
            cw = Math.max(cw, font.width(OrbitaFonts.menu(n)) + 28);
        }
        cw = Math.max(cw, font.width(OrbitaFonts.bold("Novedades " + OrbitaClient.version())) + 80);
        int ch = 26 + NEWS.length * 11;
        int cx = width - 8 - cw;
        int cy = height - 40 - ch;
        if (cy < height * 0.62f || cx < left + 170) {
            return; // pantalla demasiado pequeña: no tapar el planeta ni el menú
        }
        float time = dev.orbita.client.ui.anim.Animation.seconds();
        boolean hover = mouseX >= cx && mouseX < cx + cw && mouseY >= cy && mouseY < cy + ch;
        int slide = Math.round((1f - t) * 20f);
        cx += slide;
        UiDraw.roundedRect(g, cx - 1, cy - 1, cw + 2, ch + 2, 7, OrbitaTheme.withAlpha(hover ? 0x608B7CFF : 0x30FFFFFF, t));
        dev.orbita.client.ui.render.Fx.gradientCard(g, cx, cy, cw, ch, 6,
                OrbitaTheme.withAlpha(0xD0201A3A, t), OrbitaTheme.withAlpha(0xD00D0F17, t));
        dev.orbita.client.ui.render.Fx.sweep(g, cx, cy, cw, ch, (time * 0.18f) % 1.6f, OrbitaTheme.withAlpha(0x22FFFFFF, t));
        UiDraw.circle(g, cx + 10, cy + 10, 3, OrbitaTheme.withAlpha(0xFF7CFFB2, t * (0.6f + 0.4f * Mth.sin(time * 3f))));
        g.drawString(font, OrbitaFonts.bold("Novedades " + OrbitaClient.version()), cx + 18, cy + 6, OrbitaTheme.withAlpha(OrbitaTheme.TEXT, t), false);
        long on = dev.orbita.client.hud.HudManager.get().modules().stream().filter(m -> m.isEnabled()).count();
        String chip = on + " activos";
        int chipW = font.width(chip) + 10;
        UiDraw.roundedRect(g, cx + cw - chipW - 6, cy + 4, chipW, 12, 6, OrbitaTheme.withAlpha(0x408B7CFF, t));
        g.drawString(font, chip, cx + cw - chipW - 1, cy + 6, OrbitaTheme.withAlpha(0xFFE0DBFF, t), false);
        for (int i = 0; i < NEWS.length; i++) {
            int ly = cy + 21 + i * 11;
            UiDraw.circle(g, cx + 11, ly + 4, 1, OrbitaTheme.withAlpha(OrbitaTheme.ACCENT_2, t));
            g.drawString(font, OrbitaFonts.menu(NEWS[i]), cx + 17, ly, OrbitaTheme.withAlpha(OrbitaTheme.TEXT_MUTED, t), false);
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}
