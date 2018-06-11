package es.gob.afirma.android.signfolder.proxy;

public class NotificationRegistryResult {

    private boolean active;

    private String error;

    public NotificationRegistryResult(boolean active) {
        this(active, null);
    }

    public NotificationRegistryResult(boolean active, String error) {
        this.active = active;
        this.error = error;
    }

    public boolean isActive() {
        return this.active;
    }

    public String getError() {
        return this.error;
    }
}
