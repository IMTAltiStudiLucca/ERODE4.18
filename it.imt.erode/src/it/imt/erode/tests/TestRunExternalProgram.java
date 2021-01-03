package it.imt.erode.tests;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;

public class TestRunExternalProgram {

	public static void main(String[] args) throws IOException {
		//String[] myArgs = new String[] {"g++", "test.cpp","-o","test.o"};
		String[] myArgs = new String[] {"g++", "test.cc","-o","test.o"};
		executeExternalProcess(myArgs);
		
		System.out.println("");
		System.out.println("");
		System.out.println("");
		System.out.println("");
		
		myArgs = new String[] {"/Users/anvan/OneDrive/OneDrive - Danmarks Tekniske Universitet/ERODE/it.imt.erode/test.o"};
		executeExternalProcess(myArgs);
		
	}

	private static void executeExternalProcess(String[] myArgs) throws IOException {
		//g++ test.cpp -o test.o
		ProcessBuilder pb = new ProcessBuilder(myArgs);
		//ProcessBuilder pb = new ProcessBuilder("g++", "test2.cpp","-o","test.o");
		//ProcessBuilder pb = new ProcessBuilder("cat", "test.cpp");
		//pb = new ProcessBuilder(List<String> command)
		Process process = pb.start();

		InputStream is = process.getInputStream();
		InputStreamReader isr = new InputStreamReader(is);
		BufferedReader br = new BufferedReader(isr);
		String line;

		InputStream is_err = process.getErrorStream();
		InputStreamReader isr_err = new InputStreamReader(is_err);
		BufferedReader br_err = new BufferedReader(isr_err);


		System.out.printf("Output of running %s is:\n", Arrays.toString(myArgs));
		boolean hasOutput=false;
		while ((line = br.readLine()) != null) {
			hasOutput=true;
			System.out.println(line);
		}
		br.close();
		isr.close();

		System.out.println("");
		System.out.printf("Error output of running %s is:\n", Arrays.toString(myArgs));
		boolean hasErrorOutput=false;
		while ((line = br_err.readLine()) != null) {
			hasErrorOutput=true;
			System.out.println(line);
		}
		br_err.close();
		isr_err.close();

		
		if(hasOutput) {
			System.out.println("There is output");
		}
		else {
			System.out.println("There is NO output");
		}
		if(hasErrorOutput) {
			System.out.println("There is error output");
		}
		else {
			System.out.println("There is NO error output");
		}
		
	}

}
