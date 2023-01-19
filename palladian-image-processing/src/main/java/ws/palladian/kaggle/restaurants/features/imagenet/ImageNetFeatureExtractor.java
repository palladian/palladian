//package ws.palladian.kaggle.restaurants.features.imagenet;
//
//import static io.grpc.netty.NegotiationType.PLAINTEXT;
//
//import java.awt.image.BufferedImage;
//import java.io.BufferedReader;
//import java.io.Closeable;
//import java.io.File;
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.InputStreamReader;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.util.Objects;
//import java.util.concurrent.CountDownLatch;
//import java.util.concurrent.TimeUnit;
//
//import javax.imageio.ImageIO;
//
//import org.apache.commons.io.output.ByteArrayOutputStream;
//import org.apache.commons.lang3.StringUtils;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import com.google.protobuf.ByteString;
//
//import io.grpc.Channel;
//import io.grpc.netty.NettyChannelBuilder;
//import ws.palladian.core.FeatureVector;
//import ws.palladian.core.InstanceBuilder;
//import ws.palladian.kaggle.restaurants.features.FeatureExtractor;
//import ws.palladian.kaggle.restaurants.features.imagenet.ImageNetGrpc.ImageNetBlockingStub;
//import ws.palladian.kaggle.restaurants.features.imagenet.ImageNetProtos.Categories;
//import ws.palladian.kaggle.restaurants.features.imagenet.ImageNetProtos.Categories.Category;
//import ws.palladian.kaggle.restaurants.features.imagenet.ImageNetProtos.Image.Builder;
//import ws.palladian.kaggle.restaurants.utils.PortUtil;
//
///**
// * <p>
// * Provides access to the <a href=
// * "https://www.tensorflow.org/versions/master/tutorials/image_recognition/index.html">
// * Inception-v3 model</a> using
// * <a href="https://www.tensorflow.org">TensorFlow</a>. <i>Inception-v3 is
// * trained for the ImageNet Large Visual Recognition Challenge using the data
// * from 2012. This is a standard task in computer vision, where models try to
// * classify entire images into 1000 classes, like "Zebra", "Dalmatian", and
// * "Dishwasher".</i>
// * 
// * <p>
// * This extractor requires Python and TensorFlow
// * <a href="https://www.tensorflow.org/versions/r0.7/get_started/os_setup.html">
// * to be installed</a>. At the time of this writing, I was using TensorFlow
// * 0.7.1. Also, <tt>grpcio</tt> for Python must be installed (
// * <tt>pip install grpcio</tt>).
// * 
// * <p>
// * When both requisites are successfully installed, this extractor takes care of
// * starting the necessary Python process as server and communicates with this
// * server for feature extraction. In order to shut down the process after use,
// * make sure to call {@link #close()} when you're done.
// * 
// * <p>
// * For the Inception architecture see this paper:
// * <a href="http://arxiv.org/pdf/1512.00567v3.pdf">Rethinking the Inception
// * Architecture for Computer Vision</a>; Christian Szegedy, Vincent Vanhouck,
// * Sergey Ioffe, Jonathon Shlens, Zbigniew Wojn; 2015.
// * 
// * @author Philipp Katz
// */
//public class ImageNetFeatureExtractor implements FeatureExtractor, Closeable {
//
//	/** The logger for this class. */
//	private static final Logger LOGGER = LoggerFactory.getLogger(ImageNetFeatureExtractor.class);
//
//	/** A tensor containing the normalized prediction across 1000 labels. */
//	public static final String TENSOR_SOFTMAX = "softmax:0";
//
//	/**
//	 * A tensor containing the next-to-last layer containing 2048 float
//	 * description of the image.
//	 */
//	public static final String TENSOR_POOL_3 = "pool_3:0";
//
//	private static final String LOCALHOST = "localhost";
//
//	/** Name of the tensor which is used for extraction. */
//	private final String tensorName;
//
//	/** gRPC stub to access the Python server. */
//	private final ImageNetBlockingStub blockingStub;
//
//	/** The Process which represents the running Python server. */
//	private final Process process;
//
//	/** Indicates that {@link #close()} was invoked. */
//	private boolean closed = false;
//
//	/**
//	 * Create a new {@link ImageNetFeatureExtractor}.
//	 * 
//	 * @param pathToPython
//	 *            Path to the <tt>python</tt> executable.
//	 * @param pathToGraph
//	 *            Path to the GraphDef file.
//	 * @param tensorName
//	 *            Name of the tensor name to use (see {@link #TENSOR_SOFTMAX}
//	 *            and {@link #TENSOR_POOL_3}).
//	 */
//	public ImageNetFeatureExtractor(File pathToPython, File pathToGraph, String tensorName) {
//		Objects.requireNonNull(pathToPython, "pathToPython was null");
//		Objects.requireNonNull(pathToGraph, "pathToGraph was null");
//		Objects.requireNonNull(tensorName, "tensorName was null");
//		if (!pathToPython.isFile()) {
//			throw new IllegalArgumentException(pathToPython + " is not a file");
//		}
//		if (!pathToGraph.isFile()) {
//			throw new IllegalArgumentException(pathToGraph + " is not a file");
//		}
//		String pathToScript = copyPythonScriptsToTemp();
//		int port = PortUtil.getFreePort();
//		process = startServer(pathToPython.getAbsolutePath(), pathToScript, port, pathToGraph.getAbsolutePath(),
//				tensorName);
//		Channel channel = NettyChannelBuilder.forAddress(LOCALHOST, port).negotiationType(PLAINTEXT).build();
//		blockingStub = ImageNetGrpc.newBlockingStub(channel);
//		this.tensorName = tensorName;
//	}
//
//	/**
//	 * Copy the included Python scripts to a temporary directory, so that they
//	 * can be executed.
//	 * 
//	 * @return The full absolute path to the main script of the server.
//	 */
//	private static String copyPythonScriptsToTemp() {
//		try {
//			Path tempDirectory = Files.createTempDirectory("imagenet_server");
//			tempDirectory.toFile().deleteOnExit();
//			for (String file : new String[] { "/python/imagenet_server.py", "/python/imagenet_pb2.py" }) {
//				InputStream inputStream = ImageNetFeatureExtractor.class.getResourceAsStream(file);
//				Path destination = tempDirectory.resolve(file.substring(file.lastIndexOf('/') + 1));
//				Files.copy(inputStream, destination);
//				LOGGER.info("Copy {} to {}", file, destination);
//			}
//			return tempDirectory.resolve("imagenet_server.py").toAbsolutePath().toString();
//		} catch (IOException e) {
//			throw new IllegalStateException(
//					"Encountered IOException while copying Python scripts to temporary directory.", e);
//		}
//	}
//
//	/**
//	 * Start the Python server process and block further execution until the
//	 * server is running.
//	 * 
//	 * @param pythonPath
//	 *            Path to the python executable.
//	 * @param scriptPath
//	 *            Path to the server script.
//	 * @param port
//	 *            The port.
//	 * @param graphPath
//	 *            Path to graph model.
//	 * @param tensorName
//	 *            Name of the tensor to use.
//	 * @return The process which represents the running server.
//	 * @throws IllegalStateException
//	 *             in case anything goes wrong.
//	 */
//	private static Process startServer(String pythonPath, String scriptPath, int port, String graphPath,
//			String tensorName) {
//		// the script expects exactly three arguments: (1) port of the server,
//		// (2) path to graph model, (3) name of the tensor.
//		// the -u flag is used to disable output buffering from the python
//		// script, see here
//		// http://stackoverflow.com/questions/107705/disable-output-buffering
//		String[] command = { pythonPath, "-u", scriptPath, String.valueOf(port), graphPath, tensorName };
//		LOGGER.debug("Executing {}", StringUtils.join(command, ' '));
//		ProcessBuilder builder = new ProcessBuilder(command);
//		builder.redirectErrorStream(true);
//		Process process;
//		try {
//			process = builder.start();
//		} catch (IOException e) {
//			throw new IllegalStateException("IOException when trying to start the process", e);
//		}
//		BufferedReader outputReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
//		CountDownLatch awaitServerStart = new CountDownLatch(1);
//		new Thread() {
//			@Override
//			public void run() {
//				try {
//					String line;
//					while ((line = outputReader.readLine()) != null) {
//						// switch to DEBUG, after server is running
//						if (awaitServerStart.getCount() > 0) {
//							LOGGER.info("[output] {}", line);
//						} else {
//							LOGGER.debug("[output] {}", line);
//						}
//						if (line.startsWith("running on port")) {
//							awaitServerStart.countDown();
//						}
//					}
//				} catch (IOException e) {
//					throw new IllegalStateException("IOException while reading process output", e);
//				}
//			}
//		}.start();
//		try {
//			if (!awaitServerStart.await(1, TimeUnit.MINUTES)) {
//				throw new IllegalStateException("Start timeout reached.");
//			}
//		} catch (InterruptedException e) {
//			throw new IllegalStateException("Interrupted during wait for server start", e);
//		}
//		return process;
//	}
//
//	@Override
//	public FeatureVector extract(BufferedImage image) {
//		if (closed) {
//			throw new IllegalStateException("#close was already called.");
//		}
//		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//		try {
//			ImageIO.write(image, "jpg", outputStream);
//		} catch (IOException e) {
//			throw new IllegalStateException("Could not write image", e);
//		}
//		Builder builder = ImageNetProtos.Image.newBuilder();
//		builder.setData(ByteString.copyFrom(outputStream.toByteArray()));
//		Categories classificationResult = blockingStub.classify(builder.build());
//		InstanceBuilder instanceBuilder = new InstanceBuilder();
//		for (Category category : classificationResult.getCategoryList()) {
//			instanceBuilder.set(createFeatureName(category), category.getScore());
//		}
//		return instanceBuilder.create();
//	}
//
//	private String createFeatureName(Category category) {
//		return tensorName.replace(":0", "") + ":" + category.getNodeId();
//	}
//
//	@Override
//	public void close() throws IOException {
//		LOGGER.debug("Stopping the process");
//		process.destroy();
//		closed = true;
//	}
//
//}
