package es.gob.afirma.android.signfolder.proxy;

import org.xml.sax.SAXException;

import java.io.IOException;

/**
 * Interfaz que define las opciones de comprobaci&oacute;n del estado de las notificaciones
 * de un usuario.
 */
public interface NotificationChecker {

    /**
     * Consulta si las notificaci&oacute;nes est&aacute;n activas para el usuario.
     * @return XML de respuesta de la consulta.
     * @throws SAXException
     *             Cuando se encuentra un XML mal formado.
     * @throws IOException
     *             Cuando existe alg&uacute;n problema en la lectura/escritura
     *             de XML o al recuperar la respuesta del servidor.
     */
    NotificationState recoverNotificationState() throws SAXException, IOException;

    /**
     * Env&iacute;a una solicitud para la desactivaci&oacute;n de las notificaciones del usuario.
     * @return XML de respuesta de la petici&oacute;n.
     * @throws SAXException
     *             Cuando se encuentra un XML mal formado.
     * @throws IOException
     *             Cuando existe alg&uacute;n problema en la lectura/escritura
     *             de XML o al recuperar la respuesta del servidor.
     */
    NotificationState disableNotification() throws SAXException, IOException;
}
