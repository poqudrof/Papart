/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.inria.papart.multitouch.detection;

import fr.inria.papart.calibration.files.PlanarTouchCalibration;
import fr.inria.papart.multitouch.tracking.TouchPointTracker;
import fr.inria.papart.multitouch.tracking.TrackedElement;
import fr.inria.papart.procam.Papart;
import fr.inria.papart.procam.PaperScreen;
import fr.inria.papart.utils.MathUtils;
import static fr.inria.papart.utils.MathUtils.absd;
import static fr.inria.papart.utils.MathUtils.constrain;
import java.util.ArrayList;
import processing.core.PConstants;
import processing.core.PGraphics;
import tech.lity.rea.colorconverter.ColorConverter;

/**
 * [experimental] Similar to the ColorTracker but for all the calibrated colors.
 *
 * @author Jérémy Laviole
 */
public class CalibratedColorTracker extends ColorTracker {

    int numberOfRefs = 3;
    private final ColorReferenceThresholds references[];
    TouchDetectionLargeColor largeDetectionColor;

    public CalibratedColorTracker(PaperScreen paperScreen, float scale) {
        super(paperScreen, scale);

        references = new ColorReferenceThresholds[numberOfRefs];

        // Load all the colors. 
        for (int fileId = 0; fileId < numberOfRefs; fileId++) {
            String fileName = Papart.colorThresholds + fileId + ".txt";
            String[] list = Papart.getPapart().getApplet().loadStrings(fileName);

            references[fileId] = new ColorReferenceThresholds();

            for (String data : list) {
                references[fileId].loadParameter(data);
            }
            System.out.println("Ref: " + fileId + " "
                    + " A " + references[fileId].averageA
                    + " B " + references[fileId].averageB);
        }
    }
    PlanarTouchCalibration largerTouchCalibration;

    @Override
    public void initTouchDetection() {
        super.initTouchDetection();

        largeDetectionColor = new TouchDetectionLargeColor(trackedView);
        largerTouchCalibration = Papart.getPapart().getDefaultColorZoneCalibration();
        
        largerTouchCalibration.setMaximumDistance(largerTouchCalibration.getMaximumDistance() * scale);
        largerTouchCalibration.setMinimumComponentSize((int) (largerTouchCalibration.getMinimumComponentSize() * scale * scale)); // Quadratic (area)
        largerTouchCalibration.setSearchDepth((int) (largerTouchCalibration.getSearchDepth() * scale));
        largerTouchCalibration.setTrackingMaxDistance(largerTouchCalibration.getTrackingMaxDistance() * scale);
        largerTouchCalibration.setMaximumRecursion((int) (largerTouchCalibration.getMaximumRecursion() * scale));

        largeDetectionColor.setCalibration(largerTouchCalibration);

        System.out.println("Second Calibration loaded");
        // share the colorFoundArray ?
        largeColorFoundArray = largeDetectionColor.createInputArray();

    }    
    protected byte[] largeColorFoundArray;

    public int getReferenceColor(int id) {
        return references[id].getReferenceColor();
    }

    @Override
    public ArrayList<TrackedElement> findColor(int time) {

        int currentImageTime = paperScreen.getCameraTracking().getTimeStamp();

        // once per image
        if (lastImageTime == currentImageTime) {
            // return the last known points. 
            return trackedElements;
        }
        lastImageTime = currentImageTime;

        // Get the image
        capturedImage = trackedView.getViewOf(paperScreen.getCameraTracking());
        capturedImage.loadPixels();

        // Reset the colorFoundArray
        touchDetectionColor.resetInputArray();

        // Default to RGB 255 for now, for color distances. 
        paperScreen.getGraphics().colorMode(PConstants.RGB, 255);

        // Tag each pixels
        for (int x = 0; x < capturedImage.width; x++) {
            for (int y = 0; y < capturedImage.height; y++) {
                int offset = x + y * capturedImage.width;
                int c = capturedImage.pixels[offset];

                // for each except the last DEBUG
//               byte id = 0;
                for (byte id = 0; id < numberOfRefs; id++) {

                    reference = references[id];
                    boolean good = false;

//                    if (id == 0) {
//                        good = MathUtils.colorFinderHSBRedish(paperScreen.getGraphics(),
//                                reference.referenceColor, c, reference.hue, reference.saturation, reference.brightness);
//                    } else {
                    good = colorFinderLAB(paperScreen.getGraphics(),
                            c, reference);
//                        good = MathUtils.colorFinderHSB(paperScreen.getGraphics(),
//                                c, reference.referenceColor, reference.hue, reference.saturation, reference.brightness);
//                    }
                    // HSB only for now.
                    if (good) {
//                    if (references[id].colorFinderHSB(paperScreen.getGraphics(), c)) {
                        colorFoundArray[offset] = id;
                    }

                }
            }
        }

        int erosion = 0;

        // Step1 -> small-scale colors (gomettes)
// EROSION by color ?!
//        ArrayList<TrackedElement> newElements
//                = touchDetectionColor.compute(time, erosion, this.scale);
      smallElements = touchDetectionColor.compute(time, erosion, this.scale);
        
        ///
        System.arraycopy(colorFoundArray, 0, largeColorFoundArray, 0, colorFoundArray.length);
        
        ArrayList<TrackedElement> newElements2
                = largeDetectionColor.compute(time, erosion, this.scale);
//
        // Step 2 -> Large -scale colors (ensemble de gomettes) 
//        TouchPointTracker.trackPoints(trackedElements, smallElements, time);
        trackedElements.clear();
        trackedElements.addAll(smallElements);
        TouchPointTracker.trackPoints(trackedLargeElements, newElements2, time);
        
//        for(TrackedElement te : trackedElements){
//            te.filter(time);
//        }

//        return trackedElements;
        return trackedLargeElements;
    }
    
     ArrayList<TrackedElement> smallElements;
       protected final ArrayList<TrackedElement> trackedLargeElements = new ArrayList<>();
    public  ArrayList<TrackedElement> smallElements(){
        return trackedElements;
    }
    
    
    
    public static ColorConverter converter = new ColorConverter();

    /**
     * Color distance on the LAB scale. The incomingPix is compared with the
     * baseline. The method returns true if each channel validates the condition
     * for the given threshold.
     *
     * @param g
     * @param baseline
     * @param incomingPix
     * @param LTresh
     * @param ATresh
     * @param BTresh
     * @return
     */
    public static boolean colorFinderLAB(PGraphics g, int baseline, int incomingPix,
            float LTresh, float ATresh, float BTresh) {

        double[] labBase = converter.RGBtoLAB((int) g.red(baseline), (int) g.green(baseline), (int) g.blue(baseline));
        double[] labIncoming = converter.RGBtoLAB((int) g.red(incomingPix), (int) g.green(incomingPix), (int) g.blue(incomingPix));
        return labIncoming[0] > 50.0 // Very large light base
                //                absd(labBase[0] - labIncoming[0]) < LTresh * 20  // Very large light base
                && absd(labBase[1] - labIncoming[1]) < ATresh
                && absd(labBase[2] - labIncoming[2]) < BTresh;
    }

    /**
     * Color distance on the LAB scale. The incomingPix is compared with the
     * baseline. The method returns true if each channel validates the condition
     * for the given threshold.
     *
     * @param g
     * @param ref
     * @return
     */
    public static boolean colorFinderLAB(PGraphics g, int incomingPix,
            ColorReferenceThresholds ref) {

        double[] lab = converter.RGBtoLAB((int) g.red(incomingPix), (int) g.green(incomingPix), (int) g.blue(incomingPix));

        double l = constrain(lab[0], 0, 100);
        double A = constrain(lab[1], -128, 128);
        double B = constrain(lab[2], -128, 128);
        
        double d
                = Math.sqrt(Math.pow(l - ref.averageL, 2)
                        + Math.pow(A - ref.averageA, 2)
                        + Math.pow(B - ref.averageB, 2));

//        System.out.println("d: "  + d);
        return d < (ref.AThreshold + ref.BThreshold + ref.LThreshold) * 3f;
    }
    
}
