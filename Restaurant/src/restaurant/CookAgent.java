package restaurant;

import agent.Agent;

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
	private List<Order> orders = Collections.synchronizedList(new ArrayList<Order>());
	private List<Order> changedOrders = Collections.synchronizedList(new ArrayList<Order>());
	private Map<String,FoodData> inventory = Collections.synchronizedMap(new HashMap<String,FoodData>());
	public enum Status {pending, preparing, prepared, cooking, done}; // order status
	private boolean needsRestock = false;
	private boolean isReordering = false; //To ensure orders are requested one at a time. If I need one more reordering boolean, I'll change it to an enum, but for now, I believe this is sufficient

	public RevolvingStand stand;
	private boolean reordering = true; //To determine if the cook can reorder from markets or not. A hack for nonnormative demonstrations
	Set<MyMarket> markets = Collections.synchronizedSet(new HashSet<MyMarket>());
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
		stand = null;
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
		if (!reordering)
			return;
		needsRestock = true;
		stateChanged();
	}

	/** Message for when a Market runs out of food
	 * @param market the market the message belongs to
	 */
	public void msgIHaveNoFood(MarketAgent market) {
		print(market + " is out of food!");
		synchronized (markets) {
			for (MyMarket m:markets) {
				if (m.market.equals(market)) {
					m.status = MarketStatus.deadtome;
					stateChanged();
					return;
				}
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
		synchronized (markets) {
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
	}

	/** Message to receive food from a market
	 * @param market the market the food belongs to
	 * @param curOrder the food being sent
	 */
	public void msgTakeMyFood(MarketAgent market, List<FoodData> curOrder) {
		print(market + " is giving me food");
		isReordering = false;
		synchronized (markets) {
			for (MyMarket m:markets) {
				if (m.market.equals(market)) {
					m.currentOrder = curOrder;
					m.status = MarketStatus.paid;
					stateChanged();
					return;
				}
			}
		}
	}

	public void msgOrderChanged(WaiterAgent waiter, int tableNum, String choice) {
		changedOrders.add(new Order(waiter, tableNum, choice));
		stateChanged();
	}


	/** Scheduler.  Determine what action is called for, and do it. */
	protected boolean pickAndExecuteAnAction() {


		if (!changedOrders.isEmpty()) {
			doCheckReorder(changedOrders.remove(0));
		}
		
		//A minor hack for the revolving stand so I can reuse methods
		if (!stand.isEmpty()) {
			RevolvingOrder temp = stand.remove();
			orders.add(0, new Order(temp.waiter, temp.tableNum, temp.name));
		}

		synchronized (markets) {
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
		}

		if (needsRestock && !markets.isEmpty()) {
			DoOrderFood();
			return true;
		}


		if (!isReordering && !needsRestock)
			doCheckStock(); //Nothing stops here, cook just makes a note to himself that he needs to reorder. Happens at all times the cook is awake, because this cook is responsible

		synchronized (orders) {
			//If there exists an order o whose status is done, place o.
			for(Order o:orders){
				if(o.status == Status.done){
					placeOrder(o);
					return true;
				}
			}
			//If there exists an order o whose status is pending, cook o.
			for(Order o:orders){
				if(o.status == Status.prepared){
					cookOrder(o);
					return true;
				}
			}

			for(Order o:orders) {
				if (o.status == Status.pending) {
					prepareOrder(o);
					return true;
				}
			}
		}

		/*for (MyMarket m:markets) { //If you want to do something with the dead market
		if (m.status == MarketStatus.deadtome) {
			m.msgF***YOU();
			return true;
		}
	}*/



		//we have tried all our rules (in this case only one) and found
		//nothing to do. So return false to main loop of abstract agent
		//and wake ourselves in 3 seconds to check for new orders.
		timer.schedule(new TimerTask(){
			public void run(){//this routine is like a message reception    
				stateChanged();
			}
		}, 3000);
		//print("Nothing to do, taking a 3 second nap");
		return false;
	}


	// *** ACTIONS ***

	/** Starts a timer for the order that needs to be cooked. 
	 * @param order
	 */
	private void cookOrder(Order order){
		if (inventory.get(order.choice).amount <= 2) {
			this.msgIAmOutOfInventory();
		}
		if (inventory.get(order.choice).amount <= 0) {
			order.waiter.msgOutOfStock(order.tableNum);
			orders.remove(order);
			return;
		}
		DoCooking(order);
		order.status = Status.cooking;
		inventory.get(order.choice).amount -= 1;
	}

	/** Takes some time to prepare the order so that the customer can change his mind
	 * @param order
	 */
	private void prepareOrder(final Order order) {
		order.status = Status.preparing;
		print("Preparing the order (Customer can change his mind at this time)");
		timer.schedule(new TimerTask(){
			public void run(){//this routine is like a message reception    
				order.status = Status.prepared;
				stateChanged();
			}
		}, 3000);
	}

	/** The cook is always checking his stock.
	 */
	private void doCheckStock() {
		for (String key:inventory.keySet()) {
			if (inventory.get(key).amount <= 2) {
				this.msgIAmOutOfInventory();
				return;
			}
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
		//If we get to this point, that means there are no markets.
		print("found no markets to order from");
		isReordering = false;
	}

	private void DoPurchaseFood(MyMarket m) {
		print("buying food");
		cashier.msgBuyMeFood(m.currentOrder, m.currentOrderCost, m.market);
		m.status = MarketStatus.paying;
	}

	private void DoStockFood(MyMarket m) {
		print("stocking food");
		needsRestock = false;
		for (FoodData f:m.currentOrder)
			inventory.get(f.type).amount += f.amount;
		m.status = MarketStatus.available;
	}

	private void doCheckReorder(Order order) {
		print("Checking if order is already cooking");
		for (Order o:orders) {
			if (o.tableNum == order.tableNum) {
				if (o.status == Status.pending || o.status == Status.preparing) {
					print("Order successfully changed");
					o.choice = order.choice;
					o.waiter.msgMindChangeApproved(o.tableNum);
				}
				else {
					print("Order is already cooking. Too bad!");
				}
				stateChanged();
				return;
			}
		}
		print("Order not found");
		order.waiter.msgIHaveNoIdeaWhatYoureTalkingAbout(order.tableNum);
		stateChanged();
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
		stateChanged();
	}

	/** Another hack, this time to remove inventory*/
	public void removeFood(String food) {
		if (!inventory.containsKey(food))
			return;
		inventory.get(food).amount = 0;
		print("Removing stock of " + food);
		stateChanged();
	}

	/** Another hack, this time to decide if the cook can reorder from the market to demonstrate nonnormatives*/
	public void setReordering(boolean reordering) {
		this.reordering = reordering;
	}
	public void setStand(RevolvingStand stand) {
		this.stand = stand;
	}
}



