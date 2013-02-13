package restaurant;

	/** Public class to store information about food.
	 *  Contains the food type, its cooking time, and amount in inventory
	 *  public to share with cashier and market when passing it back and forth.
	 */
	public class FoodData {
		String type; //kind of food
		double cookTime;
		public int amount;
		// other things ...

		public FoodData(String type, double cookTime){
			this.type = type;
			this.cookTime = cookTime;
			amount = 3;
		}
	}

