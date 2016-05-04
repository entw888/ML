import org.kramerlab.teaching.ml.datasets.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

//TODO exceptions
/**
 * Created by David Klopp, Christian Stricker, Markus Vieth on 21.04.2016.
 */
public class DecisionTree {

    /**
     * Inner Node class
     */
    private class Node {
        Attribute attribute = null;
        List<Edge> edges = new ArrayList<>();
        List<Integer> indices;
        boolean isSingleNode = false;
        Value value;

        /**
         * Constructor
         * @param indices
         */
        public Node(List<Integer> indices) {
            this.indices = indices;
        }

        public void addEdge(Edge edge) {
            this.edges.add(edge);
        }

        public Value getValue() {
            if (isSingleNode)
                return value;
            return null;
        }
    }


    /**
     * Edge class
     */
    private class Edge {
        Value value = null;
        Node start;
        Node end;

        /**
         * Constructor
         * @param value
         */
        public Edge(Value value, Node start) {
            this.value = value;
            this.start = start;
            this.start.addEdge(this);
        }
    }




    private Node root = null;

    private Instance[] data;
    private Dataset dataset;
    private Attribute classAttribute;

    /**
     * default constructor
     */
    public DecisionTree() {
        
    }




    //--------------------------------------------------------------------------
    //-------------------------Train tree---------------------------------------
    //--------------------------------------------------------------------------

    /**
     *
     * @param node
     * @return
     */
    public Attribute selectAttribute(Node node) {
        Attribute select = null;
        double maxGain = -1.0;
        for (Attribute attribute : dataset.getAttributes()) {
            double gain = this.informationGain(attribute, node.indices);
            if (gain > maxGain) {
                select = attribute;
            }
        }

        return select;
    }


    /**
     *
     */
    public void train(List<Integer> trainset) {
        // create root Node
        this.root = new Node(trainset);
        this.train_recursive(this.root);
    }

    /**
     * Internal method
     * @param n
     */
    public void train_recursive(Node n) {
        // todo remove attributes to prevet duplicates

        // exit function
        if (this.isSingleNode(n)) {
            return;
        }
        //select attribute with biggest informationGain
        n.attribute = this.selectAttribute(n);

        // create edges for each value of the attribute
        NominalAttribute attr = (NominalAttribute)n.attribute;
        for (int i = 0; i<attr.getNumberOfValues(); i++) {
            Value v = attr.getValue(i);
            Edge edge = new Edge(v, n);

            List<Integer> childIndices = new ArrayList<>();
            Node child = new Node(childIndices);

            edge.end = child;
        }


        for (Integer idx : n.indices) {
            Instance i = this.data[idx];
            Value v = i.getValue(n.attribute);

            // add index to right edge
            for (Edge edge : n.edges) {
                if (edge.value.equals(v)) {
                    edge.end.indices.add(idx);
                    break;
                }
            }

        }

        // create tree
        for (Edge e : n.edges) {
            this.train_recursive(e.end);
        }
    }

    /**
     *
     * @param node
     * @return
     */
    private boolean isSingleNode(Node node) {

        if (node.indices.size() == 0) {
            //TODO default value
            return false;
        }

        Value value = data[node.indices.get(0)].getValue(classAttribute);

        for (int i = 1; i < node.indices.size(); i++) {
            Instance instance = data[node.indices.get(i)];
            if (! instance.getValue(classAttribute).equals(value)) {
                return false;
            }
        }

        node.isSingleNode = true;
        node.value = value;
        return true;
    }


    //--------------------------------------------------------------------------
    //-------------------------classify tree------------------------------------
    //--------------------------------------------------------------------------


    public double classify(List<Integer> data) {
        int correctlyClassified = 0;
        // repeat for each instance
        for (Integer i : data) {
            Instance instance = this.data[i];

            // iterate over tre
            Node currentNode = this.root;
            while (!currentNode.isSingleNode) {
                Attribute attr = currentNode.attribute;
                Value value = instance.getValue(attr);

                // find right edge
                for (Edge edge : currentNode.edges) {
                    if (edge.value.equals(value)) {
                        currentNode = edge.end;
                        break;
                    }
                }
            }

            // check if class attr is correct
            if ( currentNode.getValue().equals(instance.getValue(classAttribute)) ) {
                correctlyClassified ++;
            }

        }

        return (double)correctlyClassified/(double)data.size();
    }




    //--------------------------------------------------------------------------
    //-------------------------Constructor--------------------------------------
    //--------------------------------------------------------------------------

    /**
     * loads arff
     * @param path path to arff
     */
    public DecisionTree(String path) {
        try {
            this.loadArff(path);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * loads arff
     * @param file arff file
     */
    public DecisionTree(File file) {
        try {
            this.loadArff(file);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * loads arff
     * @param path path to arff
     * @throws Exception see kramerlabs dataset
     */
    public void loadArff(String path) throws Exception {
        File file = new File(path);
        this.loadArff(file);
    }

    /**
     * loads arff
     * @param file arff file
     * @throws Exception see kramerlabs dataset
     */
    public void loadArff(File file) throws Exception {
        this.dataset = new Dataset();
        dataset.load(file);
        this.data = new Instance[dataset.getNumberOfInstances()];

        for (int i = 0; i < data.length; i++) {
            this.data[i] = dataset.getInstance(i);
        }
        this.classAttribute = this.dataset.getAttributes().get(this.dataset
                .getClassIndex());
    }


    // Implement a method informationGain that takes two arguments: attribute A
    // and a list of indices i 1 , i 2 , i m .
    /**
     * calculates information gain for given attribute and instances
     * @param attribute given attribute
     * @param indices given indices of instances
     * @return information gain
     */
    public double informationGain(Attribute attribute, List<Integer> indices) {
        List<Attribute> attributes = this.dataset.getAttributes();
        Attribute classAttr = attributes.get(this.dataset.getClassIndex());

        //TODO throw Exception
        // Check if nominal
        if (!attribute.isNominal()) {
            System.err.println(attribute.getName() + "is not nominal");
            return Double.NaN;
        } else if (!classAttr.isNominal()) {
            System.err.println(classAttr.getName() + "is not nominal");
            return Double.NaN;
        }

        NominalAttribute attr = (NominalAttribute) attribute;
        // Init gain
        double gain = calculateEntropy((NominalAttribute)classAttr,
                indices);

        // sum over all values in attribute
        for (int v = 0; v < attr.getNumberOfValues(); v++) {
            List<Integer> subIndices = new ArrayList<>();
            // Alternative we could copy data in a list and remove already
            // picked instances to improve runtime
            // creates subset with indices of instances with value v
            for (int i : indices) {
                Instance instance = data[i];
                NominalValue value = (NominalValue)instance.getValue(attr);
                if (attr.getValue(v).equals(value)) {
                    subIndices.add(i);
                }
            }

            // calculates entropy
            double entropy = calculateEntropy((NominalAttribute)classAttr,
                    subIndices);

            // see formula
            gain -= entropy * ((double)subIndices.size())
                    /((double)indices.size());
        }

        return gain;
    }


    /**
     * calculates entropy
     * @param classAttr given class attribute
     * @param indices indices of instances
     * @return entropy
     */
    private double calculateEntropy(NominalAttribute classAttr, List<Integer>
            indices
    ) {
        int[] values = new int[classAttr.getNumberOfValues()];
        // calculates number of instances with value v in class attribute
        for (int v = 0; v < classAttr.getNumberOfValues(); v++) {
            values[v] = 0;
            for(int i : indices) {
                Instance instance = data[i];
                NominalValue value = (NominalValue)instance.getValue(classAttr);
                NominalValue classValue = classAttr.getValue(v);
                if (classValue.equals(value)) {
                    values[v]++;
                }
            }
        }
        return calculateEntropy(values);
    }

    /**
     * calculates entropy
     * @param values given values
     * @return entropy
     */
    private double calculateEntropy(int[] values) {
        double sum = 0;
        for (int i : values) {
            sum += i;
        }
        double entropy = 0.0;

        for (int value : values) {
            double p = value/sum;
            entropy -= p * log2(p);
        }

        return entropy;
    }


    /**
     * calculates log to base 2
     * @param a given parameter
     * @return log2(a) or 0 if a == 0
     */
    private double log2(double a) {
        if ( Double.compare(0.0, Math.abs(a)) == 0 )
            return 0;
        return Math.log(a) / Math.log(2);
    }



    //--------------------------------------------------------------------------
    //-------------------------Train and Testset--------------------------------
    //--------------------------------------------------------------------------


    /**
     * @return 2/3 trainset
     */
    public List<Integer> getTrainset() {
        // get List with all indices
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < this.data.length; i++) {
            indices.add(i);
        }

        // get 2/3 of the data
        List<Integer> trainIndices = new ArrayList<Integer>();

        // get random indices
        int size = indices.size() * 2/3;
        while (trainIndices.size() < size) {
            // add random element to trainIndices
            Random random = new Random();
            Integer randomIdx = random.nextInt(indices.size());
            trainIndices.add(indices.get(randomIdx));
            // remove Element from all indices
            indices.remove(randomIdx);
        }

        return trainIndices;
    }


    public List<Integer> getInverseSet(List<Integer> originalSet) {
        List<Integer> inverseSet = new ArrayList<>();
        for (int i=0; i<this.data.length; i++) {
            if (!originalSet.contains(i))
                inverseSet.add(i);
        }
        return inverseSet;
    }





    //--------------------------------------------------------------------------
    //-------------------------test---------------------------------------------
    //--------------------------------------------------------------------------



    /**
     * prints some test data
     */
    private void testPrint() {
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < data.length; i++) {
            indices.add(i);
        }

        /*for (Attribute attr : this.dataset.getAttributes()) {
            System.out.print("Attribute " + attr.getName());
            System.out.print(" has an InformationGain of " + informationGain
                    (attr, indices));
            System.out.println();
        }*/

        List<Integer> trainset = this.getTrainset();
        this.train(trainset);
        System.out.println(this.classify(this.getInverseSet(trainset)));
    }

    /**
     * a test
     * @param args none
     */
    public static void main(String[] args) {
        DecisionTree dt = new DecisionTree("res/weather.nominal.arff");
        dt.testPrint();
    }
}
