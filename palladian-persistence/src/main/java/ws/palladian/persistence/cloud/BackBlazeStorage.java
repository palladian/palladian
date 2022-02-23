package ws.palladian.persistence.cloud;

import com.backblaze.b2.client.B2ListFilesIterable;
import com.backblaze.b2.client.B2StorageClient;
import com.backblaze.b2.client.contentSources.B2ContentSource;
import com.backblaze.b2.client.contentSources.B2ContentTypes;
import com.backblaze.b2.client.contentSources.B2FileContentSource;
import com.backblaze.b2.client.exceptions.B2Exception;
import com.backblaze.b2.client.structures.B2FileVersion;
import com.backblaze.b2.client.structures.B2ListFileVersionsRequest;
import com.backblaze.b2.client.structures.B2UploadFileRequest;
import com.backblaze.b2.client.webApiHttpClient.B2StorageHttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.io.FileHelper;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Simplified API to upload files to Backblaze
 * Usage: add a file to the upload queue BackblazeStorage.getInstance(config).upload(...)
 * close all current connections BackblazeStorage.getInstance(config).closeAllSafely()
 *
 * @author Felix Pistorius, David Urbansky
 * @since 07.11.2017
 */
public class BackBlazeStorage {
    /**
     * The logger for this class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(BackBlazeStorage.class);

    private final BackBlazeConfig config;

    /** Thread pool to run UploadConnections */
    private final ThreadPoolExecutor executor;

    /** Available backblaze connections to upload a file */
    private final Queue<ClientConnection> clientConnections = new ConcurrentLinkedQueue<>();

    /** Open upload tasks */
    private final Queue<UploadTask> uploadTaskQueue = new ConcurrentLinkedQueue<>();

    /** Open delete tasks */
    private final Queue<DeleteTask> deleteTaskQueue = new ConcurrentLinkedQueue<>();

    /** Flag for upload listener thread */
    private boolean running = true;

    public interface UploadCallback {
        void callback(boolean success, String errorMessage);
    }

    abstract static class Task {
        // will be called after the upload
        UploadCallback callback;

        protected abstract void execute(B2StorageClient client);

        protected void callback(boolean success, String errorMessage) {
            if (callback != null) {
                callback.callback(success, errorMessage);
            }
        }
    }

    /**
     * Describes an upload
     */
    private class UploadTask extends Task {
        // name of the file on Backblaze
        String fileName;

        // the source file on the local storage
        File sourceFile;

        @Override
        public void execute(B2StorageClient client) {
            if (fileName == null && sourceFile == null || !sourceFile.exists()) {
                callback(false, "Invalid UploadTask for: " + fileName);
                return;
            }

            B2ContentSource source = B2FileContentSource.build(sourceFile);

            // B2UploadListener uploadListener = new B2UploadListener() {
            // @Override
            // public void progress(B2UploadProgress progress) {
            // final double percent = (100. * (progress.getBytesSoFar() / (double) progress.getLength()));
            // System.out.printf("%s - progress(%03.2f, %s) : %s\n", fileName, percent, progress.toString(), stopWatch.getElapsedTimeString());
            // }
            // };

            B2UploadFileRequest request = B2UploadFileRequest.builder(config.getBucketId(), fileName, B2ContentTypes.B2_AUTO, source)
                    // .setListener(uploadListener)
                    .build();

            String errorMessage = null;
            try {
                LOGGER.info("upload file: " + fileName);
                client.uploadSmallFile(request);
            } catch (B2Exception e) {
                e.printStackTrace();
                errorMessage = e.getMessage();
            } finally {
                callback(errorMessage == null, errorMessage);
            }
        }
    }

    private static class DeleteTask extends Task {
        private Collection<B2FileVersion> toDelete;

        @Override
        public void execute(B2StorageClient client) {
            String errorMessage = null;
            for (B2FileVersion b2FileVersion : toDelete) {
                try {
                    // delete file version
                    client.deleteFileVersion(b2FileVersion.getFileName(), b2FileVersion.getFileId());
                } catch (B2Exception e) {
                    e.printStackTrace();

                    if (errorMessage == null) {
                        errorMessage = e.getMessage();
                    } else {
                        errorMessage += "\n" + e.getMessage();
                    }
                }
            }

            callback(errorMessage == null, errorMessage);
        }

        public void setToDelete(Collection<B2FileVersion> toDelete) {
            this.toDelete = toDelete;
        }
    }

    private static final Map<BackBlazeConfig, BackBlazeStorage> instances = new HashMap<>();

    public static BackBlazeStorage getInstance(BackBlazeConfig config) {
        BackBlazeStorage instance = instances.get(config);
        if (instance == null) {
            instance = new BackBlazeStorage(config);
            instances.put(config, instance);
        }

        return instance;
    }

    private BackBlazeStorage(BackBlazeConfig config) {
        this.config = config;

        int numberOfUploadConnections = config.getNumberOfThreads();
        executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(numberOfUploadConnections);
        for (int i = 0; i < numberOfUploadConnections; i++) {
            clientConnections.add(new ClientConnection("BackblazeStorage-" + i));
        }

        // upload listener thread
        new Thread() {
            @Override
            public void run() {
                while (running) {
                    Task poll = uploadTaskQueue.poll();
                    if (poll == null) {
                        poll = deleteTaskQueue.poll();
                    }

                    if (poll != null) {
                        ClientConnection availableConnection = getAvailableConnection();
                        availableConnection.setTask(poll);
                        executor.execute(availableConnection);
                    } else {
                        deepSleep(50);
                    }
                }
                // we reach this point if running = false
                interrupt();
            }
        }.start();
    }

    private ClientConnection getAvailableConnection() {
        ClientConnection poll = clientConnections.poll();
        while (poll == null) {
            deepSleep(15);
            poll = clientConnections.poll();
        }
        return poll;
    }

    /**
     * Add a file to the upload queue and delete it locally once the upload is complete.
     */
    public void upload(File file) {
        upload(file.getName(), file, null);
    }

    public boolean uploadIfChanged(File file) {
        boolean uploadFileBecauseThingsChanged = true;
        Collection<B2FileVersion> versions = getVersions(file.getName());
        String newSha1 = FileHelper.getSha1(file);
        for (B2FileVersion version : versions) {
            if (version.getContentSha1().equals(newSha1)) {
                uploadFileBecauseThingsChanged = false;
                break;
            }
        }
        if (uploadFileBecauseThingsChanged) {
            upload(file.getName(), file, null);
        }

        return uploadFileBecauseThingsChanged;
    }

    /**
     * Add a file to the upload queue and delete it locally once the upload is complete.
     */
    public void uploadAndDelete(File file) {
        BackBlazeStorage.UploadCallback uploadCallback = (state, errorMessage) -> FileHelper.delete(file);
        upload(file.getName(), file, uploadCallback);
    }

    /**
     * Add a file to the upload queue
     *
     * @param fileName   name of the file on backblaze
     * @param sourceFile the source file which will be uploaded
     * @param callback   UpdateCallback which will be called after the upload
     */
    public void upload(String fileName, File sourceFile, UploadCallback callback) {
        UploadTask uploadTask = new UploadTask();
        uploadTask.fileName = fileName;
        uploadTask.sourceFile = sourceFile;
        uploadTask.callback = callback;

        uploadTaskQueue.add(uploadTask);
    }

    public void delete(String fileName) {
        delete(fileName, null);
    }

    public void delete(String fileName, UploadCallback callback) {
        Collection<B2FileVersion> currentVersions = getVersions(fileName);
        delete(currentVersions, callback);
    }

    public void delete(Collection<B2FileVersion> b2FileVersions) {
        delete(b2FileVersions, null);
    }

    public void delete(Collection<B2FileVersion> b2FileVersions, UploadCallback callback) {
        DeleteTask deleteTask = new DeleteTask();
        deleteTask.callback = callback;
        deleteTask.setToDelete(b2FileVersions);

        deleteTaskQueue.add(deleteTask);
    }

    public int getNumberOfWaitingUploads() {
        return uploadTaskQueue.size();
    }

    /**
     * Close all connections and stop the upload listener thread
     */
    public void closeAll() {
        running = false;

        // close all connections
        ClientConnection poll = clientConnections.poll();
        while (poll != null) {
            poll = clientConnections.poll();
        }

        executor.shutdown();
        instances.remove(config);
    }

    /**
     * Wait until the upload task queue are empty and then call closeAll()
     *
     * @see BackBlazeStorage#closeAll()
     */
    public void closeAllSafely() {
        int size = getNumberOfWaitingUploads();
        while (size != 0) {
            deepSleep(1000);
            size = getNumberOfWaitingUploads();
        }
        closeAll();
    }

    /**
     * Hold a backblaze client and upload a given UpdateTask
     */
    private class ClientConnection implements Runnable {
        private final String userAgent;

        private Task task;

        public ClientConnection(String userAgent) {
            this.userAgent = userAgent;
        }

        public void setTask(Task task) {
            this.task = task;
        }

        /**
         * upload the given task
         */
        public void run() {
            if (task != null) {
                B2StorageClient client = B2StorageHttpClientBuilder.builder(config.getAccountId(), config.getApplicationKey(), userAgent).build();

                task.execute(client);

                try {
                    if (client != null) {
                        client.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            clientConnections.add(this);
        }
    }

    /**
     * Get the last file versions by the given prefix of the file name
     *
     * @param filePrefix name of the file to delete
     * @return file or null
     */
    public Collection<B2FileVersion> getVersions(String filePrefix) {
        B2ListFileVersionsRequest listFileVersionsRequest = B2ListFileVersionsRequest.builder(config.getBucketId()).setPrefix(filePrefix).build();

        Collection<B2FileVersion> result = new ArrayList<>();

        try {
            // get all file versions
            B2StorageClient client = B2StorageHttpClientBuilder.builder(config.getAccountId(), config.getApplicationKey(), "version agent").build();
            B2ListFilesIterable b2FileVersions = client.fileVersions(listFileVersionsRequest);
            if (b2FileVersions != null) {
                for (B2FileVersion b2FileVersion : b2FileVersions) {
                    result.add(b2FileVersion);
                }
            }

            client.close();
        } catch (B2Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    /**
     * Get the http status code of a given URL
     *
     * @param url requested url
     * @return status code or -1 if an error occurs
     */
    public static int getStatusCode(String url) {
        if (url == null) {
            return -1;
        }

        HttpURLConnection connection = null;
        try {
            URL urlObj = new URL(url);
            connection = (HttpURLConnection) urlObj.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();

            return connection.getResponseCode();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                try {
                    connection.disconnect();
                } catch (Exception e) {
                    // ccl
                }
            }
        }

        return -1;
    }

    private void deepSleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        BackBlazeConfig config = new BackBlazeConfig("", "", "", 10);
        BackBlazeStorage storage = BackBlazeStorage.getInstance(config);

        StopWatch stopWatch = new StopWatch();
        File[] files = FileHelper.getFiles("path-to-files", ".jpg");

        // upload files
        //                for (File file : files) {
        //                    storage.upload(file);
        //                }
        //        System.out.println(stopWatch.getElapsedTimeString());

        // upload only if content in the files changed (different sha1)
        for (File file : files) {
            boolean uploaded = storage.uploadIfChanged(file);
            System.out.println(file.getName() + " uploaded: " + uploaded);
        }

        // find by name and check with local versions
        //        for (File file : files) {
        //            Collection<B2FileVersion> currentVersions = storage.getVersions(file.getName());
        //            for (B2FileVersion currentVersion : currentVersions) {
        //                System.out.println(currentVersion.getContentSha1());
        //                System.out.println(FileHelper.getSha1(file));
        //            }
        //        }

        // delete files
        //        for (File file : files) {
        //            storage.delete(file.getName());
        //        }

        //        storage.closeAllSafely();
    }
}
