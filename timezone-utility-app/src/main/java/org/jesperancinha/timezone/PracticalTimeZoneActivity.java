package org.jesperancinha.timezone;


import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.TimePicker;

import org.jesperancinha.google.api.GoogleAPI;
import org.jesperancinha.timezone.util.SystemUiHider;
import com.newrelic.agent.android.NewRelic;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.Date;


/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 *
 * @see SystemUiHider
 */
public class PracticalTimeZoneActivity extends Activity {
    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * If set, will toggle the system UI visibility upon interaction. Otherwise,
     * will show the system UI visibility upon interaction.
     */
    private static final boolean TOGGLE_ON_CLICK = true;

    /**
     * The flags to pass to {@link SystemUiHider#getInstance}.
     */
    private static final int HIDER_FLAGS = SystemUiHider.FLAG_HIDE_NAVIGATION;

    /**
     * The instance of the {@link SystemUiHider} for this activity.
     */
    private SystemUiHider mSystemUiHider;

    private EditText placeName = null;

    private EditText countryCode = null;

    private EditText extraInfo = null;

    private TextView timeZone = null;

    private TimePicker digitalClock = null;

    private Button btnTimeZoneRequest = null;

    private Thread t;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_practical_time_zone);

        final View controlsView = findViewById(R.id.fullscreen_content_controls);
        final View contentView = findViewById(R.id.fullscreen_content);
        controlsView.setMinimumWidth(contentView.getWidth());

        // Set up an instance of SystemUiHider to control the system UI for
        // this activity.
        mSystemUiHider = SystemUiHider.getInstance(this, contentView, HIDER_FLAGS);
        mSystemUiHider.setup();
        mSystemUiHider
                .setOnVisibilityChangeListener(new SystemUiHider.OnVisibilityChangeListener() {
                    // Cached values.
                    int mControlsHeight;
                    int mShortAnimTime;

                    @Override
                    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
                    public void onVisibilityChange(boolean visible) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
                            // If the ViewPropertyAnimator API is available
                            // (Honeycomb MR2 and later), use it to animate the
                            // in-layout UI controls at the bottom of the
                            // screen.
                            if (mControlsHeight == 0) {
                                mControlsHeight = controlsView.getHeight();
                            }

                            controlsView.animate()
                                    .translationY(visible ? 0 : mControlsHeight)
                                    .setDuration(mShortAnimTime);
                        } else {
                            // If the ViewPropertyAnimator APIs aren't
                            // available, simply show or hide the in-layout UI
                            // controls.
                            controlsView.setVisibility(visible ? View.VISIBLE : View.GONE);
                        }

                        if (visible && AUTO_HIDE) {
                            // Schedule a hide().
                            delayedHide(AUTO_HIDE_DELAY_MILLIS);
                        }
                    }
                });

        // Set up the user interaction to manually show or hide the system UI.
        contentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (TOGGLE_ON_CLICK) {
                    mSystemUiHider.toggle();
                } else {
                    mSystemUiHider.show();
                }

            }
        });

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        findViewById(R.id.dummy_button).setOnTouchListener(mDelayHideTouchListener);

        placeName = (EditText) findViewById(R.id.editPlaceName);
        countryCode = (EditText) findViewById(R.id.editCountryCode);
        extraInfo = (EditText) findViewById(R.id.editExtraInfo);

        timeZone = (TextView) findViewById(R.id.labelTimeZone);
        digitalClock = (TimePicker) findViewById(R.id.timePicker);

        digitalClock.setEnabled(true);

        btnTimeZoneRequest = (Button) findViewById(R.id.btnTimeZoneRequest);

        btnTimeZoneRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final StringBuilder location = new StringBuilder(placeName.getText().toString());
                final String country = countryCode.getText().toString();
                String extra = extraInfo.getText().toString();

                if (location.toString().isEmpty()) {
                    showPopup(PracticalTimeZoneActivity.this);
                } else {
                    if (null != t && t.isAlive()) {
                        t.interrupt();
                        t = null;
                        btnTimeZoneRequest.setText(getString(R.string.time_zone_time_request));
                    } else {
                        btnTimeZoneRequest.setText(getString(R.string.checking_times));
                        if (null != country) {
                            location.append(" ").append(country);
                        }

                        t = new Thread(new Runnable() {

                            @Override
                            public void run() {

                                try {

                                    final DateTimeZone dateTimeZone = GoogleAPI.getTimeZone(location.toString(), country);
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            if (!isFinishing()) {
                                                if (null != dateTimeZone) {
                                                    timeZone.setText(dateTimeZone.getID());
                                                    DateTime now = DateTime.now(dateTimeZone);
                                                    digitalClock.setCurrentHour(now.getHourOfDay());
                                                    digitalClock.setCurrentMinute(now.getMinuteOfHour());
                                                } else {
                                                    timeZone.setText(getString(R.string.not_found));
                                                }
                                            }
                                        }
                                    });

                                } catch (Exception e) {

                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            if (!isFinishing()) {
                                                timeZone.setText(getString(R.string.not_found));
                                            }
                                        }
                                    });

                                }
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (!isFinishing()) {
                                            btnTimeZoneRequest.setText(getString(R.string.time_zone_time_request));
                                        }
                                    }
                                });

                            }

                        });


                        t.start();
                    }
                }

            }
        });


        NewRelic.withApplicationToken("AA84e02234381c26f55e1f0e9c5b435bbbdeb05b82").start(this.getApplication());
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }


    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            finish();
            return false;
        }
    };

    Handler mHideHandler = new Handler();
    Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            mSystemUiHider.hide();
        }
    };

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

    private void showPopup(final Activity context) {
        int popupWidth = 400;
        int popupHeight = 400;

        // Inflate the popup_layout.xml
        LinearLayout viewGroup = (LinearLayout) context.findViewById(R.id.popup);
        LayoutInflater layoutInflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View mainView = layoutInflater.inflate(R.layout.fragment_practical_time_zone_error, viewGroup);

        // Creating the PopupWindow
        final PopupWindow popup = new PopupWindow(context);
        popup.setContentView(mainView);
        popup.setWidth(popupWidth);
        popup.setHeight(popupHeight);
        popup.setFocusable(true);

        // Some offset to align the popup a bit to the right, and a bit down, relative to button's position.
        int OFFSET_X = 0;
        int OFFSET_Y = 0;

        // Clear the default translucent background
        popup.setBackgroundDrawable(new BitmapDrawable());

        // Displaying the popup at the specified location, + offsets.
        popup.showAtLocation(mainView, Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL, 0 + OFFSET_X, 0 + OFFSET_Y);

        long timeEnd = new Date().getTime();

        Button btnGoBack = (Button) mainView.findViewById(R.id.btnGoBack);
        btnGoBack.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                popup.dismiss();
            }
        });
    }

}
