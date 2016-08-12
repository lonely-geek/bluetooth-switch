package com.jesstech.bluetoothspp;


public class DeviceData 
{
	private String device_name ;
	private byte device_id ;
	private boolean device_status ;
	private byte all_device_status ;
	
	public DeviceData()
	{
		device_name = "NULL";
		device_id = -1 ;
		device_status = false ;
		all_device_status = 0 ;		
	}
	
	//device name
	public String get_device_name()
	{
		return device_name;	
	}
	
	public void set_device_name(String name)
	{
		device_name = name;	
	}
	
	//device id
	public byte get_device_id()
	{
		return device_id ;
	}
	
	public void set_device_id(byte id)
	{
		device_id = id ;
	}
	
	// device status
	public boolean get_device_status(byte id)
	{	
		if((all_device_status & (1 << id)) == 0)
			device_status =  false;
		else
			device_status = true;
		
		return device_status ;
	}
	
	public void set_device_status(byte id ,boolean on)
	{
		if(on)
			all_device_status |=  1 << id ;
		else
			all_device_status &=  ~(1 << id) ;
	}
	
	// all device status
	public byte get_all_device_status()
	{
		return all_device_status;
	}
	
	public void set_all_device_status(byte status)
	{
		all_device_status = status ;
	}
	
	

}
