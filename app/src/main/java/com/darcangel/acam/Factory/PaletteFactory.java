package com.darcangel.acam.Factory;

import com.darcangel.acam.pallete.Arctic;
import com.darcangel.acam.pallete.Banded;
import com.darcangel.acam.pallete.Blackhot;
import com.darcangel.acam.pallete.DoubleRainBow;
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
            "CoubleRainbow",
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
            DoubleRainBow.palette,
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