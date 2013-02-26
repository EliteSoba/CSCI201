package restaurant;

import agent.Agent;
import java.util.*;

import restaurant.CookAgent.Status;


/** Host agent for restaurant. //TODO: UPDATE THIS DESCRIPTION
 *  Keeps a list of all the waiters and tables.
 *  Assigns new customers to waiters for seating and 
 *  keeps a list of waiting customers.
 *  Interacts with customers and waiters.
 */
public class MarketAgent extends Agent {

	/** Private class to hold cook information and state */
	private class MyCook {
		public CookAgent cook;
		public CookStatus status;
		public List<FoodData> currentOrder;
		public int currentCost;

		public MyCook(CookAgent cook) {
			this.cook = cook;
			status = CookStatus.nothing;
			currentOrder = new ArrayList<FoodData>();
			currentCost = 0;
		}
	}
	public enum CookStatus {nothing, ordering, paying, paid};
	/** You may be wondering why I have a list of cooks here instead of just one cook, because it seems silly when I only use one cook ever.
	 * WELL THIS IS BECAUSE I LOST 5 POINTS FOR NOT HAVING THE COOKS AS A LIST FOR MY 4.1 DELIVERY.
	 * I'm also considering things like making the name a list of names to avoid any clobbering issues.
	 * Maybe I'll put in a list of inventory maps to make sure that my inventory never gets clobbered either.
	 * And to make sure that everything goes well, I might as well make a List of each List so that my Lists never have the opportunity to get clobbered.*/
	private List<MyCook> cooks;


	Timer timer = new Timer();
	Map<String, FoodData> inventory;

	//Name of the market
	private String name;

	/** Constructor for MarketAgent class 
	 * @param name name of the market */
	public MarketAgent(String name) {
		super();
		this.name = name;
		cooks = new ArrayList<MyCook>();
		inventory = new HashMap<String, FoodData>();
		String[] temp = new Menu().choices;
		FoodData tempo;
		for (int i = 0; i < temp.length; i++) {
			tempo = new FoodData(temp[i], 0);
			tempo.amount = 100;
			inventory.put(temp[i], tempo);

		}

	}

	// *** MESSAGES ***

	/** Cook sends this message to request food. Assumes choices and amounts are of same length.
	 * @param cook the cook that wants food
	 * @param choices the list of names of foods
	 * @param amounts the list of amounts of foods
	 */
	public void msgINeedFood(CookAgent cook, List<FoodData> currentOrder) {
		for (MyCook c:cooks) {
			if (c.cook.equals(cook)) {
				c.currentOrder = new ArrayList<FoodData>();
				print(cook + " needs food!");
				for (int i = 0; i < currentOrder.size(); i++) {
					c.currentOrder.add(currentOrder.get(i));
				}
				c.status = CookStatus.ordering;
				stateChanged();
				return;
			}
		}
	}

	/** Cashier sends money to the market. Market just eats money.
	 * We assume that the price is sufficient for the order and that they order the same thing.
	 * No requirements to assume otherwise, and if they don't, it gets too complex
	 * In my design doc, I allow for a change in order, but upon implementation, I realize this is too convoluted
	 * and I can't expect the cashier or cook to know what they want to change. I'll just let them be in debt if they need to.
	 * 
	 */
	public void msgTakeMyMoney(CashierAgent cashier, double price) {
		print(cashier + " paid for food!");
		cooks.get(0).status = CookStatus.paid;
		if (price == 0) //If they pay nothing, they get nothing. A small hack.
			cooks.get(0).currentOrder = new ArrayList<FoodData>();
		stateChanged();
	}

	/** Scheduler.  Determine what action is called for, and do it. */
	protected boolean pickAndExecuteAnAction() {

		if (cooks.get(0).status == CookStatus.ordering) {
			DoCalculateOrder();
			return true;
		}
		if (cooks.get(0).status == CookStatus.paid) {
			DoFulfilOrder();
			return true;
		}

		//we have tried all our rules and found
		//nothing to do. So return false to main loop of abstract agent
		//and wait.
		return false;
	}

	// *** ACTIONS ***

	/** Calculates the price of an order and returns what can be sold to the cook and at what price.
	 * As of present, there is nothing in the inventory of the market, so all is bad.
	 */
	private void DoCalculateOrder() {
		print("calculating cost of order");
		boolean outOfStock = true;
		for (FoodData o:cooks.get(0).currentOrder) {
			print("searching for " + o.type);
			if (inventory.get(o.type) == null) {
				print("couldn't find " + o.type);
				o.amount = 0;
			}
			else if (o.amount > inventory.get(o.type).amount) {
				print ("found item " + o.type);
				o.amount = inventory.get(o.type).amount;
				if (o.amount > 0)
					outOfStock = false;
			}
			else {
				print ("found item " + o.type);
				outOfStock = false;
			}
		}

		if (outOfStock) {//for all orders in cook.currentOrder, amount = 0
			cooks.get(0).cook.msgIHaveNoFood(this);
			cooks.get(0).status = CookStatus.nothing;
		}

		else {
			cooks.get(0).cook.msgHereIsPrice(this, DoCalculatePrice(cooks.get(0).currentOrder), cooks.get(0).currentOrder);
			cooks.get(0).status = CookStatus.paying;
		}
		stateChanged();
	}

	/** Gives the food to the cook
	 *
	 */
	private void DoFulfilOrder() {
		print("fulfilling order in 10000 ms");
		cooks.get(0).status = CookStatus.nothing;
		final MarketAgent temp = this;
		timer.schedule(new TimerTask(){
			public void run(){//this routine is like a message reception    
				cooks.get(0).cook.msgTakeMyFood(temp, cooks.get(0).currentOrder);
				stateChanged();
			}
		}, 10000);
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

	/** Returns the total price of an order
	 * This is an int in the design document, but now I decided to allow for decimal prices.
	 * @param currentOrder the order to calculate the price of
	 * @return the price of the order
	 */
	double DoCalculatePrice(List<FoodData> currentOrder) {
		double total = 0;
		Menu menu = new Menu();
		for (FoodData o:currentOrder)
			total += (menu.getPrice(o.type)/2)* o.amount;
		return total;
	}
	/** A GUI hack to remove all food from the market*/
	public void goOutOfStock() {
		print("Going out of stock!");
		for (String food: inventory.keySet())
			inventory.get(food).amount = 0;
	}

	public boolean isOutOfStock() {
		for (String food:inventory.keySet())
			if (inventory.get(food).amount != 0)
				return false;
		return true;
	}

	/** A hack*/
	public void setCook(CookAgent cook) {
		cooks.add(new MyCook(cook));
	}

}
