package es.gob.afirma.android.gcm;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.SSLHandshakeException;

import es.gob.afirma.android.signfolder.AppPreferences;
import es.gob.afirma.android.signfolder.LoginActivity;
import es.gob.afirma.android.signfolder.PetitionListActivity;
import es.gob.afirma.android.signfolder.SFConstants;


/**
 * Created by sergio.martinez on 27/09/2017.
 */
public class StartFromNotificationActivity extends FragmentActivity {

    final static String EXTRA_RESOURCE_CERT_B64 = "es.gob.afirma.signfolder.cert"; //$NON-NLS-1$

    Thread thread = new Thread(new Runnable() {

        @Override
        public void run() {
            try  {
                URL url = new URL(AppPreferences.getInstance().getSelectedProxyUrl());
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                int code = connection.getResponseCode();

                if(code == 200) {
                    Intent openActivityIntent = new Intent(getApplicationContext(), PetitionListActivity.class);
                    openActivityIntent.putExtra(PetitionListActivity.EXTRA_RESOURCE_CERT_B64, getIntent().getStringExtra(EXTRA_RESOURCE_CERT_B64));
                    openActivityIntent.putExtra(PetitionListActivity.EXTRA_RESOURCE_OPEN_FROM_NOTIFICATION, true);
                    openActivityIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(openActivityIntent);
                }
            } catch (SSLHandshakeException e) {
                // Fallo por el protocolo SSL, pero tiene conexion
                Intent openActivityIntent = new Intent(getApplicationContext(), PetitionListActivity.class);
                openActivityIntent.putExtra(PetitionListActivity.EXTRA_RESOURCE_CERT_B64,  getIntent().getStringExtra(EXTRA_RESOURCE_CERT_B64));
                openActivityIntent.putExtra(PetitionListActivity.EXTRA_RESOURCE_OPEN_FROM_NOTIFICATION, true);
                openActivityIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(openActivityIntent);
            } catch (Exception e) {
                Log.e(SFConstants.LOG_TAG, "No se puede conectar con el portafirmas");
            }
            finish();
        }
    });

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.i(SFConstants.LOG_TAG, "Iniciamos desde una notificacion");

        // Se inicia la actividad de login y la de peticion de firma
        Intent notificationIntent = new Intent(getApplicationContext(), LoginActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(notificationIntent);
        try {
            thread.start();
        } catch (Exception e) {
            //Se queda en la pantalla de login
            Log.e(SFConstants.LOG_TAG, "No se puede conectar con el portafirmas");
        }
    }
}
