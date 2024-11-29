package qengine.storage;

import fr.boreal.model.logicalElements.api.*;
import fr.boreal.model.logicalElements.factory.impl.SameObjectTermFactory;
import fr.boreal.model.logicalElements.impl.SubstitutionImpl;
import qengine.model.RDFAtom;
import qengine.model.StarQuery;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour la classe {@link RDFHexaStore}.
 */
public class RDFHexaStoreTest {
    private static final Literal<String> SUBJECT_1 = SameObjectTermFactory.instance().createOrGetLiteral("subject1");
    private static final Literal<String> SUBJECT_2 = SameObjectTermFactory.instance().createOrGetLiteral("subject2");
    private static final Literal<String> SUBJECT_3 = SameObjectTermFactory.instance().createOrGetLiteral("subject3");
    private static final Literal<String> SUBJECT_4 = SameObjectTermFactory.instance().createOrGetLiteral("subject4");

    private static final Literal<String> PREDICATE_1 = SameObjectTermFactory.instance().createOrGetLiteral("predicate1");
    private static final Literal<String> PREDICATE_2 = SameObjectTermFactory.instance().createOrGetLiteral("predicate2");
    private static final Literal<String> OBJECT_1 = SameObjectTermFactory.instance().createOrGetLiteral("object1");
    private static final Literal<String> OBJECT_2 = SameObjectTermFactory.instance().createOrGetLiteral("object2");
    private static final Literal<String> OBJECT_3 = SameObjectTermFactory.instance().createOrGetLiteral("object3");
    private static final Variable VAR_X = SameObjectTermFactory.instance().createOrGetVariable("?x");
    private static final Variable VAR_Y = SameObjectTermFactory.instance().createOrGetVariable("?y");
    private static final Variable VAR_Z = SameObjectTermFactory.instance().createOrGetVariable("?z");

    @Test
    public void testAddAllRDFAtoms() {
        RDFHexaStore store = new RDFHexaStore();

        // Version stream
        // Ajouter plusieurs RDFAtom
        RDFAtom rdfAtom1 = new RDFAtom(SUBJECT_1, PREDICATE_1, OBJECT_1);
        RDFAtom rdfAtom2 = new RDFAtom(SUBJECT_2, PREDICATE_2, OBJECT_2);

        Set<RDFAtom> rdfAtoms = Set.of(rdfAtom1, rdfAtom2);

        assertTrue(store.addAll(rdfAtoms.stream()), "Les RDFAtoms devraient être ajoutés avec succès.");

        // Vérifier que tous les atomes sont présents
        Collection<Atom> atoms = store.getAtoms();
        assertTrue(atoms.contains(rdfAtom1), "La base devrait contenir le premier RDFAtom ajouté.");
        assertTrue(atoms.contains(rdfAtom2), "La base devrait contenir le second RDFAtom ajouté.");

        // Version collection
        store = new RDFHexaStore();
        assertTrue(store.addAll(rdfAtoms), "Les RDFAtoms devraient être ajoutés avec succès.");

        // Vérifier que tous les atomes sont présents
        atoms = store.getAtoms();
        assertTrue(atoms.contains(rdfAtom1), "La base devrait contenir le premier RDFAtom ajouté.");
        assertTrue(atoms.contains(rdfAtom2), "La base devrait contenir le second RDFAtom ajouté.");
    }

    /*
    @Test
    public void testAddRDFAtom() {
        // est censé marché puisque AddAll marche
        throw new NotImplementedException();
    }
    */



    @Test
    public void testAddDuplicateAtom() {

        RDFHexaStore store = new RDFHexaStore();
        store.add(new RDFAtom(SUBJECT_1, PREDICATE_1, OBJECT_1));

        assertEquals(1, store.getAtoms().size());

        store.add(new RDFAtom(SUBJECT_1, PREDICATE_1, OBJECT_2));

        assertEquals(2, store.getAtoms().size());

        store.add(new RDFAtom(SUBJECT_1, PREDICATE_1, OBJECT_1)); // on remet le premier atome, normalement l'hexastore n'est pas censé l'ajouter

        assertEquals(2, store.getAtoms().size());



    }

    @Test
    public void testSize() {
        RDFHexaStore store = new RDFHexaStore();

        assertEquals(0, store.size());



        store.add(new RDFAtom(SUBJECT_1, PREDICATE_1, OBJECT_1)); // RDFAtom(subject1, triple, object1)
        store.add(new RDFAtom(SUBJECT_2, PREDICATE_1, OBJECT_2)); // RDFAtom(subject2, triple, object2)
        store.add(new RDFAtom(SUBJECT_1, PREDICATE_1, OBJECT_3)); // RDFAtom(subject1, triple, object3)
        store.add(new RDFAtom(SUBJECT_2, PREDICATE_2, OBJECT_3));

        assertEquals(4, store.size());

        store.add(new RDFAtom(SUBJECT_1, PREDICATE_1, OBJECT_1)); // on remet le premier atome, normalement l'hexastore n'est pas censé l'ajouter

        assertEquals(4, store.getAtoms().size());

        store.add(new RDFAtom(SUBJECT_2, PREDICATE_1, OBJECT_3));

        assertEquals(5, store.size());



    }

    @Test
    public void testMatchAtom() {
        RDFHexaStore store = new RDFHexaStore();
        store.add(new RDFAtom(SUBJECT_1, PREDICATE_1, OBJECT_1)); // RDFAtom(subject1, triple, object1)
        store.add(new RDFAtom(SUBJECT_2, PREDICATE_1, OBJECT_2)); // RDFAtom(subject2, triple, object2)
        store.add(new RDFAtom(SUBJECT_1, PREDICATE_1, OBJECT_3)); // RDFAtom(subject1, triple, object3)
        store.add(new RDFAtom(SUBJECT_2, PREDICATE_2, OBJECT_3));


            // tests générés par gpt mais j'ai modifié les bêtises et vérifiés à la main que les tests correspondent bien aux valeurs ajoutées juste en haut là
            // ** Cas 1: spo **
            RDFAtom query1 = new RDFAtom(SUBJECT_1, PREDICATE_1, OBJECT_1);
            assertTrue(store.match(query1).hasNext(), "Expected to match exactly RDFAtom(subject1, predicate1, object1)");


            // ** Cas 1: spo (mais le tuple n'est pas dans la base);
            RDFAtom query1False = new RDFAtom(SUBJECT_1, PREDICATE_2, OBJECT_1);
            List<Substitution> resultsFalse = collectSubstitutions(store.match(query1False));
            assertEquals(0, resultsFalse.size(), "Expected 0 match for this query");



            // ** Cas 2: ?xpo **
            RDFAtom query2 = new RDFAtom(VAR_X, PREDICATE_1, OBJECT_1);
            List<Substitution> results2 = collectSubstitutions(store.match(query2));
            assertEquals(1, results2.size(), "Expected one match for ?x predicate1 object1");
            Substitution expected2 = new SubstitutionImpl();
            expected2.add(VAR_X, SUBJECT_1);
            assertTrue(results2.contains(expected2), "Missing substitution: " + expected2);

            // ** Cas 3: s?yo **
            RDFAtom query3 = new RDFAtom(SUBJECT_1, VAR_Y, OBJECT_1);
            List<Substitution> results3 = collectSubstitutions(store.match(query3));
            assertEquals(1, results3.size(), "Expected one match for subject1 ?y object1");
            Substitution expected3 = new SubstitutionImpl();
            expected3.add(VAR_Y, PREDICATE_1);
            assertTrue(results3.contains(expected3), "Missing substitution: " + expected3);

            // ** Cas 4: sp?z **
            RDFAtom query4 = new RDFAtom(SUBJECT_1, PREDICATE_1, VAR_Z);
            List<Substitution> results4 = collectSubstitutions(store.match(query4));
            assertEquals(2, results4.size(), "Expected two matches for subject1 predicate1 ?z");
            Substitution expected4_1 = new SubstitutionImpl();
            expected4_1.add(VAR_Z, OBJECT_1);
            Substitution expected4_2 = new SubstitutionImpl();
            expected4_2.add(VAR_Z, OBJECT_3);
            assertTrue(results4.contains(expected4_1), "Missing substitution: " + expected4_1);
            assertTrue(results4.contains(expected4_2), "Missing substitution: " + expected4_2);

            // ** Cas 5: ?x?yo **
            RDFAtom query5 = new RDFAtom(VAR_X, VAR_Y, OBJECT_3);
            List<Substitution> results5 = collectSubstitutions(store.match(query5));
            assertEquals(2, results5.size(), "Expected two matches for ?x ?y object1");
            Substitution expected5_1 = new SubstitutionImpl();
            expected5_1.add(VAR_X, SUBJECT_1);
            expected5_1.add(VAR_Y, PREDICATE_1);
            Substitution expected5_2 = new SubstitutionImpl();
            expected5_2.add(VAR_X, SUBJECT_2);
            expected5_2.add(VAR_Y, PREDICATE_2);
            assertTrue(results5.contains(expected5_1), "Missing substitution: " + expected5_1);
            assertTrue(results5.contains(expected5_2), "Missing substitution: " + expected5_2);

            // ** Cas 6: ?x p ?z **
            RDFAtom query6 = new RDFAtom(VAR_X, PREDICATE_1, VAR_Z);
            List<Substitution> results6 = collectSubstitutions(store.match(query6));
            assertEquals(3, results6.size(), "Expected three matches for ?x predicate1 ?z");
            Substitution expected6_1 = new SubstitutionImpl();
            expected6_1.add(VAR_X, SUBJECT_1);
            expected6_1.add(VAR_Z, OBJECT_1);
            Substitution expected6_2 = new SubstitutionImpl();
            expected6_2.add(VAR_X, SUBJECT_2);
            expected6_2.add(VAR_Z, OBJECT_2);
            Substitution expected6_3 = new SubstitutionImpl();
            expected6_3.add(VAR_X, SUBJECT_1);
            expected6_3.add(VAR_Z, OBJECT_3);
            assertTrue(results6.contains(expected6_1), "Missing substitution: " + expected6_1);
            assertTrue(results6.contains(expected6_2), "Missing substitution: " + expected6_2);
            assertTrue(results6.contains(expected6_3), "Missing substitution: " + expected6_3);

            // ** Cas 7: s ?y ?z **
            RDFAtom query7 = new RDFAtom(SUBJECT_1, VAR_Y, VAR_Z);
            List<Substitution> results7 = collectSubstitutions(store.match(query7));
            assertEquals(2, results7.size(), "Expected two matches for subject1 ?y ?z");
            Substitution expected7_1 = new SubstitutionImpl();
            expected7_1.add(VAR_Y, PREDICATE_1);
            expected7_1.add(VAR_Z, OBJECT_1);
            Substitution expected7_2 = new SubstitutionImpl();
            expected7_2.add(VAR_Y, PREDICATE_1);
            expected7_2.add(VAR_Z, OBJECT_3);
            assertTrue(results7.contains(expected7_1), "Missing substitution: " + expected7_1);
            assertTrue(results7.contains(expected7_2), "Missing substitution: " + expected7_2);

            // ** Cas 8: ?x ?y ?z **
            RDFAtom query8 = new RDFAtom(VAR_X, VAR_Y, VAR_Z);
            List<Substitution> results8 = collectSubstitutions(store.match(query8));
            assertEquals(4, results8.size(), "Expected four matches for ?x ?y ?z");
            Substitution expected8_1 = new SubstitutionImpl();
            expected8_1.add(VAR_X, SUBJECT_1);
            expected8_1.add(VAR_Y, PREDICATE_1);
            expected8_1.add(VAR_Z, OBJECT_1);
            Substitution expected8_2 = new SubstitutionImpl();
            expected8_2.add(VAR_X, SUBJECT_1);
            expected8_2.add(VAR_Y, PREDICATE_1);
            expected8_2.add(VAR_Z, OBJECT_3);
            Substitution expected8_3 = new SubstitutionImpl();
            expected8_3.add(VAR_X, SUBJECT_2);
            expected8_3.add(VAR_Y, PREDICATE_1);
            expected8_3.add(VAR_Z, OBJECT_2);
            Substitution expected8_4 = new SubstitutionImpl();
            expected8_4.add(VAR_X, SUBJECT_2);
            expected8_4.add(VAR_Y, PREDICATE_2);
            expected8_4.add(VAR_Z, OBJECT_3);
            assertTrue(results8.contains(expected8_1), "Missing substitution: " + expected8_1);
            assertTrue(results8.contains(expected8_2), "Missing substitution: " + expected8_2);
            assertTrue(results8.contains(expected8_3), "Missing substitution: " + expected8_3);
            assertTrue(results8.contains(expected8_4), "Missing substitution: " + expected8_4);
        }

// Fonction utilitaire pour collecter les substitutions
        private List<Substitution> collectSubstitutions(Iterator<Substitution> iterator) {
            List<Substitution> results = new ArrayList<>();
            iterator.forEachRemaining(results::add);
            return results;
        }




    @Test
    public void testMatchStarQuery() {
        RDFHexaStore store = new RDFHexaStore();
        store.add(new RDFAtom(SUBJECT_1, PREDICATE_1, OBJECT_1));
        store.add(new RDFAtom(SUBJECT_1, PREDICATE_1, OBJECT_3));


        store.add(new RDFAtom(SUBJECT_2, PREDICATE_1, OBJECT_1));

        store.add(new RDFAtom(SUBJECT_3, PREDICATE_1, OBJECT_3));


        store.add(new RDFAtom(SUBJECT_4, PREDICATE_1, OBJECT_1));
        store.add(new RDFAtom(SUBJECT_4, PREDICATE_1, OBJECT_3));


        // pour tester si la requête en étoile marche même si la variable réponse n'est pas tout le temps à la place du sujet
        store.add(new RDFAtom(OBJECT_2, PREDICATE_2, SUBJECT_1));
        store.add(new RDFAtom(OBJECT_2, PREDICATE_2, SUBJECT_2));



        // premier test simple, on regarde si on a  (x, p1, o1) et (x, p1, o3)


        RDFAtom atom1_1 = new RDFAtom(VAR_X, PREDICATE_1, OBJECT_1);
        RDFAtom atom1_2 = new RDFAtom(VAR_X, PREDICATE_1, OBJECT_3);

        ArrayList<RDFAtom> atomsQuery1 = new ArrayList<>();
        atomsQuery1.add(atom1_1);
        atomsQuery1.add(atom1_2);

        ArrayList<Variable> answerVariables1 = new ArrayList<>();
        answerVariables1.add(VAR_X);
        StarQuery starQuery = new StarQuery("oui",  atomsQuery1, answerVariables1);

        Substitution expected1_1 = new SubstitutionImpl();
        Substitution expected1_2 = new SubstitutionImpl();

        expected1_1.add(VAR_X, SUBJECT_1);
        expected1_2.add(VAR_X, SUBJECT_4);

        List<Substitution> starQueryAnswer1 = collectSubstitutions(store.match(starQuery));

        assertTrue(starQueryAnswer1.contains(expected1_1), "Missing substitution: " + expected1_1);
        assertTrue(starQueryAnswer1.contains(expected1_2), "Missing substitution: " + expected1_2);



        // deuxième test où la variable réponse n'est pas que à la place du sujet
        // on cherche (x, o1, p3) et (o2, p2, x)
        ArrayList<RDFAtom> atomsQuery2 = new ArrayList<>();

        RDFAtom atom2_1 = new RDFAtom(VAR_X, PREDICATE_1, OBJECT_3);
        RDFAtom atom2_2 = new RDFAtom(OBJECT_2, PREDICATE_2, VAR_X);

        atomsQuery2.add(atom2_1);
        atomsQuery2.add(atom2_2);

        ArrayList<Variable> answerVariables2 = new ArrayList<>();
        answerVariables2.add(VAR_X);
        StarQuery starQuery2 = new StarQuery("oui",  atomsQuery2, answerVariables2);

        Substitution expected2_1 = new SubstitutionImpl();
        expected2_1.add(VAR_X, SUBJECT_1);


        List<Substitution> starQueryAnswer2 = collectSubstitutions(store.match(starQuery2));

        assertTrue(starQueryAnswer2.contains(expected2_1), "Missing substitution: " + expected2_1);


    }

    // Vos autres tests d'HexaStore ici
}
