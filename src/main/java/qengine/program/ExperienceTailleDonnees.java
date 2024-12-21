package qengine.program;

import fr.boreal.model.kb.api.FactBase;
import fr.boreal.storage.natives.SimpleInMemoryGraphStore;
import qengine.model.RDFAtom;
import qengine.model.StarQuery;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static qengine.program.Utils.*;
import static qengine.program.Utils.countAnswers;

public class ExperienceTailleDonnees {




    public static void main(String[] args) throws IOException {
        /*
         * On récupère tous les triplets et toutes les requêtes qu'on a générées
         */

        System.out.println("=== Parsing RDF Data ===");
        List<RDFAtom> rdfAtoms = parseRDFData(DATA_500k);

        System.out.println("\n=== Parsing Sample Queries ===");
        List<StarQuery> starQueries = new ArrayList<>();

        //On ajoute TOUTES les requêtes selon tous les templates qu'on nous a fourni
        for (String fichier_queryset : getQuerysetFiles(WORKING_DIR_QUERIES))
            starQueries.addAll(parseSparQLQueries(fichier_queryset));

        // on a toutes les requêtes, on mélange une fois
        Collections.shuffle(starQueries);



        /*
         * on ajoute tous les atomes à Integraal (j'aurais pu utiliser mon truc)
         */

        System.out.println("\n=== Adding the atoms with Integraal ===");
        FactBase factBase = new SimpleInMemoryGraphStore();
        for (RDFAtom atom : rdfAtoms) {
            factBase.add(atom);  // Stocker chaque RDFAtom dans le store
        }


        // on exécute environ 30% des requêtes pour "chauffer la machine"

        for (int i = 0; i <= starQueries.size() / 3; i++) {
            executeStarQuery(starQueries.get(i), factBase);
        }


        // on reshuffle histoire de

        Collections.shuffle(starQueries);


        //mtn qu'on a biiieeeen chauffé la machine, on mesure le temps

        long start = System.currentTimeMillis();


        // Exécuter les requêtes sur le store (et compter le nombre de réponses
        for (StarQuery starQuery : starQueries) {
            executeStarQuery(starQuery, factBase);

        }


        long end = System.currentTimeMillis();


        long temps = end - start;


        System.out.printf("temps mis : %dms" +
                "\n", temps);
    }
}
