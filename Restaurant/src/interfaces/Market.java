package interfaces;

import restaurant.CashierAgent;

public interface Market {



	/** Cashier sends money to the market. Market just eats money.
	 * We assume that the price is sufficient for the order and that they order the same thing.
	 * No requirements to assume otherwise, and if they don't, it gets too complex
	 * In my design doc, I allow for a change in order, but upon implementation, I realize this is too convoluted
	 * and I can't expect the cashier or cook to know what they want to change. I'll just let them be in debt if they need to.
	 * 
	 */
	public abstract void msgTakeMyMoney(CashierAgent cashier, double price);
}
