package com.devebot.opflow.sample.bdd.steps;

import com.devebot.opflow.OpflowCommander;
import com.devebot.opflow.OpflowBuilder;
import com.devebot.opflow.exception.OpflowBootstrapException;
import com.devebot.opflow.sample.services.FibonacciCalculator;
import com.devebot.opflow.sample.models.FibonacciInputItem;
import com.devebot.opflow.sample.models.FibonacciInputList;
import com.devebot.opflow.sample.models.FibonacciOutputItem;
import com.devebot.opflow.sample.models.FibonacciOutputList;
import com.devebot.opflow.supports.OpflowJsonTool;
import com.devebot.opflow.supports.OpflowStringUtil;
import java.util.HashMap;
import java.util.Map;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.jbehave.core.annotations.BeforeScenario;
import org.jbehave.core.annotations.Given;
import org.jbehave.core.annotations.Named;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author drupalex
 */
public class FibonacciMasterSteps {
    private final static Logger LOG = LoggerFactory.getLogger(FibonacciMasterSteps.class);
    private final Map<String, OpflowCommander> commanders = new HashMap<>();
    
    private final Map<String, Map<String, Object>> context = new HashMap<>();
    protected Map<String, Object> getContext(String contextName) {
        if (!context.containsKey(contextName)) {
            context.put(contextName, new HashMap<String, Object>());
        }
        return context.get(contextName);
    }
    
    @BeforeScenario
    public void beforeEachScenario() {
        commanders.clear();
    }
    
    @Given("a Commander named '$commanderName' with default properties file")
    public void createCommander(@Named("commanderName") final String commanderName) throws OpflowBootstrapException {
        commanders.put(commanderName, OpflowBuilder.createCommander("commander.properties"));
        if (LOG.isDebugEnabled()) LOG.debug("Commander[" + commanderName + "] has been created");
    }
    
    @Given("a Commander named '$commanderName' with properties file: '$propFile'")
    public void createCommander(@Named("commanderName") final String commanderName, 
            @Named("propFile") final String propFile) throws OpflowBootstrapException {
        commanders.put(commanderName, OpflowBuilder.createCommander(propFile));
        if (LOG.isDebugEnabled()) LOG.debug("Commander[" + commanderName + "] / [" + propFile + "] has been created");
    }
    
    @Given("a registered FibonacciCalculator interface in Commander named '$commanderName'")
    public void instantiateFibonacciCalculator(@Named("commanderName") final String commanderName)
            throws OpflowBootstrapException {
        commanders.get(commanderName).registerTypeWithDefault(FibonacciCalculator.class);
    }
    
    @When("I send a request to Commander '$commanderName' to calculate fibonacci of '$number'")
    public void callCommanderCalc(@Named("commanderName") final String commanderName,
            @Named("number") final int number) throws OpflowBootstrapException {
        FibonacciCalculator calculator = commanders.get(commanderName).registerTypeWithDefault(FibonacciCalculator.class);
        FibonacciOutputItem result = calculator.calc(new FibonacciInputItem(number));
        if (LOG.isDebugEnabled()) LOG.debug("FibonacciOutput: " + OpflowJsonTool.toString(result));
        getContext(commanderName).put("output", result);
    }
    
    @When("I send a request to Commander '$commanderName' to calculate fibonacci of a list of '$numbers'")
    public void callCommanderCalc(@Named("commanderName") final String commanderName,
            @Named("numbers") final String numbers) throws OpflowBootstrapException {
        FibonacciCalculator calculator = commanders.get(commanderName).registerTypeWithDefault(FibonacciCalculator.class);
        FibonacciInputList list = new FibonacciInputList();
        Integer[] integers = OpflowStringUtil.splitByComma(numbers, Integer.class);
        for(Integer integer: integers) {
            list.add(new FibonacciInputItem(integer));
        }
        FibonacciOutputList results = calculator.calc(list);
        if (LOG.isDebugEnabled()) LOG.debug("FibonacciOutputList: " + OpflowJsonTool.toString(results));
        getContext(commanderName).put("outputList", results);
    }
    
    @Then("the fibonacci value of '$number' by Commander '$commanderName' must be '$result'")
    public void verifyCommanderCalcMethod(@Named("commanderName") final String commanderName,
            @Named("number") final int number,
            @Named("result") final int result) throws OpflowBootstrapException {
        FibonacciOutputItem singleResult = (FibonacciOutputItem) getContext(commanderName).get("output");
        MatcherAssert.assertThat(singleResult.getNumber(), Matchers.equalTo(number));
        MatcherAssert.assertThat((int)singleResult.getValue(), Matchers.equalTo(result));
    }
    
    @When("I close Commander named '$commanderName'")
    public void closeCommander(@Named("commanderName") String commanderName) {
        commanders.get(commanderName).close();
    }
}
