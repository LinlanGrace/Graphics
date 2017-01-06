//****************************************************************************
//      Cylinder class
//****************************************************************************
// Created by Linlan Chen(linlan.ch@gmail.com)
//

public class Cylinder3D {
	private Vector3D center;
	private int m, n;
	public Mesh3D mesh;
	private float rx, ry;
	public float umin, umax;
	public Mesh3D endcapmesh;
	
	public Cylinder3D(float _x, float _y, float _z, float _rx, float _ry, float _umin, float _umax, int _m, int _n) {
		center = new Vector3D(_x, _y, _z);
		rx = _rx;
		ry = _ry;
		umin = _umin;
		umax = _umax;
		m = _m;
		n = _n;
		initMesh();
	}

	public void set_center(float _x, float _y, float _z) {
		center.x = _x;
		center.y = _y;
		center.z = _z;
		fillMesh(); // update the triangle mesh
	}

	public void set_radius(float _rx, float _ry, float _umin, float _umax) {
		rx = _rx;
		ry = _ry;
		umin = _umin;
		umax = _umax;
		fillMesh(); // update the triangle mesh
	}

	public void set_m(int _m) {
		m = _m;
		initMesh(); // resized the mesh, must re-initialize
	}

	public void set_n(int _n) {
		n = _n;
		initMesh(); // resized the mesh, must re-initialize
	}

	public int get_n() {
		return n;
	}

	public int get_m() {
		return m;
	}

	private void initMesh() {
		mesh = new Mesh3D(m, n);
		endcapmesh = new Mesh3D(m + 1, 2);
		fillMesh(); // set the mesh vertices and normals
	}

	// fill the triangle mesh vertices and normals
	// using the current parameters for the sphere
	private void fillMesh() {
		int i, j;
		float theta, u;
		float d_theta = (float) (2.0 * Math.PI) / ((float) (m - 1));
		float d_u = (float) Math.abs(umax - umin) / (float) (n - 1);
		float c_theta, s_theta;
		
		endcapmesh.v[0][0].x = center.x;
		endcapmesh.v[0][0].y = center.y;
		endcapmesh.v[0][0].z = center.z + umax;
		endcapmesh.n[0][0].x = 0;
		endcapmesh.n[0][0].y = 0;
		endcapmesh.n[0][0].z = 1;
		endcapmesh.v[0][1].x = center.x;
		endcapmesh.v[0][1].y = center.y;
		endcapmesh.v[0][1].z = center.z + umin;
		endcapmesh.n[0][1].x = 0;
		endcapmesh.n[0][1].y = 0;
		endcapmesh.n[0][1].z = -1;
		
		for (i = 0, theta = -(float) Math.PI; i < m; ++i, theta += d_theta) {
			c_theta = (float) Math.cos(theta);
			s_theta = (float) Math.sin(theta);

			endcapmesh.v[i + 1][0].x = center.x + rx * c_theta;
			endcapmesh.v[i + 1][0].y = center.y + ry * s_theta;
			endcapmesh.v[i + 1][0].z = center.z + umax;
			
			endcapmesh.v[i + 1][1].x = center.x + rx * c_theta;
			endcapmesh.v[i + 1][1].y = center.y + ry * s_theta;
			endcapmesh.v[i + 1][1].z = center.z + umin;
			
			endcapmesh.n[i+1][0].x = 0;
			endcapmesh.n[i+1][0].y = 0;
			endcapmesh.n[i+1][0].z = 1;
				
			endcapmesh.n[i+1][1].x = 0;
			endcapmesh.n[i+1][1].y = 0;
			endcapmesh.n[i+1][1].z = -1;
			
			for (j = 0, u = umin; j < n; ++j, u += d_u) {
				// vertex location
				mesh.v[i][j].x = center.x + rx * c_theta;
				mesh.v[i][j].y = center.y + ry * s_theta;
				mesh.v[i][j].z = center.z + u;
				
				// unit normal to cylinder at this vertex
				Vector3D v = new Vector3D(ry * c_theta, rx * s_theta, 0);
				v.normalize();
				mesh.n[i][j].x = v.x;
				mesh.n[i][j].y = v.y;
				mesh.n[i][j].z = v.z;
				
			}
		}
	
	}
}