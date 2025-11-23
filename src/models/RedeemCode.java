package models;

public class RedeemCode {
	private String codeString;
	private double goldValue;
	private boolean isUsed;

	// RedeemCode Constructor used to define the code details
	public RedeemCode(String codeString, double goldValue) {
		this.codeString = codeString;
		this.goldValue = goldValue;
		this.isUsed = false;
	}

	// Getters
	public String getCodeString() {
		return codeString;
	}

	public double getGoldValue() {
		return goldValue;
	}

	public boolean isUsed() {
		return isUsed;
	}

	// Setter
	public void setUsed(boolean isUsed) {
		this.isUsed = isUsed;
	}

	@Override
	// This method is used to display the details of the code
	public String toString() {
		return "Value: " + goldValue + "G\tCode: [" + codeString + "] | Used: " + isUsed;
	}
}