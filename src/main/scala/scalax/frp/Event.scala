package scalax.frp

/** Represents an event emitted by a `Publisher` class.
  * An `Event` can be either a `Fire` or a `Stop`.
  * @tparam A The type of data contained by the event.
  */
sealed abstract class Event[+A]

/** Represents a single item from a stream of events.
  * @tparam A the type of the item in the event
  * @param event the item in the event
  */
case class Fire[A](event: A) extends Event[A]

/** Represents the end of a stream of events.
  */
case object Stop extends Event[Nothing]