package ws.palladian.kaggle.restaurants.utils;

import java.io.IOException;
import java.net.ServerSocket;

public final class PortUtil {

	private PortUtil() {
		// no op.
	}

	/**
	 * @return An unused port on the current machine.
	 */
	public static final int getFreePort() {
		try (ServerSocket socket = new ServerSocket(0);) {
			return socket.getLocalPort();
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

}
