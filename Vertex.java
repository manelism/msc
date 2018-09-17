package msc;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;

public class Vertex implements Comparable<Vertex> {

	private String name;
	private List<Edge> adjacenciesList;
	private boolean visited;
	private Vertex predecessor;
	private float distance = Float.MAX_VALUE;

	public Vertex(String name) {
		this.name = name;
		this.adjacenciesList = new ArrayList<>();
	}

	public void addNeighbour(Edge edge) {
		this.adjacenciesList.add(edge);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<Edge> getAdjacenciesList() {
		return adjacenciesList;
	}

	public void setAdjacenciesList(List<Edge> adjacenciesList) {
		this.adjacenciesList = adjacenciesList;
	}

	public boolean isVisited() {
		return visited;
	}

	public void setVisited(boolean visited) {
		this.visited = visited;
	}

	public Vertex getPredecessor() {
		return predecessor;
	}

	public void setPredecessor(Vertex predecessor) {
		this.predecessor = predecessor;
	}

	public float getDistance() {
		return distance;
	}

	public void setDistance(float distance) {
		this.distance = distance;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (!(obj instanceof Vertex)) return false;
		Vertex other = (Vertex) obj;
		return this.name.equals(other.name);
	}

	@Override
	public int hashCode() {
		return this.name.hashCode();
	}

	@Override
	public String toString() {
		return this.name;
	}

	@Override
	public int compareTo(Vertex otherVertex) {
		return Float.compare(this.distance, otherVertex.getDistance());
	}


	public static class Edge {

		private float weight;
		private Vertex startVertex;
		private Vertex targetVertex;

		public Edge(float weight, Vertex startVertex, Vertex targetVertex) {
			this.weight = weight;
			this.startVertex = startVertex;
			this.targetVertex = targetVertex;
		}

		public float getWeight() {
			return weight;
		}

		public void setWeight(float weight) {
			this.weight = weight;
		}

		public Vertex getStartVertex() {
			return startVertex;
		}

		public void setStartVertex(Vertex startVertex) {
			this.startVertex = startVertex;
		}

		public Vertex getTargetVertex() {
			return targetVertex;
		}

		public void setTargetVertex(Vertex targetVertex) {
			this.targetVertex = targetVertex;
		}
	}

	public static class DijkstraShortestPath {

		public void computeShortestPaths(Vertex sourceVertex){

			sourceVertex.setDistance(0);
			PriorityQueue<Vertex> priorityQueue = new PriorityQueue<>();
			priorityQueue.add(sourceVertex);
			sourceVertex.setVisited(true);
			sourceVertex.setPredecessor(null);

			while( !priorityQueue.isEmpty() ){ 
				// Getting the minimum distance vertex from priority queue
				Vertex actualVertex = priorityQueue.poll();

				for(Edge edge : actualVertex.getAdjacenciesList()){

					Vertex v = edge.getTargetVertex();
					if( v != null && !v.isVisited() )
					{						
						float newDistance = actualVertex.getDistance() + edge.getWeight();
						priorityQueue.remove(v);
						v.setDistance(newDistance);
						v.setPredecessor(actualVertex);
						priorityQueue.add(v);
					}
				}
				actualVertex.setVisited(true);
			}
		}


		public List<Vertex> getShortestPathTo(Vertex targetVertex){
			List<Vertex> path = new ArrayList<>();

			for(Vertex vertex = targetVertex; vertex != null; vertex = vertex.getPredecessor())
				path.add(vertex);

			Collections.reverse(path);
			return path;
		}
	}
}