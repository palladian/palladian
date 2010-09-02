package tud.iir.helper;

public class ListSimilarity {
	
	private double shiftSimilartiy = -1.0;
	private double squaredShiftSimilartiy = -1.0;
	private double rmse = -1.0;

	public void setShiftSimilartiy(double shiftSimilartiy) {
		this.shiftSimilartiy = shiftSimilartiy;
	}

	public double getShiftSimilartiy() {
		return shiftSimilartiy;
	}
	
	public void setSquaredShiftSimilartiy(double squaredShiftSimilartiy) {
		this.squaredShiftSimilartiy = squaredShiftSimilartiy;
	}

	public double getSquaredShiftSimilartiy() {
		return squaredShiftSimilartiy;
	}	
	
	public double getRmse() {
		return rmse;
	}

	public void setRmse(double rmse) {
		this.rmse = rmse;
	}
	
}