
//****************************************************************************
//      Main Program for CS680 PA4
//****************************************************************************
// Description: 
//   

//     LEFTMOUSE: draw line segments 
//     RIGHTMOUSE: draw triangles 
//
//     The following keys control the program:
//
// Q,q: quit
// C,c: clear polygon (set vertex count=0)
// R,r: randomly change the color
// F,f: show flat shading
// G,g: show gouraud shading
// P,p: show phong shading
// T,t: show testing examples (toggles between smooth shading and flat
// shading test cases)
// >: increase the step number for examples
// <: decrease the step number for examples
// +,-: increase or decrease spectral exponent
// D,d: toggle diffuse light
// A,a: toggle ambient light
// S,s: toggle specular light
// O,o: toggle object move
// K,k: toggle camera move
// Z,z: scale the objects
// I,i: toggle light switching
// 9 :  add ka
// 8 :  add kd
// 7 :  reduce ks
// L,l: toggle light switching (the lights are initialized to be turned on)
// 1  : turn off infiniteLight
// 2  : turn off pointLight
// 3  : turn off ambientLight
// 4  : turn on radial attenuation
// 5  : turn on angular attenuation
//****************************************************************************
// History :
//   Aug 2004 Created by Jianming Zhang based on the C
//   code by Stan Sclaroff
//  Nov 2014 modified to include test cases for shading example for PA4
//
//   Dec 2016 Modified by Linlan Chen (linlan.ch@gmail.com)
//	

import javax.swing.*;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.event.*;
import java.awt.image.*;
//import java.io.File;
//import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;

//import javax.imageio.ImageIO;
import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.awt.GLCanvas;//for new version of gl
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;

import com.jogamp.graph.geom.Triangle;
import com.jogamp.opengl.util.FPSAnimator;//for new version of gl

public class PA4 extends JFrame implements GLEventListener, KeyListener, MouseListener, MouseMotionListener {

	private static final long serialVersionUID = 1L;
	private final int DEFAULT_WINDOW_WIDTH = 512;
	private final int DEFAULT_WINDOW_HEIGHT = 512;
	private final float DEFAULT_LINE_WIDTH = 1.0f;

	private GLCapabilities capabilities;
	private GLCanvas canvas;
	private FPSAnimator animator;

	final private int numTestCase;
	private int testCase;
	private BufferedImage buff;
	private int[][] depBuffer;
	@SuppressWarnings("unused")
	private ColorType color;
	private Random rng;

	// specular exponent for materials
	private int ns = 5;

	private ArrayList<Point3D> lineSegs;
	private ArrayList<Point3D> triangles;

	// 1 for flat shading, 2 for gouraud, 3 for phong
	private int shadingType;
	private int Nsteps;
	private int xmove, ymove;
	private boolean moveobj, movecamera;
	private float scale;
	private boolean[] matsterms = new boolean[3];
	private boolean switchLight = true;
	private boolean infiniteLight = true;
	private boolean pointLight = true;
	private boolean ambientLight = true;
	private boolean radialAtten = false;
	private boolean angularAtten = false;
	/** The quaternion which controls the rotation of the world. */
	private Quaternion viewing_quaternion = new Quaternion();
	private Vector3D viewing_center = new Vector3D((float) (DEFAULT_WINDOW_WIDTH / 2),
			(float) (DEFAULT_WINDOW_HEIGHT / 2), (float) 0.0);
	/** The last x and y coordinates of the mouse press. */
	private int last_x = 0, last_y = 0;
	/** Whether the world is being rotated. */
	private boolean rotate_world = false;
	private float deltaAmbient = 0;
	private float deltaDiffuse = 0;
	private float deltaSpecular = 0;
	
	public PA4() {
		capabilities = new GLCapabilities(null);
		capabilities.setDoubleBuffered(true); // Enable Double buffering

		canvas = new GLCanvas(capabilities);
		canvas.addGLEventListener(this);
		canvas.addMouseListener(this);
		canvas.addMouseMotionListener(this);
		canvas.addKeyListener(this);
		canvas.setAutoSwapBufferMode(true); // true by default. Just to be
											// explicit
		canvas.setFocusable(true);
		getContentPane().add(canvas);

		animator = new FPSAnimator(canvas, 60); // drive the display loop @ 60
												// FPS

		numTestCase = 3;
		testCase = 4;
		Nsteps = 12;

		xmove = 0;
		ymove = 0;
		moveobj = false;
		movecamera = false;
		scale = 1;

		matsterms[0] = true;
		matsterms[1] = true;
		matsterms[2] = true;

		setTitle("CS480/680 PA4");
		setSize(DEFAULT_WINDOW_WIDTH, DEFAULT_WINDOW_HEIGHT);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setVisible(true);
		setResizable(false);

		rng = new Random();
		color = new ColorType(1.0f, 0.0f, 0.0f);
		lineSegs = new ArrayList<Point3D>();
		triangles = new ArrayList<Point3D>();
		shadingType = 2;
	}

	public void run() {
		animator.start();
	}

	public static void main(String[] args) {
		PA4 P = new PA4();
		P.run();
	}

	// ***********************************************
	// GLEventListener Interfaces
	// ***********************************************
	public void init(GLAutoDrawable drawable) {
		GL gl = drawable.getGL();
		gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
		gl.glLineWidth(DEFAULT_LINE_WIDTH);
		Dimension sz = this.getContentPane().getSize();
		buff = new BufferedImage(sz.width, sz.height, BufferedImage.TYPE_3BYTE_BGR);
		depBuffer = new int[512][512];
		for (int index1 = 0; index1 < 512; index1++) {
			for (int index2 = 0; index2 < 512; index2++) {
				depBuffer[index1][index2] = -10000;
			}
		}
		clearPixelBuffer();
		clearDepthBuffer();
	}

	// Redisplaying graphics
	public void display(GLAutoDrawable drawable) {
		GL2 gl = drawable.getGL().getGL2();
		WritableRaster wr = buff.getRaster();
		DataBufferByte dbb = (DataBufferByte) wr.getDataBuffer();
		byte[] data = dbb.getData();

		gl.glPixelStorei(GL2.GL_UNPACK_ALIGNMENT, 1);
		gl.glDrawPixels(buff.getWidth(), buff.getHeight(), GL2.GL_BGR, GL2.GL_UNSIGNED_BYTE, ByteBuffer.wrap(data));
		drawTestCase();
	}

	// Window size change
	public void reshape(GLAutoDrawable drawable, int x, int y, int w, int h) {
		// deliberately left blank
	}

	public void displayChanged(GLAutoDrawable drawable, boolean modeChanged, boolean deviceChanged) {
		// deliberately left blank
	}

	void clearPixelBuffer() {
		lineSegs.clear();
		triangles.clear();
		Graphics2D g = buff.createGraphics();
		g.setColor(Color.BLACK);
		g.fillRect(0, 0, buff.getWidth(), buff.getHeight());
		g.dispose();
	}

	void clearDepthBuffer() {
		for (int index1 = 0; index1 < 512; index1++) {
			for (int index2 = 0; index2 < 512; index2++) {
				depBuffer[index1][index2] = -10000;
			}
		}
	}

	/**
	 * 
	 * @param dir
	 *            dir == 1 right dir == -1 left dir == 2 up dir == -2 down
	 */
	void move(int dir) {
		if (movecamera) {
			if (dir == 1)
				xmove -= 5;
			else if (dir == -1)
				xmove += 5;
			else if (dir == 2)
				ymove += 5;
			else
				ymove -= 5;
			System.out.println(xmove + " " + ymove);

		} else if (moveobj) {
			if (dir == 1)
				xmove += 5;
			else if (dir == -1)
				xmove -= 5;
			else if (dir == 2)
				ymove -= 5;
			else
				ymove += 5;
		} else {
		}

	}

	void reset() {
		moveobj = false;
		movecamera = false;
		scale = 1;
		xmove = 0;
		ymove = 0;
		viewing_quaternion.reset();
	}

	// drawTest
	void drawTestCase() {
		/* clear the window and vertex state */
		clearPixelBuffer();
		clearDepthBuffer();
		switch (testCase) {
		case 3:
			shadeTest(1, 1); 
			break;
		case 4:
			shadeTest(1, 2); 
			break;
		case 5:
			shadeTest(1, 3); 
			break;
		case 0:
			shadeTest(1, 2);
			break;
		case 1:
			shadeTest2(1, 2);
			break;
		case 2:
			shadeTest3(1, 2);
			break;
		}
	}

	// ***********************************************
	// KeyListener Interfaces
	// ***********************************************
	public void keyTyped(KeyEvent key) {
		// Q,q: quit
		// C,c: clear polygon (set vertex count=0)
		// R,r: randomly change the color
		// F,f: show flat shading
		// G,g: show gouraud shading
		// P,p: show phong shading
		// T,t: show testing examples (toggles between smooth shading and flat
		// shading test cases)
		// >: increase the step number for examples
		// <: decrease the step number for examples
		// +,-: increase or decrease spectral exponent
		// D,d: toggle diffuse light
		// A,a: toggle ambient light
		// S,s: toggle specular light
		// O,o: toggle object move
		// K,k: toggle camera move
		// Z,z: scale the objects
		// I,i: toggle light switching
		// 9  : add ka
		// 8  : add kd
		// 7  : reduce ks
		// L,l: toggle light switching (the lights are initialized to be turned on)
		// 1  : turn off infiniteLight
		// 2  : turn off pointLight
		// 3  : turn off ambientLight
		// 4  : turn on radial attenuation
		// 5  : turn on angular attenuation
		switch (key.getKeyChar()) {
		case 'Q':
		case 'q':
			new Thread() {
				public void run() {
					animator.stop();
				}
			}.start();
			System.exit(0);
			break;
		case 'L':
		case 'l':
			switchLight = !switchLight;
			break;
		case '1':
			if(switchLight)
				infiniteLight = !infiniteLight;
			System.out.println(infiniteLight + " infinite ");
			break;
		case '2':
			if(switchLight)
				pointLight = !pointLight;
			System.out.println(pointLight + " point ");
			break;
		case '3':
			if(switchLight)
				ambientLight = !ambientLight;
			System.out.println(ambientLight + " ambient ");
			break;
		case '4':
			if(switchLight)
				radialAtten = !radialAtten;
			System.out.println(radialAtten + " radialAtten ");
			break;
		case '5':
			if(switchLight)
				angularAtten = !angularAtten;
			System.out.println(angularAtten + " angularAtten ");
			break;
		case '9':
			deltaAmbient = (deltaAmbient == 0) ? 0.1f : 0;
			break;
		case '8':
			deltaDiffuse = (deltaDiffuse == 0) ? 0.2f : 0;
			break;
		case '7':
			deltaSpecular = (deltaSpecular == 0) ? -0.1f : 0;
			break;
		case 'R':
		case 'r':
			color = new ColorType(rng.nextFloat(), rng.nextFloat(), rng.nextFloat());
			break;
		case 'C':
		case 'c':
			clearPixelBuffer();
			clearDepthBuffer();
			break;
		case 'F':
		case 'f':
			shadingType = 1;
			testCase = 3;
			drawTestCase();
			break;
		case 'G':
		case 'g':
			shadingType = 2;
			testCase = 4;
			drawTestCase();
			break;
		case 'T':
		case 't':
			testCase = testCase >= numTestCase ? 0 :(testCase + 1) % numTestCase;
			drawTestCase();
			break;
		case 'D':
		case 'd':
			matsterms[1] = !matsterms[1];
			break;
		case 'A':
		case 'a':
			matsterms[0] = !matsterms[0];
			break;
		case 'S':
		case 's':
			matsterms[2] = !matsterms[2];
			break;
		case '<':
			Nsteps = Nsteps < 4 ? Nsteps : Nsteps / 2;
			System.out.printf("Nsteps = %d \n", Nsteps);
			drawTestCase();
			break;
		case '>':
			Nsteps = Nsteps > 190 ? Nsteps : Nsteps * 2;
			System.out.printf("Nsteps = %d \n", Nsteps);
			drawTestCase();
			break;
		case '+':
			ns++;
			drawTestCase();
			break;
		case '-':
			if (ns > 0)
				ns--;
			drawTestCase();
			break;
		case 'K':
		case 'k':
			movecamera = true;
			moveobj = false;
			break;
		case 'O':
		case 'o':
			System.out.println("moveobj");
			moveobj = true;
			movecamera = false;
			break;
		case 'Z':
			scale = (float) (scale < 1.2 ? scale + 0.1 : scale);
			System.out.println(scale);
			break;
		case 'z':
			scale = (float) (scale > 0.6 ? scale - 0.1 : scale);
			break;
		case 'P':
		case 'p':
			shadingType = 3;
			testCase = 5;
			drawTestCase();
			break;
		case '0':
			reset();
			break;
		default:
			break;
		}
	}

	public void keyPressed(KeyEvent key) {
		switch (key.getKeyCode()) {
		case KeyEvent.VK_ESCAPE:
			new Thread() {
				public void run() {
					animator.stop();
				}
			}.start();
			System.exit(0);
			break;
		case KeyEvent.VK_UP:
			System.out.println("up");
			move(2);
			break;
		case KeyEvent.VK_DOWN:
			System.out.println("down");
			move(-2);
			break;
		case KeyEvent.VK_LEFT:
			System.out.println("left");
			move(-1);
			break;
		case KeyEvent.VK_RIGHT:
			System.out.println("right");
			move(1);
			break;
		default:
			break;
		}
	}

	public void keyReleased(KeyEvent key) {
		// deliberately left blank
	}

	// **************************************************
	// MouseListener and MouseMotionListener Interfaces
	// **************************************************
	public void mouseClicked(MouseEvent mouse) {
		// deliberately left blank
	}

	public void mousePressed(MouseEvent mouse) {
		int button = mouse.getButton();
		if (button == MouseEvent.BUTTON1) {
			last_x = mouse.getX();
			last_y = mouse.getY();
			rotate_world = true;
		}
	}

	public void mouseReleased(MouseEvent mouse) {
		int button = mouse.getButton();
		if (button == MouseEvent.BUTTON1) {
			rotate_world = false;
		}
	}

	public void mouseMoved(MouseEvent mouse) {
		// Deliberately left blank
	}

	/**
	 * Updates the rotation quaternion as the mouse is dragged.
	 * 
	 * @param mouse
	 *            The mouse drag event object.
	 */
	public void mouseDragged(final MouseEvent mouse) {
		if (this.rotate_world) {
			// get the current position of the mouse
			final int x = mouse.getX();
			final int y = mouse.getY();

			// get the change in position from the previous one
			final int dx = x - this.last_x;
			final int dy = y - this.last_y;

			// create a unit vector in the direction of the vector (dy, dx, 0)
			final float magnitude = (float) Math.sqrt(dx * dx + dy * dy);
			if (magnitude > 0.0001) {
				// define axis perpendicular to (dx,-dy,0)
				// use -y because origin is in upper lefthand corner of the
				// window
				final float[] axis = new float[] { -(float) (dy / magnitude), (float) (dx / magnitude), 0 };

				// calculate appropriate quaternion
				final float viewing_delta = 3.1415927f / 180.0f;
				final float s = (float) Math.sin(0.5f * viewing_delta);
				final float c = (float) Math.cos(0.5f * viewing_delta);
				final Quaternion Q = new Quaternion(c, s * axis[0], s * axis[1], s * axis[2]);
				this.viewing_quaternion = Q.multiply(this.viewing_quaternion);

				// normalize to counteract acccumulating round-off error
				this.viewing_quaternion.normalize();

				// save x, y as last x, y
				this.last_x = x;
				this.last_y = y;
				drawTestCase();
			}
		}

	}

	public void mouseEntered(MouseEvent mouse) {
		// Deliberately left blank
	}

	public void mouseExited(MouseEvent mouse) {
		// Deliberately left blank
	}

	public void dispose(GLAutoDrawable drawable) {
		// TODO Auto-generated method stub

	}

	void shadeTest(int scene, int shading) {
		// the simple example scene includes one sphere and one torus
		float radius = (float) 50.0 * scale;
		Sphere3D sphere = new Sphere3D((float) 256.0 + xmove, (float) 256.0 + ymove, (float) 128.0,
				(float) 1.2 * radius, Nsteps, Nsteps);
		Torus3D torus = new Torus3D((float) 128.0 + xmove, (float) 384.0 + ymove, (float) 128.0, (float) 0.6 * radius,
				(float) 1.1 * radius, Nsteps, Nsteps);
		Ellipsoid3D el = new Ellipsoid3D((float) 384.0 + xmove, (float) 128.0 + ymove, (float) 128.0,
				(float) 1.2 * radius, (float) 0.6 * radius, (float) 0.6 * radius, Nsteps, Nsteps);
		Cylinder3D cy = new Cylinder3D(386.0f + xmove, 384f + ymove, 128f, (float) (0.5 * radius),
				(float) (0.5 * radius), -50, 50, Nsteps, Nsteps);
		Superquadric3D box = new Superquadric3D(0.3f, 0.3f, 128f, 128f, 128f, radius, radius, radius, Nsteps, Nsteps);
		// view vector is defined along z axis
		// this example assumes simple othorgraphic projection
		// view vector is used in
		// (a) calculating specular lighting contribution
		// (b) backface culling / backface rejection
		Vector3D view_vector = new Vector3D((float) 0.0, (float) 0.0, (float) 1.0);

		// material properties for the sphere and torus
		// ambient, diffuse, and specular coefficients
		// specular exponent is a global variable
		ColorType torus_ka = new ColorType(0.1f + deltaAmbient, 0.1f + deltaAmbient, 0.1f);
		ColorType torus_kd = new ColorType(0.0f + deltaDiffuse, 0.5f + deltaDiffuse, 0.9f);
		ColorType torus_ks = new ColorType(1.0f + deltaSpecular, 1.0f + deltaSpecular, 1.0f);
		ColorType sphere_ka = new ColorType(0.1f + deltaAmbient, 0.1f + deltaAmbient, 0.1f);
		ColorType sphere_kd = new ColorType(0.9f + deltaDiffuse, 0.3f + deltaDiffuse, 0.1f);
		ColorType sphere_ks = new ColorType(1.0f + deltaSpecular, 1.0f + deltaSpecular, 1.0f);
		ColorType el_ka = new ColorType(0.3f + deltaAmbient, 0.3f + deltaAmbient, 0.3f);
		ColorType el_kd = new ColorType(0.8f + deltaDiffuse, 0.9f + deltaDiffuse, 0.9f);
		ColorType el_ks = new ColorType(1.0f + deltaSpecular, 1.0f + deltaSpecular, 1.0f);
		ColorType cy_ka = new ColorType(0.1f + deltaAmbient, 0.1f + deltaAmbient, 0.1f);
		ColorType cy_kd = new ColorType(0.5f + deltaDiffuse, 0.5f + deltaDiffuse, 0.9f);
		ColorType cy_ks = new ColorType(1f + deltaSpecular, 1f + deltaSpecular, 1f);
		ColorType box_ka = new ColorType(0.1f + deltaAmbient, 0.2f + deltaAmbient, 0.1f);
		ColorType box_kd = new ColorType(0.0f + deltaDiffuse, 0.5f + deltaDiffuse, 0.9f);
		ColorType box_ks = new ColorType(1.0f + deltaSpecular, 1.0f + deltaSpecular, 1.0f);
		Material[] mats = { new Material(sphere_ka, sphere_kd, sphere_ks, matsterms, ns),
				new Material(torus_ka, torus_kd, torus_ks, matsterms, ns),
				new Material(el_ka, el_kd, el_ks, matsterms, ns), new Material(cy_ka, cy_kd, cy_ks, matsterms, ns),
				new Material(box_ka, box_kd, box_ks, matsterms, ns) };

		// define one infinite light source, with color = white
		ColorType light_color = new ColorType(1.0f, 1.0f, 1.0f);
		ColorType light_color1 = new ColorType(0.0f, 0.0f, 1.0f);

		Vector3D light_direction = new Vector3D((float) 0.0, (float) (-1.0 / Math.sqrt(2.0)),
				(float) (1.0 / Math.sqrt(2.0)));
		Light light = new Light(light_color, light_direction, 1);
		Light plight = new Light(light_color1, 500, 500, 200, 2);
		Light alight = new Light(light_color, 3);
		// normal to the plane of a triangle
		// to be used in backface culling / backface rejection
		Vector3D triangle_normal = new Vector3D();

		// a triangle mesh
		Mesh3D mesh;
		Mesh3D endcapmesh = new Mesh3D(0, 0);
		boolean hasEndcap = false;
		int i, j, n, m;

		// temporary variables for triangle 3D vertices and 3D normals
		Vector3D v0, v1, v2, n0, n1, n2;

		// projected triangle, with vertex colors
		Point3D[] tri = { new Point3D(), new Point3D(), new Point3D() };

		for (int k = 0; k < 5; ++k) // loop three times: shade sphere, torus,
									// then ellipsoid
		{
			if (k == 0) {
				mesh = sphere.mesh;
				n = sphere.get_n();
				m = sphere.get_m();
			} else if (k == 1) {
				mesh = torus.mesh;
				n = torus.get_n();
				m = torus.get_m();
			} else if (k == 2) {
				mesh = el.mesh;
				n = el.get_n();
				m = el.get_m();
			} else if (k == 3) {
				mesh = cy.mesh;
				endcapmesh = cy.endcapmesh;
				hasEndcap = true;
				n = cy.get_n();
				m = cy.get_m();
				// System.out.println(cy.umin + " " + cy.umax);
			} else {
				mesh = box.mesh;
				n = box.get_n();
				m = box.get_m();
			}

			// rotate the surface's 3D mesh using quaternion
			if (movecamera) {
				mesh.rotateMesh(viewing_quaternion.conjugate(), viewing_center);
				endcapmesh.rotateMesh(viewing_quaternion.conjugate(), viewing_center);
				//light.rotateLight(viewing_quaternion, viewing_center);
			} else {
				mesh.rotateMesh(viewing_quaternion, viewing_center);
				endcapmesh.rotateMesh(viewing_quaternion, viewing_center);
			}
			// draw triangles for the current surface, using vertex colors
			// this works for Gouraud and flat shading only (not Phong)

			for (i = 0; i < m - 1; ++i) {
				for (j = 0; j < n - 1; ++j) {
					v0 = mesh.v[i][j];
					v1 = mesh.v[i][j + 1];
					v2 = mesh.v[i + 1][j + 1];
					triangle_normal = computeTriangleNormal(v0, v1, v2);

					if (view_vector.dotProduct(triangle_normal) > 0.0) // front-facing
																		// triangle?
					{
						if (shading == 2) {
							// vertex colors for Gouraud shading
							n0 = mesh.n[i][j];
							n1 = mesh.n[i][j + 1];
							n2 = mesh.n[i + 1][j + 1];
							tri[0].c = null;
							tri[1].c = null;
							tri[2].c = null;
							if(infiniteLight){
								tri[0].c = light.applyLight(mats[k], view_vector, n0, tri[0].c);
								tri[1].c = light.applyLight(mats[k], view_vector, n1, tri[1].c);
								tri[2].c = light.applyLight(mats[k], view_vector, n2, tri[2].c);
							}
							if(pointLight){
								if(radialAtten){
									plight.radialAtten = true;
								}
								if(angularAtten){
									plight.angularAtten = true;
								}
								plight.calL(tri[0].x, tri[0].y, tri[0].z);
								tri[0].c = plight.applyLight(mats[k], view_vector, n0, tri[0].c);
								plight.calL(tri[1].x, tri[1].y, tri[1].z);
								tri[1].c = plight.applyLight(mats[k], view_vector, n1, tri[1].c);
								plight.calL(tri[2].x, tri[2].y, tri[2].z);
								tri[2].c = plight.applyLight(mats[k], view_vector, n2, tri[2].c);
							}
							if(ambientLight){
								tri[0].c = alight.applyLight(mats[k], view_vector, n0, tri[0].c);
								tri[1].c = alight.applyLight(mats[k], view_vector, n1, tri[1].c);
								tri[2].c = alight.applyLight(mats[k], view_vector, n2, tri[2].c);
							}
						} else if (shading == 1) {
							// flat shading: use the normal to the triangle
							// itself
							n2 = n1 = n0 = triangle_normal;
							tri[2].c = null;
							if(infiniteLight)
								tri[2].c = tri[1].c = tri[0].c = light.applyLight(mats[k], view_vector, triangle_normal, tri[2].c);
							if(pointLight){
								if(radialAtten){
									plight.radialAtten = true;
								}
								if(angularAtten){
									plight.angularAtten = true;
								}
								plight.calL(tri[2].x, tri[2].y, tri[2].z);
								tri[2].c = tri[1].c = tri[0].c = plight.applyLight(mats[k], view_vector, triangle_normal, tri[2].c);
							}
							if(ambientLight)
								tri[2].c = tri[1].c = tri[0].c = alight.applyLight(mats[k], view_vector, triangle_normal, tri[2].c);
						}else {
							// vertex normal for Phong shading

							tri[0].normal = mesh.n[i][j];
							tri[1].normal = mesh.n[i][j + 1];
							tri[2].normal = mesh.n[i + 1][j + 1];
							n0 = mesh.n[i][j];
							n2 = mesh.n[i][j + 1];
							n1 = mesh.n[i + 1][j + 1];
							tri[0].c = null;
							tri[1].c = null;
							tri[2].c = null;
							if(infiniteLight){
								tri[0].c = light.applyLight(mats[k], view_vector, n0, tri[0].c);
								tri[1].c = light.applyLight(mats[k], view_vector, n1, tri[1].c);
								tri[2].c = light.applyLight(mats[k], view_vector, n2, tri[2].c);
							}
							if(pointLight){
								if(radialAtten){
									plight.radialAtten = true;
								}
								if(angularAtten){
									plight.angularAtten = true;
								}
								plight.calL(tri[0].x, tri[0].y, tri[0].z);
								tri[0].c = plight.applyLight(mats[k], view_vector, n0, tri[0].c);
								plight.calL(tri[1].x, tri[1].y, tri[1].z);
								tri[1].c = plight.applyLight(mats[k], view_vector, n1, tri[1].c);
								plight.calL(tri[2].x, tri[2].y, tri[2].z);
								tri[2].c = plight.applyLight(mats[k], view_vector, n2, tri[2].c);
							}
							if(ambientLight){
								tri[0].c = alight.applyLight(mats[k], view_vector, n0, tri[0].c);
								tri[1].c = alight.applyLight(mats[k], view_vector, n1, tri[1].c);
								tri[2].c = alight.applyLight(mats[k], view_vector, n2, tri[2].c);
							}}
						n0 = mesh.n[i][j];
						n1 = mesh.n[i][j + 1];
						n2 = mesh.n[i + 1][j + 1];
						tri[0].x = (int) v0.x;
						tri[0].y = (int) v0.y;
						tri[0].z = (int) v0.z;
						tri[1].x = (int) v1.x;
						tri[1].y = (int) v1.y;
						tri[1].z = (int) v1.z;
						tri[2].x = (int) v2.x;
						tri[2].y = (int) v2.y;
						tri[2].z = (int) v2.z;

						if(shading != 3)
							SketchBase.drawTriangle(buff, tri[0], tri[1], tri[2], shading, depBuffer, mats[k], view_vector, light);
						else{
						
							SketchBase.drawTrianglePhong(buff, tri[0], tri[1], tri[2], n0, n1, n2, depBuffer, mats[k], view_vector, light);
						//	SketchBase.drawTrianglePhong1(buff, tri[0], tri[1], tri[2], depBuffer, mats[k], view_vector, light);
						}
					}

					v0 = mesh.v[i][j];
					v1 = mesh.v[i + 1][j + 1];
					v2 = mesh.v[i + 1][j];
					triangle_normal = computeTriangleNormal(v0, v1, v2);

					if (view_vector.dotProduct(triangle_normal) > 0.0) // front-facing
																		// triangle?
					{
						if (shading == 2) {
							// vertex colors for Gouraud shading
							n0 = mesh.n[i][j];
							n1 = mesh.n[i + 1][j + 1];
							n2 = mesh.n[i + 1][j];
							tri[0].c = null;
							tri[1].c = null;
							tri[2].c = null;
							if(infiniteLight){
								tri[0].c = light.applyLight(mats[k], view_vector, n0, tri[0].c);
								tri[1].c = light.applyLight(mats[k], view_vector, n1, tri[1].c);
								tri[2].c = light.applyLight(mats[k], view_vector, n2, tri[2].c);
							}
							if(pointLight){
								if(radialAtten){
									plight.radialAtten = true;
								}
								if(angularAtten){
									plight.angularAtten = true;
								}
								plight.calL(tri[0].x, tri[0].y, tri[0].z);
								tri[0].c = plight.applyLight(mats[k], view_vector, n0, tri[0].c);
								plight.calL(tri[1].x, tri[1].y, tri[1].z);
								tri[1].c = plight.applyLight(mats[k], view_vector, n1, tri[1].c);
								plight.calL(tri[2].x, tri[2].y, tri[2].z);
								tri[2].c = plight.applyLight(mats[k], view_vector, n2, tri[2].c);
							}
							if(ambientLight){
								tri[0].c = alight.applyLight(mats[k], view_vector, n0, tri[0].c);
								tri[1].c = alight.applyLight(mats[k], view_vector, n1, tri[1].c);
								tri[2].c = alight.applyLight(mats[k], view_vector, n2, tri[2].c);
							}
						} else if (shading == 1) {
							// flat shading: use the normal to the triangle
							// itself
							n2 = n1 = n0 = triangle_normal;
							tri[2].c = null;
							if(infiniteLight)
								tri[2].c = tri[1].c = tri[0].c = light.applyLight(mats[k], view_vector, triangle_normal, tri[2].c);
							if(pointLight){
								if(radialAtten){
									plight.radialAtten = true;
								}
								if(angularAtten){
									plight.angularAtten = true;
								}
								plight.calL(tri[2].x, tri[2].y, tri[2].z);
								tri[2].c = tri[1].c = tri[0].c = plight.applyLight(mats[k], view_vector, triangle_normal, tri[2].c);
							}
							if(ambientLight)
								tri[2].c = tri[1].c = tri[0].c = alight.applyLight(mats[k], view_vector, triangle_normal, tri[2].c);
						} else {
							// vertex normal for Phong shading
							tri[0].normal = mesh.n[i][j];
							tri[1].normal = mesh.n[i + 1][j + 1];
							tri[2].normal = mesh.n[i + 1][j];
							n0 = mesh.n[i][j];
							n1 = mesh.n[i + 1][j + 1];
							n2 = mesh.n[i + 1][j];
							tri[0].c = null;
							tri[1].c = null;
							tri[2].c = null;
							if(infiniteLight){
								tri[0].c = light.applyLight(mats[k], view_vector, n0, tri[0].c);
								tri[1].c = light.applyLight(mats[k], view_vector, n1, tri[1].c);
								tri[2].c = light.applyLight(mats[k], view_vector, n2, tri[2].c);
							}
							if(pointLight){
								if(radialAtten){
									plight.radialAtten = true;
								}
								if(angularAtten){
									plight.angularAtten = true;
								}
								plight.calL(tri[0].x, tri[0].y, tri[0].z);
								tri[0].c = plight.applyLight(mats[k], view_vector, n0, tri[0].c);
								plight.calL(tri[1].x, tri[1].y, tri[1].z);
								tri[1].c = plight.applyLight(mats[k], view_vector, n1, tri[1].c);
								plight.calL(tri[2].x, tri[2].y, tri[2].z);
								tri[2].c = plight.applyLight(mats[k], view_vector, n2, tri[2].c);
							}
							if(ambientLight){
								tri[0].c = alight.applyLight(mats[k], view_vector, n0, tri[0].c);
								tri[1].c = alight.applyLight(mats[k], view_vector, n1, tri[1].c);
								tri[2].c = alight.applyLight(mats[k], view_vector, n2, tri[2].c);
							}
						}
						n0 = mesh.n[i][j];
						n1 = mesh.n[i + 1][j + 1];
						n2 = mesh.n[i + 1][j];
						tri[0].x = (int) v0.x;
						tri[0].y = (int) v0.y;
						tri[0].z = (int) v0.z;
						tri[1].x = (int) v1.x;
						tri[1].y = (int) v1.y;
						tri[1].z = (int) v1.z;
						tri[2].x = (int) v2.x;
						tri[2].y = (int) v2.y;
						tri[2].z = (int) v2.z;
						
						if(shading != 3)
							SketchBase.drawTriangle(buff, tri[0], tri[1], tri[2], 2, depBuffer, mats[k], view_vector, light);
						else
						{
							
						SketchBase.drawTrianglePhong(buff, tri[0], tri[1], tri[2], n0, n1, n2, depBuffer, mats[k], view_vector, light);
					//	SketchBase.drawTrianglePhong1(buff, tri[0], tri[1], tri[2], depBuffer, mats[k], view_vector, light);
						}
					}
				}
			}

			if (hasEndcap && k == 3) {
				for (int index1 = 0; index1 < 2; index1++) {
					for (int index = 1; index < m; index++) {
						v0 = endcapmesh.v[0][index1];
						if (index == m - 1) {
							v1 = endcapmesh.v[1][index1];
						} else {
							v1 = endcapmesh.v[index + 1][index1];
						}
						v2 = endcapmesh.v[index][index1];

						if (index1 == 0)
							triangle_normal = computeTriangleNormal(v0, v1, v2);
						else
							triangle_normal = computeTriangleNormal(v0, v2, v1);

						if (view_vector.dotProduct(triangle_normal) > 0.0) // front-facing
																			// triangle?
						{
							if (shading == 2 || shading == 3) {
								// vertex colors for Gouraud shading
								n0 = endcapmesh.n[0][index1];
								n1 = endcapmesh.n[index][index1];
								n2 = endcapmesh.n[index + 1][index1];
								tri[0].c = null;
								tri[1].c = null;
								tri[2].c = null;
								if(infiniteLight){
									tri[0].c = light.applyLight(mats[k], view_vector, n0, null);
									tri[1].c = light.applyLight(mats[k], view_vector, n1, null);
									tri[2].c = light.applyLight(mats[k], view_vector, n2, null);
								}
								if(pointLight){
									if(radialAtten){
										plight.radialAtten = true;
									}
									if(angularAtten){
										plight.angularAtten = true;
									}
									plight.calL(tri[0].x, tri[0].y, tri[0].z);
									tri[0].c = plight.applyLight(mats[k], view_vector, n0, tri[0].c);
									plight.calL(tri[1].x, tri[1].y, tri[1].z);
									tri[1].c = plight.applyLight(mats[k], view_vector, n1, tri[1].c);
									plight.calL(tri[2].x, tri[2].y, tri[2].z);
									tri[2].c = plight.applyLight(mats[k], view_vector, n2, tri[2].c);
								}
								if(ambientLight){
									tri[0].c = alight.applyLight(mats[k], view_vector, n0, tri[0].c);
									tri[1].c = alight.applyLight(mats[k], view_vector, n1, tri[1].c);
									tri[2].c = alight.applyLight(mats[k], view_vector, n2, tri[2].c);
								}
							} else {
								// flat shading: use the normal to the
								// triangle
								// itself
								n2 = n1 = n0 = triangle_normal;
								tri[2].c = null;
								if(infiniteLight)
									tri[2].c = tri[1].c = tri[0].c = light.applyLight(mats[k], view_vector, triangle_normal, tri[2].c);
								if(pointLight){
									if(radialAtten){
										plight.radialAtten = true;
									}
									plight.calL(tri[2].x, tri[2].y, tri[2].z);
									tri[2].c = tri[1].c = tri[0].c = plight.applyLight(mats[k], view_vector, triangle_normal, tri[2].c);
								}
								if(ambientLight)
									tri[2].c = tri[1].c = tri[0].c = alight.applyLight(mats[k], view_vector, triangle_normal, tri[2].c);
							
							}

							tri[0].x = (int) v0.x;
							tri[0].y = (int) v0.y;
							tri[0].z = (int) v0.z;
							tri[1].x = (int) v1.x;
							tri[1].y = (int) v1.y;
							tri[1].z = (int) v1.z;
							tri[2].x = (int) v2.x;
							tri[2].y = (int) v2.y;
							tri[2].z = (int) v2.z;

							if(pointLight || infiniteLight || ambientLight)
								SketchBase.drawTriangle(buff, tri[0], tri[1], tri[2], 2, depBuffer, mats[k], view_vector, light);
						}
					}
				}

			}
		}
	}

	void shadeTest2(int scene, int shading) {
		// the simple example scene includes one sphere and one torus
		float radius = (float) 40.0 * scale;
		Sphere3D sphere = new Sphere3D((float) 256.0 + xmove, (float) 256.0 + ymove, (float) 128.0,
				(float) 1.2 * radius, Nsteps, Nsteps);
		Torus3D torus = new Torus3D((float) 200.0 + xmove, (float) 384.0 + ymove, (float) 128.0, (float) 0.6 * radius,
				(float) 1.1 * radius, Nsteps, Nsteps);
		Ellipsoid3D el = new Ellipsoid3D((float) 384.0 + xmove, (float) 128.0 + ymove, (float) 128.0,
				(float) 1.2 * radius, (float) 0.6 * radius, (float) 0.6 * radius, Nsteps, Nsteps);
		Cylinder3D cy = new Cylinder3D(386.0f + xmove, 384f + ymove, 128f, (float) (0.5 * radius),
				(float) (0.5 * radius), -50, 50, Nsteps, Nsteps);
		Superquadric3D box = new Superquadric3D(2.5f, 0.5f, 128f, 128f, 128f, radius, radius, radius, Nsteps, Nsteps);
		Vector3D view_vector = new Vector3D((float) 0.0, (float) 0.0, (float) 1.0);
		
		ColorType torus_ka = new ColorType(0.4f + deltaAmbient, 0.6f + deltaAmbient, 0.1f + deltaAmbient);
		ColorType torus_kd = new ColorType(0.4f + deltaDiffuse, 0.5f + deltaDiffuse, 0.9f + deltaDiffuse);
		ColorType torus_ks = new ColorType(1.0f + deltaSpecular, 1.0f + deltaSpecular, 1.0f + deltaSpecular);
		ColorType sphere_ka = new ColorType(0.1f + deltaAmbient, 0.3f + deltaAmbient, 0.1f + deltaAmbient);
		ColorType sphere_kd = new ColorType(0.9f + deltaDiffuse, 0.3f + deltaDiffuse, 0.4f + deltaDiffuse);
		ColorType sphere_ks = new ColorType(1.0f + deltaSpecular, 1.0f + deltaSpecular, 1.0f + deltaSpecular);
		ColorType el_ka = new ColorType(0.3f + deltaAmbient, 0.3f + deltaAmbient, 0.3f + deltaAmbient);
		ColorType el_kd = new ColorType(0.8f + deltaDiffuse, 0.8f + deltaDiffuse, 0.8f + deltaDiffuse);
		ColorType el_ks = new ColorType(1.0f + deltaSpecular, 1.0f + deltaSpecular, 1.0f + deltaSpecular);
		ColorType box_ka = new ColorType(0.1f + deltaAmbient, 0.2f + deltaAmbient, 0.1f + deltaAmbient);
		ColorType box_kd = new ColorType(0.0f + deltaDiffuse, 0.5f + deltaDiffuse, 0.9f + deltaDiffuse);
		ColorType box_ks = new ColorType(1.0f + deltaSpecular, 1.0f + deltaSpecular, 1.0f + deltaSpecular);
		Material[] mats = { new Material(sphere_ka, sphere_kd, sphere_ks, matsterms, ns),
				new Material(torus_ka, torus_kd, torus_ks, matsterms, ns),
				new Material(el_ka, el_kd, el_ks, matsterms, ns), 
				new Material(box_ka, box_kd, box_ks, matsterms, ns) };

		ColorType light_color = new ColorType(1.0f, 1.0f, 1.0f);
		ColorType light_color1 = new ColorType(0.0f, 1.0f, 0.0f);
		Vector3D light_direction = new Vector3D((float) 0.0, (float) (-1.0 / Math.sqrt(2.0)),
				(float) (1.0 / Math.sqrt(2.0)));
		Light light = new Light(light_color, light_direction, 1);
		Light plight = new Light(light_color1, 100, 100, 100, 2);
		Light alight = new Light(light_color, 3);
		plight.setDirction(0,-1,-1);
		Vector3D triangle_normal = new Vector3D();

		// a triangle mesh
		Mesh3D mesh;
		int i, j, n, m;

		// temporary variables for triangle 3D vertices and 3D normals
		Vector3D v0, v1, v2, n0, n1, n2;

		// projected triangle, with vertex colors
		Point3D[] tri = { new Point3D(), new Point3D(), new Point3D() };

		for (int k = 0; k < 4; ++k) 
		{
			if (k == 0) {
				mesh = sphere.mesh;
				n = sphere.get_n();
				m = sphere.get_m();
			} else if (k == 1) {
				mesh = torus.mesh;
				n = torus.get_n();
				m = torus.get_m();
			} else if (k == 2) {
				mesh = el.mesh;
				n = el.get_n();
				m = el.get_m();
			} else {
				mesh = box.mesh;
				n = box.get_n();
				m = box.get_m();
			}

			// rotate the surface's 3D mesh using quaternion
			if (movecamera) {
				mesh.rotateMesh(viewing_quaternion.conjugate(), viewing_center);
				
			} else {
				mesh.rotateMesh(viewing_quaternion, viewing_center);
				
			}

			for (i = 0; i < m - 1; ++i) {
				for (j = 0; j < n - 1; ++j) {
					v0 = mesh.v[i][j];
					v1 = mesh.v[i][j + 1];
					v2 = mesh.v[i + 1][j + 1];
					triangle_normal = computeTriangleNormal(v0, v1, v2);

					if (view_vector.dotProduct(triangle_normal) > 0.0) // front-facing
																		// triangle?
					{
						if (shading == 2) {
							// vertex colors for Gouraud shading
							n0 = mesh.n[i][j];
							n1 = mesh.n[i][j + 1];
							n2 = mesh.n[i + 1][j + 1];
							tri[0].c = null;
							tri[1].c = null;
							tri[2].c = null;
							if(infiniteLight){
								tri[0].c = light.applyLight(mats[k], view_vector, n0, tri[0].c);
								tri[1].c = light.applyLight(mats[k], view_vector, n1, tri[1].c);
								tri[2].c = light.applyLight(mats[k], view_vector, n2, tri[2].c);
							}
							if(pointLight){
								if(radialAtten){
									plight.radialAtten = true;
								}
								if(angularAtten){
									plight.angularAtten = true;
								}
								plight.calL(tri[0].x, tri[0].y, tri[0].z);
								tri[0].c = plight.applyLight(mats[k], view_vector, n0, tri[0].c);
								plight.calL(tri[1].x, tri[1].y, tri[1].z);
								tri[1].c = plight.applyLight(mats[k], view_vector, n1, tri[1].c);
								plight.calL(tri[2].x, tri[2].y, tri[2].z);
								tri[2].c = plight.applyLight(mats[k], view_vector, n2, tri[2].c);
							}
							if(ambientLight){
								tri[0].c = alight.applyLight(mats[k], view_vector, n0, tri[0].c);
								tri[1].c = alight.applyLight(mats[k], view_vector, n1, tri[1].c);
								tri[2].c = alight.applyLight(mats[k], view_vector, n2, tri[2].c);
							}
						} else if (shading == 1) {
							// flat shading: use the normal to the triangle
							// itself
							n2 = n1 = n0 = triangle_normal;
							tri[2].c = null;
							if(infiniteLight)
								tri[2].c = tri[1].c = tri[0].c = light.applyLight(mats[k], view_vector, triangle_normal, tri[2].c);
							if(pointLight){
								if(radialAtten){
									plight.radialAtten = true;
								}
								if(angularAtten){
									plight.angularAtten = true;
								}
								plight.calL(tri[2].x, tri[2].y, tri[2].z);
								tri[2].c = tri[1].c = tri[0].c = plight.applyLight(mats[k], view_vector, triangle_normal, tri[2].c);
							}
							if(ambientLight)
								tri[2].c = tri[1].c = tri[0].c = alight.applyLight(mats[k], view_vector, triangle_normal, tri[2].c);
						}else {
							// vertex normal for Phong shading
							tri[0].normal = mesh.n[i][j];
							tri[1].normal = mesh.n[i][j + 1];
							tri[2].normal = mesh.n[i + 1][j + 1];
							tri[0].c = light.applyLight(mats[k], view_vector, tri[0].normal, null);
							tri[1].c = light.applyLight(mats[k], view_vector, tri[1].normal, null);
							tri[2].c = light.applyLight(mats[k], view_vector, tri[2].normal, null);

						}

						tri[0].x = (int) v0.x;
						tri[0].y = (int) v0.y;
						tri[0].z = (int) v0.z;
						tri[1].x = (int) v1.x;
						tri[1].y = (int) v1.y;
						tri[1].z = (int) v1.z;
						tri[2].x = (int) v2.x;
						tri[2].y = (int) v2.y;
						tri[2].z = (int) v2.z;

						if(pointLight || infiniteLight || ambientLight)
							SketchBase.drawTriangle(buff, tri[0], tri[1], tri[2], shading, depBuffer, mats[k], view_vector, light);
					}

					v0 = mesh.v[i][j];
					v1 = mesh.v[i + 1][j + 1];
					v2 = mesh.v[i + 1][j];
					triangle_normal = computeTriangleNormal(v0, v1, v2);

					if (view_vector.dotProduct(triangle_normal) > 0.0) // front-facing
																		// triangle?
					{
						if (shading == 2) {
							// vertex colors for Gouraud shading
							n0 = mesh.n[i][j];
							n1 = mesh.n[i + 1][j + 1];
							n2 = mesh.n[i + 1][j];
							tri[0].c = null;
							tri[1].c = null;
							tri[2].c = null;
							if(infiniteLight){
								tri[0].c = light.applyLight(mats[k], view_vector, n0, tri[0].c);
								tri[1].c = light.applyLight(mats[k], view_vector, n1, tri[1].c);
								tri[2].c = light.applyLight(mats[k], view_vector, n2, tri[2].c);
							}
							if(pointLight){
								if(radialAtten){
									plight.radialAtten = true;
								}
								if(angularAtten){
									plight.angularAtten = true;
								}
								plight.calL(tri[0].x, tri[0].y, tri[0].z);
								tri[0].c = plight.applyLight(mats[k], view_vector, n0, tri[0].c);
								plight.calL(tri[1].x, tri[1].y, tri[1].z);
								tri[1].c = plight.applyLight(mats[k], view_vector, n1, tri[1].c);
								plight.calL(tri[2].x, tri[2].y, tri[2].z);
								tri[2].c = plight.applyLight(mats[k], view_vector, n2, tri[2].c);
							}
							if(ambientLight){
								tri[0].c = alight.applyLight(mats[k], view_vector, n0, tri[0].c);
								tri[1].c = alight.applyLight(mats[k], view_vector, n1, tri[1].c);
								tri[2].c = alight.applyLight(mats[k], view_vector, n2, tri[2].c);
							}
						} else if (shading == 1) {
							// flat shading: use the normal to the triangle
							// itself
							n2 = n1 = n0 = triangle_normal;
							tri[2].c = null;
							if(infiniteLight)
								tri[2].c = tri[1].c = tri[0].c = light.applyLight(mats[k], view_vector, triangle_normal, tri[2].c);
							if(pointLight){
								if(radialAtten){
									plight.radialAtten = true;
								}
								if(angularAtten){
									plight.angularAtten = true;
								}
								plight.calL(tri[2].x, tri[2].y, tri[2].z);
								tri[2].c = tri[1].c = tri[0].c = plight.applyLight(mats[k], view_vector, triangle_normal, tri[2].c);
							}
							if(ambientLight)
								tri[2].c = tri[1].c = tri[0].c = alight.applyLight(mats[k], view_vector, triangle_normal, tri[2].c);
						} else {
							// vertex normal for Phong shading
							tri[0].normal = mesh.n[i][j];
							tri[1].normal = mesh.n[i + 1][j + 1];
							tri[2].normal = mesh.n[i + 1][j];
							tri[0].c = light.applyLight(mats[k], view_vector, tri[0].normal, null);
							tri[1].c = light.applyLight(mats[k], view_vector, tri[1].normal, null);
							tri[2].c = light.applyLight(mats[k], view_vector, tri[2].normal, null);
						}

						tri[0].x = (int) v0.x;
						tri[0].y = (int) v0.y;
						tri[0].z = (int) v0.z;
						tri[1].x = (int) v1.x;
						tri[1].y = (int) v1.y;
						tri[1].z = (int) v1.z;
						tri[2].x = (int) v2.x;
						tri[2].y = (int) v2.y;
						tri[2].z = (int) v2.z;
						
						if(pointLight || infiniteLight || ambientLight)
							SketchBase.drawTriangle(buff, tri[0], tri[1], tri[2], shading, depBuffer, mats[k], view_vector, light);
					}
				}
			}
		}
	}
	
	void shadeTest3(int scene, int shading) {
		// the simple example scene includes one sphere and one torus
		float radius = (float) 40.0 * scale;
		Sphere3D sphere = new Sphere3D((float) 256.0 + xmove, (float) 256.0 + ymove, (float) 128.0,
				(float) 1.2 * radius, Nsteps, Nsteps);
		Torus3D torus = new Torus3D((float) 128.0 + xmove, (float) 384.0 + ymove, (float) 128.0, (float) 0.6 * radius,
				(float) 1.1 * radius, Nsteps, Nsteps);
		Ellipsoid3D el = new Ellipsoid3D((float) 384.0 + xmove, (float) 128.0 + ymove, (float) 128.0,
				(float) 1.2 * radius, (float) 0.6 * radius, (float) 0.6 * radius, Nsteps, Nsteps);
		Cylinder3D cy = new Cylinder3D(386.0f + xmove, 384f + ymove, 128f, (float) (0.5 * radius),
				(float) (0.5 * radius), -50, 50, Nsteps, Nsteps);
		Superquadric3D box = new Superquadric3D(0.3f, 0.3f, 128f, 128f, 128f, radius, radius, radius, Nsteps, Nsteps);
		Vector3D view_vector = new Vector3D((float) 0.0, (float) 0.0, (float) 1.0);

		ColorType torus_ka = new ColorType(0.1f + deltaAmbient, 0.1f + deltaAmbient, 0.1f + deltaAmbient);
		ColorType torus_kd = new ColorType(0.0f + deltaDiffuse, 0.5f + deltaDiffuse, 0.9f + deltaDiffuse);
		ColorType torus_ks = new ColorType(1.0f + deltaSpecular, 1.0f + deltaSpecular, 1.0f + deltaSpecular);
		ColorType sphere_ka = new ColorType(0.1f + deltaAmbient, 0.1f + deltaAmbient, 0.1f + deltaAmbient);
		ColorType sphere_kd = new ColorType(0.9f + deltaDiffuse, 0.3f + deltaDiffuse, 0.1f + deltaDiffuse);
		ColorType sphere_ks = new ColorType(1.0f + deltaSpecular, 1.0f + deltaSpecular, 1.0f + deltaSpecular);
		ColorType el_ka = new ColorType(0.3f + deltaAmbient, 0.3f + deltaAmbient, 0.3f + deltaAmbient);
		ColorType el_kd = new ColorType(0.8f + deltaDiffuse, 0.9f + deltaDiffuse, 0.9f + deltaDiffuse);
		ColorType el_ks = new ColorType(1.0f + deltaSpecular, 1.0f + deltaSpecular, 1.0f + deltaSpecular);
		ColorType cy_ka = new ColorType(0.1f + deltaAmbient, 0.1f + deltaAmbient, 0.1f + deltaAmbient);
		ColorType cy_kd = new ColorType(0.5f + deltaDiffuse, 0.5f + deltaDiffuse, 0.9f + deltaDiffuse);
		ColorType cy_ks = new ColorType(1f + deltaSpecular, 1f + deltaSpecular, 1f + deltaSpecular);
		ColorType box_ka = new ColorType(0.1f + deltaAmbient, 0.2f + deltaAmbient, 0.1f + deltaAmbient);
		ColorType box_kd = new ColorType(0.0f + deltaDiffuse, 0.5f + deltaDiffuse, 0.9f + deltaDiffuse);
		ColorType box_ks = new ColorType(1.0f + deltaSpecular, 1.0f + deltaSpecular, 1.0f + deltaSpecular);
		Material[] mats = { new Material(sphere_ka, sphere_kd, sphere_ks, matsterms, ns),
				new Material(torus_ka, torus_kd, torus_ks, matsterms, ns),
				new Material(el_ka, el_kd, el_ks, matsterms, ns), new Material(cy_ka, cy_kd, cy_ks, matsterms, ns),
				new Material(box_ka, box_kd, box_ks, matsterms, ns) };

		ColorType light_color = new ColorType(1.0f, 1.0f, 1.0f);
		ColorType light_color1 = new ColorType(1.0f, 0.0f, 0.0f);
		Vector3D light_direction = new Vector3D((float) 0.0, (float) (-1.0 / Math.sqrt(2.0)),
				(float) (1.0 / Math.sqrt(2.0)));
		Light light = new Light(light_color, light_direction, 1);
		Light plight = new Light(light_color1, 500, 500, 200, 2);
		Light alight = new Light(light_color, 3);
		Vector3D triangle_normal = new Vector3D();

		// a triangle mesh
		Mesh3D mesh;
		Mesh3D endcapmesh = new Mesh3D(0, 0);
		boolean hasEndcap = false;
		int i, j, n, m;

		// temporary variables for triangle 3D vertices and 3D normals
		Vector3D v0, v1, v2, n0, n1, n2;

		// projected triangle, with vertex colors
		Point3D[] tri = { new Point3D(), new Point3D(), new Point3D() };

		for (int k = 0; k < 5; ++k) 
		{
			if (k == 0) {
				mesh = sphere.mesh;
				n = sphere.get_n();
				m = sphere.get_m();
			} else if (k == 1) {
				mesh = torus.mesh;
				n = torus.get_n();
				m = torus.get_m();
			} else if (k == 2) {
				mesh = el.mesh;
				n = el.get_n();
				m = el.get_m();
			} else if (k == 3) {
				mesh = cy.mesh;
				endcapmesh = cy.endcapmesh;
				hasEndcap = true;
				n = cy.get_n();
				m = cy.get_m();
			} else {
				mesh = box.mesh;
				n = box.get_n();
				m = box.get_m();
			}

			// rotate the surface's 3D mesh using quaternion
			if (movecamera) {
				mesh.rotateMesh(viewing_quaternion.conjugate(), viewing_center);
				endcapmesh.rotateMesh(viewing_quaternion.conjugate(), viewing_center);
				//light.rotateLight(viewing_quaternion, viewing_center);
			} else {
				mesh.rotateMesh(viewing_quaternion, viewing_center);
				endcapmesh.rotateMesh(viewing_quaternion, viewing_center);
			}
			// draw triangles for the current surface, using vertex colors
			// this works for Gouraud and flat shading only (not Phong)

			for (i = 0; i < m - 1; ++i) {
				for (j = 0; j < n - 1; ++j) {
					v0 = mesh.v[i][j];
					v1 = mesh.v[i][j + 1];
					v2 = mesh.v[i + 1][j + 1];
					triangle_normal = computeTriangleNormal(v0, v1, v2);

					if (view_vector.dotProduct(triangle_normal) > 0.0) // front-facing
																		// triangle?
					{
						if (shading == 2) {
							// vertex colors for Gouraud shading
							n0 = mesh.n[i][j];
							n1 = mesh.n[i][j + 1];
							n2 = mesh.n[i + 1][j + 1];
							tri[0].c = null;
							tri[1].c = null;
							tri[2].c = null;
							if(infiniteLight){
								tri[0].c = light.applyLight(mats[k], view_vector, n0, tri[0].c);
								tri[1].c = light.applyLight(mats[k], view_vector, n1, tri[1].c);
								tri[2].c = light.applyLight(mats[k], view_vector, n2, tri[2].c);
							}
							if(pointLight){
								if(radialAtten){
									plight.radialAtten = true;
								}
								if(angularAtten){
									plight.angularAtten = true;
								}
								plight.calL(tri[0].x, tri[0].y, tri[0].z);
								tri[0].c = plight.applyLight(mats[k], view_vector, n0, tri[0].c);
								plight.calL(tri[1].x, tri[1].y, tri[1].z);
								tri[1].c = plight.applyLight(mats[k], view_vector, n1, tri[1].c);
								plight.calL(tri[2].x, tri[2].y, tri[2].z);
								tri[2].c = plight.applyLight(mats[k], view_vector, n2, tri[2].c);
							}
							if(ambientLight){
								tri[0].c = alight.applyLight(mats[k], view_vector, n0, tri[0].c);
								tri[1].c = alight.applyLight(mats[k], view_vector, n1, tri[1].c);
								tri[2].c = alight.applyLight(mats[k], view_vector, n2, tri[2].c);
							}
						} else if (shading == 1) {
							// flat shading: use the normal to the triangle
							// itself
							n2 = n1 = n0 = triangle_normal;
							tri[2].c = null;
							if(infiniteLight)
								tri[2].c = tri[1].c = tri[0].c = light.applyLight(mats[k], view_vector, triangle_normal, tri[2].c);
							if(pointLight){
								if(radialAtten){
									plight.radialAtten = true;
								}
								if(angularAtten){
									plight.angularAtten = true;
								}
								plight.calL(tri[2].x, tri[2].y, tri[2].z);
								tri[2].c = tri[1].c = tri[0].c = plight.applyLight(mats[k], view_vector, triangle_normal, tri[2].c);
							}
							if(ambientLight)
								tri[2].c = tri[1].c = tri[0].c = alight.applyLight(mats[k], view_vector, triangle_normal, tri[2].c);
						}else {
							// vertex normal for Phong shading
							tri[0].normal = mesh.n[i][j];
							tri[1].normal = mesh.n[i][j + 1];
							tri[2].normal = mesh.n[i + 1][j + 1];
							tri[0].c = light.applyLight(mats[k], view_vector, tri[0].normal, null);
							tri[1].c = light.applyLight(mats[k], view_vector, tri[1].normal, null);
							tri[2].c = light.applyLight(mats[k], view_vector, tri[2].normal, null);

						}

						tri[0].x = (int) v0.x;
						tri[0].y = (int) v0.y;
						tri[0].z = (int) v0.z;
						tri[1].x = (int) v1.x;
						tri[1].y = (int) v1.y;
						tri[1].z = (int) v1.z;
						tri[2].x = (int) v2.x;
						tri[2].y = (int) v2.y;
						tri[2].z = (int) v2.z;

						if(pointLight || infiniteLight || ambientLight)
							SketchBase.drawTriangle(buff, tri[0], tri[1], tri[2], shading, depBuffer, mats[k], view_vector, light);
					}

					v0 = mesh.v[i][j];
					v1 = mesh.v[i + 1][j + 1];
					v2 = mesh.v[i + 1][j];
					triangle_normal = computeTriangleNormal(v0, v1, v2);

					if (view_vector.dotProduct(triangle_normal) > 0.0) // front-facing
																		// triangle?
					{
						if (shading == 2) {
							// vertex colors for Gouraud shading
							n0 = mesh.n[i][j];
							n1 = mesh.n[i + 1][j + 1];
							n2 = mesh.n[i + 1][j];
							tri[0].c = null;
							tri[1].c = null;
							tri[2].c = null;
							if(infiniteLight){
								tri[0].c = light.applyLight(mats[k], view_vector, n0, tri[0].c);
								tri[1].c = light.applyLight(mats[k], view_vector, n1, tri[1].c);
								tri[2].c = light.applyLight(mats[k], view_vector, n2, tri[2].c);
							}
							if(pointLight){
								if(radialAtten){
									plight.radialAtten = true;
								}
								if(angularAtten){
									plight.angularAtten = true;
								}
								plight.calL(tri[0].x, tri[0].y, tri[0].z);
								tri[0].c = plight.applyLight(mats[k], view_vector, n0, tri[0].c);
								plight.calL(tri[1].x, tri[1].y, tri[1].z);
								tri[1].c = plight.applyLight(mats[k], view_vector, n1, tri[1].c);
								plight.calL(tri[2].x, tri[2].y, tri[2].z);
								tri[2].c = plight.applyLight(mats[k], view_vector, n2, tri[2].c);
							}
							if(ambientLight){
								tri[0].c = alight.applyLight(mats[k], view_vector, n0, tri[0].c);
								tri[1].c = alight.applyLight(mats[k], view_vector, n1, tri[1].c);
								tri[2].c = alight.applyLight(mats[k], view_vector, n2, tri[2].c);
							}
						} else if (shading == 1) {
							// flat shading: use the normal to the triangle
							// itself
							n2 = n1 = n0 = triangle_normal;
							tri[2].c = null;
							if(infiniteLight)
								tri[2].c = tri[1].c = tri[0].c = light.applyLight(mats[k], view_vector, triangle_normal, tri[2].c);
							if(pointLight){
								if(radialAtten){
									plight.radialAtten = true;
								}
								if(angularAtten){
									plight.angularAtten = true;
								}
								plight.calL(tri[2].x, tri[2].y, tri[2].z);
								tri[2].c = tri[1].c = tri[0].c = plight.applyLight(mats[k], view_vector, triangle_normal, tri[2].c);
							}
							if(ambientLight)
								tri[2].c = tri[1].c = tri[0].c = alight.applyLight(mats[k], view_vector, triangle_normal, tri[2].c);
						} else {
							// vertex normal for Phong shading
							tri[0].normal = mesh.n[i][j];
							tri[1].normal = mesh.n[i + 1][j + 1];
							tri[2].normal = mesh.n[i + 1][j];
							tri[0].c = light.applyLight(mats[k], view_vector, tri[0].normal, null);
							tri[1].c = light.applyLight(mats[k], view_vector, tri[1].normal, null);
							tri[2].c = light.applyLight(mats[k], view_vector, tri[2].normal, null);
						}

						tri[0].x = (int) v0.x;
						tri[0].y = (int) v0.y;
						tri[0].z = (int) v0.z;
						tri[1].x = (int) v1.x;
						tri[1].y = (int) v1.y;
						tri[1].z = (int) v1.z;
						tri[2].x = (int) v2.x;
						tri[2].y = (int) v2.y;
						tri[2].z = (int) v2.z;
						
						if(pointLight || infiniteLight || ambientLight)
							SketchBase.drawTriangle(buff, tri[0], tri[1], tri[2], shading, depBuffer, mats[k], view_vector, light);
					}
				}
			}

			if (hasEndcap && k == 3) {
				for (int index1 = 0; index1 < 2; index1++) {
					for (int index = 1; index < m; index++) {
						v0 = endcapmesh.v[0][index1];
						if (index == m - 1) {
							v1 = endcapmesh.v[1][index1];
						} else {
							v1 = endcapmesh.v[index + 1][index1];
						}
						v2 = endcapmesh.v[index][index1];

						if (index1 == 0)
							triangle_normal = computeTriangleNormal(v0, v1, v2);
						else
							triangle_normal = computeTriangleNormal(v0, v2, v1);

						if (view_vector.dotProduct(triangle_normal) > 0.0) // front-facing
																			// triangle?
						{
							if (shading == 2 || shading == 3) {
								// vertex colors for Gouraud shading
								n0 = endcapmesh.n[0][index1];
								n1 = endcapmesh.n[index][index1];
								n2 = endcapmesh.n[index + 1][index1];
								tri[0].c = null;
								//tri
								if(infiniteLight){
									tri[0].c = light.applyLight(mats[k], view_vector, n0, null);
									tri[1].c = light.applyLight(mats[k], view_vector, n1, null);
									tri[2].c = light.applyLight(mats[k], view_vector, n2, null);
								}
								if(pointLight){
									if(radialAtten){
										plight.radialAtten = true;
									}
									if(angularAtten){
										plight.angularAtten = true;
									}
									plight.calL(tri[0].x, tri[0].y, tri[0].z);
									tri[0].c = plight.applyLight(mats[k], view_vector, n0, tri[0].c);
									plight.calL(tri[1].x, tri[1].y, tri[1].z);
									tri[1].c = plight.applyLight(mats[k], view_vector, n1, tri[1].c);
									plight.calL(tri[2].x, tri[2].y, tri[2].z);
									tri[2].c = plight.applyLight(mats[k], view_vector, n2, tri[2].c);
								}
								if(ambientLight){
									tri[0].c = alight.applyLight(mats[k], view_vector, n0, tri[0].c);
									tri[1].c = alight.applyLight(mats[k], view_vector, n1, tri[1].c);
									tri[2].c = alight.applyLight(mats[k], view_vector, n2, tri[2].c);
								}
							} else {
								// flat shading: use the normal to the
								// triangle
								// itself
								n2 = n1 = n0 = triangle_normal;
								tri[2].c = null;
								if(infiniteLight)
									tri[2].c = tri[1].c = tri[0].c = light.applyLight(mats[k], view_vector, triangle_normal, tri[2].c);
								if(pointLight){
									if(radialAtten){
										plight.radialAtten = true;
									}
									plight.calL(tri[2].x, tri[2].y, tri[2].z);
									tri[2].c = tri[1].c = tri[0].c = plight.applyLight(mats[k], view_vector, triangle_normal, tri[2].c);
								}
								if(ambientLight)
									tri[2].c = tri[1].c = tri[0].c = alight.applyLight(mats[k], view_vector, triangle_normal, tri[2].c);
							
							}

							tri[0].x = (int) v0.x;
							tri[0].y = (int) v0.y;
							tri[0].z = (int) v0.z;
							tri[1].x = (int) v1.x;
							tri[1].y = (int) v1.y;
							tri[1].z = (int) v1.z;
							tri[2].x = (int) v2.x;
							tri[2].y = (int) v2.y;
							tri[2].z = (int) v2.z;

							if(pointLight || infiniteLight || ambientLight)
								SketchBase.drawTriangle(buff, tri[0], tri[1], tri[2], 2, depBuffer, mats[k], view_vector, light);
						}
					}
				}

			}
		}
	}
	// helper method that computes the unit normal to the plane of the triangle
	// degenerate triangles yield normal that is numerically zero
	private Vector3D computeTriangleNormal(Vector3D v0, Vector3D v1, Vector3D v2) {
		Vector3D e0 = v1.minus(v2);
		Vector3D e1 = v0.minus(v2);
		Vector3D norm = e0.crossProduct(e1);

		if (norm.magnitude() > 0.000001)
			norm.normalize();
		else // detect degenerate triangle and set its normal to zero
			norm.set((float) 0.0, (float) 0.0, (float) 0.0);

		return norm;
	}

}