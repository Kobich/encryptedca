package com.engboost.encryptedca.certificates;

import android.app.Application;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.io.InputStream;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

/** Owns a single import operation across Fragment view recreation. */
public final class CertificateImportViewModel extends AndroidViewModel {
    private final CertificateProfileStore store;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final MutableLiveData<CertificateImportState> state =
            new MutableLiveData<>(CertificateImportState.idle());

    public CertificateImportViewModel(@NonNull Application application) {
        super(application);
        store = new CertificateProfileStore(application);
    }

    LiveData<CertificateImportState> getState() {
        return state;
    }

    /**
     * Takes ownership of password. It is cleared even when either URI cannot be opened.
     * The operation continues when a Fragment view goes away; a recreated view observes its result.
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
                    state.postValue(CertificateImportState.error(e.getError()));
                } catch (Exception e) {
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

    @Override protected void onCleared() {
        // Do not interrupt active writes: compensation in CertificateProfileStore must complete.
        executor.shutdown();
    }
}
