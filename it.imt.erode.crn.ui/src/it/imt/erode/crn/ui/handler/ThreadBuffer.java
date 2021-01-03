package it.imt.erode.crn.ui.handler;

import java.util.ArrayList;

public class ThreadBuffer<Worker extends Thread> {

	private ArrayList<Worker> threadPool;

	public ThreadBuffer(int size){
		threadPool = new ArrayList<Worker>(size);
	}

	public synchronized void addWorker(Worker worker) {

		threadPool.add(worker);
		/*if(threadPool.size()>1){
			System.out.println("There are "+threadPool.size()+" threads before me");
		}*/
		
		while(threadPool.get(0) != worker) 
		{
			try { 
				wait(); 
			}
			catch (InterruptedException e) 
			{
				System.out.println("Wait interrupted: "+e.getMessage());
			} 
			finally { }
		} 
		//threadPool.remove(0);
		//notifyAll(); 
	}
	
	public synchronized void removeFirstWorker() {
		threadPool.remove(0);
		if(threadPool.size()>0){
			notifyAll();
		}
	}

}