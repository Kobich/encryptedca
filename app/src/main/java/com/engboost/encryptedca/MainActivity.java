package com.engboost.encryptedca;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.engboost.encryptedca.certificates.CertificateProfileImportFragment;

/** Хост Activity, размещающий повторно используемый экран импорта профиля. */
public final class MainActivity extends AppCompatActivity {
    /**
     * Создаёт экран-хост и добавляет экран импорта при первом запуске Activity.
     *
     * @param savedInstanceState состояние Activity после пересоздания
     */
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
