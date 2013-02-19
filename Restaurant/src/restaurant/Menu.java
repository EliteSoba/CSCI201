package restaurant;

import java.util.ArrayList;
import java.util.HashMap;


public class Menu {

    public String choices[] = new String[]
	{ "Steak"  ,
	  "Chicken", 
	  "Salad"  , 
	  "Pizza"  };
    HashMap<String, Double> prices;
    
    public Menu() {
    	prices = new HashMap<String, Double>();
    	prices.put("Steak", 15.99);
    	prices.put("Chicken", 10.99);
    	prices.put("Salad", 5.99);
    	prices.put("Pizza", 8.99);
    }
    
    public void addItem(String item) {
    	String temp[] = new String[choices.length+1];
    	for (int i = 0; i < choices.length; i++) {
    		temp[i] = choices[i];
    		if (item.equals(choices[i]))
    			return;
    	}
    	temp[choices.length] = item;
    	choices = temp;
    }
    
    public void removeItem(String item) {
    	ArrayList<String> temp = new ArrayList<String>();
    	for (int i = 0; i < choices.length; i++) {
    		if (!choices[i].endsWith(item))
    			temp.add(choices[i]);
    	}

    	String temp2[] = new String[temp.size()];
    	for (int i = 0; i < temp.size(); i++) {
    		temp2[i] = temp.get(i);
    	}
    	choices = temp2;
    }
    
    public double getPrice(String item) {
    	if (prices.containsKey(item))
    		return prices.get(item);
    	return 0;
    }

}
    
