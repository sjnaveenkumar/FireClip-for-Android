package com.abara.fireclip;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputEditText;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.view.View;
import android.widget.Toast;

import com.abara.fireclip.util.FireClipUtils;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

/**
 * Activity to Create a new FireClip account.
 * <p>
 * Created by abara on 01/08/16.
 */
public class CreateAccountActivity extends AppCompatActivity implements View.OnClickListener {

    private FirebaseAuth firebaseAuth;

    private FirebaseAuth.AuthStateListener authStateListener;
    private String name;

    private TextInputEditText nameBox, emailBox, passBox, confirmPassBox;
    private AppCompatButton createSignupBtn;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_account);

        firebaseAuth = FirebaseAuth.getInstance();

        nameBox = (TextInputEditText) findViewById(R.id.create_name_box);
        emailBox = (TextInputEditText) findViewById(R.id.create_email_box);
        passBox = (TextInputEditText) findViewById(R.id.create_pass_box);
        confirmPassBox = (TextInputEditText) findViewById(R.id.create_pass_confirm_box);
        createSignupBtn = (AppCompatButton) findViewById(R.id.create_signup_btn);

        createSignupBtn.setOnClickListener(this);

        /**
         * Listen for user authentication changes.
         */
        authStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {

                FirebaseUser user = firebaseAuth.getCurrentUser();

                if (user != null) {

                    /**
                     * Set the user's name after account has been created.
                     */
                    UserProfileChangeRequest changeRequest = new UserProfileChangeRequest.Builder().setDisplayName(name).build();
                    user.updateProfile(changeRequest).addOnCompleteListener(CreateAccountActivity.this, new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {

                            if (task.isSuccessful()) {

                                Intent deviceActivity = DeviceNameActivity.getStarterIntent(CreateAccountActivity.this, true);
                                startActivity(deviceActivity);

                                // Subscribe to user's UID as topic, to receive push notifications.
                                FireClipUtils.subscribe();
                            }

                        }
                    });

                }

            }
        };

    }

    /**
     * Add auth listener.
     */
    @Override
    protected void onStart() {
        super.onStart();
        firebaseAuth.addAuthStateListener(authStateListener);
    }

    /**
     * Validate and create account.
     */
    @Override
    public void onClick(View view) {
        name = nameBox.getText().toString();
        String email = emailBox.getText().toString();
        String password = passBox.getText().toString();
        String confirmPassword = confirmPassBox.getText().toString();

        if (!name.isEmpty() && !email.isEmpty() && !password.isEmpty() && !confirmPassword.isEmpty()) {

            if (password.contentEquals(confirmPassword)) {

                if (password.length() < 6 && confirmPassword.length() < 6) {
                    Snackbar.make(findViewById(R.id.create_root_view), "Password must be more than 6 characters", Snackbar.LENGTH_LONG).show();
                } else {
                    final ProgressDialog dialog = new ProgressDialog(this);
                    dialog.setMessage("Creating your account...");
                    dialog.setProgressDrawable(new ColorDrawable(ContextCompat.getColor(this, R.color.colorPrimary)));
                    dialog.setIndeterminate(true);
                    dialog.setCancelable(false);
                    dialog.show();

                    // Create user
                    firebaseAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {

                            if (!task.isSuccessful()) {
                                Toast.makeText(CreateAccountActivity.this, "Signup failed! ", Toast.LENGTH_SHORT).show();
                            }
                            dialog.dismiss();

                        }
                    });
                }

            } else {
                Snackbar.make(findViewById(R.id.create_root_view), "Passwords mismatch!", Snackbar.LENGTH_SHORT).show();
            }
        } else {
            Snackbar.make(findViewById(R.id.create_root_view), "Detail(s) are missing!", Snackbar.LENGTH_SHORT).show();
        }

    }

    /**
     * Remove auth listener.
     */
    @Override
    protected void onStop() {
        super.onStop();
        if (authStateListener != null) {
            firebaseAuth.removeAuthStateListener(authStateListener);
        }
    }
}
