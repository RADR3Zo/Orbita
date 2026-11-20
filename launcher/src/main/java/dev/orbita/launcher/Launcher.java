package dev.orbita.launcher;

import dev.orbita.launcher.core.LauncherConfig;
import dev.orbita.launcher.core.Progress;
import dev.orbita.launcher.core.auth.Account;
import dev.orbita.launcher.core.auth.MicrosoftAuth;
import dev.orbita.launcher.core.install.GameInstaller;
import dev.orbita.launcher.core.install.LaunchSpec;
import dev.orbita.launcher.core.launch.GameLauncher;
import dev.orbita.launcher.core.mods.ModManager;

import javax.swing.SwingUtilities;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/** Estado compartido del launcher: configuración, gestor de mods, cuenta y proceso del juego. */
public final class Launcher {
    public final LauncherConfig config = LauncherConfig.load();
    public final ModManager mods = new ModManager();
    public final ExecutorService worker = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "orbita-worker");
        t.setDaemon(true);
        return t;
    });

    private final List<Runnable> accountListeners = new CopyOnWriteArrayList<>();
    private final List<Runnable> modsListeners = new CopyOnWriteArrayList<>();
    private volatile Process game;
    private volatile boolean verifyNext;

    /** Cuenta con la que se juega (la elegida en la página Cuentas). */
    public synchronized Account account() {
        migrateAccounts();
        return config.accounts.get(config.activeAccount);
    }

    /** Pasa el formato viejo (un nombre + una cuenta de Microsoft) a la lista de cuentas. */
    private void migrateAccounts() {
        if (config.accounts == null) {
            config.accounts = new java.util.ArrayList<>();
        }
        if (config.accounts.isEmpty()) {
            String name = config.username != null && config.username.matches("[A-Za-z0-9_]{3,16}") ? config.username : "Jugador";
            config.accounts.add(Account.offline(name));
            if (config.account != null) {
                config.accounts.add(config.account);
                config.activeAccount = 1;
            }
            config.account = null;
        }
        config.activeAccount = Math.max(0, Math.min(config.accounts.size() - 1, config.activeAccount));
    }

    public synchronized java.util.List<Account> accounts() {
        migrateAccounts();
        return java.util.List.copyOf(config.accounts);
    }

    /** Añade una cuenta (si ya estaba, la actualiza) y la deja como activa. */
    public synchronized void addAccount(Account acc) {
        migrateAccounts();
        int i = indexOf(acc);
        if (i >= 0) {
            config.accounts.set(i, acc);
        } else {
            config.accounts.add(acc);
            i = config.accounts.size() - 1;
        }
        config.activeAccount = i;
        saveAccounts();
    }

    public synchronized void selectAccount(int index) {
        migrateAccounts();
        if (index >= 0 && index < config.accounts.size()) {
            config.activeAccount = index;
            saveAccounts();
        }
    }

    public synchronized void removeAccount(int index) {
        migrateAccounts();
        if (index < 0 || index >= config.accounts.size()) {
            return;
        }
        config.accounts.remove(index);
        if (config.accounts.isEmpty()) {
            config.accounts.add(Account.offline("Jugador"));
        }
        if (config.activeAccount >= index && config.activeAccount > 0) {
            config.activeAccount--;
        }
        saveAccounts();
    }

    /** Cambia el nombre de la cuenta sin premium activa. */
    public synchronized void renameActiveOffline(String name) {
        migrateAccounts();
        if (!account().isMicrosoft()) {
            config.accounts.set(config.activeAccount, Account.offline(name));
            config.username = name;
        }
    }

    private int indexOf(Account acc) {
        for (int i = 0; i < config.accounts.size(); i++) {
            Account a = config.accounts.get(i);
            if (a.type().equals(acc.type()) && (a.isMicrosoft() ? a.uuid().equals(acc.uuid()) : a.name().equalsIgnoreCase(acc.name()))) {
                return i;
            }
        }
        return -1;
    }

    private void saveAccounts() {
        Account a = config.accounts.get(config.activeAccount);
        if (!a.isMicrosoft()) {
            config.username = a.name();
        }
        config.save();
        fireAccountChanged();
    }

    /** Vuelve a leer las cuentas de launcher.json (el juego también las cambia desde su menú de cuentas). */
    public synchronized void reloadAccounts() {
        LauncherConfig disk = LauncherConfig.load();
        if (disk.accounts != null && !disk.accounts.isEmpty()) {
            config.accounts = disk.accounts;
            config.activeAccount = disk.activeAccount;
            config.username = disk.username;
            config.account = null;
            fireAccountChanged();
        }
    }

    /** Lo pone la ventana: abre una página por su nombre ("accounts", "perf"...). */
    public volatile java.util.function.Consumer<String> navigate = s -> {
    };

    public void onAccountChanged(Runnable r) {
        accountListeners.add(r);
    }

    public void onModsChanged(Runnable r) {
        modsListeners.add(r);
    }

    public void fireAccountChanged() {
        accountListeners.forEach(SwingUtilities::invokeLater);
    }

    public void fireModsChanged() {
        modsListeners.forEach(SwingUtilities::invokeLater);
    }

    public boolean gameRunning() {
        Process p = game;
        return p != null && p.isAlive();
    }

    public void requestVerify() {
        verifyNext = true;
    }

    /** Instala lo que falte (Minecraft, Fabric, mods) y arranca el juego. Bloqueante: llamar desde un hilo de trabajo. */
    public Process launch(Progress p) throws IOException {
        Account acc = account();
        if (acc.isMicrosoft() && acc.expired()) {
            p.status("Renovando la sesión de Microsoft…");
            acc = new MicrosoftAuth(config.microsoftClientId).refresh(acc, p);
            addAccount(acc);
        }

        LaunchSpec spec = new GameInstaller(verifyNext).install(p);
        verifyNext = false;

        p.progress(-1);
        p.status("Preparando mods…");
        mods.migrateIfNeeded(p);
        mods.quarantineIncompatible(p);
        mods.ensureCore(p);
        if (config.installPerformancePack || config.performancePackVersion < ModManager.PACK_VERSION) {
            p.status("Instalando el pack de rendimiento…");
            mods.installPerformancePack(installedProjects(), p);
            config.installPerformancePack = false;
            config.performancePackVersion = ModManager.PACK_VERSION;
            config.save();
        }
        fireModsChanged();

        p.status("Iniciando Minecraft…");
        game = GameLauncher.launch(spec, acc, config, p::log);
        return game;
    }

    /** IDs de proyecto de Modrinth de los mods instalados (vacío si no hay conexión). */
    public Set<String> installedProjects() {
        try {
            return mods.scan(true).stream()
                    .map(m -> m.projectId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toCollection(HashSet::new));
        } catch (IOException e) {
            return new HashSet<>();
        }
    }
}
