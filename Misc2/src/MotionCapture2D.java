import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JLabel;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

public class MotionCapture2D extends MotionCapture{
	private static int cams=1;
	public MotionCapture2D(int trackingPnts) {
		
		super(cams, trackingPnts);	
		
		pos=new int[trackingPoints][2]; //2 for 2d
		raw=new int[cams][trackingPoints][2];		
		raw2=new int[cams][trackingPoints*2][2];//*2 for 2 for each color	
	}
	public void run() {

		setupStartingFrames();
		camera[0] = new VideoCapture();
		//camera[0] = new VideoCapture("C:\\Users\\suprm\\Pictures\\ef.mp4"); //open camera1 (x,y) axis
		camera[0].open(0);
		
		setupCameraSettings();
		camera[0].read(frames[0][0]);
		
		//System.out.println(frame.height());
		
		//turns on webcam view
		displayViews();
		//setting up the bounds for filters
		updateBounds();
	  	addUI();
	  	dimensions=getDimensions();
		while(true) {			
			//reads the frames
			camera[0].read(frames[0][0]);
			Mat[] frames= {this.frames[0][0]};
			//sets up the images to display on screen
			setImages(frames);					
			
			//recordPositions(raw,0);
			//Demo.moveMouse(raw[0][0][1], raw[0][0][0]);
			MotionVisualizer.update(raw2[0]);
			
		}
	}
	protected void recordPositions(float[][] positions,int num) { //p is the coordinates of the center of the rectangle for each camera. Num is the which camera.
//		for(int i=0; i<positions.length; i++) {
//			pos[num][0]=(int) (p[0]-dimensions[0]/2); //x coordinate
//			pos[num][1]=(int) (p[1]-dimensions[1]/2); //y coordinate
//		}
		
		
		//raw[num][0]=p[1];
		//raw[num][1]=p[0];
		//rawXY2[0]=p[0];//maybe remove these
		//rawXY2[1]=p[1];
	}
	
}
