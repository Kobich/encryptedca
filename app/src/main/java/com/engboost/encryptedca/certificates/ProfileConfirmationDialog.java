package com.engboost.encryptedca.certificates;

import android.app.Dialog;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;
import com.engboost.encryptedca.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/** Подтверждение смены или удаления профиля, сохраняющееся при повороте. */
public final class ProfileConfirmationDialog extends DialogFragment {
    /** Создаёт диалог с идентификатором операции.
     * @param id идентификатор профиля
     * @param name имя для сообщения
     * @param delete требуется ли удаление
     * @return диалог подтверждения
     */
    static ProfileConfirmationDialog create(String id, String name, boolean delete) {
        Bundle arguments = new Bundle();
        arguments.putString("id", id);
        arguments.putString("name", name);
        arguments.putBoolean("delete", delete);
        ProfileConfirmationDialog dialog = new ProfileConfirmationDialog();
        dialog.setArguments(arguments);
        return dialog;
    }

    /** Передаёт подтверждённую операцию состоянию родительского списка. */
    @NonNull @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle arguments = requireArguments();
        boolean delete = arguments.getBoolean("delete");
        return new MaterialAlertDialogBuilder(requireContext())
                .setTitle(delete ? R.string.delete_profile_title : R.string.select_profile_title)
                .setMessage(getString(delete ? R.string.delete_profile_confirmation
                        : R.string.select_profile_confirmation, arguments.getString("name")))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(delete ? R.string.delete : R.string.select, (dialog, which) -> {
                    CertificateProfileListViewModel model = new ViewModelProvider(requireParentFragment())
                            .get(CertificateProfileListViewModel.class);
                    if (delete) {
                        model.delete(arguments.getString("id"));
                    } else {
                        model.select(arguments.getString("id"));
                    }
                }).create();
    }
}
