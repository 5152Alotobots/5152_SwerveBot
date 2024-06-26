package frc.robot.library.vision.limelight;

import edu.wpi.first.math.filter.LinearFilter;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.wpilibj.shuffleboard.BuiltInLayouts;
import edu.wpi.first.wpilibj.shuffleboard.Shuffleboard;
import edu.wpi.first.wpilibj.shuffleboard.ShuffleboardLayout;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.library.drivetrains.swervectre.CommandSwerveDrivetrain;
//import frc.robot.library.vision.limelight.util.DetectedObject;
//import frc.robot.library.vision.limelight.util.DetectedObjectList;

import java.util.Map;

import static frc.robot.library.vision.limelight.SubSys_Limelight_Constants.*;

public class SubSys_Limelight extends SubsystemBase {
    private CommandSwerveDrivetrain subSys_Drive;
    //private DetectedObjectList detectedObjectList = new DetectedObjectList();
    //private ShuffleboardLayout detectedObjectsShuffleboard;
    private LinearFilter txFilter;
    private double txFiltered;

    public SubSys_Limelight(CommandSwerveDrivetrain subSys_Drive) {
        this.subSys_Drive = subSys_Drive;
        //DetectedObject.setDrive(subSys_Drive);
        /*
        detectedObjectsShuffleboard = Shuffleboard
                .getTab("Limelight")
                .getLayout("Detected Objects", BuiltInLayouts.kGrid)
                .withProperties(Map.of("Number of columns", 1, "Number of rows", 3));
        detectedObjectsShuffleboard.addStringArray("Highest Confidence", () -> detectedObjectList.sortByConfidence().toArray());
        // Only log closest to robot if drive is available
        if (subSys_Drive != null) {
            detectedObjectsShuffleboard.addStringArray("Closest To Robot:", () -> detectedObjectList.sortByPose(subSys_Drive.getState().Pose).toArray());
        } else {
            detectedObjectsShuffleboard.addString("Closest To Robot:", () -> "Cannot get Pose from drive!");
        }
        */
        txFilter = LinearFilter.movingAverage(5);
        txFiltered = 0.0;
    }

    /**
     * Gets target distance from the camera
     *
     * @param targetHeight               distance from floor to center of target in meters
     * @param targetOffsetAngle_Vertical ty entry from limelight of target crosshair (in radians)
     * @return the distance to the target in meters
     */
    public double targetDistanceMetersCamera(
            double targetHeight,
            double targetOffsetAngle_Vertical) {
        double angleToGoalRadians = LL_OFFSET.getRotation().getY() + targetOffsetAngle_Vertical;
        return (targetHeight - LL_OFFSET.getZ()) / Math.tan(angleToGoalRadians);
    }

    public boolean getNoteDetected(){
        return LimelightLib.getTV(NN_LIMELIGHT);
    }

    public double getNoteTx(){
        return LimelightLib.getTX(NN_LIMELIGHT);
    }

    public double getNoteTxFilt(){
        return txFiltered;
    }


    @Override
    public void periodic() {

        // Returns false if JSON cannot be received
        boolean limelightConnected =
                !NetworkTableInstance.getDefault()
                        .getTable(NN_LIMELIGHT)
                        .getEntry("json")
                        .getString("")
                        .isEmpty();

        // If we are using getObject detection and we are connected
        if (OBJECT_DETECTION_ENABLED && limelightConnected) {
            // Get weather we have a detected getObject
            boolean objectDetected = LimelightLib.getTV(NN_LIMELIGHT);
            // If we do, get all JSON results

            // Update decay
            /*
            detectedObjectList.update();
            if (objectDetected) {
                LimelightLib.LimelightResults latestResults = LimelightLib.getLatestResults(NN_LIMELIGHT);

                // Loop through every result in the array
                for (LimelightLib.LimelightTarget_Detector detection : latestResults.targetingResults.targets_Detector) {
                    // compute the offsets in radians (DetectedObject uses radians)
                    double horizontalOffset = Math.toRadians(detection.tx);
                    double verticalOffset = Math.toRadians(-detection.ty); // MAKE CCW POS

                    // Compute the distance to the getObject
                    DetectedObject note = new DetectedObject(horizontalOffset, verticalOffset,DetectedObject.ObjectType.NOTE, LL_OFFSET);
                    detectedObjectList.add(new DetectedObjectList.DetectedObjectPair(note, detection.confidence));
                }
            }
            */

            if(objectDetected){
                // Filter Tx
                txFiltered = txFilter.calculate(LimelightLib.getTX(NN_LIMELIGHT));
            }else{
                txFiltered = 0.0;
                txFilter.reset();
            }
            SmartDashboard.putBoolean("LimelightObjectDetected", objectDetected);
            SmartDashboard.putNumber("Limelight_tx", LimelightLib.getTX(NN_LIMELIGHT));
            SmartDashboard.putNumber("Limelight_txFiltered", txFiltered);
        }
        
    }
}
