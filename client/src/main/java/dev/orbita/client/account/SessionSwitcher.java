package dev.orbita.client.account;

import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.yggdrasil.ProfileResult;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import dev.orbita.client.OrbitaClient;
import dev.orbita.client.mixin.MinecraftSessionAccessor;
import dev.orbita.client.ui.widget.AccountCard;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.gui.screens.social.PlayerSocialManager;
import net.minecraft.client.multiplayer.ProfileKeyPairManager;
import net.minecraft.client.multiplayer.chat.report.ReportEnvironment;
import net.minecraft.client.multiplayer.chat.report.ReportingContext;
import net.minecraft.util.Util;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Cambia la cuenta con la que se juega sin reiniciar Minecraft: usuario, perfil (skin), servicios de Mojang,
 * claves de chat firmado y reportes. Desde el menú principal (no dentro de un mundo).
 */
public final class SessionSwitcher {
    private SessionSwitcher() {
    }

    /**
     * Cambia a la cuenta guardada en ese índice. Si es premium y su sesión caducó, la renueva primero.
     * onDone recibe null si salió bien, o el mensaje de error. Todo vuelve al hilo del juego.
     */
    public static void switchTo(int index, Consumer<String> status, Consumer<String> onDone) {
        Minecraft mc = Minecraft.getInstance();
        Util.backgroundExecutor().execute(() -> {
            try {
                Accounts.Acc acc = Accounts.list().get(index);
                if (acc.premium() && acc.expired()) {
                    status.accept("Renovando la sesión de " + acc.name() + "…");
                    acc = MicrosoftLogin.refresh(acc, status);
                    Accounts.add(acc);
                }
                Accounts.Acc finalAcc = acc;
                UserApiService api = apiFor(finalAcc);
                mc.execute(() -> {
                    apply(mc, finalAcc, api);
                    Accounts.add(finalAcc); // la deja como activa también en el launcher
                    onDone.accept(null);
                });
            } catch (IOException | RuntimeException e) {
                String msg = e.getMessage() != null ? e.getMessage() : e.toString();
                mc.execute(() -> onDone.accept(msg));
            }
        });
    }

    private static UserApiService apiFor(Accounts.Acc acc) {
        if (!acc.premium()) {
            return UserApiService.OFFLINE;
        }
        try {
            return new YggdrasilAuthenticationService(Minecraft.getInstance().getProxy()).createUserApiService(acc.accessToken());
        } catch (Exception e) {
            OrbitaClient.LOG.warn("No se pudo preparar los servicios de Mojang para {}: {}", acc.name(), e.toString());
            return UserApiService.OFFLINE;
        }
    }

    private static void apply(Minecraft mc, Accounts.Acc acc, UserApiService api) {
        UUID id = parseUuid(acc.uuid());
        User user = new User(acc.name(), id, acc.premium() ? acc.accessToken() : "0",
                Optional.ofNullable(acc.xuid()).filter(x -> !x.isEmpty() && !"0".equals(x)), Optional.empty());
        MinecraftSessionAccessor s = (MinecraftSessionAccessor) mc;
        s.orbita$setUser(user);
        s.orbita$setUserApiService(api);
        s.orbita$setUserPropertiesFuture(CompletableFuture.supplyAsync(() -> {
            try {
                return api.fetchProperties();
            } catch (Exception e) {
                return UserApiService.OFFLINE_PROPERTIES;
            }
        }, Util.nonCriticalIoPool()));
        s.orbita$setProfileFuture(CompletableFuture.supplyAsync(() -> {
            ProfileResult r = mc.services().sessionService().fetchProfile(id, true);
            return r != null ? r : new ProfileResult(new com.mojang.authlib.GameProfile(id, acc.name()));
        }, Util.nonCriticalIoPool()));
        s.orbita$setPlayerSocialManager(new PlayerSocialManager(mc, api));
        s.orbita$setProfileKeyPairManager(ProfileKeyPairManager.create(api, user, mc.gameDirectory.toPath()));
        s.orbita$setReportingContext(ReportingContext.create(ReportEnvironment.local(), api));
        AccountCard.resetSkin(); // la cara de la tarjeta vuelve a buscar la skin de la cuenta nueva
        OrbitaClient.LOG.info("Cuenta cambiada a {} ({})", acc.name(), acc.premium() ? "premium" : "sin premium");
    }

    private static UUID parseUuid(String s) {
        String h = s.replace("-", "");
        if (h.length() == 32) {
            return UUID.fromString(h.substring(0, 8) + "-" + h.substring(8, 12) + "-" + h.substring(12, 16) + "-"
                    + h.substring(16, 20) + "-" + h.substring(20));
        }
        return UUID.nameUUIDFromBytes(s.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
