package restaurant;

import agent.Agent;
import java.util.*;
import restaurant.CookAgent.FoodData;


/** Host agent for restaurant. //TODO: UPDATE THIS DESCRIPTION
 *  Keeps a list of all the waiters and tables.
 *  Assigns new customers to waiters for seating and 
 *  keeps a list of waiting customers.
 *  Interacts with customers and waiters.
 */
public class CashierAgent extends Agent {

	/** Private class for storing customer data*/
	private class MyCustomer {
		public WaiterAgent waiter; //The waiter in charge of the customer
		public CustomerAgent customer;
		public String choice;
		public CustomerState state = CustomerState.paying;
		double money;
		
		public MyCustomer(WaiterAgent w, CustomerAgent c, String ch) {
			waiter = w;
			customer = c;
			choice = ch;
			money = 0;
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
    	public int cost;
    	
    	public Order(List<FoodData> f, MarketAgent m, int c) {
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
	kidneys = 0;
	cookOrders = new ArrayList<Order>();
    }

    // *** MESSAGES ***
    
    public void msgCustomerDone(WaiterAgent waiter, CustomerAgent customer, String choice) {
    	customers.add(new MyCustomer(waiter, customer, choice));
    }
    
    public void msgTakeMyMoney(CustomerAgent customer, int money) {
    	customers.get(customers.indexOf(customer)).state = CustomerState.awaitingChange;
    	customers.get(customers.indexOf(customer)).money = money;
    }
    
    public void msgICantPay(CustomerAgent customer) {
    	customers.get(customers.indexOf(customer)).state = CustomerState.poor;
    }
    
    public void msgBuyMeFood(List<FoodData> food, MarketAgent market, int cost) {
    	cookOrders.add(new Order(food, market, cost));
    }

    /** Scheduler.  Determine what action is called for, and do it. */
    protected boolean pickAndExecuteAnAction() {
	
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
			
	for (Order o:cookOrders) {
			DoProcessOrder(o);
			return true;
		}

	//we have tried all our rules and found
	//nothing to do. So return false to main loop of abstract agent
	//and wait.
	return false;
    }
    
    // *** ACTIONS ***
    
    private void DoCalculateBill(MyCustomer customer) {
    	double bill = DoCalculatePrice(customer.choice);
		customer.waiter.msgHereIsBill(customer.customer, bill);
		customer.state = CustomerState.paid;
    }
    
    private void DoCalculateChange(MyCustomer customer) {
		int change = customer.money - DoCalculatePrice(customer.choice);
		customer.msgTakeYourChange(change);
		customers.remove(customer);
	}
	
	private void DoTakeKidney(MyCustomer customer) {
		customer.removeKidney();
		kidneys++;
		customer.waiter.msgCustomerPaidWithBody(customer.customer);
		customers.remove(customer);
	}

	private void DoProcessOrder(Order order) {
		if (order.cost > funds) {//If too expensive, orders nothing. If desired, an algorithm to remove certain foods from the order could be used
			order.market.msgTakeMyMoney(this, 0);
			cookOrders.remove(order);
		}
		else {
			order.market.msgTakeMyMoney(this, order.cost, order.food);
			cookOrders.remove(order);
		}
	}

    // *** EXTRA ***

    /** Returns the name of the market 
     * @return name of market */
    public String getName(){
        return name;
    }    
    
    //TODO: add an actual price calculation
    private double DoCalculatePrice(String choice) {
		double price = 50;
	return price;
}

}
