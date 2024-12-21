package qengine.storage;

import fr.boreal.model.logicalElements.api.*;
import fr.boreal.model.logicalElements.impl.SubstitutionImpl;
import org.apache.commons.lang3.NotImplementedException;
import qengine.model.RDFAtom;
import qengine.model.StarQuery;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Implémentation d'un HexaStore pour stocker des RDFAtom.
 * Cette classe utilise six index pour optimiser les recherches.
 * Les index sont basés sur les combinaisons (Sujet, Prédicat, Objet), (Sujet, Objet, Prédicat),
 * (Prédicat, Sujet, Objet), (Prédicat, Objet, Sujet), (Objet, Sujet, Prédicat) et (Objet, Prédicat, Sujet).
 */
public class RDFHexaStore implements RDFStorage {

    protected HashMap<Term, Integer> dictionaire_Term_int;
    protected HashMap<Integer, Term> dictionaire_int_Term;


    protected BPlusTree SPO;
    protected BPlusTree SOP;
    protected BPlusTree PSO;
    protected BPlusTree POS;
    protected BPlusTree OSP;
    protected BPlusTree OPS;


    public RDFHexaStore() {

        //les deux dictionnaires pour encoder et décoder les Terms
        dictionaire_Term_int = new HashMap<>();
        dictionaire_int_Term = new HashMap<>();

        // les 6 indexes (structures B+Tree)
        SPO = new BPlusTree(20);
        SOP = new BPlusTree(20);
        PSO = new BPlusTree(20);
        POS = new BPlusTree(20);
        OSP = new BPlusTree(20);
        OPS = new BPlusTree(20);

    }



    @Override
    public boolean add(RDFAtom atom) {

        Term S = atom.getTripleSubject();
        Term P = atom.getTriplePredicate();
        Term O = atom.getTripleObject();

        int Sint;
        int Pint;
        int Oint;



        // on ajoute, si c'est pas déjà fait, l'entier associé à chaque ressource RDF (pour stocker des entiers dans les index et pas les chaines directement)
        if (!dictionaire_Term_int.containsKey(S)) {
            Sint = dictionaire_Term_int.size();
            dictionaire_Term_int.put(S, Sint);
            dictionaire_int_Term.put(Sint, S);

        }
        else{
            Sint = dictionaire_Term_int.get(S);
        }

        if (!dictionaire_Term_int.containsKey(P)) {
            Pint = dictionaire_Term_int.size();
            dictionaire_Term_int.put(P, Pint);
            dictionaire_int_Term.put(Pint, P);
        }
        else{
            Pint = dictionaire_Term_int.get(P);
        }

        if (!dictionaire_Term_int.containsKey(O)) {
            Oint = dictionaire_Term_int.size();
            dictionaire_Term_int.put(O, Oint);
            dictionaire_int_Term.put(Oint, O);
        }
        else {
            Oint = dictionaire_Term_int.get(O);
        }

        // on regarde si le tuple n'existe pas déjà, s'il existe déjà on ne fait rien
        if(!SPO.search(new Key(Sint, Pint,Oint))){
            SPO.insert(new Key(Sint, Pint, Oint));
            SOP.insert(new Key(Sint, Oint, Pint));
            POS.insert(new Key(Pint, Oint, Sint));
            PSO.insert(new Key(Pint, Sint, Oint));
            OSP.insert(new Key(Oint, Sint, Pint));
            OPS.insert(new Key(Oint, Pint, Sint));

            return true;
        }
        else {
            return false;
        }
    }

    @Override
    public long size() {
        return SPO.getSize();
    }

    @Override
    public Iterator<Substitution> match(RDFAtom atom) {

        // ce que je dois faire : choisir le bon index parmi les 6 selon la forme du triplet RDF (l'endroit les variables réponses)
        // problème : je sais pas où sont les variables dans un triplet rdf ... on va dire que c'est tout ce qui commence par '?'


        // on va diviser les cas selon où sont les variables


        List<Substitution> results = new ArrayList<>();


        // on enregistre les entiers associés aux term du RDFAtom
        int Sint;
        int Pint;
        int Oint;


        boolean Svariable=false,
                Pvariable=false,
                Ovariable=false;

        if (atom.getTripleSubject().isVariable()) {
            Svariable = true;
            Sint = -1;
        } else {
            if (dictionaire_Term_int.containsKey(atom.getTripleSubject())) {
                Sint = dictionaire_Term_int.get(atom.getTripleSubject());
            }
            else{
                return (new ArrayList<Substitution>()).iterator();
            }
        }


        if (atom.getTriplePredicate().isVariable()) {
            Pvariable = true;
            Pint = -1;
        } else {
            if (dictionaire_Term_int.containsKey(atom.getTriplePredicate())) {
                Pint = dictionaire_Term_int.get(atom.getTriplePredicate());
            } else {
                return (new ArrayList<Substitution>()).iterator();
            }
        }


        if (atom.getTripleObject().isVariable()) {
            Ovariable = true;
            Oint = -1;
        } else {
            if (dictionaire_Term_int.containsKey(atom.getTripleObject())) {
                Oint = dictionaire_Term_int.get(atom.getTripleObject());
            } else {
                return (new ArrayList<Substitution>()).iterator();
            }
        }





        HashMap<Variable, Term> mapVT;


        // cas (?x ?y ?z)   -> on utilise SPO
        if(Svariable && Pvariable && Ovariable){
            for( Key key : SPO.getAllKeys() ){
                mapVT = new HashMap<>();
                mapVT.put((Variable) atom.getTripleSubject(), dictionaire_int_Term.get(key.part1));
                mapVT.put((Variable) atom.getTriplePredicate(), dictionaire_int_Term.get(key.part2));
                mapVT.put((Variable) atom.getTripleObject(), dictionaire_int_Term.get(key.part3));

                results.add(new SubstitutionImpl(mapVT));
            }
        }

        // cas (s ?y ?z)  -> on utilise SPO encore
        if(!Svariable && Pvariable && Ovariable){

            for( Key key : SPO.searchByFirstOne(Sint) ){
                mapVT = new HashMap<>();
                mapVT.put((Variable) atom.getTriplePredicate(), dictionaire_int_Term.get(key.part2));
                mapVT.put((Variable) atom.getTripleObject(), dictionaire_int_Term.get(key.part3));

                results.add(new SubstitutionImpl(mapVT));
            }
        }


        // cas (?x y ?z)  -> on utilise PSO
        if(Svariable && !Pvariable && Ovariable){

            for( Key key : PSO.searchByFirstOne(Pint) ){
                mapVT = new HashMap<>();
                mapVT.put((Variable) atom.getTripleSubject(), dictionaire_int_Term.get(key.part2));
                mapVT.put((Variable) atom.getTripleObject(), dictionaire_int_Term.get(key.part3));

                results.add(new SubstitutionImpl(mapVT));
            }
        }


        // cas (?x ?y o)  -> on utilise OSP
        if(Svariable && Pvariable && !Ovariable){
            for( Key key : OSP.searchByFirstOne(Oint) ){
                mapVT = new HashMap<>();
                mapVT.put((Variable) atom.getTripleSubject(), dictionaire_int_Term.get(key.part2));
                mapVT.put((Variable) atom.getTriplePredicate(), dictionaire_int_Term.get(key.part3));

                results.add(new SubstitutionImpl(mapVT));
            }
        }


        // cas (?x p o)  -> on utilise POS
        if(Svariable && !Pvariable && !Ovariable){
            for( Key key : POS.searchByFirstTwo(Pint, Oint) ){
                mapVT = new HashMap<>();
                mapVT.put((Variable) atom.getTripleSubject(), dictionaire_int_Term.get(key.part3));
                results.add(new SubstitutionImpl(mapVT));
            }
        }


        // cas (s ?y o)  -> on utilise SOP
        if(!Svariable && Pvariable && !Ovariable){
            for( Key key : SOP.searchByFirstTwo(Sint, Oint) ){
                mapVT = new HashMap<>();
                mapVT.put((Variable) atom.getTriplePredicate(), dictionaire_int_Term.get(key.part3));
                results.add(new SubstitutionImpl(mapVT));
            }
        }


        // cas (s p ?z)  -> on utilise SPO encore
        if(!Svariable && !Pvariable && Ovariable){

            for( Key key : SPO.searchByFirstTwo(Sint, Pint) ){
                mapVT = new HashMap<>();
                mapVT.put((Variable) atom.getTripleObject(), dictionaire_int_Term.get(key.part3));


                results.add(new SubstitutionImpl(mapVT));
            }

        }


        // cas (s p o)  -> on utilise OPS pour changer (j'imagine que ça répartit la charge d'utiliser les différents index)
        if(!Svariable && !Pvariable && !Ovariable){
            if (OPS.search(Oint, Pint, Sint))
                // on ajoute la substitution vide (veut dire qu'il y a eu une réponse, si pas de réponse on aurait pas mis de substitution du tout)
                results.add(new SubstitutionImpl(new HashMap<Variable, Term>()));
        }

        return results.iterator();

    }



    @Override
    public Iterator<Substitution> match(StarQuery q) {

        // idée du programme : on récupère TOUTES les subsitutions de chacun des RDFAtoms
        // On identifie l'atome qui a le moins de substitutions possibles.
        // Pour chacun de ses élements, on regarde s'il appartient à toutes les autres subsitutions (de tous les autres RDFAtoms)
        // Si c'est le cas, c'est bien une substitution valide

        // Le fait de prendre la plus petite subsitution c'est pour faire le moins de contains() possibles sur toutes les autres


        ArrayList<Substitution> result = new ArrayList<>();
        ArrayList<ArrayList<Substitution>> toutesLesSubstitutions = new ArrayList<>();
        int nbrSubstitutions = Integer.MAX_VALUE;

        // pour chaque atome de la requête
        for (RDFAtom rdfAtom: q.getRdfAtoms()){
            
            //on fait un match sur cet atome
            ArrayList<Substitution> substitutions = new ArrayList<>();
            this.match(rdfAtom).forEachRemaining(substitutions::add);
            toutesLesSubstitutions.add(substitutions);

            // on enregistre le match ayant fait le plus petit nombre de substitutions
            if (substitutions.size() < nbrSubstitutions) {
                nbrSubstitutions = substitutions.size();
                result = substitutions;
            }
        }

        toutesLesSubstitutions.remove(result);


        // Intersection dynamique à partir du plus petit ensemble 
        for (List<Substitution> substitutions : toutesLesSubstitutions) {
            result.retainAll(substitutions); // Réduit la liste principale par intersection
        }

        return result.iterator();
    }




    @Override
    public Collection<Atom> getAtoms() {
        List<Atom> atoms = new ArrayList<>();
        Term[] terms;

        for (Key key : OPS.getAllKeys()) {
            terms = new Term[3];
            terms[0] = dictionaire_int_Term.get(key.part3);
            terms[1] = dictionaire_int_Term.get(key.part2);
            terms[2] = dictionaire_int_Term.get(key.part1);

            atoms.add(new RDFAtom(terms));

        }
        System.out.println(atoms);
        return atoms;

    }
}

