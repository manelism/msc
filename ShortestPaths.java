package msc;

import java.util.ArrayList;
import java.util.List;

public class ShortestPaths {
	List<Vertex> path = new ArrayList<>();
	Float distance;
	
	public ShortestPaths(List<Vertex> path, Float distance) {
		this.path = path;
		this.distance = distance;
	}
	
	public List<Vertex> getPath() {
		return path;
	}
	
	public void setPath(List<Vertex> path) {
		this.path = path;
	}
	
	public Float getDistance() {
		return distance;
	}
	
	public void setDistance(Float distance) {
		this.distance = distance;
	}

	@Override
	public String toString() {
		return this.path.toString() + ", "+ this.distance;
	}
}
