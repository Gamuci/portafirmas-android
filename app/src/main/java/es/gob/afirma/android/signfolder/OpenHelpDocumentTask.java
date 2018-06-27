package es.gob.afirma.android.signfolder;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.List;

import es.gob.afirma.android.network.HttpConnectionHelper;

/** Tarea as&iacute;ncrona para la descarga y apertura del documento de ayuda de la
 * aplicaci&oacuten. Si el documento ya se descargo previamente, se abrir&aacute; directamente. */
final class OpenHelpDocumentTask extends AsyncTask<Void, Void, File> {

	private static final String PDF_MIMETYPE = "application/pdf"; //$NON-NLS-1$
    private static final String PDF_NAME = "Manual_Portafirmas.pdf"; //$NON-NLS-1$

	private final WeakReference<FragmentActivity> activity;

	/**
	 * Crea una tarea as&iacute;ncrona para la descarga y apertura del documento de ayuda. En caso
	 * de que el documento estuviese ya descargado, lo abrir&iacute;a directamente.
	 * @param activity Actividad sobre la que mostrar las notificaciones.
	 */
	OpenHelpDocumentTask(final FragmentActivity activity) {
		this.activity = activity != null ? new WeakReference<>(activity) : null;
	}

	@Override
	protected File doInBackground(final Void... args) {

		// Cargamos la URL externa del documento de ayuda
		String helpUrl = AppPreferences.getInstance().getHelpUrl();
		String helpVersion = AppPreferences.getInstance().getHelpVersion();

		if (this.activity == null || this.activity.get() == null) {
			return null;
		}

		// Calculamos la ruta de guardado del documento de ayuda
		File helpFile = new File(
				this.activity.get().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
				PDF_NAME);

		// Comprobamos si existe
		boolean exist = helpFile.exists();

		// Si existe, leemos el numero de version de nuestra aplicacion, para comprobar si la
        // ayuda esta actualizada
        boolean updated;
        String appVersion;
        try {
            PackageInfo pInfo = this.activity.get().getPackageManager().getPackageInfo(this.activity.get().getPackageName(), 0);
            appVersion = Integer.toString(pInfo.versionCode);
            updated = helpVersion.equals(appVersion);
        }
        catch (Exception e) {
            Log.w(SFConstants.LOG_TAG, "No se pudo comprobar la version del documento de ayuda");
            updated = false;
            appVersion = null;
        }

		// Si no esta descargada la ayuda o si no esta actualizada, la descargamos
		try {
			if (!exist || !updated) {

				Log.i(SFConstants.LOG_TAG, "Descargamos el fichero de ayuda");

				InputStream docIs = HttpConnectionHelper.preConnect(helpUrl);
				FileOutputStream fos = new FileOutputStream(helpFile);

				int n;
				byte[] data = new byte[128 * 1024];
				while ((n = docIs.read(data)) > 0) {
					fos.write(data, 0, n);
				}
				docIs.close();
				fos.flush();
				fos.close();

				if (appVersion != null) {
                    AppPreferences.getInstance().setHelpVersion(appVersion);
                }
			}
		}
		catch (SecurityException e) {
			Log.e(SFConstants.LOG_TAG, "No se pudo acceder al fichero o comprobar su existencia: " + helpFile.toString(), e);
			helpFile = null;
		}
		catch (IOException e) {
			Log.e(SFConstants.LOG_TAG, "No se pudo descargar el fichero: " + helpFile.toString(), e);
			helpFile = null;
		}

		// Abrimos el documento
		return helpFile;
	}

	@Override
	protected void onPostExecute(final File helpFile) {

	    if (this.activity == null || this.activity.get() == null) {
	        Log.w(SFConstants.LOG_TAG, "Se ha perdido el contexto de ejecucion. No se abrira la ayuda");
	        return;
        }

		if (helpFile == null) {
            Log.w(SFConstants.LOG_TAG, "No se ha podido descargar el fichero de ayuda");
			Toast.makeText(this.activity.get(), "No se ha podido cargar el fichero de ayuda", Toast.LENGTH_SHORT).show();
			return;
		}

		viewPdf(helpFile, this.activity.get());
	}

	private void viewPdf (final File file, final FragmentActivity activity) {
		final String adobePackage = "com.adobe.reader"; //$NON-NLS-1$
		final String gdrivePackage = "com.google.android.apps.viewer"; //$NON-NLS-1$
		boolean isGdriveInstalled = false;


		Uri fileUri = FileProvider.getUriForFile(this.activity.get(), this.activity.get().getApplicationContext().getPackageName(), file);

		final Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setDataAndType(fileUri, PDF_MIMETYPE);
		intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

		final PackageManager pm = activity.getPackageManager();
		final List<ResolveInfo> list = pm.queryIntentActivities(intent, 0);
		if (list.isEmpty()) {
			Log.w(SFConstants.LOG_TAG, "No hay visor pdf instalado"); //$NON-NLS-1$
			new AlertDialog.Builder(activity)
					.setTitle(R.string.error)
					.setMessage(R.string.no_pdf_viewer_msg)
					.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(final DialogInterface dialog, final int which) {
							// Cerramos la ventana
						}
					})
					.create().show();
		}
		else {

			for (final ResolveInfo resolveInfo : list) {
				if (resolveInfo.activityInfo.name.startsWith(adobePackage)) {
					intent.setPackage(resolveInfo.activityInfo.packageName);
					activity.startActivity(intent);
					return;
				}
				else if (resolveInfo.activityInfo.name.startsWith(gdrivePackage)) {
					intent.setPackage(resolveInfo.activityInfo.packageName);
					isGdriveInstalled = true;
				}
			}

			if (isGdriveInstalled) {
				activity.startActivity(intent);
				return;
			}

			Log.i(SFConstants.LOG_TAG, "Ni Adobe ni Gdrive instalado"); //$NON-NLS-1$
			new AlertDialog.Builder(activity)
					.setTitle(R.string.aviso)
					.setMessage(R.string.no_adobe_reader_msg)
					.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(final DialogInterface dialog, final int which) {
							activity.startActivity(intent);
						}
					})
					.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(final DialogInterface dialog, final int which) {
							// Cerramos la ventana
						}
					})
					.create().show();
		}
	}
}
