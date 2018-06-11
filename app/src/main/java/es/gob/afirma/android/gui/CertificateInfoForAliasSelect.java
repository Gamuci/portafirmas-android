package es.gob.afirma.android.gui;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Clase que contiene la informaci&oacute;n del certificado.
 * Nombre com&uacute;n, fecha comienzo validez y fecha expiraci&oacute;n.
 * @author Astrid Idoate
 *
 */
public class CertificateInfoForAliasSelect implements Serializable {


	/** Serial ID. */
	private static final long serialVersionUID = 1L;

	private String commonName;
	private Date notAfterDate;
	private Date notBeforeDate;
	private String issuer;
	private SimpleDateFormat sdf;

	private String alias;

	/**
	 * @param commonName Nombre com&uacute;n del certificado.
	 * @param notBeforeDate Fecha en la que comienza la validez del certificado.
	 * @param notAfterDate Fecha de expiraci&oacute;n del certificado.
	 * @param alias Alias asignado al certificado.
	 * @param issuer Emisor del certificado.
	 */
	CertificateInfoForAliasSelect(final String commonName,final Date notBeforeDate, final Date notAfterDate, final String alias, final String issuer) {
		this.commonName = commonName;
		this.notBeforeDate = notBeforeDate;
		this.notAfterDate = notAfterDate;
		this.alias = alias;
		this.issuer = issuer;

		this.sdf = new SimpleDateFormat("dd/MM/yyyy", new Locale("ES")); //$NON-NLS-1$ //$NON-NLS-2$
	}


	/** Devuelve la fecha de expiraci&oacute;n del certificado.
	 * @return notAfterDate fecha de expiraci&oacute;n del certificado.
	 */
	public String getNotAfterDate(){
		return this.sdf.format(this.notAfterDate);
	}


	/** Devuelve la fecha de comienzo de validez del certificado.
	 * @return notBeforeDate fecha en la que comienza la validez del certificado.
	 */
	public String getNotBeforeDate(){
		return this.sdf.format(this.notBeforeDate);
	}

	/** Devuelve el nombre com&uacute;n del certificado
	 * @return commonName nombre com&uacute;n del certificado.
	 */
	public String getCommonName(){
		return this.commonName;
	}

	/** Devuelve el alias del certificado.
	 * @return alias Alias del certificado.
	 */
	public String getAlias(){
		return this.alias;
	}

	/** Devuelve el nombre del emisor del certificado.
	 * @return issuer Emisor del certificado.
	 */
	public String getIssuer(){
		return this.issuer;
	}

	/**
	 * @param alias Alias del certificado.
	 */
	public void setAlias(final String alias){
		this.alias = alias;
	}

}
