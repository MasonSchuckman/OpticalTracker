package javax.vecmath;


public class Vector3d {

	private float x;
	private float y;
	private float z;

	public Vector3d()
	{
		this.setX(0);
		this.setY(0);
		this.setZ(0);
	}

	public Vector3d(float [] vals) {
		this.setX(vals[0]);
		this.setY(vals[1]);
		this.setZ(vals[2]);
	}	
	
	public Vector3d(float x, float y, float z)
	{
		this.setX(x);
		this.setY(y);
		this.setZ(z);
	}

	public void setX(float x) {
		this.x = x;
	}

	public float getX() {
		return x;
	}

	public void setY(float y) {
		this.y = y;
	}

	public float getY() {
		return y;
	}
	
	public void setZ(float z) {
		this.z = z;
	}

	public float getZ() {
		return z;
	}

	public void set(float x, float y,float z)
	{
		this.setX(x);
		this.setY(y);
		this.setZ(z);		
	}


	public float dot(Vector3d v2)
	{
		float result = 0.0f;
		result = this.getX() * v2.getX() + this.getY() * v2.getY() +this.getZ()*v2.getZ();
		return result;
	}

	public float getLength()
	{
		return (float)Math.sqrt(getX()*getX() + getY()*getY()+ getZ()*getZ());
	}
//
//	public float getDistance(Vector3d v2)
//	{
//		return (float) Math.sqrt((v2.getX() - getX()) * (v2.getX() - getX()) + (v2.getY() - getY()) * (v2.getY() - getY()));
//	}


	public Vector3d add(Vector3d v2)
	{
		Vector3d result = new Vector3d();
		result.setX(getX() + v2.getX());
		result.setY(getY() + v2.getY());
		result.setZ(getZ() + v2.getZ());
		return result;
	}
	public Vector3d getUnitVector() {
		float m=this.getLength();
		return new Vector3d(this.x/m,this.y/m,this.z/m);
	}
	public Vector3d subtract(Vector3d v2)
	{
		Vector3d result = new Vector3d();
		result.setX(this.getX() - v2.getX());
		result.setY(this.getY() - v2.getY());
		result.setZ(this.getZ() - v2.getZ());
		return result;
	}

	public Vector3d multiply(float scaleFactor)
	{
		Vector3d result = new Vector3d();
		result.setX(this.getX() * scaleFactor);
		result.setY(this.getY() * scaleFactor);
		result.setZ(this.getZ() * scaleFactor);
		return result;
	}
	public float[] asArray() {
		return new float[]{x,y,z};
	}
	public Vector3d normalize()
	{
		float len = getLength();
		if (len != 0.0f)
		{
			this.setX(this.getX() / len);
			this.setY(this.getY() / len);
			this.setZ(this.getZ() / len);
		}
		else
		{
			this.setX(0.0f);
			this.setY(0.0f);
			this.setZ(0.0f);
		}

		return this;
	}

	public String toString()
	{
		return "X: " + getX() + " Y: " + getY() + " Z: " + getZ();
	}


}
