package scalax.frp

/** Typeclass for types that can get the current time. EventStream methods
  * that generate a time stamp of type `T` will require an implicitly-available
  * instance of `Time[T]`. A default implementation is available as [[SystemTime]],
  * which simply returns the result of `System.currentTimeMillis`.
  *
  * @tparam T The type of the time.
  */
trait Time[T] {

	/** @return the current time */
	def currentTime: T
}