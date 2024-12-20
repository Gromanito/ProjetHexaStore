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

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;




//fichier pour séparer les requêtes selon leur nombre de réponses
// on fait jusqu'à 20 (y aura un fichier 0 réponse, un fichier 1 réponse, un fichier 2 réponses etc)
public class SeparerRequetes {



    private static final String WORKING_DIR = "data/";
    private static final String TEMPLATE_FICHIER = WORKING_DIR + "queries_%s_answers.queryset";
    private static final String GENERATED_DATA = WORKING_DIR + "generated_data.nt";
    private static final String GENERATED_QUERIES = WORKING_DIR + "generated_queries.queryset";

    public static void main(String[] args) throws IOException {
        /*
         * On récupère tous les triplets et toutes les requêtes qu'on a générées
         */

        System.out.println("=== Parsing RDF Data ===");
        List<RDFAtom> rdfAtoms = parseRDFData(GENERATED_DATA);

        System.out.println("\n=== Parsing Sample Queries ===");
        List<StarQuery> starQueries = parseSparQLQueries(GENERATED_QUERIES);

        /*
         * on ajoute tous les atomes à Integraal (j'aurais pu utiliser mon truc
         */
        System.out.println("\n=== Adding the atoms with Integraal ===");
        FactBase factBase = new SimpleInMemoryGraphStore();
        for (RDFAtom atom : rdfAtoms) {
            factBase.add(atom);  // Stocker chaque RDFAtom dans le store
        }


        // on va stocker le nombre de réponse (de 0 à 20) pour chaque requête
        HashMap<Integer, ArrayList<Query>> queries = new HashMap<>();

        for (int i = 0; i <= 20; i++) {
            queries.put(i, new ArrayList<Query>());
        }



        // Exécuter les requêtes sur le store (et compter le nombre de réponses
        for (StarQuery starQuery : starQueries) {
            queries.get(countAnswers(starQuery, factBase)).add(starQuery);
        }



        // pour chaque nombre de réponses possible, on crée les fichiers

        for (int i = 0; i <= 20; i++) {
            String filename = String.format( TEMPLATE_FICHIER, i);
            File file = new File(filename);

            // Supprimer le fichier s'il existe
            if (file.exists()) {
                if (file.delete()) {
                    System.out.println("Fichier supprimé : " + filename);
                } else {
                    System.err.println("Erreur lors de la suppression : " + filename);
                    continue;
                }
            }

            // Recréer le fichier et y écrire du contenu
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(String.join(',', queries.get(i))); // Ajouter le contenu du fichier
                System.out.println("Fichier créé : " + filename);
            } catch (IOException e) {
                System.err.println("Erreur lors de la création ou de l'écriture dans le fichier : " + filename);
                e.printStackTrace();
            }
        }


         }



    /**
     * Parse et affiche le contenu d'un fichier RDF.
     *
     * @param rdfFilePath Chemin vers le fichier RDF à parser
     * @return Liste des RDFAtoms parsés
     */
    private static List<RDFAtom> parseRDFData(String rdfFilePath) throws IOException {
        FileReader rdfFile = new FileReader(rdfFilePath);
        List<RDFAtom> rdfAtoms = new ArrayList<>();

        try (RDFAtomParser rdfAtomParser = new RDFAtomParser(rdfFile, RDFFormat.NTRIPLES)) {
            int count = 0;
            while (rdfAtomParser.hasNext()) {
                RDFAtom atom = rdfAtomParser.next();
                rdfAtoms.add(atom);  // Stocker l'atome dans la collection
                System.out.println("RDF Atom #" + (++count) + ": " + atom);
            }
            System.out.println("Total RDF Atoms parsed: " + count);
        }
        return rdfAtoms;
    }

    /**
     * Parse et affiche le contenu d'un fichier de requêtes SparQL.
     *
     * @param queryFilePath Chemin vers le fichier de requêtes SparQL
     * @return Liste des StarQueries parsées
     */
    private static List<StarQuery> parseSparQLQueries(String queryFilePath) throws IOException {
        List<StarQuery> starQueries = new ArrayList<>();

        try (StarQuerySparQLParser queryParser = new StarQuerySparQLParser(queryFilePath)) {
            int queryCount = 0;

            while (queryParser.hasNext()) {
                Query query = queryParser.next();
                if (query instanceof StarQuery starQuery) {
                    starQueries.add(starQuery);  // Stocker la requête dans la collection
                    System.out.println("Star Query #" + (++queryCount) + ":");
                    System.out.println("  Central Variable: " + starQuery.getCentralVariable().label());
                    System.out.println("  RDF Atoms:");
                    starQuery.getRdfAtoms().forEach(atom -> System.out.println("    " + atom));
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
    private static void executeStarQuery(StarQuery starQuery, FactBase factBase) {
        FOQuery<FOFormulaConjunction> foQuery = starQuery.asFOQuery(); // Conversion en FOQuery
        FOQueryEvaluator<FOFormula> evaluator = GenericFOQueryEvaluator.defaultInstance(); // Créer un évaluateur
        Iterator<Substitution> queryResults = evaluator.evaluate(foQuery, factBase); // Évaluer la requête

        System.out.printf("Execution of  %s:%n", starQuery);
        System.out.println("Answers:");
        if (!queryResults.hasNext()) {
            System.out.println("No answer.");
        }
        while (queryResults.hasNext()) {
            Substitution result = queryResults.next();
            System.out.println(result); // Afficher chaque réponse
        }
        System.out.println();
    }


        private static int countAnswers(StarQuery starQuery, FactBase factBase) {
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
}



