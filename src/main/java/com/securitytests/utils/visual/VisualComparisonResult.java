package com.securitytests.utils.visual;

import java.util.Date;

/**
 * Represents the result of a visual comparison between screenshots
 */
public class VisualComparisonResult {
    private final boolean hasDifference;
    private final double diffSize;
    private final String baselineImagePath;
    private final String actualImagePath;
    private final String diffImagePath;
    private final Date timestamp;
    
    /**
     * Create a new visual comparison result
     * @param hasDifference Whether there is a difference between the images
     * @param diffSize Size of the difference (number of different pixels)
     * @param baselineImagePath Path to the baseline image
     * @param actualImagePath Path to the actual image
     * @param diffImagePath Path to the difference image (if any)
     */
    public VisualComparisonResult(boolean hasDifference, double diffSize, 
                                String baselineImagePath, String actualImagePath, 
                                String diffImagePath) {
        this.hasDifference = hasDifference;
        this.diffSize = diffSize;
        this.baselineImagePath = baselineImagePath;
        this.actualImagePath = actualImagePath;
        this.diffImagePath = diffImagePath;
        this.timestamp = new Date();
    }
    
    /**
     * Check if there is a difference
     * @return True if there is a difference, false otherwise
     */
    public boolean hasDifference() {
        return hasDifference;
    }
    
    /**
     * Get the size of the difference (number of different pixels)
     * @return Difference size
     */
    public double getDiffSize() {
        return diffSize;
    }
    
    /**
     * Get the path to the baseline image
     * @return Baseline image path
     */
    public String getBaselineImagePath() {
        return baselineImagePath;
    }
    
    /**
     * Get the path to the actual image
     * @return Actual image path
     */
    public String getActualImagePath() {
        return actualImagePath;
    }
    
    /**
     * Get the path to the difference image
     * @return Difference image path, or null if no difference
     */
    public String getDiffImagePath() {
        return diffImagePath;
    }
    
    /**
     * Get the timestamp of this comparison
     * @return Comparison timestamp
     */
    public Date getTimestamp() {
        return timestamp;
    }
    
    /**
     * Get the difference percentage
     * @param totalPixels Total number of pixels in the image
     * @return Difference percentage (0-100)
     */
    public double getDiffPercentage(int totalPixels) {
        if (totalPixels <= 0) return 0;
        return (diffSize / totalPixels) * 100.0;
    }
    
    /**
     * Check if this is a new baseline (no comparison was made)
     * @return True if this is a new baseline, false otherwise
     */
    public boolean isNewBaseline() {
        return baselineImagePath != null && actualImagePath != null && diffImagePath == null && !hasDifference;
    }
    
    /**
     * Check if the comparison failed due to an error
     * @return True if there was an error during comparison
     */
    public boolean isError() {
        return hasDifference && diffImagePath == null;
    }
    
    @Override
    public String toString() {
        if (isNewBaseline()) {
            return "New baseline created: " + baselineImagePath;
        } else if (isError()) {
            return "Error during comparison";
        } else if (hasDifference) {
            return String.format("Visual difference detected: %f pixels different, baseline: %s, actual: %s, diff: %s", 
                diffSize, baselineImagePath, actualImagePath, diffImagePath);
        } else {
            return "No visual difference detected";
        }
    }
}
