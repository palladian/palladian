package tud.iir.daterecognition.technique;

import java.util.ArrayList;
import java.util.HashMap;

public abstract class TechniqueDateRater<T> {

    public abstract HashMap<T, Double> rate(ArrayList<T> list);
}