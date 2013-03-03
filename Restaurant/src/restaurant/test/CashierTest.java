package restaurant.test;

import java.util.ArrayList;

import junit.framework.TestCase;

import org.junit.Test;

import restaurant.CashierAgent;
import restaurant.FoodData;
import restaurant.CashierAgent.CustomerState;
import restaurant.test.mock.MockCustomer;
import restaurant.test.mock.MockMarket;
import restaurant.test.mock.MockWaiter;

public class CashierTest extends TestCase{

	
	CashierAgent cashier;
	MockCustomer customer;
	MockWaiter waiter;
	MockMarket market;
	protected void setUp() {
		cashier = new CashierAgent("Cashier");
		customer = new MockCustomer("Customer");
		waiter = new MockWaiter("Waiter");
		market = new MockMarket("Market");
	}
	
	@Test
	public void testMarketOrder() {
		ArrayList<FoodData> food = new ArrayList<FoodData>();
		food.add(new FoodData("Steak",1));
		assertTrue(cashier.cookOrders.isEmpty());
		int funds = cashier.funds;
		cashier.msgBuyMeFood(food, 50, market);
		cashier.pickAndExecuteAnAction();
		assertTrue(cashier.funds == funds-50);
		assertTrue(market.log.getLastLoggedEvent().getMessage().equalsIgnoreCase("Received message msgTakeMyMoney from Cashier with $50.0"));
		
	}
	
	@Test
	public void testNormalCustomerPay() {
		assertTrue(cashier.customers.isEmpty());
		cashier.msgCustomerDone(waiter, customer, "Steak");
		int funds = cashier.funds;
		assertFalse(cashier.customers.isEmpty());
		assertTrue(cashier.customers.get(0).state == CustomerState.paying);
		cashier.pickAndExecuteAnAction();
		assertTrue(cashier.customers.get(0).state == CustomerState.paid);
		assertTrue(waiter.log.getLastLoggedEvent().getMessage().equalsIgnoreCase("Received message msgHereIsBill from Cashier for Customer " + customer.getName() + " of price 15.99"));
		cashier.msgTakeMyMoney(customer, 20);
		assertTrue(cashier.customers.get(0).state == CustomerState.awaitingChange);
		cashier.pickAndExecuteAnAction();
		assertTrue(customer.log.getLastLoggedEvent().getMessage().equalsIgnoreCase("Received message msgTakeYourChange from Cashier with $4.01 in change"));
		assertTrue(cashier.customers.isEmpty());
		
	}
	
	@Test
	public void testKidneyCustomerPay() {
		assertTrue(cashier.customers.isEmpty());
		cashier.msgCustomerDone(waiter, customer, "Steak");
		int funds = cashier.funds;
		assertFalse(cashier.customers.isEmpty());
		assertTrue(cashier.customers.get(0).state == CustomerState.paying);
		cashier.pickAndExecuteAnAction();
		assertTrue(cashier.customers.get(0).state == CustomerState.paid);
		assertTrue(waiter.log.getLastLoggedEvent().getMessage().equalsIgnoreCase("Received message msgHereIsBill from Cashier for Customer " + customer.getName() + " of price 15.99"));
		cashier.msgICantPay(customer);
		assertTrue(cashier.customers.get(0).state == CustomerState.poor);
		cashier.pickAndExecuteAnAction();
		assertTrue(customer.log.getLastLoggedEvent().getMessage().equalsIgnoreCase("Received message removeKidney from cashier"));
		assertTrue(cashier.customers.isEmpty());
		
	}

}
