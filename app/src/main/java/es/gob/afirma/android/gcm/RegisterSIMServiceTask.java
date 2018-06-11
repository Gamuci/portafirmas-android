package es.gob.afirma.android.gcm;

import android.os.AsyncTask;
import android.util.Log;

import es.gob.afirma.android.signfolder.SFConstants;
import es.gob.afirma.android.signfolder.proxy.NotificationState;
import es.gob.afirma.android.signfolder.proxy.ProxyConnector;

/**
 * Created by sergio.martinez on 17/08/2017.
 */

class RegisterSIMServiceTask extends AsyncTask<Object, Object, Void> {

    private ProxyConnector conn;

    RegisterSIMServiceTask(final ProxyConnector conn) {
        this.conn = conn;
    }

    @Override
    protected Void doInBackground(Object... params) {

        Boolean backgroundRegistry = (Boolean) params[4];
        RegistrationIntentService registrationService = (RegistrationIntentService) params[0];
        NotificationState result;
        try {
            result = this.conn.signOnNotificationService(
                    params[1].toString(), // Token GCM
                    params[2].toString(), // Id dispositivo
                    params[3].toString() // Certificado de usuario
            );
        } catch (Exception e) {
            Log.w(SFConstants.LOG_TAG, "Error al registrar la aplicacion en el sistema de notificaciones", e);
            registrationService.noticeResult(false, backgroundRegistry);
            return null;
        }

        boolean registered = result != null && result.getState() == NotificationState.STATE_ENABLED;
        if (!registered) {
            Log.w(SFConstants.LOG_TAG, "El portafirmas notifico un error en el registro en el sistema de notificaciones: "
                    + (result != null ? result.getError() : null));
        }

        registrationService.noticeResult(registered, backgroundRegistry);

        return null;
    }
}
