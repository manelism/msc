package msc;

import java.util.Objects;

public class KeyShortestPaths {
	Vertex source, target;

	public  KeyShortestPaths(Vertex source, Vertex target) {
		this.source = source;
		this.target = target;
	}


    @Override
    public int hashCode() {
        return Objects.hash(source, target);
    }

    @Override
    public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (!(obj instanceof KeyShortestPaths)) return false;
		KeyShortestPaths other = (KeyShortestPaths) obj;
		return this.source.equals(other.source) && this.target.equals(other.target);
    }
    
    @Override
    public String toString() {
    	return source.toString() + ", " + target.toString();
    }
	
	
	public Vertex getSource() {
		return source;
	}

	public void setSource(Vertex source) {
		this.source = source;
	}

	public Vertex getTarget() {
		return target;
	}

	public void setTarget(Vertex target) {
		this.target = target;
	}
}
