package es.gob.afirma.android.signfolder;

import android.os.AsyncTask;
import android.util.Log;

import es.gob.afirma.android.signfolder.proxy.ProxyConnector;
import es.gob.afirma.android.signfolder.proxy.RequestResult;
import es.gob.afirma.android.signfolder.proxy.SignRequest;

/** Tarea as&iacute;ncrona para el rechazo de peticiones de firma. Despu&eacute;s
 * del rechazo actualiza la lista con las peticiones pendientes.
 * @author Carlos Gamuci */
final class RejectRequestsTask extends AsyncTask<Void, Void, RequestResult[]> {

	private final String[] requestIds;
	private final ProxyConnector conn;
	private final OperationRequestListener listener;
	private String reason;
	private Throwable t;

	/**
	 * Crea una tarea as&iacute;ncrona para el rechazo de peticiones.
	 * @param requests Listado de peticiones que se desean rechazar.
	 * @param conn Objeto para la comunicaci&oacute;n con el servicio proxy.
	 * @param listener Manejador que gestiona el comportamiento de la operaci&oacute;n al finalizar.
	 */
	 RejectRequestsTask(final SignRequest[] requests,
			 final ProxyConnector conn,
			 final OperationRequestListener listener,
			 final String reason) {
		this.requestIds = new String[requests.length];
		this.conn = conn;
		this.listener = listener;
		this.reason = reason;
		this.t = null;

		for (int i = 0; i < requests.length; i++) {
			this.requestIds[i] = requests[i].getId();
		}
	}

	/**
	 * Crea una tarea as&iacute;ncrona para el rechazo de una petici&oacute;n.
	 * @param requestId Identificador de la petici&oacute;n a rechazar.
	 * @param conn Objeto para la comunicaci&oacute;n con el servicio proxy.
	 * @param listener Manejador que gestiona el comportamiento de la operaci&oacute;n al finalizar.
	 */
	RejectRequestsTask(final String requestId,
			final ProxyConnector conn,
			final OperationRequestListener listener,
			final String reason) {
		this.requestIds = new String[] { requestId };
		this.conn = conn;
		this.listener = listener;
		this.reason = reason;
		this.t = null;
	}

    @Override
	protected RequestResult[] doInBackground(final Void... arg) {

        	// Enviamos la peticion de rechazo
    	RequestResult[] result = null;
        try {
			result = this.conn.rejectRequests(this.requestIds, this.reason);
		} catch (final Exception e) {
			Log.w(SFConstants.LOG_TAG, "Ocurrio un error en el rechazo de las solicitudes de firma: " + e); //$NON-NLS-1$
			this.t = e;
		}

        return result;
    }

    @Override
	protected void onPostExecute(final RequestResult[] rejectedRequests) {

    	if (rejectedRequests != null) {
    		for (final RequestResult rResult : rejectedRequests) {
    			if (rResult.isStatusOk()) {
    				this.listener.requestOperationFinished(OperationRequestListener.REJECT_OPERATION, rResult);
    			}
    			else {
    				this.listener.requestOperationFailed(OperationRequestListener.REJECT_OPERATION, rResult, this.t);
    			}
    		}
    	}
    	else {
    		this.listener.requestOperationFailed(OperationRequestListener.REJECT_OPERATION, null, this.t);
    	}
    }
}
