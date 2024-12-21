package qengine.program;

import fr.boreal.model.formula.api.FOFormula;
import fr.boreal.model.formula.api.FOFormulaConjunction;
import fr.boreal.model.kb.api.FactBase;
import fr.boreal.model.logicalElements.api.Substitution;
import fr.boreal.model.query.api.FOQuery;
import fr.boreal.model.query.api.Query;
import fr.boreal.model.queryEvaluation.api.FOQueryEvaluator;
import fr.boreal.query_evaluation.generic.GenericFOQueryEvaluator;
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
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class Utils {

    public static final String WORKING_DIR = "data/";

    public static final String WORKING_DIR_QUERIES = WORKING_DIR + "queries/";


    public static final String WORKING_DIR_500k = WORKING_DIR + "data_500k/";
    public static final String WORKING_DIR_2M = WORKING_DIR + "data_2M/";

    public static final String DATA_500k = WORKING_DIR_500k + "data_generated_500k.nt";
    public static final String DATA_2M = WORKING_DIR_2M + "data_generated_2M.nt";


    public static List<String> getQuerysetFiles(String directoryPath) throws IOException {
        try (var stream = Files.list(Paths.get(directoryPath))) {
            return stream
                    .filter(Files::isRegularFile) // Filtre pour garder uniquement les fichiers
                    .filter(path -> path.toString().endsWith(".queryset")) // Filtre sur l'extension
                    .map(Path::toString) // Convertit les chemins en chaînes
                    .collect(Collectors.toList()); // Collecte les résultats dans une liste
        }
    }



    /**
     * Parse et affiche le contenu d'un fichier RDF.
     *
     * @param rdfFilePath Chemin vers le fichier RDF à parser
     * @return Liste des RDFAtoms parsés
     */
    public static List<RDFAtom> parseRDFData(String rdfFilePath) throws IOException {
        FileReader rdfFile = new FileReader(rdfFilePath);
        List<RDFAtom> rdfAtoms = new ArrayList<>();

        try (RDFAtomParser rdfAtomParser = new RDFAtomParser(rdfFile, RDFFormat.NTRIPLES)) {
            int count = 0;
            while (rdfAtomParser.hasNext()) {
                RDFAtom atom = rdfAtomParser.next();
                rdfAtoms.add(atom);  // Stocker l'atome dans la collection
                //System.out.println("RDF Atom #" + (++count) + ": " + atom);
            }
            //System.out.println("Total RDF Atoms parsed: " + count);
        }
        return rdfAtoms;
    }

    /**
     * Parse et affiche le contenu d'un fichier de requêtes SparQL.
     *
     * @param queryFilePath Chemin vers le fichier de requêtes SparQL
     * @return Liste des StarQueries parsées
     */
    public static List<StarQuery> parseSparQLQueries(String queryFilePath) throws IOException {
        List<StarQuery> starQueries = new ArrayList<>();

        try (StarQuerySparQLParser queryParser = new StarQuerySparQLParser(queryFilePath)) {
            int queryCount = 0;

            while (queryParser.hasNext()) {
                Query query = queryParser.next();
                if (query instanceof StarQuery starQuery) {
                    starQueries.add(starQuery);  // Stocker la requête dans la collection
                    //System.out.println("Star Query #" + (++queryCount) + ":");
                    //System.out.println("  Central Variable: " + starQuery.getCentralVariable().label());
                    //System.out.println("  RDF Atoms:");
                    //starQuery.getRdfAtoms().forEach(atom -> System.out.println("    " + atom));
                } else {
                    System.err.println("Requête inconnue ignorée.");
                }
            }
            System.out.println("Total Queries parsed: " + starQueries.size());
        }
        return starQueries;
    }

    /**
     * Exécute une requête en étoile sur le store et affiche les résultats.
     *
     * @param starQuery La requête à exécuter
     * @param factBase  Le store contenant les atomes
     */
    public static void executeStarQuery(StarQuery starQuery, FactBase factBase) {
        FOQuery<FOFormulaConjunction> foQuery = starQuery.asFOQuery(); // Conversion en FOQuery
        FOQueryEvaluator<FOFormula> evaluator = GenericFOQueryEvaluator.defaultInstance(); // Créer un évaluateur
        Iterator<Substitution> queryResults = evaluator.evaluate(foQuery, factBase); // Évaluer la requête

        //System.out.printf("Execution of  %s:%n", starQuery);
        //System.out.println("Answers:");
        if (!queryResults.hasNext()) {
            //System.out.println("No answer.");
        }
        while (queryResults.hasNext()) {
            Substitution result = queryResults.next();
            //System.out.println(result); // Afficher chaque réponse
        }
        //System.out.println();
    }


    public static int countAnswers(StarQuery starQuery, FactBase factBase) {
        FOQuery<FOFormulaConjunction> foQuery = starQuery.asFOQuery(); // Conversion en FOQuery
        FOQueryEvaluator<FOFormula> evaluator = GenericFOQueryEvaluator.defaultInstance(); // Créer un évaluateur
        Iterator<Substitution> queryResults = evaluator.evaluate(foQuery, factBase); // Évaluer la requête

        int i = 0;
        while(queryResults.hasNext()) {
            i++;
            queryResults.next();
        }

        return i;

    }


    public static int countAnswers(StarQuery starQuery, RDFHexaStore factBase) {

        Iterator<Substitution> queryResults = factBase.match(starQuery);

        int i = 0;
        while(queryResults.hasNext()) {
            i++;
            queryResults.next();
        }

        return i;

    }
}
