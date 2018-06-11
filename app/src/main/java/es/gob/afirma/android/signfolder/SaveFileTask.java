package es.gob.afirma.android.signfolder;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;

/** Tarea para descarga de fichero en segundo plano. */
public class SaveFileTask extends AsyncTask<Void, Void, File> {

	private final InputStream dataIs;
	private final String filename;
	private final boolean publicFile;
	private final SaveFileListener listener;
	private final WeakReference<Context> context;

	/** Crea una tarea para descarga de fichero en segundo plano.
	 * @param dataIs Flujo de lectura de los datos del fichero.
	 * @param filename Nombre del fichero a guardar.
	 * @param publicFile Indica si el fichero debe estar disponible para los usuarios y aplicaciones.
	 * @param listener Clase a la que notificar el sesultado de la tarea.
	 * @param context Contexto en el que se ejecuta la tarea.
	 */
	SaveFileTask(final InputStream dataIs, final String filename,
			final boolean publicFile, final SaveFileListener listener, final Context context) {
		this.dataIs = dataIs;
		this.filename = filename;
		this.publicFile = publicFile;
		this.listener = listener;
		this.context = context != null ? new WeakReference<>(context) : null;
	}

	@Override
	protected File doInBackground(final Void... arg0) {

		File outFile;
		if (this.publicFile) {
			int i = 0;
			// Nos aseguramos de no pisar un fichero existente
			do {
				outFile = new File(
					Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
					generateFileName(this.filename, i++)
				);
			} while (outFile.exists());

			Log.i(SFConstants.LOG_TAG, "Se intenta guardar en el directorio externo el fichero: " + outFile.getAbsolutePath()); //$NON-NLS-1$
		}
		else {
            if (this.context != null && this.context.get() != null) {
                Context currentContext = this.context.get();
                outFile = new File(
                        currentContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                        this.filename);

                Log.i(SFConstants.LOG_TAG, "Se intenta guardar de forma temporal el fichero: " + outFile); //$NON-NLS-1$
            }
			else {
				Log.w(SFConstants.LOG_TAG, "Se ha perdido el contexto y no se podra guardar el fichero en un directorio");
				outFile = null;
			}
		}

		// Si se definio el fichero de salida, lo guardamos
		if (outFile != null) {
            try {
                final FileOutputStream fos = new FileOutputStream(outFile);
                writeData(this.dataIs, fos);
                fos.close();
                this.dataIs.close();
            } catch (final Exception e) {
                Log.e(SFConstants.LOG_TAG, "Error al guardar el fichero en un directorio externo: " + e); //$NON-NLS-1$
                outFile = null;
            }
        }

		return outFile;
	}

	/**
	 * Escribe los datos de un flujo de entrada en uno de salida.
	 * @param is Flujo de entrada.
	 * @param os Flujo de salida.
	 * @throws IOException Cuando ocurre un error.
	 */
	private static void writeData(final InputStream is, final OutputStream os) throws IOException {
		int n;
		final byte[] buffer = new byte[1024];
		while ((n = is.read(buffer)) > 0) {
			os.write(buffer, 0, n);
		}
	}

	@Override
	protected void onPostExecute(final File result) {

		if (result == null) {
			this.listener.saveFileError(this.filename);
		}
		else {
			this.listener.saveFileSuccess(result);
		}
	}

	/**
	 * Genera un nombre de fichero agregando un indice al final del nombre propuesto. Si el
	 * &iacute;ndice es menor o igual a 0, se devuelve el nombre indicado.
	 * @param docName Nombre inicial del fichero.
	 * @param index &Iacute;ndice que agregar.
	 * @return Nombre generado.
	 */
	private static String generateFileName(final String docName, final int index) {
		if (index <= 0) {
			return docName;
		}

		final int lastDocPos = docName.lastIndexOf('.');
		if (lastDocPos == -1) {
			return docName + '(' + index + ')';
		}

		return docName.substring(0, lastDocPos) + '(' + index + ')' + docName.substring(lastDocPos);
	}

	interface SaveFileListener {

		void saveFileSuccess(File outputFile);

		void saveFileError(String filename);
	}
}