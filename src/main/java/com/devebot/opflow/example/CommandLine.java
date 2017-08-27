package com.devebot.opflow.example;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.devebot.opflow.OpflowRpcResult;
import com.devebot.opflow.OpflowTask;
import com.devebot.opflow.OpflowUtil;
import com.devebot.opflow.exception.OpflowBootstrapException;
import com.devebot.opflow.exception.OpflowConnectionException;
import com.devebot.opflow.exception.OpflowOperationException;
import java.util.Queue;

/**
 *
 * @author drupalex
 */
public class CommandLine {
    
    public static void main(String ... argv) {
        CommandLine main = new CommandLine();
        JCommander.newBuilder().addObject(main).build().parse(argv);
        main.dispatch();
    }
    
    @Parameter(names={"--program", "-p"}, description = "Program mode (client/server)")
    String mode;
    
    @Parameter(names={"--action", "-a"}, description = "Client action: request/random/publish")
    String action;
    
    @Parameter(names={"--number", "-n"})
    int number = 0;
    
    @Parameter(names={"--number-max", "-m"})
    int numberMax = 40;
    
    @Parameter(names={"--total", "-t"})
    int total = 0;
    
    @Parameter(names={"--range", "-r"})
    String range = null;
    
    @Parameter(names={"--json", "-j"})
    String json;

    public void dispatch() {
        FibonacciRpcMaster master = null;
        FibonacciRpcWorker worker = null;
        FibonacciPubsubHandler sender = null;
        FibonacciPubsubHandler pubsub = null;
        try {
            if ("server".equals(mode)) {
                final FibonacciData.Setting setting = new FibonacciData.Setting();

                System.out.println("[+] start Fibonacci RPC Worker ...");
                worker = new FibonacciRpcWorker(setting);
                worker.process();

                System.out.println("[-] start Fibonacci Setting Subscriber ...");
                pubsub = new FibonacciPubsubHandler(setting);
                pubsub.subscribe();

                System.out.println("[*] Waiting for message. To exit press CTRL+C");
            } else {
                if("request".equals(action)) {
                    System.out.println("[+] request Fibonacci(" + number + ")");
                    master = new FibonacciRpcMaster();
                    printResult(number, OpflowUtil.exhaustRequest(master.request(number)));
                    master.close();
                } else if ("random".equals(action)) {
                    if (total <= 0) total = 10;
                    int[] rangeInt = parseRange(range);
                    if (rangeInt.length != 2) {
                        rangeInt = new int[] {20, 40};
                    } else {
                        if (rangeInt[0] < 10) rangeInt[0] = 10;
                        if (50 < rangeInt[1] || rangeInt[1] <= rangeInt[0]) rangeInt[1] = 50;
                    }
                    
                    System.out.println("[+] calculate Fibonacci() of " + total + " random number in range [" +
                            rangeInt[0] + ", " + rangeInt[1] + "].");
                    OpflowTask.Countdown countdown = new OpflowTask.Countdown(total);
                    master = new FibonacciRpcMaster();
                    Queue<FibonacciData.Pair> queue = master.random(total, rangeInt[0], rangeInt[1]);
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ie) {}
                    while(!queue.isEmpty()) {
                        FibonacciData.Pair req = queue.poll();
                        printResult(req.getNumber(), OpflowUtil.exhaustRequest(req.getSession()));
                        countdown.check();
                    }
                    countdown.bingo();
                    System.out.println("[-] random command has been finished");
                    master.close();
                } else {
                    sender = new FibonacciPubsubHandler();
                    sender.publish(numberMax);
                    sender.close();
                    System.out.println("[-] numberMax has been sent");
                }
            }
        } catch (Exception exception) {
            System.out.println("[+] Error:");
            if (exception instanceof OpflowConnectionException) {
                printErrors(new String[] {
                    "[-] Connect to RabbitMQ has been failed. Try again with log4j TRACE mode.",
                    "[-] Verify the Connection Parameter/{URI, host,...} on console or logfile.",
                });
            } else if (exception instanceof OpflowBootstrapException) {
                printErrors(new String[] {
                    "[-] Initialize broker has been failed.",
                });
            } else if (exception instanceof OpflowOperationException) {
                printErrors(new String[] {
                    "[-] Invoke produce()/consume() has been failed.",
                });
            }
            printErrors(new String[] {
                "[+] Exception details:",
                "[-] class: " + exception.getClass().getName(),
                "[-] message: " + exception.getMessage()
            });
            // close all of connections
            if ("server".equals(mode)) {
                if (worker != null) worker.close();
                if (pubsub != null) pubsub.close();
            } else {
                if (master != null) master.close();
                if (sender != null) sender.close();
            }
        }
    }
    
    private void printResult(int number, OpflowRpcResult result) {
        System.out.println("[-] ConsumerID: " + result.getWorkerTag());
        for(OpflowRpcResult.Step step: result.getProgress()) {
            System.out.println("[-] Fibonacci(" + number + ") percent: " + step.getPercent());
        }
        if (result.isTimeout()) {
            System.out.println("[-] Fibonacci(" + number + ") is timeout.");
        }
        if (result.isFailed()) {
            System.out.println("[-] Fibonacci(" + number + ") is failed: " + result.getErrorAsString());
        }
        if (result.isCompleted()) {
            System.out.println("[-] Fibonacci(" + number + ") -> " + result.getValueAsString());
        }
        System.out.println();
    }
    
    private void printErrors(String[] msgs) {
        for(String msg: msgs) {
            System.out.println(msg);
        }
    }
    
    private int[] parseRange(String rangeStr) {
        String[] rangeArr = OpflowUtil.splitByComma(rangeStr);
        int[] rangeInt = new int[rangeArr.length];
        for(int i=0; i<rangeInt.length; i++) {
            try {
                rangeInt[i] = Integer.parseInt(rangeArr[i]);
            } catch (NumberFormatException nfe) {}
        }
        return rangeInt;
    }
}
