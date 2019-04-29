package logging;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Utility class for debug logs.
 */
public final class Logger {
	private static final Queue<Runnable> TASKS = new ConcurrentLinkedQueue<>();
	private static final Thread EXECUTOR = new Thread(() -> {
		while (!Thread.currentThread().isInterrupted()) {
			try {
				while (TASKS.peek() == null)
					Thread.onSpinWait();
				TASKS.poll().run();
			}
//			catch (InterruptedException e) {
//				Thread.currentThread().interrupt();
//				running = false;
//			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
	});
	static {
		EXECUTOR.setDaemon(true);
		EXECUTOR.setName("Logger");
		EXECUTOR.start();
	}


	private static final Queue<OutputStreamWriter> writers = new ConcurrentLinkedQueue<>();


	public static void addOutput(final String fileName) {
		try {
			writers.add(
					new OutputStreamWriter(
							Files.newOutputStream(Path.of(fileName))
					)
			);
		}
		catch (IOException e) {
			System.err.println("IOException while preparing log file: " + fileName);
			e.printStackTrace();
		}
	}

	public static void addOutput(final OutputStream stream) {
		writers.add(new OutputStreamWriter(stream));
	}


	public static void log(final String message) {
		TASKS.offer(() -> {
			try {
				for (final var writer : writers) {
					writer.append(message);
					writer.flush();
				}
			}
			catch (IOException e) {
				System.err.println("IOException while logging");
				e.printStackTrace();
			}
		});
	}

	/**
	 * Logs the message and appends a new line.
	 */
	public static void logln(final String message) {
		log(message + System.lineSeparator());
	}

	public static void logf(final String message, final Object... args) {
		log(String.format(message, args));
	}

}
