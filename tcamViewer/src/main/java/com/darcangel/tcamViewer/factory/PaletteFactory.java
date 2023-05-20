package com.darcangel.tcamViewer.factory;

import com.darcangel.tcamViewer.pallete.Arctic;
import com.darcangel.tcamViewer.pallete.Banded;
import com.darcangel.tcamViewer.pallete.Blackhot;
import com.darcangel.tcamViewer.pallete.DoubleRainbow;
import com.darcangel.tcamViewer.pallete.Fusion;
import com.darcangel.tcamViewer.pallete.Gray;
import com.darcangel.tcamViewer.pallete.Ironblack;
import com.darcangel.tcamViewer.pallete.Isotherm;
import com.darcangel.tcamViewer.pallete.Rainbow;
import com.darcangel.tcamViewer.pallete.Sepia;

public class PaletteFactory {
    private final String[] paletteNames = {
            "Arctic",
            "Banded",
            "Blackhot",
            "DoubleRainbow",
            "Fusion",
            "Gray",
            "Ironblack",
            "Isotherm",
            "Rainbow",
            "Sepia"
    };
    private final int[][][] palettes = {
            Arctic.palette,
            Banded.palette,
            Blackhot.pallete,
            DoubleRainbow.palette,
            Fusion.palette,
            Gray.palette,
            Ironblack.palette,
            Isotherm.palette,
            Rainbow.palette,
            Sepia.palette
    };

    public String[] getPaletteNames() {
        return paletteNames;
    }

    public String getPaletteName(int index) {
        if(index < paletteNames.length) {
            return paletteNames[index];
        } else {
            return null;
        }
    }

    public int[][] getPaletteByName(final String name) {
        for(int index=0; index<paletteNames.length; index++) {
            if(paletteNames[index].equalsIgnoreCase(name)) {
                return palettes[index];
            }
        }
        return null;
    }
}