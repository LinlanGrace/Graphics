//****************************************************************************
//       3D Point Class
//****************************************************************************
// History :
//	 2D Point Class
//   Nov 6, 2014 Created by Stan Sclaroff
//	 3D Point Class
//	 Nov 27, 2016 Modifed by Linlan Chen to make it 3D

public class Point3D implements Comparable<Point3D>
{
	public int x, y, z;
	public float u, v; // uv coordinates for texture mapping
	public ColorType c;
	public Vector3D normal;
	public Point3D(int _x, int _y, int _z, ColorType _c)
	{
		u = 0;
		v = 0;
		x = _x;
		y = _y;
		z = _z;
		c = _c;
		normal = new Vector3D(0,0,0);
	}

	public Point3D(int _x, int _y, int _z, Vector3D n){
		normal.x = n.x;
		normal.y = n.y;
		normal.z = n.z;
		u = 0;
		v = 0;
		x = _x;
		y = _y;
		z = _z;
		c = null;
	}
	
	public Point3D(int _x, int _y, int _z, ColorType _c, float _u, float _v)
	{
		u = _u;
		v = _v;
		x = _x;
		y = _y;
		z = _z;
		c = _c;
		normal = new Vector3D(0,0,0);
	}
	public Point3D()
	{
		c = new ColorType(1.0f, 1.0f, 1.0f);
		normal = new Vector3D(0,0,0);
	}
	public Point3D( Point3D p)
	{
		u = p.u;
		v = p.v;
		x = p.x;
		y = p.y;
		z = p.z;
		c = new ColorType(p.c.r, p.c.g, p.c.b);
		normal = new Vector3D(p.normal.x, p.normal.y, p.normal.z);
	}
	
	public int compareTo(Point3D o) {
		
		if(this.x > o.x){
			return 1;
		}
		else if(this.x < o.x){
			return -1;

		}
		
		return 0;
	}
}