package com.abara.fireclip;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.abara.fireclip.util.Utils;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by abara on 11/10/16.
 */
public class FeedbackActivity extends AppCompatActivity implements View.OnClickListener {

    private AppCompatButton sendBtn;
    private TextInputEditText feedbackBox;
    private AppCompatImageView githubImg;

    private DatabaseReference feedRef;
    private FirebaseUser clipUser;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback);
        Toolbar toolbar = (Toolbar) findViewById(R.id.feed_toolbar);
        toolbar.setTitle(R.string.feedback_text);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        feedRef = FirebaseDatabase.getInstance().getReference("feedbacks");
        clipUser = FirebaseAuth.getInstance().getCurrentUser();

        feedbackBox = (TextInputEditText) findViewById(R.id.feedback_box);
        sendBtn = (AppCompatButton) findViewById(R.id.feedback_send_btn);
        githubImg = (AppCompatImageView) findViewById(R.id.feedback_github);

        sendBtn.setOnClickListener(this);
        githubImg.setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.feedback_send_btn:
                String feedback = feedbackBox.getText().toString();

                if (!feedback.isEmpty()) {
                    submitFeedback(feedback);
                } else {
                    Snackbar.make(findViewById(R.id.feedback_rootview), "Feedback cannot be empty!", Snackbar.LENGTH_LONG)
                            .show();
                }
                break;
            case R.id.feedback_github:
                Snackbar.make(findViewById(R.id.feedback_rootview), "Github page hasn't still established.", Snackbar.LENGTH_LONG)
                        .show();
                break;
        }

    }

    private void submitFeedback(final String feedback) {
        Map<String, Object> feedMap = new HashMap<>();
        feedMap.put(Utils.DATA_MAP_TIME, ServerValue.TIMESTAMP);
        feedMap.put(Utils.DATA_MAP_FEED, feedback);
        feedRef.child(clipUser.getUid()).push().setValue(feedMap)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        onBackPressed();
                        Toast.makeText(FeedbackActivity.this, "Your feedback has been submitted!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Snackbar.make(findViewById(R.id.feedback_rootview), "Can't submit feedback", Snackbar.LENGTH_LONG)
                                .setAction("Retry", new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        submitFeedback(feedback);
                                    }
                                }).show();
                    }
                });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
