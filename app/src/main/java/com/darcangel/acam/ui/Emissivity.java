package com.darcangel.acam.ui;

import android.util.Pair;

import java.util.ArrayList;
import java.util.List;

public class Emissivity {
    public static List<Pair<String, Integer>> emissivityList;

    public Emissivity() {
        emissivityList = new ArrayList<>();
        emissivityList.add(new Pair("Aluminum, polished", 5));
        emissivityList.add(new Pair("Aluminum, oxidized", 25));
        emissivityList.add(new Pair("Brass, tarnished", 22));
        emissivityList.add(new Pair("Brass, polished", 3));
        emissivityList.add(new Pair("Brick, common", 85));
        emissivityList.add(new Pair("Brick, plastered", 94));
        emissivityList.add(new Pair("Carbon", 96));
        emissivityList.add(new Pair("Chipboard, untreated", 90));
        emissivityList.add(new Pair("Clay, fired", 91));
        emissivityList.add(new Pair("Concrete", 95));
        emissivityList.add(new Pair("Elec Tape, Black", 96));
        emissivityList.add(new Pair("Enamel", 90));
        emissivityList.add(new Pair("Formica", 93));
        emissivityList.add(new Pair("Soil", 93));
        emissivityList.add(new Pair("Glass Pane", 97));
        emissivityList.add(new Pair("Granite", 86));
        emissivityList.add(new Pair("Iron, hot rolled", 77));
        emissivityList.add(new Pair("Iron sheet, galvanized", 28));
        emissivityList.add(new Pair("Lacquer, black", 97));
        emissivityList.add(new Pair("Lacquer, white", 87));
        emissivityList.add(new Pair("Lead, oxidized", 63));
        emissivityList.add(new Pair("Leather, tanned", 77));
        emissivityList.add(new Pair("Oil, thick", 82));
        emissivityList.add(new Pair("Paint, oil, avg", 94));
        emissivityList.add(new Pair("Paper, white", 90));
        emissivityList.add(new Pair("Plasterboard", 90));
        emissivityList.add(new Pair("Plastic, PCB", 91));
        emissivityList.add(new Pair("Plastic, PVC", 93));
        emissivityList.add(new Pair("Porcelain, glazed", 92));
        emissivityList.add(new Pair("Rubber", 94));
        emissivityList.add(new Pair("Snow", 80));
        emissivityList.add(new Pair("Steel, rolled", 50));
        emissivityList.add(new Pair("Tar Paper", 92));
        emissivityList.add(new Pair("Varnish, oak floor", 92));
        emissivityList.add(new Pair("Water", 98));
        emissivityList.add(new Pair("Wood, plywood", 82));
    };
}
