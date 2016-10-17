package com.abara.fireclip;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatTextView;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

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
 * Created by abara on 29/07/16.
 */

public class IntroActivity extends AppCompatActivity implements View.OnClickListener, GoogleApiClient.OnConnectionFailedListener {

    private static final int RC_SIGN_IN = 9001;
    private static final String TAG = IntroActivity.class.getSimpleName();

    private FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener authStateListener;
    private GoogleApiClient googleApiClient;

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

        authStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {

                FirebaseUser user = firebaseAuth.getCurrentUser();

                if (user != null) {
                    Intent deviceActivity = new Intent(IntroActivity.this, DeviceNameActivity.class);
                    deviceActivity.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(deviceActivity);
                }

            }
        };

        createAccountView.setOnClickListener(this);

    }

    @Override
    protected void onStart() {
        super.onStart();
        firebaseAuth.addAuthStateListener(authStateListener);
    }

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

    private void signInWithGoogle() {
        Intent signinIntent = Auth.GoogleSignInApi.getSignInIntent(googleApiClient);
        startActivityForResult(signinIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {

            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);

            if (result.isSuccess()) {
                GoogleSignInAccount account = result.getSignInAccount();
                firebaseAuthWithGoogle(account);
            } else {
                Toast.makeText(IntroActivity.this, "Google Authentication failed!", Toast.LENGTH_SHORT).show();
            }

        }

    }

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
                    Log.d(TAG, "onComplete: Firebase Auth failed for the user!");
                }
                dialog.dismiss();

            }
        });

    }

    @Override
    protected void onStop() {
        super.onStop();
        if (authStateListener != null) {
            firebaseAuth.removeAuthStateListener(authStateListener);
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG, "GoogleApiClient connection failed due to -> " + connectionResult.getErrorMessage());
    }
}
