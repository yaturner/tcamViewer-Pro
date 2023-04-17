package com.darcangel.tcamViewer.model;

import com.darcangel.tcamViewer.constants.Constants;
import com.darcangel.tcamViewer.utils.CameraUtils;

import java.util.Date;
import java.util.Locale;

import io.sentry.Sentry;

public class RecordingDto {
    private Date startDate, endDate;
    private int numFrames;
    private int version;

    public RecordingDto() {
        numFrames = 0;
        startDate = new Date();
        version = 1;
    }

    public RecordingDto(final String recordingFooter) {

    }

    public String generateFooter(final Date endDate) {
        try {
            String start = CameraUtils.sdfRecording.format(startDate);
            String end = CameraUtils.sdfRecording.format(endDate);
            String[] startWords = start.split(" ");
            String[] endWords = end.split(" ");
            //frames are 1 based not 0
            String result = String.format(Locale.getDefault(), Constants.RECORDING_FOOTER, startWords[1], startWords[0],
                    endWords[1], endWords[0], numFrames+1, version);
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            Sentry.captureException(e);
            return null;
        }
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public int getNumFrames() {
        return numFrames;
    }

    public void setNumFrames(int numFrames) {
        this.numFrames = numFrames;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public void incrFrameCount() {
        numFrames = numFrames + 1;
    }
}
