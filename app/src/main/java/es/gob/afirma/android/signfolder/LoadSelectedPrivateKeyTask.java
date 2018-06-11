package es.gob.afirma.android.signfolder;

import android.content.Context;
import android.os.AsyncTask;
import android.security.KeyChain;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import es.gob.afirma.android.crypto.MobileKeyStoreManager.KeySelectedEvent;
import es.gob.afirma.android.crypto.MobileKeyStoreManager.PrivateKeySelectionListener;

final class LoadSelectedPrivateKeyTask extends AsyncTask<Void, Void, PrivateKey> {

	private final String selectedAlias;
	private final WeakReference<Context> context;
	private final PrivateKeySelectionListener listener;
	private X509Certificate[] certChain;
	private Throwable t;

	LoadSelectedPrivateKeyTask(final String certAlias, final PrivateKeySelectionListener listener, final Context context) {
		this.selectedAlias = certAlias;
		this.listener = listener;
		this.context = context != null ? new WeakReference<>(context) : null;
	}

	@Override
	protected PrivateKey doInBackground(final Void... params) {

		final PrivateKey pk;
		if (this.context != null && this.context.get() != null) {
			try {
				pk = KeyChain.getPrivateKey(this.context.get(), this.selectedAlias);
				this.certChain = KeyChain.getCertificateChain(this.context.get(), this.selectedAlias);
			} catch (final Exception e) {
				e.printStackTrace();
				this.t = e;
				return null;
			}
		}
		else {
			this.t = new IOException("Se ha perdido la referencia al contexto pora la carga de los certificados");
			pk = null;
		}

		return pk;
	}

	@Override
	protected void onPostExecute(final PrivateKey privateKey) {

		final KeySelectedEvent ksEvent;
		if (privateKey != null) {
			ksEvent = new KeySelectedEvent(new PrivateKeyEntry(privateKey, this.certChain), this.selectedAlias);
		}
		else {
			ksEvent = new KeySelectedEvent(this.t);
		}

		this.listener.keySelected(ksEvent);
	}
}
