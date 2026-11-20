package dev.orbita.client.nova.module;

public enum Category {
    HUD("HUD"), UTILITY("Utility"), VISUAL("Visual"), EFFECTS("Effects"), SKY("Sky"), COSMETICS("Cosmetics"), ACTIVE("Active"), SETTINGS("Settings");

    public final String label;
    Category(String label) { this.label = label; }
}
