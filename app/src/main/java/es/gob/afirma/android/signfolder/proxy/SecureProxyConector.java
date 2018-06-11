package es.gob.afirma.android.signfolder.proxy;

import android.util.Log;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import es.gob.afirma.android.network.HttpConnectionHelper;
import es.gob.afirma.android.network.HttpResponse;
import es.gob.afirma.android.signfolder.AppPreferences;
import es.gob.afirma.android.signfolder.SFConstants;
import es.gob.afirma.android.util.Base64;
import es.gob.afirma.core.misc.http.HttpError;

class SecureProxyConector implements ProxyConnector, NotificationChecker {

    private static final String PARAMETER_NAME_OPERATION = "op"; //$NON-NLS-1$
    private static final String PARAMETER_NAME_DATA = "dat"; //$NON-NLS-1$

    private final String proxyUrl;

    private DocumentBuilder db = null;

    SecureProxyConector(final String proxyUrl) {
        this.proxyUrl = proxyUrl;
    }


    @Override
    public void init() throws IOException {
        try {
            this.db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (final ParserConfigurationException e) {
            Log.e(SFConstants.LOG_TAG,
                    "No se ha podido cargar un manejador de XML: " + e.toString(), e); //$NON-NLS-1$
            throw  new IOException("No se ha podido cargar un manejador de XML", e);
        }
    }


    @Override
    public boolean isCompatibleService() throws IOException {

        String baseUrl = this.proxyUrl.substring(0, this.proxyUrl.lastIndexOf('/'));
        String versionServiceUrl = baseUrl + "/version";

        byte[] version;
        try {
            HttpResponse response = HttpConnectionHelper.connect(versionServiceUrl);
            version = response.getContent();
        }
        catch (HttpError e) {
            // Si no dispone del servicio de consulta de version, entonces no es
            // compatible con el proxy seguro
            if (e.getResponseCode() == 404) {
                return false;
            }
            throw e;
        }
        catch (FileNotFoundException e) {
            return false;
        }

        String versionText = new String(version);
        String majorVersion = versionText.substring(0, versionText.indexOf("."));
        try {
            return Integer.parseInt(majorVersion) >= 2;
        }
        catch (Exception e) {
            Log.w(SFConstants.LOG_TAG,"No se ha encontrado el numero de version");
            return false;
        }
    }

    @Override
    public boolean needLogin() {
        return true;
    }

    @Override
    public RequestResult loginRequest() throws Exception {
        String xml = "<lgnrq />"; //$NON-NLS-1$
        String url = this.proxyUrl + createUrlParams(ProxyServiceOperations.LOGIN_REQUEST.getCode(), xml); //$NON-NLS-1$

        HttpResponse httpResponse = HttpConnectionHelper.connect(url);

        Document doc = loadDocument(httpResponse.getContent());

        return LoginTokenResponseParser.parse(doc);
    }

    @Override
    public boolean tokenValidation(byte[] pkcs1, String cert) throws Exception {
        String xml = "<rqtvl><cert>" + cert + "</cert><pkcs1>" + Base64.encode(pkcs1) + "</pkcs1></rqtvl>"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        String url = this.proxyUrl + createUrlParams(ProxyServiceOperations.LOGIN_VALIDATION.getCode(), xml); //$NON-NLS-1$

        HttpResponse httpResponse = HttpConnectionHelper.connect(url);

        Document doc = loadDocument(httpResponse.getContent());

        return LoginValidationResponseParser.parse(doc).isStatusOk();
    }

    @Override
    public void logoutRequest() throws Exception {
        String xml = "<lgorq />"; //$NON-NLS-1$
        String url = this.proxyUrl + createUrlParams(ProxyServiceOperations.LOGOUT.getCode(), xml); //$NON-NLS-1$

        HttpResponse httpResponse = HttpConnectionHelper.connect(url);

        Document doc = loadDocument(httpResponse.getContent());

        LogoutResponseParser.parse(doc);
    }

    @Override
    public PartialSignRequestsList getSignRequests(String signRequestState, String[] filters, int numPage, int pageSize) throws SAXException, IOException {
        final String xml = XmlRequestsFactory.createRequestListRequest(
                signRequestState,
                AppPreferences.getInstance().getSupportedFormats(),
                filters,
                numPage,
                pageSize);
        String url = this.proxyUrl + createUrlParams(ProxyServiceOperations.REQUEST_LIST.getCode(), xml); //$NON-NLS-1$

        HttpResponse httpResponse = HttpConnectionHelper.connect(url);

        Document doc = loadDocument(httpResponse.getContent());

        return RequestListResponseParser.parse(doc);
    }

    @Override
    public TriphaseRequest[] preSignRequests(SignRequest request) throws IOException, SAXException {
        String xml = XmlRequestsFactory.createPresignRequest(request);
        String url = this.proxyUrl + createUrlParams(ProxyServiceOperations.PRESIGN.getCode(), xml); //$NON-NLS-1$

        HttpResponse httpResponse = HttpConnectionHelper.connect(url);

        Document doc = loadDocument(httpResponse.getContent());

        return PresignsResponseParser.parse(doc);
    }

    @Override
    public RequestResult postSignRequests(TriphaseRequest[] requests) throws IOException, SAXException {

        String xml = XmlRequestsFactory.createPostsignRequest(requests);
        String url = this.proxyUrl + createUrlParams(ProxyServiceOperations.POSTSIGN.getCode(), xml); //$NON-NLS-1$

        HttpResponse httpResponse = HttpConnectionHelper.connect(url);

        Document doc = loadDocument(httpResponse.getContent());

		return PostsignsResponseParser.parse(doc);
    }

    @Override
    public RequestDetail getRequestDetail(String requestId) throws SAXException, IOException {

        String xml = XmlRequestsFactory.createDetailRequest(requestId);
        String url = this.proxyUrl + createUrlParams(ProxyServiceOperations.DETAIL.getCode(), xml); //$NON-NLS-1$

        HttpResponse httpResponse = HttpConnectionHelper.connect(url);

        Document doc = loadDocument(httpResponse.getContent());

        return RequestDetailResponseParser.parse(doc);
    }

    @Override
    public AppConfiguration getApplicationList() throws SAXException, IOException {
        String xml = XmlRequestsFactory.createAppListRequest();
        String url = this.proxyUrl + createUrlParams(ProxyServiceOperations.APP_LIST.getCode(), xml); //$NON-NLS-1$

        HttpResponse httpResponse = HttpConnectionHelper.connect(url);

        Document doc = loadDocument(httpResponse.getContent());

        return ApplicationListResponseParser.parse(doc);
    }

    @Override
    public RequestResult[] rejectRequests(String[] requestIds, String reason) throws SAXException, IOException {
        String xml = XmlRequestsFactory.createRejectRequest(requestIds, reason);
        String url = this.proxyUrl + createUrlParams(ProxyServiceOperations.REJECT.getCode(), xml); //$NON-NLS-1$

        HttpResponse httpResponse = HttpConnectionHelper.connect(url);

        Document doc = loadDocument(httpResponse.getContent());

        return RejectsResponseParser.parse(doc);
    }

    @Override
    public DocumentData getPreviewDocument(String documentId, String filename, String mimetype) throws IOException {
        return getPreview(ProxyServiceOperations.PREVIEW_DOCUMENT.getCode(), documentId, filename, mimetype);
    }

    @Override
    public DocumentData getPreviewSign(String documentId, String filename) throws IOException {
        return getPreview(ProxyServiceOperations.PREVIEW_SIGN.getCode(), documentId, filename, null);
    }

    @Override
    public DocumentData getPreviewReport(String documentId, String filename, String mimetype) throws IOException {
        return getPreview(ProxyServiceOperations.PREVIEW_REPORT.getCode(), documentId, filename, mimetype);
    }

    /** Obtiene la previsualizaci&oacute;n de un documento.
     * @param operation Identificador del tipo de documento (datos, firma o informe).
     * @param documentId Identificador del documento.
     * @param filename Nombre del fichero.
     * @param mimetype MIME-Type del documento.
     * @return Datos del documento.
     * @throws IOException Cuando existe alg&uacute;n problema en la lectura/escritura
     *                     de XML o al recuperar la respuesta del servidor. */
    private DocumentData getPreview(final String operation,
                                    final String documentId, final String filename,
                                    final String mimetype) throws IOException {
        String xml = XmlRequestsFactory.createPreviewRequest(documentId);
        String url = this.proxyUrl + createUrlParams(operation, xml); //$NON-NLS-1$

        InputStream responseIs = HttpConnectionHelper.preConnect(url);

        final DocumentData docData = new DocumentData(documentId, filename, mimetype);
        docData.setDataIs(responseIs);

        return docData;
    }

    @Override
    public RequestResult[] approveRequests(String[] requestIds) throws SAXException, IOException {
        String xml = XmlRequestsFactory.createApproveRequest(requestIds);
        String url = this.proxyUrl + createUrlParams(ProxyServiceOperations.APPROVE.getCode(), xml); //$NON-NLS-1$

        HttpResponse httpResponse = HttpConnectionHelper.connect(url);

        Document doc = loadDocument(httpResponse.getContent());

        return ApproveResponseParser.parse(doc);
    }

    @Override
    public boolean isNotificationsSupported() {
        return true;
    }

    @Override
    public NotificationState signOnNotificationService(String token, String device, String certB64) throws SAXException, IOException {
        String xml = XmlRequestsFactory.createRegisterNotificationRequest(token, device, certB64);
        String url = this.proxyUrl + createUrlParams(ProxyServiceOperations.SIGN_ON_NOTIFICATIONS.getCode(), xml); //$NON-NLS-1$

        HttpResponse httpResponse = HttpConnectionHelper.connect(url);

        Document doc = loadDocument(httpResponse.getContent());

        return NotificationRegisterParser.parse(doc);
    }

    @Override
    public NotificationState recoverNotificationState() throws SAXException, IOException {
        String xml = XmlRequestsFactory.createRecoverNotificationStateRequest();
        String url = this.proxyUrl + createUrlParams(ProxyServiceOperations.CHECK_NOTIFICATIONS_STATE.getCode(), xml); //$NON-NLS-1$

        HttpResponse httpResponse = HttpConnectionHelper.connect(url);

        Document doc = loadDocument(httpResponse.getContent());

        return NotificationStateParser.parse(doc);
    }

    @Override
    public NotificationState disableNotification() throws SAXException, IOException {
        String xml = XmlRequestsFactory.createChangeStateNotificationRequest();
        String url = this.proxyUrl + createUrlParams(ProxyServiceOperations.CHANGE_NOTIFICATIONS_STATE.getCode(), xml); //$NON-NLS-1$

        HttpResponse httpResponse = HttpConnectionHelper.connect(url);

        Document doc = loadDocument(httpResponse.getContent());

        return NotificationRegisterParser.parse(doc);
    }

    private Document loadDocument(final byte[] data) throws SAXException, IOException {
        return this.db.parse(new ByteArrayInputStream(data));
    }

    private static String createUrlParams(final String op, final String data) {
        return "?" + PARAMETER_NAME_OPERATION + "=" + op + "&" + PARAMETER_NAME_DATA + "=" + Base64.encode(data.getBytes(), true); //$NON-NLS-1$ //$NON-NLS-2$
    }
}
