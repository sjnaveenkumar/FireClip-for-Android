package com.abara.fireclip;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatTextView;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import com.abara.fireclip.util.FireClipUtils;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.messaging.FirebaseMessaging;

/**
 * Activity to login to FireClip.
 * <p>
 * Created by abara on 24/09/16.
 */
public class LoginActivity extends AppCompatActivity implements View.OnClickListener {

    private FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener authStateListener;

    private TextInputEditText emailBox, passBox;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        firebaseAuth = FirebaseAuth.getInstance();

        emailBox = (TextInputEditText) findViewById(R.id.login_email_box);
        passBox = (TextInputEditText) findViewById(R.id.login_password_box);

        AppCompatButton signInButton = (AppCompatButton) findViewById(R.id.login_email_signin_btn);
        AppCompatTextView forgotPassView = (AppCompatTextView) findViewById(R.id.login_forgot_pass_txt);

        signInButton.setOnClickListener(this);
        forgotPassView.setOnClickListener(this);

        /**
         * Listen for auth changes, start device activity after SignIn and,
         * subscribe to topic, to receive push notification.
         */
        authStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {

                FirebaseUser user = firebaseAuth.getCurrentUser();

                if (user != null) {
                    Intent deviceActivity = DeviceNameActivity.getStarterIntent(LoginActivity.this, false);
                    startActivity(deviceActivity);

                    // Subscribe to user's UID as topic, to receive push notifications.
                    FireClipUtils.subscribe();
                }

            }
        };

    }

    /**
     * Handle clicks for Sign in and launch forget password dialog.
     */
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.login_email_signin_btn:
                signIn();
                break;
            case R.id.login_forgot_pass_txt:
                launchForgotPasswordDialog();
                break;
        }
    }

    /**
     * Show forget password dialog.
     */
    private void launchForgotPasswordDialog() {

        final View view = LayoutInflater.from(this).inflate(R.layout.dialog_forgot_pass, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(view)
                .setPositiveButton(R.string.action_send, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int i) {

                        String email = ((TextInputEditText) view.findViewById(R.id.forgot_dialog_email_box)).getText().toString();

                        if (!email.isEmpty()) {

                            Snackbar.make(findViewById(R.id.login_root_view), "Sending...", Snackbar.LENGTH_SHORT).show();
                            firebaseAuth.sendPasswordResetEmail(email).addOnCompleteListener(LoginActivity.this, new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {

                                    if (!task.isSuccessful()) {
                                        Toast.makeText(LoginActivity.this, "You are not yet registered", Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(LoginActivity.this, "Password reset mail sent", Toast.LENGTH_SHORT).show();
                                    }

                                }
                            });
                        } else {
                            Toast.makeText(LoginActivity.this, "Enter a valid email address", Toast.LENGTH_SHORT).show();
                        }

                    }
                }).
                setNegativeButton(R.string.action_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int i) {
                        dialog.dismiss();
                    }
                });

        AlertDialog dialog = builder.create();
        dialog.show();

    }

    /**
     * Validate and Sign in using email and password.
     */
    private void signIn() {

        String email = emailBox.getText().toString();
        String password = passBox.getText().toString();


        if (!email.isEmpty() && !password.isEmpty()) {

            final ProgressDialog dialog = new ProgressDialog(this);
            dialog.setMessage("Signing in...");
            dialog.setProgressDrawable(new ColorDrawable(Color.parseColor("#FF9800")));
            dialog.setIndeterminate(true);
            dialog.setCancelable(false);
            dialog.show();

            firebaseAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (!task.isSuccessful()) {
                                Toast.makeText(LoginActivity.this, "Sign in failed!", Toast.LENGTH_SHORT).show();
                            }
                            dialog.dismiss();
                        }
                    });

        } else {
            Snackbar.make(findViewById(R.id.login_root_view), "Detail(s) missing.", Snackbar.LENGTH_SHORT).show();
        }

    }

    /**
     * Add auth state listener.
     */
    @Override
    protected void onStart() {
        super.onStart();
        firebaseAuth.addAuthStateListener(authStateListener);
    }

    /**
     * Remove auth state listener.
     */
    @Override
    protected void onStop() {
        super.onStop();
        if (authStateListener != null) {
            firebaseAuth.removeAuthStateListener(authStateListener);
        }
    }

}
