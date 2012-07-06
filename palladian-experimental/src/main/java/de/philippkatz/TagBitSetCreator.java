package de.philippkatz;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections15.Bag;
import org.apache.commons.collections15.bag.HashBag;
import org.apache.commons.lang3.StringUtils;

import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.LineAction;

/**
 * <p>
 * Throw-away converter for creating a BitSet dataset from the DeliciousT140 dataset. I made some experiments in KNIME
 * with association rule mining and therefore needed this format. From all available tags, for each document, a line
 * with a bit vector, representing the presence of a tag is written. This can then be input into the association mining
 * nodes.
 * </p>
 * 
 * @author Philipp Katz
 */
class TagBitSetCreator {

    public static void main(String[] args) throws IOException {

        final FileWriter writer = new FileWriter(new File("/Users/pk/Desktop/tag_bit_values_more.txt"));

        final Bag<String> availTags = new HashBag<String>();
        FileHelper.performActionOnEveryLine("/Users/pk/Desktop/delicioust140/big_train.txt", new LineAction() {
            @Override
            public void performAction(String line, int lineNumber) {
                String[] split = line.split("#");
                for (int i = 1; i < split.length; i++) {
                    availTags.add(split[i]);
                }
            }
        });

        // do a bit of cleaning
        final List<String> prunedTages = new ArrayList<String>();
        for (String tag : availTags.uniqueSet()) {
            if (tag != null && availTags.getCount(tag) > 5 && tag.length() > 1) {
                prunedTages.add(tag);
            }
        }
        System.out.println("#  org. tags: " + availTags.uniqueSet().size());
        System.out.println("# tags: " + prunedTages.size());
        writer.write(StringUtils.join(prunedTages, ";"));
        writer.write("\n");

        FileHelper.performActionOnEveryLine("/Users/pk/Desktop/delicioust140/big_train.txt", new LineAction() {
            @Override
            public void performAction(String line, int lineNumber) {
                if (lineNumber % 100 == 0) {
                    System.out.println(lineNumber);
                }
                String[] split = line.split("#");
                Set<String> tags = new HashSet<String>();
                for (int i = 1; i < split.length; i++) {
                    tags.add(split[i]);
                }
                StringBuilder outputLine = new StringBuilder();
                for (String tag : prunedTages) {
                    if (tags.contains(tag)) {
                        outputLine.append(1);
                    } else {
                        outputLine.append(0);
                    }
                    outputLine.append(";");
                }
                try {
                    writer.write(outputLine.substring(0, outputLine.length() - 2));
                    writer.write("\n");
                } catch (IOException e) {
                    breakLineLoop();
                }
            }
        });
        writer.close();
    }

}
