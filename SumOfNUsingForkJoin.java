/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ocp.exam.concurrency;

import java.util.concurrent.RecursiveTask;
import java.util.concurrent.ForkJoinPool;

/**
 * This class illustrates how we can compute sum of 1 to N numbers using fork/join framework.
 * The range of numbers are divided into half until the range can be handled by a thread.
 * Once the range summation completes, the result gets summed up together.
 */

class SumOfNUsingForkJoin {

    private static final long N = 1000_000; //the target to sum, usually be the program parameter

    public static void main(String[] args) {

        //Make a fork-join pool
        ForkJoinPool pool = new ForkJoinPool();

        //Submit the computation task to the fork-join pool
        long computedSum = pool.invoke(new SumFromTo(0, N, N / pool.getParallelism()));

        //This is the formula sum for the range 1..N
        long formulaSum = (N * (N + 1)) / 2;

        //Compare the computed sum and the formula sum
        System.out.printf("Sum for range 1..%d; computed sum = %d, formula sum = %d%n", N, computedSum, formulaSum);
    }

    /**
     * This is the recursive implementation of the algorithm; inherit from RecursiveTask instead of RecursiveAction
     * since we're returning values.
     */
    static class SumFromTo extends RecursiveTask<Long> {

        private static final int MIN_THRESHOLD = 1000;//the minimum big enough threshold to avoid inefficient small subtasks.

        long from;
        long to;//inclusive

        /**
         * The limit to stop dividing tasks and calculate the result. It should be at least {@link #MIN_THRESHOLD}.
         */
        long threshold;

        public SumFromTo(long from, long to, long threshold) {
            if (from >= to)
                throw new IllegalArgumentException(
                        String.format("The from value %d must be strictly less than the to value %d", from, to));

            if (threshold <= MIN_THRESHOLD)
                throw new IllegalArgumentException(String.format("Please provide a big enough threshold to avoid " +
                        "inefficient small subtasks. The minimum is %d", MIN_THRESHOLD));

            this.from = from;
            this.to = to;
            this.threshold = threshold;
        }

        /**
         * The method performs task dividing if necessary then fork and join the tasks.
         * The task dividing happens only when the range is too big for a single thread. Otherwise, it calculates
         * the results sequentially.
         */
        @Override
        public Long compute() {

            if ((to - from) <= threshold) {//the range is something that can be handled by a thread, so do summation

                long localSum = 0;
                for (long i = from; i <= to; i++) {
                    localSum += i;
                }
                System.out.printf("%-10sSum of value range %d to %d is %d%n", "", from, to, localSum);
                return localSum;

            } else {//the range is too big for a thread to handle, so fork the computation

                long mid = (from + to) / 2;
                System.out.printf("Forking computation into two ranges: %d to %d and %d to %d %n", from, mid, mid + 1, to);
                SumFromTo firstHalf = new SumFromTo(from, mid, threshold);
                firstHalf.fork();

                SumFromTo secondHalf = new SumFromTo(mid + 1, to, threshold);
                long resultSecond = secondHalf.compute();//or secondHalf.invoke

                // now, wait for the first half of computing sum to complete, once done, add it to the remaining part
                return firstHalf.join() + resultSecond;
            }
        }
    }
}
