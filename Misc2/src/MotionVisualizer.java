
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.*; 
import java.awt.geom.Line2D;
import java.util.ArrayList; 
  
class MyCanvas extends JComponent { 
	private  int[] x=new int[30];
	private  int[] y=new int[30];
	int points=0;
    public void paint(Graphics g) 
    {     	
    	
        // draw and display the line 
    	  
    	int [] xx= {100,150,200,150};
    	int [] yy= {100, 150, 200,200};
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
		if(x.size()>10) {
			x.remove(0);
			y.remove(0);
			can.update(x, y);
	        can.repaint();
		}
	}
    public static boolean detectLine() {
    	
    	return false;
    }
} 
