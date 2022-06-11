package com.darcangel.acam.factory;

import com.darcangel.acam.pallete.Arctic;
import com.darcangel.acam.pallete.Banded;
import com.darcangel.acam.pallete.Blackhot;
import com.darcangel.acam.pallete.DoubleRainbow;
import com.darcangel.acam.pallete.Fusion;
import com.darcangel.acam.pallete.Gray;
import com.darcangel.acam.pallete.Ironblack;
import com.darcangel.acam.pallete.Rainbow;
import com.darcangel.acam.pallete.Sepia;

public class PaletteFactory {
    private String[] paletteNames = {
            "Arctic",
            "Banded",
            "Blackhot",
            "DoubleRainbow",
            "Fusion",
            "Gray",
            "Ironblack",
            "Rainbow",
            "Sepia"
    };
    private int[][][] palettes = {
            Arctic.palette,
            Banded.palette,
            Blackhot.pallete,
            DoubleRainbow.palette,
            Fusion.palette,
            Gray.palette,
            Ironblack.palette,
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