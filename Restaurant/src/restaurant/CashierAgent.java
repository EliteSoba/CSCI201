package restaurant;

import agent.Agent;
import java.util.*;


/** Host agent for restaurant. //TODO: UPDATE THIS DESCRIPTION
 *  Keeps a list of all the waiters and tables.
 *  Assigns new customers to waiters for seating and 
 *  keeps a list of waiting customers.
 *  Interacts with customers and waiters.
 */
public class CashierAgent extends Agent {

	Menu menu = new Menu();
	/** Private class for storing customer data*/
	private class MyCustomer {
		public WaiterAgent waiter; //The waiter in charge of the customer
		public CustomerAgent customer;
		public String choice;
		public CustomerState state;
		double money;

		public MyCustomer(WaiterAgent w, CustomerAgent c, String ch) {
			waiter = w;
			customer = c;
			choice = ch;
			money = 0;
			state = CustomerState.paying;
		}
	}

	//List of customers
	List<MyCustomer> customers;
	enum CustomerState {paying, paid, awaitingChange, poor};

	//number of kidneys taken from customers
	int kidneys;
	//Name of the Cashier
	private String name;
	//Initial funds of restaurant
	int funds;

	/** Private class for cook orders*/
	private class Order {
		public List<FoodData> food;
		public MarketAgent market;
		public double cost;

		public Order(List<FoodData> f, MarketAgent m, double c) {
			food = f;
			market = m;
			cost = c;
		}
	}

	List<Order> cookOrders;

	/** Constructor for CashierAgent class 
	 * @param name name of the market */
	public CashierAgent(String name) {
		super();
		this.name = name;
		funds = 100000;
		customers = Collections.synchronizedList(new ArrayList<MyCustomer>());
		kidneys = 0;
		cookOrders = Collections.synchronizedList(new ArrayList<Order>());
	}

	// *** MESSAGES ***

	public void msgCustomerDone(WaiterAgent waiter, CustomerAgent customer, String choice) {
		print("I acknowledge that " + customer + " has finished eating");
		customers.add(new MyCustomer(waiter, customer, choice));
		stateChanged();
	}

	public void msgTakeMyMoney(CustomerAgent customer, double money) {
		print(customer + " has paid!");
		synchronized (customers) {
			for (MyCustomer c:customers) {
				if (c.customer.equals(customer)) {
					c.state = CustomerState.awaitingChange;
					c.money = money;
				}
			}
		}
		stateChanged();
	}

	public void msgICantPay(CustomerAgent customer) {
		print(customer + " can't pay!");
		synchronized (customers) {
			for (MyCustomer c:customers) {
				if (c.customer.equals(customer)) {
					c.state = CustomerState.poor;
				}
			}
		}
		stateChanged();
	}

	public void msgBuyMeFood(List<FoodData> food, double cost, MarketAgent market) {
		print("cook wants food!");
		cookOrders.add(new Order(food, market, cost));
		stateChanged();
	}

	/** Scheduler.  Determine what action is called for, and do it. */
	protected boolean pickAndExecuteAnAction() {

		synchronized (customers) {
			for (MyCustomer c:customers) {
				if (c.state == CustomerState.paying) {
					DoCalculateBill(c);
					return true;
				}
			}

			for (MyCustomer c:customers) {
				if (c.state == CustomerState.awaitingChange) {
					DoCalculateChange(c);
					return true;
				}
			}

			for (MyCustomer c:customers) {
				if (c.state == CustomerState.poor) {
					DoTakeKidney(c);
					return true;
				}
			}
		}

		synchronized (cookOrders) {
			for (Order o:cookOrders) {
				DoProcessOrder(o);
				return true;
			}
		}

		//we have tried all our rules and found
		//nothing to do. So return false to main loop of abstract agent
		//and wait.
		return false;
	}

	// *** ACTIONS ***

	private void DoCalculateBill(MyCustomer customer) {
		print("Calculating bill for " + customer.customer);
		double bill = DoCalculatePrice(customer.choice);
		customer.waiter.msgHereIsBill(customer.customer, bill);
		customer.state = CustomerState.paid;
		stateChanged();
	}

	private void DoCalculateChange(MyCustomer customer) {
		print("Calculating change for " + customer.customer);
		double change = customer.money - DoCalculatePrice(customer.choice);
		customer.customer.msgTakeYourChange(change);
		customers.remove(customer);
		stateChanged();
	}

	private void DoTakeKidney(MyCustomer customer) {
		print("Removing kidney from " + customer.customer);
		customer.customer.removeKidney();
		kidneys++;
		customer.waiter.msgCustomerPaidWithBody(customer.customer);
		customers.remove(customer);
		stateChanged();
	}

	private void DoProcessOrder(Order order) {
		print("Ordering food from market");
		if (order.cost > funds) {//If too expensive, orders nothing. If desired, an algorithm to remove certain foods from the order could be used
			order.market.msgTakeMyMoney(this, 0);
			cookOrders.remove(order);
		}
		else {
			order.market.msgTakeMyMoney(this, order.cost/*, order.food*/); //Food has been removed from the order because passing lists as messages makes me sad
			cookOrders.remove(order);
		}
		stateChanged();
	}

	// *** EXTRA ***

	/** Returns the name of the market 
	 * @return name of market */
	public String getName(){
		return name;
	}    

	public String toString() {
		return name;
	}

	private double DoCalculatePrice(String choice) {
		return menu.getPrice(choice);
	}

}
