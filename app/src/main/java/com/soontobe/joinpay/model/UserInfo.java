package com.soontobe.joinpay.model;

/**
 * User information model for information exchange between RadarView and
 * BigBubble
 * 
 */
public class UserInfo {
	private int userId;
	private String userName;
	private String publicNote;
	private String personalNote;
	private int amountOfMoney;
	private boolean isMyself; // Is this user just myself
	private boolean isContact; // Is this user a contact of mine
	private boolean isLocked; // Lock status of this user
	private boolean isSelecetd; // Is this user selected

	public UserInfo() {
		isContact = false;
		isMyself = false;
		amountOfMoney = 0;
		userName = "";
		publicNote = "";
		personalNote = "";
	}

	public boolean isMyself() {
		return isMyself;
	}

	public void setMyself(boolean isMyself) {
		this.isMyself = isMyself;
	}

	public boolean isSelecetd() {
		return isSelecetd;
	}

	public void setSelecetd(boolean isSelecetd) {
		this.isSelecetd = isSelecetd;
	}

	public boolean isContact() {
		return isContact;
	}

	public void setContactState(boolean isContact) {
		this.isContact = isContact;
	}

	public String getPublicNote() {
		return publicNote;
	}

	public void setPublicNote(String publicNote) {
		this.publicNote = publicNote;
	}

	public String getPersonalNote() {
		return personalNote;
	}

	public void setPersonalNote(String personalNote) {
		this.personalNote = personalNote;
	}

	public boolean isLocked() {
		return isLocked;
	}

	public void setLocked(boolean isLocked) {
		this.isLocked = isLocked;
	}

	public int getUserId() {
		return userId;
	}

	public void setUserId(int userId) {
		this.userId = userId;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public int getAmountOfMoney() {
		return amountOfMoney;
	}

	public void setAmountOfMoney(int amountOfMoney) {
		this.amountOfMoney = amountOfMoney;
	}

	@Override
	public String toString() {
		String str = "UserInfo:";
		str += "Name = " + userName;
		return str;
	}

}