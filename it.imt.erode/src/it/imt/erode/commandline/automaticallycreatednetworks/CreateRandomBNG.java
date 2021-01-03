package it.imt.erode.commandline.automaticallycreatednetworks;

import it.imt.erode.importing.automaticallygeneratedmodels.RandomBNG;

public class CreateRandomBNG {
	
	
	public static void main(String[] args) {
		String path ="/Users/andrea/OneDrive - Danmarks Tekniske Universitet/runtimes/runtime-ERODE.product(4)/SantAnna/large/";
		int r = 25000000;
		int s =  2500000;
		int maxNumberOfProducts=2;
		double nonLinerarityFactor=0.3;
		int nlArity=2;
		
		//RandomBNG.createRandomNet(path,s, r, maxNumberOfProducts, nonLinerarityFactor, nlArity,"");
		
		
//		r = 30000000;
//		s =  3000000;
//		RandomBNG.createRandomNet(path,s, r, maxNumberOfProducts, nonLinerarityFactor, nlArity,"2");
		
		r = 300;
		s =  30;
		RandomBNG.createRandomNet(path,s, r, maxNumberOfProducts, nonLinerarityFactor, nlArity,"2");
	}

	public static void mainTACASToolPaper(String[] args) {
		String path ="/Users/andrea/Copy/TOOLBiology/DeAR-CRN/erode/runtime-ERODE.product/tacas2017/RandomNetworks/";
		//String path ="/Volumes/WDELEMENTS/experiments/tacas2017/RandomNetworks/";
		
		int maxNumberOfProducts=1;//int maxNumberOfProducts=2;
		//double nonLinearityFactor=0.3;
		
		//BioNetGenImporter.printRandomCRNToNetFile("/Users/andrea/Copy/TOOLBiology/DeAR-CRN/erode/runtime-ERODE.product/tacas2017/RandomNetworks/randomCRN2.net", 10, 20, 0.5, 3, 10, 1000, null, null);
		//createRandomNet(path,100000, 1000000,maxNumberOfProducts, 0.3, 2,"1");
		//createRandomNet(path,50000, 500000,maxNumberOfProducts, 0.3, 2,"1");
		//createRandomNet(path,25000, 250000,maxNumberOfProducts, 0.3, 2,"1");
		//createRandomNet(path,10000, 100000,maxNumberOfProducts, 0.3, 2,"1");
		//createRandomNet(path,25000, 250000,maxNumberOfProducts, 0, 2,"1");
		/*for(int i=5;i<=50;i+=5){
			createRandomNet(path,100000*i, 1000000*i,maxNumberOfProducts, 0.3, 2);
		}*/
		
		/*for(double nlf=0;nlf<=1;nlf+=0.2){
			createRandomNet(path,250000, 3500000, maxNumberOfProducts, nlf);
		}*/
//		int r=3500000;
//		int s= 250000;
//		createRandomNet(path,s, r, maxNumberOfProducts, 0.2, 2);
//		createRandomNet(path,s, r, maxNumberOfProducts, 0.4, 2);
//		createRandomNet(path,s, r, maxNumberOfProducts, 0.6, 2);
//		createRandomNet(path,s, r, maxNumberOfProducts, 0.8, 2);
//		createRandomNet(path,s, r, maxNumberOfProducts, 1.0, 2);
		
		int r=3500000;
		int s= 250000;
		for(int i=1;i<=5;i++){
			//createRandomNet(path,s, r, maxNumberOfProducts, 0, 2,String.valueOf(i));
			/*createRandomNet(path,s, r, maxNumberOfProducts, 0.2, 2,String.valueOf(i));
			createRandomNet(path,s, r, maxNumberOfProducts, 0.4, 2,String.valueOf(i));
			createRandomNet(path,s, r, maxNumberOfProducts, 0.6, 2,String.valueOf(i));
			createRandomNet(path,s, r, maxNumberOfProducts, 0.8, 2,String.valueOf(i));
			createRandomNet(path,s, r, maxNumberOfProducts, 1.0, 2,String.valueOf(i));*/
		}
		//createRandomNet(path,4, 4, maxNumberOfProducts, 0.2, 2,String.valueOf(0));
		r=50000;
		s= 5000;
		path ="/Users/andrea/Copy/TOOLBiology/DeAR-CRN/erode/runtime-ERODE.product/tacas2017/RandomNetworks/scalabilityBDE/";
		for(int i=1;i<=5;i++){
//			createRandomNet(path,s, r, maxNumberOfProducts, 0, 2,String.valueOf(i));
//			createRandomNet(path,s, r, maxNumberOfProducts, 0.2, 2,String.valueOf(i));
			r=50000;
			s= 5000;
			RandomBNG.createRandomNet(path,s, r, maxNumberOfProducts, 0.3, 2,String.valueOf(i));				
			while(r<=300000){
				//R=    250,000 S=   25,000
				r+=50000;
				s+=5000;
				RandomBNG.createRandomNet(path,s, r, maxNumberOfProducts, 0.3, 2,String.valueOf(i));				
			}
			/*createRandomNet(path,s, r, maxNumberOfProducts, 0.4, 2,String.valueOf(i));
			createRandomNet(path,s, r, maxNumberOfProducts, 0.6, 2,String.valueOf(i));
			createRandomNet(path,s, r, maxNumberOfProducts, 0.8, 2,String.valueOf(i));
			createRandomNet(path,s, r, maxNumberOfProducts, 1.0, 2,String.valueOf(i));*/
		}
		
		path ="/Users/andrea/Copy/TOOLBiology/DeAR-CRN/erode/runtime-ERODE.product/tacas2017/RandomNetworks/scalabilityFDE/";
		for(int i=1;i<=5;i++){
			r=500;
			s= 50;
//			createRandomNet(path,s, r, maxNumberOfProducts, 0, 2,String.valueOf(i));
//			createRandomNet(path,s, r, maxNumberOfProducts, 0.2, 2,String.valueOf(i));
			RandomBNG.createRandomNet(path,s, r, maxNumberOfProducts, 0.3, 2,String.valueOf(i));				
			while(r<=10000){
				//R=    250,000 S=   25,000
				r+=500;
				s+=50;
				RandomBNG.createRandomNet(path,s, r, maxNumberOfProducts, 0.3, 2,String.valueOf(i));				
			}
			/*createRandomNet(path,s, r, maxNumberOfProducts, 0.4, 2,String.valueOf(i));
			createRandomNet(path,s, r, maxNumberOfProducts, 0.6, 2,String.valueOf(i));
			createRandomNet(path,s, r, maxNumberOfProducts, 0.8, 2,String.valueOf(i));
			createRandomNet(path,s, r, maxNumberOfProducts, 1.0, 2,String.valueOf(i));*/
		}
		
		/*for(double nlf=0.2;nlf<=1;nlf+=0.2){
			//createRandomNet(path,250, 1500, maxNumberOfProducts, nlf, 2);
			//createRandomNet(path,250, 1500, maxNumberOfProducts, nlf, 5);
		}*/
//		createRandomNet(path,250, 1500, maxNumberOfProducts, 0.2, 2);
//		createRandomNet(path,250, 1500, maxNumberOfProducts, 0.4, 2);
//		createRandomNet(path,250, 1500, maxNumberOfProducts, 0.6, 2);
//		createRandomNet(path,250, 1500, maxNumberOfProducts, 0.8, 2);
//		createRandomNet(path,250, 1500, maxNumberOfProducts, 1.0, 2);
		
//		createRandomNet(path,250, 1500, maxNumberOfProducts, 0.2, 5);
//		createRandomNet(path,250, 1500, maxNumberOfProducts, 0.4, 5);
//		createRandomNet(path,250, 1500, maxNumberOfProducts, 0.6, 5);
//		createRandomNet(path,250, 1500, maxNumberOfProducts, 0.8, 5);
//		createRandomNet(path,250, 1500, maxNumberOfProducts, 1.0, 5);
		
		//double nlf=0.4;
		/*for(int degree=5;degree<=50;degree+=5){
			createRandomNet(path,250, 1500, maxNumberOfProducts, nlf, degree);
		}*/
//		createRandomNet(path,250, 1500, maxNumberOfProducts, 0.4, 20);
//		createRandomNet(path,250, 1500, maxNumberOfProducts, 0.4, 40);
//		createRandomNet(path,250, 1500, maxNumberOfProducts, 0.4, 60);
//		createRandomNet(path,250, 1500, maxNumberOfProducts, 0.4, 80);
//		createRandomNet(path,250, 1500, maxNumberOfProducts, 0.4, 100);		
	}

}
