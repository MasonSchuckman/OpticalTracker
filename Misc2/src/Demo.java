import java.awt.AWTException;
import java.awt.Robot;

public class Demo {
	private static Robot robot;
	private static PointTracker tracker;
	
	public static void main(String [] args) throws AWTException {
		robot = new Robot();
		int points=1;
		MotionCapture2D cap=new MotionCapture2D(points);		
		//MotionCapture3D cap=new MotionCapture3D(2);
		cap.start();	
		
		tracker=new PointTracker(points);
	}
	
	public static void moveMouse(float x,float y) {
		// robot.mouseMove((int)x*3, (int)(y*2.3));
	}
	public static void update(int [][]positions) {
		tracker.update(positions);
	}
}
