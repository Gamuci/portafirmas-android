package es.gob.afirma.android.signfolder.proxy;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Analizador de la respuesa del servicio de comprobaci&oacute;n de estado de las notificaciones.
 */
class NotificationStateParser {

    private static final String NOTIFICATION_STATE_RESPONSE_NODE = "notst"; //$NON-NLS-1$
    private static final String ERROR_RESPONSE_NODE = "err"; //$NON-NLS-1$

    private static final String ATTR_ERROR_CODE = "cd"; //$NON-NLS-1$
    private static final String ATTR_STATUS = "status"; //$NON-NLS-1$
    private static final String ERROR_CODE_UNSUPPORTED_OPERATION = "ERR-01"; //$NON-NLS-1$

    private NotificationStateParser() {
        // No instanciable
    }

    /**
     * Analiza un documento XML y, en caso de tener el formato correcto, obtiene el estado de las
     * notificaciones del usuario o un estado "desconocido".
     * @param doc Documento XML.
     * @return Objeto con los datos del XML.
     * @throws IllegalArgumentException Cuando el XML no tiene el formato esperado.
     */
    static NotificationState parse(final Document doc) {

        if (doc == null) {
            throw new IllegalArgumentException("El documento proporcionado no puede ser nulo");  //$NON-NLS-1$
        }

        if (!NOTIFICATION_STATE_RESPONSE_NODE.equalsIgnoreCase(doc.getDocumentElement().getNodeName())) {

            if (ERROR_RESPONSE_NODE.equalsIgnoreCase(doc.getDocumentElement().getNodeName())) {
                return parseError(doc.getDocumentElement());
            }
            throw new IllegalArgumentException("El elemento raiz del XML debe ser '" + //$NON-NLS-1$
                    NOTIFICATION_STATE_RESPONSE_NODE + "' y aparece: " + //$NON-NLS-1$
                    doc.getDocumentElement().getNodeName());
        }

        final String status = doc.getDocumentElement().getAttribute(ATTR_STATUS);
        if (status == null) {
            return new NotificationState(NotificationState.STATE_UNKNOWN);
        }

        return new NotificationState(Integer.parseInt(status));
    }

    private static NotificationState parseError(Element node) {
        String errCode = node.getAttribute(ATTR_ERROR_CODE);
        if (errCode != null && errCode.equals(ERROR_CODE_UNSUPPORTED_OPERATION)) {
            return new NotificationState(NotificationState.STATE_UNKNOWN);
        }
        String textError = node.getTextContent();
        if (textError == null) {
            return new NotificationState(NotificationState.STATE_UNKNOWN);
        }
        return new NotificationState(NotificationState.STATE_UNKNOWN, textError.trim());
    }
}
