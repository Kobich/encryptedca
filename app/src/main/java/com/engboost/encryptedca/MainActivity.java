package com.engboost.encryptedca;

import android.os.Bundle;
import android.content.res.Configuration;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowCompat;
import androidx.core.graphics.Insets;

import com.engboost.encryptedca.certificates.CertificateProfileListFragment;

/** Хост экранов списка и импорта профилей. */
public final class MainActivity extends AppCompatActivity {
    /**
     * Создаёт экран-хост и добавляет список профилей при первом запуске Activity.
     *
     * @param savedInstanceState состояние Activity после пересоздания
     */
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        boolean lightTheme = (getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) != Configuration.UI_MODE_NIGHT_YES;
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(lightTheme);
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                .setAppearanceLightNavigationBars(lightTheme);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.fragment_container), (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.ime());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new CertificateProfileListFragment())
                    .commit();
        }
    }
}
