package es.gob.afirma.android.signfolder;

import android.os.AsyncTask;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeoutException;

import es.gob.afirma.android.signfolder.proxy.CommManager;

/** Configura el conector apropiado para la conexi&oacute;n con el proxy. */
final class ConfigureProxyConnectorTask extends AsyncTask<Void, Void, Throwable> {

    private byte[] certEncoded;
	private ConfigureProxyConnectorTask.ConnectorListener listener;

    ConfigureProxyConnectorTask(byte[] certEncoded, ConnectorListener listener) {
        this.certEncoded = certEncoded;
	    this.listener = listener;
    }

	@Override
	protected Throwable doInBackground(final Void... args) {

	    Throwable t = null;

	    // Se realiza la peticion para realizar el login
        Timer timer = new Timer();
		timer.schedule(new TaskKiller(this, this.listener), 10000);

		try {
			CommManager.configureConnector(this.certEncoded);
		}
		catch (Exception e) {
			t = e;
		}

		timer.cancel();
		return t;
	}

	@Override
	protected void onPostExecute(final Throwable t) {
		if (t == null) {
			this.listener.configurationSuccess();
		}
		else {
			this.listener.configurationError(t);
		}
	}

    class TaskKiller extends TimerTask {
        private AsyncTask<?, ?, ?> mTask;
        private ConnectorListener listener;
        TaskKiller(AsyncTask<?, ?, ?> task, ConnectorListener listener) {
            this.mTask = task;
            this.listener = listener;
        }

        public void run() {
            this.mTask.cancel(true);
            this.listener.configurationError(new TimeoutException("Se ha excedido el tiempo de conexion con el proxy"));
        }
    }

	public interface ConnectorListener {
		void configurationSuccess();
		void configurationError(Throwable t);
	}
}
