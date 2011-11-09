//package de.philippkatz.activities.wikipedia;
//
//import f00f.net.irc.martyr.GenericAutoService;
//import f00f.net.irc.martyr.IRCConnection;
//import f00f.net.irc.martyr.InCommand;
//import f00f.net.irc.martyr.State;
//import f00f.net.irc.martyr.commands.MessageCommand;
//import f00f.net.irc.martyr.services.AutoJoin;
//import f00f.net.irc.martyr.services.AutoReconnect;
//import f00f.net.irc.martyr.services.AutoRegister;
//import f00f.net.irc.martyr.services.AutoResponder;
//
//public class WikipediaUpdateAnalyzer {
//
//    public WikipediaUpdateAnalyzer() {
//
//        IRCConnection connection = new IRCConnection();
//
//        new AutoResponder(connection);
//
//        // we could also overwrite the getNickIterator method,
//        // to make sure we have a unique nickname, but the randomly
//        // generated should suffice for now.
//        String user = createRandomName();
//        new AutoRegister(connection, user, user, user);
//
//        // keep up the connection and reconnect, if the connection dies
//        AutoReconnect autoReconnect = new AutoReconnect(connection);
//
//        // make sure we are present in the specified channel all the time
//        new AutoJoin(connection, "#en.wikipedia");
//        
//        // callback, which receives all messages
//        new MessageListener(connection);
//        
//        // let's go
//        autoReconnect.go("irc.wikimedia.org", 6667);
//
//    }
//
//    private static String createRandomName() {
//        return "user" + (System.currentTimeMillis() % 1000);
//    }
//
//    public static void main(String[] args) {
//        new WikipediaUpdateAnalyzer();
//    }
//
//}
//
//class MessageListener extends GenericAutoService {
//
//    protected MessageListener(IRCConnection connection) {
//        super(connection);
//    }
//
//    @Override
//    protected void updateState(State state) {
//        // NOP.nop
//    }
//
//    @Override
//    protected void updateCommand(InCommand command) {
//
//        // just print out all messages to console.
//        if (command instanceof MessageCommand) {
//            MessageCommand msg = (MessageCommand) command;
//            System.out.println(System.currentTimeMillis() + "\t" + msg.getMessage());
//        }
//
//    }
//
//}