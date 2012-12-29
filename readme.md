Scala FRP
=========

###Background

Scala FRP (stands for **F**unctional **R**eactive **P**rogramming) is a library inspired by [Ingo Maier's](http://lampwww.epfl.ch/~imaier/) paper, [Deprecating the Observer Pattern](http://lampwww.epfl.ch/~imaier/pub/DeprecatingObserversTR2010.pdf). Ingo Maier made an implementation of his "scala.react" framework which is available [on Github](https://github.com/ingoem/scala-react) in its original form. I also made a version of the same library that works with SBT to manage its dependencies, available [here](https://github.com/dylemma/scala.react).

###What it Does

Functional Reactive Programming is an alternative approach to the Publisher/Subscriber Pattern. It replaces the idea of a `Publisher` with an `EventStream`, and replaces the idea of a `Subscriber` with the fact that `EventStream`s can be composed and observed in a functional style.

###Getting Started

Currently the only way for you to use this library is to build it from source. I'll eventually start publishing things on a Maven repository. 

Once you've got some binary version of Scala FRP, the bare minimum you should do is

	import scala.frp._
	implicit object observer extends Observer

There are two main classes that you will want to interact with. `EventStream` is a read-only class that you can attach event handlers to, and create mappings and combinations with. `EventSource` is a subclass of `EventStream` that also exposes `fire` and `stop` methods.

The implicit `Observer` is needed to attach event handlers to an `EventStream`s. It keeps references to your event handlers (and the `EventStream` only keeps weak references) so that your event handlers can be garbage-collected once the `Observer` gets garbage-collected.

For more details, [check out the docs](http://dylemma.github.com/scala.frp/)

###How to Use it

Start by making an `EventSource`, which is itself an `EventStream`.

	val ints = EventSource[Int]

Now, `ints` can either `fire` events or `stop`. Once a stream has stopped, it can't fire any more events. Stopping an event stream isn't mandatory, but sometimes you need to know when a stream has stopped (e.g. `EventStream` concatenation). You can hook directly into the stream by using

	//requires an implicit Observer
	ints sink {
		case Fire(e) => 
			//handle event `e`
			true
		case Stop =>
			//handle stop
			false
	}

If using the `observe` method, your handler should return `true` if you want it to continue listening for new events, or `false` if it should stop listening. The `Observe` object provides some convenience methods:

	//listen to only the `Fire(i)` events
	ints foreach { i => println("Saw event: " + i) }

	//or with syntax sugar
	for(i <- ints) println("Saw event: " + i)

	//listen for the `Stop` event
	ints onEnd { println("Found the end of the stream!") }

	//listen to only the next `Fire` event
	ints onNext { i => println("It was " + i) }

There are a bunch of combinators that you can use with `EventStream`s.

	val intsTimesTwo: EventStream[Int] = ints.map { _ * 2 }
	val eventInts: EventStream[Int] = ints.filter{ _ % 2 == 0 }
	val firstThreeInts: EventStream[Int] = ints.take(3)
	val theRestOfThoseInts = ints.drop(3)

	import scala.concurrent.duration._
	val intsOnTime = ints.before(2 seconds fromNow)
	val intsInTime = ints.within(2.seconds)

There are even more, so check them out in the [docs](http://dylemma.github.com/scala.frp/api/current/index.html#scala.frp.EventStream)