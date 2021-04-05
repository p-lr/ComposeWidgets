# StartStopButton

A Jetpack compose implementation of a button which has two states (started, and stopped).
It animates when transitioning between states. If a click happens in the middle of a transition, the
state holder (typically a view-model) decides whether the state changes or not.

This button is useful when a component can be started or stopped, but click events are
debounced to avoid starting and stopping at a too high pace.

This button carries two information:

* The state started/stopped,
* The state transition, during which the button cannot change of state.

<p align="center">
<img src="https://user-images.githubusercontent.com/15638794/113584091-c605e500-962a-11eb-9e28-7a7229479c7a.gif">
</p>