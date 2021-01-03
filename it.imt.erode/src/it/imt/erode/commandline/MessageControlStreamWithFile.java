package it.imt.erode.commandline;

import java.io.BufferedWriter;
import java.io.IOException;

import org.eclipse.ui.console.MessageConsoleStream;

public class MessageControlStreamWithFile {

	private MessageConsoleStream out;
	private BufferedWriter bw;

	public MessageControlStreamWithFile(MessageConsoleStream out, BufferedWriter bw) {
		this.out=out;
		this.bw=bw;
	}
	
	public MessageControlStreamWithFile(MessageConsoleStream out) {
		this.out=out;
	}
	
	public void print(String message){
		if(out!=null){
			out.print(message);
		}
		if(bw!=null){
			try {
				bw.write(message);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
