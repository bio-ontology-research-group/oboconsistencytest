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

OWLOntologyManager manager = OWLManager.createOWLOntologyManager()
def ontset = new TreeSet()
new File("onts/").eachFile { f ->
  ontset.add(manager.loadOntologyFromOntologyDocument(f))
}

OWLOntology ont = manager.createOntology(IRI.create("http://bio2vec.net/test-obo.owl"), ontset)

OWLDataFactory fac = manager.getOWLDataFactory()
ConsoleProgressMonitor progressMonitor = new ConsoleProgressMonitor()
OWLReasonerConfiguration config = new SimpleConfiguration(progressMonitor)
def f1 = new ElkReasonerFactory()
OWLReasoner reasoner = f1.createReasoner(ont,config)
reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY)

def bb = new BlackBoxExplanation(ont, f1, reasoner)
ExplanationGenerator expl = new HSTExplanationGenerator(bb)

def map = [:].withDefault { 0 }
reasoner.getEquivalentClasses(fac.getOWLNothing()).each { cl ->
  println "Generating explanations for $cl"
  expl.getExplanation(cl).each { ax ->
    map[ax] += 1
  }
}
PrintWriter fout = new PrintWriter(new BufferedWriter(new FileWriter("expl.txt")))
map.each { ax, count ->
  fout.println("$ax\t$count")
}
fout.flush()
fout.close()
