package com.yuyang.wirelesstransmission;

import android.os.PersistableBundle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;

import com.journeyapps.barcodescanner.CaptureManager;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;


public class QRScanActivity extends AppCompatActivity {
    private DecoratedBarcodeView mDBV;
    private CaptureManager captureManager;
    @Override
    protected void onPause() {
        super.onPause();
        captureManager.onPause();
    }
    @Override
    protected void onResume() {
        super.onResume();
        captureManager.onResume();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        captureManager.onDestroy();
    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return mDBV.onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event);
    }
    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
        captureManager.onSaveInstanceState(outState);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qrscan);
        mDBV=(DecoratedBarcodeView)findViewById(R.id.dbv);
        captureManager = new CaptureManager(this,mDBV);
        captureManager.initializeFromIntent(getIntent(),savedInstanceState);
        captureManager.decode();

    }
}
