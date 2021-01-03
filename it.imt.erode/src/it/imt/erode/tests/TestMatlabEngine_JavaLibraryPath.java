package it.imt.erode.tests;

import java.lang.reflect.Field;
import java.util.Arrays;

import com.mathworks.engine.MatlabEngine;

/*
 * Info can be found here:
 * https://www.mathworks.com/help/matlab/matlab-engine-api-for-java.html
 * Setup info
 * https://www.mathworks.com/help/matlab/matlab_external/setup-environment.html
 * 
 * javac -classpath /Applications/MATLAB_R2018a.app/extern/engines/java/jar/engine.jar TestMatlabEngine.java 
 * java -Djava.library.path=/Applications/MATLAB_R2018a.app/bin/maci64 -classpath .:/Applications/MATLAB_R2018a.app/extern/engines/java/jar/engine.jar TestMatlabEngine
 * 
 * java -classpath .:/Applications/MATLAB_R2018a.app/extern/engines/java/jar/engine.jar TestMatlabEngine
 * 
 * 
 * java -Djava.library.path=/Applications/MATLAB_R2018a.app/bin/maci64 -classpath .:/Users/anvan/OneDrive/OneDrive\ -\ Danmarks\ Tekniske\ Universitet/ERODE/TestMatlab/lib/matlab/engine.jar TestMatlabEngine
 * 
 */

public class TestMatlabEngine_JavaLibraryPath{
	public static void main(String[] args) throws Exception{
		/*DOES NOT WORK*/
		String jlp = System.getProperty("java.library.path");
		//System.out.println("before: ");
		System.out.println(jlp);
		/*System.setProperty("java.library.path",jlp+":/Applications/MATLAB_R2018a.app/bin/maci64");
		System.setProperty("java.library.path","/Applications/MATLAB_R2018a.app/bin/maci64");
		System.out.println("after: ");
		System.out.println(System.getProperty("java.library.path"));
		 */
		//Works, but if you print System.getProperty("java.library.path") it does not seem updated
		addLibraryPath("/Applications/MATLAB_R2018a.app/bin/maci64");
		
		MatlabEngine eng = MatlabEngine.startMatlab();
		double[] a = {2.0 ,4.0, 6.0};
		double[] roots = eng.feval("sqrt", a);
		for (double e: roots) {
			System.out.println(e);
		}
		
		System.out.println("Before running the script");
		//run("/Users/anvan/OneDrive/OneDrive - Danmarks Tekniske Universitet/runtimes/runtime-ERODE.product(4)/VanderPol/vanderPol.m")
		String script = "/Users/anvan/OneDrive/OneDrive - Danmarks Tekniske Universitet/runtimes/runtime-ERODE.product(4)/VanderPol/vanderPol.m";
		//eng.feval("run", script);
		eng.eval("run(\""+ script+"\")");
		System.out.println("After running the script");
		
		eng.close();
	}
	
	public static void addLibraryPath(String pathToAdd) throws Exception {
	    Field usrPathsField = ClassLoader.class.getDeclaredField("usr_paths");
	    usrPathsField.setAccessible(true);

	    String[] paths = (String[]) usrPathsField.get(null);

	    for (String path : paths) {
	    		System.out.println(path);
	    		if (path.equals(pathToAdd)) {
	    			return;
	    		}
	    }
	        

	    String[] newPaths = Arrays.copyOf(paths, paths.length + 1);
	    newPaths[newPaths.length - 1] = pathToAdd;
	    usrPathsField.set(null, newPaths);
	}
}