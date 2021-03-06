import weka.core.*;
import java.util.Random;
import weka.classifiers.Classifier;
import weka.classifiers.trees.J48;
import weka.core.Capabilities;

/**
 * Created by David on 24.05.16.
 */
public class OptimalJ48 extends Classifier {
    protected CVParameterSelection selection;
    private String[] OPTIONS = {"C 0.1 0.5 10"};

    public OptimalJ48() throws Exception {
        J48 temp = new J48();
        this.selection = new CVParameterSelection();
        selection.setCVParameters(OPTIONS);
        //selection.setNumberOfFolds(15);
        selection.setClassifier(temp);
    }

    /**
     * Generates a classifier. Must initialize all fields of the classifier that
     * are not being set via options (ie. multiple calls of buildClassifier must
     * always lead to the same result). Must not change the dataset in any way.
     *
     * @param data
     *         set of instances serving as training data
     *
     * @throws Exception
     *         if the classifier has not been generated successfully
     */
    @Override
    public void buildClassifier(Instances data) throws Exception {
        this.selection.buildClassifier(data);
    }

    /**
     * Classifies the given test instance. The instance has to belong to a
     * dataset when it's being classified. Note that a classifier MUST implement
     * either this or distributionForInstance().
     *
     * @param instance
     *         the instance to be classified
     *
     * @return the predicted most likely class for the instance or
     * Utils.missingValue() if no prediction is made
     *
     * @throws Exception
     *         if an error occurred during the prediction
     */
    @Override
    public double classifyInstance(Instance instance) throws Exception {
        return this.selection.classifyInstance(instance);
    }

    /**
     * Predicts the class memberships for a given instance. If an instance is
     * unclassified, the returned array elements must be all zero. If the class
     * is numeric, the array must consist of only one element, which contains
     * the predicted value. Note that a classifier MUST implement either this or
     * classifyInstance().
     *
     * @param instance
     *         the instance to be classified
     *
     * @return an array containing the estimated membership probabilities of the
     * test instance in each class or the numeric prediction
     *
     * @throws Exception
     *         if distribution could not be computed successfully
     */

    @Override
    public double[] distributionForInstance(Instance instance) throws Exception {
        return this.selection.distributionForInstance(instance);
    }

    /**
     * Returns the Capabilities of this classifier. Maximally permissive
     * capabilities are allowed by default. Derived classifiers should override
     * this method and first disable all capabilities and then enable just those
     * capabilities that make sense for the scheme.
     *
     * @return the capabilities of this object
     *
     * @see Capabilities
     */
    @Override
    public Capabilities getCapabilities() {
        return this.selection.getCapabilities();
    }


    // e)
    
    /**
     * User pieces=3 for trainset, validationset and testset
     * @param pieces number of pieces to split the data
     * @return Array containing number of pieces Instances
     */
    public Instances[] splitDataset(int pieces, Instances data) {
        // size of dataset
        int size = data.numInstances();
        int piece_size = size/pieces;

        // randomize the data
        data.randomize(new Random());

        // save trainings, validation and test set
        Instances[] datasets = new Instances[pieces];

        // split the data
        for (int i = 0; i<pieces; i++) {
            int lowerBound =  (int)(Math.ceil(piece_size*i));
            int upperBound =  (int)(Math.ceil(piece_size*(i+1)));
            datasets[i] = new Instances(data, lowerBound, upperBound);
        }

        return datasets;
    }
}
