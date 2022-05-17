package com.darcangel.acam.ui;

import android.util.Pair;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Singleton;

@Singleton
public class Emissivity {
    public static List<Pair<String, Integer>> emissivityList;

    public Emissivity() {
        emissivityList = new ArrayList<>();
        emissivityList.add(new Pair<String, Integer>("Aluminum, polished", 5));
        emissivityList.add(new Pair<String, Integer>("Aluminum, oxidized", 25));
        emissivityList.add(new Pair<String, Integer>("Brass, tarnished", 22));
        emissivityList.add(new Pair<String, Integer>("Brass, polished", 3));
        emissivityList.add(new Pair<String, Integer>("Brick, common", 85));
        emissivityList.add(new Pair<String, Integer>("Brick, plastered", 94));
        emissivityList.add(new Pair<String, Integer>("Carbon", 96));
        emissivityList.add(new Pair<String, Integer>("Chipboard, untreated", 90));
        emissivityList.add(new Pair<String, Integer>("Clay, fired", 91));
        emissivityList.add(new Pair<String, Integer>("Concrete", 95));
        emissivityList.add(new Pair<String, Integer>("Elec Tape, Black", 96));
        emissivityList.add(new Pair<String, Integer>("Enamel", 90));
        emissivityList.add(new Pair<String, Integer>("Formica", 93));
        emissivityList.add(new Pair<String, Integer>("Soil", 93));
        emissivityList.add(new Pair<String, Integer>("Glass Pane", 97));
        emissivityList.add(new Pair<String, Integer>("Granite", 86));
        emissivityList.add(new Pair<String, Integer>("Iron, hot rolled", 77));
        emissivityList.add(new Pair<String, Integer>("Iron sheet, galvanized", 28));
        emissivityList.add(new Pair<String, Integer>("Lacquer, black", 97));
        emissivityList.add(new Pair<String, Integer>("Lacquer, white", 87));
        emissivityList.add(new Pair<String, Integer>("Lead, oxidized", 63));
        emissivityList.add(new Pair<String, Integer>("Leather, tanned", 77));
        emissivityList.add(new Pair<String, Integer>("Oil, thick", 82));
        emissivityList.add(new Pair<String, Integer>("Paint, oil, avg", 94));
        emissivityList.add(new Pair<String, Integer>("Paper, white", 90));
        emissivityList.add(new Pair<String, Integer>("Plasterboard", 90));
        emissivityList.add(new Pair<String, Integer>("Plastic, PCB", 91));
        emissivityList.add(new Pair<String, Integer>("Plastic, PVC", 93));
        emissivityList.add(new Pair<String, Integer>("Porcelain, glazed", 92));
        emissivityList.add(new Pair<String, Integer>("Rubber", 94));
        emissivityList.add(new Pair<String, Integer>("Snow", 80));
        emissivityList.add(new Pair<String, Integer>("Steel, rolled", 50));
        emissivityList.add(new Pair<String, Integer>("Tar Paper", 92));
        emissivityList.add(new Pair<String, Integer>("Varnish, oak floor", 92));
        emissivityList.add(new Pair<String, Integer>("Water", 98));
        emissivityList.add(new Pair<String, Integer>("Wood, plywood", 82));
    };
}
