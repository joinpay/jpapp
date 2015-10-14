package com.soontobe.joinpay;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import com.ibm.mobile.services.core.IBMBluemix;
import com.ibm.mobile.services.push.IBMPush;
import com.ibm.mobile.services.push.IBMPushNotificationListener;
import com.ibm.mobile.services.push.IBMSimplePushNotification;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import bolts.Continuation;
import bolts.Task;

/**
 * This class is the access point of the whole application.
 * After the user hit the "JoinPay" button, it will jump to the radar view pane.
 */
public class MainActivity extends Activity{

	private static final String TAG = "main_screen";
	private IBMPush push = null;
	private IBMPushNotificationListener notificationlistener = null;
	public static Context context;

	// For getting a profile picture
	static final int REQUEST_IMAGE_CAPTURE = 1;
	String mCurrentPhotoPath;

	@SuppressLint("DefaultLocale")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);		
		requestWindowFeature(Window.FEATURE_NO_TITLE);	// No Title Bar
		setContentView(R.layout.activity_main);

		// Populate a URL to allow users to create a Citi Bank account
		TextView link = (TextView) findViewById(R.id.citiSignupTextView);
	    link.setMovementMethod(LinkMovementMethod.getInstance());

		// Display a welcome message to the currently logged in user
	    TextView userView = (TextView) findViewById(R.id.welcome_user_name);
	    String username = String.valueOf(Constants.userName.charAt(0)).toUpperCase() + Constants.userName.substring(1, Constants.userName.length());
	    userView.setText(String.format(getString(R.string.welcome_message), username));

		// Flush the cache whenever the app is started, so that the chat tab stays
		// up to date with the chat web page
        clearCache(this, getResources().getInteger(R.integer.minutes_to_keep));

		///////////////////////////////////////////////////
		/////////////////  IBM Push Code  ///////////////// 
		///////////////////////////////////////////////////
		context = this;
		IBMBluemix.initialize(this, Constants.appKey, Constants.appSecret, Constants.baseURL);			//init for IBM push
		push = IBMPush.initializeService();
		notificationlistener = new IBMPushNotificationListener() {
		
			@Override
			public void onReceive(final IBMSimplePushNotification message) {		
				Log.d(Constants.PUSH_TAG, "I got a push msg");
				runOnUiThread(new Runnable() {
					
					///////////////////////////////////////////////////
					/////////////////  PUSH Received  /////////////////
					///////////////////////////////////////////////////
					@Override
					public void run() {
						Log.d(Constants.PUSH_TAG, "msg: " + message.getAlert());
						
						///////////////// Open Approve / Deny Dialog /////////////////
						try{
							final Dialog dialog = new Dialog(context);
							dialog.setContentView(R.layout.push_msg_dialog);
							dialog.setTitle("New Message");
							TextView text = (TextView) dialog.findViewById(R.id.push_msg_text);
							text.setText(message.getAlert());
				 
							Button dialogButtonCancel = (Button) dialog.findViewById(R.id.push_dialog_button);
							dialogButtonCancel.setOnClickListener(new OnClickListener() {
								@Override
								public void onClick(View v) {
									dialog.dismiss();
								}
							});
							dialog.show();
						}
						catch(Exception e){
							Log.e(Constants.PUSH_TAG,"dialog error");
							e.printStackTrace();
						}
					}
				});
			}
		};
		
		///////////////// More Push Registration /////////////////
		push.register("dev4", Constants.userName).continueWith(new Continuation<String, Void>() {
			@Override
			public Void then(Task<String> task) throws Exception {
				if(task.isFaulted()) {
					Log.e(Constants.PUSH_TAG, "failed to push list of subscriptions");
					return null;
				} else {
					push.getSubscriptions().continueWith(new Continuation<List<String>, Void>(){
						public Void then(Task<List<String>> task1) throws Exception{
							if(task1.isFaulted()) {
								Log.e(Constants.PUSH_TAG, "failed to push list of subscriptions");
							} else {
								List<String> tags = task1.getResult();
								if(tags.size() > 0) {
									push.unsubscribe(tags.get(0)).continueWith(new Continuation<String, Void>() {

										@Override
										public Void then(Task<String> task2) throws Exception {
											if(task2.isFaulted()) {
												Log.e(Constants.PUSH_TAG, "subscribe failed");
											} else {
												push.subscribe("testtag").continueWith(new Continuation<String, Void>() {
													public Void then(bolts.Task<String> task1) throws Exception {
														if(task1.isFaulted()) {
															Log.e(Constants.PUSH_TAG,"Push Subscription Failed" + task1.getError().getMessage());
														} else {
															push.listen(notificationlistener);
															Log.d(Constants.PUSH_TAG,"Push Subscription Success");
														}
														return null;
													};
												});
											}
											return null;
										}
									});
								} else {
									Log.d(Constants.PUSH_TAG, "" + task1.getResult());
									push.subscribe("testtag").continueWith(new Continuation<String, Void>() {
										public Void then(bolts.Task<String> task1) throws Exception {
											if(task1.isFaulted()) {
												Log.e(Constants.PUSH_TAG,"Push Subscription Failed" + task1.getError().getMessage());
											} else {
												push.listen(notificationlistener);
												Log.d(Constants.PUSH_TAG,"Push Subscription Success");
											}
											return null;
										};
									});
								}
							}
							return null;
						}
					});
					return null;
				}
			}
		});
	}

    /**
     * Recursively clears and deletes the given directory.
     * @param dir The directory to be deleted.
     * @param numMinutes The age after which files can be destroyed.
     * @return The number of deleted files.
     */
    private static int clearCacheFolder(final File dir, final int numMinutes) {
        int deletedFiles = 0;
        if (dir != null && dir.isDirectory()) {
            try {
                for (File child : dir.listFiles()) {
                    // Recursively delete subdirectories
                    if(child.isDirectory())
                        deletedFiles += clearCacheFolder(child, numMinutes);

                    // Delete files and subdirectories in this dir
                    // only empty dirs can be deleted, so subdirs are deleted recursively first
                    if (child.lastModified() < new Date().getTime() - numMinutes * DateUtils.MINUTE_IN_MILLIS) {
                        if(child.delete())
                            deletedFiles++;
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, String.format("Failed to clean the cache, error %s", e.getMessage()));
            }
        }
        return deletedFiles;
    }

    /**
     * Clears the cache folder of the application.
     * @param context The context in which the cache is stored.
     * @param numMinutes The age after which files are deleted.
     */
    public static void clearCache(final Context context, final int numMinutes) {
        Log.i(TAG, String.format("Starting cache prune. Deleting files older than %d minutes", numMinutes));
        int numDeletedFiles = clearCacheFolder(context.getCacheDir(), numMinutes);
        Log.i(TAG, String.format("Cache pruning completed. %d files deleted", numDeletedFiles));
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * This method is called when the user presses the "Enter" button.  It starts the radar
	 * activity.
	 * @param view The View that was clicked.
	 */
	public void onEnterClicked(View view){
		Log.d(TAG, "\"" + getString(R.string.button_enter) + "\" clicked");
		startActivity(new Intent(this, RadarViewActivity.class));
	}

	/**
	 * This method is called when the user presses the "check points" button.  It starts the
	 * points summary activity.  Currently, this button is hidden and cannot be clicked.
	 * @param view The View that was clicked.
	 */
	public void onCheckPointsClicked(View view){
		Log.d(TAG, "\"" + getString(R.string.button_points) + "\" clicked");
		startActivity(new Intent(this, PointBalance.class));
	}

	/**
	 * This method is called when the user presses the "my accounts" button. It starts the
	 * account activity.
	 * @param view The View that was clicked.
	 */
	public void onAccountsClicked(View view) {
		Log.d(TAG, "\"" + getString(R.string.button_accounts) + "\" clicked");
		startActivity(new Intent(this, CitiAccountActivity.class));		
	}

	/**
	 * This method is called when the user presses the "logout" button.  It returns to the login
	 * activity and ends this activity.
	 * @param view The View that was clicked.
	 */
	public void onLogoutClicked(View view) {
		Log.d(TAG, "\"" + getString(R.string.button_logout) + "\" clicked");
		startActivity(new Intent(this, LoginActivity.class));

		// Updates to the user's location should stop when they log out
		Intent locationServiceIntent = new Intent(getApplicationContext(), SendLocation.class);
		stopService(locationServiceIntent);
		push.unsubscribe("testtag");
		finish();
	}

	/**
	 * Called when the user presses the "profile picture" button.  It dispatches an intent
	 * which allows the user to take a picture.
	 * @param view The View that was clicked.
	 */
	public void onProfilePictureClicked(View view) {
		Log.d(TAG, "\"" + getString(R.string.button_picture) + "\" clicked");
		startActivity(new Intent(this, PictureActivity.class));
	}
	
	@Override
	protected void onStop() {
		// Stop the activity
		super.onStop();
	}
}
