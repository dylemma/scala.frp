Scala FRP
=========

###Background

Scala FRP (**F**unctional **R**eactive **P**rogramming) is a library inspired by [Ingo Maier's](http://lampwww.epfl.ch/~imaier/) paper, [Deprecating the Observer Pattern](http://lampwww.epfl.ch/~imaier/pub/DeprecatingObserversTR2010.pdf). Ingo Maier made an implementation of his "scala.react" framework which is available [on Github](https://github.com/ingoem/scala-react) in its original form. I also made a version of the same library that works with SBT to manage its dependencies, available [here](https://github.com/dylemma/scala.react).

###What it Does

Functional Reactive Programming is an alternative approach to the Publisher/Subscriber Pattern. It replaces the idea of a `Publisher` with an `EventStream`, and replaces the idea of a `Subscriber` with the fact that `EventStream`s can be composed and observed in a functional style.

Another thing that comes with the library is some semi-automatic memory management. A problem with the PubSub pattern was that it was easy to create a situation where the Publisher and Subscriber had a cyclical reference path between each other, preventing either from ever being garbage-collected. This library provides an `Observer` trait that manages references between an `EventStream` and any handlers, so that there won't be any cyclical references.

###How to Use it

Make an `EventStream`. `EventSource` is an implementation of `EventStream` that also has a `fire` method, so use that:

	val ints = new EventSource[Int]

Make an implicit `Observer`. You can either explicitly define one or mix it into the containing class. Once the `Observer` instance gets garbage-collected, any attached event handlers are fair game for garbage collection.

	implicit object obs extends Observer

At this point you can set up handlers and modified versions of the stream.

	Observe.events(ints){ i => println("Event: " + i) }
	//alternate syntax
	for(i <- ints) println("Event: " + i)

	//make a mapped event stream
	val intsTimesTwo: EventStream[Int] = ints.map(_ * 2)
	//or use 'for' syntax
	val intsTimesTwo: EventStream[Int] = for(i <- ints) yield i * 2