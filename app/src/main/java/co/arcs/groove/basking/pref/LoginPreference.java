package co.arcs.groove.basking.pref;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ViewSwitcher;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import co.arcs.android.util.MainThreadExecutorService;
import co.arcs.groove.basking.R;
import co.arcs.groove.thresher.Client;
import co.arcs.groove.thresher.GroovesharkException.InvalidCredentialsException;

public class LoginPreference extends DialogPreference {

    private EditText usernameField;
    private EditText passwordField;
    private ViewSwitcher container;
    private Button okButton;

    public LoginPreference(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.preferenceStyle);
    }

    public LoginPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        setPersistent(false);
        setPositiveButtonText(android.R.string.ok);
        setNegativeButtonText(android.R.string.cancel);
        setDialogLayoutResource(R.layout.view_login_form);
    }

    @Override
    protected void onAttachedToHierarchy(PreferenceManager preferenceManager) {
        super.onAttachedToHierarchy(preferenceManager);

        String username = getSharedPreferences().getString(PreferenceKeys.USERNAME, null);
        if (username != null) {
            setSummary(username);
        }
    }

    @Override
    protected void showDialog(Bundle state) {
        super.showDialog(state);
        okButton = ((AlertDialog) getDialog()).getButton(DialogInterface.BUTTON_POSITIVE);

        // Replace the button's on click listener with our own
        okButton.setOnClickListener(okButtonOnClickListener);
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        String username = getSharedPreferences().getString(PreferenceKeys.USERNAME, null);
        String password = getSharedPreferences().getString(PreferenceKeys.PASSWORD, null);

        usernameField = (EditText) view.findViewById(R.id.username);
        passwordField = (EditText) view.findViewById(R.id.password);
        container = (ViewSwitcher) view.findViewById(R.id.container);

        usernameField.setText(username);
        passwordField.setText(password);

        if (username != null) {
            usernameField.setSelection(username.length());
        }
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if (positiveResult) {
            Editor edit = getSharedPreferences().edit();
            edit.putString(PreferenceKeys.USERNAME, usernameField.getText().toString());
            edit.putString(PreferenceKeys.PASSWORD, passwordField.getText().toString());
            edit.apply();
            setSummary(usernameField.getText().toString());
        }
    }

    private final View.OnClickListener okButtonOnClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {

            if (!validateUsername()) {
                showUsernameError();
                return;
            }
            if (!validatePassword()) {
                showPasswordError();
                return;
            }

            // Show progress 'bar'
            container.setDisplayedChild(1);
            okButton.setEnabled(false);

            ListenableFuture<Void> loginFuture = validateCredentials(usernameField.getText()
                    .toString(), passwordField.getText().toString());

            Futures.addCallback(loginFuture, new FutureCallback<Void>() {

                @Override
                public void onFailure(Throwable arg0) {
                    if (arg0 instanceof InvalidCredentialsException) {
                        showBadPasswordDialog();
                    } else if (arg0 instanceof IOException) {
                        showContinueAnywayDialog();
                    }
                    container.setDisplayedChild(0);
                    okButton.setEnabled(true);
                }

                @Override
                public void onSuccess(Void arg0) {
                    LoginPreference.this.onClick(getDialog(), DialogInterface.BUTTON_POSITIVE);
                    getDialog().dismiss();
                }
            }, MainThreadExecutorService.get());
        }
    };

    private boolean validateUsername() {
        return (usernameField.getText().length() > 0);
    }

    private boolean validatePassword() {
        // Min length taken from GS website
        return (passwordField.getText().length() >= 5);
    }

    private void showUsernameError() {
        usernameField.setError(getContext().getResources()
                .getString(R.string.pref_error_username_required));
    }

    private void showPasswordError() {
        passwordField.setError(getContext().getResources()
                .getString(R.string.pref_error_password_required));
    }

    /**
     * Check the credentials by attempting to log in. Returns null if
     * successful, else the future will fail with an exception from
     * {@link Client#login(String, String)}.
     */
    private ListenableFuture<Void> validateCredentials(String username, String password) {
        ListeningExecutorService executor = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool());

        return executor.submit(new Callable<Void>() {

            @Override
            public Void call() throws Exception {
                new Client().login(usernameField.getText().toString(),
                        passwordField.getText().toString());
                return null;
            }
        });
    }

    private void showContinueAnywayDialog() {
        Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(R.string.pref_error_noconnection_title);
        builder.setMessage(R.string.pref_error_noconnection_message);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                LoginPreference.this.onClick(dialog, DialogInterface.BUTTON_POSITIVE);
                LoginPreference.this.getDialog().dismiss();
            }
        });
        builder.setNegativeButton(android.R.string.cancel, null);
        builder.create().show();
    }

    private void showBadPasswordDialog() {
        Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(R.string.pref_error_badpassword_title);
        builder.setMessage(R.string.pref_error_badpassword_message);
        builder.setNeutralButton(android.R.string.ok, null);
        builder.create().show();
    }
}
