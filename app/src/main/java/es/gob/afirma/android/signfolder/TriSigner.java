package es.gob.afirma.android.signfolder;

import android.util.Log;

import org.xml.sax.SAXException;

import java.io.IOException;
import java.security.PrivateKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import es.gob.afirma.android.crypto.AOPkcs1Signer;
import es.gob.afirma.android.signfolder.proxy.ProxyConnector;
import es.gob.afirma.android.signfolder.proxy.RequestResult;
import es.gob.afirma.android.signfolder.proxy.SignRequest;
import es.gob.afirma.android.signfolder.proxy.TriphaseRequest;
import es.gob.afirma.android.signfolder.proxy.TriphaseSignDocumentRequest;
import es.gob.afirma.android.signfolder.proxy.TriphaseSignDocumentRequest.TriphaseConfigData;
import es.gob.afirma.android.util.AOException;

/** Firmador trif&aacute;sico.
 * @author Tom&aacute;s Garc&iacute;a-Mer&aacute;s */
final class TriSigner {

	/** Firma de forma trif&aacute;sica una petici&oacute;n de firma.
	 * @param request Petici&oacute;n de firma.
	 * @param pk Clave privada para al firma.
	 * @param certificateChain Cadena de certificaci&oacute;n del certificado de firma.
	 * @param conn Objeto para la comunicaci&oacute;n con el servicio proxy.
	 * @return Resultado de la operaci&oacute;n.
	 * @throws CertificateEncodingException Cuando hay un error en la codificaci&oacute;n del certificado.
	 * @throws IOException Cuando hay un error de lectura/escritura de datos.
	 * @throws SAXException Cuando el XML est&aacute; mal formado. */
	static RequestResult sign(final SignRequest request, final PrivateKey pk,
			final X509Certificate[] certificateChain, final ProxyConnector conn)
			throws CertificateException, IOException, SAXException {

		// *****************************************************************************************************
		// **************************** PREFIRMA ***************************************************************
		//******************************************************************************************************

		Log.i(SFConstants.LOG_TAG, "TriSigner - sign: == PREFIRMA =="); //$NON-NLS-1$

		//Log.i(SFConstants.LOG_TAG, " ======== Parametros prefirma: " + request.getDocs()[0].getParams());

		// Mandamos a prefirmar y obtenemos los resultados
		final TriphaseRequest[] signRequests = signPhase1(request, conn);

		// *****************************************************************************************************
		// ******************************* FIRMA ***************************************************************
		//******************************************************************************************************

		//Log.i(SFConstants.LOG_TAG, " ======== Parametros resultado prefirma: " + signRequest[0].getDocumentsRequests()[0].getParams());

		Log.i(SFConstants.LOG_TAG, "TriSigner - sign: == FIRMA =="); //$NON-NLS-1$

		// Recorremos las peticiones de firma
		for (TriphaseRequest signRequest : signRequests) {
			// Si fallo una sola firma de la peticion, esta es erronea al completo
			if (!signRequest.isStatusOk()) {
				Log.w(SFConstants.LOG_TAG, "Se encontro prefirma erronea, se aborta el proceso de firma. La traza de la excepcion es: " + signRequest.getException()); //$NON-NLS-1$
				return new RequestResult(request.getId(), false);
			}

			// Recorremos cada uno de los documentos de cada peticion de firma
			for (final TriphaseSignDocumentRequest docRequests : signRequest.getDocumentsRequests()) {
				// Firmamos las prefirmas y actualizamos los parciales de cada documento de cada peticion
				try {
					signPhase2(docRequests, pk);
				}
				catch(final Exception e) {
					Log.w(SFConstants.LOG_TAG, "Error en la fase de FIRMA: " + e); //$NON-NLS-1$
					e.printStackTrace();

					// Si un documento falla en firma toda la peticion se da por fallida
					return new RequestResult(request.getId(), false);
				}
			} // Documentos de peticion
		} // Peticiones

		// *****************************************************************************************************
		// **************************** POSTFIRMA **************************************************************
		//******************************************************************************************************

		Log.i(SFConstants.LOG_TAG, "TriSigner - sign: == POSTFIRMA =="); //$NON-NLS-1$

		//Log.i(SFConstants.LOG_TAG, " ======== Parametros postfirma: " + request.getDocs()[0].getParams());

		// Mandamos a postfirmar y recogemos el resultado
		return signPhase3(signRequests, conn);
	}

	/**
	 * Obtiene el nombre de un algoritmo de firma que usa un algoritmo de huella espec&iacute;fico.
	 * @param mdAlgorithm Algoritmo de huella digital.
	 * @return Algoritmo de firma que utiliza el algoritmo de huella digital indicado.
	 */
	private static String getSignatureAlgorithm(final String mdAlgorithm) {
		return mdAlgorithm.replace("-", "") + "withRSA"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	/**
	 * Genera la prefirma de una petici&oacute;n de firma.
	 * @param request Petici&oacute;n de firma.
	 * @param conn Objeto para la comunicaci&oacute;n con el servicio proxy.
	 * @throws CertificateEncodingException Cuando hay un error en la codificaci&oacute;n del certificado.
	 * @throws IOException Cuando hay un error de lectura/escritura de datos.
	 * @throws SAXException Cuando el XML est&aacute; mal formado.
	 */
	private static TriphaseRequest[] signPhase1(final SignRequest request, final ProxyConnector conn)
			throws CertificateException, SAXException, IOException {
		return conn.preSignRequests(request);
	}

	/**
	 * Genera la firma PKCS#1 (segunda fase del proceso de firma trif&aacute;sica) y muta el objeto
	 * de petici&oacute;n de firma de un documento para almacenar el mismo el resultado.
	 * @param docRequest Petici&oacute;n de firma de un documento.
	 * @param key Clave privada de firma.
	 * @throws AOException Cuando ocurre alg&uacute;n error al generar el PKCS#1 o falta alg&uacute;n
	 * par&aacute;metro obligatorio dentro del resultado de prefirma de alguno de los documentos.
	 */
	private static void signPhase2(final TriphaseSignDocumentRequest docRequest, final PrivateKey key) throws AOException {

		final String signatureAlgorithm = getSignatureAlgorithm(docRequest.getMessageDigestAlgorithm());

		final TriphaseConfigData config = docRequest.getPartialResult();

		// TODO: Es posible que se ejecute mas de una firma como resultado de haber proporcionado varios
		// identificadores de datos o en una operacion de contrafirma.

		byte[] preSign;
		try {
			preSign = config.getPreSign();
		}
		catch (final IOException e) {
			// Cuando la respuesta no indica el numero de firmas y no se ha devuelto ninguna
			Log.e(SFConstants.LOG_TAG, "No se ha devuelto ningun resultado de firma"); //$NON-NLS-1$
			preSign = null;
		}

		if (preSign == null) {
			throw new AOException("El servidor no ha devuelto la prefirma del documento"); //$NON-NLS-1$
		}

		final byte[] pkcs1sign = new AOPkcs1Signer().sign(
				preSign,
				signatureAlgorithm,
				key);

		// Configuramos la peticion de postfirma indicando las firmas PKCS#1 generadas
		config.setPk1(pkcs1sign);

		if (config.isNeedPreSign() == null || !config.isNeedPreSign().booleanValue()) {
			config.removePreSign();
		}
	}

	/**
	 * Genera la postfirma de un listado de prefirmas.
	 * @param request Petici&oacute;n de firma.
	 * @param conn Objeto para la comunicaci&oacute;n con el servicio proxy.
	 * @throws CertificateEncodingException Cuando hay un error en la codificaci&oacute;n del certificado.
	 * @throws IOException Cuando hay un error de lectura/escritura de datos.
	 * @throws SAXException Cuando el XML est&aacute; mal formado.
	 */
	private static RequestResult signPhase3(final TriphaseRequest[] request, final ProxyConnector conn)
			throws CertificateException, SAXException, IOException {
		return conn.postSignRequests(request);
	}
}
