
//****************************************************************************
// SketchBase.  
//****************************************************************************
// Comments : 
//   Subroutines to manage and draw points, lines an triangles
//
// History :
//   Aug 2014 Created by Jianming Zhang (jimmie33@gmail.com) based on code by
//   Stan Sclaroff (from CS480 '06 poly.c)
//   Dec 2016 Modified by Linlan Chen (linlan.ch@gmail.com) to make it support phong shading in drawTrianglePhong and drawLinePhong
//  

import java.awt.image.BufferedImage;
import java.util.*;

public class SketchBase {
	public SketchBase() {
		// deliberately left blank
	}

	/**********************************************************************
	 * Draws a point. This is achieved by changing the color of the buffer at
	 * the location corresponding to the point.
	 * 
	 * @param buff
	 *            Buffer object.
	 * @param p
	 *            Point to be drawn.
	 */
	public static void drawPoint(BufferedImage buff, Point3D p, int[][] depBuffer) {
		if (p.x >= 0 && p.x < buff.getWidth() && p.y >= 0 && p.y < buff.getHeight()) {
			if (p.z >= depBuffer[p.x][buff.getHeight() - p.y - 1]) {
				buff.setRGB(p.x, buff.getHeight() - p.y - 1, p.c.getRGB_int());
				depBuffer[p.x][buff.getHeight() - p.y - 1] = p.z;
			}
		}
	}

	/**********************************************************************
	 * Draws a line segment using Bresenham's algorithm, linearly interpolating
	 * RGB color along line segment. This method only uses integer arithmetic.
	 * 
	 * @param buff
	 *            Buffer object.
	 * @param p1
	 *            First given endpoint of the line.
	 * @param p2
	 *            Second given endpoint of the line.
	 */
	public static void drawLine(BufferedImage buff, Point3D p1, Point3D p2, int[][] depBuffer, Material mats,
			Vector3D view_vector, Light light, boolean isPhong) {
		int x0 = p1.x, y0 = p1.y, z0 = p1.z;
		int xEnd = p2.x, yEnd = p2.y, zEnd = p2.z;
		int dx = Math.abs(xEnd - x0), dy = Math.abs(yEnd - y0);

		if (dx == 0 && dy == 0) {
			if (p1.y >= 0 && p1.y < buff.getHeight() && p1.x >= 0 && p1.x < buff.getWidth()) {
				if (p1.z >= depBuffer[p1.x][buff.getHeight() - p1.y - 1]) {
					drawPoint(buff, p1, depBuffer);
					depBuffer[p1.x][buff.getHeight() - p1.y - 1] = p1.z;
				}
			}
			return;
		}

		// if slope is greater than 1, then swap the role of x and y
		boolean x_y_role_swapped = (dy > dx);
		if (x_y_role_swapped) {
			x0 = p1.y;
			y0 = p1.x;
			xEnd = p2.y;
			yEnd = p2.x;
			dx = Math.abs(xEnd - x0);
			dy = Math.abs(yEnd - y0);
		}

		// initialize the decision parameter and increments
		int p = 2 * dy - dx;
		int twoDy = 2 * dy, twoDyMinusDx = 2 * (dy - dx);
		int x = x0, y = y0, z = z0;

		// set step increment to be positive or negative
		int step_x = x0 < xEnd ? 1 : -1;
		int step_y = y0 < yEnd ? 1 : -1;

		// deal with setup for color interpolation
		// first get r,g,b integer values at the end points
		int r0 = p1.c.getR_int(), rEnd = p2.c.getR_int();
		int g0 = p1.c.getG_int(), gEnd = p2.c.getG_int();
		int b0 = p1.c.getB_int(), bEnd = p2.c.getB_int();

		// compute the change in r,g,b
		int dr = Math.abs(rEnd - r0), dg = Math.abs(gEnd - g0), db = Math.abs(bEnd - b0);

		// deal with setup for normal vector interpolation
		// compute the change in normal
		Vector3D n0 = p1.normal;
		Vector3D nEnd = p2.normal;
		int step_r = r0 < rEnd ? 1 : -1;
		int step_g = g0 < gEnd ? 1 : -1;
		int step_b = b0 < bEnd ? 1 : -1;
		int whole_step_r = step_r * (dr / dx);
		int whole_step_g = step_g * (dg / dx);
		int whole_step_b = step_b * (db / dx);

		// compute remainder, which will be corrected depending on decision
		// parameter
		dr = dr % dx;
		dg = dg % dx;
		db = db % dx;

		// initialize decision parameters for red, green, and blue
		int p_r = 2 * dr - dx;
		int twoDr = 2 * dr, twoDrMinusDx = 2 * (dr - dx);
		int r = r0;

		int p_g = 2 * dg - dx;
		int twoDg = 2 * dg, twoDgMinusDx = 2 * (dg - dx);
		int g = g0;

		int p_b = 2 * db - dx;
		int twoDb = 2 * db, twoDbMinusDx = 2 * (db - dx);
		int b = b0;

		Vector3D n = n0;

		// draw start pixel
		if (x_y_role_swapped) {
			if (x >= 0 && x < buff.getHeight() && y >= 0 && y < buff.getWidth()
					&& z0 >= depBuffer[y][buff.getHeight() - x - 1]) {
				if (isPhong) {
					p1.c = light.applyLight(mats, view_vector, p1.normal, null);
					drawPoint(buff, p1, depBuffer);
				} else
					buff.setRGB(y, buff.getHeight() - x - 1, (r << 16) | (g << 8) | b);
				depBuffer[y][buff.getHeight() - x - 1] = z;
			}

		} else {
			if (y >= 0 && y < buff.getHeight() && x >= 0 && x < buff.getWidth()
					&& z0 >= depBuffer[x][buff.getHeight() - y - 1]) {
				if (isPhong) {
					p1.c = light.applyLight(mats, view_vector, p1.normal, null);
					drawPoint(buff, p1, depBuffer);
					// System.out.println(p1.c.r + " "+ p1.c.g + " " + p1.c.b +
					// " " + r + " " + g + " " +b);
				} else
					buff.setRGB(x, buff.getHeight() - y - 1, (r << 16) | (g << 8) | b);
				depBuffer[x][buff.getHeight() - y - 1] = z;
			}
		}

		while (x != xEnd) {
			// increment x and y
			x += step_x;
			if (p < 0)
				p += twoDy;
			else {
				y += step_y;
				p += twoDyMinusDx;
			}

			double distop1 = Math.abs(x - x0);
			double distop2 = Math.abs(x - xEnd);
			z = (int) (z0 * distop2 / (distop1 + distop2) + zEnd * distop1 / (distop2 + distop1));

			// increment r by whole amount slope_r, and correct for accumulated
			// error if needed
			r += whole_step_r;
			if (p_r < 0)
				p_r += twoDr;
			else {
				r += step_r;
				p_r += twoDrMinusDx;
			}

			// increment g by whole amount slope_b, and correct for accumulated
			// error if needed
			g += whole_step_g;
			if (p_g < 0)
				p_g += twoDg;
			else {
				g += step_g;
				p_g += twoDgMinusDx;
			}

			// increment b by whole amount slope_b, and correct for accumulated
			// error if needed
			b += whole_step_b;
			if (p_b < 0)
				p_b += twoDb;
			else {
				b += step_b;
				p_b += twoDbMinusDx;
			}

			float ratio = 1;
			ratio = (float) (Math.abs(x - x0)) / dx;

			n.x = ratio * p2.normal.x + (float) (1 - ratio) * p1.normal.x;
			n.y = ratio * p2.normal.y + (1 - ratio) * p1.normal.y;
			n.z = ratio * p2.normal.z + (1 - ratio) * p1.normal.z;
			n.normalize();

			if (x_y_role_swapped) {
				if (x >= 0 && x < buff.getHeight() && y >= 0 && y < buff.getWidth()
						&& z >= depBuffer[y][buff.getHeight() - x - 1]) {

					buff.setRGB(y, buff.getHeight() - x - 1, (r << 16) | (g << 8) | b);
					depBuffer[y][buff.getHeight() - x - 1] = z;
				}

			} else {
				if (y >= 0 && y < buff.getHeight() && x >= 0 && x < buff.getWidth()
						&& z >= depBuffer[x][buff.getHeight() - y - 1]) {
					if (isPhong) {

						Point3D tmp = new Point3D(x, y, z, light.applyLight(mats, view_vector, n, null));
						drawPoint(buff, tmp, depBuffer);

					} else {
						buff.setRGB(x, buff.getHeight() - y - 1, (r << 16) | (g << 8) | b);
					}
					depBuffer[x][buff.getHeight() - y - 1] = z;
				}
			}
		}
	}

	/**********************************************************************
	 * Draws a filled triangle. The triangle may be filled using flat fill or
	 * smooth fill. This routine fills columns of pixels within the left-hand
	 * part, and then the right-hand part of the triangle.
	 * 
	 * * /|\ / | \ / | \ *---|---* left-hand right-hand part part
	 *
	 * @param buff
	 *            Buffer object.
	 * @param p1
	 *            First given vertex of the triangle.
	 * @param p2
	 *            Second given vertex of the triangle.
	 * @param p3
	 *            Third given vertex of the triangle.
	 * @param do_smooth
	 *            Flag indicating whether flat fill or smooth fill should be
	 *            used.
	 */
	public static void drawTriangle(BufferedImage buff, Point3D p1, Point3D p2, Point3D p3, int shading,
			int[][] depBuffer, Material mats, Vector3D view_vector, Light light) {
		// sort the triangle vertices by ascending x value
		Point3D p[] = sortTriangleVerts(p1, p2, p3);

		int x;
		float y_a, y_b;
		float dy_a, dy_b;
		float dr_a = 0, dg_a = 0, db_a = 0, dr_b = 0, dg_b = 0, db_b = 0;
		// p[2].c = light.applyLight(mats, view_vector, p[2].normal, null);
		Point3D side_b_end = new Point3D(p[2]);
		Point3D side_a = new Point3D(p[0]), side_b = new Point3D(p[0]);

		if (shading == 1) {
			side_a.c = new ColorType(p1.c);
			side_b.c = new ColorType(p1.c);
		}

		y_b = p[0].y;
		dy_b = ((float) (p[2].y - p[0].y)) / (p[2].x - p[0].x);

		if (shading == 2) {
			// calculate slopes in r, g, b for segment b
			dr_b = ((float) (p[2].c.r - p[0].c.r)) / (p[2].x - p[0].x);
			dg_b = ((float) (p[2].c.g - p[0].c.g)) / (p[2].x - p[0].x);
			db_b = ((float) (p[2].c.b - p[0].c.b)) / (p[2].x - p[0].x);
		}

		// if there is a left-hand part to the triangle then fill it
		if (p[0].x != p[1].x) {
			y_a = p[0].y;
			dy_a = ((float) (p[1].y - p[0].y)) / (p[1].x - p[0].x);

			if (shading == 2) {
				// calculate slopes in r, g, b for segment a
				dr_a = ((float) (p[1].c.r - p[0].c.r)) / (p[1].x - p[0].x);
				dg_a = ((float) (p[1].c.g - p[0].c.g)) / (p[1].x - p[0].x);
				db_a = ((float) (p[1].c.b - p[0].c.b)) / (p[1].x - p[0].x);
			}

			if (shading == 3) {
				double ratio = (float) (p[1].x - p[0].x) / (p[2].x - p[0].x);
				side_b_end.x = p[1].x;
				side_b_end.y = (int) (ratio * p[2].y + (1 - ratio) * p[0].y);
				side_b_end.z = (int) (ratio * p[2].z + (1 - ratio) * p[0].z);
				side_b_end.normal.x = (float) (ratio * p[2].normal.x + (1 - ratio) * p[0].normal.x);
				side_b_end.normal.y = (float) (ratio * p[2].normal.y + (1 - ratio) * p[0].normal.y);
				side_b_end.normal.z = (float) (ratio * p[2].normal.z + (1 - ratio) * p[0].normal.z);
				side_b_end.normal.normalize();
			}

			// loop over the columns for left-hand part of triangle
			// filling from side a to side b of the span
			for (x = p[0].x; x < p[1].x; ++x) {
				if (shading == 1 || shading == 2) {
					drawLine(buff, side_a, side_b, depBuffer, mats, view_vector, light, false);

					++side_a.x;
					++side_b.x;
					y_a += dy_a;
					y_b += dy_b;
					side_a.y = (int) y_a;
					side_b.y = (int) y_b;
					if (shading == 2) {
						side_a.c.r += dr_a;
						side_b.c.r += dr_b;
						side_a.c.g += dg_a;
						side_b.c.g += dg_b;
						side_a.c.b += db_a;
						side_b.c.b += db_b;
					}
				}
				if (shading == 3) {

					drawLine(buff, side_a, side_b, depBuffer, mats, view_vector, light, true);

					++side_a.x;
					++side_b.x;
					y_a += dy_a;
					y_b += dy_b;
					side_a.y = (int) y_a;
					side_b.y = (int) y_b;

					float ratio = (float) (side_a.x - p[0].x) / (float) (p[1].x - p[0].x);
					side_a.normal.x = ratio * p[1].normal.x + (1 - ratio) * p[0].normal.x;
					side_a.normal.y = ratio * p[1].normal.y + (1 - ratio) * p[0].normal.y;
					side_a.normal.z = ratio * p[1].normal.z + (1 - ratio) * p[0].normal.z;
					side_a.normal.normalize();
					side_b.normal.x = ratio * side_b_end.normal.x + (1 - ratio) * p[0].normal.x;
					side_b.normal.y = ratio * side_b_end.normal.y + (1 - ratio) * p[0].normal.y;
					side_b.normal.z = ratio * side_b_end.normal.z + (1 - ratio) * p[0].normal.z;
					side_b.normal.normalize();
					side_a.c = light.applyLight(mats, view_vector, side_a.normal, null);
					side_b.c = light.applyLight(mats, view_vector, side_b.normal, null);
				}
			}
		}

		// there is no right-hand part of triangle
		if (p[1].x == p[2].x)
			return;

		// set up to fill the right-hand part of triangle
		// replace segment a
		side_a = new Point3D(p[1]);
		if (shading == 1)
			side_a.c = new ColorType(p1.c);

		if (shading == 3)
			side_a.c = light.applyLight(mats, view_vector, side_a.normal, null);

		y_a = p[1].y;
		dy_a = ((float) (p[2].y - p[1].y)) / (p[2].x - p[1].x);
		if (shading == 2) {
			// calculate slopes in r, g, b for replacement for segment a
			dr_a = ((float) (p[2].c.r - p[1].c.r)) / (p[2].x - p[1].x);
			dg_a = ((float) (p[2].c.g - p[1].c.g)) / (p[2].x - p[1].x);
			db_a = ((float) (p[2].c.b - p[1].c.b)) / (p[2].x - p[1].x);
		}

		// loop over the columns for right-hand part of triangle
		// filling from side a to side b of the span
		for (x = p[1].x; x <= p[2].x; ++x) {
			if (shading == 1 || shading == 2) {
				drawLine(buff, side_a, side_b, depBuffer, mats, view_vector, light, false);

				++side_a.x;
				++side_b.x;
				y_a += dy_a;
				y_b += dy_b;
				side_a.y = (int) y_a;
				side_b.y = (int) y_b;
				if (shading == 2) {
					side_a.c.r += dr_a;
					side_b.c.r += dr_b;
					side_a.c.g += dg_a;
					side_b.c.g += dg_b;
					side_a.c.b += db_a;
					side_b.c.b += db_b;
				}
			}
			if (shading == 3) {
				drawLine(buff, side_a, side_b, depBuffer, mats, view_vector, light, true);

				++side_a.x;
				++side_b.x;
				y_a += dy_a;
				y_b += dy_b;
				side_a.y = (int) y_a;
				side_b.y = (int) y_b;
				float ratio = (float) (x - p[1].x) / (float) (p[2].x - p[1].x);
				side_a.normal.x = ratio * p[2].normal.x + (1 - ratio) * p[1].normal.x;
				side_a.normal.y = ratio * p[2].normal.y + (1 - ratio) * p[1].normal.y;
				side_a.normal.z = ratio * p[2].normal.z + (1 - ratio) * p[1].normal.z;
				side_a.normal.normalize();
				side_b.normal.x = ratio * p[2].normal.x + (1 - ratio) * side_b_end.normal.x;
				side_b.normal.y = ratio * p[2].normal.y + (1 - ratio) * side_b_end.normal.y;
				side_b.normal.z = ratio * p[2].normal.z + (1 - ratio) * side_b_end.normal.z;
				side_b.normal.normalize();
				side_a.c = light.applyLight(mats, view_vector, side_a.normal, null);
				side_b.c = light.applyLight(mats, view_vector, side_b.normal, null);
			}

		}
	}

	/**********************************************************************
	 * Helper function to bubble sort triangle vertices by ascending x value.
	 * 
	 * @param p1
	 *            First given vertex of the triangle.
	 * @param p2
	 *            Second given vertex of the triangle.
	 * @param p3
	 *            Third given vertex of the triangle.
	 * @return Array of 3 points, sorted by ascending x value.
	 */
	private static Point3D[] sortTriangleVerts(Point3D p1, Point3D p2, Point3D p3) {
		Point3D pts[] = { p1, p2, p3 };
		Point3D tmp;
		int j = 0;
		boolean swapped = true;

		while (swapped) {
			swapped = false;
			j++;
			for (int i = 0; i < 3 - j; i++) {
				if (pts[i].x > pts[i + 1].x) {
					tmp = pts[i];
					tmp.normal = pts[i].normal;
					pts[i] = pts[i + 1];
					pts[i].normal = pts[i + 1].normal;
					pts[i + 1] = tmp;
					pts[i + 1].normal = tmp.normal;
					swapped = true;
				}
			}
		}
		return (pts);
	}

	static public void drawTrianglePhong1(BufferedImage buff, Point3D p1, Point3D p2, Point3D p3, int[][] depBuffer,
			Material mats, Vector3D view_vector, Light light) {
		// sort the triangle vertices by ascending x value

		Point3D p[] = sortTriangleVerts(p1, p2, p3);
		int x;
		float y_a, y_b;
		float dy_a, dy_b;

		Point3D side_b_end = new Point3D(p[2]);
		Point3D side_a = new Point3D(p[0]), side_b = new Point3D(p[0]);
		y_b = p[0].y;
		dy_b = ((float) (p[2].y - p[0].y)) / (p[2].x - p[0].x);

		// if there is a left-hand part to the triangle then fill it
		if (p[0].x != p[1].x) {
			y_a = p[0].y;
			dy_a = ((float) (p[1].y - p[0].y)) / (p[1].x - p[0].x);

			double ratio = (float) (p[1].x - p[0].x) / (p[2].x - p[0].x);
			side_b_end.x = p[1].x;
			side_b_end.y = (int) (ratio * p[2].y + (1 - ratio) * p[0].y);
			side_b_end.z = (int) (ratio * p[2].z + (1 - ratio) * p[0].z);
			side_b_end.normal.x = (float) (ratio * p[2].normal.x + (1 - ratio) * p[0].normal.x);
			side_b_end.normal.y = (float) (ratio * p[2].normal.y + (1 - ratio) * p[0].normal.y);
			side_b_end.normal.z = (float) (ratio * p[2].normal.z + (1 - ratio) * p[0].normal.z);
			side_b_end.normal.normalize();

			// loop over the columns for left-hand part of triangle
			// filling from side a to side b of the span
			for (x = p[0].x; x < p[1].x; ++x) {

				drawLinePhong1(buff, side_a, side_b, depBuffer, mats, view_vector, light);

				++side_a.x;
				++side_b.x;
				y_a += dy_a;
				y_b += dy_b;
				side_a.y = (int) y_a;
				side_b.y = (int) y_b;

				ratio = (float) (side_a.x - p[0].x) / (float) (p[1].x - p[0].x);
				side_a.normal.x = (float) (ratio * p[1].normal.x + (1 - ratio) * p[0].normal.x);
				side_a.normal.y = (float) (ratio * p[1].normal.y + (1 - ratio) * p[0].normal.y);
				side_a.normal.z = (float) (ratio * p[1].normal.z + (1 - ratio) * p[0].normal.z);
				side_a.normal.normalize();
				side_b.normal.x = (float) (ratio * side_b_end.normal.x + (1 - ratio) * p[0].normal.x);
				side_b.normal.y = (float) (ratio * side_b_end.normal.y + (1 - ratio) * p[0].normal.y);
				side_b.normal.z = (float) (ratio * side_b_end.normal.z + (1 - ratio) * p[0].normal.z);
				side_b.normal.normalize();
				
				side_a.c = light.applyLight(mats, view_vector, side_a.normal, null);
				
				side_b.c = light.applyLight(mats, view_vector, side_b.normal, null);
			
			}
		}

		// there is no right-hand part of triangle
		if (p[1].x == p[2].x)
			return;

		// set up to fill the right-hand part of triangle
		// replace segment a
		side_a = new Point3D(p[1]);

		side_a.c = light.applyLight(mats, view_vector, p[1].normal, null);

		y_a = p[1].y;
		dy_a = ((float) (p[2].y - p[1].y)) / (p[2].x - p[1].x);

		// loop over the columns for right-hand part of triangle
		// filling from side a to side b of the span
		for (x = p[1].x; x <= p[2].x; ++x) {

			drawLinePhong1(buff, side_a, side_b, depBuffer, mats, view_vector, light);

			++side_a.x;
			++side_b.x;
			y_a += dy_a;
			y_b += dy_b;
			side_a.y = (int) y_a;
			side_b.y = (int) y_b;
			float ratio = (float) (x - p[1].x) / (float) (p[2].x - p[1].x);
			side_a.normal.x = ratio * p[2].normal.x + (1 - ratio) * p[1].normal.x;
			side_a.normal.y = ratio * p[2].normal.y + (1 - ratio) * p[1].normal.y;
			side_a.normal.z = ratio * p[2].normal.z + (1 - ratio) * p[1].normal.z;
			side_a.normal.normalize();
			side_b.normal.x = ratio * p[2].normal.x + (1 - ratio) * side_b_end.normal.x;
			side_b.normal.y = ratio * p[2].normal.y + (1 - ratio) * side_b_end.normal.y;
			side_b.normal.z = ratio * p[2].normal.z + (1 - ratio) * side_b_end.normal.z;
			side_b.normal.normalize();
			side_a.c = light.applyLight(mats, view_vector, side_a.normal, null);
			side_b.c = light.applyLight(mats, view_vector, side_b.normal, null);
		}
	}

	public static void drawLinePhong1(BufferedImage buff, Point3D p1, Point3D p2, int[][] depBuffer, Material mats,
			Vector3D view_vector, Light light) {

		int x0 = p1.x, y0 = p1.y, z0 = p1.z;
		int xEnd = p2.x, yEnd = p2.y, zEnd = p2.z;
		int dx = Math.abs(xEnd - x0), dy = Math.abs(yEnd - y0);

		if (dx == 0 && dy == 0) {
			if (p1.y >= 0 && p1.y < buff.getHeight() && p1.x >= 0 && p1.x < buff.getWidth()) {
				if (p1.z >= depBuffer[p1.x][buff.getHeight() - p1.y - 1]) {
					p1.c = light.applyLight(mats, view_vector, p1.normal, null);
					drawPoint(buff, p1, depBuffer);
					depBuffer[p1.x][buff.getHeight() - p1.y - 1] = p1.z;
				}
			}
			return;
		}

		Vector3D n0 = p1.normal;
		Vector3D nEnd = p2.normal;

		Vector3D n = n0;

		int x = x0;
		int y = y0;
		int z = z0;

		// draw start pixel

		if (y >= 0 && y < buff.getHeight() && x >= 0 && x < buff.getWidth()
				&& z0 >= depBuffer[x][buff.getHeight() - y - 1]) {
			p1.c = light.applyLight(mats, view_vector, p1.normal, null);
			drawPoint(buff, p1, depBuffer);
			depBuffer[x][buff.getHeight() - y - 1] = z;
		}

		while (y != yEnd) {
			// increment x and y

			if (yEnd > y0)
				y++;
			else
				y--;

			double distop1 = Math.abs(y - y0);
			double distop2 = Math.abs(y - yEnd);
			z = (int) (z0 * distop2 / (distop1 + distop2) + zEnd * distop1 / (distop2 + distop1));

			float ratio = (float) (Math.abs(y - y0)) / dy;
			n.x = ratio * p2.normal.x + (float) (1 - ratio) * p1.normal.x;
			n.y = ratio * p2.normal.y + (float) (1 - ratio) * p1.normal.y;
			n.z = ratio * p2.normal.z + (float) (1 - ratio) * p1.normal.z;
			n.normalize();

			if (y >= 0 && y < buff.getHeight() && x >= 0 && x < buff.getWidth()
					&& z >= depBuffer[x][buff.getHeight() - y - 1]) {
				Point3D tmp = new Point3D(x, y, z, light.applyLight(mats, view_vector, n, null));
				drawPoint(buff, tmp, depBuffer);
				depBuffer[x][buff.getHeight() - y - 1] = z;
			}

		}
	}

	public static void drawLinePhong(BufferedImage buff, Point3D p1, Point3D p2, int[][] zBuff, Material mat,
			Vector3D view_vector, Light light) {
		Vector3D n = new Vector3D(p1.normal);
		float normal_dx = 0, normal_dy = 0, normal_dz = 0;
		float steps = 0;
		int step_x = Math.abs(p2.x - p1.x);
		int step_y = Math.abs(p2.y - p1.y);
		steps = Math.max(step_x, step_y);
		float dx = (p2.x - p1.x) / steps;
		float dy = (p2.y - p1.y) / steps;
		float dz = (p2.z - p1.z) / steps;

		if (p1.normal != null && p2.normal != null) {
			normal_dx = (p2.normal.x - p1.normal.x) / steps;
			normal_dy = (p2.normal.y - p1.normal.y) / steps;
			normal_dz = (p2.normal.z - p1.normal.z) / steps;
		}

		Point3D tmp = new Point3D(p1.x, p1.y, p1.z, p1.c);
		tmp.normal = new Vector3D(p1.normal);

		tmp.c = light.applyLight(mat, view_vector, n, null);

		drawPoint(buff, tmp, zBuff);

		float x = tmp.x, y = tmp.y, z = tmp.z;

		for (int i = 0; i < steps; i++) {
			x += dx;
			y += dy;
			z += dz;

			tmp.x = (int) x;
			tmp.y = (int) y;
			tmp.z = (int) z;
			// normal vector
			n.x += normal_dx;
			n.y += normal_dy;
			n.z += normal_dz;
			n.normalize();
			// calculate color every step
			tmp.c = light.applyLight(mat, view_vector, n, null);
			drawPoint(buff, tmp, zBuff);
		}
	}

	public static void drawTrianglePhong(BufferedImage buff, Point3D p1, Point3D p2, Point3D p3, Vector3D n1,
			Vector3D n2, Vector3D n3, int[][] zBuff, Material mat, Vector3D view_vector, Light light) {
		// sort the triangle vertices by ascending x value
		float dx1, dy1, dz1, dx2, dy2, dz2;
		float normal_dx1 = 0, normal_dx2 = 0, normal_dy1 = 0, normal_dy2 = 0, normal_dz1 = 0, normal_dz2 = 0;
		p1.normal = n1;
		p2.normal = n2;
		p3.normal = n3;

		// sort vertices in x ascending order
		Point3D p[] = new Point3D[] { p1, p2, p3 };
		Arrays.sort(p);

		Point3D a = new Point3D(p[0]), b = new Point3D(p[0]);
		float ay = a.y, az = a.z, by = b.y, bz = b.z;

		// draw the left side of the triangle

		if (p[0].x != p[1].x) {

			float steps = p[1].x - p[0].x;

			dx1 = 1.0f;
			dy1 = (p[1].y - p[0].y) / steps;
			dz1 = (p[1].z - p[0].z) / steps;
			if (p[1].normal != null && p[0].normal != null) {
				normal_dx1 = (p[1].normal.x - p[0].normal.x) / steps;
				normal_dy1 = (p[1].normal.y - p[0].normal.y) / steps;
				normal_dz1 = (p[1].normal.z - p[0].normal.z) / steps;

			}

			steps = p[2].x - p[0].x;
			dx2 = 1.0f;
			dy2 = (p[2].y - p[0].y) / steps;
			dz2 = (p[2].z - p[0].z) / steps;
			if (p[2].normal != null && p[0].normal != null) {
				normal_dx2 = (p[2].normal.x - p[0].normal.x) / steps;
				normal_dy2 = (p[2].normal.y - p[0].normal.y) / steps;
				normal_dz2 = (p[2].normal.z - p[0].normal.z) / steps;
			}

			// draw left
			for (int i = p[0].x; i < p[1].x; i++) {
				drawLinePhong(buff, a, b, zBuff, mat, view_vector, light);

				a.x += 1;
				b.x += 1;

				ay += dy1;
				az += dz1;
				by += dy2;
				bz += dz2;

				a.y = (int) ay;
				a.z = (int) az;
				b.y = (int) by;
				b.z = (int) bz;

				a.normal.x += normal_dx1;
				a.normal.y += normal_dy1;
				a.normal.z += normal_dz1;
				b.normal.x += normal_dx2;
				b.normal.y += normal_dy2;
				b.normal.z += normal_dz2;
				a.normal.normalize();
				b.normal.normalize();
				a.c = light.applyLight(mat, view_vector, a.normal, null);
				b.c = light.applyLight(mat, view_vector, b.normal, null);

			}
		}

		if (p[1].x == p[2].x)
			return;

		a = new Point3D(p[1]);

		float steps = p[2].x - p[1].x;
		dx1 = 1.0f;
		dy1 = (p[2].y - p[1].y) / steps;
		dz1 = (p[2].z - p[1].z) / steps;
		if (p[2].normal != null && p[1].normal != null) {
			normal_dx1 = (p[2].normal.x - p[1].normal.x) / steps;
			normal_dy1 = (p[2].normal.y - p[1].normal.y) / steps;
			normal_dz1 = (p[2].normal.z - p[1].normal.z) / steps;
		}

		steps = p[2].x - p[0].x;
		dx2 = 1.0f;
		dy2 = (p[2].y - p[0].y) / steps;
		dz2 = (p[2].z - p[0].z) / steps;
		if (p[2].normal != null && p[0].normal != null) {
			normal_dx2 = (p[2].normal.x - p[0].normal.x) / steps;
			normal_dy2 = (p[2].normal.y - p[0].normal.y) / steps;
			normal_dz2 = (p[2].normal.z - p[0].normal.z) / steps;
		}

		ay = a.y;
		az = a.z;

		for (int i = p[1].x; i <= p[2].x; i++) {
			drawLinePhong(buff, a, b, zBuff, mat, view_vector, light);

			a.x++;
			b.x++;

			ay += dy1;
			az += dz1;
			by += dy2;
			bz += dz2;

			a.y = (int) ay;
			a.z = (int) az;
			b.y = (int) by;
			b.z = (int) bz;

			a.normal.x += normal_dx1;
			a.normal.y += normal_dy1;
			a.normal.z += normal_dz1;
			b.normal.x += normal_dx2;
			b.normal.y += normal_dy2;
			b.normal.z += normal_dz2;
			a.c = light.applyLight(mat, view_vector, a.normal, null);
			b.c = light.applyLight(mat, view_vector, b.normal, null);
		}
	}

}