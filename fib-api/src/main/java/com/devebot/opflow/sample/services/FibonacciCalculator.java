package com.devebot.opflow.sample.services;

import com.devebot.opflow.sample.models.FibonacciInputItem;
import com.devebot.opflow.sample.models.FibonacciInputList;
import com.devebot.opflow.sample.models.FibonacciOutputItem;
import com.devebot.opflow.sample.models.FibonacciOutputList;
import java.util.List;

/**
 *
 * @author drupalex
 */
public interface FibonacciCalculator {
    FibonacciOutputItem calc(int number);
    FibonacciOutputItem calc(FibonacciInputItem data);
    FibonacciOutputList calc(FibonacciInputList list);
    FibonacciOutputList calc(List<FibonacciInputItem> list);
}
