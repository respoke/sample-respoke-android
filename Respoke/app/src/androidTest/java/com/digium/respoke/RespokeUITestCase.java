package com.digium.respoke;

import android.app.Activity;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.action.ViewActions;
import android.support.test.espresso.matcher.ViewMatchers;
import android.support.test.internal.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import android.support.test.runner.lifecycle.Stage;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;

import com.android.support.test.deps.guava.collect.Iterables;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isChecked;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.not;

/**
 * Created by jasonadams on 2/10/15.
 */
public abstract class RespokeUITestCase<T extends android.app.Activity> extends ActivityInstrumentationTestCase2<T> {

    public static int TEST_TIMEOUT = 60;  // Timeout in seconds

    private final static String TAG = "RespokeUITestCase";


    public RespokeUITestCase(Class activityClass) {
        super(activityClass);
    }


    public static String generateTestEndpointID() {
        String uuid = "";
        String chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
        int rnd = 0;
        int r;

        for (int i = 0; i < 6; i += 1) {
            if (rnd <= 0x02) {
                rnd = (int) (0x2000000 + Math.round(java.lang.Math.random() * 0x1000000));
            }
            r = rnd & 0xf;
            rnd = rnd >> 4;
            uuid = uuid + chars.charAt(r);
        }
        return "test_user_" + uuid;
    }


    public static String generateTestGroupID() {
        return "group_" + generateTestEndpointID();
    }
    

    Activity getCurrentActivity() throws Throwable {
        getInstrumentation().waitForIdleSync();
        final Activity[] activity = new Activity[1];
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                java.util.Collection<Activity> activites = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED);
                activity[0] = Iterables.getOnlyElement(activites);
            }});
        return activity[0];
    }


    // Wait for an activity of the specified class to be at the top of the display stack.
    // This is necessary to avoid problems with Espresso's asynchronous synchronization methods and
    // the SDK socket implementation.
    public boolean waitForActivity(long timeoutSecs, Class targetClass) {
        long timeoutDate = System.currentTimeMillis() + (timeoutSecs * 1000);
        boolean desiredViewVisible = false;

        try {
            do {
                if (System.currentTimeMillis() > timeoutDate) {
                    Log.e(TAG, "Timeout inside TapInspectTestCase : class = " + this.getClass().getSimpleName());
                    break;
                }
                synchronized (this) {
                    this.wait(100);
                }

                try {
                    Activity currentActivity = getCurrentActivity();
                    desiredViewVisible = currentActivity.getClass().getName().equals(targetClass.getName());
                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                }

            } while (!desiredViewVisible);
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }

        return desiredViewVisible;
    }


    public void loginEndpoint(String endpointID, String groupID) {
        onView(withId(R.id.endpoint_id_text_box)).check(matches(isDisplayed()));
        onView(withId(R.id.endpoint_id_text_box)).perform(ViewActions.clearText());

        onView(withId(R.id.group_id_text_box)).check(matches(isDisplayed()));
        onView(withId(R.id.group_id_text_box)).perform(ViewActions.clearText());

        onView(withId(R.id.brokered_auth_toggle_button)).check(matches(isDisplayed()));
        onView(withId(R.id.brokered_auth_toggle_button)).check(matches(not(isChecked())));

        onView(withId(R.id.error_message)).check(matches(not(isDisplayed())));
        onView(withId(R.id.connect_button)).check(matches(isDisplayed()));
        onView(withId(R.id.progress_circle)).check(matches(not(isDisplayed())));

        // Type the endpoint ID and group ID into the text boxes
        onView(withId(R.id.endpoint_id_text_box)).perform(ViewActions.typeText(endpointID));
        onView(withId(R.id.group_id_text_box)).perform(ViewActions.typeText(groupID));

        // Try pressing the connect button
        onView(withId(R.id.connect_button)).perform(ViewActions.click());

        // The previous statement will return after the authentication has completed, but before the
        // connection & group join complete due to technicalities in the Espresso testing framework.
        // Therefore, the test must come up with another method of determining when the group list
        // activity has appeared. In the mean time, make sure the buttons look correct while the
        // connection is in progress.
        onView(withId(R.id.progress_circle)).check(matches(isDisplayed()));

        // Wait for the group list activity to appear
        waitForActivity(TEST_TIMEOUT, GroupListActivity.class);

        // The group list activity should now be visible
        onView(withId(R.id.group_list)).check(matches(ViewMatchers.isDisplayed()));
    }


    public void logout() {
        // The group list activity should now be visible
        onView(withId(R.id.group_list)).check(matches(ViewMatchers.isDisplayed()));

        Espresso.pressBack();

        // The connect activity should now be visible
        onView(withId(R.id.endpoint_id_text_box)).check(matches(isDisplayed()));
    }
}
