package com.engboost.encryptedca.certificates;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Загружает список и выполняет выбор и удаление вне главного потока. */
public final class CertificateProfileListViewModel extends AndroidViewModel {
    private final CertificateProfileStore store;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final MutableLiveData<State> state = new MutableLiveData<>(
            new State(Collections.emptyList(), null, true, null));
    private boolean running;

    /** Создаёт хранилище для операций списка.
     * @param application контекст приложения
     */
    public CertificateProfileListViewModel(@NonNull Application application) {
        super(application);
        store = new CertificateProfileStore(application);
    }

    /** @return наблюдаемое состояние списка */
    LiveData<State> getState() { return state; }

    /** Перечитывает индекс при возврате на экран. */
    void refresh() { execute(null, false); }

    /** Выбирает профиль после подтверждения.
     * @param id идентификатор выбранного профиля
     */
    void select(String id) { execute(id, false); }

    /** Удаляет профиль после подтверждения.
     * @param id идентификатор удаляемого профиля
     */
    void delete(String id) { execute(id, true); }

    /** Выполняет операцию и публикует актуальный индекс.
     * @param id профиль или null для обновления
     * @param delete требуется ли удаление
     */
    private void execute(String id, boolean delete) {
        if (running) {
            return;
        }
        running = true;
        State previous = state.getValue();
        state.setValue(new State(previous.rows, previous.activeId, true, null));
        executor.execute(() -> {
            CertificateProfileError error = null;
            List<Row> rows = previous.rows;
            String active = previous.activeId;
            try {
                if (id != null) {
                    if (delete) {
                        store.deleteProfile(id);
                    } else {
                        // Не разрешаем выбрать профиль с отсутствующим ключом или CA.
                        store.getProfile(id);
                        store.setActiveProfile(id);
                    }
                }
            } catch (CertificateProfileException e) {
                error = e.getError();
            } catch (RuntimeException e) {
                error = CertificateProfileError.STORAGE_FAILED;
            }
            try {
                rows = new ArrayList<>();
                for (String profileId : store.getProfileIds()) {
                    rows.add(new Row(profileId, store.getDisplayName(profileId)));
                }
                rows.sort((left, right) -> left.id.compareTo(right.id));
                active = store.getActiveProfileId();
            } catch (RuntimeException e) {
                error = CertificateProfileError.STORAGE_FAILED;
            }
            State result = new State(rows, active, false, error);
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                running = false;
                state.setValue(result);
            });
        });
    }

    /** Даёт начатой операции завершиться при уходе с экрана. */
    @Override protected void onCleared() { executor.shutdown(); }

    /** Данные строки без расшифрованных сертификатов. */
    static final class Row {
        final String id;
        final String name;
        /**
         * Создаёт строку списка.
         * @param id идентификатор
         * @param name пользовательское имя
         */
        Row(String id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    /** Снимок списка, выбора и текущей операции. */
    static final class State {
        final List<Row> rows;
        final String activeId;
        final boolean busy;
        final CertificateProfileError error;
        /** Создаёт неизменяемый снимок состояния. */
        State(List<Row> rows, String activeId, boolean busy, CertificateProfileError error) {
            this.rows = Collections.unmodifiableList(new ArrayList<>(rows));
            this.activeId = activeId;
            this.busy = busy;
            this.error = error;
        }
    }
}
