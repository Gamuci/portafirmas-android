package es.gob.afirma.android.signfolder;

import android.os.AsyncTask;
import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;

import es.gob.afirma.android.signfolder.proxy.ProxyConnector;

/** Carga los datos remotos necesarios para la configuraci&oacute;n de la aplicaci&oacute;n. */
final class LogoutRequestTask extends AsyncTask<Void, Void, Void> {

	private final ProxyConnector conn;

	/**
	 * Crea la tarea para la carga de la configuraci&oacute;n de la aplicaci&oacute;n
	 * necesaria para su correcto funcionamiento.
	 * @param conn Objeto para la comunicaci&oacute;n con el servicio proxy.
	 */
	LogoutRequestTask(ProxyConnector conn) {
		this.conn = conn;
	}

	class TaskKiller extends TimerTask {
		private AsyncTask<?, ?, ?> mTask;
		TaskKiller(AsyncTask<?, ?, ?> task) {
			this.mTask = task;
		}

		public void run() {
			mTask.cancel(true);
		}
	}

	@Override
	protected Void doInBackground(final Void... args) {
		// Se realiza la peticion para realizar el login
		// Se realiza la peticion para realizar el login
		Timer timer = new Timer();
		timer.schedule(new TaskKiller(this), 10000);
		try {
			this.conn.logoutRequest();
		} catch (final Exception e) {
			Log.w(SFConstants.LOG_TAG, "No se pudo realizar el logout: " + e); //$NON-NLS-1$
		}
		timer.cancel();
		return null;
	}
}
