package es.gob.afirma.android.gcm;

import android.app.IntentService;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.ads.identifier.AdvertisingIdClient;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;

import es.gob.afirma.android.signfolder.AppPreferences;
import es.gob.afirma.android.signfolder.R;
import es.gob.afirma.android.signfolder.SFConstants;
import es.gob.afirma.android.signfolder.proxy.CommManager;

public class RegistrationIntentService extends IntentService {

    private static final String TAG = RegistrationIntentService.class.getName();

    public static final String EXTRA_RESOURCE_CERT_B64 = "es.gob.afirma.signfolder.cert"; //$NON-NLS-1$
    public static final String EXTRA_RESOURCE_USER_PROXY_ID = "es.gob.afirma.signfolder.userproxy"; //$NON-NLS-1$
    public static final String EXTRA_RESOURCE_TOKEN = "es.gob.afirma.signfolder.token"; //$NON-NLS-1$
    public static final String EXTRA_RESOURCE_BACKGROUND_REGISTRY = "es.gob.afirma.signfolder.invisibleregistry"; //$NON-NLS-1$

    private String certB64 = null;
    private String userProxyId = null;

    private String token = null;

    public RegistrationIntentService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        try {
            this.certB64 = intent.getStringExtra(EXTRA_RESOURCE_CERT_B64);
            this.userProxyId = intent.getStringExtra(EXTRA_RESOURCE_USER_PROXY_ID);
            this.token = intent.getStringExtra(EXTRA_RESOURCE_TOKEN);

            // Comprobamos si este registro lo hacemos de forma invisible para el usuario (porque se
            // haya actualizado el token de notificaciones) o si es con su conocimiento.
            boolean backgroundOperation = intent.getBooleanExtra(EXTRA_RESOURCE_BACKGROUND_REGISTRY, false);

            // Notificamos el nuevo token al Portafirmas
            sendRegistrationToServer(token, backgroundOperation);

            // [END register_for_gcm]
        } catch (Exception e) {
            Log.e( SFConstants.LOG_TAG, "Error al recuperar el token de registro de Google", e);
            // If an exception happens while fetching the new token or updating our registration data
            // on a third-party server, this ensures that we'll attempt the update at a later time.
            noticeResult(false, false);
        }
    }

    /** Establece y notifica el resultado de la operacion de registro. */
    public void noticeResult(final boolean registered, final boolean backgroundOperation) {

        // Registramos el estado de las notificaciones para este usuario/proxy y, en caso positivo,
        // el token de notificacion
        if (this.userProxyId != null) {

            Log.i(SFConstants.LOG_TAG, "Activacion de las notificaciones para el usuario-portafirmas: " + registered);
            AppPreferences.getInstance().setPreferenceBool(
                    AppPreferences.PREFERENCES_KEY_PREFIX_NOTIFICATION_ACTIVE + this.userProxyId,
                    registered);

            if (registered && this.token != null) {
                AppPreferences.getInstance().setPreference(
                        AppPreferences.PREFERENCES_KEY_PREFIX_NOTIFICATION_TOKEN + this.userProxyId,
                        this.token);
            }
        }

        // Anuncinamos el resultado
        sendBroadcast(registered, backgroundOperation);
    }

    private void sendBroadcast (boolean success, boolean backgroundOperation){
        Intent intent = new Intent ("message"); //put the same message as in the filter you used in the activity when registering the receiver
        intent.putExtra("success", success);
        intent.putExtra("background", backgroundOperation);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    /**
     * Se solicita al Portafirmas el alta en el sistema de notificaciones.
     * @param token Nuevo token.
     */
    private void sendRegistrationToServer(String token, boolean backgroundOperation) {
        try {
            AdvertisingIdClient.Info deviceInfo = AdvertisingIdClient.getAdvertisingIdInfo(getApplicationContext());
            if (deviceInfo == null) {
                Log.w(SFConstants.LOG_TAG, "No se pudo obtener la informacion del dispositivo ni su token de notificacion");
                return;
            }
            final String androidId = deviceInfo.getId();
            //final String androidId = "" + android.provider.Settings.Secure.getString(getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
            Log.i(SFConstants.LOG_TAG, "Registramos el nuevo token para el envio de notificaciones: " + token);
            new RegisterSIMServiceTask(CommManager.getProxyConnector()).execute(this, token, androidId, this.certB64, backgroundOperation);
        }
        catch (Exception e) {
            Log.w(SFConstants.LOG_TAG, "Error durante el registro del token de notificacion", e);
        }
    }
}