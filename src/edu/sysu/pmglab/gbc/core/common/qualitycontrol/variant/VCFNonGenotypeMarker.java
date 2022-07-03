package edu.sysu.pmglab.gbc.core.common.qualitycontrol.variant;

import edu.sysu.pmglab.commandParser.types.STRING;
import edu.sysu.pmglab.container.VolumeByteStream;
import edu.sysu.pmglab.container.array.IntArray;
import edu.sysu.pmglab.easytools.ByteCode;

import java.util.HashMap;
import java.util.Map;

/**
 * @author suranyi
 */

public class VCFNonGenotypeMarker {
    final VolumeByteStream lineCache;
    final HashMap<String, String> genotypeFormat = new HashMap<>();
    final IntArray pointers = new IntArray(9);
    private Map<String, String> InfoDict;

    public VCFNonGenotypeMarker(VolumeByteStream lineCache) {
        this.lineCache = lineCache;
    }

    public VCFNonGenotypeMarker update() {
        this.InfoDict = null;
        this.pointers.clear();
        int index = 0;
        for (byte code : lineCache) {
            if (code == ByteCode.TAB) {
                this.pointers.add(index);
            }

            if (this.pointers.size() == this.pointers.getCapacity()) {
                return this;
            }
            index++;
        }

        if (this.pointers.size() < this.pointers.getCapacity()) {
            // 无基因型时
            this.pointers.add(lineCache.size());
        }

        return this;
    }

    public HashMap<String, String> formatGenotype(int seek, int length, String[] formatter) {
        this.genotypeFormat.clear();
        String[] values = new String(lineCache.getCache(), seek, length).split(":");
        for (int i = 0, l = Math.min(formatter.length, values.length); i < l; i++) {
            this.genotypeFormat.put(formatter[i], values[i]);
        }
        return genotypeFormat;
    }

    public String getChromosome() {
        return new String(lineCache.getCache(), 0, this.pointers.get(0));
    }

    public int getPosition() {
        int position = 0;
        for (int i = this.pointers.get(0) + 1, end = this.pointers.get(1); i < end; i++) {
            position = position * 10 + (this.lineCache.cacheOf(i) - 48);
        }
        return position;
    }

    public String getID() {
        int start = this.pointers.get(1) + 1;
        return new String(this.lineCache.getCache(), start, this.pointers.get(2) - start);
    }

    public byte[] getREF() {
        return this.lineCache.cacheOf(this.pointers.get(2) + 1, this.pointers.get(3));
    }

    public byte[] getALT() {
        return this.lineCache.cacheOf(this.pointers.get(3) + 1, this.pointers.get(4));
    }

    public double getQUAL() {
        int start = this.pointers.get(4) + 1;
        int length = this.pointers.get(5) - start;

        if (length == 1 && lineCache.cacheOf(start) == ByteCode.PERIOD) {
            return Double.NaN;
        } else {
            try {
                return Double.parseDouble(new String(lineCache.getCache(), start, length));
            } catch (NumberFormatException e) {
                return Double.NaN;
            }
        }
    }

    public String getFilter() {
        int start = this.pointers.get(5) + 1;
        return new String(this.lineCache.getCache(), start, this.pointers.get(6) - start);
    }

    public Map<String, String> getInfo() {
        if (InfoDict == null) {
            int start = this.pointers.get(6) + 1;
            InfoDict = (Map<String, String>) STRING.MAP_SEMICOLON.convert(new String(this.lineCache.getCache(), start, this.pointers.get(7) - start));
        }
        return InfoDict;
    }

    public String getFormat() {
        int start = this.pointers.get(7) + 1;
        return new String(this.lineCache.getCache(), start, this.pointers.get(8) - start);
    }

    public int getGenotypeStart() {
        return this.pointers.get(-1);
    }
}
