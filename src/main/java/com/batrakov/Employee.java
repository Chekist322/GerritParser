package com.batrakov;

public class Employee {
	private String mName;
	private String mTeam;
	
	public Employee() {
		mName = null;
		mTeam = null;
	}
	
	public Employee(String aName, String aTeam) {
		mName = aName;
		mTeam = aTeam;
	}
	
	public void setName(String aName) {
		mName = aName;
	}
	
	public void setTeam(String aTeam) {
		mTeam = aTeam;
	}
	
	public String getName() {
		return mName;
	}
	
	public String getTeam() {
		return mTeam;
	}
}
