package es.gob.afirma.android.signfolder;

import android.os.AsyncTask;
import android.util.Log;

import java.util.List;

import es.gob.afirma.android.signfolder.proxy.PartialSignRequestsList;
import es.gob.afirma.android.signfolder.proxy.ProxyConnector;
import es.gob.afirma.android.signfolder.proxy.SignRequest;

/** Tarea as&iacute;ncrona para la carga de peticiones de firma en una lista de peticiones.
 * @author Carlos Gamuci. */
final class LoadSignRequestsTask extends AsyncTask<Void, Void, PartialSignRequestsList> {

	private final String state;
	private final String[] filters;
	private final ProxyConnector conn;
	private final LoadSignRequestListener listener;
	private final int numPage;
	private final int pageSize;

	/**
	 * Codigo de error de autenticacion (perdida de sesion)
	 */
	private static final String AUTH_ERROR = "ERR-11"; //$NON-NLS-1$

	/**
	 * Crea la tarea asincrona para la carga de peticiones de firma.
	 * @param state Estado de las peticiones que se solicitan (pendiente, rechazadas o firmadas).
	 * @param numPage N6uacute;mero de la p&aacute;gina de peticiones que se desea ver.
	 * @param pageSize N&uacute;mero de peticiones pm&aacute;ximas por p&aacute;gina.
	 * @param filters Filtros que han de cumplir las peticiones.
	 * @param conn Objeto para la comunicaci&oacute;n con el servicio proxy.
	 * @param listener Manejador para el postproceso de las peticiones de firma cargadas.
	 */
	LoadSignRequestsTask(final String state, final int numPage, final int pageSize, final List<String> filters, final ProxyConnector conn, final LoadSignRequestListener listener) {
		this.state = state;
		this.filters = filters != null ? filters.toArray(new String[filters.size()]) : null;
		this.conn = conn;
		this.listener = listener;
		this.numPage = numPage;
		this.pageSize = pageSize;
	}

    @Override
    protected PartialSignRequestsList doInBackground(final Void... arg) {

    	// Aqui se carga la lista de peticiones de documentos
    	PartialSignRequestsList signRequests;
    	try {
    		signRequests = this.conn.getSignRequests(
    				this.state,
    				this.filters,
    				this.numPage,
    				this.pageSize);
    	}
    	catch (final Exception e) {
    		e.printStackTrace();
    		signRequests = null;
    		Log.e(SFConstants.LOG_TAG, "Ocurrio un error al recuperar las peticiones de firma: " + e); //$NON-NLS-1$
			// Si se ha perdido la sesion o el certificado de autenticacion no es valido vuelve a la pantalla de login
			if(e.getMessage().contains(AUTH_ERROR)) {
                this.listener.lostSession();
			}
    	}
    	catch (final Throwable t) {
    		t.printStackTrace();
    		signRequests = null;
    		Log.e(SFConstants.LOG_TAG, "Problema grave al listar las peticiones: " + t); //$NON-NLS-1$
    	}

    	return signRequests;
    }

    @Override
	protected void onPostExecute(final PartialSignRequestsList partialSignRequests) {

    	// Si se cancela la operacion, no se actualiza el listado
    	if (isCancelled()) {
    		return;
    	}

		if (partialSignRequests == null) {
			this.listener.errorLoadingSignRequest();
			return;
		}

		final int numPages = partialSignRequests.getTotalSignRequests() / this.pageSize +
				(partialSignRequests.getTotalSignRequests() % this.pageSize == 0 ? 0 : 1);
		this.listener.loadedSignRequest(partialSignRequests.getCurrentSignRequests(), this.numPage, numPages);
    }

    /** Interfaz que gestiona la respuesta a las solicitudes de carga de peticiones de firma. */
    interface LoadSignRequestListener {

    	/** Se ejecuta cuando las peticiones de firma se han cargado correctamente.
    	 * @param signRequests Peticiones de firma cargadas.
    	 * @param pageNumber N&uacute;mero de p&aacute;gina.
    	 * @param numPages N&uacute;mero total de p&aacute;ginas. */
    	void loadedSignRequest(List<SignRequest> signRequests, int pageNumber, int numPages);

    	/** Se ejecuta cuando ocurre un error durante la carga de las peticiones de firma. */
    	void errorLoadingSignRequest();

		/** Se ejecuta cuando se pierde la sesion con el portafirmas. */
		void lostSession();
	}
}
