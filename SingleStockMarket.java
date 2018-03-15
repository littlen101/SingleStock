/*
  Author: Nick Lewis
  Email: nlewis2016@my.fit.edu
  Course: cse2010
  Section: 14
  Description: A simple single stock text simulator that uses heap adaptable priority queues
  				- Seller queue is prioritized as : Lowest price => Lowest time
  				- Buyer queue is prioritized as : Highest price => Lowest time
  				
  				Once a buyer's buy range includes a seller's sell range then a transaction occurs
  					{ The minimum of the amounts to trade is taken from both and buyer/seller
  					  information is then updated
  					  
  					  Then the heaps are updated with the updated information 
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Scanner;


public class SingleStockMarket {
	/**
	 * input format : time buyer price quantity
	 * A Global enum container of indexes bases off of the input format
	 * 
	 * based of the element selected, creates an instance based off its args
	 * 
	 */
	public enum Positions {
		/* The index that represents the time in any input field */
		TIME(1),
		
		/* The index that represents the person of possible input fields */
		PERSON(2),
		
		/* The index that represents the price of possible input fields */
		PRICE(3),
		
		/* The index that represents the quantity of possible input fields */
		QUANTITY(4);
		
		// The index of the current instance
		private int in;
		
		/**
		 * Creates an instance of the Positions enum with the given index as its index
		 * @param i, the index given by one of the possible enums
		 */
		Positions(int i){
			in = i;
		}
		
		/**
		 * Gets the index of this current instance
		 * 
		 * @return Integer, the index of the current instance
		 */
		public int index() {
			return in;
		}
	}

	/**
	 * 
	 * The internal data for and order that is also used to sort orders
	 * holds the Time and Cost of an Order
	 * 
	 * @Implements Comparable, defines the natural ordering of keys in 
	 * 						   ascending order of cost then time
	 *
	 */
	static class Key implements Comparable<Key>{
		// the time aspect of a key; at what time an order was created/updated
		int time;
		
		// the cost aspect of a key; how much something costs
		double cost;
		
		/**
		 * The Key constructor
		 * @param t, the in-coming time argument that will be the time for this instance 
		 * @param c, the in-coming time argument that will be the cost for this instance
		 */
		public Key(int t, double c) {
			time = t;
			cost = c;
		}

		/**
		 * Method to give information on whether or not the current instance should be 
		 * 	< 0 : Before
		 *  = 0 : Same
		 *  > 0 : After
		 *  
		 *  the comparing object; if the cost of the two keys are the same then time is then
		 *  the decider; Premise : there are no duplicate times - ever
		 *  
		 *  @param other, the Key to be compared
		 *  @return Integer, based of the above table returns the respective result
		 */
		@Override
		public int compareTo(Key other) {
			if (other.cost == cost) {
				return time - other.time;
			}
			
			return (int) Math.ceil(cost - other.cost);
		}

		/**
		 * Standard : a key is equal to another key iff the times and cost are the same
		 * 
		 * @param obj, the foreign object to be casted and equated 
		 * @return Boolean, whether the standard is true for the
		 * 					current instance and the comparing object
		 */
		@Override
		public boolean equals(Object obj) {
			Key other = (Key) obj;
			return ((time == other.time) && (cost == other.cost));
		}
	}

	/**
	 * Represents an order that has the following information 
	 * 	time - The time an order was created or updated
	 * 	buyer/seller - The name of the buyer/seller of the order
	 * 	price - The amount that the stock should be bought/sold for
	 * 	quantity - The amount of stocks that should be bought/sold
	 * 
	 * Is the main data structure for the priority queue
	 * 
	 *
	 */
	static class Order {
		//The way Orders are compared to one another; holds the time and price data
		Key key;
		
		//The name of the person who made the order
		String name;
		
		//How much stock the person wants to buy/sell
		int quanity;

		/**
		 * Constructor to build the order takes in a Key, String, and Integer
		 * @param k, The key that has the time and cost of the order
		 * @param n, the name of the person that placed the order
		 * @param num, the amount of stock to be bought/sold
		 */
		public Order(Key k, String n, int num) {
			key = k;
			name = n;
			quanity = num;
		}
		
		/**
		 * Establishes the equivalence of one order to another
		 * An order is equivalent to another if all attributes are all equivalent to one another
		 */
		@Override
		public boolean equals(Object obj) {
			Order other = (Order) obj;
			return ((key.equals(other.key)) && (name.equalsIgnoreCase(other.name)) 
					&& (quanity == other.quanity));
		}

		/**
		 * Used for debugging, a String representation of an order displaying 
		 * all the values of its attributes
		 */
		@Override
		public String toString() {
			return String.format("Buyer = %s Time = %d Cost = %d Quan = %d", 
					name, key.time, key.cost, quanity);
		}
	}

	/**
	 * A comparator for the a buyer representation of an order
	 * Sorts the buyers in descending order
	 * 
	 * iff the costs of the keys are the same then the time is the decider
	 * time remains ascending 
	 */
	private static final class BuyerComparator implements  Comparator<Key>{
		@Override
		public int compare(Key or1, Key or2) {
			if(or1.cost == or2.cost) {
				if(or1.time < or2.time) {
					return -1;
				} else if (or1.time == or2.time) {
					return 0;
				} else {
					return 1;
				}
			}
			
			return (int) Math.ceil((or2.cost - or1.cost));
		}
		
	}

	/**
	 * An event handler for when the query is to add a new Buy/Sell order
	 * Takes in the respective entries list and heap and then creates a new order
	 * if one doesn't already exist 
	 * @param instr, the information given by the query for creating orders  
	 * @param entries, the ArrayList of all the current entries currently in the heap
	 * @param heap, the respective priority queue containing buy/sell nodes
	 * @return Boolean, whether it was able to add the item or not
	 */
	public static boolean handleNewOrder (String[] instr,
										ArrayList<Entry<Key, Order>> entries,
										HeapAdaptablePriorityQueue<Key,Order> heap) {
		//A Loop to go through all of the entries to try and find conflicts
		for(Entry<Key,Order> entry : entries) {
			if (instr[Positions.PERSON.index()].equalsIgnoreCase(entry.getValue().name))
				return false;
		}
		
		//The Key for the Order to be created
		Key ckey = new Key(Integer.parseInt(instr[Positions.TIME.index()]),
											Double.parseDouble(instr[Positions.PRICE.index()]));
		
		//The creation and insertion of a new Order into the queue and the entry into the list
		entries.add(heap.insert(ckey, new Order(ckey, instr[Positions.PERSON.index()],
										Integer.parseInt(instr[Positions.QUANTITY.index()]))));
		return true;
	}

	/**
	 * Changes the time, price, and quantity of an order with the given changes 
	 * @param instr, contains the information about the changes; what the new numbers are.
	 * @param entries, the respective array list of entries to search through in-order to change
	 * @param heap, the respective priority queue for either buy/sell node
	 * @return Boolean, whether or not this method was able to find and change a node
	 */
	public static boolean changeOrder (String[] instr,
									ArrayList<Entry<Key, Order>> entries,
									HeapAdaptablePriorityQueue<Key,Order> heap) {
		
		//Goes through all the entries to find the node listed in the instructions
		for (Entry<Key,Order> entry : entries) {
			if(entry.getValue().name.equalsIgnoreCase(instr[Positions.PERSON.index()])) {
				entry.getKey().time = Integer.parseInt(instr[Positions.TIME.index()]);
				entry.getKey().cost = Double.parseDouble(instr[Positions.PRICE.index()]);
				entry.getValue().quanity = Integer.parseInt(instr[Positions.QUANTITY.index()]);
				
				//A method that reconstructs the heap in linear time
				heap.heapify();
				return true;
			}
		}
		return false;
	}

	/**
	 * Locates and cancels an order by removing it from the priority queue and entries list
	 * @param instr, contains the information on what order to cancel
	 * @param entries, the list of all of the buy/sell entries created by the priority queue
	 * @param heap, the respective priority queue of buy/sell order nodes
	 * @return Boolean, whether or not it was able to find and cancel someone's order
	 */
	public static boolean cancelOrder (String[] instr,
										ArrayList<Entry<Key, Order>> entries,
										HeapAdaptablePriorityQueue<Key,Order> heap) {
		//State variables that control the return information and the queue and list 
		boolean status = false;
		Entry<Key, Order> toRemove = null;
		
		//Loop that goes through all the entries and tries to find a matching order name 
		for(Entry<Key, Order> e : entries) {
			if(e.getValue().name.equalsIgnoreCase(instr[Positions.PERSON.index()])) {
				toRemove = e;
				status = true;
			}
		}

		//If a matching order name was found it removes the entry from the queue and list
		if (status) {
			heap.remove(toRemove);
			entries.remove(toRemove);
		}

		return status;
	}

	/**
	 * Checks whether or not the next possible transaction is within limits already
	 * Limits are if buying something at a price that is greater or equal to the price someone 
	 * is selling at 
	 * @param sellHeap, the queue that contains the sell orders
	 * @param buyHeap, the queue that contains the buy orders
	 * @return Boolean, returns whether or not a transaction can happen
	 */
	public static boolean validTransaction (HeapAdaptablePriorityQueue<Key,Order> sellHeap,
										    HeapAdaptablePriorityQueue<Key,Order> buyHeap) {
		//A state variable that controls what information is returned 
		boolean status = false;
		
		//Checks to make sure that there is something in both heaps
		if(!(sellHeap.isEmpty()) && !(buyHeap.isEmpty())) {
			//Sets the comparing entries by the min of both the heaps
			Entry<Key, Order> seller = sellHeap.min();
			Entry<Key, Order> buyer = buyHeap.min();
			
			//The test to see whether or not the transaction is legal or not
			status = (buyer.getKey().cost >= seller.getKey().cost);
		}
		
		return status;
	}

	/**
	 * Executes a buy order which happens when the highest priority orders are within range of
	 * each other. Finds the price the exchange went for and the amount that was exchanged
	 * Standard = {Transaction Price, Transaction Quantity}
	 * 
	 * @param sellHeap, the heap that contains the sell order to be tested
	 * @param buyHeap, the heap that contains the buy order to be tested
	 * @return Double Array, the values of the standard in respective positions 
	 */
	public static double[] executeBuyOrder (
										HeapAdaptablePriorityQueue<Key,Order> sellHeap,
										HeapAdaptablePriorityQueue<Key,Order> buyHeap) {
		//Initialization of the return array
		double[] result = {-1, -1};
		
		//Calls to check if there is a valid transaction that can happen
		if(validTransaction(sellHeap, buyHeap)) {
			//The buyer and seller that will do the exchange
			Entry<Key, Order> seller = sellHeap.min();
			Entry<Key, Order> buyer = buyHeap.min();
			
			//The cost is an average of the two individuals bottom-line; 
			//The quantity is the minimum of the two
			double finalCost = Double.parseDouble(
					String.format("%.2f", (seller.getKey().cost + buyer.getKey().cost) / 2));
			int finalQuanity = Math.min(seller.getValue().quanity, buyer.getValue().quanity);
			
			//The alteration of the quantities of each entry
			seller.getValue().quanity = seller.getValue().quanity - finalQuanity;
			buyer.getValue().quanity = buyer.getValue().quanity - finalQuanity;
			
			//the new information to pass back to the caller
			result[0] = finalCost;
			result[1] = finalQuanity;
		}
		
		return result;
	}
	
	/**
	 * A method to print the given instructions and possibly attach a message to the end of it
	 * @param instructions, a string array of words to be printed on the same line
	 * @param message, an additional message that will be attached to the end of the array
	 */
	public static void printInstructions (String[] instructions, String message) {
		//Goes through the array and prints every word with a space after
		for(String e : instructions) {
			System.out.print(e + " ");
		}
		
		//Finishes the line with a string 
		System.out.println(message);
	}

	/**
	 * After a transaction occurs the quantity of items at the top of the heap might be zero
	 * iff the minimum is then removed from its queue and respective list
	 * @param heap, the priority queue that will be check for a valid minimum
	 * @param entries, the list to be cleared if an entry is no longer valid
	 */
	public static void cleanHeap (HeapAdaptablePriorityQueue<Key,Order> heap,
								  ArrayList<Entry<Key, Order>> entries) {
		//
		if(heap.min().getValue().quanity == 0)
			entries.remove(heap.removeMin());
	}

	/**
	 * Main method of the program. Takes in the input from a file at argument 0 of the command-line
	 * arguments. The program runs as long as there are queries to be run. Organizes some printing
	 * and is a centralized point for other methods to return the status of events 
	 * 
	 * @param args, the file path of a file that contains the queries
	 * @throws FileNotFoundException, an exception that is thrown if based of the path no file was 
	 * 									found.
	 */
	public static void main (String[] args) throws FileNotFoundException {
		//A file scanner to take in input from a specified path
		Scanner stdin = new Scanner(new File(args[0]));
		
		//Declaration and initialization of the array lists that will hold respective entries
		ArrayList<Entry<Key, Order>> sellEntries = new ArrayList<>();
		ArrayList<Entry<Key, Order>> buyEntries = new ArrayList<>();
		
		//Declaration and initialization of the priority queues that will hold respective key, orders
		HeapAdaptablePriorityQueue<Key,Order> sellHeap = new HeapAdaptablePriorityQueue<>();
		HeapAdaptablePriorityQueue<Key,Order> buyHeap = new HeapAdaptablePriorityQueue<>(new BuyerComparator());

		//A loop that will go through a file as long as there is input
		while (stdin.hasNextLine()) {
			//A state variable that will change based of the type of query
			boolean flag = true;
			
			//A message to be appended to the end of an instruction if some conflicts arise
			String errMessage = "";
			
			//The next query broken up by space for quick access to specifics information
			String[] input = stdin.nextLine().split(" ");
			
			//A state variable that will change based of the type of query
			Entry<Key, Order> current = null;

			//All the possible queries this program will accept
			if(input[0].equalsIgnoreCase("EnterBuyOrder")) {
				errMessage = (handleNewOrder(input, buyEntries, buyHeap)) ? "" : "ExistingBuyerError";
			} else if (input[0].equalsIgnoreCase("EnterSellOrder")) {
				errMessage = (handleNewOrder(input, sellEntries, sellHeap)) ? "" : "ExistingSellerError";
			} else if (input[0].equalsIgnoreCase("ChangeBuyOrder")) {
				errMessage = (changeOrder(input, buyEntries, buyHeap)) ? "" : "noBuyerError";
			} else if (input[0].equalsIgnoreCase("ChangeSellOrder")) {
				errMessage = (changeOrder(input, sellEntries, sellHeap)) ? "" : "noSellerError";
			} else if (input[0].equalsIgnoreCase("CancelBuyOrder")) {
				errMessage = (cancelOrder(input, buyEntries, buyHeap)) ? "" : "noBuyerError";
			} else if (input[0].equalsIgnoreCase("CancelSellOrder")) {
				errMessage = (cancelOrder(input, sellEntries, sellHeap)) ? "" : "noSellerError";
			} else if (input[0].equalsIgnoreCase("DisplayHighestBuyOrder")) {
				//Changes the flag state to false so a execution doesn't happen
				flag = false;
				
				//Sets the current Entry to the minimum of the buy queue to be printed
				current = buyHeap.min();
				
			} else if (input[0].equalsIgnoreCase("DisplayLowestSellOrder")) {
				//Changes the flag state to false so a execution doesn't happen
				flag = false;
				
				//Sets the current Entry to the minimum of the sell queue to be printed
				current = sellHeap.min();
			}
			
			//Checks whether to execute a buy order or not
			if (flag) {
				//Takes the just executed query and prints out its status
				printInstructions(input, errMessage);
				
				//Checks to see if a buy order can and will happen
				double[] toPrint = executeBuyOrder(sellHeap, buyHeap);
				
				//If a buy order happened then the values of toPrint will not be -1
				if(toPrint[0] != -1) {
					//Creates a string array of the format of an execution { title cost quantity }
					String[] instruc = {"ExecuteBuySellOrders", String.valueOf(toPrint[0]),
																String.valueOf((int) toPrint[1])};
					printInstructions(instruc, "");
					
					//The printing format for the execution of a buyer with its information
					System.out.printf("Buyer: %s %d%n", buyHeap.min().getValue().name,
										   buyHeap.min().getValue().quanity);
					
					//The printing format for the execution of a seller with its information
					System.out.printf("Seller: %s %d%n", sellHeap.min().getValue().name,
							   sellHeap.min().getValue().quanity);
					
					//Cleans the queues now that the information is no longer needed
					cleanHeap(sellHeap, sellEntries);
					cleanHeap(buyHeap, buyEntries);
				}
			} else {
				//Prints the current entry that was changed based of queries
				String in = (current !=  null)? current.getValue().name + " " + current.getKey().time + 
							" " + current.getKey().cost + " " + current.getValue().quanity : "";
				
				//Prints out the instruction and also the string of the order to be presented in
				//						{name time cost quantity}
				printInstructions(input, in);
			}
			
		}
		//Closes the file scanner when done to prevent potential memory leaks
		stdin.close();
	}
}
