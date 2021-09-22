package org.cloudburstmc.server.network;


/**
 * recorder of the network statistics
 */
public class NetworkStatistics {

    private double upload = 0;

    private double download = 0;

    public void add(double upload, double download) {
        this.upload += upload;
        this.download += download;
    }

    public double getUpload() {
        return upload;
    }

    public double getDownload() {
        return download;
    }

    public void reset() {
        this.upload = 0;
        this.download = 0;
    }

}
