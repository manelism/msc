package msc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.formats.FunctionalSyntaxDocumentFormat;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;

import msc.Vertex.DijkstraShortestPath;
import msc.Vertex.Edge;

public class App {


	// Load ontology from computer file
	private static OWLOntology loadOntologyFile(File file) throws OWLOntologyCreationException{		
		return OWLManager.createOWLOntologyManager().loadOntologyFromOntologyDocument(file);
	}


	// Load ontology via its url
	private static OWLOntology loadOntologyWeb(String url) throws OWLOntologyCreationException {
		// Example:	http://protege.stanford.edu/ontologies/pizza/pizza.owl
		return OWLManager.createOWLOntologyManager().loadOntology(IRI.create(url));
	}


	// Save ontology
	private static void saveOntology(OWLOntology o, File file) throws OWLOntologyStorageException, FileNotFoundException{
		OWLManager.createOWLOntologyManager().saveOntology(o, new FunctionalSyntaxDocumentFormat(), new FileOutputStream(file));
	}


	private static OWLLiteral getLabelClass(OWLOntology o, OWLClass cls) {
		OWLLiteral label = null;
		IRI IRI = cls.getIRI();
		
		for(OWLAnnotationAssertionAxiom a : o.getAnnotationAssertionAxioms(IRI))  {
			
			if(a.getProperty().isLabel()) {
				if(a.getValue() instanceof OWLLiteral) 
					label = (OWLLiteral) a.getValue();
			} 
			else {
				OWLDataFactory df = o.getOWLOntologyManager().getOWLDataFactory();
				OWLAnnotation labelAdded = df.getOWLAnnotation(df.getRDFSLabel(), df.getOWLLiteral(cls.getIRI().getFragment().toString()));
				label = (OWLLiteral) labelAdded.getValue();
			}
		}
		return label;
	}


	private static int getClassesNumber(OWLOntology o){
		int nb = 0;
		
		for (OWLClass c : o.getClassesInSignature())
			if (c.isOWLClass() && !c.isOWLThing())
				nb++;
		return nb;
	}

	
	private static int getDifferentClassesNumber(OWLOntology o1, OWLOntology o2){
		int nb = 0;
		
		for (OWLClass Ci : o1.getClassesInSignature()) {
			if (Ci.isOWLClass() && !Ci.isOWLThing())
				for(OWLClass Cj : o2.getClassesInSignature()) {
					if (Cj.isOWLClass() && !Cj.isOWLThing())
						if (getLabelClass(o1, Ci).equals(getLabelClass(o2, Cj)))
							nb++;
				}
		}
		return getClassesNumber(o1) + getClassesNumber(o2) - 2* nb;		
	}


	private static int getParentsNumberOfClass(OWLOntology o, OWLClass cls ){
		return getParentsNumberOfEachClass(o).get(cls);
	}

	
	private static Set<OWLClass> getSuperClasses(OWLOntology o, OWLClass cls){
		Set<OWLClass> superclasses = new HashSet<OWLClass>();  

		for (final OWLSubClassOfAxiom ax : o.getSubClassAxiomsForSubClass(cls)) {
			if (ax.getSuperClass().isOWLClass()) {
				OWLClass superclass = ax.getSuperClass().asOWLClass();
				superclasses.addAll(getSuperClasses(o, superclass)); 
				superclasses.add(superclass);
				}				
		}		
		return superclasses;	      
	}
	


	private static Set<OWLClass> getRootClasses(OWLOntology o){
		Set<OWLClass> root = new HashSet<>();

		for (final OWLClass c : o.getClassesInSignature()) {
			if (!c.isOWLThing() )
				root.add(recRootClasses(o, c));	
		}
		return root;	      
	}


	private static OWLClass recRootClasses(OWLOntology o, OWLClass c){

		for (final  OWLSubClassOfAxiom ax : o.getSubClassAxiomsForSubClass(c)) {
			if (ax.getSuperClass().isOWLClass())
				c = recRootClasses(o, ax.getSuperClass().asOWLClass());				
		}
		return c;	      
	}

	
	// Get all common subsumers of c1 and c2
	private static Set<OWLClass> getCS(OWLOntology o1, OWLOntology o2, OWLClass c1, OWLClass c2){
		Set<OWLClass> cs = new HashSet<>();		

		if(getRootClasses(o1).contains(c1)) {
			cs.add(c1);
			return cs;
		}
		
		if(getRootClasses(o2).contains(c2)) {
			cs.add(c2);
			return cs;
		} 
		
		for (OWLClass Ci : getSuperClasses(o1, c1)) {
			if (!Ci.isOWLThing() && Ci.isOWLClass()) {
				
				// get classes labels - same classes have same labels/names in all ontologies			
				OWLLiteral ciLabel = getLabelClass(o1, Ci);	
				for(OWLClass Cj : getSuperClasses(o2, c2))
					if (ciLabel.getLiteral().equalsIgnoreCase( getLabelClass(o2, Cj).getLiteral()))
						cs.add(Cj);
			}
		}
		return cs;	      
	}

	
	// Get the least common subsumers of c1 and c2
	private static OWLClass getLCS(OWLOntology o1, OWLOntology o2, OWLClass c1, OWLClass c2){
		
		if(getRootClasses(o1).contains(c1))
			return c1;												
		
		if(getRootClasses(o2).contains(c2))					
			return c2;
		
		Set<OWLClass> cs = getCS(o1,o2,c1,c2);
	
		HashMap<KeyShortestPaths, ShortestPaths> paths1 = getAllDistances(o1);
		HashMap<KeyShortestPaths, ShortestPaths> paths2 = getAllDistances(o2);
		
		HashMap<String, Vertex> vertex1 = setEdges(o1);
		HashMap<String, Vertex> vertex2 = setEdges(o2);
		
		Vertex source1 = vertex1.get(c1.getIRI().getFragment());
		Vertex source2 = vertex2.get(c2.getIRI().getFragment());
		
		HashMap<Float, OWLClass> lcs = new HashMap<>();	
		
		// get all distances from both of c1 and c2 to c in order to compare distances
		for (OWLClass c : cs) {
			Vertex target = vertex1.get(c.getIRI().getFragment());

			KeyShortestPaths key1 = new KeyShortestPaths(source1, target);
			KeyShortestPaths key2 = new KeyShortestPaths(source2, target);
			lcs.put(paths1.get(key1).getDistance() + paths2.get(key2).getDistance(), c);
		}

		// compare distances and get the class with the min distance (the LCS)
		Set<Float> distance = lcs.keySet();
		Float min = (float) -1.0;	
		for(Float dist : distance)
			if ( min < 0 || min > dist)
				min = dist;
		 
		return  lcs.get(min);	      
	}


	private static HashMap<OWLClass, Integer> getParentsNumberOfEachClass(OWLOntology o){
		HashMap<OWLClass, Integer> parents = new HashMap<OWLClass, Integer>();  
	
		// Print all sub classes axioms (relations)
		for(OWLClass cls : o.getClassesInSignature())      
			
			// Count number of direct different parents of each concept
			for (final OWLSubClassOfAxiom ax : o.getSubClassAxiomsForSubClass(cls))	
					if (parents.containsKey(cls))
						parents.put(cls, (int)parents.get(cls)+1);
					else
						parents.put(cls, 1);      
		return parents;
	}


	private static HashMap<OWLSubClassOfAxiom, Float> getNecDegOfEachSubClassOfAxiom(OWLOntology o){
		HashMap<OWLSubClassOfAxiom, Float> necdeg = new HashMap<OWLSubClassOfAxiom, Float>(); 
		HashMap<OWLClass, Integer> parents = getParentsNumberOfEachClass(o);	

		for(OWLClass cls : o.getClassesInSignature())
			
			for (final OWLSubClassOfAxiom ax : o.getSubClassAxiomsForSubClass(cls)) 
					necdeg.put(ax, (float) 1 / parents.get(cls));      
		return necdeg;
	}
	

	private static HashMap<String, Vertex> setVertexes(OWLOntology o){
		String conceptname;
		Vertex conceptvertex;
		HashMap<String, Vertex> vertexes =  new HashMap<String, Vertex>();

		for(OWLClass cls : o.getClassesInSignature()) {
			if (!cls.isOWLThing()) {
				conceptname = cls.getIRI().getFragment().toString();
				conceptvertex = new Vertex(conceptname);
				vertexes.put(conceptname, conceptvertex);
			}
		}
		return vertexes;
	}

	
	private static HashMap<String, Vertex> setEdges(OWLOntology o){
		String subclassname, superclassname;
		float necdist;
		
		HashMap<String, Vertex> vertexes = setVertexes(o);
		HashMap<OWLSubClassOfAxiom, Float> nec =  getNecDegOfEachSubClassOfAxiom(o);
		
		for (OWLSubClassOfAxiom subclassof : nec.keySet())

			if (subclassof.getSubClass().isOWLClass() && subclassof.getSuperClass().isOWLClass()) {
				subclassname = subclassof.getSubClass().asOWLClass().getIRI().getFragment().toString();
				superclassname = subclassof.getSuperClass().asOWLClass().getIRI().getFragment().toString();

				necdist = 1 / nec.get(subclassof);
				vertexes.get(subclassname).addNeighbour(new Edge(necdist, vertexes.get(subclassname), vertexes.get(superclassname)));
			}
		return vertexes;
	}
	
	
	public static void resetGraph(HashMap<String, Vertex> vertex) {
		for (Vertex v : vertex.values()) {
			v.setVisited(false);
		}
	}

	
	public static HashMap<KeyShortestPaths, ShortestPaths> getAllDistances(OWLOntology o){
		HashMap<KeyShortestPaths, ShortestPaths> distances = new HashMap<KeyShortestPaths, ShortestPaths>();
		HashMap<String, Vertex> vertex = setEdges(o);
		
		DijkstraShortestPath shortestPath = new DijkstraShortestPath();	

		for (OWLClass Ci : o.getClassesInSignature())
			
			if (!Ci.isOWLThing()) {
				resetGraph(vertex);
				Vertex source = vertex.get(Ci.getIRI().getFragment());
				shortestPath.computeShortestPaths(source);

				for (OWLClass Cj : getSuperClasses(o, Ci))					
					if (Cj.isOWLClass()) {			 
						Vertex target = vertex.get(Cj.getIRI().getFragment());
						KeyShortestPaths key = new KeyShortestPaths(source, target);					
						ShortestPaths paths = new ShortestPaths(shortestPath.getShortestPathTo(target), target.getDistance());
						distances.put(key, paths);
					}
			}
		return distances;	
	}


	public static Float distance(OWLOntology o1, OWLOntology o2, OWLClass c1, OWLClass c2){
		
		if (getLabelClass(o1, c1).getLiteral().equalsIgnoreCase( getLabelClass(o2, c2).getLiteral()))
			return (float) 1;
		
		OWLClass lcs = getLCS(o1, o2, c1, c2);
		Set<OWLClass> roots1 = getRootClasses(o1);
		Set<OWLClass> roots2 = getRootClasses(o2);
		
		HashMap<KeyShortestPaths, ShortestPaths> paths1 = getAllDistances(o1);
		HashMap<KeyShortestPaths, ShortestPaths> paths2 = getAllDistances(o2);
		
		HashMap<String, Vertex> vertex1 = setEdges(o1);
		HashMap<String, Vertex> vertex2 = setEdges(o2);
		
		Vertex sourceLCS1 = vertex1.get(lcs.getIRI().getFragment());
		Vertex sourceLCS2 = vertex2.get(lcs.getIRI().getFragment());
		HashMap<OWLClass, Float> distances = new HashMap<>();

		if(roots1.contains(lcs))
			distances.put(lcs, (float) 1);
		else
			for(OWLClass root : roots1) {

				// get all distances from lcs to roots in ontology1
				Vertex targetRoot = vertex1.get(root.getIRI().getFragment());
				KeyShortestPaths key = new KeyShortestPaths(sourceLCS1, targetRoot);	
				distances.put(root, paths1.get(key).getDistance()); 
			}

		if(roots2.contains(lcs))
			distances.put(lcs, (float) 1);
		else 
			for(OWLClass root : roots2) {
				// get all distances from lcs to roots in ontology2
				Vertex targetRoot = vertex2.get(root.getIRI().getFragment());
				KeyShortestPaths key = new KeyShortestPaths(sourceLCS2, targetRoot);		
				distances.put(root, paths2.get(key).getDistance());
			}

		// compare distances from lcs to each root then get the min distance
		Collection<Float> dist = distances.values();

		return Collections.min(dist);	
	}

	
	public static Float simNec(OWLOntology o1, OWLOntology o2, OWLClass c1, OWLClass c2){
		OWLClass lcs = getLCS(o1, o2, c1, c2);
		
		HashMap<String, Vertex> vertex1 = setEdges(o1);
		HashMap<String, Vertex> vertex2 = setEdges(o2);
		
		Vertex vertexC1 = vertex1.get(c1.getIRI().getFragment().toString()); 	
		Vertex vertexC2 = vertex2.get(c2.getIRI().getFragment().toString());
		Vertex vertexLCS = vertex1.get(lcs.getIRI().getFragment().toString());
		
		if (vertexC2.equals(vertexC1))
			return (float) 1;
		
		HashMap<KeyShortestPaths, ShortestPaths> paths1 = getAllDistances(o1);
		HashMap<KeyShortestPaths, ShortestPaths> paths2 = getAllDistances(o2);
		
		// Compute distance between c1(c2) and its LCS
		Float distC1Lcs = null, distC2Lcs = null;
		if (vertexC1.equals(vertexLCS))
			distC1Lcs = (float) 1;												
		 else {
			KeyShortestPaths key1 = new KeyShortestPaths(vertexC1, vertexLCS);
			distC1Lcs = paths1.get(key1).getDistance();
		}
		
		if (vertexC2.equals(vertexLCS))
			distC2Lcs = (float) 1;
		 else {
			 KeyShortestPaths key2 = new KeyShortestPaths(vertexC2, vertexLCS);
			 distC2Lcs = paths2.get(key2).getDistance();
		}
		
		Float distLcsRoot = 2 * distance(o1, o2, c1, c2);
		
		return distLcsRoot / (distC1Lcs + distC2Lcs + distLcsRoot);		
	}
	
	
	public static Float weightNec(OWLOntology o, OWLClass cls){
		Float simNec = (float) 0;
		
		for(OWLClass c : o.getClassesInSignature())
			if (c.isOWLClass() && !c.isOWLThing() && !c.equals(cls))
				simNec += simNec(o, o, c, cls);
		
		return simNec / getClassesNumberDifOf(o, cls);
	}
	
	
	public static Float getClassesNumberDifOf(OWLOntology o, OWLClass cls){
		Float nb = (float) 0;
		
		for(OWLClass c : o.getClassesInSignature())
			if (c.isOWLClass() && !c.isOWLThing() && !c.equals(cls))
				nb++;
		
		return nb;
	}
	
	
	
	public static double simNecOnto(OWLOntology o1, OWLOntology o2){			
		Float simNec1 = (float) 0;
		Float w=(float)1, ww=(float) 1;
		
		for(OWLClass c1 : o1.getClassesInSignature())

			if (c1.isOWLClass() && !c1.isOWLThing()) {

				for(OWLClass c2 : o2.getClassesInSignature())
					if(c2.isOWLClass() && !c2.isOWLThing()) {
						simNec1 += weightNec(o1, c1) * weightNec(o2, c2) * simNec(o1, o2, c1, c2);
						w += weightNec(o2, c2);
					}
				ww += w * weightNec(o1, c1);
			}

		return 1 - ( simNec1 / (ww * (getClassesNumber(o1) + getClassesNumber(o2)))); // necessary semantic similarity
	}
	
	
	
	public static void main(String[] args) throws OWLOntologyCreationException {

		System.out.println("Chargement des ontologies");
		long debut = System.currentTimeMillis();
		 
			
		// Load ontologies from computer file
		OWLOntology o = loadOntologyFile(new File("/home/manel/Téléchargements/bodysystemAnnot.owl"));
		OWLOntology o2 = loadOntologyFile(new File("/home/manel/Téléchargements/bodysystemAnnot.owl")); 
		
		//Affiche la durée d'exécution en millisecondes
		long fin = System.currentTimeMillis()-debut;
		System.out.println("Ontologies chargées. Temps de chargement: "+ fin);



		// Transform DL ontology to possibilistic DL ontology
		// Note: possibility degrees are always equals to one so we just will set necessity degrees
		// Set necessity degree for each SubClassOfAxiom
		
		
		// Display necessity degrees			
		System.out.println("======================================");
		System.out.println("Display necessity degrees of subClassOf relations");
		System.out.println("======================================");
		
		HashMap<OWLSubClassOfAxiom, Float> nec =  getNecDegOfEachSubClassOfAxiom(o);
		
	 	for (OWLSubClassOfAxiom key : nec.keySet()) {

			System.out.println( "\nThe subclass: "+key.getSubClass().asOWLClass().getIRI().getFragment() +" has "+getParentsNumberOfClass(o, key.getSubClass().asOWLClass())+" parents");
			System.out.println("Necessity degree of the relation from subclass: "+key.getSubClass().asOWLClass().getIRI().getFragment() +" to superclass: "+key.getSuperClass().asOWLClass().getIRI().getFragment()+ " is: "+ nec.get(key));
		}
		
		
		
		System.out.println("======================================");
		System.out.println("Display all superclasses of each class");
		System.out.println("======================================");

		HashMap<OWLClass, Set<OWLClass>> parentsofclass = new HashMap<>();

		for (OWLClass cls:o.getClassesInSignature()) {
			if (!cls.isOWLThing()) 
				parentsofclass.put(cls, getSuperClasses(o,cls));		
		}

		System.out.println(parentsofclass);
		


		// Necessary distance. calculate the depth (distance) between 2 concepts, so calculate the shortest way (necessity degree min)
		// Finding shortest path between 2 concepts - Dijkistra algorithm
		System.out.println("======================================");
		System.out.println("Calculating minimum distance");
		System.out.println("======================================");

		
		HashMap<String, Vertex> vertex = setEdges(o);
		
		//display edges
		
		System.out.println("======================================");
		System.out.println("Display edges");
		System.out.println("======================================");
	
	
		for (String key : vertex.keySet()) {
			for (Edge edg  : vertex.get(key).getAdjacenciesList()) 
				System.out.println(vertex.get(key)+"\n NEC " + edg.getWeight()+" "+edg.getStartVertex() +" "+edg.getTargetVertex());
		}
		 


		
	
		
		System.out.println("======================================");
		System.out.println("Display shortest necessary paths from classes to root");
		System.out.println("======================================");
		 
		HashMap<KeyShortestPaths, ShortestPaths> paths = getAllDistances(o);
		
		for (OWLClass Ci:o.getClassesInSignature()) { 
			
			if (!Ci.isOWLThing() && Ci.isOWLClass()) {			
				Vertex source = vertex.get(Ci.getIRI().getFragment());
				if (!getRootClasses(o).contains(Ci)){
					
					for(OWLClass Cj:getRootClasses(o)) {
						Vertex target = vertex.get(Cj.getIRI().getFragment());
						KeyShortestPaths key = new KeyShortestPaths(source, target);

						System.out.println("\n Shortest necessary path from "+key.getSource()+" to "+key.getTarget()+ " is "+paths.get(key).getPath() + " with distance "+paths.get(key).getDistance());
					}
				}
			}
		}
		 
		
		// some tests
		
		OWLClass c1=null,c2 = null;
		for (OWLClass c:o2.getClassesInSignature()) {c2=c;}
		for (OWLClass c:o.getClassesInSignature()) {c1=c;break;} 
	
		System.out.println("Similarity necessity between "+c1.getIRI().getFragment().toString()+" in Body System ontology and "+c2.getIRI().getFragment().toString()+" in RH-MeSH ontology is: "+simNec(o,o2,c1,c2));
	
		
		System.out.println("\nSimilarity necessity between Body System and modified version of Body System ontologies is: "+ simNecOnto(o,o2));


		System.out.println("---FIN----");
	}
}

