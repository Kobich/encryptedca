package com.engboost.encryptedca.certificates;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.engboost.encryptedca.R;

/** Стартовый экран сохранённых пар с ручным выбором активного профиля. */
public final class CertificateProfileListFragment extends Fragment {
    private CertificateProfileListViewModel viewModel;
    private LinearLayout profileRows;
    private TextView selectionText;
    private TextView listStatusText;
    private Button addProfileButton;

    /** Получает состояние, переживающее поворот экрана. */
    @Override public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(CertificateProfileListViewModel.class);
    }

    /** Создаёт разметку списка. */
    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater,
            @Nullable ViewGroup container, @Nullable Bundle state) {
        return inflater.inflate(R.layout.fragment_certificate_profile_list, container, false);
    }

    /** Подключает переход к импорту и наблюдение за списком. */
    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        profileRows = view.findViewById(R.id.profile_rows);
        selectionText = view.findViewById(R.id.selection_status);
        listStatusText = view.findViewById(R.id.list_status);
        addProfileButton = view.findViewById(R.id.add_profile);
        addProfileButton.setOnClickListener(v -> {
            addProfileButton.setEnabled(false);
            getParentFragmentManager().beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.fragment_container, new CertificateProfileImportFragment())
                .addToBackStack("import")
                .commit();
        });
        viewModel.getState().observe(getViewLifecycleOwner(), this::render);
    }

    /** Обновляет список после импорта и возврата в приложение. */
    @Override public void onResume() {
        super.onResume();
        viewModel.refresh();
    }

    /** Строит строки и показывает активный профиль.
     * @param state актуальное состояние списка
     */
    private void render(CertificateProfileListViewModel.State state) {
        addProfileButton.setEnabled(!state.busy);
        selectionText.setText(state.activeId == null
                ? R.string.no_active_profile : R.string.active_profile_selected);
        if (state.busy) {
            listStatusText.setText(R.string.profiles_loading);
        } else if (state.error == CertificateProfileError.PROFILE_INCOMPLETE) {
            listStatusText.setText(R.string.error_profile_incomplete);
        } else if (state.error != null) {
            listStatusText.setText(R.string.error_storage);
        } else if (state.rows.isEmpty()) {
            listStatusText.setText(R.string.profiles_empty);
        } else {
            listStatusText.setText(R.string.profiles_hint);
        }
        profileRows.removeAllViews();
        for (CertificateProfileListViewModel.Row row : state.rows) {
            View item = getLayoutInflater().inflate(R.layout.item_certificate_profile, profileRows, false);
            String name = row.name == null || row.name.trim().isEmpty()
                    ? getString(R.string.unnamed_profile) : row.name;
            ((TextView) item.findViewById(R.id.profile_name)).setText(name);
            ((TextView) item.findViewById(R.id.profile_id)).setText(row.id);
            boolean active = row.id.equals(state.activeId);
            item.findViewById(R.id.profile_active).setVisibility(active ? View.VISIBLE : View.GONE);
            View select = item.findViewById(R.id.select_profile);
            select.setEnabled(!state.busy && !active);
            select.setOnClickListener(v -> confirm(row.id, name, false));
            View delete = item.findViewById(R.id.delete_profile);
            delete.setEnabled(!state.busy);
            delete.setContentDescription(getString(R.string.delete_profile_accessibility, name));
            delete.setOnClickListener(v -> confirm(row.id, name, true));
            profileRows.addView(item);
        }
    }

    /** Открывает сохраняемый при повороте диалог подтверждения. */
    private void confirm(String id, String name, boolean delete) {
        if (getChildFragmentManager().findFragmentByTag("profile_confirmation") == null) {
            ProfileConfirmationDialog.create(id, name, delete)
                    .show(getChildFragmentManager(), "profile_confirmation");
        }
    }

    /** Освобождает ссылки на уничтоженные View. */
    @Override public void onDestroyView() {
        profileRows = null;
        selectionText = null;
        listStatusText = null;
        addProfileButton = null;
        super.onDestroyView();
    }
}
