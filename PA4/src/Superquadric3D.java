//****************************************************************************
//      SuperQuadric class
//****************************************************************************
//		Created by Linlan Chen (linlan.ch@gmail.com)
//

public class Superquadric3D {
	private Vector3D center;
	private int m, n;
	public Mesh3D mesh;
	private float rx, ry, rz;
	float e1, e2;

	public Superquadric3D(float _e1, float _e2, float _x, float _y, float _z, float _rx, float _ry, float _rz, int _m,
			int _n) {
		e1 = _e1;
		e2 = _e2;
		center = new Vector3D(_x, _y, _z);
		rx = _rx;
		ry = _ry;
		rz = _rz;
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

	public void set_radius(float _rx, float _ry, float _rz) {
		rx = _rx;
		ry = _ry;
		rz = _rz;
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
		fillMesh(); // set the mesh vertices and normals
	}

	// fill the triangle mesh vertices and normals
	// using the current parameters for the sphere
	private void fillMesh() {
		int i, j;
		float theta, phi;
		float d_theta = (float) (2.0 * Math.PI) / ((float) (m - 1));
		float d_phi = (float) Math.PI / ((float) n - 1);
		float c_theta, s_theta;
		float c_phi, s_phi;
		int sign_s_theta, sign_c_theta;
		int sign_s_phi, sign_c_phi;
		for (i = 0, theta = -(float) Math.PI; i < m; ++i, theta += d_theta) {
			c_theta = (float) Math.cos(theta);
			s_theta = (float) Math.sin(theta);
			sign_s_theta = s_theta > 0 ? 1 : -1;
			sign_s_theta = (int) (s_theta == 0 ? 0 : sign_s_theta);
			sign_c_theta = c_theta > 0 ? 1 : -1;
			sign_c_theta = (int) (s_theta == 0 ? 0 : sign_c_theta);

			for (j = 0, phi = (float) (-0.5 * Math.PI); j < n; ++j, phi += d_phi) {
				// vertex location
				c_phi = (float) Math.cos(phi);
				s_phi = (float) Math.sin(phi);
				sign_s_phi = s_phi > 0 ? 1 : -1;
				sign_s_phi = (int) (s_phi == 0 ? 0 : sign_s_phi);
				sign_c_phi = c_phi > 0 ? 1 : -1;
				sign_c_phi = (int) (c_phi == 0 ? 0 : sign_c_phi);
				/*
				 * mesh.v[i][j].x = (float) (center.x + rx *
				 * Math.pow(Math.abs(c_phi), e1) * Math.pow(c_theta, e2));
				 * mesh.v[i][j].y = (float) (center.y + ry *
				 * Math.pow(Math.abs(c_phi), e1) * Math.pow(s_theta, e2));
				 * mesh.v[i][j].z = (float) (center.z + rz * Math.pow(s_phi,
				 * e1));
				 */

				
				mesh.v[i][j].x = (float) (center.x + rx * sign_c_phi * Math.pow(Math.abs(c_phi), e1) * sign_c_theta
						* Math.pow(Math.abs(c_theta), e2));
				mesh.v[i][j].y = (float) (center.y + ry * sign_c_phi * Math.pow(Math.abs(c_phi), e1) * sign_s_theta
						* Math.pow(Math.abs(s_theta), e2));
				mesh.v[i][j].z = (float) (center.z + rz * sign_s_phi * Math.pow(Math.abs(s_phi), e1));

				/*
				mesh.v[i][j].x = (float)((float) center.x + (rx * c_phi * Math.pow(Math.abs(c_phi) , e1 - 1) * c_theta * Math.pow(Math.abs(c_theta), e2 - 1)));
				mesh.v[i][j].y = (float) ((float)center.y+ (ry * c_phi * Math.pow(Math.abs(c_phi), e1 - 1) * s_theta * Math.pow(Math.abs(s_theta), e2 - 1)));
				mesh.v[i][j].z = (float) ((float)center.z+ (rz * s_phi * Math.pow(Math.abs(s_phi), e1 - 1)));
				*/
				// unit normal to superellipsoid at this vertex
			
				/*			
				mesh.n[i][j].x = (float) (ry * rz * sign_s_phi * sign_s_theta * Math.pow(Math.abs(s_phi), e1-1) * Math.pow(Math.abs(c_phi), e1+1) * c_theta * Math.pow(Math.abs(s_theta), e2-1));
				mesh.n[i][j].y = (float) (rz * rx * sign_s_phi * sign_c_theta * Math.pow(Math.abs(s_phi), e1-1) * Math.pow(Math.abs(c_phi), e1+1) * s_theta * Math.pow(Math.abs(c_theta), e2-1));
				mesh.n[i][j].z = (float) (rx * ry * sign_c_theta * s_phi * Math.pow(Math.abs(c_theta), e2-1) * Math.pow(Math.abs(s_theta), e2+1) * Math.pow(Math.abs(c_phi), 2*e1-1) 
										+ rx * ry * sign_s_theta * s_phi * Math.pow(Math.abs(s_theta), e2-1) * Math.pow(Math.abs(c_theta), e2+1) * Math.pow(Math.abs(c_phi), 2*e1-1));
				mesh.n[i][j].normalize();
				
				
				mesh.n[i][j].x = (float) (ry * rz * Math.pow(Math.abs(s_phi), e1-1) * Math.pow(Math.abs(c_phi), e1+1) * c_theta * Math.pow(Math.abs(s_theta), e2-1));
				mesh.n[i][j].y = (float) (rz * rx * Math.pow(Math.abs(s_phi), e1-1) * Math.pow(Math.abs(c_phi), e1+1) * s_theta * Math.pow(Math.abs(c_theta), e2-1));
				mesh.n[i][j].z = (float) (rx * ry * s_phi * Math.pow(Math.abs(c_theta), e2-1) * Math.pow(Math.abs(s_theta), e2+1) * Math.pow(Math.abs(c_phi), 2*e1-1) 
										+ rx * ry * sign_s_theta * s_phi * Math.pow(Math.abs(s_theta), e2-1) * Math.pow(Math.abs(c_theta), e2+1) * Math.pow(Math.abs(c_phi), 2*e1-1));
				mesh.n[i][j].normalize();
				
				mesh.n[i][j].x = c_phi * c_theta;
				mesh.n[i][j].y = c_phi * s_theta;
				mesh.n[i][j].z = s_phi;
				*
				*/

				mesh.n[i][j].x =  (float) (ry * rz * Math.pow(Math.abs(s_phi), e1 - 1)* c_phi * Math.pow(Math.abs(s_theta), e2 - 1) * c_theta );		
				mesh.n[i][j].y =  (float) (rx * rz * Math.pow(Math.abs(s_phi), e1 - 1) * c_phi * Math.pow(Math.abs(c_theta), e2 - 1) * s_theta);
				mesh.n[i][j].z = (float) (rx * ry * Math.pow(Math.abs(c_phi), e1 - 1) * s_phi * (Math.pow(Math.abs(c_theta), e2 + 1) * Math.pow(Math.abs(s_theta), e2 - 1) + Math.pow(Math.abs(s_theta), e2 + 1) * Math.pow(Math.abs(c_theta), e2 - 1) )) ;
				mesh.n[i][j].normalize();
				
				
		/* 		Vector3D v = new Vector3D((float) (1 * Math.pow(c_phi, 2 - e1) * Math.pow(c_theta, 2 - e2) / rx),
					(float) (1 * Math.pow(c_phi, 2 - e1) * Math.pow(s_theta, 2 - e2) / ry),
					(float) (1 * Math.pow(s_phi, 2 - e1)) / rz);
				 v.normalize();
				 mesh.n[i][j].x = v.x;
				 mesh.n[i][j].y = v.y;
				 mesh.n[i][j].z = v.z;
*/
				
			}
		}
	}
}
