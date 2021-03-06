package com.soontobe.joinpay.widget;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.EditText;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.soontobe.joinpay.R;
import com.soontobe.joinpay.model.UserInfo;


/**
 * This customized popup windows allows users to edit transaction
 * details for a specific user.
 */
public class BigBubblePopupWindow extends PopupWindow {

    /**
     * For tagging logs from this class.
     */
    public static final String TAG = "popup";

    /**
     * The user for who this popup window is editing a
     * transaction.
     */
    private UserInfo mUserInfo;

    /**
     * Users input the amount of money to set for the user here.
     */
    private EditText mEditText;

    /**
     * Used to display the user's name.
     */
    private TextView mTextView;

    /**
     * @return This window's user.
     * @see #mUserInfo
     */
    public final UserInfo getUserInfo() {
        return mUserInfo;
    }

    /**
     * Updates the window to display the given user.  NOTE:
     * this will cause a event to be fired to all TextWatchers for
     * this popup window.
     *
     * @param info The user to display in the window.
     */
    public final void setUserInfo(final UserInfo info) {
        if (info == null) {
            Log.e(TAG, "Cannot set popup window to NULL user");
            return;
        }

        this.mUserInfo = info;

        // The user's name goes at the top of the window
        mTextView.setText(info.getUserName());

        // The user's amount goes in the EditText
        mEditText.setText(info.getAmountOfMoney().toString());

        Log.d(TAG, String.format("Popup configure for user: %s "
                        + "| amount: %s",
                info.getUserName(),
                info.getAmountOfMoney().toString()));
    }

    /**
     * Constructs a new BigBubblePopupWindow for the given user.
     *
     * @param contentView The View in which the popup window is displayed.
     * @param userInfo    The user that the window was created for.
     */
    public BigBubblePopupWindow(final View contentView,
                                final UserInfo userInfo) {
        super(contentView, LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT, true);

        // Get the attached view's background so that the user
        // can click outside of the popup to close it
        this.setBackgroundDrawable(contentView.getBackground());

        Log.d(TAG, "Creating big bubble popup window");

        // Collect UI elements
        mEditText = (EditText) contentView
                .findViewById(R.id.edittext_inbubble);
        mTextView = (TextView) contentView
                .findViewById(R.id.textview_name_inbubble);

        // Configure UI to display the given user.
        setUserInfo(userInfo);
    }

    /**
     * Sets the text fields focus listener.
     *
     * @param listener The listener to assign to the amount text field.
     */
    public final void setFocusListener(
            final View.OnFocusChangeListener listener) {
        mEditText.setOnFocusChangeListener(listener);
    }

    /**
     * Sets the OnEditorActionListener of the popup window's amount
     * field.
     *
     * @param listener The listener for the amount field.
     */
    public final void setEditorListener(
            final TextView.OnEditorActionListener listener) {
        mEditText.setOnEditorActionListener(listener);
    }
}
