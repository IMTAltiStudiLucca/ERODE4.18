package it.imt.erode.commandline;

import java.io.BufferedWriter;
import java.io.IOException;

import org.eclipse.ui.console.MessageConsoleStream;

import com.microsoft.z3.Z3Exception;

import it.imt.erode.importing.UnsupportedFormatException;
import it.imt.erode.simulation.output.IDataOutputHandler;

public interface ICommandLine {

	void executeCommands(boolean print, MessageConsoleStream out, BufferedWriter bwOut) throws IOException, InterruptedException, UnsupportedFormatException, Z3Exception;

	void setDataOutputHandler(IDataOutputHandler dog);

	void setMessageDialogShower(IMessageDialogShower msgShower);

	void setTerminator(Terminator terminator);

}
