package com.engboost.encryptedca;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.engboost.encryptedca.certificates.CertificateProfileImportFragment;

/** Prototype host. The reusable import UI lives in CertificateProfileImportFragment. */
public final class MainActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new CertificateProfileImportFragment())
                    .commit();
        }
    }
}
