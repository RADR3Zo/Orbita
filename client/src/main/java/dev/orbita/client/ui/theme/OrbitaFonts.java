package dev.orbita.client.ui.theme;

import dev.orbita.client.OrbitaClient;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;

/** Tipografía moderna del menú (Raleway, licencia OFL) con la fuente de Minecraft como respaldo. */
public final class OrbitaFonts {
    public static final FontDescription MENU = new FontDescription.Resource(OrbitaClient.id("menu"));
    public static final FontDescription MENU_BOLD = new FontDescription.Resource(OrbitaClient.id("menu_bold"));

    private OrbitaFonts() {
    }

    public static Component menu(String text) {
        return Component.literal(text).withStyle(s -> s.withFont(MENU));
    }

    public static Component bold(String text) {
        return Component.literal(text).withStyle(s -> s.withFont(MENU_BOLD));
    }
}
