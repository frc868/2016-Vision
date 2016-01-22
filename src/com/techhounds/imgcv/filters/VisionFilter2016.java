/*
 * Copyright (c) 2013, Paul Blankenbaker
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package com.techhounds.imgcv.filters;

import com.techhounds.imgcv.PolygonCv;
import edu.wpi.first.wpilibj.networktables.NetworkTable;
import java.util.ArrayList;
import java.util.List;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

/**
 * A image filter which performs all of the operations in an attempt to locate
 * the rectangular target areas from the 2013 FRC competition.
 *
 * @author Paul Blankenbaker
 */
public final class VisionFilter2016 implements MatFilter {
	
	//Configurations
	
	private static int[]	colorFilterMin    = {60, 100, 20}; //TODO make all final as well
	private static int[]	colorFilterMax    = {90, 255, 255};
	private static double[] bestTargetColors  = {100, 100, 255};
	private static double[] otherTargetColors = {255, 100, 100};
	private static int      targetOutlineThickness   = 1;
	private static int		blackWhiteThresh  = 40;
	private static int		dilateFactor      = 3;
	private static int		erodeFactor       = 5;
	
	private static double	polygonEpsilon    = 5.0; //used for detecting polygons from contours
	private static double	targetHeightMin   = 10.0; 
	private static double	targetWidthMin    = 50.0;
	private static int		targetSidesMin    = 2; //ie at least 3 sides
	
	private static double   targetHeightIdeal = 30;
	private static double   targetWidthIdeal  = 150;
	private static double   targetRatioIdeal  = 0.15;
	private static double   targetAreaIdeal   = 7500;
	
	private static double	targetTapeWidth   = 24; //inches
	private static double	cameraHorizFOV    = 67; //could be wrong
	private static double	cameraResolutionX = 800;

	private NetworkTable	networkTable;
	private double          frameCount        = 0;
	
	//Processing Filters
	
    private final MatFilter		_ColorRange; //Used to filter image to a specific color range.
    private final Dilate		_Dilate;     //grows remaining parts of the images
    private final Erode			_Erode;      //shrinks remaining parts of the images
    private final GrayScale		_GrayScale; //Used to convert image to a gray scale rendering.
    private final BlackWhite	_BlackWhite; //Used to convert from gray scale to black and white.
    private final CrossHair     _CrossHair;  //used to draw a crosshair
    private final Scalar        _BestTargetOverlay;
    private final Scalar		_OtherTargetOverlay;

    //Constructs a new instance by pre-allocating all of our image filtering objects.
    public VisionFilter2016() { 
    	_ColorRange = createHsvColorRange();
    	_Dilate 	= new Dilate(dilateFactor);  
    	_Erode		= new Erode(erodeFactor); 
        _GrayScale  = new GrayScale();
        _BlackWhite = createBlackWhite(); //TODO can we move these to separate filters?
        _CrossHair  = new CrossHair();    //or possibly make our own methods for each of these?
        _BestTargetOverlay = new Scalar(bestTargetColors);
        _OtherTargetOverlay = new Scalar(otherTargetColors);
    }
    
    public static MatFilter createDilate() {
    	return new Dilate(dilateFactor);
    }
    
    public static MatFilter createErode() {
    	return new Erode(erodeFactor);
    }

    /**
     * Helper method to provide a single location that creates the image filter
     * used to go from a gray scale image to a black and white image.
     *
     * @return A image filter that converts a gray scale image to a black and
     * white image.
     */
    public static BlackWhite createBlackWhite() {
        return new BlackWhite(blackWhiteThresh, 255, false);
    }
    
    public static MatFilter createHsvColorRange() {
        Sequence filter = new Sequence();
        filter.addFilter(ColorSpace.createBGRtoHSV());
        filter.addFilter(new ColorRange(colorFilterMin, colorFilterMax, true));
        return filter;
    }

    /**
     * Method to filter a source image and return the filtered results.
     *
     * @param srcImage - The source image to be processed (passing {@code null}
     * is not permitted).
     *
     * @return The original image with overlay information applied (we do a lot
     * of filtering and try to locate the 2013 FRC rectangular target regions).
     */
    @Override
    public Mat process(Mat srcImage) {
    	List<PolygonCv> targets = new ArrayList<>();  //list of potential targets in image
        Mat processedImage      = new Mat();
        PolygonCv bestTarget;
        int targetsFound;
        
        processedImage = primaryProcessing(srcImage.clone()); //creates new color processed image
        targets = findTargets(processedImage); //detects targets in new processed image
        targetsFound = targets.size();
        
        if(targetsFound > 0) {
        	bestTarget = findBestTarget(targets);
        	
        	if(networkTable != null) { 
        		targetAnalysis(bestTarget); //TODO pass BEST not FIRST polygon
        		networkTable.putNumber("FrameCount", frameCount++); 
        	}
        	
        	return drawTargets(srcImage.clone(), targets, bestTarget); //always draw targets if found
        	
         } else { //unnecessary
         	return drawCrossHair(srcImage.clone()); //always draw crosshairs regardless
         } //don't need to increment frameCount as no target (ie usable data) is detected
    }
    
    private Mat primaryProcessing(Mat inputImage) { //does basic color/erosion processing
    	_ColorRange.process(inputImage); 
        _Dilate.process(inputImage); //what if we erode first?
        _Erode.process(inputImage);
        _GrayScale.process(inputImage);
        _BlackWhite.process(inputImage);
        //blur is done via camera focus, not here
        
        return inputImage; //convenience sake, not necessary
    }
    
    private List<PolygonCv> findTargets(Mat inputImage) { //finds potential targets in an image
	    Mat hierarchy 	          = new Mat();
	    List<MatOfPoint> contours = new ArrayList<>(); //list of objects in image
        List<PolygonCv>  targets  = new ArrayList<>(); //list of potential targets in image
        PolygonCv  		 currentTarget;
        
        Imgproc.findContours(inputImage, contours, hierarchy, //doesn't modify inputImage
        					 Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
        
        for(int i = 0; i < contours.size(); i++) { //for each 'contour' object
        	currentTarget = PolygonCv.fromContour(contours.get(i), polygonEpsilon); //create a polygon
        	        	
        	if(currentTarget.getHeight() 	> targetHeightMin && 
        	   currentTarget.getWidth() 	> targetWidthMin  && 
        	   currentTarget.size() 		> targetSidesMin) {
        		
        		targets.add(currentTarget); //if within range, add to list of potential targets
        	} //TODO is the array list necessary?
        }
        
        return targets;
    }
 
    private PolygonCv findBestTarget(List<PolygonCv> targetList) {
    	PolygonCv bestTarget = null;
    	PolygonCv currentTarget;
    	int       bestTargetValue = 0;
    	
    	for(int i = 0; i < targetList.size(); i++) {
    		currentTarget = targetList.get(i);
    		
    		if(getTargetRating(currentTarget) > bestTargetValue)  {
    			bestTarget = currentTarget;
    		}
    	}
    	
    	return bestTarget;
    }
    
    private int getTargetRating(PolygonCv inputTarget) {
    	int targetRating = 1000;
    	
    	targetRating += -(Math.abs(inputTarget.getHeight() - targetHeightIdeal));
    	targetRating += -(Math.abs(inputTarget.getWidth() - targetWidthIdeal));
    	
    	return targetRating;
    }
    
    private void targetAnalysis(PolygonCv foundTarget) { //tells the robo info about the target
        double offCenterDegreesX, targetDistance;
        float targetWidth 	= foundTarget.getWidth();
        float targetX		= foundTarget.getCenterX();
    	
    	offCenterDegreesX = ((targetX / (cameraResolutionX / 2)) - 1) * cameraHorizFOV;
    	
    	targetDistance = (targetTapeWidth / 2) / 
    					 	Math.tan(
    							 Math.toRadians(
    									 (targetWidth / cameraResolutionX) * (cameraHorizFOV / 2)));
    	
    	networkTable.putNumber("OffCenterDegreesX", offCenterDegreesX);
    	networkTable.putNumber("DistanceToTarget",  targetDistance);
    }

    private Mat drawTargets(Mat inputImage, List<PolygonCv> targetList, PolygonCv bestTarget) {
    	List<MatOfPoint> contours = new ArrayList<>();
    	List<MatOfPoint> bestContours = new ArrayList<>();
    	PolygonCv        currentTarget; 
    	
    	bestContours.add(bestTarget.toContour());
    	
    	for(int i = 0; i < targetList.size(); i++) {
    		currentTarget = targetList.get(i);
    		
    		contours.add(currentTarget.toContour());
    		currentTarget.drawInfo(inputImage, _OtherTargetOverlay);
    	} //TODO avoid drawing best target twice, maybe remove from master target list
    	
    	Imgproc.drawContours(inputImage, contours, -1, _OtherTargetOverlay, targetOutlineThickness); 
    	Imgproc.drawContours(inputImage, bestContours, -1, _BestTargetOverlay, targetOutlineThickness);
    	//TODO figure out what the -1 is for
    	
    	drawCrossHair(inputImage);
    	
    	return inputImage; //again only for convenience, such as when clone is passed as argument
    }
    
    private Mat drawCrossHair(Mat inputImage) {
    	return _CrossHair.process(inputImage);
    }
    
    public void setNetworkTable(NetworkTable nt) {
    	networkTable = nt;
    }
}


