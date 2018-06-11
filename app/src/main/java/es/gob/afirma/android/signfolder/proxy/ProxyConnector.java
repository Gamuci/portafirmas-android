package es.gob.afirma.android.signfolder.proxy;

import org.xml.sax.SAXException;

import java.io.IOException;
import java.security.KeyStore;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;

/**
 * Interfaz que deben implementar todas las clases que sirvan para comunicarse con el proxy del
 * Portafirmas.
 */
public interface ProxyConnector {

    /**
     * Comprueba que se cumplan los requisitos para la conexi&oacute;n con el servicio proxy.
     * @throws IOException Cuando ocurre alg&uacute;n error en la comprobaci&oacute;n.
     */
    void init() throws IOException;

    /**
     * Comprueba si el servicio configurado pertenece a un proxy compatible.
     * @return {@code true} si el proxy configurado es compatible, {@code false} en caso contrario.
     * @throws IOException Cuando se produce un error al conectarse al proxy.
     */
    boolean isCompatibleService() throws IOException;

    /**
     * Indica si el servicio de Portafirmas correspondiente a este conector requiere un proceso
     * de login previo.
     * @return {@code true} si el servicio requiere login, {@code false} en caso contrario.
     */
    boolean needLogin();

    /**
     * Solicita el acceso para el usuario.
     * @return Respuesta a la petici&oacute;n con el token de acceso.
     * @throws Exception Cuando ocurre un error en la comunicaci&oacute;n.
     */
    RequestResult loginRequest() throws Exception;

    /**
     * Envia el token de acceso firmado al servidor para validar el acceso del usuario.
     * @param pkcs1 Firma PKCS#1 del token de acceso.
     * @param cert Certificado usado para firmar.
     * @return @code true} si el acceso se completo correctamente, {@code false} en caso contrario.
     * @throws Exception Cuando ocurre un error en la comunicaci&oacute;n.
     */
    boolean tokenValidation(byte[] pkcs1, String cert) throws Exception;

    /**
     * Env&aacute;a una solicitud de cierre de sesi&oacute;n.
     * @throws Exception Cuando se produce un error en la comunicaci&oacute;n.
     */
    void logoutRequest() throws Exception;

    /**
     * Obtiene la peticiones de firma. Las peticiones devueltas deben cumplir
     * las siguientes condiciones:
     * <ul>
     * <li>Estar en el estado se&ntilde;alado (unresolved, signed o rejected).</li>
     * <li>Que todos los documentos que contiene se tengan que firmar con los
     * formatos de firma indicados (s&oacute;lo si se indica alguno)</li>
     * <li>Que las solicitudes cumplan con los filtros establecidos. Estos
     * filtros tendran la forma: key=value</li>
     * </ul>
     * @param signRequestState Estado de las peticiones que se desean obtener.
     * @param filters
     *            Listado de filtros que deben cumplir las peticiones
     *            recuperadas. Los filtros soportados son:
     *            <ul>
     *            <li><b>orderAscDesc:</b> con valor "asc" para que sea orden
     *            ascendente en la consulta, en cualquier otro caso ser&aacute;
     *            descendente</li>
     *            <li><b>initDateFilter:</b> fecha de inicio de las peticiones</li>
     *            <li><b>endDateFilter:</b> fecha de fin de las peticiones</li>
     *            <li><b>orderAttribute:</b> par&aacute;metro para ordenar por
     *            una columna de la petici&oacute;n</li>
     *            <li><b>searchFilter:</b> busca la cadena introducida en
     *            cualquier texto de la petici&oacute;n (asunto, referencia,
     *            etc)</li>
     *            <li><b>labelFilter:</b> texto con el nombre de una etiqueta.
     *            Filtra las peticiones en base a esa etiqueta, ej: "IMPORTANTE"
     *            </li>
     *            <li><b>applicationFilter:</b> texto con el identificador de
     *            una aplicaci&oacute;n. Filtra las peticiones en base a la
     *            aplicaci&oacute;n, ej: "SANCIONES"</li>
     *            </ul>
     * @param numPage N&uacute;mero de p&aacute;gina del listado.
     * @param pageSize N&uacute;mero de peticiones por p&aacute;gina.
     * @return Lista de peticiones de firma
     * @throws SAXException
     *             Si el XML obtenido del servidor no puede analizarse
     * @throws IOException
     *             Si ocurre un error de entrada / salida
     */
    PartialSignRequestsList getSignRequests(
            String signRequestState, String[] filters, int numPage, int pageSize)
            throws SAXException, IOException;

    /** Inicia la pre-firma remota de las peticiones.
     * @param request Petici&oacute;n de firma.
     * @return Prefirmas de las peticiones enviadas.
     * @throws IOException Si ocurre algun error durante el tratamiento de datos.
     * @throws CertificateEncodingException Si no se puede obtener la codificaci&oacute;n del certificado.
     * @throws SAXException Si ocurren errores analizando el XML de respuesta. */
    TriphaseRequest[] preSignRequests(SignRequest request) throws IOException,
            CertificateException,
            SAXException;

    /**
     * Inicia la post-firma remota de las peticiones.
     *
     * @param requests
     *            Peticiones a post-firmar
     * @return Listado con el resultado de la operaci&oacute;n de firma de cada
     *         petici&oacute;n.
     * @throws IOException
     *             Si ocurre algun error durante el proceso
     * @throws CertificateEncodingException
     *             Cuando el certificado est&aacute; mal codificado.
     * @throws SAXException
     *             Si ocurren errores analizando el XML de respuesta
     */
    RequestResult postSignRequests(TriphaseRequest[] requests) throws IOException,
            CertificateEncodingException, SAXException;

    /**
     * Obtiene los datos de un documento.
     *
     * @param requestId
     *            Identificador de la petici&oacute;n.
     * @return Datos del documento.
     * @throws SAXException
     *             Cuando se encuentra un XML mal formado.
     * @throws IOException
     *             Cuando existe alg&uacute;n problema en la lectura/escritura
     *             de XML o al recuperar la respuesta del servidor.
     */
    RequestDetail getRequestDetail(String requestId) throws SAXException, IOException;

    /**
     * Obtiene el listado de aplicaciones para las que hay peticiones de firma.
     * @return Configuracion de aplicaci&oacute;n.
     * @throws SAXException
     *             Cuando se encuentra un XML mal formado.
     * @throws IOException
     *             Cuando existe alg&uacute;n problema en la lectura/escritura
     *             de XML o al recuperar la respuesta del servidor.
     */
    AppConfiguration getApplicationList()
            throws SAXException, IOException;

    /**
     * Rechaza las peticiones de firma indicadas.
     *
     * @param requestIds
     *            Identificadores de las peticiones de firma que se quieren
     *            rechazar.
     * @return Resultado de la operacion para cada una de las peticiones de
     *         firma.
     * @throws SAXException
     *             Si el XML obtenido del servidor no puede analizarse
     * @throws IOException
     *             Si ocurre un error de entrada / salida
     */
    RequestResult[] rejectRequests(String[] requestIds,
                                   String reason) throws SAXException, IOException;

    /** Obtiene la previsualizaci&oacute;n de un documento.
     * @param documentId Identificador del documento.
     * @param filename Nombre del fichero.
     * @param mimetype MIME-Type del documento.
     * @return Datos del documento.
     * @throws IOException Cuando existe alg&uacute;n problema en la lectura/escritura
     *                     de XML o al recuperar la respuesta del servidor. */
    DocumentData getPreviewDocument(String documentId,
                                    String filename,
                                    String mimetype) throws IOException;

    /** Obtiene la previsualizaci&oacute;n de una firma.
     * @param documentId Identificador del documento.
     * @param filename Nombre del fichero.
     * @return Datos del documento.
     * @throws IOException Cuando existe alg&uacute;n problema en la lectura/escritura
     *                     de XML o al recuperar la respuesta del servidor. */
    DocumentData getPreviewSign(String documentId,
                                String filename) throws IOException;

    /** Obtiene la previsualizaci&oacute;n de un informe de firma.
     * @param documentId Identificador del documento.
     * @param filename Nombre del fichero.
     * @param mimetype MIME-Type del documento.
     * @return Datos del documento.
     * @throws IOException Cuando existe alg&uacute;n problema en la lectura/escritura
     *                     de XML o al recuperar la respuesta del servidor. */
    DocumentData getPreviewReport(String documentId,
                                  String filename, String mimetype) throws IOException;

    /**
     * Aprueba peticiones de firma (les da el visto bueno).
     *
     * @param requestIds
     *            Identificador de las peticiones.
     * @return Resultado de la operaci&oacute;n.
     * @throws SAXException
     *             Cuando se encuentra un XML mal formado.
     * @throws IOException
     *             Cuando existe alg&uacute;n problema en la lectura/escritura
     *             de XML o al recuperar la respuesta del servidor.
     */
    RequestResult[] approveRequests(String[] requestIds) throws SAXException, IOException;

    /**
     * Indica si el conector soporta las funciones de notificaci&oacute;n.
     * @return {@code true} si se soportan las notificaciones, {@code false} en caso contrario.
     */
    boolean isNotificationsSupported();

    /**
     * Da de alta en el sistema de notificaciones.
     * @param token
     * 			Token de registro en GCM.
     * @param device
     * 			Identificador de dispositivo.
     * @param certB64
     * 			Certificado en base 64 del usuario.
     * @return Resultado del proceso de alta en el sistema de notificaciones.
     * 			Indica
     * @throws SAXException
     *             Cuando se encuentra un XML mal formado.
     * @throws IOException
     *             Cuando existe alg&uacute;n problema en la lectura/escritura
     *             de XML o al recuperar la respuesta del servidor.
     */
    NotificationState signOnNotificationService(
            String token, String device, String certB64)
            throws SAXException, IOException;


}
