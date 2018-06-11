package es.gob.afirma.android.gcm;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;
import com.google.firebase.iid.FirebaseInstanceId;

import java.lang.ref.WeakReference;

import es.gob.afirma.android.signfolder.R;
import es.gob.afirma.android.signfolder.SFConstants;

/**
 * Tarea que recupera de Google el token para el env&iacute;o de notificaciones.
 * Esta tarea se ejecuta al inicio para tener el token necesario por si se habilitan
 * las notificaciones o, si ya estaban habilitadas, para comprobar si ha cambiado y
 * es necesario volver a registrarlo.
 */
public class RecoverNotificationTokenAsyncTask extends AsyncTask<Object, Object, String> {

    // TODO: Migrar por completo a FireBase. Hay que sustituir el uso de esta tarea por el
    // uso de una clase que extienda FirebaseInstanceId

    private final WeakReference<Context> context;
    private final RecoverNotificationTokenListener listener;

    public RecoverNotificationTokenAsyncTask(Context context, RecoverNotificationTokenListener listener) {
        this.context = context != null ? new WeakReference<>(context) : null;
        this.listener = listener;
    }

    @Override
    protected String doInBackground(Object[] objects) {

        String token = null;
        if (context != null && context.get() != null) {
            try {
                // Intentamos obtener el token de FireBase
                token = FirebaseInstanceId.getInstance().getToken();
                Log.i(SFConstants.LOG_TAG, "Token de FireBase: " + token);

                if (token == null) {
                    // Si no tenemos token de FireBase, probamos con el token antiguo.
                    InstanceID instanceID = InstanceID.getInstance(context.get());
                    token = instanceID.getToken(
                            context.get().getString(R.string.gcm_defaultSenderId),
                            GoogleCloudMessaging.INSTANCE_ID_SCOPE);
                }
            } catch (Exception e) {
                Log.w(SFConstants.LOG_TAG, "No se pudo obtener el token para notificaciones", e);
                token = null;
            }
        }
        else {
            Log.w(SFConstants.LOG_TAG, "Se ha perdido la referencia al contexto proporcionado para la recepcion del token de notificacion");
        }
        return token;
    }

    @Override
    protected void onPostExecute(String token) {
        if (this.listener != null) {
            this.listener.updateNotificationToken(token);
        }
    }

    /**
     * Interfaz con los m&eacute;todos para obtener el token de notificaci&oacute;n.
     */
    public interface RecoverNotificationTokenListener {
        /**
         * Informa del token para notificaciones.
         * @param token Token para notificaciones.
         */
        void updateNotificationToken(String token);
    }
}
