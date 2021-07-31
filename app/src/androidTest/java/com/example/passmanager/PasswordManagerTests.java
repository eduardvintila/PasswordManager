package com.example.passmanager;


import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.longClick;
import static androidx.test.espresso.action.ViewActions.pressImeActionButton;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withParent;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.is;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import androidx.preference.PreferenceManager;
import androidx.test.espresso.DataInteraction;
import androidx.test.espresso.ViewInteraction;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.passmanager.model.ApplicationDatabase;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.hamcrest.core.IsInstanceOf;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

/**
 * Contains various instrumented tests that check the functionalities provided by the Password
 * Manager. The tests are to be executed sequentially, in ascending order by their method's name.
 *
 * TODO: Remove duplicate code in the tests.
 */
@LargeTest
@RunWith(AndroidJUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class PasswordManagerTests {

    @Rule
    public ActivityScenarioRule<AuthActivity> rule = new ActivityScenarioRule<>(AuthActivity.class);


    @BeforeClass
    public static void before() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        context.deleteDatabase(ApplicationDatabase.DB_NAME);

        PreferenceManager.getDefaultSharedPreferences(context).edit().clear().apply();
    }

    /**
     * Create a new database, add a new entry, and verify that its data was inserted
     * correctly.
     */
    @Test
    public void a() {

        // Create the database.
        onView(allOf(withId(R.id.firstPassEditText),isDisplayed()))
            .perform(replaceText("1Q2w3e4r5t6y#"), closeSoftKeyboard());
        onView(allOf(withId(R.id.secondPassEditText), isDisplayed()))
                .perform(replaceText("1Q2w3e4r5t6y#"), closeSoftKeyboard());
        onView(allOf(withId(R.id.createDbBtn), isDisplayed()))
                .perform(click());

        // Authenticate.
        ViewInteraction authBtn = onView(
                allOf(withId(R.id.authBtn), isDisplayed()));
        authBtn.check(matches(isDisplayed()));

        onView(allOf(withId(R.id.editTextPassword), isDisplayed()))
                .perform(replaceText("1Q2w3e4r5t6y#"), closeSoftKeyboard());

        authBtn.perform(click());

        // Go to the add entry menu.
        onView(allOf(withId(R.id.fab), isDisplayed())).perform(click());

        onView(allOf(withId(R.id.entryNameEditText)))
               .perform(scrollTo(), replaceText("Yahoo"),
                       closeSoftKeyboard());

        onView(allOf(withId(R.id.entryDescriptionEditText)))
                .perform(scrollTo(), replaceText("parola mail yahoo"),
                        closeSoftKeyboard());

        onView(allOf(withId(R.id.linkEditText)))
                .perform(scrollTo(), replaceText("mail.yahoo.com"),
                        closeSoftKeyboard());

        onView(allOf(withId(R.id.usernameEditText)))
                .perform(scrollTo(), replaceText("testmail123@yahoo.com"),
                        closeSoftKeyboard());

        onView(allOf(withId(R.id.categoriesSpinner))).perform(scrollTo(), click());

        DataInteraction appCompatCheckedTextView = onData(anything())
                .inAdapterView(childAtPosition(
                        withClassName(is("android.widget.PopupWindow$PopupBackgroundView")),
                        0))
                .atPosition(7); // Select the email category from the spinner.
        appCompatCheckedTextView.perform(click());

        ViewInteraction checkableImageButton = onView(
                allOf(withId(R.id.text_input_end_icon), // Check the "show password" icon.
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        1),
                                0),
                        isDisplayed()));
        checkableImageButton.perform(click());

        onView(allOf(withId(R.id.entryPassEditText)))
               .perform(scrollTo(), replaceText("parolayahoo123"),
                       closeSoftKeyboard());

        // Check if the password weakness warning is displayed.
        ViewInteraction textView = onView(
                allOf(withId(R.id.textinput_helper_text), isDisplayed()));
        textView.check(matches(isDisplayed()));
        // Save the entry.
        onView(allOf(withId(R.id.saveEntryBtn))).perform(scrollTo(), click());


        // Check that the entry has been added.
        ViewInteraction textView2 = onView(
                allOf(withId(R.id.numberOfEntriesTextView), withText("(1)"),
                        isDisplayed()));
        textView2.check(matches(withText("(1)")));

        // Click on the Email category.
        ViewInteraction recyclerView = onView(withId(R.id.categoriesRecyclerView));
        recyclerView.perform(actionOnItemAtPosition(7, click()));

        // Click on the entry
        ViewInteraction recyclerView2 = onView(
                allOf(withId(R.id.entriesRecyclerView),
                        childAtPosition(
                                childAtPosition(
                                        hasDescendant(withText("Email")),
                                        5),
                                0)));
        recyclerView2.perform(actionOnItemAtPosition(0, click()));

        // Check if the entry information is accurate.
        ViewInteraction textView3 = onView(
                allOf(withId(R.id.entryNameTextView),
                        isDisplayed()));
        textView3.check(matches(withText("Yahoo")));

        onView(allOf(withId(R.id.usernameTextView), isDisplayed()))
                .check(matches(withText("testmail123@yahoo.com")));

        onView(allOf(withId(R.id.entryDescriptionTextView), isDisplayed()))
                .check(matches(withText("parola mail yahoo")));

        onView(allOf(withId(R.id.linkTextView), isDisplayed()))
                .check(matches(withText("mail.yahoo.com")));

        onView(allOf(withId(R.id.loadDialogBtn)))
                .perform(scrollTo(), click());

        onView(allOf(withId(R.id.editTextMasterPassword), isDisplayed()))
            .perform(replaceText("1Q2w3e4r5t6y#"), closeSoftKeyboard());

        ViewInteraction materialButton5 = onView(
                allOf(withId(android.R.id.button1), withText("Ok"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.buttonPanel),
                                        0),
                                3)));
        materialButton5.perform(scrollTo(), click());

        onView(allOf(withId(R.id.entryPassTextView), isDisplayed()))
                .check(matches(withText("parolayahoo123")));

        pressBack();
    }

    /**
     * Enable the "auto-decrypt" option in the settings menu.
     */
    @Test
    public void b() {
        // Authenticate.
        ViewInteraction authBtn = onView(
                allOf(withId(R.id.authBtn), isDisplayed()));
        authBtn.check(matches(isDisplayed()));

        onView(allOf(withId(R.id.editTextPassword), isDisplayed()))
                .perform(replaceText("1Q2w3e4r5t6y#"), closeSoftKeyboard());

        authBtn.perform(click());


        // Open the overflow menu.
        ViewInteraction overflowMenuButton = onView(
                allOf(childAtPosition(
                        childAtPosition(
                                withId(R.id.topAppBar),
                                1),
                        0),
                        isDisplayed()));
        overflowMenuButton.perform(click());

        // Go to the settings menu.
        ViewInteraction materialTextView = onView(
                allOf(withId(R.id.title), withText(R.string.settings),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.content),
                                        0),
                                0),
                        isDisplayed()));
        materialTextView.perform(click());

        // Enable the auto decrypt option for passwords in the entries.
        ViewInteraction recyclerView = onView(
                allOf(withId(R.id.recycler_view),
                        childAtPosition(
                                withId(android.R.id.list_container),
                                0)));
        recyclerView.perform(actionOnItemAtPosition(1, click()));

        // Enter the master password in order to enable the option.
        ViewInteraction appCompatEditText2 = onView(
                allOf(withId(R.id.editTextMasterPassword),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.custom),
                                        0),
                                0),
                        isDisplayed()));
        appCompatEditText2.perform(replaceText("1Q2w3e4r5t6y#"), closeSoftKeyboard());

        ViewInteraction materialButton2 = onView(
                allOf(withId(android.R.id.button1),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.buttonPanel),
                                        0),
                                3)));
        materialButton2.perform(scrollTo(), click());

        pressBack();
    }

    /**
     * Add some entries.
     */
    @Test
    public void c() {
        // Authenticate.
        ViewInteraction authBtn = onView(
                allOf(withId(R.id.authBtn), isDisplayed()));
        authBtn.check(matches(isDisplayed()));

        onView(allOf(withId(R.id.editTextPassword), isDisplayed()))
                .perform(replaceText("1Q2w3e4r5t6y#"), closeSoftKeyboard());

        authBtn.perform(click());

        // Go to the add entry menu.
        onView(allOf(withId(R.id.fab), isDisplayed())).perform(click());

        // Add the "Gmail" entry.
        onView(allOf(withId(R.id.entryNameEditText)))
                .perform(scrollTo(), replaceText("Gmail"),
                        closeSoftKeyboard());

        onView(allOf(withId(R.id.entryDescriptionEditText)))
                .perform(scrollTo(), replaceText("date gmail"),
                        closeSoftKeyboard());

        onView(allOf(withId(R.id.linkEditText)))
                .perform(scrollTo(), replaceText("mail.google.com"),
                        closeSoftKeyboard());

        onView(allOf(withId(R.id.usernameEditText)))
                .perform(scrollTo(), replaceText("googletest123@gmail.com"),
                        closeSoftKeyboard());

        onView(allOf(withId(R.id.categoriesSpinner)))
                .perform(scrollTo(), click());
        DataInteraction appCompatCheckedTextView = onData(anything())
                .inAdapterView(childAtPosition(
                        withClassName(is("android.widget.PopupWindow$PopupBackgroundView")),
                        0))
                .atPosition(7); // Select the Email category.
        appCompatCheckedTextView.perform(click());

        ViewInteraction checkableImageButton = onView(
                allOf(withId(R.id.text_input_end_icon), // Check the "show password" icon.
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        1),
                                0),
                        isDisplayed()));
        checkableImageButton.perform(click());

        // Generate a random password.
        onView(allOf(withId(R.id.generatePassBtn))).perform(scrollTo(), click());

        // Save the entry, returning to the main menu.
        onView(allOf(withId(R.id.saveEntryBtn))).perform(scrollTo(), click());


        // Go to the add entry menu.
        onView(allOf(withId(R.id.fab), isDisplayed())).perform(click());

        onView(allOf(withId(R.id.entryNameEditText)))
                .perform(scrollTo(), replaceText("Netflix"),
                        closeSoftKeyboard());

        onView(allOf(withId(R.id.entryDescriptionEditText)))
                .perform(scrollTo(), replaceText("cont netflix"),
                        closeSoftKeyboard());

        onView(allOf(withId(R.id.linkEditText)))
                .perform(scrollTo(), replaceText("netflix.com"),
                        closeSoftKeyboard());

        onView(allOf(withId(R.id.usernameEditText)))
                .perform(scrollTo(), replaceText("testnetflix"),
                        closeSoftKeyboard());

        onView(allOf(withId(R.id.categoriesSpinner))).perform(scrollTo(), click());

        DataInteraction appCompatCheckedTextView2 = onData(anything())
                .inAdapterView(childAtPosition(
                        withClassName(is("android.widget.PopupWindow$PopupBackgroundView")),
                        0))
                .atPosition(1); // Select the "Streaming" category from the spinner.
        appCompatCheckedTextView2.perform(click());

        ViewInteraction checkableImageButton2 = onView(
                allOf(withId(R.id.text_input_end_icon), // Check the "show password" icon.
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        1),
                                0),
                        isDisplayed()));
        checkableImageButton2.perform(click());

        onView(allOf(withId(R.id.entryPassEditText)))
                .perform(scrollTo(), replaceText("parolanetflix"),
                        closeSoftKeyboard());
        // Save the entry.
        onView(allOf(withId(R.id.saveEntryBtn))).perform(scrollTo(), click());

        // Go to the add entry menu.
        onView(allOf(withId(R.id.fab), isDisplayed())).perform(click());

        onView(allOf(withId(R.id.entryNameEditText)))
                .perform(scrollTo(), replaceText("Interfon bloc"),
                        closeSoftKeyboard());

        onView(allOf(withId(R.id.entryDescriptionEditText)))
                .perform(scrollTo(), replaceText("interfon"),
                        closeSoftKeyboard());

        onView(allOf(withId(R.id.usernameEditText)))
                .perform(scrollTo(), replaceText("bloc2a"),
                        closeSoftKeyboard());

        onView(allOf(withId(R.id.pinRadioButton)))
                .perform(scrollTo(), click());

        ViewInteraction checkableImageButton3 = onView(
                allOf(withId(R.id.text_input_end_icon), // Check the "show password" icon.
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        1),
                                0),
                        isDisplayed()));
        checkableImageButton3.perform(click());


        onView(allOf(withId(R.id.entryPassEditText)))
                .perform(scrollTo(), replaceText("1234"),
                        closeSoftKeyboard());

        // Save the entry.
        onView(allOf(withId(R.id.saveEntryBtn))).perform(scrollTo(), click());
    }

    /**
     * Change the master password and verify that all entries are not corrupted.
     */
    @Test
    public void d() {
        // Authenticate.
        ViewInteraction authBtn = onView(
                allOf(withId(R.id.authBtn), isDisplayed()));
        authBtn.check(matches(isDisplayed()));

        onView(allOf(withId(R.id.editTextPassword), isDisplayed()))
                .perform(replaceText("1Q2w3e4r5t6y#"), closeSoftKeyboard());

        authBtn.perform(click());

        // Open the overflow menu.
        ViewInteraction overflowMenuButton = onView(
                allOf(childAtPosition(
                        childAtPosition(
                                withId(R.id.topAppBar),
                                1),
                        0),
                        isDisplayed()));
        overflowMenuButton.perform(click());

        // Go to the "change master password" menu
        ViewInteraction materialTextView = onView(
                allOf(withId(R.id.title), withText(R.string.change_master_password),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.content),
                                        0),
                                0),
                        isDisplayed()));
        materialTextView.perform(click());

        // Change the master password.
        ViewInteraction appCompatEditText2 = onView(
                allOf(withId(R.id.masterPassEditText), isDisplayed()));
        appCompatEditText2.perform(replaceText("1Q2w3e4r5t6y#"), closeSoftKeyboard());

        ViewInteraction appCompatEditText3 = onView(
                allOf(withId(R.id.firstPassEditText), isDisplayed()));
        appCompatEditText3.perform(replaceText("1Q2w3e4r5t6y7u#"), closeSoftKeyboard());

        ViewInteraction appCompatEditText4 = onView(
                allOf(withId(R.id.secondPassEditText), isDisplayed()));
        appCompatEditText4.perform(replaceText("1Q2w3e4r5t6y7u#"), closeSoftKeyboard());

        ViewInteraction materialButton2 = onView(
                allOf(withId(R.id.updateMasterPassBtn), isDisplayed()));
        materialButton2.perform(click());

        // Click the "Email" category.
        ViewInteraction categoriesRecyclerView = onView(withId(R.id.categoriesRecyclerView));
        categoriesRecyclerView.perform(actionOnItemAtPosition(7, click()));

        // Click the first entry (Yahoo).
        ViewInteraction recyclerView2 = onView(
                allOf(withId(R.id.entriesRecyclerView),
                        childAtPosition(
                                childAtPosition(
                                        hasDescendant(withText("Email")),
                                        5),
                                0)));
        recyclerView2.perform(actionOnItemAtPosition(0, click()));
        onView(allOf(withId(R.id.entryNameTextView), isDisplayed()))
                .check(matches(withText("Yahoo")));
        onView(allOf(withId(R.id.usernameTextView), isDisplayed()))
                .check(matches(withText("testmail123@yahoo.com")));
        onView(allOf(withId(R.id.entryPassTextView), isDisplayed()))
                .check(matches(withText("parolayahoo123")));
        pressBack();

        // Click the second entry (Gmail).
        ViewInteraction recyclerView3 = onView(
                allOf(withId(R.id.entriesRecyclerView),
                        childAtPosition(
                                childAtPosition(
                                        hasDescendant(withText("Email")),
                                        5),
                                0)));
        recyclerView3.perform(actionOnItemAtPosition(1, click()));
        onView(allOf(withId(R.id.entryNameTextView), isDisplayed()))
                .check(matches(withText("Gmail")));
        onView(allOf(withId(R.id.usernameTextView), isDisplayed()))
                .check(matches(withText("googletest123@gmail.com")));
        // TODO: Check that the entry password matches the generated one.
        /*onView(allOf(withId(R.id.entryPassTextView), isDisplayed()))
                .check(matches(withText("d@Jx5-!#XYwo")));*/
        pressBack();

        // Click the "Streaming" Category
        categoriesRecyclerView.perform(actionOnItemAtPosition(1, click()));
        // Click the first entry (Netflix).
        ViewInteraction recyclerView6 = onView(
                allOf(withId(R.id.entriesRecyclerView),
                        childAtPosition(
                                childAtPosition(
                                        hasDescendant(withText("Streaming")),
                                        5),
                                0)));
        recyclerView6.perform(actionOnItemAtPosition(0, click()));
        onView(allOf(withId(R.id.entryNameTextView), isDisplayed()))
                .check(matches(withText("Netflix")));
        onView(allOf(withId(R.id.usernameTextView), isDisplayed()))
                .check(matches(withText("testnetflix")));
        onView(allOf(withId(R.id.entryPassTextView), isDisplayed()))
                .check(matches(withText("parolanetflix")));
        pressBack();

        // Click the "Others" Category. TODO: Make sure that this category is visible on screen.
        categoriesRecyclerView.perform(actionOnItemAtPosition(0, click()));
        // Click the first entry.
        ViewInteraction recyclerView8 = onView(
                allOf(withId(R.id.entriesRecyclerView),
                        childAtPosition(
                                childAtPosition(
                                        hasDescendant(withText("Others")),
                                        5),
                                0)));
        recyclerView8.perform(actionOnItemAtPosition(0, click()));
        onView(allOf(withId(R.id.entryNameTextView), isDisplayed()))
                .check(matches(withText("Interfon bloc")));
        onView(allOf(withId(R.id.usernameTextView), isDisplayed()))
                .check(matches(withText("bloc2a")));
        onView(allOf(withId(R.id.entryPassTextView), isDisplayed()))
                .check(matches(withText("1234")));
        pressBack();
    }

    /**
     * Insert a new category, add an entry in this category, and check that the deletion of the
     * category doesn't corrupt the entry's data.
     */
    @Test
    public void e() {
        // Authenticate.
        ViewInteraction authBtn = onView(
                allOf(withId(R.id.authBtn), isDisplayed()));
        authBtn.check(matches(isDisplayed()));

        onView(allOf(withId(R.id.editTextPassword), isDisplayed()))
                .perform(replaceText("1Q2w3e4r5t6y7u#"), closeSoftKeyboard());

        authBtn.perform(click());

        // Open the overflow menu.
        ViewInteraction overflowMenuButton = onView(
                allOf(childAtPosition(
                        childAtPosition(
                                withId(R.id.topAppBar),
                                1),
                        0),
                        isDisplayed()));
        overflowMenuButton.perform(click());

        // Go to the "add category" menu
        ViewInteraction materialTextView = onView(
                allOf(withId(R.id.title), withText(R.string.add_new_category),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.content),
                                        0),
                                0),
                        isDisplayed()));
        materialTextView.perform(click());

        onView(allOf(withId(R.id.categoryNameEditText)))
                .perform(replaceText("Test Category"), closeSoftKeyboard());

        // Save category.
        onView(allOf(withId(R.id.saveCategoryBtn))).perform(click());

        // Go to the add entry menu.
        onView(allOf(withId(R.id.fab), isDisplayed())).perform(click());

        onView(allOf(withId(R.id.entryNameEditText)))
                .perform(scrollTo(), replaceText("Test Entry"),
                        closeSoftKeyboard());

        onView(allOf(withId(R.id.entryDescriptionEditText)))
                .perform(scrollTo(), replaceText("Description Test"),
                        closeSoftKeyboard());

        onView(allOf(withId(R.id.linkEditText)))
                .perform(scrollTo(), replaceText("test.com"),
                        closeSoftKeyboard());

        onView(allOf(withId(R.id.usernameEditText)))
                .perform(scrollTo(), replaceText("testuser"),
                        closeSoftKeyboard());

        onView(allOf(withId(R.id.categoriesSpinner))).perform(scrollTo(), click());

        DataInteraction appCompatCheckedTextView2 = onData(anything())
                .inAdapterView(childAtPosition(
                        withClassName(is("android.widget.PopupWindow$PopupBackgroundView")),
                        0))
                .atPosition(10); // Select the "Test Category" category from the spinner.
        appCompatCheckedTextView2.perform(click());

        ViewInteraction checkableImageButton2 = onView(
                allOf(withId(R.id.text_input_end_icon), // Check the "show password" icon.
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        1),
                                0),
                        isDisplayed()));
        checkableImageButton2.perform(click());

        onView(allOf(withId(R.id.entryPassEditText)))
                .perform(scrollTo(), replaceText("test123"),
                        closeSoftKeyboard());
        // Save the entry.
        onView(allOf(withId(R.id.saveEntryBtn))).perform(scrollTo(), click());

        // Long click the "Test Category" category.
        ViewInteraction categoriesRecyclerView = onView(allOf(withId(R.id.categoriesRecyclerView)));
        categoriesRecyclerView.perform(actionOnItemAtPosition(10, longClick()));

        // Click the delete category button.
        ViewInteraction actionMenuItemView = onView(
                allOf(withId(R.id.deleteCategoryBtn),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.action_mode_bar),
                                        1),
                                1),
                        isDisplayed()));
        actionMenuItemView.perform(click());

        // Click the confirmation button in the dialog.
        ViewInteraction materialButton4 = onView(
                allOf(withId(android.R.id.button1),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.buttonPanel),
                                        0),
                                3)));
        materialButton4.perform(scrollTo(), click());

        // Click the "Others" Category. TODO: Make sure that this category is visible on screen.
        categoriesRecyclerView.perform(actionOnItemAtPosition(0, click()));
        // Click the "Test Entry" entry.
        ViewInteraction recyclerView8 = onView(
                allOf(withId(R.id.entriesRecyclerView),
                        childAtPosition(
                                childAtPosition(
                                        hasDescendant(withText("Others")),
                                        5),
                                0)));
        recyclerView8.perform(actionOnItemAtPosition(1, click()));
        onView(allOf(withId(R.id.entryNameTextView), isDisplayed()))
                .check(matches(withText("Test Entry")));
        onView(allOf(withId(R.id.usernameTextView), isDisplayed()))
                .check(matches(withText("testuser")));
        onView(allOf(withId(R.id.entryPassTextView), isDisplayed()))
                .check(matches(withText("test123")));
        onView(allOf(withId(R.id.entryDescriptionTextView), isDisplayed()))
                .check(matches(withText("Description Test")));
        onView(allOf(withId(R.id.linkTextView), isDisplayed()))
                .check(matches(withText("test.com")));
        pressBack();
    }

    private static Matcher<View> childAtPosition(
            final Matcher<View> parentMatcher, final int position) {

        return new TypeSafeMatcher<View>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("Child at position " + position + " in parent ");
                parentMatcher.describeTo(description);
            }

            @Override
            public boolean matchesSafely(View view) {
                ViewParent parent = view.getParent();
                return parent instanceof ViewGroup && parentMatcher.matches(parent)
                        && view.equals(((ViewGroup) parent).getChildAt(position));
            }
        };
    }
}
