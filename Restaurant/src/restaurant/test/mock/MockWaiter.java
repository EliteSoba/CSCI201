package restaurant.test.mock;

import interfaces.Customer;
import interfaces.Waiter;

public class MockWaiter extends MockAgent implements Waiter {


	public MockWaiter(String name) {
		super(name);
	}

	@Override
	public void msgHereIsBill(Customer customer, double bill) {
		log.add(new LoggedEvent("Received message msgHereIsBill from Cashier for Customer " + customer.getName() + " of price " + bill));

	}

	@Override
	public void msgCustomerPaidWithBody(Customer customer) {
		log.add(new LoggedEvent("Received message msgCustomerPaidWithBody from Cashier for Customer " + customer.getName()));

	}

}
