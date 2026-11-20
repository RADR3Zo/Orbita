package dev.orbita.client.mixin;

import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.yggdrasil.ProfileResult;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.gui.screens.social.PlayerSocialManager;
import net.minecraft.client.multiplayer.ProfileKeyPairManager;
import net.minecraft.client.multiplayer.chat.report.ReportingContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.concurrent.CompletableFuture;

/** Campos de la sesión de Minecraft que hay que cambiar para usar otra cuenta sin reiniciar el juego. */
@Mixin(Minecraft.class)
public interface MinecraftSessionAccessor {
    @Mutable
    @Accessor("user")
    void orbita$setUser(User user);

    @Mutable
    @Accessor("profileFuture")
    void orbita$setProfileFuture(CompletableFuture<ProfileResult> future);

    @Mutable
    @Accessor("userApiService")
    void orbita$setUserApiService(UserApiService service);

    @Mutable
    @Accessor("userPropertiesFuture")
    void orbita$setUserPropertiesFuture(CompletableFuture<UserApiService.UserProperties> future);

    @Mutable
    @Accessor("playerSocialManager")
    void orbita$setPlayerSocialManager(PlayerSocialManager manager);

    @Mutable
    @Accessor("profileKeyPairManager")
    void orbita$setProfileKeyPairManager(ProfileKeyPairManager manager);

    @Accessor("reportingContext")
    void orbita$setReportingContext(ReportingContext context);
}
