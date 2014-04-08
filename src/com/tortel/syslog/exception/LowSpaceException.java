package com.tortel.syslog.exception;

/**
 * And exception for when there is low free space on the primary storage
 */
public class LowSpaceException extends Exception {
    private static final long serialVersionUID = 4701987157679370945L;
    private double size;
    
    public LowSpaceException(double size){
        this.size = size;
    }
    
    /**
     * Get the free space (In MB)
     * @return
     */
    public double getFreeSpace(){
        return size;
    }
}
