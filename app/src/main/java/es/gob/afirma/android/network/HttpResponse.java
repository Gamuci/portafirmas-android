package es.gob.afirma.android.network;

/**
 * Objeto con la respuesta de la conexi&oacute;n a un servicio realizada a traves de
 * {@link HttpConnectionHelper}.
 */
public class HttpResponse {

    private byte[] content;

    public void setContent(final byte[] content) {
        this.content = content;
    }

    public byte[] getContent() {
        return this.content;
    }
}
