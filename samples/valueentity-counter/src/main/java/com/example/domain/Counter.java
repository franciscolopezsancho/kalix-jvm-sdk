/* This code was initialised by Akka Serverless tooling.
 * As long as this file exists it will not be re-generated.
 * You are free to make changes to this file.
 */

package com.example.domain;

import com.example.CounterApi;
import com.google.protobuf.Empty;


// tag::class[]
/**
 * A Counter represented as a value entity.
 */
public class Counter extends AbstractCounter {
  
    @SuppressWarnings("unused")
    private final String entityId;

    public Counter(String entityId) { // <1>
        this.entityId = entityId;
    }
    // end::class[]
    
      @Override
      public CounterDomain.CounterState emptyState() {
        return CounterDomain.CounterState.getDefaultInstance();
      }
    
      // tag::increase[]
      @Override
      public Effect<Empty> increase(CounterDomain.CounterState currentState, CounterApi.IncreaseValue command) {
        if (command.getValue() < 0) { // <1>
          return effects().error("Increase requires a positive value. It was [" + command.getValue() + "].");
        }
        CounterDomain.CounterState newState =  // <2>
                currentState.toBuilder().setValue(currentState.getValue() + command.getValue()).build();
        return effects()
                .updateState(newState) // <3>
                .thenReply(Empty.getDefaultInstance());
    }
    // end::increase[]

    @Override
    public Effect<Empty> decrease(
            CounterDomain.CounterState currentState,
            CounterApi.DecreaseValue command) {
      if (command.getValue() < 0) {
        return effects().error("Decrease requires a positive value. It was [" + command.getValue() + "].");
      }
      CounterDomain.CounterState newState =
          currentState.toBuilder().setValue(currentState.getValue() - command.getValue()).build();
      return effects()
          .updateState(newState)
          .thenReply(Empty.getDefaultInstance());
    }
    
    @Override
    public Effect<Empty> reset(CounterDomain.CounterState currentState, CounterApi.ResetValue command) {
      CounterDomain.CounterState newState =
          currentState.toBuilder().setValue(0).build();
      return effects()
          .updateState(newState)
          .thenReply(Empty.getDefaultInstance());
    }
    
    // tag::getCurrentCounter[]
    @Override
    public Effect<CounterApi.CurrentCounter> getCurrentCounter(
            CounterDomain.CounterState currentState, // <1>
            CounterApi.GetCounter command) {
        CounterApi.CurrentCounter current =
                CounterApi.CurrentCounter.newBuilder().setValue(currentState.getValue()).build(); // <2>
        return effects().reply(current);
    }
    // end::getCurrentCounter[]
}