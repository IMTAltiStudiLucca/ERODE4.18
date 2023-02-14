package it.imt.erode.importing.automaticallygeneratedmodels;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class QueuePRISM {

	public static void writeQueueNetwork(int queueSize,int clientClasses, int lambda, int mu,String fileName) throws IOException {
		BufferedWriter bw = new BufferedWriter(new FileWriter(fileName));
		
		bw.write(
  "ctmc\n"
+ "\n"
+ "//Queue model with "+clientClasses+" classes of clients (CC"+clientClasses+") and  queue with "+queueSize+" positions (QS"+queueSize+").\n//\t These parameters are hardcoded in the model\n"
+ "//In addition, the model has arrival rate lambda = "+lambda+", and service rate mu = "+mu+".\n"
+ "const double la="+lambda+"; \n"
+ "const double mu="+mu+";\n"
+ "const double k="+((int)(0.5+queueSize/2.0))+";//ceiling(queue capacity/2)\n"
+ "\n"
+ "module client1\n"
+ "  \n"
+ "  // Create a new job\n"
+ "  [create1] true -> la : true ;\n"
+ "\n"
+ "  // Serve the job\n"
+ "  [serve1]  true -> mu : true;\n"
+ "\n"
+ "endmodule\n"
+ "\n"
+ "");
		
		for(int clientClass=2;clientClass<=clientClasses;clientClass++) {
			bw.write(
					  "module client"+clientClass+" = client1 [create1=create"+clientClass+",\n"
					+ "                          serve1=serve"+clientClass+" ]\n"
					+ "endmodule\n");
		}
		bw.write("\n");
		
		
		
		bw.write("module scheduler\n"
				+ "  //One job per entry in the queue\n");
		for(int q=1;q<=queueSize;q++) {
			//job1 : [0..3] init 0; // First job in the queue
			bw.write("  job"+q+" : [0.."+clientClasses+"] init 0; //Position "+q+" in the queue\n");
		}
		//bw.write("  tot_jobs : [0.."+queueSize+"] init 0; //How many jobs are currently in the queue\n");
		bw.write("\n\n");
		
		
		for(int q=queueSize;q>=2;q--) {
			bw.write("  // Place a new job in position "+q+" of the queue\n");
			for(int clientClass=1;clientClass<=clientClasses;clientClass++) {
				//createi
				bw.write("  [create"+clientClass+"] job1 >0 ");
				for(int q2=2;q2<q;q2++) {
					bw.write(" & job"+q2+">0 ");
				}
				bw.write(" & job"+q+"=0 ");
				//bw.write(" -> (job"+q+"'="+clientClass+")& (tot_jobs' = tot_jobs + 1);");
				bw.write(" -> (job"+q+"'="+clientClass+");");
				bw.write("\n");
				//[create1] job1 >0 & job2=0 -> (job2'=1);
				//[create2] job1 >0 & job2=0 -> (job2'=2);
				//[create3] job1 >0 & job2=0 -> (job2'=3);
			}
			bw.write("\n");
		}
		
		
		bw.write("  // Place a new job in position 1 of the queue");
		bw.write("\n");
		for(int clientClass=1;clientClass<=clientClasses;clientClass++) {
			//createi
			//bw.write("  [create"+clientClass+"] job1 =0  -> (job1'="+clientClass+") & (tot_jobs' = tot_jobs + 1);\n");
			bw.write("  [create"+clientClass+"] job1 =0  -> (job1'="+clientClass+");\n");
			  /*
			  // Place a new job in position 1 of the queue
			  [create1] job1 =0  -> (job1'=1);
			  [create2] job1 =0  -> (job1'=2);
			  [create3] job1 =0  -> (job1'=3);
			  */
		}
		bw.write("\n\n");
		
		bw.write("  // Serve the job at the head of the queue");
		bw.write("\n");
		for(int clientClass=1;clientClass<=clientClasses;clientClass++) {
			//[serve1] job1=1 -> (job1'=job2) & (job2'=0);
			bw.write("  [serve"+clientClass+"] job1="+clientClass+" -> (job1'=job2) ");
			for(int q=2;q<queueSize;q++) {
				bw.write("& (job"+q+"'=job"+(q+1)+")");
			}
			//bw.write("& (job"+queueSize+"'=0) & (tot_jobs' = tot_jobs - 1);\n");
			bw.write("& (job"+queueSize+"'=0);\n");
		}
		
		bw.write("\n");
		bw.write("endmodule\n");
		bw.write("\n");
		
		bw.write("system\n"
				+ "  scheduler || client1");
		for(int clientClass=2;clientClass<=clientClasses;clientClass++) {
			bw.write(					  " || client"+clientClass);
		}
		bw.write("\n");
		bw.write("endsystem\n");
		bw.write("\n");
		
		bw.write("\n");
		//bw.write("formula tot_jobs = (job1 > 0 ? 1 : 0) + (job2 > 0 ? 1 : 0) + (job3 > 0 ? 1 : 0) + (job4 > 0 ? 1 : 0) + (job5 > 0 ? 1 : 0);\n");
		bw.write("formula tot_jobs =");
		for(int q=1;q<=queueSize;q++) {
			bw.write(" (job"+q+" > 0 ? 1 : 0)");
			if(q<queueSize) {
				bw.write(" +");
			}
		}
		bw.write(";\n");
		bw.write("\n");
		
		bw.write("// Distance from desired (k) occupancy to actual one\n");
		bw.write("rewards \"percent_op\"\n");
		bw.write("	k - tot_jobs > 0  :   k - tot_jobs ;\n");
		bw.write("	k - tot_jobs <= 0 : -(k - tot_jobs);\n");
		bw.write("endrewards\n");
		bw.write("\n");
		
		bw.write("// Tot jobs waiting\n");
		bw.write("rewards \"tot_jobs_waiting\"\n");
		bw.write("	true  : tot_jobs ;\n");
		bw.write("endrewards\n");
		bw.write("\n");
		bw.close();
		
	}
	
	
}

/*
//This is an example of a FIFO queue with size 2 (job1, job2), and 3 classes of clients (client1 - client3) 

ctmc

const double la=1; 
const double mu=3;

module client1
  
  // Create a new job
  [create1] true -> la : true ;

  // Serve the job
  [serve1]  true -> mu : true;

endmodule

module client2 = client1 [create1=create2,
                          serve1=serve2 ]
endmodule

module client3 = client1 [create1=create3,
                          serve1=serve3 ]
endmodule

module scheduler
  //one job per entry in the queue
  job1 : [0..3] init 0; // Position 1 in the queue
  job2 : [0..3] init 0; // Position 2 in the queue

  // Place a new job in position 2 of the queue
  [create1] job1 >0 & job2=0 -> (job2'=1);
  [create2] job1 >0 & job2=0 -> (job2'=2);
  [create3] job1 >0 & job2=0 -> (job2'=3);

  // Place a new job in position 1 of the queue
  [create1] job1 =0  -> (job1'=1);
  [create2] job1 =0  -> (job1'=2);
  [create3] job1 =0  -> (job1'=3);

  // Serve the job at the head of the queue
  [serve1] job1=1 -> (job1'=job2) & (job2'=0);
  [serve2] job1=2 -> (job1'=job2) & (job2'=0);
  [serve3] job1=3 -> (job1'=job2) & (job2'=0);

endmodule

system
  scheduler || client1 || client2 || client3
endsystem

*/