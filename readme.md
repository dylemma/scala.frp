Scala FRP
=========

This library replaces the idea of `Publishers` and `Subscribers` with `EventStreams`. An `EventStream` can be treated like a collection of events; it can be transformed similarly to any other Scala collection, and instead of needing to call `publisher.addEventListener(new EventListener(){...})`, you simply call `eventStream.foreach{...}`

###Background

Scala FRP (stands for **F**unctional **R**eactive **P**rogramming) is a library inspired by [Ingo Maier's](http://lampwww.epfl.ch/~imaier/) paper, [Deprecating the Observer Pattern](http://lampwww.epfl.ch/~imaier/pub/DeprecatingObserversTR2010.pdf). Ingo Maier made an implementation of his "scala.react" framework which is available [on Github](https://github.com/ingoem/scala-react) in its original form. I also made a version of the same library that works with SBT to manage its dependencies, available [here](https://github.com/dylemma/scala.react).

###Getting it

Scala FRP is built for Scala 2.10.0; as long as your project is using Scala 2.10, you can simply add the following to your sbt settings:

	libraryDependencies += "io.dylemma" %% "scala-frp" % "1.0"

###Example Usage

	import io.dylemma.frp._

	// Mix in `Observer` for free memory management.
	object Example extends App with Observer {

		// Create a source of events.
		val ints = EventSource[Int]

		// You can derive new event streams any other event stream.
		val evenInts = ints.filter{ _ % 2 == 0 }
		val intsWithIndices = ints.zipWithIndex
		val soonInts = ints.before(2 seconds fromNow)

		// Attach event handlers.
		ints.foreach{ x => println(s"An int: $x") }
		evenInts.foreach { x => println(s"Even int: $x") }
		intsWithIndices.foreach { case (x, i) => println(s"$ith int was $x") }
		soonInts.foreach { x => println(s"$x came soon enough") }

		// Fire events!
		ints fire 1
		ints fire 2
		ints fire 3

	}

###FRP-101

Attaching event handlers requires an implicit `Observer`. The observer helps make sure that no cyclical references are made between the `EventStream` and the handler function; it keeps a weak reference to the handler so that the handler function may be garbage-collected once the `Observer` is garbage-collected. You can get an implicit `Observer` one of two ways:

 - Make one yourself: `implicit object myObserver extends Observer`
 - Mix it into the containing class, like in the example above. The `Observer` trait has an implicit reference to itself.

With that requirement out of the way, you're ready to start! There are two main classes that you will want to interact with. 

 - **EventStream** is a read-only class that you can attach event handlers to, and create mappings and combinations with. 
 - **EventSource** is a concrete implementation of `EventStream` that also exposes `fire` and `stop` methods.

###FRP-102

`EventStreams` are *finite*. At a low level, they emit events as an `Event[A]`, which can either be a `Fire(item)` or a `Stop`. When you call `foreach` on a stream, it will receive all `Fire` events until the stream emits a `Stop`. You can attach to these lower-level events with the `sink(handler: Event[A] => Boolean)` method. The handler will remain attached until it returns `false` in response to an event.

Because of the concept of *finite* `EventStreams`, streams are able to be concatenated; handlers may be attached that simply wait for the last event from the stream, or the eventual `Stop`.


For further details and a full list of capabilities, [check out the docs](http://dylemma.github.com/scala.frp/)