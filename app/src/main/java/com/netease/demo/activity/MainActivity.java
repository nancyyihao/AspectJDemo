package com.netease.demo.activity;

import android.app.Activity;
import android.os.Bundle;

import com.netease.demo.R;

public class MainActivity extends Activity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
  }
}
