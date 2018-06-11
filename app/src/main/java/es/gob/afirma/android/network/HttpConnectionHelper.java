package es.gob.afirma.android.network;

import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import es.gob.afirma.android.signfolder.SFConstants;

/**
 * Clase con metodos de ayuda para la conexi&oacute;n con servicios remotos.
 */
public class HttpConnectionHelper {

    private static final String HTTPS = "https"; //$NON-NLS-1$

    private static final boolean DEBUG = false;

    private HttpConnectionHelper() {
        // Evitamos que se instancie la clase
    }

    /**
     * Conecta con la URL indicada y obtiene el flujo de datos para su lectura.
     * @param url URL del servicio o contenido a recuperar.
     * @return Flujo para la lectura de los datos.
     * @throws IOException Cuando ocurre cualquier error durante la comunicaci&oacute;n.
     */
    public static InputStream preConnect(final String url) throws IOException {

        if (url.startsWith(HTTPS)) {
            try {
                AndroidUrlHttpManager.disableSslChecks();
            }
            catch(final Exception e) {
                Log.w(SFConstants.LOG_TAG,
                        "No se ha podido ajustar la confianza SSL, es posible que no se pueda completar la conexion: " + e //$NON-NLS-1$
                );
            }
        }

        if (DEBUG) {
            if (url.length() < 3500) {
                Log.i(SFConstants.LOG_TAG, " == URL: " + url);
            } else {
                int count = 0;
                Log.i(SFConstants.LOG_TAG, " == URL fragmentada: ");
                while (count < url.length()) {
                    Log.i(SFConstants.LOG_TAG, url.substring(count, Math.min(count + 3500, url.length())));
                    count += 3500;
                }
            }
        }

        final InputStream is = AndroidUrlHttpManager.getRemoteDataByPost(url);

        if (url.startsWith(HTTPS)) {
            AndroidUrlHttpManager.enableSslChecks();
        }

        return is;
    }

    /**
     * Conecta con la URL indicada y descarga su contenido.
     * @param url URL del servicio o contenido a recuperar.
     * @return Respuesta obtenida con los datos cargados.
     * @throws IOException Cuando ocurre cualquier error durante la comunicaci&oacute;n.
     */
    public static HttpResponse connect(final String url) throws IOException {

        final InputStream is = preConnect(url);
        byte[] response = readInputStream(is);
        is.close();

        if (DEBUG) {
            String xmlResponse = new String(response);
            if (xmlResponse.length() < 3500) {
                Log.i(SFConstants.LOG_TAG, " == Respuesta: " + xmlResponse);
            } else {
                int count = 0;
                Log.i(SFConstants.LOG_TAG, " == Respuesta fragmentada: ");
                while (count < xmlResponse.length()) {
                    Log.i(SFConstants.LOG_TAG, xmlResponse.substring(count, Math.min(count + 3500, xmlResponse.length())));
                    count += 3500;
                }
            }
        }

        HttpResponse httpResponse = new HttpResponse();
        httpResponse.setContent(response);

        return httpResponse;
    }

    private static byte[] readInputStream(InputStream is) throws IOException {

        int n;
        byte[] buffer = new byte[10240];
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        while ((n = is.read(buffer)) > 0) {
            baos.write(buffer, 0, n);
        }
        return baos.toByteArray();
    }
}
