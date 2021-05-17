package com.unistrong.ee7162HU.QRScan;

//import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.unistrong.ee7162HU.R;

public class ConsoleActivity extends BaseActivity {

    private static final String TAG = ConsoleActivity.class.getSimpleName();

    private static final int MAX_LINES = 200;
    private static ScrollView mScroll;
    private static LinearLayout mLayout;
    private EditText mInput;
    private static Runnable mRunnableScroll = new Runnable() {
        @Override
        public void run() {
            mScroll.fullScroll(ScrollView.FOCUS_DOWN);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.e(TAG, "onCreate() ConsoleActivity");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_console);

        mScroll = (ScrollView) findViewById(R.id.scrollReception);
        mLayout = (LinearLayout) findViewById(R.id.layoutReception);
        mInput = (EditText) findViewById(R.id.editEmission);

        ((Button) findViewById(R.id.button_send)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                writeData(mInput.getText().toString());
            }
        });

        setToReadLine();

        Log.e(TAG, "onCreate: 2");
    }

    @Override
    protected void onDataReceived(final String buffer) {
//        Log.e(TAG, "onDataReceived: buffer = "+buffer);
        runOnUiThread(new Runnable() {
            public void run() {
                if (mLayout.getChildCount() > MAX_LINES) {
                    mLayout.removeViewAt(0);
                }

                TextView tv = new TextView(getBaseContext());
                tv.setText(buffer);
                mLayout.addView(tv);
                mScroll.post(mRunnableScroll);
            }
        });
    }
}