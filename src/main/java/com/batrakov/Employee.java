package com.batrakov;

public class Employee {
	private String mName;
	
	Employee() {
		mName = null;
	}
	
	public void setName(String aName) {
		mName = aName;
	}
	
	public String getName() {
		return mName;
	}
}
