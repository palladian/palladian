package tud.iir.reporting;

import java.awt.Color;
import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import tud.iir.control.Controller;
import tud.iir.helper.DateHelper;
import tud.iir.helper.MathHelper;
import tud.iir.knowledge.Concept;
import tud.iir.knowledge.Entity;
import tud.iir.knowledge.Fact;
import tud.iir.knowledge.FactValue;
import tud.iir.knowledge.KnowledgeManager;
import tud.iir.persistence.DatabaseManager;
import tud.iir.persistence.OntologyManager;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

/*
 * tutorials:
 * http://www.screaming-penguin.com/node/4005
 * http://www.javaresources.biz/jfreechart_tutorial.jsp
 * http://www.java2s.com/Code/Java/Chart/JFreeChartLineChartDemo6.htm
 */

/**
 * The Reporter creates reports.
 */
public class Reporter {

    private static Reporter instance = null;
    private int runtime;

    private Reporter() {
    }

    public static Reporter getInstance() {
        if (instance == null)
            instance = new Reporter();
        return instance;
    }

    public int getRuntime() {
        return runtime;
    }

    public void setRuntime(int runtime) {
        this.runtime = runtime;
    }

    public static String getReportFolderPath() {
        // String subFolderName = "web";
        // if (Controller.EXTRACTION_SOURCES == Controller.SELECTION) subFolderName = "selection";
        // return "data/reports/"+subFolderName+"/";
        return "data/reports/";
    }

    private PdfPCell makeBenchmarkTableCell(String input) {
        return makeBenchmarkTableCell(input, Color.WHITE);
    }

    private PdfPCell makeBenchmarkTableCell(String input, Color color) {
        PdfPCell cell = new PdfPCell();

        cell.addElement(new Paragraph(input));
        cell.setBorder(Element.RECTANGLE);
        cell.setBackgroundColor(color);

        return cell;
    }

    private PdfPTable createBenchmarkTable(ReportSet reportSet) {

        PdfPTable table = new PdfPTable(12);

        // table.setWidths(WIDTHS);
        table.setWidthPercentage(100);

        PdfPCell cell;

        String[] headings = { "Concept", "Tot. ent.", "Tot. c. ent.", "E. Precision", "c. ent./min.", "Tot. facts", "Tot. c. facts", "Fact Precision",
                "c. Facts/min.", "Avg. facts/entity (recall)", "Avg. c. facts/entity", "Fact F1" };
        System.out.println(headings[0]);

        // create table head
        for (int i = 0; i < 12; ++i) {
            cell = new PdfPCell();
            cell.addElement(new Paragraph(headings[i]));
            cell.setBorder(Element.RECTANGLE);
            cell.setBackgroundColor(Color.LIGHT_GRAY);
            // cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            // setColor(movie, cell);
            table.addCell(cell);
        }

        // iterate through all domains and enter statistics in the table
        Iterator<Map.Entry<Concept, Report>> domainIterator = reportSet.entrySet().iterator();

        while (domainIterator.hasNext()) {
            Map.Entry<Concept, Report> entry = domainIterator.next();
            Concept currentDomain = entry.getKey();
            Report domainReport = entry.getValue();

            cell = makeBenchmarkTableCell(currentDomain.getName());
            cell.setBackgroundColor(Color.LIGHT_GRAY);
            table.addCell(cell);
            table.addCell(makeBenchmarkTableCell(domainReport.getTotalEntitiesForView()));
            table.addCell(makeBenchmarkTableCell(domainReport.getTotalCorrectEntitiesForView()));
            table.addCell(makeBenchmarkTableCell(domainReport.getEntityPrecisionForView()));
            table.addCell(makeBenchmarkTableCell(domainReport.getCorrectEntitiesPerMinuteForView()));
            table.addCell(makeBenchmarkTableCell(domainReport.getTotalFactsForView()));
            table.addCell(makeBenchmarkTableCell(domainReport.getTotalCorrectFactsForView()));
            table.addCell(makeBenchmarkTableCell(domainReport.getFactPrecisionForView()));
            table.addCell(makeBenchmarkTableCell(domainReport.getCorrectFactsPerMinuteForView()));
            table.addCell(makeBenchmarkTableCell(domainReport.getAvgFactsPerEntityForView()));
            table.addCell(makeBenchmarkTableCell(domainReport.getAvgCorrectFactsPerEntityForView()));
            table.addCell(makeBenchmarkTableCell(domainReport.getFactF1ForView()));
        }

        // create table foot with summarization of all domains
        table.addCell(makeBenchmarkTableCell("Total", Color.LIGHT_GRAY));
        table.addCell(makeBenchmarkTableCell(reportSet.getTotalEntitiesForView(), Color.LIGHT_GRAY));
        table.addCell(makeBenchmarkTableCell(reportSet.getTotalCorrectEntitiesForView(), Color.LIGHT_GRAY));
        table.addCell(makeBenchmarkTableCell(reportSet.getTotalEntityPrecisionForView(), Color.LIGHT_GRAY));
        table.addCell(makeBenchmarkTableCell(reportSet.getCorrectEntitiesPerMinuteForView(), Color.LIGHT_GRAY));
        table.addCell(makeBenchmarkTableCell(reportSet.getTotalFactsForView(), Color.LIGHT_GRAY));
        table.addCell(makeBenchmarkTableCell(reportSet.getTotalCorrectFactsForView(), Color.LIGHT_GRAY));
        table.addCell(makeBenchmarkTableCell(reportSet.getTotalFactPrecisionForView(), Color.LIGHT_GRAY));
        table.addCell(makeBenchmarkTableCell(reportSet.getCorrectFactsPerMinuteForView(), Color.LIGHT_GRAY));
        table.addCell(makeBenchmarkTableCell(reportSet.getAvgFactsPerEntityForView(), Color.LIGHT_GRAY));
        table.addCell(makeBenchmarkTableCell(reportSet.getAvgCorrectFactsPerEntityForView(), Color.LIGHT_GRAY));
        table.addCell(makeBenchmarkTableCell(reportSet.getFactF1ForView(), Color.LIGHT_GRAY));

        // Paragraph p = new Paragraph();
        // p.add(new Phrase("test"));
        // p.setLeading(16);
        //
        // // the first cell with the full title spans all the columns
        // PdfPCell cell = new PdfPCell();
        // cell.addElement(p);
        //
        // cell.setColspan(3);
        // cell.setBorder(PdfPCell.CELL);
        // //setColor(movie, cell);
        // table.addCell(cell);

        return table;
    }

    /**
     * Evaluate the measures for all domains and create a report holding those values.
     * 
     * @return A set of reports.
     */
    private ReportSet evaluate(KnowledgeManager knowledgeManager) {

        int runtime = this.getRuntime();
        ReportSet reportSet = new ReportSet(runtime);

        ArrayList<Concept> concepts = knowledgeManager.getConcepts();
        Iterator<Concept> conceptIterator = concepts.iterator();
        while (conceptIterator.hasNext()) {

            // create a new measure map for the current concept
            Report report = new Report();

            Concept conceptEntry = conceptIterator.next();
            Iterator<Entity> entityIterator = conceptEntry.getEntitiesByTrust().iterator();
            while (entityIterator.hasNext()) {

                Entity entityEntry = entityIterator.next();

                // update report
                if (!entityEntry.isInitial()) {
                    report.totalEntities++;
                    if (entityEntry.isCorrect())
                        report.totalCorrectEntities++;
                }

                int factsForEntity = 0;

                Iterator<Fact> factIterator = entityEntry.getFacts().iterator();
                while (factIterator.hasNext()) {

                    Fact factEntry = factIterator.next();

                    // update report
                    factsForEntity++;
                    report.totalFacts++;
                    if (factEntry.isAbsoluteCorrect())
                        report.totalCorrectFacts++;

                    Iterator<FactValue> factValueIterator = factEntry.getValues().iterator();
                    while (factValueIterator.hasNext()) {

                        FactValue factValue = factValueIterator.next();
                        Logger.getRootLogger().info(factValue.getValue() + "/" + factValue.getCorroboration());
                    }
                }
            }

            if (report.totalFacts > 0.0)
                report.factPrecision = report.totalCorrectFacts / report.totalFacts;
            else
                report.factPrecision = 0.0;
            report.correctFactsPerMinute = (60 * report.totalCorrectFacts / runtime);
            if (report.totalEntities > 0.0)
                report.avgFactsPerEntity = report.totalFacts / report.totalEntities;
            else
                report.avgFactsPerEntity = 0.0;
            if (report.totalCorrectEntities > 0.0)
                report.avgCorrectFactsPerEntity = report.totalCorrectFacts / report.totalCorrectEntities;
            else
                report.avgCorrectFactsPerEntity = 0.0;
            if (report.factPrecision + report.avgFactsPerEntity > 0.0)
                report.factF1 = (2 * report.factPrecision * report.avgFactsPerEntity) / (report.factPrecision + report.avgFactsPerEntity);
            else
                report.factF1 = 0.0;
            if (report.totalEntities > 0.0)
                report.entityPrecision = report.totalCorrectEntities / report.totalEntities;
            else
                report.entityPrecision = 0.0;
            report.correctEntitiesPerMinute = (60 * report.totalCorrectEntities / runtime);

            // add the report for the currently evaluated domain
            reportSet.put(conceptEntry, report);
        }

        return reportSet;

    }

    /**
     * Create a report for the current extraction process. the report will be created in the correct folder depending on whether the complete web or only a
     * selction was used for extraction three report files will be created: 1. the complete result set with all measures for each domain will be saved 2. only
     * the total and averaged results will be saved 3. a pdf file with a table with all measures for all domains and charts will be saved
     */
    public void updateChartsOnly() {
        String chart1ExtractionQuantity = "extractionQuantity.png";
        String chart2ExtractionPrecision = "extractionPrecision.png";
        ChartCreator.createXYChart(chart1ExtractionQuantity, ReportFileParser.getExtractionQuantities(), "Extraction Quantities", "Time",
                "Number of Extractions", false, ChartCreator.XY_LINE_CHART);
        ChartCreator.createXYChart(chart2ExtractionPrecision, ReportFileParser.getExtractionQualities(), "Extraction Qualities", "Time", "Percentage", false,
                ChartCreator.XY_LINE_CHART);

    }

    public void createReport(KnowledgeManager knowledgeManager) {

        // create the report set by evaluating the extractions
        ReportSet reportSet = evaluate(knowledgeManager);

        // 1. save the complete report set
        reportSet.saveCompleteReportSet();

        // 2. save the total and averaged results only
        reportSet.saveTotalOnly();

        // 3. create a pdf report file
        // create images from reports (save them)
        String chart1ExtractionQuantity = "extractionQuantity.png";
        String chart2ExtractionPrecision = "extractionPrecision.png";
        ChartCreator.createXYChart(chart1ExtractionQuantity, ReportFileParser.getExtractionQuantities(), "Extraction Quantities", "Time",
                "Number of Extractions", false, ChartCreator.XY_LINE_CHART);
        ChartCreator.createXYChart(chart2ExtractionPrecision, ReportFileParser.getExtractionQualities(), "Extraction Qualities", "Time", "Percentage", false,
                ChartCreator.XY_LINE_CHART);

        // assemble the file
        try {
            Document document = new Document(PageSize.A4.rotate());
            document.setMargins(24, 24, 24, 24);
            document.addTitle("Extraction Report");
            document.addAuthor("David Urbansky");
            document.addSubject("This report evaluates extraction processes.");
            document.addKeywords("web information extraction,report");
            document.addCreator(Controller.NAME);

            PdfWriter.getInstance(document, new FileOutputStream(getReportFolderPath() + DateHelper.getCurrentDatetime() + "_" + "reports.pdf"));
            document.open();

            // add full benchmark table
            document.add(this.createBenchmarkTable(reportSet));

            // add progress images
            Image png = Image.getInstance(getReportFolderPath() + chart1ExtractionQuantity);
            png.setAlignment(Image.MIDDLE);
            document.add(png);
            png = Image.getInstance(getReportFolderPath() + chart2ExtractionPrecision);
            png.setAlignment(Image.MIDDLE);
            document.add(png);

            // close to save
            document.close();

        } catch (FileNotFoundException e) {
            Logger.getRootLogger().error(e.getMessage());
        } catch (DocumentException e) {
            Logger.getRootLogger().error(e.getMessage());
        } catch (IOException e) {
            Logger.getRootLogger().error(e.getMessage());
        }
    }

    private void analyzeCorroboration(String fileName) {

        ArrayList<ArrayList<Double[]>> al = new ArrayList<ArrayList<Double[]>>();

        TreeMap<Double, Integer> counts = new TreeMap<Double, Integer>();

        try {
            BufferedReader br = new BufferedReader(new FileReader(fileName));

            String line = "";
            do {
                line = br.readLine();

                if (line != null && line.indexOf(" : ") > -1) {
                    String[] parts = line.split(" : ");
                    Double d = Double.valueOf(parts[1]);
                    // System.out.println("d:"+d);
                    if (counts.containsKey(d)) {
                        Integer c = counts.get(d);
                        counts.put(d, ++c);
                    } else {
                        counts.put(d, 1);
                    }
                }

            } while (line != null);

            br.close();

        } catch (FileNotFoundException e) {
            Logger.getRootLogger().error(fileName, e);
        } catch (IOException e) {
            Logger.getRootLogger().error(fileName, e);
        }

        ArrayList<Double[]> corroboration = new ArrayList<Double[]>();

        Iterator<Map.Entry<Double, Integer>> countIterator = counts.entrySet().iterator();
        while (countIterator.hasNext()) {
            Map.Entry<Double, Integer> entry = countIterator.next();
            Double[] d = new Double[2];
            d[0] = entry.getKey();
            d[1] = (double) entry.getValue();
            // System.out.println(d[0]+","+d[1]);
            corroboration.add(d);
        }

        // add up corroborations (cumulative)
        Collections.sort(corroboration, new Comparator<Double[]>() {
            public int compare(Double[] e1, Double[] e2) {
                return (int) (10000 * e2[0] - 10000 * e1[0]);
                // return e1.getName().compareTo(e2.getName());
            }
        });
        Iterator<Double[]> corroborationIterator = corroboration.iterator();
        Double[] lastEntry = null;
        while (corroborationIterator.hasNext()) {
            Double[] entry = corroborationIterator.next();
            if (lastEntry == null) {
                lastEntry = entry;
                continue;
            }
            entry[1] += lastEntry[1];

            lastEntry = entry;
            System.out.println(entry[0] + "," + entry[1]);
        }

        // for output change order again
        Collections.sort(corroboration, new Comparator<Double[]>() {
            public int compare(Double[] e1, Double[] e2) {
                return (int) (10000 * e1[0] - 10000 * e2[0]);
                // return e1.getName().compareTo(e2.getName());
            }
        });
        Iterator<Double[]> corroborationIterator2 = corroboration.iterator();
        while (corroborationIterator2.hasNext()) {
            Double[] entry = corroborationIterator2.next();
            System.out.println(entry[0] + "," + entry[1]);
        }

        al.add(corroboration);

        ChartCreator.createXYChart("corroborationAnalysis.png", al, "Corroboration Dispersion", "Corroboration", "Count with higher or equal corroboration",
                true, ChartCreator.XY_LINE_CHART);

    }

    public void createDBReport(boolean openFile) {

        Document document = new Document(PageSize.A4.rotate());
        document.setMargins(24, 24, 24, 24);
        document.addTitle("Database Report");
        document.addAuthor("David Urbansky");
        document.addSubject("This report evaluates extraction processes.");
        document.addKeywords("web information extraction,report");
        document.addCreator(Controller.NAME);

        String fileName = getReportFolderPath() + DateHelper.getCurrentDatetime() + "_databaseReport.pdf";

        try {
            PdfWriter.getInstance(document, new FileOutputStream(fileName));

            document.open();

            Paragraph spacer = new Paragraph(" ");

            String dateString = String.valueOf(Calendar.getInstance().get(Calendar.DAY_OF_MONTH)) + " "
                    + String.valueOf(Calendar.getInstance().getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.ENGLISH)) + " "
                    + String.valueOf(Calendar.getInstance().get(Calendar.YEAR));
            // TODO font not working
            Font font = new Font();
            font.setStyle(Font.BOLD);
            font.setFamily("Arial");
            font.setSize(26);
            Paragraph title = new Paragraph("Database Report, " + dateString);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setFont(font);
            document.add(title);
            document.add(spacer);

            PdfPTable table;
            int tableWidth = 40;

            KnowledgeManager km = OntologyManager.getInstance().loadOntology();
            DatabaseManager dm = DatabaseManager.getInstance();

            table = new PdfPTable(2);
            // table.setWidths(WIDTHS);
            table.setWidthPercentage(tableWidth);
            table.setHorizontalAlignment(Element.ALIGN_LEFT);

            table = addTableCell("Total", table, 2);
            table = addTableCell("Concepts", table);
            table = addTableCell(dm.getTotalConceptsNumber(), table);
            table = addTableCell("Attributes", table);
            table = addTableCell(dm.getTotalAttributesNumber(), table);
            table = addTableCell("Entities", table);
            table = addTableCell(dm.getTotalEntitiesNumber(), table);
            table = addTableCell("Facts", table);
            table = addTableCell(dm.getTotalFactsNumber(), table);
            table = addTableCell("Sources", table);
            table = addTableCell(dm.getTotalSourcesNumber(), table);
            document.add(table);
            document.add(spacer);

            Iterator<Concept> cIterator = km.getConcepts().iterator();
            while (cIterator.hasNext()) {
                Concept concept = cIterator.next();

                table = new PdfPTable(2);
                table.setWidthPercentage(tableWidth);
                table.setHorizontalAlignment(Element.ALIGN_LEFT);

                int totalEntities = dm.getTotalEntitiesNumber(concept.getName());
                int totalFacts = dm.getTotalFactsNumber(concept.getName());
                double avgFactsPerEntity = 0.0;
                if (totalFacts > 1)
                    avgFactsPerEntity = MathHelper.round((double) totalFacts / (double) totalEntities, 3);
                String conceptNames = concept.getName();
                if (concept.getSynonyms().size() > 0)
                    conceptNames += " (" + concept.getSynonymsToString() + ")";
                table = addTableCell(conceptNames, table, 2);
                table = addTableCell("Entities", table);
                table = addTableCell(totalEntities, table);
                table = addTableCell("Facts", table);
                table = addTableCell(totalFacts, table);
                table = addTableCell("Avg. Facts/Entity", table);
                table = addTableCell(avgFactsPerEntity, table);
                document.add(table);
                document.add(spacer);
            }

            // close to save
            document.close();

            // if (openFile)
            // Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + fileName);
            Desktop.getDesktop().open(new File(fileName));

        } catch (DocumentException e) {
            Logger.getRootLogger().error(fileName, e);
        } catch (FileNotFoundException e) {
            Logger.getRootLogger().error(fileName, e);
        } catch (IOException e) {
            Logger.getRootLogger().error(fileName, e);
        }

    }

    private PdfPTable addTableCell(double numText, PdfPTable table) {
        return addTableCell(String.valueOf(numText), table, 1);
    }

    private PdfPTable addTableCell(int numText, PdfPTable table) {
        return addTableCell(String.valueOf(numText), table, 1);
    }

    private PdfPTable addTableCell(String text, PdfPTable table) {
        return addTableCell(text, table, 1);
    }

    private PdfPTable addTableCell(String text, PdfPTable table, int colspan) {
        PdfPCell cell = new PdfPCell();
        cell.addElement(new Paragraph(text));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setColspan(colspan);
        if (colspan == 2) {
            cell.setBackgroundColor(Color.LIGHT_GRAY);
            cell.setVerticalAlignment(Element.ALIGN_CENTER);
        } else
            cell.setBackgroundColor(Color.WHITE);
        // cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        // setColor(movie, cell);
        table.addCell(cell);
        return table;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        // Reporter.getInstance().createReport(0);
        int a = 4;
        int b = 7;
        double c = (double) a / (double) b;
        System.out.println(c);

        // Reporter.getInstance().createReport();
        Reporter.getInstance().updateChartsOnly();
        Reporter.getInstance().analyzeCorroboration("data/reports/corroborationAnalysisCities.txt");

    }
}