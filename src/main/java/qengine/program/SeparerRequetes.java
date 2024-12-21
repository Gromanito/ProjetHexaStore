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
import qengine.storage.RDFStorage;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
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


//fichier pour séparer les requêtes selon leur nombre de réponses
// on fait jusqu'à 15 (y aura un fichier 0 réponse, un fichier 1 réponse, un fichier 2 réponses etc)
public class SeparerRequetes {


    public static final String nom_fichier_0_reponse_500k = WORKING_DIR_500k + "0_reponse.queryset";
    public static final String nom_fichier_1_10_reponse_500k = WORKING_DIR_500k + "10_reponse.queryset";
    public static final String nom_fichier_plus_10_reponse_500k = WORKING_DIR_500k + "plus_10_reponse.queryset";

    public static final String nom_fichier_0_reponse_2M = WORKING_DIR_2M + "0_reponse.queryset";
    public static final String nom_fichier_1_10_reponse_2M = WORKING_DIR_2M + "10_reponse.queryset";
    public static final String nom_fichier_plus_10_reponse_2M = WORKING_DIR_2M + "plus_10_reponse.queryset";

    public static void main(String[] args) throws IOException {
        /*
         * On récupère tous les triplets et toutes les requêtes qu'on a générées
         */
        System.out.println("=== Parsing RDF Data ===");
        List<RDFAtom> rdfAtoms = parseRDFData(DATA_500k);

        System.out.println("\n=== Parsing Sample Queries ===");
        List<StarQuery> starQueries = new ArrayList<>();

        //On ajoute TOUTES les requêtes selon tous les templates qu'on nous a fourni
        for(String fichier_queryset : getQuerysetFiles(WORKING_DIR_QUERIES))
            starQueries.addAll(parseSparQLQueries(fichier_queryset));




        /* on ajoute tous les atomes à Integraal
         */
        System.out.println("\n=== Adding the atoms with Integraal===");
        FactBase factBase = new SimpleInMemoryGraphStore();
        for (RDFAtom atom : rdfAtoms) {
            factBase.add(atom);  // Stocker chaque RDFAtom dans le store
        }






        ArrayList<String> zero_reponse = new ArrayList<>();
        ArrayList<String> un_dix_reponses = new ArrayList<>();
        ArrayList<String> plus_dix_reponse = new ArrayList<>();


        // Exécuter les requêtes sur le store (et compter le nombre de réponses
        for (StarQuery starQuery : starQueries) {
            int nbrReponses = countAnswers(starQuery, factBase);

            if (nbrReponses == 0    &&     zero_reponse.size() < 15_000)
                zero_reponse.add(starQuery.getLabel());
            else if (nbrReponses <= 10    &&     un_dix_reponses.size() < 15_000 )
                un_dix_reponses.add(starQuery.getLabel());
            else if (plus_dix_reponse.size() < 15_000)
                plus_dix_reponse.add(starQuery.getLabel());

        }

        System.out.printf("zero_reponse : %d\n", zero_reponse.size());
        System.out.printf("110_reponse : %d\n", un_dix_reponses.size());
        System.out.printf("+10_reponse : %d\n", plus_dix_reponse.size());



        HashMap<String, ArrayList<String>> noms_fichiers = new HashMap<>();
        // choisir d'enregistrer les requêtes pour quelle taille? (mettre en commentaire un des 2 groupes de 3 lignes)
        noms_fichiers.put(nom_fichier_0_reponse_500k, zero_reponse);
        noms_fichiers.put(nom_fichier_1_10_reponse_500k, un_dix_reponses);
        noms_fichiers.put(nom_fichier_plus_10_reponse_500k, plus_dix_reponse);

        //noms_fichiers.put(nom_fichier_0_reponse_2M, zero_reponse);
        //noms_fichiers.put(nom_fichier_1_10_reponse_2M, un_dix_reponses);
        //noms_fichiers.put(nom_fichier_plus_10_reponse_2M, plus_dix_reponse);




        for (String filename : noms_fichiers.keySet()) {

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
                writer.write(String.join("\n\n", noms_fichiers.get(filename))); // Ajouter le contenu du fichier

            } catch (IOException e) {
                System.err.println("Erreur lors de la création ou de l'écriture dans le fichier : " + filename);
                e.printStackTrace();
            }
        }



    }


}



