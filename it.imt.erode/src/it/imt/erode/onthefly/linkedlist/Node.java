package it.imt.erode.onthefly.linkedlist;

public class Node<Data> {

	private Node<Data> next;
	private Node<Data> prev;	
	
	private Data data;
	
	public Node(Data data){
		this.data=data;
	}
	
	public Node<Data> getPrev() {
		return prev;
	}

	public Node<Data> getNext() {
		return next;
	}
	
	public Data getData() {
		return data;
	}

	public void setPrev(Node<Data> node) {
		this.prev = node;
	}

	public void setNext(Node<Data> node) {
		this.next = node;
	}
	
	public void setData(Data data) {
		this.data = data;
	}
	
	@Override
	public String toString() {
		return data.toString();
	}	
	

}
