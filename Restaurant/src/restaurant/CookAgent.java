package restaurant;

import agent.Agent;
import java.util.Timer;
import java.util.TimerTask;
import java.util.*;
import restaurant.layoutGUI.*;
import java.awt.Color;


/** Cook agent for restaurant.
 *  Keeps a list of orders for waiters
 *  and simulates cooking them.
 *  Interacts with waiters only.
 */
public class CookAgent extends Agent {

	//List of all the orders
	private List<Order> orders = new ArrayList<Order>();
	private Map<String,FoodData> inventory = new HashMap<String,FoodData>();
	public enum Status {pending, cooking, done}; // order status
	private boolean needsRestock = false;
	private boolean isReordering = false; //To ensure orders are requested one at a time. If I need one more reordering boolean, I'll change it to an enum, but for now, I believe this is sufficient

	Set<MyMarket> markets = new HashSet<MyMarket>();
	CashierAgent cashier;

	//Name of the cook
	private String name;

	//Timer for simulation
	Timer timer = new Timer();
	Restaurant restaurant; //Gui layout

	/** Constructor for CookAgent class
	 * @param name name of the cook
	 */
	public CookAgent(String name, Restaurant restaurant) {
		super();

		this.name = name;
		this.restaurant = restaurant;
		//Create the restaurant's inventory.
		inventory.put("Steak",new FoodData("Steak", 5));
		inventory.put("Chicken",new FoodData("Chicken", 4));
		inventory.put("Pizza",new FoodData("Pizza", 3));
		inventory.put("Salad",new FoodData("Salad", 2));
	}

	enum MarketStatus {available, ordering, paying, paid, deadtome};
	/** Private class to store market information.
	 * Contains the market, status, and order info
	 */
	public class MyMarket {
		public MarketAgent market;
		public MarketStatus status;
		public List<FoodData> currentOrder;
		public double currentOrderCost;

		public MyMarket(MarketAgent mark) {
			market = mark;
			status = MarketStatus.available;
			currentOrder = new ArrayList<FoodData>();
			currentOrderCost = 0;
		}

	}
	/** Private class to store order information.
	 *  Contains the waiter, table number, food item,
	 *  cooktime and status.
	 */
	private class Order {
		public WaiterAgent waiter;
		public int tableNum;
		public String choice;
		public Status status;
		public Food food; //a gui variable

		/** Constructor for Order class 
		 * @param waiter waiter that this order belongs to
		 * @param tableNum identification number for the table
		 * @param choice type of food to be cooked 
		 */
		public Order(WaiterAgent waiter, int tableNum, String choice){
			this.waiter = waiter;
			this.choice = choice;
			this.tableNum = tableNum;
			this.status = Status.pending;
		}

		/** Represents the object as a string */
		public String toString(){
			return choice + " for " + waiter ;
		}
	}





	// *** MESSAGES ***

	/** Message from a waiter giving the cook a new order.
	 * @param waiter waiter that the order belongs to
	 * @param tableNum identification number for the table
	 * @param choice type of food to be cooked
	 */
	public void msgHereIsAnOrder(WaiterAgent waiter, int tableNum, String choice){
		print("received order from " + waiter);
		orders.add(new Order(waiter, tableNum, choice));
		stateChanged();
	}

	/** Cook calls this when he is out of food.
	 */
	public void msgIAmOutOfInventory() {
		if (isReordering)
			return;
		print("I am out of stock!");
		needsRestock = true;
		stateChanged();
	}

	/** Message for when a Market runs out of food
	 * @param market the market the message belongs to
	 */
	public void msgIHaveNoFood(MarketAgent market) {
		print(market + " is out of food!");
		for (MyMarket m:markets) {
			if (m.market.equals(market)) {
				m.status = MarketStatus.deadtome;
				stateChanged();
				return;
			}
		}
	}

	/** Message to denote the price of a market order
	 * @param market the market the message belongs to
	 * @param price the price of the order
	 * @param curorder the order
	 */
	public void msgHereIsPrice(MarketAgent market, double price, List<FoodData> curorder) {
		print(market + " wants my money");
		for (MyMarket m:markets) {
			if (m.market.equals(market)) {
				m.currentOrderCost = price;
				m.currentOrder = curorder;
				m.status = MarketStatus.ordering;
				stateChanged();
				return;
			}
		}
	}

	/** Message to receive food from a market
	 * @param market the market the food belongs to
	 * @param curOrder the food being sent
	 */
	public void msgTakeMyFood(MarketAgent market, List<FoodData> curOrder) {
		print(market + " is giving me food");
		isReordering = false;
		for (MyMarket m:markets) {
			if (m.market.equals(market)) {
				m.currentOrder = curOrder;
				m.status = MarketStatus.paid;
				stateChanged();
				return;
			}
		}
	}


	/** Scheduler.  Determine what action is called for, and do it. */
	protected boolean pickAndExecuteAnAction() {

		if (needsRestock) {
			DoOrderFood();
			return true;
		}

		//If there exists an order o whose status is done, place o.
		for(Order o:orders){
			if(o.status == Status.done){
				placeOrder(o);
				return true;
			}
		}
		//If there exists an order o whose status is pending, cook o.
		for(Order o:orders){
			if(o.status == Status.pending){
				cookOrder(o);
				return true;
			}
		}

		/*for (MyMarket m:markets) { //If you want to do something with the dead market
		if (m.status == MarketStatus.deadtome) {
			m.msgF***YOU();
			return true;
		}
	}*/

		for (MyMarket m:markets) {
			if (m.status == MarketStatus.ordering) {
				DoPurchaseFood(m);
				return true;
			}
		}

		for (MyMarket m:markets) {
			if (m.status == MarketStatus.paid) {
				DoStockFood(m);
				return true;
			}
		}

		//we have tried all our rules (in this case only one) and found
		//nothing to do. So return false to main loop of abstract agent
		//and wait.
		return false;
	}


	// *** ACTIONS ***

	/** Starts a timer for the order that needs to be cooked. 
	 * @param order
	 */
	private void cookOrder(Order order){
		if (inventory.get(order.choice).amount <= 0) {
			order.waiter.msgOutOfStock(order.tableNum);
			orders.remove(order);
			return;
		}
		DoCooking(order);
		order.status = Status.cooking;
		inventory.get(order.choice).amount -= 1;
		if (inventory.get(order.choice).amount <= 2) {
			this.msgIAmOutOfInventory();
		}
	}

	private void placeOrder(Order order){
		DoPlacement(order);
		order.waiter.msgOrderIsReady(order.tableNum, order.food);
		orders.remove(order);
	}

	/** Orders food from an applicable market
	 * Does not check if the order is empty because it is assumed that there will be something to order if this is called
	 */
	private void DoOrderFood() {
		print("ordering food");
		needsRestock = false;
		isReordering = true;
		List<FoodData> restock = new ArrayList<FoodData>();
		for (String key:inventory.keySet()) {
			FoodData f = inventory.get(key);
			if (f.amount == 10)
				continue;
			FoodData temp = new FoodData(f.type, f.cookTime);
			temp.amount = 10 - f.amount;
			restock.add(temp);
		}

		for (MyMarket m:markets) {
			if (m.status != MarketStatus.deadtome) {
				m.market.msgINeedFood(this, restock);
				return;
			}
		}
	}

	private void DoPurchaseFood(MyMarket m) {
		print("buying food");
		cashier.msgBuyMeFood(m.currentOrder, m.currentOrderCost, m.market);
		m.status = MarketStatus.paying;
	}

	private void DoStockFood(MyMarket m) {
		print("stocking food");
		for (FoodData f:m.currentOrder)
			inventory.get(f.type).amount += f.amount;
		m.status = MarketStatus.available;
	}


	// *** EXTRA -- all the simulation routines***

	/** Returns the name of the cook */
	public String getName(){
		return name;
	}

	private void DoCooking(final Order order){
		print("Cooking:" + order + " for table:" + (order.tableNum+1));
		//put it on the grill. gui stuff
		order.food = new Food(order.choice.substring(0,2),new Color(0,255,255), restaurant);
		order.food.cookFood();

		timer.schedule(new TimerTask(){
			public void run(){//this routine is like a message reception    
				order.status = Status.done;
				stateChanged();
			}
		}, (int)(inventory.get(order.choice).cookTime*1000));
	}
	public void DoPlacement(Order order){
		print("Order finished: " + order + " for table:" + (order.tableNum+1));
		order.food.placeOnCounter();
	}
	/** Sets the cashier*/
	public void setCashier(CashierAgent cashier) {
		this.cashier = cashier;
	}

	/** A hack to add markets*/
	public void addMarket(MarketAgent market) {
		markets.add(new MyMarket(market));
	}
}



