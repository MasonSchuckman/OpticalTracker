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

public class MotionCapture3D extends MotionCapture{
	private static int cams=2;
	public MotionCapture3D(int trackingPnts) {	
		
		super(cams, trackingPnts);	
		
		pos=new int[trackingPoints][3]; //3 for 3d
		raw=new int[cams][trackingPoints][3];	
	}
	public void run() {

		setupStartingFrames();
		for(int i=0; i<2; i++) { //open cameras
			camera[i] = new VideoCapture(); 
			camera[i].open(i);
		}
		
		setupCameraSettings();
		
		for(int i=0; i<2; i++) { 			
			//convert to grayscale and set up the first frame
			
			camera[i].read(frames[i][0]);
			
			Imgproc.cvtColor(frames[i][0], frames[i][1], Imgproc.COLOR_BGR2GRAY);
			Imgproc.GaussianBlur(frames[i][1], frames[i][1], new Size(21, 21), 0);
		}
		
		//turns on webcam view
		displayViews();
		//setting up the bounds for filters
		updateBounds();
	  	addUI();
	  	dimensions=getDimensions();
		while(true) {	
			
			//reads the frames
			for(int i=0; i<2; i++) { //open cameras
				camera[i].read(frames[i][0]);
			}			
			Mat[] frames= {this.frames[0][0],this.frames[1][0]};
			
			//sets up the images to display on screen
			setImages(frames);						
		}
	}
	
	protected void recordPositions(float[] p,int num) { //p is the coordinates of the center of the rectangle for each camera. Num is the which camera.
				
		rawXY1[0]=p[0];
		rawXY1[1]=p[1];
		pos[num][0]=(int) (p[0]-dimensions[0]/2); //x coordinate
		pos[num][1]=(int) (p[1]-dimensions[1]/2); //y coordinate
		pos[num][2]=(int) (p[0]-dimensions[0]/2); //z coordinate
		//raw[num][0]=p[0];//x
		//raw[num][1]=p[1];//y
		//raw[num][2]=p[0];//z
		rawXY2[0]=p[0];
		rawXY2[1]=p[1];
	}
	
}
