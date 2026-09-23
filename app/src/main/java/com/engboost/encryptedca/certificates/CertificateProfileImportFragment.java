package com.engboost.encryptedca.certificates;

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
import androidx.lifecycle.ViewModelProvider;

import com.engboost.encryptedca.R;

/** Экран выбора документов и отображения состояния CertificateImportViewModel. */
public final class CertificateProfileImportFragment extends Fragment {
    private static final String STATE_P12_URI = "p12_uri";
    private static final String STATE_CA_URI = "ca_uri";

    private Uri p12Uri;
    private Uri caUri;
    private CertificateImportViewModel viewModel;
    private EditText profileNameInput;
    private EditText passwordInput;
    private TextView p12FileNameText;
    private TextView caFileNameText;
    private TextView statusText;
    private Button selectP12Button;
    private Button selectCaButton;
    private Button importButton;

    private final ActivityResultLauncher<String[]> p12Picker = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) {
                    p12Uri = uri;
                    if (p12FileNameText != null) p12FileNameText.setText(documentName(uri));
                }
            });
    private final ActivityResultLauncher<String[]> caPicker = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) {
                    caUri = uri;
                    if (caFileNameText != null) caFileNameText.setText(documentName(uri));
                }
            });

    /**
     * Восстанавливает выбранные документы и получает сохраняемую ViewModel.
     *
     * @param savedInstanceState состояние фрагмента после пересоздания
     */
    @Override public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            p12Uri = savedInstanceState.getParcelable(STATE_P12_URI);
            caUri = savedInstanceState.getParcelable(STATE_CA_URI);
        }
        viewModel = new ViewModelProvider(this).get(CertificateImportViewModel.class);
    }

    /**
     * Создаёт разметку экрана импорта сертификатов.
     *
     * @param inflater источник XML-разметки
     * @param container родительский контейнер фрагмента
     * @param savedInstanceState сохранённое состояние
     * @return корневое представление экрана
     */
    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater,
                                                  @Nullable ViewGroup container,
                                                  @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_certificate_profile_import, container, false);
    }

    /**
     * Находит поля разметки, подключает выбор файлов и наблюдение за импортом.
     *
     * @param view созданное корневое представление
     * @param savedInstanceState сохранённое состояние представления
     */
    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        profileNameInput = view.findViewById(R.id.display_name);
        passwordInput = view.findViewById(R.id.p12_password);
        p12FileNameText = view.findViewById(R.id.p12_file_name);
        caFileNameText = view.findViewById(R.id.ca_file_name);
        statusText = view.findViewById(R.id.import_status);
        selectP12Button = view.findViewById(R.id.select_p12);
        selectCaButton = view.findViewById(R.id.select_ca);
        importButton = view.findViewById(R.id.import_profile);
        if (p12Uri != null) p12FileNameText.setText(documentName(p12Uri));
        if (caUri != null) caFileNameText.setText(documentName(caUri));

        selectP12Button.setOnClickListener(v -> p12Picker.launch(new String[]{"*/*"}));
        selectCaButton.setOnClickListener(v -> caPicker.launch(
                new String[]{"application/x-pem-file", "text/plain", "*/*"}));
        importButton.setOnClickListener(v -> startImport());
        viewModel.getState().observe(getViewLifecycleOwner(), this::render);
    }

    /**
     * Сохраняет URI выбранных документов при пересоздании фрагмента.
     *
     * @param outState Bundle для сохранения состояния
     */
    @Override public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(STATE_P12_URI, p12Uri);
        outState.putParcelable(STATE_CA_URI, caUri);
    }

    /**
     * Освобождает ссылки на представления уничтожаемого экрана.
     */
    @Override public void onDestroyView() {
        profileNameInput = null;
        passwordInput = null;
        p12FileNameText = null;
        caFileNameText = null;
        statusText = null;
        selectP12Button = null;
        selectCaButton = null;
        importButton = null;
        super.onDestroyView();
    }

    /**
     * Проверяет ввод, очищает поле пароля и передаёт массив во владение ViewModel.
     */
    private void startImport() {
        if (p12Uri == null || caUri == null) {
            statusText.setText(R.string.select_both_files);
            return;
        }
        char[] password = copyPassword();
        String displayName = profileNameInput.getText().toString().trim();
        if (displayName.isEmpty()) displayName = null;
        passwordInput.getText().clear();
        viewModel.importProfile(displayName, p12Uri, caUri, password);
    }

    /**
     * Копирует пароль из поля ввода в массив символов.
     *
     * @return массив пароля, который далее очищает ViewModel
     */
    private char[] copyPassword() {
        int length = passwordInput.length();
        char[] result = new char[length];
        passwordInput.getText().getChars(0, length, result, 0);
        return result;
    }

    /**
     * Отображает состояние импорта и включает либо блокирует кнопки.
     *
     * @param state новое состояние операции
     */
    private void render(CertificateImportState state) {
        boolean importing = state.status == CertificateImportState.Status.IMPORTING;
        selectP12Button.setEnabled(!importing);
        selectCaButton.setEnabled(!importing);
        importButton.setEnabled(!importing);
        if (state.status == CertificateImportState.Status.IMPORTING) {
            statusText.setText(R.string.importing);
        } else if (state.status == CertificateImportState.Status.SUCCESS) {
            statusText.setText(getString(R.string.import_success, state.profileId));
        } else if (state.status == CertificateImportState.Status.ERROR) {
            statusText.setText(errorMessage(state.error));
        }
    }

    /**
     * Получает имя документа; при недоступности метаданных использует часть URI.
     *
     * @param uri URI выбранного документа
     * @return отображаемое имя документа
     */
    private String documentName(Uri uri) {
        try (Cursor cursor = requireContext().getContentResolver().query(uri,
                new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst() && !cursor.isNull(0)) {
                return cursor.getString(0);
            }
        } catch (Exception ignored) {
            // URI можно использовать, даже если провайдер не возвращает имя документа.
        }
        String fallback = uri.getLastPathSegment();
        return fallback == null ? getString(R.string.document_name_unknown) : fallback;
    }

    /**
     * Выбирает локализованную строку для кода ошибки импорта.
     *
     * @param error категория ошибки
     * @return сообщение для пользователя
     */
    private String errorMessage(CertificateProfileError error) {
        if (error == CertificateProfileError.FILE_UNAVAILABLE) return getString(R.string.error_file_unavailable);
        if (error == CertificateProfileError.PKCS12_PASSWORD_OR_CORRUPT) return getString(R.string.error_p12_invalid);
        if (error == CertificateProfileError.CERTIFICATE_INVALID) return getString(R.string.error_certificate_invalid);
        if (error == CertificateProfileError.PROFILE_INCOMPLETE) return getString(R.string.error_profile_incomplete);
        return getString(R.string.error_storage);
    }
}
