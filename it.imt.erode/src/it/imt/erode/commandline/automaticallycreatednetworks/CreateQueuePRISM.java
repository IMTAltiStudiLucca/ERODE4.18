package it.imt.erode.commandline.automaticallycreatednetworks;

import java.io.IOException;

import it.imt.erode.importing.automaticallygeneratedmodels.QueuePRISM;

public class CreateQueuePRISM {

	public static void main(String[] args) {
		int clientClasses=2;
		//int queueSize=13;
		int lambda=2;
		int mu=3;//5
		for(int queueSize=2;queueSize<=2;queueSize+=2) {
			//String fileName="/Users/andrea/OneDrive - Scuola Superiore Sant'Anna/runtimes/runtime-ERODE.product(4)/UAI/queueCC"+
			//String fileName="/Users/andrea/OneDrive - Scuola Superiore Sant'Anna/runtimes/runtime-ERODE.product(6)/UCTMC_TAC/closedFormSolution/models/queue/queueCC"+
			String fileName="/Users/andrea/Dropbox/prism-4.7-osx64/my-models/ctmcs/queue/queueCC"+
					clientClasses+"QS"+queueSize+
					"La"+lambda+"mu"+mu+
					".sm";
			try {
				QueuePRISM.writeQueueNetwork(queueSize, clientClasses, lambda,mu, fileName);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		System.out.println("Completed");

	}

}
