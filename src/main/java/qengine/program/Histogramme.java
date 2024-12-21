package qengine.program;

import fr.boreal.model.formula.api.FOFormula;
import fr.boreal.model.formula.api.FOFormulaConjunction;
import fr.boreal.model.kb.api.FactBase;
import fr.boreal.model.logicalElements.api.Substitution;
import fr.boreal.model.query.api.FOQuery;
import fr.boreal.model.query.api.Query;
import fr.boreal.model.queryEvaluation.api.FOQueryEvaluator;
import fr.boreal.query_evaluation.generic.GenericFOQueryEvaluator;
import fr.boreal.storage.natives.SimpleInMemoryGraphStore;
import org.eclipse.rdf4j.rio.RDFFormat;
import qengine.model.RDFAtom;
import qengine.model.StarQuery;
import qengine.parser.RDFAtomParser;
import qengine.parser.StarQuerySparQLParser;
import qengine.storage.RDFHexaStore;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import static qengine.program.Utils.*;

public class Histogramme {



    public static void main(String[] args) throws IOException {
        /*
         * On récupère tous les triplets et toutes les requêtes qu'on a générées
         */

        System.out.println("=== Parsing RDF Data ===");
        List<RDFAtom> rdfAtoms = parseRDFData(DATA_500k);

        System.out.println("\n=== Parsing Sample Queries ===");
        List<StarQuery> starQueries = new ArrayList<>();

        //On ajoute TOUTES les requêtes selon tous les templates qu'on nous a fourni
        for(String fichier_queryset : getQuerysetFiles(WORKING_DIR_500k))
            starQueries.addAll(parseSparQLQueries(fichier_queryset));

        /*
         * on ajoute tous les atomes à Integraal (j'aurais pu utiliser mon truc)

        System.out.println("\n=== Adding the atoms with Integraal ===");
        FactBase factBase = new SimpleInMemoryGraphStore();
        for (RDFAtom atom : rdfAtoms) {
            factBase.add(atom);  // Stocker chaque RDFAtom dans le store
        }
         */


        /* on ajoute tous les atomes à mon hexastore
         */
        System.out.println("\n=== Adding the atoms with jsp ===");
        FactBase factBase = new SimpleInMemoryGraphStore();
        for (RDFAtom atom : rdfAtoms) {
            factBase.add(atom);  // Stocker chaque RDFAtom dans le store
        }


        // on va stocker le nombre de réponse (de 0 à 15) pour chaque requête
        HashMap<Integer, ArrayList<String>> queries = new HashMap<>();





        // Exécuter les requêtes sur le store (et compter le nombre de réponses
        for (StarQuery starQuery : starQueries) {
            int nbrReponses = countAnswers(starQuery, factBase);
            if (!queries.containsKey(nbrReponses))
                queries.put(nbrReponses, new ArrayList<>());
            else
                queries.get(nbrReponses).add(starQuery.getLabel());
        }

        for (int i : queries.keySet()){
            System.out.printf("nombre de requetes qui ont %d réponses : %d\n", i, queries.get(i).size());
        }

    }



}
