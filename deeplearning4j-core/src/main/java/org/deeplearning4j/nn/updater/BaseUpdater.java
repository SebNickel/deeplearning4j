package org.deeplearning4j.nn.updater;

import org.deeplearning4j.nn.api.Layer;
import org.deeplearning4j.nn.api.Updater;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.gradient.Gradient;
import org.deeplearning4j.nn.params.DefaultParamInitializer;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.learning.GradientUpdater;
import org.nd4j.linalg.ops.transforms.Transforms;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Adam Gibson
 */
public abstract class BaseUpdater implements Updater {
    protected Map<String,GradientUpdater> updaterForVariable = new HashMap<>();


    @Override
    public void update(Layer layer, Gradient gradient,int iteration) {
        Map<String,INDArray> newGradients = new HashMap<>();
        for(Map.Entry<String,INDArray> gradientPair : gradient.gradientForVariable().entrySet()) {
            GradientUpdater updater = init(gradientPair.getKey(),gradientPair.getValue(),layer);
            INDArray gradient2 = updater.getGradient(gradientPair.getValue(), iteration);
            postApply(layer,gradient2,gradientPair.getKey());
            newGradients.put(gradientPair.getKey(),gradient2);
        }

        //apply the updates
        for(Map.Entry<String,INDArray> gradientPair : newGradients.entrySet()) {
            gradient.setGradientFor(gradientPair.getKey(),gradientPair.getValue());
        }

    }

    /**
     * Apply the regularization
     * @param layer
     * @param gradient
     * @param param
     */
    public void postApply(Layer layer,INDArray gradient,String param) {
        NeuralNetConfiguration conf = layer.conf();
        INDArray params = layer.getParam(param);
        if(conf.isUseRegularization() && conf.getL2() > 0 && !(param.equals(DefaultParamInitializer.BIAS_KEY)))
            gradient.subi(params.mul(conf.getL2()));
        if(conf.isUseRegularization() && conf.getL1() < 0 && !(param.equals(DefaultParamInitializer.BIAS_KEY)))
            gradient.subi(Transforms.sign(params).muli(conf.getL1()));


        if(conf.isConstrainGradientToUnitNorm())
            gradient.divi(gradient.norm2(Integer.MAX_VALUE));

        gradient.divi((double) layer.input().size(0));
    }

    public abstract void init();

    public abstract GradientUpdater init(String variable, INDArray gradient, Layer layer);

}
