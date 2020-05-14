
public class TrackingPoint implements Comparable<TrackingPoint>{
	public int []position;
	public float []velocity;
	public TrackingPoint(int dimension) { //2d or 3d
		position=new int[dimension];
		velocity=new float[dimension];
	}
	public TrackingPoint(int []pos) {
		this(pos.length);
		position=pos.clone();
	}
	
	public void set(int[]pos) {
		position[0]=pos[0];
		position[1]=pos[1];
	}
	
	@Override
	public int compareTo(TrackingPoint o) { //sorts by x position
		return position[0]-o.position[0];		
	}
	public int getX() {
		return position[0];		
	}
	public int getY() {
		return position[1];		
	}
	public int getZ() {
		return position[2];		
	}
}
