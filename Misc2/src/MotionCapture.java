
import java.awt.Color;
import java.awt.GridLayout;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JToggleButton;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;

import org.opencv.imgproc.Imgproc;
import org.opencv.video.Video;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

/*this class is responsible for all optical motion tracking.
 *includes features to tune the color filters with UI
 *the heart of this system is to read a frame from each camera, only look at the pixels that
 *fit inside the color filters specified by the UI(also called masks or limiters), then
 *find contours of those pixels, determine which contour is largest, and make a box around that contour
 *finally, send the x,y,z data of that box to the simulation, the x,y,z is gathered from the 2 cameras,
 *1 camera gets the x,y data of the contour, and the second camera gets the z position
 *the 2 cameras must be set at 90 degree angles to each other to achieve 3 dimensional motion tracking
 */
public class MotionCapture extends Thread{

	protected JFrame jframes[];
	protected JLabel vidpanels[];
	protected static boolean debuggingViews=false; 
	protected String path;
	protected int cameras;

	protected int[][] pos;//camera,position x y 
	protected int[][][] raw;//camera,raw x y
	protected int[][][] raw2;//camera,tracker,raw x y
	protected float []rawXY1;
	protected float []rawXY2;

	//set brightness to 160 in camera app!!!!!

	protected int[][][] bounds;       //camera, color, upper RGB, lower RGB	
	protected Scalar matBounds[][][]; //camera, color, upper RGB, lower RGB	

	protected int[] redPresetBounds={145,44,52,80,15,10};    // [finding Red] upper RGB, lower RGB. (changeable by UI)
	protected int[] greenPresetBounds={54,171,149,0,68,45};// [finding Green] upper RGB, lower RGB. (changeable by UI)
	
	protected Mat[][]Filtered;
	protected Mat[][]frames;

	protected boolean contourViewing=false;
	protected ArrayList<List<MatOfPoint>>contours;

	protected VideoCapture camera[];
	protected int trackingPoints;
	public MotionCapture(int cams,int trackingPnts) {
		cameras=cams;
		trackingPoints=trackingPnts;

		camera=new VideoCapture[cams];
		jframes=new JFrame[cams];
		vidpanels=new JLabel[cams];

		bounds=new int[cams][trackingPoints][6];//color filtering bounds. a set of 6 for each tracking point being used.
		//order of the six is: upper RGB, lower RGB. (this is changeable by UI)

		Filtered=new Mat[cams][trackingPoints];
		matBounds=new Scalar[cams][trackingPoints][2]; //lower and upper Scalar bounds

		for(int i=0; i<cams; i++) {
			bounds[i][0]=redPresetBounds;
			if(trackingPoints>1)
				bounds[i][1]=greenPresetBounds;
		}
		frames=new Mat[cams][7];//array of all frames stored for each camera. 
		//Order: frame,firstFrame,gray,frameDelta,thresh,outr,outg

		//System.out.println("Enter the path of opencv_java411.dll");
		//Scanner sc=new Scanner(System.in);
		//path=sc.nextLine();
		//load library
		path="C:\\Users\\suprm\\Documents\\opencv-4.1.1-vc14_vc15\\opencv\\build\\java\\x64\\opencv_java411.dll";

		System.load(path);
	}

	protected void getAngles(Point []p1,Point[]p2){
		if(p1!=null)
			for(int i=0; i<4; i++) {
				//System.out.println(p1[i]);
			}
		//return ;		
	}

	public int[]getDimensions(){
		return new int[] {frames[0][0].cols(),frames[0][0].rows()};
	}

	protected int[]dimensions; 

	protected void setupCameraSettings() {		
		for(VideoCapture Camera:camera) {
			Camera.set(Videoio.CAP_V4L2, 1);			
			Camera.set(Videoio.CAP_PROP_AUTO_EXPOSURE, .25);//.25 enables manual exposure control, .75 re-enables auto control			
			Camera.set(Videoio.CAP_PROP_FPS, 60);	
			//exposure of -3 works best in my room
			Camera.set(Videoio.CAP_PROP_EXPOSURE, -2);			
		}		
	}
	
	protected void setupStartingFrames() {//initializes starting mats for each camera
		for(Mat[] frames:this.frames) {
			for(int i=0; i<7; i++) {
				frames[i]=new Mat();
			}
		}
		for(int i=0; i<cameras; i++) {
			Filtered[i][0]=new Mat();
		}
	}


	protected void displayViews() {	

		for(int i=0; i<cameras; i++) {
			jframes[i]=new JFrame("Camera "+(i+1));
			//displaying the cameras' views
			JFrame jframe=jframes[i];

			jframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

			vidpanels[i]=new JLabel();
			JLabel vidpanel=vidpanels[i];			
			jframe.setContentPane(vidpanel);
			jframe.setVisible(true);
			jframe.setSize(656, 518); //TODO: set these based on camera dimensions.
			if(i!=0)jframe.setLocation(656, 0);
		}					
		if(debuggingViews) {
			views=new ArrayList<JFrame>();
			panels=new ArrayList<JLabel>();
			for(int i=0; i<2; i++)createWindow();
		}
	}

	
	protected ArrayList<List<MatOfPoint>> findContours(Mat frame,int camNum){
		ArrayList<List<MatOfPoint>> contours=new ArrayList<List<MatOfPoint>>(trackingPoints);

		for(int i=0; i<trackingPoints; i++) {	

			Filtered[camNum][i]=new Mat();
			Mat filtered=Filtered[camNum][i];

			Scalar lowerb=matBounds[0][i][0];//TODO: change first dimension to be "camNum", if I  
			Scalar upperb=matBounds[0][i][1];//later want to have cameras individually set color bounds.			

			Mat mask=new Mat();

			Core.inRange(frame, lowerb, upperb, mask);   //create a mask to filter out extraneous colors
			Core.bitwise_and(frame, frame,filtered,mask);//then use the mask

			List<MatOfPoint> cnts=new ArrayList<MatOfPoint>();

			//find the contours in the image.
			Imgproc.threshold(mask,frames[camNum][4],40,255,Imgproc.THRESH_BINARY);
			Imgproc.findContours(frames[camNum][4], cnts, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
			
			//add contours to an ArrayList to return			
			contours.add(cnts);
		}
		return contours;
	}
	
	protected boolean calibrating=false;
	protected int thick=0; //thickness of outlining boxes
	protected int MAX=400; //max area of a contour to be detected. (in pixels)
	protected int MIN=40;
	protected double points[]=new double[4];
	/*
	 * finds given a list of contours for each color and the 2 webcam frames, find and draw a box
	 * around the largest contour found by each webcam onto the frame.
	 */
	protected void addRectangles(Mat []frames,ArrayList<List<MatOfPoint>> contours,int camNum) { // String col

		for(int Color=0; Color<trackingPoints; Color++) {
			List<MatOfPoint> cnts=contours.get(Color);
			String col="RED";
			if(Color==1)col="GREEN";
			Point points[] = null;

			Scalar color=new Scalar(0,0,0);
			int num=0;
			if(col=="RED") {
				color.set(new double[] {0,255,0});
				num=0;
			}
			if(col=="GREEN") {
				color.set(new double[] {100,120,255});
				num=1;
			}

			Mat matrix; 
			if(frames.length==1) //sloppy way of handling whether to add rectangles to each color layer
				matrix=frames[0]; //(contour viewing) or all the base layer(not contour viewing)
			else {
				matrix=frames[Color];				
			}

			ArrayList<Integer>Largests=new ArrayList<Integer>();
			
			int big=-1;
			
			Collections.sort(cnts, new Comparator<MatOfPoint>() { //sorts cnts largest to smallest
				@Override
				public int compare(MatOfPoint a, MatOfPoint b) {					
					return (int) (b.size().area()-a.size().area());
				}
			});
			
			// finds the x largest cnts in the image
			for (int i=0; i<cnts.size(); i++) {				
				MatOfPoint contour=cnts.get(i);
				double area=contour.size().area();
				if (area<MAX&&area>MIN) {					
					Largests.add(i);
					//System.out.println("got here, area: " +largest);
					big=i;
				}
			}			
			//System.out.println(Largests.size());

			if (big!=-1) { //if there is a significantly large enough contour in the image, identify it as an object to track.
				for(int TrackingOb=0; TrackingOb<Largests.size(); TrackingOb++) {
					if(TrackingOb>=raw2[0].length)continue;
					big=Largests.get(TrackingOb);
					Rect r = Imgproc.boundingRect(cnts.get(big));
					MatOfPoint2f dst = new MatOfPoint2f();
					cnts.get(big).convertTo(dst, CvType.CV_32F);
	
					//making the bounding rectangle around the tracked object.
					points = new Point[4];
					int[] p = new int[4];
	
					p[0]=r.x;
					p[1]=r.y;
					p[2]=r.width;
					p[3]=r.height;
	
					Imgproc.rectangle(matrix, // Matrix obj of the image
							new Point(p[0],p[1]), // p1
							new Point(p[0]+p[2],p[1]+p[3]), // p2
							color, // Scalar object for color
							thick // Thickness of the line
							);				
	
					//gets the center of mass of the rectangle
					p=getCOM(p);
					raw[camNum][Color][0]= (p[0]-dimensions[0]/2);
					raw[camNum][Color][1]= (p[1]-dimensions[1]/2);
					
					raw2[camNum][TrackingOb][0]= (p[0]-dimensions[0]/2);
					raw2[camNum][TrackingOb][1]= (p[1]-dimensions[1]/2);
				}
			}					
		}		        	
	}	
	
	
	//System.out.println(new Color(Mat2BufferedImage(frame).getRGB(a, b)));	//gets the RGB value of a pixel at a,b
	protected void setImages(Mat[]frames) {
		if(calibrating)return;
		int camNum=0;
		for(Mat frame:frames) {	
			
			ArrayList<List<MatOfPoint>> c=findContours(frame,camNum);
			
			if(contourViewing||focusingCurrentColor) {
				addRectangles(Filtered[camNum],c,camNum);				
			}
			else {
				addRectangles(new Mat[] {frame},c,camNum);
			}

			if(contourViewing&&!focusingCurrentColor) {
				//this code combines the two different filtered images.
				//(puts the red and blue layers together into 1 image, including the rectangles)
				ArrayList<Mat> layers=new ArrayList<Mat>();
				for(Mat layer:Filtered[camNum]) {
					layers.add(layer);
					if(debuggingViews)
						updateWindows(layers);					
					frame=combineLayers(layers);					
				}				
			}
			if(focusingCurrentColor) {
				frame=Filtered[camNum][currentColor];
			}
			//paints the next frame for the current camera	
			if(!calibrating) {
				JLabel vidpanel=vidpanels[camNum];
				vidpanel.setIcon(new ImageIcon(Mat2BufferedImage(frame)));		       
				vidpanel.repaint();				
				camNum++;
			}
		}
	}
	ArrayList<JFrame>views;
	ArrayList<JLabel>panels;
	protected void createWindow() {
		JFrame jframe=new JFrame("View");
		//displaying the cameras' views
		views.add(jframe);
		jframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		JLabel vidpanel=new JLabel();		
		panels.add(vidpanel);
		jframe.setContentPane(vidpanel);
		jframe.setVisible(true);
		jframe.setSize(656, 518); //TODO: set these based on camera dimensions.
		jframe.setLocation(656, 0);		
	}
	protected void updateWindows(ArrayList<Mat>frames) {
		int index=0;
		for(JLabel panel:panels) {
			panel.setIcon(new ImageIcon(Mat2BufferedImage(frames.get(index))));		       
			panel.repaint();
			index++;
		}
	}
	
	
	
	int [] bestBounds;
	protected void autoCalibrate(Mat f,int camNum) { ///TODO: adjust this to be able to calibrate for mutliple colors.
		int []original=colorToBounds(getMiddleScreenColor(),5);
		Mat frame=f.clone();
		ArrayList<List<MatOfPoint>> c;
		List<MatOfPoint> cnts;
		MatOfPoint2f contour2 = new MatOfPoint2f();
		
		int wide=60;//width of testing off of the base color at middle of the screen
		int inc=15;//increments in which are tested
		int min=50;//min size
		float best=0;
		for(int r=inc; r<wide; r+=inc) {
			bounds[camNum][0][0]=original[0]+r;
			for(int r2=inc; r2<wide; r2+=inc) {
				bounds[camNum][0][3]=original[0]-r2;				
				for(int g=inc; g<wide; g+=inc) {
					bounds[camNum][0][1]=original[1]+g;
					for(int g2=inc; g2<wide; g2+=inc) {
						bounds[camNum][0][4]=original[1]-g2;						
						for(int b=inc; b<wide; b+=inc) {
							bounds[camNum][0][2]=original[2]+b;
							for(int b2=inc; b2<wide; b2+=inc) {
								bounds[camNum][0][5]=original[2]-b2;{	
									//if(!checkValidBounds())continue;
									c=findContours(frame,camNum);
									double largest=0;
									int big=-1;
									cnts=c.get(0);
									// find largest contour area in first image
									for (int i=0; i<cnts.size(); i++) {				
										MatOfPoint contour=cnts.get(i);
										double area=contour.size().area();
										if (area>largest&&area<MAX&&area>min) {
											largest=area;				
											big=i;
										}
									}
									if(big!=-1) {
										//System.out.println("big enough");
										cnts.get(big).convertTo(contour2, CvType.CV_32F);
										Rect rec = Imgproc.boundingRect(cnts.get(big));
										float thresh=1.1f;
										if(rec.width/(float)rec.height>thresh||rec.height/(float)rec.width>thresh)continue;
										//System.out.println("acceptable");
										float roundness=calculateRoundness(contour2);
										
										//int []xy=findMiddleOfContour(cnts.get(big));
										//System.out.println(new Color(Mat2BufferedImage(frames[0][0]).getRGB(xy[0], xy[1]))); //gets color of middle of a contour
										//System.out.print(roundness);
										if(roundness>best) {
											
											best=roundness;
											bestBounds=bounds[0][0];
											System.out.println(best+" GOT HERE " +Math.max(rec.width/(float)rec.height, rec.height/(float)rec.width));
										}			
									}									
								}
							}
						}
					}
				}
			}
		}
		if(bestBounds==null)
			bestBounds=original;
		bounds[0][0]=bestBounds;
		updateBounds();
		refreshSliders();
		System.out.println(best);

		calibrating=false;
	}
	
	protected boolean checkValidBounds() {
		for(int i=0; i<6; i++) {
			if(bounds[0][0][i]>255||bounds[0][0][i]<0)return false;
		}
		return true;
	}
	protected Color getMiddleScreenColor() {
		int x=dimensions[0]/2;
		int y=dimensions[1]/2;
		return new Color(Mat2BufferedImage(frames[0][0]).getRGB(x, y));
	}
	protected int[] colorToBounds(Color col,int range){
		int r=col.getRed();
		int g=col.getGreen();
		int b=col.getBlue();
		int []base= {r,g,b};
		int []bounds=new int[6];	
		
		for(int j=0; j<3; j++) {
			bounds[j]=base[j]+range;
		}
		
		for(int j=3; j<6; j++) {
			bounds[j]=base[j%3]-range;
		}
		
		return bounds;
	}
	
	protected int[] findMiddleOfContour(Mat contour) {
		Rect r = Imgproc.boundingRect(contour);
		
		int[] p = new int[4];

		p[0]=r.x;
		p[1]=r.y;
		p[2]=r.width;
		p[3]=r.height;
		return getCOM(p);
	}
	
	protected float calculateRoundness(MatOfPoint2f circle) {		
		double perimeter = Imgproc.arcLength(circle, true);
		double area=Imgproc.contourArea(circle);
		if(perimeter==0)return 0;
		double roundness=4*3.14*area/Math.pow(perimeter, 2);
		return (float) roundness;
	}
	
	protected Mat combineLayers(ArrayList<Mat>colorLayers) {
		if(colorLayers.size()==1)
			return colorLayers.get(0);
		else {
			Core.addWeighted(colorLayers.get(0), 1, colorLayers.get(1), 1, 0, colorLayers.get(0));
			colorLayers.remove(1);	
			return combineLayers(colorLayers);
		}
	}
	protected boolean focusingCurrentColor=false;
	protected int selected=0;
	protected int[]getCOM(int[]p) {
		int x=p[0]+(p[2]/2);
		int y=p[1]+(p[3]/2);

		return new int[]{x,y};		
	}
	protected String[] texts= {"Upper Red: ","Upper Green: ","Upper Blue: ","Lower Red: ","Lower Green: ","Lower Blue: "};
	protected ArrayList<JLabel>statuses; //shows the limiting variables for color detection
	protected double[]exposure= {-3,-3};
	protected ArrayList<JLabel>exposures;//shows the exposure of each camera

	protected void updateBounds() {
		//updates filter bounds		
		for(int cam=0; cam<cameras; cam++) {
			int color=0;
			
			Scalar [][]boundsForCam=matBounds[cam];
			for(Scalar mb[]:boundsForCam) {
				mb[0]=new Scalar(bounds[cam][color][5], bounds[cam][color][4], bounds[cam][color][3]); 
				mb[1]=new Scalar(bounds[cam][color][2], bounds[cam][color][1], bounds[cam][color][0]); 
				color++;
			}
		}
		//		int [][]boundsForCam=bounds[0];
		//		int color=1;
		//		System.out.println("");
		//		System.out.println("");
		//		for(int mb[]:boundsForCam) {
		//			
		//			for(int i=0; i<6; i++) {
		//				System.out.println(color+"  "+mb[i]);
		//			}
		//			color++;
		//		}
	}
	protected ArrayList<JSlider> sliders; //sliders for color limiting
	protected ArrayList<JSlider> exposureSliders; //TODO: finish implementing slider to control camera exposure
	protected void addUI() {
		statuses=new ArrayList<>();
		// Create and set up a frame window
		JFrame.setDefaultLookAndFeelDecorated(true);
		JFrame frame = new JFrame("Color bounds control panel");
		frame.setSize(500, 500);
		frame.setLayout(new GridLayout(5, 2));
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		sliders=new ArrayList<JSlider>();
		for(int i=0; i<6; i++) {
			//add sliders and values to frame        	
			frame.add(addSlider(i));
			frame.add(statuses.get(i));
		}
		//exposure slider adding to the frame
		//        for(int i=0; i<1; i++) {
		//        	frame.add(exposureSliders.get(i));
		//        	frame.add(exposures.get(i));
		//        }

		//add the drop down box for selecting which filter to edit
		addSelectionBox(frame);
		addFocusButton(frame);
		addCalibrateButton(frame);
		frame.pack();
		frame.setVisible(true);
	}

	protected void addSelectionBox(JFrame f){    //creates the selection box to choose which color filter to edit

		final JLabel label = new JLabel();          
		label.setHorizontalAlignment(JLabel.CENTER);  
		label.setSize(100,50);  
		int s=10;
		JButton b=new JButton("Select");  
		b.setBounds(0,0,s,s);  
		String colorOptions[]={"Red","Green"};        
		@SuppressWarnings({ "rawtypes", "unchecked" })
		JComboBox cb=new JComboBox(colorOptions);    
		cb.setBounds(50, 100,s,s);    
		f.add(cb);
		f.add(label);
		f.add(b); 
		label.setHorizontalAlignment(JLabel.CENTER); 

		b.addActionListener(new ActionListener() {  
			public void actionPerformed(ActionEvent e) {       
				String data = "Filter Selected: "+ cb.getItemAt(cb.getSelectedIndex());  
				label.setText(data);

				selected=cb.getSelectedIndex();	
				currentColor=selected;
				refreshSliders();
			}
		});

		JToggleButton toggleButton = new JToggleButton("Click Me!");     
		toggleButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				boolean status=((JToggleButton)e.getSource()).isSelected();
				if(status)toggleButton.setText("true");else toggleButton.setText("false");	        	 
				contourViewing=status;
			}
		});
		f.add(toggleButton);
	}
	protected void addFocusButton(JFrame f) {
		JToggleButton toggleButton = new JToggleButton("Focus this color");     
		toggleButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				boolean status=((JToggleButton)e.getSource()).isSelected();
				if(status)toggleButton.setText("Focusing");else toggleButton.setText("Focus this color");	        	 
				focusingCurrentColor=status;
			}
		});
		f.add(toggleButton);
	}
	
	protected void addCalibrateButton(JFrame f) {
		JButton button = new JButton("Push to calibrate");     
		button.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				calibrating=!calibrating;
				autoCalibrate(frames[0][0],0);
				if(calibrating)
					button.setText("Calibrating");
				else 
					button.setText("Calibrate");	
			}
		});
		f.add(button);
	}

	//TODO: finish implementing exposure control via slider.
	protected JPanel addExposureSlider(int num) {
		// Set the panel to add buttons
		JPanel panel = new JPanel();

		// Add status label to show the status of the slider
		JLabel status = new JLabel("Exposure of Camera "+num+": "+exposure[num]);//, JLabel.CENTER);

		// Set the slider
		JSlider slider = new JSlider(); 
		slider.setMinorTickSpacing(1);
		slider.setPaintTicks(true);
		// Set the labels to be painted on the slider
		slider.setPaintLabels(true);
		slider.setMaximum(3);
		slider.setMinimum(-8);
		slider.setValue((int) exposure[num]);
		// Add positions label in the slider
		Hashtable<Integer, JLabel> position = new Hashtable<Integer, JLabel>();
		position.put(-8, new JLabel("-8"));
		position.put(-3, new JLabel("-3"));
		position.put(0, new JLabel("0"));
		position.put(3, new JLabel("3"));

		// Set the label to be drawn
		slider.setLabelTable(position);        
		// Add change listener to the slider
		slider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {                
				int val=((JSlider)e.getSource()).getValue();                

				status.setText("Exposure of Camera "+num +": "+ val);
				exposure[num]=val;

				updateBounds();
			}
		});
		statuses.add(status);
		// Add the slider to the panel
		sliders.add(slider);
		panel.add(slider);
		return panel;
	}
	protected int currentColor;
	protected JPanel addSlider(int num) { //returns a slider corresponding to a color limiter
		// Set the panel to add buttons
		JPanel panel = new JPanel();

		// Add status label to show the status of the slider
		JLabel status = new JLabel(texts[num]+bounds[0][currentColor][num]);

		// Set the slider
		JSlider slider = new JSlider(); 
		slider.setMinorTickSpacing(10);
		slider.setPaintTicks(true);
		// Set the labels to be painted on the slider
		slider.setPaintLabels(true);
		slider.setMaximum(255);
		slider.setValue(bounds[0][0][num]);
		// Add positions label in the slider
		Hashtable<Integer, JLabel> position = new Hashtable<Integer, JLabel>();
		position.put(0, new JLabel("0"));
		position.put(100, new JLabel("100"));
		position.put(200, new JLabel("200"));
		position.put(255, new JLabel("255"));

		// Set the label to be drawn
		slider.setLabelTable(position);        
		// Add change listener to the slider
		slider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {                
				int val=((JSlider)e.getSource()).getValue();        

				status.setText(texts[num] + val);
				bounds[0][currentColor][num]=val; //CHANGE        		

				updateBounds();
			}
		});
		statuses.add(status);
		// Add the slider to the panel
		sliders.add(slider);
		panel.add(slider);
		return panel;		
	}

	protected void refreshSliders() {
		for(int i=0; i<sliders.size(); i++) {
			JSlider s=sliders.get(i);
			int val=bounds[0][currentColor][i]; 
			s.setValue(val);
			statuses.get(i).setText(texts[i] + val);
			//System.out.println(currentColor+" ");
		}
	}

	protected BufferedImage image;	
	public BufferedImage Mat2BufferedImage(Mat m) { //converts a matrix object to a bufferedImage to be displayed.
		// Fastest code
		// output can be assigned either to a BufferedImage or to an Image

		int type = BufferedImage.TYPE_BYTE_GRAY;
		if ( m.channels() > 1 ) {
			type = BufferedImage.TYPE_3BYTE_BGR;
		}
		int bufferSize = m.channels()*m.cols()*m.rows();
		byte [] b = new byte[bufferSize];
		m.get(0,0,b); // get all the pixels
		image= new BufferedImage(m.cols(),m.rows(), type);
		final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
		System.arraycopy(b, 0, targetPixels, 0, b.length);  
		return image;
	}

	protected void cal(Mat f,int camNum,int original[]) {
		Mat frame=f.clone();
		ArrayList<List<MatOfPoint>> c;
		List<MatOfPoint> cnts;
		MatOfPoint2f contour2 = new MatOfPoint2f();
		
		int wide=40;
		int inc=10;
		int min=50;
		float best=0;
		for(int r=inc; r<wide; r+=inc) {
			bounds[camNum][0][0]=original[0]+r;
			for(int r2=inc; r2<wide; r2+=inc) {
				bounds[camNum][0][3]=original[0]-r2;				
				for(int g=inc; g<wide; g+=inc) {
					bounds[camNum][0][1]=original[1]+g;
					for(int g2=inc; g2<wide; g2+=inc) {
						bounds[camNum][0][4]=original[1]-g2;						
						for(int b=inc; b<wide; b+=inc) {
							bounds[camNum][0][2]=original[2]+b;
							for(int b2=inc; b2<wide; b2+=inc) {
								bounds[camNum][0][5]=original[2]-b2;{	
									//if(!checkValidBounds())continue;
									c=findContours(frame,camNum);
									double largest=0;
									int big=-1;
									cnts=c.get(0);
									// find largest contour area in first image
									for (int i=0; i<cnts.size(); i++) {				
										MatOfPoint contour=cnts.get(i);
										double area=contour.size().area();
										if (area>largest&&area<MAX&&area>min) {
											largest=area;				
											big=i;
										}
									}
									if(big!=-1) {
										//System.out.println("big enough");
										cnts.get(big).convertTo(contour2, CvType.CV_32F);
										Rect rec = Imgproc.boundingRect(cnts.get(big));
										float thresh=1.15f;
										if(rec.width/(float)rec.height>thresh||rec.height/(float)rec.width>thresh)continue;
										//System.out.println("acceptable");
										float roundness=calculateRoundness(contour2);
										
										//int []xy=findMiddleOfContour(cnts.get(big));
										//System.out.println(new Color(Mat2BufferedImage(frames[0][0]).getRGB(xy[0], xy[1]))); //gets color of middle of a contour
										//System.out.print(roundness);
										if(roundness>best) {
											
											best=roundness;
											bestBounds=bounds[0][0];
											System.out.println(best+" GOT HERE " +Math.max(rec.width/(float)rec.height, rec.height/(float)rec.width));
										}			
									}									
								}
							}
						}
					}
				}
			}
		}
		if(bestBounds==null)
			bestBounds=original;
		bounds[0][0]=bestBounds;
		updateBounds();
		refreshSliders();
		System.out.println(best);

		calibrating=false;
	}
}
