/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.imt.erode.importing.sbml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.sbml.jsbml.ASTNode;



/**
 * @author Isabel Cristina Perez-Verona, Andrea Vandin
 */
public class BannedNamesList {
    ArrayList <String> bannedNames;
    Map<String,String> localTerms;

    public BannedNamesList(){
        bannedNames =  new ArrayList<>();
        this.bannedNames.add("ALL");
        this.bannedNames.add("modelDefinitions");
        this.bannedNames.add("element");
        this.bannedNames.add("ID");
        this.bannedNames.add("euler");
        this.bannedNames.add("max");
        this.bannedNames.add("min");
        this.bannedNames.add("IC");
        this.bannedNames.add("epsilon");
        this.bannedNames.add("kmax");
        this.bannedNames.add("species");
        this.bannedNames.add("slopes");
        this.bannedNames.add("cora");
        this.bannedNames.add("param");
        this.bannedNames.add("delta");
        this.bannedNames.add("NO");
        this.bannedNames.add("Hill");
        this.bannedNames.add("ssa");
        this.bannedNames.add("d");
        this.bannedNames.add("PQ");
        this.bannedNames.add("M0");
        localTerms = new HashMap<>();
        
        }
    
    public static boolean isNumeric(String str) { 
    	  try {  
    	    Double.parseDouble(str);  
    	    return true;
    	  } catch(NumberFormatException e){  
    	    return false;  
    	  }  
    	}
    
    public boolean isBanned(String string){
        boolean flag = false;
        
        //ANDREA
        
        if(string.startsWith("_") && string.matches("^[a-zA-Z0-9_]*$")) {
        	return true;
        }
        
        //Andrea: can't do this check because this method is invoked also for something like '1*k1'
//        else if (Character.isDigit(string.charAt(0)) && !isNumeric(string)){
//        	return true;
//        }

        
        for (String name : bannedNames)
            if (name.equalsIgnoreCase(string))
                flag=true;
    return flag;
    }
    
    public boolean isBanned(ASTNode node){
        boolean flag = false;
        for (String name : bannedNames)
            if (name.equalsIgnoreCase(node.getId()))
                flag=true;
    return flag;
    }
    
    
    public String verifySingleTerm (String string){
        if (isBanned(string)) {
            //localTerms.put(string, string+"_a1");
        	localTerms.put(string, makeAdmissible(string));//ANDREA
            string = localTerms.get(string);}

    return string;}
    
    public String verifyTerms (String formula){
    	for (Entry<String, String> term : localTerms.entrySet()) {
    		String key = term.getKey();
            String value = term.getValue();
            if (formula.contains(key)) {
                formula.replace(key, value);
            }
		}    	
//        localTerms.entrySet().forEach((term) -> {
//            String key = term.getKey();
//            String value = term.getValue();
//            if (formula.contains(key)) {
//                formula.replace(key, value);
//            }
//        });
    return formula;}
    
    //Andrea
    public static String makeAdmissible(String str) {
    	//before there was a spaghetti code doing: str+"_a1"
    	return "s"+str;
    }
    public ASTNode verifyASTNodeTerm (ASTNode node){
        ASTNode node1 = node.clone();
        //localTerms.put(node.getId(), node.getId()+"_a1");
        localTerms.put(node.getId(), makeAdmissible(node.getId()));
        
        if(node.getChildCount()!=0)
            for(ASTNode term : node.getChildren()){
                if (isBanned(term)){
                    node1=term.clone();
                    if (bannedNames.contains(node.getId())){
                        //node1.setId(term.getId()+"_a1");
                    	node1.setId(makeAdmissible(term.getId()));
                        term = node1;}
                }
            }
        
        else
            if (isBanned(node)){
                //node1.setId(node.getId()+"_a1");
            	node1.setId(makeAdmissible(node.getId()));
                node = node1;}
         
    return node;}
    
    public void addBannedName(String string){
        this.bannedNames.add(string);}

    public ArrayList<String> getBannedNames() {
        return bannedNames;
    }
    
    
    
}
