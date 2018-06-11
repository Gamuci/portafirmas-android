package es.gob.afirma.android.gcm;

/*
 * Copyright 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.util.List;

import es.gob.afirma.android.signfolder.AppPreferences;
import es.gob.afirma.android.signfolder.LoginActivity;
import es.gob.afirma.android.signfolder.R;
import es.gob.afirma.android.signfolder.SFConstants;

import static es.gob.afirma.android.gcm.CommonUtilities.SENDER_ID;
import static es.gob.afirma.android.gcm.CommonUtilities.displayMessage;

/**
 * {@link IntentService} responsible for handling GCM messages.
 */
public class GCMIntentService extends GCMBaseIntentService {

    public static final String KEY_COUNT = "notificationCount";
    public static final String NOTIF_ID = "notificationId";
    final static String EXTRA_RESOURCE_CERT_B64 = "es.gob.afirma.signfolder.cert"; //$NON-NLS-1$

    private static final String SEPARATOR = "\\$\\$";

    public GCMIntentService() {
        super(SENDER_ID);
    }

    @Override
    protected void onRegistered(Context context, String registrationId) {
        Log.i(SFConstants.LOG_TAG, "Dispositivo registrado: regId = " + registrationId);
        displayMessage(context, getString(R.string.request_notification));
        ServerUtilities.register(context, registrationId);
    }

    @Override
    protected void onUnregistered(Context context, String registrationId) {
        Log.i(SFConstants.LOG_TAG, "Dispositivo desregistradoDevice unregistered");
        displayMessage(context, getString(R.string.request_notification));
        if (GCMRegistrar.isRegisteredOnServer()) {
            ServerUtilities.unregister(context, registrationId);
        } else {
            // This callback results from the call to unregister made on
            // ServerUtilities when the registration to the server failed.
            Log.i(SFConstants.LOG_TAG, "Se ignora la callback para el desregistro");
        }
    }

    @Override
    protected void onMessage(Context context, Intent intent) {
        Log.i(SFConstants.LOG_TAG, "Mensaje recibido");
        String url = null;
        String dni = null;
        String op = null;
        Bundle data = intent.getExtras();

        // TODO: Se verifica que las notificaciones esten activadas para el usuario y proxy concreto
        // para mostrarla solo en caso afirmativo

        // Se obtienen los 3 parametros obtenidos: url + dni + codigoOperacion
        if(data != null) {

            String body = (String) data.get("gcm.notification.body");
            if (body != null) {

                String[] param = body.split(SEPARATOR);
                if(param.length == 3) {
                    url = param[0];
                    dni = param[1];
                    op = param[2];
                }
            }
        }

        // Los 3 campos son obligatorios para gestionar la aplicacion
        if (url == null || dni == null || op == null) {
            return;
        }

        // Inicializamos las preferencias de la aplicacion, ya que no lo estaran si esta se arranca
        // directamente desde la notificacion
        AppPreferences prefs = AppPreferences.getInstance();
        prefs.init(getApplicationContext());

        // Se guarda en el fichero de preferencias un contador de notificaciones para apilarlas en una sola
        int currentCount = prefs.getPreferenceInt(KEY_COUNT + url, 0);
        currentCount = currentCount + 1;
        prefs.setPreferenceInt(KEY_COUNT + url, currentCount);

        // Gets an instance of the NotificationManager service
        String message;
        if(currentCount != 1) {
            message = context.getString(R.string.requests_notification, currentCount);
        }
        else {
            message = context.getString(R.string.request_notification);
        }

        // Verifica que el servidor proxy obtenido esta configurado en la aplicacion
        final List<String> servers = prefs.getServersList();
        if (servers.isEmpty()) {
            Log.w(SFConstants.LOG_TAG, "La lista de servidores esta vacia. Se ignora la notificacion del servidor: " + url);
            return;
        }

        // Se obtienen los alias de todos los servidores
        CharSequence[] aliases = servers.toArray(new CharSequence[servers.size()]);

        // Se recorren todos los servidores
        int selectedServer = 0;
        boolean found = false;
        while (selectedServer < aliases.length && !found) {
            // Se obtiene la URL para cada alias
            String serverURL = prefs.getServer(aliases[selectedServer].toString());
            // Si la URL del servidor configurado coincide con el que se envia en la
            // notificacion lo seleccionamos como proxy por defecto
            if (serverURL.equalsIgnoreCase(url)) {
                prefs.setSelectedProxy(
                        aliases[selectedServer].toString(),
                        serverURL);
                found = true;
            }
            selectedServer++;
        }

        // Preparamos la notificacion
        NotificationManager mNotifyMgr =
                (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);

        // Creacion de la notificacion a mostrar
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
                .setContentTitle(aliases[selectedServer - 1].toString())
                .setDefaults(Notification.DEFAULT_ALL)
                .setContentText(message);

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mBuilder.setSmallIcon(R.mipmap.ic_launcher_foreground);
            mBuilder.setColor(getResources().getColor(R.color.ic_launcher_background));
        } else {
            mBuilder.setSmallIcon(R.mipmap.ic_launcher);
        }

        // La prioridad se puede configurar a partir de JellyBean
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN){
            mBuilder.setPriority(Notification.PRIORITY_HIGH);
        }

        // Si se encuentra el proxy entre los listados, se lanza la notificacion y se intenta
        // cargar el listado de peticiones (si la sesion estaba ya iniciada se cargaran correctamente)
        if (found) {
            Intent notificationIntent = new Intent(context, StartFromNotificationActivity.class);
            String lastCertUsed = prefs.getLastCertificate();
            notificationIntent.putExtra(EXTRA_RESOURCE_CERT_B64, lastCertUsed);
            PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
                    notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);
            mBuilder.setContentIntent(contentIntent);
            mBuilder.setAutoCancel(true);
            // Obtenemos el identificador de la notificacion para borrarla y reeplazarla por la nueva
            int notId = prefs.getPreferenceInt(NOTIF_ID + url, 0);
            if (notId == 0) {
                //Se guarda el nuevo identificador de la notificacion en preferencias
                notId = (int) System.currentTimeMillis();
                prefs.setPreferenceInt(NOTIF_ID + url, notId);
            }

            // Eliminamos la notificacion anterior. Esto se usa, por ejemplo, para borrar
            // "2 peticiones pendientes" y escribir "3 peticiones pendientes"
            if (mNotifyMgr != null) {
                mNotifyMgr.cancel(notId);
                mNotifyMgr.notify(notId, mBuilder.build());
            }
        }
        else {
            // Si no es un servidor configurado no se cuentan ni se muestran las notificaciones
            Log.w(SFConstants.LOG_TAG, "Se ha recibido una notificacion de un servidor no registrado: " + url);
            prefs.setPreferenceInt(KEY_COUNT + url, currentCount - 1);
        }
    }

    @Override
    protected void onDeletedMessages(Context context, int total) {
        Log.i(SFConstants.LOG_TAG, "Recibida notificacion de borrado de mensajes");
        String message = getString(R.string.requests_notification, total);
        displayMessage(context, message);
        // notifies user
        generateNotification(context, message);
    }

    @Override
    public void onError(Context context, String errorId) {
        Log.i(SFConstants.LOG_TAG, "Error recibido: " + errorId);
        displayMessage(context, getString(R.string.notification_error, errorId));
    }

    @Override
    protected boolean onRecoverableError(Context context, String errorId) {
        // log message
        Log.i(SFConstants.LOG_TAG, "Error recuperable recibido: " + errorId);
        displayMessage(context, getString(R.string.notification_recoverable_error, errorId));
        return super.onRecoverableError(context, errorId);
    }

    /**
     * Issues a notification to inform the user that server has sent a message.
     */
    public static void generateNotification(Context context, String message) {

        Log.i(SFConstants.LOG_TAG, "Generando notificacion");

        int icon = R.drawable.arrow_first;
        long when = System.currentTimeMillis();
        NotificationManager notificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification = new Notification(icon, message, when);
        Intent notificationIntent = new Intent(context, LoginActivity.class);
        // set intent so it does not start a new activity
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_SINGLE_TOP);
        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        if (notificationManager != null) {
            notificationManager.notify(0, notification);
        }
    }
}
