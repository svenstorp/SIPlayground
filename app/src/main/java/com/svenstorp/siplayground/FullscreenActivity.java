package com.svenstorp.siplayground;

import android.annotation.SuppressLint;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.TextView;

import java.util.Calendar;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class FullscreenActivity extends AppCompatActivity {
    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler mHideHandler = new Handler();
    private TextView mContentView;
    private TextView mStatusView;
    private long deviceId = 0;
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };
    private View mControlsView;
    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
            mControlsView.setVisibility(View.VISIBLE);
        }
    };
    private boolean mVisible;
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };

    public class CardReaderBroadcastReceiver extends BroadcastReceiver {
        FullscreenActivity activity;

        public CardReaderBroadcastReceiver(FullscreenActivity activity) {
            this.activity = activity;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            CardReader.Event event = (CardReader.Event)intent.getSerializableExtra("Event");
            switch(event) {
                case DeviceDetected:
                    activity.deviceId = intent.getLongExtra("Serial", 0);

                    activity.mStatusView.setText("Device (" + activity.deviceId + ") online");
                    break;
                case ReadStarted:
                    activity.mStatusView.setText("Device (" + activity.deviceId + ") reading card " + intent.getLongExtra("CardId", 0) + "...");
                    break;
                case ReadCanceled:
                    activity.mStatusView.setText("Device (" + activity.deviceId + ") online");
                    break;
                case Readout:
                    CardReader.CardEntry cardEntry = (CardReader.CardEntry)intent.getParcelableExtra("Entry");
                    if (cardEntry.startTime != 0) {
                        long timeDiff = cardEntry.finishTime - cardEntry.startTime;
                        long minutes = timeDiff / (60*1000);
                        long seconds = (timeDiff - minutes * 60 * 1000) / 1000;
                        long hundreds = (timeDiff - minutes * 60 * 1000 - seconds * 1000);
                        activity.mContentView.setText(String.format("%d:%02d.%02d", minutes, seconds, hundreds));
                    }
                    else {
                        activity.mContentView.setText("Ingen startstämpel");
                    }
                    activity.mStatusView.setText(String.format("Device (%d) card %d read", activity.deviceId, cardEntry.cardId));
                    break;
            }
        }
    }
    private CardReaderBroadcastReceiver mMessageReceiver = new CardReaderBroadcastReceiver(this);

    private CardReader cardReader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_fullscreen);

        mVisible = true;
        mControlsView = findViewById(R.id.fullscreen_content_controls);
        mContentView = findViewById(R.id.fullscreen_content);
        mStatusView = findViewById(R.id.statusView);


        // Set up the user interaction to manually show or hide the system UI.
        mContentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggle();
            }
        });
        mStatusView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggle();
            }
        });

        // Create SIReader
        cardReader = new CardReader(this, Calendar.getInstance());
        cardReader.execute();

        // Set up local broadcast receiver
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter(CardReader.EVENT_IDENTIFIER));
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }

    private void toggle() {
        if (mVisible) {
            hide();
        } else {
            show();
        }
    }

    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mControlsView.setVisibility(View.GONE);
        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    @SuppressLint("InlinedApi")
    private void show() {
        // Show the system bar
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        mVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    /**
     * Schedules a call to hide() in delay milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }
}
