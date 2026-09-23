package com.engboost.encryptedca.certificates;

import android.app.Application;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.io.InputStream;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

/** Сохраняет операцию импорта и её результат при пересоздании представления Fragment. */
public final class CertificateImportViewModel extends AndroidViewModel {
    private static final String LOG_TAG = "CertificateImport";
    private final CertificateProfileStore store;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final MutableLiveData<CertificateImportState> state =
            new MutableLiveData<>(CertificateImportState.idle());

    /**
     * Создаёт состояние импорта и хранилище профилей для приложения.
     *
     * @param application экземпляр Android-приложения
     */
    public CertificateImportViewModel(@NonNull Application application) {
        super(application);
        store = new CertificateProfileStore(application);
    }

    /**
     * Возвращает наблюдаемое состояние текущего импорта.
     *
     * @return LiveData состояний экрана импорта
     */
    LiveData<CertificateImportState> getState() {
        return state;
    }

    /**
     * Принимает владение паролем и очищает его, даже если URI нельзя открыть.
     * Импорт продолжается при уничтожении View; новое представление получает его результат.
     *
     * @param displayName необязательное имя профиля
     * @param p12Uri URI контейнера PKCS#12
     * @param caUri URI сертификата CA
     * @param password пароль; владение массивом передаётся ViewModel
     */
    void importProfile(String displayName, Uri p12Uri, Uri caUri, char[] password) {
        if (state.getValue() != null && state.getValue().status == CertificateImportState.Status.IMPORTING) {
            Arrays.fill(password, '\0');
            return;
        }
        state.setValue(CertificateImportState.importing());
        try {
            executor.execute(() -> {
                try (InputStream p12 = getApplication().getContentResolver().openInputStream(p12Uri);
                     InputStream ca = getApplication().getContentResolver().openInputStream(caUri)) {
                    if (p12 == null || ca == null) {
                        throw new CertificateProfileException(CertificateProfileError.FILE_UNAVAILABLE,
                                "Selected document could not be opened");
                    }
                    state.postValue(CertificateImportState.success(
                            store.importProfile(displayName, p12, password, ca)));
                } catch (CertificateProfileException e) {
                    Log.e(LOG_TAG, "Import failed at " + e.getError()
                            + ", password length=" + password.length, e);
                    state.postValue(CertificateImportState.error(e.getError()));
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Could not open selected document", e);
                    state.postValue(CertificateImportState.error(CertificateProfileError.FILE_UNAVAILABLE));
                } finally {
                    Arrays.fill(password, '\0');
                }
            });
        } catch (RejectedExecutionException e) {
            Arrays.fill(password, '\0');
            state.setValue(CertificateImportState.error(CertificateProfileError.FILE_UNAVAILABLE));
        }
    }

    /**
     * Завершает приём задач при уничтожении ViewModel, позволяя начатому импорту закончить запись.
     */
    @Override protected void onCleared() {
        // Не прерываем запись: хранилище должно завершить импорт или компенсирующую очистку.
        executor.shutdown();
    }
}
