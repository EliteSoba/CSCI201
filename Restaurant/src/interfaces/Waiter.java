package interfaces;

import restaurant.CustomerAgent;

public interface Waiter {

	/** Waiter receives this from the Cashier with the bill for the customer
	 * @param customer the customer with the bill
	 * @param bill the bill
	 */
	public abstract void msgHereIsBill(Customer customer, double bill);

	/** Cashier sends this to alert the waiter that the customer left without paying money
	 * @param customer The customer that left
	 */
	public abstract void msgCustomerPaidWithBody(Customer customer);

}
