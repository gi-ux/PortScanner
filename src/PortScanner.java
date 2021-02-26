import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;


class PortScanner {
	final static List<Future<ScanResult>> futures = new ArrayList<>();

	public static void main(final String... args) throws InterruptedException, ExecutionException, IOException {
		ArrayList<String> ips = new ArrayList<>();

		final ExecutorService es = Executors.newFixedThreadPool(20);
		final int timeout = 200;

		IntStream.rangeClosed(1, 254).mapToObj(num -> "192.168.1." + num).parallel().filter((addr) -> {
			try {
				return InetAddress.getByName(addr).isReachable(2000);
			} catch (IOException e) {
				return false;
			}
		}).forEach(ips::add);
		String indirizzi = "Indirizzi: ";
		System.out.println(indirizzi);
		PrintWriter myWriter = new PrintWriter("PortScannerNogara.txt");
		myWriter.append(indirizzi);
		for (String s : ips) {
			System.out.println(s);
			myWriter.append("\n" + s);
		}
		for (String s : ips) {
			System.out.println("Scan delle porte dell'indirizzo: " + s);
			myWriter.append("\n" + "Scan delle porte (dalla 1 alla 1000) dell'indirizzo: " + s);
			scanPorte(s, es, timeout, myWriter);
		}
		myWriter.close();
		es.shutdown();
	}

	public static void scanPorte(String ip, ExecutorService es, int timeout, PrintWriter myWriter)
			throws InterruptedException, ExecutionException, IOException {

		for (int port = 1; port <= 1000; port++) {
			futures.add(portIsOpen(es, ip, port, timeout));
		}
		es.awaitTermination(200L, TimeUnit.MILLISECONDS);
		int openPorts = 0;
		for (final Future<ScanResult> f : futures) {
			if (f.get().isOpen()) {
				openPorts++;
				myWriter.append("\n" + f.get().getPort());
				System.out.println(f.get().getPort());
			}
		}
		System.out.println("There are " + openPorts + " open ports on host " + ip);
		myWriter.append("\n" + "There are " + openPorts + " open ports on host " + ip);
		futures.clear();
	}

	public static Future<ScanResult> portIsOpen(final ExecutorService es, final String ip, final int port,
			final int timeout) {
		return es.submit(new Callable<ScanResult>() {
			@Override
			public ScanResult call() {
				try {
					Socket socket = new Socket();
					socket.connect(new InetSocketAddress(ip, port), timeout);
					socket.close();
					return new ScanResult(port, true);
				} catch (Exception ex) {
					return new ScanResult(port, false);
				}
			}
		});
	}

	

	public static class ScanResult {
		private int port;

		private boolean isOpen;

		public ScanResult(int port, boolean isOpen) {
			super();
			this.port = port;
			this.isOpen = isOpen;
		}

		public int getPort() {
			return port;
		}

		public void setPort(int port) {
			this.port = port;
		}

		public boolean isOpen() {
			return isOpen;
		}

		public void setOpen(boolean isOpen) {
			this.isOpen = isOpen;
		}

	}
}