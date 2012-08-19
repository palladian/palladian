package ws.palladian.helper.io;

public abstract class LineAction {

    protected boolean looping = true;
    public final Object[] arguments;

    /**
     * <p>
     * Create a new {@link LineAction}.
     * </p>
     */
    public LineAction() {
        arguments = null;
    };

    /**
     * <p>
     * Create a new {@link LineAction} and pass parameters to be used inside the loop.
     * </p>
     * 
     * @param parameters
     */
    public LineAction(Object[] parameters) {
        arguments = parameters;
    };

    /**
     * <p>
     * The action to perform for every line. To cancel the loop, invoke {@link #breakLineLoop()}.
     * </p>
     * 
     * @param line The string content of the line.
     * @param lineNumber The number of the line, starting with <code>0</code>.
     */
    public abstract void performAction(String line, int lineNumber);

    /**
     * <p>
     * Break the loop, before the whole iteration has ended. To be called from {@link #performAction(String, int)}.
     * </p>
     */
    public void breakLineLoop() {
        looping = false;
    }
}