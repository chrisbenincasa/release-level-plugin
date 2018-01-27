import com.chrisbenincasa.release_level.model.{Beta, PreAlpha}

object vals {
  @Beta
  val pig = 1

  def method(): Unit = {
    @PreAlpha
    val stuff = {
      123
    }
    val other = {
      123
    }

    println(stuff)
    println(pig)
  }
}
