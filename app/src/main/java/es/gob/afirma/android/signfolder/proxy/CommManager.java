package es.gob.afirma.android.signfolder.proxy;

import android.util.Log;

import java.io.IOException;
import java.net.URL;

import es.gob.afirma.android.signfolder.AppPreferences;
import es.gob.afirma.android.signfolder.SFConstants;

/** Gestor de comunicaciones con el servidor de portafirmas m&oacute;vil.
 * @author Tom&aacute;s Garc&iacute;a-Mer&aacute;s. */
public final class CommManager {

//	private String signFolderProxyUrl;

	private static ProxyConnector connector;

//	private static CommManager instance = null;
//
//	/** Obtiene una instancia de la clase.
//	 * @return Gestor de comunicaciones con el Proxy. */
//	public static CommManager getInstance() {
//		if (instance == null) {
//			instance = new CommManager();
//		}
//		return instance;
//	}
//
//	private CommManager() {
//		this.signFolderProxyUrl = AppPreferences.getInstance().getSelectedProxyUrl();
//	}

    public static void configureConnector(byte[] certEncoded) throws IOException {

        String proxyUrl = AppPreferences.getInstance().getSelectedProxyUrl();

        connector = new SecureProxyConector(proxyUrl);

        if (connector.isCompatibleService()) {
            Log.i(SFConstants.LOG_TAG, "Se configura el conector con el proxy seguro");
        }
        else {
            Log.i(SFConstants.LOG_TAG, "Se configura el conector de retrocompatible con proxies previos a la v2.0");
            connector = new OldProxyConector(certEncoded, proxyUrl);
        }
        connector.init();
    }

    public static ProxyConnector getProxyConnector() {
	    return connector;
    }


//	public RequestResult loginRequest() throws Exception {
//	    return this.connector.loginRequest();
//	}
//
//	public boolean tokenValidation(final byte[] pkcs1, String cert, PrivateKeyEntry pke) throws Exception {
//		return this.connector.tokenValidation(pkcs1, cert, pke);
//	}
//
//	public void logoutRequest() throws Exception {
//        this.connector.logoutRequest();
//		instance = null;
//	}
//
//	/**
//	 * Obtiene la peticiones de firma. Las peticiones devueltas deben cumplir
//	 * las siguientes condiciones:
//	 * <ul>
//	 * <li>Estar en el estado se&ntilde;alado (unresolved, signed o rejected).</li>
//	 * <li>Que todos los documentos que contiene se tengan que firmar con los
//	 * formatos de firma indicados (s&oacute;lo si se indica alguno)</li>
//	 * <li>Que las solicitudes cumplan con los filtros establecidos. Estos
//	 * filtros tendran la forma: key=value</li>
//	 * </ul>
//	 * @param signRequestState Estado de las peticiones que se desean obtener.
//	 * @param filters
//	 *            Listado de filtros que deben cumplir las peticiones
//	 *            recuperadas. Los filtros soportados son:
//	 *            <ul>
//	 *            <li><b>orderAscDesc:</b> con valor "asc" para que sea orden
//	 *            ascendente en la consulta, en cualquier otro caso ser&aacute;
//	 *            descendente</li>
//	 *            <li><b>initDateFilter:</b> fecha de inicio de las peticiones</li>
//	 *            <li><b>endDateFilter:</b> fecha de fin de las peticiones</li>
//	 *            <li><b>orderAttribute:</b> par&aacute;metro para ordenar por
//	 *            una columna de la petici&oacute;n</li>
//	 *            <li><b>searchFilter:</b> busca la cadena introducida en
//	 *            cualquier texto de la petici&oacute;n (asunto, referencia,
//	 *            etc)</li>
//	 *            <li><b>labelFilter:</b> texto con el nombre de una etiqueta.
//	 *            Filtra las peticiones en base a esa etiqueta, ej: "IMPORTANTE"
//	 *            </li>
//	 *            <li><b>applicationFilter:</b> texto con el identificador de
//	 *            una aplicaci&oacute;n. Filtra las peticiones en base a la
//	 *            aplicaci&oacute;n, ej: "SANCIONES"</li>
//	 *            </ul>
//	 * @param numPage N&uacute;mero de p&aacute;gina del listado.
//	 * @param pageSize N&uacute;mero de peticiones por p&aacute;gina.
//	 * @return Lista de peticiones de firma
//	 * @throws SAXException
//	 *             Si el XML obtenido del servidor no puede analizarse
//	 * @throws IOException
//	 *             Si ocurre un error de entrada / salida
//	 */
//	public PartialSignRequestsList getSignRequests(
//			final String signRequestState, final String[] filters,
//			final int numPage, final int pageSize) throws SAXException,
//			IOException {
//		return this.connector.getSignRequests(signRequestState, filters, numPage, pageSize);
//	}
//
//	/** Inicia la pre-firma remota de las peticiones.
//	 * @param request Petici&oacute;n de firma.
//	 * @return Prefirmas de las peticiones enviadas.
//	 * @throws IOException Si ocurre algun error durante el tratamiento de datos.
//	 * @throws CertificateEncodingException Si no se puede obtener la codificaci&oacute;n del certificado.
//	 * @throws SAXException Si ocurren errores analizando el XML de respuesta. */
//	public TriphaseRequest[] preSignRequests(final SignRequest request) throws IOException,
//			CertificateException,
//			SAXException {
//		return this.connector.preSignRequests(request);
//	}
//
//	/**
//	 * Inicia la post-firma remota de las peticiones.
//	 *
//	 * @param requests
//	 *            Peticiones a post-firmar
//	 * @return Listado con el resultado de la operaci&oacute;n de firma de cada
//	 *         petici&oacute;n.
//	 * @throws IOException
//	 *             Si ocurre algun error durante el proceso
//	 * @throws CertificateEncodingException
//	 *             Cuando el certificado est&aacute; mal codificado.
//	 * @throws SAXException
//	 *             Si ocurren errores analizando el XML de respuesta
//	 */
//	public RequestResult postSignRequests(final TriphaseRequest[] requests) throws IOException,
//			CertificateEncodingException, SAXException {
//		return this.connector.postSignRequests(requests);
//	}
//
//	/**
//	 * Obtiene los datos de un documento.
//	 *
//	 * @param requestId
//	 *            Identificador de la petici&oacute;n.
//	 * @return Datos del documento.
//	 * @throws SAXException
//	 *             Cuando se encuentra un XML mal formado.
//	 * @throws IOException
//	 *             Cuando existe alg&uacute;n problema en la lectura/escritura
//	 *             de XML o al recuperar la respuesta del servidor.
//	 */
//	public RequestDetail getRequestDetail(final String requestId) throws SAXException, IOException {
//		return this.connector.getRequestDetail(requestId);
//	}
//
//	/**
//	 * Obtiene el listado de aplicaciones para las que hay peticiones de firma.
//	 * @return Configuracion de aplicaci&oacute;n.
//	 * @throws SAXException
//	 *             Cuando se encuentra un XML mal formado.
//	 * @throws IOException
//	 *             Cuando existe alg&uacute;n problema en la lectura/escritura
//	 *             de XML o al recuperar la respuesta del servidor.
//	 */
//	public AppConfiguration getApplicationList()
//			throws SAXException, IOException {
//		return this.connector.getApplicationList();
//	}
//
//	/**
//	 * Rechaza las peticiones de firma indicadas.
//	 *
//	 * @param requestIds
//	 *            Identificadores de las peticiones de firma que se quieren
//	 *            rechazar.
//	 * @return Resultado de la operacion para cada una de las peticiones de
//	 *         firma.
//	 * @throws SAXException
//	 *             Si el XML obtenido del servidor no puede analizarse
//	 * @throws IOException
//	 *             Si ocurre un error de entrada / salida
//	 */
//	public RequestResult[] rejectRequests(final String[] requestIds,
//			final String reason) throws SAXException, IOException {
//	    return this.connector.rejectRequests(requestIds, reason);
//	}
//
//	/** Obtiene la previsualizaci&oacute;n de un documento.
//	 * @param documentId Identificador del documento.
//	 * @param filename Nombre del fichero.
//	 * @param mimetype MIME-Type del documento.
//	 * @return Datos del documento.
//	 * @throws SAXException Cuando se encuentra un XML mal formado.
//	 * @throws IOException Cuando existe alg&uacute;n problema en la lectura/escritura
//	 *                     de XML o al recuperar la respuesta del servidor. */
//	public DocumentData getPreviewDocument(final String documentId,
//			final String filename, final String mimetype) throws SAXException, IOException {
//        return this.connector.getPreviewDocument(documentId, filename, mimetype);
//	}
//
//	/** Obtiene la previsualizaci&oacute;n de una firma.
//	 * @param documentId Identificador del documento.
//	 * @param filename Nombre del fichero.
//	 * @return Datos del documento.
//	 * @throws SAXException Cuando se encuentra un XML mal formado.
//	 * @throws IOException Cuando existe alg&uacute;n problema en la lectura/escritura
//	 *                     de XML o al recuperar la respuesta del servidor. */
//	public DocumentData getPreviewSign(final String documentId,
//			final String filename) throws SAXException, IOException {
//        return this.connector.getPreviewSign(documentId, filename);
//	}
//
//	/** Obtiene la previsualizaci&oacute;n de un informe de firma.
//	 * @param documentId Identificador del documento.
//	 * @param filename Nombre del fichero.
//	 * @param mimetype MIME-Type del documento.
//	 * @return Datos del documento.
//	 * @throws SAXException Cuando se encuentra un XML mal formado.
//	 * @throws IOException Cuando existe alg&uacute;n problema en la lectura/escritura
//	 *                     de XML o al recuperar la respuesta del servidor. */
//	public DocumentData getPreviewReport(final String documentId,
//			final String filename, final String mimetype) throws SAXException, IOException {
//        return this.connector.getPreviewReport(documentId, filename, mimetype);
//	}
//
//	/**
//	 * Aprueba peticiones de firma (les da el visto bueno).
//	 *
//	 * @param requestIds
//	 *            Identificador de las peticiones.
//	 * @return Resultado de la operaci&oacute;n.
//	 * @throws SAXException
//	 *             Cuando se encuentra un XML mal formado.
//	 * @throws IOException
//	 *             Cuando existe alg&uacute;n problema en la lectura/escritura
//	 *             de XML o al recuperar la respuesta del servidor.
//	 */
//	public RequestResult[] approveRequests(final String[] requestIds) throws SAXException, IOException {
//        return this.connector.approveRequests(requestIds);
//	}
//
//	/**
//	 * Da de alta en el sistema de notificaciones.
//	 *
//	 * @param token
//	 * 			Token de registro en GCM.
//	 * @param device
//	 * 			Identificador de dispositivo.
//	 * @param certB64
//	 * 			Certificado en base 64 del usuario.
//	 * @return Resultado del proceso de alta en el sistema de notificaciones.
//	 * 			Indica
//	 * @throws SAXException
//	 *             Cuando se encuentra un XML mal formado.
//	 * @throws IOException
//	 *             Cuando existe alg&uacute;n problema en la lectura/escritura
//	 *             de XML o al recuperar la respuesta del servidor.
//	 */
//	public NotificationRegistryResult signOnNotificationService(
//			final String token, final String device, final String certB64)
//			throws SAXException, IOException {
//        return this.connector.signOnNotificationService(token, device, certB64);
//	}

	/** Verifica si la URL de proxy configurada es correcta.
	 * @return <code>true</code> si es correcta, <code>false</code> si no lo es. */
	public static boolean verifyProxyUrl() {

	    String proxyUrl = AppPreferences.getInstance().getSelectedProxyUrl();

		boolean correctUrl = true;
		if (proxyUrl == null || proxyUrl.trim().length() == 0) {
			correctUrl = false;
		}
		else {
			try {
				new URL(proxyUrl);
			}
			catch (final Exception e) {
				correctUrl = false;
			}
		}
		return correctUrl;
	}



//	public FirePreSignResult claveFirmaPreSignRequests(final SignRequest[] requests) throws IOException, SAXException {
//		final String dataB64UrlSafe = prepareParam(
//				XmlRequestsFactory.createClaveFirmaPreSignRequest(requests)
//		);
//
//		return PresignsClaveFirmaResponseParser.parse(getRemoteDocument(prepareUrl(
//				OPERATION_PRESIGN_CLAVE_FIRMA, dataB64UrlSafe)));
//	}
//
//	public RequestResult[] claveFirmaPostSignRequests(final FirePreSignResult firePreSignResult) throws IOException, SAXException {
//		final String dataB64UrlSafe = prepareParam(
//				XmlRequestsFactory.createClaveFirmaPostSignRequest(firePreSignResult)
//		);
//
//		return PostsignsClaveFirmaResponseParser.parse(getRemoteDocument(prepareUrl(
//				OPERATION_POSTSIGN_CLAVE_FIRMA, dataB64UrlSafe)));
//	}
//	public RequestResult claveConnectGetUrl() throws Exception {
//
//		final UrlHttpManager urlManager = UrlHttpManagerFactory.getInstalledManager();
//
//		String xml = "<claveRequest />"; //$NON-NLS-1$
//
//		String url = this.signFolderProxyUrl + createUrlParams(OPERATION_CLAVE_LOGIN_REQUEST, xml); //$NON-NLS-1$
//
//		String xmlResponse = new String(urlManager.readUrl(url, UrlHttpMethod.POST));
//
//		System.out.println("Respuesta a la peticion de login a clave:\n" + xmlResponse); //$NON-NLS-1$
//
//		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
//				.parse(new InputSource(new StringReader(xmlResponse)));
//
//		return ClaveUrlRequestResponseParser.parse(doc);
//	}
//
//	public RequestResult claveConnectValidateLogin() throws Exception {
//		final UrlHttpManager urlManager = UrlHttpManagerFactory.getInstalledManager();
//
//		String xml = "<claveLoginValidation />"; //$NON-NLS-1$
//
//		String url = this.signFolderProxyUrl + createUrlParams(OPERATION_CLAVE_LOGIN_VALIDATION, xml); //$NON-NLS-1$
//
//		String xmlResponse = new String(urlManager.readUrl(url, UrlHttpMethod.POST));
//
//		System.out.println("Respuesta a la verificacion de login con clave:\n" + xmlResponse); //$NON-NLS-1$
//
//		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
//				.parse(new InputSource(new StringReader(xmlResponse)));
//
//		return ClaveLoginValidationResponseParser.parse(doc);
//	}
}