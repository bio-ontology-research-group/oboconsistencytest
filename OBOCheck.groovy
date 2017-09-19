@Grapes([
          @Grab(group='org.semanticweb.elk', module='elk-owlapi', version='0.4.2'),
          @Grab(group='net.sourceforge.owlapi', module='owlapi-api', version='4.3.2'),
          @Grab(group='net.sourceforge.owlapi', module='owlapi-apibinding', version='4.3.2'),
          @Grab(group='net.sourceforge.owlapi', module='owlapi-impl', version='4.3.2'),
          @Grab(group='net.sourceforge.owlapi', module='owlapi-parsers', version='4.3.2'),
          @GrabConfig(systemClassLoader=true)
        ])

import org.semanticweb.owlapi.model.parameters.*
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.elk.owlapi.ElkReasonerConfiguration
import org.semanticweb.elk.reasoner.config.*
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.reasoner.*
import org.semanticweb.owlapi.reasoner.structural.StructuralReasoner
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.io.*;
import org.semanticweb.owlapi.owllink.*;
import org.semanticweb.owlapi.util.*;
import org.semanticweb.owlapi.search.*;
import org.semanticweb.owlapi.manchestersyntax.renderer.*;
import org.semanticweb.owlapi.reasoner.structural.*
import com.clarkparsia.owlapi.explanation.*
import com.clarkparsia.owlapi.explanation.util.*
import org.semanticweb.owlapi.manchestersyntax.renderer.*

PrintWriter fout = new PrintWriter(new BufferedWriter(new FileWriter("explanations-full.txt")))
OWLOntologyManager manager = OWLManager.createOWLOntologyManager()
def ontset = new TreeSet()
fout.println("Loading ontologies and generating imports closure")
new File("onts/").eachFile { f ->
  //  fout.println(f)
  def o = manager.loadOntologyFromOntologyDocument(f)
  ontset.add(o)
  fout.println("Imports closure of $f: "+manager.getImportsClosure(o).collect { it.getOntologyID() })
}
//fout.flush()
//fout.close()

OWLOntology ont = manager.createOntology(IRI.create("http://bio2vec.net/test-obo.owl"), ontset)


OWLDataFactory fac = manager.getOWLDataFactory()
ConsoleProgressMonitor progressMonitor = new ConsoleProgressMonitor()
OWLReasonerConfiguration config = new SimpleConfiguration(progressMonitor)
def f1 = new ElkReasonerFactory()
OWLReasoner reasoner = f1.createReasoner(ont,config)
reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY)

def bb = new BlackBoxExplanation(ont, f1, reasoner)
ExplanationGenerator expl = new HSTExplanationGenerator(bb)

/*
fout = new PrintWriter(new BufferedWriter(new FileWriter("incoherent.txt")))
reasoner.getEquivalentClasses(fac.getOWLNothing()).each { cl ->
  def label = null
  EntitySearcher.getAnnotations(cl, ont, fac.getRDFSLabel()).each { a ->
    OWLAnnotationValue val = a.getValue()
    if (val instanceof OWLLiteral) {
      label = val.getLiteral()
    }
  }
  fout.println(label + " ("+cl.toString()+")")
}
fout.flush()
fout.close()
*/

//fout = new PrintWriter(new BufferedWriter(new FileWriter("explanations-full.txt")))
fout.println("\n\nGenerating explanations for unsatisfiable classes:\n")
// counting
def map = [:].withDefault { 0 }
reasoner.getEquivalentClasses(fac.getOWLNothing()).each { cl ->
  def label = null
  EntitySearcher.getAnnotations(cl, ont, fac.getRDFSLabel()).each { a ->
    OWLAnnotationValue val = a.getValue()
    if (val instanceof OWLLiteral) {
      label = val.getLiteral()
    }
  }
  println "Generating explanations for $label ($cl)"
  fout.println("Explanation for incoherence of $label ($cl): ")
  ManchesterOWLSyntaxOWLObjectRendererImpl rendering = new ManchesterOWLSyntaxOWLObjectRendererImpl()
  rendering.setShortFormProvider(new AnnotationValueShortFormProvider(
				   [ fac.getRDFSLabel() ], Collections.<OWLAnnotationProperty, List<String>>emptyMap(), manager))
  expl.getExplanation(cl).each { ax ->
    map[ax] += 1
    def l = []
    ontset.each {
      if (EntitySearcher.containsAxiom(ax, it, true)) {
	l << it.getOntologyID()
      }
    }
    fout.println("\t"+rendering.render(ax))
    fout.println("\t\t\t$ax")
    fout.println("\t\t\tasserted in $l")
  }
  fout.println("q.e.d.")
  fout.println("\n\n")
}
fout.flush()
fout.close()
fout = new PrintWriter(new BufferedWriter(new FileWriter("explanations.txt")))
map.each { ax, count ->
  fout.println("$ax\t$count")
}
fout.flush()
fout.close()
