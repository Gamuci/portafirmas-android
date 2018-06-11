package es.gob.afirma.android.signfolder;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import java.lang.ref.WeakReference;

import es.gob.afirma.android.signfolder.proxy.AppConfiguration;
import es.gob.afirma.android.signfolder.proxy.ProxyConnector;

/** Carga los datos remotos necesarios para la configuraci&oacute;n de la aplicaci&oacute;n. */
final class LoadConfigurationDataTask extends AsyncTask<Void, Void, AppConfiguration> {

	private final String certB64;
	private final String certAlias;
	private final ProxyConnector conn;
	private final WeakReference<Context> context;
	private final LoadConfigurationListener listener;
	private Throwable t = null;

	/**
	 * Crea la tarea para la carga de la configuraci&oacute;n de la aplicaci&oacute;n
	 * necesaria para su correcto funcionamiento.
	 * @param certB64 Certificado para la autenticaci&oacute;n de la petici&oacute;n.
	 * @param certAlias Alias del certificado para la autenticaci&oacute;n de la petici&oacute;n.
	 * @param conn Objeto para la comunicaci&oacute;n con el servicio proxy.
	 * @param context Contexto de la aplicaci&oacute;n.
	 * @param listener Manejador del resultado de la operaci&oacute;n.
	 */
	LoadConfigurationDataTask(final String certB64, final String certAlias,
                              final ProxyConnector conn, final Context context, final LoadConfigurationListener listener) {
		this.certB64 = certB64;
		this.certAlias = certAlias;
		this.conn = conn;
		this.context = new WeakReference<>(context);
		this.listener = listener;
	}

	@Override
	protected AppConfiguration doInBackground(final Void... args) {

		AppConfiguration config;
    	try {
    		config = this.conn.getApplicationList();
    	} catch (final Exception e) {
    		Log.w(SFConstants.LOG_TAG, "No se pudo obtener la lista de aplicaciones: " + e); //$NON-NLS-1$
    		config = null;
    		this.t = e;
    	}

		return config;
	}

	@Override
	protected void onPostExecute(final AppConfiguration appConfig) {

		// Agregamos la configuracion necesaria
		// Como primer elemento aparecera el elemento que desactiva el filtro
        if (this.context.get() != null) {
            if (appConfig != null) {
                appConfig.getAppIdsList().add(0, ""); //$NON-NLS-1$
                appConfig.getAppNamesList().add(0, this.context.get().getString(R.string.filter_app_all_request));
                this.listener.configurationLoadSuccess(appConfig, this.certB64, this.certAlias);
            }
            else {
                this.listener.configurationLoadError(this.t);
            }
        }
	}

	/** Interfaz con los metodos para gestionar los resultados de la carga de la
	 * configuraci&oacute;n de la aplicaci&oacute;n. */
	interface LoadConfigurationListener {

		void dismissDialog();

		void configurationLoadSuccess(AppConfiguration appConfig, String certB64, String certAlias);

		void configurationLoadError(Throwable t);
	}
}
