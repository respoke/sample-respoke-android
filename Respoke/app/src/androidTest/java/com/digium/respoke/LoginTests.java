package com.digium.respoke;

import android.support.test.espresso.action.ViewActions;
import android.support.test.espresso.matcher.ViewMatchers;
import android.test.suitebuilder.annotation.LargeTest;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isChecked;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.not;

import com.digium.respoke.GroupListActivity;


/**
 * Created by jasonadams on 2/10/15.
 */
@LargeTest
public class LoginTests extends RespokeUITestCase<ConnectActivity> {


    public LoginTests() {
        super(ConnectActivity.class);
    }


    @Override
    public void setUp() throws Exception {
        super.setUp();
        getActivity();
    }


    public void testSuccessfulLogin() {
        String testEndpointID = generateTestEndpointID();
        String testGroupID = generateTestGroupID();

        // Attempt to connect to Respoke with the specified endpoint and group IDs
        loginEndpoint(testEndpointID, testGroupID);

        // Logout if everything was successful
        logout();
    }


    public void testFailedLoginWithBlankEndpoint() {
        String testEndpointID = generateTestEndpointID();

        onView(withId(R.id.endpoint_id_text_box)).check(matches(isDisplayed()));
        onView(withId(R.id.endpoint_id_text_box)).perform(ViewActions.clearText());

        onView(withId(R.id.group_id_text_box)).check(matches(isDisplayed()));
        onView(withId(R.id.group_id_text_box)).perform(ViewActions.clearText());

        onView(withId(R.id.brokered_auth_toggle_button)).check(matches(isDisplayed()));
        onView(withId(R.id.brokered_auth_toggle_button)).check(matches(not(isChecked())));

        onView(withId(R.id.error_message)).check(matches(not(isDisplayed())));
        onView(withId(R.id.connect_button)).check(matches(isDisplayed()));
        onView(withId(R.id.progress_circle)).check(matches(not(isDisplayed())));

        // Try pressing the connect button when no endpoint ID is specified
        onView(withId(R.id.connect_button)).perform(ViewActions.click());

        // An error message should have been displayed
        onView(withId(R.id.error_message)).check(matches(isDisplayed()));
        onView(withId(R.id.error_message)).check(matches(ViewMatchers.withText(R.string.endpoint_id_blank)));

        // Type a legitimate endpoint ID into the text box
        onView(withId(R.id.endpoint_id_text_box)).perform(ViewActions.typeText(testEndpointID));

        // The error message should have become hidden again
        onView(withId(R.id.error_message)).check(matches(not(isDisplayed())));
    }

}
