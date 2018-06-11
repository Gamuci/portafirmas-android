package es.gob.afirma.android.signfolder;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnShowListener;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import java.net.URL;
import java.util.Collections;
import java.util.List;

/**
 * Clase para la construcci&oacute;n del di&aacute;logo que muestra al usuario las opciones de
 * configuraci&oacute;n del acceso a los distintos Portafirmas.
 */
final class LoginOptionsDialogBuilder {

	private final LoginOptionsListener listener;

	private final AlertDialog alertDialog;

    private CharSequence[] items;

	private int selectedServer;

	LoginOptionsDialogBuilder(final Context context, final LoginOptionsListener listener) {

		this.listener = listener;
        final LayoutInflater inflater = LayoutInflater.from(context);

		List<String> servers = AppPreferences.getInstance().getServersList();

		// Si no hay ningun servidor configurado, reestablecemos los por defecto
		if (servers.isEmpty()) {
			AppPreferences.getInstance().setDefaultServers();
			servers = AppPreferences.getInstance().getServersList();
		}

		// Ordenamos por alias
		Collections.sort(servers);
		this.items = servers.toArray(new CharSequence[servers.size()]);
		this.selectedServer = servers.indexOf(AppPreferences.getInstance().getSelectedProxyAlias());

		// Si no hay ningun servidor marcado como por defecto, marcamos el primero
		if (this.selectedServer == -1) {
			this.selectedServer = 0;
			String selectedServerAlias = this.items[this.selectedServer].toString();
			AppPreferences.getInstance().setSelectedProxy(
					selectedServerAlias,
					AppPreferences.getInstance().getServer(selectedServerAlias));
		}

		// Mostramos el dialogo con los servidores
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setCustomTitle(inflater.inflate(R.layout.dialog_server_title, null));
		builder.setSingleChoiceItems(this.items, this.selectedServer, new OnClickListener() {
			@Override
			public void onClick(final DialogInterface d, final int n) {
				setSelectedServer(n);
			}
		});

		builder.setPositiveButton(R.string.ok,
			new OnClickListener() {
				@Override
				public void onClick(final DialogInterface dialog, final int which) {
					final String selectedAlias = getItems()[getSelectedServer()].toString();
					AppPreferences.getInstance().setSelectedProxy(
							selectedAlias,
							AppPreferences.getInstance().getServer(selectedAlias)
					);
				}
			}
		);

		builder.setNeutralButton(R.string.dialog_server_new_button,
				new OnClickListener() {
					@Override
					public void onClick(final DialogInterface dialog, final int which) {
						addServer(context, inflater);
					}
			}
		);
		builder.setNegativeButton(R.string.cancel, null);
		this.alertDialog = builder.create();

		if (!servers.isEmpty()) {
			this.alertDialog.setOnShowListener(new OnShowListener()
			{
			    @Override
			public void onShow(final DialogInterface dialog)
			{
			        final ListView lv = getAlertDialog().getListView();
			        lv.setOnItemLongClickListener(new OnItemLongClickListener()
			    {
			    @Override
			    public boolean onItemLongClick(final AdapterView<?> parent, final View view, final int position, final long id)
			    {
			    	editServer(
		    			context,
		    			inflater,
		    			getItems()[position].toString(),
		    			AppPreferences.getInstance().getServer(getItems()[position].toString())
			    	);
					getAlertDialog().dismiss();
					return true;
			    }
			    });
			}
			});
		}
	}

	/**
	 * Muestra un di&aacute;logo para agregar un nuevo servidor proxy al listado de servidores.
	 * @param context Contexto sobre el que mostrar el di&aacute;logo.
	 * @param inflater Para la edici&oacute;n del layout del di&aacute;logo.
	 */
	private void addServer(final Context context, final LayoutInflater inflater) {

		final View view = inflater.inflate(R.layout.dialog_add_server, null);

		final EditText aliasField = view.findViewById(R.id.alias);
		final EditText urlField = view.findViewById(R.id.url);
		aliasField.requestFocus();

		final AlertDialog dialog = new AlertDialog.Builder(context)
				.setView(view)
				.setTitle(R.string.dialog_add_server_title)
				.setPositiveButton(R.string.ok, null)
				.setNegativeButton(R.string.cancel, new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int i) {
						// Abrimos el dialogo de seleccion
						final LoginOptionsDialogBuilder dialogBuilder = new LoginOptionsDialogBuilder(context, getListener());
						dialogBuilder.show();
					}
				}).create();

		dialog.show();

		// Definimos un comportamiento especial en el boton de aceptar para que se validen los
		// campos sin que se cierre el dialogo
		final Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
		positiveButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				String newAlias = aliasField.getText().toString().trim();
				String newUrl = urlField.getText().toString().trim();

				// Comprobamos que no haya campos vacios
				if (newAlias.isEmpty() || newUrl.isEmpty()) {
					getListener().onErrorLoginOptions(context.getString(R.string.dialog_server_empty_fields));
					return;
				}

				// Comprobamos que el nuevo alias no pise a algun otro
				if (!AppPreferences.getInstance().getServer(newAlias).isEmpty()) {
					getListener().onErrorLoginOptions(context.getString(R.string.dialog_server_duplicated));
					return;
				}

				// Comprobamos que la URL este bien formada
				try {
					new URL(newUrl).toString();
				}
				catch (final Exception e) {
					getListener().onErrorLoginOptions(context.getString(R.string.invalid_url));
					return;
				}

				// Guardamos la informacion del nuevo servidor
				AppPreferences.getInstance().saveServer(newAlias, newUrl);

				// Establecemos este servidor como el seleccionado
				AppPreferences.getInstance().setSelectedProxy(newAlias, newUrl);

				// Cerramos el dialogo de edicion y abrimos el de seleccion
				dialog.dismiss();
				final LoginOptionsDialogBuilder dialogBuilder = new LoginOptionsDialogBuilder(context, getListener());
				dialogBuilder.show();
			}
		});
	}

	void editServer(final Context context, final LayoutInflater inflater, final String alias, final String url) {

		final View view = inflater.inflate(R.layout.dialog_add_server, null);

		final EditText aliasField = view.findViewById(R.id.alias);
		final EditText urlField = view.findViewById(R.id.url);

		aliasField.setText(alias);
		urlField.setText(url);

		aliasField.requestFocus();

		final AlertDialog dialog = new AlertDialog.Builder(context)
				.setView(view)
				.setTitle(R.string.dialog_edit_server_title)
				.setPositiveButton(R.string.ok, null)
				.setNegativeButton(R.string.cancel, new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int i) {
						// Abrimos el dialogo de seleccion
						final LoginOptionsDialogBuilder dialogBuilder = new LoginOptionsDialogBuilder(context, getListener());
						dialogBuilder.show();
					}
				})
				.setNeutralButton(R.string.dialog_server_delete_button, new OnClickListener() {
					@Override
					public void onClick(final DialogInterface dialog, final int which) {
						// Eliminamos el proxy
						AppPreferences.getInstance().removeServer(alias);
						// Si era el proxy seleccionado, eliminamos la configuracion
						if (AppPreferences.getInstance().getSelectedProxyAlias().equals(alias)) {
							AppPreferences.getInstance().removeProxyConfig();
						}
						// Abrimos el dialogo de seleccion
						final LoginOptionsDialogBuilder dialogBuilder = new LoginOptionsDialogBuilder(context, getListener());
						dialogBuilder.show();
					}
				}).create();

		dialog.show();

		// Definimos un comportamiento especial en el boton de aceptar para que se validen los
		// campos sin que se cierre el dialogo
		final Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
		positiveButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {

				String newAlias = aliasField.getText().toString().trim();
				String newUrl = urlField.getText().toString().trim();

				// Comprobamos que no haya campos vacios
				if (newAlias.isEmpty() || newUrl.isEmpty()) {
					getListener().onErrorLoginOptions(context.getString(R.string.dialog_server_empty_fields));
					return;
				}

				// Comprobamos que, si se ha cambiado el alias, no pise a algun otro
				if (!newAlias.equals(alias) &&
						!AppPreferences.getInstance().getServer(newAlias).isEmpty()) {
					getListener().onErrorLoginOptions(context.getString(R.string.dialog_server_duplicated));
					return;
				}

				// Comprobamos que la URL este bien formada
				try {
					new URL(newUrl).toString();
				}
				catch (final Exception e) {
					getListener().onErrorLoginOptions(context.getString(R.string.invalid_url));
					return;
				}

				// Eliminamos la configuracion de servidor anterior y guardamos la nueva
				AppPreferences.getInstance().removeServer(alias);
				AppPreferences.getInstance().saveServer(newAlias, newUrl);

				// Establecemos este servidor como el seleccionado
				AppPreferences.getInstance().setSelectedProxy(newAlias, newUrl);

				// Cerramos el dialogo de edicion y abrimos el de seleccion
				dialog.dismiss();
				final LoginOptionsDialogBuilder dialogBuilder = new LoginOptionsDialogBuilder(context, getListener());
				dialogBuilder.show();
			}
		});

	}

	LoginOptionsListener getListener() {
		return this.listener;
	}

	public void show() {
		this.alertDialog.show();
	}

    private AlertDialog getAlertDialog() {
        return this.alertDialog;
    }
    private CharSequence[] getItems() {
        return this.items;
    }
    private int getSelectedServer() {
        return this.selectedServer;
    }
    private void setSelectedServer(final int s) {
        this.selectedServer = s;
    }

	/**
	 * Interfaz a la que se notifica cuando ocurre un error en la configuraci&oacute;n de la
	 * aplicaci&oacute;n.
	 */
	public interface LoginOptionsListener {

		void onErrorLoginOptions(final String url);
	}
}
