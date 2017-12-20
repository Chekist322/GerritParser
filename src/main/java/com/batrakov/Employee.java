package com.batrakov;

public class Employee {
	private String mName;
	private String mTeam;

	public Employee() {
		mName = null;
		mTeam = null;
	}
	
	public void setName(String aName) {
		mName = aName;

	}
	
	public String getName() {
		return mName;
	}

	public void setTeam(String aTeam) {
		mTeam = aTeam;
	}
}
