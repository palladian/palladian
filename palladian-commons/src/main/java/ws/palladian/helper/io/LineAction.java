package ws.palladian.helper.io;

public abstract class LineAction {

    protected boolean looping = true;
    public Object[] arguments = null;

    public LineAction() {
    };

    public LineAction(Object[] parameters) {
        arguments = parameters;
    };

    public abstract void performAction(String line, int lineNumber);

    public void breakLineLoop() {
        looping = false;
    }
}