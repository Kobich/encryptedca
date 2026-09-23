package com.engboost.encryptedca.certificates;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.os.SystemClock;
import android.view.View;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.engboost.encryptedca.MainActivity;
import com.engboost.encryptedca.R;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;

/** Проверки навигации и подтверждений на реальных Android View. */
@RunWith(AndroidJUnit4.class)
public final class CertificateProfileListInstrumentedTest {
    /**
     * Проверяет ручной выбор, поворот диалога, удаление активного профиля и возврат из формы.
     * @throws Exception если тестовые документы недоступны
     */
    @Test public void selectsDeletesAndReturnsFromImport() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        CertificateProfileStore store = new CertificateProfileStore(context);
        for (String id : store.getProfileIds()) {
            store.deleteProfile(id);
        }
        Context tests = InstrumentationRegistry.getInstrumentation().getContext();
        String id;
        try (InputStream p12 = tests.getAssets().open("client.p12");
             InputStream ca = tests.getAssets().open("ca.pem")) {
            id = store.importProfile("UI test", p12, "1234".toCharArray(), ca);
        }
        assertNull(store.getActiveProfileId());
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            awaitList(scenario);
            onView(withText(R.string.no_active_profile)).check(matches(isDisplayed()));
            onView(withText("UI test")).perform(click());
            scenario.recreate();
            onView(withText(R.string.select_profile_title)).check(matches(isDisplayed()));
            onView(withText(R.string.cancel)).perform(click());
            assertNull(store.getActiveProfileId());
            awaitList(scenario);
            onView(withText("UI test")).perform(click());
            onView(withText(R.string.select)).perform(click());
            awaitList(scenario);
            assertTrue(id.equals(store.getActiveProfileId()));
            onView(withText(R.string.profile_active)).check(matches(isDisplayed()));
            scenario.recreate();
            awaitList(scenario);
            onView(withText(R.string.profile_active)).check(matches(isDisplayed()));

            onView(withId(R.id.add_profile)).perform(click());
            onView(withId(R.id.back_to_profiles)).check(matches(isDisplayed())).perform(click());
            awaitList(scenario);
            onView(withContentDescription(context.getString(R.string.delete_profile_accessibility, "UI test")))
                    .perform(click());
            onView(withText(R.string.cancel)).perform(click());
            assertTrue(store.containsProfile(id));
            onView(withContentDescription(context.getString(R.string.delete_profile_accessibility, "UI test")))
                    .perform(click());
            onView(withText(R.string.delete)).perform(click());
            awaitList(scenario);
            assertNull(store.getActiveProfileId());
            assertTrue(store.getProfileIds().isEmpty());
            onView(withText(R.string.profiles_empty)).check(matches(isDisplayed()));
        } finally {
            for (String profileId : store.getProfileIds()) {
                store.deleteProfile(profileId);
            }
        }
    }

    /**
     * Ожидает окончания фоновой операции списка по доступности его основной кнопки.
     * @param scenario запущенная Activity
     */
    private void awaitList(ActivityScenario<MainActivity> scenario) {
        long deadline = SystemClock.uptimeMillis() + 5000;
        AtomicBoolean ready = new AtomicBoolean();
        while (SystemClock.uptimeMillis() < deadline) {
            scenario.onActivity(activity -> {
                View button = activity.findViewById(R.id.add_profile);
                ready.set(button != null && button.isEnabled());
            });
            if (ready.get()) {
                return;
            }
            SystemClock.sleep(50);
        }
        assertTrue("List operation timed out", ready.get());
    }
}
