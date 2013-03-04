package restaurant.test.mock;

import restaurant.CashierAgent;
import interfaces.Market;

public class MockMarket extends MockAgent implements Market {

	public MockMarket(String name) {
		super(name);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void msgTakeMyMoney(CashierAgent cashier, double price) {
		log.add(new LoggedEvent("Received message msgTakeMyMoney from Cashier with $" + price));

	}

}
