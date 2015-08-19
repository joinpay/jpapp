package com.soontobe.joinpay;

import java.util.List;

import bolts.Continuation;
import bolts.Task;

import com.ibm.mobile.services.core.IBMBluemix;
import com.ibm.mobile.services.push.IBMPush;
import com.ibm.mobile.services.push.IBMPushNotificationListener;
import com.ibm.mobile.services.push.IBMSimplePushNotification;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;


/**
 * This class is the access point of the whole application. After the user hit the "JoinPay" button, it will jump to the radar view pane.
 *
 */
public class MainActivity extends Activity{
	private IBMPush push = null;
	private IBMPushNotificationListener notificationlistener = null;
	public static Context context;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);		
		requestWindowFeature(Window.FEATURE_NO_TITLE);  					//No Title Bar
		setContentView(R.layout.activity_main);
		TextView link = (TextView) findViewById(R.id.citiLink);
	    String linkText = "Don't have a citi account? <a href='http://citi-online-banking.mybluemix.net/'>Sign Up Here</a>";
	    link.setText(Html.fromHtml(linkText));
	    link.setMovementMethod(LinkMovementMethod.getInstance());
	    
	    TextView userView = (TextView) findViewById(R.id.welcome_user_name);
	    userView.setText("Welcome " + Constants.userName);
				
		///////////////////////////////////////////////////
		/////////////////  IBM Push Code  ///////////////// 
		///////////////////////////////////////////////////
		context = this;
		IBMBluemix.initialize(this, Constants.appKey, Constants.appSecret, Constants.baseURL);			//init for IBM push
		push = IBMPush.initializeService();
		notificationlistener = new IBMPushNotificationListener() {
		
			@Override
			public void onReceive(final IBMSimplePushNotification message) {		
				Log.d("push", "I got a push msg");
				runOnUiThread(new Runnable() {
					
					///////////////////////////////////////////////////
					/////////////////  PUSH Received  /////////////////
					///////////////////////////////////////////////////
					@Override
					public void run() {
						Log.d("push", "msg: " + message.getAlert());
						
						///////////////// Open Approve / Deny Dialog /////////////////
						try{
							final Dialog dialog = new Dialog(context);
							dialog.setContentView(R.layout.dialog);
							dialog.setTitle("New Message");
							TextView text = (TextView) dialog.findViewById(R.id.text);
							text.setText(message.getAlert());
				 
							Button dialogButtonCancel = (Button) dialog.findViewById(R.id.dialogButtonCancel);
							dialogButtonCancel.setOnClickListener(new OnClickListener() {
								@Override
								public void onClick(View v) {
									dialog.dismiss();
								}
							});
							dialog.show();
						}
						catch(Exception e){
							Log.e("push","dialog error");
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
					Log.e("push", "failed to push list of subscriptions");
					return null;
				} else {
					push.getSubscriptions().continueWith(new Continuation<List<String>, Void>(){
						public Void then(Task<List<String>> task1) throws Exception{
							if(task1.isFaulted()) {
								Log.e("push", "failed to push list of subscriptions");
							} else {
								List<String> tags = task1.getResult();
								if(tags.size() > 0) {
									push.unsubscribe(tags.get(0)).continueWith(new Continuation<String, Void>() {

										@Override
										public Void then(Task<String> task2) throws Exception {
											if(task2.isFaulted()) {
												Log.e("push", "subscribe failed");
											} else {
												push.subscribe("testtag").continueWith(new Continuation<String, Void>() {
													public Void then(bolts.Task<String> task1) throws Exception {
														if(task1.isFaulted()) {
															Log.e("push","Push Subscription Failed" + task1.getError().getMessage());	
														} else {
															push.listen(notificationlistener);
															Log.d("push","Push Subscription Success");								
														}
														return null;
													};
												});
											}
											return null;
										}
									});
								} else {
									Log.d("push", "" + task1.getResult());
									push.subscribe("testtag").continueWith(new Continuation<String, Void>() {
										public Void then(bolts.Task<String> task1) throws Exception {
											if(task1.isFaulted()) {
												Log.e("push","Push Subscription Failed" + task1.getError().getMessage());	
											} else {
												push.listen(notificationlistener);
												Log.d("push","Push Subscription Success");								
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

	public void onButtonClick(View view){		//join pay button
		Log.d("button", "join pay click");
		startActivity(new Intent(this, RadarViewActivity.class));
	}
	
	public void onCheckPointsClicked(View view){		//check points point
		Log.d("button", "click button 0");
		startActivity(new Intent(this, PointBalance.class));
	}

	public void onCitiAccountClicked(View view) {
		startActivity(new Intent(this, CitiAccountActivity.class));		
	}
	
	public void onLogoutClicked(View view) {
		startActivity(new Intent(this, LoginActivity.class));
		finish();
	}
	
	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
	}
}
