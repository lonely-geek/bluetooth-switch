/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jesstech.bluetoothspp;

import java.util.ArrayList;
import java.util.List;
import com.jesstech.bluetoothspp.R;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

/**
 * This is the main Activity that displays the current chat session.
 */
public class BluetoothChat extends Activity {
    // Debugging
    private static final String TAG = "BluetoothChat";
    private static final boolean D = true;

    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;

    
    // Layout Views
    private TextView mTitle;
    private ListView mConversationView;
    private List<DeviceData> device_lists;
    

    // Name of the connected device
    private String mConnectedDeviceName = null;
    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    // Member object for the chat services
    private BluetoothChatService mChatService = null;
    // Device Address
    private String device_mac_addr = "";
    
    SharedPreferences settings = null ;
    
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(D) Log.e(TAG, "+++ ON CREATE +++");
        
        // Set up the window layout
        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.main);
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.custom_title);
        
        // Set up the custom title
        mTitle = (TextView) findViewById(R.id.title_left_text);
        mTitle.setText(R.string.app_name);
        mTitle = (TextView) findViewById(R.id.title_right_text);
        
        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        
        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
       
        // Initialize the array adapter for the conversation thread
        //mConversationArrayAdapter = new ArrayAdapter<String>(this, R.layout.message);
        mConversationView = (ListView)findViewById(R.id.in);
        device_lists = getLists();
        mConversationView.setAdapter(new DeviceItemAdapter(this, device_lists));
        mConversationView.setOnItemClickListener(sendDataListener);
        
        settings = getSharedPreferences(Public.PREFS_NAME, 0);
		Public.b_hex = settings.getBoolean(Public.SETTING_SEND_MODE, true);
		Public.b_secure = settings.getBoolean(Public.SETTING_SECURE_CONNECT, false);
		Public.b_fix_channel = settings.getBoolean(Public.SETTING_FIX_CHANNEL, false);		
    }
    
    @Override
    public void onStart() {
        super.onStart();
        if(D) Log.e(TAG, "++ ON START ++");
        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
        	Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        	startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        } else {
            if (mChatService == null) setupChat();
        }
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        if(D) Log.e(TAG, "+ ON RESUME +");
        
        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mChatService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
            	// Start the Bluetooth chat services
            	mChatService.start();
            	
            	device_mac_addr = settings.getString(Public.SETTING_MAC_ADDR, "");
            	if(device_mac_addr.length()== 17){        		
            		BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(device_mac_addr);
            		mChatService.connect(device, Public.b_secure);
            	}
            }
            
            if(mChatService.getState() == BluetoothChatService.STATE_CONNECTED){
             	byte status_req[] = {0x60} ;
                sendBinary(status_req);
            }
        }
    }
    
    @Override
    public synchronized void onPause() {
        super.onPause();
        if(D) Log.e(TAG, "- ON PAUSE -");
    }

    @Override
    public void onStop() {
        super.onStop();
        if(D) Log.e(TAG, "-- ON STOP --");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop the Bluetooth chat services
        if (mChatService != null) mChatService.stop();
        if(D) Log.e(TAG, "--- ON DESTROY ---");
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.option_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent serverIntent = null;
        switch (item.getItemId()) {
        case R.id.insecure_connect_scan:
            // Launch the DeviceListActivity to see devices and do scan
            serverIntent = new Intent(this, DeviceListActivity.class);
            startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_INSECURE);
            return true;
        case R.id.setting:
        	serverIntent = new Intent(this, SettingActivity.class);
            startActivity(serverIntent);
            return true;
        }
        return false;
    }
    
    
    /************************  Private Methods *************************************/
    
    private  List<DeviceData> getLists()
    {
    	 List<DeviceData> lists = new ArrayList<DeviceData>();
    	 for(byte i = 0; i < 4 ;i++)
    	 {
    		 DeviceData dd = new DeviceData();
    		 dd.set_device_name("Switch - " + (i+1));
    		 dd.set_device_id(i);
    		 dd.set_device_status(i, false);
    		 lists.add(dd);
    	 }
    	 return lists ;
    }
    
    private void updateStatus(byte status)
    {
    	if(status == 0xD0)return;
    	for(byte i = 0 ; i < 4 ; i++)
    	{
    		device_lists.get(i).set_all_device_status(status);
    		View view =  mConversationView.getChildAt(i);
    		CheckBox cb = (CheckBox)view.findViewById(R.id.checkBox1);
    		cb.setChecked(device_lists.get(i).get_device_status(i));
    	}

    }
    
    private void connectDevice(Intent data, boolean secure) {
        // Get the device MAC address
        String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        // Get the BLuetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        mChatService.connect(device, secure);
        // save the mac addr to database
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(Public.SETTING_MAC_ADDR, address);
        editor.commit();
    }
      
    private void setupChat() {
        Log.d(TAG, "setupChat()");
        
        // Initialize the BluetoothChatService to perform bluetooth connections
        mChatService = new BluetoothChatService(this, mHandler);
    }
    
    private void ensureDiscoverable() {
        if(D) Log.d(TAG, "ensure discoverable");
        if (mBluetoothAdapter.getScanMode() !=
            BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    private void sendBinary(byte[] buf) {
        // Check that we're actually connected before trying anything
        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }
        mChatService.write(buf);
    }
    

    
    // The Handler that gets information back from the BluetoothChatService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_STATE_CHANGE:
                if(D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                switch (msg.arg1) {
                case BluetoothChatService.STATE_CONNECTED:
                    mTitle.setText(R.string.title_connected_to);
                    mTitle.append(mConnectedDeviceName);
                    //mConversationArrayAdapter.clear();
                    break;
                case BluetoothChatService.STATE_CONNECTING:
                    mTitle.setText(R.string.title_connecting);
                    break;
                case BluetoothChatService.STATE_LISTEN:
                case BluetoothChatService.STATE_NONE:
                    mTitle.setText(R.string.title_not_connected);
                    break;
                }
                break;
            case MESSAGE_WRITE: {
                byte[] writeBuf = (byte[]) msg.obj;
                int len = msg.arg1;
                
                if (Public.b_hex) {
                	StringBuilder str = new StringBuilder();
                    //str.append("SEND: ");
                	
                    for (int i=0; i<len; i++) {
                    	str.append(String.format("%02X ", writeBuf[i]));
                    }                
                    //mConversationArrayAdapter.add(str.toString());
                } else {
                    String writeMessage = new String(writeBuf);
                    //mConversationArrayAdapter.add("SEND: " + writeMessage);
                    //mConversationArrayAdapter.add(writeMessage);
                }
                
                break;
            }
            case MESSAGE_READ: {
                byte[] readBuf = (byte[]) msg.obj;
            	int len = msg.arg1;
            	updateStatus(readBuf[0]);	
            	/*
                if (Public.b_hex) {
                    StringBuilder str = new StringBuilder();
                    str.append(mConnectedDeviceName+": ");
                    
                    for (int i=0; i<len; i++) {
                    	str.append(String.format("%02X ", readBuf[i]));
                    }
                   
                } else {
                	String readMessage = new String(readBuf, 0, len);
                }*/
                break;
            }
            case MESSAGE_DEVICE_NAME:
                // save the connected device's name
                mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                Toast.makeText(getApplicationContext(), "Connected to "
                               + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                byte status_req[] = {0x60} ;
                sendBinary(status_req);               
                break;
            case MESSAGE_TOAST:
                Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                               Toast.LENGTH_SHORT).show();
                break;
            }
        }
    };
    
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(D) Log.d(TAG, "onActivityResult " + resultCode);
        switch (requestCode) {
        case REQUEST_CONNECT_DEVICE_SECURE:
            // When DeviceListActivity returns with a device to connect
            if (resultCode == Activity.RESULT_OK) {
                connectDevice(data, true);
            }
            break;
        case REQUEST_CONNECT_DEVICE_INSECURE:
            // When DeviceListActivity returns with a device to connect
            if (resultCode == Activity.RESULT_OK) {
                connectDevice(data, Public.b_secure);
            }
            break;
        case REQUEST_ENABLE_BT:
            // When the request to enable Bluetooth returns
            if (resultCode == Activity.RESULT_OK) {
                // Bluetooth is now enabled, so set up a chat session
                setupChat();
            } else {
                // User did not enable Bluetooth or an error occured
                Log.d(TAG, "BT not enabled");
                Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
    
    private OnItemClickListener sendDataListener = new OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int position, long id) {
        	
        	CheckBox cb = (CheckBox)v.findViewById(R.id.checkBox1);
        	boolean status = cb.isChecked();
        	DeviceData da = (DeviceData)av.getAdapter().getItem(position);
        	da.set_device_status(da.get_device_id(), !status);
        	byte status_req[] = {(byte)(da.get_all_device_status()&0x7F)};
        	sendBinary(status_req);
        	updateStatus(status_req[0]);
        	cb.setChecked(!status);
        	
        }
    };
}
