package org.example.domain

import com.akkaserverless.scalasdk.valueentity.ValueEntity
import com.google.protobuf.empty.Empty
import org.example.Components
import org.example.ComponentsImpl
import org.example.state.CounterState
import org.example.valueentity

// This code is managed by Akka Serverless tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

/** A value entity. */
abstract class AbstractCounter extends ValueEntity[CounterState] {

  def components: Components =
    new ComponentsImpl(commandContext())

  /** Command handler for "Increase". */
  def increase(currentState: CounterState, increaseValue: valueentity.IncreaseValue): ValueEntity.Effect[Empty]

  /** Command handler for "Decrease". */
  def decrease(currentState: CounterState, decreaseValue: valueentity.DecreaseValue): ValueEntity.Effect[Empty]

}
