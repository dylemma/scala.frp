package scala.frp

trait Stream[+A] {
	def sink(handler: A => Boolean)(implicit obs: Observer): Unit
	
	private [frp] def addHandler(handler: A => Boolean): Unit
	private [frp] def removeHandler(handler: A => Boolean): Unit
}
