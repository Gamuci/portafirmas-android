package es.gob.afirma.android.signfolder.proxy;

/**
 * Clase que permite identificar si el usuario tiene habilitadas las notificaciones.
 */
public class NotificationState {

    /** Las notificaciones estan activadas. */
    public static final int STATE_ENABLED = 1;
    /** Las notificaciones estan desactivadas. */
    public static final int STATE_DISABLED = 2;
    /** No se conoce el estado de las notificaciones. */
    public static final int STATE_UNKNOWN = 3;

    private int state;

    private String error;

    /**
     * Crea el estado de las notificaciones.
     * @param state Estado en el que se encuentran las notificaciones.
     */
    NotificationState(int state) {
        this.state = state;
        this.error = null;
    }

    /**
     * Asigna el estado desconocido a las notificaciones cuando se produce un error.
     * @param  state Estado en el que se encuentran las notificaciones.
     * @param error Mensaje descriptivo del error.
     */
    NotificationState(int state, String error) {
        this.state = state;
        this.error = error;
    }

    /**
     * Obtiene el estado de las notificaciones.
     * @return Estado de las notificaciones.
     */
    public int getState() {
        return this.state;
    }

    /**
     * Obtiene el mensaje del error producido al detectar las notificaciones.
     * @return Mensaje del error producido o {@code null} si no se ha producido ning&uacute;n error.
     */
    public String getError() {
        return this.error;
    }
}
