package com.abara.fireclip;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatTextView;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.abara.fireclip.util.FireClipUtils;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

/**
 * Activity shown first, and when logged out.
 * <p>
 * Created by abara on 29/07/16.
 */
public class IntroActivity extends AppCompatActivity implements View.OnClickListener, GoogleApiClient.OnConnectionFailedListener {

    private static final int RC_SIGN_IN = 9001;

    private FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener authStateListener;
    private GoogleApiClient googleApiClient;

    // Photo URL of the Google account.
    private Uri photoUri;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);

        firebaseAuth = FirebaseAuth.getInstance();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder()
                .requestIdToken(getResources().getString(R.string.fireclip_web_client_id))
                .requestEmail()
                .build();

        googleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();


        AppCompatButton googleSignInButton = (AppCompatButton) findViewById(R.id.intro_google_signin_btn);
        AppCompatButton emailSignInButton = (AppCompatButton) findViewById(R.id.intro_email_signin_btn);
        AppCompatTextView createAccountView = (AppCompatTextView) findViewById(R.id.intro_create_account_txt);

        googleSignInButton.setOnClickListener(this);
        emailSignInButton.setOnClickListener(this);

        /**
         * Listen for auth changes, start device activity after Google SignIn.
         */
        authStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {

                final FirebaseUser user = firebaseAuth.getCurrentUser();

                if (user != null) {

                    FireClipUtils.updateUserDetails(null, photoUri)
                            .addOnCompleteListener(IntroActivity.this, new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    Intent deviceActivity = DeviceNameActivity.getStarterIntent(IntroActivity.this, false);
                                    startActivity(deviceActivity);

                                    // Subscribe to user's UID as topic, to receive push notifications.
                                    FireClipUtils.subscribe();
                                }
                            });
                }

            }
        };

        createAccountView.setOnClickListener(this);

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
     * Handle Sign in with email,
     * Sign in with Google, and
     * Create account.
     */
    @Override
    public void onClick(View view) {

        switch (view.getId()) {
            case R.id.intro_email_signin_btn:
                startActivity(new Intent(this, LoginActivity.class));
                break;
            case R.id.intro_google_signin_btn:
                signInWithGoogle();
                break;
            case R.id.intro_create_account_txt:
                startActivity(new Intent(this, CreateAccountActivity.class));
                break;
        }

    }

    /**
     * Sign in with Google account.
     */
    private void signInWithGoogle() {
        Intent signinIntent = Auth.GoogleSignInApi.getSignInIntent(googleApiClient);
        startActivityForResult(signinIntent, RC_SIGN_IN);
    }

    /**
     * Handle results from Google Sign in.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {

            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);

            if (result.isSuccess()) {
                GoogleSignInAccount account = result.getSignInAccount();
                photoUri = account.getPhotoUrl();
                firebaseAuthWithGoogle(account);
            } else {
                Toast.makeText(IntroActivity.this, "Google Authentication failed!", Toast.LENGTH_SHORT).show();
            }

        }

    }

    /**
     * Sign in to FireClip with Google account.
     */
    private void firebaseAuthWithGoogle(GoogleSignInAccount account) {

        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);

        final ProgressDialog dialog = new ProgressDialog(this, R.style.Theme_Dialog);
        dialog.setMessage("Signing in...");
        dialog.setProgressDrawable(new ColorDrawable(Color.parseColor("#FF9800")));
        dialog.setIndeterminate(true);
        dialog.setCancelable(false);
        dialog.show();

        firebaseAuth.signInWithCredential(credential).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {

                if (!task.isSuccessful()) {
                    Toast.makeText(IntroActivity.this, "Google Authentication failed!", Toast.LENGTH_SHORT).show();
                }
                dialog.dismiss();

            }
        });

    }

    /**
     * Remove the auth listener.
     */
    @Override
    protected void onStop() {
        super.onStop();
        if (authStateListener != null) {
            firebaseAuth.removeAuthStateListener(authStateListener);
        }
    }

    /**
     * Connection to GoogleApi failed.
     * Just log it for now.
     */
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d("IntroActivity", "GoogleApiClient connection failed due to -> " + connectionResult.getErrorMessage());
    }
}
