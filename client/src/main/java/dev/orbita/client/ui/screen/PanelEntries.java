package dev.orbita.client.ui.screen;

import dev.orbita.client.config.OrbitaConfig;
import dev.orbita.client.hud.HudManager;
import dev.orbita.client.hud.HudModule;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Las tarjetas que muestra {@link ControlPanelScreen}: qué es cada una (módulo del HUD, zoom, freelook,
 * perfil o módulo de Nova), en qué categoría va y qué pasa al activarla o abrirla.
 *
 * <p>Separado de la pantalla para que ControlPanelScreen solo se ocupe de dibujar y de las opciones.
 * Para añadir una tarjeta nueva: crea aquí un {@link PanelCard} y regístralo en {@code ControlPanelScreen#init}.
 */
final class PanelEntries {
    private PanelEntries() {
    }

    enum PanelFilter {
        ALL("Todos"), HUD("HUD"), MECHANIC("Mecánicas"), COSMETIC("Cosméticos"), VISUAL("Visual"), PERF("Rendimiento"),
        OTHER("Otros");

        final String label;

        PanelFilter(String label) {
            this.label = label;
        }
    }

    /** Una tarjeta del menú: un módulo del HUD o una función (zoom, perfil). */
    interface PanelCard {
        String name();

        String description();

        String glyph();

        /** Objeto de Minecraft que se dibuja como icono de la tarjeta. */
        ItemStack icon();

        PanelFilter filter();

        boolean toggleable();

        boolean enabled();

        void setEnabled(boolean value);

        /** Abre las opciones o la pantalla asociada. */
        void open(ControlPanelScreen screen);

        /** Icono propio dibujado (ModIcons); null = el objeto de Minecraft. */
        default String iconKey() {
            return null;
        }
    }

    record ModuleEntry(HudModule module) implements PanelCard {
        public String iconKey() {
            return module.id();
        }

        public String name() {
            return module.name();
        }

        public String description() {
            return module.description();
        }

        public String glyph() {
            return module.glyph();
        }

        public ItemStack icon() {
            return module.iconStack();
        }

        public PanelFilter filter() {
            return module.category() == HudModule.Category.MECHANIC ? PanelFilter.MECHANIC : PanelFilter.HUD;
        }

        public boolean toggleable() {
            return true;
        }

        public boolean enabled() {
            return module.isEnabled();
        }

        public void setEnabled(boolean value) {
            module.setEnabled(value);
            HudManager.get().save();
        }

        public void open(ControlPanelScreen screen) {
            screen.showOptions(this);
        }
    }

    static final PanelCard ZOOM = new PanelCard() {
        public String name() {
            return "Zoom";
        }

        public String description() {
            return "Acerca la vista manteniendo una tecla";
        }

        public String glyph() {
            return "ZOOM";
        }

        public String iconKey() {
            return "zoom";
        }

        public ItemStack icon() {
            return ICON_ZOOM;
        }

        public PanelFilter filter() {
            return PanelFilter.MECHANIC;
        }

        public boolean toggleable() {
            return true;
        }

        public boolean enabled() {
            return OrbitaConfig.get().zoomEnabled;
        }

        public void setEnabled(boolean value) {
            OrbitaConfig.get().zoomEnabled = value;
            OrbitaConfig.save();
        }

        public void open(ControlPanelScreen screen) {
            screen.showOptions(this);
        }
    };

    private static final ItemStack ICON_ZOOM = new ItemStack(Items.SPYGLASS);
    private static final ItemStack ICON_PROFILE = new ItemStack(Items.ELYTRA);
    private static final ItemStack ICON_FREELOOK = new ItemStack(Items.ENDER_EYE);

    static final PanelCard FREELOOK = new PanelCard() {
        public String name() {
            return "Freelook";
        }

        public String description() {
            return "Mira alrededor sin girar a tu personaje";
        }

        public String glyph() {
            return "EYE";
        }

        public String iconKey() {
            return "freelook";
        }

        public ItemStack icon() {
            return ICON_FREELOOK;
        }

        public PanelFilter filter() {
            return PanelFilter.MECHANIC;
        }

        public boolean toggleable() {
            return true;
        }

        public boolean enabled() {
            return OrbitaConfig.get().freelookEnabled;
        }

        public void setEnabled(boolean value) {
            OrbitaConfig.get().freelookEnabled = value;
            OrbitaConfig.save();
        }

        public void open(ControlPanelScreen screen) {
            screen.showOptions(this);
        }
    };

    static final PanelCard PROFILE = new PanelCard() {
        public String name() {
            return "Perfil y capas";
        }

        public String description() {
            return "Tu jugador y el guardarropa";
        }

        public String glyph() {
            return "CAPA";
        }

        public String iconKey() {
            return "profile";
        }

        public ItemStack icon() {
            return ICON_PROFILE;
        }

        public PanelFilter filter() {
            return PanelFilter.OTHER;
        }

        public boolean toggleable() {
            return false;
        }

        public boolean enabled() {
            return true;
        }

        public void setEnabled(boolean value) {
        }

        public void open(ControlPanelScreen screen) {
            net.minecraft.client.Minecraft.getInstance().setScreen(new ProfileScreen(screen));
        }
    };

    /** Un módulo traído de Nova (cosméticos, cielos, video, modo ahorro, extras de PvP). */
    record NovaEntry(dev.orbita.client.nova.module.Module module, ItemStack icon) implements PanelCard {
        private static final java.util.Map<String, net.minecraft.world.item.Item> ICONS = java.util.Map.ofEntries(
                java.util.Map.entry("Wings", Items.PHANTOM_MEMBRANE), java.util.Map.entry("Cape", Items.PURPLE_BANNER),
                java.util.Map.entry("Halo", Items.GLOWSTONE_DUST), java.util.Map.entry("Crown", Items.GOLDEN_HELMET),
                java.util.Map.entry("Hat", Items.LEATHER_HELMET), java.util.Map.entry("Bandana", Items.RED_DYE),
                java.util.Map.entry("Pet", Items.BONE), java.util.Map.entry("Coins", Items.GOLD_INGOT),
                java.util.Map.entry("Trail", Items.BLAZE_POWDER), java.util.Map.entry("AuraRings", Items.END_CRYSTAL),
                java.util.Map.entry("GroundSigil", Items.ENCHANTING_TABLE), java.util.Map.entry("Emotes", Items.PLAYER_HEAD),
                java.util.Map.entry("SkyModule", Items.SUNFLOWER), java.util.Map.entry("VideoBackground", Items.PAINTING),
                java.util.Map.entry("UnlockFps", Items.REPEATER), java.util.Map.entry("FpsBoost", Items.REDSTONE),
                java.util.Map.entry("SmartFps", Items.OBSERVER), java.util.Map.entry("LiteMode", Items.FEATHER),
                java.util.Map.entry("NoParticles", Items.GUNPOWDER), java.util.Map.entry("NoClouds", Items.WHITE_WOOL),
                java.util.Map.entry("NoWeather", Items.WATER_BUCKET), java.util.Map.entry("CritEffects", Items.NETHERITE_SWORD),
                java.util.Map.entry("Nametags", Items.NAME_TAG), java.util.Map.entry("NameChanger", Items.OAK_SIGN),
                java.util.Map.entry("ClientSettings", Items.SOUL_LANTERN));

        static NovaEntry of(dev.orbita.client.nova.module.Module m) {
            return new NovaEntry(m, new ItemStack(ICONS.getOrDefault(m.getClass().getSimpleName(), Items.NETHER_STAR)));
        }

        private boolean saving() {
            return module instanceof dev.orbita.client.nova.module.ClientSettings;
        }

        public String name() {
            return saving() ? "Modo ahorro" : dev.orbita.client.nova.core.Lang.t(module.name);
        }

        public String description() {
            return saving() ? "Medio o Extra: más FPS con un toque y todo vuelve al apagarlo"
                    : dev.orbita.client.nova.core.Lang.desc(module.name, module.description);
        }

        public String glyph() {
            return "NOVA";
        }

        public String iconKey() {
            return module.getClass().getSimpleName();
        }

        public PanelFilter filter() {
            if (module instanceof dev.orbita.client.nova.module.Visual.NoParticles
                    || module instanceof dev.orbita.client.nova.module.Visual.NoClouds
                    || module instanceof dev.orbita.client.nova.module.Visual.NoWeather) {
                return PanelFilter.PERF;
            }
            return switch (module.category) {
                case COSMETICS -> PanelFilter.COSMETIC;
                case SKY, EFFECTS, VISUAL -> PanelFilter.VISUAL;
                case UTILITY -> module instanceof dev.orbita.client.nova.module.PvpExtras.NameChanger ? PanelFilter.VISUAL : PanelFilter.PERF;
                default -> PanelFilter.PERF;
            };
        }

        public boolean toggleable() {
            return !module.alwaysOn;
        }

        public boolean enabled() {
            return module.enabled;
        }

        public void setEnabled(boolean value) {
            module.setEnabled(value);
            saveNova();
        }

        public void open(ControlPanelScreen screen) {
            screen.showOptions(this);
        }
    }

    static void saveNova() {
        if (dev.orbita.client.nova.NovaClient.modules != null) {
            dev.orbita.client.nova.NovaClient.modules.save();
        }
    }
}
