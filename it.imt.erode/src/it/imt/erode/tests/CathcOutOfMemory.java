package it.imt.erode.tests;

public class CathcOutOfMemory {

	public static void main(String[] args) throws Exception {
		int i=0;
		while(true){
			i++;
			System.out.println(i);
			try{
				int[] pippo = new int[1000000000];
				pippo[0]=1;
			}catch(OutOfMemoryError e){
				System.out.println("Ciao ciao");
				throw new Exception(e);
			}
			
		}

	}

}
