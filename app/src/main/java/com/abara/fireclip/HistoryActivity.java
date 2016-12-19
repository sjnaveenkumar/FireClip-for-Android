package com.abara.fireclip;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatSpinner;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.abara.fireclip.adapter.HistoryAdapter;
import com.abara.fireclip.util.AndroidUtils;
import com.abara.fireclip.util.FireClipUtils;
import com.abara.fireclip.util.HistoryClip;
import com.abara.fireclip.util.ItemClickListener;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;

import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmResults;
import io.realm.Sort;

/**
 * Activity for managing local history clips.
 * <p>
 * Created by abara on 11/09/16.
 */
public class HistoryActivity extends AppCompatActivity implements ItemClickListener {

    private Realm realm;
    private RealmChangeListener changeListener;

    private AppCompatSpinner sortBySpinner;
    private HistoryAdapter adapter;
    private ArrayAdapter<String> spinnerAdapter;
    private RecyclerView historyList;

    private ArrayList<String> sortOptions;
    private RealmResults<HistoryClip> historyClips;
    private String from;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.history_text);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        realm = Realm.getDefaultInstance();
        sortOptions = new ArrayList<>();

        historyList = (RecyclerView) findViewById(R.id.history_list);
        historyList.setLayoutManager(new LinearLayoutManager(this));
        historyList.setItemAnimator(new DefaultItemAnimator());
        historyList.setHasFixedSize(false);

        historyClips = realm.where(HistoryClip.class).findAllSorted("timestamp", Sort.DESCENDING);

        adapter = new HistoryAdapter(this, historyClips, this);
        historyList.setAdapter(adapter);

        // Listen for database changes, and update the adapter.
        changeListener = new RealmChangeListener() {

            @Override
            public void onChange(Object element) {
                adapter.notifyDataSetChanged();
            }
        };
        realm.addChangeListener(changeListener);

        sortBySpinner = (AppCompatSpinner) findViewById(R.id.sort_by_spinner);

        // Sort options includes Latest, Oldest and name of devices clips received from.
        sortOptions.add(0, "Latest");
        sortOptions.add(1, "Oldest");
        RealmResults<HistoryClip> deviceNames = historyClips.distinct("from").sort("from", Sort.ASCENDING);
        for (HistoryClip clip : deviceNames) {
            sortOptions.add(clip.getFrom());
        }

        spinnerAdapter = new ArrayAdapter<>(this, R.layout.simple_list_item, sortOptions);
        sortBySpinner.setAdapter(spinnerAdapter);
        sortBySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {

                if (position == 0) {

                    // Latest clips
                    from = null;
                    historyClips = realm.where(HistoryClip.class).findAllSorted("timestamp", Sort.DESCENDING);

                } else if (position == 1) {

                    // Oldest clips
                    from = null;
                    historyClips = realm.where(HistoryClip.class).findAllSorted("timestamp", Sort.ASCENDING);

                } else {

                    // Clips from devices
                    from = sortOptions.get(position);
                    historyClips = realm.where(HistoryClip.class).equalTo("from", from).findAllSorted("timestamp", Sort.DESCENDING);

                }

                adapter = new HistoryAdapter(HistoryActivity.this, historyClips, HistoryActivity.this);
                historyList.setAdapter(adapter);

            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                // Nothing to do
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.history, menu);
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * Handle click for back button and,
     * delete all history.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.action_delete:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.title_delete_history_dialog);
                String message = (from == null) ? getResources().getString(R.string.dialog_signout_message)
                        : getResources().getString(R.string.dialog_delete_history_msg, from);
                builder.setMessage(message);
                builder.setPositiveButton("Delete All", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        realm.beginTransaction();
                        boolean deleted = historyClips.deleteAllFromRealm();
                        realm.commitTransaction();
                        if (deleted) {
                            if (from != null) {
                                sortOptions.remove(from);
                                sortBySpinner.setSelection(0);
                                spinnerAdapter.notifyDataSetChanged();
                            }
                            Toast.makeText(HistoryActivity.this, "History deleted!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(HistoryActivity.this, "Cannot delete history, try again!", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                builder.create().show();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Close the realm instance.
     */
    @Override
    protected void onDestroy() {
        realm.close();
        super.onDestroy();
    }

    @Override
    public void onItemClick(int position) {
        // Nothing to do.
        // Automatically handled by adapter.
    }

    /**
     * Called when an HistoryItem is long clicked.
     *
     * @param position position of item in the list.
     */
    @Override
    public void onLongClick(final int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.title_delete_dialog);
        builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                realm.beginTransaction();
                historyClips.deleteFromRealm(position);
                realm.commitTransaction();
                adapter.notifyDataSetChanged();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.create().show();
    }

    /**
     * Called when <b>PIN IT</b> is clicked.
     *
     * @param item HistoryItem to be pinned.
     */
    @Override
    public void onAddPin(final HistoryClip item) {

        Snackbar.make(findViewById(R.id.history_root_view), "Pinning...", Snackbar.LENGTH_SHORT).show();

        String deviceName = AndroidUtils.getDeviceName(this);

        FireClipUtils.pinItem(item.getContent(), deviceName, item.getTimestamp()).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                Snackbar.make(findViewById(R.id.history_root_view), "Pinned!", Snackbar.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Snackbar.make(findViewById(R.id.history_root_view), "Couldn't pin", Snackbar.LENGTH_SHORT)
                        .setAction(R.string.action_retry, new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                onAddPin(item);
                            }
                        }).show();
            }
        });
    }

}
