package interfaces;


public interface Customer {

	/** Cashier sends this after the customer has paid
	 * @param change The change the customer is due. Leaves it all as tip
	 */
	public abstract void msgTakeYourChange(double change);
	public abstract void removeKidney();
}
