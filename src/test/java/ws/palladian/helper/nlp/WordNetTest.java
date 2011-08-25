package ws.palladian.helper.nlp;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import junit.framework.Assert;

import org.junit.Ignore;
import org.junit.Test;

/**
 * Test for WordNet helper class. Test cases are not executed automatically, as WordNet files need to be installed.
 * 
 * @author Philipp Katz
 */
public class WordNetTest {

    @Test
    @Ignore
    public void testGerundNormalizer() {
        
        Map<String, String> testSet = new HashMap<String, String>();
        
        // testSet.put("routing", "route"); // TODO
        testSet.put("accounting", "account");
        testSet.put("advertising", "advertise");
        testSet.put("aging", "age");
        testSet.put("amazing", "amaze");
        testSet.put("amusing", "amuse");
        testSet.put("authoring", "author");
        testSet.put("backpacking", "backpack");
        testSet.put("baking", "bake");
        testSet.put("banking", "bank");
        testSet.put("beating", "beat");
        testSet.put("bedding", "bed");
        testSet.put("being", "be");
        testSet.put("bicycling", "bicycle");
        testSet.put("biking", "bike");
        testSet.put("billing", "bill");
        testSet.put("binding", "bind");
        testSet.put("booking", "book");
        testSet.put("brainstorming", "brainstorm");
        testSet.put("branding", "brand");
        testSet.put("brewing", "brew");
        testSet.put("broadcasting", "broadcast");
        testSet.put("budgeting", "budget");
        testSet.put("building", "build");
        testSet.put("burning", "burn");
        testSet.put("buying", "buy");
        testSet.put("caching", "cache");
        testSet.put("camping", "camp");
        testSet.put("cataloging", "catalog");
        testSet.put("cataloguing", "catalogue");
        testSet.put("charting", "chart");
        testSet.put("cleaning", "clean");
        testSet.put("climbing", "climb");
        testSet.put("clothing", "clothe");
        testSet.put("clustering", "cluster");
        testSet.put("coaching", "coach");
        testSet.put("coding", "code");
        testSet.put("coloring", "color");
        testSet.put("computing", "compute");
        testSet.put("consulting", "consult");
        testSet.put("cooking", "cook");
        testSet.put("cooling", "cool");
        testSet.put("cracking", "crack");
        testSet.put("cycling", "cycle");
        testSet.put("dating", "date");
        testSet.put("debugging", "debug");
        testSet.put("decorating", "decorate");
        testSet.put("dining", "dine");
        testSet.put("drawing", "draw");
        testSet.put("drinking", "drink");
        testSet.put("driving", "drive");
        testSet.put("eating", "eat");
        testSet.put("editing", "edit");
        testSet.put("encoding", "encode");
        testSet.put("engineering", "engineer");
        testSet.put("farming", "farm");
        testSet.put("filtering", "filter");
        testSet.put("flooring", "floor");
        testSet.put("flying", "fly");
        testSet.put("folding", "fold");
        testSet.put("formatting", "format");
        testSet.put("freelancing", "freelance");
        testSet.put("funding", "fund");
        testSet.put("fundraising", "fundraise");
        testSet.put("gaming", "game");
        testSet.put("gardening", "garden");
        testSet.put("graphing", "graph");
        testSet.put("grilling", "grill");
        testSet.put("hacking", "hack");
        testSet.put("handwriting", "handwrite");
        testSet.put("healing", "heal");
        testSet.put("hiking", "hike");
        testSet.put("hiring", "hire");
        testSet.put("hitting", "hit");
        testSet.put("hosting", "host");
        testSet.put("housing", "house");
        testSet.put("imaging", "image");
        testSet.put("indexing", "index");
        testSet.put("interesting", "interest");
        testSet.put("interviewing", "interview");
        testSet.put("investing", "invest");
        testSet.put("knitting", "knit");
        testSet.put("learning", "learn");
        testSet.put("licensing", "license");
        testSet.put("lighting", "light");
        testSet.put("linking", "link");
        testSet.put("listening", "listen");
        testSet.put("living", "live");
        testSet.put("loading", "load");
        testSet.put("lodging", "lodge");
        testSet.put("logging", "log");
        testSet.put("mailing", "mail");
        testSet.put("manufacturing", "manufacture");
        testSet.put("mapping", "map");
        testSet.put("marketing", "market");
        testSet.put("meeting", "meet");
        testSet.put("messaging", "message");
        testSet.put("mining", "mine");
        testSet.put("mixing", "mix");
        testSet.put("modeling", "model");
        testSet.put("modelling", "model");
        testSet.put("monitoring", "monitor");
        testSet.put("moving", "move");
        testSet.put("naming", "name");
        testSet.put("networking", "network");
        testSet.put("nursing", "nurse");
        testSet.put("organizing", "organize");
        testSet.put("outsourcing", "outsource");
        testSet.put("packaging", "package");
        testSet.put("packing", "pack");
        testSet.put("painting", "paint");
        testSet.put("parenting", "parent");
        testSet.put("parsing", "parse");
        testSet.put("planning", "plan");
        testSet.put("podcasting", "podcast");
        testSet.put("positioning", "position");
        testSet.put("pricing", "price");
        testSet.put("printing", "print");
        testSet.put("processing", "process");
        testSet.put("profiling", "profile");
        testSet.put("programming", "program");
        testSet.put("publishing", "publish");
        testSet.put("quilting", "quilt");
        testSet.put("racing", "race");
        testSet.put("ranking", "rank");
        testSet.put("rating", "rate");
        testSet.put("reading", "read");
        testSet.put("recording", "record");
        testSet.put("recruiting", "recruit");
        testSet.put("recycling", "recycle");
        testSet.put("remodeling", "remodel");
        testSet.put("rendering", "render");
        testSet.put("reporting", "report");
        testSet.put("retouching", "retouch");
        testSet.put("ripping", "rip");
        testSet.put("roleplaying", "roleplay");
        testSet.put("running", "run");
        testSet.put("saving", "save");
        testSet.put("scaling", "scale");
        testSet.put("scanning", "scan");
        testSet.put("scheduling", "schedule");
        testSet.put("scraping", "scrape");
        testSet.put("scripting", "script");
        testSet.put("scrolling", "scroll");
        testSet.put("searching", "search");
        testSet.put("selling", "sell");
        testSet.put("sewing", "sew");
        testSet.put("sharing", "share");
        testSet.put("shipping", "ship");
        testSet.put("shopping", "shop");
        testSet.put("skinning", "skin");
        testSet.put("smashing", "smash");
        testSet.put("sorting", "sort");
        testSet.put("speaking", "speak");
        testSet.put("spelling", "spell");
        testSet.put("streaming", "stream");
        testSet.put("string", "string");
        testSet.put("studying", "study");
        testSet.put("surfing", "surf");
        testSet.put("tagging", "tag");
        testSet.put("taking", "take");
        testSet.put("teaching", "teach");
        testSet.put("testing", "test");
        testSet.put("thinking", "think");
        testSet.put("threading", "thread");
        testSet.put("tracking", "track");
        testSet.put("trading", "trade");
        testSet.put("training", "train");
        testSet.put("troubleshooting", "troubleshoot");
        testSet.put("tuning", "tune");
        testSet.put("typesetting", "typeset");
        testSet.put("typing", "type");
        testSet.put("volunteering", "volunteer");
        testSet.put("voting", "vote");
        testSet.put("walking", "walk");
        testSet.put("warming", "warm");
        testSet.put("wedding", "wed");
        testSet.put("writing", "write");

        /** ending with -ing, but no gerunds. */
        testSet.put("beijing", "beijing");
        testSet.put("ning", "ning");
        testSet.put("ping", "ping");
        testSet.put("spring", "spring");
        testSet.put("swing", "swing");
        

        for (Entry<String, String> entry : testSet.entrySet()) {
            String ingForm = entry.getKey();
            String normForm = entry.getValue();
            Assert.assertEquals(normForm, WordNet.gerundToInfinitive(ingForm));
        }
    }

}
