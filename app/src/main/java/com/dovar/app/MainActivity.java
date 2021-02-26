package com.dovar.app;

import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.dovar.demo.R;
import com.dovar.dplugin.core.DPlugin;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.tv_install).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                File file = new File(Environment.getExternalStorageDirectory() + File.separator + "plugin.apk");
                DPlugin.install("demo", file.getAbsolutePath());
            }
        });
        findViewById(R.id.tv_log).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DynamicAgent.test();
            }
        });
    }

}
