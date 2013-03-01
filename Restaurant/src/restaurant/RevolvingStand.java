package restaurant;


import java.util.Vector;

class RevolvingOrder {
	public String name;
	public WaiterAgent waiter;
	public int tableNum;
	
	public RevolvingOrder(String n, WaiterAgent w, int table) {
		name = n;
		waiter = w;
		tableNum = table;
	}
}

public class RevolvingStand extends Object {
  private final int N = 5;
  private int count = 0;
  private Vector<RevolvingOrder> theData;
  
  synchronized public void insert(RevolvingOrder data) {
      while (count == N) {
          try{ 
              System.out.println("\tFull, waiting");
              wait(5000);                         // Full, wait to add
          } catch (InterruptedException ex) {};
      }
          
      insert_item(data);
      count++;
      if(count == 1) {
          System.out.println("\tNot Empty, notify");
          notify();                               // Not empty, notify a 
                                                  // waiting consumer
      }
  }
  
  synchronized public RevolvingOrder remove() {
      RevolvingOrder data;
      while(count == 0)
          try{ 
              System.out.println("\tEmpty, waiting");
              wait(5000);                         // Empty, wait to consume
          } catch (InterruptedException ex) {};

      data = remove_item();
      count--;
      if(count == N-1){ 
          System.out.println("\tNot full, notify");
          notify();                               // Not full, notify a 
                                                  // waiting producer
      }
      return data;
  }
  
  private void insert_item(RevolvingOrder data){
      theData.addElement(data);
      System.out.println("Added order to revolving stand: " + data.name);
  }
  
  private RevolvingOrder remove_item(){
      RevolvingOrder data = theData.firstElement();
      theData.removeElementAt(0);
      return data;
  }
  
  public RevolvingStand(){
      theData = new Vector<RevolvingOrder>();
  }
}

