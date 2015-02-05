package com.soontobe.joinpay;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TabHost;
import android.widget.Toast;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TabHost.TabSpec;
import android.widget.TabWidget;
import android.widget.TextView;

import com.soontobe.joinpay.fragment.TransactionFragment;
import com.soontobe.joinpay.fragment.HistoryFragment;
import com.soontobe.joinpay.fragment.RequestFragment;
import com.soontobe.joinpay.fragment.SendFragment;

/**
 * 
 * Main activity.
 *
 */

/* TODO: 
 * 		1. Numeric Keyboard should contain arithmetic operations
 */

public class RadarViewActivity extends FragmentActivity 
implements OnTabChangeListener, TransactionFragment.OnFragmentInteractionListener,
HistoryFragment.OnFragmentInteractionListener {

	final String serviceContext = "RadarViewActivity";
	
	private TabHost mTabHost;
	private int mCurrentTab;
	private SendFragment mSendFragment;
	private RequestFragment mRequestFragment;
	private HistoryFragment mHistoryFragment;

	public static final String JUMP_KEY = "_jump";
	private static final String TAG = "RadarViewActivity";
	private static final String TAG_SEND = "tab_send";
	private static final String TAG_REQUEST = "tab_request";
	private static final String TAG_HISTORY = "tab_history";

	private static final int contactListRequestCode = 1;
	private static final int proceedToConfirmRequestCode = 2;
	public static final int historyRequestCode = 3;
	private static final int sendTab = 0;
	private static final int requestTab = 1;
	private static final int historyTab = 2;
	private boolean endOnce = false;

	private ArrayList<String[]> paymentInfo;

	public Map<String, Boolean> lockInfo;

	WebConnector webConnector;
	private ArrayList<String> fileNameList; 	// posttestserver
	private int visitedFilesCount = 0; 			// posttestserver
	private Set<String> onlineNameList = new HashSet<String>();

	final static int maxPositions = 5;
	
	ArrayList<Integer> usedPositionsListSendFragment = new ArrayList<Integer>();
	ArrayList<Integer> usedPositionsListRequestFragment = new ArrayList<Integer>();
	ArrayList<String> namesOnScreen = new ArrayList<String>();
	
	WebConnector WebConnector;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);  //No Title Bar
		setContentView(R.layout.activity_radar_view);
		//mSendFragment = new SendFragment();
		//mRequestFragment = new RequestFragment();
		//mHistoryFragment = new HistoryFragment();
		MainActivity.context = this;
		mTabHost = (TabHost)findViewById(android.R.id.tabhost);
		setupTabs();
		mTabHost.setOnTabChangedListener(this);

		//Receive jump command
		//Intent intent = getIntent();
		//int jump_target = intent.getIntExtra(JUMP_KEY, 0);
		//if(jump_target == historyRequestCode){
		//	mCurrentTab = historyTab;
		//}

		if (savedInstanceState != null) {
			Log.d("history","resuming history fragment");
			Log.d("send","resuming history fragment");
			Log.d("request","resuming history fragment");
			mHistoryFragment = (HistoryFragment) getFragmentManager().findFragmentByTag(TAG_HISTORY);
			mSendFragment = (SendFragment) getFragmentManager().findFragmentByTag(TAG_SEND);
			mRequestFragment = (RequestFragment) getFragmentManager().findFragmentByTag(TAG_REQUEST);
		} else {
			Log.d("history","creating history fragment");
			Log.d("send","creating send fragment");
			Log.d("request","creating request fragment");
			mHistoryFragment = new HistoryFragment();
			mSendFragment = new SendFragment();
			mRequestFragment = new RequestFragment();
		}
		 
		mCurrentTab = requestTab;
		mTabHost.setCurrentTab(sendTab);
		getFragmentManager().beginTransaction().replace(R.id.tab_send, mSendFragment, TAG_SEND).commit();
		
		mTabHost.setCurrentTab(requestTab);
		getFragmentManager().beginTransaction().replace(R.id.tab_request, mRequestFragment, TAG_REQUEST).commit();
			

		lockInfo = new HashMap<String, Boolean>();
		lockInfo.put("total", false);
		setEventListeners();
		runPostTestServer();
		IntentFilter restIntentFilter = new IntentFilter(Constants.RESTRESP);
		registerReceiver(restResponseReceiver, restIntentFilter);
	}
	
	@Override
	protected void onStop(){
		try{
			unregisterReceiver(restResponseReceiver);		//remove the receiver
		}
		catch(Exception e){}
	    super.onStop();
	}
	
	BroadcastReceiver restResponseReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String receivedServiceContext = intent.getStringExtra("context");
			
			if(serviceContext.equals(receivedServiceContext)) {
				String url = intent.getStringExtra("url");
				String method = intent.getStringExtra("method");
				String response = intent.getStringExtra("response");
				
				try {
					int pos = 0;
					JSONArray arr = new JSONArray(response);
					for(int i = 0; i < arr.length(); i++) {
						JSONObject objUser = arr.getJSONObject(i);
						String user = objUser.getString("username");
						if(namesOnScreen.contains(user)) {
							continue;
						}
						if(!usedPositionsListSendFragment.contains(pos)) {
							namesOnScreen.add(user);
							mSendFragment.addContactToView(user, pos);
							mRequestFragment.addContactToView(user, pos);
							usedPositionsListSendFragment.add(pos);
						} else {
							i--;
						}
						pos++;
					}
					endOnce = false;
				} catch (JSONException e) {
					Log.d("nearby", "failed to parse response, look for error");
					try{
						JSONObject obj = new JSONObject(response);
						if(obj.has("message") && obj.getString("message").equals("Unauthorized")){
							Log.d("nearby", "see unauthorized");
							Toast.makeText(getApplicationContext(), "Lost connection to server, login again", Toast.LENGTH_LONG).show();
							if(!endOnce){
								Intent intentApplication = new Intent(getApplicationContext(), LoginActivity.class);
								startActivity(intentApplication);
								endOnce = true;
							}
						}
						else{
							Log.d("nearby", "something is wrong, not sure what");
							Toast.makeText(getApplicationContext(), "Unknown problem with server...", Toast.LENGTH_SHORT).show();
						}
					}
					catch(JSONException ex){
						if(response.toLowerCase(Locale.ENGLISH).contains("404")){
							Log.d("nearby", "its a 404");
							Toast.makeText(getApplicationContext(), "Cannot locate server", Toast.LENGTH_LONG).show();
							if(!endOnce){
								Intent intentApplication = new Intent(getApplicationContext(), LoginActivity.class);
								startActivity(intentApplication);
								endOnce = true;
							}
						}
						else {
							Log.d("nearby", "something is wrong, not sure what");
							Toast.makeText(getApplicationContext(), "Unknown problem with server...", Toast.LENGTH_SHORT).show();
						}
					}
					e.printStackTrace();
				}
			}
		}
	};
	
	/**
	 * This method aims to retrieve new payments from the website.
	 * For demo use only because the message retrieval mechanism is experimental.
	 */
	private void runPostTestServer() {
		Log.d("RadarViewActivity", "onCreate");
		new Thread() {
			@Override
			public void run() {
				boolean doOnce = false;
				Log.d("RadarViewActivity", "run");
				while (true) {
					try {
						if(doOnce) Thread.sleep(5000);
						else doOnce = true;
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					
					Intent intent = new Intent(getApplicationContext(), RESTCalls.class);
					String url = "http://join-pay.mybluemix.net/nearby/users";
					intent.putExtra("method","get");
					intent.putExtra("url",url);
					intent.putExtra("context", serviceContext);
					startService(intent);

					/*
					fileNameList = webConnector.getFileNameList(Constants.urlPrefix, visitedFilesCount);
					for (String i : fileNameList) {
						String newFile = webConnector.getFile(Constants.urlPrefix + "/" + i);
						for (String name : Constants.deviceNameList) {
							if (onlineNameList.contains(name) || name.equals(Constants.userName)) continue;
							if (newFile.contains(name + "IsOnline")) {
								onlineNameList.add(name);
							}
						}

						if (newFile.contains(Constants.transactionIntiatorTag + Constants.userName)) {
							continue;
						}

						int idx1 = newFile.indexOf(Constants.transactionBeginTag);
						int idx2 = newFile.indexOf(Constants.transactionEndTag);
						if (idx1 >= 0 && idx2 >= 0) {
							int st = idx1 + Constants.transactionBeginTag.length();
							int ed = idx2;
							String data = newFile.substring(st, ed);
							//							Log.d("RadarViewActivity paymentInfo from web", newFile.substring(st, ed));

							String[] paymentStrings = data.split("\\|");
							String [] relevantItem = {};
							String [] groupNote = {};
							String [] summary = {};
							for (int k = 0;k < paymentStrings.length;k++) {
								String[] items = paymentStrings[k].split(",");
								//								paymentInfoFromWeb.add(items);
								if (items.length >= 4) {
									if (items[2].equals(Constants.userName)) {  	//	payer
										relevantItem = items;
									} else if (items[3].equals(Constants.userName)) {	//	payee
										relevantItem = items;
									}
								}

								if (items[0].equals("group_note")) {
									groupNote = items;
								}

								if (items[0].equals("summary")) {
									summary = items;
								}
							}

							if (relevantItem.length > 0) {
								ArrayList<String []> popupPaymentInfo = new ArrayList<String[]>();
								popupPaymentInfo.add(relevantItem);
								popupPaymentInfo.add(groupNote);
								popupPaymentInfo.add(summary);
								

								final ArrayList<String []> finalInfo = popupPaymentInfo;
								runOnUiThread(new Runnable() {
									@Override
									public void run() {
										
										mHistoryFragment.addPendingTransItem(finalInfo);
										mCurrentTab = 2;	//jump to history view
										mTabHost.setCurrentTab(mCurrentTab);
									}
								});
							}



						}


					}
					visitedFilesCount += fileNameList.size();
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							for (String i : onlineNameList) {
								Log.d("onlineNameList", i);
								if (mCurrentTab == 0) {
									mSendFragment.addUserToView(i);
								} else if (mCurrentTab == 1) {
									mRequestFragment.addUserToView(i);
								}
							}
						}
					});
					Log.d("visitedFilesCount", "" + visitedFilesCount);
					*/
				}
			}
		}.start();
	}

	@Override
	public View onCreateView(String name, Context context, AttributeSet attrs) {
		// TODO Auto-generated method stub
		return super.onCreateView(name, context, attrs);
	}

	private void setEventListeners() {
		Button btn = (Button) findViewById(R.id.btn_radar_view_back);
		btn.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				Button btn = (Button) v;
				// TODO Auto-generated method stub
				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					btn.setBackgroundResource(R.drawable.arrow_active);
				} else if (event.getAction() == MotionEvent.ACTION_UP) {
					btn.setBackgroundResource(R.drawable.arrow_normal);
				}
				return false;
			}
		});

		/*btn = (Button) findViewById(R.id.btn_radar_view_cross);
		btn.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				Button btn = (Button) v;
				// TODO Auto-generated method stub
				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					btn.setBackgroundResource(R.drawable.cross_active);
				} else if (event.getAction() == MotionEvent.ACTION_UP) {
					btn.setBackgroundResource(R.drawable.cross_normal);
				}
				return false;
			}
		});*/
	}	
	

	private void setupTabs() {
		// Setup tabs
		mTabHost.setup();
		mTabHost.addTab(newTab(TAG_SEND, R.string.tab_send, R.id.tab_send));
		mTabHost.addTab(newTab(TAG_REQUEST, R.string.tab_request, R.id.tab_request));
		mTabHost.addTab(newTab(TAG_HISTORY, R.string.tab_history, R.id.tab_history));
		mTabHost.setCurrentTab(1);
		mTabHost.setCurrentTab(2);
		mTabHost.setCurrentTab(0);
	}

	private TabSpec newTab(String tag, int labelId, int tabContentId) {
		//Log.d(TAG, "buildTab(): tag=" + tag);
		View indicator = LayoutInflater.from(this).inflate(R.layout.tab, (ViewGroup) findViewById(android.R.id.tabs), false);
		((TextView) indicator.findViewById(R.id.tab_text)).setText(labelId);

		TabSpec tabSpec = mTabHost.newTabSpec(tag);
		tabSpec.setIndicator(indicator);
		tabSpec.setContent(tabContentId);
		return tabSpec;
	}
	
	private boolean mFragmentInitState[] = {true, false, false};
	@Override
	public void onTabChanged(String tabId) {
		//Log.d(TAG, "onTabChanged(): tabId=" + tabId);
		FragmentManager fm = getFragmentManager();
		if(TAG_SEND.equals(tabId)){
			Log.d("tab", "changing tab to send");
			fm.beginTransaction().replace(R.id.tab_send, mSendFragment, TAG_SEND).commit();
			mFragmentInitState[0] = true;
			mCurrentTab = 0;
			mSendFragment.setMyName(Constants.userName);
		}
		else if (TAG_REQUEST.equals(tabId)){
			Log.d("tab", "changing tab to request");
			fm.beginTransaction().replace(R.id.tab_request, mRequestFragment, TAG_REQUEST).commit();
			mFragmentInitState[1] = true;
			mCurrentTab = 1;
			mRequestFragment.setMyName(Constants.userName);
		}
		else if (TAG_HISTORY.equals(tabId)){
			Log.d("tab", "changing tab to history");
			fm.beginTransaction().replace(R.id.tab_history, mHistoryFragment, TAG_HISTORY).commit();
			mCurrentTab = 2;
			
			//Reset selected users and amounts
			ArrayList<Integer> targetUserIndex = mRequestFragment.getUnlockedSelectedUserIndex();
			for (Integer index : targetUserIndex) {
				if (index == -1) {
					Log.d("money", "clearing self money");
					mRequestFragment.myUserInfo.setAmountOfMoney(0);
					mRequestFragment.mSelfBubble.setUserInfo(mRequestFragment.myUserInfo);
				} else {
					Log.d("money", "clearing money");
					mRequestFragment.mUserInfoList.get(index).setAmountOfMoney(0);
					mRequestFragment.mUserBubbles.get(index).setUserInfo(mRequestFragment.mUserInfoList.get(index));
					mRequestFragment.mUserBubbles.get(index).setSelectState(false);
					mRequestFragment.mUserBubbles.get(index).switchExpandPanel(false);
				}
			}
		}
		else {
			Log.w("RadarViewActivity_onTabChanged", "Cannot find tab id=" + tabId);
		}

		// change history tab color. Should be refactored later.
		if (tabId.equals("tab_history")) {
			mTabHost.getTabWidget().getChildAt(mTabHost.getCurrentTab()).setBackgroundColor(Color.rgb(0xc2, 0xd4, 0x2d));
		} else {
			TabWidget tabWidget = mTabHost.getTabWidget();
			tabWidget.getChildAt(2).setBackgroundColor(Color.rgb(0xe6, 0xe6, 0xe6));
		}

	}

	@Override
	public void onFragmentInteraction(Uri uri) {
		Log.d(TAG, uri.toString());		
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == contactListRequestCode) { 
			if (resultCode == RESULT_OK) {
				String nameArray[];
				
				//Send
				nameArray = data.getStringArrayExtra("name");
				Constants.debug(nameArray);
				for(String name: nameArray){
					boolean foundFree = false;
					for(int i = maxPositions - 1; i >= 0; i--) {
						if(namesOnScreen.contains(name)) {																	//to do  - this doesn't work correctly yet
							Toast.makeText(getApplicationContext(), "This user is already added", Toast.LENGTH_SHORT).show();
							continue;
						}
						if(!usedPositionsListSendFragment.contains(i)) {
							Log.d("bubble", "adding user to position " + i);
							mSendFragment.addContactToView(name, i);
							mRequestFragment.addContactToView(name, i);
							usedPositionsListSendFragment.add(i);
							foundFree = true;
							break;	
						}
						else{
							Log.d("bubble", "skipping, position taken");
						}
					}
					if(!foundFree) {
						Toast.makeText(getApplicationContext(), "Maximum users reached", Toast.LENGTH_SHORT).show();
					}
				}
				
				/*dsh - removed, new code above adds to both tabs
				 * switch(mCurrentTab){
				case 0:
					//Send
					nameArray = data.getStringArrayExtra("name");
					for(String name: nameArray){
						int i;
						for(i = maxPositions - 1; i >= 0; i--) {
							if(!usedPositionsListSendFragment.contains(i)) {
								mSendFragment.addContactToView(name,i);
								usedPositionsListSendFragment.add(i);
								break;	
							} 
						}
						if(i < 0) {
							Toast.makeText(getApplicationContext(), "Maximum users reached", Toast.LENGTH_SHORT).show();
						}
					}
					//mSendFragment.addContactToView(data.getDataString());
					break;
				case 1:
					//Request
					nameArray = data.getStringArrayExtra("name");
					for(String name: nameArray){
						int i;
						if(namesOnScreen.contains(name)) {
							continue;
						}
						for(i = maxPositions - 1; i >= 0; i--) {
							if(!usedPositionsListRequestFragment.contains(i)) {
								namesOnScreen.add(name);
								mRequestFragment.addContactToView(name,i);
								usedPositionsListRequestFragment.add(i);
								break;	
							} 
						}
						if(i < 0) {
							Toast.makeText(getApplicationContext(), "Maximum users reached", Toast.LENGTH_SHORT).show();
						}
						//						mRequestFragment.addContactToView(name);
					}
					//Request
					break;
				default:
					break;
				}*/
				//TODO: Inform mSendFragment of mRequestFragment that we have new contact selected
				// switch case.: mCurrentTab
			} 
		} else if (requestCode == proceedToConfirmRequestCode) {
			if (resultCode == RESULT_OK) {
				paymentInfo = new ArrayList<String []>();
				String dataString = data.getData().toString();
				String[] paymentStrings = dataString.split("\\|");
				for (int i = 0;i < paymentStrings.length;i++) {
					String[] items = paymentStrings[i].split(",");
					paymentInfo.add(items);
				}
				mTabHost.setCurrentTab(historyTab);
			} 
		}
	}
	
	/**
	 * on send button click
	 * @param v
	 */
	public void proceedToConfirm(View v) {
		Intent i = new Intent(this, SendConfirmActivity.class);
		ArrayList<String[]> paymentInfo = new ArrayList<String[]>();
		switch(mCurrentTab) {
		case 0:
			paymentInfo = mSendFragment.getPaymentInfo();
			i.putExtra("transactionType", "Send");
			break;
		case 1:
			paymentInfo = mRequestFragment.getPaymentInfo();
			i.putExtra("transactionType", "Request");
			break;
		default:
			break;
		}
		Bundle extras = new Bundle();
		extras.putSerializable("paymentInfo", paymentInfo);
		i.putExtras(extras);
		startActivityForResult(i, proceedToConfirmRequestCode);
	}

	public void contactButtonOnClick(View v) {
		//Log.d("contactButtonOnClick", "clicked");
		startActivityForResult(new Intent(this, ContactListActivity.class), contactListRequestCode);
	}

	public void setSendTotalLock(View v) {
		ImageView iv = (ImageView) v;
		if (lockInfo.get("total")) {
			iv.setImageResource(R.drawable.unlocked_darkgreen);
			lockInfo.put("total", false);
			findViewById(R.id.edit_text_total_amount).setEnabled(true);
		} else {
			iv.setImageResource(R.drawable.locked_darkgreen);
			lockInfo.put("total", true);
			findViewById(R.id.edit_text_total_amount).setEnabled(false);
		}
	}

	public void onClickBackButton(View v){
		Intent i = new Intent(this, MainActivity.class);
		startActivity(i);
		finish();
	}

	public void onClickClearButton(View v){
		switch(mCurrentTab){
		case 0:
			mSendFragment.clearUserMoneyAmount();
			break;
		case 1:
			mRequestFragment.clearUserMoneyAmount();
			break;
		}
		//Clear total lock state
		
		lockInfo.put("total", false);
		findViewById(R.id.edit_text_total_amount).setEnabled(true);
		ImageView lockView = (ImageView)findViewById(R.id.send_total_lock);
		lockView.setImageResource(R.drawable.unlocked_darkgreen);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if(keyCode == KeyEvent.KEYCODE_BACK){
			onClickBackButton(getCurrentFocus());
			return true;
		}

		return super.onKeyDown(keyCode, event);
	}
}
