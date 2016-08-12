package com.jesstech.bluetoothspp;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;



public class DeviceItemAdapter extends BaseAdapter {

	private Context context;
	private List<DeviceData> device_lists;
	private LayoutInflater layoutInflater;
	private CheckBox cb ;
	
	
	DeviceItemAdapter(Context context, List<DeviceData> device_lists)
	{
		this.context = context ;
		this.device_lists = device_lists ; 
		layoutInflater = LayoutInflater.from(this.context);
	}
	
	public int getCount() {
		return device_lists.size();
	}

	public Object getItem(int position) {
		return device_lists.get(position);
	}

	public long getItemId(int position) {
		return position;
	}

	public View getView(int position,View convertView,ViewGroup parent) {
		if (convertView == null) {
            convertView = layoutInflater.inflate(R.layout.device_item, null);
        }
		
		cb = (CheckBox)convertView.findViewById(R.id.checkBox1);
		cb.setText(device_lists.get(position).get_device_name());
		byte id = device_lists.get(position).get_device_id();
		cb.setChecked(device_lists.get(position).get_device_status(id));
		return convertView;
	}
	
}