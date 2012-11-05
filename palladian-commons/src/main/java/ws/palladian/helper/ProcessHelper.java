package ws.palladian.helper;

import java.io.IOException;
import java.io.InputStream;

import org.apache.log4j.Logger;

import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.StringOutputStream;

/**
 * This class should provide convenience methods for interacting with the OS functionality.
 * 
 * @author David Urbansky
 * 
 */
public class ProcessHelper {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(ProcessHelper.class);

    /**
     * Run a command on the console/terminal.
     * 
     * @param consoleCommand The command to run.
     * @return The console output that was read after executing the command.
     */
    public static String runCommand(String consoleCommand) {
        StringBuilder result = new StringBuilder();

        StringOutputStream stringOutputStream = new StringOutputStream();
        Process p = null;
        InputStream in = null;
        try {
            p = Runtime.getRuntime().exec(consoleCommand);
            in = p.getInputStream();
            byte[] buffer = new byte[4096];

            int n = 0;
            while ((n = in.read(buffer)) != -1) {
                stringOutputStream.write(buffer, 0, n);
            }

            result.append(stringOutputStream.toString());

        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        } finally {
            FileHelper.close(in, stringOutputStream);
            if (p != null) {
                p.destroy();
            }
        }

        return result.toString();
    }

}
