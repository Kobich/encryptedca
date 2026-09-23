package com.engboost.encryptedca.certificates;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.engboost.encryptedca.R;

import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Reusable minimal UI for importing a PKCS#12 client certificate and a CA PEM.
 * The source documents are opened once and are never copied to application storage.
 */
public final class CertificateProfileImportFragment extends Fragment {
    private static final String STATE_P12_URI = "p12_uri";
    private static final String STATE_CA_URI = "ca_uri";

    private final ExecutorService importExecutor = Executors.newSingleThreadExecutor();
    private Uri p12Uri;
    private Uri caUri;
    private CertificateProfileStore profileStore;
    private EditText displayName;
    private EditText password;
    private TextView p12FileName;
    private TextView caFileName;
    private TextView status;
    private Button selectP12;
    private Button selectCa;
    private Button importButton;

    private final ActivityResultLauncher<String[]> p12Picker = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null && isAdded()) {
                    p12Uri = uri;
                    p12FileName.setText(fileName(uri));
                }
            });
    private final ActivityResultLauncher<String[]> caPicker = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null && isAdded()) {
                    caUri = uri;
                    caFileName.setText(fileName(uri));
                }
            });

    @Override public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            p12Uri = savedInstanceState.getParcelable(STATE_P12_URI);
            caUri = savedInstanceState.getParcelable(STATE_CA_URI);
        }
        profileStore = new CertificateProfileStore(requireContext().getApplicationContext());
    }

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater,
                                                  @Nullable ViewGroup container,
                                                  @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_certificate_profile_import, container, false);
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        displayName = view.findViewById(R.id.display_name);
        password = view.findViewById(R.id.p12_password);
        p12FileName = view.findViewById(R.id.p12_file_name);
        caFileName = view.findViewById(R.id.ca_file_name);
        status = view.findViewById(R.id.import_status);
        selectP12 = view.findViewById(R.id.select_p12);
        selectCa = view.findViewById(R.id.select_ca);
        importButton = view.findViewById(R.id.import_profile);
        if (p12Uri != null) p12FileName.setText(fileName(p12Uri));
        if (caUri != null) caFileName.setText(fileName(caUri));

        selectP12.setOnClickListener(v -> p12Picker.launch(new String[]{"*/*"}));
        selectCa.setOnClickListener(v -> caPicker.launch(
                new String[]{"application/x-pem-file", "text/plain", "*/*"}));
        importButton.setOnClickListener(v -> importSelectedFiles());
    }

    @Override public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(STATE_P12_URI, p12Uri);
        outState.putParcelable(STATE_CA_URI, caUri);
    }

    @Override public void onDestroy() {
        importExecutor.shutdownNow();
        super.onDestroy();
    }

    private void importSelectedFiles() {
        if (p12Uri == null || caUri == null) {
            status.setText(R.string.select_both_files);
            return;
        }
        char[] passwordChars = copyPassword();
        String name = displayName.getText().toString().trim();
        if (name.isEmpty()) name = null;
        password.getText().clear();
        setImportInProgress(true);

        Context applicationContext = requireContext().getApplicationContext();
        Uri selectedP12 = p12Uri;
        Uri selectedCa = caUri;
        String finalName = name;
        importExecutor.execute(() -> {
            try (InputStream p12 = applicationContext.getContentResolver().openInputStream(selectedP12);
                 InputStream ca = applicationContext.getContentResolver().openInputStream(selectedCa)) {
                if (p12 == null || ca == null) throw new IllegalStateException("Не удалось открыть выбранный файл");
                String id = profileStore.importProfile(finalName, p12, passwordChars, ca);
                postResult(getString(R.string.import_success, id));
            } catch (Exception e) {
                postResult(getString(R.string.import_failed, userMessage(e)));
            } finally {
                postImportFinished();
            }
        });
    }

    private char[] copyPassword() {
        int length = password.length();
        char[] result = new char[length];
        password.getText().getChars(0, length, result, 0);
        return result;
    }

    private void postResult(String message) {
        if (getActivity() != null) getActivity().runOnUiThread(() -> {
            if (isAdded() && status != null) status.setText(message);
        });
    }

    private void postImportFinished() {
        if (getActivity() != null) getActivity().runOnUiThread(() -> {
            if (isAdded() && importButton != null) setImportInProgress(false);
        });
    }

    private void setImportInProgress(boolean inProgress) {
        importButton.setEnabled(!inProgress);
        selectP12.setEnabled(!inProgress);
        selectCa.setEnabled(!inProgress);
        if (inProgress) status.setText(R.string.importing);
    }

    private String fileName(Uri uri) {
        try (Cursor cursor = requireContext().getContentResolver().query(uri,
                new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) return cursor.getString(0);
        }
        return uri.getLastPathSegment();
    }

    private static String userMessage(Exception error) {
        return error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
    }
}
