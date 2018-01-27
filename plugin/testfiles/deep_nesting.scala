import com.chrisbenincasa.release_level.model.Gamma

object deep_nesting {
  def one() = {
    def two() = {
      val xyz = 123
      def three() = {
        @Gamma
        def four: Int = 1

        println(xyz + 1)
      }
      three
    }
    two
  }
}
