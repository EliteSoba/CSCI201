package restaurant.test.mock;

import interfaces.Customer;

public class MockCustomer extends MockAgent implements Customer {

	public MockCustomer(String name) {
		super(name);
	}

	@Override
	public void msgTakeYourChange(double change) {
		log.add(new LoggedEvent("Received message msgTakeYourChange from Cashier with $" + change + " in change"));

	}

	@Override
	public void removeKidney() {
		log.add(new LoggedEvent("Received message removeKidney from cashier"));

	}

}
