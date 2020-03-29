package com.testing.PrivateExtractorGUI;

public class Point implements Comparable<Point> {
	double latitude;
	double longitude;
    long time;
 
    public double getLatitude() {
		return latitude;
	}

	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}

	public long getTime() {
		return time;
	}

	public void setTime(long time) {
		this.time = time;
	}

	@Override
    public String toString() {
        return "User [lat=" + latitude + "/long=" + longitude + "@=" + time +"]";
    }
	
	@Override
	public int hashCode() {
	    int hash = 7;
	    hash = 31 * hash + Long.hashCode(time);
	    hash = 31 * hash + Double.hashCode(latitude);
	    hash = 31 * hash + Double.hashCode(longitude);
	    return hash;
	}
	
	@Override
	public boolean equals(Object o) {
	    if(o == null)
	    {
	        return false;
	    }
	    if (o == this)
	    {
	        return true;
	    }
	    if (getClass() != o.getClass())
	    {
	        return false;
	    }
	     
	    Point e = (Point) o;
	    return (this.getLatitude() == e.getLatitude() && 
	    		this.getLongitude() == e.getLongitude() &&
	    		this.getTime() == e.getTime());
	}

	@Override
	public int compareTo(Point o) {
		if(this.time > o.time) return 1; 
	    if(this.time < o.time) return -1;
	    else                   return 0;
	}
}