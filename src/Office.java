import java.util.concurrent.Semaphore;

/**
 * 
 * @author Bua Pietro
 *
 */
public class Office
{
	// Change these
	private final int NR_OF_DEVELOPERS = 5; // !!! If NR_OF_DEVELOPERS smaller than [nr_minimum_developers] 
											// nr_minimum_developers will be set to 1 !! Change it to your wishes!!
	private int nr_minimum_developers = 3; 	// If less than 1 developer declared then value is 1
	private final int NR_OF_CUSTOMERS = 7;

	// Don't change these
	private Thread leader;
	private Thread[] developers;
	private Thread[] customers;
	

	
	private int waitingDevelopers = 0, 
				waitingCustomers = 0;
	
	private Semaphore leaderAvailabilty,
					  nextDeveloper, nextCustomer,  
					  meetingInvitationCus, meetingInvitationDev,
					  travel, travelAway;
	private Semaphore mutex;
	
	private boolean available = true;
	
	
	/**
	 * Office constructor, here we initialize all the threads and we make an instance of the Semaphores.
	 */
	public Office()
	{
		nextDeveloper = new Semaphore(0, true);
		nextCustomer = new Semaphore(0, true);
		leaderAvailabilty = new Semaphore(0, true);
		meetingInvitationCus = new Semaphore(0,true );
		meetingInvitationDev = new Semaphore(0,true);
		travel = new Semaphore(0,true);
		travelAway = new Semaphore(0,true);
		
		mutex = new Semaphore(1); //
		
		if(NR_OF_DEVELOPERS < nr_minimum_developers)
		{
			nr_minimum_developers = 1;
		}
		
		leader = new Leader("Klaas");
		leader.start();
		
		developers = new Thread[NR_OF_DEVELOPERS];
		for(int i = 0; i < NR_OF_DEVELOPERS;i++)
		{
			developers[i] = new Developer( Integer.toString(i) );
			developers[i].start();
		}
		
		customers = new Thread[NR_OF_CUSTOMERS];
		for(int i=0; i< NR_OF_CUSTOMERS;i++)
		{
			customers[i] = new Customer(  Integer.toString(i) );
			customers[i].start();
		}
	}
	
    ////////////////////////////////////////////////////////////////////////////////
	/**
	 * 
	 * @author Pietro Bua & Rohmin Mirza
	 *
	 */
	class Leader extends Thread
	{
		/**
		 * 
		 * @param name
		 */
		public Leader( String name )
		{
			super(name);
		}

		//////////////////////////////////////////////////
		/**
		 * 
		 * @param n
		 */
		private void sendWaitingDevsToMeeting(int n)
		{			
			for(int i=0, t = n; i<t ;i++)
			{
				meetingInvitationDev.release(); // These developers don't have to wait for the meeting
			}
		}
		
		/**
		 * 
		 * @param n
		 */
		private void sendWaitingDevsToWork(int n)
		{			
			for(int i=0, t = n; i<t ;i++)
			{
				waitingDevelopers--;
				nextDeveloper.release();
			}
		}
		
		
		//////////////////////////////////////////////////
		/**
		 * 
		 * @param n
		 */
		private void sendWaitingCustToMeeting(int n)
		{			
			for(int i=0, t = n; i<t ;i++)
			{
				meetingInvitationCus.release();
			}
		}

		/**
		 * 
		 * @param n
		 */
		private void sendWaitingCustsAway(int n)
		{			
			for(int i=0, t = n; i<t ;i++)
			{
				waitingCustomers--;
				nextCustomer.release();
			}
		}
		
		/**
		 * 
		 * @param n
		 */
		private void inviteWaitingCustomers(int n)
		{			
			for(int i=0, t = n; i<t ;i++)
			{
				travel.release();
			}
		}
		
		/**
		 * 
		 * @param n
		 * @throws InterruptedException
		 */
		private void customersTravellingBack(int n) throws InterruptedException
		{			
			for(int i=0, t = n; i<t ;i++)
			{
				travelAway.acquire();
			}
		}

		//////////////////////////////////////////////////
		/**
		 * Here the leader decides with who to meet or if he should sleep.
		 */
		public void run()
		{
			while(true)
			{
				//justLive();
				try
				{					
					mutex.acquire();
					System.out.println("waitingCustomers: " + waitingCustomers);
					System.out.println("waitingDevelopers: " + waitingDevelopers);
					if( waitingCustomers > 0)
					{
						System.out.println("Leader - waitingCustomers: " + waitingCustomers );
						if(waitingDevelopers>0)
						{
							System.out.println("waitingDevelopers: " + waitingDevelopers );
							
							sendWaitingDevsToWork(waitingDevelopers-1); // Tell all developers (only 1 not) to go back to work
							System.out.println("All waitingDevelopers (apart from 1) sent to work");
							assert( waitingDevelopers == 1 ):"More than 1 waiting developer: " + waitingDevelopers;
							
							// Have the meeting
							available = false;
							meetingInvitationDev.release(); 			// Developer (1) can enter the meeting
							
							inviteWaitingCustomers(waitingCustomers);
							System.out.println("Customers arrived");
							
							sendWaitingCustToMeeting(waitingCustomers);	// Customers can enter the meeting
							System.out.println("All waitingCustomers sent to the meeting room");
							mutex.release();
							
							meeting();									// === !! HAVE THE MEETING !! ===
							
							mutex.acquire();
							available = true;

							sendWaitingDevsToWork(waitingDevelopers); 	// 
							assert(waitingDevelopers == 0):"Some developers are waiting: " + waitingDevelopers;
							sendWaitingCustsAway( waitingCustomers ); 	// Customers can go away
							
							System.out.println("All customers travelling away");
							customersTravellingBack(waitingCustomers);
							mutex.release();
						}
						else
						{
							mutex.release();
						}
					}
					else if(waitingDevelopers >= nr_minimum_developers)
					{
						System.out.println("Leader - waitingDevelopers: " + waitingDevelopers);
						
						available = false;
						sendWaitingDevsToMeeting(waitingDevelopers);
						System.out.println("All waitingDevelopers sent to meeting room");
						mutex.release();
						
						meeting();										// === !! HAVE THE MEETING !! ===
						
						mutex.acquire();
						available = true;
						sendWaitingDevsToWork(waitingDevelopers);
						System.out.println("All waitingDevelopers sent back to work");
						assert(waitingDevelopers==0):"Some developers are waiting: " + waitingDevelopers;
						mutex.release();
					}
					else
					{
						//System.out.println("Nothing to do release mutex");
						mutex.release();
					}
				}
				catch (InterruptedException e)
				{
					System.out.println( e );
					Thread.currentThread().interrupt();
				}
				
				// ========================================================================
				try
				{
					System.out.println("Leader will go back to sleep");
					leaderAvailabilty.acquire();
				}
				catch (InterruptedException e)
				{
					System.out.println( e );
					//e.printStackTrace();
				}
			}
		}
		
		/**
		 * This method is used to generate a random time for the meeting
		 */
		private void meeting() throws InterruptedException
		{
			int time = (int)(Math.random() * 1000);
			System.out.println( ">>>>>>>>> Meeting " + time + " ms <<<<<<<<<<");
			Thread.sleep( time );
		}
		
	}
	
     ////////////////////////////////////////////////////////////////////////////////
	/**
	 * 
	 * @author Bua_Pietro
	 *
	 */
	class Developer extends Thread
	{
		/**
		 * 
		 * @param name
		 */
		public Developer( String name )
		{
			super(name);
		}
		
		/**
		 * In this method the developer develops and checks if he can meet with a leader.
		 */
		public void run()
		{
			while(true)
			{
				try
				{
					justDevelop();
					mutex.acquire(); // Mutex for available and waitingDevelopers and available
					if( available )
					{
						assert( available ): "Leader is not available";
						waitingDevelopers++;
						System.out.println("waitingDevelopers: " + waitingDevelopers);
						mutex.release();
						
						leaderAvailabilty.release();   // Tell leader I want to meet
						
						System.out.println("Developer " + getName() + " is waiting for invitation");
						nextDeveloper.acquire();
						if( available )
						{
							System.out.println("Developer " + getName() + " is waiting to enter the meeting");
							meetingInvitationDev.acquire();
						}
					}
					else
					{
						mutex.release();
						System.out.println("Leader is not available: Developer " + getName() + " back to work!");
					}
				}
				catch (InterruptedException e)
				{
					System.out.println( e );
					Thread.currentThread().interrupt();
				}
			}
		}
		
		/**
		 * Makes a random time in which the developer is programming
		 */
		private void justDevelop() throws InterruptedException
		{
	        int time = (int)(Math.random() * 1000);
			System.out.println("Developer " + getName() + " living " + time + "ms");
			Thread.sleep( time );
		}
	}
	
     ////////////////////////////////////////////////////////////////////////////////
	/**
	 * 
	 * @author Bua_Pietro
	 *
	 */
	class Customer extends Thread
	{
		/**
		 * 
		 * @param name
		 */
		public Customer( String name )
		{
			super(name);
		}
		
		/**
		 * In this method the customer tells at the office he wants to meet with the leader and waits for him 
		 * to call the customer
		 */
		public void run()
		{
			while(true)
			{
				try
				{
					justLive();

					mutex.acquire();
					waitingCustomers++;
					System.out.println("waitingCustomers: " + waitingCustomers );
					mutex.release();
					
					leaderAvailabilty.release(); // Tell leader I want to meet
					
					System.out.println("Customer " + getName() + " waiting to travel");
					travel.acquire();            // Wait for travel invitaion
					travel();
					
					System.out.println("Customer " + getName() + " is waiting to be invited");
					nextCustomer.acquire();      // Have the meeting
					
					System.out.println("Customer " + getName() + " enters the meeting room");
					meetingInvitationCus.acquire();

					travel();
					travelAway.release();
					System.out.println("Customer " + getName() + " returned");
				}
				catch (InterruptedException e)
				{
					System.out.println( e );
					Thread.currentThread().interrupt();
				}
			}
		}
		
		/**
		 * Makes a random time in which the customer is living
		 */
		private void justLive() throws InterruptedException
		{
			int time = (int)(Math.random() * 1000);
			System.out.println( "Customer " + getName() + " living " + time + "ms");
			Thread.sleep( time );
		 }
		
		/**
		 * Makes a random time in which the customer is living
		 */
		private void travel() throws InterruptedException
		{
			int time = (int)(Math.random() * 1000);
			System.out.println( "Customer " + getName() + " travelling " + time + "ms");
			Thread.sleep( time );
		 }		
	}
}
