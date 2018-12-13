package cj.test.website.testnet;

import cj.studio.ecm.net.Circuit;
import cj.studio.ecm.net.CircuitException;
import cj.studio.ecm.net.Frame;
import cj.studio.ecm.net.IInputChannel;
import cj.studio.ecm.net.io.MemoryContentReciever;
import cj.studio.ecm.net.io.MemoryInputChannel;
import cj.studio.ecm.net.io.MemoryOutputChannel;
import cj.studio.ecm.net.io.SimpleInputChannel;

public class TestNet {

	public static void main(String[] args) throws CircuitException {
//		testFrame();
		testMemoryFrame();
//		testCircuit();
//		testSyncCircuit();
	}

	static void testSyncCircuit() {
		MemoryOutputChannel output = new MemoryOutputChannel();
		Circuit circuit = new Circuit(output, "net/1.0 200 ok");
		circuit.content().beginWait();
		new Thread(new Runnable() {

			@Override
			public void run() {
				for (int i = 0; i < 1; i++) {
					byte[] b = ("-----" + i + "\r\n").getBytes();
					circuit.content().writeBytes(b, 0, b.length);
				}
				byte[] b = new byte[] { 'a', 'b' };
				circuit.content().writeBytes(b, 0, b.length);

//				circuit.content().flush();
				circuit.content().close();
			}
		}).start();

		circuit.content().waitClose();

		byte[] b = output.readFully();

		System.out.println(new String(b) + "\r\n" + (circuit.content().writedBytes()) + "bytes");

	}

	static void testCircuit() {
		MemoryOutputChannel output = new MemoryOutputChannel();
		Circuit circuit = new Circuit(output, "net/1.0 200 ok");
		for (int i = 0; i < 100000; i++) {
			byte[] b = ("-----" + i + "\r\n").getBytes();
			circuit.content().writeBytes(b, 0, b.length);
		}
		byte[] b = new byte[] { 'a', 'b' };
		circuit.content().writeBytes(b, 0, b.length);

		circuit.content().flush();
		circuit.content().close();

		b = output.readFully();

		System.out.println(new String(b) + "\r\n" + (circuit.content().writedBytes()) + "bytes");

	}

	static void testFrame() throws CircuitException {
		IInputChannel input = new SimpleInputChannel();
		Frame frame = new Frame(input, "get /website/?type=a net/1.0");

		MemoryContentReciever reciever = new MemoryContentReciever();
		frame.content().accept(reciever);

		input.begin(null);
		for (int i = 0; i < 1000000; i++) {
			byte[] b = ("-----" + i + "\r\n").getBytes();
			input.writeBytes(b, 0, b.length);
		}
		input.flush();
		byte[] b = new byte[] { 'a', 'b' };
		input.done(b, 0, b.length);

		b = reciever.readFully();
		System.out.println(new String(b) + "\r\n" + (frame.content().revcievedBytes() / 1024F / 1024F) + "mb");
	}
	static void testMemoryFrame() throws CircuitException {
		IInputChannel input = new MemoryInputChannel(8192);
		Frame frame = new Frame(input, "get /website/?type=a net/1.0");

		MemoryContentReciever reciever = new MemoryContentReciever();
		frame.content().accept(reciever);

		input.begin(null);
		for (int i = 0; i < 1000000; i++) {
			byte[] b = ("-----" + i + "\r\n").getBytes();
			input.writeBytes(b, 0, b.length);
		}
		input.flush();
		byte[] b = new byte[] { 'a', 'b' };
		input.done(b, 0, b.length);

		b = reciever.readFully();
		System.out.println(new String(b) + "\r\n" + (frame.content().revcievedBytes() / 1024F / 1024F) + "mb");
	}
}
