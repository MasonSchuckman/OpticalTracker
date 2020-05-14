
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.*;
import javax.vecmath.Vector2d;

import java.awt.geom.Line2D;
import java.util.ArrayList; 
  
class MyCanvas extends JComponent { 
	private  int[] x=new int[30];
	private  int[] y=new int[30];
	int points=0;
    public void paint(Graphics g) 
    {     	
    	
        // draw and display the line 
    	  
    	int [] xx= {100,150,200};
    	int [] yy= {100, 150, 200};
	    g.drawPolygon(x, y, points);
    } 
    public void update(ArrayList<Integer> X, ArrayList<Integer> Y) {
    	points=0;
    	for(int i=0; i<X.size(); i++) {
    		x[i]=X.get(i)+200;
    		y[i]=Y.get(i)+200;
    		points++;
    	}
    }
} 
  
public class MotionVisualizer { 
	
	private static PointTracker tracker;
	public static int[][]positionss;
	public static ArrayList<Integer> x;
	public static ArrayList<Integer> y;
	public static MyCanvas can;
    public static void main(String[] a) 
    
    { 
    	x=new ArrayList<Integer>();
    	y=new ArrayList<Integer>();
    	can=new MyCanvas();
    	int points=1;
		MotionCapture2D cap=new MotionCapture2D(points);		
		//MotionCapture3D cap=new MotionCapture3D(2);
		cap.start();			
		tracker=new PointTracker(points);
		
		//JFrame stuff
        JFrame window = new JFrame(); 
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); 
        window.setBounds(30, 30, 600, 600); 
        
        window.getContentPane().add(can); 
        window.setVisible(true); 
        window.addMouseListener(new MouseAdapter() {
		    @Override
		    public void mouseClicked(MouseEvent e) {
		        can.update(x, y);
		        can.repaint();
		    }
		});
        
    } 
    
    public static void update(int [][]positions) {
		tracker.update(positions);
		x.add(positions[0][0]);
		y.add(positions[0][1]);
		positionss=positions.clone();
		if(x.size()>5) {
			x.remove(0);
			y.remove(0);
			can.update(x, y);
	        can.repaint();
	        detectLine();
		}
	}
    public static boolean detectLine() {
    	int minSize=50;
    	int initialX=x.get(0);
    	int initialY=y.get(0);
    	
    	float average=0;
    	Vector2d init=new Vector2d(new int[]{x.get(0)-x.get(1),y.get(0)-y.get(1)});
    	init.normalize();
    	for(int i=2; i<x.size(); i++) {
    		Vector2d current=new Vector2d(new int[]{x.get(0)-x.get(i),y.get(0)-y.get(i)});
    		current.normalize();
    		average+=Math.abs(init.cross(current));
    	}    	
    	
    	float dx=initialX-x.get(x.size()-1);
    	float dy=initialY-y.get(y.size()-1);
    	float dis=(float) Math.hypot(dx, dy);
    	
		//lower average value corresponds to a straighter push.
    	
    	if(average>.05&&average<3)
    		//System.out.print("bad "+average);
			System.out.print("");
    	else if(dis>minSize&&average!=0)
    		System.out.println("good push! "+average+" dis: "+dis);
    	
    	return false;
    }
} 
