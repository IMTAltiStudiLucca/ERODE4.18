package it.imt.erode.commandline.automaticallycreatednetworks;

import java.io.IOException;

import it.imt.erode.importing.automaticallygeneratedmodels.QueuePRISM;

public class CreateQueuePRISM {

	public static void main(String[] args) {
		int clientClasses=2;
		int queueSize=15;
		String fileName="/Users/andrea/OneDrive - Scuola Superiore Sant'Anna/runtimes/runtime-ERODE.product(4)/UAI/queueCC"+
				clientClasses+"QS"+queueSize+".sm";
		try {
			QueuePRISM.writeQueueNetwork(queueSize, clientClasses, fileName);
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Completed");

	}

}
