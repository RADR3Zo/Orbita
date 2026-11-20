package dev.orbita.launcher.core;

/** Informe de progreso de tareas largas (instalación, descargas, inicio de sesión). */
public interface Progress {
    void status(String text);

    /** Fracción 0..1, o un valor negativo para "indeterminado". */
    void progress(double fraction);

    void log(String line);

    Progress CONSOLE = new Progress() {
        private String last = "";

        @Override
        public void status(String text) {
            if (!text.equals(last)) {
                last = text;
                System.out.println("[Órbita] " + text);
            }
        }

        @Override
        public void progress(double fraction) {
        }

        @Override
        public void log(String line) {
            System.out.println(line);
        }
    };
}
