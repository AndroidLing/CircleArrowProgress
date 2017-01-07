package com.test;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;

import com.doctor.CircleArrowProgress;

public class MainActivity extends Activity implements View.OnClickListener {

    CircleArrowProgress progress_1;
    CircleArrowProgress progress_2;
    CircleArrowProgress progress_3;
    float percent;

    private final int[] mColors = new int[]{
            Color.parseColor("#A0522D"),
            Color.parseColor("#EE30A7"),
            Color.parseColor("#5CACEE"),
            Color.parseColor("#008B00"),
            Color.parseColor("#8B2500"),
            Color.parseColor("#8968CD")
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        progress_1 = (CircleArrowProgress) findViewById(R.id.progress_1);
        progress_2 = (CircleArrowProgress) findViewById(R.id.progress_2);
        progress_3 = (CircleArrowProgress) findViewById(R.id.progress_3);
        findViewById(R.id.btn_setColors).setOnClickListener(this);
        findViewById(R.id.btn_addPercent).setOnClickListener(this);
        findViewById(R.id.btn_subPercent).setOnClickListener(this);
        findViewById(R.id.btn_start).setOnClickListener(this);
        findViewById(R.id.btn_stop).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_setColors:
                progress_1.setCircleSchameColors(mColors);
                progress_2.setCircleSchameColors(mColors);
                progress_3.setCircleSchameColors(mColors);
                break;
            case R.id.btn_addPercent:
                percent += 0.05f;
                progress_1.setPercent(percent);
                progress_2.setPercent(percent);
                progress_3.setPercent(percent);
                break;
            case R.id.btn_subPercent:
                percent -= 0.05f;
                progress_1.setPercent(percent);
                progress_2.setPercent(percent);
                progress_3.setPercent(percent);
                break;
            case R.id.btn_start:
                progress_1.start();
                progress_2.start();
                progress_3.start();
                break;
            case R.id.btn_stop:
                progress_1.stop();
                progress_2.stop();
                progress_3.stop();
                break;

        }
    }
}
