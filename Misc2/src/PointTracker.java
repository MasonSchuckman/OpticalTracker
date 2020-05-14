import java.util.ArrayList;
import java.util.Collections;

public class PointTracker {
	private int points;
	private boolean initialized=false;
	ArrayList<TrackingPoint>trackers;
	Test test;
	int[][]poses;
	public PointTracker(int pnts) {
		points=pnts;
		trackers=new ArrayList<TrackingPoint>(points);
		poses=new int[pnts][2];
		test=new Test(poses);
	}
	
	public void initializePoints(int[][]positions) {
		for(int pos[]:positions) {
			trackers.add(new TrackingPoint(pos));
		}
	}
	
	public void update(int[][]positions) {
		if(!initialized) {
			System.out.println("Initialized!");
			initializePoints(positions);
			initialized=true;
			return;
		}


		//float []l=new float[2];
		//Collections.sort(trackers);		
		ArrayList<Integer>used=new ArrayList<Integer>();
		for(int i=0; i<points; i++) {
			TrackingPoint current=trackers.get(i);
			float lowest=Integer.MAX_VALUE;
			int best=0;
			for(int j=0; j<points; j++) {				
				float dif=diff(current,positions[j]);
				//System.out.println(dif);
				if(dif<lowest)
				{					
					lowest=dif; 
					best=j;
				}
			}
			if(!used.contains(best)) {
				used.add(best);
				current.set(positions[best]);
				poses[i]=positions[best];
				//l[i]=lowest;
			}			
			//System.out.println(lowest+" tracker:"+i);
		}
		//System.out.println();
		//System.out.println();
		//System.out.println(l[0]+"  "+l[1]);
		//print(positions[0]);
		test.render();

	}

	public float diff(TrackingPoint t,int []pos) {
		if(pos.length==3)
			return (t.getX() - pos[0])*(t.getX() - pos[0])+(t.getY() - pos[1])*(t.getY() - pos[1])+(t.getZ() - pos[1])*(t.getZ() - pos[1]);
		else
			return (t.getX() - pos[0])*(t.getX() - pos[0])+(t.getY() - pos[1])*(t.getY() - pos[1]);
	}
	
	public void print(float[] a) {
		if(a.length==2)
			System.out.println(a[0]+"  "+a[1]);
		else
			System.out.println(a[0]+"  "+a[1]+"  "+a[2]);
	}
	public void print(int[] a) {
		if(a.length==2)
			System.out.println(a[0]+"  "+a[1]);
		else
			System.out.println(a[0]+"  "+a[1]+"  "+a[2]);
	}
	public String toString(int[] a) {
		if(a.length==2)
			return(a[0]+"  "+a[1]);
		else
			return(a[0]+"  "+a[1]+"  "+a[2]);
	}
}
