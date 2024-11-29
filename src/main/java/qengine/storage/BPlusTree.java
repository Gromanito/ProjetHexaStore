package qengine.storage;





// code (bien évidemment) généré par chat GPT (c'est juste pour avoir une implémentation d'un B+Tree rapidement et pouvoir consacrer le travail sur le dictionnaire et les index)
// mais j'ai quand même dû modifié un peu pour que ça s'adpapte au projet


import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;






// Classe pour représenter la clé sous forme de triplet d'entiers

class Key implements Comparable<Key> {
    int part1, part2, part3;

    public Key(int part1, int part2, int part3) {
        this.part1 = part1;
        this.part2 = part2;
        this.part3 = part3;
    }

    public void setPart1(int part1) {
        this.part1 = part1;
    }

    public void setPart2(int part2) {
        this.part2 = part2;
    }

    public void setPart3(int part3) {
        this.part3 = part3;
    }

    @Override
    public int compareTo(Key other) {
        if (this.part1 != other.part1) return Integer.compare(this.part1, other.part1);
        if (this.part2 != other.part2) return Integer.compare(this.part2, other.part2);
        return Integer.compare(this.part3, other.part3);
    }

    @Override
    public String toString() {
        return "(" + part1 + ", " + part2 + ", " + part3 + ")";
    }
}












// Classe abstraite de base pour les nœuds
abstract class Node {
    List<Key> keys;

    Node() {
        this.keys = new ArrayList<>();
    }

    abstract boolean isLeaf();
}






// Classe pour les nœuds internes
class InternalNode extends Node {
    List<Node> children;

    InternalNode() {
        super();
        this.children = new ArrayList<>();
    }

    @Override
    boolean isLeaf() {
        return false;
    }
}





// Classe pour les nœuds feuilles
class LeafNode extends Node {

    LeafNode next;

    LeafNode() {
        super();
        this.next = null;
    }

    @Override
    boolean isLeaf() {
        return true;
    }
}










// Classe pour le B+Tree
public class BPlusTree {
    private final int degree;
    private Node root;
    private int size = 0;

    public BPlusTree(int degree) {
        this.degree = degree;
        this.root = new LeafNode();
    }




    // Méthode pour vérifier si un triplet dans le B+ tree
    public boolean search(Key key) {
        LeafNode leaf = findLeafNode(key);
        for (int i = 0; i < leaf.keys.size(); i++) {
            if (leaf.keys.get(i).compareTo(key) == 0) {
                return true;
            }
        }
        return false;
    }

    public boolean search(int one, int two, int three){
        return search(new Key(one, two, three));
    }



    // Recherche tous les triplets ayant (one, x, y)
    public List<Key> searchByFirstOne(int one) {
        List<Key> result = new LinkedList<>();
        LeafNode leaf = findLeafNode(new Key(one, Integer.MIN_VALUE, Integer.MIN_VALUE));

        while (leaf != null) {
            for (Key key : leaf.keys) {
                if (key.part1 == one) {
                    result.add(key);
                } else if (key.part1 > one) {
                    // On arrête dès que part1 dépasse la valeur recherchée
                    return result;
                }
            }
            leaf = leaf.next; // Passer au nœud feuille suivant
        }
        return result;
    }

    // Recherche tous les triplets ayant (one, two, y)
    public List<Key> searchByFirstTwo(int one, int two) {
        List<Key> result = new LinkedList<>();
        LeafNode leaf = findLeafNode(new Key(one, two, Integer.MIN_VALUE));

        while (leaf != null) {
            for (Key key : leaf.keys) {
                if (key.part1 == one && key.part2 == two) {
                    result.add(key);
                } else if (key.part1 > one || (key.part1 == one && key.part2 > two)) {
                    // On arrête dès que part1 > one ou part1 == one et part2 > two
                    return result;
                }
            }
            leaf = leaf.next; // Passer au nœud feuille suivant
        }
        return result;
    }



    public List<Key> getAllKeys() {
        List<Key> result = new LinkedList<>();
        Node current =  root;

        // Si la racine est un nœud interne, on descend jusqu'aux nœuds feuilles
        while (current != null && !(current instanceof LeafNode)) {
            // on peut faire ce cast car on vient de vérifier dans le while si c'était pas un noeud de type feuille

            InternalNode internalNode = (InternalNode) current;

            current = internalNode.children.getFirst();  // Descente vers le nœud feuille
        }

        // on peut dire que current est une feuille prck on est sorti du while (le code est AFFREUX) (j'ai modifié du chat gpt mais bon ça reste horrible)
        LeafNode firstLeaf = (LeafNode) current;

        // Parcours des nœuds feuilles pour récupérer toutes les clés
        while (current != null) {
            result.addAll(current.keys);  // Ajouter les clés du nœud feuille courant
            current = firstLeaf.next;  // Passer au nœud feuille suivant
        }

        return result;
    }




    public void insert(Key key) {
        LeafNode leaf = findLeafNode(key);
        int pos = Collections.binarySearch(leaf.keys, key);
        if (pos >= 0) {
            // La clé existe déjà, on ne l'ajoute pas (ou on peut gérer les doublons ici si nécessaire)
            return;
        } else {
            pos = -pos - 1;  // Position où insérer
            leaf.keys.add(pos, key);
        }

        // Vérifier si le nœud doit être scindé
        if (leaf.keys.size() > degree) {
            splitLeafNode(leaf);
        }
        this.size++;
    }



    private LeafNode findLeafNode(Key key) {
        Node current = root;
        while (!current.isLeaf()) {
            InternalNode internal = (InternalNode) current;
            int i = 0;
            while (i < internal.keys.size() && key.compareTo(internal.keys.get(i)) >= 0) {
                i++;
            }
            current = internal.children.get(i);
        }
        return (LeafNode) current;
    }



    private void splitLeafNode(LeafNode leaf) {
        LeafNode newLeaf = new LeafNode();
        int mid = degree / 2;

        // Copier la seconde moitié des clés dans le nouveau nœud feuille
        newLeaf.keys.addAll(leaf.keys.subList(mid, leaf.keys.size()));

        // Supprimer la seconde moitié des clés du nœud original
        leaf.keys.subList(mid, leaf.keys.size()).clear();

        // Chaînage des nœuds feuilles
        newLeaf.next = leaf.next;
        leaf.next = newLeaf;

        // Si le nœud scindé est la racine, on crée un nouveau nœud racine
        if (leaf == root) {
            InternalNode newRoot = new InternalNode();
            newRoot.keys.add(newLeaf.keys.get(0));  // Ajout de la première clé du nouveau nœud pour le séparer
            newRoot.children.add(leaf);
            newRoot.children.add(newLeaf);
            root = newRoot;
        } else {
            // Insérer dans le parent
            insertIntoParent(leaf, newLeaf.keys.get(0), newLeaf);
        }
    }

    private void insertIntoParent(Node left, Key key, Node right) {
        InternalNode parent = findParent(root, left);
        int pos = Collections.binarySearch(parent.keys, key);
        pos = pos >= 0 ? pos + 1 : -pos - 1;
        parent.keys.add(pos, key);
        parent.children.add(pos + 1, right);

        if (parent.keys.size() > degree) {
            splitInternalNode(parent);
        }
    }

    private InternalNode findParent(Node current, Node child) {
        if (current.isLeaf()) return null;
        InternalNode parent = (InternalNode) current;
        for (Node node : parent.children) {
            if (node == child) return parent;
            InternalNode found = findParent(node, child);
            if (found != null) return found;
        }
        return null;
    }

    private void splitInternalNode(InternalNode internal) {
        InternalNode newInternal = new InternalNode();
        int mid = degree / 2;

        newInternal.keys.addAll(internal.keys.subList(mid + 1, internal.keys.size()));
        newInternal.children.addAll(internal.children.subList(mid + 1, internal.children.size()));

        Key upKey = internal.keys.get(mid);
        internal.keys.subList(mid, internal.keys.size()).clear();
        internal.children.subList(mid + 1, internal.children.size()).clear();

        if (internal == root) {
            InternalNode newRoot = new InternalNode();
            newRoot.keys.add(upKey);
            newRoot.children.add(internal);
            newRoot.children.add(newInternal);
            root = newRoot;
        } else {
            insertIntoParent(internal, upKey, newInternal);
        }
    }

    public int getSize() {
        return size;
    }


}
