//****************************************************************************
//       Infinite light source class
//****************************************************************************
// History :
//   Nov 6, 2014 Created by Stan Sclaroff
//
//   Dec 2016 Modified by Linlan Chen to support 3 kinds of lights and attenuation
// 1 infinite light
// 2 point light
// 3 ambient light
//

public class Light {
	public Vector3D direction;
	public ColorType color;
	int x, y, z;
	int lightType;
	public boolean radialAtten;
	public boolean angularAtten;
	public double distance = 0;
	public double thetal = 0;
	public Vector3D viewVec = new Vector3D(0,0,0);
	
	public Light(ColorType _c, Vector3D _direction, int lightType) {
		this.lightType = lightType;
		color = new ColorType(_c);
		direction = new Vector3D(_direction);
		direction.normalize();
		x = 0;
		y = 0;
		z = 0;
		radialAtten = false;
		angularAtten =false;
	}
	

	public Light(ColorType _c, int lightType){
		this.lightType = lightType;
		x = 0;
		y = 0;
		z = 0;
		direction = new Vector3D(0, 0, 0);
		color = new ColorType(_c);
		radialAtten = false;
		angularAtten =false;
	}
	
	public Light(ColorType _c, int _x, int _y, int _z, int lightType) {
		this.lightType = lightType;
		color = new ColorType(_c);
		x = _x;
		y = _y;
		z = _z;
		direction = new Vector3D(1, 1, 1);
		direction.normalize();
		radialAtten = false;
		angularAtten =false;
	}
	
	public void setDirction(int _x, int _y, int _z){
		direction.x = _x;
		direction.y = _y;
		direction.z = _z;
		direction.normalize();
	}
	
	public void calL(int _x, int _y, int _z){
		viewVec.x = x - _x;
		viewVec.y = y - _y;
		viewVec.z = z - _z;
		distance = viewVec.magnitude();
		viewVec.normalize();
		thetal = 0 ;
	}

	// apply this light source to the vertex / normal, given material
	// return resulting color value
	public ColorType applyLight(Material mat, Vector3D v, Vector3D n, ColorType c) {
		ColorType res = new ColorType();
		if(c == null){
			res.r = 0;
			res.g = 0;
			res.b = 0;
			}
		else{
			res.r = c.r;
			res.g = c.g;
			res.b = c.b;
		}
		
		if (lightType == 1) {
			// dot product between light direction and normal
			// light must be facing in the positive direction
			// dot <= 0.0 implies this light is facing away (not toward) this
			// point
			// therefore, light only contributes if dot > 0.0

			// ambient component
			if (mat.ambient) {
				res.r += (float) (mat.ka.r * color.r);
				res.g += (float) (mat.ka.g * color.g);
				res.b += (float) (mat.ka.b * color.b);
			}
			
			double dot = direction.dotProduct(n);
			if (dot > 0.0) {
				// diffuse component
				if (mat.diffuse) {
					res.r += (float) (dot * mat.kd.r * color.r);
					res.g += (float) (dot * mat.kd.g * color.g);
					res.b += (float) (dot * mat.kd.b * color.b);
				}
				// specular component
				if (mat.specular) {
					Vector3D r = direction.reflect(n);
					dot = r.dotProduct(v);
					if (dot > 0.0) {
						res.r += (float) Math.pow((dot * mat.ks.r * color.r), mat.ns);
						res.g += (float) Math.pow((dot * mat.ks.g * color.g), mat.ns);
						res.b += (float) Math.pow((dot * mat.ks.b * color.b), mat.ns);
					}
				}
			}
		} else if (lightType == 2) {

			// dot product between light direction and normal
			// light must be facing in the positive direction
			// dot <= 0.0 implies this light is facing away (not toward) this
			// point
			// therefore, light only contributes if dot > 0.0
			
			// ambient component
		
			
			double dot = direction.dotProduct(n);
			if (dot > 0.0) {
				double radialFactor = 1;
				double angularFactor = 1;
				
				if(radialAtten)
					radialFactor = 1 / (1000 + 10 * distance + 10 * Math.pow(distance, 2));
				if(angularAtten){
					double cosalpha = direction.conjugate().dotProduct(viewVec.conjugate());
					if(cosalpha < thetal)
						angularFactor = 0;
					else{
						angularFactor = Math.pow(cosalpha, 2);
					}

				}
				// diffuse component
				if (mat.diffuse) {
					res.r += (float) (dot * mat.kd.r * color.r) * radialFactor * angularFactor;
					res.g += (float) (dot * mat.kd.g * color.g) * radialFactor * angularFactor;
					res.b += (float) (dot * mat.kd.b * color.b) * radialFactor * angularFactor;
				}
				// specular component
				if (mat.specular) {
					Vector3D r = direction.reflect(n);
					dot = r.dotProduct(viewVec);
					if (dot > 0.0) {
						res.r += (float) Math.pow((dot * mat.ks.r * color.r), mat.ns) * radialFactor * angularFactor;
						res.g += (float) Math.pow((dot * mat.ks.g * color.g), mat.ns) * radialFactor * angularFactor;
						res.b += (float) Math.pow((dot * mat.ks.b * color.b), mat.ns) * radialFactor * angularFactor;
					}
				}
			}
		} else if(lightType == 3){
			if (mat.ambient) {
				res.r += (float) (mat.ka.r * color.r);
				res.g += (float) (mat.ka.g * color.g);
				res.b += (float) (mat.ka.b * color.b);
			}
		} else {
		}
		

		// clamp so that allowable maximum illumination level is not
		// exceeded
		res.r = (float) Math.min(1.0, res.r);
		res.g = (float) Math.min(1.0, res.g);
		res.b = (float) Math.min(1.0, res.b);
		return res;
	}

	public void rotateLight(Quaternion q, Vector3D center) {
		Quaternion q_inv = q.conjugate();
		Vector3D vec;

		Quaternion p;

		p = new Quaternion((float) 0.0, direction.minus(center));
		p = q.multiply(p);
		p = p.multiply(q_inv);
		vec = p.get_v();
		direction = vec.plus(center);

	}
}
