package cj.studio.gateway.mic;

import java.io.PrintWriter;

public interface IHelpFormatter {

	void printHelp(PrintWriter pw, MicCommand cmd);

}
