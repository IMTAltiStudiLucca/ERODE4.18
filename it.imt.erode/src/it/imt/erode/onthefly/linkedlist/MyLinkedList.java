package it.imt.erode.onthefly.linkedlist;

import java.util.HashMap;

public class MyLinkedList<Data> {
	
	private Node<Data> headNode;
	private Node<Data> tailNode;
	private Node<Data> currentNode;
	private int size;
	private boolean pointsBeforeHead;
	
	//To get fast remove
	private boolean noReplicasAndDataImplementsEqualsAndHashCode=false;
	private HashMap<Data, Node<Data>> dataToNode;

	/*
	 * Generates an empty list 
	 */
	public MyLinkedList(boolean noReplicasAndDataImplementsEqualsAndHashCode) {
		super();
		this.noReplicasAndDataImplementsEqualsAndHashCode=noReplicasAndDataImplementsEqualsAndHashCode;
		if(noReplicasAndDataImplementsEqualsAndHashCode) {
			dataToNode=new HashMap<>();
		}
		reset();
		size=0;
	}
		
	public MyLinkedList(Node<Data> head,Node<Data> tail, int size,boolean noReplicasAndDataImplementsEqualsAndHashCode) {
		this(noReplicasAndDataImplementsEqualsAndHashCode);
		this.headNode=head;
		this.tailNode=tail;
		this.size=size;		
		//checkConsistency("end of Partition(IBlock headBlock,IBlock tailBlock, int size)");
	}
	
	public MyLinkedList(Node<Data> node,boolean noReplicasAndDataImplementsEqualsAndHashCode) {
		this(node,node,1,noReplicasAndDataImplementsEqualsAndHashCode);
	}
	
	public MyLinkedList(Node<Data> head,Node<Data> tail,boolean noReplicasAndDataImplementsEqualsAndHashCode) {
		this(head,tail,2,noReplicasAndDataImplementsEqualsAndHashCode);
	}
	
	public int size() {
		return size;
	}
	public boolean isEmpty() {
		return size==0;
	}

//	public Node<Data> getFirstNode() {
//		return headNode;
//	}
//	
//	public Node<Data> getLastNode() {
//		return tailNode;
//	}
	
	public Node<Data> getCurrentNode() {
		return currentNode;
	}
	public Data getCurrent() {
		return (currentNode==null)?null:currentNode.getData();
	}
	/**
	 * Updates currentNode to the next node, and returns the data stored in it
	 * @return
	 */
	public Data moveToNextNode() {
		if(pointsBeforeHead) {
			currentNode=headNode;
			pointsBeforeHead=false;
		}
		else if (currentNode!=null) {
			currentNode=currentNode.getNext();
		}
		
		if(currentNode!=null) {
			return currentNode.getData();
		}
		return null;
	}
	public boolean hasNext() {
		if(pointsBeforeHead) {
			return headNode!=null;
		}
		else{
			if(currentNode==null) {
				return false;
			}
			return currentNode.getNext()!=null;
		}
	}
	public void reset() {
		currentNode=null;
		pointsBeforeHead = true;
	}
	
	
	/**
	 * 
	 * @param data
	 * @return true if the boolean flag is set to true, and no previous copy of this pair was already present. Returns always true if the flag is set to false
	 */
	public boolean add(Data data) {
		Node<Data> node = new Node<Data>(data);
		return add(node);
	}
	
	/**
	 * 
	 * @param node
	 * @return true if the boolean flag is set to true, and no previous copy of this pair was already present. Returns always true if the flag is set to false
	 */
	public boolean add(Node<Data> node) {
		//checkConsistency("begin of add(IBlock block)");
		if(noReplicasAndDataImplementsEqualsAndHashCode) {
			if(dataToNode.containsKey(node.getData())) {
				//I don't add replicas!
				return false;
			}
			else {
				dataToNode.put(node.getData(), node);
			}
//			Node<Data> prev = dataToNode.put(node.getData(), node);
//			boolean added= prev==null;
//			if(!added) {
//				//I don't add replicas
//				node.setPrev(prev.getPrev());
//				node.setNext(prev.getNext());
//				cleanNodePointers(prev);
//				if(currentNode==prev) {
//					currentNode=node;
//				}
//				if(tailNode==prev) {
//					tailNode=node;
//				}
//				if(headNode==prev) {
//					headNode=node;
//				}
//				return false;
//			}
		}
		
		//Special case: this is the first node I insert
		if(size==0){
			headNode=node;
			tailNode=node;
		}
		else{
			tailNode.setNext(node);
			node.setPrev(tailNode);
			tailNode=node;
		}
		size++;
		
		//checkConsistency("end of add(IBlock block)");
		return true;
	}
	
	public boolean contains(Data data) {
		if(noReplicasAndDataImplementsEqualsAndHashCode) {
			Node<Data> node = dataToNode.get(data);
			return node!=null;
		}
		else {
			Node<Data> c = headNode;
			while(c!=null) {
				if(c.getData().equals(data)) {
					return true;
				}
				c=c.getNext();
			}
			return false;
		}
		
	}
	
	private void addBetween(Node<Data> prev, Node<Data> next, Node<Data> nodeToAdd) {
		//checkConsistency("begin of addBetween(IBlock prev, IBlock next, IBlock blockToAdd)");
		
		if(noReplicasAndDataImplementsEqualsAndHashCode) {
			dataToNode.put(nodeToAdd.getData(), nodeToAdd);
		}
		
		if(prev!=null){
			prev.setNext(nodeToAdd);
		}
		nodeToAdd.setPrev(prev);
		if(next!=null){
			next.setPrev(nodeToAdd);
		}
		nodeToAdd.setNext(next);
		
		if(headNode==next){
			headNode=nodeToAdd;
		}
		if(tailNode==prev){
			tailNode=nodeToAdd;
		}
		size++;
		
		//checkConsistency("end of addBetween(IBlock prev, IBlock next, IBlock blockToAdd)");
	}
	
	/**
	 * This method is an O(n) implementation of remove if userGuaranteesNoReplicasWillBeAdded=false.   
	 * 		We start from head, and we scan all nodes until we find one with the given data. We remove such node using the other remove.
	 * Otherwise we use an internal data hashmap for efficiently finding the node to remove
	 * @param data
	 * @return true if a node was actually removed, or false otherwise
	 */
	public boolean remove(Data data) {
		if(noReplicasAndDataImplementsEqualsAndHashCode) {
			Node<Data> toRemove = dataToNode.get(data);
			return remove(toRemove);
		}
		else {
			Node<Data> c = headNode;
			while(c!=null) {
				if(c.getData().equals(data)) {
					remove(c);
					return true;
				}
				c=c.getNext();
			}
		}
		return false;
	}
	
	/**
	 * This is an efficient implementation of remove that should be used only if you have a reference of the node
	 * you want to delete. YOU SHOULD NOT CREATE A NEW NODE TO USE IT.
	 * If the node removed is the one pointed by currentNode, we modify the pointer so to point to the previous one. In this way a 'next' will point to the 'real next node', without risking to skip it.  
	 * 
	 * @param nodeFromListToRemove
	 * @return true if a node was actually removed, or false otherwise
	 */
	public boolean remove(Node<Data> nodeFromListToRemove) {
		//checkConsistency("begin of remove(IBlock block)")

		if(nodeFromListToRemove==null) {
			return false;
		}
		if(noReplicasAndDataImplementsEqualsAndHashCode) {
			dataToNode.remove(nodeFromListToRemove.getData());
		}
		
		//Special case: empty partition
		if(size==0){
			return false;
		}
		
		//Handle the case in which we remove currentNode
		if(nodeFromListToRemove.equals(currentNode)) {
			if(nodeFromListToRemove.equals(headNode)) {
				//We remove the head node. We should point 'before it'
				reset();
			}
			else {
				//We remove an intermediate or last node. We should point to the previous one
				currentNode=currentNode.getPrev();
			}
		}

		//Special case: one node only: headNode = tailNode
		if(size==1){
			if(headNode.equals(nodeFromListToRemove)){
				headNode=null;
				tailNode=null;
				size=0;
				cleanNodePointers(nodeFromListToRemove);
				return true;
			}
			else{
				//CRNReducerCommandLine.println("Problema: 1 blocco solo, ma quello che voglio rimuovere non e' uguale a head");
				return false;
			}
		}
		else{

			//I have at least two nodes: I want to remove either the head, or the tail, or an intermediate node  

			//Special case: I want to remove the head
			if(headNode.equals(nodeFromListToRemove)){
				headNode=headNode.getNext();
				headNode.setPrev(null);
				cleanNodePointers(nodeFromListToRemove);
				size--;
				//checkConsistency("end of remove(IBlock block)");
				return true;
			}

			//Special case: I want to remove the tail, and I have at least two nodes, otherwise it was also the head, and I managed it in the previous if 
			if(tailNode.equals(nodeFromListToRemove)){
				tailNode=tailNode.getPrev();
				tailNode.setNext(null);
				cleanNodePointers(nodeFromListToRemove);
				size--;

				//checkConsistency("end of remove(IBlock block)");
				return true;
			}

			//Normal case: I remove a node which is neither head nor tail
			Node<Data> prev = nodeFromListToRemove.getPrev();
			Node<Data> next = nodeFromListToRemove.getNext();

			prev.setNext(next);
			next.setPrev(prev);
			cleanNodePointers(nodeFromListToRemove);
			size--;

			//checkConsistency("end of remove(IBlock block)");

			return true;
		}
	}
	
	public void moveAtEnd(Data v) {
		remove(v);
		add(v);
	}
	
	private void cleanNodePointers(Node<Data> nodeToRemove){
		nodeToRemove.setNext(null);
		nodeToRemove.setPrev(null);
	}

	public void substituteAndDecreaseSize(Node<Data> alias, Node<Data> toBeRemoved) {

		//checkConsistency("begin of substituteAndDecreaseSize(IBlock aliasBlock, IBlock toBeRemoved)");
		if(alias.equals(toBeRemoved)){
			//CRNReducerCommandLine.println("You want to substitute a node with itself. I skip this");
			return;
		}

		if(size<=1){
			//CRNReducerCommandLine.println("You want to substitute a node when size="+size+". I skip this");
			return;
		}
		
		//size>1
		if(size==2){
			//Then I have that one is head. I  just remove the one to be removed
			remove(toBeRemoved);
			return;
		}
		
		if(currentNode!=null && currentNode.equals(toBeRemoved)) {
			currentNode=alias;
		}

		//special case: size >2, and TBR points to alias
		if(toBeRemoved.getNext()!=null && toBeRemoved.getNext().equals(alias)){
			Node<Data> prevOfTBR = toBeRemoved.getPrev();
			if(prevOfTBR!=null){
				prevOfTBR.setNext(alias);
			}
			alias.setPrev(prevOfTBR);
			size--;
			if(headNode.equals(toBeRemoved)){
				headNode=alias;
			}
			if(tailNode.equals(toBeRemoved)){
				tailNode=alias;
			}
			cleanNodePointers(toBeRemoved);
		}
		else{
			//special case: size >2, and AB points to TBR
			if(toBeRemoved.getPrev()!=null && toBeRemoved.getPrev().equals(alias)){
				Node<Data> nextOfTBR = toBeRemoved.getNext();
				if(nextOfTBR!=null){
					nextOfTBR.setPrev(alias);
				}
				alias.setNext(nextOfTBR);
				size--;
				if(headNode.equals(toBeRemoved)){
					headNode=alias;
				}
				if(tailNode.equals(toBeRemoved)){
					tailNode=alias;
				}
				cleanNodePointers(toBeRemoved);
			}
			//normal case: size >2, and TBR and AB do not point each other/
			else{
				Node<Data> prevOfTBR = toBeRemoved.getPrev();
				Node<Data> nextOfTBR = toBeRemoved.getNext();
				remove(toBeRemoved);
				remove(alias);
				addBetween(prevOfTBR,nextOfTBR,alias);
			}
		}
		//checkConsistency("end of substituteAndDecreaseSize(IBlock aliasBlock, IBlock toBeRemoved)");
	}
		
	

	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append("The list has ");
		sb.append(size);
		if(size==1){
			sb.append(" node:\n");
		}
		else{
			sb.append(" nodes:\n");
		}
		if(currentNode!=null) {
			sb.append("Current node is: ");
			sb.append(currentNode.toString());
			sb.append("\n");
		}
		
		Node<Data> current = headNode;
		int b=1;
		while(current!=null){
			sb.append("Node "+b+", ");
			b++;
			sb.append(current.toString());
			sb.append("\n");
			current=current.getNext();
		}
		return sb.toString();
	}
	
	private boolean checkConsistency(){
		
		int realSize = 0;
		Node<Data> currentNode = headNode;
		while(currentNode!=null){
			realSize++;
			currentNode=currentNode.getNext();
		}
		if(size!=realSize){
			//CRNReducerCommandLine.println("Problema: size="+size+", realSize="+realSize);
			return false;
		}
	
		
		if(size==0){
			if(headNode != null){
				//CRNReducerCommandLine.println("Problema: size=0, ma head non e' nullo");
				return false;
			}
			if(tailNode != null){
				//CRNReducerCommandLine.println("Problema: size=0, ma tail non e' nullo");
				return false;
			}
		}
		//size>0
		else{
			if(headNode == null){
				//CRNReducerCommandLine.println("Problema: size>0, ma  head e' nullo");
				return false;
			}
			if(tailNode == null){
				//CRNReducerCommandLine.println("Problema: size>0, ma tail e' nullo");
				return false;
			}
			if(headNode.getPrev() != null){
				//CRNReducerCommandLine.println("Problema: prev di head non e' nullo");
				return false;
			}
			if(tailNode.getNext() != null){
				//CRNReducerCommandLine.println("Problema: next di tail non e' nullo");
				return false;
			}
			if(size==1){
				if(!headNode.equals(tailNode)){
					//CRNReducerCommandLine.println("Problema: size =1, ma head =/= tail");
					return false;
				}
			}
			if(size > 1){
				if(headNode.equals(tailNode)){
					//CRNReducerCommandLine.println("Problema: size >1, ma head = tail");
					return false;
				}
			}
		}
		return true;
	}
	
	@SuppressWarnings("unused")
	private boolean checkConsistency(String message){
		if(!checkConsistency()){
			//CRNReducerCommandLine.println(message);
			System.exit(-1);
		}
		return true;
	}

}
