/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jtd;

/**
 *
 * @author LostMekka
 */
public class PointF {
	
	public float x, y;

	public PointF(float x, float y) {
		this.x = x;
		this.y = y;
	}

	public PointF() {
		x = 0f;
		y = 0f;
	}
	
	public PointI getPountI(){
		return new PointI(Math.round(x), Math.round(y));
	}
	
	public float distanceTo(PointF p){
		return (float)Math.sqrt((x - p.x)*(x - p.x) + (y - p.y)*(y - p.y));
	}

	public float hammingDistanceTo(PointF p){
		return Math.abs(x - p.x) + Math.abs(y - p.y);
	}

	public float length(){
		return (float)Math.sqrt(x*x + y*y);
	}
	
	public float travelTo(PointF p, float travelDistance, boolean stopAtArrival){
		float dx = p.x - x, dy = p.y - y;
		float d = (float)Math.sqrt(dx*dx + dy*dy);
		if((travelDistance >= d) && stopAtArrival){
			x = p.x;
			y = p.y;
			return d;
		}
		x += dx * travelDistance / d;
		y += dy * travelDistance / d;
		return travelDistance;
	}

	public float getRotationTo(PointF p){
		if(p.x == x){
			if(p.y < y){
				return 180f;
			} else {
				return 0f;
			}
		}
		return (float)(Math.atan2(p.y - y, p.x - x) / Math.PI) * 180f;
	}
	
	public void rotate(float amount){
		amount = amount / 180f * (float)Math.PI;
		float tmp = x * (float)Math.cos(amount) - y * (float)Math.sin(amount);
		y = x * (float)Math.sin(amount) + y * (float)Math.cos(amount);
		x = tmp;
	}
	
	@Override
	public PointF clone(){
		return new PointF(x, y);
	}
}
