package org.icij.datashare.text.nlp.ixapipe;

import org.junit.Before;
import org.junit.Test;

import java.util.Properties;

import static org.fest.assertions.Assertions.assertThat;
import static org.icij.datashare.text.Language.ITALIAN;
import static org.icij.datashare.text.nlp.Pipeline.Property.STAGES;

public class IxapipePipelineTest {
    private IxapipePipeline ixapipePipeline;
    @Before
    public void setUp() throws Exception {
        Properties props = new Properties();
        props.setProperty(STAGES.getName(), "POS,NER");
        ixapipePipeline = new IxapipePipeline(props);
    }

    @Test
    public void test_initialize() {
        ixapipePipeline.initialize(ITALIAN);

        assertThat(IxaPosModels.getInstance().isLoaded(ITALIAN)).isEqualTo(true);
        assertThat(IxaNerModels.getInstance().isLoaded(ITALIAN)).isEqualTo(true);
    }
}