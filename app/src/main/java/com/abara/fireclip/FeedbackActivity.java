package com.abara.fireclip;

import android.content.Intent;
import android.net.Uri;
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

import com.abara.fireclip.util.AndroidUtils;
import com.abara.fireclip.util.FireClipUtils;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;

/**
 * Activity to send feedback.
 * <p>
 * Created by abara on 11/10/16.
 */
public class FeedbackActivity extends AppCompatActivity implements View.OnClickListener, View.OnLongClickListener {

    private static final String GITHUB_ANDROID_REPO = "https://github.com/lvabarajithan/FireClip-for-Android";

    private AppCompatButton sendBtn;
    private TextInputEditText feedbackBox;
    private AppCompatImageView githubImg;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback);
        Toolbar toolbar = (Toolbar) findViewById(R.id.feed_toolbar);
        toolbar.setTitle(R.string.feedback_text);
        toolbar.setNavigationIcon(R.drawable.ic_action_close);
        setSupportActionBar(toolbar);

        feedbackBox = (TextInputEditText) findViewById(R.id.feedback_box);
        sendBtn = (AppCompatButton) findViewById(R.id.feedback_send_btn);
        githubImg = (AppCompatImageView) findViewById(R.id.feedback_github);

        sendBtn.setOnClickListener(this);
        githubImg.setOnClickListener(this);
        githubImg.setOnLongClickListener(this);

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
                Intent githubIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_ANDROID_REPO));
                startActivity(githubIntent);
                Toast.makeText(this, "Long press to pin the repository, and directly open the repository from desktop.", Toast.LENGTH_LONG).show();
                break;
        }

    }

    /**
     * Method to submit the feedback.
     */
    private void submitFeedback(final String feedback) {

        FireClipUtils.addFeedback(this, feedback)
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
                                .setAction(R.string.action_retry, new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        submitFeedback(feedback);
                                    }
                                }).show();
                    }
                });

    }

    /**
     * Exit on clicking close button.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Pin repository, on long click.
     */
    @Override
    public boolean onLongClick(View v) {

        FireClipUtils.pinItem(GITHUB_ANDROID_REPO, AndroidUtils.getDeviceName(this), System.currentTimeMillis())
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        Toast.makeText(FeedbackActivity.this, "Repo link pinned!", Toast.LENGTH_SHORT).show();
                    }
                });
        return true;
    }
}
