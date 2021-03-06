package es.gob.afirma.android.signfolder;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/** Preferencias de la aplicaci&oacute;n. */
public final class AppPreferences {

	private static final String CONFIG_PROPERTIES = "config.properties"; //$NON-NLS-1$

	private static final String KEY_FORMATS_SUPPORTED = "supported.format"; //$NON-NLS-1$

	private static final String KEY_HELP_URL = "help.url"; //$NON-NLS-1$

    private static final String PREFERENCES_KEY_HELP_VERSION = "helpVersion";

	/** &Uacute;ltimo certificado utilizado */
	private static final String PREFERENCES_KEY_LAST_CERT = "lastCert"; //$NON-NLS-1$

//	/** Indicador de si se quiere firmar con Cl@ve Firma */
//	private static final String PREFERENCES_KEY_ENABLED_CLAVEFIRMA = "enabledClaveFirma"; //$NON-NLS-1$

	/** Clave de preferencia de la URL del Proxy. */
	private static final String PREFERENCES_KEY_SELECTED_PROXY_URL = "URL_PROXY"; //$NON-NLS-1$

	/** Clave de preferencia de alias del proxy seleccionado . */
	private static final String PREFERENCES_KEY_SELECTED_PROXY_ALIAS = "alias"; //$NON-NLS-1$

	/** Prefijo de clave de preferencia para registrar cuando un usuario tiene activas las
	 * notificaciones para un proxy. */
	public static final String PREFERENCES_KEY_PREFIX_NOTIFICATION_ACTIVE = "not_act_"; //$NON-NLS-1$

	public static final String PREFERENCES_KEY_PREFIX_NOTIFICATION_TOKEN = "not_tok_"; //$NON-NLS-1$

	/** Prefijo clave de preferencias de servidores. */
	private static final String PREFERENCES_KEY_PREFIX_SERVER = "server"; //$NON-NLS-1$

	/** Token actual para el env&iacute;o de notificaciones. */
	 private static final String PREFERENCES_KEY_CURRENT_TOKEN = "currentToken";

	private static final String CONFIG_SEPARATOR = ";"; //$NON-NLS-1$

	/** Alias del portafirmas por defecto de la AGE. */
	private static final String DEFAULT_PROXY_GOB_ALIAS = "Portafirmas General AGE";
	/** URL del portafirmas por defecto de la AGE. */
	private static final String DEFAULT_PROXY_GOB_URL = "https://servicios.seap.minhap.es/pfmovil/signfolder";
	/** Alias del portafirmas por defecto de RedSARA. */
	private static final String DEFAULT_PROXY_REDSARA_ALIAS = "Portafirmas RedSARA";
	/** URL del portafirmas por defecto de RedSARA. */
	private static final String DEFAULT_PROXY_REDSARA_URL = "https://portafirmas.redsara.es/pfmovil/pf";

	private static Properties config;

	private static AppPreferences mInstance;

	private static SharedPreferences sharedPref;

	private AppPreferences(){ }

	public static AppPreferences getInstance(){
		if (mInstance == null) mInstance = new AppPreferences();
		return mInstance;
	}

	/** Inicializa las preferencias a partir de su fichero de propiedades.
	 * @param activity Actividad padre. */
	public void init(final Context activity) {
		if(config ==  null) {
			config = new Properties();
			try {
				config.load(activity.getAssets().open(CONFIG_PROPERTIES));
			} catch (final Exception e) {
				// Esto no deberia ocurrir nunca
				throw new RuntimeException("No se encuentra el fichero de configuracion " + CONFIG_PROPERTIES, e); //$NON-NLS-1$
			}
		}
		if(sharedPref == null) {
			sharedPref = PreferenceManager.getDefaultSharedPreferences(activity);
		}
	}

//	/**
//	 * Define si se ha habilitado la firma con Cl@ve Firma.
//	 * @param  enabledClavefirma Certificado del usuario.
//	 */
//	public void setEnabledClavefirma(boolean enabledClavefirma) {
//		setPreferenceBool(PREFERENCES_KEY_ENABLED_CLAVEFIRMA, enabledClavefirma);
//	}
//
//	/**
//	 * Recupera si se ha habilitado la firma con Cl@ve Firma.
//	 * @return Certificado del usuario.
//	 */
//	public boolean getEnabledClavefirma() {
//		return getPreferenceBool(PREFERENCES_KEY_ENABLED_CLAVEFIRMA, false);
//	}

	/**
	 * Define el ultimo certificado con el que se accedio.
	 * @param  certEncoded Certificado del usuario.
	 */
	public void setLastCertificate(String certEncoded) {
		setPreference(PREFERENCES_KEY_LAST_CERT, certEncoded);
	}

	/**
	 * Recupera el ultimo certificado con el que se accedio.
	 * @return Certificado del usuario.
	 */
	public String getLastCertificate() {
		return getPreference(PREFERENCES_KEY_LAST_CERT, "");
	}

	/** Recupera el listado de formatos soportados.
	 * @return Listado de formatos soportados. */
	public String[] getSupportedFormats() {
		return config.getProperty(KEY_FORMATS_SUPPORTED).split(CONFIG_SEPARATOR);
	}

	/** Recupera la URL del documento de ayuda de la aplicaci&oacute;n.
	 * @return URL del documento. */
	public String getHelpUrl() {
		return config.getProperty(KEY_HELP_URL);
	}

    /**
     * Establece la versi&oacute;n de la aplicaci&oacute;n a la que corresponde la ayuda descargada.
     * @param version Versi&oacute;n actual de la aplicaci&oacute;n.
     */
    public void setHelpVersion(String version) {
        setPreference(PREFERENCES_KEY_HELP_VERSION, version);
    }


    /** Recupera la versi&oacute;n de la ayuda que se encuentra descargada.
     * @return Versi&oacute;nCode de la instancia de la aplicaci&oacute;n que se descarg&oacute; la ayuda actual. */
    public String getHelpVersion() {
        return getPreference(PREFERENCES_KEY_HELP_VERSION);
    }


    public String getPreference(final String key) {
		return getPreference(key, ""); //$NON-NLS-1$
	}

	public String getPreference(final String key, final String defaultValue) {
		return sharedPref.getString(key.trim(), defaultValue);
	}

	public void setPreference(final String key, final String value) {
		final SharedPreferences.Editor editor = sharedPref.edit();
		editor.putString(key.trim(), value);
		editor.apply();
	}

	public int getPreferenceInt(final String key, final int defaultValue) {
		return sharedPref.getInt(key.trim(), defaultValue);
	}

	public void setPreferenceInt(final String key, final int value) {
		final SharedPreferences.Editor editor = sharedPref.edit();
		editor.putInt(key.trim(), value);
		editor.apply();
	}

	public boolean getPreferenceBool(final String key, final boolean defaultValue) {
		return sharedPref.getBoolean(key.trim(), defaultValue);
	}

	public void setPreferenceBool(final String key, final boolean value) {
		final SharedPreferences.Editor editor = sharedPref.edit();
		editor.putBoolean(key.trim(), value);
		editor.apply();
	}

	private void removePreference(final String key) {
		final SharedPreferences.Editor editor = sharedPref.edit();
		editor.remove(key.trim());
		editor.apply();
	}

	/** Establece un proxy como el seleccionado.
	 * @param alias Alias del proxy.
	 * @param urlProxy URL del proxy. */
	public void setSelectedProxy(final String alias, final String urlProxy) {
		setPreference(PREFERENCES_KEY_SELECTED_PROXY_ALIAS, alias);
		setPreference(PREFERENCES_KEY_SELECTED_PROXY_URL, urlProxy);
	}

	/** Recupera la direcci&oacute;n del servidor proxy para la conexi&oacute;n con el Portafirmas.
	 * @return URL del Portafirmas. */
	public String getSelectedProxyUrl() {
		return getPreference(PREFERENCES_KEY_SELECTED_PROXY_URL, "");
	}

	/** Recupera el alias del proxy actualmente seleccionado.
	 * @return Alias del proxy o cadena vaci&iacute;a si no hay ninguno. */
	public String getSelectedProxyAlias() {
		return getPreference(PREFERENCES_KEY_SELECTED_PROXY_ALIAS, "");
	}
	
	public void saveServer(final String alias, final String url) {
		setPreference(PREFERENCES_KEY_PREFIX_SERVER + alias, url);
	}

	/**
	 * Devuelve la URL asignada al servidor.
	 * @param alias Alias del servidor.
	 * @return URL del servidor o cadena vac&iacute;a si no exist&iacute;a.
	 */
	public String getServer(final String alias) {
		return getPreference(PREFERENCES_KEY_PREFIX_SERVER + alias, "");
	}

	public void setCurrentToken(final String token) {
		setPreference(PREFERENCES_KEY_CURRENT_TOKEN, token);
	}

	public String getCurrentToken() {
		return getPreference(PREFERENCES_KEY_CURRENT_TOKEN);
	}

	public void removeServer(final String alias) {
		removePreference(PREFERENCES_KEY_PREFIX_SERVER + alias);
	}
	
	public void removeProxyConfig() {
		removePreference(PREFERENCES_KEY_SELECTED_PROXY_ALIAS);
		removePreference(PREFERENCES_KEY_SELECTED_PROXY_URL);
	}
	
	public List<String> getServersList() {
		ArrayList<String> servers = new ArrayList<>();
		Map<String, ?> allPrefs = sharedPref.getAll();
	    Set<String> set = allPrefs.keySet();
	    for(String s : set){
	    	if (s.startsWith(PREFERENCES_KEY_PREFIX_SERVER)) {
	    		servers.add(s.replaceFirst("^" + PREFERENCES_KEY_PREFIX_SERVER,""));
	    	}
	    }
	    return servers;
	}

	/**
	 * Establece los servidores proxy por defecto de la aplicaci&oacute;n. El del Portafirmas
	 * General de la AGE y el de RedSara.
	 */
	public void setDefaultServers() {
		saveServer(DEFAULT_PROXY_GOB_ALIAS, DEFAULT_PROXY_GOB_URL);
		saveServer(DEFAULT_PROXY_REDSARA_ALIAS, DEFAULT_PROXY_REDSARA_URL);

		setSelectedProxy(DEFAULT_PROXY_GOB_ALIAS, DEFAULT_PROXY_GOB_URL);
	}
}
