package es.gob.afirma.android.signfolder;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Base64;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import es.gob.afirma.android.gcm.RegistrationIntentService;
import es.gob.afirma.android.signfolder.ConfigureFilterDialogBuilder.FilterConfig;
import es.gob.afirma.android.signfolder.LoadSignRequestsTask.LoadSignRequestListener;
import es.gob.afirma.android.signfolder.proxy.CommManager;
import es.gob.afirma.android.signfolder.proxy.FirePreSignResult;
import es.gob.afirma.android.signfolder.proxy.ProxyConnector;
import es.gob.afirma.android.signfolder.proxy.RequestResult;
import es.gob.afirma.android.signfolder.proxy.SignRequest;
import es.gob.afirma.android.signfolder.proxy.SignRequest.RequestType;

/**
 * Actividad que representa una lista de documentos pendientes de ser firmados o
 * rechazados. Al pulsar prolongadamente sobre un elemento se muestra un menu
 * contextual donde se puede ver los detalles del documento, firmar
 * individualmente o rechazar.
 * <p>
 * La visibilidad de la lista y la etiqueta de "No hay elementos" se delega en
 * el Layout por medio del uso de los elementos "list" y "empty" reconocidos por
 * el ListActivity.
 *
 * @author Carlos Gamuci
 */
public final class PetitionListActivity extends FragmentActivity implements
		OperationRequestListener, LoadSignRequestListener, OnItemClickListener,
		DialogFragmentListener
//        ClaveFirmaPreSignTask.ClaveFirmaPreSignListener,
//		ClaveFirmaPostSignTask.ClaveFirmaPostSignListener
    {

	/**
	 * Clave usada internamente para guardar el estado de la propiedad
	 * "needReload"
	 */
	private static final String KEY_SAVEINSTANCE_NEED_RELOAD = "saveinstance_needReload"; //$NON-NLS-1$

	/**
	 * Clave usada internamente para guardar el estado de la propiedad
	 * "loadingRequests"
	 */
	private static final String KEY_SAVEINSTANCE_LOADING = "saveinstance_loading"; //$NON-NLS-1$

	/**
	 * Clave para comprobaci&oacute;n del estado de las solicitudes de firma que
	 * se muestran actualmente.
	 */
	private static final String SIGN_REQUEST_STATE_KEY = "SignRequestState"; //$NON-NLS-1$

    public static final String EXTRA_RESOURCE_CERT_B64 = "es.gob.afirma.signfolder.cert"; //$NON-NLS-1$
    public static final String EXTRA_RESOURCE_CERT_ALIAS = "es.gob.afirma.signfolder.alias"; //$NON-NLS-1$
    public static final String EXTRA_RESOURCE_APP_IDS = "es.gob.afirma.signfolder.apps.ids"; //$NON-NLS-1$
    public static final String EXTRA_RESOURCE_APP_NAMES = "es.gob.afirma.signfolder.apps.names"; //$NON-NLS-1$
    public static final String EXTRA_RESOURCE_OPEN_FROM_NOTIFICATION = "es.gob.afirma.signfolder.openFromNotification"; //$NON-NLS-1$

	public static final String KEY_COUNT = "notificationCount";

	private static final String KEY_DAYS_TO_EXPIRE = "caducidad";

	private static final int DEFAULT_DAYS_TO_EXPIRE = 3;

	private final static int PAGE_SIZE = 50;

	/** Di&aacute;logo para la configuraci&oacute;n de los filtros. */
	private final static int DIALOG_FILTER = 11;

	/** Di&aacute;logo para confirmar el cierre de la sesi&oacute;n. */
	private final static int DIALOG_CONFIRM_EXIT = 12;

	/** Di&aacute;logo para confirmar el rechazo de peticiones. */
	private final static int DIALOG_CONFIRM_REJECT = 13;

	/** Di&aacute;logo de advertencia de que no se han seleccionado peticiones. */
	private final static int DIALOG_NO_SELECTED_REQUEST = 14;

	/** Di&aacute;logo para confirmar la firma de peticiones. */
	private final static int DIALOG_CONFIRM_SIGN = 15;

	/** Di&aacute;logo para mostrar el resultado devuelto por la pantalla de detalle. */
	private final static int DIALOG_RESULT_SIMPLE_REQUEST = 16;

	/**
	 * Di&aacute;logo de notificaci&oacute;n de error al procesar las
	 * peticiones.
	 */
	private final static int DIALOG_ERROR_PROCESSING = 16;

	/** Tag para la presentaci&oacute;n de di&aacute;logos */
	private final static String DIALOG_TAG = "dialog"; //$NON-NLS-1$

	/** Indica si debe recargarse el listado de peticiones. */
	private boolean needReload = true;

	private boolean loadingRequests = false;

	private boolean allSelected = false;

	private String currentState = null;

	private boolean openedFromNotification = false;

	private Menu menuRef;

	String getCurrentState() {
		return this.currentState;
	}

	private int currentPage = 1;

	/** Tarea de carga en ejecuci&oacute;n. */
	private LoadSignRequestsTask loadingTask = null;

	/** Informacion trifasica que se obtiene en la prefirma y reutiliza en la postfirma. */
	private FirePreSignResult firePreSignResult = null;

	private String certAlias = null;

	String getCertAlias() {
		return this.certAlias;
	}

	private String certB64 = null;

	String getCertB64() {
		return this.certB64;
	}

	private String[] appIds = null;
	private String[] appNames = null;

	private FilterConfig filterConfig = null;

	void setFilterConfig(final FilterConfig filterConfig) {
		this.filterConfig = filterConfig;
	}

	private int numPages = -1;

	// Numero de peticiones solicitadas y procesadas
	int numRequestToSignPending;
	int numRequestToApprovePending;
	int numRequestToRejectPending;

	private ProgressDialog progressDialog = null;

	ProgressDialog getProgressDialog() {
		return this.progressDialog;
	}

	void setProgressDialog(final ProgressDialog pd) {
		this.progressDialog = pd;
	}

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.currentState = getIntent().getStringExtra(SIGN_REQUEST_STATE_KEY);
		if (this.currentState == null) {
			this.currentState = SignRequest.STATE_UNRESOLVED;
		}

		// Si esta configurado que no se necesita recargar la pagina, no lo
		// hacemos
		if (savedInstanceState != null) {
			loadSavedInstances(savedInstanceState);
		}

		// Cargamos los extras proporcionados a la actividad
		loadIntentExtra(getIntent());

		// Configuramos la vista segun el estado
		if (SignRequest.STATE_SIGNED.equals(this.currentState)) {
			setTitle(R.string.title_signed_petition_list);
			setContentView(R.layout.activity_resolved_petition_list);
		} else if (SignRequest.STATE_REJECTED.equals(this.currentState)) {
			setTitle(R.string.title_rejected_petition_list);
			setContentView(R.layout.activity_resolved_petition_list);
		} else {
			setTitle(R.string.title_unresolved_petition_list);
			setContentView(R.layout.activity_unresolved_petition_list);
		}

		getListView().setOnItemClickListener(this);

        // Comprobamos si se encuentran activadas las notificaciones siempre y cuando el proxy
        // las soporte
		ProxyConnector conn = CommManager.getProxyConnector();
		if (conn == null) {
			closeActivity();
			return;
		}
        if (conn.isNotificationsSupported()) {
            String userProxyId = getUserProxyId();
            AppPreferences prefs = AppPreferences.getInstance();
            boolean registered = prefs.getPreferenceBool(
                    AppPreferences.PREFERENCES_KEY_PREFIX_NOTIFICATION_ACTIVE + userProxyId,
                    false);

            Log.i(SFConstants.LOG_TAG, "Las notificaciones se encuentran activadas: " + registered);

            if (registered && prefs.getCurrentToken() != null) {

                Log.i(SFConstants.LOG_TAG,  "El usuario tiene dado de alta el token: " +
                        prefs.getPreference(
                        AppPreferences.PREFERENCES_KEY_PREFIX_NOTIFICATION_TOKEN + userProxyId));


                // Comprobamos si el token de notificaciones que tiene dado de alta ese usuario
                // es igual al actual. En caso contrario, se actualiza
                if (!prefs.getCurrentToken().equals(
                        prefs.getPreference(
                                AppPreferences.PREFERENCES_KEY_PREFIX_NOTIFICATION_TOKEN + userProxyId))
                        ) {

                    Log.i(SFConstants.LOG_TAG, "Damos de alta el nuevo token de notificaciones");

                    Intent intent = new Intent(this, RegistrationIntentService.class);
                    intent.putExtra(RegistrationIntentService.EXTRA_RESOURCE_CERT_B64, this.certB64);
                    intent.putExtra(RegistrationIntentService.EXTRA_RESOURCE_USER_PROXY_ID, userProxyId);
                    intent.putExtra(RegistrationIntentService.EXTRA_RESOURCE_TOKEN, prefs.getCurrentToken());
					intent.putExtra(RegistrationIntentService.EXTRA_RESOURCE_BACKGROUND_REGISTRY, true);
                    startService(intent);
                } else {
                    Log.i(SFConstants.LOG_TAG, "El usuario ya tiene dado de alta el token de notificaciones correcto");
                }
            }
        }
	}

	/** Obtiene la clave con la que se guarda el token con el que actualmente est&aacute;
	 * dado de alta el usuario para la recepci&oacute;n de las notificaciones del Portafirmas
	 * actual.
	 */
	private String getUserProxyId() {
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("MD5");
		}
		catch (NoSuchAlgorithmException e) {
			Log.w(SFConstants.LOG_TAG,
					"No se puede comprobar el token dado de alta actualmente. " +
							"No se actualizara el token: " + e);
			return null;
		}
		md.update(AppPreferences.getInstance().getSelectedProxyUrl().getBytes());
		md.update(this.certB64.getBytes());

		return Base64.encodeToString(md.digest(), Base64.NO_PADDING).trim();
	}

	private void loadSavedInstances(final Bundle savedInstanceState) {
		this.needReload = savedInstanceState
				.containsKey(KEY_SAVEINSTANCE_NEED_RELOAD) ? savedInstanceState
				.getBoolean(KEY_SAVEINSTANCE_NEED_RELOAD) : false;

		this.loadingRequests = savedInstanceState
				.containsKey(KEY_SAVEINSTANCE_LOADING) ? savedInstanceState
				.getBoolean(KEY_SAVEINSTANCE_LOADING) : true;

		setFilterConfig(ConfigureFilterDialogBuilder
				.loadFilter(savedInstanceState));
	}

	private void loadIntentExtra(final Intent intent) {

		this.certAlias = intent.getStringExtra(EXTRA_RESOURCE_CERT_ALIAS);
		this.certB64 = intent.getStringExtra(EXTRA_RESOURCE_CERT_B64);
		this.appIds = intent.getStringArrayExtra(EXTRA_RESOURCE_APP_IDS);
		this.appNames = intent.getStringArrayExtra(EXTRA_RESOURCE_APP_NAMES);

//		// Si solo esta documentado el certificado se viene de pinchar en una notificacion
//		if(this.certAlias == null && this.appIds == null && this.appNames == null) {
//		    try {
//                CommManager.configureConnector(Base64.decode(this.certB64, Base64.URL_SAFE));
//            }
//            catch (Exception e) {
//		        Log.e(SFConstants.LOG_TAG, "Se ha encontrado un problema al configurar el conector con el proxy al acceder desde una notificacion", e);
//            }
//		}
	}

	@Override
	protected void onStart() {
		super.onStart();

		if (this.needReload) {
			updateCurrentList(1);
		}
	}

	/**
	 * Metodo que define la accion a realizar al pulsar en el boton Reject
	 *
	 * @param v
	 *            Vista desde la que se invoco el metodo
	 */
	public void onClickReject(final View v) {

		final SignRequest[] reqs = getSelectedRequests();

		if (reqs == null || reqs.length == 0) {
			showNoSelectedRequestDialog();
			return;
		}

		// Mostramos el dialogo para confirmar el rechazo
		final CustomAlertDialog dialog = CustomAlertDialog.newInstance(
				DIALOG_CONFIRM_REJECT,
				getString(R.string.dialog_title_confirm_reject),
				reqs.length > 1 ?
                        getString(R.string.dialog_msg_reject_request) :
						getString(R.string.dialog_msg_reject_request),
				getString(android.R.string.ok),
				getString(android.R.string.cancel),
				this);

		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				dialog.show(getSupportFragmentManager(), DIALOG_TAG);
			}
		});
	}

	private void showNoSelectedRequestDialog() {
		final CustomAlertDialog dialog = CustomAlertDialog.newInstance(
				DIALOG_NO_SELECTED_REQUEST, getString(R.string.aviso),
				getString(R.string.dialog_msg_no_selected_requests),
				getString(android.R.string.ok), null, this);

		try {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					dialog.show(getSupportFragmentManager(), DIALOG_TAG);
				}
			});
		} catch (final Exception e) {
			Log.w(SFConstants.LOG_TAG,
					"No se ha podido mostrar el dialogo informando de que no hay peticiones seleccionadas: " + e); //$NON-NLS-1$
		}
	}

	private void showErrorDialog(final int dialogId, final String message) {

		final CustomAlertDialog dialog = CustomAlertDialog.newInstance(
				dialogId, getString(R.string.aviso), message,
				getString(android.R.string.ok), null, this);

		try {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					dialog.show(PetitionListActivity.this.getSupportFragmentManager(), DIALOG_TAG);
				}
			});
		} catch (final Throwable e) {
			Log.w(SFConstants.LOG_TAG,
					"No se ha podido mostrar el dialogo de error con el mensaje: " + message + ": " + e); //$NON-NLS-1$ //$NON-NLS-2$
			e.printStackTrace();
		}
	}

	/**
	 * Metodo que define la accion a realizar al pulsar en el boton Sign
	 *
	 * @param v
	 *            Vista desde la que se invoco el metodo
	 */
	public void onClickSign(final View v) {

		// Fijamos las peticiones seleccionadas
		final SignRequest[] requests = getSelectedRequests();

		if (requests == null || requests.length == 0) {
			showNoSelectedRequestDialog();
			return;
		}

		final CustomAlertDialog dialog = CustomAlertDialog.newInstance(
				DIALOG_CONFIRM_SIGN,
				getString(R.string.aviso),
				getViewContentDialogConfirmOperation(requests),
				getString(android.R.string.ok),
				getString(R.string.cancel),
				this);

		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				dialog.show(getSupportFragmentManager(), DIALOG_TAG);
			}
		});

	}

	/**
	 * Devuelve la vista con el contenido del di&aacute;logo de
	 * confirmaci&oacute;n de rechazo, firma o visto bueno de las peticiones
	 * seleccionadas
	 *
	 * @param requests
	 *            Peticiones seleccionadas.
	 * @return Vista del di&aacute;logo de confirmaci&oacute;n.
	 */
	private View getViewContentDialogConfirmOperation(final SignRequest[] requests) {
		final LayoutInflater li = LayoutInflater.from(PetitionListActivity.this);
		final View view = li.inflate(R.layout.dialog_view_reject_sign_vb, null);
		int countApproved = 0;
		for (final SignRequest request : requests) {
			if (request.getType() == RequestType.APPROVE) {
				countApproved++;
			}
		}
		final TextView tvSign = (TextView) view.findViewById(R.id.tvSign);
		final TextView tvApprove = (TextView) view.findViewById(R.id.tvApprove);

		// Peticiones de firma
		if (requests.length - countApproved > 0) {
			tvSign.setVisibility(View.VISIBLE);
			if (requests.length - countApproved == 1) {
				tvSign.setText(R.string.dialog_msg_sign_petition_1); //$NON-NLS-1$
			} else {
				tvSign.setText(getString(R.string.dialog_msg_sign_petition_2, Integer.valueOf(requests.length - countApproved))); //$NON-NLS-1$
			}
		}
		// Peticiones de visto bueno
		if (countApproved > 0) {
			tvApprove.setVisibility(View.VISIBLE);
			if (countApproved == 1) {
				tvApprove
						.setText(R.string.dialog_msg_approve_petition_1); //$NON-NLS-1$
			} else {
				tvApprove
						.setText(getString(R.string.dialog_msg_approve_petition_2, Integer.valueOf(countApproved))); //$NON-NLS-1$
			}
		}

		return view;
	}

	/**
	 * Recupera el componente lista de la pantalla.
	 *
	 * @return Lista.
	 */
	private ListView getListView() {
		return (ListView) findViewById(R.id.list);
	}

	/**
	 * Recupera el componente lista de la pantalla.
	 *
	 * @return Lista.
	 */
	private ListAdapter getListAdapter() {
		return getListView().getAdapter();
	}

	/**
	 * Recupera el listado de solicitudes seleccionadas.
	 *
	 * @return Listado de solicitudes.
	 */
	private SignRequest[] getSelectedRequests() {
		final ListView lv = getListView();
		lv.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);

		final PetitionListArrayAdapter adapter = (PetitionListArrayAdapter) lv.getAdapter();
		if (adapter == null) {
			return null;
		}
		final List<SignRequest> requests = new ArrayList<SignRequest>();

		for (int i = 0; i < adapter.getCount(); i++) {
			final PetitionListAdapterItem item = adapter.getItem(i);
			if (item instanceof PetitionElement) {
				final SignRequest request = ((PetitionElement) item)
						.getSignRequest();
				if (request.isSelected()) {
					requests.add(request);
				}
			}
		}

		return requests.toArray(new SignRequest[requests.size()]);
	}

	protected RejectRequestsTask rejectRequests(final String reason, final SignRequest... signRequests) {
		final RejectRequestsTask rrt = new RejectRequestsTask(signRequests,
				CommManager.getProxyConnector(), this, reason);
		rrt.execute();
		return rrt;
	}

	// Definimos el menu de opciones de la aplicacion, cuyas opciones estan
	// definidas
	// para cada listado de peticiones
	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		menuRef = menu;
		if (SignRequest.STATE_SIGNED.equals(this.currentState)) {
			getMenuInflater().inflate(
					R.menu.activity_petition_list_signed_options_menu, menu);
		} else if (SignRequest.STATE_REJECTED.equals(this.currentState)) {
			getMenuInflater().inflate(
					R.menu.activity_petition_list_rejected_options_menu, menu);
		} else {
			getMenuInflater()
					.inflate(
							R.menu.activity_petition_list_unresolved_options_menu,
							menu);
		}

		// En el proxy antiguo no se soportan las notificaciones
		ProxyConnector conn = CommManager.getProxyConnector();
		if (conn != null && conn.isNotificationsSupported()) {
			String userProxyId = getUserProxyId();
			boolean registered = AppPreferences.getInstance().getPreferenceBool(
					AppPreferences.PREFERENCES_KEY_PREFIX_NOTIFICATION_ACTIVE + userProxyId,
					false);

			// Si el usuario no esta registrado en el sistema de notificaciones, le mostramos
			// la opcion para que lo pueda hacer
			if (!registered) {
				Log.i(SFConstants.LOG_TAG, "El usuario no tiene activadas las notificaciones. Se muestra la opcion en el menu.");
				menu.findItem(R.id.notifications).setVisible(true);
			}
		}

		return true;
	}

	// Definimos que hacer cuando se pulsa una opcion del menu de opciones de la
	// aplicacion
	// En el ejemplo se indica la opcion seleccionada
	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {

		// Seleccionar todos los elementos del listado actual
		if (item.getItemId() == R.id.select_all) {
			selectAllRequests(true);
			this.allSelected = true;
		}
		// Deseleccionar todos los elementos del listado actual
		else if (item.getItemId() == R.id.unselect_all) {
			selectAllRequests(false);
			this.allSelected = false;
		}
		// Actualizar listado actual
		else if (item.getItemId() == R.id.update) {
			updateCurrentList(1);
		}
		// Cambiar al listado de peticiones firmadas
		else if (item.getItemId() == R.id.see_signeds) {
			changeCurrentRequestList(SignRequest.STATE_SIGNED);
		}
		// Cambiar al listado de peticiones rechazadas
		else if (item.getItemId() == R.id.see_rejects) {
			changeCurrentRequestList(SignRequest.STATE_REJECTED);
		}
		// Cambiar al listado de peticiones pendientes
		else if (item.getItemId() == R.id.see_unresolveds) {
			changeCurrentRequestList(SignRequest.STATE_UNRESOLVED);
		}
		// Definir filtro
		else if (item.getItemId() == R.id.filter) {
			showDialog(DIALOG_FILTER, this.filterConfig == null ? new Bundle()
					: this.filterConfig.copyToBundle(null));
		}
		// Eliminar filtro
		else if (item.getItemId() == R.id.no_filter) {
			setFilterConfig(this.filterConfig.reset());
			this.filterDialogBuilder.resetLayout();
			invalidateOptionsMenu();
			updateCurrentList(1);
		}
		// Habilitar/deshabilitar notificaciones
		else if (item.getItemId() == R.id.notifications) {
			if (checkPlayServices()) {
				// Start IntentService to register this application with GCM.
				changesNotificationState();
			}
		}
		// Abrir ayuda
		else if (item.getItemId() == R.id.help) {
			OpenHelpDocumentTask task = new OpenHelpDocumentTask(this);
			task.execute();
		}
		// Cerrar sesion
		else if (item.getItemId() == R.id.logout) {
			showConfirmExitDialog();
		}

		return true;
	}

	/** Actualiza la lista de peticiones que se muestra actualmente. */
	void updateCurrentList(final int page) {

		if (!this.loadingRequests) {
			this.currentPage = page;

			// Configuramos los filtros
			final List<String> filters = ConfigureFilterDialogBuilder
					.generateFilters(this.filterConfig);
			this.loadingTask = new LoadSignRequestsTask(
					this.currentState, page, PAGE_SIZE, filters,
					CommManager.getProxyConnector(), this);
			setVisibilityLoadingMessage(true, null, this.loadingTask);
			this.loadingTask.execute();

			// Se reinicia el contador de notificaciones para el servidor cargado
			AppPreferences.getInstance().setPreferenceInt(KEY_COUNT + AppPreferences.getInstance().getSelectedProxyUrl(), 0);
		}
	}

	/**
	 * Cambia el listado de peticiones actual por el listado con las peticiones
	 * con el estado indicado.
	 *
	 * @param stateSigned
	 *            Estado de las peticiones que deben mostrarse.
	 */
	private void changeCurrentRequestList(final String stateSigned) {
		if (this.loadingRequests && this.loadingTask != null) {
			setVisibilityLoadingMessage(false, null, null);
			this.loadingTask.cancel(true);
		}
		this.needReload = true;
		getIntent().putExtra(SIGN_REQUEST_STATE_KEY, stateSigned);
		recreate();
	}

	final void closeActivity() {
		finish();
	}

	/**
	 * Selecciona o deselecciona todo el listado de peticiones en pantalla.
	 *
	 * @param selected
	 *            {@code true} para seleccionar todas las peticiones,
	 *            {@code false} para deseleccionarlas todas.
	 */
	private void selectAllRequests(final boolean selected) {

		// Seleccionamos cada elemento
		for (int i = 0; i < getListAdapter().getCount(); i++) {
			if (getListAdapter().getItem(i) instanceof PetitionElement) {
				((PetitionElement) getListAdapter().getItem(i))
						.getSignRequest().setSelected(selected);
			}
		}

		// Notificamos el cambio a la lista
		((ArrayAdapter<?>) getListAdapter()).notifyDataSetChanged();

		// Notificamos el cambio a la pantalla
		onContentChanged();
	}

	@Override
	public void requestOperationFinished(final int operation,
			final RequestResult request) {

		synchronized (this) {

			if (operation == SIGN_OPERATION) {
				this.numRequestToSignPending--;
			} else if (operation == APPROVE_OPERATION) {
				this.numRequestToApprovePending--;
			} else if (operation == REJECT_OPERATION) {
				this.numRequestToRejectPending--;
			}

			if (this.numRequestToSignPending <= 0
					&& this.numRequestToApprovePending <= 0
					&& this.numRequestToRejectPending <= 0) {
				setVisibilityLoadingMessage(false, null, null);

				// Una vez finalizada una operacion, recargamos el listado por
				// la primera pagina
				updateCurrentList(1);
			}
		}
	}

	@Override
	public void requestOperationFailed(final int operation,
			final RequestResult request, final Throwable t) {

		synchronized (this) {

			if (operation == SIGN_OPERATION) {
				this.numRequestToSignPending--;
			} else if (operation == APPROVE_OPERATION) {
				this.numRequestToApprovePending--;
			} else if (operation == REJECT_OPERATION) {
				this.numRequestToRejectPending--;
			}

			if (this.numRequestToSignPending <= 0
					&& this.numRequestToApprovePending <= 0
					&& this.numRequestToRejectPending <= 0) {

				if (t != null) {
					Log.e(SFConstants.LOG_TAG, "Error al procesar las peticiones de firma", t); //$NON-NLS-1$
				}

				setVisibilityLoadingMessage(false, null, null);

				final String errorMsg = getString(operation == REJECT_OPERATION ? R.string.error_msg_rejecting_requests
						: R.string.error_msg_procesing_requests);
				Log.w(SFConstants.LOG_TAG, "Error: " + errorMsg); //$NON-NLS-1$
				showErrorDialog(DIALOG_ERROR_PROCESSING, errorMsg);
			}
		}
	}

	@Override
	public void onBackPressed() {
		// Preguntamos si debe cerrarse la sesion
		showConfirmExitDialog();
	}

	/**
	 * Muestra un mensaje al usuario pidiendo confirmacion para cerrar la
	 * sesi&oacute;n del usuario.
	 */
	private void showConfirmExitDialog() {

		final CustomAlertDialog dialog = CustomAlertDialog.newInstance(
				DIALOG_CONFIRM_EXIT,
				getString(R.string.dialog_title_close_session),
				getString(R.string.dialog_msg_close_session),
				getString(android.R.string.ok),
				getString(android.R.string.cancel),
				this);

		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				dialog.show(getSupportFragmentManager(), DIALOG_TAG);
			}
		});
	}

	@Override
	public void loadedSignRequest(final List<SignRequest> signRequests, final int pageNumber, final int numOfPages) {

		// Se termina la carga
		setVisibilityLoadingMessage(false, null, null);

		// Mostramos u ocultamos el texto de "No hay resultados" segun
		// corresponda
		final TextView emptyTextView = findViewById(R.id.empty);
		emptyTextView.setVisibility(signRequests == null || signRequests
				.size() == 0 ? View.VISIBLE : View.INVISIBLE);
		emptyTextView.setText(getString(R.string.no_request_avaible));


		// Ya no tenemos que recargar el listado
		this.needReload = false;

		// Guardamos el numero de paginas del listado
		this.numPages = numOfPages;

		// Mostramos el listado de peticiones
		((ListView) findViewById(R.id.list)).setAdapter(preparePetitionList(signRequests,
				this.currentState, numOfPages > 1));
	}

	/**
	 * Preparamos las peticiones para mostrarlas, anteponiendo las proximas a caducar
	 * y ajustando el layout de los datos.
	 * @param signRequests Peticiones de firma.
	 * @param state Estado de las peticiones (pendientes, procesadas o rechazadas).
	 * @param needPagination Indica si debe mostrarse el elemento para la paginaci&oacute;n.
	 * @return Listado de peticiones.
	 */
	private PetitionListArrayAdapter preparePetitionList(
			final List<SignRequest> signRequests, final String state, final boolean needPagination) {

		final List<PetitionListAdapterItem> plAdapterItem = new ArrayList<>();

		// En el caso del listado de peticiones pendientes, situaremos al principio de la lista
		// las peticiones proximas a caducar. Para ello, las extraeremos de la lista, las
		// ordenaremos por fecha de caducidad y las agregaremos al principio del listado final

		if (SignRequest.STATE_UNRESOLVED.equals(state)) {
			// Obtenemos el numero de dias para el cual consideramos una peticion que caduca proximamente
			int days = AppPreferences.getInstance().getPreferenceInt(KEY_DAYS_TO_EXPIRE, DEFAULT_DAYS_TO_EXPIRE);

			// Identificamos la fecha maxima de caducidad
			DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
			Calendar c = Calendar.getInstance();
			c.setTime(new Date());
			c.add(Calendar.DATE, days);
			Date dateFuture = c.getTime();

			// Creamos una lista con las peticiones proximas a caducar
			List<SignRequest> priorityList = new ArrayList<>();
			for (int i = signRequests.size() - 1; i >= 0 ; i--) {
				SignRequest signRequest  = signRequests.get(i);
				if (signRequest.getExpirationDate() != null) {
					// Obtenemos la fecha de caducidad
					Date date;
					try {
						date = dateFormat.parse(signRequest.getExpirationDate());
					}
					catch (Exception e) {
						Log.w(SFConstants.LOG_TAG, "Fecha de caducidad mal formada. Se ignorara: " + e);
						continue;
					}

					// Comprobamos si la fecha de caducidad es menor que la fecha limite
					if (dateFuture.compareTo(date) > -1) {
						priorityList.add(signRequest);
						signRequests.remove(i);
					}
				}
			}

			// Si se han identificado fechas proximas a caducar, las ordenamos y las agregamos al listado final
			if (priorityList.size() > 0) {

				// Ordenamos
				Collections.sort(priorityList, new SignRequestComparator(dateFormat));

				// Insertamos la cabecera de peticiones proximas a caducar
				plAdapterItem.add(new PanelTitle(getString(R.string.exp_date_requests), this));

				// Insertamos en el listado las peticiones a punto de caducar
				for (final SignRequest request : priorityList) {
					plAdapterItem.add(new PetitionElement(
									request,
									R.layout.array_adapter_unresolved_request,
									this));
				}

				// Si hay fechas no proximas a caducar, agregamos otra cabecera. Esta es innecesaria,
				// si no las hubiese
				plAdapterItem.add(new PanelTitle(getString(R.string.reminding_requests), this));
			}
		}

		// Incluimos el resto de peticiones
		for (final SignRequest request : signRequests) {
			plAdapterItem.add(new PetitionElement(
							request,
							SignRequest.STATE_UNRESOLVED.equals(state) ?
									R.layout.array_adapter_unresolved_request
									: R.layout.array_adapter_resolved_request,
							this));
		}

		// Agregamos pie de pagina si detectamos que es necesaria paginacion
		if (needPagination) {
			plAdapterItem.add(new PanelPaginationElement(this.numPages, this.currentPage, this));
		}

		return new PetitionListArrayAdapter(this, plAdapterItem);
	}

	/**
	 * Interfaz que implementan los tipos de elemento que componen la lista de
	 * peticiones
	 */
	private interface PetitionListAdapterItem {
		public int getViewType();

		public View getView(LayoutInflater inflater, View convertView,
                            int position);
	}

	/**
	 * Tipo de elemento de la lista de peticiones (petici&oacute;n, panel de
	 * paginaci&oacute;n).
	 */
	private enum PetitionListItemType {
		PETITION_ITEM, PAGINATION_PANEL
	}

	private class PetitionElement implements PetitionListAdapterItem {

		final SignRequest request;
		private final int layoutId;
		private final Context context;

		public PetitionElement(final SignRequest signRequest, final int layoutResId, final Context context) {
			this.request = signRequest;
			this.layoutId = layoutResId;
			this.context = context;
		}

		@Override
		public int getViewType() {
			return PetitionListItemType.PETITION_ITEM.ordinal();
		}

		@Override
		public View getView(final LayoutInflater inflater, final View convertView,
				final int position) {

			View v = convertView;

			if (v == null) {
				final LayoutInflater vi = (LayoutInflater) this.context
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				v = vi.inflate(this.layoutId, null);
			}

			if (this.request != null) {

				final ImageView icon = v.findViewById(R.id.priorityIcon);
				final TextView primaryText = v.findViewById(R.id.primaryText);
				final TextView secondaryText = v.findViewById(R.id.secondaryText);
				final ImageView typeIcon = v.findViewById(R.id.typeIcon);
				final TextView dateText = v.findViewById(R.id.dateText);

				primaryText.setText(this.request.getSender());

				if (this.request.getSubject() != null) {
					secondaryText.setText(this.request.getSubject());
				}

				if (dateText != null && this.request.getDate() != null) {
					dateText.setText(this.request.getDate());
				}

				// Cada tiene un icono
				if (this.request.getPriority() > 0
						&& this.request.getPriority() <= 4) {
					int iconResourceId;
					if (this.request.getPriority() == 1) {
						// Icono en transparente
						iconResourceId = R.drawable.icon_priority_1;
					} else if (this.request.getPriority() == 2) {
						// Icono amarillo
						iconResourceId = R.drawable.icon_priority_2;
					} else if (this.request.getPriority() == 3) {
						// Icono naranja
						iconResourceId = R.drawable.icon_priority_3;
					} else {
						// Icono rojo
						iconResourceId = R.drawable.icon_priority_4;
					}
					icon.setImageResource(iconResourceId);
				}

				if (SignRequest.VIEW_NEW.equals(this.request.getView())) {
					v.setBackgroundResource(R.drawable.array_adapter_selector_grey);
					primaryText.setTextAppearance(this.context, R.style.NewRequestPrimaryText);
					secondaryText.setTextAppearance(this.context, R.style.NewRequestSecondaryText);
					if (dateText != null) {
						dateText.setTextAppearance(this.context, R.style.NewRequestPrimaryText);
					}
				}
				else if (SignRequest.VIEW_READED.equals(this.request.getView())) {
					v.setBackgroundResource(R.drawable.array_adapter_selector_white);
					primaryText.setTextAppearance(this.context, R.style.ReadedRequestPrimaryText);
					secondaryText.setTextAppearance(this.context, R.style.ReadedRequestSecondaryText);
					if (dateText != null) {
						dateText.setTextAppearance(this.context, R.style.ReadedRequestPrimaryText);
					}
				}

				// Icono diferente dependiendo de si la peticion es de fima o de
				// visto bueno
				if (this.request.getType() == RequestType.APPROVE) {
					typeIcon.setImageResource(R.drawable.icon_vb);
				} else if (this.request.getType() == RequestType.SIGNATURE) {
					typeIcon.setImageResource(R.drawable.icon_sign);
				}

				final CheckBox check = (CheckBox) v.findViewById(R.id.check);
				if (check != null) {
					// Si se pulsa el checkbutton, se selecciona/deselecciona el
					// elemento
					check.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
						@Override
						public void onCheckedChanged(final CompoundButton arg0,
								final boolean checked) {
							PetitionElement.this.request.setSelected(checked);
						}
					});
					check.setChecked(this.request.isSelected());
				}

			}

			return v;
		}

		SignRequest getSignRequest() {
			return this.request;
		}
	}

	/**
	 * Adaptador para la lista de l&iacute;neas de firma.
	 */
	class PetitionListArrayAdapter extends ArrayAdapter<PetitionListAdapterItem> {

		private final LayoutInflater mInflater;

		PetitionListArrayAdapter(final Context context, final List<PetitionListAdapterItem> objects) {
			super(context, 0, objects);
			this.mInflater = LayoutInflater.from(context);
		}

		@Override
		public int getViewTypeCount() {
			return PetitionListItemType.values().length;
		}

		@Override
		public int getItemViewType(final int position) {
			return getItem(position).getViewType();
		}

		@Override
		public View getView(final int position, final View convertView, final ViewGroup parent) {
			return getItem(position).getView(this.mInflater, convertView, position);
		}
	}

	private class PanelPaginationElement implements PetitionListAdapterItem {

		final int nPages;
		final int page;
		final Context context;

		PanelPaginationElement(final int numPages, final int currentPage, final Context context) {
			this.nPages = numPages;
			this.page = currentPage;
			this.context = context;
		}

		@Override
		public int getViewType() {
			return PetitionListItemType.PAGINATION_PANEL.ordinal();
		}

		@Override
		public View getView(final LayoutInflater inflater, final View convertView, final int position) {

			View paginationView = convertView;
			if (paginationView == null) {
				paginationView = ((LayoutInflater) this.context
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE))
						.inflate(R.layout.array_adapter_panel_paginacion, null);
			}

			View pagButton = paginationView.findViewById(R.id.arrowFirst);
			pagButton.setVisibility(this.page == 1 ? View.INVISIBLE : View.VISIBLE);
			pagButton.setOnClickListener(
					new OnClickListener() {
						@Override
						public void onClick(final View v) {
							v.setSelected(true);
							updateCurrentList(1);
						}
					});

			pagButton = paginationView.findViewById(R.id.arrowLeft);
			pagButton.setVisibility(this.page == 1 ? View.INVISIBLE : View.VISIBLE);
			pagButton.setOnClickListener(
					new OnClickListener() {
						@Override
						public void onClick(final View v) {
							v.setSelected(true);
							updateCurrentList(PanelPaginationElement.this.page - 1);
						}
					});

			pagButton = paginationView.findViewById(R.id.arrowRigth);
			pagButton.setVisibility(this.page == this.nPages ? View.INVISIBLE : View.VISIBLE);
			pagButton.setOnClickListener(
					new OnClickListener() {
						@Override
						public void onClick(final View v) {
							v.setSelected(true);
							updateCurrentList(PanelPaginationElement.this.page + 1);
						}
					});

			pagButton = paginationView.findViewById(R.id.arrowLast);
			pagButton.setVisibility(this.page == this.nPages ? View.INVISIBLE : View.VISIBLE);
			pagButton.setOnClickListener(
					new OnClickListener() {
						@Override
						public void onClick(final View v) {
							v.setSelected(true);
							updateCurrentList(PanelPaginationElement.this.nPages);
						}
					});

			((TextView) paginationView.findViewById(R.id.paginationText)).setText(
					getString(R.string.pagination_separator, this.page, this.nPages));

			return paginationView;
		}
	}

	@Override
	public void onItemClick(final AdapterView<?> adapter, final View view, final int position, final long id) {

		// Solo procesamos los click sobre los elementos de peticion, no sobre
		// la barra de paginacion
		if (((PetitionListAdapterItem) adapter.getItemAtPosition(position)).getViewType() ==
				PetitionListItemType.PETITION_ITEM.ordinal()) {

			final SignRequest signRequest = ((PetitionElement) adapter.getItemAtPosition(position))
					.getSignRequest();
			signRequest.setViewed(true);
			showRequestDetails(signRequest.getId());
		}
	}

	/**
	 * Abre un activity en donde muestra el detalle de la petici&oacute;n.
	 *
	 * @param requestId
	 *            Identificador de la petici&oacute;n de la que se quiere ver el
	 *            detalle.
	 */
	private void showRequestDetails(final String requestId) {

		final Intent changeActivityIntent = new Intent(this, PetitionDetailsActivity.class);
		changeActivityIntent.putExtra( PetitionDetailsActivity.EXTRA_RESOURCE_REQUEST_STATE, getCurrentState());
		changeActivityIntent.putExtra( PetitionDetailsActivity.EXTRA_RESOURCE_REQUEST_ID, requestId);
		changeActivityIntent.putExtra( PetitionDetailsActivity.EXTRA_RESOURCE_CERT_B64, this.certB64);
		changeActivityIntent.putExtra( PetitionDetailsActivity.EXTRA_RESOURCE_CERT_ALIAS, this.certAlias);

		startActivityForResult(changeActivityIntent, PetitionDetailsActivity.REQUEST_CODE);
	}

	@Override
	protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == PetitionDetailsActivity.REQUEST_CODE) {
			// Si se proceso le peticion correctamente actualizamos el listado
			if (resultCode == PetitionDetailsActivity.RESULT_SIGN_OK || resultCode == PetitionDetailsActivity.RESULT_REJECT_OK) {
				updateCurrentList(1);
			}
			// Si se trato de firmar la peticion y fallo, se muestra el error
			else if (resultCode == PetitionDetailsActivity.RESULT_SIGN_FAILED) {
				Log.e(SFConstants.LOG_TAG, "Error al firmar la peticion desde la actividad de detalle"); //$NON-NLS-1$
				showErrorDialog(DIALOG_RESULT_SIMPLE_REQUEST, getString(R.string.error_msg_procesing_request));
			}
			// Si se trato de rechazar la peticion y fallo, se muestra el error
			else if (resultCode == PetitionDetailsActivity.RESULT_REJECT_FAILED) {
				Log.e(SFConstants.LOG_TAG, "Error al rechazar la peticion desde la actividad de detalle"); //$NON-NLS-1$
				showErrorDialog(DIALOG_RESULT_SIMPLE_REQUEST, getString(R.string.error_msg_rejecting_request));
			}
			// Si ha caducado la sesion vuelve a la actividad principal
			else if (resultCode == PetitionDetailsActivity.RESULT_SESSION_FAILED) {
				this.lostSession();
			}
			// Si se ha cerrado la sesion, se vuelve a la actividad principal
			else if (resultCode == PetitionDetailsActivity.RESULT_SESSION_CLOSED) {
				this.closeActivity();
			}
		}

//		// Obtenemos la respuesta tras autorizar la operacion en Cl@ve Firma
//		else if (requestCode == FireWebViewActivity.REQUEST_CODE) {
//			// Si la peticion ha sido correcta iniciamos la finalizacion de firma
//			if (resultCode == RESULT_OK) {
//
//				Log.w(SFConstants.LOG_TAG, "Iniciamos la AsyncTask de la PostFirma");
//				// Se inicia el WebView
//				ClaveFirmaPostSignTask cct = new ClaveFirmaPostSignTask(this.firePreSignResult, this, this);
//				//showProgressDialog(getString(R.string.dialog_msg_clave),null);
//				cct.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
//			}
//			else {
//				showToastMessage(getString(R.string.toast_msg_fire_comunication_ko));
//			}
//		}
	}

	@Override
	public boolean onPrepareOptionsMenu(final Menu menu) {

		if (this.filterConfig != null) {
			menu.findItem(R.id.no_filter).setVisible(
					!FilterConfig.isDefaultConfig(this.filterConfig));
		}

		// Mostramos el elemento para la seleccion o deseleccion de todas las
		// peticiones segun corresponda
		if (menu.findItem(R.id.select_all) != null) {
			menu.findItem(R.id.select_all).setVisible(!this.allSelected);
			menu.findItem(R.id.unselect_all).setVisible(this.allSelected);
		}

		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public void errorLoadingSignRequest() {

		// Se termina la carga
		setVisibilityLoadingMessage(false, null, null);
		// TODO: Eliminar para que se recargue la pagina en caso de error
		// Ya no tenemos que recargar el listado
		this.needReload = false;

		// Mostramos texto de falta de peticiones
		final TextView emptyTextView = (TextView) findViewById(R.id.empty);
		emptyTextView.setText(getString(R.string.error_msg_loading_requests));
		emptyTextView.setVisibility(View.VISIBLE);
	}

	@Override
	public void lostSession() {
        if (!openedFromNotification) {
            showToastMessage(ErrorManager.getErrorMessage(ErrorManager.ERROR_LOST_SESSION));
        }
		this.closeActivity();
	}

	/**
	 * Muestra un mensaje en un toast.
	 * @param message
	 */
	void showToastMessage(final String message) {
		dismissProgressDialog();
		this.runOnUiThread(new Runnable() {
			public void run() {
				Toast.makeText(PetitionListActivity.this, message, Toast.LENGTH_SHORT).show();
			}
		});
	}
	/**
	 * Establece la visibilidad de un mensaje/s&iacute;mbolo de carga.
	 *
	 * @param visible
	 *            Establece si el mensage sera visible o no.
	 */
	void setVisibilityLoadingMessage(final boolean visible, final RejectRequestsTask rrt,
			final LoadSignRequestsTask lsrt) {
		if (visible) {
			if (rrt != null) {
				showProgressDialog(
						getString(R.string.dialog_msg_loading_petitions), rrt,
						null);
			} else if (lsrt != null) {
				showProgressDialog(
						getString(R.string.dialog_msg_loading_petitions), null,
						lsrt);
			} else {
				showProgressDialog(
						getString(R.string.dialog_msg_loading_petitions), null,
						null);
			}
		} else {
			dismissProgressDialog();
		}
	}

	/**
	 * Establece la visibilidad de un mensaje/s&iacute;mbolo de carga.
	 *
	 * @param visible
	 *            Establece si el mensage sera visible o no.
	 */
	void setVisibilityProgressMessage(final boolean visible) {
		if (visible) {
			showProgressDialog(
					getString(R.string.dialog_msg_processing_requests), null,
					null);
		} else {
			dismissProgressDialog();
		}
	}

	@Override
	protected void onSaveInstanceState(final Bundle outState) {
		super.onSaveInstanceState(outState);

		outState.putBoolean(KEY_SAVEINSTANCE_NEED_RELOAD, this.needReload);
		outState.putBoolean(KEY_SAVEINSTANCE_LOADING, this.loadingRequests);
	}

	private ConfigureFilterDialogBuilder filterDialogBuilder;

	ConfigureFilterDialogBuilder getFilterDialogBuilder() {
		return this.filterDialogBuilder;
	}

	@Override
	protected Dialog onCreateDialog(final int id,
			final Bundle savedInstanceState) {

		this.filterDialogBuilder = new ConfigureFilterDialogBuilder(
				savedInstanceState, this.appIds, this.appNames, this);
		this.filterDialogBuilder.setPositiveButton(R.string.ok,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(final DialogInterface dialog, final int identifier) {
						setFilterConfig(getFilterDialogBuilder().getFilterConfig());
						invalidateOptionsMenu();
						updateCurrentList(1);
					}
				});
		this.filterDialogBuilder.setNegativeButton(R.string.cancel, null);

		return this.filterDialogBuilder.create();
	}

	@Override
	public void onDialogPositiveClick(final int dialogId, final String reason) {

		// Dialogo de confirmacion de cierre de sesion
		if (dialogId == DIALOG_CONFIRM_EXIT) {
			CryptoConfiguration.setCertificateAlias(null);
			CryptoConfiguration.setCertificatePrivateKeyEntry(null);
			try {
				LogoutRequestTask lrt = new LogoutRequestTask(CommManager.getProxyConnector());
				lrt.execute();
			} catch (Exception e) {
				Log.e(SFConstants.LOG_TAG,
						"No se ha podido cerrar sesion: " + e); //$NON-NLS-1$
			}
			closeActivity();
		}
		// Dialogo de confirmacion de rechazo de peticiones
		else if (dialogId == DIALOG_CONFIRM_REJECT) {

			final SignRequest[] selectedRequests = getSelectedRequests();
			if (selectedRequests == null || selectedRequests.length == 0) {
				// Esto no deberia ocurrir nunca, ya que se se comprobo anteriormente que
				// hubiese peticiones seleccionadas
				showNoSelectedRequestDialog();
				return;
			}

			this.numRequestToRejectPending = selectedRequests.length;
			setVisibilityLoadingMessage(true, rejectRequests(reason, selectedRequests),
					null);

		}
		// Dialogo de confirmacion de firma de peticiones
		else if (dialogId == DIALOG_CONFIRM_SIGN) {

			setVisibilityProgressMessage(true);

			// Comprobamos que haya elementos seleccionados
			final SignRequest[] requests = getSelectedRequests();
			if (requests == null || requests.length == 0) {
				showNoSelectedRequestDialog();
				return;
			}

			// Separamos entre peticiones de firma y de visto bueno
			final List<SignRequest> requestToSign = new ArrayList<>();
			final List<SignRequest> requestToApprove = new ArrayList<>();
			for (final SignRequest req : requests) {
				if (req.getType() == RequestType.SIGNATURE) {
					requestToSign.add(req);
				} else {
					requestToApprove.add(req);
				}
			}
			// Mandamos a aprobar las peticiones de visto bueno
			this.numRequestToApprovePending = requestToApprove.size();
			if (this.numRequestToApprovePending > 0) {
				Log.i(SFConstants.LOG_TAG, "Peticiones para aprobar: " + this.numRequestToApprovePending); //$NON-NLS-1$
				approveRequests(
						requestToApprove.toArray(new SignRequest[requestToApprove.size()]));
			}

			// Mandamos a firmar las peticiones de firma
			this.numRequestToSignPending = requestToSign.size();
			if (this.numRequestToSignPending > 0) {
				Log.i(SFConstants.LOG_TAG, "Peticiones para firmar: " + this.numRequestToSignPending); //$NON-NLS-1$
				// Dependiendo de si firmamos con certificado local o con Cl@ve Firma
//				if(AppPreferences.getInstance().getEnabledClavefirma()) {
//					doSignWithClaveFirma(requestToSign.toArray(new SignRequest[requestToSign.size()]));
//				}
//				else {
					signRequets(
							this.certAlias,
							requestToSign.toArray(new SignRequest[requestToSign.size()]));
//				}
			}
		}
		// Dialogo de error procesando peticiones
		else if (dialogId == DIALOG_ERROR_PROCESSING) {
			// Actualizamos el listado
			updateCurrentList(1);
		}
	}

	/**
	 * Manda a aprobar el listado de peticiones indicado. Llama a la
	 * operaci&oacute;n de &eacute;xito o fallo del listener una vez por cada
	 * petici&oacute;n.
	 *
	 * @param requests
	 *            Listado de peticiones de visto bueno.
	 */
	private void approveRequests(final SignRequest[] requests) {
		new ApproveRequestsTask(requests,
				CommManager.getProxyConnector(), this).execute();
	}

	/** Manda a firmar (a traves de una tarea as&iacute;ncrona) el listado de
	 * peticiones de firma indicado. Llamar&aacute; a la operaci&oacute;n de
	 * &eacute;xito o fallo del listener, una vez por cada petici&oacute;n
	 * procesada.
	 *
	 * @param alias Alias del certificado de firma que se debe usar.
	 * @param requests Listado de peticiones de firma. */
	private void signRequets(final String alias, final SignRequest[] requests) {
		new RequestSigner(alias, this, this).sign(requests);
	}

//	private void doSignWithClaveFirma(final SignRequest[] requests) {
//		// Se inicia el WebView
//		ClaveFirmaPreSignTask cct = new ClaveFirmaPreSignTask(requests, this, this);
//		//showProgressDialog(getString(R.string.dialog_msg_clave),null);
//		cct.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
//	}

	@Override
	public void onDialogNegativeClick(final int dialogId) {
		// No se implementa comportamiento
	}

	/** Cierra el di&aacute;logo de espera en caso de estar abierto. */
	void dismissProgressDialog() {
		this.loadingRequests = false;
		if (getProgressDialog() != null) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					getProgressDialog().dismiss();
				}
			});
		}
	}

	/** Muestra un di&aacute;logo de espera con un mensaje. */
	private void showProgressDialog(final String message,
			final RejectRequestsTask rrt, final LoadSignRequestsTask lsrt) {
		this.loadingRequests = true;

		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				try {
					setProgressDialog(ProgressDialog.show(
							PetitionListActivity.this, null, message, true));
					getProgressDialog().setOnKeyListener(new OnKeyListener() {
						@Override
						public boolean onKey(final DialogInterface dialog,
								final int keyCode, final KeyEvent event) {
							if (keyCode == KeyEvent.KEYCODE_BACK) {
								// comprobamos si se esta ejecutando alguna
								// tarea para cancelarla
								if (rrt != null) {
									rrt.cancel(true);
								} else if (lsrt != null) {
									lsrt.cancel(true);
								}
								dismissProgressDialog();
								return true;
							}
							return false;
						}
					});
				} catch (final Exception e) {
					Log.e(SFConstants.LOG_TAG,
							"No se ha podido mostrar el dialogo de progreso: " + e); //$NON-NLS-1$
				}
			}
		});
	}


//	/**
//	 * Cuando se finaliza correctamente el llamada a FIRe que procesa las peticiones,
//	 * recibimos el identificador de la transaccion de FIRe y la URL de redireccion
//	 * a la pagina web desde la que hacer la autorizaci&oacute;n.
//	 * @param firePreSignResult Informacion de prefirma y de la transaccion de FIRe para permitir la
//	 * autorizaci&oacute;n del usuario.
//	 */
//	@Override
//	public void firePreSignSuccess(FirePreSignResult firePreSignResult) {
//
//		// Almacenamos la informacion trifasica para reutilizarla al solicitar las postfirmas
//		this.firePreSignResult = firePreSignResult;
//
//		Log.w(SFConstants.LOG_TAG, "Recibido del PreSignTask:\n" + this.firePreSignResult.toString());
//
//		// Abrimos una actividad con un WebView en la que se muestre la URL recibida
//		Bundle bundle = new Bundle();
//		bundle.putString(FireWebViewActivity.EXTRA_PARAM_TRANSACTION_ID, firePreSignResult.getTransactionId());
//		bundle.putString(FireWebViewActivity.EXTRA_PARAM_URL, firePreSignResult.getURL());
//
//		Intent intent = new Intent(this, FireWebViewActivity.class);
//		intent.putExtras(bundle);
//		startActivityForResult(intent, FireWebViewActivity.REQUEST_CODE);
//	}
//
//	@Override
//	public void firePreSignFailed(Throwable cause) {
//		Log.e(SFConstants.LOG_TAG, "Ha fallado en la prefirma la operacion de firma con Cl@ve Firma", cause); //$NON-NLS-1$
//
//		synchronized (this) {
//			this.numRequestToSignPending = 0;
//
//			if (numRequestToSignPending <= 0 && this.numRequestToApprovePending <= 0) {
//
//				setVisibilityLoadingMessage(false, null, null);
//
//				final String errorMsg = getString(R.string.error_msg_procesing_requests);
//				Log.w(SFConstants.LOG_TAG, "Error: " + errorMsg); //$NON-NLS-1$
//				showErrorDialog(DIALOG_ERROR_PROCESSING, errorMsg);
//			}
//		}
//	}
//
//
//	@Override
//	public void firePostSignSuccess(RequestResult[] requestResults) {
//
//		synchronized (this) {
//			this.numRequestToSignPending = 0;
//
//			if (this.numRequestToSignPending <= 0
//					&& this.numRequestToApprovePending <= 0) {
//				setVisibilityLoadingMessage(false, null, null);
//
//				// Una vez finalizada una operacion, recargamos el listado por
//				// la primera pagina
//				updateCurrentList(1);
//			}
//		}
//	}
//
//	@Override
//	public void firePostSignFailed(Throwable cause) {
//		Log.e(SFConstants.LOG_TAG, "Ha fallado en la postfirma la operacion de firma con Cl@ve Firma", cause); //$NON-NLS-1$
//
//		synchronized (this) {
//			this.numRequestToSignPending = 0;
//
//			if (this.numRequestToSignPending <= 0
//					&& this.numRequestToApprovePending <= 0) {
//
//				setVisibilityLoadingMessage(false, null, null);
//
//				final String errorMsg = getString(R.string.error_msg_procesing_requests);
//				Log.w(SFConstants.LOG_TAG, "Error: " + errorMsg); //$NON-NLS-1$
//				showErrorDialog(DIALOG_ERROR_PROCESSING, errorMsg);
//			}
//		}
//	}

	// Funciones para implementar el registro de notificaciones GCM
	private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

	/**
	 * Check the device to make sure it has the Google Play Services APK. If
	 * it doesn't, display a dialog that allows users to download the APK from
	 * the Google Play Store or enable it in the device's system settings.
	 */
	private boolean checkPlayServices() {
		GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
		int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
		if (resultCode != ConnectionResult.SUCCESS) {
			if (apiAvailability.isUserResolvableError(resultCode)) {
				apiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST)
						.show();
			} else {
				Log.w(SFConstants.LOG_TAG, "Dispositivo no soportado.");
				Toast.makeText(this, R.string.error_msg_unsupported_device, Toast.LENGTH_SHORT).show();
				finish();
			}
			return false;
		}
		return true;
	}

	private void changesNotificationState() {

		// Comprobamos si el usuario ya esta registrado en el sistema de notificaciones

		// TODO: Cuando este disponible un servicio para la baja en el sistema de notificaciones, lo usaremos
		// Si el usuario ya esta registrado en el servicio de notificaciones, lo damos de baja

		// Si aun no se ha registrado, lo hacemos
		String userProxyId = getUserProxyId();
		Intent intent = new Intent(this, RegistrationIntentService.class);
		intent.putExtra(RegistrationIntentService.EXTRA_RESOURCE_CERT_B64, this.certB64);
		intent.putExtra(RegistrationIntentService.EXTRA_RESOURCE_USER_PROXY_ID, userProxyId);
        intent.putExtra(RegistrationIntentService.EXTRA_RESOURCE_TOKEN, AppPreferences.getInstance().getCurrentToken());

		startService(intent);
	}

	private BroadcastReceiver  bReceiver = new BroadcastReceiver(){

		@Override
		public void onReceive(Context context, Intent intent) {
			if(intent.getExtras().getBoolean("success")) {
                menuRef.findItem(R.id.notifications).setVisible(false);
            }

            // Si el registro no es una actualizacion automatica del token, mostramos al usuario el resultado de la operacion
            if (!intent.getExtras().getBoolean("background")) {
                if (intent.getExtras().getBoolean("success")) {
                    Toast.makeText(context, R.string.toast_msg_gcm_connection_ok, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, R.string.toast_msg_gcm_connection_ko, Toast.LENGTH_SHORT).show();
                }
            }

		}
	};

	protected void onResume(){
		super.onResume();
		this.openedFromNotification = getIntent().getBooleanExtra(EXTRA_RESOURCE_OPEN_FROM_NOTIFICATION, false);
		LocalBroadcastManager.getInstance(this).registerReceiver(bReceiver, new IntentFilter("message"));
	}

	protected void onPause (){
		super.onPause();
		LocalBroadcastManager.getInstance(this).unregisterReceiver(bReceiver);
	}

	private class PanelTitle implements PetitionListAdapterItem {

		final String text;
		final Context context;

		PanelTitle(final String text, final Context context) {
			this.text = text;
			this.context = context;
		}

		@Override
		public int getViewType() {
			return PetitionListItemType.PAGINATION_PANEL.ordinal();
		}

		@Override
		public View getView(final LayoutInflater inflater, final View convertView, final int position) {

			View paginationView = convertView;
			if (paginationView == null) {
				paginationView = ((LayoutInflater) this.context
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE))
						.inflate(R.layout.array_adapter_header, null);
			}

			((TextView) paginationView.findViewById(R.id.header)).setText(this.text);/*
					getString(R.string.pagination_separator, Integer.valueOf(this.page), Integer.valueOf(this.nPages)));*/

			return paginationView;
		}
	}

	/** Clase para la comparacion de peticiones por fecha de caducidad. */
	private class SignRequestComparator implements Comparator<SignRequest> {

		DateFormat dateFormat;

		SignRequestComparator(DateFormat dateFormat) {
			this.dateFormat = dateFormat;
		}

		@Override
		public int compare(final SignRequest object1, final SignRequest object2) {
			Date expDate1;
			Date expDate2;
			try {
				expDate1 = this.dateFormat.parse(object1.getExpirationDate());
				expDate2 = this.dateFormat.parse(object2.getExpirationDate());
				return expDate1.compareTo(expDate2);
			} catch (ParseException e) {
				return object1.getExpirationDate().compareTo(object2.getExpirationDate());
			}
		}
	}
}