package tud.iir.visualization.applets;

import java.awt.Color;

import javax.swing.JComponent;

import prefuse.Display;
import prefuse.Visualization;
import prefuse.action.ActionList;
import prefuse.action.RepaintAction;
import prefuse.action.assignment.ColorAction;
import prefuse.action.layout.graph.ForceDirectedLayout;
import prefuse.activity.Activity;
import prefuse.controls.DragControl;
import prefuse.controls.FocusControl;
import prefuse.controls.NeighborHighlightControl;
import prefuse.controls.PanControl;
import prefuse.controls.WheelZoomControl;
import prefuse.controls.ZoomControl;
import prefuse.controls.ZoomToFitControl;
import prefuse.data.Graph;
import prefuse.data.Tuple;
import prefuse.data.event.TupleSetListener;
import prefuse.data.io.GraphMLReader;
import prefuse.data.tuple.TupleSet;
import prefuse.render.DefaultRendererFactory;
import prefuse.render.LabelRenderer;
import prefuse.util.ColorLib;
import prefuse.util.GraphLib;
import prefuse.util.PrefuseLib;
import prefuse.util.force.ForceSimulator;
import prefuse.util.ui.JPrefuseApplet;
import prefuse.util.ui.UILib;
import prefuse.visual.NodeItem;
import prefuse.visual.VisualGraph;
import prefuse.visual.VisualItem;

public class PrefuseGraph extends JPrefuseApplet {

    private static final long serialVersionUID = -533270749017439499L;

    @Override
    public void init() {
        UILib.setPlatformLookAndFeel();
        JComponent graphview = createGraph("/ldweb.xml", "name");
        this.getContentPane().add(graphview);
    }

    public JComponent createGraph(String datafile, String label) {
        Graph g = null;
        if (datafile == null) {
            g = GraphLib.getGrid(15, 15);
        } else {
            try {
                g = new GraphMLReader().readGraph(datafile);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return showGraph(g, 48);
    }

    public JComponent showGraph(Graph graph, int focusNodeID) {
        // -- 2. the visualization --------------------------------------------

        // add the graph to the visualization as the data group "graph"
        // nodes and edges are accessible as "graph.nodes" and "graph.edges"
        final Visualization vis = new Visualization();
        VisualGraph vg = vis.addGraph("graph", graph);
        // vis.add("graph", graph);
        vis.setInteractive("graph.edges", null, false);

        // -- 3. the renderers and renderer factory ---------------------------

        TupleSet focusGroup = vis.getGroup(Visualization.FOCUS_ITEMS);
        focusGroup.addTupleSetListener(new TupleSetListener() {

            @Override
            public void tupleSetChanged(TupleSet ts, Tuple[] add, Tuple[] rem) {
                for (int i = 0; i < rem.length; ++i)
                    ((VisualItem) rem[i]).setFixed(false);
                for (int i = 0; i < add.length; ++i) {
                    ((VisualItem) add[i]).setFixed(false);
                    ((VisualItem) add[i]).setFixed(true);
                }
                vis.run("draw");

            }
        });

        // draw the "name" label for NodeItems
        LabelRenderer r = new LabelRenderer("name");
        r.setRoundedCorner(8, 8); // round the corners

        // create a new default renderer factory
        // return our name label renderer as the default for all non-EdgeItems
        // includes straight line edges for EdgeItems by default
        vis.setRendererFactory(new DefaultRendererFactory(r));

        // -- 4. the processing actions ---------------------------------------

        // create our nominal color palette
        // pink for females, baby blue for males
        // int[] palette = new int[] { ColorLib.rgb(190, 190, 255), ColorLib.rgb(200, 180, 180) };
        // map nominal data values to colors using our provided palette
        // DataColorAction fill = new DataColorAction("graph.nodes", "gender", Constants.NOMINAL, VisualItem.FILLCOLOR, palette);
        ColorAction fill = new ColorAction("graph.nodes", VisualItem.FILLCOLOR, ColorLib.gray(80));
        fill.add("_fixed", ColorLib.rgb(160, 160, 200));
        fill.add("_highlight", ColorLib.rgb(255, 130, 130));

        // use black for node text
        ColorAction text = new ColorAction("graph.nodes", VisualItem.TEXTCOLOR, ColorLib.gray(255));
        // use light grey for edges
        ColorAction edges = new ColorAction("graph.edges", VisualItem.STROKECOLOR, ColorLib.rgb(255, 50, 50));

        // create an action list containing all color assignments
        ActionList color = new ActionList(Activity.INFINITY);
        color.add(fill);
        color.add(text);
        color.add(edges);

        // create an action list with an animated layout
        // ActionList layout = new ActionList(Activity.DEFAULT_STEP_TIME);
        ActionList layout = new ActionList(Activity.INFINITY);
        // ActionList layout = new ActionList(5000,100);

        ForceDirectedLayout fdl = new ForceDirectedLayout("graph");
        ForceSimulator fsim = fdl.getForceSimulator();
        fsim.getForces()[0].setParameter(0, -10.0f);
        fsim.getForces()[1].setParameter(0, 0.02f);
        layout.add(fdl);

        layout.add(new RepaintAction());

        // add the actions to the visualization
        vis.putAction("color", color);
        vis.putAction("layout", layout);

        // -- 5. the display and interactive controls -------------------------

        Display d = new Display(vis);

        // set display size
        d.setSize(800, 500);
        d.setForeground(Color.GRAY);
        d.setBackground(Color.WHITE);

        // drag individual items around
        d.addControlListener(new DragControl());

        // pan with left-click drag on background
        d.addControlListener(new PanControl());

        // zoom with right-click drag
        d.addControlListener(new ZoomControl());

        // zoom with wheel
        d.addControlListener(new WheelZoomControl());

        d.addControlListener(new ZoomToFitControl());
        d.addControlListener(new NeighborHighlightControl());
        d.addControlListener(new FocusControl(1));

        // -- 6. launch the visualization -------------------------------------

        // create a new window to hold the visualization

        // assign the colors
        vis.run("color");
        // start up the animated layout
        vis.run("layout");

        // position and fix the default focus node
        NodeItem focus = (NodeItem) vg.getNode(focusNodeID);
        PrefuseLib.setX(focus, null, 400);
        PrefuseLib.setY(focus, null, 250);
        focusGroup.setTuple(focus);

        return d;
    }

}