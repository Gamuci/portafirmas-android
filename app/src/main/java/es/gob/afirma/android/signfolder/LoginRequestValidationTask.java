package es.gob.afirma.android.signfolder;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.security.KeyStore;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeoutException;

import es.gob.afirma.android.crypto.AOPkcs1Signer;
import es.gob.afirma.android.signfolder.proxy.ProxyConnector;
import es.gob.afirma.android.signfolder.proxy.RequestResult;
import es.gob.afirma.android.util.Base64;

/** Carga los datos remotos necesarios para la configuraci&oacute;n de la aplicaci&oacute;n. */
final class LoginRequestValidationTask extends AsyncTask<Void, Void, Boolean> {

	private final String certB64;
	private final String certAlias;
	private final ProxyConnector conn;
	private final WeakReference<Context> context;
	private final KeyStore.PrivateKeyEntry privateKeyEntry;
	private Throwable t;
	public Timer timer = null;

	private LoadConfigurationDataTask.LoadConfigurationListener listener;

	class TaskKiller extends TimerTask {
		private AsyncTask<?, ?, ?> mTask;
		LoadConfigurationDataTask.LoadConfigurationListener listener;
		TaskKiller(AsyncTask<?, ?, ?> task, LoadConfigurationDataTask.LoadConfigurationListener listener) {
			this.mTask = task;
			this.listener = listener;
		}

		public void run() {
			mTask.cancel(true);
			this.listener.configurationLoadError(new TimeoutException("Se ha excedido el tiempo de conexion con el proxy"));
		}
	}

	/**
	 * Crea la tarea para la carga de la configuraci&oacute;n de la aplicaci&oacute;n
	 * necesaria para su correcto funcionamiento.
	 * @param certB64 Certificado para la autenticaci&oacute;n de la petici&oacute;n.
	 * @param conn Objeto para la comunicaci&oacute;n con el servicio proxy.
	 * @param context Contexto de la aplicaci&oacute;n.
	 * @param listener Manejador del resultado de la operaci&oacute;n.
	 * @param pke Clave privada del almac&eacute;n de claves.
	 */
	LoginRequestValidationTask(final String certB64, final String certAlias,
							   final ProxyConnector conn, final Context context,
							   final LoadConfigurationDataTask.LoadConfigurationListener listener,
							   final KeyStore.PrivateKeyEntry pke) {
		this.certB64 = certB64;
		this.certAlias = certAlias;
		this.conn = conn;
		this.context = context != null ? new WeakReference<>(context) : null;
		this.listener = listener;
		this.privateKeyEntry = pke;
	}

	@Override
	protected Boolean doInBackground(final Void... args) {

		// Si el conector no requiere login, obviamos este proceso
		if (!this.conn.needLogin()) {
			return Boolean.TRUE;
		}

		// Se realiza la peticion para realizar el login
		timer = new Timer();
		timer.schedule(new TaskKiller(this, this.listener), 10000);
		t = null;
		boolean validLogin = false;
		try {
			// Solicitud del token de inicio
			RequestResult token = this.conn.loginRequest();
			if (!token.isStatusOk()) {
				throw new IllegalArgumentException("Se ha producido un error al pedir el login");
			}

			// Firma del token
			final AOPkcs1Signer signer = new AOPkcs1Signer();
			final byte[] pkcs1 = signer.sign(Base64.decode(token.getId()), "SHA256withRSA", //$NON-NLS-1$
					privateKeyEntry.getPrivateKey());

			// Se realiza la firma del token y se envia
			validLogin = this.conn.tokenValidation(pkcs1, this.certB64);
			if(!validLogin) {
				t = new Exception("El proxy ha denegado el acceso");
			}
		} catch (IllegalArgumentException e) {
			Log.w(SFConstants.LOG_TAG, "Login no necesario: Se trabaja con una version antigua del portafirmas: " + e); //$NON-NLS-1$
		} catch (final Exception e) {
			Log.w(SFConstants.LOG_TAG, "No se pudo realizar el login: " + e); //$NON-NLS-1$
		}
		timer.cancel();
		return validLogin;
	}

	@Override
	protected void onPostExecute(final Boolean validLogin) {
		if (validLogin) {
		    if (this.context != null && this.context.get() != null) {
                final LoadConfigurationDataTask lcdt = new LoadConfigurationDataTask(
                        this.certB64,
                        this.certAlias,
                        this.conn,
                        this.context.get(),
                        this.listener);
                lcdt.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
		}
		else {
			this.listener.configurationLoadError(this.t);
		}
	}
}
