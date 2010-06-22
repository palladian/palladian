package tud.iir.gui;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.util.Observable;
import java.util.Observer;

import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;

import tud.iir.control.Controller;
import tud.iir.extraction.ExtractionProcessManager;
import tud.iir.helper.LoggerMessage;
import tud.iir.knowledge.KnowledgeManager;
import tud.iir.persistence.DatabaseManager;
import tud.iir.persistence.OntologyManager;
import tud.iir.reporting.Reporter;
import tud.iir.web.SourceRetrieverManager;

// TODO show basic states of extractors (number of loop, current concept/attribute)
// TODO show current run time, download size, number of requests
/**
 * The GUIManager manages the complete layout of the WebKnox Core application.
 */
public class GUIManager implements Observer {

    private static GUIManager instance = null;

    JFrame f = null;

    JTextArea overviewEntityExtractionLogArea = null;
    JTextArea overviewFactExtractionLogArea = null;
    JTextArea overviewQAExtractionLogArea = null;
    JTextArea entityExtractionLogArea = null;
    JTextArea factExtractionLogArea = null;
    JTextArea qaExtractionLogArea = null;

    JButton entityExtractionButton = null;
    JButton factExtractionButton = null;
    JButton factExtractionBenchmarkButton = null;
    JButton qaExtractionButton = null;

    final JFileChooser fileChooser = new JFileChooser(new File("data"));

    private boolean showLogging = true;

    private GUIManager() {
        // super(""); // Initialize thread.
        System.out.println(this);
        // start();

        createGUI();
    }

    public static GUIManager getInstance() {
        if (instance == null) {
            instance = new GUIManager();
        }
        return instance;
    }

    public static boolean isInstanciated() {
        if (instance == null) {
            return false;
        }
        return true;
    }

    /**
     * Get notified when the object changes.
     * 
     * @param o The observable object.
     * @param arg More arguments.
     */
    public void update(Observable o, Object arg) {
        LoggerMessage lm = (LoggerMessage) arg;

        if (lm.getLoggerName().equalsIgnoreCase("EntityExtractionLogger")) {
            if (isShowLogging()) {
                if (entityExtractionLogArea.getLineCount() >= 400)
                    entityExtractionLogArea.setText("");
                if (overviewEntityExtractionLogArea.getLineCount() >= 400)
                    overviewEntityExtractionLogArea.setText("");
                entityExtractionLogArea.append(lm.getMessage());
                entityExtractionLogArea.setCaretPosition(entityExtractionLogArea.getDocument().getLength());
                overviewEntityExtractionLogArea.append(lm.getMessage());
                overviewEntityExtractionLogArea.setCaretPosition(overviewEntityExtractionLogArea.getDocument().getLength());
            }
        } else if (lm.getLoggerName().equalsIgnoreCase("FactExtractionLogger")) {
            if (lm.getMessage(false).equals("finished benchmark")) {
                factExtractionButton.setEnabled(true);
                factExtractionBenchmarkButton.setEnabled(true);
            }
            if (isShowLogging()) {
                if (factExtractionLogArea.getLineCount() >= 400)
                    factExtractionLogArea.setText("");
                if (overviewFactExtractionLogArea.getLineCount() >= 400)
                    overviewFactExtractionLogArea.setText("");
                factExtractionLogArea.append(lm.getMessage());
                factExtractionLogArea.setCaretPosition(factExtractionLogArea.getDocument().getLength());
                overviewFactExtractionLogArea.append(lm.getMessage());
                overviewFactExtractionLogArea.setCaretPosition(overviewFactExtractionLogArea.getDocument().getLength());
            }
        } else if (lm.getLoggerName().equalsIgnoreCase("QAExtractionLogger")) {
            if (isShowLogging()) {
                if (qaExtractionLogArea.getLineCount() >= 400)
                    qaExtractionLogArea.setText("");
                if (overviewQAExtractionLogArea.getLineCount() >= 400)
                    overviewQAExtractionLogArea.setText("");
                qaExtractionLogArea.append(lm.getMessage());
                qaExtractionLogArea.setCaretPosition(qaExtractionLogArea.getDocument().getLength());
                overviewQAExtractionLogArea.append(lm.getMessage());
                overviewQAExtractionLogArea.setCaretPosition(overviewQAExtractionLogArea.getDocument().getLength());
            }
        }
    }

    public void createGUI() {

        // try {
        // UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        // } catch(Exception e) {
        // System.out.println("Error setting native LAF: " + e);
        // }

        f = new JFrame(Controller.NAME);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setSize(1000, 700);

        // p.setLayout(new GridLayout(2, 2));

        createMenu(f);

        JTabbedPane tabbedPane = new JTabbedPane();
        // ImageIcon icon = createImageIcon("images/middle.gif");

        // overview panel
        JComponent overviewPanel = createPanel();
        // tabbedPane.addTab("Tab 1", icon, panel1, "Does nothing");
        tabbedPane.addTab("Overview", overviewPanel);
        tabbedPane.setMnemonicAt(0, KeyEvent.VK_1);

        // entity extraction panel
        JComponent entityExtractionPanel = createPanel();
        tabbedPane.addTab("Entity Extraction", entityExtractionPanel);
        tabbedPane.setMnemonicAt(1, KeyEvent.VK_2);

        // fact extraction panel
        JComponent factExtractionPanel = createPanel();
        tabbedPane.addTab("Fact Extraction", factExtractionPanel);
        tabbedPane.setMnemonicAt(2, KeyEvent.VK_3);

        JComponent qaExtractionPanel = createPanel();
        tabbedPane.addTab("QA Extraction", qaExtractionPanel);
        tabbedPane.setMnemonicAt(1, KeyEvent.VK_4);

        tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);

        // p.add(tabbedPane);

        // log areas for overview
        overviewEntityExtractionLogArea = createLogArea(80, 10, overviewPanel);
        overviewFactExtractionLogArea = createLogArea(80, 10, overviewPanel);
        overviewQAExtractionLogArea = createLogArea(80, 10, overviewPanel);

        // entity extraction log text area
        entityExtractionLogArea = createLogArea(80, 30, entityExtractionPanel);

        // fact extraction log text area
        factExtractionLogArea = createLogArea(80, 30, factExtractionPanel);

        // fact extraction log text area
        qaExtractionLogArea = createLogArea(80, 30, qaExtractionPanel);

        // entity extraction start/stop
        entityExtractionButton = new JButton("start entity extraction");
        overviewPanel.add(entityExtractionButton);
        entityExtractionButton.addActionListener(new EntityExtractionListener());

        // fact extraction start/stop
        factExtractionButton = new JButton("start fact extraction");
        overviewPanel.add(factExtractionButton);
        factExtractionButton.addActionListener(new FactExtractionListener());

        // run fact extraction benchmark
        factExtractionBenchmarkButton = new JButton("run fact extraction benchmark");
        overviewPanel.add(factExtractionBenchmarkButton);
        factExtractionBenchmarkButton.addActionListener(new FactExtractionBenchmarkListener());

        // Q/A extraction start/stop
        qaExtractionButton = new JButton("start Q/A extraction");
        overviewPanel.add(qaExtractionButton);
        qaExtractionButton.addActionListener(new QAExtractionListener());

        // data- and knowledgebase operations

        // // clear entire database
        // JButton clearDBButton = new JButton("clear entire database");
        // p.add(clearDBButton);
        // clearDBButton.addActionListener(new ClearDBListener());
        //		
        // // clean unused ontology elements
        // JButton cleanDBButton = new JButton("clean database");
        // p.add(cleanDBButton);
        // cleanDBButton.addActionListener(new CleanDBListener());
        //		
        // // clear entire owl knowledgebase
        // JButton clearOWLKBButton = new JButton("clear entire owl knowledgebase");
        // p.add(clearOWLKBButton);
        // clearDBButton.addActionListener(new ClearOWLKBListener());

        // loading window
        // loadingBar = new JProgressBar(0, 100);
        // loadingBar.setIndeterminate(true);
        // loadingBar.setVisible(false);
        // //progressBar.setStringPainted(true);
        //
        // //JPanel panel = new JPanel();
        // //panel.add(progressBar);
        // //panel.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        // //loadingBar.add(panel);
        //        
        // overviewPanel.add(loadingBar);

        f.add(tabbedPane, BorderLayout.CENTER);
        // f.pack();
        f.setVisible(true);
    }

    private JComponent createPanel() {
        JPanel panel = new JPanel(false);
        // panel.setLayout(new GridLayout(1, 1));
        return panel;
    }

    private JTextArea createLogArea(int width, int height, JComponent panel) {
        JTextArea logArea = new JTextArea(height, width);
        logArea.setEditable(false);
        panel.add(logArea);
        JScrollPane scrollPane = new JScrollPane(logArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        panel.add(scrollPane);
        return logArea;
    }

    private void createMenu(JFrame frame) {
        JMenuBar menuBar;
        JMenu menu;
        JMenuItem menuItem;
        JSeparator separator;

        // create the menu bar.
        menuBar = new JMenuBar();

        // // open files
        menu = new JMenu("Open");
        menu.setMnemonic(KeyEvent.VK_O);
        menu.getAccessibleContext().setAccessibleDescription("The only menu in this program that has menu items");
        menuBar.add(menu);

        menuItem = new JMenuItem("File", KeyEvent.VK_F);
        menuItem.addActionListener(new OpenFileActionListener());
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_1, ActionEvent.ALT_MASK));
        menuItem.getAccessibleContext().setAccessibleDescription("This doesn't really do anything");
        menu.add(menuItem);

        // // knowledge base operations
        menu = new JMenu("Knowledge Base");
        menu.setMnemonic(KeyEvent.VK_K);
        menu.getAccessibleContext().setAccessibleDescription("The only menu in this program that has menu items");
        menuBar.add(menu);

        // menu items
        // find synonyms
        menuItem = new JMenuItem("Find Synonyms", KeyEvent.VK_F);
        menuItem.addActionListener(new FindSynonymsListener());
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_1, ActionEvent.ALT_MASK));
        menuItem.getAccessibleContext().setAccessibleDescription("This doesn't really do anything");
        menu.add(menuItem);

        // clear database
        menuItem = new JMenuItem("Clear Entire Database", KeyEvent.VK_C);
        menuItem.addActionListener(new ClearDBListener());
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_2, ActionEvent.ALT_MASK));
        menuItem.getAccessibleContext().setAccessibleDescription("This doesn't really do anything");
        menu.add(menuItem);

        // clean database
        menuItem = new JMenuItem("Clean Up Database", KeyEvent.VK_U);
        menuItem.addActionListener(new CleanDBListener());
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_3, ActionEvent.ALT_MASK));
        menuItem.getAccessibleContext().setAccessibleDescription("This doesn't really do anything");
        menu.add(menuItem);

        // clear owl knowledge base
        menuItem = new JMenuItem("Clear OWL Knowledge Base", KeyEvent.VK_O);
        menuItem.addActionListener(new ClearOWLKBListener());
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_4, ActionEvent.ALT_MASK));
        menuItem.getAccessibleContext().setAccessibleDescription("This doesn't really do anything");
        menu.add(menuItem);

        // /// source settings
        menu = new JMenu("Source Retrieval");
        menu.setMnemonic(KeyEvent.VK_S);
        menu.getAccessibleContext().setAccessibleDescription("The only menu in this program that has menu items");
        menuBar.add(menu);

        // submenu: sources
        ButtonGroup group = new ButtonGroup();
        JMenu submenu = new JMenu("Source");
        menuItem = new JRadioButtonMenuItem("Google");
        menuItem.setMnemonic(KeyEvent.VK_G);
        if (ExtractionProcessManager.getSourceRetrievalSite() == SourceRetrieverManager.GOOGLE)
            menuItem.setSelected(true);
        menuItem.addActionListener(new SourceChangeListener());
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_1, ActionEvent.ALT_MASK));
        menuItem.getAccessibleContext().setAccessibleDescription("This doesn't really do anything");
        group.add(menuItem);
        submenu.add(menuItem);
        menuItem = new JRadioButtonMenuItem("Hakia");
        menuItem.setMnemonic(KeyEvent.VK_H);
        if (ExtractionProcessManager.getSourceRetrievalSite() == SourceRetrieverManager.HAKIA)
            menuItem.setSelected(true);
        menuItem.addActionListener(new SourceChangeListener());
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_2, ActionEvent.ALT_MASK));
        menuItem.getAccessibleContext().setAccessibleDescription("This doesn't really do anything");
        group.add(menuItem);
        submenu.add(menuItem);
        menuItem = new JRadioButtonMenuItem("Microsoft");
        menuItem.setMnemonic(KeyEvent.VK_M);
        if (ExtractionProcessManager.getSourceRetrievalSite() == SourceRetrieverManager.MICROSOFT)
            menuItem.setSelected(true);
        menuItem.addActionListener(new SourceChangeListener());
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_3, ActionEvent.ALT_MASK));
        menuItem.getAccessibleContext().setAccessibleDescription("This doesn't really do anything");
        group.add(menuItem);
        submenu.add(menuItem);
        menuItem = new JRadioButtonMenuItem("Yahoo!");
        menuItem.setMnemonic(KeyEvent.VK_Y);
        if (ExtractionProcessManager.getSourceRetrievalSite() == SourceRetrieverManager.YAHOO)
            menuItem.setSelected(true);
        menuItem.addActionListener(new SourceChangeListener());
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_4, ActionEvent.ALT_MASK));
        menuItem.getAccessibleContext().setAccessibleDescription("This doesn't really do anything");
        group.add(menuItem);
        submenu.add(menuItem);
        menuItem = new JRadioButtonMenuItem("Yahoo! BOSS");
        menuItem.setMnemonic(KeyEvent.VK_B);
        if (ExtractionProcessManager.getSourceRetrievalSite() == SourceRetrieverManager.YAHOO_BOSS)
            menuItem.setSelected(true);
        menuItem.addActionListener(new SourceChangeListener());
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_5, ActionEvent.ALT_MASK));
        menuItem.getAccessibleContext().setAccessibleDescription("This doesn't really do anything");
        group.add(menuItem);
        submenu.add(menuItem);
        menuItem = new JRadioButtonMenuItem("Bing");
        menuItem.setMnemonic(KeyEvent.VK_I);
        if (ExtractionProcessManager.getSourceRetrievalSite() == SourceRetrieverManager.BING)
            menuItem.setSelected(true);
        menuItem.addActionListener(new SourceChangeListener());
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_6, ActionEvent.ALT_MASK));
        menuItem.getAccessibleContext().setAccessibleDescription("This doesn't really do anything");
        group.add(menuItem);
        submenu.add(menuItem);

        menu.add(submenu);

        // submenu: number of results for queries to the source
        group = new ButtonGroup();
        submenu = new JMenu("Number");
        menuItem = new JRadioButtonMenuItem("5");
        if (ExtractionProcessManager.getSourceRetrievalCount() == 5)
            menuItem.setSelected(true);
        menuItem.addActionListener(new ChangeSourceCountListener());
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_1, ActionEvent.ALT_MASK));
        menuItem.getAccessibleContext().setAccessibleDescription("This doesn't really do anything");
        group.add(menuItem);
        submenu.add(menuItem);
        menuItem = new JRadioButtonMenuItem("8");
        if (ExtractionProcessManager.getSourceRetrievalCount() == 8)
            menuItem.setSelected(true);
        menuItem.addActionListener(new ChangeSourceCountListener());
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_2, ActionEvent.ALT_MASK));
        menuItem.getAccessibleContext().setAccessibleDescription("This doesn't really do anything");
        group.add(menuItem);
        submenu.add(menuItem);
        menuItem = new JRadioButtonMenuItem("10");
        if (ExtractionProcessManager.getSourceRetrievalCount() == 10)
            menuItem.setSelected(true);
        menuItem.addActionListener(new ChangeSourceCountListener());
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_3, ActionEvent.ALT_MASK));
        menuItem.getAccessibleContext().setAccessibleDescription("This doesn't really do anything");
        group.add(menuItem);
        submenu.add(menuItem);
        menuItem = new JRadioButtonMenuItem("15");
        if (ExtractionProcessManager.getSourceRetrievalCount() == 15)
            menuItem.setSelected(true);
        menuItem.addActionListener(new ChangeSourceCountListener());
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_4, ActionEvent.ALT_MASK));
        menuItem.getAccessibleContext().setAccessibleDescription("This doesn't really do anything");
        group.add(menuItem);
        submenu.add(menuItem);
        menuItem = new JRadioButtonMenuItem("20");
        if (ExtractionProcessManager.getSourceRetrievalCount() == 20)
            menuItem.setSelected(true);
        menuItem.addActionListener(new ChangeSourceCountListener());
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_5, ActionEvent.ALT_MASK));
        menuItem.getAccessibleContext().setAccessibleDescription("This doesn't really do anything");
        group.add(menuItem);
        submenu.add(menuItem);
        menuItem = new JRadioButtonMenuItem("30");
        if (ExtractionProcessManager.getSourceRetrievalCount() == 30)
            menuItem.setSelected(true);
        menuItem.addActionListener(new ChangeSourceCountListener());
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_6, ActionEvent.ALT_MASK));
        menuItem.getAccessibleContext().setAccessibleDescription("This doesn't really do anything");
        group.add(menuItem);
        submenu.add(menuItem);
        menuItem = new JRadioButtonMenuItem("50");
        if (ExtractionProcessManager.getSourceRetrievalCount() == 50)
            menuItem.setSelected(true);
        menuItem.addActionListener(new ChangeSourceCountListener());
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_7, ActionEvent.ALT_MASK));
        menuItem.getAccessibleContext().setAccessibleDescription("This doesn't really do anything");
        group.add(menuItem);
        submenu.add(menuItem);
        menuItem = new JRadioButtonMenuItem("80");
        if (ExtractionProcessManager.getSourceRetrievalCount() == 80)
            menuItem.setSelected(true);
        menuItem.addActionListener(new ChangeSourceCountListener());
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_8, ActionEvent.ALT_MASK));
        menuItem.getAccessibleContext().setAccessibleDescription("This doesn't really do anything");
        group.add(menuItem);
        submenu.add(menuItem);
        menuItem = new JRadioButtonMenuItem("100");
        if (ExtractionProcessManager.getSourceRetrievalCount() == 100)
            menuItem.setSelected(true);
        menuItem.addActionListener(new ChangeSourceCountListener());
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_9, ActionEvent.ALT_MASK));
        menuItem.getAccessibleContext().setAccessibleDescription("This doesn't really do anything");
        group.add(menuItem);
        submenu.add(menuItem);
        menuItem = new JRadioButtonMenuItem("500");
        if (ExtractionProcessManager.getSourceRetrievalCount() == 500)
            menuItem.setSelected(true);
        menuItem.addActionListener(new ChangeSourceCountListener());
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_0, ActionEvent.ALT_MASK));
        menuItem.getAccessibleContext().setAccessibleDescription("This doesn't really do anything");
        group.add(menuItem);
        submenu.add(menuItem);

        menu.add(submenu);

        // /// extraction settings
        menu = new JMenu("Extraction Settings");
        menu.setMnemonic(KeyEvent.VK_E);
        menu.getAccessibleContext().setAccessibleDescription("The only menu in this program that has menu items");
        menuBar.add(menu);

        // entity extraction
        submenu = new JMenu("Entity Extraction");
        submenu.setMnemonic(KeyEvent.VK_E);
        submenu.getAccessibleContext().setAccessibleDescription("The only menu in this program that has menu items");
        menu.add(submenu);

        // TODO distinguish between automatically derived synonyms (WordNET) and given synonyms (and no synonmys)
        // set entity extraction settings
        menuItem = new JCheckBoxMenuItem("Use Concept Synonyms");
        menuItem.setMnemonic(KeyEvent.VK_U);
        menuItem.addActionListener(new UseConceptSynonymsListener());
        if (ExtractionProcessManager.isUseConceptSynonyms())
            menuItem.setSelected(true);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_1, ActionEvent.ALT_MASK));
        menuItem.getAccessibleContext().setAccessibleDescription("This doesn't really do anything");
        submenu.add(menuItem);

        // fact extraction
        submenu = new JMenu("Fact Extraction");
        submenu.setMnemonic(KeyEvent.VK_F);
        menu.add(submenu);

        // set fact extraction settings
        menuItem = new JCheckBoxMenuItem("Find New Attributes And Values");
        menuItem.setMnemonic(KeyEvent.VK_F);
        menuItem.addActionListener(new FindNewAttributesAndValuesListener());
        if (ExtractionProcessManager.isFindNewAttributesAndValues())
            menuItem.setSelected(true);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_1, ActionEvent.ALT_MASK));
        submenu.add(menuItem);

        // QA extraction
        submenu = new JMenu("QA Extraction");
        submenu.setMnemonic(KeyEvent.VK_Q);
        menu.add(submenu);

        // set QA extraction settings
        menuItem = new JCheckBoxMenuItem("Continue QA Extraction");
        menuItem.setMnemonic(KeyEvent.VK_C);
        menuItem.addActionListener(new ContinueQAExtractionListener());
        if (ExtractionProcessManager.isContinueQAExtraction())
            menuItem.setSelected(true);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_1, ActionEvent.ALT_MASK));
        submenu.add(menuItem);

        separator = new JSeparator();
        menu.add(separator);

        menuItem = new JCheckBoxMenuItem("Show Logging");
        menuItem.setMnemonic(KeyEvent.VK_S);
        menuItem.addActionListener(new ShowLoggingListener());
        if (isShowLogging())
            menuItem.setSelected(true);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_1, ActionEvent.ALT_MASK));
        menuItem.getAccessibleContext().setAccessibleDescription("This doesn't really do anything");
        menu.add(menuItem);

        // /// benchmark settings
        menu = new JMenu("Benchmark Settings");
        menu.setMnemonic(KeyEvent.VK_B);
        menu.getAccessibleContext().setAccessibleDescription("The only menu in this program that has menu items");
        menuBar.add(menu);

        submenu = new JMenu("Fact Extraction");
        submenu.setMnemonic(KeyEvent.VK_F);
        submenu.getAccessibleContext().setAccessibleDescription("The only menu in this program that has menu items");
        menu.add(submenu);

        // set benchmark settings
        menuItem = new JRadioButtonMenuItem("Full Set");
        group = new ButtonGroup();
        menuItem.setMnemonic(KeyEvent.VK_F);
        menuItem.addActionListener(new BenchmarkSetSizeListener());
        if (ExtractionProcessManager.getBenchmarkSetSize() == ExtractionProcessManager.BENCHMARK_FULL_SET)
            menuItem.setSelected(true);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_1, ActionEvent.ALT_MASK));
        menuItem.getAccessibleContext().setAccessibleDescription("This doesn't really do anything");
        group.add(menuItem);
        submenu.add(menuItem);

        menuItem = new JRadioButtonMenuItem("Half Set");
        group = new ButtonGroup();
        menuItem.setMnemonic(KeyEvent.VK_H);
        menuItem.addActionListener(new BenchmarkSetSizeListener());
        if (ExtractionProcessManager.getBenchmarkSetSize() == ExtractionProcessManager.BENCHMARK_HALF_SET)
            menuItem.setSelected(true);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_2, ActionEvent.ALT_MASK));
        menuItem.getAccessibleContext().setAccessibleDescription("This doesn't really do anything");
        group.add(menuItem);
        submenu.add(menuItem);

        separator = new JSeparator();
        submenu.add(separator);

        menuItem = new JRadioButtonMenuItem("Google Top 8");
        group = new ButtonGroup();
        menuItem.setMnemonic(KeyEvent.VK_G);
        menuItem.addActionListener(new BenchmarkSetListener());
        if (ExtractionProcessManager.getBenchmarkSet() == ExtractionProcessManager.GOOGLE_8)
            menuItem.setSelected(true);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_1, ActionEvent.ALT_MASK));
        menuItem.getAccessibleContext().setAccessibleDescription("This doesn't really do anything");
        group.add(menuItem);
        submenu.add(menuItem);

        menuItem = new JRadioButtonMenuItem("Hakia Top 8");
        menuItem.setMnemonic(KeyEvent.VK_H);
        menuItem.addActionListener(new BenchmarkSetListener());
        if (ExtractionProcessManager.getBenchmarkSet() == ExtractionProcessManager.HAKIA_8)
            menuItem.setSelected(true);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_2, ActionEvent.ALT_MASK));
        menuItem.getAccessibleContext().setAccessibleDescription("This doesn't really do anything");
        group.add(menuItem);
        submenu.add(menuItem);

        menuItem = new JRadioButtonMenuItem("Microsoft Top 8");
        menuItem.setMnemonic(KeyEvent.VK_M);
        menuItem.addActionListener(new BenchmarkSetListener());
        if (ExtractionProcessManager.getBenchmarkSet() == ExtractionProcessManager.MICROSOFT_8)
            menuItem.setSelected(true);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_3, ActionEvent.ALT_MASK));
        menuItem.getAccessibleContext().setAccessibleDescription("This doesn't really do anything");
        group.add(menuItem);
        submenu.add(menuItem);

        menuItem = new JRadioButtonMenuItem("Yahoo Top 8");
        menuItem.setMnemonic(KeyEvent.VK_Y);
        menuItem.addActionListener(new BenchmarkSetListener());
        if (ExtractionProcessManager.getBenchmarkSet() == ExtractionProcessManager.YAHOO_8)
            menuItem.setSelected(true);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_4, ActionEvent.ALT_MASK));
        menuItem.getAccessibleContext().setAccessibleDescription("This doesn't really do anything");
        group.add(menuItem);
        submenu.add(menuItem);

        // /// reporter operations
        menu = new JMenu("Reporter");
        menu.setMnemonic(KeyEvent.VK_R);
        menu.getAccessibleContext().setAccessibleDescription("The only menu in this program that has menu items");
        menuBar.add(menu);

        // create database report
        menuItem = new JMenuItem("Database Report", KeyEvent.VK_D);
        menuItem.addActionListener(new CreateDatabaseReportListener());
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_1, ActionEvent.ALT_MASK));
        menuItem.getAccessibleContext().setAccessibleDescription("This doesn't really do anything");
        menu.add(menuItem);

        menuBar.add(Box.createHorizontalGlue());

        // /// help
        menu = new JMenu("?");
        menu.setMnemonic(KeyEvent.VK_H);
        menu.getAccessibleContext().setAccessibleDescription("The only menu in this program that has menu items");
        menuBar.add(menu);

        // create database report
        menuItem = new JMenuItem("Version", KeyEvent.VK_V);
        menuItem.addActionListener(new ShowVersionListener());
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_1, ActionEvent.ALT_MASK));
        menuItem.getAccessibleContext().setAccessibleDescription("This doesn't really do anything");
        menu.add(menuItem);

        frame.setJMenuBar(menuBar);
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        GUIManager.getInstance();
    }

    class OpenFileActionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {

            int returnValue = fileChooser.showOpenDialog(f);

            if (returnValue == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                try {
                    Desktop.getDesktop().open(file);
                } catch (IOException e1) {
                    JOptionPane.showMessageDialog(f, e1.getMessage().substring(e1.getMessage().indexOf("Error message: ") + 15), "Error Openening File",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    class EntityExtractionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            if (ExtractionProcessManager.entityExtractionIsRunning) {
                ExtractionProcessManager.stopEntityExtraction();
                entityExtractionButton.setText("start entity extraction");
            } else {
                ExtractionProcessManager.startEntityExtraction();
                entityExtractionButton.setText("stop entity extraction");
            }
        }
    }

    class FactExtractionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            if (ExtractionProcessManager.factExtractionIsRunning) {
                ExtractionProcessManager.stopFactExtraction();
                factExtractionButton.setText("start fact extraction");
                factExtractionBenchmarkButton.setEnabled(true);
            } else {
                ExtractionProcessManager.startFactExtraction();
                factExtractionButton.setText("stop fact extraction");
                factExtractionBenchmarkButton.setEnabled(false);
            }
        }
    }

    class FactExtractionBenchmarkListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            ((JButton) e.getSource()).setEnabled(false);
            factExtractionButton.setEnabled(false);
            ExtractionProcessManager.runFactExtractionBenchmark();
        }
    }

    class QAExtractionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            if (ExtractionProcessManager.qaExtractionIsRunning) {
                ExtractionProcessManager.stopQAExtraction();
                qaExtractionButton.setText("start Q/A extraction");
            } else {
                ExtractionProcessManager.startQAExtraction();
                qaExtractionButton.setText("stop Q/A extraction");
            }
        }
    }

    final static class FindSynonymsListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            // showLoadingWindow();
            /*
             * new Thread( new Runnable() { public void run() { KnowledgeManager km = DatabaseManager.getInstance().loadOntology();
             * km.calculateAttributeSynonyms(); } }).start();
             */
            KnowledgeManager km = DatabaseManager.getInstance().loadOntology();
            km.calculateAttributeSynonyms();
            // hideLoadingWindow();
        }
    }

    final static class ClearDBListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            DatabaseManager.getInstance().clearCompleteDatabase();
        }
    }

    final static class CleanDBListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            DatabaseManager.getInstance().cleanUnusedOntologyElements();
        }
    }

    final static class ClearOWLKBListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            OntologyManager.getInstance().clearCompleteKnowledgeBase();
        }
    }

    // sources menu
    final static class SourceChangeListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            String buttonLabel = ((JRadioButtonMenuItem) e.getSource()).getText();
            if (buttonLabel.equalsIgnoreCase("google")) {
                SourceRetrieverManager.getInstance().setSource(SourceRetrieverManager.GOOGLE);
            } else if (buttonLabel.equalsIgnoreCase("hakia")) {
                SourceRetrieverManager.getInstance().setSource(SourceRetrieverManager.HAKIA);
            } else if (buttonLabel.equalsIgnoreCase("microsoft")) {
                SourceRetrieverManager.getInstance().setSource(SourceRetrieverManager.MICROSOFT);
            } else if (buttonLabel.equalsIgnoreCase("yahoo!")) {
                SourceRetrieverManager.getInstance().setSource(SourceRetrieverManager.YAHOO);
            } else if (buttonLabel.equalsIgnoreCase("yahoo! boss")) {
                SourceRetrieverManager.getInstance().setSource(SourceRetrieverManager.YAHOO_BOSS);
            } else if (buttonLabel.equalsIgnoreCase("bing")) {
                SourceRetrieverManager.getInstance().setSource(SourceRetrieverManager.BING);
            }
            // System.out.println(ExtractionProcessManager.getSourceRetrievalSite());
        }
    }

    final static class ChangeSourceCountListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            int number = Integer.valueOf(((JRadioButtonMenuItem) e.getSource()).getText());
            SourceRetrieverManager.getInstance().setResultCount(number);
            System.out.println(ExtractionProcessManager.getSourceRetrievalCount());
        }
    }

    // extraction settings menu
    final static class UseConceptSynonymsListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            ExtractionProcessManager.setUseConceptSynonyms(!ExtractionProcessManager.isUseConceptSynonyms());
        }
    }

    final static class FindNewAttributesAndValuesListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            ExtractionProcessManager.setFindNewAttributesAndValues(!ExtractionProcessManager.isFindNewAttributesAndValues());
        }
    }

    final static class ContinueQAExtractionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            ExtractionProcessManager.setContinueQAExtraction(!ExtractionProcessManager.isContinueQAExtraction());
        }
    }

    class ShowLoggingListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            setShowLogging(!isShowLogging());
        }
    }

    // benchmark settings menu
    final static class BenchmarkSetSizeListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            String buttonLabel = ((JRadioButtonMenuItem) e.getSource()).getText();
            if (buttonLabel.equalsIgnoreCase("full")) {
                ExtractionProcessManager.setBenchmarkSetSize(ExtractionProcessManager.BENCHMARK_FULL_SET);
            } else if (buttonLabel.equalsIgnoreCase("half")) {
                ExtractionProcessManager.setBenchmarkSetSize(ExtractionProcessManager.BENCHMARK_HALF_SET);
            }
        }
    }

    final static class BenchmarkSetListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            String buttonLabel = ((JRadioButtonMenuItem) e.getSource()).getText();
            if (buttonLabel.equalsIgnoreCase("google top 8")) {
                ExtractionProcessManager.setBenchmarkSet(ExtractionProcessManager.GOOGLE_8);
            } else if (buttonLabel.equalsIgnoreCase("hakia top 8")) {
                ExtractionProcessManager.setBenchmarkSet(ExtractionProcessManager.HAKIA_8);
            } else if (buttonLabel.equalsIgnoreCase("microsoft top 8")) {
                ExtractionProcessManager.setBenchmarkSet(ExtractionProcessManager.MICROSOFT_8);
            } else if (buttonLabel.equalsIgnoreCase("yahoo top 8")) {
                ExtractionProcessManager.setBenchmarkSet(ExtractionProcessManager.YAHOO_8);
            }
        }
    }

    // reporter menu
    final static class CreateDatabaseReportListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            Reporter.getInstance().createDBReport(true);
        }
    }

    // help menu
    class ShowVersionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            JOptionPane.showMessageDialog(f, "Currently the Version is " + Controller.VERSION, Controller.NAME, JOptionPane.INFORMATION_MESSAGE);
        }
    }

    public boolean isShowLogging() {
        return showLogging;
    }

    public void setShowLogging(boolean showLogging) {
        this.showLogging = showLogging;
    }
}