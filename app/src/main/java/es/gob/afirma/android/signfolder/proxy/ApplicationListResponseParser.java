package es.gob.afirma.android.signfolder.proxy;

import android.util.Log;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.List;

import es.gob.afirma.android.signfolder.SFConstants;


/** Analizador de XML para la obtenci&oacute;n de la lista de aplicaciones activas.
 * @author Carlos Gamuci */
final class ApplicationListResponseParser {

	private static final String APP_LIST_NODE = "appConf"; //$NON-NLS-1$
	private static final String ERROR_NODE = "err"; //$NON-NLS-1$
	private static final String APP_ID_ATTR = "id"; //$NON-NLS-1$


	/** Analiza un documento XML y, en caso de tener el formato correcto, obtiene la lista de aplicaciones
	 * que pueden enviar peticiones de firma.
	 * @param doc Documento XML.
	 * @return Objeto con los datos del XML.
	 * @throws IllegalArgumentException Cuando el XML no tiene el formato esperado.
	 */
	static AppConfiguration parse(final Document doc) {

		if (doc == null) {
			throw new IllegalArgumentException("El documento proporcionado no puede ser nulo");  //$NON-NLS-1$
		}

		// Comprobamos si hemos recibido un XML correcto
		if (!APP_LIST_NODE.equalsIgnoreCase(doc.getDocumentElement().getNodeName())) {

			// Si no es un nodo de error, informamos de un problema grave en el acceso
			if (!ERROR_NODE.equalsIgnoreCase(doc.getDocumentElement().getNodeName())) {
				throw new IllegalArgumentException("El elemento raiz del XML debe ser '" + APP_LIST_NODE + //$NON-NLS-1$
						"' y aparece: " + doc.getDocumentElement().getNodeName()); //$NON-NLS-1$
			}

			// Si se trata de una respuesta de error, simplemente la obviamos, ya que no es obligatorio
			// dispone de las aplicaciones.
			Log.w(SFConstants.LOG_TAG,
					"Se ha recibido un mensaje de error del servicio al pedir las aplicaciones: "
							+ doc.getDocumentElement().getTextContent());
		}

		final List<String> appIds = new ArrayList<>();
		final List<String> appNames = new ArrayList<>();
		final NodeList appNodes = doc.getDocumentElement().getChildNodes();
		for (int i = 0; i < appNodes.getLength(); i++) {
			// Nos aseguramos de procesar solo nodos de tipo Element
			i = XmlUtils.nextNodeElementIndex(appNodes, i);
			if (i == -1) {
				break;
			}
			try {
				appIds.add(appNodes.item(i).getAttributes().getNamedItem(APP_ID_ATTR).getNodeValue());
				final String appName = XmlUtils.getTextContent(appNodes.item(i));
				appNames.add(normalizeValue(appName));
			} catch (final Exception e) {
				throw new IllegalArgumentException("Se encontro un nodo de aplicacion no valido: " + e, e); //$NON-NLS-1$
			}
		}

		final AppConfiguration config = new AppConfiguration();
		config.setAppIdsList(appIds);
		config.setAppNamesList(appNames);

		return config;
	}

	/**
	 * Deshace los cambios que hizo el proxy para asegurar que el XML est&aacute;ba bien formado.
	 * @param value Valor que normalizar.
	 * @return Valor normalizado.
	 */
	private static String normalizeValue(final String value) {
		return value.trim()
				.replace("&_lt;", "<") //$NON-NLS-1$ //$NON-NLS-2$
				.replace("&_gt;", ">"); //$NON-NLS-1$ //$NON-NLS-2$
	}
}
